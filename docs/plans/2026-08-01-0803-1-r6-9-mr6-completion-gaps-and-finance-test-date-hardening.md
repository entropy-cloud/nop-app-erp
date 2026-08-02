# 2026-08-01-0803-1 R6.9 MR6 完成判据遗漏补拆 + finance 测试日期硬化

> Plan Status: completed
> Last Reviewed: 2026-08-01
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR6 工作项 R6.9（唯一剩余 `todo` 工作项）
> Related: `docs/plans/2026-08-01-0656-1-r6-8-mr6-full-verification-completion-criteria.md`（R6.8 核验暴露本 plan 全部输入 + Follow-up 定义 R6.9 范围）；`docs/plans/2026-07-31-2115-1-r6-1-finance-d-mutation-per-mutation-split.md`（finance 域 per-mutation 拆分先例 + Decision 方案 A facade-as-helper-holder）；`docs/architecture/processor-extension-pattern.md`（真相源 `:7/:29/:42/:44-47/:66`）；`docs/architecture/processor-per-mutation-exemption-registry.md §E`（4 处遗漏登记）
> Mission: audit-remediation
> Work Item: R6.9
> Audit: required

## Current Baseline

- **R6.8 核验结论 = MR6 完成判据未完全满足**（plan 2026-08-01-0656-1 Phase 1 Evidence）：全量 grep 暴露 4 处遗漏，登记本 plan（R6.9）补拆/补登记。**MR6 milestone 保持 OPEN**（Rule 13 不可降级——完成判据"所有模块所有 BizModel 的 `@BizMutation` 调用满足 `processor-extension-pattern.md`"未完全满足）。exemption-registry §E 已诚实登记全部 4 处。
- **遗漏 1（类别 A :42）— `ErpFinBudgetScenarioProcessor`**（`module-finance/erp-fin-service/.../budget/ErpFinBudgetScenarioProcessor.java`，671 行）：facade 持有 **2 个内联 D-mutation public 入口**——`rollForward`（`:104`，~27 行：config 门控 + requireScenario + 状态守卫 + createRollForwardScenario + copyBudgetLinesForRollForward[for 循环 newEntity/saveEntity] + writeRollforwardLog）+ `carryForward`（`:136`，~32 行：config 门控 + requireScenario + validateCarryForwardPreconditions + BigDecimal 聚合/computeCarriedAmount + appendCarryForwardLines + writeCarryForwardVoucher[凭证+行+回链 newEntity/saveEntity] + 置 CLOSED + save）。两者均为 ≥3 步含实体创建/BigDecimal/凭证写入。R6.0 triage（plan 2026-07-31-2109-1 line 100）误移除（理由"同因 S-mutation 纯委托 D=0"实误）。**S-mutation 4 子类已存在**（`ErpFinBudgetScenarioSubmitForApprovalProcessor/Approve/Reject/Cancel`，MR5 R5.3 成果，本 plan 不动）。BizModel `ErpFinBudgetScenarioBizModel:73-87` rollForward/carryForward 经 `@BizMutation` → `budgetScenarioProcessor.rollForward/carryForward` 路由。
  - **shared helper 归属现状**：`generateBudgetVoucher`（`:602`，**public**，审核通过生成 BUDGET 影子凭证，被 S-mutation Approve Processor 消费）+ `reverseBudgetVoucher`（`:613`，**public**，作废红冲，被 S-mutation Cancel Processor 消费）+ `requireScenario`/`save`/`validateTransition`/`loadBudgetLines`（被 rollForward/carryForward + S-mutation 子类共享）。注：exemption-registry §E 原述「protected helper」实为 public，不影响 :42 判定（二者非 BizModel-routed D-mutation 入口，是 helper）亦不影响方案 A（facade 保留为 shared helper 持有者，per-mutation Processor `@Inject` facade 调用 helper，访问修饰符无关）。
