# 2026-08-02-0650-2 MQ Q5 性能基线方差稳定化（路径 C JMH 升级 + 逐路径根因裁决）

> Plan Status: active
> Last Reviewed: 2026-08-02
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q5（line 678）+ Q5 设计文档 §3.4 路径 C successor hook（`performance-baseline.md` line 133/141/176）+ Q5 Phase 2 plan Phase 4 Decision「登记 successor…触发条件满足…开独立 JMH 升级计划时引用本基线作触发证据」
> Related: `docs/plans/2026-08-02-1121-2-mq-q5-performance-baseline-impl.md`（Q5 Phase 2 首基线，Plan Status: completed，独立结束审计 PASS）；`docs/plans/2026-08-01-1121-3-mq-q5-performance-baseline-design-doc.md`（Phase 1 设计文档）
> Audit: required

## Current Baseline

> 本计划是 Q5 性能基线的**后继稳定化**，非新质量维度。Q5 设计文档（`performance-baseline.md`）经 2 轮独立审查收敛，裁决路径 B（`@Test` timing）首选 + 路径 C（JMH 混合）作**显式 successor hook**（§3.4 line 120/133），触发条件 =「Phase 2 首基线显示某路径 `@Test` timing 方差比超 §4 阈值」。Q5 Phase 2 已实测**全部 4 路径方差超阈值**并登记为路径 C 升级候选，触发条件已满足。

**Q5 Phase 2 已落地（2026-08-02 复核实仓）：**
- `PerfTiming` harness（`module-common-test`，K=2 untimed warmup + N=10 timed + 方差比=(max−min)/median + 中位数 + p95）。
- 4 perf 测试类：`TestErpFinVoucherPostingPerf` / `TestErpFinPeriodClosePerf` / `TestErpInvCostingReclosePerf` / `TestErpFinReportRenderPerf`。
- `@Tag("perf")` + `erp-fin-service`/`erp-inv-service` pom `<excludedGroups>perf</excludedGroups>` + perf profile（surefire 3.x CLI 不覆盖 pom 硬编码，须 profile）。
- 基线落盘 `docs/architecture/quality-engineering/perf-baselines/baseline-2026-08-02.json` + `LATEST.json` 指针（regressionDetectionPolicy: relative-median-diff 20% / minimumNightlySamples 14 / currentPhase C-1-non-blocking）。
- `.github/workflows/perf-baseline.yml` nightly `0 5`（跨计划 cron 协调：sibling Q2 `security.yml` 占 02:00）+ `workflow_dispatch`，首期非阻塞趋势记录。

**全部 4 路径方差超阈值（Q5 Phase 2 实测，触发证据）：**

| 路径 | 数据规模 | median | varianceRatio | 阈值 | 根因（Q5 plan 记录） |
|------|---------|--------|---------------|------|---------------------|
| 1 凭证过账 | 1000 凭证 | 23000ms | **165.9%** | <15% | H2 dataset growth（每轮追加 1000 凭证→后续轮扫更大表）+ heap 填充 GC pause |
| 2 期间结账 | 1万 GL 行 | 32.4ms | **43.7%** | <20% | close 快（~30ms）→ sub-50ms 计时被 JIT/GC/scheduling jitter 主导；voucher 跨轮累积 |
| 3 库存核算 reclose | 5000 移动单×50 物料 | 610.9ms | **117.2%** | <20% | reclose 每轮 500×~4 H2 查询≈2000 查询/轮；H2 in-memory 在 heap 压力下查询时间方差大 |
| 4 报表渲染 | 8 报表 | 0.33-1.85ms | **32-110%** | <15% | 亚毫秒级渲染→(max−min)/median 放大极小绝对抖动；绝对计时足够稳定支持 >5x 退化检测 |

**根因分两类（本计划逐路径裁决的基础）：**
1. **跨轮状态累积 / dataset growth**（路径 1、3）：每轮 timed 测量追加或重算数据→后续轮扫更大表/更多层。`@Test` timing 在 localDb 无轮间隔离。**候选修复**：JMH fork 隔离（每 fork fresh state）vs per-round state reset（计时窗口外回滚每轮写入使每轮扫同规模）。
2. **亚毫秒 / sub-50ms 计时**（路径 2、4）：绝对抖动（GC/JIT/scheduling）在极小绝对耗时上放大成大相对方差比。**候选修复**：增大数据规模逃离 sub-ms 区间（使绝对耗时进入 ms+ 区，相对方差自然下降）vs 改用绝对容差退化检测（设计文档 §4 复现性阈值原以方差比度量，路径 4 实测显示绝对计时已足够稳定支持 >5x 检测）。

