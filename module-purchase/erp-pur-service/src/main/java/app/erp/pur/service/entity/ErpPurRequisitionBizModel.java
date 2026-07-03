
package app.erp.pur.service.entity;

import app.erp.pur.biz.ConvertToOrderRequest;
import app.erp.pur.biz.IErpPurRequisitionBiz;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurRequisition;
import app.erp.pur.service.processor.ErpPurRequisitionProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 采购请购单 BizModel（Facade）。审批状态机 + 请购→订单转化编排委托
 * {@link ErpPurRequisitionProcessor}（protected step 方法，下游可逐 step 覆盖）。
 */
@BizModel("ErpPurRequisition")
public class ErpPurRequisitionBizModel extends CrudBizModel<ErpPurRequisition> implements IErpPurRequisitionBiz {

    @Inject
    ErpPurRequisitionProcessor requisitionProcessor;

    public ErpPurRequisitionBizModel() {
        setEntityName(ErpPurRequisition.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurRequisition submit(@Name("requisitionId") Long requisitionId, IServiceContext context) {
        return requisitionProcessor.submit(requisitionId, context);
    }

    @Override
    @BizMutation
    public ErpPurRequisition withdrawSubmit(@Name("requisitionId") Long requisitionId, IServiceContext context) {
        return requisitionProcessor.withdrawSubmit(requisitionId, context);
    }

    @Override
    @BizMutation
    public ErpPurRequisition approve(@Name("requisitionId") Long requisitionId, IServiceContext context) {
        return requisitionProcessor.approve(requisitionId, context);
    }

    @Override
    @BizMutation
    public ErpPurRequisition reject(@Name("requisitionId") Long requisitionId, IServiceContext context) {
        return requisitionProcessor.reject(requisitionId, context);
    }

    @Override
    @BizMutation
    public ErpPurRequisition reverseApprove(@Name("requisitionId") Long requisitionId, IServiceContext context) {
        return requisitionProcessor.reverseApprove(requisitionId, context);
    }

    @Override
    @BizMutation
    public ErpPurRequisition cancel(@Name("requisitionId") Long requisitionId, IServiceContext context) {
        return requisitionProcessor.cancel(requisitionId, context);
    }

    @Override
    @BizMutation
    public ErpPurOrder convertToOrder(@Name("requisitionId") Long requisitionId,
                                      @Name("request") ConvertToOrderRequest request, IServiceContext context) {
        return requisitionProcessor.convertToOrder(requisitionId, request, context);
    }
}
