# 2026-09-27-0325-2 看板/报表读路径查询性能优化批

> Plan Status: completed
> Last Reviewed: 2026-09-28
> Source: `docs/analysis/2026-09-27-perf-ux-deep-optimization-analysis.md` §2（A1/A2/A3/A6/A7/A8/A9/B1/B2/J1-J3 + §6 A4/A5/D1 部分裁决）
> Related: `docs/plans/2026-09-27-0318-1-ux-row-action-p1-fixes.md`（同批独立计划，无依赖）
> Audit: required

## Current Baseline

- 4 个核心域看板存在**全量物化到内存**的读路径站点（2026-09-27 HEAD 实核）：
  - `ErpSalDashboardBizModel.java`：KPI 发票求和 `:74` / Trend 12 个月全量 `:105` / loader `:219-226`；同文件 `findCustomerTopN:132-145` 为 SQL 聚合正面范式。**另**：arBalance 与超期预警经 `arApItemBiz.findOpenItems`（`:84,:187,:235-242`）全量内存加载。
  - `ErpPurDashboardBizModel.java`：KPI `:83` / Trend `:115` / `loadActiveInvoicesInRange(null,null)` 全表 `:142` / onTimeRate 全量 receives + 订单扫描 `setLimit(5000)` 静默截断 `:283-303`；三单匹配预警 N+1（每发票 3-4 次子查询 `:189-208` + `hasPriceVariance:314-358` + `:200` 循环 partner 查询）；超期预警逐行 partner `:236`；**另**：apBalance/超期经 `findOpenItems`（`:94,:224-226,:267-274`）。`findVendorTopN:159` 有 nameCache 而 `:200/:236` 无。
  - `ErpInvDashboardBizModel.java`：KPI `:82` 同请求两次全量扫描（`sumMoveQtyInRange:298` 逐行 `qty.abs()` 求和 `:320`、`sumOutgoingCostInRange:326`，各自 `loadDoneMovesInRange:347-354`+`loadMoveLines:356-362` 巨型 IN）；Trend `:112` StockLedger 12 个月全量；滞销预警 `:417-439` 全量出库记录求每物料最后出库日（**无上限**）；`ALERT_MAX_ROWS=5000`（`:63`）实际封顶的是 StockBalance 扫描（`:174,:217`）。`:441-444` 注释载明平台坑：无维度聚合会被强制注入主键维度生成非法 SQL——**聚合必须按维度分组后内存汇总**。
  - `ErpFinDashboardBizModel.java`：`sumArApOpen:228-242` / `sumBankBalance:216-226` 全量实体内存求和；**另**（首轮审计漏盘点）：KPI revenue/expense 按 `b.getSubject().getSubjectClass()` to-one 导航内存分类求和 `:63-77`、Trend 同型 `:96-115`。`applyOrgAndSchemaScope:259-269` 语义=「scope 不可解析跳过 filter」。
- fin 现金流量表（`ErpFinReportBizModel.java:511-529`）直接法/间接法各全量加载一次，且**过滤不对称**：直接法 `loadPostedVoucherLines(periodId)` 不过滤影子凭证（`:326`），间接法 `(periodId, true)` 排除 BUDGET/COMMITMENT（`:375`，`:506-510` javadoc D3 子裁决）——共享加载必须以未过滤超集为基准。
- 无界查询面（8+ 域 Report/Dashboard 零 setLimit）：inv `:235,:250,:278`、mfg `:377,:393,:407,:414`、qa `:331,:338,:350,:362`、cs `:253,:262,:295`、crm `:297,:345,:352,:366`、hr `:349,:365`、ast dashboard+report 全文 0 处 setLimit。
- 平台聚合机制边界（决定可行性）：
  - 仓内先例仅覆盖**单实体** GROUP BY + sum/count、单查询多聚合（`ErpInvStockLedgerBizModel.java:156-174`、`ErpCrmReportBizModel.java:209-211,:260-262`、`ErpPrjDashboardBizModel.java:92-93`）、单列/多维非键维度分组后内存汇总（`ErpInvDashboardBizModel.sumBalanceTotalCost:441-458` GROUP BY warehouseId、`findWarehouseDistribution` 同型）。**日期维度分组无既有先例**（全仓 `mainField("businessDate")` 维度用法零命中）——机制外推到日期列须 Phase 0 以平台文档/实仓 spike 证据确认。全仓 `addJoin`/owner 字段/`max()` 聚合零命中。
  - `QueryFieldBean` 字段仅 name/owner/aggFunc/alias（`../nop-entropy/docs-for-ai/02-core-guides/dql-query.md:40-47`），不支持表达式/CASE/跨列比较——跨实体谓词（如 receive.businessDate ≤ order.deliveryDate）与 abs() 聚合**不可 SQL 化**。
