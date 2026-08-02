# ARM-MA4 pur+sal+inv+qa+crm 代码质量抽样审查报告（A4.5）

> 里程碑：MA4（代码与前端质量层 / 代码实现质量维度）
> Roadmap 工作项：A4.5（pur+sal+inv+qa+crm 代码质量抽样，A 级合并）
> Plan：`docs/plans/2026-07-29-0430-2-audit-remediation-ma4-pur-sal-inv-qa-crm-code-quality.md`
> 行为基线：`docs/design/purchase/{state-machine,three-way-match,returns}.md` + `docs/design/sales/{state-machine,use-cases,returns}.md` + `docs/design/inventory/state-machine.md`（库存成本方法见 `docs/design/finance/costing-methods.md`）+ `docs/design/quality/state-machine.md` + crm README；`docs/architecture/{processor-extension-pattern,posting-exemptions}.md`
> Skill：`docs/skills/code-quality-audit-prompt.md`（7 重点领域 + P0-P3 严重性指南）
> 实仓快照：2026-07-29（HEAD `find module-<domain> -path "*service*" -name "*.java"`：purchase 111 / sales 109 / inventory 89 / quality 69 / crm 75 = 合计 453 文件）
> 裁决：**Verdict = ⚠️(P1)**——五域代码实现质量在**编排健壮性（6+6 Processor 步骤化 protected 方法 + 三轴状态分离 + 全量迁移守卫）/ 核销与匹配算术正确性（PaymentSettler 双余额校验 + ThreeWayMatcher 数量门禁+价格容差 / StockMoveBookkeeper UK+retry 并发安全 P0-MA2-020 已修复落地 / SPC Cp/Cpk/Cpm/UCL/LCL 公式正确）/ 跨域写经 I\*Biz Facade（production 代码零凭证直写，P0-MA1-021 已修复落地复核确认）/ 状态守卫（P0-MA2-017 已修复落地复核确认）/ 异常规范化（全 NopException + erp.err.{pur,sal,inv,qa,crm}.* ErrorCode）/ BigDecimal 货币类型安全（money 字段全域 BigDecimal，crm 7 列 DECIMAL↔Double 仅影响 ratio/percent 非货币字段，实测精度损失 ≤1e-15 不可见）** 七面扎实；零 P0——无活跃数据破坏路径（核销/匹配/成本/SPC 算术经主路径测试覆盖；upsertBalance UK+retry 多线程测试覆盖；多币种归 P1-MA2-002/009 finance 引擎层 deferred；业财悬挂归 finance DeferredPostingSweepJob 兜底[reverse 方向除外→P1-MA4-020]）。**3 项新 P1**（P1-MA4-020 inventory 到岸成本反向过账业财悬挂[reverse 方向无 sweep 覆盖]——MR1；P1-MA4-021 pur+sal+inv 过账/核销/成本链路测试有效性系统性不足[多币种零覆盖+业财异常悬挂零覆盖+STANDARD 红冲成本不变量+SPECIFIC 成本调整+rollbackReceive 不对称+SalReversalListener 3/4 回退]——MR2；P1-MA4-022 pur+sal+inv Processor/过账链路跨域 daoFor 投影[同 P1-MA1-022 根因]——MR1 不重复计入 MR2）+ **4 项新 P2** watch-only（P2-MA4-010 五域可维护性热点合并 / P2-MA4-011 五域自动化防护[R2d 未覆盖 daoFor(ErpFin/ErpInv/ErpPur) + 无算术回归门控] / P2-MA4-012 quality NCR/SPC 链路可观测性缺陷合并[zero-voucher + post-commit cascade swallow] / P2-MA4-013 crm Forecast 链路缺陷合并[stageName stub + refresh 并发]）。MA1/MA2/MA3 已知 finding 运行时复核 **20 项中 19 项「如登记」无升级 + 2 项「已确认修复」（P0-MA1-021 / P0-MA2-017 / P0-MA2-020）**；**1 项复核建议降级**（P1-MA1-009 crm DECIMAL↔Double 实测精度影响可忽略，建议 MR1 裁决降 P2）。本审计原则上**无 P0**（代码静态审查 + 测试有效性抽样，无活跃数据破坏；算术误差均经现有测试/约束兜底或已登记 deferred）。

---

## 1. 范围与基线

### 1.1 在范围（代码实现质量，非状态机业务正确性——A 级合并按链路抽样）

五域 `module-<domain>/erp-<short>-service/src/main/java/` 核心组件（合计 453 service 文件，抽样重点链路）：

| 域 | 抽样组件 | 文件 |
|----|---------|------|
| **purchase** | 6 Processor 审批轴 + PaymentSettler + ThreeWayMatcher + 3 PostingDispatcher + PurAcctDocProvider + PurPostingExecutor + PurReversalListener | `processor/ErpPur{Order,Receive,Invoice,Payment,Return,Requisition}Processor.java` / `service/entity/PaymentSettler.java` / `service/ThreeWayMatcher.java` / `service/posting/{PurInvoice,PurPayment,PurReturn}PostingDispatcher.java` / `service/posting/PurAcctDocProvider.java` |
| **sales** | 6 Processor 审批轴 + SalAcctDocProvider + 3 PostingDispatcher + SalPostingExecutor + SalReversalListener + DeliveryStockMoveBuilder + ReceiptSettler + ReturnRefundOrchestrator | `processor/ErpSal{Order,Delivery,Invoice,Receipt,Return,Quotation}Processor.java` / `service/posting/{SalInvoice,SalReceipt,SalReturn}PostingDispatcher.java` / `service/posting/SalAcctDocProvider.java` / `service/DeliveryStockMoveBuilder.java` |
| **inventory** | StockMoveBookkeeper + ErpInvOwnershipTransferProcessor + ErpInvLandedCostProcessor + 成本方法链(StandardCostResolver/CostMethodResolver/CostAdjustmentService) + 7 CostingStrategy + 5 PostingDispatcher + InvPostingExecutor | `service/StockMoveBookkeeper.java` / `processor/ErpInvLandedCostProcessor.java` / `service/{StandardCostResolver,CostMethodResolver,CostAdjustmentService}.java` / `service/costing/*CostingStrategy.java` / `service/posting/*PostingDispatcher.java` |
| **quality** | ErpQaInspectionBizModel + NcrPostingDispatcher + NcrReturnOrchestrator + InspectionTrigger + SpcCalculator 系列 | `service/entity/ErpQaInspectionBizModel.java` / `service/posting/NcrPostingDispatcher.java` / `service/NcrReturnOrchestrator.java` / `service/spc/{SpcControlLimitCalculator,SpcCapabilityCalculator,SpcRuleEngine,SpcOutOfControlHandler}.java` |
| **crm** | 4 BizModel + LeadScoringEngine + ForecastAggregator + PriceRuleEngine + FunnelAggregationEngine | `service/entity/ErpCrm{Lead,Forecast,PriceRule,FunnelStageMetrics}BizModel.java` / `service/engine/{LeadScoringEngine,ForecastAggregator,PriceRuleEngine,FunnelAggregationEngine}.java` |

### 1.2 不在范围（Non-Goals 见 plan）

- A2.8/9/11/12/14 状态机业务正确性（done）——本审计复核其 finding 运行时状态
- A4.1a/b finance 过账 Facade（done）——本审计复核 pur/sal 侧过账调用点错误传播
- A3.5 owner doc vs 代码 drift（done）——本审计复核以 A3.5 已登记 finding 为输入
- A4.7/A4.8 view.xml drift / A5.x 测试覆盖深度统计 / A6.x 权限注解完整性
- 代码缺陷批量修复（在 MR2/MR1）

---

## 2. 7 重点领域逐项审查（五域合并裁决）

### 领域 1：架构和边界完整性（裁决：**PARTIAL——跨域写经 Facade 全合规；跨域只读 daoFor 维持 P1-MA1-022 投影**）

