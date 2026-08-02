# MA4 manufacturing 代码质量审计 — MRP/DRP 引擎 / 质量集成与 NCR（A4.2b）

> Audit Status: closed
> Plan: `docs/plans/2026-07-29-0024-2-audit-remediation-ma4-mfg-mrp-quality-code-quality.md`
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA4（工作项 A4.2b，S 级拆分 2/2）
> Skill: `docs/skills/code-quality-audit-prompt.md`（7 重点领域）
> Related: A4.2a `docs/audits/2026-07-29-0024-arm-ma4-mfg-work-order-bom-code-quality.md`（工单/BOM 拆分 1/2，不同功能模块）/ A2.6b `docs/audits/2026-07-28-0109-arm-ma2-mfg-mrp-bom-state-machine.md`（业务正确性，已知 finding 输入）/ A3.4（owner-doc drift 输入）
> Verdict Date: 2026-07-29

## 范围

manufacturing 域 MRP/DRP 引擎 + 质量集成与 NCR（mfg 侧跨域 Facade 调用点）+ 关联计算链路（成本卷积/生产差异/批次基因/CRP/预测消费/委外过账）的**代码实现质量**审计（非业务正确性状态机——归 A2.6b；非 owner-doc drift——归 A3.4）。

审计对象代码（`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/`，实时仓库核实）：

- **MRP/DRP 引擎**：`mrp/MrpEngine`（净需求/低层码递归）+ `mrp/MrpReleaseService`（采购/委外/工单三路径释放）+ `mrp/DemandAggregator`（销售/安全库存/预测需求整合）
- **仿真引擎**：`simulation/SimulationMrpEngine`（What-If fork）+ `simulation/ErpMfgSimulationParamResolver`（参数覆盖）+ `simulation/SimulationVersionComparator`
- **关联计算引擎**：`costing/CostRollupService`（成本卷积递归 + 记忆化 + 成环检测）+ `costing/ProductionVarianceCalculator`（6 类差异 + 阈值预警 dispatchVarianceAlertIfOverThreshold）
- **差异/委外过账**：`posting/ProductionVarianceDispatcher` + `posting/ProductionVarianceAcctDocProvider` + `posting/SubcontractPostingDispatcher` + `posting/SubcontractIssueAcctDocProvider` + `posting/SubcontractReceiptAcctDocProvider` + `posting/SubcontractFeeAcctDocProvider` + `posting/MfgSubcontractReversalListener` + `posting/MfgPostingExecutor`
- **批次基因链**：`genealogy/BatchGenealogyTracer`（forward/backward/traceChain）+ `genealogy/BatchGenealogyWriter`（完工写入 + 自动建批）
- **CRP**：`crp/CrpLoadCalculator`（双源负荷 + 产能 + overload）+ `job/ErpMfgCrpRunJob`（cron）
- **质量集成跨域 Facade 调用点**：`processor/ErpMfgWorkOrderProcessor:194-197` 经 `IErpQaInspectionBiz` + `InspectionTrigger.enforceGate`（门控逻辑实现质量归 A4.2a；本审计仅复核 mfg 侧 Facade 调用合规性）

owner docs：`docs/design/manufacturing/{mrp,simulation-engine,variance-analysis,batch-genealogy,subcontracting,crp,material-reservation}.md`。

## 7 重点领域审查结果

### 1. 架构和边界完整性 — ⚠️ 有缺陷（P1-MA4-012）

跨域访问合规性逐站核查：

- **跨域只读 daoFor 绕 I\*Biz**（P1-MA4-012，同 P1-MA1-022/P1-MA4-008 根因在 MRP/成本/基因/委外投影）：
  - `MrpEngine.java:192,203,213` daoFor(ErpMdMaterial/ErpInvStockBalance) mfg→md/inv 只读
  - `DemandAggregator.java:86,87,125,238` daoFor(ErpSalOrder/ErpSalOrderLine/ErpMdMaterial/ErpInvStockBalance) mfg→sal/md/inv 只读
  - `MrpReleaseService.java:137,152` daoFor(ErpPurOrder/ErpPurOrderLine) mfg→pur **写**（O-4 豁免已登记 posting-exemptions.md §MrpReleaseService 采购单段；委外单段登记缺失 → P1-MA2-038 已登记）
  - `SimulationMrpEngine.java:260,394,405,415` + `SimulationVersionComparator.java:170` daoFor(ErpMdMaterial/ErpInvStockBalance) mfg→md/inv 只读
  - `CostRollupService.java:140,299` daoFor(ErpMdMaterial/ErpMdMaterialSku) mfg→md 只读
  - `SubcontractPostingDispatcher.java:259,270` daoFor(ErpInvStockMove/ErpInvStockLedger) mfg→inv 只读（+ `:283` 经 AcctSchemaResolver daoFor(ErpMdAcctSchema)）
  - `BatchGenealogyWriter.java:266` daoFor(ErpInvBatch) mfg→inv **写**（自动建批 ensureOutputLot + 累加总量）—— O-4 类豁免**未登记** posting-exemptions.md（与 P1-MA2-038 同型半治理，归 P1-MA4-012 一并裁决，不在本审计单独开新 P1）
  - `ProductionVarianceDispatcher.java:208` 经 AcctSchemaResolver daoFor(ErpMdAcctSchema)
