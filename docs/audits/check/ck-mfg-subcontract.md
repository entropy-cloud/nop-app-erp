# ck-mfg-subcontract — manufacturing「委外/批次追溯/差异分析」切片实现代码检查报告

> 工作项：C4.3。执行日期：2026-08-25。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-manufacturing/erp-mfg-service/src/main/java` 委外/追溯/差异切片 24 个手写生产文件——委外链 16 类（Facade `ErpMfgSubcontractOrderBizModel` + `ErpMfgSubcontractOrderProcessor` + 9 个 per-mutation Processor（IssueMaterials/ReceiveFinished/PostProcessingFee/ReverseCompletion/SubmitForApproval/Approve/Reject/ReverseApprove/WithdrawApproval）+ `SubcontractPostingDispatcher` + 3 个 AcctDocProvider（Issue/Receipt/Fee）+ `MfgSubcontractReversalListener` + 状态机 Bean 2 类（Document/Approval）+ 裸 `ErpMfgSubcontractOrderLineBizModel`）+ 批次追溯 3 类（`genealogy/BatchGenealogyWriter`/`BatchGenealogyTracer` + `ErpMfgBatchGenealogyBizModel`）+ 差异链 6 类（`costing/ProductionVarianceCalculator` + `ErpMfgCostVarianceBizModel` + `ErpMfgCostVarianceCalculateVariancesProcessor` + `ProductionVarianceDispatcher` + `ProductionVarianceAcctDocProvider`）+ Forecast 链 3 类（`ErpMfgForecastBizModel`/`ErpMfgForecastLineBizModel`/`ErpMfgForecastStateMachine`）+ 接线核对（`_vfs/erp/mfg/beans/app-service.beans.xml` 全 bean 注册 + `ErpMfgSubcontractOrder.xbiz` 审批动作委托 + `fin/deferred-posting-sweep.batch.xml` 兜底链）。跨域核实：`ErpInvStockMoveReverseProcessor`（inverseMoveType/反向仓库装配）、`ErpInvStockMoveGenerateMoveProcessor`（幂等 + doConfirm/doComplete）、`ErpInvStockMoveProcessor`（reservesOnConfirm/bookkeeper 仓库解析）、`ErpInvBatch` ORM（warehouseId mandatory）、`ErpFinPostingProcessor`（recordPostFailure/幂等命中返回 null）、`ErpFinDeferredPostingRetryHelper`（O-16 补偿语义）、`AbstractApproveProcessor`/`AbstractSubmitForApprovalProcessor`/`AbstractRejectProcessor`（validateNotCancelled 骨架守卫）、`module-manufacturing/model/app-erp-manufacturing.orm.xml`（7 实体列集/versionProp）、`module-inventory/model/app-erp-inventory.orm.xml`（ErpInvBatch）；测试盲区核实（`TestErpMfgSubcontractReverse`/`TestErpMfgBatchGenealogy`）。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.2/B4）。核心链逐文件深读（委外 Facade 506 行全读、三段动作 Processor/红冲链/三 Provider/listener/dispatcher 全读、基因链 Writer/Tracer/BizModel 全读、差异 Calculator 517 行全读）+ 平台与跨域源码实证（inverseMoveType 仅翻转 INCOMING↔OUTGOING、ErpInvBatch 仓库强制、fin 引擎失败落异常表 + sweep 重试、common 骨架 validateNotCancelled）+ arm-index 复用裁决。
> 切片边界：工单/报工归 C4.1、BOM/MRP/CRP/仿真归 C4.2 不重复检查。`CostRollupService.aggregateSubcontractCost`（标准侧归集，C4.2 范围）仅作本切片 SUBCONTRACT 差异标准侧的传导面注记；`MrpPlanLineReleaseSubcontractRequestProcessor`/`MrpReleaseService` 归 C4.2。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-mfg3-001（D6/D8）委外成品入库计价仅含加工费——材料成本未计入产成品存货，1408 委外物资科目永久残留材料成本净额

- **控制点**：`app/erp/mfg/service/processor/ErpMfgSubcontractOrderProcessor.java#computeReceiptUnitCost`（L395-402：`BigDecimal fee = nz(order.getProcessingFee()); BigDecimal total = fee; return total.divide(receivedQty, 4, RoundingMode.HALF_UP);`——**单位成本 = 订单头加工费 / 收货数量，发料材料成本完全不参与**）+ `#generateReceiptMove`（L384 `ml.setUnitCost(unitCost)` 将该值写入入库移动单行 → inventory ledger totalCost = 加工费）+ `SubcontractPostingDispatcher#buildReceiptEvent`（L246 `line.put(KEY_FINISHED_COST, ledger.getTotalCost().abs())`——SR 凭证 Dr 1405/Cr 1408 金额 = 加工费）
- **证据**：三段凭证净额推演（科目分解经 Provider 实证：SI Dr 1408/Cr 1401（材料成本，来自发料移动单流水）；SR Dr 1405/Cr 1408（仅加工费）；SF Dr 1408/Cr 2202（加工费））：**1408 净余额 = 材料成本 + 加工费 − 加工费 = 材料成本 ≠ 0**——委外物资中转科目每单沉淀一笔永不结清的材料成本；同时产成品库存估值（入库移动单 unitCost）与 SR 凭证 Dr 1405 均只含加工费，材料成本既未进存货也未结转。owner doc `subcontracting.md §成本构成`「委外成品成本 = 发出材料成本 + 加工费」与 `§成本分配规则`（成品入库结转委外物资）不满足。代码 javadoc 自述「材料成本取委外行加工费汇总近似（本期简化，精确材料成本归集归 N=2 计划 2026-07-13-0455-2）」——但**实现连注释声称的近似都没做**（未汇总任何行级金额，只用头 processingFee），且该偏离未记载于 owner doc `§实现约定`（该节仅记载「加工费为订单头级单一金额」）。行级 `unitProcessingFee`/`amount` 列存在但运行时零消费（见 P3-CK-mfg3-016）。SR 段受 `erp-mfg.subcontract-posting-enabled` 门控，但**入库移动单 unitCost 与产成品计价不受 config 门控**——默认配置下缺陷即生效。
- **问题**：用户可见正确性 + GL 一致性双重缺陷：产成品存货低估（缺材料成本）+ 1408 科目余额永不归零（对账断裂）。后续销售出库成本、差异分析均继承低估的产成品单价。
- **建议修复方向**：`computeReceiptUnitCost` 分子改为「发料移动单流水材料成本合计 + 头加工费」（dispatcher 已有 `loadLedgers(issueMoveId)` 现成读数通道）；或 owner doc 显式裁决 Deferred 并补记实现约定（须同步修代码注释与实现不符处）。
- **arm-index 裁决**：新增（grep「computeReceiptUnitCost/委外成品单位成本/材料成本 计价」零命中；A2.6a 声称的「委外 reverseCompletion 双路径最终一致」不含计价维度）。

### P1-CK-mfg3-002（D8/D6）reverseCompletion 永不反向委外收货（成品入库）移动单——红冲后材料退回但成品仍滞留库存，数量守恒破坏

