package app.erp.crm.service.processor;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmStage;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpCrmLead moveStage per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含漏斗阶段流转编排（sequence 单向递增守卫 + convLog 全量留痕）；共享 protected helper 单一真相源在
 * {@link ErpCrmLeadProcessor}（slim-to-S-delegation facade）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCrmLeadMoveStageProcessor {

    @Inject
    ErpCrmLeadProcessor facade;

    public ErpCrmLead moveStage(Long leadId, Long toStageId, IServiceContext context) {
        ErpCrmLead lead = facade.requireLead(leadId, context);
        facade.validateMovable(lead, context);
        ErpCrmStage toStage = facade.requireStage(toStageId, context);
        Long fromStageId = lead.getStageId();
        facade.validateStageDirection(lead, fromStageId, toStage, context);
        facade.doMoveStage(lead, toStage, fromStageId, context);
        return lead;
    }
}
