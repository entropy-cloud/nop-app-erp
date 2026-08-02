package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.processor.ErpMfgScheduleToJobCardProcessor;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ErpMfgScheduleToJobCard generatePendingJobCards per-mutation Processor（R6.2，
 * {@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含批量待排程工序卡生成编排（config-gated + 逐单 best-effort）；共享 protected helper 单一真相源在
 * {@link ErpMfgScheduleToJobCardProcessor}，复用 {@link ErpMfgScheduleToJobCardGenerateJobCardsFromScheduleProcessor}
 * 单单生成 + facade {@code findWorkOrdersPendingJobCards}（:45 只读查询豁免）。
 *
 * <p>事务边界：跟随 Facade {@code @BizMutation} 事务，本类不带 {@code @Transactional}。
 */
public class ErpMfgScheduleToJobCardGeneratePendingJobCardsProcessor {

    static final Logger LOG = LoggerFactory.getLogger(ErpMfgScheduleToJobCardGeneratePendingJobCardsProcessor.class);

    @Inject
    ErpMfgScheduleToJobCardProcessor facade;
    @Inject
    ErpMfgScheduleToJobCardGenerateJobCardsFromScheduleProcessor generateJobCardsFromScheduleProcessor;

    public Integer generatePendingJobCards(IServiceContext context) {
        if (!facade.isAutoGenerateEnabled()) {
            LOG.info("erp-mfg-jobcard-auto-gen-skipped: erp-mfg.jobcard-auto-generate-on-schedule=false");
            return 0;
        }
        List<ErpMfgWorkOrder> pending = facade.findWorkOrdersPendingJobCards(
                ErpMfgScheduleToJobCardProcessor.DEFAULT_PENDING_LIMIT, context);
        if (pending.isEmpty()) {
            return 0;
        }
        int success = 0;
        for (ErpMfgWorkOrder wo : pending) {
            try {
                generateJobCardsFromScheduleProcessor.generateJobCardsFromSchedule(wo.getId(), context);
                success++;
            } catch (Exception e) {
                LOG.warn("erp-mfg-jobcard-auto-gen-failed: workOrderId={} code={}", wo.getId(), wo.getCode(), e);
            }
        }
        LOG.info("erp-mfg-jobcard-auto-gen-done: total={} success={}", pending.size(), success);
        return success;
    }
}
