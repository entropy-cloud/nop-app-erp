# 2026-07-21-1206-2-finance-budget-multi-year-carryforward A2 — Finance Budget Multi-Year / Carry-Forward / Commitment Accounting（预算多年度 / 结转 / 承付会计）

> Plan Status: active
> Last Reviewed: 2026-07-21
> Source: `docs/backlog/deepening-roadmap.md` §Milestone A §A2（line 52/83 — Budget Multi-Year / Carry-Forward，**可能需要新 budget 实体/字段 ORM 变更**）；`docs/analysis/erp-survey/2026-07-20-post-survey-strategic-gaps.md` §3.8（line 207-224 — Budget Management Design Enhancement，对照 OFBiz 7+ 实体套件）；`docs/design/finance/budget.md` line 3（已实现基线 + Deferred 显式列出承付款 / 滚动预算 / 调整版本链管理 = 本计划范围）；A1 plan `2026-07-21-0827-1` §Deferred But Adjudicated「A2 预算多年度 + A3 多公司运营深度」明确为本计划前身
> Related: `docs/plans/2026-07-21-0827-1-finance-gl-mapping-rule-tables.md`（A1 — GL Mapping Rule，预算 GL 维度规则可能复用 resolver；本计划不改 A1 接口契约）；`docs/design/finance/budget.md`（既有 owner doc，line 109 行——本计划 EXPAND）；`docs/design/finance/posting.md`（postingType=BUDGET 既有实现 + 承付款 postingType=COMMITMENT 字典已就绪 line 48-49）；`docs/design/finance/period-close.md`（期末结账，预算结转与期间状态机相关）；`docs/architecture/multi-company.md`（多公司基础，预算按 orgId + acctSchemaId 隔离既有）；`module-finance/model/app-erp-finance.orm.xml` line 1657-1830（ErpFinBudgetScenario/Line/ControlLog 三实体权威模型源）；`docs/analysis/erp-survey/2026-06-22-0000-idempiere.md`（iDempiere Fact.java:78-84 四 PostingType 对比 + Budget Fact 实现细节）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-21，对 finance ORM ErpFinBudgetScenario/Line/ControlLog + budget.md + posting.md + period-close.md + idempiere 对比报告扫描）：

### 已落地的预算基础设施（M1 P4 完成基线）

| 实体 | 表名 | 关键字段（与 A2 相关）| 已支持能力 |
|------|------|----------------------|------------|
| `ErpFinBudgetScenario` | `erp_fin_budget_scenario` | `fiscalYear`(单年度 int) + `scenarioType`(ANNUAL/ROLLING/ADJUSTMENT 字典) + `parentScenarioId`(版本链 self-FK) + `validFrom`/`validTo`(生效区间) + `controlLevel`(NONE/WARN/HARD) | 年度预算审批即过账 BUDGET 凭证 + 调整预算经 parentScenarioId 链 + 控制级别 HARD/WARN |
| `ErpFinBudgetLine` | `erp_fin_budget_line` | `periodId`(月/季/年粒度) + `subjectId` + `costCenterId` + `budgetAmountSource/Functional` + `commitmentAmount`(派生) + `actualAmount`(派生) + `availableAmount`(派生) | 按"科目×期间×维度"切分预算额度 + 派生字段查询时计算 |
| `ErpFinBudgetControlLog` | `erp_fin_budget_control_log` | `scenarioId` + `budgetLineId` + `sourceBillType`/`sourceBillCode` + `requestedAmount` + `committedAmount` + `actionResult`(PASS/WARNED/BLOCKED) | 审计超预算拦截/放行记录 |

**关键发现（A2 起点）**：
- `fiscalYear` 是**单年度 int 字段**（如 2026），跨年度预算需多个 Scenario 记录（年度预算 2026 + 年度预算 2027），无统一多年度视图。
- `scenarioType=ROLLING` 字典值已定义但**无滚动预算自动复制引擎**（budget.md line 3 Deferred 显式列出）。
- `parentScenarioId` 支持 1 层调整版本链，但**无多年度结转规则引擎**（年度终了时剩余预算自动/手动结转至下一年度）。
- `postingType=COMMITMENT` 字典值已就绪（line 48-49 标注 Deferred），但**无承付款实际过账逻辑**（采购订单 APPROVED 应生成 COMMITMENT 凭证，订单 CANCELLED 或被发票接收时红冲；budget.md line 78 业务规则定义但未实现）。
- `commitmentAmount`/`actualAmount`/`availableAmount` 三个派生字段在 budget.md 中描述为"查询时计算不落库"，但查询性能在大数据量下需评估（A2 评估是否物化为快照表）。

### 已落地的相关基础设施（A2 复用基础）

