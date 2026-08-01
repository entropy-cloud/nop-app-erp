# 2026-08-01-1158-2-mq-q1-mutation-testing-design-doc 变异测试有效性 Phase 1 设计文档

> Plan Status: completed
> Last Reviewed: 2026-08-01
> Mission: audit-remediation
> Work Item: MQ Q1
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q1（line 674, 783）；`docs/architecture/quality-engineering/README.md` §实施顺序裁决（位 2）
> Related: `docs/plans/2026-08-01-1121-1-mq-q0-quality-gap-analysis-readme.md`（Q0 顺序基线，前置 done）；`docs/plans/2026-08-01-1158-3-mq-q4-fault-injection-design-doc.md`（Q4，Q1↔Q4 协同——Q1 盲区类即 Q4 优先覆盖路径，同批起草）
> Audit: required

## Current Baseline

> 本计划是 MQ 文档先行工作流的 **Phase 1**：产出审查收敛的设计文档 `docs/architecture/quality-engineering/mutation-testing.md`，**不改任何代码/ORM/CI**。MQ roadmap（line 843-862）与 Q0 README（line 20-22）明确：Phase 1 设计文档经独立子代理 ≥2 轮审查收敛后，方可编写 Phase 2 实现 plan。

**audit-remediation 主线状态**：M0 / MA1-MA7 / MR1-MR6 / MV / MG 全 done；MR6 milestone CLOSED。验证基线 `mvn clean install -DskipTests` 156 模块全绿；`mvn test` 1903 测试 0 failures。

**Q1 现状（NOT FOUND 证据，引用 Q0 README §范围矩阵 §Q1，核验日期 2026-08-01）**：

- 全仓零 pitest 配置：`rg "pitest" --glob '*.xml'` 零命中。
- 审计文档 MA5（A5.1-A5.4）反复写"需 pitest 运行"但**从未执行** → 1903 个测试的 mutation score **完全未知**。
- 测试/ mutation 比历史数据（MA5 审计快照，非实时）：finance 64 测试 / 137 mutation（比 0.47）、mfg 30/74（0.41）、hr 15/92（0.16 全域最低）、assets 14/61（0.23）—— 指示 finance/mfg 为高复杂度高优先级目标域，但比例未经 pitest 实测验证。
- 关键约束：Nop 代码生成产物在 `_gen/` 包下，pitest 须配置 `excludedClasses`/`targetClasses` 排除 `*_gen` 包，否则存活变异体被生成代码噪声主导（roadmap line 783）。
- 与 Q4 协同：Q1 发现的盲区类正是 Q4 应优先覆盖的可恢复性路径（roadmap line 786，Q0 README §实施顺序裁决 line 152）。

**剩余差距**：无 Q1 设计 owner doc。pitest 插件配置范式、目标域选择策略、mutation score 基线定义、CI 门控形态（per-commit vs nightly）、存活变异体分类工作流均未裁决，须在 Phase 1 文档中独立审查后定夺。

## Goals

- 产出 MQ Q1 的 Phase 1 设计文档 `docs/architecture/quality-engineering/mutation-testing.md`，经独立子代理 ≥2 轮审查收敛（第 1 轮规范合规 + 第 2 轮覆盖面/可执行性），覆盖 MQ 文档先行工作流要求的 6 节：
  1. **现状评估**（引用实仓证据：零 pitest + MA5 测试/mutation 比历史 + `_gen` 包约束）
  2. **目标与非目标**（首轮聚焦 finance/mfg/inv 高复杂度域；不追求全域首跑即覆盖）
  3. **技术选型**——pitest（默认）vs 其他变异测试工具的替代评估 + 插件配置范式（`targetClasses`/`excludedClasses` 排除 `_gen`）+ 变异算子集选择
  4. **实施步骤**（插件接入 + 首跑基线 + 存活变异体分析）
  5. **验收判据**（三目标域 mutation score 基线落盘 + 存活变异体分类清单 + `_gen` 噪声排除验证）
  6. **CI 门控设计**——per-commit 全量不现实（耗时），裁决 nightly 调度 + 软门控（score 退化阈值）vs per-commit 增量
- 审查记录持久化在文档 `## Review Record` 节。
- 文档须显式声明与 Q4 的协同接口（Q1 输出的盲区类清单格式，供 Q4 消费）。