- **遗漏 2（类别 A :42 边界）— `ErpFinPostingProcessor`**（`module-finance/erp-fin-service/.../posting/ErpFinPostingProcessor.java`，955 行）：facade 持有 **2 个 D-mutation 入口**——`process`（`:126`，正向过账编排，@SingleSession，幂等前置→resolveProvider→resolveOpenPeriod→generateFacts→resolveSubjects→balanceTotals→多账套循环 persistVoucher+generateArApItems）+ `reverseProcess`（`:209`，红冲编排，@SingleSession，findAllPostedVouchers→逐张 buildReversalDraft+persistVoucher+cancelOnReverse）。BizModel `ErpFinVoucherBizModel:72-83` post/reverse 经 `@BizMutation`(@Transactional REQUIRES_NEW) → `postingProcessor.process/reverseProcess`。**本类是 `processor-extension-pattern.md:66` 明示的"业财过账引擎（IErpFinVoucherBiz + ErpFinPostingProcessor）"canonical Facade+Processor 范例**（forward/reverse 对称逆操作 + 全域过账共享引擎，非无关 mutation 拼装）。process/reverseProcess 共享大量 protected step（resolveProvider/resolveOpenPeriod/generateFacts/persistVoucher 等），拆分将产生重复或须共享基类。**裁决=engine 豁免登记 OR 拆两 Processor**（Phase 1 Decision）。
- **遗漏 3（类别 B :5/:7）— `ErpInvCostingBizModel.reclosePeriodCosts`**（`module-inventory/erp-inv-service/.../costing/ErpInvCostingBizModel.java`，327 行，`@BizModel("ErpInvCosting")` 服务型 BizObject，finance 期末结账经 `IBizObjectManager.getBizObject("ErpInvCosting")` 跨模块调用）：方法 `reclosePeriodCosts`（`:74-116`，~40 行：嵌套循环 move→line→ledger + costMethodResolver 分支[层/加权平均] + recomputeIncomingLayerIfMissing/recomputeOutgoingCogs/recomputeWeightedAverageOutgoing[含 BigDecimal 算术 + appendLayer/saveOrUpdateEntity 实体写] + ormTemplate.flushSession）= ≥3 步内联，零 Processor 委托。R6.0 triage 未扫 `costing/` 包（仅扫 `entity/`），域外遗漏。BizModel 全部 private helper（recompute*×3 + find* 查询×6 + appendLayer + nz）仅被 reclosePeriodCosts 使用，可整体迁入 Processor。
- **遗漏 4（类别 B 登记缺口 + 边界）— `ErpMdSupplierApprovalBizModel`**（`module-master-data/erp-md-service/.../entity/ErpMdSupplierApprovalBizModel.java`，270 行）：
  - **6 项单步状态翻转**（apply `:93`/approve `:106`/probate `:123`/suspend `:136`/reinstate `:156`/reject `:171`）：均 require+守卫+setStatus(+审计字段)+updateEntity，approve 含 `requireQualificationValid` 属 `validate*` 允许。判 `:46` **合法豁免**，但整个 entity 未入 registry §A（master-data 无 §A 段；R6.7 master-data 仅拆 ErpMdCurrency.refreshRatesFromApi）→ **登记缺口，须补登记**（documentation-only，零代码变更）。
  - **`suspendByPartner`（`:143`）边界**：批量循环（findActiveByPartner + `for (approval) doSuspend(...)` + return count）。`doSuspend` 本身是 :46 单步翻转，但 suspendByPartner 含 for 循环内写（updateEntity）。按 exemption-registry §判定规则条件 4「无循环副作用：无 for/stream 内的写/创建」= **不满足豁免**。R6.7 已有批量操作拆 Processor 先例（`ErpHrSalarySimulationApplyBatchAdjustmentProcessor`/`ErpHrShiftAssignmentAssignBatchProcessor`/`ErpHrShiftAssignmentCopyFromPeriodProcessor`/`ErpB2bAsnRetryMatchProcessor`）→ 一致裁决=须拆（Phase 2 Decision 复议确认）。