- **差异过账跨域写经 Facade 合规**：`ProductionVarianceDispatcher:107` + `SubcontractPostingDispatcher:133` 经 `MfgPostingExecutor` → `IErpFinVoucherBiz.post/reverse` Facade（`MfgPostingExecutor.java:32,40`），production 代码无 daoFor(ErpFin\*) 直写。**PASS**
- **CRP 跨域 APS 经 SPI 合规**：`CrpLoadCalculator.java:80,342` 经 `IErpApsLoadSourceProvider` SPI（aps-service 经 ioc:collect-beans 注入），无 daoFor(ErpAps\*) 直耦合；manufacturing 域内只读（WorkOrder/RoutingOperation/Workcenter/Calendar/Capacity）。**PASS**
- **质量集成跨域 Facade 合规**：`ErpMfgWorkOrderProcessor:194` 经 `IErpQaInspectionBiz` + `InspectionTrigger.enforceGate` Facade 触发质检，无 daoFor(ErpQa\*) 直写（enforceGate 实现质量归 A4.2a）。**PASS**

**裁决**：跨域写经 Facade（差异/委外过账、质检触发）合规；跨域只读 daoFor 是 P1-MA1-022 同根因投影（P1-MA4-012 登记，MR1 一并裁决不重复计入 MR2）。

### 2. 核心实现正确性 — PASS（无活跃算术/事务/幂等缺陷；1 项 P2 可维护性注记）

- **MrpEngine 净需求算术**（`MrpEngine.java:112-118`）：net = gross − available − scheduled(恒 0)；负值归零。算术正确。`scheduled` 恒 0 是已知简化（已开放 PO/WO 计划接收未抵扣毛需求——MrpEngine javadoc Non-Goal 未显式列举但属功能范围简化，非算术错误），`scheduledReceipt` 列恒写 0 数据略误导 → 归 P2-MA4-005 可维护性注记。
- **MrpEngine 低层码递归终止**（`:102-157`）：`path.contains(materialId)` 兜底防环（:108-110）+ BomExpander.explode 显式环检测（:148）+ path add/remove 回溯（:146-155）。递归终止正确。**PASS**
- **MrpReleaseService 多路径释放事务边界与幂等**（`MrpReleaseService.java`）：三路径（purchase/work/subcontract）均在 `@BizMutation` 入口事务内（BizModel 层），`requireReleasable:115-127` 幂等守卫（isFirmed 拒绝 ERR_MRP_LINE_ALREADY_FIRMED + orderType 匹配守卫），`advancePlanToFirmedIfComplete:218-236` 全行 firmed 时推进 FIRMED。幂等正确。**PASS**
- **SimulationMrpEngine fork 一致性**（`SimulationMrpEngine.java`）：fork processMaterial/lotSize/mfgLeadDays/purLeadDays/availableQuantity/topDemandsByMaterial 对齐 MrpEngine，覆盖经 paramResolver；javadoc:51 显式声明「任何 MrpEngine 算法变更须同步本类」。DRY 违反是 Decision E2 显式接受的残留风险 → 归 P2-MA4-005 可维护性注记。
- **CostRollupService 卷积递归终止与成环检测**（`CostRollupService.java:130-180`）：记忆化 `computed` 缓存（:131-134）+ path 成环检测（:135-139 抛 ERR_BOM_CYCLE）+ path add/remove 回溯（:158-176）。钻石型 DAG 经缓存避免重复计算（缓存于方法末尾 :178 写入， siblings 复用）。成环检测正确。**PASS**（CostRollupService 自身 ERR_BOM_CYCLE 路径无直接测试 → 归 P1-MA4-011 测试有效性）
- **ProductionVarianceCalculator 6 类差异算术**（`ProductionVarianceCalculator.java:128-203`）：逐类复核——材料用量（stdRollup×completed vs wo.materialCost）/ 人工效率（actualMins/60×stdRate vs stdLabor×completed）/ 人工费率残差（actLabor − actualAtStdRate）/ 制造费用 / 产量（(completed−planned)×stdUnit）/ 委外（仅非零生成行）。`divideSafe:447-452` + `:166 actualMins.signum()==0` 防除零。算术正确（TestErpMfgProductionVariance 行级数值断言印证）。**PASS**
- **dispatchVarianceAlertIfOverThreshold 错误传播**（`:225-261`）：`try { notificationBiz.notify } catch (Exception) { LOG.warn }` 降级不阻断主计算——观察侧职责解耦正确。**PASS**
- **BatchGenealogyWriter 基因链写入幂等一致性**（`BatchGenealogyWriter.java`）：`writeOnCompletion:64-76` best-effort 包裹（config-gated，失败仅 LOG.error 不阻断完工）；`ensureOutputLot:149-170` 幂等（按 batchNo 反查，存在则累加总量）；`usedInputLots:113-119` 去重同一输入批次。幂等正确。**PASS**
- **CrpLoadCalculator 负荷算术**（`CrpLoadCalculator.java`）：`distributeByWorkOrder:256-309` 按区间日均匀分派 + setup 首日；`distributeByApsSlots:194-250` 按排程时段跨日逐日累加（Duration.between 精确分钟/60）+ 窗口截断；`computeLoadRate:535-540` + `isOverloaded:542-550`（含 capacity=0 时 9999 哨兵）。算术正确。**PASS**
- **Subcontract/ProductionVariance posting dispatcher tryPost 吞咽**：
  - `ProductionVarianceDispatcher.dispatchIfApplicable:106-117` + `reverseIfExists:148-153` try/catch 吞咽过账/红冲失败保持 posted=false —— **P1-MA4-007 已登记**（A4.2a，完工编排层 + 差异 dispatcher），复核「如登记」无升级。
  - `SubcontractPostingDispatcher.dispatchFeePosting:132-143` try/catch 吞咽加工费过账失败保持 posted=false —— 同型。
  - `SubcontractPostingDispatcher.dispatchIssuePosting/dispatchReceiptPosting` → `postEvent:146-156` try/catch 吞咽发料/收货过账失败，**但 issue/receipt 路径无 posted 标志追踪**（仅 fee 路径 markPosted）—— 发料/收货过账失败后库存已移动但 GL 缺凭证，无 posted=false 记录、无重试入口 → **新 P1-MA4-010**（业财悬挂闭环缺失同型根因，MA2 审状态机/A4.2a 审工单 ManufacturingIssuePostingDispatcher/A4.1b 审期间编排均未覆盖委外 issue/receipt dispatcher）。
