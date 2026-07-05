
package app.erp.pur.biz;

import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import java.util.List;

import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurRequisition;
import app.erp.pur.dao.entity.ErpPurRequisitionLine;

/**
 * 采购订单业务接口。标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）
 * 由 {@link IApprovableBiz} 声明，运行时由平台 {@code approval-support.xbiz} 标准 source 提供。
 */
public interface IErpPurOrderBiz extends ICrudBiz<ErpPurOrder>, IApprovableBiz<ErpPurOrder> {

    @BizMutation
    ErpPurOrder cancel(@Name("orderId") Long orderId, IServiceContext context);

    /**
     * 由请购单派生采购订单（跨聚合写：请购 APPROVED → 新建订单 + 行 + 回链 requisitionId）。
     * 含幂等防重复转化（查既有 docStatus≠CANCELLED 且 requisitionId 命中的订单）。
     */
    @BizAction
    ErpPurOrder createFromRequisition(@Name("requisition") ErpPurRequisition requisition,
                                      @Name("lines") List<ErpPurRequisitionLine> lines,
                                      @Name("supplierId") Long supplierId,
                                      @Name("request") ConvertToOrderRequest request,
                                      IServiceContext context);

    /**
     * 幂等键查询：是否存在 docStatus≠CANCELLED 且 {@code requisitionId} 命中的订单。
     */
    @BizAction
    boolean existsActiveByRequisition(@Name("requisitionId") Long requisitionId, IServiceContext context);

    /**
     * 回写源订单收货进度（由入库单审核后跨聚合调用）。
     */
    @BizAction
    void updateReceiveStatus(@Name("orderId") Long orderId,
                             @Name("receiveStatus") String receiveStatus,
                             IServiceContext context);
}
