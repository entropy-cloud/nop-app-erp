# 2026-08-19-2040-3-rc-mr1-r1-86-87-88-aps-auto-create-routing-dispatch-family aps 域越界修复族（工单工序自动创建 + 替代路由选择 + 自动派工引擎）

> Plan Status: completed
> Mission: requirement-compliance
> Work Item: RC-R1.86 + RC-R1.87 + RC-R1.88（MR1 越界项，aps 域收尾三件套；含 mnt 计划 R1.76 successor「矩阵修订归 RC-R1.86-88 协同」注记的收口）
> Last Reviewed: 2026-08-20
> Source: `docs/backlog/requirement-compliance-roadmap.md` MR1 行 RC-R1.86/87/88 + arm-index P1-RC-088/P1-RC-089/P1-RC-090
> Related: A1.50 审计 `2026-08-06-2243-2`（aps-full）+ A4.2 报告 `docs/audits/2026-08-07-1410-rc-ma4-a4-2-178-181-aps-runtime.md`；mnt 联动先例 plan `2026-08-19-0445-3-rc-mr1-r1-76-77-mnt-cross-domain-linkage.md`（D3 拉取消费模型 + matrix §2.4 登记）
> Audit: required

## Authorization Ledger

- RC-R1.86：**B 类**（2026-08-12 批量裁决「事件订阅 + 批量创建 DRAFT」纯代码逻辑/跨域契约预授权零 ORM；跨域 mfg 协调义务经 D1 决策项 + matrix Java 层边登记履行）
- RC-R1.87：**A 类 + 越界回落**（2026-08-12 A 类「aps: RC-R1.87（ErpApsOperationOrder 加 selectedRoutingId 列）」；UNSCHEDULABLE dict 值 = 数据变更（对齐 R1.88 B 类裁决「dict 加值是数据非结构变更」）；增量列 routingSelectionReason/manualOverride 等 R1.73 式越界回落双独立子 agent 批准，记录落盘本计划）
- RC-R1.88：**B 类**（2026-08-12 批量裁决「dict 加 HOLD/ON_HOLD 是数据非结构变更」+ 调度接线 + BizModel 逻辑预授权——**零 ORM 结构变更**：派工状态载体 = operation-order-status dict 值（HOLD/ON_HOLD/IN_PROGRESS），派工/保持审计载体 = 既有 `ErpApsDispatchLog` 实体（dispatchType/dispatchedBy/dispatchedAt/conditionCheckResult/三维布尔/previousStatus/newStatus 字段全部就绪），规则载体 = 既有 `ErpApsDispatchRule` 实体（全字段就绪）；跨域 inventory 物料齐套 + mfg JobCard 协调经 D5/D6 决策项 + matrix 登记）

## Current Baseline

- **P1-RC-088（UC-APS-01 工序工单自动创建缺失）**：`ErpApsOperationOrderBizModel#defaultPrepareSave:63` 仅自动 businessDate；无 mfg→aps WorkOrder 下达事件订阅、无工艺路线读取、无批量 DRAFT 创建、无 workcenter 存在性校验。A4.2.178 确认：无 OperationOrder 的 WorkOrder 在 CRP 负荷盲（`CrpLoadCalculator:107/115` apsSlotsByWo.get 无 slot=null）。L1 `use-cases.md:10,12` 要求 manufacturing 发布下达事件→APS 读工艺路线工序列表→按 sequence 批量建 DRAFT→继承字段+totalDuration→工艺路线缺失跳过告警/工作中心不存在拒绝。
- **P1-RC-089（UC-APS-06 替代路由选择缺失）**：`ErpApsSchedulingEngine` 零引用 `ErpApsOpRouting`（恒用 op.machineId 主工作中心）；`ErpApsOpRoutingBizModel` 17 行空壳；`ErpApsOperationOrder` ORM 无 selectedRoutingId 列；operation-order-status dict 无 UNSCHEDULABLE 值。A4.2.179 确认：主工作中心过载/备选闲置并存，`findFreeSlotForward:81` null → 留 DRAFT + NO_AVAILABLE_SLOT 冲突，无备选尝试。
- **P1-RC-090（UC-APS-07 自动派工引擎缺失）**：module-aps 零 job/scheduler/batch 文件 + app-erp-all job conf 零 aps；`ErpApsDispatchRuleBizModel`/`ErpApsDispatchLogBizModel` 各 17 行空壳（但 **`ErpApsDispatchRule`/`ErpApsDispatchLog` ORM 实体字段全就绪**——Rule 含 enableAuto/requireMaterial/requireOperator/requireTooling/maxLookaheadMinutes/dispatchAheadMinutes/maxConcurrentOps/priorityThreshold/enabledHours/holdUntil/holdReason，Log 含 dispatchType/previousStatus/newStatus/conditionCheckResult/dispatchedBy/dispatchedAt/三维布尔）；无物料齐套/操作工/工装检查；dict 无 HOLD/ON_HOLD；无 JobCard 创建联动。scheduling.md:9 Non-Goal「自动派工…执行 / nop-job 定时自动重排」系 AI 自标，arm-index §4 三判据裁决不成立 → Q4=(a) 强制实现（本计划 supersede 该 Non-Goal 行）。
- aps 域已落地能力（不得回归）：前向/后向/优先级排产引擎 + 有限产能 + 插单重排 + ATP/CTP 影子模拟 + capacity reservation，erp-aps-service 当前 10 测试类全绿基线（顶层 7 + statemachine/ 3）；scheduling.md 实现约定之「JobCard 按 OperationOrder 排程自动创建」已有制造域 `ErpMfgWorkOrder__generateJobCardsFromSchedule` 经 `IErpApsLoadSourceProvider` SPI 的消费先例 + `erp-mfg-jobcard-auto-generate.job.yaml` 已按 PLANNED 排程自动建卡（本计划 D6 须与之 reconcile 幂等/去重）。
- `ErpApsOperationOrder` 既有列已含 workOrderId/operationName/machineId/setupTime/runtimePerUnit/qty/totalDuration/priority/sequence/plannedStart/EndDateT/status 等（继承字段无 ORM 缺口）；缺 selectedRoutingId 及路由族增量列（见 Phase 2）——**派工族零列缺**（状态/审计载体见 Authorization Ledger）。

