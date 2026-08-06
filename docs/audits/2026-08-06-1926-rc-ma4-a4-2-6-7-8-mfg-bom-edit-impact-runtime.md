# MA4 A4.2.6 + A4.2.7 + A4.2.8 运行时验证报告 — BOM 内容编辑后已开工工单成本/物料需求运行时影响确认（P1-RC-009 家族运行时会计影响裁决）

> Audit Status: closed
> Mission: requirement-compliance
> Work Item: A4.2.6 + A4.2.7 + A4.2.8（合并：MA4 运行时行为验证 — A1.10 §7 SP-1/SP-2/SP-3 同根因[P1-RC-009 BOM 快照缺失]同控制点[BomExpander.loadLines 实时查 ErpMfgBomLine 无版本/快照门控]同 owner doc[manufacturing/]）
> Source: `docs/plans/2026-08-06-1926-2-rc-ma4-a4-2-6-7-8-mfg-bom-edit-impact-runtime.md`；存疑点来源 `docs/audits/2026-08-02-2231-1-rc-ma1-a1-10-mfg-f3-bom-routing.md` §7 SP-1/SP-2/SP-3 + §5 P1-RC-009 裁决
> Related: `docs/plans/2026-08-07-0400-3-rc-ma4-a4-2-ext-domain-expander.md`（A4.2 展开器 done）；`docs/audits/2026-08-02-2231-1-...-a1-10-...md`（A1.10 done，P1-RC-009 已登记）；`docs/audits/arm-index.md`（P1-RC-009 finding 行）
> Verdict: ⚠️(P1) — **维持 P1-RC-009 = P1**（不升 P0，不触发 MR0）。运行时确认：BOM 子件行编辑**不**默认破坏活跃会计路径——差异计算材料标准来自 FIRMED 卷算（冻结），完工 materialCost = Σ 领料单（不经 BOM 重展开），差异路径 config 默认 off；二次齐套 + 成本卷算读新 BOM 内容但前者是预执行校验非过账、后者产 CALCULATED 须人工 FIRM 方成标准。P1-RC-009 快照缺失仍是合规缺口（L1 要求快照；二次齐套/卷算读实时内容）→ 维持 P1 待 MR1。

---

## 0. 执行摘要

本验证是**只读运行时行为评估**（无代码/ORM/api.xml/真相源变更），结果表面 = 本报告 + arm-index `P1-RC-009` 运行时会计影响确认注记。范式对齐 A4.1.15（done — FIFO 到岸成本 delta 层消耗正确性运行时探针 + P0 升级候选裁决先例）。

**核心问题**（A1.10 §7 SP-1/SP-2/SP-3）：BOM 内容（子件行 `ErpMfgBomLine`）编辑后，已审核/已开工工单的运行时路径——差异计算/成本重算/二次齐套——是否**实际读新 BOM 内容**？若读 + 致成本结转凭证错误 → P1-RC-009 升 P0（活跃会计数据破坏）；若不读（或不致凭证错误）→ 维持 P1。

**裁决结论（先行）**：**维持 P1-RC-009 = P1**。三路径实测分述：

| 运行时路径 | 是否经 BomExpander.explode 读 BOM 子件行 | 受 BOM 子件行编辑影响 | 是否致活跃会计凭证错误 | 证据 |
|---|---|---|---|---|
| 差异计算（材料标准） | **否** — 读 FIRMED `ErpMfgCostRollupLine`（冻结标准） | 否 | 否 | `ProductionVarianceCalculator.java:114,129` |
| 差异计算（人工标准/工序） | 否（读 `ErpMfgBomOperation` 工艺路线，非子件行） | 仅工艺路线编辑影响；子件行编辑不影响 | 否（config 默认 off） | `ProductionVarianceCalculator.java:123,140,147` |
| 成本重算（CostRollup） | 否（自有 `loadLines` 直接读 `ErpMfgBomLine`） | **是**（读实时子件行） | 否（产 CALCULATED，须人工 FIRM 方成差异标准） | `CostRollupService.java:162,317-322` |
| 二次齐套（KitAvailabilityChecker） | **是**（`bomExpander.explode(bomId,...)`） | **是**（读实时子件行） | 否（预执行物料需求校验，非会计过账） | `KitAvailabilityChecker.java:66,133-143` |
| 完工 materialCost | 否（Σ 领料单成本聚合） | 否 | 否 | `ErpMfgMaterialIssueConfirmProcessor.java:121` |

