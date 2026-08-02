# 表单布局 DSL 演进：从 `<layout>` 文本到结构化 `<body>` 树

> Status: 研究分析（布局语法设计探索，为简化 view.xml 配置做输入）。
> 与 `2026-07-31-1000-complex-page-pattern-catalog.md` §8（cell/view 能力扩展）为不同关注点：§8 讲"单个 cell 能做什么"，本文讲"字段如何排版组织"。
> 所有机制结论基于 nop-entropy 源码实证（标注 `文件:行号`）。

## 0. 问题

Nop 现有表单布局用 `<layout>` 文本 DSL 描述。本文评估：**有没有比 `<layout>` 文本更直观简易、支持复杂嵌套与 tab、支持单组件 delta 定制、关键信息从 meta 取、字段只靠 name 表达的布局语法？** 结论是肯定的——把平台内部已有的布局树显式外化为结构化 XML `<body>` 即可。

## 1. 现状机制（源码实证）

### 1.1 layout 文本被解析成内部树

`form.xdef:36` 定义 `<layout>form-layout</layout>`（`form-layout` 是一种文本 DSL 类型）。codeGen 解析时，文本被解析为 `formModel.layout.groups` 结构（`web.xlib:142/148`）：

```
groups: [ group ]                          # ======>id[标题]====== 区块
   └─ id, label, foldable, folded
   └─ rows: [ row ]                        # 文本中的每一行
        └─ cells: [ cell ]                 # 行内字段（id/colspan/readonly/...）
```

即**文本只是这棵 group→row→cell 树的紧凑序列化**。

### 1.2 字段配置跨到 `<cells>`，靠 id 关联（核心痛点）

`form.xdef:84-97` 的 `<cells>` 是平铺的 `<cell id>` 列表，承载字段的"配置"（gen-control/visibleOn/view/...）。渲染时布局树里的字段/区块按 id 回查 cells 取配置：

- `web.xlib:225` `const groupModel = formModel.getCell(formTable.id);` —— tab 的 `visibleOn` 从 `getCell(tab.id)` 取。
- `web.xlib:256/271` 同型 —— section/group 的配置同样跨到 cells。

也就是说：**字段/区块的"位置"在 layout 文本里，"配置"在 cells 里，两者靠 id 间接关联**。

### 1.3 字段控件从 meta 生成

`GenFormSimpleCell`（`web.xlib:349-433`）对每个 cell 从 objMeta 取 propMeta（label/domain/control），按 domain→控件映射生成 AMIS。即 label/类型本就在 meta，layout 文本里的 `field[label]` 只是"覆盖默认 label"。

## 2. 痛点归纳

| # | 痛点 | 实证 |
|---|------|------|
| 1 | 位置（文本）与配置（XML cells）分离，靠 id 关联，认知割裂 | `web.xlib:225 getCell` |
| 2 | `<layout>` 是整体文本，**无法对单个字段/区块做 XML delta**（只能整段替换文本） | form.xdef:36 `form-layout` 为单字符串 |
| 3 | tab/section 的配置（visibleOn）割裂到 cells 写 | `web.xlib:225/256/271` |
| 4 | label 在文本里重复声明（meta 已有 displayName） | `field[label]` 文本标记 |
| 5 | 复杂嵌套（tab 套 section 套子表）文本标记 `====>` 表达力弱、不可嵌套 | 文本 DSL 无嵌套语法 |

痛点 2 是最关键的——它使"单个组件 delta 定制"在布局层不可行，违背 Nop "Delta 定制"核心理念。

## 3. 方案：结构化 `<body>` 布局树

### 3.1 核心思想

把 `<layout>` 文本 + `<cells>` 平铺列表**合并为一棵嵌套 XML 树** `<body>`：

- 布局结构（`<section>/<tabs>/<tab>/<row>`）和字段（`<field>`）都是 XML 节点；
- 字段只写 `name`，`label/domain/control` 默认从 objMeta 取；
- 字段的配置（gen-control/visibleOn/view/...）直接挂在 `<field>` 节点上（位置与配置合一）；
- 每个节点有 id/name，可被 `x:extends`/`x:override` delta 定位。

### 3.2 语法元素

