# 预算管理(Budget)

## 目的

设计 ERP 的预算编制、预算控制、预算对比能力,实现管理会计的预算管理闭环。

## 设计范式

对照 iDempiere `Fact.java:78-84` 的四种 PostingType(Actual/Budget/Commitment/Reservation)。**预算作为 PostingType=BUDGET 的"影子凭证"**,与实际凭证(PostingType=ACTUAL)并行入账,复用同一套凭证引擎、GlBalance、试算平衡机制,零特例。赤龙 ERP 无预算模块(反向印证走 iDempiere 范式)。

预算执行数 = 同维度(科目×期间×成本中心×项目)的 Budget 凭证累计;预算余量 = 预算数 − 实际数 − 承付款,**无需新建预算余额表,复用 `ErpFinGlBalance` 的 `postingType` 维度即可**。

## 实体清单

> 字段约定遵循 `docs/design/domain-design-guidelines.md` §10(标准字段)+ §11(状态机)。表前缀 `erp_fin_`、类名 `ErpFin*`、字典 `erp-fin/*`。ORM 权威模型以 `module-finance/model/app-erp-finance.orm.xml` 为准。

### ErpFinBudgetScenario(预算方案,表 `erp_fin_budget_scenario`)

管理预算版本(年度预算/滚动预算/调整预算),一个方案下挂全部预算行。

| 字段 | 含义 |
|---|---|
| id/code/name/orgId | 标准 |
| acctSchemaId | 账套(多账套独立预算) |
| fiscalYear | 预算年度 |
| scenarioType | 方案类型 dict `erp-fin/budget-scenario-type`:ANNUAL/ROLLING/ADJUSTMENT |
| parentScenarioId | 调整预算的源方案(版本链) |
| validFrom/validTo | 生效区间 |
| currencyId/exchangeRate/amountSource/amountFunctional | 多币种四件套(预算币种) |
| controlLevel | 控制级别 dict:NONE/WARN/HARD(仅告警/硬拦截) |
| docStatus | dict `erp-fin/budget-status`:DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED |
| approveStatus | dict `erp-fin/approve-status`(共用):UNSUBMITTED/PENDING/APPROVED/REJECTED |
| 标准审计字段 | version/delVersion/createdBy/createTime/updatedBy/updateTime/remark |

**状态机**:`DRAFT → SUBMITTED → APPROVED`(终态,写入 postingType=BUDGET 的初始预算凭证);`SUBMITTED → REJECTED → DRAFT`(修改重提);`APPROVED → CANCELLED`(红冲原预算凭证)。**APPROVED 才生效参与控制**。

### ErpFinBudgetLine(预算明细行,表 `erp_fin_budget_line`)

按"科目 × 期间 × 维度"切分,每行一个预算额度。

| 字段 | 含义 |
|---|---|
| id/scenarioId/lineNo/orgId | 标准 |
| acctSchemaId | 账套 |
| periodId | 会计期间(粒度=月/季/年) |
| subjectId/subjectCode | 预算科目 |
| costCenterId | 成本中心(见 cost-center.md) |
| departmentId/projectId/partnerId/warehouseId/materialId | 复用现有辅助维度 |
| budgetAmountSource/budgetAmountFunctional | 预算金额(本位币 = source × rate) |
| currencyId/exchangeRate | 多币种四件套 |
| commitmentAmount | 累计承付款(PostingType=COMMITMENT 凭证汇总,派生) |
| actualAmount | 累计实际发生(PostingType=ACTUAL 凭证汇总,派生) |
| availableAmount | 预算余量(= budget − commitment − actual,派生,查询时计算不落库) |

### ErpFinBudgetControlLog(预算控制日志,表 `erp_fin_budget_control_log`)

审计超预算拦截/放行记录。

| 字段 | 含义 |
|---|---|
| id/orgId/businessDate | 标准 |
| scenarioId/budgetLineId | 关联预算 |
| sourceBillType/sourceBillCode | 触发单据(PURCHASE_ORDER/AP_PAYMENT 等) |
| subjectId/costCenterId/projectId/periodId | 命中维度 |
| requestedAmount | 申请占用金额 |
| committedAmount | 实际占用 |
| actionResult | dict:PASS/WARNED/BLOCKED |
| operatorId/operatedAt/reason | 操作人/时间/原因 |

## 业务规则

1. **预算方案审批即过账**:`ErpFinBudgetScenario` APPROVED 时生成 `postingType=BUDGET` 的预算凭证,借贷规则按 `subject.direction` 自动取(资产/费用类记借方,负债/收入类记贷方),凭证头 `acctSchemaId` 来自 scenario。该凭证走正常 `DRAFT → POSTED` 流程并写入 `ErpFinVoucherBillR`(billType=BUDGET_SCENARIO)。

2. **预算控制钩子位置**:作为 `IErpFinFactsValidator` 之外的**业务校验扩展点**——在 purchase/sales 域审核动作的事务内同步调用 `IErpFinBudgetControlBiz.check(subjectId, costCenterId, periodId, amount, sourceBill)`。返回 BLOCKED → throw 阻断审核;WARN → 写日志放行;PASS → 静默。这是强一致校验(控制必须实时),不走事件。

3. **承付款生成**:采购订单 APPROVED 时生成 `postingType=COMMITMENT` 凭证;订单 CANCELLED 或被发票接收时红冲 COMMITMENT。budgetLine.commitmentAmount = Σ Commitment 凭证。

4. **实际数派生**:实际凭证 postingType=ACTUAL(现有所有凭证默认),actualAmount = Σ Actual 凭证在匹配维度的本位币金额。

5. **预算对比**:报表直接按 `(acctSchemaId, subjectId, periodId, costCenterId, projectId, postingType)` 分组 `ErpFinVoucherLine`,得到 Budget/Commitment/Actual 三列;无需独立预算余额表。

6. **期间控制**:预算凭证同样受 `ErpFinAccountingPeriodStatus.glStatus` 约束(已结账期间不可改预算)。

7. **多账套独立**:管理账有预算、税务账通常无预算——通过 `ErpFinBudgetScenario.acctSchemaId` 隔离。

8. **控制级别 HARD 下**:预算余量 < 0 时采购订单/付款单审核抛 `NopException`,单据保持 SUBMITTED 不前推。

## 与现有实体的关系

- **ErpFinVoucher/VoucherLine/GlBalance**:新增 `postingType` 列(dict `erp-fin/posting-type`:ACTUAL=10/BUDGET=20/COMMITMENT=30/RESERVATION=40),ACTUAL 为默认值,向后兼容。
- **ErpMdSubject**:新增 `isBudgetable BOOLEAN` 控制是否参与预算。
- **成本中心**:budgetLine.costCenterId 依赖 cost-center.md 的 ErpMdCostCenter。
- **purchase/sales 域**:通过 IErpFinBudgetControlBiz 同步接口(强一致)。

## 关键决策

> **预算凭证用 PostingType=BUDGET** —— 与 iDempiere Fact.java:78-84 完全一致,让预算/实际复用同一套凭证引擎、GlBalance、试算平衡机制,零特例。

## 菜单归属

finance 域「预算管理」分组:预算方案、预算明细、预算控制日志(预算 vs 实际对比报表)。

## 参考

- `docs/analysis/erp-survey/2026-06-22-0000-idempiere.md`(GL Budget/PostingType/GL Distribution)
- `docs/analysis/erp-survey/2026-06-22-0000-redragon-erp.md`(赤龙无预算,反向印证)
- `docs/design/finance/posting.md`(IErpFinFactsValidator 扩展点)
