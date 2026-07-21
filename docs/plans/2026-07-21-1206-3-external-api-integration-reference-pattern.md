# 2026-07-21-1206-3-external-api-integration-reference-pattern D1 — External API Integration Reference Pattern（外部 API 集成参考模式）

> Plan Status: active
> Last Reviewed: 2026-07-21
> Source: `docs/backlog/deepening-roadmap.md` §Milestone D §D1（line 73/89 — External API Integration Reference Pattern：auth pattern + rate limiting + API client lifecycle + reference implementation plan；**§6 明确 D1 ORM 变更=否，仅为参考文档**）；`docs/analysis/erp-survey/2026-07-20-post-survey-strategic-gaps.md` §3.2（line 93-115 — Reference value from Wimoor `ApiBuildService`：标准化外部集成层参考架构；line 111 显式标注 "Reference design document (not standard SPI)"）
> Related: `docs/architecture/integration-pattern.md`（既有 43 行，仅覆盖 webhook 出站/入站 → 通用外部 API 集成参考模式在本计划 **NEW** 文档 `external-api-integration-pattern.md` 中落地；既有文档末尾增交叉引用段避免断链，**EXPAND-vs-NEW Decision 见 Phase 0 Explore (e)**）；`docs/architecture/b2b-integration.md`（B2B EDI 集成契约层 — 本计划提供通用 API 集成参考，与之相互引用；既有实现为 `IErpB2bEdiProvider` + `UblInvoiceEdiProvider`/`UblDespatchAdviceEdiProvider`）；`docs/architecture/integration-and-transaction-patterns.md`（集成与事务模式 — 与本计划事务边界段相关）；`docs/architecture/idempotency-pattern.md`（幂等模式 — API 重试与幂等性引用）；`docs/design/master-data/exchange-rate-management.md`（参考实现接入的 owner doc）；`docs/analysis/erp-survey/2026-07-20-0000-wimoor-compare.md`（Wimoor ApiBuildService 对比，line 121-166）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-21，对 integration-pattern.md 既有 + b2b-integration.md + b2b SPI 实测 + notify 模块 ORM + master-data exchange-rate-management.md + Wimoor 对比报告扫描）：

### 已落地的集成基础设施（实测核实）

| 文档/能力 | 路径 | 实测覆盖 | 缺失（D1 范围）|
|----------|------|----------|----------------|
| `integration-pattern.md` | `docs/architecture/integration-pattern.md` | **43 行**，webhook 出站/入站（事件类型 + 签名验证 + 幂等 + IP 白名单）—— 但 webhook 部分本身也仅为文档约定，**无对应实体落地** | 通用第三方 API 集成模式（auth 类型 + rate limiting + endpoint registry + client lifecycle）|
| `b2b-integration.md` | `docs/architecture/b2b-integration.md` | B2B EDI 集成契约层（EDI 格式 SPI 派发 + 信封状态机 + ASN 入站） | 引用通用 API 集成参考（与本计划相互引用）|
| `integration-and-transaction-patterns.md` | `docs/architecture/integration-and-transaction-patterns.md` | 集成与事务边界（同步/异步/最终一致） | API 客户端事务边界具体实施 |
| `idempotency-pattern.md` | `docs/architecture/idempotency-pattern.md` | 幂等键 + 重复请求处理 | API 客户端重试与幂等具体实施 |
| **Nop Platform GraphQL driver** | `../nop-entropy/docs-for-ai/` | 第三方 GraphQL API 作为 ORM 数据源（读路径） | 写路径 + 非 GraphQL 第三方 API（REST/SOAP/EDI）参考 |
| **Nop Platform `xpl` 标签库** | `../nop-entropy/docs-for-ai/` | HTTP 客户端封装能力 | 标准化封装范式 / 共享 xpl 库 |
| **Nop Platform IoC `@Inject`** | `../nop-entropy/docs-for-ai/` | 按 bean 名称注入（无 SPI needed for known impl） | API client bean 命名约定 + 多实现切换 |
| **Nop Platform 安全层** | `../nop-entropy/docs-for-ai/` | auth token 处理 | 标准 auth pattern 范式（OAuth2/API Key/LWA） |
| **Nop Platform dict API** | nop-dict | 运行时字典覆盖/补充（无需重启） | 提供运行时配置能力，可作为 endpoint registry 的轻量替代（per Phase 0 Explore (b) 候选 C） |

### 已落地的相关先例（应用层 — 实测核实）

