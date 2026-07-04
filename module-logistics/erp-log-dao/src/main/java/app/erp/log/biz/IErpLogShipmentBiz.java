
package app.erp.log.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.log.dao.entity.ErpLogShipment;

public interface IErpLogShipmentBiz extends ICrudBiz<ErpLogShipment>{

    /**
     * 确认发运：DRAFT→ADVISED。预约承运商（{@code client.adviseShipment} 由异步 worker 派发，
     * nop-job 注册为部署 follow-up）。幂等键 {@code referenceNo}=运单号。
     */
    @BizMutation
    ErpLogShipment advise(@Name("shipmentId") Long shipmentId, IServiceContext context);

    /**
     * 承运商网关下单（completeDeliveryOrder）：ADVISED→DISPATCHED。重试（5xx/超时指数退避，4xx 不重试），
     * 死信保留 ADVISED。供异步 worker / 手动重试 / 测试调用（幂等：已 DISPATCHED 直接返回）。
     */
    @BizMutation
    ErpLogShipment completeShipment(@Name("shipmentId") Long shipmentId, IServiceContext context);

    /**
     * 取消发运：ADVISED/DISPATCHED→CANCELLED（经 {@code client.cancelShipment}，承运商不支持则标记本地取消）。
     */
    @BizMutation
    ErpLogShipment cancelShipment(@Name("shipmentId") Long shipmentId, IServiceContext context);

    /**
     * 承运商追踪回调入口（webhook）。HMAC-SHA256 校验（{@code erp-log.webhook-signature-required}）；
     * 按 event 推进 IN_TRANSIT/DELIVERED；幂等；DELIVERED 触发运费过账入口。
     */
    @BizMutation
    ErpLogShipment handleTrackingWebhook(@Name("carrierCode") String carrierCode,
                                         @Name("signature") String signature,
                                         @Name("payload") String payload,
                                         IServiceContext context);

    /**
     * 追踪轮询兜底（{@code erp-log.tracking-poll-cron}）：对 DISPATCHED 未 DELIVERED 运单调
     * {@code client.trackShipment} 推进。返回本轮推进的运单数。
     */
    @BizMutation
    int scanForPolling(IServiceContext context);
}
