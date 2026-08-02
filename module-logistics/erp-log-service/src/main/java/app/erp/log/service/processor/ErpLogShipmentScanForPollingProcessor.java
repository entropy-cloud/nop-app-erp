package app.erp.log.service.processor;

import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.log.service.ErpLogConstants;
import io.nop.core.context.IServiceContext;

import java.util.List;

/**
 * ErpLogShipment scanForPolling per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 *
 * <p>轮询兜底：对 DISPATCHED/IN_TRANSIT 运单调 trackShipment 推进；本轮迁移到 DELIVERED 的运单逐个触发
 * {@link #onDelivered} 运费过账/到岸成本编排（继承 {@link AbstractErpLogShipmentDeliveredProcessor}），
 * onDelivered 失败不中断扫描（运单保持 PENDING 由下轮重试）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 * （R6.7，processor-extension-pattern.md 每 mutation 一 Processor）。
 */
public class ErpLogShipmentScanForPollingProcessor extends AbstractErpLogShipmentDeliveredProcessor {

    public int scanForPolling(IServiceContext context) {
        List<ErpLogShipment> advanced = gatewayDispatcher.scanForPolling(context);
        for (ErpLogShipment shipment : advanced) {
            if (!ErpLogConstants.SHIPMENT_STATUS_DELIVERED.equals(shipment.getStatus())) {
                continue;
            }
            try {
                onDelivered(shipment, context);
            } catch (Exception e) {
                LOG.error("轮询驱动 DELIVERED 后 onDelivered 失败，运单 {} 保持 PENDING：{}",
                        shipment.getCode(), e.getMessage(), e);
            }
        }
        return advanced.size();
    }
}
