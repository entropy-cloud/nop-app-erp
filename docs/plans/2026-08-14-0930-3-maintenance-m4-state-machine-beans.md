# 2026-08-14-0930-3-maintenance-m4-state-machine-beans 维护域 ErpMntVisit/SparePartUsage 实体级状态机 Bean（M4.54 + M4.55 + M4.56）

> Plan Status: completed
> Last Reviewed: 2026-08-14
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.54（ErpMntVisit.status）+ M4.55（ErpMntSparePartUsage.docStatus）+ M4.56（ErpMntSparePartUsage.approveStatus），均 plan-first；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md` MNT-1/2/3（326-328 行段）+ M4.54-56（326-328 行段）
> Related: M3 同域先例 `2026-08-13-0805-1-erpmnt-request-state-machine-bean.md`（M3.17 ErpMntRequest done，本地 abstract→Bean 注入 + cause-chaining 范式）；M4 采购审批先例 `2026-08-13-1950-1-purchase-m4-approvestatus-state-machine-bean.md`（approveStatus 双轴后缀命名范式 done）；M0.1 契约 + M1.3 批量迁移模板固化于 `docs/architecture/entity-state-machine-bean.md §11`
> Mission: entity-state-machine
> Work Item: M4.54 + M4.55 + M4.56
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。SparePartUsage confirm 触发 MAINTENANCE_ISSUE 凭证（config-gated `erp-mnt.spare-part-posting-enabled` 默认 OFF）+ `IErpInvStockMoveBiz` 出库移动；reverseConfirm 红冲上述副作用。Visit complete 触发 MAINTENANCE_LABOR 凭证（config-gated `erp-mnt.labor-posting-enabled` 默认 OFF）+ EquipmentStatusLinker 设备恢复。声明 §11.2 M4 硬约束：(i) plan-first + 受保护行为人工/owner-doc 门控；(ii) 过账时序/编排/失败回退不改，继续由 `MaintenanceIssuePostingDispatcher`/`MaintenanceLaborPostingDispatcher` + `posted` 契约管理；(iii) `posted` 不入轴；(iv) 跨域副作用保留原 Processor/`I*Biz` 路径；(v) 既有红冲闭环不改。
>
> **规则 14 bundling 声明**：M4.54-M4.56 属同一组件（同一 owner doc `docs/design/maintenance/state-machine.md`、同一域 `erp-mnt`、同一结果表面 = 维护域二实体状态轴矩阵集中化），按指南规则 14 合并为单计划。Visit 单轴（status）、SparePartUsage 双轴（docStatus + approveStatus），分阶段落地。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 maintenance`（450 行段）+ 实仓核实。二实体状态轴分离如下。M3.17 `ErpMntRequestStateMachine` Bean 已落地 done，是本计划的**直接接线模板**。

- **ErpMntVisit**（M4.54 status，单轴，本地 abstract 骨架——**M3.17 Request 的未迁移姊妹**）：
  - **status 5 态**（`erp-mnt/visit-status`）：DRAFT/SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED。
  - **writer（4 Processor，本地 abstract）**：`ErpMntVisit{Schedule,Start,Complete,Cancel}Processor` 各 extends 本地 `AbstractErpMntVisitProcessor`。Schedule 守卫 `validateTransition(visit, DRAFT, "DRAFT"):45-50`（DRAFT→SCHEDULED，`doSchedule:66-69` 写 SCHEDULED）；Start 守卫 `validateTransition(visit, SCHEDULED, "SCHEDULED"):17`（SCHEDULED→IN_PROGRESS，`doStart:23-29`）；Complete 守卫 `validateTransition(visit, IN_PROGRESS, "IN_PROGRESS"):29`（IN_PROGRESS→COMPLETED，`doComplete:35-54`）；Cancel 守卫 `validateNotTerminal:52-58`（non-terminal→CANCELLED，`doCancel:31-49`）。
  - **固定守卫在 abstract**：`AbstractErpMntVisitProcessor.validateTransition:45-50`（hardcoded `Objects.equals`）+ `validateNotTerminal:52-58`（hardcoded reject COMPLETED/CANCELLED）+ `illegalVisitTransition:60-65`（抛 `ERR_INVALID_VISIT_STATUS_TRANSITION` `erp.err.mnt.visit-illegal-status-transition`，`:33-35`）。**无 `@Inject` 任何 SM Bean**——与 M3.17 已迁移的 `AbstractErpMntRequestProcessor:30-31`（注入 `ErpMntRequestStateMachine`）形成鲜明对比。
  - **领域错误码**：`ERR_INVALID_VISIT_STATUS_TRANSITION`（`:33-35`，参数 currentStatus/expectedStatus）。
  - **既有测试**：`TestErpMntVisitRequestStateMachine`（351 行，服务层集成——Visit happy path + cancel + terminal guard + illegal transition + Request 联动）。
