# 2026-08-14-1931-1-erpast-core-lifecycle-state-machine-beans 资产域核心生命周期 + 跟踪实体状态机 Bean（M4.40 + M4.41 + M4.52 + M4.53）

> Plan Status: completed
> Review Hold: §11.2 M4 (i) 人工/owner-doc 门控**已于 2026-08-14 经人工确认解除**（见 Draft Review Record 门控确认记录）——本计划触及受保护资产/业财过账行为（Asset 资本化→CAPITALIZATION 凭证、处置→DISPOSAL 清理凭证、DepreciationSchedule 折旧→DEPRECIATION 凭证、Inventory 盘盈盘亏过账、Maintenance 维修费用化/资本化凭证；reverseDepreciation/reversePost 红冲上述副作用）。M4 plan-first 门控成立且经人工确认；已转 `active` 进入实施。
> Last Reviewed: 2026-08-14
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.40（ErpAstAsset.status）+ M4.41（ErpAstDepreciationSchedule.status）+ M4.52（ErpAstInventory.status）+ M4.53（ErpAstMaintenance.status），均 plan-first；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md` AST-1/2/12/13（312-313, 324-325 行段）+ M4.40/41/52/53（同）
> Related: M3 同域先例 `2026-08-13-0805-2-erpast-movement-state-machine-beans.md`（M3.15+M3.16 ErpAstMovement 双轴 done，assets 域 INLINE→Bean + 双轴后缀命名范式）；M4 维护先例 `2026-08-14-0930-3-maintenance-m4-state-machine-beans.md`（status 单轴 Bean + 本地 abstract 接线 done）；M0.1 契约 + M1.3 批量迁移模板固化于 `docs/architecture/entity-state-machine-bean.md §11`
> Mission: entity-state-machine
> Work Item: M4.40 + M4.41 + M4.52 + M4.53
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。四实体的 status 轴均触发或被业财过账约束：Asset.status DRAFT→IN_SERVICE（资本化入账凭证）/处置终态（处置清理凭证）；DepreciationSchedule.status PENDING→EXECUTED（DEPRECIATION 折旧凭证）/→REVERSED（红字冲销）；Inventory.status →POSTED（盘点盘盈盘亏过账）/→REVERSED（红冲）；Maintenance.status →POSTED（维修费用化/资本化过账）/→REVERSED（红冲）。声明 §11.2 M4 硬约束：(i) plan-first + 受保护行为人工/owner-doc 门控；(ii) 过账时序/编排/失败回退不改，继续由各 `*PostingDispatcher` + `posted` 契约管理；(iii) `posted` 不入轴（M4.52/M4.53 的 POSTED 是 status 终态值，非 `posted` boolean 字段——见 Decision (A)）；(iv) 跨域副作用（`IErpFinAcctDocProvider`、`IErpInvStockMoveBiz` 库存转固）保留原 Processor/`I*Biz` 路径；(v) 既有红冲闭环不改。
>
> **规则 14 bundling 声明**：M4.40+M4.41+M4.52+M4.53 属同一组件（同一 owner doc `docs/design/assets/state-machine.md`、同一域 `erp-ast`、同一结果表面 = 资产域核心生命周期/跟踪实体 status 轴矩阵集中化），按指南规则 14 合并为单计划。四实体均为 status 单轴（无 approveStatus 审批轴——审批轴归 M4.42-51 同域计划 2/3），分阶段落地。本计划与计划 2（M4.42-47）/计划 3（M4.48-51）按"生命周期轴 vs 审批文档轴"自然分面，避免单计划 14 轴过载。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3 assets`（448 行段）+ 实仓核实。assets 域采用 **Facade + per-mutation Processor 两层结构**（`processor-extension-pattern.md`），**无共享 Abstract*Processor 骨架**（与维护域 Visit 不同）。assets 域既有 1 个 SM Bean：`ErpAstMovementApprovalStateMachine` + `ErpAstMovementDocumentStateMachine`（M3.15+M3.16 done，`statemachine/` 包 + `app-service.beans.xml:97-100` 已注册）。

- **ErpAstAsset**（M4.40 status，单轴——**跨实体 writer 轴**，特殊形态）：
  - **status 5 态**（`erp-ast/asset-status`）：DRAFT/IN_SERVICE/IDLE/SCRAPPED/SOLD。**IDLE = 预留死状态（owner doc §2/§5 Deferred）**——全 `module-assets` 零 `setStatus(...IDLE)` writer，折旧引擎仅查 IN_SERVICE 等价满足「IDLE 默认停提」。
  - **writer（跨实体文档 Processor side-effect，全 5 处）**：(1) `ErpAstAssetCapitalizationProcessor.executeApprove`（资本化审批通过写 `asset.setStatus(IN_SERVICE)`）；(2) `ErpAstAssetCapitalizationProcessor.executeReverseApprove:102`（reverseApprove posted 窗口写 `asset.setStatus(DRAFT)`——**IN_SERVICE→DRAFT 逆资本化**）；(3) `ErpAstDisposalProcessor.executeApprove:76`（处置审批通过写 `asset.setStatus(SCRAPPED/SOLD)`，按 disposalType）；(4) `ErpAstDisposalProcessor.executeReverseApprove:116`（reverseApprove posted 窗口写回 IN_SERVICE）；(5) `ErpAstInventoryProcessor:270`（`handleShortageTriggerDisposal` 盘亏触发处置写现有资产 `asset.setStatus(SCRAPPED)`——**Phase 2 Inventory 接线时须跨阶段调用本计划 Asset Bean**）。`ErpAstAssetBizModel` 为 CrudBizModel 桩（零状态机 mutation），**无 Asset 自有 status Processor**。
  - **无领域 status-transition 错误码**：Asset 无独立 `ERR_ASSET_*_STATUS_TRANSITION`；资本化/处置守卫用 `ERR_CAPITALIZATION_*`/`ERR_DISPOSAL_*` + `ERR_DISPOSAL_ASSET_ALREADY_DISPOSED`（终态不可恢复）。
  - **既有测试**：无 Asset status 矩阵测试（资本化/处置集成测试间接覆盖）。
