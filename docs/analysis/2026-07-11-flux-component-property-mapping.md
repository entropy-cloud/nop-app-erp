# view.xml ↔ Flux 组件属性完整对照表

> 日期：2026-07-11
> 数据来源：view.xml XDef schemas（nop-entropy/nop-xdefs）+ Flux renderer schemas（nop-chaos-flux/packages/flux-renderers-*）
> 状态标记：✅ 已支持 | ❌ 未支持(建议补充) | 🔄 设计差异(Flux有替代) | ⚠️ 名称不同但语义相同

---

## 1. 基础属性（BaseSchema — 所有组件共有）

对应 view.xml 的 `disp.xdef`（每个 col/cell 继承）和 `xdsl.xdef` 的通用属性。

| view.xml / disp.xdef | Flux BaseSchema | Flux 类型 | 状态 | 映射说明 |
|---|---|---|---|---|
| `id` | `id` | `string` | ✅ | 直接映射 |
| `label` | `label` | `string` | ✅ | 直接映射 |
| — | `name` | `string` | ✅ | view.xml 中由 col/cell 的 `id` 充当字段名 |
| — | `className` | `string` | ✅ | view.xml 的 `<className>` |
| — | `frameClassName` | `string` | ✅ | 无 view.xml 对应 |
| — | `classAliases` | `Record<string,string>` | ✅ | Flux 独有：Tailwind 别名 |
| `visibleOn="${expr}"` | `visible` | `boolean \| string` | ✅ | Flux 统一值语义：`visible: "${expr}"`，去 On 后缀 |
| — | `hidden` | `boolean \| string` | ✅ | `hidden` 与 `visible` 互补 |
| `disabledOn="${expr}"` | `disabled` | `boolean \| string` | ✅ | Flux 统一值语义：`disabled: "${expr}"` |
| `<if>${expr}</if>` | `when` | `boolean \| string` | ⚠️ | view.xml 的 `<if>` → Flux `when` |
| — | `testid` | `string` | ✅ | 测试 ID |
| — | `frameWrap` | `boolean \| 'label' \| 'group' \| 'none'` | ✅ | Flux 独有：字段框装饰模式 |
| — | `validateOn` | `'change' \| 'blur' \| 'submit'` | ✅ | Flux 独有：校验触发时机 |
| — | `showErrorOn` | `'touched' \| 'dirty' \| 'visited' \| 'submit'` | ✅ | Flux 独有：错误显示时机 |
| — | `onMount` / `onUnmount` | `ActionSchema` | ✅ | Flux 独有：生命周期 action |
| — | `xui:imports` | `XuiImportSpec[]` | ✅ | Flux 独有：声明式能力导入 |
| `xui:role` | — | — | 🔄 | Flux 在平台层处理权限裁剪，schema 中不保留 |
| `xui:permissions` | — | — | 🔄 | 同上 |

---

## 2. 表单字段基础（BoundFieldSchemaBase — 所有输入控件共有）

对应 view.xml 的 `disp.xdef` + `form.xdef` 的 `<cell>` 属性。

| view.xml cell/disp 属性 | Flux BoundFieldSchemaBase | Flux 类型 | 状态 | 映射说明 |
|---|---|---|---|---|
| `id`（cell/col 的 id） | `name` | `string` | ✅ | 字段绑定路径 |
| `readonly="true"` | `readOnly` | `boolean \| string` | ✅ | 接受表达式 |
| `mandatory="true"` | `required` | `boolean \| string` | ✅ | 接受表达式 |
| — | `mode` | `'normal' \| 'horizontal'` | ✅ | 字段布局模式 |
| — | `labelAlign` | `'top' \| 'left' \| 'right' \| 'inherit'` | ✅ | 标签对齐 |
| — | `labelWidth` | `string \| number` | ✅ | 标签宽度 |
| `<hint>` | `hint` | `string` | ✅ | 录入提示 |
| `<desc>` | `description` | `string` | ✅ | 字段描述 |
| — | `remark` | `FieldRemarkSchema` | ✅ | 字段备注图标+内容 |
| — | `labelRemark` | `FieldRemarkSchema` | ✅ | 标签备注图标+内容 |
| `domain="roleId"` | — | — | 🔄 | Flux 无 domain 概念；由 flux-control.xlib 在编译期推导出具体控件类型 |
| `stdDomain="enum"` | — | — | 🔄 | 同上 |
| `control="myControl"` | — | — | 🔄 | 同上 |
| `<gen-control>` | — | — | 🔄 | Flux 无等价物；在 page.yaml 中用 `x:gen-extends` XPL 直接生成 Flux JSON |
| `maxLength` / `minLength` | `maxLength` / `minLength` | `number` | ✅ | InputSchema 属性 |
| `depends="a,b"` | — | — | ❌ | Flux 无显式依赖声明；由表达式引用自动推导 |
| `placeholder` | `placeholder` | `string` | ✅ | InputSchema 属性 |
| `defaultValue` | — | — | 🔄 | Flux 通过 `data` 初始值或 `initAction` 设置 |
| `notSubmit="true"` | — | — | 🔄 | Flux 无此概念；隐藏字段不参与提交由 `hiddenFieldPolicy.clearValueWhenHidden` 控制 |
| `custom="true"` | — | — | ✅ | 仅 view.xml 校验层概念，不影响 Flux 输出 |
| `submitOnChange="true"`（cell级） | — | — | 🔄 | Form 级 `submitOnChange: true` 覆盖 |
| `clearValueOnHidden="true"` | `hiddenFieldPolicy.clearValueWhenHidden` | `boolean` | ✅ | Flux 在 Form 级统一配置 |
| `<validator>` | `validate` | `{ action, debounce, message }` | ⚠️ | Flux 用 action 校验：`validate: { action: { ... }, message: "..." }` |

---

## 3. 表单（Form）

对应 view.xml `form.xdef` 的 `<form>` → Flux `FormSchema`。

