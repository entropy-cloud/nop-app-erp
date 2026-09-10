# 03 — 实体方法缺口（贫血领域模型）+ 双源迁移矩阵

> 规则依据：`../nop-entropy/docs-for-ai/02-core-guides/domain-logic-and-ddd.md`（稳定领域事实/只读计算/状态判断 isXxx/canXxx/calculateXxx 放实体；实体跨实体只读用 requireBiz()）+ owner doc `entity-state-machine-bean.md`（固定迁移矩阵集中承载于 ErpXxxStateMachine Bean）
> 历史关联：L-6（plan 2026-07-20-2200-1）当时实测 22 Processor / 132 处内联并裁决「不立即改造」，follow-up 触发条件之一「新增审批流单据 ≥5」早已满足——本文件给出全域量化证据，建议立即立项。

## 1. 量化全景

| 测量 | 数值 |
|------|------|
| 手写实体类（`*/dao/entity/Erp*.java`，369 个 ORM 实体 + Constants 等） | 401 个文件 |
| 含 `public boolean is*/can*` 方法的实体 | **22 个（≈6%）** |
| Processor/BizModel 内联状态比较（`Objects.equals(...Status...)` 及同型） | **~400 处**（分域：fin AR/AP 42 + ast 112 + pur/sal/inv 111 + mfg 25 + hr/prj/qa/mnt ~69 + crm/cs/ct/b2b/md 44 + aps 11 + drp 7 + log 3） |
| 完全空壳实体域 | **mfg / aps / logistics / drp 四域 100% 空壳**（如 `ErpMfgWorkOrder.java:7-11` 仅 `class ErpMfgWorkOrder extends _ErpMfgWorkOrder{}`） |
| 已有实体方法却被绕过 | 13 个实体已有 `isApproved()/isRejected()/isCancelled()` 三件套（pur/sal 审批族），但 Processor 大量仍用内联（`ErpSalReturnApproveProcessor.java:112` 证明 entity.isApproved() 可用而根 Processor 不用） |

**结构性结论**：这正是「避免过程式编程、在实体上增加适当方法」最佳实践的最大缺口——状态语义散落在 ~400 个调用点，任何字典值变更/新状态引入都要全量 hunt，且已产生真实漂移（见 §3 双源矩阵）。

## 2. 上提候选 Top 清单（按域，签名建议）

### purchase / sales / inventory（111 处）

1. `ErpSalReturn.isSubmitted()` — 消费点 `ErpSalReturnProcessor.java:148/155/162`（approve/reject/withdraw 三处 `status==null || !Objects.equals(status, SUBMITTED)`）
2. `ErpSalReturn.isEditable()` — `ErpSalReturnProcessor.java:140-142`（UNSUBMITTED/REJECTED 双态；同型适用于 sal/pur 全部根 Processor 的 validateTransitionForSubmit）
3. `ErpSalInvoice.isFullyReceived()` — `ErpSalReturnProcessor.java:337`
4. `ErpPurReceive.isApproved()` **已存在未用** — `ErpPurReturnProcessor.java:451`（零成本修复）
5. `ErpFinAccountingPeriod.isOpen()` — `ErpSalReturnProcessor.java:307` 等跨域消费点（fin 实体方法，**待复核**跨域实体方法边界）

### finance / assets（224 处）

1. `ErpFinArApItem.isOpenOrPartial()` — `ErpFinBadDebtProcessor.java:300-301,449-450` 重复 2 次 + `ErpFinReportBizModel`/`AnnualCloseService`/`BadDebtProvisionCalculator` 同型
2. `ErpFinArApItem.isWrittenOff()/isOpen()` — `ErpFinBadDebtProcessor.java:317,457,464,471` 四连
3. `ErpAstAsset.isDisposed()` — `ErpAstInventoryProcessor.java:287-289`（SCRAPPED/SOLD/DISPOSED 三连；ErpAstAsset 实体无任何状态 helper）
4. `ErpAstCip.isInConstruction()` — `ErpAstCipProcessor.java:97,204`
5. `ErpFinBudgetScenario.isApproved()/isDraft()` — `ErpFinBudgetScenarioCarryForwardProcessor.java:111,116`；另 `ErpFinAccountingPeriod.isFinalClosed()/canClose()`（`AnnualCloseService.java:119` 等）

### mfg / aps / logistics / drp（46 处，实体 100% 空壳）

1. `ErpMfgWorkOrder.isApproved()/isCancelled()` + `ErpMfgSubcontractOrder.isApproved()/isCancelled()` — 同型比较在 mfg 重复 ≥12 处（`ErpMfgWorkOrderApproveProcessor.java:85,90`、`ErpMfgWorkOrderRejectProcessor.java:84,89`、`ErpMfgWorkOrderReverseApproveProcessor.java:84`、`ErpMfgWorkOrderWithdrawApprovalProcessor.java:78`、`ErpMfgWorkOrderSubmitForApprovalProcessor.java:79`、SubcontractOrder 族同型 5 处）
2. `ErpApsOperationOrder.progress()/isFinished()` — `ErpApsOperationOrderBizModel.java:316-317` Gantt 进度三目（终态判断先例：`ErpApsOperationOrderStateMachine.java:155` isTerminal 可镜像）
3. `ErpMfgWorkOrder.isCompleted()` — `ErpMfgCostVarianceCalculateVariancesProcessor.java:39`
4. `ErpMfgWorkOrder.isFullyCompleted(qty)/getRemainingQuantity()` — `ErpMfgWorkOrderReportCompletionProcessor.java:37-45`（工单剩余量/达量计算内联）
5. `ErpApsOpRouting.isQtyWithinBatch(qty)/isEffectiveOn(date)` — `ErpApsSchedulingEngine.java:603-621`
6. `ErpLogShipment.isWindowBookable()` — `ErpLogDeliveryBookingBizModel.java:68-69`；`shipment.isFreightSettled()` — `AbstractErpLogShipmentDeliveredProcessor.java:95`