- **logistics 域 Carrier Gateway**（既有，实测）：`IErpLogCarrierGatewayClient` SPI + `MockCarrierGatewayClientFactory` 实现（plan `2026-07-14-0508-1` 测试）；config-gated（`erp-log.webhook-signature-required`）；这是**应用层第三方 API 客户端的既有先例**——本计划将其抽象为通用参考模式。
- **b2b 域 EDI 格式 SPI**（既有，实测）：实际 SPI 是 `IErpB2bEdiProvider`（**非** `IErpB2bEdiGatewayClient`——此名不存在）+ `ErpB2bEdiRegistry` 收集器（启动时按 `IErpB2bEdiProvider.getCode()` 建立 formatCode→Provider 查找表）+ `UblInvoiceEdiProvider`/`UblDespatchAdviceEdiProvider` 实现（按 EDI 报文类型派发，而非 Client/Factory/Registry 三层结构）；**注意** `IErpB2bEdiFormatBiz` 是 entity CrudBiz 接口（非 SPI，与 `IErpB2bEdiProvider` 是不同类型）；本计划文档化此 SPI 范式但**不**强行归纳为与 logistics 同构（承认两域范式异构性，各自适用场景不同）。
- **notify 域 Webhook 表**：`docs/architecture/integration-pattern.md` 引用 `ErpSysWebhookConfig`/`ErpSysWebhookLog`，但**实测 notify 模块 ORM 仅有 `ErpSysNotificationTemplate`/`ErpSysNotification`/`ErpSysNotificationRead` 三实体**，webhook 配置实体尚未落地。本计划在 owner doc 中如实记录此文档/代码漂移。
- **既有 webhook 出站事件**（设计层）：integration-pattern.md 描述的 webhook 范式为设计约定，应用层尚未实体化；本计划提供通用 endpoint 配置范式参考（候选 C：yaml/dict，不建实体）。

### 待深化差距（D1 范围）

| 差距 | 现状 | D1 目标 |
|------|------|---------|
| **第三方 API 集成参考模式文档** | `integration-pattern.md` 43 行仅覆盖 webhook（且 webhook 表本身未实体化） | **NEW** `external-api-integration-pattern.md` ~300 行，覆盖 OAuth2/API Key/LWA + rate limiting + endpoint 配置范式 + client lifecycle（既有 integration-pattern.md 末尾增交叉引用段）|
| **endpoint 配置范式（候选 C）** | 无 | yaml config + Nop Platform 运行时 dict（轻量配置 + 运行时变更）—— **不**新建实体（D1 ORM 变更=否）|
| **标准 auth pattern 范式** | 散落在 logistics 域实现内 | 文档化 `IErpExternalApiAuthProvider` 参考接口（**参考，非强制 SPI**）+ OAuth2/ApiKey/LWA 3 标准流程描述 |
| **rate limiting 标准方案** | 无（每域自实现）| 令牌桶算法 per-tenant/per-key，使用 platform `Cache` 或应用层 ConcurrentHashMap；Guava RateLimiter 推荐 |
| **API client lifecycle 范式** | 散落在 logistics 域 | 标准化创建/健康检查/熔断/恢复 4 阶段范式（文档化 + 反模式自检表）|
| **参考实现（1 场景）** | logistics 是已有实现 | 1 个轻量新参考（master-data 汇率查询 API 客户端）作为完整 client lifecycle 文档化演示 |
| **Wimoor `ApiBuildService` 对照** | 仅 compare 报告提及 | 本计划 owner doc §对照段总结可借鉴的设计（参考架构差异 + 可借鉴 vs 不借鉴）|

### 关键风险/缺口

- **平台能力 vs 应用层重复**：Nop Platform 已提供 GraphQL driver + xpl HTTP 封装 + IoC + 安全层 + dict API；本计划**不**替代平台能力，而是提供**应用层参考模式**（何时用 GraphQL driver / 何时用 xpl / 何时用 Java HttpClient + @Inject）。
- **B2B/logistics 既有实现不被重构**：本计划是参考模式文档化，不重构已有 `IErpLogCarrierGatewayClient` / `IErpB2bEdiProvider` 实现；既有实现作为参考案例引用。
- **D1 ORM 变更=否（deepening-roadmap.md §6 line 89 明确）**：本计划**不**新建 `ErpSysExternalSystem` 实体；endpoint 配置走 yaml + 运行时 dict 路径（候选 C 裁决）。如未来需要持久化 endpoint registry，独立 successor plan + 显式 ORM 授权。
- **本计划是文档为主 + 1 个轻量参考实现**：strategic gap plan §3.2 line 111 明确是「Reference design document (not standard SPI)」；本计划尊重此边界，参考实现仅作为完整 client lifecycle 文档化演示。
- **`IErpExternalApiAuthProvider` 边界**：作为 owner doc §3 的**参考接口**描述（非强制 SPI），不强制全域统一实现；如业务客户需要跨域统一 auth 模式，触发 successor 升级为标准 SPI（owner doc 授权）。
- **rate limiting 跨节点一致性**：单节点 Guava RateLimiter 简单但多节点不一致；多节点需 Redis 或 platform `Cache`。本计划提供两种实现参考，默认单节点 + successor 触发多节点 Redis。
- **OAuth2 token 刷新复杂度**：OAuth2 access_token 过期 + refresh_token 刷新流程复杂；本计划文档化标准流程 + 不强制实现（业务客户接入时再实施）。

