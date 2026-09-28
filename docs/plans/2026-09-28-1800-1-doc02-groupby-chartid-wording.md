# 2026-09-28-1800-1 DOC-02 1418-4 计划「GROUP BY chartId」措辞归一

> Plan Status: completed
> Last Reviewed: 2026-09-28
> Source: `docs/backlog/perf-ux-debt-consolidation-roadmap.md` M2 DOC-02（终版靶点经 roadmap 级草案审查 F-3 重定：分析报告 §2.4 无失准表述无需改动；失准措辞位于 1418-4 计划文件四处）
> Related: `docs/plans/2026-09-28-1418-4-a9-addendum-unbounded-queries.md`（被归一对象，已 completed——1418-4 结束审计 round 2 非阻断残留「计划 Goals F-4 孪生与 Phase 1 条目的字面 GROUP BY chartId 措辞留待 doc-only 归一」）；`docs/plans/2026-09-28-1418-4…md` 执行期留痕（执行形态权威记载）
> Audit: required

## Current Baseline

- **事实背景**：F-4（`ErpQaDashboardBizModel.countOutOfControlCharts` 及孪生 `countInadequateCapabilityCharts`）的执行形态是 **chartId 单列投影（减传输列）+ `notNull("chartId")` 过滤 + 内存 HashSet 去重**——纯维度投影在 Nop ORM 中仍被强制注入主键维度（无 SQL GROUP BY，每行一结果），首轮 `size()` 直数行数 3≠2 被护栏捕获后照 ast 先例修正。执行形态权威记载三处：1418-4 执行期留痕、Goals F-4 主条目（:22，措辞已正确）、分析报告 §2.2 A9 补遗回填。
- **失准措辞站点（HEAD 逐一 grep 实证，4 处，全在 `docs/plans/2026-09-28-1418-4-…md`）**：
  - :16 机制先例行——「①维度分组投影聚合（`sumBalanceTotalCost`/ast `loadAssetIdsWithExecutedDepreciationInPeriod`——F-2/F-4 直接同款）」：ast 先例实为单列投影+内存去重，且 F-2（(projectId,userId) 分组投影）与 F-4（单列+内存去重）形态并不相同，「F-2/F-4 直接同款」一语双失；
  - :23 Goals F-4 孪生行——「同法 `GROUP BY chartId` 投影」；
  - :58 Phase 1 Fix F-4 条目——「`GROUP BY chartId` 投影 + `notNull(...)` 过滤…→ `size()` 即 distinct 计数」；
  - :62 Phase 1 Fix F-4 孪生条目——「同法 `GROUP BY chartId` 投影」。
- **失准/相关字面站点全集（草案审查 iteration 1 grep 实证：1418-4 全文「GROUP BY」10 处——:22/:23/:33/:58/:60/:62/:86/:96/:129/:139）**：
  - 本批归一面 = roadmap 指名 4 处（:16/:23/:58/:62）；
  - **:22 的 `ErpCsQualityDashboardBizModel.java:319` 原语引用已失效**（1418-4 Closure round-1 整改新增 buildClosedTicketQuery 致行号漂移，notNull 现位于 :331）——授权面外不动，登记为同族残留供溯源；
  - **:139（Closure Audit Evidence round 2）含字面「GROUP BY chartId」**——系审计引文（引用该残留措辞本身），闭包证据保护不改（B-1 裁决：grep 断言口径=4→1）；
  - :22（否定式「无 SQL GROUP BY」）/:60（F-2 真分组投影）/:86（F-1 被否决替代方案引文）/:96（留痕否定式+失效交叉引用，见下）为正确/历史上下文；
  - **:33 与 :129（D-17 触发条件描述）同源失准**——「改 GROUP BY scheduleId 投影（…批次 2 ast 先例）」把同一误归属复制到 mnt 修法（活代码证实 ast 先例为单列投影+内存去重；纯维度投影将命中 :96 记载的主键注入坑）——**授权面外**（DOC-02 owner 面仅 chartId 四处），不得改亦不得封为「正确上下文」，改判「授权面外同源残留」入 Deferred（M-1）；
  - **:96 留痕结尾括注为失效交叉引用（M-2）**：「报告 §2.4 对该先例的『维度投影』描述建议随 doc-only 批校正」——报告 §2.4 经 roadmap F-3 实裁无该表述，该括注指向不存在的文本；处置裁决=留痕段不动（授权面外），在 Deferred 显式登记 roadmap 靶点偏差。
