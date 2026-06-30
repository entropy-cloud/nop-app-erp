---
分析日期: 2026-06-30
类型: 高级场景设计深度对比（计划 03 的设计依据输入）
状态: 已完成
方法: 三层挖掘（erp-survey 调研报告 → /Users/abc/sources/erp/ 源码回查 → 提炼落地）
服务对象: docs/plans/03-advanced-scenario-design-gap-fill.md（Phase 1-3 执行项的设计依据）
证据图例: 🟢=源码回查实测 / 🟡=调研报告已记载（经复核）/ ⚪=领域常识（开源零覆盖）
---

# 高级场景设计深度对比：为计划 03 提供设计依据

> **本文档定位**：计划 `03-advanced-scenario-design-gap-fill.md` 经独立草案审查通过后，发现"执行项只列了文件名与 erp-survey 行号引用，未把设计洞察提炼为设计依据"。本文档通过**三层挖掘**（调研报告 + 源码回查 + 提炼），为 Phase 1-3 的 9 个场景补充"最佳实践 + 反模式 + 落地方案要点 + 证据"，作为各设计文档撰写时的**设计依据输入**。
>
> **核心价值**：源码回查发现了多处计划现有引用的"过度引申"（票据证据、TMS 主证、EDI 主证），以及 1 处实体归属错误（供应商评分卡）、若干设计决策未裁决。本文档是计划 03 实质修订的依据。

## 0. 跨场景基线：两个统一范式（先定锚）

### 0.1 独立扩展工程范式（适用 Phase 3 全部 + Phase 2 VMI/召回）

权威样板：`docs/design/l10n/cn-golden-tax.md`（独立 `module-l10n-cn` + 独立 appName + **凭证指针反查核心域，不污染核心实体**）。

| 要素 | 证据 | 落地约定 |
|---|---|---|
| 独立 Maven 工程 + 独立 appName | `cn-golden-tax.md:9,12-19` | module-crm/module-logistics/module-b2b 各自独立 |
| **不污染核心实体** | `cn-golden-tax.md:109,113,120` + `domain-module-split-analysis.md:138-150` | 用 `relatedBillType/relatedBillCode` 弱指针反查 sales/purchase/inventory，**核心域零字段新增** |

> ⚠️ **架构冲突裁决**：`l10n-strategy.md:166-185` 描述的"Delta ORM 片段给核心实体加列"与 `cn-golden-tax.md:109` 的"凭证指针反查、不改核心实体"矛盾。**统一采用后者**（与跨域 DAG 一致）。Phase 2/3 各文档须显式声明"核心实体零污染"。

### 0.2 SPI 提供者注册中心统一范式（适用 4 个场景）

🟢 源码回查发现：本仓已有的 `IErpFinAcctDocProvider`（业财过账）与 Metasfresh `ShipperGatewayServicesRegistry.java:43-125`（TMS）、`AcctDocRegistry.java:25-33` 是**同一模式**。建议在 `docs/architecture/` 收敛为统一范式文档（避免每个 owner doc 重复描述）：

```
通用形态：
  interface IXxxProvider { String getCode(); ... }          // 每个实现一个 code
  @Inject Map<String, IXxxProvider> providers;              // Nop IoC 按 bean 自动聚合
  // 新增实现 = 1 个 @Component bean，零改 commons
```

**适用场景**：业财过账（IErpFinAcctDocProvider）、TMS 承运商网关、EDI 格式、CRM 转化服务（内部）。

---

## 一、Phase 1：财务扩展（2 场景）

### 1.1 费用报销 / 员工借款 / 备用金

**最佳实践**（🟢 Odoo hr_expense 源码 + 🟢 ERPNext payment_entry 源码）：
1. **报销对方科目 = AP-员工**（员工垫付模式）—— Odoo `hr_expense.py:1780-1805` `_get_expense_account_destination`：`payment_mode=own_account` → `partner.property_account_payable_id`
2. **payment_mode 区分垫付方**（own_account/company_account）决定对方科目 —— Odoo `hr_expense.py:244-253`
3. **价税分离三件套**（untaxed/tax/total）便于进项税抵扣 —— Odoo `hr_expense.py:154-187`
4. **项目/成本中心归集作为报销行原生维度** —— Odoo `_inherit=['analytic.mixin']`
5. **预付款走单独科目，不污染应收应付余额** —— ERPNext `payment_entry.py:235-274` `book_advance_payments_in_separate_party_account`
6. **员工借款/还款复用收付款单 + party 多态**（party_type=Employee）—— ERPNext `payment_entry.py:548`，无需独立还款单

