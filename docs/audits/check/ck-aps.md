# ck-aps — aps（高级排产）实现代码检查报告

> 工作项：C8.1（aps 分册）。执行日期：2026-08-26。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-aps` 手写生产代码——erp-aps-service 23 文件（`ErpApsSchedulingEngine`（贪心前向/后向排产 + 替代路由选择 RC-R1.87）+ `WorkCenterTimeline`、`ErpApsSchedulingProcessor`（共享 facade：数据加载/预留获取释放/requireSchedule 守卫）+ 5 个 per-mutation Processor（ScheduleForward/ScheduleBackward/InsertRushOrder/WorkOrderToOperation/AutoDispatch/RoutingManualOverride）、`ErpApsOperationOrderBizModel`（13 边状态机接线 + start/complete/cancel/hold/unhold/updateSchedule/batchScheduleForward/findGanttData）+ `ErpApsScheduleBizModel`（DRAFT→PUBLISHED→ARCHIVED）、`ErpApsOperationOrderStateMachine`、`ErpApsAtpCtpServiceImpl`、`ApsLoadSourceProvider`、2 个 job（WorkOrderScan RC-R1.86 / AutoDispatch RC-R1.88）、Configs/Constants/Errors）+ 7 个 CRUD BizModel（6 个 stub）+ dao 层（SchedulingResult/CtpResult/BatchOperationResult 等）。跨文件核实：`module-aps/model/app-erp-aps.orm.xml`（7 实体 versionProp/UK_APS_CAPACITY_RESERVATION_SLOT/UK_APS_OPERATION_ORDER_CODE_ORG）、`app-service.beans.xml`（13 bean）、app-erp-all 2 个 job.yaml（cron 键与 bean 键比对）、notify seed（aps.* 事件）、测试 10 文件（TestErpApsCapacityReservation 场景逐条核对）。owner docs：`docs/design/aps/`（README/scheduling/state-machine/auto-dispatch/alternative-routing/use-cases）。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。erp-aps-service 全部 23 文件通读（无抽样）+ orm/beans/job-yaml 接线核对 + 平台源码实证 1 处（`QueryBean.addOrderField(name, desc)` L433——本域 6 处全部正确）+ arm-index aps 相关条目（P1-RC-088/089/090、P2-RC-079/080、P0-MA2-019、P1-MA2-077/078）逐条现状复核。
> 切片边界：`erp-aps-web` 甘特页面契约 drift 不深查（后端 findGanttData 已核）；`erp-aps-api` 骨架与 `_gen/` 不查；constraint-based-planning.md 标注「暂不编码」不查。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-aps-001（D6/D10）loadPendingOrders 按 earliestStartDateT 过滤排除 NULL——主建单路径不写 earliestStartDateT，有展望期的排产方案对自动工序整体漏排且无任何冲突报告

- **控制点 A**：`app/erp/aps/service/processor/ErpApsSchedulingProcessor.java#loadPendingOrders`（L84-92：`q.addFilter(in("status", [DRAFT, UNSCHEDULABLE])); if (schedule.getHorizonStart() != null) { q.addFilter(ge("earliestStartDateT", schedule.getHorizonStart())); } if (schedule.getHorizonEnd() != null) { q.addFilter(le("earliestStartDateT", schedule.getHorizonEnd())); }`——SQL 语义下 `earliest_start_date_t >= ?` 对 **NULL 行恒不成立**，NULL earliest 的工序被静默排除在待排集合外）
- **控制点 B**：`app/erp/aps/service/processor/ErpApsWorkOrderToOperationProcessor.java#buildOperationOrder`（L166-186：setCode/setWorkOrderId/setOperationName/setSequence/setMachineId/setSetupTime/setRuntimePerUnit/setQty/setPriority/setStatus/setOrgId/setBusinessDate/setTotalDuration——**不设置 earliestStartDateT/latestEndDateT**；CTP 影子路径（ErpApsAtpCtpServiceImpl L225）反而显式 `shadow.setEarliestStartDateT(...)`）
- **证据**：UC-APS-01 主链（RC-R1.86 拉取扫描自动建 DRAFT）产出的工序 earliestStartDateT 恒 NULL；`ErpApsSchedule.horizonStart/horizonEnd` 非空时（README「排产方案：排产日期……展望期区间」为方案核心字段），`run()` 的 pending 集合**不含任何自动工序**——排产返回 0 scheduled 0 conflict，静默空转。引擎侧 `effectiveEarliestStart`（Engine L573-585）与 `floor`（L587-596）本为 NULL earliest 设计了三级兜底（earliest→planned→floor），被查询前置过滤整个旁路。owner doc scheduling.md §2.1「从 OperationOrder 的 earliestStartDateT（**或 plannedStartDateT 兜底**）开始」的兜底语义对带展望期方案失效。horizon 为空的方案不受影响（无该过滤）——测试 `TestErpApsCapacityReservation` 等用例的方案即 horizon 显式设置但工序均手工 seed earliestStartDateT（HORIZON_START 常量对齐），未覆盖「自动工序 + 有展望期」组合。
- **问题**：D6 查询边界 + D10 NULL 语义——核心循环（扫描建单 → 排产）对基线部署形态断裂且不可见。
- **建议修复方向**：过滤改 `or(isNull("earliestStartDateT"), ge/le(...))`（或以 `ge(plannedStartDateT, ...)` 兜底对齐引擎逻辑）；或 buildOperationOrder 写入 earliestStartDateT = now（工单下达时刻）。补「自动建单 + horizon 方案 → 断言 scheduled>0」贯通测试。
- **arm-index 裁决**：新增（grep「earliestStartDateT NULL/loadPendingOrders 过滤」零命中；P1-RC-088 resolved RC-R1.86 记录的是建单链路落地维度）。

