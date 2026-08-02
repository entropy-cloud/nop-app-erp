# 2026-08-01-0656-1 R6.8 MR6 全量验证 + 完成判据核验 + compliance 基线集中裁决

> Plan Status: completed
> Last Reviewed: 2026-08-01
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR6 R6.8（唯一剩余 `todo` 工作项）
> Related: `docs/plans/2026-07-31-2115-1`~`2026-08-01-0001-3`（R6.1–R6.7 per-domain 拆分计划，全 completed）；`docs/plans/2026-07-31-1705-2`（MV V.1+V.2 全量绿基线 + compliance 基线裁决，最近一次全量验证锚点）；`docs/plans/2026-07-25-1057-2`（per-mutation 拆分先例 + R8 checker 二次校准）；`docs/plans/2026-07-25-1057-1`（compliance 基线漂移裁决范式）
> Audit: required

## Current Baseline

- **R6.1–R6.7 全部 completed**：256 个须拆 mutation（类别 A 92 + 类别 B 164）已全部拆为独立 `<Entity><Method>Processor`（self-contained `process()` + protected step，非空心委托），对应 BizModel 改 `@Inject` per-mutation Processor + 单行委托；类别 A 多 mutation 共用 facade 已瘦身（delete-after-extract / slim-to-S-delegation）；合法豁免 77 项登记于 `docs/architecture/processor-per-mutation-exemption-registry.md`。各子批次闭合时均跑分域 `mvn test`（全 0 failures）+ 全量 `mvn clean install -DskipTests`（156 模块 BUILD SUCCESS）。
- **R6.x plans 显式 defer 到 R6.8 的 compliance 漂移**（多份计划 closure 注记 "归 R6.8 全量验证工作项"）：R8/R2c/R2d 累积漂移。本 plan 起草时实测 `bash docs/audits/nop-compliance-checker.sh` 汇总（2026-08-01）：
  | 规则 | baseline | actual | delta | 性质 |
  |------|----------|--------|-------|------|
  | R8（Processor 无 xbiz 接线） | 42 | **248** | +206 | **false positive**：大部分 MR6 新 per-mutation D-mutation Processor 是 self-contained（含 `process()` + protected step），不继承 `Abstract*Processor`，故不命中 R8 既有排除模式（`:289 grep extends Abstract*Processor`）。它们经 BizModel `@Inject` 路由消费（非 xbiz），R8 原始语义不覆盖。delta = +206 净增（原 42 基线中部分被 R6.x delete-after-extract 消解，新 per-mutation false positive 与原基线消解项之差 = 206——Phase 1 将逐站点精确分类，不预设全部 248 均为 per-mutation false positive）。|
  | R2c（全生产代码 daoFor 总量） | 1250 | **1380** | +130 | **合法增长**：每个新 per-mutation Processor 实现自身 `dao()` 方法 `return daoProvider.daoFor(<EntityClass>.class)`（抽象基类编排骨架契约），对齐 `1057-2` 先例（149 S-mutation Processor 曾 +149）。|
  | R2d（Processor daoFor(ErpMd*)） | 28 | **32** | +4 | **合法增长**：少数新 per-mutation Processor 读取 master-data 实体（跨域只读聚合）。|
  | R1d | 17 | 14 | -3 | **改善**（R6.x 重构移除部分 BizModel findAllByQuery）|
  | R2a | 38 | 34 | -4 | **改善**（BizModel ErpMd* daoFor 下移到 Processor）|
  | R2b | 325 | 240 | -85 | **改善**（BizModel 跨域 daoFor 大量下移到 Processor）|
  | R3/R5/R6/R7/R10/R11/R12a/R12b/R12c | 见基线 | = 基线 | 0 | 无漂移 |
- **R8 checker 排除逻辑现状**（`nop-compliance-checker.sh:287-298`）：`find` 收集全部 `*Processor.java`（排除 test/ + module-common-service/），逐文件 grep `extends Abstract[A-Z][a-zA-Z]*Processor` 早退（仅排除 MR5 的 149 S-mutation 子类），剩余若无 `${base}.xbiz.xml` 即计数。MR6 的 per-mutation D-mutation Processor 不继承抽象基类（直接 `process()` + protected step），全部漏入计数。
- **全量测试基线**：最近一次全量 `mvn test` 绿为 MV V.1（plan `2026-07-31-1705-2`，2026-07-31 执行）= 0 failures / 0 errors / 1 skipped（`ErpAllWebPagesCollectTest @Disabled`）/ 1902 单测方法。**已知日期边界风险**：今日为 2026-08-01，多份 R6.x plan 注记 finance/mfg/assets/pur/sal 域存在 `YearMonth.now()` 7→8 月翻滚致日期敏感测试快照漂移（如 `TestErpFinBadDebtReversal` / `TestErpMfgSubcontractReverse` / `TestErpAstMaintenance`），各子批次用「install 上游 -DskipTests 隔离」或「重录 8 月基线」处理。全量 `mvn test` 将集中暴露这些日期敏感失败。
- **完成判据尚未核验**（roadmap §MR6 用户明确要求）：所有模块所有 BizModel 的 `@BizMutation` 调用满足 `processor-extension-pattern.md`——即零"多 mutation 共用一个 Processor"（类别 A :42 违规）+ 零"≥3 步 mutation 内联在 BizModel 无 Processor"（类别 B :5/:7 违规）。该判据的 grep 校验是本 plan 的核心交付。

## Goals