- 数值守护栏已就位：每域 `TestErp*Dashboard` JUnit + `TestErpFinReportRendering`（现金流直/间接逐分）+ 浏览器层 `*.value.spec.ts`（`playwright.config.ts` 在仓根）。**已知缺口**：sal/pur Trend 测试只断言跨月合计（`TestErpSalDashboard.java:114-120`、pur `:128-134`），未断言逐月桶值。
- 剩余差距：企业数据量下看板 OOM+秒级 RT 风险；无界查询随事务量线性恶化。

## Goals

- sal/pur/inv/fin 四看板中**可安全等价改写**的 KPI/Trend/TopN/预警求和方法改为 DB 级聚合，消除全量实体物化；三单匹配 N+1 消除（批量 in + 内存 Map，纯等价变换）。
- **豁免登记（Goal 边界）**：sal/pur 的 arBalance/apBalance/超期预警中经 `IErpFinArApItemBiz.findOpenItems` 的 item 加载保持现状（I*Biz 管道携带 CrudBizModel 数据权限检查点，`TestErpSalDashboardRowFilterCoverage.java:47-54,:142-144` 文档化为运行时语义；跨域直连 DAO 违反平台规则，加聚合接口触 api.xml 保护区）——计入本计划末尾残留站点白名单，不参与「清零」判定。
- 8+ 域 Report/Dashboard 无界查询全部有界化（SQL 聚合或每域硬上限常量），逐方法分类留痕。
- 全部改写方法**返回数值与改造前逐位一致**（含 abs/截断/影子凭证过滤语义——以守护栏测试 + 本计划显式等价性裁决为保证）。

## Non-Goals

- 不改任何 API 契约（api.xml/IBiz 签名不变；不给 IErpFinArApItemBiz 增加聚合方法——归 Deferred，触发条件见末尾）。
- 不动 fin 过账/凭证生成写路径；fin 报表仅读路径重写与去重，聚合口径（含 D3 影子凭证过滤不对称）不变。
- 不做 onTimeRate 聚合重写（跨实体谓词不可 SQL 化且消除截断必然改变数值语义——Deferred）。
- 不做现金流量表/账龄报表完整 SQL 聚合重写（Deferred）。
- 不做 AcctSchemaResolver 缓存（归服务层治理计划）。
- 不新增看板接口/不改 page.yaml。

## Task Route

- Type: `implementation-only change`（读路径查询重写，用户可见行为=数值不变、RT 改善）
- Owner Docs: `docs/design/dashboards.md`（KPI 语义权威，代码注释自引）、`docs/design/dashboard-semantic-layer.md`
- Skill Selection Basis: 实施后以 `code-quality-audit-prompt.md` 自检行为质量；机制探索（Phase 0）以 `../nop-entropy/docs-for-ai/02-core-guides/dql-query.md` 为平台依据

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（JUnit embedded 基建已存在；playwright E2E 经 `playwright.config.ts` webServer）

## Execution Plan

### Phase 0 - 平台聚合机制 Explore（决策前置）

Status: completed
Targets: 只读探索，不改代码；结论落盘计划文件本节下方
Skill: none

- Item Types: `Explore`×1 → `Decision`×3

- [x] Explore：精读 `../nop-entropy/docs-for-ai/02-core-guides/dql-query.md` + 实仓 QueryBean 用法，确认：①aggFunc 支持集合（sum/count/max/…）；②GROUP BY 维度 + 多聚合字段的规范写法；③**日期列作 GROUP BY 维度**的可行性证据（平台文档明示或实仓 spike 实证——仓内无日期分桶先例，`sumBalanceTotalCost:441-458` 为 warehouseId 维度先例，仅证明非键普通列可作维度，日期列外推须独立证据）
      - Skill: none
