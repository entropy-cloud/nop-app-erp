# 2026-08-01-1121-3-mq-q5-performance-baseline-design-doc 性能基线与回归门控 Phase 1 设计文档

> Plan Status: active
> Last Reviewed: 2026-08-01
> Mission: audit-remediation
> Work Item: MQ Q5
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q5（line 678, 787）；`docs/architecture/quality-engineering/README.md` §实施顺序裁决（位 6，硬依赖 Q6 已满足）
> Related: `docs/plans/2026-08-01-1121-1-mq-q0-quality-gap-analysis-readme.md`（Q0 顺序基线，前置 done）；`docs/plans/2026-08-01-1357-1-mq-q6-clock-test-infrastructure-impl.md`（Q6 Phase 2，Q5 硬依赖——ThreadLocalFrozenClock 使性能计时确定性可复现）；`docs/plans/2026-08-01-1158-3-mq-q4-fault-injection-design-doc.md`（Q4，文档结构参照）
> Audit: required

## Current Baseline

> 本计划是 MQ 文档先行工作流的 **Phase 1**：产出审查收敛的设计文档 `docs/architecture/quality-engineering/performance-baseline.md`，**不改任何代码/ORM/CI**。MQ roadmap（line 843-862）与 Q0 README（line 20-22）明确：Phase 1 设计文档经独立子代理 ≥2 轮审查收敛后，方可编写 Phase 2 实现 plan。

**audit-remediation 主线状态**：M0 / MA1-MA7 / MR1-MR6 / MV / MG 全 done；MR6 milestone CLOSED。MQ 进行中：Q0/Q1/Q4/Q6 已 done，Q2/Q3/Q5/Q7 待办。验证基线 `mvn clean install -DskipTests` 156 模块全绿；`mvn test` 全绿。

**Q5 现状（NOT FOUND 证据，引用 Q0 README §范围矩阵 §Q5 + roadmap line 787 + 实仓复核，核验日期 2026-08-01）**：

- 全仓零性能测试基础设施：`rg "PerfTest|Benchmark|JMH|Gatling" --glob '*.java' --glob '*.xml'` 零命中（2026-08-01 复核确认，无 JMH / Gatling / 任何 `*PerfTest*` / `*Benchmark*` 类或配置）。
- 性能测试被显式 defer 到"首次生产数据规模"，但**无任何基础设施**承载该 defer。roadmap line 787 引用的 `db-index-design.md` **实仓 + archive 均未找到**（NOT FOUND，陈旧引用——本计划须在设计文档 §现状评估 标注此 stale reference 并以实存 owner doc 替代）；`posting.md`（过账链路真相源）实存。
- **Q6 硬依赖已满足**（roadmap line 891 + Q0 README line 62）：Q6 done —— `ThreadLocalFrozenClock`（`module-common-test`）+ 15 域子类迁移 + `TestClockRolloverFinance` 跨月模拟 + `clock-rollover.yml` nightly。性能基线的测量确定性（计时/期间数据不随墙钟漂移）现在可复现——这是 Q5 解除阻塞的硬前提。
- **关键路径候选**（roadmap line 787 + Q0 README line 59 + 实存 owner doc 对齐）：
  - **凭证过账**（`docs/design/finance/posting.md` + `posting-log.md`）：批量凭证过账吞吐量（roadmap line 787 建议 1000 凭证）；`ErpFinVoucherBillR` 已加 (billCode, businessType) 索引（R3.6），过账延迟随凭证累积应稳定。
  - **期间结账**（`docs/design/finance/period-close.md`）：大规模 GL 行结账（roadmap line 787 建议 1 万行）。
  - **库存核算 reclose**（`docs/design/finance/costing-methods.md`）：`ErpInvCostingBizModel.reclosePeriodCosts`（R6.9 已拆 Processor）重新核算成本层，数据量大时性能敏感。
  - **报表渲染**：nop-report 子系统 `renderHtml`，各域种子报表渲染耗时。
- **关键风险（roadmap line 787 明示）**：测量方法选型（JMH 微基准 vs 简单 `@Test` timing）+ CI 软门控设计（退化阈值如何定义以容忍 CI runner 硬件方差 + nightly vs per-commit）。Q6 时钟硬化解决了"计时确定性"，但 CI runner 硬件方差仍需门控设计裁决。