### P1-CK-aps-002（D7/D6）run() 全量排产不感知既有 PLANNED 工序时段/产能预留——引擎时间轴仅含维护约束，增量排产的 persist 预留 pre-check 必然冲突并整轮回滚

- **控制点 A**：`ErpApsSchedulingProcessor#run`（L60-77：`List<ErpApsOperationOrder> pending = loadPendingOrders(schedule); ... engine.scheduleForward(pending, maintenance, **null**, routings, horizonStart)`——**frozenPlanned=null**：时间轴仅由 `buildTimelines(maintenanceConstraints)` 构建（Engine L462-474，只加 MAINTENANCE busy 区间），既有 PLANNED/IN_PROGRESS 工序的已占时段与 `erp_aps_capacity_reservation` 既有行**均不进入时间轴**）
- **控制点 B**：`ErpApsSchedulingProcessor#persist` → `#acquireReservation`（L174-197：`if (hasOverlappingReservation(machineId, start, end)) throw new NopException(ERR_APS_CAPACITY_CONFLICT)`——pre-check 查询的是**全部既有预留行**，不区分来源）
- **证据**：增量场景：第一轮 scheduleForward 排定 100 工序（PLANNED + 各自预留行）→ 新工单扫描建 DRAFT 工序 → 第二轮 scheduleForward：pending 仅新工序，引擎从 earliest（自动工序 NULL→floor=now/horizonStart，见 001）贪心放置——**与既有 PLANNED 工序同时段同工作中心即重叠**（这正是有限产能排产要解决的常态）→ acquireReservation pre-check 命中既有预留 → 抛 ERR_APS_CAPACITY_CONFLICT → @BizMutation 整轮回滚 → 重试同结果 → **增量排产不可用**（除非人工把全部 PLANNED revertToDraft 后全量重排，或所有新工序都排在既有工序之后的空闲时段——引擎不感知占用故不会主动避让）。测试面：`TestErpApsCapacityReservation#testConcurrentScheduleForwardSharedWorkcenterThrowsCapacityConflict` 仅覆盖「pre-insert 预留模拟并发胜出」场景（注释自认单会话模拟），**未覆盖「合法既有 PLANNED + 新 DRAFT 增量排产」**——该场景按现实现必抛冲突。insertRushOrder 路径反而正确（frozen 预填 L107-111 + 按窗口释放），对照凸显 run() 路径缺口。CTP 模拟路径也正确（`snapshotTimelines` 预填 PLANNED 工序，L617-631）——**同一引擎有两种时间轴构建口径，唯独全量 run() 不预填**。
- **问题**：D7 并发/一致性设计断层——预留表被用作并发兜底却未被引擎当作排产输入；第二次以后的排产基本失败。
- **建议修复方向**：run() 加载既有 PLANNED/IN_PROGRESS 工序（或按 reservation 表）作为 frozen 预填时间轴（对齐 insertRushOrder/snapshotTimelines 口径）；或 persist 冲突时对该工序标记 UNSCHEDULABLE + conflict 继续而非整轮抛（保留预留表并发兜底语义）。
- **arm-index 裁决**：新增（grep「增量排产/frozen null/预留 不感知」零命中；P0-MA2-019 resolved 记录的是并发 UK 兜底维度——其设计前提「引擎先排完再抢占 pre-check」仅在单轮全量场景成立）。

### P2-CK-aps-003（D8）产能预留生命周期不完整——cancel()/complete()/updateSchedule()/doDispatch() 均不释放预留，ghost 预留行只增不减并持续收缩可排产能

