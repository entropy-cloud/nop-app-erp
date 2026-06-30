# CRP（产能需求计划）

## 目的

设计 CRP（Capacity Requirements Planning）的产能建模、负荷计算与可视化。补齐 `manufacturing/mrp.md:10` 自承认延迟的"产能计划"缺口。

## 边界

- 本模块负责：工作中心产能建模（日历/班次/按产品产能/换模时间）、CRP 负荷计算与负荷报表（已占用 vs 可用时段）。
- 本模块不负责：MRP 物料需求计算（`mrp.md`，CRP 与 MRP 解耦，`mrp.md:10` 已声明边界）；APS 优化排产求解（属 follow-up）；实际生产执行（工单/作业卡，已有）。
- 实体为**建议命名，待 ORM 计划落地**（`model/app-erp-manufacturing.orm.xml` 是 ask-first 保护区域，本文件不复述 schema）。

## 设计依据

> 来源 `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §2.1。

### 核心设计点（产能四要素分离）

🟢 Odoo `mrp_workcenter` 源码将产能分解为四个正交要素，避免单一标量：

1. **工作中心日历（时钟）**——出勤时段决定负荷上限（🟢 `mrp_workcenter.py:30,78,81` + `mrp_workcenter.py:157` `load_limit`，负荷上限取自日历出勤时长，非硬编码 capacity）。
2. **按产品并行产能**——同工作中心不同产品产能/换模不同，支持混线（🟢 `_get_capacity` `mrp_workcenter.py:427-437` + `mrp.workcenter.capacity` `:613-636`）。
3. **换模/清理时间**——工序切换的准备耗时（🟢 `mrp_workcenter.py:81`）。
4. **效率系数**——工作中心效率折算（🟢 `mrp_workcenter.py:78`）。

### 与现有字段的语义澄清

现有 `ErpMfgWorkcenter.capacity`（orm.xml:328）/`workHoursPerDay`（orm.xml:331）是**默认标量产能**（粗粒度，整工作中心一个值）。按产品产能需新建 `ErpMfgWorkcenterCapacity` 子实体承载细粒度。**裁决 D1：CRP 不引入排产方案单据**——负荷计算结果用报表呈现 + 已有 `ErpMfgWorkOrder.plannedStartDate`（orm.xml:362）/`plannedEndDate`（orm.xml:363）承接排产日期，避免新增状态机。

## 实体清单

> 表前缀 `erp_mfg_`、类名 `ErpMfg*`、字典 `erp-mfg/*`。以下为建议命名，待 ORM 计划落地。

### ErpMfgWorkcenterCalendar（工作中心日历/班次，表 `erp_mfg_workcenter_calendar`）

| 字段 | 含义 |
|---|---|
| id/workcenterId/orgId | 标准 |
| calendarName | 日历名称（如"两班倒"/"三班倒"） |
| shiftType | dict `erp-mfg/shift-type`：ONE_SHIFT/MORNING/AFTERNOMENT/NIGHT |
| workDatePattern | 工作日模式（周一至周五/周一至周六等） |
| startTime/endTime | 班次起止时间 |
| effectiveFrom/effectiveTo | 生效区间 |
| 标准审计字段 | |

### ErpMfgWorkcenterCapacity（按产品产能，表 `erp_mfg_workcenter_capacity`）

| 字段 | 含义 |
|---|---|
| id/workcenterId/orgId | 标准 |
| materialId | 关联物料（→ErpMdMaterial，按产品产能） |
| capacityPerHour | 每小时产能（每小时产出数量） |
| capacityUnit | 产能单位（件/千克等） |
| setupTime | 换模时间（切换到本产品的准备耗时） |
| cleanupTime | 清理时间 |
| efficiencyFactor | 效率系数（默认 1.0） |
| 标准审计字段 | |

### ErpMfgCrpLoad（CRP 负荷快照行，表 `erp_mfg_crp_load`）

| 字段 | 含义 |
|---|---|
| id/orgId/workcenterId | 标准 |
| workOrderId | 关联工单（→ErpMfgWorkOrder，弱指针） |
| jobCardId | 关联作业卡（→ErpMfgJobCard，可空） |
| materialId | 物料 |
| loadDate | 负荷日期 |
| loadHours | 占用工时（负荷） |
| setupHours | 换模工时（首序） |
| sourceBillType/sourceBillCode | 来源（WORK_ORDER/JOB_CARD） |
| 标准审计字段 | |

**负载报表（派生视图，非独立表）**：按 `workcenterId × 日期` 聚合 `ErpMfgCrpLoad.loadHours` 得已占用，对比日历出勤时长得负荷率，超负荷高亮。

## 业务规则

1. **CRP 不产生凭证**：CRP 属计划层，不触发业财过账。超负荷触发的加班/外协才过账（复用 WorkOrder laborCost / SubcontractOrder，见 `manufacturing/state-machine.md`）。
2. **负荷上限取自日历出勤时长**：`ErpMfgWorkcenter.workHoursPerDay` 仅作默认值（无日历时兜底）；配置日历后以日历出勤时段为准（🟢 `mrp_workcenter.py:157`）。
3. **CRP 运行 = 聚合负荷快照**：CRP 运行时扫描已审核工单的工序（JobCard）+ 工时，按 `plannedStartDate/EndDate` 分配到工作中心日期，写入 `ErpMfgCrpLoad` 快照。
4. **超负荷只告警不自动改单**：CRP 是只读分析工具，超负荷提示计划员人工干预（调整工单日期/加班/外协），不自动重排（反 Odoo 前向排产的 Gantt 填充，那是有限产能排产，超出 CRP 范围）。
5. **与 MRP 解耦**：MRP 产出计划生产单（数量/物料），CRP 消费计划单 + BOM 工序计算产能需求，两者独立运行（`mrp.md:10`）。

## 跨域协作

| 对端 | 协作内容 |
|---|---|
| manufacturing/WorkOrder | 读取 `plannedStartDate/plannedEndDate` + 工序 JobCard 工时作为负荷来源 |
| maintenance | 设备停机（`ErpMntDowntime`）扣减工作中心可用时段 |

## 配置点

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `erp-mfg.crp-run-schedule` | — | CRP 定时运行 cron（如每日凌晨） |
| `erp-mfg.crp-overload-threshold` | 1.0 | 超负荷阈值（负荷率 > 此值告警） |

## 反模式警示

- ⛔ **产能硬编码为单一标量字段**（`capacity=100` 一刀切）——🟢 Odoo 反例特意按 product 查 `capacity_ids`（`mrp_workcenter.py:427-437`）；按产品混线产能必须用子实体。
- ⛔ **把 CRP 当 APS 承诺**——🟢 源码确认开源无真 APS（Odoo 仅前向排产 Gantt 填充 `mrp_workcenter.py:360-364`、ERPNext 仅日期计划 `production_plan.json` 无 capacity/finite 字段）。CRP 只做负荷可视化与告警，不做优化求解。
- ⛔ **引入排产方案单据**（裁决 D1 反面）——排产日期已有 WorkOrder 的 plannedStartDate/EndDate 承接，新增单据会引入不必要的状态机。

## 菜单归属

manufacturing 域「产能计划」分组：工作中心日历、工作中心产能（按产品）、CRP 负荷报表。

## 证据强度标注

| 证据 | 强度 | 说明 |
|---|---|---|
| 产能四要素分离 | 🟢 | Odoo `mrp_workcenter.py:30,78,81` 源码实测 |
| 按产品产能子实体 | 🟢 | Odoo `mrp.workcenter.capacity` `:613-636`、`_get_capacity` `:427-437` 源码实测 |
| 负荷上限取自日历 | 🟢 | Odoo `mrp_workcenter.py:157` `load_limit` 源码实测 |
| 开源无真 APS | 🟢 | Odoo 仅前向排产 `:360-364`、ERPNext 仅日期计划，源码实测 |
| 本项目 ErpMfgWorkOrder.plannedStartDate/EndDate | 🟢 | `module-manufacturing/model/app-erp-manufacturing.orm.xml:362-363` 实测 |
| 本项目 ErpMfgWorkcenter.capacity/workHoursPerDay | 🟢 | 同上 orm.xml:328,331 实测 |
| APS 优化求解（follow-up） | ⚪ | 领域常识，开源零覆盖 |

## 参考

- `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §2.1（设计依据）
- `docs/design/manufacturing/mrp.md:10`（CRP 与 MRP 边界）
- `docs/design/manufacturing/state-machine.md`（WorkOrder/JobCard 状态、外协过账）
- `docs/design/manufacturing/bom-and-routing.md`（工序/工作中心）
