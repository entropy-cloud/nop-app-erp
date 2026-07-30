# 通知派发子系统（notify）

> 本文档是 notify 子系统的**域级入口**。通知收件箱前端范式见 [`inbox-patterns.md`](inbox-patterns.md)，通知类型/频控/通道策略见 `docs/architecture/notification-strategy.md`。

## 定位

`module-notify`（逻辑工程 `app-erp-notify`，appName `erp-notify`，icon `bell`）**不是 18 个业务域之一**，而是**跨域通知派发子系统**：为所有业务域提供统一的通知能力——业务事件经模板渲染、接收人解析、频控合并后落站内消息，并 config-gated 派发邮件/短信外发通道。

- 业务域（采购/销售/财务等）**不**自建通知基础设施，统一调用 `IErpSysNotificationBiz.notify(eventType, context)`。
- 子系统对业务方是 **best-effort** 服务：通知派发失败不回滚调用方业务事实（`notify()` 内部 try/catch + config-gated 静默跳过）。

## 边界

- 本子系统负责：通知模板管理、通知实例派发（模板渲染→接收人解析→频控合并→站内落库→外发通道）、已读状态、用户面收件箱后端。
- 本子系统**不**负责：业务事件判定（由各业务域决定何时调 `notify()`）；审批工作流引擎（由 `nop-workflow` / `.xwf` 承载，本子系统仅被 wf listener 触发派发）；用户/角色主数据（复用平台 `nop-auth`）。
- 持久化字段、字典、状态码以 `module-notify/model/app-erp-notify.orm.xml` 为准。
- 通知策略（类型/频控/通道）权威：`docs/architecture/notification-strategy.md`。

## 工程与模型

| 项 | 值 |
|----|----|
| Maven 工程 | `app-erp-notify`（二级简称 `sys`） |
| appName | `erp-notify` |
| 权威模型 | `module-notify/model/app-erp-notify.orm.xml` |
| 实体包 | `app.erp.notify.dao.entity` |
| 表前缀 | `erp_sys_notification*` |
| 标准模块链 | `model → codegen → dao → meta → service → web → app → api` |

## 核心业务对象

| 对象 | 业务含义 |
|------|----------|
| 通知模板（`ErpSysNotificationTemplate`） | `subjectTpl`/`bodyTpl` 为 `${var}` 插值模板，按业务事件键 `notificationType` 查找 ACTIVE 模板；含接收人解析配置 + 频控合并策略 + 渠道集 |
| 通知实例（`ErpSysNotification`） | 派发产出的站内消息记录，按 `recipientUserId` 投递 |
| 通知已读记录（`ErpSysNotificationRead`） | 唯一键 (notificationId, userId) 防重复；已读状态派生自此关联（**非** lifecycle `status`） |

## 子系统结构与派发链

```
业务域 → IErpSysNotificationBiz.notify(eventType, context)
                          │
                          ▼
        ErpSysNotificationBizModel（薄委派层）
                          │  查找 ACTIVE 模板（按 notificationType）
                          ▼
                 NotificationDispatcher.dispatch(template, context)
                          │
          ┌───────────────┼───────────────────────┐
          ▼               ▼                       ▼
   模板渲染           接收人解析              频控合并
  (${var}插值)     (RecipientResolver)   (MergeCoordinator)
                          │
                          ▼
              站内消息落库（ErpSysNotification）
                          │
                          ▼
          外发通道（config-gated）
     ┌──────────────┬───────────────┐
     ▼              ▼               ▼
  IN_APP(默认)   EMAIL          SMS
  (已落库)    IEmailSender    ISmsSender
              (nop-integration)
```

### 关键组件

| 组件 | 职责 |
|------|------|
| `ErpSysNotificationBizModel` | 薄委派层：`notify()` 委派 Dispatcher；`markRead`/`markAllRead`/`findUnread`/`findRead`/`countUnread` 维护已读状态 |
| `NotificationDispatcher` | 模板渲染 + 接收人解析 + 频控合并 + 站内落库 + config-gated 外发派发 |
| `NotificationRecipientResolver` | 按模板 `recipientResolver` 类型解析 userId 集合（见下表） |
| `NotificationMergeCoordinator` | 频控合并：命中窗口内既有实例则合并（`mergeCount` +1），否则新建 |
| `NoopEmailSender`/`NoopSmsSender` | 无真实供应商时的降级空实现（不阻断） |

### 接收人解析策略

| 解析器 | 机制 | 当前状态 |
|--------|------|----------|
| `ROLE` | 角色名 → `NopAuthRole` → `NopAuthUserRole` → userId | ✅ 已落地（复用平台 nop-auth） |
| `ORG` | `deptId` → `NopAuthUser` | ✅ 已落地 |
| `USER_LIST` | 模板配置 userId 列表（静态 + 支持 `${var}` 从 context 插值，如提单人 `submitterUserId`） | ✅ 已落地 |
| `PARTNER` | `partnerId` → 用户映射 | ⚠️ 占位（partner→user 映射未建立，WARN 返回空，config-gated） |

### 通道与失败语义

- **站内消息（IN_APP）**：默认通道，派发即落 `ErpSysNotification` 表。
- **邮件/短信（EMAIL/SMS）**：经 `nop-integration` 的 `IEmailSender`/`ISmsSender` SPI 派发；config-gated（见配置项），无供应商/未启用时 WARN 跳过，不阻断业务。
- **失败降级**：`notify()` 整体 try/catch——模板缺失/渲染失败/接收人解析失败/落库失败均不抛出给调用方（返回空列表）；各业务消费者额外在调用外包 try/catch（warn-and-continue）。

