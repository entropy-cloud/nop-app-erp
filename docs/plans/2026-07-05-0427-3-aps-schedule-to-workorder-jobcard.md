# 2026-07-05-0427-3-aps-schedule-to-workorder-jobcard APS 排程 → 工单/工序卡自动生成

> Plan Status: completed
> Last Reviewed: 2026-07-05
> Source: deferred 项承接（`2026-07-04-0831-1-aps-operation-order-scheduling-engine.md` Deferred「JobCard 按 OperationOrder 排程自动创建」，触发条件「本计划落地 + manufacturing JobCard 排程集成需求」——APS 排产引擎已 completed；`2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md` 工单/工序卡状态机已 completed，本计划为其排程集成后继）
> Related: `2026-07-04-0831-1-aps-operation-order-scheduling-engine.md`（APS 排产引擎，JobCard 自动创建 Deferred）；`2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md`（WorkOrder/JobCard 状态机）；`2026-07-05-0306-2-crp-load-source-aps-operationorder.md`（APS 排程时间已可被 CRP 跨域读）
> Mission: erp
> Work Item: extended M2 制造 APS↔工单排程集成（承接 0831-1 + 2237-1 Deferred）
> Audit: required

## Current Baseline

（实时核实于 2026-07-05）

- **APS 排产引擎已落地**：`ErpApsOperationOrder`（头，含 workOrderId 弱参照、plannedStartT/plannedEndT/priority/sequence/status）+ `ErpApsOpRouting`（行，工序级，operationId 外键）；`ErpApsSchedulingEngine`（前向/后向贪心排产 + 维护避让 + 前序约束）；`ErpApsSchedulingProcessor`（Facade）；`scheduleForward`/`scheduleBackward`/`insertRushOrder`/`checkFeasibility`（CTP）@BizMutation（plan 0831-1 completed）。
- **WorkOrder/JobCard 状态机已落地**：`ErpMfgWorkOrder`（10 态状态机 + 三轴审批 + sourceOrderType SALES_ORDER/FORECAST/MANUAL）+ JobCard（领料/报工/完工 + 成本归集 + 完工质检 config-gated，plan 2237-1 completed）；`ErpMfgWorkOrder.sourceOrderType` 含 FORECAST/MANUAL 但无 `APS_SCHEDULE` 来源标记。
- **APS↔制造当前仅弱参照**：`ErpApsOperationOrder.workOrderId` 指向 WorkOrder，但 WorkOrder 侧无「按已排程 OperationOrder 自动生成 JobCard」的入口；JobCard 当前由 WorkOrder 齐套/开工流程手动驱动（2237-1）。
- **跨域读 APS 已有范式**：plan 0306-2 已落地 SPI `IErpApsLoadSourceProvider`（声明于 mfg-dao、实现于 aps-service）供 CRP 跨域读 OperationOrder 排程时间——本计划可复用同范式让制造域读已排程 OperationOrder。
- **无事件总线**：当前跨域协同为同步 I*Biz 调用（无发布/订阅事件机制）；APS 排程完成无领域事件广播。

剩余差距：APS 排程产出（OperationOrder 计划时间）与制造执行（JobCard）脱节——排程完成后需人工在制造域手工建 JobCard 并对照排程时间，无自动集成。

## Goals

- 新增制造域 @BizMutation `generateJobCardsFromSchedule(workOrderId)`：读取该 WorkOrder 关联的、已排程（有 plannedStartT/plannedEndT）的 `ErpApsOperationOrder` + `ErpApsOpRouting` 工序，按工序生成 JobCard（每工序一卡），回写 JobCard 计划开工/完工时间 = 对应 OperationOrder 排程时间，并标记来源。
- WorkOrder/JobCard 新增 `sourceScheduleId` 弱参照（加性可选列），承接「由 APS 排程生成」溯源；`sourceOrderType` 扩展 `APS_SCHEDULE`（字典加性，保护区域契约同步）。
- 幂等：已生成 JobCard 的 WorkOrder 重复调用报 `ERR_JOB_CARDS_ALREADY_GENERATED` 或按 config 增量补建缺失工序卡（Decision 裁定）。
- config-gated 自动触发开关 `erp-mfg.jobcard-auto-generate-on-schedule`（默认 false 手动触发；true 时排程完成自动调，需制造域轮询/查询入口，见 Decision）。

## Non-Goals

