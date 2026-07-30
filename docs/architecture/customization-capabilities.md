# 定制开发能力（Customization Capabilities）

> **定位**：本文是 nop-app-erp 作为**产品化通用 ERP** 的定制能力总览。说明产品如何利用 Nop 平台内置的扩展机制，快速定制适配各个领域的业务 ERP 系统，同时保持升级路径不被破坏。
>
> **平台机制权威**：所有机制的完整说明见 `../nop-entropy/docs-for-ai/`。本文只描述 nop-app-erp 如何组合使用这些能力、决策顺序与项目约定，不重复平台细节。
>
> **声明-实证对齐原则（plan 2026-07-31-0310-3 / R2.9）**：本文按「方案 A」维护——**保留全部能力章节以表达设计意图与平台可用能力**，同时为每项能力追加「实证状态注记」，诚实区分三种状态：`✅ 经项目实证落地` / `△ 平台机制可用，本项目零业务实证` / `⚠️ 平台能力，本项目未启用`。客户化首项场景落地时按 successor 补运行时实证（见各节注记与文末 §Deferred 实证）。

## 产品定位

nop-app-erp 是基于 Nop 平台架构的**产品化通用 ERP 产品**：

- **通用**：内置 18 个业务域（主数据/库存/采购/销售/财务 + 资产/项目/制造/质量/维护 + CRM/CS/HR/APS/合同/DRP/物流/B2B）的标准能力，覆盖中等规模 ERP 的进销存+财务一体化+制造全链及外围协作域。
- **产品化**：作为可发布的标准产品基线，不是一次性项目代码。基线通过 `nop-cli gen` 生成，遵循模型优先开发。
- **可定制**：充分利用 Nop 平台的扩展能力（Delta 定制、扩展字段、动态实体、模块化组装等），快速适配零售、制造、贸易、医疗、教育等各领域的具体业务，**不改基线源码**。
- **升级友好**：定制层与基线分离，基线升级时定制自动合并，不破坏客户化改动。

> **「不改基线源码」边界澄清（P1-MA3-061）**：此处的"基线源码"特指**生成物**（`_gen/` 下文件、`_` 前缀文件如 `_*.xbiz`/`_*.view.xml`/`_app.orm.xml`/`_service.beans.xml`）。**非下划线扩展层（保留层）手写是合法的——它是扩展自己的 codegen 产物，不是"改基线源码"**（见 §能力四）。两层数学边界：改 `_gen/`/`_*.xml` = 违反硬规则；手写非下划线 `Xxx.xbiz`/`Xxx.view.xml` 继承生成基线 = 合法扩展。本产品主路径 = codegen ORM 模型 + 非下划线保留层 + 模块化组装（均已实证落地），Delta/EAV/nop-dyn 等扩展能力 = 平台可用 + successor 验证（见各能力节实证注记）。

## 定制能力总览

nop-app-erp 的定制能力按"改动成本从低到高、灵活度从高到低"排列。决策时**优先选择低成本能力**：

