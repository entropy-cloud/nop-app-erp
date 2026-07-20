---
调研日期: 2026-07-20
分类: 深度对比分析
状态: 已完成
---

# NocoBase vs Nop Platform / nop-app-erp 深度对比分析报告

> **摘要**：本文对 NocoBase（Node.js 无代码平台，~21k Stars）与 Nop Platform（Java 模型驱动低代码平台）+ nop-app-erp（19 域 ERP 参考应用）进行 13 个维度的系统对比。核心发现：两种平台代表了"运行时元数据解释"与"编译时模型生成"两种根本不同的哲学路径，各自在特定场景有不可替代的优势。报告末尾给出战略选择建议与相互借鉴点。

---

## 1. 平台哲学：UI 驱动 vs 模型驱动

这是两者最根本的差异，决定了所有其他维度的设计选择。

### NocoBase：运行时元数据驱动的无代码

NocoBase 的核心假设是：**数据模型在运行时通过数据库中的元数据表定义**。`Collection` 类（`packages/core/database/src/collection.ts:154-190`）在构造函数中从 `CollectionOptions`（包含 `name`、`fields`、`inherits` 等）动态构建 Sequelize 模型。字段增删改直接操作 `collections` 表，Sequelize 模型在运行时重建。

```typescript
// collection.ts:166-189 — 运行时动态构建模型
constructor(options: CollectionOptions, context: CollectionContext) {
    this.modelInit();  // 动态创建 Sequelize Model
    this.db.modelCollection.set(this.model, this);
    this.setFields(options.fields);  // 运行时注册字段
}
```

这意味着：
- 数据库是模式的真相源（`collections` 表存储字段元数据）
- 模式变更无需停机——API 调用即可增删字段
- 无代码生成步骤，无编译期
- 真正的 WYSIWYG：浏览器内所见即所得

### Nop Platform：编译时模型驱动低代码

Nop 的核心假设是：**磁盘上的 XML 文件是唯一真相源，所有代码从模型生成**。开发流程（`model-first-development.md:5-12`）：

1. 修改 `model/*.orm.xml`
2. 首次建模块用 `nop-cli gen` 生成骨架
3. 后续变更用 `./mvnw` 触发增量再生
4. 只在非生成文件中写定制逻辑

这意味着：
- XML 文件是权威源，数据库从模型推导
- 模式变更需修改 XML → codegen → migration（编译时验证）
- 生成的 Java 代码具有完整类型安全
- 编译期捕获错误（字段名、类型、关系）

**根本差异**：NocoBase 将"模式即数据"（Pattern as Data），Nop 将"模型即代码"（Model as Code）。前者换来极致的运行时灵活性，后者换来极致的编译期安全性。

---

## 2. 插件架构

### NocoBase：正式生命周期 + Node.js npm 包

NocoBase 的 `Plugin` 抽象类（`packages/core/server/src/plugin.ts:44-139`）定义了完整的生命周期钩子：

```typescript
abstract class Plugin<O = any> implements PluginInterface {
    afterAdd()   {}  // 注册后
    beforeLoad() {}  // 预加载
    async load() {}  // 主初始化
    async install(options?) {}  // 首次安装
    async upgrade() {}  // 版本升级
    async beforeEnable() / afterEnable() {}
    async beforeDisable() / afterDisable() {}
    async beforeRemove() / afterRemove() {}
}
```

`PluginManager`（`plugin-manager.ts:428-472`）按注册顺序加载插件（运行时 enable 操作使用 `@hapi/topo` 拓扑排序处理依赖关系，见 `sort()` 方法 `plugin-manager.ts:1191-1203`）：
1. 先遍历所有插件调用 `beforeLoad()`
2. 再遍历调用 `loadCollections()` → `loadAI()` → `load()`
3. 插件发现通过扫描 `node_modules` 中的 `@nocobase/plugin-*` 前缀

插件可注册 `collections/`、`migrations/`、`actions/`、`commands/`。预置插件清单（`presets/nocobase/package.json`）列出 108+ 插件，涵盖 ACL、auth、workflow、UI blocks、data visualization 等。

