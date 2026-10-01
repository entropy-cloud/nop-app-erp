---
分析日期: 2026-10-01
类型: 用户故事三维覆盖矩阵（设计 / 实现 / 测试 + 隐性缺口裁定）
方法: 4 路独立子代理按 Epic 分簇实仓核验（owner doc 章节、BizModel/ORM/config 锚点、spec/JUnit 文件名均经 ls/grep/find 实证）+ 执行者对 3 处代理间矛盾点交叉复核（US-LG-01 webhook E2E 存在性、US-FN-02 负向角色配对、US-SO-03 可用量校验默认态）
输入: `docs/requirements/2026-09-22-erp-user-stories.md`（实测 53 条 US）
冲突权威: 实时仓库证据 > 本矩阵 > `2026-09-22-erp-user-story-gap-analysis.md` / `2026-06-30-1200-feature-coverage-matrix.md`
承载: `docs/backlog/user-story-coverage-roadmap.md` USC-01（plan `2026-10-01-0214-1`）
---

# 用户故事三维覆盖矩阵（USC-01 产出）

## 一、结论摘要

| 判定 | 数量 | 说明 |
|------|------|------|
| ✅ 满足 | **40** | 设计+实现+测试三维证据齐备，验收条件核心有运行时断言（JUnit 或 E2E 任一层） |
| 🔶 部分具备 | **12** | 见 §四 分流表：6 项为 roadmap 已裁定的收口项（prod 灰度/移动可达/条码档位）；2 项为测试断言深度缺口；4 项为实现级缺口（其中 2 项升级人工裁决） |
| 🕒 延迟/出界 | **1** | US-PL-06 门户（设计层 future 声明内，无缺口） |
| ❌ 明显缺口 | **0** | 无整故事级缺失；最重为 US-PO-01 链路断裂（🔶 内标 Must 级验收条件未闭环） |

**Must 级真实缺口（需人工裁决归属）**：US-PO-01「RFQ→报价→PO 链路」实现断裂（仅请购→订单有实现，RFQ/报价单为孤立 CRUD+审批实体）——gap analysis 曾记 ✅，本矩阵以实仓证据降档并升级登记（§五）。

**测试面总评**：53 条 US 中 51 条至少一层运行时断言（JUnit 或 E2E）；仅 US-IV-05/US-PL-08（条码/PDA，同源）与 US-PL-03 移动视口维度实现+测试双缺。缺口的主体形态不是「无测试」，而是**收口类**（%prod 默认关）与**断言深度类**（正路径有、分支/对照/行为级断言弱）。

## 二、判定口径

1. ✅/🔶/🕒/❌ 沿用 gap analysis 定义；🔶 扩展含「验收条件部分未闭环（附缺口行）」。
2. **测试证据有效性**：JUnit 运行时断言与浏览器层 E2E 同为运行时证据；「文档提及」「测试文件存在但断言未达验收条件」不算证据。文件存在 + 头部 javadoc 声明为弱指针，本矩阵仅作辅助注记。
3. **config-gate 诚实性**（横切 2 / lessons/14）：机制完整 + 已裁决 opt-in（RC-R1.13 类三级部署范式）= ✅ 注记；%test 已开 %prod 默认关且 roadmap 判「需满足收口」= 🔶。
4. **已裁决覆盖边界不计缺口**：xwf 4 实体浏览器层不可达（plan 2026-07-09-2330-1）、设计内 Deferred（b2b 出站自动化/MFT、拣配、DB 唯一索引）、roadmap §9 Non-Goal（审批金额多级矩阵）、档位语义（US-IV-05 商贸/制造档 Must、轻部署 Could）。

## 三、九 Epic 四列主表（53 条）

> 列说明：设计=owner doc 精确锚点；实现=BizModel/ORM/config 落点；测试=最强运行时断言锚点；缺口=隐性缺口初裁（详分流见 §四；「—」=无缺口，注记见括号）。

### Epic A 主数据（4 条）

| ID | 判定 | 设计证据 | 实现证据 | 测试证据 | 缺口 |
|----|------|---------|---------|---------|------|
| US-MD-01 | ✅ | `master-data/sku-multi-unit.md`（多单位换算 :76/多 barcode :126/条码唯一 :139） | `ErpMdMaterialSkuBizModel.enforceBarcodeUnique`(:321)+`erp-md.sku-barcode-unique` 默认 true（ErpMdConstants :20） | `TestErpMdSkuServices.testBarcodeDuplicateRejected`(:183)+`testFindSkuByBarcode` | —（DB 唯一索引 Deferred 设计已登记；条码 e2e 缺、JUnit 兜底） |
| US-MD-02 | ✅ | `master-data/unified-party-identity.md`（Party 抽象 :26/IErpPartyBiz :75） | `ErpPartyBizModel`+`ErpMdPartnerBizModel.isCodeUnique`(:59) | `TestErpPartyBiz`+`md-party-query.action.spec.ts`+`party-search-picker.visual.spec.ts` | — |
| US-MD-03 | ✅ | `master-data/README.md` 核心业务对象（COA/币种/组织 A3）+`exchange-rate-management.md`+`finance/multiple-accounting-schemas.md` | ErpMdSubject/AcctSchema/Currency/ExchangeRate/Organization 五 BizModel（erp-md-service/entity/） | `TestErpMdSubjectBiz`+`TestErpMdExchangeRateApiClient`+`md-exchange-rate-api.action.spec.ts` | —（组织树环检测为验收外增强） |
| US-MD-04 | 🔶 | `master-data/README.md`（启用/停用 :112/关键属性审核规则 :133） | 软停用 status 列+逻辑删除+`countReferences`/`IErpMd*ReferenceChecker` SPI 实证；**变更审计日志实现无**（`critical-attributes` 键文档声明未接线，grep 零命中） | `TestStub*ReferenceChecker`+`TestErpMdMaterialBiz.testCountReferences` | **变更历史链路实现缺**→USC-03 承接（roadmap 关闭口径：Should·条件触发，软停用/引用约束+审计字段已满足；独立变更历史 UI 不立项除非合规点名） |