| 域 | 控制点 | 裁决 | 证据 |
|----|--------|------|------|
| purchase | 跨域写经 I\*Biz Facade | ✅ PASS | 全部跨域写经 Facade——`IErpInvStockMoveBiz.generateMove` / `IErpFinVoucherBiz.post` / `IErpFinBudgetCommitmentBiz.commit` / `IErpMdSupplierApprovalBiz`。production 代码零 `daoFor(ErpFin*)` 写凭证 |
| purchase | 跨域只读 daoFor | ❌ 投影 | `ErpPurOrderProcessor:302 daoFor(ErpMdSubject)` + `:314 daoFor(ErpFinAccountingPeriod)` + `ErpPurPaymentProcessor:228,240` 同型 + `ErpPurDashboardBizModel:155,184,230 daoFor(ErpMdPartner)` —— **「如 P1-MA1-022 登记」**，详 P1-MA4-022 |
| sales | 跨域写经 I\*Biz Facade | ✅ PASS | `IErpInvStockMoveBiz` / `IErpMdPartnerBiz` / `IErpQaInspectionBiz` / `IErpFinBudgetCommitmentBiz` / `IErpFinVoucherBiz` / `IErpMdAcctSchemaBiz`（DeliveryStockMoveBuilder:26 范式） |
| sales | 跨域只读 daoFor | ❌ 投影 | `ErpSalOrderProcessor:377 daoFor(ErpMdSubject)` + `:389 daoFor(ErpFinAccountingPeriod)` —— **「如 P1-MA1-022 登记」**。Dashboard facade `daoFor(ErpMdPartner)` read-only 永久接受 |
| inventory | 跨域写经 I\*Biz Facade | ✅ PASS | P0-MA1-021 **已确认修复**——`CostAdjustmentPostingDispatcher:64-80` 经 `IErpFinVoucherBiz.reverse()` Facade（无 `markOriginalVoucherReversed` 直写，grep 0 matches）。4 PostingExecutor + 5 dispatcher 全经 Facade |
| inventory | 跨域只读 daoFor | ❌ 投影 | 16 站点——`CostMethodResolver:61,70` / `StandardCostResolver:75,85,99` / `CostAdjustmentService:210,219,233,291` / `ErpInvLandedCostProcessor:267,473,477` / `ErpInvDashboardBizModel:152,372,384,396` —— **「如 P1-MA1-022 登记」**，详 P1-MA4-022 |
| quality | 跨域写经 I\*Biz Facade | ✅ PASS | `NcrPostingExecutor→IErpFinVoucherBiz.post` / `NcrReturnOrchestrator:99,117→IErpPurReturnBiz.save + IErpSalReturnBiz.save`（**P1-MA1-029 同型复核 CLEAN**——经 Facade 非 daoFor saveEntity 绕过审批管道）/ `SpcOutOfControlHandler→IErpQaNonConformanceBiz + IErpQaActionBiz` |
| quality | 跨域只读 daoFor | ❌ 投影 | 3 站点——`NcrPostingDispatcher:121 daoFor(ErpInvStockBalance)` / `NcrReturnOrchestrator:135 daoFor(ErpInvStockBalance)` / `ErpQaReportBizModel:361 daoFor(ErpMdMaterial)` —— **「如 P1-MA1-022 登记」**（qa 已在 9 域枚举内） |
| crm | 跨域访问 | ✅ PASS | **零跨域 daoFor**（grep `daoFor(Erp(Sal|Md|Inv|Fin|...)` 在 crm service = 0 matches）。`ErpCrmConversionProcessor→IErpMdPartnerBiz + IErpSalQuotationBiz` Facade。Dashboard `ErpCrmReportBizModel` 仅读域内 `ErpCrmForecast/ForecastLine/Stage`。**crm 不在 P1-MA1-022 9 域内，无投影** |

**裁决**：跨域写经 I\*Biz Facade 五域全合规（P0-MA1-021 已修复落地）；跨域只读 daoFor 维持 P1-MA1-022 投影（pur+sal+inv 25 站点 + qa 3 站点，详 P1-MA4-022）；crm 边界全清。**无新边界违规站点**。

### 领域 2：核心实现正确性（裁决：**FAIL——inventory 到岸成本反向过账悬挂（P1-MA4-020）+ 已登记 P1-MA2-023/024 复核「如登记」+ upsertBalance 已修复**）