| view.xml form 属性 | Flux FormSchema | Flux 类型 | 状态 | 映射说明 |
|---|---|---|---|---|
| `id` | `id` | `string` | ✅ | |
| `editMode="add/edit/view/query"` | — | — | 🔄 | Flux 无 editMode 概念；由 flux-web.xlib 在编译期按 editMode 选择不同控件标签（edit-xxx vs view-xxx vs query-xxx） |
| `title` | — | — | 🔄 | Flux Form 无 title；用外层 Dialog/Drawer 的 title |
| `size="sm/md/lg"` | — | — | ❌ | Flux Form 无 size 属性；通过 className 控制 |
| `defaultColumnRatio` | `columnCount` | `number` | ⚠️ | view.xml 用比例，Flux 用绝对列数 |
| `mode`（layout） | `mode` | `'normal' \| 'horizontal' \| 'inline'` | ✅ | |
| `labelAlign` | `labelAlign` | `'top' \| 'left' \| 'right'` | ✅ | |
| `labelWidth` | `labelWidth` | `string \| number` | ✅ | |
| — | `gap` | `number \| string` | ✅ | Flux 独有：字段间距 |
| `<layout>` | — | — | 🔄 | view.xml 的布局 DSL → flux-web.xlib 转换为 Flux form body 的 CSS Grid / Flex 布局 |
| `<cells>` | `body` | `BaseSchema[]` | ✅ | flux-web.xlib 将 cells 转换为 Flux 控件放入 body |
| — | `actions` | `BaseSchema[]` | ✅ | Flux 独有：显式操作按钮区域 |
| — | `actionsClassName` | `string` | ✅ | |
| — | `bodyClassName` | `string` | ✅ | |
| `<data>` | `data` | `Record<string, any>` | ✅ | 表单初始数据 |
| — | `statusPath` | `string` | ✅ | Flux 独有：发布 `{submitting, validating, dirty, valid, errorCount}` |
| — | `valuesPath` | `string` | ✅ | Flux 独有：发布表单值快照 |
| — | `autoInit` | `boolean` | ✅ | Flux 独有：自动初始化 form runtime |
| `<initApi>` | `loadAction` | `ActionSchema` | ⚠️ | view.xml `<initApi>` → Flux `loadAction: { action: "ajax", args: {...} }` |
| — | `autoLoad` | `boolean` | ✅ | 是否在 mount 时自动触发 loadAction |
| `<api url="@mutation:X__save">` | `submitAction` | `ActionSchema` | ⚠️ | view.xml `<api>` → Flux `submitAction: { action: "ajax", args: {...} }` |
| — | `onSubmitSuccess` | `ActionSchema` | ✅ | Flux 独有：提交成功后 action |
| — | `onSubmitError` | `ActionSchema` | ✅ | Flux 独有：提交失败后 action |
| — | `onValidateError` | `ActionSchema` | ✅ | Flux 独有：校验失败后 action |
| — | `initAction` | `ActionSchema` | ✅ | Flux 独有：form runtime 创建后触发 |
| `submitOnChange="true"` | `submitOnChange` | `boolean` | ✅ | **已支持**。Flux 防抖 300ms 自动提交 |
| `submitOnInit="true"` | — | — | ❌ | 可用 `initAction: { action: "submitForm" }` 替代 |
| `preventEnterSubmit="true"` | `preventEnterSubmit` | `boolean` | ✅ | |
| — | `autoFocus` | `boolean` | ✅ | Flux 独有：自动聚焦第一个控件 |
| — | `scrollToFirstError` | `boolean` | ✅ | 校验失败时滚动到第一个错误字段 |
| `resetAfterSubmit="true"` | — | — | ❌ | **建议补充**：`onSubmitSuccess: { action: "resetForm" }` 可实现，但无开关 |
| `static="true"` | `static` | `boolean \| string` | ✅ | 只读预览模式 |
| `<rules>` | `rules` | `FormCrossFieldRule[]` | ✅ | 跨字段规则（equalsField/notEqualsField） |
| `wrapWithPanel="true"` | — | — | 🔄 | Flux Form 无 panel 包装；用显式 actions 区域替代 |
| `submitText="保存"` | — | — | 🔄 | Flux Form 无内置提交按钮；手写 actions 区域 |
| `persistData="key"` | — | — | ❌ | **建议补充**：可用 Zustand persist 中间件 |
| `persistDataKeys` | — | — | ❌ | 同上 |
| `inheritData="true"` | — | — | 🔄 | Flux Form 硬编码 `scopePolicy: 'form'`（隔离），设计上不支持隐式继承 |
| `canAccessSuperData="true"` | — | — | 🔄 | 同上 |
| `promptPageLeave="true"` | — | — | ❌ | **建议补充**：未保存数据离开提示 |
| `interval="3000"` | — | — | 🔄 | Flux 通过 `data-source.interval` 实现轮询，不在 Form 层 |
| `checkInterval` / `initCheckInterval` | — | — | 🔄 | 同上 |
| `silentPolling="true"` | — | — | 🔄 | Flux 轮询的加载状态由 data-source 控制 |
| `initFetch="false"` | `autoLoad` | `boolean` | ⚠️ | 语义相反：`autoLoad: false` = `initFetch: false` |
| `initFetchOn="${expr}"` | — | — | 🔄 | 用 `autoLoad: "${expr}"` 替代 |
| `stopAutoRefreshWhen="${expr}"` | — | — | 🔄 | Flux `data-source.stopWhen: "${expr}"` |
| `<asyncApi>` | — | — | 🔄 | Flux 无长轮询 API；用 `data-source.interval` + `stopWhen` |
| `<initAsyncApi>` | — | — | 🔄 | 同上 |
| `<messages>` | — | — | 🔄 | Flux 在 action 级配置 `messages: { success, failed }` |
| `reload="gridName"` | — | — | 🔄 | Flux `submitAction.then: [{ action: "component:refresh", componentId: "..." }]` |
| `target="gridName"` | — | — | 🔄 | 同上 |
| `redirect="url"` | — | — | 🔄 | Flux `submitAction.then: [{ action: "navigate", args: { url } }]` |

---

## 4. CRUD

对应 view.xml `xview.xdef` 的 `<crud>` → Flux `CrudSchema`。

