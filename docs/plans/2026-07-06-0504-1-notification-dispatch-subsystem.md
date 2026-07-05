# 2026-07-06-0504-1-notification-dispatch-subsystem 通知派发子系统

> Plan Status: completed
> Last Reviewed: 2026-07-06
> Source: deferred items recorded in 8+ completed plans (0306-1 / 1452-3 / 1838-2 / 0315-1 / 2200-2 / 0831-3 / 0700-1 / 0700-2)；owner doc `docs/architecture/notification-strategy.md`
> Related: `2026-07-05-0306-1-scheduler-wire-periodic-jobs.md`（提醒类作业等本子系统）、`2026-07-06-0315-1-workflow-approval-xwf.md`（抄送/通知步骤）、`2026-07-05-1838-2-manufacturing-production-variance.md`（差异预警）、`2026-07-04-1452-3-finance-posting-runtime-monitoring.md`（运行监控告警通道）
> Audit: required

## Current Baseline

- **设计已存在**：`docs/architecture/notification-strategy.md` 定义三类通知（业务提醒/异常告警/系统通知）、频控合并窗口（业务提醒 5 分钟/异常告警 1 分钟/系统通知不合并）、`ErpSysNotificationTemplate`+`ErpSysNotificationRead` 两表、按角色/组织配置接收人、声明使用 `nop-message` 组件。
- **平台默认路线**：`../nop-entropy/docs-for-ai/02-core-guides/reporting-and-notification-integration.md` 明确——外发通道（邮件/短信）默认走 `nop-integration`（`IEmailSender.sendEmail(EmailMessage)` / `ISmsSender.sendMessage(SmsMessage)`，已有 `JavaEmailSender`/`TencentEmailSender`/`YunpianSmsSender`/`TencentSmsSender` 实现）；应用层负责"谁收什么通知、通知表达什么业务结果、失败是否影响业务事实"。
- **平台组件状态**：`nop-integration`（外发通道）与 `nop-message`（异步消息总线：codec/core/kafka/pulsar/debezium）均存在于 nop-entropy，**均未接入** `app-erp-all`（pom 仅含 `nop-wf-service/web`、`nop-report-service/web`）。
- **应用层零实现**：全仓无 `ErpSysNotification*` 实体（18 域 ORM 无此实体）、无 `NotificationService`/`NotificationBizModel`、无 `nop-integration`/`nop-message` 引用、无通知模块。`module-cs` 中仅一处字符串匹配，非实现。
- **接收人基础**：`docs/design/roles-and-permissions.md` 定义业务角色体系（核心/扩展/审核管理角色），但 0315-1 已裁决"ERP 角色定义基础设施"为 deferred（触发条件：角色基础设施落地）。平台 `nop-auth`（`NopAuthUser`/`NopAuthRole`/`NopAuthUserRole`/`NopAuthDept`）提供用户-角色-部门底座。
- **待解除的下游阻塞**（均为已 completed 计划的 Deferred；触发条件因计划而异，多数为"通知派发通道/告警基础设施落地时"，部分为"生产部署需自动通知值班时"）：
  - 0306-1：CRM 事件提醒 / CSAT 调查提醒（扫描方法已就绪，无"发送"副作用；触发条件=通知派发通道落地）
  - 0700-1：CRM 事件提醒派发的扫描入口（派发本身归 0306-1；0700-1 自身 deferred 为 cron 注册）
  - 0700-2：SLA 升级通知 / CSAT 提醒派发的扫描入口（触发条件=通知派发通道落地）
  - 1452-3：运行监控告警通道（SMS/邮件/webhook）与升级链（触发条件=生产部署需自动通知值班时）
  - 1838-2：生产差异预警通知（触发条件=通知派发通道落地）
  - 0315-1：WORKFLOW 通知/抄送步骤（`specialType="cc"`，触发条件=通知派发通道落地）
  - 2200-2：电子签署过期轮询 + 超时通知（触发条件=生产部署）
  - 0831-3：排班定时提醒（触发条件=cron/定时需求，依赖本通道）

## Goals

