# 03 高级业务场景设计文档补齐计划

> Plan Status: completed
> Last Reviewed: 2026-06-30（执行完成，独立结束审计通过）
> Source: `docs/analysis/2026-06-30-0000-advanced-scenario-gap-analysis.md`（经独立子代理两轮审查定稿）+ **`docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md`（三层挖掘：调研报告 + 源码回查 + 提炼，本计划的设计依据）**
> Related: `docs/plans/02-documentation-improvement-plan.md`（已完成）、`docs/requirements/product-scope.md`、`docs/backlog/implementation-roadmap.md`
> Audit: required

> **修订记录（2026-06-30）**：初版经独立草案审查通过（0 阻塞），但审查侧重事实核查+模板合规。随后基于 0001 深度对比分析（回查 /Users/abc/sources/erp/ 源码）发现：① 3 处证据过度引申（票据/TMS/EDI 主证错误）；② 2 处设计错误（供应商评分卡归属、TMS SPI 应三层）；③ 5 处设计决策未裁决；④ Phase 3 SPI 契约深度不足。本次修订将 0001 的设计依据落入各执行项，并修正上述问题。详见各 Phase 执行项的"设计依据"行与下方 Design Rationale。

## Current Baseline

**项目阶段**（`product-scope.md:33`）：post-codegen。10 域 ORM 已就位（145 实体、1096 Java 文件、82 模块），下一步是端到端业务循环验证 + BizModel + 页面开发。"垂直行业扩展工程"与"外部集成（税控/银行/物流/电商）"列为延迟范围（`product-scope.md:49-52`）。

**设计层已覆盖**（前序四份审计 + 02 计划确认，本计划不重复）：
- 核心进销存+财务+制造+资产+质量+维护+项目的业财一体、状态机、跨域协作、可配置点、横切关注点（见 `feature-inventory.md` + 02 计划 8 阶段产出）
- 采购寻源链（请购→询价→报价→订单）实体已存在：`module-purchase/model:160,188,214` 的 `ErpPurRfq`/`ErpPurRfqLine`/`ErpPurQuotation`/`ErpPurQuotationLine`/`ErpPurSupplierPriceList`（含多供应商比价关系）

**剩余差距**（分析报告识别，已用 ORM 权威核实）：
- **P0（2 个，与核心域强耦合且已暴露不一致）**：
  - 费用报销/员工借款——`projects/use-cases.md:57`、`projects/cost-collection.md:37,119,153,165` 已引用为成本归集来源但无 owner 实体（"被引用无 owner"的真实不一致）
  - 资金管理/票据——仅有 `ErpFinFundAccount` + 银行对账，缺票据/贴现/授信/现金预测
- **P1 强耦合域扩展（4 个）**：CRP/APS（`manufacturing/mrp.md:10` 项目自承认延迟）、供应商评分卡/AVL（RFQ 已存在，仅评分/准入缺）、VMI/寄售（`ErpInvStockBalance` 无 `ownerId` 维度）、批次召回事件（有追溯+NCR+退货底层，缺事件聚合）
- **P1 独立扩展模块（3 个）**：CRM、运输/TMS、EDI/ASN——属 `product-scope.md:49-52` 延迟范围，仅需设计骨架 + SPI 契约
- **P2（4 个 + ABC 备选，明确不纳入核心）**：DRP、HRMS/薪酬、POS/电商、售后服务、库存 ABC 分类

## Goals

- 为分析报告识别的 **2 个 P0 + 7 个 P1** 场景补充设计文档，使 post-codegen 阶段的扩展域实施有设计依据。
- P0 + P1 强耦合域（共 6 个场景）产出**实施级设计**（实体清单 + 状态机 + 业财过账 businessType + 跨域协作 + 配置点）。
- P1 独立扩展模块（3 个场景）产出**设计骨架 + SPI 接口契约**，明确深化到实施级延迟到客户需求触发。
- 消除 `projects` 文档与 `feature-inventory.md` 关于"费用报销"的内部不一致。
- 同步基线索引（`feature-inventory.md`、`design/README.md`、`backlog/implementation-roadmap.md`、`analysis/README.md`），使新设计文档可被发现。

## Non-Goals

- **不修改任何 `model/*.orm.xml`**——本计划仅文档化业务语义与建议实体名；实体落地属后续 ORM 计划，`model/*.orm.xml` 是 ask-first 保护区域（`product-scope.md:58`）。设计文档中出现的实体名（如 `ErpFinExpenseClaim`）是**建议命名**，标注"待 ORM 计划落地"。
- **不实现代码**（BizModel/xbiz/view.xml/Java）——仅设计文档。
- **不纳入 P2**（DRP/HRMS/POS/售后/ABC 分类）——与分析报告一致，归 `Deferred But Adjudicated`。
- **不深化 P1 独立扩展模块的 use-case 级设计**——CRM/TMS/EDI 仅骨架 + 契约。
- **不修改 erp-survey 调研文档**——已定稿。
- **不重新审计已覆盖的核心闭环**——前序四份审计结论继续成立。

## Task Route

- Type: `app-layer design change`（文档层面，不改 ORM/代码）
- Owner Docs: `docs/design/finance/`、`docs/design/manufacturing/`、`docs/design/purchase/`、`docs/design/inventory/`、`docs/design/quality/`、`docs/design/README.md`、`docs/design/feature-inventory.md`、`docs/architecture/`（仅 EDI/B2B 集成契约）
- Skill Selection Basis: `Skill: none`——本计划全部为业务设计文档编写，与 02 计划一致，无匹配的可复用技能（`docs/skills/README.md` 无设计编写技能）

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. 纯文档工作，不涉及端口、环境变量、密钥、外部服务、数据迁移。

