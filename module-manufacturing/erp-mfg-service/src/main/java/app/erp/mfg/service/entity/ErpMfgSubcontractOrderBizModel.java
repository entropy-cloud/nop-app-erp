
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgSubcontractOrderBiz;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.service.processor.ErpMfgSubcontractOrderIssueMaterialsProcessor;
import app.erp.mfg.service.processor.ErpMfgSubcontractOrderPostProcessingFeeProcessor;
import app.erp.mfg.service.processor.ErpMfgSubcontractOrderProcessor;
import app.erp.mfg.service.processor.ErpMfgSubcontractOrderReceiveFinishedProcessor;
import app.erp.mfg.service.processor.ErpMfgSubcontractOrderReverseCompletionProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;

/**
 * 委外加工单 BizModel（Facade，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 委外生命周期三段（issueMaterials/receiveFinished/postProcessingFee）+ reverseCompletion 委托 4 个
 * {@code ErpMfgSubcontractOrder<Method>Processor}（R6.2 per-mutation 拆分）；{@code cancel}（:46 单步状态翻转）
 * 为合法豁免保留委托 facade。审批动作经实体 xbiz 委托 per-mutation Processor（plan 2026-07-30-1909-2 R5.5）。
 *
 * <p>语义见 {@code docs/design/manufacturing/subcontracting.md}。
 */
@BizModel("ErpMfgSubcontractOrder")
public class ErpMfgSubcontractOrderBizModel extends CrudBizModel<ErpMfgSubcontractOrder> implements IErpMfgSubcontractOrderBiz {

    @Inject
    ErpMfgSubcontractOrderProcessor subcontractOrderProcessor;
    @Inject
    ErpMfgSubcontractOrderIssueMaterialsProcessor issueMaterialsProcessor;
    @Inject
    ErpMfgSubcontractOrderReceiveFinishedProcessor receiveFinishedProcessor;
    @Inject
    ErpMfgSubcontractOrderPostProcessingFeeProcessor postProcessingFeeProcessor;
    @Inject
    ErpMfgSubcontractOrderReverseCompletionProcessor reverseCompletionProcessor;

    public ErpMfgSubcontractOrderBizModel() {
        setEntityName(ErpMfgSubcontractOrder.class.getName());
    }

    @Override
    @BizMutation
    public ErpMfgSubcontractOrder cancel(@Name("subcontractOrderId") Long subcontractOrderId, IServiceContext context) {
        return subcontractOrderProcessor.cancel(subcontractOrderId, context);
    }

    @Override
    @BizMutation
    public ErpMfgSubcontractOrder issueMaterials(@Name("subcontractOrderId") Long subcontractOrderId,
                                                  @io.nop.api.core.annotations.core.Optional @Name("sourceWarehouseId") Long sourceWarehouseId,
                                                  IServiceContext context) {
        return issueMaterialsProcessor.issueMaterials(subcontractOrderId, sourceWarehouseId, context);
    }

    @Override
    @BizMutation
    public ErpMfgSubcontractOrder receiveFinished(@Name("subcontractOrderId") Long subcontractOrderId,
                                                   @Name("receivedQty") BigDecimal receivedQty,
                                                   @io.nop.api.core.annotations.core.Optional @Name("destWarehouseId") Long destWarehouseId,
                                                   IServiceContext context) {
        return receiveFinishedProcessor.receiveFinished(subcontractOrderId, receivedQty, destWarehouseId, context);
    }

    @Override
    @BizMutation
    public ErpMfgSubcontractOrder postProcessingFee(@Name("subcontractOrderId") Long subcontractOrderId, IServiceContext context) {
        return postProcessingFeeProcessor.postProcessingFee(subcontractOrderId, context);
    }

    @Override
    @BizMutation
    public ErpMfgSubcontractOrder reverseCompletion(@Name("subcontractOrderId") Long subcontractOrderId, IServiceContext context) {
        return reverseCompletionProcessor.reverseCompletion(subcontractOrderId, context);
    }

}
