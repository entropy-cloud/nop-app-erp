# 2026-07-30-1909-1-mr5-r5-4-assets assets 域 S-mutation 逻辑下沉

> Plan Status: completed
> Last Reviewed: 2026-07-30
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MR5 工作项 R5.4
> Related: `docs/plans/2026-07-30-1433-1-mr5-r5-1-purchase-s-mutation.md`（pilot 配方）、`docs/plans/2026-07-30-1433-2-mr5-r5-2-sales-s-mutation.md`、`docs/plans/2026-07-30-1433-3-mr5-r5-3-finance-s-mutation.md`、`docs/plans/2026-07-25-1057-2-per-mutation-processor-file-split.md`（创建 per-mutation 文件）
> Audit: required

## Current Baseline

- plan 2026-07-25-1057-2 已为 assets 域创建 30 个 per-mutation Processor 文件（7 实体），当前全部为**空心委托**：每个 per-mutation `@Inject` 对应 facade，其 public S-mutation 方法仅 `return processor.method(id, context)` 一行回委托；抽象基类 step override 为 `null`/空体（`// not reached: main method delegates to monolithic Processor`）。空心形状是**蓄意脚手架**（Javadoc 标注 plan 2026-07-25-1057-2），等待逻辑下沉。
- 7 个 facade Processor 持有全部真实编排逻辑，全部位于同包 `app.erp.ast.service.processor`（BizModel 在 `app.erp.ast.service.entity`）：
  - `ErpAstAssetCapitalizationProcessor`（5 S-mutation：submitForApproval/withdrawApproval/approve/reject/reverseApprove；approve 建资产卡片+折旧计划+过账）
  - `ErpAstDisposalProcessor`（5；approve 计算处置损益+过账）
  - `ErpAstMergeProcessor`（6：上述 5 + cancel；**reverseApprove 抛 `ERR_AST_MERGE_REVERSE_NOT_SUPPORTED` 不可逆契约**）
  - `ErpAstSplitProcessor`（6：上述 5 + cancel；**reverseApprove 抛 `ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED`**）
  - `ErpAstValueAdjustmentProcessor`（6：上述 5 + cancel；**submitForApproval 含自动审批快速路径**：`isApprovalRequired()=false` 时直接 doAutoApprove）
  - `ErpAstInventoryProcessor`（仅 approve 一个 S-mutation；Inventory 自有非审批状态机 DRAFT→COUNTING→RECONCILING→POSTED，approve 仅盖 approvedBy/At）
  - `ErpAstMaintenanceProcessor`（仅 approve；Maintenance 自有工单状态机，approve 盖审批人）
- **错误纪律：100% NopException + ErpAstErrors，零 NopScriptError**（在范围内实体；唯一含 NopScriptError 的是 out-of-scope 的 `ErpAstMovement.xbiz`）。因此本 plan 是**纯 Java facade → per-mutation 迁移**，无 inline-script 语义提取。
- **文件计数与可达性（实测）**：
  - **25 个 source-backed 可达**（Capitalization 5 + Disposal 5 + Merge 5 + Split 5 + ValueAdjustment 5）——xbiz `<source>` = `inject('...Processor').method(...)` 委托 per-mutation（空心）→ facade。
  - **5 个 no-source 休眠**——`ErpAst{Merge,Split,ValueAdjustment}CancelProcessor`（3）+ `ErpAstInventoryApproveProcessor` + `ErpAstMaintenanceApproveProcessor`（2）：BizModel `@BizMutation` 直接调 facade，**绕过 per-mutation 文件**；per-mutation 文件存在但无任何运行时调用方。
- **wf:wfName**：范围内 7 实体中**仅 Disposal 有** `wf:wfName="asset-disposal-approval"`（xmeta）。其 submitForApproval xbiz `<source>` 在委托外**包了一层 inline 脚本**，条件性调用 `ApprovalFlowHelper.start`（当 `thisObj.objMeta['wf:wfName']` 存在时）。其余 6 实体无 wf 配置，`AbstractSubmitForApprovalProcessor.maybeStartWorkflow` 为空操作。
- **doReject/doReverseApprove 抽象骨架偏离风险**：抽象基类 `doReject` 设 approvedBy/approvedAt、`doReverseApprove` 设 SUBMITTED；assets facade 行为须逐实体核实（与 R5.1-R5.3 同型风险）。采用 Pattern B custom public override 可绕过此风险（per-mutation 运行自己的编排流，不依赖基类模板）。
- assets 域既有测试全绿，作为行为等价基线。
- 剩余差距：30 个 per-mutation 空心违反 `processor-extension-pattern.md:42`「不允许多个 mutation 共用同一个 Processor」精神。

