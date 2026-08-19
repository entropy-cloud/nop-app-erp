# 配送时间窗口管理（Delivery Time Window）

> 客户特定配送时间段管理、时段预约、容量控制与超时处理。
> 参考：Odoo delivery time slots, SAP TM time windows

## 业务目标

- 客户维度配送时间窗口定义（如周一至周三 9:00-12:00）
- 客户自助选择/预约配送时段
- 时段容量控制（每个窗口最大配送单数）
- 超时/爽约处理机制（改期费、优先预约权）
- 与发运排程集成

## 时间窗口模型

### 窗口定义

每个窗口由以下因素定义：
- **客户**（partnerId）：不同客户可配置不同窗口
- **星期**（weekday）：1=周一 … 7=周日
- **开始时间**（startTime）：如 09:00
- **结束时间**（endTime）：如 12:00
- **最大容量**（maxCapacity）：该时段最多可预约数

### 窗口示例

| 客户 | 星期 | 时段 | 最大预约数 |
|------|------|------|-----------|
| 客户A | 周一 | 09:00-12:00 | 5 |
| 客户A | 周一 | 14:00-17:00 | 3 |
| 客户A | 周三 | 09:00-12:00 | 4 |
| 客户B | 周二 | 10:00-11:30 | 2 |
| 客户B | 周四 | 14:00-16:00 | 3 |

## 数据模型

### ErpLogDeliveryWindow（配送时间窗口）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| partnerId | BIGINT | 客户(往来单位)ID |
| orgId | BIGINT | 业务组织 |
| weekday | INT | 星期(1=周一 … 7=周日) |
| startTime | TIME | 开始时间 |
| endTime | TIME | 结束时间 |
| maxCapacity | INT | 最大预约数 |
| currentBooked | INT | 当前已预约数 |
| isActive | BOOLEAN | 是否启用 |
| effectiveFrom | DATE | 生效日期 |
| effectiveTo | DATE | 失效日期 |
| allowedShipmentTypes | VARCHAR(200) | 允许的发运类型 |
| remark | VARCHAR(1000) | 备注 |
| delVersion | BIGINT | 逻辑删除版本 |
| version | INT | 数据版本 |
| createdBy | VARCHAR(50) | 创建人 |
| createTime | TIMESTAMP | 创建时间 |
| updatedBy | VARCHAR(50) | 修改人 |
| updateTime | TIMESTAMP | 修改时间 |

### ErpLogDeliveryBooking（配送时段预约）— 已物化（RC-R1.84，P1-RC-086）

> 原标「预留」；2026-08-19 经 plan `2026-08-19-2040-1` Phase 2 物化（A 类纯加性授权 + 双独立子 agent 批准，记录见计划 §ORM Approvals）。字段契约与下表一致；`bookedTime` 实现为 VARCHAR(8)（与 `ErpLogDeliveryWindow.startTime/endTime` 的既有实现同型，本表 TIME 标注为文档侧类型偏差，容量语义承载于窗口行）。幂等 UK：`UK_LOG_DELIVERY_BOOKING_SHIPMENT(shipmentId, delVersion)` =「同一发运单不可重复预约」，逻辑删释放槽位。

每个发运单关联一个预约记录：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| shipmentId | BIGINT | 发运单ID |
| windowId | BIGINT | 窗口ID（→ ErpLogDeliveryWindow） |
| bookedDate | DATE | 预约日期 |
| bookedTime | TIME | 预约时间 |
| status | VARCHAR(20) | 状态：BOOKED / CONFIRMED / ARRIVED / MISSED / CANCELLED |
| missedFee | DECIMAL(20,4) | 爽约费 |
| priorityScore | INT | 优先级评分(高优先客户优先重新分配) |
| remark | VARCHAR(1000) | 备注 |
| delVersion | BIGINT | 逻辑删除版本 |
| version | INT | 数据版本 |
| createdBy | VARCHAR(50) | 创建人 |
| createTime | TIMESTAMP | 创建时间 |
| updatedBy | VARCHAR(50) | 修改人 |
| updateTime | TIMESTAMP | 修改时间 |

## 业务流程

