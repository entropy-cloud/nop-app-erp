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
     * 业务类型: 销售退货冲减 
     */
    int BUSINESS_TYPE_SALES_RETURN = 150;
                    
    /**
     * 业务类型: 费用报销 
     */
    int BUSINESS_TYPE_EXPENSE_CLAIM = 160;
                    
    /**
     * 业务类型: 员工借款 
     */
    int BUSINESS_TYPE_EMPLOYEE_ADVANCE = 170;
                    
    /**
     * 业务类型: 借款清算 
     */
    int BUSINESS_TYPE_EMPLOYEE_ADVANCE_SETTLE = 180;
                    
    /**
     * 业务类型: 应收票据收到 
     */
    int BUSINESS_TYPE_NOTES_RECEIVABLE_RECEIVED = 190;
                    
    /**
     * 业务类型: 票据贴现 
     */
    int BUSINESS_TYPE_NOTES_RECEIVABLE_DISCOUNTED = 200;
                    
    /**
     * 业务类型: 票据背书转让 
     */
    int BUSINESS_TYPE_NOTES_RECEIVABLE_ENDORSED = 210;
                    
    /**
     * 业务类型: 票据托收承兑 
     */
    int BUSINESS_TYPE_NOTES_RECEIVABLE_COLLECTION = 220;
                    
    /**
     * 业务类型: 开出应付票据 
     */
    int BUSINESS_TYPE_NOTES_PAYABLE_ISSUED = 230;
                    
    /**
     * 业务类型: 应付票据兑付 
     */
    int BUSINESS_TYPE_NOTES_PAYABLE_HONORED = 240;
                    
    /**
     * 业务类型: 授信利息 
     */
    int BUSINESS_TYPE_CREDIT_FACILITY_INTEREST = 250;
                    
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
     * 期间状态: 已复核 
     */
    int PERIOD_STATUS_CLOSED_FINAL = 50;
                    
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
                    
    /**
     * 审核状态: 未提交 
     */
    int APPROVE_STATUS_UNSUBMITTED = 10;
                    
    /**
     * 审核状态: 已提交 
     */
    int APPROVE_STATUS_SUBMITTED = 20;
                    
    /**
     * 审核状态: 已审核 
     */
    int APPROVE_STATUS_APPROVED = 30;
                    
    /**
     * 审核状态: 已驳回 
     */
    int APPROVE_STATUS_REJECTED = 40;
                    
    /**
     * 报销付款方式: 员工垫付 
     */
    int EXPENSE_PAYMENT_MODE_OWN_ACCOUNT = 10;
                    
    /**
     * 报销付款方式: 公司直付 
     */
    int EXPENSE_PAYMENT_MODE_COMPANY_ACCOUNT = 20;
                    
    /**
     * 费用类型: 差旅 
     */
    int EXPENSE_TYPE_TRAVEL = 10;
                    
    /**
     * 费用类型: 业务招待 
     */
    int EXPENSE_TYPE_ENTERTAINMENT = 20;
                    
    /**
     * 费用类型: 办公 
     */
    int EXPENSE_TYPE_OFFICE = 30;
                    
    /**
     * 费用类型: 通讯 
     */
    int EXPENSE_TYPE_COMMUNICATION = 40;
                    
    /**
     * 费用类型: 交通 
     */
    int EXPENSE_TYPE_TRANSPORTATION = 50;
                    
    /**
     * 费用类型: 其他 
     */
    int EXPENSE_TYPE_OTHER = 60;
                    
    /**
     * 借款类型: 费用预支 
     */
    int ADVANCE_TYPE_EXPENSE_ADVANCE = 10;
                    
    /**
     * 借款类型: 备用金 
     */
    int ADVANCE_TYPE_IMPREST = 20;
                    
    /**
     * 借款类型: 差旅借款 
     */
    int ADVANCE_TYPE_TRAVEL = 30;
                    
    /**
     * 报销单状态: 草稿 
     */
    int EXPENSE_CLAIM_STATUS_DRAFT = 10;
                    
    /**
     * 报销单状态: 已提交 
     */
    int EXPENSE_CLAIM_STATUS_SUBMITTED = 20;
                    
    /**
     * 报销单状态: 已审核 
     */
    int EXPENSE_CLAIM_STATUS_APPROVED = 30;
                    
    /**
     * 报销单状态: 已驳回 
     */
    int EXPENSE_CLAIM_STATUS_REJECTED = 40;
                    
    /**
     * 报销单状态: 已作废 
     */
    int EXPENSE_CLAIM_STATUS_CANCELLED = 50;
                    
    /**
     * 借款单状态: 草稿 
     */
    int ADVANCE_STATUS_DRAFT = 10;
                    
    /**
     * 借款单状态: 已提交 
     */
    int ADVANCE_STATUS_SUBMITTED = 20;
                    
    /**
     * 借款单状态: 已审核 
     */
    int ADVANCE_STATUS_APPROVED = 30;
                    
    /**
     * 借款单状态: 已驳回 
     */
    int ADVANCE_STATUS_REJECTED = 40;
                    
    /**
     * 借款单状态: 已作废 
     */
    int ADVANCE_STATUS_CANCELLED = 50;
                    
    /**
     * 票据类型: 银行承兑汇票 
     */
    int NOTES_TYPE_BANK_ACCEPTANCE = 10;
                    
    /**
     * 票据类型: 商业承兑汇票 
     */
    int NOTES_TYPE_COMMERCIAL_ACCEPTANCE = 20;
                    
    /**
     * 应收票据状态: 收到 
     */
    int NOTES_RECEIVABLE_STATUS_RECEIVED = 10;
                    
    /**
     * 应收票据状态: 已贴现 
     */
    int NOTES_RECEIVABLE_STATUS_DISCOUNTED = 20;
                    
    /**
     * 应收票据状态: 已背书 
     */
    int NOTES_RECEIVABLE_STATUS_ENDORSED = 30;
                    
    /**
     * 应收票据状态: 托收中 
     */
    int NOTES_RECEIVABLE_STATUS_COLLECTION_PENDING = 40;
                    
    /**
     * 应收票据状态: 已承兑 
     */
    int NOTES_RECEIVABLE_STATUS_HONORED = 50;
                    
    /**
     * 应收票据状态: 已拒付 
     */
    int NOTES_RECEIVABLE_STATUS_DISHONORED = 60;
                    
    /**
     * 应收票据状态: 已注销 
     */
    int NOTES_RECEIVABLE_STATUS_WRITE_OFF = 70;
                    
    /**
     * 应付票据状态: 已开出 
     */
    int NOTES_PAYABLE_STATUS_ISSUED = 10;
                    
    /**
     * 应付票据状态: 已兑付 
     */
    int NOTES_PAYABLE_STATUS_HONORED = 20;
                    
    /**
     * 应付票据状态: 已拒付 
     */
    int NOTES_PAYABLE_STATUS_DISHONORED = 30;
                    
    /**
     * 应付票据状态: 已注销 
     */
    int NOTES_PAYABLE_STATUS_WRITE_OFF = 40;
                    
    /**
     * 授信额度类型: 银行承兑额度 
     */
    int CREDIT_FACILITY_TYPE_BANK_ACCEPTANCE_LINE = 10;
                    
    /**
     * 授信额度类型: 贷款额度 
     */
    int CREDIT_FACILITY_TYPE_LOAN_LINE = 20;
                    
    /**
     * 现金流方向: 流入 
     */
    int CASH_FLOW_DIRECTION_INFLOW = 10;
                    
    /**
     * 现金流方向: 流出 
     */
    int CASH_FLOW_DIRECTION_OUTFLOW = 20;
                    
}
