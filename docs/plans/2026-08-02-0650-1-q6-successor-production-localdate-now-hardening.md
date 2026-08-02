# 2026-08-02-0650-1 Q6 successor — 生产侧 LocalDate.now() 残留硬化

> Plan Status: completed
> Last Reviewed: 2026-08-02
> Source: `docs/backlog/audit-remediation-roadmap.md` MQ Q6（successor 行：生产 `LocalDate.now()` 残留 2 处）
> Related: `docs/plans/2026-08-01-1357-1-mq-q6-clock-test-infrastructure-impl.md`（Q6 Phase 2 实现，已 completed）；`docs/architecture/quality-engineering/clock-test-infrastructure.md` §1.1 + §7
> Mission: audit-remediation
> Work Item: Q6 successor（生产侧 LocalDate.now() 残留硬化）
> Audit: required

## Current Baseline

Q6（时钟测试基础设施硬化）已在 plan `2026-08-01-1357-1` 完成**测试侧**根治：`ThreadLocalFrozenClock`（路径 C 应用层 thread-local delegating clock）落地，15 域子类静态旁路迁移完成，月初翻车税根因（测试侧未冻结时钟 + 全局静态并行不安全）已闭合。但 Q6 设计文档 §1.1 显式登记 2 处**生产侧** `LocalDate.now()` 直调残留为 watch-only successor，本期范围外。

**实仓复核（Q6 §1.1 核验命令 `rg -rn "LocalDate\.now\(\)" --glob '*.java' module-*/erp-*-service/src/main/java`，2026-08-02 复跑）确认恰好 2 处残留**：

1. `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinVoucherTemplateRenderTemplateProcessor.java:75` — `LocalDate today = LocalDate.now();`，用于 `findActiveTemplate` 按 `validFrom`/`validTo` 有效期过滤凭证模板（日期敏感，月初/年末边界可能选中不同模板）。
2. `module-master-data/erp-md-service/src/main/java/app/erp/md/service/processor/ErpMdCurrencyRefreshRatesFromApiProcessor.java:39` — `LocalDate today = LocalDate.now();`，作为汇率刷新的 rate date（`fetchRates(base, targets, today)`）+ upsert 的 `validFrom`/`validTo`（`validTo = today.plusDays(1)`）。

**规范替换入口已确立**：7 处 peer 生产代码已用 `CoreMetrics.today()`（`NotesPostingDispatcher:79,108`、`CommitmentVoucherGenerator:136,209`、`ErpFinBudgetControlBiz:204`、`BudgetVoucherGenerator:216`、`MockExchangeRateApiClient:58`；`CoreMetrics.today()` 在 service src/main 出现 100+ 次）。两处残留文件均**仅** `import java.time.LocalDate`，未 `import io.nop.api.core.time.CoreMetrics`。

**测试覆盖缺口（精确）**：
- finance：`TestErpFinVoucherTemplateExpr` 测表达式求值，**零触达** `findActiveTemplate` 的 `validFrom`/`validTo` 日期过滤——日期敏感路径完全无覆盖。
- master-data：`TestErpMdExchangeRateApiClient.testRefreshRatesFromApiWritesExchangeRate`（:161-193）**端到端触达** Processor 的 `today` upsert 路径（`currencyBiz.refreshRatesFromApi("USD", CTX)` :174），但**仅断言** count/rate 值（7.20/0.92），**未冻结时钟**且**未断言** `validFrom`/`validTo` 日期字段——故日期确定性（`validFrom == today` / `validTo == today+1` 走墙钟而非冻结值）无覆盖。新增测试的**区分价值**是「冻结时钟下的日期确定性 + `validFrom`/`validTo` 显式断言」，而非「触达路径」（路径已被场景 4 覆盖）。

**测试侧冻结机制可用**：Q6 路径 C `ThreadLocalFrozenClock`（`module-common-test`，`ensureRegistered()` + `install(LocalDate)` + `clear()`）是**线程本地、域无关**的——`currentDate()`/`currentDateTime()` 委托线程本地冻结值，`currentTimeMillis()`/`nanoTime()` 始终走真实系统时钟。故 master-data（Q6 §1.2 列为 4 个无 `*FrozenClockExtension` 子类的模块之一）的测试**无需新建域子类**，直接用 `ThreadLocalFrozenClock` 即可冻结。