- **finance 测试日期脆弱（R6.8 核验暴露 5 处，非 R6.x 回归）**——R6.1 finance 2026-07-31 306 全绿 + R6.8 零 Java 变更，全部为 7→8 月翻滚暴露：
  - **`TestErpFinNotesPayableStateMachine`（4 方法：testHonorReleasesCredit/testIssueCommercialAcceptanceNoCreditCheck/testIssueBankAcceptanceOccupiesCredit/testWriteOffReleasesCredit）**：行为型日期脆弱——过账引擎 `resolveOpenPeriod(voucherDate)` 在 8 月抛 `erp.err.fin.posting.period-not-found`（`seedBase()` 仅 seed `"2026-07"` OPEN 期间 `:121`，票据 issue() 自动创建期间在 8 月但无对应 OPEN 期间），致 `POSTED=false` + 无凭证。**非 value drift**（重录会 bless 过账失败，违反测试语义）→ 须测试数据硬化（按 voucher/运行月预置 OPEN 期间）。
  - **`TestErpFinDashboard.testTrendMonthlySeries`（1 方法）**：测试逻辑型日期脆弱——`getDashboardTrend(2, CTX)` = "近 2 月"窗口，硬编码 seed 2026-06/2026-07 GL 余额 + 断言窗口含 6/7 月；8 月窗口={8,7}月，6 月被挤出 → `assertTrue(hasJun)` 失败。须 seed 改为 `YearMonth.now()` 相对月（非快照漂移，`force-save` 无法修复硬编码断言）。
  - （注：R6.8 已重录 6 处 clean value-drift——BadDebt 3 + EmployeeAdvance 3——为 8 月基线，不在本 plan 范围。）
- **验证基线**：`mvn clean install -DskipTests` 156 模块 BUILD SUCCESS；finance `mvn test` 306 = 301 绿 + 5 pre-existing date-fragility（本 plan 硬化目标）；compliance checker exit 0（R8=0/R2c=1380/R2d=32，R6.8 校准后）；i18n checker exit 0。

## Goals

- **G1（MR6 完成判据闭合）**：R6.8 核验暴露的 4 处遗漏全部消解——遗漏 1（BudgetScenario rollForward/carryForward）+ 遗漏 3（InvCosting reclosePeriodCosts）+ 遗漏 4 边界（suspendByPartner）拆为独立 `<Entity><Method>Processor`（self-contained `process()` + protected step）；遗漏 2（Posting process/reverseProcess）经 Phase 1 Decision 裁决（engine 豁免登记 OR 拆两 Processor）；遗漏 4 的 6 项 :46 翻转补登记 exemption registry §A。复跑 R6.8 完成判据 grep 核验 = 零违规。
- **G2（finance 测试日期硬化）**：5 处 pre-existing date-fragility（NotesPayable 4 + Dashboard 1）硬化为日期无关（按 voucher/运行月预置期间 + `YearMonth.now()` 相对 seed），消除月翻滚致失败，保留原有业务断言语义。
- **G3（MR6 milestone 闭合 + bookkeeping）**：MR6 milestone 由 OPEN 转 CLOSED（完成判据满足后）；exemption-registry §E 全部 4 项标记 resolved；arm-index P1-MA3-062 最终状态回填；roadmap R6.9 `todo`→`done` + MR6 milestone 闭合注记；`docs/logs/2026/08-01.md` 追加条目。

## Non-Goals

- **不重开 MR5 / R6.1-R6.8**：S-mutation + 256 已拆 D-mutation 状态保持 done；本 plan 仅补 R6.8 暴露的遗漏。
- **不新增业务语义/状态机迁移/错误码调整**：遗漏 1/3/4 是编排位置迁移（facade/BizModel → per-mutation Processor），语义逐字搬运不变；遗漏 2 若拆则仅编排位置迁移，若豁免则零代码变更。
- **不触及 ORM 模型 / api.xml / view.xml**：本 plan 是 Java Processor 提取 + beans.xml 注册 + 测试 fixture/逻辑硬化 + registry/markdown bookkeeping，无 ORM ask-first。
- **不处理 R3.x successor**（employee-id data-auth / SoD 全域铺开 / `_helper.ts` 解除 / R2c 泛型重构）：optimization candidate，无 re-trigger 条件。
- **不硬化 R6.8 已重录的 6 处 clean value-drift**（BadDebt/EmployeeAdvance）：已为 8 月基线，不在本 plan 范围。
- **不为 BudgetScenario 的 config-gated rollForward/carryForward 补集成测试**：测试覆盖深挖属 MR2/MR3（已完成）；本 plan 仅验证既有测试行为等价。

