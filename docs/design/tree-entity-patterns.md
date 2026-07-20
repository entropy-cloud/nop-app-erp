# 树形实体 CRUD 范式（Tree Entity Patterns）

> Owner docs: `docs/backlog/frontend-ui-roadmap.md` §F10、`docs/design/<domain>/ui-patterns.md`（每域业务语义）、`docs/architecture/view-and-page-strategy.md`（view.xml grid 与 page 层结构）
> 平台权威源：`../nop-entropy/docs-for-ai/03-runbooks/build-tree-crud-page.md`
> 落地计划：`docs/plans/2026-07-20-1020-1-f10-tree-entity-views.md`（4 实体 tree CRUD + tree-select + add-child + tree picker）

## 1. 目的与范围

固化「ERP 自引用树形实体」的标准 CRUD 范式，供 P3/ext 域后续引入的树形实体按图施工。

**适用范围**：实体 ORM 含 `parentId` self-FK 列，且业务上以层级（分类/科目/部门/目录）组织。

**不适用**：
- 1:N 头行子表（行子表通过 `headId` 反向引用，非自引用树）→ `child-table-editor-patterns.md`
- 多级 BOM（层级经子行实体 `ErpMfgBomLine.bomId` 反向表达，非自引用 `parentId`）→ F16 BOM 复杂页面 successor
- 扁平分类表（无 `parentId`，如 `ErpAstAssetCategory`）→ 标准 CRUD（非本范式）

## 2. 后端前置条件（已就绪，本范式不改后端）

`__findList` 是 Nop Platform `CrudBizModel` 内置 @BizQuery（`../nop-entropy-wt/nop-entropy-feat-agent/nop-service-framework/nop-biz/src/main/java/io/nop/biz/crud/CrudBizModel.java:1528-1532`）：

```java
public List<T> findList(@Optional @Name("query") QueryBean query,
                       FieldSelectionBean selection,
                       IServiceContext context)
```

自动暴露于所有继承 `CrudBizModel<T>` 的 BizModel。支持：
- URL 参数 `filter_parentId=__null` 过滤根节点
- GraphQL selection 嵌套 `children @TreeChildren(max:N)` 自动按 ORM `children` 关系递归返回子节点
- 其他 `filter_xxx` URL 参数照常生效（如 ErpMdSubject 的 `filter_isLeaf=1`）

**本范式仅定制 view.xml + page.yaml**，不动 ORM/BizModel/Java。

## 3. view.xml 树形 CRUD 三件套（最小闭环）

### 3.1 tree-list grid（克隆 list + 加 `@TreeChildren` selection）

```xml
<grid id="tree-list" x:prototype="list">
    <selection>children @TreeChildren(max:5)</selection>
</grid>
```

`@TreeChildren(max:5)` 是 Nop GraphQL 引擎对标准 GraphQL 的扩展，表示嵌套返回子节点对象（子节点字段集与父节点相同）。`max:5` 覆盖典型 5 级层级（物料分类/科目/部门/服务目录）。

### 3.2 crud 引用 tree-list + tree 表格配置

```xml
<crud name="main" grid="tree-list">
    <table loadDataOnce="true" sortable="false" pager="none">
        <api url="@query:Xxx__findList/{@listSelection}?filter_parentId=__null"/>
    </table>
</crud>
```

三个 table 属性的组合语义：
- `loadDataOnce="true"`：一次性加载所有根节点 + 子节点（通过 `@TreeChildren` 嵌套返回），前端不再分页拉取
- `sortable="false"`：树形结构按层级排序，禁用列排序（避免子节点排序打乱层级）
- `pager="none"`：不分页（树形分页无业务意义）
- URL `filter_parentId=__null`：只拉取根节点，子节点由 `@TreeChildren` 自动展开

### 3.3 add-child simple page（预填父节点上下文）

```xml
<simple name="add-child" form="add">
    <data>
        <parentId>$id</parentId>
    </data>
</simple>
```

`$id` 是当前行（即将成为父节点）的 id。`<data>` 块显式注入上下文到 add 表单。**注意**：一旦 `<simple>` 声明了 `<data>`，外部上下文不会自动完整继承；其他需要的父节点字段要手工传入（对齐 `build-tree-crud-page.md §两个关键细节`）。

### 3.4 rowActions 追加 row-add-child-button

```xml
<crud name="main" grid="tree-list">
    ...
    <rowActions>
        <action id="row-add-child-button" level="primary" label="新增子节点">
            <dialog page="add-child"/>
        </action>
    </rowActions>
</crud>
```

默认 `merge` 策略：与既有 row-view/row-update/row-delete 并存。label 中文，F15 i18n successor 加 `i18n-en:label`。

## 4. tree-select 父节点选择器（替代扁平 picker）

edit/add 表单的 `parentId` 字段升级为 AMIS `tree-select` 下拉：

```xml
<cell id="parentId">
    <gen-control>
        <tree-select clearable="@:true">
            <source>
                <url>@query:Xxx__findList/{@listSelection}?filter_parentId=__null&amp;filter_id__ne=$id</url>
            </source>
        </tree-select>
    </gen-control>
</cell>
```

**两个关键 URL 参数**：
- `filter_parentId=__null`：从根节点开始展开整树
- `filter_id__ne=$id`：排除当前编辑节点自身（防节点成为自身父节点，循环引用）

`@listSelection` 由 grid 配置自动推导字段列表，tree-select 内部按 `@TreeChildren(max:5)` 嵌套展开。

