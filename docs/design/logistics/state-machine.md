# 物流/运输域状态机

> **设计要点依据**：本状态机按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度组织。审查本状态机时使用该提示词。
>
> 物流域的状态对象为**发运单**（Shipment）。

## 适用对象：发运单（Shipment）

### 1. 状态定义

| 状态 | 业务含义（等待什么） | 业务单据影响 |
|------|----------------------|--------------|
| 草稿（DRAFT） | 发运单已创建，等待确认发运 | 无影响，可编辑 |
| 已预约（ADVISED） | 已向承运商预约取件/发运，等待承运商接单 | 关联出库单锁定（不允许重复发运） |
| 已派发（DISPATCHED） | 承运商已接单，等待运输中更新 | 库存已出账（写入 inventory） |
| 运输中（IN_TRANSIT） | 货物在途，等待签收 | 追踪信息持续更新 |
| 已签收（DELIVERED） | 终态：客户已签收 | 触发运费过账（path-1 销售运费）/ 到岸成本自动创建（path-2 采购运费，config-gated） |
| 已取消（CANCELLED） | 终态：发运取消 | 释放关联出库单锁定 |

### 2. 迁移完整性

```
草稿 (DRAFT)
  ├─ 预约承运商 → 已预约 (ADVISED)
  │                ├─ 承运商接单/网关下单成功 → 已派发 (DISPATCHED)
  │                ├─ 承运商拒接/网关超时 → 重试（最多3次）
  │                │     └─ 重试耗尽 → 保留 ADVISED，人工干预
  │                └─ 取消 → 已取消 (CANCELLED)
  ├─ 直接取消 → 已取消 (CANCELLED)
  └─ 编辑后重新提交

已派发 (DISPATCHED)
  ├─ 跟踪更新 → 运输中 (IN_TRANSIT)
  ├─ 取消（网关支持取消） → 已取消 (CANCELLED)
  └─ 异常（网关取消失败） → 人工处理

运输中 (IN_TRANSIT)
  ├─ 签收 → 已签收 (DELIVERED)
  ├─ 异常（货物退回） → 已取消 (CANCELLED)
  └─ 部分签收 → 记录部分签收，状态保持 IN_TRANSIT（等待剩余）
```

| 迁移 | 触发人 | 前置条件 | 结果 |
|------|--------|----------|------|
| DRAFT→ADVISED | 发货员 | 承运商已配置、必填字段完整、关联出库单有效 | 调用 `adviseShipment`，生成网关日志 |
| ADVISED→DISPATCHED | 承运商网关 | 网关下单成功、运单号已获取 | 存储运单号/面单 URL，出库单锁定 |
| ADVISED→CANCELLED | 发货员 | 承运商未接单 | 释放出库单锁定 |
| DISPATCHED→IN_TRANSIT | 承运商回调/定时任务 | 网关追踪状态变为在途 | 更新追踪信息 |
| IN_TRANSIT→DELIVERED | 承运商回调/签收确认 | 网关追踪状态变为已签收或人工确认 | 触发运费过账 |
| DRAFT→CANCELLED | 发货员 | 无未完成网关调用 | 释放关联锁定 |
| IN_TRANSIT→CANCELLED | 发货员+审批 | 货物退回、网关取消成功或人工确认 | 触发逆向物流流程 |

### 3. 终态与恢复

- 终态：`已签收（DELIVERED）`、`已取消（CANCELLED）`。
- 终态不可直接恢复。DELIVERED 后若需退货，走 sales 域标准退货流程。
- CANCELLED 后可重新创建发运单（新的 `code`，关联原出库单）。