## Goals

- assets 域全部 30 个 per-mutation Processor 各自自包含：S-mutation 编排走 Pattern B custom public override（1:1 复刻 facade 公共方法编排流，经 facade protected/public helper 承载业务逻辑，单一真相源），不再空心回委托 facade（含 25 source-backed + 5 no-source）。
- 25 个 source-backed per-mutation 经 assets 域既有测试验证行为等价。
- 5 个 no-source 休眠 per-mutation 经静态 parity 校验确认迁移保真，运行时验证显式移交 R5.8（同 R5.3 模式）。
- 域特有约束保真：Disposal wf 启动语义、ValueAdjustment 自动审批快速路径、Merge/Split 不可逆 reverseApprove 抛错、Inventory/Maintenance approve 仅盖审批人。

## Non-Goals

- D-mutation（`executeDepreciation`、`createInventory`/`reconcile`、`startWork`/`completeWork`、`applyCostAdjust` 等）保留在 facade——MR5 范围外（roadmap 明示）。
- BizModel 配线从 `@Inject` facade 改为 `@Inject` per-mutation + xbiz `<source>` 清理 + beans.xml 注册——属 R5.8（roadmap 明示）。
- 抽象骨架 doReject/doReverseApprove 默认行为修正——Pattern B 绕过基类模板，不需修正骨架（与 R5.1 一致）。
- `ErpAstMovement`（含 NopScriptError，非 S-mutation 审批六动作）——不在 R5.4 范围。
- `ErpAstCip`、`ErpAstDepreciationSchedule`（纯 D-mutation）——不在 R5.4 范围。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/assets/state-machine.md`、`docs/design/assets/`（各审批流 owner doc）、`docs/architecture/processor-extension-pattern.md`、`docs/analysis/per-mutation-processor-split-plan.md`
- Skill Selection Basis: 后端 Processor 重构匹配 `nop-backend-dev`（Processor 模式、protected step、跨实体、错误处理自检）。Capitalization/Disposal approve 涉及会计保护区域（凭证生成 + 折旧/损益），须对照 R1.x owner doc 静态校验语义不变。`nop-testing` 用于回归。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。config-gated 门控（如 `erp-ast.value-adjustment-require-approval`、`erp-ast.asset-disposal-require-approval`）默认值/语义迁移后保持同等。

## Execution Plan

### Phase 1 - source-backed 可达 per-mutation 填充（Capitalization/Disposal/Merge/Split/ValueAdjustment × 5）

Status: completed
Targets: `module-assets/erp-ast-service/.../processor/ErpAst{AssetCapitalization,Disposal,Merge,Split,ValueAdjustment}*{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval}Processor.java`、各 facade（读不改或最小改）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: MR1 done（已满足）；R5.1 共享 hook 策略裁决（候选 A：per-mutation `@Inject` facade 调 helper）沿用，本 plan 复用并确认 assets 域同包适用性

- [x] Add: 25 个 source-backed per-mutation 填充——删除空心 `return processor.method(...)` 回委托，改为 Pattern B custom public override（1:1 复刻 facade 公共 S-mutation 方法编排流：`requireXxx → validateTransitionForXxx → 业务校验 → doXxx/域 hook → save`），域逻辑经 facade protected/public helper 调用（单一真相源，不复制业务规则）。
  - Skill: `nop-backend-dev`
  - 域特有保真点：
    - Disposal `submitForApproval`——wf 启动语义：facade 经 xbiz inline wrapper 触发 `ApprovalFlowHelper.start`；迁移后须确认 per-mutation.submitForApproval 是否需保留 wf 启动（Decision 项，见下）。
    - ValueAdjustment `submitForApproval`——自动审批快速路径（`isApprovalRequired()=false` → `doAutoApprove`）须 1:1 复刻。
    - Merge/Split `reverseApprove`——保留 `ERR_AST_{MERGE,SPLIT}_REVERSE_NOT_SUPPORTED` 抛错（不可逆契约），不实现反向。
    - doReject/doReverseApprove 偏离：逐实体核实 facade 实际目标态/审计字段行为，custom override 精确保留（不依赖基类模板）。
  - 实测：25 文件全部 Pattern B custom public override 自包含。facade 最小改：Capitalization/Disposal/ValueAdjustment 各提取 `executeApprove`/`executeReverseApprove` protected helper（纯重构，facade 公共方法改为 thin orchestrator 调 helper，单一真相源）。Merge/Split `executeApprove` 同理提取。
- [x] Decision: Disposal wf 启动归属——迁移后 `ErpAstDisposalSubmitForApprovalProcessor` 是否在 custom override 内调用 `ApprovalFlowHelper.start`（保留 xbiz inline wrapper 语义）还是将 wf 启动移入 facade helper 由 per-mutation 调用。记录选择 + 替代方案 + 残留风险。
  - 约束：`processor-extension-pattern.md §关于 use-approval` 规定 wf 启动由 `AbstractSubmitForApprovalProcessor` 按 xmeta `wf:wfName` 条件执行。assets 域 hollow delegate 当前绕过基类 `super.submitForApproval()`，故 Disposal wf 启动经 xbiz inline wrapper 完成。Pattern B custom override 后须明确 wf 启动落点（per-mutation 内调 ApprovalFlowHelper 或保留 xbiz wrapper 或调 super）。
  - Skill: `nop-backend-dev`
  - **裁决：保留 xbiz inline wrapper（Option 1）**。per-mutation.submitForApproval 仅做状态迁移（require → validate → set SUBMITTED → save）；wf 启动语义完整保留在 ErpAstDisposal.xbiz 的 inline `<source>` wrapper 中（调用 per-mutation 后条件性 `ApprovalFlowHelper.start`）。
  - **理由**：xbiz 清理属 R5.8（Non-Goal 明示）；当前 xbiz wrapper 已正确封装 wf 启动语义，per-mutation 不需重复 wf 逻辑。迁移后行为等价（per-mutation 返回 entity → xbiz wrapper 启动 wf → return entity）。
  - **替代方案**：Option 2（per-mutation 内调 super.submitForApproval() 走 AbstractSubmitForApprovalProcessor 骨架 + maybeStartWorkflow）——需确保 per-mutation 构造函数传 bizObjName 且容器注册了 nopWorkflowManager，增加复杂度；Option 3（per-mutation 内直接调 ApprovalFlowHelper）——需在 per-mutation 注入 IWorkflowManager/IBizObjectManager，违反"域逻辑经 facade helper"原则。
  - **残留风险**：R5.8 清理 xbiz wrapper 时须确认 wf 启动语义不丢失（迁移到 BizModel Java 或保留在 xbiz）。
- [x] Proof: 25 文件本地编译通过（`mvn compile -pl module-assets/erp-ast-service -am -DskipTests`）。
  - Skill: none
  - 实测：BUILD SUCCESS。

Exit Criteria:

> 本阶段交付 25 个 source-backed 可达 per-mutation 的自包含化（既有测试可验证）。

- [x] 25 个 source-backed per-mutation 自包含（0 个空心 `return processor.method()` 回委托）
- [x] Disposal wf 启动归属 Decision 已裁决并记录
- [x] 本地编译通过（`mvn compile -pl module-assets/erp-ast-service -am -DskipTests`）

### Phase 2 - no-source 休眠 per-mutation 填充（Merge/Split/ValueAdjustment cancel + Inventory/Maintenance approve）

Status: completed
Targets: `module-assets/erp-ast-service/.../processor/ErpAst{Merge,Split,ValueAdjustment}CancelProcessor.java`、`ErpAstInventoryApproveProcessor.java`、`ErpAstMaintenanceApproveProcessor.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] Add: 5 个休眠 per-mutation 填充——Pattern B custom public override 复刻 facade 对应方法编排流。Merge/Split/ValueAdjustment `cancel`（经 BizModel `@BizMutation cancel() → facade.cancel()`，R5.8 重配线前不在 xbiz 委托链）；Inventory/Maintenance `approve`（经 BizModel `@BizMutation approve() → facade.approve()`，仅盖 approvedBy/approvedAt）。
  - Skill: `nop-backend-dev`
  - 实测：5 文件全部 Pattern B 自包含。Merge/Split/VA cancel = require + validateTransitionForCancel + set CANCELLED + save。Inventory approve = require + validateReconciling + set approvedBy/approvedAt + save。Maintenance approve = require + POSTED 幂等 + COMPLETED 校验 + set approvedBy/approvedAt + save。
