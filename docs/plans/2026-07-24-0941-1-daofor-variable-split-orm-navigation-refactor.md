# 2026-07-24-0941-1-daofor-variable-split-orm-navigation-refactor daoFor variable-split 子模式（ORM 导航可替代）重构（F1 successor）

> Plan Status: completed
> Mission: erp
> Work Item: daoFor Type 1 真违规子集 — `dao = daoFor(X); dao.getEntityById(FK)` variable-split 形态逐处分类 + safe 子集重构
> Last Reviewed: 2026-07-24
> Source: `docs/plans/2026-07-24-2000-1-daofor-type1-orm-navigation-refactor-batch2.md` §Deferred But Adjudicated「`getEntityById(FK)` variable-split 子模式（闭包审计发现）」（触发条件：逐处 Type 1/Type 2/ORM-gap 分类完成——**本计划即该分类 + 重构**，触发条件由本计划主动驱动满足；与 chained 形态收尾同型，2000-1 已验证范式）
> Related: `docs/audits/2026-07-23-0000-architecture-governance-review.md` §F1（HIGH）、`docs/analysis/governed-path-cost-evaluation.md` §3.6（variable-split successor + 收尾结论）、`docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md`（daoFor 6 类分类，Type 1/Type 2 定义来源）、`docs/plans/2026-07-24-0605-3-daofor-type1-orm-navigation-refactor-batch1.md` + `2026-07-24-2000-1`（chained 形态两批范式 + 多行 grep 教训）、`docs/audits/compliance-baseline.md`（checker R2b=314/R2c=1071/R2d=27 基线门控）
> Audit: required

## Current Baseline

governed-path 成本评估（`docs/analysis/governed-path-cost-evaluation.md` §3.2）已裁决 Type 1（ORM 导航可替代）可安全重构（不引入跨域 service 耦合）。chained 形态（`daoFor(X).getEntityById(FK)`）经两批（0605-3 + 2000-1，累计 37 处）全域清零，closure audit PASS。

2000-1 闭包审计发现 **variable-split 子模式**——语法形态 `IEntityDao<X> xxxDao = daoProvider(.).daoFor(X); ... xxxDao.getEntityById(FK)`（变量拆分，非 chained）——登记为 successor（`2000-1:247` 给出候选清单）。该子模式与 chained 语义同类但语法不同，**非纯机械替换**：部分为 **Type 2 会话存活豁免**（如 voucher-by-link 循环加载，batch 1 `ErpFinVoucherBizModel:154,161` 已显式豁免「避免依赖 to-many 懒加载的会话存活」），部分为 Type 1 可替换，个别可能 ORM-gap。

**关键发现（iter-1 独立审查实测）**：variable-split 形态比 2000-1 候选清单（16 站点，fin/inv/ast/prj）分布更广——实测多行 grep 另发现 **crm**（`LeadActivityDerivationHelper:40`）、**quality**（`SpcSamplingService:110` / `SpcControlLimitCalculator:94`）、**inventory**（`ErpInvLandedCostProcessor:236,403`）、**finance**（`PartnerBalanceUpdater:37` / `ErpFinBankStatementLineBizModel:45`）等站点。多数为 `voucherDao.getEntityById(link.getVoucherId())` 在 `ErpFinVoucherBillR` 链接集合上的**循环逐条加载**（典型 Type 2 批量读，非持有单一托管父实体导航）。故 Phase 1 三态分类是必需步骤——不能假定全部 Type 1。

**2000-1 候选清单（起始枚举，file:line 为 2000-1 闭包时点；iter-1 审查已证部分行号漂移 + 形态差异，Phase 1 须用多行 grep 重新核实）：**

| # | file:line（2000-1 时点） | 域 | iter-1 审查核实 | 备注 |
|---|--------------------------|----|-----------------|------|
| 1 | `CostAdjustmentPostingDispatcher:127-129` | inv | ✅ 命中（行号一致：129） | voucher-by-link 循环加载（Type 2 候选）|
| 2 | `CostAdjustmentService:207-208` | inv | ✅ 命中（行号一致：208） | material 加载（Type 1 候选）|
| 3 | `ErpFinPostingProcessor:474-476` | fin | ✅ 命中 | voucher-by-link 循环（Type 2 候选）|
| 4 | `ErpFinPostingProcessor:839-841` | fin | ✅ 命中 | 同上 |
| 5 | `ErpFinPostingProcessor:857-860` | fin | ✅ 命中 | 同上 |
| 6 | `ErpFinPostingProcessor:900-902` | fin | ✅ 命中 | 同上 |
| 7 | `ErpAstMergeProcessor:439` | ast | 待 Phase 1 多行 grep 核实 | 资产合并 |
| 8 | `ErpAstInventoryProcessor:340` | ast | ⚠️ **helper-wrapped**（`assetDao().getEntityById()`，非 variable-split 形态）| Phase 1 须单独分类（helper 方法形态）|
| 9 | `AdvanceOffsetOrchestrator:183-185` | fin | ✅ 命中 | voucher-by-link 循环（Type 2 候选）|
| 10 | `CreditFacilityInterestVoucherBuilder:95` | fin | 待 Phase 1 核实 | 授信额度利息 |
| 11 | `BadDebtProvisionService:169-172` | fin | ✅ 命中（行号一致：172） | voucher-by-link 循环（Type 2 候选）|
| 12 | `BankReconAdjustmentVoucherBuilder:126` | fin | 待 Phase 1 核实 | 银行对账调整 |
| 13 | `ErpFinBadDebtProcessor:139` | fin | ⚠️ **helper-wrapped**（`arApItemDao().getEntityById()`，非 variable-split 形态）| Phase 1 须单独分类 |
| 14 | `ProjectCostAggregator:169` | prj | 待 Phase 1 核实 | 项目成本聚合 |
| 15 | `ProjectCostAggregator:176` | prj | 待 Phase 1 核实 | 同上 |
| 16 | `ErpPrjProjectSettlementProcessor:270` | prj | 待 Phase 1 核实（可能 ORM-gap：`projects.orm.xml` 明示 assetCard 关系因 DAG 环约束未建模）| 项目结算 |

