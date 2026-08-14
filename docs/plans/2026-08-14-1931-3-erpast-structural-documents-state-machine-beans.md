# 2026-08-14-1931-3-erpast-structural-documents-state-machine-beans 资产域拆分/合并文档双轴状态机 Bean（M4.48 + M4.49 + M4.50 + M4.51）

> Plan Status: active
> Review Hold: §11.2 M4 (i) 人工/owner-doc 门控**已于 2026-08-14 经人工确认解除**（见 Draft Review Record 门控确认记录）——本计划触及受保护资产/业财过账行为（Split approve→结构性资产拆分过账 + 卡片重组、Merge approve→结构性资产合并过账 + 卡片重组；二者均 post-only 无 reverse，reverseApprove 无条件抛 ERR_*_REVERSE_NOT_SUPPORTED 不可逆契约）。M4 plan-first 门控成立且经人工确认；已转 `active` 进入实施。
> Last Reviewed: 2026-08-14
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.48（ErpAstSplit.docStatus）+ M4.49（ErpAstSplit.approveStatus）+ M4.50（ErpAstMerge.docStatus）+ M4.51（ErpAstMerge.approveStatus），均 plan-first；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M4.48-51 行段，320-323 区间）
> Related: M4 采购审批先例 `2026-08-13-1950-1-purchase-m4-approvestatus-state-machine-bean.md`（facade `validateTransitionForXxx` + per-mutation 双路径 + 双轴后缀命名 done）；M3 同域先例 `2026-08-13-0805-2-erpast-movement-state-machine-beans.md`（同域双轴 done）；同批计划 1 `2026-08-14-1931-1-erpast-core-lifecycle-state-machine-beans.md`（assets status 轴基线）、计划 2 `2026-08-14-1931-2-erpast-value-change-documents-state-machine-beans.md`（assets 文档双轴 facade 范式，先执行建立文档双轴接线基线）
> Mission: entity-state-machine
> Work Item: M4.48 + M4.49 + M4.50 + M4.51
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。二文档实体的 approve 动作触发业财过账：Split approve 触发 `AssetSplitPostingDispatcher`（仅 post 路径，**无 reverse**——遵守 owner doc `split-merge.md §关键业务规则 5` 不可逆契约）；Merge approve 触发 `AssetMergePostingDispatcher`（仅 post 路径，无 reverse）。approve 同时执行资产卡片结构性重组（Split: 源资产拆分为多个新卡片；Merge: 多源资产合并为一个卡片）+ Asset.status side-effect。声明 §11.2 M4 硬约束：(i) plan-first + 受保护行为人工/owner-doc 门控；(ii) 过账时序/编排不改，继续由 `AssetSplitPostingDispatcher`/`AssetMergePostingDispatcher` + `posted` 契约管理；(iii) `posted` 不入轴；(iv) **不可逆契约（无 reverse/reverseApprove-撤销）不改**——reverseApprove per-mutation Processor 无条件抛 `ERR_AST_SPLIT/MERGE_REVERSE_NOT_SUPPORTED`（无 posted 窗口、无状态迁移发生，错误更正走资产处置 + 新建流程）；(v) 跨域副作用保留原 Processor 路径。
>
> **规则 14 bundling 声明**：M4.48-51 属同一组件（同一 owner doc `docs/design/assets/split-merge.md`、同一域 `erp-ast`、同一结果表面 = 资产域拆分/合并文档双轴矩阵集中化），按指南规则 14 合并为单计划。二实体（Split/Merge）均 docStatus+approveStatus 双轴、均 5 动作审批生命周期、均经 facade `validateTransitionForXxx` + per-mutation Processor 双路径、均触发结构性过账且**均不可逆**——结构高度同构，按实体分阶段落地。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3 assets`（448 行段）+ 实仓核实。二实体均 **Facade + per-mutation Processor 两层结构**（`processor-extension-pattern.md`），**无共享 Abstract*Processor 骨架**。assets 域既有 1 个 SM Bean（Movement 双轴，M3.15+M3.16 done）。本计划在同批计划 1（status 轴）+ 计划 2（文档双轴 facade 范式）建立的 assets Bean 基线之上追加。

- **ErpAstSplit**（M4.48 docStatus + M4.49 approveStatus，双轴，facade + 6 per-mutation Processor）：
  - **approveStatus 4 态**（`wf/approve-status`）：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED。
  - **docStatus 退化/生命周期**（`erp/doc-status`）：DRAFT/ACTIVE/CANCELLED（CANCELLED 经 cancel Processor 或 useLogicalDelete，待实仓核实）。
  - **writer（facade `ErpAstSplitProcessor` + per-mutation）**：facade 含 `validateTransitionFor{Submit,Withdraw,Approve,Reject,Cancel}`（hardcoded `Objects.equals`，同 Disposal 范式）+ `executeApprove`（写 APPROVED/ACTIVE + 源资产拆分为多卡片 + 比例/金额平衡校验 + `AssetSplitPostingDispatcher.tryPost`）。**reverseApprove = 无条件抛错（不可逆契约）**：facade `reverseApprove` 委托 per-mutation `ErpAstSplitReverseApproveProcessor.reverseApprove:22-26`，后者 `requireSplit` 后**直接 `throw ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED`**（无 posted 判定、无 executeReverseApprove 方法体、短路在 validateTransitionForReverseApprove 之前）。per-mutation `ErpAstSplit{SubmitForApproval,Approve,Reject,WithdrawApproval,Cancel}Processor` 调 facade。
  - **领域错误码**：`ERR_AST_SPLIT_ILLEGAL_STATUS_TRANSITION`（:239-242，approveStatus）+ `ERR_AST_SPLIT_ILLEGAL_DOC_TRANSITION`（:243-246）+ `ERR_AST_SPLIT_SOURCE_NOT_IN_SERVICE`/`PROPORTION_NOT_BALANCED`/`AMOUNT_NOT_BALANCED`/`CROSS_CATEGORY_NOT_ALLOWED`/`ALREADY_POSTED`/`INSUFFICIENT_NET_VALUE`/`NO_LINES`/`TARGET_ASSET_CODE_DUPLICATE`（动态守卫）+ **`ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED`（:279-282，不可逆契约——执行后不可撤销，错误更正走资产处置 + 新建流程）**。
  - **过账**：`AssetSplitPostingDispatcher`（**仅 post 路径，无 reverse**——beans.xml:37-41 注释「遵守 owner doc 不可逆契约」）。
- **ErpAstMerge**（M4.50 docStatus + M4.51 approveStatus，双轴，facade + 6 per-mutation Processor）：
  - **approveStatus 4 态** + **docStatus（erp/doc-status）**。
  - **writer（facade `ErpAstMergeProcessor` + per-mutation）**：facade `validateTransitionForXxx`（同范式）+ `executeApprove`（写 APPROVED/ACTIVE + 多源资产合并为一卡片 + 币种/类别一致性校验 + `AssetMergePostingDispatcher.tryPost`）。**reverseApprove = 无条件抛错（不可逆契约）**：facade `reverseApprove` 委托 per-mutation `ErpAstMergeReverseApproveProcessor.reverseApprove:22-26`，`requireMerge` 后直接 `throw ERR_AST_MERGE_REVERSE_NOT_SUPPORTED`（无 posted 判定、短路）。per-mutation `ErpAstMerge{SubmitForApproval,Approve,Reject,WithdrawApproval,Cancel}Processor` 调 facade。
  - **领域错误码**：`ERR_AST_MERGE_ILLEGAL_STATUS_TRANSITION`（:289-292）+ `ERR_AST_MERGE_ILLEGAL_DOC_TRANSITION`（:293-296）+ `ERR_AST_MERGE_SOURCE_NOT_IN_SERVICE`/`CROSS_CATEGORY_NOT_ALLOWED`/`CROSS_CURRENCY_NOT_ALLOWED`/`NO_SOURCES`/`ALREADY_POSTED`（动态守卫）+ **`ERR_AST_MERGE_REVERSE_NOT_SUPPORTED`（:317-320，不可逆契约）**。
  - **过账**：`AssetMergePostingDispatcher`（**仅 post 路径，无 reverse**——beans.xml:43-47）。
- **不可逆契约（关键差异 vs 计划 2 文档）**：Split/Merge 的 approve 触发资产卡片结构性重组（不可物理回退），故 `AssetSplit/MergePostingDispatcher` 仅 post 无 reverse。**reverseApprove Mutation 存在但无条件抛错**——per-mutation `ErpAst{Split,Merge}ReverseApproveProcessor:22-26` 在 `requireXxx` 后直接 `throw ERR_AST_SPLIT/MERGE_REVERSE_NOT_SUPPORTED`，无 posted 判定、无窗口期、短路在任何状态守卫之前（owner doc `split-merge.md §关键业务规则 5` + `:116` 实证）。这与计划 2 的 ValueAdjustment/Disposal/Capitalization（reverseApprove 有真实 posted=true 红冲 + posted=false 不对称窗口）形成鲜明对比——Split/Merge 的 reverseApprove 连 posted=false 窗口都没有。错误更正路径 = 资产处置 + 新建流程。
- **既有 Bean 注册**：`app-service.beans.xml:97-100`（仅 Movement 双轴）。2 facade Processor（L77-80）+ 各 per-mutation Processor（L103-138）已注册。**2 实体 4 SM Bean 未注册**（greenfield）。
- **M4 采购审批 + 同域先例（facade 范式直接范本）**：1950-1 facade `validateTransitionForXxx` + per-mutation 注入 Bean 范式 done；assets Movement 双轴命名 done；计划 2 Disposal facade（已实仓核实 284 行）为本计划提供同域文档双轴直接范本。
- **合规基线**：`docs/audits/compliance-baseline.md` R5=0、R11=0。
- **owner doc 覆盖**：`docs/design/assets/split-merge.md`（§关键业务规则 5 不可逆契约 + 拆分/合并业务语义 + 比例/金额平衡/跨类别/币种一致性规则）。**owner doc 状态机章节**：split-merge.md 侧重业务规则而非状态机矩阵——双轴迁移矩阵须以代码为权威建立，owner doc 补矩阵章节（Decision 裁定，对齐 Movement 补章节先例）。

## Goals

- 为 2 个资产拆分/合并文档实体的 docStatus + approveStatus 双轴各落地实体级 `ErpAst*StateMachine` Bean（双轴各自独立 Bean，Approval/Document 后缀命名 §1），承载命名动作迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态。**直接镜像同批计划 2 Disposal facade 范式 + assets Movement 双轴命名**。
  - `ErpAstSplitApprovalStateMachine` + `ErpAstSplitDocumentStateMachine`
  - `ErpAstMergeApprovalStateMachine` + `ErpAstMergeDocumentStateMachine`
- 将 facade `validateTransitionForXxx`（hardcoded `Objects.equals`）改调 Bean `assertCanXxx`（try/catch common 码 → cause-chain 领域码），`executeApprove` 目标态改调 `*TargetStatus()`（**reverseApprove 不接线**——per-mutation 无条件抛错保持，Bean reverseApprove 边为名义元数据）。**动态业务守卫与副作用保留原位**（比例/金额平衡、跨类别/币种一致性、源资产 IN_SERVICE 校验、资产卡片重组、过账、不可逆守卫 `ERR_*_REVERSE_NOT_SUPPORTED`）。
- 层 2 四方对照（dict ↔ `split-merge.md` ↔ Bean 元数据 ↔ 全部 writer）逐实体逐轴裁定，含 reverseApprove→REJECTED 名义目标态（运行时不可达）+ 不可逆契约（reverseApprove 无条件抛 REVERSE_NOT_SUPPORTED，无 posted 窗口）+ doc-cancelled 守卫登记。
- 新增层 1 矩阵完备性表驱动测试（greenfield，4 Bean）；层 3 既有集成测试全绿回归。
- 保持全部既有外部行为不变（错误码值/参数、过账时序、不可逆契约、资产卡片重组时序）。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml。
- 不迁移 `posted`（§11.2 M4 (iii)）；过账编排保留在 `AssetSplit/MergePostingDispatcher` + Processor 原位。
- 不修改共享骨架（assets 域无 Abstract*Processor；module-common-service 零改动）。
- **不改变不可逆契约**——不实现 Split/Merge 的 reverse 过账路径（owner doc §关键业务规则 5；`AssetSplit/MergePostingDispatcher` 仅 post 无 reverse 保持）；reverseApprove per-mutation Processor 无条件抛 `ERR_*_REVERSE_NOT_SUPPORTED` 保持（无 posted 窗口）。
- 不接管 Asset.status 轴（归计划 1 M4.40——approve 中对源/目标资产 status 的 side-effect 由计划 1 守卫）。
- 不迁移 ErpAstValueAdjustment/Disposal/Capitalization（归计划 2）/ ErpAstAsset/DepreciationSchedule/Inventory/Maintenance（归计划 1）/ ErpAstMovement（done）/ ErpAstCip（系统派生）。
- 不引入全局 CRUD 写锁（M0.1 successor）。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）。
- 不证 Delta 覆盖（M4 保护域单项，归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M0.2 清单 + M1.3 模板 §11 + **同批计划 2 Disposal facade 直接范本** + M4 采购审批 1950-1；落地 4 个双轴 Bean + facade/per-mutation 接线 + 测试 + 四方对照；不改契约/模型/公共 API/共享骨架。**M4 plan-first**——approve 触发结构性资产过账 + 卡片重组）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 模板 + §11.2 M4 变体 + §1 双轴命名）、`docs/design/assets/split-merge.md`（§关键业务规则 5 不可逆契约 + 拆分/合并业务规则）、`docs/design/assets/state-machine.md`（§实现模式与守卫边界 PROC 路径）、`docs/design/domain-design-guidelines.md`（§16.4 reverseApprove→REJECTED）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（AST-9/10/11）、`docs/architecture/processor-extension-pattern.md`、`docs/skills/state-machine-business-review-prompt.md`、`docs/plans/2026-08-14-1931-2-erpast-value-change-documents-state-machine-beans.md`（同域文档双轴直接范本）
- Skill Selection Basis: 路线图 M4.48-51 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「facade validateTransition + per-mutation 接线、Bean 注册、`@Inject` 非 private、cause-chaining 错误码、不可逆契约守卫保留、过账副作用保留、产品化可定制性自检」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成测试回归」。`state-machine-business-review-prompt.md` 匹配层 2 四方对照。必需输入均已就绪。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护业财过账行为（Split/Merge approve 触发结构性资产过账 + 卡片重组，且不可逆）。在人工/owner-doc 确认「以行为保持的矩阵集中化方式迁移此 4 轴、过账路径完整保留、不可逆契约不改」可接受前为阻塞前置。
- 无端口/环境变量/CORS/密钥/.env/外部服务依赖。无数据迁移。

## Execution Plan

### Phase 1 - ErpAstSplit 双轴 Bean（M4.48 + M4.49）

Status: planned
Targets: `.../statemachine/ErpAstSplit{Approval,Document}StateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpAstSplitProcessor.java`、`.../processor/ErpAstSplit{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval,Cancel}Processor.java`、`.../test/.../statemachine/TestErpAstSplit{Approval,Document}StateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done；同批计划 2 Disposal facade 范式已 done（或同期建立）；M4 plan-first 门控解除

