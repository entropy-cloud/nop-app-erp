# 设备维护域视图按钮需求覆盖分析

## 分析范围

| 实体 | 分类 | 说明 |
|------|------|------|
| ErpMntEquipment | CRUD | 设备（资产实物登记） |
| ErpMntSchedule | CRUD | 维护计划（周期性预防维护模板） |
| ErpMntVisit | CRUD（应含域专用状态按钮） | 维护访问/工单——有 5 态状态机 |
| ErpMntRequest | CRUD（应含域专用状态按钮） | 维护请求/报修——有 6 态状态机 |
| ErpMntCalibration | CRUD | 校准记录（全流程已显式延后） |
| ErpMntVisitTask | CRUD | 维护任务/作业模板（Visit 子表） |
| ErpMntDowntimeEntry | CRUD | 停机记录（隐式生命周期） |
| ErpMntEquipmentCategory | CRUD | 设备分类（树形配置） |
| ErpMntMaintenanceTeam | CRUD | 维护团队（配置） |
| ErpMntMaintenanceTeamMember | CRUD | 团队成员（子表） |
| ErpMntSparePartUsage | CRUD | 备件消耗（子表） |
| ErpMntSparePartUsageLine | CRUD | 备件消耗行（子表） |
| MeterReading | — | 代码库中不存在对应实体（未 codegen） |

## 期望按钮推导依据

- **CRUD 基线**（METHODOLOGY §1.1）：所有实体默认 toolbar={add-button, batch-delete-button}, row={row-view-button, row-update-button, row-delete-button}
- **state-machine.md § 维护访问（MaintenanceVisit）5 态状态机**：DRAFT→SCHEDULED（排程）、SCHEDULED→IN_PROGRESS（开始执行）、IN_PROGRESS→COMPLETED（完成）、任意→CANCELLED（取消）。原文 §2 迁移表："DRAFT→SCHEDULED：维护主管排程"；"SCHEDULED→IN_PROGRESS：维护人员开始执行"；"IN_PROGRESS→COMPLETED：维护人员完成"；"任意非终态→CANCELLED"。
- **state-machine.md § 维护请求（MaintenanceRequest）6 态状态机**：OPEN→ACCEPTED（受理）、ACCEPTED→COMPLETED（完成）、OPEN/ACCEPTED→REJECTED（拒绝）、OPEN→CANCELLED（取消）。原文 §适用对象二："待受理(OPEN) → 受理 → 已受理(ACCEPTED)"，"维修中（维护访问执行中）→ 已完成(COMPLETED)"。
- **state-machine.md § 实现偏离与 Non-Goal**：校准管理全流程显式延后，"ErpMntCalibration 仅 CRUD 骨架"。原文："校准管理全流程 — ErpMntCalibration 仅 CRUD 骨架 | 量具校准 + 下次校准日期推进（独立面）| 计量管理需求时"。
- **domain-design-guidelines.md §16.2**：maintenance 域 docStatus 取值 `DRAFT / SCHEDULED / IN_PROGRESS / COMPLETED / CANCELLED`，但 Calibration 的 workflow 已显式延后。
- **domain-design-guidelines.md §16.3**：带审批的单据期望 submit/withdraw/approve/reject/reverse-approve 标准 WF 按钮。MaintenanceVisit/Request 使用 docStatus 状态机（非 approveStatus），因此期望的不是标准 WF 按钮而是域专用状态迁移按钮。

## 逐实体分析

### ErpMntEquipment — CRUD

- **期望按钮**：CRUD 基线 {add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button}
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

> 注：ui-patterns.md § 设备列表与详情提及"卡片/列表双视图"和"顶部搜索框"，均为布局/过滤项，非 action 按钮。设备详情页的维护时间轴为聚合展示，无独立操作按钮需求。

### ErpMntSchedule — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

> 维护计划属预防性维护模板/配置类实体，无状态机。计划周期自动生成访问由定时任务触发（nop-job），非 UI 按钮触发。

### ErpMntVisit — CRUD（应为自定义状态机单据）

