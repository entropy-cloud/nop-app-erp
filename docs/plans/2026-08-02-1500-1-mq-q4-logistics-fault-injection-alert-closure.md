# 2026-08-02-1500-1 mq-q4-logistics-fault-injection-alert-closure Q4 logistics 过账悬挂告警闭环 + 故障注入回归（P1-MA2-080）

> Plan Status: completed
> Last Reviewed: 2026-08-02
> Source: `docs/architecture/quality-engineering/fault-injection.md` §1.3（logistics 第 7 域边界声明）+ §8 successor 表「logistics（P1-MA2-080）过账悬挂覆盖，优先级高」；finding ID 权威索引 `docs/audits/arm-index.md`（P1-MA2-080 = logistics「网关异常 + 运费过账失败」）
> Related: `docs/plans/2026-08-01-1357-3-mq-q4-fault-injection-impl.md`（Q4 首轮 6 域）、`docs/plans/2026-08-01-1158-3-mq-q4-fault-injection-design-doc.md`（Phase 1 设计文档）
> Audit: required

## Current Baseline

- **Q4 首轮 6 域 done**：`FaultInjectionStubs` harness 落盘 `module-common-test/src/main/java/app/erp/common/test/FaultInjectionStubs.java`（公开 helper：`throwingVoucherBiz(methodName, toThrow)` / `recordingNotificationBiz(capturedEventType[])` / `throwingProxy` / `defaultReturn` / `testFault`）；9 个故障注入测试覆盖 finance/hr/assets/qa/projects/maintenance（A3-unit + A4-alert 契约）。
- **CI 门控已激活**：`.github/workflows/compliance.yml` `fault-injection-coverage` job（line 212-257）单向收紧 6 域（patterns = `FailureHangs|FailureAlert|FaultInjection`，baseline=6 域）。
- **logistics 是同型根因第 7 域，明确 deferred**：Q4 设计文档 §1.3 + §8 successor 表 + Q4 impl plan §Deferred But Adjudicated 三处登记「logistics（P1-MA2-080）过账悬挂覆盖，触发条件=6 域 harness 沉淀后扩展，优先级高」。**触发条件已满足**（Q4 首轮 6 域 harness + 测试已沉淀）。
- **确认的实时缺陷（P1-MA2-080）**：`AbstractErpLogShipmentDeliveredProcessor.onDelivered`（`module-logistics/erp-log-service/.../processor/AbstractErpLogShipmentDeliveredProcessor.java:88-101`）的 catch 块在 `voucherBiz.post(event, context)` 失败时**仅 `LOG.warn`/`LOG.error`**——**未派发 `IErpSysNotificationBiz.notify` 告警**。`ErpLogShipmentScanForPollingProcessor.scanForPolling`（line 25-31）catch 同样仅 LOG。对照 6 个 peer G4 域（如 `MaintenanceLaborPostingDispatcher:121-148`）：catch 块均含 `notificationBiz.notify(NOTIFY_EVENT_*_FAILURE, ctx, serviceSvc)` 告警派发（包裹自身 try/catch 降级）。**logistics 是唯一缺告警闭环的同型根因域**。
- **logistics 无 finance sweep 兜底**（fault-injection.md §1.3 + posting-log.md §期末结账前置检查覆盖矩阵）：运费过账失败 → `freightSettlementStatus` 永久 PENDING，无 sweep 重试、无告警、无前置检查阻断——仅期末试算平衡人工发现。这是项目已知失败模式 #1（`project-context.md §已知失败模式`「业财过账吞异常致 posted 悬挂」）的现存实例。
- **IErpSysNotificationBiz 已在 logistics 模块接线**（`GatewayDispatcher.java` field + dead-letter 告警路径），故 onDelivered 补告警复用既有模块级依赖，零新引入。
- **剩余差距**：onDelivered/scanForPolling 缺告警派发（生产缺陷）+ logistics 缺故障注入回归测试（零保护）+ CI 门控仅覆盖 6 域（logistics 第 7 域未纳入）。

## Goals

- **关闭 P1-MA2-080 告警闭环缺陷**：在 `AbstractErpLogShipmentDeliveredProcessor.onDelivered` 失败路径补 `IErpSysNotificationBiz.notify` 告警派发（对齐 6 个 peer G4 域的 `NOTIFY_EVENT_*_FAILURE` + 降级 try/catch 范式），使运费过账失败可观测（A4-alert 契约）。
- **新增 logistics 故障注入回归测试**：复用 `FaultInjectionStubs` harness，注入 `throwingVoucherBiz` 使 `post` 抛异常 + `recordingNotificationBiz` 录制告警，断言 A1（freightSettlementStatus 保持 PENDING 一致）+ A2（异常可观测）+ A4-alert（captured event type = `log.freight-posting-failure`）。
- **CI 门控扩展 6→7 域**：`compliance.yml` `fault-injection-coverage` job baseline 从 6 上调至 7（单向收紧，logistics 纳入）。

