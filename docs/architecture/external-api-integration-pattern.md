# 外部 API 集成参考模式

> **本文定位**：通用第三方 API 集成的应用层参考模式（auth pattern + rate limiting + endpoint 配置范式 + API client lifecycle）。
>
> **范围**：deepening-roadmap.md §Milestone D §D1 — Reference design document (not standard SPI)。
> 与 `integration-pattern.md`（webhook 出站/入站）是兄弟文档，两者通过本文末尾「与既有集成文档的关系」段相互引用。
>
> **D1 ORM 变更=否**：本模式不新建 `ErpSysExternalSystem` 实体；endpoint 配置走 yaml + 运行时 dict（候选 C 裁决，见 §5）。
>
> **边界声明**：本文是**参考模式**（reference pattern），不强制全域统一 SPI；既有 logistics/b2b 实现作为参考案例引用，**不**被本文重构。

## §1 目的与范围

### 1.1 要解决的差距

`nop-app-erp` 已有的集成文档/实现：

- `integration-pattern.md`（43 行，webhook 出站/入站 + 签名 + IP 白名单）—— 但 webhook 配置表 `ErpSysWebhookConfig`/`ErpSysWebhookLog` **尚未实体化**（文档/代码漂移，见 §6.2）。
- `b2b-integration.md`（B2B EDI 集成契约层 — `IErpB2bEdiProvider` SPI + `ErpB2bEdiRegistry` 收集器）。
- `integration-and-transaction-patterns.md`（集成与事务模式）。
- `idempotency-pattern.md`（幂等模式）。
- 应用层既有先例：logistics Carrier Gateway（Client/Factory/Registry 三层）、b2b EDI Format（Provider 按类型派发）。

**缺失**：通用第三方 REST/SOAP/GraphQL API 集成的参考模式 —— 何时用平台能力、何时用应用层封装、auth pattern、rate limiting、endpoint 配置、client lifecycle。

### 1.2 D1 与 D2 边界

| 工作项 | 主题 | D1 关系 |
|--------|------|---------|
| **D1（本文）** | 外部 API 集成参考模式 | 本身 |
| D2 Business Module Metadata | 模块元数据（module-meta.json） | 不同主题 — 模块组装层而非集成层 |
| D3 Cost Calculation Sub-Calculator | 成本计算子计算器注入 | finance 内部模式，与外部 API 不同 |
| D4 Plugin Hot Management Research | 插件热管理可行性 | D1 完成后可启动（依赖关系见 deepening-roadmap.md §7） |

### 1.3 Non-Goals

- 不替代 Nop Platform 平台能力（GraphQL driver / xpl / IoC / 安全层 / dict API）。
- 不新建 `ErpSysExternalSystem` 实体（D1 ORM 变更=否）。
- 不强制 `IErpExternalApiAuthProvider` 标准 SPI（仅参考接口描述）。
- 不重构 logistics/b2b 既有实现（仅作为参考案例引用）。
- 不实现 OAuth2 完整流程（仅文档化标准流程）。
- 不实现 SDK 自动生成 / API gateway 反向代理 / 完整监控方案（均归 successor）。

## §2 平台能力边界（决策树）

### 2.1 Nop Platform 已提供的集成能力清单

