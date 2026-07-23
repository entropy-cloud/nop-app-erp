# 2026-07-24-0605-1-generated-file-contentdiff-zero-drift-verification 生成文件 content-diff 零手编辑漂移验证（治理审查闭包项 #12）

> Plan Status: completed
> Mission: erp
> Work Item: 生成文件 content-diff 零手编辑漂移验证（governance audit closure #12）
> Last Reviewed: 2026-07-24
> Source: `docs/audits/2026-07-23-0000-architecture-governance-review.md` §闭包前必须项 #12（绿色信号 caveat，P2）+ §绿色信号表「生成文件 commit 配对源模型」行 caveat
> Related: `docs/plans/2026-07-24-0930-1-compliance-guard-activation-ci-baseline.md`（checker 不覆盖生成文件手编辑检测，#12 为互补验证）、`docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md`（_ 前缀文件编辑必被覆盖决策树来源）
> Audit: required

## Current Baseline

治理审查 v2 §绿色信号声称「22,413 个 `_` 前缀生成文件，最近 2 月 `_app.orm.xml` 86 次 commit 均成对出现源模型变更」，但附 **caveat**：「全仓单 author `canonical`，无法用 blame 区分人写 vs 重生成，需 content-diff 抽样进一步验证」。闭包项 #12 是该 caveat 的唯一收口动作，目前**未执行**。

实时仓库核实（2026-07-24）：

- `git ls-files | grep '_*.{java,xml}'`（含 `_app.orm.xml`）= **797 个 git-tracked `_` 前缀文件**（370 java + 427 xml），全部未被 `.gitignore` 忽略（`git check-ignore` 0 命中）。审查报告的「22,413」与 git-tracked 实测严重不符——审查可能计入非 `_` 前缀的生成产物（xmeta/xbiz/i18n）或采用不同口径，**本计划必须先落权威人口定义并调和该差异**。
- 已有保护纪律文档化：`docs/lessons/06-codegen-product-edit-overwrite.md` 确立「`_` 前缀 / `_gen/` / `__XGEN_FORCE_OVERRIDE__` 编辑必被覆盖」决策树（经多轮审计）；`docs/plans/2026-07-16-2134-1` 提供 compliance checker / skill 自检机制。但从未做过一次全仓 content-diff 实证。
- `nop-compliance-checker.sh`（16 规则）不覆盖生成文件手编辑检测——`#12` 是与之互补的唯一验证层。

剩余差距：闭包项 #12 未执行 → 绿色信号 caveat 仍悬而未决 → 「生成文件零手编辑漂移」仅是纪律声明，非实证。

## Goals

1. **建立权威生成文件人口**：精确定义并枚举「git-tracked `_` 前缀 java/xml 生成文件」集合，调和审查报告 22,413 与实测 797 的差异（落权威计数 + 口径裁决）。
2. **执行分层 content-diff 抽样**：覆盖全 19 域 + java/xml 双类型 + `_gen/` 子目录 vs 顶层 `_` 文件，用 `git log -p` 增量 diff 验证每次变更均由 codegen 模板驱动（可追溯到对应源模型变更），无孤立手编辑。
3. **闭包 #12**：消除绿色信号 caveat；若发现真漂移则登记为 Fix successor（规则 13 不可降级）而非静默处理。

## Non-Goals

- **不修复任何发现的漂移**——本计划是验证工作；发现的手编辑漂移若属 live defect，登记为 Fix successor（带 file:line + 触发条件），由独立计划修复。若属合规 `_` 前缀文件的人工 delta（如 notify-inbox / business-type.dict.yaml 经多轮审计认可的例外），如实记录为「已认可例外」。
- **不改 ORM 模型 / 字典 / BizModel 业务逻辑**（纯验证 + 文档）。
- **不做全量 22,413（或 797）文件逐个 diff**——分层抽样满足 Architecture Governance Prompt §6「Falsifiable Guards」的可证伪要求（抽样设计须能在发现漂移时失败）。
- 不建立持续 CI 门控（自动 content-diff hook 归 successor，触发条件：手编辑漂移复发或人工要求自动化）。

