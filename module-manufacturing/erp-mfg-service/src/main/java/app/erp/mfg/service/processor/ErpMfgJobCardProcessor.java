package app.erp.mfg.service.processor;

import app.erp.mfg.biz.JobCardWorkRecord;
import app.erp.mfg.dao.entity.ErpMfgJobCard;
import app.erp.mfg.dao.entity.ErpMfgJobCardTimeLog;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 作业卡状态机编排 Processor（{@code processor-extension-pattern.md} 两层结构：Facade + Processor）。
 * Facade {@code ErpMfgJobCardBizModel} 仅负责入口/事务/委托，编排委托本类。
 *
 * <p>作业卡 8 态状态机 + 报工成本归集（人工成本 = durationMins/60 × hourlyRate → 回写 WorkOrder.laborCost）。
 * 每个动作只编排步骤顺序，各步骤为 {@code protected} 方法、以 {@link IServiceContext} 为末参。
 *
 * <p>事务边界：跟随 Facade {@code @BizMutation} 事务，本类不带 {@code @Transactional}。
 */
public class ErpMfgJobCardProcessor {

    static final BigDecimal SIXTY = new BigDecimal("60");
    static final int COST_SCALE = 4;

    @Inject
    IDaoProvider daoProvider;

    public ErpMfgJobCard startJob(Long jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = requireJobCard(jobCardId, context);
        requireStatus(jc, ErpMfgConstants.JOB_CARD_STATUS_OPEN, "OPEN");
        jc.setStatus(ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS);
        jobCardDao().updateEntity(jc);
        return jc;
    }

    public ErpMfgJobCard recordWork(JobCardWorkRecord record, IServiceContext context) {
        ErpMfgJobCard jc = requireJobCard(record.getJobCardId(), context);
        String status = jc.getStatus();
        if (status == null || (!Objects.equals(status, ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS)
                && !Objects.equals(status, ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED))) {
            throw illegalTransition(jc, status, "WORK_IN_PROGRESS 或 SUBMITTED");
        }

        BigDecimal duration = nz(record.getDurationMins());
        BigDecimal rate = nz(record.getHourlyRate());
        // 人工成本 = durationMins/60 × hourlyRate
        BigDecimal laborCost = duration.divide(SIXTY, COST_SCALE, RoundingMode.HALF_UP).multiply(rate);

        ErpMfgJobCardTimeLog log = newLog(jc, record, laborCost);
        daoProvider.daoFor(ErpMfgJobCardTimeLog.class).saveEntity(log);

        // 回写 JobCard 累计产量
        BigDecimal completed = nz(record.getCompletedQuantity());
        BigDecimal scrapped = nz(record.getScrappedQuantity());
        jc.setCompletedQuantity(nz(jc.getCompletedQuantity()).add(completed));
        jc.setScrappedQuantity(nz(jc.getScrappedQuantity()).add(scrapped));
        jobCardDao().updateEntity(jc);

        // 回写 WorkOrder 人工成本
        applyLaborCostToWorkOrder(jc.getWorkOrderId(), laborCost);
        return jc;
    }

    public ErpMfgJobCard submitJob(Long jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = requireJobCard(jobCardId, context);
        String status = jc.getStatus();
        if (status == null || (!Objects.equals(status, ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS)
                && !Objects.equals(status, ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD))) {
            throw illegalTransition(jc, status, "WORK_IN_PROGRESS 或 ON_HOLD");
        }
        jc.setStatus(ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED);
        jobCardDao().updateEntity(jc);
        return jc;
    }

    public ErpMfgJobCard completeJob(Long jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = requireJobCard(jobCardId, context);
        requireStatus(jc, ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED, "SUBMITTED");
        jc.setStatus(ErpMfgConstants.JOB_CARD_STATUS_COMPLETED);
        jobCardDao().updateEntity(jc);
        return jc;
    }

    public ErpMfgJobCard holdJob(Long jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = requireJobCard(jobCardId, context);
        requireStatus(jc, ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS, "WORK_IN_PROGRESS");
        jc.setStatus(ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD);
        jobCardDao().updateEntity(jc);
        return jc;
    }

    public ErpMfgJobCard resumeJob(Long jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = requireJobCard(jobCardId, context);
        requireStatus(jc, ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD, "ON_HOLD");
        jc.setStatus(ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS);
        jobCardDao().updateEntity(jc);
        return jc;
    }

    public ErpMfgJobCard cancelJob(Long jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = requireJobCard(jobCardId, context);
        String status = jc.getStatus();
        if (status == null || (!Objects.equals(status, ErpMfgConstants.JOB_CARD_STATUS_OPEN)
                && !Objects.equals(status, ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS)
                && !Objects.equals(status, ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD))) {
            throw illegalTransition(jc, status, "OPEN、WORK_IN_PROGRESS 或 ON_HOLD");
        }
        jc.setStatus(ErpMfgConstants.JOB_CARD_STATUS_CANCELLED);
        jobCardDao().updateEntity(jc);
        return jc;
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpMfgJobCard requireJobCard(Long jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = jobCardDao().getEntityById(jobCardId);
        if (jc == null) {
            throw new NopException(ErpMfgErrors.ERR_JOB_CARD_NOT_FOUND)
                    .param(ErpMfgErrors.ARG_JOB_CARD_ID, jobCardId);
        }
        return jc;
    }

    protected void requireStatus(ErpMfgJobCard jc, String expected, String expectedLabel) {
        String current = jc.getStatus();
        if (current == null || !Objects.equals(current, expected)) {
            throw illegalTransition(jc, current, expectedLabel);
        }
    }

    protected ErpMfgJobCardTimeLog newLog(ErpMfgJobCard jc, JobCardWorkRecord record, BigDecimal laborCost) {
        IEntityDao<ErpMfgJobCardTimeLog> dao = daoProvider.daoFor(ErpMfgJobCardTimeLog.class);
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

    protected void applyLaborCostToWorkOrder(Long workOrderId, BigDecimal laborCostDelta) {
        if (workOrderId == null || laborCostDelta == null || laborCostDelta.signum() == 0) {
            return;
        }
        IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
        ErpMfgWorkOrder wo = dao.getEntityById(workOrderId);
        if (wo == null) {
            return;
        }
        wo.setLaborCost(nz(wo.getLaborCost()).add(laborCostDelta));
        ErpMfgWorkOrderProcessor.recomputeTotals(wo);
        dao.updateEntity(wo);
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpMfgJobCard> jobCardDao() {
        return daoProvider.daoFor(ErpMfgJobCard.class);
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    protected NopException illegalTransition(ErpMfgJobCard jc, String current, String expected) {
        return new NopException(ErpMfgErrors.ERR_INVALID_STATUS_TRANSITION)
                .param(ErpMfgErrors.ARG_JOB_CARD_ID, jc.getId())
                .param(ErpMfgErrors.ARG_CURRENT_STATUS, current)
                .param(ErpMfgErrors.ARG_EXPECTED_STATUS, expected);
    }
}
