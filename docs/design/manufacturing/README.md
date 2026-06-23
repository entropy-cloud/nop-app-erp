# 制造域（manufacturing）

## 目的

说明制造域的业务语义、BOM/工艺/工单模型、状态机与跨域协作。制造域管理从 BOM 定义到工单执行、领料、完工、成本核算的完整生产流程。

## 边界

- 本域负责：BOM（头/行/工艺/联副产品）、工单（WorkOrder）、工序作业卡（JobCard）、工艺路线（Routing）、工作中心（Workcenter）、生产计划、停机记录、外协。
- 本域不负责：物料/SKU/仓库主数据（master-data）；库存写入（inventory 域，本域调用）；生产成本凭证生成（finance 域，本域触发）；设备实物维护（maintenance 域）；完工质检（quality 域）。
- 持久化字段、字典、状态码以 `model/app-erp-manufacturing.orm.xml` 为准。

## 工程与模型

| 项 | 值 |
|----|----|
| Maven 工程 | `app-erp-manufacturing` |
| appName | `app-erp-mfg` |
| 权威模型 | `model/app-erp-manufacturing.orm.xml` |
| 实体包 | `app.erp.mfg.dao.entity` |
| 表前缀 | `erp_mfg_` |
| 类名前缀 | `ErpMfg*` |
| 字典命名空间 | `erp-mfg/*` |

## 核心业务对象

| 对象 | 业务含义 |
|------|----------|
| BOM（头/行） | 物料清单：产出物料、产出量、子件清单（含数量/单位/工序绑定） |
| BOM 工艺（BomOperation） | BOM 绑定的工艺路线：工序、工作中心、标准工时、费率 |
| BOM 联副产品（BomByproduct） | 联产品/副产品定义 |
| 工单（WorkOrder） | 生产订单：产出物料、数量、BOM、计划/实际起止、状态、关联领料/完工移动单 |
| 工序作业卡（JobCard） | 工单下的工序执行卡：工序、工作中心、计划/完成数量、工时记录 |
| 工艺路线（Routing） | 工序序列定义（可被多个 BOM 引用） |
| 工作中心（Workcenter） | 生产单元：产能、费率、工作时间 |
| 生产计划（ProductionPlan） | 基于 MRP 的生产计划建议 |
| 停机记录（DowntimeEntry） | 工作中心停机记录（影响排产） |

### 工单与库存的关系

工单通过两类库存移动单影响库存：
- **领料消耗**（move_raw）：从仓库领取原材料，生成出库移动单（outgoing）。
- **完工入库**（move_finished）：产成品入库，生成入库移动单（incoming）。

工单审核/执行时调用 `IErpInvStockMoveBiz` 生成移动单。

### 工单与 BOM 的关系

工单引用一个 BOM。工单审核时：
- 按 BOM 子件清单生成领料需求（领料移动单的计划数量 = BOM 子件数量 × 工单产出量）。
- 按 BOM 工艺生成工序列表（JobCard）。
- 多级 BOM（`use_multi_level_bom`）展开子件递归。

## 状态机

工单有复杂状态机（草稿→已提交→未开始→生产中→齐套/部分齐套→已完工/已停工/已关闭/已取消），作业卡有独立状态机。详细规则见 [`state-machine.md`](state-machine.md)。

## 跨域协作

| 协作场景 | 对端域 | 协作方式 |
|----------|--------|----------|
| 领料消耗写库存 | inventory | 工单领料 → `IErpInvStockMoveBiz` 生成出库移动单 |
| 完工入库写库存 | inventory | 工单完工 → 生成入库移动单 |
| 存货估值与成本结转 | finance | 移动单完成触发存货估值凭证；工单完工触发成本结转凭证 |
| 完工质检 | quality | 工单完工触发完工检验（若 BOM 配置 `inspection_required`） |
| 工作中心停机 | maintenance | 工作中心停机记录关联设备维护状态 |
| 物料/仓库引用 | master-data | BOM/工单引用物料/SKU/仓库主数据 |

## 关键业务规则

1. **BOM 多版本**：同一产出物料可有多个 BOM，`is_active` + `is_default` 区分激活与默认。
2. **BOM 类型**：normal（制造）与 phantom（虚拟件/Kit，展开不生产）。
3. **消耗控制**：`consumption`（flexible 允许超耗 / warning 超耗警告 / strict 严格按 BOM）。
4. **齐套校验**：工单进入生产前校验所有子件库存可用量是否满足（齐套 / 部分齐套）。
5. **领料方式**：`transfer_material_against`（按工单领料 / 按作业卡领料）。
6. **多级 BOM 展开**：`use_multi_level_bom` 控制是否递归展开子件。
7. **联副产品**：BOM 可定义联副产品，完工时一并入库。
8. **外协工序**：部分工序外协，外协费用计入工单成本。

## 本域文档

| 文档 | 职责 |
|------|------|
| `README.md`（本文件） | 域概览、BOM/工单模型、跨域协作 |
| `state-machine.md` | 工单与作业卡状态机 |
| `bom-and-routing.md` | BOM 结构、工艺路线、展开规则、成本计算 |
| `material-reservation.md` | 工单物料预留设计（预留量计算、齐套校验、预留释放） |

## 实现落位提示

| 设计含义 | 默认实现落位 |
|----------|--------------|
| BOM 展开递归、齐套校验、完工数量计算 | Entity/Processor（稳定领域逻辑） |
| 工单 CRUD、领料/完工动作 | BizModel（事务入口） |
| 领料/完工写库存的多步骤编排 | Processor（生成移动单 + 更新工单进度 + 触发财务） |
| 成本结转凭证触发 | 财务域监听（本域实现 `IErpFinAcctDocProvider`） |
| 完工质检触发 | quality 域监听工单完工事件 |