## Design Rationale（设计依据，来自 0001 深度对比分析）

> 以下跨场景范式与裁决由 `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` 提炼，每个 Phase 执行项的"设计依据"行引用其中具体小节。证据强度统一标注：🟢源码回查实测 / 🟡调研记载（经复核）/ ⚪领域常识（开源零覆盖）。

### 范式 A：独立扩展工程 + 核心实体零污染（适用 Phase 2 VMI/召回 + Phase 3 全部）

- 样板：`docs/design/l10n/cn-golden-tax.md`（独立 Maven 工程 + 独立 appName + **凭证指针反查核心域**）。
- ⚠️ **架构冲突裁决**：`l10n-strategy.md:166-185` 的"Delta ORM 给核心实体加列"与 `cn-golden-tax.md:109` 的"不改核心实体"矛盾——**统一采用后者**（与跨域 DAG、`domain-module-split-analysis.md:138-150` 一致）。
- **核心实体零污染清单**：不在 `ErpSalDelivery` 加 carrierId、不在 `ErpSalOrder` 加 opportunityId（吸收 🟢 Odoo `sale_crm/models/crm_lead.py:13` + `delivery/models/sale_order.py:13` 两个污染反例）。`ErpInvStockBalance`/`Ledger` 加 ownerId 是**合理的库存维度扩展**（owner 是库存正交轴，🟢 `stock_quant.py:74` 证据），不属"污染"。

### 范式 B：SPI 提供者注册中心统一形态（适用 Phase 2 业财过账 + Phase 3 TMS/EDI）

🟢 源码回查发现 Metasfresh `ShipperGatewayServicesRegistry.java:43-125` 与 `AcctDocRegistry.java:25-33` 是**同一模式**。本仓 `IErpFinAcctDocProvider` 亦同构。统一形态：

```
interface IXxxProvider { String getCode(); ... }
@Inject Map<String, IXxxProvider> providers;   // Nop IoC 按 bean 自动聚合
// 新增实现 = 1 个 @Component bean，零改 commons
```

Phase 3 各文档采用此形态描述 SPI；建议（非本计划范围）在 `docs/architecture/` 增补"SPI 提供者注册中心统一范式"节收敛描述。

### 范式 C：证据强度诚实标注（Closure Gates :187"无编造证据"门控的服务规则）

每个设计文档的"开源参考"节必须区分：🟢 源码 file:line 可核实 / 🟡 调研报告记载 / ⚪ 开源零覆盖（领域常识）。**禁止把调研报告的泛泛记载升格为"源码实测"，禁止把领域常识伪装为调研证据**（0001 分析已纠正 3 处此类过度引申）。

### 裁决清单（5 处，须在对应文档显式记录）

| # | 裁决 | 理由 |
|---|---|---|
| D1 | CRP **不引入**排产方案单据（仅负荷报表 + WorkOrder 已有 plannedStartDate/EndDate 承接） | 避免新增状态机；🟢 开源无真 APS（Odoo 仅前向排产、ERPNext 仅日期计划），APS 属 follow-up |
| D2 | NCR 升级召回 = **新增 NCR status 值 `ESCALATED_TO_RECALL`**（非不改状态仅建 Recall） | 显式可查、便于审计（0001 §2.4） |
| D3 | TMS 运费**双路径**：销售运费→配送行/FREIGHT 凭证；采购运费→Landed Cost | 🟢 Odoo sale_order.py:13,67-72 + costing-methods.md:287-309 |
| D4 | 备用金**不建独立实体**，作为 `ErpFinEmployeeAdvance.advanceType=IMPREST` 特化 | ⚪ 开源零覆盖，中国会计实践 |
| D5 | 供应商评分卡**拆分归属**：AVL 准入→master-data，评分周期数据→purchase | 单一职责 + 评分引用采购链数据（0001 §2.2） |

## Execution Plan

### Phase 1 — P0 完整设计（费用报销 + 资金/票据）

Status: completed
Targets: `docs/design/finance/expense-claim.md`(新)、`docs/design/finance/treasury.md`(新)、`docs/design/projects/cost-collection.md`(修引用注释)、`docs/design/feature-inventory.md`(补条目)
Skill: none

- Item Types: `Add | Fix`
- Prereqs: 无

- [x] **Add** 创建 `docs/design/finance/expense-claim.md`——费用报销/员工借款/备用金设计。**设计依据：0001 §1.1**。内容须含：
  - **paymentMode 字段 + 对方科目决策**（🟢 Odoo `hr_expense.py:1780-1805`）：own_account→应付-员工、company_account→银行待结算
  - 实体清单（建议，待 ORM 落地）：`ErpFinExpenseClaim`/`Line`（报销，含 projectId/costCenterId 归集维度 + 价税分离 untaxed/tax/total）、`ErpFinEmployeeAdvance`（借款，advanceType: EXPENSE_ADVANCE/IMPREST/TRAVEL）
  - **借款科目方向=其他应收款-员工预支**（非应付，方向相反；🟢 ERPNext `payment_entry.py:235-274` book_advance_payments_in_separate_party_account）
  - **裁决 D4**：备用金不建独立实体，作为 advanceType=IMPREST 特化（⚪ 开源零覆盖）
  - **还款复用 `ErpFinPayment` + partyType=EMPLOYEE + `ErpFinReconciliation`**（sourceBillType=EMPLOYEE_ADVANCE），不建独立还款单（🟢 ERPNext `payment_entry.py:548`）
  - 业财过账 businessType：EXPENSE_CLAIM / EMPLOYEE_ADVANCE / EMPLOYEE_ADVANCE_SETTLE，复用 `IErpFinAcctDocProvider` + posted 兜底
  - **预算控制钩子**：APPROVED 前同步调 `IErpFinBudgetControlBiz.check`（budget.md:74）
  - **反模式警示**：不学 Odoo 单轴 state（🟢 `hr_expense.py:122-142`），坚持三轴（docStatus + approveStatus + posted + paidStatus）；不学管伊佳逗号字符串多账户
  - 证据强度：🟢 Odoo hr_expense + 🟢 ERPNext payment/advance 源码实测；⚪ 备用金属领域常识
  - Skill: none
