# 设备维护域状态机

> **设计要点依据**：本状态机按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度组织。审查本状态机时使用该提示词。
>
> 维护域有两类状态对象：**维护访问**（MaintenanceVisit，计划执行）与**维护请求**（MaintenanceRequest，报修）。

## 适用对象一：维护访问（MaintenanceVisit）

### 1. 状态定义

| 状态 | 业务含义（等待什么） | 设备状态 |
|------|----------------------|----------|
| 草稿（DRAFT） | 已生成（计划自动或请求触发）未排程 | 不变 |
| 已排程（SCHEDULED） | 已安排执行人与时间，等待执行 | 不变 |
| 执行中（IN_PROGRESS） | 正在执行维护 | 维护中 |
| 已完成（COMPLETED） | 终态：维护完成 | 恢复运行中/闲置 |
| 已取消（CANCELLED） | 终态：作废 | 不变（或恢复） |

### 2. 迁移完整性

```
草稿 (DRAFT)
  ├─ 排程 → 已排程 (SCHEDULED)
  │           ├─ 开始执行 → 执行中 (IN_PROGRESS)
  │           │              ├─ 完成 → 已完成 (COMPLETED)
  │           │              └─ 取消 → 已取消 (CANCELLED)
  │           └─ 取消 → 已取消 (CANCELLED)
  └─ 取消 → 已取消 (CANCELLED)
```

| 迁移 | 触发人 | 前置条件 | 结果 |
|------|--------|----------|------|
| DRAFT→SCHEDULED | 维护主管 | 执行人已分配、计划时间已定 | 排程待执行 |
| SCHEDULED→IN_PROGRESS | 维护人员 | 已排程、到达执行时间 | 设备进入"维护中" |
| IN_PROGRESS→COMPLETED | 维护人员 | 维护内容已记录、备件消耗已入账 | 设备恢复，维护历史归档 |
| 任意非终态→CANCELLED | 维护主管 | — | 作废 |

### 3. 终态与恢复

- 终态：`已完成（COMPLETED）`、`已取消（CANCELLED）`。
- 终态不可恢复；若需再次维护，新建维护访问（计划生成或请求触发）。

### 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| 排程冲突（设备/人员同时段已排） | 提示冲突，调整时间 |
| 执行中发现需更多备件 | 中途补领备件（追加出库移动单） |
| 维护中发现额外故障 | 记录到维护内容，或转维护请求处理大故障 |
| 设备不可停机（生产中） | 排程时校验设备生产状态，协商停机窗口 |
| 计划生成的访问无人排程 | 产生 TODO 提醒维护主管 |
| 并发状态变更 | 乐观锁 |

### 5. 可达性

- 从 DRAFT 可达 SCHEDULED→IN_PROGRESS→COMPLETED，以及 CANCELLED。
- 无不可达状态，无死锁。终态无出边。

### 6. 角色与权限

| 迁移 | 执行角色 |
|------|----------|
| 排程（DRAFT→SCHEDULED） | 维护主管 |
| 开始执行/完成 | 维护人员 |
| 取消 | 维护主管 |

### 7. 外部依赖

| 外部场景 | 内部处理 |
|----------|----------|
| 维护计划周期触发 | 定时任务（nop-job）按周期生成 DRAFT 访问 |
| 维护请求触发 | 请求受理后生成 DRAFT 访问 |
| 备件消耗出库 | 调用 `IErpInvStockMoveBiz` |
| 设备停机通知制造 | 发布事件，制造域订阅（调整排产） |

### 8. TODO / 任务策略

| 状态 | 是否产生 TODO | TODO 类型 |
|------|---------------|-----------|
| DRAFT | 是 | assigned（维护主管）—— 待排程 |
| SCHEDULED | 是 | assigned（维护人员）—— 待执行 |
| IN_PROGRESS | 是 | confirm（执行中待完成确认） |
| COMPLETED/CANCELLED | 否 | — |

避免"计划生成的访问长期无人排程"：DRAFT 产生 TODO 提醒。

### 9. 场景演练

#### 场景 A：预防性维护 happy path

1. 维护计划（每月润滑）到期 → 定时任务生成维护访问（DRAFT）。
2. 维护主管排程 → SCHEDULED。
3. 维护人员执行 → IN_PROGRESS → 消耗备件（出库）→ COMPLETED。
4. 设备恢复运行。

#### 场景 B：报修响应性维护

1. 操作员发现故障 → 提交维护请求。
2. 请求受理 → 生成维护访问（DRAFT）。
3. 排程 → 执行 → 完成，设备恢复。

#### 场景 C：维护中发现额外故障

1. 预防维护执行中，发现重大故障。
2. 本次访问记录已发现问题，先完成基础维护（COMPLETED）。
3. 大故障另开维护请求处理。