| 元素 | 作用 | 对应现状 |
|------|------|---------|
| `<body>` | 表单布局树根 | layout 文本整体 |
| `<section id title foldable>` | 分组/区块 | `====>id[title]` / `====^` |
| `<tabs>` / `<tab id title>` | 标签页 | `layoutControl="tabs"` |
| `<row>` | 一行（多字段横排） | 文本中的一行 |
| `<field name>` | 单字段（配置从 meta） | 文本 `field[label]` + 对应 `<cell>` |
| 紧凑行 `<row>code orgId</row>` | name 列表空格分隔 | 文本行的紧凑性 |
| `@field` 前缀 | 只读 | 文本 `@field` |

`<field>` 直接复用 `disp.xdef` 的全部能力（gen-control/visibleOn/view/actions/onEvent），与 cell 同源——它就是"放在布局树位置上的 cell"。

### 3.3 示例：采购订单 edit form

**现状写法**（位置与配置分离，tab 配置跨到 cells）：
```xml
<form id="edit" size="lg" layoutControl="tabs">
  <layout>
========>baseInfo[基本信息]======
 code docDate orgId status
========>amount[金额信息]======
 @totalAmount @totalTaxAmount
========>lines[明细行]======
 lines[明细行](2)
========>audit[审计信息]======
 createdBy createTime
  </layout>
  <cells>
    <cell id="lines">
      <view path="/erp/pur/pages/ErpPurOrderLine/ErpPurOrderLine.view.xml" grid="sub-grid-edit"/>
    </cell>
    <cell id="audit"><visibleOn>${docType=='STD'}</visibleOn></cell>   <!-- tab 显隐写在这，割裂 -->
  </cells>
</form>
```

**结构化 `<body>` 写法**（位置与配置合一，所见即所得）：
```xml
<form id="edit" size="lg">
  <body>
    <section id="baseInfo" title="基本信息">
      <row>code orgId</row>
      <row>docDate status</row>
    </section>

    <tabs>
      <tab id="amount" title="金额信息">
        <row>@totalAmount @totalTaxAmount</row>
      </tab>
      <tab id="lines" title="明细行">
        <field name="lines" colspan="2">
          <view path="/erp/pur/pages/ErpPurOrderLine/ErpPurOrderLine.view.xml" grid="sub-grid-edit">
            <actions><action id="import-from-order" label="从订单导入行"/></actions>
          </view>
        </field>
      </tab>
      <tab id="audit" title="审计信息" visibleOn="${docType=='STD'}">   <!-- 显隐内聚在 tab 上 -->
        <row>createdBy createTime</row>
      </tab>
    </tabs>
  </body>
</form>
```

## 4. 五个需求的对照

| 需求 | 现状 layout 文本 | 结构化 `<body>` |
|------|----------------|----------------|
| 直观简易 | `====>id[title]` 文本标记隐晦 | XML 嵌套所见即所得；`<row>code orgId</row>` 保留紧凑 |
| 复杂嵌套 + tab | 文本无嵌套语法，靠 `layoutControl` 整体切换 | `<tabs>/<tab>/<section>/<row>` 任意嵌套（tab 套 section 套子表） |
| 单组件 delta | ❌ 文本整体替换，不可单字段 delta | ✅ `<field name="status">` 是节点，x:extends 按 name 合并、x:override 定制 |
| meta 驱动 | `field[label]` 重复声明 label | `<field name="code"/>` 只写 name，label/domain/control 从 meta |
| name 表达字段 | `field[label]` 名+标签混合 | `<field name="code"/>` 或 `<row>code orgId</row>` 纯 name |

## 5. 单组件 delta 定制（现状最大短板的解决）

子项目/定制方只需按 name 定位，定制单个字段，位置自动继承父级：

```xml
<form id="edit" x:extends="_gen/_ErpPurOrder.view.xml">
  <body>
    <section id="baseInfo">
      <field name="status">                 <!-- 按 name 定位：只改控件，位置不动 -->
        <gen-control><c:script>return {type:'tpl', tpl:'<span class=...>${status}</span>'}</c:script></gen-control>
      </field>
    </section>
    <tabs>
      <tab id="amount">
        <field name="totalAmount" visibleOn="${showAmount}"/>   <!-- 加显隐 -->
      </tab>
    </tabs>
  </body>
</form>
```

现状要做等价定制，必须去 `<cells>` 写 `<cell id="status">`/`<cell id="totalAmount">`，且无法表达"在哪个 tab/section 里"——位置信息丢失。结构化方案位置与配置合一，delta 自然且局部。