**反模式**：
- ⛔ Odoo 单轴 state（draft→submitted→approved→posted→in_payment→paid 七态串一轴，`hr_expense.py:122-142`）—— **本项目坚持三轴分离**（docStatus + approveStatus + posted + paidStatus）
- ⛔ 把员工借款塞进应付发票表（借款方向=其他应收，与应付相反）
- ⛔ 管伊佳逗号字符串多账户（business-design-takeaways.md 主题 8.2）—— 多费用类别用子表

**落地要点**：
- 实体：`ErpFinExpenseClaim`/`Line`（报销）、`ErpFinEmployeeAdvance`（借款，含 advanceType=IMPREST 备用金特化）—— 归 finance 域
- paymentMode 字段 + 对方科目决策（own_account→应付-员工 / company_account→银行待结算）
- 借款科目方向=其他应收款-员工预支（**非应付**）
- 还款复用 `ErpFinPayment`（partyType=EMPLOYEE）+ `ErpFinReconciliation`（sourceBillType=EMPLOYEE_ADVANCE），**不建独立还款单**
- businessType 新增：EXPENSE_CLAIM / EMPLOYEE_ADVANCE / EMPLOYEE_ADVANCE_SETTLE
- 预算控制钩子：APPROVED 前同步调 `IErpFinBudgetControlBiz.check`（budget.md:74）
- 证据：🟢 Odoo hr_expense 源码实测；🟢 ERPNext payment/advance 源码实测；⚪ 备用金属领域常识（开源零覆盖）

### 1.2 资金管理 / 票据

**⚠️ 计划现有证据修正（关键）**：03 计划 :65 引用 `metasfresh.md:83 BankingAcctDocProvider` + `redragon-erp.md:41 银行字段` 作为票据证据 —— 源码回查确认**两者均非票据证据**：
- 🟢 `BankingAcctDocProvider.java:40`（实测）只注册 `C_BankStatement`（银行对账单），**不处理票据**
- 🟢 `ApInvoiceHead.java:207-229`（实测）是收款银行账户三件套，**无票据字段**
- 🟢 iDempiere `X_C_Payment.java:2259-2271`（实测）TenderType 6 种无 Notes，西方支票文化
- **结论**：4 个开源 ERP 均无中式承兑汇票实体。treasury.md 须诚实标注"开源零覆盖 + 领域常识；科目分解范式参考 Metasfresh Doc_BankStatement"

**最佳实践**（🟢 Metasfresh Doc_BankStatement 科目分解 + ⚪ 中国会计准则）：
1. **科目分解五件套**：资产/在途/手续费/利息/汇兑损益 —— 🟢 `Doc_BankStatement.java:206-547`（createFacts_TrxAmt/BankFee/Interest/CurrencyExchangeGainOrLoss）
2. **票据独立成表走 SPI 过账**（反 iDempiere 把 Check 塞进 tenderType）—— ⚪ 领域常识 + 🟢 iDempiere 反例证据
3. **应收票据 vs 应付票据方向对称**（应收=资产/借方，应付=负债/贷方）—— ⚪ 会计准则
4. **贴现息走财务费用-利息支出**（非冲减应收票据）—— 🟢 `Doc_BankStatement.java:384 PayBankFee_Acct` / `:473 B_InterestExp_Acct`
5. **贴现外币产生汇兑损益** —— 🟢 `Doc_BankStatement.java:482-547`
6. **授信额度控制**（开银承占用授信）—— 🟢 ERPNext `bank_guarantee` 概念 + ⚪ 领域规则

**反模式**：
- ⛔ 把票据塞进付款表 tenderType（iDempiere X_C_Payment.java:2267 Check）—— 丢失生命周期
- ⛔ 贴现息冲减应收票据（应走财务费用）

**落地要点**：
- 实体：`ErpFinNotesReceivable`（应收票据，7 态：RECEIVED/ENDORSED/DISCOUNTED/COLLECTION_PENDING/HONORED/DISHONORED/WRITE_OFF）、`ErpFinNotesPayable`（应付票据）、`ErpFinNotesDiscount`（贴现明细）、`ErpFinCreditFacility`（授信）、`ErpFinCashForecast`（**物化视图**，类比 ErpFinBankLedgerLine）
- businessType：NOTES_RECEIVABLE_RECEIVED/DISCOUNTED/ENDORSED/COLLECTION + NOTES_PAYABLE_ISSUED/HONORED + CREDIT_FACILITY_INTEREST
- 贴现凭证复用 Metasfresh 五科目分解：借 银行存款(实得) / 借 财务费用-贴现息 / [借/贷] 汇兑损益 / 贷 应收票据(票面)
- 与银行对账解耦（bank-reconciliation.md:11 已明确）
- 开银承前同步校验 creditFacility.availableAmount

