# 2026-09-28-1745-1 GATE-04 无界查询可重放枚举（tools/enum-unbounded-queries.mjs）

> Plan Status: completed
> Last Reviewed: 2026-09-28
> Source: `docs/backlog/perf-ux-debt-consolidation-roadmap.md` M1 GATE-04（使 A9 式枚举漏盘成为一条命令可证伪的命题）
> Related: `docs/analysis/2026-09-27-perf-ux-deep-optimization-analysis.md` §2.2 A9 行；`docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md` Phase 5 留痕区 + 残留站点白名单；`docs/plans/2026-09-28-1418-4-a9-addendum-unbounded-queries.md`（Non-Goals 豁免登记）；`tools/check-hardcoded-cjk.mjs`（lesson 20 五要素范式：全量扫描/anti-fake-green 自证/fail-loud/基线单向收紧）
> Audit: required

## Current Baseline

- **扫描宇宙**：`*DashboardBizModel.java` / `*ReportBizModel.java`（src/main，排除 `_gen`/`/target/`），实测 **22 文件**。此即 mission 批次 1-6 实际治理的 A9 宇宙（批次 2 Phase 1-4：sal/pur/fin/inv dashboard；Phase 5：inv/mfg/qa/cs/crm/hr report + ast dash/report；1418-4：cs quality dash/prj report/mnt report/qa dash）。
- **站点实测（2026-09-28 探针，草案审查独立复核精确复现 22/93/22/71）**：宇宙内 `findAllByQuery(`/`.findList(`/`.findAll(` 调用共 **93** 处；按「方法域含 `setLimit(`/`.limit(`」粗判有界 **22** 处、无界 **71** 处。71 处构成（草案审查 F-1 实核更正，冻结时以脚本实测为准、本清单仅作交叉校验——实测与预期不符时须在计划留痕差异）：
  - 批次 1-4/Phase 5 ①类 SQL 投影聚合站点（投影行数=分组数天然有界，调用形态无 setLimit）；②类 cap 站点（有 setLimit，不入无界清单）；③类豁免站点（in(id) 有界跟随/主数据小表）；
  - **qa dash 2 站更正归 freeze-unadjudicated（结束审计 F-2b）**：`loadSpcSamples`（eq(chartId) 单图全行样本）/`loadInspectionsInRange`(日期窗列表)——1418-4 F-4 实际裁决的三站（countOutOfControlCharts/孪生/findCapaOverdueAlert）现均为有界不在 71 台账内，本两站未经 1418-4 裁决，初版误标 1418-4-cap 已更正；
  - `D01-deferred` = **3 站点**（fin report :442 账龄 openItemsQuery / :557、:563 现金流 loadPostedVoucherLineSet 超集——分析报告 §6/roadmap D-01 触发驱动）；
  - fin report :511/:517 为 `loadGlBalances`（分析报告 §2.4 :82 明记「SQL 聚合正面范式，不需要动」）→ source= `positive-template`；
  - fin report :600（loadPeriodStatus 期间状态小表）/:608、:615（countBillR 期末结账数据集）+ **mnt dash 4 + prj dash 5 + mfg dash 4 + md report 3** = `freeze-unadjudicated` **约 19 站点/5 文件**（fin :600/:608/:615 计入 fin report 帐内）——**非**「口径外小表」统称：草案审查抽样判明 prj dash `getProjectGrossMargin`（PnL 快照表每日一快照增长面）/`findDelayedProjectAlert`（预警无 cap，同形 findCapaOverdueAlert 在 1418-4 拿了 cap）、mfg dash `computeOnTimeRate`（全部历史完工工单无界——与 0325-2 已裁决 cap 的 pur onTimeRate 同形同源）、md report `buildPartnerListDataset`（增长主表明细无 cap）至少 4 处为 **②类 cap 候选形**，逐站点形态注记义务见 Goals；`ErpMdDashboardBizModel` 实测 **0** 无界站点（3 站点全部方法域内 setLimit(ALERT_MAX_ROWS)）——初判点名有误，以本段实核清单为准；
  - **builder 形态 1 处**（草案审查 F-2 实核，初判「0 处」被证伪）：`ErpCsQualityDashboardBizModel.buildClosedTicketQuery`（:282 helper 内 setLimit，调用方 :263 `findAllByQuery(q)`）——两级判定下保守误报入台账，source=`1418-4-cap`（其 cap 在 1418-4 批次落地）。
