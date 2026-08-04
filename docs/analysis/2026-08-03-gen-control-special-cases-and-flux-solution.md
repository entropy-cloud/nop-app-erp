# ERP view.xml gen-control 特殊编写情况分析与 flux 最佳实现方案

> 日期：2026-08-03（初稿）→ 2026-08-03（二轮修订：子 agent 两轮审查后，统一数据口径 + 修正方案为 flux reaction 优先）
> 背景：purchase add 弹窗打不开的根因之一是 view.xml 的 `<gen-control>` 手写了 AMIS 格式的 `onEvent`/`actionType`（flux 不支持），且 flux 编译校验对此类 columns 不递归（已修复，见 `nop-chaos-flux/docs/architecture/nested-schema-field-classification.md` §3.5.2）。本文全面分析 view.xml 中 gen-control 的特殊编写情况，并给出 flux 最佳实现方案。
> 数据来源：全仓库源 view.xml（排除 `_gen`/`target`/`_dump`）**按 `<gen-control>` 块级解析**（337 个块）；两轮独立子 agent 审查核验通过。

## 1. 现状数据

### 1.1 规模与 type 分布

- **gen-control 块数：337**（分布在 18 个模块；注意 `grep -c gen-control` 的 677 是字符串出现次数，含闭合标签与注释，非块数）。
- **出现位置**：283 块在 grid `<col>` 内、**54 块在 form `<cell>` 内**（16%）。
- **type 分布（按块，去重后 16 种）**：

| type | 块数 | 写法 | flux 是否有对应渲染器 |
| --- | --- | --- | --- |
| tpl | 155（单引号 13 + 双引号 142） | c:script return | **✗ 无** |
| input-number | 66（38 带 onEvent） | c:script return | ✓ |
| picker | 32 | c:script return | ✓（契约不同，见 §2.2） |
| number | 13 | c:script return | **✗ 无**（condition-builder 类型定义非渲染器） |
| input-text | 13 | c:script return | ✓ |
| button | 7（7 带 actionType） | c:script return | ✓（格式需转换） |
| input-date | 7 | c:script return | ✓ |
| input-password | 5 | c:script return | ✓ |
| switch | 5 | c:script return | ✓ |
| input-datetime | 2 | c:script return | ✓ |
| static | 2 | c:script return | **✗ 无** |
| datetime | 2 | c:script return | **✗ 无** |
| button-group-select | 2 | c:script return | ✓ |
| `<tree-select>` | 8 | XML 节点式 | ✓ |
| `<input-number>` | 7 | XML 节点式 | ✓ |
| `<select>` | 6 | XML 节点式 | ✓ |
| `<input-date>` | 3 | XML 节点式 | ✓ |
| `<wrapper>` | 2 | XML 节点式 | 需评估 |

**关键结论**：
1. **flux 缺 4 种显示型渲染器：tpl / static / number / datetime**（输入型 input-number/input-date/input-datetime 都有；tpl 替代可用 status/badge/text）。
2. **tpl 是最大头（155 块，46%）**——状态标签模板，全部是 AMIS 模板字符串。
3. **54 块（16%）在 form cell**——下文 columns 递归修复的覆盖边界须另行确认（见 §4.4）。

### 1.2 字段使用频率（c:script return 块内，行首键出现次数）

```
tpl 146 / name 115 / required 69 / actionType 66 / step 64 / label 61 / actions 61 /
validations 56 / validationErrors 56 / onEvent 52 / value 44 / change 43 / args 43 /
source 36 / valueField 36 / labelField 36 / joinValues 36 / extractValue 36 ...
```

> 口径说明：本表为**行首键出现次数**（同一块内同键多处时计数偏少，如 tpl 键内联的块、嵌套 action 内的 actionType）。**块级口径**见 §1.3。

### 1.3 需要转换的 AMIS 格式规模（块级 + 键级）

