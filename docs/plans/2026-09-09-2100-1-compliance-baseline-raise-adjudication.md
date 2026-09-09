---
status: active
mission: ai-check-r3
work-item: ai-check-r3-compliance-baseline-raise
group: "2026-09-09-2100"
verify: [test]
---

# 2026-09-09-2100-1 compliance 基线漂移独立裁决（R2b +2 / R2c +5 / R12a +1 per-site 证据 + Fix-or-raise 机器块对齐）

## Current Baseline

- 本计划是 ai-check-r3 mission 内多批计划显式登记的 deferred successor：MI.3（`successor: 2026-09-07-r12a-compliance-baseline-raise` trigger:R12a 70→71 预存漂移独立基线裁决，plan `2026-09-06-2104-3` Closure Gates 注记）、MI.4（R2b/R2c 同通道归独立基线裁决，plan `2026-09-07-0043-1`）、MI.8 批 2（`successor: ai-check-r3-compliance-baseline-raise` trigger:compliance-baseline 机器块对齐 actual（R2b 242/R2c 1542/R12a 71），plan `2026-09-07-1715-2` Verification 注记）。按 deferred-item 触发义务，本计划产出该唯一裁决落点。
- 漂移现状（2026-09-09 起草时 HEAD `060ddab0a` 实跑复证，工作树零脏面）：`bash docs/audits/nop-compliance-checker.sh` exit 0，actual R2b=242 / R2c=1542 / R12a=71；`docs/audits/compliance-baseline.md §BASELINE (machine-readable)` 块 R2b: 240 / R2c: 1537 / R12a: 70——差值 **+2 / +5 / +1**，其余 16 条规则 actual 与基线持平或更低（R1a-c 0 / R1d 14 / R2a 34 / R2d 38 / R3 5 / R4 0 / R5 0 / R6 2 / R7 0 / R8 0 / R10 14 / R11 0 / R12b 66 / R12c 42）。
- 漂移已证为本批外预存（MI 各批 + M0.3 + M1 各切片交叉证据）：M0.3 快照行（known-good-baselines `ai-check-r3-m0` 行，2026-09-06，HEAD 零生产代码变更）即登记 actual 全表 R2b=242/R2c=1542/R12a=71 并注记「机器可读块上调注记归 compliance-baseline owner 流程 successor」；2026-08-31 行已登记「R2c=1542 与 08-28 plan-0219-2 基线 1537 差 +5 已被 ai-check 批/后续合法吸收」；MI.3 定位 R12a per-site = commit `0a825a42a`（r1-F2.9 新增 `module-assets/.../ErpAstDepreciationReversalListener.java` 引入第 71 处 import，晚于基线末次收紧 `b53b9234b`）；MI.4/MI.5a/MI.5b 均经 HEAD 临时 worktree 复跑证明 actual 与工作树逐值一致（本批 diff 零 daoFor/import 结构变更）。
- 门控语义：CI workflow（`.github/workflows/compliance.yml`）解析机器块比对 checker 汇总（`compliance-baseline.md` 门控说明节）；机器块滞后于 actual 即处于「actual > baseline」的已知失败模式暴露态（`docs/context/project-context.md` §已知失败模式 #1：Compliance 基线漂移 → 唯一出口 = 独立基线裁决计划 Fix 或 baseline-raise 带 per-site 证据）。checker 脚本本身输出逐站点 `file:lineno` 行，per-site 清单可机械枚举。
- 修改机器块的前例与约束：机器块头部注记「修改本块须经独立计划裁决」——前例 plan `2026-07-25-1057-1`（裁决性上调 R2b 315→…逐站点 file:line + 源计划 + 合法性分类 + 提交时间线核实）；人类可读表与机器块曾因只调块不调表而漂移（`compliance-baseline.md` 表格同步注记，plan `2026-08-20-0518-3` 同步修复）——本计划必须双写同步。
- 基线冻结参照点：`b53b9234b`（baseline 末次收紧提交，MI.3 注记）；其后的 r1-F 系列深化计划（F2.3/F2.4/F2.6/F2.7/F2.9 等）新增生产代码为差值主要来源候选。
- Skill：`compliance-baseline-drift-adjudication-prompt`（roadmap 横切关注点 10 指定：合法新增走 baseline-raise + per-site 证据，用该 prompt）。
- 剩余差距：+2/+5/+1 的逐站点 file:line + commit + 源计划证据未成册；机器块与人类可读表未对齐 actual；known-good-baselines 无本裁决行。