**剩余差距**：2 处生产残留 + 2 处日期路径零直接测试 + Q6 设计文档 successor 行待闭合回填。

## Goals

- 将 2 处生产 `LocalDate.now()` 替换为 `CoreMetrics.today()`（生产运行时行为完全不变——非冻结时 `CoreMetrics.today()` 委托 `DEFAULT_CLOCK` == `LocalDate.now()`；替换的唯一效果是使这两条路径在测试中可被 `ThreadLocalFrozenClock` 冻结为确定性日期）。
- 为两条日期敏感路径补聚焦测试：冻结时钟到有效期/汇率日期边界，断言日期敏感分支行为（凭证模板按 `validFrom`/`validTo` 过滤；汇率 upsert 的 `validFrom`/`validTo` = today/today+1）。
- 闭合 Q6 设计文档 §1.1 残留段 + §7 successor 行（标记残留已消解，回填本 plan 证据）。

## Non-Goals

- **不**做超出这 2 处的全仓 `LocalDate.now()` 扫荡——Q6 §1.1 核验命令已确认 `module-*/erp-*-service/src/main/java` 仅此 2 处生产残留；其余 `LocalDate.now()`（若存在于 web/app/codegen 等非 service 层或生成包）不在 Q6 同族日期敏感范围。
- **不**新建 master-data `MdFrozenClockExtension` 子类——Q6 §1.2 的「4 模块无子类」是独立的 Q6 范围，本计划用域无关的 `ThreadLocalFrozenClock` 覆盖即可。
- **不**改动测试侧 `ThreadLocalFrozenClock` 本身（Q6 Phase 2 已落地）。
- **不**触及生产过账链路时钟入口（Q6 §1.1 已确认过账链路合规，如 `NotesPostingDispatcher`、`ErpFinPostingProcessor` 均用 `CoreMetrics.today()`）。
- **不**扩展到其余 watch-only Q6 successor（C-3 并发 CI 矩阵 / 路径 A 平台 scope API 上游贡献——触发条件未满足）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/quality-engineering/clock-test-infrastructure.md`（§1.1 残留登记 + §7 successor 行）；`docs/design/finance/`（凭证模板语义）；`docs/design/master-data/`（币种/汇率语义）
- Skill Selection Basis: 触及生产 Processor 代码（R6.x per-mutation Processor），匹配 `nop-backend-dev`（决策门 + 跨实体调用 + 产品化可定制性自检）；新增聚焦测试匹配 `nop-testing`（测试基类选择 + `ThreadLocalFrozenClock` 冻结机制）。两 skill 的必需输入（owner doc、设计文档、实仓代码）均已就绪。

## Infrastructure And Config Prereqs

- 无超出既有基线的基础设施依赖。
- Q6 `ThreadLocalFrozenClock`（`module-common-test`）已落地，测试侧冻结机制可用。
- `module-common-test` 已是 finance/master-data service 测试的传递依赖（Q6 测试已消费 `ThreadLocalFrozenClock`）。

## Execution Plan

### Phase 1 — 生产 LocalDate.now() → CoreMetrics.today() + 聚焦日期边界测试 + 文档闭合

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinVoucherTemplateRenderTemplateProcessor.java`；`module-master-data/erp-md-service/src/main/java/app/erp/md/service/processor/ErpMdCurrencyRefreshRatesFromApiProcessor.java`；新增 2 个测试类；`docs/architecture/quality-engineering/clock-test-infrastructure.md`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Q6 Phase 2 done（plan `2026-08-01-1357-1`，已 completed）

- [x] `Fix` — finance `ErpFinVoucherTemplateRenderTemplateProcessor`：`import io.nop.api.core.time.CoreMetrics;`；`findActiveTemplate` 内 `LocalDate today = LocalDate.now();` → `LocalDate today = CoreMetrics.today();`
  - Skill: `nop-backend-dev`
