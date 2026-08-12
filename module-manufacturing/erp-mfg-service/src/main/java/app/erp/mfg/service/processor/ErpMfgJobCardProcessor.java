package app.erp.mfg.service.processor;

import app.erp.mfg.biz.JobCardWorkRecord;
import app.erp.mfg.dao.entity.ErpMfgJobCard;
import app.erp.mfg.dao.entity.ErpMfgJobCardTimeLog;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import io.nop.api.core.time.CoreMetrics;

/**
 * 作业卡共享 helper 持有者（R6.2 per-mutation 拆分后 facade 瘦身：7 个 D-mutation public 入口已迁入
 * {@code ErpMfgJobCard<Method>Processor}，本类仅保留 protected helper 供 per-mutation Processor 经同包调用）。
 *
 * <p>作业卡报工成本归集辅助：人工成本 = durationMins/60 × hourlyRate → 回写 WorkOrder.laborCost。
 * 事务边界：跟随 Facade {@code @BizMutation} 事务，本类不带 {@code @Transactional}。
 */
public class ErpMfgJobCardProcessor {

    @Inject
    IDaoProvider daoProvider;

    // ---------- 校验/查询辅助（protected，供 per-mutation Processor 复用与覆盖） ----------

    protected ErpMfgJobCard requireJobCard(Long jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = jobCardDao().getEntityById(jobCardId);
        if (jc == null) {
            throw new NopException(ErpMfgErrors.ERR_JOB_CARD_NOT_FOUND)
                    .param(ErpMfgErrors.ARG_JOB_CARD_ID, jobCardId);
        }
        return jc;
    }

    protected ErpMfgJobCardTimeLog newLog(ErpMfgJobCard jc, JobCardWorkRecord record, BigDecimal laborCost) {
        IEntityDao<ErpMfgJobCardTimeLog> dao = daoProvider.daoFor(ErpMfgJobCardTimeLog.class);
        ErpMfgJobCardTimeLog log = dao.newEntity();
        log.setJobCardId(jc.getId());
        log.setWorkOrderId(jc.getWorkOrderId());
        log.setOperatorId(record.getOperatorId());
        log.setWorkDate(record.getWorkDate() != null ? record.getWorkDate() : CoreMetrics.today());
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

    /**
     * 经 StateMachine Bean 断言来源态合法；非法边（Bean 报告 common 层码）映射为领域
     * {@code ERR_INVALID_STATUS_TRANSITION}（既有码，保持误命名「work-order」）+ 实体编号/上下文，
     * common 码作 cause 保留（契约 §7）。重命名该码归独立 Fix plan successor（属行为变更，本计划 Non-Goal）。
     */
    protected NopException illegalTransition(ErpMfgJobCard jc, String current, String expected, Throwable cause) {
        return new NopException(ErpMfgErrors.ERR_INVALID_STATUS_TRANSITION, cause)
                .param(ErpMfgErrors.ARG_JOB_CARD_ID, jc.getId())
                .param(ErpMfgErrors.ARG_CURRENT_STATUS, current)
                .param(ErpMfgErrors.ARG_EXPECTED_STATUS, expected);
    }
}
