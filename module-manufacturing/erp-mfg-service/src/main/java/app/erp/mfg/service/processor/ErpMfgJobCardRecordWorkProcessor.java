package app.erp.mfg.service.processor;

import app.erp.mfg.biz.JobCardWorkRecord;
import app.erp.mfg.dao.entity.ErpMfgJobCard;
import app.erp.mfg.dao.entity.ErpMfgJobCardTimeLog;
import app.erp.mfg.service.statemachine.ErpMfgJobCardStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * ErpMfgJobCard recordWork per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含报工编排（报工时长记录 + 累计产量 + 人工成本回写 WorkOrder）；共享 protected helper 单一真相源在
 * {@link ErpMfgJobCardProcessor}。人工成本 = durationMins/60 × hourlyRate。
 *
 * <p>recordWork 不改 status（validation-only）：固定来源态校验经 {@link ErpMfgJobCardStateMachine#assertCanRecordWork}
 * Bean（{WORK_IN_PROGRESS, SUBMITTED} allow-list）。动态副作用（TimeLog 记录 + 报工数量累计 + laborCost 回写、乐观锁）保留原位。
 */
public class ErpMfgJobCardRecordWorkProcessor {

    static final BigDecimal SIXTY = new BigDecimal("60");
    static final int COST_SCALE = 4;

    @Inject
    ErpMfgJobCardProcessor facade;
    @Inject
    ErpMfgJobCardStateMachine stateMachine;

    public ErpMfgJobCard recordWork(JobCardWorkRecord record, IServiceContext context) {
        ErpMfgJobCard jc = facade.requireJobCard(record.getJobCardId(), context);
        String from = jc.getStatus();
        try {
            stateMachine.assertCanRecordWork(from);
        } catch (NopException e) {
            throw facade.illegalTransition(jc, from, "WORK_IN_PROGRESS 或 SUBMITTED", e);
        }
        BigDecimal laborCost = computeLaborCost(record);

        ErpMfgJobCardTimeLog log = facade.newLog(jc, record, laborCost);
        facade.daoProvider.daoFor(ErpMfgJobCardTimeLog.class).saveEntity(log);

        accumulateQuantities(jc, record);
        facade.jobCardDao().updateEntity(jc);

        facade.applyLaborCostToWorkOrder(jc.getWorkOrderId(), laborCost);
        return jc;
    }

    protected BigDecimal computeLaborCost(JobCardWorkRecord record) {
        BigDecimal duration = ErpMfgJobCardProcessor.nz(record.getDurationMins());
        BigDecimal rate = ErpMfgJobCardProcessor.nz(record.getHourlyRate());
        return duration.divide(SIXTY, COST_SCALE, RoundingMode.HALF_UP).multiply(rate);
    }

    protected void accumulateQuantities(ErpMfgJobCard jc, JobCardWorkRecord record) {
        BigDecimal completed = ErpMfgJobCardProcessor.nz(record.getCompletedQuantity());
        BigDecimal scrapped = ErpMfgJobCardProcessor.nz(record.getScrappedQuantity());
        jc.setCompletedQuantity(ErpMfgJobCardProcessor.nz(jc.getCompletedQuantity()).add(completed));
        jc.setScrappedQuantity(ErpMfgJobCardProcessor.nz(jc.getScrappedQuantity()).add(scrapped));
    }
}
