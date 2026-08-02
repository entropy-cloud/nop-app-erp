# rc-ma1-a1-11 mfg-F4 差异/批次/看板 需求-实现符合性五级追踪审计报告

> 报告类型：requirement-compliance MA1 切片 A1.11
> 切片：mfg-F4 差异/批次/看板（roadmap 标签；权威 UC 范围 = UC-MFG-11/12/13 共 3 UC）
> 审计时间：2026-08-02
> 审计基线 HEAD：`c9e87bbc402f7fe088142fecbbce30406d1524cc`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）
> 上游计划：`docs/plans/2026-08-02-2231-2-rc-ma1-a1-11-mfg-f4-variance-batch-kanban.md`（草案审查 `acceptable as-is` 两轮，独立子代理 `ses_03d19fb47ffe3ZtNdx4KVu6nl1` + `ses_03d15960cffe2at0KFGM8941f3`）
> 真相源层级（§4 Q1）：L1 = `docs/design/manufacturing/use-cases.md`（UC-MFG-11 `:195` / UC-MFG-12 `:216` / UC-MFG-13 `:238`，锚点经 `docs/audits/rc-requirement-baseline-inventory.md` A1.11 确认，inventory `:345` 一致）；L2 = `variance-analysis.md` + `batch-genealogy.md` + `dashboards.md §7`（设计参考，冲突一律以 L1 为准）；L3 = 实仓代码；L4 = 测试；L5 = 复用 A4.2a/A4.2b/A2.6a + 本切片差异。

---

## 9. 与既有 MA2/MA4 报告差异增量声明（前置声明，便于读者识别复用边界）

> 依方法论 §6 段落 9 + §去重协议，本报告前置声明与既有 MA2/MA4 报告的差异增量。

| 既有报告 | 覆盖维度 | 已证实结论（本切片复用） | 本切片补的差异增量（需求契约视角） |
|---------|---------|----------------------|--------------------------|
| `2026-07-29-0024-arm-ma4-mfg-work-order-bom-code-quality.md`（A4.2a） | 工单/BOM 链路**代码质量**（编排健壮性/BOM 展开/算术/错误处理/失败恢复闭环/架构边界/测试有效性） | BomExpander 算术正确；错误处理规范化（全 NopException + erp.err.mfg.*）；**3 P1（P1-MA4-007 完工编排差异过账吞咽 / P1-MA4-008 跨域 daoFor / P1-MA4-009 业财异常路径零覆盖）+ 1 P2**。A4.2a:244 显式交接：ProductionVarianceCalculator 实现质量 + BatchGenealogyWriter 实现质量归 A4.2b | 本切片不重审代码质量维度；只补**需求契约 vs 实现符合性**（UC-MFG-12 6 类差异完整性 + PPV 归属 + UC-MFG-11 KPI/阈值/权限符合性 + resolved finding HEAD 复核[P1-MA4-007/009]） |
| `2026-07-29-0024-arm-ma4-mfg-mrp-quality-code-quality.md`（A4.2b） | MRP/质量集成/成本核算/基因追溯**代码质量**（算术正确性/事务边界/异常规范化/测试主路径断言强度） | 维度 2 核心实现正确性 **PASS**：ProductionVarianceCalculator 6 类差异算术 PASS（A4.2b:54 逐类复核 + divideSafe 防除零 + BigDecimal 类型安全）+ dispatchVarianceAlertIfOverThreshold 错误传播降级 PASS（A4.2b:55）+ BatchGenealogyWriter 基因链写入幂等一致性 PASS（A4.2b:56 writeOnCompletion best-effort + ensureOutputLot 幂等 + usedInputLots 去重）+ 重算幂等四步链（红冲→删旧→重算→派发 + 一致不变量）。**3 P1（P1-MA4-010 委外吞咽 / P1-MA4-011 测试有效性 / P1-MA4-012 跨域 daoFor）+ 1 P2** | 本切片不重审代码质量维度；只补**需求契约 vs 实现符合性**（UC-MFG-12 6 类完整性与 PPV 归属 + UC-MFG-13 追溯链完整性与召回报告降级 + resolved finding HEAD 复核[P1-MA4-010/011/012]） |
| `2026-07-28-0109-arm-ma2-mfg-work-order-jobcard-state-machine.md`（A2.6a） | 工单/作业卡状态机业务正确性 + 事务回滚一致性 | 工单 10 态全守卫；完工 COMPLETED 触发差异/批次写入的触发点已覆盖（A2.6a pass 结论） | 本切片复用完工 COMPLETED 触发点结论，不重审状态机维度；只补完工触发差异/批次写入的需求契约验收 |
| `2026-07-28-1510-arm-ma2-multi-company-isolation.md`（A2.18，P1-MA2-093） | 11 dashboard BizModel 经 IDaoProvider 直访绕过（空）认证管道（`:99-101` 显式列 dashboard） | P1-MA2-093 orgId 查询隔离全仓未落地，含 mfg dashboard 直访；**resolved R1.29**（`ErpOrgIsolationQueryTransformer` 全局 IQueryTransformer 注入） | 本切片 UC-MFG-11 ③ 行级权限投影复用 P1-MA2-093（resolved 维持），追加 RC 交叉引用注记，不新建 |

**结论**：本切片裁决焦点 = **UC-MFG-11/12/13 需求契约↔实现符合性**。差异算术正确性/基因链幂等/状态机触发点/代码质量四面**复用 A4.2a/A4.2b/A2.6a pass 结论**（接受，不重审）；本切片只补需求视角差异（UC-MFG-11 看板 KPI/阈值/权限符合性 + UC-MFG-12 6 类完整性与 PPV 归属 + UC-MFG-13 追溯链完整性与召回报告降级 + resolved finding HEAD 复核）。

---

## 1. 需求契约原文（L1 逐字引用，禁止转述）

> 真相源：`docs/design/manufacturing/use-cases.md`（UC 锚点经 `docs/audits/rc-requirement-baseline-inventory.md` A1.11 确认 = `:195/:216/:238`，inventory `:345` 一致）。

### UC-MFG-11 制造看板（`use-cases.md:195-213`）

逐字引用验收标准：

```
// KPI 指标数据源正确(实时聚合, 非硬编码)
KPI 卡片值 == 对应实体的实时聚合(按期间/orgId/权限过滤)                        [断言①]
  在制工单/完工量/准时率, 齐套待产, 状态分布, 齐套不足/延期预警

// 预警触发
预警项 == 满足阈值条件的记录(阈值来自系统配置, 非硬编码)                         [断言②]

// 权限
看板数据受行级权限约束(只看自己组织/部门/成本中心)                              [断言③]
```

涉及机制：`../dashboards.md`、各域 state-machine.md、roles-and-permissions.md（行级权限）。

### UC-MFG-12 生产成本差异分析（`use-cases.md:216-234`）

逐字引用验收标准：

```
工单完工过账 → 触发差异计算（6 类，对齐 variance-analysis.md + ProductionVarianceCalculator）  [断言④]
材料用量差异 = (实际用量 - 标准用量) × 标准单价
人工效率差异 = (实际工时 - 标准工时) × 标准小时费率
人工费率差异 = (实际小时费率 - 标准小时费率) × 实际工时
制造费用差异（OVERHEAD） = 实际制造费用 - 标准制造费用
产量差异（VOLUME） = (实际产出 - 计划产出) × 标准单位成本
委外费差异（SUBCONTRACT） = 实际委外费 - 标准委外费
（材料价格差异 PPV 归采购域：在采购入库 DONE 时由 inventory 域
 InvPostingDispatcher.dispatchPurchasePriceVariance 捕获过账 PURCHASE_PRICE_VARIANCE，
 不在生产差异内，避免重复计入）                                                [断言⑤/⑥]
差异记录逐条写入 ErpMfgCostVariance（每差异类型一条，varianceType = MATERIAL_USAGE /
 LABOR_EFFICIENCY / LABOR_RATE / OVERHEAD / VOLUME / SUBCONTRACT）              [断言⑤]
差异报表可按工作中心/产品/期间/差异类型分组聚合                                 [断言⑦]
```

涉及机制：`variance-analysis.md`、`bom-and-routing.md §成本计算`。

### UC-MFG-13 生产批次追溯（`use-cases.md:238-251`）

逐字引用验收标准：

```
完工入库时：记录输入批次→输出批次关系到 ErpMfgBatchGenealogy                  [断言⑧]
前向追溯：给定 outputLotId → 查出所有 inputLotId                              [断言⑨]
反向追溯：给定 inputLotId → 查出所有 outputLotId                              [断言⑩]
多级追溯：递归上下游节点展示完整批次链                                         [断言⑪]
召回报告：从问题批次出发识别所有受影响成品批次                                 [断言⑫]
```

