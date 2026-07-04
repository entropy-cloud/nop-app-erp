# APS 排产算法

> **实现偏离补注**（2026-07-04，plan `2026-07-04-0831-1`）：
> - **贪心启发式非优化求解**：本期排产引擎为前向/后向贪心填充启发式（与 Axelor/Odoo 开源基线一致），无 ILP/CP 优化求解（计划 Non-Goal）。
> - **MAINTENANCE 单约束 + capacity=1**：仅消费 `ErpApsConstraint` MAINTENANCE 类型约束；PERSONNEL/TOOL/capacity>1 并联排产归 follow-up（`constraint-type` 字典与实体已预留）。
> - **工作中心班次日历未展开**：本期工作中心可用时间轴 = horizon 全域 − 维护停机区间；班次/节假日重复展开（`ErpMfgWorkcenterCalendar` shift 模式展开）归 follow-up。
> - **ATP/CTP 跨域只读聚合经 IDaoProvider**：本期对 inventory/manufacturing 域的 ATP 库存聚合与 CTP 工艺路线追溯采用 `IDaoProvider` 只读实体查询，而非跨域 I*Biz 强注入（I*Biz 强注入在 aps-service 单模块部署/测试时因依赖模块未组装而启动失败）。仅只读聚合、非裸 SQL、未破坏物理边界；完整 `app-erp-all` 部署等价。CTP 影子 OperationOrder 经 `IEntityDao.newEntity()` 构造，仅参与内存模拟，从不 save。
> - **甘特图前端可视化 / `dragUpdateOperation` 拖拽后端校验**：Non-Goal，归前端计划。
> - **APS→CRP 负荷来源 re-wiring / JobCard 按 OperationOrder 排程自动创建 / maintenance 停机事件订阅扣减 / 自动派工（DispatchRule/DispatchLog）执行 / nop-job 定时自动重排**：均为 Non-Goal，归各 owner 计划 follow-up（触发条件：本计划落地）。

## 目的

详细设计 APS 排产算法的三种核心模式（前向/后向/优先级排产）、有限产能约束、插单重排、ATP/CTP 模拟、以及甘特图数据模型。本文档是排产引擎的实现基准。

## 设计边界

- 本设计负责：OperationOrder 的排程时间计算（plannedStartDateT/plannedEndDateT）、产能冲突检测、插单区间重排、交期承诺模拟。
- **与 execution 的边界**：实际报工在 manufacturing/JobCard，APS 只负责排产计算。JobCard 按 OperationOrder 排程时间创建执行。
- **与 maintenance 的边界**：ErpApsConstraint（维护/停机约束）作为排产输入，静态维护计划在排产前导入。
- 本文档不涉及：MRP 物料需求计算、工艺路线创建、BOM 展开。

---

## 一、核心数据结构

### 1.1 OperationOrder 时间字段（详细）

| 字段 | 含义 | 计算方式 |
|------|------|----------|
| setupTime | 换模/准备时间（分钟） | 从工艺路线继承，或人工指定 |
| runtimePerUnit | 每件加工时间（分钟） | 从工艺路线继承 |
| qty | 加工数量 | 从 WorkOrder 派生或人工调整 |
| totalDuration | 总耗时 | `setupTime + runtimePerUnit × qty`（派生字段） |
| plannedStartDateT | 计划开工时间 | APS 排产输出 |
| plannedEndDateT | 计划完工时间 | APS 排产输出 = plannedStartDateT + totalDuration |
| earliestStartDateT | 最早可开工时间 | 前向排产的起点约束（物料齐套/前置工序完工） |
| latestEndDateT | 最晚必须完工时间 | 后向排产的终点约束（客户交期） |

### 1.2 工作中心时间片（ErpApsTimeSlot）

排产引擎将工作中心的可利用时间划分为离散时间片：

| 字段 | 含义 |
|------|------|
| machineId | 工作中心（→ErpMfgWorkcenter） |
| startTime/endTime | 时间片起止 |
| isAvailable | 是否可用（true=可用 / false=维护停机） |
| bookedOperationId | 已占用工序（→ErpApsOperationOrder，null 时空闲） |