### Epic B 采购 P2P（6 条）

| ID | 判定 | 设计证据 | 实现证据 | 测试证据 | 缺口 |
|----|------|---------|---------|---------|------|
| US-PO-01 | 🔶⚠ | `purchase/requisition.md` 状态机（:29 中标转订单/报价转订单规则）+`state-machine.md` | **链路断裂**：仅 `ErpPurRequisitionBizModel.convertToOrder`(:46)；RFQ/Quotation BizModel 无 award/accept/convert 动作（全域 grep 零命中，孤立 CRUD+审批实体） | `TestErpPurRequisitionConvertToOrder`+C01 起点即 PO；RFQ→报价→PO 段零断言 | **「RFQ→报价→PO 链路」验收条件未闭环（Must）→ 归属冲突升级（§五）** |
| US-PO-02 | ✅ | `purchase/use-cases.md` UC-PUR-03 部分入库（:81） | `ErpPurReceiveProcessor.triggerIncomingMove`(:409)+容差守卫 `ERR_RECEIVE_QTY_OVER_TOLERANCE` | `TestErpPurReceiveStockMove`+`TestErpPurReceiveOverReceiptTolerance`（正/负/边界/幂等/回滚）+p2p-chain | — |
| US-PO-03 | ✅ | `purchase/three-way-match.md`（匹配规则 :28/处理策略 :93 含 config 表） | `ThreeWayMatcher`（strict 抛错 :81/:106，`erp-pur.match-strict-mode` 默认 false）+`findThreeWayMatchDiffAlert` 预警看板 | `TestErpPurThreeWayMatch`（strict 拒绝×2/非 strict 放行/容差，6 方法）+`TestErpPurPriceVariancePosting`（9 组） | —（匹配结果可查=看板聚合，逐单明细较弱，注记） |
| US-PO-04 | ✅ | `purchase/state-machine.md` 付款状态机（:210） | `PurAcctDocProvider`（AP_INVOICE/PAYMENT/1404 差异拆分）+`PaymentSettler`+`paidStatus` 列 | `TestErpPurInvoicePosting`+`TestErpPurPaymentSettlement` 族+C01（settle→PAID/AP OPEN）+p2p-chain | — |
| US-PO-05 | 🔶 | `purchase/state-machine.md` 角色与权限（:111 SoD 程序级强制） | SoD：`SoDGuard.assertApproverNotCreator`（common-service）+`ErpPurOrderProcessor`(:345) 调用；**金额阈值门控无实现**（erp-pur.* 12 键无阈值键、订单无 xwf） | `TestErpPurOrderApproval.testSoDCreatorCannotSelfApprove`(:167)+e1-2-purchase | **阈值审批未实现**→roadmap §9 Non-Goal（审批金额多级矩阵 §3.4 裁细则归 wf 配置）→无缺口、仅登记；SoD 轴全链满足 |
| US-PO-06 | ✅ | `purchase/returns.md`+`supplier-evaluation.md`（三档阈值 :35） | `ErpPurReturn(+Line)`+`ErpPurSupplierScorecard(+Criteria/Variable)`+`ScorecardCalculator`+红门 `supplier-scorecard-red-gate` 默认 true（module-meta.yaml） | `TestErpPurReturn*` 族 8 类+`TestErpPurScorecardCalc/Linkage`+`pur-return.action.spec.ts` | —（评分卡浏览器层 e2e 缺、JUnit 兜底，注记） |

### Epic C 销售 O2C（6 条）

