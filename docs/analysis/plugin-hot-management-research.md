# 插件热管理可行性研究（Plugin Hot Management Feasibility Research）

> **本文定位**：deepening-roadmap §Milestone D / D4（P3 可行性研究）的交付物。对比 3 种插件热管理路径在 Nop Platform 上的可行性、侵入面与代价，给出推荐路径裁决，供业务客户/架构决策参考。
>
> **范围声明**：本文是**纯研究/分析文档**，不含生产代码变更、不改 ORM 模型、不改 Maven 构建结构、不改 `app-erp-all` 打包方式（见 Non-Goals）。
>
> **Source**：`docs/backlog/deepening-roadmap.md` §5 Milestone D / D4；`docs/analysis/erp-survey/2026-07-20-post-survey-strategic-gaps.md`（OFBiz/ERP5/NocoBase 对标）。
>
> **Related**：
> - `docs/architecture/business-module-metadata.md`（D2 owner doc — `module-meta.json` + `ModuleMetaReader`，本研究路径 3 的元数据信息输入）
> - `docs/architecture/external-api-integration-pattern.md`（D1 owner doc — 3 集成案例作为插件边界评估参考）
> - `docs/architecture/domain-module-split-analysis.md`（模块拆分基线 + DAG）
> - `docs/plans/2026-07-22-0444-1-deepening-d4-plugin-hot-management-research.md`（执行计划）
>
> **Plan**：`docs/plans/2026-07-22-0444-1-deepening-d4-plugin-hot-management-research.md`

---

## §1. 目的与范围

### 1.1 要回答的问题

`nop-app-erp` 当前 19 个业务域（18 业务域 + 1 跨域通知派发子系统）均采用 Maven 多模块编译期依赖结构，由 `app-erp-all` 聚合启动包统一加载。新增/移除一个域需要 `nop-cli gen` + `mvn install` 重新构建整个 workspace，无法在运行时启用/禁用域。

业务客户与架构评审提出的需求：**是否可能让 ERP 按"插件"方式管理业务域——按客户/按部署场景选择性装载域、甚至在运行时启用/禁用域**（例如：轻量部署只装主数据+库存+采购+财务，重型部署装全部 19 域；或某客户不需要制造/质量域时可不装载）。

本研究评估 3 条候选路径在该需求空间下的可行性、对 Nop Platform 核心/Quarkus 启动模型/GraphQL schema 装配/代码生成管线的侵入面，并给出推荐。

### 1.2 研究范围边界裁决（Decision R0 — 可比性前提）

**裁决**：「插件热管理」在本项目语境下不是一个单一能力，而是**一个能力谱系**。为让 3 条候选路径可比，本研究显式将该谱系拆为 3 个正交维度，每条路径只覆盖其中一段：

| 维度 | 能力语义 | 生效时机 | 是否卸载类 |
|------|---------|---------|-----------|
| **D-Load** | 动态类加载/卸载（OSGi bundle 级 install/uninstall，含类卸载 + 资源释放） | 运行时（已启动后） | ✅ 真卸载 |
| **D-Select** | 启动期可选装载（构建/部署拓扑裁剪，决定哪些域进入启动包） | 启动期（构建/部署时决定，启动时定型） | — 不适用（未装载即不存在） |
| **D-Switch** | 应用层路由级开关（类已加载，菜单/权限/GraphQL action 级启用/禁用） | 运行时（已启动后） | ❌ 不卸载（仅隐藏入口） |

**3 条候选路径与维度的对应**：

| 路径 | 主要覆盖维度 | 次要覆盖 |
|------|-------------|---------|
| 路径 1 OSGi-style | **D-Load** | （理论可达 D-Switch，但代价远超路径 3） |
| 路径 2 Maven module isolation | **D-Select** | （零运行时能力） |
| 路径 3 NocoBase-style plugin manager | **D-Switch** | （经重启可模拟 D-Select 的部分语义） |

**裁决理由**：若不显式声明此边界，3 条路径会因为"不是一个东西"而不可比——OSGi 论"能否卸载类"，Maven isolation 论"能否裁剪启动包"，NocoBase-style 论"能否隐藏菜单"。本研究按维度分别评估，再在 §6 推荐裁决中按"业务客户真实需求落在哪个维度"做选择。

**残留风险**：业务客户可能同时想要 D-Load + D-Switch（既要运行时卸载、又要细粒度开关）。该组合需求的代价见 §6.4（需路径 1 + 路径 3 叠加，归 successor）。

### 1.3 Non-Goals

- 不实现任何插件系统代码（纯研究/分析）。
- 不修改 Nop Platform 核心（`nop-entropy`）。
- 不修改 Maven 构建结构或 `app-erp-all` 打包方式。
- 不修改任何 ORM 模型、api.xml 或 owner doc 的业务语义。
- 不推广到全 19 域的插件化改造（超出可行性研究范围）。
- 不做版本范围求解器（SemVer range resolution，D2 Deferred successor）。
- 不做 SaaS 多租户版本管理编排（D2 Deferred successor）。

### 1.4 当前现状基线（输入）

来自 D1/D2 落地后的仓库实测：

