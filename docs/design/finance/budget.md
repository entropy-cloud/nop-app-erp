# 预算管理(Budget)

> 基线：ORM 3 实体 + postingType=BUDGET 影子凭证范式 + HARD/WARN/NONE 三级预算控制 SPI + 预算对比报表。
> 余额统一从 `ErpFinVoucherLine` 聚合，不动 `ErpFinGlBalance`；实际数聚合（损益结转/坏账/年报/试算平衡/汇兑）已隔离 BUDGET 凭证。
> **A2 新增能力**：多年度视图（`budgetGroupCode`）/ 滚动预算自动复制引擎（rollForward）/ 预算结转规则引擎（carryForward）/ 承付会计（COMMITMENT 凭证 + 占用释放 SPI）/ 滚动+结转日志实体（RollforwardLog / CarryForwardLog）。
> Deferred successor：跨币种结转汇率差异 / commitment 一并结转 / 预算物化快照表 / 预算编制工作流 / 预算冻结多级 / 报表多年度维度实施 / 多公司合并预算 / 销售订单等其他场景承付。

## 目的

设计 ERP 的预算编制、预算控制、预算对比能力,实现管理会计的预算管理闭环。

## 设计范式

对照 iDempiere `Fact.java:78-84` 的四种 PostingType(Actual/Budget/Commitment/Reservation)。**预算作为 PostingType=BUDGET 的"影子凭证"**,与实际凭证(postingType=NORMAL)并行入账,复用同一套凭证引擎、GlBalance、试算平衡机制,零特例。赤龙 ERP 无预算模块(反向印证走 iDempiere 范式)。

预算执行数 = 同维度(科目×期间×成本中心×项目)的 Budget 凭证累计;预算余量 = 预算数 − 实际数 − 承付款,**无需新建预算余额表,复用 `ErpFinGlBalance` 的 `postingType` 维度即可**。

> **余量公式实现注记（P1-MA3-025）**：code（`ErpFinBudgetControlBiz`）以**显式三通道分离**（P1-MA2-084）落地三项式 `available = budgetBalance − actualBalance − commitmentBalance`：`budgetBalance`=BUDGET 通道、`actualBalance`=NORMAL/非 BUDGET 非 COMMITMENT 通道（**承付款不计入 actual 通道**，由独立 commitmentBalance 通道减去）、`commitmentBalance`=COMMITMENT 通道。数值上等价 `budget − (actual + commitment)`，但消除「actual 隐式含 commitment」误导。文档「实际凭证」= postingType=NORMAL（默认），非字典值 ACTUAL（字典无 ACTUAL，见 §与现有实体的关系）。

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
| approveStatus | dict `erp-fin/approve-status`(共用):UNSUBMITTED/SUBMITTED/APPROVED/REJECTED |
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
| actualAmount | 累计实际发生(postingType=NORMAL/非 BUDGET 非 COMMITMENT 凭证汇总,派生) |
| availableAmount | 预算余量(= budget − commitment − actual，三通道分离派生，查询时计算不落库，见 §设计范式 余量公式实现注记) |

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

3. **承付款生成**：采购订单 APPROVED 时生成 `postingType=COMMITMENT` 凭证；订单 CANCELLED 或被发票接收时红冲 COMMITMENT。budgetLine.commitmentAmount = Σ Commitment 凭证。销售订单承付（收入预算预留）语义对称（详见 §承付会计 §sales 承付扩展）。

4. **实际数派生**:实际凭证 postingType=NORMAL(现有所有凭证默认),actualAmount = Σ Normal 凭证在匹配维度的本位币金额。

5. **预算对比**:报表直接按 `(acctSchemaId, subjectId, periodId, costCenterId, projectId, postingType)` 分组 `ErpFinVoucherLine`,得到 Budget/Commitment/Actual 三列;无需独立预算余额表。

6. **期间控制**:预算凭证同样受 `ErpFinAccountingPeriodStatus.glStatus` 约束(已结账期间不可改预算)。

7. **多账套独立**:管理账有预算、税务账通常无预算——通过 `ErpFinBudgetScenario.acctSchemaId` 隔离。

8. **控制级别 HARD 下**:预算余量 < 0 时采购订单/付款单审核抛 `NopException`,单据保持 SUBMITTED 不前推。

## 与现有实体的关系

