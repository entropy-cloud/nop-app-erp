# 2026-08-20-1255-1-rc-mv-full-verification-closure-audit MV 全量验证与任务级结束审计

> Plan Status: completed
> Mission: requirement-compliance
> Work Item: MV V.1 + V.2 + V.3（全量验证与跨维度一致性回归）
> Last Reviewed: 2026-08-20
> Source: `docs/backlog/requirement-compliance-roadmap.md` MV 节（V.1/V.2/V.3 全 todo；deps `MR1 done` 已满足——RC-R1.1~R1.89 全 89 行 done，最后 RC-R1.88/89 于 2026-08-20 闭包）
> Related: MR1 全部 89 份修复计划（`docs/plans/2026-08-07-1932-*` 至 `docs/plans/2026-08-20-0518-*`）；R1.0 展开映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`；基线权威 `docs/audits/compliance-baseline.md`；后继计划 `2026-08-20-1255-2-rc-mg-knowledge-consolidation.md`（deps 本计划 completed）
> Audit: required

## Current Baseline

（2026-08-20 实仓核验）

- **MR1 全量 done**：RC-R1.1~RC-R1.89 共 89 行（96 findings + 2 裁决 P2 修复行）全部 done，每行有独立 fix plan + 独立结束审计（逐计划级）。
- **最近一次全仓验证态**：RC-R1.89 闭包（2026-08-20）实跑全仓 `mvn clean install -DskipTests`（156 reactor 模块 BUILD SUCCESS）+ 全仓 `mvn test`（surefire XML 权威计数 **3789 tests / 0 failures / 0 errors / 1 skipped**，614 文件，唯一 skip = 已知 @Disabled ErpAllWebPagesCollectTest）+ checker actual==baseline（R2c 1505→1507 / R2d 37→38 / R12c 40→41 三站点裁决性上调，per-site 证据落 `docs/audits/compliance-baseline.md`）。其后仅 docs 提交（`957888ffc`），无未提交生产代码变更（审查时仅本批两份 draft 计划文件未跟踪）——生产代码与该验证态一致。
- **MV 三行 todo 无任务级证据**：上述证据属 RC-R1.89 计划闭包，非 MV 路线图行证据；V.1（任务级全量绿色验证 + known-good-baselines 登记）、V.2（checker 基线对比）未以 MV 名义执行留痕；**V.3 任务级 closure audit 从未运行**——MR1 89 行仅有逐计划独立结束审计，无跨批次任务级审计（逐计划审计无法发现批次间交互回归与回填一致性漂移）。
- **零 P0 证实**：R1.0 展开器证实 arm-index 无实体 `P0-RC-` finding（`docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md` §统计「零 P0 证实」），MR0 无活跃行 → V.3 的「全部 P0」子句范围落空，实际深度审计范围 = **关键 P1 修复**。
- **关键 P1 的机械可提取构成**（Tier 定义依据）：① 2026-08-12 批量裁决 A 类 ORM 变更 21 项（RC-R1.43/44/45/49/51/57/58/60/66/67/70/71/72/73/74/75/80/81/82/84/87——按 A 类批量授权执行，**双独立子 agent 批准为条件义务**（越界回落时触发）非普遍义务，如 R1.43/R1.45 未触发回落故无批准记录）；② 修复触大会计过账核心路径的行——机械判据 = plan 的 Targets 或正文触及 `*PostingDispatcher` / `*AcctDocProvider` / `VoucherFact` / `ErpFinPostingProcessor` 生产代码（已知成员如 RC-R1.50（PurAcctDocProvider）、RC-R1.89（SalaryPostingDispatcher），完整清单按判据从 89 份计划机械提取，不以特定措辞字面 grep 为准）；③ 其余 P1 行按域分层。

## Goals

- V.1：以 MV 任务级名义 fresh 重跑 `mvn clean install -DskipTests` + 全仓 `mvn test`，绿色证据登记 `docs/testing/known-good-baselines.md`。
- V.2：`bash docs/audits/nop-compliance-checker.sh` actual == baseline 零未登记漂移（漂移则经基线裁决流程收口）。
- V.3：独立子代理（新会话）任务级 closure audit——关键 P1 修复深度核验（Tier 1 全部 + Tier 2 分层抽样）+ 全 89 行文档面一致性核验；证据落盘。
- roadmap MV 三行 → done，解除 MG 依赖。

## Non-Goals

- 不修复审计中新发现的活跃缺陷——发现项逐项裁决：P0 走 MR0 通道追加行、P1/P2 登记 arm-index/ successor，不在本计划内展开修复（本计划是验证与审计表面，非修复表面）。
- 不复跑 MA1-MA4 审计维度（需求符合性审计已收口，去重协议见 roadmap MA1 节）。
- 不含 MG 知识沉淀（G.1-G.3 由后继计划 `2026-08-20-1255-2` 承载）。
- 不含 Playwright E2E 全量 sweep（roadmap V.1 验证命令仅 mvn 两条；E2E 基线活动独立维护）。
- 不做 plans 批量归档（AGENTS.md §14 人工批准约束）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/backlog/requirement-compliance-roadmap.md` MV 节 + `docs/testing/known-good-baselines.md` + `docs/audits/compliance-baseline.md`
- Skill Selection Basis: V.3 任务级结束审计 → `docs/skills/closure-audit-prompt.md`（计划级审计方法升维到任务级多计划清单）；V.2 若漂移 → `docs/skills/compliance-baseline-drift-adjudication-prompt.md`；V.1 纯命令验证不加载技能。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（mvn + bash；全仓 test 历史耗时 ~10 分钟量级）。

