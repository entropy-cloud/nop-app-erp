# Plan: 全域 FK 名称解析整改（tagSet="disp" 替换机制 D）

> Plan Status: superseded
> Superseded By: `2026-07-14-2256-2-fk-display-name-resolution-conformance.md`（本文件为非模板探索草案，范围由该正式计划统一承接并按模板重写）

## 问题

整个项目 266+ 实体的 FK 名称解析全部采用**机制 D**（手写 xmeta 派生 `*Name` prop + `@BizLoader` Java 方法 + view.xml 列替换为 `*Name`），但这些 FK 引用全部已有 `<to-one>` 关系声明（无论是同模块还是 `notGenCode="true"` 跨模块桩）。

Nop 平台已有完整自动管线：

```
ORM tagSet="disp" → 代码生成 ext:relation + ext:joinRightDisplayProp
    → control.xlib:view-relation 自动映射 AMIS 列名
    → XuiViewAnalyzer 自动加入 GraphQL selection
    → AMIS 显示名称而非原始 ID
```

这意味着所有手写代码是冗余的。详情见 docs-for-ai 更新：
- `orm-model-design.md` §显示名解析（补充了 control.xlib 自动管线说明）
- `cross-module-entity-reference.md` §7 显示名解析策略对比（纠正了跨模块限制论述）

## 范围

| 项目 | 值 |
|------|-----|
| 影响模块 | **全部 18 业务模块 + notify**（每个模块的 `erp-*-dao` ORM 桩 + `erp-*-meta` xmeta + `erp-*-service` BizModel + `erp-*-web` view.xml） |
| 涉及文件 | ~18 个 ORM XML 的桩列 + ~37 个 BizModel Java（master-data）+ 其他模块的 `@BizLoader` + 所有 `*Name` xmeta 派生 prop + 所有 view.xml 列替换 |
| 增量 | 约 -2000+ 行 Java + -500 行 xmeta + 恢复 view.xml 原始 FK 列名 |

## 原理

### 管线全链路

```
ORM 层:
  master-data orm.xml:  ErpMdPartner.name 加 tagSet="disp"
  purchase orm.xml:     notGenCode 桩 ErpMdPartner.name 加 tagSet="disp"
  （每个引用 master-data 的模块，在其桩的 name/code 列加 tagSet="disp"）
        ↓
代码生成 (_{shortName}.xmeta.xgen:106,117):
  为每个 <to-one tagSet="pub"> 生成：
    <prop name="partner" ext:joinRightDisplayProp="name" .../>
    <prop name="partner.name" internal="true" lazy="true" .../>
  为每个 FK 列生成：
    <prop name="partnerId" ext:relation="partner" .../>
        ↓
AMIS 渲染 (control.xlib:view-relation:976-997):
  读到 partnerId 的 ext:relation="partner"
  → 找到 partner relation prop 的 ext:joinRightDisplayProp="name"
  → 把 AMIS 列 name 从 "partnerId" 改为 "partner.name"
        ↓
GraphQL selection (XuiViewAnalyzer.addRelationDispProp:381-395):
  自动将 partner.name 加入查询字段
        ↓
后端返回: {"partner.name": "五金件"}
```

### 关键发现

**跨模块也完全支持**。代码生成器见到的被引用实体是**本模块的桩定义**。只要桩的 `name` 列也有 `tagSet="disp"`，`getDisplayPropModel()` 就能找到显示列，`ext:joinRightDisplayProp` 就能生成。`control.xlib:view-relation` 不关心目标实体在哪个模块。

**view.xml 不需要写 `category.name`**。AMIS 列保持原始 FK 列名 `categoryId`。`control.xlib:view-relation` 在页面渲染时自动将列 `name` 从 `categoryId` 改为 `category.name`。

## 执行步骤

### Step 1：ORM 模型加 `tagSet="disp"`

#### 1a：master-data 源实体

在 `module-master-data/model/app-erp-master-data.orm.xml` 中，对以下实体的显示列加 `tagSet="disp"`：

| 实体 | 显示列 | 说明 |
|------|--------|------|
| `ErpMdMaterialCategory` | `name` | 被 category, parent, materialCategory 引用 |
| `ErpMdUoM` | `name` | 被 uoM, fromUoM, toUoM 引用 |
| `ErpMdWarehouse` | `name` | 被 defaultWarehouse, warehouse 引用 |
| `ErpMdTaxRate` | `name` | 被 defaultTaxRate, taxRate 引用 |
| `ErpMdMaterial` | `name` | 被 material 引用 |
| `ErpMdOrganization` | `name` | 被 org 引用 |
| `ErpMdPartner` | `name` | 被 partner 引用 |
| `ErpMdCurrency` | `name` | 被 fromCurrency, toCurrency, functionalCurrency 引用 |
| `ErpMdEmployee` | `name` | 被 manager 引用 |
| `ErpMdSubject` | `name` | 被 sourceSubject, targetSubject 引用 |
| `ErpMdCostCenter` | `name` | 被 parent 引用 |
| `ErpMdAcctSchema` | `code` | 被 targetAcctSchema, acctSchema 引用（用 code 而非 name） |

例如：`<column name="name" code="NAME" ... tagSet="disp"/>`

