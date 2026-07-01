# 定制开发能力（Customization Capabilities）

> **定位**：本文是 nop-app-erp 作为**产品化通用 ERP** 的定制能力总览。说明产品如何利用 Nop 平台内置的扩展机制，快速定制适配各个领域的业务 ERP 系统，同时保持升级路径不被破坏。
>
> **平台机制权威**：所有机制的完整说明见 `../nop-entropy/docs-for-ai/`。本文只描述 nop-app-erp 如何组合使用这些能力、决策顺序与项目约定，不重复平台细节。

## 产品定位

nop-app-erp 是基于 Nop 平台架构的**产品化通用 ERP 产品**：

- **通用**：内置 18 个业务域（主数据/库存/采购/销售/财务 + 资产/项目/制造/质量/维护 + CRM/CS/HR/APS/合同/DRP/物流/B2B）的标准能力，覆盖中等规模 ERP 的进销存+财务一体化+制造全链及外围协作域。
- **产品化**：作为可发布的标准产品基线，不是一次性项目代码。基线通过 `nop-cli gen` 生成，遵循模型优先开发。
- **可定制**：充分利用 Nop 平台的扩展能力（Delta 定制、扩展字段、动态实体、模块化组装等），快速适配零售、制造、贸易、医疗、教育等各领域的具体业务，**不改基线源码**。
- **升级友好**：定制层与基线分离，基线升级时定制自动合并，不破坏客户化改动。

## 定制能力总览

nop-app-erp 的定制能力按"改动成本从低到高、灵活度从高到低"排列。决策时**优先选择低成本能力**：

| 能力 | 改动成本 | 灵活度 | 适用场景 | 平台文档 |
|------|----------|--------|----------|----------|
| 配置驱动（字典/参数/编码规则） | 极低 | 低 | 选项值、编号规则、开关 | `03-modules/nop-sys.md` |
| 扩展字段（EAV） | 低 | 中 | 加字段不改表结构 | `03-modules/nop-sys.md`（NopSysExtField） |
| nop-dyn 动态实体 | 低 | 高 | 运行时新增实体/页面/SQL，无需 codegen | `03-modules/nop-dyn.md` |
| Delta 定制（含 task.xml） | 中 | 高 | 覆盖基线 ORM/beans/xbiz/view/task，升级兼容 | `02-core-guides/delta-customization.md` |
| task.xml 编排 | 中 | 高 | 按步骤粒度配编排逻辑，支持 Delta 精确覆写某一步 | `03-modules/nop-task.md` |
| 非下划线扩展层 | 中 | 中 | 在生成保留层补充业务逻辑/页面 | `02-core-guides/delta-customization.md`、`view-and-page-customization.md` |
| 模块化组装 | 中 | 高 | 按需引入/裁剪业务域模块 | `01-repo-map/domain-module-pattern.md` |
| BizModel/Processor 手写 | 高 | 极高 | 复杂业务逻辑，无法用模型表达 | `02-core-guides/domain-logic-and-ddd.md` |

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

**决策提示**：
- 普通扩展字段优先用 `@BizLoader`，而不是先改 DTO 或 Delta（见 `03-runbooks/add-bizloader-field.md`）。
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

1. **Delta 自动合并**：基线升级后，Delta 按 `x:extends="super"` 重新合并，定制保留。
2. **扩展字段独立存储**：EAV 数据在独立表，基线表结构变化不影响扩展数据。
3. **nop-dyn 运行时配置**：动态实体配置在数据库，基线代码升级不触碰动态配置。
4. **模块化组装**：客户组装的模块集合独立于基线模块演进。
5. **保留层不冲突**：非下划线扩展文件继承生成基线，基线升级时生成层刷新、保留层定制保留。

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
