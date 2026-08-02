# 2026-08-02-0651-1 compliance 基线漂移裁决（R2b/R2c/R2d，post-R6.8）

> Plan Status: completed
> Last Reviewed: 2026-08-02
> Source: `docs/backlog/audit-remediation-roadmap.md`（全工作项 done 后，闭合回路复跑 compliance checker 暴露未裁决漂移）+ `docs/context/project-context.md §已知失败模式（Compliance 基线漂移）`
> Related: `docs/plans/2026-08-01-0656-1-r6-8-mr6-full-verification-completion-criteria.md`（R6.8 上一次基线裁决，设 R2c=1380/R2d=32/R2b=240）+ `docs/plans/2026-07-31-1705-2-v1-v2-full-build-test-and-compliance-baseline-adjudication.md`（V.2 裁决先例）+ `docs/plans/2026-07-25-1057-1-compliance-baseline-drift-adjudication.md`（首例漂移裁决先例）
> Audit: required

## Current Baseline

- **audit-remediation roadmap 全工作项 done**（M0→MA1-MA7→MR1-MR6[CLOSED]→MV→MG→MQ Q0-Q7）。无 `todo`/`ready` 工作项。
- **compliance gate 当前处于 RED 语义**（下一次 push/PR 至 master 时 `.github/workflows/compliance.yml` `compliance` job 将 FAIL）。本计划起草时实测 `bash docs/audits/nop-compliance-checker.sh` 汇总表对照 `docs/audits/compliance-baseline.md ## BASELINE (machine-readable)`：

  | 规则 | R6.8 基线 | 当前 actual | delta | 门控判定 |
    |------|-----------|------------|-------|---------|
    | R2b（BizModel daoFor(Erp*)） | 240 | **229** | **−11** | 改善（自动 PASS，鼓励回写） |
    | R2c（全生产代码 daoFor() 总量） | 1380 | **1382** | **+2** | ❌ REGRESSION（CI fail） |
    | R2d（Processor daoFor(ErpMd*)） | 32 | **34** | **+2** | ❌ REGRESSION（CI fail） |
    | 其余 16 规则（R1a-c/R1d/R2a/R3/R4/R5/R6/R7/R8/R10/R11/R12a-c） | — | — | 0 | ✅ |

- **基线最后裁决源**：plan `2026-08-01-0656-1`（R6.8 全量验证 + compliance 集中裁决），落 `compliance-baseline.md` `## R6.8 MR6 后 compliance 基线集中裁决注记` + `## BASELINE (machine-readable)`（R2b=240 / R2c=1380 / R2d=32）。锚定 commit `252a6a387`。
- **漂移源（git diff 252a6a387..HEAD 实测，prod 非 test）**：post-R6.8 的 R6.9 补拆 per-mutation Processor（commits `fb5e7d5c3`/`99c42f6da`/`6aaaf6bcb`）+ 其后 MQ 生产侧触及。`git diff 252a6a387..HEAD -- '*.java'` 显示新增 `daoProvider.daoFor(...)` 站点集中于：
  - `ErpFinBudgetScenarioRollForwardProcessor`（5 处 daoFor）
  - `ErpFinBudgetScenarioCarryForwardProcessor`（9 处 daoFor）
  - `ErpInvCostingReclosePeriodCostsProcessor`（~15 处 Inv daoFor：StockLedger/StockBalance/StockMove/CostLayer）
  - `ErpMdSupplierApprovalSuspendByPartnerProcessor`（**2 处 `daoFor(ErpMdSupplierApproval.class)`** = R2d +2 的直接来源，ErpMd* 前缀）
  - 对应 BizModel（`ErpInvCostingBizModel.reclosePeriodCosts` 等）daoFor 下移到 Processor → R2b −11（BizModel 失去 daoFor 站点），同 daoFor 在 Processor 侧重计 → R2c 净 +2。
