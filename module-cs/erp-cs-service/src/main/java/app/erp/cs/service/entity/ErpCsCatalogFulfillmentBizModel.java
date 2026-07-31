package app.erp.cs.service.entity;

import app.erp.cs.biz.IErpCsCatalogFulfillmentBiz;
import app.erp.cs.dao.entity.ErpCsCatalogFulfillment;
import app.erp.cs.service.processor.ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

/**
 * 目录项履行映射 BizModel（{@code docs/design/customer-service/service-catalog.md §三}）。
 *
 * <p>{@link #executeFulfillmentSteps} 委派 {@link ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor}
 * —— 按 catalogItemId 加载 sequence 排序的 fulfillment 行，登记各动作执行结果（写 TicketAction 审计 DONE/SKIPPED）。
 *
 * <p>完整多步履行编排（ASSIGN_AGENT 技能匹配 / CREATE_CHILD_TICKET 子工单 / INVOKE_WORKFLOW 跨域）归 successor。
 */
@BizModel("ErpCsCatalogFulfillment")
public class ErpCsCatalogFulfillmentBizModel extends CrudBizModel<ErpCsCatalogFulfillment>
        implements IErpCsCatalogFulfillmentBiz {

    @Inject
    ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor executeFulfillmentStepsProcessor;

    public ErpCsCatalogFulfillmentBizModel() {
        setEntityName(ErpCsCatalogFulfillment.class.getName());
    }

    @Override
    @BizMutation
    public List<ErpCsCatalogFulfillment> executeFulfillmentSteps(@Name("catalogItemId") Long catalogItemId,
                                                                  @Name("ticketId") Long ticketId,
                                                                  IServiceContext context) {
        return executeFulfillmentStepsProcessor.executeFulfillmentSteps(catalogItemId, ticketId, context);
    }

    

}
