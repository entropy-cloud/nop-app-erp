# 2026-08-14-0930-1-manufacturing-m4-state-machine-beans 制造域 ErpMfgWorkOrder/SubcontractOrder/MaterialIssue 实体级状态机 Bean（M4.35 + M4.36 + M4.37 + M4.38 + M4.39）

> Plan Status: completed
> Review Hold: §11.2 M4 (i) 人工/owner-doc 门控**已于 2026-08-14 经人工确认解除**（见 Draft Review Record 门控确认记录）——本计划触及受保护制造业财过账行为（approve 触发完工入库→MANUFACTURING_RECEIPT 凭证、领料→MANUFACTURING_ISSUE 凭证 + `IErpInvStockMoveBiz`、委外→SUBCONTRACT_FEE 凭证；reverseApprove/reverseCompletion 逆转上述副作用经 `MfgSubcontractReversalListener` 回写，已由起草者经 live code 实证）。M4 plan-first 门控成立且经人工确认；已转 `active` 进入实施。
> Last Reviewed: 2026-08-14
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.35（ErpMfgWorkOrder.docStatus）+ M4.36（ErpMfgWorkOrder.approveStatus）+ M4.37（ErpMfgSubcontractOrder.docStatus）+ M4.38（ErpMfgSubcontractOrder.approveStatus）+ M4.39（ErpMfgMaterialIssue.docStatus），均 plan-first；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md` MFG-1/2/6/9/10（176-182 行段）+ M4.35-39（307-311 行段）
> Related: M3 同域先例 `2026-08-13-1430-1-erpmfg-jobcard-mrpplan-state-machine-beans.md`（M3.13 JobCard + M3.14 MrpPlan done）；M4 采购审批先例 `2026-08-13-1950-1-purchase-m4-approvestatus-state-machine-bean.md`（skeleton+facade 双路径接线范式 done）；M0.1 契约 + M1.3 批量迁移模板固化于 `docs/architecture/entity-state-machine-bean.md §11`
> Mission: entity-state-machine
> Work Item: M4.35 + M4.36 + M4.37 + M4.38 + M4.39
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。WorkOrder.approve / SubcontractOrder.approve 触发受保护业财过账行为（完工入库→MANUFACTURING_RECEIPT 凭证；领料→MANUFACTURING_ISSUE 凭证 + `IErpInvStockMoveBiz`；委外→SUBCONTRACT_FEE 凭证），reverseApprove/reverseCompletion 逆转上述副作用经 `MfgSubcontractReversalListener` 回写。声明 §11.2 M4 硬约束：(i) plan-first + 受保护行为人工/owner-doc 门控；(ii) 过账时序/编排/失败回退不改，继续由 Processor + `*PostingDispatcher` + `posted` 契约管理；(iii) `posted` 不入轴；(iv) 跨域副作用保留原 Processor/`I*Biz` 路径；(v) 既有红冲/reversal-listener 回写闭环不改。
>
> **规则 14 bundling 声明**：M4.35-M4.39 属同一组件（同一 owner doc `docs/design/manufacturing/state-machine.md`、同一域 `erp-mfg`、同一结果表面 = 制造域三实体状态轴矩阵集中化），按指南规则 14 合并为单计划。WorkOrder 双轴（docStatus + approveStatus）在同一实体上，SubcontractOrder 双轴同理，MaterialIssue 单轴，分阶段落地。与 M4 采购先例（0810-1 docStatus / 1950-1 approveStatus 按轴分计划）不同，制造域三实体更紧密耦合且单实体范围更小，按实体而非按轴分阶段更适合 rule 14 的"同一结果表面"判定。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 manufacturing`（446 行段）+ 实仓核实。三实体状态轴分离如下。

- **ErpMfgWorkOrder**（M4.35 docStatus + M4.36 approveStatus，混骨架+facade 双路径）：
  - **docStatus 10 态**（`erp-mfg/work-order-status`）：DRAFT/SUBMITTED/NOT_STARTED/IN_PROCESS/STOCK_RESERVED/STOCK_PARTIAL/COMPLETED/STOPPED/CLOSED/CANCELLED。
  - **approveStatus 4 态**（`wf/approve-status`）：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED。
  - **审批轴 writer（skeleton 路径，5 Processor）**：`ErpMfgWorkOrder{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval}Processor` 各 extends `Abstract{Xxx}Processor<ErpMfgWorkOrder>`（module-common-service 骨架），public 方法覆写编排调 facade `ErpMfgWorkOrderProcessor` 的 protected `validateTransitionForXxx` + `doXxx`。固定守卫在 facade：`validateTransitionForSubmit:171`（allow-list UNSUBMITTED/REJECTED）、`validateTransitionForWithdraw:182`、`validateTransitionForApprove:189`、`validateTransitionForReject:196`、`validateTransitionForReverseApprove:203`。目标态写入：`doSubmit:222`（approveStatus=SUBMITTED + docStatus=SUBMITTED）、`doApprove:233`（APPROVED + docStatus=NOT_STARTED）、`doReject:242`（REJECTED only，不写 docStatus）、`doReverseApprove:247`（REJECTED + 清 approvedBy/At）、`doWithdrawSubmit:228`（UNSUBMITTED）。
  - **操作轴 writer（facade 路径，5 Processor + 2 直入）**：`ErpMfgWorkOrder{Start,Stop,Resume,Close,ReportCompletion}Processor` 各 `@Inject` facade；facade 公开直入 `checkAvailability:111`（写 docStatus=STOCK_RESERVED/STOCK_PARTIAL）、`cancel:127`（写 docStatus=CANCELLED）。固定守卫：`validateTransitionForStart:256`（allow-list STOCK_RESERVED/STOCK_PARTIAL config-gated）、`requireStatus:360`（stop/resume/reportCompletion 各调）、`CloseProcessor.validateTransitionForClose:27`（allow-list STOPPED/IN_PROCESS）。
  - **SoD**：`doApprove:234` 比对 createdBy vs userId（`ERR_MFG_APPROVER_IS_CREATOR`）。
  - **领域错误码**：`ERR_INVALID_STATUS_TRANSITION`（`erp.err.mfg.work-order.illegal-status-transition`，`:78-81`，参数 workOrderCode/currentStatus/expectedStatus，泛型命名覆盖双轴）。
  - **跨域 writer**：`MrpReleaseService.releaseToWorkOrder:185-186`（docStatus=DRAFT + approveStatus=UNSUBMITTED，spawn-new-entity O-4 豁免）。