- **Forecast 消费去重**（`DemandAggregator.collectForecastDemands:160-230`）：`clearSynthesized:257-271` 每次重算前清除非 MANUAL 合成行（含 FORECAST），按物料聚合 forecastQty。CONSUMED 不回写是 P1-MA2-036 已登记 dict 死状态，非去重缺陷。**PASS（如登记）**
- **cron job 并发**（`ErpMfgCrpRunJob`）：`CrpLoadCalculator.calculateLoad:95-131` 清区间再写——并发执行同窗口 last-write-wins（幂等无重复副作用，较 P1-MA2-086 其它 job 如 sla-scan 轻）。**P1-MA2-086 已登记**，复核「如登记」无升级（CRP 段属该 family 中较轻者）。

**裁决**：核心算术/递归终止/幂等/事务边界均正确；唯一代码层缺陷是 SubcontractPostingDispatcher issue/receipt 过账失败吞咽无闭环（P1-MA4-010）；scheduledReceipt 恒 0 + Simulation fork DRY 属 P2 可维护性注记。

### 3. 类型和契约质量 — PASS（无类型不匹配/契约漂移）

- **MrpReleaseService 三路径释放返回契约一致**：均返回 `String billCode`（采购 `RELEASE_PO_CODE_PREFIX+lineId` / 工单 `RELEASE_WO_CODE_PREFIX+lineId` / 委外 `RELEASE_SUBCONTRACT_CODE_PREFIX+lineId`），回写 convertedBillCode。**PASS**
- **差异 6 类 BigDecimal 类型安全**（`ProductionVarianceCalculator`）：全程 BigDecimal + `scale:443-445`（4 位 HALF_UP 统一）+ `divideSafe` 防除零；无 double 算术混入（仅 `MrpEngine.mfgLeadDays:187` + `CrpLoadCalculator` threshold 用 double 换算天数/阈值，属配置换算非金额计算）。**PASS**
- **基因链 trace 返回结构契约**：`forwardTrace/backwardTrace` 返回 `List<ErpMfgBatchGenealogy>`，`traceChain` 同类型 + maxDepth/direction 参数。契约清晰。**PASS**
- **仿真引擎 What-If 入参契约**：`runSimulation(Long scenarioId)` / `promoteToFormalPlan(Long scenarioVersionId)` 单参；paramResolver 经 `(scenarioId, materialId, paramType)` 三元组解析。契约清晰。**PASS**

**裁决**：本领域无类型不匹配或契约漂移。

### 4. 错误处理和操作安全 — ⚠️ 有缺陷（P1-MA4-010 同 site）