## Task Route

- Type: `implementation-only change`（per-mutation Processor 提取 + 测试硬化）+ 局部 `verification or audit work`（R6.8 完成判据复验 + MR6 milestone 闭合判定）
- Owner Docs: `docs/architecture/processor-extension-pattern.md`（真相源 `:7/:29/:42/:44-47/:66`）；`docs/architecture/processor-per-mutation-exemption-registry.md §E`（4 处遗漏登记 + §判定规则）；`docs/design/finance/budget.md`（rollForward/carryForward 语义）；`docs/design/finance/posting.md`（过账引擎语义）；`docs/design/finance/period-close.md`+`costing-methods.md`（InvCosting reclose 语义）；`docs/design/master-data/`（supplier approval 状态机）
- Skill Selection Basis: `nop-backend-dev`（Processor per-mutation 纪律决策门 + 反模式自检表 + `@Inject` 纪律 + facade-as-helper-holder 方案 A）；`nop-testing`（回归验证 + 测试日期硬化范式）。涉及会计保护区域（预算凭证/过账引擎/成本重算），须对照 owner doc 静态校验语义不变。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 — Posting engine 裁决 + 类别 A BudgetScenario 补拆

Status: completed
Targets: `module-finance/erp-fin-service/.../budget/ErpFinBudgetScenario*Processor.java`（新建 2 文件）；`ErpFinBudgetScenarioProcessor.java`（facade 瘦身）；`ErpFinBudgetScenarioBizModel.java`（重配线）；`docs/architecture/processor-per-mutation-exemption-registry.md`（Posting 豁免登记若裁决为豁免）；`docs/architecture/processor-extension-pattern.md`（若裁决为豁免，补 engine 例外交叉引用，仅在 owner doc 确认 :66 已背书时）
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: R6.8 done（已满足）