| 能力 | 平台位置 | 适用场景 | 应用层何时不用重复 |
|------|---------|---------|------------------|
| **GraphQL driver as ORM datasource** | `nop-orm-dao-graphql` 等 | 第三方 GraphQL API 作为 ORM 只读数据源 | 仅读路径 — 无需自己写 HTTP 客户端 |
| **xpl 标签库 HTTP 封装** | `xpl` 标签库 + Nop HTTP 工具 | 简单一次性 HTTP 调用（无复杂业务编排） | 单步 HTTP 调用直接用 xpl |
| **IoC `@Inject` + `ioc:collect-beans`** | `nop-ioc` | 多实现按类型收集 + 注册中心建图 | 多 provider 切换 — 不要自己写 if/else |
| **`IRateLimiter` / `DefaultRateLimiter`** | `nop-commons` `io.nop.commons.concurrent.ratelimit` | 单节点令牌桶限流 | **不要引入 Guava RateLimiter** |
| **安全层 auth token 处理** | `nop-auth-core` | 平台用户认证 / token 管理 | 应用层第三方 OAuth2 流程在 §3 参考描述 |
| **`nop-dict` 运行时字典** | `nop-dict` | 字典 / 配置运行时覆盖（无需重启） | endpoint 配置走 dict 而非自建配置表 |
| **`AppConfig.var(...)` 配置读取** | `nop-config` | yaml 配置 + 运行时覆盖 | 全部配置经此 API（不要直接读文件） |
| **`Cache` / `ICache`** | `nop-cache` | API token / endpoint 配置缓存 | 不要自实现 ConcurrentHashMap 缓存（除非极简场景） |
| **`NopException` + `ErrorCode`** | `nop-api-core` | 业务异常标准化 | 所有 API 错误必须转 NopException + ErrorCode |

### 2.2 决策树：何时用平台 vs 何时用应用层封装

```
外部 API 调用需求
│
├─ 是第三方 GraphQL API 且只读？
│   └─ YES → 用 Nop GraphQL driver 作为 ORM datasource（平台能力，零应用代码）
│
├─ 是简单一次性 HTTP 调用（无多 provider 切换、无复杂业务逻辑）？
│   └─ YES → 用 xpl 标签库 HTTP 封装（平台能力，无 Java 类）
│
├─ 是复杂业务逻辑 + 多 provider 切换 + 状态管理？
│   └─ YES → 应用层 SPI + Factory + Registry 三层（见 §7 logistics 案例）
│            ↓ 进一步判断
│            ├─ 单步骤业务 → BizModel `@BizMutation` 直接实现
│            ├─ 多步骤拓扑稳定 → Processor 模式（protected step 方法）
│            └─ 多步骤拓扑可变 → task.xml 编排
│
└─ 是 EDI / 报文格式派发（按业务单据类型）？
    └─ YES → 应用层 Provider SPI + Registry 按类型派发（见 §7 b2b 案例）
```

### 2.3 反模式：重复造轮子

| 反模式 | 正确做法 |
|--------|---------|
| 引入 Guava / Hystrix / Resilience4j 等"成熟库"重复实现限流/熔断 | 优先用平台 `IRateLimiter`；如需熔断 successor 评估平台能力后再决定 |
| 在 BizModel 中用 `if (provider.equals("dhl")) { ... }` 硬编码派发 | 用 `ioc:collect-beans` + Registry 模式（见 logistics `ErpLogCarrierGatewayRegistry`） |
| 自建 ConcurrentHashMap 缓存 API token / endpoint 配置 | 用平台 `Cache` 或 `AppConfig.var(...)` |
| 在 yaml 中读 `Properties.load(new FileInputStream(...))` | 用 `AppConfig.var(key, defaultValue)` |
| throws RuntimeException / new RuntimeException("API failed") | 必须 `NopException` + `ErrorCode.define()` + 中文描述 |
| 每个域自实现 HTTP 客户端 + 不复用 SPI | 抽象 `IErpExternalApiClient` SPI（仅在 3+ 域有同类需求时） |

## §3 Auth Pattern 参考模式

> **声明**：本节描述的是**参考流程**，不是强制 SPI。`IErpExternalApiAuthProvider` 是**参考接口**（非强制），不要求全域统一实现。如业务客户需要跨域统一 auth 模式，触发 successor 升级为标准 SPI（见 Deferred）。

### 3.1 三种标准 auth pattern

#### API Key（最简单）

