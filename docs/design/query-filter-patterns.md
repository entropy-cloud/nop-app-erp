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

## 7. ext 8 域 22 列表页双筛选面字段集冻结表（plan 2026-07-21-0330-2 §5）

> 决策来自 plan `2026-07-21-0330-2-f8-f2-ext-domains-list-page-enhancement.md` Phase 1。
> 22 列表页 = crm 3 + cs 2 + hr 4 + aps 3 + logistics 3 + b2b 3 + contract 2 + drp 2。
> 字段集按各域 ui-patterns.md + 实时 ORM 字段核实冻结。plan 原稿中的部分字段（如 ErpB2bEdiDoc.direction / ErpDrpParameter.parameterType / ErpHrAttendance.shiftId）在实际 ORM 中不存在，按实际可用字段替代。

### 7.1 crm: ErpCrmLead（可编辑，含 leadType 切换）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | leadType | eq | 线索/商机类型（Phase 0 (a) 降级方案 A：asideFilter 字段实现，非独立 Tab）|
| asideFilter | sourceId | eq | 线索来源 |
| asideFilter | stageId | eq | 漏斗阶段 |
| asideFilter | ownerId | eq | 负责人（Phase 0 (b) Decision C：My/Team/All 快捷按钮归 Deferred）|
| asideFilter | partnerId | eq | 客户 |
| asideFilter | createdAt | date-between | 创建时间（ORM 实际字段名为 `createTime`，plan 原文 `createdAt` 为笔误）|
| query | code | like | 编码 |
| query | leadStatusId | eq | 线索状态 |

### 7.2 crm: ErpCrmCampaign（可编辑）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | orgId | eq | 业务组织 |
| asideFilter | medium | eq | 渠道 |
| asideFilter | source | eq | 来源 |
| asideFilter | startDate | date-between | 开始日期 |
| query | code | like | 编码 |
| query | campaignName | like | 主题 |

### 7.3 crm: ErpCrmForecast（可编辑）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | orgId | eq | 业务组织 |
| asideFilter | periodId | eq | 预测周期 |
| asideFilter | territoryId | eq | 销售区域 |
| asideFilter | teamId | eq | 销售团队 |
| asideFilter | ownerId | eq | 销售员 |
| query | currencyId | eq | 币种 |
| query | lastCalculatedAt | date-between | 最近计算时间 |

### 7.4 cs: ErpCsTicket（可编辑）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | ticketTypeId | eq | 工单类型 |
| asideFilter | slaPolicyId | eq | SLA 策略 |
| asideFilter | status | in | 工单状态（多选 IN 过滤）|
| asideFilter | priority | eq | 优先级 |
| asideFilter | assignedToId | eq | 分配处理人 |
| asideFilter | createTime | date-between | 创建时间 |
| query | code | like | 单号 |
| query | customerId | eq | 客户 |

### 7.5 cs: ErpCsSurvey（可编辑）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | orgId | eq | 业务组织 |
| asideFilter | ticketId | eq | 关联工单 |
| asideFilter | surveyChannel | eq | 发送渠道 |
| asideFilter | surveySentAt | date-between | 调查发送时间 |
| asideFilter | respondedAt | date-between | 响应时间 |
| query | surveyToken | like | 调查令牌 |

### 7.6 hr: ErpHrEmployee（可编辑）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | departmentId | eq | 部门 |
| asideFilter | positionId | eq | 职位 |
| asideFilter | employmentStatus | eq | 雇佣状态 |
| asideFilter | employeeType | eq | 员工类型 |
| asideFilter | orgId | eq | 业务组织 |
| asideFilter | hireDate | date-between | 入职日期 |
| query | code | like | 工号 |
| query | fullName | like | 全名 |

### 7.7 hr: ErpHrAttendance（可编辑）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | employeeId | eq | 员工 |
| asideFilter | orgId | eq | 业务组织 |
| asideFilter | source | eq | 来源 |
| asideFilter | isAbsent | eq | 是否旷工 |
| asideFilter | date | date-between | 考勤日期 |
| query | employeeId | eq | 员工 |
| query | date | date-between | 考勤日期 |

### 7.8 hr: ErpHrLeaveRequest（可编辑）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | employeeId | eq | 员工 |
| asideFilter | leaveType | eq | 休假类型 |
| asideFilter | status | in | 状态（多选）|
| asideFilter | approverId | eq | 审批人 |
| asideFilter | orgId | eq | 业务组织 |
| asideFilter | startDate | date-between | 开始日期 |
| query | code | like | 单号 |
| query | businessDate | date-between | 业务日期 |