- **事件驱动实时自动**（APS 排程完成即广播事件、制造域订阅即时建卡）：需事件总线基础设施（当前无），本期仅手动 @BizMutation 入口 + config-gated 自动（轮询查询范式），事件驱动留后继。
- **JobCard 重排/同步**（APS 重排后 JobCard 时间自动更新）：本期仅首次生成；重排后同步留后继（需冲突解决：已开工 JobCard 是否可改时间）。
- **人员/刀具派工**（DispatchRule/DispatchLog 实体已预留但本期不填）：仍归 0831-1 Deferred。
- **甘特拖拽后端 / 自动派工执行 / cron 自动重排**：仍归 0831-1 Deferred。
- **多工序卡拆分**（一工序按数量拆多卡）：本期一工序一卡，数量承载于 JobCard（匹配 2237-1 模型）。

## Task Route

- Type: `implementation-only change`（承接已裁定 Deferred；新增制造域 BizModel 方法 + 加性弱参照列 + 字典加性扩展，无既有契约破坏）
- Owner Docs: `docs/design/manufacturing/state-machine.md`（WorkOrder/JobCard 状态机 + APS 排程来源补注）；`docs/design/aps/scheduling.md`（排程产物 → 工单/工序卡的消费方补注）；`docs/design/manufacturing/README.md`
- Skill Selection Basis: 涉及 BizModel 方法 + ORM 加性列 + 字典同步 + 跨域读 APS（I*Biz/SPI 范式），加载 `nop-backend-dev`（实体服务、跨实体调用、ErrorCode）。

### Key Decisions

- **Decision: 触发机制**
  - 选择：制造域 @BizMutation `generateJobCardsFromSchedule(workOrderId)` 手动入口为主；config-gated `erp-mfg.jobcard-auto-generate-on-schedule`（默认 false）。自动模式经制造域查询「已排程但未生成 JobCard」的 WorkOrder 列表入口（@BizQuery）+ 外部调度（nop-job，参照 0306-1 范式）触发，非事件驱动。
  - 替代方案：(a) APS 排程完成广播领域事件、制造域订阅——拒绝，当前无事件总线基础设施；(b) 制造域轮询每条 OperationOrder——拒绝，批量查询 WorkOrder 级更高效且符合既有 nop-job 批处理范式。
  - 残留风险：自动模式有延迟（轮询周期），非实时；手动入口为主保证可控。
- **Decision: JobCard 粒度**
  - 选择：一工序（`ErpApsOpRouting`）一 JobCard，数量 = WorkOrder 计划生产量，计划时间 = 该工序对应 OperationOrder 的 plannedStartT/plannedEndT。
  - 替代方案：按数量拆多卡——拒绝，2237-1 JobCard 模型为一工序一卡，拆分引入并行/批次语义复杂度，归后继。
- **Decision: 幂等策略**
  - 选择：config-gated——默认重复调用抛 `ERR_JOB_CARDS_ALREADY_GENERATED`；`erp-mfg.jobcard-incremental-rebuild`（默认 false）true 时仅补建缺失工序卡（已存在的不重建不删）。
  - 替代方案：每次重建——拒绝，已开工 JobCard 不能随意删除（状态机保护）。

## Infrastructure And Config Prereqs

- 新增 config 项：`erp-mfg.jobcard-auto-generate-on-schedule`（默认 false）、`erp-mfg.jobcard-incremental-rebuild`（默认 false）。
- 自动模式依赖 nop-job 调度（参照 0306-1 已落地范式）；无新基础设施。
- 无数据迁移（加性可选列）；回滚策略：移除 @BizMutation + 还原字典（保留加性列无害）。

## Execution Plan

### Phase 1 - WorkOrder 加性弱参照 + 字典扩展 + 跨域读 APS

Status: completed
Targets: `module-manufacturing/model/app-erp-manufacturing.orm.xml`（WorkOrder/JobCard 加性列）；`erp-mfg/source-order-type` 字典加 `APS_SCHEDULE`；codegen；`IErpApsOperationOrderBiz`/SPI 查询方法（读已排程 OperationOrder + OpRouting）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无

- [x] `Decision`: 在计划记录触发机制 / JobCard 粒度 / 幂等策略裁决（见 Task Route Key Decisions）
  - Skill: `nop-backend-dev`
- [x] `Add`: `ErpMfgWorkOrder`/`ErpMfgJobCard` 加性可选列 `sourceScheduleId`（弱参照，null=非排程生成）；`erp-mfg/source-order-type` 字典加性追加 `APS_SCHEDULE`（保护区域契约同步，参照 0831-2 字典同步范式）；重新 codegen
  - Skill: `nop-backend-dev`
- [x] `Add`: 制造域读 APS 已排程 OperationOrder + OpRouting 的查询入口——经 I*Biz（`IErpApsOperationOrderBiz` 声明查询方法于 aps-dao）或复用 `IErpApsLoadSourceProvider` 同范式 SPI（记录选用理由）
  - Skill: `nop-backend-dev`

Exit Criteria:

> 仅证明加性列/字典生成 + 跨域查询可用；建卡逻辑在 Phase 2 验证。