## Goals

- R2b +2 / R2c +5 / R12a +1 **逐站点**机械枚举（`file:line` + 引入 commit + 源计划），与基线冻结点 `b53b9234b` 的站点清单 diff 精确对账（+2/+5/+1 逐位闭合，不多不少）。
- 逐站点合法性三态分类（合法新增[源计划已审计登记] / 合法跨域编排[豁免通道既有裁决] / 需修复漂移），每站点留证据链；据此作出 **Fix-or-raise** 裁决（逐规则记录选择、替代方案与残余风险）。
- 裁决执行：baseline-raise 部分——机器块数值对齐 + 人类可读表同步 + 注记行（沿 `2026-07-25-1057-1` / 表格同步注记前例格式）；Fix 部分——在本计划内完成 bounded 修复（站点级重构）并回归验证。
- 收官断言：checker 全 19 规则 actual ≤ 机器块、exit 0，机器块 ↔ 人类可读表逐行一致零漂移；CJK `--strict` 零回归；known-good-baselines 登记裁决行。

## Non-Goals

- 不修改 checker 脚本判定逻辑与输出格式（脚本保持纯报告工具）。
- 不下调任何既有基线值（单向收紧纪律）；不动 16 条零漂移规则的基线值。
- 不重开既有裁决（lesson 09/10、CJK 白名单登记、validate:flux 325 条 variant 漂移 successor 在案——非本计划范围）。
- 不承接 M1.x 审计 finding 的修复（M2.x 通道，先写失败测试）；本计划 Fix 仅限 Phase 2 分类为「需修复漂移」的 checker 站点本身。
- 不做 roadmap/前序计划的 done 状态翻转（owner/engine 机制处置）。

## Phase 1 — 红线冻结与逐站点增量清单

> 类型：Proof（3 项 Proof）。
> Skill: compliance-baseline-drift-adjudication-prompt
> Targets: `docs/audits/compliance-baseline.md`（只读参照）+ 本计划勾选注记（清单暂存）
> Prereqs: 无

- [x] <Proof> 记录裁决时点仓库状态并实跑红线：HEAD hash、`git status --porcelain` 脏面披露、`bash docs/audits/nop-compliance-checker.sh` 全表 actual（对照 §BASELINE 块逐规则标出差值规则）
      - Skill: compliance-baseline-drift-adjudication-prompt
- [x] <Proof> 逐站点枚举三条差值规则：从 checker 输出提取 R2b/R2c/R12a 完整 `file:lineno` 站点清单（HEAD）；在基线冻结点 `b53b9234b`（`git worktree add` 临时目录）复跑 checker 取同规则站点清单；diff 得增量站点集，断言 R2b +2 / R2c +5 / R12a +1 逐位闭合
      - Skill: compliance-baseline-drift-adjudication-prompt
- [x] <Proof> 增量站点溯源：逐站点 `git log --follow --oneline -- <file>` 定位引入提交，交叉引用源计划（r1-F 系列 / ai-check 各批 plan 编号）形成「站点 → commit → 源计划」证据表（暂存本计划注记，Phase 2 消费）
      - Skill: compliance-baseline-drift-adjudication-prompt

Exit Criteria:

- [x] checker 全表 actual 与差值规则标定在案；+2/+5/+1 与增量站点集计数精确闭合
- [x] 每个增量站点具备 `file:line` + commit + 源计划三要素证据

## Phase 2 — 逐站点合法性分类与 Fix-or-raise 裁决

> 类型：Decision-heavy（2 Decision + 1 Proof）。
> Skill: compliance-baseline-drift-adjudication-prompt
> Targets: 本计划注记（分类表与裁决记录）
> Prereqs: Phase 1 完成

