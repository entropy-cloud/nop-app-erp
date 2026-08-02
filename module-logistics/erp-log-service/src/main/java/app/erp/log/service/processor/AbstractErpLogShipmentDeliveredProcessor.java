package app.erp.log.service.processor;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.inv.biz.IErpInvLandedCostBiz;
import app.erp.inv.dao.entity.ErpInvLandedCost;
import app.erp.log.dao.entity.ErpLogCarrier;
import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.log.service.ErpLogConfigs;
import app.erp.log.service.ErpLogConstants;
import app.erp.log.service.ErpLogErrors;
import app.erp.log.service.event.ShipmentDeliveredEvent;
import app.erp.log.service.gateway.GatewayDispatcher;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ErpLogShipment DELIVERED 后运费过账/到岸成本编排共享基类（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 *
 * <p>持有 {@link #onDelivered} 编排链（path-1 运费过账 + path-2 到岸成本自动创建），供
 * {@code ErpLogShipmentHandleTrackingWebhookProcessor} 与 {@code ErpLogShipmentScanForPollingProcessor}
 * 两个 per-mutation Processor 复用，避免大段重复（参 R6.7 helper 归属裁决：同实体多 mutation 共享 helper
 * 抽到域专属基类，仅当重复显著时）。下游可经 Delta beans.xml 同名 bean id 覆盖子类后逐个覆盖 protected step。
 */
abstract class AbstractErpLogShipmentDeliveredProcessor {
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractErpLogShipmentDeliveredProcessor.class);

    static final String NOTIFY_EVENT_LOG_FREIGHT_POSTING_FAILURE = "log.freight-posting-failure";

    @Inject
    IDaoProvider daoProvider;

    @Inject
    GatewayDispatcher gatewayDispatcher;

    @Inject
    IErpFinVoucherBiz voucherBiz;

    @Inject
    IErpInvLandedCostBiz landedCostBiz;

    @Inject
    IErpSysNotificationBiz notificationBiz;

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
            // G4 错误传播分级（plan 2026-08-02-1500-1 P1-MA2-080）：logistics 无 finance sweep 兜底，
            // 失败派发告警使运费过账悬挂可被感知（对齐 MaintenanceLaborPostingDispatcher.dispatchFailureAlert 范式）。
            dispatchFreightFailureAlert(shipment, e);
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

    /** 运费过账失败告警派发（G4；通知失败降级不阻断主流程，对齐 GatewayDispatcher.dispatchDeadLetterAlert 范式）。 */
    protected void dispatchFreightFailureAlert(ErpLogShipment shipment, Exception cause) {
        if (notificationBiz == null) {
            return;
        }
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("shipmentCode", shipment.getCode());
        ctx.put("carrierId", shipment.getCarrierId());
        ctx.put("relatedBillCode", shipment.getRelatedBillCode());
        ctx.put("errorCode", cause instanceof NopException ? ((NopException) cause).getErrorCode() : cause.getClass().getName());
        ctx.put("errorMessage", cause.getMessage());
        ctx.put("postingNo", shipment.getCode());
        IServiceContext serviceCtx = new ServiceContextImpl();
        try {
            notificationBiz.notify(NOTIFY_EVENT_LOG_FREIGHT_POSTING_FAILURE, ctx, serviceCtx);
        } catch (Exception notifyErr) {
            LOG.warn("运费过账失败告警派发失败（降级）：shipmentCode={}, reason={}",
                    shipment.getCode(), notifyErr.getMessage());
        }
    }

    protected PostingEvent buildFreightPostingEvent(ErpLogShipment shipment) {
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
    protected Long resolveAcctSchemaId(Long orgId) {
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
    }

    protected Long resolveCarrierPartnerId(ErpLogShipment shipment) {
        if (shipment.getCarrierId() == null) {
            return null;
        }
        ErpLogCarrier carrier = shipment.getCarrier();
        return carrier != null ? carrier.getPartnerId() : null;
    }

    protected ErpLogShipment markSettled(ErpLogShipment shipment) {
        shipment.setFreightSettlementStatus(ErpLogConstants.SETTLEMENT_STATUS_SETTLED);
        return shipment;
    }

    protected void publishDeliveredEvent(ErpLogShipment shipment) {
        ShipmentDeliveredEvent evt = new ShipmentDeliveredEvent(shipment.getId(), shipment.getCode(),
                shipment.getRelatedBillType(), shipment.getRelatedBillCode(), shipment.getCarrierId(),
                shipment.getFreightAmount(), shipment.getFreightCurrencyId());
        LOG.info("ShipmentDeliveredEvent 发布（path-2 采购运费交接）：{}", evt.getShipmentCode());
    }

    protected IEntityDao<ErpLogShipment> dao() {
        return daoProvider.daoFor(ErpLogShipment.class);
    }
}
