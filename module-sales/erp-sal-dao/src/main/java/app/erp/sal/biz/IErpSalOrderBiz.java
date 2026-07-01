
package app.erp.sal.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.sal.dao.entity.ErpSalOrder;

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
}