### Nop Platform：Maven 模块链 + IoC Bean 贡献

Nop 没有"插件生命周期"的形式概念，而是通过**编译时模块链**和**运行时 IoC 容器**实现扩展：

| 层级 | Nop 机制 | NocoBase 对比 |
|------|---------|-------------|
| 模块定义 | Maven 模块 + `orm.xml` 模型 | npm 包 + Plugin class |
| 组装 | Maven 依赖（`dependency`） | PluginManager 拓扑排序 |
| 配置注入 | `beans.xml` IoC（Nop IoC 容器） | Plugin constructor options |
| 跨模块调用 | `I*Biz` 接口 + `@Inject` | `app.pm.get(name)` 获取插件实例 |
| 运行时扩展 | `x:extends` delta + SPI Provider | Plugin 方法覆盖 + 事件监听 |

Nop 的模块链（`domain-module-pattern.md:5-8`）：`model → codegen → dao → meta → service → web → app → api`。每个环节在 Maven 编译期完成，无运行时插件加载开销。

**关键差异**：NocoBase 的插件可在运行时 enable/disable（`plugin-manager.ts:480+`），Nop 的模块在编译期确定；NocoBase 的插件可热加载，Nop 需重启。

---

## 3. 数据模型定义

### NocoBase：运行时 Collection/Field

`Collection` 类（`collection.ts:154-190`）在构造函数中动态创建 Sequelize 模型：

- 31 种字段类型（`core/database/src/fields/`）
- 支持继承（`inherits: string[]`）、排序、视图（`viewName`）
- 字段通过 `setFields()` 在运行时注册到 Sequelize
- 支持 `Repository` 模式封装 CRUD

```typescript
interface CollectionOptions {
    name: string;
    fields?: FieldOptions[];
    inherits?: string[];
    sortable?: CollectionSortable;
    viewName?: string;  // 数据库视图
    template?: string;   // 模板
}
```

### Nop Platform：编译时 orm.xml + codegen

Nop 的数据模型在 `model/*.orm.xml` 中定义（`model-first-development.md:14-20`）：

```xml
<entity name="app.erp.pur.dao.entity.ErpPurOrder" tableName="erp_pur_order">
    <columns>
        <column name="orderId" code="ORDER_ID" propId="1" stdSqlType="VARCHAR" precision="32"/>
        <column name="totalAmount" code="TOTAL_AMOUNT" propId="5" stdSqlType="DOUBLE"/>
    </columns>
</entity>
```

代码生成三步（`model-first-development.md:172-198`）：
1. 从 orm.xml 生成 `_app.orm.xml`（聚合 ORM）
2. 从 app.orm.xml 生成 `_Nop*.java`（实体类）
3. 从 app.orm.xml 生成其他模型派生

**差异本质**：NocoBase 的"模型"是运行时内存对象，字段是 JS 属性；Nop 的"模型"是 XML 源文件，字段是 Java 类属性。前者数据库 schema 可随时变更，后者需经过模型→生成→编译→迁移的完整管线。

---

## 4. UI 渲染

### NocoBase：JSON Schema（Formily → React）

NocoBase 的 UI 层基于 JSON Schema + Formily 2 + Ant Design 5：
- 页面和区块定义为 JSON Schema，存储于 `uiSchemas` 集合
- 区块（Block）可插拔：表格、表单、看板、日历、甘特图
- WYSIWYG 页面设计器：浏览器内拖拽配置

数据流：`用户操作 → REST API → Resourcer 中间件 → ACL 检查 → Action Handler → Repository → 数据库`

### Nop Platform：AMIS view.xml（XML → JSON → AMIS Renderer）

Nop 采用百度 AMIS 作为前端渲染引擎，通过三层 Delta 架构（`view-and-page-customization.md:20-31`）：

```
xmeta (实体元数据) → codegen → _gen/_Xxx.view.xml (自动生成基线)
    ↓ x:extends
Xxx.view.xml (保留层定制)
    ↓ GenPage 读取
main.page.yaml (入口)
    ↓ x:gen-extends
AMIS JSON (运行时输出)
```

