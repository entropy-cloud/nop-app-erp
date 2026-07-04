# 2026-07-05-0306-2-crp-load-source-aps-operationorder CRP 负荷来源切换为 APS OperationOrder 排程时间

> Plan Status: completed
> Last Reviewed: 2026-07-05
> Source: `docs/plans/2026-07-03-1707-1-manufacturing-crp-load-engine.md` Deferred「APS OperationOrder 排程时间作为负荷来源」+ `docs/plans/2026-07-04-0831-1-aps-operation-order-scheduling-engine.md` Deferred「CRP 负荷来源切换为 OperationOrder 排程时间」（双计划互指后继，触发条件均已满足：APS 3.10/3.11 done、CRP 2.8 done）
> Related: `2026-07-03-1707-1`（CRP 引擎基线，本期改其负荷来源）、`2026-07-04-0831-1`（APS 排程引擎，提供 OperationOrder 排程时间）、`2026-07-05-0306-1-scheduler-wire-periodic-jobs.md`（crp-run cron 注册，与本计划正交：注册触发 vs 负荷来源）
> Audit: required

## Current Baseline

实时仓库核实：

- **CRP 负荷来源当前 = WorkOrder 计划日期（fallback，无 APS）**：`CrpLoadCalculator.findWorkOrdersInWindow` 按 `WorkOrder.plannedStartDate ≤ periodTo AND plannedEndDate ≥ periodFrom` 过滤（`module-manufacturing/erp-mfg-service/.../crp/CrpLoadCalculator.java:211-212`）；`woStart`/`woEnd` 取 `WorkOrder.plannedStartDate/plannedEndDate`（:230,241），按 RoutingOperation 分派 standardTime→loadHours、setupTime→首日 setupHours。类注释 `:43` 自述"负荷来源（fallback，无 APS）"，`:51` 列"APS 工序级排产（写 OperationOrder）"为 Non-Goal——即本计划要解除的 Deferred。
- **APS 排程时间的承载实体已确认（粒度可达），引擎是否回填待 Phase 1 核实**：`module-aps/model/app-erp-aps.orm.xml` 中 **`ErpApsOperationOrder`**（`:55-91`）即工序级排产单元，携带 `workOrderId`（propId 3，关联 WorkOrder）+ `sequence`（5，工序顺序）+ `machineId`（6，工作中心/设备）+ `plannedStartDateT`/`plannedEndDateT`（8/9，计划开工/完工时间）+ `setupTime`（12）+ `earliestStartDateT`/`latestEndDateT`（27/28）——**这恰好是 CRP 所需的 RoutingOperation×workCenter×time 粒度**。注意区分：`ErpApsSchedule`（`:94-118`）是排产方案/场景（仅 `horizonStart`/`horizonEnd` 展望期窗口 + `status`，**非**工序时间）；`ErpApsConstraint`（`:121-138`）的 `startTime`/`endTime`（propId 4/5）是维护/可用性约束窗口，**非**排程工序时间。**开放问题（Phase 1）**：APS 引擎（计划 `0831-1`）在 PUBLISH 时是否实际回填 `OperationOrder.plannedStartDateT/plannedEndDateT`（实体有字段，但引擎写入路径须核实——若引擎只产出内部排程而不回填 OperationOrder，则 CRP 无可读数据）。
- **模块依赖方向（关键约束）**：`erp-mfg-service` 当前**不依赖** `erp-aps`（`rg erp-aps module-manufacturing/erp-mfg-service/pom.xml` 零命中）；反向 `erp-aps-service` **已** compile 依赖 `app-erp-manufacturing-dao`（`module-aps/erp-aps-service/pom.xml:38-42`），`erp-aps-dao` 不依赖 mfg。既有方向为 aps→mfg-dao（R）。因此 mfg-service 直接 compile 依赖 aps-dao 虽不单独成环（aps-dao 不反向依赖 mfg），但仍把 APS ORM 实体耦合进 mfg——SPI 反转可避免。
- **既有可复用模式（机制属实，放置层需个案裁决）**：跨域解耦 SPI 经 `ioc:collect-beans by-type` 收集在本仓已成文——`IErpFinAcctDocProvider`/`IErpFinVoucherReversedListener` **声明于 `module-finance/erp-fin-service/.../posting/`**（非 fin-dao），跨域实现散布 inv/sal/pur/ast/hr/prj/log 各 service（`module-finance/erp-fin-service/.../beans/app-service.beans.xml:16,31` 收集）；承运商网关 SPI 为 `IErpLogCarrierGatewayClient`/`IErpLogCarrierGatewayClientFactory`（`module-logistics/erp-log-service/.../spi/`）。故"接口放哪个模块"无固定规则——本案因 aps-service 已依赖 mfg-dao，放 mfg-dao 可让 aps 零新 compile 依赖实现；最终放置由 Phase 1 Explore 裁决（见 Task Route Decision）。

