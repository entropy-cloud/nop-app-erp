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

### ErpLogDeliveryBooking（配送时段预约）— 预留

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

## 涉及的领域机制

- `carrier-shipment.md` — 发运单与承运商派发
- `use-cases.md` UC-LOG-01 — 发运单创建流程
- `../inventory/warehouse-slot.md` — 仓库月台预约（可扩展对接）
- `../sales/customer-integration.md` — 客户主数据对接
