---
status: active
mission: ai-check-r3
work-item: MG.1
group: "2026-09-11-1425"
verify: [test]
---

# 2026-09-11-1425-1 MG.1 新失败模式沉淀与终态复核（lessons/skills 提升裁决 + known-good-baselines 终态行复核 + 历史执行目录只读保留核证）

## Current Baseline

- **roadmap MG.1 义务（Deps: MV.3，已 done）**：①新失败模式沉淀——可复用模式提升 `docs/lessons/` / `docs/skills/`（roadmap 点名示例：CJK 白名单裁决范式）；②known-good-baselines 终态行复核；③历史执行目录只读保留。本计划为本批 N=1；MG.2（状态回写收官）依赖本计划，列本批 N=2。
- **MV.3 移交面**（plan `2026-09-11-0906-3` Non-Goals 显式归属本计划）：CJK 白名单裁决范式、R1b/R1d 三元断言新失败模式、known-good-baselines 终态行复核、历史执行目录只读盘点——四项均为本计划盘点输入。
- **lessons 现状（2026-09-11 实核）**：`docs/lessons/` 编号 01–21 连续零孤儿（20/21 由 MV.3 落盘），`README.md` 索引逐条一行摘要 + 四条提升裁决 blockquote（2026-08-20 / 08-28 ×2 / 09-11）。新编入课号顺延 **22+**。既有裁决体例：roadmap 点名必入 + 证据高频候选逐一三态裁决（新立 / 归并既有课边界扩展注记 / 低于阈值 watch-only 不入）+ 同族划界说明。
- **已知候选清单（盘点输入，非预设结论；均来自 r3 执行实录）**：
  - **R1b/R1d 三元断言不足**——checker 门控收尾断言只复核三条头条规则（R2b/R2c/R12a）而非 19 规则全表，R2.5 commit `48b57cd06` 引入的 R1b/R1d 漂移穿越 M2.5~MV.1 多轮收尾未被捕获，最终由 M2.9 收官按回填交接协议 successor `m29-r1b-r1d-baseline-adjudication`（plan `2026-09-11-0906-1`）全表复跑闭合。复现 ≥2 处（M2.5 登记、MV.1 披露）；与 lesson 07（基线漂移的建设期处置）同族需划界。
  - **CJK 白名单裁决范式**（roadmap 点名）——文件级 per-CAT 白名单四要素登记（文件/理由/owner doc 指针/裁决来源）+ 批注账单向对账；lesson 20 第五要素已含「白名单唯一豁免 + 批注账」，本候选预计归并 lesson 20 边界扩展，除非盘点发现独立于脚本门控的复用面。
  - **`_cases` 快照外科式文本变换**——受影响快照按旧值→新值仅消息/名称列外科重录、保留 `*` 通配符与 `@var:` 前缀、禁用 `force-save-output` 全量重录（会窄化通配符断言，MI.6 批 1 实证）；MI.6 批 1/批 2、MI.8 批 1/批 2、M2.5 多批复现（≥4 次），trap 面（全量重录）与正确面（外科变换）成对出现。
  - **姊妹会话并发写/构建干扰隔离**——同机多 agent 会话并发编辑 + 构建负载干扰下的全量验证经 `git clone` 钉住 HEAD 隔离重跑（2026-09-02 并发构建事故处置、2026-09-08 MI.9 收官验证两案，≥2 次）。
- **known-good-baselines 现状**：表尾终态行 `ai-check-r3-mv1`（2026-09-11，plan `2026-09-11-0457-2`）：install 156/156 SUCCESS + 全 reactor `mvn test` 4084/0/0/1 + compliance checker exit 0（R2b=242/R2c=1543/R12a=71）+ CJK report CAT1..4=0/0/0/0 + strict PASS + i18n PASS；其前依次为 2026-09-09 基线裁决行、2026-09-08 MI 终态行、2026-09-06 M0.6 收官行。R1b/R1d 漂移已由 plan `2026-09-11-0906-1` 闭合（R1b=0/R1d=15 机器块双写上调 + checker 全表 ≤ 机器块 exit 0）——终态行的 Known Failures 节 R1b/R1d 披露与该闭合的承接关系是复核点。
- **执行目录现状**：本轮唯一规范执行目录 `docs/audits/check/2026-09-06-1645-ai-check-r3/`（2026-09-11 实核 42 文件，其中 `ck-*-r3.md` 报告 28 份 + `ai-check-r3-index.md` + `m0-3/m0-4/m0-5/m1-17/m2-0` 等产物）；历史轮次目录（含 r1 `ai-check`/r2 `2026-08-28-2049-ai-check-r2/` 及更早）只读约定（roadmap 横切关注点 9）。
- **仓库现状（2026-09-11 1425 起草时实核）**：HEAD `f99890833`（MV.3 ledger 证据收官 commit），`git status --porcelain` 零输出（干净树）；脏面将仅由本批两份计划 untracked 文件构成。
- **剩余差距**：三项义务均未履行——提升裁决未做（候选未三态裁决、零落盘）、终态行未复核、执行目录只读保留未核证；本计划完成后 MG.2（roadmap 收官状态回写）解锁。