### 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| 网关下单超时/失败 | 自动重试（最多 3 次，指数退避），重试耗尽后保留 ADVISED 状态，标记网关异常，人工干预 |
| 承运商拒接 | 保留 ADVISED，通知发货员更换承运商或取消 |
| 追踪长时间无更新（超过预计送达日期 3 天） | 系统自动标记"追踪异常"，通知物流主管人工跟进 |
| 部分签收 | 记录签收明细，状态保持 IN_TRANSIT，等待剩余货物签收 |
| 货物退回（Return to Sender） | 记录退回原因，进入 CANCELLED。如需要重新发运，新建发运单（含退回标记） |
| 取消发运但网关不支持取消 | 标记"人工取消"，通知发货员联系承运商线下处理 |
| 并发更新同一发运单 | 乐观锁 |

### 5. 可达性

- 从 DRAFT 可达所有状态（直接或间接）。
- ADVISED/DISPATCHED/IN_TRANSIT 为中间状态，均可达终态 DELIVERED 或 CANCELLED。
- 无不可达状态，无死锁。终态无出边。

### 6. 角色与权限

| 迁移 | 执行角色 |
|------|----------|
| DRAFT→ADVISED | 发货员 |
| ADVISED→DISPATCHED | 网关（系统自动） |
| ADVISED→CANCELLED | 发货员 |
| DISPATCHED→IN_TRANSIT | 网关回调或系统自动 |
| IN_TRANSIT→DELIVERED | 网关回调或签收确认 |
| DRAFT→CANCELLED | 发货员 |
| IN_TRANSIT→CANCELLED（退货场景） | 发货员 + 物流主管审批 |

危险操作：
- **IN_TRANSIT→CANCELLED**（退货/取消在途）：需物流主管审批，因涉及逆向物流和运费争议。
- **ADVISED→CANCELLED**（已预约取消）：需要确认承运商未开始处理，防止产生取消费。

### 7. 外部依赖

| 外部场景 | 内部处理 |
|----------|----------|
| 销售出库审核触发发运 | sales 域发布出库事件，本域订阅生成发运单草稿 |
| 承运商网关回调（追踪更新） | 本域暴露网关回调端点，更新发运单状态和 `ErpLogShipmentLog` |
| 运费过账 | DELIVERED 后本域**直接调用** `IErpFinVoucherBiz.post(PostingEvent{businessType=FREIGHT})`（参 inventory `InvPostingExecutor` 范式），非事件订阅模型 |

> **实现裁决补注（plan 2026-07-04-1115-3 + 2026-07-11-2329-1）**：原描述"DELIVERED 后本域发布 `ShipmentDeliveredEvent`，finance 域订阅执行过账"已调整为：
> - **path-1（SALES_DELIVERY）**：**直接调用** `IErpFinVoucherBiz.post(PostingEvent{businessType=FREIGHT})`（参 inventory `InvPostingExecutor` 范式），与现有全域过账一致。
> - **path-2（PURCHASE_RECEIPT 采购运费）**：已从事件占位升级为 config-gated **到岸成本自动编排**。`erp-log.path2-landed-cost-auto-create=true`（默认 false，向后兼容）时，DELIVERED 后调用 `IErpInvLandedCostBiz.generateFreightLandedCost(receiveCode, freightAmount, ...)` 创建 DRAFT 到岸成本单（FREIGHT 费用行），由用户人工审核触发分摊→成本层更新→`LANDED_COST(490)` 过账（引擎由 plan `2026-07-10-1100-3` 提供）。config 关闭时退化为事件占位 + SETTLED（向后兼容）。
> - **path-2 浏览器层 E2E 覆盖（plan `2026-07-19-0849-2`）**：logistics path-2 完整链路（handleTrackingWebhook DELIVERED → onDelivered → handlePurchaseReceiptDelivered → generateFreightLandedCost → DRAFT ErpInvLandedCost）已有浏览器层 E2E 覆盖——`tests/e2e/business-actions/log-path2-landed-cost-auto-create.action.spec.ts` 2 用例（正路径 DRAFT 头+行字段精确数值断言 + freightAmount=0 边界显式断言无 LandedCost 创建），承接 2026-07-11-2329-1 后端落地 + TestErpLogPath2LandedCost 单测覆盖；freightAmount ≤ 0/null 分支由 path-1（SALES_DELIVERY）作代表验证 onDelivered 触发面。path-2 失败重试/scanForPolling 轮询驱动 DELIVERED + path-2 外币 freight 汇兑分支仍归 successor（不同结果面，见 2026-07-19-0849-2 Deferred But Adjudicated）。

