# 2026-08-14-0930-2-quality-m4-state-machine-beans 质量域 ErpQaInspection/NonConformance/Recall 实体级状态机 Bean（M4.58 + M4.59 + M4.60 + M4.61 + M4.62）

> Plan Status: active
> Last Reviewed: 2026-08-14
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.58（ErpQaInspection.docStatus）+ M4.59（ErpQaInspection.approveStatus）+ M4.60（ErpQaNonConformance.status）+ M4.61（ErpQaRecall.status）+ M4.62（ErpQaRecall.approveStatus），均 plan-first；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md` QA-1/2/3/4/5（330-334 行段）+ M4.58-62（330-334 行段）
> Related: M4 采购审批先例 `2026-08-13-1950-1-purchase-m4-approvestatus-state-machine-bean.md`（skeleton+facade 双路径 + Recall 同构 approval-orchestrator-facade 范式 done）；M3 同构先例 `2026-08-13-0805-1-erpmnt-request-state-machine-bean.md`（单 status 轴 abstract→Bean 范式 done）；M0.1 契约 + M1.3 批量迁移模板固化于 `docs/architecture/entity-state-machine-bean.md §11`
> Mission: entity-state-machine
> Work Item: M4.58 + M4.59 + M4.60 + M4.61 + M4.62
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。Inspection recordResult/failInspection 触发 NCR 自动生成 + posted 三件套；NCR resolve 触发报废损失/退货编排过账（NcrPostingDispatcher/NcrReturnOrchestrator）；Recall approve 触发 status=APPROVED + close→销售退货过账。声明 §11.2 M4 硬约束：(i) plan-first + 受保护行为人工/owner-doc 门控；(ii) 过账时序/编排/失败回退不改；(iii) `posted` 不入轴；(iv) 跨域副作用（`IErpPurReturnBiz`/`IErpSalReturnBiz`/NCR auto-create）保留原 Processor/`I*Biz` 路径；(v) 既有红冲/reversal 回写闭环不改。
>
> **规则 14 bundling 声明**：M4.58-M4.62 属同一组件（同一 owner doc `docs/design/quality/state-machine.md`、同一域 `erp-qa`、同一结果表面 = 质量域三实体状态轴矩阵集中化），按指南规则 14 合并为单计划。Inspection 双轴（docStatus + approveStatus）、NCR 单轴（status）、Recall 双轴（status + approveStatus），分阶段落地。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 quality`（454 行段）+ 实仓核实。三实体状态轴分离如下。

- **ErpQaInspection**（M4.58 docStatus + M4.59 approveStatus，特殊三字段分离）：
  - **三字段**：`result`（4 态 `erp-qa/inspection-result`：PENDING/ACCEPTED/CONDITIONAL/REJECTED——**实际驱动的状态机**）、`docStatus`（3 态 `erp/doc-status`：DRAFT/ACTIVE/CANCELLED——**泛型生命周期，零状态机 writer**）、`approveStatus`（4 态 `wf/approve-status`——**仅让步审批时写 APPROVED**）。
  - **result 轴 writer（本地 abstract 路径）**：`AbstractErpQaInspectionProcessor.requireInspectionPending:65-70`（守卫 result==PENDING）+ `illegalInspectionTransition:58-63`（抛 `ERR_INVALID_INSPECTION_STATUS_TRANSITION` `erp.err.qa.inspection.illegal-status-transition`，`:37-40`）。3 Processor 写 result：`RecordResultProcessor:57`（result=ACCEPTED/CONDITIONAL/REJECTED）、`PassInspectionProcessor:17`（ACCEPTED）、`FailInspectionProcessor:22`（REJECTED）。
  - **docStatus 轴**：零 writer——M0.2 清单标记为「纳入」，但实仓 docStatus 仅在测试 seed 写 ACTIVE，**无 Processor/BizModel 写 docStatus**。M4.58 迁移目标 = 为零-writer 的泛型轴建立 Bean 矩阵（Decision 裁定是否为 DRAFT→ACTIVE/CANCELLED 建立 Bean，或裁定为 dict-only 占位轴排除迁移）。
  - **approveStatus 轴**：`RecordResultProcessor:60-62` 仅在 concession+CONDITIONAL 时写 approveStatus=APPROVED + approvedBy/At。**非完整 5 动作审批生命周期**——无 submit/reject/reverseApprove/withdraw writer。M4.59 迁移目标 = Decision 裁定 approveStatus 轴的迁移范围（完整 5 动作矩阵 vs 仅 concession-approve 单边）。
  - **领域错误码**：`ERR_INVALID_INSPECTION_STATUS_TRANSITION`（`:37-40`，参数 inspectionCode/currentStatus/expectedStatus）。
  - **既有测试**：`TestErpQaInspectionStateMachine`（347 行，13 tests，GraphQL 集成）。
