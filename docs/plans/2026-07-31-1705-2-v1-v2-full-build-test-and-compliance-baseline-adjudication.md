# 2026-07-31-1705-2-v1-v2-full-build-test-and-compliance-baseline-adjudication 全量验证基线（构建/测试绿 + compliance 基线裁决）

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MV 工作项 V.1 + V.2（todo，依赖 MR4 done；R4.1 由 plan `-1705-1` 解除阻塞）
> Related: plan `2026-07-31-1705-1-r4-1-cross-dimension-adjudication.md`（前置门控）；plan `2026-07-31-1705-3-v3-v4-v5-e2e-regression-closure-audit-index-integrity.md`（后续门控，依赖本计划绿基线）
> Audit: required

## Current Baseline

- **roadmap 状态**：MV 里程碑 V.1–V.5 全部 todo。V.1 依赖 MR4 done（R4.1 由 `-1705-1` 完成）；V.2 依赖 V.1。本计划覆盖 V.1+V.2（静态验证基线结果表面），V.3+V.4+V.5 归 `-1705-3`。
- **V.1 已知基线**（M0 锚点 `docs/audits/compliance-baseline.md §M0 锚点注记`，HEAD=0e963531d，2026-07-27）：
  - `mvn clean install -DskipTests` → BUILD SUCCESS。**模块数口径不一致**：M0 锚点记 156 reactor 模块；`project-context.md` 记 154；最近 R2.15（v22）记 156。需本计划实测落定权威值。
  - `mvn test` → 0 failures / 0 errors / 1 skipped（`ErpAllWebPagesCollectTest` `@Disabled`，见 `docs/testing/known-good-baselines.md §Known Failures (Accepted)`）。M0 锚点记单测方法计数=1756；roadmap `§当前基线` 历史引用「~2890」为起草期粗估（已由 M0 注记修正为 1756）。
  - 自 M0 锚点后 62+ commits（含 4 P0 hotfix + 全部 MR1/MR2/MR3/MR5 修复）未做全量绿基线复确认——本计划是审计-修复回归起点的**首次全量回归确认**。
- **V.2 compliance 实测漂移**（本计划起草时复跑 `bash docs/audits/nop-compliance-checker.sh` 实测，对照 `compliance-baseline.md §BASELINE` 机器可读块）：
  - **5 项 post-M0 回归**（与 A7.4 报告 §残留风险「5 项 post-audit compliance 回归[R2a/R2b/R2c/R5/R12c]」一致，A7.4 明确将其 deferred 至 MV V.2）：

    | 规则 | BASELINE | 当前实测 | 漂移 | 初判来源 |
    |------|----------|----------|------|----------|
    | R2a（BizModel daoFor(ErpMd*)） | 37 | 38 | +1 | MR 修复期新增 dashboard/report 跨域只读 |
    | R2b（BizModel daoFor(Erp*)） | 315 | 325 | +10 | 同上族 |
    | R2c（全生产代码 daoFor() 总量） | 1228 | 1250 | +22 | MR 修复期新增 service 代码（R3.3/R3.4/R5.x 等） |
    | R5（@Inject + private） | 0 | 1 | +1 | **R3.4 新增 `ErpRoleDataAuthChecker.java:30` `@Inject private IDaoProvider`**（疑似违反 Nop IoC「@Inject 字段不能 private」规则，候选 Fix 而非 baseline-raise） |
    | R12c（import AcctSchemaResolver 跨域） | 38 | 40 | +2 | MR 修复期新增过账链路 import |

  - 其余 14 条可计数规则（R1a/R1b/R1c/R1d/R2d/R3/R4/R6/R7/R8/R10/R11/R12a/R12b）实测均 ≤ baseline，零漂移。
- **门控语义澄清**：roadmap V.2 文案「不得高于 M0 基线」中的「M0 基线」是概念锚点；**操作性门控基线**是 `compliance-baseline.md §BASELINE` 机器可读块（已含历次裁决性上调）。本计划对照操作性基线裁决 5 项漂移：每项要么 `Fix`（驱动回降）要么 `adjudicated baseline-raise`（经独立计划裁决上调 BASELINE 块 + 注记理由），不得留作裸漂移。
- **剩余差距**：全量绿基线未复确认；5 项 compliance 漂移未裁决（裸漂移状态）；`ErpRoleDataAuthChecker` R5 疑似缺陷未修。

