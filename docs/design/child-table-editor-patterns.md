# 子表行内编辑范式（Child Table Editor Patterns）

> Owner docs: `docs/backlog/frontend-ui-roadmap.md` §F4 Phase 2、`docs/design/picker-patterns.md`（F4 Phase 1 picker 接线）、`docs/architecture/view-and-page-strategy.md`（view.xml 嵌套层次）
> 落地计划：`docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md`（P0 8 对头行子表编辑）

## 1. 目的与范围

固化「ERP 头行单据子表行内编辑」的标准范式，供后续域（P1 inventory/finance、P2 mfg/assets/projects、ext 8 域）按图施工。

**适用范围**：1:N 头行实体，行级数据随头一起保存（聚合根 `__save`）。

**不适用**：
- 多子表 tab 容器（→ F12）
- 跨单据行导入（→ F9）
- 批次/序列号选择器嵌入行（→ F10 + 业务专用 picker）
- 树形子表（→ F10）

## 2. 后端前置条件（已就绪，本范式不改后端）

`__save` mutation 接受嵌套 `lines:[...]`，由 codegen 自动展开 one-to-many 关系为聚合根级 save。本范式仅在 view.xml 层定制，不动 ORM/BizModel。

权威证据（抽样）：
- `_ErpPurOrder.xmeta:222` — `<prop name="lines" ext:kind="to-many" insertable="true" updatable="true" tagSet="pub,cascade-delete,insertable,updatable"/>`
- `ErpPurOrderInputBean.java:366` — `List<ErpPurOrderLineInputBean> _lines`
- `_gen/_ErpPurOrder.java:2088` — `OrmEntitySet<ErpPurOrderLine> _lines`

## 3. view.xml 嵌套子表的代码生成管线（Explore 验证结论）

**机制（方案 A：view.xml `<view path=... grid=.../>`）**：

1. 头实体 `<form id="view">` 与 `<form id="edit">` 的 `<layout>` 末尾追加 `lines[明细行](2)`（占整行 width=2）
2. 同 form 的 `<cells>` 内追加 `<cell id="lines"><view path="/erp/{short}/pages/{LineEntity}/{LineEntity}.view.xml" grid="sub-grid-edit"/></cell>`
3. 行实体的 `XxxLine.view.xml` 内**手写**新增 `<grid id="sub-grid-edit" x:prototype="list" editMode="list-edit">`，列集采用 `bounded-merge` 仅保留子表所需列
4. 同样手写 `<grid id="sub-grid-view" x:prototype="list" editMode="list-view">` 供头 form `view` 模式使用（只读）

**codegen 展开链**（nop-entropy）：
- `web.xlib:GenDispView` → 检测 `refView.grid` → `GenInputTable`（编辑态）或 `GenTable`（查看态）
- `GenInputTable` 输出 AMIS `input-table`（含 `addable/removable/editable/needConfirm=false/showIndex=true`）+ `columns[]`
- `columns[]` 由 `GenGridCols` → `GenGridCol` → 每个 `<col>` 经 `DefaultControl`（依据 propMeta.domain 与 ext:relation 自动生成 picker 等控件）

**裁决依据**：
- `_dump/nop-app/nop/web/xlib/web.xlib:535-555`（GenInputTable）+ `475-534`（GenDispView 分发）
- `nop-entropy/nop-wf/nop-wf-web/.../NopWfDefinitionAuth/_gen/_NopWfDefinitionAuth.view.xml:59-72`（codegen 自动生成 sub-grid-edit/sub-grid-view 模式，但仅当实体被反向引用为子表时生成；ERP Line 实体需手写新增）
- `nop-entropy/nop-rule/nop-rule-web/.../NopRuleDefinition.view.xml:148-150`（消费方 cell 嵌套 `<view path=... grid="sub-grid-view"/>`）

**方案 B（被否决）**：直接 delta `picker.page.yaml`/`*.page.yaml` 手写 AMIS `input-table` JSON — 绕过 view.xml 抽象，定制散落难维护。

## 4. 行内 picker 接线

**机制**：列定义中的 FK 字段（`materialId`、`warehouseId`、`taxRateId` 等）由 codegen 自动生成 AMIS `picker` 控件，picker URL 由 `XuiHelper.getRelationPickerUrl` 从 propMeta 的 `ext:relation` + 关系 bizObjName + moduleId 推导。

示例：`ErpPurOrderLine` 的 `materialId` 列 → 关系 `material` → bizObjName=`ErpMdMaterial` + moduleId=`erp/md` → picker URL `/erp/md/pages/ErpMdMaterial/picker.page.yaml`。

**已落地 picker（F4 Phase 1）**：物料/币种/科目/合作伙伴/员工/资产/仓库（仓库 pick-list 由本 Phase 2 补齐）。所有 FK 字段直接复用，**无需在本范式再接线**。

**调用方局部 filter（机制 C）**：本 Phase 2 不实现，留到业务专用 picker 后续 plan。

## 5. 行内自动推算（amount / amountWithTax）

**机制（方案 A：cell onEvent + setValue）**：行级数量/单价/税率列的 `<col>` 内通过 `<gen-control>` 注入 AMIS `input-number` + `onEvent.change` → `setValue` action，在行数据作用域内更新 `amount` / `amountWithTax` / `taxAmount`。

公式：
```
amount = quantity * unitPrice                       // 不含税金额
taxAmount = amount * taxRate / 100                 // 税额
amountWithTax = amount + taxAmount                 // 含税金额
```

精度：scale=4 HALF_UP（对齐 xmeta `<schema domain="amount" precision="20" scale="4"/>`）。前端 AMIS 公式按 JS number 计算，提交时后端 BigDecimal 接管做最终定精度。