```xml
<view x:schema="/nop/schema/xui/xview.xdef">
    <objMeta>/nop/auth/model/NopAuthUser/NopAuthUser.xmeta</objMeta>
    <grids><grid id="list"/></grids>
    <forms><form id="edit"/></forms>
    <pages><crud name="main" grid="list"/></pages>
</view>
```

**差异**：NocoBase 的 UI Schema 在运行时完全动态（存储在 DB 表中可随时编辑），Nop 的 view.xml 是编译时文件 + 运行时 `x:extends` 合并。NocoBase 有 WYSIWYG 设计器；Nop 当前靠手写/生成 XML（AMIS 本身支持 JSON 编辑但不自带可视化设计器）。

---

## 5. 工作流/自动化

### NocoBase：Trigger → Processor → Instruction 管线

工作流（`plugin-workflow/src/server/Plugin.ts:54-56`）注册三种可扩展注册表：

```typescript
class PluginWorkflowServer extends Plugin {
    instructions: Registry<InstructionInterface> = new Registry();
    triggers: Registry<Trigger> = new Registry();
    functions: Registry<CustomFunction> = new Registry();
}
```

**触发器类型**（`triggers/CollectionTrigger.ts:27-32`）：
- `CollectionTrigger`（数据表 afterCreate/afterUpdate/afterDestroy）
- `ScheduleTrigger`（定时 Cron）
- `ActionTrigger`（自定义按钮）
- `ManualTrigger`（人工审批）

**指令类型**（15+）：Create、Update、Query、Condition、Loop、Parallel、Manual、Delay、Request、SQL、JavaScript、Aggregate、Mailer 等

**执行模型**（`Processor.ts:48-58`）：Processor 维护 `jobsMapByNodeKey`，按 DAG 执行 flow nodes，支持事务、超时、重试、信号中断。

### Nop Platform：XWF 工作流 + xbiz 事件

Nop 的工作流定义在 `.xwf` 文件中（`workflow-configuration.md:5-27`）：

```xml
<workflow wfName="my-flow" wfVersion="1" displayName="我的流程">
    <start startStepName="first-step"/>
    <end/>
    <steps>
        <step name="first-step" displayName="第一步">
            <assignment>...</assignment>
            <transition>...</transition>
        </step>
    </steps>
</workflow>
```

支持会签（and-group/or-group/seq-group/vote-group）、子流程、抄送、驳回/撤回。

同时，Nop 的 xbiz 提供了字段级声明式逻辑（`domain-design-guidelines.md:683-729`）：

| 逻辑层 | 载体 | 用途 |
|--------|------|------|
| 字段级 | xmeta autoExpr / xbiz beforeSave | 默认值、自动计算 |
| 业务动作 | BizModel @BizMutation | 审批、过账、跨域操作 |
| 流程编排 | XWF 工作流 | 多步审批流 |

**差异**：NocoBase 的工作流是"纯事件驱动"的——所有触发器都是对数据变化的反应；Nop 的工作流是"状态机驱动"的——步骤、转换、动作是显式定义的状态机。NocoBase 的 Instruction 类型更丰富（15+ vs Nop 的 5+ 会签类型），但 Nop 的 xbiz 声明式逻辑更为轻量（无需创建完整工作流即可实现字段级自动行为）。

---

## 6. 权限系统

### NocoBase：ACL 资源-动作-角色 + 固定参数注入

ACL 核心（`core/acl/src/acl.ts:75-249`）：

```typescript
class ACL extends EventEmitter {
    roles = new Map<string, ACLRole>();
    can({role, resource, action}): CanResult | null
}
```

**评估流程**：
1. 检查角色-资源-动作权限
2. 检查角色策略
3. 合并固定参数（如 `createdById: $user.id`）
4. 返回 null → 403

