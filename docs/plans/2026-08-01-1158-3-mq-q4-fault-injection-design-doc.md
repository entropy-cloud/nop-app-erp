# 2026-08-01-1158-3-mq-q4-fault-injection-design-doc 故障注入测试 Phase 1 设计文档

> Plan Status: active
> Last Reviewed: 2026-08-01
> Mission: audit-remediation
> Work Item: MQ Q4
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q4（line 677, 787）；`docs/architecture/quality-engineering/README.md` §实施顺序裁决（位 3，Q1↔Q4 协同）
> Related: `docs/plans/2026-08-01-1121-1-mq-q0-quality-gap-analysis-readme.md`（Q0 顺序基线，前置 done）；`docs/plans/2026-08-01-1158-2-mq-q1-mutation-testing-design-doc.md`（Q1，Q1↔Q4 协同——Q1 盲区类即 Q4 优先覆盖路径）；`docs/design/finance/posting-log.md`（过账错误传播 G1-G4 分级 + 延迟重试模型，Q4 可恢复性断言契约权威真相源）；MR1.16（P1-MA2-032/048/060/064/068/074 + P1-MA4 系列单点修复，Q4 是其系统性回归保护超集）
> Audit: required

## Current Baseline

> 本计划是 MQ 文档先行工作流的 **Phase 1**：产出审查收敛的设计文档 `docs/architecture/quality-engineering/fault-injection.md`，**不改任何代码/ORM/CI**。MQ roadmap（line 843-862）与 Q0 README（line 20-22）明确：Phase 1 设计文档经独立子代理 ≥2 轮审查收敛后，方可编写 Phase 2 实现 plan。

**audit-remediation 主线状态**：M0 / MA1-MA7 / MR1-MR6 / MV / MG 全 done；MR6 milestone CLOSED。验证基线 `mvn clean install -DskipTests` 156 模块全绿；`mvn test` 1903 测试 0 failures。

**Q4 现状（NOT FOUND + 同型根因 + 既有先例证据，引用 Q0 README §范围矩阵 §Q4 + roadmap line 787 + 实仓复核，核验日期 2026-08-01）**：

- 无**通用化**故障注入 harness：`rg -il "fault.injection|FaultInjector|@InjectFault" --glob '*.java' --glob '*.xml'` 零命中（关键字零命中）。但**已有 per-point 过账悬挂测试先例**（非通用化，散落各域），实测存在，构成路径 A（应用层 stub/override）的可行性与起点证据：
  - `module-inventory/.../posting/TestErpInvPostingDispatcherFailureHangs.java`（Proxy stub `IErpFinVoucherBiz.post` 抛 + `InvPostingExecutor.postEvent` 抛）
  - `module-purchase/.../posting/TestErpPurPostingDispatcherFailureHangs.java`
  - `module-sales/.../posting/TestErpSalPostingDispatcherFailureHangs.java`
  - `module-assets/.../posting/TestDepreciationPostingFailureAlert.java`（R1.16）
  - `module-inventory/.../processor/TestErpInvLandedCostReverseFailureAlert.java`
  - `module-projects/.../posting/TestTimesheetPostingFailureAlert.java`
  - `module-manufacturing/.../TestErpMfgVarianceRecomputeReversal.java`（`ThrowingMfgPostingExecutor extends MfgPostingExecutor`）
  - 这些 per-point 测试无统一 harness 抽象、无跨域可恢复性断言契约——Q4 正是要把它们泛化为系统性保护。
