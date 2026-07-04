# 2026-07-05-0306-1-scheduler-wire-periodic-jobs 接线 bootstrap 运营定时作业到 nop-job 调度器

> Plan Status: completed
> Last Reviewed: 2026-07-05
> Source: `docs/architecture/job-scheduling.md`（权威作业目录，由 `2026-07-04-1600-1` 重写）§8 "零作业注册" + 约 20 个域计划 Deferred 段（"nop-job 接线时"）
> Related: `2026-07-04-1600-1-batch-scheduling-architecture.md`（批处理架构权威化，本计划是其显式接线后继）、`2026-07-05-0115-1-finance-ar-ap-auto-reconciliation.md`（唯一已接线作业 `ErpFinAutoReconJob` 参考实现）
> Audit: required

## Current Baseline

实时仓库逐项核实（`grep`/`read`，非采信旧记忆）：

- **调度框架已就绪、作业注册几乎为零**：`nop-job-local` 已接入 `app-erp-all/pom.xml`（`BeanMethodJobInvoker`，本地反射，无 RPC，证据 `docs/architecture/job-scheduling.md:73`）。`app-erp-all/src/main/resources/_vfs/nop/job/conf/scheduler.yaml` 仅 **1 条**作业注册（`erp-fin-ar-ap-auto-recon`，由计划 `0115-1` Phase 2 落地）。`job-scheduling.md:74` 自述"无任何 job bean 实现"——该陈述早于 `0115-1`；当前全仓真实 job bean 仅 `module-finance/erp-fin-service/.../job/ErpFinAutoReconJob.java` 一个（`rg "class Erp.*Job"` 其余命中均为制造域 `ErpMfgJobCard` 工单卡片，非调度作业）。
- **参考接线模式已验证（三件套）**：(1) job bean 类（无参 `execute()`，`@Inject` 域 `I*Biz`，`AppConfig.var` 读 cron 配置空值跳过）；(2) 域 `app-service.beans.xml` 显式 `<bean id="erpFinAutoReconJob" class="..."/>`（证据 `module-finance/erp-fin-service/.../beans/app-service.beans.xml:74-75`）；(3) `app-erp-all/.../scheduler.yaml` 条目（`jobName`/`trigger.cronExpr`/`invoker.bean`+`method`，证据 `scheduler.yaml:3-10`）。
- **目标作业的 BizModel 入口方法均已交付**（可被 GraphQL/测试调用），仅缺 job bean + scheduler 注册。逐方法核实签名（`rg` 实时仓库）：
  | 作业 | 入口方法（已交付） | 实证 file:line | 入参派生方式 |
  |------|-------------------|----------------|--------------|
  | `erp-ast-depreciation` | `IErpAstDepreciationScheduleBiz.executeBatchDepreciation(String period, ctx)` | `ErpAstDepreciationScheduleBizModel.java:45` | period=当前月（YYYY-MM） |
  | `erp-mnt-due-visit-generation` | `IErpMntScheduleBiz.generateDueVisits(LocalDate asOfDate, ctx)` | `ErpMntScheduleBizModel.java:26` | asOfDate=今天 |
  | `erp-cs-sla-scan` | `IErpCsTicketBiz.scanOverdueTickets(ctx)` | `ErpCsTicketBizModel.java:230` | 无参（全局扫描+升级为副作用） |
  | `erp-fin-cash-forecast-refresh` | `IErpFinCashForecastBiz.refreshForecast(fromDate, toDate, …)` | `ErpFinCashForecastBizModel.java:45` | 窗口=config-gated 预测期限 |
  | `erp-mfg-crp-run` | `IErpMfgCrpBiz.calculateLoad(periodFrom, periodTo, workcenterIds)` | `ErpMfgCrpLoadBizModel.java:35` | period=当前月，workcenterIds=null（全工作中心） |
  | `erp-crm-forecast-recalc` | `IErpCrmForecastBiz.refreshForecast(Long periodId, ctx)` | `ErpCrmForecastBizModel.java:31` | periodId=查当前 OPEN 预测期间 |
  | `erp-crm-lead-scoring-recalc` | `IErpCrmLeadScoreBiz.recalculateScore(leadId, …)`（批量逐线索） | `ErpCrmLeadScoreBizModel.java:33` | 迭代 active 线索逐条调 |
