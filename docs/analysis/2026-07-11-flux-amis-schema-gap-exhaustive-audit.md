# amis v6.13.1 ↔ Flux Schema 完整缺口审计

> 日期：2026-07-11
> 数据来源：amis schema.json (v6.13.1)、Flux schemas.ts (nop-chaos-flux)、ERP property mapping
> 目标：识别所有 amis 配置项的 Flux 对应状态，最大化 amis→Flux 简单配置映射
> 状态标记：✅ 已支持 | 🔴 缺失-建议新增 | 🟡 缺失-已有替代 | ⛔ 不采纳(确认) | 🔵 DESIGN-ACK-NOT-IMPL

---

## 审计方法

1. 以 amis schema.json 中每个组件的 properties 声明为权威列表
2. 逐一对照 Flux schemas.ts 中对应组件的 interface 定义
3. 参考 ERP property mapping 文档确认实际使用情况
4. 参考 Flux design.md 决策表确认已拒绝项
5. 对"已有设计计划覆盖"的项（如 `autoFillHeight`、`toggleOnRowClick`、`tooltipPlacement`、`countDown`、`asideResizable`、`responseType`）标注为 **计划中**

---

## 1. CRUD (amis CRUD + CRUD2 → Flux CrudSchema)

amis CRUD 有两个版本：Legacy CRUD（`type: 'crud'`，schema.json 行 9254-9367）和 CRUD2（`type: 'crud2'`，schema.json 行 11214-11557）。ERP 使用 Legacy CRUD。

### 1.1 数据加载与请求

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `api` | `AMISApi` | ✅ | `loadAction` / `source` | `loadAction: { action: "ajax", args: {...} }` | no (action 包装) |
| `source` | `string` | ✅ | `source` | 直接映射 | yes |
| `initFetch` | `boolean` | 🟡 | `loadAction`(reaction 自动触发) | Flux reaction 模型自动触发 | no (范式差异) |
| `initFetchOn` | `Expression` | 🟡 | `loadAction` reaction + `when` | `when: "${expr}"` 控制 | no |
| `interval` | `number` | ✅ | `polling.enabled` / data-source `interval` | `polling: { enabled: true }` + data-source `interval` | no (下沉到 data-source) |
| `silentPolling` | `boolean` | ✅ | data-source 层控制 | data-source `silent` | no (下沉) |
| `stopAutoRefreshWhen` | `Expression` | ✅ | `polling.stopWhen` | `polling: { stopWhen: "${expr}" }` | yes (属性重命名) |
| `stopAutoRefreshWhenModalIsOpen` | `boolean` | 🟡 | `polling.stopWhen` | `polling.stopWhen: "${$surface.hasOpenSurface}"` | no (表达式替代) |
| `deferApi` | `AMISApi` | 🔴 | — | 树表懒加载 API；Flux `useTableTree` 无 per-node lazy fetch。归 candidate future（B7 backlog） | no |
| `syncResponse2Query` | `boolean` | ⛔ | — | 低价值，Flux CRUD 总是替换数据 | — |

### 1.2 查询区

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `filter` | `FormBase` | ✅ | `queryForm` | `queryForm: { body: [...] }` 内联 | no (结构转换) |
| `filterTogglable` | `boolean \| object` | ✅ | `filterTogglable` | 直接映射 | yes |
| `filterDefaultVisible` | `boolean` | ✅ | `filterTogglable.defaultCollapsed` | `filterTogglable: { defaultCollapsed: !filterDefaultVisible }` | yes (语义反转) |
| `autoGenerateFilter` | `boolean \| object` | 🔵 | `autoGenerateQueryForm` | schema 已声明，runtime 未实现 | yes (属性重命名) |
| `parsePrimitiveQuery` | `boolean \| object` | ✅ | `queryForm.parsePrimitiveQuery` | 直接映射 | yes |

### 1.3 分页与排序

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `perPage` | `number` | ✅ | `pagination.pageSize` | `pagination: { pageSize: N }` | yes |
| `perPageAvailable` | `number[]` | ✅ | `pagination.pageSizeOptions` | 直接映射 | yes |
| `pageField` | `string` | ✅ | `pageField` | 直接映射 | yes |
| `perPageField` | `string` | ✅ | `pageSizeField` | 属性重命名 | yes |
| `totalField` | `string` | 🔴 | — | 低优先级；Flux `normalizeCrudSourceValue` 已处理 `total`/`count`/`rows.length` 回退。如需可加 `totalField?: string`（缺省 `total`） | yes |
| `pageDirectionField` | `string` | ⛔ | — | Flux 无 cursor-based 分页；低价值 | — |
| `alwaysShowPagination` | `boolean` | 🔴 | — | **建议新增** `pagination.alwaysShow?: boolean`。Flux 当前 totalPages ≤ 1 时隐藏分页栏。归 Non-Blocking Follow-up | yes |
| `autoJumpToTopOnPagerChange` | `boolean` | 🔴 | — | **建议新增** `autoJumpToTopOnPagerChange?: boolean`。翻页时 `window.scrollTo(0, 0)` 或 scroll container 顶部 | yes |
| `orderBy` | `string` | ✅ | scope sort state 初始值 | 通过 `sortStatePath` 初始值设置 | no |
| `orderDir` | `'asc'\|'desc'` | ✅ | scope sort state 初始值 | 同上 | no |

### 1.4 选择

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `selectable` | `boolean` | ✅ | `selection.type` | `selection: { type: 'checkbox' }` | no (结构转换) |
| `multiple` | `boolean` | ✅ | `selection.type` | `checkbox` vs `radio` | no |
| `keepItemSelectionOnPageChange` | `boolean` | ✅ | `selection.keepOnPageChange` | 直接映射 | yes |
| `maxKeepItemSelectionLength` | `number` | ⛔ | — | Flux 已删字段（不采纳），超限策略归 feature 级设计 | — |
| `labelTpl` | `Template` | 🔴 | — | **低优先级建议新增**。keepItemSelectionOnPageChange 时已选项的展示文案模板。Flux 可加 `selection.labelTpl?: string` | yes |
| `checkOnItemClick` | `boolean` | 🔴 **计划中** | `selection.toggleOnRowClick` | 见 design doc §1.3，已在 plan Phase 2 | yes |

### 1.5 工具栏与操作

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `headerToolbar` | `array` | ✅ | `toolbar` | Flux region | no (结构转换) |
| `footerToolbar` | `array` | ✅ | `footerToolbar` | 直接映射 | yes |
| `toolbar` | `SchemaCollection` | 🟡 | Page `header` region | Flux 用 Page 的 `header` | no |
| `toolbarInline` | `boolean` | ⛔ | — | Flux toolbar 已用 flex 布局；低价值 | — |
| `bulkActions` | `array` | 🟡 | `listActions` | Flux 统一到 `listActions`，selection-aware | no (结构转换) |
| `itemActions` | `array` | 🟡 | `columns[].buttons` | Flux 在列 buttons 中声明行操作 | no |
| `hideQuickSaveBtn` | `boolean` | 🔴 | — | **低优先级**。Flux quick edit 入口由 column `quickEdit` 控制，如需全局隐藏开关可加 `hideQuickSaveBtn?: boolean` | yes |

### 1.6 数据处理模式

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `loadDataOnce` | `boolean` | ✅ | `clientMode.loadDataOnce` / `loadAllData` | 两个入口对应两种 source 路径 | yes |
| `loadDataOnceFetchOnFilter` | `boolean` | ✅ | `clientMode.fetchOnFilter` | 直接映射 | yes |
| `matchFunc` | `string` | 🔵 | `clientMode.matchFunc` | schema 已声明，runtime 未实现 | yes |
| `defaultParams` | `object` | ✅ | `defaultParams` | 直接映射 | yes |
| `syncLocation` | `boolean` | ✅ | `syncLocation` | 直接映射 | yes |