- **onEvent 块：52**（事件名：change 43 / click 5 / blur 4）
- **actionType 出现次数：75**（值分布：setValue 46 / dialog 9 / ajax 8 / closeDialog 4 / close 4 / toast 2 / link 1 / custom 1；**含 56 块内嵌**，部分块内嵌套多个）
- **tpl 模板：155 块**
- **joinValues/extractValue：36 处**（picker）
- **XML 节点式：26 块**（Nop XPL 控件标签，格式兼容）

## 2. 各种情况详细分析

### 2.1 input-number 计算联动（66 块，38 带 onEvent）—— 核心情况

**典型结构**（`ErpPurOrderLine.view.xml` quantity 列）：

```xml
<col id="quantity" mandatory="true">
    <gen-control>
        <c:script><![CDATA[
            return {
                type: 'input-number',
                name: 'quantity',
                required: true,
                step: 1,
                validations: { minimum: 0.0001 },
                validationErrors: { minimum: '数量必须大于 0' },
                onEvent: {
                    change: {
                        actions: [{
                            actionType: 'setValue',
                            args: { value: {
                                amount: '${ROUND(quantity * unitPrice, 4)}',
                                taxAmount: '${ROUND(quantity * unitPrice * taxRate / 100, 4)}',
                                amountWithTax: '${ROUND(quantity * unitPrice * (1 + taxRate / 100), 4)}'
                            } }
                        }]
                    }
                }
            };
        ]]></c:script>
    </gen-control>
</col>
```

**结构拆解**：
- `step/validations/validationErrors`：数值步长与校验消息——**合理定制**（默认生成无此能力；flux 校验是 min/max + validate action，与 AMIS `validations:{minimum}` + `validationErrors:{minimum:'...'}` 无直接对应，需确认映射）
- `onEvent.change.actions[0].actionType='setValue'`：**AMIS 遗留格式**——flux 的事件字段（renderer 显式声明的 onChange 等）值是 `ActionSchema | ActionSchema[]`，**不支持 AMIS 事件映射** `{change:{actions:[...]}}` 与 `actionType`

**为什么写 gen-control**：默认生成（`ui:number` col 属性）只能生成基础 input-number，无法表达步长/校验消息定制和**字段联动计算**（quantity 变化 → 重算 amount/taxAmount/amountWithTax）。ORM 层 `amount` 等是普通列（无 expr/computed 定义），**计算只在前端发生**。

**flux 最佳实现（二选一）**：

**方案 1（推荐）：flux 原生 `reaction` 声明式联动**（`type: 'reaction'`，schema.ts 契约：`watch: SchemaValue` + `actions: ActionSchemaLike` + 可选 when/immediate/debounce/once）：
```json
// 独立 reaction 节点（表单 body 或区域级；AMIS onEvent.change.actions[0] 一次设 3 字段 → 3 个 setValue action）
{ "type": "reaction", "watch": "quantity", "actions": [
    { "action": "setValue", "args": { "path": "amount", "value": "${$Math.round(quantity * unitPrice * 10000) / 10000}" } },
    { "action": "setValue", "args": { "path": "taxAmount", "value": "${$Math.round(quantity * unitPrice * taxRate / 100 * 10000) / 10000}" } },
    { "action": "setValue", "args": { "path": "amountWithTax", "value": "${$Math.round(quantity * unitPrice * (1 + taxRate / 100) * 10000) / 10000}" } }
] }
```
- **优点**：声明式、flux 原生（reaction renderer 已实现，watch 裸名自动归一化为 `${quantity}`，actions 单对象/数组均支持，reaction 可挂任意 schema 节点位置——form body 得 form scope、table 列 cell region 得行级 rowScope、combo item 得 itemScope）、不碰 input-number renderer、语义清晰（watch 字段变化 → 执行 actions）
- **表达式函数缺口（必补）**：flux-formula builtins **无 ROUND**（58 处模板用 ROUND；未知函数运行时抛 `Call target is not a function`）。解决路径：(a) flux-formula 注册 ROUND builtin（一行，`$Math.round` 已存在）；(b) 或改写为 `$Math.round(x * 10000) / 10000`（四舍五入到 4 位小数的等价写法，上例采用）。写入执行路径 §4.3。
- **语义等价映射（AMIS onEvent → flux reaction/事件）**：