- [x] **Add** 创建 `docs/design/finance/treasury.md`——资金管理/票据设计。**设计依据：0001 §1.2**。**⚠️ 证据修正**（初版引 metasfresh.md:83 + redragon-erp.md:41 属过度引申，源码回查确认均非票据证据）。内容须含：
  - **诚实标注开源零覆盖**：4 个开源 ERP 均无中式承兑汇票实体（🟢 `BankingAcctDocProvider.java:40` 只注册银行对账单；🟢 `ApInvoiceHead.java:207-229` 是收款银行账户；🟢 `X_C_Payment.java:2259-2271` tenderType 无 Notes）。本设计依据中国《企业会计准则》+ 领域常识
  - **科目分解五件套（借 Metasfresh 范式）**：资产/在途/手续费/利息/汇兑损益 —— 🟢 `Doc_BankStatement.java:206-547`（createFacts_TrxAmt/BankFee/Interest/CurrencyExchangeGainOrLoss），**此为银行对账单处理，非票据，仅作科目分解参考**
  - 实体清单：`ErpFinNotesReceivable`（应收票据，7 态：RECEIVED/ENDORSED/DISCOUNTED/COLLECTION_PENDING/HONORED/DISHONORED/WRITE_OFF）、`ErpFinNotesPayable`（应付票据）、`ErpFinNotesDiscount`（贴现明细）、`ErpFinCreditFacility`（授信）、`ErpFinCashForecast`（**物化视图**，类比 ErpFinBankLedgerLine，由 AR/AP 到期 + 票据到期派生）
  - **应收票据 vs 应付票据方向对称**（应收=资产/借方，应付=负债/贷方）
  - **贴现息走财务费用-利息支出**（非冲减应收票据）；外币贴现产生汇兑损益（🟢 Doc_BankStatement.java:482-547 范式）
  - **反模式**：不把票据塞进 payment.tenderType（反 🟢 iDempiere `X_C_Payment.java:2267` Check）；贴现息不冲减应收票据
  - businessType：NOTES_RECEIVABLE_RECEIVED/DISCOUNTED/ENDORSED/COLLECTION + NOTES_PAYABLE_ISSUED/HONORED + CREDIT_FACILITY_INTEREST
  - 开银承前同步校验 creditFacility.availableAmount（强一致，参考 budget.md:74）
  - 与银行对账解耦（bank-reconciliation.md:11 已明确）
  - Skill: none
- [x] **Fix** 修正 `docs/design/projects/cost-collection.md` + `projects/use-cases.md` 中"费用报销"引用——添加指向 `finance/expense-claim.md` 的回链，消除"被引用无 owner"不一致。注：源分析报告 §3.1 曾误引 `projects/state-machine.md:88` 为报销出处，经核查该行实为"工时成本→应付职工薪酬"（非报销），**不纳入本次修正范围**，仅修 use-cases.md:57 与 cost-collection.md:37,119,153,165 五处真实报销引用。
  - Skill: none
- [x] **Fix** `docs/design/feature-inventory.md` 扩展业务功能表补"费用报销/员工借款"（owner=`finance/expense-claim.md`）与"资金管理/票据"（owner=`finance/treasury.md`）两条目。撤销分析报告 §八识别的不一致。
  - Skill: none

Exit Criteria:

> 本阶段交付两份新设计文档（含 0001 §1.1/§1.2 的设计依据落地）+ 两份文档修正，消除 projects 引用不一致。

- [x] `finance/expense-claim.md` 与 `finance/treasury.md` 创建完成，每份含：边界、实体清单（标注待 ORM 落地）、状态机、业财过账 businessType、跨域协作、配置点、**反模式警示**、**证据强度诚实标注（🟢/🟡/⚪）**
- [x] expense-claim.md 明确 paymentMode 对方科目决策、借款方向（其他应收）、还款复用 ErpFinPayment
- [x] treasury.md **不引用 metasfresh.md:83/redragon-erp.md:41 作为票据证据**，改诚实标注开源零覆盖 + Metasfresh Doc_BankStatement 科目分解范式
- [x] `projects/cost-collection.md` 与 `projects/use-cases.md` 的报销引用指向新 owner doc
- [x] `feature-inventory.md` 补两条目且无悬空引用

---

### Phase 2 — P1 强耦合域扩展完整设计（CRP + 供应商评分 + VMI + 批次召回）

Status: completed
Targets: `docs/design/manufacturing/crp.md`(新)、`docs/design/purchase/supplier-evaluation.md`(新)、`docs/design/inventory/consignment.md`(新)、`docs/design/quality/recall.md`(新)
Skill: none

- Item Types: `Add`
- Prereqs: 无（可与 Phase 1 并行；建议在 Phase 1 后做以保持业财过账 businessType 扩展模式一致）