- **ErpAstDepreciationSchedule**（M4.41 status，单轴，delete-after-extract facade）：
  - **status 4 态**（`erp-ast/depreciation-schedule-status`）：PENDING/EXECUTED/REVERSED/CANCELLED（owner doc §折旧计划条目 3 态 + disposal cancelPendingSchedules 写 CANCELLED）。
  - **writer（4 per-mutation Processor，经 facade 共享 helper）**：`ErpAstDepreciationScheduleExecuteDepreciationProcessor`（PENDING→EXECUTED）；`ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor`（批量 PENDING→EXECUTED）；`ErpAstDepreciationScheduleReverseDepreciationProcessor`（EXECUTED→REVERSED）；`ErpAstDepreciationScheduleRecalculateForCapitalizationMaintenanceProcessor`（资本化/维修后重算）。facade `ErpAstDepreciationScheduleProcessor`（151 行）为 delete-after-extract 共享 protected helper 持有者（无 `:45` 查询）。**disposal/资本化 side-effect 写 CANCELLED**：`ErpAstDisposalProcessor.cancelPendingSchedules:202-211`（PENDING→CANCELLED）+ `restoreCancelledSchedules:213-222`（CANCELLED→PENDING）。
  - **领域错误码**：`ERR_SCHEDULE_ILLEGAL_STATUS_TRANSITION`（`erp.err.ast.schedule.illegal-status-transition`，参数 currentStatus/expectedStatus，无 entityCode 参数）。并发兜底 `ERR_AST_DEPRECIATION_ALREADY_EXECUTED`。
  - **既有测试**：无 status 矩阵测试。
- **ErpAstInventory**（M4.52 status，单轴）：
  - **status 5 态**（`erp-ast/inventory-status`，已实仓核实）：DRAFT/COUNTING/RECONCILING/POSTED/CANCELLED（owner doc `docs/design/assets/inventory.md` §一状态机 + dict + `ErpAstConstants` 一致，**无 REVERSED 态**）。
  - **writer（7 per-mutation Processor + facade）**：CreateInventory（写 DRAFT）/SubmitForCount（写 COUNTING）/Approve（**不迁移**——守卫 RECONCILING 记 approver）/Reconcile（写 RECONCILING）/ProcessVariance（**不迁移**——守卫 RECONCILING 算差异）/Post（写 POSTED，经 `AssetInventoryPostingDispatcher` 过账）/Reverse（**回卷写 RECONCILING**——reverse 为回卷动作非终态，红冲副作用归 Dispatcher.reverse，posted 不入轴）；Cancel 写 CANCELLED（facade `ErpAstInventoryProcessor:83`，非 per-mutation）。
  - **领域错误码**：`ERR_AST_INVENTORY_ILLEGAL_STATUS_TRANSITION`（参数 inventoryCode/currentStatus/expectedStatus）+ `ERR_AST_INVENTORY_NOT_RECONCILED`/`VARIANCE_NOT_PROCESSED`/`ALREADY_POSTED`。
  - **过账**：`AssetInventoryPostingDispatcher`（post + reverse）。
  - **既有测试**：无 status 矩阵测试。
- **ErpAstMaintenance**（M4.53 status，单轴）：
  - **status 6 态**（`erp-ast/maintenance-status`，已实仓核实）：DRAFT/SUBMITTED/IN_PROGRESS/COMPLETED/POSTED/CANCELLED（owner doc `docs/design/assets/maintenance.md` §一状态机 + dict + `ErpAstConstants` 一致，**无 APPROVED/REVERSED 态**）。
  - **writer（8 per-mutation Processor + facade）**：CreateMaintenance（写 DRAFT）/Submit（写 SUBMITTED）/Approve（**不迁移**——守卫 COMPLETED，POSTED 已过账守卫）/DecideTreatment（**不迁移**——守卫 COMPLETED 裁决处置）/StartWork（写 IN_PROGRESS）/CompleteWork（写 COMPLETED）/Post（写 POSTED，经 `MaintenanceExpensePostingDispatcher`/`MaintenanceCapitalizationPostingDispatcher` 过账）/Reverse（**回卷写 COMPLETED**——reverse 为回卷动作非终态，红冲副作用归 Dispatcher.reverse，posted 不入轴）；Cancel 写 CANCELLED（facade `ErpAstMaintenanceProcessor`，非 per-mutation）。
  - **领域错误码**：`ERR_AST_MAINTENANCE_ILLEGAL_STATUS_TRANSITION`（参数 maintenanceCode/currentStatus/expectedStatus）+ `TREATMENT_NOT_DECIDED`/`ALREADY_POSTED`/`ALREADY_REVERSED`/`CAPITALIZE_BELOW_THRESHOLD`。
  - **过账**：`MaintenanceExpensePostingDispatcher` + `MaintenanceCapitalizationPostingDispatcher`（post + reverse）。
  - **既有测试**：无 status 矩阵测试。