- [ ] `Decision`（不可逆契约建模 + 双轴接线 + docStatus 轴）：(A) reverseApprove 目标态=REJECTED（§16.4 约定）——但 **Split/Merge reverseApprove 是无条件抛错动作（per-mutation Processor:22-26，无 posted 判定、短路在 facade validateTransitionForReverseApprove 之前）**，运行时从不产生状态迁移。Bean 建模裁定：Approval Bean 将 reverseApprove APPROVED→REJECTED 声明为 **名义边（nominal edge，供矩阵完备性/可达性元数据 M5.1 消费 + §16.4 约定对齐）**，javadoc 显式标注「运行时不可达——per-mutation Processor 无条件抛 ERR_*_REVERSE_NOT_SUPPORTED」；Bean `assertCanReverseApprove` 存在但**不被接线**（reverseApprove 路径不经 facade validateTransition，per-mutation 短路）。**层 1 矩阵测试仅断言 reverseApprove 边在 `transitions()` 元数据中存在**（元数据完备性）；**运行时不可达由层 3 Phase 3 Proof 断言**（reverseApprove 调用无条件抛 ERR_*_REVERSE_NOT_SUPPORTED）。(B) docStatus 轴：approve 写 ACTIVE（SplitProcessor:117）+ cancel 守卫（`validateTransitionForCancel:176-187` 检查 ACTIVE/CANCELLED/posted 三条件 + `isCancelled():459-463`）——Document Bean 据实仓裁定（approve→ACTIVE 命名边 vs ACTIVE 预留死状态退化轴，同计划 2 Decision (B) 范式；实仓 facade 已写 ACTIVE，倾向命名边）。(C) `validateTransitionForCancel` 委托 Document Bean `isCancelled()`（docStatus 轴部分；该守卫的 ACTIVE/posted 条件属动态业务守卫保留原位，仅 CANCELLED 判定委托 Document Bean）。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] `Add`：落地 `ErpAstSplitApprovalStateMachine`（5 动作 6 边：submit UNSUBMITTED/REJECTED→SUBMITTED、approve SUBMITTED→APPROVED、reject SUBMITTED→REJECTED、reverseApprove APPROVED→REJECTED **[名义边，运行时不可达——无条件抛 ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED]**、withdraw SUBMITTED→UNSUBMITTED）+ `ErpAstSplitDocumentStateMachine`（据 Decision (B)）+ `assertCanXxx`/`*TargetStatus()`/分类/`transitions()`。reverseApprove 边 javadoc 标注不可达 + 不接线。注册 2 Bean。镜像计划 2 Disposal 双轴结构。
  - Skill: `nop-backend-dev`
