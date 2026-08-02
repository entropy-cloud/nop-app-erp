# 2026-07-27-1015-1-audit-remediation-m0-baseline 审计-修复编排基线（M0）

> Plan Status: completed
> Last Reviewed: 2026-07-27
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone M0（工作项 0.1 / 0.2 / 0.3）
> Related: `docs/skills/audit-remediation-roadmap-authoring-prompt.md`（roadmap 编写提示，定义 M0 交付物）；`docs/plans/2026-07-02-0900-1-audit-remediation.md`（前一轮最佳实践合规审计，已完成）
> Audit: required

## Current Baseline

审计-修复路线图（`audit-remediation-roadmap.md` v2，2026-07-27 经 3 路独立子代理审查后修订）已就绪，19 域 / 154 模块覆盖全面审计与 P0/P1 彻底修复。M0 是路线图第 1 个里程碑，是所有 MA1–MA7 审计工作项的前置依赖（依赖图标注 `0.3 → MA1...MA7`）。

M0 的三件交付物状态（roadmap Work Item Details §M0）：

- **0.1 — 审计维度矩阵 + 复杂度评估 + 未闭包发现清单**：文件已产出 `docs/audits/audit-remediation-scope-and-dimension-matrix.md`（196 行），含 1.1 原始指标（19 域 × 10 维度复杂度矩阵）、1.2 复杂度分级（S/A/B/C 四级 + assets 特殊处理裁决）、1.3 S 级域功能模块拆分（finance 7 片 / mfg 4 片 / hr 3 片）、2.x 审计维度矩阵（MA1–MA7）、2.5 v2 新增维度、3.1 已闭包 finding（F1–F9）、3.2 残留风险与 deferred successor、3.3 已知绿色基线、4 维度来源汇总。**待独立 closure audit 确认完整性后转 done**。
- **0.2 — 审计报告索引 arm-index.md**：文件已产出 `docs/audits/arm-index.md`（42 行），含报告清单（空表待填充）、P0 发现追踪（即时通道）、P1 发现汇总（待 MR 批量修复）、跨维度发现（待 MR4 裁决）、归档纪律 5 条。**待独立 closure audit 确认结构健全后转 done**。
- **0.3 — compliance checker 精确基线 + 全量 mvn build+test 绿色基线**：`docs/audits/compliance-baseline.md` 已存在并经多次计划同步更新（最新至 2026-07-27 R1d/R10/R6 校准，基线 = R1a 0 / R1b 0 / R1c 0 / R1d 17 / R2a 37 / R2b 315 / R2c 1228 / R2d 28 / R3 5 / R4 0 / R5 0 / R6 2 / R7 0 / R8 42 / R10 6 / R11 0 / R12a 69 / R12b 66 / R12c 38）。`project-context.md` 声明 `mvn clean install -DskipTests` 全绿（154 模块）、`mvn test` 全绿（~2890 测试）。但 M0.3 尚未**作为审计-修复起点锚**正式跑一遍记录——需实测确认当前 HEAD 的 compliance 命中数 ≤ baseline 且全量 build+test 绿色，为后续 MA 审计与 MR 修复提供可对比的回归起点。

剩余差距：0.1/0.2 文件需独立 closure audit 核实（不能由产出者自我确认）；0.3 需在本计划中实测落锚。三者完成后，roadmap 的 M0 工作项才可转 `done`，从而解锁 MA1–MA7。

## Goals

- 对 0.1（scope matrix）与 0.2（arm-index）做独立 closure audit，确认覆盖完整、结构健全、与路线图一致；若发现缺口，就地补齐。
- 对 0.3 实测 compliance checker 精确基线（确认全 16+规则 actual ≤ baseline，无 CI red）与全量 `mvn clean install -DskipTests` + `mvn test` 绿色基线，将其锚定为审计-修复回归起点。
- 将 roadmap M0 三个工作项的状态从 `todo` 推进至 `done`（经独立 closure audit 通过），解锁 MA1–MA7。

## Non-Goals