- **ErpQaNonConformance (NCR)**（M4.60 status，单轴，混 BizModel 直入 + Processor abstract）：
  - **status 5 态**（`erp-qa/ncr-status`）：OPEN/IN_REVIEW/RESOLVED/ESCALATED_TO_RECALL/CANCELLED。**无 approveStatus**（实体无该字段）。
  - **writer（BizModel 直入）**：`ErpQaNonConformanceBizModel.submitReview:50-56`（OPEN→IN_REVIEW）、`escalateToRecall:81-88`（IN_REVIEW→ESCALATED_TO_RECALL，status-only placeholder）、`cancel:98-108`（OPEN/IN_REVIEW→CANCELLED，inline guard `:101-103`）。
  - **writer（Processor abstract）**：`AbstractErpQaNonConformanceProcessor.requireNcrStatus:39-44` + `illegalNcrTransition:46-51`（抛 `ERR_INVALID_NCR_STATUS_TRANSITION` `erp.err.qa.ncr.illegal-status-transition`，`:57-60`）。4 Processor：`ResolveProcessor`（IN_REVIEW→RESOLVED + CAPA gate + NcrPostingDispatcher）、`PostNcrProcessor`（require RESOLVED）、`ReverseNcrProcessor`（红冲 reversal）、`UpgradeToRecallProcessor`（IN_REVIEW→ESCALATED_TO_RECALL + 创建 Recall）。
  - **BizModel 内重复 guard**：`ErpQaNonConformanceBizModel:119-131` 有 private `requireNcrStatus`/`illegalNcrTransition` 重复。
  - **领域错误码**：`ERR_INVALID_NCR_STATUS_TRANSITION`（`:57-60`，参数 ncrCode/currentStatus/expectedStatus）。
  - **无矩阵测试**（NCR 被间接覆盖于 `TestErpQaNcrCapaEndToEnd`）。
- **ErpQaRecall**（M4.61 status + M4.62 approveStatus，双轴，dedicated approval-orchestrator-facade）：
  - **status 5 态**（`erp-qa/recall-status`）：OPEN/APPROVED/IN_PROGRESS/CLOSED/CANCELLED。
  - **approveStatus 4 态**（`wf/approve-status`）：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED。
  - **审批轴 writer（skeleton Pattern B 路径，5 Processor）**：`ErpQaRecall{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval}Processor` 各 extends platform `Abstract{Xxx}Processor`，public 方法覆写调 **dedicated orchestrator-facade** `ErpQaRecallProcessor` 的 protected `validateTransitionForXxx:68-105` + `doXxx:119-150`。目标态写入：`doSubmit:119`（SUBMITTED）、`doApprove:129`（APPROVED + status=APPROVED + approvedBy/At）、`doReject:137`（REJECTED + status=CANCELLED + approvedBy/At）、`doReverseApprove:145`（REJECTED + 清 approvedBy/At）、`doWithdrawSubmit:124`（UNSUBMITTED）。xbiz `ErpQaRecall.xbiz` 直接 inject 各 Processor（不经 BizModel）。
  - **操作轴 writer（Processor abstract 路径，5 Processor）**：`ErpQaRecall{Register,LocateTargets,NotifyCustomers,GenerateReturns,Close}Processor` 各 extends `AbstractErpQaRecallProcessor:27-71`。Register 写 status=OPEN + approveStatus=UNSUBMITTED；LocateTargets 守卫 status=APPROVED→IN_PROGRESS；Close 守卫 IN_PROGRESS→CLOSED（notify gate）。BizModel `cancel:60-71` 直入写 status=CANCELLED（inline guard OPEN/APPROVED/IN_PROGRESS）。
  - **领域错误码**：`ERR_INVALID_RECALL_STATUS_TRANSITION`（`erp.err.qa.recall.illegal-status-transition`，`:99-102`，参数 recallCode/currentStatus/expectedStatus）+ `ERR_RECALL_APPROVAL_REQUIRED`/`ERR_RECALL_NOTIFY_INCOMPLETE`。
  - **既有测试**：`TestErpQaRecallStateMachine`（189 行，7 tests，GraphQL 集成）。