- **控制点**：`app/erp/mfg/service/processor/ErpMfgSubcontractOrderProcessor.java#generateReceiptMove`（L366-374：`StockMoveRequest` 只设 `setDestWarehouseId(destWarehouseId)`，**从不设 sourceWarehouseId**）+ `#canSafelyReverse`（L219-225：非 INCOMING 类型要求 `original.getSourceWarehouseId() != null`——MANUFACTURE 收货移动单恒为 null → **恒返回 false**）+ `#reverseOneMove`（L196-201：`if (!canSafelyReverse(original)) { LOG.warn("委外红冲跳过库存移动反向…"); return; }`）+ 跨域实证 `ErpInvStockMoveReverseProcessor#buildReverseRequest`（L49-55：反向单 `destWarehouseId = original.getSourceWarehouseId()`——为 null 时反向 MANUFACTURE 入库记账无目标仓库，mfg 侧守卫确为必要防护，但副作用是**永远跳过**）
- **证据**：`reverseCompletion` 时序：SI 凭证红冲 ✓ → SR/SF 凭证红冲 ✓ → 发料（OUTGOING）移动单反向 ✓（材料退回仓库）→ **收货（MANUFACTURE）移动单被 canSafelyReverse 恒拒跳过**（仅 LOG.warn，无告警派发）→ posted=false + CANCELLED。结果：材料已回库 + 成品数量仍在产成品仓 = **库存双计**；SR 凭证已红冲但库存不回滚 = GL 与库存反向失配。owner doc `subcontracting.md §实现约定：委外红冲完工` 声明「反向**两段**库存移动（issue/receipt 经 IErpInvStockMoveBiz.reverse）」——实现只反向一段。arm-index A2.6a 裁决记录「三个候选 P0 经证据证伪：委外 reverseCompletion 双路径保证最终一致」——**库存维度该结论不成立**（双路径指 GL 域级/财务监听两路径，均不含收货库存回滚）。测试 `TestErpMfgSubcontractReverse` 对红冲后余额**零断言**（grep balance/assertBalance 零命中），盲区确认。
- **问题**：委外红冲闭环在库存侧断裂：多组织/多仓场景库存虚增不可自动恢复；「委外发出=收回+损耗」守恒式破坏。
- **建议修复方向**：收货反向需补目标仓库语义——反向单显式指定 destWarehouseId（如经委外单留存发料仓或成品仓配置），并推动 inventory 侧支持「MANUFACTURE 反向冲减入库」或改用负数量 INCOMING 冲销语义（须与 inv 域联合裁决）；最低限度：跳过时派发告警（对齐正向 `dispatchFailureAlert` 范式）而非仅 LOG.warn。
- **arm-index 裁决**：新增（grep「canSafelyReverse/收货 移动单 反向跳过」零命中）。

### P1-CK-mfg3-003（D3/D5）审批族 Pattern B custom override 绕过骨架 validateNotCancelled——CANCELLED 委外单可被 submit/approve 复活并重新进入发料链

- **控制点**：`app/erp/mfg/service/processor/ErpMfgSubcontractOrderApproveProcessor.java#approve`（L30-35：custom override 直调 `processor.requireOrder → validateTransitionForApprove → doApprove`，**不走基类模板**）对照 `app/erp/common/service/AbstractApproveProcessor.java#approve`（L27-41 模板含 `validateNotCancelled(entity, context)` 步骤）+ `ErpMfgSubcontractOrderProcessor#validateTransitionForApprove`（L255-262：**仅审批轴** `approvalStateMachine.assertCanApprove(SUBMITTED)`，无 docStatus 守卫）+ `#cancel`（L127-133：**只翻 docStatus=CANCELLED，approveStatus 不动**）+ `#validateTransitionForSubmit`（L237-244 同型仅审批轴；`ErpMfgSubcontractOrderDocumentStateMachine#assertCanSubmit` javadoc 自认「本边为元数据/守卫语义，**无独立 docStatus 侧守卫接线**」）
- **证据**：复活路径一（审核复活）：创建 → submitForApproval（approveStatus=SUBMITTED, docStatus=SUBMITTED）→ cancel（docStatus=CANCELLED，**approveStatus 仍 SUBMITTED**）→ approve：审批轴 SUBMITTED 通过 → `doApprove` 写 docStatus=APPROVED——作废单复活为已审核，可继续 `issueMaterials` 发料出库。复活路径二（重提复活）：创建（UNSUBMITTED）→ cancel（DRAFT 态合法）→ submitForApproval：审批轴 UNSUBMITTED 通过 → docStatus=SUBMITTED。同型第三边：CANCELLED+SUBMITTED 单可被 reject 翻成 docStatus=REJECTED（终态漂移）。Withdraw/ReverseApprove processor 同为 Pattern B 绕过（withdraw javadoc 显式声明「不引入骨架的 validateNotCancelled，保真既有行为」）。三个抽象骨架（Approve/SubmitForApproval/Reject）均含 validateNotCancelled（module-common-service 实证）——custom override 使 `isCancelled` 钩子成为死代码路径。
- **问题**：终态可复活（D3 清单项）。对照 workorder 切片 P1-CK-mfg-002（驳回互锁，P1）同级的用户可见生命周期管理断裂：取消的委外单可复活并触发真实库存出库。同族前例 P1-MA2-059（assets 全 5 INLINE 动作缺 isCancelled 守卫，已修复）——**mfg 委外站点未被该修复覆盖**（该修复针对 assets xbi INLINE 动作）。
- **建议修复方向**：三个 Pattern B override 补 `docStatus==CANCELLED 拒绝`（或改为真正覆写骨架单步而非绕过模板）；`doSubmit` 前置 docStatus ∈ {DRAFT, REJECTED} 守卫（Bean 边已定义，接线即可）。
- **arm-index 裁决**：新增（带同族注记 P1-MA2-059/P1-MA2-050 族——purchase/sales/assets 站点已修，mfg subcontract 新站点）。

### P1-CK-mfg3-004（D6/D10）基因链输入批次按产成品仓解析——原料批次在原料库时系统性解析失败，前向追溯/召回报告静默为空

- **控制点**：`app/erp/mfg/service/genealogy/BatchGenealogyWriter.java#doWrite`（L129 `String warehouseId = outputLine.getDestWarehouseId();`——**用产出行的入库仓作为输入批次查找仓**）+ `#resolveInputLot`（L213-219：`findBatchByNo(batchNo, issueLine.getMaterialId(), warehouseId)`）+ `#findBatchByNo`（L266-276：`q.addFilter(eq("warehouseId", warehouseId))`）+ ORM 实证 `module-inventory/model/app-erp-inventory.orm.xml` ErpInvBatch `warehouseId` 列 **mandatory="true"**（L901，批次台账按仓库分账）
- **证据**：正常制造场景原料批次收货于原料仓 W1、产成品入库于成品仓 W2（`ErpMfgWorkOrderLine.destWarehouseId`）：`resolveInputLot` 在 W2 查 (batchNo, material) → 恒 null → `continue` **静默跳过（无日志，对照 catch 路径有 LOG.error+告警）** → 基因链零输入行 → `forwardTrace`（成品→原料）返回空、`recallReport` 无法经基因边扩散。owner doc `batch-genealogy.md §业务目标`「原料批次 → 生产批次 → 成品批次全链路追溯」与召回场景 1/2 在多仓布局下整体失效。测试 `TestErpMfgBatchGenealogy` 的 `seedBatch` 固定 `batch.setWarehouseId(WAREHOUSE_ID)` 且工单 destWarehouse 同仓（L416/L458）——单仓测试布局掩盖。领料行本身无仓库列（orm 实证，issue 行仅 batchNo），唯一正确的输入仓来源应为领料单头仓库（`ErpMfgMaterialIssue.warehouseId`，测试 L474 可见该字段存在）。
- **问题**：追溯主场景（前向追溯/召回范围识别）静默失效——比 best-effort 缺口（Decision 3 已裁决异常路径）更糟：这不是失败，是查找维度错误导致的系统性空链，且零可观测信号（见 P3-CK-mfg3-015 日志缺口）。
- **建议修复方向**：`resolveInputLot` 的仓库维度改用领料单头 `issue.getWarehouseId()`（`findIssueLinesWithBatch` 已持有 issue 实体，可下传）或去掉仓库过滤改 (batchNo, materialId) 全仓匹配 + orgId 过滤；跳过路径补 LOG。
- **arm-index 裁决**：新增（grep「resolveInputLot/destWarehouse 批次/基因链 输入批次」零命中；MA4 系列审计覆盖 genealogy 但未登记该维度）。

