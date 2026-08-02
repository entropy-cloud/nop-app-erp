# xview.xdef 设计评估：合理性与不合理性

> Status: 认识纠正。本文纠正 `2026-07-31-1100-form-layout-dsl-evolution.md` §11 把 complex 当作"新提案"的误判——complex 与 UiContainer 递归组合早已是 xview.xdef 的既有 schema 能力，问题是 codegen 未实现。
> 所有结论基于 nop-entropy 源码实证（标注 `文件:行号`）。

## 1. 背景：一个被低估的 schema

前几轮讨论（complex、tab 内联组合、form.body）都在"设计新 schema 能力"。但仔细读 `xview.xdef` 后发现：**这些能力早已存在**。本文评估 xview.xdef 的真实设计，区分"schema 合理性"与"codegen 落地不合理性"，并据此纠正方案定位。

## 2. xview.xdef 的真实结构（重新发现）

### 2.1 UiContainer 是递归组合子

`xview.xdef:67-192` 定义 `UiContainer`，其内可含 `crud / picker / simple / tabs / wizard`（均为 `xdef:ref` 各自模型）。关键是：**凡 `xdef:ref="UiContainer"` 的位置都递归可嵌套**。

| 位置 | 是否 UiContainer（可递归嵌套） | 证据 |
|------|------------------------------|------|
| `<pages>` | ✅ `xdef:ref="UiContainer"` | `:194` |
| `<tab>` | ✅ `xdef:ref="UiContainer"` | `:159-161` |
| `<complex>` | ✅ `xdef:ref="UiContainer"` | `:195` |
| `<complex><aside>` | ✅ `xdef:ref="UiContainer"` | `:196` |
| `<wizard><step>` | ✅ `xdef:ref="UiContainer"` | `:190` |

即 pages → tabs → tab → crud → ... 任意深度组合，schema 层完全支持。

### 2.2 complex 早已存在（含 aside 分栏）

`xview.xdef:194-198`：
```xml
<pages xdef:key-attr="name" xdef:body-type="list" xdef:ref="UiContainer">
    <complex xdef:name="UiComplexPageModel" xdef:ref="UiContainer">
        <aside xdef:ref="UiContainer" />
    </complex>
</pages>
```

`complex` 是 pages 下的独立 page 类型，主区是 UiContainer（可放 crud/simple/tabs/...），另有 `aside`（也是 UiContainer，左右分栏）。**前几轮"提案"的 complex 是对这个既有定义的重新发明**，且 schema 版本还多了 aside 分栏。

### 2.3 tab 支持内联（不只是 page 引用）

`xview.xdef:159-161`：
```xml
<tab name="!string" page="string" ... xdef:ref="UiContainer"/>
```
`tab` 同时有 `page="string"`（引用）和 `xdef:ref="UiContainer"`（内联）。即 tab 既能引用同级子页，也能直接内嵌 crud/simple/tabs。

## 3. 合理性（schema 设计层面，非常优秀）

1. **UiContainer 递归组合**：一个概念统一所有嵌套。pages/tab/complex/aside/wizard-step 都是 UiContainer，任意深度组合天然成立——教科书级的组合设计。
2. **内联 + 引用双模式**：复用走 `page=`/`grid=`/`form=` 引用，一次性组合走内联。两种模式并存，按场景选择。
3. **complex 已含主区 + aside 分栏**，原生覆盖"左树右内容/左筛右列表"等左右布局。
4. **三层分离**（grids/forms/pages，`xview.xdef:25/29/194`）：列定义、表单定义、页面组装分离，各自命名、被引用复用。
5. **page 类型各司其职**：crud（列表）/simple（单表单）/tabs（分组）/wizard（向导）/picker（选择）/complex（自由组合），职责清晰。

**结论**：xview.xdef 的 schema 设计完备且优雅，能表达任意复杂页面，无需新增组合类 schema 概念。

## 4. 不合理性（全在 codegen，不在 schema）

schema 能力完备，但 **codegen 只实现了一个窄子集，把大部分组合能力变成死代码**：

| schema 能力 | codegen 实现 | 证据 |
|------------|------------|------|
| **complex page** | ❌ 完全休眠 | 无 `page_complex.xpl`（仅有 page_crud/picker/simple/tabs/wizard）；`impl_GenPage.xpl:19-29` dispatch 只有 crud/picker/simple/tabs，**无 complex 分支**；xlib 全树 grep `complex/UiComplex/UiContainer` **0 命中** |
| **tab 内联 UiContainer** | ❌ 完全休眠 | `page_tabs.xpl:9` 只 `LoadPage(tabModel.page \|\| tabModel.name)`——只按名引用同级子页，**完全不渲染 tab 内联的子组件** |
| **layoutMode（crud）** | ❌ 休眠 | `xview.xdef:72` 已声明 `bottom-detail`，`page_crud.xpl` 未引用（前序文档已证） |
| crud/picker/simple/tabs/wizard 顶层 | ✅ | 对应 page_*.xpl 存在 |