---

## 二、Phase 2：制造/采购/库存/质量扩展（4 场景）

### 2.1 CRP / APS

**最佳实践**（🟢 Odoo mrp_workcenter 源码）：
1. **产能四要素分离**：工作中心日历（时钟）+ 按产品并行产能 + 换模/清理时间 + 效率系数 —— 🟢 `mrp_workcenter.py:30,78,81` + `mrp.workcenter.capacity`(`:613-636`)
2. **负荷上限取自日历出勤时长**（非硬编码 capacity 字段）—— 🟢 `mrp_workcenter.py:157` `load_limit`
3. **按产品维度定义产能**（同工作中心不同产品不同产能/换模，支持混线）—— 🟢 `_get_capacity`(`:427-437`)
4. **排产 = 可用时段 vs 已占用时段 Gantt 填充** —— 🟢 `mrp_workcenter.py:360-364`

**反模式**：
- ⛔ 产能硬编码为单一标量字段（`capacity=100`）—— Odoo 反例特意按 product 查 capacity_ids
- ⛔ 把 CRP 当 APS 承诺 —— **🟢 源码确认开源无真 APS**（Odoo 仅前向排产，ERPNext 仅日期计划 `production_plan.json` 无 capacity/finite 字段）

**落地要点**：
- 实体：`ErpMfgWorkcenterCapacity`（按产品产能）、`ErpMfgWorkcenterCalendar`（日历/班次）、`ErpMfgCrpLoad`（负荷快照行，弱指针关联 WorkOrder/JobCard）
- **CRP 不产生凭证**（计划层）；超负荷触发的加班/外协才过账（复用 WorkOrder laborCost / SubcontractOrder）
- **P1 不引入排产方案单据**（仅负荷报表 + WorkOrder 已有 plannedStartDate/EndDate 承接），避免新增状态机
- **诚实标注**：APS（优化求解）属后续 follow-up；CRP 与 MRP 解耦（mrp.md:10 边界）
- 现有 `ErpMfgWorkcenter.capacity/workHoursPerDay` 是"默认标量产能"，按产品产能需新建 WorkcenterCapacity 子实体

### 2.2 供应商评分卡 / AVL 准入

**⚠️ 计划实体归属修正（实质）**：03 计划 :93 建议放 master-data（`ErpMdSupplierScore`/`ErpMdSupplierApproval`）。**应拆分**：
- AVL 准入（资格主数据）→ **master-data**（`ErpMdSupplierApproval`）
- 评分卡周期数据（业务绩效）→ **purchase**（`ErpPurSupplierScorecard*`）
- 理由：评分卡引用 RFQ/PO/质检单等采购链数据，属采购绩效评估产物（domain-design-guidelines.md §1.1 单一职责）

**最佳实践**（🟢 ERPNext 8-doctype 体系源码实测）：
1. **评分 = 维度(criteria) × 公式(formula) × 权重(weight)，公式引用变量(variable)从业务 path 取值** —— 🟢 `supplier_scorecard_criteria`(formula/weight) + `_variable`(path)
2. **周期化评估**（起止日期 + total_score）—— 评分是时点快照，非实时累加 —— 🟢 `supplier_scorecard_period`
3. **评分 → 评级档位(standing) → RFQ 三档联动**（warn/hold/prevent）—— 🟢 `supplier_scorecard` warn_rfqs/hold_rfqs/prevent_rfqs

**反模式**：
- ⛔ 评分实时累加在供应商主数据单字段（`ErpMdPartner.qualityScore`）—— ERPNext 特意做 period 快照表
- ⛔ 公式硬编码 Java —— 用 nop 规则引擎/DSL