**示例 col 定义**（ErpPurOrderLine `sub-grid-edit` 内 `quantity` 列）：

```xml
<col id="quantity" mandatory="true">
    <gen-control>
        <input-number>
            <onEvent>
                <change>
                    <actions j:list="true">
                        <action actionType="setValue">
                            <args>
                                <value>
                                    <amount>${ROUND(quantity * unitPrice, 4)}</amount>
                                    <taxAmount>${ROUND(quantity * unitPrice * taxRate / 100, 4)}</taxAmount>
                                    <amountWithTax>${ROUND(quantity * unitPrice * (1 + taxRate / 100), 4)}</amountWithTax>
                                </value>
                            </args>
                        </action>
                    </actions>
                </change>
            </onEvent>
        </input-number>
    </gen-control>
</col>
```

`unitPrice`、`taxRate` 列同型 onEvent。`amount` / `amountWithTax` / `taxAmount` 列设 `static`（编辑态只读，由推算填充）。

**实现备注**：
- AMIS `setValue` 不指定 `componentName` 时默认作用于当前数据作用域（input-table 行级 scope）
- `${ROUND(x, 4)}` 为 AMIS 公式（companion 表达式），若 AMIS 版本不支持 `ROUND`，降级为不四舍五入（提交后端 BigDecimal 兜底）
- onEvent 触发后行内 `static` 控件即时刷新（AMIS reactive）

## 6. 头聚合（totalAmount / totalTaxAmount / totalAmountWithTax）

**机制（方案 A：cell onEvent 监听 lines 数组）**：头表单 `lines` cell 的 `<view path=...>` 同层添加 `<onEvent>` 监听 input-table 的 `change` 事件 → 计算 `lines` 数组的 sum → 写回头字段。

由于 view.xml 的 `<view path=...>` 子元素直接产出 input-table AMIS 节点，onEvent 直接挂到该 input-table 上即可：

```xml
<cell id="lines">
    <view path="/erp/pur/pages/ErpPurOrderLine/ErpPurOrderLine.view.xml" grid="sub-grid-edit"/>
</cell>
```

input-table 自身已含 `needConfirm=false`，每次行内编辑都触发 `change` 事件冒泡到父 form 作用域。父 form 上的 `totalAmount` / `totalTaxAmount` / `totalAmountWithTax` cell 改为编辑态只读（`@totalAmount`），并通过监听 input-table 的 change 事件刷新：

```xml
<!-- 头表单 amount 分组改为 -->
=========>amount[金额信息]======
@amountSource[合计金额(源币不含税)] @amountFunctional[合计金额(本位币不含税)]
@totalAmount[合计金额(本位币不含税)] @totalTaxAmount[合计税额]
@totalAmountWithTax[合计金额(含税)] @discountRate[整单折扣率(%)]
@discountAmount[折扣金额] @paidAmount[已付金额]
```

**头聚合的 setValue 由 input-table 的 onEvent.change 触发**（同样挂在 lines cell view 的 input-table 上，与行内 setValue 合并）：

```xml
<cell id="lines">
    <view path="..." grid="sub-grid-edit">
        <onEvent>
            <change>
                <actions j:list="true">
                    <action actionType="setValue">
                        <args>
                            <value>
                                <totalAmount>${SUM(lines, 'amount')}</totalAmount>
                                <totalTaxAmount>${SUM(lines, 'taxAmount')}</totalTaxAmount>
                                <totalAmountWithTax>${SUM(lines, 'amountWithTax')}</totalAmountWithTax>
                            </value>
                        </args>
                    </action>
                </actions>
            </change>
        </onEvent>
    </view>
</cell>
```

> **注**：AMIS 公式 `${SUM(arr, 'field')}` 是否原生支持须运行时核实；如不支持，改写为 `${lines|reduce:<lambda>}` 或在 AMIS adaptor 层补 helper。本范式 Phase 4 落地时验证。

**后端权威源不变**：服务端 `persistTotalAmounts` 仍在 `__save` 时按 BigDecimal scale=4 重算头聚合，前端实时聚合仅为 UX 反馈，不替代后端校验。

## 7. 行级校验（Phase 4）

**机制**：`<col>` 内 `<gen-control>` 的 AMIS input-number 加 `validations` + `validateApi`：

| 校验 | 触发 | 提示 |
|------|------|------|
| 数量 > 0 | `validations: { minimum: 0.0001 }` | 「数量必须大于 0」 |
| 单价 ≥ 0 | `validations: { minimum: 0 }` | 「单价不能为负」 |
| 金额 = 数量 × 单价 | `validateApi`（前端公式校验） | 「金额与数量×单价不一致」 |

行级校验仅显示警告，不阻塞其他合法行保存（AMIS input-table 默认行为：非法行使表单整体 invalid，但保存按钮可强制提交，由后端二次校验）。

## 8. P0 8 对头行子表列集表

每对头行的 `sub-grid-edit` grid 列集（9-11 列），按域字段名差异。**字段名以 `<domain>/model/_*.xmeta` 权威源为准**（已抽样核实）：

### 8.1 purchase 域

| 头实体 | 行实体 | 列集（顺序） |
|--------|--------|-------------|
| `ErpPurOrder` | `ErpPurOrderLine` | `lineNo` `materialId` `uoMId` `quantity` `unitPrice` `amount` `taxRate` `taxAmount` `amountWithTax` `warehouseId` `remark` |
| `ErpPurReceive` | `ErpPurReceiveLine` | `lineNo` `materialId` `uoMId` `quantity` `unitPrice` `amount` `taxRate` `taxAmount` `warehouseId` `batchNo` `remark` |
| `ErpPurInvoice` | `ErpPurInvoiceLine` | `lineNo` `materialId` `uoMId` `quantity` `unitPrice` `amount` `taxRate` `taxAmount` `remark` |
| `ErpPurReturn` | `ErpPurReturnLine` | `lineNo` `materialId` `uoMId` `quantity` `unitPrice` `amount` `taxRate` `taxAmount` `reason` `remark` |