### 剩余差距

(1) CRP 负荷来源硬绑 WorkOrder 计划日期，无 APS 排程时间路径；(2) APS 引擎在 PUBLISH 时是否回填 `ErpApsOperationOrder.plannedStartDateT/plannedEndDateT`（粒度可达，写入路径待 Phase 1 核实）；(3) 无跨域读取 APS 排程时间的解耦契约（接口放置层待裁决）；(4) `crp.md` 负荷来源章节仍写"fallback，无 APS"，与 APS 已落地不符（owner-doc 漂移）。

## Goals

- 为 CRP 增加 APS `ErpApsOperationOrder` 排程时间（`plannedStartDateT/plannedEndDateT` × `machineId` 工作中心 × `sequence` 工序）作为可选负荷来源（config-gated），APS 启用且工单存在已排程 OperationOrder 时按排程时间分派负荷到工作中心×日；否则回退 WorkOrder 计划日期（行为不变）。
- 以跨域 SPI 契约（接口放置层由 Phase 1 裁决——倾向 mfg-dao 以复用 aps→mfg-dao 既有依赖；aps-service 实现、CrpLoadCalculator 可选注入）读取 APS 排程时间，避免 mfg→aps 编译依赖与 APS ORM 实体耦合进 mfg。
- 修正 `crp.md` 负荷来源章节漂移。

## Non-Goals

- **不改 CRP 负荷计算/聚合/超载告警算法**（standardTime→loadHours、setup→首日、capacityHours、loadRate、overloaded 阈值均不变）。
- **不改 APS 排程引擎**（只消费其产出的排程时间，不回写）。
- **不触发 crp-run cron 注册**（归 `0306-1`，与本计划正交）。
- **maintenance 停机扣减可用时段**（归 `1707-1` 另一 Deferred，事件机制未落地）。
- **CRP 可视化页面/班次级粒度**（归 `1707-1` Deferred）。
- **JobCard 按 OperationOrder 自动创建**（归 `0831-1` Deferred，触发条件"JobCard 排程集成需求"未满足）。

## Task Route

- Type: `architecture change`（跨域 SPI 契约 + 模块边界）+ `implementation-only change`（CRP 负荷来源分支）。
- Owner Docs: `docs/design/manufacturing/crp.md`（负荷来源章节修正）、`docs/architecture/`（若引入跨域 SPI 记模块边界）。
- Skill Selection Basis: BizModel/Processor 跨域读、SPI 接口设计、IDaoProvider、config-gated 分支、JunitAutoTestCase——匹配 `nop-backend-dev`。
- **Decision（跨域读取 APS 排程时间的契约形态），三选一**：
  (a) **SPI 反转**——声明 `IErpApsLoadSourceProvider`（入参 workOrderId/period 窗口，出参按 OperationOrder 序列的 workCenter×plannedStart~End 时段集合），aps-service 实现（读自身 `ErpApsOperationOrder` 按 `workOrderId` 聚合 `machineId`×`plannedStartDateT/plannedEndDateT`×`setupTime`），CrpLoadCalculator `@Inject` 可选（null when APS 缺），beans.xml `ioc:collect-beans by-type` 收集。**接口放置层**：倾向 mfg-dao（因 aps-service 已 compile 依赖 mfg-dao，aps 可零新依赖实现）；与 fin 范式（接口在 fin-service）不同，本案个案裁决——Phase 1 Explore 最终确认放置层与 collect-beans 跨模块可收集性。
  (b) mfg-service compile 依赖 aps-dao，CrpLoadCalculator 经 `IDaoProvider.daoFor(ErpApsOperationOrder.class)` 直读——实现最简且不成环（aps-dao 不反向依赖 mfg），但把 aps ORM 实体耦合进 mfg，且违反 AGENTS.md"优先 I*Biz / SPI，IDaoProvider 仅当 I*Biz 无法满足并注释"的跨域访问偏好。
  (c) APS 排程完成后回写排程时间到 mfg 拥有字段（反规范化）——重复存储 + 一致性维护，过度工程。
  - **倾向 (a) SPI 反转**（mfg 不新增 compile 依赖、不耦合 APS ORM 实体、APS 缺失时优雅降级、符合跨域 SPI 偏好）。**最终裁决经 Phase 1 Explore 确认**：aps-service 可实现该接口、collect-beans 可跨模块收集、且 APS 引擎 PUBLISH 时回填 `OperationOrder.plannedStartDateT/plannedEndDateT`（实体字段与粒度已确认存在）；**若 Explore 发现引擎未回填这些字段**（即 OperationOrder 时间为空），回退将本计划降级为 Deferred（见反松弛：不可降级的仅为已确认缺陷/漂移，本案为未证实假设，可裁定后继）。**残留风险**：SPI 出参 DTO 须表达"按 workOrderId 的工序序列（OperationOrder.sequence）× workCenter（machineId）× plannedStart~End 时段"，Phase 1 据 APS 实际实体关系定型。