- **异常规范化**：MRP/质量/计算链路主路径异常均扩展 NopException + ErrorCode（`erp.err.mfg.*`）—— MrpEngine ERR_MRP_INVALID_PLAN_STATUS/ERR_MRP_PLAN_NOT_FOUND / MrpReleaseService ERR_MRP_LINE_ALREADY_FIRMED/ERR_MRP_RELEASE_UNSUPPORTED_ORDER_TYPE/ERR_SUBCONTRACT_RELEASE_MISSING_SUPPLIER / CostRollupService ERR_BOM_CYCLE/ERR_ROLLUP_BASE_COST_MISSING/ERR_BOM_NOT_FOUND / ProductionVarianceCalculator ERR_VARIANCE_NO_STANDARD_COST/ERR_WORK_ORDER_NOT_FOUND / BatchGenealogyTracer ERR_MFG_GENEALOGY_INVALID_DIRECTION/ERR_MFG_GENEALOGY_MAX_DEPTH_EXCEEDED / CrpLoadCalculator ERR_CRP_PERIOD_INVALID / SimulationMrpEngine ERR_MFG_SIMULATION_*。**PASS**
- **差异公式与 owner doc 一致性（P1-MA3-043 复核）**：code 6 类（材料用量/人工效率/人工费率/制造费用/产量/委外），`variance-analysis.md:62-68` 正确列全 6 类；`use-cases.md UC-MFG-12` 列 4 公式错误——**P1-MA3-043 已登记**（doc drift，MR2），复核「如登记」无代码层缺陷。
- **预警实现与 doc Deferred 标注（P1-MA3-045 复核）**：code `dispatchVarianceAlertIfOverThreshold:225-261` 已实现，`variance-analysis.md:89` 标 Deferred + `:113-115` 配置表未列 variance-alert-enabled/threshold——**P1-MA3-045 已登记**（doc drift，MR2），复核「如登记」无代码层缺陷。
- **过账失败错误传播**：SubcontractPostingDispatcher issue/receipt/fee + ProductionVarianceDispatcher dispatch/reverse 均 try/catch 吞咽 —— fee/variance 路径保留 posted=false 可观测；**issue/receipt 路径无 posted 追踪**（P1-MA4-010）。
- **裸异常**：无裸 throw new RuntimeException；config 解析（`isSubcontractReleaseEnabled:266-276` / `resolveVarianceAlertThreshold:268-278` / `BatchGenealogyTracer.defaultMaxDepth:120-132`）catch NumberFormatException 兜底默认值。**PASS**

**裁决**：异常规范化扎实；唯一缺陷是 issue/receipt 过账失败吞咽无闭环（P1-MA4-010）；MA3 drift 复核无升级。

### 5. 测试有效性 — ⚠️ 有缺陷（P1-MA4-011）

抽样 MRP/质量/计算相关测试逐项核查断言强度与异常路径覆盖：

- **TestErpMfgProductionVariance**（强）：行级 standardAmount/actualAmount/varianceAmount 数值断言 + 凭证行 dcDirection/debitAmount/creditAmount 断言 + 负向（无标准成本 ERR_VARIANCE_NO_STANDARD_COST / 非 COMPLETED 拒绝）+ 幂等（重算行数不变）+ 委外差异 + 过账 posted 标志。**PASS**
- **TestErpMfgBatchGenealogy**（强）：基因行 inputLotId/inputQty/outputQty/outputLotId/lotStatus/isInputConsumed 断言 + forwardTrace/backwardTrace + traceChain 多级 + 环路防护 + maxDepth ErrorCode + 非法方向 ErrorCode + recallReport。**PASS**
- **TestErpMfgCostRollup**（强）：单位成本四要素分解断言 + base cost missing throws + overhead 两模式 + subcontract aggregation enabled/disabled + 四要素和不变量。**PASS**
- **TestErpMfgVarianceAlert**（强）：阈值 over/under/disabled 三分支 + ErpSysNotification 行落入 + recipient 匹配。**PASS**
- **TestErpMfgMrpEndToEnd**（强）：SO 需求→MRP 运行→制造件展开+采购件净需求→释放采购单/工单→isFirmed/convertedBillCode 回写→FIRMED + 幂等拒绝 + 类型不匹配拒绝。**PASS**
- **TestErpMfgSubcontracting**（中）：委外全链 + 加工费凭证行断言（Dr 委外物资/Cr 应付账款 借贷平衡）+ 非法迁移拒绝 + MRP 委外释放 + 幂等拒绝。**PASS（主路径）**

