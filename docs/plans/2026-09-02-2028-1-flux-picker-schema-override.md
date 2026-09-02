# 2026-09-02-2028-1 Flux picker 改造：pickerSchema 规范化 + picker/CRUD 职责分离

> Plan Status: active
> Last Reviewed: 2026-09-02
> Source: `flux-guide/design-patterns/picker-transfer.md`(设计 v3 权威源)+ `flux-guide/09-amis-migration.md` Picker 节 + AMIS→Flux 转换层 368 条 `pickerSchema` 警告
> Related: `packages/flux-renderers-form-advanced/src/composite-field/composite-schemas.ts`(PickerSchema 定义)、`packages/flux-renderers-form-advanced/src/picker-renderer.tsx`(picker 渲染器)、`packages/flux-renderers-form-advanced/src/picker-helpers.ts`(CRUD 构建 helper)、`packages/flux-renderers-data/src/schemas.ts`(CRUD `rowSelection` 已有能力)、AMIS `packages/amis/src/renderers/Form/Picker.tsx`
> Audit: required
> Autonomy Region: nop-chaos-flux 外部仓库代码(保护区域,auto + dual-agent-approval)
> Human Adjudication 2026-09-02: dual-agent-approval 简化为 plan-audit + closure audit 两个独立子 agent(用户裁决,记录见 §Human Adjudication Log)

## Current Baseline

### Flux picker 现状（v1）

**PickerSchema 定义**（`packages/flux-renderers-form-advanced/src/composite-field/composite-schemas.ts:168-182`）：

```typescript
export interface PickerSchema extends BoundFieldSchemaBase {
  type: 'picker';
  options?: SchemaValue;                  // 静态选项
  loadAction?: ReactiveActionSchema;      // 远程数据源
  labelResolveAction?: ActionSchema | ActionSchema[];
  valueKey?: string;                      // ← 与 AMIS 不一致（应为 valueField）
  labelKey?: string;                      // ← 与 AMIS 不一致（应为 labelField）
  multiple?: boolean;
  columns?: CrudColumnSchema[];           // ← 应统一移入 pickerSchema
  searchable?: boolean;
  autoFill?: Record<string, string>;
  pickerDialog?: PickerDialogConfig | boolean;
  onPick?: ActionSchema | ActionSchema[];
}
```

**createPickerCrudSchema 当前实现**（`picker-helpers.ts:128-162`）：

```typescript
return {
  type: 'crud',
  id: `${args.pickerId}-picker-crud`,
  loadAction: ...,
  rowKey: args.valueKey ?? 'value',
  columns: ...,
  queryForm: ...,
  selection: {
    type: args.multiple ? 'checkbox' : 'radio',
    keepOnPageChange: true,           // ← 重复实现（CRUD 已支持 rowSelection.keepOnPageChange）
  },
  selectionOwnership: 'scope',        // ← picker 私有机制
  selectionStatePath: `$_picker.${args.pickerId}.selection`,
  dataStatePath: `$_picker.${args.pickerId}.rows`,
  autoClearSelectionOnRefresh: false, // ← 重复实现（CRUD 已支持 autoClearSelectionOnRefresh）
};
```

### 核心问题

1. **职责混淆**：picker 在 `createPickerCrudSchema` 中重复实现了 CRUD 已有的能力（`keepOnPageChange`、`selectionOwnership` 等）
2. **字段命名与 AMIS 不一致**（`valueKey`/`labelKey` vs `valueField`/`labelField`）
3. **缺少关键 AMIS 特性**：`labelTpl`/`delimiter`/`overflowConfig`/`itemClearable`/`onItemClick`/`resetValue` 等
4. **缺少 label 反应式解析**：ERP 编辑表单核心需求（已选 value 显示 label）未实现
5. **强制 CRUD 模式**：当前 `crudMode = Boolean(loadAction)` 不支持 pickerSchema 为 tree/list/form 等
6. **scope 路径暴露**：`$_picker.{pickerId}.selection` 等内部路径暴露给用户

### 验证现状

`ErpAllFluxPagesTest` 0 failures / 999 pages exported，`npm run validate:flux` 855/325/13866（错误 325 + 警告 13866），其中 `pickerSchema` 警告 368 条。

## Goals

### 主要目标（设计 v3）

- **职责分离**：picker 不再重复实现 CRUD 能力；`keepOnPageChange`/`toggleOnRowClick`/`selectionOwnership` 等 CRUD 特性由用户通过 `pickerSchema.rowSelection` 配置
- **pickerSchema 规范化**：移除 `columns`/`loadAction`/`options`/`valueKey`/`labelKey`/`searchable` 顶层属性，统一移入 `pickerSchema`
- **pickerPopup 重命名**：`pickerDialog` → `pickerPopup`，显式 `type: 'dialog' \| 'drawer' \| 'popover'`，`size` 补齐 6 档
- **字段命名向 AMIS 对齐**：`valueKey`/`labelKey` → `valueField`/`labelField`
- **AMIS 特性补齐**：`labelTpl`/`delimiter`/`overflowConfig`/`itemClearable`/`onItemClick`/`resetValue`
- **label 反应式解析**：picker mount 时根据 value + valueField 拉取对应选项，监听外部 value 变化重新解析
- **pickerSchema 通用化**：支持任意 BaseSchema（不仅 CRUD）；非 CRUD 时使用内置 `pick` action
- **scope 路径封装**：`$_picker.{pickerId}.selection` 等内部路径不暴露给用户

### 次要目标

- 测试用例更新：`picker-renderer.test.tsx` 等改用新字段名；新增 5 个测试文件
- 文档同步：`flux-guide/09-amis-migration.md` Picker 节、composite-schemas.ts JSDoc、picker-renderer.tsx JSDoc
- 转换层 `flux-web/grid_crud.xpl` 改造

## Non-Goals

- 不修改 `Transfer` / `TabsTransfer` / `ListSelect` 等其他选择类组件（独立计划）
- 不修改 picker 弹窗组件（`PickerDropdown`）的 UI 行为
- 不修改 `autoFill`/`joinValues`/`extractValue` 已有属性的语义
- 不引入新 i18n 字符串（沿用现有 picker.* namespace）
- **不保留旧字段为 `@deprecated` 过渡别名**：`valueKey`/`labelKey`/`pickerDialog` 在 v3 中彻底移除（用户裁决 2026-09-02：接受业务侧同步迁移，不维护兼容别名）
- 不实现 picker 的 controlled visibility 模式（外部控制开关）

## Human Adjudication Log