- **控制点 A**：grep 实证 `releaseReservationsByOrder` 生产代码仅 2 个调用点：`ErpApsSchedulingInsertRushOrderProcessor` L80（revertToDraft 前置）+ `ErpApsRoutingManualOverrideProcessor` L58（回退 DRAFT 前置）——`ErpApsOperationOrderBizModel#cancel`（L243-258）、`#complete`（L228-239）、`#updateSchedule`（L341-358）、`ErpApsAutoDispatchProcessor#doDispatch`（L348-376）**均不释放**
- **证据**：owner doc state-machine.md §2 状态表逐字「已取消（CANCELLED）……**释放产能预留**」「PLANNED|IN_PROGRESS→CANCELLED ……**释放预留产能**」——但 §4 并发段又裁决「PLANNED→IN_PROGRESS/FINISHED/CANCELLED 状态翻转的预留释放归 P1-MA2-077 MR1」（Bean javadoc 同注，Deferred）。**裁决存在但表内承诺未兑现且无 successor 触发条件**；实际后果：FINISHED/CANCELLED 工序的预留行永久滞留 → `hasOverlappingReservation` 把死时段当占用 → 与 P1-CK-aps-002 叠加使可排产能单调收缩（假冲突递增）。updateSchedule 的预留失同步（拖拽后旧行残留）无任何裁决覆盖。
- **问题**：D8 冗余资源失同步（部分 adjudicated MR1，部分无裁决）。
- **建议修复方向**：cancel/complete 入口释放（或 FINISHED/CANCELLED 预约标记失效——pre-check 按状态 join 过滤）；updateSchedule 同步迁移预留行；owner doc MR1 触发条件补登。
- **arm-index 裁决**：部分复用（cancel/complete 释放 = P1-MA2-077 MR1 已裁决 Deferred——登记现状确认 + ghost 累积/updateSchedule 失同步为新增维度）。

### P2-CK-aps-004（D5/D6）updateSchedule（甘特拖拽 mutation）零产能校验、零状态守卫、零预留同步——设计规则 8「拖拽需后端产能校验」无实现但 mutation 已暴露

- **控制点**：`ErpApsOperationOrderBizModel#updateSchedule`（L341-358：`if (start == null) throw ...; order.setPlannedStartDateT(Timestamp.valueOf(start)); if (end != null) { order.setPlannedEndDateT(...); } updateEntity(order, null, context);`——**无状态检查（FINISHED/CANCELLED/IN_PROGRESS 均可拖）、无重叠校验、不更新 capacity_reservation、end 可空导致 start/end 不变式破坏**（plannedEnd 残留旧值 < 新 start 时段倒挂））
- **证据**：scheduling.md §8.3 接口定义「dragUpdateOperation(opId, newStartTime): void // 拖拽更新工序时间（**需校验产能**）」+ §11 规则 8「甘特图拖拽调整需后端产能校验（**前端不信任用户输入**）」；实现约定（scheduling.md 头部）把「甘特图前端可视化 / dragUpdateOperation 拖拽后端校验」标 Non-Goal 归前端计划——但 mutation 本体已存在于 BizModel 且经 GraphQL 可调，调用即产生：与既有 PLANNED 工序重叠的排程 + 预留表失同步（联动 003）+ 任意终态工序时间篡改。Non-Goal 裁决覆盖「校验」，未覆盖「暴露无守卫的裸写入口」。
- **问题**：D5 入参/状态边界（设计规则明确要求的后端校验缺位 + 入口先行暴露）。
- **建议修复方向**：最小修复：入口加状态守卫（仅 DRAFT/PLANNED）+ end 缺省时按 totalDuration 推算 + 同步迁移预留；完整产能校验随前端计划落地。
- **arm-index 裁决**：新增（grep「updateSchedule/dragUpdate 校验」零命中）。

### P2-CK-aps-005（D6）前向排产排序键 (priority, latestEndDateT, sequence) 下同工单优先级乱序时前置工序约束失效——后序工序可先于前序工序排定（finish-before-start 倒挂）

- **控制点**：`app/erp/aps/service/scheduling/ErpApsSchedulingEngine.java#applyPredecessorConstraint`（L519-531：`if (op.getSequence() != null && op.getSequence() > chain.lastSequence) { ... return max(earliest, chain.lastEnd + buffer); } return earliest;`——**仅约束 sequence 大于链游标的工序**；`recordChain`（L547-553）只推进不回溯）+ `sortByForward`（L494-504：priority ASC → latestEndDateT ASC nullsLast → sequence ASC → workOrderId ASC——**priority 是全局首键**，同工单内优先级可不同）
- **证据**：反例：工单 W 的 op10(priority=5) / op20(priority=1)：排序后 op20 先处理——chain 为空，op20 从自身 earliest 起排（可能当天 8:00）；op10 后处理——`10 > chain.lastSequence(20)` 为 false，无约束（正确方向），但 **op20 未等待 op10 完工**——产出排程 op20.plannedEnd 可能早于 op10.plannedStart，违反 owner doc scheduling.md §2.4「每个工序的 earliestStartDateT = MAX(**上工序.plannedEndDateT**, 工作中心最早可用时间)」与 §11 规则 2「前置工序必须完成才能开始下工序」。后向对称（applySuccessorConstraint L533-545 仅约束 sequence < lastSequence）。构造条件：同工单工序 priority 或 latestEndDateT 乱序（急单工序提权是 priority 字段的设计用途，README「0=最高」）。当前测试按同工单同 priority + sequence 升序构造，未覆盖乱序。
- **问题**：D6 算法边界（约束模型为单游标链，非全序依赖图）。
- **建议修复方向**：同工单工序强制 sequence 主序（排序键把 workOrderId+sequence 提到 priority 之前，priority 仅作工单间排序）；或链记录改为 per-sequence map 支持乱序回填校验。
- **arm-index 裁决**：新增（grep「前序约束/sequence 倒挂/chain 游标」零命中）。