- **ErpFinVoucher/VoucherLine/GlBalance**:新增 `postingType` 列(dict `erp-fin/posting-type`：NORMAL/OPENING_BALANCE/ADJUSTMENT/CLOSING/REVERSAL/BUDGET/COMMITMENT，7 字符串值），NORMAL 为默认值（实际发生），向后兼容。预算/承付作为 BUDGET/COMMITMENT 影子凭证与 NORMAL 实际凭证并行入账。
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

---

## 多年度视图（A2）

> 同一预算编制可能跨多个年度（如 3 年滚动预算、跨年度项目预算）。设计目标：保留单年度 Scenario 语义（与既有 `fiscalYear` int 字段、`parentScenarioId` 调整链不冲突）+ 提供跨年度聚合视图。

### 决策（候选 A/B/C 权衡表）

| 候选 | 实现 | 优点 | 缺点 | 裁决 |
|------|------|------|------|------|
| A：扩展 `fiscalYear` 为 VARCHAR 范围 | "2026-2028" | 字段少 | 查询不便；语义模糊；破坏既有 int 字典 | ❌ reject |
| B：新增 `ErpFinBudgetGroup` 实体（多对一） | Scenario.budgetGroupId → Group | 规范化 | 增加实体复杂度；业务价值不匹配 | ❌ reject |
| **C：新增 `budgetGroupCode` 字段**（同组 Scenario 共享 code） | Scenario.budgetGroupCode VARCHAR(50) | 字段冗余简单；查询经 code 聚合；不破坏 `parentScenarioId` 链 | 仅靠 code 约束，无外键 | ✅ **选择** |

**裁决理由**：(i) 不破坏既有 `parentScenarioId` 调整版本链语义；(ii) 字段冗余 + 查询经 code 聚合足够；(iii) 多对一 Group 实体增加复杂度，业务价值不匹配。

### `budgetGroupCode` 字段语义

| 字段 | 类型 | 必填 | 含义 |
|------|------|------|------|
| `ErpFinBudgetScenario.budgetGroupCode` | VARCHAR(50) | 否 | 同组 Scenario 共享 code（如 "3Y-PLAN-2026"）。空表示独立预算。 |

查询多年度预算：`WHERE budgetGroupCode = ? ORDER BY fiscalYear`。

### 与 `parentScenarioId` 的边界

- `budgetGroupCode` = 横向多年度并列关系（年度 2026、2027、2028 同组）。
- `parentScenarioId` = 纵向单年度调整版本链（2026 年度原始 → 2026 调整 v1 → 2026 调整 v2）。
- 两者正交，不冲突；同一年度可有多个 Scenario（原始 + 多次调整），它们 `budgetGroupCode` 相同、`parentScenarioId` 链状。

---

## 滚动预算自动复制引擎（A2）

> 字典 `erp-fin/budget-scenario-type` 中 `ROLLING` 已定义但 A2 之前无实现。A2 落地 `@BizMutation rollForward(scenarioId, newFiscalYear, strategy)`。

### 3 策略（`erp-fin/budget-rollforward-strategy` 字典）

| 策略 | 算法 | 适用场景 |
|------|------|---------|
| **FIXED_PERCENTAGE**（默认） | 按源 Scenario 的所有 BudgetLine 金额 100% 复制（用户后续手工调整） | 延续型预算，业务量稳定 |
| ZERO_BASED | 仅复制 BudgetLine 结构（科目×期间×维度），金额清零 | 零基预算，下年度重新编制 |
| INCREMENTAL | 按配置的年增长率（如 inflation+5%）自动上调 BudgetLine 金额 | 增量预算，业务持续增长 |

### 复制算法（rollForward）

1. 加载源 Scenario（必须 APPROVED；其他状态抛 `ERP_FIN_BUDGET_SCENARIO_NOT_APPROVED`）。
2. 创建目标 Scenario：
   - `code` = 源 code + "-" + newFiscalYear（保证唯一）
   - `fiscalYear` = newFiscalYear
   - `scenarioType` = 源 scenarioType（保持 ROLLING/ANNUAL/ADJUSTMENT）
   - `parentScenarioId` = 源 Scenario id（**形成版本链，标识"由滚动复制而来"**）
   - `budgetGroupCode` = 源 budgetGroupCode（**继承同组**）
   - `controlLevel` / `currencyId` / `exchangeRate` / `acctSchemaId` / `orgId` = 源值
   - `docStatus` = DRAFT（待用户提交审批）
3. 复制 BudgetLine：按 periodId 重映射（`(newPeriod.year - oldPeriod.year) = (newFiscalYear - source.fiscalYear)` 偏移），找到目标年度同期期间。无对应期间时跳过该行（不抛错）。
4. 按 strategy 调整金额：
   - FIXED_PERCENTAGE：amount × 1.0
   - ZERO_BASED：amount = 0
   - INCREMENTAL：amount × (1 + 配置增长率，默认 0.05)
5. 写 `ErpFinBudgetRollforwardLog`：sourceScenarioId / targetScenarioId / strategy / newFiscalYear / sourceAmount / targetAmount。
6. 返回目标 Scenario。

### 配置项

| 键 | 默认值 | 含义 |
|----|--------|------|
| `erp-fin.budget-roll-forward-enabled` | false | 总开关（默认关，渐进启用） |
| `erp-fin.budget-rollforward-default-strategy` | FIXED_PERCENTAGE | 缺省策略 |
| `erp-fin.budget-rollforward-incremental-rate` | 0.05 | INCREMENTAL 增长率 |

### 浏览器层验证（A2）

`rollForward` 三策略经 Playwright E2E 全栈验证（`fin-budget-rollforward-carryforward.action.spec.ts`，config-gate `erp-fin.budget-roll-forward-enabled=true` 经 `playwright.config.ts` webServer JVM arg 启用）。**自包含 setup**：经 GraphQL `ErpMdSubject__save`（唯一 code EXPENSE/DEBIT 隔离）+ `ErpFinAccountingPeriod__save`（source/target 年度两 OPEN 期间，同 month 使 `remapPeriodId` 命中）+ `ErpFinBudgetScenario__save` 直置 APPROVED（绕 submit/approve 状态机避免 BUDGET 影子凭证污染）+ `ErpFinBudgetLine__save`。三策略行金额确定性派生断言：FIXED_PERCENTAGE 1000→1000 / ZERO_BASED 1000→0 / INCREMENTAL 1000→1050（×(1+0.05)）；目标方案 fiscalYear/parentScenarioId/docStatus=DRAFT 经 `__get` 独立反查；RollforwardLog 写入经 `countBudgetRollforwardLogs >= 1` 断言。**voucher code precision 50 约束**：rollForward 目标方案 code = source.code + "-" + newFiscalYear，source code 用短前缀规避（同 1407-1 latent defect 范式）。

---

## 结转规则引擎（A2）

> 年度终了时，上年度预算剩余（或已用）按规则结转至下年度，避免年度切换导致预算归零。`@BizMutation carryForward(scenarioId, targetScenarioId, rule)`。

### 4 规则（`erp-fin/budget-carry-forward-rule` 字典）

| 规则 | 算法 | 业务含义 |
|------|------|---------|
| **REMAINING_FULL**（默认） | 结转金额 = 预算余量（budget − actual − commitment），全部结转 | 剩余预算全部延续到下年 |
| REMAINING_RATIO | 结转金额 = 预算余量 × 配置比例（如 50%） | 部分延续，避免累积过多 |
| USED_FULL | 结转金额 = actualAmount（本年实际作为下年基线） | "本年实际即下年基线"范式 |
| NONE | 不结转（强制下年重新编制） | 零基预算，年度独立 |

### 结转算法（carryForward）

1. 加载源 Scenario + 目标 Scenario（两者必须同 orgId + 同 acctSchemaId + 同 currencyId，否则抛 `ERP_FIN_BUDGET_CARRY_FORWARD_RULE_INVALID`）。
2. 前置检查：源 Scenario 必须 APPROVED；目标 Scenario 必须 DRAFT（未审批，待结转后审批）。
3. 期间状态机协调：源 Scenario 所在年度的所有会计期间必须 CLOSED（年度已结账）。`ErpFinAccountingPeriodStatus.glStatus = CLOSED` 是结转的硬前置（见 `period-close.md §预算结转与期间状态机`）。
4. 计算结转金额（按规则）：
   - REMAINING_FULL：carriedAmount = budget − actual − commitment
   - REMAINING_RATIO：carriedAmount = (budget − actual − commitment) × ratio
   - USED_FULL：carriedAmount = actual
   - NONE：carriedAmount = 0（直接返回）
5. 在目标 Scenario 增补 BudgetLine（按源 Scenario 的 subjectId × costCenterId 维度合并；同维度累加金额）。
6. 生成结转凭证（postingType=BUDGET，billType=`BUDGET_SCENARIO_CARRY_FORWARD`，billCode=`CARRY-FORWARD-{sourceCode}-{targetCode}`），写入目标 Scenario 关联的 VoucherLine。
7. 源 Scenario 状态置为 `CLOSED`（新增预算状态字典值，见下表）；写 `closedAt` 时间戳。
8. 写 `ErpFinBudgetCarryForwardLog`：sourceScenarioId / targetScenarioId / rule / sourceRemaining / sourceUsed / carriedAmount。
9. 返回目标 Scenario（含结转后的 BudgetLine）。

### 预算状态字典扩展（`erp-fin/budget-status`）

| 值 | 含义 | A2 新增 |
|----|------|---------|
| DRAFT / SUBMITTED / APPROVED / REJECTED / CANCELLED | 既有 | 否 |
| **CLOSED** | 已结转（源 Scenario 终态，结转后不可再调整） | ✅ |

### 配置项

| 键 | 默认值 | 含义 |
|----|--------|------|
| `erp-fin.budget-carry-forward-enabled` | false | 总开关 |
| `erp-fin.budget-carry-forward-default-rule` | REMAINING_FULL | 缺省规则 |
| `erp-fin.budget-carry-forward-ratio` | 0.5 | REMAINING_RATIO 比例 |

### commitment 与结转

A2 默认 **commitment 不结转**（与 actualAmount 合并记录在源 Scenario 的余量计算中，结转后由源 Scenario 的 CLOSED 终态保留审计轨迹）。客户如有"未释放 commitment 一并结转至下年度"需求，归 successor（`commitment 一并结转` Deferred）。

### 浏览器层验证（A2）

`carryForward` 四规则经 Playwright E2E 全栈验证（`fin-budget-rollforward-carryforward.action.spec.ts`，config-gate `erp-fin.budget-carry-forward-enabled=true` 经 `playwright.config.ts` webServer JVM arg 启用）。**自包含 setup**：经 GraphQL `ErpMdSubject__save`（唯一 code EXPENSE/DEBIT，使 actual 聚合按 subjectId 隔离仅命中本 spec actual voucher）+ `ErpFinAccountingPeriod__save`（单一 OPEN 期间）+ `ErpFinBudgetScenario__save` 直置源 APPROVED / 目标 DRAFT + `ErpFinBudgetLine__save`（budget 1000）；REMAINING_FULL/RATIO/USED_FULL 三规则另经 `ErpFinVoucher__save` 直置 actual 凭证（postingType=NORMAL + POSTED + isReversed=false，`aggregateActualForLine` 排除 BUDGET/COMMITMENT）+ `ErpFinVoucherLine__save`（debitAmount=actual 400，DEBIT 科目 actual=debit−credit）。四规则结转金额确定性派生断言：REMAINING_FULL 1000−400=600 / REMAINING_RATIO (1000−400)×0.5=300 / USED_FULL=400 / NONE=0（carriedAmount=0 不增补行）；源方案 docStatus=CLOSED + closedAt 经 `__get` 独立反查；CarryForwardLog 写入经 `countBudgetCarryForwardLogs >= 1` 断言（NONE 规则同样写 Log）。**voucher code precision 50 约束**：结转凭证 code = "CARRY-FORWARD-"+sourceCode+"-"+targetCode+"-"+uuid8，source/target code 用单字符短前缀规避（同 1407-1 latent defect 范式）。

---

## 承付会计（A2）

> budget.md §业务规则3 既有定义："采购订单 APPROVED 时生成 `postingType=COMMITMENT` 凭证；订单 CANCELLED 或被发票接收时红冲 COMMITMENT"。A2 落地此能力的实际过账逻辑。

### 4 接入点（严格对齐 budget.md:78 业务规则 + 释放完整性声明）

| # | hook 点 | 时机 | 动作 | 事务边界 |
|---|---------|------|------|---------|
| 1 | **commit** | `ErpPurOrder.approve` 后置 | 生成 COMMITMENT 凭证（Dr 预算占用科目 / Cr 应付-承付） | **SYNC 同事务**（与既有 `IErpFinBudgetControlBiz.check()` 强一致） |
| 2 | **release-on-cancel** | `ErpPurOrder.reverseApprove` / `cancel`（订单取消路径） | 红冲原 COMMITMENT 凭证（金额取负） | 经 `IErpFinBudgetCommitmentBiz.release`（SYNC 同事务，与既有 reverseApprove 同事务） |
| 3 | **release-on-invoice-approve** | `ErpPurInvoice.approve`（**AP 发票过账 = 实际占用产生 = 释放承付**） | 红冲原 COMMITMENT 凭证 | SYNC 同事务 |
| 4 | **release-on-return** | `ErpPurReturn.approve` 后置（config-gated `erp-fin.commitment-release-on-return` 默认 OFF） | 经 `IErpFinBudgetCommitmentBiz.releaseIfPresent(PURCHASE_ORDER, poCode)` 红冲原 PO 承付凭证 | SYNC 同事务 |

#### 全额释放语义

接入点 #3 与 #4 均为**全额释放语义**：`reverseCommitment` 按 `billCode`（订单 code）反查原承付凭证并**全额红冲**（无 `amount` 入参，不按开票/退货金额比例部分释放）。

- **#3 多发票容错**：一张 PO 多次部分开票时，首张发票 `approve` 全额红冲承付（实际占用产生时即释放，避免 `actual + commitment` 双重占用预算）；后续发票 `approve` 经容错守卫 `hasUnreversedCommitment=false` 跳过（`ErpPurInvoiceProcessor.runCommitmentReleaseOnInvoiceApproveHook` catch `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED` 静默跳过）。按开票金额比例的部分释放归 successor（触发条件：多组织预算硬约束启用 + 部分开票为常态业务路径）。
- **#4 退货全额释放**：退货减少实际采购量 → 同步释放承付。`releaseIfPresent` 走全额红冲，故**部分退货亦全额红冲整张 PO 承付**——剩余未开票数量失去承付保护，可能允许超预算放行新订单（与 #3 同风险类）。按比例部分释放归 successor（与 #3 同 successor）。

#### 冲销恢复语义

**发票 `reverseApprove` / `cancel` 不恢复承付**（保守方向：保持已释放状态）。`ErpPurInvoiceProcessor.reverseApprove/cancel` 仅红冲 AP ACTUAL 凭证，不调 `commit()` 恢复承付；`ErpSalInvoiceProcessor` 同型。

- **残留风险**：冲销后 `actual↓` + `commitment=0`（已释放）→ 余量偏高，可能允许超预算放行新订单。
- **successor 触发条件**：多组织预算硬约束启用 + 冲销频率上升致超预算放行成为实际痛点时，实现 `commit()` 自动恢复（须跨实体反查原始 PO/SO + 处理部分冲销 + 跨期语义）。
- sales 侧承付冲销恢复对称问题随本声明一并记录；sales 侧 hook 接线与 purchase 同型，归 successor（触发条件：sales 承付控制启用）。

### reject release-receive-complete（ErpPurReceive 入库路径）

`ErpPurReceive.approve`（采购入库）是**库存移动**（inventory 物理入库），**不产生 AP ACTUAL 占用**。承付不应在入库时释放——业务规则 budget.md:78 "订单 CANCELLED 或被发票接收时红冲" 中的 "被发票接收" = `ErpPurInvoice.approve`（AP 发票过账产生 ACTUAL 应付），**不是** `ErpPurReceive.approve`。在入库时释放承付会导致 actual + commitment 双重占用预算（红冲 commitment 但 actual 尚未产生）。

### CommitmentAcctDocProvider

新增 Provider（`app.erp.fin.service.posting.CommitmentAcctDocProvider`）实现 `IErpFinAcctDocProvider`（与 BUDGET 同型）：
- **不走 Provider 路由**：`getSupportedBusinessTypes()` 返回**空集** → 注册中心不路由任何业务类型到此 Provider；`createFacts()` 返回**空列表**（仅为满足接口约定，不应被调用）。
- **承付凭证直接由 `CommitmentVoucherGenerator` 写入**（与 `BudgetVoucherGenerator` 同型），不经 `ErpFinAcctDocRegistry` 路由。
- Provider 存在仅为：文档化承付科目解析约定（`erp-fin.budget-commitment-subject-code` 配置）+ 满足接口约定（注册中心可发现）+ 为 successor（多维承付科目解析）保留接入点。
- 科目解析：复用既有 Provider 机制（不强制接入 A1 GL Mapping Rule；如需 budgetScenarioId 维度规则，归 successor）。

> 与 `posting.md §承付凭证 Provider` 描述一致（声明支持的业务类型集合为空，承付凭证由专用生成器直接写入）。

### `ErpFinVoucher.postingType = COMMITMENT`

承付凭证结构（Voucher + VoucherLine + VoucherBillR）与 BUDGET / ACTUAL 凭证一致，仅 `postingType` 字段值不同：
- 业财回链：`billType = PURCHASE_ORDER_COMMITMENT`，`billCode = 订单 code`，便于按订单反查全部承付凭证。
- `isReversed` 标记原凭证是否已被红冲（红冲凭证自身 `isReversed = true` 不参与余量聚合）。

### SPI 契约（`IErpFinBudgetCommitmentBiz`）

```java
// commit：创建 COMMITMENT 凭证；返回 commitmentVoucherId
Long commit(String sourceBillType, String sourceBillCode, Long subjectId, Long costCenterId,
            Long periodId, BigDecimal amount, IServiceContext context);

// release：红冲原 COMMITMENT 凭证；返回 reversalVoucherId（无原凭证可红冲时抛 ERR_BUDGET_COMMITMENT_ALREADY_RELEASED 守卫）
Long release(String sourceBillType, String sourceBillCode, IServiceContext context);
```

### 配置项

| 键 | 默认值 | 含义 |
|----|--------|------|
| `erp-fin.budget-commitment-enabled` | false | 总开关（默认关，保护既有 113 purchase 测试不触发承付凭证） |
| `erp-fin.budget-commitment-subject-code` | （必配） | 采购承付占用科目编码（启用采购承付时必填） |
| `erp-fin.budget-commitment-sales-subject-code` | （启用 sales 承付时必配） | 销售承付占用科目编码（与采购科目独立；sales 承付用收入面科目） |
| `erp-fin.commitment-release-on-return` | false | release-on-return 总开关（接入点 #4；默认关。启用时退货审核全额红冲原 PO 承付，须同时开启 `budget-commitment-enabled`） |

---

## sales 承付扩展

> 将承付凭证生成从采购订单扩展至销售订单，使**收入预算预留**可经承付凭证表达。承付传统是支出面概念（采购占用支出预算），
> 但在预算会计中收入面承付同样成立：销售订单 approve 时预留收入预算容量，销售发票过账（实际收入产生）时释放。

### Phase 1 Decision：采纳 sales 承付

| 候选 | 裁决 | 理由 |
|------|------|------|
| **(a) 采纳 sales 承付**（收入预算预留） | ✅ **选择** | (i) 与采购支出承诺对称（commit/release 范式完全复用）；(ii) iDempiere Fact.java COMMITMENT 支持对称；(iii) 基础设施已泛型（SPI 按 sourceBillType 派发，Dr/Cr 方向经 subject.direction 自动取）；(iv) config-gated 默认关 = opt-in 零回归 |
| (b) 否决 — sales 不适用承付 | ❌ reject | 收入面承付虽非主流，但 config-gate 已隔离风险；否决会使收入预算控制缺乏预留表达能力 |

**残留风险**：收入面承付非主流 ERP 范式，业务方接受度需验证。缓解：config-gate 默认关（`erp-fin.budget-commitment-enabled` + 独立科目配置），仅显式启用场景生效。

### Generator 泛化（billType 派发）

`CommitmentVoucherGenerator` 的 billType/sourceBillType 从采购硬编码泛化为按 sourceBillType 派发（无条件泛化，消除硬编码耦合 + 为 successor 预留）：

| sourceBillType | 派发 billType | 适用场景 |
|----------------|---------------|---------|
| `PURCHASE_ORDER` | `PURCHASE_ORDER_COMMITMENT` | 采购承付（既有） |
| `SALES_ORDER` | `SALES_ORDER_COMMITMENT` | 销售承付（收入预留） |

`resolveCommitmentBillType(sourceBillType)` 贯穿 commit/reverse/find 三路径，保证占用/释放 lookup 对称（不撞跨场景 billType）。未知 sourceBillType 回退采购 billType（向后兼容）。

### sales 接入点（镜像采购 3 接入点）

| # | hook 点 | 时机 | 动作 | 事务边界 |
|---|---------|------|------|---------|
| 1 | **commit** | `ErpSalOrder.approve` 后置 | 生成 COMMITMENT 凭证（billType=SALES_ORDER_COMMITMENT） | SYNC 同事务 |
| 2 | **release-on-cancel** | `ErpSalOrder.reverseApprove` / `cancel` | 红冲原 COMMITMENT 凭证 | SYNC 同事务 |
| 3 | **release-on-invoice-approve** | `ErpSalInvoice.approve`（AR 发票过账 = 实际收入产生 = 释放承付） | 红冲原 COMMITMENT 凭证 | SYNC 同事务 |

release-on-invoice 反查路径（镜像采购 invoiceLine→receiveLine→receive→order）：
`invoiceLine.deliveryLineId → deliveryLine.deliveryId → delivery.orderId → order.code`，
对每个唯一 order.code 调用 `IErpFinBudgetCommitmentBiz.release(SALES_ORDER, orderCode)`。

**科目独立配置**：sales 承付科目经 `erp-fin.budget-commitment-sales-subject-code` 独立配置（与采购 `budget-commitment-subject-code` 并列），使收入面承付科目（贷方/收入类方向）与支出面（借方/费用类方向）分离。

---

## 承付占用/释放 SPI（A2）

### 与既有 `IErpFinBudgetControlBiz.check()` 的协同

| SPI | 调用点 | 时机 | 数据载体 | 强弱一致 |
|-----|--------|------|---------|---------|
| `IErpFinBudgetControlBiz.check()` | purchase/sales 域审核事务内 | 实时余量校验（PASS/WARNED/BLOCKED） | `ErpFinBudgetControlLog`（审计日志） | SYNC 强一致 |
| `IErpFinBudgetCommitmentBiz.commit()` | `ErpPurOrder.approve` 后置 | 占用预算（生成 COMMITMENT 凭证） | `ErpFinVoucher` + `VoucherLine`（承付凭证） | SYNC 强一致 |
| `IErpFinBudgetCommitmentBiz.release()` | `ErpPurOrder.reverseApprove/cancel` + `ErpPurInvoice.approve` | 释放预算（红冲 COMMITMENT 凭证） | `ErpFinVoucher` + `VoucherLine`（红冲凭证） | SYNC 强一致 |

**协同关系**：check 是余量校验（不落库占用），commit 是实际占用（落库 COMMITMENT 凭证），release 是占用释放（红冲凭证）。三者事务边界均为 SYNC 同事务（与既有 `IErpFinBudgetControlBiz` 范式一致；release 不走事件总线异步，避免事务跨域复杂度）。

### 错误码（`ErpFinErrors`）

| 错误码 | 含义 |
|--------|------|
| `ERP_FIN_BUDGET_SCENARIO_NOT_APPROVED` | rollForward/carryForward 前置条件不满足（源 Scenario 必须 APPROVED） |
| `ERP_FIN_BUDGET_PERIOD_MISMATCH` | periodId 不在 fiscalYear 范围（结转/滚动期间映射失败） |
| `ERP_FIN_BUDGET_COMMITMENT_ALREADY_RELEASED` | 重复 release 守卫（原 COMMITMENT 凭证已红冲或不存在） |
| `ERP_FIN_BUDGET_CARRY_FORWARD_RULE_INVALID` | rule 字典值校验失败 / 跨 orgId + acctSchemaId + currencyId |

### 浏览器层验证

承付会计三接入点经 `tests/e2e/business-actions/fin-commitment-accounting.action.spec.ts` 全栈浏览器层覆盖（4 用例 + helper 扩展）：

- **helper 镜像 BUDGET 范式**：`findCommitmentVoucherIdByCode(page, billCode, reversal)`（`tests/e2e/orchestration/_helper.ts`）镜像既有 `findBudgetVoucherIdByCode:120`——按 `ErpFinVoucherBillR.billCode` 反查 + `postingType='COMMITMENT'` 过滤 + `reversalOfVoucherId` 区分正向（null）/红冲（非 null）。`assertVoucherLines` 共享原语复用。
- **两组 setup 隔离接入点 #3 干扰**：
  - (A) **order-only setup**：`__save` 建订单 + `submitForApproval` + `approve`，**不创建发票**——隔离发票审核释放，专测接入点 #1（commit）+ #2（release-on-cancel）。head `totalAmountWithTax` 必填（commit hook 读此字段，null/0 时 commit SPI early-return 不产凭证）。
  - (B) **full-chain `runP2pChain`/`runO2cChain`**：链路末端发票 approve 触发接入点 #3 release——专测 #3 + 采购 release hook 容错回归（Phase 1 Fix）。helper 扩展 `cleanupP2p`/`cleanupO2c` 追加 `cleanupVoucherByBillCode(poCode/soCode)` 清理承付凭证回链（postingType-agnostic 已覆盖 COMMITMENT）。

### release hook 容错对称性

采购 `ErpPurOrderProcessor.runCommitmentReleaseHook` 原**无 try-catch**（与销售 `ErpSalOrderProcessor.runCommitmentReleaseHook:359-370` 不对称），承付已被接入点 #3（发票审核释放）红冲后，订单反审核/作废再次调 `release()` 抛 `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED` 阻断业务流。Fix：补 try-catch（catch `NopException` + LOG.debug 静默跳过），镜像销售 hook 范式，恢复两域容错对称性。详见 `docs/bugs/2026-07-26-0410-pur-commitment-release-hook-tolerance-asymmetry.md`。

---

## 版本审计链（A2）

> A2 提供版本审计链的**字段基础**（`parentScenarioId` + `budgetGroupCode` + `closedAt` + RollforwardLog + CarryForwardLog）。前端多年度版本树形可视化归 frontend successor。

### 多年度版本树（数据语义）

```
budgetGroupCode = "3Y-PLAN-2026"
├─ 2026 Scenario (原始 ANNUAL)
│  ├─ parentScenarioId: null
│  └─ BudgetLines: ...
├─ 2026 Scenario (ADJUSTMENT v1, parentScenarioId → 2026 原始)
├─ 2027 Scenario (ROLLING, parentScenarioId → 2026 原始，经 rollForward)
└─ 2028 Scenario (ROLLING, parentScenarioId → 2027，经 rollForward)
```

查询某 budgetGroupCode 下全部 Scenario + parentScenarioId 即可构建版本树。

### RollforwardLog / CarryForwardLog 实体

| 实体 | 关键字段 | 索引 |
|------|---------|------|
| `ErpFinBudgetRollforwardLog` | scenarioId / sourceScenarioId / targetScenarioId / strategy / newFiscalYear / sourceAmount / targetAmount / rolledAt / rolledBy | scenarioId / sourceScenarioId |
| `ErpFinBudgetCarryForwardLog` | scenarioId / sourceScenarioId / targetScenarioId / rule / sourceRemaining / sourceUsed / carriedAmount / carriedAt / carriedBy | scenarioId / sourceScenarioId |

日志实体仅审计记录，不参与预算控制（控制始终从 `ErpFinVoucherLine` 聚合派生）。

---

## 反模式自检表扩展（A2）

> 在 budget.md 既有反模式基础上，A2 补充以下承付/结转/滚动特定反模式。

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 在 `ErpPurReceive.approve`（采购入库）释放承付 | **在 `ErpPurInvoice.approve`（AP 发票过账）释放承付**——入库是库存移动不产生 AP ACTUAL |
| release 走事件总线 ASYNC | **release SYNC 同事务**——与既有 `IErpFinBudgetControlBiz.check()` 强一致范式一致；ASYNC 引入事务跨域复杂度 |
| release 重复触发无守卫 | **`ERP_FIN_BUDGET_COMMITMENT_ALREADY_RELEASED` 守卫**——原凭证已红冲或不存在时抛错，避免重复占用预算 |
| 跨 orgId / acctSchemaId / currencyId 结转 | **同 orgId + 同 acctSchemaId + 同 currencyId 内结转**——跨公司结转归 A3 successor；跨币种汇率差异归 treasury successor |
| 结转时上年度 Scenario 仍可调整 | **结转后源 Scenario status=CLOSED**（终态不可再调整）——避免已结转数据被改 |
| commitment 一并结转 | **A2 默认不结转 commitment**（与 actualAmount 合并记录）——业务语义复杂，归 successor |
| 预算物化为快照表 | **保持派生查询**——`commitmentAmount/actualAmount/availableAmount` 从 VoucherLine 聚合不落库；物化 successor 触发条件：BudgetLine > 10 万行或查询 P95 > 500ms |
