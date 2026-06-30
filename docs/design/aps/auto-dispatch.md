# 自动派工（Auto Dispatch）

## 目的

详细设计自动派工功能：根据预定义规则（物料已齐套 + 机器可用 + 操作工可用），将 PLANNED 状态的 OperationOrder 自动推动为 IN_PROGRESS，减少人工派工等待时间。支持按工作中心配置规则、手动覆盖/暂停派工、派工操作日志。

## 设计边界

- 本设计负责：自动派工触发条件检查、派工规则配置、派工执行引擎、派工日志、手动强制派工/暂停/保持。
- **与 scheduling 的边界**：排产（scheduling）负责计算 OperationOrder 的 `plannedStartDateT`；自动派工在排产完成后，根据规则自动推进到执行状态。
- **与 manufacturing 的边界**：IN_PROGRESS 后的实际报工在 `manufacturing/JobCard` 中，自动派工不涉及报工。
- **与 inventory 的边界**：物料齐套检查调用 inventory 域的可用量接口。
- 本设计不负责：排产计算、报工管理、工作中心日历管理。

---

## 一、自动派工规则（ErpApsDispatchRule）

### 1.1 实体定义

| 字段 | 含义 |
|------|------|
| id/orgId | 标准 |
| workcenterId | 工作中心（→ErpMfgWorkcenter） |
| ruleName | 规则名称 |
| enableAuto | 是否启用自动派工（true=启用，false=该工作中心不自动派工） |
| requireMaterial | 派工前是否需要检查物料齐套（true=物料检查通过才派工） |
| requireOperator | 派工前是否需要检查操作工可用（true=至少一名指定操作工在岗） |
| requireTooling | 派工前是否需要检查工装夹具可用（true=工装就绪才派工） |
| maxLookaheadMinutes | 最大前瞻窗口（分钟）：仅对 plannedStartDateT 在当前时间 + maxLookaheadMinutes 内的工序进行自动派工 |
| dispatchAheadMinutes | 提前派工时间（允许在 plannedStartDateT 前多少分钟自动派工，如提前 15 分钟） |
| autoConfirmMaterial | 物料齐套后是否自动确认（true=自动确认，false=需人工确认） |
| maxConcurrentOps | 最大并行工序数（该工作中心同一时间最多派工数量，默认为 capacity×1.5） |
| priorityThreshold | 仅对优先级 ≤ 此值的工序自动派工（如仅自动派工 priority ≤ 50 的工序） |
| enabledHours | 允许自动派工的时段（JSON，如 [{"start":"08:00","end":"17:00"}]，空=全天） |
| holdUntil | 暂停派工直到此时间（管理员设置"暂停到明天 8:00"） |
| holdReason | 暂停原因（管理员设置时填写） |
| 标准审计字段 | |

### 1.2 规则默认值

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| enableAuto | true | 默认启用自动派工 |
| requireMaterial | true | 默认检查物料齐套 |
| requireOperator | true | 默认检查操作工可用 |
| maxLookaheadMinutes | 120 | 默认前瞻 2 小时 |
| dispatchAheadMinutes | 15 | 默认提前 15 分钟派工 |
| maxConcurrentOps | 工作中心 capacity | 默认最多并行工位数 |

> 🟢 Manufacturing Execution Systems（MES）典型派工逻辑：物料 + 资源 + 人员齐备 → 自动派工。

---

## 二、触发条件检查

### 2.1 检查流程

自动派工引擎周期性运行（如每分钟扫描），对每个工作中心执行：

```
DISPATCH_CHECK(workcenterId):
    1. 加载该工作中心的 ErpApsDispatchRule
        └─ 若 enableAuto=false → 跳过此工作中心
        └─ 若有 holdUntil > now → 跳过（暂停中）

    2. 查询该工作中心 eligible 的 OperationOrder：
        条件：status = PLANNED
           AND plannedStartDateT <= now + maxLookaheadMinutes
           AND plannedStartDateT >= now - dispatchAheadMinutes  // 不派工已过期的
           AND priority <= priorityThreshold（若有配置）
           AND 该工作中心当前执行中工序数 < maxConcurrentOps

    3. 按 (plannedStartDateT ASC, priority ASC) 排序

    4. FOR each eligibleOp IN sortedList:
        │
        ├─► if requireMaterial:
        │     checkMaterialAvailability(op)
        │     └─ 不满足 → continue（跳过此工序）
        │
        ├─► if requireOperator:
        │     checkOperatorAvailability(workcenterId)
        │     └─ 不满足 → continue（跳过此工序）
        │
        ├─► if requireTooling:
        │     checkToolingAvailability(op)
        │     └─ 不满足 → continue（跳过此工序）
        │
        ├─► 所有条件满足 → DISPATCH(op)
        │       ├─ OperationOrder.status = IN_PROGRESS
        │       ├─ plannedStartDateT 微调为当前时间（可选）
        │       └─ 创建日志记录
        │
        └─► 继续下一个 eligibleOp（直到 maxConcurrentOps 满额）
```