- [x] **Add** 创建 `docs/design/manufacturing/crp.md`——CRP/APS 设计。**设计依据：0001 §2.1**。内容须含：
  - **产能四要素分离**（🟢 Odoo `mrp_workcenter.py:30,78,81` + `mrp.workcenter.capacity`(`:613-636`)）：工作中心日历(时钟) + 按产品并行产能 + 换模/清理时间 + 效率系数
  - **负荷上限取自日历出勤时长**（非硬编码 capacity 字段，🟢 `mrp_workcenter.py:157`）—— 与现有 `ErpMfgWorkcenter.workHoursPerDay` 字段语义须澄清（该字段是默认标量产能，按产品产能需新建 WorkcenterCapacity 子实体）
  - 实体清单：`ErpMfgWorkcenterCapacity`（按产品产能，对应 Odoo mrp.workcenter.capacity）、`ErpMfgWorkcenterCalendar`（日历/班次，对应 Odoo resource_calendar）、`ErpMfgCrpLoad`（负荷快照行，弱指针关联 WorkOrder/JobCard）
  - **裁决 D1**：不引入排产方案单据（仅负荷报表 + WorkOrder 已有 plannedStartDate/EndDate 承接），避免新增状态机
  - **CRP 不产生凭证**（计划层）；超负荷触发的加班/外协才过账（复用 WorkOrder laborCost / SubcontractOrder）
  - **诚实标注**：🟢 开源无真 APS（Odoo 仅前向排产 Gantt 填充 `mrp_workcenter.py:360-364`、ERPNext 仅日期计划），APS（优化求解）属 follow-up；CRP 与 MRP 解耦（mrp.md:10 边界）
  - **反模式**：产能不硬编码为单一标量（Odoo 反例特意按 product 查 capacity_ids）
  - Skill: none
- [x] **Add** 创建 `docs/design/purchase/supplier-evaluation.md`——供应商评分卡 + AVL 准入设计。**设计依据：0001 §2.2**。**⚠️ 实体归属修正（裁决 D5）**：初版建议全放 master-data，应**拆分**——AVL 准入（资格主数据）放 master-data，评分卡周期数据（业务绩效）放 purchase。内容须含：
  - **拆分实体**：`ErpMdSupplierApproval`（master-data，资格生命周期 APPLIED/APPROVED/PROBATION/SUSPENDED/REJECTED）+ `ErpPurSupplierScorecard`/`Criteria`/`Variable`（purchase，周期评分）
  - 理由：评分引用采购链数据（RFQ/PO/质检单），属采购绩效产物（单一职责）
  - **ERPNext 8-doctype 完整体系**（🟢 源码实测 `erpnext/buying/doctype/supplier_scorecard*/`）：评分=维度(criteria)×公式(formula)×权重(weight)，公式引用变量(variable)从业务 path 取值；周期化评估（起止日期+total_score）；评分→评级档位(standing)→RFQ 三档联动(warn/hold/prevent)
  - **数据源已存在**（强化"仅评分/准入缺"判断）：🟢 `ErpQaInspection.supplierId`(quality orm.xml:139，质量合格率) + 🟢 `ErpPurSupplierPriceList`(orm.xml:277，价格竞争力) + PO/Receive 交货日期（按时交货率）
  - 评分卡**不过账**；公式用 nop 规则引擎/DSL（不硬编码 Java）
  - standing=RED → 写 SupplierApproval=SUSPENDED → RFQ 创建校验
  - **反模式**：不实时累加在供应商主数据单字段（ERPNext 特意做 period 快照表）；不耦合进 RFQ 审核事务
  - Skill: none
- [x] **Add** 创建 `docs/design/inventory/consignment.md`——VMI/寄售/受托代销设计。**设计依据：0001 §2.3**。内容须含：
  - **证据精确化（坐实并细化弱证据）**：🟢 `stock_quant.py:74` owner_id 字段 + `:155` 唯一键含 owner；🟢 `res_config_settings.py:21` Consignment 是 feature group 开关（默认关）；🟢 **owner 不参与 stock.rule**（grep `stock_rule.py` owner_id 空命中）—— **Odoo 无 VMI 自动结算闭环**，结算靠 EDI/发票层；本项目"所有权转移自动触发 AP_INVOICE"是**自建能力，非开源借鉴**，仅借鉴 owner 维度建模
  - 实体变更：`ErpInvStockBalance`/`StockLedger` 加 `ownerId` + `ownershipType`（字典 OWNED/VMI_SUPPLIER/CONSIGNMENT_OUT/CUSTOMER_PROVIDED）；新增 `ErpInvOwnershipTransfer`（所有权转移单）
  - **物理移动 vs 所有权转移单据分离**（关键设计决策）：`ErpInvOwnershipTransfer` 的 sourceLocId=destLocId（物理位置不变，仅法权变更），**不可复用普通移动单**
  - businessType: OWNERSHIP_TRANSFER —— 借 存货(自有) / 贷 应付-供应商
  - owner 是库存正交维度（不是污染核心实体，见 Design Rationale 范式 A 澄清）
  - 配置 `erp-inv.ownership-tracking-enabled`（默认 false，对应 Odoo feature group）
  - **反模式**：不声称"借鉴 Odoo VMI 自动结算"；owner 不塞进物料主数据（同物料可能 OWNED/VMI 并存）
  - Skill: none