## Goals

- **V.1**：跑全量 `mvn clean install -DskipTests`（确认 BUILD SUCCESS + 落定权威模块数）+ `mvn test`（确认 0 failures/0 errors/1 skipped 已知 skip），作为审计-修复回归起点的首次全量绿基线复确认。
- **V.2**：对照 `§BASELINE` 块裁决 5 项 compliance 漂移，每项给出 `Fix`（驱动回降并复跑）或 `adjudicated baseline-raise`（裁决上调 BASELINE 块 + compliance-baseline.md 注记 + per-site 证据）的二选一裁决，使 checker 复跑 actual ≤ baseline。
- 修掉 R5 疑似缺陷（`ErpRoleDataAuthChecker` private @Inject）若裁决为 Fix。
- 更新 `compliance-baseline.md §BASELINE` 块（仅裁决性上调项）+ 新增 V.2 注记段。

## Non-Goals

- 不裁决跨维度发现（R4.1，归 `-1705-1`）。
- 不做 E2E 回归（V.3）、独立 closure audit（V.4）、索引完整性校验（V.5）——归 `-1705-3`。
- 不重新审计 compliance 规则集（19 规则已由 A7.4 验证激活性）；本计划只裁决已有漂移。
- 不处理 F15 i18n checker 基线（已接入 CI，基线 0/0 干净，R3.7 已闭合；非 V.2 范围除非复跑发现回归）。
- 不更新 MG 知识沉淀（G.1–G.4，依赖 MV 全 done）。

## Task Route

- Type: `verification or audit work`（V.1 纯验证）+ `implementation-only change`（V.2 R5 Fix 若裁决为修）
- Owner Docs: `docs/audits/compliance-baseline.md`（§BASELINE + §M0 锚点 + §回归门控规则）；`docs/architecture/processor-extension-pattern.md`（R2c daoFor 分类背书）；`docs/analysis/shared-kernel-extraction-decision.md`（R12 共享内核裁决）
- Skill Selection Basis: V.1 无技能（机械构建/测试）。V.2 compliance 裁决引用历次基线裁决范式（`compliance-baseline.md` 历史注记段：R2c 裁决性上调 / R1d 注释校准 / checker 校准范式矩阵），非新审计维度；若 R5 裁决为 Fix 则加载 `nop-backend-dev`（@Inject 字段非 private 是 Nop IoC 硬规则）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 构建需 `nop-entropy` 父 POM 已在本地 Maven 仓库可用（见 `project-context.md`）。
- compliance checker 是 grep-based，不依赖构建产物；但 V.2 的 Fix 路径（若改 Java）需 V.1 绿构建作为复跑前提。

## Execution Plan

### Phase 1 - V.1 全量绿基线复确认

Status: completed
Targets: 全仓（154/156 reactor 模块）
Skill: none

- Item Types: `Proof`
- Prereqs: R4.1 done（`-1705-1`）

- [x] `mvn clean install -DskipTests` → 确认 BUILD SUCCESS，落定权威 reactor 模块数（消除 154 vs 156 口径分歧），记录于本计划 + `project-context.md` 若需更正
- [x] `mvn test` → 确认 0 failures / 0 errors / 1 skipped（`ErpAllWebPagesCollectTest @Disabled`），记录单测方法计数（对照 M0 锚点 1756）
- [x] 若 V.1 红：定位失败用例/模块，登记为 P0（按 roadmap P0 即时通道纪律就地修复或异步注入 fix plan），不静默降级

Exit Criteria:

> 本阶段交付全量绿基线确认证据。解除 V.2 / `-1705-3` 对绿构建的依赖。

- [x] `mvn clean install -DskipTests` BUILD SUCCESS 且模块数权威值已落定
- [x] `mvn test` 0 failures / 0 errors / 1 skipped（已知 skip 可解释）