外部触发渠道：
- 用户手工创建发运单（主要渠道）。
- sales 域出库事件触发自动创建发运单草稿（可选集成）。
- 承运商网关回调（异步追踪）。
- 定时任务：检查预计送达日期超期发运单。
- 轮询兜底（`scanForPolling`）：对 DISPATCHED/IN_TRANSIT 运单调 `trackShipment` 推进状态；DELIVERED 翻转后补调 `onDelivered`（与 webhook 路径一致，path-1 + path-2 均受益）。

### 8. TODO / 任务策略

| 状态 | 是否产生 TODO | TODO 类型 |
|------|---------------|-----------|
| DRAFT | 是 | assigned（发货员）—— 待确认发运 |
| ADVISED | 是 | assigned（系统/发货员）—— 等待承运商接单或人工处理网关异常 |
| DISPATCHED | 否 | —（系统自动追踪） |
| IN_TRANSIT | 否 | —（系统自动追踪） |
| DELIVERED | 否 | — |
| CANCELLED | 否 | — |

避免"草稿发运单长期滞留"：DRAFT 超过 24 小时产生升级 TODO，通知物流主管。
避免"网关异常长期未处理"：ADVISED 网关异常标记后 4 小时升级通知。

### 9. 场景演练

#### 场景 A：正常发运送达

1. 发货员创建发运单（DRAFT），关联销售出库单，选择承运商。
2. 确认发运 → 调用承运商网关预约（ADVISED）。
3. 网关下单成功 → 获取运单号 + 面单 URL → DISPATCHED。
4. 承运商扫描取件 → 追踪更新为在途（IN_TRANSIT）。
5. 客户签收 → 回调通知 → DELIVERED。
6. 触发运费过账 → 生成 FREIGHT 凭证。

#### 场景 B：承运商网关异常

1. 发货员创建发运单（DRAFT），确认发运（ADVISED）。
2. 网关调用超时 → 自动重试第 1 次失败 → 重试第 2 次失败 → 重试第 3 次失败。
3. 标记网关异常，保留 ADVISED，生成 `ErpLogShipmentLog` 记录错误。
4. 系统通知发货员：网关异常，需人工处理。
5. 发货员检查配置：API 端点有误。修复配置后手动触发重试。
6. 重试成功 → 网关正常接单 → DISPATCHED。

#### 场景 C：货物退回（Return to Sender）

1. 发运单在途（IN_TRANSIT）。
2. 承运商通知：客户拒收/地址错误/无法配送。
3. 发货员收到退回通知，确认退回原因。
4. 发货员提交 CANCELLED 审批（需要物流主管审批）。
5. 审批通过 → CANCELLED，记录退回原因和退回费用。
6. 如需重新发运，新建发运单（标记"原发运单XX退回重发"），关联原出库单。

### 10. 与设计文档一致性

- 发运单与承运商关联见 `logistics/README.md`。
- 三层 SPI 契约见 `logistics/README.md` §SPI 契约。
- 状态码归 `model/app-erp-logistics.orm.xml` 中 `erp-log/shipment-status` 字典。
- 网关异步下单策略见 `logistics/README.md` 业务规则 §1。

## 审查提示

审查本状态机时，使用 `docs/skills/state-machine-business-review-prompt.md`，重点检查：
- 网关异常的重试和人工干预路径是否完整。
- 终态 DELIVERED 的运费过账触发机制。
- 货物退回（Return to Sender）的审批权限是否落实。
- DRAFT→CANCELLED 与 IN_TRANSIT→CANCELLED 的审批差异。
- 关联出库单在发运单取消时的锁定释放逻辑。
- 乐观锁防止并发状态更新。
