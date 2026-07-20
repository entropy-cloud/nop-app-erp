# 列表页查询/筛选条件范式（F8 + F2）

> 本文档固化 nop-app-erp 列表页 `query` + `asideFilter` 双筛选面字段集、`filterOp` 配置、
> 只读实体清单与「搜索 → 行点击 → 详情 dialog」范式。
>
> 来源 plan：`docs/plans/2026-07-20-0629-2-f8-f2-search-filter-and-readonly-views.md`。
> 关联文档：`docs/architecture/view-and-page-strategy.md`、各域 `docs/design/<domain>/ui-patterns.md`、
> `../nop-entropy/docs-for-ai/02-core-guides/page-customization.md`。

## 1. 双筛选面架构（codegen 默认）

Nop codegen 为每个头实体生成两个独立筛选面，绑定到同一 `<crud>`：

```xml
<crud name="main" grid="list" asideFilterForm="asideFilter" filterForm="query">
    <table autoFillHeight="true">
        <api url="@query:ErpXxx__findPage" gql:selection="{@pageSelection}"/>
    </table>
</crud>
```

- **`asideFilter`（侧边栏）** — `_gen/*.view.xml` 默认 `<form id="asideFilter" editMode="query" x:abstract="true" submitOnChange="true"/>` 空抽象 stub。语义：持久可见的多维筛选 sidebar，承载业务专用筛选字段（FK + status + date range），用户变更任意字段后自动 submit（`submitOnChange="true"`）。手写层通过 `<form id="asideFilter" x:override="bounded-merge">` 补字段。
- **`query`（顶部紧凑）** — `_gen/*.view.xml` 默认 `<form id="query" editMode="query" title="查询条件" x:abstract="true"/>` 空抽象 stub。语义：顶部紧凑的快速精确查找（如 `code`、`name`、`businessDate` 范围），需要点击「查询」按钮显式触发。手写层通过 `<form id="query" x:override="bounded-merge">` 或直接 `<form id="query" editMode="query">` 补字段。

### 1.1 双面字段分配规则

| 字段类型 | 落点 | 例子 |
|---------|------|------|
| 业务专用 FK（物料/仓库/供应商/客户/科目） | `asideFilter` | `materialId` `warehouseId` `supplierId` `customerId` `subjectId` |
| 业务状态（单据/审批/可用/启用） | `asideFilter` | `docStatus` `approveStatus` `status` |
| 日期范围（业务日期/创建时间） | `asideFilter` 或 `query`（择一，不重复） | `businessDate` `voucherDate` |
| 业务类型 / 枚举 | `asideFilter` | `businessType` `direction` `itemType` `voucherType` |
| 布尔开关（含零库存/含未过账） | `asideFilter` | `includeZero` `includeUnposted` |
| 快速精确查找（code/name 单字段） | `query` | `code` `name` `serialNo` `batchNo` |

**反模式**：同一字段同时出现在 `asideFilter` 和 `query`（后端 filter map 合并会覆盖，行为不确定）。
**反模式**：`asideFilter` 字段过多（>8）导致 sidebar 比表格还高 → 拆为「常用筛选 + 高级筛选折叠」。
**反模式**：`query` 字段过多（>5）失去「快速」语义 → 移到 `asideFilter`。

### 1.2 `filterOp` 配置规范

`filterOp` 决定后端 QueryBean filter 的运算符。codegen 默认按字段类型推断（字符串=`eq`、数字=`eq`、日期=`eq`），业务筛选通常需要 override：

| 字段类型 | filterOp | 后端转译 |
|---------|---------|---------|
| 编码字段（精确匹配） | `eq`（默认） | `code = ?` |
| 名称/批号/序列号（模糊匹配） | `like` | `name LIKE '%' || ? || '%'` |
| 日期字段（范围查询） | `date-between` 或 `ge` + `le` | `businessDate BETWEEN ? AND ?` |
| 数值字段（范围查询） | `ge` + `le` | `quantity >= ? AND quantity <= ?` |
| FK 字段（精确匹配） | `eq`（默认） | `materialId = ?` |
| 状态字段（精确匹配） | `eq`（默认） | `status = ?` |

`date-between` 模式下，单个 cell 在前端展开为「开始日期 + 结束日期」两个输入框，后端 filter map 自动生成 `fieldName: { ge: ?, le: ? }` 结构。

`like` 模式下，前端单输入框，后端 SQL `LIKE '%<value>%'`（前缀通配）。