> 时间片由工作中心日历（班次/节假日）+ ErpApsConstraint（维护约束）生成。
> 🟢 Axelor `Machine.xml` 工作中心日历 + 产能配置。

---

## 二、前向排产算法

### 2.1 算法描述

从 OperationOrder 的 `earliestStartDateT`（或 `plannedStartDateT` 兜底）开始，沿时间轴正向寻找工作中心的可利用时间槽，分配后计算完工时间。

### 2.2 输入/输出

| 输入 | 说明 |
|------|------|
| OperationOrder 列表 | 待排产的工序，已按优先级排序 |
| WorkCenter 时间片 | 各工作中心可用时段（考虑维护/节假日） |
| 工序顺序约束 | 同一 WorkOrder 下前工序完成后下工序才可开工 |

| 输出 | 说明 |
|------|------|
| plannedStartDateT/plannedEndDateT | 每个工序的计划时间 |
| status→PLANNED | 排产完成标记 |
| 产能冲突报告 | 无法安排时输出冲突详情 |

### 2.3 算法步骤

```
FORWARD_SCHEDULE(operationOrders, workCenters):
    1. 将 operationOrders 按 (priority ASC, sequence ASC) 排序
    2. 初始化每个工作中心的时间轴指针 t_ptr[machineId] = 当前时间

    FOR each op IN sortedOperations:
        3. 获取 op 对应的工作中心 machine = op.machineId
        4. 获取 earliestStart = op.earliestStartDateT
            若 earliestStart 为空，使用 now 或 op.plannedStartDateT

        5. 计算 duration = op.setupTime + op.runtimePerUnit × op.qty

        6. 从 t_ptr[machine] 开始，在工作中心日历中向后查找
           一个连续的可用时段 (timeSlot)：
             起点 = max(earliestStart, t_ptr[machine])
             终点 = 起点 + duration
             需满足：timeSlot 内无维护停机、无其他工序占用

        7. 若找到可用时段：
            op.plannedStartDateT = 起点
            op.plannedEndDateT = 终点
            t_ptr[machine] = 终点 + 换模缓冲（setupTime 已在 duration 中）
            标记 timeSlot 为已占用
            对于下一个工序（sequence+1），earliestStartDateT = 终点

        8. 若未找到可用时段：
            标记 op 为 "UNSCHEDULABLE"
            记录冲突原因（无连续可用时段）
            继续下一个工序

    RETURN (scheduledOps, unscheduledOps, conflictReport)
```

### 2.4 前置工序约束

同一 WorkOrder 的工序存在 sequence 依赖：

```
工序 10（OP-0010）: 下料    machine=A  duration=30min
    │
    └─► 工序 20（OP-0020）: 车削    machine=B  duration=60min
            │                  earliestStartDateT ≥ OP-0010. plannedEndDateT
            │
            └─► 工序 30（OP-0030）: 热处理  machine=C  duration=120min
                     │
                     └─► 工序 40（OP-0040）: 检验    machine=A  duration=15min
```

每个工序的 `earliestStartDateT` = `MAX(上工序. plannedEndDateT, 工作中心最早可用时间)`。

### 2.5 算法复杂度

`O(n × m)`，其中 n = 工序数，m = 每工作中心需扫描的时间片数。

---

## 三、后向排产算法

### 3.1 算法描述

从 `latestEndDateT`（通常为客户交期映射到末道工序的 deadline）出发，沿时间轴反向寻找时间槽，推算出每道工序的最晚开工时间。

### 3.2 输入/输出

| 输入 | 说明 |
|------|------|
| OperationOrder 列表 | 同一 WorkOrder 的工序链 |
| latestEndDateT | 末道工序的最晚完工时间（从客户交期映射） |
| WorkCenter 时间片 | 同前向排产 |

| 输出 | 说明 |
|------|------|
| plannedStartDateT/plannedEndDateT | 逆向推算的时间 |
| feasible | 是否能在 deadline 内完成 |
| 交期不可达告警 | 当 earliestStartDateT 早于当前时间时 |

### 3.3 算法步骤

