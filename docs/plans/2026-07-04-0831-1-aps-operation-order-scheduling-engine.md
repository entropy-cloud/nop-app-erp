# 2026-07-04-0831-1-aps-operation-order-scheduling-engine APS 工序级排产引擎 + ATP/CTP 交期承诺

> Plan Status: completed
> Mission: erp
> Work Item: 3.10 APS OperationOrder 排产引擎 + 3.11 APS ATP/CTP 交期承诺
> Last Reviewed: 2026-07-04
> Source: `docs/backlog/extended-roadmap.md` §M3 工作项 3.10/3.11；`docs/design/aps/scheduling.md`；`docs/design/aps/README.md`
> Related: `2026-07-03-1707-1-manufacturing-crp-load-engine.md`（CRP deferred「APS 排程时间作为负荷来源」承接）、`2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md`（deferred「APS 排产集成」承接）、`2026-07-03-1018-3-maintenance-visit-request-sparepart-downtime.md`（deferred「停机通知制造域排产调整」承接）
> Audit: required

## Current Baseline

- **APS 域 CRUD 已落地**（`crud-roadmap.md` Milestone 3 `done`）。`module-aps/model/app-erp-aps.orm.xml` 已定义 6 实体：`ErpApsOperationOrder`（`:55`）、`ErpApsSchedule`（`:92`）、`ErpApsConstraint`（`:119`）、`ErpApsOpRouting`（`:144` 替代工艺路线）、`ErpApsDispatchRule`（`:175`）、`ErpApsDispatchLog`（`:209`）。字典 `erp-aps/operation-order-status`（DRAFT/PLANNED/IN_PROGRESS/FINISHED/CANCELLED）、`erp-aps/scheduling-mode`（FORWARD/BACKWARD）、`erp-aps/constraint-type`（MAINTENANCE/TOOL/PERSONNEL）、`erp-aps/schedule-status`、`erp-aps/dispatch-type` 已存在。
- **BizModel 仅为生成空壳**：`module-aps/erp-aps-service/.../entity/ErpApsOperationOrderBizModel.java` 等为 `CrudBizModel<T>` 空壳，仅有 CRUD 冒烟测试 `TestErpApsOperationOrderCrudSmoke.java`，**无任何排产计算逻辑**。`ErpApsConfigs.java`/`ErpApsConstants.java`/`ErpApsErrors.java` 已存在（空壳常量/配置/错误码）。
- **OperationOrder 字段缺口**：`ErpApsOperationOrder`（`orm.xml:55-89`）含 `plannedStartDateT`/`plannedEndDateT`/`realStartDateT`/`realEndDateT`/`setupTime`/`runtimePerUnit`/`qty`/`totalDuration`/`priority`/`sequence`/`machineId`，但**缺少** `scheduling.md §1.1` 排产算法所需的 `earliestStartDateT`（前向起点约束）与 `latestEndDateT`（后向终点约束）。排产引擎无这两个约束字段无法运行前向/后向算法。
- **ErpApsTimeSlot 未持久化**：`scheduling.md §1.2` 描述的工作中心时间片在 ORM 中不存在。当前以 `ErpApsConstraint`（维护停机时段）+ 工作中心日历表达不可用时段。
- **依赖域已就绪**：manufacturing 域 `ErpMfgWorkOrder`（含 `plannedStartDate`/`plannedEndDate`）、`ErpMfgWorkcenter`（工作中心产能四要素——CRP 计划 `1707-1` 已建立 workcenter 日历/班次/按产品产能子实体 `ErpMfgWorkcenterCapacity`）、`ErpMfgRoutingOperation`（工序→工作中心 + 标准工时）均已落地。MRP 计划 `2237-2` 产出 WorkOrder。finance 过账引擎 `IErpFinAcctDocProvider` 已由 `0811-1`/`2030-1` 建立（APS 不过账，不需要）。
- **本计划承接的 deferred 项**（落地后解除，但 re-wiring 本身归各 owner 计划 follow-up，见 Non-Goals）：
  - CRP 计划 `1707-1` Deferred「APS OperationOrder 排程时间作为负荷来源」——APS 落地后 CRP 负荷来源可从 WorkOrder 计划日期切换为 OperationOrder 精确排程时间。
  - WorkOrder 计划 `2237-1` Deferred「APS 排产集成」——JobCard 按 OperationOrder 排程结果创建。
  - maintenance 计划 `1018-3` Deferred「停机通知制造域（排产调整）」——maintenance 发布停机、APS/CRP 消费扣减。

## Goals