- [x] <Decision> 逐站点合法性三态分类：合法新增（源计划经审计、改写在案）/ 合法跨域编排（`docs/architecture/posting-exemptions.md` 等豁免通道既有裁决覆盖）/ 需修复漂移（无源计划登记、无豁免覆盖、违反 R 规则意图）；分类为「需修复漂移」的站点逐个定修复方式（站点级重构 → Phase 3 执行；或按级别立 finding 转 M2.x 通道并在本计划登记转移），不留悬空项
      - Skill: compliance-baseline-drift-adjudication-prompt
- [x] <Decision> 逐规则 Fix-or-raise 总裁决：R2b / R2c / R12a 各自记录选择（raise 吸收 / bounded Fix / 混合）、考虑过的替代方案（含「全部重构归零 vs 全部 raise」两端方案）与残余风险；框架性或前例已裁决的选择可作为约束引用（如 2026-08-31 行「+5 合法吸收」先例、`2026-07-25-1057-1` 分类框架），无需重复完整替代分析
      - Skill: compliance-baseline-drift-adjudication-prompt
- [x] <Proof> 分类与裁决交叉复核：增量站点集 × 分类结果覆盖完整性（无站点未分类）× 裁决一致性（分类为合法的站点不得进入 Fix 分支；raise 值 = 基线 + 合法站点计数）自洽断言在案
      - Skill: compliance-baseline-drift-adjudication-prompt

Exit Criteria:

- [x] 三规则全部作出显式裁决且理由/替代方案/残余风险落注记
- [x] 站点分类覆盖完整，Fix 分支站点清单与转移 M2.x 登记闭合

## Phase 3 — 裁决执行（机器块对齐 + bounded Fix）

> 类型：Add-heavy（Fix 分支按 Phase 2 裁决触发；1 Add + 1 Fix + 1 Proof）。
> Skill: compliance-baseline-drift-adjudication-prompt
> Targets: `docs/audits/compliance-baseline.md`（机器块 + 人类可读表 + 注记行）；Fix 分支另触 `module-*` 生产代码站点
> Prereqs: Phase 2 完成

- [x] <Add> baseline-raise 落盘：机器块差值规则数值改为裁决值（逐行 `RULE: value`）；人类可读表对应行同步；新增注记行记录本计划裁决（差值 +2/+5/+1、逐站点分类摘要、源计划指针、沿 `2026-07-25-1057-1` 前例格式）；机器块 ↔ 人类可读表全 19 规则逐行一致断言
      - Skill: compliance-baseline-drift-adjudication-prompt
- [x] <Fix> （仅当 Phase 2 存在 Fix 分支站点）站点级 bounded 修复：逐站点重构（消除违规模式，不改业务行为），修复后单站点 checker 计数下降复跑在案；无 Fix 分支站点时本项记「Phase 2 裁决无 Fix 分支」并说明，不虚设
      - Skill: compliance-baseline-drift-adjudication-prompt
- [x] <Proof> 裁决后门控复跑：`bash docs/audits/nop-compliance-checker.sh` exit 0 且全 19 规则 actual ≤ 机器块；若执行了 Fix 分支，受影响域模块 `mvn test -pl <域 service>` 全绿零回归（生产代码变更触发的验证卫生）
      - Skill: compliance-baseline-drift-adjudication-prompt

Exit Criteria:

- [x] 机器块与人类可读表同步对齐裁决值，注记行在案，逐行一致断言通过
- [x] checker exit 0 且全规则 actual ≤ 机器块（Fix 分支如有：受影响模块测试全绿）

## Phase 4 — 收官登记与全量验证

> 类型：Add + Proof（2 Add + 2 Proof）。
> Skill: compliance-baseline-drift-adjudication-prompt
> Targets: `docs/testing/known-good-baselines.md` + `docs/logs/2026/09-09.md`（或实际执行日日志）+ 本计划
> Prereqs: Phase 3 完成

- [x] <Add> known-good-baselines 登记裁决行（Scope = compliance checker 基线裁决；Commands Passed = checker exit 0 全表数字 + 机器块对齐断言 + Fix 分支测试如触发；Notes = MI.3/MI.4/MI.8 三处 successor 登记闭合声明）
      - Skill: compliance-baseline-drift-adjudication-prompt
- [x] <Add> 当日开发日志条目（简短：裁决结果 + 机器块新值 + 三处 successor 登记闭合）
      - Skill: none
