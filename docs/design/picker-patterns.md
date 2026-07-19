# Picker 定制范式

> Status: stable
> Owner docs: `docs/backlog/frontend-ui-roadmap.md` §F4 Phase 1、`docs/architecture/view-and-page-strategy.md` §文件层次结构 / §picker.page.yaml 范式
> 平台参考: `../nop-entropy/docs-for-ai/02-core-guides/view-and-page-customization.md`、`../nop-entropy/docs-for-ai/03-runbooks/customize-view.md`、`../nop-entropy/docs-for-ai/04-reference/safe-api-reference.md`（`__findList` / `__findPage` 端点）

## 1. 范式目标

本文档固化 ERP 系统 7 个高频关联 Picker 的列集、筛选字段、调用方 filter 注入约定，作为：

- 后续 F4 Phase 2（子表行内编辑）中选择器列集的稳定基线
- 各业务域新增通用主数据 Picker 时的参考模板
- view.xml picker 定制反模式自检的判据

仅覆盖扁平列表式选择器。树形（科目树/分类树/BOM 树/组织架构树）归 F10，业务专用（批次/序列号/未付发票核销）归独立功能请求。

## 2. 平台管线（codegen 默认 → view.xml delta）

每实体 codegen 默认产物（见 `_gen/_<Entity>.view.xml`）：

```xml
<grids>
    <grid id="list" x:abstract="true"><cols>...全部字段...</cols></grid>
    <grid id="pick-list" x:prototype="list" x:abstract="true"/>   <!-- 默认克隆 list -->
</grids>
<forms>
    <form id="query" editMode="query" x:abstract="true"/>          <!-- 默认空 query -->
</forms>
<pages>
    <picker name="picker" grid="pick-list" filterForm="query" x:abstract="true">
        <table noOperations="true">
            <api url="@query:<Entity>__findPage" gql:selection="{@pageSelection}"/>
        </table>
    </picker>
</pages>
```

`picker.page.yaml` 仅是 `<web:GenPage view="..." page="picker"/>` 的展开入口（见 `module-purchase/erp-pur-web/.../ErpPurOrder/picker.page.yaml`），运行时由 `page_picker.xpl` + `grid_crud.xpl`（nop-entropy）展开为完整 AMIS JSON：

- grid 列集 → AMIS `columns`
- `filterForm`（默认 `query`）→ 顶部筛选表单
- `<api url>` → AMIS `source`/`api`，通过 `XuiHelper.appendFilterProps(url, fixedProps)` 自动追加 `filter_<prop>` 查询参数

**修改入口**：view.xml 的 `<grid id="pick-list">` 与 `<form id="pick-query">`（或共用 `<form id="query">`）。**不要**直接编辑 `_gen/_*.view.xml` 或 `picker.page.yaml`（除非需要 delta 覆盖弹窗壳）。

## 3. bounded-merge 写法范式

```xml
<view x:extends="_gen/_ErpMdXxx.view.xml" ...>

    <grids>
        <grid id="pick-list">
            <cols x:override="bounded-merge">
                <!-- 只列要保留的列；其余继承自 list grid 的列全部丢弃 -->
                <col id="id" mandatory="true" ui:number="true" sortable="true"/>
                <col id="code" mandatory="true" sortable="true"/>
                <col id="name" mandatory="true" sortable="true"/>
                <!-- ...业务专用列... -->
            </cols>
            <!-- 可选：硬性 filter（如叶子科目） -->
            <filter>
                <eq name="isLeaf" value="1"/>
            </filter>
        </grid>
    </grids>

    <forms>
        <!-- picker 专用查询表单（与主表 query 分离，避免互相污染） -->
        <form id="pick-query" editMode="query" title="查询条件">
            <layout>
 code[编码] name[名称]
 status[状态] otherProp[其它]
            </layout>
            <cells>
                <cell id="code" filterOp="like"/>
                <cell id="name" filterOp="like"/>
                <!-- status / otherProp 默认 eq，无需 filterOp -->
            </cells>
        </form>
    </forms>

    <pages>
        <!-- 关键：filterForm="pick-query" 覆盖默认的 query -->
        <picker name="picker" filterForm="pick-query"/>
    </pages>
</view>
```