**iter-1 新发现站点（2000-1 清单外，Phase 1 须纳入）：** `LeadActivityDerivationHelper:40`（crm）/ `SpcSamplingService:110` + `SpcControlLimitCalculator:94`（qa）/ `ErpInvLandedCostProcessor:236,403`（inv）/ `PartnerBalanceUpdater:37` + `ErpFinBankStatementLineBizModel:45,56`（fin）。

**关键风险（2000-1 教训 + iter-1/iter-2 审查）**：单行 grep 会系统性漏看多行 / variable-split 形态（2000-1 闭包审计 FAIL 的直接教训）。Phase 1 **必须**用**可工作的**多行 grep。iter-1 审查实证草案原列 regex `dao\s*=\s*daoFor\(...` **返回 0 命中**（实际代码含 `daoProvider.daoFor(` 前缀）。iter-1 修正为 `Dao\s*=...`（要求变量名以大写 `Dao` 结尾）仍**系统性漏看小写 `dao` 变量**（iter-2 审查 Major-1 实证：`Dao` 命中 18 行 vs `[Dd]ao` 命中 69 行；`ErpAstMergeProcessor:437`/`ProjectCostAggregator:169,176`/`CreditFacilityInterestVoucherBuilder:95` 等 6 个候选清单内站点用小写 `dao` 被 iter-1 regex 漏看）。**已校正为按 `IEntityDao<...>` 类型声明匹配（最稳健，不依赖变量名大小写）**：

```
rg -U 'IEntityDao<[^>]+>\s+\w+\s*=\s*\w+(\(\))?\.daoFor\([^)]+\)\s*;[\s\S]{0,200}?\.getEntityById' module-*/erp-*-service/src/main/java
```

（排除 `_gen`/`target`/test；`(\(\))?` 同时覆盖 `daoProvider.daoFor(` 与 `daoProvider().daoFor(` 两种前缀形态——iter-3 审查 Major-1 实证前版 regex 漏看 `()` 括号前缀形态致 7 文件缺失；`{0,200}` 容忍变量声明与 getEntityById 间的循环/语句跨度；helper-wrapped 形态 `xxxDao().getEntityById()` 由辅助 grep `Dao\(\)\.getEntityById` 补充覆盖）

**枚举宇宙远大于 16-候选清单（iter-2 审查关键发现）**：上述 `IEntityDao<...>`-typed regex 实测匹配 **~58 文件**（iter-3 校正后，含 `daoProvider().daoFor(` 括号前缀形态）——远超 2000-1 候选清单的 16 站点 + iter-1 新发现站点。多数为 voucher-by-link 循环加载（Type 2 候选）或同域子实体批量读。**候选清单仅为 Phase 1 的起点子集，非穷举**——Phase 1 权威枚举须以 regex 输出（真实宇宙）为准，self-check 对账对象是 **regex 输出**（真实宇宙）而非候选清单表。这与 2000-1 闭包审计 FAIL 教训对齐（清单不能替代全量 grep）。

**checker 基线**（2000-1 后）：R2b=314 / R2c=1071 / R2d=27 / R2a=37。本批预期 R2c 下降（safe 子集站点数），R2b 下降取决于 BizModel 跨域站点。

剩余差距：variable-split 子模式未分类、未重构；`getEntityById(FK)` 全形态（chained + variable-split）未全域清零。

## Goals

1. **分类 variable-split 子模式**：对全域 variable-split 生产站点（2000-1 候选清单 16 站点 + iter-1 审查新发现 crm/qa/inv/fin 站点，Phase 1 权威枚举定终值）逐处做三态分类——(a) **safe** Type 1（ORM `<to-one>` 已建模，关系 getter 可替代）；(b) **Type 2 会话存活豁免**（voucher-by-link 循环加载 / 依赖 to-many 懒加载或会话存活，关系导航不安全，保留 + 登记理由）；(c) **ORM-gap**（业务上应有关系但 ORM 未建模，保护区域，移出范围）。
2. **重构 safe 子集**：将 Type 1 safe 站点改为 ORM `<to-one>` 关系 getter，下降 checker R2c/R2b 基线，行为不变。
3. **登记 Type 2 豁免**：对保留为豁免的 Type 2 站点，在 plan §Deferred 或 `posting-exemptions.md`（若属跨域写文件）登记理由（会话存活 / 懒加载依赖），避免未来重复审查。
4. **关闭 `getEntityById(FK)` 全形态工作流**：variable-split 收尾后，`getEntityById(FK)` chained + variable-split 两形态生产站点全域清零（仅余已登记豁免 / Type 5 dashboard / Non-Goal 排除）。

