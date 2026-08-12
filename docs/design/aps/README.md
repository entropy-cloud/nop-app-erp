# APS（高级排产）域（aps）

## 目的

设计 APS（Advanced Planning & Scheduling）模块的**工序级有限产能排产**。通过 `OperationOrder`（工序工单）实现工序级排程，弥补 MRP 只解物料需求的空白，并区别于 CRP 负荷报表。

## 边界

- 本模块负责：工序级排产（OperationOrder）、工作中心产能约束、前向/后向排产算法、排产甘特图可视化、排产方案版本管理。
- **与 manufacturing 的边界**：APS 使用 manufacturing 域的 WorkOrder（主工单）和 WorkCenter（工作中心）。APS 生成 OperationOrder（工序工单）作为排产层，manufacturing 域的 JobCard（作业卡）作为执行层。APS 不涉及 BOM/工艺路线定义。
- **与 CRP 的边界**：CRP（`manufacturing/crp.md`）是负荷报表（只读分析），APS 是排产计算（写入 OperationOrder 的排程时间）。CRP 告诉计划员"哪里有负荷"，APS 告诉计划员"每个工序何时在哪里执行"。
- 本模块不负责：MRP 物料需求计算（`manufacturing/mrp.md`）；实际生产执行报工（manufacturing 的 JobCard）。
- 持久化字段、字典、状态码以 `module-aps/model/app-erp-aps.orm.xml` 为准。
- 跨域协作规则见 `../domain-design-guidelines.md`，全局流程见 `../flow-overview.md`。

## 工程与模型

| 项 | 值 |
|----|----|
| Maven 工程 | `app-erp-aps` |
| appName | `erp-aps`（两级） |
| 权威模型 | `module-aps/model/app-erp-aps.orm.xml` |
| 实体包 | `app.erp.aps.dao.entity` |
| 表前缀 | `erp_aps_` |
| 类名前缀 | `ErpAps*` |
| 字典命名空间 | `erp-aps/*` |

## 核心业务对象

| 对象 | 业务含义 |
|------|----------|
| 工序工单（OperationOrder） | APS 排产的核心数据结构：从属于主工单（WorkOrder）的一道工序，绑定工作中心/设备、计划开工与完工时间、优先级、换模时间与单件工时、加工数量、外协标记。同一工单的不同工序可排在不同的工作中心与时段 |
| 排产方案（Schedule） | 一次排产计算的方案版本：排产日期、前向/后向排产模式、展望期区间、方案状态（草稿/发布/归档） |
| 排产约束（Constraint） | 工作中心层面的排产约束：维护停机、刀具寿命、人员约束等不可用时段 |

## 状态机

工序工单状态流转：`DRAFT → PLANNED（APS 排产完成）→ IN_PROGRESS（开始执行）→ FINISHED（完成）`；`DRAFT/PLANNED → CANCELLED`。详细规则见 [`state-machine.md`](state-machine.md)。

## 跨域协作

| 协作场景 | 对端域 | 协作方式 |
|----------|--------|----------|
| 工序从属主工单 | manufacturing | OperationOrder 从属 WorkOrder |
| 产能约束 | manufacturing | 引用 WorkCenter 的日历/班次/产能参数 |
| 执行层衔接 | manufacturing | JobCard 按 OperationOrder 排程创建（执行层） |
| 物料需求衔接 | manufacturing | MRP 产出工单，APS 消费工单工序排产 |
| 设备停机扣减 | maintenance | 设备停机扣减工作中心可用时段 |
| 交期承诺 | sales | 销售订单 ATP/CTP 承诺经 APS 模拟排产获得可承诺交期 |
| 负荷报表 | manufacturing/CRP | CRP 读取 APS 排产结果做负荷报表 |

跨域调用走 `I*Biz` 接口，不做 ORM 层跨工程 `refEntityName`。

## 关键业务规则

1. **OperationOrder 是排产基本单元**：不是工单级排产，是工序级排产。同一工单的不同工序可能在不同工作中心、不同时间执行。
2. **有限产能约束**：同一工作中心同一时间只安排一个 OperationOrder 执行（考虑 capacity 并联生产除外）。相邻 OperationOrder 之间的空隙（换模/清理时间）自动插入。
3. **排产模式**：前向排产（从工单计划开工时间正向填充）和后向排产（从客户交期倒推）。
4. **交期承诺（ATP/CTP）**：销售订单审核时，通过 APS 模拟排产获得可承诺交期。
5. **重排触发**：工单变更/插单/急单时触发区间重排（不全局重排，避免排产波动扩散）。

## 业财过账

APS 不产生会计凭证。APS 排产结果驱动的生产执行（WorkOrder/JobCard）走 manufacturing 域标准过账。

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-aps.scheduling-mode` | FORWARD | 排产模式：FORWARD / BACKWARD |
| `erp-aps.auto-reschedule-on-insert` | true | 插单时是否自动触发区间重排 |
| `erp-aps.time-bucket-minutes` | 15 | 排产时间槽粒度（分钟） |

## 菜单归属

aps 域 TOPM「高级排产」，分组：工序工单（排产甘特图）、排产方案、排产约束。

## 反模式警示

- ⛔ **排产与执行混在同一实体**——OperationOrder（排产层）与 JobCard（执行层）分离，排产调整不影响已报工数据。
- ⛔ **全局重排**——插单只触发区间重排，避免全工单链排产波动。
- ⛔ **把 CRP 当 APS**——CRP 是只读负荷报表，APS 写入排程时间。

## 本域文档

| 文档 | 职责 |
|------|------|
| `README.md`（本文件） | 域概览、工序排产模型、跨域协作 |
| `state-machine.md` | 工序工单状态机 |
| `scheduling.md` | 排产算法（前向/后向/有限产能） |
| `constraint-based-planning.md` | 约束排产深化设计（E1.3，2026-08-12 erp-survey 批次，暂不编码） |
| `auto-dispatch.md` | 自动分派规则 |
| `alternative-routing.md` | 替代工艺路线 |
| `ui-patterns.md` | 前端模式（排产甘特图） |
| `use-cases.md` | 用例 |

## 参考

- `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §APS（源码分析见 erp-survey）
- `docs/design/manufacturing/crp.md`（CRP 与 APS 边界）
- `docs/design/manufacturing/mrp.md`
- `docs/design/manufacturing/bom-and-routing.md`
- `docs/design/manufacturing/state-machine.md`