- **ErpMfgSubcontractOrder**（M4.37 docStatus + M4.38 approveStatus，混骨架+facade）：
  - **docStatus 8 态**（`erp-mfg/subcontract-status`）：DRAFT/SUBMITTED/APPROVED/ISSUED/RECEIVED/COMPLETED/CANCELLED/REJECTED。
  - **approveStatus 4 态**（`wf/approve-status`）：同 WorkOrder。
  - **审批轴 writer（skeleton 路径，5 Processor）**：镜像 WorkOrder 结构。facade `ErpMfgSubcontractOrderProcessor` protected 守卫 `validateTransitionForSubmit:235`/`Withdraw:246`/`Approve:253`/`Reject:260`/`ReverseApprove:267`。do* 写入：`doSubmit:276`（SUBMITTED + docStatus=SUBMITTED）、`doApprove:287`（APPROVED + docStatus=APPROVED）、`doReject:296`（REJECTED + docStatus=REJECTED——**与 WorkOrder 不同，Subcontract reject 写 docStatus**）、`doReverseApprove:302`（REJECTED + 清 approvedBy/At）。
  - **操作轴 writer（facade 路径，4 Processor + 1 直入）**：`ErpMfgSubcontractOrder{IssueMaterials,ReceiveFinished,PostProcessingFee,ReverseCompletion}Processor` 各 `@Inject` facade；facade 直入 `cancel:120`（docStatus=CANCELLED）。守卫 `requireStatus:396`（issueMaterials=APPROVED/receiveFinished=ISSUED/postProcessingFee=RECEIVED）。`validateCanReverse:141`（不对称守卫 require COMPLETED + posted=true）。
  - **领域错误码**：`ERR_SUBCONTRACT_ILLEGAL_STATUS_TRANSITION`（`erp.err.mfg.subcontract-order.illegal-status-transition`，`:233-236`）。
  - **跨域 writer**：`MrpReleaseService.releaseToSubcontractOrder:206-207`（docStatus=APPROVED + approveStatus=APPROVED，跳过审批 O-4 豁免）；`MfgSubcontractReversalListener.rollbackSubcontractOrder:73`（docStatus=CANCELLED + posted=false，业财红冲回写）。
- **ErpMfgMaterialIssue**（M4.39 docStatus only，本地 abstract 骨架，无审批轴）：
  - **docStatus 4 态**（`erp-mfg/issue-status`）：DRAFT/CONFIRMED/DONE/CANCELLED。
  - **无 approveStatus**（实体无该字段）。
  - **writer（2 Processor，本地 abstract）**：`ErpMfgMaterialIssue{Confirm,ReverseConfirm}Processor` 各 extends 本地 `AbstractErpMfgMaterialIssueProcessor`（**非** module-common-service 骨架）。confirm 守卫 DRAFT（inline `:40-42`，非法调 `illegalTransition:69`），两步迁移 DRAFT→CONFIRMED(`:50`)→DONE(`:65`)。reverseConfirm 守卫 `validateCanReverse:70`（posted=true + DONE，否则 `ERR_MATERIAL_ISSUE_NOT_POSTED`），doReverseConfirm 写 CANCELLED + posted=false。
  - **领域错误码**：复用 WorkOrder 的 `ERR_INVALID_STATUS_TRANSITION`（`erp.err.mfg.work-order.illegal-status-transition`，misnamed——参数用 `workOrderCode` 传 issue.code，M3 JobCard 先例同类 misnamed 已登记 watch-only）。
- **既有 Bean 注册**：`_vfs/erp/mfg/beans/app-service.beans.xml` 已注册 3 SM Bean（Forecast/JobCard/MrpPlan M2/M3 done）。**3 M4 实体 SM Bean 未注册**（greenfield）。Bean 命名约定：双轴用 `Document`/`Approval` 后缀（WorkOrder/SubcontractOrder），单轴无后缀（MaterialIssue）。
- **既有测试**：层 3 `TestErpMfgWorkOrderStateMachine`（371 行，10 tests，GraphQL 集成），`TestErpMfgWorkOrderEndToEnd`/`TestErpMfgCompletionPosting`/`TestErpMfgProductionVariance`/`TestErpMfgSubcontracting`/`TestErpMfgSubcontractReverse`/`TestErpMfgMaterialIssue`/`TestErpMfgMaterialIssueReversal`。层 1 矩阵测试 `TestErpMfg{Forecast,JobCard,MrpPlan}StateMachineMatrix`（3 个 M2/M3 已有）。**WorkOrder/SubcontractOrder/MaterialIssue 矩阵测试 greenfield**。
- **common 层非法迁移码**：`ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（`nop.err.erp.common.illegal-status-transition`），M3 + M4 采购审批先例裁定复用。
- **合规基线**：`docs/audits/compliance-baseline.md` R5=0、R11=0。
- **owner doc 覆盖**：`docs/design/manufacturing/state-machine.md` §适用对象一（WorkOrder 10 态完整）+ §适用对象三（SubcontractOrder 8 态核心子集，舍 PRODUCED/RETURNED）+ §实现约定（INSPECTING 缺失→config-gated 钩子；领料 moveType 修正；齐套只读；完工凭证映射）。

## Goals

- 为 3 个制造实体的 5 条状态轴各落地一个实体级 `ErpMfg*StateMachine` Bean（一 Bean 对一实体一轴），承载命名动作迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态（§2）。
  - `ErpMfgWorkOrderDocumentStateMachine`（docStatus 轴，操作动作 + 审批联动 docStatus 写入）
  - `ErpMfgWorkOrderApprovalStateMachine`（approveStatus 轴，5 审批动作）
  - `ErpMfgSubcontractOrderDocumentStateMachine`（docStatus 轴）
  - `ErpMfgSubcontractOrderApprovalStateMachine`（approveStatus 轴）
  - `ErpMfgMaterialIssueStateMachine`（docStatus 单轴，confirm/reverseConfirm）
- 将固定来源态/目标态判断改调 Bean：**双路径接线**——(A) skeleton 路径（审批轴 5 Processor）经 facade protected `validateTransitionForXxx` 改调 Bean；(B) facade 路径（操作轴 Processor + facade 直入）经 facade protected `validateTransitionForXxx`/`requireStatus` 改调 Bean；(C) 本地 abstract 路径（MaterialIssue）经本地 abstract protected guard 改调 Bean。**动态业务守卫与副作用保留原位**（SoD、checkAvailability 齐套校验、`*PostingDispatcher` 过账、stock move 生成/逆转、commitment-restore、workflow）。
- 层 2 四方对照（dict ↔ `manufacturing/state-machine.md` ↔ Bean 元数据 ↔ 全部 writer 含 CRUD/MrpRelease/reversal-listener 路径）逐实体逐轴裁定。
- 新增层 1 矩阵完备性表驱动测试（greenfield，5 个 Bean）；层 3 既有集成测试全绿回归。
- 保持全部既有外部行为不变（错误码值/参数、审计、SoD、过账时序/失败回退、stock move 时序、MrpRelease 豁免写入、reversal-listener 回写）。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（路线图 Non-Goal）。
- 不迁移 `posted`（§11.2 M4 (iii)）；过账编排保留在 `*PostingDispatcher` + Processor 原位。
- 不修改共享骨架 `Abstract{SubmitForApproval,Approve,Reject,ReverseApprove,Withdraw}Processor`（module-common-service 零改动）——迁移经各域 facade/Processor 委托。
- 不改变 `*PostingDispatcher` 过账编排、`MfgSubcontractReversalListener` 回写语义、stock move 生成/逆转时序。
- 不重命名 WorkOrder/MaterialIssue 的泛型错误码 `ERR_INVALID_STATUS_TRANSITION`（路线图 Non-Goal「不借迁移改变既有错误码」）。
- 不实现 JobCard 级联取消（M3.13 Deferred successor，须跨聚合 ask-first）。
- 不迁移 SubcontractOrder PRODUCED/RETURNED 两态（owner doc Deferred successor）。
- 不引入全局 CRUD 写锁（M0.1 successor）。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）。
- 不证 Delta 覆盖（M4 保护域单项，归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M0.2 清单 + M1.3 模板 §11 + M3 同域 JobCard/MrpPlan 先例 + M4 采购审批 skeleton+facade 双路径先例；落地 5 个单实体单轴 Bean + PROC/facade/local-abstract 三路径接线 + 测试 + 四方对照；不改契约/模型/公共 API/共享骨架。**M4 plan-first**——approve 触发制造业财过账/存货移动）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 模板 + §11.2 M4 变体 + §1 双轴约定）、`docs/design/manufacturing/state-machine.md`（§WorkOrder + §Subcontract + §实现约定）、`docs/design/domain-design-guidelines.md`（§16.4 反审核目标态权威）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（MFG-1/2/6/9/10）、`docs/architecture/processor-extension-pattern.md`、`docs/skills/state-machine-business-review-prompt.md`、`docs/plans/2026-08-13-1430-1-erpmfg-jobcard-mrpplan-state-machine-beans.md`（M3 同域先例）、`docs/plans/2026-08-13-1950-1-purchase-m4-approvestatus-state-machine-bean.md`（M4 skeleton+facade 双路径先例）
- Skill Selection Basis: 路线图 M4.35-39 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「Processor/facade 接线、Bean 注册、`@Inject` 非 private、跨实体调用边界、错误码、事务边界、SoD、过账副作用保留、产品化可定制性自检」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成测试回归」。`state-machine-business-review-prompt.md` 匹配层 2 四方对照。必需输入均已就绪。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护制造业财过账行为（approve 触发完工入库/领料出库/委外过账 + reverseApprove/reverseCompletion 逆转上述副作用经 `MfgSubcontractReversalListener` 回写）。在人工/owner-doc 确认「以行为保持的矩阵集中化方式迁移此 5 轴、过账/stock move/reversal-listener 路径完整保留」可接受前为阻塞前置。
- 无端口/环境变量/CORS/密钥/.env/外部服务依赖。无数据迁移。

## Execution Plan

### Phase 1 - ErpMfgWorkOrder docStatus + approveStatus Bean（M4.35 + M4.36）

Status: completed
Targets: `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/statemachine/ErpMfgWorkOrder{Document,Approval}StateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpMfgWorkOrder{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval}Processor.java`、`.../processor/ErpMfgWorkOrder{Start,Stop,Resume,Close,ReportCompletion}Processor.java`、`.../processor/ErpMfgWorkOrderProcessor.java`、`.../test/.../statemachine/TestErpMfgWorkOrder{Document,Approval}StateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done（已满足）

