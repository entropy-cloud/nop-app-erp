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

- [x] <Add> 撰写 lesson 20「机械类违规须脚本基线门控而非抽样审计」：问题定义（抽样式审计对机械可判定违规的零遗漏性缺陷，r1/r2「执行多轮仍存在问题」实录）→ 方案五要素（全量扫描脚本 / CAT 分级 / 基线快照 + 单向收紧 strict 门控 / anti-fake-green 自证 / 白名单唯一豁免 + 批注账）→ r3 案例实录（CAT-1 335→0 / CAT-2 204→0 / CAT-3 390→0 / CAT-4 1700→0 分批对账链 + 白名单 27 文件 + `--self-test` 13/13）→ 既有范式复用（compliance checker 数值门控 + F15 checker 盲区补位原则）→ 自检清单（何时该建脚本而非抽审 / 新脚本的 anti-fake-green 义务 / 基线单向收紧纪律）；显式互链 lesson 07（基线漂移是门控的运维面）与 lesson 17（三路交叉是发现面，本课是清剿面）。
      〔执行证据 2026-09-11：`docs/lessons/20-mechanical-violations-script-baseline-gating.md` 落盘——问题定义（r1/r2 抽样缺陷 + 「脚本绿 = 闭环」论点）/ 五要素表（逐要素 r3 落地形态 + 反面）/ CAT 终态对账表（四条对账链含分批拆解 + 白名单 27 文件批1 11+批2 16 + `--self-test` 13/13 + 冻结快照 453→170 files + MI.8 批1 门控抓真犯与批2 快照误触还原实录）/ 既有 checker 复用节（nop-compliance-checker.sh + i18n-coverage-checker.sh 盲区补位）/ 与 07/17/12/13 划界表 / 三段自检清单，全部在案〕
      - Skill: none

Exit Criteria:

- [x] lesson 20 落盘且含全部五要素 + r3 实录对账数字（与 `ai-check-r3-mv1` 行/批注账逐值一致）+ 自检清单 + lesson 07/17 互链
      〔执行证据 2026-09-11：五要素齐备；对账数字与 `docs/testing/known-good-baselines.md` `ai-check-r3-mv1` 终态行 + 2026-09-08 MI 终态行 + `docs/audits/cjk-baseline.md` §批注账逐值一致（CAT-1 335→0 = MI.2 93+MI.3 165+MI.4 77 / CAT-2 204→0 = MI.5a 136+MI.5b 68 / CAT-3 390→0 = MI.6 批1 181+批2 209 / CAT-4 1700→0 = MI.8 批1 382+批2 1318 / 白名单 27 = 批1 11+批2 16 / `--self-test` 13/13）；lesson 07/17 互链在「与既有 lesson 的划界」节〕
- [x] 文件命名/体例与既有 lessons 一致（编号、kebab slug、问题-方案-案例-清单结构）
      〔执行证据 2026-09-11：`20-mechanical-violations-script-baseline-gating.md` kebab slug 顺延编号 20；结构对齐既有范式（blockquote 来源/适用场景/失败模式 + 核心论点 + 方案/案例表 + 自检清单），与 lesson 07/17/19 体例一致〕

## Phase 2 — Pattern ② 入课 lesson 21（Add）

> 统一类型：Add（1 项 Add）。
> Skill: none
> Targets: `docs/lessons/21-standard-codification-before-conformance-audit.md`
> Prereqs: Phase 1 完成（编号顺延一致）