| view.xml crud/table 属性 | Flux CrudSchema | Flux 类型 | 状态 | 映射说明 |
|---|---|---|---|---|
| `name="main"` | `name` | `string` | ✅ | |
| `grid="list"` | `columns` | `CrudColumnSchema[]` | ⚠️ | view.xml 引用 grid 定义；Flux 直接内联 columns |
| `filterForm="query"` | `queryForm` | `CrudQueryFormConfig` | ⚠️ | view.xml 引用 form 定义；Flux 内联 queryForm 配置 |
| `asideFilterForm="asideFilter"` | — | — | 🔄 | Flux 用 Page 的 `aside` region 放置侧边筛选 |
| `<table><api url="@query:X__findPage">` | `loadAction` | `ReactiveActionSchema` | ⚠️ | view.xml api → Flux `loadAction: { action: "ajax", args: { url: "/r/X__findPage" } }`；Flux 的 loadAction 是 reaction 类型，dependsOn 变化时自动重新触发 |
| — | `source` | `SchemaValue` | ✅ | Flux 独有：直接表达式数据源（替代 loadAction） |
| `<table mode="cards">` | `listMode` | `'table' \| 'cards' \| 'list'` | ⚠️ | `mode` → `listMode` |
| — | `card` | `SchemaInput` | ✅ | cards 模式的行模板 |
| — | `item` | `SchemaInput` | ✅ | list 模式的行模板 |
| `<listActions>` | `listActions` | `SchemaInput` | ✅ | 工具栏操作按钮 |
| `<rowActions>` | — | — | ⚠️ | Flux 在 columns 中用 `buttons` region 定义行操作 |
| `<itemActions>` | — | — | ⚠️ | Flux 在 columns 中用 `buttons` region |
| — | `toolbar` | `SchemaInput` | ✅ | Flux 独有：顶部工具栏 |
| — | `footerToolbar` | `SchemaInput` | ✅ | Flux 独有：底部工具栏 |
| — | `toolbarLayout` | `CrudToolbarLayoutConfig` | ✅ | Flux 独有：工具栏布局配置 |
| `<table filterDefaultVisible="false">` | `filterTogglable` | `boolean \| CrudFilterToggleConfig` | ⚠️ | Flux 用 `filterTogglable: { defaultCollapsed: true }` |
| `<table filterTogglable="true">` | `filterTogglable` | `boolean \| CrudFilterToggleConfig` | ✅ | |
| `<table stopAutoRefreshWhenModalIsOpen="true">` | — | — | ❌ | **建议补充**；Flux 的 `polling.stopWhen` 可用 `${$surface.hasOpenSurface}` 近似 |
| `<table maxItemSelectionLength="5">` | `selection.maxSelectionLength` | `number` | ✅ | |
| `<table alwaysShowPagination="true">` | — | — | ❌ | **建议补充** |
| `<table initFetch="false">` | — | — | 🔄 | Flux CRUD 的 loadAction 是 reaction，自动触发；用 `control.dedup` 或条件表达式控制 |
| `<table autoFillHeight="true">` | — | — | ❌ | **高优先级建议补充**：337 个 _gen view 都在用 |
| `<table loadDataOnce="true">` | `clientMode.loadDataOnce` / `loadAllData` | `boolean` | ✅ | Flux 双入口：`clientMode.loadDataOnce` 或顶层 `loadAllData` |
| `<table sortable="true">` | column `sortable` | `boolean` | ✅ | 在每个 column 上控制 |
| `<table noOperations="true">` | — | — | 🔄 | 不生成操作列；在 flux-web.xlib 中控制 |
| `<table multiple="true">` | `selection.type` | `'checkbox' \| 'radio'` | ⚠️ | `multiple: true` → `selection: { type: 'checkbox' }` |
| `<table pickerMode="true">` | — | — | 🔄 | Flux 用 `selectionOwnership: 'scope'` + 外部读取 |
| `<table rowDrag="true">` | — | — | ⚠️ | CRUD 层不直接支持；内嵌 Table 支持 `draggable: true` |
| `<table colDrag="true">` | `columnSettings.draggable` | `boolean` | ✅ | 列拖拽 |
| `<table pager="xxx">` | `pagination` | `CrudPaginationConfig` | ⚠️ | Flux 分页配置更丰富 |
| `<table operationSize="200">` | — | — | ❌ | Flux 操作列宽度由 buttons region 内容决定 |
| `<defaultParams>` | `defaultParams` | `Record<string, SchemaValue>` | ✅ | |
| `<beforeTable>` / `<afterTable>` | — | — | ❌ | Flux 用 Page body 组合替代 |
| `<saveOrderApi>` | `quickSaveAction` | `ActionSchema` | ⚠️ | 行排序保存 |
| — | `selectionOwnership` | `'local' \| 'controlled' \| 'scope'` | ✅ | Flux 独有：选中状态归属 |
| — | `selectionStatePath` | `string` | ✅ | |
| — | `paginationOwnership` | `'local' \| 'controlled' \| 'scope'` | ✅ | |
| — | `sortOwnership` | `'local' \| 'controlled' \| 'scope'` | ✅ | |
| — | `filterOwnership` | `'local' \| 'controlled' \| 'scope'` | ✅ | |
| `rowKey`（默认 id） | `rowKey` | `string` | ✅ | 默认 `'id'` |
| — | `autoClearSelectionOnRefresh` | `boolean` | ✅ | 默认 `true` |
| — | `syncLocation` | `boolean` | ✅ | URL 同步查询条件 |
| — | `columnSettings` | `CrudColumnSettingsConfig` | ✅ | 列设置（显示/隐藏/排序） |
| — | `responsive` | `CrudResponsiveConfig` | ✅ | 响应式配置 |
| — | `autoGenerateQueryForm` | `boolean \| config` | ✅ | 自动生成查询表单 |
| — | `clientMode` | `CrudClientModeConfig` | ✅ | 客户端模式（loadDataOnce/filterOnAllColumns/matchFunc） |
| — | `polling` | `CrudPollingConfig` | ✅ | 轮询配置 `{ enabled, sourceId, stopWhen }` |
| — | `statusPath` | `string` | ✅ | 发布 `$crud` 状态摘要 |
| — | `migrationHints` | `CrudMigrationHints` | ✅ | AMIS 迁移辅助 |
| — | `onQuerySubmit` | `ActionSchema` | ✅ | 查询提交事件 |
| — | `onQueryReset` | `ActionSchema` | ✅ | 查询重置事件 |
| — | `onRowClick` | `ActionSchema` | ✅ | 行点击事件 |
| — | `onSelectionChange` | `ActionSchema` | ✅ | 选中变化事件 |
| — | `onRefresh` | `ActionSchema` | ✅ | 刷新事件 |
| — | `onError` | `ActionSchema` | ✅ | 加载失败事件 |
| — | `dataStatePath` | `string` | ✅ | 数据状态路径 |
| `<table checkOnItemClick="true">` | — | — | ❌ | **建议补充**：点击行切换选中 |
| — | `pageField` / `pageSizeField` | `string` | ✅ | 请求分页参数名（默认 `page`/`perPage`） |

