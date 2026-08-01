# 2026-08-01-1158-1-mq-q6-clock-test-infrastructure-design-doc 时钟测试基础设施硬化 Phase 1 设计文档

> Plan Status: completed
> Last Reviewed: 2026-08-01
> Mission: audit-remediation
> Work Item: MQ Q6
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q6（line 679, 789）；`docs/architecture/quality-engineering/README.md` §实施顺序裁决（位 1）
> Related: `docs/plans/2026-08-01-1121-1-mq-q0-quality-gap-analysis-readme.md`（Q0 顺序基线，前置 done）；bug `docs/bugs/2026-08-01-finance-period-resolution-month-rollover.md`（Q6 痛点根因证据）；plan `docs/plans/2026-08-01-0803-1`（R6.9 finance 测试日期硬化——单点补丁，Q6 是其系统性根治超集）
> Audit: required

## Current Baseline

> 本计划是 MQ 文档先行工作流的 **Phase 1**：产出审查收敛的设计文档 `docs/architecture/quality-engineering/clock-test-infrastructure.md`，**不改任何代码/ORM/CI**。MQ roadmap（line 843-862）与 Q0 README（line 20-22）明确：Phase 1 设计文档经独立子代理 ≥2 轮审查收敛后，方可编写 Phase 2 实现 plan。故本计划的范围**仅为设计文档**。

**audit-remediation 主线状态**：M0 / MA1-MA7 / MR1-MR6 / MV / MG 全 done；MR6 milestone CLOSED。验证基线 `mvn clean install -DskipTests` 156 模块全绿；`mvn test` 1903 测试 0 failures。

**Q6 现状（NOT FOUND + 痛点证据，引用 Q0 README §范围矩阵 §Q6 + bug doc，核验日期 2026-08-01）**：

- 时钟入口为平台 API：`nop-entropy` 的 `CoreMetrics.today()` / `CoreMetrics.currentTimeMillis()`（`CoreMetrics.java:21-40` 默认 `IClock` 包装 `System.currentTimeMillis()`/`LocalDate.now()`）。生产代码已合规使用平台 API（如 `NotesPostingDispatcher:79,108` `voucherDate = issueDate ?: CoreMetrics.today()`、`ErpFinPostingProcessor:495 resolveOpenPeriod(voucherDate)`）。
- 测试侧冻结机制存在但**全局静态并行不安全**：`module-common-test/.../AbstractFrozenClockExtension.java:63,68` 调用 `CoreMetrics.registerClock(...)`（`CoreMetrics.java:44`），15 域子类共用**一个全局静态时钟槽** → 同 JVM 并行测试互相污染。
- 月初翻车税（**唯一反复发作痛点**，roadmap line 98）：每月月初（系统时钟跨月，如 7/31→8/1），finance 域 4 个测试类由绿转红，合计 **1 failure + 10 errors**：
  - `TestErpFinBadDebtReversal`（3 errors）— 期间 `MONTH=8` 期望 7
  - `TestErpFinEmployeeAdvanceCashRepayReversal`（3 errors）— 期间 `NAME=2026-08` 错配
  - `TestErpFinNotesPayableStateMachine`（4 errors）— `POSTED=false` 期望 true（voucherDate=今天落在 seed 期间外）
  - `TestErpFinDashboard.testTrendMonthlySeries`（1 failure）— 趋势窗口相对"今天"滚动
  - 诊断陷阱：失败**形似**「过账吞异常致 posted 悬挂」回归，stash 隔离实验证伪（bug doc line 25-26）。
- R6.9 已做**单点**日期硬化（NotesPayable 按 voucher date 预置 OPEN 期间 + Dashboard `YearMonth.now()` 相对 seed），但**未根治**全局静态时钟并行不安全的根因——Q6 是其系统性超集。

**剩余差距**：无 Q6 设计 owner doc。技术选型（路径 A：nop-entropy `CoreMetrics` 加 thread-local clock 支持；路径 B：测试侧 `@ParameterizedTest` 多日期而非钉死单日）尚未裁决，须在 Phase 1 文档中独立审查后定夺。

## Goals

