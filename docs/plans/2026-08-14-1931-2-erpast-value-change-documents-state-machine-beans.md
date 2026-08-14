# 2026-08-14-1931-2-erpast-value-change-documents-state-machine-beans 资产域价值调整文档双轴状态机 Bean（M4.42 + M4.43 + M4.44 + M4.45 + M4.46 + M4.47）

> Plan Status: active
> Review Hold: §11.2 M4 (i) 人工/owner-doc 门控**已于 2026-08-14 经人工确认解除**（见 Draft Review Record 门控确认记录）——本计划触及受保护资产/业财过账行为（ValueAdjustment approve→VALUE_ADJUSTMENT 减值/增值凭证、Disposal approve→DISPOSAL 清理凭证 + Asset 终态 SCRAPPED/SOLD、Capitalization approve→CAPITALIZATION 入账凭证 + Asset DRAFT→IN_SERVICE + 库存转固 stock move；reverseApprove posted=true 窗口红冲上述副作用，posted=false 不对称窗口悬挂经 DeferredPostingSweepJob 兜底）。M4 plan-first 门控成立且经人工确认；已转 `active` 进入实施。
> Last Reviewed: 2026-08-14
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.42（ErpAstValueAdjustment.docStatus）+ M4.43（ErpAstValueAdjustment.approveStatus）+ M4.44（ErpAstDisposal.docStatus）+ M4.45（ErpAstDisposal.approveStatus）+ M4.46（ErpAstAssetCapitalization.docStatus）+ M4.47（ErpAstAssetCapitalization.approveStatus），均 plan-first；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md` AST-3/4/5/6/7/8（314-319 行段）+ M4.42-47
> Related: M4 采购审批先例 `2026-08-13-1950-1-purchase-m4-approvestatus-state-machine-bean.md`（facade `validateTransitionForXxx` + per-mutation 双路径 + 双轴 Approval/Document 后缀命名范式 done）；M3 同域先例 `2026-08-13-0805-2-erpast-movement-state-machine-beans.md`（同域双轴 + INLINE→Bean done）；M4 维护先例 `2026-08-14-0930-3-maintenance-m4-state-machine-beans.md`（双轴 Bean done）；同批计划 1 `2026-08-14-1931-1-erpast-core-lifecycle-state-machine-beans.md`（assets status 轴，先执行建立 assets Bean 基线）
> Mission: entity-state-machine
> Work Item: M4.42 + M4.43 + M4.44 + M4.45 + M4.46 + M4.47
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。三文档实体的 approve 动作均触发业财过账：ValueAdjustment approve 触发 VALUE_ADJUSTMENT 减值/增值凭证；Disposal approve 触发 DISPOSAL 清理凭证（结转原值/累计折旧/损益）+ Asset.status 终态（SCRAPPED/SOLD）；Capitalization approve 触发 CAPITALIZATION 入账凭证 + Asset.status DRAFT→IN_SERVICE。reverseApprove posted=true 窗口红冲上述副作用。声明 §11.2 M4 硬约束：(i) plan-first + 受保护行为人工/owner-doc 门控；(ii) 过账时序/编排/失败回退不改，继续由 `ValueAdjustmentPostingDispatcher`/`DisposalPostingDispatcher`/`CapitalizationPostingDispatcher` + `posted` 契约管理；(iii) `posted` 不入轴；(iv) Asset.status side-effect 写入保留原 Processor 路径（本计划只接管文档自身 approveStatus/docStatus 轴，Asset.status 轴归计划 1 M4.40）；(v) 既有 reverseApprove posted=false 不对称窗口（owner doc §4 实现约定）不改。
>
> **规则 14 bundling 声明**：M4.42-47 属同一组件（同一 owner doc `docs/design/assets/state-machine.md`、同一域 `erp-ast`、同一结果表面 = 资产域价值调整文档双轴矩阵集中化），按指南规则 14 合并为单计划。三实体（ValueAdjustment/Disposal/Capitalization）均 docStatus+approveStatus 双轴、均 5 动作完整审批生命周期（submit/approve/reject/reverseApprove/withdraw）、均经 facade `validateTransitionForXxx` + per-mutation Processor 双路径、均触发业财过账——结构高度同构，按实体分阶段落地。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3 assets`（448 行段）+ 实仓核实。三实体均 **Facade + per-mutation Processor 两层结构**（`processor-extension-pattern.md`），**无共享 Abstract*Processor 骨架**。assets 域既有 1 个 SM Bean（Movement 双轴，M3.15+M3.16 done）。本计划为先执行的计划 1 建立的 assets Bean 基线之上追加。