### 1.7 表格内嵌属性（Legacy CRUD 继承 Table）

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `columns` | `array` | ✅ | `columns` | 直接映射 | yes |
| `columnsTogglable` | `boolean\|'auto'` | ✅ | `columnSettings.enabled` | `columnSettings: { enabled: true }` | no (结构转换) |
| `autoFillHeight` | `boolean \| object` | 🔴 **计划中** | — | 见 design doc §1.1，已在 plan Phase 1 | yes |
| `rowClassNameExpr` | `string` | ⛔ | — | cell 级 className 替代 | no |
| `combineNum` | `number` | ✅ | `combineNum` | 直接映射 | yes |
| `combineFromIndex` | `number` | 🔴 | — | **建议新增** `combineFromIndex?: number`。Non-Blocking Follow-up | yes |
| `prefixRow` / `affixRow` | `object` | ✅ | `prefixRow` / `affixRow` | 结构化对象 | no (结构转换) |
| `resizable` | `boolean` | ✅ | `columnResize` | 直接映射 | yes |
| `placeholder` | `string` | ✅ | `empty` | 属性重命名 | yes |
| `showHeader` | `boolean` | 🔴 | — | **低优先级**。Flux 当前总是显示表头。可加 `showHeader?: boolean`（缺省 `true`） | yes |
| `showFooter` | `boolean` | 🔴 | — | **低优先级**。Flux 用 `affixRow` 存在与否控制 | yes |
| `showIndex` | `boolean` | ⛔ | — | Flux 暂不实现，可由数据/列定义派生 | — |
| `tableLayout` | `'fixed'\|'auto'` | ⛔ | — | 用样式系统控制 | — |
| `itemBadge` | `BadgeObject` | ⛔ | — | 低价值；如需可加行级 badge 支持 | — |
| `expandConfig` | `object` | 🟡 | `expandable` | `expandable: { expandRowByClick, expandedRowKeys }` | no (结构转换) |

### 1.8 CRUD2 特有属性（amis CRUD2 增量）

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `loadType` | `'more'\|'pagination'` | ✅ | `pagination.mode: 'infinite'\|'pages'` | 直接映射（语义等价） | yes |
| `keepItemSelectionOnPageChange` | `boolean` | ✅ | `selection.keepOnPageChange` | 直接映射 | yes |
| `autoJumpToTopOnPagerChange` | `boolean` | 🔴 | — | 见 §1.3 | yes |
| `primaryField` | `string` | ✅ | `rowKey` | 属性重命名 | yes |

---

## 2. Table (amis Table2 BaseTableSchema2 → Flux TableSchema)

### 2.1 表格级属性

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `source` | `string` | ✅ | `source` | 直接映射 | yes |
| `columns` | `array` | ✅ | `columns` | 直接映射 | yes |
| `title` | `string \| Schema` | ✅ | `header` | `header: SchemaInput \| string` | no (结构差异) |
| `footer` | `string \| Schema` | ✅ | `footer` | `footer: SchemaInput \| string` | no |
| `loading` | `boolean \| string \| Schema` | ✅ | `loading` / `loadingContent` | Flux 有 loading boolean + loadingContent slot | yes |
| `rowClassNameExpr` | `string` | ⛔ | — | cell 级 className 替代 | no |
| `lineHeight` | `string` | 🔴 | — | **低优先级**。固定行高 CSS。可通过 className 控制或新增 `lineHeight?: string` | yes |
| `bordered` | `boolean` | ✅ | `bordered` | 直接映射 | yes |
| `showHeader` | `boolean` | 🔴 | — | **低优先级**。可加 `showHeader?: boolean`（缺省 `true`） | yes |
| `keyField` | `string` | ✅ | `rowKey` | 属性重命名 | yes |
| `childrenColumnName` | `string` | ✅ | `rowChildrenField` | 属性重命名 | yes |
| `tableLayout` | `'fixed'\|'auto'` | ⛔ | — | 用样式系统控制 | — |
| `autoFillHeight` | `boolean \| object` | 🔴 **计划中** | — | 见 design doc §1.1 | yes |
| `sticky` | `boolean` | ✅ | `affixHeader` | 属性重命名 | yes |
| `canAccessSuperData` | `boolean` | ⛔ | — | Flux 隔离 scope 设计；不支持隐式父域穿透 | — |
| `lazyRenderAfter` | `number` | ⛔ | — | 虚拟滚动已覆盖 | — |
| `columnsTogglable` | `boolean\|'auto'` | ✅ | `columnSettings.enabled` | 结构转换 | no |
| `popOverContainer` | `any` | ⛔ | — | Flux Popover 用 Base UI portal（自动定位容器） | — |
| `itemBadge` | `BadgeObject` | ⛔ | — | 低价值 | — |

### 2.2 RowSelection (amis RowSelectionSchema)

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `type` | `string` | ✅ | `rowSelection.type` | 直接映射 | yes |
| `keyField` | `string` | ✅ | `rowKey` | 表级统一 | yes |
| `disableOn` | `string` | ✅ | `rowSelection.checkableWhen` | 属性重命名 | yes |
| `selectedRowKeys` | `array` | ✅ | `rowSelection.selectedRowKeys` | 直接映射 | yes |
| `selectedRowKeysExpr` | `string` | 🔴 | — | **低优先级**。Flux 用 scope state path 表达式驱动。如需可加 `rowSelection.selectedRowKeysExpr?: string` | yes |
| `columnWidth` | `number` | 🔴 | — | **低优先级**。选择列宽度。Flux 固定 `CONTROL_COLUMN_WIDTH=40`。如需可加 `rowSelection.columnWidth?: number` | yes |
| `rowClick` | `boolean` | 🔴 **计划中** | `rowSelection.toggleOnRowClick` | 见 design doc §1.3 | yes |
| `selections` | `array` | 🔴 | — | **低优先级**。自定义选择菜单（全选/反选/选奇数行等）。Flux `handleSelectAll` 已支持全选/反选 | no |

### 2.3 Expandable (amis ExpandableSchema)

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `type` | `string` | 🟡 | `expandable.expandedRow` | Flux 用 region | no |
| `keyField` | `string` | ✅ | `rowKey` | 表级统一 | yes |
| `expandableOn` | `string` | 🔴 | — | **建议新增** `expandable.expandableWhen?: string`。行可展开条件表达式 | yes |
| `expandedRowClassNameExpr` | `string` | ⛔ | — | 样式系统 marker class | no |
| `expandedRowKeys` | `array` | ✅ | `expandable.expandedRowKeys` | 直接映射 | yes |
| `expandedRowKeysExpr` | `string` | 🔴 | — | **低优先级**。Flux 用 scope state path | yes |

### 2.4 Table 列属性 (amis ColumnSchema + AMISTableColumnBase)

amis 有两套列定义：Table2 的 `ColumnSchema`（schema.json 行 11888-12030）和 Legacy Table 的 `AMISTableColumnBase`（行 10283-10444）。

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `name` | `string` | ✅ | `name` | 直接映射 | yes |
| `title` / `label` | `string \| Schema` | ✅ | `label` / `labelRegionKey` | 直接映射 | yes |
| `type` | `string` | 🟡 | `cell` region | Flux 列内容由 `cell` region 内容决定渲染 | no |
| `fixed` | `string` | ✅ | `fixed: 'left'\|'right'` | 直接映射 | yes |
| `width` | `number \| string` | ✅ | `width` | 直接映射 | yes |
| `align` | `string` | ✅ | `align` | 直接映射 | yes |
| `headerAlign` | `string` | 🔴 | — | **建议新增** `headerAlign?: 'left'\|'center'\|'right'`。表头内容对齐 | yes |
| `vAlign` | `string` | 🔴 | — | **低优先级**。列垂直对齐 `top\|middle\|bottom`。可加 `vAlign?: 'top'\|'middle'\|'bottom'` | yes |
| `sortable` / `sorter` | `boolean` | ✅ | `sortable` | 直接映射 | yes |
| `searchable` | `boolean \| Schema` | ✅ | `searchable` | 直接映射 | yes |
| `filterable` | `object` | ✅ | `filterable` / `filterOptions` | 结构转换 | no |
| `toggled` | `boolean` | ✅ | `toggled` | 直接映射 | yes |
| `hidden` | `boolean` | ✅ | `hidden` | 直接映射 | yes |
| `children` | `array` | ✅ | `children` (多级表头) | 直接映射 | yes |
| `copyable` | `boolean \| object` | ✅ | `copyable` | 直接映射 | yes |
| `popOver` | `object` | ✅ | `popOver` | 结构转换（inline 对象） | no |
| `quickEdit` | `object` | ✅ | `quickEdit` | 结构转换 | no |
| `quickEditOnUpdate` | `object` | 🔴 | — | **低优先级**。更新时快速编辑配置。Flux 可通过 `quickEdit.mode` 区分 | no |
| `className` | `string` | ✅ | (BaseSchema) `className` | 直接映射 | yes |
| `classNameExpr` | `string` | 🔴 | — | **建议新增** `classNameExpr?: string`（cell 级条件样式表达式）。或通过 `cell.className` 表达式 | yes |
| `titleClassName` | `string` | 🔴 | — | **低优先级**。表头单元格样式。可通过 `labelRegionKey` 控制 | yes |
| `labelClassName` | `string` | 🔴 | — | **低优先级**。列头样式。同上 | yes |
| `remark` | `string` | 🔴 | — | **低优先级**。列表头提示。可加 `remark?: string` 或通过 `labelRegionKey` | yes |
| `rowSpanExpr` | `string` | 🔴 | — | **低优先级**。行合并表达式。Flux `combineNum` 仅做连续相同值合并；如需表达式合并需新能力 | yes |
| `colSpanExpr` | `string` | 🔴 | — | **低优先级**。列合并表达式。同上 | yes |
| `breakpoint` | `string` | 🟡 | `responsive.mode: 'expand'` + `responsive.breakpoint` | Flux 用表级响应式配置，非列级。如需列级 breakpoint 可加 `breakpoint?: 'xs'\|'sm'\|'md'\|'lg'` | yes |
| `value` | `any` | ⛔ | — | Flux 列默认值无意义（列是显示器不是输入控件） | — |
| `unique` | `boolean` | ⛔ | — | Flux 列级无此概念；校验在 form 层 | — |
| `canAccessSuperData` | `boolean` | ⛔ | — | Flux 隔离 scope 设计 | — |
| `lazyRenderAfter` | `number` | ⛔ | — | 虚拟滚动已覆盖 | — |
| `innerStyle` | `object` | ⛔ | — | 用 className 系统 | — |