| 能力 | 改动成本 | 灵活度 | 适用场景 | 实际启用 | 平台文档 |
|------|----------|--------|----------|----------|----------|
| 配置驱动（字典/参数/编码规则） | 极低 | 低 | 选项值、编号规则、开关 | ✅ 广泛落地（19 ORM 全覆盖 `ext:dict` + 279 字典 yaml） | `03-modules/nop-sys.md` |
| 扩展字段（EAV） | 低 | 中 | 加字段不改表结构 | ⚠️ 平台能力，本项目未启用（客户化字段经 codegen ORM 加列承载） | `03-modules/nop-sys.md`（NopSysExtField） |
| nop-dyn 动态实体 | 低 | 高 | 运行时新增实体/页面/SQL，无需 codegen | ⚠️ 平台能力，本项目未启用（采用 codegen ORM，类型安全/性能优先） | `03-modules/nop-dyn.md` |
| Delta 定制（含 task.xml） | 中 | 高 | 覆盖基线 ORM/beans/xbiz/view/task，升级兼容 | △ 平台机制可用，业务级零实证（仅 2 个平台层 nop-auth view delta） | `02-core-guides/delta-customization.md` |
| task.xml 编排 | 中 | 高 | 按步骤粒度配编排逻辑，支持 Delta 精确覆写某一步 | ⚠️ 平台能力，本项目未启用（复杂编排全部走 Processor + protected step） | `03-modules/nop-task.md` |
| 非下划线扩展层 | 中 | 中 | 在生成保留层补充业务逻辑/页面 | ✅ 广泛落地（19 模块 352 套 BizModel/xbiz/view） | `02-core-guides/delta-customization.md`、`view-and-page-customization.md` |
| 模块化组装 | 中 | 高 | 按需引入/裁剪业务域模块 | ✅ 已验证（DAG dao-only 依赖解耦 + app-erp-all 聚合） | `01-repo-map/domain-module-pattern.md` |
| BizModel/Processor 手写 | 高 | 极高 | 复杂业务逻辑，无法用模型表达 | ✅ 广泛落地（Processor/Facade 两层 + per-mutation 子类） | `02-core-guides/domain-logic-and-ddd.md` |

> **「实际启用」列说明（P1-MA3-059）**：8 能力中 4 项 ✅ 经项目实证落地 + 1 项 △ 平台机制可用但业务级零实证（Delta）+ 3 项 ⚠️ 平台能力本项目未启用（EAV/nop-dyn/task.xml）。BizLoader 计算字段作为 §能力六 独立章节声明，同样 ⚠️ 平台能力未启用（见 §能力六 实证注记）。未启用项均为**合理设计选择**（codegen ORM/Processor 模式更类型安全 + 性能 + codegen 纪律），非能力缺失；启用为 successor（首项客户化场景落地时验证）。

> **BizModel/Processor 的配置余地细化**：拓扑稳定的复杂流程（流程骨架不可配、仅单步实现需按客户/行业覆盖）按 `processor-extension-pattern.md` 实现 Facade + Processor 两层结构，通过 protected 步骤 + 派生 bean 同名覆盖留配置余地；拓扑可变的编排仍首选 task.xml（见 `service-layer-orchestration.md`）。

## 决策顺序（Model → Delta → Java）

定制时遵循平台默认决策顺序（见 `../nop-entropy/docs-for-ai/00-start-here/ai-defaults.md`）：

1. **先判断是否能改模型**（`model/*.orm.xml` / `*.xmeta`）——加字段、改字段属性、加实体，优先改源模型重新生成。
2. **不能只靠模型时，再判断是否能用 Delta**——覆盖已有基线的 ORM/beans/xbiz/view，用 `_vfs/_delta/` 差量覆盖。
3. **模型和 Delta 都不足时，才写 Java 或其他保留层代码**——BizModel 实现复杂业务逻辑。

**硬规则**：不允许手工修改任何生成物（`_gen/`、`_*.xml`、`_*.java`、`_app.orm.xml`、`_service.beans.xml`）。如需改变生成结果，只能改源模型、Delta、非下划线保留层或 codegen 模板，然后重新生成。

## 能力一：Delta 定制（基线差量覆盖）

**用途**：在不改基线源码的前提下，覆盖或扩展基线的 ORM、beans、xbiz、view 等生成产物。是适配各领域业务的核心手段。

**机制**：Delta 文件放在 `_vfs/_delta/{deltaDir}/`，路径对应原始文件，使用 `x:extends="super"` 声明差量合并。

**适用场景**（按 nop-app-erp 实际域举例）：

| 定制需求 | Delta 做法 |
|----------|------------|
| 给物料加客户专属字段（如医药批文号） | Delta ORM 扩展 `ErpMdMaterial` 实体加列 |
| 改采购订单的审核流为多级审 | Delta beans/xbiz 覆盖审批配置 |
| 销售出库页加客户专属按钮 | Delta view 覆盖 `ErpSalDelivery.view.xml` |
| 凭证模板按行业调整借贷科目 | Delta xbiz/beans 覆盖凭证生成配置 |
| 不同租户用不同物料分类树 | VFS 租户层（`_tenant/{tenantId}/...`） |