- [x] `Fix` — master-data `ErpMdCurrencyRefreshRatesFromApiProcessor`：`import io.nop.api.core.time.CoreMetrics;`；`refreshRatesFromApi` 内 `LocalDate today = LocalDate.now();` → `LocalDate today = CoreMetrics.today();`
  - Skill: `nop-backend-dev`
- [x] `Proof` — 新增 finance 聚焦测试（如 `TestErpFinVoucherTemplateActiveByDate`）：用 `ThreadLocalFrozenClock.ensureRegistered()` + `install(boundaryDate)`，覆盖 (a) `validTo = 冻结日-1` 的模板被排除；(b) `validFrom = 冻结日+1` 的模板被排除；(c) 跨冻结日的模板被选中。`afterEach` `clear()`。验证命令：`mvn -pl module-finance/erp-fin-service -am test -Dtest=TestErpFinVoucherTemplateActiveByDate`。
  - Skill: `nop-testing`
- [x] `Proof` — 新增 master-data 聚焦测试（如 `TestErpMdCurrencyRefreshRatesDate`）：注入 mock `ErpMdExchangeRateApiClientFactory`（返回固定汇率 map）+ `ThreadLocalFrozenClock.install(frozenDate)`，断言 upsert 的 `ErpMdExchangeRate.validFrom == frozenDate` 且 `validTo == frozenDate.plusDays(1)`；切换 `install(otherDate)` 复跑断言日期随之改变（证明走冻结时钟而非墙钟）。验证命令：`mvn -pl module-master-data/erp-md-service -am test -Dtest=TestErpMdCurrencyRefreshRatesDate`。
  - Skill: `nop-testing`
- [x] `Fix` — Q6 设计文档 `clock-test-infrastructure.md` §1.1 残留段：将「残留 `LocalDate.now()` 直调（范围外，登记 successor）」段标注为已由本 plan 消解（保留历史段落 + 追加闭合注记 + 本 plan 引用）；§7 successor 表对应行状态翻转为「已闭合」+ 闭合回填。
  - Skill: none

Exit Criteria:

- [x] `rg -rn "LocalDate\.now\(\)" --glob '*.java' module-finance/erp-fin-service/src/main/java module-master-data/erp-md-service/src/main/java` 返回 0 命中（2 处残留全消解）。
- [x] 2 个新增聚焦测试在冻结时钟下通过（断言日期敏感分支按冻结日而非墙钟判定），且 `clear()` 后无线程本地泄漏。
- [x] Q6 设计文档 §1.1 + §7 successor 行闭合回填一致。

## Draft Review Record

- Independent draft review iteration 1: `needs-revision`（`ses_03f4cc9b3ffex7G4I1WicSfgLb`，独立子代理 fresh session）— 0 BLOCKER / 1 MAJOR / 2 MINOR。全部事实声明经 live 仓库核验通过（scope 恰好 2 处、line 号 + 逻辑、CoreMetrics.today peer 模式、ThreadLocalFrozenClock API、successor 触发条件满足、单结果表面、反松弛/退出标准质量 STRONG、worth-drafting=YES）。MAJOR：master-data 既有测试特征化事实错误——`TestErpMdExchangeRateApiClient.testRefreshRatesFromApiWritesExchangeRate:161-193` **端到端触达** Processor upsert 路径（:174 调 `refreshRatesFromApi`），仅未冻结时钟 + 未断言 `validFrom`/`validTo`（原计划误称"不触达路径"）。MINOR：peer 计数 8→7（off-by-one）；Q6-doc closure 项 typing（非阻塞）。修订：§Current Baseline master-data 测试缺口精确化（路径已触达，缺口=日期确定性断言）+ peer 计数 8→7 + 明示新增测试区分价值（冻结时钟日期确定性，非触达路径）。
- Independent draft review iteration 2: `accept`（`ses_03f498a0affeN7IwKeGuiqGE`，独立子代理 fresh session 确认轮）— iter-1 MAJOR + 2 MINOR 全部 resolved 并经 live 仓库复核；修订未引入新问题；一致性全 PASS（master-data 特征化准确 / Proof 项区分价值对齐 / peer 计数 7 正确 / scope 仍恰好 2 处 / 文本一致）。结论：计划已收敛为可接受执行契约，建议翻转为 `active`。已据此翻转 `Plan Status: draft → active`。

