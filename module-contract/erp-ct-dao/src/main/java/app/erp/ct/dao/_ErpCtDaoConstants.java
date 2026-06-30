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
                    
    /**
     * 审批状态: 等待中 
     */
    int APPROVAL_STATUS_WAITING = 10;
                    
    /**
     * 审批状态: 待审批 
     */
    int APPROVAL_STATUS_PENDING = 20;
                    
    /**
     * 审批状态: 已通过 
     */
    int APPROVAL_STATUS_APPROVED = 30;
                    
    /**
     * 审批状态: 已驳回 
     */
    int APPROVAL_STATUS_REJECTED = 40;
                    
    /**
     * 审批状态: 已跳过 
     */
    int APPROVAL_STATUS_SKIPPED = 50;
                    
    /**
     * 返利类型: 采购返利 
     */
    int REBATE_TYPE_PURCHASE = 10;
                    
    /**
     * 返利类型: 销售返利 
     */
    int REBATE_TYPE_SALES = 20;
                    
    /**
     * 返利协议状态: 草稿 
     */
    int REBATE_AGREEMENT_STATUS_DRAFT = 10;
                    
    /**
     * 返利协议状态: 生效中 
     */
    int REBATE_AGREEMENT_STATUS_ACTIVE = 20;
                    
    /**
     * 返利协议状态: 已到期 
     */
    int REBATE_AGREEMENT_STATUS_EXPIRED = 30;
                    
    /**
     * 返利协议状态: 已结算 
     */
    int REBATE_AGREEMENT_STATUS_SETTLED = 40;
                    
    /**
     * 结算单状态: 草稿 
     */
    int SETTLEMENT_STATUS_DRAFT = 10;
                    
    /**
     * 结算单状态: 已过账 
     */
    int SETTLEMENT_STATUS_POSTED = 20;
                    
    /**
     * 结算单状态: 已取消 
     */
    int SETTLEMENT_STATUS_CANCELLED = 30;
                    
    /**
     * 计提方法: 期末一次性 
     */
    int ACCRUAL_METHOD_PERIOD_END = 10;
                    
    /**
     * 计提方法: 逐笔累计 
     */
    int ACCRUAL_METHOD_PROGRESSIVE = 20;
                    
}
