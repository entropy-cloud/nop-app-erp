# 汇率管理

## 目的

定义 nop-app-erp 的汇率主数据管理机制，支持日表、FALLBACK 兜底、预警和已引用锁定。

## 汇率表结构

```
ErpMdExchangeRate（汇率日表）
    ├─ rateId
    ├─ fromCurrencyId（源币种）
    ├─ toCurrencyId（目标币种）
    ├─ rateDate（汇率日期）
    ├─ rate（汇率值）
    ├─ rateType（FIXED/MIDDLE/SELLING）
    └─ isActive
```

## FALLBACK 兜底

当指定日期的汇率不存在时，按以下优先级查找：

| 优先级 | 查找规则 |
|--------|----------|
| 1 | 当天汇率 |
| 2 | 最近一天汇率（向前 7 天内） |
| 3 | 月初汇率 |
| 4 | 上月末汇率 |
| 5 | 报错（缺失汇率） |

## 汇率预警

- 汇率波动超过阈值（如 5%）时自动告警
- 配置项：`erp-md.exchange-rate-alert-threshold`（默认 0.05）
- 告警渠道：站内消息

## 已引用锁定

已用于业务单据的汇率不允许修改：

- 校验逻辑：查询是否有业务单据引用该汇率
- 锁定后只能新增新汇率，不能修改旧汇率
- 锁定状态：`isLocked = true`

## 汇率来源

| 来源 | 说明 | 更新频率 |
|------|------|----------|
| 手工录入 | 财务员手动维护 | 随时 |
| 央行数据 | 自动采集中国外汇交易中心数据 | 每日 |
| 第三方 API | 接入汇率数据服务商 | 实时/每日 |

## 自动汇率刷新（API 客户端，D1，plan `2026-07-21-1206-3`）

> 本节落地 D1 外部 API 集成参考模式 §7.3 案例 C 的实际接入：master-data 域汇率查询 API 客户端。
> 完整参考架构（auth + rate limiting + client lifecycle + 3 案例对比）见
> [`docs/architecture/external-api-integration-pattern.md`](../../architecture/external-api-integration-pattern.md)。

### 接入入口

- **BizModel**：`ErpMdCurrencyBizModel.refreshRatesFromApi(baseCurrency, context)` —— `@BizMutation` 入口。
- **SPI**：`IErpMdExchangeRateApiClient.fetchRates(baseCurrency, targetCurrencies, asOfDate)`（dao 模块，跨工程消费者经 `erp-md-dao` 依赖）。
- **Factory**：`ErpMdExchangeRateApiClientFactory`（service 模块）—— 单 provider per 配置切换 + 内置 `IRateLimiter`（令牌桶，platform-first）+ TTL 缓存。
- **Mock 实现**：`MockExchangeRateApiClient`（默认 provider，无外部 HTTP，固定汇率表）。

### 配置项（5 项，全部 config-gated 默认关）

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `erp-md.exchange-rate-api-enabled` | `false` | 启用开关（config-gated 默认关） |
| `erp-md.exchange-rate-api-provider` | `mock` | provider 切换（mock/exchangerate-host/...） |
| `erp-md.exchange-rate-api-key` | 空 | API Key（OAuth2 场景为 client_id） |
| `erp-md.exchange-rate-api-rate-limit-rps` | `10` | 每秒请求数限制（Nop `IRateLimiter`） |
| `erp-md.exchange-rate-api-cache-ttl-secs` | `300` | 响应缓存 TTL（秒） |

### 行为约定