- **既有 Bean 注册**：`_vfs/erp/qa/beans/app-service.beans.xml`（115 行）已注册各 Processor + `ErpQaRecallProcessor` orchestrator-facade + NCR posting services。**零 SM Bean 注册**（greenfield）。5 实体轴 SM Bean 全部新建。
- **common 层非法迁移码**：`ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`，先例复用。
- **合规基线**：`docs/audits/compliance-baseline.md` R5=0、R11=0。
- **owner doc 覆盖**：`docs/design/quality/state-machine.md` §适用对象一（Inspection 4 态 result 完整）+ §适用对象二（NCR 5 态含召回升级）+ §实现约定（result 反馈经 business 查 quality 而非事件驱动；强制质检 config-gated；NCR 过账引擎 AUTO_POST/MANUAL_POST；让步审批简化为 approveStatus=APPROVED）。

## Goals

- 为 3 个质量实体的 5 条状态轴各落地一个实体级 `ErpQa*StateMachine` Bean（一 Bean 对一实体一轴），承载命名动作迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态。
  - `ErpQaInspectionResultStateMachine`（result 轴，recordResult/passInspection/failInspection）——M4.58 docStatus 经 Decision 裁定并入或排除
  - `ErpQaInspectionApprovalStateMachine`（approveStatus 轴，concession-approve）——M4.59 裁定迁移范围
  - `ErpQaNonConformanceStateMachine`（status 单轴，submitReview/resolve/postNcr/reverseNcr/upgradeToRecall/cancel）
  - `ErpQaRecallStateMachine`（status 轴，register/locateTargets/notifyCustomers/close/cancel）
  - `ErpQaRecallApprovalStateMachine`（approveStatus 轴，5 审批动作）
- 将固定来源态/目标态判断改调 Bean：**三路径接线**——(A) Recall 审批轴经 dedicated orchestrator-facade `ErpQaRecallProcessor.validateTransitionForXxx` 改调 Bean（M4 采购 facade 路径同构）；(B) Inspection result + NCR status + Recall 操作轴经本地 abstract protected guard / BizModel inline guard 改调 Bean（M3 Request abstract 路径同构）；(C) Recall xbiz 审批 Processor 接线不动（仍调 orchestrator-facade，facade 内部改调 Bean）。**动态业务守卫与副作用保留原位**（CAPA gate、NcrPostingDispatcher、NcrReturnOrchestrator、Recall target locator、notify gate、NCR auto-create）。
- 层 2 四方对照逐实体逐轴裁定。
- 新增层 1 矩阵完备性表驱动测试（greenfield，5 个 Bean）；层 3 既有集成测试全绿回归。
- 保持全部既有外部行为不变。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml。
- 不迁移 `posted`（§11.2 M4 (iii)）；过账编排保留在 `NcrPostingDispatcher`/`NcrReturnOrchestrator` + Processor 原位。
- 不修改共享骨架 `Abstract{Xxx}Processor`（module-common-service 零改动）。
- 不改变 NCR 过账模式（AUTO_POST/MANUAL_POST config-gated）、退货编排路径、Recall 召回流程。
- 不实现业务单据作废联动取消质检（owner doc Deferred successor）。
- 不引入强制质检事件驱动反馈（owner doc Deferred——business 查 quality 保持）。
- 不引入全局 CRUD 写锁（M0.1 successor）。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）。
- 不证 Delta 覆盖（M4 保护域单项，归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M0.2 清单 + M1.3 模板 §11 + M4 采购审批 facade 先例 + M3 Request abstract 先例；落地 5 个单实体单轴 Bean + 三路径接线 + 测试 + 四方对照；不改契约/模型/公共 API/共享骨架。**M4 plan-first**）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 模板 + §11.2 M4 变体 + §1 双轴约定）、`docs/design/quality/state-machine.md`（§Inspection + §NCR + §实现约定）、`docs/design/domain-design-guidelines.md`（§16.4）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（QA-1/2/3/4/5）、`docs/architecture/processor-extension-pattern.md`、`docs/skills/state-machine-business-review-prompt.md`、`docs/plans/2026-08-13-1950-1-purchase-m4-approvestatus-state-machine-bean.md`（facade 路径先例）、`docs/plans/2026-08-13-0805-1-erpmnt-request-state-machine-bean.md`（abstract 路径先例）
- Skill Selection Basis: 路线图 M4.58-62 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「Processor/facade/BizModel 接线、Bean 注册、`@Inject` 非 private、跨实体调用边界、错误码、事务边界、过账副作用保留、产品化可定制性自检」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成测试回归」。`state-machine-business-review-prompt.md` 匹配层 2 四方对照。必需输入均已就绪。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护质量业财过账行为（NCR resolve 触发报废损失/退货过账 + Recall close→销售退货过账 + Inspection posted 三件套 + reverseNcr 红冲）。在人工/owner-doc 确认前为阻塞前置。
- 无端口/环境变量/CORS/密钥/.env/外部服务依赖。无数据迁移。

