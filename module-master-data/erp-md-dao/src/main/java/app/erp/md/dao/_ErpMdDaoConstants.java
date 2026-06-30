package app.erp.md.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpMdDaoConstants {
    
    /**
     * 物料类型: 商品 
     */
    int MATERIAL_TYPE_GOODS = 10;
                    
    /**
     * 物料类型: 原材料 
     */
    int MATERIAL_TYPE_RAW_MATERIAL = 20;
                    
    /**
     * 物料类型: 半成品 
     */
    int MATERIAL_TYPE_SEMI_PRODUCT = 30;
                    
    /**
     * 物料类型: 产成品 
     */
    int MATERIAL_TYPE_FINISHED_PRODUCT = 40;
                    
    /**
     * 物料类型: 服务 
     */
    int MATERIAL_TYPE_SERVICE = 50;
                    
    /**
     * 物料类型: 包装物 
     */
    int MATERIAL_TYPE_PACKAGING = 60;
                    
    /**
     * 物料类型: 消耗品 
     */
    int MATERIAL_TYPE_CONSUMABLE = 70;
                    
    /**
     * 启用状态: 启用 
     */
    int ACTIVE_STATUS_ACTIVE = 10;
                    
    /**
     * 启用状态: 停用 
     */
    int ACTIVE_STATUS_INACTIVE = 20;
                    
    /**
     * 存货计价方法: 移动加权平均 
     */
    int COST_METHOD_MOVING_AVERAGE = 10;
                    
    /**
     * 存货计价方法: 全月一次加权平均 
     */
    int COST_METHOD_WEIGHTED_AVERAGE = 20;
                    
    /**
     * 存货计价方法: 先进先出 
     */
    int COST_METHOD_FIFO = 30;
                    
    /**
     * 存货计价方法: 后进先出 
     */
    int COST_METHOD_LIFO = 40;
                    
    /**
     * 存货计价方法: 标准成本 
     */
    int COST_METHOD_STANDARD = 50;
                    
    /**
     * 存货计价方法: 具体辨认(个别计价) 
     */
    int COST_METHOD_SPECIFIC = 60;
                    
    /**
     * 存货计价方法: 批次 
     */
    int COST_METHOD_BATCH = 70;
                    
    /**
     * 价格校验级别: 不校验 
     */
    int PRICE_VALIDATION_OFF = 10;
                    
    /**
     * 价格校验级别: 警告放行 
     */
    int PRICE_VALIDATION_WARN = 20;
                    
    /**
     * 价格校验级别: 强制拦截 
     */
    int PRICE_VALIDATION_HARD = 30;
                    
    /**
     * 往来单位类型: 客户 
     */
    int PARTNER_TYPE_CUSTOMER = 10;
                    
    /**
     * 往来单位类型: 供应商 
     */
    int PARTNER_TYPE_SUPPLIER = 20;
                    
    /**
     * 往来单位类型: 客户且供应商 
     */
    int PARTNER_TYPE_BOTH = 30;
                    
    /**
     * 往来单位类型: 员工(内部往来) 
     */
    int PARTNER_TYPE_EMPLOYEE = 40;
                    
    /**
     * 仓库类型: 普通仓 
     */
    int WAREHOUSE_TYPE_NORMAL = 10;
                    
    /**
     * 仓库类型: 在途仓 
     */
    int WAREHOUSE_TYPE_IN_TRANSIT = 20;
                    
    /**
     * 仓库类型: 报废仓 
     */
    int WAREHOUSE_TYPE_SCRAP = 30;
                    
    /**
     * 仓库类型: 受托代销仓 
     */
    int WAREHOUSE_TYPE_CONSIGNMENT_IN = 40;
                    
    /**
     * 仓库类型: 委托代销仓 
     */
    int WAREHOUSE_TYPE_CONSIGNMENT_OUT = 50;
                    
    /**
     * 仓库类型: 保税仓 
     */
    int WAREHOUSE_TYPE_BONDED = 60;
                    
    /**
     * 科目类别: 资产 
     */
    int SUBJECT_CLASS_ASSET = 10;
                    
    /**
     * 科目类别: 负债 
     */
    int SUBJECT_CLASS_LIABILITY = 20;
                    
    /**
     * 科目类别: 权益 
     */
    int SUBJECT_CLASS_EQUITY = 30;
                    
    /**
     * 科目类别: 收入 
     */
    int SUBJECT_CLASS_INCOME = 40;
                    
    /**
     * 科目类别: 费用 
     */
    int SUBJECT_CLASS_EXPENSE = 50;
                    
    /**
     * 科目类别: 成本 
     */
    int SUBJECT_CLASS_COST = 60;
                    
    /**
     * 科目余额方向: 借方 
     */
    int SUBJECT_DIRECTION_DEBIT = 10;
                    
    /**
     * 科目余额方向: 贷方 
     */
    int SUBJECT_DIRECTION_CREDIT = 20;
                    
    /**
     * 科目余额类型: 借余 
     */
    int BALANCE_TYPE_DEBIT = 10;
                    
    /**
     * 科目余额类型: 贷余 
     */
    int BALANCE_TYPE_CREDIT = 20;
                    
    /**
     * 科目余额类型: 借贷双余 
     */
    int BALANCE_TYPE_BOTH = 30;
                    
    /**
     * 组织类型: 集团 
     */
    int ORG_TYPE_GROUP = 10;
                    
    /**
     * 组织类型: 公司 
     */
    int ORG_TYPE_COMPANY = 20;
                    
    /**
     * 组织类型: 分公司 
     */
    int ORG_TYPE_BRANCH = 30;
                    
    /**
     * 组织类型: 部门 
     */
    int ORG_TYPE_DEPARTMENT = 40;
                    
    /**
     * 组织类型: 车间 
     */
    int ORG_TYPE_WORKSHOP = 50;
                    
    /**
     * 组织类型: 门店 
     */
    int ORG_TYPE_STORE = 60;
                    
    /**
     * 核算表性质: 财务账(法定账) 
     */
    int ACCT_SCHEMA_NATURE_FINANCIAL = 10;
                    
    /**
     * 核算表性质: 管理账(内部账) 
     */
    int ACCT_SCHEMA_NATURE_MANAGEMENT = 20;
                    
    /**
     * 核算表性质: 税务账 
     */
    int ACCT_SCHEMA_NATURE_TAX = 30;
                    
    /**
     * 核算表性质: 合并账 
     */
    int ACCT_SCHEMA_NATURE_CONSOLIDATION = 40;
                    
    /**
     * 核算表性质: 预算账 
     */
    int ACCT_SCHEMA_NATURE_BUDGET = 50;
                    
    /**
     * 税种: 增值税 
     */
    int TAX_TYPE_VAT = 10;
                    
    /**
     * 税种: 销售税 
     */
    int TAX_TYPE_SALES_TAX = 20;
                    
    /**
     * 税种: 预提税 
     */
    int TAX_TYPE_WITHHOLDING = 30;
                    
    /**
     * 税种: 关税 
     */
    int TAX_TYPE_CUSTOMS = 40;
                    
    /**
     * 税种: 其他 
     */
    int TAX_TYPE_OTHER = 50;
                    
    /**
     * 结算类型: 现金 
     */
    int SETTLEMENT_TYPE_CASH = 10;
                    
    /**
     * 结算类型: 银行转账 
     */
    int SETTLEMENT_TYPE_BANK_TRANSFER = 20;
                    
    /**
     * 结算类型: 票据 
     */
    int SETTLEMENT_TYPE_NOTE = 30;
                    
    /**
     * 结算类型: 抵扣 
     */
    int SETTLEMENT_TYPE_OFFSET = 40;
                    
    /**
     * 结算类型: 在线支付 
     */
    int SETTLEMENT_TYPE_ONLINE = 50;
                    
    /**
     * 银行账户类型: 活期 
     */
    int BANK_ACCOUNT_TYPE_DEMAND = 10;
                    
    /**
     * 银行账户类型: 定期 
     */
    int BANK_ACCOUNT_TYPE_FIXED = 20;
                    
    /**
     * 银行账户类型: 信用账户 
     */
    int BANK_ACCOUNT_TYPE_CREDIT = 30;
                    
    /**
     * 银行账户类型: 其他 
     */
    int BANK_ACCOUNT_TYPE_OTHER = 40;
                    
    /**
     * 地址类型: 账单地址 
     */
    int ADDRESS_TYPE_BILLING = 10;
                    
    /**
     * 地址类型: 发货地址 
     */
    int ADDRESS_TYPE_SHIPPING = 20;
                    
    /**
     * 地址类型: 注册地址 
     */
    int ADDRESS_TYPE_REGISTERED = 30;
                    
    /**
     * 地址类型: 其他 
     */
    int ADDRESS_TYPE_OTHER = 40;
                    
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
                    
}
