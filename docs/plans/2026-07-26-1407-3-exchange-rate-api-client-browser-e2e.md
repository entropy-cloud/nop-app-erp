# 2026-07-26-1407-3-exchange-rate-api-client-browser-e2e 外部 API 集成汇率查询浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-26
> Source: `docs/backlog/deepening-roadmap.md` §8.5 D1（落地证据声明 JUnit 单层验证，浏览器层未提及）
> Related: `2026-07-21-1206-3-external-api-integration-reference-pattern.md`（D1 后端落地 + 参考实现）/ `docs/architecture/external-api-integration-pattern.md` §7.3 案例 C
> Audit: required

## Current Baseline

- D1 外部 API 集成参考实现已落地（plan 2026-07-21-1206-3）：master-data 汇率查询 API 客户端参考实现（`IErpMdExchangeRateApiClient` SPI + `MockExchangeRateApiClient` + `ErpMdExchangeRateApiClientFactory` config-gated + `IRateLimiter` 令牌桶 + TTL 缓存 + `ErpMdCurrencyBizModel.refreshRatesFromApi(@BizMutation)` 入口 + `IErpMdCurrencyBiz` 接口扩展）。
- JUnit 单层验证齐备：`TestErpMdExchangeRateApiClient` 5 场景全绿（Mock 确定性数据 USD→CNY 7.20 等 / rate limiting RATE_LIMITED 错误 / 缓存 TTL 复用 / refreshRatesFromApi 端到端写入 ErpMdExchangeRate 表 / config-gated 默认 false 抛 ERR_EXCHANGE_RATE_API_UNAVAILABLE）。
- 浏览器层覆盖现状：**零浏览器层覆盖** `refreshRatesFromApi` 入口（grep tests/e2e 无 exchange/currency/rate 相关 spec）。
- config 现状：`erp-md.exchange-rate-api-enabled` + `provider` + `key` + `rate-limit-rps` + `cache-ttl-secs` **均未在** `playwright.config.ts` webServer JVM args 启用（默认 false / 关闭）。
- `refreshRatesFromApi(baseCurrency)` 入参单参数（baseCurrency String，默认 USD），返回 `List<ErpMdExchangeRate>`；读 `ErpMdCurrency.findAll()` 作目标币种集。
- 剩余差距：`refreshRatesFromApi` @BizMutation 经 GraphQL 全栈可达但浏览器层无验证；需启用 config-gate + provider/key 配置方能触达。

## Goals

- 为 D1 汇率查询 API 客户端 `refreshRatesFromApi` 入口补全栈浏览器层 E2E 覆盖，收口「JUnit 单层验证但零浏览器层 E2E」缺口。
- 验证 `refreshRatesFromApi(baseCurrency)` 经 GraphQL `/graphql` 端到端可达：config 启用后 Mock provider 返回确定性汇率 + 写入 `ErpMdExchangeRate` 表（经 `__get` findPage 反查新增行断言 rate/currencyCode 字段）；与 config 关闭路径对照（守卫 token 语义）。
- owner doc `docs/design/master-data/exchange-rate-management.md` §自动汇率刷新（API 客户端，D1）增「浏览器层验证」实现注记。

## Non-Goals

- 真实第三方 provider 接入（D1 Deferred「业务客户接入需求」未触发，归 successor）。
- OAuth2 通用实现 / 多节点 Redis-based rate limiting / `ErpSysExternalSystem` 实体化（D1 Deferred successor，触发条件未满足）。
- rate limiting RATE_LIMITED 错误浏览器层（webServer JVM 全局单实例，令牌桶限流为并发场景，浏览器层单 spec 串行难以稳定复现 RATE_LIMITED；JUnit `testRateLimitingTriggersError` 已覆盖，归 watch-only residual）。
- 生产代码变更（纯测试 + config + 文档）。

## Task Route

- Type: `verification or audit work`（浏览器层 E2E 验证补全，零生产代码变更）
- Owner Docs: `docs/design/master-data/exchange-rate-management.md`（§自动汇率刷新（API 客户端，D1））
- Skill Selection Basis: 匹配 `nop-testing`（Playwright 浏览器层 E2E + 既有 business-actions/_helper 复用 + config-gated 特性 webServer JVM arg 启用范式 + Mock provider 确定性断言），对齐 0500-2 / 0410-2 同型先例。

## Infrastructure And Config Prereqs

- webServer JVM arg 追加 `-Derp-md.exchange-rate-api-enabled=true -Derp-md.exchange-rate-api-provider=mock -Derp-md.exchange-rate-api-key=test-key -Derp-md.exchange-rate-api-rate-limit-rps=100 -Derp-md.exchange-rate-api-cache-ttl-secs=60`（启用 config-gate + Mock provider；rate-limit-rps 调高避免浏览器层单 spec 误触限流；cache-ttl 设 60s 平衡缓存验证与 spec 隔离）。
- 种子 `erp_md_currency.csv` 已含 USD/CNY/EUR 等币种（1234-1 主数据种子），`refreshRatesFromApi` 读 `findAll()` 作目标币种集无需额外种子。
- No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 - Explore + Mock provider 确定性数据核实