**剩余差距**：无 Q5 设计 owner doc。关键路径选择 + 基线定义（数据规模 × 时间阈值）+ 测量方法 + CI 软门控设计均未裁决，须在 Phase 1 文档中独立审查后定夺。

## Goals

- 产出 MQ Q5 的 Phase 1 设计文档 `docs/architecture/quality-engineering/performance-baseline.md`，经独立子代理 ≥2 轮审查收敛（第 1 轮规范合规 + 第 2 轮覆盖面/可执行性），覆盖 MQ 文档先行工作流要求的 6 节：
  1. **现状评估**（引用实仓证据：零性能基础设施 + db-index-design.md stale reference 标注 + Q6 解除阻塞 + 4 关键路径 owner doc 对齐）
  2. **目标与非目标**（建立 4 关键路径性能基线 + CI 软门控；不追求生产级压测）
  3. **技术选型**——JMH（微基准）vs 简单 `@Test` timing（集成级）vs 混合 + CI 软门控形态（退化阈值 + nightly vs per-commit）的替代评估与裁决理由
  4. **实施步骤**（基线测量 harness 落地 + 4 关键路径基线数据生成 + 首次基线落盘）
  5. **验收判据**（4 关键路径均有可复现基线 + CI 软门控在退化 > 阈值时告警 + 基线可随 Q6 时钟冻结复现）
  6. **CI 门控设计**（软门控 nightly 测量 + 基线对比 + 退化阈值容忍 CI runner 方差）
- 文档须显式声明测量方法裁决（JMH vs 简单 timing），这是 roadmap line 787 明示的关键决策。
- 文档须显式声明 CI runner 硬件方差下的退化阈值设计（绝对阈值 vs 相对退化比），避免假阳性/假阴性。

## Non-Goals

- **不实现任何代码/ORM/CI 变更**——本计划仅产出设计文档。Phase 2 实现（基线测量 harness + 4 关键路径基线数据 + CI 软门控）是**独立的后续 plan**，须在本设计文档审查收敛后方可起草（MQ 文档先行工作流）。
- 不追求生产级压测（无生产数据规模 + 无真实负载模拟）；仅建立 CI 可复现的回归基线。
- 不修改 `nop-entropy` 源码。
- 不重新推导 NOT FOUND 证据（引用 Q0 README，避免双真相源）。
- 不优化既有代码性能（性能基线是回归门控，不是性能优化任务）。
- 不编写 Q2/Q3 设计（同批独立 plan）。

## Task Route

