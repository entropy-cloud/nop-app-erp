# 2026-07-06-0642-1-operational-notification-consumers 域运营事件通知消费者接线

> Plan Status: completed
> Last Reviewed: 2026-07-06
> Source: deferred 项承接（`2026-07-06-0504-1` 全量域消费者接线、`2026-07-05-0306-1` 需通知派发通道的提醒类作业、`2026-07-05-1838-2` 差异预警通知通道）；core-business-roadmap M5 业财可运维性通知闭环
> Related: `2026-07-06-0504-1-notification-dispatch-subsystem.md`（通道，已完成）、`2026-07-05-0306-1-scheduler-wire-periodic-jobs.md`（调度器，已完成）、`2026-07-05-1838-2-manufacturing-production-variance.md`（差异引擎，已完成）
> Audit: required

## Current Baseline

通知派发通道已就绪但**无任何域消费者接线**——`IErpSysNotificationBiz.notify(eventType, context)`（`module-notify/erp-notify-dao`）已落地，`notify` 为薄委派层（模板渲染→接收人解析→频控合并→站内落库→config-gated 外发），但全 ERP 无业务调用方。

逐项实时核实（消费者侧现状）：

- **CS SLA**：`ErpCsTicketBizModel.scanOverdueTickets`（`module-cs/.../entity/ErpCsTicketBizModel.java:231`）扫描超时工单仅建 `ESCALATE` 审计行；`findSlaWarnings`（:257）仅查询返回。源码注释明示「通知 escalationUserId（L1，config-gated；**通知占位，实际发送属 nop-notification 独立面**）」（:247）。
- **过账异常**：`ErpFinPostingExceptionRecorder`（`module-finance/.../posting/`）以 REQUIRES_NEW 独立事务落 PENDING 异常记录，无通知派发。M5 计划 `2026-07-04-1452-1` 的异常工作台仅落表，告警通知归本期。
- **销售信用**：`CreditLimitChecker.check`（`module-sales/.../entity/CreditLimitChecker.java:81`）SOFT_WARNING 路径超额度仅记告警放行，HARD_BLOCK 抛错；均无通知。SOFT_WARNING 恰是需提醒销售员跟进的场景。
- **CRM 活动提醒**：`ErpCrmEventBizModel.findDueReminders`（`module-crm/.../entity/ErpCrmEventBizModel.java:81`）仅返回到期事件列表，无副作用、无 scheduler job。`0306-1` 已将其显式 Deferred（触发条件=通知派发通道落地）。
- **CSAT 调查提醒**：`ErpCsSurveyBizModel.findSurveyReminders`（:119）/`findExpiredSurveys`（:132）仅查询，无 scheduler job。`0306-1` 已显式 Deferred。
- **生产差异**：`ProductionVarianceCalculator`（`module-manufacturing`，`1838-2` 交付）计算差异并 config-gated 过账，无阈值告警通知。`1838-2` Deferred「差异预警通知通道」触发条件=通知派发通道落地。
- **scheduler.yaml**（`app-erp-all/.../nop/job/conf/scheduler.yaml`）现 9 个 job，**无** `erp-crm-event-reminder` / `erp-cs-csat-reminder` 条目。
- **种子模板**：`_seed_erp-notify.sql`（mysql/oracle/postgresql 三套）仅 3 条样例（`cs.sla-overdue`=7101 / `fin.posting-exception`=7102 / `sal.credit-over-limit`=7103），证明三类频控可运行，非业务消费方。

剩余差距：6 个域运营事件（CS SLA 升级 / 过账异常 / 信用超限 / CRM 活动到期 / CSAT 调查到期 / 生产差异超阈值）到站内消息的派发链全部断开；2 个提醒类 scheduler job 未接线。

## Goals

- 将 6 个域运营事件接入 `IErpSysNotificationBiz.notify()` 派发链：调用方落 notify 调用点 + 接收人上下文 + config-gated 门控
- 接线 2 个提醒类 scheduler job（`erp-crm-event-reminder` / `erp-cs-csat-reminder`），完成 `0306-1` Deferred
- 补齐 3 类新种子模板（`crm.event-reminder` / `cs.csat-reminder` / `mfg.production-variance`），复用既有 3 类
- 解除 `0306-1`、`1838-2`、`0504-1` 三处 Deferred（通知派发通道基础设施落地后触发条件已满足的承接项）

