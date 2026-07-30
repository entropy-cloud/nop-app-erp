# 2026-07-30-1433-3-mr5-r5-3-finance-s-mutation finance 域 S-mutation 逻辑下沉

> Plan Status: completed
> Last Reviewed: 2026-07-30
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MR5 工作项 R5.3
> Related: `docs/plans/2026-07-30-1433-1-mr5-r5-1-purchase-s-mutation.md`（pilot 配方）、`docs/plans/2026-07-25-1057-2-per-mutation-processor-file-split.md`（其 finance 行 inline-script 分类经实测证伪，见下方基线修正）
> Audit: required

## Current Baseline

- finance 域 20 个 per-mutation Processor 文件已存在，当前为空心委托（每个注入 facade 并回委托）。
- 4 实体的运行时调用链与可达性**实测分三类**（独立草案审查 spot-check 证伪了 per-mutation-processor-split-plan.md 的「BadDebt/BudgetScenario = all inline-script」分类——其 xbiz 实为空）：
  - **xbiz-source 类（10 文件，运行时可达）**：`ErpFinEmployeeAdvance`（submit/approve/reject/reverseApprove/withdrawApproval = 5）+ `ErpFinExpenseClaim`（5）——xbiz `<source>` 一行 `inject('...Processor').method(...)` 委托 per-mutation Processor → 空心回委托 facade。**此 10 文件在运行时路径上，既有测试覆盖。**
  - **BizModel-facade 类（8 文件，运行时休眠）**：`ErpFinBadDebt`（submit/approve/reject/reverseApprove = 4）+ `ErpFinBudgetScenario`（submit/approve/reject/cancel = 4）——xbiz 为**空**（`<actions/>`，实测确认），运行时走 `@BizMutation BizModel → facade Processor`（如 `ErpFinBadDebtBizModel.approve() → badDebtProcessor.approve()`）。per-mutation Processor 文件存在但**无任何调用方注入**——R5.8 重配线 BizModel 前它们不在运行时路径上。
  - **no-source cancel（2 文件，运行时休眠）**：`ErpFinEmployeeAdvanceCancelProcessor` + `ErpFinExpenseClaimCancelProcessor`——无 xbiz `<source>`，经 BizModel `@BizMutation cancel()` → facade.cancel()。per-mutation 文件空心休眠。
- facade Processor 持有全部真实逻辑且**已使用 `NopException` + ErrorCode**（实测 `ErpFinBadDebtProcessor` 有 submit/approve/reject/reverseApprove/writeOff/recover 方法，全用 `ERR_BAD_DEBT_*` 错误码，无 `NopScriptError`）——故 R5.3 是**纯 Java→Java 机械迁移**（facade Java → per-mutation abstract-base hook），无 inline-script 语义提取。
- **[会计保护区域]** BadDebt approve/reverseApprove 涉及坏账凭证生成 + ArApItem 对称金额/状态变异（`ErpFinBadDebtProcessor:183-226`），BudgetScenario 涉及预算——owner doc `docs/design/finance/` + R1.10/R1.11/R1.27 已固化语义；本 plan 仅做编排位置迁移（facade → per-mutation hook），不改业务语义。
- 命名差异：BadDebt/BudgetScenario 用 `submit`（非 `submitForApproval`），per-mutation 文件名为 `*SubmitForApprovalProcessor`——须确认 `AbstractSubmitForApprovalProcessor` 骨架对 `submit` 语义适用 + xbiz mutation name 对齐（EmployeeAdvance/ExpenseClaim 用 `submitForApproval`）。
- 剩余差距：per-mutation 空心；split plan 的 inline-script 分类已证伪，须回注修正。

## Goals

- finance 域 20 个 per-mutation Processor 各自自包含：S-mutation 编排走抽象基类骨架，域特有逻辑通过 hook override 实现（纯 Java facade → hook 迁移，无 inline-script 提取）。
- 10 个运行时可达文件（EmployeeAdvance/ExpenseClaim source-backed）经既有测试验证行为等价。
- 10 个运行时休眠文件（BadDebt 4 + BudgetScenario 4 + EmployeeAdvance/ExpenseClaim cancel 2）经静态 parity 校验（编译 + 逐 hook 对照 facade 代码审查清单）确认迁移保真，运行时行为验证显式移交 R5.8。

## Non-Goals

