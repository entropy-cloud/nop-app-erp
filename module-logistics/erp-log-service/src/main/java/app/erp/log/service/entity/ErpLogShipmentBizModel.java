
package app.erp.log.service.entity;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.inv.biz.IErpInvLandedCostBiz;
import app.erp.inv.dao.entity.ErpInvLandedCost;
import app.erp.log.biz.IErpLogShipmentBiz;
import app.erp.log.dao.entity.ErpLogCarrier;
import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.log.service.ErpLogConfigs;
import app.erp.log.service.ErpLogConstants;
import app.erp.log.service.ErpLogErrors;
import app.erp.log.service.event.ShipmentDeliveredEvent;
import app.erp.log.service.gateway.GatewayDispatcher;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.core.lang.json.JsonTool;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import io.nop.biz.crud.EntityData;

/**
 * 发运单聚合根 Biz。CRUD 之外承载运单状态机与网关集成动作（advise/completeShipment/cancelShipment/
 * handleTrackingWebhook/scanForPolling），网关调用 + ORM 编排委托 {@link GatewayDispatcher}（参 InvPostingDispatcher 范式：
 * 一致 dao.getEntityById + dao.saveOrUpdateEntity，避免 requireEntity 与直接 dao 操作的 session 不一致）。
 *
 * <p>状态机（{@code state-machine.md §1/§2}）：DRAFT→ADVISED（{@link #advise}）→DISPATCHED
 * （{@link GatewayDispatcher#completeShipment}）→IN_TRANSIT→DELIVERED（webhook {@link #handleTrackingWebhook}
 * 或轮询 {@link #scanForPolling}）；CANCELLED 终态（{@link #cancelShipment}）。
 *
 * <p>DELIVERED 触发运费过账（path-1，{@link #onDelivered}，3.18 wiring）或到岸成本自动创建（path-2，
 * config-gated {@code erp-log.path2-landed-cost-auto-create}，plan 2026-07-11-2329-1）。
 */
@BizModel("ErpLogShipment")
public class ErpLogShipmentBizModel extends CrudBizModel<ErpLogShipment> implements IErpLogShipmentBiz {
    private static final Logger LOG = LoggerFactory.getLogger(ErpLogShipmentBizModel.class);