**落地要点**：
- 实体：`ErpMdSupplierApproval`（master-data，资格生命周期 APPLIED/APPROVED/PROBATION/SUSPENDED）+ `ErpPurSupplierScorecard`/`Criteria`/`Variable`（purchase，周期评分）
- 数据源已存在：🟢 `ErpQaInspection.supplierId`(quality orm.xml:139，质量合格率) + 🟢 `ErpPurSupplierPriceList`(orm.xml:277，价格竞争力) + PO/Receive 交货日期（按时交货率）
- 评分卡**不过账**；standing=RED → 写 SupplierApproval=SUSPENDED → RFQ 创建校验
- 证据升级：🟢 ERPNext 是 8-doctype 完整体系（非单 doctype），路径 `erpnext/buying/doctype/supplier_scorecard*/`

### 2.3 VMI / 寄售 / 受托代销

**⚠️ 证据精确化**：03 计划 :95 已声明弱证据。源码回查**进一步坐实**：
- 🟢 `stock_quant.py:74` owner_id 字段存在，`:155` 唯一键含 owner
- 🟢 `res_config_settings.py:21` Consignment 是 **feature group 开关**（默认关）
- 🟢 **owner 不参与 stock.rule**（grep `stock_rule.py` owner_id 空命中）—— **Odoo 无 VMI 自动结算闭环**，结算靠 EDI/发票层
- **结论**：本项目"所有权转移自动触发 AP_INVOICE"是**自建能力，非开源借鉴**；只能借鉴 owner 维度建模

**最佳实践**：
1. **owner 作为库存维度独立轴**（与 product/lot/location 并列）—— 🟢 `stock_quant.py:74,155`
2. **owner 是 feature group 开关**（默认关，避免非 VMI 用户被字段干扰）—— 🟢 `res_config_settings.py:21`
3. **出库按 owner 匹配 quant**（防止把供应商寄售库存发给普通订单）—— 🟢 `stock_quant.py:144-145`

**反模式**：
- ⛔ 误以为 Odoo 有完整 VMI 结算闭环 —— 文档不得声称"借鉴 Odoo VMI 自动结算"
- ⛔ **owner 转移用普通移动单**（物理位置变）—— 所有权转移是法权变更，物理可能不变，须专用单据
- ⛔ owner 塞进物料主数据（同物料可能部分 OWNED 部分 VMI 并存）

**落地要点**：
- `ErpInvStockBalance`/`StockLedger` 加 `ownerId` + `ownershipType`（字典 OWNED/VMI_SUPPLIER/CONSIGNMENT_OUT/CUSTOMER_PROVIDED）
- `ErpInvOwnershipTransfer`（所有权转移单，**sourceLocId=destLocId 物理位置不变**）触发 AP_INVOICE
- businessType: OWNERSHIP_TRANSFER —— 借 存货(自有) / 贷 应付-供应商
- 配置 `erp-inv.ownership-tracking-enabled`（默认 false，对应 Odoo feature group）

### 2.4 批次召回事件

**⚠️ 证据精确化**：03 计划 :97 已声明零命中。源码回查精确化：
- 🟢 `grep -rli "recall" /Users/abc/sources/erp/` ~30 文件，**全部技术语义或量具校准召回，无一例批次召回实体**
- 🟢 唯一业务相关：**Carbon 量具校准召回**（`carbon/.../quality.ts:26514`）"量具超差→产品召回→通知客户"逻辑，写在质量文档，**无独立 Recall 实体**
- 🟢 Carbon `NonConformanceIssue` 关联广度（supplierId/customerId/jobOperationId/trackedEntityId/approvalRequirements `quality.models.ts:462-485`）—— **召回底层能力（目标定位）已有零件**

**最佳实践**：
1. 召回 = 事件聚合层，复用已有底层（trace-chain + NCR + 退货），不重建 —— 🟢 Carbon NCI 关联广度 + 本项目底层
2. 召回触发双入口：手动 + 从量具超差/批次 NCR 升级 —— 🟢 Carbon 校准召回逻辑
3. 召回目标定位靠追溯链反查（批次→销售出库→客户）—— 本项目 trace-chain.md 反向追溯
4. 客户通知是必备动作（非可选）—— 🟢 Carbon "customer is immediately notified"

**反模式**：
- ⛔ 把召回做成 NCR 的一个 status（召回是一对多批量事件，NCR 是单点不合格）
- ⛔ 召回直接改库存余额（应走标准销售退货移动单）

