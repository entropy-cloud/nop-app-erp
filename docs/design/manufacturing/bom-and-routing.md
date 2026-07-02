# BOM 与工艺路线

## 目的

说明 BOM 结构、工艺路线、多级展开规则、齐套校验、成本计算的业务语义。

## BOM 模型

### BOM 头（Bom）

| 字段语义 | 说明 |
|----------|------|
| 产出物料 | 该 BOM 生产的产品（物料/SKU） |
| 产出量 + 单位 | 该 BOM 一次产出的标准数量与计量单位 |
| 类型 | normal（制造）/ phantom（虚拟件/Kit，展开不实际生产） |
| 激活/默认 | is_active + is_default：同一产出物料可有多个 BOM，默认 BOM 用于工单自动选择 |
| 是否含工艺 | with_operations：是否绑定工艺路线 |
| 检验要求 | inspection_required：完工是否触发质检 |
| 允许替代料 | allow_alternative_item：子件是否允许使用替代物料 |
| 消耗控制 | consumption：flexible（允许超耗）/ warning（超耗警告）/ strict（严格按 BOM） |
| 齐套策略 | ready_to_produce：all_available（齐套才可生产）/ asap（可部分齐套生产） |
| 批量 | batch_size：标准生产批量 |

### BOM 子件行（BomItem）

| 字段语义 | 说明 |
|----------|------|
| 子件物料 | 消耗的原材料/零件 |
| 数量 + 单位 | 单位产出消耗的子件数量 |
| 序号 | 行顺序 |
| 绑定工序 | operation：该子件在哪道工序消耗（可空，表示首道工序） |
| 替代料标识 | bom_product_template_attribute_value_ids：按变体生效 |
| 递归子 BOM | child_bom：展开时的递归引用 |

### BOM 工艺行（BomOperation）

| 字段语义 | 说明 |
|----------|------|
| 工序 | operation（引用工艺路线的工序） |
| 工作中心 | workstation / workstation_type |
| 标准工时 | time_in_mins（分钟） |
| 固定工时 | fixed_time（不随批量变化的固定工时） |
| 费率 | hour_rate（工作中心的小时费率） |
| 工序成本 | operating_cost = (time_in_mins/60) × hour_rate |
| 批量 | batch_size |
| 在制/完工仓 | wip_warehouse / fg_warehouse |

### BOM 联副产品（BomByproduct）

联产品/副产品：生产过程中与主产品同时产出的其他产品。完工时一并入库，成本按分摊规则计算。

## 工艺路线（Routing）

工艺路线是工序序列的独立定义，可被多个 BOM 引用（复用）：

- 工序（Operation）：一道独立工序，有标准工时、工作中心。
- 工序依赖：工序间有先后顺序（线性或带分支）。
- 子工序（SubOperation）：工序可细分为子工序。

> BOM 与工艺路线的关系：BOM 通过 `with_operations` 决定是否绑定工艺。绑定后，工单引用 BOM 即获得完整工艺路线；未绑定则工单无工序（只做领料/完工，不做报工）。

## 多级 BOM 展开

工单审核时可配置 `use_multi_level_bom` 控制展开深度：

- **单级展开**（false）：只展开当前 BOM 的直接子件。
- **多级展开**（true）：递归展开子件的子件 BOM，直到所有层级。

展开规则：
- 遇到 `phantom` 类型子件时，展开其子件而非创建生产订单（虚拟件不实际生产）。
- 展开结果缓存到 BOM 展开明细表（避免每次重复计算）。
- 多级展开数量按上层需求量乘积计算。

## 齐套校验

工单审核后进入 NOT_STARTED 状态，进行齐套校验：

1. 按 BOM 子件清单 × 工单产出量计算每个子件的需求量。
2. 查询每个子件在指定仓库的可用量（现有量 − 预留量）。
3. 全部满足 → STOCK_RESERVED（已齐套）；部分不足 → STOCK_PARTIAL（部分齐套）。

齐套后系统预留子件库存（增加预留量，减少可用量），供本工单领料使用。

## 成本计算

工单成本由三部分构成：

| 成本项 | 来源 |
|--------|------|
| 直接材料成本 | 领料移动单的物料成本（按移动时单位成本 × 数量） |
| 直接人工成本 | JobCard 工时记录 × 工作中心人工费率 |
| 制造费用 | 工序工时 × 工作中心制造费率（含设备折旧分摊等） |

完工入库时按成本计算结果确定产成品单位成本，生成存货估值凭证：
- 借：产成品存货（按完工成本）
- 贷：原材料存货（领料成本）+ 在制品（人工+制造成本结转）

成本计算方法由财务域的成本核算模块决定（移动加权平均/FIFO/标准成本）。

### 多级成本卷算（Cost Rollup）

制造件（非采购件）的成本须通过 BOM 多级展开逐层计算。卷算流程：

1. **确定层级**：遍历 BOM 树，按低层编码排序。采购件（物料类型=PURCHASE）取采购价作为基础成本；制造件（物料类型=MANUFACTURE）的初值为 0。
2. **逐层卷算**：从最低层级开始向上汇总—某层物料成本 = 直接材料（各子件用量 × 子件单位成本）+ 直接人工（工时 × 人工费率）+ 制造费用（工时 × 制造费率）。子件为制造件时，其单位成本取上一步计算结果。
3. **联副产品分摊**：联产品/副产品产出时，按约定分摊规则（按重量/按售价/按数量）从主产品成本中扣减。
4. **写入成本价**：卷算结果写入物料主数据的标准成本字段（`ErpMdProduct.standardCost`），用于工单完工时的存货估值。

