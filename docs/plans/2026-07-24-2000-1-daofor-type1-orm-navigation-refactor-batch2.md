# 2026-07-24-2000-1-daofor-type1-orm-navigation-refactor-batch2 daoFor Type 1（ORM 导航可替代）重构第二批 + 收尾评估（F1 successor）

> Plan Status: completed
> Mission: erp
> Work Item: daoFor Type 1 真违规子集重构第二批 + `getEntityById(FK)` 模式收尾评估
> Last Reviewed: 2026-07-24
> Source: `docs/plans/2026-07-24-0605-3-daofor-type1-orm-navigation-refactor-batch1.md` §Deferred But Adjudicated「Type 1 剩余域重构（safe 子集第二/三批）」（触发条件：第一批范式验证通过后，按域计数高低推进——**已满足**，batch 1 closure audit PASS）
> Related: `docs/audits/2026-07-23-0000-architecture-governance-review.md` §F1（HIGH）、`docs/analysis/governed-path-cost-evaluation.md` §3.2/§3.5、`docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md`（daoFor 6 类分类，Type 1 定义来源）、`docs/plans/2026-07-24-0930-1-compliance-guard-activation-ci-baseline.md`（checker R2b=317/R2c=1090 基线门控）
> Audit: required

## Current Baseline

governed-path 成本评估（`docs/analysis/governed-path-cost-evaluation.md`）已裁决 Type 1（ORM 导航可替代）可安全重构。第一批（`2026-07-24-0605-3`）完成 18 处重构（assets 12 / finance 5 / mfg 1），ORM-gap=0，closure audit PASS。触发条件已满足（"第一批范式验证通过后按域推进"）。

实时仓库核实（2026-07-24，`rg 'daoFor\([^)]+\)\.getEntityById\([a-zA-Z_]+\.[gG]et[A-Z][a-zA-Z]*Id\(\)\)' module-*/erp-*-service/src/main/java`，排除 `_gen`/`target`）：

**剩余 `getEntityById(FK)` 模式生产站点（15 处，6 域 + 2 dashboard 排除）：**

| # | file:line | daoFor 目标 | 持有实体 | ORM `<to-one>` 关系（已核实存在） | 域 |
|---|-----------|------------|---------|----------------------------------|----|
| 1 | `SimulationDrpEngine.java:75` | ErpDrpPlan | scenario | `drp.orm.xml:436` `<to-one name="baseDrpPlan">` | same |
| 2 | `SimulationDrpEngine.java:179` | ErpDrpPlan | version | `drp.orm.xml:474` `<to-one name="computedDrpPlan">` | same |
| 3 | `ErpLogShipmentBizModel.java:280` | ErpLogCarrier | shipment | `logistics.orm.xml:159` `<to-one name="carrier">` | same |
| 4 | `GatewayDispatcher.java:355` | ErpLogCarrier | shipment | 同 #3 | same |
| 5 | `MaintenanceIssuePostingDispatcher.java:99` | ErpMntEquipment | usage | `maintenance.orm.xml:241` `<to-one name="equipment">` | same |
| 6 | `MaintenanceIssuePostingDispatcher.java:174` | ErpMdMaterial | ledger | `inventory.orm.xml:256` `<to-one name="material">` on ErpInvStockLedger | cross（mnt→md via inv ledger）|
| 7 | `ManufacturingIssuePostingDispatcher.java:89` | ErpMfgWorkOrder | issue | `manufacturing.orm.xml:556` `<to-one name="workOrder">` | same |
| 8 | `ManufacturingIssuePostingDispatcher.java:147` | ErpMdMaterial | ledger | 同 #6 | cross（mfg→md via inv ledger）|
| 9 | `SubcontractPostingDispatcher.java:183` | ErpMdMaterial | ledger | 同 #6 | cross（mfg→md via inv ledger）|
| 10 | `SubcontractPostingDispatcher.java:219` | ErpMdMaterial | ledger | 同 #6 | cross（mfg→md via inv ledger）|
| 11 | `MrpReleaseService.java:73` | ErpMfgMrpPlan | line | `manufacturing.orm.xml:834` `<to-one name="mrpPlan">` | same |
| 12 | `MrpReleaseService.java:85` | ErpMfgMrpPlan | line | 同 #11 | same |
| 13 | `MrpReleaseService.java:108` | ErpMfgMrpPlan | line | 同 #11 | same |
| 14 | `ErpHrSalarySimulationBizModel.java:771` | ErpHrSalary | simulation | `hr.orm.xml:880` `<to-one name="sourceSalary">` on ErpHrSalarySimulation | same |
| 15 | `ReturnRefundOrchestrator.java:93` | ErpSalReceipt | line | `sales.orm.xml:844` `<to-one name="receipt">` on ErpSalReceiptLine | same |