- [x] Decision-a（inv KPI 合并机制）：qty.abs() 语义保全方案——**按符号拆分两条单实体 GROUP BY**（`qty>0` 与 `qty<0` 各一条 `GROUP BY moveId` 求和）+ moves 范围查询做 moveType 映射，内存按 moveType 桶装并 abs 合成；数学等价性：Σ|qᵢ| = Σₘ(P(m)−N(m))、Σqᵢ = Σₘ(P(m)+N(m))（BigDecimal 精确无舍入，已第二轮审查验证）。**分块纪律**：两条 GROUP BY 查询携带 `in(moveId)` 过滤（与现状 loadMoveLines 同约束），moveId 超 500 分块执行多趟 GROUP BY 后内存合并部分和；替代方案（单条 SQL SUM）因混合符号下逐位不一致被否决
      - Skill: none
- [x] Decision-b（滞销 MAX 机制）：优先 `max()` aggFunc（若 Phase 0 确认支持）；否则 `(materialId, businessDate)` 两字段 GROUP BY 后内存取 MAX（行数 ≤ 物料×活跃天，天然有界）；**不加 setLimit**（物料维度自然有界，无序 limit=非确定截断）；`ALERT_MAX_ROWS` 保留于 StockBalance 扫描原位不动
      - Skill: none
- [x] Decision-c（月桶机制，按域分治）：sal/pur/inv Trend =「SQL GROUP BY 原始 businessDate（≤366 行）→ 内存按月分桶」，**以 Phase 0 ③证据通过为实施前提**（日期维度外推无仓内先例；证据不通过则降级为「GROUP BY (维度列, 主键) 后内存月桶」并在本节记录）；fin Trend =「GROUP BY (subjectId, periodId) 多维分组（StockLedger 多维先例内）+ period→(year,month) 内存映射，显式保留现 `:106` period.startDate 空值跳过语义」——fin GlBalance 无 businessDate 字段，不适用日期分桶
      - Skill: none

Exit Criteria:

- [x] 三项 Decision 结论与本节记录一致地落盘（支持/不支持证据各一条），Phase 1-4 按裁决机制实施

Phase 0 Explore 结论（2026-09-27 实证）：

- 平台文档 `dql-query.md` §QueryFieldBean 明载 `aggFunc: count/sum/avg/min/max`——**max() 受支持**（Decision-b 主路径成立；`QueryFieldBean.mainField(x).sum()` 为链式写法先例）。
- 维度分组机制 = `QueryBean.fields` 中「维度字段（mainField）+ 聚合字段」→ 隐式 GROUP BY 维度（`sumBalanceTotalCost:441-458` 实证，null 维度行单独成组被计入）。
- Decision-a/b/c 裁决确认：日期列与 warehouseId 同为普通列维度（同一 fields 机制），以 Phase 1 sal Trend 逐月断言为日期分桶的最终实证门（test-first）。

### Phase 1 - 销售看板 SQL 聚合改造

Status: completed
Targets: `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/dashboard/ErpSalDashboardBizModel.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/dashboard/TestErpSalDashboard.java`
Skill: none

- Item Types: `Fix`×3 + `Proof`×1
- Prereqs: Phase 0 裁决

- [x] Proof（先行，test-first）：`TestErpSalDashboard` 补 Trend **逐月桶 key+值断言**（现仅合计，月桶错位不可检）
      - Skill: none
- [x] Fix KPI 发票求和：改 `QueryFieldBean.sum()`（findCustomerTopN 范式）
      - Skill: none
- [x] Fix `getSalesTrend`：Decision-c 机制（GROUP BY businessDate → 内存月桶）
      - Skill: none
- [x] Fix 超期预警 partner 名称：distinct partnerId 批量 `in("id", ids)` 建 Map（id 列表超 500 分块查询）
      - Skill: none

Exit Criteria:

- [x] `TestErpSalDashboard` 全绿且含新增逐月断言；本文件「发票全量物化站点」清零（口径见 Closure 白名单）
- [x] `mvn test -pl module-sales/erp-sal-service -am` 0 failures

### Phase 2 - 采购看板 SQL 聚合 + 三单匹配 N+1 消除

Status: completed
Targets: `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/dashboard/ErpPurDashboardBizModel.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/dashboard/TestErpPurDashboard.java`
Skill: none

- Item Types: `Fix`×4 + `Decision`×1 + `Proof`×1
- Prereqs: Phase 0/1

- [x] Proof（先行）：`TestErpPurDashboard` 补 Trend 逐月桶断言
      - Skill: none
