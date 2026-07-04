# 2026-07-03-1707-1-manufacturing-crp-load-engine 制造产能需求计划（CRP）负荷计算引擎

> Plan Status: completed
> Last Reviewed: 2026-07-03
> Source: `docs/backlog/extended-roadmap.md` 工作项 2.8；`docs/design/manufacturing/crp.md`；`docs/design/manufacturing/README.md`
> Related: `docs/plans/2026-07-02-1538-2-manufacturing-bom-routing-rollup.md`（BOM/工艺路线 done）、`docs/plans/2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md`（WorkOrder done）、`docs/plans/2026-07-02-2237-2-manufacturing-mrp-engine.md`（MRP done）
> Mission: erp
> Work Item: 2.8 CRP 负荷计算
> Audit: required

## Current Baseline

实时仓库逐项核实的事实：

- **工作中心实体存在但产能为单一标量（反模式）**：`ErpMfgWorkcenter`（`module-manufacturing/model/app-erp-manufacturing.orm.xml:325`，表 `erp_mfg_workcenter`）含 `code`/`name`、计费/工时字段（`hourlyRate`/`workHoursPerDay`/`isExternal`）+ **单一标量 `capacity`**（`:332` stdSqlType=VARCHAR/stdDataType=string，domain 标注 DECIMAL 但落 string）+ `capacityUnit`（`:333`）。`crp.md §反模式警示` 明确「产能硬编码为单一标量」为禁用模式——CRP 须按产品产能子实体建模，不复用此标量（既有 `hourlyRate`/`workHoursPerDay` 属计费/工时维度，非按产品产能，CRP 不依赖）。
- **CRP 三实体均为新建（不存在）**：`ErpMfgWorkcenterCalendar`（工作中心日历/班次）、`ErpMfgWorkcenterCapacity`（按产品产能+换模/清理/效率）、`ErpMfgCrpLoad`（负荷快照行）在 manufacturing ORM 实体清单中**均不存在**（已核实实体清单：BOM/Routing/WorkOrder/JobCard/MaterialIssue/MrpPlan/Subcontract/BatchGenealogy/CostRollup/CostVariance/Workcenter，无 CRP 相关）。
- **负荷来源数据已就绪**：`ErpMfgWorkOrder`（`orm.xml:366-367`）含 `plannedStartDate`/`plannedEndDate`（DATE）；`ErpMfgRoutingOperation`（`orm.xml:305` workcenterId）经 `workcenterId` 关联工作中心（工单工序经工艺路线工序定工作中心）。无 APS 模块（OperationOrder 排产未落地），负荷来源本期取 WorkOrder 计划日期 + RoutingOperation 标准工时。（注：`ErpMfgWorkOrderLine` 无 workcenterId 列；`JobCard`(`:784`)/`CostVariance`(`:857`) 虽带 workcenterId 但非负荷分派主路径。）
- **BizModel 为空 CRUD 壳**：`ErpMfgWorkcenterBizModel` 为 `CrudBizModel` 壳，无 CRP 计算方法。无 CRP service / 负荷计算类（`erp-mfg-service` 下 `crp` 关键字零命中）。
- **nop-rule 引擎可用（非硬编码）**：平台 `nop-rule` 模块提供 `evaluateRule(ruleName, version, inputs)`（见 `../nop-entropy/docs-for-ai/03-modules/nop-rule.md`），但 CRP 负荷计算为日历/算术聚合（非评分公式），本期用纯 Java 聚合即可；nop-rule 留给评分卡类计划。
- **DAG 依赖方向**：manufacturing CRP 为只读报表，读取 manufacturing 域内（WorkOrder/RoutingOperation/Workcenter），**无跨域写依赖**（不写 finance/inventory）。maintenance 停机扣减可用时段为事件驱动（maintenance 不反向被 manufacturing 依赖），本期 Non-Goal。
- **剩余差距**：(1) 无日历/班次建模；(2) 无按产品产能子实体（仍是标量）；(3) 无 CRP 负荷计算（WorkOrder 计划日期→工作中心×日期负荷聚合→对比可用产能→负荷率+超负荷告警）；(4) 无负荷报表查询。

## Goals