### P2-CK-aps-006（D4/D2）三个告警事件模板种子缺失——aps.workorder-no-routing / aps.operation-workcenter-missing / aps.dispatch-material-shortage 经 notify 静默跳过

- **控制点**：`app/erp/aps/service/ErpApsConstants.java`（L48-50 三个 NOTIFY_EVENT 常量）+ 消费点 `ErpApsWorkOrderToOperationProcessor#createOperationOrdersFromWorkOrder`（L88-101：无工艺路线/工作中心缺失 → LOG.warn + notify → **notify 静默跳过（无 ACTIVE 模板）**）+ `ErpApsAutoDispatchProcessor#notifyShortage`（L403-415 同型）对照 `module-notify/deploy/sql/{mysql,oracle,postgresql}/_seed_erp-notify.sql`（25 个种子类型中 **aps.* 零命中**；repo 全仓 *.sql grep 三事件名零命中）
- **证据**：与 ck-logistics P1-CK-log-002 同根（notify 侧静默跳过设计 + 种子失同步），但 aps 侧有 `result.setSkippedNoRouting(true)/getRejectedSequences()` 返回载体 + LOG.warn 兜底（计划员手动触发可见返回值），告警非唯一闭环入口，故 P2。job 拉取路径（ErpApsWorkOrderScanJob）调 `createOperationOrdersFromWorkOrder` 返回值被丢弃（只累计 createdCount）——**job 触发时跳过完全不可见**。
- **问题**：D4 告警接线断裂（job 路径放大）。
- **建议修复方向**：三库 seed 补 3 行模板（对齐 log.draft-escalation 7201 范式，ROLE 计划员接收）；与 ck-notify P1-CK-notify-002 联动批量补齐。
- **arm-index 裁决**：新增（grep 三事件名 arm-index 零命中）。

### P2-CK-aps-007（D4）WorkOrderScanJob biz 直调无 runInSession 包裹——与同模块 AutoDispatchJob 自述范式相悖，job 路径实体写入缺会话载体

- **控制点 A**：`app/erp/aps/service/job/ErpApsWorkOrderScanJob#execute`（L38-47：`Integer created = operationOrderBiz.scanReleasedWorkOrders(ctx);`——**直接 biz 接口调用，无 ormTemplate.runInSession**）
- **控制点 B**：同模块 `ErpApsAutoDispatchJob#execute`（javadoc 逐字「扫描经 runInSession 包裹（对齐 ErpHrLeaveApproverTimeoutJob 范式）：**biz 代理直调无请求级 ORM session，派工的实体更新须在打开的 session 内完成**」+ L57 `ormTemplate.runInSession(session -> operationOrderBiz.scanAutoDispatch(ctx))`）+ 全仓 job 范式（logistics TrackingPollJob/DraftEscalationJob、contract/hr/b2b 各 job 均 runInSession；ck-contract.md L219 裁决「@SingleSession 经 IBiz 代理不生效的补偿，hr 先例」）
- **证据**：`scanReleasedWorkOrders` → `createOperationOrdersFromWorkOrder` → `opOrderDao().saveEntity(...)`（L105）+ notify 链的 NotificationDao 写入——nop-job BeanMethodJobInvoker 反射调用不提供请求级 session（AutoDispatchJob javadoc 自证），无 session 时 dao 写入的运行时行为（抛错或隐式开 session）依平台版本而定。两 job 同模块不同范式，至少其一有错；按自述范式 WorkOrderScanJob 为缺包裹方。
- **问题**：D4 job 接线（同构不对称 + 违背自认范式）。
- **建议修复方向**：execute 改 `ormTemplate.runInSession(session -> operationOrderBiz.scanReleasedWorkOrders(ctx))`；修复阶段以 job 单测实证（若平台隐式开 session 则降级 not-a-problem 并统一两 job 注释）。
- **arm-index 裁决**：新增（grep「WorkOrderScanJob session」零命中；P1-RC-088 resolved RC-R1.86 记录的是建单链路维度）。