## 6. 关键洞察：非新概念，是内部模型外化

`<body>` 树与平台已有的 `formModel.layout.groups`（§1.1 文本解析产物）**本质同构**：

```
<body>                  ≈   layout.groups
  <section>/<tab>       ≈   group (id/label/foldable)
    <row>               ≈   row
      <field>           ≈   cell (id/colspan/readonly + 配置)
```

区别仅在于：现状先把这棵树序列化成文本（`<layout>`），再把配置剥离到 `<cells>`；结构化方案直接把这棵树写成 XML、配置内聚回节点。因此：

- **不是发明新概念**，是把平台内部树显式化；
- **codegen 下游不变**（GenFormTable/GenFormRow/GenFormSimpleCell 照常消费 group/row/cell）；
- 只需新增一个 `<body>` → groups 结构的解析器（与现有 `form-layout` 文本解析器并列）。

## 7. codegen 对接与向后兼容

### 7.1 解析入口

`GenFormBody`（`web.xlib:130`）增加分支：检测 form 是否有 `<body>` 子节点：

- 有 `<body>` → 走结构化解析器，`<body>` 树 → groups 结构 → 复用现有 GenLayoutGroups/GenLayoutTabs/GenFormTable；
- 无 `<body>` → 走现有 `<layout>` 文本解析（完全保留）。

### 7.2 字段配置内聚

`<field>` 节点本身即 cell（复用 disp.xdef）。无需再写 `<cells>`；旧 `<cells>` 作为兼容仍支持（无 `<body>` 时）。

### 7.3 共存与迁移

- **简单表单继续用 `<layout>` 文本**（几十字段平铺时仍最紧凑）；
- **复杂页（多 tab/嵌套/子表）用 `<body>` 树**；
- 两者可共存于同一项目，由页面复杂度选择；
- codegen 生成器可逐步改为默认产 `<body>`，旧页面保留文本，渐进迁移，零破坏。

### 7.4 双向等价

`<body>` 树与 `<layout>` 文本表达力等价（都映射到同一 groups 结构），可双向转换。理论上可提供工具把旧 layout 文本自动转成 `<body>`。

## 8. 紧凑性 tradeoff

文本 `<layout>` 对"平铺表单"紧凑（一行多字段）。结构化 XML 对简单场景会 verbose。缓解：

- **行内 name 列表**：`<row>code orgId status</row>` 与文本行几乎同样紧凑；
- **按复杂度选择**：简单平铺用文本，复杂嵌套用 `<body>`；
- **label 不写**：`<field name>` 默认从 meta，比 `field[label]` 更短。

实测采购订单（4 tab + 子表）：结构化 `<body>` 行数与 layout+cells 合计相当，但可读性与可 delta 性显著提升——复杂页不是"更冗长"，而是"把分散的两处合为一处"。

## 9. schema 改动提案

`form.xdef` 增可选 `<body>` 子节点（与 `<layout>` 二选一）：

```xml
<form id="..." ...>
    <objMeta>v-path</objMeta>
    <layout>form-layout</layout>     <!-- 现状，保留 -->
    <body xdef:ref="form-body.xdef"/> <!-- 新增，可选，与 layout 二选一 -->
    <cells .../>                      <!-- 现状，保留（无 body 时用）-->
    ...
</form>
```

`form-body.xdef`（新增）定义嵌套布局元素：
```xml
<body xdef:body-type="list">
    <section id="string" title="string" foldable="boolean" folded="boolean"
             xdef:name="UiLayoutSection">
        <body xdef:ref="form-body.xdef"/>   <!-- 递归，支持嵌套 -->
    </section>
    <tabs id="string" xdef:name="UiLayoutTabs">
        <tab id="!string" title="string" visibleOn="string" xdef:name="UiLayoutTab">
            <body xdef:ref="form-body.xdef"/>
        </tab>
    </tabs>
    <row fields="csv-set" xdef:name="UiLayoutRow"/>   <!-- 紧凑：name 列表 -->
    <field name="!string" label="string" colspan="int" readonly="boolean"
           visibleOn="string" xdef:ref="disp.xdef" xdef:name="UiLayoutField"/>
</body>
```

（`<field>` 复用 `disp.xdef`，直接拥有 gen-control/view/actions/onEvent/visibleOn 全部能力；`<row fields=>` 是紧凑语法糖，展开为多个 `<field>`。）