## Goals

- P1-RC-088：WorkOrder 下达→OperationOrder 自动创建链路——按 D1 裁决的跨域模型（事件发布或拉取，对齐 R1.76 拉取先例）读工艺路线工序列表→按 sequence 批量建 DRAFT→继承字段+totalDuration 计算→工艺路线缺失跳过+告警→工作中心不存在拒绝。
- P1-RC-089：替代路由选择——`ErpApsSchedulingEngine` 集成 SELECT_ROUTING（priority ASC 查启用路由 + 批量约束 + 主选产能不足自动备选 + setupTimeDelta/runtimePerUnitDelta 计入 totalDuration + selectedRoutingId/routingSelectionReason 回写）+ UNSCHEDULABLE dict 值 + manualOverride 人工强制 mutation。
- P1-RC-090：自动派工引擎——nop-job 周期扫描 PLANNED 工序（前瞻窗口/优先级阈值/maxConcurrentOps）+ 物料齐套检查（跨域 inventory 按 D5 裁决）+ 操作工/工装可用检查 + 全满足 status=IN_PROGRESS 记 DispatchLog + 手动强制派工/HOLD/UNHOLD mutation（dict 加 HOLD/ON_HOLD 值）+ 派工后缺料 ON_HOLD 通知 + JobCard 联动（按 D6 裁决复用既有 seam）。
- scheduling.md Non-Goal 行 supersede 更新 + owner doc 实现注记 + arm-index 三行 → done + mnt R1.76「矩阵修订归 RC-R1.86-88 协同」successor 注记收口。

## Non-Goals