## Execution Plan

### Phase 1 - ErpQaInspection result + approveStatus Bean（M4.58 + M4.59）

Status: planned
Targets: `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/statemachine/ErpQaInspection{Result,Approval}StateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/AbstractErpQaInspectionProcessor.java`、`.../processor/ErpQaInspection{RecordResult,PassInspection,FailInspection}Processor.java`、`.../test/.../statemachine/TestErpQaInspection{Result,Approval}StateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done（已满足）

- [ ] `Decision`（Inspection 三字段轴迁移范围裁决）：(A) **M4.58 docStatus 轴**：实仓零状态机 writer（仅测试 seed ACTIVE）——裁定是否为 DRAFT→ACTIVE/CANCELLED 建立 Bean 矩阵，或裁定为 dict-only 泛型占位轴排除迁移（对齐 M0.2 §5.1 死状态登记范式）。若裁定排除，M4.58 标记为 `deferred-but-adjudicated` 并登记 successor。(B) **M4.59 approveStatus 轴**：仅 concession-approve 单边（CONDITIONAL 时写 APPROVED），非完整 5 动作生命周期——裁定 Bean 迁移范围（仅 concession-approve 边 vs 完整 5 动作矩阵据实仓 writer 推导）。(C) **result 轴**（实仓驱动的实际状态机）虽然 M0.2 未单列为独立工作项，但其固定迁移判断是 Inspection 核心迁移目标——Bean 命名为 `ErpQaInspectionResultStateMachine`。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] `Add`：落地 `ErpQaInspectionResultStateMachine` Bean——3 动作矩阵（recordResult PENDING→ACCEPTED/CONDITIONAL/REJECTED、passInspection PENDING→ACCEPTED、failInspection PENDING→REJECTED）+ `assertCanRecordResult/PassInspection/FailInspection` + `*TargetStatus()` + `transitions()` + 终态={ACCEPTED, CONDITIONAL, REJECTED}。严格无状态。
  - Skill: `nop-backend-dev`
- [ ] `Add`：按 Phase 1 Decision 裁定落地 `ErpQaInspectionApprovalStateMachine`（concession-approve 单边或完整矩阵）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：在 `_vfs/erp/qa/beans/app-service.beans.xml` 注册 Bean。
  - Skill: `nop-backend-dev`
- [ ] `Decision | Add`（接线）：`AbstractErpQaInspectionProcessor.requireInspectionPending:65-70` + `illegalInspectionTransition:58-63` 改调 Bean `assertCanXxx`（try/catch common 码 → 领域码 `ERR_INVALID_INSPECTION_STATUS_TRANSITION`）；RecordResult/PassInspection/FailInspection Processor 目标态改调 Bean `*TargetStatus()`。posted 三件套写入 + NCR auto-create 保留原位。abstract 注入 `@Inject` Bean（非 private）。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：层 1 矩阵完备性 + 层 2 四方对照（dict `erp-qa/inspection-result` + `wf/approve-status` ↔ owner doc §Inspection ↔ Bean ↔ 全部 writer）。
  - Skill: `nop-testing` + `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] Inspection result/approveStatus Bean 存在/注册/无状态；abstract + Processor 委托 Bean。
- [ ] Inspection 层 1 矩阵测试本地全绿。

### Phase 2 - ErpQaNonConformance status Bean（M4.60）

