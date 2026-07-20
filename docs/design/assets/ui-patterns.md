# 固定资产域页面设计要点

> 本文档定义固定资产域关键业务页面的结构布局、交互模式与导航流程。
> 字段定义以 `model/app-erp-assets.orm.xml` 为准，业务语义与状态机见 `state-machine.md`、`depreciation-and-posting.md`。
> 调研引用格式 `[源项目#要点]`，详见 `docs/analysis/erp-survey/`。

## 设计原则

1. **一物一卡**：每项固定资产一张卡片，卡片集中展示资产全生命周期信息（原值、折旧、净值、状态）。
2. **折旧计划可视化**：折旧计划以时间轴形式展示未来各期折旧额和账面净值变化曲线。
3. **处置流程引导**：报废/出售流程分步引导（选择资产 → 确认处置原因 → 预览清理损益 → 执行），避免误操作。
4. **资产移动不改价值**：资产位置/部门转移操作独立于价值变更，页面清晰区分"移动"和"价值调整"。

## 页面清单

| 页面 | 类型 | 主要用户 | 复杂度 |
|------|------|----------|--------|
| 资产卡片编辑 | 表单 | 资产管理员 | ★★☆ |
| 资产卡片详情 | 仪表板式详情 | 资产管理员/财务员 | ★★★ |
| 折旧计划查看 | 列表+图表 | 财务员 | ★★☆ |
| 资产移动登记 | 表单 | 资产管理员 | ★☆☆ |
| 资产价值调整 | 表单 | 财务员 | ★★☆ |
| 资产处置向导 | 分步向导 | 财务员 | ★★★ |
| 资产类别配置 | 表单+卡片式 | 管理员 | ★☆☆ |

## 各页面设计要点

### 资产卡片详情

**页面入口**：资产管理 → 资产列表 → 点击卡片

```
┌────────────────────────────────────────────────────────────┐
│ 资产编码: AST-2026-001  状态: 🟢 使用中                     │
│ ────────────────────────────────────────────────────────── │
│ ┌───── 基本信息 ────────────┐ ┌─── 财务信息 ─────────────┐ │
│ │ 资产名称: 服务器 X100      │ │ 原值: ¥ 200,000          │ │
│ │ 类别: 电子设备             │ │ 残值率: 5% → 残值: ¥10,000│ │
│ │ 取得日期: 2024-01-15      │ │ 累计折旧: ¥ 95,000        │ │
│ │ 使用部门: IT 部            │ │ 账面净值: ¥ 105,000       │ │
│ │ 存放位置: 机房 A-01        │ │ 折旧方法: 直线法           │ │
│ │ 品牌型号: Dell R740        │ │ 折旧年限: 5 年            │ │
│ └───────────────────────────┘ └────────────────────────────┘
│ ────────────────────────────────────────────────────────── │
│ 操作按钮组: [移动] [价值调整] [处置] [查看折旧计划] [查看凭证] │
│ ────────────────────────────────────────────────────────── │
│ 折旧计划时间轴                                                │
│ ┌─ 2024 ──┬─ 2025 ──┬─ 2026 ──┬─ 2027 ──┬─ 2028 ──┬─ 2029┐ │
│ │ ▓▓▓▓▓▓▓ │ ▓▓▓▓▓▓▓▓│ ▓▓▓▓▓▓▓▓│ ▓▓▓▓░░░░│ ░░░░░░░░│░░░░░│ │
│ │ 已折旧   │ 已折旧   │ 已折旧   │ 折旧中   │ 待折旧   │      │ │
│ │ 3.17万   │ 3.17万   │ 3.17万   │ 1.58万   │ 0       │      │ │
│ └─────────┴─────────┴─────────┴─────────┴─────────┴──────┘ │
│ ────────────────────────────────────────────────────────── │
│ 关联凭证列表:                                                │
│ 2024-02-01 | 折旧凭证 | DEP-2024-001 | ¥3,167 [查看]        │
│ 2024-03-01 | 折旧凭证 | DEP-2024-002 | ¥3,167 [查看]        │
└────────────────────────────────────────────────────────────┘
```

**要点**：
- 财务信息区字段以数值展示，原值/折旧/净值用不同颜色区分
- "品牌型号"、经手人"等字段在当前 ORM 中尚无对应列，为设计意图待补充 [注：ErpAstAsset 无 brand/model 列；ErpAstMovement 无 handler 列；ErpAstDisposal 无 reason 列]
- 折旧计划时间轴：已折旧部分实心填充，未折旧部分虚线描边
- 当前折旧期高亮标记
- "处置"按钮仅在资产状态为"使用中"或"闲置"时可用