净结论：BOM 子件行编辑**不默认破坏活跃会计路径**——材料差异标准冻结于 FIRMED 卷算 + 完工成本不经 BOM + 差异路径 config 默认 off。但二次齐套与成本卷算**确实读实时 BOM 内容**（快照缺失），故 P1-RC-009 仍是合规缺口（L1 UC-MFG-10 要求快照），维持 P1 待 MR1。

**config 默认值核验**：`erp-mfg.variance-auto-calc-enabled` = **false**（三源一致 + 无部署 override）→ 差异计算路径**非默认活跃**。

**P0 升级裁决**：命中决策分支 ②（差异/完工路径不经 BOM 重展开）+ ③（差异路径 BOM 读取相邻[工艺路线]但 config 默认 off）→ **维持 P1-RC-009，不触发 MR0**。与 A1.10 §5.3 P0 即时通道结论一致（功能缺失类非活跃数据破坏类）。

---

## 1. BomExpander.explode 调用方全集 census（SP-1 前置）

`grep -n "BomExpander" module-manufacturing`（main 源码）全集——5 个类注入 `BomExpander`：

| # | 调用方类（file:line） | 调用方法 | 是否传 bomId | 是否经 `loadLines` 实时查 `ErpMfgBomLine` | 用途 |
|---|---|---|---|---|---|
| 1 | `KitAvailabilityChecker` (`workorder/KitAvailabilityChecker.java:46,66`) | `bomExpander.explode(bomId, plannedQty, true)` | 是（`resolveBomId(wo)`，wo.bomId 优先 `:133-135`） | 是 | 齐套校验（多级展开 × plannedQuantity 对照库存可用量） |
| 2 | `MrpEngine` (`mrp/MrpEngine.java:62,68`) | `bomExpander.explode(...)` | 是（`bom.getId()`，单级） | 是 | MRP 净需求展开 |
| 3 | `SimulationMrpEngine` (`simulation/SimulationMrpEngine.java:60,70`) | `bomExpander.explode(...)` | 是 | 是 | MRP 仿真 |
| 4 | `CostRollupService` (`costing/CostRollupService.java:72,78`) | **仅** `bomExpander.findDefaultBomOrNull(materialId)`（`:144`）—— **不调 explode** | —（仅查默认 BOM 头） | 否（BOM 子件行用**自有** `loadLines` `:162,317-322` 直接读 `ErpMfgBomLine`） | 成本卷算 |
| 5 | `ErpMfgBomBizModel` (`entity/ErpMfgBomBizModel.java:37,45`) | 委托 `explode`/`findDefaultBom` 给 BomExpander（`:45` 只读 GraphQL 入口） | 是 | 是 | BOM 实体服务只读展开入口 |

**关键缺失项（差异计算路径）**：`ProductionVarianceCalculator`（`costing/ProductionVarianceCalculator.java`）**完全不注入/不使用 `BomExpander`**（imports `:1-37` 无 `BomExpander`，`grep BomExpander` 跨该类零命中）。差异计算的材料标准侧来自 `findFirmedRollupLine(productId)`（`:114`）读 FIRMED `ErpMfgCostRollupLine`，**不经 BOM 重展开**。

**BomExpander.loadLines 无版本/快照门控确认**：`BomExpander.java:144-149` `loadLines(bomId)` = `QueryBean` 实时查 `ErpMfgBomLine`（`eq("bomId", bomId)`），无版本/快照/时间点过滤。**确认 P1-RC-009 控制点成立**：任何经 `explode` 的路径（齐套/MRP/仿真）读实时 `ErpMfgBomLine` 内容。

---

## 2. SP-1 差异/重算/齐套路径 BOM 重展开核验

### 2.1 SP-1(a) 差异计算路径 BOM 重展开核验 — **否定（材料标准不经 BOM 重展开）**

**对象**：`ProductionVarianceCalculator.calculateVariances(workOrderId)`（`costing/ProductionVarianceCalculator.java:106-216`）。