**落地要点**：
- 实体：`ErpQaRecall`（事件头，triggerType: MANUAL/GAUGE_NCR_UPGRADE/BATCH_NCR_UPGRADE/REGULATORY）+ `ErpQaRecallTarget`（受影响客户/批次明细）
- **NCR 升级实现裁决**：建议新增 NCR status 值 `ESCALATED_TO_RECALL`（显式可查），而非不改状态仅建 Recall
- 召回**不过账**；触发的销售退货走 sales 标准退货过账
- 召回强制审批（高风险，参考 domain-design-guidelines.md §6.1）
- 工作量如实标注"中等扩展"（非轻量）—— 需新增召回实体 + NCR 升级动作 + 批量退货编排 + 通知
- 证据：🟢 Carbon 量具召回 + NCI 关联；⚪ 召回事件实体属领域常识（开源无）

---

## 三、Phase 3：独立扩展（3 场景）

### 3.1 CRM

**最佳实践**（🟢 Odoo crm + 🟢 ERPNext crm 源码）：
1. **单实体 + type 判别**（`crm.lead` type ∈ {lead, opportunity}）—— 🟢 `crm_lead.py:84-125`（比 Lead/Opportunity 两表更精简，对齐本项目风格）
2. **漏斗 = 可配置阶段表**（有序记录 + 团队作用域 + 概率默认值，非硬编码 enum）—— 🟢 `crm_stage.py:14-33`
3. **金额三件套**：expectedRevenue（一次性）+ recurringRevenue/recurringPlan（MRR）—— 🟢 `crm_lead.py:141-150`
4. **营销活动归因**（UTM mixin：campaign/medium/source）—— 🟢 `crm_lead.py:24-28`
5. **ERPNext CRM 边界**：Quotation 在 selling 域不在 crm 域 —— 🟢 源码确认 `selling/doctype/Quotation` vs `crm/doctype/opportunity`，与本项目 sales/README.md:9 吻合

**反模式**：
- ⛔ 🟢 Odoo `sale.order.opportunity_id` 外键污染销售核心（`sale_crm/models/crm_lead.py:13`）—— 本项目反向：转化结果用弱指针存在 CRM 侧，**sales 实体零字段新增**

**落地要点**：
- 实体：`ErpCrmLead`（单实体，leadType 判别）+ `ErpCrmStage`（阶段表）+ `ErpCrmActivity` + `ErpCrmCampaign`（UTM 归因）+ `ErpCrmLeadConvLog`（阶段流转审计）
- 转化衔接：弱指针 `relatedBillType=SALES_QUOTATION` + `IErpSalQuotationBiz`，**不在 sales 实体加 opportunityId**
- CRM 是业务模块**无外部 SPI**（不像 TMS/EDI），仅内部转化服务 `IErpCrmConversionBiz`
- 证据补强：🟢 `crm_lead.py:84-125` + `crm_stage.py:14-33` + `sale_crm/models/crm_lead.py:13`

### 3.2 运输 / 物流 TMS

**⚠️ SPI 形态修正（关键）**：03 计划 :122 单层 `ICarrierGatewayProvider` —— **应改三层**（Metasfresh 黄金参考）。

**最佳实践**（🟢 Metasfresh shipper.gateway 源码全读）：
1. **SPI 三层**：Client（`ShipperGatewayClient.java:37-58`）+ ClientFactory（`:29-34`，per-carrier 配置化 client）+ **自动聚合 Registry**（`ShipperGatewayServicesRegistry.java:43-125`，`@Inject Optional<List<Factory>>` → `ImmutableMap<gatewayId, Factory>`）
2. **中立数据模型放 SPI 模块**（DeliveryOrder/PackageLabels/Address 等 POJO，承运商无关）—— 🟢 `spi/model/`
3. **承运商配置参数化**（url/凭证/OAuth2/trackingUrlTemplate/additionalProperties）—— 🟢 `commons/model/ShipperConfig.java:35-47`
4. **异步下单**（不阻塞主事务）—— 🟢 `commons/async/DeliveryOrderWorkpackageProcessor.java`
5. **新增承运商 = 1 个 @Service Factory bean，零改 commons** —— 🟢 `DhlShipperGatewayClientFactory.java:16-47`

**反模式**：
- ⛔ 🟢 Odoo 命名约定派发 `getattr(self,'%s_send_shipping'%delivery_type)`（`stock_delivery/models/delivery_carrier.py:50-51`）—— 脆弱，与 iDempiere 反射 Doc 工厂同类
- ⛔ 🟢 Odoo `sale.order.carrier_id` + 配送行烘焙进销售订单（`delivery/models/sale_order.py:13`）—— 污染销售核心