涉及机制：`batch-genealogy.md`、`inventory/lot-management.md`、`quality/inspection-integration.md`。

**断言计数**：UC-MFG-11 ×3（①②③）+ UC-MFG-12 ×4（④⑤[⑥内嵌于⑤]⑦）+ UC-MFG-13 ×5（⑧⑨⑩⑪⑫）= **12 条验收标准**（草案审查 iter1/iter2 实测一致，覆盖 3 UC 无跳号无合并）。

---

## 2. 实现证据（L3 代码路径，含行号 + 跨域调用链）

### 2.1 UC-MFG-11 制造看板（KPI 聚合 + 状态分布 + 趋势 + 延期预警 + CRP 图）

| 验收点 | 实现路径（file:line） | 备注 |
|-------|---------------------|------|
| KPI 在制工单数 | `ErpMfgDashboardBizModel.getDashboardKpi:65-90` → `countByDocStatusIn:253-258`（docStatus IN [IN_PROCESS, STOCK_RESERVED]） | DB count，非硬编码 |
| KPI 本期完工量 | `getDashboardKpi` → `sumCompletedQtyInRange:267-278`（COMPLETED + actualEndDate 期内 Σ completedQuantity） | 实时聚合 |
| KPI 准时率 | `getDashboardKpi` → `computeOnTimeRate:280-295`（COMPLETED 且 actualEndDate ≤ plannedEndDate / 全 COMPLETED） | 实时聚合 |
| KPI 齐套待产 | `getDashboardKpi:78` → `countByDocStatus(STOCK_PARTIAL):260-265` | DB count |
| 工单状态分布 | `getWorkOrderStatusDistribution:93-115`（DB GROUP BY docStatus + COUNT） | DB 级聚合（非全表物化） |
| 产成品产出趋势 | `getDashboardTrend:118-144`（近 N 月完工量按 actualEndDate 月份聚合） | 实时聚合 |
| 齐套不足预警 | `getDashboardKpi` 返回 `stockPartialCount`（STOCK_PARTIAL 计数）+ `dashboards.md §7 :175` 显式标缺件明细 Non-Goal（ErpMfgMaterialReservation 未物化） | 状态驱动 |
| 工单延期预警 | `findDelayedWorkOrderAlert:147-173`（plannedEndDate < today 且未 COMPLETED/CLOSED/CANCELLED → 列出 + overdueDays） | **today 截止，无 config threshold** |
| CRP 负荷/产能 | `getCrpLoadChartData:186-249` 委派 `CrpLoadCalculator.getLoadReport`，按 loadDate 聚合 | 实时聚合（A4.2b 已审算术 PASS） |

### 2.2 UC-MFG-12 生产成本差异分析（计算 + 阈值告警 + 过账/红冲 + 重算）

| 验收点 | 实现路径（file:line） | 备注 |
|-------|---------------------|------|
| 完工触发差异计算（config-gated） | `ErpMfgWorkOrderReportCompletionProcessor.reportCompletion:86-92`（willFinish && `isVarianceAutoCalcEnabled()` 时红冲→删旧→重算→派发四步；G3 错误分级 catch `:93-101`） | config `erp-mfg.variance-auto-calc-enabled` 默认 false |
| 6 类差异计算 | `ProductionVarianceCalculator.calculateVariances:106-216`：MATERIAL_USAGE `:128-137` / LABOR_EFFICIENCY `:139-157` / LABOR_RATE `:159-167` / OVERHEAD `:169-178` / VOLUME `:180-189`（costElement=MATERIAL 简化建模）/ SUBCONTRACT `:191-203`（仅非零时生成行） | A4.2b:54 算术 PASS 复用 |
| 逐条写 ErpMfgCostVariance | `calculateVariances:205-211`（逐行 dao.saveEntity + flush）；`buildLine:302-327`（varianceType=入参 / posted 默认 false） | varianceType 枚举对齐 L1 6 类 |
| PPV 归采购域（不重复计入） | 生产侧 `ProductionVarianceCalculator` 仅算 MATERIAL_USAGE（**不**算 MATERIAL_PRICE，`:131` 命名 MATERIAL_USAGE 非 MATERIAL_PRICE）；采购侧 `InvPostingDispatcher.dispatchPurchasePriceVariance:103`（inventory 域，被 `:79` 调用）捕获 `PURCHASE_PRICE_VARIANCE`（`PurchasePriceVarianceAcctDocProvider:57`） | 跨域职责分离，避免重复计入 |
| 差异阈值告警 | `ProductionVarianceCalculator.dispatchVarianceAlertIfOverThreshold:225-261`（config `erp-mfg.variance-alert-enabled` 默认 true + `erp-mfg.variance-alert-threshold` 默认 100；超阈值调 `IErpSysNotificationBiz.notify("mfg.production-variance")`；通知失败降级 warn 不阻断） | config-gated 三键 |
| 差异过账（PRODUCTION_VARIANCE） | `ProductionVarianceDispatcher.dispatchIfApplicable:70-118`（按成本要素汇总净差异 → `buildEvent:156-184` 组装 PostingEvent → `executor.postEvent` → 成功 markPosted；catch `:111-117` 吞异常保持 posted=false） | billHeadCode=`{wo.code}-PV` |
| 差异红冲 | `ProductionVarianceDispatcher.reverseIfExists:133-154`（billHeadCode=`{wo.code}-PV` 对称 → `executor.reverse` → try/catch 守护吞异常不阻断重算） | 红冲失败孤儿凭证经 log warn 可观测 |
| 手动重算四步幂等 | `ErpMfgCostVarianceCalculateVariancesProcessor.calculateVariances:33-52`（COMPLETED 校验 `:39-43` → `reverseIfExists:45` → `deleteByWorkOrder:47` → `calculateVariances:48` → `dispatchIfApplicable:50`） | 两条同型 call site（A=手动 Processor / B=完工 `ErpMfgWorkOrderReportCompletionProcessor:89-92`）共享四步链 |
| 多维分组聚合报表 | `ErpMfgCostVarianceBizModel.aggregateByType:60-88`（按 varianceType 聚合 standardAmount/actualAmount/varianceAmount；可选 costElement 过滤）+ `findByWorkOrder:54-57` | 报表渲染归 Deferred |

### 2.3 UC-MFG-13 生产批次追溯（写入 + 前向/反向/多级 + 召回报告）

| 验收点 | 实现路径（file:line） | 备注 |
|-------|---------------------|------|
| 完工写入 input→output | `BatchGenealogyWriter.writeOnCompletion:64-76`（config-gated `erp-mfg.genealogy-write-enabled` 默认 true；best-effort try/catch `:71-75` LOG.error 不阻断完工）→ `doWrite:80-145`（找输出行 + 找带批次领料行 + ensureOutputLot + 按领料行写基因行 inputLotId/outputLotId/比例分摊 inputQty） | Decision 1 完工聚合点 + Decision 2 自动建批 + Decision 3 best-effort |
| 完工自动建批 | `BatchGenealogyWriter.ensureOutputLot:149-170`（batchNo=`FG-{woCode}` 派生；既有批次累加 totalQuantity/availableQuantity；新建状态 OPEN） | Decision 2 |
| 前向追溯 | `BatchGenealogyTracer.forwardTrace:46-51`（outputLotId → 直接 inputLots，eq filter） | 单级 |
| 反向追溯 | `BatchGenealogyTracer.backwardTrace:53-58`（inputLotId → 直接 outputLots，eq filter） | 单级 |
| 多级递归追溯 | `BatchGenealogyTracer.traceChain:65-110`（FORWARD 用 output 找 input 向上游递归；BACKWARD 用 input 找 output 向下游递归；visited 环路防护 + maxDepth 上限超限抛 ErrorCode `:89-92`；非法方向抛 ErrorCode `:66-78`） | 含 config `erp-mfg.genealogy-max-trace-depth` |
| 召回报告（降级） | `ErpMfgBatchGenealogyBizModel.recallReport:70-107`（`setDegraded(true):78` + 起始批次自身可能是受影响成品 `collectAffectedIfFinishedGood:84` + 反向递归 backwardTrace 找下游产出批次 `:89-105` + `collectAffectedIfFinishedGood:109-128` 排除 REJECTED + 查是否为基因行产出视为受影响候选） | **RecallReport.degraded=true**（位置/去向查询归 inventory successor，`batch-genealogy.md:141-143`） |

---

## 3. 测试证据（L4 测试断言，注明断言强度）

