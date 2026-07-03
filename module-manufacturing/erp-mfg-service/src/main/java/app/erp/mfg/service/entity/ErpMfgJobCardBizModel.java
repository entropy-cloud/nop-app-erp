
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgJobCardBiz;
import app.erp.mfg.biz.JobCardWorkRecord;
import app.erp.mfg.dao.entity.ErpMfgJobCard;
import app.erp.mfg.dao.entity.ErpMfgJobCardTimeLog;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.RequestBean;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 作业卡 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现作业卡 8 态状态机
 * （{@code docs/design/manufacturing/state-machine.md §适用对象二`}）+ 报工成本归集。
 *
 * <p>报工（{@link #recordWork}）：在 WORK_IN_PROGRESS/SUBMITTED 态录入 JobCardTimeLog，
 * 人工成本 = {@code durationMins/60 × hourlyRate} → JobCardTimeLog.laborCost，累加回写
 * {@link ErpMfgWorkOrder#getLaborCost}（+ JobCard.completedQuantity/scrappedQuantity 累加）。
 *
 * <p>状态机迁移校验前置 JobCard {@code status}，违反抛 {@link NopException}。
 *
 * <p>权威：{@code docs/design/manufacturing/state-machine.md §适用对象二}、
 * {@code docs/plans/2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md} Phase 4。
 */
@BizModel("ErpMfgJobCard")
public class ErpMfgJobCardBizModel extends CrudBizModel<ErpMfgJobCard> implements IErpMfgJobCardBiz {

    static final BigDecimal SIXTY = new BigDecimal("60");
    static final int COST_SCALE = 4;

    public ErpMfgJobCardBizModel() {
        setEntityName(ErpMfgJobCard.class.getName());
    }

    @Override
    @BizMutation
    public ErpMfgJobCard startJob(@Name("jobCardId") Long jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = requireJobCard(jobCardId, context);
        requireStatus(jc, ErpMfgConstants.JOB_CARD_STATUS_OPEN, "OPEN");
        jc.setStatus(ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS);
        dao().updateEntity(jc);
        return jc;
    }

    @Override
    @BizMutation
    public ErpMfgJobCard recordWork(@RequestBean JobCardWorkRecord record, IServiceContext context) {
        ErpMfgJobCard jc = requireJobCard(record.getJobCardId(), context);
        Integer status = jc.getStatus();
        if (status == null || (status != ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS
                && status != ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED)) {
            throw illegalTransition(jc, status, "WORK_IN_PROGRESS 或 SUBMITTED");
        }

        BigDecimal duration = nz(record.getDurationMins());
        BigDecimal rate = nz(record.getHourlyRate());
        // 人工成本 = durationMins/60 × hourlyRate（Phase 1 类型修正后为 DECIMAL 数值计算）
        BigDecimal laborCost = duration.divide(SIXTY, COST_SCALE, RoundingMode.HALF_UP).multiply(rate);

        ErpMfgJobCardTimeLog log = newLog(jc, record, laborCost);
        daoFor(ErpMfgJobCardTimeLog.class).saveEntity(log);

        // 回写 JobCard 累计产量 + WorkOrder 人工成本
        BigDecimal completed = nz(record.getCompletedQuantity());
        BigDecimal scrapped = nz(record.getScrappedQuantity());
        jc.setCompletedQuantity(nz(jc.getCompletedQuantity()).add(completed));
        jc.setScrappedQuantity(nz(jc.getScrappedQuantity()).add(scrapped));
        dao().updateEntity(jc);

        applyLaborCostToWorkOrder(jc.getWorkOrderId(), laborCost);
        return jc;
    }

    @Override
    @BizMutation
    public ErpMfgJobCard submitJob(@Name("jobCardId") Long jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = requireJobCard(jobCardId, context);
        Integer status = jc.getStatus();
        if (status == null || (status != ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS
                && status != ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD)) {
            throw illegalTransition(jc, status, "WORK_IN_PROGRESS 或 ON_HOLD");
        }
        jc.setStatus(ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED);
        dao().updateEntity(jc);
        return jc;
    }

    @Override
    @BizMutation
    public ErpMfgJobCard completeJob(@Name("jobCardId") Long jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = requireJobCard(jobCardId, context);
        requireStatus(jc, ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED, "SUBMITTED");
        jc.setStatus(ErpMfgConstants.JOB_CARD_STATUS_COMPLETED);
        dao().updateEntity(jc);
        return jc;
    }

    @Override
    @BizMutation
    public ErpMfgJobCard holdJob(@Name("jobCardId") Long jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = requireJobCard(jobCardId, context);
        requireStatus(jc, ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS, "WORK_IN_PROGRESS");
        jc.setStatus(ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD);
        dao().updateEntity(jc);
        return jc;
    }

    @Override
    @BizMutation
    public ErpMfgJobCard resumeJob(@Name("jobCardId") Long jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = requireJobCard(jobCardId, context);
        requireStatus(jc, ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD, "ON_HOLD");
        jc.setStatus(ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS);
        dao().updateEntity(jc);
        return jc;
    }

    @Override
    @BizMutation
    public ErpMfgJobCard cancelJob(@Name("jobCardId") Long jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = requireJobCard(jobCardId, context);
        Integer status = jc.getStatus();
        if (status == null || (status != ErpMfgConstants.JOB_CARD_STATUS_OPEN
                && status != ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS
                && status != ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD)) {
            throw illegalTransition(jc, status, "OPEN、WORK_IN_PROGRESS 或 ON_HOLD");
        }
        jc.setStatus(ErpMfgConstants.JOB_CARD_STATUS_CANCELLED);
        dao().updateEntity(jc);
        return jc;
    }

    // ---------- helpers ----------

    private ErpMfgJobCard requireJobCard(Long jobCardId, IServiceContext context) {
        return requireEntity(String.valueOf(jobCardId), null, context);
    }

    private void requireStatus(ErpMfgJobCard jc, int expected, String expectedLabel) {
        Integer current = jc.getStatus();
        if (current == null || current != expected) {
            throw illegalTransition(jc, current, expectedLabel);
        }
    }

    private ErpMfgJobCardTimeLog newLog(ErpMfgJobCard jc, JobCardWorkRecord record, BigDecimal laborCost) {
        IEntityDao<ErpMfgJobCardTimeLog> dao = daoFor(ErpMfgJobCardTimeLog.class);
        ErpMfgJobCardTimeLog log = dao.newEntity();
        log.setJobCardId(jc.getId());
        log.setWorkOrderId(jc.getWorkOrderId());
        log.setOperatorId(record.getOperatorId());
        log.setWorkDate(record.getWorkDate() != null ? record.getWorkDate() : LocalDate.now());
        log.setDurationMins(nz(record.getDurationMins()));
        log.setSetupMins(nz(record.getSetupMins()));
        log.setRunMins(nz(record.getRunMins()));
        log.setHourlyRate(nz(record.getHourlyRate()));
        log.setCompletedQuantity(nz(record.getCompletedQuantity()));
        log.setScrappedQuantity(nz(record.getScrappedQuantity()));
        log.setLaborCost(laborCost);
        log.setRemark(record.getRemark());
        return log;
    }

    private void applyLaborCostToWorkOrder(Long workOrderId, BigDecimal laborCostDelta) {
        if (workOrderId == null || laborCostDelta == null || laborCostDelta.signum() == 0) {
            return;
        }
        IEntityDao<ErpMfgWorkOrder> dao = daoFor(ErpMfgWorkOrder.class);
        ErpMfgWorkOrder wo = dao.getEntityById(workOrderId);
        if (wo == null) {
            return;
        }
        wo.setLaborCost(nz(wo.getLaborCost()).add(laborCostDelta));
        ErpMfgMaterialIssueBizModel.recomputeTotals(wo);
        dao.updateEntity(wo);
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private NopException illegalTransition(ErpMfgJobCard jc, Integer current, String expected) {
        return new NopException(ErpMfgErrors.ERR_INVALID_STATUS_TRANSITION)
                .param(ErpMfgErrors.ARG_JOB_CARD_ID, jc.getId())
                .param(ErpMfgErrors.ARG_CURRENT_STATUS, current)
                .param(ErpMfgErrors.ARG_EXPECTED_STATUS, expected);
    }
}