- [x] **Add** 创建 `docs/design/quality/recall.md`——批次召回事件设计。**设计依据：0001 §2.4**。内容须含：
  - **证据精确化**：🟢 `grep -rli "recall" /Users/abc/sources/erp/` ~30 文件全部技术语义或量具校准，**无一例批次召回实体**；唯一业务相关是 🟢 Carbon 量具校准召回（`quality.ts:26514`，"量具超差→产品召回→通知客户"逻辑，无独立 Recall 实体）；🟢 Carbon `NonConformanceIssue` 关联广度（supplierId/customerId/jobOperationId/trackedEntityId，`quality.models.ts:462-485`）—— 召回底层能力（目标定位）已有零件
  - 实体：`ErpQaRecall`（事件头，triggerType: MANUAL/GAUGE_NCR_UPGRADE/BATCH_NCR_UPGRADE/REGULATORY）+ `ErpQaRecallTarget`（受影响客户/批次明细）
  - **裁决 D2**：NCR 升级召回 = 新增 NCR status 值 `ESCALATED_TO_RECALL`（显式可查），非不改状态仅建 Recall
  - 召回目标定位靠追溯链反查（trace-chain.md 反向追溯，批次→销售出库→客户）；**前置条件声明**：批次/序列号追溯必须启用
  - 召回**不过账**；触发的销售退货走 sales 标准退货过账；召回强制审批（高风险）
  - 客户通知是必备动作（🟢 Carbon "customer is immediately notified"）
  - **工作量如实标注"中等扩展"**（非轻量）：需新增召回实体 + NCR 升级动作 + 批量退货编排 + 通知
  - **反模式**：召回不做 NCR 的一个 status（一对多批量 vs 单点不合格）；召回不直接改库存余额（走标准退货移动单）
  - 证据强度：🟢 Carbon 量具召回 + NCI 关联；⚪ 召回事件实体属领域常识（开源无）
  - Skill: none

Exit Criteria:

> 本阶段交付四份强耦合域扩展设计，每份含完整实施级要素 + 0001 §2.x 设计依据落地 + 证据诚实标注。

- [x] 四份新文档创建完成，每份含：边界、实体清单（标注待 ORM 落地）、状态机/流程、业财过账或跨域协作、配置点、**反模式警示**、**证据强度标注（🟢/🟡/⚪）**
- [x] CRP 含产能四要素 + 裁决 D1（不引入排产单）+ APS 诚实标注；供应商评分执行裁决 D5（拆分归属）+ ERPNext 8-doctype 证据；VMI 坐实"Odoo 无结算闭环、本项目自建"+ 所有权转移单据分离；召回执行裁决 D2（ESCALATED_TO_RECALL）+ Carbon 证据 + 工作量诚实标注

---

### Phase 3 — P1 独立扩展模块设计骨架（CRM + TMS + EDI/ASN）

Status: completed
Targets: `docs/design/crm/README.md`(新)、`docs/design/logistics/README.md`(新)、`docs/architecture/b2b-integration.md`(新)
Skill: none

- Item Types: `Add | Decision`
- Prereqs: 无（与 Phase 2 独立，可并行）

- [x] **Decision** 三模块定位为"独立扩展工程"（参考 l10n-cn 模式，见 Design Rationale 范式 A），**不纳入 product-scope 的 10 域基线**，作为可选 `module-crm`/`module-logistics`/`module-b2b` 按需组装。理由记录在本阶段产出文档中。考虑的替代方案：纳入核心域子模块（拒绝，因 `product-scope.md:49-52` 明确外部集成/垂直行业为延迟范围）；残留风险：骨架文档与未来深化可能脱节，通过 Follow-up 触发条件缓解。
  - Skill: none
- [x] **Add** 创建 `docs/design/crm/README.md`——CRM 模块设计骨架。**设计依据：0001 §3.1**。内容须含：
  - 模块定位（线索→商机→转化，与 sales 域从报价单起的边界；🟢 ERPNext 边界确认 `selling/doctype/Quotation` vs `crm/doctype/opportunity`，与 sales/README.md:9 吻合）
  - **单实体 + type 判别**（建议 `ErpCrmLead`，leadType ∈ {LEAD, OPPORTUNITY}，🟢 Odoo `crm_lead.py:84-125`），比 Lead/Opportunity 两表更精简
  - **补漏斗阶段表** `ErpCrmStage`（有序记录 + 团队作用域 + 概率默认值，**非硬编码 enum**，🟢 `crm_stage.py:14-33`）+ `ErpCrmActivity` + `ErpCrmCampaign`（UTM 归因，🟢 `crm_lead.py:24-28`）+ `ErpCrmLeadConvLog`（阶段流转审计）
  - **与 sales 衔接（核心零污染）**：转化结果用弱指针 `relatedBillType=SALES_QUOTATION` + `IErpSalQuotationBiz`，**不在 sales 实体加 opportunityId**（反 🟢 Odoo `sale_crm/models/crm_lead.py:13` 污染反例）
  - CRM 是业务模块**无外部 SPI**（不像 TMS/EDI），仅内部转化服务 `IErpCrmConversionBiz.convertToQuotation`
  - **显式标注"实施级设计延迟到客户需求触发"**（product-scope.md:49-52 延迟范围）
  - 证据：🟢 `crm_lead.py:84-125` + `crm_stage.py:14-33` + `sale_crm/models/crm_lead.py:13` + 🟡 erpnext.md:33 crm 域
  - Skill: none