| 验收点 | 测试引用（TestFile#method） | 断言强度 | 备注 |
|-------|----------------------------|---------|------|
| UC-MFG-11 KPI 算术 | `TestErpMfgDashboard#testKpiEmptyDatasetReturnsZeros:49-56` + `#testKpiAggregationAndOnTimeRate:58-91` | **强**（inProcessCount=2/periodCompletedQty=300/stockPartialCount=1/onTimeRate=0.3333 精确数值 + 期间外工单不计入） | 实时聚合强验证 |
| UC-MFG-11 状态分布 | `TestErpMfgDashboard#testWorkOrderStatusDistribution:93-104` | **强**（状态分组计数 + 排序） | DB GROUP BY 验证 |
| UC-MFG-11 趋势 | `TestErpMfgDashboard#testTrendMonthlySeries:106-122` | **强**（近 2 月合计 50+70=120） | 月聚合验证 |
| UC-MFG-11 延期预警 | `TestErpMfgDashboard#testDelayedWorkOrderAlertTriggersAndNot:124-140` | **强**（W1 过去+IN_PROCESS 触发 / W2 过去+COMPLETED 不触发 / W3 未来+IN_PROCESS 不触发 + overdueDays=10） | **today 截止，无 config threshold 测试**（与 P2-RC-009 一致） |
| UC-MFG-11 行级权限 | —（无行级权限测试，`enableActionAuth=OptionalBoolean.FALSE` 全程关闭认证） | **无** | 复用 P1-MA2-093（resolved R1.29） |
| UC-MFG-11 CRP 图 | `TestErpMfgDashboardCrpChart`（同包） | 强（plan `2026-07-17-2010-1` 覆盖负荷/产能/负荷率派生） | A4.2b 已审算术 PASS |
| UC-MFG-12 6 类差异 | `TestErpMfgProductionVariance`（833 行）：6 类行级数值断言（MATERIAL_USAGE `:114` / LABOR_EFFICIENCY `:120` / LABOR_RATE `:127` / OVERHEAD `:133` / VOLUME `:138`）+ 6 类含 SUBCONTRACT `:336` + SUBCONTRACT 零跳过 `:363-365` | **强**（行级数值精确 + 6 类完整性 + 边界[两侧 0 不生成 SUBCONTRACT 行]） | A4.2b:54 算术 PASS 印证 |
| UC-MFG-12 完工触发 + 凭证 | `TestErpMfgProductionVariance`：完工 config-gated 触发 + PRODUCTION_VARIANCE 凭证生成 + posted=true + 凭证行级（SUBJECT_SUBCONTRACT_VARIANCE=1416 / WIP_SUBCONTRACT=1417 `:391-396`）+ 过账失败无凭证 `:433` | **强** | 闭合业财一体路径 |
| UC-MFG-12 阈值告警 | `TestErpMfgVarianceAlert#testVarianceOverThresholdTriggersNotify:67-92` + `#testVarianceUnderThresholdSkipsNotify:93-117` + `#testAlertDisabledSkipsNotify:118-` | **强**（超阈值 notify 调用 + ErpSysNotification 行落入 + 未超阈值跳过 + config 关闭静默 3 场景） | **闭合 P1-MA4-007 测试可见性**（R2.11） |
| UC-MFG-12 重算幂等红冲 | `TestErpMfgVarianceRecomputeReversal#testRecomputeReversesOriginalVoucherAndRepostsNew:119-188`（原凭证 isReversed=true + 红字凭证 + 新 NORMAL 凭证 + 反查 {wo.code}-PV 仅 1 条 isReversed=false + 数据行全 posted=true）+ `#testCompletionAutoRecomputeReversesViaCallSiteB:188-251`（完工 call site B）+ `#testReverseFailureDoesNotBlockRecompute:251-285`（红冲失败容错）+ `#testFirstCalculateVariancesToleratesNoSourceVoucher:285-` | **强**（一致不变量 ErpFinVoucherBillR 反查 + 数据行与凭证金额一致 + 全 posted=true + 红冲失败不阻断） | A4.2b 重算幂等四步链印证 |
| UC-MFG-12 多维聚合 | `TestErpMfgProductionVariance`（aggregateByType 覆盖） | 强（按类型聚合数值） | |
| UC-MFG-13 完工写入基因链 | `TestErpMfgBatchGenealogy#testWriteOnCompletionWithBatchMaterial:72-112` | **强**（inputLotId/inputMaterialId/inputQty=2/outputMaterialId/outputQty=2/lotStatus=RELEASED/isInputConsumed=true 行级断言 + 产出批次自动创建 batchNo/totalQuantity） | Decision 1/2 强验证 |
| UC-MFG-13 无批次跳过 | `TestErpMfgBatchGenealogy#testWriteSkippedWhenNoBatchMaterial:114-136` | **强**（无 batchNo → 不报错、不写基因行） | Decision 边界 |
| UC-MFG-13 前向/反向 | `TestErpMfgBatchGenealogy#testForwardAndBackwardTrace:138-172` | **强**（forwardTrace 返回 1 直接输入 + backwardTrace 返回 1 直接产出） | 单级 |
| UC-MFG-13 多级 + 环路 + maxDepth | `TestErpMfgBatchGenealogy#testTraceChainCycleProtectionAndMaxDepth:174-217` | **强**（FORWARD 多级 2 边 + BACKWARD 多级 2 边 + 环路防护不无限递归 + maxDepth=1 超限抛错 + 非法方向抛错） | 递归 + 环路 + 深度全验证 |
| UC-MFG-13 召回报告 | `TestErpMfgBatchGenealogy#testRecallReport:219-237` | **仅冒烟**（仅断言 status=0 + data 非空；**未断言 affectedLots 含 lotB/lotC 内容 + 未断言 degraded=true + 未断言 sourceLotId=lotA**） | **P1-RC-010**——验收标准⑫"识别受影响成品批次"零内容断言 |
| UC-MFG-13 best-effort 写失败 | —（无测试触发 best-effort 写失败路径，`BatchGenealogyWriter.writeOnCompletion:71-75` catch） | **无** | Decision 3 缺口可观测性（归 P1-RC-010 一并登记） |

---

## 4. 运行时行为证据（L5）

> 复用 A4.2a/A4.2b/A2.6a 已证实行为（§去重协议），本切片只补需求视角差异。

### 4.1 UC-MFG-12 完工触发差异计算/过账链 运行时行为 = 强闭环（HEAD 复核）

完工达量（willFinish）+ config `erp-mfg.variance-auto-calc-enabled=true` 时，`ErpMfgWorkOrderReportCompletionProcessor:86-92` 执行红冲→删旧→重算→派发四步链：
- **G3 错误分级**（`ErpMfgWorkOrderReportCompletionProcessor:93-101`）：「无 FIRMED 标准成本」（`ERR_VARIANCE_NO_STANDARD_COST`）→ LOG.warn 容错跳过；其他失败 → LOG.error + `dispatchVarianceFailureAlert`（`ErpMfgWorkOrderProcessor:150-167`）派发 `IErpSysNotificationBiz.notify("mfg.production-variance-posting-failure", ctx)`。
- 内层 `ProductionVarianceDispatcher.dispatchIfApplicable:111-117` 仍 catch 吞过账失败保持 posted=false（设计：告警在外层派发，posted=false 数据级可观测）。
- 期末结账前置检查扫 finance 异常工作台 PENDING/RETRYING 兜底。

**P1-MA4-007 HEAD 复核 = resolved 维持**（详见 §5.4）。

### 4.2 UC-MFG-12 6 类差异算术 + 重算幂等 运行时行为 = PASS（复用 A4.2b）

A4.2b:54 已逐类复核 6 类差异算术正确性（MATERIAL_USAGE/LABOR_EFFICIENCY/LABOR_RATE/OVERHEAD/VOLUME/SUBCONTRACT）+ divideSafe 防除零 + BigDecimal 类型安全；A4.2b:55 dispatchVarianceAlertIfOverThreshold 错误传播降级 PASS；A4.2b:56 BatchGenealogyWriter writeOnCompletion best-effort + ensureOutputLot 幂等 + usedInputLots 去重 PASS；重算幂等四步链（红冲→删旧→重算→派发）+ 一致不变量经 `TestErpMfgVarianceRecomputeReversal` 强断言印证。本切片复用，不重审算术/幂等维度。

### 4.3 UC-MFG-13 完工写入 + 追溯链 运行时行为 = 强闭环（复用 A4.2b + 本切片 L4）

A4.2b:56 已证实 BatchGenealogyWriter 基因链写入幂等一致性（writeOnCompletion best-effort + ensureOutputLot 幂等 + usedInputLots 去重）；本切片 `TestErpMfgBatchGenealogy` 强断言覆盖完工写入（行级数值 + 自动建批）+ 前向/反向（单级）+ 多级递归（FORWARD/BACKWARD + 环路防护 + maxDepth + 非法方向）。运行时行为符合 L1 ⑧⑨⑩⑪。

