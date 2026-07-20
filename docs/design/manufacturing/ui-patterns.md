# 制造域页面设计要点

> 本文档定义制造域关键业务页面的结构布局、交互模式与导航流程。
> 字段定义以 `model/app-erp-manufacturing.orm.xml` 为准，业务语义与状态机见 `state-machine.md`、`bom-and-routing.md`。
> 调研引用格式 `[源项目#要点]`，详见 `docs/analysis/erp-survey/`。

## 设计原则

1. **BOM 树形可视化**：BOM 结构以树形展开（父物料→子件→子件的子件），支持多级 BOM 展开/折叠，显示每层用量。
2. **工单进度可跟踪**：工单详情页集中展示"计划 → 领料 → 报工 → 完工"四阶段的进度条，每阶段百分比可视化。
3. **领料与完工联动**：工单的领料移动单和完工移动单在工单详情页统一管理，不跳转到库存域页面。
4. **工序卡（JobCard）时效跟踪**：工序卡显示标准工时 vs 实际工时对比，超时高亮标记。

## 页面清单

| 页面 | 类型 | 主要用户 | 复杂度 |
|------|------|----------|--------|
| BOM 编辑 | 表单+树形子表 | 工艺员 | ★★★ |
| BOM 树形浏览 | 只读树+详情 | 工艺员/计划员 | ★★☆ |
| 工单编辑 | 表单+子表 | 计划员 | ★★★ |
| 工单详情（进度） | 仪表板式详情 | 计划员/车间主任 | ★★★ |
| 作业卡（JobCard）操作 | 卡片式操作 | 操作工 | ★★☆ |
| 工艺路线编辑 | 步骤列表 | 工艺员 | ★★☆ |
| 工作中心管理 | 表单 | 车间主任 | ★☆☆ |

## 各页面设计要点

### BOM 编辑

**页面入口**：制造管理 → BOM 管理 → 新建/编辑 BOM

**头区**：
```
┌────────────────────────────────────────────────┐
│ 产出物料: [M2M选择]  版本: [自动]  数量: [___]  │
│ BOM 类型: (制造 / 虚拟件)                      │
│ 单位: (自动带出)                               │
│ 默认: [☑] 激活: [☑]                           │
│ 备注: [___]                                     │
└────────────────────────────────────────────────┘
```
[注：当前 ORM 中 ErpMfgBom 尚无"数量"和独立"版本"字段（版本由 ErpMfgProductionVersion 管理）。这些为设计意图，待补充 ORM。]

**子件子表**（核心区域）：
```
┌──────────────────────────────────────────────────────────┐
│ [+] [从 BOM 导入] [多级展开]  [校验 BOM 完整性]           │
│ ┌────┬────────┬──────┬────┬────┬──────┬──────┬─────────┐│
│ │序号│ 子件   │用量   │单位│损耗率│工序  │仓库  │替代料  ││
│ │ 1  │[选择]  │2      │PC  │5%    │OP10  │WH1   │[可选]  ││
│ │ 2  │[选择]  │1      │PC  │0%    │OP20  │WH1   │        ││
│ └────┴────────┴──────┴────┴────┴──────┴──────┴─────────┘│
│ ──────────────────────────────────────────────────────── │
│ 联副产品子表:                                            │
│ ┌────┬────────┬──────┬────┬────────┐                    │
│ │ 物料   │ 类型   │ 产出率│ 单位│ 成本分摊%│            │
│ └────┴────────┴──────┴────┴────────┘                    │
└──────────────────────────────────────────────────────────┘
```

**要点**：
- 子件物料选择弹窗显示：物料编码、名称、规格、库存单位、当前库存水平
- 工序列下拉选择（从工艺路线中选择），将子件绑定到特定工序消耗
- "多级展开"按钮展示完整 BOM 树（递归展开子 BOM）
- "校验 BOM 完整性"按钮检查：物料是否存在、子件是否有 BOM（若子件是制造件）、是否成环
- [注：ORM 中 ErpMfgBomLine 尚无"损耗率""仓库""替代料"列，ErpMfgBomByproduct 尚无"类型""产出率""成本分摊%"列。这些为设计意图，待补充 ORM。]

