
package app.erp.pur.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.pur.dao.entity.ErpPurOrder;

/**
 * 采购订单业务接口。除标准 CRUD 外，定义三轴审批状态机契约（对齐 {@code docs/design/purchase/state-machine.md} §2
 * 「采购订单｜仅状态推进」，订单审核不直接触发库存/凭证——下游单据才触发）。
 */
public interface IErpPurOrderBiz extends ICrudBiz<ErpPurOrder> {

    @BizMutation
    ErpPurOrder submit(@Name("orderId") Long orderId, IServiceContext context);

    @BizMutation
    ErpPurOrder withdrawSubmit(@Name("orderId") Long orderId, IServiceContext context);

    @BizMutation
    ErpPurOrder approve(@Name("orderId") Long orderId, IServiceContext context);

    @BizMutation
    ErpPurOrder reject(@Name("orderId") Long orderId, IServiceContext context);

    @BizMutation
    ErpPurOrder reverseApprove(@Name("orderId") Long orderId, IServiceContext context);

    @BizMutation
    ErpPurOrder cancel(@Name("orderId") Long orderId, IServiceContext context);
}