### 资产处置向导

**弹窗/抽屉引导**：
```
步骤 1: 选择处置类型
├─ 报废 (清理损失)
└─ 出售 (清理收益/损失) → 输入出售金额

步骤 2: 确认处置信息
├─ 处置日期: [选择]
├─ 处置原因: [下拉 + 备注]
├─ 预览清理损益:
│  原值: ¥200,000 - 累计折旧: ¥95,000 - 处置费用: ¥2,000 = 清理损益: ¥103,000
└─ 关联凭证预览 (自动生成)

步骤 3: [确认处置]
```

### 资产移动登记

**简单表单**：
- 资产（自动带出）、原位置/部门（自动带出）、目标位置/部门（必选）
- 移动日期、移动原因、经手人
- 移动后仅更新卡片上的位置/部门字段，不触发任何财务动作

## 跨页面导航流

```
资产类别配置 → 资产列表 → 新建资产卡片
    ↓
资产卡片详情 → [折旧计划] → [执行折旧] → [查看折旧凭证]
    ↓
资产卡片详情 → [价值调整] → 输入调整金额/原因 → [生成调整凭证]
    ↓
资产卡片详情 → [处置] → 处置向导 → [确认] → [生成清理凭证]
    ↓
资产卡片详情 → [移动] → 选择目标位置/部门 → [确认]
```

## 调研参考

| 设计点 | 参考来源 | 应用方式 |
|--------|----------|----------|
| 资产卡片信息集中展示 | 管伊佳#Material 扩展自资产 | 卡片详情页集中全部字段 |
| 折旧计划时间轴可视化 | Yu-FAMS#DepreciationSchedule | 水平时间轴展示折旧进度 |
| 处置清理损益预览 | OFBiz#AssetDisposal | 处置前预览清理损益计算 |
| 资产类别绑定科目映射 | OFBiz#AssetCategory | 资产类别配置中设置默认折旧科目 |


## 非状态 visibleOn 模式（F7 §1）

> 落地计划：`docs/plans/2026-07-20-1020-2-f7-non-status-visibleon-and-master-data-interactions.md`
> 跨域范式参考：`docs/design/visible-on-patterns.md`

assets 域含字段值驱动 visibleOn 的实体：

| 实体 | 字段 | 表达式 | clearValueOnHidden | 业务语义 |
|------|------|--------|-------------------|----------|
| `ErpAstMaintenance` | `capitalizedAmount` | `${treatment == 'CAPITALIZE'}` | true | 费用化（EXPENSE）时无资本化金额 |

`erp-ast/maintenance-treatment` 字典值：`CAPITALIZE`/`EXPENSE`（2 值）。
`CAPITALIZE` 时 `capitalizedAmount` 显；`EXPENSE` 时隐藏并清空（防隐藏字段提交脏数据）。

写法与反模式见 `visible-on-patterns.md §4`。

## 主交易实体 form 布局分组

> 适用范围：固定资产域 8 个主交易实体（不含已 1500-1 覆盖的 `ErpAstAsset` / `ErpAstDepreciationSchedule` 与 F4P2 已覆盖的 `ErpAstInventory`）独立 `view.xml` 的 `<form id="view">` / `<form id="edit">` 分组。
> 决策来源：`docs/plans/2026-07-20-2059-1-f3-p1-mfg-tier-form-layout.md` Phase 0.B。
> 资产域主实体高度同构：基本都含 `docStatus/approveStatus/approvedBy/approvedAt` 审批轴 + `posted/postedAt/postedBy` 过账轴 + 多币种金额（amountSource/amountFunctional/exchangeRate/currencyId）。CIP / Disposal / ValueAdjustment 为状态复杂实体，需突出处置/调整类型字段。

### 模板分化决策

| 实体 | 分组结构 |
|------|----------|
| ErpAstAssetCapitalization | baseInfo + amount + status + posting + audit |
| ErpAstCip | baseInfo + amount + status + posting + audit（≥20 字段，size=lg） |
| ErpAstDisposal | baseInfo + amount + status + posting + audit |
| ErpAstMaintenance | baseInfo + amount + status + posting + audit |
| ErpAstMerge | baseInfo + amount + status + posting + audit |
| ErpAstMovement | baseInfo + transfer + amount + status + posting + audit |
| ErpAstSplit | baseInfo + amount + status + posting + audit |
| ErpAstValueAdjustment | baseInfo + amount + status + posting + audit |