**测试空洞（P1-MA4-011）**：
- (a) **多币种凭证行级断言缺失**：差异过账 + 委外加工费过账凭证行均未校验 amountSource/amountFunctional/exchangeRate/debitAmount/creditAmount（所有测试单币种 CURRENCY_ID 固定 + exchangeRate 恒 ONE），多币种 bug（P1-MA3-039 同型）对 mfg 差异/委外测试不可见——与 P1-MA4-009（工单/BOM）+ A4.1a P1-MA4-002 + A4.1b P1-MA4-005 同族，MR2 协同。
- (b) **过账失败悬挂零覆盖**：SubcontractPostingDispatcher（issue/receipt/fee）+ ProductionVarianceDispatcher 的 try/catch 吞咽路径（P1-MA4-007/010）无测试触发（无 mock post 抛异常→断言 posted=false / issue/receipt 无追踪）——闭合 P1-MA4-010 测试可见性。
- (c) **CostRollupService ERR_BOM_CYCLE 路径无直接测试**：BomExpander 成环检测有 TestErpMfgBomExplosion.testCycleDetection 覆盖，但 CostRollupService.computeUnit 自有 path 成环检测（:135-139，不经 BomExpander.explode）无直接测试——成环算术路径回归保护缺口。
- (d) **MrpEngine 净需求 scheduledReceipt 恒 0 路径**无测试断言该列恒 0（数据契约无回归保护）。
- (e) **CRP overload 阈值边界 + APS fallback** 覆盖薄（TestErpMfgCrpLoadSource 存在但 APS 模式 fallback 断言较轻）。

**裁决**：主路径测试断言强度高（数值/凭证行/负向/幂等）；空洞集中在多币种凭证行 + 业财异常悬挂 + CostRollup 成环路径——P1-MA4-011（MR2，与 P1-MA4-009 + A5.1 互补不重叠）。

### 6. 可维护性和未来变更风险 — ⚠️ 有 P2 watch-only 风险（P2-MA4-005）

- **SimulationMrpEngine fork DRY 漂移风险**：`SimulationMrpEngine` 复制 MrpEngine ~150 行核心算法（processMaterial/lotSize/mfgLeadDays/purLeadDays/availableQuantity/topDemandsByMaterial），javadoc:51 显式声明须手工同步。MrpEngine 算法变更若漏同步 → 仿真与正式结果分叉（Decision E2 显式接受的残留风险，bounded by Non-Goals + 头部注释）。
- **MrpEngine scheduledReceipt 恒 0**：`MrpEngine.java:113 BigDecimal scheduled = BigDecimal.ZERO;` + line.setScheduledReceipt(scheduled)——计划接收未抵扣毛需求（功能范围简化），scheduledReceipt 列恒 0 数据略误导。
- **委外 AcctDocProvider 系列 4 个对称性**：SubcontractIssue/Receipt/Fee + ProductionVariance 4 个 Provider 结构对称（readAmount/readString/resolveSubcontractSubjectCode/fact 重复模式），可维护性可接受（范式复用，非缺陷）。
- **预留子系统未实现 owner doc 维护风险**（P1-MA3-042 复核）：material-reservation.md 288 行描述的预留子系统未实现，KitAvailabilityChecker 只读——MR2 改写 doc（owner-doc drift，非代码缺陷）。
- **MfgSubcontractReversalListener 粗粒度回滚**：`rollbackSubcontractOrder:61-75` 任一段（ISSUE/RECEIPT/FEE）红冲均置整单 docStatus=CANCELLED；若仅 ISSUE 凭证被红冲而 RECEIPT/FEE 仍有效，整单被取消（粗粒度）。镜像 PurReversalListener 范式 + posted==true 前置 + 三段共用单据的设计接受，但回滚粒度粗——P2 watch-only。
- **dispatchVarianceAlertIfOverThreshold productCode 占位**：`ProductionVarianceCalculator.java:251 ctx.put("productCode", String.valueOf(wo.getProductId()))`——productCode 用 productId 数字占位而非物料编码（通知模板 `${productCode}` 显示数字非编码），通知可读性差——P2 watch-only。

**裁决**：P2-MA4-005 合并 5 项可维护性热点（fork DRY / scheduledReceipt / 预留 doc / 委外回滚粒度 / productCode 占位），均 watch-only 不阻断。

### 7. 自动化和防护覆盖 — ⚠️ 有 P2 watch-only 缺口（P2-MA4-005 同条）

- **compliance checker 规则守护**：MRP/质量/计算链路无域专用 compliance checker 规则（与 A4.2a P2-MA4-004 同型，归 MA4 通用防护层）。
- **测试门控**：主路径算术（差异/卷积/基因/CRP）有强数值断言门控；缺口在业财异常悬挂（P1-MA4-010/011）+ 多币种（P1-MA4-011）+ CostRollup 成环——归 P1-MA4-011 + P2-MA4-005。

**裁决**：防护缺口与测试有效性（领域 5）+ 可维护性（领域 6）同根因，合并 P2-MA4-005 watch-only。

## MA1/MA2/MA3 已知 finding 运行时复核