- **流程**：调用方在请求头/查询参数中携带预共享 key（`Authorization: Bearer <key>` 或 `X-API-Key: <key>`）。
- **适用**：公开 API（汇率查询/天气/物流跟踪等免费 API）。
- **凭证管理**：key 经 yaml config 或 dict 配置；机密场景经 `EncryptionHelper` 加密存储。
- **参考实现**：本计划 master-data ExchangeRateApiClient（Mock 无 key；真实 provider 经 `erp-md.exchange-rate-api-key` 配置）。

#### OAuth2（最复杂）

- **标准流程**（Authorization Code Grant）：
  1. 应用重定向用户到 OAuth2 provider 授权页（带 client_id + redirect_uri + scope）。
  2. 用户授权后，provider 重定向回 redirect_uri 携带 `code`。
  3. 应用用 `code` + `client_secret` 换取 `access_token` + `refresh_token`。
  4. 应用用 `access_token` 调用 API；token 过期（401）时用 `refresh_token` 刷新。
- **token 管理**：`access_token` 缓存（TTL = `expires_in - 缓冲`）；`refresh_token` 持久化（仅在 token rotation 场景）。
- **机密保护**：`client_secret` 经 `EncryptionHelper` 加密存储，不进 git。
- **本计划不实现**：OAuth2 流程复杂；本文档化标准流程 + 业务客户接入时按需实施（Deferred）。

#### LWA (Login with Amazon) — Wimoor 案例

Wimoor `ApiBuildService`（见 `docs/analysis/erp-survey/2026-07-20-0000-wimoor-compare.md:121-166`）的 LWA 流程是 OAuth2 的 Amazon SP-API 变体：
- 凭证管理：`AmazonAuthority implements RateLimitConfiguration`（Wimoor 调研报告 §6.2 指此为反模式 — 领域实体耦合基础设施）。
- nop 借鉴方式：`IErp*Biz` + `@Inject` SPI 管理第三方认证 + `IErpExternalApiAuthProvider` 参考接口。

### 3.2 `IErpExternalApiAuthProvider` 参考接口（非强制 SPI）

```java
// 参考接口描述 — 不强制实现；业务客户需要时再升级为标准 SPI
public interface IErpExternalApiAuthProvider {
    /** 凭证类型枚举（API_KEY / OAUTH2 / LWA / NONE）。 */
    AuthType getAuthType();

    /** API_KEY 模式：返回 key；其他模式抛 UnsupportedOperationException。 */
    String getApiKey();

    /** OAUTH2/LWA 模式：返回有效 access_token（内部缓存 + 自动刷新）；API_KEY 模式抛 UnsupportedOperationException。 */
    String getAccessToken();

    /** OAUTH2/LWA 模式：刷新 access_token；其他模式抛 UnsupportedOperationException。 */
    String refreshAccessToken();
}
```

> **何时升级为标准 SPI**：业务客户接入 ≥ 2 个 OAuth2 API + 跨域统一 auth 管理需求 + owner doc 授权（触发 successor）。

## §4 Rate Limiting

### 4.1 平台已提供 `IRateLimiter`（token bucket）

Nop Platform `nop-commons` 提供 `io.nop.commons.concurrent.ratelimit.IRateLimiter` + `DefaultRateLimiter`：

```java
// 平台令牌桶限流器（无需引入 Guava）
IRateLimiter limiter = DefaultRateLimiter.create(10.0); // 10 permits/sec
if (!limiter.tryAcquire()) {
    throw new NopException(ERR_XXX_RATE_LIMITED).param(...);
}
```

- 单节点令牌桶算法（`DefaultRateLimiter(permitsPerSecond)`）。
- 支持 `tryAcquire()` 非阻塞 + `acquire()` 阻塞 + 统计 `getAcquireSuccessCount/getAcquireFailCount`。

### 4.2 候选方案权衡表