- [x] **Decision: `ErpFinPostingProcessor` process/reverseProcess 裁决（engine 豁免登记 OR 拆两 Processor）**。真相源 = `processor-extension-pattern.md:66`（明示 `IErpFinVoucherBiz + ErpFinPostingProcessor` 为 canonical 业财过账引擎 Facade+Processor 范例）。考虑的替代方案：
  - (a) **engine 豁免登记**：process/reverseProcess 是同一过账引擎的 forward/reverse 对称逆操作（非 :42 所指"无关 mutation 拼装"），pattern doc :66 自身以此为 canonical Processor 范例；两者共享 ~95% protected step（resolveProvider/resolveOpenPeriod/generateFacts/resolveSubjects/balanceTotals/persistVoucher），拆分将产生重复或须抽共享基类（违背 self-contained per-mutation 目标）。处置=exemption-registry 新增 §Engine 段（或 §B 扩展）登记 + pattern doc :66 交叉引用 + 零生产代码变更。**最低风险**（会计保护区域零改动）。
  - (b) **拆两 Processor**：`ErpFinPostingProcessProcessor` + `ErpFinPostingReverseProcessProcessor`，共享 protected step 上提到 `AbstractErpFinPostingProcessor` 基类。语义不变但触碰全域过账引擎核心，@SingleSession/@Transactional 边界须重新验证，风险最高。
  - 记录选择、替代方案、残留风险。若选 (a)，须 Explore 核实 pattern doc :66 原文确实以本类（含 process+reverseProcess 两入口）为 canonical 范例（非仅名义引用）；若核实为真，(a) 为真相源背书的正确裁决。若选 (b)，须核实 @SingleSession/@Transactional REQUIRES_NEW 边界在新 Processor 中等价。
  - **裁决结论=(a) engine 豁免登记**。Explore 核实 `processor-extension-pattern.md:66` 原文：「参照实例：业财过账引擎（`IErpFinVoucherBiz` + `ErpFinPostingProcessor`…）」，确实以本类（process+reverseProcess 两入口共享 ~95% protected step）为 canonical Facade+Processor 范例。已落地：exemption-registry §Engine 段登记 + pattern doc :66 已含交叉引用（原文即背书，无需追加）。零生产代码变更。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpFinBudgetScenarioProcessor` 2 D-mutation 拆分 → `ErpFinBudgetScenarioRollForwardProcessor`（提取 rollForward 主流程 + 其专属 helper：isRollForwardEnabled/resolveStrategy/createRollForwardScenario/copyBudgetLinesForRollForward/adjustAmountByStrategy/remapPeriodId/writeRollforwardLog）+ `ErpFinBudgetScenarioCarryForwardProcessor`（提取 carryForward 主流程 + 其专属 helper：isCarryForwardEnabled/resolveRule/validateCarryForwardPreconditions/isSourceFiscalYearFullyClosed/aggregateSourceAmounts/aggregateActualForScenario/aggregateActualForLine/computeCarriedAmount/appendCarryForwardLines/writeCarryForwardVoucher/resolveFirstPeriodId + carryForward 消费的 generateBudgetVoucher/reverseBudgetVoucher 调用路径）。每 Processor self-contained `process()` + protected step。facade 按 R6.1 方案 A 保留为 shared helper 持有者（requireScenario/save/validateTransition/loadBudgetLines/resolveUserId 等被 S-mutation 子类 + 新 per-mutation 共享的 helper 留 facade；per-mutation `@Inject` facade 调用）。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpFinBudgetScenarioBizModel` rollForward/carryForward 重配线——新增 `@Inject ErpFinBudgetScenarioRollForwardProcessor` + `ErpFinBudgetScenarioCarryForwardProcessor`，方法体从 `return budgetScenarioProcessor.rollForward(...)` 改为 `return rollForwardProcessor.rollForward(...)` 单行委托（carryForward 同）。facade `budgetScenarioProcessor` 注入字段已移除。
  - Skill: `nop-backend-dev`
- [x] Add: beans.xml 注册 2 新 Processor bean（对齐 R6.1 既有 per-mutation bean 注册范式）。
  - Skill: `nop-backend-dev`
- [x] Proof: finance service 本地编译通过（`mvn compile -pl module-finance/erp-fin-service -am -DskipTests`）+ grep 确认 BudgetScenarioBizModel rollForward/carryForward 已改单行委托、BudgetScenarioProcessor facade 不再持 rollForward/carryForward public 入口。
  - Skill: none
  - 注：rollForward/carryForward 提取为新 Processor 类后，异常路径测试的堆栈跟踪类名可能漂移（对齐 R6.x 先例）；Phase 3 finance `mvn test` 与 Closure Gates 全量 `mvn test` 会捕获并按需重录为新基线（GraphQL 经 BizModel 契约面不变，预期无漂移或仅类名漂移）。**实测**：8 个 BudgetScenario 测试（3 rollForward + 5 carryForward）全绿，行为等价。

Exit Criteria:

> 本阶段交付 Posting 裁决结论 + BudgetScenario 2 per-mutation 自包含 + facade 瘦身 + BizModel 重配线 + 编译通过。

- [x] Posting engine 裁决已记录（选择 + 替代分析 + 残留风险 + 真相源 :66 Explore 核实结论）；裁决为 (a) 则 exemption-registry 已登记，裁决为 (b) 则两 Processor + 共享基类已落地
- [x] 2 个新 BudgetScenario per-mutation Processor 文件存在且 self-contained（`process()` + protected step，非回委托 facade public 入口）
- [x] BudgetScenarioBizModel rollForward/carryForward 改单行委托 + beans.xml 注册 + finance service 本地编译通过

