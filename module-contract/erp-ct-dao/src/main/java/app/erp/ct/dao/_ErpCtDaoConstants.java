package app.erp.ct.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpCtDaoConstants {
    
    /**
     * 合同状态: 草稿 
     */
    String CONTRACT_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 合同状态: 谈判中 
     */
    String CONTRACT_STATUS_NEGOTIATION = "NEGOTIATION";
                    
    /**
     * 合同状态: 执行中 
     */
    String CONTRACT_STATUS_ACTIVE = "ACTIVE";
                    
    /**
     * 合同状态: 已中止 
     */
    String CONTRACT_STATUS_SUSPENDED = "SUSPENDED";
                    
    /**
     * 合同状态: 已到期 
     */
    String CONTRACT_STATUS_EXPIRED = "EXPIRED";
                    
    /**
     * 合同状态: 已终止 
     */
    String CONTRACT_STATUS_TERMINATED = "TERMINATED";
                    
    /**
     * 合同类型: 采购合同 
     */
    String CONTRACT_TYPE_PURCHASE = "PURCHASE";
                    
    /**
     * 合同类型: 销售合同 
     */
    String CONTRACT_TYPE_SALES = "SALES";
                    
    /**
     * 合同类型: 劳动合同 
     */
    String CONTRACT_TYPE_EMPLOYMENT = "EMPLOYMENT";
                    
    /**
     * 合同类型: 服务合同 
     */
    String CONTRACT_TYPE_SERVICE = "SERVICE";
                    
    /**
     * 合同方向: 进向(我方付款) 
     */
    String CONTRACT_DIRECTION_INBOUND = "INBOUND";
                    
    /**
     * 合同方向: 出向(客户付款) 
     */
    String CONTRACT_DIRECTION_OUTBOUND = "OUTBOUND";
                    
    /**
     * 开票条款: 预付款 
     */
    String INVOICE_TERM_ADVANCE = "ADVANCE";
                    
    /**
     * 开票条款: 里程碑 
     */
    String INVOICE_TERM_MILESTONE = "MILESTONE";
                    
    /**
     * 开票条款: 月结 
     */
    String INVOICE_TERM_MONTHLY = "MONTHLY";
                    
    /**
     * 开票条款: 完工 
     */
    String INVOICE_TERM_COMPLETION = "COMPLETION";
                    
    /**
     * 版本状态: 草稿 
     */
    String VERSION_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 版本状态: 定稿 
     */
    String VERSION_STATUS_FINALIZED = "FINALIZED";
                    
    /**
     * 版本状态: 已签署 
     */
    String VERSION_STATUS_SIGNED = "SIGNED";
                    
    /**
     * 审批状态: 等待中 
     */
    String APPROVAL_STATUS_WAITING = "WAITING";
                    
    /**
     * 审批状态: 待审批 
     */
    String APPROVAL_STATUS_PENDING = "PENDING";
                    
    /**
     * 审批状态: 已通过 
     */
    String APPROVAL_STATUS_APPROVED = "APPROVED";
                    
    /**
     * 审批状态: 已驳回 
     */
    String APPROVAL_STATUS_REJECTED = "REJECTED";
                    
    /**
     * 审批状态: 已跳过 
     */
    String APPROVAL_STATUS_SKIPPED = "SKIPPED";
                    
    /**
     * 返利类型: 采购返利 
     */
    String REBATE_TYPE_PURCHASE = "PURCHASE";
                    
    /**
     * 返利类型: 销售返利 
     */
    String REBATE_TYPE_SALES = "SALES";
                    
    /**
     * 返利协议状态: 草稿 
     */
    String REBATE_AGREEMENT_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 返利协议状态: 生效中 
     */
    String REBATE_AGREEMENT_STATUS_ACTIVE = "ACTIVE";
                    
    /**
     * 返利协议状态: 已到期 
     */
    String REBATE_AGREEMENT_STATUS_EXPIRED = "EXPIRED";
                    
    /**
     * 返利协议状态: 已结算 
     */
    String REBATE_AGREEMENT_STATUS_SETTLED = "SETTLED";
                    
    /**
     * 结算单状态: 草稿 
     */
    String SETTLEMENT_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 结算单状态: 已过账 
     */
    String SETTLEMENT_STATUS_POSTED = "POSTED";
                    
    /**
     * 结算单状态: 已取消 
     */
    String SETTLEMENT_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 计提方法: 期末一次性 
     */
    String ACCRUAL_METHOD_PERIOD_END = "PERIOD_END";
                    
    /**
     * 计提方法: 逐笔累计 
     */
    String ACCRUAL_METHOD_PROGRESSIVE = "PROGRESSIVE";
                    
}