- **产能四要素分离建模**（`crp.md §核心设计点`）：新增 `ErpMfgWorkcenterCalendar`（日历/班次：shiftType/workDatePattern/startTime/endTime/effectiveFrom-To）、`ErpMfgWorkcenterCapacity`（按产品：materialId/capacityPerHour/setupTime/cleanupTime/efficiencyFactor）、`ErpMfgCrpLoad`（负荷快照行：workcenterId/loadDate/loadHours/setupHours/workOrderId）三实体至 manufacturing ORM（**ask-first 保护区域**，新增不改动既有表）+ codegen 生成 dao/meta/service/web。
- **CRP 负荷计算引擎**（只读，不写排程）：`IErpMfgCrpBiz.calculateLoad(periodFrom, periodTo, workcenterIds?)`——扫描 WorkOrder（plannedStartDate~plannedEndDate 落区间、非 CANCELLED）经 RoutingOperation 分派到工作中心，按 workcenter×loadDate 聚合 loadHours（标准工时）+ setupHours（换模），对比 WorkcenterCalendar 可用工时 × WorkcenterCapacity.efficiencyFactor 得 capacityHours，计算 loadRate=loadHours/capacityHours；写 `ErpMfgCrpLoad` 快照行（重算前清区间快照）。
- **负荷报表查询**：`IErpMfgCrpBiz.getLoadReport(periodFrom, periodTo, workcenterIds?)` 返回 workcenter×date 聚合（loadHours/capacityHours/loadRate/overloaded），`overloaded = loadRate > erp-mfg.crp-overload-threshold`（默认 1.0）。
- **配置门控**：`erp-mfg.crp-overload-threshold`（默认 1.0）经 `AppConfig.var(..., defaultValue)` 读取。
- **行为测试覆盖**：负荷计算（单/多工单分派到工作中心×日期 + 日历可用工时 + 效率折算 + 负荷率）、超负荷告警（loadRate>阈值）、重算清旧快照、空 WorkOrder（capacityHours 非零 loadHours 零）。

## Non-Goals

- **APS 工序级排产（写 OperationOrder 排程时间）**：`crp.md §边界` CRP 只读不写，APS（`aps/README.md`）写入排程。属 M3 工作项 3.10/3.11。**触发条件**：APS 模块落地时（successor）——届时 CRP 负荷来源可切换为 OperationOrder 排程时间（本期 fallback 为 WorkOrder 计划日期）。
- **maintenance 停机扣减工作中心可用时段**：`crp.md §跨域协作` 事件驱动，maintenance 发布停机、CRP 消费扣减。需 maintenance 停机事件机制（当前 maintenance 计划 1018-3 停机通知制造为 Non-Goal）。**触发条件**：maintenance 停机事件 + APS/排产停机窗口联动需求时（successor）。
- **MRP 物料需求**：`mrp.md` 已 done（2.3），与 CRP 产能正交。
- **Workcenter.capacity 标量废弃迁移**：既有标量保留为旧显示字段，CRP 一律用 WorkcenterCapacity 子实体（反模式警示）。**触发条件**：存量数据迁移需求时（out-of-scope improvement）。
- **CRP 负荷可视化页面（AMIS）**：本期交付负荷报表 GraphQL 查询；AMIS 负荷甘特/热力图页面为独立前端面。**触发条件**：CRP 报表前端需求时。
- **CRP 定时运行（`erp-mfg.crp-run-schedule` cron）**：`crp.md §配置点` 列出该 cron，但本期负荷计算以按需 `@BizMutation calculateLoad` 暴露入口（手动/nop-job 可调），cron 接线属部署配置非硬依赖。**触发条件**：CRP 定时自动运行需求时（on-demand 入口已可被周期调度复用）。

## Task Route

- Type: `app-layer design change + implementation`（新增 3 实体至 manufacturing ORM → codegen → CRP 负荷计算 + 报表查询；纯只读计算，无跨域写依赖，无 finance 过账）。
- Owner Docs: `docs/design/manufacturing/crp.md`（产能四要素分离 + 实体清单 + 业务规则 + 边界 + 反模式）、`docs/design/manufacturing/README.md`（manufacturing 域边界）、`docs/architecture/data-dependency-matrix.md`（manufacturing CRP 只读，无跨域写）。
- Skill Selection Basis: ORM 模型新增 + BizModel + 只读计算/报表（@BizQuery）+ 配置门控 → 加载 `nop-backend-dev`（实体服务 + 计算方法模式）；ORM 模型变更须经 ask-first 审查；测试 `nop-testing`；草案/结束审计 `plan-audit-prompt.md`/`closure-audit-prompt.md`。
- **Decision（负荷来源 fallback）**：**选择** 本期负荷来源取 WorkOrder（plannedStartDate~plannedEndDate）+ RoutingOperation（workcenterId + 标准工时），无 APS 时按计划日期均匀分派到区间内工作日。**替代**：等待 APS OperationOrder 排程时间（APS 未落地，CRP 不可达，rejected）。**残留风险**：均匀分派与实际排产有偏差（CRP 为粗负荷报表，可接受；APS 落地后切换精确来源）。
- **Decision（负荷桶粒度）**：**选择** 日粒度（`ErpMfgCrpLoad.loadDate` + loadHours + setupHours），按 workcenter×date 聚合。**替代**：班次粒度（数据量爆炸 + 日历班次本期建模但负荷按日聚合已足，rejected）。**残留风险**：日内多班次超负荷不可见（日级负荷率反映日总量，班次级为 APS 范畴）。
- **Decision（标量 capacity 处理）**：**选择** 既有 `ErpMfgWorkcenter.capacity` 标量保留不删不依赖，CRP 一律用新增 `ErpMfgWorkcenterCapacity` 子实体（符合反模式警示）。**替代**：迁移标量到子实体（存量数据迁移 + 兼容性风险，归 Non-Goal）。**残留风险**：两套产能字段并存（文档注明 CRP 仅用子实体）。