### 2.2 物料齐套检查

调用 inventory/齐套接口（`IErpMfgMaterialAvailabilityService`）：

| 入参 | 说明 |
|------|------|
| operationOrderId | 工序 ID，用以追溯 BOM 子件需求 |
| warehouseId | 领料仓库（从 WorkOrder 继承） |

| 返回 | 说明 |
|------|------|
| isAvailable | 是否齐套 |
| shortageItems | 缺料清单（物料 + 缺料数量 + 预计可用时间） |

> 检查范围：该工序（及前置已开工工序）累积需要消耗的全部子件物料。
> 🟢 Axelor `StockAvailabilityService.java` 齐套检查。

### 2.3 操作工可用检查

| 检查项 | 说明 |
|--------|------|
| 工作中心是否配置了操作工 | ErpMfgWorkcenterSkill（操作工技能/资格） |
| 当班操作工是否在岗 | 查当天的排班（human-resource/shift-scheduling） |
| 操作工是否已分配其他任务 | 该操作工当前 IN_PROGRESS 的工序数 < 允许上限 |

> 简单场景：工作中心至少有 1 名在岗操作工即可派工。
> 复杂场景：需匹配技能等级（未来扩展）。

---

## 三、派工执行

### 3.1 自动派工操作

```
DISPATCH(operationOrder):
    1. 写入 OperationOrder：
        ├─ status = IN_PROGRESS
        ├─ dispatchTime = now
        ├─ dispatchType = AUTO
        └─ dispatchRuleId = 匹配的 ErpApsDispatchRule.id
    2. 创建 DispatchLog（见 §四）
    3. 发布 OperationOrderDispatched 事件
        └─ 订阅方：
            ├─ manufacturing → 创建 JobCard（可开始报工）
            ├─ inventory → 锁定子件库存（消耗预留）
            └─ notification → 通知操作工
```

### 3.2 手动派工

计划员/调度员可手动触发派工（覆盖/补充自动派工）：

```
手动派工入口：
    ├─► 在 OperationOrder 列表/甘特图中右键 → "开始执行"
    │
    ├─► 系统执行条件检查（同自动派工逻辑，但跳过 rule.enableAuto 检查）
    │
    ├─► 强制跳过条件检查（manualOverride = true）：
    │       └─ 即使物料未齐套也可强制派工（需填写原因）
    │
    └─► 写入 OperationOrder：
            ├─ status = IN_PROGRESS
            ├─ dispatchTime = now
            ├─ dispatchType = MANUAL
            └─ manualOverrideReason = "紧急插单，物料在途"
```

### 3.3 派工保持（Hold）

计划员可对某工序设置"保持"（不自动派工）：

```
HOLD(op):
    └─ OperationOrder.dispatchHold = true
    └─ OperationOrder.dispatchHoldReason = "等待外协件到货"

UNHOLD(op):
    └─ OperationOrder.dispatchHold = false（清除保持状态）
    └─ 重新进入自动派工检查循环
```

> 保持中的工序在自动派工扫描中被跳过。

---

## 四、派工日志（ErpApsDispatchLog）

### 4.1 实体定义

| 字段 | 含义 |
|------|------|
| id/orgId | 标准 |
| operationOrderId | 派工的工序（→ErpApsOperationOrder） |
| workcenterId | 工作中心 |
| dispatchType | 派工类型 dict：AUTO（自动派工）/ MANUAL（手动派工） / HOLD（保持）/ UNHOLD（解除保持） |
| previousStatus | 派工前状态 |
| newStatus | 派工后状态 |
| conditionCheckResult | 条件检查结果（JSON） |
| dispatchedBy | 派工人（AUTO=系统，MANUAL→ErpHrEmployee） |
| dispatchedAt | 派工时间 |
| materialAvailable | 物料齐套结果（true/false/null） |
| operatorAvailable | 操作工可用结果（true/false/null） |
| toolingAvailable | 工装可用结果（true/false/null） |
| note | 备注 |