## Goals

- 候选模式三态裁决落盘：对 r3 全执行面候选（上述已知候选 + Phase 1 机械枚举补充）逐项裁决 **新立入课 / 归并既有课边界扩展注记 / watch-only 不入**，每项记录证据源、复现频次、划界说明；点名候选（CJK 白名单裁决范式、R1b/R1d 三元断言）必须显式裁决。
- 合格者落盘：新立候选入 `docs/lessons/`（编号顺延，体例对齐既有课）或 `docs/skills/`（仅可复用工作方法面，且注册表缺位时）；归并候选以边界扩展注记落入既有课；`README.md` 索引与提升裁决注记同步。
- known-good-baselines 终态行复核：`ai-check-r3-mv1` 行与 `docs/audits/compliance-baseline.md` 机器块（0906-1 闭合后 R1b=0/R1d=15/R2b=242/R2c=1543/R12a=71 双写一致）、`docs/audits/cjk-baseline.md` §批注账四条对账链、2026-09-08 MI 终态行交叉一致，复核证据在案。
- 历史执行目录只读保留核证：本轮目录 inventory 与本轮索引登记零缺零余；历史轮目录零删改（git 状态核证）。

## Non-Goals

- 零生产代码/checker 脚本/seed/ORM/api.xml 改动；known-good-baselines 既有行数值零改写（复核为只读对账；若发现漂移，登记并走独立处置路径，不就地改数值）。
- 不做 roadmap 状态翻转（owner/engine 依独立结束审计处置；本计划不触碰 Work Item Status 块）；不做 finding 语义复审；不重开 lesson 09/10、compliance baseline、CJK 白名单既有裁决。
- 不重述 r1/r2 mission 的方法学沉淀（各自收官流程所有）；不修改 lessons 01–21 正文（归并类裁决仅以边界扩展注记形式追加，注记需含裁决来源与日期；新课互链不重写旧课正文）。
- 不做 MG.2 项（roadmap 终态核对、backlog README 行更新、收尾日志——归本批 N=2）。

## Phase 1 — 候选模式机械盘点与三态裁决

> 统一类型：Decision（2 项 Decision）。
> Skill: none（roadmap MG.1 行指定 none；裁决阈值遵循 AGENTS.md 规则 11「模式重复出现足以证明复用才提升」，体例遵循 `docs/lessons/README.md` 既有提升裁决范式）
> Targets: 裁决记录落本计划勾选注记；落盘目标在 Phase 2 Targets 确定
> Prereqs: MV.3 done（plan `2026-09-11-0906-3`）

