---
name: nop-frontend-dev
description: Nop平台前端页面开发（AMIS view.xml / page.yaml）。涵盖XView三层模型、grid/form/page定制、bounded-merge、Delta覆盖、业务动作按钮。触发词：前端开发、页面、view.xml、AMIS、grid、form、弹窗、drawer、表格、表单。
---

# Nop 前端页面开发

## 什么时候用我

| 场景 | 触发关键词 |
|------|-----------|
| 创建/修改后台管理页面 | "页面"、"view.xml"、"AMIS"、"后台页" |
| 调整列表/表格列 | "grid"、"列表"、"表格"、"列" |
| 调整表单 | "form"、"表单"、"编辑"、"新增" |
| 组织页面结构 | "crud"、"tabs"、"drawer"、"弹窗"、"page" |
| Delta覆盖平台页面 | "覆盖平台页面"、"Delta定制" |
| 添加业务按钮 | "按钮"、"业务动作"、"批量操作" |
| 子表编辑 | "子表"、"child-table"、"关联编辑" |

---

## 必读文档路径

以下路径相对于 `{DOCS-FOR-AI}` 目录。`{DOCS-FOR-AI}` 的实际位置由当前项目 `AGENTS.md` 的 "Nop Platform Documentation" 部分指定（本项目中为 `../nop-entropy/docs-for-ai/`）。

### 全局必读（写任何前端/页面代码前全部读完）

| 文档 | 为什么必读 | 不读会怎样 |
|------|-----------|-----------|
| `00-start-here/application-project-defaults.md` | 决策顺序（Model→Delta→Java）、view.xml三层模型 | 优先级搞反，手改生成物 |
| `02-core-guides/view-and-page-customization.md` | view.xml三层模型、bounded-merge、x:prototype模式、生成物与定制物关系 | **编辑生成物→下次重新生成被覆盖**；merge策略选错→定制丢失或冲突 |
| `02-core-guides/delta-customization.md` | Delta机制原理、`_vfs/_delta/` 位置、`x:extends="super"`、合并规则 | 覆盖平台页面时直接改基础模块源码→升级丢失；Delta路径写错→不生效 |
| `03-runbooks/prefer-delta-over-direct-modification.md` | 何时用Delta vs 非下划线扩展文件 | 不该Delta时Delta，该Delta时直接改 |

### 按场景选读

| 场景 | 首选手册 | 第二步 |
|------|---------|--------|
| 字段从模型走到页面 | `03-runbooks/make-field-reach-page.md` | `03-runbooks/add-field-and-validation.md` |
| 字段改成复杂控件 | `03-runbooks/replace-field-with-complex-control.md` | `02-core-guides/page-dsl-pattern-catalog.md` |
| 父页面编辑子表 | `03-runbooks/add-child-table-editor-to-page.md` | `03-runbooks/build-related-drawer-page.md` |
| 页面加业务按钮 | `03-runbooks/add-page-business-action.md` | `03-runbooks/write-bizmodel-method.md` |
| 树形后台页 | `03-runbooks/build-tree-crud-page.md` | `02-core-guides/page-dsl-pattern-catalog.md` |
| 关联drawer页 | `03-runbooks/build-related-drawer-page.md` | `03-runbooks/add-runtime-or-related-page.md` |
| Tabs工作台页 | `03-runbooks/build-tabs-workspace-page.md` | `03-runbooks/build-admin-workspace-page.md` |
| 导出/批量操作 | `03-runbooks/add-export-or-batch-operations.md` | `03-runbooks/add-page-business-action.md` |
| Delta覆盖平台页面 | `03-runbooks/override-platform-page-with-delta.md` | `02-core-guides/delta-customization.md`, `03-runbooks/prefer-delta-over-direct-modification.md` |
| 认证与权限（页面可见性） | `02-core-guides/auth-and-permissions.md` | |
| 复杂页面DSL模式 | `02-core-guides/page-dsl-pattern-catalog.md` | |

---

## 自检纪律

**每完成一个 `.view.xml` / `.page.yaml` 文件后，对照下方反模式表逐项校验。** 自检在每个文件完成后执行，不是逐操作检查。任何不通过项必须修复后才能提交。

---

## 项目文件位置