- **为何 R6.9/MQ closure 未捕获**：各 R6.9/MQ 计划日志记录「compliance exit 0」指的是 **checker 脚本退出码**——checker 是**纯 reporter**（`compliance-baseline.md §回归门控规则` + `compliance.yml` 注释：option b「gate 逻辑在 CI / checker 保持纯 reporter」），脚本退出码恒为 0，不反映 actual vs baseline。真正的门控判定在 `compliance.yml` `Enforce baseline gate` step（python 解析 actual > baseline => `sys.exit(1)`）。R6.9/MQ closure 误把脚本退出码当作门控通过，致漂移累积——即 `project-context.md §已知失败模式（Compliance 基线漂移）` 描述的复发模式。
- **剩余差距**：3 项需裁决（R2b −11 回写 / R2c +2 裁决 / R2d +2 裁决），并恢复 CI gate green 语义。

## Goals

- 对 R2c +2 / R2d +2 漂移逐站点裁决：每个 net-new daoFor 站点分类为 `Fix`（重构消除，对齐 `2026-07-24-0605-3`/`2026-07-24-2000-1` ORM `<to-one>` getter 或 I*Biz 先例）或 `baseline-raise`（合法 per-mutation Processor `dao()` 契约 / 跨域只读聚合，对齐 R6.8 `+130`/`+4` 与 `1057-2` `+149` 先例）。
- 在同一裁决中回写 R2b 改善（240→229），对齐 R6.8 同计划回写 R1d/R2a/R2b 的先例。
- 恢复 CI compliance gate green 语义（全 19 规则 actual ≤ updated baseline）。
- 经独立子代理草案审查 + 独立结束审计（遵循 `compliance-baseline.md §回归门控规则`「调高基线的唯一途径：开独立计划……逐项人工确认」）。

## Non-Goals

- 不引入新的 compliance checker 校准（不改 `nop-compliance-checker.sh` 逻辑）——纯基线裁决。
- 不处理被外部条件门控的 successor（生产部署 / NVD key / nightly 累积 ≥14/30 / 团队决议 / 平台级 nop-entropy 改造 / 框架迁移）——这些均经核验触发条件未满足。
- 不重开已 done 的 MQ 工作项。
- 不重开 MR6（CLOSED）。
- 不改 R6.9 已落地的 per-mutation Processor 业务语义（仅评估 daoFor 可重构性；若重构则语义等价）。

## Task Route

- Type: `verification or audit work`（compliance 基线裁决，遵循 3 例先例：`1057-1` / `1705-2` / `0656-1`）
- Owner Docs: `docs/audits/compliance-baseline.md`（权威基线 + `## BASELINE (machine-readable)` 机器可读块 + `## 回归门控规则`）+ `docs/context/project-context.md §已知失败模式` + `docs/architecture/processor-extension-pattern.md`（per-mutation Processor `dao()` 契约合法性背书）+ `docs/architecture/data-dependency-matrix.md`（跨域只读聚合合法性）
- Skill Selection Basis: `nop-debugging`（git diff 逐站点归因 + checker 输出复核）+ `nop-backend-dev`（评估每个 daoFor 站点的 Fix 可行性——ORM `<to-one>` getter 替代 vs I*Biz 注入 vs 接受为 Processor 契约产物）。无需审计 skill——这是机械化基线裁决，非新维度审计。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（checker `docs/audits/nop-compliance-checker.sh` + CI gate `.github/workflows/compliance.yml` 已就绪并激活）。
- 无 ORM 变更（R6.9 Processor 已建模，无新 ORM 关系需求；若 Phase 1 识别 Fix 候选且该 Fix 需 ORM `<to-one>`，则该 ORM 关系应已存在——R6.x 收尾后 `getEntityById(FK)` chained/variable-split 两形态全域清零，仅余已登记豁免）。

## Execution Plan

### Phase 1 - 逐站点漂移归因 + Fix-vs-raise 裁决