## Non-Goals

- 审批工作流通知（wf 任务分配/结果通知 + CC 抄送步骤）——独立结果面 `2026-07-06-0642-2-approval-workflow-notifications.md` 承接（集成模式不同：wf listener/BizObject 反射，非 BizModel @Inject）
- 合同电子签章超时通知（触发条件=生产部署，未满足，`2200-2` Deferred 保持）
- HR 排班变更通知（无明确 Deferred 记录，软触发，本期不纳入）
- 通知中心前端页面 / 接收人偏好 UI / 模板编辑器（AMIS 定制面，`0504-1` Deferred）
- webhook 通道 / nop-message 异步总线（`0504-1` Deferred，触发条件未满足）
- 基于角色的动态组织层级精确路由（依赖角色基础设施，`0504-1`/`0315-1` Deferred）
- 各域业务报表渲染（`0504-2` Deferred，渲染能力已就绪，按需各域接线）

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/notification-strategy.md`（通知类型/频控/通道）、各域 owner docs（CS `customer-service/sla.md`、Finance `finance/posting-log.md`、Sales `sales/README.md`、CRM `crm/README.md`、Manufacturing `manufacturing/state-machine.md`）、`docs/architecture/job-scheduling.md`（scheduler 三件套）
- Skill Selection Basis: 后端 BizModel/job 改动 + 跨实体 `IErpSysNotificationBiz` 注入 + scheduler 三件套接线 → 匹配 `nop-backend-dev`（决策门/xbiz/实体服务/跨实体调用/事务边界）；改动后须跑 `mvn test`，匹配 `nop-testing`（行为测试）。前端零改动，不加载 `nop-frontend-dev`。

## Infrastructure And Config Prereqs

- 通道侧 `erp-notify.email-enabled`/`sms-enabled` 默认 false（bootstrap 仅站内消息）——本期消费者不依赖外发供应商，无新基础设施
- 接收人解析：种子模板用 ROLE resolver（角色名→`nop-auth` `NopAuthUserRole`），角色基础设施未完全落地时 resolver 静默返回空并 WARN（不阻断业务）——本计划沿用既有 config-gated 语义，不改 resolver
- 新增 config 键（各消费者门控，默认值见 Execution Plan）：经各域 `Erp*Configs`/`AppConfig.var`，默认开启站内派发、空值跳过

## Execution Plan

### Phase 1 — 既有种子模板消费者接线（CS SLA / 过账异常 / 销售信用）

Status: completed
Targets: `module-cs/erp-cs-service/.../entity/ErpCsTicketBizModel.java`、`module-finance/erp-fin-service/.../posting/ErpFinPostingExceptionRecorder.java`、`module-sales/erp-sal-service/.../entity/CreditLimitChecker.java`、各域 `erp-*-service/pom.xml`（新增 `erp-notify-dao` compile 依赖）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 通道（`0504-1` 已完成）

- [x] Decision: 跨域访问方式——各消费者域 `@Inject IErpSysNotificationBiz`（compile 依赖 `erp-notify-dao`），与既有 `purchase→inventory-dao`/`sales→finance-dao` 单向无环范式一致。`erp-notify-dao` 是干净叶子模块（仅依赖 parent+codegen），新增依赖不引入环。记录于计划并核实 pom 无环。
  - Skill: `nop-backend-dev`
- [x] Add: CS——`scanOverdueTickets` 升级时 + `findSlaWarnings` 预警时调 `notify("cs.sla-overdue", {ticketId, ticketCode, customerName, escalationUserId, deadlineDateTime})`，config-gated（`erp-cs.sla-notify-enabled` 默认 true）。接收人优先 escalationUserId（USER_LIST），回退模板 ROLE。复用已接线的 `erp-cs-sla-scan` scheduler job（scheduler.yaml:30，无需新增 SLA job）——该 job 调 `scanOverdueTickets`，notify 调用点就位后既有 cron 自动派发 SLA 通知。
  - Skill: `nop-backend-dev`
- [x] Add: Finance——在 `ErpFinPostingExceptionRecorder` 内（REQUIRES_NEW 子事务已提交的 lambda 内，使 `txn().afterCommit` 在内层已提交事务上生效，避免外层正在回滚的主过账事务吞掉通知）落 PENDING 后调 `notify("fin.posting-exception", {exceptionId, billHeadCode, businessType, postingType, errorCode, errorMessage, failedStage, voucherDate})`。注意 Recorder 现签名返回 void 且无 `amount`/`exceptionId` 入参——`exceptionId` 取落库后实体主键，金额从 `eventData` 派生（若有），无金额则模板不渲染该字段。config-gated（`erp-fin.posting-exception-notify-enabled` 默认 true）。
  - Skill: `nop-backend-dev`
- [x] Add: Sales——`CreditLimitChecker.check` SOFT_WARNING 超额度放行后调 `notify("sal.credit-over-limit", {customerId, customerName, orderNo, orderAmount, creditLimit, overAmount})`，config-gated（`erp-sal.credit-notify-enabled` 默认 true）。HARD_BLOCK 路径已抛错，不通知（拒绝即明确反馈）。
  - Skill: `nop-backend-dev`
- [x] Proof: 各域新增/扩展行为测试——断言 `notify` 被调 + `ErpSysNotification` 行落入（recipient 匹配）+ config 关闭时跳过。CS：扩展 `TestErpCsTicketSlaCsat`；Finance：扩展过账异常测试；Sales：扩展 `TestErpSalOrderApproval`。命令 `mvn test -pl module-cs/erp-cs-service,module-finance/erp-fin-service,module-sales/erp-sal-service -am`。
  - Skill: `nop-testing`

Exit Criteria:

- [x] CS/Finance/Sales 三消费者在触发场景下产生 `ErpSysNotification` 站内消息行，config 关闭路径静默跳过（可观察：测试断言 notify 调用次数 + 落库行）
- [x] 三域 `erp-notify-dao` 依赖引入后 `mvn test -pl <三域 service> -am` 全绿，无环依赖编译错误

### Phase 2 — 提醒类 scheduler job 接线（CRM 活动 / CSAT 调查）

Status: completed
Targets: 新增 `module-crm/erp-crm-service/.../job/ErpCrmEventReminderJob.java`、`module-cs/erp-cs-service/.../job/ErpCsCsatReminderJob.java`；`app-erp-all/.../nop/job/conf/scheduler.yaml`；各域 `app-service.beans.xml`；种子模板 SQL（mysql/oracle/postgresql）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1（notify 调用模式确立）；`0306-1` scheduler 三件套范式

- [x] Add: CRM 活动 reminder job——`ErpCrmEventReminderJob.execute()` 双层门控（`erp-crm.event-reminder-cron` 空=不调度）→ 调 `IErpCrmEventBiz.findDueReminders(windowMinutes)` → 逐事件 `notify("crm.event-reminder", {eventId, title, ownerUserId, dueTime, leadName})`，接收人=ownerUserId（USER_LIST）。复用 `0306-1` job bean 范式（cron 门控+委托+try/catch 单条隔离）。
  - Skill: `nop-backend-dev`
- [x] Add: CSAT 调查 reminder job——`ErpCsCsatReminderJob.execute()` 双层门控（`erp-cs.csat-reminder-cron`）→ 调 `findSurveyReminders` + `findExpiredSurveys` → `notify("cs.csat-reminder", {surveyId, ticketCode, customerName, state})`，接收人=客服 agent（ROLE 或指派 userId）。
  - Skill: `nop-backend-dev`
- [x] Add: scheduler.yaml 新增 2 条 job（jobName/trigger.cronExpr/invoker.bean+method），设计默认 cron；`app-service.beans.xml` 各注册一个 `<bean>`。cron 配置键双层门控（设计值 + in-bean 空值跳过）。
  - Skill: `nop-backend-dev`
- [x] Add: 种子模板 SQL 三套（mysql/oracle/postgresql）新增 `crm.event-reminder`（业务提醒，5 分钟合并）+ `cs.csat-reminder`（业务提醒，5 分钟合并），接收人 resolver=USER_LIST（owner/agent），与既有 3 条同表同结构。
  - Skill: `nop-backend-dev`
- [x] Proof: 2 个 job 行为测试（cron 空值跳过 + cron 非空委托 + notify 调用断言），复用 `TestErpCsSlaScanJob`/`TestErpCrmForecastRecalcJob` 范式。命令 `mvn test -pl module-crm/erp-crm-service,module-cs/erp-cs-service -am`。
  - Skill: `nop-testing`

Exit Criteria:

- [x] `erp-crm-event-reminder` / `erp-cs-csat-reminder` 完整三件套（job bean + beans.xml `<bean>` + scheduler.yaml 条目），cron 双层门控 intact
- [x] 2 个新种子模板落三套 SQL，job 触发后产生对应 `ErpSysNotification` 行（测试断言）

### Phase 3 — 生产差异阈值告警接线

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../`（`ProductionVarianceCalculator` 或 `ProductionVarianceDispatcher` 触发点）；种子模板 SQL 三套
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1（notify 调用模式）；`1838-2` 差异引擎（已完成）