- 落地**通知派发子系统**这一唯一结果面：一个统一的 `notify(eventType, context)` 派发入口，按业务事件解析模板→解析接收人→频控合并→落站内消息→（按通道配置）经 `nop-integration` 派发邮件/短信。
- 落地 `notification-strategy.md` 声明的三实体骨架（模板/通知实例/已读）与频控合并规则。
- 经 `IEmailSender`/`ISmsSender` 接入 `nop-integration`（config-gated，bootstrap 默认站内消息通道，邮件/短信通道经配置启用，无真实供应商时不阻断）。
- 提供最高优先级业务事件的种子模板（过账异常告警、SLA 超期、信用超额度）作为可验证样例，证明端到端派发闭环。
- 解除上述 8 个下游 Deferred 的"通道不存在"硬阻塞，使后续消费者接线计划可启动。

## Non-Goals

- 接线**全部**域消费者（CRM 提醒/CSAT/SLA 升级/差异预警/工作流抄送/签章超时等的实际触发点接线）——这些是独立后续计划，本计划只交付通道与样例模板。
- webhook 通道（`nop-integration` 当前仅邮件/短信 SPI）。
- 通知中心前端页面、接收人偏好 UI、富文本/多语言模板编辑器（前端面）。
- 角色基础设施本身（仍为 deferred；接收人解析复用平台 `nop-auth` 用户-角色-部门底座 + 模板配置，不新建角色模型）。
- 基于角色的精确路由（如"部门负责人"动态组织层级）——本计划 config-gated 静态接收人为主，动态组织层级归后继。
- `nop-message` 异步总线接入（Kafka/Pulsar）——本计划同步派发 + `txn().afterCommit` 钩子即可满足 bootstrap；异步总线归生产部署后继。

## Task Route

- Type: `architecture change`（新增跨域 sys 子系统 + 平台组件接入）+ `implementation-only change`
- Owner Docs: `docs/architecture/notification-strategy.md`（业务语义）、`docs/design/roles-and-permissions.md`（角色名空间）、`../nop-entropy/docs-for-ai/02-core-guides/reporting-and-notification-integration.md`（平台默认路线）、`../nop-entropy/docs-for-ai/03-runbooks/`（实体/BizModel 创建范式）
- Skill Selection Basis: 实现阶段涉及新增 ORM 实体 + codegen + BizModel + 跨平台 SPI 接入 → `nop-backend-dev` 匹配工作方法（实体服务创建、自定义动作、跨实体/平台调用、错误码、事务边界、产品化自检）；模型设计阶段 `nop-backend-dev` 路由的 ORM 模型文档。模型变更经 `mvn clean install -DskipTests` 增量重新生成（见 project-context）。

## Infrastructure And Config Prereqs

- 平台组件 `nop-integration-api`（+ 至少一个邮件/短信实现作为 Maven optional 依赖，bootstrap 可仅引 API、实现经 IoC config 选择）。
- config 项（命名空间 `erp-notify`）：`enabled`（总开关）、`default-channel`（IN_APP）、`email-enabled`/`sms-enabled`（默认 false，无供应商时不调用）、`merge-enabled`（默认 true）、各 `eventType` 的频控窗口覆盖。
- 接收人解析所需的平台 `nop-auth` 用户-角色-部门数据（已在 app-erp-all，无新增基础设施）。
- 无外部服务硬依赖：无真实邮件/短信供应商时，通道派发跳过并仅落站内消息 + 日志（不阻断业务）。

## Execution Plan

### Phase 1 - 接收人解析 Decision 与子系统模块骨架

