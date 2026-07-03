
package app.erp.sal.service.entity;

import app.erp.sal.biz.IErpSalDeliveryBiz;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.service.processor.ErpSalDeliveryProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 销售出库单 BizModel（Facade）。审批状态机 + 出库审核触发库存移动编排委托
 * {@link ErpSalDeliveryProcessor}（protected step 方法，下游可逐 step 覆盖）。
 */
@BizModel("ErpSalDelivery")
public class ErpSalDeliveryBizModel extends CrudBizModel<ErpSalDelivery> implements IErpSalDeliveryBiz {

    @Inject
    ErpSalDeliveryProcessor deliveryProcessor;

    public ErpSalDeliveryBizModel() {
        setEntityName(ErpSalDelivery.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalDelivery submit(@Name("deliveryId") Long deliveryId, IServiceContext context) {
        return deliveryProcessor.submit(deliveryId, context);
    }

    @Override
    @BizMutation
    public ErpSalDelivery withdrawSubmit(@Name("deliveryId") Long deliveryId, IServiceContext context) {
        return deliveryProcessor.withdrawSubmit(deliveryId, context);
    }

    @Override
    @BizMutation
    public ErpSalDelivery approve(@Name("deliveryId") Long deliveryId, IServiceContext context) {
        return deliveryProcessor.approve(deliveryId, context);
    }

    @Override
    @BizMutation
    public ErpSalDelivery reject(@Name("deliveryId") Long deliveryId, IServiceContext context) {
        return deliveryProcessor.reject(deliveryId, context);
    }

    @Override
    @BizMutation
    public ErpSalDelivery reverseApprove(@Name("deliveryId") Long deliveryId, IServiceContext context) {
        return deliveryProcessor.reverseApprove(deliveryId, context);
    }

    @Override
    @BizMutation
    public ErpSalDelivery cancel(@Name("deliveryId") Long deliveryId, IServiceContext context) {
        return deliveryProcessor.cancel(deliveryId, context);
    }
}