```
BACKWARD_SCHEDULE(operationOrders, latestEndDateT):
    1. 将 operationOrders 按 sequence DESC 排序（从末道工序开始）
    2. 初始化 currentDeadline = latestEndDateT

    FOR each op IN reversedOperations:
        3. duration = op.setupTime + op.runtimePerUnit × op.qty
        4. machine = op.machineId

        5. 从 currentDeadline 开始，在工作中心日历中反向查找
           一个连续的可用时段 (timeSlot)：
             终点 = currentDeadline
             起点 = 终点 - duration
             需满足：timeSlot 完全可用，且起点不早于前工序结束

        6. 若找到可用时段：
            op.plannedEndDateT = 终点
            op.plannedStartDateT = 起点
            currentDeadline = 起点（作为前工序的 latestEndDateT）
            标记 timeSlot 为已占用

        7. 若未找到：
            标记 "LATE"（交期不可达）
            返回当前排产结果 + 交期差距报告

    RETURN (scheduledOps, isFeasible, gapReport)
```

### 3.4 交期不可达处理

```
后向排产结果：末道工序最晚需在 7/15 完成
    │
    ├─► 前向推算头道工序最晚开工为 7/10
    │
    ├─► 但头道工序 earliestStartDateT 为 7/12（物料 7/12 到）
    │
    └─► 交期差距 = 7天（需压缩周期或与客户协商延迟）
```

---

## 四、优先级排产

### 4.1 优先级排序规则

| 规则 | 排序键 | 适用场景 |
|------|--------|----------|
| 优先级数字 | priority ASC | 计划员手工排优先级（0=最高，999=最低） |
| 最早交期（EDD） | requiredEndDate ASC | 按客户交期紧迫度排序 |
| 关键比率（CR） | (dueDate - now) / remainingWork ASC | CR 越小越紧迫 |
| 最短加工时间（SPT） | totalDuration ASC | 先完成短工序（减少在制品） |
| 客户优先级 | 客户等级 ASC | VIP 客户优先 |

### 4.2 默认排序策略

```
排序策略：priority ASC → requiredEndDate ASC → sequence ASC
    │
    1. 优先级数字：计划员或紧急工单标记
    2. 要求交期：同一优先级内交期早的优先
    3. 工序顺序：同一工单内按 sequence
```

---

## 五、有限产能约束

### 5.1 约束规则

| 约束 | 规则 | 算法影响 |
|------|------|----------|
| 工作中心唯一性 | 同一工作中心同一时间只安排一个工序 | 时间槽检查 |
| 换模时间 | 相邻工序在不同产品间切换需 setupTime | 作为 duration 的一部分 |
| 维护停机 | 维护时间不可排产 | 时间片标记为不可用 |
| 并联产能 | 同一工作中心多个工位同时生产（特殊场景） | 需 capacity 参数（默认=1） |
| 人员约束 | 指定操作工在同一时间只可操作一台设备 | 可选约束（人机绑定） |

### 5.2 产能并行度

| 字段 | 含义 | 默认 |
|------|------|------|
| ErpMfgWorkcenter.capacity | 并行工位数量 | 1 |
| ErpMfgWorkcenter.calendarId | 工作日历（班次/节假日） | — |

当 `capacity > 1` 时，同一工作中心同一时间可安排 `capacity` 个工序并行。

---

## 六、插单与重排

### 6.1 插单流程

```
RUSH_ORDER_INSERTION(newOperationOrder):
    1. 检测新工序的时间窗口
        ├─ 前向：从 earliestStartDateT 开始寻找最近可用时段
        └─ 后向：从 latestEndDateT 开始逆向寻找

    2. 若找到空闲时段且不影响已有 PLANNED 工序：
        └─ 直接插入，标记 PLANNED

    3. 若需要抢占已有 PLANNED 工序的时段：
        ├─ 比较新工序优先级 vs 被影响工序优先级
        │
        ├─ 若新单优先级更高：
        │   ├─ 确定影响时间窗口
        │   │   └─ 窗口范围 = [newOp.earliestStart, newOp.latestEnd + buffer]
        │   │
        │   ├─ 窗口内受影响工序（优先级低于新单）→ DRAFT
        │   │
        │   ├─ 窗口内不受影响工序（优先级高于新单）→ 保留
        │   │
        │   └─ 重新排产窗口内所有 DRAFT 工序（包含新单）
        │
        └─ 若新单优先级低于所有已有工序：
            └─ 标记新单为 "WAITING" 或建议下个排产窗口

    4. 重排完成后，发布排产变更事件
```