**数据域过滤**：通过模板变量注入 SQL 过滤条件：
```json
{ "createdById": "$user.id" }
```
支持 `$user`（当前用户对象）、`$nRole`（当前角色名）变量。

### Nop Platform：三层权限 + XMeta 数据规则

Nop 权限系统分三层（`auth-and-permissions.md:3-10`）：

| 层 | 控制点 | 机制 |
|----|--------|------|
| HTTP 路径认证 | URL 模式匹配 | `AuthFilterConfig.isPublicPath()` |
| 操作权限 | BizModel 方法级别 | `nopActionAuthChecker` |
| 数据权限 | 自动附加到查询条件 | `nopDataAuthChecker` |

Nop 的数据权限支持 `action-auth.xml` 中声明的行级规则（类似 NocoBase 的固定参数注入但更结构化），结合 XMeta 的字段级可见性控制。

**差异**：NocoBase 的 ACL 设计更简洁——一个 `can()` 方法统一处理；Nop 分三层但配置路径灵活（可在 xmeta、action-auth.xml、beans.xml 中分别控制）。NocoBase 的固定参数注入（`createdById = $currentUser`）比 Nop 的 action-auth 规则更直观。

---

## 7. API 设计

### NocoBase：自动 REST 端点

`Resourcer`（`core/resourcer/src/resourcer.ts`）从 Collection 定义自动生成 REST 端点：

```
GET    /api/{collection}:list
POST   /api/{collection}:create
GET    /api/{collection}:get
PUT    /api/{collection}:update
DELETE /api/{collection}:destroy
```

支持自定义 action、中间件链、资源关联嵌套。

### Nop Platform：自动 GraphQL（IGraphQLEngine）

Nop 所有 HTTP 入口统一通过 `IGraphQLEngine`（`api-and-graphql.md:14-38`）：

```
POST /graphql    (GraphQL query string)
GET|POST /r/{op} (REST)
GET|POST /p/{op} (内容感知)
POST /px/{svc}/{method} (分布式 RPC)
POST /jsonrpc    (JSON-RPC 2.0)
```

operationName 统一格式：`{bizObj}__{method}`（如 `ErpPurOrder__findPage`）。

**差异**：NocoBase 默认 REST + 可扩展；Nop 默认 GraphQL + 自动适配 REST/RPC。GraphQL 的优势在于客户端可精确选择返回字段、支持嵌套查询；REST 的优势是简单通用、缓存友好。NocoBase 的 REST 更"标准"（`/api/collection:action`），Nop 的 `/r/{opName}` 更灵活（任意 BizModel 方法自动暴露）。

---

## 8. Delta 定制

### NocoBase：Schema 补丁

NocoBase 的定制主要通过在 Plugin 中覆盖行为或通过在客户端层替换 Schema 组件实现。无正式的"差量合并"机制——定制通常意味着替换或扩展组件。

### Nop Platform：形式化 Delta 机制

Nop 的 Delta 是平台级内置机制（`delta-customization.md:3-13`）：

- **文件层**：`_vfs/_delta/{deltaDir}/...` 中的文件自动覆盖原始路径
- **合并层**：`x:extends="super"` + `x:override="replace|remove|merge"` 实现精确差量
- **多层**：支持多个 delta 目录解决产品线定制

```xml
<orm x:extends="super" x:schema="/nop/schema/orm/orm.xdef">
    <entities>
        <!-- 增量修改 -->
    </entities>
</orm>
```

同时，生成管线的扩展文件模式（保留层覆盖生成层）提供了第二层定制能力（`model-first-development.md:66-80`）：

```
全局: _vfs/_delta/default/nop/auth/orm/app.orm.xml
局部: Xxx.view.xml 通过 x:extends _gen/_Xxx.view.xml
```

**差异**：这是 Nop 在架构理念上的显著差异化能力。Nop 的 Delta 机制是**平台原生**的，适用于模型、配置、页面、i18n 等任何 XDSL 资源；NocoBase 没有对应的形式化差分机制，定制通常意味着 Plugin 覆盖或 Schema 替换（这与 NocoBase "运行时元数据即真相"的设计哲学一致——Schema 替换而非差量合并是其自然选择）。

