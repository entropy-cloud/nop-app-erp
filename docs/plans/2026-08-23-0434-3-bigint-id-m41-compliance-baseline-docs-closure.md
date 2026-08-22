# 2026-08-23-0434-3-bigint-id-m41-compliance-baseline-docs-closure 主键/外键 string 化 M4.1（3/3）：compliance 复跑 + baseline 终态 + 文档/登记册/roadmap mission 收尾

> Plan Status: active（2026-08-23 独立草案审查两轮收敛：iteration 2 `acceptable as-is`，共识达成；批准记录见 Draft Review Record）
> Mission: id-string-migration
> Work Item: M4.1（其三：compliance 复跑裁决 + baseline 终态 + 文档/登记册/roadmap mission 收尾）
> Last Reviewed: 2026-08-23
> Source: `docs/backlog/id-string-migration-roadmap.md` M4.1 行 + M4 收尾清单（checker → baseline 更新 → §16A 清理 → orm-model-design 注记 → 日志）+ 规则 8（M4.1 更新 `domain-design-guidelines.md` §16A 与 `known-good-baselines.md`）
> Related: `docs/plans/2026-08-23-0434-1-bigint-id-m41-full-build-test-restore.md`（批内序 1，前置：build+test 证据）、`docs/plans/2026-08-23-0434-2-bigint-id-m41-e2e-suite-id-assertion-repair.md`（批内序 2，前置：E2E 证据）、`docs/audits/compliance-baseline.md`（§基线表 + §BASELINE 机器可读块）、`tools/id-migration-registry.json5`（M0.2 登记册终态裁决对象）
> Audit: required（独立草案审查 + 独立结束审计；无 `model/*.orm.xml` 变更不触发该保护区域；Phase 2 含 nop-entropy 外部仓库 **docs-for-ai 文档**最小注记写入——保护区域适配裁决见 Phase 2 Decision 项）

## Current Baseline

（live 实测 2026-08-23）