### P1-CK-mfg3-005（D6/D4）SUBCONTRACT 差异实际侧 `wo.subcontractCost` 全仓零 writer——实际委外费恒 0，差异行失真并可过账错误方向凭证

- **控制点**：`app/erp/mfg/service/costing/ProductionVarianceCalculator.java` L196-206（`BigDecimal actSubcontract = nz(wo.getSubcontractCost());`——实际侧唯一来源）对照 grep 全 `erp-mfg-service/src/main/java` `setSubcontractCost` **仅 2 处且均写 CostRollupLine**（`CostRollupService.java` L130/L350，标准侧）——**`ErpMfgWorkOrder.subcontractCost` 无任何生产 writer**（读者仅 `ErpMfgWorkOrderProcessor` L435 / `AbstractErpMfgMaterialIssueProcessor` L124 的 totalCost 聚合 + 本 Calculator）
- **证据**：`ErpMfgConstants` L230-235 对 `CONFIG_SUBCONTRACT_COST_AGGREGATION_ENABLED` 的 javadoc 声明「开时按物料聚合已过账（COMPLETED）委外订单加工费，**按产量分摊填入 subcontractCost**」——实现（CostRollupService.aggregateSubcontractCost）只填 **rollup 行**（标准侧），从不写工单实际侧；声明语义与实现不符。后果链：开启委外费归集（std≠0）后，任一完工工单手动/自动计算差异 → SUBCONTRACT 行 = 0 − std×completed（**全额假 favorable**）→ `ProductionVarianceDispatcher.dispatchIfApplicable` 按要素净额过账（subcontractNet<0 → 借 1417 在制品/贷 1416 制造差异-委外）——**基于恒零实际数的错误方向凭证**。两 config（aggregation 默认 false、variance-auto-calc 默认 false）同时开启即触发；手动 `ErpMfgCostVariance__calculateVariances` 入口在仅开启 aggregation 时即可产出失真行（不过账）。
- **问题**：第 6 类差异（plan 2026-07-14-0035-1）实际侧数据通道缺失——差异分析报表失真 + 潜在 GL 错误入账；C4.1 已登记的 `P1-CK-mfg-003`（红冲不回退工单成本字段）家族的「工单成本四要素完整性」维度在 subcontract 要素上从源头即为空。
- **建议修复方向**：补 WO 侧归集 writer（按完工工单聚合其关联 COMPLETED 委外单加工费，镜像 `aggregateSubcontractCost` 的 inv 侧口径，注意经委外单 productId↔工单 productId 的关联语义裁决）；或差异行实际侧临时改从委外单直接聚合（绕过 wo 字段）；或 owner doc 显式裁决该行为 Deferred 并修正 constants javadoc 与 variance-analysis.md 公式注记。
- **arm-index 裁决**：新增（grep「subcontractCost」arm-index 仅 RC 计划文本 1 处，无 finding；「MrpEngine scheduledReceipt 恒 0」P2-MA4-005 热点为同形不同字段——零 writer 家族注记）。

### P2-CK-mfg3-006（D2/D8，同型 P1-CK-mfg-005 族新站点）委外红冲三段 GL + 库存反向失败吞异常后仍推进终态 CANCELLED+posted=false——入口守卫永久封口且无告警通道

- **控制点**：`app/erp/mfg/service/processor/ErpMfgSubcontractOrderProcessor.java#reverseOneVoucher`（L164-176：`catch (Exception e)` 仅 `LOG.warn`（NopException）/`LOG.error`（其他），**无 dispatchFailureAlert**——对照正向 `SubcontractPostingDispatcher#dispatchFailureAlert` L171-188 有 notify 通道）+ `#reverseOneMove`（L189-212 同型吞咽）+ `#validateCanReverse`（L143-151：`COMPLETED && posted==true` 双前置——红冲后 posted=false + CANCELLED 终态，**重试入口永久关闭**）+ `ErpMfgSubcontractOrderReverseCompletionProcessor#reverseCompletion`（L19-26：吞咽后无条件 `doReverseCompletion`）
- **证据**：时序：SF 红冲失败（如期间锁定 `ERR_PERIOD_CLOSED`）→ 吞 → SR/SI 照常红冲 → 状态翻 CANCELLED/posted=false。结果：GL 侧残留未红冲凭证 + 单据已终态 + 入口封死 + 仅日志无通知。与 P1-CK-mfg-005（领料 reverseConfirm 同型，P1）同根因：lesson 09 红冲方向变体。差异点：委外侧 finance sweep 对 REVERSAL 类型异常有重试（`ErpFinPostingProcessor#recordReverseFailure` 落异常表 + `deferred-posting-sweep.batch.xml` 重试，引擎侧实证）——**GL 侧悬挂可被 sweep 兜底**（降 P2 依据），但库存反向（issue 段失败）无任何兜底且封口后不可重试；且 sweep 重试成功后 `posted` 不会回写 true（helper 仅标记异常记录 RETRIED），单据状态与 GL 状态仍分叉。
- **问题**：红冲失败的可观测性（无告警）+ 库存段不可恢复 + sweep 成功后 posted/状态不回写。
- **建议修复方向**：`reverseOneVoucher`/`reverseOneMove` catch 分支派发 `mfg.subcontract-reverse-failure` 告警（复用 dispatcher notify 范式）；库存段失败考虑中止红冲保持可重试（与 mfg-005 统一裁决）。
- **arm-index 裁决**：同型登记（P1-CK-mfg-005 族，委外站点新增计数；MfgSubcontractReversalListener 粗粒度回滚维度另有 P2-MA4-005 watch-only 复用注记见 §跨域关联）。

### P2-CK-mfg3-007（D5/D10）三段动作入参边界缺失——仓库参数可空直传库存、收货数量无上限守卫、非正数量静默替换

