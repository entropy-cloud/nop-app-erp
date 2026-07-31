package app.erp.log.service.processor;

import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.log.service.gateway.GatewayDispatcher;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpLogShipment cancelShipment per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 *
 * <p>ADVISED/DISPATCHED/IN_TRANSIT/DRAFT→CANCELLED（DISPATCHED 以上经承运商取消），原 BizModel 为 GatewayDispatcher
 * 单行委托，按 plan 全 6 mutation 须拆迁移至此（BizModel → Processor 单行委托，编排位置迁移，业务语义不变）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpLogShipmentCancelShipmentProcessor {

    @Inject
    GatewayDispatcher gatewayDispatcher;

    public ErpLogShipment cancelShipment(Long shipmentId, IServiceContext context) {
        return gatewayDispatcher.cancelShipment(shipmentId, context);
    }
}
