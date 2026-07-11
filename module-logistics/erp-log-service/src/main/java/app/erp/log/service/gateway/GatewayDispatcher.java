package app.erp.log.service.gateway;

import app.erp.log.dao.entity.ErpLogCarrier;
import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.log.dao.entity.ErpLogShipmentLog;
import app.erp.log.dao.entity.ErpLogShipmentParcel;
import app.erp.log.service.ErpLogConfigs;
import app.erp.log.service.ErpLogConstants;
import app.erp.log.service.ErpLogErrors;
import app.erp.log.service.spi.ErpLogCarrierGatewayRegistry;
import app.erp.log.service.spi.IErpLogCarrierGatewayClient;
import app.erp.log.service.spi.model.Address;
import app.erp.log.service.spi.model.DeliveryOrderRequest;
import app.erp.log.service.spi.model.DeliveryOrderResult;
import app.erp.log.service.spi.model.ParcelInfo;
import app.erp.log.service.spi.model.TrackingResult;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmEntitySet;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 承运商网关派发器。承载网关调用编排 + 运单状态机 ORM 操作（completeDeliveryOrder 重试/写回、trackShipment 轮询推进、
 * advise/cancelShipment 状态迁移、webhook 解析后的追踪推进），被 {@code ErpLogShipmentBizModel} 委托调用。
 *
 * <p><b>ORM 范式</b>：本类为 Facade 编排层（参 {@code InvPostingDispatcher}），在调用方 {@code @BizMutation} 事务内执行，
 * 一致使用 {@code dao.getEntityById} 加载 + {@code dao.saveOrUpdateEntity} 持久化（避免 requireEntity 与直接 dao 操作的 session 不一致）。
 *
 * <p><b>重试策略</b>：5xx/超时按 {@code erp-log.gateway-max-retries} 指数退避（{@code erp-log.retry-base-interval-secs}），
 * 4xx（认证错误）不重试。重试耗尽→死信：保留 ADVISED，错误写入 {@code remark} + {@link ErpLogShipmentLog}。
 */