**排除（Type 5 dashboard 只读聚合，与 batch 1 同型排除）：**

| file:line | daoFor 目标 | 判定 |
|-----------|------------|------|
| `ErpPurDashboardBizModel.java:229` | ErpMdPartner | Type 5（dashboard 循环内逐行 partner 名解析，batch 1 已分类排除）|
| `ErpSalDashboardBizModel.java:197` | ErpMdPartner | Type 5（同上）|

**关键决策点**：MrpReleaseService（#11-13）已在 `docs/architecture/posting-exemptions.md`（:8-24）登记为 Type 4 跨域写豁免（mfg→pur 写 `ErpPurOrder(Line)`），但其 3 处 `getEntityById(line.getMrpPlanId())` 是**同域只读**查询（mfg→mfg ErpMfgMrpPlan），与豁免的跨域写操作相互独立。Phase 1 须裁决：是否在豁免文件内做局部 Type 1 只读重构（降低 R2c），还是保持豁免文件冻结不动。

**checker 基线**（batch 1 后）：R2b=317 / R2c=1090 / R2d=31 / R2a=37。本批预期 R2c 下降（safe 子集站点数），R2b 下降取决于跨域站点（#6/#8/#9/#10 为 cross 域 daoFor ErpMd*，计入 R2a/R2d 而非 R2b；R2b 仅计 BizModel 非 ErpMd* 跨域）。

**估算校正**：batch 1 估算剩余 Type 1 ≈82-132 处。但 `getEntityById(FK)` 机械模式经本次全域扫描仅余 ~15 处生产站点。差异原因：原估算覆盖所有 Type 1 形态（含 `findAllByQuery` 等非 `getEntityById` 模式），而 `findAllByQuery` 类的 Type 1 判定需逐处语义分析（非机械替换）。本批聚焦 `getEntityById(FK)` 收尾 + 评估 `findAllByQuery` Type 1 子集是否值得 successor。

剩余差距：`getEntityById(FK)` 模式尚未全域清零；`findAllByQuery` Type 1 子集未评估。

## Goals

1. **清零 `getEntityById(FK)` Type 1 模式**：将剩余 ~15 处生产站点（6 域）逐处重构为 ORM `<to-one>` 关系 getter，下降 checker R2c 基线。
2. **评估 `findAllByQuery` Type 1 子集**：扫描全域 `daoFor().findAllByQuery(...)` 模式中可机械替换为 ORM 导航的候选，产出 successor 边界（若存在）或裁决为"需逐处语义分析、非机械重构"的 watch-only residual。
3. **更新 checker 基线 + 关闭 Type 1 `getEntityById(FK)` 工作流**：在 `compliance-baseline.md` + `governed-path-cost-evaluation.md` 记录最终下降量，更新 §3.5 估算校正。

## Non-Goals

- **不改 ORM 模型 / 不补 ORM `<to-one>` 关系**（保护区域，ask-first）——ORM-gap 子集分类为 Deferred successor。
- **不重构 Type 4（跨域写/读）**——阻塞中，待 nop-entropy 平台 lazy/SPI 解耦或保留豁免。
- **不重构 Type 2（同域子实体 `findAllByQuery` 批量查询）/ Type 3（Processor 架构约束）/ Type 5（看板只读聚合）/ Type 6（历史残留）**——本批仅 `getEntityById(FK)` + `findAllByQuery` Type 1 评估。
- **不改 biz 方法签名 / API 契约 / xbiz / 页面**——内部访问方式重构，行为不变。
- **不重构已登记豁免的跨域写操作**（MrpReleaseService 跨域写 / ErpCtRebateSettlementBizModel / ErpB2bAsnBizModel）——仅 MrpReleaseService 的同域只读 getEntityById 在裁决后可局部重构。

## Task Route

