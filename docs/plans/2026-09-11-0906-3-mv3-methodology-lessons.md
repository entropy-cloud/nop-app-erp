---
status: active
mission: ai-check-r3
work-item: MV.3
group: "2026-09-11-0906"
verify: [test]
---

# 2026-09-11-0906-3 MV.3 方法学沉淀（脚本基线门控 + 规范成文先行入 lessons）

## Current Baseline

- **roadmap MV.3 义务（Deps: MV.2）**：两条模式入 `docs/lessons/`——①「机械类违规须脚本基线门控而非抽样审计」②「规范成文先于符合性审计」。本组 N=1（R1b/R1d 裁决）与 N=2（MV.2 索引终态）执行后依赖链满足（MV.2 产出的终态注记为本计划 Pattern ① 对账证据源）。
- **lessons 现状**：`docs/lessons/` 编号 01–19 齐备（最新 `19-statemachine-throw-domain-error-code-directly.md`），`README.md` 索引逐条一行摘要 + 提升裁决注记（2026-08-20/08-28 两批裁决范式：roadmap 点名必入 + 证据高频候选逐一裁决 + 同族划界归并）。新编入课号顺延 **20、21**。
- **Pattern ① 证据基（r3 主线实绩）**：r1/r2 抽样式审计的动机缺陷——「执行过多轮但始终存在问题」，机械可判定违规（硬编码中文）缺确定性清剿手段；本轮 M0.2 交付 `tools/check-hardcoded-cjk.mjs`（CAT-1..5 五类分级 + 文件级白名单豁免 + `--baseline` 快照 + `--strict` 单向收紧门控 + `--self-test` anti-fake-green 自证 13/13）；M0.3 冻结基线 `docs/audits/cjk-baseline.md`（对齐 compliance-baseline.md 单向收紧范式）；MI 批协议「每批第一动作 = 脚本红线 → 修复 → CAT 该域归零 + 域测试绿」+ 批注账；终态 CAT-1 335→0 / CAT-2 204→0 / CAT-3 390→0 / CAT-4 1700→0、白名单 27 文件四要素齐备（`ai-check-r3-mv1` 行交叉一致）。既有先例锚：`nop-compliance-checker.sh`（R1-R12 数值门控）+ `i18n-coverage-checker.sh`（F15 anti-fake-green 纪律）——「复用成熟 checker 范式、新脚本只补盲区」的方法论。
- **Pattern ② 证据基（M0.1 反事实对照）**：F15 曾将后端消息列 Non-Goal 的根因 = 标准不成文（全 docs 树无日志语言规定）——无标准则审计 finding 无判定准绳、修复无授权；本轮 M0.1 先立 `docs/architecture/i18n-compliance.md` 权威 owner doc（判定准绳 + 白名单登记格式 + 修复模式对照表，roadmap 规则 5：判定冲突以之为唯一权威），M0.5 冻结五维检查清单后才开 M1 审计——MI.2~MI.6/M1.x DIM-I 维全程零标准争议复判（修复批直接消费成文裁定，无 finding 争议与标准漂移）。对照面：M0.1 前的合规基线表在 roadmap 内冻结为历史快照（双重真相源 → 单一权威收敛）。
- **划界约束（防与既有 lessons 重复）**：lesson 07（compliance 基线漂移）讲「计划变更后未对账基线」——本计划 Pattern ① 讲「为何机械违规必须脚本门控而非抽样」，互补不同层；lesson 17（三路交叉审计）讲审计增量方法，非清剿手段；lesson 12/13 与本计划无叠面。入课内容须显式互链划界，不重写既有案例。
- **剩余差距**：lesson 20/21 不存在；`docs/lessons/README.md` 缺两行索引；MV.3 未完成则 MG.1（新失败模式沉淀，含 CJK 白名单裁决范式与 R1b/R1d「三元断言不足全表复跑」新失败模式）保持阻塞。

## Goals