## Closure Gates

- [x] 范围内行为完成（2 处生产替换 + 2 聚焦测试 + 文档闭合）
- [x] 相关文档对齐（Q6 设计文档 §1.1/§7 闭合回填）
- [x] 已运行验证：`mvn -pl module-finance/erp-fin-service -am test` + `mvn -pl module-master-data/erp-md-service -am test` 全绿；Closure 时全量 `mvn clean install -DskipTests`（156 模块）+ `mvn test` 全绿 + compliance checker exit 0 + i18n checker exit 0
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中
- [x] 生产运行时行为零变更已核实（非冻结运行时 `CoreMetrics.today()` == `LocalDate.now()`）

## Deferred But Adjudicated

### 全仓其余 LocalDate.now()（非 service 层 / 生成包）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: Q6 §1.1 核验命令 scope 为 `module-*/erp-*-service/src/main/java`（生产业务逻辑层）；本计划继承该 scope。web/app/codegen 等层或 `_gen` 包内若存在 `LocalDate.now()` 不属 Q6 同族日期敏感问题（非业务日期判定）。
- Successor Required: no — 触发：若未来审计发现非 service 层有日期敏感 `LocalDate.now()` 影响业务行为，开独立计划。

### C-3 并发 CI 矩阵 / 路径 A 平台 scope API

- Classification: `watch-only successor`
- Why Not Blocking Closure: Q6 §7 已登记，触发条件（surefire 切 `parallel=methods` / nop-entropy 接受 scope API PR）未满足。
- Successor Required: yes — 触发条件见 Q6 §7。

## Closure

Status Note: executed 2026-08-02；独立结束审计由 fresh sub-agent session（GLM-5.2 closure auditor，本次会话）执行并通过——见下方 Closure Audit Evidence。

Closure Audit Evidence:

- Executor / Agent: opencode executor（GLM-5.2），执行日期 2026-08-02。
- 生产替换（2 处）：
  - `module-finance/.../ErpFinVoucherTemplateRenderTemplateProcessor.java:76` — `LocalDate.now()` → `CoreMetrics.today()`（+ `import io.nop.api.core.time.CoreMetrics;`）
  - `module-master-data/.../ErpMdCurrencyRefreshRatesFromApiProcessor.java:40` — 同上
  - 残留核验：`rg -rn "LocalDate\.now\(\)" --glob '*.java' module-finance/erp-fin-service/src/main/java module-master-data/erp-md-service/src/main/java` → 0 命中（exit 1）。
- 新增聚焦测试（2 个）：
  - `TestErpFinVoucherTemplateActiveByDate`（4 用例：past/future/active 边界 + 非冻结委托墙钟反向证明）— `mvn -pl module-finance/erp-fin-service -am test -Dtest=TestErpFinVoucherTemplateActiveByDate` → Tests run: 4, Failures: 0, Errors: 0。
  - `TestErpMdCurrencyRefreshRatesDate`（双冻结日 validFrom/validTo 确定性断言）— `mvn -pl module-master-data/erp-md-service -am test -Dtest=TestErpMdCurrencyRefreshRatesDate` → Tests run: 1, Failures: 0, Errors: 0。
- 既有快照测试日期硬化（Q6 同族，闭合门控必需）：
  - `TestErpMdExchangeRateApiClient` 原未冻结时钟，其 `testRefreshRatesFromApiWritesExchangeRate` 的 DB 快照（`VALID_FROM=2026-08-01`）随墙钟漂移——经 git stash 复核确认在 HEAD（原 `LocalDate.now()`）下今日（2026-08-02）即红（`value=2026-08-02, expected=2026-08-01`），为本 plan 生产路径的同族日期脆弱性。本 plan 用域无关 `ThreadLocalFrozenClock`（冻结到 `2026-08-01`，**未**新建 master-data `MdFrozenClockExtension` 子类，符合 non-goal）硬化该测试，使其确定性（零快照重录，冻结日即快照参考日）。这使 `mvn test` 全绿的门控可达成。
