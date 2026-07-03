package app.erp.drp.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpDrpDaoConstants {
    
    /**
     * DRP计划状态: 草稿 
     */
    String DRP_PLAN_STATUS_DRAFT = "DRAFT";
                    
    /**
     * DRP计划状态: 已计算 
     */
    String DRP_PLAN_STATUS_COMPUTED = "COMPUTED";
                    
    /**
     * DRP计划状态: 已批准 
     */
    String DRP_PLAN_STATUS_APPROVED = "APPROVED";
                    
    /**
     * DRP计划状态: 已执行 
     */
    String DRP_PLAN_STATUS_EXECUTED = "EXECUTED";
                    
    /**
     * DRP明细行状态: 建议 
     */
    String DRP_LINE_STATUS_SUGGESTED = "SUGGESTED";
                    
    /**
     * DRP明细行状态: 已批准 
     */
    String DRP_LINE_STATUS_APPROVED = "APPROVED";
                    
    /**
     * DRP明细行状态: 已下单 
     */
    String DRP_LINE_STATUS_ORDERED = "ORDERED";
                    
    /**
     * DRP明细行状态: 已取消 
     */
    String DRP_LINE_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 补货类型: 仓间调拨 
     */
    String DRP_REPLENISHMENT_TYPE_TRANSFER = "TRANSFER";
                    
    /**
     * 补货类型: 向供应商采购 
     */
    String DRP_REPLENISHMENT_TYPE_PURCHASE = "PURCHASE";
                    
    /**
     * 补货方法: 最小-最大 
     */
    String DRP_REPLENISHMENT_METHOD_MIN_MAX = "MIN_MAX";
                    
    /**
     * 补货方法: 定期 
     */
    String DRP_REPLENISHMENT_METHOD_PERIODIC = "PERIODIC";
                    
    /**
     * 补货方法: 按需 
     */
    String DRP_REPLENISHMENT_METHOD_LOT_FOR_LOT = "LOT_FOR_LOT";
                    
    /**
     * 安全库存计算方法: 标准差法 
     */
    String DRP_SS_METHOD_STATISTICAL = "STATISTICAL";
                    
    /**
     * 安全库存计算方法: 简易法 
     */
    String DRP_SS_METHOD_SIMPLE = "SIMPLE";
                    
    /**
     * 安全库存计算方法: 需求驱动法 
     */
    String DRP_SS_METHOD_DDMRP = "DDMRP";
                    
    /**
     * 服务水平: 95% 
     */
    String DRP_SERVICE_LEVEL_PCT95 = "PCT95";
                    
    /**
     * 服务水平: 97.5% 
     */
    String DRP_SERVICE_LEVEL_PCT97_5 = "PCT97_5";
                    
    /**
     * 服务水平: 99% 
     */
    String DRP_SERVICE_LEVEL_PCT99 = "PCT99";
                    
    /**
     * 服务水平: 99.5% 
     */
    String DRP_SERVICE_LEVEL_PCT99_5 = "PCT99_5";
                    
    /**
     * 越库状态: 待匹配 
     */
    String DRP_XDOCK_STATUS_PENDING = "PENDING";
                    
    /**
     * 越库状态: 已匹配 
     */
    String DRP_XDOCK_STATUS_MATCHED = "MATCHED";
                    
    /**
     * 越库状态: 暂存中 
     */
    String DRP_XDOCK_STATUS_STAGING = "STAGING";
                    
    /**
     * 越库状态: 已装车 
     */
    String DRP_XDOCK_STATUS_LOADED = "LOADED";
                    
    /**
     * 越库状态: 已完成 
     */
    String DRP_XDOCK_STATUS_COMPLETED = "COMPLETED";
                    
    /**
     * 越库状态: 已取消 
     */
    String DRP_XDOCK_STATUS_CANCELLED = "CANCELLED";
                    
}