### BOM 树形浏览

**页面入口**：制造管理 → BOM 查询

```
┌────────────────────────────────────────────────┐
│ 产出物料: [M2M选择]  版本: [下拉选择]            │
│ [展开全部] [折叠] [打印]                         │
├────────────────────────────────────────────────┤
│ 📦 MAT-001 成品 A (产出量: 1)                   │
│  ├─ 📦 MAT-010 子件 X × 2 (消耗工序 OP10)       │
│  │   └─ 📦 MAT-020 子-sub X1 × 1              │
│  ├─ 📦 MAT-011 子件 Y × 1 (消耗工序 OP20)       │
│  └─ 📦 MAT-012 子件 Z × 0.5 (消耗工序 OP20)    │
│      └─ (虚拟件展开→ 实际消耗子件)               │
│                                                 │
│ 📎 工艺路线: OP10(切割) → OP20(组装) → OP30(检验)│
└────────────────────────────────────────────────┘
```

**要点**：
- 树形节点显示：物料编码、名称、用量、消耗工序、单位
- 虚拟件（phantom）节点特殊标记（如 📎 图标），点击展开真实消耗
- 工艺路线以水平流程图展示在各层级上方
- [注：ORM 中 ErpMfgJobCard 尚无 code 列（作业卡编号），设计意图待补充 ORM。]

### 工单详情（进度仪表板）

**页面入口**：制造管理 → 工单管理 → 点击工单

```
┌────────────────────────────────────────────────────────────┐
│ 工单号: WO-2026-001  产出: 成品 A × 100  状态: ⚙ 生产中    │
├────────────────────────────────────────────────────────────┤
│ 进度总览                                                    │
│ 计划 ━━━━━━━━━━━━━━ ■━━━━━━━━━━━━  80%                     │
│ 领料 ━━━━━━━━━━━━━ ■━━━━━━━━━━━━  75% (已领 80/100 套)    │
│ 报工 ━━━━━━━━━━ ■━━━━━━━━━━━━━━  50% (OP10:100% OP20:0%) │
│ 完工 ━━━━━ ■━━━━━━━━━━━━━━━━━━  20% (已入库 20)            │
├────────────────────────────────────────────────────────────┤
│ 领料/完工移动单                                              │
│ ┌────┬────────┬──────┬───────┬────────┬────────┐          │
│ │ 类型│ 移动单号│ 物料 │ 数量  │ 状态   │ 操作   │          │
│ │ 领料│ SM-001 │ 子件X│ 200   │ ✅完成 │ [查看] │          │
│ │ 领料│ SM-002 │ 子件Y│ 100   │ ⏳待领 │ [执行] │          │
│ │ 完工│ SM-003 │ 成品A│ 20    │ ✅完成 │ [查看] │          │
│ └────┴────────┴──────┴───────┴────────┴────────┘          │
├────────────────────────────────────────────────────────────┤
│ 工序作业卡列表                                              │
│ ┌────┬───────┬────────┬──────┬──────┬──────┬────────────┐ │
│ │ 工序│ 作业卡│ 工作中心│计划数│完成数│工时比│ 操作       │ │
│ │ OP10│ JC-001│ 切割机│ 100  │ 100  │ 1.2x │ [完成]     │ │
│ │ OP20│ JC-002│ 组装线│ 100  │ 50   │ 0.9x │ [报工]     │ │
│ └────┴───────┴────────┴──────┴──────┴──────┴────────────┘ │
└────────────────────────────────────────────────────────────┘
```

**要点**：
- 进度条颜色：100%=绿色、50-99%=蓝色、<50%=黄色、0%=灰色
- 工时比 > 1.0（超时）红色标记，< 1.0（省时）绿色标记
- 移动单行操作：未完成的显示"执行"按钮（直接推进到 DONE），完成的显示"查看"
- 领料/完工移动单通过 I*Biz 调用库存域实现，工单详情页通过 ErpMfgMaterialIssue.workOrderId 间接查询 [注：ErpMfgWorkOrder 无直连 to-many 指向 ErpMfgMaterialIssue；完工入库实体为设计意图待补充 ORM]
- 报工操作：弹出报工表单（完成数量、工时、报废数量）