**落地要点**：
- 实体：`ErpLogCarrier`/`ErpLogCarrierConfig`/`ErpLogShipment`/`Line`/`Parcel`/`Log`
- **SPI 三层**：`IErpLogCarrierGatewayClient`(completeDeliveryOrder/getPackageLabelsList/adviseShipment) + `IErpLogCarrierGatewayClientFactory`(newClientForCarrierId) + `ErpLogCarrierGatewayRegistry`(@Inject Map)
- 中立 DTO 包对应 Metasfresh `spi/model/`
- **运费双路径裁决**：销售运费→配送行/`FREIGHT` 凭证；采购运费→Landed Cost（costing-methods.md:287-309）
- **不在 `ErpSalDelivery` 加 carrierId**（核心零污染），承运商关联在 logistics 发运单侧弱指针
- 主证改 Metasfresh（metasfresh.md:30 + 源码），Odoo 降为反模式对照

### 3.3 EDI / ASN / B2B

**⚠️ 主证修正 + SPI 深度补充**：03 计划 :124 单 `IErpB2BIntegrationProvider` —— **应补"适用性派发 + 信封状态机表"**。

**最佳实践**（🟢 Odoo account_edi 源码全读）：
1. **EDI 格式 = 可插拔 SPI + 适用性派发**（`_get_move_applicability` 返回 post/cancel/batching callables 字典）—— 🟢 `account_edi_format.py:58-68`
2. **EDI 信封表 + 状态机**（to_send/sent/to_cancel/cancelled + error + blocking_level + attachment + UNIQUE(format,source)）—— 🟢 `account_edi_document.py:14-58` —— **ASN 必须有此事务跟踪**
3. **双向**：导出 builder + 导入 decoder —— 🟢 `sale_edi_ubl/models/sale_order.py:7-30`
4. **异步发送**（web service 类走异步队列/cron）—— 🟢 `account_edi_format.py:40-42`
5. **Webhook 出站复用** integration-pattern.md 的 `ErpSysWebhookConfig/Log`

**反模式**：
- ⛔ 🟢 ERPNext edi 仅 `code_list`/`common_code` 映射表（源码确认 `edi/doctype/`）—— **非 EDI 引擎，不可作主证**，仅可借鉴"代码映射"概念
- ⛔ EDI 引擎烘焙进核心域

**落地要点**：
- 实体：`ErpB2bEdiFormat`(格式配置) + **`ErpB2bEdiDoc`(信封/事务，state+blocking_level+attachment，UNIQUE(formatId,relatedBillType,relatedBillCode))** + `ErpB2bAsn`/`Line`(提前发货通知) + `ErpB2bCodeMapping`(内外代码映射) + `ErpB2bEdiLog`
- **SPI 补适用性派发**：`IErpB2bEdiProvider.getApplicability(relatedBillType)` 返回 {outbound, inbound, batchable} + `generatePayload`/`parsePayload` + `needsWebService`
- ASN 入站：Webhook HMAC 验签 → parsePayload → 建 Asn + EdiDoc(state=RECEIVED) → 弱指针关联 purchase 收货（**ASN 不直接写库存，由 purchase 决定**）
- ASN 必须挂 `sourceEdiDocId`（来自哪条 EDI）
- 主证改 Odoo account_edi（metasfresh.md:30 + 源码），ERPNext edi 标注"仅代码映射，非引擎"
- 属架构文档 `architecture/b2b-integration.md`（集成层），但 ASN 实体表前缀归 module-b2b 工程

---

## 四、对计划 03 的修订指令汇总

### 4.1 证据修正（3 处过度引申）

| 计划位置 | 现引证据（错误/过度） | 修正为 |
|---|---|---|
| Phase 1 treasury :65 | metasfresh.md:83 BankingAcctDocProvider + redragon-erp.md:41 | **诚实标注开源零覆盖票据**；科目分解范式引 🟢 `Doc_BankStatement.java:206-547`（实测，处理银行对账单）；反例引 🟢 iDempiere `X_C_Payment.java:2259-2271` |
| Phase 3 TMS :122 | odoo.md:33 stock_delivery/stock_fleet | **主证改 Metasfresh** 🟢 `ShipperGatewayServicesRegistry.java:43-125` + `DhlShipperGatewayClientFactory.java`；Odoo 降为反模式 🟢 `stock_delivery/models/delivery_carrier.py:50-51` |
| Phase 3 EDI :124 | odoo.md:31,32 + erpnext.md:33 | **主证改 Odoo account_edi** 🟢 `account_edi_format.py:7-120` + `account_edi_document.py:14-58` + `sale_edi_ubl`；**ERPNext edi 须标注"仅 code_list 映射，非引擎"** |