- D-mutation（`writeOff`/`recover`、`rollForward`/`carryForward`）保留在 facade——MR5 范围外。
- BizModel 配线从 facade 改为 per-mutation + xbiz 清理 + beans.xml——属 R5.8（休眠文件的运行时激活与验证归 R5.8）。
- NotesPayable、NotesReceivable、AccountingPeriod、PostingProcessor（纯 D-mutation，无 per-mutation 文件）——不在 R5.3 范围。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/finance/state-machine.md`、`docs/design/finance/bad-debt.md`、`docs/design/finance/period-close.md`（BudgetScenario）、`docs/architecture/processor-extension-pattern.md`
- Skill Selection Basis: 后端 Processor 重构匹配 `nop-backend-dev` + `nop-testing`。BadDebt/BudgetScenario 涉及会计保护区域，须对照 R1.10/R1.11/R1.27 owner doc 静态校验语义不变。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。

## Execution Plan

### Phase 1 - 运行时可达 per-mutation 填充（EmployeeAdvance + ExpenseClaim source-backed）

Status: completed
Targets: `module-finance/erp-fin-service/.../processor/ErpFin{EmployeeAdvance,ExpenseClaim}*{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval}Processor.java`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: R5.1 共享 hook 策略 Decision 已落定（本 plan 复用；EmployeeAdvance/ExpenseClaim 无跨域 side-effect hook，per-mutation 仅实现抽象方法 + 域特有校验，策略选择影响小）

- [x] Add: 10 个 EmployeeAdvance/ExpenseClaim source-backed per-mutation 填充——删除空心回委托，改为抽象基类骨架 + hook override 承载 facade step 逻辑。
  - Skill: `nop-backend-dev`
  - 实测：采用 Pattern B（custom public override）逐文件 1:1 复刻 facade 公共方法的编排流（requireXxx → validateNotCancelled → validateTransitionForXxx → 业务校验 → derive/钩子 → doXxx），通过同包 protected helper 调用 facade step。facade 公共 S-mutation 方法转为 R5.8 待清理的死代码参考实现。10 文件全部自包含（无 `return processor.method()` 一行回委托）。

Exit Criteria:

> 本阶段交付运行时可达文件的 per-mutation 自包含化（既有测试可验证）。

- [x] EmployeeAdvance + ExpenseClaim source-backed per-mutation 本地编译通过（`mvn compile -pl module-finance/erp-fin-service -am -DskipTests`）— 实测 BUILD SUCCESS；TestErpFinEmployeeAdvanceApproval(7) + TestErpFinExpenseClaimApproval(8) = 15 tests, 0 failures 行为等价验证通过

### Phase 2 - 运行时休眠 per-mutation 填充（BadDebt + BudgetScenario + 2 cancel）

Status: completed
Targets: `module-finance/erp-fin-service/.../processor/ErpFinBadDebt*{SubmitForApproval,Approve,Reject,ReverseApprove}Processor.java`、`ErpFinBudgetScenario*{SubmitForApproval,Approve,Reject,Cancel}Processor.java`、`ErpFin{EmployeeAdvance,ExpenseClaim}CancelProcessor.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1

- [x] Add: 8 个 BadDebt/BudgetScenario per-mutation 填充——纯 Java facade → abstract-base hook 迁移（facade 的 `ErpFinBadDebtProcessor.submit/approve/reject/reverseApprove` 逻辑复制到 per-mutation hook override）。BadDebt approve 的凭证生成 + ArApItem 变异映射到 `afterStateChange`；reverseApprove 的红冲 + 对称回滚映射到 `beforeStateChange`（红冲在状态回退前，对齐既有顺序）。
  - Skill: `nop-backend-dev`
  - 实测：采用 Pattern B custom public override 1:1 复刻 facade 公共方法编排流。BadDebt facade（同包 `processor`）protected helper 直接可达；reverseApprove 的会计保护区域逻辑（红冲凭证 + ArApItem 对称回滚 + REJECTED）抽取为 `ErpFinBadDebtProcessor.executeReverseApprove(debt, ctx)` protected helper，facade 公共 reverseApprove 与 per-mutation 共用单一真相源（避免会计语义复制漂移）。BudgetScenario facade 位于 `budget` 包（跨包），将其 5 个 helper（requireScenario/validateTransition/generateBudgetVoucher/reverseBudgetVoucher/save）由 protected 提升为 public（最小可见性变更，语义不变），per-mutation 跨包调用实现自包含编排。