## Non-Goals

- **不为 logistics 增加 finance sweep 兜底**——logistics 保持 G4（告警 + 人工试算平衡），对齐 `posting-log.md §期末结账前置检查覆盖矩阵`（logistics 经前置检查阻断，非 sweep 重试）。将 logistics 升级为 G1/G2 sweep 覆盖域是架构变更，超本计划范围。
- **不覆盖 scanForPolling 网关重试耗尽（gateway retry exhaustion）**——P1-MA2-080 含两个子发现（运费过账缺告警 + 轮询重试不幂等）。本计划仅修运费过账告警闭环（onDelivered catch 路径，与 peer G4 域同型）；轮询重试幂等性是独立并发议题（R1.28 UK 幂等族已覆盖 cron 幂等，scanForPolling 重试幂等归其 successor）。
- **不修改 nop-entropy**——零平台改动（路径 A 应用层闭环）。
- **不重跑 Q1 变异测试**——本计划是 Q4 故障注入扩展，与 Q1 变异测试正交。

## Task Route

- Type: `bug investigation + implementation-only change`（确认的实时缺陷修复 + 回归测试，非契约/模型变更）
- Owner Docs: `docs/architecture/quality-engineering/fault-injection.md`（§1.3 logistics 边界声明 + §4.2 A4-alert 契约 + §5.2 覆盖清单 + §8 successor 表）、`docs/design/finance/posting-log.md`（§G4 分级 + §期末结账前置检查覆盖矩阵）、`docs/design/flow-overview.md §事务边界`
- Skill Selection Basis: `nop-backend-dev`（生产修复触及 Processor catch 路径 + IErpSysNotificationBiz 跨实体注入，须遵循 Processor per-mutation 范式 + IoC 注入规则）；`nop-testing`（故障注入测试是标准 JUnit，非 JunitAutoTestCase 快照，但须遵循测试隔离纪律）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. `FaultInjectionStubs` 已落盘 `module-common-test`（optional 依赖隔离），logistics service 测试已可引用。`IErpSysNotificationBiz` 接口已在 6 个 peer 域的生产代码中注入使用，logistics 引入零新依赖。

## Execution Plan

### Phase 1 - logistics 告警闭环生产修复 + 故障注入测试

Status: completed
Targets: `module-logistics/erp-log-service/src/main/java/app/erp/log/service/processor/AbstractErpLogShipmentDeliveredProcessor.java`、`module-logistics/erp-log-service/src/test/java/app/erp/log/service/processor/TestLogPostingFaultInjection.java`
Skill: `nop-backend-dev`

> 执行注记（2026-08-02）：
> - **测试包修正**：计划原文写 `app.erp.log.service.posting`，实际落 `app.erp.log.service.processor`（与 `AbstractErpLogShipmentDeliveredProcessor` 同包）。理由：`@Inject` field 为 package-private，按 `FaultInjectionStubs` harness 契约（"测试须置于 dispatcher 同包"）+ 6 个 peer 域测试（mnt/qa/ast/fin/hr/prj 均与 dispatcher 同包）一致，须同包经 field 赋值注入桩。logistics 用 `processor` 子包（R6.7）而非 peer 域的 `posting` 子包，故测试随之置于 `processor`。MINOR，无契约影响。
> - **scanForPolling 裁决**：onDelivered 内部已派发告警，scanForPolling 外层 per-shipment catch（`ErpLogShipmentScanForPollingProcessor:27-30`）保持 LOG（不重复告警），避免双告警。对齐 peer 范式（告警归属最内层过账方法）。

- Item Types: `Fix | Add | Proof`
- Prereqs: Q4 首轮 done（harness 已落盘）

- [x] `Fix`：`AbstractErpLogShipmentDeliveredProcessor` 增加 `@Inject IErpSysNotificationBiz notificationBiz` field（包级可见，对齐 Nop IoC + peer 范式）+ `NOTIFY_EVENT_LOG_FREIGHT_POSTING_FAILURE = "log.freight-posting-failure"` 常量
  - Skill: `nop-backend-dev`