| 域 | 控制点 | 裁决 | 证据 |
|----|--------|------|------|
| purchase | PaymentSettler 核销算术 | ✅ PASS | `settle:55-111` 双余额校验（`amount>invoiceBalance→ERR_SETTLE_OVER_INVOICE_BALANCE` / `amount>paymentRemaining→ERR_SETTLE_OVER_PAYMENT_BALANCE`）+ `paymentRemaining` 循环递减 + `recomputeInvoicePaid/recomputePaymentWrittenOff` 事后聚合 + `reverseSettlement` 负金额行回退 |
| purchase | ThreeWayMatcher 匹配算法 | ✅ PASS | `match:50-108` 数量强制门禁（strict）+ 价格容差百分比 `|inv-ord|/ord*100` HALF_UP 4 位 + non-strict warn 放行——与 owner doc three-way-match.md 一致 |
| purchase | 过账悬挂（业财闭环） | ⚠️ 维持 | 3 PostingDispatcher tryPost `catch(Exception){LOG; return false}` 吞咽——**但 finance `ErpFinPostingExceptionRecorder` 在 voucherBiz.post 内部失败时 REQUIRES_NEW 写 ErpFinPostingException + DeferredPostingSweepJob 每 5min cron 兜底重试**。purchase 3 dispatcher 路径经 finance sweep 兜底（与 assets 折旧 P1-MA4-013 无 sweep 不同）。维持 family P1，不重复登记 |
| sales | 状态机对称性 + 红冲闭环 | ✅ PASS | 6 Processor `doReverseApprove` 全置 REJECTED + 清 approvedBy/At；cancel/doReverseApprove 前置 `posted==TRUE` 守卫；`SalReversalListener` 4 rollback 前置 posted 守卫幂等；`ReturnRefundOrchestrator` HashSet 去重避免重复反向核销 |
| sales | 多币种凭证装配（P1-MA2-009a 复核） | ❌ 维持 | `SalAcctDocProvider:101 fact.setAmount(amount)` 单一字段；`VoucherFact:16` 无 amountSource/amountFunctional——**「如 P1-MA2-009 登记」**，下游引擎 `ErpFinPostingProcessor:818-819` 同 amt 写 source+functional |
| sales | 收款核销汇兑损益（P1-MA2-009b 复核） | ❌ 维持 | `SalAcctDocProvider:87-91` RECEIPT 仅 Dr BANK/Cr AR 同金额，**无 6051 FX plug**；`ReceiptSettler:55-100` 无 currencyId/exchangeRate 检查。对比 `NotesReceivableAcctDocProvider:80,86-90` 已实现 FX plug 范式证明平台支持——**「如 P1-MA2-009 登记」** |
| inventory | **upsertBalance 并发安全（P0-MA2-020 复核）** | ✅ **已确认修复** | ORM UK 存在：`app-erp-inventory.orm.xml:415 UK_INV_STOCK_BALANCE_NATURAL on (orgId,materialId,skuId,warehouseId,locationId,batchNo,ownerId)` 含 `constraint=` 触发 DDL；`StockMoveBookkeeper.updateBalanceWithRetry:252-319` MANAGED/TRANSIENT/SAVING 三分支 + `flushAndCheckConflict:325-335` UK violation catch + evict + reload + retry；`isUniqueConstraintViolation:418-431` 走 cause chain；`TestErpInvConcurrentDeduct` 多线程测试断言行数=1 无 split-quantity。**修复完全落地** |
| inventory | **到岸成本反向过账悬挂（新发现 P1-MA4-020）** | ❌ FAIL | `ErpInvLandedCostProcessor.doReverseApprove:204-217` 包 `postingDispatcher.reverse(landedCost)` 于 `catch(Exception){LOG.warn/error}`——`voucherBiz.reverse` 经 REQUIRES_NEW 独立提交。若 GL 反向已提交后 postingDispatcher.reverse 抛出被吞咽，或反向编排后续步骤失败：GL 已反向但库存成本层未反向 + posted 标志状态不一致 → **业财不一致无自动恢复**（reverse 方向 DeferredPostingSweepJob 不覆盖）。详 P1-MA4-020 |
| inventory | STANDARD 红冲成本不变量（P1-MA2-024 复核） | ❌ 维持 | `StandardCostingStrategy.onIncoming:43 / onOutgoing:70` **重解析** standardCostResolver.resolve(materialId) 忽略传入 unitCost 参数；reverse 流程 `ErpInvStockMoveProcessor.reverse:144` 传 `rl.setUnitCost(nz(ol.getUnitCost()))` 被**静默丢弃**。若原发移动与反向之间发生 STANDARD_REVALUATION 成本调整发布新 FIRMED ErpMfgCostRollup，反向使用**新**标准成本 → GL COGS 反向与原始发出 COGS 发散。**「如 P1-MA2-024 登记」**——确认缺陷位置 |
| inventory | SPECIFIC 历史成本守卫（P1-MA2-023 复核） | ⚠️ 部分 | outgoing 路径**已锁历史成本**：`SpecificCostingStrategy.onOutgoing:92-117,138` 读 `layer.getUnitCost()`（非 balance.avgCost）——基础守卫**在位**。但 `CostAdjustmentService:108-112` 无 SPECIFIC 分支 → `applyAverageLike:123-135` 对 SPECIFIC 设 `balance.avgCost(newUnitCost)`（语义错误，SPECIFIC 应 avgCost=null）+ 不更新 per-batch `ErpInvCostLayer.unitCost` → per-batch 成本调整无效。outgoing 仍用 layer.unitCost 故**无活跃数据破坏**，降级 P2-MA4-010 |
| inventory | 到岸成本分摊算术 | ✅ PASS | `LandedCostAllocationEngine.allocate:47-99` 末行吸收余数（`:72-74` 保证 Σ=total）+ SCALE=6 HALF_UP + totalBase=0 守卫 + 空输入守卫。**注**：`createAndApplyCostAdjust:357-374` 设 line.adjustAmount 但 `CostAdjustmentService.applyLine:106-107` 重算 adjustAmount=(newCost-oldCost)×onHand 覆盖引擎值——onHand<qty 时已消耗部分成本调整静默丢失（期末简化，归 P2-MA4-010） |
| quality | **状态守卫（P0-MA2-017 复核）** | ✅ **已确认修复** | `ErpQaInspectionBizModel.passInspection:272-281` + `failInspection:283-293` 双方法均加 `requireInspectionPending:259-264`（`result!=null && result!=PENDING` 抛 ERR_INVALID_INSPECTION_STATUS_TRANSITION）+ `markPosted:266-270` 三件套 + `failInspection:291` 触发 `ncrLifecycleService.autoCreateNcrFromInspection`；`reInspect` 方法已删除（接口签名清理 + 测试验证 unknown-operation）。**修复完全落地** |
| quality | SPC 算术正确性 | ✅ PASS | `SpcControlLimitCalculator` UCL/LCL=cl±3σ̂，σ̂=R̄/d2，d2/d3/d4 标准常量表 n=2..10 正确（`:50-78`）；`SpcCapabilityCalculator.computeCp:233-238 (USL-LSL)/6σ` / `computeCpk:240-247 min((USL-μ)/3σ,(μ-LSL)/3σ)` / `computeCpm:250-262` 全正确；P 图 `AttributesControlLimitFormulas.calcP:31-49` 负下限钳 0；BigDecimal + HALF_UP 全域 |
| quality | NCR 过账异常吞咽 | ✅ PASS | `NcrPostingDispatcher.dispatchScrap:63-83` **不带 tryPost wrapper**——异常直接传播，@BizMutation 事务回滚。与 hr/assets/mfg tryPost swallow 同型根因**不适用**于 quality 主路径 |
| crm | Lead 评分算术 | ✅ PASS | `LeadScoringEngine.normalize:125-145` BigDecimal HALF_UP + `sumMaxWeighted.signum()<=0→0` 守卫 + 阈值→动作矩阵 config-gated |
| crm | Forecast/PriceRule/Funnel 算术 | ✅ PASS | `ForecastAggregator` money 全 BigDecimal（weightedAmount=revenue×probability/100 scale 2）；`PriceRuleEngine.applyRule:149-164` discount path `BigDecimal.valueOf(discountPercent).divide(100,6,HALF_UP)`→factor 无损；`FunnelAggregationEngine` 整数比率 double→round4 精度足够 |
| crm | DECIMAL↔Double 类型（P1-MA1-009 复核） | ⚠️ **建议降级** | 7 列 ORM 仍 double（`app-erp-crm.orm.xml:921,922,1248,1452,1496,1497,1498`），getter 返回 Double。**但实测 Java 使用**：`ForecastAggregator:196-197` BigDecimal accuracy→doubleValue 损失 ≤1e-15（scale 4 不可见）；`PriceRuleEngine:156` BigDecimal.valueOf(Double) 2 位百分数无损往返；money 字段全 BigDecimal 不受影响。**「如 P1-MA1-009 登记」但建议 MR1 裁决降 P2**——缺陷为 ORM 一致性/卫生非活跃正确性 bug |

**裁决**：核销/匹配/SPC 算术正确性 PASS；upsertBalance 并发安全已修复落地（P0-MA2-020）；状态守卫已修复落地（P0-MA2-017）；**核心新缺陷在 inventory 到岸成本反向过账悬挂**（P1-MA4-020——reverse 方向无 sweep 覆盖，REQUIRES_NEW 独立提交 + 编排层吞咽致 GL/库存发散）；STANDARD 红冲成本不变量 P1-MA2-024 + 多币种凭证 P1-MA2-002/009 复核「如登记」。

### 领域 3：类型和契约质量（裁决：**PASS（crm DECIMAL↔Double 建议降级 + inventory 5-strategy locationId fallback P3）**）

| 域 | 控制点 | 裁决 | 证据 |
|----|--------|------|------|
| purchase | BigDecimal 货币类型安全 | ✅ PASS | PaymentSettler/ThreeWayMatcher/PurAcctDocProvider 无 double/float 误用；`readDecimal:107-116` 显式 BigDecimal/null→ZERO 处理 |
| purchase | PostingDispatcher 返回类型契约 | ✅ PASS | 3 dispatcher 统一返回 boolean（与 assets 6 Long/3 boolean 漂移不同） |
| sales | VoucherFact 单 amount 字段（P1-MA2-009） | ⚠️ 维持 | 单一字段确认——多币种分离缺陷归 finance 引擎层（P1-MA2-009） |
| inventory | BigDecimal 货币类型安全 | ✅ PASS | `ErpInvConfigs.roundCost` 一致应用；除法恒定 HALF_UP + 显式 scale；无 double/float 货币 |
| inventory | 5-strategy locationId fallback 不一致 | ⚠️ P3 | FIFO/LIFO/SPECIFIC/BATCH/WeightedAverage 错用 `move.getDestWarehouseId()/getSourceWarehouseId()` 作 locationId fallback（跨域值），MovingAverage/Standard 正确用 `getDestLocationId()/getSourceLocationId()`。窄触发（line.locationId==null && move.locationId!=null）致 orphan all-zero balance 行——无数量/成本破坏，归 P2-MA4-010 |
| quality | BigDecimal + 状态常量 | ✅ PASS | inspectionResult/ncr-status/disposition-type 全经 ErpQaConstants 常量；无字面值漂移；`IErpPurReturnBiz.save(Map,IServiceContext)` 契约稳定 |
| crm | DECIMAL↔Double（P1-MA1-009） | ⚠️ 建议降级 | 7 列仍 double，但**money 字段全 BigDecimal 不受影响**，ratio/percent 字段精度损失 ≤1e-15 不可见——建议 MR1 降 P2（详领域 2） |