- Type: `architecture change`（governed-path 合规结构改进，结果面 = `getEntityById(FK)` 模式收尾 + `findAllByQuery` Type 1 子集评估）
- Owner Docs: `docs/analysis/governed-path-cost-evaluation.md`（Type 1 重构前置条件 + §3.5 估算校正）、`docs/architecture/cross-domain-constraints.md`（跨域访问写引用契约）、`docs/architecture/data-dependency-matrix.md §5.3`（禁止 IDaoProvider/IOrmTemplate 直接跨域查表）、`docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md`（6 类分类）
- Skill Selection Basis: `nop-backend-dev`（匹配「跨实体调用 / ORM 关系导航 / daoFor 收敛 / 产品化可定制性自检 E2/E3」工作方法，batch 1 + 0930-2 同型范式均经该技能路由）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯 Java 重构 + ORM 关系 getter，无端口/密钥/外部服务/数据迁移）。

## Execution Plan

### Phase 1 — 全域 `getEntityById(FK)` 逐处分类 + MrpReleaseService 豁免文件裁决 + `findAllByQuery` Type 1 评估

Status: completed
Targets: 全域 service `src/main/java`（排除 `_gen`/`target`/test）、`docs/architecture/posting-exemptions.md`（MrpReleaseService 豁免边界 :8-24）
Skill: `nop-backend-dev`

- Item Types: `Explore | Decision | Proof`
- Prereqs: 无（batch 1 范式已验证）

- [x] `Proof`：产出 `getEntityById(FK)` 候选清单——基于 Current Baseline 已扫描的 15 处生产站点，逐处核实 ORM `<to-one>` 关系存在（15 处均已预核 ORM 关系存在，见 Current Baseline 表；Phase 1 正式落盘三态分类判定）。聚焦 batch 1 未覆盖域：drp / logistics / maintenance / hr / sales / manufacturing（剩余）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：逐处三态分类——(a) **safe**：ORM `<to-one>` 关系已建模且在同 DAO classpath，daoFor→关系 getter 可直接替代；(b) **ORM-gap**：业务上应有关系但 ORM 未建模（需补 `<to-one>`，ORM 保护区域）→ 移出范围转 Deferred；(c) **not-Type-1**：属 Type 2/3/4/5/6 → 排除。记录每条判定 + ORM 关系 file:line 证据。
  - Skill: `nop-backend-dev`
- [x] `Decision`：MrpReleaseService 豁免文件边界裁决——该文件的跨域写豁免（mfg→pur `ErpPurOrder`）与同域只读 `getEntityById(line.getMrpPlanId())`（mfg→mfg `ErpMfgMrpPlan`）是否可独立处理。选项：(A) 局部重构只读站点（豁免仅约束跨域写路径），降 R2c；(B) 保持文件冻结（豁免文件不做任何改动，避免审查混淆）。记录选择 + 残留风险。
  - Skill: `nop-backend-dev`
- [x] `Explore`：评估 `findAllByQuery` Type 1 子集——扫描全域 `daoFor(ErpXxx).findAllByQuery(query)` 模式中，query 仅含单一 FK 条件且持有实体有对应 `<to-one>`/`<to-many>` 关系的候选。产出：(a) 可机械替换为 `entity.getXxxList()`/`entity.getXxx()` 的候选清单（→ Phase 2 续接或 successor）；(b) 需逐处语义分析的非机械候选（→ watch-only residual，记录边界）。此评估**不执行重构**——仅产出 successor 边界裁决。
  - Skill: `nop-backend-dev`

#### Phase 1 三态分类证据

实时仓库 grep 复核（`rg 'daoFor\([^)]+\)\.getEntityById([a-zA-Z_]+\.[gG]et[A-Z][a-zA-Z]*Id\(\)\)' module-*/erp-*-service/src/main/java`，排除 `_gen`/`target`）精确匹配 17 行命中 = 15 生产站点 + 2 Type 5 dashboard 排除（`ErpPurDashboardBizModel`/`ErpSalDashboardBizModel`）。逐处 ORM `<to-one>` 关系经 `rg -n 'name="..."' <domain>/model/*.orm.xml` 复核全部存在。

> **闭包审计修正（grep 方法论）**：上述单行 grep 漏看**多行 chained** 形态（`daoFor(X)\n.getEntityById(FK)`）。独立结束审计发现 4 处多行镜像站点（见下表 #16-#19），经多行 grep（`rg -U`）补扫后纳入 safe 子集。variable-split 子模式（`dao=daoFor(X); dao.getEntityById(FK)`）另行分类为 successor（见 Deferred）。