| 候选 | 算法 | 多节点一致性 | 依赖 | 适用场景 | 裁决 |
|------|------|-------------|------|---------|------|
| A：自实现 ConcurrentHashMap + 时间窗口 | 时间窗口 | 不一致 | 无 | 学习项目；不推荐生产 | ❌ |
| B：Guava RateLimiter | 令牌桶 | 不一致 | +Guava 依赖 | Java 生态成熟库 | ❌ 平台已有等价物，无需引入 |
| C：Nop Platform `IRateLimiter` | 令牌桶 | 不一致 | 平台内置 | 单节点生产 | ✅ **默认方案** |
| D：Redis-based (Redisson) | 令牌桶 + Lua | 一致 | +Redis | 多节点生产 | ⚠️ successor（多节点部署触发） |

### 4.3 默认方案 + successor 触发条件

- **默认**：单节点 `DefaultRateLimiter`（候选 C）。无需额外依赖；与平台工具统一。
- **successor 触发条件**：生产部署多节点 + rate limit 不一致引发问题 → 升级为 Redis-based（候选 D）。
- **配置**：`erp-md.exchange-rate-api-rate-limit-rps`（默认 10）。

### 4.4 per-tenant / per-key 限流约定

- **per-provider**：每个 API provider 一个 `IRateLimiter` 实例（Factory 内 ConcurrentHashMap<providerId, IRateLimiter>）。
- **per-tenant**：当前为单租户私有部署（per-orgId 限流属 SaaS 化 successor）。
- **per-api-key**：第三方 API 按 key 维度限流（每个 key 一个 limiter，避免一个 key 触发限流影响其他 key）。

## §5 Endpoint 配置范式（候选 C 裁决）

### 5.1 候选评估

| 候选 | 描述 | 裁决 |
|------|------|------|
| A：新建 `ErpSysExternalSystem` 实体 | 持久化 endpoint registry | ❌ deepening-roadmap.md §6 line 89 显式禁止 D1 ORM 变更 |
| B：扩展 notify 域 webhook 表 | 复用 webhook 配置实体 | ❌ 该表本身未实体化（文档/代码漂移） |
| C：yaml config + 运行时 dict | 双层配置（编译期 + 运行时） | ✅ **裁决** |

### 5.2 候选 C 设计

#### 双层覆盖模型

```
编译期（yaml）        运行时（dict 覆盖）       最终值
─────────────────  ─────────────────────  ─────────────────
erp-md.exchange-   dict[erp-md/exchange-   dict 优先
rate-api-key=xxx   rate-api-key]=yyy       yaml 兜底
```

- **yaml config**（`app-erp-all/src/main/resources/application.yaml`）：编译期默认值，运维变更需重启。
- **Nop Platform `nop-dict`**（运行时 dict 覆盖）：业务/运维经管理界面变更 dict 值，无需重启（`AppConfig.var(...)` 实时读取最新值）。
- **应用层 API**：`AppConfig.var(key, defaultValue)` 自动合并 yaml + dict 层（dict 优先）。

#### 参考实现配置点（master-data 汇率查询）

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `erp-md.exchange-rate-api-enabled` | `false` | 启用开关（config-gated 默认关） |
| `erp-md.exchange-rate-api-provider` | `mock` | provider 切换（mock/exchangerate-host/...） |
| `erp-md.exchange-rate-api-key` | 空 | API Key（OAuth2 场景为 client_id） |
| `erp-md.exchange-rate-api-rate-limit-rps` | `10` | 每秒请求数限制 |
| `erp-md.exchange-rate-api-cache-ttl-secs` | `300` | token / 响应缓存 TTL |

### 5.3 何时升级为候选 A（持久化实体）

- **触发条件**：业务客户多 API 配置场景（> 5 个第三方系统）+ endpoint 运行时管理需求 + ORM 授权。
- **successor plan**：独立 plan + 显式 ORM 授权 + 候选 A 实体设计（如 `ErpSysExternalSystem` + `ErpSysExternalSystemAuth`）。

## §6 API Client Lifecycle

### 6.1 四阶段范式

