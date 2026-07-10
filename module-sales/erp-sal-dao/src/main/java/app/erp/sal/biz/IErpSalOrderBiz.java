
package app.erp.sal.biz;

import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import java.util.List;

import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalQuotation;
import app.erp.sal.dao.entity.ErpSalQuotationLine;

/**
 * 销售订单业务接口。标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）
 * 由 {@link IApprovableBiz} 声明，运行时由平台 {@code approval-support.xbiz} 标准 source 提供。
 */
public interface IErpSalOrderBiz extends ICrudBiz<ErpSalOrder>, IApprovableBiz<ErpSalOrder> {

    @BizMutation
    ErpSalOrder cancel(@Name("orderId") Long orderId, IServiceContext context);

    /**
     * 由报价单派生销售订单（跨聚合写：报价 APPROVED+已确认 → 新建订单 + 行 + 回链 quotationId）。
     */
    @BizAction
    ErpSalOrder createFromQuotation(@Name("quotation") ErpSalQuotation quotation,
                                    @Name("lines") List<ErpSalQuotationLine> lines,
                                    IServiceContext context);

    /**
     * 幂等键查询：是否存在 docStatus≠CANCELLED 且 {@code quotationId} 命中的订单。
     */
    @BizAction
    boolean existsActiveByQuotation(@Name("quotationId") Long quotationId, IServiceContext context);

    /**
     * 回写源订单发货进度（由出库单审核后跨聚合调用）。
     */
    @BizAction
    void updateDeliveryStatus(@Name("orderId") Long orderId,
                              @Name("deliveryStatus") String deliveryStatus,
                              IServiceContext context);

    /**
     * UC-SAL-11：对订单应用促销规则引擎，写回订单行折扣/赠品行 + 重算订单头合计。
     */
    @BizMutation
    void applyPricingRules(@Name("orderId") String orderId, IServiceContext context);
}