## Goals

1. **NEW owner doc**：`docs/architecture/external-api-integration-pattern.md`（**NEW**，~300 行，含 8 大段 + EXPAND-vs-NEW Decision 记录 + 反模式自检表；既有 `integration-pattern.md` 末尾增交叉引用段保留 webhook 主题不破坏既有引用）。
2. **归纳既有应用层先例为通用参考模式**：将 logistics Carrier Gateway（Client/Factory/Registry 三层）+ b2b EDI Format（Format/Provider 按类型派发）两种范式抽象为通用参考文档，承认两域范式异构性，各自适用场景说明。
3. **endpoint 配置候选 C 落地（yaml + 运行时 dict）**：**不**新建实体（D1 ORM 变更=否）；通过 yaml config + Nop Platform 运行时 dict API 提供配置能力（运行时变更无需重启）。
4. **1 个轻量参考实现**：master-data 域汇率查询 API 客户端（`IErpMdExchangeRateApiClient` SPI + Mock 实现 + Factory + config-gated 接入 exchange-rate-management.md 的 `refreshRates` mutation）作为完整 client lifecycle 文档化演示。
5. **既有 owner doc 回链**：`b2b-integration.md` + `integration-and-transaction-patterns.md` + `idempotency-pattern.md` 各增「通用 API 集成参考」段。
6. **Wimoor `ApiBuildService` 对照段**：owner doc 增「与 Wimoor ApiBuildService 对照」段（参考架构差异 + 可借鉴 vs 不借鉴的设计）。
7. **测试基线**：master-data service 既有测试不回归；新增 `TestErpMdExchangeRateApiClient`（Mock 实现 + rate limiting 验证 + 重试幂等）。
8. **roadmap 同步**：`deepening-roadmap.md` §D1 状态 `todo → done` + §8.5 落地证据段落。
9. **解锁 D4 Plugin Hot Management Research**：D1 是 D4 的前置（per deepening-roadmap.md §7 依赖图 line 101 mermaid edge `D1 --> D4`），D4 可在 D1 完成后启动。

## Non-Goals

- **替代 Nop Platform 平台能力**—— 本计划是应用层参考模式，不替代 GraphQL driver / xpl / IoC / 安全层 / dict API；文档显式标注何时用平台能力 vs 何时用应用层封装。
- **新建 `ErpSysExternalSystem` 实体**—— deepening-roadmap.md §6 line 89 明确 D1 ORM 变更=否；本计划走候选 C（yaml + 运行时 dict）；如未来需要持久化 endpoint registry，独立 successor plan + 显式 ORM 授权。
- **重构 logistics/b2b 既有实现**—— 既有 `IErpLogCarrierGatewayClient` / `IErpB2bEdiProvider` 是已经过验证的实现；本计划仅将其作为参考案例引用，**不**重构。
- **强制 `IErpExternalApiAuthProvider` 标准 SPI**—— owner doc §3 仅作参考接口描述；不要求全域统一实现；如业务客户需要跨域统一 auth 模式，触发 successor。
- **D4 Plugin Hot Management Research**—— D4 是独立研究项（P3 可行性研究），不属本计划范围；D1 完成后 D4 可启动。
- **D2 Business Module Metadata (BT5-style)**—— 模块元数据与外部 API 集成是不同主题；D2 是独立 plan。
- **D3 Cost Calculation Sub-Calculator Injection**—— 成本计算子计算器注入是 finance 内部模式，与外部 API 集成不同主题。
- **OAuth2 完整实现**—— OAuth2 access_token + refresh_token 流程复杂；本计划仅文档化标准流程 + 不强制实现。
- **第三方 API SDK 自动生成**—— 从 OpenAPI/Swagger 规范自动生成 API client SDK 属 successor。
- **API gateway 反向代理**—— API gateway（如 Kong/APISIX）反向代理属部署架构层，不属应用层集成参考。
- **API 监控/可观测性完整方案**—— API 调用监控 + 链路追踪 + 指标采集属 observability successor；本计划仅在 client lifecycle 段简述。
- **API 安全审计**—— API 调用日志审计 + 合规要求（如 GDPR 数据访问审计）属 security/compliance successor。
- **跨境数据合规**—— 跨境 API 数据传输合规（如欧盟 GDPR / 中国数据安全法）属 l10n/legal successor。
- **notify 域 webhook 表实体化**—— integration-pattern.md 引用的 `ErpSysWebhookConfig`/`ErpSysWebhookLog` 实体化属 notify 域 successor；本计划在 owner doc 如实记录文档/代码漂移，不修复（独立 successor 触发条件：notify 域 webhook 接入具体业务需求）。

## Task Route