- **相邻残留（本批实核发现，显式登记不扩scope）**：`docs/plans/2026-09-27-0325-2-…md` Phase 5 留痕区 :203 ast 行「GROUP BY assetId 维度投影去重」与 ast 代码注释（`ErpAstDashboardBizModel.java:278`）同源措辞残留——授权面外（roadmap F-3 枚举未含），Deferred 登记。
- roadmap 硬约束核对：本批纯 doc-only 文本归一，不改业务语义、不改任何 checked 状态、不改非目标文件。

## Goals

- `docs/plans/2026-09-28-1418-4-…md` 四处措辞归一至执行形态（**仅措辞，不动 checked 状态/结构/其他段落**；:58/:62 为历史执行条目，归一时保留原条目语义仅更正技术形态描述；四处替换文本逐字钉死——m-1）：
  - :16 → 「①**维度分组投影聚合**（`sumBalanceTotalCost` warehouseId 维度分组 SUM+内存合计 / F-2 的 (projectId,userId) 分组投影）；ast `loadAssetIdsWithExecutedDepreciationInPeriod` 与 F-4 实为 chartId/assetId **单列投影+内存 HashSet 去重**（平台对纯维度投影仍注入主键维度，详见 1418-4 执行期留痕）——F-2 与 F-4 形态不同」；
  - :23 → 「同法 chartId 单列投影+内存 HashSet 去重（见 F-4 主条目执行形态）」；
  - :58 → 「chartId 单列投影（减传输列）+ `notNull("chartId")` 过滤 + 内存 HashSet 去重（原语实证 `ErpQaDashboardBizModel.java:304` notNull 过滤——被归一实现自身；形态按 ast 先例）→ 去重后 `size()` 即 distinct 计数」；
  - :62 → 「同 F-4：chartId 单列投影+`notNull` 过滤+内存 HashSet 去重（B-3 纳入；护栏 `TestErpQaDashboardSpc.testInadequateCapabilityCount` :70 在位）」。
- 归一后 grep 断言（B-1 可满足口径）：1418-4 文件内字面「GROUP BY chartId」命中数 **4 → 1**，剩余唯一命中为 :139 Closure 审计历史引文（闭包证据保护，非失准站点）；归一后「GROUP BY」存量 7 处逐处定性——:22 否定式 / :33、:129 授权面外同源残留（D-17，Deferred 登记）/ :60 F-2 真分组投影 / :86 被否决替代方案引文 / :96 留痕否定式（含失效括注，Deferred 登记）/ :139 Closure 引文。

## Non-Goals

- 不改分析报告 §2.2/§2.4（roadmap F-3 实裁：报告无失准表述；A9 补遗回填已载正确形态）。
- 不改 0325-2 留痕区历史记录正文（历史执行记录保持时点原貌；残留登记见 Deferred）。
- 不动 1418-4 的 checked 状态、Draft Review Record、Closure 段（已 completed 计划的闭包证据不可追溯变更）。
- 不改任何代码/模型（纯 doc）。

## Task Route

- Type: `implementation-only change`（doc-only 化妆品级文本归一，roadmap 终版指名）
- Owner Docs: `docs/plans/2026-09-28-1418-4-…md`（被归一对象）；事实依据=该计划执行期留痕 + `docs/analysis/2026-09-27-perf-ux-deep-optimization-analysis.md` §2.2 A9 补遗回填
- Skill Selection Basis: 已扫描 `docs/skills/README.md` 全表——指名点文本归一，无匹配技能。Skill: none

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（doc-only，无验证命令义务；Proof 为 grep 一致性比对）

## Execution Plan

### Phase 1 - 四处归一 + grep 断言

Status: completed
Targets: `docs/plans/2026-09-28-1418-4-a9-addendum-unbounded-queries.md`（:16/:23/:58/:62）
Skill: none

- Item Types: `Fix`×4（同一文件内的措辞-事实漂移）
- Prereqs: none