- **模块结构**：19 域 Maven 多模块（`module-<domain>/erp-<short>-{dao,meta,service,web,app,api}`），编译期依赖，DAG 无环（见 `domain-module-split-analysis.md` §4.1）。
- **启动模型**：`app-erp-all` 聚合启动包依赖全部 19 域 `-web`（传递 `-service`/`-dao`/`-meta`），classpath 含全部域产物。
- **IoC 装配**：Nop 应用侧 bean 发现是**文件驱动、启动期一次性**（非 Java classpath scanning，非运行时动态注册）。模块经 `/{moduleId}/_module` 零字节标记文件被发现，bean 经 `/{moduleId}/beans/app.beans.xml` 与 `app-*.beans.xml` 被装配（见 `nop-entropy/docs-for-ai/02-core-guides/ioc-and-config.md` §模块与 bean 发现规则）。
- **GraphQL schema 装配**：运行时由 `IGraphQLEngine` 按 operationName `{bizObj}__{method}` 路由到 BizModel 方法；BizModel 的 `@BizQuery`/`@BizMutation` 在启动期随 bean 装配被注册为 GraphQL action。Schema 在启动期定型，运行时无动态增删 action 的标准入口（见 `api-and-graphql.md` §统一请求分发模型）。
- **代码生成管线**：构建期（`mvn install`）经 `.xgen` 脚本（`{app}-codegen/postcompile/gen-orm.xgen`、`{app}-meta/precompile/gen-meta.xgen` 等）增量生成 `_gen/`、`_app.orm.xml`、`_service.beans.xml`、`_module-meta.json` 等产物。`_` 前缀文件均为生成物，手改会在下次 `mvn install` 被覆盖（见 `model-first-development.md` §模块级生成链 + `architecture-principles.md`）。
- **D2 元数据能力**：`ModuleMetaReader`（`app-erp-all`）已可运行时扫描全部 `_module-meta.json`，提供 `listModules`/`getModule`/`checkDependencyIntegrity`/`listOptionalFeatures` 诊断 API；`optionalFeatures` 字段已声明各域 config-gated 特性清单（见 `business-module-metadata.md` §4 + §8.2）。
- **对标参考**（来自 `post-survey-strategic-gaps.md`）：OFBiz `component-load.xml` 启动期组件装载；ERP5 BT5 BusinessTemplate 导出/导入/安装/升级可追溯；NocoBase npm 插件 + 运行时启用/禁用管理器。

---

## §2. 路径 1 — OSGi-style 动态 Bundle 加载/卸载

> 覆盖维度：**D-Load**（运行时类加载/卸载）。参考对标：Eclipse Equinox / Apache Felix 容器模型。

### 2.1 路径描述

引入 OSGi 容器（Felix 或 Equinox），将每个业务域打包为独立 OSGi bundle（jar + `MANIFEST.MF` 声明 `Import-Package`/`Export-Package`），在运行时经容器 API `installBundle`/`uninstallBundle`/`startBundle`/`stopBundle` 动态装载/卸载。每个 bundle 拥有独立 classloader，bundle 卸载时其类与类加载器可被 GC 回收。

### 2.2 在 Nop Platform + Quarkus 上的可行性

**结论：理论可行，工程代价极高，与平台核心假设正面冲突。不推荐。**

#### 2.2.1 类加载器隔离 vs 平台"单 classpath + 文件驱动 bean 发现"假设

Nop Platform 的 IoC 装配、VFS 资源解析、ORM `_app.orm.xml` 聚合、GraphQL schema 装配均假设**单一扁平 classpath**：

- `_module` 标记文件 + `_vfs/{moduleId}/beans/app.beans.xml` 经 VFS 在启动期一次性扫描全部模块（`ioc-and-config.md` §模块与 bean 发现规则 1-4）。
- 跨模块 ORM `<to-one refEntityName>` 经 DAG 引用，要求被引用实体的 `_*Entity.class` 在当前 classpath 可达（`cross-module-entity-reference.md`）。
- `ioc:collect-beans` 按 type/annotation 收集 bean，要求全部候选实现类在同一可见集合内（`ioc-and-config.md` §`<ioc:collect-beans>`）。

OSGi 的 per-bundle classloader 隔离会**直接破坏上述假设**：跨 bundle 的 `refEntityName` 解析、`ioc:collect-beans` 收集、VFS `_module` 扫描都需要桥接 classloader 或显式 `Import-Package`/`Export-Package` 声明，等于在平台核心的"单 classpath"模型之上强加一层 OSGi 模块化模型，两层模型语义不对齐。

#### 2.2.2 Quarkus 启动模型冲突

Quarkus 的核心卖点是**关闭期强化（closed-world enhancement）+ 启动期 bean 装配**：构建时（build-time augmentation）扫描全部 bean，生成字节码加速启动；运行时不再做 classpath 扫描。这与 OSGi 的"运行时动态 install/uninstall"根本对立——OSGi 要求运行时类可见性可变，Quarkus 要求运行时类集合关闭。

要在 Quarkus 下跑 OSGi，只能把 OSGi 容器作为**应用内的一个普通库**（`BundleContext` 由应用 main 启动），但这意味着 OSGi bundle 内的类**不参与** Quarkus/Nop IoC 装配——等于在 Nop 应用内开一个"OSGi 飞地"，飞地内的 BizModel 不会被 `IGraphQLEngine` 发现、不会进 GraphQL schema、不会被 `ioc:collect-beans` 收集。要让飞地内的 BizModel 重新接入平台，需要手写桥接注册（运行时动态注册 GraphQL action + 动态注册 bean），而平台无此标准入口（见 §2.2.3）。

#### 2.2.3 GraphQL schema 动态注册障碍

Nop GraphQL schema 在启动期由 BizModel 反射（`@BizQuery`/`@BizMutation`）+ XMeta 装配定型，`IGraphQLEngine` 按 operationName 路由，运行时**无动态增删 action 的标准 API**（`api-and-graphql.md` §统一请求分发模型）。OSGi 运行时 install 一个新 bundle 后，其 BizModel 的 action 不会自动出现在 GraphQL schema 中；要暴露它们需要：