- Type: `architecture change`（扩展跨领域技术 owner doc + 1 轻量参考实现）
- Owner Docs:
  - `docs/backlog/deepening-roadmap.md` §D1（line 73/89）
  - `docs/architecture/external-api-integration-pattern.md`（**NEW** 主交付 — 本计划 Phase 0 落地）
  - `docs/architecture/integration-pattern.md`（既有，末尾增交叉引用段指向 NEW 文档）
  - `docs/architecture/b2b-integration.md`（增「通用 API 集成参考」段）
  - `docs/architecture/integration-and-transaction-patterns.md`（增「API client 事务边界」段）
  - `docs/architecture/idempotency-pattern.md`（增「API client 重试与幂等」段）
  - `docs/design/master-data/exchange-rate-management.md`（汇率查询 API 客户端接入段）
  - `../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md`（平台 HTTP 客户端辅助工具）
- Skill Selection Basis: 加载 `nop-backend-dev`（SPI + BizModel + IBiz + 跨实体 + config-gated + Mock 工厂范式）；Phase 3 view.xml 不涉及（无新实体 → 无 codegen → 无 view.xml 定制需求，仅文档变更）；不加载 `nop-testing`（既有测试范式直接复用）。最终：`nop-backend-dev`。

## Infrastructure And Config Prereqs

- 本地运行：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- **关键 config**（参考实现 + 通用范式，全部走 yaml + 运行时 dict）：
  - `erp-md.exchange-rate-api-enabled`（默认 `false`）— 汇率查询 API 客户端启用开关
  - `erp-md.exchange-rate-api-provider`（默认 `mock`）— provider 切换（mock/exchangerate-host/fixed-fetch）
  - `erp-md.exchange-rate-api-key`（默认空）— API Key 配置
  - `erp-md.exchange-rate-api-rate-limit-rps`（默认 `10`）— 每秒请求数限制
- webServer JVM args（E2E 测试时）：追加 `erp-md.exchange-rate-api-enabled=true` 启用参考实现测试
- 无新 ORM 实体 / 无新 DB 表（D1 ORM 变更=否）

## Execution Plan

### Phase 0 — Explore + Owner Doc 扩展 + 关键 Decision

Status: planned
Targets: `docs/architecture/external-api-integration-pattern.md`（**NEW**）+ `docs/architecture/integration-pattern.md`（既有，末尾增交叉引用段）+ plan 内 Decision 记录
Skill: `nop-backend-dev`

- Item Types: `Explore | Decision | Add`
- Prereqs: deepening-roadmap.md D1 todo + integration-pattern.md 既有 + Wimoor 对比报告

- [ ] `Explore` (a)：Nop Platform 集成能力边界核实。
  - 核实范围：`../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md` 的 HTTP 客户端辅助工具；`xpl` 标签库的 HTTP 封装；GraphQL driver 第三方 API 接入；IoC `@Inject Map<String, IExternalApiAuthProvider>` 多实现注入；安全层 auth token 处理；`nop-dict` 运行时 dict 覆盖能力（是否支持运行时配置变更无需重启）。
  - 输出：平台能力清单 + 何时用平台 vs 何时用应用层封装的决策树，入 owner doc §平台能力边界。
  - Skill: `nop-backend-dev`
- [ ] `Explore` (b)：endpoint 配置方案裁决候选评估（**全部不建实体**）。
  - 候选 A：新建 `ErpSysExternalSystem` 实体（**deepening-roadmap.md §6 line 89 显式禁止 ORM 变更 → 排除**）。
  - 候选 B：扩展 notify 域 webhook 表（**该表本身未实体化 → 排除**）。
  - 候选 C：纯文档参考 + yaml config + Nop Platform 运行时 dict（运行时变更无需重启）。
  - 核实范围：`nop-dict` API 是否提供运行时覆盖能力（无需重启）；yaml config 的运维便利性；与 D2 Business Module Metadata 的潜在重叠。
  - 输出：候选 C 裁决（A/B 已排除）+ yaml + dict 协同设计，入 owner doc §endpoint 配置范式。
  - Skill: `nop-backend-dev`
- [ ] `Explore` (c)：参考实现候选场景评估。
  - 候选 1：master-data 域汇率查询 API 客户端（接入 `exchange-rate-management.md` 的 `refreshRates` mutation）。
  - 候选 2：logistics 域 carrier tracking API 客户端（接入既有 `IErpLogCarrierGatewayClient`，但已有实现 → 重叠）。
  - 候选 3：b2b 域 EDI gateway API 客户端（同上，已有实现 → 重叠）。
  - 候选 4：finance 域银行汇率 API 客户端（与候选 1 类似但更复杂，需銀企直连）。
  - 核实范围：候选 1 的业务价值（exchange-rate-management.md 既有自动汇率刷新需求）；实现复杂度（公开 API like exchangerate.host 简单 / 銀企直连复杂）；与既有实现的重叠度。
  - 输出：候选场景选择裁决 + 理由。
  - Skill: `nop-backend-dev`
