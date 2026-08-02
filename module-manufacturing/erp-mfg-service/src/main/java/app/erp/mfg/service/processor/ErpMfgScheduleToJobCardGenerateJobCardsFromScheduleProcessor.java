package app.erp.mfg.service.processor;

import app.erp.mfg.biz.ApsLoadSlot;
import app.erp.mfg.dao.entity.ErpMfgJobCard;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.List;

/**
 * ErpMfgScheduleToJobCard generateJobCardsFromSchedule per-mutation Processor（R6.2，
 * {@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 APS 排程→工序卡自动生成编排；共享 protected helper 单一真相源在 {@link ErpMfgScheduleToJobCardProcessor}。
 *
 * <p>事务边界：跟随 Facade {@code @BizMutation} 事务，本类不带 {@code @Transactional}。
 */
public class ErpMfgScheduleToJobCardGenerateJobCardsFromScheduleProcessor {

    @Inject
    ErpMfgScheduleToJobCardProcessor facade;

    public ErpMfgWorkOrder generateJobCardsFromSchedule(Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = facade.requireWorkOrder(workOrderId);
        facade.validateStatusForJobCardGen(wo);

        List<ApsLoadSlot> slots = facade.fetchSlots(Collections.singletonList(workOrderId));
        if (slots.isEmpty()) {
            throw new NopException(ErpMfgErrors.ERR_NO_SCHEDULED_OPERATIONS)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, wo.getCode());
        }

        List<ErpMfgJobCard> existing = facade.findJobCardsForWorkOrder(workOrderId);
        List<ApsLoadSlot> toBuild = facade.resolveSlotsToBuild(slots, existing, wo);
        if (toBuild.isEmpty()) {
            return wo;
        }

        for (ApsLoadSlot slot : toBuild) {
            ErpMfgJobCard jc = facade.newJobCard(wo, slot);
            facade.jobCardDao().saveEntity(jc);
        }

        facade.markWorkOrderScheduled(wo, slots);
        facade.workOrderDao().updateEntity(wo);
        return wo;
    }
}
