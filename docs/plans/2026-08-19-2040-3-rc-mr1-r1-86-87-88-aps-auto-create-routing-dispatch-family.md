# 2026-08-19-2040-3-rc-mr1-r1-86-87-88-aps-auto-create-routing-dispatch-family aps 域越界修复族（工单工序自动创建 + 替代路由选择 + 自动派工引擎）

> Plan Status: active
> Mission: requirement-compliance
> Work Item: RC-R1.86 + RC-R1.87 + RC-R1.88（MR1 越界项，aps 域收尾三件套；含 mnt 计划 R1.76 successor「矩阵修订归 RC-R1.86-88 协同」注记的收口）
> Last Reviewed: 2026-08-19
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

Status: planned
Targets: `module-aps/erp-aps-service/`（创建编排）+ `module-manufacturing/erp-mfg-service/`（D1 接线点，若裁决为 mfg 侧触发）+ matrix 登记
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: D1 裁决先行

- [ ] Decision: D1 跨域触发模型裁决——选项 A：mfg WorkOrder RELEASED 后置调 aps Facade（`createOperationOrdersFromWorkOrder(workOrderId)`，推送直连先例 = R1.59/R1.77/R1.85 直连族；事务传播与失败隔离语义须显式：联动失败不阻断 WorkOrder 下达主流程，对齐 R1.59 降级范式；伴随 mfg→aps Java 层 pom 边 + matrix 登记）；选项 B：aps 侧 job 拉取扫描 RELEASED 工单（R1.76 拉取消费先例，零新边）。裁决依据 matrix 允许边方向 + 幂等可控性，结果 + Java 层边登记写入 owner doc。
      - Skill: none
- [ ] Add: 批量创建编排——读 WorkOrder 绑定工艺路线工序列表（经 mfg IBiz/允许的只读通道）→ 按 sequence 依次建 OperationOrder(DRAFT) 继承 workOrderId/operationName/machineId/setupTime/runtimePerUnit/qty + 计算 totalDuration；工艺路线缺失→跳过 + LOG.warn 必做 + notify 告警派发（事件 `aps.workorder-no-routing`，无 ACTIVE 模板静默跳过，R1.4 范式）；工作中心不存在→该工序拒绝创建并告警（同通道，L1 异常路径双分支）；幂等守卫（同 WorkOrder 重复触发不重复建单）；同一编排 Facade 暴露手动触发 @BizMutation 入口（覆盖 L1 触发条件「或计划员手动触发」，守卫/幂等与自动路径同源）。
      - Skill: `nop-backend-dev`
- [ ] Proof: `TestErpApsWorkOrderToOperationOrder` 至少 6 组——批量创建+字段继承+totalDuration、sequence 排序、工艺路线缺失跳过+告警、工作中心不存在拒绝、幂等重触零重复、（若 D1 选 A）联动失败隔离 WorkOrder 下达不受阻。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] WorkOrder 下达→工序工单 DRAFT 批量创建链路运行时可观察（成功/跳过/拒绝三模式）
- [ ] CRP 负荷盲缺口闭合佐证：A4.2.178 场景（有 OperationOrder 的 WorkOrder 出现在 CRP 负荷来源）一组断言

### Phase 2 — P1-RC-089 替代工艺路线选择

Status: planned
Targets: `module-aps/model/app-erp-aps.orm.xml`（selectedRoutingId + 越界回落增量列）+ `module-aps/erp-aps-service/`（SchedulingEngine 路由选择 + manualOverride mutation）+ dict + §ORM Approvals
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: 双独立子 agent 批准（selectedRoutingId 列 A 类 + routingSelectionReason/manualOverride 等增量列 R1.73 式越界回落批准，合并一次复核）；Phase 1 无硬依赖（排产引擎消费的 OperationOrder 既有存量即可开发）

- [ ] Add: ORM `ErpApsOperationOrder.selectedRoutingId` 列（A 类授权）+ routingSelectionReason（dict 新值 DEFAULT/PRIMARY_OVERBOOKED/PRIMARY_DOWN/BATCH_CONSTRAINT，数据变更）+ manualOverride 标记列（越界回落批准载体）；operation-order-status dict 加 UNSCHEDULABLE 值（数据变更）。
      - Skill: `nop-backend-dev`