- [x] **Add** 创建 `docs/design/logistics/README.md`——运输/TMS 模块设计骨架。**设计依据：0001 §3.2**。**⚠️ SPI 形态修正（关键）**：初版单层 `ICarrierGatewayProvider` → **改三层**。内容须含：
  - 模块定位（发运单 = "怎么发/找谁运/运单号/面单"；sales 出库单 = "要发什么"；两者弱指针关联）
  - 实体：`ErpLogCarrier`/`ErpLogCarrierConfig`/`ErpLogShipment`/`Line`/`Parcel`/`Log`
  - **SPI 三层（核心交付物，照搬 Metasfresh 形态）**：`IErpLogCarrierGatewayClient`(completeDeliveryOrder/getPackageLabelsList/adviseShipment) + `IErpLogCarrierGatewayClientFactory`(newClientForCarrierId) + `ErpLogCarrierGatewayRegistry`(@Inject Map)，补中立 DTO 包（对应 🟢 Metasfresh `spi/model/`）。**单层 Provider 会丢失"per-carrier 配置化 client"能力**（🟢 `DhlShipperGatewayClientFactory.java:34-47` 证）
  - **裁决 D3（运费双路径）**：销售运费→配送行/FREIGHT 凭证；采购运费→Landed Cost（costing-methods.md:287-309）
  - **核心零污染**：不在 `ErpSalDelivery` 加 carrierId（反 🟢 Odoo `delivery/models/sale_order.py:13` 污染反例），承运商关联全在 logistics 发运单侧弱指针
  - **主证改 Metasfresh**（🟢 `ShipperGatewayServicesRegistry.java:43-125` + `DhlShipperGatewayClientFactory.java` + `ShipperConfig.java:35-47` + `DeliveryOrderWorkpackageProcessor.java:103-140`），Odoo 降为反模式对照（🟢 `stock_delivery/models/delivery_carrier.py:50-51` 命名约定派发，与 iDempiere 反射 Doc 工厂同类脆弱设计）
  - 异步下单（不阻塞主事务）；承运商配置参数化（url/凭证/OAuth2/trackingUrlTemplate）
  - 实施级延迟声明
  - Skill: none
- [x] **Add** 创建 `docs/architecture/b2b-integration.md`——EDI/ASN/B2B 集成契约（架构文档，集成层）。**设计依据：0001 §3.3**。**⚠️ 主证修正 + SPI 深度补充**：初版单 `IErpB2BIntegrationProvider` → **补"适用性派发 + 信封状态机表"**。内容须含：
  - 模块定位（集成层，独立 `module-b2b` 工程；ASN 实体表前缀归 module-b2b，本架构文档描述其语义）
  - 实体：`ErpB2bEdiFormat`(格式配置) + **`ErpB2bEdiDoc`(信封/事务，🟢 Odoo `account_edi_document.py:14-58` 范式：state to_send/sent/to_cancel/cancelled + error + blocking_level + attachment + UNIQUE(formatId,relatedBillType,relatedBillCode))** + `ErpB2bAsn`/`Line`(提前发货通知，须挂 sourceEdiDocId) + `ErpB2bCodeMapping`(内外代码映射) + `ErpB2bEdiLog`
  - **SPI 补适用性派发**（核心）：`IErpB2bEdiProvider.getApplicability(relatedBillType)` 返回 {outbound, inbound, batchable}（🟢 `account_edi_format.py:58-68` `_get_move_applicability`）+ `generatePayload`/`parsePayload` + `needsWebService`；双向（builder 导出 + decoder 导入，🟢 `sale_edi_ubl/models/sale_order.py:7-30`）
  - ASN 入站：Webhook HMAC 验签 → parsePayload → 建 Asn + EdiDoc(state=RECEIVED) → 弱指针关联 purchase 收货，**ASN 不直接写库存，由 purchase 决定**
  - Webhook 出站复用 integration-pattern.md 的 `ErpSysWebhookConfig/Log`（不另造）
  - **主证改 Odoo account_edi**（🟢 `account_edi_format.py:7-120` + `account_edi_document.py:14-58` + `sale_edi_ubl`）；**ERPNext edi 须标注"仅 code_list/common_code 映射，非完整 EDI 引擎，弱参考"**（🟢 源码确认 `erpnext/edi/doctype/` 仅两 doctype）
  - 异步发送（web service 类走异步队列，🟢 `account_edi_format.py:40-42`）
  - 核心零污染：全程弱指针反查 purchase/sales/inventory
  - 实施级延迟声明
  - Skill: none

Exit Criteria:

> 本阶段交付三份骨架文档 + SPI 契约（含方法签名/中立 DTO/状态机），每份明确"深化延迟到客户触发"。

- [x] 三份新文档创建完成，每份含：模块定位、最小实体清单（标注延迟）、**SPI 契约（三层或适用性派发 + 信封状态机，非单层 Provider）**、衔接点（弱指针，核心零污染）、实施级延迟声明、**证据强度标注 + 主证正确（TMS=Metasfresh、EDI=Odoo account_edi、ERPNext edi 标注非引擎）**
- [x] CRM 采用单实体 ErpCrmLead + ErpCrmStage 阶段表；TMS 采用三层 SPI + 裁决 D3 运费双路径；EDI 含适用性派发 + ErpB2bEdiDoc 信封状态机
- [x] 三模块定位为可选扩展工程，未污染 product-scope 10 域基线（不在核心实体加 carrierId/opportunityId）

---

### Phase 4 — 基线与索引同步

Status: completed
Targets: `docs/design/feature-inventory.md`、`docs/design/README.md`、`docs/backlog/implementation-roadmap.md`、`docs/analysis/README.md`
Skill: none

- Item Types: `Fix`
- Prereqs: Phase 1-3 完成

- [x] **Fix** `docs/design/feature-inventory.md` 补全 P1 强耦合域 + P1 独立扩展的功能条目（标注 owner doc，P1 独立扩展标注"可选模块，实施级设计延迟"）。
  - Skill: none