## 状态机

### 通知实例 lifecycle（`ErpSysNotification.status`，字典 `notification-status`）

| 状态 | 含义 |
|------|------|
| `PENDING` | 待发送（预留，当前同步派发不经过） |
| `SENT` | 已发送（站内消息落库即此态） |
| `MERGED` | 已合并（频控命中既有实例） |
| `FAILED` | 发送失败（预留） |

> **关键约束**：`status` 是通知 **lifecycle**，**非已读状态**。已读状态派生自 `ErpSysNotificationRead` 关联。收件箱页面禁用 `<filter><eq name="status" value="READ"/>`（见 `inbox-patterns.md`）。

### 模板状态（字典 `template-status`）

`DRAFT` → `ACTIVE`。仅 `ACTIVE` 模板参与派发；业务事件无 ACTIVE 模板时 config-gated 静默跳过（WARN）。

## 与 18 业务域的关系（消费者接线）

业务域通过 `IErpSysNotificationBiz.notify(eventType, context)` 接入派发链。已接线消费者（详见 `notification-strategy.md §业务消费者接线清单` + `§审批工作流通知接线`）：

| 消费域 | 事件类型 | 调用点 |
|--------|----------|--------|
| cs | `cs.sla-overdue` / `cs.csat-reminder` | `ErpCsTicketBizModel` / `ErpCsCsatReminderJob`（scheduler） |
| cs | `cs.entitlement-expiry` | `ErpCsEntitlementExpiryJob`（scheduler） |
| finance | `fin.posting-exception` | `ErpFinPostingExceptionRecorder`（REQUIRES_NEW 隔离） |
| sales | `sal.credit-over-limit` | `CreditLimitChecker`（SOFT_WARNING 路径） |
| crm | `crm.event-reminder` / `crm.sequence-overdue` | `ErpCrmEventReminderJob` / `ErpCrmSequenceOverdueJob`（scheduler） |
| manufacturing | `mfg.production-variance` | `ProductionVarianceCalculator`（旁路告警） |
| hr | `hr.contract-expiry` | `ErpHrContractExpiryJob`（scheduler）+ `ErpHrEmploymentContractBizModel` |
| workflow | `wf.<entity>.{result,task-assigned,cc}` | `.xwf` listener/on-enter（pur-payment / sal-receipt / ast-disposal / hr-salary） |

## 配置项

命名空间 `erp-notify.*`（经 `AppConfig.var(...)` 读取，权威 `ErpNotifyConfigs.java`）：

| 配置键 | 默认 | 含义 |
|--------|------|------|
| `erp-notify.enabled` | `true` | 通知总开关 |
| `erp-notify.email-enabled` | `false` | 邮件通道启用（无供应商时跳过 + WARN） |
| `erp-notify.sms-enabled` | `false` | 短信通道启用（无供应商时跳过 + WARN） |
| `erp-notify.merge-enabled` | `true` | 频控合并启用 |

频控窗口常量（`ErpNotifyConstants`）：业务提醒 5 分钟（300s）、异常告警 1 分钟（60s）；合并组 key = `notificationType + "#" + recipientUserId`。

## Successor（超出本子系统当前范围）

- **异步总线（`nop-message`）**：当前同步派发 + `txn().afterCommit` 满足单实例部署；生产部署/多实例/通知量需削峰时接入 Kafka/Pulsar（topic + partition + lease 消费者注册），归后继。
- **scheduler 驱动通知的重复派发（plan 2026-07-30-0841-2 R1.28 P1-MA2-086 通知重复类，仅 document）**：cs（`cs.sla-overdue`/`cs.csat-reminder`/`cs.entitlement-expiry`）、crm（`crm.event-reminder`/`crm.sequence-overdue`）、hr（`hr.contract-expiry`）等 scheduler 触发的通知全部运行于 `nop-job-local` 单实例（非分布式，无 cluster leader 锁）。单实例下重复触发（cron 抖动/重试）的**实体级行副作用**已由各 job body 幂等收口（如 cs SLA 经 `hasEscalationAction` 去重）；但**通知派发本身**无跨调用去重键（通知频控为 `notificationType + "#" + recipientUserId` 窗口合并，非确定性幂等）。多实例部署切换时通知可能重复，通知去重键 successor（与 cron 平台级并发防护 successor 同触发条件）。
- 全局 header 未读小角标（需修改全局 layout delta + WebSocket 推送）。
- 通知偏好设置页（需新 ORM 实体）。
- partnerId/deptId 精确路由（依赖 partner→user 映射落地）。
- `markUnread` 反向操作；批量删除/归档 + 数据保留策略。
- 审批类通知专用渲染（含「去审批」跳转，依赖 xwf 浏览器层可达性突破）。

## 关联文档

- 收件箱前端范式：[`inbox-patterns.md`](inbox-patterns.md)
- 通知策略（类型/频控/通道/消费者清单）：`docs/architecture/notification-strategy.md`
- 落地计划：`docs/plans/2026-07-06-0504-1-notification-dispatch-subsystem.md`（派发子系统）、`docs/plans/2026-07-19-2200-3-notify-inbox-page.md`（收件箱页面）
- 模块边界：`docs/architecture/module-boundaries.md §Owner Docs`