### 4.2 设计错误修正（2 处实质）

1. **供应商评分卡实体归属**（Phase 2 :93）：拆分 AVL 准入（master-data `ErpMdSupplierApproval`）+ 评分周期（purchase `ErpPurSupplierScorecard*`）。理由：单一职责 + 评分引用采购链数据。
2. **TMS SPI 形态**（Phase 3 :122）：单层 `ICarrierGatewayProvider` → **三层**（Client + ClientFactory + Registry），补中立 DTO 包。缺则丢失"per-carrier 配置化 client"能力。

### 4.3 设计决策裁决（5 处需在文档显式记录）

1. **核心实体零污染**（影响 Phase 2 VMI + Phase 3 全部）：统一凭证指针反查范式（cn-golden-tax.md:109），拒绝 l10n-strategy.md Delta 加列；不在 `ErpSalDelivery` 加 carrierId、不在 `ErpSalOrder` 加 opportunityId、不在 `ErpInvStockBalance` 之外的核心表加 ownerId（balance/ledger 加是合理的库存维度扩展）。
2. **CRP 不引入排产方案单据**（Phase 2 :91）：仅负荷报表 + WorkOrder 已有 plannedStartDate/EndDate 承接；APS 属 follow-up。
3. **NCR 升级召回**（Phase 2 :97）：新增 NCR status `ESCALATED_TO_RECALL`（显式可查）。
4. **TMS 运费双路径**（Phase 3 :122）：销售运费→配送行/FREIGHT 凭证；采购运费→Landed Cost。
5. **备用金不建独立实体**（Phase 1 :63）：作为 `ErpFinEmployeeAdvance.advanceType=IMPREST` 特化。

### 4.4 SPI 契约深度补充（Phase 3 骨架从"过浅"到"恰当"）

03 计划 Closure Gates :187 要求"必需章节含 SPI 契约"。当前 Phase 3 仅列单层 SPI 名，**过浅**。补充：
- **TMS**：三层 SPI 关键方法签名（completeDeliveryOrder/getPackageLabelsList/adviseShipment + newClientForCarrierId + Registry @Inject Map）+ 中立 DTO 清单
- **EDI**：适用性派发 `getApplicability()` + 信封状态机表 `ErpB2bEdiDoc`（state+blocking_level）
- **CRM**：无外部 SPI，仅内部 `IErpCrmConversionBiz.convertToQuotation`
- 边界：不写 ORM/xbiz/view（保持 Non-Goal），但 SPI 方法签名 + 中立 DTO + 状态机是"骨架"与"完整实施设计"的分界

### 4.5 架构建议（非 Phase 范围但相关）

🟢 源码回查发现：业财过账 `IErpFinAcctDocProvider`、TMS `IErpLogCarrierGatewayClient`、EDI `IErpB2bEdiProvider`、CRM 转化服务的注册中心**同构**（@Inject Map + 自动聚合 + 新增=1 bean）。建议在 `docs/architecture/` 增补"SPI 提供者注册中心统一范式"节，收敛四个场景的重复描述。证据：🟢 Metasfresh `AcctDocRegistry.java:25-33` 与 `ShipperGatewayServicesRegistry.java:43-125` 是同一模式的两个实例。

---

## 五、证据强度总览（诚实标注）

| 场景 | 🟢 开源直接借鉴（源码实测） | 🟡 开源零件 + 本项目组合 | ⚪ 领域常识/自建 |
|---|---|---|---|
| 费用报销 | Odoo hr_expense + ERPNext payment/advance | 数据源（projects 已引用） | — |
| 资金/票据 | Metasfresh Doc_BankStatement 科目分解 | — | 中式承兑汇票全生命周期（4 开源零覆盖） |
| CRP | Odoo 产能四要素 + 前向排产 | 日历/班次/按产品产能 | 真 APS（优化求解） |
| 供应商评分 | ERPNext 8-doctype 公式/变量/周期/评级 | 数据源（quality/purchase 已有） | — |
| VMI | Odoo owner 维度建模（仅此） | — | 所有权转移结算闭环（Odoo 不做，自建） |
| 召回 | Carbon NCI 关联广度 + 量具召回 | 本项目 trace-chain/NCR/退货 | 召回事件聚合实体（开源无） |
| CRM | Odoo crm_lead/stage + ERPNext opportunity | — | — |
| TMS | Metasfresh SPI 三层 + 中立模型 + 异步 | — | — |
| EDI | Odoo account_edi 框架 + 信封状态机 | integration-pattern webhook | X12/EDIFACT 标准细节 |