- **期望按钮**：CRUD 基线 + {row-schedule-button, row-start-button, row-complete-button, row-cancel-button}
  - `row-schedule-button`：DRAFT→SCHEDULED（排程），state-machine.md §2 迁移表："维护主管排程"
  - `row-start-button`：SCHEDULED→IN_PROGRESS（开始执行），state-machine.md §2："维护人员开始执行"
  - `row-complete-button`：IN_PROGRESS→COMPLETED（完成），state-machine.md §2："维护内容已记录、备件消耗已入账"
  - `row-cancel-button`：任意非终态→CANCELLED（取消），state-machine.md §2："维护主管取消"
- **实际按钮**：CRUD 基线（pure CRUD，无任何状态迁移按钮）
- **差距**：
  - `row-schedule-button`: missing (blocker) — state-machine.md §2 明确要求 DRAFT→SCHEDULED 迁移，不可在 UI 触发排程
  - `row-start-button`: missing (blocker) — state-machine.md §2 明确要求 SCHEDULED→IN_PROGRESS 迁移，不可在 UI 开始执行
  - `row-complete-button`: missing (blocker) — state-machine.md §2 明确要求 IN_PROGRESS→COMPLETED 迁移，不可在 UI 完成
  - `row-cancel-button`: missing (blocker) — state-machine.md §2 允许任意非终态取消，不可在 UI 取消
- **判定**：blocker — 4 个核心状态迁移按钮全部缺失，维护访问工单无法在 UI 完成生命周期

### ErpMntRequest — CRUD（应为自定义状态机单据）

- **期望按钮**：CRUD 基线 + {row-accept-button, row-complete-button, row-reject-button, row-cancel-button}
  - `row-accept-button`：OPEN→ACCEPTED（受理），state-machine.md §适用对象二："受理 → 已受理(ACCEPTED)"
  - `row-complete-button`：ACCEPTED→COMPLETED（完成），state-machine.md §适用对象二："维修中 → 已完成(COMPLETED)"
  - `row-reject-button`：OPEN/ACCEPTED→REJECTED（拒绝），state-machine.md §适用对象二："拒绝（误报或无法处理）→ 已拒绝(REJECTED)"
  - `row-cancel-button`：OPEN→CANCELLED（取消），state-machine.md §适用对象二："取消 → 已取消(CANCELLED)"
- **实际按钮**：CRUD 基线（pure CRUD，无任何状态迁移按钮）
- **差距**：
  - `row-accept-button`: missing (blocker) — state-machine.md §适用对象二 明确要求 OPEN→ACCEPTED 受理操作
  - `row-complete-button`: missing (blocker) — state-machine.md §适用对象二 明确要求 ACCEPTED→COMPLETED 完成操作
  - `row-reject-button`: missing (blocker) — state-machine.md §适用对象二 明确要求→REJECTED 拒绝操作
  - `row-cancel-button`: missing (blocker) — state-machine.md §适用对象二 允许 OPEN→CANCELLED
- **判定**：blocker — 4 个核心状态迁移按钮全部缺失，维护请求/报修无法在 UI 完成生命周期

### ErpMntCalibration — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

> state-machine.md § 实现偏离与 Non-Goal 显式声明校准管理全流程延后，当前仅为 CRUD 骨架。ORM 虽含 docStatus/approveStatus 字段，但 workflow 按钮不作为期望（延后项）。

### ErpMntVisitTask — CRUD

- **期望按钮**：CRUD 基线（Visit 子表实体）
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

> 维护任务为 Visit 的明细行实体（`<to-many name="tasks">`）。单独 CRUD 页面可用作模板管理，标准 CRUD 足够。

### ErpMntDowntimeEntry — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

> state-machine.md §适用对象三 定义停机记录为隐式生命周期（startTime/endTime 驱动），无显式状态字段，标准 CRUD 满足。

### ErpMntEquipmentCategory — CRUD

- **期望按钮**：CRUD 基线（配置类实体，父级引用自身形成树）
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpMntMaintenanceTeam — CRUD

- **期望按钮**：CRUD 基线（配置类实体）
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpMntMaintenanceTeamMember — CRUD

- **期望按钮**：CRUD 基线（子表实体）
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpMntSparePartUsage — CRUD

- **期望按钮**：CRUD 基线（Visit 子表实体）
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpMntSparePartUsageLine — CRUD

- **期望按钮**：CRUD 基线（SparePartUsage 子表）
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### MeterReading — （代码库中不存在）

- **期望按钮**：N/A
- **实际按钮**：N/A
- **差距**：无（未找到对应实体，既无 ORM 定义也无 view.xml 页面）
- **判定**：info — 用户列出的实体名 MeterReading 在 module-maintenance 的 ORM 模型和已 codegen 页面中均不存在。可能为计划中的未来实体或命名差异。

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| CRUD | ErpMntEquipment | 0 | clean | — |
| CRUD | ErpMntSchedule | 0 | clean | — |
| **CRUD**（应为 Custom） | **ErpMntVisit** | **4** | **blocker** | 缺少排程/开始执行/完成/取消 4 个状态迁移按钮 |
| **CRUD**（应为 Custom） | **ErpMntRequest** | **4** | **blocker** | 缺少受理/完成/拒绝/取消 4 个状态迁移按钮 |
| CRUD | ErpMntCalibration | 0 | clean | WF 延后（设计已知） |
| CRUD | ErpMntVisitTask | 0 | clean | — |
| CRUD | ErpMntDowntimeEntry | 0 | clean | — |
| CRUD | ErpMntEquipmentCategory | 0 | clean | — |
| CRUD | ErpMntMaintenanceTeam | 0 | clean | — |
| CRUD | ErpMntMaintenanceTeamMember | 0 | clean | — |
| CRUD | ErpMntSparePartUsage | 0 | clean | — |
| CRUD | ErpMntSparePartUsageLine | 0 | clean | — |
| — | MeterReading | N/A | info | 代码库中不存在 |

### 总评

- 总实体数：13（12 标准已 codegen 实体 + 1 未找到实体）
- 无差距实体：10（76.9%）
- Blocker 差距：2（ErpMntVisit、ErpMntRequest）
- Major 差距：0
- Minor 差距：0
- Info 差距：1（MeterReading 未找到）

### 核心发现

**两个具有完整状态机的业务实体——ErpMntVisit（维护访问/工单）和 ErpMntRequest（维护请求/报修）——的 view.xml 仅有纯 CRUD 按钮，缺少全部状态迁移按钮。** state-machine.md 详细定义了 Visit 的 5 态（DRAFT→SCHEDULED→IN_PROGRESS→COMPLETED/CANCELLED）和 Request 的 6 态（OPEN→ACCEPTED→COMPLETED/REJECTED/CANCELLED），但 view.xml 中无任何对应操作，用户无法在 UI 完成这些实体的生命周期流转。

其余 10 个实体（Equipment、Schedule、Calibration、VisitTask、DowntimeEntry、EquipmentCategory、MaintenanceTeam、MaintenanceTeamMember、SparePartUsage、SparePartUsageLine）均为标准 CRUD，无差距。

MeterReading 在代码库中不存在对应的 ORM 实体或页面，可能为计划中未落地的实体或术语差异。

### 建议修复方案

1. **ErpMntVisit**：在 `main` CRUD rowActions 中添加：
   - `row-schedule-button`（DRAFT→SCHEDULED，维护主管角色可见）
   - `row-start-button`（SCHEDULED→IN_PROGRESS，维护人员角色可见）
   - `row-complete-button`（IN_PROGRESS→COMPLETED，可见条件：备件消耗已确认）
   - `row-cancel-button`（非终态→CANCELLED，维护主管角色可见）
   - 各按钮的可见性控制 `${status == 'DRAFT'}`、`${status == 'SCHEDULED'}` 等

2. **ErpMntRequest**：在 `main` CRUD rowActions 中添加：
   - `row-accept-button`（OPEN→ACCEPTED，生成 DRAFT 维护访问）
   - `row-complete-button`（ACCEPTED→COMPLETED）
   - `row-reject-button`（OPEN→REJECTED）
   - `row-cancel-button`（OPEN→CANCELLED）
   - 可见性按 status 条件控制