## Execution Plan

### Phase 1 - V.1 全量绿色验证

Status: completed
Targets: 全仓 Maven reactor + `docs/testing/known-good-baselines.md`
Skill: none

- Item Types: `Proof | Fix | Add`
- Prereqs: MR1 done（已满足）

- [x] Proof: fresh `mvn clean install -DskipTests` 全仓 BUILD SUCCESS（记录 reactor 模块计数，预期 156）
      - Skill: none
      - Evidence: BUILD SUCCESS（Total time 01:40，HEAD=`957888ffc`）；reactor 模块计数 = **156**（`mvn validate` Building 行计数），与预期一致。
- [x] Proof: fresh 全仓 `mvn test`（surefire XML 权威计数记录 tests/failures/errors/skipped；与 RC-R1.89 基线 3789/0/0/1 对照，任何偏差逐项归因）
      - Skill: none
      - Evidence: 全 reactor BUILD SUCCESS（Total time 12:51）；surefire XML 聚合（614 文件）= **3789 tests / 0 failures / 0 errors / 1 skipped**，唯一 skip = `TEST-io.nop.app.all.web.ErpAllWebPagesCollectTest.xml`（已知 @Disabled JDK26/ANTLR H-2）。与 RC-R1.89 基线 3789/0/0/1 **逐项零偏差**，无需归因。
- [x] Add: `docs/testing/known-good-baselines.md` 增 MV 任务级基线行（命令、计数、git state、证据指向本计划）
      - Skill: none
      - Evidence: 2026-08-20 基线行已添加（Git State = `957888ffc` + 2 未跟踪 draft 计划文件；含 install 156 模块 + test 3789/0/0/1 + checker 19 规则零漂移三命令证据，指向本计划）。
- [x] Fix:（条件触发）任一命令红 → 定位是否 RC-R1.x 批次引入回归：是 → 本计划内最小修复收口（代码逻辑类预授权；触保护区域按 roadmap 预授权声明门控走双独立子 agent 批准）；否（预存/环境）→ 按基线例外规则附前置失败证据登记
      - Skill: nop-debugging（条件触发时加载）
      - Evidence: 未触发——两条命令均绿，零偏差。

Exit Criteria:

- [x] 两条命令绿色证据（或失败归因登记）存在于 known-good-baselines.md 新基线行
- [x] 计数与 RC-R1.89 基线对照无未归因偏差