| AMIS onEvent | flux 对应 |
| --- | --- |
| `onEvent.change`（43 块） | `{type:'reaction', watch:'<字段>', actions:[...]}` 或 renderer `onChange` |
| `onEvent.click`（5 块） | renderer `onClick`（button 等）或 reaction |
| `onEvent.blur`（4 块，input-text） | renderer `onBlur` 或 reaction |
| `actionType:'setValue'` + `args.value:{f1,v1,f2,v2}` | 多个 `{action:'setValue', args:{path:'f1', value:'v1'}}` |
| `actionType:'dialog'/'drawer'/'ajax'/'close'...` | `onClick:{action:'openDialog'/'openDrawer'/'ajax'/'closeSurface'}` |

**方案 2：`onChange` 格式转换 + 字段控件统一事件派发**（`onEvent:{change:{actions:[X]}}` → `onChange: X`、`actionType`→`action`）
- **编译端已支持**：schema 声明 `onChange` → node-renderer 自动编译为 `props.events.onChange`（`node-renderer-resolved.tsx:243`），shape-validation 校验 action 格式。
- **运行时缺口（审查实证后修正）**：字段控件（input-number 等 10 个）值变化只走 `handlers.onChange`（setValue + validate），**不调用 `props.events.onChange`**——转换后联动静默不触发。**但这可在统一抽象层解决**：`useFormFieldController` 扩展 `events` 参数 + `useFormFieldFromProps` 透传，在 `wrappedHandlers.onChange` 统一派发（setValue → events.onChange → validate），10 个控件迁移到 `useFormFieldFromProps` 即自动获得。方案与触发时机设计见 `nop-chaos-flux/docs/architecture/field-onchange-event-dispatch.md`。
- **触发时机（组件库惯例）**：**直接触发**（不等待 validation）——值变化 → 更新值 → 派发 onChange action → 校验。与 React Hook Form/Formik 一致（事件处理器直接执行，validation 独立关注点）；联动计算（amount=quantity×unitPrice）需即时反馈，非法输入时联动结果仍有意义。

**结论**：方案 1（reaction）与方案 2（统一事件派发）**互补**——纯字段联动（watch 字段 → 计算写回）可用 reaction；**renderer 声明式 onChange 事件**（含 onBlur/onClick 等非 watch 场景）走统一派发。推荐：**主推方案 2**（与 AMIS onEvent 语义 1:1 对应、改动集中在一个 hook、控件迁移即自动获得），reaction 作为补充。

### 2.2 picker 引用选择（32 块）—— 数据源迁移

**典型结构**：

```js
{
    type: 'picker',
    name: 'materialId',
    label: '物料',
    required: true,
    source: '/erp/md/pages/ErpMdMaterial/picker.page.yaml',
    valueField: 'id',
    labelField: 'name',
    joinValues: false,
    extractValue: true
}
```

**结构拆解**：
- `source/valueField/labelField`：**AMIS 遗留字段名**——flux `PickerSchema` 契约是 `loadAction`/`labelResolveAction`/`valueKey`/`labelKey`/`options`/`columns`/`searchable`/`autoFill`/`pickerDialog`/`multiple`/`readOnly`/`onPick`（非穷尽），**没有 source/valueField/labelField**
- `joinValues/extractValue`：AMIS 值绑定模式——flux 无对应
- **注意**：nop-entropy 的 flux-control.xlib 生成 picker 时也输出 valueField/labelField/joinValues——该侧也是 AMIS 遗留，需一并核对

**澄清（picker 渲染器与页面级 picker 的区分）**：
- **flux 支持 picker 渲染器** ✓（`PickerSchema` 契约见上，渲染器已实现）。
- `flux-web.xlib` 注释"Flux 无页面级 picker schema"指 **`UiContainerModel.type=='picker'` 页面容器**（view.xml 里 `<picker>` 容器类型），与 picker 渲染器无关。
- ERP 的 352 个 `picker.page.yaml`（`web:GenPage page="picker"` 生成）是 **AMIS picker 页面**；flux 模式 picker 渲染器用 `loadAction` 数据源，**不使用这些页面**——不是阻断，是数据源方式不同。

