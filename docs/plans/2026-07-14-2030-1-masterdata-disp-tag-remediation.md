# Plan: master-data 同模块 FK 名称解析整改（tagSet="disp" 替换机制 D）

> Plan Status: superseded
> Superseded By: `2026-07-14-2256-2-fk-display-name-resolution-conformance.md`（本文件为非模板探索草案，master-data 子集范围已并入该正式计划的 Phase 1 pilot）

## 问题

master-data 域的 37 处 FK 名称解析全部采用**机制 D**（手写 xmeta 派生 prop + `@BizLoader` Java 方法 + view.xml bounded-merge），但这些 FK 引用全部是**同模块内**引用（目标实体在同一 `app-erp-master-data.orm.xml` 中定义）。

Nop 平台代码生成器对同模块 `<to-one tagSet="pub">` 引用，在被引用实体的显示列标注 `tagSet="disp"` 后，会自动生成 `{relation}.{displayProp}` 路径属性（`_{shortName}.xmeta.xgen` 第 117 行）。这意味着 37 个 `@BizLoader` 方法 + 37 个 xmeta 派生 prop 声明可被消除。

## 范围

| 项目 | 值 |
|------|-----|
| 影响模块 | `module-master-data`（`erp-md-model` / `erp-md-meta` / `erp-md-service` / `erp-md-web`） |
| 涉及文件 | 1 个 ORM XML + 19 个 BizModel Java + 19 个 xmeta + 19+ 个 view.xml + 代码生成产物 |
| 不涉及 | 其它 17 个域的跨模块引用（机制 D 保持不变） |
| 增量 | 约 -400 行 Java + -50 行 xmeta，ORM 模型增 10+ 行 `tagSet="disp"` |

## 方案

1. ORM 模型：在 all 被引用的 master-data 实体的 `name`（或 `code`）列上加 `tagSet="disp"`
2. 重新代码生成：`mvn clean install -DskipTests` → 代码生成器自动产生 `{relation}.{dispCol}` 路径属性
3. xmeta：手写层删除所有派生 `*Name` prop 声明
4. Java：删除所有 `@BizLoader` FK 名称解析方法（`*BizModel.java`）
5. view.xml：将 `categoryName` → `category.name`（使用自动路径属性）
6. 测试：更新快照和 E2E 测试断言

## 执行步骤

### Step 1：ORM 模型加 `tagSet="disp"`

在 `app-erp-master-data.orm.xml` 中，对以下实体的 `name` 列加 `tagSet="disp"`：

| 实体 | 显示列 | 被谁引用（同模块内） |
|------|--------|---------------------|
| `ErpMdMaterialCategory` | `name` | `ErpMdMaterial.category`, `ErpMdMaterialCategory.parent`, `ErpMdSupplierApproval.materialCategory` |
| `ErpMdUoM` | `name` | `ErpMdMaterial.uoM`, `ErpMdMaterialSku.uoM`, `ErpMdUoMConversion.fromUoM/toUoM` |
| `ErpMdWarehouse` | `name` | `ErpMdMaterial.defaultWarehouse`, `ErpMdLocation.warehouse` |
| `ErpMdTaxRate` | `name` | `ErpMdMaterial.defaultTaxRate`, `ErpMdMaterialSku.taxRate` |
| `ErpMdMaterial` | `name` | `ErpMdMaterialSku.material`, `ErpMdUoMConversion.material`, `ErpMdSupplierApproval.materialCategory` (间接) |
| `ErpMdOrganization` | `name` | `ErpMdEmployee.org`, `ErpMdWarehouse.org`, `ErpMdAcctSchema.org`, `ErpMdCostCenter.org`, `ErpSysConfig.org` |
| `ErpMdPartner` | `name` | `ErpMdEmployee.partner`, `ErpMdBankAccount.partner`, `ErpMdPartnerAddress.partner`, `ErpMdPartnerContact.partner`, `ErpMdSupplierApproval.partner` |
| `ErpMdCurrency` | `name` | `ErpMdOrganization.functionalCurrency`, `ErpMdAcctSchema.functionalCurrency`, `ErpMdExchangeRate.fromCurrency/toCurrency` |
| `ErpMdEmployee` | `name` | `ErpMdWarehouse.manager`, `ErpMdCostCenter.manager` |
| `ErpMdSubject` | `name` | `ErpMdSubjectMapping.sourceSubject/targetSubject` |
| `ErpMdCostCenter` | `name` | 被其他 master-data 实体引用（如 CostCenter 自引用 parent） |
| `ErpMdAcctSchema` | `code` | `ErpMdSubjectMapping.targetAcctSchema`, `ErpMdAcctSchemaCoa.acctSchema` (用 code 而非 name) |

