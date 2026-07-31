# 页面配置的"推定 + 组装"架构（总纲）

> Status: 设计原则（统领 `2026-07-31-1000-complex-page-pattern-catalog.md` 与 `2026-07-31-1100-form-layout-dsl-evolution.md`）。
> 核心命题：复杂页面不应"每字段写一堆控件"。字段的控件、GraphQL selection（含 labelProp/depends 等隐藏信息）、格式化等，应由 meta+domain **自动推定**；页面只做**组装**（用哪些字段、怎么排列）和**局部覆盖**。

## 1. 问题：之前的方案没充分信任推定

前几轮讨论（cell 扩展、complex、form.body）都暗含一个假设："每个字段要在页面表达一大堆控件配置"。这导致页面臃肿，且与模型层重复。

实际上**绝大部分字段配置可以根据 domain/type 自动推定**，而 **GraphQL 要取哪些数据也是根据 form/grid 实际用到的字段推定的**——推定不仅要看显示字段，还要带上隐藏信息（`depends` 依赖字段、`labelProp` 显示属性等）。这些推定逻辑应从页面配置中**分解出去**，成为独立的、meta 驱动的机制。页面只剩"组装 + 局部覆盖"。

## 2. 推定层：平台已经实现（源码实证）

### 2.1 控件推定（domain/type → 控件）

`DefaultControl`（`web.xlib:655-671`）调用 `XuiHelper.getControlTag(controlLib, dispMeta, propMeta, objMeta, editMode)`（`XuiHelper.java:56/118`），按 `domain/stdDomain/relKind/stdDataType/editMode` 自动选控件 tag。项目经 `controlLib`（如 `/erp/xlib/control.xlib`）做全局 domain→控件映射。

即：写 `<field name="amount"/>`，控件由 `amount` 域自动推定为 input-number（带精度），无需手写。

### 2.2 GraphQL selection 推定（用到的字段 → selection，含隐藏信息）

`XuiViewAnalyzer.getFormSelection/getListSelection`（`XuiHelper.java:190/197`）从 form/grid 实际用到的字段推 `FieldSelectionBean`，且**已自动纳入隐藏信息**：

| 隐藏信息 | 推定逻辑 | 证据 |
|---------|---------|------|
| `labelProp`（FK 显示属性，如 materialId→material.name） | `addRelationDispProp` | `XuiViewAnalyzer:381/398` |
| `depends`（cell/col 声明的依赖字段） | `addPropDepends` + `addDepend` | `:279/360`，声明在 `disp.xdef:16` |
| PK 字段 | `appendPkFields` | `:179` |
| view 引用的依赖 | `addViewDepends` | `:315` |

即：form/grid 里写了 `<field name="materialId"/>`，推定会自动把 `material.name`（labelProp）和该字段 `depends` 声明的隐藏传递字段一起加进 GraphQL selection——页面不需要手写 `gql:selection`。

**结论**：用户描述的推定机制，平台层**已经完备**。"分解出去"不是要新建，是要在页面配置层**充分信任和利用**它。

## 3. 四层架构

| 层 | 职责 | 谁来做 | 现状 |
|---|------|-------|------|
| **推定层**（自动） | 控件、selection、格式化、校验、labelProp、depends | 平台（meta+domain 驱动） | ✅ 已实现（DefaultControl + XuiViewAnalyzer） |
| **组装层**（显式） | 用哪些字段、怎么排列组合（section/crud/tabs） | 页面配置（complex/body），**只写 name + 结构** | 本系列文档提案 |
| **分级局部入口** | form 级 / grid 级 / field 级，各自独立明确的覆盖入口 | 页面配置（仅偏离推定时用） | ✅ form.xdef/grid.xdef/disp.xdef；cell 扩展=field 级 |
| **全局字段处理** | 按 domain 统一控件/格式/色块，一处改全局生效 | controlLib + domain 映射 | ✅ `getControlTag` 用 controlLib，项目可自定义 |

### 3.1 推定层（自动，不在页面写）

- 控件：domain/type → 控件（§2.1）
- selection：用到的字段 → GraphQL selection，含 labelProp/depends/PK（§2.2）
- 格式化/校验/字典渲染：domain 驱动

### 3.2 组装层（页面只写 name + 结构）

页面配置只声明"用哪些字段（name）、怎么排列组合"。控件/selection/格式全部由推定层填。示例：