- [ ] `Add`（接线）：`ErpAstSplitProcessor` 注入 2 Bean（非 private）；`validateTransitionForSubmit/Withdraw/Approve/Reject` 各改调 Approval Bean `assertCanXxx`（try/catch common 码 → cause-chain `ERR_AST_SPLIT_ILLEGAL_STATUS_TRANSITION`）；`validateTransitionForCancel` 改调 Document Bean `isCancelled()`；`executeApprove` 目标态改调 Bean `*TargetStatus()`。**reverseApprove 不接线**（per-mutation `ErpAstSplitReverseApproveProcessor:22-26` 短路在 facade validateTransitionForReverseApprove 之前，无条件抛 `ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED`——保留原位，Bean reverseApprove 边为名义元数据不接运行时）。per-mutation 5 Processor（Submit/Approve/Reject/Withdraw/Cancel）经 facade 透传自动生效；ReverseApprove per-mutation 保持无条件抛错不变。**比例/金额平衡（PROPORTION_NOT_BALANCED/AMOUNT_NOT_BALANCED）、跨类别（CROSS_CATEGORY_NOT_ALLOWED）、源 IN_SERVICE（SOURCE_NOT_IN_SERVICE）、净值充足（INSUFFICIENT_NET_VALUE）、目标编码唯一（TARGET_ASSET_CODE_DUPLICATE）、已过账（ALREADY_POSTED）全部保留原位**。资产卡片重组 + `AssetSplitPostingDispatcher.tryPost` + posted 置位保留原位。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：层 1 矩阵完备性（2 Bean 独立测试）+ 层 2 四方对照（dict `wf/approve-status` + `erp/doc-status` ↔ `split-merge.md` §关键业务规则 ↔ Bean ↔ 全部 writer：facade validateTransition 6 + executeApprove/ReverseApprove 2 + per-mutation 6 + 创建写 + CRUD 路径排除 + 不可逆守卫 REVERSE_NOT_SUPPORTED 登记）。
  - Skill: `nop-testing` + `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] Split 双轴 Bean 存在/注册/无状态；facade validateTransition（Submit/Withdraw/Approve/Reject/Cancel）+ executeApprove 委托 Bean，内联 `Objects.equals` 状态判断已移除；reverseApprove 不接线（per-mutation 无条件抛 `ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED` 保持，Bean 边为名义元数据）。
- [ ] Split 层 1 矩阵测试本地 `mvn test -pl module-assets/erp-ast-service -am -Dtest=TestErpAstSplitApprovalStateMachineMatrix,TestErpAstSplitDocumentStateMachineMatrix` 全绿。

### Phase 2 - ErpAstMerge 双轴 Bean（M4.50 + M4.51）

Status: planned
Targets: `.../statemachine/ErpAstMerge{Approval,Document}StateMachine.java`、`.../processor/ErpAstMergeProcessor.java`、`.../processor/ErpAstMerge{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval,Cancel}Processor.java`、`.../test/.../statemachine/TestErpAstMerge{Approval,Document}StateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（Split 双轴 + facade 接线 + 不可逆守卫范式已固化）