- Pattern ① 入课为 lesson 20：机械类违规的确定性清剿 = 全量扫描脚本 + 基线单向收紧 + strict 门控 + anti-fake-green + 白名单唯一豁免通道 + 分批批注账，含 r3 CAT 计数对账实录与「脚本即测试」自检清单。
- Pattern ② 入课为 lesson 21：符合性审计前先成文标准（权威 owner doc + 冻结快照 + 清单冻结），含 F15 Non-Goal 反事实与 M0.1→MI/M1 零争议实录与「标准先行」自检清单。
- `docs/lessons/README.md` 索引两行 + 与 07/12/13/17 互链划界；lessons 树自洽（无孤儿文件、编号连续）。

## Non-Goals

- 零生产代码/checker 脚本/基线文件改动（纯文档沉淀面）。
- 不做 MG.1 项（CJK 白名单裁决范式、R1b/R1d 三元断言新失败模式、known-good-baselines 终态行复核、历史执行目录只读盘点——归 MG.1）；不做 roadmap 状态翻转；不做索引状态回填。
- 不修改 lessons 01–19 正文（互链仅限 README 索引行与新 lesson 内反向引用）。
- 不重述 r1/r2 mission 的方法学（各自收官流程所有）。

## Phase 1 — Pattern ① 入课 lesson 20（Add）

> 统一类型：Add（1 项 Add）。
> Skill: none（roadmap MV.3 行指定 none；课程体例遵循 `docs/lessons/README.md` 既有格式）
> Targets: `docs/lessons/20-mechanical-violations-script-baseline-gating.md`
> Prereqs: 本组 N=2（MV.2）终态注记落盘（对账数字取自终态注记而非中间态）

- [ ] <Add> 撰写 lesson 20「机械类违规须脚本基线门控而非抽样审计」：问题定义（抽样式审计对机械可判定违规的零遗漏性缺陷，r1/r2「执行多轮仍存在问题」实录）→ 方案五要素（全量扫描脚本 / CAT 分级 / 基线快照 + 单向收紧 strict 门控 / anti-fake-green 自证 / 白名单唯一豁免 + 批注账）→ r3 案例实录（CAT-1 335→0 / CAT-2 204→0 / CAT-3 390→0 / CAT-4 1700→0 分批对账链 + 白名单 27 文件 + `--self-test` 13/13）→ 既有范式复用（compliance checker 数值门控 + F15 checker 盲区补位原则）→ 自检清单（何时该建脚本而非抽审 / 新脚本的 anti-fake-green 义务 / 基线单向收紧纪律）；显式互链 lesson 07（基线漂移是门控的运维面）与 lesson 17（三路交叉是发现面，本课是清剿面）。
      - Skill: none

Exit Criteria:

- [ ] lesson 20 落盘且含全部五要素 + r3 实录对账数字（与 `ai-check-r3-mv1` 行/批注账逐值一致）+ 自检清单 + lesson 07/17 互链
- [ ] 文件命名/体例与既有 lessons 一致（编号、kebab slug、问题-方案-案例-清单结构）

## Phase 2 — Pattern ② 入课 lesson 21（Add）

> 统一类型：Add（1 项 Add）。
> Skill: none
> Targets: `docs/lessons/21-standard-codification-before-conformance-audit.md`
> Prereqs: Phase 1 完成（编号顺延一致）

- [ ] <Add> 撰写 lesson 21「规范成文先于符合性审计」：问题定义（标准不成文 → finding 无判定准绳 → 修复无授权，F15 后端消息 Non-Goal 反事实实录）→ 方案三步（权威 owner doc 成文 + 判定冲突唯一权威声明 / 成文时点快照冻结防真相源漂移[互链 lesson 13] / 检查清单冻结后才开审计[M0.5 先例]）→ r3 案例实录（`i18n-compliance.md` 承载合规基线裁定表 → MI.2~MI.6 修复批 + M1.x DIM-I 维零标准争议复判）→ 自检清单（审计 mission 开工前「标准在哪/谁裁决/冲突以谁为准」三问）；显式互链 lesson 12（成文标准 ≠ 文档化简化滥用的挡箭牌——关闭载体仍是代码行为）与 lesson 13（快照冻结防陈旧）。
      - Skill: none