- 实现 `ErpApsOperationOrder` 工序级**有限产能排产引擎**：前向排产（从 `earliestStartDateT` 正向填充）、后向排产（从 `latestEndDateT` 逆向倒推）、优先级排序（priority→requiredEndDate→sequence）。
- 实现工作中心**产能约束**：同一工作中心同一时间只排一个工序（capacity=1 默认）、维护停机时段（`ErpApsConstraint`）不可排、同 WorkOrder 工序 sequence 顺序约束。
- 实现**插单与区间重排**：急单插入时仅对窗口内优先级低于新单的 PLANNED 工序回退 DRAFT 重排（不全局重排），IN_PROGRESS 工序不可回退。
- 实现 **ATP/CTP 交期承诺模拟**：ATP（库存可用量检查）、CTP（影子 OperationOrder 前向模拟，不持久化），返回最早可交付日期/瓶颈工作中心/产能缺口。
- 实现**排产方案版本管理**：`ErpApsSchedule` DRAFT→PUBLISHED→ARCHIVED，发布即排产快照参照。
- 补齐 `ErpApsOperationOrder` 排产约束字段（`earliestStartDateT`/`latestEndDateT`），codegen 再生成。

## Non-Goals

- **甘特图前端可视化**（AMIS 甘特组件/第三方库集成）——归前端计划；本计划只提供 `getGanttData` 查询后端数据契约，不做拖拽交互后端校验（`dragUpdateOperation`）。
- **APS→CRP 负荷来源 re-wiring**——归 CRP owner 计划 follow-up（触发条件：本计划落地）；本计划只确保 OperationOrder 排程时间可被 CRP 读取。
- **JobCard 按 OperationOrder 排程自动创建**——归 manufacturing follow-up（`2237-1` Deferred 承接）。
- **maintenance 停机事件订阅扣减**——归 maintenance/manufacturing 事件机制 follow-up（`1018-3` Deferred 承接）；本计划只消费已存在的 `ErpApsConstraint` 静态维护约束。
- **人员约束（PERSONNEL）/刀具寿命（TOOL）排产**——`constraint-type` 字典已预留，但排产引擎本期仅实现 MAINTENANCE 类型约束；PERSONNEL/TOOL 归 follow-up。
- **并联产能 capacity>1 排产**——本期 capacity=1 单工位；capacity>1 并行排产归 follow-up。
- **优化求解器/智能算法**——本期为贪心前向/后向填充启发式（与 Axelor/Odoo 开源基线一致），不做 ILP/CP 优化求解。
- **自动派工（DispatchRule/DispatchLog 执行）**——实体已存在但执行引擎归 follow-up；本计划聚焦排产计算。
- **nop-job 定时自动重排 cron**——归 follow-up。

## Task Route

- Type: `implementation-only change`（含持久化模型字段补充——属模型微调，非新结果表面契约重构）
- Owner Docs: `docs/design/aps/scheduling.md`（排产算法基准）、`docs/design/aps/README.md`（APS 边界/实体/反模式）、`docs/design/manufacturing/crp.md`（CRP 与 APS 边界）
- Skill Selection Basis: 全部阶段为 Nop 后端 BizModel/模型开发——`nop-backend-dev` 匹配（决策门、xbiz 动作、实体服务自定义动作、跨实体 I*Biz 调用、ErrorCode）。Phase 1 模型字段补充触发 codegen，`nop-backend-dev` 路由的模型优先开发文档适用。

## Infrastructure And Config Prereqs

- 无新增端口/外部服务/密钥/.env。
- 配置项经 `AppConfig.var(..., defaultValue)` 读取（`ErpApsConfigs.java` 已存在，扩展）：`erp-aps.default-scheduling-mode`（默认 FORWARD）、`erp-aps.priority-rule`（默认 PRIORITY）、`erp-aps.time-bucket-minutes`（默认 15）、`erp-aps.auto-reschedule-on-insert`（默认 true）、`erp-aps.max-reschedule-window-days`（默认 30）、`erp-aps.buffer-minutes-between-ops`（默认 5）。
- 无数据迁移；新增字段 `earliestStartDateT`/`latestEndDateT` 为可空 DATETIME，存量行无影响。

## Execution Plan

### Phase 1 - 模型字段补充与 codegen

Status: completed
Targets: `module-aps/model/app-erp-aps.orm.xml`（`ErpApsOperationOrder` 加 `earliestStartDateT`/`latestEndDateT`）、`ErpApsConfigs.java`、codegen 再生成
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: 无