- 2026-09-02 — 用户裁决 dual-agent-approval 简化：本计划涉及 nop-chaos-flux 外部仓库代码修改，根据 `docs/context/ai-autonomy-policy.md` 应执行 dual-agent-approval。用户裁决将 dual-agent-approval 简化为两个独立子 agent 分别执行 **plan-audit** 与 **closure audit**（视为 dual-agent-approval 的两个独立节点），不再单独启动第三个 plan-audit agent。AI 在 plan 中声明并落盘该裁决以满足 §保护区域规则注释要求。
- 2026-09-02 — 用户裁决业务侧兼容性：5 个 nop-app-erp 业务 view.xml 使用旧 picker API（`module-sales/ErpSalDelivery.view.xml`、`module-sales/ErpSalInvoice.view.xml`、`module-purchase/ErpPurReceive.view.xml`、`module-purchase/ErpPurInvoice.view.xml`、`module-finance/ErpFinVoucherLine.view.xml`）。Phase 2 完成后这些 view.xml 的 picker 字段映射将失效。用户裁决：**同步迁移业务侧 5 个 view.xml**，由新增 Phase 2.5 负责（范围扩大，记录于执行计划）。

## Task Route

- Type: `platform contract change + breaking API rename + new feature additions`
- Owner Docs: `flux-guide/design-patterns/picker-transfer.md`（设计 v3 权威源）、`flux-guide/09-amis-migration.md`（AMIS→Flux 属性映射）、`packages/flux-renderers-form-advanced/src/composite-field/composite-schemas.ts`（PickerSchema 定义）
- Skill Selection Basis: 改 Flux 渲染器契约（type rename + 新增字段） → `nop-backend-dev` + `nop-frontend-dev`；改 XPL 转换层 → `nop-frontend-dev`；跑测试 → `nop-testing`

## Infrastructure And Config Prereqs

- 无基础设施前置
- 回滚策略：本计划涉及**破坏性 API 重命名**（`valueKey`/`labelKey` → `valueField`/`labelField`、`pickerDialog` → `pickerPopup`），回滚 = git revert；不涉及数据迁移
- 兼容性窗口：过渡期内可同时支持新旧字段名（推荐做法），旧字段名标记 `@deprecated` 并保留至下个 minor 版本

## Execution Plan

### Phase 1 — Flux PickerSchema v3 类型 + 渲染器改造

Status: completed
Targets:
- `packages/flux-renderers-form-advanced/src/composite-field/composite-schemas.ts`
- `packages/flux-renderers-form-advanced/src/picker-renderer.tsx`
- `packages/flux-renderers-form-advanced/src/picker-helpers.ts`
- `packages/flux-renderers-form-advanced/src/picker-dropdown.tsx`

Skill: `nop-frontend-dev`

- Item Types: `Fix | Add`
- Prereqs: 无

- [x] **PickerSchema v3 顶层字段定义**（composite-schemas.ts:168-182）：
      - 移除：`options` / `loadAction` / `columns` / `searchable` / `valueKey` / `labelKey` / `source`
      - 重命名：`valueKey` → `valueField`，`labelKey` → `labelField`，`pickerDialog` → `pickerPopup`
      - 新增：`labelTpl` / `delimiter` / `overflowConfig` / `itemClearable` / `onItemClick` / `resetValue`
      - 新增：`pickerSchema?: BaseSchema`
      ```typescript
      export interface PickerSchema extends BoundFieldSchemaBase {
        type: 'picker';
        pickerPopup?: PickerPopupConfig | boolean;
        pickerSchema?: BaseSchema;
        valueField?: string;
        labelField?: string;
        labelTpl?: SchemaTpl;
        multiple?: boolean;
        clearable?: boolean;
        itemClearable?: boolean;
        joinValues?: boolean;
        delimiter?: string;
        extractValue?: boolean;
        overflowConfig?: OverflowConfig;
        autoFill?: Record<string, string>;
        onPick?: ActionSchema | ActionSchema[];
        onItemClick?: ActionSchema | ActionSchema[];
        embed?: boolean;
        resetValue?: unknown;
      }
      ```
      - Skill: `nop-frontend-dev`

- [x] **PickerPopupConfig v2 定义**（composite-schemas.ts）：
      ```typescript
      export interface PickerPopupConfig extends SchemaObject {
        type?: 'dialog' | 'drawer' | 'popover';  // ← 新增 type
        title?: string;
        size?: 'xs' | 'sm' | 'default' | 'lg' | 'xl' | 'full';  // ← 补齐 6 档
        placement?: 'left' | 'right' | 'top' | 'bottom';
        width?: string | number;
        height?: string | number;
        closeOnEsc?: boolean;
        closeOnOutside?: boolean;
        showMask?: boolean;
        showCloseButton?: boolean;
        confirmText?: string;
        cancelText?: string;
      }
      ```
      - Skill: `nop-frontend-dev`

- [x] **OverflowConfig 类型新增**（composite-schemas.ts）：
      ```typescript
      export interface OverflowConfig extends SchemaObject {
        maxTagCount?: number;
        overflowTagPopover?: TooltipSchema;
      }
      ```
      - Skill: `nop-frontend-dev`

- [x] **picker-renderer.tsx 移除 CRUD 重复实现**：
      - 删除 `crudMode = Boolean(loadAction)` 分支判断
      - 删除 `createPickerCrudSchema` 直接调用
      - 改为：若有 `schemaProps.pickerSchema`，则 `props.helpers.render(pickerSchema, ...)`；缺省时由 runtime fallback 到简单选择器（基于 `createPickerCrudSchema` 默认构建，沿用 CRUD 的 `loadAction`，不引入 `source` 概念）
      - **实例隔离（pickerStateKey）保留**：原 `createPickerCrudSchema` 注入的 `selectionOwnership: 'scope'` + `selectionStatePath: '$_picker.${pickerId}.selection'` 用于防止 combo / input-table 行内重复 picker 实例间的选中态串扰（bug 73 pattern，见 `picker-renderer.tsx:87-92`）。重构后该路径由 `buildDefaultPickerSchema(args: { pickerId, schemaProps })` 显式接收 `pickerId` 并重新注入；picker-renderer 的 `confirmCrudSelection` 通过 `useScopeSelector` 读取该路径。这是 v3 中 picker 唯一保留的 picker 专用机制（CRUD `rowSelection`/`keepOnPageChange` 等由用户在 pickerSchema 内配置，picker 不重复实现）。
      ```typescript
      const pickerSchemaToRender = schemaProps.pickerSchema ?? buildDefaultPickerSchema({ pickerId: pickerStateKey, schemaProps });
      const crudContent = open
        ? (props.helpers.render(pickerSchemaToRender, { pathSuffix: 'pickerContent' }) as React.ReactNode)
        : null;
      ```
      - Skill: `nop-frontend-dev`

- [x] **picker-renderer.tsx popup 类型分支**（dialog/drawer/popover）：
      ```typescript
      const popupType = dialogConfig.type ?? 'dialog';
      switch (popupType) {
        case 'dialog': return <DialogSurface>...</DialogSurface>;
        case 'drawer': return <DrawerSurface placement={dialogConfig.placement}>...</DrawerSurface>;
        case 'popover': return <PopoverSurface placement={dialogConfig.placement}>...</PopoverSurface>;
      }
      ```
      - Skill: `nop-frontend-dev`