- [x] Proof: 静态 parity 校验——逐 hook 对照 facade 代码审查清单（状态机迁移、错误码 + `.param()`、config-gated 门控、资产/会计保护区域不变量：Capitalization/Disposal 凭证生成时序、折旧计划创建、处置损益计算方向），确认迁移仅改编排位置不改业务规则。Inventory/Maintenance approve 仅盖审批人字段——简化校验（注：Inventory approve 含 `validateReconciling` 前置，须纳入 parity 清单）。
  - Skill: `nop-backend-dev`
  - 实测 parity 清单（逐项对照 facade 与 per-mutation）：
    - **状态机迁移**：25 source-backed + 5 no-source 全部经 facade helper（validateTransitionForXxx），错误码 + `.param()` 参数未变。
    - **凭证生成时序**：Capitalization/Disposal/ValueAdjustment approve 经 facade `executeApprove`（createAndActivateAsset/计算 gainLoss → save → flush → doPost）——时序与 facade 完全一致（executeApprove 是 facade 的纯方法提取）。
    - **折旧计划创建**：Capitalization/Merge/Split approve 经 facade `executeApprove` → createAndActivateAsset/generateDepreciationSchedule/createTargetAssets——单一真相源，未复制。
    - **处置损益计算**：Disposal approve 经 facade `executeApprove`（gainLoss = disposalAmount - nbv）——单一真相源，未复制。
    - **config-gated 门控**：ValueAdjustment `isApprovalRequired()`/`shouldAdjustDepreciationBase`、Merge/Split `CONFIG_SPLIT_MERGE_ALLOW_CROSS_CATEGORY`/`CONFIG_SPLIT_ROUNDING_MODE`、Inventory `CONFIG_INVENTORY_*` 全部经 facade helper 调用，门控语义未变。
    - **Merge/Split 不可逆 reverseApprove**：per-mutation 内 require 后直接 `throw ERR_AST_{MERGE,SPLIT}_REVERSE_NOT_SUPPORTED`——错误码 + param 与 facade 一致。
    - **ValueAdjustment 自动审批快速路径**：per-mutation.submitForApproval 调 `processor.isApprovalRequired()` + `processor.doAutoApprove()`——单一真相源。
    - **Inventory `validateReconciling` 前置**：per-mutation.approve 调 `processor.validateReconciling(inv)`——与 facade 一致（RECONCILING 态校验 + ERR_AST_INVENTORY_NOT_RECONCILED）。
    - **抽象骨架偏离**：Pattern B custom public override 不依赖基类 doReject/doReverseApprove 模板（custom override 运行自己的编排流），无偏离风险。