- [ ] `Explore` (d)：rate limiting 实现方案裁决候选评估。
  - 候选 A：单节点 ConcurrentHashMap + 时间窗口算法。
  - 候选 B：单节点 Guava RateLimiter（令牌桶）。
  - 候选 C：多节点 Redis-based rate limiter（如 Redisson）。
  - 候选 D：Nop Platform Cache（如支持原子操作）。
  - 核实范围：既有项目内 rate limiting 使用情况（grep `RateLimiter`/`rate-limit`/`Semaphore`）；Nop Platform Cache 的原子操作能力；多节点部署计划。
  - 输出：4 方案权衡表 + 默认方案裁决 + 多节点 successor 触发条件。
  - Skill: `nop-backend-dev`
- [ ] `Explore` (e)：EXPAND 既有 owner doc vs NEW 新文档 Decision。
  - 候选 EXPAND：扩展 `integration-pattern.md` 既有 43 行 → ~300 行（保持单一文档 + 既有 webhook 段保留作 §10 + 既有引用不断链）。
  - 候选 NEW：新建 `external-api-integration-pattern.md` + 既有 integration-pattern.md 保留 webhook-only 并交叉引用（与 deepening-roadmap.md line 73 / post-survey-strategic-gaps.md line 467/491 显式标注 NEW 一致）。
  - 核实范围：roadmap §D1 line 73 显式标注 "**NEW**"（与 EXPAND 矛盾）；既有 integration-pattern.md 是否被其他文档引用（grep）；NEW 文档与既有 webhook-only 文档的边界划分。
  - 输出：EXPAND-vs-NEW Decision + 理由（如选 EXPAND 需解释为何偏离 roadmap；如选 NEW 需规划两文档边界）。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：基于 Explore (a)~(e)，确定 D1 实现方式。
  - **平台能力边界**（裁决依据 Explore a）：文档化决策树「何时用 GraphQL driver（读第三方 GraphQL API）/ 何时用 xpl HTTP 封装（简单一次性调用）/ 何时用 Java HttpClient + @Inject SPI（复杂业务逻辑 + 多 provider 切换）」。
  - **endpoint 配置**（裁决依据 Explore b）：选 **候选 C（纯文档参考 + yaml config + Nop Platform 运行时 dict）**。理由：(i) deepening-roadmap.md §6 line 89 显式禁止 ORM 变更；(ii) notify 域 webhook 表本身未实体化；(iii) Nop Platform dict API 提供运行时覆盖能力（无需重启）；(iv) yaml 提供编译期配置 + dict 提供运行时覆盖，双层覆盖满足大部分需求。
  - **参考实现**（裁决依据 Explore c）：选 **候选 1（master-data 汇率查询 API 客户端）**。理由：(i) 业务价值明确（exchange-rate-management.md 既有自动汇率刷新需求）；(ii) 实现复杂度低（公开 API like exchangerate.host 免费免认证）；(iii) 与既有 logistics/b2b 实现无重叠（不同域 + 不同 SPI）；(iv) 配套文档演示完整 client lifecycle。
  - **rate limiting 实现**（裁决依据 Explore d）：选 **候选 B（单节点 Guava RateLimiter）+ 候选 D（Nop Platform Cache 用于配置缓存）**。理由：(i) Guava RateLimiter 是令牌桶成熟实现，无需自实现；(ii) 多节点 Redis successor 触发条件明确；(iii) Nop Platform Cache 用于 API 配置缓存（如 auth token 缓存）。
  - **EXPAND-vs-NEW**（裁决依据 Explore e）：**裁决候选 NEW（与 roadmap §D1 line 73 + strategic gap plan line 467/491 一致）**。理由：(i) roadmap 与 strategic gap plan 两份源文档均显式标注 NEW；(ii) 既有 integration-pattern.md 范围明确（webhook-only，43 行）；(iii) NEW 文档聚焦"通用外部 API 集成参考模式"独立主题，与 webhook 文档语义边界清晰；(iv) 既有 integration-pattern.md 末尾增交叉引用段指向 NEW 文档避免引用断链。**此 Decision 取代 plan 早期版本的 EXPAND 假设**。
  - **OAuth2 实现边界**：文档化标准流程（access_token + refresh_token）+ 不强制实现；如业务客户接入 OAuth2 API，按文档实施 + 触发 successor 升级为通用 OAuth2 provider。
  - **`IErpExternalApiAuthProvider` 边界**：owner doc §3 仅作**参考接口描述**（非强制 SPI），不要求全域统一实现；如业务客户需要跨域统一 auth 模式，触发 successor 升级为标准 SPI。
  - **选择依据**：平台能力边界明确避免应用层重复造轮子；endpoint 配置走候选 C 尊重 ORM 变更=否约束；参考实现候选 1 业务价值最大且复杂度可控；rate limiting 走成熟工具避免自实现；EXPAND-vs-NEW 遵循 roadmap 显式标注。
  - Skill: none