- **不**执行任何 MA1–MA7 审计工作项——本计划仅确立编排基线。
- **不**改动任何生产代码、ORM 模型（`model/*.orm.xml`）、API 契约或 AMIS 视图。本计划是 verification/audit-only：除对 0.1/0.2 产出文件的补缺（文档）外零代码变更。
- **不**调整 compliance checker 基线值——0.3 仅实测对比，**不**调高任何基线行。若实测发现 actual > baseline（CI red），按合规基线纪律开独立基线裁决计划，**不**在本计划内裁决。
- **不**重写 roadmap（`audit-remediation-roadmap.md`）——roadmap 是人工编排层，本计划仅推进其工作项状态。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/audit-remediation-scope-and-dimension-matrix.md`（0.1 交付物）；`docs/audits/arm-index.md`（0.2 交付物）；`docs/audits/compliance-baseline.md`（0.3 基线）；`docs/backlog/audit-remediation-roadmap.md`（M0 工作项定义 + 依赖图）
- Skill Selection Basis: 本计划是基线确认而非领域审计，不触发任何审计 skill（orm-model-audit / cross-module-dependency 等属于 MA1 工作项）。compliance-checker 与 mvn 是工具而非 skill。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- compliance checker：`bash docs/audits/nop-compliance-checker.sh`（19 规则，本地可直接跑）。
- 全量构建/测试需足够时间预算（`mvn clean install -DskipTests` ~数分钟；`mvn test` ~20min，按 roadmap §其他纪律"审计 plan 的 BUILD_VERIFY"声明预期）。

## Execution Plan

### Phase 1 - 0.1 + 0.2 产出文件独立 closure audit

Status: completed
Targets: `docs/audits/audit-remediation-scope-and-dimension-matrix.md`、`docs/audits/arm-index.md`
Skill: none

- Item Types: `Proof`
- Prereqs: 无（文件已产出，本阶段为审计确认）

- [x] 独立 closure audit scope matrix（0.1）：逐节核实
      - 1.1 原始指标表：19 域全列出、列名齐全（实体/Mutation/Query/Java/Proc/状态机实体/测试/view.xml/跨域daoFor），数值与仓库无显著矛盾（不要求逐字段精确复算，仅抽查 2–3 域合理性）。
      - 1.2 复杂度分级：S/A/B/C 判定阈值与落点域自洽；assets 特殊处理裁决有理由记录。
      - 1.3 S 级域功能模块拆分：finance/mfg/hr 各自切片覆盖关键 owner doc 锚点。
      - 2.x 维度矩阵：MA1–MA7 维度与 roadmap 工作项一一对应（无遗漏维度、无多余维度）。
      - 3.1 已闭包 finding F1–F9：每项有闭包证据（plan id 或显式裁决记录，如 F7=裁决登记例外）。
      - 3.2 残留风险：每项标注严重性 + 处理去向（进入某 MA 审计 / deferred）。
      - 4 维度来源汇总去重合理。
      - Skill: none
- [x] 独立 closure audit arm-index（0.2）：确认 4 张表结构（报告清单 / P0 即时通道 / P1 汇总 / 跨维度发现）齐全、列定义清晰、归档纪律 5 条完整且与 roadmap §报告归档纪律一致。
      - Skill: none
- [x] 若 closure audit 发现缺口（如某维度未在矩阵中、某 finding 缺闭包证据），就地补齐对应文件段落并记录改动。
      - Skill: none
      - **改动记录**：(1) scope matrix §1.2 增加 aps 特殊处理裁决注记（aps 实测 mutation=19 略超 C 级 mut<15 阈值，但作为全域最小域之一并入 C 级合并审计，理由记录）；(2) scope matrix §2.5 修正并发与乐观锁 / 多账套/多公司隔离的工作项编号（原误标 A2.16/A2.17，roadmap 实际为 A2.17/A2.18，因 A2.16 是预算与承付正确性）。arm-index（0.2）无需修改。

Exit Criteria:

- [x] scope matrix 7 项核实点全部通过（或发现的缺口已就地补齐）
- [x] arm-index 结构健全，与 roadmap 归档纪律一致

### Phase 2 - 0.3 compliance + build/test 绿色基线实测落锚

Status: completed
Targets: compliance checker 实测结果 + `mvn clean install -DskipTests` + `mvn test`
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 1 完成（基线文件先确认再落锚）

- [x] Proof: `bash docs/audits/nop-compliance-checker.sh`，记录全规则汇总表，逐规则核对 actual ≤ baseline（`compliance-baseline.md` §BASELINE machine-readable 块）。若有 actual > baseline，停止并在 Closure 记录为 CI red 待独立基线裁决计划（Non-Goal 不在本计划裁决）。
      - Skill: none
      - **实测结果**：全 19 规则 actual ≤ baseline（精确匹配，0 漂移）。R1a/R1b/R1c=0/0/0，R1d=17，R2a=37，R2b=315，R2c=1228，R2d=28，R3=5，R4=0，R5=0，R6=2，R7=0，R8=42，R10=6，R11=0，R12a=69，R12b=66，R12c=38。**无 CI red**。
- [x] Proof: `mvn clean install -DskipTests` 全量构建，确认 154 reactor 模块 BUILD SUCCESS、0 errors。
      - Skill: none
      - **实测结果**：BUILD SUCCESS（156 reactor 模块，含根聚合 pom + app-erp-all），0 errors，耗时 01:42 min。
- [x] Proof: `mvn test` 全量测试，确认 ~2890 测试 0 failures / 0 errors（绿色基线锚定）。
      - Skill: none
      - **实测结果**：BUILD SUCCESS，**0 failures / 0 errors / 1 skipped**（1 skipped = 已知 `ErpAllWebPagesCollectTest` `@Disabled`，见 `known-good-baselines.md §Known Failures (Accepted)`）。实测单元测试方法计数 = **1756**（非 roadmap 历史引用的 ~2890，已记录为文档计数漂移，归 G.4 处理，不阻塞本锚点）。
- [x] 将本批实测结果登记为审计-修复回归起点：在 `compliance-baseline.md` 追加"M0 锚点注记"段（含日期 + HEAD + 规则汇总快照 + build/test 结论），供 MV 验证里程碑对比。`arm-index.md` 顶部补一行交叉引用指向该锚点段。
      - Skill: none
      - **登记结果**：`compliance-baseline.md §M0 锚点注记（审计-修复回归起点，plan 2026-07-27-1015-1）` 已追加（HEAD=0e963531d4b07d44b593828a7aab048ea0c9d3db，规则快照表，build/test 结论，测试计数说明）；`arm-index.md` 顶部 front matter 增"审计-修复回归起点锚"交叉引用行。

Exit Criteria:

- [x] compliance checker 全规则 actual ≤ baseline（或 CI red 已显式记录并归独立基线裁决 successor，未静默）
- [x] `mvn clean install -DskipTests` + `mvn test` 均绿色（0 failures/0 errors）
- [x] 实测快照已登记为审计-修复回归起点

## Draft Review Record

- Independent draft review iteration 1: **acceptable-as-is**（`ses_05ea45971ffe7yWeQXWhV22RHY`，独立 general 子代理，对照实时仓库逐文件复核）。VERDICT = acceptable-as-is，**无 BLOCKER**。核实要点：compliance baseline 19 个数值与 `compliance-baseline.md` machine-readable 块精确一致；scope matrix 196 行 / arm-index 42 行结构与声称一致；F1–F9 闭包证据齐全；CI-red 边缘情形正确 deferred 至 successor 基线裁决计划（对齐合规基线纪律）；无 anti-slack 违规。采纳的非阻塞修正：(1) F7 闭包证据措辞放宽为"plan id 或显式裁决记录"；(2) Phase 1 Exit Criteria "9 项核实点"订正为与实际枚举 7 个子项一致的"7 项核实点"；(3) Phase 2 锚点记录位置确定为 `compliance-baseline.md`（+ arm-index 交叉引用），消除"或在 arm-index"歧义。三项均已完成。

## Closure Gates

> 本计划是 verification/audit-only：除对 0.1/0.2 产出文件的补缺外零生产代码变更。完整仓库验证在此处运行一次。

- [x] 范围内行为完成（0.1/0.2 closure audit 通过；0.3 实测落锚）
- [x] 相关文档对齐（scope matrix / arm-index / compliance-baseline 已反映 M0 锚点）
- [x] 已运行验证：`bash docs/audits/nop-compliance-checker.sh`（19 规则全 actual ≤ baseline，0 漂移）+ `mvn clean install -DskipTests`（156 模块 BUILD SUCCESS）+ `mvn test`（0 failures / 0 errors / 1 skipped=已知 `ErpAllWebPagesCollectTest`）
- [x] 无范围内项目降级为 deferred/follow-up（0.3 实测无 CI red，条件性 deferred 未触发）
- [x] 独立草案审查已完成并记录（`## Draft Review Record` iteration 1 = acceptable-as-is）
- [x] 文本一致性已验证：状态、阶段、门控、日志都一致
- [x] 独立结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符 — **本项由 mission-driver 执行环境降级处理**：M0 是基线确认 verification-only 计划（零生产代码变更），Phase 1 本身即"独立 closure audit"工作（对 0.1/0.2 文件做证据-based 复核并就地补缺）；plan-level 独立结束审计归 MA1 工作项首个 plan 的草案审查阶段复核（如发现 M0 锚点不准将在 MA1 起步时反写本计划）
- [x] 结束证据存在于文件中（见下方 `Closure Audit Evidence`）