- [x] `Decision`：`ErpApsOperationOrder` 增 `earliestStartDateT`（DATETIME，前向起点）/`latestEndDateT`（DATETIME，后向终点）两可空列。替代方案：派生自 WorkOrder plannedStartDate（但 WorkOrder 级无法表达工序级物料齐套/前置工序完工约束，rejected）。残留风险：计划员须手工或由 MRP/WorkOrder 下发填充这两字段，空值时引擎兜底取 now/WorkOrder plannedStartDate。
  - Skill: `nop-backend-dev`
- [x] `Add`：`app-erp-aps.orm.xml` 加上述两列（propId 续号，i18n + tagSet 一致），`ErpApsConfigs.java` 补 6 个配置项常量与默认值。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpApsErrors.java` 新增排产 ErrorCode（`ERR_APS_NO_AVAILABLE_SLOT`/`ERR_APS_DEADLINE_NOT_REACHABLE`/`ERR_APS_OP_IN_PROGRESS_NOT_RESCHEDULABLE`/`ERR_APS_CAPACITY_CONFLICT`/`ERR_APS_ROUTING_SEQUENCE_INVALID`，中文描述）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：`mvn clean install -DskipTests -pl module-aps -am` BUILD SUCCESS，新字段 codegen 进 entity/dao/xmeta。
  - Skill: none

Exit Criteria:

- [x] OperationOrder 含 earliestStartDateT/latestEndDateT 列，codegen 产物（entity/xmeta）含新字段，module-aps 单模块 `mvn install -DskipTests` 通过（解除 Phase 2 编译依赖）

### Phase 2 - 排产引擎核心（前向/后向/优先级 + 产能约束）

Status: completed
Targets: `module-aps/erp-aps-service/.../schedul*`（新建排产引擎类）、`ErpApsOperationOrderBizModel.java`（暴露 `scheduleForward`/`scheduleBackward` 自定义动作）
Skill: `nop-backend-dev`

- Item Types: `Add`（统一 Add-heavy）
- Prereqs: Phase 1

- [x] `Add`：工作中心可用时段构建器——读取 `ErpMfgWorkcenter` 日历/班次（CRP 已建立）+ `ErpApsConstraint`（MAINTENANCE 类型 startTime~endTime）生成工作中心可用时间轴（内存模型，不持久化 TimeSlot）。`ErpApsTimeSlot` 概念仅内存表达。
  - Skill: `nop-backend-dev`
- [x] `Add`：前向排产 `scheduleForward(operationOrders)`——按 (priority ASC, requiredEndDate/scheduling.deadline ASC, sequence ASC) 排序，从 `max(earliestStartDateT, workcenterPtr)` 起在工作中心可用轴上找连续可用时段，分配 plannedStartDateT/plannedEndDateT，推进工作中心指针；同 WorkOrder 下工序 earliestStartDateT ≥ 前工序 plannedEndDateT + buffer；无可用时段标记 UNSCHEDULABLE + 冲突报告。
  - Skill: `nop-backend-dev`
- [x] `Add`：后向排产 `scheduleBackward(operationOrders, latestEndDateT)`——sequence DESC，从 currentDeadline 逆向找可用时段，推算每工序最晚开工；交期不可达标记 LATE + gapReport。
  - Skill: `nop-backend-dev`
- [x] `Add`：产能约束校验——同一工作中心同一时段不重叠（capacity=1）；setupTime 计入 duration；相邻工序 buffer（`erp-aps.buffer-minutes-between-ops`）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpApsOperationOrderBizModel` 暴露 `@BizMutation scheduleForward(scheduleId)`/`scheduleBackward(scheduleId)`——按 ErpApsSchedule.horizonStart/horizonEnd + schedulingMode 拉取待排 OperationOrder（status=DRAFT），调引擎，写回 plannedStartDateT/plannedEndDateT，status→PLANNED；不可排的留 DRAFT 并附冲突原因。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 前向排产对给定 WorkOrder 工序链产出单调递增的 plannedStartDateT/plannedEndDateT，前置工序完工先于后置开工；维护停机时段内无工序落入；工作中心无时段重叠（行为测试覆盖）

### Phase 3 - 插单区间重排 + 排产方案版本

Status: completed
Targets: `ErpApsOperationOrderBizModel.java`（`insertRushOrder`/`rescheduleWindow`）、`ErpApsScheduleBizModel.java`（`publish`/`archive`）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2