- [x] <Decision> 机械枚举 r3 全执行面候选并逐项三态裁决：盘点源 = 本轮执行目录 28 份 `ck-*-r3.md` + `cjk-baseline.md` §批注账七批行 + 28 份 ledger 计划的 Decision/Non-Goals/Follow-up 段 + `docs/logs/2026/09-06~09-11` 当日日志；对每候选记录证据源、独立复现频次（新立门槛 ≥2 次）、与既有 lesson 01–21 的同族划界（特别对 lesson 20 第五要素白名单面、lesson 07 基线漂移运维面、lesson 17 三路交叉发现面）；MV.3 移交的四项点名候选（CJK 白名单裁决范式 / R1b-R1d 三元断言 / 终态行复核 / 执行目录盘点）逐项显式裁决，零遗漏
      - Skill: none
      - **裁决记录（2026-09-11 执行注记）**——盘点源实核：28 份 ck 报告 ↔ 索引产物表逐名机械 diff 为空；`cjk-baseline.md` §批注账实存 4 批行（MI.6 批1/批2 + MI.8 批1/批2，CAT-1/2 链经 known-good-baselines MI 终态行承载）全行扫描；r3 ledger 计划 Decision/Non-Goals/Follow-up/successor 面全量 grep（roadmap 28 行工作项注记交叉）；`docs/logs/2026/09-02/06/07/08/09/10/11.md` 逐日扫描（09-02 为点名候选 ④ 证据源）。**逐候选三态裁决**：
      - ①【新立 → lesson 22】R1b/R1d 三元断言不足（点名）：复现 ≥2——Case A M2.5 `48b57cd06` 漂移穿越 M2.5~MV.1 四轮三元收尾（M2.9 全表同跑暴露，plan `2026-09-11-0457-1` Phase 3 收官阻塞披露 + `ai-check-r3-index.md` §M2.9 注记）+ Case B F2.9 `0a825a42a` closure 仅核 daoFor 漏 R12 import 面（`compliance-baseline.md:748` 行规注记 + plan `2026-09-09-2100-1:143`）；与 lesson 07 划界（07=漂移被发现后运维面 / 22=断言范围纪律——漂移如何被收尾漏掉）、与 lesson 20 划界（20=建门 / 22=用门）。
      - ②【归并 → lesson 20 边界扩展注记】CJK 白名单裁决范式（roadmap 点名）：第五要素已含「白名单唯一豁免 + 批注账」；增量面 = C1 `@Description` 专属 / C2 功能契约窄类逐簇裁决 + 27 文件四要素实绩 + 批注账对账闭环——与脚本门控耦合（白名单是 checker 豁免通道），无独立复用面，注记落 lesson 20 文末（裁决来源 plan `2026-09-07-0902-1` Phase 1 Decision + 本计划号 + 日期）。
      - ③【新立 → lesson 23】`_cases` 快照外科式文本变换：复现 ≥4——MI.6 批1（约 133 文件外科变换 + `force-save-output` 实测窄化通配符断言弃用，plan `2026-09-07-0902-1:79/198`）+ MI.6 批2（RFC 4180 重引号 + app-erp-all 3 case 外科同步，`2026-09-07-1715-1`）+ MI.7（跨模块 38 文件遗留修复，`2026-09-07-0902-2`）+ MI.8 批2（quoted-key/flow→block 等价重序列化 + PyYAML 语义树复验，`2026-09-07-1715-2`）；trap 面（全量重录）与正确面（外科变换）成对；与 lesson 06（生成产物禁令不覆盖测试资产）/ seed-data.md §快照重录双面义务（seed 变更才授权全量重录）划界。
      - ④【新立 → lesson 24】姊妹会话并发写/构建干扰隔离：复现 2——09-02 共享 `~/.m2` typo 态 `nop-dao` 污染 ClassNotFound（`docs/logs/2026/09-02.md`）+ 09-08 MI.9 姊妹 151 文件在制致 4 次全量伪失败 → `git clone` 钉 HEAD `6c495dc2e` 隔离重跑全绿（`docs/logs/2026/09-08.md` + `mi9-closure-verification.md` + known-good-baselines MI 行 Git State 披露，日志自证「与 09-02 在案并发构建事故同型」）；与 lesson 05（单点失败归因）/22（断言范围）划界。
      - ⑤【非候选（履行性义务）】MV.3 移交的 known-good-baselines 终态行复核 + 历史执行目录只读盘点：验证性义务而非失败模式（无复现、无缺陷形态），由本计划 Phase 3 两项 Proof 落实，不立项。
      - ⑥【watch-only ×6 → Deferred But Adjudicated 节】validate:flux 325 variant 外部漂移（successor: nop-chaos-flux dist 基线裁决在案）/ 磁盘资源耗尽验证截断（09-07 temp 43G 假绿 + 09-11 ENOSPC，计数聚合对账纪律已被 lesson 24 协议要素 4 吸收，独立课门槛不足）/ surefire 汇总 vs Results 块聚合计数口径差（known-good-baselines 注记 + M1.17 裁决承载）/ 墙钟毫秒竞态 flaky（`docs/bugs/2026-08-25-frozen-clock-millis-testclock-displacement.md` 专文承载 + mfg2-028-r3 延伸）/ I*Biz 代理方法分发需 install 刷新产物（M2.5 单例 <2）/ CLOSURE_SCRIPT_CHECK ledger 结构缺陷（M2.9+MV.2 两案，修复协议由计划指南 closer 规则承载）。
      - ⑦与 lesson 01–21 零重复立项核证：01-05/08-16/18/19 无叠面（逐课扫描）；06/07/17/20 划界见各新课划界节；lesson 21 成文先例被 ② 注记引用不重立。
- [x] <Decision> 落盘目标裁决：新立候选确定目标编号（22+ 顺延）与 kebab slug；判定「方法面可复用且 skills 注册表缺位」的候选确定 skills 文件名与注册表行；watch-only 候选登记不入理由（本计划 Deferred But Adjudicated 节承载）；零新立亦须记录零提升裁决与证据（诚实空面合法）
      - Skill: none
      - **落盘目标注记（2026-09-11）**：新立 3 课——`docs/lessons/22-closure-assertion-scope-full-gating-table.md`（前轮中断执行已落盘候选稿，本执行实核其 Case A/B 事实与源注记逐值一致后保留：`compliance-baseline.md:748/762-773` + roadmap M2.9 行 + `2026-09-09-2100-1:143`）+ `docs/lessons/23-snapshot-surgical-text-transform-no-full-rerecord.md` + `docs/lessons/24-concurrent-session-interference-isolated-full-verification.md`（本次新写）；归并 1——lesson 20 文末边界扩展注记；联动边界扩展 1——lesson 07 文末「全表断言」注记（兑现 lesson 22 划界节「07 课已附 2026-09-11 边界扩展注记」前向引用，不重写正文）；索引同步——`docs/lessons/README.md` §Lessons 追加 22/23/24 三行 + 表尾 MG.1 提升裁决 blockquote。**skills 裁决：零提升**——快照治理与并发隔离为项目验证纪律（lessons 面承载），非跨任务可复用提示方法；skills 注册表无缺位（基线漂移裁决已有 `compliance-baseline-drift-adjudication-prompt.md`），零 skills 文件与注册表行变更。

Exit Criteria:

- [x] 裁决记录逐候选在案（证据源 + 频次 + 三态结论 + 划界说明），点名候选零遗漏，与 lessons 01–21 零重复立项
      - 注记：见 Phase 1 第 1 项裁决记录 ①~⑦——MV.3 移交四项（①②⑤）+ 机械枚举补充（③④⑥）逐项三态裁决，点名候选（CJK 白名单范式 / R1b-R1d 三元断言）显式裁决零遗漏。
- [x] Phase 2 落盘清单确定（目标文件路径逐一列明），或零提升裁决已记录证据
      - 注记：落盘清单 = `docs/lessons/22-*.md`（实核保留）+ `23-*.md` + `24-*.md` + `docs/lessons/README.md` + `07-*.md`/`20-*.md` 边界扩展注记；skills 零提升裁决已记录（注册表无缺位）。

## Phase 2 — 合格模式落盘（lessons / skills + 索引同步）

> 统一类型：Add（2 项 Add）。
> Skill: none
> Targets: `docs/lessons/22-*.md`（编号以 Phase 1 裁决为准）、`docs/lessons/README.md`、（视裁决）`docs/skills/` 新文件 + `docs/skills/README.md` 注册表行
> Prereqs: Phase 1 完成（落盘清单已确定）

- [x] <Add> 新立候选撰写入课：体例对齐既有课（blockquote 来源/适用场景/失败模式 + 问题定义 + 方案要素表 + r3 案例实录对账数字 + 自检清单 + 同族划界节）；实录数字与 `ai-check-r3-mv1` 终态行 / 批注账 / 源计划注记逐值一致；归并候选在既有课文末追加边界扩展注记（裁决来源 + 日期 + 新案例要点，不重写正文）
      - Skill: none
      - **执行注记（2026-09-11）**：lesson 22 实核保留（来源 blockquote 指向 MV.3 移交与本计划；Case A 数字 R1b=1>0/R1d=15>14/`48b57cd06`/0906-1 闭合 R1b=0/R1d=15 双写/R2b=242/R2c=1543/R12a=71 与 `compliance-baseline.md` 机器块、L762 M2.9 披露注记、L764-773 0906-1 裁决注记逐值一致；Case B 与 `compliance-baseline.md:748` 行规注记一致）。lesson 23/24 新写（体例对齐 20/21：来源/适用场景/失败模式 blockquote + 核心论点 + 要素表 + r3 案例实录 + 划界表 + 自检清单 + 关联；实录数字与源计划/批注账/日志逐值一致——23 课 133 文件/181 行 56 文件/209 行 88 文件/38 文件/3 case 70-0-0-1；24 课 `6c495dc2e`/33:00 min/4 次伪失败/151 文件姊妹面）。归并注记落 lesson 20 文末（裁决来源 `2026-09-07-0902-1` Phase 1 Decision + 日期 + C1/C2 窄类要点 + 27 文件四要素实绩）+ lesson 07 文末（全表断言升级 + `48b57cd06` 穿越实录），两条注记均不重写正文。
- [x] <Add> `docs/lessons/README.md` 索引同步：新课逐课一行摘要（体例对齐 04–21 行）+ 表尾提升裁决 blockquote（来源 mission ai-check-r3 MG.1 + 本计划号 + 同族划界说明 + 不重写既有课承诺）；skills 提升候选（若有）落文件 + 注册表行（使用场景/不使用场景/必需输入/预期输出四列齐备）
      - Skill: none
      - **执行注记（2026-09-11）**：README §Lessons 追加 22/23/24 三行（体例对齐 04–21：`NN-slug.md` — **标题**：一行摘要含实录与划界）+ 表尾追加「2026-09-11 提升裁决（ai-check-r3 MG.1，plan `2026-09-11-1425-1`…）」blockquote（MV.3 移交四项逐一裁决 + 机械枚举补充两项新立 + watch-only 六项指针 + skills 零提升 + 不重写既有课承诺）。skills 提升候选为零（Phase 1 裁决），零注册表行变更。

Exit Criteria:

- [x] 落盘文件与索引一一对应：`ls docs/lessons/` 编号连续零孤儿；README 索引行 ↔ 实存文件机械核对一致
      - 注记（2026-09-11 机械核验）：`ls docs/lessons/` = 01–24 连续零孤儿（25 文件含 README）；README §Lessons 索引行 04–24 ↔ 实存文件 04–24 机械核对一一对应（grep 提取 22 个 `` NN-*.md `` 逐一 `ls` 命中）；01–03 既有文件无索引行为本计划起草前即存在的预存状态（MV.3 收官时同态），本计划零触碰零恶化，登记为预存观察不阻塞。
- [x] `git status` 变更面 = Phase 1 裁决的登记文件集 + 本计划账面，零生产代码/checker/seed 触碰（机械过滤核证注记在案）
      - 注记（2026-09-11 `git status --porcelain` 机械核验；含独立闭包审计 Minor-1 处置后修订）：`M docs/lessons/07-*.md` + `M docs/lessons/20-*.md` + `M docs/lessons/README.md` + `?? docs/lessons/22-*.md` + `?? docs/lessons/23-*.md` + `?? docs/lessons/24-*.md` + 本计划与姊妹 `2026-09-11-1425-2` 两 untracked 计划文件 + `M docs/logs/2026/09-11.md`（本计划 AGENTS.md 规则 8 日报条目，注记落盘后写入，属本计划账面）——恰 = Phase 1 落盘清单 + 本计划账面（含自身日志），`module-*`/`app-erp-all`/`tools/`/`_init-data`/`*.orm.xml`/`*.api.xml` 零命中（机械过滤 `ZERO_PRODUCTION_CODE_CHANGES` 成立）。