**裁决**：BigDecimal 货币类型安全五域扎实；crm DECIMAL↔Double 实测影响可忽略建议降级；inventory 5-strategy locationId fallback P3（归 P2-MA4-010）。**无类型不匹配致主路径破坏**。

### 领域 4：错误处理和操作安全（裁决：**PASS——异常规范化扎实，失败恢复闭环缺陷归领域 2**）

| 域 | 控制点 | 裁决 | 证据 |
|----|--------|------|------|
| purchase | NopException + ErrorCode | ✅ PASS | 全 `throw new NopException(ErpPurErrors.ERR_*)`；`erp.err.pur.*` 46+ ErrorCode + 作用域参数键（ARG_ORDER_CODE/ARG_RECEIVE_CODE 等）；**零裸异常**（grep RuntimeException/IllegalArgumentException = 0）；容错对称性精准（`isCommitmentAlreadyReleased:328-331` 精准识别已释放异常容错跳过，其他重新抛出） |
| sales | NopException + ErrorCode | ✅ PASS | `ErpSalErrors` 46 ErrorCode `erp.err.sal.*` + 强制 ARG_* 绑定；3 dispatcher 区分 NopException(LOG.warn) vs Exception(LOG.error+Throwable)；**零裸异常**（service 层 grep = 0） |
| inventory | NopException + ErrorCode | ✅ PASS | `ErpInvErrors` 30+ ErrorCode `erp.err.inv.*` + ARG_* 参数键；并发 ErrorCode 齐全（ERR_INV_CONCURRENT_DEDUCT_CONFLICT / ERR_INV_BALANCE_INSERT_CONFLICT）；O-22 敏感金额屏蔽（`InvPostingDispatcher.maskAmount:275-281`） |
| quality | NopException + ErrorCode | ✅ PASS | `ErpQaErrors` 22 ErrorCode `erp.err.qa.*` + 作用域参数键；NcrPostingDispatcher 主路径无 tryPost wrapper 异常直接传播 |
| quality | resolveStockBalance silent zero-voucher | ⚠️ P2 | `NcrPostingDispatcher.resolveStockBalance:117-131` 无余额时 LOG.warn + return null → scrapAmount=0 → GL 收零金额凭证 + posted=true。归 P2-MA4-012 |
| crm | NopException + ErrorCode | ✅ PASS | `ErpCrmErrors` 30+ ErrorCode `erp.err.crm.*`；report 路径注入守卫（`StringHelper.isValidVpath`）；render-type 白名单；temp 资源清理 delete-on-exception |

**裁决**：异常规范化五域扎实（全 NopException + 域前缀 ErrorCode + 作用域参数键 + 零裸异常）；错误传播分级正确。失败恢复闭环/告警缺陷归 P1-MA4-020（inventory reverse）+ P2-MA4-012（quality zero-voucher/cascade）。

### 领域 5：测试有效性（裁决：**FAIL——pur+sal+inv 三域异常路径系统性零覆盖（P1-MA4-021）；qa+crm 测试空洞 P2**）

| 域 | 主路径覆盖 | 异常路径覆盖 | 裁决 |
|----|-----------|-------------|------|
| purchase | **强**——TestErpPurPaymentSettlement 5 场景（部分/全额/反向核销）+ TestErpPurProcureToPayEnd 3 E2E（黄金/reverse/异常）+ TestErpPurThreeWayMatch 5 场景（strict/non-strict/无回链/容差/数量超） | ❌ 零覆盖——多币种（全 exchangeRate=ONE）/ 业财悬挂（3 dispatcher try/catch）/ PurReversalListener.rollbackReceive 不对称（P1-MA2-051）/ settle 三单匹配二次门禁（P1-MA2-003） | P1-MA4-021 |
| sales | **强**——32 测试类覆盖 Order/Delivery/Invoice/Receipt/Return/Quotation 主路径 + O2C E2E + 信用控制 3 场景 + SalReversalListener rollbackInvoice | ❌ 零覆盖——多币种（4 过账测试全 exchangeRate=ONE）/ 过账悬挂 / SalReversalListener rollbackReceipt/Return/Delivery 3/4 路径 / 6051 FX plug | P1-MA4-021 |
| inventory | **强**——TestErpInvConcurrentDeduct 6 并发测试（多线程无 oversell + UK retry）+ 7 CostingStrategy 主路径 + LandedCostAllocationEngine 5 场景 | ❌ 零覆盖——STANDARD 红冲后重估不变量（P1-MA2-024）/ 到岸成本反向悬挂（P1-MA4-020）/ SPECIFIC 成本调整层行为 / dispatcher reverse 失败 / onHand<qty 部分消耗 | P1-MA4-021 |
| quality | **强**——TestErpQaInspectionStateMachine 主路径+状态守卫 sad paths + TestErpQaNcrPosting 7 场景 + SPC 4 套件公式数值精确断言 | ❌ 零覆盖——NCR posting 失败 / SAL_RETURN / zero-voucher / SPC cascade 失败 / CONDITIONAL 终态守卫 / reverseNcr RETURN | P2-MA4-012 |
| crm | **强**——TestErpCrmForecastAndScoring 6 测试 + TestFunnelAggregationEngine 8 测试 + TestPriceRuleEngine 9 测试（BigDecimal scale-insensitive 断言） | ❌ 零覆盖——DECIMAL↔Double 精度负向 / stageName 快照 / accuracyOf 中间值（仅 1.0 happy case）/ 并发 refresh / deviationAmount | P2-MA4-013 |

**裁决**：主路径断言强度五域扎实（数值 + 凭证 + 状态 + 公式），但**异常路径系统性零覆盖**——pur+sal+inv 三域多币种/业财悬挂/成本不变量/核销门禁零覆盖（P1-MA4-021，目标 MR2）；qa+crm 异常路径空洞 P2（P2-MA4-012/013）。多币种算术正确性直接影响财务报表，测试补齐优先。

### 领域 6：可维护性和未来变更风险（裁决：**PASS（P2 watch-only 重复模式提取候选）**）

| 域 | 控制点 | 裁决 | 证据 |
|----|--------|------|------|
| purchase | 6 Processor 公共方法重复 | ⚠️ P2 | `validateTransitionFor*`(5×6=30)/`doSubmit`/`doApprove`/`doReject`/`doReverseApprove`/`doCancel`/`illegalTransition` 几乎逐字相同——提取候选 `AbstractPurApproveProcessor<T>` |
| purchase | 12 死代码 per-mutation Processor | ⚠️ P2 | Receive/Invoice/Payment/Return/Requisition 各 2（Reject+WithdrawApproval）+ Order 2 = 12 Java 类存在但 INLINE xbiz 未 inject() 引用——「如 P2-MA2-054 登记」 |
| sales | 6 WithdrawApproval Java Processor 死代码 | ⚠️ P2 | 6 类完整实现 AbstractWithdrawApprovalProcessor 但 xbiz withdrawApproval 是 INLINE JavaScript 永不调用——与 P1-MA2-057 同根因 |
| inventory | 5 CostingStrategy layer-based 骨架重复 | ⚠️ P2 | FIFO/LIFO/SPECIFIC/BATCH ~85% 相同（query-consume-loop-writeback）——提取候选 `AbstractLayerBasedCostingStrategy` |
| inventory | CostingStrategy 注册模式 | ✅ PASS | `StockMoveBookkeeper.initStrategyRegistry:94-103` registry 模式，新增方法=register(strategy) 零 if/switch——AP-01 正面范例 |
| quality | SPC dead computation | ⚠️ P2 | `SpcRuleEngine.evaluateRules:151-154` 计算 oneSigma/twoSigma 4 变量但规则 1-4 不消费（Western Electric 5-8 未实现） |
| crm | ForecastAggregator.resolveStageName stub | ⚠️ P2 | `:171-173 return null` 无 TODO——`ErpCrmForecastLine.stageName` 永久 NULL（对比 FunnelAggregationEngine:275 正确快照） |