- **ErpAstValueAdjustment**（M4.42 docStatus + M4.43 approveStatus，双轴，facade + 6 per-mutation Processor）：
  - **approveStatus 4 态**（`wf/approve-status`）：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED。
  - **docStatus 退化/生命周期**（`erp/doc-status`）：DRAFT/ACTIVE/CANCELLED（待实仓核实 writer——CANCELLED 经 cancel Processor 或 useLogicalDelete）。
  - **writer（facade + 6 per-mutation）**：facade `ErpAstValueAdjustmentProcessor` 含 `validateTransitionFor{Submit,Withdraw,Approve,Reject,ReverseApprove,Cancel}`（hardcoded `Objects.equals`，同 Disposal 范式）+ `executeApprove`/`executeReverseApprove`（写 approveStatus/docStatus + `ValueAdjustmentPostingDispatcher.tryPost`/reverse）。per-mutation `ErpAstValueAdjustment{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval,Cancel}Processor` 调用 facade。reverseApprove 含 `ERR_ADJUSTMENT_ALREADY_REVERSED` 不对称守卫 + `ERR_ADJUSTMENT_APPROVAL_REQUIRED`（强制审批配置）。
  - **领域错误码**：`ERR_ADJUSTMENT_ILLEGAL_STATUS_TRANSITION`（approveStatus，参数 adjustmentCode/currentStatus/expectedStatus）+ `ERR_ADJUSTMENT_ILLEGAL_DOC_TRANSITION`（docStatus）。
  - **特殊**：减值/重估调整类型（`ERR_ADJUSTMENT_TYPE_INVALID`）+ 已红冲不可二次红冲（`ERR_ADJUSTMENT_ALREADY_REVERSED`）。
- **ErpAstDisposal**（M4.44 docStatus + M4.45 approveStatus，双轴，facade + 5 per-mutation Processor——**已实仓核实，直接范本**）：
  - **approveStatus 4 态** + **docStatus（erp/doc-status）**。
  - **writer（facade `ErpAstDisposalProcessor` 284 行，已实仓核实）**：`validateTransitionForSubmit:133-139`（UNSUBMITTED/REJECTED）、`validateTransitionForWithdraw:141-146`（SUBMITTED）、`validateTransitionForApprove:148-153`（SUBMITTED）、`validateTransitionForReject:155-160`（SUBMITTED）、`validateTransitionForReverseApprove:162-167`（APPROVED）、`validateTransitionForCancel:169-173`（docStatus isCancelled 守卫）。`executeApprove:62-101` 写 approveStatus=APPROVED + docStatus=ACTIVE + `asset.setStatus(SCRAPPED/SOLD):76` + `DisposalPostingDispatcher.tryPost:90` + posted 置位。`executeReverseApprove:111-129` posted=true 窗口红冲 + `asset.setStatus(IN_SERVICE):116`。per-mutation 5 Processor 调 facade。
  - **领域错误码**：`ERR_DISPOSAL_ILLEGAL_STATUS_TRANSITION`（:271-276 `illegalTransition()` 辅助方法）+ `ERR_DISPOSAL_ILLEGAL_DOC_TRANSITION`（:278-283 `illegalDocTransition()` 辅助方法）+ `ERR_DISPOSAL_ASSET_NOT_DISPOSABLE`/`ALREADY_DISPOSED`（Asset 来源态动态守卫）。
