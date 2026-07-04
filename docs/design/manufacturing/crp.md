# CRP（产能需求计划）

## 目的

设计 CRP（Capacity Requirements Planning）的产能建模、负荷计算与可视化。CRP 是**只读负荷报表**（分析工具），不写入排程方案。APS 排产见独立 `docs/design/aps/README.md`。

## 边界

- 本模块负责：工作中心产能建模（日历/班次/按产品产能/换模时间）、CRP 负荷计算与负荷报表（已占用 vs 可用时段、超负荷告警）。
- **与 APS 的边界**：CRP 只读不写（负荷率报告 + 告警），APS（`aps/README.md`）写入 OperationOrder 的排程时间。CRP 回答"哪里有负荷"，APS 回答"每个工序何时在哪里执行"。
- 本模块不负责：MRP 物料需求（`mrp.md`）；APS 工序级排产（`aps/README.md`）；生产执行（WorkOrder/JobCard）。

## 设计依据

> 参考 Odoo `mrp_workcenter` 产能四要素分离。
>
> 来源 `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §2.1。

### 核心设计点（产能四要素分离）

1. **工作中心日历（时钟）**——出勤时段决定负荷上限（🟢 `mrp_workcenter.py:30,78,81` + `:157` `load_limit`）。
2. **按产品并行产能**——同工作中心不同产品产能/换模不同（🟢 `_get_capacity` `:427-437`）。
3. **换模/清理时间**——工序切换准备耗时（🟢 `mrp_workcenter.py:81`）。
4. **效率系数**——效率折算（🟢 `mrp_workcenter.py:78`）。

## 实体清单

> 表前缀 `erp_mfg_`、类名 `ErpMfg*`。与 manufacturing 域共享。

### ErpMfgWorkcenterCalendar（工作中心日历/班次）

| 字段 | 含义 |
|------|------|
| id/workcenterId/orgId | 标准 |
| calendarName | 日历名称 |
| shiftType | dict `erp-mfg/shift-type`：ONE_SHIFT/MORNING/AFTERNOMENT/NIGHT |
| workDatePattern | 工作日模式 |
| startTime/endTime | 班次起止时间 |
| effectiveFrom/effectiveTo | 生效区间 |

### ErpMfgWorkcenterCapacity（按产品产能）

| 字段 | 含义 |
|------|------|
| id/workcenterId/orgId | 标准 |
| materialId | 关联物料 |
| capacityPerHour | 每小时产能 |
| setupTime | 换模时间 |
| cleanupTime | 清理时间 |
| efficiencyFactor | 效率系数（默认 1.0） |

### ErpMfgCrpLoad（CRP 负荷快照行）

| 字段 | 含义 |
|------|------|
| id/orgId/workcenterId | 标准 |
| workOrderId | 关联工单（弱指针） |
| loadDate | 负荷日期 |
| loadHours | 占用工时 |
| setupHours | 换模工时 |

**负载报表**：按 workcenterId×日期 聚合 loadHours，对比日历出勤时长得负荷率。

## 业务规则

1. **CRP 不产生凭证**：属计划层。
2. **CRP 不写入排程**：只读分析，超负荷提示人工干预或调用 APS 重排。
3. **CRP 读取 APS 排产结果**：若有 APS 模块，负荷数据从 OperationOrder 的排程时间派生。

## 跨域协作

| 对端 | 协作内容 |
|------|---------|
| manufacturing/WorkOrder | 读取 plannedStartDate/EndDate |
| **APS** | CRP 可消费 APS OperationOrder 的排程时间作为负荷来源 |
| maintenance/downtime | 设备停机扣减工作中心可用时段 |

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-mfg.crp-run-schedule` | — | CRP 定时运行 cron（旧语义键，已由下方 `erp-mfg.crp-run-cron` 接线取代） |
| `erp-mfg.crp-run-cron` | —（默认不执行，运维启用配置键生效） | CRP 定时运行 cron 门控。**SCHEDULED**（plan 2026-07-05-0306-1）：`ErpMfgCrpRunJob` + `scheduler.yaml` 已接线，空值=跳过；非空时按窗口调 `IErpMfgCrpBiz.calculateLoad()`（全工作中心） |
| `erp-mfg.crp-run-default-window-months` | 0（当月） | CRP 定时计算默认向前窗口（月），0=当前自然月 |
| `erp-mfg.crp-overload-threshold` | 1.0 | 超负荷阈值 |