## Non-Goals

- **不实现任何代码/ORM/CI 变更**——本计划仅产出设计文档。Phase 2 实现（pitest 插件接入 + 首跑）是**独立的后续 plan**，须在本设计文档审查收敛后方可起草（MQ 文档先行工作流）。
- 不首跑 pitest（首跑属 Phase 2，会产生大量首次发现，须先有分类工作流）。
- 不覆盖全域首跑（Q1 首轮聚焦 finance/mfg/inv 三域；其余域 successor）。
- 不重新推导 NOT FOUND 证据（引用 Q0 README，避免双真相源）。
- 不编写 Q4 故障注入设计（同批独立 plan）。

## Task Route

- Type: `app-layer design change`（设计文档编写；纯文档，零代码）
- Owner Docs: `docs/architecture/quality-engineering/README.md`（Q0 顺序基线 + 文档先行工作流引用）；`docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q1 + §横切关注点 §文档先行工作流
- Skill Selection Basis: AGENTS.md 强制技能扫描已完成——`nop-backend-dev`/`nop-frontend-dev`/`nop-testing`/`nop-debugging` 均不匹配"编写变异测试策略设计文档"。`nop-testing`（JunitAutoTestCase/快照）留待 Phase 2。故 `Skill: none`（与 roadmap Q1 行 Skill 列 + Q0 README 一致）。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. 本计划纯文档，不涉及端口/环境变量/CORS/密钥/.env/外部服务。（Phase 2 实现才涉及 pitest 插件 + JVM agent 依赖，本计划不接入。）

## Execution Plan

### Phase 1 - 编写 Q1 设计文档草稿

Status: completed
Targets: `docs/architecture/quality-engineering/mutation-testing.md`（新建）
Skill: none

- Item Types: `Add | Decision`
- Prereqs: Q0 done（已满足）；Q0 README §实施顺序裁决落盘（已满足）

- [x] Add: 创建 `mutation-testing.md`，含 MQ 文档先行工作流要求的 6 节骨架
      - Skill: none
- [x] Add: §现状评估 —— 引用（非重推导）Q0 README §Q1 + MA5 审计：零 pitest 配置、1903 测试 mutation score 未知、finance/mfg 测试/mutation 比历史、`_gen` 包约束。标注可复现核验命令（`rg "pitest" --glob '*.xml'`）+ 核验日期。
      - Skill: none
- [x] Decision: §技术选型 —— 评估并裁决：
      - 工具：pitest（默认，开源 Maven 插件）vs 其他（如 Jumble）—— 给出选 pitest 的理由
      - 变异算子集：默认全集 vs 收缩子集（首跑降噪）
      - 插件配置范式：`targetClasses`/`excludedClasses` 如何排除 `_gen` 包 + 如何限定首跑到 finance/mfg/inv 三域（profile/per-module 策略）
      - 记录候选 + 考虑的替代 + 残留风险（如生成代码噪声、首跑耗时长）
      - Skill: none
- [x] Add: §实施步骤 —— 插件接入位置（哪些 module 的 pom）+ 首跑基线流程 + 存活变异体分析工作流
      - Skill: none
- [x] Add: §验收判据 —— 三目标域 mutation score 基线落盘 + 存活变异体分类清单（真实盲区 vs `_gen` 噪声 vs 等价变异）+ `_gen` 排除验证（目标域非 `_gen` 类被覆盖）
      - Skill: none
- [x] Add: §CI 门控设计 —— 裁决 nightly 调度 + 软门控（score 退化阈值，对齐 compliance-baseline 单向收紧模式）vs per-commit 增量；给出与 `.github/workflows/compliance.yml` 的集成方式（若纳入）
      - Skill: none
- [x] Add: §与 Q4 协同接口 —— 声明 Q1 输出的盲区类清单格式（哪些类的存活变异体指向可恢复性路径），供 Q4 Phase 2 优先覆盖消费
      - Skill: none

Exit Criteria:

> 本计划纯文档，零代码/ORM/CI 变更。完整仓库 `typecheck`/`build`/`test` 不适用（按 plan authoring guide，无代码更改的计划删除验证命令门控）。本 Phase 仅新建一个 .md 文件，零代码回归面，故不设 `mvn` 验证门控（与前置 Q0 plan 一致）。

- [x] `mutation-testing.md` 落盘，含上述 6 节 + Q4 协同接口，技术选型 Decision 记录候选+替代+残留风险三要素
- [x] §现状评估每条证据标注可复现核验命令 + 核验日期

### Phase 2 - 独立子代理设计文档审查循环（≥2 轮至收敛）

Status: completed
Targets: `docs/architecture/quality-engineering/mutation-testing.md`（`## Review Record` 节）
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 1 草稿落盘