**材料差异标准侧**（`:128-137`）：
```java
ErpMfgCostRollupLine stdLine = findFirmedRollupLine(productId);  // :114 — FIRMED 卷算行（冻结标准）
BigDecimal stdMaterial = nz(stdLine.getMaterialCost()).multiply(completed);  // :129 — 标准材料 = FIRMED 单位材料成本 × 完工量
BigDecimal actMaterial = nz(wo.getMaterialCost());                // :130 — 实际材料 = 工单累加 materialCost
```
- 标准材料来自 **FIRMED `ErpMfgCostRollupLine`**（`findFirmedRollupLine:114`，取最近 `status=FIRMED` 的卷算行 `:342-367`），**不读 `ErpMfgBomLine`、不经 `BomExpander.explode`**。
- 故 BOM 子件行（`ErpMfgBomLine`）编辑**不影响**材料差异标准 → PRODUCTION_VARIANCE 凭证材料侧金额不受影响。

**人工差异标准侧**（`:139-167`）：读 `ErpMfgBomOperation`（**工艺路线**，非子件行）：
- `sumBomOperationStandardMins(bomId)`（`:140,369-380`）— BOM 工序标准工时。
- `deriveStandardLaborRate(bomId, ...)`（`:147,397-422`）— BOM 工序工作中心费率均值。
- `resolvePrimaryWorkcenterId(bomId)`（`:123,424-437`）— 首工序工作中心。
- 这些读 `ErpMfgBomOperation`（按 `bomId` 实时），**不是 `ErpMfgBomLine`**。SP-1 触发条件明确为「BOM 子件行编辑」（A1.10 §7 SP-1：增/删/改物料或数量），工艺路线编辑不在 SP-1 范围。
- **结论**：差异计算**不经 BOM 子件行重展开**读标准用量。材料标准冻结于 FIRMED 卷算。

### 2.2 SP-1(b) 成本重算路径 BOM 读取核验 — **是（读实时 BOM），但产 CALCULATED 非差异标准**

**对象**：`CostRollupService.rollup(bomId)`（`costing/CostRollupService.java:85-95`）+ `computeUnit`（`:130-180`）。

- `computeUnit` 对制造件用**自有** `loadLines(bom.getId())`（`:162,317-322`）实时读 `ErpMfgBomLine` 算材料成本 + `sumOperationCost(bom.getId())`（`:167,182-203`）读 `ErpMfgBomOperation` 算人工。
- 亦用 `bomExpander.findDefaultBomOrNull(materialId)`（`:144`）定位默认 BOM，但**不调 explode**。
- **产出的 `ErpMfgCostRollup` 状态 = `COST_ROLLUP_STATUS_CALCULATED`**（`:103`），`FIRMED` 由人工动作置位（Non-Goal，`:59` 注释明确）。
- **结论**：成本重算**确实读实时 BOM 子件行**（BOM 编辑后卷算会算出新标准），但产出是 CALCULATED，**须人工 FIRM 后方成为差异计算的标准源**（`findFirmedRollupLine` 仅取 FIRMED `:347-348`）。故 BOM 编辑 → 卷算 → 默认不自动 FIRM → 不自动改变差异标准 → 不致活跃凭证错误。

### 2.3 SP-1(c) 二次齐套 BOM 编辑后读新内容核验 — **是（读实时 BOM）**

**对象**：`KitAvailabilityChecker.check(workOrderId)`（`workorder/KitAvailabilityChecker.java:62-89`）。

- `resolveBomId(wo)`（`:64,133-143`）：`wo.getBomId()` 优先（`:134-135`，工单创建时锁定的 bomId 引用），缺失回落默认 BOM（`:137`）。
- `bomExpander.explode(bomId, plannedQty, true)`（`:66`）→ BomExpander.loadLines 实时查 `ErpMfgBomLine`。
- **结论**：STOCK_PARTIAL 强制开工后补料二次齐套（再次调 `check`）**会读 BOM 编辑后的新内容**。此为预执行物料需求校验（对照库存可用量），**非会计过账路径**——不直接产 GL 凭证，但会致齐套判断基于编辑后 BOM（物料需求正确性偏差，归 P1 合规缺口，非 P0 凭证错误）。