- [x] <Add> 撰写 lesson 21「规范成文先于符合性审计」：问题定义（标准不成文 → finding 无判定准绳 → 修复无授权，F15 后端消息 Non-Goal 反事实实录）→ 方案三步（权威 owner doc 成文 + 判定冲突唯一权威声明 / 成文时点快照冻结防真相源漂移[互链 lesson 13] / 检查清单冻结后才开审计[M0.5 先例]）→ r3 案例实录（`i18n-compliance.md` 承载合规基线裁定表 → MI.2~MI.6 修复批 + M1.x DIM-I 维零标准争议复判）→ 自检清单（审计 mission 开工前「标准在哪/谁裁决/冲突以谁为准」三问）；显式互链 lesson 12（成文标准 ≠ 文档化简化滥用的挡箭牌——关闭载体仍是代码行为）与 lesson 13（快照冻结防陈旧）。
      〔执行证据 2026-09-11：`docs/lessons/21-standard-codification-before-conformance-audit.md` 落盘——问题定义（F15 反事实：全 docs 树无日志语言规定 → 后端消息 Non-Goal + r1/r2 多轮未立项清剿根因）/ 三步表（i18n-compliance.md 唯一权威声明 + roadmap 裁定表冻结为历史快照 + 探针计数指针化防 lesson 13 陈旧 + M0.5 五维 × 21 核对单元 105 基础格冻结矩阵）/ r3 零争议实录（MI.2~MI.6 修复批消费成文裁定 + M1.x DIM-I 维 28 份 ck-* 报告零 finding 争议 + 白名单四要素机械核验可行）/ 三问两查自检清单 / 与 12/13/20 划界表，全部在案〕
      - Skill: none

Exit Criteria:

- [x] lesson 21 落盘且含三步方案 + F15 反事实与 r3 实录 + 自检清单 + lesson 12/13 互链
      〔执行证据 2026-09-11：三步方案表 + F15 Non-Goal 反事实（根因 = 标准不成文）+ r3 零争议实录 + 「三问两查」自检清单齐备；lesson 12 互链（关闭载体判别式：成文标准 ≠ 挡箭牌）与 lesson 13 互链（快照冻结 + 计数指针化）在「与既有 lesson 的划界」节〕
- [x] 两条 lesson 对同一案例集的引用口径一致（无相互矛盾的数字/表述）
      〔执行证据 2026-09-11：两课对 r3 案例集引用逐值互查一致——lesson 20 引 CAT 对账数字（335→0/204→0/390→0/1700→0/白名单 27/self-test 13/13），lesson 21 引 CAT-1/2/3 终态时与 lesson 20 同源同值且互链指向；M0.1/M0.5/M0.2 产物指针、mission/plan 指针两课一致，零矛盾表述〕

## Phase 3 — 索引对账与树自洽（Proof）

> 统一类型：Proof（2 项 Proof）。
> Skill: none
> Targets: `docs/lessons/README.md`
> Prereqs: Phase 1 + Phase 2 完成

- [x] <Proof> README 索引更新：lesson 20/21 各一行摘要（体例对齐 04–19 行）+ 提升裁决注记（来源 mission ai-check-r3 MV.3 + roadmap 点名义务 + 同族划界说明）。
      〔执行证据 2026-09-11：`docs/lessons/README.md` §Lessons 追加 lesson 20/21 两行索引（体例对齐 04–19 行：编号反引号 + 加粗论点 + 摘要 + 实录数字 + 自检/划界提示）+ 表尾新增「2026-09-11 提升裁决（ai-check-r3 MV.3，plan 2026-09-11-0906-3，roadmap 点名义务）」blockquote（来源 mission + 点名义务 + 同族划界说明 + 不重写 01–19 承诺），全部在案〕
      - Skill: none
- [x] <Proof> 树自洽机械核验：`ls docs/lessons/` 编号 01–21 连续零孤儿；README 索引行 ↔ 实存文件一一对应；全 lessons 树 `git status` 变更面 = 本计划登记文件集（新 2 文件 + README，零生产代码触碰声明）。
      〔执行证据 2026-09-11：`ls docs/lessons/` = 01–21 连续 21 文件 + README.md，零孤儿零缺号；README §Lessons 索引行 04–21 + 头部推荐文件名 01–03 与实存文件一一对应；`git status --porcelain` 变更面恰 3 文件（M README + ?? 20/21 两新文件），零生产代码/脚本/模型触碰〕
      - Skill: none

Exit Criteria:

- [x] README 两行索引 + 提升裁决注记在案，编号连续零孤儿
      〔执行证据 2026-09-11：两行索引 + 提升裁决 blockquote 落盘；`ls` 机械核验 01–21 连续零孤儿（见上项执行证据）〕
- [x] 变更面核验通过（恰 3 文件），零生产代码/脚本触碰声明在案
      〔执行证据 2026-09-11：`git status --porcelain` = M docs/lessons/README.md + ?? docs/lessons/20-*.md + ?? docs/lessons/21-*.md，恰 3 文件零越界；后续收官写入面另含本计划勾选 + roadmap MV.3 行翻转 + 当日日志（mission-driver 步骤 3b/4b 义务，非生产代码），Closure Findings 变更面项由独立闭包审计复核〕

## Draft Review Record

- dispatch review #review-2026-09-09-210030-mission-driver-2026-09-11-0906-3-mv3-methodology-lessons-1-e22a5ed2 to opencode/glm-5.3-flash
- 2026-09-11：iteration 1，共识 accept #review-2026-09-09-210030-mission-driver-2026-09-11-0906-3-mv3-methodology-lessons-1-e22a5ed2

## Verification

> 本计划为纯文档沉淀面（Non-Goals：零生产代码/checker 脚本/基线文件改动）。按计划指南「无代码更改的计划删除验证命令门控并说明原因」：不适用 `mvn` build/test、compliance checker、CJK/i18n 门控；验证证据由 Phase 1~2 落盘的 lesson 20/21 正文（五要素/三步方案 + 实录对账数字 + 自检清单 + 互链划界）与 Phase 3 的机械核验（`ls` 编号连续零孤儿、README 索引行 ↔ 实存文件一一对应、`git status` 变更面恰 3 文件）承载。实录对账数字须与 `docs/testing/known-good-baselines.md` `ai-check-r3-mv1` 终态行/MI 终态行逐值一致（CAT-1 335→0 / CAT-2 204→0 / CAT-3 390→0 / CAT-4 1700→0 / 白名单 27 文件 / `--self-test` 13/13）。frontmatter `verify: [test]` 门控由独立闭包审计 visit 全 reactor `mvn test` 实跑绿承载（ledger 派生完成公式要求，见 pass 线）。

闭包审计确认（2026-09-11，独立闭包审计 visit）：

- Phase 1~3 全部执行项与 Exit Criteria 勾选 `[x]`，勾选注记含逐值证据——独立复核通过：lesson 20/21 实盘正读核验（五要素表 + CAT 终态四条对账链 + MI.8 门控抓真犯/快照误触实录 + 既有 checker 盲区补位 + 07/17/12/13/20 划界表 + 三段/三问两查自检清单；F15 反事实 + 三步方案 + 零争议实录），对账数字（CAT-1 335→0 / CAT-2 204→0 / CAT-3 390→0 / CAT-4 1700→0 / 白名单 27 = 批1 11+批2 16 / `--self-test` 13/13 / 快照 453→170 files）与 `ai-check-r3-mv1` 终态行 + MI 终态行 + MI.9 收官 pass 线逐值一致；README 两行索引 + 2026-09-11 提升裁决 blockquote 在案；`ls docs/lessons/` = 01–21 连续 21 文件 + README 零孤儿；两课互引数字同源零矛盾。
- 闭包 visit 全 reactor `mvn test` 实跑绿（run 2026-09-11-1403，独立闭包审计 visit 复跑）：BUILD SUCCESS exit 0（13:57 min，156 模块全 SUCCESS），Results 块聚合 4084/0/0/1（748 个 `Tests run:` 块合计 8168/0/0/2 = 模块行 × Results 块双计折半，零失败零错误，1 skipped=预存 accepted）——与 `ai-check-r3-mv1` 终态行逐位一致，零新增失败。本 visit `git diff --name-only HEAD~1` 确认零生产模块变更，install 门按增量指引不适用。

- pass test 2026-09-11-1403-closure exit=0

## Closure Findings