- 产出 MQ Q6 的 Phase 1 设计文档 `docs/architecture/quality-engineering/clock-test-infrastructure.md`，经独立子代理 ≥2 轮审查收敛（第 1 轮规范合规 + 第 2 轮覆盖面/可执行性），覆盖 MQ 文档先行工作流要求的 6 节：
  1. **现状评估**（引用实仓证据：`AbstractFrozenClockExtension.java:63,68` + bug doc 4 类失败 + R6.9 单点硬化边界）
  2. **目标与非目标**
  3. **技术选型**——路径 A（平台 `CoreMetrics` thread-local clock）vs 路径 B（测试侧日期参数化）vs 路径 C（混合：thread-local clock + 按需参数化）的替代方案、裁决理由、残留风险
  4. **实施步骤**（按所选路径，含 15 域子类兼容迁移方案 + 跨 nop-entropy 改造边界声明）
  5. **验收判据**（月初翻车税消除 + 并行测试不污染 + 15 域子类迁移完成性 + 回归绿）
  6. **CI 门控设计**（如适用——月初时钟滚动 CI 矩阵是否纳入）
- 审查记录持久化在文档 `## Review Record` 节（审查者 task id + 轮次 + 结论 + 修改摘要）。

## Non-Goals

- **不实现任何代码/ORM/CI 变更**——本计划仅产出设计文档。Phase 2 实现（含 `CoreMetrics` 改造或测试侧迁移）是**独立的后续 plan**，须在本设计文档审查收敛后方可起草（MQ 文档先行工作流）。
- 不修改 `nop-entropy` 源码（路径 A 若被选中，其实施属 Phase 2 + 须遵守 AGENTS.md「对 nop-entropy 的更改记录在 nop-entropy/ai-dev/logs/」）。
- 不重录 finance 测试快照（bug doc line 40 明确：按新月度重录快照只是把时间炸弹推到下个月，非修复）。
- 不覆盖 Q1-Q7 其他维度（各有独立 Phase 1 设计文档计划）。
- 不重新推导 NOT FOUND 证据（引用 Q0 README + bug doc，避免双真相源）。

## Task Route

- Type: `app-layer design change`（设计文档编写；纯文档，零代码）
- Owner Docs: `docs/architecture/quality-engineering/README.md`（Q0 顺序基线 + 文档先行工作流引用）；`docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q6 + §横切关注点 §文档先行工作流；bug `docs/bugs/2026-08-01-finance-period-resolution-month-rollover.md`
- Skill Selection Basis: AGENTS.md 强制技能扫描已完成——`nop-backend-dev`/`nop-frontend-dev`/`nop-testing`/`nop-debugging` 均不匹配"编写测试时钟基础设施设计文档"。`nop-testing`（JunitAutoTestCase/快照录制）触及测试框架但其范围是测试编写而非设计文档裁决，留待 Phase 2 实现计划加载。故 `Skill: none`（与 roadmap Q6 行 Skill 列 + Q0 README 一致）。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. 本计划纯文档，不涉及端口/环境变量/CORS/密钥/.env/外部服务。

## Execution Plan

### Phase 1 - 编写 Q6 设计文档草稿

Status: completed
Targets: `docs/architecture/quality-engineering/clock-test-infrastructure.md`（新建）
Skill: none

- Item Types: `Add | Decision`
- Prereqs: Q0 done（已满足）；Q0 README §实施顺序裁决落盘（已满足）

- [x] Add: 创建 `clock-test-infrastructure.md`，含 MQ 文档先行工作流要求的 6 节骨架
      - Skill: none
- [x] Add: §现状评估 —— 引用（非重推导）Q0 README §Q6 + bug doc 实仓证据：`AbstractFrozenClockExtension.java:63,68` 全局静态 `registerClock`、15 域子类、4 类 finance 月初失败（1 failure + 10 errors）、R6.9 单点硬化边界。每条标注可复现核验命令。
      - Skill: none
- [x] Decision: §技术选型 —— 评估并裁决三路径：
      - 路径 A：nop-entropy `CoreMetrics` 加 thread-local clock 支持（根治并行不安全，但跨仓库改平台，触及 nop-entropy 保护区域）
      - 路径 B：测试侧 `@ParameterizedTest` 多日期而非钉死单日（应用层内闭环，但 15 域子类迁移面大 + 不解全局静态槽并行污染）
      - 路径 C：混合（thread-local clock 根治并行 + 关键日期敏感类按需参数化）
      - 记录候选 + 考虑的替代 + 残留风险（如路径 A 的 nop-entropy 升级耦合、路径 B 的迁移工作量与并行不彻底）
      - Skill: none
- [x] Add: §实施步骤 —— 按所选路径给出 15 域子类兼容迁移方案 + 跨 nop-entropy 改造边界声明（哪些在应用层、哪些须平台 PR）+ finance 4 类回归验证步骤
      - Skill: none
- [x] Add: §验收判据 —— 月初翻车税消除（跨月滚动 4 类仍绿）+ 并行测试不污染（同 JVM 多域并行 `CoreMetrics` 不串扰）+ 15 域子类迁移完成性 + 全量 `mvn test` 0 failures
      - Skill: none
- [x] Add: §CI 门控设计 —— 裁决是否引入"月初时钟滚动 CI 矩阵"（如系统日期参数化跑 finance 套件）作为回归层；若引入，给出与现有 `.github/workflows/compliance.yml` 的集成方式
      - Skill: none

Exit Criteria:

> 本计划纯文档，零代码/ORM/CI 变更。完整仓库 `typecheck`/`build`/`test` 不适用（按 plan authoring guide，无代码更改的计划删除验证命令门控）。

- [x] `clock-test-infrastructure.md` 落盘，含上述 6 节，技术选型 Decision 记录候选+替代+残留风险三要素
- [x] §现状评估每条证据标注可复现核验命令 + 核验日期

### Phase 2 - 独立子代理设计文档审查循环（≥2 轮至收敛）

Status: completed
Targets: `docs/architecture/quality-engineering/clock-test-infrastructure.md`（`## Review Record` 节）
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 1 草稿落盘