- **既有 Bean 注册**：`_vfs/erp/ast/beans/app-service.beans.xml:97-100`（仅 Movement 双轴 2 Bean，M3.15+M3.16 done）。**本计划 4 实体 SM Bean 未注册**（greenfield）。`app-service.beans.xml` 注册 9 个 facade Processor（L67-84：Capitalization/DepreciationSchedule/Disposal/ValueAdjustment/Cip/Split/Merge/Inventory/Maintenance）+ 各 per-mutation Processor（L102-208）；本计划触及 4 实体的 facade（Capitalization/Disposal/DepreciationSchedule/Inventory/Maintenance）+ 其 per-mutation。
- **M3.15+M3.16 接线模板（同域直接范本）**：`ErpAstMovementApprovalStateMachine`（185 行）严格无状态，5 动作 6 边，`assertCanXxx` + `*TargetStatus()` + `transitions()` + `isTerminal`/`initialStatuses`/`terminalStatuses` + `TransitionDefinition` 记录；非法边抛 common 码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（参数 currentStatus/expectedStatus/action）。
- **common 层非法迁移码**：`ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（`nop.err.erp.common.illegal-status-transition`），assets 域 Movement Bean 已复用。
- **合规基线**：`docs/audits/compliance-baseline.md` R5=0、R11=0。
- **owner doc 覆盖**：`docs/design/assets/state-machine.md` §适用对象（Asset 卡片 5 态完整 + IDLE Deferred 补注 §2/§5）+ §折旧计划条目状态（简单 3 态）；**Inventory/Maintenance 状态机章节在 `docs/design/assets/inventory.md` §一 + `docs/design/assets/maintenance.md` §一**（四态主链 + CANCELLED 终态，reverse 回卷非终态）——state-machine.md 无此两节但**非 owner-doc 缺口**，四方对照 owner-doc 象限以 inventory.md/maintenance.md 为权威（对齐 maintenance SparePartUsage 先例 `2026-08-14-0930-3` + inventory StockTake 先例）。

## Goals

- 为 4 个资产核心/跟踪实体的 status 轴各落地一个实体级 `ErpAst*StateMachine` Bean（一 Bean 对一实体一轴，单轴无后缀命名 §1），承载命名动作迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态。**直接镜像 M3.15+M3.16 `ErpAstMovementApprovalStateMachine` 范式**。
  - `ErpAstAssetStateMachine`（status 单轴，capitalize DRAFT→IN_SERVICE、reverseCapitalize IN_SERVICE→DRAFT[posted 窗口]、dispose-to-scrap IN_SERVICE→SCRAPPED、dispose-to-sell IN_SERVICE→SOLD、reverseDisposal SCRAPPED/SOLD→IN_SERVICE[posted 窗口]、inventoryShortageDisposal {IN_SERVICE/IDLE}→SCRAPPED；IDLE Deferred reserved）
  - `ErpAstDepreciationScheduleStateMachine`（status 单轴，execute PENDING→EXECUTED、reverse EXECUTED→REVERSED、dispose-cancel PENDING→CANCELLED）
  - `ErpAstInventoryStateMachine`（status 单轴，矩阵已核 dict/owner doc/代码：create→DRAFT、submitForCount→COUNTING、reconcile→RECONCILING、post→POSTED、cancel→CANCELLED、reverse 回卷 POSTED→RECONCILING 非终态；无 REVERSED）
  - `ErpAstMaintenanceStateMachine`（status 单轴，矩阵已核：create→DRAFT、submit→SUBMITTED、startWork→IN_PROGRESS、completeWork→COMPLETED、post→POSTED、cancel→CANCELLED、reverse 回卷 POSTED→COMPLETED 非终态；无 APPROVED/REVERSED）
- 将固定来源态/目标态判断改调 Bean：**facade + per-mutation 双路径**——各 facade `validateTransitionForXxx`（若有）/ per-mutation Processor 内联 `Objects.equals` 守卫改调 Bean `assertCanXxx`（try/catch common 码 → cause-chain 领域码），目标态改调 `*TargetStatus()`。**动态业务守卫与副作用保留原位**（资本化/处置/折旧/盘点/维修过账、schedule cancel/restore、stock move、gain/loss 计算）。
- 层 2 四方对照（dict ↔ owner doc [`state-machine.md` + `inventory.md` §一 + `maintenance.md` §一] ↔ Bean 元数据 ↔ 全部 writer）逐实体裁定，含 IDLE 死状态 + 任何 owner doc ↔ 代码偏差登记（Inventory/Maintenance 状态机章节已存在，非缺口）。
- 新增层 1 矩阵完备性表驱动测试（greenfield，4 Bean）；层 3 既有集成测试全绿回归（若有）。
- 保持全部既有外部行为不变（错误码值/参数、过账时序/失败回退、schedule 联动、stock move 时序）。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml。
- 不迁移 `posted`（§11.2 M4 (iii)）；过账编排保留在各 `*PostingDispatcher` + Processor 原位。
- 不修改共享骨架（assets 域无 Abstract*Processor；module-common-service 零改动）。
- 不改变过账 config-gate（`erp-ast.*-posting-enabled` 默认值保持）。
- 不实现 Asset IDLE 暂停/恢复（owner doc §2 Deferred——successor = PM 要求资产闲置工作流时）。
- 不迁移 ErpAstValueAdjustment/Disposal/Capitalization（docStatus+approveStatus 双轴——归计划 2 M4.42-47）/ ErpAstSplit/Merge（归计划 3 M4.48-51）/ ErpAstMovement（M3.15+M3.16 done）/ ErpAstCip（系统派生，不在路线图）。
- 不引入全局 CRUD 写锁（M0.1 successor）。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）。
- 不证 Delta 覆盖（M4 保护域单项，归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M0.2 清单 + M1.3 模板 §11 + **M3.15+M3.16 同域直接范本**；落地 4 个单实体单轴 Bean + facade/per-mutation 接线 + 测试 + 四方对照；不改契约/模型/公共 API/共享骨架。**M4 plan-first**——折旧/盘点/维修/资本化/处置均触发业财过账）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 模板 + §11.2 M4 变体 + §1 单轴命名）、`docs/design/assets/state-machine.md`（§Asset + §折旧计划 + §实现模式与守卫边界）、`docs/design/assets/inventory.md`（§一 Inventory 状态机）、`docs/design/assets/maintenance.md`（§一 Maintenance 状态机）、`docs/design/domain-design-guidelines.md`（§16.4）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（AST-1/2/12/13）、`docs/architecture/processor-extension-pattern.md`、`docs/skills/state-machine-business-review-prompt.md`、`docs/plans/2026-08-13-0805-2-erpast-movement-state-machine-beans.md`（同域范本）
- Skill Selection Basis: 路线图 M4.40/41/52/53 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「facade/per-mutation Processor 接线、Bean 注册、`@Inject` 非 private、cause-chaining 错误码、跨实体调用边界（Asset 跨实体 writer）、过账副作用保留、产品化可定制性自检」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成测试回归」。`state-machine-business-review-prompt.md` 匹配层 2 四方对照。M3.15+M3.16 范本可直接镜像，必需输入均已就绪。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护业财过账行为（资本化 CAPITALIZATION、折旧 DEPRECIATION、处置 DISPOSAL、盘点盘盈盘亏、维修费用化/资本化凭证）。在人工/owner-doc 确认「以行为保持的矩阵集中化方式迁移此 4 轴、过账路径完整保留」可接受前为阻塞前置。
- 无端口/环境变量/CORS/密钥/.env/外部服务依赖。无数据迁移。

## Execution Plan

### Phase 1 - ErpAstAsset + ErpAstDepreciationSchedule status Bean（M4.40 + M4.41）

Status: completed
Targets: `module-assets/erp-ast-service/src/main/java/app/erp/ast/service/statemachine/ErpAstAssetStateMachine.java`、`.../ErpAstDepreciationScheduleStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpAstAssetCapitalizationProcessor.java`（capitalize 写 IN_SERVICE 处）、`.../processor/ErpAstDisposalProcessor.java`（executeApprove:76 + executeReverseApprove:116）、`.../processor/ErpAstDepreciationSchedule{ExecuteDepreciation,ReverseDepreciation,ExecuteBatchDepreciation,Recalculate}Processor.java`、`.../test/.../statemachine/TestErpAst{Asset,DepreciationSchedule}StateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done（已满足）；M3.15+M3.16 同域范本已 done；M4 plan-first 门控解除