---

## 3. Form (amis FormSchema → Flux FormSchema)

### 3.1 请求/生命周期（Flux 下沉到 action）

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `api` | `AMISApi` | ✅ | `submitAction` | `submitAction: { action: "ajax", args: {...} }` | no (action 包装) |
| `initApi` | `AMISApi` | ✅ | `initAction` / `loadAction` | `loadAction: { action: "ajax", args: {...} }` + `autoLoad: true` | no |
| `initFetch` | `boolean` | ✅ | `autoLoad` (语义反转) | `autoLoad: false` = `initFetch: false` | yes (反转) |
| `initFetchOn` | `Expression` | ✅ | `autoLoad` (表达式) | `autoLoad: "${expr}"` | yes |
| `asyncApi` | `AMISApi` | ⛔ | — | Flux 无长轮询 API；用 data-source `interval` + `stopWhen` | — |
| `initAsyncApi` | `AMISApi` | ⛔ | — | 同上 | — |
| `initFinishedField` | `string` | ⛔ | — | 同上 | — |
| `initCheckInterval` | `number` | ⛔ | — | 同上 | — |
| `checkInterval` | `number` | ⛔ | — | 同上 | — |
| `finishedField` | `string` | ⛔ | — | 同上 | — |
| `interval` | `number` | 🟡 | data-source `interval` | 不在 Form 层 | no |
| `silentPolling` | `boolean` | 🟡 | data-source 层 | 不在 Form 层 | no |
| `stopAutoRefreshWhen` | `Expression` | 🟡 | data-source `stopWhen` | 不在 Form 层 | no |
| `feedback` | `Schema` | 🟡 | `submitAction.then` | `then: [{ action: "openDialog", ... }]` | no |

### 3.2 提交行为

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `submitOnChange` | `boolean` | ✅ | `submitOnChange` | 直接映射（Flux debounce 300ms） | yes |
| `submitOnInit` | `boolean` | 🟡 | `initAction: { action: "submitForm" }` | Flux 用 initAction 显式触发 submit | no |
| `resetAfterSubmit` | `boolean` | 🟡 | `onSubmitSuccess: [{ action: "component:reset" }]` | Flux 用显式 action 链 | no |
| `clearAfterSubmit` | `boolean` | 🟡 | `onSubmitSuccess` action | Flux 用显式 action 链 | no |
| `redirect` | `string` | 🟡 | `submitAction.then: [{ action: "navigate" }]` | Flux 用 then 链 | no |
| `reload` | `string` | 🟡 | `submitAction.then: [{ action: "component:refresh" }]` | Flux 用 then 链 | no |
| `target` | `string` | 🟡 | `submitAction.then` action 链 | Flux 用 then 链 | no |
| `submitText` | `string` | 🟡 | 手写 actions region | Flux Form 无内置提交按钮 | no |
| `wrapWithPanel` | `boolean` | ⛔ | — | Flux Form 无 panel 包装；用显式 actions 区域 | — |
| `preventEnterSubmit` | `boolean` | ✅ | `preventEnterSubmit` | 直接映射 | yes |

### 3.3 持久化与离开提示

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `persistData` | `string` | ⛔ | — | Zustand persist 中间件（宿主层） | — |
| `persistDataKeys` | `string[]` | ⛔ | — | 同上 | — |
| `clearPersistDataAfterSubmit` | `boolean` | ⛔ | — | 同上 | — |
| `promptPageLeave` | `boolean` | ⛔ | — | 宿主路由守卫（`statusPath.dirty`） | — |
| `promptPageLeaveMessage` | `string` | ⛔ | — | 同上 | — |

### 3.4 布局与显示

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `mode` | `'normal'\|'inline'\|'horizontal'\|'flex'` | ✅ | `mode: 'normal'\|'horizontal'\|'inline'` | Flux 不含 `'flex'`（3 值） | yes |
| `columnCount` | `number` | ✅ | `columnCount` | 直接映射 | yes |
| `horizontal` | `FormHorizontal` | ✅ | `labelWidth` | Flux 用 `labelWidth` 替代 `horizontal.leftFixed` | no |
| `labelAlign` | `LabelAlign` | ✅ | `labelAlign` | 直接映射 | yes |
| `labelWidth` | `number\|string` | ✅ | `labelWidth` | 直接映射 | yes |
| `autoFocus` | `boolean` | ✅ | `autoFocus` | 直接映射 | yes |
| `static` | `boolean` | ✅ | `static` | 直接映射 | yes |
| `affixFooter` | `boolean` | 🔴 | — | **低优先级**。固定底部按钮。可通过 CSS sticky 控制 | yes |
| `panelClassName` | `string` | 🟡 | `bodyClassName` | Flux 无 panel 概念 | yes |
| `debug` | `boolean` | ⛔ | — | Flux 有独立的 `scope-debug` renderer | — |
| `debugConfig` | `object` | ⛔ | — | 同上 | — |
| `rules` | `array` | ✅ | `rules: FormCrossFieldRule[]` | 结构差异（Flux `{ rule, field, target, message }` vs amis `{ rule, message, name }`） | no |
| `primaryField` | `string` | ⛔ | — | Flux Form 无 primaryField 概念 | — |
| `canAccessSuperData` | `boolean` | ⛔ | — | Flux `scopePolicy: 'form'` 硬隔离 | — |
| `name` | `string` | ✅ | (BaseSchema) | 直接映射 | yes |
| `messages` | `object` | 🟡 | action 级 `messages` | Flux 在 submitAction 上配置 | no |

### 3.5 Regions

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `body` | `SchemaCollection` | ✅ | `body` | 直接映射 | yes |
| `actions` | `array` | ✅ | `actions` | 直接映射 | yes |
| `tabs` | `deprecated` | ⛔ | — | 请用 `type: 'tabs'` 组件 | — |
| `fieldSet` | `deprecated` | ⛔ | — | 请用 `type: 'fieldset'` 组件 | — |
| `data` | `object` | ✅ | `data` | 直接映射 | yes |

---

## 4. FormItem (amis AMISFormItemBase → Flux BoundFieldSchemaBase)