## 跨页面导航流

```
BOM 定义 → [BOM 展开查询] → [引用此 BOM 的工单列表]
    ↓
生产计划 → 创建工单 (引用 BOM)
    ↓
工单详情 → [审核] → [领料] → [报工] → [完工入库]
    ↓          ↓          ↓
 作业卡列表  领料移动单   质检触发
    ↓
 工序执行 → [报工录入] → [工时记录]
```

## 调研参考

| 设计点 | 参考来源 | 应用方式 |
|--------|----------|----------|
| BOM 标准字段集 | Odoo#mrp.bom | BOM 编辑页面字段布局 |
| 工单 + 作业卡双层 + 工时卡 | ERPNext#Work Order + Job Card | 工单详情嵌入作业卡列表 + 工时记录 |
| 工单 9 态状态机 | ERPNext#work_order.json | 工单状态标签 + 迁移按钮 |
| BOM 树形展开 + 多版本 | Odoo#mrp.bom | 树形浏览 + is_active/is_default 版本管理 |
| 齐套校验 + 物料预留维度 | ERPNext#Material Request | 工单领料阶段的预留量展示 |
| 联副产品管理 | Odoo#mrp.bom.byproduct | BOM 子表增加联副产品子表 |

## 主交易实体 form 布局分组

> 适用范围：制造域 11 个主交易实体（不含已 1500-1 覆盖的 `ErpMfgWorkOrder` / `ErpMfgBom` / `ErpMfgJobCard`）独立 `view.xml` 的 `<form id="view">` / `<form id="edit">` 分组。
> 决策来源：`docs/plans/2026-07-20-2059-1-f3-p1-mfg-tier-form-layout.md` Phase 0.A。
> 制造域主实体普遍含 `docStatus/approveStatus/status` 单一状态轴；委外/领料/差异类需要 amount/posting/variance 等业务专用组。

### 模板分化决策

| 实体 | 分组结构 |
|------|----------|
| ErpMfgMrpPlan | baseInfo + status + audit |
| ErpMfgForecast | baseInfo + status + audit |
| ErpMfgSubcontractOrder | baseInfo + amount + status + posting + audit |
| ErpMfgMaterialIssue | baseInfo + amount + status + posting + audit |
| ErpMfgCostRollup | baseInfo + status + audit |
| ErpMfgCostVariance | baseInfo + standard + actual + variance + audit |
| ErpMfgMrpDemand | baseInfo + quantity + reference + audit |
| ErpMfgBatchGenealogy | baseInfo + input + output + date + audit |
| ErpMfgProductionVersion | baseInfo + validity + audit |
| ErpMfgRouting | baseInfo + audit |
| ErpMfgCrpLoad | baseInfo + load + audit |

### ErpMfgSubcontractOrder 模板（含金额+审批+过账三状态轴，状态复杂实体）

```xml
<form id="view" size="lg">
    <layout x:override="replace">
=========>baseInfo[基本信息]======
 code[委外单号] orgId[业务组织]
 workOrderId[工单] supplierId[供应商]
 workcenterId[工作中心] routingId[工艺路线]
 productionVersionId[生产版本] productId[产品]
 businessDate[业务日期]
=========>amount[金额信息]======
 currencyId[币种] exchangeRate[汇率]
 processingFee[加工费] totalAmount[总金额]
 amountSource[源币种金额] amountFunctional[本位币金额]
=========>status[状态信息]======
 docStatus[单据状态] approveStatus[审核状态]
 approvedBy[审核人] approvedAt[审核时间]
=========>posting[过账信息]======
 posted[已过账] postedStatus[过账状态]
 postedAt[过账时间] postedBy[过账人]
========^audit[审计信息]=========
 remark[备注]
 createdBy[创建人] createTime[创建时间]
 updatedBy[修改人] updateTime[修改时间]
    </layout>
</form>
```

### ErpMfgCostVariance 模板（突出差异分析）