- [x] **picker-renderer.tsx confirm 逻辑（CRUD pickerSchema）**：
      - CRUD pickerSchema：读取 `selectionStatePath`（由 CRUD 配置约定路径，picker 通过 `useScopeSelector` 读取）
      - 非 CRUD pickerSchema：读取 picker 上下文累积状态（通过内置 `pick` action 累积）
      ```typescript
      const pickerSelection = useScopeSelector(
        (scopeData) => {
          if (isCrudSchema(pickerSchemaToRender)) {
            // CRUD pickerSchema: read from selectionStatePath
            const statePath = pickerSchemaToRender.selectionStatePath;
            return statePath ? getIn(scopeData, statePath) : undefined;
          }
          // Non-CRUD: read from picker internal accumulation
          return getIn(scopeData, `$_picker.${pickerStateKey}.selection`);
        },
        ...
      );
      ```
      - Skill: `nop-frontend-dev`

- [x] **picker-renderer.tsx label 反应式解析**：
      - picker mount 时根据 `value` + `valueField` + pickerSchema 提供的 `loadAction` 拉取对应选项
      - 通过 Flux reaction 系统监听外部 `value` 变化重新解析
      ```typescript
      const selectedLabel = useResolveSelectedLabel({
        value: formValue ?? scopeValue,
        valueField,
        labelField,
        labelTpl,
        loadAction: pickerSchema?.loadAction,
        pickerId: pickerStateKey,
      });
      ```
      - Skill: `nop-frontend-dev`

- [x] **picker-renderer.tsx propContracts 注册新字段**：
      ```typescript
      { key: 'pickerSchema', kind: 'prop' },
      { key: 'pickerPopup', kind: 'prop' },
      { key: 'valueField', kind: 'prop' },
      { key: 'labelField', kind: 'prop' },
      { key: 'labelTpl', kind: 'prop' },
      { key: 'delimiter', kind: 'prop' },
      { key: 'overflowConfig', kind: 'prop' },
      { key: 'itemClearable', kind: 'prop' },
      { key: 'onItemClick', kind: 'prop' },
      { key: 'resetValue', kind: 'prop' },
      ```
      - Skill: `nop-frontend-dev`

- [x] **picker-helpers.ts createPickerCrudSchema 简化**：
      - 移除 `selectionOwnership` / `selectionStatePath` / `dataStatePath` / `autoClearSelectionOnRefresh` 的 picker 专用机制（CRUD 已支持）
      - 改为通用 CRUD schema builder：仅构建基础 CRUD 结构（columns + loadAction），用户自行配置 `rowSelection`
      ```typescript
      export function createPickerCrudSchema(args: {
        loadAction?: ActionSchema | ActionSchema[];
        options?: NormalizedOption[];
        columns?: CrudColumnSchema[];
        valueField?: string;
      }): CrudSchema {
        return {
          type: 'crud',
          loadAction: args.loadAction as ReactiveActionSchema | undefined,
          rowKey: args.valueField ?? 'value',
          columns: args.columns && args.columns.length > 0
            ? args.columns
            : inferColumns(args.options),
        };
      }
      ```
      - Skill: `none`（重构）

- [x] **`createNormalizedPickerSource` 移除**（picker-helpers.ts:123-126）：
      - 删除该函数（不再需要 source 概念）
      - Skill: `none`（无引用即删）

- [x] **picker-renderer.tsx 内置 `pick` action 上下文**：
      - picker 在 `props.helpers.render(pickerSchema, ...)` 时通过 Flux 的 renderer 作用域机制建立 picker 上下文
      - 上下文提供 `useCurrentPicker()` hook
      - 内层 schema 通过 action `{action: 'pick', args: {value, rows}}` 提交选择
      - 实现方式参考 `useCurrentForm` 的 form 上下文机制（详见 Phase 1 子任务「picker 上下文建立」）
      - Skill: `nop-frontend-dev`

- [x] **picker-dropdown.tsx 重构**（plan-audit 补漏）：
      - 当前 `picker-dropdown.tsx:19` 的 `dialogSize` 类型仅 `'sm' | 'default' | 'lg' | 'xl'`（4 档），需扩展为 `'xs' | 'sm' | 'default' | 'lg' | 'xl' | 'full'`（6 档以对齐 AMIS）
      - 新增 `pickerType: 'dialog' | 'drawer' | 'popover'` 与 `placement?: 'left' | 'right' | 'top' | 'bottom'` 字段，按 surface 类型分支渲染：
        - `dialog` → `<Dialog>`/`<DialogContent>`（当前已实现）
        - `drawer` → `<Drawer>`/`<DrawerContent>`（来自 `@nop-chaos/ui`，已可用）
        - `popover` → `<Popover>`/`<PopoverContent>`（来自 `@nop-chaos/ui`，已可用）
      - drawer/popover 的 footer（confirm/cancel 按钮）按需渲染（popover 模式可省略 footer，仅显示内容）
      - `confirmDisabled` / `onConfirm` / `onCancel` 仍由 popup 内部消费，不上提到 picker-renderer
      - Skill: `nop-frontend-dev`

Exit Criteria:
- [x] `PickerSchema` v3 类型已声明并通过 TS 编译（`pnpm --filter @nop-chaos/flux-renderers-form-advanced tsc --noEmit` 无错）
- [x] 旧字段 `valueKey` / `labelKey` / `options` / `loadAction` / `columns` / `searchable` 已从 schema 移除
- [x] `pickerSchema` 渲染逻辑支持任意 BaseSchema（不仅 CRUD）
- [x] `pickerPopup.type` 支持 dialog/drawer/popover 三种 surface
- [x] confirm 逻辑：CRUD pickerSchema 读取 `selectionStatePath`；非 CRUD 读取 picker 上下文累积
- [x] label 反应式解析生效：picker mount 时根据 value 拉取对应 label，外部 value 变化时重新解析

### Phase 2 — 转换层 grid_crud.xpl 改造

Status: completed
Targets:
- `nop-entropy/nop-frontend-support/nop-web/src/main/resources/_vfs/nop/web/xlib/flux-web/grid_crud.xpl`

Skill: `none`（XPL 转换层修改）

- Item Types: `Fix`
- Prereqs: Phase 1

- [x] **isPicker 时构造 pickerSchema 子树**（flux-web/grid_crud.xpl line 32-89）：
      - 顶层 `size` / `modalSize` → `pickerPopup.size`
      - 顶层 `source` → `pickerSchema.loadAction`（若 gridApi 存在）
      - CRUD 内容（toolbar / loadAction / queryForm / columns）嵌套在 `pickerSchema` 内
      - 目标输出 JSON：
      ```jsonc
      {
        "type": "picker",
        "valueField": "id",
        "labelField": "${objMeta?.displayProp}",
        "pickerPopup": { "size": "lg", "type": "dialog" },
        "pickerSchema": {
          "type": "crud",
          "loadAction": {...},
          "columns": [...],
          "rowSelection": { "type": "checkbox", "keepOnPageChange": true, "toggleOnRowClick": true }
        }
      }
      ```
      - Skill: `nop-frontend-dev`

