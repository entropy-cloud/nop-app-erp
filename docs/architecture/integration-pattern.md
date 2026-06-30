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