    public ErpLogShipmentBizModel() {
        setEntityName(ErpLogShipment.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpLogShipment> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpLogShipment entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
    }


    @Inject
    GatewayDispatcher gatewayDispatcher;
    @Inject
    IErpFinVoucherBiz voucherBiz;
    @Inject
    IErpInvLandedCostBiz landedCostBiz;

    @Override
    @BizMutation
    @SingleSession
    public ErpLogShipment advise(@Name("shipmentId") Long shipmentId, IServiceContext context) {
        return gatewayDispatcher.advise(shipmentId);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpLogShipment completeShipment(@Name("shipmentId") Long shipmentId, IServiceContext context) {
        return gatewayDispatcher.completeShipment(shipmentId, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpLogShipment cancelShipment(@Name("shipmentId") Long shipmentId, IServiceContext context) {
        return gatewayDispatcher.cancelShipment(shipmentId, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpLogShipment handleTrackingWebhook(@Name("carrierCode") String carrierCode,
                                                @Name("signature") String signature,
                                                @Name("payload") String payload,
                                                IServiceContext context) {
        ErpLogCarrier carrier = gatewayDispatcher.findCarrierByCode(carrierCode);
        if (carrier == null) {
            throw new NopException(ErpLogErrors.ERR_LOG_GATEWAY_NOT_REGISTERED)
                    .param(ErpLogErrors.ARG_CARRIER_CODE, carrierCode);
        }
        String secret = resolveWebhookSecret(carrier);

        boolean required = AppConfig.var(ErpLogConfigs.CONFIG_WEBHOOK_SIGNATURE_REQUIRED,
                ErpLogConfigs.DEFAULT_WEBHOOK_SIGNATURE_REQUIRED);
        if (required && !verifySignature(payload, signature, secret)) {
            throw new NopException(ErpLogErrors.ERR_LOG_WEBHOOK_SIGNATURE_INVALID)
                    .param(ErpLogErrors.ARG_CARRIER_CODE, carrierCode);
        }

        Map<String, Object> event = parsePayload(payload);
        String trackingNo = asString(event.get("trackingNo"));
        String eventType = asString(event.get("eventType"));
        String signedBy = asString(event.get("signedBy"));

        ErpLogShipment shipment = gatewayDispatcher.findShipmentByTrackingNo(trackingNo);
        if (shipment == null) {
            return null;
        }
        // 幂等：advanceTracking 已 DELIVERED 返回 false；同 event 重复不重复推进。
        boolean advanced = gatewayDispatcher.advanceTracking(shipment, eventType, signedBy);
        gatewayDispatcher.writeWebhookLog(shipment, eventType, payload, advanced);
        if (advanced && ErpLogConstants.TRACKING_EVENT_DELIVERED.equals(eventType)) {
            onDelivered(shipment, context);
        }
        return shipment;
    }

    @Override
    @BizMutation
    @SingleSession
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

    /**
     * DELIVERED 触发运费过账/到岸成本编排入口（3.18 wiring + plan 2026-07-11-2329-1 path-2 升级）。
     * <p>按 {@code relatedBillType} 分流：
     * <ul>
     *   <li>{@code SALES_DELIVERY}（path-1）：构建携带 {@code businessType=FREIGHT} + billData 的 {@link PostingEvent}，
     *       调 {@link IErpFinVoucherBiz#post}（直接调用范式参 {@code InvPostingExecutor}，非设计文档原描述的"finance 订阅事件"模型）。
     *       {@code erp-log.shipment-settlement-mode=AUTO}（默认）才调 post；MANUAL 仅标记待处理。</li>
     *   <li>{@code PURCHASE_RECEIPT}（path-2）：config-gated {@code erp-log.path2-landed-cost-auto-create}（默认 false）。
     *       若开启且 {@code freightAmount > 0}，调 {@link IErpInvLandedCostBiz#generateFreightLandedCost} 自动创建 DRAFT
     *       到岸成本单（FREIGHT 费用行）；成功 → mark SETTLED；失败 → 保持 PENDING（对齐 path-1 可重试语义）。
     *       若 {@code freightAmount} 为 null 或 ≤ 0 → mark SETTLED（无可分摊运费）。
     *       config 关闭时 → 发布 {@link ShipmentDeliveredEvent} 占位 + mark SETTLED（向后兼容）。</li>
     * </ul>
     * 成功 {@code freightSettlementStatus} PENDING→SETTLED；已 SETTLED 幂等抛 {@link ErpLogErrors#ERR_LOG_SHIPMENT_ALREADY_DELIVERED}。
     */
    protected void onDelivered(ErpLogShipment shipment, IServiceContext context) {
        if (ErpLogConstants.SETTLEMENT_STATUS_SETTLED.equals(shipment.getFreightSettlementStatus())) {
            throw new NopException(ErpLogErrors.ERR_LOG_SHIPMENT_ALREADY_DELIVERED)
                    .param(ErpLogErrors.ARG_SHIPMENT_CODE, shipment.getCode());
        }
        String relatedBillType = shipment.getRelatedBillType();

        if (ErpLogConstants.RELATED_BILL_TYPE_PURCHASE_RECEIPT.equals(relatedBillType)) {
            handlePurchaseReceiptDelivered(shipment, context);
            return;
        }

        // path-1 默认（SALES_DELIVERY 及其他）：运费过账
        String mode = AppConfig.var(ErpLogConfigs.CONFIG_SHIPMENT_SETTLEMENT_MODE,
                ErpLogConfigs.SETTLEMENT_MODE_AUTO);
        if (ErpLogConfigs.SETTLEMENT_MODE_MANUAL.equals(mode)) {
            LOG.info("运费结算模式=MANUAL，运单 {} DELIVERED 后标记待人工处理", shipment.getCode());
            return;
        }
        PostingEvent event = buildFreightPostingEvent(shipment);
        try {
            Long voucherId = voucherBiz.post(event, context);
            if (voucherId != null) {
                gatewayDispatcher.saveShipment(markSettled(shipment));
            }
        } catch (Exception e) {
            // 过账失败不阻塞 DELIVERED 终态：保持 PENDING，由兜底扫描重试（参 InvPostingDispatcher 失败语义）。
            if (e instanceof NopException) {
                LOG.warn("运费过账失败，运单 {} 保持 DELIVERED、freightSettlementStatus=PENDING：{}",
                        shipment.getCode(), e.getMessage());
            } else {
                LOG.error("运费过账异常，运单 {} 保持 DELIVERED、freightSettlementStatus=PENDING", shipment.getCode(), e);
            }
        }
    }

    /**
     * path-2 采购运费→到岸成本编排（plan 2026-07-11-2329-1）。
     *
     * <p>config-gated {@code erp-log.path2-landed-cost-auto-create}（默认 false）：
     * <ul>
     *   <li>开启且 freightAmount > 0 → 调 {@code generateFreightLandedCost} 创建 DRAFT；成功 mark SETTLED，失败保持 PENDING</li>
     *   <li>开启但 freightAmount ≤ 0/null → mark SETTLED（无可分摊运费）</li>
     *   <li>关闭 → 发布事件占位 + mark SETTLED（向后兼容）</li>
     * </ul>
     */
    protected void handlePurchaseReceiptDelivered(ErpLogShipment shipment, IServiceContext context) {
        boolean autoCreate = AppConfig.var(ErpLogConstants.CONFIG_PATH2_LANDED_COST_AUTO_CREATE, false);
        BigDecimal freightAmount = shipment.getFreightAmount();

        if (!autoCreate) {
            publishDeliveredEvent(shipment);
            gatewayDispatcher.saveShipment(markSettled(shipment));
            return;
        }

        if (freightAmount == null || freightAmount.compareTo(BigDecimal.ZERO) <= 0) {
            LOG.info("运单 {} PURCHASE_RECEIPT DELIVERED 但 freightAmount={}≤0，无 path-2 职责，标记 SETTLED",
                    shipment.getCode(), freightAmount);
            gatewayDispatcher.saveShipment(markSettled(shipment));
            return;
        }

        try {
            ErpInvLandedCost landedCost = landedCostBiz.generateFreightLandedCost(
                    shipment.getRelatedBillCode(), freightAmount,
                    shipment.getFreightCurrencyId(), null, context);
            LOG.info("path-2 自动创建到岸成本单 {}：运单 {} / 采购入库单 {} / 运费 {}",
                    landedCost.getCode(), shipment.getCode(), shipment.getRelatedBillCode(), freightAmount);
            publishDeliveredEvent(shipment);
            gatewayDispatcher.saveShipment(markSettled(shipment));
        } catch (Exception e) {
            // path-2 失败保持 PENDING，允许 scanForPolling/webhook 重入重试（对齐 path-1 失败语义）
            if (e instanceof NopException) {
                LOG.error("path-2 到岸成本自动创建失败，运单 {} 保持 PENDING：{}",
                        shipment.getCode(), e.getMessage());
            } else {
                LOG.error("path-2 到岸成本自动创建异常，运单 {} 保持 PENDING", shipment.getCode(), e);
            }
        }
    }

    private PostingEvent buildFreightPostingEvent(ErpLogShipment shipment) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.FREIGHT);
        event.setBillHeadCode(shipment.getCode());
        event.setOrgId(shipment.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(shipment.getOrgId()));
        event.setCurrencyId(shipment.getFreightCurrencyId());
        event.setExchangeRate(BigDecimal.ONE);
        event.setVoucherDate(CoreMetrics.today());

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpLogConstants.BILL_DATA_FREIGHT_AMOUNT, shipment.getFreightAmount());
        billData.put(ErpLogConstants.BILL_DATA_FREIGHT_CURRENCY_ID, shipment.getFreightCurrencyId());
        billData.put(ErpLogConstants.BILL_DATA_RELATED_BILL_TYPE, shipment.getRelatedBillType());
        billData.put(ErpLogConstants.BILL_DATA_FREIGHT_TERMS, shipment.getFreightTerms());
        billData.put(ErpLogConstants.BILL_DATA_SHIPPER_ID, shipment.getShipperId());
        billData.put(ErpLogConstants.BILL_DATA_CARRIER_PARTNER_ID, resolveCarrierPartnerId(shipment));
        event.setBillData(billData);
        return event;
    }

    /** 按 orgId 解析默认账套（运单不携带 acctSchemaId，由组织主数据解析）。 */
    @SuppressWarnings("unchecked")
    private Long resolveAcctSchemaId(Long orgId) {
        if (orgId == null) {
            return null;
        }
        IEntityDao<app.erp.md.dao.entity.ErpMdAcctSchema> dao =
                daoProvider().daoFor(app.erp.md.dao.entity.ErpMdAcctSchema.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orgId", orgId));
        // O-5：追加 code 排序确保确定性
        q.addOrderField("code", false);
        app.erp.md.dao.entity.ErpMdAcctSchema schema = dao.findFirstByQuery(q);
        return schema != null ? schema.getId() : null;
    }

    private Long resolveCarrierPartnerId(ErpLogShipment shipment) {
        if (shipment.getCarrierId() == null) {
            return null;
        }
        ErpLogCarrier carrier = daoProvider().daoFor(ErpLogCarrier.class).getEntityById(shipment.getCarrierId());
        return carrier != null ? carrier.getPartnerId() : null;
    }

    private ErpLogShipment markSettled(ErpLogShipment shipment) {
        shipment.setFreightSettlementStatus(ErpLogConstants.SETTLEMENT_STATUS_SETTLED);
        return shipment;
    }

    private void publishDeliveredEvent(ErpLogShipment shipment) {
        ShipmentDeliveredEvent evt = new ShipmentDeliveredEvent(shipment.getId(), shipment.getCode(),
                shipment.getRelatedBillType(), shipment.getRelatedBillCode(), shipment.getCarrierId(),
                shipment.getFreightAmount(), shipment.getFreightCurrencyId());
        LOG.info("ShipmentDeliveredEvent 发布（path-2 采购运费交接）：{}", evt.getShipmentCode());
    }

    /** webhook HMAC 密钥：取承运商编码（mock 测试可控；真实部署可扩展 credentials JSON 内 webhookSecret）。 */
    private String resolveWebhookSecret(ErpLogCarrier carrier) {
        return carrier.getCode();
    }

    private boolean verifySignature(String payload, String signature, String secret) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            String expected = sb.toString();
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    signature.toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String payload) {
        return (Map<String, Object>) JsonTool.parseNonStrict(payload);
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