### hr / projects / quality / maintenance（~69 处）

1. `ErpQaInspection.isAccepted()/isRejected()` — 7+ 处消费点（`ErpQaDashboardBizModel.java:81/84/116`、`ErpQaReportBizModel.java:236/239`、`SpcSamplingService.java:303`、`NcrLifecycleService.java:89`）
2. `ErpHrSalarySimulation.isDraft()/isInReview()/isApproved()` — 6 处（BizModel:291/312/331 + 3 个 Processor）
3. `ErpQaAction.isCompleted()/isVerified()` — `ErpQaActionBizModel.java:66`、`NcrLifecycleService.java:110-115`
4. `ErpHrEmployee.isActiveEmployed()` — `ErpHrSalaryBizModel.java:270-271`（ACTIVE/PROBATION 集合判断内联）
5. `ErpMntEquipment.isDecommissioned()/isRunning()` — mnt 域 EQUIPMENT_STATUS 内联 10+ 处（`EquipmentStatusLinker.java:82/85/100/103`、`EquipmentRuntimeCalculator.java:53`、`OeeCalculator`、`DecommissionedEquipmentGuard`）
6. 静态工具承载领域规则：`InspectionResultEvaluator.java:35-63/84-110` → 上提 `ErpQaInspectionLine.evaluate()` + `ErpQaInspection.aggregateResult(...)`

### crm / cs / contract / b2b / master-data（44 处）

1. `ErpCtContract.isActive()/isSuspended()/requireInvoiceable()` — 同一对 ACTIVE/SUSPENDED 检查复制 3 次（`ErpCtInvoicePlanTriggerInvoiceProcessor.java:46-50`、`ErpCtInvoicePlanGenerateByTermProcessor.java:65-69`、`ErpCtConsumptionPeriodSummarizeProcessor.java:154-158`）
2. `ErpCtSignatureRequest.canTransitionTo()/isTerminal()` — `AbstractErpCtSignatureRequestProcessor.java:161-186` 迁移矩阵与 `ErpCtSignatureRequestBizModel.java:127-154`「state machine core」段双份实现
3. `ErpCsTicket.isPreProcessing()` — `ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor.java:285,578-584`（NEW/ASSIGNED/IN_PROGRESS 逐一 equals）
4. `ErpCrmLead.isMovable()` — `ErpCrmLeadProcessor.java:96-101` 与 `ErpCrmConversionProcessor.java:152` 同语义
5. `ErpCsTicketTimerSession.isStopped()/isPaused()/isRunning()` — Stop/Resume/Pause 三个 Timer Processor 散布（`ErpCsTicketTimerSessionStopTimerProcessor.java:42` 等 6 处）
6. 只读聚合上提：`ErpCsTicketBizModel.java:476-511` totalTimeSpent/totalBillableTime/totalBilledAmount 循环 TimeEntry → ErpCsTicket 实体方法（经 timeEntries to-many）；`ErpCtDocumentBizModel.java:419-463` rebuildFullTextSearch/metadataTagValues 纯实体派生计算 → ErpCtDocument 实体方法

## 3. [major] 双源迁移矩阵漂移（StateMachine Bean vs 内联）

违反 `entity-state-machine-bean.md`「固定迁移矩阵集中承载」契约：

- **同类混用**：`ErpPurInvoiceProcessor.java` — `:149/:164/:172` 走 `stateMachine.assertCanXxx`，`:134/:142/:157` 同矩阵内联 `Objects.equals`
- **根类整块内联而子类已走 Bean**：sales 根 Processor（`ErpSalReturnProcessor.java:135-176`）迁移矩阵整块内联保留，per-mutation 子类已全部覆写走 StateMachine（`ErpSalReturnApproveProcessor.java:82-88`）——根级版本沦为漂移面
- **缺 StateMachine Bean 的实体**：`ErpApsSchedule`（aps 仅有 OperationOrder Bean；`ErpApsScheduleBizModel.java:50,69-73` 内联状态矩阵）、`ErpLogDeliveryBooking`（`ErpLogDeliveryBookingBizModel.java:134-135,150-151` BOOKED/CONFIRMED 重复）、hr 的 Simulation/DevelopmentPlan/Survey/Recruitment/Assessment 五实体（`ErpHrSalarySimulationBizModel.java:287-341` 5 态审批链内联；`ErpHrDevelopmentPlanBizModel.java:94-101/164-190` plan-item 矩阵 if-chain 且与 `AbstractErpHrDevelopmentPlanProcessor.java:124-129` 双份维护漂移）
- 修复方向：内联清零、统一走 Bean 断言；根 Processor 的守卫方法降级为仅调 Bean + 领域 ErrorCode

## 4. 修复路径建议（successor 计划参考）

1. **批处理机械化**：`isApproved/isSubmitted/isTerminal` 三件套按域批量上提（每域一个计划；ORM 实体手写类非生成物，不触发 codegen 保护区域，但 finance/assets 属业务保护区域须双 agent 批准）
2. **迁移矩阵收敛**：以 `entity-state-machine-bean.md` 契约为准，根 Processor 内联守卫全部替换为 Bean 断言（对齐 2026-09-07-2200-1 Phase 5 收口先例——AbstractProcessor illegalStatusException 抽象化已消 149 处）
3. **L-6 follow-up 触发条件已满足**：本次全域 ~400 处量化即立项证据；优先方向 A（实体方法上提）为主，方向 B（共享工具）仅用于跨实体通用谓词