Status: planned
Targets: `.../statemachine/ErpQaNonConformanceStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/AbstractErpQaNonConformanceProcessor.java`、`.../entity/ErpQaNonConformanceBizModel.java`、`.../test/.../statemachine/TestErpQaNonConformanceStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（Inspection 范式已固化）

- [ ] `Decision`（NCR 双 guard 源统一）：(A) BizModel inline guard（`submitReview:50`/`escalateToRecall:81`/`cancel:98`）+ Processor abstract guard（`requireNcrStatus:39`）两套重复——统一改调 Bean，BizModel 重复 private guard 方法移除或委托 Bean。(B) `resolve` 的 CAPA gate + posting dispatch 为动态业务守卫保留原位（非固定迁移边）。(C) `postNcr`/`reverseNcr` 守卫 require RESOLVED + posted 状态——固定来源态守卫改调 Bean，posted 判定保留原位。(D) `upgradeToRecall` 跨实体创建 Recall 副作用保留原位。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] `Add`：落地 `ErpQaNonConformanceStateMachine`——6 动作矩阵（submitReview OPEN→IN_REVIEW、resolve IN_REVIEW→RESOLVED、postNcr RESOLVED→(RESOLVED 不变 + posted)、reverseNcr posted→(RESOLVED 不变)、upgradeToRecall IN_REVIEW→ESCALATED_TO_RECALL、cancel OPEN/IN_REVIEW→CANCELLED）+ `assertCanXxx` + `*TargetStatus()` + `transitions()` + 终态={RESOLVED, ESCALATED_TO_RECALL, CANCELLED}。注册 1 Bean。
  - Skill: `nop-backend-dev`
- [ ] `Add`（接线）：abstract `requireNcrStatus:39`/`illegalNcrTransition:46` + BizModel 重复 guard 改调 Bean；4 Processor 目标态改调 Bean。CAPA gate + NcrPostingDispatcher + NcrReturnOrchestrator + Recall auto-create 保留原位。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：层 1 矩阵完备性 + 层 2 四方对照。
  - Skill: `nop-testing` + `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] NCR Bean 存在/注册/无状态；abstract + BizModel + Processor 委托 Bean，重复 guard 统一。
- [ ] NCR 层 1 矩阵测试本地全绿。

### Phase 3 - ErpQaRecall status + approveStatus Bean（M4.61 + M4.62）

Status: planned
Targets: `.../statemachine/ErpQaRecall{State,Approval}StateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpQaRecallProcessor.java`、`.../processor/AbstractErpQaRecallProcessor.java`、`.../processor/ErpQaRecall{Register,LocateTargets,NotifyCustomers,GenerateReturns,Close}Processor.java`、`.../entity/ErpQaRecallBizModel.java`、`.../test/.../statemachine/TestErpQaRecall{State,Approval}StateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1-2（Inspection/NCR 范式已固化；Recall 双轴 + orchestrator-facade 最复杂）

- [ ] `Decision`（Recall orchestrator-facade 接线点 + 双轴联动裁决）：(A) 审批轴经 dedicated orchestrator-facade `ErpQaRecallProcessor.validateTransitionForXxx:68-105` 改调 `ErpQaRecallApprovalStateMachine`——对齐 M4 采购 facade 路径先例。(B) `doApprove:129` 联动写 approveStatus=APPROVED + status=APPROVED、`doReject:137` 联动写 approveStatus=REJECTED + status=CANCELLED——Bean 按**单轴**建模，联动写入保留在 facade `doXxx` 原位。(C) 操作轴经 `AbstractErpQaRecallProcessor` + BizModel inline guard 改调 `ErpQaRecallStateMachine`。(D) xbiz 审批 Processor 接线不动（仍调 orchestrator-facade，facade 内部改调 Bean）。(E) `reverseApprove` 目标态=REJECTED（实仓 `:145`）。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] `Add`：落地 `ErpQaRecallApprovalStateMachine`（5 动作审批矩阵，reverseApprove=REJECTED）+ `ErpQaRecallStateMachine`（status 操作矩阵：register→OPEN、approve→APPROVED、locateTargets APPROVED→IN_PROGRESS、close IN_PROGRESS→CLOSED、reject→CANCELLED、cancel OPEN/APPROVED/IN_PROGRESS→CANCELLED）+ `assertCanXxx` + `transitions()` + 终态。注册 2 Bean。
  - Skill: `nop-backend-dev`
- [ ] `Add`（接线）：orchestrator-facade `ErpQaRecallProcessor.validateTransitionForXxx:68-105` + `doXxx:119-150` 目标态改调 Bean；操作轴 abstract `AbstractErpQaRecallProcessor.requireRecallStatus:49` + BizModel `cancel:60` inline guard 改调 Bean。同时统一/移除 BizModel 内重复的 private guard 方法（对齐 Phase 2 NCR 重复 guard 统一范式）。target locator + notify gate + generateReturns 保留原位。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：层 1 矩阵完备性（2 Bean 独立测试）+ 层 2 四方对照。
  - Skill: `nop-testing` + `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] 2 Recall Bean 存在/注册/无状态；orchestrator-facade + abstract + BizModel 委托 Bean。