- [x] Proof: 第 1 轮审查——**规范合规审查**，由独立子代理（新会话，不复用作者上下文）执行。审查项：文档结构完整性（6 节齐备）/ 与项目约定一致性（AGENTS.md + roadmap authoring + plan authoring + MQ 文档先行工作流）/ 反模式检查（无双真相源漂移、路径裁决是否误套 owner doc）/ 是否引用正确 owner doc（Q0 README + bug doc）。输出 BLOCKER/MAJOR/MINOR 分级修改意见。
      - Skill: none
- [x] Proof: 第 2 轮审查——**覆盖面与可执行性审查**，由**另一个**独立子代理（不同 task id，新会话）执行。审查项：目标是否可达 / 实施步骤是否可执行 / 验收判据是否可验证 / 是否遗漏关键风险（如 nop-entropy 升级耦合、15 域子类迁移顺序、并行测试隔离）/ 三路径替代方案是否充分评估 / 是否与现有测试基础设施冲突（`AbstractFrozenClockExtension` 15 子类）。输出 BLOCKER/MAJOR/MINOR 分级修改意见。
      - Skill: none
- [x] Add: 作者据审查意见修订文档并重审，直至两轮均无 BLOCKER/MAJOR；`## Review Record` 节持久化两轮审查者 task id + 轮次 + 结论 + 修改摘要
      - Skill: none

Exit Criteria:

- [x] §Review Record 记录 ≥2 轮审查，两轮由不同子代理会话执行，无残留 BLOCKER/MAJOR
- [x] 技术选型 Decision 经审查后仍自洽（或据审查修订后自洽）

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_044845aa3ffetgwfoQ27C3maoO`，独立子代理 fresh session cold context）— 0 BLOCKER / 0 MAJOR / 3 MINOR（全部 cosmetic，无契约缺陷）：(a) "Phase 2" 术语在计划内"审查收敛阶段"与 roadmap"实现阶段"间存在重载，上下文可消歧；(b) "如适用"措辞继承自 roadmap line 850，实际执行 item 为"裁决"非可选；(c) Task Route 类型 `app-layer design change` 可辩护。所有 Current Baseline 实仓主张独立复核 PASS（`registerClock` 于 `AbstractFrozenClockExtension.java:63,68`、精确 15 子类、4 个 finance 测试类、stash 隔离诊断、行引用）。doc-only closure-gate 省略有正当理由。已应用 rule-7 一致性修订：Phase 2 item-type `Proof` → `Proof | Add`。无 BLOCKER/MAJOR，converged → 转 active。

## Closure Gates

> 本计划无代码/ORM/view/CI 变更（纯设计文档）。按 plan authoring guide §Closure Gates："对于无代码更改的计划（仅文档），删除验证命令门控并说明原因"——故不设 `mvn typecheck/build/test` 门控，原因：零 Java/ORM/CI 变更，全量构建无回归面。`bash docs/audits/nop-compliance-checker.sh` 同理不设（零生产代码/daoFor/import 变更）。

- [x] 范围内行为完成：`clock-test-infrastructure.md` 6 节落盘且 Review Record 收敛
- [x] 相关文档对齐：文档引用 Q0 README + bug doc（无双真相源）；与 roadmap §MQ Q6 一致
- [x] 无验证命令门控（纯文档计划，原因如上）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查（本计划本身）已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中
- [x] `docs/logs/{year}/{month}-{day}.md` 追加本计划日志条目（计划级结束步骤）

## Deferred But Adjudicated

### Q6 Phase 2 实现（CoreMetrics 改造 / 测试侧迁移 / 15 域子类迁移）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: MQ 文档先行工作流强制要求 Phase 1 设计文档审查收敛后方可编写 Phase 2 实现 plan。本计划仅交付设计文档。Phase 2 是独立结果表面的后续 plan。
- Successor Required: yes —— 触发条件：本计划 done（设计文档审查收敛）+ 技术选型 Decision 落定。届时 DRAFT_PLANS 起草 Phase 2 实现 plan（加载 `nop-testing` skill），plan 引用本文档作为范围与验收依据。

### 月初时钟滚动 CI 矩阵的实际接线

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本设计文档仅裁决是否纳入 CI 矩阵；实际接线（若裁决纳入）属 Phase 2。
- Successor Required: yes —— 随 Q6 Phase 2 plan 一并处理。

## Closure

Status Note: 本计划为纯文档 Phase 1 设计文档（零代码/ORM/view/CI 变更）。Phase 1（6 节设计文档落盘）+ Phase 2（独立子代理审查循环至收敛）全部完成。设计文档 `docs/architecture/quality-engineering/clock-test-infrastructure.md` 经 3 轮独立子代理审查（R1 规范合规 `ses_04475edf...` / R2 覆盖面-可执行性 `ses_04475ab7...` / R3 收敛复核 `ses_0446e86d...`，三会话 task id 不同均 fresh cold context）收敛，无残留 BLOCKER/MAJOR。技术选型裁决=路径 C（应用层 thread-local delegating clock），Decision 项 rule-9 合规（候选 + 替代 + 残留风险）。独立结束审计 PASS。Q6 Phase 1 文档先行工作流交付完成，转 done；Phase 2 实现 plan 为 successor。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理，新会话 fresh cold context（task `ses_0446a9db1ffenVJxXzrHrfQIp6`，general 类型，未复用执行者上下文）
- Evidence: **PASS**（closure audit 判据逐条核验）——
  1. **范围内行为完成**：`clock-test-infrastructure.md` 含 §1-§7 + `## Review Record`，6 必需节（现状评估/目标与非目标/技术选型/实施步骤/验收判据/CI 门控设计）齐备；Review Record 3 轮（3 个不同 task id）收敛结论声明无残留 BLOCKER/MAJOR。
  2. **相关文档对齐**：文档 header 列 Q0 README + bug doc + R6.9 plan + Q0 plan 为「上游真相源（只引用）」，§1 显式「引用（非重推导）」，无双真相源漂移。
  3. **无验证命令门控（doc-only，justified）**：plan line 90 + Closure Gates note 明示零 Java/ORM/CI 变更面，doc-only 计划按 plan authoring guide 正确省略 mvn 门控。
  4. **无范围内项目降级**：Phase 2 实现按 MQ 文档先行工作流显式 out-of-scope（Non-Goals + Deferred with successor trigger）；范围内 items（设计文档 + 审查）全部执行。
  5. **独立草案审查**：`## Draft Review Record` 记 `ses_044845aa3ffetgwfoQ27C3maoO` accept，独立 cold session，live-evidence 主张独立复核 PASS。
  6. **文本一致性**：Phase 1（6 items [x] + Status completed）+ Phase 2（3 items [x] + Status completed）+ Exit Criteria + Closure Gates 全 [x]，Plan Status completed，内部一致。
  7. **独立结束审计**：本审计为 fresh cold session（未复用执行者上下文）。
  8. **结束证据**：本节。
  9. **docs/logs/ 条目**：`docs/logs/2026/08-01.md` 顶部追加 Q6 Phase 1 条目（reverse-chronological，含 6 节 + 3 轮审查 task id + 裁决路径 C + CI C-2 + doc-only + successor）。
  - **evidence spot-check（live 实仓复核）**：`AbstractFrozenClockExtension.java:63,68` registerClock ✓；15 子类 ✓；2 处 LocalDate.now 残留 ✓；assets 19 文件读 CoreMetrics.today ✓。
  - **design-only scope**：`git status --short` 仅 `.md`（设计文档 + plan）untracked，零 `.java/.xml/.yml` 源码变更。
  - （非阻塞观察：仓库根 `io/` 孤儿目录为无关 stray 构建产物，非本 plan 产生，建议作为 repo hygiene 清理。）

Follow-up:

- Q6 Phase 2 实现 plan（设计文档收敛后起草，加载 `nop-testing` skill，按路径 C 实施 `ThreadLocalFrozenClock` + 15 子类静态旁路迁移 + HR 调用点 + finance 4 类接线 + §6 CI 矩阵）。