> **实证状态注记（P1-MA3-057）**：本项目当前**业务级 Delta = 0**——产品基线的定制需求已由「非下划线保留层 + 模块化组装 + 配置驱动」覆盖（均 ✅ 经项目实证落地）。Delta 合并机制经**平台层**实证可用（`app-erp-all/_vfs/_delta/default/nop/auth/pages/` 下 2 个 nop-auth view delta，证实 `x:extends="super"` 差量合并 + `_dump/` 来源标注机制工作）。**业务级 Delta 实证为 successor**：首项真实业务域 Delta 上线时验证 ORM/beans/xbiz/view 的差量合并、调试（`nop.debug=true`）与升级再合并路径。声明 Delta 为"核心手段"指其在平台机制层的地位，非本项目已实证的业务定制路径。

**项目约定**：
- Delta 目录名默认 `default`；按客户/行业可建多个 deltaDir（如 `default`、`pharma`、`retail`），通过配置激活。
- Delta 文件必须 `x:extends="super"`，路径与原文件一致。
- 升级基线时 Delta 自动合并，无需手工迁移。
- 调试是否生效：`nop.debug=true` + 查看 `_dump/{appName}/...` 的 `<!--LOC:...-->` 来源标注。

**完整机制**见 `../nop-entropy/docs-for-ai/02-core-guides/delta-customization.md`。

## 能力二：扩展字段（EAV 动态字段）

**用途**：运行时给实体加字段，不改表结构、不重新生成。适合客户化字段多、字段不固定的场景。

**机制**：基于 `nop-sys` 的 `NopSysExtField`（`nop_sys_ext_field` 表），实体-属性-值（EAV）模式存储。扩展字段在 ORM 实体上声明后，通过 `extFields.fldX.string` 等属性路径访问。

**适用场景**：
- 物料加客户专属属性（如服装行业的颜色/尺码、食品行业的保质期/配料）。
- 往来单位加行业专属属性（如医院的科室、学校的院系）。
- 凭证分录加辅助核算维度（如项目、合同、成本中心）。

> **实证状态注记（P1-MA3-058）**：本项目当前客户化字段经 **codegen ORM 物理加列**承载（如多币种四件套字段），**EAV 路径未启用**（全域 19 ORM 文件零 `extField`/`extFields`/`NopSysExtField` 声明）。这是合理设计选择——codegen ORM 类型安全 + 查询性能 + 字段固定，优于 EAV 的运行时灵活但类型/性能较弱。EAV 机制经平台 `nop-sys` 模块可用（`NopSysExtField` + `extFields.fldX.string` 路径）。**EAV 实证为 successor**：首个客户启用 EAV（字段不固定、需运行时增减）时验证 ORM/xmeta/view 集成与持久化路径。

**与 Delta ORM 加列的区别**：
- Delta 加列：物理加列，查询性能好，字段固定，需要重新生成。
- 扩展字段：EAV 存储，不改表，字段可运行时增减，查询性能略低。

**完整机制**见 `../nop-entropy/docs-for-ai/03-modules/nop-sys.md`（NopSysExtField）与 `03-runbooks/override-platform-page-with-delta.md`（页面引用扩展字段）。

## 能力三：nop-dyn 动态实体（运行时建模）

**用途**：运行时定义业务实体、属性、关系、页面、SQL，无需 codegen、无需重启。适合需求频繁变化、字段不固定、需要低代码建表的场景。

**机制**：基于 `nop-dyn` 模块，核心实体 `NopDynEntityMeta`/`NopDynPropMeta`/`NopDynPage`/`NopDynSql` 等。支持两种存储类型：`VIRTUAL`（虚拟实体不建表）/ `REAL`（真实实体建表）。页面支持 AMIS/OpenTiny/Formily 三种 schema。