## 10. 风险与开放问题

1. **两套语法长期共存的认知负担**：需明确"简单用文本、复杂用 body"的指导原则，并在生成器层面收敛到默认一种（建议复杂页默认 `<body>`）。
2. **x:extends 合并语义**：`<body>` 树的 delta 合并需明确"按 field@name / tab@section / row@index 定位"的合并键，比平铺 cells 的 id 合并略复杂，但与 Nop 既有 x:override 机制一致。
3. **紧凑行 `<row fields=>` 与 `<field>` 的等价展开**：codegen 需在解析早期把 `<row fields="a b c">` 展开为 3 个 `<field>`，统一后续处理。
4. **与 §8 cell/view 扩展的关系**：两者正交。§8 扩展单个 cell/view 的能力（actions/onEvent），本文重构字段的组织方式（文本→树）。可独立推进，组合后 `<body>` 内的 `<field>` 直接享有 §8 的 actions/onEvent。
5. **生成器改造范围**：需新增 `<body>` 解析器（XPL），但下游 GenFormTable/GenFormRow/GenFormSimpleCell 不变。属平台侧改动，app 无法独立交付（同 §8 平台补丁约束）。

## 11. page 层 complex 块组合（更对路的方案）

> ⚠️ **认识纠正（见 `2026-07-31-1300-xview-schema-assessment.md`）**：complex 与 tab 内联组合早已是 xview.xdef 的既有 schema 能力（`xview.xdef:159 tab 是 UiContainer` / `:195 complex 含 aside`），问题是 codegen 未实现（无 `page_complex.xpl`、`impl_GenPage` 无 complex 分支、`page_tabs.xpl:9` 只 LoadPage 引用）。本节最初把 complex 当"新提案"，实为对既有 schema 的重新发明。真实改进 = **激活 codegen 实现既有 schema**，而非设计新 schema。本节其余分析（complex 优于 form.body、bind 随头提交挑战）仍有效，作为"激活后如何用"的参考。

### 11.1 动机

前文 §3–§9 的 `<body>` 方案解决的是"单 form 内字段如何排版"。但 ERP 复杂页面（头表单 + 多子表 + 关联列表）的复杂性主要在 **page 层的块组合**，不在 form 内部字段排版。更对路的思路：**不动 form，在 `<pages>` 增加一个 `complex` 类型，把 section/crud/tabs 作为一等块自由组合**。

### 11.2 定位与替代

`complex` 是 page 层"自由块组合容器"，每个块（`<section>`/`<crud>`/`<tabs>`/`<tab>`）是一等公民，任意嵌套平铺。它**替代现状"在 pages 里定义 simple/crud 子页、再用 tabs 引用 page 名"的间接模式**（见 `page-structure-patterns.md` §2 机制 B——现状要先声明 `<simple name=headerForm>`、`<crud name=relatedLines>`，再用 `<tabs><tab page="headerForm"/>` 间接引用，啰嗦且割裂）。complex 直接平铺：

```xml
<complex name="detail">
  <section form="edit" title="基本信息">
    <layout>code orgId docDate status</layout>          <!-- 头表单片段，label 从 meta -->
  </section>
  <tabs>
    <tab title="明细行">
      <crud name="lines" bind="lines" grid="sub-list">  <!-- 子表：随头提交 -->
        <cols><col name="materialId"/><col name="quantity"/><col name="amount"/></cols>
        <actions><action id="import-from-order" label="从订单导入行"/></actions>
      </crud>
    </tab>
    <tab title="关联入库">
      <crud name="receives" grid="rcv-list" filter_orderId="${id}" noOperations="true"/>  <!-- 独立只读 -->
    </tab>
  </tabs>
</complex>
```

### 11.3 优势：子表用 crud，从根源解决 input-table 能力弱

前几轮（含 `2026-07-31-1000-complex-page-pattern-catalog.md` §8）反复绕"cell/input-table 怎么加 actions/onEvent"，根因是 input-table 能力弱（无 toolbar/batch/分页）。complex 让子表直接是 `<crud>`，天生拥有 toolbar/actions/batch/分页/复杂列——不用再给 input-table 打补丁。