```
1. 创建 (Create)
   ├─ Factory.newClient(config) 构造配置化 client
   ├─ 凭证经 EncryptionHelper 解密
   └─ 注入 endpoint / timeout / retry 策略
│
2. 健康检查 (Health Check)
   ├─ 启动时可选 health check（轻量 ping 接口）
   └─ 不阻断应用启动（仅日志告警）
│
3. 熔断 (Circuit Break)
   ├─ 连续 N 次失败触发熔断（快速失败）
   ├─ 熔断期间直接抛 ERR_XXX_UNAVAILABLE 不调用远端
   └─ 半开状态：定时探测恢复
│
4. 恢复 (Recover)
   ├─ 半开探测成功 → 关闭熔断
   └─ 失败 → 继续熔断
```

> **本计划不实现完整熔断器**：logistics GatewayDispatcher 是参考实现（重试 + 死信，无熔断状态机）；完整熔断 successor（触发条件：生产高可用需求 + 平台熔断能力评估）。

### 6.2 重试与幂等

- **重试策略**：指数退避（base × 2^n，最大 maxRetries 次），参考 logistics `GatewayDispatcher`（`erp-log.gateway-max-retries` + `erp-log.retry-base-interval-secs`）。
- **可重试条件**：5xx / 网络超时 / 429 Too Many Requests；**不可重试**：4xx 客户端错误（除 429）。
- **幂等性**：见 `idempotency-pattern.md` §规则 1（业务自然键作幂等键）。
- **API client 幂等键约定**：每个 mutation API 必须接受 `clientRequestId` 参数（业务自然键，如 `purOrder.code`），重复请求返回已有结果。

### 6.3 事务边界（引用 `integration-and-transaction-patterns.md`）

- **本地优先**：影响本地状态的操作必须在任何外部写入之前持久化（`integration-and-transaction-patterns.md §本地优先规则`）。
- **afterCommit 钩子**：依赖成功本地事务的外部写入应在 `txn().afterCommit()` 中运行（不要在事务回滚后调用外部 API）。
- **外部结果所有权**：结果源自外部系统响应时，本地系统负责启动/查询/下载，**不**伪造外部行为。
- **API client 不可在事务内阻塞**：长时间外部调用应异步（如 logistics tracking 轮询经 cron，不在用户事务内同步等待）。

### 6.4 文档/代码漂移记录

> **如实记录**（不修复，归 successor）：
>
> - `integration-pattern.md` 引用的 `ErpSysWebhookConfig` / `ErpSysWebhookLog` **尚未实体化**（notify 模块 ORM 仅 `ErpSysNotificationTemplate`/`ErpSysNotification`/`ErpSysNotificationRead` 三实体）。
> - webhook 配置实体化属 notify 域 successor（触发条件：notify 域 webhook 接入具体业务需求）。
> - 本文所有 webhook 相关引用按"设计约定"理解，不视为已落地能力。

## §7 参考实现案例（3 案例对比）

### 7.1 案例 A：logistics Carrier Gateway（Client/Factory/Registry 三层）

> **既有实现**（plan `2026-07-14-0508-1`，本计划引用不重构）

**三层结构**：

| 层 | 类 | 职责 |
|----|----|------|
| Client SPI | `IErpLogCarrierGatewayClient` | 封装单个承运商所有动作（completeDeliveryOrder / trackShipment / cancelShipment / getRateQuote 等） |
| Factory SPI | `IErpLogCarrierGatewayClientFactory` | 按 carrierId 构造配置化 client（读 `ErpLogCarrierConfig` 解密凭证） |
| Registry | `ErpLogCarrierGatewayRegistry` | 启动时收集所有 Factory bean，按 `getGatewayId()` 建立查找表，运行时按 carrier.gatewayId 派发 |

**适用场景**：复杂业务编排（一个 provider 有 5+ 动作）+ 多 provider 切换（mock/dhl/sf/...）+ 凭证持久化（`ErpLogCarrierConfig` 实体）。

