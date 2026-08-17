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

> **实现注记（plan 2026-08-14-2304-3，RC-R1.27 P1-RC-053 调度接线）**：自动触发路径已接线——`app-erp-all` job conf `erp-prj-pnl-calc.job.yaml`（jobName=erp-prj-pnl-calc，enabled `@cfg:nop.job.erp-prj-pnl-calc.enabled|false` 部署 opt-in；cronExpr `@cfg:erp-prj.pnl-calc-cron|0 0 1 * * ?`）→ invoker `nopBatchTaskRunner` → `pnl-calc.batch.xml`（loader: status in [DRAFT,OPEN,ON_HOLD]，chunk 事务）→ `ErpPrjProjectPnlCalcHelper.recalculateOne()`（逐条 REQUIRES_NEW 独立事务 + try/catch WARN 单条失败隔离 + `batchChunkCtx.serviceContext` null 兜底 `ServiceContextImpl`）→ `IErpPrjProjectPnlBiz.refreshPnl`（既有 `ProjectPnlCalculator` 引擎）。业务键双层门控：`erp-prj.pnl-auto-calc-enabled`（默认 false，总开关）+ `erp-prj.pnl-calc-cron`（默认 `0 0 1 * * ?`，空值=跳过）。批量调度路径测试 `TestErpPrjPnlCalcJob`（5 组 batch 任务级）。多币种折算（exchangeRate=ONE）仍归 P2-RC-050 successor，非本行范围。

2. **结算**:项目 status→COMPLETED 时,基于最新 PnlSnapshot 生成 ErpPrjProjectSettlement(settlementType=FINAL);如客户合同为总价合同且结算后仍有资产(如自建固定资产),settlementType=CLOSE 触发 transferToAsset=true,调用 IErpAstAssetBiz 生成资产卡片(assets 域),并生成转固凭证(经 finance 域 IErpFinAcctDocProvider 注册 PROJECT_SETTLEMENT 类型)。

> **实现注记（plan 2026-08-17-0142-1，RC-R1.63 P1-RC-052 UC-PRJ-07 ④⑤ 质保金留存 + 到期返还）**：三裁决已落地——
> **D1 留存填充（选项 A，config 驱动仅 FINAL）**：`createSettlement` 对 `settlementType=FINAL`（竣工结算，L1 UC-PRJ-07 场景）自动填 `retentionAmount = finalRevenue × erp-prj.settlement-retention-ratio`（scale 4 HALF_UP；默认 0=设计性 opt-in——留存逻辑存在且配置驱动，零为显式 opt-in 默认非静默缺失）+ `retentionDueDate = businessDate + erp-prj.settlement-retention-due-months`（默认 12）；INTERIM（阶段结算无尾款留存语义）/CLOSE（自建转固非应收）不填；手工覆盖路径保留（CRUD update 可改）。否决纯手工录入（留存语义依赖操作员自觉，违反 L1「留存」自动行为）。
> **D2 质保金凭证（选项 A，扩展 ProjectSettlementAcctDocProvider，保留 PROJECT_SETTLEMENT businessType 零字典变更）**：主结算凭证 createFacts 留存时增平衡腿「借 1122 应收账款-质保金 / 贷 2241 其他应付款-质保金」（金额=retentionAmount，标 projectId 辅助核算；ACCOUNT_KEY_RETENTION_RECEIVABLE/RETENTION_PAYABLE 供 GL mapping 规则覆盖 subjectCode，空匹配回退 Provider 默认编码——**部署须预置 1122/2241 科目**，否则过账抛 ERR_SUBJECT_NOT_FOUND）；返还经同 Provider 同 businessType，billData `RETENTION_RETURN=true` + billHeadCode=结算单号`#RETURN` 独立凭证，镜像腿「借 2241 / 贷 1122」对冲清零。生命周期一致性：主结算红冲（reverse 按结算单号自动覆盖留存腿），返还凭证独立存在 → `reverseSettlement`/`cancel` 前置守卫「未返还」（已返还拒绝红冲/取消，避免返还凭证悬挂）。否决新 businessType 枚举（须 orm.xml 字典变更超 B 类授权边界 + 双 Provider 冗余）。
> **D3 到期返还（选项 A，已过账凭证反查幂等）**：`IErpPrjProjectSettlementBiz.returnRetention(settlementId)` @BizMutation（per-mutation `ErpPrjProjectSettlementReturnRetentionProcessor`）；守卫链 docStatus=APPROVED + approveStatus=APPROVED + posted=true + retentionAmount>0 + retentionDueDate<=today，失败抛 `ERR_RETENTION_RETURN_NOT_ALLOWED`（中文描述 + reason 参数）；幂等标记 = ErpFinVoucherBillR（billCode=结算单号`#RETURN` + businessType=PROJECT_SETTLEMENT）存在性反查（镜像 assets #CATCHUP 范式，零 ORM 变更），已返还重复调用 no-op 零副作用；返还凭证过账失败显式抛 `ERR_RETENTION_RETURN_POSTING_FAILED`（与主结算过账失败隔离——返还是用户显式操作不吞异常）。否决 remark 文本弱标记（不可反查不可红冲）。合同域驱动的留存比例（结算 ORM 无 contractId 维度）登记 successor；测试 `TestErpPrjProjectSettlementRetention` 10 组（填充/零填充/返还成功/幂等/守卫×4/凭证行级 + GL 平衡/红冲守卫）。

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

## 命名与编号收敛

- **文件名**：本文件保留 `projects/profitability.md`（15+ 处仓库引用：use-cases/dashboards/analysis/audits/job-scheduling/logs）。`extended-roadmap.md` 既有目标路径 `projects/pnl-settlement.md` 反向修正为 `projects/profitability.md`（残留风险：零——pnl-settlement.md 从未存在）。
- **UC 编号口径**：以本域 `projects/use-cases.md` 为权威源——UC-PRJ-05=任务 DAG 校验、UC-PRJ-06=项目损益汇总、UC-PRJ-07=竣工结算与质保金、UC-PRJ-08=项目结算转固。`extended-roadmap.md` 原标注「2.6b=UC-PRJ-05~07、2.6c=UC-PRJ-08」修正为「2.6b=UC-PRJ-06~08（损益/结算/转固）、2.6c=UC-PRJ-05（DAG 校验）」。

## 参考

- `docs/analysis/erp-survey/2026-06-22-0000-odoo.md`(sale_project/sale_timesheet)
- `docs/design/projects/cost-collection.md`(成本归集,盈利分析数据源)