### Phase 2 - V.2 compliance 基线对比

Status: completed
Targets: `bash docs/audits/nop-compliance-checker.sh` 输出 vs `docs/audits/compliance-baseline.md`
Skill: compliance-baseline-drift-adjudication-prompt（仅漂移时）

- Item Types: `Proof | Fix`
- Prereqs: Phase 1 完成（同一代码态）

- [x] Proof: checker 复跑全 19 规则 actual == baseline 零未登记漂移（对齐 RC-R1.89 闭包三站点上调后基线 R2c=1507/R2d=38/R12c=41）
      - Skill: none
      - Evidence: 全 19 规则逐项相等——R1a=0 R1b=0 R1c=0 R1d=14 R2a=34 R2b=237 R2c=1507 R2d=38 R3=5 R4=0 R5=0 R6=2 R7=0 R8=0 R10=12 R11=0 R12a=70 R12b=66 R12c=41（与 BASELINE 机器可读块 19 行逐行一致；三上调站点 R2c=1507/R2d=38/R12c=41 复确认）。
- [x] Fix:（条件触发）漂移 → 按裁决流程收口：修复致漂移的违规站点（Fix）或 baseline-raise 带 per-site 证据登记（对齐 `compliance-baseline-drift-adjudication-prompt.md`），禁止静默放行
      - Skill: compliance-baseline-drift-adjudication-prompt
      - Evidence: 未触发——零漂移。

Exit Criteria:

- [x] checker 对齐证据（逐规则计数或漂移裁决记录）落盘于本计划 Closure 节

### Phase 3 - V.3 独立子代理任务级 closure audit

Status: completed
Targets: MR1 全部 89 份修复计划 + `docs/audits/arm-index.md` 注记 + 各域 owner doc 回填 + 审计报告落 `docs/audits/`
Skill: closure-audit-prompt

- Item Types: `Proof | Fix`
- Prereqs: Phase 1-2 完成

- [x] Proof: 独立子代理（新会话，非执行者）按 closure-audit-prompt 方法执行任务级审计，范围两层：
      - **文档面全覆盖**（89/89 行）：roadmap 行 done 注记 ↔ plan `Plan Status: completed` ↔ arm-index finding done 注记 ↔ owner doc 回填 ↔（A 类行）授权链完整性（2026-08-12 批量授权引用、或越界回落的双独立子 agent 批准记录、或无回落发生的执行记录，三者其一成立即通过）——五点一致性逐行核验；
      - **深度行为核验抽样**：Tier 1 全部（A 类 21 项 ORM 行 + 按机械判据（plan Targets/正文触及 `*PostingDispatcher`/`*AcctDocProvider`/`VoucherFact`/`ErpFinPostingProcessor` 生产代码）提取的会计过账核心路径行）+ Tier 2 其余行按域分层抽样 ≥20%（每个触及域至少 1 行）；核验维度 = 修复 claim 的测试存在性 + 关键断言强度 + owner doc 契约对齐（owner-doc → 代码一致性抽核按 closure-audit-prompt 模板）。
      - Skill: closure-audit-prompt
      - Evidence: 独立子代理 cold session（ses_fe209d15dffe4AWOvQdd66OZNo）执行，报告落盘 `docs/audits/2026-08-20-1255-rc-mv-task-level-closure-audit.md`。Tier 1 = 30 行（A 类 21 + 过账核心机械提取 9 行：R1.17/18/42/50/53/56/63/64/89；18 份 grep 命中中 9 份因仅背景提及剔除）；Tier 2 = 22 行（等距 + 域覆盖，37% ≥ 20%，17 适用域全覆盖）。初轮 VERDICT = needs revision（3 发现，零 P0）。
