package app.erp.log.service.processor;

import app.erp.common.test.FaultInjectionStubs;
import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.log.service.ErpLogConstants;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * logistics G4 故障注入测试（A4-alert，设计文档 §5.2）。harness 第 7 域覆盖 logistics
 *（plan 2026-08-02-1500-1，P1-MA2-080 运费过账悬挂告警闭环）。
 *
 * <p>断言契约（设计文档 §4.2 G4）：
 * <ul>
 *   <li>A2+A4（告警闭环）：注入 throwingVoucherBiz 使 {@code voucherBiz.post} 抛异常后，
 *       {@code onDelivered} catch 块派发 captured event type = {@code log.freight-posting-failure}</li>
 *   <li>A1（一致性）：{@code freightSettlementStatus} 保持 PENDING（过账失败未误置 SETTLED）</li>
 *   <li>可恢复（非静默悬挂）：{@code onDelivered} 不向调用方抛异常（异常被 catch + 告警）</li>
 * </ul>
 *
 * <p>恢复路径：告警 + 期末试算平衡人工发现（logistics 保持 G4 分级，无 finance sweep 兜底）。
 *
 * <p>纯单元测试（无 IoC 容器）：{@code shipment.orgId=null} 使 {@code resolveAcctSchemaId} 经
 * {@link app.erp.md.dao.AcctSchemaResolver#resolvePrimarySchemaId} 的 orgId==null 早返回（不触及 daoProvider），
 * 对齐 {@code TestQaPostingFaultInjection} 的 materialId=null 范式。
 *
 * <p>测试包：{@code app.erp.log.service.processor}（与 {@link AbstractErpLogShipmentDeliveredProcessor} 同包，
 * 经 package-private field 赋值注入桩，对齐 {@code FaultInjectionStubs} 的 harness 契约）。
 */
public class TestLogPostingFaultInjection {

    @Test
    public void testFreightPostingFailureDispatchesAlert() {
        ErpLogShipmentScanForPollingProcessor processor = new ErpLogShipmentScanForPollingProcessor();
        processor.voucherBiz = FaultInjectionStubs.throwingVoucherBiz("post",
                FaultInjectionStubs.testFault("test.log-freight-posting-down"));
        String[] captured = new String[1];
        processor.notificationBiz = FaultInjectionStubs.recordingNotificationBiz(captured);

        ErpLogShipment shipment = new ErpLogShipment();
        shipment.setCode("SHP-FAIL-001");
        shipment.setRelatedBillType(ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY);
        shipment.setFreightSettlementStatus(ErpLogConstants.SETTLEMENT_STATUS_PENDING);
        shipment.setFreightAmount(new BigDecimal("150"));
        shipment.setOrgId(null);
        shipment.setCarrierId(null);

        IServiceContext context = new ServiceContextImpl();

        assertDoesNotThrow(() -> processor.onDelivered(shipment, context),
                "onDelivered 过账失败应被 catch（可恢复，非静默悬挂）");

        assertEquals(AbstractErpLogShipmentDeliveredProcessor.NOTIFY_EVENT_LOG_FREIGHT_POSTING_FAILURE,
                captured[0],
                "运费过账失败应派发 log.freight-posting-failure 告警（A2+A4）");
        assertEquals(ErpLogConstants.SETTLEMENT_STATUS_PENDING, shipment.getFreightSettlementStatus(),
                "运费过账失败后 freightSettlementStatus 保持 PENDING（A1 一致性）");
    }
}