**flux 最佳实现**：
- 整段迁移：`source: '.../picker.page.yaml'` → `loadAction: {action:'ajax', args:{url:'@query:ErpMdMaterial__findPage?...'}}`（flux picker 测试实证：`loadAction: {action:'ajax', args:{url:'/api/owners'}}`）；`valueField` → `valueKey`，`labelField` → `labelKey`；删 `joinValues`/`extractValue`
- picker.page.yaml 迁移后可留作 AMIS 兼容或清理（flux 不用）

### 2.3 tpl 状态标签（155 块，最大头）

**典型结构**：

```js
// type: 'tpl' 控件（13 单引号）
{
    type: 'tpl',
    tpl: '<span class="label label-${totalDebit == totalCredit ? "success" : "danger"}">' +
         '${totalDebit == totalCredit ? "✓ 平衡" : "✗ 不平衡"}</span>'
}
```

```js
// 双引号形式（142）
type: "tpl",
tpl: '<span class="label label-${' + valueProp + " == 'ACTIVE' ? 'primary' : 'default'}\" ...>"
```

**结构拆解**：
- **AMIS 模板字符串**：HTML 片段 + `${...}` 表达式 + JS 字符串拼接（`'...' + valueProp + "..."`）
- 用途：状态值 → 彩色标签（label-${status} → primary/success/danger）、平衡检查、格式化显示
- flux 无 tpl 渲染器

**flux 最佳实现**（需按用途分级）：
- 状态彩色标签 → flux `status`（value/labelMap/levelMap/iconMap）或 `badge`
- 纯文本/表达式 → flux `text`（allowSource，支持表达式）
- 155 块需逐个语义分类（状态标签/平衡检查/格式化），是最大的迁移工作量

### 2.4 button 操作按钮（7 块，7 带 actionType）

**典型结构**：

```js
{
    type: 'button',
    label: '从出库导入行',
    level: 'primary',
    icon: 'fa fa-download',
    tooltip: '...',
    actionType: 'dialog',
    dialog: { title: '...', size: 'lg', actions: [...] }
}
```

**结构拆解与映射**：
- `label/level/icon/tooltip`：外观——**合理**（flux button 支持）
- `actionType` 值映射（与 flux-web.xlib NormalizeAction L777-847 一致）：dialog→openDialog / drawer→openDrawer / ajax→ajax / close/closeDialog→closeSurface / toast→showToast / link→navigate
- **深度遗留**：`actionType:'custom'` + `script` 内嵌 `doAction(...)`（AMIS 全局运行时 API，ErpFinVoucher autoBalance）——flux 无对应物，需重构为 flux action 链
- **条件字段**：`condition: "${status == 'INACTIVE'}"`（4 块，switch 停用确认）——AMIS 条件动作，→ flux `when` 条件
- **disabledOn: '${!businessType}'**（1 块，ErpFinVoucherTemplate）——AMIS 条件字段，→ flux BaseSchema `disabled`（表达式）
- `clearValueOnHidden`（2 块）——→ flux `hiddenFieldPolicy`（已有）

**flux 最佳实现**：`actionType`+`dialog` → `onClick: {action:'openDialog', args}`；condition → when；disabledOn → disabled；custom/script 深度遗留逐个重构。

### 2.5 number / static / datetime 显示型（13 + 2 + 2 块）

```js
{ type: 'number', kilometer: true, precision: 2 }      // 千分位显示：flux ✗
{ type: 'static', name: 'businessType', label: '业务类型' }  // 只读字段：flux ✗
{ type: 'datetime', format: 'YYYY-MM-DD' }             // 日期显示：flux ✗
```

- 三个都是 **AMIS 显示型控件，flux 无渲染器**（有 input-number/input-date/input-datetime 输入型，显示型不同）
- **flux 替代**：number → input-number readOnly 或 text（格式化表达式）；static → 只读字段或 text；datetime → text（格式化表达式）或补渲染器