| ID | 判定 | 设计证据 | 实现证据 | 测试证据 | 缺口 |
|----|------|---------|---------|---------|------|
| US-SO-01 | ✅ | `sales/quotation.md`（ACCEPTED 转订单）+`crm/state-machine.md` Lead 轴 | `ErpSalQuotationBizModel.convertToOrder`(:55)+`ErpSalOrderBizModel.createFromQuotation`(:314)+防重复守卫+`ErpCrmLeadBizModel.convertToQuotation`(:201) | `TestErpSalQuotationToOrder`+`TestErpCrmLeadConversion`+o2c-chain+`crm-lead.action.spec.ts` | —（商机独立阶段 e2e 由 crm-lead spec 兼顾，注记） |
| US-SO-02 | ✅ | `sales/README.md` 定价引擎（:72）+`date-ranged-validity-pattern.md` §7 | `ErpSalPriceList/PricingRule` 实体+`ErpSalPricingRuleEngine`+`applyPricingRules`(:140) 行级回写+信用挂起 `TestErpSalCreditHold*` 对应实现 | `TestErpSalPricingRuleEngine`+`TestErpSalPricingEndToEnd`+`TestErpSalPriceLineDeterminism`+`sal-date-range-validation.action.spec.ts` | —（pricingSource 行级来源浏览器断言缺、JUnit 兜底，注记） |
| US-SO-03 | ✅ | `sales/use-cases.md` UC-SAL-02/03+`inventory/cross-domain.md` 余量校验（:71） | `DeliveryStockMoveBuilder` 出库扣减+`ErpSalOrderProcessor.validateOrderAvailability`(:227)+`erp-sal.order-availability-check-level` **默认 OFF（RC-R1.13 已裁决三级部署范式）** | `TestErpSalOrderAvailabilityCheck`+`TestErpInvConcurrentDeduct`+o2c-chain+`inventory-stock-move.action.spec.ts` | —（校验为已裁决 opt-in 部署档位；拣配 Deferred 设计自认，注记） |
| US-SO-04 | ✅ | `finance/ar-ap-reconciliation.md`（核销流程/账龄分级/账龄报表） | `ErpSalReceiptBizModel.settle/reverseSettlement`(:49/:57)+`ReconciliationSettler`+`ErpFinArApItem`+`ArApAgingRow` | `TestErpSalReceiptSettlement`+`TestErpSalOrderToCashEnd`+`TestErpFinAging`+`fin-ar-ap-aging.value.spec.ts` | — |
| US-SO-05 | ✅ | `sales/returns.md`（换货 :30-34/原单关联） | `ErpSalReturnBizModel.generateExchangeDelivery`(:47)+双向 FK `exchangeDeliveryId`↔`exchangeReturnId`（同事务双写） | 退货专项 JUnit 10 类+`sal-return-exchange.action.spec.ts`+`o2c-reverse`+`voucher-back-link` | —（换货价差分支 e2e 断言深度未逐行核验，注记） |
| US-SO-06 | 🔶 | `crm/sales-forecast.md`（实体清单/重算流程）+`crm/territory.md` ErpCrmQuota（:84） | `ErpCrmQuotaBizModel`（getQuotaRollup/finalize/distributeAnnualQuota/getTerritoryPipeline :49-89）+`refreshForecast`(:32) | `TestErpCrmTerritoryQuota`+`TestErpCrmForecastAndScoring`+`crm-forecast-accuracy.value.spec.ts` | **「预测/配额/实际同屏」组合视图缺**（实现件齐、同屏口径无报表/看板证据）→归属冲突升级（§五，Should） |

### Epic D 库存仓储（6 条）

| ID | 判定 | 设计证据 | 实现证据 | 测试证据 | 缺口 |
|----|------|---------|---------|---------|------|
| US-IV-01 | ✅ | `inventory/README.md` 三层模型（:27） | ErpInvStockBalance/Ledger/Move 实体+`StockMoveBookkeeper`+`ErpInvDashboardBizModel` | `TestErpInvStockMoveBookkeeping`+`TestErpInvLedgerImmutable`+`TestErpInvConcurrentDeduct`+`inventory.value`+`inv-snapshot.value` | — |
| US-IV-02 | 🔶 | `inventory/state-machine.md` 调拨状态机（:178）+`cross-domain.md` 在途规则（:81） | `ErpInvTransferOrder` 含 `inTransitWarehouseId`（_gen :48-50）+`confirm`(:27)+状态机 | 状态机矩阵 JUnit+e2e 仅 `inventory.write.spec.ts:135` 子表 CRUD 写入 | **「在途可见」无行为级断言**（confirm 后在途数量/两段移动单未测）→USC-07 承接 |
| US-IV-03 | ✅ | `inventory/state-machine.md` 盘点状态机（:158）+`use-cases.md` UC-INV-07/10 | `ErpInvStockTakeBizModel.startTake/completeTake/cancelTake`(:42/:59/:69)+差异移动单+过账 | `TestErpInvStockTakeCompleteDiffMove`（盘盈/盘亏/零差异/幂等 5 方法）+`TestErpInvPosting` | —（盘点浏览器层 e2e 缺、JUnit 直达验收条件，注记） |
| US-IV-04 | ✅ | `inventory/trace-chain.md`（正/反/退货/批次追溯） | `ErpInvBatch`（expiryDate）+`ErpInvSerialNumber`+四向追溯 `forwardTrace/backwardTrace/returnTrace/batchTrace`(:97-115) | `TestErpInvTraceChain`+`TestErpInvBatchExpiryInterception`+`TestErpInvSerialNumberOutboundGuard`+`inv-inventory-trace.value` | — |
| US-IV-05 | 🔶 | `inventory/barcode-integration.md`（PDA 场景 1-7/架构/4 配置键） | **实现缺**：module-inventory 无 BarcodeService/扫码入口，4 配置键零命中，前端无扫码面（仅 md 域 SKU barcode 字段） | **无** | **实现+测试双缺→USC-05 承接**（档位：商贸/制造档 Must，轻部署 Could） |
| US-IV-06 | ✅ | `drp/README.md`+`drp/use-cases.md` UC-DRP-02..06+`safety-stock-optimization.md` | `DrpEngine/DrpReleaseService/DrpDemandAggregator`+`MrpEngine`+五 MRP 实体+`ErpInvDrpSafetyStockCalc` | `TestErpDrpEngine` 族 7 类+C20a/C20b+`drp-*` action spec 5 件 | —（全矩阵证据最厚条目） |

### Epic E 财务合规（8 条）