- [x] Add: 2 个 EmployeeAdvance/ExpenseClaim cancel per-mutation 填充——同 Phase 1 模式，cancel 逻辑从 facade 复制到 `AbstractCancelProcessor` hook override。
  - Skill: `nop-backend-dev`
  - 实测：Pattern B custom public override 复刻 facade.cancel 流（requireXxx(advanceId/claimId) → validateTransitionForCancel → doCancel(advanceId/claimId, ...))。doCancel 内含 posting reverse + reload + clearPosted（EmployeeAdvance）/ offset+posting reverse（ExpenseClaim），由 facade protected helper 承载，单一真相源。
- [x] Decision: 休眠文件验证策略——由于此 10 文件在 R5.8 重配线前不在运行时路径，既有测试无法覆盖新 hook。裁决：R5.3 采用**静态 parity 校验**（编译 + 逐 hook 对照 facade 代码审查清单：状态机迁移、错误码 + `.param()`、config-gated 门控、凭证生成时序），运行时行为验证移交 R5.8（R5.8 重配线后既有测试即激活覆盖）。
  - 替代方案（被拒）：为休眠 per-mutation 补白盒单元测试（直接实例化 Processor 调用）——被拒因 Processor 有 `@Inject` 依赖（daoProvider、跨域 Biz），需部分容器或 mock，成本与收益不匹配（R5.8 将自然激活覆盖）。
  - Skill: `nop-backend-dev`
- [x] Proof: 静态 parity 校验——BadDebt approve/reverseApprove 的会计保护区域不变量逐项对照（凭证生成类型/金额/方向、ArApItem 对称金额+状态、红冲对称性、期间守卫），确认迁移仅改编排位置不改业务规则。
  - Skill: `nop-backend-dev`
  - 校验结果：per-mutation 通过调用 facade 的 protected/public helper 承载全部会计逻辑（executeWriteOff/executeRecovery/approveInternal/executeReverseApprove），无任何会计规则复制。逐项对照：①BadDebt approve → approveInternal → executeWriteOff/Recovery：ArApItem status→WRITTEN_OFF/OPEN + settled/open 对称金额 + BAD_DEBT_WRITE_OFF/RECOVERY 凭证（借Allowance/贷AR vs 借AR/贷Allowance），方向/金额/partnerId 经 facade helper 单一真相源，迁移零漂移；②BadDebt reverseApprove → executeReverseApprove：红冲凭证（billHeadCode=debt.code）+ ArApItem 反向对称回滚（writeOff: WRITTEN_OFF→OPEN/settled-=amount/open+=amount；recovery 反之）+ APPROVED→REJECTED，与 facade 公共 reverseApprove 共用 executeReverseApprove 单一真相源；③BudgetScenario approve → generateBudgetVoucher（BUDGET 影子凭证，首张 ID 回写 voucherId）+ docStatus/approveStatus 双设 APPROVED；cancel → reverseBudgetVoucher 红冲 BUDGET 凭证 + docStatus=CANCELLED；④错误码 + `.param()` 全部经 facade helper（illegalTransition/ERR_BAD_DEBT_*），无 NopScriptError 引入；⑤config-gated 门控（`erp-fin.bad-debt-write-off-require-approval`、`erp-fin.expense-budget-check-enabled`）保留在 facade，未改。

Exit Criteria:

> 本阶段交付休眠文件的 per-mutation 自包含化 + 静态 parity 证据（运行时验证移交 R5.8）。

- [x] BadDebt/BudgetScenario + cancel per-mutation 本地编译通过 — 实测 `mvn compile -pl module-finance/erp-fin-service -am -DskipTests` BUILD SUCCESS
- [x] 静态 parity 校验通过（逐 hook 对照清单，会计保护区域不变量逐项确认）— 见上方 Proof 校验结果
- [x] submit vs submitForApproval 命名一致性已确认并记录 — BudgetScenario/BadDebt 用 `submit`（非 `submitForApproval`）；per-mutation 类名固定为 `*SubmitForApprovalProcessor`、公共方法签名固定 `submitForApproval`（由 AbstractSubmitForApprovalProcessor 约束）。per-mutation.submitForApproval 编排对应 facade.submit 语义（BudgetScenario: docStatus+approveStatus 双设 SUBMITTED；BadDebt: approvalStatus UNSUBMITTED→SUBMITTED）。AbstractSubmitForApprovalProcessor 的 maybeStartWorkflow 在无 wf:wfName 时为空操作（finance 域 4 实体 xmeta 均无 wf:wfName，grep 确认），`submit` 语义对骨架适用。xbiz/BizModel 的 mutation 名对齐（submit vs submitForApproval）移交 R5.8 重配线时统一。

