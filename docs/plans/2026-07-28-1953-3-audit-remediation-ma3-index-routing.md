# 2026-07-28-1953-3-audit-remediation-ma3-index-routing MA3 索引路由有效性（A3.7）

> Plan Status: active
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA3（工作项 A3.7）
> Related: `docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.3「索引路由」行（MA3，当前 `新维度`）；`docs/audits/arm-index.md`（P1 索引）；`docs/skills/index-routing-audit-prompt.md`（审计方法——4 步：覆盖表 / 基于角色路由测试 / 结构质量检查 / 发现返回）；`docs/index.md`（顶层文档路由器——审查主目标）；各 `docs/*/README.md` 与子索引（审查目标）；`docs/plans/2026-07-28-1953-1-audit-remediation-ma3-owner-doc-vs-code-drift.md` + `2026-07-28-1953-2-audit-remediation-ma3-api-contract-consistency.md`（同批 MA3 审计，不同结果表面）
> Audit: required

## Current Baseline

索引路由有效性审计（文档-实现一致性层 MA3 第七项）。`docs/index.md` 是 nop-app-erp 的**顶级文档路由器**（自述"本文件是顶级文档路由器"），`AGENTS.md` §快速路由与 §文档所有权依赖它将任务路由到正确 owner doc。`docs/*/README.md` 与各目录子索引构成分层路由结构。

若索引结构失效（目标文件不存在 / 内容与声明目的不匹配 / 应索引的文档缺失 / 路由深度过深 / 角色无法通过索引找到答案），将导致**代理按错误路由实现**、**重复创建已有文档**、**owner doc 边界碎裂**、**审计/计划引用断链**。

**实时仓库 `docs/` 索引规模**（待审查）：

- **顶层索引 `docs/index.md`**（143 行）：路由权威 / 首先阅读表（~40 行路由条目）/ 推荐默认路径 / 技能路由表 / 域快速参考（占位"可选"）/ 目录角色（~22 个目录角色声明）/ 核心原则 / 命名规则。
- **`AGENTS.md` §快速路由**：独立路由表（8 行任务路由），与 `docs/index.md` 路由表部分重叠。
- **各目录 README/子索引**：`docs/context/README.md`、`docs/backlog/README.md`、`docs/design/README.md`、`docs/architecture/README.md`（已确认存在）、`docs/audits/`（`00-audit-execution-guide.md` + `arm-index.md`）、`docs/skills/README.md`、`docs/logs/index.md`、`docs/testing/index.md`、`docs/bugs/00-*.md`、`docs/requirements/README.md`、`docs/references/`、`docs/articles/README.md`、`docs/examples/README.md`、`docs/analysis/README.md`、`docs/lessons/README.md`、`docs/retrospectives/README.md`、`docs/input/README.md`、`docs/discussions/README.md`、`docs/process/`。
- **已知路由风险信号**：(1) `docs/index.md` 域快速参考表为占位（`<area>`/`<path>`），18 域未填充——多域项目应填充；(2) AGENTS.md 与 index.md 路由表重叠（同一事实两维护点，漂移风险）；(3) 文档树持续增长（plans/ 380 文件、audits/ 持续增长、design/ 18 域目录），索引时效性风险；(4) 归档机制（`docs/archive/`）引入的引用断链风险（AGENTS.md §14 要求检查 archive/）。

**此前从未做过一次索引路由有效性的系统性审查**。已知未核验控制点（index-routing-audit-prompt 4 步）：

- **步骤 1 覆盖表**：每个索引条目的目标文件是否存在 + 内容是否匹配声明目的 + 声明目的是否含糊 + 多条目指向同目标但描述不同 + 应索引的现有文档在索引中缺失。
- **步骤 2 基于角色路由测试**：4 角色（新开发人员 / AI 代理 / 审查者 / 维护者）的真实信息需求，追踪从索引到答案的最短路径，记录成功/失败与跳数。
- **步骤 3 结构质量检查**：孤立文件（无法从索引访问）/ 过时引用（指向已移动/重命名/删除文件）/ 深度不平衡（>3 跳）/ 重复（同一规则多索引无交叉引用）/ 类别混淆 / 缺少中间索引（>10 文件目录无 README）。
- **步骤 4 发现返回**：按严重性排序，每个含标题/受影响路径/当前差距/对路由有效性的影响/建议。

剩余差距：需要一次系统性索引路由有效性审查。发现的 blocker/major（**索引条目指向不存在文件** / **应索引的核心文档缺失** / **角色无法通过索引到达关键答案** / **>10 文件目录无 README**）登记为 P1（文档类目标 MR2）走 MR2 批量修复。索引审查为文档层，原则上不产生 P0；若发现索引断链致代理实际走入错误实现路径，升级标注交接相关审计维度。

## Goals

- 按 `index-routing-audit-prompt.md` 对 `docs/` 索引结构做系统性路由有效性审查（4 步：覆盖表 / 基于角色路由测试 / 结构质量检查 / 发现返回），产出审计报告。
- **覆盖全域索引面**：`docs/index.md` 顶层路由器 + AGENTS.md §快速路由 + 各 `docs/*/README.md` 子索引 + 关键目录中间索引。
- **角色路由测试定制化**：用 nop-app-erp 真实任务场景替换通用角色需求（如"finance 过账引擎 owner doc 在哪"/"某域 orm.xml 在哪"/"审计-修复 roadmap 当前 todo 在哪"/"Nop 平台 Delta 定制 runbook 在哪"）。
- scope matrix §2.3「索引路由」行终态标记（无终态标记 → `✅`/`⚠️(P1)`）。
- 发现的 blocker/major 登记为 P1 汇总至 `arm-index.md` §P1 发现汇总（文档类目标 MR2）。roadmap A3.7 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**做设计文档内容质量审查 — 归 A3.1（done）。本审计审**索引结构**的路由有效性（条目是否存在/匹配/可达），不审被索引文档的内容质量。
- **不**做前瞻性文档缺失扫描 — 归 A3.2（done）。本审计查"已有文档是否被正确索引"，不查"从未写的文档"。
- **不**做 owner doc vs 代码 drift — 归 A3.3-A3.5（同批）。本审计审索引路由，不审文档与代码一致性。
- **不**做 API 契约一致性 — 归 A3.6（同批）。
- **不**做可定制性验证 — 归 A3.8。
- **不**批量修复索引（补建缺失 README / 修复断链 / 填充域快速参考表）— finding 经 R2.0 展开机制进入 MR2。本审计只识别路由缺口 + 建议。
- **不**审计 `nop-entropy/docs-for-ai/` 兄弟目录索引（非本仓所有权）；仅在角色路由测试中核验跨目录引用可达性。
- **不**手改生成物或 ORM。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/index.md`（顶层文档路由器）；`AGENTS.md` §快速路由 + §文档所有权（路由规则依赖方）；各 `docs/*/README.md` 与子索引（`context/README.md`、`backlog/README.md`、`design/README.md`、`skills/README.md`、`logs/index.md`、`testing/index.md`、`audits/arm-index.md` + `00-audit-execution-guide.md`、`bugs/00-*.md`、`requirements/README.md`、`references/`、`articles/README.md`、`examples/README.md`、`analysis/README.md`、`lessons/README.md`、`retrospectives/README.md`、`input/README.md`、`discussions/README.md`）；`docs/references/document-naming-and-timeliness.md`（命名规则依赖方）
- Skill Selection Basis: `index-routing-audit-prompt.md`（roadmap A3.7 指定此 skill——索引路由有效性专用方法 4 步[覆盖表/角色路由测试/结构质量/发现] + 定制说明[角色需求替换/项目特定结构规则/分层索引检查/跳数阈值]。项目定制化层见 `docs/skills/README.md`）。与 A3.3-A3.6 不同结果表面（索引路由结构 vs 业务语义/契约），独立计划。
- Verification: 审计不改代码/文档，故无单测回归；报告产出即更新 `arm-index.md`。索引修复在 MR2 批量进行（本审计只识别 P1 + 建议）。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。本审计为纯文档索引审查，不构建/不运行应用。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码/文档，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。索引审查无回归风险，build/test 门控为同型审计 plan 的标准 Closure 实践。

## Execution Plan

### Phase 1 - 索引路由有效性系统性审查（4 步）

Status: planned
Targets: `docs/index.md`；`AGENTS.md` §快速路由 + §文档所有权；各 `docs/*/README.md` 与子索引；`docs/` 全目录树（孤立文件/缺中间索引检测）；`docs/archive/`（引用断链检测）
Skill: `index-routing-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）。本审计为 MA3 第七项，仅依赖 0.3，可与 A3.3-A3.6 同批推进（不同结果表面无相互依赖）。

- [ ] 步骤 1「覆盖表」：读取 `docs/index.md` 顶层路由表 + AGENTS.md §快速路由 + 各子索引，为每个索引条目记录（entry / stated purpose / target path / exists / matches purpose / notes）。标记：目标不存在 / 内容不匹配声明目的 / 声明目的含糊 / 多条目指同目标描述不同 / 应索引的现有文档缺失。
      - Skill: `index-routing-audit-prompt.md`
- [ ] 步骤 2「基于角色路由测试」：4 角色真实信息需求定制化追踪——角色 A 新开发人员（"如何设置开发环境运行项目"/"finance 过账引擎代码在哪"）/ 角色 B AI 代理（"编码前必须遵循哪些规则"/"某域 orm.xml owner doc 在哪"/"Nop Delta 定制 runbook 在哪"）/ 角色 C 审查者（"当前审计-修复 roadmap todo 在哪"/"近期实现日志在哪"/"某 plan 的关闭门控"）/ 角色 D 维护者（"何时更新索引 vs 新建文档"/"哪些文档已知过时"/"归档规则"）。每需求记录（persona / need / starting point / hops / found / path taken / problem）。
      - Skill: `index-routing-audit-prompt.md`
- [ ] 步骤 3「结构质量检查」：检查孤立文件（目录树中无法从任何索引访问）/ 过时引用（指向已移动/重命名/删除文件，含 archive/ 引用断链）/ 深度不平衡（>3 跳）/ 重复（同一规则多索引无交叉引用，重点 AGENTS.md vs index.md 路由表重叠）/ 类别混淆 / 缺少中间索引（>10 文件目录无 README，重点 architecture/、process/、references/）。
      - Skill: `index-routing-audit-prompt.md`
- [ ] 步骤 4「发现返回」：按严重性排序返回发现，每个含（标题 / 受影响索引条目或文件路径 / 当前差距 / 对路由有效性的影响 / 建议）。
      - Skill: `index-routing-audit-prompt.md`
- [ ] 产出审计报告 `docs/audits/2026-07-28-1953-arm-ma3-index-routing.md`（含：覆盖表 / 4 角色路由测试结果矩阵 / 结构质量检查摘要 / blocker/major/minor/note finding 清单 / 域快速参考表占位裁决 / AGENTS.md-vs-index.md 重复裁决 / 裁决通过/失败 / 剩余风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [ ] 覆盖表产出（每索引条目一行，exists/matches 列填写）
- [ ] 4 角色路由测试矩阵产出（每角色至少 2 需求，hops/found/problem 填写）
- [ ] 结构质量检查摘要产出（孤立文件/过时引用/深度/重复/缺中间索引 各至少一句结论）
- [ ] blocker/major/minor/note finding 清单产出，每个含受影响路径/差距/影响/建议

### Phase 2 - finding 汇总交接 MR2 + 索引/矩阵更新

Status: planned
Targets: 索引路由 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.3「索引路由」行
Skill: none

- Item Types: `Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [ ] finding 汇总：全部 blocker/major 登记为 P1 至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA3-NNN`、报告、描述、目标 MR2、修复状态 todo）。与 A3.1-A3.6 已登记 P1-MA3-* 去重无冲突。
      - Skill: none
- [ ] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.3「索引路由」行终态标记（`新维度` → `✅`/`⚠️(P1)`）。
      - Skill: none

Exit Criteria:

- [ ] 所有 blocker/major 已登记 arm-index §P1 汇总（目标 MR2），待 R2.0 展开
- [ ] 与 A3.1-A3.6 已登记 P1 经交叉去重无重复登记
- [ ] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_05769c927ffeWCeY8sdajaYnOC`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：roadmap A3.7 `todo` + Owner Doc/Skill/Deps 精确匹配 ✓；`docs/index.md` 实测 143 行且所有描述章节（路由权威/首先阅读表/技能路由/域快速参考占位/目录角色）均存在 ✓；`<area>`/`<path>` 占位 + AGENTS.md 路由表重叠风险信号经核实为真 ✓；skill 4 步方法与 plan phases 匹配 ✓；与 A3.1-A3.6/A3.8 边界清晰（索引路由结构 vs 文档内容质量）✓；anti-slack 零禁词 ✓；finding ID 不冲突（next P1-MA3-024+）✓。**采纳 3 项非阻塞修订**：(1) plans/ 文件计数更新 274+→380（实测）；(2) `docs/architecture/README.md` "待核实"→已确认存在；(3) scope matrix 行状态措辞精确化"新维度"→"无终态标记"。Plan Status 转 active。

## Closure Gates

> 本计划主体是文档索引审查（不改代码/文档）。完整仓库验证在此处运行一次（同型审计 plan 的标准 Closure 实践）。索引修复在 MR2 批量进行，本审计只识别 finding + 建议。

- [ ] 范围内行为完成（A3.7 索引路由有效性审查报告产出 + arm-index 更新 + scope matrix 标记完成）
- [ ] 相关文档对齐（审计报告、arm-index、scope matrix 结论已反映）
- [ ] 已运行验证：索引审查无代码变更，build/test 门控仅作回归基线确认（同型审计 plan 的相同 Closure 实践）
- [ ] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR2）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 设计文档内容质量（A3.1）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计审索引结构路由有效性；被索引文档的内容质量归 A3.1（done）。若索引审查中发现文档质量问题，标注交接。
- Successor Required: `no`——A3.1 已收口。

### 索引批量修复（MR2）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本审计识别路由缺口 + 建议；补建缺失 README / 修复断链 / 填充域快速参考表 / 去重 AGENTS.md-vs-index.md 属修复工作，经 R2.0 展开机制进入 MR2。
- Successor Required: `yes`——MR2 执行时落地。

### nop-entropy/docs-for-ai/ 兄弟目录索引

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 非本仓所有权。仅在角色路由测试中核验跨目录引用可达性（index.md 已路由到 `../nop-entropy/docs-for-ai/`）。
- Successor Required: `no`——跨仓索引归 nop-entropy 仓。

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