| Finding | 注册审计 | 运行时复核结论 |
|---------|---------|---------------|
| P1-MA1-022 | MA1 跨域只读 | **如登记**——MRP/成本/基因/委外链路 daoFor(ErpInv\*/ErpMd\*/ErpSal\*) 只读投影现正式枚举为 P1-MA4-012（MR1 一并裁决），无行为升级 |
| P1-MA2-036 | A2.6b 状态机 | **如登记**——MrpEngine/DemandAggregator/ForecastBizModel 确认无 setStatus(MRP_STATUS_CANCELLED)/setStatus(FORECAST_STATUS_CONSUMED) 写入；dict 死状态维持（MR1） |
| P1-MA2-037 | A2.6b 状态机 | **如登记**——mrp.md §建议单释放 "RELEASED" 文字 vs 实现 isFirmed 布尔 drift（owner-doc，MR1），无代码层缺陷 |
| P1-MA2-038 | A2.6b 状态机 | **如登记**——MrpReleaseService.releaseToSubcontractOrder:199-201 config-gated 生成 APPROVED 委外单，posting-exemptions.md §MrpReleaseService 委外段登记缺失（治理，MR1），无代码层缺陷 |
| P1-MA2-086 | A2.17 并发 | **如登记**——ErpMfgCrpRunJob cron 并发，CrpLoadCalculator.calculateLoad 清区间再写 last-write-wins（该 family 中较轻者），维持 MA2 并发 family（MR1） |
| P1-MA3-042 | A3.4 owner-doc drift | **如登记**——material-reservation.md 预留子系统未实现（doc drift，MR2），KitAvailabilityChecker 只读，MRP 范围无代码层缺陷 |
| P1-MA3-043 | A3.4 owner-doc drift | **如登记**——use-cases.md UC-MFG-12 列 4 公式 vs code 6 类（doc drift，MR2），variance-analysis.md:62-68 正确，无代码层缺陷 |
| P1-MA3-045 | A3.4 owner-doc drift | **如登记**——dispatchVarianceAlertIfOverThreshold 已实现，variance-analysis.md:89 标 Deferred + 配置表未列（doc drift，MR2），无代码层缺陷 |
| Subcontract/ProductionVariance posting dispatcher tryPost 吞咽 | A2.6b 交接 | **发现新相邻代码路径缺陷 P1-MA4-010**——SubcontractPostingDispatcher.dispatchIssuePosting/dispatchReceiptPosting→postEvent:146-156 吞咽过账失败**无 posted 追踪**（fee 路径有 posted=false 保留）。MA2 审状态机/A4.2a 审工单 ManufacturingIssuePostingDispatcher + ProductionVarianceDispatcher/A4.1b 审期间编排均未覆盖委外 issue/receipt dispatcher |

## P0-P3 finding 清单（按严重性排序）

### P1（blocker/major，目标 MR1 代码类 / MR2 测试类）