- **ErpAstAssetCapitalization**（M4.46 docStatus + M4.47 approveStatus，双轴，facade + 5 per-mutation Processor）：
  - **approveStatus 4 态** + **docStatus（erp/doc-status）**。
  - **writer（facade `ErpAstAssetCapitalizationProcessor` + per-mutation `ErpAstAssetCapitalization{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval}Processor`）**：facade `validateTransitionForXxx`（同 Disposal 范式）+ `executeApprove`（写 APPROVED/ACTIVE + `asset.setStatus(IN_SERVICE)` + 生成折旧计划 + `CapitalizationPostingDispatcher.tryPost` + 库存转固 `IErpInvStockMoveBiz`）+ `executeReverseApprove`（posted 窗口 reverse + asset→DRAFT + cancelSchedules）。
  - **领域错误码**：`ERR_CAPITALIZATION_ILLEGAL_STATUS_TRANSITION`（:64-67）+ `ERR_CAPITALIZATION_ILLEGAL_DOC_TRANSITION`（:68-71）+ `ERR_CAPITALIZATION_CATEGORY_MISSING`/`ORIGINAL_VALUE_INVALID`/`USEFUL_LIFE_MISSING`（动态守卫）。
- **既有 Bean 注册**：`app-service.beans.xml:97-100`（仅 Movement 双轴）。3 facade Processor（L67-74）+ 各 per-mutation Processor（L102-162）已注册。**3 实体 6 SM Bean 未注册**（greenfield）。
- **M4 采购审批先例（facade 范式直接范本）**：`ErpPurInvoice/PaymentProcessor.validateTransitionForXxx`（facade protected 方法）+ per-mutation Processor 注入 Bean 的接线范式已在 1950-1 done。本计划镜像该范式（assets 版）。
- **reverseApprove posted=false 不对称窗口**：owner doc `assets/state-machine.md §4 实现约定`——资本化/处置 reverseApprove 仅 posted=true 时回滚资产行为，posted=false 窗口仅设单据 REJECTED，资产保持终态。悬挂经 `DeferredPostingSweepJob` 兜底。
- **合规基线**：`docs/audits/compliance-baseline.md` R5=0、R11=0。
- **owner doc 覆盖**：`docs/design/assets/state-machine.md` §适用对象（Asset 生命周期，资本化/处置即 DRAFT→IN_SERVICE / IN_SERVICE→SCRAPPED/SOLD 的触发源）+ §实现模式与守卫边界（PROC 路径 = 资本化/处置/价值调整 Processor 含完整业务守卫 + 过账联动）+ §4 实现约定（reverseApprove 不对称窗口）。**owner doc 缺口**：ValueAdjustment/Capitalization 无独立文档双轴 §章节（仅有 Asset 卡片 + Movement 两节）——Phase 四方对照须以代码为权威 + Decision 裁定补 owner doc 章节。

## Goals

- 为 3 个资产价值调整文档实体的 docStatus + approveStatus 双轴各落地实体级 `ErpAst*StateMachine` Bean（双轴各自独立 Bean，Approval/Document 后缀命名 §1），承载命名动作迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态。**直接镜像 M4 采购审批先例 1950-1 facade 范式 + assets Movement 双轴命名**。
  - `ErpAstValueAdjustmentApprovalStateMachine` + `ErpAstValueAdjustmentDocumentStateMachine`
  - `ErpAstDisposalApprovalStateMachine` + `ErpAstDisposalDocumentStateMachine`
  - `ErpAstAssetCapitalizationApprovalStateMachine` + `ErpAstAssetCapitalizationDocumentStateMachine`
