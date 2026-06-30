# 全模块协同审计计划

> 审计内容：模块边界正确性、标准字段一致性、跨域数据流连续性、状态机协同正确性、业财一体完整性、依赖 DAG 合规性

## 审计维度总表

| 维度 | 检查项 | 方法 | 通过标准 |
|------|--------|------|---------|
| A. 模块边界 | 每个功能语义应属于且仅属于一个模块 | 遍历所有模块职责声明，检查是否有重叠或空缺 | 无重叠、无空缺，每个功能归属明确 |
| B. 命名约定 | 模块名/表前缀/实体前缀/appName 一致 | 对照 `domain-module-split-analysis.md` 命名表 | 全部模块遵守同一规则 |
| C. 标准字段 | 所有业务实体携带 orgId/businessDate/posted/多币种四件套/审计字段 | 扫描 ORM XML 对应字段 | 符合 `domain-design-guidelines.md` §14 |
| D. 三轴状态 | docStatus + approveStatus + posted 三轴分离 | 扫描所有状态机文档 + ORM 状态字段 | 统一语义，不混用 |
| E. 业财一体 | 所有需过账的模块定义了 businessType，注册了 IErpFinAcctDocProvider | 对照 flow-overview + posting.md businessType 表 | 全覆盖，businessType 命名规范 |
| F. 跨域 DAG | 模块间依赖关系是无环有向图 | 扫描 ORM cross-module ref + I*Biz 引用 | 符合 master-data → ... → finance 方向 |
| G. 业务流连续 | 端到端业务流（P2P/O2C/MFG/Q2C）无断裂 | 遍历各域 state-machine 的 APPROVED 触发事件 | 每条触发都有上下游承接 |
| H. 状态机协同 | 上游状态迁移正确触发下游单据创建 | 跨域状态映射表（domain-design-guidelines §16.5）全覆盖 | 每对上下游映射有文档记录 |

---

## 各模块待核查清单

### 原有 10 模块

| 模块 | 待核查重点 |
|------|-----------|
| master-data | 是否所有模块需要的共享字典/主数据足量定义；orgId 一致；是否有不应在此的字段 |
| purchase | 业务单过账 businessType 是否完整；三单匹配跨域是否正确；退货流程是否连续 |
| sales | 同 purchase；合同与合同模块的边界是否清晰（contract 模块接管） |
| inventory | StockBalance/Ledger 标准字段是否完整；VMI 扩展的 ownerId 是否合规；条码集成引用 |
| finance | posting.md businessType 表是否覆盖所有模块；posted 范式是否一致 |
| manufacturing | WorkOrder 状态机 10 态与 JobCard、OperationOrder（APS）的关系是否清晰；MRP→CRP→APS 协作 |
| quality | NCR 状态机已含 ESCALATED_TO_RECALL？质检跨域触发是否完整 |
| assets | 折旧凭证 businessType 定义；资产处置与 finance 的衔接 |
| projects | 状态集已修正（OPEN/ON_HOLD）？成本归集引用 expense-claim.md 正确？ |
| maintenance | 维护工单与 customer-service 工单的边界？设备停机对 CRP/APS 的约束？ |

### 新增 8 模块

| 模块 | 待核查重点 |
|------|-----------|
| crm | 转化结果弱指针指向 sales/Quotation 是否正确；Event 与 maintanence/cs 无重叠 |
| customer-service | Ticket 触发 NCR 升级路径？SLA 与 system-config 集成？ |
| human-resource | 工时表 Timesheet 与 projects 域 cost-collection 的衔接；薪酬凭证过账 businessType 注册 |
| aps | OperationOrder 与 manufacturing/WorkOrder + JobCard 的三层关系；ATP/CTP 回写 sales 的机制 |
| contract | InvoicePlan 生成 AP/AR invoice 的机制；ConsumptionLine 与 inventory 出库的关联 |
| drp | 补货建议生成 TransferOrder（inventory）或 PurchaseOrder（purchase）的机制 |
| logistics | 发运单与 sales/Delivery 通过弱指针关联，运费双路径是否正确 |
| b2b | 信封状态机与 purchase/sales 单据的联动；ASN 入站→purchase/Receive 的衔接 |

---

## 业务流核查清单

### 流 1：采购到付款（P2P）

```
请购 → 询价 → 报价 → 采购订单 → 入库 → 质检 → 发票 → 三单匹配 → 付款 → 核销
 purchase purchase purchase purchase  inventory quality  purchase purchase  purchase  finance
```

检查点：每一步状态迁移→下游单据创建是否连续。businessType: PURCHASE_INPUT, AP_INVOICE, PAYMENT

### 流 2：订单到收款（O2C）

```
报价 → 销售订单 → 出库 → 发货 → 发票 → 收款 → 核销
sales   sales     sales  logistics sales  sales  finance
```

检查点：出库→发货（sales→logistics）的弱指针；发货单运费→FREIGHT 凭证。businessType: SALES_OUTPUT, AR_INVOICE, RECEIPT

### 流 3：制造到入库（M2I）

```
MRP → 工单 → 领料 → 报工 → 质检 → 完工入库 → 成本结转
mfg  mfg   inv    mfg   quality  inv       finance
```

检查点：MRP→WorkOrder→JobCard→OperationOrder(APS) 四层分解；领料出库→StockMove；businessType: MANUFACTURING_COST

### 流 4：CRM→合同→订单→发货→售后

```
线索→商机→报价单→合同→订单→出库→发运→售后工单
crm  crm  sales  contract sales sales logistics cs
```

检查点：crm→sales 转化弱指针；contract→sales InvoicePlan 触发发票；logistics→cs 发运异常升级工单

---

## 命名约定对照表

| 属性 | master-data | purchase | sales | inventory | finance | ...（原有） | crm | cs | hr | aps | contract | drp | logistics | b2b |
|------|-------------|----------|-------|-----------|---------|------------|-----|----|----|-----|----------|-----|-----------|-----|
| appName | erp-md | erp-pur | erp-sal | erp-inv | erp-fin | ... | erp-crm | erp-cs | erp-hr | erp-aps | erp-ct | erp-drp | erp-log | erp-b2b |
| 表前缀 | erp_md_ | erp_pur_ | erp_sal_ | erp_inv_ | erp_fin_ | ... | erp_crm_ | erp_cs_ | erp_hr_ | erp_aps_ | erp_ct_ | erp_inv_drp_ | erp_log_ | erp_b2b_ |
| 实体前缀 | ErpMd | ErpPur | ErpSal | ErpInv | ErpFin | ... | ErpCrm | ErpCs | ErpHr | ErpAps | ErpCt | ErpInvDrp | ErpLog | ErpB2b |
| 模块目录 | module-master-data | module-purchase | module-sales | module-inventory | module-finance | ... | module-crm | module-cs | module-hr | module-aps | module-contract | module-drp | module-logistics | module-b2b |