Exit Criteria:

- [ ] lesson 21 落盘且含三步方案 + F15 反事实与 r3 实录 + 自检清单 + lesson 12/13 互链
- [ ] 两条 lesson 对同一案例集的引用口径一致（无相互矛盾的数字/表述）

## Phase 3 — 索引对账与树自洽（Proof）

> 统一类型：Proof（2 项 Proof）。
> Skill: none
> Targets: `docs/lessons/README.md`
> Prereqs: Phase 1 + Phase 2 完成

- [ ] <Proof> README 索引更新：lesson 20/21 各一行摘要（体例对齐 04–19 行）+ 提升裁决注记（来源 mission ai-check-r3 MV.3 + roadmap 点名义务 + 同族划界说明）。
      - Skill: none
- [ ] <Proof> 树自洽机械核验：`ls docs/lessons/` 编号 01–21 连续零孤儿；README 索引行 ↔ 实存文件一一对应；全 lessons 树 `git status` 变更面 = 本计划登记文件集（新 2 文件 + README，零生产代码触碰声明）。
      - Skill: none

Exit Criteria:

- [ ] README 两行索引 + 提升裁决注记在案，编号连续零孤儿
- [ ] 变更面核验通过（恰 3 文件），零生产代码/脚本触碰声明在案

## Draft Review Record

- dispatch review #review-2026-09-09-210030-mission-driver-2026-09-11-0906-3-mv3-methodology-lessons-1-e22a5ed2 to opencode/glm-5.3-flash
- 2026-09-11：iteration 1，共识 accept #review-2026-09-09-210030-mission-driver-2026-09-11-0906-3-mv3-methodology-lessons-1-e22a5ed2

## Verification

> 本计划为纯文档沉淀面（Non-Goals：零生产代码/checker 脚本/基线文件改动）。按计划指南「无代码更改的计划删除验证命令门控并说明原因」：不适用 `mvn` build/test、compliance checker、CJK/i18n 门控；验证证据由 Phase 1~2 落盘的 lesson 20/21 正文（五要素/三步方案 + 实录对账数字 + 自检清单 + 互链划界）与 Phase 3 的机械核验（`ls` 编号连续零孤儿、README 索引行 ↔ 实存文件一一对应、`git status` 变更面恰 3 文件）承载。实录对账数字须与 `docs/testing/known-good-baselines.md` `ai-check-r3-mv1` 终态行/MI 终态行逐值一致（CAT-1 335→0 / CAT-2 204→0 / CAT-3 390→0 / CAT-4 1700→0 / 白名单 27 文件 / `--self-test` 13/13）。

## Closure Findings

- [ ] 范围内行为完成（lesson 20/21 落盘 + README 两行索引，Goals 三项全部落地；Phase 1~3 全部执行项与 Exit Criteria 勾选，实录对账数字与 known-good-baselines 终态行逐值一致）
- [ ] 相关文档对齐（README 索引 + 07/12/13/17 互链划界在案；lessons 树编号 01–21 连续零孤儿）
- [ ] 已运行验证（按 Verification 节：纯文档沉淀面，以机械核验注记为验证证据，构建/测试命令门控不适用——理由已注记）
- [ ] 变更面核验（收官时 `git status` 复核写入面仅限 2 个新 lesson 文件 + `docs/lessons/README.md`，零生产代码/脚本触碰）
- [ ] 无范围内项目降级为 deferred/follow-up（MG.1 项按 Non-Goals 归属 successor，属范围边界而非降级）
- [ ] 独立草案审查已完成并记录（见 Draft Review Record）
- [ ] 文本一致性已验证：frontmatter status、各 Phase 状态、Exit Criteria、Closure Findings 与 `docs/logs/` 条目一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中（`## Closure` Status Note + Closure Audit Evidence 配对 dispatch/accepted 行在案）

## Closure

Status Note: <why the plan can close>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