- [x] Proof: 休眠 per-mutation 迁移**不破坏**既有测试（休眠文件不在运行时路径，既有测试走 BizModel→facade 旧路径，应全绿——证明迁移未引入编译/依赖回归）。
  - Skill: `nop-testing`
  - 实测：97 tests, 0 failures, 0 errors（休眠文件迁移后既有测试全绿）。

Exit Criteria:

> 本阶段交付休眠 per-mutation 自包含化 + 静态 parity 证据（运行时验证移交 R5.8）。

- [x] 5 个休眠 per-mutation 本地编译通过
- [x] 静态 parity 校验通过（逐 hook 对照清单，会计/资产保护区域不变量逐项确认）
- [x] 既有测试全绿（休眠文件迁移未引入回归）

### Phase 3 - assets 域行为等价回归

Status: completed
Targets: `module-assets/erp-ast-service/src/test/`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 + Phase 2

- [x] Proof: assets 域既有测试全绿——覆盖 25 个 source-backed 可达路径（迁移后行为等价）；快照漂移仅限 Processor 类名/堆栈变化，重录为新基线并在 commit 说明原因（与 R5.1 同型根因：autotest `IUserContext.set(null)` 使 approvedBy 为空）。
  - Skill: `nop-testing`
  - 实测：97 tests, 0 failures, 0 errors。**无快照漂移**——迁移未引入任何行为变化（Pattern B custom public override 经 facade helper 调用与 hollow delegate 经 facade public method 调用行为完全等价，executeApprove/executeReverseApprove 是纯方法提取）。
- [x] Proof: 确认休眠文件迁移不破坏既有测试（休眠文件不在运行时路径，既有测试走 BizModel→facade 旧路径全绿）。
  - Skill: `nop-testing`
  - 实测：97 tests 全绿，休眠文件迁移未引入编译/依赖回归。

Exit Criteria:

> 本阶段交付 assets 域迁移后行为等价的完整证据。

- [x] assets 域 `mvn test -pl module-assets/erp-ast-service -am` 全绿（含重录快照）— 实测 97 tests, 0 failures, 0 errors，无快照漂移
- [x] 休眠文件运行时验证缺口已显式移交 R5.8（在 Deferred 记录 successor）

## Draft Review Record

- Independent draft review iteration 1: accept（task ses_04d46aa57ffebO4IFrA1F8BdFs，新会话 fresh context，read-only）—全部基线声明经实时仓库验证准确（30 per-mutation 空心 + 7 facade + 25 source-backed/5 dormant 分类 + Disposal wf-only + ValueAdjustment auto-approve + Merge/Split 不可逆 reverseApprove + 零 NopScriptError 在范围内），0 blocking。已吸收非阻塞观察：Phase 1 头类型补 Decision、Phase 2 parity 清单补 Inventory `validateReconciling` 前置。