Status: completed
Targets: `module-master-data/erp-md-service/src/main/java/app/erp/md/service/exchange/MockExchangeRateApiClient.java`, `module-master-data/erp-md-service/src/main/java/app/erp/md/service/entity/ErpMdCurrencyBizModel.java`, `tests/e2e/business-actions/_helper.ts`
Skill: `nop-testing`

- Item Types: `Decision | Proof`
- Prereqs: 无（独立计划）

- [x] Proof: 核实 `MockExchangeRateApiClient.fetchRates` 确定性汇率表（base USD→targets 的固定值，对齐 JUnit `testMockFetchReturnsDeterministicData`），记录文件行号锚点 + 完整汇率期望值表。
  - Skill: `nop-testing`
- [x] Proof: 核实 `refreshRatesFromApi` 写入 `ErpMdExchangeRate` 字段集（currencyId/rateType/rate/fromCurrencyId/toCurrencyId/validFrom/validTo），记录行号锚点。
  - Skill: `nop-testing`
- [x] Decision: 断言策略裁决——`refreshRatesFromApi` 返回 `List<ErpMdExchangeRate>` 经 GraphQL 字段选择集断言，还是经 `ErpMdExchangeRate__findPage` 反查新增行断言（后者隔离性更强，对齐既有 value-spec 反查范式）。记录选择 + 理由。同时裁决 config 关闭路径覆盖（webServer 全局启用无法 per-spec toggle，对齐 simulation/intercompany config-gate 范式——关闭路径守卫经 JUnit 已覆盖，浏览器层不重复）。
  - Skill: `nop-testing`

Exit Criteria:

> 本阶段交付 Mock provider 确定性数据核实 + 断言策略裁决，解除 Phase 2 实施阻塞。

- [x] Mock 汇率期望值表 + 写入字段行号锚点 + 断言策略裁决落盘 plan Execution Decision 段

#### Execution Decisions (Phase 1)

**Proof 1 — Mock 确定性汇率表**（`MockExchangeRateApiClient.java:29-47`，`MOCK_RATES` 静态初始化块）：

| baseCurrency | targetCurrency | rate | 行号锚点 |
| --- | --- | --- | --- |
| USD | CNY | 7.20 | :32 |
| USD | EUR | 0.92 | :33 |
| USD | JPY | 150.00 | :34 |
| USD | USD | 1.00 | :35 |
| CNY | USD | 0.139 | :39 |
| EUR | USD | 1.087 | :44 |

对齐 JUnit `testMockFetchReturnsDeterministicData`（USD→CNY 7.20 / USD→EUR 0.92 / USD→JPY 150.00，GBP 缺键）。`asOfDate` 不参与 mock 计算（:58 仅日志可追溯）。

**Proof 2 — `refreshRatesFromApi` 写入 `ErpMdExchangeRate` 字段集**（`ErpMdCurrencyBizModel.java:99-107`）：

| 字段 | 写入值 | 行号锚点 |
| --- | --- | --- |
| `fromCurrencyId` | `baseCurrencyEntity.getId()` | :101 |
| `toCurrencyId` | `targetCurrency.getId()` | :102 |
| `rateType` | 固定字符串 `"MIDDLE"` | :103 |
| `validFrom` | `today`（`LocalDate.now()`） | :104（值源 :85） |
| `validTo` | `today.plusDays(1)` | :105（值源 :86） |
| `rate` | `rates.get(targetCode)`（mock 确定性值） | :107 |

幂等 upsert 键 `(fromCurrencyId, toCurrencyId, validFrom)`（:96-97 `findExistingRate`，:120-129 三元组查询）；第二次 refresh 同区间走 update 不新增行。

**Decision 1 — 断言策略裁决**：采用 **双层断言**（layered，对齐 nop-testing 三层验证模型）：
- **层 1（返回值字段选择集）**：`refreshRatesFromApi(baseCurrency:"USD")` GraphQL mutation 返回 `List<ErpMdExchangeRate>` 选择 `rate fromCurrencyId toCurrencyId rateType validFrom`，逐条断言 mock 确定性 rate 值（USD→CNY 7.20 / USD→EUR 0.92）。
- **层 2（findPage 反查持久化）**：经 `ErpMdExchangeRate__findPage` 按 `fromCurrencyId` 过滤反查新增行，关联 `ErpMdCurrency.code` 断言 `fromCurrencyCode=USD` / `toCurrencyCode` ∈ {CNY,EUR} / `rate` 字段持久化。
- 理由：返回值断言证明 mutation 全栈可达 + rate 派生正确；findPage 反查独立证明持久化写入（绕过 mutation 返回值的内存投影，对齐既有 `findItems`/`findFirst` 反查范式）。