| ID | 缺陷 | 文件:行 | 严重性 | 影响 | 目标 MR | 状态 |
|----|------|--------|--------|------|---------|------|
| **P1-MA4-010** | **委外 issue/receipt 过账失败吞咽无 posted 追踪/闭环（业财悬挂）**：`SubcontractPostingDispatcher.dispatchIssuePosting:78-94` + `dispatchReceiptPosting:99-114` 经 `postEvent:146-156` `catch(Exception){ LOG.warn/error }` 吞咽过账失败，**issue/receipt 路径无 posted 标志**（仅 `dispatchFeePosting:124-143` 有 posted=true/false 追踪）。发料/收货过账失败时库存已移动（issueMaterials/receiveFinished 已生成 StockMove 并 DONE）但 GL 缺 SUBCONTRACT_ISSUE/RECEIPT 凭证，无 posted=false 记录、无重试入口、DeferredPostingSweepJob（finance 域）不扫描委外单。期末结账前置检查仅扫 finance 异常工作台，间接兜底失效。config=true（业务要求委外过账）+ 永久性失败（科目/模板配置错误）→ GL 缺委外发料/收货凭证（原材料/委外物资/产成品余额漂移），COMPLETED 后不可自动补救。与 P1-MA4-007（完工编排层差异吞咽）/ P1-MA4-004（期间编排吞咽）同型根因（编排层过账失败吞咽致业财悬挂），但 MA2 审状态机/A4.2a 审工单 ManufacturingIssuePostingDispatcher（工单领料，不同 dispatcher）/A4.1b 审期间编排均未覆盖委外 issue/receipt dispatcher。非 P0：需 config=true + 永久性失败前置 + LOG 可见性。修复方式：MR1 裁决——方案 A issue/receipt 段引入段级 posted 追踪字段（或复用 postedStatus dict 多段）+ 失败进 ErpFinPostingException 异常工作台由期末前置检查兜底；方案 B 三段统一 posted 语义（仅全三段成功置 posted=true，任一段失败 posted=false + 告警）。触及会计保护区域，修复须独立 plan-audit + 人工确认 | `SubcontractPostingDispatcher.java:78-94,99-114,146-156` | major | 业财悬挂（GL 缺委外发料/收货凭证）+ 无闭环重试 | MR1 | todo |
| **P1-MA4-011** | **MRP/成本/基因/委外链路测试有效性不足（多币种凭证行级断言缺失 + 业财异常悬挂零覆盖 + CostRollup 成环路径无测试）**：(a) 多币种凭证行级断言缺失——TestErpMfgProductionVariance/TestErpMfgSubcontracting 凭证行均未校验 amountSource/amountFunctional/exchangeRate/debitAmount/creditAmount（所有测试单币种 CURRENCY_ID 固定 + exchangeRate 恒 ONE），多币种 bug（P1-MA3-039 同型）对 mfg 差异/委外过账测试不可见；(b) 业财异常悬挂零覆盖——SubcontractPostingDispatcher（issue/receipt/fee）+ ProductionVarianceDispatcher try/catch 吞咽路径（P1-MA4-007/010）无测试触发（无 mock post 抛异常→断言 posted=false/issue-receipt 无追踪）；(c) CostRollupService ERR_BOM_CYCLE 路径无直接测试（BomExpander 成环有 TestErpMfgBomExplosion.testCycleDetection，但 CostRollupService.computeUnit 自有 path 成环检测 :135-139 不经 BomExpander.explode，无回归保护）；(d) MrpEngine scheduledReceipt 恒 0 无断言；(e) CRP overload 阈值边界 + APS fallback 覆盖薄。修复方式：MR2 补——(1) 差异/委外多币种 E2E（exchangeRate≠ONE + 凭证行级 amountSource≠amountFunctional 断言，闭合 P1-MA3-039 mfg 投影）；(2) dispatcher 过账失败悬挂测试（mock post 抛异常→断言 posted=false + 终态不受影响，闭合 P1-MA4-010 测试可见性）；(3) CostRollup 成环 assertThrows ERR_BOM_CYCLE；(4) CRP overload 边界 + APS fallback。与 A4.2a P1-MA4-009 + A4.1a P1-MA4-002 + A4.1b P1-MA4-005 + A5.1 互补不重叠 | `TestErpMfgProductionVariance.java` / `TestErpMfgSubcontracting.java` / `TestErpMfgCostRollup.java` | major | 多币种 bug + 业财悬挂对测试不可见 | MR2 | todo |
| **P1-MA4-012** | **MRP/成本/基因/委外链路跨域 daoFor(ErpInv\*/ErpMd\*/ErpSal\*) 绕 I\*Biz（同 P1-MA1-022/P1-MA4-008 根因在 MRP/成本/基因/委外投影）**：多站点 mfg→{inv,md,sal} 只读（+ BatchGenealogyWriter→inv 写、MrpReleaseService→pur 写 O-4 豁免）——`MrpEngine:192,203,213` / `DemandAggregator:86,87,125,238` / `SimulationMrpEngine:260,394,405,415` / `SimulationVersionComparator:170` / `CostRollupService:140,299` / `SubcontractPostingDispatcher:259,270`（+ AcctSchemaResolver daoFor ErpMdAcctSchema）/ `BatchGenealogyWriter:266`（inv 写，O-4 豁免未登记）/ `ProductionVarianceDispatcher:208`（AcctSchemaResolver）。违反 AGENTS.md「跨实体访问应通过 I\*Biz 接口」+ data-dependency-matrix.md §5.3。与 P1-MA1-022（9 域）+ P1-MA4-003（finance posting）+ P1-MA4-006（finance budget/arap）+ P1-MA4-008（mfg 工单/BOM）同根因，本批是其在 MRP/成本/基因/委外代码的投影（P1-MA1-022/P1-MA4-008 未显式枚举此投影）。read-only 无活跃数据破坏（BatchGenealogyWriter→inv 写 + MrpReleaseService 委外单 APPROVED 写归 P1-MA2-038 O-4 豁免登记治理）。修复方式：MR1——同 P1-MA1-022 方案 A（inv/md/sal I\*Biz 补便捷只读方法后迁移多站点）或方案 B（永久接受登记 posting-exemptions.md）；BatchGenealogyWriter→inv 写登记 posting-exemptions.md §BatchGenealogyWriter。**不重复计入 MR2**（同 P1-MA1-022/P1-MA4-003/006/008 一并裁决） | `MrpEngine.java:192,203,213` 等（见上） | major | 架构边界治理缺陷（read-only 无数据破坏） | MR1 | todo |

### P2（可维护性/测试差距，watch-only）