## Task Route

- Type: `verification or audit work`（结果面 = 治理审查闭包证据）
- Owner Docs: `docs/audits/2026-07-23-0000-architecture-governance-review.md`（闭包项 #12 + 绿色信号表）、`docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md`（`_` 前缀编辑覆盖决策树）、`docs/lessons/`（代码生成产物编辑必被覆盖）
- Skill Selection Basis: `nop-debugging`（若 content-diff 发现疑似漂移需根因定位是人写 vs 重生成）；无 BizModel/页面编写，主任务为 git diff 分析 → 其余 Skill: none。
- Bundling 裁决（rule 4/14）：#12 是单一闭包项的单一结果面（生成文件零漂移证据），不与其他 successor（F2d 字面量 / F1 daoFor）合并——验证应在重构前独立完成，确保重构触及的生成文件基线可信。
- 引用精度注记：`_` 前缀编辑必被覆盖决策树的权威来源是 `docs/lessons/06-codegen-product-edit-overwrite.md`；`docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md` 提供 checker/skill 自检机制（非决策树来源，此处仅作交叉引用）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（仅需 git history + shell）。

## Execution Plan

### Phase 1 — 权威人口定义 + 分层抽样设计

Status: completed
Targets: `docs/audits/2026-07-24-0605-generated-file-content-diff-evidence.md`（§1 决策 + §2 抽样框矩阵）
Skill: none

- Item Types: `Decision | Proof`
- Prereqs: 无

- [x] `Decision`：裁决权威生成文件人口口径——(a) 仅 git-tracked `_` 前缀 java/xml（实测 797，本仓唯一可经 `git log -p` 追溯的集合），或 (b) 扩展到非 `_` 前缀但已知生成的产物（xmeta/xbiz/i18n，但无稳定标志区分生成 vs 手写，不可证伪）。推荐 (a)：git-tracked + `_` 前缀是 Architecture Governance Prompt 闭包项 #12 命令 `git log -p -- '_*.{java,xml}'` 的精确匹配集，且可经 blame/diff 追溯。记录审查报告 22,413 的口径差异调和结论（推测为审查计入全树文件数或不同定义；以本计划权威 797 为准）+ 残留风险（非 `_` 前缀生成产物不在本验证范围，登记为 successor 若需扩展）。
  - Skill: none
  - 裁决=**口径 (a)**（git-tracked + `_` 前缀 java/xml = 797）。22,413↔797 调和：22,413 不匹配任何 git-tracked `_` 前缀口径（max any-ext=2,238），最可能为口误/位序错排或包含工作树未跟踪文件的不同口径；权威以 797 为准。残留风险=非 `_` 前缀生成产物（xmeta/xbiz/i18n 各 351 个 `_` 前缀子集）登记 successor。详见 `docs/audits/2026-07-24-0605-generated-file-content-diff-evidence.md §1`。
- [x] `Proof`：产出权威人口清单（按 19 域 + java/xml + `_gen/` vs 顶层 `_` 维度的分布矩阵），作为 Phase 2 抽样框。命令：`git ls-files | grep -E '(^|/)(_[^/]+\.(java|xml)|_app\.orm\.xml)$'` + 按域/类型聚合。
  - Skill: none
  - 矩阵已产出：797 = 95 顶层（19 域 × 5 类文件，均匀）+ 702 `_gen/`（351 java + 351 xml，按域递减，hr/fin 各 72 最多、notify 6 最少）。详见 `docs/audits/2026-07-24-0605-generated-file-content-diff-evidence.md §2`。

Exit Criteria:

- [x] 权威人口口径裁决已记录（含 22,413↔797 调和结论）
- [x] 分层抽样框矩阵已产出（19 域 × java/xml × `_gen`/顶层 分布）

### Phase 2 — content-diff 抽样执行 + 漂移判定

Status: completed
Targets: git history（最近 2 月 + 历史全量抽样）；证据落 `docs/audits/2026-07-24-0605-generated-file-content-diff-evidence.md §3`
Skill: `nop-debugging`