**裁决**：成本方法策略可扩展性良好（registry 模式）；主要可维护性风险是 Processor/Dispatcher/Strategy 重复模式（五域同型，归 P2-MA4-010 watch-only 提取候选）。

### 领域 7：自动化和防护覆盖（裁决：**FAIL——R2d 未覆盖 daoFor(ErpFin/ErpInv/ErpPur) + 无算术回归门控（P2-MA4-011）**）

| 域 | 控制点 | 裁决 | 证据 |
|----|--------|------|------|
| purchase | R2d 覆盖 daoFor(ErpMd*) | ✅ PASS | `nop-compliance-checker.sh:154-160` 命中 `ErpPurOrderProcessor:302` + `ErpPurPaymentProcessor:228` |
| purchase | R2d 未覆盖 daoFor(ErpFin*) | ❌ FAIL | `ErpPurOrderProcessor:314 daoFor(ErpFinAccountingPeriod)` + `ErpPurPaymentProcessor:240` 漏检——与 assets P2-MA4-007 同型 |
| sales | R2d 覆盖 | ✅ PASS | 命中 `ErpSalOrderProcessor:377 daoFor(ErpMdSubject)` |
| sales | R2d 未覆盖 daoFor(ErpFin*) | ❌ FAIL | `ErpSalOrderProcessor:389 daoFor(ErpFinAccountingPeriod)` 漏检 |
| inventory | R2d 未覆盖 daoFor(ErpInv/ErpPur/ErpMfg) | ❌ FAIL | CostMethodResolver/StandardCostResolver（*Resolver 不在范围）+ CostAdjustmentService（*Service 不在范围）+ ErpInvLandedCostProcessor:267,473,477 daoFor(ErpPurReceive) 非 ErpMd* 漏检 |
| quality | R2a 覆盖 BizModel daoFor(ErpMd*) | ✅ PASS | 命中 `ErpQaReportBizModel:361 daoFor(ErpMdMaterial)` |
| quality | R2d 未覆盖 daoFor(ErpInv*) | ❌ FAIL | `NcrPostingDispatcher:121` + `NcrReturnOrchestrator:135` daoFor(ErpInvStockBalance) 漏检 |
| crm | R2d 覆盖 | ✅ PASS | crm 零跨域 daoFor，R2d/R2a 全清 |
| 五域 | 算术回归门控 | ⚠️ 部分 | 并发有门控（TestErpInvConcurrentDeduct）；**缺口**：多币种/STANDARD 红冲不变量/到岸成本反向悬挂/NCR zero-voucher/SPC cascade 无 CI 门控（归 P1-MA4-021 + P2-MA4-012/013 补测试后形成门控） |

**裁决**：R8 规则澄清正确；R2d 跨域 daoFor 检查**遗漏 ErpFin/ErpInv/ErpPur 方向**（五域同型扩展缺口）；算术/过账/并发回归门控存在空洞。归 P2-MA4-011。

---

## 3. MA1/MA2/MA3 已知 finding 运行时复核

> 每项标记「如登记」（无新代码层缺陷）/「已确认修复」/「发现新代码层缺陷」。

| Finding ID | 原描述 | 代码实现质量角度复核 | 终态 |
|-----------|--------|---------------------|------|
| `P0-MA1-021`（done，inv→fin 凭证直写） | CostAdjustmentPostingDispatcher.markOriginalVoucherReversed 跨域写 ErpFinVoucher | **已确认修复**——inventory 全域 grep `markOriginalVoucherReversed` = 0 matches（仅 javadoc 引用）；`CostAdjustmentPostingDispatcher:64-80` 经 `IErpFinVoucherBiz.reverse()` Facade。跨域凭证直写已消除 | 不升级（修复落地确认） |
| `P0-MA2-017`（fixed，qa 状态守卫缺失） | passInspection/failInspection/reInspect 三方法完全缺状态守卫 | **已确认修复**——`passInspection:272-281` + `failInspection:283-293` 双方法均加 `requireInspectionPending:259-264` 守卫 + `markPosted:266-270` 三件套 + `failInspection:291` 触发 autoCreateNcrFromInspection；`reInspect` 方法已删除（接口签名清理 + TestErpQaInspectionStateMachine.testReInspectActionRemoved 验证 unknown-operation）。**修复完全落地** | 不升级（修复落地确认） |
| `P0-MA2-020`（done，inv stock balance UK） | erp_inv_stock_balance 无 UK → upsertBalance 并发 split corruption | **已确认修复**——ORM UK 存在（`orm.xml:415 UK_INV_STOCK_BALANCE_NATURAL` 含 constraint= 触发 DDL）；`StockMoveBookkeeper.updateBalanceWithRetry:252-319` 三分支 + UK violation catch + evict + reload + retry；`TestErpInvConcurrentDeduct` 多线程测试断言行数=1。**修复完全落地** | 不升级（修复落地确认） |
| `P1-MA1-009`（todo MR1，crm DECIMAL↔Double 7 列） | 7 列 stdSqlType=DECIMAL vs stdDataType=double 浮点精度损失 | **如登记 + 建议降级**——ORM 7 列仍 double + getter 返回 Double 确认。**但实测 Java 使用**：money 字段全 BigDecimal 不受影响；ratio/percent 字段（commitAccuracy/discountPercent/conversionRate 等）精度损失 ≤1e-15 在 scale 2-4 不可见；`PriceRuleEngine:156 BigDecimal.valueOf(Double)` 2 位百分数无损往返。**建议 MR1 裁决降 P2**（缺陷为 ORM 一致性/卫生非活跃正确性 bug） | **建议降级 P1→P2**（MR1 裁决） |
| `P1-MA1-022`（todo MR1，9 域合并跨域 daoFor） | pur/sal/ast/inv/mnt/prj/qa/drp/aps 跨域只读 IDaoProvider | **如登记**——本审计复核确认 pur（7 站点）+ sal（2 站点）+ inv（16 站点）+ qa（3 站点）= 28 站点全部 read-only 无活跃数据破坏；crm 零跨域 daoFor 不在枚举。详 P1-MA4-022 投影 | 不升级（维持 P1，投影 P1-MA4-022） |
| `P1-MA1-029`（todo MR1，contract 跨域写半治理） | ErpCtInvoicePlanBizModel 跨域写绕 I*Biz | **如登记 + 同型复核 CLEAN**——quality `NcrReturnOrchestrator:99,117` 经 `IErpPurReturnBiz.save` / `IErpSalReturnBiz.save` Facade（Map-based builder，purchase/sales 域独占审批/库存/过账语义），**非 daoFor saveEntity 绕过**。clean for 同型根因 | 不升级（quality 侧 clean） |
| `P1-MA2-002`（todo MR1，P2P 多币种） | VoucherFact 单 amount，PurAcctDocProvider 写源币种，无 E2E 多币种证据 | **如登记**——`VoucherFact:14-19` 单 amount 确认；`PurAcctDocProvider:79-90` 写源币种 TOTAL_* 未分离 amountSource/amountFunctional；purchase 4 测试类全 exchangeRate=ONE。归 P1-MA4-021 测试补齐 + MR1 与 P1-MA2-002 一并裁决 | 不升级（维持 P1，测试侧归 P1-MA4-021） |
| `P1-MA2-003`（todo MR1，付款核销缺三单匹配复核） | PaymentSettler.settle 仅校验 approveStatus=APPROVED 不复核三单匹配 | **如登记**——`requireInvoiceForSettle:152-157` 仅校验 APPROVED 不复核 ThreeWayMatcher 完成。维持 P1 | 不升级（维持 P1） |
| `P1-MA2-009`（todo MR1，O2C 多币种+汇兑损益） | (a) VoucherFact 单 amount (b) RECEIPT 无 6051 FX plug | **如登记**——(a) `SalAcctDocProvider:101` 单字段确认；(b) `SalAcctDocProvider:87-91` RECEIPT 仅 Dr BANK/Cr AR 同金额无 FX plug 确认；对比 `NotesReceivableAcctDocProvider:80,86-90` 已实现 FX plug 范式。4 测试全 exchangeRate=ONE。维持 MR1 | 不升级（维持 P1） |
| `P1-MA2-023`（todo MR1，SPECIFIC 历史成本守卫） | SPECIFIC 成本方法缺历史成本守卫 | **如登记 + 部分降级**——outgoing 路径**已锁历史成本**（`SpecificCostingStrategy.onOutgoing:92-117` 读 layer.getUnitCost()）。新发现：CostAdjustmentService 无 SPECIFIC 分支致语义错误 + per-batch 层不更新，但**无活跃数据破坏**（outgoing 仍用 layer.unitCost）→ 降级 P2-MA4-010 | 部分降级（outgoing 守卫在位，CostAdjustment 语义错误降 P2） |
| `P1-MA2-024`（todo MR1，STANDARD 红冲成本不变量） | STANDARD 红冲成本跨重估破缺 | **如登记**——`StandardCostingStrategy.onIncoming:43/onOutgoing:70` 重解析 standardCostResolver.resolve 忽略传入 unitCost；reverse 流程传 unitCost 被静默丢弃。确认缺陷位置 | 不升级（维持 P1） |
| `P1-MA2-049`（todo MR1，Quotation/Rfq reverseApprove→SUBMITTED） | reverseApprove 设 SUBMITTED 违反 owner doc 强制 REJECTED | **如登记**——`ErpPurQuotation.xbiz:97` + `ErpPurRfq.xbiz:97` 仍设 SUBMITTED。状态机业务正确性归 A2.8 | 不升级（维持 P1） |
| `P1-MA2-050`（todo MR1，INLINE 缺 isCancelled 守卫） | 6 实体 INLINE reject/withdrawApproval 缺 docStatus 守卫 | **如登记**——6 实体 INLINE 均仅校验 `status!=='SUBMITTED'` 无 `docStatus!=='CANCELLED'`。状态机业务正确性归 A2.8 | 不升级（维持 P1） |
| `P1-MA2-051`（todo MR1，rollbackReceive 不对称） | PurReversalListener.rollbackReceive 仅设 posted=false 保留 APPROVED | **如登记**——`PurReversalListener:112-123` 确认仅设 posted=false 保留 APPROVED（javadoc:117 deliberate），与其他三实体降级 REJECTED 不对称 | 不升级（维持 P1） |
| `P1-MA2-056`（todo MR1，Contract reverseApprove→SUBMITTED） | ErpSalContract.xbiz reverseApprove 设 SUBMITTED | **如登记**——`ErpSalContract.xbiz:97` 仍设 SUBMITTED 违反 owner doc 强制 REJECTED | 不升级（维持 P1） |
| `P1-MA2-057`（todo MR1，INLINE withdrawApproval + Contract 缺守卫） | 6 实体 INLINE withdrawApproval 缺守卫 | **如登记 + 新发现死代码**——6 实体 xbiz withdrawApproval 均仅校验 `status==='SUBMITTED'` 缺守卫确认；**新发现**：6 个 Java WithdrawApprovalProcessor 完整实现但 xbiz INLINE 永不调用（死代码，归 P2-MA4-010） | 不升级（维持 P1，死代码归 P2-MA4-010） |
| `P2-MA3-034`（todo MR2，StockTake COUNTING vs CONFIRMED） | owner doc drift | **如登记**——文档层 drift，非代码缺陷 | 不升级（维持 P2） |
| `P2-MA3-035`（todo MR2，冲销反向移动取负 vs code 翻转 moveType） | owner doc drift | **如登记**——文档层 drift | 不升级（维持 P2） |