### 8.2 sales 域

| 头实体 | 行实体 | 列集（顺序） |
|--------|--------|-------------|
| `ErpSalOrder` | `ErpSalOrderLine` | `lineNo` `materialId` `uoMId` `quantity` `unitPrice` `amount` `taxRate` `taxAmount` `amountWithTax` `warehouseId` `remark` |
| `ErpSalDelivery` | `ErpSalDeliveryLine` | `lineNo` `materialId` `uoMId` `quantity` `unitPrice` `amount` `taxRate` `taxAmount` `warehouseId` `batchNo` `remark` |
| `ErpSalInvoice` | `ErpSalInvoiceLine` | `lineNo` `materialId` `uoMId` `quantity` `unitPrice` `amount` `taxRate` `taxAmount` `remark` |
| `ErpSalReturn` | `ErpSalReturnLine` | `lineNo` `materialId` `uoMId` `quantity` `unitPrice` `amount` `taxRate` `taxAmount` `reason` `remark` |

**字段名核实证据**（抽样）：
- `ErpPurOrderLine._xmeta`: `amountWithTax` 存在（propId=13）
- `ErpPurReceiveLine._xmeta`: 无 `amountWithTax`；含 `batchNo`（propId=15）+ `warehouseId`（propId=14）
- `ErpPurInvoiceLine._xmeta`: 无 `amountWithTax` + 无 `warehouseId` + 无 `batchNo`
- `ErpPurReturnLine._xmeta`: 无 `amountWithTax` + 无 `warehouseId`；含 `reason`（propId=13，字段名非 returnReason）
- `ErpSalOrderLine._xmeta`: 同 ErpPurOrderLine 结构（`amountWithTax` 存在 propId=13）
- `ErpSalDeliveryLine._xmeta`: 含 `warehouseId`（propId=13）+ `batchNo`（propId=14）；无 `amountWithTax`
- `ErpSalInvoiceLine._xmeta`: 同 ErpPurInvoiceLine 结构（最小列集）
- `ErpSalReturnLine._xmeta`: 同 ErpPurReturnLine 结构（含 `reason`）

## 9. 反模式自检表

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 头表单直接 inline 大段 AMIS input-table JSON | `<cell id="lines"><view path=".../Line.view.xml" grid="sub-grid-edit"/></cell>` |
| 在头 view.xml 复制整份 Line grid 定义 | Line 自己的 view.xml 定义 `sub-grid-edit`，头只引用 |
| 行级数量列不加 onEvent 直接静态 | `quantity`/`unitPrice`/`taxRate` 列必须挂 onEvent.setValue 推算 amount |
| 头聚合字段编辑态可编辑 | `@totalAmount` 等改为只读，由 input-table onEvent 刷新 |
| 自动推算后端用 `@BizLoader` | 前端 onEvent setValue，避免后端 round-trip（Non-Goal） |
| 行实体 view.xml 忘了加 `sub-grid-view` | 头表单 `<form id="view">` 需要只读子表 grid |
| 子表 grid 列集 > 12 列 | 最少列集原则：标识 + 数量 + 单价 + 金额 + 税务 + 仓库 + 备注 |
| 在 `_gen/_*.view.xml` 加 sub-grid-edit | 在保留层 `Line.view.xml` 加（手写） |
| 头表单 form `view` 与 `edit` 模式共用一个 sub-grid | 分别引用 `sub-grid-view` 与 `sub-grid-edit` |

## 10. 落地后影响与 successor

**直接 successor**：
- F4 Phase 2 P1（inventory/finance 3 对）独立 plan
- F4 Phase 2 P2/P3（mfg/assets/projects + ext 8 域 ~36+ 对）独立 plan

**间接 successor**：
- F7（非状态 visibleOn）— 行级字段条件显示（如 Receive 行 batchNo 仅 batchTracked 物料显示）
- F9（跨单据导航）— 从订单导入行
- F12（多子表 tabs 容器）— 头表单同时含多个子表

## 11. 变更记录

| 日期 | 变更 | 来源 |
|------|------|------|
| 2026-07-19 | 初版落地（P0 8 对头行子表编辑范式 + 列集表 + onEvent 自动推算 + 头聚合机制 + 反模式自检表） | `docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md` |
| 2026-07-20 | P1 扩展（inventory 3 头行对列集表 + 退化变体 + add 表单嵌入决策 + warehouse/location picker 补齐） | `docs/plans/2026-07-20-0629-1-f4p2-child-table-editor-p1-inventory.md` |
| 2026-07-20 | P2 扩展（mfg/assets/projects 3 头行对列集表 + 减法变体 + ErpAstCip 不适用裁决 + ErpPrjProject picker 补齐） | `docs/plans/2026-07-20-1020-3-f4p2-child-table-editor-p2-mfg-assets-projects.md` |
| 2026-07-21 | finance 凭证子表变体（ErpFinVoucher↔ErpFinVoucherLine + dcDirection 行内切换 visibleOn + 科目 picker 8 字段快照 + subject 驱动 6 维度 visibleOn + 多币种自动推算 + autoBalance 按钮触发头合计 + xview schema 适配裁决） | `docs/plans/2026-07-20-2059-3-f4p2-finance-voucher-child-table-editor.md` |

## 12. P1 inventory 3 头行对列集表