### 4.4 UC-MFG-13 召回报告 运行时行为 = 降级但满足 L1 ⑫

`recallReport` 实际行为：以问题批次为起点，反向递归 backwardTrace 找出所有下游产出批次，对每个产出经 `collectAffectedIfFinishedGood` 判定是否为成品产出（forwardTrace 非空即视为产出）→ 加入 affectedLots。**行为满足 L1 ⑫"识别所有受影响成品批次"**。`degraded=true` 标注位置/去向查询归 inventory successor（`batch-genealogy.md:141-143` 显式登记，触发条件=inventory 暴露按批次的位置/去向查询方法集时）。

### 4.5 UC-MFG-11 KPI 实时聚合 运行时行为 = 强闭环

`getDashboardKpi` 全部 KPI（inProcessCount/periodCompletedQty/stockPartialCount/onTimeRate）经 DB count/Σ 实时聚合（`TestErpMfgDashboard#testKpiAggregationAndOnTimeRate` 强数值断言印证：2/300/1/0.3333）；状态分布经 DB GROUP BY；趋势经 actualEndDate 月份聚合。**断言①满足**（非硬编码常量）。

### 4.6 UC-MFG-11 延期/齐套预警 运行时行为 = 功能可用但阈值非 config 驱动

`findDelayedWorkOrderAlert` 用 `plannedEndDate.isBefore(today)` 截止（today 来自 `CoreMetrics.currentDate()`，动态但非 config threshold）；齐套不足预警为 `stockPartialCount`（STOCK_PARTIAL 状态计数，无 config threshold）。**断言②部分偏离**：L1 要求"阈值来自系统配置,非硬编码"，实际预警阈值 derivation 为状态/日期驱动而非 config 键（与 finance 看板 `findCashFlowAlert` 用 `AppConfig.var` 对比偏差）。预警项本身返回真实数据（非硬编码常量），但阈值不可配置。

### 4.7 UC-MFG-11 行级权限 运行时行为 = 复用 P1-MA2-093（resolved R1.29）

`ErpMfgDashboardBizModel` 经 `IDaoProvider` 直访（`:55 daoProvider` + `:57 ormTemplate`），所有查询经 `dao.findAllByQuery(q)` / `ormTemplate.findListByQuery(q)`。**P1-MA2-093 resolved R1.29**（`ErpOrgIsolationQueryTransformer` 全局 IQueryTransformer 注入）覆盖此直访路径——本切片 HEAD 复核 mfg dashboard 直访维持 A2.18:99-101 登记，R1.29 全局守卫覆盖。断言③投影复用 P1-MA2-093，追加 RC 交叉引用注记，不新建。

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 符合性结论 + resolved finding HEAD 复核）

### 5.1 五级追踪矩阵

| UC（断言） | L1（逐字） | L2（设计参考） | L3（代码 file:line） | L4（测试 + 强度） | L5（运行时行为） | 冲突裁决 |
|-----------|-----------|--------------|-------------------|----------------|---------------|---------|
| **UC-MFG-11** 制造看板（3 断言：①KPI 实时聚合 ②预警阈值 config ③行级权限） | `use-cases.md:195-213`（§1 逐字引用） | `dashboards.md §7 制造看板 :163-178`（KPI 表 + 预警列表）+ `§设计原则 :9-13`（指标不硬编码 + 行级权限）+ `§实现约定 :236-243`（GraphQL 聚合 + orgId/部门/成本中心过滤 + 阈值放 NopSysVariable） | `ErpMfgDashboardBizModel.java:65-90` getDashboardKpi + `:93-115` 状态分布 + `:118-144` 趋势 + `:147-173` 延期预警；CRP 图 `:186-249` | `TestErpMfgDashboard#testKpiAggregationAndOnTimeRate:58-91`（强）+ `#testWorkOrderStatusDistribution:93-104`（强）+ `#testTrendMonthlySeries:106-122`（强）+ `#testDelayedWorkOrderAlertTriggersAndNot:124-140`（强，但 today 截止无 config threshold）；**无行级权限测试** | KPI 实时聚合强闭环（§4.5）；预警功能可用但阈值非 config（§4.6）；行级权限复用 P1-MA2-093 R1.29（§4.7） | L2 与 L1 一致 |
| **UC-MFG-12** 生产差异（4 断言：④完工触发 ⑤6 类逐条写 ErpMfgCostVariance ⑥PPV 归采购不重复 ⑦多维聚合报表） | `use-cases.md:216-234`（§1 逐字引用） | `variance-analysis.md §核心计算逻辑 :59-69`（6 类公式）+ `§使用流程 :81-89`（PPV 归采购 InvPostingDispatcher.dispatchPurchasePriceVariance + ProductionVarianceCalculator + Dispatcher PRODUCTION_VARIANCE + 手动 calculateVariances + 阈值告警 dispatchVarianceAlertIfOverThreshold）+ `§重算幂等实现注记 :91-109`（红冲→删旧→重算→派发四步 + 一致不变量）+ `§配置点 :111-117`（三键） | `ErpMfgWorkOrderReportCompletionProcessor.java:86-92` 完工触发四步 + `ProductionVarianceCalculator.java:106-216` 6 类 + `:225-261` 阈值告警 + `ProductionVarianceDispatcher.java:70-118` 过账 + `:133-154` 红冲 + `ErpMfgCostVarianceCalculateVariancesProcessor.java:33-52` 手动重算 + `ErpMfgCostVarianceBizModel.aggregateByType:60-88`；PPV 跨域 `InvPostingDispatcher.dispatchPurchasePriceVariance:103` + `PurchasePriceVarianceAcctDocProvider:57` | `TestErpMfgProductionVariance`（833 行强断言：6 类行级数值 + 6 类含 SUBCONTRACT + 完工触发 + 凭证 + posted=true + 过账失败无凭证）+ `TestErpMfgVarianceAlert`（3 场景强）+ `TestErpMfgVarianceRecomputeReversal`（4 测试强：一致不变量 + 红冲失败容错 + 首次无源凭证容错） | 行为已证实（引用 A4.2b §2 算术 PASS + §事务边界与幂等 PASS + 本切片 §4.1/§4.2 完工触发链 + 重算幂等强闭环）；**P1-MA4-007 完工差异吞咽 HEAD 复核 resolved**（§5.4） | L2 与 L1 一致 |
| **UC-MFG-13** 批次追溯（5 断言：⑧完工记录 input→output ⑨前向 ⑩反向 ⑪多级递归 ⑫召回报告识别受影响成品批次） | `use-cases.md:238-251`（§1 逐字引用） | `batch-genealogy.md §核心查询 :67-81`（前向/反向/全链 SQL）+ `§实施决策 :115-139`（Decision 1 完工聚合点 / Decision 2 自动建批 / Decision 3 best-effort）+ `§recallReport 降级说明 :141-143`（degraded=true 仅返回受影响成品批次集合，位置/去向归 inventory successor） | `BatchGenealogyWriter.java:64-76` writeOnCompletion + `:80-145` doWrite + `:149-170` ensureOutputLot；`BatchGenealogyTracer.java:46-51` forwardTrace + `:53-58` backwardTrace + `:65-110` traceChain；`ErpMfgBatchGenealogyBizModel.java:70-107` recallReport（degraded=true `:78`） | `TestErpMfgBatchGenealogy#testWriteOnCompletionWithBatchMaterial:72-112`（强）+ `#testWriteSkippedWhenNoBatchMaterial:114-136`（强）+ `#testForwardAndBackwardTrace:138-172`（强）+ `#testTraceChainCycleProtectionAndMaxDepth:174-217`（强）；**`#testRecallReport:219-237` 仅冒烟**（未断言 affectedLots 内容/degraded/sourceLotId）；**best-effort 写失败无测试** | 完工写入 + 追溯链强闭环（§4.3）；召回报告降级但满足 L1 ⑫"识别受影响成品批次"（§4.4，degraded=true 位置/去向归 inventory successor 增强 successor）；测试有效性缺口 P1-RC-010 | L2 recallReport 降级与 L1 ⑫"识别受影响成品批次"不冲突——L1 仅要求"识别"，位置/去向为增强维度（owner doc scenario 1 :85-91 非验收标准）。**L2 §recallReport 降级说明 :141-143 为 documented simplification 候选，§4 三判据核验见 §5.3** |

### 5.2 候选缺口/偏离逐条分级（12 验收标准全覆盖）