- **G1（完成判据核验）**：通过可复现的 grep/启发式校验，证明仓库满足 MR6 完成判据——零裸 facade 持有 ≥2 个 D-mutation（类别 A `:42` 违规），零 BizModel 内联 ≥3 步 `@BizMutation`（类别 B `:5/:7` 违规）。剩余 mutation 全部要么已拆为 per-mutation Processor，要么登记于 exemption registry（`:44-47` 合法豁免：纯查询 ≤2 步 / 单步状态翻转 / 标准 CRUD）。
- **G2（compliance 基线集中裁决）**：裁决 R6.x 累积的 compliance 漂移（R8 false positive + R2c/R2d 合法增长）——R8 checker 校准使其反映真实架构语义（per-mutation Processor 经 BizModel @Inject 路由非 xbiz），R2c/R2d 基线裁决性上调带 per-site 证据；R1d/R2a/R2b 改善回写基线。裁决后 checker 全规则 actual ≤ baseline，CI green。
- **G3（全量验证）**：全量 `mvn clean install -DskipTests`（156 模块）+ `mvn test`（0 failures/0 errors）全绿；日期敏感快照漂移按既有范式处理（重录 8 月基线 or 确认 pre-existing）。
- **G4（bookkeeping）**：arm-index P1-MA3-062 状态回填 `done (R6.8)`；roadmap R6.8 `todo`→`done`；MR6 milestone 闭合注记；`docs/logs/2026/08-01.md` 聚合条目。

## Non-Goals

- **不重开 MR5**：S-mutation 拆分状态保持 done，本 plan 不动 S-mutation Processor。
- **不新增 per-mutation Processor 拆分**：R6.1–R6.7 已完成全部 256 须拆 mutation；本 plan 仅验证 + 裁决，不新增拆分工作（若验证发现遗漏，登记为 successor，不在本 plan 内补拆）。
- **不处理 R3.x successor**（employee-id data-auth / SoD 全域铺开 / `_helper.ts` 解除）：这些是 optimization candidate，无本 plan 触发的 re-trigger 条件。
- **不做 R2c successor (a)/(b)**（AbstractProcessor 泛型重构消除 daoFor 契约产物）：1057-2 裁决选 (c) 接受基线上调，本 plan 维持该裁决。
- **不触及 ORM 模型 / api.xml / view.xml**：本 plan 是验证 + checker 脚本（`docs/audits/`）+ 基线文档（`docs/audits/compliance-baseline.md`）+ 索引/日志，零生产业务代码变更。

## Task Route

- Type: `verification or audit work`（MR6 完成判据核验 + compliance 基线裁决）+ 局部 `implementation-only change`（R8 checker 脚本校准 + 基线文档回写）
- Owner Docs: `docs/architecture/processor-extension-pattern.md`（完成判据真相源 `:7/:29/:42/:44-47`）；`docs/audits/compliance-baseline.md`（基线门控真相源）；`docs/audits/arm-index.md`（P1-MA3-062）；`docs/architecture/processor-per-mutation-exemption-registry.md`（合法豁免登记）
- Skill Selection Basis: `nop-backend-dev`（理解 Processor 编排范式 + per-mutation 架构，用于设计完成判据校验启发式与 R8 校准语义判断）；compliance checker 校准范式为既有先例链（`1057-1`/`1057-2`/`0823-1`/`0941-2`，非 registered skill，作为方法论参考引用）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 本 plan 不引入端口/环境变量/外部服务。

## Execution Plan

### Phase 1 — 完成判据 grep 核验 + compliance 漂移精确定盘

Status: completed
Targets: `docs/architecture/processor-extension-pattern.md`（判据引用）；`docs/architecture/processor-per-mutation-exemption-registry.md`（豁免交叉核对）；实仓 BizModel/Processor 全域
Skill: `nop-backend-dev`

- Item Types: `Proof`
- Prereqs: R6.1–R6.7 completed（已满足）

- [x] **类别 A 核验（:42 零多 mutation 共用 facade）**：grep/启发式扫描全部 `<Entity>Processor` facade（排除 per-mutation 子类与 module-common-service 抽象基类），逐一确认 public D-mutation 入口方法数 ≤1（剩余合法的仅 S-mutation 单行委托 / `:45` 只读查询 / `:46` 单步状态翻转 / protected helper）。任何仍持有 ≥2 个 D-mutation public 入口的 facade = 完成判据违反，须登记（登记为 successor 补拆，不在本 plan 内实施）。
  - Skill: `nop-backend-dev`
- [x] **类别 B 核验（:5/:7 零 ≥3 步内联 @BizMutation）**：grep 全域 `*BizModel.java` 中 `@BizMutation` 注解方法，逐方法启发式判定步骤数（语义语句计数，含跨实体写/多步编排）。≥3 步且无 Processor 委托（方法体非单行 `return ...Processor.method(...)`）= 完成判据违反，须登记。≤2 步 / 单步状态翻转经 exemption registry 交叉核对确认合法豁免。
  - Skill: `nop-backend-dev`
- [x] **合规豁免完整性交叉核对**：确认 exemption registry 中 77 项合法豁免（类别 B 70 + 类别 A 7 查询）在实仓存在且仍符合 `:44-47` 豁免边界（纯查询 ≤2 步 / 单步状态翻转 / 标准 CRUD）；无"登记豁免但实为 ≥3 步"的误豁免。
  - Skill: `nop-backend-dev`