- **config-gated 默认关**：未启用时调 `refreshRatesFromApi` 抛 `ERR_EXCHANGE_RATE_API_UNAVAILABLE`。
- **rate limiting**：超过 `erp-md.exchange-rate-api-rate-limit-rps` 抛 `ERR_EXCHANGE_RATE_API_RATE_LIMITED`。
- **缓存**：同 cacheKey（`baseCurrency|sorted(targetCurrencies)|asOfDate`）在 TTL 内走缓存（隐式幂等）。
- **幂等性**：写入 `ErpMdExchangeRate` 时按 `(fromCurrencyId, toCurrencyId, validFrom)` upsert（已存在则更新 rate 字段，对齐 `idempotency-pattern.md §规则 1`）。
- **区间互斥（C3）**：`ErpMdExchangeRateBizModel.defaultPrepareSave/Update` 钩子接入 `ErpDateRangeOverlapValidator`，同 `fromCurrencyId + toCurrencyId + rateType` 维度区间互斥（MUTEX 策略），重叠抛 `ERR_MD_DATE_RANGE_OVERLAP`。详见 `../date-ranged-validity-pattern.md §6`。
- **部分成功**：API 返回的 targetCurrency 不在主数据币种表中 → 跳过（不抛异常，部分成功语义）。

### 与 logistics/b2b 范式异构

本接入是 Client/Factory **两层**（单 provider per 配置切换，非并存）；logistics Carrier Gateway 是 Client/Factory/Registry **三层**（多 provider 并存 + 凭证管理）；b2b EDI Format 是 Provider/Registry **两层**（多 format 并存 + 按类型派发）。三案例范式选择矩阵见 `external-api-integration-pattern.md §7.4`。

### Deferred successor

- 真实 provider 接入（exchangerate.host 公开 API / 銀企直连）：触发条件 — 业务客户接入需求。
- OAuth2 流程实施：触发条件 — 接入 OAuth2 provider API（如銀企直连 OAuth2）。
- 完整熔断状态机：触发条件 — 生产高可用需求 + 平台熔断能力评估。

### 浏览器层验证（plan `2026-07-26-1407-3`）

`refreshRatesFromApi` `@BizMutation` 经 GraphQL `/graphql` 全栈可达性由 `tests/e2e/business-actions/md-exchange-rate-api.action.spec.ts` 覆盖（收口「JUnit 单层验证但零浏览器层 E2E」缺口）：

- **config-gate 启用**：webServer JVM args 全局启用 5 项 exchange-rate-api config（`enabled=true` + `provider=mock` + `key=test-key` + `rate-limit-rps=100` + `cache-ttl-secs=60`）；config 关闭路径守卫 `ERR_EXCHANGE_RATE_API_UNAVAILABLE` 经 JUnit `testConfigGatedDefaultDisabled` 已覆盖，浏览器层不重复（webServer 全局启用无法 per-spec toggle，对齐 simulation/intercompany config-gate 范式）。
- **双层断言**：(1) `refreshRatesFromApi(baseCurrency:"USD")` 返回 List 选择集断言 Mock 确定性 rate 值（USD→CNY 7.20 / USD→EUR 0.92，rateType=MIDDLE）；(2) `ErpMdExchangeRate__findPage` 按 fromCurrencyId 反查持久化行，关联 `ErpMdCurrency.code` 断言 fromCurrency/toCurrency code + rate 字段。
- **自包含 EUR setup**：种子 `erp_md_currency.csv` 仅含 CNY+USD，EUR 不在种子 → spec 经 `ErpMdCurrency__save` 自包含建 code="EUR" 币种（mock 按目标币种 code 匹配），使 refresh 写入 2 条汇率（USD→CNY + USD→EUR）；JPY 不在主数据 → 跳过（部分成功语义）。
- **幂等重写覆盖**：第二次 `refreshRatesFromApi` 按 `(fromCurrencyId, toCurrencyId, validFrom)` upsert 重写同区间汇率行不累积（count 稳定 2→2，对齐 JUnit cache 复用语义）。
- **MIDDLE/SPOT 隔离**：BizModel 写 `rateType="MIDDLE"`，种子 `erp_md_exchange_rate.csv` 用 `SPOT`（validFrom=2026-01-01）→ cleanup 按 `fromCurrencyId=USD AND rateType=MIDDLE` 过滤仅删本 spec 产物，不触碰种子 SPOT 行。
- **已知限制（watch-only residual）**：rate limiting `RATE_LIMITED` 错误浏览器层不复现（webServer JVM 全局单实例 + 令牌桶限流为并发场景，浏览器层单 spec 串行难以稳定复现；JUnit `testRateLimitingTriggersError` 已覆盖）。