| ID | 判定 | 设计证据 | 实现证据 | 测试证据 | 缺口 |
|----|------|---------|---------|---------|------|
| US-FN-01 | ✅ | `finance/posting.md`（三层模型/幂等保证/Provider 机制） | `ErpFinPostingProcessor.post`(:123 幂等)/`alreadyPosted`(:504)+`ErpFinTemplateAcctDocProvider`+`ErpFinVoucherBillR` | `TestErpFinPostingService`+`finance-voucher-post.action.spec.ts`（幂等 re-post 断言）+o2c/p2p-chain posted+回链断言 | —（业务单重复审核幂等 e2e 缺、JUnit 兜底，注记） |
| US-FN-02 | ✅ | `finance/period-close.md`（反结账约束/审计轨迹 RC-R1.44） | `ErpFinAccountingPeriodBizModel.reverseClose`+kill-switch `erp-fin.reverse-close-approval-required`+FNPT `reverseClose roles="管理员"`（erp-fin.action-auth.xml :56） | `fin-period-close-wizard.action.spec.ts` 全链+`e1-1-finance`（restricted denied/管理员 pass）+`TestErpFinReverseCloseAuditTrail`+C13 | —（负向用 restricted 角色非「财务员」精确配对；kill-switch 审批流为 documented simplification，注记） |
| US-FN-03 | ✅ | `finance/bank-reconciliation.md`（自动勾对规则 2/余额恒等式规则 5/BANK_RECON_ADJ 规则 6） | `ErpFinBankReconciliationBizModel.generate/post/reverse`+`BankStatementMatcher`+`BankReconAdjustmentVoucherBuilder` | `fin-bank-recon.action.spec.ts`（正/红冲/2 守卫）+C14 | —（勾对分支级断言/恒等式阻断负向未见，注记） |
| US-FN-04 | ✅ | `finance/budget.md`（承付会计 A2/控制规则/结转） | `ErpFinBudgetControlBiz`+`erp-fin.budget-check-enabled`（默认 false opt-in，e2e 显式开启）+COMMITMENT 影子凭证 | `fin-budget-control.action.spec.ts`（HARD 阻断/WARN 放行+ControlLog）+`fin-budget-vs-actual.value`+JUnit 5 类 | —（开关关闭对照负向未见，注记；opt-in 为已裁决部署契约） |
| US-FN-05 | ✅ | `finance/ar-ap-reconciliation.md` 汇兑规则（:279-297）+`period-close.md` FX 重估 | `ExchangeRevaluationService`（`exchange-revaluation-enabled` **默认 true**）+核销/票据 FX 开关+多币种四件套字段 | `TestErpFinExchangeRevaluation`+`TestErpSalMultiCurrencyReconFx`+票据 FX e2e 3 件+`md-exchange-rate-api` | —（期末重估无 e2e 专项、recon-fx 默认 false 未开——设计声明 opt-in，注记；IAS 21 前期 reversal 残留设计已自认） |
| US-FN-06 | 🔶 | `finance/posting.md` 冲销机制（:314/:608 红字同向取负/isReversed/reversalOfVoucherId）+`roles-and-permissions.md:101` | `ErpFinVoucherBizModel.reverse/reverseVoucher/previewReverseVoucher`(:85-142)+`VoucherReversedEvent` 域监听回退+audit tagSet（模板/映射配置实体） | 红冲 e2e 全家（o2c/p2p-reverse、各域 reverseApprove 红字凭证行断言）+`TestErpFinVoucherTemplateAuditLog`（audit-save 留痕） | **%test 灰度、prod 默认关的审计生产化→USC-03 承接**（收口口径）；Voucher 本体依赖业务列非通用操作日志（注记） |
| US-FN-07 | ✅ | `finance/use-cases.md` 报表用例+`dashboards.md` | `ErpFinReportBizModel.renderHtml/download`(:98-114)+balanceSheet/incomeStatement/cashFlow 数据集（:247-293） | `reports.download.spec.ts`（fin 三大报表×{xlsx,pdf}）+各自 `.{smoke,value}` spec+`TestErpFinReportRendering` | — |
| US-FN-08 | ✅ | `finance/expense-claim.md`（冲抵三路径/现金还款/配置点） | `ErpFinExpenseClaimBizModel`+`ErpFinEmployeeAdvanceBizModel`+`AdvanceOffsetOrchestrator`（`advance-auto-offset-on-expense` 默认 true） | `fin-expense-claim.action.spec.ts`（凭证行+红冲）+`fin-employee-advance-cash-repay(.reverse)`+`TestErpFinExpenseOffsetAdvance` | —（报销审批自动冲抵的浏览器层断言缺、JUnit 直达，注记） |

### Epic F 制造质量（6 条）