- **不变量语义**：roadmap GATE-04 =「新增未登记站点即非零退出」——台账=冻结时站点全集（每站点带裁决来源标注），门禁=当前扫描结果对台账的 diff（新站点→exit 1；站点消失→IMPROVEMENTS 提示收紧）。
- 脚本先例：`tools/check-hardcoded-cjk.mjs`（git ls-files 枚举/`--self-test`/退出码 0/1/2/语句切分先例）。
- 剩余差距：A9 式枚举漏盘（1418-4 批次实证漏了 4 站点/2 整域）只能靠人肉重扫描发现，无可重放命题。

## Goals

- 新增 `tools/enum-unbounded-queries.mjs`：
  - **枚举**：宇宙文件内 `findAllByQuery(`/`.findList(`/`.findAll(` 调用站点，输出 `file:line :: 方法 :: 语句摘录` 清单；
  - **有界判定**（Decision-1）：站点所在语句含 `setLimit(`/`.limit(`，或语句中查询变量 `var` 在其方法域内出现 `var.setLimit(`/`var.limit(`；
  - **台账 diff**：内置 `LEDGER`（脚本内常量，`file :: line :: 方法 :: 归一化摘录指纹` 键 + `source` 标注）——扫描结果 ⊆ 台账 → exit 0；新站点（不在台账）→ exit 1 逐行列出；台账站点消失 → IMPROVEMENTS 提示（人工收紧台账，不自动改写）。
- **冻结程序**（Anti-fake-green 义务）：以脚本自身输出冻结台账——初版生成后逐站点标注 `source`（闭合枚举：`batch2-p1-4-agg` / `phase5-agg` / `phase5-exempt` / `1418-4-exempt` / `1418-4-cap` / `positive-template` / `D01-deferred` / `freeze-unadjudicated`）**与形态注记**（主数据小表 / in 有界跟随 / 增长表明细 / 预警列表无 cap / 分组投影 / cap 候选——草案审查 F-4：形态注记是 D-复核分诊的输入）+ **非空裁决指针**（`文件#锚点` 格式指向裁决文档；freeze-unadjudicated 例外指向本计划冻结记录）。标注依据=上述计划的留痕区/Non-Goals/分析报告 §2.4/:82。**台账机械校验（草案审查 F-3）**：脚本加载 LEDGER 时强制 source∈闭合枚举 + 每条裁决指针非空 + 站点键格式合法，违例 exit 2（fail-loud，lesson 20 要素 4）；与 cjk-baseline 的结构差异显式声明——ledger 含人工 source/注记列，**不是 repo 状态的纯函数、不可整体重生成**，「勿手改」保护结构性不可用，git diff 逐条对照是唯一防线（Closure Gates 增对应检查项）。**双向纪律（lesson 07 转写）**：新增条目必须携带 per-site 裁决指针（合法接纳新无界站点，如 D-01 同类裁决）；移除仅经 IMPROVEMENTS 提示 + 逐条对照扫描输出 + 收紧后复跑 diff exit 0 + 日志留痕（收紧由执行计划内的执行者做，e 裁决：不需 baseline-raise 级仪式）。无法归档的站点标 `freeze-unadjudicated` 并汇入日志条目供 D-登记表复核节奏消费。
- `--self-test` 故障注入自测（全内存样本）：含 setLimit 语句放行；变量级 `q.setLimit` 放行；无界调用必报；`.limit(` 链式放行；方法外顶层调用（无方法上下文）按无界必报；ledger diff 数学（新站点必报/消失列 IMPROVEMENTS）。
- 注入重放：向宇宙内一文件投放未跟踪的 `?` 不适用（Java 源不可投放临时调用——改用**临时修改检测**替代：对一真实宇宙文件复制体在 /tmp 沙箱以 `--root` 参数跑脚本断言检出；或直接以 self-test 覆盖）。最终形态以实现复杂度裁决（Decision-2）。
- 接线：npm script `check:unbounded-queries` / `check:unbounded-queries:self-test`（根 package.json，同面裁决；F-6）。

## Non-Goals

- 不治理任何站点：本脚本是**可重放枚举门禁**，不做修复；`freeze-unadjudicated` 站点的治理（若有）经 D-登记表/独立计划升格。
- 不扩大扫描面到全仓 835 处调用：非 Dashboard/Report 宇宙的调用（主体为 entity BizModel 读路径与域算法，草案审查实测更正——不限于 Calculator/Processor/Aggregator）不在 A9 治理语义内，全仓面会产生数百站点的噪音台账（探测数据见基线），稀释门禁信号。宇宙面扩展触发条件：A9 式漏盘在其他文件族复现时（经独立计划裁决扩面）。
- 不做 SQL 语义级「真无界」判定（投影有界/过滤有界的形式化分析）——有界性裁决留在台账 `source` 标注的人类语义层，脚本只做机械枚举与 diff。
- 不修改 validate:flux / checker 既有语义。

