
package app.erp.sal.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.sal.dao.entity.ErpSalDelivery;

/**
 * 销售出库单业务接口。除标准 CRUD 外，定义三轴审批状态机契约（对齐 {@code docs/design/sales/state-machine.md}）：
 * approve 触发出库库存移动（{@code IErpInvStockMoveBiz.generateMove}，含可用量校验），reverseApprove 内部冲销。
 */
public interface IErpSalDeliveryBiz extends ICrudBiz<ErpSalDelivery> {

    @BizMutation
    ErpSalDelivery submit(@Name("deliveryId") Long deliveryId, IServiceContext context);

    @BizMutation
    ErpSalDelivery withdrawSubmit(@Name("deliveryId") Long deliveryId, IServiceContext context);

    @BizMutation
    ErpSalDelivery approve(@Name("deliveryId") Long deliveryId, IServiceContext context);

    @BizMutation
    ErpSalDelivery reject(@Name("deliveryId") Long deliveryId, IServiceContext context);

    @BizMutation
    ErpSalDelivery reverseApprove(@Name("deliveryId") Long deliveryId, IServiceContext context);

    @BizMutation
    ErpSalDelivery cancel(@Name("deliveryId") Long deliveryId, IServiceContext context);
}