| ID | 判定 | 设计证据 | 实现证据 | 测试证据 | 缺口 |
|----|------|---------|---------|---------|------|
| US-MF-01 | ✅（制造档） | `manufacturing/bom-and-routing.md` 多级展开（:62）+`mrp.md` 建议单释放 | `BomExpander`+`explode(bomId,qty,useMultiLevel)`(:63)+`ErpMfgMrpPlanBizModel.runMrp`(:30)+`bom-max-depth` 兜底 | `TestErpMfgBomExplosion`+`TestErpMfgMrpEngine/EndToEnd`+`mfg-mrp-simulation.action.spec.ts`+C08 释放 | —（档位验收登记 USC-06 完成；建议→PR/工单释放 e2e 由 C08 承接，注记） |
| US-MF-02 | ✅（制造档） | `manufacturing/material-reservation.md` 齐套校验（:121/:177）+`state-machine.md` 场景 B | `checkAvailability`(:66)+`start` STOCK_RESERVED 门控+`validateTransitionForStart`(:319)+`allow-partial-kit-start` 默认 false fail-closed | `TestErpMfgWorkOrderStateMachine`（Full→RESERVED/Partial→PARTIAL）+`mfg-work-order.action.spec.ts`+mfg-chain | —（档位验收登记 USC-06 完成；start 前未齐套的浏览器层负向断言弱、JUnit 兜底，注记） |
| US-MF-03 | ✅（制造档） | `manufacturing/state-machine.md` 作业卡（:184/:230 SoD） | `ErpMfgJobCardBizModel`（startJob/recordWork/submitJob/completeJob :53-90）+`ErpMfgJobCardTimeLog` 写入 Processor | mfg-chain 报工链（laborCost 回写断言）+`TestErpMfgScheduleToJobCard` | —（档位验收登记 USC-06 完成；无独立 jobcard e2e、TimeLog 明细行断言弱，注记） |
| US-MF-04 | ✅（制造档） | `manufacturing/variance-analysis.md`+`bom-and-routing.md` 成本卷算（:84-100） | `ErpMfgCostRollup/Variance BizModel`+`calculateVariances`(:50 重算前红冲再过新凭证)+`variance-auto-calc-enabled` | `mfg-variance-recompute-reversal.action.spec.ts`+`mfg-variance.spec.ts`（PRODUCTION_VARIANCE 凭证 6 行数值）+JUnit 5 类 | —（档位验收登记 USC-06 完成；overhead/subcontract 增强开关 e2e 未开，注记） |
| US-MF-05 | ✅ | `quality/inspection-integration.md`（触发/让步/NCR-CAPA 闭环）+`state-machine.md` 场景 A-D | `ErpQaInspection/NonConformance/Action BizModel` 全动作+`InspectionTemplateMatcher`+`erp-mfg.inspection-gate-enabled` | `quality-ncr*` spec 6 件（含 CONCESSION 无副作用对照）+`mfg-inspection-gate.spec.ts`+`TestErpQaInspectionStateMachine`（让步→CONDITIONAL 须审批）+C09 | —（档位验收登记 USC-06 完成；让步审批轴 e2e 断言缺、JUnit 直达，注记） |
| US-MF-06 | ✅ | `quality/recall.md`（定位/NCR 升级/跨域协作） | `ErpQaRecallBizModel.locateTargets`(:79)+`RecallTargetLocator`（batchTrace→OUTGOING 反查 Delivery→partnerId/shippedQty）+notifyCustomers 门控 | `qa-recall.action.spec.ts` 全链+`quality-recall-generate-returns`+JUnit 4 类 | —（档位验收登记 USC-06 完成；库存维度 target 明细断言深度未逐行核验，注记） |

### Epic G 资产/维护/项目（3 条）

| ID | 判定 | 设计证据 | 实现证据 | 测试证据 | 缺口 |
|----|------|---------|---------|---------|------|
| US-OP-01 | ✅ | `assets/depreciation-and-posting.md`（折旧凭证/资本化/处置清理） | `ErpAstAsset/DepreciationSchedule/Disposal BizModel`+`ErpAstDisposalProcessor`（双凭证+回填）+`DepreciationPostingDispatcher` | `ast-depreciation/cip-capitalization/value-adjustment.action.spec.ts`（含凭证行数值）+报表 value 层+`e1-2-assets` 负向 | —（Disposal 浏览器层属 xwf 不可达裁决边界，注记） |
| US-OP-02 | ✅ | `maintenance/use-cases.md` UC-MAIN-01/03/04/05+`state-machine.md` | `ErpMntSchedule/Request/Visit/SparePartUsage` xbiz+`SparePartUsageConfirm/ReverseConfirmProcessor` | `mnt-*` action spec 8 件+`TestErpC10MntRequestSparePart`+维护报表/看板 value 层 | — |
| US-OP-03 | ✅ | `projects/cost-collection.md`（工时成本/归集/结转）+`task-dag.md` | `ErpPrjTask/Timesheet/CostCollection/ProjectSettlement/ProjectPnl` 全 xbiz | `projects-task/timesheet-posting/pnl-settlement/settlement-posting` 4 spec+`TestErpC12` | — |

### Epic H HR/合同/B2B/物流（6 条）