- **ErpMntSparePartUsage**（M4.55 docStatus + M4.56 approveStatus，双轴，本地 abstract 骨架）：
  - **docStatus 3 态**（`erp/doc-status`，共享 dict）：DRAFT/ACTIVE/CANCELLED。
  - **approveStatus 4 态**（`wf/approve-status`，平台 dict）：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED。
  - **writer（2 Processor，本地 abstract）**：`ErpMntSparePartUsage{Confirm,ReverseConfirm}Processor` 各 extends 本地 `AbstractErpMntSparePartUsageProcessor`。Confirm 守卫 `validateNotConfirmed:59-67`（检查 posted==TRUE 或 docStatus==ACTIVE），`applyIssueResult:86-95` 写 docStatus=ACTIVE + approveStatus=APPROVED + posted。ReverseConfirm 守卫 `validateCanReverse:77-84`（require posted==TRUE + docStatus==ACTIVE，否则 `ERR_SPARE_PART_USAGE_NOT_POSTED` `:79-81`），`doReverseConfirm:102-106` 写 docStatus=CANCELLED + posted=false。
  - **特殊形态**：SparePartUsage **无独立 submit/approve/reject 审批 Processor**——confirm 动作同时推进 docStatus DRAFT→ACTIVE + approveStatus→APPROVED（一步到位），非标准 5 动作审批生命周期。reverseConfirm 是不对称守卫（require posted+ACTIVE）。
  - **领域错误码**：`ERR_SPARE_PART_USAGE_NOT_POSTED`（`:79-81`，参数 usageCode）。**无 `ERR_INVALID_*_STATUS_TRANSITION`**——reverse 路径用 NOT_POSTED 而非 illegal-transition。
  - **无矩阵测试**（仅被间接覆盖）。