- [x] WorkOrder/JobCard `sourceScheduleId` 列 + `APS_SCHEDULE` 字典 codegen 产物存在，制造域编译通过
- [x] 跨域查询返回指定 WorkOrder 的已排程 OperationOrder + 工序行（非空验证）

### Phase 2 - generateJobCardsFromSchedule 建卡逻辑

Status: completed
Targets: `ErpMfgWorkOrderBizModel`（或 purpose-built Processor）；`ErpMfgErrors`（新 ErrorCode）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] `Add`: @BizMutation `generateJobCardsFromSchedule(workOrderId)`——按工序（ErpApsOpRouting）生成 JobCard（一工序一卡），计划时间 = 对应 OperationOrder plannedStartT/plannedEndT，回写 `sourceScheduleId` + WorkOrder `sourceOrderType=APS_SCHEDULE`；JobCard 初始状态对齐 2237-1 状态机入口
  - Skill: `nop-backend-dev`
- [x] `Add`: 幂等门控——默认重复调用抛 `ERR_JOB_CARDS_ALREADY_GENERATED`；config `erp-mfg.jobcard-incremental-rebuild`=true 时仅补建缺失工序卡（已存在不重建不删）；校验 WorkOrder 状态允许建卡（对齐 2237-1 状态门）
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 调用后该 WorkOrder 每道已排程工序各生成一 JobCard，计划时间 = 排程时间，来源标记 `APS_SCHEDULE` + sourceScheduleId 回写
- [x] 重复调用默认抛错；incremental 模式仅补缺；状态不允许时抛 ErrorCode

### Phase 3 - 自动触发入口（config-gated）+ 测试 + owner doc

Status: completed
Targets: 自动 @BizQuery 查询入口（待自动 WorkOrder 列表）；可选 nop-job 接线（参照 0306-1）；行为测试；`docs/design/manufacturing/state-machine.md`；`docs/design/aps/scheduling.md`；`docs/logs/2026/07-05.md`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] `Add`: `findWorkOrdersPendingJobCards` @BizQuery（已排程 + 无 JobCard 的 WorkOrder 列表）+ `generatePendingJobCards` 批量入口（config-gated `erp-mfg.jobcard-auto-generate-on-schedule`）；nop-job 接线参照 0306-1 三件套范式（job bean + app-service.beans.xml + scheduler.yaml，双层门控）
  - Skill: `nop-backend-dev`
- [x] `Proof`: 行为测试——手动建卡全链 / 幂等抛错 / incremental 补缺 / 状态门控拒绝 / 无排程 WorkOrder 不建卡；策略 `JunitAutoTestCase` + GraphQL，`mvn test -pl module-manufacturing/erp-mfg-service -am`
  - Skill: `nop-backend-dev`
- [x] `Add`: owner doc 对齐——`state-machine.md` 补「APS 排程来源建卡」入口 + `APS_SCHEDULE` 来源说明；`aps/scheduling.md` 补「排程产物 → 工单/工序卡消费方」；事件驱动后继触发条件
  - Skill: none

Exit Criteria:

- [x] 新增行为测试全绿；2237-1 WorkOrder/JobCard 套件零回归；0831-1 APS 套件零回归（aps-service 无改动或仅查询方法）
- [x] owner doc 偏离补注收口

## Draft Review Record

- Independent draft review iteration 1: accept (independent review pass, 2026-07-05) because format/section/Phase 结构合规；三阶段退出标准均可测试且覆盖 Closure Gates；范围内单一结果表面（APS 排程→工单/工序卡生成）边界清晰，Non-Goals 显式排除事件驱动/重排/派工/拆卡；基线声明经实时核实准确（0831-1/2237-1/0306-1/0306-2 均 completed；`IErpApsLoadSourceProvider` SPI 存在；`source-order-type` 字典尚无 `APS_SCHEDULE`；0831-2 字典同步范式与 0306-1 nop-job 三件套引用有效）；三个 Decision 均含选择+替代+残留风险。无 Blocker/Major。Minor：Phase 1 将 I*Biz-vs-SPI 选择内嵌于 `Add` 项而非独立 `Decision`（已要求记录选用理由，符合规则精神，留待结束审计/深度审计复查）。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成（手动建卡 + 幂等 + config-gated 自动入口）
- [x] 相关文档对齐（state-machine.md / scheduling.md / 当日日志）
- [x] 已运行验证：根 `mvn clean install -DskipTests` + `mvn test -pl module-manufacturing/erp-mfg-service -am`
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 事件驱动实时自动（APS 排程完成广播事件 → 制造域订阅即时建卡）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 当前无事件总线基础设施；本期手动入口 + nop-job 轮询自动。
- Successor Required: yes（触发条件：平台领域事件机制落地时）