- **Decision（负荷来源选择门控）**：**选择** config-gated `erp-mfg.crp-load-source`（`WORK_ORDER` 默认 | `APS`）。APS 模式下：工单有已排程 OperationOrder 时按 APS 时段分派；某工单无 OperationOrder 时按 WorkOrder 计划日期回退（混合 tolerated）。**替代**：纯 APS（无则跳过该工单）——会静默丢负荷，rejected。**残留风险**：混合模式下同一窗口可能 APS 与 WorkOrder 日期并存——以日志记录"来源分布"缓解。

## Infrastructure And Config Prereqs

- 无新增端口/密钥/.env/外部服务/数据迁移。
- 依赖 APS 引擎已落地（`0831-1` done）产出 OperationOrder 排程时间。
- 回滚策略：config `erp-mfg.crp-load-source=WORK_ORDER` 即恢复原行为；SPI 接口为加性新增，移除 aps-service 实现即降级。

## Execution Plan

### Phase 1 - Explore: APS 排程实体关系 + 模块环确认 + 契约定型

Status: completed
Targets: 本计划 Task Route Decision 段
Skill: `nop-backend-dev`

- Item Types: `Explore | Decision`
- Prereqs: 无

- [x] `Explore`：核实 APS 引擎（计划 `0831-1`）写入路径——PUBLISH 时是否回填 `ErpApsOperationOrder.plannedStartDateT/plannedEndDateT`（+ `machineId` 工作中心 + `sequence` 工序，这些字段实体已具备，达 RoutingOperation×workCenter 粒度）。若回填 → CRP 直接经 SPI 读取 OperationOrder 即可；若引擎只产内部排程不回填 → 据降级条款处置（改由 SPI 实现内部转换或降级 Deferred）。
  - Skill: `nop-backend-dev`
  - **核实结果（live-verified）**：`ErpApsSchedulingEngine.scheduleForward`（`module-aps/erp-aps-service/.../scheduling/ErpApsSchedulingEngine.java:89-90`）与 `scheduleBackward`（:145-146）**确实写回** `op.setPlannedStartDateT(start)/setPlannedEndDateT(end)` 并 `setStatus(PLANNED)`；冲突时清空为 null（:81-82, 81-82）保留 DRAFT。引擎由 `ErpApsOperationOrderBizModel.scheduleForward/scheduleBackward` 调用（0831-1 Phase 2 已落地）。结论：**APS 引擎在排程成功时回填，CRP 经 SPI 直接读取可行**，无需降级。
- [x] `Explore`：核实模块依赖环（基线已确认 aps-service→mfg-dao 既有、mfg-service→aps 不存在、aps-dao 不依赖 mfg）；并核实 `ioc:collect-beans by-type` 在 app-erp-all 合并上下文能跨模块收集 aps-service 实现的 mfg 侧接口；裁决 **接口放置层**（mfg-dao 让 aps 零新依赖实现 vs mfg-service 对齐 fin 范式）。
  - Skill: `nop-backend-dev`
  - **核实结果（live-verified）**：(1) `module-aps/erp-aps-service/pom.xml:38-42` 已 compile 依赖 `app-erp-manufacturing-dao`；(2) `module-manufacturing/erp-mfg-service/pom.xml` 无 aps 依赖（基线无误）；(3) `ioc:collect-beans by-type` 跨模块收集能力经 fin 范式证实——`IErpFinAcctDocProvider`（声明于 fin-service）跨 inv/sal/pur/ast/hr/prj/log 各 service 实现，由 `module-finance/erp-fin-service/.../beans/app-service.beans.xml:16,31` 收集，全仓启动期注入工作。本案 aps-service 实现的 mfg 侧接口在 `app-erp-all` 合并上下文同样可收集。
