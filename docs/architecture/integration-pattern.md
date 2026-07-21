# 外部 API 集成模式

## 目的

定义 nop-app-erp 与外部系统的集成模式，当前采用 webhook-only 模式。

## 集成模式

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| Webhook（出站） | 本系统主动推送事件到外部 URL | 订单状态变更通知、库存预警 |
| Webhook（入站） | 外部系统推送事件到本系统 API | 电子发票回传、支付状态回调 |

## Webhook 出站

### 事件类型

| 事件 | 触发时机 | 数据 |
|------|----------|------|
| order.approved | 采购/销售订单审核通过 | 订单号、金额、供应商/客户 |
| stock.low | 库存低于安全库存 | 物料、仓库、当前量 |
| invoice.created | 发票生成 | 发票号、金额、类型 |

### 实现

- 使用 `nop-message` 组件的 webhook 通道
- 事件注册在 `ErpSysWebhookConfig` 表
- 支持重试（指数退避，最多 3 次）
- 失败记录在 `ErpSysWebhookLog` 表

## Webhook 入站

- 暴露 REST API 接收外部推送
- 签名验证（HMAC-SHA256）
- 幂等处理（基于 event_id 去重）
- 异步处理（接收后 202，异步执行业务逻辑）

## 安全

- Webhook URL 配置 HTTPS
- 出站请求携带签名头（`X-Signature`）
- 入站请求验证签名
- IP 白名单（可选）

## 通用外部 API 集成参考模式

> 本文聚焦 webhook 出站/入站模式。**通用第三方 REST/SOAP/GraphQL API 集成参考模式**（auth pattern + rate limiting + endpoint 配置范式 + API client lifecycle）见独立文档：
>
> - [`external-api-integration-pattern.md`](./external-api-integration-pattern.md)（**NEW**，plan `2026-07-21-1206-3` D1 落地）
>
> 该文档与本文章节互补：
>
> | 主题 | 归属 |
> |------|------|
> | Webhook 出站/入站（事件 + 签名 + 幂等 + IP 白名单） | 本文 |
> | OAuth2 / API Key / LWA auth pattern | `external-api-integration-pattern.md §3` |
> | Rate limiting（平台 `IRateLimiter` 令牌桶） | `external-api-integration-pattern.md §4` |
> | Endpoint 配置范式（yaml + 运行时 dict，D1 ORM 变更=否） | `external-api-integration-pattern.md §5` |
> | API client lifecycle（创建/健康检查/熔断/恢复） | `external-api-integration-pattern.md §6` |
> | 参考实现案例（logistics / b2b / master-data） | `external-api-integration-pattern.md §7` |

## 文档/代码漂移记录（2026-07-21，plan `2026-07-21-1206-3` D1 Phase 0）

> **如实记录**（不修复，归 successor）：
>
> 本文「Webhook 出站」实现段引用 `ErpSysWebhookConfig` / `ErpSysWebhookLog` 两表，但实测 notify 模块 ORM **仅有** `ErpSysNotificationTemplate` / `ErpSysNotification` / `ErpSysNotificationRead` 三实体 —— webhook 配置/日志表**尚未实体化**。
>
> - 当前实现：webhook 出站事件经 `ErpSysNotification` 通用通知派发（见 `docs/architecture/notification-strategy.md`），未单独建 webhook 配置实体。
> - successor 触发条件：notify 域 webhook 接入具体业务需求（多目标 URL + 多事件类型 + 凭证管理）。
> - 本文 webhook 段落按"设计约定"理解，不视为已落地能力；webhook 配置实体化时同步更新本文。