- [x] **compliance 精确 actual 盠点**：复跑 `bash docs/audits/nop-compliance-checker.sh`，记录全 16 可计数规则精确 actual，与 baseline 逐行比对，产出漂移清单（R8/R2c/R2d 增量 + R1d/R2a/R2b 改善）。R8 增量逐站点分类（per-mutation false positive vs 真实违规）。
  - Skill: none

Exit Criteria:

> 本阶段交付完成判据核验结论 + compliance 漂移精确清单，为 Phase 2 裁决提供输入。本地化检查（grep 命令可复现）。

- [x] 类别 A + 类别 B 核验结论明确：要么"完成判据满足（零违规）"，要么"发现 N 处遗漏，登记为 successor"——两种结论均可推进 Phase 2/3
- [x] compliance 漂移清单（R8/R2c/R2d 增量精确值 + R8 false positive 分类）已产出，可复现 grep 命令记录在案

### Phase 1 Evidence — 完成判据核验结论：发现遗漏，登记 successor（MR6 milestone 保持 OPEN）

> 可复现 grep（2026-08-01 实仓）。结论 = **完成判据未完全满足**（R6.0 triage 2 处误移除 + 1 处域外遗漏 + 1 处登记缺口），登记 R6.9 successor 补拆，MR6 milestone 按 Phase 3 条件分支保持 OPEN。

**类别 A 核验（facade public D-mutation 入口方法数）**——全域 `*Processor.java`（排 test/_gen/module-common-service/`extends Abstract*Processor`）逐 facade 列 public IServiceContext 方法名，扣 S-mutation 六动作（approve/reject/submitForApproval/reverseApprove/withdrawApproval/cancel，含 `submit`→`submitForApprovalProcessor` 单行委托）+ registry §B 查询/单步翻转豁免后，剩 D-mutation 入口数：

| Facade | D-mutation 入口 | 判定 |
|--------|----------------|------|
| `module-finance/.../budget/ErpFinBudgetScenarioProcessor` | rollForward（`:104`）+ carryForward（`:136`）= **2** | **:42 违规**（generateBudgetVoucher/reverseBudgetVoucher 为 protected helper，不计入口）。R6.0 triage（plan 2026-07-31-2109-1 line 100）误移除，理由"同因 S-mutation 纯委托 D=0"实误——rollForward/carryForward 为内联 D-mutation（10+ 语句，含 BigDecimal 凭证生成/红冲 + RollforwardLog/CarryForwardLog 创建），`ErpFinBudgetScenarioBizModel:81/82` rollForward + `:86` carryForward 经 `@BizMutation` → `budgetScenarioProcessor.*` 路由。无 per-mutation Processor（仅 4 个 S-mutation 子类 SubmitForApproval/Approve/Reject/Cancel）。登记 successor。 |
| `module-finance/.../posting/ErpFinPostingProcessor` | process（`:126`）+ reverseProcess（`:209`）= **2** | **边界 :42**：`ErpFinVoucherBizModel:69-73` post + `:78-83` reverse 经 `@BizMutation` → `postingProcessor.*`。R6.0 triage 误移除（同 line 100）。但本类是 `processor-extension-pattern.md:66` 明示的"业财过账引擎（IErpFinVoucherBiz + ErpFinPostingProcessor）"canonical Facade+Processor 范例（forward/reverse 对称逆操作 + 全域过账共享引擎，非无关 mutation 拼装）。登记 successor 裁决=登记 engine 豁免 OR 拆 process/reverseProcess 两 Processor（pattern doc 背书倾向于豁免登记）。 |
| 其余 facade（ErpPurOrder/ErpSalOrder/... 等 25 个 slim-to-S-delegation + ErpPurRequisition[convertToOrder R6.8 backstop 单 D=1] + ErpMfgWorkOrder[checkAvailability :45] + ErpAstCip[findCostItems/findProgressBillings :45] + ErpInvStockMove[5 trace @BizQuery + findByRelatedBill :45] + ErpInvLandedCost[allocatePreview :45] + ErpPrjProjectSettlement/ErpFinBadDebt[submit 单行委托 S] + ErpCrmConversion[getCreatedOpportunity :45]）| ≤1 | ✅ 满足 |

**类别 B 核验（BizModel `@BizMutation` 内联 ≥3 步无 Processor）**——全域 `*BizModel.java`（排 test/_gen）`@BizMutation` 方法，扣 `*Processor.` 单行委托 + registry §A 70 项豁免后：

| BizModel.method | 步数 | 判定 |
|----------------|------|------|
| `module-inventory/.../costing/ErpInvCostingBizModel.reclosePeriodCosts`（`:74-116`，`@BizModel("ErpInvCosting")` 服务型 BizObject，finance 期末结账调用）| **≥3**（嵌套循环 move→line→ledger + BigDecimal 成本法分支[层/加权平均] + appendLayer/saveOrUpdateEntity 实体写 + ormTemplate.flushSession，~40 行）| **:5/:7 违规**——无 Processor 委托。R6.4 inventory 域外遗漏（R6.0 triage 未扫 `costing/` 包，仅扫 `entity/`）。登记 successor。 |
| `module-master-data/.../ErpMdSupplierApprovalBizModel`（apply/approve/probate/suspend/reinstate/reject 6 项 + suspendByPartner）| 6 项 = :46 单步状态翻转（require+守卫+setStatus+updateEntity，approve 含 `requireQualificationValid` 属 `validate*` 允许；`doSuspend` helper 亦单步翻转）；**suspendByPartner** = 批量循环（findActiveByPartner + for-doSuspend + return count）= 边界（循环 :46 翻转）| **登记缺口 + 边界**：整个 entity 未入 registry §A（master-data 无 §A 段，R6.7 master-data 仅拆 ErpMdCurrency.refreshRatesFromApi 1 项，ErpMdSupplierApproval 全 entity 域外遗漏）。6 项判 :46 合法豁免（补登记）；suspendByPartner 批量循环登记 successor 复议（循环 :46 vs ≥3 步批处理）。 |
| 其余 12 个无 Processor 委托 BizModel（ErpPrjTask 4 / ErpQaAction 3 / ErpInvStockTake 3 / ErpB2bPartnerProfile 3 / ErpMfgForecast 2 / ErpApsSchedule 2 / ErpPurRfq 1 / ErpPurQuotation 1 / ErpMntEquipment 1 / ErpHrTimesheet 1 等）| ≤2 / 单步翻转 | ✅ 与 registry §A 逐项精确匹配（projects 4 / quality 3 / inventory 3 / b2b 3 / mfg 2 / aps 2 / purchase 2 / maintenance 1 / hr 1），合法豁免 |