- [x] `Decision`：据 Explore 裁决契约形态（倾向 SPI 反转 (a)）+ 接口放置层，定型 `IErpApsLoadSourceProvider` 方法签名（入参 workOrderId/period 窗口，出参按 OperationOrder 序列的 workCenter×plannedStart~End 时段集合），记录选择+替代+残留风险到本计划。
  - Skill: `nop-backend-dev`
  - **裁决（契约形态）**：**选择 SPI 反转 (a)**——mfg 侧声明接口、aps-service 实现、`ioc:collect-beans by-type` 收集、`CrpLoadCalculator` `@Inject List<IErpApsLoadSourceProvider>`（空 list 时优雅降级）。**替代 (b) mfg-service 直依赖 aps-dao**：rejected——把 APS ORM 实体耦合进 mfg，且违反 AGENTS.md "跨域优先 I*Biz/SPI" 偏好。**替代 (c) 反规范化回写**：rejected——重复存储 + 一致性维护，过度工程。**残留风险**：SPI 出参 DTO 须表达"按 workOrderId 的工序序列 × workCenter × plannedStart~End 时段"——见下签名定型。
  - **裁决（接口放置层）**：**选择 `module-manufacturing/erp-mfg-dao/.../app/erp/mfg/biz/IErpApsLoadSourceProvider`**——因 aps-service 已 compile 依赖 mfg-dao，aps 可零新依赖实现；mfg-service 已 compile 依赖 mfg-dao，可 `@Inject` 零新依赖。**替代 mfg-service 对齐 fin 范式**：rejected——会强制 aps-service 新增 compile 依赖 mfg-service（更重，引入 BizModel 依赖）。**残留风险**：mfg-dao 持有描述 APS 数据形态的契约——但这是 SPI 反转的标准形态（消费方声明、提供方实现），可接受。
  - **接口签名定型**：
    ```java
    package app.erp.mfg.biz;
    public interface IErpApsLoadSourceProvider {
        // 按 workOrderIds 批量查询 APS 已排程（status=PLANNED）的 OperationOrder，
        // 返回每个工序的 workCenter×plannedStartT~plannedEndT 时段。
        // periodFrom/periodTo 用于过滤排程时段与窗口相交（避免拉全表）。
        // APS 未启用 / 无匹配 → 返回空 List（不返回 null）。
        List<ApsLoadSlot> findScheduledSlots(List<Long> workOrderIds, LocalDate periodFrom, LocalDate periodTo);
    }
    ```
    `ApsLoadSlot`（`@DataBean`，与 `CrpLoadReportItem` 同包）：`Long workOrderId`、`Integer sequence`、`Long workcenterId`（← OperationOrder.machineId）、`LocalDateTime plannedStartT`（← plannedStartDateT）、`LocalDateTime plannedEndT`（← plannedEndDateT）、`BigDecimal setupTime`（← OperationOrder.setupTime）。

Exit Criteria:

> Phase 1 解除 Phase 2 实现的契约阻塞。

- [x] 契约形态+接口放置层裁决落计划，接口签名定型，APS 引擎是否回填 OperationOrder 排程时间已确认（或据降级条款处置）

### Phase 2 - SPI 契约 + APS 实现 + CRP 负荷来源分支

Status: completed
Targets: `IErpApsLoadSourceProvider`（放置层按 Phase 1 裁决）+ 排程时段 DTO、`module-aps/erp-aps-service/.../ApsLoadSourceProvider.java`、`CrpLoadCalculator.java`、`ErpMfgConstants`、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] `Add`：按裁决落契约（SPI 反转则：mfg 侧声明 `IErpApsLoadSourceProvider` + 排程时段 DTO；aps-service 实现，读 `ErpApsOperationOrder` 按 `workOrderId` 聚合 `sequence`×`machineId`×`plannedStartDateT/plannedEndDateT`×`setupTime` 时段）。
  - Skill: `nop-backend-dev`
  - **落地**：`module-manufacturing/erp-mfg-dao/.../app/erp/mfg/biz/IErpApsLoadSourceProvider.java`（SPI 接口，`findScheduledSlots(workOrderIds, periodFrom, periodTo)`）+ `ApsLoadSlot.java`（`@DataBean` DTO，workOrderId/sequence/workcenterId/plannedStartT/plannedEndT/setupTime）；`module-aps/erp-aps-service/.../loadsource/ApsLoadSourceProvider.java`（实现，`@Inject IDaoProvider` 读 `ErpApsOperationOrder` 仅取 `status=PLANNED` 且排程时段与窗口相交）；`module-aps/erp-aps-service/.../beans/app-service.beans.xml` 注册 `ApsLoadSourceProvider` Bean（供 mfg 跨模块收集）。