- **控制点**：`app/erp/mfg/service/entity/ErpMfgSubcontractOrderBizModel.java#issueMaterials`（L54-58：`sourceWarehouseId` 为 `@Optional`，processor 无默认无校验 → `generateIssueMove` L341 `request.setSourceWarehouseId(null)` 直传）+ `#receiveFinished`（L62-67：`destWarehouseId` 同型可空；委外单 ORM **无仓库列**，无回退来源）+ `ErpMfgSubcontractOrderReceiveFinishedProcessor#receiveFinished`（L29-32：`receivedQty == null || signum() <= 0` → **静默替换**为 `sumLineQuantity()` 或 `BigDecimal.ONE`——误输 0/负数得到「成功」的兜底收货）+ 全链**无「收货 ≤ 发料」守卫**（owner doc §关键业务规则 3「收货数量不能超过发料数量（扣除损耗）」零落地；损耗 successor 但发料量上限不含损耗部分仍可执行）
- **证据**：sourceWarehouseId=null 的 OUTGOING 移动单进 inventory `doConfirm → validateAvailable → bookkeeper.upsertBalance(move, line, null, ...)`——默认配置下报 `ERR_AVAILABLE_INSUFFICIENT`（warehouseId 参数为 null 的困惑性报错）；`erp-inv.allow-negative-stock=true` 时经负库存短路放行 → 记账产生**无仓库维度的余额行**（数据污染）。destWarehouseId=null 的 MANUFACTURE 入库同型（bookkeeper onIncoming 用 null destWarehouse）。receivedQty=1 兜底：调用方漏传数量时按 1 收货并翻 RECEIVED——后续 postProcessingFee 照常完成，收货数量与实际脱钩。发料侧同样无「发料 ≤ BOM 标准×(1+损耗率)」守卫（§发料控制规则 2；BOM 展开本身为 successor，但按行 quantity 全量发料无上限）。负数静默替换维度同型 P3-CK-mfg-012。
- **问题**：入参边界（D5）+ 边界静默（D10）+ 守卫缺失（设计规则 3 的可执行部分）。
- **建议修复方向**：两仓库参数前置非空校验（抛 `ERR_SUBCONTRACT_WAREHOUSE_MISSING` 类业务码）；`receivedQty` 非正直接拒绝（对齐 mfg-012 修复方向）；补「收货 ≤ 发料合计」守卫（损耗扣除部分留 successor 裁决）。
- **arm-index 裁决**：新增（负数静默维度同型注记 P3-CK-mfg-012；grep「receiveFinished 上限/收货 校验」零命中）。

### P2-CK-mfg3-008（D6）基因链同批次多领料行去重丢量——inputQty 只记首个匹配行

- **控制点**：`app/erp/mfg/service/genealogy/BatchGenealogyWriter.java#doWrite`（L152-165：`Set<String> usedInputLots = new HashSet<>(); … if (!usedInputLots.add(inputLot.getId())) { continue; }` 后 `inputQty = nz(issueLine.getIssuedQuantity()).multiply(ratio)`——**同 lot 第二行起静默跳过，不累加数量**）
- **证据**：同一原料批次分两张领料单（或同单两行）领料（部分领料的标准场景）：行 1 lot A qty 10、行 2 lot A qty 5 → 基因链仅记录 10×ratio，丢失 5×ratio。owner doc `batch-genealogy.md §业务目标`「批次构成记录：每个产出批次的**各输入批次数量**」与数据模型 inputQty 语义（投入数量）不满足——该批次的真实投入被少计。去重意图应是「同批次合并为一行」，正确实现应为数量累加后写一行，而非丢弃后继行。部分完工比例分摊（Decision 1 已裁决近似）不受影响，此为独立缺陷。批次数量链守恒（D6 检查项）：Σ genealogy inputQty < 实际领料消耗。
- **建议修复方向**：按 lotId 聚合 issuedQuantity 后写单行（Map<lotId, qty> 累加），或在行上按 (lotId, materialId) 合并。
- **arm-index 裁决**：新增（grep「usedInputLots/去重 数量」零命中）。

### P2-CK-mfg3-009（D8/D2）差异重算链 reverseIfExists 以「posted 行存在」为门控 + 红冲吞异常 + post 幂等命中返回 null 三者复合——一次红冲失败后工单差异永久悬挂

- **控制点**：`app/erp/mfg/service/posting/ProductionVarianceDispatcher.java#reverseIfExists`（L134-142：`q.addFilter(eq("posted", true)); q.setLimit(1); … if (posted.isEmpty()) return;`——**用数据行 posted 替代「凭证存在」判定**）+ `#dispatchIfApplicable`（L106-110：`String voucherId = executor.postEvent(event); if (voucherId != null) markPosted(lines);`——fin-003 同型语义：幂等命中返回 null 时 markPosted 跳过）+ 引擎实证 `ErpFinPostingProcessor#process`（幂等命中 `return null`，ck-finance-posting P1-CK-fin-003 控制点）
- **证据**：复合时序（正常重算的边缘失败路径）：① 首次计算过账成功（行 posted=true + 凭证 `{wo}-PV` 存在）→ ② 手动重算：`reverseIfExists` 红冲失败（期间锁定等，L148-153 吞异常 warn）→ 旧凭证**未撤销**仍存在 → ③ `deleteByWorkOrder` 删旧行 → ④ `calculateVariances` 新行（posted=false）→ ⑤ `dispatchIfApplicable`：anyUnposted=true → `postEvent` 幂等命中**旧凭证**返回 null → `markPosted` 跳过 → 新行永久 posted=false → ⑥ 再次重算：`reverseIfExists` 门控查 posted=true 行 → **空 → 提前 return，不再尝试红冲** → 循环回 ⑤。终态：`{wo}-PV` 凭证为旧金额（isReversed=false）+ 数据行新金额 posted=false——owner doc `variance-analysis.md §一致不变量`（仅 1 条未红冲 NORMAL 凭证 + 行额一致 + 全行 posted=true）**三者全破**且无自愈路径。设计文档 §重算幂等实现注记 裁决了「红冲失败孤儿凭证可观测（log warn 归 finance 5.1 兜底）」，但未覆盖「门控失效后红冲永不重试 + post 幂等命中使 posted 永不回写」的复合悬挂。
- **问题**：单次红冲失败后重算链永久卡死（数据行/凭证/标志三者分叉），仅 LOG.warn 可观测。
- **建议修复方向**：`reverseIfExists` 门控改为按凭证存在性判定（`IErpFinVoucherBiz` 反查 `{wo}-PV` NORMAL 未红冲凭证，与 dispatcher 的 billHeadCode 对称），或门控保留 posted 行 + 追加「存在未红冲同码凭证」分支；与 fin-003 的返回值三态化联动修复。
- **arm-index 裁决**：新增（复合机制新维度；fin-003 单点已登记、本条为其与门控缺陷的复合场景立案）。

### P2-CK-mfg3-010（D8，同族 P2-CK-mfg-007）findFirmedRollupLine 无 orgId/期间过滤——跨组织标准成本混用 + 差异计算取「全局最新 FIRMED」

- **控制点**：`app/erp/mfg/service/costing/ProductionVarianceCalculator.java#findFirmedRollupLine`（L345-370：`new QueryBean().addFilter(eq("status", FIRMED))` **全量载入所有组织的 FIRMED rollup 头**，内存按 businessDate DESC 排序后逐头查行取首个 materialId 匹配——无 `eq("orgId", wo.getOrgId())`、无按工单业务日期对齐的期间过滤）对照 ORM `ErpMfgCostRollup` 含 `orgId` 列（orm 实证）
- **证据**：多组织部署下 A 组织工单可能取到 B 组织产品的标准成本（同产品两组织分别 FIRMED）；单组织下「全局最新 FIRMED」意味着**完工后新滚算的标准会回溯改变旧工单重算差异的基准**（设计 §核心计算逻辑字面即「取最近一条 FIRMED」——期间维度按设计宽松，orgId 维度则为隔离缺失）。与 P2-CK-mfg-007（齐套/看板无 orgId）同族不同站点。性能维度（全量载入 + 逐头查行 N+1）另计 P3-CK-mfg3-018。
- **问题**：多组织标准成本串用（D8）；差异基准的时点语义按设计执行（登记为设计事实，不裁决）。
- **建议修复方向**：补 `eq("orgId", wo.getOrgId())`；期间对齐（rollup.businessDate ≤ wo.businessDate 取最近）建议与 owner doc 联合裁决。
- **arm-index 裁决**：新增（同族注记 P2-CK-mfg-007/P2-CK-fin2-007；grep「findFirmedRollupLine orgId」零命中）。