| ID | 缺陷 | 文件:行 | 严重性 | 目标 MR | 状态 |
|----|------|--------|--------|---------|------|
| **P2-MA4-005** | **MRP/成本/基因/委外链路可维护性热点合并 5 项**：(a) SimulationMrpEngine fork DRY 漂移风险——复制 MrpEngine ~150 行核心算法，javadoc:51 显式声明须手工同步，MrpEngine 算法变更漏同步 → 仿真与正式结果分叉（Decision E2 接受的残留风险）；(b) MrpEngine scheduledReceipt 恒 0——`:113 scheduled=ZERO` + line.setScheduledReceipt(0)，计划接收未抵扣毛需求，列恒 0 数据略误导；(c) MfgSubcontractReversalListener 粗粒度回滚——`rollbackSubcontractOrder:61-75` 任一段红冲置整单 CANCELLED，仅 ISSUE 红冲时 RECEIPT/FEE 有效整单仍被取消；(d) dispatchVarianceAlertIfOverThreshold productCode 占位——`:251 ctx.put("productCode", String.valueOf(wo.getProductId()))` 用 productId 数字非物料编码，通知 `${productCode}` 显示数字；(e) compliance checker 规则守护缺口——MRP/质量/计算链路无域专用规则（与 P2-MA4-004 同型）。全部 watch-only 不阻断 | `SimulationMrpEngine.java` / `MrpEngine.java:113` / `MfgSubcontractReversalListener.java:61-75` / `ProductionVarianceCalculator.java:251` | minor | 可维护性/未来回归风险 | MR2（doc/code）/ watch | todo |

### P0 / P3

- **P0：无**（代码静态审查 + 测试有效性抽样，无活跃数据破坏路径；跨域 daoFor 只读；cron 并发由 P1-MA2-086 family 守护；委外 issue/receipt 业财悬挂需 config=true + 永久性失败前置 + LOG 可见性，非即时数据破坏）。
- **P3：无独立项**（委外 AcctDocProvider 4 个对称性、forecast sourceBillCode "FORECAST-BATCH" 占位等属可接受范式复用）。

## 裁决

**Verdict: FAIL（有代码实现质量缺陷）**——零 P0。

链路在**算术正确性（MrpEngine 净需求/低层码递归 + CostRollup 卷积记忆化+成环检测 + ProductionVariance 6 类差异 + CrpLoad 双源负荷）/ 事务边界与幂等（MrpReleaseService 三路径释放 + BatchGenealogyWriter 自动建批幂等 + 差异重算红冲-删旧-重算-派发）/ 异常规范化（全 NopException+ErrorCode erp.err.mfg.*）/ 测试主路径断言强度（差异行级数值 + 凭证行 + 基因行 + 卷积四要素 + 阈值三分支）**四面扎实，但**失败恢复闭环（P1-MA4-010 委外 issue/receipt 过账失败吞咽无 posted 追踪）/ 架构边界（P1-MA4-012 MRP/成本/基因/委外跨域 daoFor 投影）/ 测试有效性（P1-MA4-011 多币种凭证行 + 业财异常悬挂零覆盖 + CostRollup 成环路径）**三项 P1 缺陷需 MR1/MR2 修复。

MA1/MA2/MA3 已知 finding 运行时复核 **9 项全部「如登记」无升级**，其中 **Subcontract/ProductionVariance posting dispatcher tryPost 吞咽复核发现相邻代码路径新缺陷 P1-MA4-010**（SubcontractPostingDispatcher issue/receipt 段无 posted 追踪，MA2 审状态机/A4.2a 审工单 dispatcher/A4.1b 审期间编排均未覆盖）。

**3 项新 P1**（P1-MA4-010/011/012，与 MA1/MA2/MA3/A4.1a/A4.1b/A4.2a 已登记 P1 经交叉去重无重复登记——P1-MA4-012 同 P1-MA1-022/P1-MA4-003/006/008 根因在 MRP/成本/基因/委外投影，MR1 一并裁决不重复计入 MR2；P1-MA4-010 与 P1-MA4-004/007 同型根因（编排层过账失败吞咽），MR1 协同；P1-MA4-011 与 A4.2a P1-MA4-009 + A4.1a P1-MA4-002 + A4.1b P1-MA4-005 + A5.1 互补不重叠）+ **1 项新 P2** watch-only（P2-MA4-005 可维护性热点合并 5 项）。

## 剩余风险

- P1-MA4-010 委外 issue/receipt 业财悬挂在 config=false（默认）下不暴露；config=true 需永久性过账失败前置（科目/模板配置错误）才触发，LOG.warn/error 可见性 + 手工补凭证可补救。
- SimulationMrpEngine fork DRY 漂移风险由 javadoc:51 + Non-Goals 限定，MrpEngine 算法变更时须人工同步（CI 无自动检测）。
- manufacturing 代码质量全片（A4.2a 工单/BOM + A4.2b MRP/质量/成本/基因/委外）终态在此收口：**6 P1（A4.2a 3 + A4.2b 3）+ 2 P2（A4.2a 1 + A4.2b 1），零 P0**。