**Phase 1 Evidence**：

- `mvn clean install -DskipTests` → BUILD SUCCESS（01:35 min），reactor 模块数 = **156**（权威值，消除 154 vs 156 口径分歧 → 落定=156）。154 为起草期粗估/旧值；M0 锚点（156）+ R2.15 v22（156）+ 本计划实测（156）三源一致。`project-context.md` 无显式 reactor 模块数声明（仅"18 域"），无需更正；roadmap line 10/276 的 154 文案修正归 G.4（已知文档计数漂移，非本计划范围）。
- `mvn test` → BUILD SUCCESS（09:25 min），**0 failures / 0 errors / 1 skipped**（`io.nop.app.all.web.ErpAllWebPagesCollectTest` `@Disabled`，见 `known-good-baselines.md §Known Failures (Accepted)`）。单测方法计数（surefire per-module 汇总）= **1902**（M0 锚点 1756 +146，由 post-M0 R3.x / R3.4 角色行级过滤等深化计划新增测试）。V.1 无红，无 P0 登记。

### Phase 2 - V.2 compliance 5 项漂移逐项裁决

Status: completed
Targets: `docs/audits/compliance-baseline.md`（§BASELINE + 新增 V.2 注记段）；可能 `module-common-service/.../ErpRoleDataAuthChecker.java`（R5 Fix）
Skill: `nop-backend-dev`（仅 R5 Fix 项）；其余裁决引用 compliance-baseline.md 历史范式

- Item Types: `Decision | Fix | Add | Proof`
- Prereqs: Phase 1 绿基线

- [x] **R5 裁决（ErpRoleDataAuthChecker private @Inject）**：核对 `module-common-service/.../auth/ErpRoleDataAuthChecker.java:30`。AGENTS.md「@Inject 字段不能是 private」是 Nop IoC 硬规则——裁决候选=**Fix**（移除 `private` 修饰符 → 字段包级可见），驱动 R5 回 0；若该类确有特殊原因须 private（如非 IoC 注入），登记 owner-doc 豁免并裁决 baseline-raise。Fix 后复跑 checker 确认 R5=0。
  - Skill: `nop-backend-dev`
- [x] **R2a/R2b/R2c 裁决（daoFor 族 +1/+10/+22）**：对每条新增 daoFor 站点 per-site 分类——(A) 合法跨域只读（dashboard/report 聚合，IDaoProvider 已登记豁免范式，owner-doc `processor-extension-pattern.md`/data-dependency-matrix 背书）→ baseline-raise；(B) 可重构为 I*Biz 注入 → Fix。逐站点记录 file:line + 分类理由。裁决后 BASELINE 块 R2a/R2b/R2c 上调至实测值（若全 A）或驱动回降（若含 B）。
  - Skill: none（引用 compliance-baseline.md R2c 历史裁决范式）
- [x] **R12c 裁决（AcctSchemaResolver import +2）**：核对 2 处新增 import 是否为 MR 过账链路修复的合法跨域引用（R12 已裁决基线 69/66/38 为共享内核代价，`shared-kernel-extraction-decision.md` 背书）。裁决=baseline-raise（若合法）或 Fix（重构去 import）。更新 BASELINE 块 R12c。
  - Skill: none（引用 shared-kernel-extraction-decision.md）
- [x] **BASELINE 块更新**：仅对裁决为 baseline-raise 的规则更新 `compliance-baseline.md §BASELINE` 机器可读块（须经独立计划裁决=本计划即是），并新增「V.2 裁决注记」段记录 5 项漂移的逐项裁决 + per-site 证据 + Fix/baseline-raise 分类。
- [x] **Proof**: `bash docs/audits/nop-compliance-checker.sh` 复跑 → 全 19 规则 actual ≤ baseline（零裸漂移）；记录裁决后快照表。

Exit Criteria:

> 本阶段交付 compliance 零裸漂移 + 裁决证据落盘。R5 Fix 项若涉及 Java 变更需本地化复编译确认。