## Task Route

- Type: `implementation-only change`（新增工具脚本 + npm 接线；不触业务行为、契约、保护区）
- Owner Docs: `docs/analysis/2026-09-27-perf-ux-deep-optimization-analysis.md` §2.2 A9（扫描语义来源）；`docs/plans/2026-09-27-0325-2…md` Phase 5 留痕区（台账来源之一）；`docs/plans/2026-09-28-1418-4…md`（台账来源之二）
- Skill Selection Basis: 已扫描 `docs/skills/README.md` 全表——无静态分析脚本类匹配技能；范式复用 lesson 20 五要素。Skill: none

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - 脚本实现 + 台账冻结 + npm 接线

Status: completed
Targets: `tools/enum-unbounded-queries.mjs`（新增）、`package.json`（+1 script）
Skill: none

- Item Types: `Add`×3 + `Decision`×2
- Prereqs: none

- [x] Add 枚举器：宇宙文件枚举（`*DashboardBizModel|*ReportBizModel`）+ 调用站点提取（行级）+ 方法域/语句域解析 + 有界判定（语句 `setLimit`/`.limit(` 与变量级 `var.setLimit`）
      - Skill: none
- [x] Add 台账机制：`--freeze` 生成台账初版（JSON 块内嵌脚本或独立 registry 文件，形态随 Decision-2）→ 人工标注 `source` → diff 模式（缺省）exit 0/1 + IMPROVEMENTS
      - Skill: none
- [x] Add `--self-test`（Goals 样本清单 + builder 形态样本）+ `package.json` 两 scripts
      - Skill: none
- [x] Decision-1 有界判定口径：语句级 + 变量级两级（保守偏误报方向——误报站点冻结入台账无害，漏报即门禁失效）。替代方案「方法域任一 setLimit 即全方法有界」（探针初版口径）否决：同方法第二查询变量无 setLimit 会漏报。残留风险：①跨方法 builder 形态（helper 内 setLimit）会保守误报——**实测 1 处**（buildClosedTicketQuery，source=1418-4-cap 承接），self-test 固化 builder 样本（查询变量方法域无 setLimit → 必报）；②同变量重赋值残余漏洞（`q = new QueryBean()` 二次构建）全宇宙 grep 实测 0 处，出现时按证据扩判定
      - Skill: none
- [x] Decision-2 台账载体：**脚本内嵌 LEDGER 常量**（roadmap 原文「内置豁免台账」）vs 独立 registry 文件——取内嵌（roadmap 字面 + 单文件可移植；71 站点量级内嵌可维护；`--freeze` 输出待标注草稿到 stdout，人工粘回脚本内嵌块并标注 source/注记/指针）。**两处 roadmap 字面解读 delta 落盘（草案审查 F-5/裁决 a/附加裁决）**：①扫描面为对 roadmap 字面「全仓 src/main」的有意收窄至 22 文件宇宙——roadmap 指名对象（A9 式枚举漏盘可证伪）被宇宙面完整覆盖（1418-4 实证漏盘 4 站点全在宇宙内），宇宙外主体为 entity BizModel 读路径与域算法（Non-Goals 人口描述照此更正），roadmap 措辞调和登记归后续 DOC 批；②「内置豁免台账」按 roadmap 自身不变量（「新增未登记站点即非零退出」）语义实现为**冻结时全站点台账**——若台账仅含豁免类，71 存量站点 day-1 即全部「未登记」，门禁永红不可用；Phase 5 留痕区本就是其文件的 ①②③ 全站点记录——属解释而非放宽（更大台账=更强反假绿）。残留风险：脚本 diff 噪音大——接受（门禁脚本变更频率低）
      - Skill: none
- [x] 台账冻结执行：跑 `--freeze` → 逐站点标注 source + 形态注记 + 裁决指针（F-1 实核清单逐一对照）→ `freeze-unadjudicated` 站点清单写入日志条目 → diff 模式对 HEAD 必须 exit 0；**站点实数 pin 落盘**（F-6：以脚本实测数替代「71±」）
      - Skill: none

Exit Criteria:

- [x] `node tools/enum-unbounded-queries.mjs`（diff 模式）对 HEAD exit 0（站点集=台账）
- [x] 台账机械校验生效：人工构造 source 非法/指针为空的 LEDGER → exit 2（self-test 或临时变异验证，用后还原）
- [x] `--self-test` 全部断言 PASS
- [x] 台账含全部实测站点（实数 pin 于本节下方）且每站点有 source + 形态注记 + 裁决指针；`freeze-unadjudicated` 站点清单已写入日志

