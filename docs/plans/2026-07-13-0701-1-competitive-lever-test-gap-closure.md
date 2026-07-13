# 2026-07-13-0701-1-competitive-lever-test-gap-closure 竞争杠杆测试缺口闭合（多套账集成测试 + 成本策略单测 + 过账兜底扫描单测）

> Plan Status: completed
> Last Reviewed: 2026-07-13
> Source: `docs/audits/2026-07-12-1504-competitive-levers-implementation-audit.md` §五 低（L-1/L-2/L-3，三项测试覆盖缺口）
> Related: `2026-07-12-1504-1-competitive-comparison-correction.md`（审计裁决源，M-1/M-2 已由 `2026-07-13-0455-1/2` 闭合，M-3 watch-only residual，H-A/H-G/H-C/M-4 已文档勘误；本计划闭合最后三项 L 级测试缺口）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD=7e07aefa 范围）：

### L-1 — 多套账"1 事件→N 凭证"集成测试缺失（杠杆 B）

- **SchemaPropagator 已实现**：`module-finance/erp-fin-service/.../posting/SchemaPropagator.java`（134 行），`resolveTargetSchemas(orgId, primarySchemaId)`（:44）读 `ErpMdAcctSchema.isPropagate` 收集同 org 全部启用账套按 nature 优先级排序，受 `erp-fin.multi-schema-enabled` 配置门控（:86，默认 `false`）。
- **ErpFinPostingProcessor 已迭代多账套**：`ErpFinPostingProcessor.java`（873 行）`process()` 在 :121 调 `resolveTargetSchemas`，:151–178 `for (Long schemaId : targetSchemas)` 循环为每个目标账套生成独立凭证（非主账套经 `translateFactsForSchema` :578 科目翻译）。
- **ErpMdAcctSchema 模型完备**：`module-master-data/model/app-erp-master-data.orm.xml:867–910`，含 `nature`（字典 `erp-md/acct-schema-nature` FINANCIAL/MANAGEMENT/TAX/CONSOLIDATION/BUDGET）+ `isPropagate`（布尔默认 false）+ `costingMethod` + `functionalCurrencyId` + `(code, orgId)` 唯一键。
- **零测试**：`SchemaPropagator` 在全部 test 源中零引用；grep `multi-schema|multiSchema|propagat|isPropagate` 跨 module-finance 测试源零命中。

### L-2 — LIFO/Batch/Specific/WAM 成本策略独立测试缺失（杠杆 E）

- **7 策略类全部存在**（`module-inventory/erp-inv-service/.../costing/`）：`MovingAverageCostingStrategy`(92)、`WeightedAverageCostingStrategy`(104,WAM)、`FifoCostingStrategy`(207)、`LifoCostingStrategy`(194)、`StandardCostingStrategy`(90)、`SpecificCostingStrategy`(190)、`BatchCostingStrategy`(198)。
- **StockMoveBookkeeper 注册 7 策略**：`StockMoveBookkeeper.java:65–97` `@Inject` 全部 7 策略 + `@PostConstruct initStrategyRegistry()` 逐一 `register()`；`bookCompletion`（:107）经 `CostMethodResolver.resolve()` → `resolveStrategy()` 分派。
- **已有测试仅覆盖 3 策略**：`TestErpInvFifoCosting.java`（FIFO，6 方法）、`TestErpInvFifoCostingEndToEnd.java`（FIFO E2E）、`TestErpInvStandardCosting.java`（STANDARD）、`TestErpInvCostingDispatch.java`（仅 MOVING_AVERAGE 行为 + 全 7 常量名断言）。LIFO/Batch/Specific/WAM 四策略**零行为测试**（grep `LifoCostingStrategy|BatchCostingStrategy|SpecificCostingStrategy|WeightedAverageCostingStrategy` 跨全部 test 源零命中）。
- **FIFO 测试为镜像模板**：`TestErpInvFifoCosting.java` extends `JunitAutoTestCase`，6 `@Test` 方法覆盖 onIncoming 追加层 / onOutgoing 单层消耗 / onOutgoing 跨层加权 / 无成本层拒绝 / 红冲不变式 / ledger totalCost 流向 dispatcher。