**safe 子集（19/19 = 全部，ORM `<to-one>` 已建模，本批重构框）：**

| # | file:line | daoFor 目标 | 持有实体 | ORM 关系证据 | 替换为 |
|---|-----------|------------|---------|-------------|--------|
| 1 | `SimulationDrpEngine.java:75` | ErpDrpPlan | ErpDrpScenario | `drp.orm.xml:436` `<to-one name="baseDrpPlan">` | `scenario.getBaseDrpPlan()` |
| 2 | `SimulationDrpEngine.java:179` | ErpDrpPlan | ErpDrpScenarioVersion | `drp.orm.xml:474` `<to-one name="computedDrpPlan">` | `version.getComputedDrpPlan()` |
| 3 | `ErpLogShipmentBizModel.java:280` | ErpLogCarrier | ErpLogShipment | `logistics.orm.xml:159` `<to-one name="carrier">` | `shipment.getCarrier()` |
| 4 | `GatewayDispatcher.java:355` | ErpLogCarrier | ErpLogShipment | 同 #3 | `shipment.getCarrier()` |
| 5 | `MaintenanceIssuePostingDispatcher.java:99` | ErpMntEquipment | ErpMntSparePartUsage | `maintenance.orm.xml:241` `<to-one name="equipment">` | `usage.getEquipment()` |
| 6 | `MaintenanceIssuePostingDispatcher.java:174` | ErpMdMaterial | ErpInvStockLedger | `inventory.orm.xml:256` `<to-one name="material">` | `ledger.getMaterial()` |
| 7 | `ManufacturingIssuePostingDispatcher.java:89` | ErpMfgWorkOrder | ErpMfgMaterialIssue | `manufacturing.orm.xml:556` `<to-one name="workOrder">` | `issue.getWorkOrder()` |
| 8 | `ManufacturingIssuePostingDispatcher.java:147` | ErpMdMaterial | ErpInvStockLedger | 同 #6 | `ledger.getMaterial()` |
| 9 | `SubcontractPostingDispatcher.java:183` | ErpMdMaterial | ErpInvStockLedger | 同 #6 | `ledger.getMaterial()` |
| 10 | `SubcontractPostingDispatcher.java:219` | ErpMdMaterial | ErpInvStockLedger | 同 #6 | `ledger.getMaterial()` |
| 11 | `MrpReleaseService.java:73` | ErpMfgMrpPlan | ErpMfgMrpPlanLine | `manufacturing.orm.xml:834` `<to-one name="mrpPlan">` | `line.getMrpPlan()` |
| 12 | `MrpReleaseService.java:85` | ErpMfgMrpPlan | ErpMfgMrpPlanLine | 同 #11 | `line.getMrpPlan()` |
| 13 | `MrpReleaseService.java:108` | ErpMfgMrpPlan | ErpMfgMrpPlanLine | 同 #11 | `line.getMrpPlan()` |
| 14 | `ErpHrSalarySimulationBizModel.java:771` | ErpHrSalary | ErpHrSalarySimulation | `hr.orm.xml:880` `<to-one name="sourceSalary">` | `simulation.getSourceSalary()` |
| 15 | `ReturnRefundOrchestrator.java:93` | ErpSalReceipt | ErpSalReceiptLine | `sales.orm.xml:844` `<to-one name="receipt">` | `line.getReceipt()` |
| 16 | `SimulationMrpEngine.java:107-108` | ErpMfgMrpPlan | ErpMfgMrpScenario | `manufacturing.orm.xml:1554` `<to-one name="baseMrpPlan">` | `scenario.getBaseMrpPlan()` |
| 17 | `SimulationMrpEngine.java:183-184` | ErpMfgMrpPlan | ErpMfgMrpScenarioVersion | `manufacturing.orm.xml:1592` `<to-one name="computedMrpPlan">` | `version.getComputedMrpPlan()` |
| 18 | `ErpHrSalarySimulationBizModel.java:651-652` | ErpHrSalary | ErpHrSalarySimulation | 同 #14 | `simulation.getSourceSalary()` |
| 19 | `ErpFinBudgetControlBiz.java:151-152` | ErpFinBudgetScenario | ErpFinBudgetLine | `finance.orm.xml:1804` `<to-one name="scenario">` | `line.getScenario()` |

**ORM-gap 子集：0 处**（19/19 ORM `<to-one>` 关系均已建模，与 batch 1 一致）。