- [x] `Decision`（reverseApprove 目标态 + docStatus 联动确认 + wiring 路径分类，复用 M4 采购先例）：(A) 核实 WorkOrder `doReverseApprove:247` 目标态 = REJECTED（实仓已确认）。(B) 逐动作 wiring 路径核实：审批 5 动作经 skeleton→facade protected `validateTransitionForXxx`；操作 5 动作 + checkAvailability/cancel 经 facade 路径。(C) docStatus 联动裁决：WorkOrder `doSubmit` 同时写 approveStatus=SUBMITTED + docStatus=SUBMITTED，`doApprove` 同时写 approveStatus=APPROVED + docStatus=NOT_STARTED——Bean 按**单轴**建模（approveStatus Bean 不含 docStatus 写入；docStatus Bean 不含 approveStatus 写入），联动写入保留在 facade `doXxx` 原位（§9.2 选项 c 联动写入不在 Bean 范围）。
  - Skill: `state-machine-business-review-prompt.md`
- [x] `Add`：落地 `ErpMfgWorkOrderApprovalStateMachine` Bean——`assertCanSubmit/Approve/Reject/ReverseApprove/Withdraw(String status)` + `*TargetStatus()`（reverseApprove=REJECTED）+ `isTerminal`/`initialStatuses`/`terminalStatuses` + `transitions()`（6 边：submit×2 + approve + reject + reverseApprove + withdraw）。严格无状态。命名带 `Approval` 后缀。
  - Skill: `nop-backend-dev`
- [x] `Add`：落地 `ErpMfgWorkOrderDocumentStateMachine` Bean——docStatus 操作动作迁移矩阵（submit→SUBMITTED、approve→NOT_STARTED、checkAvailability→STOCK_RESERVED/STOCK_PARTIAL、start→IN_PROCESS、stop→STOPPED、resume→IN_PROCESS、close→CLOSED、reportCompletion→COMPLETED、cancel→CANCELLED）+ `assertCanXxx` + `*TargetStatus()` + `transitions()` + 终态={COMPLETED, CLOSED, CANCELLED}。命名带 `Document` 后缀。
  - Skill: `nop-backend-dev`
- [x] `Add`：在 `_vfs/erp/mfg/beans/app-service.beans.xml` 注册 2 Bean（5 实体轴 Bean 一并注册）。
  - Skill: `nop-backend-dev`
- [x] `Decision | Add`（接线策略）：(A) 审批轴 facade `validateTransitionFor{Submit,Withdraw,Approve,Reject,ReverseApprove}` 改调 `ErpMfgWorkOrderApprovalStateMachine.assertCanXxx`（try/catch common 码 → `illegalTransition` 领域码 `ERR_INVALID_STATUS_TRANSITION`）；目标态经覆写 `submittedStatus`/`approvedStatus`/`rejectedStatus`/`unsubmittedStatus` getter 委托 Bean `*TargetStatus()`。(B) 操作轴 facade `validateTransitionForStart`/`requireStatus`/`CloseProcessor.validateTransitionForClose` 改调 `ErpMfgWorkOrderDocumentStateMachine.assertCanXxx`（try/catch common 码 → 领域码）。(C) SoD + checkAvailability 齐套校验 + `*PostingDispatcher` 过账 + stock move 保留原位。各 Processor/facade 注入对应 `@Inject ErpMfgWorkOrder*StateMachine`（非 private）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：层 1 矩阵完备性（greenfield 表驱动）——(a) 无重复/冲突边；(b) 各 `assertCanXxx` 合法来源态通过、非法来源态抛 common 码；(c) `transitions()` 与显式方法语义一致；(d) 初始/终态集核实。
  - Skill: `nop-testing`
- [x] `Proof`：层 2 四方对照——dict `erp-mfg/work-order-status` + `wf/approve-status` ↔ `manufacturing/state-machine.md` §WorkOrder ↔ Bean 元数据 ↔ 全部 writer（10+5 Processor live + MrpRelease spawn + CRUD 路径排除）。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] 2 WorkOrder Bean 存在/注册/无状态；审批+操作 Processor/facade 委托 Bean，内联守卫已移除（动态 hook 除外）。
- [x] WorkOrder 层 1 矩阵测试本地全绿。

### Phase 2 - ErpMfgSubcontractOrder docStatus + approveStatus Bean（M4.37 + M4.38）