### Phase 2 — 类别 B InvCosting 补拆 + MdSupplierApproval 登记/suspendByPartner 复议

Status: completed
Targets: `module-inventory/erp-inv-service/.../costing/ErpInvCostingReclosePeriodCostsProcessor.java`（新建）；`ErpInvCostingBizModel.java`（瘦身+重配线）；`module-master-data/erp-md-service/.../entity/ErpMdSupplierApproval*Processor.java`（视复议结论新建 0-1 文件）；`ErpMdSupplierApprovalBizModel.java`（视复议结论重配线）；`docs/architecture/processor-per-mutation-exemption-registry.md`（§A master-data 段补登记 6 项 + suspendByPartner 复议结论 + §E resolved 标记）
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1

- [x] Add: `ErpInvCostingBizModel.reclosePeriodCosts` 拆分 → `ErpInvCostingReclosePeriodCostsProcessor`（提取 reclosePeriodCosts 主流程 + 其全部专属 helper：recomputeIncomingLayerIfMissing/recomputeOutgoingCogs/recomputeWeightedAverageOutgoing/findDoneMovesInPeriod/loadLines/findLedgers/findExistingLayer/findLayersForMethod/findBalanceForLedger/appendLayer/nz + LAYER_BASED_METHODS 常量；Processor `@Inject IDaoProvider`+`IOrmTemplate`+`CostMethodResolver`）。self-contained `process()` + protected step。BizModel 保留 `@BizMutation` 入口，方法体改 `return processor.process(periodId, startDate, endDate, context)` 单行委托（BizModel 仅留 `@Inject` Processor）。
  - Skill: `nop-backend-dev`
- [x] Add: beans.xml 注册 `ErpInvCostingReclosePeriodCostsProcessor` bean。
  - Skill: `nop-backend-dev`
- [x] **Decision: `ErpMdSupplierApproval.suspendByPartner` 复议**。判定准绳 = exemption-registry §判定规则（条件 4「无循环副作用」为豁免硬要求）。suspendByPartner = findActiveByPartner + `for (approval) doSuspend(...)`[循环内 updateEntity 写] + return count。考虑：
  - (a) **须拆**（strict triage）：条件 4 不满足（循环内写），且 R6.7 已有批量操作拆 Processor 先例（HrSalarySimulation.applyBatchAdjustment / HrShiftAssignment.assignBatch/copyFromPeriod / B2bAsn.retryMatch）→ 一致裁决。处置=新建 `ErpMdSupplierApprovalSuspendByPartnerProcessor`（提取 findActiveByPartner + 循环 doSuspend + count；doSuspend/findActiveByPartner helper 随迁或保留 BizModel protected 供 suspend 单步入口复用）。
  - (b) 豁免登记：每循环迭代是 :46 单步翻转，suspendByPartner 是薄批量包装。但违反条件 4 明文 + 与 R6.7 先例不一致 → 否决。
  - 预期裁决=(a) 须拆（与 R6.7 先例一致）。记录选择 + 替代分析。若 (a)：新建 Processor + BizModel suspendByPartner 改单行委托 + beans.xml 注册。
  - Skill: `nop-backend-dev`
- [x] Add: exemption-registry §A 新增 master-data 段——补登记 `ErpMdSupplierApproval` 6 项 :46 单步翻转豁免（apply/approve/probate/suspend/reinstate/reject），每项注 require+守卫+setStatus+updateEntity + approve 含 requireQualificationValid(validate* 允许)。若 suspendByPartner 复议=(a)，则 suspendByPartner 不入豁免（已拆 Processor）；若复议=(b)，则 suspendByPartner 入豁免并注循环 :46 边界理由。
  - Skill: `nop-backend-dev`
- [x] Proof: inventory + master-data service 本地编译通过（`mvn compile -pl module-inventory/erp-inv-service,module-master-data/erp-md-service -am -DskipTests`）+ grep 确认 ErpInvCostingBizModel.reclosePeriodCosts 已改单行委托、（若复议须拆）ErpMdSupplierApprovalBizModel.suspendByPartner 已改单行委托。
  - Skill: none

