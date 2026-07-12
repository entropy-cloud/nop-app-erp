# View.xml Form Layout 实现差距分析

> 审计日期：2026-07-12
> 审计范围：nop-app-erp 全 19 域 338 个实体视图（676 个 `.view.xml` + 676 个 `.xmeta`）
> 审计方法：对照 Nop Platform `xview.xdef` / `form.xdef` schema、`web.xlib` 渲染管线源码、`LayoutModelParser` 布局语法解析器，逐项验证三个常见表单布局要求的实现状态

---

## 1. 平台能力与项目现状总览

### 1.1 Nop Platform 的表单布局能力（平台已提供）

平台 `form.xdef` + `LayoutModelParser` + `web.xlib` 提供了一套完整的表单布局 DSL：

| 能力 | 平台机制 | 语法 | 文档位置 |
|------|---------|------|---------|
| 字段分组 | Layout group | `===========`>`groupId[组标题]======` | `form.xdef:36`, `LayoutModelParser:94-195` |
| 缺省收缩 | Folded group | `^groupId[组标题]` 或 `<groupId[组标题]` | `LayoutModelParser:119-123` |
| 可折叠但展开 | Foldable group | `>`groupId[组标题]` | `LayoutModelParser:124-128` |
| Tab 布局 | layoutControl | `<form layoutControl="tabs">` | `form.xdef:5`, `web.xlib:125-148` |
| 字段必填标记 | Cell modifier | `*fieldName[标签]` | `LayoutModelParser:296` |
| 字段只读标记 | Cell modifier | `@fieldName[标签]` | `LayoutModelParser:297` |
| 隐藏标签 | Cell modifier | `!fieldName` | `LayoutModelParser:298` |
| 列合并 | Cell span | `fieldName(2)` 或 `fieldName(1,2)` | `LayoutModelParser:304-313` |
| 分割线 | Divider row | `----` 单独一行 | `LayoutModelParser:223-228` |
| 弹窗大小 | size 属性 | `<form size="lg">` | `form.xdef:22`, 自动推导 <5=sm / ≥20=lg |
| 查询运算符 | filterOp | `<cell filterOp="like">` 或 xmeta `ui:filterOp="like"` | `disp.xdef:22`, `web.xlib:373-374` |
| 查询必填 | ui:queryMandatory | xmeta `<prop ui:queryMandatory="true">` | `obj-schema.xdef:92`, `web.xlib:370-371` |
| 查询字段控制 | queryable | xmeta `<prop queryable="true/false">` | `obj-schema.xdef:87`, `ObjMetaBasedFilterValidator:48-87` |
| 允许运算符集 | allowFilterOp | xmeta `<prop allowFilterOp="eq,in,like">` | `obj-schema.xdef:89` |

### 1.2 nop-app-erp 项目现状

| 要求 | 平台支持 | 项目实现 | 差距 |
|------|---------|---------|------|
| 1. 技术主键 `id` 不显示 | ✅ 可在 layout 中省略 + xmeta `internal="true"` | ❌ 337/338 实体的表单包含 `id[ID]` 作为首字段 | **严重** |
| 2. 字段分组 + 缺省收缩 | ✅ layout 分组语法 + `^` 折叠标记 | ❌ 全 338 实体零分组，95% view 表单 >10 字段全平铺 | **严重** |
| 3. 查询表单配置 | ✅ `<form id="query">` + `filterOp` + `ui:filterOp` | ❌ 全 338 实体查询表单为空壳，零 filterOp 配置 | **严重** |

---

## 2. 逐项详细分析

### 2.1 要求 1：技术主键 `id` 不应显示

#### 2.1.1 当前实现

**337/338 实体的 `view` / `edit` / `add` 表单均将 `id[ID]` 作为第一个字段。**

codegen 模板 `view-gen.xlib:GenForm` 遍历 xmeta 的所有 prop（仅跳过 `internal="true"`），生成 layout 文本。由于 `id` prop 未标记 `internal="true"`，它被生成为 layout 首字段：

```xml
<!-- _gen/_ErpPurOrder.view.xml L143-167（全部 338 个 _gen 文件同范式） -->
<form id="view" editMode="view" title="查看-采购订单">
    <layout>
 id[ID] code[单号]                          ← 技术主键暴露给用户
 orgId[业务组织] requisitionId[请购单]
 ...