### 10. 与设计文档一致性

- 维护计划模型与 assets 分工见 `maintenance/README.md`。
- 状态码归 `model/app-erp-maintenance.orm.xml`。

---

## 适用对象二：维护请求（MaintenanceRequest）

维护请求状态机 6 态：

```
待受理 (OPEN)
  ├─ 受理 → 已受理 (ACCEPTED) → 生成维护访问
  │           ├─ 维修中（维护访问执行中）→ 已完成 (COMPLETED)
  │           └─ 拒绝（误报或无法处理）→ 已拒绝 (REJECTED)
  ├─ 拒绝 → 已拒绝 (REJECTED)
  └─ 取消 → 已取消 (CANCELLED)
```

| 状态 | 业务含义 |
|------|----------|
| 待受理（OPEN） | 报修已提交，等待维护团队受理 |
| 已受理（ACCEPTED） | 已受理，已生成维护访问待执行 |
| 已完成（COMPLETED） | 终态：维修完成 |
| 已拒绝（REJECTED） | 终态：误报或无法处理 |
| 已取消（CANCELLED） | 终态：提交者撤销 |

维护请求的其他维度（异常/角色/TODO）与维护访问类似，不重复展开；审查时同样使用提示词。

## 适用对象三：停机记录（DowntimeEntry）

停机记录无显式状态字段，以时间字段隐式表达生命周期（startTime/endTime/totalMinutes）。

```
新建（startTime 已记 = 停机开始）
  ├─ record → 设备置 DOWN
  └─ complete → endTime + totalMinutes 计算 + 设备恢复（终态）
```

| 阶段 | 触发 | 结果 |
|------|------|------|
| record | 维护人员/操作员 | 设备→DOWN（经 `EquipmentStatusLinker`，`erp-mnt.equipment-status-link-enabled` 门控） |
| complete | 维护人员 | endTime + totalMinutes（startTime→endTime 差值分钟，写入 VARCHAR/DECIMAL 列）+ 设备恢复 RUNNING |

- 终态保护：endTime 非空（已 complete）不可再 record/complete。
- `relatedJobOrderId` 仅存值（记录影响生产工单），不依赖 EQL 自引用导航（基线 refEntityName 自引用 bug，修复合规后续）。

## 实现偏离与 Non-Goal（补注）

> 以下为工作项 2.7（`docs/plans/2026-07-03-1018-3-maintenance-visit-request-sparepart-downtime.md`）落地时显式延后项，**明知偏离**目标架构，已带触发条件移交后继。

| 偏离项 | 现状 | 目标架构 | 触发条件 |
|--------|------|----------|----------|
| 停机通知制造域（排产调整） | maintenance 不反向依赖 manufacturing（避免成环） | 事件驱动：maintenance 发布停机事件，制造域订阅调整排产（§7） | APS/排产停机窗口联动需求时 |
| 维修费用过账（备件消耗/工时凭证） | ✅ 已实现（plan 2026-07-10-1100-6）：备件消耗 confirm 后经 `MaintenanceIssuePostingDispatcher` 生成 MAINTENANCE_ISSUE(492) 凭证（Dr: 维修费用 6602 / Cr: 存货 1403），config-gated `erp-mnt.spare-part-posting-enabled` 默认关。工时费用化凭证仍 Deferred。 | maintenance→finance S 写：维修领料过账 MAINTENANCE_ISSUE 凭证（`docs/architecture/data-dependency-matrix.md` 列为目标架构） | 工时费用化凭证 successor |
| 预测性维护（PREDICTIVE） | `scheduleType=PREDICTIVE` 数据来源未就绪 | IoT/传感器数据驱动 | IoT 集成落地时 |
| 校准管理全流程 | ErpMntCalibration 仅 CRUD 骨架 | 量具校准 + 下次校准日期推进（独立面） | 计量管理需求时 |
| 设备-资产价值联动 | assets 域负责资产价值，maintenance 仅实物维护 | 跨域价值联动（折旧/资本化受维护影响） | 资产维护影响价值评估时 |
| 多级审批 | 访问/请求/备件消耗单级 approve 简化 | 多级审批工作流 | 多级审批需求时 |

## 审查提示

审查本状态机时，使用 `docs/skills/state-machine-business-review-prompt.md`，重点检查：
- 计划生成的维护访问是否产生 TODO（避免无人排程滞留）。
- 设备状态联动（维护中/停机/恢复）是否与维护访问状态一致。
- 维护中发现额外故障的处理路径（本次记录 + 另开请求）。
- 设备生产中时的停机窗口协商（与制造域协作）。
- 维护请求与维护访问的状态联动（请求受理生成访问）。