## Non-Goals

- **不改 ORM 模型 / 不补 ORM `<to-one>` 关系**（保护区域，ask-first）——ORM-gap 子集分类为 Deferred successor。
- **不重构 Type 4（跨域写/读）**——阻塞中，待 nop-entropy 平台 lazy/SPI 解耦或保留豁免（成本评估 §3.2）。
- **不重构 Type 2 会话存活豁免**——关系导航不安全（懒加载 / 会话存活依赖），保留并登记理由。
- **不重构 `findAllByQuery` Type 1 子集**——2000-1 已评估为 watch-only residual（可机械替换候选 <10，触发条件未满足）。
- **不重构 `ErpCtRebateSettlementBizModel` chained 只读站点**——Non-Goal 排除文件（posting-exemptions.md 登记豁免），独立 successor。
- **不改 biz 方法签名 / API 契约 / xbiz / 页面 / ORM**——内部访问方式重构，行为不变。

## Task Route

- Type: `architecture change`（governed-path 合规结构改进，结果面 = variable-split 子模式分类 + safe 重构 + `getEntityById(FK)` 全形态收尾）
- Owner Docs: `docs/analysis/governed-path-cost-evaluation.md`（Type 1 重构前置条件 + §3.6 收尾结论）、`docs/architecture/cross-domain-constraints.md`（跨域访问写引用契约）、`docs/architecture/data-dependency-matrix.md §5.3`（禁止 IDaoProvider/IOrmTemplate 直接跨域查表）、`docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md`（6 类分类，Type 1/Type 2 定义）
- Skill Selection Basis: `nop-backend-dev`（匹配「跨实体调用 / ORM 关系导航 / daoFor 收敛 / 产品化可定制性自检 E2/E3」工作方法；0605-3 / 2000-1 / 0930-2 同型范式均经该技能路由）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯 Java 重构 + ORM 关系 getter，无端口/密钥/外部服务/数据迁移）。

## Execution Plan

### Phase 1 — variable-split 子模式全域枚举 + 逐处三态分类 + 候选集完备性自检

Status: completed
Targets: 全域 service `src/main/java`（排除 `_gen`/`target`/test）、Current Baseline 候选清单 16 站点
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（chained 两批范式已验证）

- [x] `Proof`：用**已校正的可工作多行 grep**（`rg -U 'IEntityDao<[^>]+>\s+\w+\s*=\s*\w+(\(\))?\.daoFor\([^)]+\)\s*;[\s\S]{0,200}?\.getEntityById' module-*/erp-*-service/src/main/java`，排除 `_gen`/`target`/test；按 `IEntityDao<...>` 类型声明匹配 + `(\(\))?` 覆盖两种前缀形态，不依赖变量名大小写——iter-1/iter-2/iter-3 审查先后实证草案 regex 的三类盲区（前缀盲区 + 变量名大小写盲区 + 括号前缀盲区），均已校正）+ 辅助 grep（helper-wrapped 形态 `Dao\(\)\.getEntityById`）产出 variable-split 子模式**权威候选清单**。多行 grep 是 2000-1 闭包审计 FAIL 的直接教训。候选清单仅为起点子集（~58 文件真实宇宙 >> 16 候选），**权威枚举以 regex 输出为准**。
  - Skill: `nop-backend-dev`
- [x] `Decision`：逐处三态分类——(a) **safe** Type 1：持有实体有对应 ORM `<to-one>` 关系且关系在同 DAO classpath（daoFor→关系 getter 可直接替代）；(b) **Type 2 会话存活豁免**：voucher-by-link 循环逐条加载 / 依赖 to-many 懒加载 / 跨 session 实体 / 会话存活（关系导航不安全，保留 + 登记理由）；(c) **ORM-gap**：业务上应有关系但 ORM 未建模（保护区域，移出范围）；(d) **not-Type-1**：实际属 Type 3/4/5/6（排除）。每条记录 file:line + daoFor 目标 + 持有实体 + ORM 关系证据（`<domain>.orm.xml:行号` `<to-one name="...">`）+ 判定 + 理由。
  - Skill: `nop-backend-dev`
- [x] `Decision`（候选集完备性自检，**以 regex 输出为真相源**）：Phase 1 末做一次完备性自检——**权威枚举 = `IEntityDao<...>`-typed regex 输出（真实宇宙，~58 文件）**，候选清单表仅为起点子集。逐条对账：(1) regex 输出中每站点是否已分类；(2) 候选清单表内站点是否均在 regex 输出中（不在则查 helper-wrapped 形态或行号漂移）；(3) 任何清单外站点（iter-2/iter-3 确认 ~40+ 文件）须纳入分类，不得静默丢弃。与 2000-1 闭包审计 FAIL 教训（清单不能替代全量 grep）+ iter-2/iter-3 审查 Major（候选清单非穷举 + regex 前缀盲区）对齐。
  - Skill: `nop-backend-dev`

#### Phase 1 权威枚举 + 三态分类证据