**适用场景**：
- 各领域独有的业务表（如零售的促销活动、制造的车间工位、教育的课程安排），无需改基线 ORM。
- 临时数据采集表、调查问卷、自定义审批单。
- 多客户不同业务表结构，运行时配置而非硬编码。

**与 Delta/扩展字段的关系**：
- Delta：覆盖已有基线实体，需要基线存在。
- 扩展字段：给已有实体加字段，实体本身固定。
- nop-dyn：新建完整动态实体，不依赖基线，最灵活但性能与类型安全弱于生成实体。

**模块组装**：nop-dyn 支持"模块与应用组合"（`NopDynApp`/`NopDynModule`/`NopDynAppModule`），可按需把动态实体模块组装到不同应用。

> **实证状态注记（P1-MA3-059）**：本项目**未启用 nop-dyn**（19 service 模块零 `nop-dyn`/`NopDynEntity` 业务使用）。领域独有的业务表全部经 codegen ORM 新建实体（类型安全 + 性能 + codegen 纪律），而非运行时动态建模。这是合理设计选择——nop-dyn 的运行时灵活度对应类型安全与性能的弱化，与本项目"产品化标准基线 + 模型优先开发"定位一致。nop-dyn 机制经平台模块可用。**nop-dyn 实证为 successor**：出现"需求频繁变化、字段不固定、需低代码建表且不愿 codegen"的场景时验证动态实体建模与页面生成路径。

**完整机制**见 `../nop-entropy/docs-for-ai/03-modules/nop-dyn.md`。

## 能力四：非下划线扩展层（保留层定制）

**用途**：在自己的模块里扩展生成保留层，补充业务逻辑或页面，不破坏生成物。

**机制**：生成产物用 `_` 前缀（`_gen/`、`_*.xbiz`、`_*.view.xml`），非下划线文件（`Xxx.xbiz`、`Xxx.view.xml`）继承并扩展生成基线。

**适用场景**：
- BizModel：`ErpPurOrder.xbiz` 扩展 `_ErpPurOrder.xbiz`，加自定义业务方法。
- 页面：`ErpSalOrder.view.xml` 扩展 `_gen/_ErpSalOrder.view.xml`，改列表列/表单布局/按钮。
- beans：`_vfs/erp/fin/beans/app-service.beans.xml` 扩展生成 beans（VFS 路径，moduleId=`erp/fin`）。

**与 Delta 的区别**（重要）：
- **非下划线扩展层**：在自己的模块里扩展自己的生成保留层——定制自己产出的代码。
- **Delta**：在已有产品/基线模块上做差量覆盖——定制别人的代码。
- 默认判断：扩展自己的生成层用非下划线文件；定制基线产品用 Delta。

**页面定制三层架构**：

```
xmeta (实体元数据,源)
  ↓ [构建时 codegen]
_gen/_Xxx.view.xml  (自动生成 view 基线,会被覆盖)
  ↓ x:extends
Xxx.view.xml        (保留层,手写定制)
  ↓ 运行时被 web:GenPage 读取
main.page.yaml      (入口 wrapper)
  ↓ x:gen-extends 触发
AMIS JSON           (运行时输出)
```

**默认修改路径**（页面）：

| 需求 | 默认修改位置 |
|------|-------------|
| 列表列顺序/显隐/标签 | `grid/cols`（保留层 view） |
| 表单布局/只读/必填/子表 | `form/layout` 和 `form/cells` |
| 列表按钮/行按钮 | `pages/crud/listActions`、`rowActions` |
| 查询表单 | `form id="query"` |

**完整机制**见 `../nop-entropy/docs-for-ai/02-core-guides/view-and-page-customization.md`。

## 能力五：模块化组装（按需引入/裁剪业务域）

**用途**：nop-app-erp 的 18 个业务域各自独立成 Maven 工程，按需组装到具体客户的交付应用，裁剪不需要的域。

**机制**：每域独立工程（如 `app-erp-inventory`、`app-erp-finance`），由聚合工程 `app-erp-all` 选择依赖。DAG 依赖方向：master-data ← inventory ← purchase/sales ← finance；扩展域各自依赖核心域。物理目录 ↔ 逻辑工程名映射见 `domain-module-split-analysis.md §2.0`。