### P2-CK-aps-008（D5，同型 P1-CK-pur-003 族）CRUD update 无已审守卫——OperationOrder status/plannedStartDateT/machineId 可经 update_ 直改，绕过 13 边状态机与预留同步

- **控制点**：`ErpApsOperationOrderBizModel`（defaultPrepareSave 仅 businessDate 兜底 L80-86，defaultPrepareUpdate 未 override——status 可直改 FINISHED→PLANNED 等（Bean 矩阵外迁移）、plannedStartDateT/EndDateT 直改绕过 004 所需的一切校验与预留）+ `ErpApsScheduleBizModel`（status 可直改 DRAFT→ARCHIVED 跳过 publish 语义）+ Constraint/DispatchRule/OpRouting/CapacityReservation/DispatchLog 6 个 stub BizModel 无守卫（DispatchRule.enableAuto/holdUntil 直改影响派工；Constraint 时段直改不触发重排）
- **问题**：同型 P1-CK-pur-003 族——aps 站点登记（不复用展开）。
- **建议修复方向**：defaultPrepareUpdate 守卫：status 变更仅允许经命名 mutation；planned 时间/machineId 变更引导至 updateSchedule/manualOverrideRouting（先补其守卫，见 004）。
- **arm-index 裁决**：同型登记（P1-CK-pur-003 族，aps 站点新增计数）。

### P3-CK-aps-009（D4）README 配置点 4 键零消费——default-scheduling-mode / priority-rule / time-bucket-minutes / auto-reschedule-on-insert 声明后 main 代码零读取

- **控制点**：`ErpApsConfigs`（L8-20 四键声明）对照 grep 实证：`CONFIG_DEFAULT_SCHEDULING_MODE`/`CONFIG_PRIORITY_RULE`/`CONFIG_TIME_BUCKET_MINUTES`/`CONFIG_AUTO_RESCHEDULE_ON_INSERT` 在 main 代码零消费（README 配置表 + scheduling.md §十 同列）。已消费键：buffer-minutes-between-ops / max-reschedule-window-days / workorder-scan-cron / auto-dispatch-enabled+cron。
- **证据**：影响最大的 `auto-reschedule-on-insert` 默认 true 承诺「插单时是否自动触发区间重排」——实际**无任何自动触发路径**（insertRushOrder 仅手动 mutation；工单扫描 job 只建 DRAFT 不触发重排；引擎排序硬编码 priority/EDD 不读 priority-rule；排产模式由 mutation 显式区分 scheduleForward/scheduleBackward 不读 default-scheduling-mode；时间槽粒度完全未实现——Engine 连续时间轴非离散槽）。同 b2b P2-CK-b2b-004 / logistics P3-CK-log-010 家族形态。
- **问题**：D4 声明 vs 实现。
- **建议修复方向**：实现 auto-reschedule-on-insert（扫描建单后对同工作中心窗口自动调 insertRushOrder）或从 README/§十移除；其余三键同步处理。
- **arm-index 裁决**：新增（grep 四键名 arm-index 零命中）。

### P3-CK-aps-010（D4，同型 cron 键漂移家族）两个 job.yaml 的 cronExpr 键与 bean 内层空值跳过键不同名——`<key>.cron-expr` vs `<key>-cron`

- **控制点 A**：`app-erp-all/_vfs/nop/job/conf/erp-aps-auto-dispatch.job.yaml`（`cronExpr: "@cfg:erp-aps.auto-dispatch.cron-expr|0 * * * * ?"`——默认**每分钟**）对照 `ErpApsAutoDispatchJob#execute`（L45-51 读 `AppConfig.var("erp-aps.auto-dispatch-cron", "")` 空值跳过）
- **控制点 B**：`erp-aps-workorder-scan.job.yaml`（`cronExpr: "@cfg:erp-aps.workorder-scan.cron-expr|0 0/5 * * * ?"`——默认每 5 分钟）对照 `ErpApsWorkOrderScanJob#execute`（L39-44 读 `erp-aps.workorder-scan-cron`）
- **证据**：两键名（点号 `.cron-expr` vs 连字符 `-cron`）不一致——用户只配 bean 键（如 `erp-aps.auto-dispatch-cron=0 0/15 * * * ?`）+ `nop.job.<job>.enabled=true` 时，调度按 job.yaml 默认节奏（每分钟）触发、bean 检查通过 → **派工扫描每分钟跑而非配置的 15 分钟**；反之只配 `*.cron-expr` 键对 bean 门控无效（配了 cron-expr 但 -cron 空 → 永跳过）。对照单键正确形态：logistics 两 job（cronExpr 键=bean 键）。同型家族先例：P3-CK-crm-014 / mfg-013 / inv-021 / sal-023 / qa-019 / hr-012 / cs-014（b2b 报告核查其为单键不成立）。缓解：两 job enabled 默认 false + auto-dispatch 另有全局开关默认 false——默认部署零影响。
- **问题**：D4 调度键漂移（同型登记）。
- **建议修复方向**：job.yaml cronExpr 键改为与 bean 相同的 `-cron` 键（对齐 R1.35 修复形态），或在 bean 侧改读 `.cron-expr` 键——两 job 统一。
- **arm-index 裁决**：同型登记（cron 键漂移家族，aps 站点 ×2）。

