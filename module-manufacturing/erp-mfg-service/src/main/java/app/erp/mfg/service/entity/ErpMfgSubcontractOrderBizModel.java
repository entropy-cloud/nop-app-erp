
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgSubcontractOrderBiz;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.service.processor.ErpMfgSubcontractOrderProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;

/**
 * 委外加工单 BizModel（Facade，{@code processor-extension-pattern.md} 两层结构）。
 * 审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）由平台
 * {@code approval-support.xbiz} 标准 source 提供并委托 {@link ErpMfgSubcontractOrderProcessor}。
 * 委外生命周期三段（issueMaterials/receiveFinished/postProcessingFee）+ cancel + reverseCompletion 委托 Processor。
 *
 * <p>语义见 {@code docs/design/manufacturing/subcontracting.md}。
 */
@BizModel("ErpMfgSubcontractOrder")
public class ErpMfgSubcontractOrderBizModel extends CrudBizModel<ErpMfgSubcontractOrder> implements IErpMfgSubcontractOrderBiz {

    @Inject
    ErpMfgSubcontractOrderProcessor subcontractOrderProcessor;

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
        return subcontractOrderProcessor.issueMaterials(subcontractOrderId, sourceWarehouseId, context);
    }

    @Override
    @BizMutation
    public ErpMfgSubcontractOrder receiveFinished(@Name("subcontractOrderId") Long subcontractOrderId,
                                                   @Name("receivedQty") BigDecimal receivedQty,
                                                   @io.nop.api.core.annotations.core.Optional @Name("destWarehouseId") Long destWarehouseId,
                                                   IServiceContext context) {
        return subcontractOrderProcessor.receiveFinished(subcontractOrderId, receivedQty, destWarehouseId, context);
    }

    @Override
    @BizMutation
    public ErpMfgSubcontractOrder postProcessingFee(@Name("subcontractOrderId") Long subcontractOrderId, IServiceContext context) {
        return subcontractOrderProcessor.postProcessingFee(subcontractOrderId, context);
    }

    @Override
    @BizMutation
    public ErpMfgSubcontractOrder reverseCompletion(@Name("subcontractOrderId") Long subcontractOrderId, IServiceContext context) {
        return subcontractOrderProcessor.reverseCompletion(subcontractOrderId, context);
    }

}