- [ ] `Add`：落地 `ErpAstMergeApprovalStateMachine`（5 动作 6 边，同 Split 矩阵结构）+ `ErpAstMergeDocumentStateMachine`（同 Phase 1 Decision (C) 范式）。注册 2 Bean。
  - Skill: `nop-backend-dev`
- [ ] `Add`（接线，镜像 Phase 1）：`ErpAstMergeProcessor` 注入 2 Bean；`validateTransitionForSubmit/Withdraw/Approve/Reject` 改调 Approval Bean；cancel 守卫改调 Document Bean `isCancelled()`；`executeApprove` 目标态改调 Bean。**reverseApprove 不接线**（per-mutation `ErpAstMergeReverseApproveProcessor:22-26` 无条件抛 `ERR_AST_MERGE_REVERSE_NOT_SUPPORTED` 保持；Bean reverseApprove 边为名义元数据）。**源 IN_SERVICE（SOURCE_NOT_IN_SERVICE）、跨类别/币种（CROSS_CATEGORY/CURRENCY_NOT_ALLOWED）、无源（NO_SOURCES）、已过账（ALREADY_POSTED）全部保留原位**。资产卡片合并 + `AssetMergePostingDispatcher.tryPost` + posted 置位保留原位。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：层 1 矩阵完备性（2 Bean）+ 层 2 四方对照（dict ↔ `split-merge.md` ↔ Bean ↔ 全部 writer）。含不可逆契约登记。
  - Skill: `nop-testing` + `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] Merge 双轴 Bean 存在/注册/无状态；facade + 5 per-mutation（Submit/Approve/Reject/Withdraw/Cancel）委托 Bean；reverseApprove 不接线（per-mutation 无条件抛 `ERR_AST_MERGE_REVERSE_NOT_SUPPORTED` 保持，Bean 边为名义元数据）。
- [ ] Merge 层 1 矩阵测试本地 `mvn test -pl module-assets/erp-ast-service -am -Dtest=TestErpAstMerge*StateMachineMatrix` 全绿。

### Phase 3 - 层 3 既有命名动作回归 + 二实体一致性

Status: planned
Targets: `module-assets/erp-ast-service/src/test/`（既有集成测试，零新建）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1-2（二实体 4 轴 Bean + 接线已落地）

- [ ] `Proof`：层 3 既有命名动作回归——复用拆分/合并既有集成测试（approve happy path + **reverseApprove 无条件抛 ERR_*_REVERSE_NOT_SUPPORTED**（无 posted 窗口）+ reject + withdraw + cancel + illegal transition + 比例/金额平衡 + 跨类别/币种 + 资产卡片重组），证明错误码值/参数、过账时序、不可逆契约、卡片重组时序不变。本地 `mvn test -pl module-assets/erp-ast-service -am` 全绿。
  - Skill: `nop-testing`
- [ ] `Proof`：二实体一致性复核——4 Bean 命名（Approval/Document 后缀）/注册/无状态/矩阵形状一致；facade→Bean 注入 + cause-chaining + 不可逆守卫范式与计划 2 Disposal + Movement 双轴可追溯一致。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 层 3 既有集成测试全绿（零行为回归）。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_00335fee7ffedAqnzZlUZ6edkR`) — 零信任实仓核实全部 baseline 声明（6 错误码精确行号 / 2 dispatcher post-only 注释 / 2 facade + 各 6 per-mutation 注册 / greenfield SM Bean / split-merge.md §关键业务规则 5 / §16.4 reverseApprove→REJECTED / 规则 14 bundling + §11.2 M4 治理 + 规则 13 不可逆保留）均 pass。1 MAJOR（review brief 标记的 KEY risk）已修正：**reverseApprove 建模错误**——plan 误称「posted=false 窗口有效 / posted=true 不可逆」，实仓 per-mutation `ErpAst{Split,Merge}ReverseApproveProcessor:22-26` **无条件抛 `ERR_*_REVERSE_NOT_SUPPORTED`**（无 posted 判定、无 executeReverseApprove 方法体、短路在 facade validateTransitionForReverseApprove 之前）。v2 修正：baseline 改为无条件抛错；治理声明/Non-Goal 改为「无 posted 窗口」；Decision (A) 改为 Bean reverseApprove 边 = **名义元数据边（运行时不可达，javadoc 标注，不接线）**，层 1 矩阵测试显式断言不可达；Split/Merge 接线移除 reverseApprove 委托（per-mutation 保持无条件抛错）；移除 Phase 3 幻影「posted=false 窗口」测试断言。MINOR：docStatus Decision (C) 据实仓 SplitProcessor:117 已写 ACTIVE + :176-187/:459-463 isCancelled 守卫，倾向命名边（并入 Decision (B)）。
- Independent draft review iteration 2: `needs revision` → 修正后收敛 (`ses_0032e4e99ffeu7lO6e4m5NPdX5`) — iteration 1 MAJOR（无条件抛错建模）在 10 处中 8 处已正确修正；残余 2 处 Goals 高可见位仍带旧错误：(M1a) Goals L42 `posted=true 后 reverseApprove 抛` 残留条件模型；(M1b) Goals L41 引用不存在的 `executeReverseApprove` 接线目标（Split/Merge facade 无此方法）。v3 已修正 Goals 两行。2 MINOR 一并处理：(m1) Decision (A) 层 1/层 3 测试断言拆分（层 1 仅断言元数据存在，运行时不可达归层 3 Phase 3 Proof）；(m2) Decision (C) `validateTransitionForCancel` 保留 ACTIVE/posted 动态条件 + 仅 CANCELLED 判定委托 Document Bean `isCancelled()`。
- **M4 plan-first 人工/owner-doc 门控状态：confirmed（2026-08-14 人工确认解除）**（§11.2 M4 (i)）。人工/owner 于 2026-08-14 确认「以行为保持的矩阵集中化方式迁移结构性资产单据各轴、Split/Merge 过账 + 卡片重组 + post-only 不可逆契约完整保留」可接受，门控解除。据此将 Plan Status 由 `draft` 转 `active`。