inventory 域 3 对头行子表编辑（F4 Phase 2 P1）：

### 12.1 inventory 域列集

| 头实体 | 行实体 | 列集（顺序） | 自动推算 |
|--------|--------|-------------|---------|
| `ErpInvStockMove` | `ErpInvStockMoveLine` | `lineNo` `materialId` `uoMId` `quantity` `unitCost` `totalCost` `currencyId` `sourceLocationId` `destLocationId` `remark`（10 列） | `totalCost = ROUND(quantity × unitCost, 4)` |
| `ErpInvLandedCost` | `ErpInvLandedCostLine` | `lineNo` `costElement` `amount` `apPartnerId` `remark`（5 列退化变体） | 无（无可乘字段） |
| `ErpInvTransferOrder` | `ErpInvTransferOrderLine` | `lineNo` `materialId` `uoMId` `quantity` `batchNo` `remark`（6 列最小集） | 无（仅 quantity 单字段） |

**字段名核实证据**（抽样 `_ErpInvStockMoveLine.xmeta` / `_ErpInvLandedCostLine.xmeta` / `_ErpInvTransferOrderLine.xmeta`）：
- `ErpInvStockMoveLine`：含 `quantity`（propId=7）+ `unitCost`（propId=8）+ `totalCost`（propId=9）+ `currencyId`（propId=10）+ `sourceLocationId`（propId=13）+ `destLocationId`（propId=14）
- `ErpInvLandedCostLine`：含 `costElement`（propId=4，dict=`erp-inv/cost-element`）+ `amount`（propId=5）+ `apPartnerId`（propId=6）；**无 quantity / unitPrice / unitCost 字段**
- `ErpInvTransferOrderLine`：含 `quantity`（propId=7）+ `batchNo`（propId=8）；**无任何成本/金额字段**

### 12.2 退化变体（无可乘字段实体）

当行实体**无可乘字段**（即不存在 `quantity × unitPrice` 或类似乘积关系）时，sub-grid-edit **不引入 onEvent.setValue 自动推算**，仅保留 picker 接线 + 行级校验。

**裁决依据**：
- `ErpInvLandedCostLine`：`amount` 是直接录入的总金额（费用要素 + 应付往来），无乘法派生关系 → 不引入自动推算
- `ErpInvTransferOrderLine`：仅 `quantity` 一个数值字段，无对应单价/金额字段 → 不引入自动推算

**退化变体的写法差异**：
- sub-grid-edit 列集不变（lineNo + 业务字段 + remark）
- 行级校验保留（amount ≥ 0、quantity > 0 等）
- **不写 onEvent.change.actions.setValue 块**
- 头表单 lines cell 不挂头聚合 onEvent（无行级金额可累加）

### 12.3 add 表单嵌入决策（ErpInvLandedCost）

**方案 A（已采纳）**：`<form id="add">` 与 `<form id="edit">` 同构——layout 末尾追加 lines 组 + cells 内嵌 `<cell id="lines">` 子表控件。

**理由**：
- 与 P0 头实体行为一致（P0 头实体 add 表单虽为空 `<form id="add"/>`，但 ErpInvLandedCost 已含业务 add 表单）
- 用户体验流畅——新建时直接录入行，不必先建头再编辑行
- codegen `__save` 端点已支持聚合根 save 嵌套行（inventory 3 头实体沿用相同 ORM 关系模型）

**反例（不采纳方案 B）**：add 表单不嵌入 lines cell，仅 edit/view 嵌入——会导致「先建头再编辑行」的两步流程，与 P0 行为分化。

### 12.4 inventory 头表单 picker 补齐

F4 Phase 1 已落地 `ErpMdMaterial / ErpMdPartner / ErpMdCurrency / ErpMdSubject` picker 列集；本 P1 计划补齐 `ErpMdWarehouse` + `ErpMdLocation` picker（picker.page.yaml 已存在 codegen wrapper，本计划仅在 view.xml 层补 `pick-list` grid + `pick-query` filterForm）：

| Picker | pick-list 列集 | pick-query 筛选字段 |
|--------|---------------|---------------------|
| `ErpMdWarehouse` | `id` `code` `name` `warehouseType` `orgId` `status` | `code(like) | name(like) | warehouseType(eq) | status(eq)` |
| `ErpMdLocation` | `id` `warehouseId` `code` `name` `parentId` `isActive` | `code(like) | name(like) | warehouseId(eq) | isActive(eq)` |

**消费方**：
- StockMove 头表 `sourceWarehouseId` / `destWarehouseId`（→ ErpMdWarehouse）
- StockMove 头表 `sourceLocationId` / `destLocationId`（→ ErpMdLocation，头级字段）
- StockMoveLine 行表 `sourceLocationId` / `destLocationId`（→ ErpMdLocation，行级字段）
- TransferOrder 头表 `fromWarehouseId` / `toWarehouseId` / `inTransitWarehouseId`（→ ErpMdWarehouse）

### 12.5 P1 反模式补充自检

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 对无可乘字段的 LandedCostLine 强行写 onEvent.setValue | 直接录入 `amount`，仅加 `validations.minimum: 0` |
| 在 ErpMdWarehouse/ErpMdLocation 新建 picker.page.yaml | picker.page.yaml 已存在 codegen wrapper，仅在 view.xml 补 pick-list + pick-query |
| 在 inventory Line view.xml 复用 P0 的 taxRate/amountWithTax 列 | inventory Line 字段异构，按 xmeta 实际字段裁剪列集 |
| add 表单不嵌入 lines cell 强迫两步流程 | add 表单与 edit 表单同构嵌入 lines cell |

### 12.6 P1 落地证据（2026-07-20）