---

## 5. Table（独立表格组件）

对应 view.xml `grid.xdef` 的部分属性 → Flux `TableSchema`。

| view.xml grid 属性 | Flux TableSchema | Flux 类型 | 状态 | 映射说明 |
|---|---|---|---|---|
| `<cols>` | `columns` | `TableColumnSchema[]` | ✅ | |
| `<api>` | `source` | `SchemaValue` | ⚠️ | view.xml api → Flux source 表达式 |
| `<initApi>` | — | — | 🔄 | Flux Table 不自行取数；由 CRUD 或 data-source 喂数据 |
| `affixHeader="true"` | `affixHeader` | `boolean` | ✅ | **已支持**：固定表头 |
| `checkOnItemClick="true"` | — | — | ❌ | **建议补充** |
| `selectable="true"` | `rowSelection` | `{ type: 'checkbox' \| 'radio', ... }` | ⚠️ | view.xml 布尔值 → Flux 配置对象 |
| `multiple="true"` | `rowSelection.type` | `'checkbox' \| 'radio'` | ⚠️ | `multiple: true` → `type: 'checkbox'` |
| `combineNum="3"` | `combineNum` | `number` | ✅ | **已支持**：合并前 N 列相同值的单元格 |
| `combineFromIndex="1"` | — | — | ❌ | **建议补充** |
| `sortable="false"`（grid级） | — | — | 🔄 | Flux 在 column 级控制 `sortable` |
| — | `stripe` | `boolean` | ✅ | Flux 独有：斑马纹 |
| — | `bordered` | `boolean` | ✅ | Flux 独有：边框 |
| — | `virtualThreshold` | `number` | ✅ | Flux 独有：虚拟滚动阈值 |
| — | `scrollHeight` | `number` | ✅ | Flux 独有：滚动高度 |
| — | `columnResize` | `boolean` | ✅ | Flux 独有：列宽调整 |
| `<prefixRow>` | `prefixRow` | `TableSummaryRow` | ⚠️ | Flux 结构化：`{ cells: [{ column, value, align }] }` |
| `<affixRow>` | `affixRow` | `TableSummaryRow` | ⚠️ | 同上 |
| `<affixRowClassName>` | — | — | ❌ | |
| `<rowClassName>` | — | — | ❌ | **高优先级建议补充**：条件行样式 |
| `<rowClassNameExpr>` | — | — | ❌ | **高优先级建议补充**：表达式行样式 |
| `rowDrag="true"` | `draggable` | `boolean` | ✅ | **已支持**：行拖拽排序 |
| — | `orderField` | `string` | ✅ | Flux 独有：排序持久化字段 |
| — | `orderOwnership` | `'local' \| 'controlled' \| 'scope'` | ✅ | |
| `colDrag="true"` | `columnSettings.draggable` | `boolean` | ✅ | 列拖拽排序 |
| — | `multiSort` | `boolean` | ✅ | 多列排序 |
| — | `rowChildrenField` | `string` | ✅ | 树形表格子节点字段 |
| — | `expandable` | `{ expandedRowKeys, expandRowByClick, expandedRow }` | ✅ | 展开行配置 |
| — | `pagination` | `PaginationConfig` | ✅ | 分页配置 |
| — | `header` / `footer` / `empty` | `SchemaInput \| string` | ✅ | 表头/表尾/空状态 |
| — | `loading` / `loadingContent` | `boolean` / `SchemaInput` | ✅ | 加载状态 |
| — | `responsive` | `TableResponsiveConfig` | ✅ | 响应式 |
| — | `onSortChange` / `onFilterChange` / `onPageChange` / `onSelectionChange` / `onRefresh` | `BaseSchema` | ✅ | 交互事件 |
| `<saveOrderApi>` | `quickSaveAction` / `quickSaveItemAction` | `ActionSchema` | ✅ | |
| `<itemCheckableOn>` | `rowSelection.checkableWhen` | `string` | ✅ | 行可勾选条件 |
| `<stopAutoRefreshWhen>` | — | — | 🔄 | Flux 通过 data-source 层控制 |

---

## 6. 表格列（Table Column）

对应 view.xml `grid.xdef` 的 `<col>` → Flux `TableColumnSchema` / `CrudColumnSchema`。

| view.xml col 属性 | Flux TableColumnSchema | Flux 类型 | 状态 | 映射说明 |
|---|---|---|---|---|
| `id="fieldName"` | `name` | `string` | ✅ | |
| `label="显示名"` | `label` | `string` | ✅ | |
| `sortable="true"` | `sortable` | `boolean` | ✅ | |
| `hidden="true"` | `hidden` | `boolean` | ✅ | |
| `fixed="left/right"` | `fixed` | `'left' \| 'right'` | ✅ | |
| `align="center"` | `align` | `'left' \| 'center' \| 'right'` | ✅ | |
| `width="200"` | `width` | `number \| string` | ✅ | |
| `mandatory="true"` | — | — | 🔄 | Flux 列级无 mandatory；在控件层控制 |
| `readonly="true"` | — | — | 🔄 | 同上 |
| `breakpoint="md"` | — | — | ❌ | |
| `ui:number="true"` | — | — | 🔄 | Flux 列由 `cell` region 内容决定渲染 |
| `groupName="基础信息"` | — | — | ❌ | **建议补充**：列分组标题 |
| — | `toggled` | `boolean` | ✅ | Flux 独有：默认显示/隐藏（配合 columnSettings） |
| — | `resizable` | `boolean` | ✅ | Flux 独有：可调整列宽 |
| — | `minWidth` / `maxWidth` | `number` | ✅ | |
| — | `children` | `TableColumnSchema[]` | ✅ | Flux 独有：多级表头 |
| — | `copyable` | `boolean` | ✅ | Flux 独有：可复制 |
| — | `popOver` | `TableColumnPopOverConfig` | ✅ | Flux 独有：弹出内容 |
| — | `searchable` | `boolean \| SchemaInput` | ✅ | Flux 独有：可搜索 |
| — | `filterable` | `boolean \| config` | ✅ | Flux 独有：可筛选 |
| — | `filterOptions` | `TableColumnFilterOption[]` | ✅ | |
| — | `quickEdit` | `boolean \| config` | ✅ | Flux 独有：快捷编辑 |
| — | `buttons` | `BaseSchema[]` | ✅ | Flux 独有：行操作按钮 |
| — | `cell` / `labelRegionKey` / `buttonsRegionKey` 等 | `SchemaInput` | ✅ | Flux 独有：region 参数化定制 |

