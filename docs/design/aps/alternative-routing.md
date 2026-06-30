# 替代工艺路线（Alternative Routing）

## 目的

详细设计替代工艺路线功能：同一工序可配置多个可用工作中心（主选 + 备选），排产时按优先级自动选择，主选被占用或停机时自动回退到备选，记录备选与主选的换模时间差异。提高排产灵活性和产能利用率。

## 设计边界

- 本设计负责：替代路由的实体定义、优先级排序、排产引擎调用替代路由的逻辑、换模时间差计算、主选过载时自动降级。
- **与 scheduling 的边界**：本文定义"替代路由的选择逻辑"；实际排产时间计算（前向/后向）复用 `aps/scheduling.md` 的排产算法。
- **与 manufacturing/routing 的边界**：工艺路线（Routing）定义工序与工作中心的默认绑定关系；替代路由扩展这一关系为 1:N（一个工序可对应多个工作中心）。
- 本设计不负责：工作中心主数据维护、工艺路线创建、排产算法本身。

---

## 一、替代路由实体（ErpApsOpRouting）

### 1.1 实体定义

| 字段 | 含义 |
|------|------|
| id/orgId | 标准 |
| operationId | 工序（→ErpApsOperation 或 BomOperation） |
| machineId | 工作中心（→ErpMfgWorkcenter） |
| priority | 优先级（数字越小越优先，1=首选，2=第一备选，3=第二备选...） |
| setupTimeDelta | 相对主选的换模时间差（分钟，正数=比主选长，负数=比主选短） |
| runtimePerUnitDelta | 相对主选的单件加工时间差（分钟） |
| isDefault | 是否为默认路由（主选） |
| isEnabled | 是否启用 |
| effectiveFrom/effectiveTo | 有效期 |
| minBatchQty | 最小批量（低于此批量不适用本路由） |
| maxBatchQty | 最大批量（高于此批量不适用本路由） |
| 标准审计字段 | |

### 1.2 数据模型约束

```
约束：同一 operationId + machineId 唯一
约束：每个 operationId 有且仅有一个 isDefault=true 的记录（主选）
约束：priority 从 1 开始连续递增（1=主选，2=备选1，3=备选2...）
```

### 1.3 示例数据

| operationId | machineId | priority | setupTimeDelta | runtimePerUnitDelta | isDefault | 说明 |
|-------------|-----------|----------|---------------|-------------------|-----------|------|
| OP-0010（下料） | WC-001（CNC-1） | 1 | 0 | 0 | true | 主选 |
| OP-0010（下料） | WC-002（CNC-2） | 2 | +5 | +0.5 | false | 备选1：换模多5分钟，单件慢0.5分钟 |
| OP-0010（下料） | WC-005（激光切割） | 3 | +30 | -1 | false | 备选2：换模多30分钟但单件快1分钟 |
| OP-0020（车削） | WC-010（车床-A） | 1 | 0 | 0 | true | 主选 |
| OP-0020（车削） | WC-011（车床-B） | 2 | +2 | 0 | false | 备选1：换模多2分钟，单件不变 |

> 🟢 Axelor `Operation.xml` 支持"Alternate Work Center"替代工作中心配置。

---

## 二、路由选择逻辑

### 2.1 排产时路由选择流程

排产引擎在分配工作中心时，按以下逻辑选择路由：

```
SELECT_ROUTING(operationOrder):
    1. 获取 operationId 的所有启用路由（ErpApsOpRouting），按 priority ASC 排序
    2. 校验批量约束 minBatchQty / maxBatchQty
    3. 按优先级逐个尝试：
        ├─► 检查工作中心当前是否可用（无停机/维护）
        │
        ├─► 在排产时间窗口内检查产能是否充足
        │       ├─ 计算 duration = (setupTime + setupTimeDelta) + (runtimePerUnit + runtimePerUnitDelta) × qty
        │       └─ 在目标工作中心日历中寻找可用时间段
        │
        ├─► 如果可用：
        │       ├─ 选择该路由
        │       ├─ 更新 operationOrder.machineId = 选中的 machineId
        │       ├─ 更新 operationOrder.setupTime = 原始 setupTime + setupTimeDelta
        │       ├─ 更新 operationOrder.runtimePerUnit = 原始 runtimePerUnit + runtimePerUnitDelta
        │       └─ 记录 selectedRoutingId = ErpApsOpRouting.id
        │
        └─ 如果不可用：尝试下一个优先级的备选路由
              │
              └─ 全部备选不可用 → 标记为 "UNSCHEDULABLE"，记录无可用路由原因
```

### 2.2 路由选择结果记录

OperationOrder 新增字段：

| 字段 | 含义 |
|------|------|
| selectedRoutingId | 最终选用的路由（→ErpApsOpRouting.id） |
| routingSelectionReason | 选择原因 dict：DEFAULT（主选）/ PRIMARY_OVERBOOKED（主选过载）/ PRIMARY_DOWN（主选停机）/ BATCH_CONSTRAINT（批量约束） |

---

## 三、设置时间差异计算

### 3.1 换模时间差异