### P3-CK-aps-011（D10/D8）杂项：buildTimelines 对约束空时间无守卫（NPE）+ 物料齐套/ATP 聚合无 orgId 过滤（跨组织口径混合）+ ATP 公式与设计 §7.1 漂移

- **控制点 A**：`ErpApsSchedulingEngine#buildTimelines`（L462-474：`c.getStartTime().toLocalDateTime()`——constraint start/end 为 null 时 NPE；ORM constraint startTime/endTime 非强制时外部录入可触发，整轮排产 500）
- **控制点 B**：`ErpApsAutoDispatchProcessor#sumAvailable`（L316-326：`eq("materialId", materialId)` 全仓求和——**无 orgId/仓库维度过滤**）+ `ErpApsAtpCtpServiceImpl#sumOnHand/#sumReserved`（L116-139 同型）——多组织部署下 A 组织工单消耗 B 组织库存判定齐套/可承诺
- **控制点 C**：`ErpApsAtpCtpServiceImpl#atpAvailable`（L110-114：`onHand - reserved >= qty`）对照 scheduling.md §7.1「可用量 = 现有库存 + **计划入库** − 已预约量 − **安全库存**」——缺计划入库与安全库存两项（文档未标注简化裁决）；`sumReserved` 对每条 reservation line 逐条 getEntityById 判 ACTIVE（N+1）
- **arm-index 裁决**：新增（ATP 口径漂移归 P2-RC-080 相邻但该条登记的是 sales 接线维度；orgId 聚合为 orgId 隔离族读侧新站点——并入本杂项不独立展开）。

## 跨域/同型关联影响面注记（不新建 finding）

| 已登记 finding / arm 条目 | aps 受影响面 | 结论 |
| --- | --- | --- |
| `P1-CK-pur-003` 族（CRUD update 无守卫） | OperationOrder/Schedule + 6 stub | 同型登记为 P2-CK-aps-008 |
| cron 键漂移家族（crm-014/mfg-013/inv-021/sal-023/qa-019/hr-012/cs-014） | aps 两 job 均为漂移形态（`<key>.cron-expr` vs `<key>-cron`） | **同型登记为 P3-CK-aps-010**（家族新站点 ×2） |
| orgId 隔离族（P2-CK-fin2-007 / mfg-007 / hr-003④ / b2b-003 写侧形态） | aps 表现为**读侧聚合**缺 orgId（齐套/ATP 全仓求和） | 并入 P3-CK-aps-011（不独立计数） |
| `P1-CK-fin-003`（post 幂等 null） | aps 无过账面（README「APS 不产生会计凭证」） | 不适用 |
| `P0-CK-mfg-001`（固定幂等键吞增量） | 无固定键幂等形态（幂等 = hasExistingOperationOrders 存在性守卫） | 不适用 |
| `P2-RC-079`（后向排产交期不可达通知 sales 缺失，watch-only） | `scheduleBackward` DEADLINE_NOT_REACHABLE 仍仅记 SchedulingResult 不外呼 | 复用不展开 |
| `P2-RC-080`（sales 审核触发 ATP/CTP 接线缺失，watch-only） | aps 侧 earliestCompletionDate/checkFeasibility 完整 | 复用不展开 |
| `P1-MA2-077`（状态守卫，resolved）+ MR1 预留释放 Deferred | Bean 13 边 + start/complete/cancel 守卫在位；MR1 释放未落地（owner doc §4 裁决归 MR1） | 修复在位 + 现状登记为 P2-CK-aps-003 |
| `P1-MA2-078`（IN_PROGRESS cancel 审批，resolved via deferral） | cancel 仍仅三源守卫无审批门（Deferred 注记在案） | 复用 deferral 注记 |
| `P0-MA2-019`（产能预留 UK，resolved） | acquireReservation pre-check + UK + flushSession 翻译在位 | 修复在位验证（增量盲区为新增维度 P1-CK-aps-002） |
| `P1-RC-088/089/090`（resolved RC-R1.86/1.87/1.88） | 拉取建单/替代路由/自动派工三链路全部在位 | 修复在位验证（各自新增维度见 001/006/007） |

## 验证为正确（显式排除，防误报）