- [x] <Proof> 零回归复核：`node tools/check-hardcoded-cjk.mjs --strict` exit 0（CAT 面与本裁决零交叉）；无 Fix 分支时 `git status` 触碰面 = compliance-baseline.md + known-good-baselines + 日志 + 本计划（零生产代码）；有 Fix 分支时触碰面与 Phase 3 声明一致
      - Skill: compliance-baseline-drift-adjudication-prompt
- [x] <Proof> 全仓回归门控：`mvn test` 全 reactor BUILD SUCCESS 零新增失败（对照 known-good-baselines 2026-09-08 MI 终态行 4006/0/0/1；无 Fix 分支时为纯 docs 变更下的回归复核，有 Fix 分支时为生产代码变更的必要门控）
      - Skill: none

Exit Criteria:

- [x] known-good-baselines 裁决行与日志条目在案；三处 predecessor successor 登记全部闭合
- [x] `mvn test` 全 reactor 零新增失败 + CJK `--strict` 零回归在案

## Phase Evidence（执行注记，2026-09-09 实跑）

### Phase 1 — 红线冻结与逐站点增量清单

**红线状态**：HEAD = `060ddab0a2ac4c8a123ea36538fc544b32093312`（与起草时一致）；`git status --porcelain` 脏面 = 仅 2 个未跟踪计划文件（本计划 + `2026-09-09-2100-2-m116-*`），零已跟踪文件变更。checker 实跑（exit 0）：R2b=242 / R2c=1542 / R12a=71，其余 16 规则与机器块持平（R1a-c 0 / R1d 14 / R2a 34 / R2d 38 / R3 5 / R4 0 / R5 0 / R6 2 / R7 0 / R8 0 / R10 14 / R11 0 / R12b 66 / R12c 42）——差值规则标定 R2b +2 / R2c +5 / R12a +1，与 §Current Baseline 起草值逐位一致。

**冻结点复跑**：`git worktree add <tmp>/freeze-wt b53b9234b`（HEAD 版 checker 脚本复制入 worktree 保证同口径），实测 **R2b=239 / R2c=1537 / R12a=70**。站点清单机械枚举（复刻 checker find+grep 管道，逐树全量 file:line 提取，计数与 checker 汇总逐值吻合 242/1542/71 ↔ 239/1537/70），按「file|归一化代码行」键 diff：

- **R2b**：+3 新增 / 0 移除（239→242）。**机器块差值 +2 闭合注记**：块值 240 系 E3-successor（plan `2026-08-27-1540-1`，commit `dcbc09fdf`）时点实测；其后 F2.3（`b53b9234b` 本身）移除 `ErpFinBudgetCommitmentBizModel` 1 处 `daoFor(ErpFinAccountingPeriod).getEntityById` 站点（carryForward 重构，diff `dcbc09fdf..b53b9234b -- '*BizModel.java'` 唯一 daoFor 删除行实证），改善方向机器块未回写（单向收紧纪律下下降不强制回写）→ 冻结点真相 239。块差值 +2 = +3 新增 − 1 未回写改善，逐位闭合。
- **R2c**：+8 新增 / −3 移除（1537→1542），净 +5 与块差值精确闭合。+8 = hr 5（R2b 子集 3 + FQCN 形态 2——`daoFor(app.erp.hr.dao.entity.…)` 不匹配 R2b 模式 `daoFor(Erp` 故仅计 R2c）+ mfg 2 + prj 1；−3 = mfg 2（F2.5 站点自 `ErpMfgMaterialIssueConfirmProcessor` 迁移至 Abstract，净零）+ `StockMoveBookkeeper` 1（F2.11 inv-002 自然键统一移除死代码 dao 变量，改善）。
- **R12a**：+1 新增 / 0 移除（70→71），与块差值精确闭合。

**逐站点溯源证据表**（站点 → commit → 源计划，全链已审计计划）：