### P2-CK-mfg3-011（D5，同型 P1-CK-pur-003 族）切片实体全裸 CrudBizModel 无状态守卫——基因链/差异行/委外单可经通用 CRUD 手改

- **控制点**：`app/erp/mfg/service/entity/` 下 `ErpMfgSubcontractOrderBizModel`（除 6 个命名动作外 CRUD 裸放——COMPLETED 单可经 `update_` 改 `processingFee`（直接改变已过账 SF 凭证口径）/`posted`（洗掉红冲守卫前置））、`ErpMfgSubcontractOrderLineBizModel`（18 行裸类）、`ErpMfgBatchGenealogyBizModel`（**追溯记录可手改/手删**——召回报告的证据链可被覆写）、`ErpMfgCostVarianceBizModel`（**差异行金额与 posted 可手改**——打破 §一致不变量）、`ErpMfgForecastBizModel`/`ErpMfgForecastLineBizModel`（APPROVED 预测行可改 forecastQty——MRP 需求源失真）
- **证据**：全部为 `extends CrudBizModel<T>` 零 `defaultPrepareSave/Update/Delete` 覆写；`save_`/`update_`/`delete_` 通用 mutation 对任意状态实体开放。
- **问题**：同 P1-CK-pur-003/P2-CK-mfg-006 全域同型——本切片新增关注点：基因链（审计证据链）与差异行（GL 对账依据）两类**只读性最强**的记录同样裸放。
- **建议修复方向**：终态/已过账守卫（对齐 mfg-006 修复方向）；基因链/差异行建议整体禁用通用 update/delete（append-only）。
- **arm-index 裁决**：同型登记（P1-CK-pur-003 族，mfg3 站点新增计数）。

### P2-CK-mfg3-012（D3，同型 P2-CK-mfg-010）reverseApprove 无 docStatus 守卫——ISSUED/RECEIVED/COMPLETED 委外单可翻 approveStatus=REJECTED 并洗掉审批审计字段

- **控制点**：`app/erp/mfg/service/processor/ErpMfgSubcontractOrderReverseApproveProcessor.java#reverseApprove`（L31-36）→ `ErpMfgSubcontractOrderProcessor#validateTransitionForReverseApprove`（L273-280：仅 `approvalStateMachine.assertCanReverseApprove(APPROVED)`——approveStatus 审核后全程保持 APPROVED，**ISSUED/RECEIVED/COMPLETED 全通过**）+ `#doReverseApprove`（L321-326：翻 REJECTED + `setApprovedBy(null)`/`setApprovedAt(null)`，docStatus 不动）
- **证据**：对已完工（COMPLETED+posted）委外单调 reverseApprove：守卫全过 → approveStatus=REJECTED + 审计字段清空 + docStatus 仍 COMPLETED。产生双轴矛盾态 + 审批链审计被洗。后续 submit 可达（REJECTED 审批轴放行）→ docStatus COMPLETED→SUBMITTED（与 Bean 矩阵 COMPLETED 仅出边 reverseCompletion 矛盾）。发料/收货/加工费动作仅查 docStatus 不查 approveStatus，链条不断但语义混乱。
- **问题**：同 P2-CK-mfg-010（workorder 站点）——委外站点新增计数；额外维度：COMPLETED 终态单可经 reverseApprove→submit 间接离开终态（配合 P1-CK-mfg3-003 的 docStatus 无守卫根因）。
- **建议修复方向**：`validateTransitionForReverseApprove` 增加 docStatus 白名单（≤APPROVED / 未发料）。
- **arm-index 裁决**：同型登记（P2-CK-mfg-010 族）。

### P3-CK-mfg3-013（D10）traceChain 深度边界 off-by-one——恰好 depth 条边的链抛 MAX_DEPTH_EXCEEDED 并丢弃全部已收集结果

- **控制点**：`app/erp/mfg/service/genealogy/BatchGenealogyTracer.java#traceChain`（L88-92：`while (!frontier.isEmpty()) { if (currentDepth >= depth) throw new NopException(ERR_MFG_GENEALOGY_MAX_DEPTH_EXCEEDED)…`——**叶子层节点仍会进入 frontier**，边数恰等于 depth 的合法链在全部边已收集后仍抛错且不返回部分结果）
- **证据**：线性链 E 条边、depth=E：处理第 E 层前沿时收集全部 E 条边，nextFrontier=E 层叶子节点非空 → 循环顶 currentDepth=E>=depth → 抛错——用户得到错误而非已完整的追溯结果；E<depth-1 才正常返回。默认 depth=50，深链（多级 BOM 逐级加工）恰在边界时整链不可用。
- **建议修复方向**：先查前沿是否有出边再判超深，或超深时返回部分结果 + 截断标记。
- **arm-index 裁决**：新增（grep「traceChain maxDepth/GENEALOGY_MAX_DEPTH」arm-index 零命中）。

### P3-CK-mfg3-014（D6/D8）基因行 outputQty 每行重复写全量 completedQty + recallReport 口径失真（lotStatus 硬编码 RELEASED、中间品计入受影响成品）

- **控制点**：`app/erp/mfg/service/genealogy/BatchGenealogyWriter.java#doWrite`（L172-174：循环内每条输入行 `row.setOutputQty(completedQty)`——N 个输入批次产生 N 行、每行 outputQty=全量完工数，Σ outputQty = N×completedQty）+ `app/erp/mfg/service/entity/ErpMfgBatchGenealogyBizModel.java#collectAffectedIfFinishedGood`（L119-127：`forwardTrace(lotId)` 非空即视为受影响——**中间品批次同样作为产出出现**，一并计入「受影响成品」；L125 `affected.setLotStatus(ErpMfgConstants.LOT_STATUS_RELEASED)` **硬编码**而非 `lot.getStatus()`——QUARANTINE 批次报告为 RELEASED）
- **证据**：设计数据模型 outputQty 语义为产出数量；下游若对行求和（报表/对账）则产出放大 N 倍——批次链两端数量一致（D6 检查项）不成立。召回报告把所有「被生产出来」的批次（含中间品）都列为受影响，且状态字段失真（代码注释自认「受影响候选」口径，属宽报）。
- **建议修复方向**：outputQty 仅在首行写全量或改在行上记录分摊产出；recallReport 按 lot 是否终端产出（无下游 input 引用）过滤 + lotStatus 取实际值。
- **arm-index 裁决**：新增（部分复用注记：MfgSubcontractReversalListener 粗粒度回滚/productCode 占位归 P2-MA4-005；本条三个小维度未在该热点清单内）。

### P3-CK-mfg3-015（D2/D10）基因链写入静默跳过路径零日志——best-effort 语义下缺口不可观测

