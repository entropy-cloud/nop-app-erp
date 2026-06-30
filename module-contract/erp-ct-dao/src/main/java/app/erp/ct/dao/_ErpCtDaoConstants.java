package app.erp.ct.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpCtDaoConstants {
    
    /**
     * 合同状态: 草稿 
     */
    int CONTRACT_STATUS_DRAFT = 10;
                    
    /**
     * 合同状态: 谈判中 
     */
    int CONTRACT_STATUS_NEGOTIATION = 20;
                    
    /**
     * 合同状态: 执行中 
     */
    int CONTRACT_STATUS_ACTIVE = 30;
                    
    /**
     * 合同状态: 已中止 
     */
    int CONTRACT_STATUS_SUSPENDED = 40;
                    
    /**
     * 合同状态: 已到期 
     */
    int CONTRACT_STATUS_EXPIRED = 50;
                    
    /**
     * 合同状态: 已终止 
     */
    int CONTRACT_STATUS_TERMINATED = 60;
                    
    /**
     * 合同类型: 采购合同 
     */
    int CONTRACT_TYPE_PURCHASE = 10;
                    
    /**
     * 合同类型: 销售合同 
     */
    int CONTRACT_TYPE_SALES = 20;
                    
    /**
     * 合同类型: 劳动合同 
     */
    int CONTRACT_TYPE_EMPLOYMENT = 30;
                    
    /**
     * 合同类型: 服务合同 
     */
    int CONTRACT_TYPE_SERVICE = 40;
                    
    /**
     * 合同方向: 进向(我方付款) 
     */
    int CONTRACT_DIRECTION_INBOUND = 10;
                    
    /**
     * 合同方向: 出向(客户付款) 
     */
    int CONTRACT_DIRECTION_OUTBOUND = 20;
                    
    /**
     * 开票条款: 预付款 
     */
    int INVOICE_TERM_ADVANCE = 10;
                    
    /**
     * 开票条款: 里程碑 
     */
    int INVOICE_TERM_MILESTONE = 20;
                    
    /**
     * 开票条款: 月结 
     */
    int INVOICE_TERM_MONTHLY = 30;
                    
    /**
     * 开票条款: 完工 
     */
    int INVOICE_TERM_COMPLETION = 40;
                    
    /**
     * 版本状态: 草稿 
     */
    int VERSION_STATUS_DRAFT = 10;
                    
    /**
     * 版本状态: 定稿 
     */
    int VERSION_STATUS_FINALIZED = 20;
                    
    /**
     * 版本状态: 已签署 
     */
    int VERSION_STATUS_SIGNED = 30;
                    
}