### 7.9 hr: ErpHrPayrollBankFile（可编辑，codegen 默认无 query）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | status | in | 状态（多选）|
| asideFilter | bankId | eq | 开户银行 |
| asideFilter | fileFormat | eq | 文件格式 |
| asideFilter | orgId | eq | 业务组织 |
| asideFilter | paymentDate | date-between | 发放日期 |
| query | batchNo | like | 批次号 |

### 7.10 aps: ErpApsSchedule（可编辑）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | status | in | 状态（多选）|
| asideFilter | schedulingMode | eq | 排产模式 |
| asideFilter | orgId | eq | 业务组织 |
| asideFilter | scheduleDate | date-between | 排产日期 |
| query | code | like | 编号 |
| query | name | like | 方案名称 |

### 7.11 aps: ErpApsOperationOrder（可编辑）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | workOrderId | eq | 主工单 |
| asideFilter | machineId | eq | 工作中心/设备 |
| asideFilter | status | in | 状态（多选）|
| asideFilter | assignedToId | eq | 操作工 |
| asideFilter | orgId | eq | 业务组织 |
| asideFilter | plannedStartDateT | date-between | 计划开工时间 |
| query | code | like | 编号 |
| query | operationName | like | 工序名称 |

### 7.12 aps: ErpApsDispatchLog（**只读**）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | operationOrderId | eq | 工序工单 |
| asideFilter | workcenterId | eq | 工作中心 |
| asideFilter | dispatchType | eq | 派工类型 |
| asideFilter | newStatus | eq | 派工后状态 |
| asideFilter | dispatchedBy | eq | 派工人 |
| asideFilter | dispatchedAt | date-between | 派工时间 |
| query | orgId | eq | 业务组织 |

### 7.13 logistics: ErpLogShipment（可编辑，含异常筛选）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | carrierId | eq | 承运商 |
| asideFilter | relatedBillType | eq | 关联单据类型（plan 原指定 partnerId 在 erp_log_shipment 表不存在，按实际字段集冻结为 relatedBillType）|
| asideFilter | status | in | 状态（Phase 0 (c) 降级方案 A：多选 IN 实现异常筛选）|
| asideFilter | shipperId | eq | 发货员 |
| asideFilter | orgId | eq | 业务组织 |
| asideFilter | shipmentDate | date-between | 发运日期 |
| query | code | like | 单号 |
| query | trackingNo | like | 运单号 |

### 7.14 logistics: ErpLogShipmentLog（**只读**）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | shipmentId | eq | 发运单 |
| asideFilter | gatewayId | eq | 网关标识 |
| asideFilter | actionType | eq | 操作类型 |
| asideFilter | isSuccess | eq | 是否成功 |
| asideFilter | orgId | eq | 业务组织 |
| asideFilter | executedAt | date-between | 执行时间 |
| query | errorCode | like | 错误码 |

### 7.15 logistics: ErpLogCarrier（可编辑）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | carrierType | eq | 承运商类型 |
| asideFilter | gatewayId | eq | 网关标识 |
| asideFilter | partnerId | eq | 承运商往来单位 |
| asideFilter | isActive | eq | 是否启用 |
| asideFilter | orgId | eq | 业务组织 |
| query | code | like | 编码 |
| query | carrierName | like | 承运商名称 |

### 7.16 b2b: ErpB2bEdiDoc（可编辑）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | formatId | eq | EDI 格式 |
| asideFilter | state | in | 状态（多选）|
| asideFilter | blockingLevel | eq | 阻断级别 |
| asideFilter | relatedBillType | eq | 关联单据类型 |
| asideFilter | orgId | eq | 业务组织 |
| asideFilter | sentAt | date-between | 发送时间 |
| query | code | like | 事务编码 |
| query | relatedBillCode | like | 关联单据号 |

### 7.17 b2b: ErpB2bAsn（可编辑）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | partnerId | eq | 发货方（供应商）|
| asideFilter | status | in | 状态（多选）|
| asideFilter | sourceEdiDocId | eq | 来源 EDI 文档 |
| asideFilter | relatedBillType | eq | 关联采购订单类型 |
| asideFilter | orgId | eq | 业务组织 |
| asideFilter | estimatedArrivalDate | date-between | 预计到货日期 |
| query | code | like | ASN 编码 |
| query | trackingNo | like | 物流单号 |

### 7.18 b2b: ErpB2bEdiLog（**只读**）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | ediDocId | eq | EDI 事务 |
| asideFilter | direction | eq | 方向 |
| asideFilter | resultCode | like | 结果码 |
| asideFilter | orgId | eq | 业务组织 |
| asideFilter | logTime | date-between | 日志时间 |
| query | resultMsg | like | 结果消息 |