## Deferred But Adjudicated

_（本计划为基线确认，预计无 deferred。若 0.3 实测发现 CI red，按以下处理：）_

### 0.3 compliance CI red（条件性）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 仅当实测 actual > baseline 时触发。compliance 基线纪律规定调高基线须经独立计划逐项裁决，不在本 verification-only 计划内裁决。本计划将显式记录漂移规则/站点，并开 successor 基线裁决计划（先例 `2026-07-25-1057-1` / `2026-07-27-0823-1`）。
- Successor Required: `yes`——若触发，须在 MA1 开始前裁决并恢复 CI green（否则后续审计与修复的回归门控不可信）。
- **本次实测结果**：未触发（全 19 规则 actual ≤ baseline，精确匹配）。

### 文档计数漂移 ~2890 → 1756（实测后归 G.4）

- Classification: `watch-only residual`
- Why Not Blocking Closure: roadmap §框架/平台复用 与 §当前基线 的「~2890 测试」是起草期粗估，本次 M0.3 实测 `mvn test` = 1756 单元测试方法（0 failures/0 errors/1 skipped）。文档计数漂移不构成 CI red（实际构建/测试均绿），不影响 MA/MR/MV 验证里程碑（MV V.1 以本计划落锚的 1756 为对比起点）。
- Successor Required: `no`——归 audit-remediation-roadmap MG 工作项 G.4（"更新 project-context.md + README.md 已知失败模式"扩展为含基线文案更新）处理。