```xml
<form id="view" size="lg">
    <layout x:override="replace">
=========>baseInfo[基本信息]======
 workOrderId[工单] lineNo[行号]
 varianceType[差异类型] costElement[成本要素]
 materialId[物料] operationId[工序]
 workcenterId[工作中心] businessDate[业务日期]
=========>standard[标准成本]======
 standardQty[标准数量] standardPrice[标准单价]
 standardAmount[标准金额]
=========>actual[实际成本]======
 actualQty[实际数量] actualPrice[实际单价]
 actualAmount[实际金额]
=========>variance[差异分析]======
 varianceAmount[差异金额] variancePercent[差异百分比]
 posted[已过账]
========^audit[审计信息]=========
 remark[备注]
 createdBy[创建人] createTime[创建时间]
 updatedBy[修改人] updateTime[修改时间]
    </layout>
</form>
```

### query 表单基线

所有制造域主实体的 `<form id="query">` 至少含 5 个查询字段。`code` 配 `filterOp=like`；`orgId`/`status`/`docStatus`/`approveStatus` 配 `filterOp=eq`；含日期字段（如 `businessDate`）的实体配 `filterOp=date-between`。

## Line 子实体 form 分组模板

> 适用范围：制造域 11 个 Line 子实体（不含已 1500-1 覆盖的 ErpMfgBom 的内嵌 lines cell）独立 `view.xml` 的 `<form id="view">` / `<form id="edit">` 分组。
> 制造域 Line 模板**统一度高**：多数为 baseInfo + quantity + reference + audit；含成本行（ErpMfgCostRollupLine）增加 cost 组。

### 模板分化决策

| 实体 | 分组结构 |
|------|----------|
| ErpMfgWorkOrderLine | baseInfo + quantity + reference + audit |
| ErpMfgMaterialIssueLine | baseInfo + quantity + cost + reference + audit |
| ErpMfgSubcontractOrderLine | baseInfo + quantity + amount + reference + audit |
| ErpMfgBomLine | baseInfo + quantity + reference + audit |
| ErpMfgBomByproduct | baseInfo + quantity + reference + audit |
| ErpMfgBomOperation | baseInfo + operation + reference + audit |
| ErpMfgCostRollupLine | baseInfo + cost + reference + audit |
| ErpMfgForecastLine | baseInfo + quantity + reference + audit |
| ErpMfgJobCardTimeLog | baseInfo + quantity + cost + reference + audit |
| ErpMfgMrpPlanLine | baseInfo + quantity + reference + audit |
| ErpMfgRoutingOperation | baseInfo + operation + reference + audit |

### ErpMfgWorkOrderLine 模板（基准）

```xml
<form id="view" size="lg">
    <layout x:override="replace">
=========>baseInfo[基本信息]======
 lineNo[行号] lineType[行类型]
 materialId[物料] skuId[SKU] uoMId[计量单位]
=========>quantity[数量信息]======
 plannedQuantity[计划数量] actualQuantity[实际数量]
 scrappedQuantity[报废数量]
 sourceWarehouseId[来源仓库] destWarehouseId[目标仓库]
=========>reference[业务关联]======
 workOrderId[工单]
========^audit[审计信息]=========
 remark[备注]
 createdBy[创建人] createTime[创建时间]
 updatedBy[修改人] updateTime[修改时间]
    </layout>
</form>
```

### ErpMfgCostRollupLine 模板（成本行）

```xml
<form id="view" size="lg">
    <layout x:override="replace">
=========>baseInfo[基本信息]======
 lineNo[行号] materialId[物料] uoMId[计量单位]
=========>cost[成本信息]======
 materialCost[材料成本] laborCost[人工成本]
 overheadCost[制造费用] subcontractCost[委外成本]
 totalCost[总成本] unitCost[单位成本] currencyId[币种]
=========>reference[业务关联]======
 costRollupId[成本滚算单]
========^audit[审计信息]=========
 remark[备注]
 createdBy[创建人] createTime[创建时间]
 updatedBy[修改人] updateTime[修改时间]
    </layout>
</form>
```

### query 表单基线

所有制造域 Line 实体的 `<form id="query">` 至少含 5 个查询字段。`lineNo` 配 `filterOp=eq`；`materialId` 配 `filterOp=eq`；含日期字段（如 `plannedDate`/`workDate`）的实体配 `filterOp=date-between`；状态字段（如 `status`）配 `filterOp=eq`。
