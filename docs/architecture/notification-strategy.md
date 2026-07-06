# 通知策略

## 目的

定义 nop-app-erp 的通知机制，包括通知类型、频控规则与实现方案。

## 通知类型

| 类型 | 触发场景 | 通知渠道 | 优先级 |
|------|----------|----------|--------|
| 业务提醒 | 待办任务、审批待处理 | 站内消息 | 普通 |
| 异常告警 | 过账失败、库存异常、期间结账阻塞 | 站内消息 + 邮件 | 高 |
| 系统通知 | 版本升级、配置变更 | 站内消息 | 低 |

## 频控规则

为避免通知轰炸，同一类型通知在时间窗口内合并：

| 通知类型 | 时间窗口 | 合并策略 |
|----------|----------|----------|
| 业务提醒 | 5 分钟 | 同一用户、同一类型合并为一条 |
| 异常告警 | 1 分钟 | 同一错误类型合并，包含发生次数 |
| 系统通知 | 不合并 | 每条独立发送 |

## 实现方案

- **外发通道（邮件/短信）**：经 Nop Platform `nop-integration` 组件的 `IEmailSender.sendEmail(EmailMessage)` / `ISmsSender.sendMessage(SmsMessage)` SPI 派发，与平台默认路线（`../nop-entropy/docs-for-ai/02-core-guides/reporting-and-notification-integration.md`）一致；不新建平行通道适配层。
- **站内消息**：由本子系统 `module-notify`（`app-erp-notify`）负责，落 `ErpSysNotification` 表；`notify(eventType, context)` 为统一派发入口（解析模板→解析接收人→频控合并→落站内消息→config-gated 派发外发）。
- **通知模板**：存储在 `erp_sys_notification_template` 表（`ErpSysNotificationTemplate` 实体），`subjectTpl`/`bodyTpl` 为 `${var}` 插值模板，按业务事件键 `notificationType` 查找 ACTIVE 模板。
- **接收人配置**：按角色/组织/用户列表配置（`recipientResolver` = ROLE/ORG/PARTNER/USER_LIST），ROLE 复用平台 `nop-auth`（角色名 → `NopAuthUserRole` → userId）。
- **通知已读状态**：记录在 `erp_sys_notification_read` 表（`ErpSysNotificationRead`），唯一键 (notificationId, userId) 防重复。
- **config-gated**：bootstrap 默认仅站内消息通道（`erp-notify.email-enabled`/`sms-enabled` 默认 false），无真实供应商时跳过外发并 WARN，不阻断业务事实。
- **异步总线（`nop-message`）**：本期不接入（同步派发 + `txn().afterCommit` 即可满足单实例 bootstrap）；触发条件=生产部署/多实例/通知量需削峰时接入 Kafka/Pulsar，归后继。

## 业务消费者接线清单（plan 2026-07-06-0642-1）

6 个域运营事件接入 `notify()` 派发链 + 2 个提醒类 scheduler job：

| 事件类型 | 调用点 | config-gated | 默认接收人 | 备注 |
|----------|--------|--------------|-----------|------|
| `cs.sla-overdue` | `ErpCsTicketBizModel.scanOverdueTickets` + `findSlaWarnings` | `erp-cs.sla-notify-enabled` (true) | ROLE 客服主管 | 复用既有 `erp-cs-sla-scan` scheduler job 自动派发 |
| `fin.posting-exception` | `ErpFinPostingExceptionRecorder.record`（双 REQUIRES_NEW 隔离） | `erp-fin.posting-exception-notify-enabled` (true) | ROLE 财务员 | 异常记录独立事务提交后调 notify，避免主过账回滚吞掉通知 |
| `sal.credit-over-limit` | `CreditLimitChecker.check` SOFT_WARNING 路径 | `erp-sal.credit-notify-enabled` (true) | ROLE 销售员 | HARD_BLOCK 抛错拒绝不通知；SPECIAL_APPROVAL 持权限放行不通知 |
| `crm.event-reminder` | `ErpCrmEventReminderJob.execute`（scheduler） | `erp-crm.event-reminder-cron` (空=不调度) | USER_LIST ownerUserId | 设计默认 cron 每 15 分钟；查 PLANNED 状态到期事件 |
| `cs.csat-reminder` | `ErpCsCsatReminderJob.execute`（scheduler） | `erp-cs.csat-reminder-cron` (空=不调度) | ROLE 客服员 | 设计默认 cron 每日 02:00；查未响应/过期调查 |
| `mfg.production-variance` | `ProductionVarianceCalculator.calculateVariances`（旁路告警） | `erp-mfg.variance-alert-enabled` (true) + `erp-mfg.variance-alert-threshold` (默认 100) | ROLE 生产主管 | 阈值判定最大净差异行；与过账 Dispatcher 解耦 |

**通知失败降级**：所有消费者在 notify 调用外包 try/catch（warn-and-continue），与通道侧"config-gated 静默跳过不阻断业务"语义一致。审批通知（0642-2）的失败降级由 `ErpSysNotificationBizModel.notify` 内部统一 catch 实现（脚本层无需 try/catch——XLang 不执行 try/catch 语句）。

**精确接收人路由**：本期接收人沿用既有 ROLE resolver（角色名→NopAuthUserRole）+ USER_LIST（escalationUserId/ownerUserId/submitterUserId），与通道侧 config-gated 语义一致。精确组织层级路由依赖角色基础设施落地（DEFERRED，见 plan 末尾）。

## 审批工作流通知接线（plan 2026-07-06-0642-2）

4 实体 WORKFLOW 审批（付款单/收款单/资产处置/HR 薪酬）三类审批生命周期通知接入 `notify()` 派发链：

| 事件类型 | 调用点 | config-gated | 默认接收人 | 备注 |
|----------|--------|--------------|-----------|------|
| `wf.<entity>.result` | `.xwf` on-wf-end listener（approve/reject 后） | 模板存在性 | USER_LIST `${submitterUserId}`（提单人 createdBy） | resultText 区分已通过/已驳回；USER_LIST recipientConfig 支持 `${var}` 从 context 插值（0642-2 增强） |
| `wf.<entity>.task-assigned` | `.xwf` 审批步骤 `<on-enter>` | 模板存在性 | ROLE 审批人角色（财务员/经理/HR专员等） | 多级链（HR 薪酬三级）每级步骤 onEnter 各自触发 |
| `wf.<entity>.cc` | `.xwf` cc step（`specialType="cc"`）`<on-enter>` | 模板存在性 | ROLE CC 角色（财务经理/销售经理/资产管理员/HR专员） | cc step 需 confirm 后 wf 结束（标准 OA 抄送语义） |

**关键技术约束**：xbiz `<observes>` 在当前 nop-entropy 版本仅 schema 解析、运行时未触发（dead），故审批通知统一在 wf listener/on-enter 注入。`<entity>` ∈ pur-payment/sal-receipt/ast-disposal/hr-salary。