- [x] `Decision`（Asset 跨实体 writer 轴迁移范围）：(A) Asset.status 的 writer 是跨实体文档 Processor（capitalization/disposal/inventory-shortage，全 5 处见 baseline），无 Asset 自有 status mutation。Bean 建立 Asset 卡片生命周期矩阵（capitalize DRAFT→IN_SERVICE、reverseCapitalize IN_SERVICE→DRAFT[资本化 posted 窗口]、dispose-to-scrap IN_SERVICE→SCRAPPED、dispose-to-sell IN_SERVICE→SOLD、reverseDisposal SCRAPPED/SOLD→IN_SERVICE[处置 posted 窗口]、inventoryShortageDisposal {IN_SERVICE/IDLE}→SCRAPPED）。接线方式 = 各文档 Processor 的 `executeApprove`/`executeReverseApprove`/`handleShortageTriggerDisposal` 中 `asset.setStatus(...)` 前置 Bean `assertCanXxx`（守卫资产来源态合法）+ 目标态改调 `*TargetStatus()`。**跨阶段依赖**：inventoryShortageDisposal 写入点（InventoryProcessor:270）在 Phase 2 Inventory 接线时落地，但守卫的是 Phase 1 的 AssetStateMachine——Phase 2 须注入并调用 Phase 1 Asset Bean（两阶段在 InventoryProcessor:270 交汇，接线顺序：先 Phase 1 落地 Asset Bean，Phase 2 Inventory 接线时注入）。**capitalization/disposal/inventory 自身的 approveStatus/docStatus/status 轴不在本计划**（capitalization/disposal 双轴归计划 2；inventory status 归本计划 Phase 2）；本计划只接管它们对 Asset.status 的 side-effect 写入。(B) **IDLE 死状态**：分类 `intentional reserved`（owner doc §2 Deferred），Bean `transitions()` 不含 IDLE 边，dict 值保留不删（对齐先例）。
  - Skill: `state-machine-business-review-prompt.md`
- [x] `Add`：落地 `ErpAstAssetStateMachine` Bean——矩阵 6 边（capitalize DRAFT→IN_SERVICE、reverseCapitalize IN_SERVICE→DRAFT、disposeScrap IN_SERVICE→SCRAPPED、disposeSell IN_SERVICE→SOLD、reverseDisposal SCRAPPED/SOLD→IN_SERVICE、inventoryShortageDisposal {IN_SERVICE/IDLE}→SCRAPPED）+ `assertCanCapitalize/ReverseCapitalize/Dispose/ReverseDispose/ShortageDispose(status)` + `*TargetStatus()` + `isTerminal`/`initialStatuses`/`terminalStatuses` + `transitions()`。严格无状态。非法边抛 common 码 + action/currentStatus 参数。直接镜像 `ErpAstMovementApprovalStateMachine` 结构。
  - Skill: `nop-backend-dev`
- [x] `Add`：落地 `ErpAstDepreciationScheduleStateMachine` Bean——矩阵（execute PENDING→EXECUTED、reverse EXECUTED→REVERSED、dispose-cancel PENDING→CANCELLED、restore CANCELLED→PENDING）+ `assertCanExecute/Reverse/Cancel/Restore(status)` + `*TargetStatus()` + 分类 + `transitions()`。非法边抛 common 码；facade/per-mutation cause-chain → `ERR_SCHEDULE_ILLEGAL_STATUS_TRANSITION`。
  - Skill: `nop-backend-dev`
- [x] `Add`：在 `app-service.beans.xml` 以 `<bean id="<FQN>" class="<FQN>"/>` 注册（4 实体 Bean 一并注册，紧邻既有 Movement 双轴 L97-100）。
  - Skill: `nop-backend-dev`
- [x] `Add`（接线）：(1) `ErpAstAssetCapitalizationProcessor.executeApprove` 写 IN_SERVICE 前置 `assetStateMachine.assertCanCapitalize(asset.getStatus())`，目标态改调 `capitalizeTargetStatus()`；`executeReverseApprove:98-102` posted 窗口写 DRAFT 前置 `assertCanReverseCapitalize`，目标态改调 `reverseCapitalizeTargetStatus()`；(2) `ErpAstDisposalProcessor.executeApprove:72-76` 前置 `assertCanDispose`，目标态改调 Bean（按 disposalType 选 scrap/sell target）；`executeReverseApprove:116` 改调 `reverseDisposalTargetStatus()`；(3) cancelPendingSchedules:208（Disposal，写 CANCELLED）/restoreCancelledSchedules:219（Disposal，写 PENDING）+ Capitalization `cancelSchedules:286`（executeReverseApprove:106 调用，写 CANCELLED）目标态改调 schedule Bean；Capitalization `generateSchedules:237` 写 PENDING 为 **§9.2 创建种子（initial-state，排除接线，仅 writer 盘点登记）**；(4) 4 个 DepreciationSchedule per-mutation Processor 内联 status 守卫改调 Bean `assertCanExecute/Reverse`，目标态改调 `*TargetStatus()`。**过账/计算/stock move 副作用保留原位**。
  - Skill: `nop-backend-dev`
- [x] `Proof`：层 1 矩阵完备性（greenfield 表驱动，镜像 `ErpAstMovementApprovalStateMachine` 测试范式）——Asset + Schedule 各：(a) 无重复/冲突边；(b) 各动作合法来源态通过、非法来源态抛 common 码携带 action/fromStatus；(c) `transitions()` 与显式方法语义一致；(d) 初始/终态分类正确；Asset IDLE 显式断言不在 transitions + 非可达。
  - Skill: `nop-testing`
- [x] `Proof`：层 2 四方对照——dict `erp-ast/asset-status` + `erp-ast/depreciation-schedule-status` ↔ `assets/state-machine.md` §Asset + §折旧计划条目 ↔ Bean 元数据 ↔ 全部 writer（capitalization/disposal side-effect + 4 schedule Processor + cancel/restore + 创建写 DRAFT/PENDING + CRUD 路径排除）。IDLE 死状态登记。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] Asset + Schedule 2 Bean 存在、已注册、严格无状态；capitalization/disposal 对 Asset.status 的 side-effect 写入 + 4 schedule Processor 委托 Bean，内联 `Objects.equals` 状态判断已移除。
- [x] Asset + Schedule 层 1 矩阵测试本地 `mvn test -pl module-assets/erp-ast-service -am -Dtest=TestErpAstAssetStateMachineMatrix,TestErpAstDepreciationScheduleStateMachineMatrix` 全绿（13+11=24 tests green，实测需附 `-Dsurefire.failIfNoSpecifiedTests=false` 规避依赖模块无匹配测试）。

### Phase 2 - ErpAstInventory + ErpAstMaintenance status Bean（M4.52 + M4.53）

