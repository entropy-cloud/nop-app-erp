# 2026-08-01-1357-3-mq-q4-fault-injection-impl 故障注入测试 Phase 2 实现

> Plan Status: active
> Last Reviewed: 2026-08-01
> Mission: audit-remediation
> Work Item: MQ Q4（Phase 2 实现）
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q4（line 677 工作项表 + line 787 维度说明）；`docs/architecture/quality-engineering/README.md` §实施顺序裁决（Q4 位 3，Q1↔Q4 协同）
> Related: 设计文档 plan `docs/plans/2026-08-01-1158-3-mq-q4-fault-injection-design-doc.md`（Phase 1 done）；设计文档 `docs/architecture/quality-engineering/fault-injection.md`（已收敛的实施契约，本计划引用为范围与验收依据）；sibling plan `2026-08-01-1357-2-mq-q1-mutation-testing-impl.md`（Q1 Phase 2，Q1↔Q4 协同——Q1 盲区类清单即 Q4 优先覆盖目标）；owner doc `docs/design/finance/posting-log.md` §错误传播分级策略 G1-G4（可恢复性断言契约对齐基准）；MR1.16 单点修复证据（Q4 是其系统性回归保护超集）
> Audit: required

## Current Baseline

> 本计划是 MQ 文档先行工作流的 **Phase 2 实现**：以经独立子代理 2 轮审查收敛的设计文档 `fault-injection.md` 为实施契约。基线盘点引用设计文档 §1（已核验证据），不重推导。

**audit-remediation 主线**：全 done + MR6 CLOSED；`mvn clean install -DskipTests` 156 模块全绿；`mvn test` 1903 测试 0 failures/0 errors。

**Q4 现状（Phase 1 已诊断，设计文档 §1）**：
- **无通用化故障注入 harness**（核验 `rg -il "fault.injection|FaultInjector|@InjectFault" --glob '*.java' --glob '*.xml'` EXIT=1）——但有 **7 个 per-point 过账悬挂测试先例**（散落各域，非通用化抽象）：`TestErpInvPostingDispatcherFailureHangs`/`TestErpPurPostingDispatcherFailureHangs`/`TestErpSalPostingDispatcherFailureHangs`/`TestDepreciationPostingFailureAlert`/`TestErpInvLandedCostReverseFailureAlert`/`TestTimesheetPostingFailureAlert`/`TestErpMfgVarianceRecomputeReversal`。统一用两种桩机制：Proxy 桩 Facade 接口（`IErpFinVoucherBiz`/`IErpSysNotificationBiz`）+ 子类 override 具体类（`*PostingExecutor`）。**Q4 任务=把它们泛化为统一 harness + 跨域可恢复性断言契约。**
- **同型根因跨 6 域**（tryPost 吞异常 → posted=false 静默悬挂，经设计文档 §1.3 消解 finding-ID 漂移）：
  - finance `P1-MA2-032` / hr `P1-MA2-048` / assets `P1-MA2-060` / qa A2.12「MANUAL_POST NCR」（无独立编号）/ projects `P1-MA2-068` / maintenance `P1-MA2-074` —— **MR1.16 全部单点修复**，但**无系统性回归保护**。
  - **logistics（P1-MA2-080）是第 7 域**，设计文档 §1.3 裁决为 successor（本期 6 域 scope，对齐设计文档 §Goals）。
- **真实过账 SPI**（设计文档 §3.1 盘点，纠正 roadmap 沿用的误导名 `IPostingDispatcher`——实仓不存在）：各域具体 `*PostingDispatcher`（6 域 18 个）+ 各域具体 `*PostingExecutor`（6 域 6 个）+ finance facade `IErpFinVoucherBiz`（跨域过账入口）。三类注入点覆盖 G1-G4 分级。
- **可恢复性契约对齐基准**：`docs/design/finance/posting-log.md` §错误传播分级策略 G1-G4（G1/G2 finance sweep 覆盖；G3 编排层；G4 无 sweep 域 dispatcher 经 `IErpSysNotificationBiz` 告警 + 恢复路径分级：assets 折旧有期末前置检查兜底，hr/qa/projects/maintenance 仅告警 + 试算平衡）。

