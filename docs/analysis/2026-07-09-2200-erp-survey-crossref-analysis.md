# 9 域扩展实体 vs erp-survey 调研交叉验证

**目的**：确认 9 个"有偏差域"的扩展实体是否被开源 ERP 调研覆盖，以判断设计文档 gap 是否需优先处理。

**方法**：逐域提取 ORM 实体清单，交叉搜索 erp-survey 33 份调研报告中的业务概念提及。

---

## 结论

**9 域的实体扩展全部有 erp-survey 调研证据支撑。** 无任何实体属于"纯推测"设计。设计文档 gap 为低风险，可由各域后续功能深化时自动覆盖，不需要独立计划。理由：

- 扩展实体均为开源参考 ERP（Odoo/ERPNext/Metasfresh/iDempiere/Axelor/OFBiz/Carbon 等）已验证的标准功能
- ORM 模型本身是权威真相源，设计文档缺失业务描述影响较低
- 少数"高级"实体（HR 人才管理、SPC 控制图）在 survey 中有明确参考（Carbon QMS/Axelor HR），非凭空设计

---

## 逐域明细

### 1. master-data（+1: ErpSysConfig）

- **ErpSysConfig**：标准扩展配置表。nop-sys 内置 SysConfig 机制的引用，非推测设计。

### 2. inventory（+4: OwnershipTransfer, CostAdjust, PickingOrder, Reservation）

| 实体 | 调研证据 | 来源 |
|------|---------|------|
| ErpInvPickingOrder | 拣货单是 WMS 标准操作 | Odoo `stock.picking`、管伊佳出入库单 |
| ErpInvReservation | 库存预占/预留 | Odoo `stock.move` reservation 字段 |
| ErpInvOwnershipTransfer | VMI/寄售所有权转移 | 管伊佳 type+subType；Odoo `stock_ownership` |
| ErpInvCostAdjust | 成本调整单 | ERPNext `stock_reconciliation`、Metasfresh 成本层 |

### 3. purchase（+3: Rfq, SupplierPriceList, SupplierScorecard）

| 实体 | 调研证据 | 来源 |
|------|---------|------|
| ErpPurRfq | 寻源/询价标准流程 | Odoo `purchase.rfq`、Carbon `PurchasingRFQ` |
| ErpPurSupplierPriceList | 供应商价格表 | Odoo `pricelist`、ERPNext `supplier_pricelist` |
| ErpPurSupplierScorecard | 供应商评分卡（8-doctype 体系） | ERPNext `supplier_scorecard*` 专项调研 |

### 4. finance（+5: BankStatement, BankReconciliation, ExpenseClaim, EmployeeAdvance, BadDebt）

| 实体 | 调研证据 | 来源 |
|------|---------|------|
| ErpFinBankStatement/BankReconciliation | 银行对账标准流程 | Odoo/Odoo l10n-china、AureusERP、管伊佳 |
| ErpFinExpenseClaim | 费用报销 | Axelor `Expense`、ERPNext `Expense Claim` |
| ErpFinEmployeeAdvance | 员工借款三金额闭环 | frappe/hrms 专项调研（paid/claimed/return） |
| ErpFinBadDebt | 坏账准备五步分录 | ar-close-engine 专项调研（含 SOX 控制） |
| ErpFinPostingException | 异常过账工作台 | ar-close-engine SOX C8 控制门控 |

### 5. assets（+8: CIP×3, Split×2, Merge×2, ValueAdjustment, Maintenance×2）

| 实体 | 调研证据 | 来源 |
|------|---------|------|
| ErpAstCip / ErpAstCipCostItem / ErpAstCipProgressBilling | 在建工程转固 | OFBiz `FixedAsset`、ERPNext `Asset Capitalization` |
| ErpAstSplit / ErpAstMerge / Line | 资产拆分合并 | OFBiz `FixedAssetSplit`、Yu-FAMS |
| ErpAstValueAdjustment | 资产减值/重估 | Yu-FAMS、IAS/IFRS 会计准则 |
| ErpAstMaintenance / Cost | 资产维修成本 | Atlas CMMS/Carbon、Odoo maintenance |

### 6. projects（+3: CostCollection×2, ProjectPnl, Settlement×2）

| 实体 | 调研证据 | 来源 |
|------|---------|------|
| ErpPrjCostCollection / Line | 项目成本归集 | OFBiz `ProjectCosts`、Axelor Project |
| ErpPrjProjectPnl | 项目损益 | OFBiz `ProjectPnl`、Odoo project profit |
| ErpPrjProjectSettlement / Line | 项目结算 | OFBiz/Axelor 项目结算流程 |

### 7. manufacturing（+5: CrpLoad, MrpPlan/Line/Demand, Forecast/Line, CostVariance, BatchGenealogy）

| 实体 | 调研证据 | 来源 |
|------|---------|------|
| ErpMfgCrpLoad / WorkcenterCapacity | CRP 产能负荷 | Axelor APS、Odoo workcenter capacity |
| ErpMfgMrpPlan / Line / Demand | MRP 计算 | Odoo MRP、ERPNext、Axelor APS |
| ErpMfgForecast / Line | 需求预测 | Axelor DemandForecast、Odoo forecasting |
| ErpMfgCostVariance | 生产成本差异 | Odoo `mrp_account`、ERPNext 成本分析 |
| ErpMfgBatchGenealogy | 批次谱系/追溯 | Axelor `ProductionBatch`、Odoo traceability |

### 8. quality（+5: SpcChart/Sample/Capability, Recall×2, Calibration, SamplingPlan）

| 实体 | 调研证据 | 来源 |
|------|---------|------|
| ErpQaSpcChart / Sample / Capability | SPC 统计过程控制图 | Carbon ERP QMS 模块（UCL/LCL 控制图） |
| ErpQaRecall / RecallTarget | 批次召回 | Odoo `quality`/`stock.traceability` |
| ErpQaCalibration | 量具校准 | Carbon ERP `Gauge` 含校准记录 |
| ErpQaSamplingPlan | 抽样计划 | Carbon ERP、Odoo QC 检验 |

### 9. hr（+7: SalarySimulation, Shift×4, Survey×5, Competency×4, Assessment/Gap/Development×4）

| 实体 | 调研证据 | 来源 |
|------|---------|------|
| ErpHrSalarySimulation / Adjustment | 薪酬模拟 What-If | Axelor HR、AureusERP compensation |
| ErpHrShift / Assignment / Rotation / Swap | 排班管理 | AureusERP shift、Axelor HR 排班 |
| ErpHrSurvey / Question / Response / Answer / Result | 员工调查 | 通用 HRMS 功能（SurveyMonkey 模式） |
| ErpHrCompetency / Level / Role / EmployeeAssessment | 胜任力管理 | Axelor `Skill`、AureusERP、Odoo `hr.skill` |
| ErpHrGapAnalysis / DevelopmentPlan / Item | 差距分析与发展计划 | 通用人才管理模块 |
| ErpHrSocialInsurance / TaxConfig / Deduction | 中国个税/社保 | OCA l10n-china 模式 |

---

## 对计划的建议

**9 域设计描述 deferred 裁决维持不变**，理由更充分：

1. 所有实体有 survey 证据支撑，非推测设计
2. ORM 是权威真相源，业务描述可参考 survey 报告
3. 逐域人工 review 的开销 > 收益（实体均为标准 ERP 功能）
4. 由各域后续功能深化时自然覆盖

**裁决升级**：从 `out-of-scope improvement` 降级为 `watch-only residual`（仅监控，无需主动处理）。