1. 运行时反射扫描新 bundle 的 `@BizModel`/`@BizQuery`/`@BizMutation`；
2. 动态注册到 `IGraphQLEngine` 的 action 表（需平台核心改造，目前非公开 API）；
3. 动态加载对应 XMeta（`_vfs/{moduleId}/.../*.xmeta.xml`）并合并到全局 meta 注册表；
4. uninstall 时反向撤销全部注册（含已缓存的反射元数据、已生成的 `_label` 字段等）。

这是一个**平台核心级改造**，违反 Non-Goals「不修改 Nop Platform 核心」。

#### 2.2.4 与代码生成管线的冲突

Nop 代码生成是**构建期**行为：`_gen/`、`_app.orm.xml`、`_service.beans.xml`、`_module-meta.json` 均在 `mvn install` 时由 `.xgen` 脚本生成。OSGi bundle 化要求每个域 jar 自带 `MANIFEST.MF` + 包级导出声明，这与既有 codegen 产物链不兼容：

- 既有 `gen-orm.xgen` 不会生成 OSGi `Import-Package`/`Export-Package`；
- 跨域 `refEntityName` 要求被引用域的 dao jar 在编译/启动期可见（`cross-module-entity-reference.md` §dao 层依赖），OSGi 化后需把这些跨域可见性从"Maven 编译依赖"翻译为"OSGi Import-Package"，两套依赖描述需保持同步（漂移风险高）；
- `_app.orm.xml` 聚合全模块实体，OSGi 化后聚合逻辑需按 bundle 拆分或保留启动期聚合（弱化隔离价值）。

#### 2.2.5 类卸载的现实约束

即使解决了上述全部问题，OSGi bundle 卸载的类 GC 还要求：该 bundle 的全部类无任何外部强引用（包括线程局部、静态字段、长生命周期 bean 缓存）。Nop 的进程内缓存（如 A1 `GlMappingResolver` 缓存、A2 commitment SPI、各域 `IRateLimiter` 实例）会持有跨请求引用，卸载时需逐一清理，遗漏即类泄漏。这是 OSGi 在企业级 Java 长期未普及的核心原因之一。

### 2.3 侵入面评估

| 侵入对象 | 侵入程度 | 说明 |
|---------|---------|------|
| Nop Platform 核心（IoC/VFS/GraphQL） | **极高** | 需改造为支持运行时动态 bean 注册 + 动态 GraphQL action + 动态 XMeta 合并 |
| Quarkus 启动模型 | **极高** | 与关闭期强化根本对立，需绕过 build-time augmentation |
| 代码生成管线 | **高** | 需新增 OSGi manifest 生成 + 包级导出声明 + 跨域可见性翻译 |
| `app-erp-all` 打包 | **高** | 从 fat jar 改为 OSGi 容器 + 多 bundle |
| 业务域代码 | 中 | 实体/BizModel 需无对外静态状态 + 跨域引用改 Import-Package |

### 2.4 何时仍可能考虑（触发条件）

仅在以下**全部**条件成立时才重新评估路径 1：

1. 业务客户明确要求**运行时类卸载**（D-Load），且不接受"重启生效"的 D-Select 替代；
2. 部署形态为**长生命周期常驻进程**（如 SaaS 多租户共享实例，不能为单租户启停而重启）；
3. 架构 owner doc 显式授权平台核心改造（违反本研究 Non-Goals）。

当前业务场景（私有部署、按客户裁剪部署包、可接受重启生效）**不满足**上述条件。

---

## §3. 路径 2 — Maven Module Isolation（编译期模块隔离 + 启动期可选装载）

> 覆盖维度：**D-Select**（构建/部署拓扑裁剪）。参考对标：OFBiz `component-load.xml` 启动期组件清单 + Maven profile/assembly。

### 3.1 路径描述

不引入任何运行时动态机制，仅在**构建/部署拓扑层**提供可选装载能力：

1. 维持现有 19 域 Maven 多模块结构不变（每域独立 `module-<domain>/` 工程，DAG 单向依赖）。
2. `app-erp-all` 聚合启动包之外，新增 1 个或多个**裁剪版聚合工程**（如 `app-erp-core` 仅含核心 5 域、`app-erp-dist-manufacturing` 含核心 + 制造 + 质量 + 维护），通过 Maven `<dependencyManagement>` + profile 或 Maven assembly 选择性聚合域 `-web` 依赖。
3. 同一份代码产物（jar），不同部署包按客户/场景裁剪装载哪些域 jar 进入启动 classpath。
4. 启动时 Nop IoC 按既有"文件驱动 bean 发现"机制，仅发现实际在 classpath 中的 `_module` 标记文件对应的模块——未装载的域自然不被装配，其 GraphQL action / 菜单 / 权限不出现在运行时。

### 3.2 在 Nop Platform + Quarkus 上的可行性

**结论：高可行性、低代价、零平台核心侵入、与现有机制完全契合。推荐作为"按客户裁剪部署"的首选路径。**

#### 3.2.1 与平台"文件驱动 bean 发现"的天然契合

Nop 的模块发现机制（`/{moduleId}/_module` 零字节标记文件）本身就是**按 classpath 内容装配**的：classpath 里有哪些 `_module`，就装配哪些模块。路径 2 仅控制"哪些域 jar 进入 classpath"，装配逻辑零改动。未装载域的 `_module` 不在 classpath → 该域 bean 不被装配 → 该域 GraphQL action 不在 schema → 该域菜单（`action-auth.xml`）不生效。这是平台**既有能力**的自然延伸，不是新机制。

#### 3.2.2 DAG 依赖的裁剪正确性

裁剪必须遵守 DAG（`domain-module-split-analysis.md` §4.1）：