- 文档闭合：Q6 设计文档 `clock-test-infrastructure.md` §1.1 追加闭合回填段 + §7 successor 行翻转为「已闭合」。
- 全量验证（2026-08-02）：
  - `mvn clean install -DskipTests` → BUILD SUCCESS（含 app-erp-all，全 reactor）。
  - `mvn test` → 3966 tests / 0 failures / 0 errors / 2 skipped。
  - `bash docs/audits/nop-compliance-checker.sh` → exit 0（R12c AcctSchemaResolver=40 vs 基线 38 为本 plan **之前**的既有漂移：git stash 复核 HEAD 上 AcctSchemaResolver import=41，本 plan 仅新增 `CoreMetrics` import，非 3 共享类型之一，不触发 actual>baseline）。
  - `bash docs/audits/i18n-coverage-checker.sh` → exit 0（PASS，0 defects / 0 gaps）。
- 生产运行时行为零变更核验：非冻结时 `ThreadLocalFrozenClock.currentDate()` 委托 `CoreMetrics.defaultClock().currentDate()` == `LocalDate.now()`；`TestErpFinVoucherTemplateActiveByDate.testNoFreezeDelegatesToWallClock` 显式断言二者相等。

Auditor / Agent: 独立结束审计子代理（GLM-5.2 closure auditor，fresh session `MISSION_DRIVER:2026-08-02-065047-mission-driver`，2026-08-02）。审计范围与结论：
- Phase 状态/退出标准一致性：Phase 1 `Status: completed`，全部 3 项 Exit Criteria 已 `[x]`，5 个执行项全 `[x]`，无遗留 `- [ ]`。
- Exit Criteria vs live repo（实仓复核，2026-08-02）：
  - `rg "LocalDate\.now\(\)" module-finance/erp-fin-service/src/main/java module-master-data/erp-md-service/src/main/java` → 0 命中（exit 1）✓
  - 两处生产代码已改为 `CoreMetrics.today()`：`ErpFinVoucherTemplateRenderTemplateProcessor.java:76` + `ErpMdCurrencyRefreshRatesFromApiProcessor.java:40` ✓
  - 两聚焦测试类存在且非空壳：`TestErpFinVoucherTemplateActiveByDate`（4 用例：past/future/active 边界 + 反向证明非冻结委托墙钟，真注入 processor 并调 `renderTemplate`）+ `TestErpMdCurrencyRefreshRatesDate`（双冻结日 validFrom/validTo 确定性断言，真调 `currencyBiz.refreshRatesFromApi`）✓
  - Q6 设计文档闭合回填已落地：`clock-test-infrastructure.md:44`（§1.1 追加闭合段）+ `:409`（§7 successor 行翻转为「已闭合」）✓
- Anti-Hollow：测试真注入 `ErpFinVoucherTemplateRenderTemplateProcessor` / `IErpMdCurrencyBiz` 并调用其生产方法，断言日期敏感分支行为，非空体/非 `return null`/非吞异常 ✓
- 五点一致性：Plan Status / Phase Status / Exit Criteria / Closure Gates / Closure evidence 全 `completed`/`[x]`，无冲突 ✓
- Deferred honesty：2 项 deferred 项均为合法 `watch-only`/`out-of-scope`（非 service 层 `LocalDate.now()` + C-3 并发 CI 矩阵），无已确认 live defect 被隐藏 ✓
- Docs sync：`docs/architecture/quality-engineering/clock-test-infrastructure.md` §1.1 + §7 已更新；日志条目见 `docs/logs/2026/08-02.md`（executor 维护）✓
- 结论：APPROVED，全部范围内项已 landed 并经实仓复核，可闭合。

Follow-up:

- 无已确认缺陷（本计划为 successor 闭合型，无 live defect）。
