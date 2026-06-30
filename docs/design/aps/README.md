# APS（高级排产）域 — 有限产能工序级排产

## 目的

设计 APS（Advanced Planning & Scheduling）模块的**工序级有限产能排产**。通过 `OperationOrder`（工序工单）实现工序级排程，弥补 MRP 只解物料需求的空白以及与 CRP 负荷报表的区别。

## 边界

- 本模块负责：工序级排产（OperationOrder）、工作中心产能约束、前向/后向排产算法、排产甘特图可视化、排产方案版本管理。
- **与 manufacturing 的边界**：APS 使用 manufacturing 域的 WorkOrder（主工单）和 WorkCenter（工作中心）。APS 生成 OperationOrder（工序工单）作为排产层，manufacturing 域的 JobCard（作业卡）作为执行层。APS 不涉及 BOM/工艺路线定义。
- **与 CRP 的边界**：CRP（`crp.md`）是负荷报表（只读分析），APS 是排产计算（写入 OperationOrder 的排程时间）。CRP 告诉计划员"哪里有负荷"，APS 告诉计划员"每个工序何时在哪里执行"。
- 本模块不负责：MRP 物料需求计算（`manufacturing/mrp.md`）；实际生产执行报工（`manufacturing/JobCard`）。

## 设计依据