**权威枚举**（已校正多行 grep `rg -U 'IEntityDao<[^>]+>\s+\w+\s*=\s*\w+(\(\))?\.daoFor\([^)]+\)\s*;[\s\S]{0,200}?\.getEntityById'`，排除 `_gen`/`target`/test）实测匹配 **58 文件**（含 `daoProvider().daoFor(` 括号前缀形态），远超 2000-1 候选清单 16 站点 + iter-1 新发现站点。辅助 grep `Dao\(\)\.getEntityById`（helper-wrapped 形态）另覆盖 ~40 文件。候选清单表仅为起点子集，**权威枚举以 regex 输出为准**（与 iter-2/iter-3 审查对齐）。

**关键分类维度**：variable-split 站点按 FK 来源分两子形态——(A) **FK 来自作用域内托管实体 getter**（`dao.getEntityById(entity.getXxxId())`，可重构为 `entity.getXxx()`）；(B) **FK 来自原始方法参数**（`dao.getEntityById(rawId)`，load-by-id 工具方法，无持有托管实体，非 Type 1 关系导航）。绝大多数站点为 (B)（合法 load-by-id）。

**(a) safe Type 1 子集（8 处，FK 来自托管实体 getter，ORM `<to-one>` 已建模，重构框）：**

| # | file:line | daoFor 目标 | 持有实体 | ORM 关系证据 | 替换为 | 形态 |
|---|-----------|------------|---------|-------------|--------|------|
| 1 | `CostAdjustmentService:207-208` | ErpMdMaterial | ErpInvCostAdjustLine line | `inventory.orm.xml:1292` `<to-one name="material">` | `line.getMaterial()` | variable-split |
| 2 | `CreditFacilityInterestVoucherBuilder:94-95` | ErpFinFundAccount | ErpFinCreditFacility facility | `finance.orm.xml:1534` `<to-one name="fundAccount">` | `facility.getFundAccount()` | variable-split |
| 3 | `BankReconAdjustmentVoucherBuilder:124-126` | ErpMdSubject | ErpFinFundAccount fundAccount | `finance.orm.xml:1023` `<to-one name="subject">` | `fundAccount.getSubject()` | variable-split |
| 4 | `ProjectCostAggregator:168-169` | ErpPrjActivityType | ErpPrjTimesheet timesheet | `projects.orm.xml:248` `<to-one name="activityType">` | `timesheet.getActivityType()` | variable-split |
| 5 | `ProjectCostAggregator:175-176` | ErpPrjProjectType | ErpPrjProject project | `projects.orm.xml:119` `<to-one name="projectType">` | `project.getProjectType()` | variable-split |
| 6 | `ErpAstInventoryProcessor:340` | ErpAstAsset | ErpAstInventoryLine line | `assets.orm.xml:1258` `<to-one name="asset">` | `line.getAsset()` | helper-wrapped |
| 7 | `ErpFinBadDebtProcessor:139` | ErpFinArApItem | ErpFinBadDebt debt | `finance.orm.xml:1660` `<to-one name="sourceArApItem">` | `debt.getSourceArApItem()` | helper-wrapped |
| 8 | `ErpAstMergeProcessor:437-439` | ErpAstAsset | ErpAstMergeLine line | `assets.orm.xml:1114` `<to-one name="sourceAsset">` | `line.getSourceAsset()` | variable-split(loop, pure-read) |

> #8 非 voucher-by-link：`ErpAstMergeLine.sourceAsset` 为 to-one，循环内逐条纯读收集，关系导航在事务内安全（to-one 懒加载等价于原 `getEntityById` 逐条加载，均 N+1，无性能回归）。plan Type 2 定义（"voucher-by-link 循环 / 依赖 to-many 懒加载 / 跨 session / 关系导航不安全"）四条均不满足 → Type 1。

**(b) Type 2 会话存活豁免（7 处 voucher-by-link 循环逐条加载，plan 明确定义为 Type 2）：**

| # | file:line | 模式 | 理由 |
|---|-----------|------|------|
| 9 | `CostAdjustmentPostingDispatcher:127-129` | voucherDao.getEntityById(link.getVoucherId()) 循环 ErpFinVoucherBillR + 写 voucher(reversed) | voucher-by-link 循环批量加载；写 voucher |
| 10 | `ErpFinPostingProcessor:474-476` | 同上（alreadyPosted 幂等检查） | voucher-by-link 循环读 |
| 11 | `ErpFinPostingProcessor:839-841` | 同上 | voucher-by-link |
| 12 | `ErpFinPostingProcessor:857-860` | 同上 | voucher-by-link |
| 13 | `ErpFinPostingProcessor:900-902` | 同上 | voucher-by-link |
| 14 | `AdvanceOffsetOrchestrator:183-185` | voucherDao.getEntityById(link.getVoucherId()) 循环 | voucher-by-link |
| 15 | `BadDebtProvisionService:169-172` | voucherDao.getEntityById(link.getVoucherId()) 循环 | voucher-by-link |

> voucher-by-link 循环逐条加载经 plan 明确定义为 Type 2 会话存活豁免（对齐 batch 1 `ErpFinVoucherBizModel:154,161` 豁免先例「避免依赖 to-many 懒加载的会话存活」+ 批量读模式）。`ErpFinVoucherBillR` 虽有 `<to-one name="voucher">`（finance.orm.xml:607），但该子模式经 plan 裁决保留豁免。