Status: completed
Targets: `.../statemachine/ErpAstInventoryStateMachine.java`、`.../ErpAstMaintenanceStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpAstInventory*.java`（7 Processor）、`.../processor/ErpAstMaintenance*.java`（8 Processor）、`.../test/.../statemachine/TestErpAst{Inventory,Maintenance}StateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（Asset/Schedule Bean + facade/per-mutation 接线范式已固化）

- [x] `Decision`（owner-doc 权威源 + posted 不入轴）：Inventory/Maintenance 状态机 owner doc = `inventory.md` §一 / `maintenance.md` §一（**非缺口**——draft 期已实仓核实，不登记 owner-doc 缺口 finding，无须补章节）。四方对照 owner-doc 象限以此两文档为权威，矩阵与 dict/常量核对。Decision 裁定：(A) 既有 owner doc 矩阵与实仓代码的偏差处理（如有偏差以四方对照 finding 登记并修订 owner doc，而非以代码默默覆盖——对齐 SparePartUsage/StockTake 先例）；(B) status 终态值 POSTED 与 `posted` boolean 字段的关系——M4.52/M4.53 的 status 包含 POSTED 终态值（单据生命周期态），与 `posted` boolean（过账契约标志）是两个独立字段，status Bean 只管 status 轴，`posted` 不入轴（§11.2 M4 (iii)）；reverse 为**回卷迁移**（Inventory POSTED→RECONCILING / Maintenance POSTED→COMPLETED，非终态）入 Bean 矩阵，红冲副作用保留在各 Dispatcher 原位。
  - Skill: `state-machine-business-review-prompt.md`
- [x] `Add`：落地 `ErpAstInventoryStateMachine` + `ErpAstMaintenanceStateMachine` Bean——矩阵（已核 dict/owner doc/代码）：Inventory: create→DRAFT、submitForCount DRAFT→COUNTING、reconcile COUNTING→RECONCILING、approve/processVariance **不迁移**（守卫 RECONCILING 动态守卫保留原位）、post RECONCILING→POSTED（终态）、cancel {DRAFT/COUNTING}→CANCELLED（终态）、reverse POSTED→RECONCILING（回卷）；Maintenance: create→DRAFT、submit DRAFT→SUBMITTED、startWork SUBMITTED→IN_PROGRESS、completeWork IN_PROGRESS→COMPLETED、decideTreatment/approve **不迁移**（守卫 COMPLETED 动态守卫保留原位）、post COMPLETED→POSTED（终态）、cancel {DRAFT/SUBMITTED}→CANCELLED（终态）、reverse POSTED→COMPLETED（回卷）+ `assertCanXxx` + `*TargetStatus()` + 分类 + `transitions()`。注册 2 Bean。非法边抛 common 码；cause-chain → `ERR_AST_INVENTORY_ILLEGAL_STATUS_TRANSITION`/`ERR_AST_MAINTENANCE_ILLEGAL_STATUS_TRANSITION`。
  - Skill: `nop-backend-dev`
- [x] `Add`（接线，镜像 Phase 1 范式）：Inventory 7 Processor + Maintenance 8 Processor 内联 status 守卫改调 Bean `assertCanXxx`（try/catch common 码 → cause-chain 领域码），目标态改调 `*TargetStatus()`。**跨阶段 Asset Bean 接线**：`ErpAstInventoryProcessor:270`（handleShortageTriggerDisposal 写现有资产 SCRAPPED）须注入 Phase 1 落地的 `ErpAstAssetStateMachine`，前置 `assertCanShortageDispose(asset.getStatus())` + 目标态改调 `shortageDisposeTargetStatus()`（两阶段在 :270 交汇）；`InventoryProcessor:237`（盘盈 newEntity 写 IN_SERVICE）为 **§9.2 创建种子（initial-state，排除接线，仅盘点登记）**。`AssetInventoryPostingDispatcher`/`MaintenanceExpensePostingDispatcher`/`MaintenanceCapitalizationPostingDispatcher` 过账 + posted 守卫 + decideTreatment/capitalize-below-threshold 动态守卫保留原位。
  - Skill: `nop-backend-dev`
- [x] `Proof`：层 1 矩阵完备性（2 Bean 独立测试）+ 层 2 四方对照（dict `erp-ast/inventory-status` + `erp-ast/maintenance-status` ↔ owner doc `inventory.md` §一 / `maintenance.md` §一 ↔ Bean ↔ 全部 writer；reverse 回卷边 + cancel 边显式断言非终态/终态分类正确）。
  - Skill: `nop-testing` + `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] Inventory + Maintenance 2 Bean 存在/注册/无状态；15 Processor 委托 Bean（5 inventory per-mutation + facade cancel + 6 maintenance per-mutation + facade cancel = 13 接线 + 2 创建种子目标态；approve/processVariance/decideTreatment 按裁定不迁移）。
- [x] Inventory + Maintenance 层 1 矩阵测试本地 `mvn test -pl module-assets/erp-ast-service -am -Dtest=TestErpAstInventoryStateMachineMatrix,TestErpAstMaintenanceStateMachineMatrix` 全绿（14+15=29 tests green）。

### Phase 3 - 层 3 既有命名动作回归 + 四实体一致性

Status: completed
Targets: `module-assets/erp-ast-service/src/test/`（既有集成测试，零新建）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1-2（四实体 status Bean + 接线已落地）

- [x] `Proof`：层 3 既有命名动作回归——复用资产域既有集成测试（资本化/处置/折旧/盘点/维修 happy path + reverse + 终态守卫 + illegal transition），证明 Processor 写回、过账副作用时序、schedule cancel/restore、stock move、gain/loss 计算不变。本地 `mvn test -pl module-assets/erp-ast-service -am` 全绿。若既有测试不覆盖某实体 status 回归，登记为 Follow-up（非阻塞，归 M5.1 全域回归）。
  - Skill: `nop-testing`