**`bounded-merge` 的语义**：以当前节点为准，只保留显式列出的子节点，未列出的全部丢弃。适合「白名单」式裁剪——pick-list 通常只展示业务关键列，而不是 `list` grid 的全部 20+ 列。

## 4. picker.page.yaml 何时需 delta

**默认**：不动 `picker.page.yaml`。仅在以下情况做 `_vfs/_delta/default/<module>/pages/<Entity>/picker.page.yaml` 覆盖：

| 场景 | delta 改造 |
|------|----------|
| 弹窗标题/宽度要业务专用 | `picker.page.yaml` 顶层加 `title` / `size` |
| 默认排序非主键 | `picker.page.yaml` 加 `orderBy` |
| 调用方需固定传入默认 filter（如 partnerType=Supplier） | 见 §5 调用方 filter 注入 |

本 F4 Phase 1 范围内**无需** delta 覆盖 `picker.page.yaml`——view.xml 内定制已足够。

## 5. 调用方 filter 注入约定

### 5.1 三种机制对比

| 机制 | 写法 | 适用场景 |
|------|------|---------|
| **A. 用户在 picker 弹窗内筛选** | picker 的 `pick-query` 表单含筛选字段（如 `partnerType`），用户手动选择 | 同一 picker 服务于多种业务上下文（如 Partner 同时被客户/供应商调用） |
| **B. picker.page.yaml 全局固定 filter** | view.xml 的 `<grid id="pick-list"><filter><eq name="isLeaf" value="1"/></filter></grid>` | 硬性约束，所有调用方一致（如科目选择器只允许叶子科目） |
| **C. 调用方局部 filter** | 在调用方 view.xml 的 cell 上定制 AMIS picker，`source.data.filter` 传入默认条件 | 单一调用方的业务专用约束（如采购订单的供应商字段固定 partnerType=Supplier） |

### 5.2 项目默认约定

- **partnerType（客户/供应商区分）**：**机制 A**——通过 Partner picker 的 `pick-query` form 含 `partnerType` 筛选字段，用户在弹窗内手动选择。理由：同一 Partner picker 被采购域（Supplier）/销售域（Customer）/库存域（全部）调用，硬性固定 filter 会破坏多场景复用。
- **isLeaf（叶子科目约束）**：**机制 B**——ErpMdSubject 的 pick-list grid 加 `<filter><eq name="isLeaf" value="1"/></filter>`，所有调用方一致只允许选叶子科目（凭证录入硬性约束，非用户偏好）。
- **categoryId / status / materialType 等**：picker 的 `pick-query` form 提供筛选字段，用户按需选择。

### 5.3 调用方局部 filter（机制 C，本 Phase 不实现）

仅当某调用方有强业务专用约束且与全局默认冲突时使用。写法（保留给 F4 Phase 2 / 业务专用 picker）：

```xml
<!-- 在调用方 view.xml 的 form cell 上 -->
<cell id="supplierId" label="供应商">
    <picker>
        <source url="/erp/md/pages/ErpMdPartner/picker.page.yaml"
                data="{{filter: { $and: [{partnerType: 'Supplier'}]} }"/>
    </picker>
</cell>
```

本 Phase 不实现机制 C；记录到 F4 Phase 2 待办（子表行内物料/科目选择是更高频路径，统一在那时实现 caller filter 推广）。

## 6. 7 个高频 Picker 列集与筛选字段

### 6.1 ErpMdMaterial（物料选择器）

**调用场景**：采购订单行/入库行/出库行的物料字段；BOM 子件；质检单；库存移动单。