- 将 facade `validateTransitionForXxx`（hardcoded `Objects.equals`）改调 Bean `assertCanXxx`（try/catch common 码 → cause-chain 领域码），per-mutation Processor + facade `executeApprove`/`executeReverseApprove` 目标态改调 `*TargetStatus()`。**动态业务守卫与副作用保留原位**（Asset 来源态校验、过账、schedule cancel/restore、stock move、gain/loss 计算、posted 置位）。
- 层 2 四方对照（dict ↔ `assets/state-machine.md` ↔ Bean 元数据 ↔ 全部 writer）逐实体逐轴裁定，含 reverseApprove→REJECTED 目标态 + doc-cancelled 守卫 + posted=false 不对称窗口登记。
- 新增层 1 矩阵完备性表驱动测试（greenfield，6 Bean）；层 3 既有集成测试全绿回归。
- 保持全部既有外部行为不变（错误码值/参数、过账时序/失败回退、posted=false 窗口语义、Asset.status side-effect 时序）。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml。
- 不迁移 `posted`（§11.2 M4 (iii)）；过账编排保留在各 `*PostingDispatcher` + Processor 原位。
- 不修改共享骨架（assets 域无 Abstract*Processor；module-common-service 零改动）。
- 不改变 reverseApprove posted=false 不对称窗口语义（owner doc §4——悬挂经 `DeferredPostingSweepJob` 兜底，本计划不改）。
- 不接管 Asset.status 轴（归计划 1 M4.40——本计划只接管文档自身 approveStatus/docStatus 轴；executeApprove 中对 `asset.setStatus(...)` 的 side-effect 由计划 1 AssetStateMachine 守卫，两计划在该行交汇，接线顺序见 Phase Decision）。
- 不迁移 ErpAstSplit/Merge（归计划 3）/ ErpAstInventory/Maintenance/Asset/DepreciationSchedule status（归计划 1）/ ErpAstMovement（done）/ ErpAstCip（系统派生）。
- 不引入全局 CRUD 写锁（M0.1 successor）。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）。
- 不证 Delta 覆盖（M4 保护域单项，归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M0.2 清单 + M1.3 模板 §11 + **M4 采购审批 facade 先例 1950-1** + **同域 Movement 双轴先例**；落地 6 个双轴 Bean + facade/per-mutation 接线 + 测试 + 四方对照；不改契约/模型/公共 API/共享骨架。**M4 plan-first**——approve 触发 CAPITALIZATION/DISPOSAL/VALUE_ADJUSTMENT 业财过账 + Asset 终态）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 模板 + §11.2 M4 变体 + §1 双轴 Approval/Document 命名）、`docs/design/assets/state-machine.md`（§Asset + §实现模式与守卫边界 + §4 实现约定 reverseApprove 不对称窗口）、`docs/design/domain-design-guidelines.md`（§16.4 reverseApprove→REJECTED）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（AST-3/4/5/6/7/8）、`docs/architecture/processor-extension-pattern.md`、`docs/skills/state-machine-business-review-prompt.md`、`docs/plans/2026-08-13-1950-1-purchase-m4-approvestatus-state-machine-bean.md`（facade 范式先例）
- Skill Selection Basis: 路线图 M4.42-47 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「facade validateTransition + per-mutation 接线、Bean 注册、`@Inject` 非 private、cause-chaining 错误码、跨实体调用边界、过账副作用保留、产品化可定制性自检」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成测试回归」。`state-machine-business-review-prompt.md` 匹配层 2 四方对照。必需输入均已就绪。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护业财过账行为（资本化 CAPITALIZATION、处置 DISPOSAL、价值调整 VALUE_ADJUSTMENT 凭证 + Asset 终态 + schedule cancel/restore + 库存转固 stock move）。在人工/owner-doc 确认「以行为保持的矩阵集中化方式迁移此 6 轴、过账路径完整保留、reverseApprove 不对称窗口不改」可接受前为阻塞前置。
- 无端口/环境变量/CORS/密钥/.env/外部服务依赖。无数据迁移。

## Execution Plan

### Phase 1 - ErpAstDisposal 双轴 Bean（M4.44 + M4.45）