- Type: `app-layer design change`（设计文档编写；纯文档，零代码）
- Owner Docs: `docs/architecture/quality-engineering/README.md`（Q0 顺序基线 + 文档先行工作流引用 + §Q5 硬依赖 Q6 裁决）；`docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q5 + §横切关注点 §文档先行工作流；`docs/design/finance/posting.md` + `posting-log.md`（凭证过账关键路径真相源）；`docs/design/finance/period-close.md`（期间结账关键路径真相源）；`docs/design/finance/costing-methods.md`（库存核算 reclose 关键路径真相源）；`docs/architecture/quality-engineering/clock-test-infrastructure.md`（Q6 时钟硬化产物，Q5 测量确定性依赖）
- Skill Selection Basis: AGENTS.md 强制技能扫描已完成——`nop-backend-dev`/`nop-frontend-dev`/`nop-testing`/`nop-debugging` 均不匹配"编写性能基线设计文档"。故 `Skill: none`（与 roadmap Q5 行 Skill 列 `none` + Q1/Q4 设计文档计划 Skill 列一致）。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. 本计划纯文档，不涉及端口/环境变量/CORS/密钥/.env/外部服务。

> 注：Phase 2 实现时，性能基线测量需要可复现的计时环境（Q6 ThreadLocalFrozenClock 已提供时钟确定性）+ CI runner 硬件方差的门控策略。此为 Phase 2 实施关注，本 Phase 1 仅在文档中评估测量方法与门控设计。

## Execution Plan

### Phase 1 - 编写 Q5 设计文档草稿

Status: planned
Targets: `docs/architecture/quality-engineering/performance-baseline.md`（新建）
Skill: none

- Item Types: `Add | Decision`
- Prereqs: Q0 done（已满足）；Q6 done（已满足——ThreadLocalFrozenClock 使测量确定性可复现，Q5 硬依赖解除）；Q0 README §实施顺序裁决落盘（已满足，Q5 位 6）

- [ ] Add: 创建 `performance-baseline.md`，含 MQ 文档先行工作流要求的 6 节骨架
      - Skill: none
- [ ] Add: §现状评估 —— 引用（非重推导）Q0 README §Q5 + roadmap line 787 + 本计划 Current Baseline 实仓复核：零性能基础设施（核验命令零命中）、`db-index-design.md` stale reference 标注（NOT FOUND，以实存 owner doc 替代）、Q6 解除阻塞（ThreadLocalFrozenClock 使计时可复现）、4 关键路径 owner doc 对齐。标注可复现核验命令 + 核验日期。
      - Skill: none
- [ ] Decision: §测量方法选型 —— 评估并裁决性能基线测量方法（roadmap line 787 明示关键决策）：
      - 路径 A：JMH 微基准（科学但重——需独立 harness，适合纯计算密集路径如成本核算）
      - 路径 B：简单 `@Test` timing（集成级，复用现有 `JunitAutoTestCase` + localDb + Q6 时钟冻结，适合端到端路径如凭证过账/期间结账/报表渲染）
      - 路径 C：混合——端到端路径用 `@Test` timing，纯计算路径用 JMH
      - 记录候选 + 考虑的替代 + 残留风险（JMH CI 集成复杂度 / `@Test` timing GC/JIT 噪声 / 混合两套 harness 维护成本）
      - Skill: none
- [ ] Decision: §CI 软门控设计 —— 裁决退化阈值形态（roadmap line 787 明示 CI runner 硬件方差风险）：
      - 路径 A：绝对阈值（基线 × N，简单但 CI runner 升级/降级致假阳性/假阴性）
      - 路径 B：相对退化比（与上次基线对比，退化 > X% 告警，容忍 runner 方差但需历史基线存储）
      - 路径 C：nightly 测量 + 趋势记录（非阻塞告警，积累数据后转阻塞门控）
      - 记录候选 + 考虑的替代 + 残留风险（绝对阈值硬件敏感性 / 相对退化比首基线冷启动 / nightly 延迟发现）
      - Skill: none
- [ ] Add: §关键路径选择与基线定义 —— 4 关键路径（凭证过账 1000 凭证 / 期间结账 1 万行 / 库存核算 reclose / 报表渲染）各定义数据规模 × 时间阈值，引用 owner doc 确认路径真实性。须裁决每路径的 seed 数据生成策略。
      - Skill: none
- [ ] Add: §实施步骤 —— 基线测量 harness 落地（`module-common-test` 或独立 perf module）+ 4 关键路径基线数据生成 + 首次基线落盘（基线数据载体裁决：独立文件 vs 复用）
      - Skill: none
- [ ] Add: §验收判据 —— 4 关键路径均有可复现基线（Q6 时钟冻结下多轮测量方差 < 阈值）+ CI 软门控在退化 > 阈值时告警 + 基线数据落盘可追溯
      - Skill: none
- [ ] Add: §CI 门控设计 —— nightly 测量 job + 基线对比 + 退化阈值 + 基线数据存储 + 与现有 5 CI job（maven/compliance/e2e/mutation/clock-rollover）不冲突
      - Skill: none
- [ ] Add: §与 Q6 的依赖确认 —— 显式声明 Q6 ThreadLocalFrozenClock 是 Q5 测量确定性的硬前提（已满足），性能计时/期间数据不随墙钟漂移
      - Skill: none

Exit Criteria:

> 本计划纯文档，零代码/ORM/CI 变更。完整仓库 `typecheck`/`build`/`test` 不适用（按 plan authoring guide，无代码更改的计划删除验证命令门控）。

- [ ] `performance-baseline.md` 落盘，含上述 6 节 + 测量方法裁决 + CI 软门控设计 + Q6 依赖确认，两个 Decision 记录候选+替代+残留风险三要素
- [ ] §现状评估每条证据标注可复现核验命令 + 核验日期 + db-index-design.md stale reference 标注

### Phase 2 - 独立子代理设计文档审查循环（≥2 轮至收敛）

Status: planned
Targets: `docs/architecture/quality-engineering/performance-baseline.md`（`## Review Record` 节）
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 1 草稿落盘

- [ ] Proof: 第 1 轮审查——**规范合规审查**，由独立子代理（新会话）执行。审查项：6 节结构完整性 / 与项目约定一致性 / 反模式检查（无双真相源、关键路径是否引用实存 owner doc 而非 stale db-index-design.md、Q6 依赖是否真实满足而非假设）/ owner doc 引用正确性（posting.md / period-close.md / costing-methods.md / clock-test-infrastructure.md）。输出 BLOCKER/MAJOR/MINOR。
      - Skill: none