## 2. 8 核心列表页双面字段集冻结表

> 决策来自 plan `2026-07-20-0629-2` Phase 1。每页字段数控制在 3-6 之间。

### 2.1 inventory: ErpInvStockLedger（库存流水，只读）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | materialId | eq | 物料筛选 |
| asideFilter | warehouseId | eq | 仓库筛选 |
| asideFilter | businessType | eq | 业务类型（采购入库/销售出库/调拨/盘点/生产）|
| asideFilter | businessDate | date-between | 业务日期范围 |
| asideFilter | batchNo | like | 批号模糊匹配 |
| query | code | like | 流水号快速查找 |
| query | businessDate | date-between | 顶部日期范围（与 asideFilter 择一，本表 query 用 code 作为顶部精确查找）|

### 2.2 inventory: ErpInvStockBalance（库存余额，只读）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | materialId | eq | 物料 |
| asideFilter | warehouseId | eq | 仓库 |
| asideFilter | locationId | eq | 库位（**新增**，扩展自 F3 4 字段）|
| asideFilter | batchNo | like | 批号 |
| asideFilter | orgId | eq | 业务组织 |
| asideFilter | includeZero | eq（boolean） | 含零库存勾选框（**新增**）|
| query | materialId + warehouseId | eq | 顶部快速物料/仓库查询 |

### 2.3 inventory: ErpInvBatch（批次台账，只读）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | batchNo | like | 批号 |
| asideFilter | materialId | eq | 物料 |
| asideFilter | warehouseId | eq | 仓库 |
| asideFilter | status | eq | 状态（正常/过期/冻结）|
| query | batchNo | like | 顶部批号快速查找 |

### 2.4 inventory: ErpInvSerialNumber（序列号台账，只读）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | serialNo | like | 序列号（全局搜索）|
| asideFilter | materialId | eq | 物料 |
| asideFilter | status | eq | 状态（在库/已售/冻结/报废）|
| query | serialNo | like | 顶部序列号全局搜索 |

### 2.5 purchase: ErpPurOrder（采购订单，可编辑）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | supplierId | eq | 供应商 |
| asideFilter | warehouseId | eq | 收货仓库 |
| asideFilter | docStatus | eq | 单据状态 |
| asideFilter | approveStatus | eq | 审核状态 |
| asideFilter | businessDate | date-between | 订单日期范围 |
| query | code | like | 单号快速查找 |
| query | businessDate | date-between | 顶部日期范围 |

### 2.6 sales: ErpSalOrder（销售订单，可编辑）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | customerId | eq | 客户 |
| asideFilter | warehouseId | eq | 发货仓库 |
| asideFilter | docStatus | eq | 单据状态 |
| asideFilter | approveStatus | eq | 审核状态 |
| asideFilter | businessDate | date-between | 订单日期范围 |
| query | code | like | 单号快速查找 |
| query | businessDate | date-between | 顶部日期范围 |

### 2.7 finance: ErpFinVoucher（凭证，可编辑）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | voucherType | eq | 凭证字（记/收/付/转）|
| asideFilter | subjectId | eq | 会计科目（辅助核算筛选）|
| asideFilter | docStatus | eq | 凭证状态（草稿/已过账/已红冲）|
| asideFilter | businessType | eq | 业务类型 |
| asideFilter | voucherDate | date-between | 凭证日期范围 |
| query | code | like | 凭证号快速查找 |
| query | voucherDate | date-between | 顶部日期范围 |

### 2.8 finance: ErpFinArApItem（应收付明细，可编辑）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | partnerId | eq | 往来单位 |
| asideFilter | direction | eq | 方向（应收/应付）|
| asideFilter | itemType | eq | 项目类型（发票/收款/付款/核销）|
| asideFilter | status | eq | 状态（未核销/部分/已核销）|
| asideFilter | businessDate | date-between | 业务日期范围 |
| query | code | like | 单号快速查找 |
| query | businessDate | date-between | 顶部日期范围 |

## 3. 只读实体清单与「搜索 → 行点击 → 详情 dialog」范式

### 3.1 只读实体清单（本 plan 核心 6 实体）