**(c) ORM-gap（1 处，弱指针无 ORM 关系，保护区域移出范围）：**

| # | file:line | daoFor 目标 | 持有实体 | ORM 证据 | 判定 |
|---|-----------|------------|---------|---------|------|
| 16 | `ErpPrjProjectSettlementProcessor:269-270` | ErpAstAsset | ErpPrjProjectSettlement settlement | `projects.orm.xml:806` `assetCardId` 列 + `:954` 注释「反向依赖 assets-dao 将构成环。assetCardId 作为弱指针列（无 ORM 关联）」 | ORM-gap（DAG 环约束，弱指针，需补 `<to-one>` 才可重构→ask-first 保护区域） |

**(d) not-Type-1（排除：load-by-id 工具方法 / own-PK re-fetch / graph 遍历 / chained-raw-param）：**

剩余 ~90 站点均为 FK 来自原始方法参数（`subjectId`/`employeeId`/`materialId`/`chartId`/`projectId`/`taskId`/`id`/`discountId`/`categoryId`/`statementId`/`fundAccountId`/`shipmentId`/`ediDocId`/`reconciliationId`/`candidateId`/`exceptionId`/`lineId`/`voucherLineId` 等），为合法 load-by-id 工具方法（无作用域内持有托管实体，非关系导航机会）。代表站点：
- `*PostingDispatcher.resolveSubjectCode(Long subjectId)` / `loadEmployee(Long)` / `loadProject(Long)` / `loadXxxChart(Long chartId)` 等——原始 ID 参数加载，daoFor 合法。
- `ErpInvLandedCostProcessor:236-237,403-404`——`adjustDao.getEntityById(costAdjust.getId())` 按实体自身主键 re-fetch 托管副本用于写（非关系导航）。
- `ErpFinIntercompanyTransferBizModel:116-118`——org parentId 链向上遍历（带环检测的图遍历，`ErpMdOrganization` 虽有 `<to-one name="parent">` 但重构成 `org.getParent()` 遍历改变算法结构，风险高，归 not-Type-1）。
- `ProductionVarianceDispatcher:143`——**chained 形态**（`daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(workOrderId)`，regex 多行跨度误捕），`workOrderId` 为原始参数，非 variable-split 范围。

**helper-wrapped 广义模式（~40 文件 `xxxDao().getEntityById(rawParam)`）**：经 `rg '\w+Dao\(\)\.getEntityById\([a-z]\w+\.get[A-Z]\w+Id\(\)\)'` 精确扫描，仅 #6/#7 两处 FK 来自托管实体（已分类 safe）；其余均为 raw-param load-by-id。该广义模式（chained-via-helper，语义 = batch 2 chained 形态）作为独立 successor 边界记录（若 batch 2 literal-`daoFor` chained 收敛延伸至 helper 变体时一并处理）。

**候选集完备性自检**（以 regex 输出为真相源）：(1) regex 输出 58 文件中每站点均已分类（safe 8 / Type 2 豁免 7 / ORM-gap 1 / not-Type-1 ~90+）；(2) 2000-1 候选清单 16 站点全部在 regex 输出或分类结果中（#8/#13 helper-wrapped 经辅助 grep 覆盖；行号经核实）；(3) iter-1/2 新发现站点（crm/qa/inv/fin）均已纳入分类；(4) helper-wrapped 广义模式经精确 FK 扫描确认仅 2 处可重构，其余 raw-param。零静默丢弃。与 2000-1 闭包审计 FAIL 教训（清单不能替代全量 grep）对齐。

Exit Criteria:

- [x] variable-split 候选清单多行 grep 产出 + 与 Current Baseline 对账（命中数 + 差异说明）
- [x] 逐处三态分类完成（safe / Type 2 豁免 / ORM-gap / not-Type-1），每条带 file:line + ORM 关系证据 + 判定依据

### Phase 2 — safe 子集重构 + Type 2 豁免登记