### L-3 — DeferredPostingSweepJob 单测缺失（杠杆 D）

- **DeferredPostingSweepJob 已实现**：`module-finance/erp-fin-service/.../job/DeferredPostingSweepJob.java`（218 行），`execute()`（:68）读 cron 配置 → `findPendingExceptions()`（:99，扫 `ErpFinPostingException` status=PENDING AND retryCount<3 AND occurrenceTime>=now-24h，limit 100）→ 逐条 `retryOne()`（:118，REQUIRES_NEW 事务）→ 成功 `markRetried`（:174）/ 失败 `incrementRetryAndRethrow`（:183，retryCount+1，达 MAX_RETRY=3 标 RETRYING）。
- **scheduler.yaml 已接线**：`app-erp-all/.../scheduler.yaml:156–163` `erp-fin-deferred-posting-sweep` cron `0 0/5 * * * ?`，调 `erpFinDeferredPostingSweepJob.execute`。双重门控：execute() 内部也读 `erp-fin.deferred-posting-sweep-cron`（默认空→跳过）。
- **ErpFinPostingException 模型完备**：`module-finance/model/app-erp-finance.orm.xml:1488–1539`，含 status（PENDING/RETRYING/RETRIED/IGNORED/MANUAL）+ retryCount + eventData（JSON VARCHAR(4000)）+ postingType（NORMAL/REVERSAL）+ businessType + voucherDate。
- **零测试**：`Test*DeferredPosting*` / `Test*PostingSweep*` 零命中。`TestErpFinPostingExceptionWorkbench.java`（292 行）仅测 BizModel 手工 retry（:138），Job 自动扫描重试路径未覆盖。

剩余差距：3 项竞争杠杆审计 L 级测试缺口，每项对应一个已实现但零测试的组件。

## Goals

- L-1：新增多套账并行凭证集成测试，验证"1 业务事件 → N 账套 N 凭证"正确传播（`SchemaPropagator.resolveTargetSchemas` + `ErpFinPostingProcessor.process` 账套循环 + 科目翻译）。
- L-2：新增 LIFO/Batch/Specific/WAM 四策略独立单元测试，验证各策略的层消耗/批次匹配/具体辨认/全月加权行为正确性。
- L-3：新增 `DeferredPostingSweepJob` 单元测试，验证自动重试成功路径 + 重试耗尽路径 + 空配置跳过路径。

## Non-Goals

- 不修改任何生产代码（`SchemaPropagator`/`ErpFinPostingProcessor`/7 策略类/`DeferredPostingSweepJob`/`ErpFinPostingException` 均不改）——本计划纯测试新增。
- 不新增成本策略或过账逻辑——仅验证已有行为。
- 不做 Playwright E2E 层覆盖——本计划为 JUnit 单元/集成测试层。
- 不补齐 TestErpInvCostingDispatch 的 MOVING_AVERAGE 专项文件（已有行为覆盖，仅无独立文件）。

## Task Route

- Type: `verification or audit work`（纯测试新增，闭合审计测试缺口）
- Owner Docs: `docs/design/finance/multiple-accounting-schemas.md`（杠杆 B）、`docs/design/finance/costing-methods.md`（杠杆 E）、`docs/design/finance/posting-log.md`（杠杆 D）
- Skill Selection Basis: JUnit 测试编写 → 匹配 `nop-testing`（`JunitAutoTestCase` 基类、request.json5 手写、CHECKING 模式）；L-2 纯策略行为单测可能脱离 GraphQL 走纯 Java → Skill 仍 `nop-testing`（基类 + 断言范式一致）。
- Protected Areas: 无（纯测试新增，零生产代码/ORM/契约变更）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。
- L-1 测试须在测试内启用 `erp-fin.multi-schema-enabled=true`（经测试 context 或 `@NopTestConfig` localConfig），不影响生产默认 false。

