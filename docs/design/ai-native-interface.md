# AI 原生接口层设计（AI-Native Interface Layer）

## 定位

本文设计 nop-app-erp 面向 AI 原生架构的**接口层**：在不改变既有 BizModel/Processor 架构的前提下，定义 AI 如何安全地读写业务数据、如何获得人工确认门、如何被暴露为可编程工具。**当前阶段只产出设计与分析，不进入编码状态**（2026-08-12 用户指示）。

## 来源与背景

- 2026 年开源 ERP 创新趋势「AI 原生架构」：ERPClaw 定义「action 层即 API + 不可变 GL」，Twenty 将 Agents/Workflows 作为一等公民，n8n 提供 human-in-the-loop 审批门。
- 参考报告：`docs/analysis/erp-survey/2026-08-12-0000-erpclaw.md`、`2026-08-12-0000-twenty.md`、`2026-08-12-0000-n8n.md`、`2026-08-12-0000-innovation-trends.md` §1.1。
- 平台边界：`nop-ai` 提供 LLM 接入（平台能力，非应用层缺口）；**nop-datav 平台全链（含 ChatBI：NL→数据集查询/看板生成/大屏生成）已合入 `../nop-entropy` master**（2026-08-26 活仓核实，见 `dashboard-semantic-layer.md` §0）——平台侧 AI 消费 BI 能力的现成形态（不依赖 MCP）；本文只管 ERP 业务面的 AI 消费/暴露设计。

## 现状基线（nop-app-erp 已具备）

| 能力 | 现状 | 文档 |
|------|------|------|
| action 层即 API | 所有 `@BizMutation`/`@BizQuery` 自动暴露为 GraphQL（`IGraphQLEngine`）；BizModel = API 入口 | `../nop-entropy/docs-for-ai/02-core-guides/api-and-graphql.md` |
| 权限强制 | action 级 RBAC + data 级 row-filter + SoD 守卫（enforcement 栈） | `roles-and-permissions.md`、permissions-enforcement-roadmap |
| 审计 | 会计日志 + 运行监控 + 审计轨迹（Actionlog 类机制见 assets 域设计） | `posting-log.md`、`docs/design/assets/audit-trail-and-custom-fieldsets.md` |
| 人工门 | `use-approval` 审批流（DIRECT/WORKFLOW）+ nop-wf 人工节点 | `approval-framework.md`、`wf-integration-design.md` |

**结论**：ERPClaw/Twenty 的「action 层化」nop 已天然具备；缺口在 **AI 消费/暴露形态**与 **AI 安全护栏**。

## 设计要点

### 1. AI 工具暴露形态（不使用 MCP；GraphQL 类型定义即 API）

> **平台裁决（2026-08-12 用户澄清）**：应用系统提供给 AI 的接口**不通过 MCP**，整个 Nop 平台也**均不使用 MCP**。API 由 **GraphQL 类型定义**描述，经 **REST 与 GraphQL 两种方式**调用。MCP 相关设计一律否决。

- **目标**：让 AI 助手经标准协议调用 BizMethod。
- **裁决（否决 MCP）**：候选 A（MCP server 包装 BizModel action，对照 ERPClaw `mcp/tool_router.py`）与候选 B（平台 `nop-ai` MCP 适配）**均否决**——平台与应用层均不使用 MCP。
- **选定形态**：**GraphQL 类型定义即 API 描述**——
  - API 契约 = BizModel 的 GraphQL 类型定义（schema：action 名、入参类型、返回类型、文档/描述），经 `IGraphQLEngine` 自动生成并对外提供。
  - **调用通道双轨**：REST 通道 + GraphQL 通道（Nop 平台均支持，`../nop-entropy/docs-for-ai/02-core-guides/api-and-graphql.md`）；AI 助手（或任何消费者）经任一通道调用同一 action 层。
  - AI 工具发现 = GraphQL schema introspection/类型定义（工具名 = `bizObjName.action`，输入 = action 参数类型，输出 = 返回类型），无需额外协议包装。
- **不新建**：不创建 AI 专用业务方法（业务逻辑单一真相在 Processor；AI 与用户共用同一 action 层，避免双轨）。

### 2. AI 安全护栏（参考 ERPClaw per-invocation 确认 + n8n approvals）