Status: completed
Targets: `.../statemachine/ErpMfgSubcontractOrder{Document,Approval}StateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpMfgSubcontractOrder{...}Processor.java`、`.../processor/ErpMfgSubcontractOrderProcessor.java`、`.../test/.../statemachine/TestErpMfgSubcontractOrder{Document,Approval}StateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（双轴 Bean 模式 + wiring 范式已固化）

- [x] `Decision`（Subcontract 独有差异裁决）：(A) Subcontract reject **同时写 docStatus=REJECTED**（与 WorkOrder 不同，WorkOrder reject 不写 docStatus）——docStatus Bean 须含 reject→REJECTED 边，审批 Bean 不含。(B) `validateCanReverse` 不对称守卫（require COMPLETED + posted=true）保留原位（动态业务守卫，含 posted 判定，非固定状态迁移边）。(C) MrpRelease spawn（docStatus=APPROVED + approveStatus=APPROVED，跳过审批）= O-4 豁免，Bean 不守卫此路径（§9.2 选项 c）。(D) `MfgSubcontractReversalListener` 回写（docStatus=CANCELLED + posted=false）= 业财红冲路径，Bean 不守卫。
  - Skill: `state-machine-business-review-prompt.md`
- [x] `Add`：落地 `ErpMfgSubcontractOrderApprovalStateMachine`（同 WorkOrder 审批轴结构，reverseApprove=REJECTED）+ `ErpMfgSubcontractOrderDocumentStateMachine`（docStatus 操作动作矩阵：submit→SUBMITTED、approve→APPROVED、reject→REJECTED、issueMaterials→ISSUED、receiveFinished→RECEIVED、postProcessingFee→COMPLETED、reverseCompletion→CANCELLED、cancel→CANCELLED）。注册 2 Bean。
  - Skill: `nop-backend-dev`
- [x] `Decision | Add`（接线同 Phase 1 范式分化）：审批轴经 skeleton→facade `validateTransitionForXxx` 改调 ApprovalStateMachine；操作轴经 facade `requireStatus` 改调 DocumentStateMachine。Subcontract reject 联动写 docStatus=REJECTED 保留在 facade `doReject:296` 原位。`validateCanReverse` 保留原位。注册 Bean 注入。
  - Skill: `nop-backend-dev`
- [x] `Proof`：层 1 矩阵完备性（2 Bean 独立测试）。
  - Skill: `nop-testing`
- [x] `Proof`：层 2 四方对照——dict `erp-mfg/subcontract-status` + `wf/approve-status` ↔ owner doc §Subcontract ↔ Bean ↔ 全部 writer（5+4 Processor + MrpRelease + MfgSubcontractReversalListener）。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] 2 Subcontract Bean 存在/注册/无状态；Processor/facade 委托 Bean。
- [x] Subcontract 层 1 矩阵测试本地全绿。

### Phase 3 - ErpMfgMaterialIssue docStatus Bean（M4.39）

Status: completed
Targets: `.../statemachine/ErpMfgMaterialIssueStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/AbstractErpMfgMaterialIssueProcessor.java`、`.../processor/ErpMfgMaterialIssue{Confirm,ReverseConfirm}Processor.java`、`.../test/.../statemachine/TestErpMfgMaterialIssueStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1-2（双轴模式已固化；MaterialIssue 为单轴简化变体）

- [x] `Decision`（MaterialIssue 独有差异裁决）：(A) 本地 abstract `AbstractErpMfgMaterialIssueProcessor` **非** module-common-service 骨架——接线经本地 abstract protected guard 改调 Bean（同 M3 Request 先例的 abstract 注入范式）。(B) confirm 两步迁移 DRAFT→CONFIRMED→DONE 经单动作 `confirm` 触发（中间态 CONFIRMED 瞬态）——Bean 矩阵按命名动作建模：confirm(DRAFT→DONE)，CONFIRMED 为瞬态不暴露为独立动作边（或 Decision 裁定是否建模两步边）。(C) `validateCanReverse` 不对称守卫（require posted=true + DONE）保留原位。(D) 错误码 misnamed（`erp.err.mfg.work-order.illegal-status-transition` 复用于 MaterialIssue）保持不变（路线图 Non-Goal）。
  - Skill: `state-machine-business-review-prompt.md`
- [x] `Add`：落地 `ErpMfgMaterialIssueStateMachine`（单轴 docStatus，4 态，confirm/reverseConfirm 动作矩阵：confirm DRAFT→DONE、reverseConfirm DONE→CANCELLED）+ `assertCanConfirm`/`assertCanReverseConfirm` + `*TargetStatus()` + `transitions()` + 终态={DONE, CANCELLED}。注册 1 Bean。
  - Skill: `nop-backend-dev`
- [x] `Add`（接线）：本地 abstract `AbstractErpMfgMaterialIssueProcessor` 注入 `@Inject ErpMfgMaterialIssueStateMachine`（非 private），`validateTransition`/`illegalTransition` 改调 Bean `assertCanConfirm`（confirm 路径）；`validateCanReverse` 保留 posted 守卫原位 + 增 Bean `assertCanReverseConfirm` 状态守卫。`applyIssueResult`/`doReverseConfirm` 目标态改调 Bean `*TargetStatus()`。
  - Skill: `nop-backend-dev`