- [x] Decision: 触发点选择——在差异计算结果落定后（`ProductionVarianceCalculator` 输出 variance amount）按 `erp-mfg.variance-alert-threshold` 阈值判定，超阈值调 `notify`；不在过账 Dispatcher 内（保持过账纯事务性，告警为旁路）。记录理由：告警是观察侧职责，与过账落库解耦避免回滚耦合。
  - Skill: `nop-backend-dev`
- [x] Add: mfg 差异告警——计算结果超阈值时 `notify("mfg.production-variance", {workOrderId, productCode, varianceType, varianceAmount, threshold})`，config-gated（`erp-mfg.variance-alert-enabled` 默认 true + 阈值键）。接收人=生产主管（ROLE）。
  - Skill: `nop-backend-dev`
- [x] Add: 种子模板 `mfg.production-variance`（异常告警类，1 分钟合并含次数），三套 SQL。
  - Skill: `nop-backend-dev`
- [x] Proof: 扩展 `1838-2` 差异测试，断言超阈值 notify 调用 + 未超阈值不通知。命令 `mvn test -pl module-manufacturing/erp-mfg-service -am`。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 生产差异超阈值触发 `mfg.production-variance` 站内消息（测试断言），阈值 config 可调

## Draft Review Record

- Independent draft review iteration 1: accept (`ses_0cb8a650affeU4GCmIuwKUpPpe`, general agent 新会话) because — 全部 Current Baseline file:line 主张经独立仓库核实精确成立（notify 契约/erp-notify-dao 干净叶子/6 消费者集成点/scheduler.yaml 9 job 无 reminder/种子模板 3 条/mfg 差异计算器存在/跨域 dao 依赖范式既有）；三处前置 Deferred 触发条件（0504-1 全量消费者接线、0306-1 提醒类 job、1838-2 差异告警）经 0504-1 通道交付后真实满足；单计划 6 消费者共享同一 notify() 契约是 rule 14 正确应用（非过度/不足拆分）；anti-slack 合规。修订吸收 5 项非阻塞观察：Finance afterCommit 内层事务放置（N1）、context 字段对齐 Recorder 现签名（N3）、CS SLA 复用既有 job（N5）、erp-notify-dao 依赖措辞精确化（N4）、阶段聚合 Item Type（N2）。

