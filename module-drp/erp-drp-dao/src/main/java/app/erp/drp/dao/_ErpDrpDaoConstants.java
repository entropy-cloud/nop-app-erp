package app.erp.drp.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpDrpDaoConstants {
    
    /**
     * DRP计划状态: 草稿 
     */
    int DRP_PLAN_STATUS_DRAFT = 10;
                    
    /**
     * DRP计划状态: 已计算 
     */
    int DRP_PLAN_STATUS_COMPUTED = 20;
                    
    /**
     * DRP计划状态: 已批准 
     */
    int DRP_PLAN_STATUS_APPROVED = 30;
                    
    /**
     * DRP计划状态: 已执行 
     */
    int DRP_PLAN_STATUS_EXECUTED = 40;
                    
    /**
     * DRP明细行状态: 建议 
     */
    int DRP_LINE_STATUS_SUGGESTED = 10;
                    
    /**
     * DRP明细行状态: 已批准 
     */
    int DRP_LINE_STATUS_APPROVED = 20;
                    
    /**
     * DRP明细行状态: 已下单 
     */
    int DRP_LINE_STATUS_ORDERED = 30;
                    
    /**
     * DRP明细行状态: 已取消 
     */
    int DRP_LINE_STATUS_CANCELLED = 40;
                    
    /**
     * 补货类型: 仓间调拨 
     */
    int DRP_REPLENISHMENT_TYPE_TRANSFER = 10;
                    
    /**
     * 补货类型: 向供应商采购 
     */
    int DRP_REPLENISHMENT_TYPE_PURCHASE = 20;
                    
    /**
     * 补货方法: 最小-最大 
     */
    int DRP_REPLENISHMENT_METHOD_MIN_MAX = 10;
                    
    /**
     * 补货方法: 定期 
     */
    int DRP_REPLENISHMENT_METHOD_PERIODIC = 20;
                    
    /**
     * 补货方法: 按需 
     */
    int DRP_REPLENISHMENT_METHOD_LOT_FOR_LOT = 30;
                    
}