**裁决**：20 项已知 finding 运行时复核 **17 项「如登记」无升级 + 3 项「已确认修复」（P0-MA1-021 / P0-MA2-017 / P0-MA2-020）**；**1 项复核建议降级**（P1-MA1-009 crm DECIMAL↔Double 实测影响可忽略，建议 MR1 降 P2）；**1 项部分降级**（P1-MA2-023 SPECIFIC outgoing 守卫在位，CostAdjustment 语义错误降 P2-MA4-010）。

---

## 4. P0-P3 finding 清单（按严重性排序）

### 4.1 P1 finding（3 项）

| Finding ID | 域 | 描述 | 严重性 | 影响 | 修复方式 | 目标 MR |
|-----------|-----|------|-------|------|---------|---------|
| `P1-MA4-020` | inventory | **到岸成本反向过账业财悬挂（reverse 方向无 sweep 覆盖）**：`ErpInvLandedCostProcessor.doReverseApprove:204-217` 包 `postingDispatcher.reverse(landedCost)` 于 `catch(Exception){ LOG.warn/error }`——`voucherBiz.reverse` 经 `@Transactional(REQUIRES_NEW)` 独立提交。若 GL 反向已提交（REQUIRES_NEW commit）后 postingDispatcher.reverse 抛出被吞咽，或反向编排后续步骤（costAdjustmentService.reverseCostAdjust）失败：**GL 已反向但库存成本层未反向 + posted 标志状态不一致 → 业财不一致无自动恢复**。**关键差异**：reverse 方向 **DeferredPostingSweepJob 不覆盖**（sweep 仅扫 forward PENDING ErpFinPostingException）——forward tryPost 失败至少有 sweep 兜底，**reverse 失败无任何自动重试/告警**。GL 缺反向凭证但库存成本层仍反映原到岸成本分摊 → 库存估值高估 / GL 库存科目低估 / 期末试算不平衡。与 P1-MA4-004/007/010/013 family 同型根因（业财悬挂 + 编排层吞咽），但 MA2/A4.1b/A4.2/A4.3 审 forward tryPost，本审**reverse 方向编排层**——MA2/前序 A4 未覆盖。非 P0：(1) 失败需 reverse 操作（罕见）+ REQUIRES_NEW commit 后失败（特定时序）；(2) 可经期末试算平衡发现；(3) 操作员发起 reverse 提供可见性。 | major（业财悬挂需运营介入，reverse 方向无自愈路径——与 assets 折旧 forward posted=false 可重跑自愈不同，reverse 已提交 GL 无法自动回滚） | MR1 裁决——方案 A（推荐）catch 收窄为仅文档化"GL 已反向"幂等场景（pre-check isReversed 标志）+ 其他异常重新抛出由 @BizMutation 事务回滚覆盖；方案 B 调整编排顺序：cost-layer reverse 先于 GL reverse（GL reverse 成为最后提交动作，不一致窗口 bounded）；方案 C reverse-pending 状态 + sweep 覆盖 reverse 方向。触及会计保护区域，修复须独立 plan-audit + 人工确认 | MR1 |
| `P1-MA4-021` | pur+sal+inv | **pur+sal+inv 过账/核销/成本链路测试有效性系统性不足（多币种零覆盖 + 业财异常悬挂零覆盖 + 成本不变量零覆盖 + 核销门禁零覆盖）**：(a) **多币种零覆盖**——purchase 4 测试类（TestErpPurInvoicePosting/PaymentSettlement/ProcureToPayEnd/ThreeWayMatch）+ sales 4 过账测试（TestErpSalInvoicePosting/ReturnPosting/OrderToCashEnd/ReceiptSettlement）+ inventory 过账测试全部 `exchangeRate=BigDecimal.ONE` 单币种，无多币种 E2E；凭证行级 `amountSource/amountFunctional/debitAmount/creditAmount` 未断言，致 P1-MA2-002/009 多币种 bug 对测试不可见；(b) **业财异常悬挂零覆盖**——pur 3 + sal 3 + inv 5 PostingDispatcher tryPost catch-swallow 路径无 mock post 抛异常→断言 posted=false 测试；(c) **STANDARD 红冲成本不变量零覆盖**——P1-MA2-024 StandardCostingStrategy 重解析忽略传入 unitCost，无 STANDARD_REVALUATION 后 reverse 断言；(d) **SPECIFIC 成本调整层行为零覆盖**——P1-MA2-023 CostAdjustmentService 无 SPECIFIC 分支；(e) **PurReversalListener.rollbackReceive 不对称零覆盖**——P1-MA2-051 receive APPROVED+posted=false 悬挂无测试；(f) **SalReversalListener 3/4 rollback 路径零覆盖**——仅 rollbackInvoice 覆盖，rollbackReceipt/Return/Delivery 无测试；(g) **settle 三单匹配二次门禁零覆盖**——P1-MA2-003 非严格模式价格超容差 settle 无 assertThrows；(h) **到岸成本反向悬挂零覆盖**——P1-MA4-020 无 mock reverse 抛异常测试。 | major（测试空洞致 P1-MA2-002/003/009/023/024/051 + P1-MA4-020 + P1-MA2-032 family 对测试不可见；多币种/成本算术正确性直接影响财务报表） | MR2 补——(1) 多币种 pur P2P + sal O2C + inv 过账 E2E（exchangeRate≠ONE + 凭证行级 amountSource≠amountFunctional 断言，闭合 P1-MA2-002/009）；(2) PostingDispatcher 过账悬挂测试（mock post 抛异常→断言 posted=false + 终态不受影响，闭合 family 测试可见性）；(3) STANDARD 红冲后重估不变量测试（seed standard=10, outgoing 5, revalue 15, reverse → 断言 reverse 用 10 非 15，闭合 P1-MA2-024）；(4) SPECIFIC 成本调整层测试（闭合 P1-MA2-023）；(5) PurReversalListener.rollbackReceive 不对称测试（闭合 P1-MA2-051）；(6) SalReversalListener rollbackReceipt/Return/Delivery 对称测试；(7) settle 三单匹配二次门禁负向测试（闭合 P1-MA2-003）；(8) 到岸成本反向悬挂测试（闭合 P1-MA4-020）。与 A4.1a P1-MA4-002 + A4.1b P1-MA4-005 + A4.2a P1-MA4-009 + A4.2b P1-MA4-011 + A4.3 P1-MA4-014 + A4.4 P1-MA4-019 互补不重叠 | MR2 |
| `P1-MA4-022` | pur+sal+inv | **pur+sal+inv Processor/过账链路跨域 daoFor 绕 I\*Biz（同 P1-MA1-022 根因在五域过账/Processor 投影）**：pur+sal+inv 三域 25 站点跨域只读直访——**purchase**：`ErpPurOrderProcessor:302,314` + `ErpPurPaymentProcessor:228,240`（ErpMdSubject/ErpFinAccountingPeriod）+ `ErpPurDashboardBizModel:155,184,230`（ErpMdPartner）；**sales**：`ErpSalOrderProcessor:377,389`（ErpMdSubject/ErpFinAccountingPeriod）；**inventory**：`CostMethodResolver:61,70` + `StandardCostResolver:75,85,99` + `CostAdjustmentService:210,219,233,291` + `ErpInvLandedCostProcessor:267,473,477` + `ErpInvDashboardBizModel:152,372,384,396`（ErpMdMaterial/AcctSchema/Warehouse + ErpMfgCostRollup + ErpPurReceive）。违反 AGENTS.md「跨实体访问应通过 I\*Biz 接口」+ data-dependency-matrix.md §5.3。与 P1-MA1-022（9 域）+ P1-MA4-003/006/008/012/015 同根因，本批是其在 pur/sal/inv 过账/Processor/budget-hook/dashboard 的投影（P1-MA1-022 原枚举 pur/sal/inv 含部分站点，本审计复核确认 + 显式登记 dashboard/Resolver/Service 扩展）。**注**：qa 3 站点已在 P1-MA1-022 9 域枚举内不重复；crm 零跨域 daoFor 不在范围。read-only 无活跃数据破坏。 | major（架构边界违规，无活跃数据破坏——只读查询） | MR1——同 P1-MA1-022 方案 A（master-data/finance/manufacturing/purchase I\*Biz 补便捷只读方法后迁移 25 站点）或方案 B（永久接受登记 posting-exemptions.md）。**不重复计入 MR2**（同 P1-MA1-022/P1-MA4-003/006/008/012/015 一并裁决） | MR1 |