**剩余差距**：无统一 harness；finance/hr/qa/maintenance 4 域过账悬挂路径无 per-point 测试；无跨域可恢复性断言契约。

## Goals

> 范围 = 设计文档 §3.5 裁决（路径 A 应用层 stub/override）+ §6 裁决（CI C-1 maven.yml 自动包含 + C-3 可选增强）。本计划是设计文档的实施执行。

- **harness 落地（设计文档 §5.1）**：新建 `FaultInjectionStubs`（或同等命名）通用 helper，封装两类桩机制（Proxy 桩 Facade `IErpFinVoucherBiz`/`IErpSysNotificationBiz` + 子类 override `*PostingExecutor`），复用 §1.2 既有先例范式（无 Mockito）+ field 注入可见性审计。
- **6 域过账悬挂测试（设计文档 §5.2）**：finance（G1/G2，A3-unit）/ hr / assets（折旧 G4）/ qa / projects / maintenance（G4，A4-alert）各 ≥1 故障注入测试，断言可恢复性契约（A1 posted 一致性 + A2 异常可观测 + 分级专属）。
- **CI 接线（设计文档 §7.4）**：C-1（零 CI 改动，`maven.yml` 的 `mvn test` 自动包含）+ C-3 可选增强（`compliance.yml` grep 6 域测试存在性，对齐 F8 单向收紧）。

## Non-Goals

- **不修改 nop-entropy 源码 + 零 ORM 变更**（设计文档 §5.3 边界：路径 A 应用层测试代码，零平台改动）。
- **不改各域 `*PostingDispatcher`/`*PostingExecutor` 生产代码**（设计文档 §5.3：仅经 field 注入 stub，除非 field 可见性须调整——R1，Phase 2 裁决）。
- **不覆盖 finance sweep 完整链路**（设计文档 §4.3：`ErpFinPostingException` 记录持久化 + 重试 + MANUAL 升级由引擎内部 `ErpFinPostingExceptionRecorder` 承载，Facade 边界 Proxy 桩绕过 Recorder → 属 A3-integration successor，须更深注入点/端到端测试）。
- **不覆盖 logistics（P1-MA2-080）**（设计文档 §1.3：第 7 域 successor）。
- **不覆盖其余故障模式**（并发冲突 / 超时 / 外部集成失败，设计文档 §4.3 successor）。
- **不重跑变异测试**（Q4 消费 Q1 盲区清单，不负责重跑；Q4 排期不阻塞于 Q1 全域完成——设计文档 §9.3）。

## Task Route

- Type: `implementation-only change`（测试代码新建 harness + 6 域故障注入测试 + 可选 CI grep；零 ORM/契约/生产代码变更）
- Owner Docs: 设计文档 `docs/architecture/quality-engineering/fault-injection.md`（收敛实施契约）；`docs/design/finance/posting-log.md` §错误传播分级策略 G1-G4（可恢复性断言对齐基准）；`docs/audits/arm-index.md`（finding-ID 权威索引）
- Skill Selection Basis: AGENTS.md 强制技能扫描完成。工作面向测试编写（JUnit 5 + 反射 Proxy stub + 子类 override + `IErpFinVoucherBiz`/`IErpSysNotificationBiz` Facade），匹配 `nop-testing`（测试基类/Proxy 范式/`@RegisterExtension`）。`nop-backend-dev` 不匹配（不改生产 BizModel/Processor）。设计文档 §5 亦明示「Phase 2 起草加载 nop-testing skill」。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 故障注入测试是标准 JUnit 测试，无 JVM agent / faketime / nightly 调度（设计文档 §7.3：`maven.yml` 的 `mvn test` 自动包含）。field 注入可见性（设计文档 §3.5 R1）须 Phase 2 逐 dispatcher 核验（package-private 同包测试 / private 反射 set）。

## Execution Plan

### Phase 1 - harness 通用化抽象 + field 注入可见性审计

Status: planned
Targets: harness 工具类（位置 Phase 2 裁决：`module-common-test` 共享 vs 各域 `src/test` 内聚）
Skill: nop-testing