- 移除一个域时，必须同时移除所有**依赖它**的下游域（否则下游域启动期 `refEntityName`/`@Inject I*Biz` 解析失败）。
- 例如：移除 `master-data`（DAG 根）会连带移除全部 18 域（无意义）；移除 `manufacturing` 仅需同时移除依赖它的 `drp`（DRP 仿真引用 MFG）+ 检查 finance/quality 对 MFG 的引用是否为硬依赖。
- D2 `ModuleMetaReader.checkDependencyIntegrity()` **已是该裁剪校验的运行时诊断器**：裁剪后启动时运行该 API，可立即发现"装载了下游但漏装上游"的错误（`business-module-metadata.md` §4.2 + 错误码 `ERP_MODULE_DEPENDENCY_MISSING`）。这是 D2 对 D4 的天然信息输入。

#### 3.2.3 多份聚合工程的治理

为避免维护 N 个手写聚合 pom，推荐用 **Maven profile + 同一 `app-erp-all` 工程**：

- `app-erp-all/pom.xml` 默认 profile 含全部 19 域（现状）。
- 新增 `-Pdist-core` / `-Pdist-manufacturing` 等 profile，在 profile 内用 `<dependencies>` 覆盖默认域集合。
- 部署时 `mvn package -Pdist-core` 产出裁剪版启动包。
- 同一份 Java 产物，部署包差异仅在聚合 pom 的依赖集合。

**替代**：Maven assembly + descriptor 按域裁剪 fat jar；或 Gradle/自定义打包。但 Maven profile 是最小侵入方案。

#### 3.2.4 与 Quarkus 启动模型的关系

Quarkus 关闭期强化发生在 `app-erp-all` 的构建期（`quarkus-maven-plugin`）。裁剪 profile 改变的是"哪些域 jar 进入 build-time classpath"，Quarkus 据此重新强化生成针对该部署包的字节码。**完全符合** Quarkus 模型，无冲突。

### 3.3 限制

- **无运行时能力**：域的装载/移除需要重新打包 + 重启进程。不能在已启动的应用中启用/禁用域（那是 D-Switch，归路径 3）。
- **裁剪粒度 = 域级**：不能裁剪单域内的部分实体/部分 action（那是 D-Switch 的细粒度，归路径 3）。
- **多部署包的版本治理**：不同客户不同裁剪包，需明确每个裁剪包的测试矩阵（建议至少每个 profile 跑一次 `mvn clean install -DskipTests` + 关键冒烟）。
- **下游域硬依赖排查**：裁剪前必须确认目标域无下游硬依赖（D2 `businessDependencies` 元数据 + DAG 是排查依据）。

### 3.4 侵入面评估

| 侵入对象 | 侵入程度 | 说明 |
|---------|---------|------|
| Nop Platform 核心 | **零** | 完全使用既有机制（`_module` 发现 + bean 装配） |
| Quarkus 启动模型 | **零** | 符合关闭期强化模型 |
| 代码生成管线 | **零** | `_gen/`/`_app.orm.xml` 等产物链不变 |
| `app-erp-all` 打包 | **低** | 新增 Maven profile（或 assembly descriptor），默认 profile 保持现状 |
| 业务域代码 | **零** | 各域产物 jar 完全不变 |

### 3.5 何时采用（触发条件）

- 业务客户明确要求**按客户/按场景裁剪部署包**（轻量部署 vs 重型部署）。
- 可接受**重启生效**（不需要运行时启停）。
- 裁剪粒度**域级足够**（不需要单实体/单 action 级开关）。

当前若出现"客户 A 只要 5 核心域、客户 B 要全部 19 域"的部署需求，路径 2 是**零风险首选**。

---

## §4. 路径 3 — NocoBase-style 应用层插件管理器（运行时路由级开关）

> 覆盖维度：**D-Switch**（类已加载，菜单/权限/GraphQL action 级启用/禁用）。参考对标：NocoBase plugin manager（npm 插件 + 运行时启用/禁用，不卸载类）。

### 4.1 路径描述

所有 19 域仍编译进同一启动包（沿用现状），但在应用层引入**插件描述符 + 运行时启停开关**，在不卸载类的前提下，按域/按特性启用或禁用其**用户可见入口**：

1. **插件描述符**：复用 D2 `module-meta.json`（已有 `moduleId`/`version`/`businessDependencies`/`optionalFeatures`）。新增 1 个 optional 字段 `enabled`（默认 true）+ 可选 `disabledActions`（action 级细粒度禁用清单）。
2. **运行时启停**：新增一个 `ErpPluginManagerBizModel`（`app-erp-all`，对齐 D2 `ErpModuleMetaBizModel` 归属），提供 `enableModule(moduleId)`/`disableModule(moduleId)`/`enableFeature(feature)`/`disableFeature(feature)` 的 `@BizMutation`。
3. **生效语义**（不卸载类）：
   - **菜单/权限级**：`disableModule` 后该域 `action-auth.xml` 注册的菜单/资源被标记 disabled，前端菜单不渲染、权限校验拒绝（类仍加载、bean 仍装配、GraphQL action 仍可被直接调用但权限拦截）。
   - **action 级**（细粒度）：`optionalFeatures` 已声明的 config-gated 特性（如 finance 13 开关、manufacturing 9 开关）经既有 `Erp*Configs.java` 的 `*_ENABLED` 配置门控；`disableFeature` 等价于运行时把对应 config key 置 false（经 `nop-dict` 运行时覆盖，无需重启）。
   - **GraphQL action 级**：xbiz delta 机制可对单 action 做"删除/禁用"（`x:extends` 覆盖 + `x:override` 移除），但这是**构建期/部署期** delta，不是运行时动态；运行时 action 级禁用走权限拦截而非 schema 删除（见 §4.2.3）。

