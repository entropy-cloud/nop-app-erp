package app.erp.fin.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpFinDaoConstants {
    
    /**
     * 凭证字: 收 
     */
    String VOUCHER_TYPE_RECEIPT = "RECEIPT";
                    
    /**
     * 凭证字: 付 
     */
    String VOUCHER_TYPE_PAYMENT = "PAYMENT";
                    
    /**
     * 凭证字: 转 
     */
    String VOUCHER_TYPE_TRANSFER = "TRANSFER";
                    
    /**
     * 过账类型: 正常过账 
     */
    String POSTING_TYPE_NORMAL = "NORMAL";
                    
    /**
     * 过账类型: 期初余额录入 
     */
    String POSTING_TYPE_OPENING_BALANCE = "OPENING_BALANCE";
                    
    /**
     * 过账类型: 调整 
     */
    String POSTING_TYPE_ADJUSTMENT = "ADJUSTMENT";
                    
    /**
     * 过账类型: 期末结转 
     */
    String POSTING_TYPE_CLOSING = "CLOSING";
                    
    /**
     * 过账类型: 红字冲销 
     */
    String POSTING_TYPE_REVERSAL = "REVERSAL";
                    
    /**
     * 凭证状态: 草稿 
     */
    String VOUCHER_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 凭证状态: 已过账 
     */
    String VOUCHER_STATUS_POSTED = "POSTED";
                    
    /**
     * 凭证状态: 已作废 
     */
    String VOUCHER_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 借贷方向: 借 
     */
    String DC_DIRECTION_DEBIT = "DEBIT";
                    
    /**
     * 借贷方向: 贷 
     */
    String DC_DIRECTION_CREDIT = "CREDIT";
                    
    /**
     * 业务类型: 采购入库 
     */
    String BUSINESS_TYPE_PURCHASE_INPUT = "PURCHASE_INPUT";
                    
    /**
     * 业务类型: 销售出库 
     */
    String BUSINESS_TYPE_SALES_OUTPUT = "SALES_OUTPUT";
                    
    /**
     * 业务类型: 应付发票 
     */
    String BUSINESS_TYPE_AP_INVOICE = "AP_INVOICE";
                    
    /**
     * 业务类型: 应收发票 
     */
    String BUSINESS_TYPE_AR_INVOICE = "AR_INVOICE";
                    
    /**
     * 业务类型: 付款 
     */
    String BUSINESS_TYPE_PAYMENT = "PAYMENT";
                    
    /**
     * 业务类型: 收款 
     */
    String BUSINESS_TYPE_RECEIPT = "RECEIPT";
                    
    /**
     * 业务类型: 折旧 
     */
    String BUSINESS_TYPE_DEPRECIATION = "DEPRECIATION";
                    
    /**
     * 业务类型: 资本化 
     */
    String BUSINESS_TYPE_CAPITALIZATION = "CAPITALIZATION";
                    
    /**
     * 业务类型: 处置 
     */
    String BUSINESS_TYPE_DISPOSAL = "DISPOSAL";
                    
    /**
     * 业务类型: 生产成本结转 
     */
    String BUSINESS_TYPE_PRODUCTION_COST = "PRODUCTION_COST";
                    
    /**
     * 业务类型: 项目成本归集 
     */
    String BUSINESS_TYPE_PROJECT_COST = "PROJECT_COST";
                    
    /**
     * 业务类型: 期末结转 
     */
    String BUSINESS_TYPE_PERIOD_CLOSING = "PERIOD_CLOSING";
                    
    /**
     * 业务类型: 汇兑损益 
     */
    String BUSINESS_TYPE_FX_REVALUATION = "FX_REVALUATION";
                    
    /**
     * 业务类型: 采购退货冲减 
     */
    String BUSINESS_TYPE_PURCHASE_RETURN = "PURCHASE_RETURN";
                    
    /**
     * 业务类型: 销售退货冲减 
     */
    String BUSINESS_TYPE_SALES_RETURN = "SALES_RETURN";
                    
    /**
     * 业务类型: 费用报销 
     */
    String BUSINESS_TYPE_EXPENSE_CLAIM = "EXPENSE_CLAIM";
                    
    /**
     * 业务类型: 员工借款 
     */
    String BUSINESS_TYPE_EMPLOYEE_ADVANCE = "EMPLOYEE_ADVANCE";
                    
    /**
     * 业务类型: 借款清算 
     */
    String BUSINESS_TYPE_EMPLOYEE_ADVANCE_SETTLE = "EMPLOYEE_ADVANCE_SETTLE";
                    
    /**
     * 业务类型: 应收票据收到 
     */
    String BUSINESS_TYPE_NOTES_RECEIVABLE_RECEIVED = "NOTES_RECEIVABLE_RECEIVED";
                    
    /**
     * 业务类型: 票据贴现 
     */
    String BUSINESS_TYPE_NOTES_RECEIVABLE_DISCOUNTED = "NOTES_RECEIVABLE_DISCOUNTED";
                    
    /**
     * 业务类型: 票据背书转让 
     */
    String BUSINESS_TYPE_NOTES_RECEIVABLE_ENDORSED = "NOTES_RECEIVABLE_ENDORSED";
                    
    /**
     * 业务类型: 票据托收承兑 
     */
    String BUSINESS_TYPE_NOTES_RECEIVABLE_COLLECTION = "NOTES_RECEIVABLE_COLLECTION";
                    
    /**
     * 业务类型: 开出应付票据 
     */
    String BUSINESS_TYPE_NOTES_PAYABLE_ISSUED = "NOTES_PAYABLE_ISSUED";
                    
    /**
     * 业务类型: 应付票据兑付 
     */
    String BUSINESS_TYPE_NOTES_PAYABLE_HONORED = "NOTES_PAYABLE_HONORED";
                    
    /**
     * 业务类型: 授信利息 
     */
    String BUSINESS_TYPE_CREDIT_FACILITY_INTEREST = "CREDIT_FACILITY_INTEREST";
                    
    /**
     * 业务类型: 所有权转移 
     */
    String BUSINESS_TYPE_OWNERSHIP_TRANSFER = "OWNERSHIP_TRANSFER";
                    
    /**
     * 业务类型: 薪酬计提 
     */
    String BUSINESS_TYPE_SALARY = "SALARY";
                    
    /**
     * 业务类型: 薪酬发放 
     */
    String BUSINESS_TYPE_SALARY_PAYMENT = "SALARY_PAYMENT";
                    
    /**
     * 业务类型: 社保公司承担 
     */
    String BUSINESS_TYPE_SOCIAL_INSURANCE_ER = "SOCIAL_INSURANCE_ER";
                    
    /**
     * 业务类型: 公积金公司承担 
     */
    String BUSINESS_TYPE_HOUSING_FUND_ER = "HOUSING_FUND_ER";
                    
    /**
     * 业务类型: 银行对账未达账项调整 
     */
    String BUSINESS_TYPE_BANK_RECON_ADJ = "BANK_RECON_ADJ";
                    
    /**
     * 业务类型: 采购价差 
     */
    String BUSINESS_TYPE_PURCHASE_PRICE_VARIANCE = "PURCHASE_PRICE_VARIANCE";
                    
    /**
     * 业务类型: 坏账准备计提 
     */
    String BUSINESS_TYPE_BAD_DEBT_RESERVE = "BAD_DEBT_RESERVE";
                    
    /**
     * 业务类型: 坏账核销 
     */
    String BUSINESS_TYPE_BAD_DEBT_WRITE_OFF = "BAD_DEBT_WRITE_OFF";
                    
    /**
     * 业务类型: 坏账收回恢复 
     */
    String BUSINESS_TYPE_BAD_DEBT_RECOVERY = "BAD_DEBT_RECOVERY";
                    
    /**
     * 业务类型: 坏账准备释放 
     */
    String BUSINESS_TYPE_BAD_DEBT_RELEASE = "BAD_DEBT_RELEASE";
                    
    /**
     * 业务类型: 本年利润结转未分配利润 
     */
    String BUSINESS_TYPE_PROFIT_TO_RETAINED_EARNINGS = "PROFIT_TO_RETAINED_EARNINGS";
                    
    /**
     * 期间状态: 开启 
     */
    String PERIOD_STATUS_OPEN = "OPEN";
                    
    /**
     * 期间状态: 结账中 
     */
    String PERIOD_STATUS_CLOSING = "CLOSING";
                    
    /**
     * 期间状态: 已结账 
     */
    String PERIOD_STATUS_CLOSED = "CLOSED";
                    
    /**
     * 期间状态: 未开启 
     */
    String PERIOD_STATUS_NEVER_OPENED = "NEVER_OPENED";
                    
    /**
     * 期间状态: 已复核 
     */
    String PERIOD_STATUS_CLOSED_FINAL = "CLOSED_FINAL";
                    
    /**
     * 账户类型: 银行 
     */
    String FUND_ACCOUNT_TYPE_BANK = "BANK";
                    
    /**
     * 账户类型: 现金 
     */
    String FUND_ACCOUNT_TYPE_CASH = "CASH";
                    
    /**
     * 账户类型: 支付宝 
     */
    String FUND_ACCOUNT_TYPE_ALIPAY = "ALIPAY";
                    
    /**
     * 账户类型: 微信 
     */
    String FUND_ACCOUNT_TYPE_WECHAT = "WECHAT";
                    
    /**
     * 账户类型: 其他 
     */
    String FUND_ACCOUNT_TYPE_OTHER = "OTHER";
                    
    /**
     * 账户状态: 启用 
     */
    String FUND_ACCOUNT_STATUS_ACTIVE = "ACTIVE";
                    
    /**
     * 账户状态: 停用 
     */
    String FUND_ACCOUNT_STATUS_INACTIVE = "INACTIVE";
                    
    /**
     * AR/AP 方向: 应收(AR) 
     */
    String AR_AP_DIRECTION_RECEIVABLE = "RECEIVABLE";
                    
    /**
     * AR/AP 方向: 应付(AP) 
     */
    String AR_AP_DIRECTION_PAYABLE = "PAYABLE";
                    
    /**
     * AR/AP 状态: 未核销 
     */
    String AR_AP_STATUS_OPEN = "OPEN";
                    
    /**
     * AR/AP 状态: 部分核销 
     */
    String AR_AP_STATUS_PARTIAL = "PARTIAL";
                    
    /**
     * AR/AP 状态: 已核销 
     */
    String AR_AP_STATUS_SETTLED = "SETTLED";
                    
    /**
     * AR/AP 状态: 已作废 
     */
    String AR_AP_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * AR/AP 状态: 已坏账核销 
     */
    String AR_AP_STATUS_WRITTEN_OFF = "WRITTEN_OFF";
                    
    /**
     * 银行勾对状态: 未勾对 
     */
    String BANK_MATCH_STATUS_UNMATCHED = "UNMATCHED";
                    
    /**
     * 银行勾对状态: 已自动勾对 
     */
    String BANK_MATCH_STATUS_MATCHED = "MATCHED";
                    
    /**
     * 银行勾对状态: 已手工勾对 
     */
    String BANK_MATCH_STATUS_MANUAL_MATCHED = "MANUAL_MATCHED";
                    
    /**
     * 银行勾对状态: 待查(多候选) 
     */
    String BANK_MATCH_STATUS_SUSPENSE = "SUSPENSE";
                    
    /**
     * 核销单状态: 草稿 
     */
    String RECONCILIATION_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 核销单状态: 已过账 
     */
    String RECONCILIATION_STATUS_POSTED = "POSTED";
                    
    /**
     * 核销单状态: 已红冲 
     */
    String RECONCILIATION_STATUS_REVERSED = "REVERSED";
                    
    /**
     * 模块结账状态: 未结 
     */
    String MODULE_CLOSE_STATUS_OPEN = "OPEN";
                    
    /**
     * 模块结账状态: 结账中 
     */
    String MODULE_CLOSE_STATUS_CLOSING = "CLOSING";
                    
    /**
     * 模块结账状态: 已结 
     */
    String MODULE_CLOSE_STATUS_CLOSED = "CLOSED";
                    
    /**
     * 审核状态: 未提交 
     */
    String APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";
                    
    /**
     * 审核状态: 已提交 
     */
    String APPROVE_STATUS_SUBMITTED = "SUBMITTED";
                    
    /**
     * 审核状态: 已审核 
     */
    String APPROVE_STATUS_APPROVED = "APPROVED";
                    
    /**
     * 审核状态: 已驳回 
     */
    String APPROVE_STATUS_REJECTED = "REJECTED";
                    
    /**
     * 坏账单类型: 坏账核销 
     */
    String BAD_DEBT_TYPE_WRITE_OFF = "WRITE_OFF";
                    
    /**
     * 坏账单类型: 坏账收回恢复 
     */
    String BAD_DEBT_TYPE_RECOVERY = "RECOVERY";
                    
    /**
     * 报销付款方式: 员工垫付 
     */
    String EXPENSE_PAYMENT_MODE_OWN_ACCOUNT = "OWN_ACCOUNT";
                    
    /**
     * 报销付款方式: 公司直付 
     */
    String EXPENSE_PAYMENT_MODE_COMPANY_ACCOUNT = "COMPANY_ACCOUNT";
                    
    /**
     * 费用类型: 差旅 
     */
    String EXPENSE_TYPE_TRAVEL = "TRAVEL";
                    
    /**
     * 费用类型: 业务招待 
     */
    String EXPENSE_TYPE_ENTERTAINMENT = "ENTERTAINMENT";
                    
    /**
     * 费用类型: 办公 
     */
    String EXPENSE_TYPE_OFFICE = "OFFICE";
                    
    /**
     * 费用类型: 通讯 
     */
    String EXPENSE_TYPE_COMMUNICATION = "COMMUNICATION";
                    
    /**
     * 费用类型: 交通 
     */
    String EXPENSE_TYPE_TRANSPORTATION = "TRANSPORTATION";
                    
    /**
     * 费用类型: 其他 
     */
    String EXPENSE_TYPE_OTHER = "OTHER";
                    
    /**
     * 借款类型: 费用预支 
     */
    String ADVANCE_TYPE_EXPENSE_ADVANCE = "EXPENSE_ADVANCE";
                    
    /**
     * 借款类型: 备用金 
     */
    String ADVANCE_TYPE_IMPREST = "IMPREST";
                    
    /**
     * 借款类型: 差旅借款 
     */
    String ADVANCE_TYPE_TRAVEL = "TRAVEL";
                    
    /**
     * 报销单状态: 草稿 
     */
    String EXPENSE_CLAIM_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 报销单状态: 已提交 
     */
    String EXPENSE_CLAIM_STATUS_SUBMITTED = "SUBMITTED";
                    
    /**
     * 报销单状态: 已审核 
     */
    String EXPENSE_CLAIM_STATUS_APPROVED = "APPROVED";
                    
    /**
     * 报销单状态: 已驳回 
     */
    String EXPENSE_CLAIM_STATUS_REJECTED = "REJECTED";
                    
    /**
     * 报销单状态: 已作废 
     */
    String EXPENSE_CLAIM_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 借款单状态: 草稿 
     */
    String ADVANCE_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 借款单状态: 已提交 
     */
    String ADVANCE_STATUS_SUBMITTED = "SUBMITTED";
                    
    /**
     * 借款单状态: 已审核 
     */
    String ADVANCE_STATUS_APPROVED = "APPROVED";
                    
    /**
     * 借款单状态: 已驳回 
     */
    String ADVANCE_STATUS_REJECTED = "REJECTED";
                    
    /**
     * 借款单状态: 已作废 
     */
    String ADVANCE_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 票据类型: 银行承兑汇票 
     */
    String NOTES_TYPE_BANK_ACCEPTANCE = "BANK_ACCEPTANCE";
                    
    /**
     * 票据类型: 商业承兑汇票 
     */
    String NOTES_TYPE_COMMERCIAL_ACCEPTANCE = "COMMERCIAL_ACCEPTANCE";
                    
    /**
     * 应收票据状态: 收到 
     */
    String NOTES_RECEIVABLE_STATUS_RECEIVED = "RECEIVED";
                    
    /**
     * 应收票据状态: 已贴现 
     */
    String NOTES_RECEIVABLE_STATUS_DISCOUNTED = "DISCOUNTED";
                    
    /**
     * 应收票据状态: 已背书 
     */
    String NOTES_RECEIVABLE_STATUS_ENDORSED = "ENDORSED";
                    
    /**
     * 应收票据状态: 托收中 
     */
    String NOTES_RECEIVABLE_STATUS_COLLECTION_PENDING = "COLLECTION_PENDING";
                    
    /**
     * 应收票据状态: 已承兑 
     */
    String NOTES_RECEIVABLE_STATUS_HONORED = "HONORED";
                    
    /**
     * 应收票据状态: 已拒付 
     */
    String NOTES_RECEIVABLE_STATUS_DISHONORED = "DISHONORED";
                    
    /**
     * 应收票据状态: 已注销 
     */
    String NOTES_RECEIVABLE_STATUS_WRITE_OFF = "WRITE_OFF";
                    
    /**
     * 应付票据状态: 已开出 
     */
    String NOTES_PAYABLE_STATUS_ISSUED = "ISSUED";
                    
    /**
     * 应付票据状态: 已兑付 
     */
    String NOTES_PAYABLE_STATUS_HONORED = "HONORED";
                    
    /**
     * 应付票据状态: 已拒付 
     */
    String NOTES_PAYABLE_STATUS_DISHONORED = "DISHONORED";
                    
    /**
     * 应付票据状态: 已注销 
     */
    String NOTES_PAYABLE_STATUS_WRITE_OFF = "WRITE_OFF";
                    
    /**
     * 授信额度类型: 银行承兑额度 
     */
    String CREDIT_FACILITY_TYPE_BANK_ACCEPTANCE_LINE = "BANK_ACCEPTANCE_LINE";
                    
    /**
     * 授信额度类型: 贷款额度 
     */
    String CREDIT_FACILITY_TYPE_LOAN_LINE = "LOAN_LINE";
                    
    /**
     * 现金流方向: 流入 
     */
    String CASH_FLOW_DIRECTION_INFLOW = "INFLOW";
                    
    /**
     * 现金流方向: 流出 
     */
    String CASH_FLOW_DIRECTION_OUTFLOW = "OUTFLOW";
                    
    /**
     * 过账异常处置状态: 待处置 
     */
    String POSTING_EXCEPTION_STATUS_PENDING = "PENDING";
                    
    /**
     * 过账异常处置状态: 重试中 
     */
    String POSTING_EXCEPTION_STATUS_RETRYING = "RETRYING";
                    
    /**
     * 过账异常处置状态: 重试成功 
     */
    String POSTING_EXCEPTION_STATUS_RETRIED = "RETRIED";
                    
    /**
     * 过账异常处置状态: 已忽略 
     */
    String POSTING_EXCEPTION_STATUS_IGNORED = "IGNORED";
                    
    /**
     * 过账异常处置状态: 手工补录 
     */
    String POSTING_EXCEPTION_STATUS_MANUAL = "MANUAL";
                    
    /**
     * 过账异常处置动作: 重试 
     */
    String POSTING_EXCEPTION_RESOLUTION_RETRY = "RETRY";
                    
    /**
     * 过账异常处置动作: 忽略 
     */
    String POSTING_EXCEPTION_RESOLUTION_IGNORE = "IGNORE";
                    
    /**
     * 过账异常处置动作: 手工补录 
     */
    String POSTING_EXCEPTION_RESOLUTION_MANUAL = "MANUAL";
                    
}