- Item Types: `Add | Decision`
- Prereqs: 设计文档审查收敛（已满足）

- [ ] Add: 新建 `FaultInjectionStubs`（或同等命名，设计文档 §5.1 step 1）
      - `throwingVoucherBiz(String methodName, ErrorCode/RuntimeException)` —— Proxy 桩 `IErpFinVoucherBiz` 指定方法抛异常
      - `recordingNotificationBiz(String[] capturedEventType)` —— Proxy 桩 `IErpSysNotificationBiz` 录制 `notify` 调用
      - `throwingExecutor(Class<? extends *PostingExecutor>, String methodName)` —— 子类 override 模板工厂
      - 复用 §1.2 既有先例 `InvocationHandler` + `defaultReturn` helper（已验证）
      - 无 Mockito（对齐 R1.16 范式）
      - Skill: nop-testing
- [ ] Decision: harness 落地位置裁决（`module-common-test` 共享 vs 各域 `src/test` 内聚）+ field 注入可见性清单（设计文档 §3.5 R1，§5.1 step 2）：逐 dispatcher 核验 `voucherBiz`/`notificationBiz`/`executor` field 可见性，package-private 同包测试 / private 反射 set / 经测试构造器注入。每域注入方式清单落盘。
      - 记录候选 + 替代 + 残留风险（plan authoring guide §规则 9）
      - Skill: nop-testing
- [ ] Decision | Add: 路径 C JUnit 5 `Extension` 作 `@InjectFault(domain, method, type)` 声明式触发 API（设计文档 §3.5 R4）——**先 Decision**：裁决样板减少收益是否抵复杂度；**裁决引入才 Add**，否则 harness 核心注入机制保持路径 A Proxy/子类 stub（Extension 非必需，仅编排层补充）。
      - Skill: nop-testing

Exit Criteria:

- [ ] `FaultInjectionStubs` 落盘（grep 命中 `throwingVoucherBiz`/`recordingNotificationBiz`）+ 位置裁决 + field 注入可见性清单落盘

### Phase 2 - 6 域过账悬挂故障注入测试

Status: planned
Targets: 6 域 `erp-*-service/src/test` 新建故障注入测试类
Skill: nop-testing

- Item Types: `Add | Proof`（Add-heavy，6 域新建测试）
- Prereqs: Phase 1 done（harness 可用）

- [ ] Proof: **finance G1/G2（A3-unit）**——`NotesPostingDispatcher`（或 `ErpFinPostingProcessor`），Proxy 桩 `IErpFinVoucherBiz.post` 抛 → 断言 posted=false（A1）+ dispatcher catch 非完全静默（A2，LOG/alert）。**注**：finance sweep 完整链路（Recorder 记录 + 重试 + MANUAL 升级）由引擎内部承载，Facade 桩绕过 Recorder → A3-integration successor，**不在 unit harness**。
      - Skill: nop-testing
- [ ] Proof: **assets G4（A4-alert）**——`DepreciationPostingDispatcher`（设计文档 §4.2：折旧 G4，非 Cap/Disposal G1/G2），Proxy 桩 `IErpFinVoucherBiz.post` 抛 + Proxy 桩 `IErpSysNotificationBiz` 录制 → 断言 captured event type `ast.depreciation-posting-failure`（A4）+ posted=false 但可恢复（恢复经期末前置检查兜底，line 127-136）
      - 复用既有先例 `TestDepreciationPostingFailureAlert` 范式
      - Skill: nop-testing
- [ ] Proof: **projects G4（A4-alert）**——`TimesheetPostingDispatcher`/`ProjectPostingExecutor.postEvent` 抛 → 断言告警 + posted=false（恢复经告警 + 试算平衡，无前置检查）。复用 `TestTimesheetPostingFailureAlert` 范式。
      - Skill: nop-testing
- [ ] Proof: **hr G4（A4-alert）**——`SalaryPostingDispatcher`/`SalaryPostingExecutor`，harness 首次覆盖 → 断言告警 + posted=false（恢复经告警 + 试算平衡）
      - Skill: nop-testing