- ILP/CP 优化求解与 PERSONNEL/TOOL 多约束并联排产（scheduling.md 既有 Non-Goal，维持）
- 工作中心班次日历展开（scheduling.md 既有 Non-Goal，维持）
- 甘特图前端可视化与 dragUpdateOperation 拖拽（归前端计划）
- P2-RC-079/080（后向不可达通知 sales / sales 审核触发 ATP/CTP 接线，watch-only successor，跨域 sales 方向不在本计划）
- 操作工技能等级匹配与排班深查（auto-dispatch.md「简单场景：至少 1 名在岗操作工」为本计划口径；复杂技能匹配 successor）
- 工装夹具（ErpApsDispatchRule.requireTooling）的深度可用性模型——本计划落地确定最简口径：requireTooling=true 且仓库无工装载体时该维度条件结果记 null 放行 + LOG.warn（完整工装管理 successor，见 Deferred But Adjudicated）
- APS→CRP 负荷来源 re-wiring / CRP 停机扣减（mnt R1.76 已登记 watch-only，非 L1 UC-APS 强制项）

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/aps/use-cases.md`（L1 真相源，不改）+ `docs/design/aps/scheduling.md`（Non-Goal supersede）+ `docs/design/aps/alternative-routing.md` + `docs/design/aps/auto-dispatch.md` + `docs/design/aps/state-machine.md` + `docs/architecture/data-dependency-matrix.md`
- Skill Selection Basis: `nop-backend-dev`（状态机 mutation/Processor/nop-job 接线/跨域 IBiz 规范 + 反模式自检；job 接线遵循 R1.23/R1.27 已固化的 batch-task/简单 job 双范式择一）+ `nop-testing`；路由选择与派工条件算法无匹配技能（Skill: none，以 owner doc 伪码为准 + 边界单测）。

## Infrastructure And Config Prereqs

- 无新端口/外部服务。新增 job：自动派工扫描 job（enabled 默认 false + cronExpr `@cfg:` 门控，R1.4/R1.38 范式）；config：`erp-aps.auto-dispatch-cron`（默认空=跳过）+ 全局开关 `erp-aps.auto-dispatch-enabled`（默认 false，对齐 auto-dispatch.md §5.2 全局开关）。
- ORM 变更走 `mvn clean install -DskipTests` 增量重生成链；dict 值追加（UNSCHEDULABLE/HOLD/ON_HOLD）为数据变更走 dict 定义文件。

## Execution Plan

### Phase 1 — P1-RC-088 WorkOrder 下达→OperationOrder 自动创建

Status: completed
Targets: `module-aps/erp-aps-service/`（创建编排）+ `module-manufacturing/erp-mfg-service/`（D1 接线点，若裁决为 mfg 侧触发）+ matrix 登记
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: D1 裁决先行

- [x] Decision: D1 跨域触发模型裁决——**选项 B：aps 侧 job 拉取扫描**（R1.76 拉取消费先例，零新边）。裁决依据：matrix 边方向（aps-service 已有 mfg-dao compile 依赖 [ATP/CTP 先例]，选项 A 须新增 mfg-service→aps-dao 边且联动失败隔离侵入 mfg 审核主流程）+ 幂等可控性（拉取 + 幂等守卫 [同 WorkOrder 已有任一 op 即跳过整单] 天然可重试）。结果 + Java 层边登记已写入 owner doc（`scheduling.md` 实现约定块 + matrix §2.4 aps-service→notify-dao 新边 + mfg-dao 只读消费方注记）。实现：`erp-aps-workorder-scan.job.yaml`（enabled 默认 false + `erp-aps.workorder-scan-cron` 空=跳过）+ `ErpApsWorkOrderScanJob`（R1.38 简单 job bean）→ `scanReleasedWorkOrders` mutation。
      - Skill: none
- [x] Add: 批量创建编排——`ErpApsWorkOrderToOperationProcessor`（protected step 结构）：读 WorkOrder 绑定工艺路线工序列表（`IDaoProvider` 只读 mfg，matrix §9.4 豁免）→ 按 lineNo（=sequence）依次建 OperationOrder(DRAFT) 继承 workOrderId/operationName/machineId/setupTime/runtimePerUnit=runTime/qty + totalDuration（与引擎同公式单一真相源）；工艺路线缺失→整单跳过 + LOG.warn + notify `aps.workorder-no-routing`（无 ACTIVE 模板静默跳过，R1.4 范式）；工作中心不存在→该工序拒绝创建 + notify `aps.operation-workcenter-missing` 告警（L1 异常路径双分支）；幂等守卫（同 WorkOrder 重复触发不重复建单）；同一编排 Facade 暴露手动触发 `createOperationOrdersFromWorkOrder` @BizMutation 入口（守卫/幂等与自动路径同源）。
      - Skill: `nop-backend-dev`
- [x] Proof: `TestErpApsWorkOrderToOperationOrder` 7 组全绿——批量创建+字段继承+totalDuration 公式、sequence=lineNo 排序、工艺路线缺失跳过+告警落 ErpSysNotification、工作中心不存在拒绝+告警（其余照建）、幂等重触零重复、扫描仅已下达工单+跨轮幂等（SUBMITTED 排除）、CRP 负荷盲闭合（排程后 slots 进入 findScheduledSources）。（D1 选 B，联动失败隔离测试项不适用——job 内 try/catch + 逐单独立处理）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] WorkOrder 下达→工序工单 DRAFT 批量创建链路运行时可观察（成功/跳过/拒绝三模式：testBatchCreate…/testMissingRouting…/testWorkcenterMissing…）
- [x] CRP 负荷盲缺口闭合佐证：A4.2.178 场景（有 OperationOrder 的 WorkOrder 出现在 CRP 负荷来源）一组断言（testScheduledOpsAppearInCrpLoadSource）

### Phase 2 — P1-RC-089 替代工艺路线选择

Status: completed
Targets: `module-aps/model/app-erp-aps.orm.xml`（selectedRoutingId + 越界回落增量列）+ `module-aps/erp-aps-service/`（SchedulingEngine 路由选择 + manualOverride mutation）+ dict + §ORM Approvals
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: 双独立子 agent 批准（selectedRoutingId 列 A 类 + routingSelectionReason/manualOverride 等增量列 R1.73 式越界回落批准，合并一次复核）；Phase 1 无硬依赖（排产引擎消费的 OperationOrder 既有存量即可开发）

- [x] Add: ORM `ErpApsOperationOrder.selectedRoutingId` 列（propId 30，A 类授权）+ routingSelectionReason（propId 31，dict `erp-aps/routing-selection-reason` 新值 DEFAULT/PRIMARY_OVERBOOKED/PRIMARY_DOWN/BATCH_CONSTRAINT，数据变更）+ manualOverride（propId 32）+ allowFallback（propId 33，D3 裁决载体）；operation-order-status dict 加 UNSCHEDULABLE/HOLD/ON_HOLD 值（数据变更）。全部可空无默认无索引无 UK 零既有语义改动；ORM Approvals 双批准落盘（appr1-7f3a2c + appr2-7f3a91）；`mvn clean install -DskipTests` 增量重生成链通过。
      - Skill: `nop-backend-dev`
- [x] Add: 引擎 SELECT_ROUTING 集成——`ErpApsSchedulingEngine` 前向/后向均替换为候选路由集尝试：默认行（isDefault && machineId=op.machineId）定位 operationId → priority ASC 逐候选（生效期 + 批量约束过滤）尝试产能窗口（duration 计入 setupTimeDelta/runtimePerUnitDelta，差值幂等剥离-叠加）→选中回写 machineId/setupTime/runtimePerUnit/selectedRoutingId/routingSelectionReason→全不可用标 UNSCHEDULABLE + 冲突 NO_AVAILABLE_ROUTING；无路由配置/manualOverride 工序保持 op.machineId 单候选零行为变化；UNSCHEDULABLE 与 DRAFT 同池重排自愈；InsertRushOrder 插单路径同步启用（§5.2 被抢占工序尝试备选）。
      - Skill: `nop-backend-dev`
- [x] Add: `manualOverrideRouting` mutation（`ErpApsRoutingManualOverrideProcessor` per-mutation Processor）——计划员强制指定路由（覆盖自动选择 + manualOverride=true + remark 审计记录）；时间差幂等叠加；PLANNED 源回退 DRAFT + 释放产能预留；manualOverride 工序重排跳过自动路由选择；未启用/不存在路由拒绝（ERR_APS_ROUTING_NOT_AVAILABLE）；IN_PROGRESS 等非法源态拒绝。
      - Skill: `nop-backend-dev`
- [x] Decision: D3 allowFallback 载体裁决——**落在 OperationOrder.allowFallback 列**（越界回落批准覆盖；可空 Boolean null=允许降级，对齐 owner doc §4.1「检查 operationOrder 是否允许降级（allowFallback 字段）」字面指定）。结果记入 owner doc（alternative-routing.md 实现注记 D3 裁决行）。
      - Skill: none
- [x] Proof: `TestErpApsAlternativeRouting` 8 组全绿——主选可用选主选零行为变化（DEFAULT+时长 25）、主选过载自动备选（PRIMARY_OVERBOOKED + 时间差计入 totalDuration=40/setup=10/perUnit=3/排程时长 40 断言）、批量约束过滤（BATCH_CONSTRAINT）、全不可用 UNSCHEDULABLE+原因+解除后自愈重排（PRIMARY_DOWN）、manualOverride 覆盖+重排保持（含 remark 审计）、无路由配置回归（legacy 排到 earliestStart）、降级开关关闭保持 UNSCHEDULABLE 不尝试备选、强制指定未启用/不存在路由双拒绝。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 路由选择/降级/UNSCHEDULABLE/manualOverride 运行时可观察（A4.2.179 场景翻转：主选过载备选承接——testPrimaryOverloadedFallsBackWithTimeDeltas）
- [x] 既有排产引擎测试零回归（全模块 66/66 全绿：TestErpApsSchedulingEngine 6 + TestErpApsCapacityReservation + TestErpApsScheduleManagement 等）

### Phase 3 — P1-RC-090 自动派工引擎

Status: completed
Targets: `module-aps/erp-aps-service/`（派工引擎 + job + mutation 族）+ dict + job.yaml + matrix 登记
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（PLANNED 工序来源）与 Phase 2（selectedRouting 确定工作中心）完成后联调；D5/D6 裁决先行

- [x] Decision: D5 物料齐套跨域模型裁决——**选项 A：inventory 可用量经 IDaoProvider 直查**（aps-service 既有 inv-dao compile 依赖 + ErpApsAtpCtpServiceImpl 只读先例；齐套口径 = 工单 BOM 单层展开 × plannedQuantity 对照 ΣavailableQuantity，无 BOM→null 放行）。否决选项 B（KitAvailabilityChecker 系 mfg-service 内部类，暴露 Facade 须 service 级依赖破坏 aps 单模块测试）。受 matrix §9.4「mfg/inv 永久豁免」约束按 dao 边 + 注释落地；结果 + matrix Java 层边登记（§2.4 aps-service 行：inv-dao 只读消费方）。
      - Skill: none
- [x] Decision: D6 JobCard 联动裁决——**选项 A：复用制造域既有 `erp-mfg-jobcard-auto-generate.job.yaml` + `generateJobCardsFromSchedule` seam**。reconcile 去重/幂等边界：`ApsLoadSourceProvider`（CRP 负荷 SPI 同源）扩展导出 IN_PROGRESS（已派工）工序——派工先于日批建卡的工序下轮建卡自然补齐，同 seam 幂等增量零双卡；选项 B（本域派工后置直接建卡跨域写）否决。结果记入 owner doc（auto-dispatch.md 实现注记 D6 行）+ matrix 登记。
      - Skill: none
- [x] Add: 派工引擎——`ErpApsAutoDispatchProcessor`（protected step 结构）+ `erp-aps-auto-dispatch.job.yaml`（enabled 默认 false）+ `ErpApsAutoDispatchJob`（R1.38 简单 job bean：全局开关 `erp-aps.auto-dispatch-enabled` 默认 false + cron `erp-aps.auto-dispatch-cron` 空值跳过 + runInSession 包裹）：按工作中心加载 DispatchRule（enableAuto/holdUntil/enabledHours/maxLookahead/dispatchAhead/maxConcurrentOps 缺省回落工作中心 capacity/priorityThreshold）→查 eligible PLANNED 工序（窗口内 + status dict 值承载保持态零新列）→(plannedStartDateT ASC, priority ASC) 逐个检查物料齐套（D5 通道）/操作工（无排班载体 null 放行）/工装（requireTooling 且无载体→null 放行 + LOG.warn）→全满足 status=IN_PROGRESS + DispatchLog（previousStatus/newStatus/dispatchType=AUTO/三维条件 JSON/dispatchedBy=system）→不满足跳过继续；缺料且窗口内→status=ON_HOLD + notify `aps.dispatch-material-shortage`（aps→notify Java 边 matrix §2.4 登记；模板无 ACTIVE 静默跳过）。
      - Skill: `nop-backend-dev`
- [x] Add: 手动 mutation 族——`dispatchManually`（PLANNED→IN_PROGRESS，DispatchLog dispatchType=MANUAL + note 承载跳检原因，可跳过条件检查但原因必填 ERR_APS_DISPATCH_REASON_REQUIRED）/ `hold`/`unhold`（status PLANNED↔HOLD 迁移 + DispatchLog dispatchType=HOLD/UNHOLD，ON_HOLD 经 unhold 解除，状态机 Bean hold/unhold 守卫 + transitions 13 边扩展，对齐 auto-dispatch.md §3.3 保持语义）；operation-order-status dict 加 HOLD/ON_HOLD 值（数据变更，B 类裁决明示）。
      - Skill: `nop-backend-dev`
- [x] Proof: `TestErpApsAutoDispatch` 10 组全绿——规则跳过（enableAuto=false/holdUntil 未到/enabledHours 窗口外三模式）、eligible 过滤（前瞻窗口过早/过期+优先级阈值+maxConcurrentOps 满额+HOLD 态排除）、三条件组合满足派工+DispatchLog 完整（AUTO/前後态/三维布尔 null 语义/JSON/dispatchedBy=system）、缺料跳过+ON_HOLD+通知计划员落 ErpSysNotification、无 BOM null 放行（工单级齐套条件结果 null 断言）、工装开关开+无载体→null 放行（testAllConditionsPass 断言 toolingAvailable null）、手动强制派工（跳检+空原因拒绝+note 断言）、hold/unhold（PLANNED↔HOLD/ON_HOLD→PLANNED 迁移+DispatchLog+非法源态拒绝）、并发派工冲突（二次派工拒绝+重复扫描幂等零重复日志）、job cron 空值跳过+job 全链路派工（runInSession 范式）+全局开关关闭跳过（10 组含）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 自动派工链路运行时可观察（派工/跳过/保持/强制四模式 + ON_HOLD 通知：testAllConditionsPass…/testRuleSkips…/testHoldAndUnhold…/testManualDispatch…/testMaterialShortage…）
- [x] TestErpAllJobYamlLoading 计数同步（30→32：erp-aps-workorder-scan + erp-aps-auto-dispatch，已验证绿）

## ORM Approvals（双独立子 agent 批准记录 — 执行期填充）

> A 类授权 + 越界回落 dual-agent-approval：两个 fresh session 子 agent 分别独立复核 selectedRoutingId 列（A 类）+ routingSelectionReason/manualOverride/allowFallback 载体列（R1.73 式越界回落；全部可空无默认无索引无 UK、零既有语义改动），各自 APPROVE 后方可执行 ORM 编辑。

- [x] Approver 1（session id + 结论 + 日期）：appr1-7f3a2c / APPROVE / 2026-08-20（纯加性核验：4 列在 aps orm.xml 全文零命中、propId 30-33 无冲突、UK/索引零触及、owner doc §2.2/§4.1/§4.3 逐列背书、ErpApsOpRouting.id 弱引用目标存在、dict 加值纯数据变更）
- [x] Approver 2（session id + 结论 + 日期）：appr2-7f3a91 / APPROVE / 2026-08-20（独立复核 + 风险面：授权台账/Phase 2 约束逐字核对、_cases CSV 快照 header-based 子集比较零破坏、deploy SQL 为 _ 前缀生成物重生成追加可空列无约束变更、无 orm_propValue(int) 位置引用；非阻塞注记 D3 载体已由 ORM Approvals 明示 allowFallback 为候选列）

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_fe5f10e71ffeFfQiiARJi4wktw) because ① Phase 3 派工字段载体（dispatchHold/dispatchType 标记）与 B 类「零 ORM 结构变更」授权矛盾且 ORM Approvals 未覆盖 ② 反松弛违例——范围内条目含「notify 可选」③ 基线测试类计数误报（7 → 实仓 10）；另 6 条非阻塞注记（R1.76 系拉取非推送先例、D5 引用不存在的 IErpMfgMaterialAvailabilityService + matrix §9.4 约束、notify 边登记、D6 与既有 jobcard job reconcile、工装用例、:60→:63）。
- Independent draft review iteration 2: acceptable as-is (ses_fe5e89775ffejLm63HbddxAWcg) after 修订复核——三项 round-1 blocker 逐一实仓验证闭合（派工载体 = status dict 值 + 既有 DispatchLog 实体[11 字段逐列核验]三处一致化且 ORM Approvals 限定 Phase 2 路由列；notify 改确定行为；测试类计数 10 = 7+3 核验），6 条非阻塞注记全吸收；round-2 仅余小注（Goals IN_PROGRESS 笔误、L1「或计划员手动触发」显式处置、D1 条件分支记录），已在本轮补齐（笔误更正 + Phase 1 增手动触发 @BizMutation 入口同源守卫）。共识达成，草案可执行。

## Closure Gates

> 完整仓库验证在此处运行一次。

- [x] 范围内行为完成（P1-RC-088/089/090 全部验收点落地）
- [x] 相关文档对齐（scheduling.md Non-Goal 行 supersede 标注 + alternative-routing.md/auto-dispatch.md/state-machine.md 实现注记 + data-dependency-matrix D1/D5/D6 边 + aps→notify 边登记 + mnt R1.76「矩阵修订归 RC-R1.86-88 协同」successor 收口注记 + arm-index P1-RC-088/089/090 → done (RC-R1.86/87/88) + roadmap 行状态同步 + docs/logs/ 当日条目）
- [x] 已运行验证：erp-aps-service 分域 `mvn test`（+ mfg 侧接线分域）+ 全仓 `mvn clean install -DskipTests` + 全仓 `mvn test` + `bash docs/audits/nop-compliance-checker.sh`（若 actual > baseline 则 baseline-raise 登记 per-site 证据）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 操作工技能等级匹配与班次日历深查

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: auto-dispatch.md 明示「简单场景：至少 1 名在岗操作工即可派工；复杂场景（技能等级）未来扩展」；L1 UC-APS-07 验收标准为「操作工可用」不要求技能矩阵。
- Successor Required: no（触发条件：技能矩阵主数据落地后）

### 工装夹具深度可用性模型

- Classification: `watch-only residual`
- Why Not Blocking Closure: requireTooling 为规则开关维度，仓库无工装主数据载体；本计划落地确定最简口径（开关开但无载体→条件结果记 null 放行 + LOG.warn，行为可测试非模糊）；L1 未定义工装数据模型。
- Successor Required: yes（触发条件：工装主数据实体设计落地后）

### P2-RC-079/080 sales 方向跨域接线

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: arm-index 裁决 watch-only successor（P2 登记不强制）；本计划仅闭合 P1 三行。
- Successor Required: no

## Closure

Status Note: 三阶段实现与 Proof 已经独立结束审计 round 1 实仓证实全绿；round 1 列出的五项收尾缺口已全部闭合（见 Closure Execution Evidence）——roadmap :478-480 三行 done ✅ + arm-index :291-293 三行 done + docs/logs/2026/08-20.md 聚合条目 + mnt R1.76 successor ② watch-only 裁决收口注记（equipment-integration.md §4.2，RELEASED 触发条件落盘）+ Closure Gates 全仓验证运行并记录。Closure Gates 8 项全部勾选，Plan Status → completed；独立结束审计 round 2 已于 2026-08-20 复核通过（passes closure audit，见 Closure Audit Evidence round 2 节）。

Closure Execution Evidence（EXECUTE 侧收尾证据，2026-08-20）:

- 全仓 `mvn clean install -DskipTests`：**BUILD SUCCESS**（156 模块，01:40）
- 全仓 `mvn test`：**BUILD SUCCESS 3741 tests / 0 failures / 0 errors / 1 skipped**（surefire XML 权威计数 607 文件；前基线 3716 + 本计划新增 25[TestErpApsWorkOrderToOperationOrder 7 + TestErpApsAlternativeRouting 8 + TestErpApsAutoDispatch 10] = 3741 逐字吻合；唯一 skip = 已知 @Disabled ErpAllWebPagesCollectTest）
- erp-aps-service 分域：**76/0/0 全绿**（surefire XML 13 测试类逐文件核验，含新增三类 7+8+10）
- `TestErpAllJobYamlLoading`：**绿（计数 30→32**：erp-aps-workorder-scan + erp-aps-auto-dispatch）
- `bash docs/audits/nop-compliance-checker.sh`：**EXIT=0，19 规则 actual==baseline 零漂移**（R2c 1483→1497 baseline-raise + per-site 证据已登记 compliance-baseline.md「R2c 基线上调注记（plan 2026-08-19-2040-3）」节 + BASELINE 机器可读块 R2c=1497/R2d=37；R1d=14/R2a=34/R2b=236/R3=5/R6=2/R10=12/R12a=70/R12b=66/R12c=40 持平）
- 文档同步：roadmap RC-R1.86/87/88 三行 done ✅（引用本计划 + 验证证据）+ arm-index P1-RC-088/089/090 三行 done (RC-R1.86/87/88) + owner docs（scheduling.md :9/:11 Non-Goal supersede、alternative-routing.md D3 注记、auto-dispatch.md D5/D6 注记、state-machine.md 新态/新边、data-dependency-matrix §2.4 aps-service→notify-dao + mfg-dao/inv-dao 只读边）+ mnt R1.76 successor ②收口（equipment-integration.md §4.2 实现注记：判归 watch-only successor 不实现、矩阵不修订，RELEASED 触发条件 + 裁决依据 a/b/c 落盘）+ docs/logs/2026/08-20.md 本计划聚合条目

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计 round 1（closure auditor，新会话，2026-08-20，mission 2026-08-17-212541-mission-driver）
- Evidence: 已独立验证落地——`mvn test -pl module-aps/erp-aps-service` **76/0/0 全绿**（TestErpApsWorkOrderToOperationOrder 7 + TestErpApsAlternativeRouting 8 + TestErpApsAutoDispatch 10 与计划 Proof 计数逐字一致；既有引擎/状态机/排程管理零回归）+ `mvn test -pl app-erp-all -Dtest=TestErpAllJobYamlLoading` 绿（30→32 计数断言通过）+ 实仓命中：ORM propId 30-33 四列、operation-order-status dict UNSCHEDULABLE/HOLD/ON_HOLD（+ dispatch-type dict UNHOLD）、`ErpApsSchedulingEngine` 路由选择/UNSCHEDULABLE、`ErpApsWorkOrderToOperationProcessor`/`ErpApsRoutingManualOverrideProcessor`/`ErpApsAutoDispatchProcessor`、`erp-aps-workorder-scan.job.yaml`/`erp-aps-auto-dispatch.job.yaml`、状态机 HOLD/UNHOLD/shortageHold 边、owner docs（scheduling.md Non-Goal supersede :9/:11、alternative-routing.md D3 注记、auto-dispatch.md D5/D6 注记、state-machine.md 新态/新边、data-dependency-matrix §2.4 aps-service→notify-dao 边）
- 审计裁决 round 1: **needs completion（closure gaps，返回 EXECUTE）**——①roadmap `docs/backlog/requirement-compliance-roadmap.md:478-480` RC-R1.86/87/88 三行仍 `todo` 未同步 done；②arm-index `docs/audits/arm-index.md:291-293` P1-RC-088/089/090 三行仍 `todo` 未标 done；③`docs/logs/2026/08-20.md` 无本计划执行/验证聚合条目；④Goals 承诺的 mnt R1.76 successor「矩阵修订归 RC-R1.86-88 协同」收口注记未落盘（`equipment-integration.md:180` successor ②「mnt 自动生成 aps ErpApsConstraint(MAINTENANCE)」仍悬置待协同裁决记录）；⑤Closure Gates 全仓验证（全仓 `mvn clean install -DskipTests` + 全仓 `mvn test` + `bash docs/audits/nop-compliance-checker.sh` 含 baseline-raise 登记）未运行未记录（08-20 既有全仓 VERIFY 条目录得 job 计数 30，早于本计划两 job 落地，不可引用为本计划验证）

- Auditor / Agent: **independent closing audit round 2（new session，2026-08-20，冷上下文独立审计者；非本计划执行者；同时收口 MV 任务级审计 `2026-08-20-1255-rc-mv-task-level-closure-audit.md` F1 的「round 2 从未运行」缺口）**
- Evidence: 五项 round-1 缺口逐项实仓复核 + 标准结束审计抽查（不依赖执行者自录证据）：
  - **① roadmap 三行已闭合**：`requirement-compliance-roadmap.md:478-480` RC-R1.86/87/88 三行均为 `done ✅（2026-08-20 修复落地，plan docs/plans/2026-08-19-2040-3-…）`，逐行引用本计划 + 验证证据（aps 76/0/0 + TestErpAllJobYamlLoading 30→32 + 全仓 install/test + checker R2c 1483→1497）。
  - **② arm-index 三行已闭合**：`arm-index.md:291-293` P1-RC-088/089/090 修复状态列均为 `done (RC-R1.86/87/88)`（引用本计划 Phase 1/2/3 与双独立子 agent 批准 appr1-7f3a2c + appr2-7f3a91），非陈旧 `todo`（lesson 11 状态回填义务履行）。
  - **③ 日志聚合条目已落盘**：`docs/logs/2026/08-20.md:16` 含本计划完整聚合条目（RC-R1.86/87/88，P1-RC-088/089/090，三阶段摘要 + full-green verification：aps 76/0/0 + 全仓 156 模块 install + 3741/0/0/1 surefire XML 权威计数 + checker 零漂移）。
  - **④ mnt R1.76 successor ② 收口注记已落盘**：`equipment-integration.md` §4.2 实现注记（:180）明确「②已于 RC-R1.86-88 协同裁决收口（2026-08-20，plan 2026-08-19-2040-3）：判归 watch-only successor，不实现，矩阵不修订」+ 裁决依据 (a)L1 UC-APS-01/06/07 未要求自动生成约束 (b)aps 侧 MAINTENANCE 约束消费能力已存在仅便捷性缺口 (c)RC-R1.86-88 全跨域采用拉取/只读模型零新增 S 写边 + **RELEASED 触发条件**（产品基线将「停机窗口自动进入 APS 排产约束」列为验收标准时，再修订矩阵 mnt→aps S 写边并实现）。
  - **⑤ Closure Gates 全仓验证已运行并记录**：计划 Closure Execution Evidence（:175-182）记录全仓 install BUILD SUCCESS（156 模块）+ 全仓 mvn test 3741/0/0/1（surefire XML 607 文件，前基线 3716 + 新增 25 = 7+8+10 逐字吻合，唯一 skip = 已知 @Disabled）+ checker EXIT=0 19 规则 actual==baseline + R2c 1483→1497 baseline-raise；`compliance-baseline.md:625-633` 存在「R2c 基线上调注记（plan 2026-08-19-2040-3，RC-R1.86/87/88）」节（+14 per-site 证据，BizModel 层零新增 daoFor 全部位于 Processor 层）。全 reactor 验证非 scoped。**R2c 后续链已文档化**：1497 → 1505（`2026-08-20-0518-1` RC-R1.78，:431）→ 1507（RC-R1.89 等，BASELINE 块 :457/:493 同步注记 :37），系本计划关闭后的后续批次增量，非本计划缺口。
  - **标准抽查（实时仓库）**：ORM `app-erp-aps.orm.xml:104-107` selectedRoutingId(propId 30)/routingSelectionReason(31, dict `erp-aps/routing-selection-reason`)/manualOverride(32)/allowFallback(33) 四列实存全可空；dict `erp-aps/operation-order-status:14-16` UNSCHEDULABLE/HOLD/ON_HOLD + `erp-aps/dispatch-type:42-43` HOLD/UNHOLD + routing-selection-reason 四值（:19-24）；三测试类实存且 @Test 计数 7/8/10 精确一致（module-aps/erp-aps-service/src/test）；两 job yaml 实存（app-erp-all `_vfs/nop/job/conf/` erp-aps-workorder-scan + erp-aps-auto-dispatch；当前目录 33 个 .job.yaml = 本计划 30→32 后 RC-R1.80 再 +1 至 33，TestErpAllJobYamlLoading 断言 33 与 javadoc RC-R1.86/RC-R1.88 登记一致，计数链无漂移）；owner-doc 一致性抽样 0 漂移（scheduling.md :9 Non-Goal supersede 划线标注 + :11 RC-R1.86 D1 实现约定、alternative-routing.md :11 D3 allowFallback 载体裁决与 ORM :107 一致、auto-dispatch.md :9/:10 D5/D6 裁决与 matrix 登记一致、aps state-machine.md :18-20 新三态 + :46-50 新五边与 dict/mutation 族一致、data-dependency-matrix :136 aps-service→notify-dao 单向星型边 + mfg-dao/inv-dao 只读消费方注记实存）。
  - **行为面复核依据**：本审计未重跑 mvn/checker（round-2 范围限定收尾缺口 + 抽查）；行为面由 MV 计划同日新鲜全仓验证覆盖（156 模块 install SUCCESS + 3789/0/0/1 + checker 19/19 == baseline，见 `2026-08-20-1255-rc-mv-task-level-closure-audit.md`），supersede 本计划时点（3741/R2c 1497）计数。
- 审计裁决 round 2: **passes closure audit**——五项 round-1 缺口全部以持久文件证据闭合，标准抽查零漂移，ORM Approvals 双独立子 agent 批准记录在案（appr1-7f3a2c + appr2-7f3a91），Deferred But Adjudicated 三项（操作工技能矩阵 / 工装深模型 / P2-RC-079/080 sales 方向）均有明确分类与触发条件，无范围内项目被静默降级。**剩余风险（记录不阻塞）**：(1) 全局计数已随后续批次增长（tests 3789 / R2c 1507），引用本计划证据时须注意时点；(2) round-2 复核延迟至 MV 任务级审计 F1 发现后才补跑（lesson 8 closure-pending 复发样本，流程教训已由 MV 审计记录）；(3) watch-only successor（工装深模型 / 停机自动约束 RELEASED 条件 / P2-RC-079/080）触发时须回访本计划Deferred 节。

Follow-up:

- 无（已确认缺陷不入此节）