- **配置键现状**：`job-scheduling.md:§6` 已登记部分 cron 键（如 `erp-ast.depreciation-cron`、`erp-cs.sla-scan-interval`），但多数域无显式 cron 键（如 mnt/crm-forecast/drp 等）。`ErpFinAutoReconJob` 模式为 in-bean 读 `AppConfig.var(cronKey, "")` 空值跳过——本计划沿用。
- **触发条件已满足**：上述 7 作业在各域计划 Deferred 段的触发条件为"nop-job wiring / 接线 nop-job 时 / 可选部署配置 / 入口已可复用"（`job-scheduling.md:§8` 汇总）。批处理架构计划 `1600-1` 已交付权威契约与判据，其 Deferred 后继触发条件为"各域 deferred 作业触发条件满足时"——本计划即该后继切片。

### 剩余差距

(1) 7 个目标作业零 job bean、零 scheduler 注册；(2) 多数目标作业无 cron 配置键；(3) `job-scheduling.md` §3 这些作业行标 WIRED/DEFERRED 与"实际未注册"不一致需收口；(4) owner doc"配置点"表缺新 cron 键回流。

## Goals

- 为 7 个已交付 BizModel 入口的运营定时作业各落地三件套（job bean + 域 `app-service.beans.xml` `<bean>` 注册 + `app-erp-all` `scheduler.yaml` 条目），使其可被 nop-job-local 按时反射调用。
- 每作业一个 cron 配置键（复用 `job-scheduling.md:§6` 已有键；缺则新增），in-bean 读 `AppConfig.var` 空值跳过（与 `ErpFinAutoReconJob` 一致），实现"已接线、默认不自动执行、运维按部署启用"。
- 每作业一个行为测试（JunitAutoTestCase）证明：cron 空值跳过 + 非空调用委托到 BizModel（用 spy/计数或快照断言委托发生）。
- 收口 `job-scheduling.md` §3 对应作业行状态为 WIRED（scheduler-registered 语义）并回流 owner doc 配置点。

## Non-Goals

- **新增任何业务逻辑**：仅接线已交付的副作用入口；job bean 内只做入参派生（日期/期间/迭代查询）+ cron 门控 + 委托 + 日志 + 失败隔离 try/catch。
- **nop-batch 分 chunk 迁移**：归各作业 `1600-1 §7` batch-candidate 触发条件后继（数据量 ≥ 数万时）。
- **启用任何 cron**（默认全空=跳过）；启用属部署决策。
- **通知/告警通道对接**：`erp-crm-event-reminder`（`findDueReminders` 仅为查询、无派发副作用）、`erp-cs-csat-reminder`（`findSurveyReminders`/`findExpiredSurveys` 仅为查询）需通知派发通道，归 Deferred。
- **需新 BizModel 方法的作业**：`erp-fin-posting-scan`（无独立扫描方法）、`erp-sal-quotation-expiry`（待实现）、`erp-fin-credit-facility-interest`（仅手动）归 Deferred。
- **需选择语义 Decision 的作业**：`erp-pur-supplier-scorecard`（finalizeScorecard 需"定稿哪些评分卡"业务决策）、`erp-drp-run`（runDrp 需"运行哪些计划"决策）归 Deferred。
- **敏感自动执行**：`erp-hr-payroll-calc`（自动核算+过账工资，风险等级不同）归 Deferred。
- **外部基础设施相关**：`erp-log-tracking-poll`（真实承运商）、B2B SFTP/ASN/证书、合同电子签章轮询、银行文件解析 归 Deferred。
- **scheduler.yaml 迁移 nop-job-service 分布式**（归 `1600-1` D3 触发条件）。

## Task Route