### 4.2 在 Nop Platform + Quarkus 上的可行性

**结论：可行性高、代价中等、平台核心侵入低；是"运行时启停"需求的现实最优解，但需明确"不卸载类"的妥协。**

#### 4.2.1 复用 D2 `ModuleMetaReader` + `optionalFeatures` 的路径

D2 已提供：

- `_module-meta.json` 全 19 域元数据（`business-module-metadata.md` §8.2 19 yaml 源清单）。
- `ModuleMetaReader` 运行时扫描 + `listOptionalFeatures()` 汇总特性清单。
- `ErpModuleMetaBizModel` 诊断端点。

路径 3 在此基础上**叠加一层启停状态**：

- `module-meta.json` 增 `enabled`（默认 true）+ `disabledActions`（可选）。
- `ModuleMetaReader` 增 `listEnabledModules()`/`isModuleEnabled(moduleId)`/`isActionEnabled(moduleId, action)`。
- `ErpPluginManagerBizModel` 增 `enable/disable` mutation（写 `nop-dict` 运行时 dict，`AppConfig.var(...)` 实时生效，与 D1 endpoint 配置双层覆盖模型同型）。

这是 D2 元数据的**自然扩展**，不是新机制。D2 的 `optionalFeatures` 已是"特性级开关"的雏形（config-gated 布尔），路径 3 把它从"启动期配置"提升为"运行时可变状态"。

#### 4.2.2 菜单/权限级开关的实现路径

Nop 的菜单/权限经 `action-auth.xml`（手写层 `x:extends="_*.action-auth.xml"`）+ `nop-auth` 权限校验实现。运行时禁用一个域的菜单有两条路径：

- **路径 3a（推荐，零代码生成改动）**：`disableModule` 经 `nop-dict` 写入一个运行时 dict（key = `erp-plugin.{moduleId}.enabled`，value = false），前端菜单渲染 + 后端权限校验均读该 dict。前端菜单组件在渲染前查 `ModuleMetaReader.isModuleEnabled`，禁用域菜单不显示；后端在 BizModel 方法入口（或全局拦截器）查该状态，禁用域 action 抛 `NopException(ERR_PLUGIN_MODULE_DISABLED)`。
- **路径 3b（备选，重）**：运行时动态修改 `action-auth.xml` 注册表。平台无此标准 API，需平台核心改造，违反 Non-Goals。

推荐 3a：dict 驱动 + 前后端双查，零平台核心改动。

#### 4.2.3 GraphQL action 级开关的现实

Nop GraphQL schema 在启动期定型，运行时**无动态增删 action 的标准 API**（同 §2.2.3）。因此 action 级"禁用"只能做**权限拦截**（action 仍在 schema 中，但调用被拒），不能做 **schema 删除**。这是路径 3 的核心妥协：

- ✅ 可做：禁用后 action 抛异常/返回 403，前端菜单/按钮隐藏，用户无法触发。
- ❌ 不能做：禁用后 action 从 GraphQL introspection 消失（需路径 1 的动态 schema）。
- xbiz delta 的 `x:override` 移除 action 是**构建期/部署期**行为（delta 文件在启动期加载时定型），不是运行时动态；若接受"重启生效"则可用 delta 做部署期裁剪（介于路径 2 与路径 3 之间）。

#### 4.2.4 不卸载类的妥协代价

- **内存**：禁用域的类仍驻留 JVM，类加载器仍持有，无 GC 回收。对 19 域规模（约百 MB 量级 jar）影响可忽略。
- **启动时间**：全部 19 域仍参与启动期装配，启动时间不因禁用而缩短（若需缩短启动时间，那是 D-Select，归路径 2）。
- **类状态泄漏**：禁用域的长生命周期 bean（缓存、监听器）仍存在并可能响应事件；需在 `disableModule` 时主动停止该域的事件监听 + 清缓存（应用层治理，非平台核心改造）。
- **安全边界**：action 仍在 schema，攻击者知道 action 名仍可直接调用（被权限拦截，但 introspection 可见）。若需隐藏存在性，需路径 1 或路径 2。

### 4.3 侵入面评估

| 侵入对象 | 侵入程度 | 说明 |
|---------|---------|------|
| Nop Platform 核心 | **低** | 仅用既有 `nop-dict`/`AppConfig`/权限校验，无核心改造 |
| Quarkus 启动模型 | **零** | 启动期仍装配全部域，运行时仅状态开关 |
| 代码生成管线 | **零** | `_module-meta.json` 增字段由 D2 既有 `gen-meta.xgen` overlay 扩展（已在 D2 建立） |
| `app-erp-all` | **低** | 新增 `ErpPluginManagerBizModel` + `ModuleMetaReader` 扩展方法 |
| 业务域代码 | **低-中** | 各域 action 入口需查插件状态（可经全局拦截器统一处理，或逐域显式查） |

### 4.4 何时采用（触发条件）

- 业务客户明确要求**运行时启用/禁用域或特性**（不接受重启生效）。
- 可接受**不卸载类**（内存/启动时间无要求，仅要求用户可见入口可关）。
- 需求粒度可能到**特性级/ action 级**（如临时关闭某客户的预算承付、临时禁用 B2B EDI）。

---

## §5. 三路径对比矩阵

> 6 维度 × 3 路径。每格标注 ✅ 可行 / ❌ 不可行 / ⚠️ 部分可行，附理由。

### 5.1 维度定义