Exit Criteria:

> 本阶段交付 InvCosting per-mutation 自包含 + MdSupplierApproval 豁免补登记 + suspendByPartner 复议结论落地 + 编译通过。

- [x] `ErpInvCostingReclosePeriodCostsProcessor` 文件存在且 self-contained + ErpInvCostingBizModel.reclosePeriodCosts 改单行委托 + beans.xml 注册 + inventory service 编译通过
- [x] suspendByPartner 复议结论已记录；若须拆则 Processor + BizModel 重配线 + beans.xml 落地，若豁免则 exemption-registry 登记理由完整
- [x] exemption-registry §A master-data 段 6 项 :46 豁免已补登记 + master-data service 编译通过

### Phase 3 — finance 测试日期硬化

Status: completed
Targets: `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinNotesPayableStateMachine.java`；`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/dashboard/TestErpFinDashboard.java`
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 1 + Phase 2（架构变更已落地，确保测试在最终代码上硬化）

- [x] Fix: `TestErpFinNotesPayableStateMachine` 日期硬化——4 方法（testHonorReleasesCredit/testIssueCommercialAcceptanceNoCreditCheck/testIssueBankAcceptanceOccupiesCredit/testWriteOffReleasesCredit）失败的根因 = `seedBase()` 仅 seed `"2026-07"` OPEN 期间，而 issue() 过账 `resolveOpenPeriod(voucherDate)` 在 8 月无对应期间。硬化=使 seed 的 OPEN 期间覆盖票据过账 voucher date 所落月份。方案：`seedBase()` 改用 `java.time.YearMonth.now()`（或票据 voucher date 所在月）seed 当前运行月 OPEN 期间，使过账引擎在任意运行月均能 resolveOpenPeriod 成功。保留全部既有业务断言（status/credit 占用-释放）语义不变。
  - Skill: `nop-testing`
- [x] Fix: `TestErpFinDashboard.testTrendMonthlySeries` 日期硬化——根因 = `getDashboardTrend(2)` 近 2 月窗口 + 硬编码 seed 2026-06/2026-07 + 断言含 6/7 月；8 月窗口={8,7}挤出 6 月。硬化=seed 改为 `java.time.YearMonth.now()` 相对月（seed 当前月 + 上月 GL 余额），断言改为校验当前月+上月的关键字段（`m.endsWith(...)` 用相对月后缀），使窗口任意月均含 seed 数据。保留收入算术断言语义不变。
  - Skill: `nop-testing`
- [x] Proof: finance 域 `mvn test -pl module-finance/erp-fin-service -am` 全绿（目标 306 = 306 绿，0 failures/0 errors），含 5 处硬化后测试。确认硬化测试在 7 月/8 月/任意月均通过（日期无关）。
  - Skill: `nop-testing`

Exit Criteria:

> 本阶段交付 5 处测试日期无关硬化 + finance 域全绿。

- [x] TestErpFinNotesPayableStateMachine 4 方法 + TestErpFinDashboard.testTrendMonthlySeries 日期硬化落地（`YearMonth.now()` 相对 seed/期间），保留原业务断言语义
- [x] finance 域 `mvn test` 全绿（306/306，0 failures/0 errors）

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (ses_0455becf7ffeGgjlapAYvNIxgH) — 全 7 项源文件事实声明经独立子代理 live-repo 复核实测精确（BudgetScenario 2 D-mutation 入口 + 4 S-mutation 子类 / Posting process+reverseProcess + 共享 protected step / InvCosting ≥3 步内联 + 全 private helper 独占 / MdSupplierApproval 6 单步翻转 + suspendByPartner 循环内写违反条件 4 / 2 测试日期脆弱根因）。无 blocking issue。规则合规全 OK（Rule 4/14 单 plan 范围、Rule 1 基线、Rule 7 退出标准本地化、Rule 9 两 Decision 含 Explore、Rule 13 不可降级、MR6 闭合门控、反松弛、模板）。采纳 2 项非阻塞 precision 修正：(N1) generateBudgetVoucher/reverseBudgetVoucher 实为 public 非 protected（事实精度，不影响 :42 判定/方案 A）；(N2) 补异常路径快照重录 awareness 注记。计划作为执行契约可接受，提升为 active。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处：全量 `mvn clean install -DskipTests` + `mvn test` + compliance checker。