**注册范式**（`app-service.beans.xml`）：

```xml
<bean id="app.erp.log.service.spi.ErpLogCarrierGatewayRegistry"
      class="app.erp.log.service.spi.ErpLogCarrierGatewayRegistry">
    <property name="factories">
        <ioc:collect-beans by-type="app.erp.log.service.spi.IErpLogCarrierGatewayClientFactory"
                           only-concrete-classes="true" ioc:ignore-depends="true"/>
    </property>
</bean>
<bean id="app.erp.log.service.spi.mock.MockCarrierGatewayClientFactory"
      class="app.erp.log.service.spi.mock.MockCarrierGatewayClientFactory"/>
```

> **为什么不用 `@Inject Map<String, T>`**：以 bean-name 为键脆弱耦合（renaming bean 会断链）；用 `ioc:collect-beans` + 显式 `getGatewayId()` 建立业务键查找表更稳健（见 `ErpLogCarrierGatewayRegistry:25` 注释）。

### 7.2 案例 B：b2b EDI Format（Provider 按类型派发，异构范式）

> **既有实现**（plan `2026-07-19-1820-1`，本计划引用不重构）

**结构**：

| 层 | 类 | 职责 |
|----|----|------|
| Provider SPI | `IErpB2bEdiProvider` | 单一接口封装一个 EDI 格式（UBL Invoice / UBL DespatchAdvice 等）的 generatePayload / parsePayload / getApplicability |
| Registry | `ErpB2bEdiRegistry` | 启动时收集所有 Provider bean，按 `getCode()` 建立 formatCode→Provider 查找表 |

**与 logistics 范式异构性**：

- logistics 是 Client/Factory/Registry **三层**（Factory 读凭证构造 Client）。
- b2b 是 Provider/Registry **两层**（Provider 直接执行，无独立 Factory）—— 因 EDI 格式不需要 per-instance 凭证配置。
- **本计划不强求两域同构**：承认两域范式异构性，各自适用场景不同（logistics 多 carrier + 凭证管理需 Factory；b2b 多 format + 无凭证管理只需 Provider）。

### 7.3 案例 C：master-data ExchangeRateApiClient（本计划新增，轻量 SPI）

> **NEW**（plan `2026-07-21-1206-3` Phase 1）

**结构**：

| 层 | 类 | 职责 |
|----|----|------|
| Client SPI | `IErpMdExchangeRateApiClient` | 单一接口 `fetchRates(baseCurrency, targetCurrencies, asOfDate)` |
| Factory | `ErpMdExchangeRateApiClientFactory` | 按 `erp-md.exchange-rate-api-provider` config 切换实现；内置 `IRateLimiter`（限流）+ Cache（响应缓存） |
| Mock 实现 | `MockExchangeRateApiClient` | 确定性 mock 数据（USD→CNY 7.20），无外部 HTTP |

**适用场景**：单动作 API（`fetchRates` 一个方法）+ 公开 API（无复杂凭证管理）+ config-gated 默认关。

**为什么不用 Registry**：仅一个动作 + 单 provider 实例（per 配置切换，非多 provider 并存）；Factory 直接根据 config 返回单实例即可，无需 Registry 建图。

### 7.4 三案例范式选择矩阵

| 场景特征 | logistics 案例 | b2b 案例 | master-data 案例 |
|---------|---------------|----------|-----------------|
| 动作数量 | 5+（复杂） | 3+（中等） | 1（简单） |
| 多 provider 并存 | ✅（mock + dhl + sf） | ✅（UBL + X12 + EDIFACT） | ❌（单 provider per 配置） |
| 凭证持久化 | ✅（`ErpLogCarrierConfig`） | ❌ | ❌（yaml/dict） |
| 推荐范式 | Client + Factory + Registry | Provider + Registry | Client + Factory（无 Registry） |

## §8 与 Wimoor ApiBuildService 对照

