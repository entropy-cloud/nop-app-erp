# 2026-07-26-1407-3-exchange-rate-api-client-browser-e2e 外部 API 集成汇率查询浏览器层 E2E

> Plan Status: active
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

Status: planned
Targets: `module-master-data/erp-md-service/src/main/java/app/erp/md/service/exchange/MockExchangeRateApiClient.java`, `module-master-data/erp-md-service/src/main/java/app/erp/md/service/entity/ErpMdCurrencyBizModel.java`, `tests/e2e/business-actions/_helper.ts`
Skill: `nop-testing`

- Item Types: `Decision | Proof`
- Prereqs: 无（独立计划）

- [ ] Proof: 核实 `MockExchangeRateApiClient.fetchRates` 确定性汇率表（base USD→targets 的固定值，对齐 JUnit `testMockFetchReturnsDeterministicData`），记录文件行号锚点 + 完整汇率期望值表。
  - Skill: `nop-testing`
- [ ] Proof: 核实 `refreshRatesFromApi` 写入 `ErpMdExchangeRate` 字段集（currencyId/rateType/rate/fromCurrencyId/toCurrencyId/validFrom/validTo），记录行号锚点。
  - Skill: `nop-testing`
- [ ] Decision: 断言策略裁决——`refreshRatesFromApi` 返回 `List<ErpMdExchangeRate>` 经 GraphQL 字段选择集断言，还是经 `ErpMdExchangeRate__findPage` 反查新增行断言（后者隔离性更强，对齐既有 value-spec 反查范式）。记录选择 + 理由。同时裁决 config 关闭路径覆盖（webServer 全局启用无法 per-spec toggle，对齐 simulation/intercompany config-gate 范式——关闭路径守卫经 JUnit 已覆盖，浏览器层不重复）。
  - Skill: `nop-testing`

Exit Criteria:

> 本阶段交付 Mock provider 确定性数据核实 + 断言策略裁决，解除 Phase 2 实施阻塞。

- [ ] Mock 汇率期望值表 + 写入字段行号锚点 + 断言策略裁决落盘 plan Execution Decision 段

### Phase 2 - spec 实现 + webServer config 启用

Status: planned
Targets: `tests/e2e/business-actions/md-exchange-rate-api.action.spec.ts`, `tests/e2e/business-actions/_helper.ts`, `playwright.config.ts`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [ ] Add: `playwright.config.ts` webServer JVM args 追加 5 项 exchange-rate-api config（enabled/provider/key/rate-limit-rps/cache-ttl-secs）。
  - Skill: `none`
- [ ] Add: `_helper.ts` 新增 `findExchangeRatesByBase(page, baseCurrencyCode)` 反查原语（经 `ErpMdExchangeRate__findPage` 按 fromCurrencyId 过滤 + 关联 ErpMdCurrency code 断言），对齐既有 `findItems` 范式。
  - Skill: `nop-testing`
- [ ] Add: 新建 `md-exchange-rate-api.action.spec.ts`（用例覆盖 refreshRatesFromApi 正路径 + 字段断言）：(1) `refreshRatesFromApi(baseCurrency:"USD")` 返回非空 List + 逐条断言 Mock 确定性汇率值（rate 字段匹配 JUnit 期望值表）；(2) 经 `ErpMdExchangeRate__findPage` 反查新增行断言 currencyCode/rate/fromCurrencyCode/toCurrencyCode 字段；(3) 幂等/重写覆盖断言（第二次 refreshRatesFromApi 重写同区间汇率行不累积，对齐 JUnit cache 复用语义）。字段翻转/写入均经 `__get`/findPage 独立断言。
  - Skill: `nop-testing`
- [ ] Proof: 运行新 spec 全绿 + 既有 master-data 抽样回归（`master-data.write.spec.ts` + `md-*` value spec）0 新增失败（config 启用对既有 master-data 链路零回归）。
  - Skill: `nop-testing`

Exit Criteria:

> 本阶段交付 spec 全绿 + config-gate 启用 + 回归零新增失败，解除 Phase 3 owner-doc 对齐阻塞。

- [ ] spec 全绿（指定成功 + 失败模式：Mock 确定性汇率值断言 + findPage 反查 + 幂等重写）
- [ ] 既有 master-data spec 回归 0 新增失败（config-gate 启用对既有链路零回归）

### Phase 3 - owner doc 回链 + e2e-runbook + 日志

Status: planned
Targets: `docs/design/master-data/exchange-rate-management.md`, `docs/architecture/external-api-integration-pattern.md`, `docs/testing/e2e-runbook.md`, `docs/logs/2026/07-26.md`
Skill: `nop-testing`

- Item Types: `Add`
- Prereqs: Phase 2

- [ ] Add: `docs/design/master-data/exchange-rate-management.md` §自动汇率刷新（API 客户端，D1）增「浏览器层验证」实现注记（Mock provider 确定性断言 + findPage 反查 + config-gate 启用 + 幂等重写）。
  - Skill: `none`
- [ ] Add: `docs/testing/e2e-runbook.md` webServer JVM arg 段补 5 项 exchange-rate-api config + 业务动作表新增 master-data 汇率查询 API 行 + spec 计数增量 + 已知限制（rate limiting RATE_LIMITED 浏览器层不复现，JUnit 覆盖）。
  - Skill: `none`
- [ ] Add: `docs/logs/2026/07-26.md` 追加本计划日志条目（任务/Phase 摘要/验证 full-green/Skill）。
  - Skill: `none`

Exit Criteria:

> 本阶段交付 owner-doc 对齐 + 日志。完整仓库验证属 Closure Gates。

- [ ] owner doc + e2e-runbook + 日志三处更新落地

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (ses_062f3d7a6ffeVCz6GIYmBkyJe5) — baseline 全部经实时仓库核实（refreshRatesFromApi 方法签名 + 5 config keys + Mock 确定性汇率表 USD→CNY 7.20 等 + JUnit 5 场景 + 浏览器层零覆盖 grep 确认）；模板/规则合规；无阻塞项。可直接进入实施。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。

- [ ] 范围内行为完成（spec 全绿，覆盖 refreshRatesFromApi 正路径 + findPage 反查 + 幂等重写）
- [ ] 相关文档对齐（exchange-rate-management.md + e2e-runbook + 日志）
- [ ] 已运行验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + 新 spec 全绿 + 既有 master-data spec 回归 0 新增失败（纯测试 + config + 文档，零生产代码变更）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

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

Status Note: <待执行 + 独立结束审计后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立审计>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- 无非阻塞跟进项（已确认的缺陷不得出现在此处）