## 反模式警示

- ⛔ **产能硬编码为单一标量**——按产品产能用子实体。
- ⛔ **把 CRP 当 APS**——CRP 是只读负荷报表，APS 写入 OperationOrder 排程。

## 证据强度标注

| 证据 | 强度 | 说明 |
|------|------|------|
| 产能四要素分离 | 🟢 | Odoo `mrp_workcenter.py:30,78,81` |
| 按产品产能子实体 | 🟢 | Odoo `mrp.workcenter.capacity` `:613-636` |
| 负荷上限取自日历 | 🟢 | Odoo `mrp_workcenter.py:157` |

## 参考

- `docs/design/aps/README.md`（APS 工序级排产）
- `docs/design/manufacturing/mrp.md`
- `docs/design/manufacturing/state-machine.md`

## 实现偏离补注（2026-07-03，plan 2026-07-03-1707-1 落地）

> 以下为本期实现相对上方设计的明知偏离，均为计划内 Non-Goal，已记入 plan Deferred But Adjudicated。

- **负荷来源 fallback（无 APS）**：设计 §业务规则 3「CRP 读取 APS 排产结果」依赖 APS OperationOrder 排程时间。APS 模块（M3 工作项 3.10/3.11）尚未落地，本期负荷来源取 **WorkOrder（plannedStartDate~plannedEndDate）+ RoutingOperation（workcenterId + standardTime）**，按区间内日**均匀分派**。APS 落地后负荷来源可切换为精确排程时间（successor）。
- **maintenance 停机扣减可用时段为 Non-Goal**：设计 §跨域协作「maintenance/downtime 设备停机扣减工作中心可用时段」为事件驱动，需 maintenance 停机事件机制（当前 maintenance 停机通知制造为本期 Non-Goal）。本期 WorkcenterCalendar 可用工时不扣减停机（successor：maintenance 停机事件 + 排产停机窗口联动）。
- **标量 `Workcenter.capacity` 保留不删不依赖**：既有 `ErpMfgWorkcenter.capacity`/`capacityUnit` 为反模式（单一标量产能，见上方反模式警示）。本期 CRP 一律用新增 `ErpMfgWorkcenterCapacity` 子实体（按产品产能 + 换模/清理/效率），既有标量保留为旧显示字段不删（out-of-scope：存量数据迁移）。
- **负荷桶粒度为日级（非班次级）**：`ErpMfgCrpLoad.loadDate` + loadHours/setupHours 按 workcenter×date 聚合。班次建模已就绪（WorkcenterCalendar.shiftType）但负荷按日聚合已足；日内多班次超负荷不可见（班次级为 APS 范畴）。
- **CRP 可视化页面（AMIS 甘特/热力图）为 Non-Goal**：本期交付 GraphQL 负荷报表查询（`ErpMfgCrpLoad__getLoadReport`）；AMIS 可视化为独立前端面（successor）。
- **CRP 定时运行 cron 已接线**：`erp-mfg.crp-run-cron`（取代旧 `erp-mfg.crp-run-schedule`）经 `ErpMfgCrpRunJob` 三件套接线（plan 2026-07-05-0306-1，SCHEDULED）。负荷计算业务语义不变（仍按需 `@BizMutation calculateLoad`，job 仅做入参派生=当月窗口 + 全工作中心 + 委托）。APS OperationOrder 接线为负荷来源由计划 `2026-07-05-0306-2` 承接，不影响本作业注册。