---

## 7. 按钮 / 操作（Button / Action）

对应 view.xml `action.xdef` 的 `<action>` → Flux `ButtonSchema` + `onClick: ActionSchema`。

### 7a. 按钮外观属性

| view.xml action 属性 | Flux ButtonSchema | Flux 类型 | 状态 | 映射说明 |
|---|---|---|---|---|
| `id` | `id` | `string` | ✅ | |
| `label` | `label` | `string` | ✅ | |
| `level="primary"` | `variant` | `'default' \| 'destructive' \| 'outline' \| 'secondary' \| 'ghost' \| 'link'` | ⚠️ | 映射：primary→default, danger→destructive, link→link, success→default+className |
| `icon="fa fa-plus"` | `icon` | `string` | ⚠️ | AMIS: `fa fa-plus` → Flux: `plus`（Lucide 图标名，kebab-case） |
| `rightIcon` | `rightIcon` | `string` | ✅ | |
| `size="sm"` | `size` | `'default' \| 'xs' \| 'sm' \| 'lg' \| 'icon' \| 'icon-xs' \| 'icon-sm' \| 'icon-lg'` | ✅ | |
| `block="true"` | `block` | `boolean` | ✅ | **已支持**：全宽按钮 |
| `active="true"` | `active` | `boolean \| string` | ✅ | **已支持**：激活/按下状态 |
| `disabled` (隐含通过 disabledOn) | `disabled` | `boolean \| string` | ✅ | |
| `tooltip="提示"` | `tooltip` | `string` | ✅ | **已支持** |
| `disabledTip="禁用提示"` | `disabledTip` | `string` | ✅ | **已支持** |
| `tooltipPlacement="top"` | — | — | ❌ | **建议补充** |
| `iconOnly="true"` | `size: 'icon'` | — | ⚠️ | Flux 用 `size: 'icon' \| 'icon-sm'` 表达纯图标按钮 |
| `iconClassName` | — | — | ❌ | 用 className 控制 |
| `batch="true"` | — | — | 🔄 | Flux 在 CRUD 层控制：batch action 显示在工具栏 |
| `hotKey="ctrl+s"` | — | — | ❌ | **建议补充**：键盘快捷键 |
| `countDown="3"` | — | — | ❌ | **建议补充**：按钮倒计时 |
| `countDownTpl` | — | — | ❌ | 同上 |
| `required="fieldA,fieldB"` | — | — | 🔄 | Flux 在 action 层用 `when: "${fieldA && fieldB}"` 控制 |
| `onClick="js代码"` | — | — | 🔄 | Flux 不允许 JS 字符串；用 `onClick: { action: ... }` 替代 |
| `<body>` | — | — | 🔄 | Flux 按钮内容固定为 label+icon；自定义内容用其他组件 |

### 7b. 按钮行为属性 → Flux Action Algebra

| view.xml action 行为 | Flux onClick ActionSchema | 状态 | Flux 写法示例 |
|---|---|---|---|
| `<api url="@mutation:X__save?id=$id">` (actionType=ajax) | `{ action: "ajax", args: { url: "/r/X__save", method: "post", data: { id: "${$slot.record.id}" } } }` | ✅ | URL 从 `@mutation:` 转换为 `/r/`；`$id` 转为 `${$slot.record.id}` |
| `<dialog page="add"/>` (actionType=dialog) | `{ action: "openDialog", args: { title: "...", body: [<Flux form>] } }` | ✅ | page 引用内联为 Flux body |
| `<drawer page="edit"/>` (actionType=drawer) | `{ action: "openDrawer", args: { side: "right", body: [<Flux form>] } }` | ✅ | |
| `actionType="submit"` | `{ action: "submitForm" }` | ✅ | |
| `actionType="cancel"` | `{ action: "closeSurface" }` | ✅ | |
| `actionType="url"` + `url="http://..."` | `{ action: "navigate", args: { url: "http://..." } }` | ✅ | |
| `actionType="link"` + `link="/page"` | `{ action: "navigate", args: { url: "/page" } }` | ✅ | |
| `actionType="reload"` + `reload="gridName"` | `{ action: "component:refresh", componentId: "gridName" }` | ✅ | |
| `actionType="copy"` + `copyFormat` | — | ❌ | **建议补充**：可用 `setValue` 近似 |
| `<confirmText>确认？</confirmText>` | `confirmText: "确认？"` | ✅ | **直接映射**，位置在 action 对象上 |
| `<messages><success>成功</success>` | `messages: { success: "成功" }` | ✅ | **直接映射** |
| `<messages><failed>失败</failed>` | `messages: { failed: "失败" }` | ✅ | |
| `disabledOn="${!canEdit}"` | `disabled: "${!canEdit}"` | ✅ | 去 On 后缀 |
| `visibleOn="${isAdmin}"` | `visible: "${isAdmin}"` | ✅ | |
| `close="true"` | `then: [{ action: "closeSurface" }]` | 🔄 | Flux 用 then 链显式关闭 |
| `redirect="/list"` | `then: [{ action: "navigate", args: { url: "/list" } }]` | 🔄 | |
| `target="otherGrid"` | `then: [{ action: "component:refresh", componentId: "otherGrid" }]` | 🔄 | |
| `<feedback page="result"/>` | `then: [{ action: "openDialog", args: { body: [...] } }]` | 🔄 | |
| `<onEvent>` | — | 🔄 | Flux 用统一事件字段（onClick/onChange 等），不支持原始事件 JSON |
| `actionGroup` | `type: "dropdown-button"` | ✅ | Flux 有 DropdownButtonSchema |

### 7c. Flux 独有的 Action 能力（view.xml 无法表达）