- Type: `implementation-only change`（接线既有 BizModel 入口，不改 ORM/契约/业务语义）+ 少量 `app-layer design change`（owner doc 配置点回流）。
- Owner Docs: `docs/architecture/job-scheduling.md`（§3 状态收口 + §6 配置键）、各域 owner doc"配置点"表（assets/maintenance/cs/finance/crm 关键作业）、`docs/design/manufacturing/crp.md`（crp-run 不改业务，仅注册）。
- Skill Selection Basis: job bean（`@Inject` I*Biz + `AppConfig.var` cron 门控 + `IServiceContext`）、beans.xml 注册、scheduler.yaml、ErrorCode、配置门控、JunitAutoTestCase——匹配 `nop-backend-dev`。前端/ORM 不涉及。
- **Decision（默认不启用 cron 的双层门控模式）**：**选择**沿用 `ErpFinAutoReconJob` 双层模式——`scheduler.yaml` 写设计 cronExpr（决定"何时触发"），job bean 内 `AppConfig.var(cronKey, "")` 空值跳过（决定"是否真正执行"），配置键默认空字符串。**替代**：① scheduler.yaml cronExpr 留空、仅靠它门控——无法在"已注册可被发现"与"默认不执行"间兼顾，且与已落地参考实现不一致，rejected；② 配置键默认填设计 cron（默认即自动执行）——bootstrap 阶段意外触发运营作业（折旧/核销类）风险高，rejected。**残留风险**：运维须知道"设 cron 配置键才启用"——以 owner doc 配置点表 + job-scheduling.md §3 备注 + 当日日志记录缓解。
- **Decision（crp-run 计算窗口默认值）**：**选择**默认计算"当前自然月"（periodFrom=月初、periodTo=月末、workcenterIds=null 全工作中心），config-gated `erp-mfg.crp-run-default-window-months`（默认 0=当月）。**替代**：前向 N 月——无明确运营约定，rejected。**残留风险**：CRP 负荷来源当前为 WorkOrder 计划日期（fallback，无 APS）；APS OperationOrder 接线为负荷来源由计划 `2026-07-05-0306-2` 承接，不影响本作业注册。
- **Decision（crm-forecast-recalc 期间选择 + lead-scoring 批量范围）**：**选择** forecast：查 `status=OPEN` 且今天 ∈ [startDate,endDate] 的预测期间，无则 info 日志跳过；lead-scoring：迭代 `status` 为 active 类（非 CONVERTED/LOST/CANCELLED 终态）线索逐条 `recalculateScore`，单线索失败 try/catch 隔离不阻断。**替代**：硬编码期间/全量线索含终态——会重算终态线索无意义或找不到期间报错，rejected。**残留风险**：迭代量大时无断点续跑（nop-batch 迁移归 Deferred）。

## Infrastructure And Config Prereqs

- 无新增端口/密钥/.env/外部服务/数据迁移。
- 依赖 `nop-job-local` 已接入 `app-erp-all`（基线已就绪，`job-scheduling.md:73`）。
- 依赖各目标作业 BizModel 入口已交付（见 Current Baseline 表，已逐项核实）。
- 回滚策略：job bean/beans.xml/scheduler.yaml 条目均为新增可逆；移除 `scheduler.yaml` 条目即停调度，无数据迁移。

## Execution Plan

### Phase 1 - 7 个运营作业 job bean + beans.xml 注册

Status: completed
Targets: 各域 service 模块 `.../service/job/ErpXxxJob.java` + 各域 `_vfs/erp/<domain>/beans/app-service.beans.xml`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: 无（基线 BizModel 入口已就绪）

- [x] `Add`：`ErpAstDepreciationJob`（erp-ast-service/.../job/）—— `execute()` 读 `erp-ast.depreciation-cron` 空值跳过；非空派生当前月 period（`YearMonth.now()` → "YYYY-MM"），调 `IErpAstDepreciationScheduleBiz.executeBatchDepreciation(period, ctx)`，记录处理数；try/catch 失败隔离。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpMntDueVisitJob`（erp-mnt-service/.../job/）—— 读 `erp-mnt.due-visit-cron` 空值跳过；非空调 `IErpMntScheduleBiz.generateDueVisits(LocalDate.now(), ctx)`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpCsSlaScanJob`（erp-cs-service/.../job/）—— 读 `erp-cs.sla-scan-cron` 空值跳过；非空调 `IErpCsTicketBiz.scanOverdueTickets(ctx)`，记录升级数。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpFinCashForecastJob`（erp-fin-service/.../job/）—— 读 `erp-fin.cash-forecast-cron` 空值跳过；非空按 config-gated 预测期限派生 fromDate/toDate，调 `IErpFinCashForecastBiz.refreshForecast(...)`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpMfgCrpRunJob`（erp-mfg-service/.../job/）—— 读 `erp-mfg.crp-run-cron` 空值跳过；非空按 Decision 派生当月区间，调 `IErpMfgCrpBiz.calculateLoad(periodFrom, periodTo, null, ctx)`，记录写入行数。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpCrmForecastRecalcJob`（erp-crm-service/.../job/）—— 读 `erp-crm.forecast.recalc-cron` 空值跳过；非空查当前 OPEN 预测期间，无则跳过，有则调 `IErpCrmForecastBiz.refreshForecast(periodId, ctx)`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpCrmLeadScoringRecalcJob`（erp-crm-service/.../job/）—— 读 `erp-crm.lead-scoring.schedule-cron` 空值跳过；非空迭代 active 线索逐条 `IErpCrmLeadScoreBiz.recalculateScore(leadId, triggerEvent, ctx)`（triggerEvent 经 `ErpCrmConstants` 常量派生，避免魔法串），单线索 try/catch 隔离。
  - Skill: `nop-backend-dev`
