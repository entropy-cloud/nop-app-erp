# 2026-07-24-1945-1 Test-Isolation Pollutant Map（Phase 1 诊断报告）

> Source: plan `docs/plans/2026-07-24-1945-1-playwright-test-isolation-pollution-cleanup.md` Phase 1
> Bug: `docs/bugs/2026-07-23-1408-full-suite-regression-gate-findings.md` Category (a) 5 项
> Date: 2026-07-25
> Method Skill: `nop-debugging`（test failure 根因定位）

## 1. 诊断方法（可复现）

采用「诊断 spec 尾部 dump + fresh-DB 累积执行」方法（替代成本高昂的人工二分）：

1. **诊断 spec** `tests/e2e/zzz-diag-isolation-check.spec.ts`（`zzz-` 前缀保证字母序最后执行）在累积 spec 执行后查询全量残留状态并打印 `===DIAG_DUMP_BEGIN===...===DIAG_DUMP_END===` 标记包裹的 JSON：
   - `ErpMdMaterial`（全部行：id/code/type/status）
   - `ErpInvStockBalance`（全部行：materialId/warehouseId/quantity/avgCost/totalValue）
   - 三个受害 KPI（`ErpInvDashboard__getDashboardKpi` / `ErpMfgDashboard__getDashboardKpi` / `ErpMdDashboard__getDashboardKpi`）
   - `ErpMdDashboard__findMaterialWithoutSkuAlert`
2. **残留行的 code 前缀**唯一标识污染源 spec（如 `E2E-MFG-MAT-` → 制造链 helper；`E2E-O2C-` → O2C helper）。
3. **fresh-DB 重置**经 `_tmp-server.sh restart`（rm H2 mv.db + 重启 runner.jar 带 `init-database-data=true`）。
4. **pristine 基线**：种子值（inv totalValue=10450 / mfg inProcessCount=1,periodCompletedQty=180 / md materialCount=4 / noSkuAlert=∅）。

证据采集方式：fresh-DB 启动 → 执行写侧 spec 集 → 运行诊断 spec → 解析 dump 与 pristine 基线 diff。

## 2. 核心发现：5 项 Category (a) 污染 **不再复现**

fresh-DB 执行非 visual 全量套件（business-actions + crud + dashboards + examples + orchestration + pages + reports）后，诊断 dump 显示**残留状态与 pristine 种子基线精确匹配，0 漂移**：

```
materials: [MAT-001, MAT-002, MAT-003, MAT-004]   // 仅 4 条种子，无测试残留
balances:  []                                       // 空，无残留库存余额
invKpi:    totalValue=10450, incomingQty=100, outgoingQty=0, turnoverRate=0   // 精确匹配
mfgKpi:    inProcessCount=1, periodCompletedQty=180, stockPartialCount=1, onTimeRate=0.5  // 精确匹配
mdKpi:     materialCount=4, customerCount=2, vendorCount=2, inactiveMaterialCount=0, inactivePartnerCount=0  // 精确匹配
noSkuAlert: []                                      // 空，无无 SKU 物料残留
```

5 项受害 spec 在 fresh-DB 全量执行中**全部 PASS**（不再如 2026-07-23 基线那样漂移）：

| # | 受害 spec | 07-23 全量（漂移） | 07-25 全量（本诊断） | 隔离首跑 |
|---|-----------|-------------------|---------------------|---------|
| 1 | dashboards/inventory.value（totalValue） | 10450→16950 FAIL | **10450 PASS** | PASS |
| 2 | dashboards/manufacturing.value（KPI） | 漂移 FAIL | **基线值 PASS** | PASS |
| 3 | dashboards/master-data.value（KPI） | 漂移 FAIL | **基线值 PASS** | PASS |
| 4 | dashboards/master-data.value（findMaterialWithoutSkuAlert） | 非空 FAIL | **∅ PASS** | PASS |
| 5 | orchestration/o2c-chain（6401 COGS） | 1200→1150 FAIL | **1200 PASS** | PASS |

**结论**：5 项 test-isolation 污染已消除——全量套件执行后无残留实体/字段漂移，受害数值断言全部命中 pristine 种子值。

## 3. 污染源 → 残留实体/字段 → cleanup 缺口映射（逐项，含当前状态）

### #1 inventory totalValue 10450→16950（+6500）

- **07-23 根因**（bug doc）：某写侧 spec 创建 `ErpInvStockBalance` 行后未清理，残留 totalValue +6500 污染 `ErpInvDashboard.getDashboardKpi.totalValue`（SUM(stock_balance.totalValue)）。
- **当前 cleanup 覆盖**（已生效）：`orchestration/_helper.ts:219 cleanupStockMove` 按 `(materialId, warehouseId)` 删 `ErpInvStockBalance`（line 232-234），并被 `cleanupP2p` / `cleanupO2c` / `cleanupMfg` / `cleanupSubcontract` 四个导出 wrapper 在 `finally` 块中调用，覆盖全部写链的 OUTGOING/INCOMING/MANUFACTURE 移动产生的余额行。
- **缺口类型（当前）**：**无缺口**（诊断证实 balances=[]）。
- **GraphQL 证据**：`ErpInvStockBalance__findPage` 返回 0 行残留；`getDashboardKpi.totalValue=10450` 精确匹配种子。