```

**实际渲染效果**：
- `add` 表单：`id` 可输入（`insertable="true"`），但因 `tagSet="seq-default"` 框架自动生成。用户看到一个无业务含义的 ID 输入框。
- `edit` 表单：`id` 只读（`updatable="false"`），渲染为 static 文本。用户看到一行无意义的数字 ID。
- `view` 表单：`id` 只读，同上。

#### 2.1.2 仅有的例外（1/338）

`ErpInvLandedCost.view.xml` 是唯一移除 `id` 字段的自定义表单（定制层重写了 `<form id="edit">` 的 layout，直接省略 `id`）。

#### 2.1.3 平台的正确做法

有两种方式隐藏技术主键：

**方式 A（推荐）：在 layout 中省略 `id`**

在定制层 `view.xml` 中重写 `<form id="edit">` 的 `<layout>`，不包含 `id` 字段。`id` 仍在 GraphQL selection 中自动传递（`XuiViewAnalyzer.appendPkFields` 保证主键始终在查询中），只是不显示。

```xml
<form id="edit">
    <layout x:override="replace">
        code[单号] businessDate[业务日期]
        supplierId[供应商] status[状态]
    </layout>
</form>
```

**方式 B：在 xmeta 中标记 `internal="true"`**

```xml
<prop name="id" internal="true" .../>
```

codegen 会自动跳过 `internal="true"` 的 prop，不生成到 layout 和 cells 中。但这种方式会影响所有表单（包括可能需要查看 id 的调试场景），且 `internal` 更多用于真正的内部系统字段。

#### 2.1.4 影响范围

| 维度 | 数据 |
|------|------|
| 受影响实体数 | 337/338（99.7%） |
| 受影响表单数 | 337 × 3 (view + edit + add) = ~1011 个表单 |
| 修复方式 | 定制层 view.xml 重写 layout（每实体 1 处）；或修改 codegen 模板 |

---

### 2.2 要求 2：字段分组 + 缺省收缩

#### 2.2.1 当前实现

**全 338 实体零分组。** 统计数据：

| 表单类型 | >10 字段 | >15 字段 | >20 字段 | >30 字段 |
|---------|---------|---------|---------|---------|
| view 表单 | 322 (95%) | 217 (64%) | 98 (29%) | 18 (5%) |
| edit 表单 | 206 (61%) | 81 (24%) | 25 (7%) | 4 (1%) |

**最大的 5 个表单（全部无分组）**：

| 实体 | view 字段数 | edit 字段数 |
|------|-----------|-----------|
| ErpMfgWorkOrder（工单） | **43** | 33 |
| ErpCrmLead（线索） | 40 | 35 |
| ErpLogShipment（运单） | 40 | 34 |
| ErpPurOrder（采购订单） | 39 | 25 |
| ErpSalOrder（销售订单） | 39 | 25 |

这些表单渲染为 AMIS 的平铺两列 grid——用户需要滚动很长才能看到底部的字段，无法快速定位关注的字段组。

#### 2.2.2 平台分组语法示例

平台内置模块 `NopAuthUser.view.xml` 展示了正确的分组写法：

```xml
<layout>
    ===========>baseInfo[基本信息]======
    userName status[用户状态]
    nickName[昵称] deptId[部门]
    userType[用户类型] gender[性别]
    email[邮件] phone[电话]
    ===========>extInfo[扩展信息]=========
    idType[证件类型] idNbr[证件号]
    birthday[生日] workNo[工号]
    remark[备注]
</layout>
```

**折叠语法**（用户较少查看的字段缺省收缩）：

```xml
<layout>
    ===========>baseInfo[基本信息]======
    code[单号] status[状态]
    ===========^advInfo[高级信息]=========     ← ^ 表示缺省折叠
    internalNote[内部备注] tags[标签]
    customField1[自定义1] customField2[自定义2]
</layout>
```

渲染为 AMIS `<fieldSet collapsable="true" collapsed="true">`。

Tab 布局（字段组非常多时）：

```xml
<form id="edit" layoutControl="tabs">
    <layout>
        ===========>baseInfo[基本信息]======
        ...
        ===========>finance[财务信息]======
        ...
        ===========>audit[审计信息]=========
        ...
    </layout>