**合规豁免完整性**：registry §A 70 项 + §B 7 查询/单步翻转 facade 均在实仓存在且仍符合 `:44-47` 边界（无"登记豁免但实为 ≥3 步"误豁免）。**新增缺口**：ErpMdSupplierApproval 6 项 :46 翻转未登记（补登记入 registry §A master-data 段）。

**compliance 精确 actual 盘点**（`bash docs/audits/nop-compliance-checker.sh` 2026-08-01 实测汇总，与 baseline 逐行比对）：

| 规则 | baseline | actual | delta | 性质 | 处置（Phase 2） |
|------|----------|--------|-------|------|----------------|
| R8 | 42 | **248** | +206 | **false positive**：206 个 MR6 per-mutation D-mutation Processor（self-contained `process()` + protected step，不 `extends Abstract*Processor`）经 BizModel `@Inject` 路由（非 xbiz），R8 原始语义不覆盖。逐站点分类：全域 248 个 R8 命中 = 21 域 `Abstract*Processor`（abstract 基类，R6.7）+ 26 slim-to-S-delegation facade（BizModel 路由）+ 201 per-mutation Processor（BizModel `@Inject` 路由）= **全部被消费（0 真孤儿）**。R8 checker 校准=option (a)「排除被任何其他文件引用的 Processor」→ actual 回落 0。| checker 校准 + baseline=0 |
| R2c | 1250 | **1380** | +130 | **合法增长**：256 个 MR6 per-mutation Processor 各实现 `dao()` 方法 `return daoProvider.daoFor(<EntityClass>.class)`（抽象基类编排骨架契约，对齐 1057-2 +149 先例）；实仓 293 个生产文件含 `daoProvider.daoFor(`。| baseline 上调 1250→1380 |
| R2d | 28 | **32** | +4 | **合法增长**：32 个 Processor/Dispatcher/Engine `daoFor(ErpMd*)` 站点（全部跨域只读聚合：ErpMdSubject GL 过账 / ErpMdMaterial 成本 / ErpMdCurrency 汇率 / ErpMdEmployee 报销 / ErpMdAcctSchema 账套）。+4 新站点为 MR6 per-mutation Processor（ErpB2bAsnCreateReceiveFromAsn/ErpMdCurrencyRefreshRatesFromApi 等）。| baseline 上调 28→32 |
| R1d | 17 | **14** | −3 | **改善**（R6.x BizModel findAllByQuery 下移 Processor）| baseline 下调 17→14 |
| R2a | 38 | **34** | −4 | **改善**（BizModel ErpMd* daoFor 下移 Processor）| baseline 下调 38→34 |
| R2b | 325 | **240** | −85 | **改善**（BizModel 跨域 daoFor 大量下移 Processor）| baseline 下调 325→240 |
| R3/R5/R6/R7/R10/R11/R12a/R12b/R12c | 见基线 | = 基线 | 0 | 无漂移 | 不动 |

可复现 grep 命令记录在案（facade D-mutation 入口扫描 / BizModel `@BizMutation` + Processor 委托检测 / R8 消费引用 2-pass / R2d 站点清单）。

### Phase 2 — compliance 基线裁决 + R8 checker 校准

Status: completed
Targets: `docs/audits/nop-compliance-checker.sh`（R8 段校准）；`docs/audits/compliance-baseline.md`（BASELINE 块 + 裁决注记）
Skill: none（checker 校准范式 + 基线裁决，对齐 1057-1/1057-2/0823-1 先例）

- Item Types: `Decision | Add | Fix`
- Prereqs: Phase 1 漂移清单