- [ ] Proof: 第 2 轮审查——**覆盖面与可执行性审查**，由**另一个**独立子代理（不同 task id，新会话）执行。审查项：JMH/@Test/混合三测量路径是否充分评估 / 绝对/相对/nightly 三门控路径是否可执行且容忍 CI runner 方差 / 4 关键路径数据规模是否合理（1000 凭证 / 1 万行是否过重或过轻）/ seed 数据生成策略是否可落地 / Q6 时钟冻结是否真的支撑可复现计时（多轮测量方差验证）/ 与现有 5 CI job 是否冲突 / 基线数据存储载体裁决。输出 BLOCKER/MAJOR/MINOR。
      - Skill: none
- [ ] Add: 作者据审查意见修订文档并重审，直至两轮均无 BLOCKER/MAJOR；`## Review Record` 节持久化两轮审查者 task id + 轮次 + 结论 + 修改摘要
      - Skill: none

Exit Criteria:

- [ ] §Review Record 记录 ≥2 轮审查，两轮由不同子代理会话执行，无残留 BLOCKER/MAJOR
- [ ] 测量方法裁决 + CI 软门控设计经审查后可执行（或据审查修订后可执行）

## Draft Review Record

- Independent draft review iteration 1: **accept-as-is**（`ses_04220cbf8ffe8Ud72UR6V227oi`，独立子代理 fresh session cold context）— 0 BLOCKER / 0 MAJOR / 1 MINOR。M1（roadmap 行号引证 off-by-one：Q5 detail 在 line 787 而非 788，788 实为 Q6——已全文修正 787-788→787 + roadmap line 788→787）。MINOR 已修订。Baseline 核验全 PASS（PerfTest/Benchmark/JMH/Gatling 零命中确认；db-index-design.md stale reference NOT FOUND 确认[repo + archive 均无]；4 关键路径 owner doc 存在性确认；Q6 done + ThreadLocalFrozenClock 硬依赖满足确认；clock-test-infrastructure.md 存在确认）。CI runner 硬件方差风险在 §CI 软门控 Decision 三路径覆盖。Phase-1 doc-only 范围保持。converged → 转 active。

## Closure Gates

> 本计划无代码/ORM/view/CI 变更（纯设计文档）。按 plan authoring guide §Closure Gates："对于无代码更改的计划（仅文档），删除验证命令门控并说明原因"——故不设 `mvn typecheck/build/test` 门控，原因：零 Java/ORM/CI 变更，全量构建无回归面。

- [ ] 范围内行为完成：`performance-baseline.md` 6 节 + 测量方法裁决 + CI 软门控设计 + Q6 依赖确认落盘且 Review Record 收敛
- [ ] 相关文档对齐：文档引用 Q0 README（无双真相源）；与 roadmap §MQ Q5 + Q6 依赖图 + 各 owner doc 关键路径定义一致
- [ ] 无验证命令门控（纯文档计划，原因如上）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查（本计划本身）已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中
- [ ] `docs/logs/{year}/{month}-{day}.md` 追加本计划日志条目（计划级结束步骤）

## Deferred But Adjudicated

### Q5 Phase 2 实现（基线测量 harness + 4 关键路径基线数据 + CI 软门控）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: MQ 文档先行工作流强制要求 Phase 1 设计文档审查收敛后方可编写 Phase 2 实现 plan。本计划仅交付设计文档。
- Successor Required: yes —— 触发条件：本计划 done（设计文档审查收敛）+ 测量方法 + CI 门控 Decision 落定。届时 DRAFT_PLANS 起草 Phase 2 实现 plan（加载 `nop-testing` skill），plan 引用本文档作为范围与验收依据。

### 生产级压测

- Classification: `optimization candidate`
- Why Not Blocking Closure: Q5 仅建立 CI 可复现的回归基线，不追求生产数据规模压测。
- Successor Required: yes —— 触发条件：首次生产部署 + 真实负载数据可用时，作为独立性能验证工作项。

## Closure

Status Note: （计划完成时填写）

Closure Audit Evidence:

- Auditor / Agent: （独立结束审计子代理，新会话 fresh cold context）
- Evidence: （完成时填写）

Follow-up:

- Q5 Phase 2 实现 plan（设计文档收敛后起草）。
