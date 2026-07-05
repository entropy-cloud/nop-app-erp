
package app.erp.pur.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurRequisition;

/**
 * 采购请购单业务接口。标准审批动作由 {@link IApprovableBiz} 声明，运行时由平台
 * {@code approval-support.xbiz} 标准 source 提供。
 */
public interface IErpPurRequisitionBiz extends ICrudBiz<ErpPurRequisition>, IApprovableBiz<ErpPurRequisition> {

    @BizMutation
    ErpPurRequisition cancel(@Name("requisitionId") Long requisitionId, IServiceContext context);

    /**
     * 将 APPROVED 请购单转化为采购订单。入口在请购侧（请购 APPROVED → 派生订单）。
     */
    @BizMutation
    ErpPurOrder convertToOrder(@Name("requisitionId") Long requisitionId,
                               @Name("request") ConvertToOrderRequest request,
                               IServiceContext context);
}
