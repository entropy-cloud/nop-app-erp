
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
 * 销售出库单 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由平台 {@code approval-support.xbiz} 标准 source 提供，业务联动经 xbiz
 * {@code <source x:override="replace">} 注入 {@link ErpSalDeliveryProcessor#onSubmit}/{@link ErpSalDeliveryProcessor#onApproved}/{@link ErpSalDeliveryProcessor#onReverseApproved}。
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
    public ErpSalDelivery cancel(@Name("deliveryId") Long deliveryId, IServiceContext context) {
        return deliveryProcessor.cancel(String.valueOf(deliveryId), context);
    }
}