| # | 验收标准 | L3/L4 实证 | 分级 | 命中判据 |
|---|---------|----------|------|---------|
| ① | UC-MFG-11 KPI 卡片值实时聚合（非硬编码） | `getDashboardKpi:65-90` 全 DB count/Σ；`TestErpMfgDashboard#testKpiAggregationAndOnTimeRate` 强数值断言（2/300/1/0.3333） | **接受** | L1-L5 全对齐，实时聚合非硬编码 |
| ② | UC-MFG-11 预警阈值来自 config（非硬编码） | `findDelayedWorkOrderAlert:147-173` 用 `plannedEndDate.isBefore(today)`（today 动态但非 config threshold）；齐套不足为 STOCK_PARTIAL 计数（无 config threshold）；mfg dashboard 无 `erp-mfg.dashboard-*-threshold` config key | **P2（新建 P2-RC-009）** | §2 P2①（次要验收标准未完全满足——预警功能可用[返回真实数据]但阈值 derivation 为状态/日期驱动而非 config 键；与 finance 看板 `findCashFlowAlert` 用 `AppConfig.var` 对比偏差） |
| ③ | UC-MFG-11 行级权限（orgId/deptId/costCenter 过滤） | `ErpMfgDashboardBizModel` 经 IDaoProvider 直访（`:55-57`），无显式 orgId 过滤；R1.29 全局 `ErpOrgIsolationQueryTransformer` 守卫覆盖；`TestErpMfgDashboard` `enableActionAuth=FALSE` 无权限测试 | **P1（复用 P1-MA2-093，不新建）** | §2 P1①（功能未落地——dashboard 直访绕过认证管道）；**同根因同控制点** A2.18:99-101 显式列 dashboard，R1.29 已 resolved 维持，追加 RC 交叉引用注记 |
| ④ | UC-MFG-12 完工过账触发差异计算 | `ErpMfgWorkOrderReportCompletionProcessor:86-92` config-gated 四步链；`TestErpMfgProductionVariance` 完工触发强断言 | **接受** | L1-L5 全对齐 |
| ⑤ | UC-MFG-12 6 类差异逐条写 ErpMfgCostVariance | `ProductionVarianceCalculator:131-203` 6 类逐行（varianceType 枚举对齐）；`TestErpMfgProductionVariance:336` assertEquals(6, lines.size()) + 行级数值 | **接受** | L1-L5 全对齐（A4.2b:54 算术 PASS 复用） |
| ⑥ | UC-MFG-12 PPV 归采购域不重复计入 | 生产侧仅 MATERIAL_USAGE（非 MATERIAL_PRICE，`:131`）；采购侧 `InvPostingDispatcher.dispatchPurchasePriceVariance:103` + `PurchasePriceVarianceAcctDocProvider:57` 捕获 PURCHASE_PRICE_VARIANCE | **接受** | L1-L5 全对齐（跨域职责分离，避免重复计入） |
| ⑦ | UC-MFG-12 多维分组聚合报表 | `ErpMfgCostVarianceBizModel.aggregateByType:60-88`（varianceType 聚合 + costElement 过滤）；`findByWorkOrder:54-57` | **接受** | L1-L5 全对齐（工作中心/产品/期间/差异类型维度经 costElement/varianceType/workcenterId/businessDate 字段支持） |
| ⑧ | UC-MFG-13 完工入库记录 input→output | `BatchGenealogyWriter.writeOnCompletion:64-76` + `doWrite:80-145`；`TestErpMfgBatchGenealogy#testWriteOnCompletionWithBatchMaterial:72-112` 行级强断言 | **接受** | L1-L5 全对齐 |
| ⑨ | UC-MFG-13 前向追溯（outputLotId→inputLotId 全集） | `BatchGenealogyTracer.forwardTrace:46-51`；`TestErpMfgBatchGenealogy#testForwardAndBackwardTrace:160-165`（1 直接输入） | **接受** | L1-L5 全对齐 |
| ⑩ | UC-MFG-13 反向追溯（inputLotId→outputLotId 全集） | `BatchGenealogyTracer.backwardTrace:53-58`；`TestErpMfgBatchGenealogy#testForwardAndBackwardTrace:167-171`（1 直接产出） | **接受** | L1-L5 全对齐 |
| ⑪ | UC-MFG-13 多级递归追溯 | `BatchGenealogyTracer.traceChain:65-110`（环路防护 + maxDepth）；`TestErpMfgBatchGenealogy#testTraceChainCycleProtectionAndMaxDepth:174-217`（FORWARD/BACKWARD 多级 2 边 + 环路 + maxDepth + 非法方向全强断言） | **接受** | L1-L5 全对齐 |
| ⑫ | UC-MFG-13 召回报告识别受影响成品批次 | `ErpMfgBatchGenealogyBizModel.recallReport:70-107`（degraded=true + 反向递归 + collectAffectedIfFinishedGood）；`TestErpMfgBatchGenealogy#testRecallReport:219-237` **仅冒烟**（status=0 + data 非空，未断言 affectedLots 内容/degraded） | **接受 on ⑫ 功能**（降级版满足"识别"——见 §5.3）+ **P1（测试有效性 P1-RC-010）** | ⑫功能：§接受（降级版满足 L1"识别"，位置/去向归 inventory successor 增强 successor）；**测试：§2 P1⑤（仅冒烟——验收标准⑫"识别受影响成品批次"零内容断言）** |

### 5.3 UC-MFG-13 recallReport 降级 §4 三判据核验

L2 `batch-genealogy.md §recallReport 降级说明 :141-143` 显式标注 degraded=true + 位置/去向归 inventory successor。**但 L1 ⑫ 字面仅要求"识别所有受影响成品批次"**——降级版的 `collectAffectedIfFinishedGood` 经 forwardTrace 判定产出 + 反向递归 backwardTrace 找下游产出，**满足"识别"**。位置/去向查询不在 L1 ⑫ 字面（属 owner doc scenario 1 `:85-91` "确定召回范围"的增强语义，非验收标准）。

**§4 三判据核验（针对"降级标注是否构成 documented simplification"）**：
- **(i) plan 含独立 plan-audit 通过记录**：plan `2026-07-07-0305-3` 含独立草案审查（AI 代理审查），**但 AI 代理审查非人工批准**（§4：「代理独立审计通过 = 审计裁决质量证据，不算人工批准」）。
- **(ii) owner doc 显式 documented simplification 标注且经人工批准**：`batch-genealogy.md:141-143` 有显式降级说明 + 触发条件（inventory 暴露按批次的位置/去向查询方法集时），**但无 git log/commit/discussion 人工批准痕迹**（AI 落地补注）。
- **(iii) product-scope 范围裁剪登记**：product-scope 未将"召回报告位置/去向查询"列入"不在范围"。

**裁决**：三判据核验聚焦于"位置/去向查询缺失"是否构成 documented simplification。但因 **L1 ⑫ 字面仅要求"识别"（降级版满足）**，位置/去向为**增强维度 successor**（非 L1 验收标准）→ **不需要** documented simplification 裁决。L2 降级说明登记 successor 触发条件即合规（与 inventory 域能力演进挂钩）。

**结论**：UC-MFG-13 ⑫ 功能 = **接受**（降级版满足 L1"识别受影响成品批次"；位置/去向查询为增强 successor，归 inventory 域能力演进触发，不构成本切片 P1/P2 finding）。测试有效性缺口（testRecallReport 仅冒烟）单独裁决为 P1-RC-010。

### 5.4 resolved finding HEAD 复核（差异/批次相关全分区）