- [ ] `Add`：`docs/architecture/external-api-integration-pattern.md`（**NEW**，per Explore (e) Decision）
  - 新建 8 大段：§1 目的与范围（D1 vs 平台能力 vs D2 边界）/ §2 平台能力边界（决策树：何时用平台 vs 何时用应用层封装 + Nop GraphQL driver / xpl / IoC / 安全层 / dict API 能力清单）/ §3 Auth Pattern 参考模式（OAuth2/API Key/LWA + `IErpExternalApiAuthProvider` 参考接口描述，**非强制 SPI**）/ §4 Rate Limiting（Guava RateLimiter + 多节点 Redis successor）/ §5 Endpoint 配置范式（候选 C：yaml + 运行时 dict + 实施示例）/ §6 API Client Lifecycle（创建/健康检查/熔断/恢复 4 阶段 + 重试与幂等引用 idempotency-pattern.md + 事务边界引用 integration-and-transaction-patterns.md）/ §7 参考实现案例（logistics Carrier Gateway 既有 + b2b EDI Format 既有 + 本计划新增 master-data ExchangeRateApiClient 三案例对比）/ §8 与 Wimoor ApiBuildService 对照 / §9 反模式自检表（包括"每域自实现 HTTP 客户端 + 不复用 SPI → 应抽 IErpExternalApiClient SPI"）
  - Skill: none
- [ ] `Add`：既有 `integration-pattern.md` 增交叉引用段（避免引用断链）
  - 末尾增「§通用外部 API 集成参考模式」段，1 段说明 + 链接 `external-api-integration-pattern.md`；同时如实记录 webhook 表（`ErpSysWebhookConfig`/`ErpSysWebhookLog`）的文档/代码漂移（实际未实体化）
  - Skill: none

Exit Criteria:

- [ ] 5 个 Explore 结论已记录（含 EXPAND-vs-NEW (e)）；对应 Decision 已落地
- [ ] `external-api-integration-pattern.md`（**NEW**）落地（8 大段完整 + 反模式自检表）
- [ ] 既有 `integration-pattern.md` 增交叉引用段 + 文档/代码漂移如实记录
- [ ] 平台能力边界 + endpoint 配置 + 参考实现 + rate limiting + EXPAND-vs-NEW 5 项关键 Decision 在 owner doc 明确

### Phase 1 — 参考实现：master-data ExchangeRateApiClient + SPI + 测试（**无 ORM 变更**）

Status: planned
Targets: `IErpMdExchangeRateApiClient` SPI + Mock 实现 + Factory + config-gated 接入 + 测试
Skill: `nop-backend-dev`

- Item Types: `Add-heavy | Proof`
- Prereqs: Phase 0 完成

- [ ] `Add`：`IErpMdExchangeRateApiClient` SPI（dao 模块）
  - 路径：`module-master-data/erp-md-dao/src/main/java/app/erp/md/spi/IErpMdExchangeRateApiClient.java`（**NEW**，与既有 `IErpMdPartnerReferenceChecker` 同目录同 SPI 模式）
  - 方法签名：`Map<String, BigDecimal> fetchRates(String baseCurrency, Set<String> targetCurrencies, LocalDate asOfDate)` —— 返回 targetCurrency → rate 映射
  - Skill: `nop-backend-dev`
- [ ] `Add`：`MockExchangeRateApiClient` 实现（service 模块）
  - 路径：`module-master-data/erp-md-service/src/main/java/app/erp/md/service/exchange/MockExchangeRateApiClient.java`（**NEW**）
  - 实现：返回确定性 mock 数据（USD → CNY 7.20 固定）；config-gated（`erp-md.exchange-rate-api-provider=mock`）
  - Skill: `nop-backend-dev`
- [ ] `Add`：`ErpMdExchangeRateApiClientFactory`（service 模块）
  - 路径：`module-master-data/erp-md-service/src/main/java/app/erp/md/service/exchange/ErpMdExchangeRateApiClientFactory.java`（**NEW**，与 logistics `MockCarrierGatewayClientFactory` 同模式）
  - 实现：按 `erp-md.exchange-rate-api-provider` config 切换 mock/exchangerate-host/...；内置 Guava RateLimiter（按 `erp-md.exchange-rate-api-rate-limit-rps` 配置）；缓存（Nop Platform Cache，TTL 默认 5 分钟）
  - Skill: `nop-backend-dev`
- [ ] `Add`：`ErpMdCurrencyBizModel.refreshRatesFromApi` mutation（service 模块）
  - 路径：`module-master-data/erp-md-service/.../entity/ErpMdCurrencyBizModel.java` delta（既有扩展）
  - 实现：`refreshRatesFromApi(String baseCurrency)` —— 调 `IErpMdExchangeRateApiClient.fetchRates` + 写入 `ErpMdExchangeRate` 表；config-gated（`erp-md.exchange-rate-api-enabled` 默认 false）
  - Skill: `nop-backend-dev`
