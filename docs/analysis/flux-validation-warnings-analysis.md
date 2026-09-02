# Flux 页面验证 38498 Warnings 根因分析

## 执行摘要

38498 warnings 分为 **4 类根因**，涉及 nop-chaos-flux（渲染器定义）和 nop-entropy（转换层）两个仓库。

---

## 根因 1：META_FIELDS 缺失 BaseSchema 基础属性

**影响数量**：~15000+（`name`、`label`、`title` 等）

### 问题

`flux-core/src/constants.ts` 定义的 `META_FIELDS` 只包含：

```typescript
export const META_FIELDS = new Set([
  'id', 'className', 'frameClassName',
  'when', 'visible', 'hidden', 'disabled',
  'testid', 'frameWrap',
]);
```

但 `BaseSchema`（`flux-core/src/types/schema.ts:102-122`）定义了更多基础属性：

```typescript
export interface BaseSchema extends SchemaObject {
  type: string;
  id?: string;
  name?: string;      // ← 不在 META_FIELDS 中
  label?: string;     // ← 不在 META_FIELDS 中
  title?: string;     // ← 不在 META_FIELDS 中
  className?: string;
  frameClassName?: string;
  when?: boolean | string;
  visible?: boolean | string;
  hidden?: boolean | string;
  disabled?: boolean | string;
  // ...
}
```

`getAcceptedSchemaKeys()` 只添加 `META_FIELDS`，不添加 `BaseSchema` 的 `name`/`label`/`title`。

### 受影响渲染器

| 渲染器 | 缺失属性 | warnings 数量 |
|--------|----------|---------------|
| `form` | `name` | 1068 |
| `page` | `name` | 429 |
| `text` | `label` | 8497 |
| `button` | `label` | (在 propContracts 中，已接受) |
| `mapping` | `name` | 260 |
| 所有渲染器 | `title` | 40 |

### 修复方案

**方案 A（推荐）**：在 `getAcceptedSchemaKeys()` 中添加 `BaseSchema` 基础属性：

```typescript
// flux-compiler/src/schema-compiler/shape-validation-utils.ts
const BASE_SCHEMA_KEYS = ['type', 'name', 'label', 'title'];

export function getAcceptedSchemaKeys(renderer: RendererDefinition): Set<string> {
  const keys = new Set<string>(BASE_SCHEMA_KEYS);
  // ... 现有逻辑
}
```

**方案 B**：将 `name`/`label`/`title` 加入 `META_FIELDS`。

---

## 根因 2：AMIS → Flux 属性名未转换

**影响数量**：~5000+

### 问题

flux-web.xlib 转换层没有处理 AMIS 到 Flux 的属性名映射：

| AMIS 属性 | Flux 属性 | 渲染器 | warnings 数量 |
|-----------|-----------|--------|---------------|
| `level: "primary"` | `variant: "primary"` | `button` | 2521 |
| `level: "primary"` | `variant: "primary"` | `dropdown-button` | 325 |
| `visibleOn: "..."` | `when: "..."` | 所有渲染器 | 319 |
| `valueField: "id"` | `valueKey: "id"` | `picker` | 368 |
| `labelField: "name"` | `labelKey: "name"` | `picker` | 368 |
| `source: {url:...}` | `loadAction: {action:'ajax', args:{url:...}}` | `picker` | 368 |

### 证据

**button.level**：flux button 定义（`flux-renderers-basic/src/basic-renderer-definitions.ts:240-256`）只有 `variant`，没有 `level`。

**picker.valueKey**：flux picker 定义（`flux-renderers-form-advanced/src/composite-field/composite-schemas.ts:173`）是 `valueKey`，不是 `valueField`。

**visibleOn**：flux-core 中不存在 `visibleOn` 属性，应该用 `when`。

### 修复方案

在 flux-web.xlib 的转换层中添加属性名映射：