| Finding ID | 原 Verdict（audit-remediation） | HEAD `c9e87bbc4` 复核结论 | 状态 |
|-----------|-------------------------------|------------------------|------|
| **P1-MA4-007**（完工编排层差异吞咽致业财悬挂） | A4.2a：`reportCompletion:227-239`（pre-R6.1 行号）catch(Exception)→LOG.error 吞咽 GL 缺凭证无告警 | **R1.16 G3 错误分级 + 告警派发落地**：`ErpMfgWorkOrderReportCompletionProcessor.java:86-102`（R6.1 per-mutation 拆分后位置）catch 块分级——`isNoStandardCostError(e)`（`ErpMfgWorkOrderProcessor:140-147`）→ LOG.warn 容错跳过；其他 → LOG.error + `dispatchVarianceFailureAlert`（`ErpMfgWorkOrderProcessor:150-167`）派发 `IErpSysNotificationBiz.notify("mfg.production-variance-posting-failure")`。owner doc `state-machine.md:180` §实现约定同步记录。`TestErpMfgVarianceAlert` 3 场景强断言覆盖（R2.11） | **resolved 维持**（R1.16 done + R2.11 测试补强，会计正确性类 Q4 关键证据闭合——GL posted=false 悬挂经告警派发可观测） |
| **P1-MA4-009**（工单/领料/BOM 业财异常路径零覆盖 + 完工 GL voucher 行级断言缺失） | A4.2a：dispatcher 过账失败悬挂 + 多币种完工入库 GL voucher 行级断言缺失 | **R2.11 done**：`TestErpMfgVarianceAlert`（3 场景：超阈值 notify 调用 + ErpSysNotification 行落入 + 未超阈值跳过 + config 关闭静默）强断言覆盖差异失败告警派发（闭合 P1-MA4-007 测试可见性）；多币种完工入库 GL voucher 行级断言已补 | **resolved 维持**（R2.11 done） |
| **P1-MA4-010**（委外 issue/receipt 过账失败吞咽无 posted 追踪） | A4.2b：SubcontractPostingDispatcher issue/receipt 路径无 posted 标志 | **R1.16 done**（roadmap 2026-07-31 确认，arm-index ✅ resolved）：委外 issue/receipt 段引入段级 posted 追踪 + 失败进异常工作台。本切片核验 `SubcontractPostingDispatcher` 范围外（属 A4.2b 委外链路），arm-index 维持 resolved | **resolved 维持**（R1.16，本切片核验范围外，arm-index 维持） |
| **P1-MA4-011**（MRP/成本/基因/委外测试有效性：多币种凭证行级 + 业财异常悬挂 + CostRollup 成环） | A4.2b：多币种凭证行级断言缺失 + 业财异常悬挂零覆盖 + CostRollup ERR_BOM_CYCLE 无测试 | **R2.11 done**（roadmap 2026-07-31 确认，arm-index ✅ resolved）：多币种 E2E + dispatcher 过账失败悬挂测试 + CostRollup 成环 assertThrows 已补。本切片 UC-MFG-12/13 测试有效性经 `TestErpMfgProductionVariance`/`TestErpMfgVarianceAlert`/`TestErpMfgVarianceRecomputeReversal`/`TestErpMfgBatchGenealogy` 强断言印证（除 testRecallReport 冒烟归 P1-RC-010） | **resolved 维持**（R2.11，本切片测试有效性总体强，testRecallReport 冒烟归 P1-RC-010 独立） |
| **P1-MA4-012**（MRP/成本/基因/委外跨域 daoFor 绕 I\*Biz） | A4.2b：多站点 mfg→{inv,md,sal} 只读 + BatchGenealogyWriter→inv 写 O-4 豁免未登记 | **plan 2026-07-29-2225-1 done**（arm-index ✅ resolved）：读侧统一裁决登记于 `data-dependency-matrix.md §9`——md 子集=可迁移 / inv·sal 子集=永久只读豁免；BatchGenealogyWriter→inv 写（`daoFor(ErpInvBatch):266` 自动建批 ensureOutputLot）O-4 豁免登记 | **resolved 维持**（plan 2026-07-29-2225-1，BatchGenealogyWriter daoFor(ErpInvBatch) 写 O-4 豁免已登记） |

**P0 即时通道**：本切片 Phase 1 定级**未出 P0**——UC-MFG-12 P1-MA4-007 已 resolved（R1.16 G3 错误分级 + 告警派发落地，会计正确性类 Q4 关键证据闭合）；UC-MFG-13 召回报告降级满足 L1 ⑪"识别"（位置/去向为增强 successor）；UC-MFG-11 阈值非 config（P2）+ 行级权限（复用 P1-MA2-093 resolved）均非活跃数据破坏类。**不触发 MR0**，无 R0.n 实体行追加。

### 5.5 每 UC 符合性结论汇总

| UC | 结论 | 命中判据 | 关键证据 |
|----|------|---------|---------|
| **UC-MFG-11** | **接受 on ①**；**P2 on ②**（新建 P2-RC-009）；**P1 on ③**（复用 P1-MA2-093） | §2 接受（①KPI 实时聚合强闭环）+ §2 P2①（②预警阈值非 config 键）+ §2 P1①（③行级权限 dashboard 直访，复用 P1-MA2-093 resolved R1.29） | ①TestErpMfgDashboard 强断言；②findDelayedWorkOrderAlert 用 today 截止非 config；③A2.18:99-101 dashboard 登记覆盖 |
| **UC-MFG-12** | **接受**（on ④⑤⑥⑦）+ **resolved finding 复核**（P1-MA4-007/009/010/011/012 resolved 维持） | §2 接受（6 类差异完整性 + PPV 归采购不重复 + 多维聚合 + 完工触发四步链全实现，强测试覆盖） | L1 4 断言 L3/L4/L5 全对齐；P1-MA4-007 HEAD 复核 R1.16 G3 错误分级 + 告警派发落地 |
| **UC-MFG-13** | **接受 on ⑧⑨⑩⑪⑫功能**；**P1 on ⑫测试有效性**（新建 P1-RC-010） | §2 接受（⑧⑨⑩⑪ 追溯链完整性强测试覆盖；⑫功能降级版满足 L1"识别"——§5.3 §4 三判据核验）+ §2 P1⑤（⑫测试仅冒烟——testRecallReport 未断言 affectedLots 内容/degraded + best-effort 写失败无测试） | L1 5 断言功能侧全对齐；测试侧 testRecallReport 冒烟归 P1-RC-010 |

---

## 6. 与 arm-index 衔接（复用 or 新增裁决）

### 6.1 候选 finding 既有 arm-index grep 比对

| 候选缺口 | arm-index grep（mfg 差异/批次/看板同域同控制点） | 裁决 | 依据 |
|---------|----------------------------------------------|------|------|
| UC-MFG-11 ② 预警阈值非 config（预警功能可用但阈值 derivation 状态/日期驱动） | grep `阈值|config|dashboard|mfg` → A2.18 P1-MA2-093（行级权限，不同控制点）/ A1.7 UC-FIN-17 ⑪（finance 看板阈值 config 化，不同域）/ P1-MA4-007（差异过账吞咽，不同控制点） | **新建 `P2-RC-009`** | **新功能点维度**——mfg 看板预警阈值 derivation（状态/日期驱动 vs config 键），与 finance 看板阈值 config 化（P1-MA2-093 / UC-FIN-17 ⑪）不同域不同控制点；与 P1-MA4-007 差异过账不同控制点（差异 dispatcher 阈值 vs 看板预警阈值） |
| UC-MFG-11 ③ 行级权限（dashboard 直访绕过认证管道） | grep `dashboard|行级权限|orgId|mfg` → **A2.18 P1-MA2-093 命中**（`:99-101` 11 dashboard BizModel 显式列 `ErpMfgDashboardBizModel` 为直访绕过认证管道之一，orgId 查询隔离全仓未落地） | **复用 `P1-MA2-093`** | 同根因（无 IUserContext.getOrgId + 空 data-auth + dashboard 直访）同控制点（行级权限/dashboard 直访）；R1.29 已 resolved（`ErpOrgIsolationQueryTransformer` 全局 IQueryTransformer 注入覆盖 dashboard 直访）；追加 RC A1.11 交叉引用注记，**不新建** |
| UC-MFG-12 P1-MA4-007 完工差异过账吞咽（HEAD 复核） | grep `P1-MA4-007` → A4.2a 已登记 + A1.9/A1.10 已 HEAD 复核 resolved 维持 | **复用 `P1-MA4-007`**（resolved 维持） | 同根因同控制点（完工编排层差异过账 catch 吞咽），R1.16 已 resolved；本切片 HEAD `c9e87bbc4` 复核 R6.1 per-mutation 拆分后位置 `ErpMfgWorkOrderReportCompletionProcessor:86-102` 维持 G3 错误分级 + 告警派发。**会计正确性类 Q4 无例外关键证据** |
| UC-MFG-12 业财异常路径测试（P1-MA4-009/011 HEAD 复核） | grep `P1-MA4-009|P1-MA4-011` → A4.2a/A4.2b 已登记 + A1.9/A1.10 已 HEAD 复核 resolved | **复用 `P1-MA4-009` + `P1-MA4-011`**（resolved 维持） | R2.11 已 resolved；本切片核验 `TestErpMfgVarianceAlert`/`TestErpMfgProductionVariance`/`TestErpMfgVarianceRecomputeReversal` 强断言覆盖印证 |
| UC-MFG-12 P1-MA4-010 委外 + P1-MA4-012 跨域 daoFor | grep `P1-MA4-010|P1-MA4-012` → A4.2b 已登记 | **复用**（resolved 维持，本切片范围外） | 委外链路归 A4.2b 范围；BatchGenealogyWriter→inv 写 O-4 豁免已登记（plan 2026-07-29-2225-1） |
| UC-MFG-13 ⑫ 召回报告降级 | grep `recallReport|degraded|genealogy|召回` → 无既有 finding（A4.2b:56 仅覆盖 BatchGenealogyWriter 写入幂等，未覆盖 recallReport 测试有效性） | **新建 `P1-RC-010`**（测试有效性维度） | **新控制点**——recallReport 测试仅冒烟（验收标准⑫"识别受影响成品批次"零内容断言）+ best-effort 写失败路径无测试，与 P1-MA4-011（业财异常悬挂 + CostRollup 成环）不同控制点（基因链召回报告 vs 差异/委外过账）；与 P1-MA4-009（工单/BOM 业财异常）不同控制点 |
| UC-MFG-13 best-effort 写失败（Decision 3 缺口可观测性） | grep `best-effort|Decision 3|genealogy` → A4.2b:56 覆盖 best-effort 实现质量 PASS（写入幂等），未覆盖 best-effort **写失败路径测试** | **合并入 `P1-RC-010`** | 与 testRecallReport 同属 UC-MFG-13 测试有效性维度（召回报告测试 + best-effort 写失败测试），合并裁决 |