| 列 | 字段 | 说明 |
|----|------|------|
| ID | `id` | 内部主键（隐藏列宽） |
| 物料编码 | `code` | 主键候选，必输 |
| 物料名称 | `name` | 显示字段 |
| 物料类型 | `materialType` | dict=`erp-md/material-type`（原材料/半成品/成品等） |
| 分类 | `categoryId` | FK→ErpMdMaterialCategory，AMIS 自动渲染 `category.name` |
| 主计量单位 | `uoMId` | FK→ErpMdUoM，AMIS 自动渲染 `uoM.name` |
| 状态 | `status` | dict=`erp-md/active-status` |

**pick-query 筛选字段**：`code(like) | name(like) | materialType(eq) | status(eq)`

**不实现**：`specificationModel`（规格型号，ERP 模型无此字段）、`defaultPurchasePrice`（默认采购价，模型无此字段）、`currentAvailableStock`（实时库存可用量，跨实体聚合，归 F4 Phase 2 子表行内显示）。

### 6.2 ErpMdPartner（往来单位选择器 — 客户/供应商通用）

**调用场景**：采购订单头 supplierId；销售订单头 customerId；付款单/收款单 partnerId；合同 partnerId。

| 列 | 字段 | 说明 |
|----|------|------|
| ID | `id` | 内部主键 |
| 编码 | `code` | 唯一键 |
| 名称 | `name` | 显示字段 |
| 类型 | `partnerType` | dict=`erp-md/partner-type`（Customer/Supplier/Both） |
| 税号 | `taxNo` | 用于发票/税务校验 |
| 信用额度 | `creditLimit` | 金额字段，影响下单/付款决策 |
| 状态 | `status` | dict=`erp-md/active-status` |

**pick-query 筛选字段**：`code(like) | name(like) | partnerType(eq) | status(eq)`

**partnerType 区分调用方**：通过 `pick-query` 的 `partnerType` 筛选字段，用户在弹窗内手动选择 Supplier/Customer（机制 A）。

**不实现**：`level`（Partner 模型无此字段）、`creditLimit/arBalance` 联表聚合（`receivableBalance/payableBalance` 是 Partner 本实体的派生字段，已在表中，调用方按需补充）。

### 6.3 ErpMdEmployee（员工选择器）

**调用场景**：业务单据的负责人/使用人/经手人（如资产使用人、采购员、销售员）。

| 列 | 字段 | 说明 |
|----|------|------|
| ID | `id` | 内部主键 |
| 工号 | `code` | 唯一键 |
| 姓名 | `name` | 显示字段 |
| 所属组织/部门 | `orgId` | FK→ErpMdOrganization，AMIS 自动渲染 `org.name` |
| 职务 | `position` | 自由文本 |
| 状态 | `status` | dict=`erp-md/active-status` |

**pick-query 筛选字段**：`code(like) | name(like) | orgId(eq) | status(eq)`

### 6.4 ErpAstAsset（资产选择器）

**调用场景**：资产维护单头/资产领用单/资产调拨单的资产字段；折旧计提凭证行。

| 列 | 字段 | 说明 |
|----|------|------|
| ID | `id` | 内部主键 |
| 资产编码 | `code` | 唯一键 |
| 资产名称 | `name` | 显示字段 |
| 资产类别 | `categoryId` | FK→ErpAstCategory，AMIS 自动渲染 `category.name` |
| 净值 | `netBookValue` | 金额字段，影响处置决策 |
| 资产状态 | `status` | dict（在用/闲置/已处置等） |

**pick-query 筛选字段**：`code(like) | name(like) | categoryId(eq) | status(eq)`

### 6.5 ErpMdCurrency（币种选择器）

**调用场景**：所有外币单据头币种字段（采购/销售/付款/收款/凭证/资产）。

| 列 | 字段 | 说明 |
|----|------|------|
| ID | `id` | 内部主键 |
| 币种代码 | `code` | ISO 4217（CNY/USD/EUR） |
| 名称 | `name` | 中文全称 |
| 符号 | `symbol` | ¥ / $ / € |
| 小数位 | `decimalPlaces` | 决定金额精度 |
| 是否启用 | `isActive` | 停用币种不应被新单选择 |