- Item Types: `Proof`
- Item Types Note: Phase 2 is Proof-heavy (drift detection)
- Prereqs: Phase 1 完成

- [x] `Proof`：对抽样集执行 content-diff——全量 `_app.orm.xml`（19 个，关联源模型变更成对验证）+ 分层抽样 N 个 `_` 前缀 java/xml 实体（覆盖每域 ≥1，finance/mfg/inv 等大域加抽）。每文件：`git log --oneline -- <file>` 取变更点 → `git log -p -- <file> | grep -E '^[+-]' | grep -vE '^[+-]{3}'` 审查每个 diff hunk 是否可追溯到同 commit 的源模型/codegen 模板变更（成对出现）或为孤立手编辑。
  - Skill: `nop-debugging`
  - 执行：(1) 19 个 `_app.orm.xml` 全量——94/94 commit 配对 model 源，0 未配对；(2) 全局 commit 级配对扫描 115 commit——94 配 model 源 + 21 初筛候选；(3) 21 候选逐个 content-diff 复核——修正配对口径（`_gen/_Erp*.view.xml` 源为 XMeta+template+parent-view），全部判为 codegen 驱动；(4) 关键可证伪证据：d7fb77337 action-auth post-extends 经 3 轮 ORM-regen 存活、171e4e651 view 布局变更纯机械字段重排且经 2 轮 regen 保留、`_gen/_ErpFinAccountingPeriod.java` 5/5 配对 model 源。详见证据文件 §3。
- [x] `Decision`：对每个疑似漂移 hunk 判定——「codegen 驱动（合法）」/「已认可人工例外（如 notify-inbox / business-type.dict.yaml，引用既有审计裁决）」/「真手编辑漂移（live defect，Fix successor）」。逐条记录 file:line + commit + 判定 + 依据。
  - Skill: none
  - 三态判定：codegen 驱动（合法）= 115/115 commit；已认可人工例外 = 0（notify-inbox/business-type 均为非 java/xml，不在口径 (a) 范围）；真手编辑漂移 = 0。无需 Fix successor。详见证据文件 §3.5。

Exit Criteria:

- [x] 抽样集 content-diff 全部执行（抽样规模记录，满足可证伪：设计能在漂移存在时失败）
- [x] 每个疑似漂移 hunk 已分类判定（三态），无悬而未决

### Phase 3 — 闭包证据 + caveat 消除

Status: completed
Targets: `docs/audits/2026-07-23-0000-architecture-governance-review.md`（绿色信号 caveat 已更新 + 闭包项 #12 已标注 ✅）、`docs/audits/2026-07-24-0605-generated-file-content-diff-evidence.md`（独立审计证据）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 2 完成

- [x] `Add`：在治理审查 §绿色信号表「生成文件 commit 配对源模型」行移除/更新 caveat（实证后改为已验证）；闭包项 #12 标注 ✅ Done（或 🔶 若发现漂移登记 successor）。
  - Skill: none
  - 已更新 4 处：(1) §执行摘要绿色信号行（caveat → 实证）；(2) §Design Review Matrix Truth 槽；(3) §绿色信号表「生成文件 commit 配对源模型」行 caveat →「已实证消除」；(4) §闭包前必须项 #12 → ✅ Done；(5) §附录未执行核查 content-diff 行 → 已完成。
- [x] `Proof`：抽样结果 + 方法论记录于审计证据（抽样规模 / 分布 / 判定 / 结论），可被后续审计复现。
  - Skill: none
  - 已记录于 `docs/audits/2026-07-24-0605-generated-file-content-diff-evidence.md`（§1 决策 + §2 抽样框 + §3 content-diff 结果与判定 + §4 复现指南含可独立复跑命令）。

Exit Criteria:

- [x] 治理审查闭包项 #12 已标注（✅ 无漂移 / 🔶 漂移登记 successor）
- [x] 绿色信号 caveat 已据实证更新

## Draft Review Record