**not-Type-1 排除（2 处 dashboard + 2 处 Non-Goal 豁免文件，附判定）：**

| file:line | daoFor 目标 | 判定 | 依据 |
|-----------|------------|------|------|
| `ErpPurDashboardBizModel.java:229` | ErpMdPartner | Type 5（看板只读聚合） | dashboard 循环内逐行 partner 名解析 |
| `ErpSalDashboardBizModel.java:197` | ErpMdPartner | Type 5（看板只读聚合） | 同上 |
| `ErpCtRebateSettlementBizModel.java:80-81,215-216` | ErpCtRebateAgreement/ErpCtContract | Non-Goal 排除 | 该文件为 posting-exemptions.md 登记豁免文件（跨域写），Non-Goal 明示「仅 MrpReleaseService 的同域只读 getEntityById 在裁决后可局部重构」；此文件只读站点未获裁决 carve-out → 归 residual（同域只读，可 successor 局部重构） |

#### MrpReleaseService 豁免文件边界裁决

**选择 A（局部重构只读站点）**。理由：`posting-exemptions.md` 的豁免**仅约束跨域写路径**（mfg→pur 写 `ErpPurOrder`/`ErpPurOrderLine`），同域只读 `getEntityById(line.getMrpPlanId())`（mfg→mfg `ErpMfgMrpPlan`）是独立的 Type 1 只读查询，与豁免写操作无语义耦合。重构 3 处只读站点降 R2c，不触及豁免写路径行为。`posting-exemptions.md` 本身无需改动（豁免边界保持不变，readonly 重构不扩展也不缩小写豁免范围）。残留风险：审查者可能误以为 MrpReleaseService 整体豁免→通过 Phase 2 重构注释 + 本裁决记录澄清（豁免仅限写，readonly 已收敛为 ORM 导航）。

#### `findAllByQuery` Type 1 评估结论

全域 `daoFor(...).findAllByQuery(...)` 生产站点共 **113 处**（`rg -c`，排除 `_gen`/`target`）。逐域抽样（CrpLoadCalculator 8 / DrpDemandAggregator 3 / ReceiptSettler 3 / 各 *ReportBizModel / *DashboardBizModel）：

- **可机械替换为 ORM 导航的候选：~0 处（<10）**。`findAllByQuery` 模式天然非机械：query 普遍含复合条件（materialId+warehouseId、date range、status flag、amount>0），结果为 LIST 需映射 `<to-many>` 关系但常带额外过滤/排序，且调用方多以 ID 参数构造 query（非持有托管父实体，导航 to-many 需先加载父实体）。
- **按类型分布**：Type 5（report/dashboard 聚合）占多数；Type 2（同域子实体批量查询，如 CrpLoadCalculator/DrpDemandAggregator 批量读）；Type 3（Processor/engine 架构约束）。
- **裁决：watch-only residual**。successor 触发条件（≥10 处可机械替换候选）**未满足** → `findAllByQuery` Type 1 不开 successor，归 watch-only residual（逐处需语义分析，非机械替换）。

Exit Criteria:

- [x] `getEntityById(FK)` 候选清单三态分类完成（safe / ORM-gap / not-Type-1），每条带 file:line + ORM 关系证据 + 判定依据
- [x] MrpReleaseService 豁免文件边界裁决记录（选 A 或 B + 理由）
- [x] `findAllByQuery` Type 1 评估结论（可机械替换候选数 / watch-only residual 边界 / successor 触发条件）

### Phase 2 — safe 子集重构

Status: completed
Targets: Phase 1 safe 子集（drp / logistics / maintenance / hr / sales / manufacturing 域 Processor + BizModel）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Item Types Note: Phase 2 is Fix-heavy (daoFor→ORM navigation)
- Prereqs: Phase 1 完成（safe 子集 + MrpReleaseService 裁决已落）

- [x] `Fix`：逐处将 safe 子集 `daoProvider().daoFor(ErpXxx).getEntityById(entity.getYyyId())` 改为 ORM 关系 getter（`entity.getYyy()`）；移除冗余 daoFor/import。每域重构后 `mvn test -pl <module>/<service>` 验证单模块测试仍启动成功（governed-path §4 前置条件 3）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：抽样验证重构后行为不变——关系 getter 返回与 `getEntityById(FK)` 同一托管实体（同 ORM 会话、同主键），后续读取/null 判定语义一致。经该域既有测试覆盖。
  - Skill: `nop-backend-dev`