- 同型根因跨 6 域（**tryPost 吞异常 → posted=false 静默悬挂**）：finance P1-MA2-032 / hr P1-MA2-048 / assets P1-MA2-060 / qa（**finding-ID 064 vs 080 漂移——roadmap line 786/Q0 README 标 064，R1.16[roadmap line 154]与 Q4 工作项表[line 677]标 080；设计文档 §现状评估 须核对 arm-index 权威值**）/ projects P1-MA2-068 / maintenance P1-MA2-074。
- 过账错误传播权威真相源为 `docs/design/finance/posting-log.md`（§错误传播分级 G1-G4 + 延迟重试模型：G1 瞬时→`DeferredPostingSweepJob` PENDING→retry；G2 永久→retryCount≥MAX(3)→MANUAL 终态 + `IErpSysNotificationBiz` 告警；G3 编排层跨域/异步；G4 无 sweep 覆盖域→独立告警 + 期末前置检查兜底）。**注：`processor-extension-pattern.md` 仅一处顺带提及过账（line 66 `@SingleSession` 示例），无过账失败回退规则**——Q4 可恢复性断言契约须对齐 `posting-log.md` G1-G4 延迟重试模型，而非"显式文档状态回退"。
- MR1.16（R1.16 / R1.26 等）已修**单点**（catch 收窄 + `IErpSysNotificationBiz` 告警 + 不进死状态 + 期末结账前置检查扩展），但**无系统性回归保护**——同型根因在新增过账路径时无故障注入测试拦截。
- 关键约束：故障注入须明确 nop-entropy 改造 vs 应用层边界（roadmap line 787）。**实仓复核：`IPostingDispatcher` 接口不存在**（roadmap line 786 / Q0 README 沿用此名为误导）。真实过账 SPI = 各域具体 `*PostingDispatcher`（Inv/Sal/Pur/Ast/Mfg）+ 各域 `*PostingExecutor`（Inv/Sal/Pur/Mfg/Ast）+ finance facade `IErpFinVoucherBiz`。路径 A 须 stub/override 这些**各域具体类**，精确注入点清单是设计文档 §现状评估 的输入（须先盘点）。
- 与 Q1 协同：Q1 发现的盲区类正是 Q4 应优先覆盖的可恢复性路径（roadmap line 786，Q0 README line 152）。

**剩余差距**：无 Q4 设计 owner doc。故障注入 harness 设计（受控异常 / 超时 / 事务回滚注入点 + mock dispatcher）、可恢复性断言契约、与现有测试框架集成方式均未裁决，须在 Phase 1 文档中独立审查后定夺。

## Goals

- 产出 MQ Q4 的 Phase 1 设计文档 `docs/architecture/quality-engineering/fault-injection.md`，经独立子代理 ≥2 轮审查收敛（第 1 轮规范合规 + 第 2 轮覆盖面/可执行性），覆盖 MQ 文档先行工作流要求的 6 节：
  1. **现状评估**（引用实仓证据：零故障注入 + 6 域同型根因 finding ID + MR1.16 单点修复边界）
  2. **目标与非目标**（覆盖 6 域过账悬挂路径的可恢复性；不为全 mutation 路径建 harness）
  3. **技术选型**——应用层 test-scope stub/override 各域具体 `*PostingDispatcher`/`*PostingExecutor` + finance facade `IErpFinVoucherBiz`（首选，既有 per-point 先例已证可行）vs 平台层字节码插桩 vs JUnit 5 `Extension` 注入点的替代评估 + 裁决理由
  4. **实施步骤**（harness 落地 + 6 域过账悬挂路径覆盖 + 可恢复性断言）
  5. **验收判据**（6 域过账悬挂路径均有故障注入测试 + 可恢复性断言契约成立——故障后系统进入可恢复状态而非静默悬挂，对齐 `posting-log.md` G1-G4 延迟重试模型 + 不污染并行测试）
  6. **CI 门控设计**（故障注入测试是否纳入 mandatory 回归层 + 与 `.github/workflows/compliance.yml` 集成）
- 文档须显式声明可恢复性断言契约（"故障后系统进入可恢复状态而非静默悬挂"的形式化定义）。
- 文档须显式声明与 Q1 的协同接口（消费 Q1 输出的盲区类清单，作为优先覆盖目标）。

## Non-Goals