| Flux 能力 | Flux 语法 | 说明 |
|---|---|---|
| 动作链 | `then: [{ action: "ajax", ... }, { action: "component:refresh", ... }]` | 顺序执行 |
| 失败处理 | `onError: { action: "showToast", args: { level: "error", message: "${error.message}" } }` | 失败分支 |
| 完成处理 | `onSettled: { action: "setValue", args: { path: "submitting", value: false } }` | 总是执行 |
| 并行执行 | `parallel: [{ action: "ajax", ... }, { action: "ajax", ... }]` | 并行+聚合 |
| 守卫 | `when: "${shouldRun}"` | 条件执行 |
| 重试 | `control: { retry: { count: 3, delay: 1000 } }` | 自动重试 |
| 超时 | `control: { timeout: 5000 }` | 超时控制 |
| 防抖 | `control: { debounce: 300 }` | 防抖 |
| 组件方法调用 | `{ action: "component:setValue", componentId: "myForm", args: { path: "x", value: 1 } }` | 跨组件写值 |
| 命名空间方法 | `{ action: "dict:getOptions", args: { code: "gender" } }` | 模块化能力调用 |

---

## 8. 页面（Page）

对应 view.xml `xview.xdef` 的 `UiPageModel` → Flux `PageSchema`。

| view.xml page 属性 | Flux PageSchema | Flux 类型 | 状态 | 映射说明 |
|---|---|---|---|---|
| `title` | `title` | `string` (value-or-region) | ✅ | |
| `subTitle` | `subTitle` | `string` | ✅ | |
| `<remark>` | `remark` | `string` | ✅ | |
| `<data>` | `data` | `SchemaValue` | ✅ | |
| — | `statusPath` | `string` | ✅ | Flux 独有：发布 `{ refreshTick }` |
| `<initApi>` | — | — | 🔄 | Flux 用 Page body 内的 `data-source` 或子组件的 `loadAction` |
| `initFetch` | — | — | 🔄 | 同上 |
| `interval` / `silentPolling` | — | — | 🔄 | Flux 用 `data-source.interval` |
| `initFetchOn` | — | — | 🔄 | |
| `<stopAutoRefreshWhen>` | — | — | 🔄 | Flux `data-source.stopWhen` |
| `className` | `bodyClassName` | `string` | ⚠️ | |
| `headerClassName` | `headerClassName` | `string` | ✅ | |
| — | `body` | `BaseSchema[]` | ✅ | 页面主体 |
| — | `header` | `BaseSchema[]` | ✅ | Flux 独有：页头区域 |
| — | `footer` | `BaseSchema[]` | ✅ | Flux 独有：页脚区域 |
| — | `aside` | `BaseSchema[]` | ✅ | Flux 独有：侧边栏区域 |
| — | `asidePosition` | `'left' \| 'right'` | ✅ | |
| — | `asideClassName` | `string` | ✅ | |
| `asideResizor="true"` | — | — | ❌ | **建议补充**：可调整侧边栏宽度 |
| `asideMinWidth` / `asideMaxWidth` | — | — | ❌ | **建议补充** |
| `asideSticky="true"` | — | — | ❌ | **建议补充** |
| — | `modalContainer` | `string` | ✅ | Flux 独有：模态容器引用 |
| — | `footerClassName` / `toolbarClassName` | `string` | ✅ | |

---

## 9. 弹窗 / 抽屉（Dialog / Drawer）

对应 view.xml `action.xdef` 的 `<dialog>` / `<drawer>` → Flux `DialogSchema` / `DrawerSchema`。

| view.xml dialog 属性 | Flux DialogSchema / DrawerSchema | Flux 类型 | 状态 | 映射说明 |
|---|---|---|---|---|
| `page="add"` | `body: BaseSchema[]` | — | ⚠️ | view.xml 引用 view page → Flux 直接内联 body |
| `title` | `title` | `string` (value-or-region) | ✅ | |
| `size="lg"` | `size` | `'xs' \| 'sm' \| 'md' \| 'lg' \| 'xl' \| 'full'` | ✅ | Flux 多了 `xs` 和 `xl` |
| `width` / `height` | `width` / `height` | `number \| string` | ✅ | |
| `closeOnEsc="true"` | `closeOnEsc` | `boolean` | ✅ | **已支持** |
| `closeOnOutside="true"` | `closeOnOutsideClick` (Dialog) / `closeOnOutside` (Drawer) | `boolean` | ✅ | **已支持**（注意命名不一致） |
| `showCloseButton="true"` | `showCloseButton` | `boolean` | ✅ | **已支持** |
| `noActions="true"` | — | — | ⚠️ | Flux `actions: []` 空数组 |
| `<data>` | `data` | `SchemaValue` | ✅ | |
| `<actions>` | `actions` | `BaseSchema[]` | ✅ | |
| — | `confirm` | `boolean \| string` | ✅ | Flux 独有：自动生成取消/确认按钮 |
| — | `onConfirm` / `onOpen` / `onClose` | `ActionSchema` | ✅ | Flux 独有：生命周期事件 |
| — | `open` / `defaultOpen` | `boolean` | ✅ | Flux 独有：受控/非受控打开状态 |
| — | `showMask` | `boolean` | ✅ | Flux 独有：遮罩层 |
| — | `container` | `string` | ✅ | Flux 独有：容器引用 |
| — | `header` / `footer` | `BaseSchema[]` | ✅ | |
| Drawer: `side` / `placement` | `side` | `'left' \| 'right' \| 'top' \| 'bottom'` | ✅ | |
| — | `resizable` (Drawer) | `boolean` | ✅ | Flux 独有：抽屉可调整大小 |
| — | `statusPath` | `string` | ✅ | |

---

## 10. 标签页（Tabs）

对应 view.xml `xview.xdef` 的 `<tabs>` / `<tab>` → Flux `TabsSchema` / `TabsItemSchema`。

| view.xml tabs 属性 | Flux TabsSchema | Flux 类型 | 状态 | 映射说明 |
|---|---|---|---|---|
| `tabsMode="card"` | `tabsMode` | `'' \| 'line' \| 'card' \| 'radio' \| 'vertical' \| 'chrome' \| 'simple' \| 'strong' \| 'tiled' \| 'sidebar'` | ✅ | **已支持**，且模式更丰富 |
| `tabsClassName` | `contentClassName` / `toolbarClassName` | `string` | ⚠️ | |
| `closeable="true"` | — | — | ❌ | **建议补充**：可关闭标签 |
| `draggable="true"` | — | — | ❌ | **建议补充**：可拖拽排序标签 |
| — | `orientation` | `'horizontal' \| 'vertical'` | ✅ | Flux 独有 |
| — | `variant` | `'default' \| 'line'` | ✅ | Flux 独有 |
| — | `sidePosition` | `'left' \| 'right'` | ✅ | Flux 独有 |
| — | `toolbar` | `BaseSchema \| BaseSchema[]` | ✅ | Flux 独有：工具栏 |
| — | `valueOwnership` | `'local' \| 'controlled' \| 'scope'` | ✅ | Flux 独有 |
| — | `valueStatePath` | `string` | ✅ | |
| — | `statusPath` | `string` | ✅ | |