### 2.4 MA4 ↔ A5.6 边界声明

本验证审「行为是否符合需求」（BOM 编辑是否致成本凭证错误 / 是否按新 BOM 重算物料需求），与 A5.6 审「E2E 断言强度」边界按此执行。本验证**不重做** A5.6 E2E 断言强度审计（A5.6 done，`2026-08-06-1926-rc-ma4-dashboard-...md` 范式）。本验证证据为 main 源码 grep + 路径推理（只读），非 E2E 注入重现。

---

## 3. SP-2 GL 凭证影响裁决

**前置**：SP-1 确认差异计算材料标准来自 FIRMED 卷算（冻结）、完工 materialCost = Σ 领料单（不经 BOM）。故 SP-2 按决策树**否定分支**执行（确认完工过账默认不受影响）。

### 3.1 完工 materialCost 计算路径 — 不经 BOM 重展开

`ErpMfgMaterialIssueConfirmProcessor.java:121`：
```java
wo.setMaterialCost(nz(wo.getMaterialCost()).add(materialCostDelta));  // 领料确认累加领料单成本
```
- 完工 materialCost = Σ 领料单（material issue confirm）成本聚合，**不经 BOM 重展开**（A1.10 §5 + A4.2a §2.2 已静态确认，本次 file:line 复核 `:121` 锚定）。
- **结论**：完工过账（成本结转凭证 WIP → 产成品）默认**不受** BOM 子件行编辑影响。

### 3.2 PRODUCTION_VARIANCE 凭证 — 材料侧冻结，人工侧 config-gated

`ProductionVarianceDispatcher.dispatchIfApplicable(workOrderId)`（`posting/ProductionVarianceDispatcher.java:70-100`）：
- 按成本要素汇总 `ErpMfgCostVariance` 行的 `varianceAmount`（`:86-97`）→ 组装 `PostingEvent` 调 `MfgPostingExecutor` 过账。
- 凭证金额源自差异行，差异行金额 = `actualAmount − standardAmount`（`ProductionVarianceCalculator.buildLine:302-327`）。
- **材料标准侧** = FIRMED 卷算（冻结，§2.1）→ 材料差异凭证金额**不受** BOM 子件行编辑影响。
- **人工标准侧** = 读 `ErpMfgBomOperation`（工艺路线，非子件行）+ config `erp-mfg.variance-auto-calc-enabled` 默认 **off**（`ErpMfgWorkOrderProcessor.java:397`）→ 差异计算路径**非默认活跃**。
- **结论**：BOM 子件行编辑**不默认致** PRODUCTION_VARIANCE 凭证 + 成本结转凭证行级金额偏离审核时 BOM 内容。

### 3.3 GL 凭证裁决

- **默认配置下（config=false）**：完工过账 materialCost 不经 BOM + 差异路径不活跃 → **无活跃 GL 凭证错误**。
- **config-enable 时（variance-auto-calc-enabled=true）**：差异计算激活，但材料标准仍来自 FIRMED 卷算（冻结），BOM 子件行编辑仍不影响材料差异凭证。人工差异读工艺路线（非子件行）。故即便 config on，BOM **子件行**编辑仍不致材料凭证错误。
- **结论**：SP-2 否定——BOM 子件行编辑**不致**成本结转/差异凭证行级金额错误（材料标准冻结 + 完工不经 BOM）。

---

## 4. SP-3 bomId 弱隔离运营实践 + BOM 版本化核验

### 4.1 bomId 弱隔离机制

`KitAvailabilityChecker.resolveBomId`（`:133-143`）：`wo.getBomId()` 优先 → 工单创建时锁定 bomId 引用。
- **新建 BOM（新 bomId）**：不影响已建工单（工单 bomId 仍指向旧 bomId）。
- **编辑同 bomId 内容**（增/删/改 `ErpMfgBomLine`）：无隔离——二次齐套（§2.3）+ 成本卷算（§2.2）读编辑后内容。

### 4.2 BOM 版本化实践核验 — **无内容版本/快照机制**