### 7.19 contract: ErpCtContract（可编辑）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | partnerId | eq | 合作方 |
| asideFilter | status | in | 合同状态（多选）|
| asideFilter | contractType | eq | 合同类型 |
| asideFilter | contractDirection | eq | 合同方向 |
| asideFilter | orgId | eq | 业务组织 |
| asideFilter | startDate | date-between | 生效日期 |
| query | code | like | 合同编号 |
| query | contractName | like | 合同名称 |

### 7.20 contract: ErpCtRebateAgreement（可编辑）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | partnerId | eq | 对方伙伴 |
| asideFilter | contractId | eq | 关联合同 |
| asideFilter | status | in | 状态（多选）|
| asideFilter | rebateType | eq | 返利类型 |
| asideFilter | orgId | eq | 业务组织 |
| asideFilter | startDate | date-between | 有效期开始 |
| query | code | like | 协议编号 |

### 7.21 drp: ErpDrpPlan（可编辑）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | status | in | 状态（多选）|
| asideFilter | orgId | eq | 业务组织 |
| asideFilter | periodFrom | date-between | 期间开始 |
| query | code | like | 编号 |
| query | planName | like | 计划名称 |

### 7.22 drp: ErpDrpParameter（可编辑，codegen 默认无 query）

| 面 | 字段 | filterOp | 说明 |
|----|------|---------|------|
| asideFilter | warehouseId | eq | 仓库 |
| asideFilter | materialId | eq | 物料 |
| asideFilter | replenishmentMethod | eq | 补货方法（plan 原指定 parameterType 不存在）|
| asideFilter | preferredSupplierId | eq | 首选供应商 |
| asideFilter | orgId | eq | 业务组织 |
| query | preferredSourceWarehouseId | eq | 首选调出仓库 |

## 8. 特殊筛选模式（plan 2026-07-21-0330-2 §6）

### 8.1 leadType Tab/Chip 切换（Phase 0 (a) Decision A）

- **场景**：crm Lead 列表需支持「全部 / 线索 / 商机」3 态切换
- **裁决**：asideFilter 字段 + submitOnChange（**landed**），非独立 AMIS `<tabs>` 组件
- **理由**：codegen 默认双筛选面架构（`<crud asideFilterForm="asideFilter" filterForm="query">`）不引入独立 Tab 容器；AMIS Tab 模式会偏离 codegen 默认 + 与 bounded-merge 不兼容；`leadType` 字段在 ORM 存在（dict `erp-crm/lead-type`），后端 `__findPage` filter map 默认支持
- **模式**：`<cell id="leadType" filterOp="eq"/>` 渲染为 AMIS select（dict 自动），用户选择后 `submitOnChange="true"` 自动触发 `__findPage` 刷新

### 8.2 「我的/团队/全部」快捷过滤（Phase 0 (b) Decision C — deferred）

- **场景**：crm Lead + cs Ticket 按「我的/团队/全部」3 态快捷过滤
- **裁决**：**deferred-with-trigger**（前端无法可靠获取当前用户 ID）
- **降级实现**：仅落地常规 `ownerId` 字段（asideFilter），用户手动选择负责人筛选
- **successor 触发**：F11 批量操作 plan 或后端 user-context 增强 plan 启动时
- **后端 gap**：需新增 `@BizQuery findMyLeads` 或在 `__findPage` 注入当前用户上下文（触犯 Non-Goals 保护区域）

### 8.3 异常筛选（Phase 0 (c) Decision A — landed）

- **场景**：logistics Shipment 异常筛选「仅显示网关异常/追踪超期/退回」
- **裁决**：**landed**（多选 status + `filterOp=in` 实现），「追踪超期」按钮 deferred
- **模式**：`<cell id="status" filterOp="in"/>` 渲染为 AMIS multi-select tag，后端 QueryBean filter map 自动生成 `status: { in: [v1, v2, ...] }` 结构
- **successor 触发**：异常发运单数量 > 1000 时后端专用 `findExceptionList @BizQuery` plan 启动（追踪超期需后端派生字段计算 dueDate < now）

### 8.4 多选 IN 过滤模式

适用场景：状态字段需要 OR 多值匹配（如「待审核 + 已提交」）。

```xml
<cell id="status" filterOp="in"/>
```

后端转译：`status IN (?, ?, ?)`。AMIS dict 字段自动渲染为 tag-select 多选控件。

