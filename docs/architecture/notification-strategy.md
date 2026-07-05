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