Status: planned
Targets: `.../statemachine/ErpAstDisposal{Approval,Document}StateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpAstDisposalProcessor.java`（validateTransitionForXxx:133-173 + executeApprove:82-86 + executeReverseApprove:126）、`.../processor/ErpAstDisposal{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval}Processor.java`、`.../test/.../statemachine/TestErpAstDisposal{Approval,Document}StateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done；M4 采购审批 facade 先例 1950-1 done；M4 plan-first 门控解除

- [ ] `Decision`（双轴接线 + reverseApprove 目标态）：(A) reverseApprove 目标态=REJECTED（对齐 `domain-design-guidelines.md §16.4` + assets 域 R1.x + Movement 先例，非 SUBMITTED）。(B) docStatus 轴：Disposal docStatus 经 executeApprove 写 ACTIVE + cancel/isCancelled 守卫——Document Bean `transitions()` 含 approve→ACTIVE（若 approve 是 docStatus 唯一命名 writer）；若 docStatus 退化（ACTIVE 预留死状态）则按 Movement docStatus 退化轴范式（`transitions()` 空 + `isCancelled()` 只读守卫）。Phase 1 实仓核实后裁定。(C) `validateTransitionForCancel:169-173` doc-cancelled 守卫委托 Document Bean `isCancelled()`。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] `Add`：落地 `ErpAstDisposalApprovalStateMachine`（5 动作 6 边：submit UNSUBMITTED/REJECTED→SUBMITTED、approve SUBMITTED→APPROVED、reject SUBMITTED→REJECTED、reverseApprove APPROVED→REJECTED、withdraw SUBMITTED→UNSUBMITTED）+ `ErpAstDisposalDocumentStateMachine`（据 Decision (B)）+ `assertCanXxx`/`*TargetStatus()`/分类/`transitions()`。注册 2 Bean。镜像 Movement 双轴结构 + 1950-1 facade 范式。
  - Skill: `nop-backend-dev`
- [ ] `Add`（接线）：`ErpAstDisposalProcessor` 注入 2 Bean（非 private）；`validateTransitionForSubmit:133-139`/`Withdraw`/`Approve`/`Reject`/`ReverseApprove` 各改调 Approval Bean `assertCanXxx`（try/catch common 码 → cause-chain `illegalTransition`→`ERR_DISPOSAL_ILLEGAL_STATUS_TRANSITION`）；`validateTransitionForCancel:169-173` 改调 Document Bean `isCancelled()`；`executeApprove:82-83`/`executeReverseApprove:126` 目标态改调 Bean `*TargetStatus()`。per-mutation 5 Processor 经 facade 透传自动生效。**Asset 来源态校验（validateAssetDisposable:184-198）、gain/loss 计算、schedule cancel/restore、过账、posted 置位保留原位**。Asset.status side-effect（:76/:116）由计划 1 AssetStateMachine 守卫——两计划在该行交汇，接线互不冲突（Disposal Bean 管文档 approveStatus/docStatus，Asset Bean 管资产 status）。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：层 1 矩阵完备性（2 Bean 独立测试）+ 层 2 四方对照（dict `wf/approve-status` + `erp/doc-status` ↔ owner doc §Asset + §实现模式 ↔ Bean ↔ 全部 writer：facade validateTransition 6 + executeApprove/ReverseApprove 2 + per-mutation 5 + 创建写 + CRUD 路径排除）。
  - Skill: `nop-testing` + `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] Disposal 双轴 Bean 存在/注册/无状态；facade validateTransition + executeApprove/ReverseApprove 委托 Bean，内联 `Objects.equals` 状态判断已移除。
- [ ] Disposal 层 1 矩阵测试本地 `mvn test -pl module-assets/erp-ast-service -am -Dtest=TestErpAstDisposalApprovalStateMachineMatrix,TestErpAstDisposalDocumentStateMachineMatrix` 全绿。

### Phase 2 - ErpAstAssetCapitalization 双轴 Bean（M4.46 + M4.47）