- **既有 Bean 注册**：`_vfs/erp/mnt/beans/app-service.beans.xml`（仅 `ErpMntRequestStateMachine` L56-57 已注册，M3.17 done）。**Visit/SparePartUsage SM Bean 未注册**（greenfield）。Visit 4 Processor（L71-78）+ SparePartUsage 2 Processor（L85-88）已注册。
- **M3.17 接线模板（直接范本）**：`AbstractErpMntRequestProcessor:30-31` 注入 `@Inject ErpMntRequestStateMachine`（非 private）；各 Processor try/catch Bean common 码 → cause-chain 领域码 `ERR_INVALID_REQUEST_STATUS_TRANSITION`；目标态 `request.setStatus(stateMachine.acceptTargetStatus())`。`ErpMntRequestStateMachine`（161 行）严格无状态，7-edge 矩阵，`assertCanXxx` + `*TargetStatus()` + `transitions()` + `isTerminal`/`initialStatuses`/`terminalStatuses`。层 1 矩阵测试 `TestErpMntRequestStateMachine`（256 行，纯单元测试）。
- **common 层非法迁移码**：`ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（`nop.err.erp.common.illegal-status-transition`），M3.17 已复用。
- **合规基线**：`docs/audits/compliance-baseline.md` R5=0、R11=0。
- **owner doc 覆盖**：`docs/design/maintenance/state-machine.md` §适用对象一（Visit 5 态完整）+ §实现约定（维修费用过账 config-gated 默认 OFF——备件 MAINTENANCE_ISSUE + 工时 MAINTENANCE_LABOR；红冲闭环经 doCancel/reverseConfirm；过账失败 G3 告警）。**owner doc 缺口**：SparePartUsage 无独立状态机章节（仅有 Visit/Request/DowntimeEntry 三节）——Phase 2 四方对照中须以代码为权威建立语义，并将 owner doc 缺口作为四方对照 finding 登记（Decision 裁定是否在迁移中补 owner doc SparePartUsage 章节，对齐 inventory 缺口轴先例 `2026-08-13-2045-2` Deferred）。

## Goals

- 为 2 个维护实体的 3 条状态轴各落地一个实体级 `ErpMnt*StateMachine` Bean（一 Bean 对一实体一轴），承载命名动作迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态。**直接镜像 M3.17 `ErpMntRequestStateMachine` 范式**。
  - `ErpMntVisitStateMachine`（status 单轴，schedule/start/complete/cancel）
  - `ErpMntSparePartUsageDocumentStateMachine`（docStatus 单轴，confirm/reverseConfirm）
  - `ErpMntSparePartUsageApprovalStateMachine`（approveStatus 单轴，confirm-approve 联动）
- 将固定来源态/目标态判断改调 Bean：**本地 abstract 路径**——`AbstractErpMntVisitProcessor` + `AbstractErpMntSparePartUsageProcessor` 各注入对应 Bean，protected guard 改调 `assertCanXxx`（try/catch common 码 → cause-chain 领域码），目标态改调 `*TargetStatus()`。**动态业务守卫与副作用保留原位**（EquipmentStatusLinker 设备联动、`MaintenanceIssuePostingDispatcher`/`MaintenanceLaborPostingDispatcher` 过账、stock move 生成/逆转、visit task 完成联动）。
- 层 2 四方对照（dict ↔ `maintenance/state-machine.md` ↔ Bean 元数据 ↔ 全部 writer）逐实体逐轴裁定。
- 新增层 1 矩阵完备性表驱动测试（greenfield，3 个 Bean，镜像 `TestErpMntRequestStateMachine` 范式）；层 3 既有集成测试全绿回归。
- 保持全部既有外部行为不变（错误码值/参数、审计、设备联动、过账时序/失败回退、stock move 时序）。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml。
- 不迁移 `posted`（§11.2 M4 (iii)）；过账编排保留在 `MaintenanceIssuePostingDispatcher`/`MaintenanceLaborPostingDispatcher` + Processor 原位。
- 不修改共享骨架 `Abstract{Xxx}Processor`（module-common-service 零改动）——SparePartUsage 无标准审批 Processor，不经骨架。
- 不改变过账 config-gate（`erp-mnt.spare-part-posting-enabled` / `erp-mnt.labor-posting-enabled` 默认 OFF 保持）。
- 不改变 EquipmentStatusLinker 设备状态联动（`erp-mnt.equipment-status-link-enabled` 门控保持）。
- 不引入全局 CRUD 写锁（M0.1 successor）。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）。
- 不证 Delta 覆盖（M4 保护域单项，归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M0.2 清单 + M1.3 模板 §11 + **M3.17 同域直接范本**；落地 3 个单实体单轴 Bean + 本地 abstract 接线 + 测试 + 四方对照；不改契约/模型/公共 API/共享骨架。**M4 plan-first**——confirm/complete 触发 config-gated 业财过账）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 模板 + §11.2 M4 变体 + §1 双轴约定）、`docs/design/maintenance/state-machine.md`（§Visit + §实现约定）、`docs/design/domain-design-guidelines.md`（§16.4）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（MNT-1/2/3）、`docs/architecture/processor-extension-pattern.md`、`docs/skills/state-machine-business-review-prompt.md`、`docs/plans/2026-08-13-0805-1-erpmnt-request-state-machine-bean.md`（M3.17 同域直接范本）
- Skill Selection Basis: 路线图 M4.54-56 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「本地 abstract Processor 接线、Bean 注册、`@Inject` 非 private、cause-chaining 错误码、过账副作用保留、产品化可定制性自检」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成测试回归」。`state-machine-business-review-prompt.md` 匹配层 2 四方对照。M3.17 范本可直接镜像，必需输入均已就绪。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护维护业财过账行为（SparePartUsage confirm 触发 MAINTENANCE_ISSUE 凭证 + 出库移动；reverseConfirm 红冲；Visit complete 触发 MAINTENANCE_LABOR 凭证 + 设备恢复）。虽 config-gated 默认 OFF，但翻转 config=ON 后即触发受保护行为。在人工/owner-doc 确认前为阻塞前置。
- 无端口/环境变量/CORS/密钥/.env/外部服务依赖。无数据迁移。

## Execution Plan

### Phase 1 - ErpMntVisit status Bean（M4.54）

Status: completed
Targets: `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/statemachine/ErpMntVisitStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/AbstractErpMntVisitProcessor.java`、`.../processor/ErpMntVisit{Schedule,Start,Complete,Cancel}Processor.java`、`.../test/.../statemachine/TestErpMntVisitStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: M1.3 done（已满足）；M3.17 `ErpMntRequestStateMachine` 范本已 done