#### 1b：所有跨模块 `notGenCode="true"` 桩

对以下每个域的 ORM 文件，找到所有 `notGenCode="true"` 外部实体桩，在它们的 `name`（或 `code`）列上加 `tagSet="disp"`：

| 模块 | ORM 文件 | 需加 disp 的桩实体 |
|------|---------|-------------------|
| `module-purchase` | `app-erp-purchase.orm.xml` | ErpMdPartner, ErpMdMaterial, ErpMdWarehouse, ErpMdUoM, ErpMdCurrency, ErpMdOrganization, ErpMdTaxRate, ErpMdBankAccount, ErpMdSettlementMethod, ErpMdEmployee, ErpPrjProject |
| `module-sales` | `app-erp-sales.orm.xml` | 同上模式的 master-data 桩 |
| `module-finance` | `app-erp-finance.orm.xml` | 同上 + ErpMdSubject, ErpMdAcctSchema |
| `module-inventory` | `app-erp-inventory.orm.xml` | 同上 |
| `module-manufacturing` | `app-erp-manufacturing.orm.xml` | 同上 |
| `module-assets` | `app-erp-assets.orm.xml` | 同上 |
| `module-projects` | `app-erp-projects.orm.xml` | 同上（含 ErpPrjProject 自引用） |
| `module-quality` | `app-erp-quality.orm.xml` | 同上 |
| `module-maintenance` | `app-erp-maintenance.orm.xml` | 同上 |
| `module-crm` | `app-erp-crm.orm.xml` | 同上 |
| `module-cs` | `app-erp-cs.orm.xml` | 同上 |
| `module-hr` | `app-erp-hr.orm.xml` | 同上 + ErpMdEmployee 等 |
| `module-aps` | `app-erp-aps.orm.xml` | 同上 |
| `module-logistics` | `app-erp-logistics.orm.xml` | 同上 |
| `module-b2b` | `app-erp-b2b.orm.xml` | 同上 |
| `module-contract` | `app-erp-contract.orm.xml` | 同上 |
| `module-drp` | `app-erp-drp.orm.xml` | 同上 |
| `module-notify` | `app-erp-notify.orm.xml` | 同上（含 ErpMdEmployee 等） |

### Step 2：全域重新代码生成

```bash
mvn clean install -DskipTests
```

验证生成的 `_*.xmeta` 中是否：
1. FK 列 prop 有 `ext:relation="xxx"`（已有）
2. 关系 prop 有 `ext:joinRightDisplayProp="name"`（新增，因 tagSet="disp"）
3. 出现 `<prop name="xxx.name" internal="true" lazy="true">` 路径属性

### Step 3：清理 xmeta 手写派生 prop

删除所有手写 xmeta 中的 `<prop name="xxxName"...>` 声明。覆盖所有 18+1 模块的 `erp-*-meta`。

### Step 4：清理 Java `@BizLoader`

删除所有模块中 FK 名称解析的 `@BizLoader` 方法。包括：
- `module-master-data` 的 37 个 loader（19 个 BizModel）
- 其他 17 个模块的 ~130+ 个 loader

每个 loader 的识别特征：方法名为 `{relation}Name`/`{relation}Code`，体为 `batchLoadProps` + `getXxx().getName()`。

### Step 5：恢复 view.xml 列名

将每个 view.xml list grid 中的 `<col id="xxxName" label="..."/>` 恢复为 `<col id="xxxId" label="..."/>`（原始 FK 列名）。

例如：`categoryName` → `categoryId`，`partnerName` → `partnerId`。

**注意**：`uoMName` 对应关系名为 `uoM`（驼峰），所以恢复为 `uoMId`。

### Step 6：运行测试

```bash
# 全域构建
mvn clean install -DskipTests

# 全域测试
mvn test -pl module-master-data/erp-md-service
mvn test -pl module-purchase/erp-pur-service
# ... 依此类推每个域

# E2E 快照测试
mvn test -pl tests/e2e -am
```

## 退出标准

- [ ] 全量构建通过（`mvn clean install -DskipTests` 全绿）
- [ ] 无 `@BizLoader` FK 名称解析 Java 代码残留
- [ ] 无手写 `*Name` xmeta 派生 prop 残留
- [ ] view.xml 中所有 `xxxName` 已恢复为 `xxxId`（原始 FK 列名）
- [ ] E2E 快照测试通过
- [ ] docs-for-ai 对应文档已补充（已完成）

## 风险

| 风险 | 概率 | 应对 |
|------|------|------|
| `view-relation` 对某些 relation 名（如 `uoM`）映射不匹配 | 低 | 验证每个 view.xml 的列名恢复：确认 `uoMId` 存在，`view-relation` 通过 `ext:relation="uoM"` 能找到 relation prop |
| 某个实体用于显示的不是 `name` 而是 `code` | 低 | Step 1 已处理 ErpMdAcctSchema(code) 等特殊情况 |
| AMIS 列表列排序/过滤自动启用（auto path prop queryable=true） | 低 | 如需关闭，view.xml 加 `filterable="false" sortable="false"` |
| 快照测试因返回字段变化需重录 | 中 | Step 6 按 `testing.md` 快照重录流程处理 |