## Execution Plan

### Phase 1 — 多套账并行凭证集成测试（L-1）

Status: completed
Targets: `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinMultiSchemaPosting.java`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: 无

- [x] Add: 新建 `TestErpFinMultiSchemaPosting.java`（extends `JunitAutoTestCase`），测试 `SchemaPropagator.resolveTargetSchemas` + `ErpFinPostingProcessor.process` 多账套并行传播。
  - Skill: `nop-testing`
- [x] Proof: 测试用例覆盖：
  - 正路径：org 下 2 账套（FINANCIAL 主 + MANAGEMENT isPropagate=true），启用 `multi-schema-enabled`，触发一个过账事件 → 断言生成 2 张凭证（不同 acctSchemaId）。
  - 主账套凭证用原始科目；非主账套凭证经 `SubjectMappingResolver` 翻译后科目（或无映射时回退源科目）。
  - `isPropagate=false` 时仅主账套 1 张凭证。
  - `multi-schema-enabled=false` 时仅主账套 1 张凭证（向后兼容）。
  - Skill: `nop-testing`

Exit Criteria:

> 仅写此阶段实际交付的可观察结果，以及解除后续阶段阻塞所需的任何本地化检查。

- [x] `TestErpFinMultiSchemaPosting` 全方法绿（4 场景），验证多套账传播行为可观测。
- [x] `mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinMultiSchemaPosting` 通过。

### Phase 2 — LIFO/Batch/Specific/WAM 成本策略单元测试（L-2）

Status: completed
Targets: `module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvLifoCosting.java`、`TestErpInvBatchCosting.java`、`TestErpInvSpecificCosting.java`、`TestErpInvWeightedAverageCosting.java`（与既有 `TestErpInvFifoCosting` 同包 `app.erp.inv.service`）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: 无（与 Phase 1 独立）

- [x] Add: 新建 `TestErpInvLifoCosting.java`（extends `JunitAutoTestCase`），镜像 `TestErpInvFifoCosting` 结构，验证 LIFO 策略层消耗顺序（后进先出，`layer.reversed()` :194 行为）。
  - Skill: `nop-testing`
- [x] Add: 新建 `TestErpInvBatchCosting.java`，验证 BATCH 策略按 batchNo 匹配成本（198 行行为）。
  - Skill: `nop-testing`
- [x] Add: 新建 `TestErpInvSpecificCosting.java`，验证 SPECIFIC 策略按 batch/serial 精确匹配成本（190 行行为）。
  - Skill: `nop-testing`
- [x] Add: 新建 `TestErpInvWeightedAverageCosting.java`，验证 WAM 策略全月一次加权平均（104 行行为，区别于 MOVING_AVERAGE 永续加权）。
  - Skill: `nop-testing`
- [x] Proof: 每策略测试用例覆盖（镜像 FIFO 模板）：
  - onIncoming 追加成本层 / onOutgoing 消耗 / 跨层加权（适用时）/ 无成本层拒绝（适用时，WAM 例外：无 cost layer，空 avgCost 走 ZERO 路径）/ 红冲不变式。
  - 各策略特有行为：LIFO 层逆序 / BATCH 按 batchNo / SPECIFIC 按 batch+serial / WAM 全月均化。
  - Skill: `nop-testing`

Exit Criteria:

> 仅写此阶段实际交付的可观察结果。

- [x] 4 新测试文件全方法绿（每文件 4–6 方法），验证 4 策略行为正确性可观测。
- [x] `mvn test -pl module-inventory/erp-inv-service -am -Dtest="TestErpInvLifoCosting,TestErpInvBatchCosting,TestErpInvSpecificCosting,TestErpInvWeightedAverageCosting"` 通过。