## Infrastructure And Config Prereqs

- **ORM 模型变更（ask-first 保护区域）**：manufacturing ORM 新增 3 实体（WorkcenterCalendar/WorkcenterCapacity/CrpLoad）+ 字典（`erp-mfg/shift-type`、`erp-mfg/work-date-pattern`）。**新增不改动既有表**（additive，低风险）。变更后经 `nop-cli`/Maven codegen 生成 dao/entity/meta/service/web。
- 配置项：`erp-mfg.crp-overload-threshold`（默认 1.0），经 `AppConfig.var(..., defaultValue)`，无 .env。
- 无数据迁移；无新增端口/密钥/外部服务；无新增模块依赖（manufacturing 域内只读）。

## Execution Plan

### Phase 1 — 产能建模 ORM 新增（日历/班次 + 按产品产能）+ codegen

Status: completed
Targets: `module-manufacturing/model/app-erp-manufacturing.orm.xml`(扩)、`module-manufacturing/erp-mfg-codegen/`(codegen 入口)、生成的 `erp-mfg-dao`/`erp-mfg-meta`/`erp-mfg-service`/`erp-mfg-web` 新实体产物
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 既有 manufacturing ORM（Workcenter/WorkOrder/RoutingOperation）。

- [x] `Add`：manufacturing ORM 新增 `ErpMfgWorkcenterCalendar`（workcenterId R→Workcenter；calendarName；shiftType dict `erp-mfg/shift-type` ONE_SHIFT/MORNING/AFTERNOON/NIGHT；workDatePattern；startTime/endTime；effectiveFrom/effectiveTo；标准审计字段）。
  - Skill: `nop-backend-dev`
- [x] `Add`：manufacturing ORM 新增 `ErpMfgWorkcenterCapacity`（workcenterId R→Workcenter；materialId R→Material；capacityPerHour DECIMAL；setupTime DECIMAL；cleanupTime DECIMAL；efficiencyFactor DECIMAL 默认 1.0；标准审计字段）。
  - Skill: `nop-backend-dev`
- [x] `Add`：manufacturing ORM 新增 `ErpMfgCrpLoad`（workcenterId R；workOrderId 弱指针 R→WorkOrder；loadDate DATE；loadHours DECIMAL；setupHours DECIMAL；标准审计字段）+ 字典 `erp-mfg/shift-type`、`erp-mfg/work-date-pattern`。
  - Skill: `nop-backend-dev`
- [x] `Decision`：负荷来源 fallback（WorkOrder 计划日期）+ 负荷桶粒度（日）+ 标量 capacity 保留不依赖，见 Task Route Decision。
  - Skill: none
- [x] `Add`：运行 codegen 生成三实体 dao/entity/meta/service/web 骨架（CrudBizModel 默认）。
  - Skill: none

Exit Criteria:

> Phase 1 交付产能建模 + 生成的 CRUD 骨架，解除 Phase 2 负荷计算（依赖 CrpLoad 实体）与 WorkcenterCapacity/Calendar 读取基线。

- [x] 三实体 + 字典写入 manufacturing ORM，codegen 产物存在且可编译（manufacturing 模块类型检查/编译通过）