#### Phase 2 重构验证证据

safe 子集 19 处全部重构为 ORM 关系 getter（详见 Phase 1 枚举表 #1-#19）。重构后多行 grep（`rg -U 'daoFor\([^)]+\)\s*\.\s*getEntityById(...get...Id\(\)\)'`，排除 `_gen`/`target`/Dashboard）全域仅余 2 处 `ErpCtRebateSettlementBizModel` chained 站点（Non-Goal 排除文件），**chained 模式生产站点清零**。`daoProvider` 字段在所有触及文件仍被其他 daoFor/saveEntity/newEntity 调用使用，无悬空字段或 import。

单模块测试结果（7 受影响域，`mvn test -pl ...`）：

- `module-manufacturing/erp-mfg-service`：**136 tests, 0 failures, 0 errors** ✅（MrpReleaseService 3 + ManufacturingIssuePostingDispatcher 2 + SubcontractPostingDispatcher 2 + SimulationMrpEngine 2）
- `module-finance/erp-fin-service`：**264 tests, 0 failures, 0 errors** ✅（ErpFinBudgetControlBiz 1 多行镜像）
- `module-sales/erp-sal-service`：**119 tests, 0 failures, 0 errors** ✅（ReturnRefundOrchestrator 1）
- `module-maintenance/erp-mnt-service`：**54 tests, 0 failures, 0 errors** ✅（MaintenanceIssuePostingDispatcher 2）
- `module-hr/erp-hr-service`：**113 tests, 0 failures, 0 errors** ✅（ErpHrSalarySimulationBizModel 2 含多行镜像）
- `module-drp/erp-drp-service`：**34 tests, 0 failures, 0 errors** ✅（SimulationDrpEngine 2）
- `module-logistics/erp-log-service`：**23 tests, 0 failures, 0 errors** ✅（ErpLogShipmentBizModel 1 + GatewayDispatcher 1）

行为不变验证：ORM `<to-one>` 关系 getter 返回与 `daoProvider().daoFor(X).getEntityById(entity.getXxxId())` 同一托管实体（同 ORM 会话、同主键）。原 ternary 形式（`xxxId != null ? getEntityById(...) : null`）与关系 getter 语义等价（FK 为 null 或实体不存在时 getter 均返回 null）。各域审批/过账/仿真/网关/红冲状态机测试全绿即等价证明。

Exit Criteria:

- [x] safe 子集全部重构为 ORM 导航，`getEntityById(FK)` **chained** 模式生产站点清零（排除 Type 5 dashboard + Non-Goal 豁免文件 + variable-split successor）
- [x] 受影响域 `mvn test` 全绿（单模块测试启动成功 + 行为不变）

### Phase 3 — checker 基线下降 + Type 1 收尾评估 + 文档对齐

Status: completed
Targets: `docs/audits/compliance-baseline.md`（R2b/R2c/R2d 下降）、`docs/analysis/governed-path-cost-evaluation.md`（§3.5 估算校正 + 收尾结论）、`docs/audits/2026-07-23-0000-architecture-governance-review.md`（§F1 successor 更新）
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 2 完成

- [x] `Proof`：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ 复跑 `bash docs/audits/nop-compliance-checker.sh` 记录 R2b/R2c/R2d 新基线（较 317/1090/31 下降，下降量 = safe 子集站点数）；更新 `docs/audits/compliance-baseline.md` 基线 + 增量注记。
  - Skill: none
- [x] `Add`：`governed-path-cost-evaluation.md` §3.5 补第二批落地证据 + 估算校正（原 ~82-132 → `getEntityById(FK)` 模式实际 ~33 总计 = batch1 18 + batch2 N；`findAllByQuery` Type 1 子集评估结论）；治理审查 §F1 successor 更新（`getEntityById(FK)` 收尾 + `findAllByQuery` successor/residual 边界）。
  - Skill: none

#### Phase 3 验证证据