对每个 entity 的 `<column name="name"...>` 加 ` tagSet="disp"`（或 ` tagSet="disp,other-tags"`，如涉及多个标签用逗号拼接）。

特殊：`ErpMdAcctSchema` 用 `code` 作为显示列（`acctSchemaCode` loader 读 `getCode()`），所以对 `code` 列加 `tagSet="disp"`。

### Step 2：重新代码生成

```bash
mvn clean install -DskipTests -pl module-master-data -am
```

验证 `_ErpMdMaterial.xmeta` 中是否出现：
```xml
<prop name="category.name" .../>
<prop name="uoM.name" .../>
<prop name="defaultWarehouse.name" .../>
<prop name="defaultTaxRate.name" .../>
```

### Step 3：清理 xmeta 派生 prop

对所有 19 个手写 `*BizModel` 对应的 xmeta 文件，删除 `<prop name="xxxName"...>` 声明。

受影响文件清单（`module-master-data/erp-md-meta/...`）：
- `ErpMdMaterial/ErpMdMaterial.xmeta`（4 个：categoryName, uomName, defaultWarehouseName, defaultTaxRateName）
- `ErpMdMaterialSku/ErpMdMaterialSku.xmeta`（3 个）
- `ErpMdMaterialCategory/ErpMdMaterialCategory.xmeta`（1 个：parentName）
- `ErpMdOrganization/ErpMdOrganization.xmeta`（2 个）
- `ErpMdEmployee/ErpMdEmployee.xmeta`（2 个）
- `ErpMdLocation/ErpMdLocation.xmeta`（2 个）
- `ErpMdWarehouse/ErpMdWarehouse.xmeta`（2 个）
- `ErpMdSubject/ErpMdSubject.xmeta`（2 个）
- `ErpMdAcctSchema/ErpMdAcctSchema.xmeta`（2 个）
- `ErpMdSubjectMapping/ErpMdSubjectMapping.xmeta`（3 个）
- `ErpMdAcctSchemaCoa/ErpMdAcctSchemaCoa.xmeta`（1 个）
- `ErpMdCostCenter/ErpMdCostCenter.xmeta`（3 个）
- `ErpMdExchangeRate/ErpMdExchangeRate.xmeta`（2 个）
- `ErpMdBankAccount/ErpMdBankAccount.xmeta`（1 个）
- `ErpMdPartnerAddress/ErpMdPartnerAddress.xmeta`（1 个）
- `ErpMdPartnerContact/ErpMdPartnerContact.xmeta`（1 个）
- `ErpMdSupplierApproval/ErpMdSupplierApproval.xmeta`（3 个）
- `ErpMdUoMConversion/ErpMdUoMConversion.xmeta`（3 个）
- `ErpSysConfig/ErpSysConfig.xmeta`（1 个）

### Step 4：清理 Java `@BizLoader`

对上述 19 个 BizModel 文件，删除所有 FK 名称解析的 `@BizLoader` 方法和对应 import。

### Step 5：更新 view.xml

将每个 view.xml 的 list grid 中的 `<col id="xxxName">` 改为 `<col id="xxx.name">`。

对应关系示例：`categoryName` → `category.name`，`uomName` → `uoM.name`（注意关系名！如 `uoM` 而非 `uom`）。

### Step 6：运行测试

```bash
# 全量构建
mvn clean install -DskipTests

# 运行 master-data 相关测试
mvn test -pl module-master-data/erp-md-service -am

# E2E 快照测试（如存在）
mvn test -pl tests/e2e -am -Dtest=*ErpMd*FkName*
```

## 退出标准

- [ ] 全量构建通过（`mvn clean install -DskipTests` 全绿）
- [ ] master-data 模块测试通过
- [ ] 无 `@BizLoader` FK 名称解析 Java 代码残留
- [ ] 无手写 `*Name` xmeta 派生 prop 残留
- [ ] view.xml 中 `xxxName` 已全部替换为 `xxx.name`（`relation.name` 格式）
- [ ] `docs-for-ai/02-core-guides/orm-model-design.md` 的"显示名解析"节已补充（已完成）
- [ ] `docs-for-ai/02-core-guides/cross-module-entity-reference.md` 的"显示名解析策略对比"节已补充（已完成）

## 风险

| 风险 | 概率 | 应对 |
|------|------|------|
| `category.name` 点号属性在 AMIS 列中不工作 | 低 | 降级为 xmeta `mapToProp` 方案：保留 `categoryName` 映射到 `category.name` |
| 自动路径属性 `queryable="true"` 导致列表查询多 JOIN | 低 | view.xml 列加 `filterable="false" sortable="false"` 覆盖代码生成默认值 |
| `uoM.name` 关系名与属性名之间驼峰/大小写不匹配 | 低 | Step 5 时仔细核对 ORM 中 `<to-one name="...">` 的 relation name |