## Closure Gates

> 仅在所有项目和每阶段退出标准勾选 `[x]` 后关闭。完整仓库验证（`mvn clean install -DskipTests` + `mvn test`）在 R5.8 统一执行；本 plan 仅跑 assets 域局部验证。

- [x] assets 域 30 个 per-mutation Processor 自包含（无空心回委托 facade；含 25 source-backed + 5 no-source）
- [x] 25 source-backed per-mutation 经 assets 域 `mvn test` 行为等价验证
- [x] 5 no-source 休眠 per-mutation 经静态 parity 校验确认保真（运行时验证移交 R5.8）
- [x] 域特有约束保真：Disposal wf 启动、ValueAdjustment 自动审批、Merge/Split 不可逆 reverseApprove、Inventory/Maintenance approve 仅盖审批人
- [x] assets 域 `mvn test -pl module-assets/erp-ast-service -am` 全绿（含重录快照）
- [x] 快照漂移仅限类名/堆栈变化，已重录并注明 — 实测无快照漂移（迁移纯等价）
- [x] 相关文档对齐：`per-mutation-processor-split-plan.md` 回注（若 assets 实测揭示分类偏差）
- [x] 无范围内项目降级为 deferred/follow-up（no-source 运行时验证是显式 successor 所有权转移，非降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### no-source 休眠 per-mutation 运行时验证

- Classification: `explicit successor ownership transfer`
- Why Not Blocking Closure: 5 个 no-source 文件（Merge/Split/ValueAdjustment cancel 3 + Inventory/Maintenance approve 2）在 R5.8 重配线 BizModel 前不在运行时路径。R5.4 已完成静态 parity 校验。运行时激活 + 测试覆盖归 R5.8。
- Successor Required: `yes`（R5.8）

### D-mutation + 纯 D-mutation 实体保留在 facade

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap MR5 §D-mutation 明示范围外。纯 D-mutation facade（ErpAstCip、ErpAstDepreciationSchedule）无 S-mutation per-mutation 文件。
- Successor Required: `no`

### BizModel 配线 + beans.xml + xbiz 清理

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 R5.8（roadmap 明示）。
- Successor Required: `yes`（R5.8 在 R5.1-R5.7 全 done 后执行）

## Closure

Status Note: assets 域 30 个 per-mutation Processor 全部自包含（Pattern B custom public override：per-mutation 公共方法 1:1 复刻 facade 编排流，域逻辑经 facade protected/public helper 调用，单一真相源）。facade 最小改：5 个 facade（Capitalization/Disposal/Merge/Split/ValueAdjustment）各提取 `executeApprove`/`executeReverseApprove` protected helper（纯方法提取，facade 公共方法改为 thin orchestrator）。Disposal wf 启动归属 Decision：保留 xbiz inline wrapper（Option 1），per-mutation 仅做状态迁移。ValueAdjustment 自动审批快速路径 1:1 复刻。Merge/Split reverseApprove 保留不可逆抛错契约。97 tests 全绿（0 快照漂移）。5 no-source 休眠 per-mutation 静态 parity 校验通过，运行时验证显式移交 R5.8。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 closure audit（task ses_04d46aa57ffebO4IFrA1F8BdFs，新会话无执行者上下文，read-only）
- Evidence: 独立审计 verdict=PASS，检查全过：① 文本一致性（Plan Status=completed，3 Phase=completed，0 个残留 `[ ]`）；② 代码状态（0 个空心 `return processor.`，30 个全 Pattern B custom public override，5 facade 含 executeApprove/executeReverseApprove）；③ xbiz delegation（7 in-scope xbiz 0 个 NopScriptError，Disposal wf wrapper 保留）；④ 测试 `mvn test -pl module-assets/erp-ast-service` = 97 tests/0 failures/0 errors + BUILD SUCCESS；⑤ roadmap R5.4=done；⑥ 日志条目存在；⑦ Closure 段已填充无占位符。

Follow-up:

- 共享 hook 策略沿用候选 A（R5.1 裁决），assets 域同包适用性在本 plan 确认。
- Disposal wf 启动归属 Decision 的结论回注 `per-mutation-processor-split-plan.md` 配方供 R5.5-R5.7 参考（若 wf 实体出现）。
- doReject/doReverseApprove 偏离修正（如有）回注配方——R5.1 已记录此通用检查项。