### JobCard 重排/同步（APS 重排后已生成 JobCard 时间自动更新）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 需冲突解决（已开工 JobCard 是否可改时间）；本期仅首次生成。
- Successor Required: yes（触发条件：APS 重排与车间执行双向同步需求落地时）

### 人员/刀具派工 / 甘特拖拽后端 / cron 自动重排

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 仍归 0831-1 Deferred（DispatchRule/DispatchLog 已预留但本期不填）。
- Successor Required: yes（触发条件：人员/刀具约束派工 / 可视化拖拽 / 定时重排需求落地时）

## Closure

Status Note: 已完成（2026-07-05）。三阶段全部落地：Phase 1 ORM 加性列（WorkOrder/JobCard `sourceScheduleId`）+ 新字典 `erp-mfg/source-order-type`（含 APS_SCHEDULE）+ 复用 `IErpApsLoadSourceProvider` SPI 跨域读 APS；Phase 2 `ErpMfgScheduleToJobCardProcessor` + `generateJobCardsFromSchedule` @BizMutation（一工序一卡、OPEN 入口、sourceScheduleId 回写、WorkOrder sourceOrderType=APS_SCHEDULE、幂等门控 + incremental 补缺 + 状态门）；Phase 3 `findWorkOrdersPendingJobCards`/`generatePendingJobCards` + nop-job 三件套（`ErpMfgJobCardAutoGenJob` + scheduler.yaml，双层门控）+ 7 case 行为测试 + owner doc 对齐。验证 full-green：根 `mvn clean install -DskipTests`（146 模块 + app-erp-all）BUILD SUCCESS；`mvn test -pl module-manufacturing/erp-mfg-service -am` Tests run: 63（56 既有零回归 + 7 新增）；`mvn test -pl module-aps/erp-aps-service -am` Tests run: 11（ApsLoadSlot 加性字段零回归）。独立结束审计已由独立子代理新会话执行通过（执行者未自我审计；见 Closure Audit Evidence）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，未复用执行者上下文）
- Evidence: 实时仓库核实（2026-07-05）——Phase 1：`module-manufacturing/model/app-erp-manufacturing.orm.xml:499,960` WorkOrder/JobCard `sourceScheduleId` 加性列（propId 39/20）+ `source-order-type.dict.yaml` 含 `APS_SCHEDULE`（i18n 双语）+ `_gen` 实体/XMeta/i18n/Beans 全链生成；跨域读 APS 复用 `IErpApsLoadSourceProvider` SPI（`ErpMfgScheduleToJobCardProcessor.fetchSlots` 经 `ioc:collect-beans by-type` 收集，APS 缺失降级空 list）。Phase 2：`ErpMfgScheduleToJobCardProcessor.java:73-97` `generateJobCardsFromSchedule` 一工序一卡（OPEN、plannedQuantity=工单计划量、workcenterId 来自 APS、`sourceScheduleId`=OperationOrder id）、`markWorkOrderScheduled` 回写 `sourceOrderType=APS_SCHEDULE`；`resolveSlotsToBuild:177-206` 幂等门控（默认抛 `ERR_JOB_CARDS_ALREADY_GENERATED`，config `jobcard-incremental-rebuild`=true 仅补缺）；`validateStatusForJobCardGen:165-172` 状态门（仅已审核非终态）；Facade `ErpMfgWorkOrderBizModel.generateJobCardsFromSchedule:98` @BizMutation 委托；3 ErrorCode 定义于 `ErpMfgErrors.java:148,153,158`。Phase 3：`findWorkOrdersPendingJobCards`/@BizQuery + `generatePendingJobCards`/@BizMutation（`ErpMfgWorkOrderBizModel:104-113`，config-gated + 单工单失败隔离）；nop-job 三件套——`ErpMfgJobCardAutoGenJob.java`（双层门控 cron+总开关）注册于 `app-service.beans.xml:76` + `scheduler.yaml:57`（`erp-mfg-jobcard-auto-generate`）；`TestErpMfgScheduleToJobCard.java` 7 case（happy/幂等/incremental/状态门 draft+终态/无排程/批量 config-gated）全链 GraphQL 断言。Anti-hollow：Processor 336 行实体逻辑无空体/return null/吞异常；beans.xml 注册 + scheduler.yaml 接线 + test stub（`TestStubApsLoadSourceProvider`）均落地。Deferred 3 项均 out-of-scope improvement + successor required + 触发条件。文本一致性：Plan Status=completed / 三 Phase Status=completed / 全部退出标准 [x] / 全部 Closure Gates [x] / `docs/logs/2026/07-05.md` 聚合日志条目一致。

Follow-up:

- 事件驱动实时自动（见上方 Deferred）
- JobCard 重排/同步（见上方 Deferred）