- **postingType 字典**（line 44-49）：ACTUAL/BUDGET 已落地 + COMMITMENT/RESERVATION 已字典定义但实际未触发过账。A2 落地 COMMITMENT 实际过账逻辑（采购订单 APPROVED → COMMITMENT 凭证；CANCELLED 或被发票接收 → 红冲）。
- **`IErpFinBudgetControlBiz` SPI**（既有）：purchase/sales 域审核动作事务内同步调用 `check(subjectId, costCenterId, periodId, amount, sourceBill)`；返回 BLOCKED → throw 阻断；WARN → 写日志放行；PASS → 静默。A2 扩展此 SPI 或新增承付占用/释放 SPI。
- **A1 GL Mapping Resolver**（plan 2026-07-21-0827-1，C2 之前完成）：规则表 `ErpFinGlMappingRule` + 优先级链 + Provider opt-in 集成契约。A2 的 BUDGET/COMMITMENT 凭证科目解析复用既有 Provider（`BudgetAcctDocProvider` 等），新增维度（budgetScenarioId）规则接入归 successor（A2 不强制接入）。
- **`ErpFinAccountingPeriodStatus.glStatus`**（既有）：已结账期间不可改预算（budget.md line 84 业务规则 6）。A2 多年度结转需尊重期间状态机（上年度 CLOSED 后才可结转至下年度）。
- **`ErpFinVoucher`/`VoucherLine`**（既有）：BUDGET 凭证已写入；COMMITMENT 凭证结构相同（仅 postingType 字段值差异）。
- **既有 codegen 链路**：finance ORM 是权威模型源；增量 codegen 经 `mvn clean install -DskipTests` 触发；新增字段必须遵循既有 `propId` 顺序；新增实体按既有 `ErpFinBudget*` 命名。

### 待深化差距（A2 范围）

| 差距 | 现状 | A2 目标 |
|------|------|---------|
| **多年度预算视图** | 单年度 Scenario 列表，跨年度汇总需多次查询 | 新增 `fiscalYearRange` 字段（或预算组实体）+ 多年度查询辅助 |
| **滚动预算自动复制引擎** | 字典值 ROLLING 已定义但无实现 | `@BizMutation rollForward(scenarioId, newFiscalYear)` 按规则（固定/增量/零基）复制预算至下一年度 |
| **预算结转规则引擎** | 无 | `@BizMutation carryForward(scenarioId, targetScenarioId, rule)` 上年度剩余/已用预算按规则结转下年度（含 commitment 结转） |
| **承付款（COMMITMENT）实际过账** | postingType=COMMITMENT 字典已定义但无 Provider 接入 | 采购订单 APPROVED → COMMITMENT 凭证 + 订单 CANCELLED 或被采购发票接收（ErpPurInvoice.approve）→ 红冲；`PurOrderAcctDocProvider` 扩展或新增 `CommitmentAcctDocProvider` |
| **承付款占用/释放 SPI** | 仅有 `IErpFinBudgetControlBiz.check()` 实际占用校验 | 新增 `commit(sourceBill, amount)` + `release(sourceBill)` SPI；purchase order commit + order-cancel release + purchase-invoice-approve release **3 个调用点**接入（严格对齐 budget.md line 78 业务规则）|
| **预算版本审计链** | `parentScenarioId` 1 层 | 多年度版本树可视化（前端树形展示） |
| **预算对比报表多年度维度** | 既有单年度对比 | 增加多年度趋势对比（yoy 增长率）|

### 关键风险/缺口

- **承付款红冲时机一致性**：采购订单 CANCELLED → 红冲 COMMITMENT（释放承付）；被采购发票接收（`ErpPurInvoice.approve`，即 AP 发票过账时）→ 红冲 COMMITMENT（实际占用转为 ACTUAL）。两个时机需在 PurOrder 与 PurInvoice 状态机钩子接入；如时机不一致，承付金额会与实际金额重复占用预算。**缓解**：接入用 nop 事件总线 `OrderCancelledEvent` / `InvoiceApprovedEvent`，由 finance 域监听者触发红冲（与既有过账引擎事件路径一致）；release 路径明确**仅 3 hook 点**（commit + release-on-cancel + release-on-invoice-approve），与 budget.md line 78 业务规则 "订单 CANCELLED 或被发票接收时红冲" 严格对齐——**注意**：被发票接收 = `ErpPurInvoice.approve`（AP 发票过账，产生 ACTUAL），**不是** `ErpPurReceive.approve`（采购入库，库存移动，不产生 AP 发票 ACTUAL 占用）。
- **结转规则的业务语义多样性**：年度结转规则在不同企业差异巨大——剩余全部结转 / 剩余按比例结转 / 已用部分结转 / 不结转（强制重新预算）；**Decision 候选**：本计划落地 4 种内置规则 + 用户自定义扩展点（SPI），不内置全部业务场景。
- **多公司隔离与结转**：跨公司结转（子公司预算结转至母公司）属 A3 多公司 successor；本计划仅在同 orgId + 同 acctSchemaId 内结转。
- **物化 vs 派生查询性能**：`commitmentAmount`/`actualAmount`/`availableAmount` 三个派生字段在 budget.md 中"查询时计算不落库"，但承付款落地后查询频次会大幅上升（采购订单审核同步查预算余量）；**Decision 候选**：本计划保持派生（避免物化一致性问题），如生产数据量 > 阈值则物化 successor。
- **A1 集成边界**：A2 BUDGET/COMMITMENT 凭证的科目解析复用既有 Provider（不强制接入 A1 GL Mapping Rule）；如业务需要 budgetScenarioId 维度的规则，归 successor。
- **既有测试数据兼容性**：finance service 既有 218 测试（含 A1 新增 `TestErpFinGlMappingResolver` 8 场景 + `TestErpPurInvoicePosting` 扩展）；新字段全部 nullable + 默认 null + 新增承付 Provider 通过 config-gated 默认关闭（不破坏既有 PurOrder 测试）。

## Goals

