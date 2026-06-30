# 供应商评分卡 / AVL 准入（Supplier Evaluation）

## 目的

设计供应商周期评分卡（Supplier Scorecard）与 AVL 准入（Approved Vendor List）的业务语义、评分公式体系与 RFQ 联动。补齐 RFQ/报价已存在但评分/准入缺失的 P1 缺口。

## 边界

- 本模块负责：供应商评分维度/公式/权重建模、周期化评分评估、评级档位、评级 → RFQ 联动。
- **实体归属裁决 D5（拆分）**：AVL 准入（资格主数据）放 master-data，评分卡周期数据（业务绩效）放 purchase。理由：评分引用采购链数据（RFQ/PO/质检单），属采购绩效产物（`domain-design-guidelines.md` §1.1 单一职责）。
- 本模块不负责：供应商主数据（master-data 域 `ErpMdPartner`）；质检/价格/交货原始数据（已有 quality/purchase 实体）。
- 实体为**建议命名，待 ORM 计划落地**（`model/*.orm.xml` 是 ask-first 保护区域，本文件不复述 schema）。

## 设计依据

> 来源 `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §2.2。

### 核心设计点（ERPNext 8-doctype 完整体系）

🟢 ERPNext `erpnext/buying/doctype/supplier_scorecard*/`（源码实测）是 8-doctype 完整体系，核心范式：

1. **评分 = 维度(criteria) × 公式(formula) × 权重(weight)**，公式引用变量(variable)从业务 path 取值（🟢 `supplier_scorecard_criteria` formula/weight + `_variable` path）。
2. **周期化评估**（起止日期 + total_score）——评分是时点快照，非实时累加（🟢 `supplier_scorecard_period`）。
3. **评分 → 评级档位(standing) → RFQ 三档联动**（warn/hold/prevent）（🟢 `supplier_scorecard` warn_rfqs/hold_rfqs/prevent_rfqs）。

### 数据源已存在（强化"仅评分/准入缺"判断）

| 评分维度 | 数据源 | 实测位置 |
|---|---|---|
| 质量合格率 | `ErpQaInspection.supplierId` | 🟢 `module-quality/model/app-erp-quality.orm.xml:139` |
| 价格竞争力 | `ErpPurSupplierPriceList` | 🟢 `module-purchase/model/app-erp-purchase.orm.xml:277` |
| 按时交货率 | PO/Receive 交货日期 | 🟢 采购订单/入库单已有交货日期字段 |
| 询价响应 | RFQ/报价 | 🟢 `ErpPurRfq`(:160)/`ErpPurQuotation`(:214) |

## 实体清单

> AVL 准入表前缀 `erp_md_`（master-data），评分卡表前缀 `erp_pur_`（purchase）。类名/字典按所属域。以下为建议命名，待 ORM 计划落地。

### master-data 域：AVL 准入

#### ErpMdSupplierApproval（供应商准入资格，表 `erp_md_supplier_approval`）

| 字段 | 含义 |
|---|---|
| id/partnerId/orgId | 标准 |
| approvalType | dict `erp-md/supplier-approval-type`：NEW（新供应商）/RENEWAL（续期） |
| materialCategoryId | 准入物料类别（供应商对哪类物料有资格） |
| validFrom/validTo | 资格有效期 |
| qualificationDoc | 资质文件（ISO/行业认证） |
| status | dict `erp-md/supplier-approval-status`：APPLIED（已申请）/APPROVED（已批准）/PROBATION（试用期）/SUSPENDED（已暂停）/REJECTED（已驳回） |
| approvedBy/approvedAt | 批准人/时间 |
| 标准审计字段 | |

**状态机**：`APPLIED → APPROVED`（正式准入）；`APPROVED → PROBATION`（试用，新供应商）；`PROBATION → APPROVED`（试用通过）；`APPROVED/PROBATION → SUSPENDED`（评分 standing=RED 触发，见联动规则）；`SUSPENDED → APPROVED`（恢复，需审批）；`APPLIED → REJECTED`。

### purchase 域：周期评分卡

#### ErpPurSupplierScorecard（评分卡周期，表 `erp_pur_supplier_scorecard`）

| 字段 | 含义 |
|---|---|
| id/partnerId/orgId | 标准 |
| periodFrom/periodTo | 评分周期（起止日期） |
| totalScore | 总分（=Σ criteria 加权得分，派生） |
| standing | dict `erp-pur/supplier-standing`：GREEN（优秀）/YELLOW（待改进）/RED（不合格） |
| warnThreshold/holdThreshold/preventThreshold | 三档阈值（总分低于则触发对应 RFQ 联动） |
| status | dict `erp-pur/scorecard-status`：DRAFT/FINALIZED |
| 标准审计字段 | |

#### ErpPurSupplierScorecardCriteria（评分维度，表 `erp_pur_supplier_scorecard_criteria`）

| 字段 | 含义 |
|---|---|
| id/scorecardId/orgId | 标准 |
| criteriaName | 维度名（质量/价格/交货/响应） |
| weight | 权重（0-100，Σ=100） |
| formula | 公式（引用 variable，用 nop 规则引擎/DSL 表达） |
| score | 维度得分（公式计算结果，0-100） |
| weightedScore | 加权得分（=score × weight/100） |
| 标准审计字段 | |

#### ErpPurSupplierScorecardVariable（评分变量，表 `erp_pur_supplier_scorecard_variable`）

| 字段 | 含义 |
|---|---|
| id/criteriaId/orgId | 标准 |
| variableName | 变量名（如 pass_rate/on_time_rate/price_index） |
| path | 业务取值路径（如 `ErpQaInspection` 合格数/总数） |
| value | 取值（公式执行时从 path 取） |
| 标准审计字段 | |

## 业务规则

1. **评分卡不过账**：评分是绩效评估产物，不产生会计凭证。
2. **公式用 nop 规则引擎/DSL，不硬编码 Java**：criteria.formula 引用 variable，variable.path 从业务实体取值（🟢 ERPNext 范式）。新增维度 = 配置 criteria + variable，不改代码。
3. **standing → RFQ 三档联动**：
   - standing=GREEN：正常询价。
   - standing=YELLOW：RFQ 创建时 warn（提示该供应商近期评分偏低）。
   - standing=RED：RFQ 创建时 hold（需质量主管审批）或 prevent（直接禁止该供应商参与 RFQ）。
4. **standing=RED → 自动写 SupplierApproval=SUSPENDED**：评分 finalize 后若 standing=RED，同步更新 master-data 的 `ErpMdSupplierApproval.status=SUSPENDED`，使暂停立即生效。
5. **RFQ 创建校验**：RFQ 创建时校验供应商 `ErpMdSupplierApproval.status`（SUSPENDED/REJECTED 的供应商不可作为 RFQ 收件人）。
6. **周期快照非实时累加**：评分按 period 取数计算，是时点快照（🟢 ERPNext `supplier_scorecard_period` 范式）。

## 跨域协作

| 对端 | 协作内容 |
|---|---|
| master-data（ErpMdSupplierApproval） | purchase 评分 finalize 时同步写 master-data 准入状态（SUSPENDED） |
| master-data（ErpMdPartner） | 供应商主数据，评分对象 |
| quality（ErpQaInspection） | 质量合格率数据源（variable.path 取值） |
| purchase（ErpPurSupplierPriceList） | 价格竞争力数据源 |
| purchase（RFQ） | 评分联动校验（创建 RFQ 时校验 standing/approval） |

## 配置点

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `erp-pur.scorecard-evaluation-cron` | — | 周期评估 cron（如月度） |
| `erp-pur.scorecard-prevent-on-red` | true | standing=RED 时是否禁止供应商参与新 RFQ（false=hold 需审批） |

## 反模式警示

- ⛔ **评分实时累加在供应商主数据单字段**（`ErpMdPartner.qualityScore`）——🟢 ERPNext 特意做 period 快照表（`supplier_scorecard_period`），实时累加丢失历史趋势且耦合主数据。
- ⛔ **公式硬编码 Java**——用 nop 规则引擎/DSL，新增维度零改代码。
- ⛔ **评分耦合进 RFQ 审核事务**——评分 finalize 与 RFQ 审核是独立事务，RFQ 只读 standing，不参与评分计算。

## 菜单归属

- purchase 域「供应商评估」分组：供应商评分卡、评分维度、评分变量。
- master-data 域「供应商准入」分组：供应商准入资格（AVL）。

## 证据强度标注

| 证据 | 强度 | 说明 |
|---|---|---|
| ERPNext 8-doctype 评分体系 | 🟢 | `erpnext/buying/doctype/supplier_scorecard*/` 源码实测 |
| 评分=维度×公式×权重，公式引用变量 | 🟢 | `supplier_scorecard_criteria`(formula/weight) + `_variable`(path) 源码实测 |
| 周期化评估（period 快照） | 🟢 | `supplier_scorecard_period` 源码实测 |
| standing → RFQ 三档联动 | 🟢 | `supplier_scorecard` warn_rfqs/hold_rfqs/prevent_rfqs 源码实测 |
| 质量合格率数据源 | 🟢 | `module-quality/...orm.xml:139` ErpQaInspection.supplierId 实测 |
| 价格竞争力数据源 | 🟢 | `module-purchase/...orm.xml:277` ErpPurSupplierPriceList 实测 |

## 参考

- `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §2.2（设计依据）
- `docs/design/purchase/README.md`（采购域、RFQ/报价）
- `docs/design/domain-design-guidelines.md` §1.1（单一职责归属裁决）