| 实体 | 域 | 角色 | 文件齐备 |
|------|----|------|---------|
| ErpInvStockLedger | inventory | 库存流水（不可变）| view.xml + main.page.yaml + picker.page.yaml |
| ErpInvStockBalance | inventory | 库存余额（实时结果）| view.xml + main.page.yaml + picker.page.yaml |
| ErpInvBatch | inventory | 批次台账（库存维度台账）| view.xml + main.page.yaml + picker.page.yaml |
| ErpInvSerialNumber | inventory | 序列号台账（单件追溯）| view.xml + main.page.yaml + picker.page.yaml |
| ErpFinGlBalance | finance | 总账余额（期末结果）| view.xml + main.page.yaml + picker.page.yaml |
| ErpFinTrialBalance | finance | 试算平衡表（结账结果）| view.xml + main.page.yaml + picker.page.yaml |

> 长尾只读实体（aps/logistics/maintenance 等域流水/日志）按需逐域补齐（本 plan Deferred）。

### 3.2 范式裁决：dialog 模式（方案 A）

**采纳**：复用既有 codegen 默认 `<rowActions><action id="row-view-button"><dialog page="view"/></action></rowActions>` 模式。
- codegen 已为每个实体生成 `row-view-button` + `<dialog page="view"/>` 配置；
- 手写层通过 `<rowActions x:override="bounded-merge">` 仅保留 `row-view-button`，丢弃 update/delete/more；
- 「dialog」即「详情弹窗」语义（AMIS `<dialog>` 模态弹窗），不引入 AMIS `<drawer>` 侧滑组件（方案 B 否决：无业务需求支撑，与 codegen 默认偏离）。

### 3.3 只读实体 view.xml 模板

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<view x:extends="_gen/_ErpXxx.view.xml" x:schema="/nop/schema/xui/xview.xdef" xmlns:x="/nop/schema/xdsl.xdef">

    <grids>
        <grid id="list"/>
        <grid id="pick-list"/>
    </grids>

    <forms>
        <form id="view"/>
        <!-- 切断 _gen 层 edit/add form 继承：禁止编辑/新增 -->
        <form id="edit" x:abstract="true"/>
        <form id="add" x:abstract="true"/>
        <!-- 双筛选面：业务专用多维筛选放 asideFilter，快速精确查找放 query -->
        <form id="asideFilter" editMode="query" submitOnChange="true">
            <layout>
 fieldA[字段A标签] fieldB[字段B标签]
 fieldC[字段C标签] fieldD[字段D标签]
            </layout>
            <cells>
                <cell id="fieldA" filterOp="eq"/>
                <cell id="fieldC" filterOp="like"/>
                <cell id="fieldD" filterOp="date-between"/>
            </cells>
        </form>
        <form id="query" editMode="query" title="查询条件">
            <layout>
 fieldE[快速查找字段] 
            </layout>
            <cells>
                <cell id="fieldE" filterOp="like"/>
            </cells>
        </form>
    </forms>

    <pages>
        <crud name="main">
            <listActions x:override="bounded-merge"/>
            <rowActions x:override="bounded-merge">
                <action id="row-view-button" level="primary" label="@i18n:common.view">
                    <dialog page="view"/>
                </action>
            </rowActions>
        </crud>
        <picker name="picker"/>
    </pages>
</view>
```

**关键点**：

1. `<form id="edit" x:abstract="true"/>` + `<form id="add" x:abstract="true"/>` 在手写层显式抽象 — 切断 _gen 层完整 form layout 继承，使得任何对 update/add 入口的引用都会失败（form 不存在 → AMIS 报错或弹空）。即使 row-update-button 残留，dialog 也无法填充表单。
2. `<listActions x:override="bounded-merge"/>` — 空内容丢弃所有顶部批量操作（batch-delete / add-button）。
3. `<rowActions x:override="bounded-merge">` — 仅保留 `row-view-button`（白名单语义），update/delete/more 全部丢弃。
4. `<action id="row-view-button">` 显式声明 `<dialog page="view"/>` — dialog 模式（非 drawer），与 codegen 默认一致。

### 3.4 可编辑实体只扩展 asideFilter/query（保留 edit/add form）

可编辑实体（如 ErpPurOrder/ErpSalOrder/ErpFinVoucher/ErpFinArApItem）不切断 edit/add 继承，
仅扩展双筛选面字段集。模板差异：

```xml
<form id="edit"/>  <!-- 不抽象，保留 _gen 层或独立 form 覆盖 -->
<form id="add"/>
<form id="asideFilter" editMode="query" submitOnChange="true">
    <!-- 业务多维筛选字段 -->
</form>
<form id="query" editMode="query" title="查询条件">
    <!-- 既有 query form（F3 P0 已落地）-->