### 8.5 布尔 includeX 占位字段（继承核心域）

适用场景：「含零库存」「含未过账」等 UI 占位字段，后端处理（前端 custom field + boolean）。

```xml
<cell id="__includeZero" custom="true" domain="boolean"/>
```

ext 8 域本 plan 未引入新的 includeX 字段（已通过 status/date 范围覆盖业务需求）。

## 9. ext 8 域只读实体清单（plan 2026-07-21-0330-2 §7）

共 14 实体（Phase 0 Explore 裁决：3 个原 pending Explore 半只读实体均按「父表 sub-grid 编辑，独立页只读」语义归类为 confirmed readonly）：

| 实体 | 域 | 角色 | 来源 | 独立页只读 |
|------|----|------|------|-----------|
| ErpApsDispatchLog | aps | 调度引擎写入日志 | 派工引擎 | ✅ |
| ErpLogShipmentLog | logistics | 网关回调日志 | 网关 | ✅ |
| ErpLogShipmentParcel | logistics | 部分场景由网关回传 | 网关 + 父表 sub-grid | ✅（独立页只读，父表 ErpLogShipment 内 sub-grid-edit 保留）|
| ErpB2bEdiLog | b2b | EDI 网关写入 | 网关 | ✅ |
| ErpCrmLeadConvLog | crm | 线索转换引擎写入 | 转换引擎 | ✅ |
| ErpCrmLeadScore | crm | 评分引擎写入 | 评分引擎 | ✅ |
| ErpCrmLeadScoreLine | crm | 评分明细（评分头派生）| 评分引擎 | ✅ |
| ErpCrmForecastAccuracy | crm | 准确度计算引擎写入 | 计算引擎 | ✅ |
| ErpCrmFunnelStageMetrics | crm | 漏斗聚合引擎写入 | 聚合引擎 | ✅ |
| ErpCrmLeadSequenceProgress | crm | 序列推进引擎写入 | 推进引擎 | ✅ |
| ErpHrLeaveBalance | hr | 年初结转 + 请假审批引擎写入 | 结转 + 审批引擎 | ✅ |
| ErpHrSurveyResult | hr | 调查结果聚合 | 聚合引擎 | ✅ |
| ErpCtApprovalRecord | contract | 审批流程写入 | 审批流程 | ✅ |
| ErpCtConsumptionLine | contract | 上游单据回写 | 上游回写 + 父表 sub-grid | ✅（独立页只读，父表 ErpCtContractLine 内 sub-grid-edit 保留）|

**只读切断契约**（14/14 confirmed readonly）：
1. `<form id="edit" x:abstract="true"/>` + `<form id="add" x:abstract="true"/>` 显式切断 _gen 继承
2. `<listActions x:override="bounded-merge"/>` 空内容丢弃 batch-delete-button + add-button
3. `<rowActions x:override="bounded-merge">` 白名单仅 `<action id="row-view-button"><dialog page="view"/></action>`
4. asideFilter + query 双筛选面字段集（同核心域范式 §3.3）

## 10. 实施证据（ext 8 域）