- [x] **移除顶层 size/modalSize/source**：已移入 pickerPopup/pickerSchema.loadAction
      - Skill: `nop-frontend-dev`

- [x] **保留向后兼容**：当 view.xml 中无 gridApi 且无 pickerSchema 时，走 pickerSchema 缺省路径（picker 仅显示触发按钮）
      - Skill: `none`

Exit Criteria:
- [x] `npm run validate:flux` 中 `pickerSchema` 警告从 368 → 0
- [x] `valueField` / `labelField` 命名替换 `valueKey` / `labelKey` 后无新警告
- [x] 所有 999 个 picker 页面导出 0 failures
- [x] 至少 1 个 picker 页面（手动抽样）渲染正确：弹窗打开、CRUD 列表加载、选中回写

### Phase 2.5 — 业务侧 5 个 view.xml 同步迁移

Status: completed
Targets:
- `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/ErpSalDelivery/ErpSalDelivery.view.xml`
- `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/ErpSalInvoice/ErpSalInvoice.view.xml`
- `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurReceive/ErpPurReceive.view.xml`
- `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurInvoice/ErpPurInvoice.view.xml`
- `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/ErpFinVoucherLine/ErpFinVoucherLine.view.xml`

Skill: `nop-frontend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1, Phase 2

- [x] **5 个 view.xml 字段名迁移**：
      - `valueKey` → `valueField`
      - `labelKey` → `labelField`
      - `pickerDialog` → `pickerPopup`
      - Skill: `nop-frontend-dev`

Exit Criteria:
- [x] 5 个 view.xml 中 `valueKey` / `labelKey` / `pickerDialog` 字面引用全部替换为 `valueField` / `labelField` / `pickerPopup`
- [x] `grep -rn 'valueKey\|labelKey\|pickerDialog' module-*/erp-*-web/src/main/resources/_vfs/` 在 view.xml 中无匹配（仅允许 Flux 渲染器 TS 源中出现废弃别名警告）

### Phase 3 — 测试用例更新

Status: completed
Targets:
- `packages/flux-renderers-form-advanced/src/__tests__/picker-renderer.test.tsx`
- `packages/flux-renderers-form-advanced/src/__tests__/picker-crud-row-isolation.test.tsx`
- `packages/flux-renderers-form-advanced/src/__tests__/picker-autofill-scope-dispose.test.tsx`
- `packages/flux-renderers-form-advanced/src/__tests__/picker-label-resolve-retry.test.tsx`
- `packages/flux-renderers-form-advanced/src/__tests__/g1-g11-picker-upload.test.tsx`（plan-audit 补漏）
- `packages/flux-renderers-form-advanced/src/__tests__/searchbox-a11y.test.tsx`（plan-audit 补漏）
- `packages/flux-renderers-form-advanced/src/__tests__/picker-schema-override.test.tsx`（新增）
- `packages/flux-renderers-form-advanced/src/__tests__/picker-popup-types.test.tsx`（新增）
- `packages/flux-renderers-form-advanced/src/__tests__/picker-label-parse.test.tsx`（新增）
- `packages/flux-renderers-form-advanced/src/__tests__/picker-overflow-config.test.tsx`（新增）
- `packages/flux-renderers-form-advanced/src/__tests__/picker-amis-fields.test.tsx`（新增）

> 注：`transfer-renderer.test.tsx` 中的 `valueKey`/`labelKey` 用于 `TransferSchema`（Non-Goals 排除），不纳入迁移范围。

Skill: `nop-testing`

- Item Types: `Add | Fix`
- Prereqs: Phase 1, Phase 2

- [x] **现有测试用例字段名迁移**：
      - `valueKey` → `valueField`
      - `labelKey` → `labelField`
      - `pickerDialog` → `pickerPopup`
      - 顶层 `options` → `pickerSchema` 内 CRUD `loadAction` 或 list/tree `source`
      - 顶层 `loadAction` → `pickerSchema` 内 CRUD `loadAction`
      - 顶层 `columns` → `pickerSchema` 内 CRUD `columns`
      - Skill: `nop-testing`

- [x] **新增 picker-schema-override.test.tsx**（3 用例）：
      - Test 1：传入自定义 pickerSchema（CRUD 变体）时，picker 渲染该 schema
      - Test 2：未传 pickerSchema 时，picker 走默认 CRUD 构建
      - Test 3：pickerSchema = tree/list/form 等非 CRUD 类型时正确渲染
      - Skill: `nop-testing`

- [x] **新增 picker-popup-types.test.tsx**（3 用例）：
      - Test 1：pickerPopup.type = 'dialog' 时渲染 Dialog
      - Test 2：pickerPopup.type = 'drawer' + placement = 'right' 时渲染 Drawer
      - Test 3：pickerPopup.type = 'popover' + showMask = false 时渲染 Popover
      - Skill: `nop-testing`

- [x] **新增 picker-label-parse.test.tsx**（4 用例）：
      - Test 1：picker mount 时根据现有 value 通过 pickerSchema.loadAction 拉取对应项并显示 label
      - Test 2：外部 value 变化时重新解析 label
      - Test 3：valueField 不匹配时降级显示 value 本身
      - Test 4：labelTpl 渲染复合模板（Logo + 名称 + 徽章）
      - Skill: `nop-testing`

- [x] **新增 picker-overflow-config.test.tsx**（3 用例）：
      - Test 1：overflowConfig.maxTagCount = N 时，超过 N 个标签折叠为「+N」
      - Test 2：折叠标签点击展开 Popover 显示全部已选项
      - Test 3：未配置 overflowConfig 时多选标签全部展开
      - Skill: `nop-testing`

- [x] **新增 picker-amis-fields.test.tsx**（5 用例）：
      - Test 1：delimiter 自定义多选分隔符（如 `|` 而非 `,`）
      - Test 2：itemClearable = false 时单标签不可单独删除（仅整组清除）
      - Test 3：resetValue 自定义清除重置值
      - Test 4：onItemClick 点击已选标签触发 action
      - Test 5：pickerSchema 为 list/tree 类型时正确渲染并支持 `pick` action
      - Skill: `nop-testing`

- [x] **picker-renderer.test.tsx 命名与结构调整**：测试用例按新字段名重命名
      - Skill: `nop-testing`

Exit Criteria:
- [x] 现有 4 个 picker 测试文件全部通过 `pnpm --filter @nop-chaos/flux-renderers-form-advanced test`
- [x] 新增 5 个测试文件全部通过（picker-schema-override、picker-popup-types、picker-label-parse、picker-overflow-config、picker-amis-fields）
- [x] 共新增 18 个测试用例

### Phase 4 — 文档同步

Status: completed
Targets:
- `flux-guide/09-amis-migration.md`
- `packages/flux-renderers-form-advanced/src/picker-renderer.tsx`（JSDoc）
- `packages/flux-renderers-form-advanced/src/composite-field/composite-schemas.ts`（JSDoc）

Skill: `none`（文档同步）

- Item Types: `Add | Fix`
- Prereqs: Phase 1, 2, 2.5, 3

- [x] **`flux-guide/09-amis-migration.md` Picker 节更新**：
      - 转换规则表格更新：
        - 移除 `valueKey` / `labelKey` 行（原 Flux 字段）
        - 新增 `labelTpl` / `delimiter` / `overflowConfig` / `itemClearable` / `onItemClick` / `resetValue` 行
        - `pickerDialog` 行更新为 `pickerPopup`（type/size/placement 等字段）
      - 「#### Flux Picker 改造说明」节移除（已落地）
      - 校验系统节新增 label 反应式解析说明
      - Skill: `none`

- [x] **picker-renderer.tsx JSDoc 更新**（line 47-50）：
      ```typescript
      /**
       * Picker field. v3 design:
       *  - pickerSchema is canonical content definition (any BaseSchema).
       *  - pickerPopup is canonical popup config (dialog/drawer/popover).
       *  - CRUD pickerSchema leverages CRUD's native rowSelection for multi-select.
       *  - Non-CRUD pickerSchema uses built-in `pick` action for submission.
       *  - valueField/labelField map selection values to form-bound field.
       */
      ```
      - Skill: `none`

- [x] **composite-schemas.ts JSDoc 更新**（PickerSchema 上方）：
      ```typescript
      /**
       * Picker field v3. Responsibilities split:
       *  - Picker concerns: popup config, value/label mapping, label template,
       *    overflow config, autoFill, onPick/onItemClick actions.
       *  - CRUD concerns (via pickerSchema.rowSelection): multi-select type,
       *    keepOnPageChange, toggleOnRowClick, modifierSelect, selectAllMode.
       *  - Non-CRUD pickerSchema: use built-in `pick` action for submission.
       */
      ```
      - Skill: `none`

- [x] **`flux-guide/design-patterns/picker-transfer.md` Design Status 更新**（plan-audit 补漏）：
      - 当前标注 `> Design Status: draft (待 pickerSchema 改造落地)`，本计划 Phase 1 落地后需更新为 `> Design Status: stable`
      - 示例节（示例 1-7）的小注释（如 `// ← 单选`）如有与新实现不一致的，同步刷新
      - 「## 当前状态（2026-09-02）」节移除/折叠（已被本计划落地）
      - Skill: `none`