### 流程 1：窗口预约
```
1. 发货员创建发运单时选择"预约配送时段"
2. 系统展示该客户可用窗口（按星期过滤）
3. 选择时段后检查容量（currentBooked < maxCapacity）
4. 确认 → 创建 ErpLogDeliveryBooking，状态 BOOKED
5. currentBooked += 1
```

### 流程 2：爽约/超时处理
```
1. 约定时段内未完成配送
2. 状态 MISSED
3. 触发爽约费计算（配置或固定金额）
4. 客户获得优先重新预约权（priorityScore 提升）
```

### 流程 3：容量释放
```
1. 发运单 CANCELLED / DELIVERED
2. 对应预约释放
3. currentBooked -= 1
```

## 实现注记（RC-R1.84 / P1-RC-086，plan `2026-08-19-2040-1`）

- **预约引擎入口**：`ErpLogDeliveryBookingBizModel`（erp-log-service）——`book`（窗口有效期内[isActive + effectiveFrom/effectiveTo] + 星期匹配 + `currentBooked < maxCapacity` 容量守卫 → 创建 BOOKED + 计数 +1；同一发运单重复预约幂等拒绝，应用层守卫 + DB UK 并发兜底）/ `releaseForShipment`（预约 CANCELLED + 计数 -1 下限 0；无有效预约幂等 no-op）/ `markArrived` / `markMissed`（爽约费读系统参数 `erp-log.booking-missed-fee`，默认 0 + priorityScore +10 提升）。容器计数更新经 `ErpLogDeliveryWindow.version` 乐观锁防并发超卖（updateEntity 冲突时事务失败回滚，双读同值仅一笔提交成功）。
- **发运单状态机联动**：`GatewayDispatcher` 的 `cancelShipment`（→CANCELLED）与 `advanceTracking`（→DELIVERED，webhook/轮询共用）迁移点后置调 `releaseForShipment`，失败隔离 try/catch 不阻断主状态迁移（对齐 R1.59 联动降级范式）。
- **D2 裁决（预约状态 ↔ 发运单状态映射）**：选择「松耦合对齐 + 人工标记入口」——BOOKED = 预约创建态（发运单 DRAFT/ADVISED 期预约）；CONFIRMED = 预留确认态（发运单 DISPATCHED+ 语义，本切片经通用 update 入口可达）；ARRIVED = 配送到达回执（`markArrived` 人工入口；UC-LOG-07 步骤 5「ARRIVED/DELIVERED」的 DELIVERED 分支由发运单 DELIVERED 迁移点联动释放表达——预约状态字典无 DELIVERED 值，发运单 DELIVERED ⇒ 预约生命周期终结并释放容量）；MISSED = 爽约（`markMissed` 人工标记——调度员线下确认）；CANCELLED = 释放（终态联动自动 + 手工 release）。**替代方案（否决）**：(a) 预约状态机与发运单状态机强绑定自动推进——两状态轴生命周期不同步（预约可先于发运创建、后于发运释放），强绑定引入逆向依赖；(b) MISSED 过期自动扫描 job——本切片零新 job（对齐调度接线家族范围边界），人工入口满足 L1 验收语义。**残留风险（successor）**：CONFIRMED 自动推进（advise 联动）与 MISSED 自动扫描（bookedDate+endTime 过期判定）未自动化——未人工标记的过期预约停留 BOOKED 占位容量，直至发运单终态联动释放兜底。
- **爽约费配置**：`erp-log.booking-missed-fee`（BigDecimal，默认 0），L1「爽约费金额从系统参数配置读取」。
- **测试**：`TestErpLogDeliveryBooking` 9 组（容量满拒绝/预约成功计数+1/重复预约幂等拒绝/释放计数-1+下限 0+槽位复用/爽约费+priorityScore+markArrived+终态守卫/窗口过期+星期不匹配/CANCELLED 联动释放/DELIVERED 联动释放/并发计数守卫按最新值复核）。

## 涉及的领域机制

- `carrier-shipment.md` — 发运单与承运商派发
- `use-cases.md` UC-LOG-01 — 发运单创建流程
- `../inventory/warehouse-slot.md` — 仓库月台预约（可扩展对接）
- `../sales/customer-integration.md` — 客户主数据对接