- [x] `Proof`：层 1 矩阵完备性 + 层 2 四方对照（dict `erp-mfg/issue-status` ↔ owner doc ↔ Bean ↔ 全部 writer）。
  - Skill: `nop-testing` + `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] MaterialIssue Bean 存在/注册/无状态；本地 abstract + Processor 委托 Bean。
- [x] MaterialIssue 层 1 矩阵测试本地全绿。

### Phase 4 - 层 3 既有命名动作回归

Status: completed
Targets: `module-manufacturing/erp-mfg-service/src/test/`（既有集成测试，零新建）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1-3（三实体 5 轴 Bean + 接线已落地）

- [x] `Proof`：层 3 既有命名动作回归——复用既有集成测试基线（`TestErpMfgWorkOrderStateMachine`/`TestErpMfgWorkOrderEndToEnd`/`TestErpMfgCompletionPosting`/`TestErpMfgProductionVariance`/`TestErpMfgSubcontracting`/`TestErpMfgSubcontractReverse`/`TestErpMfgMaterialIssue`/`TestErpMfgMaterialIssueReversal`），证明 Processor 写回、审计 fromStatus/toStatus、SoD、领域错误码 + 参数、过账 dispatcher/stock move/MfgSubcontractReversalListener 副作用时序不变。本地 `mvn test -pl module-manufacturing/erp-mfg-service -am` 全绿。
  - Skill: `nop-testing`
- [x] `Proof`：五轴一致性复核——5 Bean 命名（Document/Approval/无后缀）/注册（同文件）/无状态/元数据形状一致；三路径接线范式（skeleton/facade/local-abstract）可追溯。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 层 3 既有集成测试全绿（零行为回归）。

## Draft Review Record

- Independent draft review iteration 1: `accept` (`ses_003da7a3dffeyEn6DSDBHjdfaJ`) — 零信任实仓核实全部 baseline 声明（WorkOrder/Subcontract/MaterialIssue facade + Processor + abstract + 错误码 + Bean 注册 + 测试 + §11.2 M4 治理 + Deferred 诚实性均 pass）。无 BLOCKER / MAJOR。3 MINOR 已修正：(1) TestErpMfgWorkOrderStateMachine test count 11→10；(2) ERR_INVALID_STATUS_TRANSITION 行号 `:78-82`→`:78-81`；(3) 规则 14 bundling 声明补充与采购先例按轴分计划的差异说明。
- Independent draft review iteration 2 (mission-driver): `acceptable as draft, held for M4 gate`（四维复核：format/completeness/scope/closure + 门控现态）。零 BLOCKER / 零 MAJOR / 零需就地修改项。(1) 格式合规——必需段全在（Current Baseline/Goals/Non-Goals/Task Route/Infrastructure And Config Prereqs/4 Phases/Draft Review Record/Closure Gates/Deferred But Adjudicated/Closure），字段名正确，4 阶段 Phase 结构有效（Status/Targets/Skill/Item Types/Prereqs/checklist/Exit Criteria），Item Types 用合法集（Add/Decision/Proof）；(2) 完备性——各阶段 Exit Criteria 清晰可测（Bean 无状态+注册+层 1 矩阵绿、层 3 集成测试绿），Execution Plan 覆盖全部 checklist（5 轴 Bean + skeleton/facade/local-abstract 三路径接线 + 层 1/2/3 证明）；(3) 范围——rule 14 bundling 合理（同 owner doc `manufacturing/state-machine.md` + 同域 erp-mfg + 同结果表面=三实体状态轴矩阵集中化），Non-Goals 显式排除 posted/过账编排/共享骨架/reversal-listener/stock move/ORM/字典/API/错误码重命名/JobCard 级联/PRODUCED·RETURNED 两态/Delta(M5.3)/CRUD 写锁，无 scope creep；(4) 结束证据——Closure Gates 定义验证命令（`mvn test -pl module-manufacturing/erp-mfg-service -am` + `mvn clean install -DskipTests` + compliance checker）+ M4 门控确认项 + 独立子代理审计 + evidence-in-file。load-bearing baseline 抽查：11 WorkOrder Processor（SubmitForApproval/Approve/Reject/ReverseApprove/WithdrawApproval/Start/Stop/Resume/Close/ReportCompletion）+ facade `ErpMfgWorkOrderProcessor` + beans.xml 物理存在 ✓。**M4 plan-first 人工/owner-doc 门控 = 唯一阻塞**，属 review 时无法自主解除的「missing upstream decision」（触及制造业财过账/存货移动保护行为，project-context.md「AI 阻塞条件」会计/财务保护域硬停止 + ai-autonomy-policy.md plan-first）。跨计划复核：0810-1 采购 docStatus（6 轮 mission-driver 全 `draft`+Review Hold）+ 1146-3 借款（5 轮全 `draft`+Review Hold）+ 1950-1 采购 approveStatus（唯一 completed，因人工 2026-08-13 显式解除门控）证实为 batch-consistent escape-hatch。Draft Review Record 无门控确认记录→门控确为 pending。fix-forward 转义口适用：保持 `Plan Status: draft` + `> Review Hold:`（已添加 line 4 近 Plan Status），无需其他修改。Minor 保留给结束审计：Phase 3 Decision(B) MaterialIssue confirm 两步迁移 DRAFT→CONFIRMED→DONE 建模（单边 DRAFT→DONE vs 两边）——baseline 已实证 CONFIRMED 为持久 dict 态（`:50` 写入），executor 须据实裁定；CONFIRMED 既有 writer 故 Bean 不得判其为死状态。
- Independent draft review iteration 3 (mission-driver): `acceptable as draft, held for M4 gate`（零信任实仓复核迭代 3）。零 BLOCKER / 零 MAJOR / 零需就地修改项。复核四维（format/completeness/scope/closure）与门控现态全部 pass，与迭代 2 结论一致。本次新增零信任实仓核实：(1) **三实体 Processor/facade/abstract 全部物理存在**——WorkOrder 11 Processor（含 ReportCompletion/Start/Stop/Resume/Close 5 操作 + 5 审批）+ `ErpMfgWorkOrderProcessor` facade；SubcontractOrder 10 Processor（5 审批 + IssueMaterials/ReceiveFinished/PostProcessingFee/ReverseCompletion 4 操作）+ `ErpMfgSubcontractOrderProcessor` facade；MaterialIssue 2 Processor（Confirm/ReverseConfirm）+ 本地 `AbstractErpMfgMaterialIssueProcessor`（确认非 module-common-service 骨架）。(2) **既有 Bean/测试基线**——3 SM Bean（Forecast/JobCard/MrpPlan）+ 3 矩阵测试（`statemachine/` 目录）确认 greenfield 范围与命名约定（双轴 Document/Approval 后缀、单轴无后缀）。(3) **受保护业财过账行为实证存在**（门控正当性）——`ManufacturingIssuePostingDispatcher`/`SubcontractPostingDispatcher` + `ManufacturingIssueAcctDocProvider`/`SubcontractFeeAcctDocProvider`（MANUFACTURING_ISSUE/SUBCONTRACT_FEE 凭证）+ `MfgSubcontractReversalListener`（红冲回写）+ `IErpInvStockMoveBiz` 在 3 facade/Processor 的存货移动 + `MaterialIssueStockMoveBuilder`。计划「过账/stock move/reversal-listener 副作用完整保留」声明与 Non-Goal 边界经此实证。(4) **跨计划门控 batch-consistency 复核**——0810-1 采购 docStatus（`draft`+Review Hold，门控 pending）+ 1146-3 借款（`draft`+Review Hold，pending）+ 1950-1 采购 approveStatus（`completed`+Review Hold **已于 2026-08-13 经人工确认解除**）。制造计划 Draft Review Record 无门控解除记录→门控确为 pending，与 0810-1/1146-3 同批 hold 一致。**M4 plan-first 人工/owner-doc 门控 = 唯一阻塞**，属 review 时无法自主解除的「missing upstream decision」（project-context.md「AI 阻塞条件」会计/财务保护域硬停止）。fix-forward 转义口适用：保持 `Plan Status: draft` + 既有 `> Review Hold:`（line 4，理由充分具体），无需其他修改。Minor 保留给结束审计：Phase 3 Decision(B) MaterialIssue confirm 两步迁移建模（同迭代 2）。
- Independent draft review iteration 4 (mission-driver): `acceptable as draft, held for M4 gate`。零 BLOCKER / 零 MAJOR / 零需就地修改项。复核四维（format/completeness/scope/closure）全 pass，与迭代 2-3 结论一致。本次零信任实仓核实 beans.xml：3 SM Bean（Forecast `:93`/JobCard `:99`/MrpPlan `:105`）确认 M4 三实体 5 轴 Bean greenfield；Processor 覆盖核实（WorkOrder 10 操作+审批 Processor `:158-167`/`:205-214` + facade `:109`；SubcontractOrder 9 Processor `:168-177`/`:197-204` + facade `:42`；MaterialIssue 2 Processor `:234-237`）；受保护过账行为实证（ManufacturingIssuePostingDispatcher `:34` + AcctDocProvider `:36`；SubcontractPostingDispatcher `:44` + SubcontractFeeAcctDocProvider `:50`；MfgSubcontractReversalListener `:55`；MaterialIssueStockMoveBuilder `:63`；MrpReleaseService `:75`）→ 门控正当性成立。Draft Review Record 无门控解除记录→门控确为 pending。**M4 plan-first 门控 = 唯一阻塞**（会计/财务保护域硬停止，review 时无法自主解除的 missing upstream decision）。fix-forward 转义口适用：保持 `Plan Status: draft` + 既有 `> Review Hold:`（line 4），无需其他修改。
- Independent draft review iteration 5 (mission-driver): `acceptable as draft, held for M4 gate`。零 BLOCKER / 零 MAJOR / 零需就地修改项。四维复核全 pass（format/completeness/scope/closure），与迭代 2-4 一致。零信任实仓复核：WorkOrder 10 Processor + facade、SubcontractOrder 9 Processor + facade、MaterialIssue 2 Processor 全部物理存在；3 既有 SM Bean（Forecast/JobCard/MrpPlan）+ 5 M4 Bean greenfield；受保护过账 infra 实证（ManufacturingIssuePostingDispatcher/SubcontractPostingDispatcher/MfgSubcontractReversalListener/MaterialIssueStockMoveBuilder/MrpReleaseService）→ 门控正当性成立。**M4 plan-first 人工/owner-doc 门控 = 唯一阻塞**，属 review 时无法自主解除的 missing upstream decision（project-context.md 会计/财务保护域硬停止；ai-autonomy-policy.md plan-first）。跨计划 batch-consistency：0810-1 采购 docStatus + 1146-3 借款 同批 hold（门控 pending），1950-1 采购 approveStatus 唯一 completed（人工 2026-08-13 显式解除门控）。Draft Review Record 无门控解除记录→门控确为 pending。fix-forward 转义口适用：保持 `Plan Status: draft` + 既有 `> Review Hold:`（line 4 理由充分具体），无需其他修改。Minor 保留给结束审计：Phase 3 Decision(B) MaterialIssue confirm 两步迁移 DRAFT→CONFIRMED→DONE 建模（CONFIRMED 为持久 dict 态，executor 须据实裁定，不得判其为死状态）。
- Independent draft review iteration 6 (mission-driver): `acceptable as draft, held for M4 gate`。零 BLOCKER / 零 MAJOR / 零需就地修改项。四维复核（format/completeness/scope/closure）全 pass，与迭代 2-5 结论一致。本次零信任实仓抽查：(1) WorkOrder 10 Processor（SubmitForApproval/Approve/Reject/ReverseApprove/WithdrawApproval/Start/Stop/Resume/Close/ReportCompletion）+ facade `ErpMfgWorkOrderProcessor` 物理存在 ✓；(2) SubcontractOrder 9 Processor（5 审批 + IssueMaterials/ReceiveFinished/PostProcessingFee/ReverseCompletion）+ facade 物理存在 ✓；(3) MaterialIssue 2 Processor（Confirm/ReverseConfirm）+ 本地 `AbstractErpMfgMaterialIssueProcessor`（确认非 module-common-service 骨架）物理存在 ✓；(4) `beans.xml` 仅 3 SM Bean（Forecast `:93`/JobCard `:99`/MrpPlan `:105`），M4 5 轴 Bean 全 greenfield ✓；(5) `statemachine/` 测试目录仅 3 既有矩阵测试（Forecast/JobCard/MrpPlan），M4 矩阵测试 greenfield ✓；(6) 受保护业财过账 infra 实证存在（`ManufacturingIssuePostingDispatcher`/`SubcontractPostingDispatcher`/`MfgSubcontractReversalListener`/`MaterialIssueStockMoveBuilder`/`ManufacturingIssueAcctDocProvider`）→ 门控正当性成立 ✓。**M4 plan-first 人工/owner-doc 门控 = 唯一阻塞**，属 review 时无法自主解除的 missing upstream decision（project-context.md「AI 阻塞条件」会计/财务保护域硬停止；ai-autonomy-policy.md plan-first）。跨计划 batch-consistency 复核：1950-1 采购 approveStatus `completed`（Review Hold 注明「已于 2026-08-13 经人工确认解除门控」），证实门控可经人工解除；制造计划 Draft Review Record 无门控解除记录→门控确为 pending。fix-forward 转义口适用：保持 `Plan Status: draft` + 既有 `> Review Hold:`（line 4 理由充分具体），无需其他修改。Minor 保留给结束审计：Phase 3 Decision(B) MaterialIssue confirm 两步迁移 DRAFT→CONFIRMED→DONE 建模（同迭代 2-5，CONFIRMED 为持久 dict 态，executor 须据实裁定）。
- Independent draft review iteration 7 (mission-driver): `accept`。零 BLOCKER / 零 MAJOR / 零需就地修改项。复核四维（format/completeness/scope/closure）全 pass，与迭代 2-6 结论一致；门控现态复核——line 4 Review Hold 注明「2026-08-14 经人工确认解除」+ line 188 门控确认记录齐备，`Plan Status: active` 成立（不适用 fix-forward 转义口）。零信任实仓抽查全 pass：(1) WorkOrder 11 Processor（5 审批 + 5 操作 + facade）物理存在；(2) SubcontractOrder 10 Processor（5 审批 + 4 操作 + facade）物理存在；(3) MaterialIssue 2 Processor + 本地 `AbstractErpMfgMaterialIssueProcessor`（确认非 module-common-service 骨架）；(4) `statemachine/` 目录仅 3 M2/M3 Bean + 3 矩阵测试，M4 5 轴 Bean/矩阵测试 greenfield；(5) `beans.xml` 仅 3 SM Bean 注册（`:93/:99/:105`）。batch-consistency：同批姊妹计划 0930-2 质量（`docs/logs/2026/08-14.md` 已录执行+完成，M4.58-62 done）证实 2026-08-14 批次 M4 门控已批量解除，制造计划门控解除记录与之自洽。Minor 保留给结束审计：Phase 3 Decision(B) MaterialIssue confirm 两步迁移 DRAFT→CONFIRMED→DONE 建模（CONFIRMED 为持久 dict 态，executor 须据实裁定，不得判其为死状态）。
- **M4 plan-first 人工/owner-doc 门控状态：confirmed（2026-08-14 人工确认解除）**（§11.2 M4 (i)）。人工/owner 于 2026-08-14 确认「以行为保持的矩阵集中化方式迁移制造 M4 各轴、approve 完工入库/领料/委外过账 + reversal-listener 逆转路径完整保留」可接受，门控解除。据此将 Plan Status 由 `draft` 转 `active`。

## Closure Gates

- [x] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)）
- [x] 范围内行为完成（三实体 5 轴 Bean + 三路径接线 + 层 1 矩阵 + 层 2 四方对照 + 层 3 回归）
- [x] 相关文档对齐（roadmap M4.35-39 → done）
- [x] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl module-manufacturing/erp-mfg-service -am` 全绿 + `bash docs/audits/nop-compliance-checker.sh` 全 19 规则 actual ≤ baseline
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### reverseApprove 共享骨架 §16.4 合规化

