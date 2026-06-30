# APS 域状态机

> **设计要点依据**：本状态机按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度组织。审查本状态机时使用该提示词。
>
> APS 域核心状态对象：**工序工单**（OperationOrder）。

## 适用对象：工序工单（OperationOrder）

### 1. 状态定义

| 状态 | 业务含义（等待什么） | 业务单据影响 |
|------|----------------------|--------------|
| 草稿（DRAFT） | 从 WorkOrder 创建但未排产，等待 APS 运算 | 不参与排产约束检查 |
| 已排程（PLANNED） | APS 已完成排产赋值（plannedStart/EndDateT），等待车间确认 | 工作中心产能已被占用 |
| 执行中（IN_PROGRESS） | 车间开始执行，等待完工 | 工作中心产能被占用，报工中 |
| 已完成（FINISHED） | 终态：工序已完工 | 释放工作中心产能，触发下工序/工单联动 |
| 已取消（CANCELLED） | 终态：工序取消（工单变更或取消） | 释放产能预留 |

### 2. 迁移完整性

```
草稿 (DRAFT)
  ├─ APS 排产 → 已排程 (PLANNED)
  └─ 取消 → 已取消 (CANCELLED)

已排程 (PLANNED)
  ├─ 开始执行 → 执行中 (IN_PROGRESS)
  ├─ 重排（插单变更）→ 草稿 (DRAFT)  [回退重新排产]
  └─ 取消 → 已取消 (CANCELLED)

执行中 (IN_PROGRESS)
  ├─ 完工 → 已完成 (FINISHED)
  └─ 异常终止 → 已取消 (CANCELLED)
```

| 迁移 | 触发人 | 前置条件 | 结果 |
|------|--------|----------|------|
| DRAFT→PLANNED | APS 引擎 / 计划员 | 排产销入了 plannedStartDateT/plannedEndDateT | 锁定工作中心产能时段 |
| PLANNED→IN_PROGRESS | 车间调度 | 物料已齐套（可选校验） | JobCard 可开始报工 |
| IN_PROGRESS→FINISHED | 车间作业人员 | 实际数量 qty 全部报工 | 产能释放，触发下工序或工单收尾 |
| PLANNED|IN_PROGRESS→CANCELLED | 计划员 / 系统 | 工单取消或工艺路线变更 | 释放预留产能 |
| PLANNED→DRAFT | APS 引擎（重排） | 区间重排触发 | 解锁产能，待重新排产 |

### 3. 终态与恢复

- 终态：`已完成（FINISHED）`、`已取消（CANCELLED）`。
- 终态不可直接恢复。若需修改，重新创建 OperationOrder。
- 已 FINISHED 的 OperationOrder 不可重排或修改。

### 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| 工序开工时物料未齐套 | 在 PLANNED→IN_PROGRESS 前增加物料齐套校验（可选） |
| 工作中心故障/停机（未在约束中登记） | 工单暂停，计划员手动调整（PLANNED→DRAFT 重排） |
| 插单/急单 | 触发区间重排，受影响区间内的 OperationOrder 回退到 DRAFT |
| 并发排产同一工作中心 | 乐观锁或资源锁防止产能双倍占用 |
| 实际数量超过排产 qty | 系统不允许超过 qty，需拆开工单 |

### 5. 可达性

- 从 DRAFT 可达所有状态；所有已定义状态至少有一条入边。
- PLANNED→DRAFT（回退）路径合法且必要（重排场景）。

### 6. 角色与权限

| 迁移 | 执行角色 |
|------|----------|
| DRAFT→PLANNED（自动排产） | 系统（APS 引擎） |
| DRAFT→PLANNED（手工调整） | 计划员 |
| PLANNED→IN_PROGRESS | 车间调度员 |
| IN_PROGRESS→FINISHED | 车间作业人员（通过报工） |
| PLANNED|IN_PROGRESS→CANCELLED | 计划员 / 生产主管 |
| PLANNED→DRAFT（重排） | APS 引擎 / 计划员 |

危险操作：
- **取消执行中的工序**：需生产主管审批，因已产生实际报工数据。
- **重排已 PLANNED 的工序**：自动重排时影响范围需限定在区间内。

### 7. 外部依赖

| 外部场景 | 内部处理 |
|----------|----------|
| WorkOrder 下达 | 触发创建下属 OperationOrder（DRAFT） |
| WorkOrder 取消 | 级联取消关联的所有 OperationOrder |
| 工作中心维护约束 | 读取 ErpApsConstraint 作为排产输入 |
| 插单/急单 | 外部事件触发区间重排（有限范围） |

外部触发渠道：
- WorkOrder 下达（main → aps）。
- 计划员手工创建/调整。
- 插单事件（sales → aps）。

### 8. TODO / 任务策略

| 状态 | 是否产生 TODO | TODO 类型 |
|------|---------------|-----------|
| DRAFT | 否 | — |
| PLANNED | 是 | pool（车间调度员）—— 待执行工序 |
| IN_PROGRESS | 是 | assigned（操作工）—— 正在执行 |
| FINISHED | 否 | — |
| CANCELLED | 否 | — |

避免"已排程工序长期未开工"：PLANNED 超过计划开工时间 24h 未改为 IN_PROGRESS 时产生催办。

### 9. 场景演练

#### 场景 A：前向排产

1. 计划员新建排产方案（Schedule），模式 FORWARD。
2. 选择一批 DRAFT 的 OperationOrder。
3. APS 引擎从 WorkOrder 的 plannedStartDate 出发，正向填充每个工序的 plannedStartDateT/plannedEndDateT，考虑工作中心产能约束和工序间顺序约束。
4. 运算完成 → OperationOrder.status = PLANNED。
5. 车间调度员按计划开工。

#### 场景 B：后向排产

1. 计划员新建排产方案，模式 BACKWARD。
2. 从客户要求的交期（或 WorkOrder.requiredEndDate）出发，逆向推算每个工序的最晚开工/完工时间。
3. 运算结果 → PLANNED。若后向排产发现产能不足无法满足交期，系统告警。

#### 场景 C：插单/急单重排

1. 新插急单创建 OperationOrder（DRAFT）。
2. 触发区间重排：仅受影响时间窗口内（新单前后）的 OperationOrder 回退到 DRAFT，其他不变（避免牛顿效应）。
3. APS 引擎重新计算受影响区间的排程。
4. 重排完成后，新单 + 受影响工序重新 → PLANNED。

### 10. 与设计文档一致性

- 状态定义见 `aps/README.md` §ErpApsOperationOrder。
- 状态码归 `model/app-erp-aps.orm.xml` dict `erp-aps/operation-order-status`。
- 重排策略参考 `aps/README.md` 业务规则第 5 条。

## 审查提示

审查本状态机时，使用 `docs/skills/state-machine-business-review-prompt.md`，重点检查：
- PLANNED→DRAFT 重排回退路径是否合法（仅 APS 引擎或计划员可执行）。
- 插单重排的范围限定（区间重排而非全局重排）。
- 执行中工序取消是否需要生产主管审批。
- 工作中心产能的双重占用防止机制。