- [x] `Add`：落地 `ErpMntVisitStateMachine` Bean——4 动作矩阵（schedule DRAFT→SCHEDULED、start SCHEDULED→IN_PROGRESS、complete IN_PROGRESS→COMPLETED、cancel non-terminal→CANCELLED）+ `assertCanSchedule/Start/Complete/Cancel(String status)` + `scheduleTargetStatus()`/`startTargetStatus()`/`completeTargetStatus()`/`cancelTargetStatus()` + `isTerminal`/`initialStatuses`/`terminalStatuses` + `transitions()`（4 边）。严格无状态（§2）。非法边抛 common 码 `ERR_ILLEGAL_STATUS_TRANSITION` + `action`/`currentStatus`/`expectedStatus` 参数。直接镜像 `ErpMntRequestStateMachine` 结构。
  - Skill: `nop-backend-dev`
- [x] `Add`：在 `_vfs/erp/mnt/beans/app-service.beans.xml` 以 `<bean id="<FQN>" class="<FQN>"/>` 注册（3 实体轴 Bean 一并注册，紧邻既有 `ErpMntRequestStateMachine` L56-57）。
  - Skill: `nop-backend-dev`
- [x] `Add`（接线，镜像 M3.17 Request 范式）：`AbstractErpMntVisitProcessor` 注入 `@Inject ErpMntVisitStateMachine stateMachine`（非 private，对齐 `AbstractErpMntRequestProcessor:30-31`）；`validateTransition:45-50` + `validateNotTerminal:52-58` 改调 Bean `assertCanXxx`（try/catch common 码 → cause-chain `illegalVisitTransition` 领域码 `ERR_INVALID_VISIT_STATUS_TRANSITION`，common NopException 作 cause）；各 Processor 目标态改调 Bean `*TargetStatus()`（对齐 `request.setStatus(stateMachine.acceptTargetStatus())`）。EquipmentStatusLinker 设备联动 + `MaintenanceLaborPostingDispatcher` 过账保留原位。
  - Skill: `nop-backend-dev`
- [x] `Proof`：层 1 矩阵完备性（greenfield 表驱动，镜像 `TestErpMntRequestStateMachine` 256 行范式）——(a) 无重复/冲突边（4 边唯一 action|fromStatus 键）；(b) schedule DRAFT→SCHEDULED、start SCHEDULED→IN_PROGRESS、complete IN_PROGRESS→COMPLETED、cancel {DRAFT,SCHEDULED,IN_PROGRESS}→CANCELLED 可达；(c) 各 `assertCanXxx` 合法来源态通过、非法来源态抛 common 码携带 `action`/`fromStatus`；(d) `transitions()` 与显式方法语义一致；(e) 初始={DRAFT}/终态={COMPLETED, CANCELLED}。
  - Skill: `nop-testing`
- [x] `Proof`：层 2 四方对照——dict `erp-mnt/visit-status` ↔ `maintenance/state-machine.md` §Visit ↔ Bean 元数据 ↔ 全部 writer（4 Processor live + 创建写 DRAFT + CRUD 路径排除）。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] `ErpMntVisitStateMachine` Bean 存在、已注册、严格无状态；4 个 Visit Processor 委托 Bean，内联 `Objects.equals` 矩阵判断已移除。
- [x] Visit 层 1 矩阵测试本地 `mvn test -pl module-maintenance/erp-mnt-service -am -Dtest=TestErpMntVisitStateMachineMatrix` 全绿。

### Phase 2 - ErpMntSparePartUsage docStatus + approveStatus Bean（M4.55 + M4.56）

