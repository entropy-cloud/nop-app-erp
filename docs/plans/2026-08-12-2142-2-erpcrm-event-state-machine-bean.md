# 2026-08-12-2142-2-erpcrm-event-state-machine-bean CRM ErpCrmEvent 实体级状态机 Bean（M2.2）

> Plan Status: active
> Last Reviewed: 2026-08-12
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M2.2（todo）
> Related: 前置 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（M0.1 done）+ `2026-08-12-0617-2-entity-state-machine-m0-2-inventory.md`（M0.2 done）+ `2026-08-12-0738-1-cs-ticket-state-machine-bean-pilot.md`（M1.1 范式）+ `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（M1.3 模板 done）；姊妹范式 `2026-08-12-1118-1-erpct-contract-state-machine-bean.md`（单实体单轴 + per-mutation Processor 接线 + 重复守卫收敛范本）
> Mission: entity-state-machine
> Work Item: M2.2
> Audit: required

## Current Baseline

- **M0.1 契约 + M1.3 模板已就绪**（`docs/architecture/entity-state-machine-bean.md` §1-§11）。M1.3 已裁定 **go**，M2 各项 Deps（M1.3）门控解除；`todo → ready` 仍需独立 plan 草案审查（路线图规则 1）。
- **M2.2 归类**：M2 简单生命周期（§11.2），非保护域、无审批子矩阵、无跨域过账副作用（Event 头状态本身不触发业财过账；Lead 字段派生 `LeadActivityDerivationHelper` 是只读聚合回写，非过账）。**非 plan-first**；但仍跨模块行为变更，须独立 plan + 独立草案审查 + 独立结束审计（路线图规则 3）。
- **活动/事件（ErpCrmEvent.status）语义**（owner doc `docs/design/crm/state-machine.md` §Event `:120-188`）：3 态 PLANNED/COMPLETED/CANCELLED；初始 = PLANNED；终态 = COMPLETED/CANCELLED（`:145`「终态不可恢复。若需重新安排，新建 Event（可关联原事件的 parentEventId）」）。设计声明边：PLANNED→COMPLETED（complete，`:140`）、PLANNED→CANCELLED（cancel，`:141`）。**无 §实现约定 段、无 stage/asynchrony/config-gated 例外**——§2 迁移表为唯一无歧义来源（§11.4「§迁移表 vs §实现约定 内部漂移」警示对 Event **不适用**）。异常路径（`:148-154`）：COMPLETED 后修改内容拒绝、重复实例独立完成、关联 Lead 已 CONVERTED 后建 Event 允许。
- **dict 实况（无死状态）**：`module-crm/erp-crm-meta/src/main/resources/_vfs/dict/erp-crm/event-status.dict.yaml:1-19` = 3 值 PLANNED/COMPLETED/CANCELLED；绑定 `ErpCrmEvent.status`（`module-crm/model/app-erp-crm.orm.xml:468 ext:dict="erp-crm/event-status"`，mandatory、**无 defaultValue**——初始 PLANNED 由调用方/创建路径负责）。3 值全部有生产 writer（无死状态）。
- **生产 writer 实况（固定迁移判断散布，已核实）**：**per-mutation Processor 路径（非 INLINE）**，BizModel 已委托：
  - `ErpCrmEventCompleteProcessor`（`module-crm/erp-crm-service/src/main/java/app/erp/crm/service/processor/ErpCrmEventCompleteProcessor.java`）：complete `:32`，守卫 `validatePlanned(event,"complete") :34` → setStatus COMPLETED `:35`；副作用 `ormTemplate.flushSession() :38` + `leadDerivationHelper.recalculateForLead(relatedLeadId) :39`（relatedLeadId==null 跳过 `:67-70`）。
  - `ErpCrmEventCancelProcessor`（同包）：cancel `:32`，守卫 `validatePlanned(event,"cancel") :34` → setStatus CANCELLED `:35`；副作用同形（`:37-38, :66-69`）。
  - 两 Processor 各自内联 `protected void validatePlanned(ErpCrmEvent, String) :54/:53`（**逐字节相同**，抛 `ERR_EVENT_ILLEGAL_STATUS_TRANSITION` + eventCode/currentStatus/expectedStatus）+ 各自 `requireEvent`（not-found `:45-52/:44-51`）。**无 `AbstractErpCrmEventProcessor` 共享骨架**、无私有 `illegalTransition` helper（后者仅存于 Lead 域 `ErpCrmLeadProcessor:217`，与 Event 无关）。
  - BizModel `ErpCrmEventBizModel`（`entity/`，Cat-B 委托后）：complete `:65` 委托 completeProcessor、cancel `:71` 委托 cancelProcessor；**保留死代码副本** `requireEvent :147-154` + `validatePlanned :156-164`（委托后无活跃调用方）。
  - **初始态 PLANNED 写入**（非迁移，创建路径，不调 `assertCan*`，§9.2 选项 c）：sequence-progress Processors（AssignSequence `:139` / SwitchSequence `:120` / AdvanceStep `:105`）+ BizModel `:281` 新建 Event 时 setStatus PLANNED。
- **动态业务守卫（保留原位，不下沉 Bean）**：(i) Lead 派生副作用（flush + recalculateForLead）；(ii) relatedLeadId==null 跳过派生；(iii) requireEvent not-found 检查（`ERR_EVENT_NOT_FOUND`）；(iv) nop-job reminder 链路（见下，独立 Job，不在 mutation Processor 内）。
- **错误码**：`ErpCrmErrors.ERR_EVENT_ILLEGAL_STATUS_TRANSITION`（`module-crm/erp-crm-service/src/main/java/app/erp/crm/service/ErpCrmErrors.java:105-107`，值 `erp.err.crm.event-illegal-status-transition`，参数 eventCode/currentStatus/expectedStatus）+ `ERR_EVENT_NOT_FOUND`（`:102-103`）。common 层码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（`module-common-service/.../ErpCommonErrors.java:23-27`，值 `nop.err.erp.common.illegal-status-transition`，参数 currentStatus/expectedStatus）已存在并被 cs/hr/ct/pur/sal Bean 复用（M1.1 Option A + `action` 补充参数范式）。
- **既有层 3 回归基线（非 greenfield，但不在 M0.1 §10 的 8 个登记基线内）**：crm 域**无** `TestErpCrmEventStateMachine`（grep 零匹配）。最近层 3 基线 = `TestErpCrmEventReminderTimeline`（`module-crm/erp-crm-service/src/test/java/app/erp/crm/service/TestErpCrmEventReminderTimeline.java`，245 行，经 `IGraphQLEngine`）：`testCompleteAndCancelAndDerivation :58-94`（PLANNED→COMPLETED `:80` + PLANNED→CANCELLED `:85` + Lead 字段派生）、`testIllegalTransitionRejected :96-112`（COMPLETED 再 complete/cancel 各拒 `ERR_EVENT_ILLEGAL_STATUS_TRANSITION` `:105-107,:109-111`）、`testEventWithoutLeadSkipsDerivation :114-123`、reminder/timeline 查询用例（与状态机正交）。另 `TestErpCrmEventReminderDisabled`、`TestErpCrmEventPerEventReminder`、`job/TestErpCrmEventReminderJob`（Job 单测，notify 链路）。**已知覆盖缺口**（层 1 矩阵补）：无 3×2 穷举矩阵、无终态全动作拒绝断言、无 `transitions()` 元数据一致性、无基线 IoC + Delta 覆盖测试。
- **跨域副作用（保留不动）**：nop-job reminder → notify 链路：`ErpCrmEventReminderJob`（`module-crm/erp-crm-service/src/main/java/app/erp/crm/service/job/ErpCrmEventReminderJob.java`）注入 `IErpCrmEventBiz` + `IErpSysNotificationBiz`，filter `status=PLANNED`（BizModel.findDueReminders `:97`），dispatch `notificationBiz.notify("crm.event-reminder", ...)`；config-gate `erp-crm.event-reminder-cron`（空=跳过）；wired at `app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-crm-event-reminder.job.yaml` + bean `app-service.beans.xml:54-55`。**只读消费 status，不经 complete/cancel Processor**——保留不动（Non-Goal）。
- **生产 Bean 注册范式已存在**：`module-crm/erp-crm-service/src/main/resources/_vfs/erp/crm/beans/app-service.beans.xml:114-117` 已以 `<bean id="<FQN>" class="<FQN>"/>` 注册 2 个 Event per-mutation Processor。StateMachine Bean 沿用此范式。
- **greenfield 范畴**：`module-crm/**/statemachine/` 不存在、无 `ErpCrm*StateMachine` Bean（grep 零匹配）。本计划为 crm 域首例 StateMachine Bean（Lead = M3.1 是独立未来轴，非本计划）。参考范式：cs 试点 + 已完成 ct/pur/sal/prj Bean。
- **合规基线**：R5（`@Inject private`）= 0（已核实 module-crm service 零违例，两 Event Processor `:23-30` + BizModel `:46-56` @Inject 字段均包级可见）、R11（Processor 重复状态判断方法）= 0。注意 R11 checker 正则仅匹配 `isAlreadyApproved/isAlreadyRejected`，**不捕获** `validatePlanned` 重复——迁移将两 Processor 的 `validatePlanned` 收敛至 Bean 后，R11 维持 0（已 0，不增）。本计划新增 1 Bean 注册 + 注入须保持 R5=0。
- **死代码候选**（登记，非本计划行为变更范围）：BizModel `requireEvent :147-154` + `validatePlanned :156-164` 为 Cat-B 委托后死代码。迁移是清理时机，但删除属独立低风险清理，归 Follow-up（带触发条件）。

## Goals

- 落地真实 `ErpCrmEventStateMachine` Bean（一 Bean 对一实体一轴 `status`），承载**已实现**迁移矩阵（PLANNED→{COMPLETED|CANCELLED}）+ 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态、可经 Delta 同名覆盖。为 crm 域首例 StateMachine Bean（建立域内范式）。
- 将 `ErpCrmEventCompleteProcessor`（complete）与 `ErpCrmEventCancelProcessor`（cancel）的**固定来源态/目标态判断**（重复的 `validatePlanned`）改调 Bean，**动态业务守卫保留原位**（Lead 派生 recalculateForLead + relatedLeadId==null 跳过、requireEvent not-found、幂等/乐观锁）。
- 保持全部既有外部行为不变（错误码 + 参数 eventCode/currentStatus/expectedStatus、complete/cancel 仅 PLANNED 合法、终态 COMPLETED/CANCELLED 全动作拒绝、Lead 派生时序、relatedLeadId==null 跳过）。
- 新增层 1 矩阵完备性表驱动测试（greenfield，不经 BizModel 入口）；层 3 既有集成测试（`TestErpCrmEventReminderTimeline` 等）回归全绿。
- 层 2 四方对照（dict ↔ `crm/state-machine.md` §Event ↔ Bean 元数据 ↔ 全部 writer 含 CRUD 路径）单轴裁定，**确认无死状态、无 §迁移表 vs §实现约定 内部漂移**（owner doc 已无歧义），禁止静默排除。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（路线图 Non-Goal）：不向 dict 新增/删除值（3 值不变）。
- 不改变任何业务状态值、动作名、错误码值、权限、Lead 派生时序、reminder Job 链路（路线图 Non-Goal「不借迁移改变既有行为」）。
- 不触碰 nop-job reminder → notify 链路（`ErpCrmEventReminderJob` + `erp-crm-event-reminder.job.yaml` + bean 注册 + BizModel.findDueReminders `:97` 的 `status=PLANNED` filter）——只读消费 status，保留不动。
- 不迁移 `ErpCrmLead.docStatus`（= M3.1，独立未来轴，独立 Bean）。
- 不删除 BizModel 死代码 `requireEvent :147-154` + `validatePlanned :156-164`（归 Follow-up，带触发条件）。
- 不引入全局 CRUD 写锁（M0.1 §9 选项 c；更强写锁 successor）。
- 不声称全域 Delta 覆盖已验证（M1.2 已证客服单轴；本计划证 Event 单轴 Delta，全域回归归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费已定稿 M0.1 契约 + M1.3 模板 + M0.2 清单，落地单轴 Bean + 接线 + 三层测试 + 四方对照；不改契约/模型/公共 API）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 迁移模板）、`docs/design/crm/state-machine.md`（§Event `:120-188`——含 §1 状态定义、§2 迁移表、§3 终态、§4 异常路径）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M0.2 清单 crm 行 M2.2）、`docs/architecture/processor-extension-pattern.md`（Bean 嵌入 Processor 编排点）
- Skill Selection Basis: 路线图 M2.2 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「per-mutation Processor 接线、重复守卫收敛、Lead 派生动态守卫保留、错误码」；`nop-testing` 匹配「矩阵表驱动测试 + 既有 IGraphQLEngine 集成测试回归」。层 2 四方对照引用 `state-machine-business-review-prompt.md` 10 维度（模板步骤 5 标配）。必需输入（owner doc + M0.1 契约 + 既有层 3 基线）已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯后端 Java + 既有 crm-service 测试容器）。
- 前置依赖：M0.1 done + M0.2 done + M1.3 done（模板 go）。均已满足。

## Execution Plan

### Phase 1 - ErpCrmEventStateMachine Bean + 注册 + 层 1 矩阵完备性测试

Status: planned
Targets: `module-crm/erp-crm-service/src/main/java/app/erp/crm/service/statemachine/ErpCrmEventStateMachine.java`（新）；`module-crm/erp-crm-service/src/main/resources/_vfs/erp/crm/beans/app-service.beans.xml`（追加 Bean 注册）；`module-crm/erp-crm-service/src/test/java/app/erp/crm/service/statemachine/TestErpCrmEventStateMachineMatrix.java`（新，层 1）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（层 1 表驱动测试）

- Item Types: `Add | Proof`
- Prereqs: M0.1 + M0.2 + M1.3 done

- [ ] `Add`：创建 `ErpCrmEventStateMachine`（无状态、不注入 DAO/IBiz/IServiceContext/事务），按契约 §4 + §11.1 步骤 1 实现。矩阵编码**已实现**迁移：
  - 显式动作方法（主路径）：`assertCanComplete(PLANNED)`、`assertCanCancel(PLANNED)`；非法来源态（COMPLETED/CANCELLED/null）抛 common 层码 + `action`/`fromStatus` 元数据。
  - 目标态方法：`completeTargetStatus()`→COMPLETED / `cancelTargetStatus()`→CANCELLED。
  - 终态分类：`isTerminal(COMPLETED|CANCELLED)`=true；PLANNED=false。
  - 只读元数据：`transitions()` 返回不可变快照（PLANNED→COMPLETED、PLANNED→CANCELLED 两条边）；`terminalStatuses()`（COMPLETED, CANCELLED）；`initialStatuses()`（PLANNED）。
  Skill: `nop-backend-dev`
- [ ] `Add`：在 `app-service.beans.xml` 以 `<bean id="app.erp.crm.service.statemachine.ErpCrmEventStateMachine" class="...ErpCrmEventStateMachine"/>` 注册（沿用既有 Processor FQN-id 范式 `:114-117`，§11.1 步骤 2）。
  Skill: `nop-backend-dev`
- [ ] `Proof`（层 1 矩阵完备性，新增 greenfield 表驱动测试 `TestErpCrmEventStateMachineMatrix`，§11.1 步骤 4）：遍历每个动作的合法/非法来源态——(a) 无重复/冲突边；(b) 从 PLANNED 可达 COMPLETED 与 CANCELLED 全部声明状态；(c) 终态 COMPLETED/CANCELLED 对 complete 与 cancel 均非法（无出边）；(d) `transitions()` 元数据与显式方法语义一致；(e) 终态/初始态集合正确。**不经 BizModel 入口**（层 1 只测 Bean）。Skill: `nop-testing`

Exit Criteria:

- [ ] `ErpCrmEventStateMachine` 落地（2 动作 + 目标态 + isTerminal + transitions 元数据），无状态（grep 证实不 import DAO/IBiz/IServiceContext/事务）。
- [ ] Bean 已在 `app-service.beans.xml` 注册（FQN id）；Bean 自身无 `@Inject`（严格无状态），Processor 接线点的 `@Inject` 字段非 private（合规 R5）。
- [ ] 层 1 矩阵测试 `mvn test -pl module-crm/erp-crm-service -Dtest=TestErpCrmEventStateMachineMatrix` 全绿，覆盖上述 (a)-(e)。
- [ ] 本地化编译检查：`mvn compile -pl module-crm/erp-crm-service -am` 通过（解除 Phase 2 接线依赖）。

### Phase 2 - Processor 接线（行为保持）+ 层 3 回归

Status: planned
Targets: `ErpCrmEventCompleteProcessor.java`（complete）、`ErpCrmEventCancelProcessor.java`（cancel）
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1

- [ ] `Fix`：两 Processor 注入 `ErpCrmEventStateMachine`（按类型注入，字段非 private），将各自内联 `validatePlanned(event, action)` 守卫替换为 `stateMachine.assertCanComplete(from)` / `stateMachine.assertCanCancel(from)`，目标态写回改 `stateMachine.<action>TargetStatus()`；删除两 Processor 的 `validatePlanned :54/:53`（矩阵部分收敛至 Bean）。Processor 捕获 Bean 的 common 层非法边报告，映射为领域 `ERR_EVENT_ILLEGAL_STATUS_TRANSITION`（保留 eventCode/currentStatus/expectedStatus 参数，common 码作 cause——对齐契约 §7 + M1.1 Option A 范式）。**动态业务守卫保留原位**：requireEvent not-found（`ERR_EVENT_NOT_FOUND`）、Lead 派生（flush + recalculateForLead）、relatedLeadId==null 跳过、乐观锁。Skill: `nop-backend-dev`
- [ ] `Proof`（层 3 既有回归保持全绿）：`mvn test -pl module-crm/erp-crm-service` 全绿——重点 `TestErpCrmEventReminderTimeline`（PLANNED→COMPLETED `:80` + PLANNED→CANCELLED `:85` + illegal transition COMPLETED 拒绝 `:105-111` + 无 Lead 跳过派生 `:114-123` + Lead 字段派生 `:88-93`）、`TestErpCrmEventReminderDisabled`、`TestErpCrmEventPerEventReminder`、`job/TestErpCrmEventReminderJob`（notify 链路不变）。证明错误码 + 参数、complete/cancel 仅 PLANNED、终态拒绝、Lead 派生时序、reminder Job filter 均不变。若既有测试因 helper 调整需微调断言，仅调整与矩阵无关部分并记录理由（不得弱化断言）。Skill: `nop-testing`

Exit Criteria:

- [ ] 两处固定来源态/目标态判断（CompleteProcessor complete + CancelProcessor cancel）均改调 Bean，grep 证实两 Processor 方法体内不再有内联 `validatePlanned` / `Objects.equals(*, EVENT_STATUS_*)` 矩阵判断（动态守卫 requireEvent/Lead 派生除外）。
- [ ] `ERR_EVENT_ILLEGAL_STATUS_TRANSITION` + 参数（eventCode/currentStatus/expectedStatus）对外不变（层 3 断言证实）；complete/cancel 仅 PLANNED 合法、终态拒绝行为不变。
- [ ] 层 3 `mvn test -pl module-crm/erp-crm-service` 全绿。

### Phase 3 - 层 2 四方对照（dict ↔ owner-doc ↔ 元数据 ↔ writer）+ Delta 适用性

Status: planned
Targets: 四方对照审计记录（写入本计划 Closure 段）；Event 单轴 Delta 证据
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度）+ `nop-testing`（Delta 双加载）

- Item Types: `Proof | Decision | Add`
- Prereqs: Phase 2

- [ ] `Proof`（四方对照，§11.1 步骤 5）：以 `state-machine-business-review-prompt.md` 10 维度审查 Event 单轴——
  - **dict ↔ 元数据**：dict 3 值 ↔ Bean `transitions()` 2 条边覆盖；每个 dict 值 writer 可达性（含 CRUD 路径，M0.1 §9.4；PLANNED 经 sequence-progress 创建路径 + CRUD 可写、COMPLETED/CANCELLED 经命名动作）。
  - **owner-doc 迁移图 ↔ 元数据**：`crm/state-machine.md §Event :130-141` 迁移图（2 条边）↔ Bean 边覆盖；显式裁定 §11.4「§迁移表 vs §实现约定 内部漂移」——Event **无** §实现约定 段，§2 迁移表为唯一无歧义来源，预期无内部漂移。
  - **元数据 ↔ 全部 writer**：盘点 `ErpCrmEvent.status` 全部写路径——生产命名动作（CompleteProcessor + CancelProcessor）+ 初始态创建（sequence-progress Processors + BizModel）+ 框架入口（`__save`/`save`，xmeta `status` insertable/updatable）+ 测试 fixture。
  - **可达性/终态/异常路径**：从 PLANNED 可达 COMPLETED/CANCELLED、终态无出边、COMPLETED 后修改拒绝（owner doc `:152`）与实现一致。
  Skill: `state-machine-business-review-prompt.md`
- [ ] `Decision`（漂移裁定，路线图规则 5）：预期无死状态、无 owner-doc 内部漂移；若层 2 实仓证据发现任何 dict 死状态、owner-doc↔实现 漂移或非法边，按 Fix/Decision 登记 + successor（禁止静默排除）。登记项示例（若适用）：BizModel 死代码 `validatePlanned :156-164`（归 Follow-up）。
  Skill: `state-machine-business-review-prompt.md`
- [ ] `Add | Proof`（Delta 适用性，§11.1 步骤 7；M2 非保护域 Delta 证为可选——**本计划显式 opt-in 将其提升为约束性交付**）：经 VFS Delta 层同名 bean id 覆盖证明替换生效——派生类覆盖一个 `assertCan<Action>`（如收紧 cancel 仅允许特定 PLANNED 子情形，或放开终态可取消），基线/Delta 双加载可区分（复用 M1.2 范式：`TestErpCrmEventStateMachineBaselineIoC` + `TestErpCrmEventStateMachineDeltaOverride`）。Skill: `nop-testing`

Exit Criteria:

- [ ] 四方对照审计记录存在且非空，每维有可追溯结论（引用 Bean 元数据 / owner doc 章节 / dict 位置 / writer 类:行）。
- [ ] 任何死状态/漂移/非法边（若发现）已按 Fix/Decision 登记 + successor，无静默排除。
- [ ] Delta 双加载运行时证据存在（非静态检查），基线/Delta 可区分。

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（`ses_009c4dc14ffet3tzynnYj3UrXl`）——无 BLOCKER、无 MAJOR。全部 load-bearing 声明经独立复核 CONFIRMED TRUE（owner doc §Event `:120-188` 3 态 + 无 §实现约定 段→§11.4 内部漂移警示不适用、ORM 字段 + dict yaml 3 值、两 Processor `validatePlanned` 重复且无共享骨架、BizModel 委托 + 死代码、错误码、common 码、`TestErpCrmEventReminderTimeline` 层 3 基线、无 `TestErpCrmEventStateMachine`、Bean 注册范式、reminder Job 只读消费 status、greenfield、R5=0/R11=0 且 R11 正则不捕获 validatePlanned、无死状态）。5 MINOR（CompleteProcessor 副作用行号一级间接、`:281` 须消歧为 `ErpCrmLeadSequenceProgressBizModel`、BizModel 死代码另有 `deriveLeadFields :169-174` 未计入、checker 门控措辞 actual==baseline 而非 exit 0、Delta 项「可选」措辞）均非阻塞；MINOR-5（Delta opt-in）已就地修正为显式约束性交付以防反松弛误读，其余为执行期精度修正。反松弛扫描 clean。草案审查收敛，Plan Status → active。

## Closure Gates

> 本计划含生产代码变更（新增 Bean + 接线 + 测试），Closure Gates 运行完整仓库验证。验证命令见 `docs/context/project-context.md`。

- [ ] 范围内行为完成（Bean + 接线 + 层 1 矩阵 + 层 3 回归 + 层 2 四方对照 + Delta 证据）
- [ ] 相关文档对齐（路线图 M2.2 done；owner doc 若有漂移补注）
- [ ] 已运行验证：`mvn clean install -DskipTests`（全仓库 BUILD SUCCESS）+ `mvn test -pl module-crm/erp-crm-service`（全绿）+ `bash docs/audits/nop-compliance-checker.sh`（exit 0，R5=0/R11=0 无漂移）
- [ ] 无范围内项目降级为 deferred/follow-up（漂移裁定必须落地登记 + successor，不得悬置）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### BizModel 死代码 `requireEvent` + `validatePlanned` 清理

- Classification: `optimization candidate`
- Why Not Blocking Closure: `ErpCrmEventBizModel:147-164` 的 `requireEvent`/`validatePlanned` 在 Cat-B 委托后无活跃调用方。删除属独立低风险清理，非状态机行为变更。
- Successor Required: yes（触发条件 = crm 下次代码整理时删除并核实无反射/测试引用）

### ErpCrmLead.docStatus 版本轴（M3.1）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: Lead 是独立实体独立轴（M3.1，docStatus 非 status），独立 Bean。本计划只迁移 Event.status。
- Successor Required: yes（触发条件 = M3.1 启动独立 plan）

## Closure

Status Note: <待执行 + 独立结束审计后填写>

Closure Audit Evidence:

- Auditor / Agent: <独立结束审计子代理（新会话）>
- Evidence: <task id / 测试结果 / 四方对照记录>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