| 你要找什么 | 典型位置 |
|-----------|---------|
| 保留层 view 文件 | `app-mall-web/src/main/resources/_vfs/app/mall/pages/{Entity}/{Entity}.view.xml` |
| 生成 view 文件 | `app-mall-web/src/main/resources/_vfs/app/mall/pages/{Entity}/_gen/_{Entity}.view.xml` |
| 页面文件 | `app-mall-web/src/main/resources/_vfs/app/mall/pages/{Entity}/*.page.yaml` |
| Delta覆盖平台页 | `app-mall-delta/src/main/resources/_vfs/_delta/default/nop/auth/pages/...` |

---

## XView 三层模型

```
view.xml
├── grids    → 列表/表格 (grid)
├── forms    → 表单 (form: edit/add/query)
└── pages    → 页面组织 (crud/simple/tabs/picker)
```

### 最小理解模型

```xml
<view x:extends="_gen/_LitemallOrder.view.xml"
      x:schema="/nop/schema/xui/xview.xdef"
      xmlns:x="/nop/schema/xdsl.xdef">

    <grids>
        <grid id="list">
            <cols x:override="bounded-merge">
                <col id="id"/>
                <col id="orderSn" label="订单编号"/>
            </cols>
        </grid>
    </grids>

    <forms>
        <form id="edit" size="lg">
            <layout>
                orderSn[订单编号] orderStatus[订单状态]
            </layout>
        </form>
    </forms>

    <pages>
        <crud name="main" grid="list" filterForm="query">
            <rowActions x:override="bounded-merge">
                <action id="row-update-button" actionType="drawer"/>
            </rowActions>
        </crud>
    </pages>
</view>
```

---

## 核心操作模式

### 1. x:override — 继承链（x:extends）中的合并算子

用于 `x:extends` 继承链中控制当前节点如何与基类合并：

| 值 | 语义 |
|----|------|
| `merge`（缺省） | 逐级合并：同名属性覆盖，同名子节点递归合并，新增子节点追加 |
| `replace` | 当前节点完全替换基础模型中的对应节点 |
| `remove` | 从结果中删除该节点 |
| `bounded-merge` | 以**当前节点**为准——只保留当前节点显式列出的子节点，基础模型中存在但当前节点未列出的子节点会被**丢弃** |
| `merge-replace` | 合并属性，但子节点或内容完全替换 |

**`bounded-merge` 适合的场景：** 继承了一个很大的列表配置（cols、rowActions、listActions），但你只想保留其中一小部分。它等价于"白名单"——你写什么就留什么，没写的全部丢弃。

```xml
<!-- 只保留 id 和 name 两列，其余继承来的列全部丢弃 -->
<cols x:override="bounded-merge">
    <col id="id"/>
    <col id="name" label="名称"/>
</cols>
```

### 2. x:prototype — 克隆同层级兄弟节点作为模板

基于同一个 view 中已存在的 grid/form/crud 克隆出变体。变体之间通常只有 filter、action 的局部差异。

```xml
<!-- 克隆 "list" grid 作为 "wait-list" 的起点 -->
<grid id="wait-list" x:prototype="list"/>

<!-- 克隆 "main" crud 作为 "wait-approve" 的起点 -->
<crud name="wait-approve" x:prototype="main"/>

<!-- 克隆 "edit" form 作为 "add" form -->
<form id="add" x:prototype="edit" editMode="add"/>
```

### 3. x:prototype-override — prototype 变体的合并算子

合并分两个阶段：先 `x:extends` 继承合并（用 `x:override`），再 `x:prototype` 克隆合并（用 `x:prototype-override`）。需要两个属性是因为同一节点会经历两次合并，可能需要不同策略。

`x:prototype-override` 支持与 `x:override` 完全相同的值，但只在 prototype 克隆阶段生效：

```xml
<!-- 从 "main" crud 克隆，但 rowActions 只保留自己列出的按钮 -->
<crud name="wait-approve" x:prototype="main">
    <listActions x:prototype-override="remove"/>
    <rowActions x:prototype-override="bounded-merge">
        <action id="row-approve-button"/>
    </rowActions>
</crud>
```

**什么时候用 `x:prototype-override`：** 当你用 `x:prototype` 克隆了一个变体，但需要裁剪克隆结果中的某些子节点（如只保留部分按钮、删除部分 action）时使用。

