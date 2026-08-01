# 2026-08-01-1121-1-mq-q0-quality-gap-analysis-readme 质量深化范围矩阵与 gap analysis 正式化

> Plan Status: completed
> Last Reviewed: 2026-08-01
> Mission: audit-remediation
> Work Item: MQ Q0
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q0
> Related: `docs/audits/audit-remediation-scope-and-dimension-matrix.md`；`docs/audits/compliance-baseline.md`；bug `docs/bugs/2026-08-01-finance-period-resolution-month-rollover.md`
> Audit: required

## Current Baseline

> 本计划不改代码/ORM/view。基线盘点从实时仓库核验（2026-08-01），为 Q0 的"NOT FOUND 证据确认"提供权威输入。MQ 里程碑是 audit-remediation MR1-MR6 全 done + MR6 CLOSED 后的**主动深化**里程碑（区别于 MR 系列的"审计发现→修复"被动模式）。

**audit-remediation 主线状态**：M0 / MA1-MA7 / MR1-MR6 / MV / MG 全部 done；MR6 milestone CLOSED（完成判据满足）。验证基线 `mvn clean install -DskipTests` 156 模块全绿；`mvn test` 1903 测试 0 failures。

**7 维度 NOT FOUND 实仓证据（核验日期 2026-08-01，HEAD 含 R6.9 收口）**：

- **Q1 变异测试（pitest）**：全仓 `rg "pitest" --glob '*.xml'` 零命中。审计文档 MA5 反复写"需 pitest 运行"但从未执行 → 1903 个测试的 mutation score 完全未知。
- **Q2 安全扫描**：全仓 `rg -il "owasp|dependency-check|spotbugs|findsecbugs|sonarqube|snyk" --glob '*.xml' --glob '*.yml' --glob '*.yaml'` 零命中。CI `.github/workflows/` 零安全扫描 job；MA6 仅做 RBAC 注解审计，传递依赖 CVE 与静态安全规则完全无人看。
- **Q3 属性测试**：全仓 `rg "jqwik|quickcheck" --glob '*.xml'` 零命中。当前测试均为黄金路径具体断言，无法证明 ERP 强不变量（借贷必相等 / 期间结账余额归零 / 成本层累加 = 余额表）在任意操作序列下恒成立。
- **Q4 故障注入**：零故障注入基础设施。审计反复发现 tryPost 吞异常 → posted=false 静默悬挂（finance P1-MA2-032 / hr P1-MA2-048 / assets P1-MA2-060 / qa P1-MA2-064 / projects P1-MA2-068 / maintenance P1-MA2-074 同型根因跨 6 域），MR1.16 已修单点但无系统性回归保护。
- **Q5 性能基线**：全仓 `rg "PerfTest|Benchmark|JMH|Gatling" --glob '*.java' --glob '*.xml'` 零命中。性能测试被显式 defer 到"首次生产数据规模"但无任何基础设施。
- **Q7 可观测性**：`rg -il "micrometer|prometheus|opentelemetry|otel"` 仅命中 `module-finance/erp-fin-service/.../ErpFinPostingMetrics.java`，其 import 仅 Nop `CoreMetrics`（进程内 ring-buffer，重启即失）+ 进程内 traceId。无 Micrometer/Prometheus/OTel/metrics API。
- **Q6 时钟基础设施（已知痛点）**：`CoreMetrics.registerClock` 全局静态（`module-common-test/.../AbstractFrozenClockExtension.java:63,68`），15 域子类共用一个全局静态时钟槽 → 并行不安全。bug `docs/bugs/2026-08-01-finance-period-resolution-month-rollover.md` 记录月初翻车重录快照税。

**目录基线**：`docs/architecture/quality-engineering/` 目录不存在（NOT FOUND）。

**剩余差距**：MQ 没有框架性 owner doc。Q1-Q7 各自的 Phase 1 设计文档需要一个稳定的范围矩阵 + 复杂度分级 + 实施顺序裁决作为引用基线，否则每个 Q 工作项独立决定顺序会漂移。Q0 的产出正是这个基线。

## Goals