- **不实现任何代码/ORM/CI 变更**——本计划仅产出设计文档。Phase 2 实现（harness + 6 域测试）是**独立的后续 plan**，须在本设计文档审查收敛后方可起草（MQ 文档先行工作流）。
- 不修改 `nop-entropy` 源码（平台层字节码插桩若被选中，其实施属 Phase 2 + 须遵守 AGENTS.md nop-entropy 日志规则）。
- 不覆盖全 mutation 路径（仅 6 域过账悬挂同型根因；其余故障模式 successor）。
- 不重新推导 NOT FOUND 证据（引用 Q0 README，避免双真相源）。
- 不编写 Q1 变异测试设计（同批独立 plan）。

## Task Route

- Type: `app-layer design change`（设计文档编写；纯文档，零代码）
- Owner Docs: `docs/architecture/quality-engineering/README.md`（Q0 顺序基线 + 文档先行工作流引用）；`docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q4 + §横切关注点 §文档先行工作流；`docs/design/finance/posting-log.md`（过账错误传播 G1-G4 分级 + 延迟重试模型——Q4 可恢复性断言契约权威真相源）；`docs/architecture/processor-extension-pattern.md`（Processor 编排层 `@SingleSession` 事务/Session 分层参照）
- Skill Selection Basis: AGENTS.md 强制技能扫描已完成——`nop-backend-dev`/`nop-frontend-dev`/`nop-testing`/`nop-debugging` 均不匹配"编写故障注入 harness 设计文档"。`nop-testing`（JunitAutoTestCase/快照）触及测试框架但其范围是测试编写，留待 Phase 2 实现计划加载。故 `Skill: none`（与 roadmap Q4 行 Skill 列 + Q0 README 一致）。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. 本计划纯文档，不涉及端口/环境变量/CORS/密钥/.env/外部服务。

## Execution Plan

### Phase 1 - 编写 Q4 设计文档草稿

Status: planned
Targets: `docs/architecture/quality-engineering/fault-injection.md`（新建）
Skill: none

- Item Types: `Add | Decision`
- Prereqs: Q0 done（已满足）；Q0 README §实施顺序裁决落盘（已满足）；MR1.16 单点修复已落地（已满足，作为 Q4 系统性保护的基线参照）

- [ ] Add: 创建 `fault-injection.md`，含 MQ 文档先行工作流要求的 6 节骨架
      - Skill: none
- [ ] Add: §现状评估 —— 引用（非重推导）Q0 README §Q4 + roadmap line 787 + MR1.16 + 本计划 Current Baseline 实仓复核：无通用化 harness（关键字零命中）**但**已有 per-point 过账悬挂测试先例（枚举 7 个测试类作为路径 A 起点证据）、6 域同型根因 finding ID（**设计文档须核对 arm-index 权威值，消解 qa 064-vs-080 漂移**）、MR1.16 单点修复边界、过账错误传播真相源 `posting-log.md` G1-G4。标注可复现核验命令 + 核验日期。
      - Skill: none
- [ ] Add: §过账 SPI 注入点盘点 —— 盘点真实过账 SPI（各域具体 `*PostingDispatcher` / `*PostingExecutor` / `IErpFinVoucherBiz`），作为路径 A stub/override 的精确注入点清单（**核验 `IPostingDispatcher` 不存在**，纠正 roadmap line 786 / Q0 README 沿用名）
      - Skill: none
- [ ] Decision: §技术选型 —— 评估并裁决故障注入机制：
      - 路径 A：应用层 test-scope stub/override 各域具体 `*PostingDispatcher`/`*PostingExecutor` + finance facade `IErpFinVoucherBiz`（首选——既有 per-point 先例已证可行，应用层内闭环，避免跨仓库依赖）
      - 路径 B：平台层字节码插桩（触及 nop-entropy，跨仓库）
      - 路径 C：JUnit 5 `Extension` + 受控异常/超时/事务回滚注入点（轻量，但可能不足以覆盖各域 dispatcher SPI）
      - 记录候选 + 考虑的替代 + 残留风险（如路径 A 对真实过账链路保真度 + 跨域具体类盘点工作量、路径 B 的平台升级耦合、路径 C 的注入点覆盖范围）
      - Skill: none
- [ ] Add: §可恢复性断言契约 —— 形式化定义"故障后系统进入可恢复状态而非静默悬挂"，**对齐真相源 `docs/design/finance/posting-log.md` §错误传播分级 G1-G4 的延迟重试模型**（**非"显式文档状态回退"**——posting-log.md 权威：G1 瞬时→`DeferredPostingSweepJob` 重试；G2 永久→MANUAL 终态 + 告警；G4 无 sweep 域→独立告警 + 期末前置检查兜底）。契约要素：故障后 posted 标志与实际过账结果一致 + PostingException 记录存在 + 单据可重试/可冲销或进异常工作台。
      - Skill: none
- [ ] Add: §实施步骤 —— harness 落地位置 + 6 域过账悬挂路径覆盖清单（finance/hr/assets/qa/projects/maintenance）+ 与 Q1 盲区类清单的消费方式
      - Skill: none
- [ ] Add: §验收判据 —— 6 域过账悬挂路径均有故障注入测试 + 可恢复性断言契约成立 + 不污染并行测试（注意与 Q6 时钟硬化的并行隔离协同）
      - Skill: none
- [ ] Add: §CI 门控设计 —— 裁决故障注入测试是否纳入 mandatory 回归层 + 与 `.github/workflows/compliance.yml` 集成方式
      - Skill: none
- [ ] Add: §与 Q1 协同接口 —— 声明消费 Q1 输出的盲区类清单（哪些类的存活变异体指向过账可恢复性路径），作为 Q4 Phase 2 优先覆盖目标
      - Skill: none

Exit Criteria:

> 本计划纯文档，零代码/ORM/CI 变更。完整仓库 `typecheck`/`build`/`test` 不适用（按 plan authoring guide，无代码更改的计划删除验证命令门控）。

- [ ] `fault-injection.md` 落盘，含上述 6 节 + 可恢复性断言契约 + Q1 协同接口，技术选型 Decision 记录候选+替代+残留风险三要素
- [ ] §现状评估每条证据标注可复现核验命令 + 核验日期

### Phase 2 - 独立子代理设计文档审查循环（≥2 轮至收敛）

Status: planned
Targets: `docs/architecture/quality-engineering/fault-injection.md`（`## Review Record` 节）
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 1 草稿落盘