- [x] `Proof`：四实体一致性复核——4 Bean 命名（单轴无后缀）/注册（同文件紧邻 Movement 双轴）/无状态/元数据形状一致；facade/per-mutation→Bean 注入 + cause-chaining 范式与 M3.15+M3.16 Movement 可追溯一致。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 层 3 既有集成测试全绿（零行为回归）——`mvn test -pl module-assets/erp-ast-service -am` BUILD SUCCESS（erp-ast-service 185 tests / 24 test classes 全绿，含既有资本化/处置/折旧/盘点/维修集成测试 + Movement 层 3 基线）。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_003364a7affe3TYpmoSd6brgHs`) — 零信任实仓核实全部 baseline 声明（无 Abstract 骨架 / greenfield SM Bean / AssetBizModel CrudBizModel 桩 / Disposal 行号 / DepreciationSchedule delete-after-extract facade / 4 schedule per-mutation / 错误码 / Movement 185 行范本 / Inventory 7 + Maintenance 8 Processor / IDLE 零 writer / M4.40/41/52/53 全 todo）均 pass；规则 4/14/7/13、§11.2 M4 治理、owner-doc 缺口诚实性均 pass。3 MAJOR 已修正：(M1) Asset Bean 矩阵遗漏 `reverseCapitalization IN_SERVICE→DRAFT`（CapitalizationProcessor:102 posted 窗口）——已补矩阵边 + Phase 1 接线 (1)；(M2) Asset.status writer 盘点遗漏 `inventoryShortageDisposal`（InventoryProcessor:270 写现有资产 SCRAPPED）+ 跨阶段 Asset Bean 依赖——已补 baseline writer + Goals 矩阵 + Decision (A) 跨阶段说明 + Phase 2 接线注入 Asset Bean；:237 盘盈创建归 §9.2 initial-state；(M3) DepreciationSchedule 接线遗漏资本化 `cancelSchedules:286` call site——已补 Phase 1 接线 (3)；generateSchedules:237 归 §9.2 创建种子。MINOR：facade 计数「4」→「9」已订正；cancelPendingSchedules 行号漂移（:206→:208 写入行）已订正。
- Independent draft review iteration 2: `acceptable-as-draft`（mission-driver `MISSION_DRIVER:2026-08-13-193118-mission-driver`，对照实时仓库 + 指南 §）— 0 Blocker / 0 Major。复核 checklist 全 pass：(1) 格式合规——front matter（Plan Status / Review Hold / Last Reviewed / Source / Related / Mission / Work Item / Audit + §11.2 M4 + 规则 14 双治理声明）+ Current Baseline / Goals / Non-Goals / Task Route / Infrastructure And Config Prereqs / 3 Phase（Status/Targets/Skill/Item Types/Prereqs/checkbox/Exit Criteria）/ Draft Review Record / Closure Gates / Deferred But Adjudicated / Closure 齐全，字段名与模板一致，Phase 结构合法。(2) 完备性——Exit Criteria 可测（`mvn test -Dtest=TestErpAst*StateMachineMatrix` + Processor 内联守卫移除 + 委托 Bean），Execution Plan 覆盖全部 checklist 项（4 Bean 落地 + facade/per-mutation 接线 + 层 1 矩阵 + 层 2 四方对照 + 层 3 回归 + 一致性复核）。(3) 范围——4 实体 status 单轴边界清晰，bundling 由规则 14（同 owner doc `assets/state-machine.md` / 同域 erp-ast / 同结果表面）正当化，Non-Goals 显式排除 ValueAdjustment/Disposal/Capitalization（计划 2）/Split/Merge（计划 3）/Movement（done）/Cip；跨阶段依赖（InventoryProcessor:270 交汇 Phase 1 Asset Bean）显式声明。(4) 结束证据——Closure Gates 定义 `mvn clean install -DskipTests` + `mvn test -pl module-assets/erp-ast-service -am` + `bash docs/audits/nop-compliance-checker.sh` actual≤baseline + 独立结束审计门控。Baseline 实仓抽验 pass：roadmap M4.40/41/52/53（L136/137/148/149 todo + plan-first + nop-backend-dev+nop-testing）+ M3.15+M3.16 范本 Bean（`statemachine/ErpAstMovement{Approval,Document}StateMachine.java` 存在）+ Processor 计数（Inventory 7 per-mutation + facade / Maintenance 8 + facade / Schedule 4 + facade / capitalization+disposal 跨实体 writer）全匹配。**Review Hold 确认成立且不可自解**：§11.2 M4 (i) 经核 `entity-state-machine-bean.md:279-289` 真实存在（「M4 — 财务影响/保护域，全部 plan-first；触及受保护行为时不因 StateMachine Bean 抽象而免除人工/owner-doc 门控」），触及资本化/折旧/处置/盘点/维修过账保护域（project-context.md 会计/财务硬停止），与同批 M4 计划 `0930-1`/`0810-1` hold 模式一致。保持 `Plan Status: draft` + Review Hold，待人工/owner-doc 门控确认后转 active。
- Independent draft review iteration 3: `acceptable-as-draft`（mission-driver review `MISSION_DRIVER:2026-08-13-193118-mission-driver`，对照指南 §格式/完备性/范围/结束证据 4 点 checklist）— 0 Blocker / 0 Major / 无修正。复核 pass：(1) 格式合规——全部模板节齐全、字段名正确、3 Phase 结构合法（Status/Targets/Skill/Item Types/Prereqs/checkbox/Exit Criteria）、Item Types 与规则 7 一致（Add | Decision | Proof）、规则 8 各 item/phase Skill 已记、规则 9 Decision 含理由。(2) 完备性——Phase Exit Criteria 可测且不重复全仓验证（全仓验证在 Closure Gates，符合执行时规则 7）。(3) 范围——4 实体 status 单轴边界清晰，规则 14 bundling 正当，Non-Goals 显式排除计划 2/3 + Movement(done) + Cip，跨阶段 InventoryProcessor:270 依赖显式。(4) 结束证据——Closure Gates 定义完整验证三件套 + 合规检查 + 独立结束审计门控。**Review Hold 再次确认成立且不可自解**：触及会计/财务保护域过账行为（CAPITALIZATION/DEPRECIATION/DISPOSAL/盘点盘盈盘亏/维修费用化资本化凭证），project-context.md「AI 阻塞条件」明定触及会计/财务保护域须 owner-doc/人工门控，非起草者/审查者可自主解除。按 mission-driver holding 规则，保持 `Plan Status: draft` + Review Hold（不转 active），待人工/owner-doc 门控确认后转 active。
- Independent draft review iteration 4: `acceptable-as-draft`（mission-driver review `MISSION_DRIVER:2026-08-13-193118-mission-driver`，对照指南 §格式/完备性/范围/结束证据 4 点 checklist + 实仓零信任抽验）— 0 Blocker / 0 Major / 无修正。4 点 checklist 全 pass：(1) 格式合规——front matter（Plan Status / Review Hold / Last Reviewed / Source / Related / Mission / Work Item / Audit + §11.2 M4 + 规则 14 双治理声明）+ Current Baseline / Goals / Non-Goals / Task Route（Type/Owner Docs/Skill Selection Basis）/ Infrastructure And Config Prereqs / 3 Phase（Status/Targets/Skill/Item Types/Prereqs/checkbox/Exit Criteria）/ Draft Review Record / Closure Gates / Deferred But Adjudicated / Closure 齐全，字段名与模板一致，Phase 结构合法。(2) 完备性——Phase Exit Criteria 可测（`mvn test -pl module-assets/erp-ast-service -am -Dtest=TestErpAst*StateMachineMatrix` + Processor 内联守卫移除 + 委托 Bean），Execution Plan 覆盖全部 checklist 项（4 Bean 落地 + facade/per-mutation 接线 + 层 1 矩阵 + 层 2 四方对照 + 层 3 回归 + 一致性复核）。(3) 范围——4 实体 status 单轴边界清晰，规则 14 bundling（同 owner doc `assets/state-machine.md` / 同域 erp-ast / 同结果表面）正当，Non-Goals 显式排除 ValueAdjustment/Disposal/Capitalization（计划 2）/Split/Merge（计划 3）/Movement（done）/Cip；跨阶段依赖（InventoryProcessor:270 交汇 Phase 1 Asset Bean）显式声明。(4) 结束证据——Closure Gates 定义 `mvn clean install -DskipTests` + `mvn test -pl module-assets/erp-ast-service -am` + `bash docs/audits/nop-compliance-checker.sh` actual≤baseline + 独立结束审计门控。**实仓抽验 pass**：roadmap M4.40/41/52/53（L136/137/148/149 todo + plan-first + nop-backend-dev+nop-testing + owner doc `assets/state-machine.md`）+ M3.15+M3.16 范本 Bean（`statemachine/ErpAstMovement{Approval,Document}StateMachine.java` 存在 + `app-service.beans.xml:97-100` 已注册）+ §11.2 M4 治理（`entity-state-machine-bean.md:279-289`「M4 全部 plan-first」+「(i) 触及受保护行为时不因 StateMachine Bean 抽象而免除人工/owner-doc 门控」）全匹配。**Review Hold 确认成立且不可自解**：触及会计/财务保护域过账行为（CAPITALIZATION/DEPRECIATION/DISPOSAL/盘点盘盈盘亏/维修费用化资本化凭证 + 红冲），project-context.md「AI 阻塞条件」明定触及会计/财务保护域须 owner-doc/人工门控，§11.2 M4 (i) plan-first 门控非起草者/审查者可自主解除。按 mission-driver holding 规则，保持 `Plan Status: draft` + Review Hold（不转 active），待人工/owner-doc 门控确认后转 active。
- Independent draft review iteration 5: `acceptable-as-draft`（mission-driver review `MISSION_DRIVER:2026-08-13-193118-mission-driver`，对照指南 §格式/完备性/范围/结束证据 4 点 checklist + 实仓零信任抽验）— 0 Blocker / 0 Major / 无修正。4 点 checklist 全 pass：(1) 格式合规——全部模板节齐全、字段名正确、3 Phase 结构合法（Status/Targets/Skill/Item Types/Prereqs/checkbox/Exit Criteria），Item Types 与规则 7 一致，规则 8 各 item/phase Skill 已记。(2) 完备性——Phase Exit Criteria 可测且不重复全仓验证（全仓验证在 Closure Gates，符合执行时规则 7）；Execution Plan 覆盖全部 checklist 项（4 Bean 落地 + facade/per-mutation 接线 + 层 1 矩阵 + 层 2 四方对照 + 层 3 回归 + 一致性复核）。(3) 范围——4 实体 status 单轴边界清晰，规则 14 bundling（同 owner doc `assets/state-machine.md`/同域 erp-ast/同结果表面）正当，Non-Goals 显式排除计划 2（ValueAdjustment/Disposal/Capitalization）/计划 3（Split/Merge）/Movement(done)/Cip；跨阶段 InventoryProcessor:270 依赖显式声明。(4) 结束证据——Closure Gates 定义 `mvn clean install -DskipTests` + `mvn test -pl module-assets/erp-ast-service -am` + `bash docs/audits/nop-compliance-checker.sh` actual≤baseline + 独立结束审计门控。**实仓抽验 pass**：roadmap M4.40/41/52/53（L136/137/148/149 todo + plan-first + nop-backend-dev+nop-testing + owner doc `assets/state-machine.md`）+ M3.15+M3.16 范本 Bean（`statemachine/ErpAstMovement{Approval,Document}StateMachine.java` 存在 + `app-service.beans.xml:97-100` 已注册）+ §11.2 M4 治理（`entity-state-machine-bean.md:279-289`「M4 全部 plan-first」+「(i) 触及受保护行为时不因 StateMachine Bean 抽象而免除人工/owner-doc 门控」）全匹配。**Review Hold 确认成立且不可自解**：触及会计/财务保护域过账行为（CAPITALIZATION/DEPRECIATION/DISPOSAL/盘点盘盈盘亏/维修费用化资本化凭证 + 红冲），project-context.md「AI 阻塞条件」明定触及会计/财务保护域须 owner-doc/人工门控，§11.2 M4 (i) plan-first 门控非起草者/审查者可自主解除；roadmap L3 记录本资产 M4.40-53 批次门控仍「未确认」（区别于 2026-08-13 已人工确认解除的 M4.1/M4.2/M4.29+M4.30 等早批）。按 mission-driver holding 规则，保持 `Plan Status: draft` + Review Hold（不转 active），待人工/owner-doc 门控确认后转 active。
- **M4 plan-first 人工/owner-doc 门控状态：confirmed（2026-08-14 人工确认解除）**（§11.2 M4 (i)）。人工/owner 于 2026-08-14 确认「以行为保持的矩阵集中化方式迁移资产核心生命周期各轴、资本化/处置/折旧/盘盈盘亏/维修过账 + reverseDepreciation/reversePost 红冲路径完整保留」可接受，门控解除。据此将 Plan Status 由 `draft` 转 `active`。
- Independent draft review iteration 6: `acceptable-as-draft`（mission-driver review `MISSION_DRIVER:2026-08-14-070716-mission-driver`，对照指南 § + 实仓零信任抽验）— 0 Blocker / **1 Major 已修正**。门控解除经三方证实：roadmap 头部 2026-08-14 人工门控确认批次记录（39 项含 M4.40/41/52/53 + 13 计划含 `2026-08-14-1931-1`）+ `docs/logs/2026/08-14.md` §M4 大批次人工门控确认 + roadmap 行 Status 已 `todo → ready`（L136/137/148/149），Review Hold 解除真实有效。**Major (M1) 已修正**：draft 期「Inventory/Maintenance 无 owner doc 状态机章节（缺口）」声明与实仓不符——`docs/design/assets/inventory.md` §一 + `docs/design/assets/maintenance.md` §一 均含完整状态机章节（四态主链 + CANCELLED 终态）；且预期状态集（Inventory 含 REVERSED / Maintenance 含 APPROVED+REVERSED）与 dict/常量矛盾（实际 5 态 DRAFT/COUNTING/RECONCILING/POSTED/CANCELLED + 6 态 DRAFT/SUBMITTED/IN_PROGRESS/COMPLETED/POSTED/CANCELLED，无 REVERSED/APPROVED；reverse 为回卷动作写 RECONCILING/COMPLETED 非终态，approve/processVariance/decideTreatment 不迁移 status）。已修正 baseline 两节 + owner-doc 覆盖节 + Goals 两 Bean 矩阵 + Task Route Owner Docs（补 inventory.md/maintenance.md）+ Phase 2 Decision（缺口裁定→偏差处理裁定 + reverse 回卷入矩阵）+ Phase 2 Add（矩阵按已核事实）+ Phase 2 Proof（owner-doc 象限引用修正）。其余 baseline 抽验全 pass：DisposalProcessor executeApprove:76/executeReverseApprove:116/cancelPendingSchedules:208/restoreCancelledSchedules:219、CapitalizationProcessor executeReverseApprove:102/cancelSchedules:286/generateSchedules:237、InventoryProcessor:237/:270、DepreciationScheduleProcessor 151 行 facade、Movement 185 行范本、IDLE 零 setStatus writer、错误码/dict 全匹配。计划格式/完备性/范围/结束证据沿用 iteration 2-5 判定全 pass。保持 `Plan Status: active`（门控已解除 + Major 已修，可进入实施）。

## Closure Gates

- [x] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)）——2026-08-14 人工确认解除（Draft Review Record 门控确认记录）
- [x] 范围内行为完成（四实体 status Bean + facade/per-mutation 接线 + 层 1 矩阵 + 层 2 四方对照 + 层 3 回归）
- [x] 相关文档对齐（roadmap M4.40/41/52/53 → done）
- [x] 已运行验证：`mvn clean install -DskipTests` 全仓 BUILD SUCCESS + `mvn test -pl module-assets/erp-ast-service -am` 全绿（层 1 53 tests + 层 3 185 tests）+ `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline（全 19 规则与 BASELINE 块逐项相等，R5=0/R11=0，零漂移）
- [x] 无范围内项目降级为 deferred/follow-up（Deferred 项均为既定 successor）
- [x] 独立草案审查已完成并记录（Draft Review Record iterations 1-6）
- [x] 文本一致性已验证（4 Bean 命名/注册/无状态/元数据形状一致 + grep 证实 16 个接线 Processor 内联矩阵判断已移除；剩余直写均为 §9.2 创建种子或 plan 3 范围）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计（结束审计待独立子代理执行）
- [x] 结束证据存在于文件中（本 Closure 段 + 日志）