- 正式化 MQ 范围矩阵：将上述 7 维度 NOT FOUND 证据**引用**（不重新推导，避免双真相源漂移）到稳定 owner doc `docs/architecture/quality-engineering/README.md`。
- 为每个维度给出**复杂度分级**（基础设施接入难度 / 涉及模块范围 / 对 nop-entropy 平台依赖程度）。
- 产出 **MQ 实施顺序裁决**（Q1-Q7 的执行顺序 + 理由），作为后续 DRAFT_PLANS 起草 Q1-Q7 Phase 1 设计文档计划的顺序依据。裁决须记录候选顺序、考虑的替代顺序、残留风险。
- 记录**候选排除维度**的纳入/排除裁决（契约测试 / 前端可访问性 / 数据迁移测试 / Nop 升级兼容性 / i18n 深度），给出排除理由与 successor 触发条件。
- 初始化 `docs/architecture/quality-engineering/` 目录。

## Non-Goals

- 不实现任何质量维度本身（变异测试 / 安全扫描 / 属性测试 / 故障注入 / 性能基线 / 时钟硬化 / 可观测性）——这些是 Q1-Q7 各自的 Phase 1 设计文档 + Phase 2 实现。
- 不修改任何代码 / ORM / view.xml / CI workflow。
- 不重新推导 NOT FOUND 证据（引用"当前基线"段与 roadmap 已核验事实，避免双真相源）。
- 不为 Q1-Q7 编写各自的 Phase 1 设计文档（那是各自工作项的 plan）。
- 不变更 audit-remediation-roadmap.md 的 MQ 工作项表结构（本计划只读引用 roadmap）。顺序裁决产出在 README，且 Decision 项已约束 README 须与 roadmap 依赖图一致（Q6→Q5 硬依赖），故不产生冲突、不回填 roadmap。

## Task Route

