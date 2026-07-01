package app.erp.fin.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpFinDaoConstants {
    
    /**
     * 凭证字: 收 
     */
    int VOUCHER_TYPE_RECEIPT = 10;
                    
    /**
     * 凭证字: 付 
     */
    int VOUCHER_TYPE_PAYMENT = 20;
                    
    /**
     * 凭证字: 转 
     */
    int VOUCHER_TYPE_TRANSFER = 30;
                    
    /**
     * 过账类型: 正常过账 
     */
    int POSTING_TYPE_NORMAL = 10;
                    
    /**
     * 过账类型: 期初余额录入 
     */
    int POSTING_TYPE_OPENING_BALANCE = 20;
                    
    /**
     * 过账类型: 调整 
     */
    int POSTING_TYPE_ADJUSTMENT = 30;
                    
    /**
     * 过账类型: 期末结转 
     */
    int POSTING_TYPE_CLOSING = 40;
                    
    /**
     * 过账类型: 红字冲销 
     */
    int POSTING_TYPE_REVERSAL = 50;
                    
    /**
     * 凭证状态: 草稿 
     */
    int VOUCHER_STATUS_DRAFT = 10;
                    
    /**
     * 凭证状态: 已过账 
     */
    int VOUCHER_STATUS_POSTED = 20;
                    
    /**
     * 凭证状态: 已作废 
     */
    int VOUCHER_STATUS_CANCELLED = 30;
                    
    /**
     * 借贷方向: 借 
     */
    int DC_DIRECTION_DEBIT = 10;
                    
    /**
     * 借贷方向: 贷 
     */
    int DC_DIRECTION_CREDIT = 20;
                    
    /**
     * 业务类型: 采购入库 
     */
    int BUSINESS_TYPE_PURCHASE_INPUT = 10;
                    
    /**
     * 业务类型: 销售出库 
     */
    int BUSINESS_TYPE_SALES_OUTPUT = 20;
                    
    /**
     * 业务类型: 应付发票 
     */
    int BUSINESS_TYPE_AP_INVOICE = 30;
                    
    /**
     * 业务类型: 应收发票 
     */
    int BUSINESS_TYPE_AR_INVOICE = 40;
                    
    /**
     * 业务类型: 付款 
     */
    int BUSINESS_TYPE_PAYMENT = 50;
                    
    /**
     * 业务类型: 收款 
     */
    int BUSINESS_TYPE_RECEIPT = 60;
                    
    /**
     * 业务类型: 折旧 
     */
    int BUSINESS_TYPE_DEPRECIATION = 70;
                    
    /**
     * 业务类型: 资本化 
     */
    int BUSINESS_TYPE_CAPITALIZATION = 80;
                    
    /**
     * 业务类型: 处置 
     */
    int BUSINESS_TYPE_DISPOSAL = 90;
                    
    /**
     * 业务类型: 生产成本结转 
     */
    int BUSINESS_TYPE_PRODUCTION_COST = 100;
                    
    /**
     * 业务类型: 项目成本归集 
     */
    int BUSINESS_TYPE_PROJECT_COST = 110;
                    
    /**
     * 业务类型: 期末结转 
     */
    int BUSINESS_TYPE_PERIOD_CLOSING = 120;
                    
    /**
     * 业务类型: 汇兑损益 
     */
    int BUSINESS_TYPE_FX_REVALUATION = 130;
                    
    /**
     * 业务类型: 采购退货冲减 
     */
    int BUSINESS_TYPE_PURCHASE_RETURN = 140;
                    
    /**
     * 期间状态: 开启 
     */
    int PERIOD_STATUS_OPEN = 10;
                    
    /**
     * 期间状态: 结账中 
     */
    int PERIOD_STATUS_CLOSING = 20;
                    
    /**
     * 期间状态: 已结账 
     */
    int PERIOD_STATUS_CLOSED = 30;
                    
    /**
     * 期间状态: 未开启 
     */
    int PERIOD_STATUS_NEVER_OPENED = 40;
                    
    /**
     * 账户类型: 银行 
     */
    int FUND_ACCOUNT_TYPE_BANK = 10;
                    
    /**
     * 账户类型: 现金 
     */
    int FUND_ACCOUNT_TYPE_CASH = 20;
                    
    /**
     * 账户类型: 支付宝 
     */
    int FUND_ACCOUNT_TYPE_ALIPAY = 30;
                    
    /**
     * 账户类型: 微信 
     */
    int FUND_ACCOUNT_TYPE_WECHAT = 40;
                    
    /**
     * 账户类型: 其他 
     */
    int FUND_ACCOUNT_TYPE_OTHER = 50;
                    
    /**
     * 账户状态: 启用 
     */
    int FUND_ACCOUNT_STATUS_ACTIVE = 10;
                    
    /**
     * 账户状态: 停用 
     */
    int FUND_ACCOUNT_STATUS_INACTIVE = 20;
                    
    /**
     * AR/AP 方向: 应收(AR) 
     */
    int AR_AP_DIRECTION_RECEIVABLE = 10;
                    
    /**
     * AR/AP 方向: 应付(AP) 
     */
    int AR_AP_DIRECTION_PAYABLE = 20;
                    
    /**
     * AR/AP 状态: 未核销 
     */
    int AR_AP_STATUS_OPEN = 10;
                    
    /**
     * AR/AP 状态: 部分核销 
     */
    int AR_AP_STATUS_PARTIAL = 20;
                    
    /**
     * AR/AP 状态: 已核销 
     */
    int AR_AP_STATUS_SETTLED = 30;
                    
    /**
     * AR/AP 状态: 已作废 
     */
    int AR_AP_STATUS_CANCELLED = 40;
                    
    /**
     * 核销单状态: 草稿 
     */
    int RECONCILIATION_STATUS_DRAFT = 10;
                    
    /**
     * 核销单状态: 已过账 
     */
    int RECONCILIATION_STATUS_POSTED = 20;
                    
    /**
     * 核销单状态: 已红冲 
     */
    int RECONCILIATION_STATUS_REVERSED = 30;
                    
    /**
     * 模块结账状态: 未结 
     */
    int MODULE_CLOSE_STATUS_OPEN = 10;
                    
    /**
     * 模块结账状态: 结账中 
     */
    int MODULE_CLOSE_STATUS_CLOSING = 20;
                    
    /**
     * 模块结账状态: 已结 
     */
    int MODULE_CLOSE_STATUS_CLOSED = 30;
                    
}