```xml
<complex name="detail">
  <section form="edit" title="基本信息">
    <layout>code orgId docDate status</layout>   <!-- 只写 name，控件/label 从 meta 推 -->
  </section>
  <tabs>
    <tab title="明细行">
      <crud name="lines" bind="lines" grid="sub-list">
        <cols><col name="materialId"/><col name="quantity"/><col name="amount"/></cols>
        <!-- materialId 的 picker、quantity 的 input-number、amount 的精度、
             selection 里的 material.name(labelProp) 全部自动推定，不写 -->
      </crud>
    </tab>
  </tabs>
</complex>
```

### 3.3 分级局部入口（仅偏离推定时用）

| 级别 | 入口 | 用途 |
|------|------|------|
| form 级 | `<form>` 属性（editMode/列数/initApi/提交端点） | 表单整体 |
| grid 级 | `<crud>/<grid>` 属性（分页/排序/批量） | 列表整体 |
| field/col 级 | `<field>/<col>` 上的 gen-control/visibleOn/actions/onEvent（cell 扩展） | 单字段覆盖推定 |

只有当某字段"偏离推定"（换控件、加显隐、加按钮）时，才在对应级别入口显式覆盖。大部分字段不需要任何 field 级配置。

### 3.4 全局字段处理（按 domain）

`controlLib` + `getControlTag` 的 domain→控件映射是全局的：所有 `amount` 域字段统一精度控件，所有 `status` dict 字段统一色块，一处改全局生效。项目自定义 controlLib 即可注入全局字段处理，无需逐页逐字段配置。

## 4. gap 与核心原则

**平台推定机制已完备，但当前 view.xml 实践没有充分信任它**——很多页面手写 `gen-control` 控件、手写 `gql:selection="{...}"`，既冗余又易与推定结果漂移（模型改了 domain，手写控件不跟随；手写 selection 漏了 labelProp 导致显示空白）。

**核心原则**：
1. **页面只写 name + 结构**（组装），能推定的绝不手写；
2. **只在偏离推定时局部覆盖**（分级入口）；
3. **全局统一处理走 controlLib 的 domain 映射**，不逐字段重复；
4. **手写 selection 是反模式**——selection 应由 `XuiViewAnalyzer` 从用到的字段自动推定（含 labelProp/depends），除非有特殊性能/安全裁剪需求。

## 5. 与本系列其他文档的关系（统领）

本文是顶层架构，统领：

- **`2026-07-31-1000-complex-page-pattern-catalog.md`**
  - §7 页面描述原语 → 组装层的原语集
  - §8 cell/view 扩展（actions/onEvent）→ **field 级局部入口**（偏离推定时的覆盖点）
- **`2026-07-31-1100-form-layout-dsl-evolution.md`**
  - complex（page 层）/ form.body（form 层）→ **组装层**的具体手法
  - 字段只写 name、label 从 meta → 推定层的利用

三者关系：**推定层（自动，本文 §2）+ 组装层（complex/body，layout-dsl 文档）+ 分级入口（cell 扩展=field 级，catalog §8）+ 全局处理（controlLib，本文 §3.4）**。

## 6. 落地优先级

1. **先治理（零平台改动）**：盘点现有 view.xml 中手写的 gen-control 控件和手写 gql:selection，凡能由 domain 推定的删除，回归 `<field name>`；补全 prop 的 `depends`/`labelProp` meta 让推定完整。这一步立即收益（页面变薄、消除漂移）。
2. **全局字段处理**：把重复的字段处理（色块/格式/通用控件）收敛进 `/erp/xlib/control.xlib` 的 domain 映射。
3. **组装层**（complex/body）：平台侧改动，待推定治理充分后再上，此时页面已薄，组装层只需组合 name + 结构。
4. **field 级入口**（cell 扩展 actions/onEvent）：少数偏离推定的字段才需要，平台最小补丁。

## 7. 参考

- 控件推定：`nop-entropy/nop-frontend-support/nop-web/.../_vfs/nop/web/xlib/web.xlib:655(DefaultControl)`、`nop-entropy/nop-frontend-support/nop-ui/src/main/java/io/nop/xui/utils/XuiHelper.java:56/118(getControlTag)`
- selection 推定：`nop-frontend-support/nop-ui/src/main/java/io/nop/xui/utils/XuiViewAnalyzer.java:190/197(getFormSelection/getListSelection)/279(addPropDepends)/360(addDepend)/381(addRelationDispProp)/398(getLabelProp)/179(appendPkFields)/315(addViewDepends)`
- schema：`disp.xdef:16(depends)/30(displayProp)`、`form.xdef`、`grid.xdef`
- 关联文档：`2026-07-31-1000-complex-page-pattern-catalog.md`（§7 原语/§8 cell 扩展=field 级入口）、`2026-07-31-1100-form-layout-dsl-evolution.md`（complex/body=组装层）