- [x] `bash docs/audits/nop-compliance-checker.sh` 复跑全 19 规则 actual ≤ baseline
- [x] 5 项漂移每项有 Fix 或 adjudicated baseline-raise 裁决记录（含 per-site file:line 证据），BASELINE 块与裁决一致
- [x] R5 若裁决为 Fix：`ErpRoleDataAuthChecker` private 已移除且 `mvn compile -pl module-common-service` 通过

**Phase 2 Evidence**：

**裁决汇总**（per-site file:line + git diff 锚点 `0e963531d`..HEAD 核实）：

| 规则 | M0 锚点 | 漂移后实测 | 裁决 | 新基线 | per-site 证据 |
|------|---------|-----------|------|--------|--------------|
| R5 | 0 | 1 | **Fix** | 0 | `module-common-service/src/main/java/app/erp/common/auth/ErpRoleDataAuthChecker.java:30-31` `@Inject private IDaoProvider daoProvider`（R3.4 plan `2026-07-31-1023-3` 新增 config-gated checker）。违反 Nop IoC 硬规则。Fix=移除 `private` → 包级可见（对齐全仓 `@Inject IDaoProvider daoProvider;` 范式），setter `setDaoProvider` 保持不变。Fix 后 checker R5=0，`mvn compile -pl module-common-service -am` BUILD SUCCESS。 |
| R2a | 37 | 38 | baseline-raise | 38 | +1 唯一新站点 `module-finance/.../entity/ErpFinReconciliationBizModel.java:460` `daoProvider().daoFor(ErpMdSubject.class)`（fin→md 跨域只读：AR/AP 核销读核算科目）。git diff 证伪 ErpB2bAsnBizModel:267 / ErpFinBudgetCommitmentBizModel:122,131 为 pre-existing（锚点已存，仅行号偏移）。 |
| R2b | 315 | 325 | baseline-raise | 325 | 净 +10（+11 新增 −1 移除）。新增站点绝大多数同域内部访问（b2b/ErpB2bAsnBizModel×2 / cs/ErpCsTicketBizModel / fin/ErpFinDashboardBizModel+ErpFinReportBizModel+ErpFinIntercompanyMatchBizModel×3 / hr/ErpHrShiftAssignmentBizModel / log/ErpLogShipmentBizModel）+ 1 跨域只读（fin→md，即 R2a）。R2b 计数口径含同域 daoFor，均为合法 IDaoProvider 范式，无 B 类候选。 |
| R2c | 1228 | 1250 | baseline-raise | 1250 | 净 +22（R2b BizModel 增量 + 非 BizModel 生产增量）。非 BizModel 新站点（排除 test/）：aps/ErpApsSchedulingProcessor / ast/ErpAstDepreciationScheduleProcessor / common/ErpOrgIsolationQueryTransformer(R1.29 org 隔离) / fin/ErpFinBudgetScenarioProcessor×2 / fin/ErpFinDeferredPostingRetryHelper / fin/ErpFinAccountingPeriodProcessor×3（含 fin→inv ErpInvLandedCost + fin→ast ErpAstDepreciationSchedule 期间结账清理只读）/ inv/StandardCostingStrategy / inv/StockMoveBookkeeper×2 / mnt/ScheduleDueGenerator / mfg/MrpReleaseService / pur/PaymentSettler。跨域站点经 `processor-extension-pattern.md` + `data-dependency-matrix.md` 背书，无 B 类候选。 |
| R12c | 38 | 40 | baseline-raise | 40 | +2 新 import：`module-finance/.../dashboard/ErpFinDashboardBizModel.java` + `module-finance/.../report/ErpFinReportBizModel.java`（finance 看板/报表消费共享内核 AcctSchemaResolver 做账套感知聚合）。`shared-kernel-extraction-decision.md` 背书分支 (b) 显式共享内核。 |

**裁决后快照**（checker 复跑 actual，全 19 规则 actual ≤ baseline，零裸漂移）：