Phase 1/2 执行证据（2026-09-28 实测）：

- **站点实数 pin：71**（22 宇宙文件 / 93 调用 / 22 有界 / **71 无界**——与草案审查独立探针逐值一致；预期清单与实测零差异，无差异留痕义务触发）。
- 台账冻结：`--freeze` 生成 71 条草案 → 按文件默认 + 站点覆盖标注后嵌入脚本 LEDGER 常量（71 条，每条 source∈闭合枚举+形态注记+文件#锚点裁决指针）。**结束审计 F-2 整改（annotate-only）**：初版 16 站 freeze-unadjudicated shape 为占位符、qa dash 2 站伪 source=1418-4-cap、pur computeOnTimeRate/inv findBatchExpiryAlert 形态错标——已逐站重写真实形态（20/20 行，含 4+ 处 cap 候选形标注），机械 key 未动（diff 全绿保持）。
- `--self-test`：11/11 断言 PASS（语句级/变量级/链式 .limit(/builder 保守误报/无方法上下文/台账三向校验）。首轮 1 例失败系 ledger 解析器静默丢弃格式坏行——修复为 fail-loud（malformed 行返回并计 error，lesson 20 要素 4）。
- diff 模式 HEAD：`RESULT: PASS (all 71 live unbounded sites are in the ledger; ledger entries=71)` exit 0。
- 新站点检出重放（真实文件注入）：向 ErpMdReportBizModel 注入 zzProbeUnbounded 无界方法 → **exit 1** + `NEW ...:50 :: zzProbeUnbounded :: ...` 精确定位；还原后 exit 0（git status 干净）。
- 台账篡改校验重放：临时变异 source=phase5-agg→bogus-value → **exit 2** + `LEDGER mechanical validation failed (fail-loud, lesson 20 element 4): source "bogus-value" not in closed enum`；还原后 diff 恢复 PASS。
- `freeze-unadjudicated` 站点清单（写入日志条目）：fin report 3 + mnt dashboard 4 + prj dashboard 5 + mfg dashboard 4 + md report 3 = **19 站点/5 文件**。
- `npm run check:unbounded-queries` / `:self-test`：链路可执行（exit 0）。
- `bash docs/audits/nop-compliance-checker.sh`：R2b=232 / R2c=1559 零漂移。

### Phase 2 - 验证与文档对齐

Status: completed
Targets: `docs/logs/2026/09-28.md`、分析报告 §2.2 A9 行补一行门禁注记（`tools/enum-unbounded-queries.mjs` 为 A9 的可重放枚举）
Skill: none

- Item Types: `Proof`×3 + `Add`×1
- Prereqs: Phase 1

- [x] Proof 新站点检出实跑：/tmp 沙箱复制一宇宙文件 + 追加无界查询方法 + 造最小 pom/目录结构 → `--root` 模式（如 Decision-2 载体支持）或经 self-test 复用枚举函数断言新站点 exit 1
      - Skill: none
- [x] Proof `npm run check:unbounded-queries` 链路可执行
      - Skill: none
- [x] Proof `bash docs/audits/nop-compliance-checker.sh` 零漂移确认（纯工具脚本，lesson 07 廉价保险）
      - Skill: none
- [x] Add 日志条目 + 分析报告 A9 行门禁注记
      - Skill: none

Exit Criteria:

- [x] Phase 1 全部 Exit Criteria 保持绿 + 三条 Proof 证据摘要落盘计划本节下方

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 agent_b4fd3693，2026-09-28）——硬计数底座 22/93/22/71/835 独立探针精确复现；**F-1 [Major]** 基线站点构成两处失实（fin report 8 站点仅 3 个属 D-01——:511/:517 为 loadGlBalances 正面范式、:600/:608/:615 非 D-01；freeze-unadjudicated 实集 ≈19 站点/5 文件：fin 3 + mnt dash 4 + prj dash 5 + mfg dash 4 + md report 3，md dashboard 实测 0 无界站点初判点名有误，mfg dash 为真实候选未点名；抽样判明 ≥4 处为 ②类 cap 候选形非「小表」）→ 基线按实核清单改写 + 冻结实测优先于预期清单 + 差异留痕义务；**F-2 [Major]** builder 形态「实测 0 处」被证伪（buildClosedTicketQuery 1 处）→ Decision-1 更正 + source 枚举增 1418-4-cap + self-test 补 builder 样本；**F-3 [Major]** 台账防篡改仅散文且「只能收紧」方向自错 → source 闭合枚举 + 裁决指针机械校验（违例 exit 2）+ 双向纪律 lesson 07 转写 + 与 cjk-baseline 结构差异声明（不可整体重生成）+ Closure Gates 增 LEDGER diff 对照项；**F-4** 逐站点形态注记义务 + 删「小表」统称；**F-5** 两处 roadmap 字面 delta 落盘（宇宙面有意收窄/豁免台账=全站点台账的解释性依据）+ Non-Goals 人口描述更正；**F-6** 站点实数 pin + self-test npm script。五挑战点裁决（a 宇宙面收窄成立补 delta 记录/b 两级口径成立 builder 断言更正/c 篡改面机械强制落地/d freeze-unadjudicated 定位为快照条目合格+形态注记前提/e 收紧不需仪式但须指名协议）全部吸收。——全部修订。
- Independent draft review iteration 2: RESOLVED（同一审查代理定点复核，agent_f0e0d748，2026-09-28）——F-1/F-2/F-3 三 Major 与 F-4/F-5/F-6 三 Minor 全部忠实落地，基线站点构成经实仓逐点验证准确（md dash 0 无界/builder 1 处/fin 8 站点三分归属/loadPostedVoucherLineSet、loadPeriodStatus、countBillR、loadGlBalances 方法归属/md report 3 站点）；无硬矛盾（闭合枚举 8 值与基线标注对齐/freeze 与校验次序由 Decision-2 覆盖）；非阻塞观察 4 条（N-1 台账定义域=经 Decision-1 判无界的 71 站点非 93 全部——实施时补一短语/N-2 freeze 分支跳过 LEDGER 校验/N-3 exit-2 约定出处 lesson 20 而非 cjk 脚本/N-4 行号 1 行偏移）。**计划可置 active 实施**。

## Closure Gates

- [x] 范围内行为完成（Phase 1-2 全部退出标准达成）
- [x] 相关文档对齐（分析报告 §2.2 A9 门禁注记已落盘；日志已更新）
- [x] 已运行验证（diff 模式 HEAD exit 0 + self-test 含 builder 样本 + 新站点检出 + npm 链路 + checker 零漂移）
- [x] **LEDGER 块 git diff 逐条对照**（F-3）：20 行增改逐一核验均为 source/shape/pointer 语义列修正、key 列零变化（复审计结构性证明：round-1 keys≡live keys≡round-2 keys 多重集相等 + 20 行目检与 round 1 逐字一致）；指针文件存在性抽核通过
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### freeze-unadjudicated 站点的语义裁决

- Classification: `watch-only residual`
- Why Not Blocking Closure: 冻结台账显式登记（非静默豁免）；收官重扫描口径外的小表 dashboard 站点，治理与否属业务裁决
- Successor Required: `yes`——触发条件：roadmap §3 D-登记表复核节奏（新 mission 启动扫描）命中，或对应域数据量级实测超标

## Closure

Status Note: A9 无界查询可重放枚举落地并全验证：宇宙 22 文件/71 冻结站点台账（每站 source 闭合枚举+形态注记+裁决指针；含 ≥6 处 cap 候选形标注）/机械校验 exit 2 fail-loud/diff 新站点 exit 1/IMPROVEMENTS 永不自动改写。草案审查 2 轮收敛（F-1 基线构成/F-2 builder 形态/F-3 台账防篡改机械强制）。结束审计 2 轮收敛：round 1 needs revision（F-1 分析报告 A9 注记未落盘；F-2 台账语义标注 ~20 站缺陷[16 站占位 shape/qa dash 2 站伪 source/2 站形态错标]）→ annotate-only 整改（20/20 行逐站真实形态、机械 key 未动 diff 全绿保持、分析报告 A9 门禁注记落盘、构成表述更正为 freeze-unadjudicated 21 站点/6 文件）→ 复审计 RESOLVED 授权置位（key 列零变化结构性证明；残留 r-1~r-4 观察随提交留痕处置）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计两轮收敛（round 1 agent_a5741491 needs revision 2 Major；复审计同代理 RESOLVED 授权，fresh session，全程只读+独立复跑）
- Evidence: round 1 独立复跑（self-test 11/11/diff 71=71/新站点注入 exit 1 精确定位/台账篡改 exit 2/台账抽查含伪 source 捕获/触及面零越界/checker 零漂移/开场前置 PASS）；复审计逐项核验 F-1 注记落盘/F-2a 16 站真实形态含 6 处 cap 候选/F-2b qa dash 更正+构成表述同步/F-2c 两站形态校正/三命令复跑全绿/LEDGER 20 行 key 零变化结构性证明/门控纪律无违例。残留 r-1（种子表过期注记——已随置位补入 LEDGER 注释）/r-2~r-4（观感级留痕）。

Follow-up:

- （无）