---

## 六、参考证据索引（源码 file:line，均可核实）

### 财务扩展
- `/Users/abc/sources/erp/odoo/addons/hr_expense/models/hr_expense.py:122-273,1169-1222,1740-1805`
- `/Users/abc/sources/erp/erpnext/erpnext/accounts/doctype/payment_entry/{payment_entry.json,payment_entry.py:235-274,548,1290-1374}`
- `/Users/abc/sources/erp/erpnext/erpnext/accounts/doctype/advance_payment_ledger_entry/advance_payment_ledger_entry.json`
- `/Users/abc/sources/erp/metasfresh/backend/de.metas.banking/de.metas.banking.base/src/main/java/de/metas/banking/{model/validator/BankingAcctDocProvider.java:33-42, .../Doc_BankStatement.java:206-547}`
- `/Users/abc/sources/erp/idempiere/org.adempiere.base/src/org/compiere/model/X_C_Payment.java:2259-2271`
- `/Users/abc/sources/erp/redragon-erp/erp-parent/erp-finance/src/main/java/com/erp/finance/ap/invoice/dao/model/ApInvoiceHead.java:196-229`

### 制造/采购/库存/质量
- `/Users/abc/sources/erp/odoo/addons/mrp/models/{mrp_workcenter.py:30,78,81,157,360-364,427-437,613-636, mrp_workorder.py}`
- `/Users/abc/sources/erp/erpnext/erpnext/manufacturing/doctype/{work_order,production_plan}/*.json`
- `/Users/abc/sources/erp/erpnext/erpnext/buying/doctype/supplier_scorecard*/`（8 doctype）
- `/Users/abc/sources/erp/odoo/addons/stock/models/{stock_quant.py:74,144-145,155, res_config_settings.py:21}`
- `/Users/abc/sources/erp/carbon/apps/erp/app/{modules/quality/quality.models.ts:462-485, routes/api+/data/quality.ts:26514}`

### 独立扩展
- `/Users/abc/sources/erp/odoo/addons/crm/models/{crm_lead.py:84-125,141-150, crm_stage.py:14-33}`
- `/Users/abc/sources/erp/odoo/addons/sale_crm/models/crm_lead.py:11-13,35`
- `/Users/abc/sources/erp/erpnext/erpnext/crm/doctype/opportunity/opportunity.json:113-236`
- `/Users/abc/sources/erp/metasfresh/backend/de.metas.shipper.gateway.{spi/ShipperGatewayClient.java:37-58, spi/ShipperGatewayClientFactory.java:29-34, commons/ShipperGatewayServicesRegistry.java:43-125, commons/model/ShipperConfig.java:35-47, commons/async/DeliveryOrderWorkpackageProcessor.java:103-140, dhl/DhlShipperGatewayClientFactory.java:16-47}`
- `/Users/abc/sources/erp/odoo/addons/delivery/models/{delivery_carrier.py:13-110, sale_order.py:13,67-72,239}`
- `/Users/abc/sources/erp/odoo/addons/stock_delivery/models/delivery_carrier.py:50-51`
- `/Users/abc/sources/erp/odoo/addons/account_edi/models/{account_edi_format.py:7-120, account_edi_document.py:14-58}`
- `/Users/abc/sources/erp/odoo/addons/sale_edi_ubl/models/sale_order.py:7-30`
- `/Users/abc/sources/erp/erpnext/erpnext/edi/doctype/`（仅 code_list/common_code）

### 本项目
- `docs/design/finance/{posting,bank-reconciliation,budget,costing-methods,ar-ap-reconciliation,multiple-accounting-schemas}.md`
- `docs/design/l10n/cn-golden-tax.md`（独立工程范式样板）
- `docs/design/{inventory/state-machine,inventory/trace-chain,quality/state-machine,manufacturing/mrp,manufacturing/bom-and-routing}.md`
- `docs/architecture/{integration-pattern,domain-module-split-analysis,customization-capabilities}.md`
- `docs/requirements/product-scope.md:49-52`（延迟范围）