- [x] **Decision: R8 checker 校准方案**。R8 当前排除仅覆盖 `extends Abstract*Processor`（MR5 S-mutation 子类），漏入 MR6 的 256 self-contained per-mutation D-mutation Processor。考虑的替代方案：
  - (a) 泛化排除：R8 循环内跳过任何被 `*BizModel.java` `@Inject` 字段引用的 Processor 类名（2-pass grep：先收集 BizModel `@Inject ... *Processor` 字段类型白名单，再排除）——最准确反映 per-mutation 架构（Processor = 内部编排，BizModel = xbiz 面），但需核实是否会误排除应被 R8 标记的 monolithic facade
  - (b) 命名模式排除：跳过匹配 per-mutation 命名模式 `<Entity><Verb>Processor`（Verb ∈ 已知 mutation 词表）的文件——较脆（新动词需手动维护）
  - (c) 不校准，仅 baseline-raise 至 actual（~248）+ 文档化全部为 false positive——最简但 checker 永久 noisy，successor = checker 校准
  - 记录选择、替代方案、残留风险（如选 (a)：若 monolithic facade 也被 @Inject 是否误排除）。若选择需探索验证，先跑临时 Explore 确认 (a) 不会误排除原 42 baseline 中的真实违规。
  - Skill: none
  - **结果**：**选 (a)（泛化为「被任何其他生产 .java 文件引用 = 已路由」）**。探索验证（临时 bash 2-pass）：(a) 精确版「BizModel @Inject 字段引用」漏看 camelCase 字段名（`\bProcessor\b` 词边界不匹配 `budgetScenarioProcessor`），改 `*Processor` 字段声明检测得 447 类型，排除后 R8=47 残留（21 域 abstract + 26 内部 helper facade 非 BizModel 直接注入）；再放宽到「被任何其他生产文件引用（≥2 文件，含 Processor→Processor helper 消费）」+ 域级 abstract 检测 → R8=**0 真孤儿**。这证实 Phase 1 的 2 处 Category A 违规（BudgetScenario/Posting facade）虽是 :42 问题但**均被消费（BizModel @Inject 路由）**，故 R8 语义下正确排除——:42 多 mutation 是另一规则（Phase 1 手动 grep 捕获），与 R8「孤儿/缺路由」正交。替代方案否决理由：(b) 命名模式对 MR6 多动词词表（runPayroll/calculateSalary/clockIn...）维护成本高且脆；(c) checker 永久 noisy 违反 G2「反映真实架构语义」。残留风险登记：自引用孤儿（`uniq -c=1`）仍计=正确；仅被 test 引用计孤儿=正确；精确 BizModel 可达性分析（跨 Processor 传递消费）为 successor。
- [x] **Fix | Add: 实施 R8 checker 校准**（按 Phase 1 Decision 选择）。若选 (a)/(b)：修改 `nop-compliance-checker.sh:287-298` R8 段排除逻辑；校准后 R8 actual 应回落至反映"真实无 xbiz 接线且非 per-mutation"的 Processor 数（预期接近原 42 或更低）。校准实施位置注释引用本 plan + 先例链（1057-1/1057-2）。残留风险登记（如未来手写 per-mutation Processor 不经 BizModel @Inject 会漏排除）。
  - Skill: none
  - **结果**：`nop-compliance-checker.sh` R8 段实施——新增 `consumed_processors` 白名单（每文件 `grep -ohE '\b*Processor\b' | sort -u` 去重后 `uniq -c | awk '$1>=2'`）+ abstract class 检测（`^[[:space:]]*(public|...)?abstract (class|interface)`）+ 白名单 membership 跳过（`grep -qxF "$cls"`）。`|| true` 防 `set -e`/pipefail 中断（grep 无匹配返回 1）。校准后 R8 actual=0（2026-08-01 实测），baseline 裁决性下调 42→0。
- [x] **Add: R2c baseline 裁决性上调**（1250 → actual，带 per-site 证据）。增量来源 = R6.x 新增 per-mutation Processor 的 `dao()` 方法 `daoProvider.daoFor(<EntityClass>)`（每 Processor 1 处，对齐 1057-2 +149 先例）。逐站点分类（同域 vs 跨域只读），确认无 B 类"应重构为 I*Biz"候选。更新 `compliance-baseline.md` BASELINE 块 R2c 值 + 新增 R6.8 裁决注记段。
  - Skill: none
  - **结果**：BASELINE 块 R2c 1250→**1380**（+130）。逐站点证据=293 个生产文件含 `daoProvider.daoFor(`，每 MR6 per-mutation Processor 一行 `dao()` 契约。无 B 类候选（抽象基类编排骨架契约，对齐 1057-2 裁决 (c)）。裁决注记段已加。
- [x] **Add: R2d baseline 裁决性上调**（28 → 32，带 per-site 证据）。+4 新站点 = 新 per-mutation Processor 读取 ErpMd* 实体（跨域只读聚合）。逐站点 file:line + 合法性分类。更新 BASELINE 块 R2d 值。
  - Skill: none
  - **结果**：BASELINE 块 R2d 28→**32**（+4）。32 站点全量 file:line + ErpMd* 类型分类（Subject/Material/Currency/Employee/AcctSchema）见 Phase 1 Evidence + 裁决注记段，全部合法跨域只读聚合。
- [x] **Add: R1d/R2a/R2b 改善回写基线**（17→14 / 38→34 / 325→240）。R6.x 重构将 BizModel daoFor/findAllByQuery 下移到 Processor，checker 实测下降。虽 gate 方向为单向收紧（下降自动 PASS），但回写基线反映真实代码计数（鼓励）。更新 BASELINE 块。
  - Skill: none
  - **结果**：BASELINE 块 R1d 17→**14** / R2a 38→**34** / R2b 325→**240**（R6.x BizModel daoFor/findAllByQuery 下移 Processor）。
- [x] **Proof: checker 复跑全 16 规则 actual ≤ baseline**（校准 + 基线更新后），exit 0，CI green 语义对齐。
  - Skill: none
  - **结果**：`bash docs/audits/nop-compliance-checker.sh` exit 0，全 19 规则 actual ≤ baseline（R8=0≤0 / R2c=1380≤1380 / R2d=32≤32 / R1d=14≤14 / R2a=34≤34 / R2b=240≤240 / 其余=基线）。i18n checker `--strict` exit 0（零回归）。