| ID | 判定 | 设计证据 | 实现证据 | 测试证据 | 缺口 |
|----|------|---------|---------|---------|------|
| US-HR-01 | ✅ | `human-resource/use-cases.md` UC-HR-01/02/06/07/08 | `ErpHrEmployee/EmploymentContract/Attendance/LeaveRequest/LeaveBalance` xbiz | `hr-leave-attendance/leave-shift-linkage/transfer.action.spec.ts`+crud/reports 层+`e1-1-hr` 负向 | —（hr 侧雇佣合同到期 Job 未验证到、ct 域 `ErpCtContractExpiryJob` 承担同类语义，注记） |
| US-HR-02 | ✅ | `human-resource/payroll.md`+`payroll-simulation.md`+`roles-and-permissions.md:288`（9 FNPT 点） | `ErpHrSalary(+Item)/TaxConfig/SocialInsurance*` xbiz+`salary-approval/v1.xwf` | `hr-payroll/salary-simulation.action.spec.ts`（七级累进配置链）+`TestErpC17`+`e1-1-hr` 负向 | —（xwf 审批浏览器层不可达为 2330-1 已裁决边界；要素配置项级 e2e 缺、JUnit 兜底，注记） |
| US-HR-03 | ✅ | `human-resource/recruitment.md`（候选人/Offer/入职实体级设计）——**实现采权威裁决 UC-HR-05 扁平模型** | `ErpHrRecruitment` 单实体状态机 OPEN→…→HIRED+hire 联动建 Employee+Contract(ACTIVE) 回写 | `hr-recruitment.action.spec.ts`（漏斗全迁移+hire 联动反查） | —（**设计文档粒度与实现模型漂移**：recruitment.md 按字面验收大面积「未实现」，实为文档粒度超前，注记+§六差异清单） |
| US-CT-01 | ✅ | `contract/contract-repository.md`+`state-machine.md` | `ErpCtContract(+Version)/InvoicePlan` xbiz+`ErpCtContractExpiryJob`（15/7/30 天分档通知派发） | `ct-contract-lifecycle/version/invoice-plan-trigger.action.spec.ts`+`TestErpCtContractExpiryJob`+C18 | —（到期提醒仅 JUnit 层，注记） |
| US-B2-01 | ✅ | `b2b/state-machine.md`（出站自动化 Deferred 明示 :6）+`asn-processing.md` | `ErpB2bEdiDoc/Asn(+Line)` xbiz+`matchPurchaseOrder/createReceiveFromAsn`+ASN 状态机/webhook Processor | `b2b-edi-doc/asn-match-receive/line-level-receive-fill` spec+看板 value 2 件+`TestErpC19` | —（档位验收登记 USC-06 完成；MFT 真实 transport=设计内 Non-Goal） |
| US-LG-01 | ✅ | `logistics/state-machine.md`+`carrier-integration.md` SPI 三层 | `ErpLogCarrier/Shipment(+Log)` xbiz+`HandleTrackingWebhookProcessor`+`AbstractErpLogShipmentDeliveredProcessor`（delivered→freight 到岸成本 DRAFT→SETTLED :88/:145） | `log-shipment.action.spec.ts`+`log-delivered-freight-posting.action.spec.ts`（**webhook→FREIGHT 过账浏览器层实证，执行者复核纠正子代理误判**）+`log-path2-landed-cost-auto-create` | —（logistics 无专属看板/报表，归 US-PL-04 注记） |

### Epic I 平台横切（8 条）

| ID | 判定 | 设计证据 | 实现证据 | 测试证据 | 缺口 |
|----|------|---------|---------|---------|------|
| US-PL-01 | 🔶 | `roles-and-permissions.md`（权限规则 :57/映射 :132/运行基线 :207/R2.7 :236/E1.2 :253） | `nop.auth.enable-action-auth` %test=true / %dev,%prod=false（application.yaml :54/:66/:81）+FNPT 声明 `_erp-*.action-auth.xml`（19 域） | `negative/` 12 件（e1-1/e1-2/e1-3 全域 enforcement+`role-login`+`dry-run-impact` 权威文档）+`f16-high-risk.visual` | **%prod 翻转+菜单塌缩 P1.5a 收口→USC-02a 承接（唯一载体）** |
| US-PL-02 | 🔶 | `roles-and-permissions.md`（数据权限 :74/行过滤 E2 全链 :83/三开关 :213/orgId 解耦 :258） | `enable-data-auth`+`role-row-filter-enabled` %test=true/prod=false+19 域 `erp-*.data-auth.xml`+`ErpRoleDataAuthChecker` | `e2-1/e2-2/e2-3` 行过滤负向+sal/qa/mnt 三域隔离 JUnit+`TestErpDataAuthStructure` | **%prod 灰度→USC-02b 承接**（15 域 inert stub 为设计内既裁决状态，注记） |
| US-PL-03 | 🔶 | `architecture/approval-framework.md`+`notification-strategy.md`（待办语义 :11/收件箱 :64） | notify 子系统+`inbox.page.yaml`（mark-read 端点）+审批执行面（DIRECT+xwf 4 实体） | `notify-inbox.action.spec.ts`（收件箱页面可达+读/标记翻转）+`e1-3` 审批相邻负向 | **移动视口可达性零证据（playwright 仅 Desktop Chrome）+无跨域待办中心聚合页→USC-04 承接** |
| US-PL-04 | ✅ | `dashboards.md`（8+域看板章节）+`dashboard-semantic-layer.md` | 各域 `*DashboardBizModel`+`*ReportBizModel`+dashboard/report page.yaml | dashboards 28 spec+`reports.download`（24×2 下载矩阵）+visual/snapshot 层 | —（logistics 无专属看板、域覆盖面演进注记） |
| US-PL-05 | ✅ | `architecture/customization-capabilities.md`（Model→Delta→Java :39/Delta 能力 :49/升级保护 :225） | 生产 Delta 实证（`_delta/default/nop/auth/pages/*` 2 件）+测试 Delta 12 域多 layer | Delta 文件随全 reactor `mvn test` 间接加载生效 | —（Delta 生效性命名专项守卫测试缺、间接证据充分，注记） |
| US-PL-06 | 🕒 | `portal/README.md`（STATUS: future :3/边界 (future)/identity-and-access ask-first :20/:72） | 无（与 future 声明**一致**） | 无 | 无缺口（需求即远期，声明内状态；触发=协同需求立项 plan-first+人工批准） |
| US-PL-07 | 🔶 | `architecture/system-baseline.md` 多租户策略（:76-80 未启用）+`multi-company.md`（org-isolation 默认 false :29）+product-scope 延迟段（:72-75） | application.yaml 无 oauth/sso/tenant 键（与「未启用」声明一致）+平台 JWT 认证 | 无（与未启用自洽） | 无缺口（延迟段边界登记完整；SSO 走平台能力 Could-Should，触发=企业部署需求） |
| US-PL-08 | 🔶 | `inventory/barcode-integration.md` PDA 场景+`view-and-page-strategy.md:106` | **实现缺**（与 US-IV-05 同源：无扫码面/无移动视口适配层） | **无移动视口测试**（playwright 仅 Desktop Chrome；全树无 setViewportSize 移动尺寸） | **实现+测试双缺→USC-05 承接**（与 US-IV-05 同一收口项） |