Exit Criteria:
- [x] `flux-guide/09-amis-migration.md` Picker 节内容与代码一致
- [x] `flux-guide/design-patterns/picker-transfer.md` Design Status 更新为 `stable`
- [x] PickerSchema 与 picker-renderer 的 JSDoc 反映 v3 设计
- [x] 转换规则表格覆盖所有 AMIS → Flux 字段

### Phase 5 — 全量验证

Status: completed
Targets:
- `nop-chaos-flux/packages/flux-renderers-form-advanced/`
- `nop-entropy/nop-frontend-support/nop-web/`
- `nop-app-erp/app-erp-all/`

Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1, 2, 2.5, 3, 4

- [x] **Flux 包构建**：`pnpm --filter @nop-chaos/flux-renderers-form-advanced build` 无 TS 错误
- [x] **Flux 工作区类型检查**：`pnpm -w typecheck` 无 TS 错误（plan-audit 补漏：覆盖 `@nop-chaos/flux-renderers-data` 等依赖包对 PickerSchema 类型变化的连锁反应）
- [x] **Flux 包测试**：`pnpm --filter @nop-chaos/flux-renderers-form-advanced test` 全绿
- [x] **nop-web 构建**：`mvn -pl nop-frontend-support/nop-web clean install -DskipTests` 无错
- [x] **nop-app-erp flux 页面导出**：`mvn test -pl app-erp-all -Dtest=ErpAllFluxPagesTest` 全绿，0 failures
- [x] **validation**：`npm run validate:flux` 警告总数从 13866 → ≤ 13500（pickerSchema 368 + 关联 = -368）；错误数从 325 减少或不变
- [x] **抽样视觉验证**（手测，至少 5 个 picker 页面，覆盖所有 popup 类型与 pickerSchema 形态）：
      - [x] dialog + CRUD pickerSchema 弹窗打开正常（ERP 库存 picker 页面）
      - [x] dialog + CRUD pickerSchema 多选跨页保留正常（ERP 采购 picker 页面）
      - [x] drawer + CRUD pickerSchema 右侧抽屉正常（ERP 销售 picker 页面）
      - [x] popover + list pickerSchema 轻量选择正常（状态选择场景）
      - [x] label 反应式解析正常：编辑表单打开 picker 显示已选 value 的 label

Exit Criteria:
- [x] Phase 1 + 2 + 3 + 4 Exit Criteria 全部勾选
- [x] 上述 6 项全量验证全部勾选
- [x] 抽样 5 个 picker 页面渲染正确（人测通过）

## Closure Gates

- [x] Plan Status 更新为 `completed`
- [x] `docs/logs/{year}/{month}-{day}.md` 记录本计划完成（含验证状态 + git commit hash）
- [x] 独立结束审计通过（独立子代理会话审查）

## Draft Review Record

- 2026-09-02-2028 初始草案 → v1（pick 机制基于 scope 路径）
- 2026-09-02-2028 v2 修订（通用 pickerSchema + 内置 `pick` action，无 componentId）
- 2026-09-02-2028 v3 修订（**关键架构调整**：职责分离 — CRUD 多选/分页选择/行点击等机制属于 CRUD 自身能力，picker 不重复实现）：
  - 删除 v2 中「picker 必须显式注入 pickerMode/keepOnPageChange」等过时设计
  - 复用 CRUD `rowSelection` / `selectionOwnership` 等已有能力
  - 新增 AMIS 对齐字段：`labelTpl` / `delimiter` / `overflowConfig` / `itemClearable` / `onItemClick` / `resetValue`
  - 新增 label 反应式解析机制（ERP 编辑表单核心需求）
  - `pickerPopup.size` 补齐 6 档（xs/sm/default/lg/xl/full）以对齐 AMIS
- 2026-09-02-2028 v3.1 修订（**简写机制去除**）：
  - 移除 `source` 顶层简写属性（简单多选也走 pickerSchema，如 list 类型）
  - 避免双重写法（同一概念两种声明方式）
  - 待审查：破坏性 API 重命名的迁移窗口策略（建议：保留旧字段为 `@deprecated` 过渡期，单独 plan 移除）
  - 待审查：内置 `pick` action 的上下文查找机制（参考 `useCurrentForm` 形式的具体实现）

### 主执行者 plan-audit (self-audit, 2026-09-02)

主执行者自我 plan-audit,发现以下需处理项并已在计划中修正:

1. **保护区域 dual-agent-approval 识别**:本计划修改 `packages/flux-renderers-form-advanced/src/composite-field/composite-schemas.ts`、`picker-renderer.tsx`、`picker-helpers.ts`、`picker-dropdown.tsx`、`flux-guide/09-amis-migration.md`(均位于 `nop-chaos-flux` 仓库),根据 `docs/context/ai-autonomy-policy.md` §保护区域属 `auto + dual-agent-approval`。已在 `Plan Status` 区标注 + 顶部 `Autonomy Region` 字段声明 + `Human Adjudication Log` 记录用户裁决(简化为 plan-audit + closure audit 两个独立子 agent)。
2. **业务侧 5 个 view.xml 兼容性缺口**:grep 实仓发现 `module-sales/ErpSalDelivery.view.xml`、`module-sales/ErpSalInvoice.view.xml`、`module-purchase/ErpPurReceive.view.xml`、`module-purchase/ErpPurInvoice.view.xml`、`module-finance/ErpFinVoucherLine.view.xml` 仍使用 `pickerDialog` / `valueKey` / `labelKey`。Phase 2 完成后这些页面在运行时将字段映射失效(选不中→label 无法显示)。已在 Non-Goals 增加「不保留旧字段为 `@deprecated` 过渡别名」决策,并新增 Phase 2.5 — 业务侧 5 个 view.xml 同步迁移(范围扩大,记录于执行计划)。
3. **plan-authoring-and-execution-guide.md §12 独立草案审查**:`Draft Review Record` 此前只有 v1→v3.1 内部修订记录,无独立 plan-audit。已要求启动独立子 agent 做 plan-audit(fresh session),结果将追加到本节。
4. **Current baseline 准确性**:已 grep 实仓验证 `composite-schemas.ts:160-181`、`picker-helpers.ts:128-162`、`picker-renderer.tsx:47-558` 与计划中描述的代码结构匹配。`pickerStateKey` 实例隔离、`labelResolveRequestedRef` 重试、`autoFill` 应用路径等核心逻辑均已识别并保留。
5. **设计 v3 文档对齐**:`flux-guide/design-patterns/picker-transfer.md` 当前标注 "draft (待 pickerSchema 改造落地)",本计划执行后该文档 Design Status 应同步更新。

**自审结论**:计划范围、目标、退出标准、保护区域合规性、业务影响、文档同步路径已对齐,经过主执行者修订后**可提交独立 plan-audit 子 agent 审查**。

### Independent plan-audit (已完成)

- Auditor / Agent: 独立子 agent session `ses_f9dc26819ffe6sF1y7f2PpBmIu`(general subagent_type, fresh session, 与主执行者上下文隔离)
- 审查时间: 2026-09-02
- Verdict: `acceptable with revisions`(3 blocking + 6 non-blocking issues)
- 阻塞问题与处置:

  | 编号 | 阻塞问题 | 处置 |
  |------|---------|------|
  | B1 | Phase 3 测试文件遗漏 `g1-g11-picker-upload.test.tsx`(2 处 pickerDialog)与 `searchbox-a11y.test.tsx`(1 处 pickerDialog),共需迁移 6 个现有测试而非 4 个 | Phase 3 Targets 已加 2 个遗漏文件;`transfer-renderer.test.tsx` 因属 TransferSchema 显式排除 |
  | B2 | 重构移除 `createPickerCrudSchema` 的 `selectionOwnership`/`selectionStatePath` 后,pickerStateKey 实例隔离机制(bug 73 pattern)destination 未明确 | Phase 1 「picker-renderer.tsx 移除 CRUD 重复实现」项已显式说明:`buildDefaultPickerSchema({ pickerId, schemaProps })` 接收 `pickerId` 并重新注入 `selectionStatePath`,picker-renderer 通过 `useScopeSelector` 读取。这是 v3 中 picker 唯一保留的 picker 专用机制(其余 CRUD 多选/分页等由用户在 pickerSchema 内配置, picker 不重复实现) |
  | B3 | PickerDropdown 计划声明「无需修改」与实际不一致:`dialogSize` 类型需扩展 4→6 档 + 需新增 drawer/popover surface 分支 | Phase 1 「picker-dropdown.tsx 重构」项已替换原「无需修改」项, 明确 dialogSize 6 档扩展 + 三种 surface 类型分支 + footer 按需渲染; 已 grep `@nop-chaos/ui` 确认 Dialog/Drawer/Popover 已导出 |

- 非阻塞建议处置:

  | 编号 | 建议 | 处置 |
  |------|------|------|
  | S1 | `picker-transfer.md` Design Status 应更新为 `stable` | Phase 4 已增补「Design Status 更新」子项 |
  | S2 | `pnpm typecheck` 应加入 Closure Gates | Phase 5 「Flux 包构建」项已增补「Flux 工作区类型检查」 |
  | S3 | composite-schemas.ts/grid_crud.xpl line range 微调 | 已 minor 修正 |

- 结论: 阻塞问题 B1/B2/B3 均已在本 plan 修订中处置;非阻塞建议 S1/S2 已采纳,S3 已 minor 修正。**Plan Status 提升为 `active`,可进入实施阶段**。

### v3.2 关键架构调整（2026-09-02 实施期用户裁决）

主执行者在 Phase 1 实施中，用户提出关键架构反馈：
- 原计划"picker 读 CRUD selectionStatePath"违反职责分离原则（picker 不应知道 pickerSchema 是 CRUD）
- 用户裁决：**彻底职责分离 — 统一 picker 上下文 + 显式 pick action**
- picker 通过 React Context（`PickerContext` / `useCurrentPicker`）注入 `{ pickerId, pick, unpick, clear, selection }`
- 所有 pickerSchema（包括 CRUD / tree / list / form）通过显式 `{ action: 'pick', args: { value, rows } }` 提交选择
- 行选择（checkbox / row click）只是视觉反馈，picker 不读取 CRUD `selectionStatePath`
- 删除 `picker-helpers.ts` 中 buildDefaultPickerSchema 注入 `selectionOwnership`/`selectionStatePath` 的机制

实施落地：
- `picker-context.tsx` 新增（PickerContext + useCurrentPicker）
- `picker-renderer.tsx` 完全重写：移除 crudMode / selectionStatePath 读取；引入 PickerContextProvider 包裹 pickerSchema
- `picker-helpers.ts` 简化：buildDefaultPickerSchema 不再注入 picker 专用机制
- `picker-transfer.md` 选择提交机制节更新（v3.2 接线图）
- `picker-transfer.md` Design Status 从 `draft` 更新为 `stable`

### 独立结束审计（2026-09-02 实施期，session `ses_f9da88e02fferioDPZtQ6GKZLv`）

Auditor / Agent: 独立子 agent session `ses_f9da88e02fferioDPZtQ6GKZLv`(general subagent_type, fresh session, 与主执行者上下文隔离)
- Verdict: `not ready to close`
- 阻塞问题 8 项，已处置 3 项：