amis FormItem 属性对应 Flux `BoundFieldSchemaBase`（所有输入控件基类）。

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `label` | `string \| false` | ✅ | `label` | 直接映射 | yes |
| `labelAlign` | `LabelAlign` | ✅ | `labelAlign` | 直接映射 | yes |
| `labelWidth` | `number\|string` | ✅ | `labelWidth` | 直接映射 | yes |
| `labelClassName` | `string` | 🔴 | — | **低优先级**。label 元素 className。可加 `labelClassName?: string` | yes |
| `labelOverflow` | `'default'\|'ellipsis'` | 🔴 | — | **低优先级**。label 截断模式。可加 `labelOverflow?: 'default'\|'ellipsis'` | yes |
| `name` | `string` | ✅ | `name` | 直接映射 | yes |
| `extraName` | `string` | 🔴 | — | **低优先级**。范围控件额外字段名。可加 `extraName?: string` | yes |
| `remark` | `RemarkBase` | ✅ | `remark: FieldRemarkSchema` | 结构转换（Flux 对象形式） | no |
| `labelRemark` | `RemarkBase` | ✅ | `labelRemark: FieldRemarkSchema` | 同上 | no |
| `hint` | `Template` | ✅ | `hint` | 直接映射 | yes |
| `description` / `desc` | `Template` | ✅ | `description` | 直接映射（Flux 统一用 `description`） | yes |
| `descriptionClassName` | `string` | 🔴 | — | **低优先级**。描述 className。可加 `descriptionClassName?: string` | yes |
| `mode` | `string` | ✅ | `mode` | 直接映射 | yes |
| `horizontal` | `FormHorizontal` | ✅ | `labelWidth` | Flux 用 `labelWidth` | no |
| `inline` | `boolean` | 🟡 | `mode: 'inline'` | Flux 用 mode 统一 | yes |
| `inputClassName` | `string` | 🔴 | — | **低优先级**。input 控件 className。可加 `inputClassName?: string` | yes |
| `placeholder` | `string \| object` | ✅ | `placeholder` (InputSchema) | 直接映射 | yes |
| `required` | `boolean` | ✅ | `required` | 直接映射 | yes |
| `readOnly` | `boolean` | ✅ | `readOnly` | 直接映射 | yes |
| `readOnlyOn` | `string` | ✅ | `readOnly` (表达式) | `readOnly: "${expr}"` | yes |
| `submitOnChange` | `boolean` | ✅ | (Form 级) `submitOnChange` | Flux 在 Form 级控制 | no |
| `validateOnChange` | `boolean` | 🟡 | `validateOn: 'change'\|'blur'\|'submit'` | Flux 用 `validateOn` 枚举 | no |
| `validationErrors` | `object` | 🟡 | `validate.message` | Flux 用 `validate: { action, message }` | no |
| `validations` | `object` | 🟡 | `validate` rules | Flux 用 validation plan | no |
| `validateApi` | `AMISApi` | 🟡 | `validate.action` | `validate: { action: { action: "ajax", ... } }` | no |
| `autoFill` | `object` | 🔴 | — | **低优先级**。控件值自动填充其他字段。Flux 可用 `onChange: [{ action: "setValue", ... }]` 替代 | no |
| `initAutoFill` | `object` | 🔴 | — | 同上，init 时填充 | no |
| `value` | `any` | 🟡 | `data` 初始值 | Flux 通过 form `data` 注入 | no |
| `clearValueOnHidden` | `boolean` | ✅ | `hiddenFieldPolicy.clearValueWhenHidden` | 结构转换（Flux Form 级统一） | no |
| `size` | `'xs'\|'sm'\|'md'\|'lg'\|'full'` | 🔴 | — | **低优先级**。字段大小。Flux 用 className 控制。可加 `size?: 'sm'\|'md'\|'lg'` | yes |
| `row` | `number` | ⛔ | — | Flux 用 flex/grid 布局 | — |

---

## 5. Button (amis AMISButton → Flux ButtonSchema)

### 5.1 外观属性

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `label` | `string` | ✅ | `label` | 直接映射 | yes |
| `level` | `string` (9 值) | ✅ | `variant` (6 值) | 映射：primary→default, danger→destructive, link→link, success→default+className, info/warning/dark/light/secondary→对应 | yes (枚举映射) |
| `size` | `'xs'\|'sm'\|'md'\|'lg'` | ✅ | `size` (8 值) | `md` → `default`；Flux 多了 icon 变体 | yes |
| `icon` | `Icon` | ✅ | `icon` (Lucide kebab-case) | `fa fa-plus` → `plus` | no (格式转换) |
| `rightIcon` | `Icon` | ✅ | `rightIcon` | 同上 | no |
| `block` | `boolean` | ✅ | `block` | 直接映射 | yes |
| `primary` | `boolean` | ⛔ deprecated | — | 用 `variant` | — |
| `activeLevel` | `string` | ⛔ | — | 用 className 控制 | — |
| `activeClassName` | `string` | ⛔ | — | 用 className 控制 | — |

### 5.2 提示与状态

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `tooltip` | `Tooltip` | ✅ | `tooltip` (string) | Flux 简化为纯字符串 | no (Flux 不接受对象) |
| `disabledTip` | `string` | ✅ | `disabledTip` | 直接映射 | yes |
| `tooltipPlacement` | `string` (enum) | 🔴 **计划中** | — | 见 design doc §3.1。Flux 用 `{ side, align }` 对象形式 | yes |
| `loadingOn` | `string` | ✅ | `loading` (表达式) | `loading: "${expr}"` | yes |
| `loadingClassName` | `string` | 🔴 | — | **低优先级**。loading spinner className。可加 `loadingClassName?: string` | yes |
| `iconClassName` | `string` | 🔴 | — | **低优先级**。icon 元素 className。可加 `iconClassName?: string` | yes |
| `rightIconClassName` | `string` | 🔴 | — | **低优先级**。同上 | yes |

### 5.3 行为属性

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `actionType` | `string` | 🟡 | `onClick: ActionSchema` | Flux action graph | no (范式差异) |
| `href` | `string` | 🔴 | — | **建议新增**。链接按钮。`href?: string` + `target?: string`。归后续增强（design.md §4 已声明） | yes |
| `onClick` | `string` (JS) | 🟡 | `onClick: ActionSchema` | Flux 不允许 JS 字符串 | no |
| `confirmText` | `string` | ✅ | action 级 `confirmText` | Flux 在 action 对象上配置 | no |
| `required` | `string[]` | 🟡 | action 级 `when` | `when: "${fieldA && fieldB}"` | no |
| `close` | `boolean\|string` | 🟡 | `then: [{ action: "closeSurface" }]` | Flux 用 then 链 | no |
| `countDown` | `number` | 🔴 **计划中** | — | 见 design doc §3.3 | yes |
| `countDownTpl` | `string` | 🔴 **计划中** | — | 同上 | yes |
| `hotKey` | `string` | ⛔ | — | 宿主/独立方案 | — |
| `badge` | `BadgeBase` | 🔴 | — | **低优先级**。按钮角标。可用 Flux `badge` 组件包裹替代 | no |
| `disabledOnAction` | `boolean` | ⛔ | — | Flux `loading` 显式控制（不在按钮内静默持有 pending 态） | — |
| `requireSelected` | `boolean` | ⛔ | — | Flux `listActions` selection-aware + `when` | no |
| `mergeData` | `boolean` | ⛔ | — | Flux surface 隔离 scope | — |
| `target` | `string` | 🟡 | `then: [{ action: "component:refresh" }]` | Flux 用 then 链 | no |
| `body` | `SchemaCollection` | ⛔ | — | Flux 按钮 `label` value-or-region 升级（见 design.md §6） | no |
| `tabIndex` | `string` | ⛔ | — | 低价值 | — |

### 5.4 Legacy Action（actionType 判别树）

amis 的 `actionType` 判别树有多个分支，每个分支有额外属性。Flux 全部用 action graph 替代：

| amis actionType | Flux Action | Simple Config? |
|---|---|---|
| `ajax` | `{ action: "ajax", args: { url, method, data } }` + `responseType` / `downloadFileName` **计划中** | no |
| `dialog` | `{ action: "openDialog", args: { title, body } }` | no |
| `drawer` | `{ action: "openDrawer", args: { side, body } }` | no |
| `reload` | `{ action: "component:refresh", componentId }` | no |
| `link` / `url` | `{ action: "navigate", args: { url } }` | no |
| `copy` | `{ action: "setValue" }` 近似 | no |
| `toast` | `{ action: "showToast", args: { level, message } }` | no |
| `email` | 🔴 缺失 — 可用 `navigate: "mailto:..."` | no |
| `saveAs` | ⛔ 后台职责 | — |
| `download` | `{ action: "ajax", args: { responseType: "blob" } }` **计划中** | no |
| `submit` / `confirm` / `reset` / `close` / `prev` / `next` / `cancel` | `{ action: "submitForm" }` / `{ action: "closeSurface" }` 等 | no |