---

## 9. ERP 就绪度

### NocoBase：零预置业务域

NocoBase 是一个**平台**，不包含任何预置的 ERP 业务域。创建 ERP 需要：
- 手动配置 Collection（表结构）
- 手动配置 Workflow（业务流程）
- 手动构建 UI 页面（区块组合）
- 自己实现会计、库存、采购等业务逻辑

### nop-app-erp：19 域预建 ORM 模型

nop-app-erp 基于 Nop Platform 构建，拥有 **18 业务域 + 1 跨域通知子系统**的完整 ORM 模型（`domain-module-split-analysis.md:46-69`）：

| 层 | 域 | 数量 |
|----|----|------|
| 核心域 | master-data、inventory、purchase、sales、finance | 5 |
| 第一批扩展 | assets、projects、manufacturing、quality、maintenance | 5 |
| 第二批扩展 | crm、cs、hr、aps、contract、drp、logistics、b2b | 8 |
| 跨域 | notify（通知派发） | 1 |

每个域包括完整的业务设计（`domain-design-guidelines.md`）：
- 双轴状态机（`docStatus` + `approveStatus` + `posted` 三轴）
- 标准字段集（`orgId`/`businessDate`/`posted`/多币种四件套）
- 跨域交互规则（19 条跨域状态映射表）
- ErrorCode 命名规范
- 三档删除策略
- 会计期间统一管理

**差异**：这是 nop-app-erp 最显著的差异化优势——开箱即用的 ERP 领域骨架，NocoBase 用户需要从零搭建。

---

## 10. 社区与生态系统

### NocoBase

- **GitHub Stars**: ~21k
- **插件数**: 108+（`presets/nocobase/package.json` 列出 100+ 依赖）
- **技术栈**: Node.js + TypeScript + React + AGPL-3.0
- **社区形态**: 国内 + 国际活跃；中文文档完善；商业化版本（NocoBase Cloud）
- **关键插件**: 工作流（19 子插件）、ACL、Auth（5 种）、UI Blocks（10+）、数据可视化、AI/MCP、备份恢复

### Nop Platform

- **GitHub Stars**: 较小（但项目目标不同——平台框架而非独立产品）
- **插件数**: 平台内置模块（~20 个，如 `nop-auth`、`nop-sys`、`nop-wf`、`nop-job`）+ 应用层模块
- **技术栈**: Java + Quarkus + Apache-2.0
- **社区形态**: 较小但专注；英文/中文混合文档
- **关键能力**: 独特 delta 机制、模型驱动代码生成、XDSL 语言族

**差异**：NocoBase 的社区规模更大，插件生态更丰富；Nop Platform 的社区较小但平台哲学更独特（Delta、XDSL、模型驱动）。

---

## 11. NocoBase 做得更好的方面

### 11.1 真正的无代码体验

NocoBase 的非技术用户可以通过浏览器 WYSIWYG 页面设计器创建数据表和页面，无需接触任何代码。Nop 需要编写 XML（orm.xml、view.xml）并通过 codegen 管线。

### 11.2 运行时模式变更

NocoBase 的 Collection 定义存储在 DB 中，用户可随时通过 UI 或 API 添加/修改/删除字段。Nop 的模型变更需修改 XML → codegen → migration，周期更长。

### 11.3 插件热管理

NocoBase 支持插件的 enable/disable（无需重启），而 Nop 的模块在编译时确定。

### 11.4 多数据源抽象

NocoBase 的 `DataSourceManager` 支持主 DB + 外部 DB + REST API 作为数据源，对异构系统集成更友好。

### 11.5 可视化工作流设计器

NocoBase 提供完整的可视化工作流设计器（节点拖拽 + 配置面板），Nop 的工作流通过 XML 配置。

### 11.6 更丰富的字段类型（31 种）

NocoBase 提供了比 Nop 更丰富的字段类型（formula、sequence、sort、uuid、nanoid、snowflake-id 等），开箱即用。