- [ ] Add: 引擎 SELECT_ROUTING 集成——scheduling.md 步骤 3「获取工作中心」替换为路由选择：priority ASC 查启用路由（生效期+批量约束过滤）→逐个尝试产能窗口（duration 计入 setupTimeDelta/runtimePerUnitDelta）→选中回写 machineId/setupTime/runtimePerUnit/selectedRoutingId/routingSelectionReason→全不可用标 UNSCHEDULABLE + 记无路由原因；无路由配置的工序保持既有 op.machineId 行为零变化（向后兼容）。
      - Skill: `nop-backend-dev`
- [ ] Add: `manualOverrideRouting` mutation——计划员强制指定路由（覆盖自动选择 + manualOverride=true + DispatchLog/备注审计）；manualOverride 工序在重排时跳过自动路由选择。
      - Skill: `nop-backend-dev`
- [ ] Decision: D3 allowFallback 载体裁决——替代路由降级开关按 owner doc §4.1（不允许降级→UNSCHEDULABLE 告警）落在 OperationOrder 列（越界回落）或 DispatchRule/全局 config；结果记入 owner doc。
      - Skill: none
- [ ] Proof: `TestErpApsAlternativeRouting` 至少 7 组——主选可用选主选零行为变化、主选过载自动备选（时间差计入断言）、批量约束过滤、全不可用 UNSCHEDULABLE+原因、manualOverride 覆盖+重排保持、无路由配置回归、降级开关关闭保持 UNSCHEDULABLE 告警。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 路由选择/降级/UNSCHEDULABLE/manualOverride 运行时可观察（A4.2.179 场景翻转：主选过载备选承接）
- [ ] 既有排产引擎测试零回归（TestErpApsSchedulingEngine/CapacityReservation/ScheduleManagement 全绿）

### Phase 3 — P1-RC-090 自动派工引擎

Status: planned
Targets: `module-aps/erp-aps-service/`（派工引擎 + job + mutation 族）+ dict + job.yaml + matrix 登记
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（PLANNED 工序来源）与 Phase 2（selectedRouting 确定工作中心）完成后联调；D5/D6 裁决先行

- [ ] Decision: D5 物料齐套跨域模型裁决——选项 A：经 inventory 可用量 IBiz/IDaoProvider 直查（aps-service 已有 inv-dao/mfg-dao compile 依赖 + IDaoProvider 只读先例可循）；选项 B：复用 mfg 齐套判定内部逻辑（注意 `IErpMfgMaterialAvailabilityService` 系 owner doc 设计名、实仓不存在，`KitAvailabilityChecker` 为 mfg-service 内部类非 dao 接口，选项 B 须先在 mfg 侧暴露 Facade）。两选项均受 matrix §9.4「mfg/inv 为 I*Biz 强注入永久豁免目标域（aps 单模块测试 NoSuchBeanFailure 先例）」约束——接线按 §2.4 dao 边 + @Nullable 容错范式；结果 + matrix Java 层边登记。
      - Skill: none
- [ ] Decision: D6 JobCard 联动裁决——选项 A：复用制造域既有 `erp-mfg-jobcard-auto-generate.job.yaml` + `ErpMfgWorkOrder__generateJobCardsFromSchedule` seam（按 PLANNED 排程自动建卡已运行，须 reconcile 去重/幂等边界——派工触发的建卡与既有 job 建卡不得双卡）；选项 B：本域派工后置直接触发建卡（跨域写，倾向否决）。结果 + matrix 登记。
      - Skill: none
- [ ] Add: 派工引擎——nop-job 周期扫描（enabled 默认 false + cron 空值跳过 + 全局开关；R1.38 简单 job bean 范式）：按工作中心加载 DispatchRule（enableAuto/holdUntil/enabledHours/maxLookahead/dispatchAhead/maxConcurrentOps/priorityThreshold）→查 eligible PLANNED 工序（plannedStartDateT 窗口内 + status != HOLD——保持态由 status dict 值承载，零新列）→按 (plannedStartDateT ASC, priority ASC) 逐个检查物料齐套（D5 通道）/操作工在岗（排班只读，无排班数据视为满足并记条件结果 null）/工装维度（requireTooling=true 且无工装载体→条件结果记 null 放行 + LOG.warn，Deferred 节登记的确定口径）→全满足 status=IN_PROGRESS + 记 DispatchLog（previousStatus/newStatus/dispatchType=AUTO/三维条件结果 JSON/dispatchedBy=系统）→不满足跳过继续；缺料且工序已派工窗口内→status=ON_HOLD + notify 通知计划员（aps→notify Java 边登记；模板无 ACTIVE 静默跳过）。
      - Skill: `nop-backend-dev`