public class GatewayDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(GatewayDispatcher.class);

    @Inject
    ErpLogCarrierGatewayRegistry gatewayRegistry;
    @Inject
    IDaoProvider daoProvider;

    /** DRAFT→ADVISED。幂等：已 ADVISED 直接返回。 */
    public ErpLogShipment advise(Long shipmentId) {
        ErpLogShipment shipment = loadShipment(shipmentId);
        String status = shipment.getStatus();
        if (!ErpLogConstants.SHIPMENT_STATUS_DRAFT.equals(status)
                && !ErpLogConstants.SHIPMENT_STATUS_ADVISED.equals(status)) {
            throw new NopException(ErpLogErrors.ERR_LOG_SHIPMENT_ILLEGAL_TRANSITION)
                    .param(ErpLogErrors.ARG_SHIPMENT_CODE, shipment.getCode())
                    .param(ErpLogErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpLogErrors.ARG_EXPECTED_STATUS, ErpLogConstants.SHIPMENT_STATUS_DRAFT);
        }
        if (!ErpLogConstants.SHIPMENT_STATUS_ADVISED.equals(status)) {
            shipment.setStatus(ErpLogConstants.SHIPMENT_STATUS_ADVISED);
            daoProvider.daoFor(ErpLogShipment.class).saveOrUpdateEntity(shipment);
        }
        return shipment;
    }

    /**
     * 承运商网关下单（completeDeliveryOrder）：ADVISED→DISPATCHED。
     * 幂等：已 DISPATCHED/IN_TRANSIT/DELIVERED/CANCELLED 直接返回。成功回写 trackingNo/labelUrl + 逐 parcel；
     * 失败死信保留 ADVISED + remark 错误。
     */
    public ErpLogShipment completeShipment(Long shipmentId, IServiceContext context) {
        ErpLogShipment shipment = loadShipment(shipmentId);
        String status = shipment.getStatus();
        if (ErpLogConstants.SHIPMENT_STATUS_DISPATCHED.equals(status)
                || ErpLogConstants.SHIPMENT_STATUS_DELIVERED.equals(status)
                || ErpLogConstants.SHIPMENT_STATUS_CANCELLED.equals(status)
                || ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT.equals(status)) {
            return shipment;
        }
        if (!ErpLogConstants.SHIPMENT_STATUS_ADVISED.equals(status)) {
            throw new NopException(ErpLogErrors.ERR_LOG_SHIPMENT_ILLEGAL_TRANSITION)
                    .param(ErpLogErrors.ARG_SHIPMENT_CODE, shipment.getCode())
                    .param(ErpLogErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpLogErrors.ARG_EXPECTED_STATUS, ErpLogConstants.SHIPMENT_STATUS_ADVISED);
        }

        IErpLogCarrierGatewayClient client = gatewayRegistry.getClient(shipment.getCarrierId(), context);
        DeliveryOrderRequest request = buildDeliveryOrderRequest(shipment);

        int maxRetries = AppConfig.var(ErpLogConfigs.CONFIG_GATEWAY_MAX_RETRIES,
                ErpLogConfigs.DEFAULT_GATEWAY_MAX_RETRIES);
        int[] intervals = parseRetryIntervals();

        NopException lastFailure = null;
        boolean retryable = false;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                DeliveryOrderResult result = client.completeDeliveryOrder(request);
                writeBackSuccess(shipment, result);
                writeLog(shipment, ErpLogConstants.GATEWAY_ACTION_COMPLETE_DELIVERY, request, result, 200, null, null, true);
                return shipment;
            } catch (NopException e) {
                lastFailure = e;
                retryable = isRetryable(e);
                if (!retryable || attempt >= maxRetries) {
                    break;
                }
                LOG.warn("承运商网关下单失败（第{}次重试），运单 {}：{}", attempt + 1, shipment.getCode(), e.getMessage());
                sleepSilently(intervals[Math.min(attempt, intervals.length - 1)]);
            }
        }

        deadLetter(shipment, ErpLogConstants.GATEWAY_ACTION_COMPLETE_DELIVERY, request, lastFailure, retryable);
        return shipment;
    }

    /** ADVISED/DISPATCHED/IN_TRANSIT/DRAFT→CANCELLED；DISPATCHED 以上经承运商取消。 */
    public ErpLogShipment cancelShipment(Long shipmentId, IServiceContext context) {
        ErpLogShipment shipment = loadShipment(shipmentId);
        String status = shipment.getStatus();
        if (ErpLogConstants.SHIPMENT_STATUS_CANCELLED.equals(status)
                || ErpLogConstants.SHIPMENT_STATUS_DELIVERED.equals(status)) {
            return shipment;
        }
        if (!ErpLogConstants.SHIPMENT_STATUS_ADVISED.equals(status)
                && !ErpLogConstants.SHIPMENT_STATUS_DISPATCHED.equals(status)
                && !ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT.equals(status)
                && !ErpLogConstants.SHIPMENT_STATUS_DRAFT.equals(status)) {
            throw new NopException(ErpLogErrors.ERR_LOG_SHIPMENT_ILLEGAL_TRANSITION)
                    .param(ErpLogErrors.ARG_SHIPMENT_CODE, shipment.getCode())
                    .param(ErpLogErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpLogErrors.ARG_EXPECTED_STATUS, ErpLogConstants.SHIPMENT_STATUS_ADVISED);
        }
        if (ErpLogConstants.SHIPMENT_STATUS_DISPATCHED.equals(status)
                || ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT.equals(status)) {
            gatewayRegistry.getClient(shipment.getCarrierId(), context)
                    .cancelShipment(shipment.getTrackingNo() != null ? shipment.getTrackingNo() : shipment.getCode());
            writeLog(shipment, ErpLogConstants.GATEWAY_ACTION_CANCEL, shipment.getCode(), null, 200, null, null, true);
        }
        shipment.setStatus(ErpLogConstants.SHIPMENT_STATUS_CANCELLED);
        daoProvider.daoFor(ErpLogShipment.class).saveOrUpdateEntity(shipment);
        return shipment;
    }

    /**
     * 单运单追踪推进（轮询/webhook 共用）。按 {@code carrierStatus} 迁移 DISPATCHED→IN_TRANSIT、IN_TRANSIT→DELIVERED。
     * 返回 true 表示状态迁移到新态。
     */
    public boolean advanceTracking(ErpLogShipment shipment, String carrierStatus, String signedBy) {
        String status = shipment.getStatus();
        if (ErpLogConstants.TRACKING_EVENT_DELIVERED.equals(carrierStatus)) {
            if (ErpLogConstants.SHIPMENT_STATUS_DELIVERED.equals(status)) {
                return false;
            }
            shipment.setStatus(ErpLogConstants.SHIPMENT_STATUS_DELIVERED);
            shipment.setActualDeliveryDate(CoreMetrics.today());
            if (signedBy != null) {
                shipment.setSignedBy(signedBy);
            }
            daoProvider.daoFor(ErpLogShipment.class).saveOrUpdateEntity(shipment);
            return true;
        }
        if (ErpLogConstants.TRACKING_EVENT_IN_TRANSIT.equals(carrierStatus)
                || ErpLogConstants.TRACKING_EVENT_PICKED_UP.equals(carrierStatus)) {
            if (ErpLogConstants.SHIPMENT_STATUS_DISPATCHED.equals(status)) {
                shipment.setStatus(ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT);
                daoProvider.daoFor(ErpLogShipment.class).saveOrUpdateEntity(shipment);
                return true;
            }
        }
        return false;
    }

    /** 轮询路径：调 client.trackShipment 取承运商侧状态后推进。返回是否迁移。 */
    public boolean advanceTrackingViaPolling(ErpLogShipment shipment, IServiceContext context) {
        if (shipment.getTrackingNo() == null) {
            return false;
        }
        IErpLogCarrierGatewayClient client = gatewayRegistry.getClient(shipment.getCarrierId(), context);
        TrackingResult result = client.trackShipment(shipment.getTrackingNo());
        boolean advanced = advanceTracking(shipment, result.getCurrentStatus(), null);
        writeLog(shipment, ErpLogConstants.GATEWAY_ACTION_TRACK, shipment.getTrackingNo(), result, 200, null, null, true);
        return advanced;
    }

    /**
     * 轮询兜底：对 DISPATCHED/IN_TRANSIT 运单调 trackShipment 推进。返回本轮状态发生迁移的运单列表
     * （BizModel 层据此对 DELIVERED 运单调用 {@code onDelivered} 完成 path-1/path-2 编排）。
     */
    public List<ErpLogShipment> scanForPolling(IServiceContext context) {
        IEntityDao<ErpLogShipment> dao = daoProvider.daoFor(ErpLogShipment.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("status", List.of(
                ErpLogConstants.SHIPMENT_STATUS_DISPATCHED,
                ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT)));
        q.setLimit(100);
        List<ErpLogShipment> candidates = dao.findAllByQuery(q);
        List<ErpLogShipment> advanced = new ArrayList<>();
        for (ErpLogShipment shipment : candidates) {
            if (advanceTrackingViaPolling(shipment, context)) {
                advanced.add(shipment);
            }
        }
        return advanced;
    }

    public ErpLogCarrier findCarrierByCode(String carrierCode) {
        IEntityDao<ErpLogCarrier> dao = daoProvider.daoFor(ErpLogCarrier.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", carrierCode));
        // O-5：追加 code 排序确保确定性
        q.addOrderField("code", false);
        return dao.findFirstByQuery(q);
    }

    public ErpLogShipment findShipmentByTrackingNo(String trackingNo) {
        if (trackingNo == null) {
            return null;
        }
        IEntityDao<ErpLogShipment> dao = daoProvider.daoFor(ErpLogShipment.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("trackingNo", trackingNo));
        // O-5：追加 trackingNo 排序确保确定性
        q.addOrderField("trackingNo", false);
        return dao.findFirstByQuery(q);
    }

    public void writeWebhookLog(ErpLogShipment shipment, String eventType, String payload, boolean advanced) {
        IEntityDao<ErpLogShipmentLog> dao = daoProvider.daoFor(ErpLogShipmentLog.class);
        ErpLogShipmentLog log = dao.newEntity();
        log.setShipmentId(shipment.getId());
        log.setGatewayId(resolveGatewayId(shipment));
        log.setActionType(ErpLogConstants.GATEWAY_ACTION_TRACK);
        log.setRequestBody(payload);
        log.setHttpStatus(200);
        log.setIsSuccess(advanced ? Boolean.TRUE : Boolean.FALSE);
        log.setErrorCode(eventType);
        log.setExecutedAt(CoreMetrics.currentDateTime());
        dao.saveEntity(log);
    }

    public ErpLogShipment loadShipment(Long shipmentId) {
        IEntityDao<ErpLogShipment> dao = daoProvider.daoFor(ErpLogShipment.class);
        ErpLogShipment shipment = dao.getEntityById(shipmentId);
        if (shipment == null) {
            throw new NopException(ErpLogErrors.ERR_LOG_SHIPMENT_ILLEGAL_TRANSITION)
                    .param(ErpLogErrors.ARG_SHIPMENT_ID, shipmentId);
        }
        return shipment;
    }

    public ErpLogShipment saveShipment(ErpLogShipment shipment) {
        daoProvider.daoFor(ErpLogShipment.class).saveOrUpdateEntity(shipment);
        return shipment;
    }

    // ---------- helpers ----------

    private DeliveryOrderRequest buildDeliveryOrderRequest(ErpLogShipment shipment) {
        DeliveryOrderRequest request = new DeliveryOrderRequest();
        request.setShipmentCode(shipment.getCode());
        request.setReferenceNo(shipment.getCode());
        request.setSender(toAddress(shipment.getSenderName(), shipment.getSenderPhone(),
                shipment.getSenderAddress(), null, null, null, null));
        request.setReceiver(toAddress(shipment.getReceiverName(), shipment.getReceiverPhone(),
                shipment.getReceiverAddress(), shipment.getReceiverCountry(), shipment.getReceiverProvince(),
                shipment.getReceiverCity(), shipment.getReceiverDistrict()));

        List<ParcelInfo> parcels = new ArrayList<>();
        IOrmEntitySet<ErpLogShipmentParcel> rel = shipment.getParcels();
        if (rel != null) {
            for (ErpLogShipmentParcel p : rel) {
                ParcelInfo info = new ParcelInfo();
                info.setParcelNo(p.getParcelNo());
                info.setWeight(p.getWeight());
                info.setLength(p.getLength());
                info.setWidth(p.getWidth());
                info.setHeight(p.getHeight());
                info.setDeclaredValue(p.getDeclaredValue());
                parcels.add(info);
            }
        }
        request.setParcels(parcels);
        return request;
    }

    private Address toAddress(String name, String phone, String detail, String country, String province,
                              String city, String district) {
        Address addr = new Address();
        addr.setName(name);
        addr.setPhone(phone);
        addr.setDetail(detail);
        addr.setCountry(country);
        addr.setProvince(province);
        addr.setCity(city);
        addr.setDistrict(district);
        return addr;
    }

    private void writeBackSuccess(ErpLogShipment shipment, DeliveryOrderResult result) {
        shipment.setTrackingNo(result.getTrackingNo());
        shipment.setLabelUrl(result.getLabelUrl());
        if (result.getEstimatedDelivery() != null) {
            shipment.setEstimatedDeliveryDate(result.getEstimatedDelivery());
        }
        shipment.setStatus(ErpLogConstants.SHIPMENT_STATUS_DISPATCHED);
        IOrmEntitySet<ErpLogShipmentParcel> rel = shipment.getParcels();
        if (rel != null) {
            IEntityDao<ErpLogShipmentParcel> dao = daoProvider.daoFor(ErpLogShipmentParcel.class);
            for (ErpLogShipmentParcel p : rel) {
                p.setTrackingNo(result.getTrackingNo());
                p.setLabelUrl(result.getLabelUrl());
                dao.saveOrUpdateEntity(p);
            }
        }
        daoProvider.daoFor(ErpLogShipment.class).saveOrUpdateEntity(shipment);
    }

    private void deadLetter(ErpLogShipment shipment, String actionType, Object request,
                            NopException failure, boolean retryable) {
        String errMsg = (retryable ? "[网关重试耗尽] " : "[网关不可重试错误] ") + failure.getMessage();
        shipment.setRemark(errMsg);
        daoProvider.daoFor(ErpLogShipment.class).saveOrUpdateEntity(shipment);
        String code = retryable ? "GATEWAY_RETRY_EXHAUSTED" : "GATEWAY_NON_RETRYABLE";
        writeLog(shipment, actionType, request, null, httpStatusOf(failure), code, errMsg, false);
        LOG.error("承运商网关下单死信，运单 {} 保留 ADVISED：{}", shipment.getCode(), errMsg);
    }

    private void writeLog(ErpLogShipment shipment, String actionType, Object request, Object response,
                          int httpStatus, String errorCode, String errorMessage, boolean success) {
        IEntityDao<ErpLogShipmentLog> dao = daoProvider.daoFor(ErpLogShipmentLog.class);
        ErpLogShipmentLog log = dao.newEntity();
        log.setShipmentId(shipment.getId());
        log.setGatewayId(resolveGatewayId(shipment));
        log.setActionType(actionType);
        log.setRequestBody(request != null ? request.toString() : null);
        log.setResponseBody(response != null ? response.toString() : null);
        log.setHttpStatus(httpStatus);
        log.setErrorCode(errorCode != null && errorCode.length() > 100 ? errorCode.substring(0, 100) : errorCode);
        log.setErrorMessage(errorMessage);
        log.setIsSuccess(success ? Boolean.TRUE : Boolean.FALSE);
        log.setExecutedAt(CoreMetrics.currentDateTime());
        dao.saveEntity(log);
    }

    private String resolveGatewayId(ErpLogShipment shipment) {
        if (shipment.getCarrierId() == null) {
            return null;
        }
        ErpLogCarrier carrier = daoProvider.daoFor(ErpLogCarrier.class).getEntityById(shipment.getCarrierId());
        return carrier != null ? carrier.getGatewayId() : null;
    }

    private boolean isRetryable(NopException e) {
        Object httpStatus = e.getParam("httpStatus");
        if (httpStatus instanceof Number) {
            int code = ((Number) httpStatus).intValue();
            return code >= 500 || code == 408;
        }
        return false;
    }

    private int httpStatusOf(NopException e) {
        Object httpStatus = e.getParam("httpStatus");
        if (httpStatus instanceof Number) {
            return ((Number) httpStatus).intValue();
        }
        return 500;
    }

    private int[] parseRetryIntervals() {
        String raw = AppConfig.var(ErpLogConfigs.CONFIG_RETRY_BASE_INTERVAL_SECS,
                ErpLogConfigs.DEFAULT_RETRY_INTERVAL_SECS);
        String[] parts = raw.split(",");
        int[] ints = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                ints[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException ex) {
                ints[i] = 30;
            }
        }
        return ints;
    }

    private void sleepSilently(int seconds) {
        // 测试友好：retry-base-interval-secs 配 0 时即不阻塞。
        try {
            Thread.sleep(Math.min(Math.max(seconds, 0), 60) * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