### 11.7 前端技术栈现代化

React 18 + Ant Design 5 + Formily 2 提供了成熟的现代化前端框架；Nop 的 AMIS 是百度内部框架，社区较小。

---

## 12. Nop Platform 做得更好的方面

### 12.1 编译期类型安全

Nop 的代码生成生成带类型注解的 Java 实体类，字段名/类型/关系在编译期验证。NocoBase 的 JS 属性在运行时解析，类型错误可能在生产环境才暴露。

### 12.2 形式化 Delta 定制

Nop 的 `x:extends` + `x:override` + `_vfs/_delta/` 提供了精确的多层差量覆盖能力，这是 NocoBase 完全缺失的机制。产品线定制（SaaS 多租户不同版本）场景优势明显。

### 12.3 事务性 BizModel

Nop 的 `@BizMutation` 自动包装事务，`@Inject I*Biz` 支持类型安全的跨模块调用，`requireEntity()` / `doFindPage()` / `doFindList()` 等安全 API 提供了标准化的服务层抽象。NocoBase 的工作流节点需要手动处理事务边界。

### 12.4 预建 ERP 领域

nop-app-erp 的 19 域完整 ORM 模型是 NocoBase 无法直接比拟的——NocoBase 上创建等价的 ERP 需要从零搭建数据模型、业务逻辑和 UI。

### 12.5 编译时模型校验

`model/*.orm.xml` 的 `x:schema` 属性在编译时校验 XML 结构、字段类型、关系完整性，早期发现问题。NocoBase 的 Collection 校验在运行时（`collection.ts:258-299` 使用 Joi），错误可能在运行时才暴露。

### 12.6 统一 API 入口

Nop 的 `IGraphQLEngine` 统一处理 GraphQL/REST/RPC/JSON-RPC，BizModel 方法无需关心调用协议。NocoBase 的 REST 是唯一默认协议，自定义 action 需额外注册。

### 12.7 Java 生态系统

Java 的类型安全、成熟的 IDE 支持、丰富的测试框架（JUnit + Nop AutoTest + snapshot testing）、更成熟的性能调优工具，对大规模 ERP 部署更友好。

---

## 13. 战略建议

### 何时选择 NocoBase

| 场景 | 理由 |
|------|------|
| **快速原型 / MVP** | 浏览器内配置，零代码开发，数天即可搭建原型 |
| **非技术团队维护** | 运营人员可直接通过 UI 维护数据模型和页面 |
| **轻量业务系统** | CRM、项目管理、审批等（无需复杂会计逻辑） |
| **多数据源集成** | 需要连接外部 DB / REST API 作为数据源 |
| **Node.js 技术栈团队** | 团队已熟悉 JS/TS，不希望引入 Java |
| **需要频繁模式变更** | 业务字段经常增减，无法接受编译部署周期 |

### 何时选择 Nop Platform + nop-app-erp

| 场景 | 理由 |
|------|------|
| **复杂 ERP 系统** | 需要完整的进销存/财务/制造领域模型 |
| **多产品线定制** | Delta 机制支持产品线间差异而不破坏升级路径 |
| **合规/审计需求** | 编译期校验 + 完整事务边界 + 审计追溯 |
| **Java 技术栈团队** | 需要利用现有 Java 基础设施和技能 |
| **高并发/高性能** | Java + Quarkus 在 ERP 规模下的性能优势 |
| **企业级部署** | 需要类型安全、IDE 支持、成熟的测试框架 |
| **长期可维护性** | 模型优先的真相源确保代码与模型一致 |

### 混合策略建议

两者并非完全对立。以下是推荐的混合方案（**注意**：混合方案涉及跨平台 API 集成、数据同步、会话/权限状态统一等工程开销，需评估实际整合成本）：