**适用场景**：
- 纯商贸客户：只组装 master-data/inventory/purchase/sales/finance，不引入 manufacturing/quality/maintenance。
- 制造客户：组装核心 5 域 + 第一批扩展 5 域。
- 完整产品：组装全部 18 域。
- 轻量客户：只组装 master-data/inventory/finance，不引入采购销售。

**模块依赖方向**（不可反向，见 `module-boundaries.md`）：

```
master-data ← inventory ← purchase/sales ← finance
                                       ↑
assets/projects/manufacturing/quality/maintenance（扩展域）
```

**垂直行业扩展**：特定行业的差异化能力可建独立扩展工程（如 `app-erp-pharma-ext`），依赖相关核心域，通过 Maven 依赖 + Delta 组装到交付应用。

**完整机制**见 `../nop-entropy/docs-for-ai/01-repo-map/domain-module-pattern.md`（可选模块 `-ext`/`-delta` 等）与本项目 `domain-module-split-analysis.md`。

## 能力六：BizLoader 与 GraphQL 扩展

**用途**：为已有实体动态增加计算字段或派生属性，不改实体结构。常用于 DTO 字段、前端展示用的计算列。

**机制**：在 BizModel 用 `@BizLoader` 注解方法，框架自动把返回值作为实体的虚拟属性暴露给 GraphQL/API。

**适用场景**：
- 采购订单的"未交量"（订单数量 − 累计入库数量）——计算字段，不入库。
- 物料的"当前库存"——关联查询字段。
- 凭证的"借贷平衡校验结果"——派生校验字段。

> **实证状态注记（P1-MA3-060）**：本项目**未使用 `@BizLoader`**（19 service 模块零 `@BizLoader` 业务使用）。上述 3 业务示例的实际实现路径分别为：
> - **未交量** → 经 SQL 查询或 Processor 计算（未以 @BizLoader 落地）
> - **当前库存** → 经 `ErpInvStockBalance` 实体查询（库存余额实体直接持久化，非派生字段）
> - **借贷平衡校验结果** → 经 `ErpFinVoucher.balanceStatus` 持久化字段 + `ErpFinVoucher.view.xml` 的 `<col id="balanceStatus" custom="true"><gen-control>` 内联 AMIS 脚本计算
>
> @BizLoader 能力经平台可用，本项目选择持久化字段 / Processor / view.xml gen-control 是合理设计选择（派生值经实体字段或前端脚本承载更直观且可查询）。**@BizLoader 实证为 successor**：出现"不入库的纯派生展示字段，且不适合 view.xml gen-control（如需服务端聚合多表）"的场景时验证 @BizLoader 落地路径。

**决策提示**：
- 普通扩展字段优先用 `@BizLoader` 或 `view.xml gen-control`，本项目当前路径选择 gen-control / 持久化字段（见实证注记），而非先改 DTO 或 Delta（见 `03-runbooks/add-bizloader-field.md`）。
- 但如果要加的是持久化字段（入库存储），先改 ORM 模型或用扩展字段，而不是 BizLoader。

**完整机制**见 `../nop-entropy/docs-for-ai/03-runbooks/add-bizloader-field.md`、`extend-api-with-delta-bizloader.md`。

## 定制场景决策矩阵

按"定制需求 × 推荐能力"给出快速决策：

| 定制需求 | 首选能力 | 备选 |
|----------|----------|------|
| 加持久化字段（所有客户都要） | 改 ORM 源模型 + 重新生成 | — |
| 加客户专属字段（个别客户） | 扩展字段 EAV | Delta ORM 加列 |
| 改字段属性（标签/必填/显隐） | 改 xmeta 或保留层 view | Delta view |
| 改业务逻辑（多步骤编排/审核流） | Delta task.xml覆写步骤 | Delta xbiz/beans 或 保留层 BizModel |
| 新增业务表（领域独有） | nop-dyn 动态实体 | 新建扩展工程 + ORM |
| 加计算/派生字段（不入库） | BizLoader | — |
| 改页面布局/按钮 | 保留层 view | Delta view |
| 裁剪/引入业务域 | 模块化组装（Maven 依赖） | — |
| 行业差异化能力 | 垂直扩展工程 + Delta | — |
| 多租户不同配置 | VFS 租户层 + Delta | — |