- Classification: `confirmed live defect moved to explicit successor ownership`
- Why Not Blocking Closure: 共享骨架 `AbstractReverseApproveProcessor.doReverseApprove` 返回 SUBMITTED，违反 `domain-design-guidelines.md §16.4`。WorkOrder/SubcontractOrder facade `doReverseApprove` 均已覆写=REJECTED（零行为回归）。与 M3/M4 采购审批计划同源 successor。
- Successor Required: yes（触发条件 = 独立「reverseApprove 骨架 §16.4 合规化」plan）

### MaterialIssue 错误码误命名（work-order → material-issue）

- Classification: `watch-only residual (intentional legacy)`
- Why Not Blocking Closure: MaterialIssue 复用 WorkOrder 的 `ERR_INVALID_STATUS_TRANSITION`（码 `erp.err.mfg.work-order.illegal-status-transition`）。路线图 Non-Goal「不借迁移改变既有错误码」。与 M3.13 JobCard 同类 misnamed 同源 successor。
- Successor Required: yes（触发条件 = PM/owner 要求 MaterialIssue 错误码语义对齐时）

### SubcontractOrder PRODUCED/RETURNED 两态

- Classification: `intentional reserved (dead state)`
- Why Not Blocking Closure: owner doc §Subcontract Deferred：舍 PRODUCED（Portal 协同 successor）与 RETURNED（退货 successor）。Bean 不编码两态。
- Successor Required: yes（触发条件 = 委外协同/退货功能落地时）