- [x] Fix :16 机制先例行先例表述拆分
      - Skill: none
- [x] Fix :23 Goals F-4 孪生行措辞
      - Skill: none
- [x] Fix :58/:62 Phase 1 两条目技术形态描述（保留原条目 checked 状态与语义）
      - Skill: none
- [x] Proof grep 断言（B-1 口径）：`GROUP BY chartId` 命中 4→1（剩余 = :139 Closure 引文）；四处新措辞的 greppable 锚串各命中一次（「单列投影+内存 HashSet 去重」:16/:23、「去重后 `size()` 即 distinct 计数」:58、「testInadequateCapabilityCount :70 在位」:62）；`git diff` 面积仅四行邻域；1418-4 文件 checked 复选框计数不变

Phase 1 执行证据（2026-09-28 实测）：

- grep 断言全绿：`GROUP BY chartId` 命中 **4→1**（剩余唯一命中 = :139 Closure 审计历史引文）；锚串「单列投影+内存 HashSet 去重」:16/:23 在位（:133 Closure Status Note 有 HEAD 既有第三命中，站点级断言不受影响）；「去重后 `size()` 即 distinct 计数」:58 唯一；「testInadequateCapabilityCount :70 在位」:62 唯一。
- `git diff` = 4 insertions/4 deletions（恰四行邻域）；1418-4 文件 checked 复选框计数 **18 不变**（脚本断言通过）。

M-1 保守性偏差显式裁决（结束审计指出，认可现状不回改）：:23 实文在括注内保留原条目「；B-3 纳入」（钉死文本漏载，实文更忠实于「保留原条目语义」指令）；:58 实文补函数名 `loadAssetIdsWithExecutedDepreciationInPeriod`（与 :16 及活代码注释一致，事实正确）；:62 「同 F-4：」写作「同 F-4——」且测试名去反引号（纯排版）。四条 greppable 锚串均不受影响——偏差登记于此，非静默。
      - Skill: none

Exit Criteria:

- [x] 四处归一落盘且 grep 断言全绿（4→1 口径，输出摘要记入本节下方）
- [x] `git diff` 仅触及目标文件四行邻域，checked 状态零变化

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 agent_931a8c76，2026-09-28）——**B-1 [Blocker]**：grep 断言「GROUP BY chartId 零命中」在计划自身 Non-Goals（Closure 段保护）下不可满足——:139 Closure round-2 审计引文亦含该字面，归一后剩余命中恰为 1 → 断言改「4→1 + 剩余唯一命中=:139 引文」；**M-1**：D-17（:33/:129）被 Goal 封为「正确上下文」实为同源失准（scheduleId 修法误归属 ast 先例，活代码证实；实施者将按误导措辞实施）→ 改判授权面外同源残留入 Deferred + roadmap D-17 行同步义务；**M-2**：:96 留痕内嵌失效交叉引用（指向报告 §2.4 不存在的文本）未登记 → 处置裁决 (b) 留痕不动+偏差显式登记；**m-1** 替换文本钉死（省略号使命中断言不可执行）；**m-2** Deferred 理由改授权面论证（:58/:62 先例证明历史条目经授权可改）；**m-3**「详见本计划执行期留痕」歧义消除。程序性核验通过：授权面/1418-4 Closure 残留自述背书/checked 保护。**GROUP BY 存量 10 处全集清单入基线**。——全部修订。
- Independent draft review iteration 2: STILL BLOCKED（同一审查代理定点复核，agent_931a8c76，2026-09-28）——B-1/M-1/M-2/m-1/m-2/m-3 全部确认 RESOLVED（B-1 三处同值/10 处基线全集与 7 处存量定性逐处一致/D-17 改判+三条 Deferred/锚串钉死且唯一/授权面论证/歧义消除），但实读活代码新引入两处：**B-2** :16 替换文本「sumBalanceTotalCost 单组 SUM」失准（ErpInvDashboardBizModel.java:506-518 实为 warehouseId 维度分组多组 SUM+内存合计）；**M-3** :58 替换文本内嵌失效行号引用 :319（1418-4 Closure round-1 整改致漂移，notNull 现位于 :331）。→ 两处定点修复。
- Independent draft review iteration 3: RESOLVED（同一审查代理增量复核，agent_931a8c76，2026-09-28）——B-2 修复与活代码逐项一致（旧错误零残留）；M-3 修复引用有效且基线补登三要素齐备；不干涉性通过（B-1 口径/锚串/Deferred/基线算术零触碰）。**计划可置 active 实施**。三轮审查全链闭合。