---

## 6. Page (amis PageSchema → Flux PageSchema)

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `title` | `Template` | ✅ | `title` (value-or-region) | 直接映射 | yes |
| `subTitle` | `Template` | ✅ | `subTitle` | 直接映射 | yes |
| `remark` | `RemarkObject` | ✅ | `remark` | 直接映射 | yes |
| `body` | `SchemaCollection` | ✅ | `body` | 直接映射 | yes |
| `aside` | `SchemaCollection` | ✅ | `aside` | 直接映射 | yes |
| `asidePosition` | `'left'\|'right'` | ✅ | `asidePosition` | 直接映射 | yes |
| `asideClassName` | `ClassName` | ✅ | `asideClassName` | 直接映射 | yes |
| `bodyClassName` | `ClassName` | ✅ | `bodyClassName` | 直接映射 | yes |
| `headerClassName` | `ClassName` | ✅ | `headerClassName` | 直接映射 | yes |
| `toolbarClassName` | `ClassName` | ✅ | `toolbarClassName` | 直接映射 | yes |
| `asideResizor` | `boolean` | 🔴 **计划中** | — | 见 design doc §4.1，Flux 命名为 `asideResizable` | yes |
| `asideSticky` | `boolean` | 🔴 **计划中** | — | 见 design doc §4.1 | yes |
| `asideMinWidth` | `number` | 🔴 **计划中** | — | 见 design doc §4.1 | yes |
| `asideMaxWidth` | `number` | 🔴 **计划中** | — | 见 design doc §4.1 | yes |
| `data` | `DefaultData` | ✅ | `data` | 直接映射 | yes |
| `initApi` | `AMISApi` | ⛔ | — | Flux 请求下沉 data-source / `loadAction` | — |
| `initFetch` / `initFetchOn` | `boolean` / `Expr` | ⛔ | — | 同上 | — |
| `interval` / `silentPolling` | `number` / `boolean` | ⛔ | — | 同上 | — |
| `stopAutoRefreshWhen` | `Expression` | ⛔ | — | 同上 | — |
| `toolbar` | `SchemaCollection` | 🟡 | `header` region | Flux `header` 已 subsume toolbar | no |
| `css` / `cssVars` | `object` | ⛔ | — | 违反 styling contract | — |
| `mobileCSS` | `object` | ⛔ | — | 同上 | — |
| `style` | `object` | ⛔ | — | 用 className | — |
| `pullRefresh` | `object` | ⛔ | — | 归 `mobile-roadmap.md`（flux-renderers-mobile） | — |
| `regions` | `array` | ⛔ | — | Flux 自动感知内容决定区域显示 | — |
| `showErrorMsg` | `boolean` | ⛔ | — | Flux 在 action 层处理错误显示 | — |
| `definitions` | `Definitions` | ⛔ | — | Flux 无 JSON schema $ref 引用 | — |
| `name` | `Name` | ✅ | (BaseSchema) | 直接映射 | yes |
| `statusPath` | — | ✅ | `statusPath` (Flux 独有) | — | — |

---

## 7. Dialog (amis DialogSchema → Flux DialogSchema)

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `title` | `SchemaCollection` | ✅ | `title` (value-or-region) | 直接映射 | yes |
| `body` | `SchemaCollection` | ✅ | `body` | 直接映射 | yes |
| `actions` | `array` | ✅ | `actions` | 直接映射 | yes |
| `data` | `DefaultData` | ✅ | `data` | 直接映射 | yes |
| `size` | `'xs'\|'sm'\|'md'\|'lg'\|'xl'\|'full'` | ✅ | `size` | 直接映射 | yes |
| `width` / `height` | `string` | ✅ | `width` / `height` | 直接映射（Flux 接受 `number\|string`） | yes |
| `closeOnEsc` | `boolean` | ✅ | `closeOnEsc` | 直接映射 | yes |
| `closeOnOutside` | `boolean` | ✅ | `closeOnOutsideClick` | 属性重命名 | yes |
| `showCloseButton` | `boolean` | ✅ | `showCloseButton` | 直接映射 | yes |
| `header` | `SchemaCollection` | ✅ | `header` | 直接映射 | yes |
| `footer` | `SchemaCollection` | ✅ | `footer` | 直接映射 | yes |
| `confirm` | `boolean` | ✅ | `confirm` | 直接映射 | yes |
| `bodyClassName` | `ClassName` | ✅ | `bodyClassName` | 直接映射 | yes |
| `headerClassName` | `ClassName` | ✅ | `headerClassName` | 直接映射 | yes |
| `overlay` | `boolean` | ✅ | `showMask` | 属性重命名 | yes |
| `data` | `object` | ✅ | `data` | 直接映射 | yes |
| `open` / `defaultOpen` | — | ✅ | `open` / `defaultOpen` (Flux 独有) | — | — |
| `statusPath` | — | ✅ | `statusPath` (Flux 独有) | — | — |
| `onConfirm` | — | ✅ | `onConfirm` (Flux 独有) | — | — |
| `container` | — | ✅ | `container` (Flux 独有) | — | — |
| `draggable` | `boolean` | 🔴 | — | **低优先级**。可拖拽 Dialog。Flux 未实现。可加 `draggable?: boolean` | yes |
| `allowFullscreen` | `boolean` | 🔴 | — | **低优先级**。可全屏 Dialog。可加 `allowFullscreen?: boolean` | yes |
| `showErrorMsg` | `boolean` | ⛔ | — | Flux action 层处理 | — |
| `showLoading` | `boolean` | ⛔ | — | Flux action 层处理（loading 是 owner 驱动） | — |
| `dialogType` | `'confirm'` | ✅ | `confirm` | Flux `confirm` 属性 | yes |
| `msg` | `string` | ⛔ | — | Flux 用 showToast action | — |
| `confirmText` | `Template` | ⛔ | — | Flux 在 action 层 `confirmText` | no |
| `cancelText` | `Template` | ⛔ | — | Flux `confirm` 自动生成 | — |
| `confirmBtnLevel` | `string` | ⛔ | — | Flux 用 variant | — |
| `cancelBtnLevel` | `string` | ⛔ | — | Flux 用 variant | — |
| `inputParams` | `any` | ⛔ | — | Flux 无此概念 | — |
| `name` | `Name` | ✅ | (BaseSchema) | 直接映射 | yes |

---

## 8. Drawer (amis DrawerSchema → Flux DrawerSchema)

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `title` | `SchemaCollection` | ✅ | `title` | 直接映射 | yes |
| `body` | `SchemaCollection` | ✅ | `body` | 直接映射 | yes |
| `actions` | `array` | ✅ | `actions` | 直接映射 | yes |
| `data` | `object` | ✅ | `data` | 直接映射 | yes |
| `size` | `enum` | ✅ | `size` | 直接映射 | yes |
| `width` / `height` | `number\|string` | ✅ | `width` / `height` | 直接映射 | yes |
| `position` | `'left'\|'right'\|'top'\|'bottom'` | ✅ | `side` | 属性重命名 (`position` → `side`) | yes |
| `closeOnEsc` | `boolean` | ✅ | `closeOnEsc` | 直接映射 | yes |
| `closeOnOutside` | `boolean` | ✅ | `closeOnOutside` | 直接映射 | yes |
| `showCloseButton` | `boolean` | ✅ | `showCloseButton` | 直接映射 | yes |
| `resizable` | `boolean` | ✅ | `resizable` (Flux 独有) | 直接映射 | yes |
| `overlay` | `boolean` | ✅ | `showMask` | 属性重命名 | yes |
| `header` | `SchemaCollection` | ✅ | `header` | 直接映射 | yes |
| `footer` | `SchemaCollection` | ✅ | `footer` | 直接映射 | yes |
| `confirm` | `boolean` | ✅ | `confirm` | 直接映射 | yes |
| `bodyClassName` | `ClassName` | ✅ | `bodyClassName` | 直接映射 | yes |
| `headerClassName` | `ClassName` | ✅ | `headerClassName` | 直接映射 | yes |
| `footerClassName` | `ClassName` | ✅ | `footerClassName` | 直接映射 | yes |
| `statusPath` | — | ✅ | `statusPath` (Flux 独有) | — | — |
| `showErrorMsg` | `boolean` | ⛔ | — | Flux action 层处理 | — |
| `inputParams` | `any` | ⛔ | — | Flux 无此概念 | — |
| `name` | `Name` | ✅ | (BaseSchema) | 直接映射 | yes |

