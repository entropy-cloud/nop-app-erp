
package app.erp.sal.biz;

import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.util.List;

import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalQuotation;
import app.erp.sal.dao.entity.ErpSalQuotationLine;

/**
 * 销售订单业务接口。除标准 CRUD 外，定义三轴审批状态机契约（对齐 {@code docs/design/sales/state-machine.md}）。
 */
public interface IErpSalOrderBiz extends ICrudBiz<ErpSalOrder> {

    @BizMutation
    ErpSalOrder submit(@Name("orderId") Long orderId, IServiceContext context);

    @BizMutation
    ErpSalOrder withdrawSubmit(@Name("orderId") Long orderId, IServiceContext context);

    @BizMutation
    ErpSalOrder approve(@Name("orderId") Long orderId, IServiceContext context);

    @BizMutation
    ErpSalOrder reject(@Name("orderId") Long orderId, IServiceContext context);

    @BizMutation
    ErpSalOrder reverseApprove(@Name("orderId") Long orderId, IServiceContext context);

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
}