- [x] `Fix`：`onDelivered` catch 块（line 93-101）在现有 LOG 之后补告警派发 `notificationBiz.notify(NOTIFY_EVENT_LOG_FREIGHT_POSTING_FAILURE, ...)`，包裹自身 try/catch 降级（对齐 `MaintenanceLaborPostingDispatcher:145-148` 范式——notify 本身失败仅 LOG.warn 不再抛）
  - Skill: `nop-backend-dev`
- [x] `Decision`：scanForPolling 的 per-shipment catch（`ErpLogShipmentScanForPollingProcessor:27-30`）是否需补告警——裁决：onDelivered 内部已补告警（扫描调 onDelivered 时告警已派发），scanForPolling 外层 catch 仅防止单运单中断扫描，保持 LOG（不重复告警）。记录理由于 plan + 必要时 owner doc 注记。
  - Skill: `nop-backend-dev`
- [x] `Add`：新建 `TestLogPostingFaultInjection`（包 `app.erp.log.service.processor`，见上执行注记包修正），构造 `ErpLogShipmentScanForPollingProcessor` 实例，注入 `FaultInjectionStubs.throwingVoucherBiz("post", ...)` 使 `voucherBiz.post` 抛异常 + `FaultInjectionStubs.recordingNotificationBiz(captured)` 录制告警，调 `onDelivered`，断言：(a) `captured[0] == NOTIFY_EVENT_LOG_FREIGHT_POSTING_FAILURE`（A4-alert）；(b) `freightSettlementStatus` 保持 PENDING（A1 一致性）；(c) 未抛异常（可恢复，非静默悬挂）
  - Skill: `nop-testing`
- [x] `Proof`：`mvn -pl module-logistics/erp-log-service test` 绿（新测试通过 + 既有 logistics 测试零回归）。指定验证命令：`mvn -pl module-logistics/erp-log-service -Dtest=TestLogPostingFaultInjection test`
  - Skill: `nop-testing`

Exit Criteria:

- [x] onDelivered 失败路径派发 `log.freight-posting-failure` 告警（A4-alert），对齐 peer G4 域范式（grep `notificationBiz.notify` 在 logistics processor catch 块内命中）
- [x] `TestLogPostingFaultInjection` 断言 A1+A2+A4-alert 三契约，本地 `mvn -pl module-logistics/erp-log-service test` 绿（解除后续 CI 门控扩展的阻塞）

### Phase 2 - CI 门控扩展 + 文档对齐 + 全量验证

Status: completed
Targets: `.github/workflows/compliance.yml`、`docs/architecture/quality-engineering/fault-injection.md`、`docs/logs/2026/08-02.md`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 done

- [x] `Add`：`compliance.yml` `fault-injection-coverage` job baseline 6→7（覆盖域列表增 logistics）；本地复跑 grep 校验 7 域命中（对齐单向收紧：actual < 7 → CI fail）
  - Skill: none
- [x] `Add`：`fault-injection.md` §8 successor 表「logistics（P1-MA2-080）过账悬挂覆盖」状态更新为「已闭合（本计划）」+ §5.2 覆盖清单增 logistics 行
  - Skill: none
- [x] `Add`：`docs/logs/2026/08-02.md` 追加本计划条目（含验证状态）
  - Skill: none

Exit Criteria:

- [x] compliance.yml fault-injection-coverage baseline=7，本地 grep 7 域命中

## Draft Review Record

- Independent draft review iteration 1: `ses_03fd0f119ffeLYufYiokyD0bEk`（独立子代理 fresh session cold context）— **accept**，0 BLOCKER / 0 MAJOR / 5 MINOR。全部 Current Baseline 事实经 live-repo 核验 PASS（FaultInjectionStubs 6 helper / compliance.yml 6 域门控 / onDelivered catch 仅 LOG 无 notify / peer mnt 告警范式 / logistics 未被 Q4 首轮覆盖）。5 MINOR 已修订：M1（compliance.yml 行范围 212-253→212-257）/ M4（Source 引用 roadmap line 677 P1-MA2-080 未命中→改引 fault-injection.md §1.3+§8 + arm-index）/ M5（补 GatewayDispatcher 已接线 IErpSysNotificationBiz 注记，强化零新依赖）。M2（qa vs quality 命名）/M3（Phase 2 Item Types 声明）非阻塞保留。

## Closure Gates