- [x] 范围内行为完成（lesson 20/21 落盘 + README 两行索引，Goals 三项全部落地；Phase 1~3 全部执行项与 Exit Criteria 勾选，实录对账数字与 known-good-baselines 终态行逐值一致）——闭包审计独立复核通过（见 Verification 确认节）
- [x] 相关文档对齐（README 索引 + 07/12/13/17 互链划界在案；lessons 树编号 01–21 连续零孤儿）——闭包审计 `ls`/实盘正读独立复核通过
- [x] 已运行验证（按 Verification 节：纯文档沉淀面，以机械核验注记为验证证据，构建/测试命令门控不适用——理由已注记；`verify: [test]` 由闭包 visit 全 reactor `mvn test` 实跑绿承载，见 pass 线）
- [x] 变更面核验（收官时 `git status` 复核写入面仅限 2 个新 lesson 文件 + `docs/lessons/README.md`，零生产代码/脚本触碰）——闭包审计复核：写入面另含本计划勾选/回执 + roadmap MV.3 行翻转 + 当日日志（mission-driver 步骤 3b/4b 义务，非生产代码），零越界
- [x] 无范围内项目降级为 deferred/follow-up（MG.1 项按 Non-Goals 归属 successor，属范围边界而非降级）——闭包审计复核 Deferred/Follow-up 段无隐藏缺陷
- [x] 独立草案审查已完成并记录（见 Draft Review Record）
- [x] 文本一致性已验证：frontmatter status、各 Phase 状态、Exit Criteria、Closure Findings 与 `docs/logs/` 条目一致——闭包审计复核：frontmatter `status: active` 保持（ledger 协议）、Phase 全 `[x]`、roadmap MV.3 行 `done` 注记 + 日志 `docs/logs/2026/09-11.md` MV.3 收官条目与实仓一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符——本节 9 项由独立闭包审计 visit（fresh session，CLOSURE_AUDIT 步骤）逐项核验后勾选
- [x] 结束证据存在于文件中（`## Closure` Status Note + Closure Audit Evidence 配对 dispatch/accepted 行在案）

## Closure

Status Note: 计划三 Phase（lesson 20 入课 / lesson 21 入课 / 索引对账与树自洽）全部执行项与 Exit Criteria `[x]` 且勾选注记含逐值证据；lesson 20 五要素 + CAT 终态四条对账链、lesson 21 三步方案 + F15 反事实 + 零争议实录落盘，对账数字与 `ai-check-r3-mv1` 终态行/MI 终态行/`cjk-baseline.md` §批注账逐值一致，两课互引同源零矛盾；README 两行索引 + 2026-09-11 提升裁决注记在案，lessons 树 01–21 连续零孤儿；写入面恰为 2 个新 lesson + README + 本计划账面 + roadmap MV.3 行翻转 + 当日日志，零生产代码/checker/基线触碰。闭包 visit 全 reactor `mvn test` BUILD SUCCESS exit 0（4084/0/0/1 = `ai-check-r3-mv1` 终态行逐位一致，零新增失败）。MG.1 依赖本项的阻塞随之解除。

Closure Gates（独立闭包审计 visit 逐项核验，2026-09-11，对应 Closure Findings 九项）：