Status: completed
Targets: `docs/audits/compliance-baseline.md`（只读核验）+ 受影响 R6.9/MQ 生产文件（只读核验）
Skill: `nop-debugging` + `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无

- [x] `Proof`：复跑 `bash docs/audits/nop-compliance-checker.sh` 捕获完整汇总表 + 逐规则输出；确认当前 actuals（R2b=229 / R2c=1382 / R2d=34）与 R6.8 基线 delta（−11 / +2 / +2）。
  - Skill: `nop-debugging`
- [x] `Proof`：逐站点 git 归因 `git diff 252a6a387..HEAD -- '*.java'`（排除 `*/src/test/*`）：
  - (a) R2d +2：定位全部 `daoFor(ErpMd*.class)` 的 net-new Processor 站点（预期 = `ErpMdSupplierApprovalSuspendByPartnerProcessor:47,74` 2 处），逐处记录 file:line + 源 commit + 同域/跨域。
  - (b) R2c +2 净：对账 net-new daoFor 站点 vs BizModel 侧移除站点（R2b −11 来源），确认净 +2 的真实分布（哪几个 Processor 的 `dao()` 契约方法或新 inline daoFor 是净增）。
  - (c) R2b −11：枚举 BizModel 侧被移除的 11 个 daoFor(Erp*) 站点（R6.9 BizModel→Processor 下移 + 任何 MQ 生产侧重构），确认全部为合法下移。
  - 产出 file:line 清单 + 源计划 + 同/跨域分类表，落 plan `## Phase 1 Evidence`。
  - Skill: `nop-debugging`
- [x] `Decision`：对每个 net-new daoFor 站点裁定 `Fix` vs `baseline-raise`，记录选择、替代方案、残留风险：
  - 预期裁定方向（Phase 1 实测确认）：R2d +2（`ErpMdSupplierApproval` 2 处，master-data **同域** Processor 读取本域实体）= `baseline-raise`（同域合法，非跨域违规，对齐 R6.8「Processor 读取 master-data 实体」先例但本处为域内）；R2c +2 净（per-mutation Processor `dao()` 契约产物）= `baseline-raise`（对齐 1057-2/R6.8 接受基线上调选 (c)）；R2b −11 = 改善回写。若发现可机械重构为 ORM `<to-one>` getter 的 Type-1 站点（对齐 `2026-07-24-0605-3`/`2000-1` 先例），则裁定 `Fix` 并在 Phase 2 执行。
  - Skill: `nop-backend-dev`

### Phase 1 Evidence — 逐站点漂移归因 + Fix-vs-raise 裁决

> 可复现命令（2026-08-02 实仓，HEAD=`70fe7dbed`）：
> - checker 汇总：`bash docs/audits/nop-compliance-checker.sh` → R2b=229 / R2c=1382 / R2d=34（其余 16 规则 = baseline）
> - 漂移源对账：`git diff 252a6a387..HEAD -- '*.java' ':!*/src/test/*' | grep -E '^[+-].*daoProvider\.daoFor\('`

**(a) R2d +2 — net-new Processor `daoFor(ErpMd*)` 站点**

| file:line | 源 commit / 计划 | daoFor 实参 | 域归属 |
|-----------|------------------|------------|--------|
| `module-master-data/erp-md-service/.../processor/ErpMdSupplierApprovalSuspendByPartnerProcessor.java:47` | `6aaaf6bcb` / plan `2026-08-01-0803-1`（R6.9 master-data SupplierApproval suspendByPartner 补拆） | `ErpMdSupplierApproval` | **同域**（master-data Processor 读 master-data 实体）|
| `module-master-data/erp-md-service/.../processor/ErpMdSupplierApprovalSuspendByPartnerProcessor.java:74` | 同上 | `ErpMdSupplierApproval` | **同域** |

- `:47`（`findActiveByPartner` protected step）= 按 partnerId+status 批量查询有效资格；`:74`（`doSuspend` protected step）= 状态翻转 + `updateEntity`。
- 0 处 ErpMd* 移除（git diff ErpMd removals 为空）→ R2d delta 完全 = 这 2 处 net-new。

**(b) R2c +2 净 — net-new daoFor 站点 vs BizModel 侧移除站点对账**

| 文件 | 角色 | ADD | RM | 净 | 性质 |
|------|------|-----|----|----|------|
| `ErpFinBudgetScenarioRollForwardProcessor`（fin，新） | per-mutation Processor | 5 | 0 | +5 | R6.9 finance BudgetScenario 补拆（commit `fb5e7d5c3`） |
| `ErpFinBudgetScenarioCarryForwardProcessor`（fin，新） | per-mutation Processor | 9 | 0 | +9 | 同上 |
| `ErpFinBudgetScenarioProcessor`（fin，facade） | 旧 monolithic Processor | 0 | 14 | −14 | rollForward/carryForward 内联逻辑下移到 2 新 Processor（对齐 per-mutation 架构） |
| `ErpInvCostingReclosePeriodCostsProcessor`（inv，新） | per-mutation Processor | 11 | 0 | +11 | R6.9 inventory InvCosting 补拆（commit `99c42f6da`） |
| `ErpInvCostingBizModel`（inv） | BizModel | 0 | 11 | −11 | reclosePeriodCosts D-mutation 下移到 Processor（= R2b −11 来源） |
| `ErpMdSupplierApprovalSuspendByPartnerProcessor`（md，新） | per-mutation Processor | 2 | 0 | +2 | R6.9 master-data SupplierApproval 补拆（commit `6aaaf6bcb`） |
| **合计** | | **27** | **25** | **+2** | 与 checker R2c delta (+1382−1380=+2) 精确吻合 |

- finance BudgetScenario：+14（2 新 Processor）−14（旧 facade）= **0 净**（完美平衡的 per-mutation 拆分迁移）
- inventory InvCosting：+11（新 Processor）−11（BizModel）= **0 净**（完美平衡的 BizModel→Processor 下移）
- master-data SupplierApproval：+2 −0 = **+2 净** ← **R2c +2 全部来自这 2 处**（与 R2d +2 同源，即 (a) 表的 2 站点）

**(c) R2b −11 — BizModel 侧被移除的 `daoFor(Erp*)` 站点**

11 处全部位于 `module-inventory/.../costing/ErpInvCostingBizModel.java`，全部为 `ErpInv*` 同域实体（ErpInvStockLedger ×3 / ErpInvStockBalance ×2 / ErpInvStockMove / ErpInvStockMoveLine / ErpInvCostLayer ×4），随 `reclosePeriodCosts` D-mutation 下移到 `ErpInvCostingReclosePeriodCostsProcessor`（R6.9 inventory 补拆，commit `99c42f6da`）。BizModel→Processor 下移是 per-mutation 架构的合法重构（对齐 R6.8 MR6 大规模 BizModel→Processor 下移先例，那次下移产生 R2b 325→240 的 −85 改善）。全部为合法下移，无 B 类「应回提 BizModel」候选。

**Fix-vs-raise 裁决（每个 net-new daoFor 站点）**

| 站点 | 选择 | 替代方案（已否决） | 残留风险 |
|------|------|-------------------|----------|
| `ErpMdSupplierApprovalSuspendByPartnerProcessor:47`（query 同域 ErpMdSupplierApproval） | **baseline-raise** | (a) 注入 `IErpMdSupplierApprovalBiz` —— **否决**：`ErpMdSupplierApprovalBizModel:53` 已 `@Inject` 本 Processor，反向注入形成循环依赖（类 javadoc 明示此约束）；(b) ORM `<to-one>` getter —— **不适用**：按 partnerId+status 的批量查询，非 FK 导航；(c) CrudBizModel `dao()` —— **不适用**：Processor 非 CrudBizModel 子类，无 `dao()` 方法 | 同域 Processor 读本域实体非 R2d 原始语义（跨域违规）关注的对象；与 R6.8 接受的 32 处 ErpMd* Processor 站点同型（本处更轻——同域而非跨域）。无功能/语义风险 |
| `ErpMdSupplierApprovalSuspendByPartnerProcessor:74`（updateEntity 同域 ErpMdSupplierApproval） | **baseline-raise** | 同上（循环依赖 / 非 FK / 无 dao()） | 同上 |
| R2c +2 净（= 上 2 站点） | **baseline-raise** | 全部对齐 R6.8 +130 / 1057-2 +149 先例：per-mutation Processor `daoProvider.daoFor(<EntityClass>)` 是 Nop 平台读取托管实体 DAO 的标准方式（抽象基类/编排骨架契约），非业务跨域编排 | 无（BudgetScenario/InvCosting 迁移为 0 净，不贡献 delta） |
| R2b −11 | **改善回写**（240→229） | 不适用（下降自动 PASS，回写反映真实代码计数） | 无 |

**结论**：3 项 delta 全部裁决为 `baseline-raise`（R2c/R2d）+ `改善回写`（R2b），**0 处 Fix 候选**。R2d +2 为同域 master-data Processor 读本域实体（比 R6.8 接受的跨域 ErpMd* 站点更轻）；R2c +2 净全部来自该同域 Processor 的 2 处 daoFor，finance/inventory 的 per-mutation 拆分为完美平衡迁移（0 净贡献）。源计划 `2026-08-01-0803-1`（R6.9 successor）已经独立草案审查 + 结束审计，其生产代码变更已经审计验证为合法 per-mutation 架构（MR6 已 CLOSED）。裁决对齐 3 先例（`1057-1` / `1705-2` / `0656-1`）的 baseline-raise 范式。

Exit Criteria:

- [x] `## Phase 1 Evidence` 节落盘：逐站点 file:line + 源 commit/计划 + 同/跨域分类 + Fix-vs-raise 裁定 + 理由，覆盖 R2b −11 / R2c +2 / R2d +2 全部 delta。
- [x] Fix-vs-raise 裁决对每个 net-new 站点记录选择 + 替代方案 + 残留风险（Decision 完整性）。

### Phase 2 - 应用 Fix（若有）+ 更新 BASELINE 块 + 裁决注记

Status: completed
Targets: `docs/audits/compliance-baseline.md`（`## BASELINE (machine-readable)` yaml 块 + 新增裁决注记节）；若 Phase 1 裁定 Fix，则含 Fix 目标 `.java` 文件
Skill: `nop-backend-dev`（仅当 Fix 应用）

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 1 裁决完成

- [x] `Fix | Add`：若 Phase 1 识别 Fix 候选，应用 ORM `<to-one>` getter / I*Biz 重构（语义等价）；否则跳过（无 Fix）。
  - Skill: `nop-backend-dev`
  - **结果**：**跳过（无 Fix）**。Phase 1 裁决 = 0 处 Fix 候选（R2d +2 同域站点无 ORM `<to-one>` getter 可替代[非 FK 导航] + 无 I*Biz 可注入[BizModel 已 @Inject 本 Processor，反向注入成循环依赖] + Processor 非 CrudBizModel 子类故无 `dao()`）。全部裁定 baseline-raise（R2c/R2d）+ 改善回写（R2b）。
- [x] `Add`：更新 `docs/audits/compliance-baseline.md`：
  - (a) 新增「post-R6.8 compliance 基线漂移裁决注记（plan 2026-08-02-0651-1）」节，含漂移源（R6.9 per-mutation Processor + MQ 生产侧）+ 逐站点证据摘要 + 源计划 + Fix-vs-raise 分类 + Decision 理由。
  - (b) 更新 `## BASELINE (machine-readable)` yaml 块：R2b 240→**229**（改善回写）、R2c 1380→**裁决值**、R2d 32→**裁决值**（裁决值 = Phase 1 实测确认的 actual；若全部 baseline-raise 则 = 1382 / 34；若有 Fix 则按 Fix 后重测值）。
  - Skill: none
  - **结果**：(a) 裁决注记节已加（漂移源对账表 + R2b/R2c/R2d 三行裁决表 + 合法性分类 + 漂移未捕获根因 + 0 Fix 候选说明）；(b) BASELINE 块更新：R2b 240→**229** / R2c 1380→**1382** / R2d 32→**34**（= Phase 1 实测 actuals，全部 baseline-raise/改善回写，无 Fix 故按原值）。
- [x] `Proof`：复跑 `bash docs/audits/nop-compliance-checker.sh` + 模拟 `compliance.yml` 门控逻辑（python 解析 actual vs 更新后 BASELINE 块）→ 全 19 规则 actual ≤ baseline → PASS。
  - Skill: `nop-debugging`
  - **结果**：见 Exit Criteria 证明（checker 复跑 + python 门控模拟全 PASS）。

Exit Criteria:

- [x] `## BASELINE (machine-readable)` yaml 块更新（R2b/R2c/R2d 三行），与新裁决注记节内部一致。
- [x] 模拟门控 PASS（actual ≤ updated baseline，零裸 REGRESSION）。
- [x] 若应用了 Fix，该 Fix 目标的 scoped `mvn compile`/`mvn test` 通过（语义等价，零回归）。
  - **结果**：N/A（0 Fix 应用，零生产代码变更）。本计划纯文档/基线裁决，跳过全量 mvn 的理由：Non-Goals 守约（零 Java/ORM/契约变更），checker + 门控模拟足以验证合规语义恢复。

## Draft Review Record

- Independent draft review iteration 1: **accept** (`ses_03f106319ffe71b5wX660DqMEX`, fresh cold-context subagent) — 独立审查者对 LIVE 仓库逐项核验全部事实主张（actuals 229/1382/34、baselines 240/1380/32、+2/−11/+2 delta、CI-red 语义、checker=pure-reporter vs CI python gate、R2d +2 单一来源=`ErpMdSupplierApprovalSuspendByPartnerProcessor:47,74`、R6.8 上一次裁决、3 先例计划存在、baseline-raise 经 `compliance-baseline.md:60` 强制要求独立计划）。VERDICT=accept，0 BLOCKER / 0 MAJOR / 3 MINOR（均非阻塞，执行期酌情处理）：MINOR-1 i18n gate 在 Closure Gates 略宽（本计划不动 view.xml，属防御性保留，非错误）；MINOR-2 `ErpInvCostingReclosePeriodCostsProcessor（~15 处）` tilde 由 Phase 1 逐站点对账消解（Exit Criteria 已强制 file:line 清单）；MINOR-3 `## Phase 1 Evidence` 节为执行期落地目标（非预 stub，符合模板行为）。无修订要求 → 共识达成，Plan Status draft→active。

## Closure Gates

> 仅在所有项目和每个阶段退出标准都勾选 `[x]` 后关闭。本计划主要为文档/基线裁决；若 Phase 2 应用 Fix 则含 scoped mvn 门控，否则跳过全量 mvn（无 Java 变更）。

- [x] 范围内行为完成：compliance gate 恢复 green 语义（actual ≤ updated baseline，全 19 规则）。
- [x] 相关文档对齐：`compliance-baseline.md`（BASELINE 块 + 新裁决注记节）内部一致；`project-context.md` 无需改（已知失败模式已记录）。
- [x] 已运行验证：`bash docs/audits/nop-compliance-checker.sh` + 模拟 `compliance.yml` 门控 PASS；`bash docs/audits/i18n-coverage-checker.sh` exit 0（本计划不动 view.xml）；若应用 Fix 则 scoped `mvn test` 绿；若纯文档基线裁决则说明跳过 mvn 全量的理由。
- [x] 无范围内项目降级为 deferred/follow-up（每项 net-new daoFor 站点均裁定 landed 或显式移出范围并记理由）。
- [x] 独立草案审查已完成并记录（`## Draft Review Record`）。
- [x] 文本一致性已验证：Plan Status、Phase Status、Exit Criteria、Closure Gates、`docs/logs/` 条目一致。
- [x] 结束审计由独立子代理（新会话，不重用执行者上下文）执行直至通过；执行者未自我审计、未将此留为 `[ ]` 人工门控占位符。
- [x] 结束证据存在于文件中（`## Closure` 节记录审计者 + 证据指针）。

## Deferred But Adjudicated

> 预期无降级项。若 Phase 1 发现某 Fix 候选重构成本超收益（如需 ORM 关系新建或破坏公共返回类型），在此登记为 `optimization candidate` 并命名触发条件（如「该 Processor 下次业务变更时一并重构」）。

## Closure

Status Note: 本 plan 闭合（CI compliance gate green 语义恢复 + R2b/R2c/R2d 三项 delta 经逐站点裁决落盘 + BASELINE 块更新，对齐 1057-1/1705-2/0656-1 三先例）。漂移源（R6.9 per-mutation Processor 补拆，plan `2026-08-01-0803-1`）已经独立草案审查 + 结束审计，其生产代码变更已经审计验证为合法 per-mutation 架构。零生产代码变更（Non-Goals 守约），纯文档/基线裁决。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话 `ses_03f057b98ffeYHXNCwZrc90Ij1`，cold context，read-only，执行者未自我审计）
- Evidence: VERDICT: **PASS**。全 10 项复核全 OK（0 BLOCKER / 0 阻塞）：
  1. Plan/Phase 状态一致性——Phase 1/2 均 `Status: completed`，全 6 Phase items `[x]` + 全 5 Exit Criteria `[x]`，零残留 `[ ]`；
  2. `### Phase 1 Evidence` 节完整——R2d +2 per-site 表 + R2c 净对账表 + R2b −11 枚举 + Fix-vs-raise decision 表（含替代方案/残留风险）均在位；
  3. `compliance-baseline.md` BASELINE 机器可读块 R2b=229 / R2c=1382 / R2d=34，其余 16 规则不变；新裁决注记节在位；
  4. checker 复跑 exit=0，actuals R2b=229 / R2c=1382 / R2d=34 与更新后 BASELINE 块逐行精确匹配；
  5. **CI 门控模拟 PASS**——`compliance.yml` `Enforce baseline gate` python 逻辑复跑 `REGRESSIONS: NONE` / `GATE: PASS` / exit=0，零规则 actual > baseline；
  6. i18n checker `--strict` exit=0，DEFECTS=0 / COVERAGE GAPS=0；
  7. `git diff --stat -- '*.java'` 空——**零生产代码变更**（Non-Goals 守约，mvn 全量跳过有据）；
  8. git 归因精度复核——`daoFor(ErpMd` net-new=2 ✓，`daoProvider.daoFor(` total=52（27 add+25 rm=net+2）✓，`ErpMdSupplierApprovalSuspendByPartnerProcessor:47,74` 两处 `daoFor(ErpMdSupplierApproval.class)` 实仓证真，文件在 `module-master-data` = **同域**（裁决关键点确认）；
  9. roadmap `audit-remediation-roadmap.md` 头部 v28 版本注记在位；
  10. Closure Gates #7/#8 执行者正确留 `[ ]` 待本审计（未自我审计）。

Follow-up:

- 无新 successor——本裁决是机械基线对账，不产生新的外部条件门控项。recurrence 防护已由 `project-context.md §已知失败模式（Compliance 基线漂移）` + 本 plan + `0656-1 §纪律强化` 承载。