- **控制点**：`app/erp/mfg/service/genealogy/BatchGenealogyWriter.java#doWrite`（L121-123 `outputLine == null → return`；L130-132 `warehouseId == null → return`；L135-137 `issueLines.isEmpty() → return`；L155-157 `resolveInputLot == null → continue`——**四条静默短路均无 LOG**，对照异常路径有 LOG.error + notify）
- **证据**：owner doc Decision 3 裁决 best-effort「由 config 开关 + **日志可观测性**兜底」——但跳过路径（而非异常路径）恰好无日志：与 P1-CK-mfg3-004 叠加时（输入批次找不到），基因链静默变空，运营完全不可感知。
- **建议修复方向**：各 return/continue 分支补 LOG.warn（含 wo.code 与跳过原因）。
- **arm-index 裁决**：新增（RC-R1.3 修复覆盖的是 catch 分支告警，不含静默跳过分支）。

### P3-CK-mfg3-016（D5/D4）死列族——postedStatus/amountSource/amountFunctional/totalAmount/line.unitProcessingFee/line.amount 运行时零消费

- **控制点**：ORM `ErpMfgSubcontractOrder` 列集（postedStatus/amountSource/amountFunctional/totalAmount）+ `ErpMfgSubcontractOrderLine`（unitProcessingFee/amount）对照 grep 全 service main 代码：`getPostedStatus/setPostedStatus/getAmountSource/getAmountFunctional` **零命中**；`setTotalAmount`/`setUnitProcessingFee` 仅 `MrpReleaseService` L205/L220 写 **常量 ZERO**（MRP 释放路径零初始化），无任何读者
- **证据**： posted 只用布尔 `posted`，过账状态机粒度列 postedStatus 从未写入；金额三列（amountSource/amountFunctional/totalAmount）与行级加工费两列全链零消费——委外加工费唯一口径是头 processingFee（实现约定已裁决），但行级/本位币列残留误导（用户在前端填了行级加工费也不生效，直接强化 P1-CK-mfg3-001 的触发面）。
- **建议修复方向**：随 P1-CK-mfg3-001 修复联合裁决（行级加工费启用则消费列，否则 ORM 清列走 dual-agent-approval）；短期 xmeta 层隐藏或标注只读。
- **arm-index 裁决**：新增（同「声明字段零消费」家族注记 P2-CK-mfg-008；列清理属 ORM 保护区域）。

### P3-CK-mfg3-017（D1）SUBJECT_FINISHED_GOODS 常量名/注释与值相悖——名为产成品实为 1401 原材料，共享常量误用陷阱

- **控制点**：`app/erp/mfg/service/ErpMfgConstants.java` L284-285（`/** 产成品存货科目编码（与 InvAcctDocProvider.SUBJECT_INVENTORY 一致，完工入库 Dr）*/ String SUBJECT_FINISHED_GOODS = "1401";`——本项目 COA 1401=原材料（SubcontractIssueAcctDocProvider javadoc「Cr: 原材料存货 1401」）、1405=产成品（SR Provider「Dr: 产成品 1405」））对照 L311-312（委外侧被迫另造 `SUBJECT_SUBCONTRACT_FINISHED_GOODS = "1405"` 且注释「对齐 SUBJECT_FINISHED_GOODS」——两常量名同义值不同）
- **证据**：当前 SI/SR 凭证科目方向均**正确**（SI 贷 1401 原材料 ✓、SR 借 1405 产成品 ✓，见 §验证为正确），但常量语义陷阱已在委外链显性化（需绕开命名另建常量）；后续任何按名取用 `SUBJECT_FINISHED_GOODS` 作产成品借方的代码会错记 1401。
- **建议修复方向**：重命名为 SUBJECT_RAW_MATERIAL_INVENTORY（或改值 1405 并核对 ManufacturingIssuePostingDispatcher 消费点——该点 mfg-021 已登记）。
- **arm-index 裁决**：新增（mfg-021 为硬编码不可配置维度，本条为命名/注释误导维度）。

### P3-CK-mfg3-018（D9）差异标准侧全量载入 + 逐头查行 N+1；基因链逐领料单查行；recallReport 无深度上限

- **控制点**：`ProductionVarianceCalculator#findFirmedRollupLine`（L349-368：`findAllByQuery(eq(status,FIRMED))` 全量头载入 + for 内逐头 `findAllByQuery` 查行）+ `BatchGenealogyWriter#findIssueLinesWithBatch`（L223-248：逐领料单 `findAllByQuery` 查行，N=单工单领料单数，有界）+ `ErpMfgBatchGenealogyBizModel#recallReport`（L87-105：while 循环**无 maxDepth 上限**——对照 traceChain 有 50 上限；visited 防环但宽深图无界展开）
- **证据**：rollup 头随产品×期间增长（每次差异计算全量载入全部组织的 FIRMED 头）；recallReport 对超深基因图无爆炸防护（traceChain 同场景有 ERR_MAX_DEPTH 防护，口径不一致）。
- **建议修复方向**：rollup 查询下推（join/子查询按 materialId+orgId+最近 businessDate）；recallReport 复用 resolveMaxDepth。
- **arm-index 裁决**：新增（同型家族 pur-011/sal-026/inv-020/mfg-014）。

## 跨域关联影响面注记（不新建 finding）

| 已登记 finding | mfg3 受影响面 | 结论 |
| --- | --- | --- |
| `P1-CK-fin-003`（post() 幂等命中返回 null vs dispatcher null=失败） | `SubcontractPostingDispatcher#dispatchFeePosting` L140-144 `String voucherId = executor.postEvent(event); if (voucherId != null) markPosted(...)`——**同型受影响面确认（全域第 9 站点）**：postProcessingFee 外层事务在 dispatch 后失败回滚（REQUIRES_NEW 凭证已提交、posted 翻转随主事务回滚）→ 重试 postProcessingFee 幂等命中返回 null → posted 永不置 true → COMPLETED+posted=false + SF 凭证存在 → reverseCompletion 守卫（posted==true）永久封口。补充：fee 过账失败经引擎落 `ErpFinPostingException` 后由 `deferred-posting-sweep` 重试成功时，helper 仅标 RETRIED **不回写委外单 posted**——单据状态与 GL 同样分叉（此维度归 fin-003 修复联动核） | **同型受影响面确认**，随 fin-003 三态化修复联动；issue/receipt 段无 posted 消费不受此语义影响 |
| `P1-CK-mfg-003`（领料红冲不回退工单成本字段） | 委外单**无** WO 侧成本累计列（materialCost 类），红冲无 mfg 侧累计字段可回退——不受该 finding 直接影响；委外侧对应缺口是收货库存不回滚（已立案 P1-CK-mfg3-002）与材料成本不入 FG 计价（P1-CK-mfg3-001） | **不受影响确认**；对应缺口由 mfg3-001/002 承担 |
| `P2-CK-mfg-005`（REQUIRES_NEW 凭证先于主事务提交） | `issueMaterials`/`receiveFinished`/`postProcessingFee` 三段均为 dispatch（内部 REQUIRES_NEW 凭证提交）→ reload → `updateEntity`（乐观锁可抛）——主事务回滚时凭证成孤儿、单据状态未推进 | **同型受影响面确认**（P2-CK-inv-012/P1-CK-pur-002 族，3 个新站点），随族修复联合裁决 |
| `P3-CK-mfg-016`（currentUserId/读 config 宽 catch 无日志） | `ErpMfgSubcontractOrderProcessor#currentUserId` L488-498 / `#readBoolConfig` L468-478、`BatchGenealogyWriter#isWriteEnabled` L278-288、`BatchGenealogyTracer#defaultMaxDepth` L120-132 | **同型登记**（全域族，4 个 mfg3 站点） |
| `P2-MA4-005`（watch-only 可维护性热点） | ① `MfgSubcontractReversalListener` 粗粒度回滚——本切片补充正确性证据：财务侧红冲**任一单段**（SI/SR/SF）即整单翻 CANCELLED+posted=false，**其余两段凭证滞留 GL 不联动红冲、两段库存移动不反向**（监听者只翻标志，L61-75），与域级 reverseCompletion 的全量红冲不对称；② `dispatchVarianceAlertIfOverThreshold` productCode 占位（L254 `String.valueOf(wo.getProductId())` 放入 productCode 键） | **复用不重复登记**（P2-MA4-005 已含两项）；监听者单段红冲的兄弟凭证滞留证据并入该 finding 修复范围参考 |
| `P3-CK-mfg-021`（mfg 过账汇率恒 ONE） | `ProductionVarianceDispatcher#buildEvent` L164 已在 C4.1 覆盖；`SubcontractPostingDispatcher` 三段 buildEvent 均透传 `order.getExchangeRate()`（非恒 ONE，L198/L232/L266） | **委外三段无此问题验证为正确**；不新增站点 |
| `P1-MA2-059`（assets INLINE 动作缺 isCancelled 守卫，已修） | 委外 Pattern B override 同根因新站点——已立案 P1-CK-mfg3-003（不复用：修复未覆盖 mfg subcontract） | **同族新站点立案**（见 003） |
| C4.2 传导注记 | `CostRollupService#aggregateSubcontractCost`（L265-288）：无 orgId 过滤、无期间窗口（全历史聚合）、`totalFee` 累加**全部** COMPLETED 单的头加工费而 `totalQty` 只累加匹配物料的行——多物料委外单的头加工费按物料重复计入标准单位成本 | **归 C4.2 立案**（CostRollupService 属 BOM/滚算切片）；此处仅登记其对 mfg3-005 标准侧的传导事实 |