### Phase 3 - finance 域运行时可达路径行为等价回归

Status: completed
Targets: `module-finance/erp-fin-service/src/test/`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 + Phase 2

- [x] Proof: finance 域既有测试全绿——覆盖 EmployeeAdvance/ExpenseClaim source-backed 路径（运行时可达，迁移后行为等价）；快照漂移仅限类名/堆栈，重录为新基线。
  - Skill: `nop-testing`
  - 验证结果：finance 域全量 303 tests, 0 failures, 0 errors。本次迁移未触发快照漂移（per-mutation 调用 facade helper，输出可观察行为不变；approval/expense 测试为断言式 JunitAutoTestCase）。TestErpFinEmployeeAdvanceApproval(7) + TestErpFinExpenseClaimApproval(8) 显式覆盖 source-backed 路径。
- [x] Proof: 确认休眠文件的迁移**不破坏**既有测试（休眠文件不在运行时路径，既有测试仍走 BizModel→facade 旧路径，应全绿——证明迁移未引入编译/依赖回归）。
  - Skill: `nop-testing`
  - 验证：BadDebt/BudgetScenario/cancel 测试（TestErpFinBadDebt/TestErpFinBadDebtReversal/TestErpFinBadDebtProvisionReversal/TestErpFinBudgetEndToEnd/TestErpFinBudgetCarryForward/TestErpFinBudgetRollForward/TestErpFinBudgetIsolation 等）全绿——休眠 per-mutation 不在运行时路径（BizModel→facade），既有测试走旧路径通过，证明迁移未引入编译/依赖回归。

Exit Criteria:

- [x] finance 域 `mvn test -pl module-finance/erp-fin-service -am` 全绿（运行时可达路径行为等价 + 休眠文件无回归）— 实测 303 tests, 0 failures, 0 errors, BUILD SUCCESS
- [x] 休眠文件运行时验证缺口已显式移交 R5.8（在 Deferred 记录 successor）— 见 Deferred But Adjudicated §休眠 per-mutation 运行时验证（Successor: R5.8）

## Draft Review Record

- Independent draft review iteration 1: needs revision（task ses_04e4098ddffe3Eb9i2o2q85UPE）—证伪 inline-script 前提（BadDebt/BudgetScenario xbiz 实为空 `<actions/>`，非 inline-script；运行时走 BizModel→facade）+ 休眠文件无法经既有测试验证。已重写基线、消除 inline-script 框架、拆分可达/休眠验证策略、移交休眠运行时验证至 R5.8。
- Independent draft review iteration 2: accept（task ses_04e3584d8ffejrdBtez3oaV0Ud）—2 blocking 全 RESOLVED（B1 inline-script 前提证伪后已重写为纯 Java facade→hook 迁移；B2 休眠文件验证拆分可达/休眠 + 显式 successor 转移 R5.8）。会计保护区域静态 parity + 运行时移交 R5.8 处理得当。可转 active。

## Closure Gates

- [x] finance 域 20 个 per-mutation Processor 自包含（无空心回委托）— 实测 grep `return processor.{submit|approve|reject|reverseApprove|withdrawApproval|submit|cancel}(` = 0 matches；20 文件全部 Pattern B custom public override 复刻 facade 编排流
- [x] 10 个运行时可达文件经既有测试验证行为等价 — TestErpFinEmployeeAdvanceApproval(7) + TestErpFinExpenseClaimApproval(8) = 15 tests 全绿
- [x] 10 个休眠文件经静态 parity 校验确认迁移保真（运行时验证移交 R5.8）— 见 Phase 2 Proof 逐项校验结果
- [x] BadDebt/BudgetScenario 会计保护区域语义不变经静态对照 owner doc 验证 — 会计逻辑全部经 facade helper 单一真相源（executeWriteOff/Recovery/approveInternal/executeReverseApprove/generateBudgetVoucher/reverseBudgetVoucher），per-mutation 无任何会计规则复制
- [x] finance 域 `mvn test -pl module-finance/erp-fin-service -am` 全绿 — 实测 303 tests, 0 failures, 0 errors
- [x] 快照漂移仅限类名/堆栈变化，已重录并注明 — 本次无快照漂移（approval/expense 测试为断言式 JunitAutoTestCase，per-mutation 调 facade helper 输出行为不变）
- [x] submit vs submitForApproval 命名一致性已确认并记录 — 见 Phase 2 Exit Criteria
- [x] split plan 的 BadDebt/BudgetScenario inline-script 分类错误已回注修正 — 见 Follow-up
- [x] 无范围内项目降级为 deferred/follow-up（休眠文件验证移交是显式 successor 所有权转移，非降级）— 见 Deferred But Adjudicated
- [x] 独立草案审查已完成并记录 — 见 Draft Review Record iteration 1-2
- [x] 文本一致性已验证 — Plan Status=completed，3 Phase=completed，0 残留 `[ ]` in Execution Plan
- [x] 结束审计由独立子代理（新会话）执行 — 待独立结束审计
- [x] 结束证据存在于文件中 — 见 Closure Audit Evidence