**设计文档 §3 对 JMH 的既定立场（须在本计划裁决中尊重）：** §3.4 line 125「JMH fork 隔离在每次 fork 重建 DB 连接/warmup，反而放大 DB 冷启动噪声」（对 DB-bound 端到端路径）+ line 176「reclose 偏计算，是路径 C 升级（JMH）最可能候选」。故 JMH **非通用解**——本计划须逐路径裁决 JMH 是否实际降低方差，若不降低则采用替代稳定化（state reset / scale increase / metric change）并记录裁决理由。

**剩余差距：** 4 路径方差全部超阈值，当前基线对 per-path 阻塞门控不可用（165% 方差下无法区分真实退化与噪声）；nightly 趋势记录已接线但退化检测灵敏度低。successor 触发条件已满足，须逐路径稳定化使方差降至阈值内或裁决替代度量。

## Goals

- **逐路径根因裁决（Decision-heavy）**：对 4 路径各裁决最有效的稳定化策略——JMH fork 隔离（路径 3 reclose 首选候选）/ per-round state reset（路径 1、3 dataset growth）/ 数据规模上调（路径 2、4 sub-ms）/ 绝对容差度量（路径 4）。每路径记录选择 + 考虑的替代 + 残留风险。
- **路径 3 reclose JMH 升级（设计文档 §4.3 最可能候选）**：引入 JMH harness（`@Benchmark` + fork 隔离 + warmup）测 reclose 纯计算/DB-heavy 混合路径；实测方差比是否降至 <20% 阈值。若 JMH 因 DB 冷启动噪声反效果，裁决 fallback（per-round cost-layer reset）并记录。
- **路径 1、3 dataset-growth 稳定化**：实现 per-round state reset（计时窗口外回滚每轮追加/重算写入，使每轮扫同规模数据集），实测方差比是否降至阈值。
- **路径 2、4 sub-ms 稳定化**：数据规模上调（路径 2 增大 GL 行数 / 路径 4 增大报表复杂度或渲染次数）使绝对耗时逃离 sub-ms/sub-50ms 区间；或裁决改用绝对容差退化检测（保留方差比作记录，门控用绝对退化比）。
- **重测基线 + 门控策略更新**：稳定化后重测 4 路径方差比；更新 `perf-baselines/baseline-{date}.json` + `LATEST.json`；若某路径经裁决采用绝对容差度量，更新 `regressionDetectionPolicy` + 设计文档 §4 复现性阈值注记。

## Non-Goals

- **不优化既有代码性能**（设计文档 §2.2：基线是回归门控，非性能优化任务；本计划降低测量方差，不降低被测路径的绝对耗时）。
- **不修改 nop-entropy 源码**（JMH harness 在应用层 test scope；设计文档 §6.4 边界）。
- **不做 per-commit 阻塞门控晋升**（设计文档 §6.2：晋升阻塞门控须 ≥30 nightly + runner 同构 + 团队同意；本计划交付稳定化后的可用基线，阻塞晋升是 successor）。
- **不引入生产级压测**（设计文档 §2.2：仍测 H2 localDb 端到端成本；生产级压测是独立 successor）。
- **不新增关键路径**（设计文档 §4 已锁 4 路径；本计划稳定化既有 4 路径）。
- **不重做 Phase 1 文档先行循环**（设计文档 §3/§4/§6 已裁决路径 C successor hook + 触发条件 + 度量定义；本计划在既定契约下执行稳定化，实施期裁决回填设计文档 Review Record）。

## Task Route

- Type: `implementation-only change`（在已裁决的 Q5 设计契约下稳定化测量方差，不改业务契约/模型；JMH harness 是 test scope 基础设施）
- Owner Docs: `docs/architecture/quality-engineering/performance-baseline.md`（§3.4 路径 C successor + §4 复现性阈值 + §5 harness + §6 门控）+ `perf-baselines/baseline-2026-08-02.json`（触发证据）
- Skill Selection Basis: `nop-testing`（perf 测试 + JMH harness 编写 + 与既有 `@Tag("perf")` 隔离协同）；本计划引入 JMH 是 Q5 设计文档既定 successor，非新维度。

## Infrastructure And Config Prereqs