- [ ] `Add`：错误码
  - 路径：`module-master-data/erp-md-service/src/main/java/app/erp/md/service/ErpMdErrors.java`（既有追加）
  - 错误码：`ERP_MD_EXCHANGE_RATE_API_UNAVAILABLE`（API 不可达）/ `ERP_MD_EXCHANGE_RATE_API_RATE_LIMITED`（rate limit 触发）/ `ERP_MD_EXCHANGE_RATE_API_RESPONSE_INVALID`（响应格式错误）
  - Skill: `nop-backend-dev`
- [ ] `Proof`：单元测试 `TestErpMdExchangeRateApiClient`（**NEW**）
  - 路径：`module-master-data/erp-md-service/src/test/java/app/erp/md/service/exchange/TestErpMdExchangeRateApiClient.java`
  - 测试场景：(1) Mock 实现 fetchRates 返回确定性数据；(2) rate limiting 验证（连续调用触发 RATE_LIMITED 错误）；(3) 缓存验证（同一 baseCurrency+targetCurrencies+asOfDate 第二次调用走缓存）；(4) refreshRatesFromApi 端到端（fetchRates → 写入 ErpMdExchangeRate 表）；(5) config-gated 默认 false 时 refreshRatesFromApi 抛 ERP_MD_EXCHANGE_RATE_API_UNAVAILABLE
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] `IErpMdExchangeRateApiClient` SPI + Mock 实现 + Factory 落地（**无 ORM 变更**）
- [ ] `refreshRatesFromApi` mutation 实现，config-gated 默认 false
- [ ] `TestErpMdExchangeRateApiClient` 5 测试场景全绿

### Phase 2 — 既有 owner doc 回链 + roadmap 同步

Status: planned
Targets: 既有 owner doc 回链段落 + `deepening-roadmap.md` §D1 done + §8.5 落地证据
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1 完成 + 全量验证通过

- [ ] `Add`：`docs/architecture/b2b-integration.md` 增「通用 API 集成参考」段
  - 内容：B2B EDI Format (`IErpB2bEdiProvider` SPI + `ErpB2bEdiRegistry` 收集器 + Provider 派发) 实现作为 D1 参考案例 + 引用 external-api-integration-pattern.md §参考实现案例
  - Skill: none
- [ ] `Add`：`docs/architecture/integration-and-transaction-patterns.md` 增「API client 事务边界」段
  - 内容：API client 调用与业务事务边界（同步/异步/最终一致 + 引用 external-api-integration-pattern.md §API Client Lifecycle）
  - Skill: none
- [ ] `Add`：`docs/architecture/idempotency-pattern.md` 增「API client 重试与幂等」段
  - 内容：API client 重试策略（指数退避 + 最大次数）+ 幂等键（基于业务关键字 + 引用 §idempotency-pattern）
  - Skill: none
- [ ] `Add`：`docs/design/master-data/exchange-rate-management.md` 增「自动汇率刷新（API 客户端）」段
  - 内容：`refreshRatesFromApi` mutation 用法 + config 配置 + provider 切换 + rate limiting + 缓存
  - Skill: none
- [ ] `Add`：`docs/architecture/README.md` 增 `external-api-integration-pattern.md` 行
  - 内容：Initial Owner Docs 段追加新文档介绍行
  - Skill: none
- [ ] `Add`：`docs/backlog/deepening-roadmap.md` §D1 done + §8.5 落地证据
  - 路径：line 73 状态 `todo → done` + §8.5 新增段（plan + owner doc NEW + 参考实现 + 测试基线 + Deferred successor + D4 解锁说明 + EXPAND-vs-NEW Decision 记录）
  - Skill: none

Exit Criteria:

- [ ] 5 处既有 owner doc 回链段落落地（b2b-integration.md / integration-and-transaction-patterns.md / idempotency-pattern.md / exchange-rate-management.md / architecture/README.md）
- [ ] roadmap §D1 状态 done + §8.5 落地证据登记 + D4 解锁说明 + EXPAND-vs-NEW Decision 记录

## Draft Review Record

- Independent draft review iteration 1: needs revision（ses_07d1a373dffe — 3 blockers: ErpSysWebhookConfig phantom + IErpB2bEdiGatewayClient phantom + D1 ORM 未授权；3 majors: EXPAND-vs-NEW 决策缺失 + ErpSysExternalSystem 实体不必要 + 参考实现未使用实体）
- Independent draft review iteration 2: needs revision（ses_07d0e5625ffe — 残留 B2: iter-1 reviewer 处方的 IErpB2bEdiFormat 名亦是 phantom，实测真实为 IErpB2bEdiProvider；残留 M1: Goal 1 + Related header 仍说 EXPAND 与 Phase 0 NEW Decision 矛盾）
- Independent draft review iteration 3: pending（修订后由独立子代理新会话复审）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在结束时运行一次。