- [x] Fix KPI/Trend/vendorTopN：发票求和 SQL 聚合；Trend 按 Decision-c；vendorTopN 改 `GROUP BY supplierId + SUM 排序` 内存截取 TopN（DB 端 ORDER BY 若平台 ORDER 支持不确定则以内存排序兜底，行数=供应商数天然有界）
      - Skill: none
- [x] Fix 三单匹配预警：`hasPriceVariance` 逐发票三连查改一次性 `in("invoiceId", ids)`（超 500 分块）批量取三类行建 Map 比对（纯等价变换：Map 按 line id 键控、空行/空价跳过逻辑逐字平移）；supplier 名批量 Map 统一 nameCache 写法
      - Skill: none
- [x] Fix 超期预警 partner 批量 Map
      - Skill: none
- [x] Decision：onTimeRate **保持内存比对算法逐位不变**；`setLimit(5000)` 升级为命名常量（如 `ON_TIME_RATE_SCAN_CAP`）+ 注释说明截断边界（超出 cap 的 receive 计入分母但永不计入分子=准时率下偏）。SQL 重写被否决：跨实体谓词不可 SQL 化 + 消除截断必然改变数值（B2 裁决）
      - Skill: none
- [x] Proof `TestErpPurDashboard` 全绿数值不变（含 onTimeRate=0.5 迟到场景既有断言）
      - Skill: none

Exit Criteria:

- [x] 「发票全量物化站点」清零（白名单口径）；`TestErpPurDashboard` 数值不变全绿

### Phase 3 - 库存看板去重扫描 + SQL 聚合

Status: completed
Targets: `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/dashboard/ErpInvDashboardBizModel.java`
Skill: none

- Item Types: `Fix`×3
- Prereqs: Phase 0 Decision-a/b

- [x] Fix KPI 双扫描合并：Decision-a 机制（符号拆分 GROUP BY moveId ×2 + moves 映射 + 内存桶装），KPI 请求内 move_line 全量物化消除（行数从行级降到 move 级聚合）
      - Skill: none
- [x] Fix Trend：Decision-c（ledger GROUP BY businessDate → 内存月桶）
      - Skill: none
- [x] Fix 滞销预警：Decision-b 机制（max() 或 (materialId,businessDate) 分组内存 MAX；不加 cap）
      - Skill: none

Exit Criteria:

- [x] `TestErpInvDashboard` 全绿数值不变（含混合符号 qty 场景若既有种子覆盖；若无则补一条混合符号断言）
- [x] KPI 请求内 line 级全量加载站点清零（白名单口径）