- [x] Proof: 第 1 轮审查——**规范合规审查**，由独立子代理（新会话）执行。审查项：6 节结构完整性 / 与项目约定一致性 / 反模式检查（无双真相源、CI 门控是否误套 compliance 模式）/ owner doc 引用正确性。输出 BLOCKER/MAJOR/MINOR。
      - Skill: none
- [x] Proof: 第 2 轮审查——**覆盖面与可执行性审查**，由**另一个**独立子代理（不同 task id，新会话）执行。审查项：`_gen` 排除策略是否可执行 / 首跑耗时估算是否现实 / 三域选择理由是否充分（finance/mfg/inv vs hr 最低比 0.16）/ nightly 调度可行性 / Q4 协同接口是否可消费。输出 BLOCKER/MAJOR/MINOR。
      - Skill: none
- [x] Add: 作者据审查意见修订文档并重审，直至两轮均无 BLOCKER/MAJOR；`## Review Record` 节持久化两轮审查者 task id + 轮次 + 结论 + 修改摘要
      - Skill: none

Exit Criteria:

- [x] §Review Record 记录 ≥2 轮审查，两轮由不同子代理会话执行，无残留 BLOCKER/MAJOR
- [x] Q4 协同接口经审查后可消费（或据审查修订后可消费）

## Draft Review Record

- Independent draft review iteration 1: **acceptable-as-is**（`ses_0448437a8ffexujX6P5qfCp0uZ`，独立子代理 fresh session cold context）— 0 BLOCKER / 0 MAJOR / 1 MINOR（Phase 2 item-type `Proof` 含 1 `Add` 项，已修订为 `Proof | Add`）。实仓基线主张独立复核 PASS：`rg "pitest" --glob '*.xml'` rc=1 零命中；`quality-engineering/` 仅 README.md；`_gen` Java 包跨 10+ dao 模块确认存在（exclusion risk 真实）；Q4 sibling plan 存在。MA5 测试/mutation 比诚实标注"非实时"且算术正确（0.47/0.41/0.16/0.23），hr 0.16 延期理由充分并路由 Phase 2 独立审查。关键 `_gen` 排除风险与 nightly-vs-per-commit CI 决策均升为 Decision/section item；Q1↔Q4 协同接口三处声明 + 交叉引用正确。doc-only closure-gate 省略有正当理由。无 anti-slack 违规；所有 Deferred 项带 successor 触发条件。converged → 转 active。

## Closure Gates

> 本计划无代码/ORM/view/CI 变更（纯设计文档）。按 plan authoring guide §Closure Gates："对于无代码更改的计划（仅文档），删除验证命令门控并说明原因"——故不设 `mvn typecheck/build/test` 门控，原因：零 Java/ORM/CI 变更，全量构建无回归面。

- [x] 范围内行为完成：`mutation-testing.md` 6 节 + Q4 协同接口落盘且 Review Record 收敛
- [x] 相关文档对齐：文档引用 Q0 README（无双真相源）；与 roadmap §MQ Q1 一致
- [x] 无验证命令门控（纯文档计划，原因如上）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查（本计划本身）已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中
- [x] `docs/logs/{year}/{month}-{day}.md` 追加本计划日志条目（计划级结束步骤）

## Deferred But Adjudicated

### Q1 Phase 2 实现（pitest 插件接入 + 首跑基线 + 存活变异体分析）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: MQ 文档先行工作流强制要求 Phase 1 设计文档审查收敛后方可编写 Phase 2 实现 plan。本计划仅交付设计文档。
- Successor Required: yes —— 触发条件：本计划 done（设计文档审查收敛）+ 技术选型 Decision 落定。届时 DRAFT_PLANS 起草 Phase 2 实现 plan，plan 引用本文档作为范围与验收依据。

### 其余 16 域的 mutation score 基线