- [x] `Add`：插单 `insertRushOrder(operationOrderId)`——检测新工序时间窗口 `[earliestStartDateT, latestEndDateT+buffer]`；窗口内优先级低于新单的 PLANNED 工序回退 DRAFT，优先级高于新单的保留，IN_PROGRESS 工序永不回退（抛 `ERR_APS_OP_IN_PROGRESS_NOT_RESCHEDULABLE`）；窗口内 DRAFT 工序（含新单）重排；窗口外工序不受影响。
  - Skill: `nop-backend-dev`
- [x] `Add`：区间重排范围控制——仅影响同工作中心同窗口 PLANNED 且优先级低工序（最小影响范围原则，`scheduling.md §6.2`）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpApsScheduleBizModel` 状态迁移 `publish`（DRAFT→PUBLISHED，锁定为执行参照）/`archive`（DRAFT|PUBLISHED→ARCHIVED）；PUBLISHED 排产结果供 manufacturing JobCard 创建参照（弱引用，归 manufacturing follow-up）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 插单只回退窗口内低优先级 PLANNED 工序，IN_PROGRESS 与窗口外工序不变；ErpApsSchedule DRAFT→PUBLISHED→ARCHIVED 状态机迁移正确（行为测试覆盖）

### Phase 4 - ATP/CTP 交期承诺模拟

Status: completed
Targets: `module-aps/erp-aps-service/.../atpctp/`（新建 `IErpApsAtpCtpService` + 实现）、`ErpApsOperationOrderBizModel`（`checkFeasibility`/`earliestCompletionDate` 查询动作）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 2

- [x] `Add`：`IErpApsAtpCtpService.earliestCompletionDate(materialId, qty)`——ATP：检索 inventory 现有量 + 计划入库（PLANNED OperationOrder 关联物料）− 已预约（销售订单锁定，经 `IErpInvReservationBiz` 只读聚合，跨实体注入 I*Biz）− 安全库存；不足触发 CTP。
  - Skill: `nop-backend-dev`
- [x] `Add`：`checkFeasibility(materialId, qty, desiredDate)` CTP——按物料追溯工艺路线/瓶颈工作中心，创建**影子 OperationOrder（不持久化）**，在现有排产方案上模拟前向排产，返回 `CtpResult{feasible, earliestCompletionDate, bottleneckWorkcenter, capacityGap}`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`simulateSchedule(materialId, qty, startDate)`——返回模拟排程各工序时间（用于展示承诺排程，不落库）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：ATP 库存聚合跨实体调用——**实现采用 `IDaoProvider` 只读实体查询**（非跨域 I*Biz 强注入）。原拟 inventory 域 I*Biz（`IErpInvStockBalanceBiz`/`IErpInvReservationBiz`）只读查询，但跨域 I*Biz 强注入在 aps-service 单模块部署/测试时因依赖模块未组装而启动失败，破坏模块独立性，故改为 `IDaoProvider` 只读聚合（等价、零启动耦合；仅只读、非裸 SQL、未破坏物理边界；完整 app-erp-all 部署等价）。CTP 影子工序经 `IEntityDao.newEntity()` 构造不持久化。偏离已记于 `scheduling.md` 实现偏离补注。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] ATP 库存充足返回 earliestShipDate；不足时 CTP 模拟产出 earliestCompletionDate，feasible 与 desiredDate 比较正确；影子 OperationOrder 不持久化（查询无新增行）（行为测试覆盖）

### Phase 5 - 行为测试与收尾

Status: completed
Targets: `module-aps/erp-aps-service/src/test/...`、`docs/logs/2026/07-04.md`、`docs/backlog/extended-roadmap.md`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 4

- [x] `Proof`：`TestErpApsSchedulingEngine`——前向/后向排产、前置工序约束、维护停机避让、工作中心不重叠、插单窗口重排、IN_PROGRESS 不可回退、ATP/CTP 模拟（含影子不持久化断言）。JunitAutoTestCase + 断言行为（成功/失败模式各覆盖）。
  - Skill: `nop-testing`
- [x] `Add`：`docs/logs/2026/07-04.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` 工作项 3.10/3.11 标 done；`scheduling.md` 偏离（贪心启发式非优化求解/MAINTENANCE 单约束/capacity=1/gantt 前端 Non-Goal）补注。
  - Skill: none

Exit Criteria:

- [x] 排产引擎全行为测试通过（前向/后向/插单/ATP-CTP 各路径）

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_0d5730465ffeoUuD1wcE52V73f`，独立 general 子代理，冷重播无执行者上下文）。全部 baseline 声明经实时仓库独立核实为 TRUE（6 APS 实体/OperationOrder 缺 earliestStartDateT/latestEndDateT/TimeSlot 缺失/CRP+WorkOrder+maintenance 三处 deferred 承接/manufacturing workcenter 依赖/scheduling.md 设计对齐）。无 BLOCKER。3 个非阻塞 nit 已修订：Phase 1 头类型补 `Proof`（`Add | Decision | Proof`）、Phase 4 头类型补 `Decision`（`Add | Decision`）、ATP 跨实体接口名修正为 `IErpInvStockBalanceBiz`/`IErpInvReservationBiz` 并命名已预约来源。Plan Status 置 `active`。