- [x] **Fix** `docs/design/README.md` 域索引补 `crm/`、`logistics/` 两个可选扩展域目录链接（标注"可选扩展，非 10 域基线"），`docs/architecture/` 索引补 `b2b-integration.md`。
  - Skill: none
- [x] **Fix** `docs/backlog/implementation-roadmap.md` 补"高级场景扩展"占位小节（记录 P1 独立扩展 + P2 的 Follow-up 触发条件）。**同时纠正 roadmap 自身的陈旧 owner-doc 漂移**：`:19-23` 阶段状态块仍标 phases 2-4 为 `todo`、`:50-56` 称"ORM 模型中尚无实体/无生成模块/项目尚未构建"，与 `product-scope.md:33-35`（codegen 已完成 145 实体）直接矛盾。此为已确认的 owner-doc 漂移（规则 13，不可降级为 follow-up），在同一次接触中修复。
  - Skill: none
- [x] **Decision** `docs/design/app-overview.md` 裁决：**不更新**。理由：app-overview 描述 10 域基线表面，本计划的 CRM/TMS/EDI 明确为可选扩展工程（Phase 3 Decision），不纳入 10 域基线；功能地图权威是 `feature-inventory.md`（已在第一项更新）。本裁决显式记录以避免执行者疑虑。
  - Skill: none
- [x] **Decision** `docs/design/domain-glossary.md` 术语评估：新设计将引入领域专有词（如承兑汇票/票据贴现/受托代销/VMI/AVL/CRP/APS/ASN/EDI）。裁决标准——**领域专有业务术语补入 glossary**（如承兑汇票、受托代销、VMI、AVL）；**标准技术缩写不补**（如 EDI/ASN/APS/CRP——通用业界术语）。按此标准评估并补条目。
  - Skill: none
- [x] **Fix** `docs/analysis/README.md` 在 06-30 分析报告条目补"本计划为其执行计划"回链。
  - Skill: none

Exit Criteria:

> 本阶段使所有新设计文档可从索引被发现，backlog 记录延迟项触发条件，并顺手纠正已发现的 roadmap owner-doc 漂移。

- [x] 六处索引/基线同步完成（`feature-inventory`/`design-README`/`roadmap`/`glossary`/app-overview 裁决记录/`analysis-README`），无悬空链接
- [x] roadmap 陈旧状态块（`:19-23`/`:50-56`）与 `product-scope.md:33-35` 的矛盾已纠正
- [x] app-overview "不更新"裁决显式记录（在 Phase 4 产出或日志）
- [x] glossary 按裁决标准补领域专有术语（承兑汇票/受托代销/VMI/AVL 等），标准缩写（EDI/ASN/APS/CRP）不补
- [x] P1 独立扩展 + P2 的延迟触发条件在 backlog 明确记录

---

## Draft Review Record

> 待独立草案审查填写。审查重点：① 范围是否合适（P0/P1 完整 vs P2 排除）；② 实体"声明但不落地 ORM"的边界是否清晰；③ Phase 3 骨架深度是否恰当（不过深也不过浅）；④ 与 product-scope 延迟范围、roadmap 当前阶段的一致性；⑤ 是否遗漏分析报告中的场景；⑥ Non-Goals 是否覆盖 P2/ABC。

- Independent draft review iteration 1: **acceptable** (0 blocking; 8 non-blocking suggestions raised, all adopted) — 审查确认：14 条模板规则全部 PASS；全部 file:line 引用经实时仓库核实属实；范围完整覆盖 13 场景 + ABC 无遗漏；ORM 保护边界强且显式；诚实处理证据强度；有效化解 product-scope 延迟范围冲突。采纳的 8 项改进：#1 Phase 3 类型标注 `Add | Decision`、#7 源审查轮次"三→两"已修；#2 glossary 术语评估、#3 app-overview 显式裁决、#4 Phase 2 弱依赖、#5 state-machine.md:88 澄清、#6 roadmap 陈旧状态块纠正、#8 TMS 证据收紧已纳入 Phase 1-4 执行项。
- Independent draft review iteration 2（基于深度对比 0001 的实质修订，2026-06-30）: **major revision applied** — 初版审查侧重事实核查+模板合规，未深入 erp-survey 源码对比。随后启动 0001 三层挖掘（调研报告+源码回查 `/Users/abc/sources/erp/`+提炼），发现并修正：① 3 处证据过度引申（Phase 1 票据 metasfresh.md:83/redragon-erp.md:41 实测非票据证据 → 改诚实标注开源零覆盖+Metasfresh Doc_BankStatement 科目分解范式；Phase 3 TMS 主证 Odoo → 改 Metasfresh SPI 三层；Phase 3 EDI 主证 → 改 Odoo account_edi，ERPNext edi 标注非引擎）；② 2 处设计错误（Phase 2 供应商评分卡归属 → 裁决 D5 拆分 master-data/purchase；Phase 3 TMS 单层 Provider → 改三层 Client/Factory/Registry）；③ 5 处裁决补入 Design Rationale（D1 CRP 不引入排产单/D2 NCR ESCALATED_TO_RECALL/D3 运费双路径/D4 备用金不建实体/D5 评分卡拆分）；④ 新增 Design Rationale 章节（范式 A 独立工程+核心零污染、范式 B SPI 注册中心统一、范式 C 证据诚实标注）；⑤ Phase 3 SPI 契约从"过浅"补到"恰当"（方法签名+中立 DTO+信封状态机）。**本修订未改变 Non-Goals/范围/结果表面，仅深化设计依据**。0001 分析文档作为本计划的设计依据输入存档。