- [ ] Proof: 第 1 轮审查——**规范合规审查**，由独立子代理（新会话）执行。审查项：6 节结构完整性 / 与项目约定一致性 / 反模式检查（无双真相源、是否误把 MR1.16 单点修复当系统性保护、是否误把 per-point 先例当通用化 harness）/ owner doc 引用正确性（`posting-log.md` G1-G4 延迟重试模型 + `IPostingDispatcher` 不存在的实仓纠正）。输出 BLOCKER/MAJOR/MINOR。
      - Skill: none
- [ ] Proof: 第 2 轮审查——**覆盖面与可执行性审查**，由**另一个**独立子代理（不同 task id，新会话）执行。审查项：三路径替代是否充分评估 / 可恢复性断言契约是否可验证 / 6 域路径覆盖是否完整（核对 finding ID）/ 应用层 vs 平台层边界裁决是否避免跨仓库依赖 / 与 Q6 并行隔离是否冲突 / 与 Q1 协同接口是否可消费。输出 BLOCKER/MAJOR/MINOR。
      - Skill: none
- [ ] Add: 作者据审查意见修订文档并重审，直至两轮均无 BLOCKER/MAJOR；`## Review Record` 节持久化两轮审查者 task id + 轮次 + 结论 + 修改摘要
      - Skill: none

Exit Criteria:

- [ ] §Review Record 记录 ≥2 轮审查，两轮由不同子代理会话执行，无残留 BLOCKER/MAJOR
- [ ] 可恢复性断言契约经审查后可验证（或据审查修订后可验证）

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_044841273ffeCJ4V96QhypM1KT`，独立子代理 fresh session cold context）— 0 BLOCKER / 3 MAJOR / 2 MINOR。F1（`IPostingDispatcher` 接口实仓不存在，真实 SPI 为各域具体 `*PostingDispatcher`/`*PostingExecutor` + `IErpFinVoucherBiz`，须重写路径 A + 加注入点盘点）/ F2（"零故障注入"误导——忽略 ≥7 个既有 per-point 过账悬挂测试先例如 `TestErpInvPostingDispatcherFailureHangs`/`ThrowingMfgPostingExecutor`，须重写为"无通用化 harness 但有 per-point 先例"）/ F3（可恢复性规则误引 `processor-extension-pattern.md`，该文仅 line 66 顺带提过账无回退规则；真相源为 `posting-log.md` G1-G4 延迟重试模型，非"显式文档状态回退"）/ F4（qa finding-ID 064-vs-080 漂移）/ F5（Phase 2 item-type）。
- Independent draft review iteration 2: **accept**（`ses_0447dc20cffeG1I2CFMhjynr4p`，独立子代理 fresh session cold context，复查修订后）— F1-F5 全部 **resolved**（实仓复核：`IPostingDispatcher` 零命中确认；路径 A 重写后的 SPI 全部存在；7 个 per-point 先例测试存在性抽查 PASS；`posting-log.md` G1-G4 + `DeferredPostingSweepJob` 延迟重试内容逐字核实；064-vs-080 漂移已 inline 标注并路由 §现状评估 核对 arm-index；Phase 2 item-type 已 `Proof | Add`）。0 新缺陷。Phase-1 doc-only 范围保持（Goals/Non-Goals/Targets 全绑定 `fault-injection.md`，无 code/ORM/CI 蔓延）；doc-only closure-gate 省略仍正当。converged → 转 active。

## Closure Gates

> 本计划无代码/ORM/view/CI 变更（纯设计文档）。按 plan authoring guide §Closure Gates："对于无代码更改的计划（仅文档），删除验证命令门控并说明原因"——故不设 `mvn typecheck/build/test` 门控，原因：零 Java/ORM/CI 变更，全量构建无回归面。

- [ ] 范围内行为完成：`fault-injection.md` 6 节 + 可恢复性断言契约 + Q1 协同接口落盘且 Review Record 收敛
- [ ] 相关文档对齐：文档引用 Q0 README（无双真相源）；与 roadmap §MQ Q4 + `posting-log.md` G1-G4 一致
- [ ] 无验证命令门控（纯文档计划，原因如上）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查（本计划本身）已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中
- [ ] `docs/logs/{year}/{month}-{day}.md` 追加本计划日志条目（计划级结束步骤）

## Deferred But Adjudicated

### Q4 Phase 2 实现（harness + 6 域过账悬挂故障注入测试）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: MQ 文档先行工作流强制要求 Phase 1 设计文档审查收敛后方可编写 Phase 2 实现 plan。本计划仅交付设计文档。
- Successor Required: yes —— 触发条件：本计划 done（设计文档审查收敛）+ 技术选型 Decision 落定。届时 DRAFT_PLANS 起草 Phase 2 实现 plan（加载 `nop-testing` skill），plan 引用本文档作为范围与验收依据。建议在 Q1 Phase 2 产出盲区类清单后启动以充分消费协同。

### 其余故障模式（非过账悬挂）的故障注入覆盖

- Classification: `optimization candidate`
- Why Not Blocking Closure: Q4 首轮聚焦 6 域过账悬挂同型根因。其余故障模式（如并发冲突 / 超时 / 外部集成失败）作为 successor。
- Successor Required: yes —— 触发条件：过账悬挂路径 harness 沉淀后扩展。

## Closure

Status Note: <待执行与独立结束审计后填写>

Closure Audit Evidence:

- Auditor / Agent: <独立结束审计子代理，新会话 fresh cold context>
- Evidence: <task id / 核验记录>

Follow-up:

- Q4 Phase 2 实现 plan（设计文档收敛后起草；建议在 Q1 Phase 2 产出盲区类清单后启动）。