| 维度 | form.body（form 层） | complex（page 层） |
|------|---------------------|-------------------|
| 子表能力 | input-table（无 toolbar/batch/分页） | **crud（全能力）** |
| 是否动 form | 改 form.xdef | **不动 form** |
| 组合本质 | 单 form 内字段排版 | 多块自由组合（复杂页真问题） |
| delta 粒度 | 字段级 | 块级（section/crud 节点） |

### 11.4 核心挑战：随头 cascade 提交

子表用 crud 带来 input-table 没有的提交策略问题。input-table 天然属于 form（数据即 form 一部分，随 `__save` 提交，cascade 自动）；crud 默认有自己的端点（独立实体）。故 complex 的 crud 须区分两种：

- **`bind="lines"`（随头提交）**：crud 数据绑到父 scope 的 `lines` 数组，不调自己端点，随父 `__save` 提交（聚合根 cascade）。
- **`filter_orderId="${id}"`（独立）**：自己 `findPage` 端点，按外键过滤。

`bind` 仍由 ORM 推导（头 `lines` 是 `<to-many cascade>` → 生成 bind；独立实体 → 生成 filter），**提交策略不在 view 重复声明**。

**但 bind 的实现有成本**：AMIS crud 的 data 是它自己 scope，不自动并入父 form data。要让"crud 当前数据随父提交"，codegen 须为 bind crud 生成数据同步配置（crud data ↔ 父 `lines` 双向绑定，或提交前 `doAction(setValue, lines: crudData)`）。这是 complex 方案的主要工作量。input-table 没此问题（结构性优势）但能力弱。

**Tradeoff**：要 crud 完整能力 → bind crud + codegen 同步（值得，但承认成本）；简单随头子表 → section 内用 form + input-table（现状保留）。

### 11.5 与 form.body 的关系（重新定位）

- **complex（page 层）= 复杂页主方案**：覆盖多块组合、关联/独立子表，解决最主流痛点。
- **form.body（form 层）= section 内字段排版可选增强**：仅当某 section 字段多、需嵌套/tab/字段级 delta 时用；多数 section 用简单 `<layout>` 文本够用，可缓。

两者层级不同、正交：complex 内 `<section>` 若需要，其 form 可用 body；complex 内 `<crud>` 享受 crud 全部能力。

### 11.6 与现状 page 类型并存

simple/crud/tabs 是预设模板（简单页直接用），complex 是自由组合（复杂页用）。complex 是它们超集（放一个 section≈simple、一个 crud≈crud页、包 tabs≈tabs页），但预设模板对简单场景更简洁，并存按复杂度选择。

## 12. 结论（修订）

复杂页面的描述改进分两层，complex 优先：

1. **【主方案·page 层】complex 块组合**：在 `<pages>` 增 `complex` 类型，section/crud/tabs 自由组合。核心收益是子表用 `<crud>` 获得完整能力（toolbar/batch/分页），从根源解决 input-table 能力弱；并替代现状 tabs 间接引用模式。关键挑战是随头 cascade 提交的 `bind` 机制（需 codegen 数据同步），简单子表可仍用 input-table。
2. **【可选·form 层】form.body 结构化布局**：仅当 section 内字段排版复杂/需字段级 delta 时启用；多数情况 section 内 `<layout>` 文本够用，可缓。

两者都属平台侧改动（新增 page 类型/解析器），与现状共存、渐进迁移。共同原则不变：提交策略由 ORM cascade 推导，不在 view 重复声明。

## 13. 参考

- 平台 schema：`nop-entropy/nop-kernel/nop-xdefs/.../_vfs/nop/schema/xui/form.xdef:36/84`、`disp.xdef`、`xview.xdef:67-193`（pages 类型集合，complex 的挂载点）
- 平台 codegen：`nop-entropy/nop-frontend-support/nop-web/.../_vfs/nop/web/xlib/web.xlib:130(GenFormBody)/142/148/193(GenLayoutGroups)/210(GenLayoutTabs)/225(getCell)/243(GenFormTable)/349(GenFormSimpleCell)`、`page_crud.xpl`（crud 渲染，complex 内 crud 块的复用基础）
- 关联文档：`docs/analysis/2026-07-31-1000-complex-page-pattern-catalog.md` §8（cell/view 能力扩展）、§3-A2/A4/A10（头行详情/tabs 工作台/关联drawer，complex 的覆盖对象）、`docs/design/page-structure-patterns.md`（机制 B tabs 间接引用模式，complex 的替代对象）