## Closure Gates

> 仅在所有项目和每个阶段退出标准都勾选 `[x]` 后关闭。本计划为纯文档变更，删除代码验证命令门控（见指南 Closure Gates 节"对于无代码更改的计划，删除验证命令门控并说明原因"）。

- [x] 范围内行为完成（Phase 1-4 全部交付物落地）
- [x] 相关文档对齐（`projects`/`feature-inventory`/索引/backlog 与新设计文档一致）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（Draft Review Record 收敛到可接受）
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、`docs/logs/` 一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为人工门控占位符
- [x] 结束证据存在于 `Closure` 节
- [x] **纯文档计划验证门控**：所有新文档含必需章节（边界/实体清单标注待 ORM 落地/状态机/业财过账或跨域协作/证据声明），所有引用的 erp-survey 证据可在对应 file:line 核实（按分析报告强化后的方法学），无编造证据

## Deferred But Adjudicated

### P2 场景（明确不纳入核心，按需独立扩展）

- **DRP（分销需求计划）**
  - Classification: `out-of-scope improvement`
  - Why Not Blocking Closure: MRP 的"需求来源"已含安全库存补货，部分覆盖 DRP；完整多级分销网络仅在 >5 分仓场景有价值（分析报告 §5.1）
  - Successor Required: yes（触发条件：客户含大型多级分销网络）
- **HRMS / 薪酬 / 考勤**
  - Classification: `out-of-scope improvement`
  - Why Not Blocking Closure: HR 本地化极重，不适合通用底座；建议接入第三方 HR（分析报告 §5.2）。证据 `metasfresh.md:31,83` liberoHR/Payroll 属 Metasfresh
  - Successor Required: yes（触发条件：客户明确需要内建薪酬，或第三方 HR 集成不满足）
- **POS / 门店零售 / 多渠道电商**
  - Classification: `out-of-scope improvement`
  - Why Not Blocking Closure: 零售业专用，B2B ERP 不需要；电商属渠道层（分析报告 §5.3）
  - Successor Required: yes（触发条件：目标客户群体转向 B2C/零售）
- **售后服务 / 保修 / 客服工单**
  - Classification: `out-of-scope improvement`
  - Why Not Blocking Closure: 与 NCR/维护/退货重叠，缺的仅是 customer-facing ticketing（分析报告 §5.4）
  - Successor Required: yes（触发条件：客户需要独立客服工单系统）
- **库存 ABC 分类**
  - Classification: `optimization candidate`
  - Why Not Blocking Closure: 低优先通用能力，`module-inventory` ORM 无 `abcClass` 字段（分析报告 §二脚注）；erp-survey 未专门记载
  - Successor Required: yes（触发条件：盘点策略/成本控制需要 ABC 分级）

### ORM 实体落地（本计划声明但不实施）

- Classification: `moved to explicit successor ownership`
- Why Not Blocking Closure: 本计划 Non-Goals 明确不改 `model/*.orm.xml`（保护区域，`product-scope.md:58`）；设计文档中的实体名是建议命名
- Successor Required: yes（后续独立 ORM 计划，按 P0→P1 优先级落地，每个域 ORM 变更走 ask-first）

## Closure

Status Note: 计划 03 的 4 个 Phase 全部交付（9 份新设计文档 + 7 份索引/基线修正），独立结束审计通过（0 阻塞）。所有裁决 D1-D5 落地，证据诚实性 7 项关键检查全部通过，Non-Goals 严格遵守（ORM 零污染、无代码、无 P2 入核心）。纯文档计划，无代码验证命令。

Closure Audit Evidence:

- Auditor / Agent: 独立 general 子代理（新会话 ses_0e92d1cf0ffeZB1hEp73OTVoEW，无执行者上下文），research-only
- Verdict: **accept**（0 阻塞问题）
- Evidence: 
  - A 交付物存在性：9 份新建 + 7 份修改文档全部到位且非空
  - B Non-Goals（最高风险）：grep 全部 orm.xml 对 20 个计划声明实体 = ZERO HITS（ORM 零污染）；变更集无 .java/.xbiz/.view.xml（无代码）；P2 仅在 roadmap 延迟节；erp-survey 未改
  - C 必需章节：9 份新文档均含 边界/实体清单(标注待落地)/状态机/businessType或跨域协作/配置点/反模式/证据标注(🟢/🟡/⚪)
  - D 证据诚实性（核心门控）：treasury 未误引票据证据、TMS 主证 Metasfresh、EDI 主证 Odoo account_edi(ERPNext edi 标注非引擎)、D5 拆分落地、VMI 未声称借鉴 Odoo 结算、D2 ESCALATED_TO_RECALL 落地、CRM 弱指针无 opportunityId 污染；本项目 file:line 引用（manufacturing orm:362/363/328/331、inventory orm:229/184、quality orm:139/268、sales orm:97/339、purchase orm:277）全部核实属实
  - E 文本一致性：Phase 1-4 退出标准满足，roadmap 陈旧漂移已纠正，日志 06-30.md 有计划 03 条目
- 非阻塞建议（已记录，不影响关闭）：工作树混合状态，提交时需分阶段 stage 避免把无关 orm.xml 改动混入计划 03 commit

Follow-up:
- P1 独立扩展模块（CRM/TMS/EDI）深化到实施级：触发条件为客户行业/外部集成需求确认（见 Phase 3 各文档声明）
- P2 场景：触发条件见 Deferred But Adjudicated 各项
- ORM 实体落地：本计划完成后启动独立 ORM 计划（按 P0→P1 优先级，每域 ORM 变更走 ask-first）