替代工作中心由于设备型号/工装不同，换模时间可能不同：

```
实际 setupTime = 工序标准 setupTime + ErpApsOpRouting.setupTimeDelta
```

| 场景 | setupTimeDelta | 说明 |
|------|---------------|------|
| 同型号设备 | 0 | 换模时间一致 |
| 老旧设备 | +10 | 换模比主选多 10 分钟 |
| 新型设备 | -5 | 换模比主选少 5 分钟（更快） |
| 激光切割（跨工艺） | +30 | 完全不同工艺，换模时间长 |

### 3.2 单件加工时间差异

```
实际 runtimePerUnit = 工序标准 runtimePerUnit + ErpApsOpRouting.runtimePerUnitDelta
```

### 3.3 总耗时计算

```
totalDuration = (setupTime + setupTimeDelta) + (runtimePerUnit + runtimePerUnitDelta) × qty
```

> 差异值可正可负。负值表示备选设备效率更高（如新设备）。

---

## 四、主选过载自动降级

### 4.1 过载检测

排产引擎在执行时，对每个工序尝试主选路由。若主选工作中心在当前排产窗口内无可用产能，触发自动降级：

```
AUTO_DEGRADE(operationOrder):
    1. 尝试主选路由（priority=1）
        │
        ├─► 主选有可用产能 → 选择主选
        │
        └─► 主选无可用产能 →
                │
                ├─► 检查 operationOrder 是否允许降级（allowFallback 字段）
                │       └─ 不允许降级 → 保持 UNSCHEDULABLE，告警
                │
                └─► 允许降级 →
                        ├─► 按 priority 逐个检查备选路由
                        ├─► 选择第一个有可用产能的备选
                        └─► 若全部备选不可用 → UNSCHEDULABLE
```

### 4.2 降级日志

每次降级记录到排产日志，供计划员审查：

```json
{
  "operationOrderId": "OP-0010",
  "workOrderCode": "MO-2026-001",
  "primaryMachine": "WC-001 (CNC-1)",
  "selectedMachine": "WC-002 (CNC-2)",
  "routingId": "R-1002",
  "degradeReason": "PRIMARY_OVERBOOKED",
  "additionalTimePenalty": 5.5,  // 分钟
  "scheduledAt": "2026-07-01T10:00:00"
}
```

### 4.3 人工强制指定

计划员可强制指定某条非默认路由：

```
计划员界面 → 右键工序 → "选择工作中心"
    │
    ├─► 展示该工序的所有可用路由（含优先级/换模时间差/总耗时预估）
    │
    ├─► 计划员直接选择某个工作中心（覆盖自动路由选择）
    │
    └─► 记录 manualOverride = true
```

---

## 五、排产引擎集成

### 5.1 修改排产算法

在 `aps/scheduling.md` 定义的前向/后向排产算法中，`步骤 3`（获取工作中心）替换为路由选择逻辑：

```
原：获取 op 对应的工作中心 machine = op.machineId
新：machine = SELECT_ROUTING(op)  // 按优先级选择可用路由
    if machine == null:
        标记 UNSCHEDULABLE
        continue
```

### 5.2 插单场景的特殊处理

插单重排时，替代路由的重新选择：

| 场景 | 处理 |
|------|------|
| 插单抢占主选产能 | 被抢占的工序自动触发 SELECT_ROUTING，尝试备选 |
| 插单使用备选 | 不影响其他工序（备选产能通常不与主选重叠） |
| 被抢占工序全部备选不可用 | 标记冲突，由计划员处理 |

---

## 六、报表与分析

| 报表 | 内容 |
|------|------|
| 路由使用率统计 | 各工作中心作为主选/备选被使用的次数和占比 |
| 降级分析 | 有多少工序因主选过载降级到备选，额外耗时总计 |
| 工作中心负载分布 | 主选 vs 备选的实际排产工时分布 |
| 备选效率对比 | 使用备选 vs 主选的实际工时差异 |

---

## 七、关键业务规则总结

1. **每道工序有且只有一个主选路由**（isDefault=true）
2. **路由选择按优先级顺序尝试**，找到第一个可用即停止
3. **允许自动降级**（主选过载/停机 → 备选），可配置
4. **换模时间差异和单件时间差异独立记录**，用于精确计算总耗时
5. **自动降级记录到排产日志**，计划员可审查
6. **计划员可强制指定路由**（manualOverride），覆盖自动选择
7. **插单重排时重新触发路由选择**，可能再次降级

## 参考

- `docs/design/aps/scheduling.md`（排产算法——路由选择集成于此）
- `docs/design/aps/use-cases.md`（APS 排产用例）
- `docs/design/aps/state-machine.md`（OperationOrder 状态机）
- `docs/design/aps/README.md`（APS 域基础实体）
- `docs/design/manufacturing/bom-and-routing.md`（BOM 与工艺路线）
- 🟢 Axelor `Operation.xml` Alternate Work Center + WorkCenter 选择逻辑
- 🟢 SAP Work Center Hierarchy（工作中心层级 + 替代选择）