- [x] `Add`：各域 `app-service.beans.xml` 增 `<bean id="erpXxxJob" class="..."/>`（仿 `app-service.beans.xml:74-75` 范式）。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 1 交付 7 个 job bean 类与 beans.xml 注册。解除 Phase 2 scheduler 注册的阻塞（bean 须先可解析）。

- [x] 7 个 `ErpXxxJob` 类存在，各含非空 `execute()` 体（cron 门控 + 委托 + 日志 + 失败隔离），无 `return null` 占位/空 `{}`/吞异常空壳
- [x] 7 个 `<bean id=.../>` 在各域 `app-service.beans.xml` 注册（`rg` 可查）

### Phase 2 - scheduler.yaml 注册 + cron 配置键

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/nop/job/conf/scheduler.yaml`、各域 `ErpXxxConstants`（配置键常量）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1（job bean 可解析）

- [x] `Add`：`scheduler.yaml` 增 7 条作业（`jobName`/`displayName`/`description`/`trigger.cronExpr`=设计 cron/`invoker.bean`+`method: execute`），仿 `scheduler.yaml:3-10`。
  - Skill: `nop-backend-dev`
- [x] `Add`：各域 `ErpXxxConstants` 增 cron 配置键常量（复用 §6 已有键如 `erp-ast.depreciation-cron`/`erp-cs.sla-scan-cron`；缺则新增 `erp-mnt.due-visit-cron`/`erp-mfg.crp-run-cron` 等），默认空字符串。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `scheduler.yaml` 含 7 条新作业（共 8 条含既有 ar-ap-auto-recon）；`rg` 各 `invoker.bean` 对应 `app-service.beans.xml` 已注册的 bean id
- [x] 各 cron 配置键在域 Constants 类声明且默认空（空值=跳过门控可工作）

### Phase 3 - 行为测试

Status: completed
Targets: 各域 `erp-<domain>-service/src/test/.../TestErpXxxJob.java`
Skill: `nop-backend-dev`

- Item Types: `Proof`
- Prereqs: Phase 2

- [x] `Proof`：每作业一个 `JunitAutoTestCase`，至少 2 case：(a) cron 配置空值 → `execute()` 跳过（不调用 BizModel，断言计数/快照不变）；(b) cron 非空 → 委托到 BizModel（断言副作用或委托计数）。对迭代型（lead-scoring）补一 case 验证单线索失败隔离不阻断。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 7 个测试类存在，每个 ≥2 case 全绿；本地 `mvn test -pl <domain>/erp-<domain>-service -am` 通过

### Phase 4 - 文档收口与配置点回流

Status: completed
Targets: `docs/architecture/job-scheduling.md`、各域 owner doc"配置点"表、当日日志
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-3

- [x] `Add`：`job-scheduling.md` §3 对应 7 作业行状态改为 WIRED（scheduler-registered 语义，与"仅 BizModel callable"区分），并在文档顶部状态取值定义处澄清 WIRED 双义（补 `SCHEDULED` 或在 WIRED 备注列注明"已 scheduler 注册"）；§6 补新 cron 键。
  - Skill: none
- [x] `Add`：各域 owner doc"配置点"表回流新 cron 键与设计默认值（assets/maintenance/cs/finance/crm/manufacturing）。
  - Skill: none

Exit Criteria:

- [x] `job-scheduling.md` §3 7 作业行状态一致（WIRED/scheduler-registered）；§6 含全部新 cron 键；owner doc 配置点表可查

## Draft Review Record

- Independent draft review iteration 1: accept（ses_0d173c1e3ffeekgLbnKwe3OfOe，general 独立子代理新会话）— 无 BLOCKER。逐项实时仓库核实：scheduler.yaml 仅 1 作业（erp-fin-ar-ap-auto-recon）属实；ErpFinAutoReconJob 三件套接线模式（job bean + app-service.beans.xml:74-75 `<bean>` + scheduler.yaml:3-10 + in-bean `AppConfig.var` 空值跳过门控）属实；7 个目标入口方法均经 file:line 核实为真实副作用入口（非 query-only）；排除项正确（findDueReminders/findSurveyReminders 为 @BizQuery 查询、posting-scan/quotation-expiry 无方法、scorecard/drp 需选择决策、payroll 敏感）；前置 1600-1 completed 合法后继。14 项最低规则全过、反松弛无违例、模板完整、owner-doc 行合规省略、单一结果表面（规则 14 不碎片化）。已采纳非阻塞 minor：lead-scoring triggerEvent 经常量派生避免魔法串（已修订）。
- 后续 implementation 时可再清理：规则 7 per-item 类型前缀与阶段级声明冗余（非正确性问题，可选）。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成（7 作业三件套接线 + 配置门控 + 测试）
- [x] 相关文档对齐（`job-scheduling.md` §3/§6 收口；owner doc 配置点回流；当日日志）
- [x] 已运行验证：根 `mvn clean install -DskipTests` + 受影响各域 `mvn test -pl <domain>/erp-<domain>-service -am`
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 需通知派发通道的提醒类作业（crm-event-reminder / cs-csat-reminder）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `findDueReminders`/`findSurveyReminders`/`findExpiredSurveys` 仅为查询方法，无"发送提醒"副作用；提醒派发需通知通道（站内消息/邮件/webhook）。
- Successor Required: yes（触发条件：通知派发通道/告警 SLA 基础设施落地时）

### 需新 BizModel 方法的扫描类作业（posting-scan / quotation-expiry / credit-facility-interest）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 无独立可调度的副作用入口（posting-scan 由过账引擎 `post(PostingEvent)` 承载；quotation-expiry 入口待实现；credit-facility-interest 仅手动）。
- Successor Required: yes（触发条件：各域补齐扫描副作用入口时；posting-scan 量大优先评估 nop-batch）

### 需选择语义 Decision 的作业（supplier-scorecard / drp-run）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `finalizeScorecard(scorecardId)`/`runDrp(planId)` 需"定稿/运行哪些"的业务决策选择逻辑，超出"接线既有副作用入口"范围。
- Successor Required: yes（触发条件：周期评分/DRP 定时运行语义 Decision 落地时）

### 敏感/外部基础设施作业（hr-payroll-calc / log-tracking-poll / b2b-sftp / ct-signature-poll / bank-file-import）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 工资自动核算+过账风险等级不同；承运商/EDI/电子签章/银行文件依赖真实外部基础设施。
- Successor Required: yes（触发条件：薪酬自动核算需求 / 真实供应商接入 / 生产部署时）

### nop-batch 分 chunk 迁移

- Classification: `optimization candidate`
- Why Not Blocking Closure: 当前 nop-job-local 直调 BizModel 对小到中等数据量够用（`1600-1 §7` batch-candidate 触发条件）。
- Successor Required: yes（触发条件：单次处理量稳定 ≥ 数万时）

## Closure

Status Note: 完成。7 个运营作业（erp-ast-depreciation / erp-mnt-due-visit-generation / erp-cs-sla-scan / erp-fin-cash-forecast-refresh / erp-mfg-crp-run / erp-crm-forecast-recalc / erp-crm-lead-scoring-recalc）均落地 nop-job 三件套接线（job bean + 域 app-service.beans.xml `<bean>` + scheduler.yaml 条目），双层门控（设计 cronExpr + in-bean AppConfig.var 空值跳过，默认全空=不自动执行）。验证：根 `mvn clean install -DskipTests` BUILD SUCCESS（146 模块）；6 受影响域 `mvn test` 全绿无回归；7 个 job 行为测试 23 case 全绿。

Closure Audit Evidence:

- 独立结束审计（新会话，ses_0d1538c39ffeQT1FBSTRI61WWm）verdict：**PASS**，无 blocker。逐项实时仓库核实：7 个作业均为完整三件套（job bean 含 cron 门控+委托+try/catch 隔离；各域 app-service.beans.xml `<bean>` 注册；scheduler.yaml 条目 invoker.bean/method 匹配）；bean id 一致；7 个 cron 配置键均声明且默认空字符串（双层门控 intact，含窗口键 erp-fin.cash-forecast-window-days / erp-mfg.crp-run-default-window-months）；@Inject 字段非 private、execute() 无 @Transactional、跨实体访问走 I*Biz（IErpCrmForecastPeriodBiz.findFirst / IErpCrmLeadBiz.findList）、triggerEvent 经 ErpCrmConstants 常量派生无魔法串。7 个测试类（共 23 case）覆盖 cron 空值跳过 + cron 非空委托，lead-scoring 含单线索失败隔离 case。job-scheduling.md §3/§6/§8 与 owner docs（assets/cs/finance/crm/manufacturing/maintenance）对齐，当日日志存在，计划内部状态一致且两处独立审计门控经本次审计落地。范围与 Non-Goals 遵守。

Follow-up:

- 通知派发通道（见上方 Deferred）
- 需新方法/选择语义/敏感外部作业（见上方 Deferred）
- nop-batch 迁移（见上方 Deferred）