- [x] 范围内行为完成（onDelivered 告警闭环 + 故障注入测试 + CI 门控 7 域）
- [x] 相关文档对齐（fault-injection.md successor 状态 + posting-log.md 无需改——logistics 保持 G4 分级）
- [x] 已运行验证：`mvn clean install -DskipTests`（156 模块 BUILD SUCCESS）+ `mvn test`（0 failures/0 errors，新测试绿 + 既有基线零回归）+ `bash docs/audits/nop-compliance-checker.sh`（无新增命中）+ 本地复跑 compliance.yml fault-injection-coverage grep（7 域命中）
- [x] 无范围内项目降级为 deferred/follow-up（P1-MA2-080 告警闭环是 Rule 13 不可降级项——确认的实时缺陷）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留作未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中

> 验证细节（执行者核验，2026-08-02）：(1) `mvn clean install -DskipTests` → 156 SUCCESS / 0 FAILURE。(2) `mvn test` 全 reactor 唯一失败 = 预存 `module-master-data TestErpMdExchangeRateApiClient.testRefreshRatesFromApiWritesExchangeRate` 日期漂移（`VALID_FROM=2026-08-02` vs 预期 `2026-08-01`，R6.9 test-hardening successor，`git status module-master-data/` 空 → 本计划零触及零因果）；`mvn test -pl '!module-master-data/erp-md-service' -fae` → 155 模块 BUILD SUCCESS 0 failures/0 errors。(3) `mvn -pl module-logistics/erp-log-service test` → 26 tests（25 baseline + 1 new TestLogPostingFaultInjection）0 failures/0 errors，零回归。(4) compliance checker 无新增命中（R1d=14/R2a=34 等为既有基线；本计划仅增 IErpSysNotificationBiz/ServiceContextImpl import + catch 内 dispatchFreightFailureAlert 调用）。(5) 本地复跑 compliance.yml `fault-injection-coverage` Python grep → 7/7 域命中。

> 结束审计（独立子代理 fresh session cold context，2026-08-02）：plan-check.mjs 脚本不在仓库（mission-driver 工具为外部托管），故按计划指南 §结束时 手工执行独立结束审计。逐项核验：(a) 生产修复 — `AbstractErpLogShipmentDeliveredProcessor` 实盘核验 `@Inject IErpSysNotificationBiz notificationBiz` field（line 54-58）+ `NOTIFY_EVENT_LOG_FREIGHT_POSTING_FAILURE` 常量（line 43）+ catch 内 `dispatchFreightFailureAlert` 调用（line 110）+ 降级 helper（line 161-179，含 notify 失败 try/catch LOG.warn）全部落地，**反空壳通过**（dispatchFreightFailureAlert 在 runtime catch 路径被调用，非孤儿方法）。(b) 测试 — `TestLogPostingFaultInjection` 实盘核验断言 A4-alert（captured[0]==`log.freight-posting-failure`）+ A1（freightSettlementStatus 保持 PENDING）+ 可恢复（assertDoesNotThrow），包修正 `app.erp.log.service.processor` 与 dispatcher 同包，对齐 harness 契约。(c) CI 门控 — `compliance.yml` line 230 DOMAINS 含 logistics，BASELINE=7（单向收紧）。(d) 文档对齐 — `fault-injection.md` §1.3（successor 已闭合注记）+ §5.2（logistics 行）+ §8 successor 表（已闭合）三处同步。(e) scanForPolling「不双告警」Decision 合理 — onDelivered catch 吞运费过账异常并派发告警，scanForPolling 外层 catch 仅对非运费过账异常触发，无双告警。(f) 五点一致性 — Plan Status=completed、两 Phase Status=completed、所有 Exit Criteria/Closure Gates [x]、Closure 证据落地、docs/logs/2026/08-02.md 条目含验证状态，全一致。(g) Deferred honesty — scanForPolling 幂等（子发现 b）诚实裁决为 watch-only residual，successor 触发条件（R1.28 cron 幂等族扩展至 logistics）已命名。审计结论：**approved**，本计划可关闭。

## Deferred But Adjudicated

### scanForPolling 网关重试幂等性（P1-MA2-080 子发现 b）

- Classification: `watch-only residual`
- Why Not Blocking Closure: P1-MA2-080 含两个子发现——(a) 运费过账缺告警闭环（本计划修复）+ (b) scanForPolling 轮询重试不幂等（DELIVERED-PENDING 运单重复触发 onDelivered）。子发现 (b) 的幂等性属 R1.28 cron/UK 幂等族（P1-MA2-086）范围——scanForPolling 经 onDelivered 幂等守卫（`ERR_LOG_SHIPMENT_ALREADY_DELIVERED` line 69-72）部分覆盖，完整幂等裁决归 R1.28 successor。
- Successor Required: yes — 触发条件：R1.28 cron 幂等族扩展至 logistics scanForPolling 时，复核 DELIVERED-PENDING 重入幂等。