- [x] `Add`：`CrpLoadCalculator` 增负荷来源分支——读 `erp-mfg.crp-load-source`：APS 且 provider 存在 → 按 APS 排程时段分派 loadHours/setupHours 到工作中心×日；否则回退 WorkOrder 计划日期（既有 `woStart/woEnd` 逻辑不动）。无排程时段的工单回退 WorkOrder 日期并日志记录来源。
  - Skill: `nop-backend-dev`
  - **落地**：`CrpLoadCalculator` 字段 `List<IErpApsLoadSourceProvider> apsLoadSourceProviders`（默认 emptyList，setter 注入）；`calculateLoad` 改为先决策 `apsMode = isApsLoadSourceEnabled()`（config=APS 且 providers 非空），aps 模式批量查 `indexApsSlotsByWorkOrder`，逐工单：有 slots → `distributeByApsSlots`（按 slot 跨日逐日累加 loadHours=mins/60、setupHours 计入时段首日，截断到 CRP 窗口），无 slots → `distributeByWorkOrder`（既有逻辑，重命名自 distributeWorkOrder，行为不变）；aps 模式汇总后 `LOG.info` 输出"aps 命中/WorkOrder 回退"工单计数。
- [x] `Add`：`ErpMfgConstants.CONFIG_CRP_LOAD_SOURCE`（默认 WORK_ORDER）；beans.xml 注册 APS 实现（若 SPI，aps-service app-service.beans.xml 经 collect-beans 收集）。
  - Skill: `nop-backend-dev`
  - **落地**：`ErpMfgConstants` 增 `CONFIG_CRP_LOAD_SOURCE`/`CRP_LOAD_SOURCE_WORK_ORDER`/`CRP_LOAD_SOURCE_APS`；`module-manufacturing/erp-mfg-service/.../beans/app-service.beans.xml` 修 `CrpLoadCalculator` Bean 注入 `<property name="apsLoadSourceProviders"><ioc:collect-beans by-type="app.erp.mfg.biz.IErpApsLoadSourceProvider" .../></property>`（镜像 finance `IErpFinAcctDocProvider` 跨域收集范式）。

Exit Criteria:

- [x] 契约 + APS 实现 + CRP 分支落地，非空壳（分支有真实查询/分派逻辑）；config 默认 WORK_ORDER 时行为与基线一致（既有 `TestErpMfgCrpLoad` 7 测试零回归为门控）
  - **验证**：`mvn clean install -DskipTests -pl module-manufacturing/erp-mfg-dao,module-aps/erp-aps-service,module-manufacturing/erp-mfg-service -am` BUILD SUCCESS（2026-07-05 04:04）。CRP 分支为加性新增 + `distributeByWorkOrder` 与基线 `distributeWorkOrder` 逻辑等价（仅改名）；既有 7 测试在 Phase 3 `mvn test` 验证零回归。

### Phase 3 - 行为测试

Status: completed
Targets: `module-manufacturing/erp-mfg-service/src/test/.../TestErpMfgCrpLoadSource.java`
Skill: `nop-backend-dev`

- Item Types: `Proof`
- Prereqs: Phase 2

- [x] `Proof`：(a) `crp-load-source=WORK_ORDER` → 负荷按 WorkOrder 计划日期（既有断言不变，回归）；(b) `=APS` + 工单有已排程 OperationOrder（plannedStartDateT/plannedEndDateT 已回填）→ 负荷按 APS 排程时间分派（断言落在 OperationOrder 排程日期而非 WorkOrder 计划日期）；(c) `=APS` 但工单无 OperationOrder（或时间未回填）→ 回退 WorkOrder 日期（混合 tolerated + 日志）。
  - Skill: `nop-backend-dev`
  - **落地**：`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgCrpLoadSource.java`（3 个 @Test，使用 `testBeansFile=/erp/mfg/beans/test-aps-load-source.beans.xml` 加载 `TestStubApsLoadSourceProvider` 桩实现 `IErpApsLoadSourceProvider`，避免引入 aps-service 形成 reactor 环，对齐 quality 模块 test-mock-sales 模式）：
    - `testWorkOrderSourceDefaultDistributesByWorkOrderDates`：默认 WORK_ORDER，2 天工单 → 2 行 CrpLoad，均匀分派 loadHours=4h、setup→首日。
    - `testApsSourceDistributesByOperationOrderSchedule`：APS 模式，WorkOrder 计划日期 7-15 但 stub 注入 OperationOrder 排程 7-20 09:00~13:00+setup=60min → CrpLoad 行落在 7-20（非 7-15），loadHours=4h、setupHours=1h，关键断言验证"7-15 无行"。
    - `testApsSourceFallsBackWhenNoScheduledSlot`：APS 模式但工单无 slot（模拟无 OperationOrder）→ 回退 WorkOrder 日期分派（RoutingOperation.standardTime=480min=8h、setup=1h）。