- [x] Fix: 审计发现项逐项裁决收口：文档/登记面漂移（注记缺失、状态不一致）当场修复；活跃缺陷不在本计划修复——P0 追加 MR0 行、P1/P2 登记 arm-index successor，全部显式列出不得静默
      - Skill: none
      - Evidence: 三发现全部裁决收口（详见审计报告 §4.1 裁决收口块）——**F1（P1，lesson 8）**：plan 2040-3 缺 round-2 独立结束审计 → 独立子代理（新会话，ses_fe1f93ed2ffeaNLj9kVnZiiYrV）补跑 round 2，五项 round-1 缺口逐项文件证据复核，**passes closure audit**，证据落盘该计划 Closure Audit Evidence 节；**F2（doc-drift）**：roadmap R1.16/17/83/84/85 五行裸 done 补 ✅ 注记（plan 引用 + 验证摘要 + 闭包审计 + arm-index）；**F3（doc-drift）**：depreciation-and-posting.md 补 RC-R1.52 补提实现注记（§5.1，与 plan 0424-2 claims 逐项相符）。零活跃缺陷 → 无 MR0/arm-index successor 登记义务。
- [x] Proof: 审计报告落盘 `docs/audits/`（含两层范围、抽样清单、发现项裁决表、VERDICT）；roadmap V.1/V.2/V.3 → done
      - Skill: none
      - Evidence: 报告 `docs/audits/2026-08-20-1255-rc-mv-task-level-closure-audit.md`（§1 范围方法/§2 Layer 1/§3 Layer 2/§4 裁决表 + §4.1 裁决收口/§5 VERDICT）；复审（同审计者 session）确认 F1/F2/F3 全收口，Layer 1 五点一致性 80/89 → **89/89**，**终裁 VERDICT = passes closure audit（发现项全部裁决收口后复审通过）**；roadmap MV 三行已翻 done ✅（requirement-compliance-roadmap.md MV 节）。

Exit Criteria:

- [x] 独立子代理审计 VERDICT = pass（或发现项全部裁决收口后复审通过），报告落盘且本计划 Closure 节引用
- [x] roadmap MV 三行状态翻 done 与审计证据一致

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_fe2766d6bffe4GGtZzhpf3Peye) because 两项 BLOCKER——Tier 1 ② 以「会计过账核心路径」措辞字面 grep 为机械判据自相矛盾（R1.50 plan 实用「会计过账逻辑收敛」措辞）+ A 类 21 项「均带双独立子 agent 批准义务」过度陈述（批量授权下双批准为越界回落条件义务，R1.43/R1.45 未触发回落无批准记录）；另 2 项 MINOR（Phase 1 item types 缺 Add、「工作树 clean」措辞）
- Independent draft review iteration 2: acceptable as-is (ses_fe27133a1fferyNM82Q7n1Xj6Y) after 两项 BLOCKER 改写（类触判据 `*PostingDispatcher`/`*AcctDocProvider`/`VoucherFact`/`ErpFinPostingProcessor` + 授权链完整性三选一核验，R1.50/R1.89 新判据下复核仍为已知成员）+ 两项 MINOR 修复；残余 1 项 MINOR（iteration 1 记录未回填）随本记录落盘解决。共识达成，Plan Status → active。

## Closure Gates

> 完整仓库验证即本计划主体（V.1/V.2 命令在 Phases 1-2 已执行，Closure 复核证据一致性即可，不重复跑全量）。V.3 审计由独立子代理执行；执行者不得自我审计。

- [x] 范围内行为完成（V.1 基线行 + V.2 对齐证据 + V.3 审计报告）
- [x] 相关文档对齐（known-good-baselines.md + roadmap MV 三行 done + 审计发现项裁决记录）
- [x] 已运行验证（`mvn clean install -DskipTests` + 全仓 `mvn test` + `bash docs/audits/nop-compliance-checker.sh`，证据在 Phase 1-2）
- [x] 无范围内项目降级为 deferred/follow-up（审计发现的活跃缺陷登记 MR0/arm-index 属显式转移所有权，非降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（注：本计划的结束审计与 Phase 3 V.3 审计同为独立子代理执行时，须为两次独立运行——V.3 审计对象是 MR1 修复批次，本计划结束审计对象是本计划自身门控）
- [x] 结束证据存在于文件中
- [x] `docs/logs/2026/08-20.md`（或执行当日）日志条目