---

## 9. Tabs (amis TabsSchema → Flux TabsSchema)

### 9.1 Tabs 容器

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `tabs` | `array` | ✅ | `items` | 属性重命名 | yes |
| `tabsMode` | `TabsMode` | ✅ | `tabsMode` | Flux 枚举更丰富 | yes |
| `source` | `string` | 🔴 | — | **低优先级**。关联数据动态生成 tabs。Flux 无等价物，可用 `loop` 近似 | no |
| `contentClassName` | `ClassName` | ✅ | `contentClassName` | 直接映射 | yes |
| `linksClassName` | `ClassName` | 🟡 | `toolbarClassName` | Flux 用 toolbarClassName（语义不完全对应） | yes |
| `mountOnEnter` | `boolean` | ✅ | (item 级) `mountOnEnter` | Flux 在 item 级控制 | no |
| `unmountOnExit` | `boolean` | ✅ | (item 级) `unmountOnExit` | Flux 在 item 级控制 | no |
| `toolbar` | `ActionSchema` | ✅ | `toolbar` | 直接映射 | yes |
| `subFormMode` | `string` | ⛔ | — | Flux 无子表单模式继承 | — |
| `subFormHorizontal` | `FormHorizontal` | ⛔ | — | 同上 | — |
| `addable` | `boolean` | 🔴 | — | **建议新增** `addable?: boolean`。支持新增 tab | yes |
| `closable` | `boolean` | 🔴 | — | **建议新增** `closable?: boolean`。支持删除 tab | yes |
| `draggable` | `boolean` | 🔴 | — | **建议新增** `draggable?: boolean`。支持拖拽排序 tab | yes |
| `editable` | `boolean` | 🔴 | — | **低优先级**。可编辑标签名。可加 `editable?: boolean` | yes |
| `showTip` | `boolean` | 🔴 | — | **低优先级**。tooltip 提示。可加 `showTip?: boolean` | yes |
| `showTipClassName` | `string` | 🔴 | — | **低优先级** | yes |
| `scrollable` | `boolean` | ⛔ deprecated | — | 已废弃 | — |
| `sidePosition` | `'left'\|'right'` | ✅ | `sidePosition` | 直接映射 | yes |
| `addBtnText` | `string` | 🔴 | — | **低优先级**。新增按钮文案。配合 `addable` | yes |
| `defaultKey` | `string\|number` | ✅ | `defaultValue` | 属性重命名 | yes |
| `activeKey` | `string\|number` | ✅ | `value` + `valueOwnership: 'controlled'` | 属性重命名 | yes |
| `collapseOnExceed` | `number` | 🔴 | — | **低优先级**。超过 N 个折叠为 dropdown | yes |
| `collapseBtnLabel` | `string` | 🔴 | — | **低优先级** | yes |
| `swipeable` | `boolean` | 🔴 | — | **低优先级**。移动端滑动切换。归 mobile-roadmap | yes |
| `orientation` | — | ✅ | `orientation` (Flux 独有) | — | — |
| `variant` | — | ✅ | `variant` (Flux 独有) | — | — |
| `valueOwnership` | — | ✅ | `valueOwnership` (Flux 独有) | — | — |
| `valueStatePath` | — | ✅ | `valueStatePath` (Flux 独有) | — | — |

### 9.2 Tab Item

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `title` | `string\|Schema` | ✅ | `title` / `label` | 直接映射 | yes |
| `body` | `SchemaCollection` | ✅ | `bodyRegionKey` | Flux 用 region | no |
| `tab` | `SchemaCollection` | ⛔ deprecated | — | 请用 `body` | — |
| `badge` | `number` | ✅ | `badge` (Flux 接受 `string\|number`) | 直接映射 | yes |
| `hash` | `string` | 🔴 | — | **低优先级**。URL hash 对应。Flux 无 hash 路由概念。归宿主 | yes |
| `icon` | `Icon` | ✅ | `icon` | 直接映射 | yes |
| `iconPosition` | `'left'\|'right'` | 🔴 | — | **低优先级**。图标位置。可加 `iconPosition?: 'left'\|'right'` | yes |
| `reload` | `boolean` | 🟡 | `onMount: { action: "component:refresh" }` | Flux 用 onMount action | no |
| `mountOnEnter` | `boolean` | ✅ | `mountOnEnter` | 直接映射 | yes |
| `unmountOnExit` | `boolean` | ✅ | `unmountOnExit` | 直接映射 | yes |
| `mode` | `string` | ⛔ | — | Flux 无子表单模式继承 | — |
| `horizontal` | `FormHorizontal` | ⛔ | — | 同上 | — |
| `closable` | `boolean` | 🔴 | — | **建议新增** item 级 `closable?: boolean`（优先级高于 tabs 级） | yes |
| `disabled` | `boolean` | ✅ | `disabled` | 直接映射 | yes |
| `key` | — | ✅ | `key` / `value` (Flux 独有) | — | — |

---

## 10. Wizard (amis WizardSchema → Flux WizardSchema)

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `steps` | `array` | ✅ | `steps` | 直接映射 | yes |
| `startStep` | `string` | ✅ | `value` / `defaultValue` | 属性重命名 | yes |
| `mode` | `'vertical'\|'horizontal'` | 🔴 | — | **建议新增** `mode?: 'vertical'\|'horizontal'`。展示模式 | yes |
| `api` | `AMISApi` | ⛔ | — | Flux 用 step `beforeEnter` action | — |
| `initApi` | `AMISApi` | ⛔ | — | 同上 | — |
| `asyncApi` | `AMISApi` | ⛔ | — | Flux 无长轮询 | — |
| `redirect` | `string` | 🟡 | `onComplete: { action: "navigate" }` | Flux 用 onComplete action | no |
| `reload` | `SchemaReload` | 🟡 | `onComplete: { action: "component:refresh" }` | Flux 用 onComplete action | no |
| `target` | `string` | 🟡 | `onComplete` action | 同上 | no |
| `actionClassName` | `ClassName` | 🔴 | — | **低优先级**。按钮 className。可加 `actionClassName?: string` | yes |
| `actionFinishLabel` | `string` | 🔴 | — | **建议新增** `actionFinishLabel?: string`。完成按钮文案 | yes |
| `actionNextLabel` | `string` | 🔴 | — | **建议新增** `actionNextLabel?: string`。下一步按钮文案 | yes |
| `actionNextSaveLabel` | `string` | 🔴 | — | **建议新增** `actionNextSaveLabel?: string`。下一步并保存按钮文案 | yes |
| `actionPrevLabel` | `string` | 🔴 | — | **建议新增** `actionPrevLabel?: string`。上一步按钮文案 | yes |
| `bulkSubmit` | `boolean` | 🔴 | — | **低优先级**。是否合并提交。Flux 当前逐步提交。可加 `bulkSubmit?: boolean` | yes |
| `readOnly` | `boolean` | ✅ | — | Flux 用 step `disabled` 或 `visible` 近似 | no |
| `affixFooter` | `boolean\|'always'` | 🔴 | — | **低优先级**。固定底部按钮。可通过 CSS 控制 | yes |
| `stepsClassName` | `string` | 🔴 | — | **低优先级**。步骤条区域 className。可加 `stepsClassName?: string` | yes |
| `bodyClassName` | `string` | ✅ | `bodyClassName` | 直接映射 | yes |
| `stepClassName` | `string` | 🔴 | — | **低优先级**。step+body 区域 className。可加 `stepClassName?: string` | yes |
| `footerClassName` | `string` | 🔴 | — | **低优先级**。底部操作栏 className。可加 `footerClassName?: string` | yes |
| `wrapWithPanel` | `boolean` | ⛔ | — | Flux Wizard 无 panel 包装 | — |
| `name` | `Name` | ✅ | (BaseSchema) | 直接映射 | yes |
| `linear` | — | ✅ | `linear` (Flux 独有，缺省 true) | — | — |
| `allowStepJump` | — | ✅ | `allowStepJump` (Flux 独有) | — | — |
| `mountOnEnter` | — | ✅ | `mountOnEnter` (Flux 独有) | — | — |
| `unmountOnExit` | — | ✅ | `unmountOnExit` (Flux 独有) | — | — |
| `onChange` / `onStepCommit` / `onComplete` / `onStepError` | — | ✅ | (Flux 独有 lifecycle actions) | — | — |
| `statusPath` | — | ✅ | `statusPath` (Flux 独有) | — | — |