- 范围内行为完成——Goals 三项全部落地且注记在案（Phase 1~3 全 `[x]`，lesson 20/21 实盘正读核验五要素/三步方案/自检清单/互链划界齐备）。
- 相关文档对齐——README 两行索引 + 提升裁决 blockquote；`ls docs/lessons/` = 01–21 连续零孤儿；两课引用口径逐值互查一致。
- 已运行验证——纯文档沉淀面，机械核验注记为验证证据（理由已注记）；frontmatter `verify: [test]` 由闭包 visit 全 reactor `mvn test` 实跑绿承载（见 pass 线）。
- 变更面核验——`git status` 复核写入面：2 新 lesson + README + 本计划账面 + roadmap + 日志，零生产代码/脚本/模型触碰。
- 无范围内项目降级为 deferred/follow-up——MG.1 项按 Non-Goals 归属 successor，非降级；Deferred/Follow-up 段零隐藏缺陷。
- 独立草案审查已完成并记录（Draft Review Record iteration 1 accept）。
- 文本一致性已验证——frontmatter `status: active`（ledger 协议保持）、Phase 勾选、Exit Criteria 全 `[x]`、Verification pass 线与 Closure 回执一致、roadmap MV.3 行 `done` 注记 + `docs/logs/2026/09-11.md` MV.3 收官条目一致。
- 结束审计由独立子代理（新会话，非执行者上下文）执行——Closure Findings 九项与本 Closure 节即该独立闭包审计产物，执行者未自我审计。
- 结束证据存在于文件中——Closure Audit Evidence + 勾选注记 + dispatch/accepted 回执对。

Closure Audit Evidence:

- Auditor / Agent: 独立闭包审计子代理（mission-driver CLOSURE_AUDIT 步骤，fresh session，模型 zhipuai-coding-plan/glm-5.3-flash）
- Evidence: 本 visit 独立复核记录——①lesson 20/21 实盘全文正读核验（五要素表/CAT 终态对账链/三步方案/F15 反事实/自检清单/划界表逐一在位）；②对账数字交叉核验：`docs/testing/known-good-baselines.md` `ai-check-r3-mv1` 终态行（CJK report CAT1..4=0/0/0/0 + strict PASS）与 MI.9 收官 pass 线（§WHITELIST 27 条目四要素 + `--self-test` 13/13）逐值一致；③README 索引行/提升裁决注记 + `ls docs/lessons/` 01–21 连续零孤儿机械核验；④`git status`/`git diff --name-only HEAD~1` 写入面核验（零生产模块变更，install 门按增量指引不适用）；⑤`plan-check.mjs --strict` 复跑 + 全 reactor `mvn test` 实跑绿 BUILD SUCCESS exit 0（13:57 min，Results 块聚合 4084/0/0/1 与 `ai-check-r3-mv1` 终态行逐位一致，run 2026-09-11-1403）。

- dispatch audit #audit-2026-09-11-1403-2026-09-11-0906-3-mv3-methodology-lessons-1-d6e07ea9 to 2026-09-09-210030-mission-driver models={exec:zhipuai-coding-plan/glm-5.3-flash,aud:zhipuai-coding-plan/glm-5.3-flash}
- accepted #audit-2026-09-11-1403-2026-09-11-0906-3-mv3-methodology-lessons-1-d6e07ea9：独立闭包审计**通过，零 Blocking**（单模型声明式降级诚实记录）——闭包 visit 全 reactor `mvn test` BUILD SUCCESS exit 0（13:57 min，零失败零错误，Results 块聚合 4084/0/0/1 = `ai-check-r3-mv1` 终态行逐位一致，run 2026-09-11-1403）；语义独立复核成立：①lesson 20/21 落盘且五要素/三步方案 + r3 实录对账数字（CAT-1 335→0/CAT-2 204→0/CAT-3 390→0/CAT-4 1700→0/白名单 27/`--self-test` 13/13）与 known-good-baselines 终态行/MI.9 收官 pass 线逐值一致；②README 两行索引 + 提升裁决注记在案、lessons 树 01–21 连续零孤儿；③Closure Findings 九项逐项核验后勾选（范围内行为/文档对齐/验证/变更面/无降级/草案审查/文本一致性/独立审计/结束证据）；④roadmap MV.3 行 `done` 翻转注记 + 当日日志 MV.3 收官条目在案（docs sync ✓）；⑤`plan-check.mjs --strict` passed=true 派生 completed；frontmatter `status: active` 依 ledger 协议保持；写入面零生产代码/脚本触碰。

Follow-up:

- 无非阻塞跟进项；MG.1 项（CJK 白名单裁决范式、R1b/R1d 三元断言不足新失败模式等）按 Non-Goals 归属 successor roadmap MG.1，非本计划 follow-up。