`ErpMfgBomLine` ORM（`module-manufacturing/model/app-erp-manufacturing.orm.xml:233-255`）字段普查：
- `version`（propId 14，`domain="version"`，`:250`）= **乐观锁数据版本**（JPA 式 @Version），非 BOM 内容版本/修订号。
- `delVersion`（propId 13，`domain="delVersion"`，`:249`）= 逻辑删除版本。
- **无** `snapshotBomVersion` / `bomRevision` / `effectiveDate` / 内容快照列。`ErpMfgWorkOrder` 仅 `bomId`（可空弱引用，A1.10 §5 已确认无 `snapshotBomVersion` 列）。

SQL DDL 三方言交叉确认（`deploy/sql/{mysql,oracle,postgresql}/_create_erp-mfg.sql:478-497` + COMMENT `:1496-1498`）：`VERSION` 注释「数据版本」= 乐观锁；无内容版本列。

**seed/部署文档普查**：`grep "erp_mfg_bom_line" *.sql` 仅命中 DDL/索引/tenant（`deploy/sql/`），无 BOM 版本化实践数据；无 BOM 变更操作指南文档。

**结论**：无 BOM 版本化/快照实践；bomId 弱隔离（工单 bomId 锁定）是唯一保护。运营「编辑同 bomId」则二次齐套/卷算读新内容（SP-1 已确认）。

---

## 5. config `erp-mfg.variance-auto-calc-enabled` 默认值核验

三源一致确认默认 **false**：

| 源 | 位置 | 值 |
|---|---|---|
| 常量声明 + 注释 | `ErpMfgConstants.java:171-173`（`CONFIG_VARIANCE_AUTO_CALC_ENABLED`，注释「默认 false=完工不自动触发差异计算」） | false |
| 消费点 | `ErpMfgWorkOrderProcessor.java:396-397`（`isVarianceAutoCalcEnabled() → readBoolConfig(..., false)`） | false |
| 模块元数据 | `erp-mfg-meta/precompile/module-meta.yaml:41-43` + `_module-meta.json:76`（`defaultValue: false`） | false |
| 部署 override | `grep "variance-auto-calc-enabled" *.yaml/*.properties` 全仓 → **零命中**（无 application.yaml override） | — |

**结论**：差异计算路径**非默认活跃**（config 默认 off）。即使 config on，材料标准仍冻结于 FIRMED 卷算（§2.1），BOM 子件行编辑仍不影响材料差异凭证。

---

## 6. P1-RC-009 P0 升级裁决（方法论 §2 判据 + 三源对照）

**决策框架**（plan §Phase 2 Decision）三分支：

| 分支 | 条件 | 裁决 |
|---|---|---|
| ① | 差异/重算经 BOM 重展开读新内容 + config 默认 on | → P0 |
| ② | 差异/重算不经 BOM 重展开（materialCost = Σ 领料单不经 BOM） | → 维持 P1 |
| ③ | 差异经 BOM 重展开但 config 默认 off | → 维持 P1 |

**本验证命中**：**② + ③**（混合，均指向维持 P1）：
- ② 命中：差异计算材料标准来自 FIRMED 卷算（§2.1，冻结，不经 BOM 子件行重展开）+ 完工 materialCost = Σ 领料单（§3.1，不经 BOM）→ **不满足** P0①「活跃数据破坏」/ P0④「会计过账正确性破坏」。
- ③ 命中：差异计算人工标准读 `ErpMfgBomOperation`（工艺路线，BOM 读取相邻）但 config 默认 off（§5）→ 非默认活跃路径。

**§2 判据对照**：
- **§2 P0①（活跃数据破坏防护未实现）**：**不成立** — 完工过账 materialCost 不经 BOM（§3.1），活跃完工过账路径不受 BOM 编辑破坏。
- **§2 P0④（会计过账正确性破坏）**：**不成立** — PRODUCTION_VARIANCE 凭证材料侧冻结于 FIRMED 卷算（§3.2），BOM 子件行编辑不致凭证金额错误；差异路径 config 默认 off。
- **§2 P1①（功能完全缺失 / 行为实质偏离）**：**成立** — BOM 快照原则完全缺失（A1.10 §5 已裁决，④审核时快照未实现 + ⑤同 bomId 内容编辑无隔离）；二次齐套 + 卷算读实时内容（§2.2/§2.3）证实内容级隔离缺失。
- **§5 Q4（会计/成本正确性类无例外，禁方案 B）**：维持 — 快照缺失仍须实现（MR1），但**运行时会计影响**经本验证确认不达 P0 阈值（无活跃凭证错误）。