### 6.2 区间重排范围控制

```
区间重排核心原则：最小影响范围

输入：
  - 新订单 OperationOrder: newOp
  - 工作中心: machineId
  - 影响时间窗口: [windowStart, windowEnd]

仅影响：
  ✓ 同一工作中心、同一时间窗口内的 PLANNED 工序
  ✓ 优先级低于新单的工序
  ✗ 窗口外的工序（完全不受影响）
  ✗ 已 IN_PROGRESS 的工序（不可回退）
  ✗ 优先级高于新单的工序（保留不动）
```

> 🟢 Axelor `ManufOrder.xml` 重排支持（人工触发+自动插单检测）。

---

## 七、ATP/CTP 交期承诺

### 7.1 ATP（Available-to-Promise）

```
ATP_CHECK(materialId, qty, earliestShipDate):
    1. 检索物料库存（on-hand + 在产 + 在途）
    2. 检索该时间段内已有预约（销售订单锁定）
    3. 可用量 = 现有库存 + 计划入库 − 已预约量 − 安全库存
    4. 若可用量 ≥ qty：
        返回 earliestShipDate（立即承诺）
    5. 若可用量不足：
        触发 CTP 检查
```

### 7.2 CTP（Capable-to-Promise）

```
CTP_CHECK(materialId, qty, desiredDate):
    1. 按物料追溯工艺路线/工作中心
    2. 获取当前排产方案的可用产能
    3. 创建影子 OperationOrder（不持久化）：
        ├─ duration = setupTime + runtimePerUnit × qty
        └─ machineId = 瓶颈工作中心
    4. 在现有排产方案上模拟前向排产：
        ├─ 从当前时间开始
        └─ 考虑已有产能占用
    5. 计算最早可交付日期（earliestCompletionDate）
    6. 若 earliestCompletionDate ≤ desiredDate：
        返回 "交期可行" + 建议交期
    7. 否则：
        返回最早可用交期 + 产能瓶颈详情
```

### 7.3 ATP/CTP 接口定义

```
IErpApsAtpCtpService {
    // 前向：根据数量 + 日期计算最早可交付日期
    earliestCompletionDate(materialId, qty): DateTime

    // 后向：检查期望交期是否可行
    checkFeasibility(materialId, qty, desiredDate): CtpResult

    // 模拟：返回排产后各工序的时间（用于展示承诺排程）
    simulateSchedule(materialId, qty, startDate): List<ScheduledOperation>
}

CtpResult {
    feasible: boolean          // 是否可行
    earliestCompletionDate: DateTime  // 最早可交付
    bottleneckWorkcenter: String     // 瓶颈工作中心（不可行时）
    capacityGap: Decimal            // 产能缺口（分钟，不可行时）
}
```

---

## 八、甘特图数据模型

### 8.1 前端所需数据

```json
{
  "workCenters": [
    {
      "id": "WC-001",
      "name": "CNC-1",
      "capacity": 1,
      "color": "#4A90D9"
    }
  ],
  "operations": [
    {
      "id": "OP-0010",
      "workOrderCode": "MO-2026-001",
      "workCenterId": "WC-001",
      "operationName": "下料",
      "plannedStartDateT": "2026-07-01T08:00:00",
      "plannedEndDateT": "2026-07-01T10:30:00",
      "status": "PLANNED",
      "priority": 1,
      "color": "#7EB8DA",
      "dependencies": []  // 依赖的前置工序 OP id 列表
    },
    {
      "id": "OP-0020",
      "workOrderCode": "MO-2026-001",
      "workCenterId": "WC-002",
      "operationName": "车削",
      "plannedStartDateT": "2026-07-01T10:30:00",
      "plannedEndDateT": "2026-07-01T13:00:00",
      "status": "PLANNED",
      "priority": 1,
      "dependencies": ["OP-0010"]
    }
  ],
  "constraints": [
    {
      "workCenterId": "WC-001",
      "startTime": "2026-07-02T12:00:00",
      "endTime": "2026-07-02T13:00:00",
      "type": "MAINTENANCE",
      "description": "日常保养"
    }
  ],
  "timeRange": {
    "start": "2026-07-01T00:00:00",
    "end": "2026-07-07T00:00:00",
    "granularity": "hour"
  }
}
```