## Deferred But Adjudicated

### 审计中新发现的活跃缺陷修复

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本计划结果表面 = 验证与审计证据，非修复；发现项经 MR0 通道（P0）或 arm-index/successor 登记（P1/P2）显式转移所有权，触发条件与裁决随登记落盘
- Successor Required: yes（触发条件：V.3 审计产出任何活跃缺陷发现项时，对应 MR0/arm-index 行即为其 successor）

## Closure

Status Note: Phases 1-3 Verification Evidence 已落盘；本计划自身的独立结束审计（区别于 Phase 3 V.3 任务级审计，按 Closure Gates 注两次独立运行要求）已于 2026-08-20 通过——9/9 门控 passes closure audit（见 Closure Audit Evidence）。Plan Status → completed 翻转与 Closure Gates 勾选由执行者在审计通过后完成（expected-pending）。

Verification Evidence (Phases 1-2, 2026-08-20, HEAD=`957888ffc`)：

- V.1a `mvn clean install -DskipTests`：全仓 BUILD SUCCESS（01:40），reactor 模块 = **156**（与预期一致）。
- V.1b `mvn test`：全 reactor BUILD SUCCESS（12:51）；surefire XML 权威计数（614 文件）= **3789 tests / 0 failures / 0 errors / 1 skipped**，唯一 skip = `ErpAllWebPagesCollectTest`（已知 @Disabled）；与 RC-R1.89 基线 3789/0/0/1 逐项零偏差。
- V.1c `docs/testing/known-good-baselines.md` 已增 2026-08-20 MV 任务级基线行（三命令 + git state + 计数，证据指向本计划）。
- V.2 `bash docs/audits/nop-compliance-checker.sh`（同代码态复跑）：全 19 规则 actual == baseline 零漂移——R1a=0 R1b=0 R1c=0 R1d=14 R2a=34 R2b=237 R2c=**1507** R2d=**38** R3=5 R4=0 R5=0 R6=2 R7=0 R8=0 R10=12 R11=0 R12a=70 R12b=66 R12c=**41**（三上调站点对齐 RC-R1.89 闭包基线）。零漂移 → 无裁决记录，条件 Fix 未触发。

Closure Audit Evidence:

- Auditor / Agent: independent plan-closure auditor（fresh cold-context session，2026-08-20；与本计划执行者、V.3 任务级审计者（ses_fe209d15dffe4AWOvQdd66OZNo，审计对象 = MR1 89 行批次）、2040-3 round-2 审计者（ses_fe1f93ed2ffeaNLj9kVnZiiYrV）均不同一——按 Closure Gates 注「两次独立运行」要求，本审计对象 = 本计划自身 9 项门控）
- Evidence: 逐门控活仓核验（2026-08-20）——
  1. **范围内行为完成 ✅**：V.1 基线行 `docs/testing/known-good-baselines.md:13`（2026-08-20，install 156 模块 + test 3789/0/0/1（614 文件）+ checker 19 规则零漂移三命令，Git State `957888ffc`，证据列指向本计划）；V.2 逐规则对齐证据在本计划 Closure §Verification Evidence（19 规则全列）；V.3 报告 `docs/audits/2026-08-20-1255-rc-mv-task-level-closure-audit.md` 落盘，§5 终裁 VERDICT = passes closure audit + §4.1 裁决收口块实存。
  2. **相关文档对齐 ✅**：roadmap MV 三行（`requirement-compliance-roadmap.md:487-489`）全 done ✅（V.1/V.2 引本计划、V.3 引审计报告）；F2 修复实存（`:408-409` R1.16/17 done ✅ 引 plan 2026-08-08-2219-2；`:475-477` R1.83/84/85 done ✅ 引 plan 2026-08-19-2040-1，注记含验证摘要/独立结束审计/arm-index 回填）；F3 修复实存（`docs/design/assets/depreciation-and-posting.md:220` §5.1 RC-R1.52 补提实现注记五要素与 plan 0424-2 claims 相符）；F1 修复实存（plan `2026-08-19-2040-3-...md:190-199` round-2 独立审计块 + `:199` passes closure audit 裁决 + `:173` Status Note 同步）。
  3. **已运行验证 ✅**（含强制验证范围检查）：Phase 1-2 全部 item/exit criteria 已勾带证据；全仓 `mvn clean install -DskipTests`（156 reactor 模块）= full-build ✅ + 全仓 `mvn test` = full-test ✅（非 scoped）；计数三处交叉一致（本计划 Phase 1 ↔ 基线行 ↔ `docs/logs/2026/08-20.md` 首条：156 模块 / 3789-0-0-1 / 614 XML / R2c=1507·R2d=38·R12c=41）；与 RC-R1.89 对照基线 3789/0/0/1 逐项零偏差成立（R1.89 VERIFY 日志同日同计数佐证）；`compliance-baseline.md` BASELINE 机器可读块 `:451-469` 19 行与 V.2 声明逐行一致；活仓佐证（未重跑 mvn，仅内部一致性核验）：HEAD=`957888ffc`、surefire `TEST-*.xml` 实存 614 文件、当前 dirty 改动全为 docs（F1/F2/F3 修复面）零生产代码变更。
  4. **无范围内项目降级 ✅**：Deferred But Adjudicated 仅含预注册 watch-only 项（审计发现活跃缺陷的显式转移通道，本计划实际零活跃缺陷未触发）；V.3 三发现全部裁决收口（F1 补跑 round-2 / F2 F3 当场修复），无 MR0/arm-index successor 义务，无 in-scope 项被静默 deferred。
  5. **独立草案审查已完成并记录 ✅**：Draft Review Record 2 轮（iteration 1 needs revision → iteration 2 acceptable as-is，双 ses ID 落盘）。
  6. **文本一致性 ✅**：三 Phase 全 completed、全部 item/exit criteria 已勾；plan / roadmap（MV done ✅）/ log（08-20.md 首条）状态一致；front matter Plan Status 仍 `active` = expected-pending（执行者于本审计通过后翻转，非遗漏）。
  7. **结束审计独立性 ✅**：三次独立运行互异——V.3 审计（报告 `:5` 审计对象明示 MR1 全 89 行修复批次，非本计划门控）+ 2040-3 round-2 + 本计划门控审计（本审计者 fresh cold session）；执行者未自我审计、Closure Gates 未被留作人工占位。
  8. **结束证据存在于文件中 ✅**：本块即证据（独立审计者落盘）。
  9. **日志条目 ✅**：`docs/logs/2026/08-20.md` 首条为本计划聚合条目（V.1/V.2/V.3 全证据链），计数与 plan/基线行逐项一致。

  **Overall VERDICT: passes closure audit**（9/9 门控通过）。残余非阻塞观察：① Plan Status → completed 翻转与 Closure Gates 9 项勾选由执行者按流程完成（expected-pending，非遗漏）；② V.3 审计报告与本计划/MG 计划 3 份文件尚未 git 提交（落盘已满足「证据在文件中」门控，提交属常规后续）。

Follow-up:

- V.3 审计报告（`docs/audits/2026-08-20-1255-rc-mv-task-level-closure-audit.md`）+ 本计划 + MG 后继计划（`2026-08-20-1255-2`）等本批 docs 变更待常规 git 提交（非阻塞，证据落盘已满足门控）。
- MG 知识沉淀（G.1-G.3）依赖已解除，由后继计划 `docs/plans/2026-08-20-1255-2-rc-mg-knowledge-consolidation.md` 承载；其 G.1 候选输入含 V.3 审计的 lesson-8 复发观察（2040-3 round-2 延迟补跑模式）。
- 零活跃缺陷——无 MR0/arm-index successor 登记义务（已确认活跃缺陷不得出现在此处）。