卷算触发方式：
- **MANUAL**：财务员手动触发（推荐，结果审核后再发布）。
- **AUTO_ON_BOM_CHANGE**：BOM 变更时自动重算。

## 外协工序

部分工序外协给供应商加工：

- BOM 工艺行标记外协工序。
- 外协费用通过采购订单或外协发票记录。
- 外协费用计入工单成本。

## 与其他域的关系

- **inventory**：领料/完工通过移动单写库存；移动单的成本数据用于成本计算。
- **finance**：完工触发存货估值与成本结转凭证。
- **quality**：完工触发质检（若 `inspection_required`）。
- **master-data**：物料/SKU/仓库/工作中心引用主数据。

## BOM 版本快照规则

工单创建时对 BOM 进行快照，确保生产过程中 BOM 变更不影响已下达工单：

- **快照时机**：工单从 DRAFT → SUBMITTED 时，将当前 BOM 头+子件行+工艺行复制到工单的快照字段
- **快照内容**：BOM 头（版本号、产出量）、子件行（物料、数量、工序）、工艺行（工序、工作中心、费率）
- **快照锁定**：工单创建后，即使 BOM 被修改，工单仍使用快照版本
- **版本追溯**：工单记录 `snapshotBomVersion` 字段，用于追溯生产时使用的 BOM 版本
- **策略配置**：`erp-mfg.bom-snapshot-strategy`（LOCK_AT_CREATION：创建时锁定 / AUTO_UPGRADE：自动升级到最新版本，按物料可配）

## 实现注记（漂移补注，计划 2026-07-02-1538-2）

> 以下为实现落地与本文档概念表述的偏离裁决，权威计划：`docs/plans/2026-07-02-1538-2-manufacturing-bom-routing-rollup.md`。

- **卷算结果落地载体**：§多级成本卷算 步骤4 称「写入 `ErpMdProduct.standardCost`」为概念漂移——本仓无 `ErpMdProduct`，且按物料冗余单值会丢失版本/状态/成本分解。实现将卷算结果写入既有 `ErpMfgCostRollup`（status 门控 DRAFT/CALCULATED/FIRMED）+ `ErpMfgCostRollupLine`（materialCost/laborCost/overheadCost/subcontractCost/totalCost/unitCost），为标准成本权威载体（版本化 + FIRMED 门控 + 成本分解）。向 `ErpMdMaterial` 冗余发布 `standardCost` 仅作 N=1 STANDARD 高频读取去 join 优化（Follow-up，Non-Goal）。
- **采购件/制造件判定**：本文档步骤1 称「物料类型=PURCHASE/MANUFACTURE」，但 `erp-md/material-type` 字典为 商品/原材料/半成品/产成品/服务/包装物/消耗品（无 PURCHASE/MANUFACTURE）。实现按 **BOM 存在性** 判定：物料有默认且有效 BOM → 制造件（卷算递归）；否则 → 采购件（取默认 SKU `purchasePrice` 为基础成本）。
- **采购件基础成本源**：取默认 SKU 的 `ErpMdMaterialSku.purchasePrice`（DECIMAL 既有）；取最近入库 cost layer（跨 N=1 成本引擎耦合）为准确性增强（Follow-up，Non-Goal）。SKU 未配价时该采购件卷算抛 `ERR_ROLLUP_BASE_COST_MISSING`。
- **工时/费率列类型修正**：`ErpMfgBomOperation.standardTime`、`ErpMfgRoutingOperation.standardTime`/`setupTime`/`runTime` 原列类型 DATETIME，`ErpMfgWorkcenter.hourlyRate` 原列类型 VARCHAR，与其 DECIMAL domain（`timeInMins`/`hourlyRate`）矛盾（存工时分钟数/费率却用 datetime/字符串，模型类型缺陷）。已修正列类型为 DECIMAL 对齐 domain，使工序成本 `standardTime/60 × workcenter.hourlyRate` 可数值计算。本文档工序行的 `time_in_mins`/`fixed_time`/`hour_rate`/`operating_cost` 为概念名，对应 ORM 列名 `standardTime`/`hourlyRate`。
- **人工/制造费用分列**：工作中心仅有单一 `hourlyRate`（无独立人工/制造费率分列），故工序工时成本统一计入 `ErpMfgCostRollupLine.laborCost`，`overheadCost`=0；待工作中心费率拆分后细化（Follow-up）。`subcontractCost` 预留给外协工序（Non-Goal）。
- **本期 Non-Goal**：WorkOrder/JobCard 状态机（2.2）、MRP（2.3）/CRP（2.8）、完工成本结转凭证（依赖 N=1 + WorkOrder）、联副产品分摊（步骤3）、外协工序成本、BOM 版本快照、AUTO_ON_BOM_CHANGE 自动重算、展开结果缓存表、`scrapRate` 纳入有效用量（损耗精细化）。卷算本期仅 MANUAL 触发 + 按标准用量计算。