- [ ] Add: 手动 mutation 族——`dispatchManually`（PLANNED→IN_PROGRESS，DispatchLog dispatchType=MANUAL + note 承载跳检原因，可跳过条件检查但原因必填）/ `hold`/`unhold`（status PLANNED↔HOLD 迁移 + DispatchLog dispatchType=HOLD/UNHOLD，对齐 auto-dispatch.md §3.3 保持语义）；operation-order-status dict 加 HOLD/ON_HOLD 值（数据变更，B 类裁决明示）。
      - Skill: `nop-backend-dev`
- [ ] Proof: `TestErpApsAutoDispatch` 至少 10 组——规则跳过（enableAuto=false/holdUntil 未到/enabledHours 窗口外）、eligible 过滤（窗口/优先级阈值/maxConcurrentOps/HOLD 态排除）、三条件组合满足派工+DispatchLog 完整、缺料跳过+ON_HOLD 通知、操作工无排班降级满足（条件结果 null）、工装开关开+无载体→null 放行+LOG.warn、手动强制派工（跳过检查+原因必填）、hold/unhold（status 迁移+DispatchLog）、并发派工冲突乐观锁、job cron 空值跳过 + 幂等（重复扫描不重复派工）。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 自动派工链路运行时可观察（派工/跳过/保持/强制四模式 + ON_HOLD 通知）
- [ ] TestErpAllJobYamlLoading 计数同步（新增 job.yaml 后）

## ORM Approvals（双独立子 agent 批准记录 — 执行期填充）

> A 类授权 + 越界回落 dual-agent-approval：两个 fresh session 子 agent 分别独立复核 selectedRoutingId 列（A 类）+ routingSelectionReason/manualOverride/allowFallback 载体列（R1.73 式越界回落；全部可空无默认无索引无 UK、零既有语义改动），各自 APPROVE 后方可执行 ORM 编辑。

- [ ] Approver 1（session id + 结论 + 日期）：______
- [ ] Approver 2（session id + 结论 + 日期）：______

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_fe5f10e71ffeFfQiiARJi4wktw) because ① Phase 3 派工字段载体（dispatchHold/dispatchType 标记）与 B 类「零 ORM 结构变更」授权矛盾且 ORM Approvals 未覆盖 ② 反松弛违例——范围内条目含「notify 可选」③ 基线测试类计数误报（7 → 实仓 10）；另 6 条非阻塞注记（R1.76 系拉取非推送先例、D5 引用不存在的 IErpMfgMaterialAvailabilityService + matrix §9.4 约束、notify 边登记、D6 与既有 jobcard job reconcile、工装用例、:60→:63）。
- Independent draft review iteration 2: acceptable as-is (ses_fe5e89775ffejLm63HbddxAWcg) after 修订复核——三项 round-1 blocker 逐一实仓验证闭合（派工载体 = status dict 值 + 既有 DispatchLog 实体[11 字段逐列核验]三处一致化且 ORM Approvals 限定 Phase 2 路由列；notify 改确定行为；测试类计数 10 = 7+3 核验），6 条非阻塞注记全吸收；round-2 仅余小注（Goals IN_PROGRESS 笔误、L1「或计划员手动触发」显式处置、D1 条件分支记录），已在本轮补齐（笔误更正 + Phase 1 增手动触发 @BizMutation 入口同源守卫）。共识达成，草案可执行。

## Closure Gates

> 完整仓库验证在此处运行一次。

- [ ] 范围内行为完成（P1-RC-088/089/090 全部验收点落地）
- [ ] 相关文档对齐（scheduling.md Non-Goal 行 supersede 标注 + alternative-routing.md/auto-dispatch.md/state-machine.md 实现注记 + data-dependency-matrix D1/D5/D6 边 + aps→notify 边登记 + mnt R1.76「矩阵修订归 RC-R1.86-88 协同」successor 收口注记 + arm-index P1-RC-088/089/090 → done (RC-R1.86/87/88) + roadmap 行状态同步 + docs/logs/ 当日条目）
- [ ] 已运行验证：erp-aps-service 分域 `mvn test`（+ mfg 侧接线分域）+ 全仓 `mvn clean install -DskipTests` + 全仓 `mvn test` + `bash docs/audits/nop-compliance-checker.sh`（若 actual > baseline 则 baseline-raise 登记 per-site 证据）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

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

Status Note: pending

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- 无（已确认缺陷不入此节）