### Wizard Step (amis WizardStepSchema → Flux WizardStepSchema)

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `api` | `AMISApi` | 🟡 | `beforeEnter: { action: "ajax", ... }` | Flux 用 beforeEnter action | no |
| `asyncApi` | `AMISApi` | ⛔ | — | Flux 无长轮询 | — |
| `title` | `string` | ✅ | `title` | 直接映射 | yes |
| `body` | `SchemaCollection` | ✅ | `body` | 直接映射 | yes |
| `name` / `key` | `string` | ✅ | `key` | 属性重命名 | yes |
| `mode` | `string` | ⛔ | — | Flux 无子表单模式继承 | — |
| `horizontal` | `FormHorizontal` | ⛔ | — | 同上 | — |
| `initApi` | `AMISApi` | 🟡 | `beforeEnter` action | Flux 用 beforeEnter | no |
| `initFetch` | `boolean` | 🟡 | `beforeEnter` action 条件 | Flux 用 beforeEnter | no |
| `initFetchOn` | `Expression` | 🟡 | `beforeEnter.when` | Flux 用 beforeEnter + when | no |
| `redirect` | `string` | 🟡 | wizard `onComplete` action | Flux 在 wizard 级处理 | no |
| `reload` | `SchemaReload` | 🟡 | wizard `onComplete` action | 同上 | no |
| `submitText` | `string` | 🔴 | — | **低优先级**。当前步骤提交按钮文案。可加 `submitText?: string`（或归 wizard 级 `actionNextSaveLabel`） | yes |
| — | — | ✅ | `description` (Flux 独有) | — | — |
| — | — | ✅ | `actions` (Flux 独有 step 级 actions region) | — | — |
| — | — | ✅ | `visible` (Flux 独有) | — | — |
| — | — | ✅ | `disabled` (Flux 独有) | — | — |
| — | — | ✅ | `beforeEnter` / `beforeLeave` (Flux 独有 lifecycle actions) | — | — |

---

## 11. ApiSchema (amis BaseApiObject → Flux ApiSchema)

| amis Property | amis Type | Flux Status | Flux Property | Recommended Action | Simple Config? |
|---|---|---|---|---|---|
| `method` | `enum` | ✅ | `method` | 直接映射 | yes |
| `url` | `UrlPath` | ✅ | `url` | 直接映射（`@query:` → `/r/` 前缀转换在 codegen） | yes |
| `data` | `object` | ✅ | `data` | 直接映射 | yes |
| `headers` | `object` | ✅ | `headers` | 直接映射 | yes |
| `dataType` | `'json'\|'form-data'\|'form'` | 🔴 | — | **低优先级**。发送体格式。Flux fetcher 由宿主决定 Content-Type。可加 `dataType?: 'json'\|'form-data'\|'form'` | yes |
| `responseType` | `'blob'` | 🔴 **计划中** | — | 见 design doc §5.1 | yes |
| `downloadFileName` | `string` | 🔴 **计划中** | — | 见 design doc §5.1 | yes |
| `sendOn` | `Expression` | 🟡 | action 级 `when` / data-source `sendOn` | Flux 在 action/data-source 层控制 | no |
| `cache` | `number` | 🟡 | action 级 `control: { cacheTTL: N }` | Flux 在 action control 层 | no |
| `autoRefresh` | `boolean` | 🟡 | Flux CRUD `loadAction` reaction 自动响应 | Flux reaction 模型 | no |
| `trackExpression` | `Expression` | 🟡 | data-source `dependsOn` | Flux 用 dependsOn | no |
| `replaceData` | `boolean` | ⛔ | — | Flux CRUD 总是替换数据 | — |
| `concatDataFields` | `string\|array` | 🔴 | — | **低优先级**。合并返回数据字段。可加 `concatDataFields?: string\|string[]` | yes |
| `responseData` | `object` | ⛔ | — | Flux 用 `responseAdaptor` | — |
| `convertKeyToPath` | `boolean` | ⛔ | — | Flux formula 自行处理 | — |
| `attachDataToQuery` | `boolean` | 🔴 | — | **低优先级**。GET 请求 data 附带到 query。可加 `attachDataToQuery?: boolean` | yes |
| `forceAppendDataToQuery` | `boolean` | 🔴 | — | **低优先级**。同上 | yes |
| `qsOptions` | `object` | 🔴 | — | **低优先级**。query string 序列化配置。可加 `qsOptions?: { arrayFormat, allowDots }` | yes |
| `silent` | `boolean` | 🔴 | — | **低优先级**。autoFill 不显示错误提示。可加 `silent?: boolean` | yes |
| `messages` | `object` | 🟡 | action 级 `messages: { success, failed }` | Flux 在 action 层配置 | no |
| `requestAdaptor` | — | ✅ | `requestAdaptor` | Flux formula 编译（沙箱） | yes |
| `responseAdaptor` | — | ✅ | `responseAdaptor` | Flux formula 编译（沙箱） | yes |

---

## 12. 汇总统计

### 12.1 按状态分类

| 状态 | 数量 | 说明 |
|---|---|---|
| ✅ 已支持 | ~180 | 有直接 Flux 属性映射 |
| 🔴 缺失-建议新增 | ~65 | Flux 应补充 |
| 🟡 缺失-已有替代 | ~45 | Flux 有等价能力，需 codegen 转换 |
| ⛔ 不采纳(确认) | ~50 | Flux 已拒绝，有设计理由 |
| 🔵 DESIGN-ACK-NOT-IMPL | 3 | schema 已声明，runtime 未实现 |
| 🔴 **计划中** | 10 | 已在 design doc + plan 中覆盖 |

### 12.2 建议"新增"项按优先级

**高优先级（ERP 高频使用）：**
- Table `autoFillHeight` — **计划中** (plan Phase 1)
- Table/CRUD `selection.toggleOnRowClick` — **计划中** (plan Phase 2)
- Table column `headerAlign` — 表头对齐，ERP 常见
- CRUD `autoJumpToTopOnPagerChange` — 翻页体验

**中优先级：**
- Button `tooltipPlacement` / `countDown` — **计划中** (plan Phase 3)
- Page `asideResizable` — **计划中** (plan Phase 4)
- ApiSchema `responseType` / `downloadFileName` — **计划中** (plan Phase 5)
- CRUD `alwaysShowPagination` — 分页栏控制
- Wizard `mode` / `actionFinishLabel` / `actionNextLabel` / `actionPrevLabel` — 向导标签
- Tabs `closable` / `draggable` / `addable` — tab 操作能力
- Table column `classNameExpr` — cell 条件样式
- Table `expandable.expandableWhen` — 行可展开条件
- Dialog `draggable` / `allowFullscreen`

**低优先级：**
- Table column `vAlign`, `titleClassName`, `labelClassName`, `remark`
- Table column `rowSpanExpr`, `colSpanExpr`
- Table `showHeader`, `lineHeight`
- CRUD `labelTpl`, `hideQuickSaveBtn`, `totalField`, `combineFromIndex`
- FormItem `labelClassName`, `labelOverflow`, `descriptionClassName`, `inputClassName`, `extraName`, `size`
- Button `href`/`target`, `loadingClassName`, `iconClassName`, `badge`
- Tabs `source`, `showTip`, `collapseOnExceed`, `swipeable`, `editable`
- Tab Item `hash`, `iconPosition`
- Wizard `actionClassName`, `bulkSubmit`, `affixFooter`, `stepsClassName`, `stepClassName`, `footerClassName`
- ApiSchema `dataType`, `qsOptions`, `concatDataFields`, `attachDataToQuery`