| 规则 | Baseline | Actual | 状态 |
|------|----------|--------|------|
| R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
| R1d | 17 | 17 | ✅ |
| R2a/R2b/R2c/R2d | 38/325/1250/28 | 38/325/1250/28 | ✅ |
| R3 | 5 | 5 | ✅ |
| R4/R5 | 0/0 | 0/0 | ✅（R5 Fix 回 0） |
| R6/R7 | 2/0 | 2/0 | ✅ |
| R8 | 42 | 42 | ✅ |
| R10/R11 | 6/0 | 6/0 | ✅ |
| R12a/R12b/R12c | 69/66/40 | 69/66/40 | ✅ |

`compliance-baseline.md` §基线表 + §BASELINE 机器可读块 + 新增 §V.2 compliance 漂移裁决注记段已同步更新。

## Draft Review Record

- Independent draft review iteration 1: accept (ses_0489144e2ffeQZ8f4oZuSIP2s2) — 独立子代理 fresh session **实测复跑 compliance checker**：5 项漂移（R2a 37→38 / R2b 315→325 / R2c 1228→1250 / R5 0→1 / R12c 38→40）与计划声明逐项精确一致；其余 14 规则 ≤ baseline 零漂移确认。R5 缺陷 `ErpRoleDataAuthChecker.java:30-31 @Inject private IDaoProvider` 实测确认违反 Nop IoC 硬规则，Fix 候选正确。M0 锚点值（156 模块/0 failures/1 skipped/1756 测试）核对一致。V.1/V.2 依赖与范围边界（不泄漏 V.3-V.5）确认。类型标记/skill 选定/baseline-raise 自引用裁决纪律/全仓验证置于 Closure Gates 均合规，零 anti-slack 违规。consensus 达成，draft→active。

## Closure Gates

> 完整仓库验证在此处运行一次（非每阶段）。本计划触及 Java（R5 Fix 可能）+ compliance 基线 markdown。

- [x] 范围内行为完成（V.1 绿基线 + V.2 零裸漂移）
- [x] 相关文档对齐（compliance-baseline.md §BASELINE + §V.2 注记；project-context.md 模块数若更正）
- [x] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test` 0 failures + `bash docs/audits/nop-compliance-checker.sh` 全 19 规则 actual ≤ baseline
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### F15 i18n checker 基线（非 V.2 范围）

- Classification: `watch-only residual`
- Why Not Blocking Closure: F15 i18n-coverage-checker.sh 已接入 CI（R3.7，plan `-1439-3`），基线 0/0 干净。V.2 仅复跑 F8 `nop-compliance-checker.sh`；若复跑 F15 发现回归则升级为范围内，否则维持 successor。
- Successor Required: `no`（除非复跑发现 defects/gaps > 0）

## Closure

Status Note: V.1 全量绿基线复确认（`mvn clean install -DskipTests` BUILD SUCCESS 156 模块 + `mvn test` 0 failures/0 errors/1 skipped/1902 单测）+ V.2 compliance 5 项漂移逐项裁决（R5 Fix 回 0；R2a/R2b/R2c/R12c baseline-raise 至实测值，零裸漂移）。compliance-baseline.md §BASELINE 块 + §基线表 + 新增 §V.2 注记段同步更新。executor 未自我审计；独立结束审计 PASS。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 fresh session cold-context（ses_048562ba4ffeoVxLNfDxhiKHJ3）
- Evidence: 9 项核查全 PASS — R5 fix 实仓确认（package-private，setter/@Inject 语义完整）；§基线表与 §BASELINE yaml 双块一致（R2a=38/R2b=325/R2c=1250/R12c=40/R5=0）；§V.2 注记段存在含 per-site file:line；per-site 抽查（ErpFinReconciliationBizModel:460 / ErpFinReportBizModel+ErpFinDashboardBizModel AcctSchemaResolver import）属实；checker 复跑全 19 规则 actual ≤ baseline（R5=0/R2a=38/R2b=325/R2c=1250/R12c=40）；`mvn clean install -DskipTests` BUILD SUCCESS（156 模块）；`mvn test` 0 failures/0 errors/1 skipped；反基线侵蚀裁决=raise-to-actual（无 headroom 通胀），R5=Fix 非 raise。VERDICT: PASS

Follow-up:

- <none in scope>（V.3/V.4/V.5 归 `-1705-3`；roadmap 154 文案修正归 G.4）