**实施范围**：
- 3 inventory 头实体 view.xml 改造（`ErpInvStockMove` / `ErpInvLandedCost` / `ErpInvTransferOrder`），头表单 view + edit（+ LandedCost add）追加 lines 组 + `<cell id="lines">` 引用 sub-grid
- 3 inventory Line 实体 view.xml 新增 sub-grid-edit + sub-grid-view（共 6 个新 grid）
- `ErpMdWarehouse` + `ErpMdLocation` picker.page.yaml 补 pick-list + pick-query（picker.page.yaml wrapper 复用 codegen 既有产物）
- 1 个写路径 E2E spec 新建：`tests/e2e/crud/inventory.write.spec.ts`（4 测试）

**写路径 E2E 验证（不可降级）**：
- ErpInvStockMove：__save 含嵌套 `lines:[...]` 持久化 2 行 + 行 totalCost = qty × unitCost = 50 派生验证
- ErpInvLandedCost：__save 含嵌套 `lines:[...]` 持久化 2 行 + 退化变体直接录入 amount/costElement 验证
- ErpInvTransferOrder：__save 含嵌套 `lines:[...]` 持久化 2 行 + 最小列集 quantity/batchNo 验证
- AMIS input-table DOM 验证：ErpInvStockMove 编辑表单含 `.cxd-InputTable` 控件（codegen 展开非降级）