Exit Criteria:

> 本阶段交付校准后的 checker + 更新后的基线，使 compliance gate 在 MR6 后仓库上 PASS。本地化验证 = checker 复跑 exit 0。

- [x] R8 checker 校准已实施（或 baseline-raise 已文档化），校准后 checker 复跑全 16 规则 actual ≤ baseline，exit 0
- [x] `compliance-baseline.md` BASELINE 块 + 裁决注记已更新（R2c/R2d 上调 + R1d/R2a/R2b 改善 + R8 校准/上调）

### Phase 3 — 全量验证 + 日期边界快照处理 + bookkeeping

Status: completed
Targets: 全量构建/测试；`docs/audits/arm-index.md`；`docs/backlog/audit-remediation-roadmap.md`；`docs/logs/2026/08-01.md`
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 2（checker + 基线已就绪）

- [x] **Proof: 全量 `mvn clean install -DskipTests`** → BUILD SUCCESS（156 reactor 模块，0 errors）。这是 MR6 后首次全量编译验证（R6.x 子批次各自跑过，本 plan 是集中确认）。
  - Skill: none
  - **结果**：`mvn clean install -DskipTests` → BUILD SUCCESS，156 reactor 模块全 SUCCESS，exit 0（2026-08-01 实测）。
- [x] **Proof: 全量 `mvn test`** → 0 failures / 0 errors。**两类快照漂移处理**：
  - (i) **类名变化快照**（roadmap R6.8 显式交付"重录快照（类名变）"）：R6.x 子批次已各自重录类名/堆栈变化（多份 plan 报告"无快照漂照——GraphQL 经 BizModel 契约面不变，Processor 为内部编排重构"）；本项验证全量跑后确无残余类名快照失败，若有则归 (ii) 分支 (b) 处理。
  - (ii) **日期边界快照**：预期 finance/mfg/assets/pur/sal 域存在 `YearMonth.now()` 7→8 月翻滚致日期敏感测试快照漂移（各 R6.x plan 已注记的 pre-existing 模式）。对每个失败分类：(a) 日期边界快照漂移 → 重录 8 月基线（对齐 `2026-07-31-2115-3` assets 5 测试重录范式），或 (b) R6.x 回归 → 必须修复（不可降级为 deferred，是不可降项），或 (c) 其他 pre-existing → 独立验证后登记
  - 目标终态：全量 0 failures / 0 errors（1 skipped `@Disabled` 除外）
  - Skill: none
  - **结果**：全量 `mvn test --fail-at-end` 失败集合**全在 finance 域**（11 方法：1 failure + 10 errors），mfg/assets/pur/sal/其余 18 域 0 失败（R6.x 子批次已各自重录 8 月基线）。逐方法分类（经 R6.1 finance plan 证实 2026-07-31 `Tests run: 306, Failures: 0, Errors: 0` 全绿 + 本 plan 零 Java 变更 → 全部 date-induced，非 R6.x 回归 = 分支 (a)/(c)，无分支 (b)）：
    - **(a) 日期边界 value drift，已重录 8 月基线（6 方法，全绿）**：`TestErpFinBadDebtReversal` 3（`erp_fin_accounting_period` MONTH 7→8 + START/END_DATE 整月漂移；过账仍成功 POSTED=true，仅期间值漂移）+ `TestErpFinEmployeeAdvanceCashRepayReversal` 3（同型 NAME=2026-08/MONTH=8/dates 漂移）。以 `mvn test -Dnop.autotest.force-save-output=true -Dmaven.test.failure.ignore=true` 重录 6 方法 output 快照为 8 月基线（对齐 2115-3 范式），重录后 CHECKING 复跑 6/6 全绿。
    - **(c) 其他 pre-existing date-fragility，独立验证后登记 successor（5 方法）**：
      - `TestErpFinNotesPayableStateMachine` 4（testHonorReleasesCredit / testIssueCommercialAcceptanceNoCreditCheck / testIssueBankAcceptanceOccupiesCredit / testWriteOffReleasesCredit）：**行为型日期脆弱**——过账引擎 `resolveOpenPeriod(voucherDate)` 在 8 月抛 `erp.err.fin.posting.period-not-found`（测试 input `erp_fin_accounting_period.csv` 为空 + 票据 voucher date 与自动创建期间在 8 月错位），致 `POSTED=false` + 无凭证。非 value drift（重录会 bless 过账失败，违反测试语义），须测试数据硬化（按 voucher date 预置 OPEN 期间 or 票据日期与运行月对齐）。**不可降级为分支 (b)**——非 R6.x 回归（R6.1 7 月 31 全绿，仅日期翻滚）。
      - `TestErpFinDashboard.testTrendMonthlySeries` 1：**测试逻辑型日期脆弱**——`getDashboardTrend(2, CTX)` = "近 2 月"窗口，硬编码 seed 2026-06/2026-07 GL 余额 + 断言窗口含 6/7 月；8 月窗口={8,7}月，6 月被挤出 → `assertTrue(hasJun)` 失败。须 seed 改为 `YearMonth.now()` 相对月（测试逻辑硬化）。非快照漂移（`force-save` 无法修复硬编码断言）。
    - **终态**：finance 306 = 301 绿 + 5 pre-existing date-fragility（已登记 R6.x-test-hardening successor）。**无 R6.x/R6.8 回归**（分支 (b) 为空）。全量其余 18 域 0 失败。本 plan 不做测试硬化（test-hardening 是独立 successor 范围；R6.8 Non-Goal 零生产代码变更，测试逻辑硬化亦超出"重录/确认 pre-existing"范围）。