Status: completed
Targets: Phase 1 safe 子集（finance/inventory/assets/projects 域 Processor + BizModel + Service + Builder/Orchestrator）
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof | Add`
- Item Types Note: Phase 2 contains Fix (daoFor→ORM navigation) + Proof (behavior-invariant sampling) + Add (Type 2 exemption registration); mixed-type phase, no single type ≥80%.
- Prereqs: Phase 1 完成（safe 子集 + Type 2 豁免集已分类）

- [x] `Fix`：逐处将 safe 子集 `IEntityDao<X> dao = daoFor(X); ... dao.getEntityById(entity.getYyyId())` 改为 ORM 关系 getter（`entity.getYyy()`）；移除仅为此站点存在的局部 dao 变量 / 冗余 import（当文件仍被其他 daoFor/saveEntity/newEntity 调用使用时保留）。每域重构后 `mvn test -pl <module>/<service> -am` 验证单模块测试仍启动成功（governed-path §4 前置条件 3）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：抽样验证重构后行为不变——关系 getter 返回与 `getEntityById(FK)` 同一托管实体（同 ORM 会话、同主键）；原 ternary（`xxxId != null ? getEntityById(...) : null`）与 getter 语义等价（FK null 或实体不存在时 getter 均返回 null）。经该域既有测试覆盖。
  - Skill: `nop-backend-dev`
- [x] `Add`：对 Type 2 会话存活豁免站点，在 plan §Deferred But Adjudicated 登记每条理由（会话存活 / 懒加载依赖 / 跨 session），避免未来重复审查。若豁免站点属 `posting-exemptions.md` 已登记文件，仅交叉引用不重复登记。
  - Skill: `nop-backend-dev`

#### Phase 2 重构验证证据

safe 子集 8 处全部重构为 ORM 关系 getter（详见 Phase 1 枚举表 #1-#8）。每处移除仅为此 getEntityById 存在的局部 dao 变量（variable-split）或改为关系 getter（helper-wrapped）；`daoProvider` 字段 / `IEntityDao` import 在所有触及文件仍被其他 daoFor/saveEntity/newEntity 调用使用，无悬空字段或 import（经 `rg IEntityDao` 逐文件核实）。ORM-gap（#16 `ErpPrjProjectSettlementProcessor:270` 弱指针）保护区域零变更。

单模块测试结果（4 受影响域，`mvn test -pl ...`，`test-compile -am` 先行 BUILD SUCCESS）：

- `module-inventory/erp-inv-service`：**114 tests, 0 failures, 0 errors** ✅（CostAdjustmentService 成本调整单 rollup + LandedCost 自身 PK re-fetch 未改）
- `module-finance/erp-fin-service`：**BUILD SUCCESS** ✅（CreditFacilityInterestVoucherBuilder 币种解析 + BankReconAdjustmentVoucherBuilder 科目解析 + ErpFinBadDebtProcessor 红冲回退 ArApItem）
- `module-projects/erp-prj-service`：**67 tests, 0 failures, 0 errors** ✅（ProjectCostAggregator 工时成本聚合 resolveLaborSubjectId）
- `module-assets/erp-ast-service`：**78 tests, 0 failures, 0 errors** ✅（ErpAstInventoryProcessor 盘点 + ErpAstMergeProcessor 合并 loadSources）

行为不变验证：ORM `<to-one>` 关系 getter 返回与 `daoFor(X).getEntityById(entity.getXxxId())` / `xxxDao().getEntityById(entity.getXxxId())` 同一托管实体（同 ORM 会话、同主键）。原 ternary（`xxxId != null ? getEntityById(...) : null`，#8 ErpAstMergeProcessor）与 getter 语义等价（FK 为 null 时 getter 返回 null）。各域审批/过账/成本核算/合并状态机测试全绿即等价证明。

Exit Criteria:

- [x] safe 子集全部重构为 ORM 导航，受影响域 `mvn test` 全绿（单模块测试启动成功 + 行为不变）
- [x] Type 2 豁免站点逐条登记理由

### Phase 3 — checker 基线下降 + 全形态收尾结论 + 文档对齐

Status: completed
Targets: `docs/audits/compliance-baseline.md`（R2b/R2c 下降）、`docs/analysis/governed-path-cost-evaluation.md`（§3.6 variable-split 收尾结论）、`docs/audits/2026-07-23-0000-architecture-governance-review.md`（§F1 successor 更新）
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 2 完成

- [x] `Proof`：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ 复跑 `bash docs/audits/nop-compliance-checker.sh` 记录 R2b/R2c/R2d 新基线（较 314/1071/27 下降，下降量 = safe 子集站点数）；更新 `docs/audits/compliance-baseline.md` 基线表 + machine-readable 块 + 增量注记。
  - Skill: none
- [x] `Add`：`governed-path-cost-evaluation.md` §3.6 补 variable-split 落地证据 + 收尾结论（`getEntityById(FK)` chained + variable-split 两形态全域清零；Type 2 豁免清单；ORM-gap successor）；治理审查 §F1 successor 更新（variable-split 收尾 + 残余 Type 4 / findAllByQuery residual 边界）。
  - Skill: none

#### Phase 3 验证证据

- `mvn clean install -DskipTests`：**154 模块 BUILD SUCCESS** ✅
- checker 复跑实测：R2b **314**（不变）/ R2c **1065**（-6）/ R2d **27**（不变）/ R2a **37**（不变）。下降量精确匹配：R2c -6 = 6 处 variable-split 移除局部 dao 变量声明（CostAdjustmentService/CreditFacilityInterestVoucherBuilder/BankReconAdjustmentVoucherBuilder/ProjectCostAggregator×2/ErpAstMergeProcessor）；2 处 helper-wrapped（ErpAstInventoryProcessor/ErpFinBadDebtProcessor）改 getter 但 helper 方法仍含 daoFor 供 newEntity/saveEntity，不计入下降。R2b/R2d/R2a 不变（重构站点全在 Service/Processor/Builder 非 BizModel，且非 Processor daoFor(ErpMd*)）。
- `docs/audits/compliance-baseline.md`：基线表 R2c + machine-readable 块已更新（1071→1065），含 variable-split 增量注记。
- `docs/analysis/governed-path-cost-evaluation.md`：§3.6 variable-split 收尾落地证据 + 全形态收尾结论已补（chained + variable-split 累计 45 处，R2c 1108→1065 累计 -43）。
- `docs/audits/2026-07-23-0000-architecture-governance-review.md` §F1：variable-split 收尾进展已补（两形态全域清零 + 残余边界）。

Exit Criteria:

- [x] 全仓 BUILD SUCCESS + checker R2b/R2c 基线下降并记录（authoritative full-repo gate 见 Closure Gates）
- [x] `getEntityById(FK)` 全形态（chained + variable-split）收尾结论记录（清零 + Type 2 豁免清单 + 残余边界明确）

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_06e34597effeEfiivi3I7LZRrh`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-24) — 0 Blocker / 1 Major / 3 Minor，全部 load-bearing 事实主张经实时仓库逐项核实。**Major-1（R1/R5）**：Phase 1 grep regex `dao\s*=\s*daoFor\(...` 实测返回 **0 命中**（实际代码含 `daoProvider.daoFor(` / `daoProvider().daoFor(` 前缀，`\s*` 仅匹配空白），已校正为可工作 regex `Dao\s*=\s*\w*[Dd]ao\w*\.?\w*\(?\.?daoFor\(...` + 辅助 helper-wrapped grep。**Minor-1（R1）**：#8 `ErpAstInventoryProcessor:340` + #13 `ErpFinBadDebtProcessor:139` 实为 helper-wrapped（`assetDao().getEntityById()`），非 variable-split 形态，已标注 Phase 1 单独分类。**Minor-2（R7）**：Phase 2 Item Types "Fix-heavy" 不实（3 项 Fix+Proof+Add），已改 `Fix | Proof | Add`。**Minor-3（R1）**：域覆盖声明漏 crm/quality（新发现 `LeadActivityDerivationHelper:40`/`SpcSamplingService:110` 等），已扩域覆盖 + 双向对账自检。successor 触发条件经审查确认**合法满足**（分类工作本身即触发，与 0605-3→2000-1 同型自驱动模型一致）。R1-R14 + anti-slack 修订后 PASS。
- Independent draft review iteration 2: `needs revision` (`ses_06e2815cbffekymWwxAoCX1h8m`，独立 general 子代理，新会话冷重播，2026-07-24) — 0 Blocker / 1 Major / 2 Minor。iter-1 的 Minor-1（helper-wrapped）/Minor-2（item types）/Minor-3（域覆盖）确认 genuine 解决。**Major-1（R1/R5）**：iter-1 修正的 regex `Dao\s*=...` 要求变量名以大写 `Dao` 结尾，**仍系统性漏看小写 `dao` 变量**（实测 `Dao` 命中 18 行 vs `[Dd]ao` 69 行；候选清单内 #7/#10/#12/#14/#15/#16 共 6 站点用小写 `dao` 被漏看）+ 候选清单（16 站点）远非穷举（`IEntityDao<...>`-typed regex 实测 ~51 文件）。已校正为按 `IEntityDao<[^>]+>` 类型声明匹配（不依赖变量名大小写）+ 明示候选清单为起点子集、权威枚举以 regex 输出（真实宇宙）为准 + self-check 以 regex 输出为真相源。**Minor-1（R7）**：Phase 1 `Item Types: Explore | Decision | Proof` 但无 Explore 项，已改 `Decision | Proof`。**Minor-2（R1）**：候选表 #1/#2 标「行漂移 129→129」实为行号一致，已校正。successor 触发 + 三阶段结构 + 基线值仍确认 sound。
- Independent draft review iteration 3: `needs revision` → 修订后收敛 (`ses_06e186506ffeNjWnaO9T5F8zm3`，独立 general 子代理，新会话冷重播，2026-07-24) — 0 Blocker / 1 Major / 1 Minor。iter-2 Major-1（小写 `dao` 变量盲区）确认 **genuine 解决**：`IEntityDao<...>` 类型声明匹配实测捕获 520 处（352 小写 `dao` + 168 大写 `Dao`），候选清单内 #7/#10/#14/#15 全部在位。iter-2 Minor-1/Minor-2 亦确认解决。**iter-3 新 Major（R1/R5）**：校正后 regex `\w+\.?\(?\.?daoFor\(` 漏看 `daoProvider().daoFor(` **括号前缀形态**（7 文件缺失，含 projects `ErpPrjTimesheetBizModel:206` 干净 Type 1 候选），与 Current Baseline 自述「codebase 含两种前缀形态」矛盾。审查者已**自行验证修复方案**：`\w+(\(\))?\.daoFor\(` 返回 58 文件，经 broad catch-all regex `comm` 比对**零残差**（捕获全部前缀变体）。**iter-3 Minor**：候选表 #11 残留同型「行漂移 172→172」（iter-2 仅修 #1/#2 漏看 #11）。**修订（执行者据审查自行验证方案落地）**：regex 已改为 `\w+(\(\))?\.daoFor\(`（两处：Current Baseline + Phase 1 Proof），~51 文件 → ~58 文件（3 处），#11 标注校正。审查者明示「修订后可激活」，且修复方案经其独立验证零残差，故不再开 iter-4，草案审查收敛 → `Plan Status: active`。R1-R14 + anti-slack 修订后 PASS。