**Decision 2 — config 关闭路径覆盖**：**浏览器层不覆盖**。
- 理由：webServer JVM args 全局启用 `erp-md.exchange-rate-api-enabled=true`（Phase 2），无法 per-spec toggle 关闭；关闭路径守卫（`ERR_EXCHANGE_RATE_API_UNAVAILABLE`）经 JUnit `testConfigGatedDefaultDisabled` 已覆盖。
- 对齐既有 config-gated 特性 webServer JVM arg 范式（simulation/intercompany/consolidation-elimination/budget-roll-forward 等：全局启用 + 关闭守卫归 JUnit，浏览器层不重复）。

**Decision 3 — 种子币种基线修正 + 自包含 EUR setup**：
- 实测 `app-erp-all/src/main/resources/_vfs/_init-data/erp_md_currency.csv` 仅含 2 币种：CNY(id=1) + USD(id=2)。**EUR 不在种子**（本计划 Current Baseline §「已含 USD/CNY/EUR」描述不准确，实测以仓库为准）。
- 裁决：spec setup 经 `ErpMdCurrency__save` 自包含建测试专用 EUR 币种（唯一 code `E2E-EUR-{ts}`，symbol=EUR），使 `refreshRatesFromApi("USD")` 写入 2 条汇率（USD→CNY 7.20 + USD→EUR 0.92），对齐 JUnit `testRefreshRatesFromApiWritesExchangeRate` 期望值表；JPY 不在主数据 → 跳过（部分成功语义）。setup/cleanup 镜像 `fin-budget-rollforward-carryforward.action.spec.ts` 自包含隔离范式（cleanup 按反依赖链：rate → EUR currency）。

### Phase 2 - spec 实现 + webServer config 启用

Status: completed
Targets: `tests/e2e/business-actions/md-exchange-rate-api.action.spec.ts`, `tests/e2e/business-actions/_helper.ts`, `playwright.config.ts`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] Add: `playwright.config.ts` webServer JVM args 追加 5 项 exchange-rate-api config（enabled/provider/key/rate-limit-rps/cache-ttl-secs）。
  - Skill: `none`
- [x] Add: `_helper.ts` 新增 `findExchangeRatesByBase(page, baseCurrencyId, selection)` 反查原语（经 `ErpMdExchangeRate__findPage` 按 fromCurrencyId 过滤 + 关联 ErpMdCurrency code 断言），对齐既有 `findItems` 范式。
  - Skill: `nop-testing`
- [x] Add: 新建 `md-exchange-rate-api.action.spec.ts`（用例覆盖 refreshRatesFromApi 正路径 + 字段断言）：(1) `refreshRatesFromApi(baseCurrency:"USD")` 返回非空 List + 逐条断言 Mock 确定性汇率值（rate 字段匹配 JUnit 期望值表）；(2) 经 `ErpMdExchangeRate__findPage` 反查新增行断言 currencyCode/rate/fromCurrencyCode/toCurrencyCode 字段；(3) 幂等/重写覆盖断言（第二次 refreshRatesFromApi 重写同区间汇率行不累积，对齐 JUnit cache 复用语义）。字段翻转/写入均经 `__get`/findPage 独立断言。
  - Skill: `nop-testing`
- [x] Proof: 运行新 spec 全绿 + 既有 master-data 抽样回归（`master-data.write.spec.ts` + `md-*` value spec）0 新增失败（config 启用对既有 master-data 链路零回归）。
  - Skill: `nop-testing`

Exit Criteria:

> 本阶段交付 spec 全绿 + config-gate 启用 + 回归零新增失败，解除 Phase 3 owner-doc 对齐阻塞。

- [x] spec 全绿（指定成功 + 失败模式：Mock 确定性汇率值断言 + findPage 反查 + 幂等重写）
- [x] 既有 master-data spec 回归 0 新增失败（config-gate 启用对既有链路零回归）

#### Phase 2 Verification Evidence

- 新 spec `md-exchange-rate-api.action.spec.ts`：1 passed (8.1s) — refreshRatesFromApi(USD) 返回 2 条（USD→CNY 7.20 / USD→EUR 0.92）+ rateType=MIDDLE + findPage 反查 fromCurrency/toCurrency code 断言 + 幂等重写 count 稳定（2→2）。
- master-data 回归抽样 11 passed (2.1m)：`master-data.write.spec.ts`（CRUD write cycle）+ `master-data.list-value.spec.ts`（findPage seed tokens）+ `dashboards/master-data.value.spec.ts`（KPI + 2 预警）+ `visual/field-format.value.spec.ts`（含 `/ErpMdExchangeRate-main` 8 位精度渲染依赖种子 SPOT 7.25 行，MIDDLE/today 行 cleanup 按 rateType 隔离不触碰种子）。config-gate 启用对既有 master-data 链路零回归。