### 2.6 字段控件（input-text/input-password/switch/input-date 等，约 30 块）

```js
{ type: 'input-text', name: 'code', ... }
{ type: 'switch', name: 'active', ... }
```

- **flux 都有对应渲染器** ✓
- 多数是 AMIS 迁移直接写死，格式兼容，保留 + 清理 AMIS 遗留字段即可

### 2.7 XML 节点式 gen-control（26 块）

非 c:script return JSON 的写法——直接写 Nop XML 控件标签：

```xml
<gen-control>
    <tree-select>...</tree-select>
</gen-control>
<gen-control>
    <input-number placeholder="..."/>
</gen-control>
<gen-control>
    <select><source><url>...</url></source></select>
</gen-control>
```

分布：tree-select 8 / input-number 7 / select 6 / input-date 3 / wrapper 2。
- 这些是 **Nop XPL 控件标签**（control.xlib/flux-control.xlib 解析），**格式本身兼容**（flux-control.xlib 有对应 edit-* 标签）
- `wrapper` 2 块（ErpCsTicket 含 `<service><api dataType="raw"><adaptor>` AMIS 数据服务 + adaptor JS）——需单独评估 flux 适配

### 2.8 汇总

| 情况 | 块数 | 分类 | flux 处理 |
| --- | --- | --- | --- |
| tpl 状态标签 | 155 | AMIS 特有（flux 无 tpl） | 迁移到 status/badge/text |
| input-number 联动 | 66（38 带 onEvent） | AMIS 格式（onEvent/actionType）+ 合理定制（step/validations） | **reaction 声明式联动**（推荐）或 onChange 转换（需先补 renderer 派发） |
| picker 引用 | 32 | AMIS 字段名 + source（页面 URL） | 整段迁移（source→loadAction、valueField→valueKey、labelField→labelKey、删 joinValues） |
| number/static/datetime 显示 | 17 | AMIS 特有（flux 无） | 迁移到只读/text |
| button 操作 | 7 | AMIS 格式（actionType/dialog/custom/condition/disabledOn） | onClick 转换 + condition→when + disabledOn→disabled + custom 重构 |
| 字段控件（text/password/switch/date） | ~30 | 兼容 | 保留 + 清理 |
| XML 节点式 | 26 | Nop XPL 控件 | 格式兼容（wrapper 需评估） |

## 3. 核心问题的本质

1. **gen-control 的存在理由**：默认生成能力有限——不能表达步长/校验消息/字段联动/自定义选择器/状态标签。gen-control 是 Nop 的**逃生舱**，这些场景用它合理。
2. **格式是 AMIS 遗留**：ERP 从 AMIS 迁移时，gen-control 内容直接照搬 AMIS 控件 JSON（onEvent 事件映射/actionType/tpl 模板字符串/joinValues/condition/disabledOn），未按 flux 格式改写。flux 编译校验此前对 columns 不递归（已修复）导致这些问题静默。
3. **计算逻辑的位置**：行内金额联动目前只在前端（onEvent）。ORM 无 computed 字段。这是 ERP 的业务设计选择，**不改变设计，只改实现格式**。

## 4. flux 最佳实现方案

### 4.1 原则

1. **flux 保持纯粹**：不做 actionType/onEvent 兼容转换（用户已确认）。AMIS 格式必须在 schema 产出前消除。
2. **声明式优先**：能用 col 属性/xmeta 或 flux 原生机制（reaction）声明的优先声明式。
3. **转换在应用层或生成器层，不在 flux 运行时**（reaction 例外：它是 flux 原生机制）。

### 4.2 分情况方案

**情况 A：input-number 联动（66 块）**
- **主推 onChange 统一派发**：flux 补字段控件统一事件派发（`useFormFieldController` 扩展 events + `useFormFieldFromProps` 透传，见 `nop-chaos-flux/docs/architecture/field-onchange-event-dispatch.md`），ERP 迁移 `onEvent:{change:{actions}}` → `onChange:{action:'setValue', args:{path,value}}`（purchase 先行验证）
- 补充 reaction（watch+actions）覆盖非控件内嵌联动场景
- step/validations：中期尝试声明式（扩展 col 属性）或确认 flux 校验映射