| # | 规则 | 站点（HEAD file:line） | 引入 commit | 源计划 | 分类 |
|---|------|----------------------|------------|--------|------|
| 1 | R2b/R2c | `ErpHrDepartmentBizModel.java:91` `daoFor(ErpHrDepartment.class).findAll()` | `751749e17`（2026-08-31） | `2026-08-30-2238-1`（F2 flux 修复批，P1-CK-hr-001） | 合法新增 |
| 2 | R2b/R2c | `ErpHrDepartmentBizModel.java:104` `daoFor(ErpHrEmployee.class).findAll()` | `751749e17` | 同上 | 合法新增 |
| 3 | R2b/R2c | `ErpHrDepartmentBizModel.java:120` `daoFor(ErpHrRecruitment.class).findAll()` | `751749e17` | 同上 | 合法新增 |
| 4 | R2c | `ErpHrPositionBizModel.java:64` `daoFor(app.erp.hr.dao.entity.ErpHrEmployee.class).findAll()` | `751749e17` | 同上 | 合法新增 |
| 5 | R2c | `ErpHrPositionBizModel.java:80` `daoFor(app.erp.hr.dao.entity.ErpHrRecruitment.class).findAll()` | `751749e17` | 同上 | 合法新增 |
| 6 | R2c | `AbstractErpMfgMaterialIssueProcessor.java:142` `daoFor(ErpInvReservation.class).findAllByQuery(q)` | `a7058c18f`（2026-08-28） | `2026-08-28-2059-3`（F2.5 mfg-工单 P1） | 合法跨域编排（既有基线站点迁移，净零） |
| 7 | R2c | `AbstractErpMfgMaterialIssueProcessor.java:150` `daoFor(ErpInvReservationLine.class).findAllByQuery(q)` | `a7058c18f` | 同上 | 同上 |
| 8 | R2c | `ErpPrjProjectSettlementProcessor.java:339` `IEntityDao<ErpPrjProjectSettlement> dao = daoProvider.daoFor(ErpPrjProjectSettlement.class)`（`findActiveFinalOrCloseSettlement` 重复创建守卫） | `88b098043`（2026-08-29） | `2026-08-29-0745-1`（F2.12 prj-qa P1，prj-004） | 合法新增 |
| 9 | R12a | `ErpAstDepreciationReversalListener.java:8` `import app.erp.fin.dao.ErpFinBusinessType;`（消费点 :58 `ErpFinBusinessType.DEPRECIATION.name()`） | `0a825a42a`（2026-08-29） | `2026-08-28-2340-1`（F2.9 assets ast2-005，与 MI.3 定位一致） | 合法跨域编排（共享内核既有裁决） |
| −1 | R2c | `StockMoveBookkeeper.java` 死 dao 变量移除（改善） | `86cf0cdb4`（2026-08-29） | `2026-08-29-0000-1`（F2.11 inv-002） | 改善吸收 |
| −1 | R2b | `ErpFinBudgetCommitmentBizModel.java` `daoFor(ErpFinAccountingPeriod).getEntityById` 移除（冻结前改善，块未回写） | `b53b9234b`（F2.3） | ai-check F2.3 fin3 批 | 改善吸收 |

（附注：`0a825a42a` 提交信息自称「compliance 零漂移」——closure 仅核 daoFor 规则漏 R12 import 面，即 `project-context.md §已知失败模式 #1` 复发实例，MI.3 已捕获，本计划即该漂移的裁决落点。）

### Phase 2 — 逐站点合法性分类与 Fix-or-raise 裁决

**逐站点三态分类**（9 增量站点全覆盖，无未分类站点）：

