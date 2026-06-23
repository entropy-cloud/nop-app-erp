# 项目盈利分析(Project Profitability)

## 目的

设计项目维度的损益汇总与结算能力:项目收入(开票)+ 成本(工时/物料/费用)汇总、毛利分析、项目结算(关闭/转固)。

## 设计边界

盈利分析只做**项目维度的损益汇总与结算**,不做总账凭证(凭证归 finance 域,经 IErpFinAcctDocProvider 注册)。对照 Odoo sale_project/sale_timesheet、ERPNext projects。

**数据源(复用现有实体,不重新设计成本归集)**:
- 收入侧:ErpPrjBilling(已有,开票)+ 可选 ErpPrjMilestone.billingAmount
- 成本侧:ErpPrjCostCollection(已有,工时/物料/费用归集)+ ErpPrjTimesheet.costAmount 明细

## 实体清单

> 字段约定遵循 `docs/design/domain-design-guidelines.md` §10/§11。表前缀 `erp_prj_`、类名 `ErpPrj*`。

### ErpPrjProjectPnl(项目损益汇总,表 `erp_prj_project_pnl`)

| 字段 | 含义 |
|---|---|
| id/code/orgId | 标准 |
| projectId | 项目(→ErpPrjProject) |
| periodFrom/periodTo | 汇总期间 |
| currencyId/exchangeRate/amountSource/amountFunctional | 多币种四件套 |
| revenueAmount | 收入合计(来自 Billing.amountFunctional 汇总) |
| costLabor | 人工成本(CostCollection.costCategory=LABOR) |
| costMaterial | 物料成本(costCategory=MATERIAL) |
| costExpense | 费用成本(costCategory=EXPENSE) |
| costSubcontract | 分包成本(costCategory=SUBCONTRACT) |
| totalCost | 成本合计(四项之和) |
| grossProfit | 毛利(revenueAmount − totalCost) |
| grossMarginPct | 毛利率%(grossProfit / revenueAmount × 100) |
| committedCost | 已承诺成本(来自 ErpPrjBudgetLine.committedAmount) |
| budgetAmount | 预算(来自 ErpPrjBudget.totalAmount) |
| forecastCompleteCost | 完工预测成本(EAC = 实际成本 + ETC) |
| calcStatus | 计算状态 dict `erp-prj/pnl-calc-status`:PENDING=10/CALCULATED=20 |
| posted/postedAt/postedBy | 业财过账(损益汇总是否已生成项目损益凭证) |
| docStatus/approveStatus | 双轴状态(复用 erp-prj 字典) |
| 标准审计字段 | |

### ErpPrjProjectSettlement(项目结算单,表 `erp_prj_project_settlement`)

| 字段 | 含义 |
|---|---|
| id/code/orgId | 标准 |
| projectId | 项目 |
| customerId | 客户(→ErpMdPartner) |
| businessDate | 结算日期 |
| currencyId/exchangeRate/amountSource/amountFunctional | 多币种四件套 |
| settlementType | 结算类型 dict `erp-prj/settlement-type`:FINAL=10(竣工结算)/INTERIM=20(阶段)/CLOSE=30(关闭转固) |
| pnlSnapshotId | 关联损益汇总(结算依据,→ErpPrjProjectPnl) |
| finalRevenue | 最终结算收入 |
| finalCost | 最终结算成本 |
| finalProfit | 最终损益 |
| retentionAmount | 质保金/保留款(尾款留存) |
| retentionDueDate | 质保金到期 |
| transferToAsset | 是否转固定资产(settlementType=CLOSE 时使用) |
| assetCardId | 转固后的资产卡片(→ErpAstAsset 跨域 notGenCode) |
| settlementVoucherCode | 结算凭证号(反查 finance 凭证,凭证指针) |
| docStatus/approveStatus | 双轴状态 |
| posted/postedAt/postedBy | 业财过账 |
| approvedBy/approvedAt | 审批 |
| 标准审计字段 | |

### ErpPrjProjectSettlementLine(结算明细,表 `erp_prj_project_settlement_line`)

| 字段 | 含义 |
|---|---|
| id/settlementId/lineNo | 主键/父表/行号 |
| lineType | dict:INCOME=10/COST=20 |
| sourceBillType/sourceBillCode | 来源单据三元组(BILLING/COST_COLLECTION) |
| subjectId | 科目(→ErpMdSubject) |
| amount | 金额 |
| 标准审计字段 | |

> 新增字典:`erp-prj/pnl-calc-status`、`erp-prj/settlement-type`。

## 关键流程

1. **损益汇总计算**:依赖 nop-job 定时任务(按月/按里程碑触发),扫描项目下所有 ErpPrjBilling 与 ErpPrjCostCollection,按 costCategory 聚合到 ErpPrjProjectPnl。多币种统一折算到 currencyId。

2. **结算**:项目 status→COMPLETED 时,基于最新 PnlSnapshot 生成 ErpPrjProjectSettlement(settlementType=FINAL);如客户合同为总价合同且结算后仍有资产(如自建固定资产),settlementType=CLOSE 触发 transferToAsset=true,调用 IErpAstAssetBiz 生成资产卡片(assets 域),并生成转固凭证(经 finance 域 IErpFinAcctDocProvider 注册 PROJECT_SETTLEMENT 类型)。

3. **业财一体**:Pnl/Settlement 不直接生成凭证,而是通过 posted=false + 事件驱动(模式 B)通知 finance 域,finance 按 ERPNext on_submit 钩子模式统一过账。

## 与现有实体的关系

- **ErpPrjBilling**:收入侧数据源。
- **ErpPrjCostCollection/ErpPrjTimesheet**:成本侧数据源。
- **ErpPrjBudget/BudgetLine**:预算/承诺成本数据源。
- **ErpAstAsset**:结算转固的目标(跨域 notGenCode)。
- **finance IErpFinAcctDocProvider**:注册 PROJECT_SETTLEMENT 类型生成凭证。
- **nop-job**:损益汇总计算依赖定时任务。

## 关键决策

> **复用现有 Billing/CostCollection,只新增汇总与结算实体** —— 不重新设计成本归集(已有 ErpPrjCostCollection),盈利分析在其上做项目维度汇总。结算转固走 assets 域 IErpAstAssetBiz + finance 凭证,保持业财边界清晰。

## 菜单归属

projects 域「盈利分析」分组:项目损益汇总、项目结算单。

## 参考

- `docs/analysis/erp-survey/2026-06-22-0000-odoo.md`(sale_project/sale_timesheet)
- `docs/design/projects/cost-collection.md`(成本归集,盈利分析数据源)