## Closure Gates

- [x] 范围内行为完成（6 消费者 notify 调用点 + 2 scheduler job 三件套 + 3 新模板）
- [x] 相关文档对齐（`notification-strategy.md` 消费者清单收口；`job-scheduling.md` 2 个新 job 入目录；各域 owner doc 通知触发点；当日日志 `docs/logs/2026/07-06.md`）
- [x] 已运行验证：根 `mvn clean install -DskipTests` + 受影响域 `mvn test -pl <域 service> -am`（cs/crm/finance/sales/mfg + notify）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status=completed、3 Phase Status 全 completed、6 条 Exit Criteria 全 [x]、7 条 Closure Gates 全 [x]、`docs/logs/2026/07-06.md` 第 3 节"域运营事件通知消费者接线 — plan 2026-07-06-0642-1"条目一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 [ ] 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 角色基础设施落地后的精确接收人路由

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期接收人沿用既有 ROLE resolver（角色名→NopAuthUserRole）+ USER_LIST（escalationUserId/ownerUserId），与通道侧 config-gated 语义一致。精确组织层级路由依赖角色基础设施（`0504-1`/`0315-1` Deferred）。
- Successor Required: `yes`（触发条件：ERP 角色定义基础设施落地时）

### 提醒类 job 的 nop-batch 分 chunk 迁移