**回归**：`tests/e2e/crud/` 49 测试全绿（45 既有 + 4 新增）；`mvn test` BUILD SUCCESS（含 `ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0）。

**P1 范式可推广性**：
- 退化变体规则（§12.2）可推广至 P2/P3 中所有无可乘字段的行实体（如 mfg JobCard 材料行、assets 维护成本行等）
- add 表单嵌入决策（§12.3）可推广至 P2/P3 中所有已有业务 add 表单的头实体
- warehouse/location picker 列集（§12.4）可作为 ext 域类似扁平主数据 picker 的模板

## 13. P2 mfg/assets/projects 3 头行对列集表

P2 mfg/assets/projects 各取 1 对头行子表编辑（F4 Phase 2 P2），延续 P0/P1 范式并扩展「减法变体」（§14）：

### 13.1 P2 域列集

| 头实体 | 行实体 | 列集（顺序） | 自动推算 |
|--------|--------|-------------|---------|
| `ErpMfgWorkOrder` | `ErpMfgWorkOrderLine` | `lineNo` `lineType` `materialId` `uoMId` `plannedQuantity` `actualQuantity` `scrappedQuantity` `sourceWarehouseId` `destWarehouseId` `remark`（10 列） | 无（退化变体；planned/actual/scrapped 三态独立录入，非乘法派生） |
| `ErpAstInventory` | `ErpAstInventoryLine` | `lineNo` `assetId` `categoryId` `bookQuantity` `actualQuantity` `varianceQuantity` `bookValue` `assessedValue` `varianceAmount` `varianceType` `disposition` `remark`（12 列） | `varianceQuantity = actualQuantity - bookQuantity` + `varianceAmount = assessedValue - bookValue`（减法变体，scale=4 HALF_UP） |
| `ErpPrjCostCollection` | `ErpPrjCostCollectionLine` | `lineNo` `costCategory` `sourceBillType` `sourceBillCode` `subjectId` `taskId` `amount` `remark`（8 列） | 无（退化变体；amount 直接录入） |

**字段名核实证据**（实时仓库 ORM 抽样）：
- `ErpMfgWorkOrderLine`（`module-manufacturing/model/app-erp-manufacturing.orm.xml:668-720`）：`lineType`（propId=4，dict=`erp-mfg/work-order-line-type`，OUTPUT/INPUT/BYPRODUCT）+ `materialId`（propId=5）+ `uoMId`（propId=7）+ `plannedQuantity`（propId=8，domain=quantity，scale=4）+ `actualQuantity`（propId=9，scale=4）+ `scrappedQuantity`（propId=10，scale=4）+ `sourceWarehouseId`（propId=11）+ `destWarehouseId`（propId=12）；**无 quantity × standardCost 可乘字段**
- `ErpAstInventoryLine`（`module-assets/model/app-erp-assets.orm.xml:1214-1288`）：`assetId`（propId=5，可空：盘盈行无对应账面卡片）+ `categoryId`（propId=8）+ `bookQuantity`（propId=9，INTEGER）+ `actualQuantity`（propId=10，INTEGER）+ `varianceQuantity`（propId=11，INTEGER）+ `varianceType`（propId=12，dict=`erp-ast/variance-type`）+ `bookValue`（propId=13，domain=amount，scale=4）+ `assessedValue`（propId=14，scale=4）+ `varianceAmount`（propId=15，scale=4）+ `disposition`（propId=16，dict=`erp-ast/inventory-line-disposition`）；**含减法可推算对** `varianceQuantity = actualQuantity - bookQuantity` + `varianceAmount = assessedValue - bookValue`
- `ErpPrjCostCollectionLine`（`module-projects/model/app-erp-projects.orm.xml:532-571`）：`costCategory`（propId=4）+ `sourceBillType`（propId=5）+ `sourceBillCode`（propId=6）+ `subjectId`（propId=7）+ `taskId`（propId=8）+ `amount`（propId=9，domain=amount，scale=4）；**无 quantity × unitPrice 可乘字段**

### 13.2 P2 picker 接线

| Line 列 | 接线 picker | 备注 |
|---------|-------------|------|
| `ErpMfgWorkOrderLine.materialId` | `/erp/md/pages/ErpMdMaterial/picker.page.yaml`（F4 Phase 1 已落地） | WorkOrderLine.materialId 可指向任何物料，无需 filter |
| `ErpMfgWorkOrderLine.sourceWarehouseId` / `destWarehouseId` | `/erp/md/pages/ErpMdWarehouse/picker.page.yaml`（F4 P1 inventory 补齐 pick-list + pick-query） | 复用 P1 既有 pick-list |
| `ErpAstInventoryLine.assetId` | `/erp/ast/pages/ErpAstAsset/picker.page.yaml`（F4 Phase 1 已落地，pick-list 含 6 业务列非空） | 资产 picker，**本计划无需补齐** |
| `ErpAstInventoryLine.categoryId` | `/erp/ast/pages/ErpAstAssetCategory/picker.page.yaml`（codegen 默认） | 资产类别 picker |
| `ErpPrjCostCollectionLine.subjectId` | `/erp/md/pages/ErpMdSubject/picker.page.yaml`（F4 Phase 1 已落地） | 科目 picker |
| `ErpPrjCostCollectionLine.taskId` | `/erp/prj/pages/ErpPrjTask/picker.page.yaml`（codegen wrapper 存在） | 项目任务 picker |

**P2 新补齐 picker**：`ErpPrjProject`（本计划 Phase 1 落地——view.xml 补非空 `<grid id="pick-list">` 7 列 + `<form id="pick-query">` 4 字段；picker.page.yaml wrapper 复用 codegen 既有产物），服务 ErpPrjCostCollection 头表 `projectId` 反向追溯。

### 13.3 P2 头表单 lines cell 兼容性裁决

3 头实体 view.xml 经实时仓库核实在嵌入 lines cell 前**均无既有嵌套结构或 tabs 容器**，可直接在 `<form id="view">` + `<form id="edit">` 末尾追加 lines 组：

| 头实体 | 既有 form 结构 | 裁决 |
|--------|---------------|------|
| `ErpMfgWorkOrder` | `_gen/_ErpMfgWorkOrder.view.xml:156,182` 仅 2 `<layout>` 块（view + edit forms），无 tabs 容器；mfg-chain E2E spec 经 GraphQL 驱动不依赖 view.xml 结构 | 方案 A：直接追加 lines cell |
| `ErpAstInventory` | `_gen/_ErpAstInventory.view.xml` 无既有嵌套结构 | 方案 A：直接追加 lines cell |
| `ErpPrjCostCollection` | `_gen/_ErpPrjCostCollection.view.xml` 无既有嵌套结构 | 方案 A：直接追加 lines cell |

## 14. 减法变体扩展范式（varianceQuantity = actualQuantity - bookQuantity）

P0 §5 固化的是**乘法变体**（`amount = quantity × unitPrice`）。P2 引入 **减法变体**：账面/实盘的差异由减法派生，与乘法变体并列：

```
varianceQuantity = actualQuantity - bookQuantity           // 差异数量
varianceAmount   = assessedValue - bookValue               // 差异金额
```

**机制（与 P0 §5 同型）**：行级 `bookQuantity` / `actualQuantity` / `bookValue` / `assessedValue` 列的 `<col>` 内通过 `<gen-control>` 注入 AMIS `input-number` + `onEvent.change` → `setValue` action，在行数据作用域内更新 `varianceQuantity` / `varianceAmount`。精度 scale=4 HALF_UP（对齐 xmeta `<schema domain="amount" precision="20" scale="4"/>`）。

**触发器列**：`actualQuantity` 触发 `varianceQuantity` 推算；`assessedValue` 触发 `varianceAmount` 推算。账面列（`bookQuantity` / `bookValue`）通常由 picker 选资产后从快照填充（编辑态只读），不挂 onEvent；若允许手工修订账面（盘盈调整场景），账面列也挂同型 onEvent 同步差异。

**示例 col 定义**（ErpAstInventoryLine `sub-grid-edit` 内 `actualQuantity` 列）：

```xml
<col id="actualQuantity" mandatory="true">
    <gen-control>
        <c:script><![CDATA[
            return {
                type: 'input-number',
                name: 'actualQuantity',
                required: true,
                step: 1,
                validations: { minimum: 0 },
                validationErrors: { minimum: '实盘数量不能为负' },
                onEvent: {
                    change: {
                        actions: [
                            {
                                actionType: 'setValue',
                                args: {
                                    value: {
                                        varianceQuantity: '${ROUND(actualQuantity - bookQuantity, 4)}'
                                    }
                                }
                            }
                        ]
                    }
                }
            };
        ]]></c:script>
    </gen-control>