## Phase 3 — known-good-baselines 终态行复核 + 执行目录只读保留核证

> 统一类型：Proof（2 项 Proof）。
> Skill: none
> Targets: 复核证据落本计划勾选注记（只读对账，零文件改写）
> Prereqs: 无（与 Phase 1/2 无依赖，可并行）

- [x] <Proof> `ai-check-r3-mv1` 终态行四面对账：①`docs/audits/compliance-baseline.md` 机器块 ↔ 人类可读表双写一致（R1b=0/R1d=15/R2b=242/R2c=1543/R12a=71 + 其余 14 规则，plan `2026-09-11-0906-1` 闭合后口径）；②终态行 Known Failures 节 R1b/R1d 披露 ↔ 0906-1 闭合承接关系注记一致；③`docs/audits/cjk-baseline.md` §批注账四条对账链（CAT-1 335→0 / CAT-2 204→0 / CAT-3 390→0 / CAT-4 1700→0、白名单 27 文件）↔ 终态行 Commands Passed 数字逐值一致；④MI 终态行（2026-09-08）↔ 终态行 CJK 数字交叉一致；漂移即登记（不就地裁决，走独立处置）
      - Skill: none
      - **对账证据（2026-09-11 只读复核，零文件改写）**：
      - ①机器块（`compliance-baseline.md:480-498`）↔ 人类可读表（:17-35）**19/19 逐行一致**：R1a=0/R1b=0/R1c=0/R1d=15/R2a=34/R2b=242/R2c=1543/R2d=38/R3=5/R4=0/R5=0/R6=2/R7=0/R8=0/R10=14/R11=0/R12a=71/R12b=66/R12c=42——0906-1 闭合后口径（R1b=0 Fix 回落 + R1d=15 raise 双写，L773 checker 复跑 exit 0 全表 ≤ 机器块）。
      - ②终态行 Known Failures 节披露「R1b=1>机器块 0 / R1d=15>机器块 14 预存漂移（源 M2.5 `48b57cd06`，机器块未放宽）+ successor `m29-r1b-r1d-baseline-adjudication` 承接」↔ `compliance-baseline.md` §R1d 基线上调注记 + R1b Fix（plan `2026-09-11-0906-1`，L764-773：R1b Fix `updateEntity(sn,null,context)` 回落=0 + R1d 14→15 per-site raise 双写 + 全表 exit 0）——披露→承接→闭合三段关系一致；终态行为登记时点历史记录（当时机器块 R1d=14），闭合后现值由 0906-1 注记与本行①承载，无矛盾。
      - ③批注账四链逐值复算：CAT-1 335→0 = MI.2 93 + MI.3 165 + MI.4 77（93+165+77=335 ✓）；CAT-2 204→0 = MI.5a 136 + MI.5b 68（=204 ✓）；CAT-3 390→0 = MI.6 批1 181 行/56 文件 + 批2 209 行/88 文件（=390 ✓，批2 16 域逐域计数求和 209 ✓）；CAT-4 1700→0 = MI.8 批1 382 行/26 文件 + 批2 1318 行/82 文件（=1700 ✓，批2 17 域逐域计数求和 1318 ✓ 且与 SNAPSHOT domains 块 17 域逐值一致）；白名单 27 = 批1 11（fin 5/ast 4/cs 2）+ 批2 16（hr 1/aps 2/inv 3/mfg 1/prj 1/pur 1/sal 1/common 2/ct 1/mnt 1/qa 1/md 1）✓；终态行 Commands Passed「CJK report CAT1..4=0/0/0/0 + strict PASS（totals 0/0/209/1318 = 冻结快照残余）」逐值一致。
      - ④MI 终态行（2026-09-08）↔ 终态行：CJK report CAT1..4=0/0/0/0 两行逐值一致 + `--strict` PASS 双行一致 + 白名单 27/27 一致；compliance 面差异仅 R2c 1542→1543 = M2.9（plan `2026-09-11-0457-1`）裁决性上调（终态行 Known Failures/Notes 已显式登记「N=1 上调后基线」，非漂移）。**四面零漂移，无需独立处置登记**。