**情况 B：picker（32 块）**
- **数据源迁移**（非阻断）：flux picker 渲染器支持 ✓，数据源用 `loadAction`（ajax/RPC），ERP 的 `source`（picker 页面 URL）迁移为 loadAction；valueField→valueKey、labelField→labelKey、删 joinValues/extractValue
- picker.page.yaml 是 AMIS 页面，flux 模式不用（迁移后留作兼容或清理）

**情况 C：tpl（155 块，最大工作量）**
- 逐个语义分类：状态标签→status/badge；纯模板→text
- 先确认 flux status/badge 的字段契约覆盖程度

**情况 D：button（7 块）**
- actionType+dialog → onClick+openDialog（值映射同 NormalizeAction）
- condition → when；disabledOn → disabled；custom+script+doAction 逐个重构

**情况 E：number/static/datetime（17 块）**
- 迁移到只读字段/text（或评估补渲染器）

**情况 F：字段控件（~30 块）**
- 保留（格式兼容），清理 AMIS 遗留字段

**情况 G：XML 节点式（26 块）**
- 格式兼容保留；wrapper+adaptor（2 块）单独评估

### 4.3 转换执行路径（建议顺序）

1. **purchase 域先行**：用 reaction 方案做 quantity→amount 联动（最小验证），重建 ERP → e2e 验证 add 弹窗打开 + 联动生效（**先做运行时实证**：reaction 在 array-editor/form 内能否触发；**同时处理 ROUND 缺口**——flux-formula 注册 ROUND builtin 或改写表达式，否则最小验证第一步就抛错）
2. **推广全 18 域**：按实测规模（onEvent 52 块 / actionType 75 键 / tpl 155 块）重估工作量，写结构化转换脚本或人工按模式改，逐域验证
3. **tpl/static/number/datetime 迁移**：逐个 view 迁移到 text/status/badge，验证显示
4. **picker 数据源迁移**：32 块 source（picker 页面 URL）→ loadAction（RPC 数据源）
5. **声明式迁移**（可选中期）：step/validations 等扩展 col 属性支持

### 4.4 防护

- flux 编译校验已补 columns 递归（本次修复）——array-editor/table 的 columns 内 AMIS 格式（actionType/onEvent）会报 invalid-action-shape（带路径）
- **覆盖边界**：columns 递归修复针对 grid/table 的 columns 字段；**form cell 的 gen-control（54 块）走 form 字段路径，覆盖情况需单独验证**（form 字段的 schema-definition 递归与 columns 不同）
- 改造后跑 flux 编译校验（validateSchema）确认 0 diagnostics，再上线

## 5. 结论

- gen-control 是 Nop 的合理逃生舱，这些特殊编写**大多数有存在理由**，不应废除
- 主要问题是 **AMIS 格式遗留**（onEvent 52 块 / actionType 75 键 / tpl 155 块 / picker 字段名 32 块），需按 flux 格式转换
- **flux 渲染器缺口是 4 种显示型**：tpl/static/number/datetime；tpl 场景可用 status/badge/text 替代
- **字段联动：flux 补字段控件统一 onChange 事件派发**（`useFormFieldController` 扩展 events，控件迁移到 `useFormFieldFromProps` 即自动获得；触发时机直接触发不 gate validation，符合组件库惯例）——与 AMIS onEvent 语义 1:1 对应；reaction（watch+actions）作为补充；**前置：flux-formula 补 ROUND builtin**（58 处模板依赖，未知函数运行时抛错）
- **picker 可用但需迁移数据源**：flux 支持 picker 渲染器 ✓，ERP 的 `source`（picker 页面 URL）迁移为 `loadAction`（RPC 数据源）；352 个 picker.page.yaml 是 AMIS 页面，flux 模式不使用
- 转换策略：应用层改 view.xml（purchase 先行 → 18 域推广），flux 保持纯粹，编译校验作为防线