- Independent draft review iteration 1: `acceptable-as-is` (`ses_06ef95acfffeBV84LP3iuffe8y`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-24) — 4 项 load-bearing 事实主张经实时仓库逐项核实**零伪**（797 git-tracked/0-gitignored 精确；#12 未完成 + 22,413 已确认；checker 不覆盖生成文件手编辑；决策树存在于 lessons/06）。R1-R14 + anti-slack 全 PASS。1 MAJOR（零代码计划保留 `mvn clean install` 门控违反模板规则——已修订：删除构建门控，改文档一致性 + 抽样可复现验证）+ 2 non-blocking MINOR（2134-1 引用精度——已修订指向 lessons/06；抽样规模 N 留 Phase 1 矩阵确定——可接受）。草案审查收敛 → `Plan Status: active`。

## Closure Gates

> 本计划为验证 + 文档，零生产代码变更（Phase 2 为只读 `git log -p`，Phase 3 仅编辑一份 markdown）。按模板规则，无代码更改的计划删除构建验证命令门控——无可回归对象。验证门控改为文档一致性 + 抽样结果可复现。

- [x] 范围内行为完成（权威人口 + content-diff 抽样 + caveat 消除）
- [x] 相关文档对齐（治理审查闭包项 #12 + 绿色信号表）
- [x] 验证证据可复现：抽样方法论 + 规模 + 分布 + 判定记录在案（零生产代码变更，无构建验证门控）
- [x] 无范围内项目降级为 deferred/follow-up（若发现漂移，真 live defect 须 Fix successor 非降级；已认可例外须记录非静默）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### content-diff 自动化 CI 门控

- Classification: `optimization candidate`
- Why Not Blocking Closure: #12 要求一次性 content-diff 抽样验证；自动化 hook（每次 commit 触发 content-diff）是增量能力，超出闭包项范围。
- Successor Required: `yes`（触发条件：手编辑漂移复发，或人工要求生成文件变更持续自动门控）

### 非 `_` 前缀生成产物（xmeta/xbiz/i18n）漂移验证

- Classification: `watch-only residual`
- Why Not Blocking Closure: 无稳定标志区分生成 vs 手写，不可经 `git log -p -- '_*'` 证伪；口径裁决 (a) 明确排除。
- Successor Required: `yes`（触发条件：需扩展验证到非 `_` 前缀生成产物 + codegen 标记机制落地）

## Closure

Status Note: passed — 独立结束审计 PASS。三阶段全部完成，零生产代码变更，caveat 经 content-diff 实证消除。

Closure Audit Evidence:

- Auditor / Agent: 独立 `general` 子代理新会话（`ses_06ee0672affe22d8HuUge44Jlu`，2026-07-24，冷重播无执行者上下文）
- Verdict: **PASS**
- 独立复验结果（全部 CONFIRMED）：797 git-tracked/0-gitignored 精确；370 java+427 xml/702 _gen+95 顶层 矩阵精确；19 个 `_app.orm.xml` 全历史 94/94 配对 model 源、0 未配对；21 漂移候选经独立复核（d7fb77337 post-extends 经 3 轮 ORM-regen 存活、171e4e651 纯机械字段重排跨 ~300 文件、FK 名称解析 commit 配对 XMeta+template+parent-view）全部判 codegen 驱动；治理审查 #12 ✅ Done + caveat 更新 5 处全部命中。
- 可证伪性检查 PASS：三层独立检测（commit 级配对 + content-diff 机械性 + regen 存活）能在漂移存在时失败。
- 结束审计门控 `[ ]`→`[x]` 经独立审计确认正确（执行者未自我审计）。
- 非阻塞 minor：证据 §3.3 称 115 commit，审计独立复测 114（1-commit 差异为合并/amend 边界，不影响零漂移结论）；日志条目于闭包时补（见下）。

Follow-up:

- content-diff 自动化 CI 门控（见上触发条件）
- 若 Phase 2 发现真漂移：逐条 Fix successor（file:line + 触发条件）— 本轮零漂移，无 successor 产生