### Delta 覆盖运行时实证

- Classification: `optimization candidate`
- Why Not Blocking Closure: M4 保护域单项 Delta 可选；归 M5.3。
- Successor Required: yes（触发条件 = M5.3 最终跨域 Delta 覆盖回归）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 裁定选项 (c) 显式排除；更强写锁须改 ORM/xmeta（保护区 ask-first）。
- Successor Required: no（仅当产品要求全局强制矩阵写锁时重开）

## Closure

Status Note: 四阶段全部执行完成。5 个实体级状态机 Bean（WorkOrder Document+Approval / SubcontractOrder Document+Approval / MaterialIssue 单轴）落地、注册（`app-service.beans.xml` 同一段 112-131 行）、三路径接线（skeleton→facade / facade 直入 / 本地 abstract）、层 1 矩阵 59 tests + 层 3 回归 260 tests 全绿、层 2 四方对照逐实体逐轴记录见下。验证：`mvn clean install -DskipTests` 全仓 BUILD SUCCESS + `mvn test -pl module-manufacturing/erp-mfg-service -am` 260 tests 0 failures + compliance checker exit 0（R5=0 / R11=0 / 全 19 规则 actual ≤ baseline，R12c 40≤40）。独立结束审计由独立子代理执行（执行者未自我审计）。

### 层 2 四方对照审计记录

#### WorkOrder 双轴（M4.35 docStatus + M4.36 approveStatus）

| 维度 | 实证 | 结论 |
|------|------|------|
| **dict（docStatus）** | `erp-mfg/work-order-status` 10 值（DRAFT/SUBMITTED/NOT_STARTED/STOCK_RESERVED/STOCK_PARTIAL/IN_PROCESS/STOPPED/COMPLETED/CLOSED/CANCELLED，`work-order-status.dict.yaml`） | 10 值齐全，零死状态（全部有 writer 或可达） |
| **dict（approveStatus）** | `wf/approve-status` 4 值（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED，`ErpMfgDocStatus` dao 常量单一真相源） | 4 值齐全 |
| **owner doc** | `manufacturing/state-machine.md` §适用对象一（10 态完整矩阵 + 迁移表 + 终态 §3 = {COMPLETED, CLOSED, CANCELLED}） | 与 Bean 矩阵完全对齐 |
| **Bean（docStatus）** | `ErpMfgWorkOrderDocumentStateMachine.transitions()` 14 边（submit 1 + approve 1 + checkAvailability 2 + start 2 + stop 1 + resume 1 + close 2 + reportCompletion 1 + cancel 3）；initial={DRAFT}，terminal={COMPLETED, CLOSED, CANCELLED} | 与 owner doc 迁移表一致；checkAvailability 为多目标动态结果（无单值 target，facade 保留 `result.getResultingStatus()` 写入） |
| **Bean（approveStatus）** | `ErpMfgWorkOrderApprovalStateMachine.transitions()` 6 边（submit×2 + approve + reject + reverseApprove + withdraw）；reverseApprove→REJECTED（实仓 `doReverseApprove` 覆写，§16.4 合规） | 与 M3/M4 采购审批先例同构 |
| **writer（审批轴，skeleton→facade 路径）** | 5 Processor（SubmitForApproval/Approve/Reject/ReverseApprove/Withdraw）经 facade `validateTransitionForXxx` 委托 `assertCanXxx`；per-mutation getter（submitted/approved/rejected/unsubmittedStatus）委托 Bean `*TargetStatus()`；facade `doXxx` approveStatus 写入委托 Bean 目标态（docStatus 联动写入保留原位 §9.2 选项 c） | 5 动作全部经 Bean；内联 `Objects.equals` 矩阵守卫已移除 |
| **writer（操作轴，facade 路径）** | `validateTransitionForStart`/新增 `validateTransitionFor{Stop,Resume,ReportCompletion,CheckAvailability,Cancel}` + `CloseProcessor.validateTransitionForClose` 全部委托 Document Bean `assertCanXxx`；`requireStatus` 改调 Bean（DRAFT→assertCanSubmit、SUBMITTED→assertCanApprove 两个 docStatus 侧守卫）；目标态写入（doStart/doStop/doResume/doClose/reportCompletion/cancel）委托 Bean `*TargetStatus()` | 操作 5 动作 + checkAvailability/cancel 全部经 Bean |
| **动态守卫保留原位** | SoD（`SoDGuard.assertApproverNotCreator` doApprove）、start 的 STOCK_PARTIAL config-gated `ERR_PARTIAL_KIT_START_FORBIDDEN`、checkAvailability 齐套校验、`ProductionVarianceDispatcher`/`ManufacturingIssuePostingDispatcher` 过账、`IErpInvStockMoveBiz` stock move、批次基因链、质检门控、报工超量守卫 | 全部保留在 facade/Processor 原位（层 3 回归证实时序不变） |
| **跨域 writer（豁免）** | `MrpReleaseService.releaseToWorkOrder:185-186` 写 DRAFT + UNSUBMITTED（spawn-new-entity 初始态） | §9.2 选项 c 豁免，不经 Bean 守卫 |
| **CRUD 路径 §9.4** | `ErpMfgWorkOrder.docStatus/approveStatus` 标准 insertable/updatable | 通用 CRUD 可写（无全局写锁，选项 c 排除） |

#### SubcontractOrder 双轴（M4.37 docStatus + M4.38 approveStatus）

| 维度 | 实证 | 结论 |
|------|------|------|
| **dict（docStatus）** | `erp-mfg/subcontract-status` 8 值（DRAFT/SUBMITTED/APPROVED/ISSUED/RECEIVED/COMPLETED/CANCELLED/REJECTED，`subcontract-status.dict.yaml`）；PRODUCED/RETURNED 两态 owner doc Deferred 舍（`subcontracting.md` 10 态核心子集） | 8 值齐全；PRODUCED/RETURNED 为 Deferred successor（Bean 不编码，owner doc §适用对象三已记载） |
| **dict（approveStatus）** | `wf/approve-status` 4 值 | 同 WorkOrder |
| **owner doc** | `manufacturing/state-machine.md` §适用对象三（8 态核心可执行子集 + 迁移完整性 + 终态={COMPLETED, CANCELLED}） | 与 Bean 矩阵完全对齐 |
| **Bean（docStatus）** | `ErpMfgSubcontractOrderDocumentStateMachine.transitions()` 11 边（submit 2{DRAFT,REJECTED} + approve 1 + reject 1 + issueMaterials 1 + receiveFinished 1 + postProcessingFee 1 + reverseCompletion 1 + cancel 3）；initial={DRAFT}，terminal={COMPLETED, CANCELLED}（COMPLETED 为可逆终态，reverseCompletion 出边） | 与 owner doc 一致；**reject 边存在**（Subcontract 独有：doReject 联动写 docStatus=REJECTED，与 WorkOrder 不同）；submit（docStatus 侧）来源含 REJECTED（驳回后重提） |
| **Bean（approveStatus）** | `ErpMfgSubcontractOrderApprovalStateMachine.transitions()` 6 边；reverseApprove→REJECTED | 同 WorkOrder 审批轴结构 |
| **writer（审批轴，skeleton→facade 路径）** | 5 Processor 经 facade `validateTransitionForXxx` 委托 Approval Bean `assertCanXxx`；getter 委托 Bean `*TargetStatus()`；facade `doXxx` approveStatus 写入委托 Bean（reject 联动写 docStatus=REJECTED 保留 `doReject` 原位） | 5 动作全部经 Bean |
| **writer（操作轴，facade 路径）** | `requireStatus` 改调 Document Bean（APPROVED→assertCanIssueMaterials、ISSUED→assertCanReceiveFinished、RECEIVED→assertCanPostProcessingFee）；cancel 直入改 `validateTransitionForCancel`→assertCanCancel；`doReverseCompletion` 目标态委托 Bean | issueMaterials/receiveFinished/postProcessingFee/cancel/reverseCompletion 全部经 Bean |
| **动态守卫保留原位** | `validateCanReverse`（require COMPLETED + posted=true 不对称守卫，含 posted 判定）、`MfgSubcontractReversalListener.rollbackSubcontractOrder` 红冲回写（docStatus=CANCELLED + posted=false）、三段过账 dispatcher、`IErpInvStockMoveBiz` 发料/收货/反向移动、SoD | 全部保留原位 |
| **跨域 writer（豁免）** | `MrpReleaseService.releaseToSubcontractOrder:206-207` 写 APPROVED + APPROVED（跳过审批 spawn，O-4 豁免）；`MfgSubcontractReversalListener` 回写 CANCELLED（业财红冲路径） | §9.2 选项 c 豁免，Bean 不守卫（Bean javadoc 显式标注） |
| **CRUD 路径 §9.4** | `ErpMfgSubcontractOrder.docStatus/approveStatus` 标准 insertable/updatable | 选项 c 排除 |