</form>
```

#### 2.2.3 推荐分组策略

对于 ERP 实体，建议按以下模式分组（以 ErpPurOrder 为例）：

| 分组 | 展开/折叠 | 字段 |
|------|----------|------|
| **基本信息**（`baseInfo`） | 展开 | code, businessDate, supplierId, warehouseId, currencyId, exchangeRate, status |
| **金额信息**（`amountInfo`） | 展开 | totalAmount, totalAmountWithTax, totalTaxAmount, discountAmount, paidAmount |
| **业务关联**（`bizRef`） | 折叠（`^`） | requisitionId, quotationId, projectId, costCenterId |
| **审批信息**（`approval`） | 折叠（`^`） | approveStatus, approvedBy, approvedAt, submittedBy, submittedAt |
| **审计信息**（`audit`） | 折叠（`^`） | createdBy, createTime, updatedBy, updateTime, remark |
| **技术字段**（不显示） | — | id（从 layout 中移除） |

---

### 2.3 要求 3：查询表单配置

#### 2.3.1 当前实现

**全 338 实体的查询表单为空壳**：

```xml
<!-- 全部 338 个 _gen 文件的查询表单 -->
<form id="query" editMode="query" title="查询条件" x:abstract="true"/>
```

**零 `filterOp` / `ui:filterOp` / `allowFilterOp` 配置**——在整个 ERP 代码库中没有任何查询运算符定制。

**7,572 个 xmeta prop 标记了 `queryable="true"`**（codegen 对几乎所有实体属性默认生成），但：
- 查询表单 `<form id="query">` 为空壳（`x:abstract="true"`），用户没有结构化查询界面
- 没有配置哪些字段应该出现在查询条件中
- 没有 `filterOp` 配置——文本字段（如物料名称、往来单位名称）应该用 `like` 查询而非默认的 `eq`，但当前未配置

#### 2.3.2 平台查询表单机制

**查询运算符解析顺序**（`web.xlib:GenFormSimpleCell:373-374`）：

```
cell.filterOp → prop.ui:filterOp → cell.ui:filterOp → prop.xui:defaultFilterOp
```

如果都没配置，查询字段名为 `filter_{propName}`，默认使用 `eq` 运算符。

配置后，查询字段名变为 `filter_{propName}__{filterOp}`，例如 `filter_materialName__like`。

**后端校验**（`ObjMetaBasedFilterValidator:48-87`）：
- `queryable="false"` 的 prop 不能作为查询条件（抛 `ERR_BIZ_PROP_NOT_SUPPORT_QUERY`）
- `allowFilterOp` 未配置时，默认允许 `{EQ, IN, DATE_BETWEEN, DATETIME_BETWEEN}`
- `allowFilterOp` 配置后，只允许列出的运算符

#### 2.3.3 推荐查询表单配置

以采购订单为例，应该为关键字段配置查询运算符：

```xml
<form id="query" editMode="query" title="查询条件">
    <layout>
        code[单号] status[状态]
        supplierId[供应商] businessDate[业务日期]
        approveStatus[审批状态]
    </layout>
    <cells>
        <cell id="code" filterOp="like"/>
        <cell id="status"/>
        <cell id="supplierId"/>
        <cell id="businessDate" filterOp="date-between"/>
        <cell id="approveStatus"/>
    </cells>
</form>
```

对应 xmeta 应配置 `ui:filterOp`：

```xml
<prop name="code" queryable="true" ui:filterOp="like"/>
<prop name="businessDate" queryable="true" ui:filterOp="date-between"/>
```

---

## 3. 差距根因分析

### 3.1 为什么三个要求都未实现？

**根因：codegen 模板 `view-gen.xlib:GenForm` 按机械规则生成表单，不做业务语义判断。**

```
xmeta 全部 prop（排除 internal）
    → codegen 逐一生成 layout 行：id[ID], code[单号], orgId[业务组织]...
    → 不分组、不折叠、不配置查询运算符
    → 定制层 view.xml 只改了 grid 列（bounded-merge），几乎不改表单