### Tab Item

| view.xml tab 属性 | Flux TabsItemSchema | Flux 类型 | 状态 | 映射说明 |
|---|---|---|---|---|
| `name` | `key` / `value` | `string \| number` | ✅ | |
| `title` | `title` / `label` | `string` | ✅ | |
| `icon` | `icon` | `string` | ✅ | |
| `hash` | — | — | ❌ | |
| `disabled="true"` | `disabled` | `boolean \| string` | ✅ | 接受表达式 |
| `mountOnEnter="true"` | `mountOnEnter` | `boolean` | ✅ | **已支持**：延迟挂载 |
| `unmountOnExit="true"` | `unmountOnExit` | `boolean` | ✅ | **已支持**：退出时卸载 |
| `reload="true"` | — | — | 🔄 | Flux 通过 `onMount: { action: "component:refresh" }` 实现 |
| `lazyLoad="true"` | `mountOnEnter` | `boolean` | ⚠️ | 近似映射 |
| `page="somePage"` | `bodyRegionKey` | — | ⚠️ | view.xml 引用 view page → Flux 内联 body |
| — | `badge` | `string \| number` | ✅ | Flux 独有：徽章 |
| `iconPosition` | — | — | ❌ | |
| `className` | — | — | ❌ | |

---

## 11. 向导（Wizard）

对应 view.xml `xview.xdef` 的 `<wizard>` / `<step>` → Flux `WizardSchema` / `WizardStepSchema`。

| view.xml wizard 属性 | Flux WizardSchema | Flux 类型 | 状态 | 映射说明 |
|---|---|---|---|---|
| `mode` | — | — | ❌ | |
| `className` | `className` | `string` | ✅ | |
| `actionClassName` | — | — | ❌ | |
| `actionPrevLabel="上一步"` | — | — | 🔄 | Flux Wizard 自动生成 Prev/Next 按钮；可通过 step `actions` region 自定义 |
| `actionNextLabel="下一步"` | — | — | 🔄 | 同上 |
| `actionNextSaveLabel` | — | — | 🔄 | 同上 |
| `actionFinishLabel="完成"` | — | — | 🔄 | 同上 |
| `initFetch="true"` | — | — | 🔄 | |
| `<api>` | — | — | 🔄 | Flux Wizard 无 api；用 step 的 `beforeEnter` action |
| `<initApi>` | — | — | 🔄 | |
| `reload` / `redirect` / `target` | — | — | 🔄 | Flux 用 `onComplete: { action: ... }` |
| `startStep="2"` | `value` / `defaultValue` | `string \| number` | ✅ | |
| — | `steps` | `WizardStepSchema[]` | ✅ | Flux 独有：结构化步骤列表 |
| — | `statusPath` | `string` | ✅ | Flux 独有：发布 `{currentStepKey, currentStepIndex, stepCount, canGoNext, canGoPrev, committing, validating, lastCommitStatus}` |
| — | `linear` | `boolean` | ✅ | Flux 独有：线性模式（默认 true） |
| — | `allowStepJump` | `boolean` | ✅ | Flux 独有：允许跳步 |
| `mountOnEnter` | `mountOnEnter` | `boolean` | ✅ | **已支持** |
| `unmountOnExit` | `unmountOnExit` | `boolean` | ✅ | **已支持** |
| — | `onChange` | `ActionSchema` | ✅ | Flux 独有 |
| — | `onStepCommit` | `ActionSchema` | ✅ | Flux 独有：步骤提交事件 |
| — | `onComplete` | `ActionSchema` | ✅ | Flux 独有：完成事件 |
| — | `onStepError` | `ActionSchema` | ✅ | Flux 独有：步骤错误事件 |

### Wizard Step

| view.xml step 属性 | Flux WizardStepSchema | Flux 类型 | 状态 | 映射说明 |
|---|---|---|---|---|
| `name` | `key` | `string \| number` | ✅ | |
| `page="formName"` | `body` | `SchemaInput` | ⚠️ | view.xml 引用 form → Flux 内联 body |
| `title` | `title` | `SchemaValue \| SchemaInput` | ✅ | |
| — | `description` | `SchemaValue \| SchemaInput` | ✅ | Flux 独有：步骤描述 |
| — | `actions` | `SchemaInput` | ✅ | Flux 独有：步骤级操作按钮（替代默认 Prev/Next） |
| — | `visible` | `SchemaValue` | ✅ | Flux 独有：步骤可见性表达式 |
| — | `disabled` | `SchemaValue` | ✅ | Flux 独有：步骤禁用表达式 |
| — | `beforeEnter` | `ActionSchema` | ✅ | Flux 独有：进入前 action（可用于数据校验或预加载） |
| — | `beforeLeave` | `ActionSchema` | ✅ | Flux 独有：离开前 action（可用于拦截未保存数据） |

---

## 12. API 配置

对应 view.xml `api.xdef` → Flux `ApiSchema`（嵌入在 `args` 中）。

