package app.erp.log.service.processor;

import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.log.service.gateway.GatewayDispatcher;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpLogShipment completeShipment per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 *
 * <p>ADVISED→DISPATCHED 承运商网关下单（completeDeliveryOrder，重试/写回/死信），原 BizModel 为 GatewayDispatcher
 * 单行委托，按 plan 全 6 mutation 须拆迁移至此（BizModel → Processor 单行委托，编排位置迁移，业务语义不变）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpLogShipmentCompleteShipmentProcessor {

    @Inject
    GatewayDispatcher gatewayDispatcher;

    public ErpLogShipment completeShipment(Long shipmentId, IServiceContext context) {
        return gatewayDispatcher.completeShipment(shipmentId, context);
    }
}