Status: planned
Targets: `.../statemachine/ErpAstAssetCapitalization{Approval,Document}StateMachine.java`、`.../processor/ErpAstAssetCapitalizationProcessor.java`、`.../processor/ErpAstAssetCapitalization{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval}Processor.java`、`.../test/.../statemachine/TestErpAstAssetCapitalization{Approval,Document}StateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（Disposal 双轴 + facade 接线范式已固化）

- [ ] `Add`：落地 `ErpAstAssetCapitalizationApprovalStateMachine`（5 动作 6 边，同 Disposal 矩阵结构）+ `ErpAstAssetCapitalizationDocumentStateMachine`（同 Phase 1 Decision (B) 范式）。注册 2 Bean。
  - Skill: `nop-backend-dev`
- [ ] `Add`（接线，镜像 Phase 1）：`ErpAstAssetCapitalizationProcessor` 注入 2 Bean；`validateTransitionForXxx` 改调 Approval Bean；cancel 守卫改调 Document Bean `isCancelled()`；`executeApprove`/`executeReverseApprove` 目标态改调 Bean。**折旧计划生成、库存转固 stock move、Asset.status side-effect（→IN_SERVICE/→DRAFT）、过账、posted 置位保留原位**。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：层 1 矩阵完备性（2 Bean）+ 层 2 四方对照（dict ↔ owner doc ↔ Bean ↔ 全部 writer）。含 reverseApprove posted=false 不对称窗口（仅设 REJECTED，资产保持终态）登记。**Capitalization Document 轴特例**：`ErpAstAssetCapitalizationProcessor.executeReverseApprove:112` 额外写 `docStatus=CANCELLED`（Disposal/ValueAdjustment reverseApprove 只写 approveStatus=REJECTED，不写 docStatus）——Document Bean 须为 Capitalization 单独登记此 reverseApprove→CANCELLED 边（不与 Disposal Document Bean 完全同构）。
  - Skill: `nop-testing` + `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] Capitalization 双轴 Bean 存在/注册/无状态；facade + 5 per-mutation 委托 Bean。
- [ ] Capitalization 层 1 矩阵测试本地 `mvn test -pl module-assets/erp-ast-service -am -Dtest=TestErpAstAssetCapitalization*StateMachineMatrix` 全绿。

### Phase 3 - ErpAstValueAdjustment 双轴 Bean（M4.42 + M4.43）

Status: planned
Targets: `.../statemachine/ErpAstValueAdjustment{Approval,Document}StateMachine.java`、`.../processor/ErpAstValueAdjustmentProcessor.java`、`.../processor/ErpAstValueAdjustment{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval,Cancel}Processor.java`、`.../test/.../statemachine/TestErpAstValueAdjustment{Approval,Document}StateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1-2

- [ ] `Decision`（ValueAdjustment 特殊守卫迁移）：(A) `ERR_ADJUSTMENT_ALREADY_REVERSED`（已红冲不可二次红冲）——是 posted/docStatus 动态守卫非固定状态迁移边，保留原位（reverseApprove 动态守卫）。(B) `ERR_ADJUSTMENT_APPROVAL_REQUIRED`（强制审批配置）——config-gated 动态守卫，保留原位。(C) 调整类型/金额校验（`TYPE_INVALID`/`AMOUNT_INVALID`）——动态业务守卫，保留原位。Bean 只接管固定 approveStatus 5 动作矩阵 + docStatus 轴。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] `Add`：落地 `ErpAstValueAdjustmentApprovalStateMachine` + `ErpAstValueAdjustmentDocumentStateMachine`（同范式）。注册 2 Bean。
  - Skill: `nop-backend-dev`
- [ ] `Add`（接线，镜像 Phase 1）：facade 注入 2 Bean；validateTransition/executeApprove/ReverseApprove 委托 Bean。**已红冲守卫、强制审批配置、调整类型/金额校验、过账、posted 置位保留原位**。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：层 1 矩阵完备性（2 Bean）+ 层 2 四方对照。
  - Skill: `nop-testing` + `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] ValueAdjustment 双轴 Bean 存在/注册/无状态；facade + 6 per-mutation 委托 Bean。
- [ ] ValueAdjustment 层 1 矩阵测试本地 `mvn test -pl module-assets/erp-ast-service -am -Dtest=TestErpAstValueAdjustment*StateMachineMatrix` 全绿。

### Phase 4 - 层 3 既有命名动作回归 + 三实体一致性