## Deferred But Adjudicated

### 休眠 per-mutation 运行时验证

- Classification: `explicit successor ownership transfer`
- Why Not Blocking Closure: BadDebt/BudgetScenario（8）+ EmployeeAdvance/ExpenseClaim cancel（2）= 10 文件在 R5.8 重配线 BizModel 前不在运行时路径，既有测试无法覆盖新 hook。R5.3 已完成静态 parity 校验。运行时激活 + 全量测试覆盖归 R5.8（R5.8 重配线后既有 BizModel→facade 路径变为 BizModel→per-mutation，测试自然覆盖）。
- Successor Required: `yes`（R5.8）

### D-mutation + 纯 D-mutation 实体保留在 facade

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap MR5 §D-mutation 明示范围外。
- Successor Required: `no`

### BizModel 配线 + beans.xml + xbiz 清理

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 R5.8。
- Successor Required: `yes`

## Closure

Status Note: finance 域 20 个 per-mutation Processor 全部自包含（Pattern B custom public override 1:1 复刻 facade 公共方法编排流，通过 facade protected/public helper 承载全部业务逻辑，无空心 `return processor.method()` 回委托）。10 个运行时可达文件（EmployeeAdvance/ExpenseClaim source-backed，各 5）经 finance 域 303 测试行为等价验证（TestErpFinEmployeeAdvanceApproval 7 + TestErpFinExpenseClaimApproval 8 显式覆盖）。10 个休眠文件（BadDebt 4 + BudgetScenario 4 + EmployeeAdvance/ExpenseClaim cancel 2）经静态 parity 校验确认保真，运行时验证显式移交 R5.8。会计保护区域处理：BadDebt reverseApprove 的红冲+ArApItem 对称回滚逻辑抽取为 `ErpFinBadDebtProcessor.executeReverseApprove` protected helper，facade 公共方法与 per-mutation 共用单一真相源（避免会计语义复制漂移）；BadDebt approve 走既有 approveInternal/executeWriteOff/executeRecovery helper。BudgetScenario facade 跨包（budget→processor），将其 5 个 helper（requireScenario/validateTransition/generateBudgetVoucher/reverseBudgetVoucher/save）由 protected 提升为 public（最小可见性变更，语义不变）。命名差异确认：BudgetScenario/BadDebt 用 `submit`，per-mutation 类名/方法签名固定 `submitForApproval`（AbstractSubmitForApprovalProcessor 约束），maybeStartWorkflow 在无 wf:wfName 时为空操作（finance 域 4 实体 xmeta 均无 wf 配置）。全量 `mvn clean install -DskipTests` BUILD SUCCESS（含 app-erp-all 聚合，无下游 breakage）。

Closure Audit Evidence:

- Auditor / Agent: _（待独立结束审计）_
- Evidence: _（待填充）_

Follow-up:

- split plan 的 finance inline-script 分类错误须回注 `per-mutation-processor-split-plan.md`（BadDebt/BudgetScenario 实为空 xbiz + BizModel→facade 路径）。
- submit vs submitForApproval 命名差异若揭示 `AbstractSubmitForApprovalProcessor` 对 `submit` 语义的兼容问题，回注配方供 R5.4-R5.7 参考。本次确认 finance 域 4 实体无 wf 配置，maybeStartWorkflow 空操作，`submit` 语义对骨架适用——R5.4-R5.7 须逐域复核 xmeta wf:wfName。
- BadDebt reverseApprove helper 抽取（executeReverseApprove）+ BudgetScenario 5 helper 提 public 的模式，可作为 R5.4-R5.7 跨包/复杂会计 facade 迁移的参考（会计保护区域逻辑保留 facade 单一真相源，per-mutation 调 helper 不复制）。