## 验证为正确（显式排除，防误报）

- **D1 机械扫描全零**：切片 24 文件 `@Inject private`=0、`System.currentTimeMillis/LocalDateTime.now/new Date()`=0（统一 `CoreMetrics`）、`extends RuntimeException`=0、字典字符串 `==` 比较=0、无手编生成产物。
- **三段凭证科目方向与 owner doc §成本分配规则一致**：SI Dr 1408（config `erp-mfg.subcontract-subject-code` 可配）/Cr 1401（SubcontractIssueAcctDocProvider 贷方行 + 借方汇总，平衡）；SR Dr 1405/Cr 1408；SF Dr 1408/Cr 2202——取值经常量实证（1401/1405/1408/2202），命名误导另立案 P3-CK-mfg3-017 但**方向无误**。
- **PRODUCTION_VARIANCE 凭证借贷平衡与方向语义正确**：`ProductionVarianceAcctDocProvider#appendElementFacts` 每要素借/贷对价（unfavorable 借差异贷 WIP / favorable 借 WIP 贷差异），零额跳过；dispatcher 传 abs+direction 与 provider 消费对称。
- **正向过账失败兜底链真实存在**（dispatcher javadoc「DeferredPostingSweepJob 兜底」声明核实）：fin 引擎 `ErpFinPostingProcessor#recordPostFailure`（O-6 含非 NopException 泛化）落 `ErpFinPostingException` → `fin/deferred-posting-sweep.batch.xml`（PENDING/retryCount<3/24h 窗口）→ `ErpFinDeferredPostingRetryHelper` REQUIRES_NEW 重建 event 重试——域侧吞异常不丢失重试通道（红冲方向同型有 recordReverseFailure）；域侧另有 G3 `dispatchFailureAlert`（三段共用，notify 降级 warn）。
- **委外 8 态字典全可达无死状态**（D3/B2）：DRAFT（初始）/SUBMITTED（doSubmit）/APPROVED（doApprove + MrpRelease 豁免路径）/ISSUED/RECEIVED/COMPLETED/CANCELLED（cancel/reverseCompletion/listener）/REJECTED（doReject/doReverseApprove）均有活跃 writer；Bean 迁移矩阵与 owner doc 8 态子集一致。
- **Forecast CONSUMED 死状态已裁决**：`ErpMfgForecastStateMachine` javadoc + owner doc Decision A（refuse-dead-state + successor），不重复登记（对齐 jobcard TRANSFERRED 先例）。
- **驳回重提无双轴互锁**（对照 P1-CK-mfg-002）：subcontract `doReject` 联动写 docStatus=REJECTED（L314-319）+ submit 运行时守卫仅审批轴（REJECTED 放行）→ 驳回后重提可达（Bean javadoc 显式声明的 Subcontract 独有设计）；withdrawApproval 后 docStatus=SUBMITTED 残留但 submit/approve/cancel 路径复算均不受阻。
- **SoD 在位**：`doApprove` L306 `SoDGuard.assertApproverNotCreator`。
- **乐观锁在位（D5）**：SubcontractOrder/SubcontractOrderLine/BatchGenealogy/CostVariance/CostRollup/Forecast/ForecastLine 全部 `versionProp="version"`（orm 逐实体实证）——同单并发 issue/receive 由版本冲突回滚保护。
- **xbiz/beans 接线完整（D4）**：`app-service.beans.xml` 全部 10 processor + 2 状态机 + dispatcher/3 Provider/listener/writer/tracer/calculator 注册；`ErpMfgSubcontractOrder.xbiz` 5 审批动作委托 processor（approve/reverseApprove 带 `auth permissions`）；三 AcctDocProvider 经 finance Registry collect-beans 自动聚合（注释声明 + 范式同 ProductionVariance）。auth 仅审批动作的口径与 WorkOrder xbiz 一致（C4.1 已按同范不立案）。
- **BigDecimal/除零纪律（D6/D10）**：差异计算统一 `scale(4, HALF_UP)`；`divideSafe`/`percent`/`actualMins.signum()==0` 三处除零守卫在位；`computeReceiptUnitCost` 对 receivedQty 非正返回 ZERO（虽引发 007 的静默维度，但无 ArithmeticException 路径）。
- **差异 6 类行结构与设计公式一致**：MATERIAL_USAGE 不含价格差异（PPV 归采购入库，设计已裁决防重复计入）；VOLUME 完工=计划通常为 0 的保留行 javadoc 声明；LABOR_EFFICIENCY/RATE 分解口径与 CostRollupService 同源（工作中心均值费率）。
- **重算正常路径幂等链在位**：`reverseIfExists`（posted 行门控）→ `deleteByWorkOrder` → `calculateVariances` → `dispatchIfApplicable`（anyUnposted + 全零跳过）——正常路径不自增生凭证（C4.1 已验证，本切片 processor 侧复核一致；边缘复合悬挂见 P2-CK-mfg3-009）。
- **markPosted 脏跟踪**：`SubcontractPostingDispatcher#markPosted`/`MfgSubcontractReversalListener` 依赖 managed entity 脏提交——P2-MA4-004 watch-only 范式，不重复登记。
- **发料单次幂等键语义正确**：委外一单一发料一收货（状态机单向），`(ERP_MFG_SUBCONTRACT_ISSUE/RECEIPT, order.code)` 幂等键无 P0-CK-mfg-001 的多段碰撞问题（对照工单增量报工）。
- **`ErpInvBatch` 直写豁免沿用**：`BatchGenealogyWriter.daoFor(ErpInvBatch)` 跨域写（ensureOutputLot 建批/累量）——IErpInvBatchBiz 仅 CRUD 无所需语义，MA4 系列审计覆盖 genealogy 未按 D1 立案；连同 `findBatchByNo` 无 orgId 过滤一并归入剩余风险声明（不立案）。
- **委外 cancel 白名单正确**：{DRAFT, SUBMITTED, APPROVED}（未发料前），Bean 与 facade 一致；ISSUED 后不可 cancel（须 reverseCompletion）语义闭环。