## Closure Gates

- [ ] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)）
- [ ] 范围内行为完成（二实体 4 轴 Bean + facade/per-mutation 接线 + 层 1 矩阵 + 层 2 四方对照 + 层 3 回归）
- [ ] 相关文档对齐（roadmap M4.48-51 → done）
- [ ] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl module-assets/erp-ast-service -am` 全绿 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### Split/Merge 不可逆契约（无 reverse 过账路径）

- Classification: `intentional reserved (不可逆设计)`
- Why Not Blocking Closure: owner doc `split-merge.md §关键业务规则 5`——拆分/合并执行后不可撤销，`AssetSplit/MergePostingDispatcher` 仅 post 无 reverse；错误更正走资产处置 + 新建流程。reverseApprove per-mutation Processor 无条件抛 `ERR_*_REVERSE_NOT_SUPPORTED`（无 posted 窗口）。本计划不改此契约。
- Successor Required: yes（触发条件 = PM 要求拆分/合并可撤销时，须先设计 reverse 过账 + 卡片逆向重组方案，触及模型/过账保护区 ask-first）

### Split/Merge 动态业务守卫（比例/金额/类别/币种/净值/编码唯一）

- Classification: `watch-only residual (dynamic guard, not migration edge)`
- Why Not Blocking Closure: PROPORTION_NOT_BALANCED/AMOUNT_NOT_BALANCED/CROSS_CATEGORY/CURRENCY_NOT_ALLOWED/SOURCE_NOT_IN_SERVICE/INSUFFICIENT_NET_VALUE/TARGET_ASSET_CODE_DUPLICATE/NO_LINES/NO_SOURCES/ALREADY_POSTED 均为业务值/来源态/config 动态守卫，非固定状态迁移边，保留原位。
- Successor Required: no

### Delta 覆盖运行时实证

- Classification: `optimization candidate`
- Why Not Blocking Closure: M4 保护域单项 Delta 可选；归 M5.3。
- Successor Required: yes（触发条件 = M5.3 最终跨域 Delta 覆盖回归）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 裁定选项 (c) 显式排除。
- Successor Required: no

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计>

Follow-up:

- <待执行后填写；Deferred 项均为既定 successor>
