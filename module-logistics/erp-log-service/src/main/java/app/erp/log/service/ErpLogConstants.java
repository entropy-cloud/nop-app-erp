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

    /** path-2 运费→到岸成本自动创建开关（默认 false，向后兼容）。plan 2026-07-11-2329-1。 */
    String CONFIG_PATH2_LANDED_COST_AUTO_CREATE = "erp-log.path2-landed-cost-auto-create";
}