Status: planned
Targets: `module-assets/erp-ast-service/src/test/`（既有集成测试，零新建）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1-3（三实体 6 轴 Bean + 接线已落地）

- [ ] `Proof`：层 3 既有命名动作回归——复用资本化/处置/价值调整既有集成测试（approve happy path + reverseApprove posted=true/false 窗口 + reject + withdraw + cancel + illegal transition + Asset 终态联动 + schedule cancel/restore + 过账），证明错误码值/参数、过账时序、posted=false 不对称窗口、gain/loss、stock move 不变。本地 `mvn test -pl module-assets/erp-ast-service -am` 全绿。
  - Skill: `nop-testing`
- [ ] `Proof`：三实体一致性复核——6 Bean 命名（Approval/Document 后缀）/注册/无状态/矩阵形状一致；facade→Bean 注入 + cause-chaining 范式与 1950-1 采购审批 + Movement 双轴可追溯一致。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 层 3 既有集成测试全绿（零行为回归）。

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (`ses_0033623baffet6rjvLOLJ3iXg1`) — 零信任实仓核实全部 baseline 声明（DisposalProcessor 284 行精确行号 validateTransitionForXxx + executeApprove:76/82-86 + executeReverseApprove:111-129/:116；ValueAdjustment 6 + Capitalization 5 + Disposal 5 per-mutation；6 错误码精确行号；3 dispatcher + 3 facade 注册；greenfield SM Bean；§16.4 reverseApprove→REJECTED；M4 采购先例 1950-1 completed）均 pass；规则 4/14 bundling、规则 7 item 类型、§11.2 M4 治理、Asset.status 与计划 1 接线分区、posted=false 不对称窗口保留、anti-slack 均 pass。2 MINOR 已处理：(M1) baseline「3 facade（L67-74）」范围含 DepreciationSchedule 共 4 facade（in-scope 3）——语义正确，保留；(M2) Capitalization `executeReverseApprove:112` 额外写 docStatus=CANCELLED（Disposal/ValueAdjustment 无此 docStatus 写）——已补 Phase 2 四方对照显式登记 Capitalization Document 轴特例边，不与 Disposal Document Bean 静默同构。无 BLOCKER / MAJOR，草案审查收敛。
- Mission-driver plan review (`2026-08-14-193118-mission-driver`) iteration 2: `acceptable as-is` — 复核 4 维度（格式完备性/完整性/范围/结束证据）均 pass，零 BLOCKER/MAJOR。零信任实仓核实：(1) Disposal 1 facade + 5 per-mutation ✓；(2) ValueAdjustment 1 facade + 6 per-mutation ✓；(3) Capitalization 1 facade + 5 per-mutation ✓；(4) beans.xml 仅 Movement 双轴注册（L97-100），6 目标 Bean greenfield ✓；(5) roadmap L138-143 确认 M4.42-47 均 plan-first + Status todo；(6) roadmap 确认 §11.2 M4 (i) 门控「未确认」「门控非起草者/审查者可自主解除」。M4 plan-first 人工/owner-doc 门控为 genuine blocker（missing upstream decision，非格式/完整性/范围/结束证据问题），保持 `Plan Status: draft` + `Review Hold` 不变（fix-forward escape hatch，同批 3 计划一致 hold）。
- Mission-driver plan review (`2026-08-14-193118-mission-driver`) iteration 3: `acceptable as-is` — 复核 4 维度（格式完备性/完整性/范围/结束证据）均 pass，零 BLOCKER/MAJOR。零信任独立实仓核实（两个 explore 子代理）全部 baseline 声明均 pass：DisposalProcessor 284 行精确、6 validateTransitionFor 行号精确、executeApprove:62-101/:76/:90 精确、executeReverseApprove:111-129/:116 精确、Capitalization executeReverseApprove docStatus=CANCELLED:112 精确、三实体 facade/per-mutation 数量精确、6 SM Bean greenfield 精确、roadmap L138-143 精确、AST-3/4/5/6/7/8 精确、owner doc gap 确认、4 先例计划均存在。1 MINOR 已修正：Disposal 错误码行号引用 :121-124/:125-128（实为 executeReverseApprove 内 setPosted 等语句）→ 已修正为 :271-276（`illegalTransition()` 辅助方法）/ :278-283（`illegalDocTransition()` 辅助方法）；错误码名称与 `illegalTransition`/`illegalDocTransition` 方法名自始正确，行号仅 baseline 描述字段引用错误，接线项动作目标行号（validateTransitionForXxx:133-173 + executeApprove:82-83 + executeReverseApprove:126）自始精确，不影响执行。M4 plan-first 人工/owner-doc 门控仍为 genuine blocker，保持 `Plan Status: draft` + `Review Hold` 不变。
- **M4 plan-first 人工/owner-doc 门控状态：confirmed（2026-08-14 人工确认解除）**（§11.2 M4 (i)）。人工/owner 于 2026-08-14 确认「以行为保持的矩阵集中化方式迁移资产价值变更单据各轴、VALUE_ADJUSTMENT/DISPOSAL/CAPITALIZATION 过账 + 库存转固 stock move + reverseApprove 红冲 + DeferredPostingSweepJob 兜底完整保留」可接受，门控解除。据此将 Plan Status 由 `draft` 转 `active`。
- Mission-driver plan review (`2026-08-14-070716-mission-driver`) iteration 4: `acceptable as-is` — 复核 4 维度（格式完备性/完整性/范围/结束证据）均 pass，零 BLOCKER/MAJOR，无需修改。零信任实仓抽查全部 pass：(1) DisposalProcessor 关键行号精确（validateTransitionFor:133/141/148/155/162/169 + executeApprove:62/:76 setStatus(SCRAPPED/SOLD)/:90 tryPost + executeReverseApprove:111/:116 + illegalTransition:271-276 + illegalDocTransition:278-283）；(2) 三实体 facade/per-mutation 数量精确（ValueAdjustment 1+6 / Disposal 1+5 / Capitalization 1+5）；(3) beans.xml L97-100 仅 Movement 双轴注册、6 目标 SM Bean greenfield；(4) Capitalization executeReverseApprove:112 额外 docStatus=CANCELLED 特例边精确；(5) roadmap M4.42-47 六项存在 + 表头「2026-08-14 人工门控确认」覆盖 M4.42-47（39 项之一），与 Draft Review Record 门控确认记录一致；(6) owner doc `assets/state-machine.md` + 1950-1 facade 先例 + 同批计划 1 存在。Plan Status 已为 `active`（门控确认后由前轮转入），本次复核确认其正确性，无改动。