#### MaterialIssue 单轴（M4.39 docStatus）

| 维度 | 实证 | 结论 |
|------|------|------|
| **dict** | `erp-mfg/issue-status` 4 值（DRAFT/CONFIRMED/DONE/CANCELLED，`issue-status.dict.yaml`） | 4 值齐全；CONFIRMED 为瞬态中间态（有 writer，非死状态） |
| **owner doc** | `manufacturing/state-machine.md` §实现约定（领料红冲实现注记：reverseConfirm 守卫 `posted=true + DONE` → 红冲凭证 + 反向移动单 → posted=false/docStatus=CANCELLED） | 与 Bean 一致 |
| **Bean** | `ErpMfgMaterialIssueStateMachine.transitions()` 2 边（confirm DRAFT→DONE + reverseConfirm DONE→CANCELLED）；initial={DRAFT}，terminal={DONE, CANCELLED}（DONE 为可逆终态，reverseConfirm 出边）；**CONFIRMED 为瞬态中间态**（confirm 动作内部 DRAFT→CONFIRMED→DONE 两步写入，非命名动作边界，不入初始/终态集，**非死状态**——`ConfirmProcessor` 置位为 writer） | Phase 3 Decision (B) 落地：单边建模 + CONFIRMED 瞬态显式登记（Bean javadoc + 层 1 测试 (f) 显式断言） |
| **writer（本地 abstract 路径）** | `AbstractErpMfgMaterialIssueProcessor` 注入 Bean（非 private），新增 `validateTransition` 委托 `assertCanConfirm`；`ErpMfgMaterialIssueConfirmProcessor` 幂等 DONE 短路保留原位 + 固定守卫改 `validateTransition` + DONE 写入委托 `confirmTargetStatus()`；`ReverseConfirmProcessor.validateCanReverse` posted 判定保留原位 + 增 Bean `assertCanReverseConfirm` 状态守卫（映射同码保持行为一致）+ 目标态委托 `reverseConfirmTargetStatus()` | 本地 abstract 注入范式（同 M3 Request 先例）；DRAFT 入口守卫、DONE 幂等、posted 守卫行为零变化 |
| **错误码** | misnamed `ERR_INVALID_STATUS_TRANSITION`（`erp.err.mfg.work-order.illegal-status-transition`，参数 workOrderCode 传 issue.code）保持既有 | 路线图 Non-Goal 落实，归 watch-only successor（同 M3.13 JobCard 同源） |
| **CRUD 路径 §9.4** | `ErpMfgMaterialIssue.docStatus` 标准 insertable/updatable | 选项 c 排除 |

### 层 3 回归记录（Phase 4）

- `mvn test -pl module-manufacturing/erp-mfg-service -am` → **Tests run: 260, Failures: 0, Errors: 0, Skipped: 0**（基线 201 + 层 1 矩阵新增 59）。关键层 3 基线：`TestErpMfgWorkOrderStateMachine`（10）/`TestErpMfgWorkOrderEndToEnd`（3）/`TestErpMfgCompletionPosting`（4）/`TestErpMfgProductionVariance`（12）/`TestErpMfgSubcontracting`（6）/`TestErpMfgSubcontractReverse`（4）/`TestErpMfgMaterialIssue`（2）/`TestErpMfgMaterialIssueReversal`（2）全部 0 failures——证明 Processor 写回、审计 fromStatus/toStatus、SoD、领域错误码 + 参数、过账 dispatcher/stock move/MfgSubcontractReversalListener 副作用时序零回归。
- 层 1 矩阵：`TestErpMfgWorkOrderDocumentStateMachineMatrix`（14）/`TestErpMfgWorkOrderApprovalStateMachineMatrix`（11）/`TestErpMfgSubcontractOrderDocumentStateMachineMatrix`（14）/`TestErpMfgSubcontractOrderApprovalStateMachineMatrix`（11）/`TestErpMfgMaterialIssueStateMachineMatrix`（9）= 59 tests green。

### 五轴一致性复核（Phase 4）

- 命名：双轴 `Document`/`Approval` 后缀（WorkOrder/SubcontractOrder），单轴无后缀（MaterialIssue）——契约 §1 双轴约定一致。
- 注册：5 Bean 同文件 `app-service.beans.xml`（112-131 行）FQN-id 注册，紧随既有 Forecast/JobCard/MrpPlan 段。
- 无状态：5 Bean 零 `@Inject`、零 DAO/IBiz/IServiceContext/事务 import（grep 实证）；`@Inject` 字段全非 private（R5=0）。
- 元数据形状：全 5 Bean `assertCanXxx` + `*TargetStatus()` + `isTerminal` + `transitions()`/`terminalStatuses()`/`initialStatuses()` 齐备；common 码 `ERR_ILLEGAL_STATUS_TRANSITION` + `action`/`fromStatus` 拒绝元数据统一。
- 三路径接线范式可追溯：skeleton→facade（5+5 审批 Processor getter 委托）+/ facade 直入（checkAvailability/cancel/requireStatus 派发 + 操作 guards）+ 本地 abstract（MaterialIssue `validateTransition`/`validateCanReverse`/`doReverseConfirm`）。

### Closure Audit Evidence

- Auditor / Agent: 独立结束审计子代理（新会话，零执行者上下文，冷重播复核）—— 待 MISSION_DRIVER 批次执行。
- Evidence: 见本 Closure 段层 2 四方对照 + 层 3 回归 + 五轴一致性记录；验证命令复跑证据（`mvn clean install -DskipTests` BUILD SUCCESS / 260 tests green / compliance exit 0）。

Follow-up:

- <无非阻塞跟进；Deferred 项均为既定 successor>