### ErpAstDisposal 模板（状态复杂实体，含处置类型 + 审批轴）

```xml
<form id="view" size="lg">
    <layout x:override="replace">
=========>baseInfo[基本信息]======
 code[处置单号] orgId[业务组织]
 assetId[资产] disposalType[处置类型]
 businessDate[业务日期] gainLoss[处置损益]
 reason[处置原因]
=========>amount[金额信息]======
 disposalAmount[处置金额] currencyId[币种]
 exchangeRate[汇率] amountSource[源币种金额]
 amountFunctional[本位币金额]
=========>status[状态信息]======
 docStatus[单据状态] approveStatus[审核状态]
 approvedBy[审核人] approvedAt[审核时间]
=========>posting[过账信息]======
 posted[已过账] postedAt[过账时间]
 postedBy[过账人]
========^audit[审计信息]=========
 remark[备注]
 createdBy[创建人] createTime[创建时间]
 updatedBy[修改人] updateTime[修改时间]
    </layout>
</form>
```

### ErpAstCip 模板（≥20 字段，size=lg）

```xml
<form id="view" size="lg">
    <layout x:override="replace">
=========>baseInfo[基本信息]======
 code[CIP编码] name[CIP名称]
 orgId[业务组织] categoryId[资产类别]
 projectId[项目] businessDate[业务日期]
 estimatedCompletionDate[预计完工日期]
 accumulatedCost[累计成本] isCompleted[是否已完工]
 completedAssetId[转资后资产] cipAssetCategorySnapshot[类别快照]
=========>amount[金额信息]======
 currencyId[币种] exchangeRate[汇率]
 amountSource[源币种金额] amountFunctional[本位币金额]
=========>status[状态信息]======
 status[状态]
=========>posting[过账信息]======
 posted[已过账] postedAt[过账时间]
 postedBy[过账人]
========^audit[审计信息]=========
 remark[备注]
 createdBy[创建人] createTime[创建时间]
 updatedBy[修改人] updateTime[修改时间]
    </layout>
</form>
```

### query 表单基线

所有资产域主实体的 `<form id="query">` 至少含 5 个查询字段。`code` 配 `filterOp=like`；`orgId`/`assetId`/`status`/`docStatus`/`approveStatus` 配 `filterOp=eq`；`businessDate` 配 `filterOp=date-between`。

## Line 子实体 form 分组模板

> 适用范围：固定资产域 6 个 Line 子实体独立 `view.xml` 的 `<form id="view">` / `<form id="edit">` 分组。
> 资产域 Line 模板分化大：CIP 类有专属 cost/billing 组；盘点行（InventoryLine）有 quantity+value 双组；分拆/合并行（SplitLine/MergeLine）有 value 组。

### 模板分化决策

| 实体 | 分组结构 |
|------|----------|
| ErpAstCipCostItem | baseInfo + amount + posting + reference + audit |
| ErpAstCipProgressBilling | baseInfo + amount + reference + audit |
| ErpAstInventoryLine | baseInfo + quantity + value + reference + audit |
| ErpAstMaintenanceCost | baseInfo + amount + reference + audit |
| ErpAstMergeLine | baseInfo + value + reference + audit |
| ErpAstSplitLine | baseInfo + value + reference + audit |

### ErpAstInventoryLine 模板（quantity + value 双组）

```xml
<form id="view" size="lg">
    <layout x:override="replace">
=========>baseInfo[基本信息]======
 lineNo[行号] assetId[资产]
 assetCodeSnapshot[资产编码快照] assetNameSnapshot[资产名称快照]
 categoryId[资产类别]
=========>quantity[数量信息]======
 bookQuantity[账面数量] actualQuantity[实际数量]
 varianceQuantity[差异数量] varianceType[差异类型]
=========>value[价值信息]======
 bookValue[账面价值] assessedValue[评估价值]
 varianceAmount[差异金额] disposition[处置方式]
=========>reference[业务关联]======
 inventoryId[资产盘点单] newAssetId[新资产]
 capitalizationId[转资单] disposalId[处置单]
========^audit[审计信息]=========
 remark[备注] investigatedRemark[调查备注]
 createdBy[创建人] createTime[创建时间]
 updatedBy[修改人] updateTime[修改时间]
    </layout>
</form>
```

### query 表单基线

所有资产域 Line 实体的 `<form id="query">` 至少含 5 个查询字段。`lineNo`/`assetId`/`categoryId` 配 `filterOp=eq`；`assetCodeSnapshot`/`assetNameSnapshot` 配 `filterOp=like`；`varianceType` 配 `filterOp=eq`。
