package app.erp.log.service;

/**
 * 物流域状态码与常量。权威值来自 {@code module-logistics/model/app-erp-logistics.orm.xml} 关联字典。
 */
public interface ErpLogConstants {

    // ---- shipment-status（erp-log/shipment-status） ----
    String SHIPMENT_STATUS_DRAFT = "DRAFT";
    String SHIPMENT_STATUS_ADVISED = "ADVISED";
    String SHIPMENT_STATUS_DISPATCHED = "DISPATCHED";
    String SHIPMENT_STATUS_IN_TRANSIT = "IN_TRANSIT";
    String SHIPMENT_STATUS_DELIVERED = "DELIVERED";
    String SHIPMENT_STATUS_CANCELLED = "CANCELLED";

    // ---- settlement-status（erp-log/settlement-status） ----
    String SETTLEMENT_STATUS_PENDING = "PENDING";
    String SETTLEMENT_STATUS_SETTLED = "SETTLED";

    // ---- freight-terms（erp-log/freight-terms） ----
    String FREIGHT_TERMS_PREPAID = "PREPAID";
    String FREIGHT_TERMS_COLLECT = "COLLECT";

    // ---- gateway-action（erp-log/gateway-action，列长 20，值须 ≤20 字符） ----
    String GATEWAY_ACTION_ADVISE = "ADVISE_SHIPMENT";
    String GATEWAY_ACTION_COMPLETE_DELIVERY = "COMPLETE_DELIVERY";
    String GATEWAY_ACTION_GET_LABEL = "GET_LABEL";
    String GATEWAY_ACTION_TRACK = "TRACK";
    String GATEWAY_ACTION_CANCEL = "CANCEL";
    String GATEWAY_ACTION_RATE_QUOTE = "RATE_QUOTE";

    // ---- relatedBillType（弱指针关联单据类型） ----
    String RELATED_BILL_TYPE_SALES_DELIVERY = "SALES_DELIVERY";
    String RELATED_BILL_TYPE_PURCHASE_RECEIPT = "PURCHASE_RECEIPT";

    // ---- webhook 回调事件类型 ----
    String TRACKING_EVENT_PICKED_UP = "PICKED_UP";
    String TRACKING_EVENT_IN_TRANSIT = "IN_TRANSIT";
    String TRACKING_EVENT_DELIVERED = "DELIVERED";

    // ---- PostingEvent.billData 键（运费过账） ----
    String BILL_DATA_FREIGHT_AMOUNT = "FREIGHT_AMOUNT";
    String BILL_DATA_FREIGHT_CURRENCY_ID = "FREIGHT_CURRENCY_ID";
    String BILL_DATA_RELATED_BILL_TYPE = "RELATED_BILL_TYPE";
    String BILL_DATA_FREIGHT_TERMS = "FREIGHT_TERMS";
    String BILL_DATA_SHIPPER_ID = "SHIPPER_ID";
    String BILL_DATA_CARRIER_PARTNER_ID = "CARRIER_PARTNER_ID";

    /** mock 承运商网关标识。 */
    String GATEWAY_ID_MOCK = "mock";

    // ---- notify 事件（跨域通知派发子系统） ----
    /** DRAFT 超阈值升级通知事件（RC-R1.37，P1-RC-084，UC-LOG-01；USER_LIST ${submitterUserId} 接收人）。 */
    String NOTIFY_EVENT_DRAFT_ESCALATION = "log.draft-escalation";

    /** path-2 运费→到岸成本自动创建开关（默认 false，向后兼容）。plan 2026-07-11-2329-1。 */
    String CONFIG_PATH2_LANDED_COST_AUTO_CREATE = "erp-log.path2-landed-cost-auto-create";

    // ---- RC-R1.84（P1-RC-086，UC-LOG-07）：配送窗口容量预约 ----

    // ---- booking-status（erp-log/booking-status） ----
    String BOOKING_STATUS_BOOKED = "BOOKED";
    String BOOKING_STATUS_CONFIRMED = "CONFIRMED";
    String BOOKING_STATUS_ARRIVED = "ARRIVED";
    String BOOKING_STATUS_MISSED = "MISSED";
    String BOOKING_STATUS_CANCELLED = "CANCELLED";

    /** 爽约费系统参数键（L1 UC-LOG-07「爽约费金额从系统参数配置读取」，默认 0）。 */
    String CONFIG_BOOKING_MISSED_FEE = "erp-log.booking-missed-fee";

    /** 爽约后优先级评分提升步长（获得优先重新预约权）。 */
    int BOOKING_MISSED_PRIORITY_SCORE_STEP = 10;

    // ---- RC-R1.85（P1-RC-087，UC-LOG-06 步骤 5）：交付状态回写 sales ----

    /**
     * sales 订单发货进度终值（erp-sal/delivery-status 字典 DELIVERED，常量本体位于 sal-service 不入 dao 层，
     * 此处按值约定镜像，回写经 IErpSalOrderBiz.updateDeliveryStatus 承载）。
     */
    String SALES_DELIVERY_STATUS_DELIVERED = "DELIVERED";
}