```xml
<!-- 在 impl_GenForm.xpl 或公共辅助中 -->
<xpl:macro name="ConvertAmisProps">
  <!-- level → variant -->
  <c:if test="${node.level != null}">
    <variant>${node.level}</variant>
    <c:remove var="node.level"/>
  </c:if>
  <!-- visibleOn → when -->
  <c:if test="${node.visibleOn != null}">
    <when>${node.visibleOn}</when>
    <c:remove var="node.visibleOn"/>
  </c:if>
  <!-- valueField → valueKey -->
  <c:if test="${node.valueField != null}">
    <valueKey>${node.valueField}</valueKey>
    <c:remove var="node.valueField"/>
  </c:if>
  <!-- labelField → labelKey -->
  <c:if test="${node.labelField != null}">
    <labelKey>${node.labelField}</labelKey>
    <c:remove var="node.labelField"/>
  </c:if>
</xpl:macro>
```

---

## 根因 3：api 属性重复生成

**影响数量**：519

### 问题

`page_simple.xpl` 和 `container_simple.xpl` 生成了 **both** `api` 和 `submitAction`：

```xml
<!-- page_simple.xpl:19-28 -->
<comment>flux form 用 submitAction 提交（非 AMIS 的 api）。把 page/form 的 api 转成 flux ActionSchema</comment>
<if test="${api != null}">
  <submitAction action="ajax" args="..."/>
</if>

<!-- 但同时又生成了 api（line 82）-->
<api xpl:attrs="xpl('thisLib:NormalizeApi',api,genScope)" xpl:if="api"/>
```

### 修复方案

删除 `<api>` 标签，只保留 `<submitAction>`：

```xml
<!-- page_simple.xpl:82 - 删除此行 -->
<!-- <api xpl:attrs="xpl('thisLib:NormalizeApi',api,genScope)" xpl:if="api"/> -->
```

---

## 根因 4：渲染器定义缺失属性

**影响数量**：~18000

### 4.1 CRUD 缺失属性

| 属性 | 用途 | warnings 数量 | 来源 |
|------|------|---------------|------|
| `headerClassName` | 表头样式 | 462 | grid_crud.xpl:44 硬编码 |
| `bodyClassName` | 内容区样式 | 462 | grid_crud.xpl:45 硬编码 |

**修复**：在 `crud-renderer-definition.ts` 的 `fields` 中添加：

```typescript
{ key: 'headerClassName', kind: 'prop' },
{ key: 'bodyClassName', kind: 'prop' },
```

### 4.2 Text 缺失属性（表单字段上下文）

| 属性 | warnings 数量 | 说明 |
|------|---------------|------|
| `label` | 8497 | CRUD 列和视图对话框中的 text 字段 |
| `required` | 4717 | 视图对话框中的必填标记 |
| `readOnly` | 1313 | 只读字段 |

**说明**：`text` 在 AMIS 中继承表单字段 chrome，但在 flux 中是纯内容渲染器。这些属性属于 `BoundFieldSchemaBase`。

**修复方案**：在 `basic-renderer-definitions.ts` 的 text fields 中添加：

```typescript
{ key: 'label', kind: 'prop' },
{ key: 'required', kind: 'meta', valueType: 'boolean' },
{ key: 'readOnly', kind: 'meta', valueType: 'boolean' },
```

### 4.3 Picker 缺失属性

| 属性 | warnings 数量 | 说明 |
|------|---------------|------|
| `hiddenFieldPolicy` | 509 | 隐藏字段策略 |
| `size` | 368 | 应转换为 `pickerDialog.size` |
| `modalSize` | 368 | 应转换为 `pickerDialog.size` |
| `pickerSchema` | 368 | 选择器面板 schema |
| `sortable` | 264 | 列排序 |
| `toggled` | 264 | 列显示/隐藏 |

**修复**：在 `composite-schemas.ts` 的 PickerSchema 中添加缺失属性，或在转换层处理。