**pick-query 筛选字段**：`code(like) | name(like) | isActive(eq)`

### 6.6 ErpMdSubject（会计科目选择器）

**调用场景**：凭证录入行 subjectId（最高频）；科目预算配置；报表取数公式。

| 列 | 字段 | 说明 |
|----|------|------|
| ID | `id` | 内部主键 |
| 科目编码 | `code` | 唯一键（多级编码） |
| 科目名称 | `name` | 显示字段 |
| 科目类别 | `subjectClass` | dict（资产/负债/权益/成本/损益） |
| 余额方向 | `direction` | dict=`erp-md/subject-direction`（借/贷） |
| 是否明细 | `isLeaf` | 凭证录入只能选叶子科目 |

**pick-query 筛选字段**：`code(like) | name(like) | subjectClass(eq) | direction(eq)`

**硬性 filter**：pick-list grid 加 `<filter><eq name="isLeaf" value="1"/></filter>`，所有调用方一致只允许选叶子科目（机制 B）。F10 树形升级时移除此 filter，改为树形选择器。

## 7. 列集设计原则

1. **最少列集原则**：编码 + 名称 + 业务关键 1-3 列 + 状态列；不超过 7 列（picker 弹窗宽度限制，避免横向滚动）。
2. **FK 字段优先**：列集优先用 FK 字段（如 `categoryId`/`uoMId`），AMIS 自动渲染关联实体的 `joinRightDisplayProp`（通常为 `name`），无需手动写 `category.name` 列。
3. **dict 字段保留**：含 `dict` 属性的字段（如 `status`/`materialType`/`partnerType`）直接用原字段 ID，AMIS 渲染为 select 下拉。
4. **避免派生计算列**：picker 是选择器不是详情页，不显示实时余额/可用量等需后端聚合的字段（归子表行内显示）。
5. **筛选字段与列集对齐**：pick-query 的筛选字段优先复用列集字段（用户看到什么就能按什么筛），新增 1-2 个高频筛选（如 partnerType 不在列上但需要按此筛）。

## 8. 反模式（自检）

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 直接编辑 `_gen/_*.view.xml` 加 pick-list 列 | 在保留层 `*.view.xml` 用 `<cols x:override="bounded-merge">` 覆盖 |
| 把整份 `list` 列复制到 `pick-list` | 用 `bounded-merge` 只保留 picker 关键列 |
| 直接编辑 `picker.page.yaml` 改弹窗 | 优先在 view.xml 内定制；仅必要时 delta 覆盖到 `_vfs/_delta/default/<module>/` |
| 在 `pick-query` 上加 `filter_` 前缀 | `editMode="query"` 自动加前缀；用 `filterOp` 控制运算符 |
| picker 与主表共用 `query` form 但字段冲突 | 用独立 `pick-query` form，避免主表筛选字段污染 picker |
| FK 字段写 `category.name` 而非 `categoryId` | FK 字段 AMIS 自动渲染关联名称，直接用原字段 ID |

## 9. 推广范围

本范式在 F4 Phase 1 落地 7 个高频 Picker 后，按以下优先级推广到 18+1 域：

- **F4 Phase 2（子表编辑）**：所有头行实体的行内 FK 字段统一接入本范式 picker（含物料/科目/合作伙伴）
- **业务专用 picker**：批次选择器、序列号录入、BOM 树选择器、未付发票核销选择器等独立功能请求
- **F10（树形实体视图）**：科目/分类/组织升级为树形 picker

## 10. 变更历史

| 日期 | 变更 | 关联 |
|------|------|------|
| 2026-07-19 | 初版落地（7 个高频 Picker 列集 + bounded-merge 范式 + 调用方 filter 三种机制） | `docs/plans/2026-07-19-1818-1-f4p1-high-frequency-picker.md` |