**与 NopAuthResource 参考实现的差异**：
- NopAuthResource 仅按 `filter_resourceType=TOPM` 过滤根节点；ERP 4 实体无类似 type 区分，统一用 `filter_parentId=__null`
- ERP 实体额外需要 `filter_id__ne=$id` 排除自身（NopAuthResource 通过 `resourceType != 'TOPM'` 间接保证）

## 5. tree picker（弹窗中以树形选择节点）

```xml
<picker name="picker">
    <table loadDataOnce="true" sortable="false" pager="none">
        <api url="@query:Xxx__findList/{@listSelection}?filter_parentId=__null"/>
    </table>
</picker>
```

picker 默认引用 `pick-list` grid（扁平列表）。本范式升级为 tree 表格配置（同 crud），让用户在弹窗中以树形结构浏览选择节点。

picker grid 由 `<picker name="picker">` 的 `grid=` 属性决定。若需沿用 tree-list grid 列集，可显式声明 `grid="tree-list"`；若需保留 pick-list 窄列集 + 树形浏览，可让 pick-list grid 也加 `<selection>children @TreeChildren(max:5)</selection>`（推荐做法）。

## 6. 4 树形实体列集表

最少列集 = `code` + `name` + `parentId` + 域专用 ≤ 3 字段（对齐 F10 plan Phase 1 决策）。

| 实体 | 域 | 列集（顺序） | 域专用业务约束 |
|------|----|----|----|
| `ErpMdMaterialCategory` | master-data | `code` `name` `parentId` `sortNum` `priceValidationLevel` | 物料分类树，叶子节点用于物料归类 |
| `ErpMdSubject` | master-data | `code` `name` `parentId` `subjectClass` `direction` `isLeaf` `status` | 凭证录入科目 picker 仅返回叶子节点（`filter_isLeaf=1`） |
| `ErpHrDepartment` | human-resource | `code` `name` `parentId` `manager` `costCenterId` `orgId` | 组织架构树，F16 复杂页面（节点嵌入员工）successor |
| `ErpCsServiceCatalogItem` | customer-service | `code` `name` `parentId` `ticketTypeId` `slaPolicyId` `fulfillmentProcessId` `isActive` `sequence` | 服务目录树，含工单类型/SLA 策略/履行流程业务字段 |

**字段名核实证据**：列集来自 4 实体手写层 `view.xml` 的 `<grid id="list">` + ORM 字段定义。

## 7. 反模式自检表

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 树形 grid 用 `__findPage` 分页拉取 | `__findList/{@listSelection}?filter_parentId=__null`，一次性加载 + `@TreeChildren` 嵌套 |
| 只把 grid 改成树表，却忘了 `<selection>children @TreeChildren(...)</selection>` | tree-list grid 必须显式声明 selection（不然子节点不会嵌套返回） |
| crud table 保留 `pager="true"` + 列排序 | `loadDataOnce="true" + sortable="false" + pager="none"` 三件套必须同时设置 |
| `add-child` 简单 `<data>` 仅注入 `parentId` 而漏掉其他必要上下文 | 显式注入所有需要的父节点字段（参考 `build-tree-crud-page.md §两个关键细节`） |
| edit/add `parentId` 字段保留扁平 picker 弹窗 | 升级为 `<tree-select>` 下拉，URL 含 `filter_id__ne=$id` 排除自身 |
| picker.page.yaml 仍用 `pick-list` 平铺列集 | 升级为 tree 表格配置（`loadDataOnce + pager=none + filter_parentId=__null`） |
| `@TreeChildren(max:N)` 用 1 或 2 | `max:5` 覆盖典型 5 级层级（对齐 NopAuthResource 范式） |
| `filter_parentId=__null` 写成 `filter_parentId=null` 或 `filter_parentId=` | Nop 专用 `__null` 字面量（标准 `null` 不会被识别） |
| 4 实体 view.xml 复制粘贴整份 grid 定义 | `<grid id="tree-list" x:prototype="list">` 只写差异（selection） |
| picker grid 同时声明 `pick-list` + tree-list 引起冲突 | picker grid 二选一（推荐沿用 pick-list 但加 selection；或显式 `grid="tree-list"`） |

## 8. 排除实体（附录）

下列实体原 roadmap §F10 曾列入，经 ORM 核实后从本范式剔除：

### 8.1 ErpMfgBom（多级 BOM 展开）

**排除原因**：ORM 无 `parentId` self-FK 列；其层级关系经子行实体（`ErpMfgBomLine` 通过 `bomId` 反向）表达，非自引用树。

**Successor**：F16 BOM 复杂手写页面 successor（多级展开/折叠 + phantom 节点图标 + 工艺路线水平流向图），需独立设计自定义可视化组件，不基于本计划 tree-list grid。

### 8.2 ErpAstAssetCategory（原 roadmap 命名错误 `ErpAstCategory`）

**排除原因**：ORM 无 `parentId` 列；资产类别为扁平分类表（`code` `name` + 5 个科目 FK + 折旧方法），非树形结构。无 tree CRUD 语义。

**Successor**：无（建议 roadmap 维护者下次更新时从 §F10 移除）。

## 9. 变更记录

| 日期 | 变更 | 来源 |
|------|------|------|
| 2026-07-20 | 初版落地（4 树形实体 CRUD 范式 + tree-select 模板 + add-child simple page + tree picker + 列集表 + 反模式自检表 + ErpMfgBom/ErpAstAssetCategory 排除附录） | `docs/plans/2026-07-20-1020-1-f10-tree-entity-views.md` |