1. **EXPAND owner doc**：`docs/design/finance/budget.md`（既有 109 行 → ~250 行扩展，新增「多年度视图」「滚动预算自动复制」「结转规则引擎」「承付会计」「承付占用/释放 SPI」「版本审计链」6 段）。
2. **`ErpFinBudgetScenario` ORM 扩展**：新增字段（如 `parentFiscalYear`/`carryForwardRule` ext:dict/`rollForwardStrategy` ext:dict）；新增 `ErpFinBudgetRollforwardLog`（滚动复制日志）+ `ErpFinBudgetCarryForwardLog`（结转日志）实体（Decision 候选 A）。
3. **滚动预算自动复制引擎**：`@BizMutation rollForward(scenarioId, newFiscalYear, strategy)`（strategy ∈ FIXED_PERCENTAGE / ZERO_BASED / INCREMENTAL），按规则生成下年度新 Scenario + BudgetLine。
4. **预算结转规则引擎**：`@BizMutation carryForward(scenarioId, targetScenarioId, rule)`（rule ∈ REMAINING_FULL / REMAINING_RATIO / USED_FULL / NONE），生成结转凭证 + 结转日志。
5. **承付款（COMMITMENT）实际过账**：扩展或新增 `CommitmentAcctDocProvider`，在 PurOrder.approve 时生成 COMMITMENT 凭证 + 在 OrderCancelledEvent / ReceiveApprovedEvent 监听者触发红冲。
5. **承付占用/释放 SPI**：`IErpFinBudgetCommitmentBiz.commit(sourceBill, amount, dimensions)` + `release(sourceBill)`；**3 个接入点**（严格对齐 budget.md line 78）：purchase-order commit / order-cancel release / purchase-invoice-approve release（与既有 `IErpFinBudgetControlBiz.check()` 协同）。
7. **BizModel + 视图层接入**：codegen 增量生成新实体 + 字段；ErpFinBudgetScenario 表单增「多年度」+「结转规则」分组（F3 范式扩展）；新增滚动/结转向导（或简单按钮 + confirm dialog）。
8. **测试基线**：finance service 既有测试不回归；新增承付生命周期测试（commit → release 红冲）+ 结转规则测试（4 种 rule 各 1 测试）+ 滚动复制测试（3 种 strategy 各 1 测试）。
9. **roadmap 同步**：`deepening-roadmap.md` §A2 状态 `todo → done` + §8.4 落地证据段落。

## Non-Goals

- **A3 多公司运营深度**—— 跨公司预算结转 / 合并预算归 A3 successor；本计划仅在同 orgId + 同 acctSchemaId 内。
- **A1 GL Mapping Rule 接入 BUDGET/COMMITMENT**—— A2 复用既有 Provider 科目解析；budgetScenarioId 维度的规则接入归 successor（触发：业务多维预算覆盖需求 + A1 resolver 稳定 ≥ 1 个月后）。
- **承付款业务场景全集**—— 仅落地采购订单（PurOrder）的承付生命周期（commit / release）；销售订单/付款单等其他业务场景的承付归 successor（触发：具体业务需求 + owner doc 授权）。
- **预算物化快照表**—— `commitmentAmount`/`actualAmount`/`availableAmount` 保持派生查询；物化 successor 触发条件：生产数据量 > 10 万行 BudgetLine 或查询 P95 > 500ms。
- **预算审批工作流编排**—— 审批流程复用既有 useApproval；本计划不改审批契约。
- **预算对比报表多年度维度实施**—— 字段基础由本计划提供，报表实施归 report successor。
- **预算绩效分析 / 预算 vs 实际差异分析**—— 复杂管理会计功能，属 BI/analytics successor。
- **跨币种预算结转汇率差异处理**—— 多币种预算结转的汇兑损益属 finance/treasury successor；本计划仅在同 currencyId 内结转。
- **预算冻结/解冻多级控制**—— 既有 controlLevel=HARD/WARN/NONE 三级足够；SOFT_FREEZE/HARD_FREEZE 多级 freeze 归 successor（触发：业务客户具体冻结管理需求）。
- **预算编制工作流（自上而下/自下而上/迭代）**—— 复杂编制流程属 budget workflow successor。

## Task Route

- Type: `app-layer design change`（EXPAND owner doc + 多年度视图 + 结转规则 + 承付会计语义）+ `implementation-only change`（ORM 字段扩展 + 新实体 + Provider + SPI + view.xml + 测试）
- Owner Docs:
  - `docs/backlog/deepening-roadmap.md` §A2（line 52/83）
  - `docs/design/finance/budget.md`（既有 EXPAND — 本计划 Phase 0 落地扩展段）
  - `docs/design/finance/posting.md`（§postingType + 承付 Provider 接入段回链）
  - `docs/design/finance/period-close.md`（§结账与预算结转时段回链）
  - `../nop-entropy/docs-for-ai/02-core-guides/service-layer.md`（BizModel 范式 + 跨实体 IBiz）
  - `../nop-entropy/docs-for-ai/03-runbooks/add-entity-or-fields.md`（增量字段添加 runbook）
  - `../nop-entropy/docs-for-ai/02-core-guides/processor-extension.md`（Processor 扩展 — 承付过账 Provider）