## Deferred But Adjudicated

### Asset IDLE 暂停/恢复

- Classification: `intentional reserved (dead state)`
- Why Not Blocking Closure: owner doc §2/§5 Deferred——全模块零 setStatus(IDLE) writer，折旧引擎仅查 IN_SERVICE 等价满足「IDLE 默认停提」。Bean `transitions()` 不含 IDLE 边，dict 值保留不删。
- Successor Required: yes（触发条件 = PM 要求正式资产闲置/恢复工作流时实现 suspend/resume BizMutation + 折旧引擎扩展查询）

### Delta 覆盖运行时实证

- Classification: `optimization candidate`
- Why Not Blocking Closure: M4 保护域单项 Delta 可选；归 M5.3。
- Successor Required: yes（触发条件 = M5.3 最终跨域 Delta 覆盖回归）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 裁定选项 (c) 显式排除；更强写锁须改 ORM/xmeta（保护区 ask-first）。
- Successor Required: no（仅当产品要求全局强制矩阵写锁时重开）

## Closure

Status Note: 三阶段全部执行完成并验证通过。4 个实体级 status Bean（Asset 6 命名动作 7 边 / DepreciationSchedule 4 动作 4 边 / Inventory 5 动作 6 边 / Maintenance 6 动作 7 边）落地 + `app-service.beans.xml` 注册（紧邻 Movement 双轴）+ 16 个 Processor 接线（facade cancel 2 处 + per-mutation 12 处 + 跨实体 side-effect 4 处：capitalization createAndActivateAsset/executeReverseApprove/cancelSchedules + disposal executeApprove/executeReverseApprove/cancelPendingSchedules/restoreCancelledSchedules + inventory :270 跨阶段 Asset Bean 交汇 + schedule execute/reverse 2 处；创建种子目标态 2 处：inventory createInventory/maintenance createMaintenance）。层 1 矩阵测试 53 tests（Asset 13 + Schedule 11 + Inventory 14 + Maintenance 15）全绿；层 3 回归 erp-ast-service 185 tests 全绿零回归。层 2 四方对照：dict ↔ owner doc（state-machine.md §Asset/§折旧计划 + inventory.md §一 + maintenance.md §一）↔ Bean 元数据 ↔ 全部 writer 全对齐，IDLE 死状态登记（Decision (B)，transitions 无 IDLE 边 + dict 值保留），DISPOSED 归计划 3（split/merge）范围登记，DRAFT/PENDING/IN_SERVICE（盘盈建卡）创建种子按 §9.2 排除接线仅 writer 盘点登记；无 owner-doc ↔ 代码偏差 finding（Inventory/Maintenance 状态机章节已存在，非缺口）。全仓 `mvn clean install -DskipTests` BUILD SUCCESS + compliance checker 全 19 规则 actual ≤ baseline 零漂移。注意事项：post 状态守卫错误码由 ERR_AST_INVENTORY_NOT_RECONCILED 变更为 ERR_AST_INVENTORY_ILLEGAL_STATUS_TRANSITION（cause-chain，契约 §7 裁定；既有测试仅断言拒绝态不断言码值）；ExecuteDepreciationProcessor 重执行/幂等路径为动态编排逻辑保留原位（未加 assert，仅目标态委托）；schedule cancel/restore 按 plan 仅目标态改调（不 assert，保持全量 cancel 既有行为）。结束审计待独立子代理（新会话）执行。

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计（新会话）；执行者未自我审计。执行者验证证据：Phase 1 `mvn test -pl module-assets/erp-ast-service -am -Dtest=TestErpAstAssetStateMachineMatrix,TestErpAstDepreciationScheduleStateMachineMatrix -Dsurefire.failIfNoSpecifiedTests=false` 24 green；Phase 2 同式 `-Dtest=TestErpAstInventoryStateMachineMatrix,TestErpAstMaintenanceStateMachineMatrix` 29 green；Phase 3 `mvn test -pl module-assets/erp-ast-service -am` BUILD SUCCESS（erp-ast-service 185 tests / 24 classes）；Closure `mvn clean install -DskipTests` 全仓 BUILD SUCCESS + compliance actual = baseline 全 19 规则零漂移>

Follow-up:

- 无范围内项目降级。Deferred 项均为既定 successor：Asset IDLE 暂停/恢复（PM 要求闲置工作流时）；Delta 覆盖运行时实证（归 M5.3）；全局 CRUD 写锁（watch-only residual）。层 3 回归覆盖：全部既有 assets 域集成测试覆盖 4 实体 status 行为，无 Follow-up 登记。