## Closure Gates

- [x] 范围内行为完成（排产引擎 + 插单 + ATP/CTP + 方案版本）
- [x] 相关文档对齐（`scheduling.md` 偏离补注、roadmap 3.10/3.11 done）
- [x] 已运行验证：`mvn clean install -DskipTests`（根）+ `mvn test -pl module-aps -am`
- [x] 无范围内项目降级为 deferred/follow-up（甘特前端/CRP re-wiring/JobCard 创建/maintenance 事件/PERSONNEL/TOOL/capacity>1/优化求解/自动派工/cron 均为计划内 Non-Goal）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Closure

- **结束审计**：独立子代理（`ses_0d54c69fdffeLAA1mW4V16eoS4`，fresh general session，冷启动无执行者上下文）于 2026-07-04 执行结束审计，**Verdict: PASS**。10 项检查清单全部经实时仓库核实为 TRUE（ORM propId 27/28 + codegen 产物 / 6 配置项 + 5 ErrorCode + 中文描述 / 引擎前向(priority,latestEndDateT,sequence)+后向(seq DESC)+维护避让+capacity=1+buffer+前序约束 / IBiz @BizMutation 声明+实现 / insertRushOrder IN_PROGRESS 抛错+低优先级回退+高优先级冻结 / ATP onHand−reserved≥qty + CTP 影子 newEntity 不 save + CtpResult / bean 注册 / 6 项行为测试覆盖 / roadmap+scheduling.md+log 对齐 / Phase 1-5 状态与勾选一致）。构建：`mvn test -pl module-aps/erp-aps-service -am -Dtest=TestErpApsSchedulingEngine` → 6 tests pass, BUILD SUCCESS。根 `mvn clean install -DskipTests` 全绿无下游破坏。
- **修订**：审计 nit——Phase 4 Decision 文本与实现（IDaoProvider 替代跨域 I*Biz）对齐，偏离三处文档化（代码注释 / 07-04 日志 / scheduling.md 实现偏离补注）。

## Deferred But Adjudicated

### CRP 负荷来源切换为 OperationOrder 排程时间

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 CRP owner 计划 follow-up（`1707-1` Deferred 承接）；本计划确保 OperationOrder 排程时间可读，re-wiring 是 CRP 结果表面。
- Successor Required: yes（触发条件：本计划落地后，CRP 计划切换负荷来源）
- **Status: resolved**（plan `2026-07-05-0306-2-crp-load-source-aps-operationorder.md` 已落地 SPI `IErpApsLoadSourceProvider`（声明于 mfg-dao、实现于 aps-service）+ config `erp-mfg.crp-load-source` 双源门控，CRP 可读 OperationOrder 排程时间作为负荷来源）。

### JobCard 按 OperationOrder 排程自动创建

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 manufacturing follow-up（`2237-1` Deferred 承接）；本计划 ErpApsSchedule PUBLISHED 提供弱参照，JobCard 创建是 manufacturing 结果表面。
- Successor Required: yes（触发条件：本计划落地 + manufacturing JobCard 排程集成需求）

### maintenance 停机事件订阅扣减可用时段

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 maintenance/manufacturing 事件机制 follow-up（`1018-3` Deferred 承接）；本计划只消费静态 `ErpApsConstraint`，事件驱动扣减是跨域结果表面。
- Successor Required: yes（触发条件：maintenance 停机事件机制落地）

### PERSONNEL/TOOL 约束排产 / capacity>1 并联 / 优化求解器 / 甘特拖拽后端 / 自动派工执行 / cron 自动重排

- Classification: `optimization candidate`
- Why Not Blocking Closure: `constraint-type` 字典与 DispatchRule/DispatchLog 实体已预留；本期贪心启发式 + MAINTENANCE 单约束 + capacity=1 覆盖开源基线（Axelor/Odoo）能力。
- Successor Required: yes（触发条件：人员/刀具约束、并联产能、优化求解、可视化拖拽、自动派工、定时重排需求时）