## Closure

Status Note: 执行完成 + 独立结束审计通过（2 Phase done，全 items/Exit Criteria/Closure Gates 勾选）。独立子代理 fresh session cold context 按计划指南 §结束时 逐项实盘核验：生产修复（onDelivered 告警闭环反空壳）+ 故障注入测试（A1+A2+A4-alert）+ CI 门控 7 域 + 文档对齐 + 五点一致性 + Deferred honesty 全通过，审计结论 approved。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（CLOSURE_AUDIT 流，fresh session cold context，无执行者上下文）—— 审计执行 2026-08-02
- 审计范围与方法：逐项实盘核验（非盲目信任 [x]），覆盖生产修复/测试/CI/文档/日志/Decision 合理性/反空壳/五点一致性/Deferred 诚实度
- 实盘核验证据：
  - 生产修复 `AbstractErpLogShipmentDeliveredProcessor.java`：`@Inject IErpSysNotificationBiz notificationBiz`（line 54-58）+ `NOTIFY_EVENT_LOG_FREIGHT_POSTING_FAILURE` 常量（line 43）+ catch 内 `dispatchFreightFailureAlert` 调用（line 110）+ 降级 helper（line 161-179，notify 失败 try/catch LOG.warn 不再抛）—— **反空壳通过**：helper 在 runtime catch 路径被调用（line 110），非孤儿
  - 故障注入测试 `TestLogPostingFaultInjection.java`：断言 A4-alert（captured[0]==`log.freight-posting-failure`）+ A1（freightSettlementStatus 保持 PENDING）+ 可恢复（assertDoesNotThrow），包 `app.erp.log.service.processor` 与 dispatcher 同包，对齐 harness 契约
  - CI 门控 `.github/workflows/compliance.yml` line 230：DOMAINS 含 logistics，BASELINE=7（单向收紧 actual<7→CI fail）
  - 文档对齐 `docs/architecture/quality-engineering/fault-injection.md`：§1.3（successor 已闭合注记 line 79）+ §5.2（logistics 行 line 309）+ §8 successor 表（已闭合 line 405）三处同步
  - 日志 `docs/logs/2026/08-02.md`：本计划条目含详细验证状态（156 模块 install SUCCESS + 155 模块 test BUILD SUCCESS + logistics 26 tests 全绿 + compliance checker 无新增命中）
  - scanForPolling「不双告警」Decision 合理性：onDelivered catch 吞运费过账异常并派发告警（不重抛），scanForPolling 外层 catch 仅对非运费过账异常触发，无双告警，对齐 peer「告警归属最内层过账方法」范式
  - 五点一致性：Plan Status=completed、两 Phase Status=completed、所有 Exit Criteria/Closure Gates [x]、Closure 证据落地、日志条目一致
  - Deferred honesty：scanForPolling 幂等（P1-MA2-080 子发现 b）诚实裁决为 watch-only residual，successor 触发条件（R1.28 cron 幂等族扩展至 logistics scanForPolling）已命名
- 审计结论：**approved** —— 范围内行为完成（P1-MA2-080 不可降级缺陷已闭合），无降级、无空壳、无契约漂移，本计划可关闭
- 执行者落盘证据：
  - 生产修复：`module-logistics/erp-log-service/src/main/java/app/erp/log/service/processor/AbstractErpLogShipmentDeliveredProcessor.java`（`@Inject IErpSysNotificationBiz notificationBiz` + `NOTIFY_EVENT_LOG_FREIGHT_POSTING_FAILURE` 常量 + `onDelivered` catch 补 `dispatchFreightFailureAlert` + `dispatchFreightFailureAlert` helper）
  - 故障注入测试：`module-logistics/erp-log-service/src/test/java/app/erp/log/service/processor/TestLogPostingFaultInjection.java`（A1+A2+A4-alert 三契约）
  - CI 门控：`.github/workflows/compliance.yml` `fault-injection-coverage` baseline 6→7（DOMAINS 增 logistics）
  - 文档对齐：`docs/architecture/quality-engineering/fault-injection.md` §1.3（successor 已闭合注记）+ §5.2（logistics 行）+ §8 successor 表（已闭合）
  - 日志：`docs/logs/2026/08-02.md` 本计划条目（含验证状态）
- 验证：见 Closure Gates 注记（156 模块 install SUCCESS + 155 模块 test BUILD SUCCESS[排除预存 master-data date-fragility flake] + logistics 26 tests 全绿 + compliance checker 无新增命中 + CI grep 7/7 域命中）。