Exit Criteria:

- [x] 测试类 ≥3 case 全绿；`mvn test -pl module-manufacturing/erp-mfg-service -am` 通过（既有 CRP 7 测试零回归 + 新增通过）
  - **验证**：`mvn test -pl module-manufacturing/erp-mfg-service -am -Dtest='TestErpMfgCrpLoadSource,TestErpMfgCrpLoad' -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false` → `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`（既有 7 + 新增 3 全绿，零回归）。全仓 `mvn clean install -DskipTests` BUILD SUCCESS（146 模块，2026-07-05 04:08）。

### Phase 4 - owner-doc 漂移修正

Status: completed
Targets: `docs/design/manufacturing/crp.md`、当日日志
Skill: none

- Item Types: `Fix`
- Prereqs: Phase 1-3

- [x] `Fix`：`crp.md` 负荷来源章节由"fallback，无 APS"修正为双源（WORK_ORDER 默认 / APS 可选）+ config 键 + SPI 契约说明；移除 Non-Goal 中"APS 工序级排产"已解除项。
  - Skill: none
  - **落地**：`docs/design/manufacturing/crp.md` §边界 第 2 条澄清"APS 已落地后 CRP 可选消费 OperationOrder 排程时间"+ 第 3 条"APS 工序级排产**写入**"措辞精确化；新增 §负荷来源双源（WORK_ORDER/APS 双表 + SPI 契约 + collect-beans 跨模块收集说明）；§配置点 增 `erp-mfg.crp-load-source`；§实现偏离补注 原"负荷来源 fallback（无 APS）"改为"已解除（plan 0306-2 落地）"；cron 接线条目移除"APS 接线归 0306-2"措辞改"已接线"。`CrpLoadCalculator` 类注释 Non-Goal 中"APS 工序级排产（写 OperationOrder）"项已在 Phase 2 类注释重写时移除。
  - 当日日志：`docs/logs/2026/07-05.md` 增本计划条目（顶部，Phase 1-4 + 验证 + 关键决策）。

Exit Criteria:

- [x] `crp.md` 负荷来源章节与实现一致；`job-scheduling.md` crp-run 行无业务变更（仅负荷来源，cron 注册归 0306-1）
  - **验证**：`crp.md` §负荷来源双源 + §配置点 + §实现偏离补注 三处与 `CrpLoadCalculator.isApsLoadSourceEnabled/distributeByApsSlots` 实现一致；`job-scheduling.md` 未触动（本计划仅切负荷来源，cron 注册归 0306-1，无业务变更）。

## Draft Review Record