- [x] <Proof> 执行目录只读保留核证：本轮目录 42 文件 inventory ↔ `ai-check-r3-index.md` 登记零缺零余（28 份 ck 报告逐名核对）；历史轮目录（`docs/audits/check/` 下 r1/r2 及更早条目）`git status` 零删改核证（只读保留，不归档不移位——AGENTS.md §14 未经人工批准不动 archive）
      - Skill: none
      - **核证证据（2026-09-11）**：①本轮目录 `ls` = **42 文件**与计划 Current Baseline 一致；28 份 `ck-*-r3.md` 目录清单 ↔ 索引产物表登记行机械 diff **为空**（`ls | grep ck-*-r3.md | sort` vs 索引提取 28 名，双向零缺零余）。非报告 14 文件登记面分解：11 文件登记于索引产物表（index 自身 + m0-3 ×6 + m0-4 + m0-5）+ 2 文件登记于索引 §M2.9/§MV.2 注记节（`m2-0-family-adjudication.md`/`m1-17-coverage-matrix-final.md`）+ 3 文件登记于持久证据面（`mi9-closure-verification.md` → known-good-baselines MI 行 Evidence 列；`m0-6-mvn-test.log`/`m0-3-mvn-test-phase3.log` 为 gitignored 原始 run 证据，已知 ignore 规则 `docs/audits/check/**/*mvn-test*.log` 承载、known-good-baselines M0.6 行登记）——**观察项**：索引产物表对 tracked 文件 `mi9-closure-verification.md` 缺显式行（其持久登记在 known-good-baselines MI 行），按 Phase 3 只读纪律不就地补行，移交 owner/MG.2 复核（`successor: ai-check-r3-owner-review trigger:MG.2 状态回写收官时复核本轮索引产物表补行必要性`）。②历史轮目录 `git status --porcelain docs/audits/check/` **零输出**——r1（`ck-*.md` 平铺 + `ai-check-index.md`）/r2（`2026-08-28-2049-ai-check-r2/`）/entity-state-machine 各轮目录零删改零移位，只读保留核证成立；全树零归档操作（AGENTS.md §14 守约）。

Exit Criteria:

- [x] 四面对账证据逐值在案（或漂移已显式登记并命名处置路径），终态行复核结论明确
      - 注记：四面逐值对账证据见 Proof ①~④ 注记——双写 19/19 一致、披露↔承接↔闭合三段一致、批注账四链求和逐值闭合、MI 行交叉一致；零漂移零独立处置登记，终态行复核结论 = **与三对照面（机器块/批注账/MI 行）交叉一致成立**。
- [x] 本轮目录零缺零余 + 历史目录零删改核证注记在案
      - 注记：28 报告逐名机械 diff 为空 + 42 文件登记面全分解（含 1 项索引产物表观察项已按只读纪律登记 successor trigger）+ `git status docs/audits/check/` 零输出核证，见 Proof ② 注记。

## Draft Review Record

- dispatch review #review-2026-09-09-210030-mission-driver-2026-09-11-1425-1-mg1-lessons-skills-consolidation-1-03b5ee70 to opencode/glm-5.3-flash
- 2026-09-11：iteration 1，共识 accept #review-2026-09-09-210030-mission-driver-2026-09-11-1425-1-mg1-lessons-skills-consolidation-1-03b5ee70

## Verification

> 本计划为纯裁决/沉淀面（Non-Goals：零生产代码/checker 脚本/seed/ORM/api.xml 改动；known-good-baselines 既有行只读对账零改写）。按计划指南「无代码更改的计划删除验证命令门控并说明原因」：不适用 `mvn` build/test、compliance checker、CJK/i18n 门控；验证证据由 Phase 1~2 落盘面机械核对（`ls docs/lessons/` 编号连续零孤儿、README 索引行 ↔ 实存文件一致、`git status` 变更面 = Phase 1 登记文件集 + 本计划账面）与 Phase 3 只读对账勾选注记承载。Phase 3 复核若发现漂移，按计划登记独立处置路径，不就地改数值。

- 结构核对（2026-09-11 closure 复核注记，ledger 计数域外以平列表记）：Phase 1 两项 Decision + 两 Exit、Phase 2 两项 Add + 两 Exit、Phase 3 两项 Proof + 两 Exit 全 `[x]`，注记齐备（裁决记录 ①~⑦ / 落盘清单 / 机械核验 / 四面对账 / inventory 分解）。
- 数字一致性核对（2026-09-11）：lesson 22 Case A/B 数字 ↔ `compliance-baseline.md:748/762-773` + roadmap M2.9 行逐值一致；lesson 23 四批实录数字 ↔ 批注账/源计划注记一致；lesson 24 两案数字 ↔ 09-02/09-08 日志 + known-good-baselines MI 行一致；lesson 20/07 边界扩展注记数字（27 文件/四链/CAT 终态）↔ 批注账与 MI 终态行一致；零矛盾。

- pass test 2026-09-11-1425-1-mg1 exit=0
  - 全 reactor `mvn test` BUILD SUCCESS exit 0（13:58 min，live log Results 块聚合 41 块 = **4084 tests / 0 failures / 0 errors / 1 skipped**，与 `ai-check-r3-mv1` 终态行逐位精确一致，零新增失败；本计划纯文档沉淀面，零生产代码/checker/seed/ORM/api.xml 写入——按 Verification 前言 compliance/CJK/i18n 门控不适用，`mvn test` 为驱动步骤 3a 加跑的加成证据）；原始日志 `/tmp/opencode/mg1-mvn-test.log`（易逝，聚合数字已固化本节）。