- Classification: `optimization candidate`
- Why Not Blocking Closure: 当前 nop-job-local 直调 BizModel 对中小数据量够用（`1600-1 §7` batch-candidate 触发条件）。
- Successor Required: `yes`（触发条件：单次到期事件量稳定 ≥ 数万时）

## Closure

Status Note: 2026-07-06 执行完成。3 Phase 全部完成（CS/Finance/Sales 三消费者 + CRM/CSAT 两 scheduler job + Mfg 差异阈值告警），full-green verification（`mvn clean install -DskipTests` BUILD SUCCESS；6 个测试类 20+ test methods 全绿，下游 inventory/crm 无回归）。详见 `docs/logs/2026/07-06.md` 域运营事件通知消费者接线章节。

Closure Audit Evidence:

- Auditor / Agent: independent closure auditor subagent（新会话，不重用执行者上下文）
- Audit Scope: 全部 Current Baseline 主张、3 Phase 全部执行项与退出标准、Closure Gates、Anti-Hollow 运行时可达性、Deferred honesty
- Live Repo Verification（独立 grep/read，非信任 [x]）:
  - CS SLA notify 调用点 `module-cs/erp-cs-service/.../entity/ErpCsTicketBizModel.java:315`（`notifySlaOverdue` helper，config-gated by `ErpCsConfigs.isSlaNotifyEnabled`，try/catch 降级，非空 body）✓
  - Finance notify 调用点 `module-finance/erp-fin-service/.../posting/ErpFinPostingExceptionRecorder.java:161`（`dispatchNotify`，REQUIRES_NEW 独立事务内 notify，config-gated，非空 body）✓
  - Sales notify 调用点 `module-sales/erp-sal-service/.../entity/CreditLimitChecker.java:169`（SOFT_WARNING 路径，config-gated by `erp-sal.credit-notify-enabled`）✓
  - CRM job `module-crm/erp-crm-service/.../job/ErpCrmEventReminderJob.java`（cron 双层门控 + `findDueReminders` 委托 + 逐条 try/catch 隔离）✓
  - CSAT job `module-cs/erp-cs-service/.../job/ErpCsCsatReminderJob.java`（`findSurveyReminders` + `findExpiredSurveys` 合流）✓
  - scheduler.yaml `app-erp-all/src/main/resources/_vfs/nop/job/conf/scheduler.yaml:84,93`（`erp-crm-event-reminder` + `erp-cs-csat-reminder` 两 jobName + cron + bean invoker）✓
  - beans.xml 注册 `module-crm/.../app-service.beans.xml`、`module-cs/.../app-service.beans.xml`（两 `<bean>` 声明）✓
  - 种子模板三套 SQL mysql/oracle/postgresql 各含 3 新事件：`crm.event-reminder`(7104) / `cs.csat-reminder`(7105) / `mfg.production-variance`(7106) ✓
  - Mfg 差异告警 `module-manufacturing/erp-mfg-service/.../costing/ProductionVarianceCalculator.java:202`（`erp-mfg.variance-alert-threshold` 阈值判定 + 旁路 notify，非过账 Dispatcher 内）✓
- 行为测试落地：`TestErpCsSlaNotification` / `TestErpCsCsatReminderJob` / `TestErpFinPostingExceptionNotify` / `TestErpMfgVarianceAlert`（+ Sales/CRM 既有测试扩展），断言 notify 调用 + `ErpSysNotification` 落库 + config 关闭路径跳过
- Anti-Hollow: 所有 notify 调用点均有真实 helper 方法体（非 `{}`/`return null`），异常吞咽均带 WARN 日志且为主流程降级语义（非静默隐藏）
- Deferred Honesty: 2 项 Deferred（精确接收人路由、batch chunk 迁移）均带触发条件，非范围内缺陷隐藏
- Logs Sync: `docs/logs/2026/07-06.md` 第 3 节已记录本计划执行结果与 full-green 验证状态
- Verdict: 通过 — 范围内 6 消费者 + 2 scheduler job + 3 种子模板全部落地且运行时可达，文本一致，无 hollow 实现

Follow-up:

- 精确接收人路由（见上方 Deferred，角色基础设施触发）
- 审批工作流通知（归 `2026-07-06-0642-2` 独立结果面）