- [x] 范围内行为完成（G1 完成判据闭合：4 处遗漏消解 + R6.8 grep 复验零违规；G2 测试日期硬化：5 处日期无关；G3 MR6 milestone 闭合 + bookkeeping）
- [x] 相关文档对齐（`processor-extension-pattern.md` 若 Posting 豁免则交叉引用；`processor-per-mutation-exemption-registry.md §A` master-data 补登记 + §E 4 项 resolved + §Engine/§B Posting 豁免；`docs/design/finance/budget.md`/`posting.md` 无语义变更故无更新；`arm-index.md` P1-MA3-062 最终状态；roadmap §MR6 R6.9 done + milestone 闭合）
- [x] 已运行验证：全量 `mvn clean install -DskipTests`（156 模块 BUILD SUCCESS）+ 全量 `mvn test`（0 failures/0 errors，含 5 处硬化测试 + 0 R6.9 回归）+ `bash docs/audits/nop-compliance-checker.sh`（exit 0，R8/R2c/R2d 不超 R6.8 基线）+ `bash docs/audits/i18n-coverage-checker.sh --strict`（exit 0）
- [x] **R6.8 完成判据 grep 复验 = 零违规**（类别 A :42 零裸 facade 多 D-mutation[Posting 若豁免则登记在案] + 类别 B :5/:7 零 ≥3 步内联）
- [x] 会计保护区域语义不变（BudgetScenario 凭证/过账引擎/InvCosting 成本重算经既有测试行为等价验证；Posting 若拆则 @SingleSession/@Transactional 边界等价）
- [x] 无范围内项目降级为 deferred/follow-up（Posting 裁决、suspendByPartner 复议均须落地结论，不可留模糊）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

_（本 plan 起草时无 deferred 项。Posting engine 裁决与 suspendByPartner 复议均须在 Phase 1/2 落地明确结论，不可降级为 deferred——二者是 MR6 完成判据的范围内项，Rule 13。）_

## Closure

Status Note: MR6 milestone 由 OPEN 转 CLOSED——R6.8 核验暴露的 4 处完成判据遗漏全部消解（BudgetScenario rollForward/carryForward 补拆为 2 per-mutation Processor + InvCosting reclosePeriodCosts 补拆 + MdSupplierApproval suspendByPartner 补拆 + Posting engine 裁决为 §Engine 豁免 + MdSupplierApproval 6 项 :46 补登记），5 处 finance 测试日期硬化，完成判据 grep 复验零违规。执行期发现并修复 1 处生产缺陷（ErpInvStockMoveReverseProcess reversal businessDate 用 CoreMetrics.today() 而非 original move date，致 3 处跨域测试日期脆弱——修正为 original date 后全量 0 failures/0 errors）。

Closure Audit Evidence:

- Auditor / Agent: 执行者自验（全量 `mvn clean install -DskipTests` BUILD SUCCESS + 全量 `mvn test` BUILD SUCCESS 0 failures/0 errors + compliance checker exit 0 + i18n checker exit 0 + R6.8 grep 复验零违规）。独立结束审计由后续子代理新会话执行。
- Evidence: 本 plan 全 [x] + 全量绿基线（156 模块 BUILD SUCCESS + `mvn test -fae` BUILD SUCCESS + `nop-compliance-checker.sh exit 0` + `i18n-coverage-checker.sh --strict exit 0`）

Follow-up:

- 无阻塞跟进项。执行期发现的生产缺陷（reversal businessDate）已修复，附带重录 1 处 FIFO costing 快照（INCOMING_DATE 由 today 改为 original date，语义正确）。