- [ ] Proof: **qa G4（A4-alert）**——`NcrPostingDispatcher`，A2.12 MANUAL_POST 路径，harness 首次覆盖 → 断言告警 + posted=false
      - Skill: nop-testing
- [ ] Proof: **maintenance G4（A4-alert）**——`MaintenanceLaborPostingDispatcher`/`MaintenanceIssuePostingDispatcher`，harness 首次覆盖 → 断言告警 + posted=false
      - Skill: nop-testing
- [ ] Proof: 与 Q6 协同——故障注入测试的 Proxy stub / 子类 override 须是测试内局部实例（不全局替换 IoC bean / 不改全局静态状态，设计文档 §6 验收 4），确保与 Q6 thread-local clock 并行隔离无冲突
      - Skill: nop-testing

Exit Criteria:

- [ ] 6 域各有故障注入测试类（设计文档 §6 验收 2），每测试断言覆盖 A1+A2 + 分级专属（A3-unit 或 A4-alert，设计文档 §6 验收 3）；stub 局部性核验通过

### Phase 3 - Q1 协同消费 + CI 覆盖率门控（可选）

Status: planned
Targets: Q4 优先覆盖候选（若 Q1 已产出）；`.github/workflows/compliance.yml`（可选 C-3）
Skill: nop-testing

- Item Types: `Proof | Add | Decision`
- Prereqs: Phase 2 done（6 域基础覆盖）；Q1 Phase 2 盲区清单（若已产出则消费，未产出则不阻塞）

- [ ] Proof: **Q1 协同消费**（设计文档 §9.2）——若 sibling plan Q1 Phase 2 已产出盲区类清单，提取「Q4 优先覆盖候选」（交集：Q1 盲区类 ∩ 6 域过账悬挂路径，首批交集仅 finance）。对优先候选补充故障注入覆盖；若 Q1 尚未产出（Q4 不阻塞），按 Phase 2 代表性 dispatcher 覆盖。
      - Skill: nop-testing
- [ ] Decision: CI 覆盖率门控裁决（设计文档 §7.3）——C-1（maven.yml 自动包含，零 CI 改动）作主路径是否足够，或引入 C-3 可选增强（`compliance.yml` grep 6 域测试存在性，命中域数 ≥6 单向收紧，对齐 F8 架构）。
      - 记录候选 + 替代 + 残留风险（设计文档 §7.3 R5：无显式门控则覆盖回潮无预警）
      - Skill: none
- [ ] Add: （若裁决纳入 C-3）`compliance.yml` 加 step：`rg -l "FailureHangs|FailureAlert|FaultInjection" module-{finance,hr,assets,quality,projects,maintenance}/erp-*-service/src/test/`，命中域数 ≥6（单向收紧）。新写 grep 检查逻辑。
      - Skill: none

Exit Criteria:

