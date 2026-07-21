package app.erp.md.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpMdDaoConstants {
    
    /**
     * 物料类型: 商品 
     */
    String MATERIAL_TYPE_GOODS = "GOODS";
                    
    /**
     * 物料类型: 原材料 
     */
    String MATERIAL_TYPE_RAW_MATERIAL = "RAW_MATERIAL";
                    
    /**
     * 物料类型: 半成品 
     */
    String MATERIAL_TYPE_SEMI_PRODUCT = "SEMI_PRODUCT";
                    
    /**
     * 物料类型: 产成品 
     */
    String MATERIAL_TYPE_FINISHED_PRODUCT = "FINISHED_PRODUCT";
                    
    /**
     * 物料类型: 服务 
     */
    String MATERIAL_TYPE_SERVICE = "SERVICE";
                    
    /**
     * 物料类型: 包装物 
     */
    String MATERIAL_TYPE_PACKAGING = "PACKAGING";
                    
    /**
     * 物料类型: 消耗品 
     */
    String MATERIAL_TYPE_CONSUMABLE = "CONSUMABLE";
                    
    /**
     * 启用状态: 启用 
     */
    String ACTIVE_STATUS_ACTIVE = "ACTIVE";
                    
    /**
     * 启用状态: 停用 
     */
    String ACTIVE_STATUS_INACTIVE = "INACTIVE";
                    
    /**
     * 存货计价方法: 移动加权平均 
     */
    String COST_METHOD_MOVING_AVERAGE = "MOVING_AVERAGE";
                    
    /**
     * 存货计价方法: 全月一次加权平均 
     */
    String COST_METHOD_WEIGHTED_AVERAGE = "WEIGHTED_AVERAGE";
                    
    /**
     * 存货计价方法: 先进先出 
     */
    String COST_METHOD_FIFO = "FIFO";
                    
    /**
     * 存货计价方法: 后进先出 
     */
    String COST_METHOD_LIFO = "LIFO";
                    
    /**
     * 存货计价方法: 标准成本 
     */
    String COST_METHOD_STANDARD = "STANDARD";
                    
    /**
     * 存货计价方法: 具体辨认(个别计价) 
     */
    String COST_METHOD_SPECIFIC = "SPECIFIC";
                    
    /**
     * 存货计价方法: 批次 
     */
    String COST_METHOD_BATCH = "BATCH";
                    
    /**
     * 价格校验级别: 不校验 
     */
    String PRICE_VALIDATION_OFF = "OFF";
                    
    /**
     * 价格校验级别: 警告放行 
     */
    String PRICE_VALIDATION_WARN = "WARN";
                    
    /**
     * 价格校验级别: 强制拦截 
     */
    String PRICE_VALIDATION_HARD = "HARD";
                    
    /**
     * 往来单位类型: 客户 
     */
    String PARTNER_TYPE_CUSTOMER = "CUSTOMER";
                    
    /**
     * 往来单位类型: 供应商 
     */
    String PARTNER_TYPE_SUPPLIER = "SUPPLIER";
                    
    /**
     * 往来单位类型: 客户且供应商 
     */
    String PARTNER_TYPE_BOTH = "BOTH";
                    
    /**
     * 往来单位类型: 员工(内部往来) 
     */
    String PARTNER_TYPE_EMPLOYEE = "EMPLOYEE";
                    
    /**
     * 往来单位类型: 报关行 
     */
    String PARTNER_TYPE_CUSTOMS_BROKER = "CUSTOMS_BROKER";
                    
    /**
     * FTA 优惠协定代码: 中国-东盟 
     */
    String CUSTOMS_PREFERENCE_CODE_ASEAN = "ASEAN";
                    
    /**
     * FTA 优惠协定代码: 中韩 
     */
    String CUSTOMS_PREFERENCE_CODE_CKFTA = "CKFTA";
                    
    /**
     * FTA 优惠协定代码: 中澳 
     */
    String CUSTOMS_PREFERENCE_CODE_CHAFTA = "CHAFTA";
                    
    /**
     * FTA 优惠协定代码: 中智 
     */
    String CUSTOMS_PREFERENCE_CODE_CCFTA = "CCFTA";
                    
    /**
     * FTA 优惠协定代码: 中新(新西兰) 
     */
    String CUSTOMS_PREFERENCE_CODE_CNZFTA = "CNZFTA";
                    
    /**
     * FTA 优惠协定代码: 中巴(巴基斯坦) 
     */
    String CUSTOMS_PREFERENCE_CODE_CPFTA = "CPFTA";
                    
    /**
     * FTA 优惠协定代码: 中哥(哥斯达黎加) 
     */
    String CUSTOMS_PREFERENCE_CODE_COSTA_RICA = "COSTA_RICA";
                    
    /**
     * FTA 优惠协定代码: 中冰(冰岛) 
     */
    String CUSTOMS_PREFERENCE_CODE_CIFTA = "CIFTA";
                    
    /**
     * FTA 优惠协定代码: 中瑞(瑞士) 
     */
    String CUSTOMS_PREFERENCE_CODE_CHFTA = "CHFTA";
                    
    /**
     * FTA 优惠协定代码: 区域全面经济伙伴关系 
     */
    String CUSTOMS_PREFERENCE_CODE_RCEP = "RCEP";
                    
    /**
     * FTA 优惠协定代码: 普惠制 
     */
    String CUSTOMS_PREFERENCE_CODE_GSP = "GSP";
                    
    /**
     * FTA 优惠协定代码: 其他 
     */
    String CUSTOMS_PREFERENCE_CODE_OTHER = "OTHER";
                    
    /**
     * 仓库类型: 普通仓 
     */
    String WAREHOUSE_TYPE_NORMAL = "NORMAL";
                    
    /**
     * 仓库类型: 在途仓 
     */
    String WAREHOUSE_TYPE_IN_TRANSIT = "IN_TRANSIT";
                    
    /**
     * 仓库类型: 报废仓 
     */
    String WAREHOUSE_TYPE_SCRAP = "SCRAP";
                    
    /**
     * 仓库类型: 受托代销仓 
     */
    String WAREHOUSE_TYPE_CONSIGNMENT_IN = "CONSIGNMENT_IN";
                    
    /**
     * 仓库类型: 委托代销仓 
     */
    String WAREHOUSE_TYPE_CONSIGNMENT_OUT = "CONSIGNMENT_OUT";
                    
    /**
     * 仓库类型: 保税仓 
     */
    String WAREHOUSE_TYPE_BONDED = "BONDED";
                    
    /**
     * 科目类别: 资产 
     */
    String SUBJECT_CLASS_ASSET = "ASSET";
                    
    /**
     * 科目类别: 负债 
     */
    String SUBJECT_CLASS_LIABILITY = "LIABILITY";
                    
    /**
     * 科目类别: 权益 
     */
    String SUBJECT_CLASS_EQUITY = "EQUITY";
                    
    /**
     * 科目类别: 收入 
     */
    String SUBJECT_CLASS_INCOME = "INCOME";
                    
    /**
     * 科目类别: 费用 
     */
    String SUBJECT_CLASS_EXPENSE = "EXPENSE";
                    
    /**
     * 科目类别: 成本 
     */
    String SUBJECT_CLASS_COST = "COST";
                    
    /**
     * 科目余额方向: 借方 
     */
    String SUBJECT_DIRECTION_DEBIT = "DEBIT";
                    
    /**
     * 科目余额方向: 贷方 
     */
    String SUBJECT_DIRECTION_CREDIT = "CREDIT";
                    
    /**
     * 科目余额类型: 借余 
     */
    String BALANCE_TYPE_DEBIT = "DEBIT";
                    
    /**
     * 科目余额类型: 贷余 
     */
    String BALANCE_TYPE_CREDIT = "CREDIT";
                    
    /**
     * 科目余额类型: 借贷双余 
     */
    String BALANCE_TYPE_BOTH = "BOTH";
                    
    /**
     * 组织类型: 集团 
     */
    String ORG_TYPE_GROUP = "GROUP";
                    
    /**
     * 组织类型: 公司 
     */
    String ORG_TYPE_COMPANY = "COMPANY";
                    
    /**
     * 组织类型: 分公司 
     */
    String ORG_TYPE_BRANCH = "BRANCH";
                    
    /**
     * 组织类型: 部门 
     */
    String ORG_TYPE_DEPARTMENT = "DEPARTMENT";
                    
    /**
     * 组织类型: 车间 
     */
    String ORG_TYPE_WORKSHOP = "WORKSHOP";
                    
    /**
     * 组织类型: 门店 
     */
    String ORG_TYPE_STORE = "STORE";
                    
    /**
     * 核算表性质: 财务账(法定账) 
     */
    String ACCT_SCHEMA_NATURE_FINANCIAL = "FINANCIAL";
                    
    /**
     * 核算表性质: 管理账(内部账) 
     */
    String ACCT_SCHEMA_NATURE_MANAGEMENT = "MANAGEMENT";
                    
    /**
     * 核算表性质: 税务账 
     */
    String ACCT_SCHEMA_NATURE_TAX = "TAX";
                    
    /**
     * 核算表性质: 合并账 
     */
    String ACCT_SCHEMA_NATURE_CONSOLIDATION = "CONSOLIDATION";
                    
    /**
     * 核算表性质: 预算账 
     */
    String ACCT_SCHEMA_NATURE_BUDGET = "BUDGET";
                    
    /**
     * 税种: 增值税 
     */
    String TAX_TYPE_VAT = "VAT";
                    
    /**
     * 税种: 销售税 
     */
    String TAX_TYPE_SALES_TAX = "SALES_TAX";
                    
    /**
     * 税种: 预提税 
     */
    String TAX_TYPE_WITHHOLDING = "WITHHOLDING";
                    
    /**
     * 税种: 关税 
     */
    String TAX_TYPE_CUSTOMS = "CUSTOMS";
                    
    /**
     * 税种: 其他 
     */
    String TAX_TYPE_OTHER = "OTHER";
                    
    /**
     * 结算类型: 现金 
     */
    String SETTLEMENT_TYPE_CASH = "CASH";
                    
    /**
     * 结算类型: 银行转账 
     */
    String SETTLEMENT_TYPE_BANK_TRANSFER = "BANK_TRANSFER";
                    
    /**
     * 结算类型: 票据 
     */
    String SETTLEMENT_TYPE_NOTE = "NOTE";
                    
    /**
     * 结算类型: 抵扣 
     */
    String SETTLEMENT_TYPE_OFFSET = "OFFSET";
                    
    /**
     * 结算类型: 在线支付 
     */
    String SETTLEMENT_TYPE_ONLINE = "ONLINE";
                    
    /**
     * 银行账户类型: 活期 
     */
    String BANK_ACCOUNT_TYPE_DEMAND = "DEMAND";
                    
    /**
     * 银行账户类型: 定期 
     */
    String BANK_ACCOUNT_TYPE_FIXED = "FIXED";
                    
    /**
     * 银行账户类型: 信用账户 
     */
    String BANK_ACCOUNT_TYPE_CREDIT = "CREDIT";
                    
    /**
     * 银行账户类型: 其他 
     */
    String BANK_ACCOUNT_TYPE_OTHER = "OTHER";
                    
    /**
     * 地址类型: 账单地址 
     */
    String ADDRESS_TYPE_BILLING = "BILLING";
                    
    /**
     * 地址类型: 发货地址 
     */
    String ADDRESS_TYPE_SHIPPING = "SHIPPING";
                    
    /**
     * 地址类型: 注册地址 
     */
    String ADDRESS_TYPE_REGISTERED = "REGISTERED";
                    
    /**
     * 地址类型: 其他 
     */
    String ADDRESS_TYPE_OTHER = "OTHER";
                    
    /**
     * 批次选择策略: 先进先出 
     */
    String BATCH_STRATEGY_FIFO = "FIFO";
                    
    /**
     * 批次选择策略: 先到期先出 
     */
    String BATCH_STRATEGY_FEFO = "FEFO";
                    
    /**
     * 批次选择策略: 手工指定 
     */
    String BATCH_STRATEGY_MANUAL = "MANUAL";
                    
    /**
     * 供应商准入类型: 新供应商准入 
     */
    String SUPPLIER_APPROVAL_TYPE_NEW = "NEW";
                    
    /**
     * 供应商准入类型: 续期 
     */
    String SUPPLIER_APPROVAL_TYPE_RENEWAL = "RENEWAL";
                    
    /**
     * 供应商准入状态: 已申请 
     */
    String SUPPLIER_APPROVAL_STATUS_APPLIED = "APPLIED";
                    
    /**
     * 供应商准入状态: 已批准 
     */
    String SUPPLIER_APPROVAL_STATUS_APPROVED = "APPROVED";
                    
    /**
     * 供应商准入状态: 试用期 
     */
    String SUPPLIER_APPROVAL_STATUS_PROBATION = "PROBATION";
                    
    /**
     * 供应商准入状态: 已暂停 
     */
    String SUPPLIER_APPROVAL_STATUS_SUSPENDED = "SUSPENDED";
                    
    /**
     * 供应商准入状态: 已驳回 
     */
    String SUPPLIER_APPROVAL_STATUS_REJECTED = "REJECTED";
                    
}