### 6.2 新建 finding 列表

**裁决：本切片新建 1 项 P1 finding `P1-RC-010` + 1 项 P2 finding `P2-RC-009`，复用 5 项既有 finding（P1-MA2-093 / P1-MA4-007 / P1-MA4-009 / P1-MA4-010 / P1-MA4-011 / P1-MA4-012）。**

**`P1-RC-010` UC-MFG-13 召回报告测试断言强度不足 + best-effort 写失败路径无测试（验收标准⑫零内容断言）**

- **L1**：`use-cases.md:248` 逐字「召回报告：从问题批次出发识别所有受影响成品批次」。
- **L3**：`ErpMfgBatchGenealogyBizModel.recallReport:70-107` 实现正确（degraded=true + 反向递归 + collectAffectedIfFinishedGood 识别产出成品批次）；`BatchGenealogyWriter.writeOnCompletion:71-75` best-effort catch（Decision 3 设计选择）。
- **L4 缺口**：(a) `TestErpMfgBatchGenealogy#testRecallReport:219-237` **仅冒烟**——只断言 `resp.getStatus()==0` + `resp.getData() != null`，**未断言**：①`affectedLots` 含 lotB/lotC 内容；②`degraded=true`；③`sourceLotId=lotA`。验收标准⑫"识别受影响成品批次"零内容断言，召回算法回归（如 `collectAffectedIfFinishedGood` 恒返空集）对测试不可见。(b) best-effort 写失败路径（`BatchGenealogyWriter.writeOnCompletion:71-75` catch 分支）**无测试触发**——无 mock 内部抛异常→断言完工入库仍成功 + LOG.error 可观测的测试（Decision 3 缺口可观测性）。
- **影响**：召回算法回归无防护 + best-effort 失败可观测性无测试背书。**功能本身实现正确**（经 §4.3/§4.4 行为证据 + 代码阅读确认），仅测试断言强度不足。
- **分级判据**：§2 P1⑤（测试断言完全缺失或仅冒烟——验收标准⑫"识别受影响成品批次"零内容断言）。
- **非 P0**：功能实现正确非活跃数据破坏；召回报告属质量追溯辅助功能（非会计正确性/核心循环）。
- **与既有 finding 不同控制点**：vs P1-MA4-011（MRP/成本/基因/委外业财异常悬挂 + CostRollup 成环，不同控制点：基因链召回报告 + best-effort 写失败 vs 差异/委外过账）；vs P1-MA4-009（工单/BOM 业财异常，不同域不同控制点）；vs P1-MA5-005（mfg 业财一体异常路径系统性空洞，归并 MA4 差异/委外投影，不含基因链召回维度）。
- **修复方式**：MR1（R1.0 展开为 RC-R1.n）——补：(1) testRecallReport 强化断言（affectedLots 含 lotB/lotC + degraded=true + sourceLotId=lotA + lotStatus=RELEASED）；(2) best-effort 写失败测试（mock `doWrite` 抛异常→断言 `reportCompletion` 仍成功 + LOG.error 含 workOrderCode）。**纯测试补充，按 roadmap 预授权类目[代码逻辑修复/测试补充]可自动执行，不触发 §5 ask-first**。

**`P2-RC-009` UC-MFG-11 制造看板预警阈值非 config 驱动（状态/日期驱动而非 config 键）**

- **L1**：`use-cases.md:206` 逐字「预警项 == 满足阈值条件的记录(阈值来自系统配置, 非硬编码)」。
- **L3**：`ErpMfgDashboardBizModel.findDelayedWorkOrderAlert:147-173` 用 `plannedEndDate.isBefore(today)`（today 来自 `CoreMetrics.currentDate()`，动态但非 config threshold）；齐套不足预警 = `stockPartialCount`（STOCK_PARTIAL 状态计数，无 config threshold）；mfg dashboard 无 `erp-mfg.dashboard-*-threshold` config key（对比 finance `findCashFlowAlert` 用 `AppConfig.var("erp-finance.cash-flow-alert-threshold")`）。
- **L4**：`TestErpMfgDashboard#testDelayedWorkOrderAlertTriggersAndNot:124-140` 强断言（W1 触发/W2/W3 不触发 + overdueDays=10），但**无 config threshold 测试**（无 config key 可测）。
- **影响**：预警功能可用（返回真实数据非硬编码常量），但阈值不可配置（运营无法调"延期 N 天才预警"或"齐套不足阈值比例"）。**与 finance 看板阈值 config 化对比偏差**。
- **分级判据**：§2 P2①（次要验收标准未完全满足——主路径[预警返回真实数据]OK，边界[阈值 derivation 非 config 键]弱）。
- **与既有 finding 不同控制点**：vs P1-MA2-093（行级权限 dashboard 直访，不同控制点：权限 vs 阈值 config）；vs P1-MA4-007（差异过账吞咽，不同控制点：差异 dispatcher 阈值 vs 看板预警阈值）；vs A1.7 UC-FIN-17 ⑪（finance 看板阈值 config 化 PASS，不同域）。
- **修复方式**：successor watch-only（P2 登记不强制）——补：(1) `ErpMfgConstants` 增 `CONFIG_DASHBOARD_DELAYED_ALERT_DAYS`（默认 0=今天截止）+ `findDelayedWorkOrderAlert` 按 config 阈值过滤；(2) 齐套不足预警增 `CONFIG_DASHBOARD_STOCK_PARTIAL_ALERT_ENABLED` config 开关（已有 stockPartialCount 计数，仅需 config-gated 预警卡片显示控制）。**纯 BizModel 代码逻辑 + ErpMfgConstants config key 补充，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**。

### 6.3 双向可追溯

- **本切片 → 新 finding**：UC-MFG-13 ⑫测试有效性 → `P1-RC-010`（新建，目标 MR1，纯测试补充预授权自动执行）；UC-MFG-11 ②阈值 → `P2-RC-009`（新建，successor watch-only，纯 BizModel/config 补充预授权自动执行）。
- **本切片 → 复用既有**：UC-MFG-11 ③行级权限 → `P1-MA2-093`（resolved R1.29，追加 RC A1.11 交叉引用注记）；UC-MFG-12 P1-MA4-007/009/010/011/012 HEAD 复核 resolved 维持。
- **新 finding → arm-index**：`P1-RC-010` + `P2-RC-009` 写入 arm-index MA1(RC) finding 分区。
- **修复行引用 finding**：MR1 的 RC-R1.n 修复行须含 `P1-RC-010` 交叉引用。
- **MV V.3 校验**：closure audit 核验 `P1-RC-010` 修复状态为 `done` 或显式 successor；`P2-RC-009` 为 successor watch-only（不强制关闭）。

---

## 7. 静态存疑点清单（供 MA4 展开）

| SP# | 存疑点 | 触发条件 | 交接 |
|-----|--------|---------|------|
| SP-1 | **完工触发差异过账失败运行时悬挂可见性**：P1-MA4-007 已 resolved（R1.16 G3 错误分级 + 告警派发落地），但告警通道 `IErpSysNotificationBiz.notify("mfg.production-variance-posting-failure")` 的实际运行时投递成功率（notify best-effort 降级不阻断主流程）+ 运营对告警的响应闭环（手动重算入口 `calculateVariances` 是否被实际使用）需运行时确认 | config=true（业务要求完工自动算差异）+ 永久性失败（标准成本未发布/卷算 base cost 缺失） | A4.2 运行时探针（与 A1.9 SP-1 同根因） |
| SP-2 | **best-effort 基因链写失败运行时缺口可观测性**：Decision 3 try/catch 不阻断完工，但实际运营中 `BatchGenealogyWriter.writeOnCompletion:71-75` catch 分支被触发频率 + LOG.error 是否被监控采集 + 基因链缺口的业务影响（部分完工无追溯行）需运行时确认 | 领料单带批次 + 完工入库 + 写入异常（如 ErpInvBatch 锁冲突/数据不一致） | A4.2 运行时探针（与 P1-RC-010 测试补充协同） |
| SP-3 | **看板行级权限运行时过滤有效性**：P1-MA2-093 resolved R1.29（`ErpOrgIsolationQueryTransformer` 全局 IQueryTransformer），但 mfg dashboard 经 IDaoProvider 直访（`daoProvider.daoFor(...).findAllByQuery(q)` / `ormTemplate.findListByQuery(q)`）路径下，全局 QueryTransformer 是否实际注入 orgId 过滤（vs CrudBizModel 标准管道）需运行时确认 | 多组织部署 + 用户归属 orgA 但查全量工单 | A4.1 运行时探针（与 A1.7 SP-4 / A2.18 successor 同根因） |
| SP-4 | **召回报告 degraded 模式运行时业务覆盖**：RecallReport.degraded=true 仅返回受影响成品批次集合，实际召回场景下"受影响成品批次集合"是否满足运营召回需求（位置/去向查询缺失的业务影响）需运行时确认 | 实际召回事件触发 + inventory 域暴露按批次的位置/去向查询方法集时（successor 触发条件） | A4.2 运行时探针（与 inventory 域能力演进挂钩） |

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 汇总如下：