| # | 维度 | 评估问题 |
|---|------|---------|
| D1 | 热加载能力 | 是否支持运行时（已启动后）装载/卸载，无需重启？ |
| D2 | 类隔离 | 是否提供类加载器级隔离与卸载时类 GC？ |
| D3 | 对平台核心侵入 | 是否需要改造 `nop-entropy`（IoC/VFS/GraphQL/代码生成模板）？ |
| D4 | 对 Quarkus 启动模型影响 | 是否与关闭期强化/启动期装配模型冲突？ |
| D5 | 对代码生成管线影响 | 是否需改 `.xgen` 脚本 / `_gen` 产物链 / `_app.orm.xml` 聚合？ |
| D6 | 实现复杂度 | 落地工作量与维护成本（含测试矩阵） |

### 5.2 对比矩阵

| 维度 | 路径 1 OSGi-style | 路径 2 Maven module isolation | 路径 3 NocoBase-style |
|------|------------------|------------------------------|----------------------|
| **D1 热加载能力** | ✅ 运行时 install/uninstall bundle（理论） | ❌ 需重新打包 + 重启 | ⚠️ 仅"开关"（dict 覆盖实时生效），类不卸载、action 不从 schema 删除 |
| **D2 类隔离** | ✅ per-bundle classloader + 卸载 GC（理论；受平台缓存引用制约，§2.2.5） | — 不适用（未装载即不存在，无隔离需求） | ❌ 单 classpath，不隔离、不卸载 |
| **D3 对平台核心侵入** | ❌ 极高 — 需动态 bean 注册 + 动态 GraphQL action + 动态 XMeta 合并（§2.2.3） | ✅ 零 — 既有 `_module` 发现机制天然支持 | ✅ 低 — 仅用 `nop-dict`/`AppConfig`/权限校验 |
| **D4 对 Quarkus 启动模型影响** | ❌ 极高 — 与关闭期强化根本对立（§2.2.2） | ✅ 零 — 符合 build-time classpath 裁剪 | ✅ 零 — 启动期装配不变 |
| **D5 对代码生成管线影响** | ❌ 高 — 需 OSGi manifest 生成 + 包导出 + 跨域可见性翻译（§2.2.4） | ✅ 零 — `_gen`/`_app.orm.xml` 产物链不变 | ✅ 零 — 复用 D2 `gen-meta.xgen` overlay |
| **D6 实现复杂度** | ❌ 极高 — 平台核心改造 + OSGi 容器集成 + 卸载状态治理 + 全域重构 | ✅ 低 — Maven profile/assembly + D2 依赖完整性校验复用 | ⚠️ 中 — 插件管理 BizModel + 前后端状态查询 + 各域 action 拦截治理 |

### 5.3 矩阵读法

- **路径 1**：仅在 D1/D2（运行时动态 + 类隔离）有理论优势，但 D3/D4/D5/D6 全面红区，且其优势在本项目场景（私有部署、可重启）无刚需。**性价比最差**。
- **路径 2**：D3/D4/D5/D6 全绿，仅缺 D1（运行时能力）。对"按客户裁剪部署"需求是零风险最优解。
- **路径 3**：D3/D4/D5 绿、D6 中，D1 部分可行（开关实时生效但类不卸载）。对"运行时启停入口"需求是现实最优解，妥协明确。

---

## §6. 推荐路径裁决（Decision R1）

### 6.1 裁决

**推荐：路径 2（Maven module isolation）为默认采用路径；路径 3（NocoBase-style）为运行时启停需求的补充；路径 1（OSGi）不采用。**

具体地：

- **当前阶段（无私有部署裁剪需求、无运行时启停需求）**：不实施任何路径，维持 `app-erp-all` 全量聚合现状。本研究已关闭 D4 可行性问题，实际实现归 successor。
- **当出现"按客户/按场景裁剪部署包"需求时**：实施路径 2（Maven profile + D2 依赖完整性校验）。这是零平台侵入、零代码生成改动、低代价的方案。
- **当出现"运行时启用/禁用域或特性"需求时**：实施路径 3（在 D2 元数据基础上叠加 `enabled` 状态 + `nop-dict` 运行时覆盖 + 前后端双查）。明确接受"不卸载类/action 仍可见于 introspection"的妥协。
- **路径 1 仅在 §2.4 三条件全成立时重新评估**（当前不成立）。

### 6.2 推荐理由

1. **需求维度匹配**：业务客户提出的"按客户选择性装载域"需求，本质是 D-Select（启动期可选装载），路径 2 精准命中且代价最低；不需要 D-Load（运行时类卸载）的重量级能力。
2. **平台契合度**：路径 2/3 完全使用 Nop Platform 既有机制（`_module` 文件发现、`nop-dict` 运行时覆盖、`AppConfig.var`、权限校验），零核心侵入，符合 AGENTS.md「不改变平台核心」原则。
3. **D2 投资复用**：路径 2 直接复用 `ModuleMetaReader.checkDependencyIntegrity()` 做裁剪校验；路径 3 直接复用 `_module-meta.json` schema + `optionalFeatures`。D2 的元数据投资在 D4 推荐路径中得到充分回报。
4. **风险可控**：路径 2/3 均为增量、可回退（移除 profile / 移除插件管理器即恢复全量聚合）；路径 1 是平台核心级重构，不可回退。
5. **演进路径保留**：若未来 SaaS 多租户场景出现 D-Load 硬需求，路径 2/3 的元数据层（`module-meta.json` + 启停状态）可作为路径 1 的信息输入，不浪费投资。

### 6.3 考虑的替代方案

3 条路径互为候选，裁决矩阵已在 §5 给出。补充被否决的次要候选：

