package app.erp.log.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpLogDaoConstants {
    
    /**
     * 承运商类型: 快递 
     */
    int CARRIER_TYPE_EXPRESS = 10;
                    
    /**
     * 承运商类型: 货运 
     */
    int CARRIER_TYPE_FREIGHT = 20;
                    
    /**
     * 承运商类型: 空运 
     */
    int CARRIER_TYPE_AIR = 30;
                    
    /**
     * 承运商类型: 海运 
     */
    int CARRIER_TYPE_SEA = 40;
                    
    /**
     * 承运商类型: 铁路 
     */
    int CARRIER_TYPE_RAIL = 50;
                    
    /**
     * 运费条款: 预付 
     */
    int FREIGHT_TERMS_PREPAID = 10;
                    
    /**
     * 运费条款: 到付 
     */
    int FREIGHT_TERMS_COLLECT = 20;
                    
    /**
     * 发运单状态: 草稿 
     */
    int SHIPMENT_STATUS_DRAFT = 10;
                    
    /**
     * 发运单状态: 已预约 
     */
    int SHIPMENT_STATUS_ADVISED = 20;
                    
    /**
     * 发运单状态: 已派发 
     */
    int SHIPMENT_STATUS_DISPATCHED = 30;
                    
    /**
     * 发运单状态: 运输中 
     */
    int SHIPMENT_STATUS_IN_TRANSIT = 40;
                    
    /**
     * 发运单状态: 已签收 
     */
    int SHIPMENT_STATUS_DELIVERED = 50;
                    
    /**
     * 发运单状态: 已取消 
     */
    int SHIPMENT_STATUS_CANCELLED = 60;
                    
    /**
     * 运费结算状态: 待结算 
     */
    int SETTLEMENT_STATUS_PENDING = 10;
                    
    /**
     * 运费结算状态: 已结算 
     */
    int SETTLEMENT_STATUS_SETTLED = 20;
                    
    /**
     * 网关操作类型: 下单 
     */
    int GATEWAY_ACTION_CREATE_SHIPMENT = 10;
                    
    /**
     * 网关操作类型: 获取面单 
     */
    int GATEWAY_ACTION_GET_LABEL = 20;
                    
    /**
     * 网关操作类型: 预约取件 
     */
    int GATEWAY_ACTION_ADVISE = 30;
                    
    /**
     * 网关操作类型: 追踪 
     */
    int GATEWAY_ACTION_TRACK = 40;
                    
    /**
     * 网关操作类型: 取消 
     */
    int GATEWAY_ACTION_CANCEL = 50;
                    
    /**
     * 单据状态: 草稿 
     */
    int DOC_STATUS_DRAFT = 10;
                    
    /**
     * 单据状态: 已生效 
     */
    int DOC_STATUS_ACTIVE = 20;
                    
    /**
     * 单据状态: 已作废 
     */
    int DOC_STATUS_CANCELLED = 30;
                    
}