- **requireSchedule DRAFT 守卫正确**（任务点名「非法迁移守卫 ERR_APS_SCHEDULE_ILLEGAL_STATUS」）：`ErpApsSchedulingProcessor#requireSchedule`（L229-242：not-found 抛 NOT_FOUND + 非 DRAFT 抛 ILLEGAL_STATUS）；`ErpApsScheduleBizModel` publish 仅 DRAFT、archive {DRAFT,PUBLISHED} 幂等含 ARCHIVED 短路——与 owner doc §9.2 一致。
- **工序状态机 13 边 Bean 矩阵完整**：start/complete/cancel 三源/revertToDraft/hold/unhold 双源/shortageHold + 引擎驱动 4 边（schedule×2/markUnschedulable）声明；`ErpApsOperationOrderBizModel` start/complete/cancel 全部 assertCan + 领域码映射 + cause 保留；FINISHED/CANCELLED 终态无出边；UNSCHEDULABLE 与 DRAFT 同池重试（loadPendingOrders L83-85，RC-R1.87 自愈语义在位）；HOLD/ON_HOLD 有 writer（hold/doShortageHold/unhold）非死状态（RC-R1.88 dict 扩展落地）。
- **insertRushOrder 区间重排语义正确**（任务点名「工序窗口重叠回退 DRAFT 重排」）：IN_PROGRESS 硬守卫先于选择（L57-63 抛 NOT_RESCHEDULABLE）；优先级数字越大越低语义正确（L68-75 `opPriority > rushPriority` 回退）；revert 前释放预留（L80）；frozen 预填时间轴（L107-111 + seedFrozenPlanned）；窗口 [earliest, latestEnd+buffer] 与 §6.1 一致；仅窗口内重排不全局（§六 反模式遵守）。
- **P0-MA2-019 并发防护在位**：acquireReservation 重叠 pre-check + UK_APS_CAPACITY_RESERVATION_SLOT 兜底 + 每次 INSERT 后 flushSession 使 UK 违例在方法边界翻译为 ERR_APS_CAPACITY_CONFLICT（JdbcException catch）；releaseReservationsByOrder 硬删 + flush 防幻读。
- **替代路由选择正确**（RC-R1.87）：resolveCandidates 默认行关联（isDefault && machineId 匹配）+ 生效期/批量过滤 + allowFallback=false 仅主选 + stripPreviousDelta 幂等剥离（重复排产先减旧 delta 再加新）+ selectionReason 四分类（blockedOnlyByMaintenance 区分 PRIMARY_DOWN/OVERBOOKED）；manualOverride=true 跳过自动选择。
- **manualOverrideRouting 正确**：剥离旧 delta → 叠加新 delta → 回写五字段 → PLANNED 时释放预留 → DRAFT 回退 + 清时间 → remark 审计追加（含操作者）；validateOverridableStatus 三源白名单。
- **WorkOrderToOperation 主干正确**：幂等守卫（任一 op 存在即跳过整单）；RELEASED_STATUSES 段与 D1 拉取裁决一致；工艺路线缺失/工作中心缺失告警 + 跳过/拒绝语义与 L1 一致；orgId 从 WO 透传（L179）；totalDuration 与引擎同公式单一真相源（L182-184）。
- **ATP/CTP 影子不持久化**：buildShadowOps 经 newEntity 构造从不 save（grep saveEntity 零命中于影子路径）——§7.2「仅做模拟」与 §11 规则 7 遵守。
- **`addOrderField` 布尔参数 6 处全部正确**（平台源码 `QueryBean.java` L433-438 实证第二参为 desc）：findReleasedWorkOrders `("createTime", false)` 升序=扫描顺序稳定（javadoc ASC 一致）✓、findGanttData `("plannedStartDateT", false)` 升序 ✓、loadRules `("workcenterId", false)`/`("id", false)` ✓、MergeCoordinator（notify 域）`("createTime", true)` 降序=取最新可合并 ✓、findUnread `("sentAt", true)` 最新优先 ✓。
- **D1 机械扫描零命中**：module-aps 全部 main 代码 `System.currentTimeMillis/new Date()/LocalDateTime.now()/@Inject private/extends RuntimeException/printStackTrace`/字符串 `==` 零命中（时间全部 CoreMetrics；job 失败接力 LOG.error 在位）。
- **beans.xml 13 bean 接线完整**：facade + 5 Processor + AtpCtp + LoadSourceProvider + StateMachine + WorkOrderToOperation + 2 job bean 全注册，无孤立声明。
- **findGanttData 依赖连线正确**：同工单 sequence 升序 link 链 + null 时间跳过 + limit 500 + machineId/status 可选过滤（§8.1 数据形态对齐）。
- **日期/时区边界（D6 任务点名「排产窗口跨日/时区」）**：引擎全程 LocalDateTime + plusMinutes/minusMinutes 自然跨日无午夜截断；`DateHelper.dateTimeToTimestamp` 单一系统时区一致换算；**多时区部署未设计**（全域单时区假设）——登记为剩余风险而非缺陷（与 logistics 同注记）。
- **AutoDispatch 主干正确**：全局开关+rule enableAuto+holdUntil+enabledHours 四层门控；maxConcurrentOps 缺省回落工作中心 capacity；缺料 ON_HOLD + 通知降级；手动派工 note 必填；operator/tooling 无载体降级 null 放行有 javadoc 裁决（auto-dispatch.md §2.3 Deferred）。