| 护栏 | 设计 | 对照现状 |
|------|------|----------|
| 状态变更确认门 | AI 发起的状态变更 action 需要显式确认标记（per-invocation），AI 无法静默绕过 | 可复用既有审批流（`use-approval`）作为高影响 action 的门；低影响 action 经 `action-auth.xml` 白名单 |
| 能力边界 | 只读查询默认开放；写操作按 enforcement 栈既有规则（RBAC + SoD + 数据权限） | 已具备，无需新机制 |
| 操作审计 | AI 发起的每次 action 记录完整输入输出 + 业务实体变化 | 复用会计日志/审计轨迹，增加 `actorType=AI` 标识字段（设计预留；**落地裁决=暂不落地**，触发条件与依据见 §前置调研结论，`erp-enhancement-roadmap.md` §8.1 E3.6 授权行未使用） |
| 限流/频控 | 防止 AI 批量误操作 | 复用既有 `IRateLimiter`（D1 已落地） |

### 3. human-in-the-loop 门（参考 n8n approvals 节点 / Medusa 长流程）

- AI 工作流中的人工确认点 = nop-wf 人工步骤（`NopWfStepInstance`/`NopWfWork` 表承载待办）——平台已具备。
- 设计：AI 编排场景下，人工确认动作复用既有 wf 回调机制（`wf-integration-design.md` 三层桥接），不新建审批路径。
- 与跨域流程编排的关系：见 `docs/architecture/cross-domain-flow-orchestration.md`（同为分析+设计，暂不编码）。

### 3.5 平台 AI 消费能力边界（2026-08-26 活仓核对）

- **ChatBI 归属平台**：nop-datav 全链（api/app/codegen/core/dao/meta/service/web + DataAuth/RbacAuth/审计实体）**已合入 `../nop-entropy` master**（含 ChatBI：NL→数据集查询/看板生成/大屏生成）——AI 消费 BI/可视化能力的现成平台形态；应用层不重复实现，只经 GraphQL/REST 调用（挂载触发条件见 `dashboard-semantic-layer.md` §0：应用层运行时配置化 KPI/外部嵌入/挂载真实需求出现）。
- **本文作用域**：ERP 业务面（BizMethod action 层 + 护栏 + 门）；平台 AI 基建（nop-ai LLM 接入、nop-datav ChatBI、nop-metadata 语义层）与应用层的关系以「外挂化消费」为准，不交叉实现。

### 4. AI 原语化 vs 外挂化（参考 Twenty）

- Twenty 将 Agents/Workflows 作为 CRM 一等公民；ERPClaw 以 AI 助手为主交互界面。
- **裁决**：nop-app-erp 采用「外挂化但接口友好」——AI 助手作为独立消费者走统一 action 层（不变更业务模型），优先保证 action 元数据完整（工具描述、参数 schema 的可生成性调研归 E3.6 前置；`business-module-metadata.md` 现有 version/businessDependencies/optionalFeatures 模块元数据为雏形）。理由：不破坏 18 域既有架构；AI 界面形态演进快，不宜固化进业务域。

## 前置调研结论（2026-08-26，E3 整体计划 Phase 1）——「最小落地集」冻结

- **introspection 可用性（活仓核验）**：平台自研 GraphQL 引擎支持完整 introspection（`__Schema/__Type/__Field` 等 spec 类型 + 字段/参数/枚举值级 description），经 config `nop.graphql.schema-introspection.enabled` 门控，**默认 false**（应用当前显式关闭）。描述元数据来源：实体字段从 orm/xmeta displayName/description 富覆盖（中文），Java action 从 `@Description` 注解，xbiz 从 displayName，读时经 i18n 解析。另：`nop.debug=true` 时启动即 dump 完整 schema（`/_dump/nop/main/graphql/schema.graphql`），应用已开启——离线发现通道现成。
- **schema 规模与质量缺口**：应用侧声明操作 804 个（260 @BizQuery + 546 @BizMutation，另有 CRUD 内建），实体类型描述完整，但**应用自定义 action 零 `@Description`**（grep 0 命中）——action 级发现描述是主要缺口。
- **双通道一致性（静态核实）**：REST 通道 `/r/{bizObj__action}`（GET/POST，`QuarkusGraphQLWebService`）与 GraphQL `/graphql` 经同一 `IGraphQLEngine`（`initRpcContext + executeRpc`）分发——通道一致性由共享引擎保证，行为级验证以 E2E 断言落地。
- **平台桥登记（平台优先，不重建）**：平台 `nop-ai-tools` 自带 `GraphQLToolProvider`（任意 `bizObj__action` 包装为 AI function tool：description + JSON Schema 输入 + 引擎执行）——平台级 GraphQL→AI 工具桥已存在，应用层不重复实现、不强制启用。
- **actorType=AI 裁决（不落地）**：最小落地集不含 AI 主动发起的写操作路径；AI 服务账号以独立 userId 进入 `IServiceContext`，既有审计（会计日志/审批记录/保密字段读审计）已记录操作者身份，足以区分调用者。`actorType` 差异化仅在需要按「AI vs 人」差异化护栏策略时有意义——触发条件（重新打开 §8.1 E3.6 授权行）= 首个 AI 服务账号投产或差异化限流/审批策略需求出现。
- **「最小落地集」清单（Phase 7 冻结验收基准，执行期不得增删）**：
  1. introspection config-gate 验证：JUnit 证明开启后 IntrospectionQuery 可用且含 description（默认保持关闭）。
  2. action 描述补全（代表性覆盖）：11 个 `getDashboardKpi` action 补 `@Description`（IBiz 同步）——AI 只读消费面范本；「新增对外 action 应带 @Description」约定登记。
  3. REST/GraphQL 双通道一致性：E2E 同一 operation（`getDashboardKpi`）经 `/graphql` 与 `/r/` 两通道数值一致断言。
  4. 护栏：高影响 action 门径复用验证——无权限角色经 `/r/` 通道调用高影响 mutation 被拒（负路径 E2E，复用 permissions 账号池范式）；限流护栏复用 `IRateLimiter` 的接线落点 = E3.5 管道入口（自动化批量面，防 AI/自动化批量误操作），人类用户面默认无限流；审批门对 AI 通道复用既有审批流（AI 无法静默绕过，既有审批测试为证）。
  5. 审计标识断言落为「调用方身份（userId）落账断言」（actorType 不落地，见上）。
  6. 平台能力登记：`GraphQLToolProvider` 存在性与复用边界登记（本文档 + `business-module-metadata.md` 雏形关系）。