## arm-index 复用 or 新增裁决（汇总）

- 关键符号 `computeReceiptUnitCost`/`canSafelyReverse`/`收货移动单 反向`/`resolveInputLot`/`usedInputLots`/`subcontractCost 零 writer`/`findFirmedRollupLine`/`GENEALOGY_MAX_DEPTH`/`SUBJECT_FINISHED_GOODS 1401`/`CANCELLED 复活` 在 `docs/audits/arm-index.md` **零命中 → 新增**。
- **复用（不重复登记，报告中注记）**：
  - CANCELLED 单审批轴漂移/复活 → **P1-MA2-059**（assets，resolved）——P1-CK-mfg3-003 为其未覆盖的 mfg 委外新站点（同族立案）。
  - MfgSubcontractReversalListener 粗粒度回滚 / dispatchVarianceAlert productCode 占位 → **P2-MA4-005**（watch-only 热点清单已含）——不立案，补正确性证据于注记表。
  - 红冲失败吞异常推进终态 → **P1-CK-mfg-005 族**——P2-CK-mfg3-006 同型登记（委外站点；GL 段有 sweep 兜底故按同型站点降 P2）。
  - CRUD 无守卫 → **P1-CK-pur-003 族**——P2-CK-mfg3-011 同型登记。
  - reverseApprove 无 docStatus 守卫 → **P2-CK-mfg-010 族**——P2-CK-mfg3-012 同型登记。
  - orgId 隔离缺失 → **P2-CK-mfg-007 族**——P2-CK-mfg3-010 新站点（差异标准侧）。
  - currentUserId/config 宽 catch → **P3-CK-mfg-016 族**——4 站点注记。
  - REQUIRES_NEW 凭证先提交 → **P2-CK-inv-012/P1-CK-pur-002 族**——3 站点注记。
  - 声明字段零消费 → **P2-CK-mfg-008 家族形**——P3-CK-mfg3-016 立案（死列维度）。
  - N+1/无界加载 → **P3-CK-pur-011 家族**——P3-CK-mfg3-018 立案。
  - post() 幂等 null → **P1-CK-fin-003**——dispatchFeePosting 同型受影响面注记（第 9 站点）+ P2-CK-mfg3-009 复合场景立案。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 5 | P1-CK-mfg3-001..005 |
| P2 | 7 | P2-CK-mfg3-006..012 |
| P3 | 6 | P3-CK-mfg3-013..018 |

按主维度：D6×4（001/005/008/014）、D8×4（002/009/010 + 001 跨 D6 计 D6）、D3×2（003/012）、D5×2（007/011 + 003 跨 D5 计 D3）、D2×2（006/015）、D10×2（004/013）、D4×1（016）、D9×1（018）、D1×1（017）。（精确主维度归属：001 D6、002 D8、003 D3、004 D10、005 D6、006 D2、007 D5、008 D6、009 D8、010 D8、011 D5、012 D3、013 D10、014 D6、015 D2、016 D5、017 D1、018 D9。）

同型/关联注记 8 项（fin-003 第 9 站点确认 / mfg-003 不受影响确认 / inv-012 族 3 站点 / mfg-016 族 4 站点 / MA4-005 两项复用 / mfg-021 委外三段证伪 / MA2-059 同族立案 / C4.2 传导注记）。

## 剩余风险（查了什么/没查什么）

- **已查**：切片 24 文件全部逐行深读（委外 Facade 506 行 + 10 processor + dispatcher 318 行 + 3 Provider + listener + 2 状态机；基因链 Writer 311 行/Tracer/BizModel；差异 Calculator 517 行/Dispatcher/Provider/BizModel/重算 processor；Forecast 3 类）；跨域源码实证 8 处（ErpInvStockMoveReverseProcessor 反向装配、inverseMoveType、generateMove 幂等+doConfirm/doComplete、ErpFinPostingProcessor 失败落表+幂等 null、RetryHelper O-16、common 三骨架 validateNotCancelled、deferred-posting-sweep.batch.xml、ErpInvBatch ORM）；orm 核对（7 实体列集/versionProp/无 receivedQuantity 列/ErpInvBatch warehouseId mandatory）；`setSubcontractCost`/`postedStatus`/`amountSource` 等死列全仓 grep；beans/xbiz 接线；测试盲区核实 2 处（SubcontractReverse 无余额断言、BatchGenealogy 单仓布局）。
- **未深查**：`erp-mfg-web` AMIS 页面对三段动作/追溯查询的契约 drift（归 C8.2）；`MrpPlanLineReleaseSubcontractRequestProcessor`/`MrpReleaseService` 委外释放链（归 C4.2，仅核其对 003 复活路径的 APPROVED 豁免写入）；`CostRollupService` 本体（归 C4.2，传导面已注记）；`ErpMfgReportBizModel` 是否消费基因链/差异行（报表切片）；`BatchGenealogyWriter` 对 `ErpInvBatch` 直写与 inv 域批次生命周期（收货建批/出库扣减 availableQuantity）的一致性——inv 域 bookkeeper 是否并行维护 ErpInvBatch 数量未核（若并行维护则 ensureOutputLot 累量存在双计风险，归 inv 侧 successor 裁决）；xmeta 前端必填拦截（007 的仓库参数若前端强制必填则触发面收窄但后端无守卫事实不变）；测试代码正确性（8 个相关测试文件仅用于行为交叉验证）。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-mfg3-001**（材料成本不入 FG 计价）——代码 javadoc 引用 N=2 计划（2026-07-13-0455-2 成本要素拆分）可能已裁决该简化为 successor；但该计划未读（超出本切片材料半径），且 owner doc 实现约定未记载、代码注释与实现不符三点确凿。建议主 agent 核对该计划后再定级（若显式 Deferred 可降 P2 并转 owner doc 补记）。
  2. **P1-CK-mfg3-002**（收货移动单永不反向）——`canSafelyReverse` 是明知防护（javadoc 自述约束），若产品语义裁决为「红冲只回退材料不回退成品（成品走退货 successor）」，则降 P2；但 owner doc 明言「反向两段库存移动」，实现与文档矛盾确凿。
  3. **P1-CK-mfg3-004**（输入批次按产出仓解析）——若项目基准数据布局约定「所有物料共用单仓」则触发面收窄；但 ErpInvBatch 仓库强制列 + WO destWarehouseId 语义（产成品入库仓）的模型事实支持多仓意图，且静默空链无兜底。
  4. **P2-CK-mfg3-009**（重算复合悬挂）——触发依赖一次红冲失败（期间锁定为常见运维场景），复现需两步时序；建议修复阶段用集成测试实证「红冲失败→重算→post 幂等命中」链。