## 升级路径保护

nop-app-erp 的定制层设计确保基线升级时不破坏客户化：

1. **Delta 自动合并**：基线升级后，Delta 按 `x:extends="super"` 重新合并，定制保留。**实证状态：△ 平台机制可用 / 业务级 successor**——合并机制经 2 个平台层 nop-auth view delta 实证可工作；业务级 Delta = 0，首项业务域 Delta 上线时验证升级再合并。
2. **扩展字段独立存储**：EAV 数据在独立表，基线表结构变化不影响扩展数据。**实证状态：△ 平台机制可用 / 项目零实证**——EAV 路径未启用（客户化字段经 codegen ORM 加列），独立存储机制经平台 `nop-sys` 可用。
3. **nop-dyn 运行时配置**：动态实体配置在数据库，基线代码升级不触碰动态配置。**实证状态：△ 平台机制可用 / 项目零实证**——nop-dyn 未启用，运行时配置独立于代码升级的机制经平台可用。
4. **模块化组装**：客户组装的模块集合独立于基线模块演进。**实证状态：✅ 经项目实证落地**——DAG dao-only 依赖解耦已验证（finance-service compile-scope 仅依赖他域 `*-dao`，裁剪 manufacturing 后 finance 可构建，见 `data-dependency-matrix.md`）。
5. **保留层不冲突**：非下划线扩展文件继承生成基线，基线升级时生成层刷新、保留层定制保留。**实证状态：✅ 经项目实证落地**——19 模块 352 套保留层（BizModel/xbiz/view）继承机制全部正确（抽样 `ErpFinVoucher.xbiz` `x:extends="_ErpFinVoucher.xbiz"` + `ErpFinVoucher.view.xml` `x:extends="_gen/_ErpFinVoucher.view.xml"` + `<cols x:override="bounded-merge">`）；codegen 增量再生（`mvn clean install -DskipTests`）刷新生成层、保留非下划线文件。

> **升级路径保护综合实证（P1-MA3-061）**：5 项机制中 **2 项 ✅ 经项目实证落地**（模块化组装 + 保留层不冲突）+ **3 项 △ 平台机制可用但项目零实证**（Delta 自动合并 + EAV 独立存储 + nop-dyn 运行时配置）。零实证的 3 项对应本项目未启用的扩展能力（见各能力节），无运行时风险（未启用即不影响）；启用时按 successor 补实证。

**禁止破坏升级路径的做法**：
- 直接改基线源码（应改 Delta 或源模型）。
- 直接改生成物 `_gen/`/`_*.xml`（应改源模型或 Delta）。
- 在基线工程内硬编码客户逻辑（应建独立扩展工程）。

## 与其他文档的关系

| 文档 | 关系 |
|------|------|
| `project-vision.md` | 产品定位（本文的"产品定位"节是其展开） |
| `system-baseline.md` | 技术基线（模块结构、技术栈） |
| `module-boundaries.md` | 模块依赖方向（模块化组装的约束） |
| `domain-module-split-analysis.md` | 18 域拆分决策与命名映射（模块化组装的基础） |
| `../nop-entropy/docs-for-ai/02-core-guides/delta-customization.md` | Delta 机制权威说明 |
| `../nop-entropy/docs-for-ai/02-core-guides/view-and-page-customization.md` | 页面定制权威说明 |
| `../nop-entropy/docs-for-ai/03-modules/nop-dyn.md` | 动态实体权威说明 |
| `../nop-entropy/docs-for-ai/03-modules/nop-sys.md` | 扩展字段/字典/序列号权威说明 |
| `../nop-entropy/docs-for-ai/00-start-here/ai-defaults.md` | Model→Delta→Java 决策顺序 |