### 4.4 Input-number 缺失属性

| 属性 | warnings 数量 |
|------|---------------|
| `precision` | 1054 |
| `sortable` | 338 |
| `toggled` | 338 |

**修复**：在 input-number 定义中添加 `{ key: 'precision', kind: 'prop' }`。

### 4.5 Button 缺失属性

| 属性 | warnings 数量 | 说明 |
|------|---------------|------|
| `batch` | 425 | 批量操作标记 |
| `visibleOn` | 319 | 应转换为 `when` |

**修复**：`batch` 是 AMIS 特有属性，flux 中由 `selection` 控制。`visibleOn` 应在转换层处理。

### 4.6 Form 缺失属性

| 属性 | warnings 数量 | 说明 |
|------|---------------|------|
| `target` | 47 | 链接目标 |
| `wrapWithPanel` | 13 | AMIS 面板包装 |
| `submitOnInit` | 1 | 应转换为 `autoLoad` |
| `controls` | 1 | 应转换为 `body` |

**修复**：这些是 AMIS 特有属性，应在转换层处理或忽略。

---

## 根因 5：validations 语法不兼容

**影响数量**：1940

### 问题

AMIS `validations` 和 flux `validate` 语法完全不同：

**AMIS validations**：
```json
{
  "type": "input-text",
  "validations": {
    "maxLength": 10,
    "isInt": true,
    "minimum": 0
  }
}
```

**Flux validate**：
```json
{
  "type": "input-text",
  "validate": {
    "action": { "action": "ajax", "args": { "url": "..." } },
    "debounce": 300,
    "message": "Validation failed"
  }
}
```

flux `validate` 是单一异步验证动作，AMIS `validations` 是多条同步规则。

### 修复方案

在转换层将 AMIS `validations` 转换为 HTML 属性或 flux 校验规则：

```xml
<!-- 方案 A：转换为 HTML 属性 -->
<c:if test="${node.validations?.maxLength != null}">
  <maxLength>${node.validations.maxLength}</maxLength>
  <c:remove var="node.validations.maxLength"/>
</c:if>

<!-- 方案 B：转换为 flux validate（需要定义校验 action）-->
```

---

## 根因 6：CRUD 列属性未声明

**影响数量**：~6600（inline-type-declaration）

### 问题

CRUD 列（`type: "index"`, `type: "operation"`）的属性如 `name`、`width`、`fixed`、`align`、`toggled`、`sortable` 未在列 schema-definition 的 fieldRules 中声明。

### 修复方案

在 `crud-renderer-definition.ts` 的列 fieldRules 中添加：

```typescript
column: {
  fieldRules: {
    name: 'value',
    width: 'value',
    fixed: 'value',
    align: 'value',
    toggled: 'value',
    sortable: 'value',
    labelClassName: 'value',
    headerClassName: 'value',
  }
}
```

---

## 修复优先级

| 优先级 | 根因 | 影响范围 | 修复难度 |
|--------|------|----------|----------|
| P0 | 根因 1（META_FIELDS） | ~15000 warnings | 低（1 行代码） |
| P0 | 根因 3（api 重复） | 519 warnings | 低（删除 1 行） |
| P1 | 根因 2（属性名转换） | ~5000 warnings | 中（添加转换宏） |
| P1 | 根因 4.1（CRUD 属性） | 924 warnings | 低（2 行代码） |
| P1 | 根因 4.2（text 属性） | ~14500 warnings | 低（3 行代码） |
| P2 | 根因 5（validations） | 1940 warnings | 高（语法重构） |
| P2 | 根因 4.3-4.6（其他属性） | ~5000 warnings | 中 |
| P3 | 根因 6（列属性） | ~6600 warnings | 中 |

---

## 验证方法

修复后运行：

```bash
cd /Users/abc/app/nop-app-erp
npm run validate:flux
# 预期：warnings 数量从 38498 降至 < 1000
```