## Closure Gates

- [x] 范围内行为完成（Phase 1 全部退出标准达成）
- [x] 相关文档对齐（被归一计划即本批对象；日志已更新）
- [x] 已运行验证（Proof grep 一致性比对；doc-only 不适用构建/测试门——理由：零代码变更面）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### D-17 同源失准（1418-4 :33/:129「GROUP BY scheduleId 投影…ast 先例」）+ roadmap §3 D-17 行同款措辞

- Classification: `watch-only residual`
- Why Not Blocking Closure: **授权面外**（DOC-02 owner 面=chartId 四处，roadmap F-3 枚举未含 D-17）；不得改亦不得封为正确上下文（M-1）。升格计划实施 D-17 时**义务**：按 ast 先例实形态（单列投影+内存去重）更正 :33/:129 措辞，并同步 roadmap §3 D-17 行同款措辞
- Successor Required: `yes`——触发条件：D-17 升格（mnt visits 实测接近 5000）时随升格计划更正

### 1418-4 :96 留痕失效交叉引用（「报告 §2.4 维度投影描述建议随 doc-only 批校正」）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 授权面外（:96 留痕段不在 roadmap 指名四处内）；该括注指向不存在的文本（报告 §2.4 经 F-3 实裁无该表述），本计划通过基线与本条目双重登记使偏差可见——留痕段正文保持原貌
- Successor Required: `no`——1418-4 文件因其他原因被修订时随手加旁注即可（登记于此）

### 0325-2 Phase 5 留痕区 :203 ast 行同源措辞残留（含 ast 代码注释 :278）

- Classification: `watch-only residual`
- Why Not Blocking Closure: **授权面外**——本批自身确经 roadmap 授权编辑 1418-4 的已勾选历史执行条目（:58/:62 先例），故 0325-2 不动的非对称依据不是「历史记录不可改」的留痕纪律，而是授权面（DOC-02 owner doc=1418-4、F-3 枚举未含 0325-2）——m-2 更正。正确形态已由 1418-4 执行期留痕 + 分析报告 §2.2 + 活代码三重权威记载，无活跃下游误用路径
- Successor Required: `no`——触发条件：0325-2 计划文件或 ErpAstDashboardBizModel 注释因其他原因被修订时随手归一（登记于此，供 D-登记表复核节奏扫描）

## Closure

Status Note: 四处措辞归一落盘且全部断言独立复跑全绿（4→1/锚串/checked 18/diff 四行邻域/零代码面/checker 零漂移/活代码三抽查）；授权面外同源残留按反松弛规则显式登记后继义务。草案审查 3 轮收敛（B-1 grep 断言自相矛盾 Blocker + B-2 替换文本自身事实错误 Blocker + M-1~M-3——复审两度在活代码实读中拦截措辞-事实漂移）。结束审计 2 轮收敛：round 1 needs revision（唯一 Blocker=计划自身阶段台账未同步+门控已勾矛盾——GATE-02 同谱系第二案；M-1 三处保守性偏差）→ 整改（阶段 completed/全项勾选/Proof 摘要落盘/M-1 显式裁决登记）→ 增量复审 RESOLVED 授权置位。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计两轮收敛（round 1 agent_5552d233 needs revision[实质核验 7 项全过+台账 Blocker]；增量复审同代理 RESOLVED 并授权，fresh session）
- Evidence: round 1 独立复跑（diff 恰 4 行/checked 18 零变化/grep 4→1/活代码三抽查[warehouseId 维度分组 SUM/:304 notNull/:319 保留原样]/触及面无越界/零代码变更/checker 零漂移/开场前置 PASS）；增量复核：整改五要素逐项落实、无新引入矛盾（:78 Skill 子弹排版观察不阻塞）。

Follow-up:

- （无）