| view.xml api 属性 | Flux ApiSchema（args 内） | Flux 类型 | 状态 | 映射说明 |
|---|---|---|---|---|
| `url="@query:X__findPage"` | `url: "/r/X__findPage"` | `string` | ⚠️ | `@query:` / `@mutation:` → `/r/` 前缀 |
| `method="post"` | `method` | `string` | ✅ | |
| `dataType="form/form-data"` | — | — | ❌ | Flux fetcher 由宿主决定 Content-Type |
| `cache="3000"` | — | — | 🔄 | Flux 用 `control: { cacheTTL: 3000 }` |
| `responseType="blob"` | — | — | ❌ | **建议补充**：文件下载 |
| `withFormData="true"` | `includeScope: "*"` | `string` | ⚠️ | view.xml 发送全部表单字段 → Flux 发送全部 scope 数据 |
| `gql:selection="{@pageSelection}"` | `selection: "id,name,status"` | `string` | ✅ | Flux 直接写字段列表 |
| `<headers>` | `headers` | `Record<string, string>` | ✅ | |
| `<data>` | `data` | `SchemaValue` | ✅ | |
| `<sendOn="${expr}">` | — | — | 🔄 | Flux data-source 的 `sendOn` 控制；action 用 `when` 控制 |
| `<trackExpression>` | — | — | 🔄 | Flux data-source 的 `dependsOn` |
| `<responseData>` | — | — | ❌ | 用 `responseAdaptor` 替代 |
| `<requestAdaptor>` | `requestAdaptor` | `string` | ✅ | Flux formula 编译（沙箱），非 new Function |
| `<adaptor>` | `responseAdaptor` | `string` | ✅ | 同上 |
| `autoRefresh="true"` | — | — | 🔄 | Flux CRUD 的 loadAction 是 reaction，自动响应 scope 变化 |
| `replaceData="true"` | — | — | 🔄 | Flux CRUD 总是替换数据 |
| `convertKeyToPath="false"` | — | — | ❌ | |

---

## 13. 表单字段控件对照（control.xlib → flux-control.xlib）

以下展示 control.xlib 中的关键 domain→control 映射，以及 Flux 对应控件。

| domain/stdDomain | view.xml control.xlib 输出（AMIS） | Flux flux-control.xlib 输出 | 状态 | 差异说明 |
|---|---|---|---|---|
| `roleId` | `{ type: "picker", x:extends: "picker.page.yaml" }` | `{ type: "picker", pickerPage: "...", valueField: "roleId" }` | ⚠️ | Flux picker 通过 `pickerPage` 引用，body 内联 |
| `userId` | `{ type: "picker", ... }` | `{ type: "picker", ... }` | ⚠️ | 同上 |
| `deptId` | `{ type: "input-tree", source: "@query:NopAuthDept__findList" }` | `{ type: "tree-select", childrenSource: { url: "/r/NopAuthDept__findList" } }` | ⚠️ | `input-tree` → `tree-select` |
| `enum` + `dict` | `{ type: "select", source: "@dict:dictName" }` | `{ type: "select", dict: "dictName" }` | ✅ | Flux 通过 `env.loadDict("dictName")` 加载 |
| `boolFlag` | `{ type: "switch", trueValue: 1, falseValue: 0 }` | `{ type: "switch", trueValue: 1, falseValue: 0 }` | ✅ | 直接映射 |
| `string` (edit) | `{ type: "input-text", ... }` | `{ type: "input-text", ... }` | ✅ | 直接映射 |
| `string` (query) | `{ type: "input-text", ... }` | `{ type: "input-text", ... }` | ✅ | |
| `int` / `long` (edit) | `{ type: "input-number", ... }` | `{ type: "input-number", ... }` | ✅ | |
| `decimal` (edit) | `{ type: "input-number", precision: N }` | `{ type: "input-number", precision: N }` | ✅ | |
| `date` (edit) | `{ type: "input-date" }` | `{ type: "input-date" }` | ✅ | |
| `date` (query) | `{ type: "input-date-range" }` | `{ type: "date-range" }` | ⚠️ | `input-date-range` → `date-range` |
| `datetime` (edit) | `{ type: "input-datetime" }` | `{ type: "input-datetime" }` | ✅ | |
| `datetime` (query) | `{ type: "input-datetime-range" }` | `{ type: "date-range", rangeKind: "datetime" }` | ⚠️ | |
| `time` (edit) | `{ type: "input-time" }` | `{ type: "input-time" }` | ✅ | |
| `to-one` (relation) | `{ type: "picker", ... }` | `{ type: "picker", ... }` | ✅ | |
| `to-many` (relation) | `{ type: "input-table", ... }` | `{ type: "input-table", ... }` 或 `{ type: "array-field", ... }` | ⚠️ | Flux 有 `input-table` 和 `array-field` 两种选择 |
| `view-any` (fallback) | `{ type: "static" }` | `{ type: "text" }` 或 `{ type: "detail-field" }` | ⚠️ | `static` → `text` 或 `detail-field` |
| `multiline` (string) | `{ type: "textarea" }` | `{ type: "textarea" }` | ✅ | |

---

## 14. 补充优先级汇总

### 高优先级（ERP 全部 CRUD 页面需要）

| 功能 | 影响范围 | 建议实现方式 |
|------|----------|-------------|
| `autoFillHeight` | 337 个 CRUD 页面 | Flux TableSchema 增加 `autoFillHeight: boolean` |
| `rowClassName` / `rowClassNameExpr` | 条件行样式（超期/预警/拒绝高亮） | Flux TableSchema 增加 `rowClassName: string` + `rowClassNameExpr: string` |
| `promptPageLeave` | 凭证录入/复杂表单 | Flux FormSchema 增加 `promptPageLeave: boolean` |

### 中优先级

| 功能 | 场景 | 建议实现方式 |
|------|------|-------------|
| `checkOnItemClick` | 批量选择 | Flux CrudSchema/TableSchema 增加 `checkOnItemClick: boolean` |
| `resetAfterSubmit` | 连续录入 | Flux FormSchema 增加 `resetAfterSubmit: boolean` |
| `asideResizor` | 宽筛选面板 | Flux PageSchema 增加可调整 aside |
| `hotKey` | 无障碍+高效操作 | Flux ButtonSchema 增加 `hotKey: string` |
| `persistData` | 多步录入 | Flux FormSchema 增加 Zustand persist 集成 |
| `stopAutoRefreshWhenModalIsOpen` | 看板+编辑弹窗 | Flux CrudPollingConfig 增加 `pauseWhenSurfaceOpen: boolean` |
| `responseType="blob"` | 报表下载 | Flux ApiSchema 增加 `responseType: 'blob'` |

### 低优先级

| 功能 | 场景 |
|------|------|
| `tooltipPlacement` | tooltip 位置 |
| `countDown` / `countDownTpl` | SMS 验证码倒计时 |
| `alwaysShowPagination` | 分页栏始终显示 |
| `tabs closeable` | 可关闭标签页 |
| `tabs draggable` | 可拖拽标签排序 |
| `combineFromIndex` | 从第 N 列开始合并 |