```

具体：
1. **`id` 显示**：codegen 遍历所有非 `internal` prop，`id` 未标记 `internal`，所以被生成。定制层未覆盖表单 layout。
2. **不分组**：codegen 不知道哪些字段属于"基本信息"哪些属于"高级信息"——这需要业务知识。定制层没有手工添加分组。
3. **查询表单为空壳**：codegen 生成 `<form id="query" x:abstract="true"/>`，等待人工定制。定制层没有填充查询字段。

### 3.2 修复路径

| 要求 | 修复位置 | 修复方式 | 批量可行性 |
|------|---------|---------|-----------|
| id 不显示 | 定制层 view.xml | 重写 `<form>` 的 `<layout>`，省略 `id` | 可按域批量处理 |
| 字段分组 | 定制层 view.xml | 重写 `<form>` 的 `<layout>`，添加 `=====`>`group[label]` 分组标记 | 需逐实体设计分组方案 |
| 查询表单 | 定制层 view.xml + xmeta | 填充 `<form id="query">` 的 layout + cells + filterOp | 需逐实体选择查询字段 |

---

## 4. 影响面与工作量评估

### 4.1 按优先级分批

| 优先级 | 工作项 | 影响实体数 | 难度 | 建议 |
|--------|-------|-----------|------|------|
| P0 | id 字段从表单 layout 中移除 | 337 | 低（机械修改） | 全域批量执行 |
| P1 | 核心交易实体字段分组（view + edit） | ~40（高频实体） | 中（需业务知识设计分组） | 按域分批 |
| P1 | 核心交易实体查询表单配置 | ~40 | 中（需选择查询字段 + 运算符） | 按域分批 |
| P2 | 全部实体字段分组 + 查询表单 | ~298（低频实体） | 中（同上但量大） | 渐进推进 |

### 4.2 最需要优先处理的实体（字段数 ≥ 25 的 edit 表单）

| 实体 | edit 字段数 | 分组建议 |
|------|-----------|---------|
| ErpCrmLead | 35 | 基本信息 / 联系方式 / 来源跟踪 / 需求描述 / 状态 |
| ErpLogShipment | 34 | 基本信息 / 承运信息 / 费用 / 状态跟踪 |
| ErpHrEmployee | 33 | 基本信息 / 联系方式 / 雇佣信息 / 薪酬 / 状态 |
| ErpMfgWorkOrder | 33 | 基本信息 / BOM / 计划 / 状态 / 完工信息 |
| ErpPurOrder | 25 | 基本信息 / 金额 / 业务关联 / 审批 |
| ErpSalOrder | 25 | 基本信息 / 金额 / 业务关联 / 审批 |

---

## 5. 与 NopAuthUser（平台参考实现）的对标

`NopAuthUser.view.xml` 是平台内置的最佳实践参考：

| 维度 | NopAuthUser | nop-app-erp（全部 338 实体） |
|------|------------|---------------------------|
| id 显示 | 不在 layout 中显示 | 显示为 `id[ID]` 首字段 |
| 字段分组 | 2 组（baseInfo + extInfo） | 无分组 |
| 折叠 | extInfo 缺省展开（可折叠） | 无折叠 |
| 查询表单 | 有结构化查询字段 | 空壳 `x:abstract="true"` |
| filterOp | 配置了查询运算符 | 零配置 |
| 自定义控件 | `__password2` 用 `gen-control` | 仅 ErpCsTicket 有 `__kbSuggestion` |

---

## 6. 建议的行动方案

### 6.1 短期（P0：id 字段隐藏）

全域批量在定制层 view.xml 重写 `<form>` layout，移除 `id` 字段。这是一个纯机械修改，每实体改 1 处（edit 表单 layout），add 表单经 `x:prototype="edit"` 自动继承。

### 6.2 中期（P1：核心实体分组 + 查询表单）

按域分批处理高频核心实体（采购订单/入库/发票/付款，销售订单/出库/发票/收款，库存移动/余额，凭证/科目，物料/SKU/往来单位，BOM/工单等），每个实体：
1. 设计字段分组方案（基本信息 / 金额 / 业务关联 / 审批 / 审计）
2. 折叠低频字段组（审批、审计信息缺省收缩）
3. 配置查询表单（选择 5-8 个关键查询字段 + 运算符）

### 6.3 长期（P2：全量覆盖 + codegen 模板增强）

对剩余低频实体渐进推进分组和查询表单配置。同时考虑增强 codegen 模板：
- 利用 ORM `<column>` 的 `tagSet` 自动推导分组（如标记 `audit` 的字段自动归入"审计信息"组并折叠）
- 利用 ORM `<column>` 的 `ext:*` 扩展属性标记查询字段和运算符，codegen 自动生成查询表单

---

## 7. docs-for-ai 文档完整性评估

### 7.1 结论：`<layout>` 配置文档严重不完整

`<layout>` 元素内部是一个自定义文本 DSL，由 `LayoutModelParser.java` 解析为 `LayoutModel` 对象。docs-for-ai 只展示了最基础的"字段名逐行排列"写法，**大量语法特性完全未文档化**。

### 7.2 逐项对比：源码能力 vs docs-for-ai 文档

| # | 语法特性 | 写法 | 源码位置 | docs-for-ai |
|---|---------|------|---------|:-----------:|
| 1 | 字段平铺 | `fieldName` 每行 1-N 个 | `parseSimpleCell:295` | ✅ 有示例 |
| 2 | 字段标签 | `fieldName[中文标签]` | `parseSimpleCell:296-301` | ✅ 有示例 |
| 3 | **分组标题** | `===========`>`groupId[组名]======` | `parseGroupLine:87-195` | ❌ 仅 `external-app-examples.md` 示例 10 出现，**无任何文字解释** |
| 4 | **缺省折叠** | `^groupId[组名]` 或 `<groupId[组名]` | `parseGroupLine:119-123` | ❌ 零提及 |
| 5 | **可折叠但展开** | `>`groupId[组名]` | `parseGroupLine:124-128` | ❌ 零提及 |
| 6 | **必填标记** | `*fieldName[label]` | `parseSimpleCell:284-285` | ❌ 零提及 |
| 7 | **只读标记** | `@fieldName[label]` | `parseSimpleCell:286-287` | ❌ 零提及 |
| 8 | **隐藏标签** | `!fieldName` | `parseSimpleCell:288-289` | ❌ 零提及 |
| 9 | **跨列合并** | `fieldName(2)` 或 `fieldName(rowSpan,colSpan)` | `parseSimpleCell:304-317` | ❌ 零提及 |
| 10 | **分割线** | `----` 单独一行 | `parseRow:223-228` | ❌ 零提及 |
| 11 | **嵌套分组** | `#` `##` `###`（1-5 级） | `parseGroupLine:131-137` | ❌ 零提及 |
| 12 | **分组结束** | 纯 `===` 行（无后续内容） | `parseGroupLine:102-110` | ❌ 零提及 |
| 13 | **分组跨列** | `===...===groupId[label](colSpan)` 或 `(rowSpan,colSpan)` | `parseGroupLine:160-171` | ❌ 零提及 |
| 14 | **Tab 布局** | `<form layoutControl="tabs">` | `form.xdef:5`, `web.xlib:125-148` | ❌ 零提及 |
| 15 | **向导布局** | `<form layoutControl="wizard">` | `form.xdef:5` | ❌ 零提及 |
| 16 | **弹窗大小 size** | `<form size="lg">` | `form.xdef:22`, `web.xlib:704-712` | ❌ 零提及 |
| 17 | **自动推导 size** | cellCount <5→sm, ≥20→lg, else→md | `GetFormDefaultSize:704-712` | ❌ 零提及 |
| 18 | **查询运算符** | cell `filterOp="like"` / xmeta `ui:filterOp="like"` | `disp.xdef:22`, `web.xlib:373-374` | ❌ 零提及 |
| 19 | **查询必填** | xmeta `ui:queryMandatory="true"` | `obj-schema.xdef:92` | ❌ 零提及 |
| 20 | **允许运算符集** | xmeta `allowFilterOp="eq,in,like"` | `obj-schema.xdef:89` | ❌ 零提及 |
| 21 | **查询字段名映射** | `filter_{prop}__{op}` | `web.xlib:381-382` | ❌ 零提及 |
| 22 | **后端查询校验** | `ObjMetaBasedFilterValidator` | `ObjMetaBasedFilterValidator:48-87` | ❌ 零提及 |