### Phase 3 — DeferredPostingSweepJob 单元测试（L-3）

Status: completed
Targets: `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/job/TestErpFinDeferredPostingSweepJob.java`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: 无（与 Phase 1/2 独立）

- [x] Add: 新建 `TestErpFinDeferredPostingSweepJob.java`（extends `JunitAutoTestCase`），验证 `DeferredPostingSweepJob.execute()` 自动扫描重试路径。
  - Skill: `nop-testing`
- [x] Proof: 测试用例覆盖：
  - 正路径：预置一条 PENDING `ErpFinPostingException`（含有效 eventData JSON + postingType=NORMAL + 合法 businessType）+ 启用 cron 配置 → `execute()` → 重试成功 → status=RETRIED + voucherId 非空。
  - 重试耗尽路径：retryCount=2 的 PENDING 异常 → execute() 失败 → retryCount=3 + status=RETRYING。
  - 空配置跳过：`erp-fin.deferred-posting-sweep-cron` 空 → execute() noop（无异常、无状态变更）。
  - REVERSAL 路径：postingType=REVERSAL → `doRetry` 走 `voucherBiz.reverse` 分支（:140）。
  - 单条失败隔离：多条 PENDING，其中一条 eventData 损坏 → 失败条目 retryCount+1，其余条目不受影响。
  - Skill: `nop-testing`

Exit Criteria:

> 仅写此阶段实际交付的可观察结果。

- [x] `TestErpFinDeferredPostingSweepJob` 全方法绿（5 场景），验证兜底扫描重试行为可观测。
- [x] `mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinDeferredPostingSweepJob` 通过。

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (ses_0a769f9f9ffeYW4vRNZI9iadNo, 2026-07-13) because baseline claims all verified against live repo (SchemaPropagator 134L / ErpFinPostingProcessor 873L process() :121,:151–178 / 7 strategies + line counts / DeferredPostingSweepJob 218L execute() :68 / zero test coverage confirmed); rule compliance R1–R12 + anti-slack + exec R7 all pass; 0 Blocker / 0 Major / 2 Minor fixed (REVERSAL 行引用 :143→:140；WAM "无成本层拒绝" 补"适用时"WAM 例外说明)

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处。

- [x] 范围内行为完成（3 项审计测试缺口全部闭合）
- [x] 相关文档对齐（`docs/audits/2026-07-12-1504-competitive-levers-implementation-audit.md` L-1/L-2/L-3 状态已标注已闭合）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ `mvn test -pl module-finance/erp-fin-service -am`（194 tests 0 failures）+ `mvn test -pl module-inventory/erp-inv-service -am`（113 tests 0 failures）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话 ses_0a74673daffe0HeNBIfgTApHwI）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（暂无——本计划为纯测试闭合，范围窄、无预期 deferred 项。）

## Closure

Status Note: completed

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话 ses_0a74673daffe0HeNBIfgTApHwI，2026-07-13）— Verdict: **PASS**，0 Blocker。

> **Closure Audit: PASS**（独立子代理，新会话 2026-07-13）。6 个目标测试文件全部存在（4+4+4+4+4+5 = 25 `@Test` 方法）。Non-Goal 遵守：`git status`/`git diff` 证实 `src/main/`、`*.orm.xml`、`*.api.xml` 零变更——仅 `src/test/java` 文件、`_cases/` 自动测试快照与文档（1 个审计 `.md` 闭合标注 + 计划文件）。两套验证命令全绿：finance `Tests run: 9, Failures: 0, Errors: 0`（Phase1+Phase3）；inventory `Tests run: 16, Failures: 0, Errors: 0`（Phase2）。覆盖抽查确认每个 Proof 场景均映射到具体测试方法。Phase 1–3 均 `Status: completed` 且清单项全 `[x]`。

Follow-up:

- （暂无）