### Phase 2 — CRP 负荷计算引擎 + 负荷报表查询 + 超负荷告警 + 测试

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../entity/ErpMfgCrpLoadBizModel.java`(扩)、`IErpMfgCrpBiz.java`(新)、`CrpLoadCalculator.java`(新)、`ErpMfgErrors.java`(扩)、`ErpMfgConstants.java`(扩)、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（CrpLoad/WorkcenterCapacity/WorkcenterCalendar 实体 + codegen）。

- [x] `Add`：`IErpMfgCrpBiz.calculateLoad(periodFrom, periodTo, workcenterIds?)`（@BizMutation）——`CrpLoadCalculator` 扫描 WorkOrder（plannedDate 落区间、非 CANCELLED）经 RoutingOperation 分派 workcenterId，按 workcenter×loadDate 聚合 loadHours+setupHours；重算前清区间 CrpLoad 快照再写新行。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpMfgCrpBiz.getLoadReport(periodFrom, periodTo, workcenterIds?)`（@BizQuery）——返回 workcenter×date 聚合：loadHours（Σ CrpLoad）/ capacityHours（WorkcenterCalendar 可用工时 × efficiencyFactor）/ loadRate / overloaded（loadRate > `erp-mfg.crp-overload-threshold`）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：`TestErpMfgCrpLoad`（单工单→工作中心×日期负荷聚合 + 日历可用工时 + 效率折算 capacityHours + loadRate；超负荷 loadRate>1.0 标 overloaded；多工单同工作中心同日累加；重算清旧快照；空 WorkOrder capacityHours 非零 loadHours 零 loadRate=0；阈值门控）。`mvn test -pl module-manufacturing/erp-mfg-service -am -Dtest=TestErpMfgCrpLoad*`。
  - Skill: `nop-testing`

Exit Criteria:

> Phase 2 交付负荷计算 + 报表查询 + 超负荷告警。完整仓库验证属 Closure Gates。

- [x] 负荷计算（workcenter×date 聚合 + 可用工时 + 效率折算 + loadRate + 重算清旧快照）+ 报表查询 + 超负荷告警单测通过

### Phase 3 — 文档/日志 + 路线图标注

Status: completed
Targets: `docs/logs/2026/{执行当日 month-day}.md`、`docs/backlog/extended-roadmap.md`、`docs/design/manufacturing/crp.md`(偏离补注)
Skill: none

- Item Types: `Add`
- Prereqs: Phase 2 完成。

- [x] `Add`：`docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` 工作项 2.8 标注 done；`crp.md` 偏离（APS 负荷来源 fallback + maintenance 停机扣减 Non-Goal + 标量 capacity 保留）补注。
  - Skill: none

Exit Criteria:

- [x] 日志/路线图/设计文档偏离补注已落地

## Draft Review Record

- Independent draft review iteration 1: **acceptable as-is**（`ses_0d8be9a35ffe11FqZKFSnOp4BA`，独立 general 子代理）。6 项核心基线声明全部经实时仓库核实（Workcenter 标量 capacity 反模式 :332、3 个 CRP 实体不存在、WorkOrder plannedDate :366-367、WorkcenterBizModel 空 CRUD 壳、零 CRP service 代码、只读无跨域写）。规则 4/7/8/9/10/11/14 + 反松弛 + 执行时规则 7 均 PASS，无 BLOCKER。3 项 nit（非阻塞）：(N1) 基线 workcenterId 来源误标（WorkOrderLine 无 workcenterId；:784 实为 JobCard、:857 实为 CostVariance；RoutingOperation 实为 :305）；(N2)「仅有 code/name+capacity+capacityUnit」不精确（实体另有 hourlyRate/workHoursPerDay/isExternal）；(N3) `crp-run-schedule` cron 既非 Goal 亦未裁决为 Non-Goal。**已修订**：基线 workcenterId 来源更正为 RoutingOperation(:305) + 注明 WorkOrderLine 无该列；「仅有」改为列出既有字段并聚焦反模式于标量 capacity；新增 `crp-run-schedule` cron Non-Goal（on-demand calculateLoad 入口本期可被周期调度复用）。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成：产能四要素建模（日历/按产品产能/负荷快照）+ CRP 负荷计算（workcenter×date 聚合 + 可用工时 + 效率折算 + loadRate）+ 报表查询 + 超负荷告警，行为测试通过
- [x] 相关文档对齐：`extended-roadmap.md` 2.8 done；当日日志已记；`crp.md` Non-Goal 偏离补注
- [x] 已运行验证：`mvn test -pl module-manufacturing/erp-mfg-service -am`；根 `mvn clean install -DskipTests`
- [x] 无范围内项目静默降级（APS 排产/maintenance 停机扣减/标量迁移/可视化页面 均为计划内 Non-Goal）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### APS OperationOrder 排程时间作为负荷来源

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: APS 模块（M3 3.10/3.11）未落地，CRP 本期 fallback 为 WorkOrder 计划日期；APS 落地后负荷来源可切换为精确排程时间。
- Successor Required: yes（触发条件：APS 模块落地时）
- **Status: resolved**（plan `2026-07-05-0306-2-crp-load-source-aps-operationorder.md` 已落地 SPI `IErpApsLoadSourceProvider` + config `erp-mfg.crp-load-source` 双源门控）。