**覆盖率：2/22 = 9%。** docs-for-ai 只文档化了第 1-2 项（字段名 + 标签），其余 20 项全部未文档化。

### 7.3 docs-for-ai 中的实际内容

**`view-and-page-customization.md` 关于 layout 的全部内容**（唯一有 layout 写法示例的文档）：

```xml
<form id="edit" size="lg">
    <layout>
        userName[用户名] nickName[昵称]
        email[邮箱] status[状态]
    </layout>
    <cells>
        <cell id="userName" mandatory="true"/>
        <cell id="email" readonly="false"/>
    </cells>
</form>
```

仅此而已。没有分组语法、没有折叠标记、没有 cell 前缀修饰符、没有 colspan、没有 layoutControl。

**`external-app-examples.md` 示例 10** 是唯一出现分组语法的地方：

```xml
<layout>
    ===========>baseInfo[基本信息]======
    userName status[用户状态]
    ...
    ===========>extInfo[扩展信息]=========
    ...
</layout>
```

但这个示例**只在展示 `feature:on` 条件布局**，`===========`>`分组语法本身没有一句解释。读者无法从中推断：
- `===========` 是什么意思
- `>` 是什么意思
- `^` 和 `<` 是否存在
- 是否可以不用 `>` 来创建不可折叠的分组

### 7.4 获取完整 layout 语法的唯一途径

| 途径 | 位置 | 可行性 |
|------|------|--------|
| `LayoutModelParser.java` 源码 | `nop-kernel/nop-xlang/.../layout/parse/LayoutModelParser.java` | 最权威，但需要读 Java 源码 |
| 平台模块示例 | `nop-auth/.../NopAuthUser.view.xml`, `nop-auth/.../NopAuthResource.view.xml` | 可从示例反推，但覆盖不全 |
| `form.xdef` schema | `nop-kernel/nop-xdefs/.../_vfs/nop/schema/xui/form.xdef` | 只有属性声明，无 layout DSL 语法 |
| `web.xlib` 渲染逻辑 | `nop-frontend-support/nop-web/.../_vfs/nop/web/xlib/web.xlib` | 可了解渲染机制，但非语法文档 |

### 7.5 影响

docs-for-ai 的 layout 文档缺失直接导致：
1. **nop-app-erp 全 338 实体零分组**——开发者不知道分组语法存在
2. **nop-app-erp 全 338 实体零折叠**——开发者不知道 `^` / `<` 标记存在
3. **nop-app-erp 查询表单全空壳**——开发者不知道 `filterOp` / `ui:filterOp` 配置方式
4. **cell 修饰符未使用**——`*` / `@` / `!` 从未出现在 ERP 代码中

**建议**：向 nop-entropy `docs-for-ai` 补充一份 `layout-syntax-reference.md`，将 `LayoutModelParser.java` 支持的全部语法特性文档化。

> **已修复**：`docs-for-ai/02-core-guides/layout-syntax-reference.md` 已创建（2026-07-12），完整文档化了分组、折叠、Tab 布局、Cell 修饰符、跨列、分割线、查询运算符等全部 22 项语法特性。`INDEX.md`、`view-and-page-customization.md`、`frontend-rendering-pipeline.md`、`page-dsl-pattern-catalog.md` 均已添加交叉引用。

---

## 8. 附录

### 7.1 关键证据索引

| 证据 | 路径 |
|------|------|
| Form schema 定义 | `../nop-entropy/nop-kernel/nop-xdefs/.../_vfs/nop/schema/xui/form.xdef` |
| Layout 解析器 | `../nop-entropy/nop-kernel/nop-xlang/.../layout/parse/LayoutModelParser.java` |
| AMIS 表单渲染 | `../nop-entropy/nop-frontend-support/nop-web/.../_vfs/nop/web/xlib/web.xlib` (GenFormBody L125-148, GenFormTable L238-284) |
| Codegen 表单生成 | `../nop-entropy/nop-frontend-support/nop-web/.../_vfs/nop/web/xlib/view-gen.xlib` (GenForm L32-60) |
| 查询运算符渲染 | `web.xlib:GenFormSimpleCell:373-382` |
| 后端查询校验 | `../nop-entropy/nop-service-framework/nop-biz/.../crud/ObjMetaBasedFilterValidator.java` |
| 平台参考实现 | `../nop-entropy/nop-auth/nop-auth-web/.../_vfs/nop/auth/pages/NopAuthUser/NopAuthUser.view.xml` |
| XMeta prop schema | `../nop-entropy/nop-kernel/nop-xdefs/.../_vfs/nop/schema/schema/obj-schema.xdef:78-101` |

### 7.2 术语对照

| 术语 | 含义 |
|------|------|
| `internal="true"` | xmeta prop 标记，codegen 跳过此字段不生成到表单 |
| `queryable="true"` | xmeta prop 标记，后端允许此字段作为查询条件 |
| `ui:filterOp` | xmeta prop 属性，指定查询表单使用的运算符（like/eq/in/date-between） |
| `allowFilterOp` | xmeta prop 属性，限制允许的运算符集合 |
| `filterOp` | cell 属性，同 `ui:filterOp` 但优先级更高 |
| `foldable` / `folded` | layout group 属性，可折叠 / 缺省折叠 |
| `layoutControl="tabs"` | form 属性，将分组渲染为 Tab 页签 |
| `bounded-merge` | x:override 策略，只保留显式列出的子节点 |