</col>
```

`assessedValue` 列同型 onEvent 推算 `varianceAmount = ROUND(assessedValue - bookValue, 4)`。`varianceQuantity` / `varianceAmount` 列设编辑态只读（由推算填充）。

**裁决依据**：
- `ErpAstInventoryLine` ORM 字段 `bookQuantity`/`actualQuantity`/`varianceQuantity` + `bookValue`/`assessedValue`/`varianceAmount` 形成 2 对减法派生关系，业务语义即「实盘 − 账面 = 差异」（资产盘点核心规则）
- 前端实时推算与 P0 §5 乘法变体同型，后端 BizModel 仍可二次校验（不冲突，因 ErpAstInventoryBizModel 无公开行级重算方法）

## 15. ErpAstCip 不适用原因 + ErpAstInventory 替换裁决

### 15.1 ErpAstCip 不适用范式（实时仓库核实证据）

P2 初期草案曾列入 `ErpAstCip ↔ ErpAstCipCostItem` 作为 assets 域头行对。经实时仓库 ORM 核实**该对不适用子表编辑范式**：

- `ErpAstCip.orm.xml` `<relations>` 仅含 4 to-one 关系（`category` / `currency` / `org` / `completedAsset`），**无 `<to-many>` 到 CipCostItem**
- `ErpAstCipCostItem` 经 `cipId` 反向指针 + `ErpAstCipBizModel.addCostItem(cipId, ...)` 单行 RPC API 设计（`ErpAstCipBizModel.java:45,70`），**非聚合根嵌套**
- 设计意图见 `docs/design/assets/cip.md:33,67,87`：CipCostItem 经跨域 hook `cipAssetId` 触发添加，业务流程是「资本化 → 资产转固 → CIP 余额迁移到新资产卡片」，CipCostItem 是 CIP 头的**只读成本明细视图**而非可编辑子表

强行实施需 ORM 修改（增加 `<to-many>` 关系），属保护区域 Non-Goal。

### 15.2 ErpAstInventory 替换裁决

P2 改用 `ErpAstInventory ↔ ErpAstInventoryLine` 作为 assets 域代表头行对：

- `ErpAstInventory.orm.xml` 含标准 `<to-many name="lines" refEntityName="...ErpAstInventoryLine" tagSet="pub,cascade-delete,insertable,updatable">` 关系 ✓
- 业务语义对齐——「资产盘点单（头）+ 多资产盘点行（行）」是标准 1:N 头行结构（每行一个资产或一条盘盈新增项；账面/实盘/差异/处置）
- 含减法自动推算（§14），范式新颖性高于 ErpAstCip，可作为 P3 ext 域类似头行对（如 hr 域薪酬头行、contract 域合同头行）的减法变体模板

### 15.3 P2 范式可推广性

- 减法变体规则（§14）可推广至 P3 中所有含「实盘/账面」「实际/预算」「本币/原币」差异派生关系的头行对
- 「不适用范式裁决」（§15）流程可推广至 P3 中 ORM 缺 `<to-many>` 关系的初判头行对：先核实 ORM 关系，无关系则归 Non-Goal 并记录 successor condition（如「需 ORM 修改后启动」）

## 16. finance 凭证子表变体（ErpFinVoucher ↔ ErpFinVoucherLine）

finance 域 ErpFinVoucher 凭证子表行内编辑（F4 Phase 2 P1 finance successor），是 P0/P1/P2 范式的最高复杂度组合（4 个全新模式 + 1 个新推算变体 + 1 个客户端单边约束 gap + 列数纪录 17 列）：

### 16.1 列集（17 列，突破 P0 8-12 列基线）

| 头实体 | 行实体 | 列集（顺序） | 自动推算 |
|--------|--------|-------------|---------|
| `ErpFinVoucher` | `ErpFinVoucherLine` | `lineNo` `subjectId` `subjectCode` `dcDirection` `debitAmount` `creditAmount` `currencyId` `exchangeRate` `amountSource` `amountFunctional` `partnerId` `departmentId` `projectId` `warehouseId` `materialId` `costCenterId` `memo`（17 列） | `amountSource = debitAmount \| creditAmount`（按 dcDirection 单边派生）+ `amountFunctional = ROUND(amountSource × exchangeRate, 4)` |

**字段名核实证据**（实时仓库 ORM `module-finance/model/app-erp-finance.orm.xml:361-441`）：`ErpFinVoucherLine` 29 列（含 6 辅助核算 FK + 1 businessType dict + 多币种 4 字段 + 科目快照 3 字段）。

### 16.2 dcDirection 行内切换 visibleOn（F7 §8 预冻结表达式落地）

借方金额 cell `visibleOn="${dcDirection == 'DEBIT'}"`，贷方金额 cell `visibleOn="${dcDirection == 'CREDIT'}"`——切换 dcDirection 时对侧 cell 隐藏，`clearValueOnHidden=true` 自动清空防脏数据提交。

**xview schema 适配裁决**：`<col>` 元素不允许 `clearValueOnHidden` 作为属性（仅 form `<cell>` 允许），故 `clearValueOnHidden` 经 `<gen-control>` 内 AMIS column object 注入（`<col><visibleOn>...</visibleOn><gen-control><c:script>return {type:'input-number', clearValueOnHidden:true, ...}</c:script></gen-control></col>`）。

per §8.3 裁决：**禁止**额外 onEvent.change.setValue 清空对侧金额——`clearValueOnHidden` 已自动处理。

### 16.3 科目 picker onEvent.setValue 8 字段快照（P0 物料快照扩展）

`subjectId` col gen-control 返回 AMIS picker + onEvent.change.actions.setValue，将所选 `ErpMdSubject` 的 8 字段（`subjectCode` + `subjectName` + 6 个 `isAuxiliary*` flag）批量快照进 input-table row scope：

```xml
<col id="subjectId" mandatory="true">
    <gen-control>
        <c:script><![CDATA[
            return {
                type: 'picker',
                name: 'subjectId',
                source: '/erp/md/pages/ErpMdSubject/picker.page.yaml',
                valueField: 'id',
                labelField: 'name',
                joinValues: false,
                extractValue: true,
                onEvent: {
                    change: {
                        actions: [{
                            actionType: 'setValue',
                            args: {
                                value: {
                                    subjectCode: '${event.data.selectedItem ? event.data.selectedItem.code : ""}',
                                    subjectName: '${event.data.selectedItem ? event.data.selectedItem.name : ""}',
                                    isAuxiliaryPartner: '${event.data.selectedItem ? (event.data.selectedItem.isAuxiliaryPartner || 0) : 0}',
                                    // ... 5 more dim flags
                                }
                            }
                        }]
                    }
                }
            };
        ]]></c:script>
    </gen-control>