### Phase 3 - owner doc 回链 + e2e-runbook + 日志

Status: completed
Targets: `docs/design/master-data/exchange-rate-management.md`, `docs/architecture/external-api-integration-pattern.md`, `docs/testing/e2e-runbook.md`, `docs/logs/2026/07-26.md`
Skill: `nop-testing`

- Item Types: `Add`
- Prereqs: Phase 2

- [x] Add: `docs/design/master-data/exchange-rate-management.md` §自动汇率刷新（API 客户端，D1）增「浏览器层验证」实现注记（Mock provider 确定性断言 + findPage 反查 + config-gate 启用 + 幂等重写）。
  - Skill: `none`
- [x] Add: `docs/testing/e2e-runbook.md` webServer JVM arg 段补 5 项 exchange-rate-api config + 业务动作表新增 master-data 汇率查询 API 行 + spec 计数增量 + 已知限制（rate limiting RATE_LIMITED 浏览器层不复现，JUnit 覆盖）。
  - Skill: `none`
- [x] Add: `docs/logs/2026/07-26.md` 追加本计划日志条目（任务/Phase 摘要/验证 full-green/Skill）。
  - Skill: `none`

Exit Criteria:

> 本阶段交付 owner-doc 对齐 + 日志。完整仓库验证属 Closure Gates。

- [x] owner doc + e2e-runbook + 日志三处更新落地

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (ses_062f3d7a6ffeVCz6GIYmBkyJe5) — baseline 全部经实时仓库核实（refreshRatesFromApi 方法签名 + 5 config keys + Mock 确定性汇率表 USD→CNY 7.20 等 + JUnit 5 场景 + 浏览器层零覆盖 grep 确认）；模板/规则合规；无阻塞项。可直接进入实施。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。

- [x] 范围内行为完成（spec 全绿，覆盖 refreshRatesFromApi 正路径 + findPage 反查 + 幂等重写）
- [x] 相关文档对齐（exchange-rate-management.md + e2e-runbook + 日志）
- [x] 已运行验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + 新 spec 全绿 + 既有 master-data spec 回归 0 新增失败（纯测试 + config + 文档，零生产代码变更）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### rate limiting RATE_LIMITED 错误浏览器层路径

- Classification: `watch-only residual`
- Why Not Blocking Closure: webServer JVM 全局单实例，令牌桶限流为并发场景，浏览器层单 spec 串行难以稳定复现 RATE_LIMITED（需快速连续调用触达令牌桶阈值）；JUnit `testRateLimitingTriggersError` 已覆盖 RATE_LIMITED 错误路径。
- Successor Required: `no`（触发条件：浏览器层需稳定复现限流场景时，可引入 per-spec rate-limit config toggle）

### 真实第三方 provider 接入 / OAuth2 通用实现 / 多节点 Redis-based rate limiting

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: D1 Deferred successor —— 触发条件均未满足（真实 provider 需业务客户接入需求 / OAuth2 需跨域统一 auth 需求 / 多节点限流需生产多节点部署 + 不一致问题）。
- Successor Required: `yes`（各独立触发条件）

## Closure

Status Note: 全 3 Phase 执行完成 + 独立结束审计 PASS。`refreshRatesFromApi` `@BizMutation` 浏览器层 E2E 覆盖落地（Mock 确定性汇率双层断言 + findPage 反查 + 幂等重写），收口「JUnit 单层验证但零浏览器层 E2E」缺口。纯测试 + config + 文档，零生产代码变更。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话 ses_0610e5c84ffe8QOhYOU0vM427h，general agent，未参与执行）
- Verdict: **PASS**（5 验证域全通过：plan 一致性 + 交付物存在 + 文档对齐 + 零生产代码变更 + 正确性 spot-check 含 MIDDLE/SPOT 隔离 + 自包含 EUR setup + Mock 确定性汇率未变）
- Execution Evidence:
  - `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（1:31 min）
  - 新 spec `md-exchange-rate-api.action.spec.ts` 1 passed (8.1s)
  - master-data 回归抽样 11 passed (2.1m)（含 `field-format.value.spec.ts` `/ErpMdExchangeRate-main` 8 位精度渲染依赖种子 SPOT 7.25 行——MIDDLE/today 行 cleanup 按 rateType 隔离不触碰种子）
- 日志：`docs/logs/2026/07-26.md` 顶部 1407-3 条目

Follow-up:

- 无非阻塞跟进项（已确认的缺陷不得出现在此处）