**这是所有"组合痛点"的真正根源**：schema 早已支持 complex/tab内联/分栏，codegen 没实现，导致：
- 项目被迫用 tabs + page 引用的间接模式（page-structure-patterns.md §2 机制 B 的割裂感）；
- 复杂页面无处组合，**子表只好下沉到 form 层**（simple 的 form 的 cell+view → input-table），这才是子表能力弱（无 toolbar/batch/分页）的根源；
- 用户感觉"复杂页面难配置"，但其实是 **schema 早已就绪、codegen 没落地**。

类比：xview.xdef 是一张设计完备的图纸，codegen 只盖了其中一角楼。

## 5. 对前几轮方案的纠正

| 前几轮提案 | 纠正 |
|-----------|------|
| 新增 complex page 类型 | ❌ 重新发明。`xview.xdef:195` 已有 complex（含 aside）。改进 = **新增 `page_complex.xpl` + `impl_GenPage` 加 complex 分支**（激活休眠），零新 schema |
| tab 内联组合 | ❌ 重新发明。`xview.xdef:159` tab 已是 UiContainer。改进 = **`page_tabs.xpl` 增加内联渲染分支**（tab 有内联子组件时直接渲染，而非 LoadPage） |
| form.body 结构化布局 | ⚠️ 仍成立但优先级降。xview.xdef 的 page 组合管不到 form 内字段排版；但复杂页主体应靠 complex/page 组合解决，form 内 layout 是次要场景 |
| cell 扩展（actions/onEvent） | ✅ 仍成立。field 级局部入口，与 page 组合正交 |
| 推定层治理 | ✅ 仍成立。删除手写控件/selection，回归 `<field name>` |

## 6. 重新确立的改进方向

不是"设计新 schema"，而是"**实现已有 schema + 治理推定 + 子表上移**"：

1. **激活 complex（平台 codegen）**：新增 `page_complex.xpl`（渲染主区 UiContainer + aside UiContainer 分栏）+ `impl_GenPage.xpl` 加 `complex` 分支。这是实现 `xview.xdef:195` 既定 schema，零新概念。
2. **激活 tab 内联（平台 codegen）**：`page_tabs.xpl:9` 增加分支——tab 有内联子组件（crud/simple/tabs）时直接经对应 page_*.xpl 渲染，无则保持 `LoadPage` 引用。实现 `xview.xdef:159` 既定 schema。
3. **子表上移到 page 层（根治子表能力弱）**：头行单据的子表从 form 层 input-table（cell+view）改为 complex/tab 内联的 `<crud bind="lines">`。子表用 crud 即获得 toolbar/batch/分页全能力。随头 cascade 提交经 `bind` + codegen 数据同步（见 layout-dsl 文档 §11.4）。
4. **激活 layoutMode（平台 codegen）**：`page_crud.xpl` 接线 `bottom-detail`（前序文档已述）。
5. **治理推定（零平台改动，先行）**：删除 view.xml 中手写 gen-control 控件与手写 gql:selection，回归 `<field name>`；补全 prop 的 depends/labelProp。见 `2026-07-31-1200-page-config-infer-assemble-architecture.md`。
6. **cell 扩展 actions/onEvent（field 级入口）**：少数偏离推定的字段用。见 `2026-07-31-1000-complex-page-pattern-catalog.md` §8。

**关键认识**：xview.xdef 的设计是对的，无需新 schema。改进 = 把 codegen 补齐到 schema 已定义的能力 + 让数据/控件由推定层自动填 + 子表上移到组合层。

## 7. 为什么 codegen 会落后于 schema（推测）

不明确，但可能：schema 设计先行（预留 complex/tab内联/layoutMode），codegen 按实际需求逐步实现，complex 等高级组合因早期无迫切需求而搁置；项目层为解决组合痛点，自行发展了 tabs+page引用（机制 B）和 form 内嵌子表（input-table）等"在已实现子集内打转"的变通，反而固化了"组合困难"的现状。打破循环的点 = 激活 complex/tab内联，让组合回归 schema 本意。

## 8. 参考

- schema：`nop-entropy/nop-kernel/nop-xdefs/.../_vfs/nop/schema/xui/xview.xdef:67(UiContainer)/151-162(tabs/tab)/190(wizard step)/194-198(pages+complex+aside)`
- codegen：`nop-entropy/nop-frontend-support/nop-web/.../_vfs/nop/web/xlib/web/impl_GenPage.xpl:19-29(无 complex 分支)`、`page_tabs.xpl:9(只 LoadPage 引用)`、`page_*.xpl(无 page_complex)`
- 关联文档：`2026-07-31-1100-form-layout-dsl-evolution.md` §11（complex 章节，已被本文纠正）、`2026-07-31-1200-page-config-infer-assemble-architecture.md`（推定层）、`2026-07-31-1000-complex-page-pattern-catalog.md` §8（cell 扩展）、`docs/design/page-structure-patterns.md` §2 机制 B（tabs 间接引用现状）