- Skill Selection Basis: 加载 `nop-backend-dev`（ORM 实体扩展 + BizModel + IBiz + 跨实体编排 + Processor + 承付 Provider + SPI）；Phase 3 view.xml 定制（form 分组 + rollForward/carryForward 按钮 + confirm dialog）也加载 `nop-frontend-dev`（与 A1 plan 范式一致）；不加载 `nop-testing`（既有 finance service 测试范式直接复用）。最终：`nop-backend-dev`（Phase 0-2）+ `nop-frontend-dev`（Phase 3）。

## Infrastructure And Config Prereqs

- 本地运行：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- **关键 config**（config-gated 默认关闭，渐进启用）：
  - `erp-fin.budget-commitment-enabled`（默认 `false`）— 承付过账启用开关；启用后 PurOrder.approve 触发 COMMITMENT 凭证
  - `erp-fin.budget-carry-forward-enabled`（默认 `false`）— 结转规则启用开关
  - `erp-fin.budget-roll-forward-enabled`（默认 `false`）— 滚动复制启用开关
- webServer JVM args（E2E 测试时）：追加上述 3 项 = true 启用对应能力测试

## Execution Plan

### Phase 0 — Explore + Owner Doc 扩展 + 关键 Decision

Status: planned
Targets: `docs/design/finance/budget.md`（既有 EXPAND）+ plan 内 Decision 记录
Skill: `nop-backend-dev`

- Item Types: `Explore | Decision | Add`
- Prereqs: deepening-roadmap.md A2 todo + A1 done + budget.md 既有 Deferred 段

- [ ] `Explore` (a)：多年度预算视图的字段设计候选评估。
  - 候选 A：扩展 `ErpFinBudgetScenario.fiscalYear` 为 VARCHAR 范围字段（如 "2026-2028"），简单但查询不便。
  - 候选 B：新增 `ErpFinBudgetGroup` 实体（多年度预算组，Scenario 多对一归属 Group），查询经 Group 聚合。
  - 候选 C：保持单年度 Scenario，新增 `budgetGroupCode` 字段（同组 Scenario 共享 code），无需新实体。
  - 核实范围：既有 OFBiz/iDempiere 多年度预算实现；性能（Group 实体 vs 字段冗余）；与既有 `parentScenarioId` 的关系（避免双重版本链）。
  - 输出：3 方案权衡表 + 裁决，入 budget.md §多年度视图。
  - Skill: `nop-backend-dev`
- [ ] `Explore` (b)：滚动预算自动复制引擎策略集设计。
  - 候选策略：(1) FIXED_PERCENTAGE（固定比例，如 100% 复制 + 用户后续调整）；(2) ZERO_BASED（零基，仅复制结构不复制金额）；(3) INCREMENTAL（增量，按业务增长率自动上调，如 inflation+5%）。
  - 核实范围：既有 iDempiere / ERP5 / SAP 滚动预算实现；与 `parentScenarioId` 链的关系（复制生成新 Scenario + parent 指向源 Scenario 还是独立）；多年度复制时的 periodId 重映射（2026-01 → 2027-01）。
  - 输出：3 策略详细算法（含 periodId 重映射逻辑）+ 默认策略裁决。
  - Skill: `nop-backend-dev`
- [ ] `Explore` (c)：预算结转规则集设计与凭证落地方式。
  - 候选规则：(1) REMAINING_FULL（剩余全部结转）；(2) REMAINING_RATIO（剩余按比例结转，如 50%）；(3) USED_FULL（已用部分结转，等同"年度实际作为下年度基线"）；(4) NONE（不结转）。
  - 核实范围：结转是否生成新 BUDGET 凭证（写入下年度 Scenario）；结转时上年度 Scenario 是否锁死（不可再调整）；结转与承付款的关系（未释放的 commitment 是否一并结转）。
  - 输出：4 规则详细算法 + 默认规则裁决 + 凭证落地方式。
  - Skill: `nop-backend-dev`
- [ ] `Explore` (d)：承付款过账接入点（hook point）评估。
  - 核实范围：`PurOrderBizModel.approve` 既有实现 + 事件总线 `OrderApprovedEvent`（如有）；CANCELLED 路径（既有 cancel mutation + 事件）；Receive APPROVED 路径（既有 `IErpPurReceiveBiz.approve` + `ReceiveApprovedEvent`）；与既有 `IErpFinBudgetControlBiz.check()` 的协同（commit 与 check 的事务边界）。
  - 输出：候选 hook 点对比评估 —— 3 接入点（commit / release-on-cancel / release-on-invoice-approve）严格对齐 budget.md:78 业务规则（"订单 CANCELLED 或被发票接收时红冲"）；4 接入点（含 release-receive-complete 即 ErpPurReceive 入库路径）作为对比项评估并 reject（ErpPurReceive 是采购入库库存移动，不产生 AP ACTUAL 占用，承付不应在入库时释放）。最终裁决：**3 接入点落地**。事务边界裁决：commit 与 check 同事务（强一致）；release 经事件总线异步（最终一致，避免事务跨域）。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：基于 Explore (a)~(d)，确定 A2 实现方式。
  - **多年度视图**（裁决依据 Explore a）：选 **候选 C（保持单年度 Scenario + 新增 `budgetGroupCode` 字段）**。理由：(i) 不破坏既有 `parentScenarioId` 调整版本链语义；(ii) 字段冗余简单 + 查询经 code 聚合足够；(iii) 多对一 Group 实体增加复杂度，业务价值不匹配。
  - **滚动预算策略**（裁决依据 Explore b）：3 策略全部落地（FIXED_PERCENTAGE / ZERO_BASED / INCREMENTAL），默认 FIXED_PERCENTAGE；复制生成新 Scenario + `parentScenarioId` 指向源 Scenario；periodId 重映射按 (newPeriod.year - oldPeriod.year) 偏移。
  - **结转规则**（裁决依据 Explore c）：4 规则全部落地，默认 REMAINING_FULL；结转生成新 BUDGET 凭证写入下年度 Scenario；上年度 Scenario 结转后 status=CLOSED（新增预算状态字典值）；commitment 不结转（与 actualAmount 合并记录），归 successor。
  - **承付过账接入点**（裁决依据 Explore d）：**3 接入点全部落地**（commit / release-on-cancel / release-on-invoice-approve）严格对齐 budget.md:78；**reject release-receive-complete（ErpPurReceive 入库路径）** —— ErpPurReceive 是采购入库（库存移动），不产生 AP ACTUAL 占用，承付不应在入库时释放（与 budget.md:78 "CANCELLED 或被发票接收" 严格对齐，**被发票接收** = `ErpPurInvoice.approve` AP 发票过账）；commit/release 经 `IErpFinBudgetCommitmentBiz` 新 SPI；事务边界：commit 与 check 同事务（强一致）；release 经事件总线异步（最终一致，避免事务跨域）。
  - **选择依据**：多年度视图最小侵入；滚动/结转规则全覆盖（业务语义多样，4-3 组合足够）；承付接入事务边界强一致 + 异步解耦平衡。
  - Skill: none