- [ ] Recall 层 1 矩阵测试本地全绿。

### Phase 4 - 层 3 既有命名动作回归

Status: planned
Targets: `module-quality/erp-qa-service/src/test/`（既有集成测试，零新建）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1-3（三实体 5 轴 Bean + 接线已落地）

- [ ] `Proof`：层 3 既有命名动作回归——复用 `TestErpQaInspectionStateMachine`/`TestErpQaRecallStateMachine`/`TestErpQaNcrCapaEndToEnd` 等，证明 Processor 写回、审计、领域错误码、NCR auto-create、posting dispatch、Recall 编排副作用不变。本地 `mvn test -pl module-quality/erp-qa-service -am` 全绿。
  - Skill: `nop-testing`
- [ ] `Proof`：五轴一致性复核——5 Bean 命名/注册/无状态/元数据形状一致；三路径接线（abstract/orchestrator-facade/BizModel-inline）可追溯。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 层 3 既有集成测试全绿（零行为回归）。

## Draft Review Record

- Independent draft review iteration 1: `accept` (`ses_003da6411ffe1qRZSPoo0hWz5P`) — 零信任实仓核实全部 baseline 声明（Inspection 三字段 + zero-writer docStatus + concession-only approveStatus；NCR BizModel inline + abstract dual guard；Recall orchestrator-facade + xbiz 直接注入；错误码；零 SM Bean 注册；§11.2 M4 治理；Deferred 诚实性均 pass）。无 BLOCKER / MAJOR。3 MINOR 已修正：(1) Phase 3 ambiguous 行引用 `requireRecallStatus` 已拆分为 `ErpQaRecallProcessor:68-105` + `AbstractErpQaRecallProcessor:49` 两文件；(2) Phase 3 补充 Recall BizModel 重复 guard 统一 callout（对齐 Phase 2 NCR 范式）；(3) Inspection test count 11→13。

## Closure Gates

- [ ] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)）
- [ ] 范围内行为完成（三实体 5 轴 Bean + 三路径接线 + 层 1 矩阵 + 层 2 四方对照 + 层 3 回归）
- [ ] 相关文档对齐（roadmap M4.58-62 → done）
- [ ] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl module-quality/erp-qa-service -am` 全绿 + `bash docs/audits/nop-compliance-checker.sh` 全 19 规则 actual ≤ baseline
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### Inspection docStatus 零-writer 占位轴

- Classification: `watch-only residual | dict-only placeholder`
- Why Not Blocking Closure: 实仓零状态机 writer（仅测试 seed ACTIVE），M4.58 经 Decision 裁定排除迁移或建立最小矩阵。
- Successor Required: yes（触发条件 = PM 要求 Inspection docStatus 参与生命周期迁移时）

### reverseApprove 共享骨架 §16.4 合规化

- Classification: `confirmed live defect moved to explicit successor ownership`
- Why Not Blocking Closure: Recall `doReverseApprove:145` 已覆写=REJECTED。与 M3/M4 先例同源 successor。
- Successor Required: yes（触发条件 = 独立「reverseApprove 骨架 §16.4 合规化」plan）

### 业务单据作废联动取消质检

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc §4 Deferred——跨域跨表面实现。残留质检单经 useLogicalDelete 手工清理。
- Successor Required: yes（触发条件 = 业务作废自动取消质检需求时）

### Delta 覆盖运行时实证

- Classification: `optimization candidate`
- Why Not Blocking Closure: M4 保护域单项 Delta 可选；归 M5.3。
- Successor Required: yes（触发条件 = M5.3）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 裁定选项 (c) 显式排除。
- Successor Required: no

## Closure

Status Note: <why the plan can close>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <无非阻塞跟进；Deferred 项均为既定 successor>