- **站点 #1–#5（hr ×5）＝ 合法新增**：同域 BizModel `defaultPrepareDelete` 引用完整性删除守卫的计数查询（子部门/在职员工/未关闭招聘单），`findAll()` + 内存过滤，代码内注释明示 P1-CK-hr-001 理由（FK 列 `isQueryable=false`，QueryBean filter 抛 `unknown-query-prop`）。R2b 口径含同域 BizModel daoFor 系既有裁决（V.2 注记）；自实体计数无法注入自身 I*Biz（R1.57 `leadDao()` / R1.76-77 先例）；计数语义非 FK 导航无 ORM to-one getter 可替代。owner doc：源计划 `2026-08-30-2238-1`（经审计）。
- **站点 #6–#7（mfg ×2）＝ 合法跨域编排（净零迁移）**：站点本体（`ErpMfgMaterialIssueConfirmProcessor:332/:342` 领料预留只读聚合）已由 RC-R1.48（plan `2026-08-15-2119-3`）裁决入基线（对齐 `KitAvailabilityChecker` 跨域只读先例）；F2.5 将其迁移至 Abstract 基类（mfg-003 红冲回退共用），+2/−2 精确对冲，净零贡献。
- **站点 #8（prj ×1）＝ 合法新增**：同域结算单 FINAL/CLOSE 重复创建守卫（prj-004），镜像同文件 :238 既有同型站点（R6.6 时代已入基线）；`setLimit(1)` 存在性查询非 FK 导航。
- **站点 #9（R12a ×1）＝ 合法跨域编排（共享内核既有裁决覆盖）**：R12 三类型已经 `shared-kernel-extraction-decision.md` 分支 (b) 裁决为显式共享内核（`module-boundaries.md §共享内核` 登记）；折旧红冲 listener 按 businessType 过滤 DEPRECIATION 凭证回退计划行，与基线内 `PurReversalListener`/`SalReversalListener` 族及 R1.63 prj settlement import、F2.1 SchemaPropagator import 同型。posting.md §反写契约 域自治裁决覆盖。

**Fix 候选筛选**：9 站点均非 @Inject private / 字符串 == / System.currentTimeMillis / 越权写（skill 反模式清单逐项核对），无「需修复漂移」类站点，**Fix 分支为空**。

**逐规则 Fix-or-raise 总裁决**：

| 规则 | 裁决 | 替代方案考量 | 残余风险 |
|------|------|------------|---------|
| R2b | **baseline-raise 240 → 242** | (a) 全部重构归零——否决：5 hr 站点为计数守卫语义，无 I*Biz 计数 API、自实体自引用注入不可行、ORM to-one 不覆盖 count 场景；重构破坏守卫可读性且无合规收益。(b) 全部 raise——采纳但以 per-site 证据约束（9 站点逐站点分类在案，非整批放水；对齐 V.2/0823-1 框架先例）。 | hr 守卫 `findAll()` 全量加载在部门/员工/招聘单规模增长时有性能面（代码注释已自认「部门子集有限」）；后续可开 performance successor 评估 `setLimit` + 分页计数。 |
| R2c | **baseline-raise 1537 → 1542** | 混合裁决不适用（无 Fix 站点）；净 +5 = 新增 6（hr5+prj1）+ 迁移对冲 0 − 改善 1，两端方案（全重构/全 raise）同 R2b 论证，raise 为唯一与 per-site 证据自洽的选择。 | FQCN 形态 daoFor 绕过 R2b 模式匹配（站点 #4/#5 仅计 R2c）——测量口径盲区已在案（同型：注释排除校准先例），如需收口开独立 checker 校准 successor，本计划 Non-Goal（不改 checker）。 |
| R12a | **baseline-raise 70 → 71** | (a) 拒绝 raise 令 listener 改魔法字符串——否决：违反跨域契约类型安全（R1.63 同款裁决）。(b) 类型迁移/SPI 化——`shared-kernel-extraction-decision.md` 已否决（enum 不可降级）。(c) raise——采纳，共享内核代价模型既有裁决覆盖。 | 新增消费方使共享内核耦合面 +1；R12 门控继续追踪，后续消费方仍须逐个裁决。 |

**交叉复核（Proof）**：增量站点集 9 站点 × 分类结果 = 9/9 已分类（0 悬空）；裁决一致性 = 分类合法站点 0 个进入 Fix 分支（Fix 清单为空，与「站点分类覆盖完整，Fix 分支站点清单与转移 M2.x 登记闭合」相容——无转移登记项）；raise 值自洽断言：R2b 240+3−1=242=actual ✓ / R2c 1537+6+0−1=1542=actual ✓ / R12a 70+1=71=actual ✓。

### Phase 3 执行注记

- <Fix> 项：**Phase 2 裁决无 Fix 分支**（9 站点全部合法：合法新增 6 + 合法跨域编排/共享内核 2 + 净零迁移计入新增侧；无「需修复漂移」站点），不虚设站点级重构；无受影响生产域模块，`mvn test` 全仓门控归 Phase 4。
- 执行卫生说明：Phase 1–3 全程零生产代码/零 checker 脚本变更（触碰面 = 本计划 + `compliance-baseline.md`），Phase 1/2 的 mvn test 义务由 Phase 4 全仓门控单次覆盖（树无差异，重复跑等价）。