- **候选 X：Spring Boot + Spring Plugin**：否决。本项目运行在 Quarkus + Nop IoC，非 Spring；引入 Spring 栈等于替换平台，违反 Non-Goals。
- **候选 Y：JPMS（Java Platform Module System，`module-info.java`）**：否决。JPMS 是构建期模块化（类似路径 2 的更细粒度版），但不提供运行时动态能力，且与 Nop 既有 Maven 多模块结构重叠增益有限；路径 2 的 Maven profile 已覆盖其价值。
- **候选 Z：微服务化拆分（每域独立服务）**：否决。这是另一个维度（进程边界）的决策，超出"插件热管理"范围；`docs/analysis/2026-06-22-0000-cross-domain-coupling-vs-microservice.md` 已就该主题独立分析。

### 6.4 残留风险

1. **路径 2 多部署包测试矩阵膨胀**：每新增一个裁剪 profile，需配套冒烟测试。缓解：复用既有全量基线，裁剪包仅跑"装载完整性 + 关键冒烟"，不做全量回归。
2. **路径 3 类状态泄漏**：禁用域的长生命周期 bean 仍可能响应事件（如 NopSysEvent 订阅）。缓解：`disableModule` 时主动取消该域事件订阅 + 清缓存（应用层治理）；该治理是 successor 实现时的强制项。
3. **路径 3 安全可见性**：禁用 action 仍出现在 GraphQL introspection。缓解：对安全敏感场景，叠加路径 2 的部署期裁剪（不装载该域 jar）。
4. **D2 元数据漂移**：路径 2/3 依赖 `businessDependencies` 元数据准确。缓解：D2 `gen-meta.xgen` 一致性校验 + successor 版本范围求解器。
5. **组合需求（D-Load + D-Switch）**：若业务客户同时要运行时卸载 + 细粒度开关，需路径 1 + 路径 3 叠加，代价超过任一单路径。归 successor，触发条件见 §6.5。

### 6.5 触发采用本推荐的事件/条件

| 路径 | 触发条件（满足任一即启动 successor plan） |
|------|------------------------------------------|
| 路径 2 | 业务客户明确要求按客户/按场景裁剪部署包 + 可接受重启生效 + 架构 owner doc 授权新增聚合 profile |
| 路径 3 | 业务客户明确要求运行时启用/禁用域或特性（不接受重启）+ 可接受不卸载类妥协 + 架构 owner doc 授权插件管理器 |
| 路径 1 | §2.4 三条件全成立（运行时类卸载刚需 + 长生命周期常驻进程 + 平台核心改造授权） |

当前均未触发，D4 以本研究关闭，实际实现归上述 successor。

---

## §7. 与 D1/D2 的关系

| 工作项 | 与本研究的关系 |
|--------|---------------|
| **D1 外部 API 集成参考模式** | D1 是 D4 的前置（roadmap §7 mermaid `D1 --> D4`）。D1 提供的 3 集成案例（logistics Carrier Gateway / b2b EDI / master-data 汇率 API client）作为本研究评估"插件边界是否涉及外部集成生命周期"的参考。结论：外部集成 client 的启停本质是 config-gated 特性开关（D-Switch），归路径 3 的 `optionalFeatures` 机制，不需要独立插件化。 |
| **D2 业务模块元数据 BT5** | D2 是本研究路径 2/3 的**直接信息输入**：`module-meta.json` 的 `businessDependencies` 支撑路径 2 裁剪校验；`optionalFeatures` 支撑路径 3 特性级开关。D2 的 `ModuleMetaReader` 是路径 3 插件管理器的自然基础。D2 与 D4 是"元数据 → 生命周期管理"的演进关系，详见 `business-module-metadata.md` §6 + 本文 §4.2.1。 |

---

## §8. 平台约束佐证（Phase 2 复核记录）

> 本段记录本研究对 Nop Platform 核心约束的断言经 `nop-entropy/docs-for-ai/` 佐证或修正的结果，确保 §2-§4 的可行性结论有平台文档依据。

| 断言 | 平台文档佐证 | 状态 |
|------|-------------|------|
| Nop 应用侧 bean 发现是文件驱动、启动期一次性，非 Java classpath scanning、非运行时动态注册 | `02-core-guides/ioc-and-config.md` §模块与 bean 发现规则 1-4（"基于文件，不是基于 Java classpath scanning"；`/{moduleId}/_module` 标记 + `app.beans.xml`/`app-*.beans.xml` 装配） | ✅ 佐证 |
| GraphQL schema 由 BizModel 反射 + XMeta 在启动期装配定型，运行时无动态增删 action 的标准 API | `02-core-guides/api-and-graphql.md` §统一请求分发模型（`IGraphQLEngine` 按 operationName 路由到 `@BizQuery`/`@BizMutation`，action 随 bean 装配注册）；`model-first-development.md` §模块级生成链（XMeta 经 `{app}-meta` 生成） | ✅ 佐证 |
| 代码生成 `_gen/`/`_app.orm.xml`/`_service.beans.xml` 是构建期产物，手改会被 `mvn install` 覆盖 | `02-core-guides/model-first-development.md` §模块级生成链 + `architecture-principles.md`（不跳过模型直接改生成物）；`ioc-and-config.md` 规则 8 | ✅ 佐证 |
| **修正**：本研究初稿曾写"Quarkus CDI 编译期发现" | 平台实际使用 **Nop 自有 IoC**（非 Quarkus CDI），bean 发现是**启动期文件驱动**而非编译期；Quarkus 仅作为运行时 + build-time 强化。`ioc-and-config.md` 全文以 Nop IoC 为准，未提及 CDI bean discovery。 | ✏️ 已在 §1.4/§2.2.2/§3.2.1 修正为"Nop IoC 启动期文件驱动发现" |
| **修正**：本研究初稿曾写"GraphQL schema 编译期生成" | 平台实际在**运行时启动期**从 BizModel 反射装配 schema（非编译期生成 schema 文件）；BizModel 类本身是编译产物，但 schema 装配是启动期行为。 | ✏️ 已在 §1.4/§2.2.3/§4.2.3 修正为"启动期装配定型" |
| 跨模块 ORM `refEntityName` 要求被引用实体类在 classpath 可达（dao 层 Maven 依赖） | `02-core-guides/cross-module-entity-reference.md` §dao 层依赖 | ✅ 佐证（支撑路径 2 DAG 裁剪正确性 + 路径 1 OSGi classloader 隔离冲突） |
| `ioc:collect-beans` 按 type/annotation 收集 bean 要求候选在同一可见集合 | `02-core-guides/ioc-and-config.md` §`<ioc:collect-beans>` | ✅ 佐证（支撑 §2.2.1 OSGi per-bundle classloader 破坏收集假设） |