Status: completed
Targets: `module-notify/model/*.orm.xml`（或裁决落点）、`app-erp-all/pom.xml`
Skill: `nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: 无

- [x] Decision: 通知子系统模块落点
  - 候选：(a) 新建 `module-notify` 走标准链 model→codegen→dao→meta→service→web 并聚合进 app-erp-all；(b) 落入 app-erp-all 内置；(c) 复用某 sys 域。
  - **选择 (a)**：新建 `module-notify`，逻辑工程名 `app-erp-notify`，appName `erp-notify`，moduleId `erp/notify`，物理子模块 `erp-notify-{codegen,api,dao,meta,service,web,app}`，字典命名空间 `erp-notify/*`。
  - 替代方案：(b) app-erp-all 内置——被否，违反 nop-entropy 域模块分离范式且使聚合工程承担业务职责；(c) 复用 sys 域——被否，通知非 18 业务域之一且 nop-sys 是平台内置。
  - 残留风险：新增第 19 个 module-* 域目录（与 18 业务域并列），需在 codebase-map/roadmap 文档中登记。app-erp-all pom 新增 `app-erp-notify-app` 依赖。
  - Skill: `nop-backend-dev`
- [x] Decision: 接收人解析机制
  - 候选：(a) 复用平台 `nop-auth`（`NopAuthRole` 角色名 → `NopAuthUserRole` → `NopAuthUser`）+ 模板配置角色名列表；(b) 模板内显式 userId 列表；(c) 动态组织层级（部门负责人）。
  - **选择 (a)+(b)**：`recipientResolver` 支持 ROLE（角色名→经 nop-auth `NopAuthUserRole` 解析为 userId 集合）/ USER_LIST（配置 userId 列表）/ ORG（deptId 下用户，bootstrap 静态）/ PARTNER（partnerId，站内消息）。模板 `recipientConfig` JSON 持有角色名/userId 列表。
  - config-gated 边界：本期不实现动态组织层级（"部门负责人"递归向上），与 0315-1「角色精确路由」Deferred 对齐。
  - 残留风险：ROLE 解析依赖 nop-auth 角色名匹配（角色名漂移会导致接收人缺失，config-gated 下静默跳过并 WARN 日志）。
  - Skill: `nop-backend-dev`
- [x] Decision: `nop-message` 异步总线是否本期接入
  - **选择不接入**：本期同步派发 + `txn().afterCommit` 钩子（业务事务提交后才派发，通知失败不回滚业务事实）。
  - 触发条件（归后继）：生产部署、多实例水平扩展、通知量需削峰时接入 `nop-message`（Kafka/Pulsar）。已记录于 Deferred But Adjudicated。
  - 与 Goals/Non-Goals 一致。
  - Skill: none

Exit Criteria:

> 仅交付 Decision 与骨架落点，解除 Phase 2 阻塞。

- [x] 三个 Decision 均在计划内记录选择、替代方案、残留风险（无代码 churn）
- [x] 模块落点 Decision 确定后，`app-erp-all` 依赖拓扑与新增模块骨架路径明确（本地化检查：新模块 pom/目录结构类型检查通过或 Decision 文本明确路径）

### Phase 2 - ORM 模型与 codegen 骨架

Status: completed
Targets: `<module>/model/app-erp-notify.orm.xml`、codegen 产物
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1 Decision

- [x] Add: `ErpSysNotificationTemplate` 实体（notificationType 业务事件键、name、channelSet IN_APP/EMAIL/SMS 位或枚举、subjectTpl、bodyTpl（XLang 模板）、recipientResolver ROLE/ORG/PARTNER/USER_LIST、recipientConfig JSON、mergeWindowSeconds、mergeStrategy NONE/MERGE_BY_USER_TYPE、status DRAFT/ACTIVE、delVersion）
- [x] Add: `ErpSysNotification` 实体（templateId、notificationType、recipientUserId、recipientPartnerId、recipientDeptId、channel、subject、body、payloadJson、status PENDING/SENT/MERGED/FAILED、mergeGroupId、sentAt、errorMsg、delVersion）—— 另增 mergeCount（合并次数）支持异常告警"包含发生次数"合并语义
- [x] Add: `ErpSysNotificationRead` 实体（notificationId、userId、readTime、delVersion）—— 按 `notification-strategy.md` 已读状态表
- [x] Add: 字典 `erp-notify/notification-channel`（IN_APP/EMAIL/SMS）、`erp-notify/notification-status`、`erp-notify/recipient-resolver`、`erp-notify/merge-strategy`、`erp-notify/template-status`
- [x] Proof: `mvn clean install -DskipTests` 经 gen-orm.xgen 增量链生成 DAO/Entity/XMeta/view 骨架，新模块 reactor 加入并编译通过
  - Skill: `nop-backend-dev`

Exit Criteria:

> 交付三实体 + 字典的生成骨架；后续 BizModel 依赖其生成。

- [x] 三实体 + 五字典经 codegen 生成且新模块编译通过（`mvn compile -DskipTests -pl <module> -am`）
- [x] 三实体经平台 `_app.orm.xml` 聚合进 app-erp-all，无实体名冲突（全 154 reactor 模块 `mvn clean install -DskipTests` 全绿，app-erp-all 成功聚合 notify-app）

### Phase 3 - NotificationService 派发引擎与频控合并

Status: completed
Targets: `<module>-service/.../ErpSysNotificationBizModel.java`、`NotificationDispatcher.java`、`NotificationRecipientResolver.java`、`NotificationMergeCoordinator.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 2

- [x] Add: `notify(eventType, context)` @BizMutation 统一派发入口——解析 ACTIVE 模板（无 ACTIVE 模板则 config-gated 静默跳过，不阻断调用方）、解析接收人、频控合并判定、落 `ErpSysNotification`（IN_APP 永落）、按 channelSet 经 `IEmailSender`/`ISmsSender` 派发（config-gated，无供应商跳过并 WARN 日志）、`txn().afterCommit` 钩子确保业务事务提交后才派发（通知失败不回滚业务事实，与平台默认路线"通知失败对业务事实有无影响"对齐：默认无影响）
- [x] Add: `NotificationRecipientResolver`——按 `recipientResolver` 类型解析（ROLE→`nop-auth` 角色名查 `NopAuthUserRole`→userId 集合；USER_LIST→配置 userId 列表；ORG→deptId 下用户；PARTNER→partnerId，站内消息按 partner 关联用户）
- [x] Add: `NotificationMergeCoordinator`——按 `mergeStrategy` + `mergeWindowSeconds` 在窗口内对同 (recipientUser, eventType) 合并（业务提醒合并为一条、异常告警合并含次数），经 `mergeGroupId` + 时间窗查询既有 PENDING/未读实例合并或新建
- [x] Add: 模板渲染（`subjectTpl`/`bodyTpl` 经 XLang 表达式对 context 求值，复用平台 `ExprParser`/`JsonTool`，不引入第三方）
- [x] Add: `markRead(notificationId)` / `markAllRead(userId)` @BizMutation、`findUnread(userId)` / `countUnread(userId)` @BizQuery
- [x] Add: ErrorCode（`ERR_NOTIFY_TEMPLATE_NOT_ACTIVE`/`ERR_NOTIFY_RECIPIENT_RESOLVE_FAILED`/`ERR_NOTIFY_CHANNEL_DISABLED`/`ERR_NOTIFY_RENDER_FAILED`）扩展 `NopException` + i18n
- [x] Decision: 频控合并的并发安全（同 (user,eventType) 并发派发）——倾向 DB 唯一约束 + 时间窗查询兜底（复用 `2026-07-05-1000-1-unique-key-constraints.md` 唯一键范式），记录替代方案（分布式锁）
  - Skill: `nop-backend-dev`

Exit Criteria:

> 交付派发引擎可观察行为：notify 入口落库 + 频控合并 + 通道派发（站内默认绿，邮件/短信 config-gated 跳过不报错）。

- [x] 单元测试：notify 触发→站内消息落 `ErpSysNotification`（status=SENT，IN_APP）、payloadJson/subject/body 渲染正确、`findUnread` 返回该实例
- [x] 单元测试：频控窗口内二次 notify 同 (user,eventType)→合并（`mergeGroupId` 一致、行数不增或按策略聚合），窗口外→新建
- [x] 单元测试：邮件/短信 channelSet 配置但供应商 config 关闭→跳过派发仅 WARN，不抛错、不阻断；`markRead` 后 `countUnread` 归零

### Phase 4 - 种子模板与端到端样例验证

Status: completed
Targets: 种子模板数据、`<module>-service` 测试、`docs/architecture/notification-strategy.md`
Skill: `nop-backend-dev`

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 3

- [x] Add: 三类通知各一种子模板（业务提醒样例：SLA 超期预警 customer-service；异常告警样例：过账异常 finance `ErpFinPostingException` 未处置；系统通知样例：信用超额度 sales）——证明三类频控与通道分支可端到端运行
- [x] Proof: 端到端测试经 `notify(eventType, context)` 触发三种子模板，断言接收人解析、模板渲染、频控合并、站内落库、`markRead`/`findUnread` 全链路（`JunitAutoTestCase` + GraphQL `@BizMutation`/`@BizQuery` 快照）
  - Skill: `nop-backend-dev`
- [x] Fix: `notification-strategy.md` 已确认 owner-doc 漂移（"使用 nop-message 组件"与平台默认路线不符）——补充实现落位说明（平台默认路线对齐：外发走 `nop-integration`、站内消息为本子系统、`nop-message` 异步总线归后继的触发条件），纠正表述
  - Skill: none

Exit Criteria:

> 三种子模板端到端绿，证明子系统可被后续消费者接线。

- [x] 三种子模板端到端测试全绿（接收人解析正确、渲染正确、频控合并正确、站内消息可读/可标记已读）
- [x] `notification-strategy.md` 实现落位与平台默认路线一致（无"自建平行通道适配层"反模式）

## Draft Review Record

- Independent draft review iteration 1: accept (`ses_0cbe39c40ffetbZPJO0VvbFKyK`) — 独立子代理经实时仓库验证全部 baseline 声明为真（`nop-integration`/`nop-message` 未接入 app-erp-all、无 `ErpSysNotification*` 实体、`IEmailSender`/`ISmsSender` 签名匹配、`nop-auth` 提供接收人底座），平台默认路线合规（外发走 nop-integration、无私有 @Inject、复用平台 helper），单一结果面，类型/Skill/Decision/反松弛均合规。已应用非阻塞精度修正：软化"均为...触发条件"为触发条件因计划而异、Related 引用 1452-1→1452-3、Phase 3 唯一键范式引用消歧为 `2026-07-05-1000-1-unique-key-constraints.md`、Phase 4 owner-doc 漂移修正项类型 Add→`Add | Fix`、"optional"→"Maven optional 依赖"。

## Closure Gates

> 完整仓库验证在结束处运行一次：`mvn clean install -DskipTests`（全 154+ reactor 模块含新 notify 模块）+ `mvn test -pl <notify-module> -am`（子系统单测/端到端）。

- [x] 范围内行为完成（派发入口 + 频控合并 + 站内消息 + nop-integration 通道 config-gated）
- [x] 相关文档对齐（`notification-strategy.md` 实现落位修正）
- [x] 已运行验证：`mvn clean install -DskipTests` 全绿、`mvn test -pl <notify-module> -am` 全绿（9 测试：5 dispatch + 4 seed-template 端到端）
- [x] 无范围内项目降级为 deferred/follow-up（域消费者全量接线、webhook、异步总线、角色基础设施、通知中心前端明确为 Non-Goal，非范围内降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### nop-message 异步总线接入（Kafka/Pulsar 削峰 / 多实例）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: bootstrap 同步派发 + `txn().afterCommit` 满足单实例；异步削峰/多实例归生产部署。
- Successor Required: `yes`（触发条件：生产部署/多实例/通知量需削峰时）

### webhook 通道

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `nop-integration` 当前仅邮件/短信 SPI；webhook 通道需新 SPI 或外部适配。
- Successor Required: `yes`（触发条件：第三方系统 webhook 订阅需求落地时）

### 基于角色的动态组织层级精确路由（部门负责人/上级审批人）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 与 0315-1「HR 精确角色路由」Deferred 同源，依赖角色定义基础设施；本期 config-gated 静态接收人。
- Successor Required: `yes`（触发条件：ERP 角色定义基础设施 / 动态组织层级路由需求落地时）

### 通知中心前端页面 / 接收人偏好 UI / 模板编辑器

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 后端 `@BizQuery`/`@BizMutation` 可独立验证；前端属 AMIS 定制面。
- Successor Required: `yes`（触发条件：通知中心前端定制启动时）

### 全量域消费者接线（CRM 提醒/CSAT/SLA 升级/差异预警/工作流抄送/签章超时/排班提醒）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划交付通道 + 样例模板；8 个消费者接线为各自域后续计划（通道已就绪即可启动）。
- Successor Required: `yes`（触发条件：各域消费者接线计划启动时）

## Closure

Status Note: 计划已完成（4 Phase 全绿，独立结束审计通过）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 `ses_0cbbc4f68fferDcU48lQP01NqX`（新会话，执行者未自我审计）—— OVERALL: close。8 项验证全通过：模块结构（7 子模块 + 根 pom/app-erp-all 接线）、ORM（3 实体 + 5 字典）、BizModel + 派发引擎（notify/markRead/markAllRead/findUnread/countUnread + Dispatcher/Resolver/MergeCoordinator + 4 ErrorCode）、nop-integration SPI 接线（IEmailSender/ISmsSender，无自建适配层）、测试（5 dispatch + 4 seed-template 端到端）、种子模板 SQL（3 类）、owner-doc 漂移修正、beans 注册。
- 验证命令：`mvn clean install -DskipTests`（154 reactor 模块全绿）、`mvn test -pl module-notify/erp-notify-service -am`（9 测试全绿：5 dispatch + 4 seed-template）。

Follow-up:

- 各域消费者接线（通道就绪后独立计划）
- nop-message 异步总线（生产部署触发）
- webhook 通道（第三方订阅触发）