| 实体 | 文件 | asideFilter 字段数 | query 字段数 | edit/add abstract |
|------|------|---------|--------|---------|
| ErpCrmLead | `module-crm/erp-crm-web/.../ErpCrmLead.view.xml` | 6 | 2 | — （可编辑）|
| ErpCrmCampaign | `module-crm/erp-crm-web/.../ErpCrmCampaign.view.xml` | 4 | 2 | — |
| ErpCrmForecast | `module-crm/erp-crm-web/.../ErpCrmForecast.view.xml` | 5 | 2 | — |
| ErpCsTicket | `module-cs/erp-cs-web/.../ErpCsTicket.view.xml` | 6 | 2 | — |
| ErpCsSurvey | `module-cs/erp-cs-web/.../ErpCsSurvey.view.xml` | 5 | 1 | — |
| ErpHrEmployee | `module-hr/erp-hr-web/.../ErpHrEmployee.view.xml` | 6 | 2 | — |
| ErpHrAttendance | `module-hr/erp-hr-web/.../ErpHrAttendance.view.xml` | 5 | 2 | — |
| ErpHrLeaveRequest | `module-hr/erp-hr-web/.../ErpHrLeaveRequest.view.xml` | 6 | 2 | — |
| ErpHrPayrollBankFile | `module-hr/erp-hr-web/.../ErpHrPayrollBankFile.view.xml` | 5 | 1 | — |
| ErpApsSchedule | `module-aps/erp-aps-web/.../ErpApsSchedule.view.xml` | 4 | 2 | — |
| ErpApsOperationOrder | `module-aps/erp-aps-web/.../ErpApsOperationOrder.view.xml` | 6 | 2 | — |
| ErpApsDispatchLog | `module-aps/erp-aps-web/.../ErpApsDispatchLog.view.xml` | 6 | 1 | ✅ |
| ErpLogShipment | `module-logistics/erp-log-web/.../ErpLogShipment.view.xml` | 6 | 2 | — |
| ErpLogShipmentLog | `module-logistics/erp-log-web/.../ErpLogShipmentLog.view.xml` | 6 | 1 | ✅ |
| ErpLogCarrier | `module-logistics/erp-log-web/.../ErpLogCarrier.view.xml` | 5 | 2 | — |
| ErpB2bEdiDoc | `module-b2b/erp-b2b-web/.../ErpB2bEdiDoc.view.xml` | 6 | 2 | — |
| ErpB2bAsn | `module-b2b/erp-b2b-web/.../ErpB2bAsn.view.xml` | 6 | 2 | — |
| ErpB2bEdiLog | `module-b2b/erp-b2b-web/.../ErpB2bEdiLog.view.xml` | 5 | 1 | ✅ |
| ErpCtContract | `module-contract/erp-ct-web/.../ErpCtContract.view.xml` | 6 | 2 | — |
| ErpCtRebateAgreement | `module-contract/erp-ct-web/.../ErpCtRebateAgreement.view.xml` | 6 | 1 | — |
| ErpDrpPlan | `module-drp/erp-drp-web/.../ErpDrpPlan.view.xml` | 3 | 2 | — |
| ErpDrpParameter | `module-drp/erp-drp-web/.../ErpDrpParameter.view.xml` | 5 | 1 | — |

只读实体（14 实体含 readonly cut-off，含 11 独立只读 + 3 父表 sub-grid 编辑）：

| 实体 | 文件 | asideFilter 字段数 | edit/add abstract |
|------|------|---------|---------|
| ErpApsDispatchLog | （见上）| 6 | ✅ |
| ErpLogShipmentLog | （见上）| 6 | ✅ |
| ErpLogShipmentParcel | `module-logistics/erp-log-web/.../ErpLogShipmentParcel.view.xml` | 2 | ✅ |
| ErpB2bEdiLog | （见上）| 5 | ✅ |
| ErpCrmLeadConvLog | `module-crm/erp-crm-web/.../ErpCrmLeadConvLog.view.xml` | 6 | ✅ |
| ErpCrmLeadScore | `module-crm/erp-crm-web/.../ErpCrmLeadScore.view.xml` | 6 | ✅ |
| ErpCrmLeadScoreLine | `module-crm/erp-crm-web/.../ErpCrmLeadScoreLine.view.xml` | 4 | ✅ |
| ErpCrmForecastAccuracy | `module-crm/erp-crm-web/.../ErpCrmForecastAccuracy.view.xml` | 6 | ✅ |
| ErpCrmFunnelStageMetrics | `module-crm/erp-crm-web/.../ErpCrmFunnelStageMetrics.view.xml` | 5 | ✅ |
| ErpCrmLeadSequenceProgress | `module-crm/erp-crm-web/.../ErpCrmLeadSequenceProgress.view.xml` | 6 | ✅ |
| ErpHrLeaveBalance | `module-hr/erp-hr-web/.../ErpHrLeaveBalance.view.xml` | 4 | ✅ |
| ErpHrSurveyResult | `module-hr/erp-hr-web/.../ErpHrSurveyResult.view.xml` | 3 | ✅ |
| ErpCtApprovalRecord | `module-contract/erp-ct-web/.../ErpCtApprovalRecord.view.xml` | 6 | ✅ |
| ErpCtConsumptionLine | `module-contract/erp-ct-web/.../ErpCtConsumptionLine.view.xml` | 3 | ✅ |

## 11. ext 域 deferred 项（plan 2026-07-21-0330-2）

- **My/Team/All 快捷过滤**（crm Lead + cs Ticket）：后端 user-context 缺失，归 Deferred。触发：F11 批量操作 plan 或后端 user-context 增强 plan 启动。
- **追踪超期异常筛选**（logistics Shipment）：需后端派生字段计算。归 Deferred。触发：异常发运单数量 > 1000 时后端专用 `findExceptionList @BizQuery` plan 启动。
- **核心域已有的列表页优化**（purchase/sales/inventory/finance 4 主域）：F8 核心 8 列表页已由 plan `2026-07-20-0629-2` 落地，本节仅扩展 ext 8 域。