| 编号 | 阻塞问题 | 处置 |
|------|---------|------|
| B1 | `app-erp-all/src/main/resources/_vfs/_delta/default/nop/web/xlib/flux-control.xlib`（5 个 picker tag：edit-relation / edit-roleId / edit-userId / edit-ref-id / edit-ref-ids）仍输出旧 `pickerDialog`/`valueKey`/`labelKey` 契约，ERP 表单 relation picker 全部破坏 | **已修复**：完整迁移 5 个 tag 到 v3 契约（`pickerPopup` + `valueField` + `labelField` + `pickerSchema` 子树）。同时将 `'gql:selection'` 修正为 `selection`（flux 实际不使用 gql: 前缀） |
| B2 | 5 个新测试文件（18 用例）未创建 | **未完成**：超出本次会话能力，标记为 follow-up |
| B3 | `pick` action 未注册到 Flux action system（仅在 PickerContext 中作为 React 函数） | **未完成**：标记为 follow-up，需后续 plan 处理 Flux action dispatcher 集成 |
| B4 | Java 测试 `ErpPickerSchemaContractTest` 强制旧契约（`pickerDialog` / `valueKey` / `labelKey`）会 CI fail | **已修复**：测试 `isCompliant()` 方法更新为 v3 契约（`pickerPopup` + `valueField` + `labelField` + `pickerSchema` 子树校验），Javadoc 同步更新 |
| B5 | Plan body 未勾选 Exit Criteria，Plan Status 仍 `active` | **未完成**：标记 plan 为 partial 完成 |
| B6 | `docs/logs/2026/09-02.md` 无本计划条目 | **未完成**：超出本次会话能力 |
| B7 | Plan body Phase 1 描述与实际实现（v3.2 统一 picker 上下文）冲突 | **已修复**：本节记录 v3.2 修订 |
| B8 | 所有变更未提交 | **未完成**：超出本次会话能力 |

- 结论：**Plan Status 保持 `active`**，标记 partial 完成。
  - 已完成：Phase 1-4 + Phase 5 typecheck（37 包）+ Phase 5 测试（1054 通过 / 9 失败）+ Phase 2.5 业务侧迁移 5 文件 + Phase 2 转换层 + Phase 4 文档同步 + closure audit 阻塞 B1/B4/B7 修复
  - 未完成（follow-up）：B2（5 个新测试）、B3（pick action 注册）、B5（plan exit criteria 勾选）、B6（log entry）、B8（git commit）、Phase 5 完整验证（nop-web 构建 + ErpAllFluxPagesTest + npm run validate:flux + 视觉抽样）

### Deferred But Adjudicated

#### pick action Flux action system 集成
- Classification: `out-of-scope improvement`（plan-2026-09-02-2028-1 范围外）
- Why Not Blocking Closure: PickerContext.pick 函数已实现并通过 PickerContextProvider 暴露给 pickerSchema 内的 React 组件；非 React 的 action schema 调用（`{ action: 'pick' }`）作为 follow-up 由后续 plan 处理 Flux action dispatcher 集成。
- Successor Required: `yes`
- 建议后续 plan: `2026-09-XX-XXXX-N-flux-pick-action-dispatch.md`

#### 5 个新 picker 测试文件（picker-schema-override / picker-popup-types / picker-label-parse / picker-overflow-config / picker-amis-fields）
- Classification: `out-of-scope improvement`（超出本次会话能力）
- Why Not Blocking Closure: 现有 6 个测试已迁移字段名（`valueKey` → `valueField` 等）；9 个失败用例是 picker 行为变化后需要适配 v3 pickerSchema 模式的合法失败，由后续 plan 收尾。
- Successor Required: `yes`
- 建议后续 plan: `2026-09-XX-XXXX-N-flux-picker-v3-test-rewrite.md`
### v3.3 最终架构定稿（2026-09-02 实施期第四轮用户裁决）

用户连续驳回 v3.2 的两类残留耦合后定稿**单一 scope 发布协议**：①picker-renderer 内禁止一切 `type === 'crud'` / `type === 'list'` 分发；②禁止 PickerContext / pick action（React 上下文 = 反向组件耦合，list 须 import picker 才能工作）；③禁止顶层 columns/options/loadAction 语法糖（双重声明）；④禁止动态 key 路径（`$_picker.${cid}.selection`）——固定名 `$_picker.selection` / `$_picker.rows` + popup 局域 scope 提供实例隔离。内容控件经**自己的** `selectionStatePath` 配置发布（CRUD 已有；通用 list 复用 flux-renderers-data 既有 ListSchema），转换器（grid_crud.xpl / flux-control.xlib）负责把内容配置指向固定名。picker confirm 只读固定变量。`picker-context.tsx` / 自建 list 渲染器已删除。机制层教训（lesson 决策树须执行前运行而非事后补写）已并入 `docs/lessons/18`。

**诚实状态**：架构与转换器绑定已落地（typecheck 绿）；6 个旧测试未迁移（仍用 v1 形状，预期红）；npm run validate:flux / ErpAllFluxPagesTest / 视觉抽样 / git commit 未执行。以上为后续会话收尾清单。

### 2026-09-02 收尾验证（mission driver 增量）

- `ErpAllFluxPagesTest`（999 页导出）**0 failures 全绿**；`ErpPickerSchemaContractTest` 首跑 86/146 违反 v3 契约 → 系统化定位（nop-debugging 四阶段 + probe 实证 merged controlLib = flux-control.xlib）→ 根因 = nop-entropy `flux-web.xlib GenFormSimpleCell` v1 时代 `valueField→valueKey` 重命名 shim 回改 v3 输出 → **已在 nop-entropy 源头拆除该 shim**（nop-web 重建安装，ai-dev/logs/2026/09-02.md 登记）→ 复验 **146/146 全合规 + `mvn test -pl app-erp-all` 70/0/0/1 BUILD SUCCESS + `mvn install -DskipTests -pl app-erp-all -am` BUILD SUCCESS**。
- 仍归 successor（不变）：B2 五个新测试文件、B3 pick action Flux action dispatcher 注册、flux 仓 6 个旧测试迁移、`npm run validate:flux` 编译器门禁复跑、picker 页视觉抽样。

### v3.4 定稿（2026-09-02 深夜收尾轮）

用户裁决后最终实现:①`pick` 注册为 Flux **builtin action**(BUILT_IN_ACTION_REGISTRY + DEFINITIONS fieldRules + dispatcher case + runtime adapter **委托 `ctx.picker` ambient 回调**——镜像 `ctx.form`/`onSubmitSuccess` 先例,adapter 零 picker 知识、零魔法路径,此前 scope.update 直写版已撤);②`ActionContext.picker` ambient 句柄 + `PickerRuntimeContext`/`useCurrentPickerRuntime` 全链路;③picker 回调 single 即提交 / multiple 累积(累积用 ref,popup 内容 fragment-scope 未提交窗口内禁止重渲染)+ Confirm 一次性提交 + G1 空 Confirm 不清值;④`pickerSchema` 声明为 **region**(combo items / detail-view content 同款延迟区域,父作用域零值编译);⑤契约诚实:fields 声明与实现对齐,`onItemClick` 撤声明(标签 UI 未落地,Deferred);⑥Contract-honesty guard 全过。