- [ ] `Add`：`docs/design/finance/budget.md` EXPAND（既有 109 行 → ~250 行）
  - 新增 6 段：§多年度视图（候选 C 裁决 + budgetGroupCode 字段语义）/ §滚动预算自动复制引擎（3 策略算法 + periodId 重映射 + parentScenarioId 链关系）/ §结转规则引擎（4 规则算法 + 凭证落地 + status=CLOSED 状态扩展）/ §承付会计（COMMITMENT Provider + **3 接入点** + 事务边界 + SPI 契约 + reject release-receive-complete 理由）/ §承付占用/释放 SPI（commit/release 签名 + 与 check 协同）/ §版本审计链（多年度树形可视化说明，前端树归 successor）/ §反模式自检表扩展（包括"承付红冲时机不一致导致重复占用"）
  - 既有 Deferred 段（line 3）标记 "已落地"（除跨币种结转等 successor）
  - Skill: none

Exit Criteria:

- [ ] 4 个 Explore 结论已记录；对应 Decision 已落地
- [ ] budget.md EXPAND 落地（新增 6 段 + 既有 Deferred 段更新）
- [ ] 多年度视图 + 滚动策略 + 结转规则 + 承付接入 4 项关键 Decision 在 budget.md 明确

### Phase 1 — ORM 扩展 + 字典 + codegen

Status: planned
Targets: `module-finance/model/app-erp-finance.orm.xml`（既有追加 + 新实体）+ 增量 codegen
Skill: `nop-backend-dev`

- Item Types: `Add-heavy | Decision`
- Prereqs: Phase 0 完成 + ORM 变更授权（deepening-roadmap.md §8 已授权 A2）

- [ ] `Add`：`ErpFinBudgetScenario` ORM 模型扩展字段（propId 26-29，既有 ErpFinBudgetScenario max propId=25）
  - 路径：`module-finance/model/app-erp-finance.orm.xml` line 1657-1715 既有 `<columns>` 段追加
  - 字段（4 字段 + 显式 propId）：`budgetGroupCode` propId=26 (VARCHAR 50) / `carryForwardRule` propId=27 (VARCHAR 20, ext:dict=erp-fin/budget-carry-forward-rule) / `rollForwardStrategy` propId=28 (VARCHAR 20, ext:dict=erp-fin/budget-rollforward-strategy) / `closedAt` propId=29 (TIMESTAMP,结转时间戳)
  - 全部 `mandatory="false"` + 默认 null + `i18n-en:displayName`
  - **propId 值在 Phase 1 实施时核实确认**（既有 ErpFinBudgetScenario 实际 max propId，按实施时实测为准，可能因后续 plan 落地有偏差）
  - Skill: `nop-backend-dev`
- [ ] `Add`：新字典 `erp-fin/budget-carry-forward-rule`（4 键：REMAINING_FULL/REMAINING_RATIO/USED_FULL/NONE）+ `erp-fin/budget-rollforward-strategy`（3 键：FIXED_PERCENTAGE/ZERO_BASED/INCREMENTAL）+ `erp-fin/budget-status` 扩展 CLOSED 值（既有 DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED + 新增 CLOSED）
  - 路径：finance ORM `<dict>` 段或既有字典扩展
  - Skill: `nop-backend-dev`
- [ ] `Add`：新实体 `ErpFinBudgetRollforwardLog`（滚动复制日志）
  - 路径：finance ORM 紧随 `ErpFinBudgetControlLog` 段
  - 字段：id/scenarioId/sourceScenarioId/targetScenarioId/strategy/newFiscalYear/sourceAmount/targetAmount/rolledAt/rolledBy/remark + 标准审计字段；idx=scenarioId/sourceScenarioId
  - Skill: `nop-backend-dev`