- pass test 2026-09-11-1425-1-mg1-closure-visit exit=0
  - 闭包审计 visit 复跑（2026-09-11 CLOSURE_AUDIT）：`mvn clean install -DskipTests` BUILD SUCCESS exit 0（01:41 min，156/156 模块全绿）+ 全 reactor `mvn test` BUILD SUCCESS exit 0（13:58 min，Results 块聚合 41 块 = 4084 tests / 0 failures / 0 errors / 1 skipped，与 `ai-check-r3-mv1` 终态行逐位一致零新增失败）；原始日志 `/tmp/opencode/mg1-closure-mvn-test.log`（易逝，聚合数字已固化本节）。

## Closure

Status Note: MG.1 三项义务全部落地——①新失败模式沉淀：MV.3 移交四项点名候选 + Phase 1 机械枚举补充候选逐项三态裁决落盘（新立 lesson 22/23/24 + 归并 lesson 20 边界扩展注记 + watch-only 六项登记 Deferred But Adjudicated + skills 零提升裁决），lesson 07 联动边界扩展注记兑现新课前向引用；②known-good-baselines 终态行复核：四面对账逐值闭合零漂移（双写 19/19、披露↔承接↔闭合、批注账四链求和、MI 行交叉）；③历史执行目录只读保留核证：42 文件 inventory + 28 报告零缺零余 + `git status docs/audits/check/` 零删改。纯裁决/沉淀面零生产代码写入，全 reactor `mvn test` 4084/0/0/1 与 `ai-check-r3-mv1` 终态行逐位一致；独立子代理（新会话）闭包审计 `passes closure audit` 零 Blocking/Major（2 Minor：Minor-1 变更面注记已修订处置，Minor-2 为执行者预登记观察项确认）。MG.2（roadmap 状态回写收官）解锁。

Closure Gates（2026-09-11 closure 复核注记：ledger 协议下门控为核对叙述，计数域外以平列表记，全项核实通过）:

- 范围内行为完成（Goals 三项义务全部落地且注记在案；零提升亦须零提升裁决证据在案）
      - 注记：三项义务证据见 Phase 1~3 勾选注记 + Verification 勾选注记；skills 零提升裁决在 Phase 1 第 2 项注记（注册表无缺位）。
- 相关文档对齐（lessons/skills 落盘与 README 索引同步；不重写既有课正文承诺在案）
      - 注记：lesson 22/23/24 + README 三行索引 + MG.1 提升裁决 blockquote + 07/20 边界扩展注记（`git diff --stat` 9 insertions 0 deletions 附录式，审计项 5 实证 append-only）；不重写承诺见 README blockquote 与两条注记文末。
- 已运行验证（按 Verification 节：纯裁决/沉淀面，以机械核对与只读对账注记为验证证据，构建/测试命令门控不适用——理由已注记）
      - 注记：机械核对（ls 连续零孤儿 / README↔文件一一对应 / 28 报告 diff 为空 / 变更面过滤）+ 只读四面对账注记在案；驱动步骤 3a 加跑全 reactor `mvn test` 4084/0/0/1 exit=0（pass 线在案）。
- 无范围内项目降级为 deferred/follow-up（watch-only 候选登记 Deferred But Adjudicated 属 Phase 1 裁决产物，非降级）
      - 注记：六项 watch-only 均为 Phase 1 三态裁决的「不入」结论（复现 <2 或既有承载），非范围内义务降级；Goals 三项义务零缩水。
- 独立草案审查已完成并记录（见 Draft Review Record）
      - 注记：dispatch review #review-2026-09-09-210030-mission-driver-2026-09-11-1425-1-mg1-lessons-skills-consolidation-1-03b5ee70，iteration 1 共识 accept（2026-09-11）。
- 文本一致性已验证：frontmatter status、各 Phase Status、Exit Criteria、Closure Gates 与 `docs/logs/` 条目一致
      - 注记：frontmatter `status: active` 保持（ledger 协议，未写 completed）；三 Phase 无独立 Status 行（勾选即完成信号）；Exit/Closure 勾选与注记一致；`docs/logs/2026/09-11.md` MG.1 条目与计划 claims 一致（独立审计项 14 PASS 实证）。
- 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
      - 注记：fresh-session 独立子代理 task `ses_f6fd35fd2ffeTLJzT7VI2xu0Z3`（general agent，read-only brief，15 项核查清单逐项自行实测）——15/15 PASS。
- 结束证据存在于文件中（Closure Audit Evidence + 勾选注记）
      - 注记：见下 Closure Audit Evidence + 各 Phase/Verification 勾选注记。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话，general agent，与执行者无共享上下文）task id `ses_f6fd35fd2ffeTLJzT7VI2xu0Z3`