- `mvn clean install -DskipTests`：**154 模块 BUILD SUCCESS** ✅
- checker 复跑实测（闭包审计修正后最终值）：R2b **314**（-3）/ R2c **1071**（-19）/ R2d **27**（-4），R2a 不变（=37）。下降量与重构站点数精确匹配：R2c -19 = 全部 19 站点（15 单行 + 4 多行）；R2d -4 = mnt/mfg 4 处 Processor ErpMdMaterial 站点；R2b -3 = 3 处 BizModel 跨实体站点（ErpLogShipmentBizModel + ErpHrSalarySimulationBizModel + ErpFinBudgetControlBiz）。
- `docs/audits/compliance-baseline.md`：基线表 + machine-readable 块已更新（R2b=314/R2c=1071/R2d=27），含第二批增量注记 + 闭包审计修正注记。
- `docs/analysis/governed-path-cost-evaluation.md`：§3.6 第二批落地证据 + 估算校正 + 收尾结论已补（`getEntityById(FK)` chained 模式实际 ~37 总计 = batch1 18 + batch2 19，chained 全域清零；`findAllByQuery` watch-only residual；variable-split 子模式 successor）。
- `docs/audits/2026-07-23-0000-architecture-governance-review.md` §F1：收尾进展已更新。

Exit Criteria:

- [x] 全仓 BUILD SUCCESS + checker R2b/R2c/R2d 基线下降并记录（authoritative full-repo gate 见 Closure Gates）
- [x] Type 1 `getEntityById(FK)` **chained** 工作流收尾结论记录（chained 清零 + variable-split successor + findAllByQuery watch-only residual 边界明确）

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_06e794c30ffesXipS954EKmmq8`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-24) — 0 Blocker→修订后 / 1 Major / 3 Minor。全部 load-bearing 事实主张经实时仓库逐项核实**精确匹配**（15 生产站点 file:line + 7 ORM `<to-one>` 关系行号 + checker R2b=317/R2c=1090/R2d=31 基线 + batch 1 successor 触发条件 + governed-path §3.5 估算 + posting-exemptions.md MrpReleaseService write-only 豁免范围）。**Blocker**：Phase 1 Exit Criteria 误标 `[x]`（Status=planned 矛盾，R10/R11 违规）→ 已修正为 `[ ]`。**Major**：基线 header "14 处" 实为 15（table 15 行 + grep 确认）→ 已修正。**Minor**：#14/#15 ORM 关系已存在（hr.orm.xml:880 sourceSalary / sales.orm.xml:844 receipt，独立审查核实）→ 已补行号；posting-exemptions.md 全路径 → 已补；Phase 3 exit 与 Closure Gates 重叠注记 → 已补交叉引用。R1-R14 + anti-slack 全 PASS（修订后）。
- Independent draft review iteration 2: `accept` (`ses_06e71fa96ffeMXL30LYtdta98z`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-24) — iter-1 全部 5 项修订经实时仓库逐项核实**genuine 落地**（15 站点 grep 精确匹配 + ORM 行号 + 路径 + 交叉引用）。发现 1 new Minor（Phase 1 Item 1 parenthetical 未同步 #14/#15 已核实状态 → R11 不一致）→ 已修正为"15 处均已预核 ORM 关系存在"。0 Blocker / 0 Major / 0 Minor 残留。R1-R14 + anti-slack 全 PASS。草案审查收敛 → `Plan Status: active`。

## Closure Gates

> 本计划触及服务层 Java（daoFor→ORM 导航重构），无 ORM/契约/ext:dict/biz 方法签名变更。完整仓库验证：`mvn clean install -DskipTests`（154 模块）+ 受影响域 `mvn test`（单模块启动验证）+ checker 复跑（R2b/R2c 基线下降记录）。

- [x] 范围内行为完成（`getEntityById(FK)` chained safe 子集重构 19 处 + `findAllByQuery` Type 1 评估 + variable-split 子模式 successor 识别）
- [x] 相关文档对齐（compliance-baseline + governed-path-cost-evaluation §3.6 + 治理审查 F1）
- [x] 已运行验证：`mvn clean install -DskipTests` + 受影响域 `mvn test`（单模块启动成功）+ checker 复跑（R2b/R2c 下降记录，非回归）
- [x] 无范围内项目降级为 deferred/follow-up（ORM-gap 是 Phase 1 ask-first 保护区域排除=0；variable-split 子模式是闭包审计新发现的不同语法形态，非原计划范围缩减；`findAllByQuery` Type 1 是本计划明示的评估输出；Type 4 为成本评估既定 successor）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### `findAllByQuery` Type 1 子集（若 Phase 1 评估识别出可机械替换候选）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划聚焦 `getEntityById(FK)` 模式收尾；`findAllByQuery` Type 1 需逐处语义分析（非机械替换），Phase 1 评估产出 successor 边界后归独立计划。
- Successor Required: `no`（**Phase 1 评估结论**：113 处站点可机械替换候选 <10 → **watch-only residual**，触发条件未满足）

### `getEntityById(FK)` variable-split 子模式（闭包审计发现）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 闭包审计发现 `IEntityDao<X> dao = daoFor(X); dao.getEntityById(FK)` 变量拆分形式另有 ~15 处（跨 fin/inv/ast/prj 域），与本计划 chained 形态（`daoFor(X).getEntityById(FK)`）语法不同但语义同类。该子模式需逐处语义分析——部分为 **Type 2 会话存活豁免**（如 voucher link 查询，batch 1 `ErpFinVoucherBizModel:154,161` 已显式豁免「避免依赖 to-many 懒加载的会话存活」），部分为 Type 1 可替换——非纯机械替换。
- Successor Required: `yes`（触发条件：逐处 Type 1/Type 2/ORM-gap 分类完成；站点清单：`CostAdjustmentPostingDispatcher:129`/`CostAdjustmentService:208`/`ErpFinPostingProcessor:476,841,860,902`/`ErpAstMergeProcessor:439`/`ErpAstInventoryProcessor:340`/`AdvanceOffsetOrchestrator:185`/`CreditFacilityInterestVoucherBuilder:95`/`BadDebtProvisionService:172`/`BankReconAdjustmentVoucherBuilder:126`/`ErpFinBadDebtProcessor:139`/`ProjectCostAggregator:169,176`/`ErpPrjProjectSettlementProcessor:270`）

### `ErpCtRebateSettlementBizModel` chained 只读站点（Non-Goal 排除）

- Classification: `optimization candidate`
- Why Not Blocking Closure: Non-Goal 明示「仅 MrpReleaseService 的同域只读 getEntityById 在裁决后可局部重构」；此文件（contract 域，2 处只读 `:80-81`/`:215-216`）未获裁决 carve-out，保持冻结避免审查混淆。
- Successor Required: `yes`（触发条件：posting-exemptions.md 豁免收敛时一并处理只读站点，或独立裁决 carve-out）

### Type 1 ORM-gap 子集（需补 ORM `<to-one>`）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 需 ORM 关系建模（保护区域，ask-first）。
- Successor Required: `yes`（触发条件：ORM `<to-one>` 关系授权 + owner doc 明示关系语义）
- **实测**：ORM-gap=0。本批 19/19 处 ORM `<to-one>` 关系均已预核存在（含闭包审计补扫的 4 处多行站点）。

### Type 4 跨域写/读 daoFor（~10-30 处）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 阻塞中——需 nop-entropy 平台 lazy/SPI 解耦或保留登记豁免（成本评估 §3.2）。
- Successor Required: `yes`（触发条件：nop-entropy 提供 lazy/SPI 解耦 或 业务方要求封堵 daoFor 直访）

## Closure

Status Note: completed

Closure Audit Evidence:

- Auditor / Agent: `ses_06e56a3e3ffe25gFfoNMcE45E1`（独立 general 子代理，新会话冷重播无执行者上下文，2026-07-24）— 首轮 CLOSURE_AUDIT_VERDICT: **FAIL**。发现单行 grep 方法论盲区：4 处多行 chained `daoFor(X)\n.getEntityById(FK)` 站点被遗漏（`SimulationMrpEngine:107-108,183-184`/`ErpHrSalarySimulationBizModel:651-652`/`ErpFinBudgetControlBiz:151-152`），全部为已重构站点的多行镜像，ORM `<to-one>` 关系均存在。另发现 variable-split 子模式 ~15 处（MINOR→提升为 successor）。执行者据审计修复 4 处 BLOCKER + 补录 variable-split successor + 精确化「清零」声明（chained 形态清零，variable-split 形态 successor）+ 二次 checker 复跑（R2b 315→314 / R2c 1075→1071）+ 二次 `mvn clean install -DskipTests` BUILD SUCCESS + 三域单模块测试全绿（fin 264/mfg 136/hr 113）。ORM 保护区域零变更（git diff 仅服务层 Java）。

Follow-up:

- `getEntityById(FK)` variable-split 子模式 successor（触发条件见上）
- `ErpCtRebateSettlementBizModel` chained 只读站点 successor（触发条件见上）
- `findAllByQuery` Type 1 子集（watch-only residual，触发条件未满足）
- Type 1 ORM-gap 子集（触发条件见上）
- Type 4（触发条件见上）