- [x] **Add: arm-index P1-MA3-062 状态回填** `done (R6.8)` + 闭合回填写（R6.8 工作项 done；MR6 milestone 是否闭合取决于下方条件分支）。
  - Skill: none
  - **结果**：arm-index P1-MA3-062 状态 `部分进展` → `done (R6.8 核验完成 + successor 已开，MR6 OPEN)`，附 R6.8 核验结论 + 4 处完成判据遗漏 + 5 处日期脆弱 successor。
- [x] **Add: roadmap R6.8 `todo`→`done`** + MR6 milestone 闭合注记。**条件分支**：
  - 若 Phase 1 核验 = 零完成判据违规（类别 A `:42` + 类别 B `:5/:7` 均满足）→ MR6 milestone 闭合（R6.0–R6.8 全 done，完成判据满足）。
  - 若 Phase 1 核验 = 发现 ≥1 处违规 → R6.8 闭合为"核验完成 + successor 已开"，但 **MR6 milestone 保持 OPEN** 直至 successor 解决（用户明确完成判据"所有模块所有 BizModel 的 @BizMutation 满足 processor-extension-pattern.md"未满足，不可关闭 milestone 覆盖未达成的完成判据——Rule 13 不可降级）。roadmap R6.8 标 done + 显式登记 successor。
  - Skill: none
  - **结果**：Phase 1 核验 = **发现 ≥1 处违规**（类别 A BudgetScenario/Posting + 类别 B InvCosting/MdSupplierApproval）→ **条件分支 2 命中**：roadmap R6.8 `todo`→`done (核验完成 + successor 已开)`；**MR6 milestone 保持 OPEN**（完成判据未完全满足，Rule 13 不可降级）；新增 R6.9 successor 工作项（per-mutation 补拆 + 测试日期硬化）。
- [x] **Add: `docs/logs/2026/08-01.md`** 聚合条目（R6.8 验证结论 + compliance 基线裁决 + 日期快照处理 + MR6 milestone 闭合/保持 OPEN 判定），格式见 `docs/logs/00-log-writing-guide.md`。
  - Skill: none
  - **结果**：`docs/logs/2026/08-01.md` 追加 R6.8 聚合条目。

Exit Criteria:

> 本阶段交付 MR6 后全量绿基线 + 全部 bookkeeping。全量验证属本阶段核心交付（非 Closure Gates 重复——Closure Gates 复跑确认）。

- [x] 全量 `mvn clean install -DskipTests` BUILD SUCCESS（156 模块）+ `mvn test` 0 R6.x/R6.8 回归（6 日期 value drift 已重录 8 月基线；5 pre-existing date-fragility 登记 successor；无分支 (b) R6.x 回归）
- [x] arm-index P1-MA3-062 + roadmap R6.8 状态已回填；MR6 milestone 保持 OPEN 判定已按 Phase 1 核验结论执行（条件分支 2）；docs/logs 条目已落

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_045997093ffeLxGqUC0avMtGCw) because baseline 验证全通过但发现 1 blocking（MR6 milestone 闭合逻辑与完成判据条件耦合缺失——Rule 5/10/13）+ 4 minor（Phase 3 Item Types 标注 / 类名快照交付未声明 / "256 全部漏入"算术过述 / compliance-checker skill 措辞）。已全部修订：条件分支 milestone 闭合、Item Types 改 `Proof | Add`、补类名快照说明、R8 delta 精确化、skill 措辞改"先例链"。
- Independent draft review iteration 2: accept (ses_04595b508ffeoO9zn2YksqWirL) — 全 5 项 finding 已解决，无新 blocking issue；compliance actuals 全量复跑与 live repo 精确匹配（R8=248/R2c=1380/R2d=32/R1d=14/R2a=34/R2b=240）；roadmap R6.8 定义忠实覆盖；anti-slack scan 干净。仅 1 cosmetic arithmetic 措辞已修正。计划作为执行契约可接受，提升为 active。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处复跑确认。

- [x] 范围内行为完成（G1 完成判据核验结论 = 发现 4 处遗漏登记 successor + G2 compliance 基线裁决（R8 校准→0 / R2c+130 / R2d+4 / R1d·R2a·R2b 改善）+ G3 全量构建绿 + test 0 R6.x 回归 + G4 bookkeeping）
- [x] 相关文档对齐（`processor-extension-pattern.md` 完成判据引用 + `compliance-baseline.md` 基线块 + 裁决注记 + `arm-index.md` P1-MA3-062 + roadmap MR6 + exemption-registry §E）
- [x] 已运行验证：`mvn clean install -DskipTests`（156 模块 BUILD SUCCESS）+ `mvn test`（**0 R6.x/R6.8 回归**；6 日期 value drift 重录 8 月基线全绿；5 pre-existing finance date-fragility 独立验证后登记 R6.9 successor，**非 R6.x 回归**——R6.1 2026-07-31 306 全绿 + 本 plan 零 Java 变更）+ `bash docs/audits/nop-compliance-checker.sh`（exit 0，R8 校准后 actual≤baseline）+ `bash docs/audits/i18n-coverage-checker.sh --strict`（exit 0，零回归确认）
- [x] 无范围内项目降级为 deferred/follow-up（完成判据核验发现 4 处遗漏 mutation + 5 处日期脆弱测试，均登记 R6.9 successor，不阻塞本 plan 闭合——本 plan 范围=验证+裁决+重录 clean drift，非补拆/非测试硬化；MR6 milestone 按 Phase 3 条件分支 2 保持 OPEN）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 独立结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 完成判据核验可能发现的遗漏 mutation（条件性）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本 plan 范围 = 验证 + 裁决（R6.8 roadmap 定义）。若 Phase 1 grep 核验发现个别 mutation 遗漏拆分（R6.0 triage 误判边界 / 新增代码），登记为 successor 补拆计划，不阻塞 R6.8 闭合——R6.8 的交付是"核验结论"本身，发现遗漏并诚实登记正是其价值。
- Successor Required: `yes`（触发条件：Phase 1 发现 ≥1 处类别 A `:42` 或类别 B `:5/:7` 违规）
- **R6.8 实测触发=已登记 R6.9 successor**（见 §Phase 1 Evidence + exemption-registry §E）：类别 A ErpFinBudgetScenarioProcessor（rollForward/carryForward）+ ErpFinPostingProcessor 边界（process/reverseProcess）+ 类别 B ErpInvCostingBizModel.reclosePeriodCosts + ErpMdSupplierApproval（6 :46 补登记 + suspendByPartner 复议）。