- [ ] `Add`：新实体 `ErpFinBudgetCarryForwardLog`（结转日志）
  - 路径：紧随 RollforwardLog
  - 字段：id/scenarioId/sourceScenarioId/targetScenarioId/rule/sourceRemaining/sourceUsed/carriedAmount/carriedAt/carriedBy/remark + 标准审计字段；idx=scenarioId/sourceScenarioId
  - Skill: `nop-backend-dev`
- [ ] `Add`：增量 codegen 触发
  - 命令：`mvn clean install -DskipTests`（触发 gen-orm.xgen 增量链）
  - 预期生成：ErpFinBudgetScenario 字段扩展（Entity + xmeta + dict）+ 2 新实体全套（Entity + DAO + BizModel + IBiz + xmeta + view.xml + page.yaml 骨架）
  - Skill: `nop-backend-dev`
- [ ] `Decision`：codegen 产物核实 + 字段名映射核对
  - 核实范围：生成的 ErpFinBudgetScenario.java 字段名 + 类型与 ORM 一致；2 新日志实体的字段集与设计一致
  - Skill: none

Exit Criteria:

- [ ] ORM 模型变更经 `xmllint --noout` well-formed 校验通过
- [ ] `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（增量 codegen 无错误）
- [ ] 2 新日志实体的 Entity + DAO + BizModel + IBiz + xmeta + view.xml 生成产物落地

### Phase 2 — 滚动复制 + 结转引擎 + 承付 Provider + SPI + 测试

Status: planned
Targets: `ErpFinBudgetScenarioBizModel` delta 扩展（rollForward/carryForward mutations）+ `CommitmentAcctDocProvider` + `IErpFinBudgetCommitmentBiz` SPI + 测试
Skill: `nop-backend-dev`

- Item Types: `Add-heavy | Proof`
- Prereqs: Phase 1 完成 + codegen 产物已生成

- [ ] `Add`：`IErpFinBudgetCommitmentBiz` SPI（dao 模块）
  - 路径：`module-finance/erp-fin-dao/src/main/java/app/erp/fin/biz/IErpFinBudgetCommitmentBiz.java`（**NEW**）
  - 方法签名：`Long commit(String sourceBillType, String sourceBillCode, Long subjectId, Long costCenterId, Long periodId, BigDecimal amount, IServiceContext context)` + `Long release(String sourceBillType, String sourceBillCode, IServiceContext context)`；返回 commitmentVoucherId（commit）或 reversalVoucherId（release）
  - Skill: `nop-backend-dev`
- [ ] `Add`：`ErpFinBudgetCommitmentBizModel` 实现（service 模块）
  - 路径：`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/budget/ErpFinBudgetCommitmentBizModel.java`（**NEW**）
  - 实现：`commit` 创建 COMMITMENT 凭证（经既有 `IErpFinPostingService.persistVoucher`）；`release` 经红冲路径（同既有 reverse 机制）；config-gated（`erp-fin.budget-commitment-enabled` 默认 false）
  - Skill: `nop-backend-dev`
- [ ] `Add`：`CommitmentAcctDocProvider`（service 模块）
  - 路径：`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/CommitmentAcctDocProvider.java`（**NEW**，实现 `IErpFinAcctDocProvider`）
  - 实现：处理 `PostingEvent.PURCHASE_ORDER_COMMITMENT`（如既有 PostingEvent 无此值则新增枚举）+ `PURCHASE_ORDER_COMMITMENT_REVERSAL`；返回 `List<VoucherFact>` 含 subjectCode（来自既有科目映射）+ amount + postingType=COMMITMENT
  - Skill: `nop-backend-dev`
- [ ] `Add`：3 接入点 hook（purchase 域 delta）—— **严格对齐 budget.md line 78 业务规则**（commit / release-on-cancel / release-on-invoice-approve）
  - 路径：`module-purchase/erp-pur-service/.../entity/ErpPurOrderBizModel.java` delta（approve mutation 触发 commit；cancel/`reverseApprove` processor hook 触发 release-on-cancel）
  - 路径：`module-purchase/erp-pur-service/.../entity/ErpPurInvoiceBizModel.java` delta（approve mutation 触发 release-on-invoice-approve —— **AP 发票过账 = 实际占用产生 = 释放承付**；**不是** `ErpPurReceive` 采购入库）
  - **路径选择 Decision（Phase 0 Explore (d) 裁决细化）**：release 触发优先走 **Processor `reverseApprove` 钩子**（与 A1 `ErpFinPostingProcessor` 消费 resolver 同 Processor 模式，post-approval reversal 路径），而非 Facade 层 `cancel()` mutation（cancel 通常为 pre-approval 取消，无 commit 释放需求）；具体路径在 Phase 2 实施时按 `ErpPurOrderBizModel` 实际 Processor 结构核实
  - 实现：approve 时调 `IErpFinBudgetCommitmentBiz.commit(...)`；release（cancel/invoice-approve）时调 `release(...)`；config-gated（默认 false 时不调用）
  - Skill: `nop-backend-dev`
- [ ] `Add`：`ErpFinBudgetScenarioBizModel` delta 扩展 rollForward/carryForward
  - 路径：`module-finance/erp-fin-service/.../entity/ErpFinBudgetScenarioBizModel.java` delta（既有 codegen 产物后扩展）
  - `rollForward(scenarioId, newFiscalYear, strategy)`：按 strategy 生成新 Scenario（budgetGroupCode 同源）+ 重映射 BudgetLine periodId；写 RollforwardLog
  - `carryForward(scenarioId, targetScenarioId, rule)`：按 rule 计算结转金额 + 生成新 BUDGET 凭证写入 targetScenario + 源 Scenario status=CLOSED；写 CarryForwardLog
  - config-gated（`erp-fin.budget-roll-forward-enabled` / `erp-fin.budget-carry-forward-enabled`）
  - Skill: `nop-backend-dev`
- [ ] `Add`：错误码
  - 路径：`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/ErpFinErrors.java`（既有追加）
  - 错误码：`ERP_FIN_BUDGET_SCENARIO_NOT_APPROVED`（rollForward/carryForward 前置条件不满足）/ `ERP_FIN_BUDGET_PERIOD_MISMATCH`（periodId 不在 fiscalYear 范围）/ `ERP_FIN_BUDGET_COMMITMENT_ALREADY_RELEASED`（重复 release 守卫）/ `ERP_FIN_BUDGET_CARRY_FORWARD_RULE_INVALID`（rule 字典值校验）
  - Skill: `nop-backend-dev`
- [ ] `Proof`：单元测试
  - `TestErpFinBudgetRollForward`（**NEW**，3 策略各 1 测试）：FIXED_PERCENTAGE 100% 复制 + ZERO_BASED 仅结构 + INCREMENTAL 按 5% 上调
  - `TestErpFinBudgetCarryForward`（**NEW**，4 规则各 1 测试）：REMAINING_FULL + REMAINING_RATIO 50% + USED_FULL + NONE
  - `TestErpFinBudgetCommitment`（**NEW**，4 测试）：commit 创建 COMMITMENT 凭证 + release-on-cancel 红冲 + release-on-invoice-approve 红冲 + 重复 release 守卫
  - Skill: `nop-backend-dev`
- [ ] `Proof`：集成测试 `TestErpPurOrderCommitment`（**NEW**）—— 端到端验证 hook 装配正确
  - 路径：`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurOrderCommitment.java`
  - 测试场景：(1) `erp-fin.budget-commitment-enabled=true` 时 `ErpPurOrderBizModel.approve(...)` 触发 COMMITMENT 凭证创建 + `ErpFinVoucherBillR` billType=PURCHASE_ORDER_COMMITMENT 回链；(2) `erp-fin.budget-commitment-enabled=false` 时（默认）approve 不触发 COMMITMENT 凭证（回归安全：保护既有 113 purchase 测试）；(3) `ErpPurInvoiceBizModel.approve(...)` 在 invoice-approve 路径触发 release 红冲（如 commit 凭证存在）
  - 与 A1 `TestErpPurInvoicePosting.testGlMappingRuleOverrideChangesSubjectCode` 同范式（端到端集成测试覆盖 hook 装配，弥补单元测试不能发现的 hook 装配错误）
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] `IErpFinBudgetCommitmentBiz` SPI + 实现 + `CommitmentAcctDocProvider` 落地
- [ ] 3 接入点 hook 落地（order-approve commit / order-cancel release / invoice-approve release），全部 config-gated 默认 false
- [ ] `ErpFinBudgetScenarioBizModel.rollForward`/`carryForward` 实现 + 错误码落地
- [ ] 3 单元测试类（11 测试场景）+ 1 集成测试（`TestErpPurOrderCommitment` 3 场景）全绿

### Phase 3 — view.xml 定制 + owner doc 回链 + roadmap 同步

Status: planned
Targets: view.xml 定制 + 既有 owner doc 回链 + `deepening-roadmap.md` §A2 done + §8.4 落地证据
Skill: `nop-frontend-dev`（view.xml 定制 + 按钮 + confirm dialog）+ `none`（纯文档项）

- Item Types: `Add`
- Prereqs: Phase 2 完成 + 全量验证通过

- [ ] `Add`：`ErpFinBudgetScenario.view.xml` form 增「多年度」+「结转规则」分组
  - 路径：`module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/ErpFinBudgetScenario/ErpFinBudgetScenario.view.xml`
  - 范式：F3 layout 分组 + 4 新字段排列；新增 rollForward/carryForward 按钮（带 confirm dialog + 參數输入）
  - Skill: `nop-frontend-dev`
- [ ] `Add`：菜单 + action-auth 注册
  - 路径：`module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/auth/erp-fin.action-auth.xml`（既有追加）
  - 2 新日志实体（RollforwardLog/CarryForwardLog）注册到 `fin-budget`（预算管理）分组
  - Skill: `nop-frontend-dev`
- [ ] `Add`：`docs/design/finance/posting.md` §postingType 增「承付（COMMITMENT）实际过账」段
  - 内容：commit/release **3 接入点**（commit / release-on-cancel / release-on-invoice-approve，严格对齐 budget.md:78）+ reject release-receive-complete 理由（ErpPurReceive 入库不产生 AP ACTUAL）+ 事务边界 + config-gated 启用 + 与既有 `IErpFinBudgetControlBiz.check()` 协同
  - Skill: none
- [ ] `Add`：`docs/design/finance/period-close.md` 增「预算结转与期间状态机」段
  - 内容：结转前置条件（上年度 CLOSED）+ 结转后 Scenario.status=CLOSED + 跨年度期间状态机协调
  - Skill: none
- [ ] `Add`：`docs/backlog/deepening-roadmap.md` §A2 done + §8.4 落地证据
  - 路径：line 52 状态 `todo → done` + §8.4 新增段（plan + owner doc + ORM 变更 + codegen 产物 + 测试基线 + Deferred successor）
  - Skill: none

Exit Criteria:

- [ ] ErpFinBudgetScenario.view.xml form 新增 2 分组 + 2 mutation 按钮
- [ ] 2 处既有 owner doc 回链段落落地（posting.md / period-close.md）
- [ ] roadmap §A2 状态 done + §8.4 落地证据登记

## Draft Review Record

- Independent draft review iteration 1: needs revision（ses_07d1a78deffe — 1 blocker: 4 hook points 声明与 3 个实现不一致 + PurReceive vs PurInvoice 语义混淆；4 majors: nop-frontend-dev 缺失 + propId 值未指定 + cancel 路径模糊 + 缺集成测试）
- Independent draft review iteration 2: needs revision（ses_07d0e9c63ffeJTC5WT04qbDM7h — 残留 B1: 3-hook 修正仅传播到 Phase 2 实施层，Phase 0 Explore/Decision + Phase 3 posting.md + Closure Gates 仍说 4 接入点；残留 M1: Task Route 已加 nop-frontend-dev 但 Phase 3 Skill slots 仍为 none）
- Independent draft review iteration 3: pending（修订后由独立子代理新会话复审）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在结束时运行一次。

- [ ] 范围内行为完成（4 新字段 + 2 新日志实体 + 滚动复制 + 结转规则 + 承付 Provider + SPI + **3 接入点** + 测试 + 文档回链）
- [ ] 相关文档对齐（budget.md EXPAND + 2 处既有 owner doc 回链）
- [ ] 已运行验证：`mvn clean install -DskipTests`（154 模块）+ `mvn test -pl module-finance/erp-fin-service`（finance service 全测试含 3 新单元测试类 11 场景）+ `mvn test -pl module-purchase/erp-pur-service`（purchase service 测试含新增 `TestErpPurOrderCommitment` 集成测试 + 既有 113 测试无回归，承付 config-gated 默认 false）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### A3 多公司运营深度（跨公司预算结转 / 合并预算）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 跨公司预算结转 / 合并预算属 A3 successor；本计划仅在同 orgId + 同 acctSchemaId 内结转。
- Successor Required: `yes`（触发条件：A3 启动 + 多公司 owner doc 授权）

### A1 GL Mapping Rule 接入 BUDGET/COMMITMENT 多维规则

- Classification: `optimization candidate`
- Why Not Blocking Closure: A2 复用既有 Provider 科目解析；budgetScenarioId 维度的规则接入归 successor。
- Successor Required: `yes`（触发条件：业务多维预算覆盖需求 + A1 resolver 稳定 ≥ 1 个月后）

### 承付款业务场景全集（销售订单/付款单等其他场景）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅落地采购订单（PurOrder）承付生命周期；其他业务场景归 successor。
- Successor Required: `yes`（触发条件：具体业务需求 + owner doc 授权）

### 预算物化快照表

- Classification: `optimization candidate`
- Why Not Blocking Closure: 派生查询在中等数据量下足够；物化 successor 触发条件明确。
- Successor Required: `yes`（触发条件：BudgetLine EstRows > 10 万行 或 查询 P95 > 500ms）

### commitment 一并结转

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 结转规则中 commitment 是否一并结转业务语义复杂（部分企业视为已发生需结转 / 部分企业视为未发生不结转）；本计划默认不结转（与 actualAmount 合并记录）。
- Successor Required: `yes`（触发条件：业务客户明确 commitment 结转需求）

### 预算对比报表多年度维度实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 字段基础由本计划提供，报表实施归 report successor。
- Successor Required: `yes`（触发条件：业务客户报表需求 + report 域 successor plan）

### 跨币种预算结转汇率差异处理

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 多币种预算结转的汇兑损益属 finance/treasury successor；本计划仅在同 currencyId 内结转。
- Successor Required: `yes`（触发条件：跨国集团多币种预算需求 + treasury owner doc 授权）

### 预算冻结/解冻多级控制

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 既有 controlLevel=HARD/WARN/NONE 三级足够；多级 freeze 归 successor。
- Successor Required: `yes`（触发条件：业务客户具体冻结管理需求）

### 预算编制工作流（自上而下/自下而上/迭代）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 复杂编制流程属 budget workflow successor；本计划仅落地复制/结转能力。
- Successor Required: `yes`（触发条件：业务客户具体编制流程需求）

## Closure

Status Note: pending（计划尚未实施）

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- A3 多公司运营深度（触发：A3 启动 + 多公司 owner doc 授权）
- A1 GL Mapping Rule 接入 BUDGET/COMMITMENT 多维规则（触发：业务多维预算覆盖需求 + A1 resolver 稳定）
- 承付款业务场景全集（触发：具体业务需求）
- 预算物化快照表（触发：数据量 > 10 万行或查询 P95 > 500ms）
- commitment 一并结转（触发：业务客户明确需求）
- 预算对比报表多年度维度实施（触发：业务客户报表需求 + report successor）
- 跨币种预算结转汇率差异处理（触发：跨国集团多币种需求 + treasury owner doc）
- 预算冻结/解冻多级控制（触发：业务客户具体冻结管理需求）
- 预算编制工作流（触发：业务客户具体编制流程需求）