### 8.2 甘特图交互

| 交互 | 说明 |
|------|------|
| 时间轴缩放 | 日/周/月切换 |
| 拖拽调整 | 拖拽块调整 plannedStartDateT（需后端校验产能） |
| 依赖线 | 前置工序→后置工序箭头连线 |
| 右键菜单 | 查看详情 / 取消排产 / 标记完成 |
| 颜色编码 | 状态颜色：DRAFT=灰 / PLANNED=蓝 / IN_PROGRESS=黄 / FINISHED=绿 / CANCELLED=红 |
| 筛选 | 按工作中心/工单/状态筛选 |
| 冲突高亮 | 产能冲突或交期不可达红色高亮 |

### 8.3 甘特图后端服务

```java
// 排产甘特图数据查询
IEtpApsGanttService {
    // 获取指定时间范围内所有工序（按工作中心分组）
    getGanttData(timeRangeStart, timeRangeEnd, machineFilter): GanttData

    // 拖拽更新工序时间（需校验产能）
    dragUpdateOperation(opId, newStartTime): void

    // 获取排产冲突报告
    getConflictReport(scheduleId): ConflictReport
}
```

> 🟢 Axelor production 前端甘特图（React + dhtmlx-gantt 或其他甘特图库）。
> 前端使用 AMIS 甘特图组件（插件）或集成第三方甘特图库（如 dhtmlx-Gantt / Frappe Gantt）。

---

## 九、排产方案管理

### 9.1 版本管理

排产方案（ErpApsSchedule）支持多版本以便对比与回退：

| 版本操作 | 说明 |
|----------|------|
| 创建方案 | 当前排产快照 |
| 保存为版本 | 在调整前保存当前版本 |
| 版本切换 | 回退到历史版本 |
| 版本对比 | 两版本间差异（哪些工序的时间变了） |

### 9.2 方案状态

```
DRAFT（排产中）
  │
  ├─► 发布 → PUBLISHED（已发布，执行参照此版本）
  │
  ├─► 存档 → ARCHIVED（历史版本）
  │
DRAFT/PUBLISHED → ARCHIVED（不再使用）
```

---

## 十、算法配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-aps.default-scheduling-mode` | FORWARD | 前向/后向排产默认模式 |
| `erp-aps.priority-rule` | PRIORITY | 排序规则：PRIORITY / EDD / CR / SPT |
| `erp-aps.time-bucket-minutes` | 15 | 排产时间槽粒度 |
| `erp-aps.auto-reschedule-on-insert` | true | 插单时自动区间重排 |
| `erp-aps.max-reschedule-window-days` | 30 | 重排最大展望期 |
| `erp-aps.buffer-minutes-between-ops` | 5 | 相邻工序间缓冲时间（分钟） |

---

## 十一、关键业务规则总结

1. **同一工作中心同一时间只能安排一个工序**（capacity=1 时）
2. **前置工序必须完成才能开始下工序**（sequence 顺序约束）
3. **前向排产从 earliestStartDateT 正向填充，后向排产从 latestEndDateT 逆向填充**
4. **插单只触发区间重排**（不全局重排，避免牛顿效应）
5. **已 IN_PROGRESS 的工序不可重排**
6. **同一个工作中心的 PLANNED 工序在插单时可回退至 DRAFT**（仅限优先级低于新单的工序）
7. **ATP/CTP 仅做模拟**（不持久化排产结果）
8. **甘特图拖拽调整需后端产能校验**（前端不信任用户输入）

## 参考

- `docs/design/aps/README.md`（APS 域基础实体）
- `docs/design/aps/use-cases.md`（排产用例）
- `docs/design/aps/state-machine.md`（OperationOrder 状态机）
- `docs/design/manufacturing/crp.md`（CRP 与 APS 边界）
- `docs/design/manufacturing/mrp.md`
- 🟢 Axelor `OperationOrder.xml`（工序排产字段 + 排产引擎）
- 🟢 Odoo `mrp_workcenter`（产能日历配置）
- ⚪ APICS Dictionary（ATP/CTP 定义）