</form>
```

## 4. 反模式自检表

| 反模式 | 正确做法 |
|--------|---------|
| 直接改 `_gen/_*.view.xml` 的 asideFilter/query form | 改保留层 `*.view.xml`，用 `<form id="asideFilter">` 或 `<form id="asideFilter" x:override="bounded-merge">` 合并 |
| 只读实体保留 `<form id="edit"/>` 不抽象 | `<form id="edit" x:abstract="true"/>` 显式切断 _gen 继承 |
| `<form id="asideFilter">` 字段过多（>8） | 拆为常用 + 高级筛选，或移到独立 page |
| 同一字段同时出现在 asideFilter 和 query | 择一，按 §1.1 规则分配 |
| asideFilter 字段未配 `filterOp` | 每个非默认字段（如 date/like）显式配 filterOp |
| 只读实体 `<rowActions>` 残留 update/delete | `<rowActions x:override="bounded-merge">` 白名单仅保留 row-view-button |
| 引入 AMIS `<drawer>` 替代 `<dialog>` | 复用 codegen 默认 `<dialog page="view"/>`，无业务需求不引入 drawer |
| 可编辑实体切断 edit/add form 继承 | 可编辑实体保留 edit/add，仅扩展 asideFilter/query |
| 只读实体添加 listActions 按钮 | `<listActions x:override="bounded-merge"/>` 空内容丢弃全部 |
| `asideFilter` 缺 `submitOnChange="true"` | 业务专用筛选必须即时反馈，保留 codegen 默认 submitOnChange |

## 5. 实施证据

| 实体 | 文件 | asideFilter 字段数 | query 字段数 | edit/add abstract |
|------|------|---------|--------|---------|
| ErpInvStockLedger | `module-inventory/erp-inv-web/.../ErpInvStockLedger.view.xml` | 5 | 1 | ✅ |
| ErpInvStockBalance | `module-inventory/erp-inv-web/.../ErpInvStockBalance.view.xml` | 6 | 4 | ✅ |
| ErpInvBatch | `module-inventory/erp-inv-web/.../ErpInvBatch.view.xml` | 4 | 1 | ✅ |
| ErpInvSerialNumber | `module-inventory/erp-inv-web/.../ErpInvSerialNumber.view.xml` | 3 | 1 | ✅ |
| ErpFinGlBalance | `module-finance/erp-fin-web/.../ErpFinGlBalance.view.xml` | 4 | — | ✅ |
| ErpFinTrialBalance | `module-finance/erp-fin-web/.../ErpFinTrialBalance.view.xml` | 4 | — | ✅ |
| ErpFinVoucher | `module-finance/erp-fin-web/.../ErpFinVoucher.view.xml` | — | 4（既有扩展）| — （可编辑）|
| ErpFinArApItem | `module-finance/erp-fin-web/.../ErpFinArApItem.view.xml` | 5 | — | — （可编辑）|
| ErpPurOrder | `module-purchase/erp-pur-web/.../ErpPurOrder.view.xml` | 5 | 5（既有扩展）| — （可编辑）|
| ErpSalOrder | `module-sales/erp-sal-web/.../ErpSalOrder.view.xml` | 5 | 5（既有扩展）| — （可编辑）|

## 6. 范围外（Deferred）

- **长尾只读实体**（aps/logistics/maintenance 等域流水/日志）：本 plan 核心 6 实体已覆盖最高频场景，长尾实体按需逐域补齐。触发条件：对应域只读视图业务需求落地。
- **行级方向色显示**（库存流水 +/- 颜色区分、借贷方向色）：F6 Deferred「负数红字显示」successor 范畴。本 plan 仅做字段格式（千分位 + 精度），不引入方向色机制。
- **StockBalance 行点击展开流水明细**（关联流水 tabs）：F9 跨单据导航 + F12 详情页结构范畴。本 plan dialog 仅显示当前实体字段。
- **GL → Sub-ledger → Voucher 3 级 drill-down**：F12 详情页结构范畴。本 plan GlBalance dialog 仅显示当前实体字段。
- **StockLedger source 列点击跳转源单据**：F9 跨单据导航范畴。本 plan source 列仅显示文本，不可点击。
- **含零库存勾选框的 visibleOn 控制**（按钮显隐联动）：F7 cross-cutting visibleOn 范畴。本 plan 勾选框作为 query 字段，前端不引入 visibleOn 联动。