**L1/L2/L3 三源对照**：
- **L1**（`docs/design/manufacturing/use-cases.md` UC-MFG-10）：要求 BOM 快照（审核时锁定内容，后续编辑不影响已审核工单物料需求/成本）。**L1 未满足**（快照未实现）→ P1 成立。
- **L2**（`bom-and-routing.md`）：§实现注记 Non-Goal「BOM 版本快照」为 AI 落地补注，A1.10 §5 三判据核验**不构成**显式人工批准 documented simplification → 不豁免。
- **L3**（实测）：BomExpander.loadLines 无快照门控（§1）+ 二次齐套/卷算读实时内容（§2.2/§2.3）+ 差异材料标准冻结 FIRMED（§2.1）+ config 默认 off（§5）→ 运行时无活跃凭证错误。

**与 A1.10 §5/§5.3 分层一致**：
- A1.10 §5 裁决 P1-RC-009 = P1（功能完全缺失类 §2 P1①，非活跃数据破坏类 §2 P0④），§5.3 明示「完工 materialCost 不经 BOM 重展开故默认完工过账不受影响，会计影响需运行时确认 SP-1/SP-2」。
- 本验证**闭合** SP-1/SP-2 运行时确认：**维持** A1.10 §5 的 P1 定级（不升 P0）。A1.10 §5 的 P0 即时通道结论「未触发 MR0」**维持**。

**裁决**：**维持 P1-RC-009 = P1，不触发 MR0**。快照缺失仍是合规缺口（L1 要求 + 二次齐套/卷算读实时内容），归 MR1 R1.0→RC-R1.n 修复（触及 ORM 结构[ErpMfgBomLine/ErpMfgWorkOrder 快照字段]须 ask-first + 独立 plan-audit，A1.10 §6.1 已登记）。

**config-enable 运营注意**（登记）：`erp-mfg.variance-auto-calc-enabled=true` 时差异计算激活，但材料标准仍冻结 FIRMED（§2.1），BOM 子件行编辑仍不影响材料凭证；人工差异读工艺路线（非子件行）。运营启用差异自动计算前应先 FIRM 一份基于审核时 BOM 的卷算作为标准（当前 FIRM 为人工动作），避免后续 BOM 编辑经卷算→FIRM→差异链路间接影响。

---

## 7. finding/注记衔接

- **P1-RC-009 维持 P1**（本验证结论）：arm-index `P1-RC-009` 行追加运行时会计影响确认注记（**不影响凭证** — 材料标准冻结 FIRMED + 完工不经 BOM + config 默认 off；二次齐套/卷算读实时内容归 P1 合规缺口待 MR1）。
- **不触发 MR0**（维持 A1.10 §5.3 结论），无 R0.n 实体行追加。
- **MR1 修复方向**（本验证指导，不实施）：
  - 修复触及 BOM 快照机制[ErpMfgBomLine 内容快照 + ErpMfgWorkOrder.snapshotBomVersion 列 + 审核 DRAFT→SUBMITTED 时复制 BOM 内容 + BomExpander.loadLines 读快照而非实时]属 **ORM 结构变更须 ask-first + 独立 plan-audit §5**。
  - 修复后须保证：二次齐套/卷算/差异读快照（审核时锁定）而非实时 `ErpMfgBomLine`。
- **与既有 finding 交叉去重声明**：
  - **P1-RC-009**（arm-index）：本验证是其运行时会计影响裁决的输入，**不新建** finding，仅追加注记。
  - **A1.10 §5/§7**（源报告）：本验证闭合其 SP-1/SP-2/SP-3，结论与 §5 P1 裁决分层一致（不撤销/不升级）。
  - **A4.2a**（MRP 代码质量审计）：复用其「完工 materialCost 不经 BOM 重展开」结论（§2.2），本验证 file:line 锚定 `ErpMfgMaterialIssueConfirmProcessor:121`。
  - **MA4 ↔ A5.6 边界**：本验证审行为符合性，不重做 A5.6 E2E 断言强度（§2.4）。
  - **P1-MA4-007**（完工编排层差异吞咽致业财悬挂，A4.2a）：resolved R1.16，与本切片 P1-RC-009 互补不重叠（差异过账失败告警 vs BOM 快照缺失不同控制点，A1.10 §6.1 已声明）。