### Phase 4 - 财务看板读路径 SUM 化 + 现金流量表去重加载

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/dashboard/ErpFinDashboardBizModel.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/report/ErpFinReportBizModel.java`
Skill: none

- Item Types: `Fix`×3 + `Proof`×1
- Prereqs: Phase 0

- [x] Fix `sumArApOpen`/`sumBankBalance`：内存求和改 **按一个有界维度（如 status/currencyId）分组 SQL sum 后内存汇总**（规避 `:441-444` 载明的无维度 SUM 非法 SQL 坑；**applyOrgAndSchemaScope 过滤语义逐字保留**，含「scope 不可解析跳过 filter」分支）
      - Skill: none
- [x] Fix KPI revenue/expense 分类求和（`:63-77`）+ Trend（`:96-115`）：两步机制——SQL 按 `(subjectId)` 分组求和（行数=科目数，天然有界）+ subject→subjectClass 映射（科目表小，单次加载）内存桶装；Trend 改「GROUP BY (subjectId, periodId) 多维分组（StockLedger 多维先例内）+ period→(year,month) 内存映射」，**显式保留现 `:106` period.startDate 空值跳过语义**（Decision-c fin 分支）
      - Skill: none
- [x] Fix 现金流量表去重：提取私有方法**共享未过滤超集单次加载**，间接法在内存重应用影子凭证排除（D3 过滤不对称保留：直接法=超集、间接法=超集减 BUDGET/COMMITMENT），聚合口径零变更
      - Skill: none
- [x] Proof：`TestErpFinDashboard` + `TestErpFinReportRendering` 全绿数值逐位不变（fin 为财务 adjacency：零写路径、零口径变更的证据 = 两测试类数值断言逐位通过）
      - Skill: none

Exit Criteria:

- [x] fin dashboard 文件内 4 个全量内存求和/分类站点（sumArApOpen/sumBankBalance/revenue-expense/Trend）清零；现金流两法共享单次加载（代码结构可证）

### Phase 5 - 其余域 Report/Dashboard 无界查询有界化

Status: completed
Targets: `ErpInvReportBizModel`、`ErpMfgReportBizModel`、`ErpQaReportBizModel`、`ErpCsReportBizModel`、`ErpCrmReportBizModel`、`ErpHrReportBizModel`、`ErpAstDashboardBizModel`、`ErpAstReportBizModel`（各域 service 模块）
Skill: none

- Item Types: `Fix`（逐方法）+ `Decision`（cap 值）×1
- Prereqs: Phase 1-4 范式定型

- [x] 逐文件盘点无界 `findAllByQuery` 站点（分析报告 §2.2 A9 清单为起点，实现时实仓 grep 复核），两分类处置并逐方法在计划本节下方留痕：①求和/计数型→SQL 聚合（Decision-c/维度分组纪律，数值逐位不变）；②明细/预警列表型→每域硬上限常量 cap=5000（各域各自声明常量，不跨域引用）；③主数据小表查找类（qa `:362`、crm `:366`、ast `:330`、cs `:295` 若核实为小表）→登记豁免理由
      - Skill: none
- [x] Decision：cap=5000 批次级裁决（与 inv 先例数值一致、各自声明；理由：预警/明细场景尾部数据价值低于 OOM 风险；现有单测种子量级≪cap 零影响）
      - Skill: none
- [x] Proof：触及域模块 `mvn test -pl <module> -am` 各自 0 failures
      - Skill: none

Exit Criteria:

- [x] 8 文件无界站点清零或逐方法登记处置（聚合/cap/豁免三类留痕于本节）
- [x] 触及域模块测试全绿

## Phase 5 留痕区（2026-09-27 实测填充）

**①聚合类（SQL 投影聚合，数值逐位等价）**：

| 站点 | 处置 |
|------|------|
| `ErpAstDashboardBizModel` KPI 原值/累计折旧 | 新增 `sumInServiceAssetValues`：GROUP BY status 单组 SUM×2（orgId null-skip 保留） |
| `ErpAstDashboardBizModel.findAssetCategoryDistribution` | GROUP BY categoryId SUM(originalValue, accumulatedDepreciation)，null 类别组跳过（原语义） |
| `ErpAstDashboardBizModel.getDepreciationTrend` | GROUP BY period SUM(actualAmount)（替代无日期过滤的全量已执行计划物化；null period 组跳过） |
| `ErpAstDashboardBizModel.sumPeriodDepreciation` | GROUP BY period 单组 SUM |
| `ErpAstDashboardBizModel.sumCipBalance` | GROUP BY isCompleted 单组 SUM |
| `ErpAstDashboardBizModel.loadAssetIdsWithExecutedDepreciationInPeriod` | GROUP BY assetId 维度投影去重（替代全量物化） |
| `ErpInvReportBizModel.toTraceRows` | N+1 消除：逐 move eq 查询改 in(moveId) 单趟批载分组（等价变换，超 500 move 由单 IN 承载——追踪链行数量级小，未分块，留痕） |

**②cap 类（明细/列表型，各域常量 REPORT_LIST_MAX_ROWS=5000 各自声明）**：

| 站点 | 分类依据 |
|------|---------|
| `ErpQaReportBizModel.loadInspections` | 过滤全可选的明细列表 |
| `ErpQaReportBizModel.loadNcrs` | 日期过滤全可选的明细列表 |
| `ErpCsReportBizModel.loadTickets` | 类型过滤全可选的明细列表 |
| `ErpMfgReportBizModel.loadVarianceLines` | 过滤全可选的差异明细列表 |
| `ErpMfgReportBizModel.aggregateActualQty` | 求和型但 periodOverlaps 跨列谓词不可 SQL 化 → cap（截断=差异下偏方向，代码注释登记） |
| `ErpAstDashboardBizModel` 缺提折旧列表（loadInServiceAssets 带 limit 参数） | 行级字段进列表行 |
| `ErpAstReportBizModel.loadAssets` | 类别过滤全可选的明细列表 |
| `ErpAstReportBizModel.loadDisposals` | 日期过滤全可选的明细列表 |

**③豁免类（主数据/配置小表或有界过滤，登记理由）**：

| 站点 | 豁免理由 |
|------|---------|
| qa `resolveMaterialNames`/`countActionsByNcr` | in(id/in(ncrId)) 有界 |
| cs `aggregateSurveys`/`resolveTicketTypeNames` | in(ticketId)/in(id) 有界 |
| crm `resolveCampaignNames`/`loadForecasts`/`aggregateForecastLines`/`resolveStageNames` | in(id) 有界或 eq(id) 可选过滤；campaign/stage/forecast 为配置型小表 |
| hr `loadAdjustments`/`loadEmployees`/`:279` partner 查询 | eq(simulationId)/in(id) 有界 |
| ast dash/report `loadCategoryNames`/`aggregatePeriodDepreciation`/`resolveCategoryNames` | in(id)/in(assetId) 有界 |
| inv `findCandidateMoves`（行扫描 `:235` 与 move 查询 `:250`） | eq(materialId)/仓库+moveIds or 过滤有界（双空参数早退） |
| mfg `deriveCrpWindow` | 既有 setLimit(5000)（原样） |
| mfg `loadApprovedForecasts`/`aggregateForecastLines` | eq(status=APPROVED) 配置型，量级=在用预测数 |

Phase 5 验证：qa 9 / cs 5 / mfg 13 / ast 9（dash+report）/ inv 8（report rendering）全绿，0 failures。

## 残留站点白名单（「清零」口径定义）

- 「全量物化站点清零」= 触及文件内 `findAllByQuery`/`findList` 于**交易大表**（invoice/order/move/move_line/stock_ledger/voucher/voucher_line/ar_ap_item）无 limit 无投影的物化站点消除。
- 显式白名单（允许残留，登记理由）：
  - sal/pur dashboard 经 `IErpFinArApItemBiz.findOpenItems` 的 4 处调用（Goal 豁免项，见 Goals；触发条件见 Deferred）
  - pur onTimeRate 内存比对（Decision：算法逐位保留 + 命名常量 cap）
  - Phase 5 登记豁免的主数据小表查找类站点（留痕区列名）

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 agent_50540c14，2026-09-27）——Blocker B1：sal/pur arBalance/apBalance/超期经 findOpenItems 全量加载与 Goal/Non-Goal/Deferred 三处矛盾；B2：onTimeRate SQL 重写与「逐位一致」互斥且守护栏不可检；Major M1：跨实体聚合（月桶/MAX/moveType 合并）仓内零先例且 abs 语义不可 SQL 化；M2：fin dashboard 漏盘点 2 站点；M3：现金流共享加载略去 D3 过滤不对称；M4：owner docs 路径全部不存在；M5：ALERT_MAX_ROWS cap 错置；Minor m1-m5。
- Independent draft review iteration 2: needs revision（独立子代理 agent_e8d16159，2026-09-27）——B1/B2/M2/M3/M4/M5/m2-m5 消解验证通过（含 Decision-a 数学等价性验证通过）。残留：**N1**：Decision-c 引用「日期分桶先例 :446-458」不实（该区间实为 warehouseId 分组，全仓日期维度零先例）；**N2**：fin GlBalance 无 businessDate，Decision-c fin 分支不可实施；N3：Phase 4 sum 类未复述无维度 SUM 坑纪律；N4：Decision-a in(moveId) 分块未写明。修订：Baseline 先例集改真实集合+「日期外推须 Phase 0 证据」；Decision-c 按域分治（sal/pur/inv 日期分桶以证据为前提+降级路径、fin 改 (subjectId,periodId) 多维分组+period 映射+空值语义保留）；Phase 4 sum 类补维度分组纪律；Decision-a 补分块合并纪律。
- Independent draft review iteration 3: accept（独立子代理 agent_e4893800，2026-09-27 定点复核）——N1 成立（warehouseId 分组实核 + 日期维度广谱零先例 + 外推/证据前提结构消解）；N2 成立（GlBalance 无 businessDate、Trend 经 period 导出月桶、Period 有 year/month/startDate、(subjectId,periodId) 在多维先例内且数学等价）；N3/N4 成立；一致性通过。备注：实施时「超 500 分块」以命名常量落盘（非阻塞）。**计划可进入实施。**

## Plan Status 演进

> Plan Status: active（2026-09-27，iteration 3 accept 后置位）

## Closure Gates

- [x] 范围内行为完成（Phase 0-5 全部退出标准达成）
- [x] 相关文档对齐：`docs/logs/2026/09-27.md` 登记；分析报告 §2 状态回填；`docs/design/dashboards.md` 若含实现机制注记则同步聚合方式说明（KPI/报表口径不变）
- [x] 已运行验证：触及模块 `mvn test -pl <module> -am` + `mvn clean install -DskipTests` 全仓 BUILD SUCCESS + 浏览器层 `*.value.spec.ts` 抽样（sal/pur/inv/fin 看板至少各 1 spec）数值全绿 + `bash docs/audits/nop-compliance-checker.sh`（漂移逐站点分类）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计（round 1 FAIL→整改；round 2 NEEDS REVISION→最小整改集完成→审计方定点核验 ROUND2 CONFIRMED，agent_1de96c01，2026-09-28）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### IErpFinArApItemBiz 聚合接口（sumOpenAmount 类）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 新增 BizQuery 触 api.xml 契约面（保护区 dual-agent-approval）；且 I*Biz 管道携带数据权限检查点，聚合接口需重新设计检查点语义（`TestErpSalDashboardRowFilterCoverage` 文档化）——接口设计与审批超出本读路径优化批边界
- Successor Required: `yes`——触发条件：api.xml 加法变更立项（含双批准）时，sal/pur arBalance/apBalance/超期预警同步切聚合接口并从白名单移除

### onTimeRate 聚合重写（消除 5000 截断）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 跨实体谓词（receive.businessDate ≤ order.deliveryDate）在 QueryFieldBean 无表达式能力下不可 SQL 化；消除截断必然改变数值（当前截断语义=准时率下偏，保守方向）；sql-lib `<eql>` 方案需独立机制裁决
- Successor Required: `yes`——触发条件：企业量级实测 onTimeRate 截断失真暴露，或 owner doc（dashboards.md）对截断语义作出裁决后

### fin 现金流量表/账龄报表完整 SQL 聚合重写

- Classification: `optimization candidate`
- Why Not Blocking Closure: 报表数字须与 owner doc 逐分对账；本轮已消除双倍加载（单次超集共享）
- Successor Required: `yes`——触发条件：月度凭证量实测 >1 万张或报表 RT 超标（profiling 证据）

## Closure

Status Note: Phase 0-5 全部完成。四核心域看板全量物化站点消除（sal/pur/inv/fin；pur 三单匹配发票头在结束审计 Blocker-1 整改中改为 id/code/supplierId 三列投影，残留站点清零），三单匹配 N+1 消除，8 文件无界治理三类处置留痕，现金流双法共享单次加载。数值逐位一致由 test-first 逐月/混合符号断言 + 既有 JUnit 数值护栏 + 浏览器层 5 value spec 种子驱动断言三层证明。实现期两处执行期纠错（inv 出库成本语义、fin 跨 session 懒加载）均经既有测试捕获并修正，护栏有效。执行期偏差留痕（Phase 3 inv）：滞销预警实现用 (moveId,materialId) 投影聚合（较 Decision-b 字面 max()/两字段分组更优：无过滤投影行数=move 数天然有界）；KPI 投影不携 in(moveId) 分块（同界）——已登记代码注释与日志，混合符号测试守护（结束审计 round 2 Minor m-4 登记为非阻塞）。checker 实测（round 2 复测）：R2b 236（≤基线 242）/R2c 1563（较 HEAD −8，N+1 消除副产物；对基线 1553 残余 +10 全为 f3.10 预存遗留，HEAD 时 +18，本批净收敛）。

Closure Audit Evidence:

- Auditor / Agent: round 1 FAIL（agent_3b4b8fff，2026-09-27：Blocker-1 pur :208 残留站点未登记→已改头投影消除；Blocker-2 分析报告 §2 回填缺位→已补；程序违规结束审计门控预勾→已取消）。round 2 NEEDS REVISION（agent_1de96c01，2026-09-28：Major M-1 dashboards.md:268 机制注记未同步→已更正「DB 级 GROUP BY supplierId 聚合 + 内存排序截取 TopN」；m-1 checker 数字刷新→R2b 236/R2c 1563 已同步计划与日志；m-2 回填措辞→已更正；m-3 预存 owner-doc 散记漂移 2 处→登记归 doc-only 清理批；m-4 Phase 3 偏差→已回注 Closure）。**定点核验 ROUND2 CONFIRMED**（agent_1de96c01，2026-09-28：①:268 单行与现实现逐项相符且该文件仅此一行改动；②数字与实测一致；③Closure Audit Evidence 完整）。

Follow-up:

- （无阻塞跟进）