## Draft Review Record

- dispatch review #review-2026-09-09-210030-mission-driver-2026-09-09-2100-1-compliance-baseline-raise-adjudication-1-bb7ff83d to 2026-09-09-210030-mission-driver
- 2026-09-09：iteration 1，共识 approved #review-2026-09-09-210030-mission-driver-2026-09-09-2100-1-compliance-baseline-raise-adjudication-1-bb7ff83d

## Verification

- pass compliance 2026-09-09 exit=0 —— `bash docs/audits/nop-compliance-checker.sh` exit 0；HEAD `060ddab0a` 实测 R2b=242/R2c=1542/R12a=71 + 其余 16 规则 = 基线；冻结点 `b53b9234b` worktree 复跑 239/1537/70，站点清单 diff 闭合 +2/+5/+1（证据 §Phase Evidence）
- pass baseline-sync 2026-09-09 exit=0 —— 机器块 + 人类可读表三行同步 242/1542/71，19/19 规则逐行一致断言通过；裁决注记节沿 `2026-07-25-1057-1` 前例格式落 `docs/audits/compliance-baseline.md`
- pass cjk-strict 2026-09-09 exit=0 —— `node tools/check-hardcoded-cjk.mjs --strict` PASS（0 new violations，CAT1..4 = 0/0/0/0）
- pass mvn-test 2026-09-09 exit=0 —— 全 reactor `mvn test` BUILD SUCCESS，4006 tests / 0 failures / 0 errors / 1 skipped（41 含测试模块聚合）= 2026-09-08 MI 终态行精确一致，零新增失败
- pass touch-surface 2026-09-09 exit=0 —— `git status` 触碰面 = 本计划 + compliance-baseline.md + ai-check-r3-roadmap.md（successor 闭合注记）+ known-good-baselines.md + 日志（另 1 untracked 姊妹计划 2100-2 为他会话产物），零生产代码（无 Fix 分支裁决）
- pass test 2026-09-09-002642 exit=0 —— 闭包审计 visit 实跑全 reactor `mvn test` BUILD SUCCESS exit 0，4006 tests / 0 failures / 0 errors / 1 skipped（41 含测试模块聚合）= 2026-09-08 MI 终态行精确一致，零新增失败；同 visit 复跑 `bash docs/audits/nop-compliance-checker.sh` exit 0（R2b=242/R2c=1542/R12a=71 全 19 规则 actual ≤ 机器块）+ `node tools/check-hardcoded-cjk.mjs --strict` PASS exit 0

## Closure

- dispatch audit #audit-2026-09-09-002642-2026-09-09-2100-1-compliance-baseline-raise-adjudication-1-ac760626 to 2026-09-09-210030-mission-driver models={exec:zhipuai-coding-plan/glm-5.3-flash,aud:zhipuai-coding-plan/glm-5.3-flash}
- accepted #audit-2026-09-09-002642-2026-09-09-2100-1-compliance-baseline-raise-adjudication-1-ac760626：独立闭包审计 ACCEPT——R2b 240→242 / R2c 1537→1542 / R12a 70→71 裁决性上调落盘（机器块 + 人类可读表双写同步 + 裁决注记节），9 增量站点 per-site 证据全合法、Fix 分支空，MI.3/MI.4/MI.8 三处 deferred successor 登记闭合（roadmap 注记回写）；闭包 visit 实跑 `bash docs/audits/nop-compliance-checker.sh` exit 0（全 19 规则 actual ≤ 机器块，242/1542/71）+ `node tools/check-hardcoded-cjk.mjs --strict` PASS exit 0 + 全 reactor `mvn test` BUILD SUCCESS exit 0（4006/0/0/1 = 2026-09-08 MI 终态行精确一致），`plan-check.mjs --strict` 绿，语义审计（21/21 项勾选一致性 / 退出标准对实仓 / 反 hollow / deferred 诚实 / 文档同步）无残留