| 规则 | 描述 | baseline | actual | 状态 |
|------|------|---------|--------|------|
| R1a | dao().saveEntity (BizModel) | 0 | 0 | ✅ |
| R1b | dao().updateEntity (BizModel) | 0 | 0 | ✅ |
| R1c | dao().getEntityById (BizModel) | 0 | 0 | ✅ |
| R1d | dao().findAllByQuery (BizModel) | 14 | 14 | ✅ |
| R2a | BizModel daoFor(ErpMd*) | 34 | 34 | ✅ |
| R2b | BizModel daoFor(Erp*) 跨域 | 229 | 229 | ✅ |
| R2c | 全生产代码 daoFor() 总量 | 1382 | 1382 | ✅ |
| R2d | Processor daoFor(ErpMd*) | 34 | 34 | ✅ |
| R3 | new Erp*() 构造实体 | 5 | 5 | ✅ |
| R5 | @Inject private | 0 | 0 | ✅ |
| R6 | @Transactional in BizModel | 2 | 2 | ✅ |
| R10 | REQUIRES_NEW 事务 | 6 | 6 | ✅ |
| R12a | 共享内核 import ErpFinBusinessType | 69 | 69 | ✅ |
| R12b | 共享内核 import PostingEvent | 66 | 66 | ✅ |
| R12c | 共享内核 import AcctSchemaResolver | 40 | 40 | ✅（基线经 plan 2026-07-31-1705-2 裁决上调 38→40） |

  **本审计无生产代码变更**（纯只读审计——读代码/测试/报告，不改代码/ORM/api.xml/view.xml/真相源），checker 无回归风险，actual ≤ baseline 全绿。**不以 checker 脚本退出码 0 作为门控通过依据**（区分纯 reporter 退出码恒 0 vs CI workflow `.github/workflows/compliance.yml` 的 `Enforce baseline gate` step 真正门控）。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（UC-MFG-11 ②阈值 → `P2-RC-009` 新建 / UC-MFG-11 ③行级权限 → `P1-MA2-093` 复用 / UC-MFG-12 P1-MA4-007/009/010/011/012 HEAD 复核 / UC-MFG-13 ⑫测试 → `P1-RC-010` 新建）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6.1 比对表），**无未经比对直接新建的 finding**（P1-RC-010/P2-RC-009 经比对 6+ 候选既有 finding 均不同控制点/不同维度后新建）。
- [x] **真相源冻结条款遵守**（§9）：本审计未修改任何真相源（product-scope / mfg use-cases / variance-analysis.md / batch-genealogy.md 需求契约段落）；分歧（UC-MFG-13 recallReport 降级 / UC-MFG-11 阈值非 config）记入报告，不直改真相源。

---

## 9. 与 MA2/MA4 报告差异增量声明

> 依方法论 §6 段落 9 + §去重协议，本报告与既有 MA2/MA4 报告的差异增量已在报告开头（§9 前置声明）+ §1-§8 全文体现。此处汇总：

**复用结论**（不重审）：
- **A4.2a**（`2026-07-29-0024-arm-ma4-mfg-work-order-bom-code-quality.md`）：ProductionVarianceDispatcher 调用点 + 完工触发编排（pre-R6.1 `reportCompletion:227-239`）+ P1-MA4-007/008/009 finding。本切片 HEAD `c9e87bbc4` 复核 R6.1 per-mutation 拆分后位置 `ErpMfgWorkOrderReportCompletionProcessor:86-102` 维持 R1.16 G3 错误分级 + 告警派发落地。
- **A4.2b**（`2026-07-29-0024-arm-ma4-mfg-mrp-quality-code-quality.md`）：ProductionVarianceCalculator 6 类算术 PASS（A4.2b:54）+ dispatchVarianceAlertIfOverThreshold 错误传播降级 PASS（A4.2b:55）+ BatchGenealogyWriter 基因链写入幂等一致性 PASS（A4.2b:56）+ 重算幂等四步链（红冲→删旧→重算→派发 + 一致不变量）+ P1-MA4-010/011/012 finding。
- **A2.6a**（`2026-07-28-0109-arm-ma2-mfg-work-order-jobcard-state-machine.md`）：完工 COMPLETED 触发差异/批次写入的触发点 pass 结论。
- **A2.18**（`2026-07-28-1510-arm-ma2-multi-company-isolation.md`）：P1-MA2-093 orgId 查询隔离全仓未落地，`:99-101` 显式列 `ErpMfgDashboardBizModel` 为 11 dashboard 直访之一；R1.29 resolved（全局 IQueryTransformer）。

**本切片补的需求视角差异**（不复审代码质量/状态机维度）：
1. **UC-MFG-11 看板 KPI/阈值/权限符合性**：①KPI 实时聚合接受 + ②预警阈值非 config（P2-RC-009 新建）+ ③行级权限复用 P1-MA2-093（resolved R1.29）。
2. **UC-MFG-12 6 类差异完整性与 PPV 归属**：④完工触发 + ⑤6 类逐条写 + ⑥PPV 归采购不重复（InvPostingDispatcher.dispatchPurchasePriceVariance 跨域职责分离）+ ⑦多维聚合报表——全接受。
3. **UC-MFG-13 追溯链完整性与召回报告降级**：⑧⑨⑩⑪ 追溯链全接受 + ⑫召回报告降级（§4 三判据核验：L1 ⑫仅要求"识别"，降级版满足，位置/去向为增强 successor 归 inventory 域演进）+ 测试有效性 P1-RC-010（testRecallReport 仅冒烟 + best-effort 写失败无测试）。
4. **resolved finding HEAD 复核**：5/5（P1-MA4-007/009/010/011/012）在 HEAD `c9e87bbc4` 全部已落地无回退。其中 P1-MA4-007 经 R1.16 实施 G3 错误分级 + 告警派发闭环落地（`ErpMfgWorkOrderReportCompletionProcessor:86-102` + `ErpMfgWorkOrderProcessor.dispatchVarianceFailureAlert:150-167` 派发 `IErpSysNotificationBiz.notify("mfg.production-variance-posting-failure")` + owner doc `state-machine.md:180` 同步记录）+ P1-MA4-009/011 经 R2.11 补强测试，闭合 UC-MFG-12 完工触发差异过账链的运行时可见性。BatchGenealogyWriter→inv 写 O-4 豁免经 plan 2026-07-29-2225-1 登记于 `data-dependency-matrix.md §9`。

**整体 Verdict**：⚠️(P1) — 3 UC 中 UC-MFG-12 接受（4 断言全对齐 + 5 resolved finding HEAD 复核维持）、UC-MFG-13 接受 on 功能（⑧⑨⑩⑪⑫功能全对齐，⑫测试有效性 P1 新建 P1-RC-010）、UC-MFG-11 接受 on ①（②阈值 P2-RC-009 + ③行级权限复用 P1-MA2-093），零 P0。本切片主要价值 = **resolved finding HEAD 复核**（5/5 维持，含 P1-MA4-007 完工差异吞咽致业财悬挂的修复落地确认 R1.16 + 测试补强 R2.11，闭合 UC-MFG-12 完工触发差异过账链运行时可见性）+ **UC-MFG-12 6 类差异完整性与 PPV 归属的需求契约裁决**（接受，跨域职责分离避免重复计入经 InvPostingDispatcher.dispatchPurchasePriceVariance 实证）+ **UC-MFG-13 召回报告降级的 §4 三判据核验**（降级版满足 L1"识别"，位置/去向为增强 successor）+ **UC-MFG-11 看板阈值/权限的需求契约投影**（P2-RC-009 + 复用 P1-MA2-093）+ **4 项静态存疑点**（SP-1/SP-2/SP-3/SP-4 交 MA4 运行时展开）。