### 4.2 P2 finding（4 项 watch-only）

| Finding ID | 描述 | 处置 |
|-----------|------|------|
| `P2-MA4-010` | **五域可维护性热点合并**：(a) **purchase 6 Processor 公共方法逐字 copy-paste**——validateTransitionFor*(5×6=30)/doSubmit/doApprove/doReject/doReverseApprove/doCancel/illegalTransition 几乎逐字相同（提取候选 AbstractPurApproveProcessor<T>）；(b) **purchase 12 死代码 per-mutation Processor**——Receive/Invoice/Payment/Return/Requisition 各 2（Reject+WithdrawApproval）+ Order 2 = 12 Java 类存在但 INLINE xbiz 未 inject()（「如 P2-MA2-054 登记」，MR1 修复 P1-MA2-050 时一并）；(c) **sales 6 WithdrawApproval Java Processor 死代码**——完整实现但 xbiz withdrawApproval INLINE 永不调用（与 P1-MA2-057 同根因）；(d) **inventory 5 CostingStrategy layer-based 骨架 ~85% 重复**（FIFO/LIFO/SPECIFIC/BATCH 提取候选 AbstractLayerBasedCostingStrategy）；(e) **inventory 5-strategy locationId fallback 不一致**——FIFO/LIFO/SPECIFIC/BATCH/WeightedAverage 错用 getDestWarehouseId 作 locationId fallback（窄触发 orphan all-zero balance 行，无数值破坏）；(f) **inventory SPECIFIC CostAdjustmentService 无 SPECIFIC 分支**——applyAverageLike 对 SPECIFIC 设 avgCost（语义错误但 outgoing 用 layer.unitCost 无破坏，P1-MA2-023 部分降级）；(g) **inventory 到岸成本 onHand<qty 部分消耗**——CostAdjustmentService.applyLine 重算覆盖引擎 allocatedAmount，已消耗部分成本调整丢失（期末简化未文档化）；(h) **quality SPC dead computation**——SpcRuleEngine.evaluateRules 计算 oneSigma/twoSigma 4 变量但规则 1-4 不消费；(i) **crm ForecastAggregator.resolveStageName:171-173 stub**——return null 无 TODO，ErpCrmForecastLine.stageName 永久 NULL（对比 FunnelAggregationEngine:275 正确快照，报表/dashboards 读 null）。 | watch-only，MR2 顺手——方案 A（推荐）(a)(d) 抽象基类收敛公共方法 + (b)(c) MR1 修复 P1-MA2-050/054/057 时一并处置 + (e) 改 5 strategy fallback 为 getLocationId + (f) 加 SPECIFIC 分支或文档标注 + (i) 实现 resolveStageName 快照；方案 B 接受现状登记 posting-exemptions.md |
| `P2-MA4-011` | **五域自动化防护缺口合并**：(a) **compliance checker R2d 未覆盖 daoFor(ErpFin/ErpInv/ErpPur)**——`nop-compliance-checker.sh:154-160` R2d 仅扫 Processor/Dispatcher/Engine 中 `daoFor(ErpMd*)`，**未覆盖** `daoFor(ErpFin*)`（pur OrderProcessor:314 + PaymentProcessor:240 / sal OrderProcessor:389）+ `daoFor(ErpInv*)`（qa NcrPostingDispatcher:121 + NcrReturnOrchestrator:135）+ `daoFor(ErpPur*)`（inv ErpInvLandedCostProcessor:267,473,477）+ *Resolver/*Service 命名不在范围（inv CostMethodResolver/StandardCostResolver/CostAdjustmentService）——P1-MA4-022 同型站点无自动防护；(b) **无算术回归门控**——多币种（P1-MA2-002/009）/ STANDARD 红冲不变量（P1-MA2-024）/ 到岸成本反向悬挂（P1-MA4-020）/ NCR zero-voucher（P2-MA4-012）/ SPC cascade 无 CI 门控。并发有门控（TestErpInvConcurrentDeduct）。与 assets P2-MA4-007 + finance P2-MA4-002 同型扩展。 | watch-only，MR2 顺手——方案 A（推荐）(a) R2d 扩展扫描 `daoFor(Erp(Fin|Inv|Pur|Sal|Mfg)` 全前缀 + *Resolver/*Service 命名（与 P2-MA4-002/007 同型扩展）；(b) P1-MA4-021 测试补齐后形成 CI 门控 |
| `P2-MA4-012` | **quality NCR/SPC 链路可观测性缺陷合并 2 项**：(a) **NcrPostingDispatcher silent zero-voucher fallback**——`resolveStockBalance:117-131` 无余额时 LOG.warn + return null → scrapAmount=0 → `dispatchScrap:69 executor.postEvent` 仍构造 amount=0 凭证 + posted=true，违反 owner doc §NCR 财务影响规则表隐含非零语义（GL 收零金额凭证噪声 + NCR 误标 posted=true，无 ErrorCode 拒绝路径让运营感知）；(b) **SpcOutOfControlHandler post-commit cascade swallow + dead defense**——`cascadeNcrAndCapa:83-91` post-commit `catch(Exception){LOG.warn}` 吞咽 NCR/CAPA 建单失败（失控 SPC 样本无对应 NCR，运营仅靠 LOG 主动巡查）+ `SpcRuleEngine.evaluate:124-129` 外层 try/catch 是 dead defense（cascadeNcrAndCapa 仅注册 afterCommit callback）+ `findExistingSpcNcr:140-151 catch(Exception)return null` 幂等 guard 失败致可能重复 NCR。 | watch-only，MR1 顺手——(a) resolveStockBalance 返回 null 时 dispatchScrap 抛 ErrorCode（ERR_NCR_NO_STOCK_BALANCE）或 scrapAmount<=0 时拒绝 posted=true；(b) cascade 失败派发 IErpSysNotificationBiz 告警 + 移除 dead defense + findExistingSpcNcr 失败 LOG.warn 保留幂等跳过 |
| `P2-MA4-013` | **crm Forecast 链路缺陷合并 2 项**：(a) **ForecastAggregator.refreshForecast TOCTOU 并发缺口**——`:50-102 requireOpen(period)` 不锁期间行（无 SELECT FOR UPDATE / 无 version 检查 / 无去重 guard），两并发 refresh 都过 requireOpen 都 delete+insert → 重复 Forecast 行 + team/company rollup 双计（self-healing 下次 refresh 重建，但手工+job 并发时金额暂时虚高）；(b) **crm 测试空洞**——DECIMAL↔Double 精度负向 / ForecastLine.stageName 快照 / accuracyOf 中间值（仅 1.0 happy case）/ 并发 refresh / deviationAmount 零覆盖（致 stageName stub P2-MA4-010[i] + 并发缺口未被发现）。 | watch-only，MR2——(a) refreshForecast 起始获取期间悲观锁或加 ErpCrmForecast(periodId,ownerId,teamId) UK fail-fast；(b) 补 4 类测试 |

### 4.3 P3 finding

- inventory 5-strategy locationId fallback（FIFO/LIFO/SPECIFIC/BATCH/WeightedAverage 错用 warehouseId 作 locationId fallback）——窄触发 orphan 行无数值破坏，归 P2-MA4-010(e) 不单独登记。
- quality SPC `SpcControlLimitCalculator.lookupD2:235-242` subgroupSize>10 静默回落 n=10（保守，无 LOG.warn 标注近似）——即时风险低，不单独登记。

---

## 5. 综合裁决

### 5.1 Verdict

**⚠️(P1)**——五域代码实现质量**核心扎实**（编排健壮性 + 核销/匹配/SPC 算术正确性 + upsertBalance 并发安全已修复 + 状态守卫已修复 + 跨域写经 Facade + 异常规范化 + BigDecimal 货币类型安全七面），但**失败恢复闭环（P1-MA4-020 inventory 到岸成本反向过账悬挂，reverse 方向无 sweep 覆盖，MA2/前序 A4 未覆盖）+ 测试有效性（P1-MA4-021 pur+sal+inv 异常路径系统性零覆盖：多币种/业财悬挂/成本不变量/核销门禁）+ 架构边界（P1-MA4-022 pur+sal+inv 跨域 daoFor 投影）** 三项 P1 缺陷需 MR1/MR2 修复。

### 5.2 P0 评估

**无 P0**——无活跃数据破坏路径：
- **PaymentSettler/ThreeWayMatcher 算术**：双余额校验 + 价格容差 HALF_UP——经主路径测试覆盖
- **upsertBalance 并发**：P0-MA2-020 已修复落地（UK + retry + 多线程测试）
- **STANDARD 红冲成本不变量**（P1-MA2-024）：silent 算术不一致但需重估事件介于两操作间（窄触发）+ reverse 是操作员发起可见 + 发散有界可经库存估值 vs GL 对账发现
- **到岸成本反向悬挂**（P1-MA4-020）：需 reverse 操作（罕见）+ REQUIRES_NEW commit 后失败（特定时序）+ 可经试算平衡发现 + 操作员可见——非 P0
- **多币种凭证/汇兑损益**（P1-MA2-002/009）：归 finance 引擎层 deferred；凭证本身 Dr/Cr 平衡不破坏资产负债表
- **跨域只读 daoFor**：read-only 无写入
- **crm DECIMAL↔Double**：money 字段全 BigDecimal 不受影响，ratio/percent 损失 ≤1e-15 不可见

### 5.3 剩余风险

1. **多币种算术缺陷对测试不可见**（P1-MA4-021 + P1-MA2-002/009）：pur+sal+inv 全部测试 exchangeRate=ONE，一旦下游 finance 引擎多币种换算有 bug，五域测试不会发现。MR2 优先补多币种 E2E + 行级断言。
2. **inventory 到岸成本反向悬挂**（P1-MA4-020）：reverse 方向无 sweep 覆盖，GL/库存发散需运营人工处置——MR1 优先收窄 catch 或调整编排顺序。
3. **STANDARD 红冲成本不变量**（P1-MA2-024）：标准成本重估后反向移动 GL COGS 发散——虽非 P0 但属高风区域（成本算术直接影响财务报表），MR1 优先。
4. **compliance checker R2d 漏检**（P2-MA4-011）：daoFor(ErpFin/ErpInv/ErpPur) 方向无静态守卫，MR2 扩展 R2d。

### 5.4 与 MA1/MA2/MA3/A4.1a-A4.4 交叉去重

- **P1-MA4-020** 与 P1-MA4-004/007/010/013 family 同型根因（业财悬挂 + 编排层吞咽）但**不同代码方向**（reverse 方向 vs forward tryPost），MA2/前序 A4 审 forward 未覆盖 reverse——新登记，MR1 协同
- **P1-MA4-021** 与 A4.1a P1-MA4-002 + A4.1b P1-MA4-005 + A4.2a P1-MA4-009 + A4.2b P1-MA4-011 + A4.3 P1-MA4-014 + A4.4 P1-MA4-019 互补不重叠（各域/各审计测试空洞独立登记）
- **P1-MA4-022** 同 P1-MA1-022/P1-MA4-003/006/008/012/015 根因在 pur/sal/inv 投影，MR1 一并裁决不重复计入 MR2
- **P2-MA4-010/011** 与 A4.1a P2-MA4-001/002 + A4.1b P2-MA4-003 + A4.2a P2-MA4-004 + A4.2b P2-MA4-005 + A4.3 P2-MA4-006/007 + A4.4 P2-MA4-008/009 同型（可维护性热点 + 自动化防护），独立登记

**pur+sal+inv+qa+crm 五域 MA4 代码质量抽样终态在此收口：3 P1 + 4 P2，零 P0。** roadmap A4.5 推进至 done（待独立 closure audit）。