- Evidence: VERDICT **`passes closure audit`**（2026-09-11）——15 项核查清单 15/15 PASS（计划结构/勾选注记/frontmatter active、lessons 01–24 连续零孤儿、README 三行索引 + MG.1 blockquote 五要素、07/20 边界扩展注记 append-only 9 insertions 0 deletions、lesson 22 事实链 `compliance-baseline.md:748/762-773` 实证、lesson 23 事实链 `2026-09-07-0902-1:79` + 批注账实证、lesson 24 事实链 09-02/09-08 日志实证、四面对账 19/19 + 四链求和 + MI 行交叉、42 文件 + 28 报告 diff 为空 + check 树零删改、变更面零生产代码、pass 线 4084/0/0/1 与终态行逐位一致、Deferred 六项、日志条目一致、roadmap MG.1 pre-flip 态确认）；Blocking 0 / Major 0 / Minor 2（Minor-1 变更面注记未含本计划日志条目——已修订 Phase 2 Exit 2 注记处置；Minor-2 执行者预登记的 `mi9-closure-verification.md` 索引产物表观察项——独立确认非阻塞）。dispatch id `#audit-2026-09-11-1810-mg1-closure-1`（本回执为任务系统回执，nonce 以 task id 谱系承载）。

Mission Ledger Receipt（02-rule-law §4.1；exec/aud 同模型 = 单模型降级，如实登记）:

- dispatch audit #audit-2026-09-09-210030-mission-driver-2026-09-11-1425-1-mg1-lessons-skills-consolidation-1-7c475564 to ses_f6fcdc2fbffe4v5lVA9UCqkc3c models={exec:zhipuai-coding-plan/glm-5.3-flash,aud:zhipuai-coding-plan/glm-5.3-flash}
- accepted #audit-2026-09-09-210030-mission-driver-2026-09-11-1425-1-mg1-lessons-skills-consolidation-1-7c475564：闭包审计通过——ledger 12 项全 `[x]` 零 unchecked + lessons 01–24 连续零孤儿（README 索引/07/20 注记/日志条目 live 复核一致、四面对账零漂移、check 树零删改）；闭包 visit 全 reactor `mvn clean install -DskipTests` 156/156 exit 0 + `mvn test` 4084/0/0/1 exit 0 与 `ai-check-r3-mv1` 终态行逐位一致，`plan-check.mjs --strict` 通过。

Follow-up:

- 无（watch-only 六项与索引产物表观察项均已登记 Deferred But Adjudicated / successor trigger，非本计划缺陷面）。

## Deferred But Adjudicated

### 低于提升阈值的 watch-only 候选（Phase 1 裁决产物，2026-09-11 回填）

- Classification: `watch-only residual`
- Why Not Blocking Closure: Phase 1 三态裁决中未达新立门槛（复现 <2 次或既有课已覆盖）的候选登记于此，附触发条件（同类模式再度复现时重新盘点）；零候选则登记零空面
- Successor Required: `no`（触发条件：后续 mission/审计再次复现同型模式时，由当轮沉淀义务重新盘点）

**逐项登记（六项，Phase 1 裁决 ⑥）**：

1. **validate:flux 325 条 `variant=primary` 外部漂移**——nop-chaos-flux dist 基线裁决已在案（`cjk-baseline.md` §批注账 MI.8 批1 行 successor 登记），既有对账协议（逐切片 100% variant 归类）稳定运转，非新失败模式；trigger: successor 裁决落地前每切片继续对账归类。
2. **磁盘资源耗尽致验证截断**——复现 2（09-07 temp 43G 堆积致 surefire 截断**误报 BUILD SUCCESS**（实跑仅 24/102 类，当轮捕获修复）+ 09-11 ENOSPC 中断后清理重跑）；计数聚合对账纪律已被 lesson 24 协议要素 4 吸收，独立成课门槛不足；trigger: 再复现一次（≥3）时盘点「全量验证完整性自证（模块/类计数对账）」独立课。
3. **surefire 汇总 vs 运行输出 Results 块聚合计数口径差**——多次复现（3991/4006/4069/4084 口径差 M1.17 已裁决「发现模式差异」），known-good-baselines 注记承载；trigger: known-good-baselines 登记口径再分歧时重新盘点。
4. **墙钟毫秒竞态 flaky（`@var` 秒界劈裂）**——`docs/bugs/2026-08-25-frozen-clock-millis-testclock-displacement.md` 专文承载 + mfg2-028-r3 延伸实证，已有持久承载不重复立项；trigger: 新增非时钟型快照竞态时并课盘点。
5. **I*Biz 代理方法分发需 install 刷新产物**——M2.5 单例（BizProxyFactoryBean 平台机制注记），<2 不立项；trigger: 二次复现时并入平台机制注记类盘点。
6. **mission-driver CLOSURE_SCRIPT_CHECK ledger 结构缺陷**——M2.9/MV.2 两案均由 closer visit 按计划指南最低规则修复权处置，修复协议已有 owner 承载（`docs/plans/00-plan-authoring-and-execution-guide.md`）；trigger: 三次以上复现时盘点「ledger 文法防御性校验」方法课。

### 非候选登记（履行性义务，Phase 1 裁决 ⑤）

- **known-good-baselines 终态行复核 + 历史执行目录只读盘点**（MV.3 移交后两项）：验证性义务而非可复用失败模式，由本计划 Phase 3 两项 Proof 履行完毕，不入 lessons/skills，不入 watch-only。