- **compliance 空白期**：checker 最后零漂移基线 = 2026-08-20（19 规则 actual==baseline：R1a/b/c=0/0/0、R1d=14、R2a=34、R2b=237、R2c=1507、R2d=38、R3=5、R4=0、R5=0、R6=2、R7=0、R8=0、R10=12、R11=0、R12a/b/c=70/66/41）。mission 期间 19 域生产代码 Long→String 签名大量翻转，各域 plan 均 Non-Goal 登记「compliance 复跑归 M4.1」。roadmap 横切 §6 预期「域迁移不改 DAO 引用面/import 面形状，R2c 等计数预期不变」——待实测证实。
- **已知失败模式警示**（project-context §已知失败模式）：生产代码变更的结束审计须复跑 checker；若漂移 → 独立基线裁决计划（Fix 或 baseline-raise 带 per-site 证据），不得带病闭包。裁决方法 = `docs/skills/compliance-baseline-drift-adjudication-prompt.md`。
- **§16A 过时陈述**（`docs/design/domain-design-guidelines.md`）：§16A.4 表「`Long id` 而非 `String id`（单据头动作）| ~13 实体（purchase/sales/mfg 部分）| 13」行——mission 完成后全部单据头动作 id 已 String（19 域 1662 列落源），该行失实；§16A.3 尾注「存量 Long id 实体不强制改（改 Long→String 破坏 BizModel 签名 + 测试），登记在已知偏离」同步失效。
- **orm-model-design.md 落地注记义务**（roadmap M4.1 行 + Work Item Details）：`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B 已在本仓 19 域全量落地，注记义务归属本计划；nop-entropy 侧任何变更须记录于 `nop-entropy/ai-dev/logs/`（AGENTS.md 规则）。
- **登记册终态**（`tools/id-migration-registry.json5`，259 条）：orm-deferral 8/8 retired + service-bridge 128/128 retired；**backward-pointer 123 条仍 active**（全部已附兑付注记，未批量 retire）。工具 fail-closed 仅消费 orm-column-deferral 条目（已清零）——backward-pointer 无工具消费方，终态处置待裁决。
- **`_tmp/bigint-id-string-fix/` 时点副本**（08-21 全量刷新）：「时点 dry-run + 新鲜度门控」回写流程已随 1662 列落源完结，副本零消费方（scan 工具独立可跑于实况源）。
- **roadmap 终态义务**：M4.1 行 `todo`；依赖图 M4 节点、头部「最后更新」、Work Item Status 终态在批内序 1/2/3 全部完成后由本计划收口。
- **known-good-baselines 2026-08-20 基线行含锚点 commit `957888ffc`**（drift 裁决 per-site git diff 的锚点输入，裁决技能必需输入之一）。
- **前置依赖**：批内序 1（build 156 模块 + 全量 test + ErpAllWebPagesTest 证据）与批内序 2（flux 全量 E2E 证据）先行 completed——本计划的基线条目与 roadmap done 引用其落盘证据，不重复执行。

## Goals

- `bash docs/audits/nop-compliance-checker.sh` 复跑：零漂移证实（对照 08-20 基线逐规则）或漂移 per-site 裁决（baseline-raise 带证据 / Fix 范围内执行），`docs/audits/compliance-baseline.md` §BASELINE 同步。
- §16A.4 `Long id` 行清理 + §16A.3 尾注修正（mission 后事实：全部单据头动作 id = String）。
- orm-model-design.md 最小落地注记（nop-entropy 侧）+ `nop-entropy/ai-dev/logs/` 记录。
- 登记册 backward-pointer 123 条终态 Decision（批量 retire 核销 + fail-closed 解析验证，或保留历史的替代裁决）。
- `_tmp/bigint-id-string-fix/` 清除 Decision。
- known-good-baselines mission 收尾条目（build + test + E2E + compliance 全证据链，引用批内序 1/2 证据）。
- roadmap M4.1 → `done`（证据摘要）+ 头部 mission 完成注记 + 依赖图终态 + 日志。

## Non-Goals

- 不改生产代码（checker 裁决出真违规时，按不可降级规则属范围内修复义务——执行修复；若修复面超 5 文件/约 200 行，暂停并按范围变更登记后开独立 Fix 计划，M4.1 done 顺延）。
- 不复跑 `mvn test` / E2E（引用批内序 1/2 落盘证据；本计划仅 checker + 文档）。
- 不动 `model/*.orm.xml` 与生成件；不重录快照（批内序 1 已完成）。
- 不处理孤儿操作人列建模 follow-up（roadmap「边界说明」登记另案裁决，非本 mission 范围）。

## Task Route

- Type: `verification or audit work`（compliance 复跑裁决 + 文档/登记册终态收尾）
- Owner Docs: `docs/audits/compliance-baseline.md`（基线权威）、`docs/skills/compliance-baseline-drift-adjudication-prompt.md`（裁决方法）、`docs/backlog/id-string-migration-roadmap.md`（M4.1 终态义务）、`docs/design/domain-design-guidelines.md` §16A、`docs/testing/known-good-baselines.md`、`tools/id-migration-registry.json5`
- Skill Selection Basis: Phase 1 漂移裁决 = `compliance-baseline-drift-adjudication-prompt`（roadmap「预期技能」行指定 M4.1 → nop-testing + compliance-baseline-drift-adjudication-prompt；nop-testing 的 JUnit 快照职责已由批内序 1 兑现，本计划不重复适用）；Phase 2/3 文档收尾无匹配技能。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。工具：`bash docs/audits/nop-compliance-checker.sh`；`node tools/check-bigint-id-types.mjs scan`（登记册 fail-closed 解析验证载体）。
- 回滚策略：本仓文档/登记册/基线变更 `git revert` 即回滚；若 Phase 1 裁决落地代码 Fix，该修复按其自身变更面回滚；nop-entropy 侧注记在其仓内回滚并记 `ai-dev/logs/`。compliance-baseline.md §BASELINE 块更新前留存 08-20 基线行对照。

## Execution Plan

### Phase 1 - compliance 复跑 + 漂移裁决

Status: planned
Targets: `docs/audits/nop-compliance-checker.sh`、`docs/audits/compliance-baseline.md`
Skill: `compliance-baseline-drift-adjudication-prompt`

- Item Types: `Proof | Decision | Fix`
- Prereqs: 批内序 1/2 completed（全部生产/测试代码变更定格，checker 结果即终态）

- [ ] Proof: `bash docs/audits/nop-compliance-checker.sh` 19 规则 actual vs baseline 对照落盘（对照 08-20 基线逐规则，证实或证伪横切 §6「计数预期不变」）。
  - Skill: `compliance-baseline-drift-adjudication-prompt`
- [ ] Decision: 漂移裁决——三分支：① 零漂移（actual == baseline）→ 结论落盘；② actual > baseline 漂移 → per-site git diff（**锚点 = 08-20 零漂移基线 commit `957888ffc`**）分类（合法新增 baseline-raise 带证据 / 真违规 Fix），裁决记录 + §BASELINE 块 + 基线表同步；③ actual < baseline（命中数下降）→ CI 门控语义下通过 + 裁决是否同步下调基线（鼓励不强制，裁决落盘）。
  - Skill: `compliance-baseline-drift-adjudication-prompt`
- [ ] Fix: 真违规站点修复（若有；不可降级——范围内执行，超阈值按 Non-Goals 登记的范围变更规则处理）+ 复跑 checker 至零漂移。
  - Skill: `compliance-baseline-drift-adjudication-prompt`

Exit Criteria:

- [ ] checker 终态落盘（actual ≤ baseline 且 > 基线处全部 per-site 裁决；含裁决记录路径）+ compliance-baseline.md 同步（含 ③ 分支下调裁决，如触发）

### Phase 2 - 文档与登记册终态

Status: planned
Targets: `docs/design/domain-design-guidelines.md`、`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md`、`tools/id-migration-registry.json5`、`_tmp/bigint-id-string-fix/`
Skill: none

- Item Types: `Fix | Add | Decision`
- Prereqs: Phase 1

- [ ] Fix: §16A.4「`Long id` 而非 `String id`」行处置——**默认改注为历史行 + mission 完成事实**（保留审计轨迹，与 §16A.4「存量登记」节性质一致；不默认删除），§16A.3 尾注修正；§16A.1「5 动作统一签名 String id」与实态一致性复核注记。
  - Skill: none
- [ ] Decision + Add: orm-model-design.md §主键设计方案 B 落地注记（最小注记：方案 B 已于 nop-app-erp 19 域 1662 列全量落地，Java 层 String / DB 层 BIGINT 保持）+ `nop-entropy/ai-dev/logs/` 当日记录。**保护区域适配裁决**：`ai-autonomy-policy.md` 外部仓库保护行针对「代码」（auto + dual-agent-approval，仅新增测试/复现用例除外）；本次写入为 docs-for-ai **文档**最小注记（零代码变更）——裁决为不属保护行「代码」范畴，本计划即跨仓库载体 + ai-dev/logs 记录义务履行；**残留风险与回退分支**：若草案审查或结束审计质疑该裁决，回退为不在本计划直写——改为在本仓 `docs/discussions/` 登记注记建议 + 人工批准 successor，裁决与理由落盘本计划。
  - Skill: none
- [ ] Decision: 登记册 backward-pointer 123 条终态——推荐批量 retire（核销 note 引用 roadmap 各域 done 行；登记册本为手工维护权威）+ fail-closed 解析验证（JSON5 可解析 + `check-bigint-id-types.mjs scan` 通过）；替代方案（保留 active 作历史记录）与选择理由落盘。
  - Skill: none
- [ ] Decision: `_tmp/bigint-id-string-fix/` 清除（rm）——理由：回写流程终结 + 零消费方 + scan 独立可跑；git 状态复核确认无跟踪文件误删（该目录应属未跟踪临时区，若含跟踪文件则改登记保留）。
  - Skill: none

Exit Criteria:

- [ ] §16A 两处修正落盘 + orm-model-design 注记 + ai-dev/logs 记录在案
- [ ] 登记册终态变更 + fail-closed 验证通过 + 两项 Decision 理由落盘

### Phase 3 - 基线终态 + roadmap done + mission 收尾

Status: planned
Targets: `docs/testing/known-good-baselines.md`、`docs/backlog/id-string-migration-roadmap.md`、`docs/logs/2026/08-23.md`（或执行当日）
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1 + Phase 2 + 批内序 1/2 completed

- [ ] Add: known-good-baselines mission 收尾条目——汇总 build（156 模块）+ test（全 reactor 计数）+ E2E（flux 全量计数）+ compliance（零漂移）四源证据（引用批内序 1/2 计划文件与其基线条目，不复制计数正文，按 lesson 13 指向权威计数源）。
  - Skill: none
- [ ] Add: roadmap M4.1 → `done`（Work Item Status 表证据摘要 + 依赖图 M4 节点 + 头部「最后更新」mission 完成注记——19 域迁移 + M4.1 收尾全链完成）。
  - Skill: none
- [ ] Add: 日志条目（含验证状态全绿段——M4.1 三计划汇总口径）。
  - Skill: none

Exit Criteria:

- [ ] known-good-baselines 收尾条目落盘（四源证据引用完整）
- [ ] roadmap M4.1 done + 头部/依赖图终态一致
- [ ] 日志条目落盘

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（ses_fd4cb9008ffevoM5KVFuJJD4Cw，fresh session，治理+技术双视角）——0 BLOCKER / 1 MAJOR / 4 MINOR。**MAJOR-1**：外部仓库（nop-entropy）写入的保护区域声明不完整（「不触发保护区域」全称断言 vs Phase 2 写入 docs-for-ai）→ 已修订：Audit 行 + Phase 2 改为显式 Decision（裁决「docs-for-ai 文档最小注记不属保护行『代码』范畴」+ 残留风险 + 回退分支：审查质疑时改 docs/discussions 登记 + 人工批准 successor）。**MINOR-1**：漂移裁决未指明锚点 → 已修订（锚点 = 08-20 基线 commit `957888ffc`，Baseline 补登）。**MINOR-2**：未枚举 actual < baseline 分支 → 已修订（三分支 Decision + Closure Gates 门控语义 actual ≤ baseline）。**MINOR-3**：回滚声明与代码 Fix/跨仓库变更不自洽 → 已修订（限定句）。**MINOR-4**：§16A.4 处置二选一未定默认 → 已修订（默认改注历史行）。M4 义务对照/登记册计数/`_tmp` 未跟踪/lesson 13 引用/checker 可运行性全部 live 证实零偏差。
- Independent draft review iteration 2: `acceptable as-is`（ses_fd4bf60faffeAGK2UzSu2WaiFV，fresh session）——iteration 1 全部 5 项发现核验解决（MAJOR-1 保护区域裁决三要素完整 + 复审者作为第二独立检视者不质疑该裁决；锚点 `957888ffc` git log 实存 + 19 规则计数逐项一致；三分支/回滚三分句/§16A.4 默认处置全落地）；「鼓励不强制」经核与 compliance-baseline.md 门控 owner-doc 原文语义逐字一致不构成松弛；无 BLOCKER/MAJOR/MINOR 新发现；批内序 1 新增 findFirstByOrg 生产代码变更经交叉核验已被 Phase 1 Prereqs 全称定格声明覆盖。**共识达成，Plan Status → active。**

## Closure Gates

> 本计划无生产代码变更（除非 Phase 1 裁决出真违规 Fix）；验证门 = checker 零漂移 + 登记册 fail-closed 解析 + 文档一致性。全仓 build/test/E2E 已由批内序 1/2 作为其交付物执行并落盘，此处引用证据不重复运行。

- [ ] 范围内行为完成（checker 终态 + §16A×2 + orm-model-design 注记 + 登记册终态 + `_tmp` 处置 + 基线/roadmap/日志）
- [ ] 相关文档对齐（roadmap 终态 = mission 收官单一动作；三计划状态与 roadmap 一致性交叉核对）
- [ ] 已运行验证：`bash docs/audits/nop-compliance-checker.sh`（终态 actual ≤ baseline 且 > 基线处全部 per-site 裁决）+ `node tools/check-bigint-id-types.mjs scan`（fail-closed 载体）通过
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致（含 roadmap M4.1 done 与批内序 1/2 completed 状态互证）
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 孤儿操作人列建模 follow-up

- Classification: `watch-only residual`
- Why Not Blocking Closure: roadmap「目的」节边界说明已登记（未分类 BIGINT 列 368/380 非主键非外键，孤儿操作人列建模问题另案裁决），非本 mission 范围。
- Successor Required: `no`（roadmap 边界说明登记载体）

### compliance 真违规修复超阈值分支

- Classification: `watch-only residual`
- Why Not Blocking Closure: 仅当 Phase 1 裁决出修复面超 5 文件/约 200 行的真违规批次时触发——按 Non-Goals 登记的范围变更规则开独立 Fix 计划，M4.1 done 顺延至 Fix 完成。
- Successor Required: `yes`（触发条件：Phase 1 裁决记录）

## Closure

Status Note: （待执行后填写）

Closure Audit Evidence:

- Auditor / Agent: （待独立结束审计填写）
- Evidence: （待填写）

Follow-up:

- （无范围内跟进项；已确认缺陷不得出现在此处。）