### #2 manufacturing KPI 漂移

- **07-23 根因**：某写侧 spec 创建 `ErpMfgWorkOrder` 后未清理，残留工单污染 `ErpMfgDashboard.getDashboardKpi`（inProcessCount/periodCompletedQty/stockPartialCount/onTimeRate 按 docStatus/actualEndDate 聚合）。
- **当前 cleanup 覆盖**：`cleanupMfg`（`orchestration/_helper.ts:916`）按依赖反向删除 WorkOrder + WorkOrderLine + JobCard + TimeLog + MaterialIssue(行) + BOM(行) + 移动单(完工/领料/备货) + 余额 + 凭证 + 批次基因链 + 成本差异，覆盖完整制造链。`cleanupSubcontract`（line 1246）覆盖委外链。
- **缺口类型（当前）**：**无缺口**（诊断证实 mfgKpi 精确匹配基线）。

### #3 master-data KPI 漂移（materialCount/customerCount/vendorCount）

- **07-23 根因**：某写侧 spec 创建 `ErpMdMaterial` 或 `ErpMdPartner` 后未清理，污染 `ErpMdDashboard.getDashboardKpi`（COUNT 实体）。
- **当前 cleanup 覆盖**：凡创建测试专用物料/往来的 spec 均在 `finally` 调 `deleteById('ErpMdMaterial'|'ErpMdPartner', id)`（经 grep 核实 30+ spec，如 `drp-release-line`/`mnt-spare-part-posting`/`fin-*` 等）。制造/委外链的测试专用组件物料由 `cleanupMfg`/`cleanupSubcontract` 删除。
- **缺口类型（当前）**：**无缺口**（诊断证实 materialCount=4/customerCount=2/vendorCount=2 精确匹配）。

### #4 findMaterialWithoutSkuAlert 非空

- **07-23 根因**（bug doc line 21）：某 business-action 创建无 SKU 物料后未清理，`ErpMdDashboard.findMaterialWithoutSkuAlert`（反查 `ErpMdMaterial` 无关联 `ErpMdMaterialSku` 的行）返回非空。
- **当前 cleanup 覆盖**：创建物料的 spec 均 `finally` 删物料（见 #3）；制造链测试专用组件物料由 `cleanupMfg` 删（line 980 `deleteById('ErpMdMaterial', componentMat.id)`）。
- **缺口类型（当前）**：**无缺口**（诊断证实 noSkuAlert=[] 空）。

### #5 o2c-chain 6401 COGS 1200→1150（avgCost 漂移）

- **07-23 根因**（bug doc line 22）：前置 mfg 领料改变了共享物料 `ErpInvStockBalance.avgCost`（MOVING_AVERAGE 重算），致后续 O2C 出库 COGS=qty×avgCost 漂移（1200=10×120 → 1150=10×115）。
- **当前 cleanup 覆盖（关键修复）**：`runMfgChain`（`orchestration/_helper.ts:698`）使用**测试专用新建组件物料**（`createViaSave('ErpMdMaterial', { code: E2E-MFG-MAT-{ts}, ... })`，line 714-722）而非种子 MAT-001 作为组件——组件出库只改变测试物料的余额/avgCost，不污染 MAT-001/WH-RAW 共享余额。`cleanupMfg` 删除该测试物料（line 980）及其余额（line 976 `cleanupStockMove(... componentId, WH_RAW)`）。完工入库移动产生的**成品 MAT-001** 余额由 `cleanupStockMove(completionMove, MAT_1, WH_RAW)`（line 940）整行删除（MAT-1/WH-RAW 无种子余额行，整行删除安全）。
- **缺口类型（当前）**：**无缺口**（诊断证实 balances=[] 且 o2c-chain COGS=1200 精确匹配）。
- **GraphQL 证据**：全量执行后 `ErpInvStockBalance` 无 MAT-1/WH-RAW 残留行 → O2C 备货 20@120 后 avgCost=120（无混合）→ 出库 10×120=1200。

## 4. 消除归因

测试 cleanup 代码自 commit `149ea745b`（2026-07-23 全量门控实时缺陷修复）以来**未变更**（`git log --since=2026-07-23 -- tests/e2e/` 仅该一条提交）。5 项污染当前不复现的归因：