**区别总结：**

| 场景 | 用哪个 | 例子 |
|------|--------|------|
| `x:extends` 继承时裁剪子节点 | `x:override="bounded-merge"` | `<cols x:override="bounded-merge">` |
| `x:prototype` 克隆后裁剪子节点 | `x:prototype-override="bounded-merge"` | `<rowActions x:prototype-override="bounded-merge">` |

### 4. 删除继承/克隆来的节点

```xml
<layout x:override="remove"/>
```

### 5. 复用已有 form 结构（x:prototype）

```xml
<form id="add" x:prototype="edit" editMode="add"/>
```

### 6. 无 objMeta 的表单必须配置 domain

```xml
<form id="queryForm" editMode="edit" title="查询">
    <layout>
        indexId[索引ID] qualifiedName[全限定类型名]
    </layout>
    <cells>
        <cell id="indexId" label="索引ID" domain="string"/>
        <cell id="qualifiedName" label="全限定类型名" domain="string"/>
    </cells>
</form>
```

### 7. 自定义字段（非实体属性）

```xml
<cell id="__password2" custom="true" notSubmit="true"/>
```

### 8. Delta 覆盖平台页面

```xml
<!-- _delta/default/nop/auth/pages/NopAuthUser/NopAuthUser.view.xml -->
<view x:extends="super" ...>
    <!-- 只写需要改的部分 -->
</view>
```

---

## 页面任务路由

先判断任务类型，再走对应路径：

1. **字段上屏** → `make-field-reach-page.md`
2. **复杂控件** → `replace-field-with-complex-control.md`
3. **子表编辑** → `add-child-table-editor-to-page.md`
4. **业务按钮** → `add-page-business-action.md`
5. **树形/关联/工作台** → 对应 runbook
6. **平台页面覆盖** → `override-platform-page-with-delta.md`

---

## 反模式表

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 直接改 `_gen/_*.view.xml` | 改保留层 `*.view.xml`（与 `_gen` 同级） |
| 直接改平台模块源码 | 用 `_vfs/_delta/default/...` + `x:extends="super"` |
| 复制整份生成view做微调 | 用 `x:extends="_gen/..."` + `bounded-merge` 只改差异 |
| 只隐藏几列却复制整个grid | `<cols x:override="bounded-merge">` 只列要保留的 |
| 该改grid/form/page没分清 | grid=列表列、form=表单字段、page=页面结构 |
| prototype变体的子节点用 `x:override` | prototype变体用 `x:prototype-override` |
| 表单字段非实体属性没加custom | `<cell id="xxx" custom="true"/>` |
| 无objMeta的表单没配domain | 每个cell配 `domain` 和 `label` |
| `<simple>` 缺少 `form` 属性 | 每个 `<simple>` 必须有 `form="formId"` |
| CRUD引用abstract grid | CRUD需要非abstract的grid |
| editMode="query" 用错 | query模式自动加 `filter_` 前缀，不需要前缀用 `edit` |
| Delta文件忘写 `x:extends="super"` | Delta文件必须 `x:extends="super"` |

---

## 项目内高价值样例

| 样例 | 位置 | 展示的模式 |
|------|------|-----------|
| 生成view上的深度定制 | `LitemallGoods/LitemallGoods.view.xml` | bounded-merge、gen-control、外部子表view引用、x:prototype复用、drawer编辑 |
| 状态分组+tabs页面 | `LitemallAftersale/LitemallAftersale.view.xml` | x:prototype克隆grid、不同filter形成分组、tabs组装 |
| 可复用page.yaml片段 | `LitemallGoods/attributes.page.yaml` | 外部page片段引用 |
| Delta覆盖+feature开关 | `app-mall-delta/.../NopAuthUser.view.xml` | `_delta/default/` 覆盖、`feature:on` 切换布局 |

---

## 参考文件

- `{DOCS-FOR-AI}/03-runbooks/admin-page-development-roadmap.md` — 页面任务路由总入口
- `{DOCS-FOR-AI}/02-core-guides/view-and-page-customization.md` — view.xml完整规范
- `{DOCS-FOR-AI}/02-core-guides/page-dsl-pattern-catalog.md` — 复杂页面DSL模式目录