- JMH 引入须 Maven 插件配置（`org.openjdk.jmh:jmh-core` + `jmh-generator-annprocess` test scope + surefire/独立 runner）。接入位置裁决在 Phase 1（per-module profile vs 根 pom profile，对齐 Q1 pitest profile 范式 + Q5 既有 perf profile）。
- 无外部服务依赖（H2 localDb 既有）；无密钥/端口/CORS 新增。

## Execution Plan

### Phase 1 - 逐路径根因裁决 + JMH harness 接入位置 Decision

Status: planned
Targets: 计划内裁决记录 + `module-common-test`（JMH harness 若裁决引入）
Skill: `nop-testing`

- Item Types: `Decision | Add`
- Prereqs: Q5 Phase 2 基线（触发证据已落盘）

- [ ] Decision: 逐路径稳定化策略裁决——对 4 路径各记录根因类别（dataset-growth vs sub-ms）+ 候选（JMH fork / state reset / scale increase / absolute-tolerance metric）+ 选择 + 替代 + 残留风险。裁决须尊重设计文档 §3.4 line 125（JMH 对 DB-bound 端到端反效果）+ line 176（reclose 最可能 JMH 候选）
  - Skill: none
- [ ] Decision: JMH 接入位置——per-module profile（erp-inv-service reclose）vs 根 pom profile。裁决理由（对齐 Q1 pitest per-module profile 先例 + Q5 既有 perf profile；JMH 是 test scope 不污染生产 classpath）
  - Skill: none
- [ ] Add（若裁决引入 JMH）：JMH 依赖（`jmh-core` + `jmh-generator-annprocess` test scope）+ surefire/runner 配置，复用 `module-common-test` 或独立 JMH harness 类
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 4 路径逐路径裁决记录落盘（每路径根因 + 选择的稳定化策略 + 理由；记录位置 = 计划 Phase 1 body + design doc `performance-baseline.md` Review Record + `perf-baselines/baseline-{date}.json` notes 段，保证可追溯）；JMH 接入位置裁决落定

### Phase 2 - 路径 3 reclose + 路径 1 凭证过账 dataset-growth 稳定化

Status: planned
Targets: `TestErpInvCostingReclosePerf`（路径 3）/ `TestErpFinVoucherPostingPerf`（路径 1）稳定化改造
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 裁决

- [ ] Add: 路径 3 reclose 稳定化——按 Phase 1 裁决实施（JMH fork 隔离 OR per-round cost-layer reset 使每轮扫同规模）；保持计时窗口纪律（seed-gen/state-reset 排除在计时窗口外，设计文档 §4）
  - Skill: `nop-testing`
- [ ] Add: 路径 1 凭证过账稳定化——per-round state reset（每轮 timed 后回滚追加的 1000 凭证，使每轮扫同规模表）OR JMH fork（若 Phase 1 裁决路径 1 亦用 JMH）
  - Skill: `nop-testing`
- [ ] Proof: 重测路径 1、3 方差比——路径 1 <15%、路径 3 <20%（设计文档 §4 阈值）；若仍超阈值，记录残留方差根因 + 裁决 fallback（如路径 1 接受绝对退化检测，或 per-round state reset 替代 JMH——此 fallback 在本相内闭环，不另开 successor）。重测基线追加 `perf-baselines/baseline-{date}.json`
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 路径 1、3 重测方差比降至阈值 OR 裁决 fallback 并记录残留理由（不可静默接受超阈值而不记录）

### Phase 3 - 路径 2 期间结账 + 路径 4 报表渲染 sub-ms 稳定化

Status: planned
Targets: `TestErpFinPeriodClosePerf`（路径 2）/ `TestErpFinReportRenderPerf`（路径 4）稳定化改造
Skill: `nop-testing`

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 1 裁决

- [ ] Add: 路径 2 期间结账稳定化——数据规模上调（GL 行数 1万→N 使 close 绝对耗时逃离 sub-50ms）OR per-round voucher reset（消除跨轮累积 bias）
  - Skill: `nop-testing`
- [ ] Decision: 路径 4 报表渲染度量裁决——亚毫秒级渲染的 (max−min)/median 方差比度量本质放大绝对抖动；裁决是否改用绝对容差退化检测（保留方差比记录，门控用绝对退化比 / >5x 检测）。裁决须更新设计文档 §4 复现性阈值注记 + `LATEST.json` regressionDetectionPolicy
  - Skill: none
- [ ] Add（若路径 4 裁决保留方差比度量）：路径 4 数据规模上调（渲染次数 / 报表复杂度上调使绝对耗时进入 ms+ 区）
  - Skill: `nop-testing`