- Type: `verification or audit work`（gap analysis 正式化 + owner doc 编写；纯文档，零代码）
- Owner Docs: `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ（Q0 定义 + Q1-Q7 维度说明 line 671-789）；`docs/audits/audit-remediation-scope-and-dimension-matrix.md`（维度矩阵参照）
- Skill Selection Basis: 无匹配技能。Q0 是 gap analysis 正式化与架构 owner doc 编写，不涉及 BizModel / view.xml / ORM / 测试编写等 nop 开发任务。AGENTS.md 强制技能扫描已完成：`nop-backend-dev`/`nop-frontend-dev`/`nop-testing` 均不匹配"写质量工程框架 README"；`nop-debugging` 不匹配（无 bug 调查）。故 `Skill: none`。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. 本计划纯文档，不涉及端口/环境变量/CORS/密钥/.env/外部服务。

## Execution Plan

### Phase 1 - 范围矩阵 + NOT FOUND 证据引用 + 复杂度分级 + 候选排除裁决

Status: completed
Targets: `docs/architecture/quality-engineering/README.md`（新建）
Skill: none

- Item Types: `Add | Decision`
- Prereqs: MG done（已满足）；audit-remediation-roadmap.md §Milestone MQ 已登记（已满足）

- [x] Add: 初始化 `docs/architecture/quality-engineering/` 目录 + README.md 骨架（标题 / 目的 / 文档先行工作流引用回 roadmap §文档先行工作流）
      - Skill: none
- [x] Add: README §范围矩阵 —— 7 维度（Q1-Q7）每维度一节，引用（非重推导）本计划 Current Baseline 已核验的 NOT FOUND 证据 + roadmap line 783-789 的维度说明。每节标注证据核验日期与核验命令（可复现）。
      - Skill: none
- [x] Add: README §复杂度分级 —— 每维度评级（基础设施接入难度 / 涉及模块范围 / 对 nop-entropy 平台依赖程度 3 轴），评级依据记录。评级用于顺序裁决输入。
      - Skill: none
- [x] Decision: README §候选维度排除裁决 —— 对 5 个候选排除维度（契约测试 / 前端可访问性 / 数据迁移测试 / Nop 升级兼容性 / i18n 深度）逐一记录纳入或排除 + 理由 + successor 触发条件（roadmap line 782 已给排除方向，本项正式化裁决并补触发条件）。
      - 考虑的替代：全部纳入（否决——范围爆炸、与 MQ"7 维度深化"目标失焦）/ 全部排除并新建独立里程碑（否决——契约测试与 i18n 部分内容可折入 Q3 属性测试，独立里程碑重复）
      - 残留风险：候选排除维度后续可能因业务变化重新进入视野 → 触发条件已记录，DRAFT_PLANS 可据此重开
      - Skill: none
- [x] Decision: README §实施顺序裁决 —— 决定 Q1-Q7 的执行顺序，记录候选顺序 + 考虑的替代顺序 + 残留风险。裁决须与 roadmap 依赖图一致（硬约束：Q6 先于 Q5；Q1↔Q4 协同关系须体现）。
      - 决策输入：依赖图（Q6→Q5 硬依赖；Q1-Q4/Q6/Q7 仅依赖 Q0）+ 复杂度分级 + 已知痛点紧迫度（Q6 时钟月初翻车税为反复痛点）
      - 候选顺序（须在执行时最终裁决并记录理由）：倾向 Q6（解除 Q5 阻塞 + 消除反复痛点）早做；Q1 与 Q4 协同故相邻排期；Q5 在 Q6 之后
      - 考虑的替代：纯文档顺序 Q1→Q7（否决——忽视 Q6→Q5 硬依赖）/ 全并行（否决——Q5 硬依赖 Q6）
      - 残留风险：顺序裁决可能被后续执行中浮现的复杂度推翻 → README 标注"顺序为建议，可在后续 plan 起草时复议并回填此处"
      - Skill: none
- [x] Add: README §文档先行工作流引用 —— 引用 roadmap §文档先行工作流（Phase 1 设计文档 + 独立审查 ≥2 轮 / Phase 2 实现 / Phase 3 closure audit），声明本 README 是 Q1-Q7 各 Phase 1 设计文档的范围与顺序基线。
      - Skill: none

Exit Criteria:

> 本计划纯文档，零代码/ORM/view 变更。完整仓库 `typecheck`/`build`/`test` 不适用（按 plan authoring guide，无代码更改的计划删除验证命令门控，理由见 Closure Gates）。`docs/logs/` 为计划级结束步骤。

- [x] `docs/architecture/quality-engineering/README.md` 落盘，含上述 6 节（骨架 / 范围矩阵 / 复杂度分级 / 候选排除裁决 / 实施顺序裁决 / 文档先行工作流引用），每节可被 Q1-Q7 各自 plan 的 Task Route 引用为 owner doc。
- [x] NOT FOUND 证据每条标注可复现核验命令 + 核验日期（2026-08-01），便于后续 plan 复验。
- [x] 实施顺序裁决与 roadmap 依赖图一致（Q6 先于 Q5 得到体现），且记录了候选/替代/残留风险三要素。

## Draft Review Record

- Independent draft review iteration 1: accept (ses_044a4df8fffeNwPTFkBt54Wnox, 独立子代理 fresh session cold context) because 计划是纯文档 gap-analysis 契约，7 维度 NOT FOUND 实仓证据全部独立复核 PASS（pitest/jqwik/security/perf/observability/clock/quality-engineering dir/bug doc），模板完整、item 全打类型、两个 Decision 带 candidate+alternative+residual-risk、doc-only gate 省略有正当理由、Q0 未误套 Q1-Q7 文档先行 Phase 拆分、顺序裁决尊重 Q6→Q5 硬依赖。唯一 MINOR（Non-Goal "只读引用" vs "回填 roadmap 注记" 内部张力）已修订：删除回填子句，依赖 README 与依赖图一致性约束（Decision 项已保证），冲突场景不再成立。无 BLOCKER/MAJOR。converged → 转 active。

## Closure Gates

> 本计划无代码/ORM/view 变更（纯架构 owner doc 编写）。按 plan authoring guide §Closure Gates："对于无代码更改的计划（仅文档），删除验证命令门控并说明原因"——故不设 `mvn typecheck/build/test` 门控，原因：零 Java/ORM/CI 变更，全量构建无回归面。`bash docs/audits/nop-compliance-checker.sh` 同理不设（零生产代码/daoFor/import 变更，checker 无新增命中面）。

- [x] 范围内行为完成：README 6 节全部落盘且内容自洽（范围矩阵证据可复现 / 复杂度分级有依据 / 两个 Decision 记录候选+替代+残留风险三要素 / 候选排除有 successor 触发条件）
- [x] 相关文档对齐：README 与 roadmap §Milestone MQ 依赖图一致（Q6→Q5 硬依赖体现）；README 引用而非重推导 NOT FOUND 证据（无双真相源漂移）
- [x] 无验证命令门控（纯文档计划，原因如上）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（Draft Review Record 收敛）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中
- [x] `docs/logs/{year}/{month}-{day}.md` 追加本计划日志条目（计划级结束步骤）

## Deferred But Adjudicated

### Q1-Q7 各维度的 Phase 1 设计文档

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: Q0 是框架/顺序基线，Q1-Q7 各自 Phase 1 设计文档是各自工作项的独立 plan（不同结果表面）。Q0 不负责编写它们，只负责为它们提供范围矩阵 + 顺序裁决引用基线。
- Successor Required: yes —— 顺序裁决产出后，后续 DRAFT_PLANS 轮次按 README 裁决顺序起草 Q1-Q7 各自的 Phase 1 设计文档计划（触发条件：本计划 Q0 done + README §实施顺序裁决落盘）。

### 5 个候选排除维度的实际落地

- Classification: `watch-only residual`
- Why Not Blocking Closure: Q0 仅裁决纳入/排除并记录 successor 触发条件，不实现被排除维度。
- Successor Required: yes —— 各被排除维度的 successor 触发条件已在 README §候选维度排除裁决记录（如契约测试作 runtime Pact successor / i18n 深度部分折入 Q3）。

## Closure

Status Note: 本计划为纯文档 gap-analysis 正式化（零代码/ORM/view/CI 变更）。Phase 1 已执行完毕，README 6 节全部落盘。独立结束审计 PASS（2026-08-01，独立子代理新会话 fresh context），所有 NOT FOUND 证据命令独立复核结果与 README 声明一致，顺序裁决满足 Q6→Q5 硬依赖。Q0 工作项交付完成，转 done。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理，新会话 fresh cold context（task `ses_044a01c12ffeuzZwSVHCdM2wrD`，general 类型，未复用执行者上下文）
- Evidence: PASS（6 项结束判据全部核验通过）——
  1. **完整性**：README 6 节全部落盘且有实质内容（§目的 / §文档先行工作流引用 / §范围矩阵 / §复杂度分级 7 行 3 轴表 / §候选维度排除裁决 / §实施顺序裁决），无占位符。
  2. **NOT FOUND 证据可复现性**：独立子代理在 live repo 实跑全部 7 条核验命令，结果与 README 声明逐条一致（Q1-Q5 + Q4 故障注入零命中 EXIT=1；Q6 命中 `AbstractFrozenClockExtension.java:63,68` 两行；Q7 命中单文件 `ErpFinPostingMetrics.java`）。
  3. **无双真相源**：README §范围矩阵 显式引用 roadmap line 697 + plan §Current Baseline，不重推导；"1903 测试"等数字与 roadmap 一致，无矛盾断言。
  4. **依赖图一致性**：推荐顺序 `Q6 → Q1 → Q4 → Q3 → Q2 → Q5 → Q7` 中 Q6（位 1）先于 Q5（位 6），满足 Q6→Q5 硬约束（roadmap line 824）；候选顺序 + 替代顺序（纯文档序/全并行均否决）+ 残留风险三要素齐备。
  5. **Decision 项质量**：两个 Decision 均记录候选+替代+残留风险；5 个候选排除维度（契约测试/前端 a11y/数据迁移/Nop 升级/i18n 深度）各有 successor 触发条件。
  6. **计划内部一致性**：Plan Status=completed；Phase 1 Status=completed；Phase 1 六项 / Exit Criteria 三项 / Closure Gates 九项全部 `[x]`，无残留 `[ ]`。
  - 无 mvn 运行（按纯文档计划 Closure Gates 正确地排除在范围外）。

Follow-up:

- 后续 DRAFT_PLANS 轮次按 README §实施顺序裁决起草 Q1-Q7 各 Phase 1 设计文档计划（非阻塞跟进，触发见上 Deferred）。