> 参考 **Axelor production**（344 Java 文件）：`ManufOrder`（主工单）+ `OperationOrder`（工序工单）双层结构。OperationOrder 是工序级排产的核心数据结构，每个 OperationOrder 绑定工作中心/设备/排程时间/优先级。
>
> 参考 Odoo `mrp_workcenter` 产能四要素分离。
>
> 来源 `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §APS。

## 实体清单

> 表前缀 `erp_aps_`、类名 `ErpAps*`、字典 `erp-aps/*`。仅在需要独立持久化时使用；核心实体与 manufacturing 域共享（WorkOrder、WorkCenter）。

### ErpApsOperationOrder（工序工单 — APS 排产核心）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/orgId | 标准 | 🟢 Axelor OperationOrder |
| workOrderId | 主工单（→ErpMfgWorkOrder） | 🟢 Axelor OperationOrder.manufOrder |
| operationName | 工序名称 | 🟢 Axelor OperationOrder.name |
| sequence | 工序顺序 | 🟢 Axelor ProdProcess.operationContinuity |
| machineId | 工作中心/设备（→ErpMfgWorkcenter） | 🟢 Axelor OperationOrder.machine |
| priority | 优先级（数字越小优先级越高） | 🟢 Axelor ManufOrder.operationOrderMaxPriority |
| plannedStartDateT/plannedEndDateT | 计划开工/完工时间（APS 排产输出） | 🟢 Axelor OperationOrder |
| realStartDateT/realEndDateT | 实际开工/完工时间 | 🟢 Axelor OperationOrder |
| setupTime | 换模/准备时间（分钟） | 🟢 Axelor ProdProcess |
| runtimePerUnit | 每件加工时间（分钟） | — |
| qty | 加工数量 | — |
| totalDuration | 总耗时（= setupTime + runtimePerUnit × qty，派生） | 🟢 Axelor OperationOrder |
| assignedToId | 操作工（→ErpHrEmployee） | — |
| isOutsourced | 是否外协工序 | 🟢 Axelor OperationOrder |
| status | dict `erp-aps/operation-order-status`：DRAFT/PLANNED/IN_PROGRESS/FINISHED/CANCELLED | 🟢 Axelor OperationOrder |
| 标准审计字段 | | |

**状态机**：`DRAFT → PLANNED（APS 排产完成） → IN_PROGRESS（开始执行） → FINISHED（完成）`；`DRAFT/PLANNED → CANCELLED`。

### ErpApsSchedule（排产方案）

| 字段 | 含义 |
|------|------|
| id/code/name/orgId | 标准 |
| scheduleDate | 排产日期 |
| schedulingMode | dict：FORWARD（前向排产）/ BACKWARD（后向排产） |
| horizonStart/horizonEnd | 排产展望期 |
| status | dict：DRAFT/PUBLISHED/ARCHIVED |
| 标准审计字段 | |

### ErpApsConstraint（排产约束，可选深化）

| 字段 | 含义 |
|------|------|
| id/machineId/orgId | 标准 |
| constraintType | dict：MAINTENANCE（维护停机）/ TOOL（刀具寿命）/ PERSONNEL（人员约束） |
| startTime/endTime | 约束时间段 |
| description | 约束描述 |

## 业务规则

1. **OperationOrder 是排产基本单元**：不是工单级排产，是工序级排产。同一工单的不同工序可能在不同工作中心、不同时间执行。
2. **有限产能约束**：同一工作中心同一时间只安排一个 OperationOrder 执行（考虑 capacity 并联生产除外）。相邻 OperationOrder 之间的空隙（换模/清理时间）自动插入。
3. **排产模式**：前向排产（从工单 plannedStartDate 开始正向填充）和后向排产（从客户交期倒推）。
4. **交期承诺（ATP/CTP）**：销售订单审核时，通过 APS 模拟排产获得可承诺交期。
5. **重排触发**：工单变更/插单/急单时触发区间重排（不全局重排，避免牛顿效应）。

## 业财过账

APS 不产生会计凭证。APS 排产结果驱动的生产执行（WorkOrder/JobCard）走 manufacturing 域标准过账。

## 跨域协作

| 对端 | 协作方式 |
|------|---------|
| manufacturing/WorkOrder | OperationOrder 从属 WorkOrder |
| manufacturing/WorkCenter | 工作中心产能约束（日历/班次/产能参数） |
| manufacturing/JobCard | JobCard 按 OperationOrder 排程创建（执行层） |
| manufacturing/MRP | MRP 产出工单，APS 消费工单工序排产 |
| maintenance/downtime | 设备停机扣减工作中心可用时段 |
| sales | 销售订单 ATP/CTP 承诺 |
| CRP | CRP 读取 APS 排产结果做负荷报表 |

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-aps.scheduling-mode` | FORWARD | 排产模式：FORWARD / BACKWARD |
| `erp-aps.auto-reschedule-on-insert` | true | 插单时是否自动触发区间重排 |
| `erp-aps.time-bucket-minutes` | 15 | 排产时间槽粒度（分钟） |

## 菜单归属

新增 aps 域 TOPM「高级排产」，分组：工序工单（排产甘特图）、排产方案、排产约束。

## 反模式警示

- ⛔ **排产与执行混在同一实体**——OperationOrder（排产层）与 JobCard（执行层）分离，排产调整不影响已报工数据。
- ⛔ **全局重排**——插单只触发区间重排，避免全工单链排产波动。
- ⛔ **把 CRP 当 APS**——CRP 是只读负荷报表，APS 写入排程时间。

## 证据强度标注

| 证据 | 强度 | 说明 |
|------|------|------|
| ManufOrder + OperationOrder 双层结构 | 🟢 | Axelor `ManufOrder.xml` + `OperationOrder.xml` 源码实测 |
| 工序级排产：plannedStartDateT/EndDateT/priority/duration | 🟢 | Axelor `OperationOrder.xml` 字段实测 |
| 工作中心/设备绑定与排程 | 🟢 | Axelor `Machine.xml` + `OperationOrder.machine` |
| 排产甘特图视图 | 🟢 | Axelor production 视图含甘特图 |
| 产能四要素分离 | 🟢 | Odoo `mrp_workcenter.py:30,78,81` |

## 参考

- `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §APS
- `docs/design/manufacturing/crp.md`（CRP 与 APS 边界）
- `docs/design/manufacturing/mrp.md`
- `docs/design/manufacturing/bom-and-routing.md`
- `docs/design/manufacturing/state-machine.md`