## 四、隐性缺口分流表

> 分类：A=测试断言缺（验收条件无运行时断言）／B=实现缺／C=实现收口（config/页面层，roadmap 已裁定承接项）／D=无缺口、仅登记。后两项分级供 USC-06/07 收窄范围。

| # | US | 缺口描述 | 分类 | 处置/承接 | 需要度 |
|---|----|---------|------|----------|--------|
| 1 | US-PL-01 | %prod action-auth 默认关 + 菜单塌缩 P1.5a 收口 | C | **USC-02a**（prod 翻转唯一载体，前置门控已满足） | Must |
| 2 | US-PL-02 | %prod data-auth/role-row-filter 默认关 | C | **USC-02b**（org 隔离维持独立开关不捆绑） | Must（多团队） |
| 3 | US-FN-06 | 审计/高危留痕 %test ON、prod 默认关 | C | **USC-03**（生产化收口口径） | Must（收口） |
| 4 | US-MD-04 | 主数据变更历史链路实现缺（`critical-attributes` 键未接线） | B（Should·条件触发） | **USC-03** 名下按 roadmap 关闭口径处置：软停用/引用约束+审计字段已满足；独立变更历史 UI 不立项除非合规客户点名 | Should |
| 5 | US-PL-03 | 移动视口可达性零证据 + 无跨域待办中心聚合页 | B（页面层） | **USC-04**（响应式 Web 待办+审批窄屏可达，Non-Goal 原生 App） | Must（流程效率） |
| 6 | US-IV-05 / US-PL-08 | 条码/PDA 扫码触发层实现+测试双缺（4 配置键未实现） | B | **USC-05**（运行时收口：触发层接线+代表场景断言；档位化验收） | 档位 Must（商贸/制造档） |
| 7 | US-IV-02 | 调拨「在途可见」无行为级断言（e2e 仅子表 CRUD 写入） | A | **USC-07**（补 confirm→在途→收发两段行为断言，复用既有调拨实体） | Must |
| 8 | US-MF-01..06 | 制造链档位 Must 验收登记完成（矩阵判「已覆盖」；档位口径=制造档 Must、完整档继承、纯商贸不组装 manufacturing/quality） | D | **已登记（USC-06 完成，plan 2026-10-01-1145-1）**——证据锚点清单落盘 manufacturing/README.md 档位验收登记段 | 档位 |
| 9 | US-B2-01 | B2B 档位验收登记完成（核心验收已覆盖） | D | **已登记（USC-06 完成）**——完整档/大客户 Should；真实 EDI 网关维持 product-scope 延迟段；锚点清单落盘 b2b/README.md 档位验收登记段 | 档位/Should |
| 10 | US-PO-01 | **RFQ→报价→PO 转单链实现缺**（仅请购→订单；RFQ/报价单孤立 CRUD+审批实体，award/accept/convert 全域零命中） | B（Must⚠） | **归属冲突升级人工裁决**（§五；无 USC-02a..07 承接项，USC-07 仅测试载体不能补实现） | Must |
| 11 | US-SO-06 | 预测/配额/实际「同屏对比」组合视图缺（实现件齐：Quota BizModel+Forecast 报表，无同屏口径载体） | B（Should） | **归属冲突升级人工裁决**（§五） | Should |
| 12 | 其余 36 条（= 53 − 上 11 行显式覆盖 17 条；含 33 ✅ + US-PO-05/US-PL-07 两个 🔶 仅登记 + US-PL-06 🕒） | 无缺口；注记级残留（分支对照断言、浏览器层补充、logistics 看板扩面等）见 §三各行括注 | D | 无缺口、仅登记（不设工作项；测试增强候选属各域日常维护面，不进本 roadmap 队列） | — |

**分流统计**：承接编号×9（USC-02a×1 + USC-02b×1 + USC-03×2 + USC-04×1 + USC-05×1[两 US 同源] + USC-06×2[降级登记] + USC-07×1）+ 归属冲突升级×2 + 仅登记×36（含 §四第 12 行批量裁定），合计覆盖 53 条 US。

## 五、归属冲突升级登记（roadmap §10 规则 6 第三分支）

### US-PO-01 — RFQ→报价→PO 转单链实现缺（Must）