- Independent draft review iteration 1: needs revision（ses_0d17383deffe6uH6tx266P2IpK，general 独立子代理新会话）because 三处 Current Baseline 事实错误违反规则 1：(L1) `IErpLogCarrierClient` 不存在，实为 `IErpLogCarrierGatewayClient`/`...ClientFactory`；(L2) `IErpFinAcctDocProvider` 声明于 fin-**service**（非 fin-dao），原"声明方 dao 模块定义接口"为误述；(L3) `ErpApsOperationOrder`（:55）自身无时间字段、`ErpApsSchedule` 有时间但无 OperationOrder/RoutingOperation/workCenter 关联、`operationOrderId` 落在 `ErpApsDispatchLog`——"OperationOrder 已携带排程时间"为未证实假设。结构/范围/反空心 deferral/触发成熟度均通过；模块方向（aps-service→mfg-dao pom:38-42、mfg→aps 不存在）已确认；与 0306-1 正交已确认。
- 已修订（iter1→iter2）：(L1) 承运商 SPI 名修正 + 删除虚假声称；(L2) 修正 fin 范式接口在 fin-service，本案放置层改由 Phase 1 Explore 裁决（倾向 mfg-dao 因 aps 已依赖 mfg-dao），Decision (a)/(b) 文本同步；(L3) 基线重写为"APS 排程时间已写入但 RoutingOperation×workCenter 粒度未证实（开放问题）"，剩余差距+Goal+Decision+Phase1 Explore 全链路同步，并加降级条款（Explore 发现粒度不可达则降级 Deferred）。另修 minor：Phase 2 Item Types 由 `Add|Decision`→`Add`（Decision 已在 Phase 1 裁决）、Phase 2 Targets 删除无错误条件的 `ErpMfgErrors`、pom 直接引用 aps-service:38-42。
- Independent draft review iteration 2: needs revision（ses_0d16db311ffebxbjwFxEv5eBGB，general 独立子代理新会话）— L1/L2 已修复（承运商 SPI 名正确；fin 范式接口在 fin-service 属实且新接口放置层改由 Phase 1 裁决）。但 L3 的重写引入两处新规则 1 事实错误：(a) 把 `startTime/endTime`（:128-129）误归于 `ErpApsSchedule`，实际属 `ErpApsConstraint`，Schedule 仅 `horizonStart/horizonEnd`；(b) 误述 `ErpApsOperationOrder` 无时间字段——实际它携带 `plannedStartDateT/plannedEndDateT`/real/earliest/latest + `machineId`（工作中心）+ `sequence`+`setupTime`，恰为 CRP 所需 operation×workCenter×time 粒度。
- 已修订（iter2→iter3）：据 orm.xml:55-91/94-118/121-138 重写基线——`ErpApsOperationOrder` 是工序级排产单元（workOrderId+sequence+machineId+plannedStartDateT/plannedEndDateT+setupTime，粒度可达）；`ErpApsSchedule` 是方案/场景（horizon 窗口）；`ErpApsConstraint` 的 startTime/endTime 是约束窗口。开放问题收窄为"APS 引擎 PUBLISH 是否回填 OperationOrder.plannedStartDateT/plannedEndDateT"（字段与粒度已确认存在）。Goal/剩余差距/Decision(a)/Phase1 Explore/Phase2 Add/Phase3 Proof 全链路实体引用同步。
- Independent draft review iteration 3: accept（ses_0d169fbdcffe1qPyOpjPlQZH4c，general 独立子代理新会话）— 逐项对照 live `module-aps/model/app-erp-aps.orm.xml` 核实：`ErpApsOperationOrder`（:55-91）字段全数属实（workOrderId/sequence/machineId/plannedStartDateT/plannedEndDateT/setupTime/earliestStartDateT/latestEndDateT）；`ErpApsSchedule`（:94-118）仅 horizonStart/horizonEnd（无 startTime/endTime）；`ErpApsConstraint`（:121-143）持 startTime/endTime；开放问题已收窄为"引擎 PUBLISH 是否回填 OperationOrder 时间"；Decision/Phase1/Phase2/Phase3 实体引用全部一致，无残留误述。L1/L2 仍 fixed；降级条款反松弛合规（未证实假设经 Explore 去险，非隐藏缺陷）；Phase 2 Item Types=`Add`、ErpMfgErrors 已移除；依赖事实 live-verified。无 BLOCKER，共识达成，可转 active。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成（CRP APS 负荷来源分支 + SPI 契约 + config 门控 + 回退）
- [x] 相关文档对齐（`crp.md` 负荷来源修正；当日日志）
- [x] 已运行验证：根 `mvn clean install -DskipTests` + `mvn test -pl module-manufacturing/erp-mfg-service -am`
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### maintenance 停机扣减工作中心可用时段

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 事件驱动，需 maintenance 停机事件机制（当前 maintenance 停机通知制造为 Non-Goal）；本计划只切负荷来源。
- Successor Required: yes（触发条件：maintenance 停机事件 + 排产停机窗口联动需求时）

### CRP 可视化页面 / 班次级粒度

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 `1707-1` 既有 Deferred（前端 / 细粒度），与本负荷来源切换无关。
- Successor Required: yes（触发条件：CRP 报表前端 / 班次级负荷需求时）

### JobCard 按 OperationOrder 自动创建

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 `0831-1` Deferred，触发条件含"manufacturing JobCard 排程集成需求"未满足；本计划只读 APS 排程时间不改 JobCard 创建。
- Successor Required: yes（触发条件：JobCard 排程集成需求时）

## Closure