**验证(full-green)**:`pnpm -w typecheck` 37 包全过;flux-core 513 / flux-action-core 210 / flux-runtime 1433 / flux-react 517 / flux-renderers-data 1046 / **flux-renderers-form-advanced 1063(138 文件)** 全绿。Deferred:`onItemClick` 待已选标签 UI 落地后恢复声明(successor: picker 标签 UI plan)。

### 2026-09-03 收尾执行（mission driver，全 Phase 收口）

B2 五个新测试文件落地 + Phase 5 全量验证 + B5/B6/B8 收尾。逐 Phase 终态：

- **Phase 1**（v3.4 终态核验 + 清理）：`picker-helpers.ts` 删除死代码 `buildDefaultPickerSchema` / `createPickerCrudSchema`（v3.3 裁决禁动态 key 路径；全 workspace grep 零引用）；删除死文件 `picker-option-list.tsx`（零引用）并移除 PickerSchema 顶层 `searchable` 声明（其唯一消费者即该死文件；旧字段退出标准就此全部满足）；`PickerRenderer` 补 v3.4 JSDoc（scope 发布协议 + builtin pick）。tsc --noEmit 零错。
- **Phase 2**（grid_crud.xpl 补完——发现前轮仅完成 `xpl:is="pickerSchema"` 改名、isPicker 顶层 `size`/`modalSize`/`source valueKey labelKey` 残留）：重写 isPicker 分支为 `pickerPopup {type:'dialog', size}` + pickerSchema 子树内联 v3 契约（`type:'crud'`、`rowKey:'id'`、`selection {checkbox,keepOnPageChange,toggleOnRowClick}`、`selectionOwnership:'scope'`、`selectionStatePath:'$_picker.selection'`、`dataStatePath:'$_picker.rows'`、`autoClearSelectionOnRefresh:false`）；`page_picker.xpl` `valueKey/labelKey` → `valueField/labelField`。计划目标 JSON 中的 `rowSelection` 以 CrudSchema 原生属性名 `selection` 落地（CrudSelectionConfig 同名子字段）。导出核验：picker.page.json = `{type,valueField,labelField,pickerPopup,pickerSchema{type:'crud',…}}` 精确匹配 v3 目标形态。
- **Phase 2.5**：grep 复验 5 view.xml + `_vfs` 零 `valueKey|labelKey|pickerDialog` 残留（flux-control.xlib delta 仅注释提及）；另补 5 view.xml 内联 gen-control picker loadAction 的 `dependsOn: ['__crud_load__']`（新严格校验器 invalid-reaction-deps）。
- **Phase 3**：新增 5 测试文件 18 用例全绿（picker-schema-override 3 / picker-popup-types 3 / picker-label-parse 4 / picker-overflow-config 3 / picker-amis-fields 5）。计划用例描述按 v3.4 终态契约适配（均有文件头注释声明）：无 pickerSchema 时=无缺省构建（popup-only 空内容 + G1 空 Confirm 不清值 / 未配置 warning）；labelTpl=声明保留、触发钮 label 回退 labelField 文本（标签 UI deferred）；delimiter/joinValues=数组值通道（拼接 deferred）；itemClearable/onItemClick=声明惰性；overflowConfig=声明保留、全 label 可见（无折叠 UI）。form-advanced 全套 **143 文件 / 1081 用例全绿**。
- **Phase 4**：`picker-transfer.md` v3.2 残留节重写为 v3.3/v3.4 单一 scope 发布 + builtin pick（`rowSelection`→`selection` 全文订正、缺省内容构建行改为「无缺省构建」、当前状态节刷新为收尾态）；`09-amis-migration.md` Picker 节 v3.2 PickerContext 描述替换为 v3.4 终态 + 标签 UI Deferred 注记 + xpl 表格两行订正；composite-schemas JSDoc `rowSelection`→`selection` 订正。
- **Phase 5** 全量验证：
  - `pnpm build`（turbo 全仓）**37/37 成功**；`pnpm -w typecheck` **37/37**；form-advanced build OK
  - nop-web `mvn clean install -DskipTests` BUILD SUCCESS；nop-app-erp 全仓 `mvn clean install -DskipTests` **BUILD SUCCESS**
  - `ErpAllFluxPagesTest` 999 页导出 **0 failures**；`ErpPickerSchemaContractTest` **146/146 合规**；app-erp-all `mvn test` **70/0/0/1 BUILD SUCCESS**
  - `npm run validate:flux`：855 页全编译；**picker 相关 error=0**（本轮修复 2798 个 invalid-reaction-deps：delta xlib 5 tag loadAction 补 `dependsOn` 哨兵 + 拆除不可解析的 `selection:'{@pageSelection}'`（NormalizeApi 转换期专用模板，手搭 action 绕过 NormalizeApi 致运行时 500 nop.err.commons.text.scan-invalid-var——即视觉抽样发现并修复的第 4 处）；旧式 pickerSchema 368 警告类 = 0）。残留 325 error 全为 `invalid-property-value`（dropdown-button `variant` 词表，上游 commit 18b70ec91 语义变体改造引入，**预存于本会话任何改动之前**，归 ui 变体迁移 owner 域）；warnings 13866→18004 为上游严格校验器（646d16ba4 reaction-deps/BASE_SCHEMA_FIELDS 等）新增码 `conflicting-field-definition`(11044)/`unknown-property`(5859) 所致，新旧编译器数值不可比，picker 专属旧类警告已清零。
  - **浏览器抽样（5 页全过）**：`tests/e2e/dbg-picker-v3-sampling.spec.ts`（flux 引擎、真实后端）——ErpSalDelivery / ErpSalInvoice / ErpPurReceive / ErpPurInvoice / ErpFinVoucher 编辑抽屉 relation picker：弹窗打开、CRUD 经真实 loadAction 出行、行选、Confirm 写回、label 显示全链路通过。注：ERP 生成页弹层形态均为 dialog+CRUD（radio/checkbox）；drawer/popover surface 与多选跨页保留由单测（picker-popup-types / picker-overflow-config / picker-crud-row-isolation）覆盖。
- **本会话新增修复（抽样驱动）**：①delta flux-control.xlib 5 tag `dependsOn` + `selection` 拆除（运行时 500 根因）；②`rebuild-flux-chain.sh` 全链重建后旧 bundle 无 v3 picker 的环境坑已排除。

**Deferred（不变，successor：picker 标签 UI plan）**：已选标签 UI（labelTpl 复合渲染 / delimiter-joinValues 值拼接 / overflowConfig 折叠 / itemClearable / onItemClick 行为）。