- [ ] 范围内行为完成（external-api-integration-pattern.md NEW + 既有 integration-pattern.md 交叉引用段 + 参考实现 + 测试 + 5 处既有 owner doc 回链）
- [ ] 相关文档对齐（external-api-integration-pattern.md + 5 处既有 owner doc 回链）
- [ ] 已运行验证：`mvn clean install -DskipTests`（154 模块）+ `mvn test -pl module-master-data/erp-md-service`（master-data service 全测试含新增 TestErpMdExchangeRateApiClient 5 场景）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### `ErpSysExternalSystem` 实体化（endpoint registry 持久化）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: deepening-roadmap.md §6 line 89 显式禁止 D1 ORM 变更（"否 — 仅为参考文档"）；本计划走候选 C（yaml + 运行时 dict）；如未来需要持久化 endpoint registry（多客户多 API 配置场景），独立 successor plan + 显式 ORM 授权。
- Successor Required: `yes`（触发条件：业务客户多 API 配置场景 + endpoint 运行时管理需求 + ORM 授权）

### notify 域 Webhook 表实体化（`ErpSysWebhookConfig` / `ErpSysWebhookLog`）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: integration-pattern.md 引用的 webhook 表尚未实体化（文档/代码漂移）；本计划在 owner doc 如实记录漂移；webhook 表实体化属 notify 域 successor。
- Successor Required: `yes`（触发条件：notify 域 webhook 接入具体业务需求）

### D4 Plugin Hot Management Research

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: D4 是独立 P3 可行性研究项（OSGi-style vs Maven module isolation vs NocoBase-style plugin manager）；D1 完成后 D4 可启动（依赖关系解除 per deepening-roadmap.md §7 mermaid edge `D1 --> D4`）；D4 需要 D1 提供具体集成案例作为插件边界评估的参考输入。
- Successor Required: `yes`（触发条件：D1 完成 + 业务客户对插件热管理需求）

### 多节点 Redis-based rate limiting

- Classification: `optimization candidate`
- Why Not Blocking Closure: 单节点 Guava RateLimiter 足够覆盖开发 + 小规模生产；多节点部署下需 Redis-based rate limiter 保证一致性。
- Successor Required: `yes`（触发条件：生产部署多节点 + rate limit 不一致引发问题）

### OAuth2 完整通用实现 + `IErpExternalApiAuthProvider` 标准 SPI

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: OAuth2 access_token + refresh_token 流程复杂；本计划仅文档化标准流程 + §3 `IErpExternalApiAuthProvider` 仅作参考接口（非强制 SPI）；具体实现按业务客户接入需求驱动。
- Successor Required: `yes`（触发条件：业务客户接入 OAuth2 API + 跨域统一 auth 模式需求 + owner doc 授权升级为标准 SPI）

### 第三方 API SDK 自动生成（OpenAPI/Swagger）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 从规范自动生成 SDK 属工具链层；本计划是参考模式文档化。
- Successor Required: `yes`（触发条件：业务客户明确需求 + 自动生成工具链选型）

### API gateway 反向代理集成

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: API gateway（Kong/APISIX）属部署架构层，不属应用层集成参考。
- Successor Required: `yes`（触发条件：生产部署架构演进 + API gateway 引入）

### API 监控/可观测性完整方案

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: API 调用监控 + 链路追踪 + 指标采集属 observability successor；本计划仅在 client lifecycle 段简述。
- Successor Required: `yes`（触发条件：生产监控需求 + observability 工具链选型）

### API 安全审计

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: API 调用日志审计 + 合规要求（GDPR / 数据安全法）属 security/compliance successor。
- Successor Required: `yes`（触发条件：合规审计需求 + security owner doc 授权）

### 跨境数据合规

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 跨境 API 数据传输合规属 l10n/legal successor。
- Successor Required: `yes`（触发条件：跨国集团业务 + 数据合规审计需求）

## Closure

Status Note: pending（计划尚未实施）

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- `ErpSysExternalSystem` 实体化（触发：多 API 配置场景 + ORM 授权）
- notify 域 Webhook 表实体化（触发：notify 域 webhook 业务需求）
- D4 Plugin Hot Management Research（触发：D1 完成 + 业务客户插件热管理需求）
- 多节点 Redis-based rate limiting（触发：生产部署多节点 + rate limit 不一致）
- OAuth2 完整通用实现 + `IErpExternalApiAuthProvider` 标准 SPI（触发：业务客户 OAuth2 接入 + 跨域统一 auth 需求）
- 第三方 API SDK 自动生成（触发：业务客户明确需求 + 自动生成工具链选型）
- API gateway 反向代理集成（触发：生产部署架构演进）
- API 监控/可观测性完整方案（触发：生产监控需求）
- API 安全审计（触发：合规审计需求 + security owner doc 授权）
- 跨境数据合规（触发：跨国集团业务 + 数据合规审计）
