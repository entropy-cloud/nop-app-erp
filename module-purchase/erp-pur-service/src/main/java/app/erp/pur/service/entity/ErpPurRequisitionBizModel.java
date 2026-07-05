
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
 * 采购请购单 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由 xbiz 一行委托注入 Processor；非审批动作（cancel/convertToOrder）在本类完成
 * Long→String 转换后委托 Processor。
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
    public ErpPurRequisition cancel(@Name("requisitionId") Long requisitionId, IServiceContext context) {
        return requisitionProcessor.cancel(String.valueOf(requisitionId), context);
    }

    @Override
    @BizMutation
    public ErpPurOrder convertToOrder(@Name("requisitionId") Long requisitionId,
                                      @Name("request") ConvertToOrderRequest request, IServiceContext context) {
        return requisitionProcessor.convertToOrder(String.valueOf(requisitionId), request, context);
    }
}