- 缺口描述：验收条件「RFQ→报价→PO 链路；PO 状态可见」前半段未闭环。实仓证据：`ErpPurRequisitionBizModel.convertToOrder`(:46) 为唯一转单实现；`ErpPurRfqBizModel`/`ErpPurQuotationBizModel` 仅 cancel+审批轴 helper，全域 grep `award|isAccepted|convertToOrder|createRfq|中标` 于 pur-service main 零命中；C01 集成测试起点即 PO。
- 为什么 USC-02a..07 均不承接：USC-02a..05 为横切收口/条码/移动面，USC-06 限制造/B2B 档位证据，USC-07 为测试深度载体且明文「不借机加功能」——寻源链补实现属功能缺口，无既有归属。
- 建议归属：或 (a) 人工批准新立「采购寻源链收口」工作项（RFQ award→Quotation accept→PO 转单 + 比价视图），或 (b) 人工裁决将 US-PO-01 验收口径收窄为「请购→订单+PO 状态可见」（对齐 C01/p2p-chain 既有证据），并将 RFQ/报价维持「实体+审批轴已就位」的 🔶 登记。
- 风险：Must 级 P2P 主干故事验收条件与实现不符，且 gap analysis 曾记 ✅（文档级证据）——存在「文档提及冒充能力存在」风险面，建议优先裁决。

### US-SO-06 — 预测/配额/实际同屏对比视图缺（Should）

- 缺口描述：实现件齐（`ErpCrmQuotaBizModel.getQuotaRollup/getTerritoryPipeline`+`crm-forecast-accuracy` 报表），但验收条件「同屏口径」无组合视图载体（无看板/报表聚合三值；`sales-forecast.md:186` 仅声明预测与配额两套数据边界）。
- 为什么不承接：同 §五-US-PO-01，组合视图属新功能面，USC-07 不能补。
- 建议归属：或人工批准 CRM 看板/报表增量（quota-vs-forecast 数据集+value 断言），或裁决验收口径为「配额聚合与预测准确率报表各自可达」（现有证据已满足）。
- 风险：Should 级，不阻塞核心发布。

## 六、与既有文档差异清单

| # | 项 | 既有说法 | 实仓证据 | 采信口径 |
|---|----|---------|---------|---------|
| 1 | US-PO-01 | gap analysis `:45` 记 ✅（证据=「RFQ/报价/PO；coverage 矩阵」文档级） | 转单链实现缺（§五） | 实仓降档 🔶⚠，升级人工裁决 |
| 2 | US-PO-05 | gap analysis `:49` 记 ✅（证据=「审批轴+SoDGuard」） | SoD 全链实证；金额阈值门控无实现 | SoD 维度 ✅ 采信；阈值维度按 gap analysis §3.4 + roadmap §9 Non-Goal（细则归 wf 配置）仅登记，不升格 |
| 3 | US-SO-03 | gap analysis `:58` 记 ✅ | 可用量校验默认 OFF（RC-R1.13 已裁决三级部署范式） | 维持 ✅：已裁决 opt-in 合法（lessons/14），非隐性缺口 |
| 4 | US-SO-06 | gap analysis `:61` 记 ✅（「sales-forecast/territory 配额」） | 组合视图缺 | 实仓降档 🔶，升级人工裁决（Should） |
| 5 | US-IV-02 | gap analysis `:68` 记 ✅（「调拨状态机（含在途设计）」） | 在途实体+confirm 实证，行为断言缺 | 测试深度缺口，降档 🔶→USC-07 |
| 6 | US-IV-05/US-PL-08 | gap analysis `:71`/`:128` 记 🔶（「设计在册；依赖页面与运行验证」） | 实现与测试均缺（4 配置键零命中、零移动视口测试） | 与 gap analysis 档位裁决一致，但缺口实质比「体验未验收」更重（实现层缺）→USC-05 明确为实现类收口 |
| 7 | US-MD-04 | gap analysis `:39` 记 🔶 Should（「审计日志可查即可」） | 变更历史链路实现缺实证（键未接线） | 与 roadmap USC-03 关闭口径一致，登记实证细节 |
| 8 | US-HR-03 | gap analysis `:112` 记 ✅ | 设计文档（实体级漏斗）与实现（扁平状态机，UC-HR-05 权威裁决）模型漂移 | 实现按权威裁决采信 ✅；recruitment.md 粒度漂移登记，建议随 hr 域文档维护批次回写（非本 roadmap 工作项） |
| 9 | US-LG-01 | （本矩阵执行中代理误判「webhook 浏览器层 E2E 未见」） | `log-delivered-freight-posting.action.spec.ts` 头部与断言实证覆盖 handleTrackingWebhook→FREIGHT 过账 | 执行者交叉复核纠正，维持 ✅ |
| 10 | 2026-06-30 coverage 矩阵 | 逐调研功能口径（Odoo 100% 等） | 本矩阵为 US 三维口径 | 两矩阵口径不同不冲突；功能级覆盖争议以实时仓+本矩阵为准 |

## 七、诚实性声明

- 满足度判定以**实仓核验锚点**（文件路径+行号/方法名/配置键/spec 文件名）为准；53 条全部经 4 路独立子代理 ls/grep/find 实证，3 处代理间/与仓内记录矛盾点由执行者交叉复核后采信实仓（§六 #9）。
- 弱指针（文件存在+javadoc 声明、断言深度未逐行核验）均在表内标注「注记/未逐行核验」，不冒充强证据。
- 「已实现未默认」按 config-gate 裁决惯例记 🔶 或注记（RC-R1.13、budget-check、recon-fx 均为已裁决 opt-in，不记缺口）。
- 已裁决覆盖边界（xwf 不可达 2330-1、设计内 Deferred、roadmap §9 Non-Goal、档位语义）不计缺口，逐条注明裁决出处。
- 本矩阵为 USC-01 一次性产物，不维护状态列；后续缺口承接进度以 `docs/backlog/user-story-coverage-roadmap.md` Work Item Status 为唯一真相源。