> 来源：`docs/analysis/erp-survey/2026-07-20-0000-wimoor-compare.md:121-166` + `2026-07-20-0000-wimoor.md`

### 8.1 架构差异

| 维度 | Wimoor | nop-app-erp（本模式 + 既有先例） |
|------|--------|----------------------------------|
| 现有集成 | 亚马逊 SP-API 全栈（订单/报告/库存/财务/通知/广告）20+ API | logistics Carrier Gateway + b2b EDI Provider + master-data ExchangeRateApiClient（本计划） |
| 工厂模式 | 单一 `ApiBuildService` 集中构建 20+ API client（方法级 Builder 重复代码） | 每 provider 一个 Factory/Provider bean + Registry 收集（按业务键派发） |
| 认证体系 | LWA Token Exchange + Restricted Data Token + `AmazonAuthority` 领域实体耦合基础设施（反模式） | `IErpExternalApiAuthProvider` 参考接口（非强制）；凭证经 yaml/dict + EncryptionHelper |
| SDK 策略 | Swagger Codegen 生成 Java SDK（`com.amazon.spapi.api.*`） | 平台 delta 模式可类似生成（successor） |
| Rate Limiting | `RateLimitConfiguration` + 代理支持（`AmazonAuthority` 耦合） | 平台 `IRateLimiter`（无领域实体耦合） |
| 派发风格 | 方法级 Builder 重复代码（每个 API client 10+ 行几乎相同） | `ioc:collect-beans` + Registry 类型安全注册 |

### 8.2 可借鉴 vs 不借鉴

#### 可借鉴

- **"工厂 + SDK + 认证引擎"三层架构**：作为复杂集成场景的参考（≥ 5 个第三方 API）。
- **Region / Endpoint 映射**：在候选 A 实体化时（`ErpSysExternalSystem` 持 region/endpoint 字段）。
- **凭证管理独立于领域实体**：避免 Wimoor `AmazonAuthority implements RateLimitConfiguration` 的反模式（领域实体耦合基础设施）。

#### 不借鉴

- **单一 `ApiBuildService` 集中所有 client**：方法级重复代码可维护性差；nop 用 SPI Map 注册更优雅（见 `ErpFinAcctDocRegistry` + `ErpLogCarrierGatewayRegistry`）。
- **领域实体耦合 RateLimitConfiguration**：限流属基础设施，应在 Factory/Client 层，不在领域实体。
- **SDK 自动生成作为唯一接入方式**：本计划参考实现采用直接 Java HttpClient（轻量 API 无需 SDK）；SDK 自动生成归 successor（OpenAPI/Swagger 规范接入）。

## §9 反模式自检表

| # | 反模式 | 后果 | 正确做法 |
|---|--------|------|---------|
| AP1 | 每域自实现 HTTP 客户端 + 不复用 SPI | 集成层重复造轮子；维护成本高 | 抽象 `IErpExternalApiClient` SPI（仅在 3+ 域有同类需求时）；< 3 域时直接用平台 xpl / HttpClient |
| AP2 | 引入 Guava / Hystrix 重复实现限流 | 增加 maven 依赖；与平台工具不一致 | 用平台 `IRateLimiter` / `DefaultRateLimiter`（候选 C 默认） |
| AP3 | 在 BizModel 中 `if (provider.equals("dhl"))` 硬编码 | 增 provider 需改业务代码 | `ioc:collect-beans` + Registry 按业务键派发 |
| AP4 | throws RuntimeException / new RuntimeException | 平台异常处理管道丢失 | `NopException` + `ErrorCode.define()` + 中文描述 |
| AP5 | 长时间外部 API 在用户事务内同步等待 | 事务超时；用户体验差 | afterCommit 异步 / cron 轮询 / 异步队列 |
| AP6 | 领域实体 implements RateLimitConfiguration | 领域耦合基础设施 | 限流责任在 Factory/Client 层 |
| AP7 | `@Inject private` 字段 | Nop IoC 容器注入失败 | `@Inject` 不能 private |
| AP8 | 不带 clientRequestId 的 mutation API | 重复请求创建重复数据 | 每个 mutation API 接受业务自然键作幂等键 |
| AP9 | 在事务回滚后调用外部 API | 本地失败但外部已生效（数据不一致） | 用 `txn().afterCommit()` 钩子 |
| AP10 | client_secret / API Key 硬编码进 git | 凭证泄露 | yaml config + 加密存储 / 环境变量注入 |
| AP11 | webhook 配置实体未实体化但代码引用 | 文档/代码漂移（integration-pattern.md 现状） | 如实记录漂移 + 触发 successor 实体化 |
| AP12 | 一个 provider 故障影响其他 provider | 级联失败 | per-provider 熔断（successor）+ 资源隔离 |