## 落地策略（分阶段）

| 阶段 | 内容 | 状态 |
|------|------|------|
| 设计 | 本文档（GraphQL 类型定义即 API + REST/GraphQL 双通道 + 护栏 + 门 + 原语化裁决；**否决 MCP**） | ✅ 已完成（本批次） |
| 前置调研 | GraphQL schema 对 AI 工具发现的可用性（introspection/类型描述完整性）；action 元数据生成可行性 | ✅ 已完成（2026-08-26，结论与最小落地集见上节） |
| 实现 | 最小落地集 6 项随 E3 整体计划实施（`2026-08-26-0735-2` Phase 7，按冻结清单逐项验收）；actorType 字段不落地（触发条件见上） | ✅ done（E3.6，2026-08-27：冻结 6 项逐项落地零增删——introspection config-gate JUnit 双证 / 11 域 `getDashboardKpi` `@Description` 补全 / 双通道一致性 E2E / 护栏负路径 E2E + `IRateLimiter` 接线 E3.5 管道入口 / 调用方身份落账断言 / `GraphQLToolProvider` 平台登记（本文档 + `business-module-metadata.md` §6.2）；零 MCP） |

## 反模式自检表

| # | 反模式 | 正确做法 |
|---|--------|----------|
| AP-1 | 为 AI 新建一套业务 API（双轨） | AI 与用户共用 BizMethod action 层 |
| AP-2 | AI 可静默执行状态变更 | 高影响 action 经既有审批门/确认门 |
| AP-3 | AI 写入绕过 enforcement | 写操作走既有 RBAC + SoD + 数据权限 |
| AP-4 | 把 AI 界面形态固化进业务模型 | 外挂化消费，业务模型不变 |
| AP-5 | 引入 MCP 包装/适配（应用层或平台层） | **否决**：API = GraphQL 类型定义，经 REST + GraphQL 双通道调用（用户 2026-08-12 裁决） |

## 相关文档

- `docs/analysis/erp-survey/2026-08-12-0000-{erpclaw,twenty,n8n}.md` — 参考报告（MCP 仅为参考项目形态，本平台否决采用）
- `docs/architecture/api-response-conventions.md` — API 响应约定；`../nop-entropy/docs-for-ai/02-core-guides/api-and-graphql.md` — GraphQL/REST 双通道调用（平台文档）
- `docs/architecture/business-module-metadata.md` — 模块元数据（AI 工具发现的雏形）
- `docs/architecture/cross-domain-flow-orchestration.md` — 跨域流程编排（同批分析+设计）
- `docs/design/dashboard-semantic-layer.md` — 看板语义层（ChatBI 平台归属 + MCP 否决，同批姊妹主题；nop-datav master 实态见其 §0）
- `../nop-entropy` master `nop-datav` 模块 — 平台 ChatBI（D6-1/D6-1b/D6-2，全链已合入 master，2026-08-26 活仓核实；应用层未挂载，边界见上 §3.5）
- `docs/backlog/erp-enhancement-roadmap.md` — 本主题 roadmap（E1 设计补充 / E3 实现门控）