</col>
```

**机制裁决**：F9 plan `2026-07-20-0629-3` 已落地 `${selectedOrderLines | map: {...}}` 行映射范式（picker multi-select + 行映射）；本变体是其「单选 + 行内 scope」扩展。AMIS picker change event `event.data.selectedItem` 暴露选中项（`extractValue:true` 保留 value id 提交；selectedItem 经 AMIS 内部 __picker 维护）。

### 16.4 subject 驱动 6 维度 visibleOn（跨实体表达式 + 宽松兜底）

6 辅助维度 col（`partnerId`/`departmentId`/`projectId`/`warehouseId`/`materialId`/`costCenterId`）的 `<visibleOn>` 引用 §16.3 快照的 `isAuxiliary*` flag——科目驱动行内显隐。`materialId` col 引用 `isAuxiliaryProduct`（subject.isAuxiliaryProduct 对应 materialId，非 productId）。

**宽松表达式 graceful degradation**：

```xml
<col id="partnerId">
    <visibleOn>${!subjectId || isAuxiliaryPartner == true}</visibleOn>
</col>
```

- 无科目（`subjectId == null`）：全显（允许先选维度再补科目）
- 有科目 + 快照成功：仅显该科目启用的维度
- 有科目 + 快照失败（picker event.data.selectedItem 不可达）：全显（兜底防「全部隐藏」）

### 16.5 多币种自动推算（P0 §5 乘法变体新应用场景）

`amountFunctional = ROUND(amountSource × exchangeRate, 4)`（scale=4 HALF_UP 对齐 xmeta）。触发器：`amountSource` + `exchangeRate` 双 cell onEvent.change.actions.setValue 推算。`amountSource` 自身由 `debitAmount`/`creditAmount` cell onEvent 派生（按 dcDirection 单边：DEBIT 行 `amountSource = debitAmount`，CREDIT 行 `amountSource = creditAmount`）。

### 16.6 autoBalance 按钮触发头合计刷新（xview schema 限制 + P0/P1 既有行为对齐）

xview.xdef schema 限制 `<cell><view>` 元素仅允许 `<data>` 子节点，**onEvent 不能直接挂到 `<view>` 上**。P0 closure audit 已发现「头聚合 onEvent 在 P0 实际未实施」（plan `2026-07-20-0629-1` Deferred §StockMove 头表单 totalCost 头级聚合）；本变体对齐 P0/P1 既有行为：**头合计 totalDebit/totalCredit 改由 autoBalance 按钮 onClick 触发刷新**（custom script compute + setValue head totals + 平衡末行），实时聚合 onEvent 留待统一对齐 plan successor。

**autoBalance 按钮 gen-control 范式**：

```xml
<cell id="autoBalance" custom="true" notSubmit="true">
    <gen-control>
        <c:script><![CDATA[
            return {
                type: 'button',
                label: '刷新合计 + 借贷自动平衡（差额填入末行）',
                level: 'warning',
                icon: 'fa fa-balance-scale',
                onEvent: {
                    click: {
                        actions: [{
                            actionType: 'custom',
                            script: 'const lines = Array.isArray(event.data.lines) ? event.data.lines.slice() : []; const totalD = lines.reduce((s, l) => s + (Number(l.debitAmount) || 0), 0); const totalC = lines.reduce((s, l) => s + (Number(l.creditAmount) || 0), 0); ... doAction({actionType:"setValue", args:{value:{lines: balancedLines, totalDebit: ..., totalCredit: ...}}});'
                        }]
                    }
                }
            };
        ]]></c:script>
    </gen-control>
</cell>
```

### 16.7 过账按钮 disabledOn + list 平衡状态列

`row-post-button` 含 `<disabledOn>${(Number(totalDebit) || 0) != (Number(totalCredit) || 0)}</disabledOn>` 子节点 + `tooltip` 提示；list grid 新增 `balanceStatus` virtual col（`custom="true"` + gen-control tpl `✓ 平衡 / ✗ 不平衡` label success/danger 着色）。

### 16.8 凭证子表变体反模式自检

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 在 `<col>` 上加 `clearValueOnHidden="true"` 属性 | `<col>` 内 `<gen-control>` AMIS object 携带 `clearValueOnHidden: true` |
| 在 `<view>` 内嵌 `<onEvent>` 子节点（schema 禁止） | 头合计改由 autoBalance 按钮 onClick custom script 触发，或归统一头聚合对齐 successor |
| subject picker onEvent 清空对侧金额（违反 §8.3） | 仅依赖 clearValueOnHidden 自动处理 |
| materialId col visibleOn 引用 `isAuxiliaryMaterial` | 引用 `isAuxiliaryProduct`（subject 字段名核实，对应 materialId） |
| 行 cell visibleOn 用严格表达式 `${isAuxiliaryPartner == true}` | 宽松 `${!subjectId \|\| isAuxiliaryPartner == true}` 防快照失败时全隐 |
| `<action>` 的 `disabledOn` 写成属性 | `<disabledOn>` 写成子节点（xview schema） |
| grid virtual col（如 balanceStatus）漏写 `custom="true"` | 加 `custom="true"` 否则 codegen 报 `col-not-prop` |

### 16.9 凭证子表变体可推广性

- dcDirection 行内切换范式可推广至任何含「借贷/收付/进出」双向字段的行实体（如 ErpFinReconciliation 行核销方向、ErpFinBankStatement 行收入/支出方向）
- 科目 picker 多字段快照 + subject 驱动维度 visibleOn 范式可推广至任何需要「主数据驱动行内字段动态显隐」的场景（如物料 picker 快照驱动批次/序列号 cell 显隐）
- autoBalance 按钮 custom script 触发头合计刷新范式可推广至任何需要「按钮触发计算 + 多字段写回」的场景（避免 xview schema 限制 onEvent on `<view>`）