- Classification: `optimization candidate`
- Why Not Blocking Closure: Q1 首轮聚焦 finance/mfg/inv 三域（高复杂度优先）。其余域（含测试/mutation 比最低的 hr 0.16）作为 successor 首跑扩展。
- Successor Required: yes —— 触发条件：三域基线落盘 + 存活变异体分类工作流沉淀后扩展。

## Closure

Status Note: 本计划为 MQ Q1 文档先行工作流 Phase 1（纯设计文档，零代码/ORM/view/CI 变更）。Phase 1（6 节设计文档 + Q4 协同接口落盘）+ Phase 2（3 轮独立子代理审查收敛：R1 规范合规 + R2 覆盖面/可执行性 + R3 收敛复核，无残留 BLOCKER/MAJOR）均已完成。设计文档 `docs/architecture/quality-engineering/mutation-testing.md` 经审查修订后裁决：pitest（默认全集）+ 父 pom `<profile><id>mutation</id>` 激活 + 排除全部生成包（`_gen` 88 + `api.beans` 176 + `api.crud` 88 = 352 类）+ 首跑 finance/mfg/inv 三域 + nightly 软门控 CI。独立结束审计 PASS（2026-08-01，独立子代理新会话 fresh cold context），9 项 closure gate 全部核验通过。Q1 Phase 1 工作项交付完成，转 done；Phase 2 实现 plan 为独立 successor。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理，新会话 fresh cold context（task `ses_0444a7099ffehq8fVjD72gqfQa`，general 类型，未复用执行者上下文）
- Evidence: PASS（9 项 closure gate 全部核验通过）——
  1. **Phase 1 + Phase 2 全 ticked**：Phase 1（6 content items + 2 exit criteria）+ Phase 2（3 items + 2 exit criteria）全部 `[x]`，两 Phase `Status: completed`，无残留 `[ ]`。
  2. **交付物完整**：`mutation-testing.md` 落盘（421 行），6 必需节（§1 现状评估 / §2 目标与非目标 / §3 技术选型 / §4 实施步骤 / §5 验收判据 / §6 CI 门控设计）+ §8 Q4 协同接口（§8.1 协同契约 / §8.2 盲区清单格式 / §8.3 协同边界）齐全。
  3. **Review Record 收敛**：3 轮审查由 3 个不同 task id 子代理会话执行（`ses_0445457e...` R1 / `ses_04450662...` R2 / `ses_0444c8c8...` R3），无残留 BLOCKER/MAJOR（R1 1 MAJ+1 MIN、R2 1 MAJ+2 MIN 经 R3 逐条 live 复核 resolved；R3 1 NEW MIN 已修订）。
  4. **纯文档（git status）**：`git status --porcelain` 仅 2 个 `.md` 条目（plan 修改 + 新建设计 doc），零 Java/pom/yml/orm/xml 代码变更。
  5. **mvn 门控正确省略**：Phase 1 Exit Criteria + Closure Gates 均明示「纯文档计划，按 plan authoring guide 删除验证命令门控，原因：零 Java/ORM/CI 变更」（与前置 Q0 plan 一致）。
  6. **Deferred 合法范围外**：Q1 Phase 2 实现（`out-of-scope improvement`，文档先行工作流硬约束）+ 其余 16 域 baseline（`optimization candidate`），均有 successor 触发条件（反松弛满足）。
  7. **无双真相源**：§1 引用（非重推导）Q0 README + MA5 + roadmap，每条标注可复现核验命令 + 核验日期；live 复核 `rg "pitest" --glob '*.xml'` EXIT=1 与文档声明一致。
  8. **文本一致性**：Plan Status / Phase statuses / gates / 日志互洽（结束审计 PASS 后执行者翻转 Plan Status=completed + tick gates + 填充 closure evidence + 追加日志，状态自洽）。
  9. **Decision 三要素**：§3.4 技术选型（候选 pitest + 7 项替代否决理由 + R1-R4 残留风险）+ §6.3 CI 门控（候选 C-1/C-2/C-3 + 裁决 C-2 + 否决理由 + R5-R7 残留风险）三要素齐全。
  - 无 mvn 运行（按纯文档计划 Closure Gates 正确排除在范围外）。

Follow-up:

- Q1 Phase 2 实现 plan（设计文档收敛后起草；加载 `nop-testing` skill；引用本文档作为范围与验收依据）。