### maintenance 停机扣减工作中心可用时段

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 事件驱动，需 maintenance 停机事件机制（当前 maintenance 计划停机通知制造为 Non-Goal）；CRP 日历可用工时本期不扣减停机。
- Successor Required: yes（触发条件：maintenance 停机事件 + 排产停机窗口联动需求时）

### CRP 负荷可视化页面（AMIS 甘特/热力图）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期交付 GraphQL 负荷报表查询；AMIS 可视化为独立前端面。
- Successor Required: yes（触发条件：CRP 报表前端需求时）

## Closure

Status Note: 三个 Phase 均已完成并通过本地验证（mfg-service 43 tests / 0 Failures，含新增 7 个 CRP 行为测试；根 `mvn clean install -DskipTests` BUILD SUCCESS 146 模块）。范围内行为（产能四要素建模 + CRP 负荷计算 workcenter×date 聚合 + 负荷报表 + 超负荷告警）由 `TestErpMfgCrpLoad` 7 个行为测试覆盖；所有 Non-Goal（APS 排产/maintenance 停机扣减/标量 capacity 迁移/班次级粒度/AMIS 可视化/cron 定时）已在计划 Deferred + crp.md 显式记录。独立结束审计（新会话子代理 `ses_0d830716affe1Sl8W32GgS2ZUq`，冷重播无执行者上下文）已通过（PASS，无 BLOCKER，3 个非阻塞 nit：efficiencyFactor defaultValue="1" 非阻塞数值等价、capacity=0 哨兵分支为合理增强、codegen CRUD 页面非可视化 Non-Goal 对象）。Plan Status 置 `completed`。

Closure Audit Evidence:

- Auditor: independent general subagent（新会话 `ses_0d830716affe1Sl8W32GgS2ZUq`，无执行者上下文，冷重播审计）。
- Evidence: 逐项核对 Phase 1/2/3 交付物与实时代码——`ErpMfgWorkcenterCalendar`（orm.xml:359）/`ErpMfgWorkcenterCapacity`（:389，efficiencyFactor 默认1）/`ErpMfgCrpLoad`（:418）三实体 + `erp-mfg/shift-type`（:118）/`erp-mfg/work-date-pattern`（:124）字典；codegen 全套（dao `_gen`、IBiz、BizModel、meta、web）；`IErpMfgCrpBiz`（calculateLoad @BizMutation/getLoadReport @BizQuery，workcenterIds @Optional，IServiceContext 末参）+ `IErpMfgCrpLoadBiz extends ICrudBiz, IErpMfgCrpBiz`；`CrpLoadReportItem`（@DataBean 8 字段）；`CrpLoadCalculator`（clearExisting 重算清旧、WorkOrder ne CANCELLED + planned 日期落区间经 RoutingOperation 分派、standardTime→loadHours + setupTime→setupHours 首日、capacityHours=calendar×efficiencyFactor、loadRate=load/cap、overloaded>threshold AppConfig.var）；`ErpMfgCrpLoadBizModel` 委托 + 末参；`ErpMfgErrors.ERR_CRP_PERIOD_INVALID` + `ErpMfgConstants` SHIFT_TYPE/WORK_DATE_PATTERN/CONFIG_CRP_OVERLOAD_THRESHOLD；beans.xml 注册 CrpLoadCalculator；`TestErpMfgCrpLoad` 7 tests 全覆盖。Nop 约定达标（包级 @Inject、@BizMutation/@BizQuery、NopException+ErrorCode、IDaoProvider 服务助手范式对齐 MrpEngine、IErpMfgCrpLoadBiz 为可编辑保留层）。文档对齐（07-03.md 全绿条目、extended-roadmap.md 2.8 ✅、crp.md 偏离补注完整）。所有 Non-Goal 在计划 Deferred + crp.md 显式记录。文本一致性成立。无 BLOCKER。
- Verdict: **PASS**（独立审计门控已置 `[x]`）。

Follow-up:

- APS OperationOrder 排程时间作为负荷来源（见上方 Deferred）
- maintenance 停机扣减工作中心可用时段（见上方 Deferred）
- CRP 负荷可视化页面（AMIS 甘特/热力图）（见上方 Deferred）
- CRP 定时运行 cron（见上方 Deferred）
- 标量 Workcenter.capacity 存量数据迁移（见上方 Deferred）