### 4.2 日志查询

| 查询维度 | 用途 |
|----------|------|
| 工作中心 × 日期 | 统计各工作中心的自动/手动派工比例 |
| 操作工 × 日期 | 操作工派工记录关联 |
| 工单 × 工序 | 追溯某工单的派工时间线 |
| 失败记录 | 条件检查失败的工序及原因（用于诊断） |

---

## 五、规则管理界面

### 5.1 工作中心规则配置

```
工作中心 → 自动派工规则页
    │
    ├─► 启用/禁用自动派工（enableAuto）
    │
    ├─► 条件开关：
    │       ☑ 检查物料齐套（requireMaterial）
    │       ☑ 检查操作工可用（requireOperator）
    │       ☐ 检查工装可用（requireTooling）
    │
    ├─► 参数配置：
    │       maxLookaheadMinutes: [120]
    │       dispatchAheadMinutes: [15]
    │       maxConcurrentOps: [3]
    │       priorityThreshold: [50]
    │
    └─► 暂停自动派工：
            holdUntil: [2026-07-03 08:00]
            holdReason: "设备检修"
```

### 5.2 全局开关

| 控制 | 说明 |
|------|------|
| 全局启用/停用自动派工 | 紧急情况下一键停止所有自动派工 |
| 按工作中心覆盖 | 各工作中心独立设置 |

---

## 六、异常处理

| 场景 | 处理 |
|------|------|
| 自动派工后发现物料不足 | 操作工在 JobCard 报工时标记"缺料" → OperationOrder 暂停（status→ON_HOLD） → 通知计划员 |
| 自动派工后操作工未到岗 | 超时（可配置，如 30 分钟）未开始报工 → 告警 → 重新分配 |
| 自动派工引擎故障 | 管理台告警，提示人工派工 |
| 并发派工冲突（同一工序被两次派工） | 乐观锁检测，第二次失败 |
| 工作中心停机未在日历中登记 | 手动暂停该工作中心的自动派工（hold） |

---

## 七、报表与监控

| 功能 | 内容 |
|------|------|
| 实时派工仪表盘 | 当前各工作中心自动/手动派工数、等待派工序数 |
| 派工效率报表 | 平均排产→派工时间间隔（自动 vs 手动对比） |
| 规则命中率 | 自动派工条件检查通过率（各条件维度） |
| 异常派工报表 | 强制派工记录、派工后异常记录 |
| 操作工利用率 | 操作工忙闲比（基于派工数据） |

---

## 八、关键业务规则总结

1. **自动派工默认启用**：可逐工作中心关闭（enableAuto=false）
2. **条件检查默认全开**：物料 + 操作工 + 工装三项检查均可独立开关
3. **派工前瞻窗口限制**：仅对 plannedStartDateT 在 `now + maxLookaheadMinutes` 内的工序派工
4. **自动派工不创建新产能**：只改变 OperationOrder 状态（PLANNED→IN_PROGRESS），不影响已排产时间槽
5. **手动派工可跳过条件检查**：需记录 overrideReason
6. **保持机制**：单个工序 hold 或整工作中心暂停（holdUntil）
7. **自动派工记录完整日志**：每次派工/保持/解除保持均记入 ErpApsDispatchLog

## 参考

- `docs/design/aps/scheduling.md`（排产算法——派工的前置条件）
- `docs/design/aps/use-cases.md`（APS 排产用例）
- `docs/design/aps/state-machine.md`（PLANNED→IN_PROGRESS 迁移）
- `docs/design/aps/alternative-routing.md`（替代路由——派工前已确定工作中心）
- `docs/design/manufacturing/bom-and-routing.md`（BOM 与物料需求）
- `docs/design/manufacturing/material-reservation.md`（物料预留与齐套）
- 🟢 MES dispatch logic（通用自动派工模式：material+machine+operator→dispatch）
- 🟢 Axelor `ManufOrder.xml`（工单派工状态管理）