---

## 8. 过程纪律自检

### 8.1 nop-compliance-checker.sh 实测

运行 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，本计划**无生产代码变更**故**无回归风险**）：

| 规则 | actual（本次） | 说明 |
|---|---|---|
| R0 类（NopException 原生异常等） | 命中 0/0/0（前三规则） | 基线既有，非本计划引入 |
| R1-Rn 类（daoFor 直接构造 / new Erp*() 等） | 命中 14/34/229/34 等 | 基线既有，非本计划引入 |

**说明**：本计划是只读评估，**零生产代码变更**（无 .java/.orm.xml/.api.xml/view.xml 修改），checker 命中均为基线既有项，**无本计划引入的漂移**。按 plan §Closure Gates，**不以 checker 退出码 0 作为门控依据**（checker 是 reporter，本计划无代码变更故无回归风险）。

### 8.2 独立性声明

- 本报告执行者 = 计划执行代理（非草案审查者）。计划草案审查已由独立子代理 ses_029279a7affe2LeMkP6536Z3m7 完成（fresh session，Draft Review Record iter-1 accept，零 Blocker）。
- **结束审计**将由独立子代理（新会话）执行（plan §Closure Gates 最后一项），执行者**不自我审计**。

### 8.3 §8 自检清单

| # | 项 | 状态 | 证据 |
|---|---|---|---|
| 1 | 需求正确性（对照 L1 UC-MFG-10） | ✅ | §6 三源对照，L1 要求快照未满足→P1 成立 |
| 2 | owner-doc 对齐（manufacturing/） | ✅ | §1-§5 引用 bom-and-routing.md / variance-analysis.md / state-machine.md |
| 3 | 架构/边界影响 | ✅ | 只读评估，无跨模块依赖变更；MA4↔A5.6 边界声明 §2.4 |
| 4 | 验证充分性 | ✅ | 调用方 census 全集（5 类）+ 三路径 file:line 证据 + config 三源 + ORM 三方言 DDL |
| 5 | 回归风险 | ✅ | 零代码变更，无回归 |
| 6 | 路由/技能选择 | ✅ | Skill: multi-dimensional-audit-prompt.md（plan 指定），维度覆盖齐全 |
| 7 | 范围漂移 | ✅ | 不重审 P1 定级本身（仅评升 P0）；不实施修复；不展开 A1.10 其他 SP |
| 8 | 与 arm-index 交叉去重 | ✅ | §7 声明，P1-RC-009 追加注记非新建 |

---

## 9. 结论

**整体 Verdict**：⚠️(P1) — **维持 P1-RC-009 = P1**（不升 P0，不触发 MR0）。

- **A4.2.6（SP-1）**：差异计算材料标准来自 FIRMED 卷算（冻结，不经 BOM 子件行重展开）；成本卷算读实时 BOM 但产 CALCULATED（须人工 FIRM）；二次齐套读实时 BOM（预执行校验非过账）。**不默认破坏活跃会计路径**。
- **A4.2.7（SP-2）**：完工 materialCost = Σ 领料单（不经 BOM）+ PRODUCTION_VARIANCE 材料侧冻结 FIRMED + 差异 config 默认 off → **不致**成本结转/差异凭证行级金额错误。
- **A4.2.8（SP-3）**：无 BOM 版本化/快照机制（`ErpMfgBomLine.version`=乐观锁非内容版本）；bomId 弱隔离是唯一保护；编辑同 bomId 则二次齐套/卷算读新内容。

P1-RC-009 仍是合规缺口（L1 UC-MFG-10 要求快照 + 二次齐套/卷算读实时内容），归 MR1 修复（触及 ORM 结构须 ask-first + 独立 plan-audit）。本验证**闭合** A1.10 §7 SP-1/SP-2/SP-3 运行时会计影响裁决，与 A1.10 §5 P1 定级 + §5.3「不触发 MR0」结论分层一致。