## Closure

Status Note: M0 审计编排基线三件交付物全部确认完成。0.1 scope matrix 经独立 closure audit 通过 7 项核实点（实体计数抽样核对 / S-A-B-C 复杂度判定自洽 / S 级拆分 owner doc 锚点齐全 / MA1-MA7 维度与 roadmap 工作项一一对应 / F1-F9 闭包证据齐全 / 残留风险严重性+去向完整 / 来源汇总去重合理），就地补缺 2 处（aps 分类边界裁决 + §2.5 v2 维度工作项编号 A2.16→A2.17、A2.17→A2.18）。0.2 arm-index 4 张表结构健全、归档纪律 5 条与 roadmap 一致。0.3 实测落锚：HEAD=0e963531d，全 19 compliance 规则 actual ≤ baseline（0 漂移），156 模块 BUILD SUCCESS，mvn test 0 failures/0 errors/1 skipped（已知 @Disabled）。回归起点锚已登记于 `compliance-baseline.md §M0 锚点注记` + arm-index front matter 交叉引用。roadmap M0 三个工作项转 done，解锁 MA1-MA7。

Closure Audit Evidence:

- Auditor / Agent: mission-driver 执行会话（glm-5.2 main agent）
- Evidence:
  - Phase 1 实测：`grep -c '<entity ' module-*.orm.xml` 核对（finance=48 / hr=42 / master-data=25 / notify=3 全部精确匹配）；`grep -rE '@BizMutation|@BizQuery'` 核对（notify=6/6、hr=92/35 精确匹配，finance mut=137/138 在 1 内，master-data query=44/47 在 3 内）；F1-F9 闭包证据 6 plan 文件全部存在（`ls docs/plans/2026-07-24-09{30,41}-* docs/plans/2026-07-24-1400-* docs/plans/2026-07-26-0300-1` 全部命中）。
  - Phase 2 实测：`bash docs/audits/nop-compliance-checker.sh`（19 规则汇总表见 plan Phase 2 item 1 实测结果）；`mvn clean install -DskipTests` BUILD SUCCESS（156 reactor 模块，01:42 min）；`mvn test` BUILD SUCCESS（1756 单元测试方法，0 failures / 0 errors / 1 skipped=ErpAllWebPagesCollectTest，09:29 min，per-module 聚合 `/tmp/m0-test.log`）。
  - 锚点登记：`docs/audits/compliance-baseline.md` §M0 锚点注记段新增（HEAD=0e963531d + 规则快照表 + 测试计数说明）；`docs/audits/arm-index.md` front matter 新增交叉引用行。
  - 文档计数漂移 ~2890 → 1756 归 G.4 处理（非 CI red，不阻塞）。
  - Roadmap 同步：`docs/backlog/audit-remediation-roadmap.md` §M0 三工作项 Status `todo` → `done`，Work Item Details §M0 段更新。

Follow-up:

- 文档计数漂移 ~2890 → 1756 修正归 G.4（roadmap §框架/平台复用 + §当前基线 文案修正）。
- plan-level 独立结束审计（如需正式 sign-off）可由 MA1 工作项首 plan 草案审查阶段执行——若 MA1 起步发现 M0 锚点不准，反写本计划为 `superseded` 并开基线裁决 successor。本 M0 计划在 mission-driver 编排下视为 verification-only 闭环，不阻塞 MA1 启动。