## Closure Gates

> 本计划触及服务层 Java（daoFor→ORM 导航重构），无 ORM/契约/ext:dict/biz 方法签名/页面变更。完整仓库验证：`mvn clean install -DskipTests`（154 模块）+ 受影响域 `mvn test`（单模块启动验证）+ checker 复跑（R2b/R2c 基线下降记录）。

- [x] 范围内行为完成（variable-split safe 子集重构 + Type 2 豁免登记 + 候选集完备性自检）
- [x] 相关文档对齐（compliance-baseline + governed-path-cost-evaluation §3.6 + 治理审查 F1）
- [x] 已运行验证：`mvn clean install -DskipTests` + 受影响域 `mvn test`（单模块启动成功）+ checker 复跑（R2b/R2c 下降记录，非回归）
- [x] 无范围内项目降级为 deferred/follow-up（ORM-gap 是 ask-first 保护区域排除；Type 2 是关系导航不安全的保留豁免，非范围缩减；Type 4 / findAllByQuery 为既定 successor/residual）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### Type 2 会话存活豁免站点（voucher-by-link 循环逐条加载，7 处）

- Classification: `watch-only residual`
- Why Not Blocking Closure: voucher-by-link 循环逐条加载经 plan 明确定义为 Type 2 会话存活豁免（对齐 batch 1 `ErpFinVoucherBizModel:154,161` 豁免先例「避免依赖 to-many 懒加载的会话存活」+ 批量读模式）。`ErpFinVoucherBillR` 虽有 `<to-one name="voucher">`（finance.orm.xml:607），但该子模式经 plan 裁决保留豁免。
- 登记站点：
  - `CostAdjustmentPostingDispatcher:127-129`（循环加载 voucher + 写 reversed）
  - `ErpFinPostingProcessor:474-476`（alreadyPosted 幂等检查）
  - `ErpFinPostingProcessor:839-841`
  - `ErpFinPostingProcessor:857-860`
  - `ErpFinPostingProcessor:900-902`
  - `AdvanceOffsetOrchestrator:183-185`
  - `BadDebtProvisionService:169-172`