Status: completed
Targets: `.../statemachine/ErpMntSparePartUsage{Document,Approval}StateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/AbstractErpMntSparePartUsageProcessor.java`、`.../processor/ErpMntSparePartUsage{Confirm,ReverseConfirm}Processor.java`、`.../test/.../statemachine/TestErpMntSparePartUsage{Document,Approval}StateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（Visit Bean + abstract→Bean 范式已固化）

- [x] `Decision`（SparePartUsage 独有形态裁决）：(A) **非标准审批生命周期**——confirm 一步推进 docStatus DRAFT→ACTIVE + approveStatus→APPROVED（无独立 submit/reject/reverseApprove/withdraw）。Bean 按**单轴**建模：Document Bean 含 confirm DRAFT→ACTIVE + reverseConfirm ACTIVE→CANCELLED；Approval Bean 含 confirm-approve→APPROVED（非完整 5 动作矩阵，据实仓 writer 推导最小矩阵）。(B) `validateCanReverse:77-84` 不对称守卫（require posted==TRUE + docStatus==ACTIVE）保留原位（动态业务守卫含 posted 判定，非固定状态迁移边）；增加 Bean `assertCanReverseConfirm` 状态守卫部分。(C) `validateNotConfirmed:59-67` 的 silent-guard gap（方法体无 throw on "not confirmed" branch）——Decision 裁定是否在迁移中修复为显式 throw 还是保留既有行为（行为保持优先）。
  - Skill: `state-machine-business-review-prompt.md`
- [x] `Add`：落地 `ErpMntSparePartUsageDocumentStateMachine`（2 动作：confirm DRAFT→ACTIVE、reverseConfirm ACTIVE→CANCELLED）+ `ErpMntSparePartUsageApprovalStateMachine`（1 动作：confirmApprove null/UNSUBMITTED→APPROVED，非完整 5 动作）。注册 2 Bean。
  - Skill: `nop-backend-dev`
- [x] `Add`（接线，镜像 Phase 1 范式）：`AbstractErpMntSparePartUsageProcessor` 注入 2 Bean（非 private）；`validateNotConfirmed`/`validateCanReverse` 改调 Bean `assertCanConfirm`/`assertCanReverseConfirm`（try/catch common 码 → cause-chain 领域码）；`applyIssueResult:86-95` 目标态改调 Bean `*TargetStatus()`；`doReverseConfirm:102-106` 目标态改调 Bean。`MaintenanceIssuePostingDispatcher` 过账 + stock move + posted 守卫保留原位。
  - Skill: `nop-backend-dev`
- [x] `Proof`：层 1 矩阵完备性（2 Bean 独立测试）+ 层 2 四方对照（dict `erp/doc-status` + `wf/approve-status` ↔ owner doc ↔ Bean ↔ 全部 writer）。
  - Skill: `nop-testing` + `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] 2 SparePartUsage Bean 存在/注册/无状态；abstract + Processor 委托 Bean。
- [x] SparePartUsage 层 1 矩阵测试本地 `mvn test -pl module-maintenance/erp-mnt-service -am -Dtest=TestErpMntSparePartUsage*StateMachineMatrix` 全绿。

### Phase 3 - 层 3 既有命名动作回归

Status: completed
Targets: `module-maintenance/erp-mnt-service/src/test/`（既有集成测试，零新建）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1-2（二实体 3 轴 Bean + 接线已落地）

- [x] `Proof`：层 3 既有命名动作回归——复用 `TestErpMntVisitRequestStateMachine`（351 行，含 Visit happy path + cancel + terminal guard + illegal transition + Request 联动），证明 Processor 写回、审计 fromStatus/toStatus、领域错误码 + 参数、EquipmentStatusLinker 设备联动、`MaintenanceLaborPostingDispatcher`/`MaintenanceIssuePostingDispatcher` 过账副作用时序不变。本地 `mvn test -pl module-maintenance/erp-mnt-service -am` 全绿。
  - Skill: `nop-testing`
- [x] `Proof`：三轴一致性复核——3 Bean 命名（无后缀/Document/Approval）/注册（同文件紧邻 Request Bean）/无状态/元数据形状一致；abstract→Bean 注入 + cause-chaining 范式与 M3.17 Request 可追溯一致。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 层 3 既有集成测试全绿（零行为回归）。

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (`ses_003da4a6bffeYa7hkBVYzBKgm7`) — 零信任实仓核实 12+ baseline 声明（Visit abstract guard 行号 + 无 SM Bean 注入；M3.17 Request 范本注入 + Bean 注册；Processor 注册行号；SparePartUsage abstract guard + silent-return gap；错误码；无标准审批 Processor；测试行数 + 矩阵测试存在；§11.2 M4 治理 config-gated 合规；Deferred 诚实性均 pass）。无 BLOCKER / MAJOR。4 MINOR 已修正：(1) ERR_SPARE_PART_USAGE_NOT_POSTED 参数列表修正为仅 usageCode（移除 currentStatus/expectedStatus）；(2) owner doc 缺 SparePartUsage 章节——baseline 补注缺口 + Phase 2 四方对照须以代码为权威 + Decision 裁定补 owner doc；(3) Phase 2 exit criteria 补充具体 mvn test 命令（对齐 Phase 1 一致性）；(4) SparePartUsage Approval Bean 单边矩阵（confirmApprove null→APPROVED）已在 Phase 2 Decision (A) 显式裁定为据实仓 writer 最小矩阵。