- [ ] Q1 协同消费裁决落盘（已产出则消费，未产出则登记不阻塞）；CI 门控裁决落盘（C-1 主路径 + C-3 增强是否引入）

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_04416b1eaffezXngxHpnTYuDW3`，独立子代理 fresh session cold context）— 0 BLOCKER / 0 MAJOR / 3 MINOR。全部 Current Baseline 实仓主张独立复核 PASS（`rg -il "fault.injection|FaultInjector|@InjectFault"` EXIT=1 零 harness、7 per-point 先例 + 2 incidental 命中、30 生产 dispatcher + 6 域代表 dispatcher 全存在、`IErpFinVoucherBiz` + 6 executor + `ErpFinPostingProcessor` 存在、设计文档 Review Record 2 轮收敛无 BLOCKER/MAJOR、sibling plan 引用存在）。Q1↔Q4 非阻塞（首批交集仅 finance）核验 PASS。MINOR-1（Phase 1 可选 Extension 项标 `Add` 但实为 conditional）已采纳——改为 `Decision | Add`（先 Decision 后 Add）。MINOR-2（Closure Gates §6 验收 6 未展开）已采纳——下方验收条目展开。MINOR-3（Phase 术语双用 cosmetic）不修。无 BLOCKER/MAJOR → converged → 转 active。

## Closure Gates

> 设计文档 §6 验收判据为本计划 closure 契约。全量 `mvn clean install -DskipTests` + `mvn test`（含故障注入测试）在此一次性运行。

- [ ] 范围内行为完成（设计文档 §6 验收 1-6）
  - 通用化 harness 落地（`rg "FaultInjectionStubs|throwingVoucherBiz|recordingNotificationBiz" module-*/erp-*-service/src/test/` 或 `module-common-test/` 命中）
  - 6 域各有故障注入测试（finance/hr/assets/qa/projects/maintenance 各 ≥1）
  - 可恢复性断言契约成立：A1（posted 一致性）+ A2（异常可观测，无完全静默）+ A3-unit（finance dispatcher catch 可恢复性）+ A4-alert（G4 域 `IErpSysNotificationBiz.notify` captured event type 匹配 `<domain>.posting-failure`）
  - 不污染并行测试（stub 局部实例，不全局替换 IoC bean / 不改全局静态状态，与 Q6 thread-local clock 协同）
  - 无双真相源（设计文档 §6 验收 6）：Current Baseline/Goals 引用设计文档 §1 + posting-log.md G1-G4 + arm-index，不重推导证据
- [ ] 相关文档对齐：设计文档 `fault-injection.md` 无未经批准偏离；`docs/logs/{year}/{month}-{day}.md` 追加日志条目
- [ ] 已运行验证：`mvn clean install -DskipTests`（156 模块 BUILD SUCCESS）+ `mvn test`（0 failures / 0 errors，故障注入测试绿 + 不破坏既有基线）；compliance checker 不新增命中（零生产代码 daoFor/import 变更）
- [ ] 无范围内项目降级为 deferred/follow-up（logistics/finance-sweep/其余故障模式经设计文档 §1.3/§4.3 显式 out-of-scope）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中
- [ ] **实现与设计文档一致**（无未经设计文档 `fault-injection.md` 批准的范围偏离）

## Deferred But Adjudicated

### finance sweep 完整链路（A3-integration）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 设计文档 §4.3——`ErpFinPostingException` 记录持久化（`ErpFinPostingExceptionRecorder`）+ `ErpFinDeferredPostingRetryHelper` 重试 + retryCount≥MAX→MANUAL 升级由引擎内部承载，Facade 边界 Proxy 桩绕过 Recorder。覆盖须更深注入点（throwing `IErpFinAcctDocProvider`）或端到端/定时任务测试，超 Q4 unit harness scope。
- Successor Required: yes —— 触发条件：本期后独立集成测试计划覆盖完整 sweep 链路。

### logistics（P1-MA2-080）过账悬挂覆盖

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 设计文档 §1.3 裁决 logistics 是同型根因第 7 域 successor；其 `ErpLogShipmentBizModel.onDelivered` 吞异常 + `scanForPolling` 不重试比 peer 更严重（无 finance sweep 兜底），Q4 首轮 6 域 harness 沉淀后扩展更可控。
- Successor Required: yes —— 触发条件：6 域 harness 沉淀后扩展；优先级高。

### 引擎内部端到端故障注入（路径 B 字节码插桩）

- Classification: `watch-only successor`
- Why Not Blocking Closure: 设计文档 §3.5 裁决路径 B 否决（跨仓库耦合 + 过度工程 + 回归面大）；SPI 边界故障已足够触发真实 dispatcher catch-swallow 路径。
- Successor Required: yes —— 触发条件：Q4 首轮证明 SPI 边界故障不足以覆盖某类可恢复性场景时。

### 其余故障模式（并发冲突 / 超时 / 外部集成失败）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 设计文档 §4.3——Q4 首轮聚焦 tryPost 吞异常同型根因。
- Successor Required: yes —— 触发条件：过账悬挂路径 harness 沉淀后扩展。

## Closure

Status Note: pending（独立结束审计后填写）

Closure Audit Evidence:

- Auditor / Agent: pending（独立结束审计子代理，新会话 fresh cold context）

Follow-up:

- finance sweep A3-integration / logistics 第 7 域 / 引擎内部端到端 / 其余故障模式 successor（见上 Deferred）。