Status Note: CRP 负荷来源双源（WORK_ORDER 默认 / APS 可选）已实现并通过全仓验证。SPI 反转契约 `IErpApsLoadSourceProvider`（声明于 mfg-dao 消费方、实现于 aps-service 提供方）+ DTO `ApsLoadSlot` 落地；`ioc:collect-beans by-type` 在 `app-erp-all` 合并上下文跨模块收集（镜像 finance `IErpFinAcctDocProvider` 范式），APS 模块缺失时 mfg 侧收集到空 list，CRP 回退 WORK_ORDER（行为不变）。`CrpLoadCalculator.calculateLoad` 经 config `erp-mfg.crp-load-source` 门控决策 apsMode：APS 模式 + 工单有已排程 OperationOrder → `distributeByApsSlots`（按 plannedStartT~plannedEndT 跨日逐日累加 loadHours=mins/60、setup→时段首日）；APS 模式但工单无 slot → `distributeByWorkOrder` 回退（既有 RoutingOperation.standardTime 均匀分派，行为不变）。混合 tolerated + INFO 日志记录"aps 命中/WorkOrder 回退"工单计数。Phase 1 决策经 Explore 核实（APS 引擎在 `ErpApsSchedulingEngine:89-90,145-146` 回填 OperationOrder 时间）；Phase 2 SPI+实现+CRP 分支落地；Phase 3 行为测试 3 case + 既有 7 case 共 10/10 全绿零回归；Phase 4 `crp.md` §负荷来源双源 + 配置点 + 偏离补注修正。回滚=改 config 或移除 aps-service `ApsLoadSourceProvider` Bean 注册。

Closure Audit Evidence:

- Auditor: independent general subagent（新会话 `ses_0d13d0ad1ffeX3Ngvg6lTEnp9G`，无执行者上下文，冷重播审计）。
- Evidence: 逐项核对 Phase 1/2/3/4 交付物与实时代码——`IErpApsLoadSourceProvider`（mfg-dao，`findScheduledSlots(List<Long>, LocalDate, LocalDate)`）+ `ApsLoadSlot`（@DataBean 6 字段）；`ApsLoadSourceProvider`（aps-service，`implements IErpApsLoadSourceProvider`，`q.addFilter(eq("status", PLANNED))` 过滤已排程 OperationOrder）；aps `app-service.beans.xml` 注册 Bean；mfg `app-service.beans.xml` 经 `ioc:collect-beans by-type` 注入；`CrpLoadCalculator`（`apsLoadSourceProviders` 字段 + `isApsLoadSourceEnabled/distributeByApsSlots/distributeByWorkOrder/indexApsSlotsByWorkOrder`）；`ErpMfgConstants.CONFIG_CRP_LOAD_SOURCE`+2 码值；`TestErpMfgCrpLoadSource` 3 case（testBeansFile 加载 `TestStubApsLoadSourceProvider` 桩，对齐 quality test-mock-sales 模式）；`crp.md` §负荷来源双源+配置点+偏离补注；APS 引擎回填路径 `ErpApsSchedulingEngine:89-91,145-147,81-83` live-verified；`1707-1` 与 `0831-1` Deferred 项均标 `Status: resolved`。Anti-Hollow：`distributeByApsSlots` 真实分钟→小时换算（`Duration.between.toMinutes/60`）+ setup→首日；`ApsLoadSourceProvider` 真实 QueryBean 4 重过滤（workOrderId in / status=PLANNED / plannedEndDateT≥winFrom / plannedStartDateT≤winTo）。Nop 约定达标（包级 @Inject List setter 注入、@BizMutation 不变、NopException 不变、跨域 SPI 反转对齐 fin 范式、aps 实现用 IDaoProvider 对齐 aps ATP/CTP 同域只读范式）。验证 live 复跑：`Tests run: 10, Failures: 0, Errors: 0`（既有 7 + 新增 3）；全仓 `mvn clean install -DskipTests` BUILD SUCCESS（146 模块，2026-07-05 04:13）。文档对齐（`docs/logs/2026/07-05.md` 顶部本计划条目 + `crp.md` 三处修正 + 双源计划 Deferred 标 resolved）。所有 Non-Goal 在计划 Deferred + crp.md 显式记录。文本一致性成立。
- Verdict: **PASS**（独立审计门控已置 `[x]`；审计提出的 Status Note + Audit Evidence 占位符 BLOCKER 已由执行者据审计回执回填）。

Follow-up:

- maintenance 停机扣减（见上方 Deferred）
- CRP 可视化/班次级（见上方 Deferred）
- JobCard 自动创建（见上方 Deferred）
