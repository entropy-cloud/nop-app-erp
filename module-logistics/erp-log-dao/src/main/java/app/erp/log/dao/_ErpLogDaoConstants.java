package app.erp.log.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpLogDaoConstants {
    
    /**
     * 承运商类型: 快递 
     */
    String CARRIER_TYPE_EXPRESS = "EXPRESS";
                    
    /**
     * 承运商类型: 货运 
     */
    String CARRIER_TYPE_FREIGHT = "FREIGHT";
                    
    /**
     * 承运商类型: 空运 
     */
    String CARRIER_TYPE_AIR = "AIR";
                    
    /**
     * 承运商类型: 海运 
     */
    String CARRIER_TYPE_SEA = "SEA";
                    
    /**
     * 承运商类型: 铁路 
     */
    String CARRIER_TYPE_RAIL = "RAIL";
                    
    /**
     * 运费条款: 预付 
     */
    String FREIGHT_TERMS_PREPAID = "PREPAID";
                    
    /**
     * 运费条款: 到付 
     */
    String FREIGHT_TERMS_COLLECT = "COLLECT";
                    
    /**
     * 发运单状态: 草稿 
     */
    String SHIPMENT_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 发运单状态: 已预约 
     */
    String SHIPMENT_STATUS_ADVISED = "ADVISED";
                    
    /**
     * 发运单状态: 已派发 
     */
    String SHIPMENT_STATUS_DISPATCHED = "DISPATCHED";
                    
    /**
     * 发运单状态: 运输中 
     */
    String SHIPMENT_STATUS_IN_TRANSIT = "IN_TRANSIT";
                    
    /**
     * 发运单状态: 已签收 
     */
    String SHIPMENT_STATUS_DELIVERED = "DELIVERED";
                    
    /**
     * 发运单状态: 已取消 
     */
    String SHIPMENT_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 运费结算状态: 待结算 
     */
    String SETTLEMENT_STATUS_PENDING = "PENDING";
                    
    /**
     * 运费结算状态: 已结算 
     */
    String SETTLEMENT_STATUS_SETTLED = "SETTLED";
                    
    /**
     * 网关操作类型: 下单 
     */
    String GATEWAY_ACTION_CREATE_SHIPMENT = "CREATE_SHIPMENT";
                    
    /**
     * 网关操作类型: 获取面单 
     */
    String GATEWAY_ACTION_GET_LABEL = "GET_LABEL";
                    
    /**
     * 网关操作类型: 预约取件 
     */
    String GATEWAY_ACTION_ADVISE = "ADVISE";
                    
    /**
     * 网关操作类型: 追踪 
     */
    String GATEWAY_ACTION_TRACK = "TRACK";
                    
    /**
     * 网关操作类型: 取消 
     */
    String GATEWAY_ACTION_CANCEL = "CANCEL";
                    
    /**
     * 单据状态: 草稿 
     */
    String DOC_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 单据状态: 已生效 
     */
    String DOC_STATUS_ACTIVE = "ACTIVE";
                    
    /**
     * 单据状态: 已作废 
     */
    String DOC_STATUS_CANCELLED = "CANCELLED";
                    
}