### finance 测试日期脆弱（R6.8 核验暴露）

- Classification: `pre-existing date-fragility`
- Why Not Blocking Closure: 5 个 finance 测试在 7→8 月翻滚暴露日期脆弱（NotesPayable 4 `period-not-found` 行为型 + Dashboard 1 硬编码窗口断言），独立验证为非 R6.x 回归（R6.1 2026-07-31 306 全绿 + 本 plan 零 Java 变更）。R6.8 范围=重录 clean value drift（BadDebt/EmployeeAdvance 6 已重录）+ 登记 pre-existing；测试逻辑/数据硬化（按 voucher date 预置期间 / `YearMonth.now()` 相对 seed）是独立 successor 范围。
- Successor Required: `yes`（R6.9 test-hardening 子项）

### R2c successor（AbstractProcessor 泛型重构）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 1057-2 裁决选 (c) 接受 per-mutation daoFor 为抽象基类契约代价；本 plan 维持该裁决，R2c 上调基线带 per-site 证据。
- Successor Required: `no`

## Closure

Status Note: 本 plan 闭合（R6.8 核验 + 裁决 + bookkeeping 全部交付）。MR6 milestone **保持 OPEN** 是正确终态——R6.8 的交付物即"核验结论"本身，发现 4 处完成判据遗漏并诚实登记为 R6.9 successor 正是其价值（Rule 13 不可降级：完成判据未完全满足即不可关闭 milestone）。独立结束审计 PASS（新会话只读复核全 10 项检查）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话 `ses_045671d78ffeOv4sFC268a0Xde`，cold context，read-only，执行者未自我审计）
- Evidence: VERDICT: PASS。全 10 项复核全 OK：
  1. compliance checker `bash docs/audits/nop-compliance-checker.sh` EXIT=0，R8=0 / R1d=14 / R2a=34 / R2b=240 / R2c=1380 / R2d=32 与 BASELINE 块逐行精确匹配（actual ≤ baseline）；
  2. i18n checker `--strict` EXIT=0（零缺陷零覆盖缺口）；
  3. R8 校准代码确认存在于 `nop-compliance-checker.sh`（`consumed_processors` 2-pass grep 白名单 + abstract class 早退检测）；
  4. `compliance-baseline.md` BASELINE 机器可读块值匹配 + R6.8 裁决注记段在位；
  5. exemption-registry §E 含全部 4 处遗漏（BudgetScenario/Posting/InvCosting/MdSupplierApproval）+ R6.9 successor 处置；
  6. roadmap R6.8 `done（核验完成 + successor 已开，MR6 OPEN）` + R6.9 `todo` + line 750 「MR6 milestone 状态：OPEN」(Rule 13)；
  7. arm-index P1-MA3-062 `done (R6.8 核验完成 + successor 已开，MR6 OPEN)`（非 todo/非陈旧）；
  8. `docs/logs/2026/08-01.md` R6.8 聚合条目完整；
  9. 4 处完成判据遗漏实仓逐一证真（BudgetScenario rollForward/carryForward + Posting process/reverseProcess + InvCosting.reclosePeriodCosts + MdSupplierApproval 6 action）+ `git diff --stat -- '*.java'` 空（零生产 Java 变更，Non-Goals 守约，本 plan 仅 6 doc + 1 audit script + 6 test-snapshot .csv 变更）；
  10. 全量 install 未由审计者重跑（时间成本；零 Java diff 保证上次 156 模块绿基线仍有效——compliance + i18n checker 均实跑 exit 0 佐证），非阻塞 caveat。
  MR6 milestone 保持 OPEN 的判定经审计复核**正确**（R6.8 交付 = 核验结论本身；诚实发现 + 登记 successor 非闭合失败）。

Follow-up:

- R6.9 successor（roadmap）：4 处完成判据遗漏补拆（类别 A BudgetScenario RollForward/CarryForward + Posting process/reverseProcess 裁决[engine 豁免 OR 拆两 Processor] + 类别 B InvCosting.reclosePeriodCosts）+ ErpMdSupplierApproval 6 :46 补登记 + suspendByPartner 复议 + finance 5 处测试日期硬化（NotesPayable 按 voucher date 预置 OPEN 期间 + Dashboard `YearMonth.now()` 相对 seed）。R6.9 解决后 MR6 milestone 可闭合。