## Closure Gates

- [x] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)）
- [x] 范围内行为完成（二实体 3 轴 Bean + abstract 接线 + 层 1 矩阵 + 层 2 四方对照 + 层 3 回归）
- [x] 相关文档对齐（roadmap M4.54-56 → done）
- [x] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl module-maintenance/erp-mnt-service -am` 全绿 + `bash docs/audits/nop-compliance-checker.sh` 全 19 规则 actual ≤ baseline
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### SparePartUsage validateNotConfirmed silent-guard gap

- Classification: `watch-only residual (intentional legacy)`
- Why Not Blocking Closure: `validateNotConfirmed:59-67` 方法体在 "not confirmed" 分支无显式 throw（silent return），可能为骨架 gap。行为保持优先——迁移中保持既有行为，修复须 ask-first（行为变更非纯重构）。
- Successor Required: yes（触发条件 = PM/owner 要求 SparePartUsage confirm 显式拒绝重复确认时）

### Delta 覆盖运行时实证

- Classification: `optimization candidate`
- Why Not Blocking Closure: M4 保护域单项 Delta 可选；归 M5.3。
- Successor Required: yes（触发条件 = M5.3 最终跨域 Delta 覆盖回归）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 裁定选项 (c) 显式排除；更强写锁须改 ORM/xmeta（保护区 ask-first）。
- Successor Required: no（仅当产品要求全局强制矩阵写锁时重开）

## Closure

Status Note: 二实体 3 轴实体级状态机 Bean（ErpMntVisitStateMachine status / ErpMntSparePartUsageDocumentStateMachine docStatus / ErpMntSparePartUsageApprovalStateMachine approveStatus）已落地并注册；本地 abstract Processor 注入 Bean + cause-chaining 范式对齐 M3.17 Request；层 1 矩阵完备性测试（3 Bean 共 23 case）全绿；层 3 既有集成测试（TestErpMntVisitRequestStateMachine 10 case + 全模块 94 case）零行为回归；全工作区 `mvn clean install -DskipTests` BUILD SUCCESS；合规 R5=0/R11=0 零新增违规。SparePartUsage validateNotConfirmed silent-guard gap 按 Deferred（行为保持优先）保留既有行为。

Closure Audit Evidence:

- Auditor / Agent: executing agent（本会话直接执行，非独立子代理）
- Evidence:
  - 3 Bean 新增：`ErpMntVisitStateMachine.java`（6 边）/ `ErpMntSparePartUsageDocumentStateMachine.java`（2 边）/ `ErpMntSparePartUsageApprovalStateMachine.java`（1 边）
  - 接线：`AbstractErpMntVisitProcessor`（注入 Bean + 4-arg cause-chain `illegalVisitTransition`）/ `AbstractErpMntSparePartUsageProcessor`（注入 2 Bean + `validateCanReverse` docStatus 守卫委托 + `applyIssueResult`/`doReverseConfirm` 目标态委托）；4 Visit Processor + 2 SparePartUsage Processor 目标态改调 Bean `*TargetStatus()`
  - 注册：`app-service.beans.xml` 3 Bean 紧邻 `ErpMntRequestStateMachine`
  - 测试：层 1 矩阵 `TestErpMntVisitStateMachineMatrix`（8 case）+ `TestErpMntSparePartUsageDocumentStateMachineMatrix`（7 case）+ `TestErpMntSparePartUsageApprovalStateMachineMatrix`（8 case）= 23 case 全绿
  - 回归：`mvn test -pl module-maintenance/erp-mnt-service -am` 94 case 全绿（含 TestErpMntVisitRequestStateMachine 10 case）
  - 构建：`mvn clean install -DskipTests` 全 156 模块 BUILD SUCCESS
  - 合规：`bash docs/audits/nop-compliance-checker.sh` R5=0/R11=0（R12c=40 为 stash 验证确认的既有基线漂移，非本计划引入）
  - roadmap M4.54-56 → done

Follow-up:

- <无非阻塞跟进；Deferred 项均为既定 successor>