## §10 EXPAND-vs-NEW Decision 记录

> **裁决**：选候选 NEW（与 deepening-roadmap.md §D1 line 73 + post-survey-strategic-gaps.md line 467/491 一致）。

| 候选 | 描述 | 裁决 |
|------|------|------|
| EXPAND | 扩展 `integration-pattern.md` 既有 43 行 → ~300 行 | ❌ 与 roadmap 显式标注 NEW 矛盾 |
| NEW | 新建本文档 + 既有 integration-pattern.md 保留 webhook-only 并交叉引用 | ✅ |

**选 NEW 的理由**：

1. roadmap（`deepening-roadmap.md §D1 line 73`）与 strategic gap plan（`2026-07-20-post-survey-strategic-gaps.md line 467/491`）两份源文档**均显式标注 NEW**。
2. 既有 `integration-pattern.md` 范围明确（webhook-only，43 行）—— EXPAND 会破坏其主题边界。
3. NEW 文档聚焦"通用外部 API 集成参考模式"独立主题，与 webhook 文档语义边界清晰。
4. 既有 `integration-pattern.md` 末尾增交叉引用段（见该文档）避免引用断链。

**此 Decision 取代 plan 早期版本的 EXPAND 假设**。

## §11 与既有集成文档的关系

| 文档 | 主题 | 与本文关系 |
|------|------|-----------|
| `integration-pattern.md` | Webhook 出站/入站 | webhook-only；末尾增交叉引用段指向本文 |
| `b2b-integration.md` | B2B EDI 集成契约层 | 引用本文 §7 案例 B（b2b EDI Provider 范式）；本文 §7.2 引用 b2b 实现作为案例 |
| `integration-and-transaction-patterns.md` | 集成与事务模式 | 本文 §6.3 引用其事务边界规则；增「API client 事务边界」段反向引用 |
| `idempotency-pattern.md` | 幂等模式 | 本文 §6.2 引用其规则 1（业务自然键作幂等键）；增「API client 重试与幂等」段反向引用 |
| `docs/design/logistics/carrier-integration.md` | logistics Carrier Gateway 业务语义 | 本文 §7.1 引用 logistics 实现作为案例 A |
| `docs/design/master-data/exchange-rate-management.md` | 汇率管理 | 增「自动汇率刷新（API 客户端）」段引用本文 §7.3 案例 C |

## 参考

- `docs/backlog/deepening-roadmap.md §Milestone D §D1`（line 73/89 — D1 范围与 ORM 变更=否）
- `docs/analysis/erp-survey/2026-07-20-post-survey-strategic-gaps.md §3.2`（line 93-115 — Reference value from Wimoor `ApiBuildService`）
- `docs/analysis/erp-survey/2026-07-20-0000-wimoor-compare.md §3`（line 121-166 — Wimoor ApiBuildService 对比）
- `../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md`（平台 HTTP 客户端辅助工具）
- `../nop-entropy/nop-commons/src/main/java/io/nop/commons/concurrent/ratelimit/`（平台 IRateLimiter / DefaultRateLimiter）