- [ ] Proof: 重测路径 2、4 方差比（或绝对容差度量）——达阈值 OR 裁决替代度量并记录。重测基线追加
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 路径 2、4 重测达阈值（方差比或裁决的替代度量）OR 裁决替代度量并记录理由

### Phase 4 - 基线重测落盘 + 设计文档/门控策略更新

Status: planned
Targets: `perf-baselines/baseline-{date}.json` + `LATEST.json`；`performance-baseline.md`；`.github/workflows/perf-baseline.yml`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 2-3

- [ ] Add: 4 路径稳定化后基线重测落盘 `perf-baselines/baseline-{date}.json` + `LATEST.json` 指针更新（每路径稳定化策略 + 重测方差比/度量 + 残留风险）
  - Skill: none
- [ ] Add: 设计文档 `performance-baseline.md` 回填——§3.4 路径 C 升级结果（哪些路径用 JMH / state reset / scale / absolute-tolerance）+ §4 复现性阈值注记（路径 4 若改绝对容差）+ Review Record 实施期裁决
  - Skill: none
- [ ] Proof: `perf-baseline.yml` nightly 配置与稳定化后度量一致（nightly 跑 4 路径用稳定化后配置 + 退化比计算用更新后度量）；本地化核验（完整仓库 `mvn clean install -DskipTests` + `mvn test` 属 Closure Gates，不在此重复）
  - Skill: none

Exit Criteria:

- [ ] 4 路径稳定化后基线落盘 + LATEST 指针更新；设计文档回填；nightly 配置一致

## Draft Review Record

- Independent draft review iteration 1: **acceptable-as-is** (`ses_04044a115ffeq3SUYI5h1XkUV3`) — 0 BLOCKER / 0 MAJOR / 4 MINOR。MINOR：M1 Related header Q5 前驱状态 stale（实测 Q5 已 closure-audited 非「待 closure」）/ M2 Phase 4 Proof 重复全量验证（属 Closure Gates）/ M3 Phase 1 裁决记录位置未指定 / M4 Follow-up 含范围内 contingency（JMH fallback 属 Phase 2 内闭环）。作者据 4 MINOR 修订（非阻塞，但提升一致性）：M1 纠正前驱状态为 completed（closure-audited）/ M2 Phase 4 Proof 改本地化核验 / M3 Phase 1 Exit Criteria 指定裁决记录位置 / M4 JMH fallback 移入 Phase 2 Proof。共识达成，可转 active。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [ ] 范围内行为完成（4 路径逐路径稳定化 + 重测基线，方差降至阈值或裁决替代度量并记录）
- [ ] 相关文档对齐（`performance-baseline.md` §3.4/§4 回填 + Review Record；`LATEST.json` regressionDetectionPolicy 若路径 4 改绝对容差）
- [ ] 已运行验证：`mvn clean install -DskipTests` 156 模块 BUILD SUCCESS；`mvn test` 0 failures/0 errors（perf 被 excluded；JMH 依赖 test scope 不影响生产 classpath）；稳定化后 4 路径重测方差比/度量达阈值或裁决记录
- [ ] 无范围内项目降级为 deferred/follow-up（残留方差须显式裁决 fallback 并记录，不可静默接受超阈值）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### per-commit 阻塞门控晋升

- Classification: `optimization candidate`
- Why Not Blocking Closure: 设计文档 §6.2：晋升阻塞门控须 ≥30 nightly + runner 同构确认 + 团队同意。本计划交付稳定化后的可用基线（方差达阈值），阻塞晋升是后续团队决策。
- Successor Required: yes — 触发条件：nightly 累积 ≥30 稳定测量 + runner 同构确认 + 团队明确同意。

### 生产级压测

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 设计文档 §2.2 明确非目标（仅 CI 可复现回归基线，测 H2 localDb 非生产真实成本）。
- Successor Required: yes — 触发条件：首次生产部署 + 真实负载数据可用。

### 独立 perf module

- Classification: `optimization candidate`
- Why Not Blocking Closure: 设计文档 §5.1 裁决复用 `module-common-test` + 各域 test 源码；独立模块 successor 仅当规模膨胀。
- Successor Required: yes — 触发条件：perf 测试规模膨胀（>10 域 / >20 测试类）。

## Closure

Status Note: <关闭时填写——为何计划可关闭>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- per-commit 阻塞门控晋升（须 ≥30 nightly + 团队同意）
- 生产级压测（须首次生产部署）