## Closure Gates

- [ ] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)）
- [ ] 范围内行为完成（三实体 6 轴 Bean + facade/per-mutation 接线 + 层 1 矩阵 + 层 2 四方对照 + 层 3 回归）
- [ ] 相关文档对齐（roadmap M4.42-47 → done）
- [ ] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl module-assets/erp-ast-service -am` 全绿 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### reverseApprove posted=false 不对称窗口悬挂兜底

- Classification: `watch-only residual (intentional legacy)`
- Why Not Blocking Closure: owner doc §4 实现约定——posted=false 窗口 reverseApprove 仅设单据 REJECTED，资产保持终态，悬挂经 `DeferredPostingSweepJob`（Cap/Disposal）兜底重试 + `IErpSysNotificationBiz` 告警。本计划不改此行为。
- Successor Required: yes（触发条件 = PM 要求消除 posted=false 悬挂窗口 / 统一 reverseApprove 语义时）

### ValueAdjustment 已红冲/强制审批/类型金额守卫

- Classification: `watch-only residual (dynamic guard, not migration edge)`
- Why Not Blocking Closure: `ERR_ADJUSTMENT_ALREADY_REVERSED`/`APPROVAL_REQUIRED`/`TYPE_INVALID`/`AMOUNT_INVALID` 是 posted/docStatus/config/业务值动态守卫，非固定状态迁移边，保留原位。
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