## arm-index 复用 or 新增裁决（汇总）

- **新增** 7 条：`earliest NULL 过滤漏排`（001）/`增量排产不感知既有占用`（002）/`updateSchedule 裸写`（004）/`前序约束乱序失效`（005）/`aps 告警模板种子缺失`（006）/`WorkOrderScanJob 无 session`（007）/`4 配置键零消费`（009）+ 003 的 updateSchedule/ghost 累积维度 + 011 杂项（约束 NPE/orgId 聚合/ATP 口径）。
- **同型登记**：P2-CK-aps-008（pur-003 族）；P3-CK-aps-010（cron 键漂移家族 ×2）。
- **复用（注记不展开）**：P2-RC-079、P2-RC-080（watch-only）、P1-MA2-078（deferral）、P1-MA2-077 MR1（部分——003 登记 ghost 累积新增维度）。
- **修复在位验证**：P0-MA2-019、P1-MA2-077（守卫面）、P1-RC-088/089/090（RC-R1.86/1.87/1.88）。
- **同型核查不适用**：P1-CK-fin-003（无过账面）、P0-CK-mfg-001（无固定幂等键形态）。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 2 | P1-CK-aps-001、P1-CK-aps-002 |
| P2 | 6 | P2-CK-aps-003..008 |
| P3 | 3 | P3-CK-aps-009..011 |

按主维度：D6×2（001、005）、D7×1（002）、D8×1（003）、D5×2（004、008）、D4×3（006、007、009，010 亦 D4 归 P3 桶）、D10×1（011）。（精确主维度归属：001 D6、002 D7、003 D8、004 D5、005 D6、006 D4、007 D4、008 D5、009 D4、010 D4、011 D10。）

同型/复用裁决：同型登记 2（008 pur-003 族、010 cron 漂移家族）+ arm 复用不登记 4（P2-RC-079/080、P1-MA2-078、P1-MA2-077-MR1 部分）+ 修复在位验证 4 + 同型核查不适用 2。

## 剩余风险（查了什么/没查什么）

- **已查**：erp-aps-service 23 文件全量通读 + orm（7 实体 versionProp/UK）+ beans 13 bean + 2 job.yaml（cron 键逐字比对）+ notify seed 三库 + 平台 addOrderField 实证 + TestErpApsCapacityReservation/Engine 测试场景核对（增量场景覆盖缺口实证）+ owner docs 五份（README/scheduling/state-machine/auto-dispatch/alternative-routing）+ D1 机械扫描。
- **未深查**：`erp-aps-web` 甘特/派工页面契约 drift（findGanttData 后端已核）；`erp-aps-api` 骨架；`_gen/`；`constraint-based-planning.md`（标注暂不编码）；nop-job-local BeanMethodJobInvoker 是否隐式开 session（平台源码该模块未定位到——007 定性依赖自述范式而非平台源码）；batchScheduleForward 行级 catch 后 managed 实体脏检查是否在提交时落库部分 PLANNED（平台 ORM flush 时序未实证——见下）。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-aps-002** 的「增量排产必冲突」结论依赖引擎贪心放置与既有占用的重叠概率（earliest 各异/工作中心分散时可避开）；建议集成测试实证：第一轮排产 → 新增 DRAFT 同工作中心 → 第二轮 scheduleForward → 断言 ERR_APS_CAPACITY_CONFLICT。若平台或业务侧存在「每轮排产前全量 revert」的隐性约定（未见代码载体），降级 P2。
  2. **P1-CK-aps-001** 需确认 `ErpApsSchedule.horizonStart` 的实际填写率（表单非强制时多数方案 horizon 为空则触发面收窄）；建议查 erp-aps-web 表单与种子数据。
  3. **P2-CK-aps-007** 的运行时行为需平台源码/单测定论（nop-job-local invoker 的 session 语义）；若隐式开 session 则两 job 范式统一为注释修复（not-a-problem）。
  4. **batchScheduleForward 部分提交**（未登记 finding）：行级 NopException catch 后，同 session 内已由引擎改写的 managed 实体（PLANNED + 时间）可能在事务提交时经脏检查落库而**无对应预留行**——该行为依赖平台 flush 时序，建议修复阶段实测后决定是否补登记。