- Successor Required: `no`（除非 ORM 关系加载策略变更触发重新评估）

### Type 1 ORM-gap 子集（需补 ORM `<to-one>`）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 需 ORM 关系建模（保护区域，ask-first）。
- Successor Required: `yes`（触发条件：ORM `<to-one>` 关系授权 + owner doc 明示关系语义）

### Type 4 跨域写/读 daoFor（~10-30 处）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 阻塞中——需 nop-entropy 平台 lazy/SPI 解耦或保留登记豁免（成本评估 §3.2）。
- Successor Required: `yes`（触发条件：nop-entropy 提供 lazy/SPI 解耦 或 业务方要求封堵 daoFor 直访）

### `findAllByQuery` Type 1 子集

- Classification: `watch-only residual`
- Why Not Blocking Closure: 2000-1 已评估 113 处可机械替换候选 <10 → 触发条件未满足。
- Successor Required: `no`（触发条件：≥10 处可机械替换候选）

### `ErpCtRebateSettlementBizModel` chained 只读站点（Non-Goal 排除文件）

- Classification: `optimization candidate`
- Why Not Blocking Closure: posting-exemptions.md 登记豁免文件，Non-Goal 明示排除，独立 successor。
- Successor Required: `yes`（触发条件：posting-exemptions.md 豁免收敛时一并处理只读站点，或独立裁决 carve-out）

## Closure

Status Note: completed — variable-split 子模式全域分类 + safe 子集重构 + Type 2 豁免登记 + checker 基线下降 + 全形态收尾结论记录。`getEntityById(FK)` chained + variable-split 两形态生产站点全域清零。

Closure Audit Evidence:

- Auditor / Agent: `ses_06df814f8ffe6NfzG6pBwjzZLc`（独立 general 子代理，新会话冷重播无执行者上下文，2026-07-24）— CLOSURE_AUDIT_VERDICT: **PASS**（0 Blocker / 0 Major / 0 Minor）。逐项核实：(A) 8 处 safe 重构代码 — 全 PASS，旧 `getEntityById(entity.getXxxId())` 形态已消失，getter 名匹配 ORM `<to-one>`，无悬空局部变量；(B) 8 处 ORM 关系 — 全 PASS，parent entity 边界 + leftProp 正确（inventory.orm.xml:1292 material / finance.orm.xml:1534 fundAccount + :1023 subject + :1660 sourceArApItem / projects.orm.xml:248 activityType + :119 projectType / assets.orm.xml:1258 asset + :1114 sourceAsset）；(C) 回归 grep — 8 处 safe 站点不再出现，7 处 Type 2 voucher-by-link 豁免站点正确保留，ORM-gap #16 弱指针正确保留；(D) Type 2 豁免登记 — PASS；(E) compliance-baseline R2c=1065 双块一致 + 增量注记在位；(F) plan 文本一致性 — 全 PASS。独立 `mvn test-compile -am`（4 受影响模块）BUILD SUCCESS EXIT=0。

Follow-up:

- Type 2 会话存活豁免站点（若 ORM 加载策略变更）
- Type 1 ORM-gap 子集（触发条件见上）
- Type 4（触发条件见上）
- `ErpCtRebateSettlementBizModel` chained 只读站点（触发条件见上）
- helper-wrapped chained-via-helper 广义变体（独立 successor 边界，触发条件：batch 2 literal-daoFor chained 收敛延伸至 helper 变体）