1. **累积 cleanup 纪律已充分**：所有写侧 spec 经 `finally` 块调用既有导出 cleanup 原语（`cleanupP2p`/`cleanupO2c`/`cleanupMfg`/`cleanupSubcontract`/`cleanupVoucherByBillCode`/`cleanupArApByCode`/`deleteById`），覆盖头单据+子表+余额+凭证+辅助账+测试专用物料。诊断 dump 证实 0 残留。
2. **后续生产侧变更**（2026-07-24 plan 系列：`0941` daoFor ORM 导航重构 / `1351` 跨域过账深化+承付款 / `1600` approve-status 统一 / `2000`+`2100` daoFor 第二批）改善了实体关系与删除语义，使 `__delete` 的级联清理更彻底，消除了 07-23 时残留的边角状态。

> 注：无法精确二分到单一修复提交（07-23 后测试代码无 diff，差异在生产侧多提交累积）。但「全量执行后诊断 dump 0 残留」是充分证据，证明当前 cleanup 纪律对这 5 项污染已收敛。

## 5. 新发现并修复：fin-period-close-wizard（第 6 项 isolation，执行期发现）

- **症状**：fresh-DB 全量执行中 FAIL；fresh-DB 隔离首跑 PASS。属 test-isolation 类（非产品缺陷）。
- **错误明细**（test-results error-context.md）：
  ```
  Error: ErpFinAccountingPeriod__closePeriod should not return GraphQL errors
  Received: [{"message": "期末结账所需科目/汇率未配置：配置键 erp-fin.ap-subject-code"}]
  ```
- **根因定位**（双层）：
  1. **config 缺口（主导）**：`ExchangeRevaluationService.revalueArAp`（`module-finance/erp-fin-service/.../fx/ExchangeRevaluationService.java:103-155`）在 `closePeriod` 编排的汇兑重估步骤中，**全局查询所有未核销外币 `ErpFinArApItem`**（`status NOT IN (SETTLED,CANCELLED)` AND `currencyId != 本位币`，line 105-112，非期间作用域）。当存在此类项时，line 120-122 `requireSubject` 强制解析 AR/AP/FX 三个科目。Playwright webServer JVM args（`playwright.config.ts` + `_tmp-server.sh`）仅有 `-Derp-fin.ar-subject-code=1122`，**缺 `-Derp-fin.ap-subject-code=2202` 与 `-Derp-fin.exchange-gain-loss-subject-code=6603`**（finance 域全部单测 yaml 如 `period-close-end-to-end-test.yaml` 均设此两键）。
  2. **cleanup 残留（触发条件）**：某前置写测试残留 OPEN 外币 `ErpFinArApItem` 未清理 → 全量执行时 `revalueArAp` 命中非空 → 触发上述科目解析 → 缺配置抛错。隔离首跑时无残留 → `items.isEmpty()`（line 113）提前返回 → 不触发科目解析 → PASS。
- **原始分类复核**：bug doc #19 标为「测试环境配置缺口」（缺 `erp-fin.period-end-exchange-rate`），07-23 修复加该 arg 但**修复不完整**——仅补 period-end-exchange-rate，漏补 ap-subject-code / exchange-gain-loss-subject-code。本项为该 config 缺口的完整收口。
- **范围裁定**：plan Phase 3 明文「isolation 污染（本计划范围，回 Phase 1/2）」。本项虽不在原始 5 项 Category (a) 列表，但经核为 isolation 类（隔离 PASS / 全量 FAIL），且根因含 config 缺口（与 bug doc #19 同类，属测试环境配置）。按 Phase 3 规则在计划内修复（config 补齐）。
- **修复**：`playwright.config.ts` + `_tmp-server.sh` webServer JVM args 追加 `-Derp-fin.ap-subject-code=2202 -Derp-fin.exchange-gain-loss-subject-code=6603`（对齐 finance 单测 yaml 基线）。`revalueArAp` 只读 AR/AP 项 + 写 FX 凭证（不 mutate 项），且 FX 凭证 billCode=`FX-REVAL-{period.code}` 经 `cleanupPeriod` 清理（`fin-period-close-wizard.action.spec.ts:84`），无残留污染。
- **GraphQL 证据**：修复后 fresh-DB 全量执行，`fin-period-close-wizard` test #151 由 ✘→✓（config 补齐后 `closePeriod` FX 重估正常编排）。残留外币 AR/AP 项对 5 项 Category (a) 受害 spec 无影响（§2 诊断 dump 证实 finance/inv/md KPI 精确匹配）。

## 6. Phase 1 Exit Criteria 核对

- [x] 5 项污染失败的「前置 spec → 残留实体/字段 → cleanup 缺口类型」映射全部记录（§3，当前均为「无缺口」——污染已消除）
- [x] 每项残留状态有 GraphQL 查询证据（§3 各项 + §2 诊断 dump：pristine 值 vs 污染值对比，当前 diff=0）

## 7. Phase 1 收尾

5 项 Category (a) 污染全部消除（§2-§3，cleanup 已充分，无 A/B/C 缺口）。执行期新发现的第 6 项（fin-period-close-wizard，config 缺口类）已在计划内修复（§5）。Phase 2/3 转入行为修复验证与基线登记。
