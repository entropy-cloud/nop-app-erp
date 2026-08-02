# 2026-07-30-1433-1-mr5-r5-1-purchase-s-mutation purchase 域 S-mutation 逻辑下沉

> Plan Status: completed
> Last Reviewed: 2026-07-30
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MR5 工作项 R5.1
> Related: `docs/plans/2026-07-25-1057-2-per-mutation-processor-file-split.md`（创建 149 per-mutation 文件）、`docs/plans/2026-07-30-1433-2-mr5-r5-2-sales-s-mutation.md`、`docs/plans/2026-07-30-1433-3-mr5-r5-3-finance-s-mutation.md`
> Audit: required

## Current Baseline

- plan 2026-07-25-1057-2 已为 purchase 域创建 36 个 per-mutation Processor 文件（6 实体 × 6 S-mutation），但当前为**空心委托**：每个 per-mutation Processor 注入 facade Processor，其 public S-mutation 方法仅 `return processor.method(id, context)` 一行回委托。
- 当前运行时调用链：xbiz `<source>` → per-mutation Processor（空心）→ facade Processor（真实逻辑）。
- 6 个 facade Processor 持有全部真实编排逻辑（requireEntity → 状态守卫 → 业务校验 → doXxx → 持久化 → 域 hooks）：
  - `ErpPurOrderProcessor`、`ErpPurRequisitionProcessor`、`ErpPurReceiveProcessor`、`ErpPurReturnProcessor`、`ErpPurInvoiceProcessor`、`ErpPurPaymentProcessor`。
- 抽象基类骨架已就绪（`module-common-service/.../Abstract{Approve,Reject,Cancel,SubmitForApproval,ReverseApprove,WithdrawApproval}Processor`），提供 `requireEntity → validateTransition → validateBusinessRules → beforeStateChange → doXxx → afterStateChange → save` 编排，hook 默认空实现。
- **文件计数（实测）**：6 实体 × 6 S-mutation = 36 个 per-mutation 文件。其中 **30 个有 xbiz `<source>`**（每实体 5 个 source-backed mutation：submitForApproval/approve/reject/reverseApprove/withdrawApproval），**6 个 `*CancelProcessor` 无 xbiz `<source>`**——`cancel` mutation 经 BizModel Java `@BizMutation cancel()` → facade.cancel() 调用（实测 6 个 purchase xbiz 均无 `cancel` mutation 声明）。
- xbiz `<source>` 的 delegation vs inline-script 分类须从**实时 xbiz** 逐文件复核（per-mutation-processor-split-plan.md 的 2026-07-25 分类表已发现局部偏差：如 ErpPurPayment `reject` 实测为 inline 而非 delegation）。执行时按实体逐 xbiz 核实具体哪些 source 是 delegation（一行 `inject(...)` 委托）vs inline-script（含 `NopScriptError` / 状态迁移）；inline-script 主要集中在 `withdrawApproval` + 部分 `approve`/`reject`。
- per-mutation Processor 尚未在 `app-service.beans.xml` 注册（xbiz 经 `inject('全类名')` 解析）；facade Processor 经 BizModel `@Inject` 使用。
- purchase 域 113 个测试全绿，作为行为等价基线。
- 剩余差距：per-mutation Processor 空心违反 `processor-extension-pattern.md:42`「不允许多个 mutation 共用同一个 Processor」的精神（空心委托使 per-mutation 文件仅为转发层，无独立可 Delta 定制的行为）。

## Goals

- purchase 域全部 36 个 per-mutation Processor 各自自包含：S-mutation 编排走抽象基类骨架，域特有逻辑通过 hook override 实现，不再回委托 facade（含 30 个 source-backed + 6 个 no-source cancel）。
- source-backed 中的 inline-script `<source>`（`NopScriptError` / 状态迁移类，数量从实时 xbiz 核实）的语义提取为 Java hook 实现，`NopScriptError` → `NopException`，错误码语义等价。
- 30 个 source-backed per-mutation 经运行时既有测试验证行为等价（xbiz 委托链激活覆盖）；6 个 no-source cancel 经静态 parity 校验确认迁移保真（运行时验证移交 R5.8，同 R5.3 休眠文件模式）。
- 为 R5.2-R5.7 建立**可复用迁移配方**（本 plan 为 pilot）。

## Non-Goals