### 12.3 对 ERP "简单配置对应" 的覆盖率

| 映射类型 | 估算覆盖 | 说明 |
|---|---|---|
| **Simple Config (yes)** — amis 属性可直接重命名/语义映射到 Flux 属性 | ~70% | 包含 ✅ 已支持 + 🔴 新增后 |
| **Structural Change (no)** — 需 codegen 转换或范式差异 | ~25% | 主要在 action graph 替代、request 下沉、scope 隔离 |
| **不采纳** | ~5% | 有明确设计理由拒绝的项 |

### 12.4 已在现有 plan 中覆盖的项（10 项）

| 项 | Plan Phase |
|---|---|
| Table `autoFillHeight` | Phase 1 |
| Table/CRUD `selection.toggleOnRowClick` | Phase 2 |
| Button `tooltipPlacement` | Phase 3 |
| Button `countDown` / `countDownTpl` | Phase 3 |
| Page `asideResizable` / `asideMinWidth` / `asideMaxWidth` / `asideSticky` | Phase 4 |
| ApiSchema `responseType` / `downloadFileName` | Phase 5 |

### 12.5 未在现有 plan 中、建议新增的"简单配置"项（约 20 项）

这些是可以实现 1:1 amis→Flux 属性映射、不涉及架构变动的项：

| 组件 | 属性 | amis 类型 | 建议 Flux 属性 |
|---|---|---|---|
| CRUD | `alwaysShowPagination` | `boolean` | `pagination.alwaysShow?: boolean` |
| CRUD | `autoJumpToTopOnPagerChange` | `boolean` | `autoJumpToTopOnPagerChange?: boolean` |
| CRUD | `labelTpl` | `Template` | `selection.labelTpl?: string` |
| CRUD | `totalField` | `string` | `totalField?: string` |
| CRUD | `combineFromIndex` | `number` | `combineFromIndex?: number` |
| CRUD | `hideQuickSaveBtn` | `boolean` | `hideQuickSaveBtn?: boolean` |
| Table | `showHeader` | `boolean` | `showHeader?: boolean` |
| Table column | `headerAlign` | `enum` | `headerAlign?: 'left'\|'center'\|'right'` |
| Table column | `vAlign` | `enum` | `vAlign?: 'top'\|'middle'\|'bottom'` |
| Table column | `classNameExpr` | `string` | `classNameExpr?: string` |
| Table expandable | `expandableOn` | `string` | `expandable.expandableWhen?: string` |
| FormItem | `labelClassName` | `string` | `labelClassName?: string` |
| FormItem | `inputClassName` | `string` | `inputClassName?: string` |
| FormItem | `descriptionClassName` | `string` | `descriptionClassName?: string` |
| Button | `href` | `string` | `href?: string` |
| Button | `target` | `string` | `target?: string` |
| Tabs | `closable` | `boolean` | `closable?: boolean` |
| Tabs | `draggable` | `boolean` | `draggable?: boolean` |
| Tabs | `addable` | `boolean` | `addable?: boolean` |
| Dialog | `draggable` | `boolean` | `draggable?: boolean` |
| Dialog | `allowFullscreen` | `boolean` | `allowFullscreen?: boolean` |
| Wizard | `mode` | `enum` | `mode?: 'vertical'\|'horizontal'` |
| Wizard | `actionFinishLabel` | `string` | `actionFinishLabel?: string` |
| Wizard | `actionNextLabel` | `string` | `actionNextLabel?: string` |
| Wizard | `actionPrevLabel` | `string` | `actionPrevLabel?: string` |
| Wizard | `actionNextSaveLabel` | `string` | `actionNextSaveLabel?: string` |
| ApiSchema | `dataType` | `enum` | `dataType?: 'json'\|'form-data'\|'form'` |

---

## 附录 A: Flux 独有能力（amis 无对应）

以下 Flux 属性是 amis 没有的，属于 Flux 设计增强：

| 组件 | Flux 能力 | 说明 |
|---|---|---|
| BaseSchema | `frameClassName`, `classAliases`, `testid`, `frameWrap` | Tailwind 别名 + 测试 ID + 框装饰 |
| BaseSchema | `validateOn`, `showErrorOn` | 校验触发时机精确控制 |
| BaseSchema | `onMount` / `onUnmount` | 生命周期 action |
| BaseSchema | `xui:imports` | 声明式能力导入 |
| Form | `statusPath`, `valuesPath` | 只读状态发布 |
| Form | `rules` (结构化跨字段) | equalsField / notEqualsField |
| CRUD | `$crud` 只读摘要 | 语义稳定字段 |
| CRUD | `selectionOwnership` / `paginationOwnership` / `sortOwnership` / `filterOwnership` | 三态 ownership 模型 |
| CRUD | `columnSettings` | 结构化列管理 |
| CRUD | `responsive` | 响应式列折叠 |
| CRUD | `clientMode` | 前端模式配置对象 |
| CRUD | `polling` | 结构化轮询配置 |
| CRUD | `filterTogglable` | 查询区折叠 |
| CRUD | `pagination.mode: 'infinite'` | 无限滚动 |
| CRUD | `migrationHints` | AMIS 迁移辅助 |
| Table | `columnWidthsOwnership` / `columnWidthsStatePath` | 列宽 scope 持久化 |
| Table | `multiSort` | 多列排序 |
| Table | `popOver` (结构化对象) | cell 详情弹层 |
| Action | `then` / `onError` / `onSettled` / `parallel` | 动作链/失败/完成/并行 |
| Action | `when` (守卫) | 条件执行 |
| Action | `control: { retry, timeout, debounce, cacheTTL }` | 声明式控制 |
| Action | `component:setValue` / `component:focus` | 组件句柄 |
| Action | 命名空间方法 (`dict:getOptions` 等) | 模块化能力调用 |
| Dialog/Drawer | `open` / `defaultOpen` | 受控/非受控 |
| Dialog/Drawer | `onConfirm` / `onOpen` / `onClose` | lifecycle actions |
| Wizard | `linear` / `allowStepJump` | 线性/跳步控制 |
| Wizard | `beforeEnter` / `beforeLeave` (step 级) | 步骤生命周期 |
| Tabs | `orientation` / `variant` / `valueOwnership` | 布局/值控制 |

---

## 附录 B: 被拒绝项的详细理由

| amis 属性 | 拒绝理由 | Flux 替代方案 |
|---|---|---|
| `rowClassNameExpr` | 样式系统 marker class 原则（X3 §3） | cell 级 className 表达式 |
| `tableLayout` | 用样式系统控制，不开皮肤枚举 | className |
| `promptPageLeave` | 宿主路由职责（X3 §3） | `statusPath.dirty` + 宿主路由守卫 |
| `persistData` / `persistDataKeys` | 状态管理职责 | Zustand persist |
| `canAccessSuperData` | Flux `scopePolicy: 'form'` 硬隔离 | `rowData` 显式投影 |
| `hotKey` | 宿主/独立方案 | 宿主键盘映射 |
| `asyncApi` / `checkInterval` / `finishedField` | Flux 无长轮询 API | data-source `interval` + `stopWhen` |
| `wrapWithPanel` | Flux Form 无 panel 包装 | 显式 actions 区域 |
| `debug` / `debugConfig` | 独立 scope-debug renderer | `type: 'scope-debug'` |
| `export-csv` / `export-excel` | 后台职责，前端不做 | 后端导出 |
| `css` / `cssVars` / `mobileCSS` / `style` | 违反 styling contract | className / 设计 token |
| `syncResponse2Query` | Flux CRUD 总是替换数据 | — |
| `toolbarInline` | Flux toolbar 已用 flex 布局 | className |
| `pullRefresh` | 归移动端组件 | flux-renderers-mobile |
| `lazyRenderAfter` | 虚拟滚动已覆盖 | `virtualThreshold` |
| `itemBadge` | 低价值 | — |
| `maxKeepSelectionLength` | Flux 已删字段 | feature 级设计 |

---

*本文档基于 amis v6.13.1 schema.json 全量审计，覆盖 CRUD、Table、Table Column、Form、FormItem、Button、Page、Dialog、Drawer、Tabs、Tab Item、Wizard、Wizard Step、ApiSchema 共 14 个组件/定义的约 340+ 个 amis 属性。*