| 层次 | 建议 | 理由 |
|------|------|------|
| **核心业务逻辑** | Nop Platform（Java） | 需要事务安全、审计、编译期校验 |
| **快速原型/配置界面** | NocoBase（前端） | 利用 WYSIWYG 页面设计器快速构建 UI |
| **外部系统集成** | NocoBase DataSource | 多数据源代理模式更适合异构系统 |
| **工作流** | 各取所长 | 简单 CRUD 触发器用 NocoBase Workflow；复杂多步审批用 Nop XWF |
| **权限系统** | Nop Platform | 三层权限体系更适合企业级需求 |

### 相互借鉴点

**Nop Platform 可向 NocoBase 学习**：
1. **可视化工作流设计器**——将 `.xwf` XML 的配置体验提升为图形化拖拽
2. **运行时字段配置**——在不修改 orm.xml 的情况下提供运行时扩展字段（类似 NocoBase 的 Field 动态增删）
3. **WYSIWYG 页面设计器**——为 AMIS view.xml 提供可视化编辑器
4. **插件生命周期管理**——引入 enable/disable 机制，减少重启需求
5. **多数据源抽象**——外部的 legacy DB / REST API 数据源代理

**NocoBase 可向 Nop Platform 学习**：
1. **正式 Delta 定制机制**——在 Schema 级别引入差量合并（而非完全替换）
2. **事务性业务逻辑抽象**——类似 `@BizMutation` 的声明式事务包装
3. **编译期/启动期校验**——在 Schema 加载时捕获更多类型错误
4. **预建 ERP 领域模型**——作为模板/种子插件发布，降低起步成本
5. **GraphQL 支持**——在 REST 之外提供 GraphQL 端点

---

## 证据索引

| 声明 | 证据源 |
|------|--------|
| NocoBase Collection 运行时构建 | `collection.ts:154-190` — 构造函数 `modelInit()` + `setFields()` |
| NocoBase 插件生命周期 | `plugin.ts:119-139` — 11 个生命周期钩子 |
| NocoBase 插件加载顺序 | `plugin-manager.ts:428-472` — `beforeLoad()` → `loadCollections()` → `load()`（按注册顺序；运行时 enable 使用 `@hapi/topo` 拓扑排序） |
| NocoBase ACL 引擎 | `acl.ts:75-249` — `can()` 方法、角色+资源+动作评估 |
| NocoBase 工作流 Trigger 类型 | `CollectionTrigger.ts:27-32` — mode bitmap（CREATE/UPDATE/DESTROY） |
| NocoBase 工作流执行 | `Processor.ts:48-80` — jobsMap + DAG 执行 |
| NocoBase REST 路由 | `resourcer.ts` — 自动 CRUD 端点 |
| NocoBase 插件清单 | `presets/nocobase/package.json` — 108+ 插件 |
| Nop 模型优先开发 | `model-first-development.md:5-12` — 先模型→再生成→再补保留层 |
| Nop Delta 定制 | `delta-customization.md:3-13` — `_vfs/_delta/` + `x:extends` |
| Nop 模块链 | `domain-module-pattern.md:5-8` — model→codegen→dao→meta→service→web→app→api |
| Nop 统一 API 入口 | `api-and-graphql.md:14-38` — `IGraphQLEngine` 统一 5 种协议 |
| Nop 三层权限 | `auth-and-permissions.md:3-10` — HTTP 路径/操作/数据 |
| Nop 工作流 XWF | `workflow-configuration.md:5-27` — step + transition + actions |
| Nop 页面三层 Delta | `view-and-page-customization.md:20-31` — xmeta→_gen→view.xml→page.yaml |
| Nop BizModel 规则 | `service-layer.md:13-21` — `CrudBizModel<T>`、`@BizQuery`/`@BizMutation` |
| nop-app-erp 19 域 | `domain-module-split-analysis.md:46-69` — 18+1 域完整映射表 |
| nop-app-erp 标准字段 | `domain-design-guidelines.md:431-508` — orgId/businessDate/posted/多币种 |
| nop-app-erp 跨域映射 | `domain-design-guidelines.md:589-614` — 19 条跨域状态映射 |
| nop-app-erp 项目阶段 | `project-context.md:30-35` — 业务逻辑深化与运营成熟度收尾阶段 |