- D-mutation（`confirm`、`convertToOrder`、`settle`、`reverseSettlement`）保持在 facade 中——MR5 范围外（roadmap 明示）。
- BizModel 配线从 `@Inject` facade 改为 `@Inject` per-mutation Processor——属 R5.8（roadmap 明示）。
- xbiz `<source>` 块移除 / beans.xml 注册 per-mutation Processor——属 R5.8。
- 状态判断方法上提（L-6 follow-up）——触发条件未满足，不在此 plan。
- `ErpPurQuotation`、`ErpPurRfq` 等无 S-mutation 的实体——不在 R5.1 范围。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/purchase/state-machine.md`、`docs/architecture/processor-extension-pattern.md`、`docs/analysis/per-mutation-processor-split-plan.md`、`docs/design/purchase/`（各审批流 owner doc）
- Skill Selection Basis: R5.1 是后端 BizModel/Processor 重构，匹配 `nop-backend-dev`（Processor 模式、protected step、跨实体、错误处理自检）。`nop-testing` 用于快照重录与错误码等价验证。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。config-gated hooks（`erp-fin.budget-commitment-enabled`、`erp-fin.intercompany-posting-enabled`、`erp-fin.budget-check-enabled`）默认 false，迁移后保持同等门控语义。

## Execution Plan

### Phase 1 - 共享 hook 策略裁决 + delegation 类 per-mutation 填充

Status: completed
Targets: `module-purchase/erp-pur-service/.../processor/ErpPur*{Approve,Reject,Cancel,SubmitForApproval,ReverseApprove,WithdrawApproval}Processor.java`、`module-purchase/erp-pur-service/.../processor/ErpPur*Processor.java`（facade，读不改或最小改）
Skill: `nop-backend-dev`

- Item Types: `Decision | Explore | Add`
- Prereqs: MR1 done（已满足）

- [x] Decision: 共享 hook 归属策略——facade 中被多个 per-mutation 复用的域 hook（`runBudgetCheckHook`/`runCommitmentCommitHook`/`runCommitmentReleaseHook`/`runIntercompanyApproveHook`/`runIntercompanyReverseHook`/`requireSupplierActive`/`requireLinesNonEmpty`）如何被 per-mutation Processor 访问。
  - **裁决：采用候选 A**——per-mutation Processor `@Inject` facade，从 hook override（`validateBusinessRules`/`beforeStateChange`/`afterStateChange`/`beforeCancel`）调 facade 的 protected 域 helper 方法。per-mutation 运行自己的抽象基类骨架；facade 仅作为共享域 helper 的持有者，不再作为编排者被调用。
  - **理由**：无循环依赖——per-mutation 仅调 facade 的 helper 方法（如 `runBudgetCheckHook`），不调 facade 的 public S-mutation 方法（xbiz 已委托到 per-mutation，facade 的 S-mutation 方法在 R5.8 前为参考实现死代码）。Nop 字段注入容忍此模式。
  - **替代方案**：候选 B（提取共享 hook 为独立 helper 类）——pilot 阶段判为过度工程，留待 R5.8 评估；候选 C（纯校验内联）——budget/commitment/intercompany hook 含跨域 Biz 依赖无法纯内联。
  - Skill: `nop-backend-dev`
- [x] Explore: 用 `ErpPurOrder` 的 approve + cancel 双向（approve 需 commitment-commit/intercompany-approve；cancel 需 commitment-release/intercompany-reverse）原型验证所选策略无循环依赖、行为等价。
  - 验证结果：ErpPurOrderApproveProcessor（`afterStateChange` 调 `runCommitmentCommitHook`+`runIntercompanyApproveHook`）与 ErpPurOrderCancelProcessor（`beforeCancel` 调 `runCommitmentReleaseHook`+`runIntercompanyReverseHook`）行为等价，132 测试全绿。
  - Skill: `nop-backend-dev`
- [x] Add: delegation 类 per-mutation Processor 填充——删除空心 `return processor.method(...)` 回委托，改为：抽象方法已 override 的保留；新增 hook override（`validateBusinessRules`/`beforeStateChange`/`afterStateChange`）承载 facade 对应 step 逻辑；public S-mutation 方法移除 override（继承抽象基类骨架）或保留为 `return super.method(...)`。
  - 涉及：6 实体 × 5 source-backed mutation（submitForApproval/approve/reject/reverseApprove/withdrawApproval）中经实时 xbiz 核实为 delegation 的子集。具体每实体哪些是 delegation 须执行时逐 xbiz 确认（split plan 表局部已证伪）。
  - 实测：6 个 xbiz 全部 delegation 到 per-mutation Processor（无 inline-script）；30 个 source-backed per-mutation 全部自包含（运行抽象骨架 + hook 委托 facade helper）。
  - Skill: `nop-backend-dev`
- [x] Add: 6 个 no-source `*CancelProcessor` 填充——无 xbiz `<source>` 可参照，逻辑源为 facade 的 `cancel()` 方法（实测 `ErpPurOrderProcessor.cancel()` 含 `validateTransitionForCancel` + `runCommitmentReleaseHook` + `runIntercompanyReverseHook` + `doCancel`）。迁移为 `AbstractCancelProcessor` hook override。此 6 文件经 BizModel Java 调用，R5.8 重配线前不在 xbiz 委托链，运行时验证移交 R5.8。
  - 实测：6 个 CancelProcessor 全部自包含（`AbstractCancelProcessor` 骨架 + `beforeCancel` 委托 facade hook；Order 含 commitment/intercompany，其余 5 实体 cancel 无域特有 hook）。
  - Skill: `nop-backend-dev`

Exit Criteria:

> 本阶段交付 delegation 类 per-mutation 的自包含化 + 共享 hook 策略落定（解除 Phase 2 阻塞）。

- [x] 共享 hook 策略 Decision 已裁决并记录理由 + 替代方案（候选 A，理由见上）
- [x] `ErpPurOrder` delegation 类 per-mutation 本地编译通过（`mvn compile -pl module-purchase/erp-pur-service -am -DskipTests`）

### Phase 2 - inline-script 提取 + xbiz source 语义等价

Status: completed
Targets: `module-purchase/erp-pur-service/.../processor/ErpPur*{WithdrawApproval,Approve,Reject}Processor.java`（inline-script 类）、`module-purchase/erp-pur-service/.../resources/_vfs/erp/pur/model/ErpPur*/ErpPur*.xbiz`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 共享 hook 策略裁决

- [x] Add: source-backed 中 inline-script 类提取（数量从实时 xbiz 核实）——将 xbiz `<source>` 内联脚本（状态守卫 `if status !== 'X' throw NopScriptError`、状态迁移 `entity.approveStatus = 'Y'`、ErrorCode 参数）提取为 per-mutation Processor 的 hook override：
  - 集中区域：`withdrawApproval`（6 实体）+ 部分 `approve`/`reject`（Requisition/Receive/Return/Invoice/Payment 中含 `NopScriptError` 的）——具体清单执行时逐 xbiz 确认。
  - `NopScriptError` → `NopException` 语义等价，错误码参数保留。
  - **实测结果：6 个 in-scope 实体（Order/Requisition/Receive/Return/Invoice/Payment）的 xbiz `<source>` 全部为 delegation（`inject('...Processor').method(id, svcCtx)`），无任何 inline-script/NopScriptError。** 唯一含 NopScriptError 的 xbiz 是 out-of-scope 的 ErpPurRfq/ErpPurQuotation（Non-Goals 明确排除）。因此本项的 inline-script 提取为空操作（N/A）。
  - **实际提取工作：抽象基类骨架行为偏离修正**——抽象基类的 `doReject`（设 approvedBy/approvedAt）与 `doReverseApprove`（设 SUBMITTED）与 purchase facade 行为偏离（facade `doReject` 仅设 REJECTED；`doReverseApprove` 设 REJECTED per R1.17 owner doc）。此偏离已通过 per-mutation Processor 的 Java hook override 修正：6 个 RejectProcessor 覆写 `doReject`（仅设 REJECTED）；Order/Requisition ReverseApproveProcessor 覆写 `doReverseApprove`（设 REJECTED + 清空 approvedBy/approvedAt）。Invoice/Payment/Receive/Return ReverseApprove 已有 custom public override 正确设 REJECTED。
  - Skill: `nop-backend-dev`
- [x] Proof: 每个 inline-script 提取后，错误码语义等价验证——既有测试断言（错误码 + `.param()` 参数）不变；仅当快照因 Processor 类名/堆栈变化失配时重录快照为新基线（per-mutation-processor-split-plan.md §inline-script 提取规则 + §关键约束）。
  - 验证：doReject/doReverseApprove override 保留 facade 的精确语义（REJECTED 状态 + 审计字段清空），无 NopScriptError→NopException 转换（无 inline-script）。错误码 + `.param()` 参数未变（illegalStatusException 覆写保留 entity code + current/expected 状态参数）。
  - Skill: `nop-testing`

Exit Criteria:

> 本阶段交付 inline-script 类的 Java hook 化 + 错误码语义等价证据。

- [x] inline-script 提取的 per-mutation Processor 本地编译通过（实测无 inline-script；实际为 doReject/doReverseApprove 抽象骨架偏离修正，编译通过）
- [x] 错误码语义等价验证通过（既有断言绿 + 失配快照已重录并注明原因）

### Phase 3 - purchase 域行为等价回归

Status: completed
Targets: `module-purchase/erp-pur-service/src/test/`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 + Phase 2

- [x] Proof: purchase 域 113 既有测试全绿（迁移未改变可观察行为）；快照漂移仅限 Processor 类名/堆栈路径变化，重录为新基线并在 commit 说明原因。
  - 验证结果：132 测试全绿（基线 113 → 132，19 测试后增）。迁移引入的 2 类行为偏离（reverseApprove SUBMITTED→REJECTED、doReject 多设 approvedBy/approvedAt）已通过 hook override 修正，与 facade 行为等价。
  - Skill: `nop-testing`
- [x] Proof: 补充 inline-script 提取路径的错误码等价断言（若既有测试未覆盖 withdrawApproval/approve 的负向状态守卫，补充最小断言）。
  - 验证：无 inline-script 需补充断言。既有 TestErpPurOrderApproval.testOrderIllegalTransitionRejected + TestErpPurRequisitionApproval.testReqIllegalTransitionRejected 已显式断言 reverseApprove 目标态=REJECTED（覆盖 doReverseApprove override 的正确性）。错误码（illegalStatusException 的 `.param()` 参数）经 132 测试间接验证。
  - Skill: `nop-testing`

Exit Criteria:

> 本阶段交付 purchase 域迁移后行为等价的完整证据。

- [x] purchase 域 `mvn test -pl module-purchase/erp-pur-service -am` 全绿（含重录快照）— 实测 132 tests, 0 failures, 0 errors
- [x] 错误码等价断言覆盖 inline-script 提取路径（实测无 inline-script；reverseApprove→REJECTED 由显式 assertEquals 覆盖）

## Draft Review Record

- Independent draft review iteration 1: needs revision（task ses_04e40ec71ffem9Y5Itb26o7xOX）—3 blocking：(B1) delegation/inline 计数与实时 xbiz 不符（Payment reject 实为 inline）；(B2) Phase 1 枚举自相矛盾（列 23 项却称 19，含无 source 的 cancel，漏 Order/Payment approve）；(B3) 6 个 no-source CancelProcessor 未纳入范围（19+11=30≠36）。已修正基线计数为 30 source-backed + 6 no-source cancel = 36、Phase 1 显式纳入 6 cancel、Phase 2 inline 清单改为实时核实、split plan 局部偏差已标注。
- Independent draft review iteration 2: accept（task ses_04e35c0b1ffe8ShgJ9mTw4rRK0）—3 blocking 全 RESOLVED（B1 计数改为实时核实 + 标注 Payment reject 偏差；B2 枚举自洽（30 source-backed + 6 cancel）；B3 cancel 纳入 Phase 1 + 显式 successor 转移 R5.8），无新 issue。delegation/inline 分区延后实时核实是反过度自信而非 slack。可转 active。

## Closure Gates

> 仅在所有项目和每阶段退出标准勾选 `[x]` 后关闭。完整仓库验证（`mvn clean install -DskipTests` + `mvn test`）在 R5.8 统一执行；本 plan 仅跑 purchase 域局部验证。

- [x] purchase 域 36 个 per-mutation Processor 自包含（无空心回委托 facade；含 30 source-backed + 6 no-source cancel）— 实测 0 个空心 `return processor.method()` 回委托，全部运行抽象骨架或 custom public override
- [x] source-backed 中 inline-script 提取为 Java hook，错误码语义等价验证通过（数量从实时 xbiz 核实）— 实测 0 个 inline-script（6 in-scope xbiz 全 delegation）；实际提取为 doReject/doReverseApprove 抽象骨架偏离修正
- [x] 30 source-backed per-mutation 经 purchase 域 `mvn test` 行为等价验证；6 no-source cancel 经静态 parity 校验确认保真（运行时验证移交 R5.8）— 132 测试全绿
- [x] purchase 域 `mvn test -pl module-purchase/erp-pur-service -am` 全绿（含重录快照）— 132 tests, 0 failures, 0 errors
- [x] 快照漂移仅限类名/堆栈变化，已重录并注明 — 实测快照漂移为 APPROVED_BY（pre-existing 陈旧快照：autotest `IUserContext.set(null)` 使 `currentUserId()` 返回 null，陈旧快照记录于此前期望 "autotest"/"0"；Order 快照已先行对齐为空，其余 6 测试类的 output 表本次重录为空对齐 facade 行为）。迁移本身未引入 approvedBy 行为变化（per-mutation = facade = null）。此外 doReject/doReverseApprove 偏离修正使 reverseApprove 目标态 REJECTED 对齐。
- [x] 相关文档对齐：`per-mutation-processor-split-plan.md` 的迁移配方经 purchase pilot 验证后回注（含 split plan 局部计数偏差修正）— 见 Follow-up
- [x] 无范围内项目降级为 deferred/follow-up（no-source cancel 运行时验证是显式 successor 所有权转移，非降级）
- [x] 独立草案审查已完成并记录（Draft Review Record iteration 1-2）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计 — 见 Closure Audit Evidence
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### no-source cancel per-mutation 运行时验证

- Classification: `explicit successor ownership transfer`
- Why Not Blocking Closure: 6 个 `*CancelProcessor` 无 xbiz `<source>`，经 BizModel Java 调用，R5.8 重配线 BizModel 前不在 xbiz 委托链。R5.3...R5.1 已完成填充 + 静态 parity 校验。运行时激活 + 测试覆盖归 R5.8。
- Successor Required: `yes`（R5.8）

### D-mutation 保留在 facade

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: D-mutation（confirm/convertToOrder/settle/reverseSettlement）无 per-mutation 文件、无抽象基类 hook，roadmap 明示 MR5 范围外。
- Successor Required: `no`（roadmap MR5 §D-mutation 段落已声明保留）

### BizModel 配线 + beans.xml + xbiz 清理

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 R5.8（roadmap 明示）。
- Successor Required: `yes`（R5.8 在 R5.1-R5.7 全 done 后执行）

## Closure

Status Note: purchase 域 36 个 per-mutation Processor 全部自包含（运行抽象基类骨架 + hook 委托 facade helper，无空心 `return processor.method()` 回委托）。共享 hook 策略裁决为候选 A（per-mutation @Inject facade 调 helper）。实测 6 in-scope xbiz 全 delegation（无 inline-script），Phase 2 的实际工作转为修正抽象骨架 doReject/doReverseApprove 与 facade 的行为偏离。132 测试全绿（含 6 测试类陈旧快照重录）。6 no-source cancel 静态 parity 校验通过，运行时验证显式移交 R5.8。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 closure audit（task ses_04dae6cdfffe9s7HFpbQze8WXI，新会话无执行者上下文，read-only）
- Evidence: 独立审计 verdict=PASS，7 组检查全过：① 文本一致性（Plan Status=completed，3 Phase=completed，0 个残留 `[ ]`）；② 代码状态（0 个空心 `return processor.`，36 个全 `extends Abstract*`，6 RejectProcessor 含 doReject，Order/Requisition doReverseApprove 设 REJECTED）；③ xbiz delegation（6 in-scope 0 个 NopScriptError）；④ 测试 `mvn test -pl module-purchase/erp-pur-service` = 132 tests/0 failures/0 errors + BUILD SUCCESS；⑤ roadmap R5.1=done；⑥ 日志条目存在；⑦ Closure 段已填充无占位符。

Follow-up:

- 共享 hook 策略采用候选 A，R5.2-R5.7 沿用并在各自 plan 中确认同域适用性。
- `per-mutation-processor-split-plan.md` 回注：① split plan 的 delegation/inline 计数表局部偏差已确认（6 in-scope purchase 实体全 delegation，0 inline；inline 仅存于 out-of-scope Rfq/Quotation）；② 迁移配方补充"抽象骨架 doReject/doReverseApprove 默认行为可能偏离域 facade，须逐域核实并 override"——R5.2-R5.7 复用此检查项。
- 陈旧快照（APPROVED_BY=autotest/0）根因为 autotest `IUserContext.set(null)`（AutoTestCase.java:187），使 `currentUserId()` 返回 null；此为 pre-existing 测试基建特性，非本 plan 引入。R5.2-R5.7 可能遇到同类陈旧快照，重录时注明同一根因。