**结论**：本研究 §2-§4 的可行性断言经平台文档复核，2 处初稿表述（"CDI 编译期"/"schema 编译期生成"）已在正文修正为精确表述，其余断言均有平台文档佐证。可行性结论（路径 1 不可行/路径 2 高可行/路径 3 中高可行）不受修正影响——修正反而强化了路径 2/3 的"零平台侵入"判断（因为 Nop IoC 启动期文件发现机制天然支持 classpath 内容裁剪）。

---

## §9. 反模式自检表

| # | 反模式 | 后果 | 正确做法 |
|---|--------|------|---------|
| AP1 | 在未出现明确裁剪/启停需求时提前实施路径 2/3 | 增加维护面而无业务回报 | 当前维持全量聚合现状；路径 2/3 实施 由 §6.5 触发条件驱动 |
| AP2 | 把路径 3 的"action 仍可见于 introspection"当成安全边界 | 攻击者可枚举 action 名 | 安全敏感场景叠加路径 2 部署期裁剪（不装载该域 jar） |
| AP3 | 路径 2 裁剪时不跑 D2 `checkDependencyIntegrity` | 漏装上游域导致启动期 `refEntityName` 失败 | 每个裁剪包启动后强制运行 `ModuleMetaReader.checkDependencyIntegrity()` |
| AP4 | 路径 3 `disableModule` 后不取消事件订阅/不清缓存 | 禁用域仍响应 NopSysEvent，行为泄漏 | successor 实现时 `disableModule` 必须主动取消订阅 + 清长生命周期状态 |
| AP5 | 为路径 1 修改 `nop-entropy` 核心 IoC/GraphQL | 违反 Non-Goals + 不可回退 | 路径 1 仅在 §2.4 三条件全成立 + 平台核心改造显式授权时启动 |
| AP6 | 把 D-Load/D-Select/D-Switch 三维度混为一谈论"插件" | 路径不可比、决策失焦 | 显式声明需求落在哪个维度（§1.2 Decision R0），按维度选路径 |
| AP7 | 路径 2 多部署包共享同一份测试基线不补裁剪冒烟 | 裁剪包遗漏上游依赖在生产才暴露 | 每个裁剪 profile 至少跑装载完整性 + 该 profile 独有的关键业务冒烟 |

---

## §10. Follow-up / Deferred Successor

本研究关闭 D4 可行性问题，以下为实现层面的 successor（均需独立 plan + 触发条件满足）：

1. **路径 2 实现**（Maven profile + 裁剪部署包）— 触发：§6.5 路径 2 条件。
2. **路径 3 实现**（插件管理器 + `enabled` 状态 + `nop-dict` 覆盖）— 触发：§6.5 路径 3 条件。依赖 D2 `module-meta.json` 扩展 `enabled`/`disabledActions` 字段（轻量 schema 演进）。
3. **路径 1 重新评估**（OSGi/动态类卸载）— 触发：§2.4 三条件全成立。
4. **D2 `businessDependencies` 版本范围求解器**（SemVer range resolution）— 触发：模块业务版本数 > 3 + 不兼容升级场景（D2 Deferred，路径 2 裁剪校验精度依赖此）。
5. **SaaS 多租户版本管理编排** — 触发：SaaS 多租户部署 + tenant-model 集成授权（D2 Deferred，与路径 3 存在信息关联，可能演化为路径 1+3 组合）。
6. **路径 3 安全加固**（禁用 action 的 schema 级隐藏）— 触发：合规审计要求 action 不可见性 + 接受平台核心改造授权。

---

## 参考

- `docs/backlog/deepening-roadmap.md` §5 Milestone D / D4（line 76/92 — D4 范围与 ORM 变更=否）
- `docs/analysis/erp-survey/2026-07-20-post-survey-strategic-gaps.md`（OFBiz/ERP5/NocoBase 对标输入）
- `docs/architecture/business-module-metadata.md`（D2 owner doc — 元数据信息输入）
- `docs/architecture/external-api-integration-pattern.md`（D1 owner doc — 集成案例输入）
- `docs/architecture/domain-module-split-analysis.md` §4.1（DAG 依赖基线）
- `../nop-entropy/docs-for-ai/02-core-guides/ioc-and-config.md` §模块与 bean 发现规则 + `<ioc:collect-beans>`
- `../nop-entropy/docs-for-ai/02-core-guides/api-and-graphql.md` §统一请求分发模型
- `../nop-entropy/docs-for-ai/02-core-guides/model-first-development.md` §模块级生成链
- `../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md` §dao 层依赖
- `../nop-entropy/docs-for-ai/02-core-guides/architecture-principles.md`（不跳过模型直接改生成物）
