# Processor per-mutation 拆分 — 合法豁免登记

> 真相源：`docs/architecture/processor-extension-pattern.md`（:7 任何 ≥3 步方法须拆 Processor / :29 每 mutation 一个 Processor / :42 禁多 mutation 共用 Processor / :44-47 例外清单）。
> 来源：plan `docs/plans/2026-07-31-2109-1-r6-0-mr6-d-mutation-inline-triage.md`（R6.0 triage）。
> 用途：R6.8 完成判据核验的对照基准——"须拆 mutation 已全部拆为 `<Entity><Method>Processor`；本登记内 mutation 合法保留 BizModel/facade"。

## 判定规则（R6.0 triage 准绳）

一个内联 `@BizMutation` 方法判定为**合法豁免**（保留 BizModel，无需 Processor），当且仅当满足**全部**条件（对齐 :44-47）：

1. **纯单实体状态翻转**：`require` 已存在实体 + 状态守卫（`if(status!=X) throw`）+ `setStatus`/审计字段 + **至多一次** `updateEntity`/`saveEntity`，无实体创建（`newEntity`/`new Erp…`）。
2. **无跨域编排**：不调用其他域 `I*Biz`。
3. **无引擎/派发/计算组件**：不调用 `*Calculator`/`*Dispatcher`/`*Poster`/`*Generator`/`*Engine`/`*Registry`/`*Provider`/`*Settler`/`*Gateway` 等。
4. **无循环副作用**：无 `for`/`stream` 内的写/创建。
5. **无金额计算**：无 `BigDecimal.add/subtract/multiply/divide` + 持久化。
6. **无非平凡副作用方法调用**：除 `require*`/`get*`/`set*`/`validate*`/`check*`/`writeAction`(审计日志)/`writeLog`/持久化原语外，不调用其他副作用方法。
7. 审计日志写入（`writeAction`/`writeLog`）视为状态翻转的固有簿记，**不**破坏豁免。

任一条件不满足 → **须拆**为 `<Entity><Method>Processor`（≥3 步 / 跨域编排 / 复合逻辑）。

> **边界争议处理**：2-3 步含轻量校验的方法，若核心仍是单实体状态翻转（仅多一个守卫/审计字段），判豁免并在此登记；若含可分离副作用（通知派发、子实体取消、计算），判须拆。R6.x 执行期可对单个边界项复议，复议结果回填本登记。

## 统计

| 类别 | 总计 | 须拆 | 合法豁免 |
|------|------|------|---------|
| 类别 B（BizModel 内联 `@BizMutation`） | 234 | 164 | **70** |
| 类别 A（facade D-mutation 入口） | 92 D-mutation + 7 D-query | 92 | **7**（≤2 步查询 :45） |
| 类别 A 单 D-mutation facade（R6.8 backstop） | 1 | 1（`ErpPurRequisition.convertToOrder`） | — |
| **合计须拆** | — | **256**（164+92） | — |

---

## A. 类别 B 合法豁免清单（70，按域分组）

> 豁免依据：`processor-extension-pattern.md:46`（单步状态翻转 `setStatus`+`updateEntity`，无跨域编排）+ `:45`（纯查询 ≤2 步）+ `:47`（标准 CRUD）。下列方法保留 BizModel，R6.1-R6.7 不动。

## aps（2）

| Entity | BizModel.method | 豁免依据 |
|--------|-----------------|---------|
| ErpApsSchedule | ErpApsScheduleBizModel.archive | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpApsSchedule | ErpApsScheduleBizModel.publish | :46 单步状态翻转+守卫/审计字段（无跨域编排） |

## b2b（9）

| Entity | BizModel.method | 豁免依据 |
|--------|-----------------|---------|
| ErpB2bEdiDoc | ErpB2bEdiDocBizModel.archive | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpB2bEdiDoc | ErpB2bEdiDocBizModel.cancel | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpB2bEdiDoc | ErpB2bEdiDocBizModel.markAcknowledged | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpB2bEdiDoc | ErpB2bEdiDocBizModel.markError | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpB2bEdiDoc | ErpB2bEdiDocBizModel.markSent | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpB2bEdiDoc | ErpB2bEdiDocBizModel.retry | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpB2bPartnerProfile | ErpB2bPartnerProfileBizModel.activate | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpB2bPartnerProfile | ErpB2bPartnerProfileBizModel.deactivate | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpB2bPartnerProfile | ErpB2bPartnerProfileBizModel.suspend | :46 单步状态翻转+守卫/审计字段（无跨域编排） |

## contract（7）

| Entity | BizModel.method | 豁免依据 |
|--------|-----------------|---------|
| ErpCtContract | ErpCtContractBizModel.expire | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpCtContract | ErpCtContractBizModel.resume | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpCtContract | ErpCtContractBizModel.suspend | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpCtContract | ErpCtContractBizModel.terminate | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpCtContractVersion | ErpCtContractVersionBizModel.finalizeVersion | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpCtSignatureRequest | ErpCtSignatureRequestBizModel.cancelSignatureRequest | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpCtSignatureRequest | ErpCtSignatureRequestBizModel.rejectSignature | :46 单步状态翻转+守卫/审计字段（无跨域编排） |

## crm（3）

| Entity | BizModel.method | 豁免依据 |
|--------|-----------------|---------|
| ErpCrmForecastPeriod | ErpCrmForecastPeriodBizModel.freeze | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpCrmQuota | ErpCrmQuotaBizModel.finalizeQuota | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpCrmQuota | ErpCrmQuotaBizModel.unfinalizeQuota | :46 单步状态翻转+守卫/审计字段（无跨域编排） |

## cs（8）

| Entity | BizModel.method | 豁免依据 |
|--------|-----------------|---------|
| ErpCsEntitlement | ErpCsEntitlementBizModel.consumeEntitlement | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpCsEntitlement | ErpCsEntitlementBizModel.releaseEntitlement | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpCsSurvey | ErpCsSurveyBizModel.submitSurvey | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpCsTicket | ErpCsTicketBizModel.adoptKnowledge | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpCsTicket | ErpCsTicketBizModel.assign | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpCsTicket | ErpCsTicketBizModel.cancel | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpCsTicket | ErpCsTicketBizModel.close | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpCsTicket | ErpCsTicketBizModel.start | :46 单步状态翻转+守卫/审计字段（无跨域编排） |

## drp（1）

| Entity | BizModel.method | 豁免依据 |
|--------|-----------------|---------|
| ErpDrpLine | ErpDrpLineBizModel.approveLine | :46 单步状态翻转+守卫/审计字段（无跨域编排） |

## finance（1）

| Entity | BizModel.method | 豁免依据 |
|--------|-----------------|---------|
| ErpFinPostingException | ErpFinPostingExceptionBizModel.manualEntry | :46 单步状态翻转+守卫/审计字段（无跨域编排） |

## hr（16）

| Entity | BizModel.method | 豁免依据 |
|--------|-----------------|---------|
| ErpHrDevelopmentPlan | ErpHrDevelopmentPlanBizModel.completePlan | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpHrEmployeeAssessment | ErpHrEmployeeAssessmentBizModel.submitAssessment | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpHrEmploymentContract | ErpHrEmploymentContractBizModel.renew | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpHrLeaveRequest | ErpHrLeaveRequestBizModel.reject | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpHrRecruitment | ErpHrRecruitmentBizModel.close | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpHrRecruitment | ErpHrRecruitmentBizModel.makeOffer | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpHrRecruitment | ErpHrRecruitmentBizModel.moveToScreening | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpHrRecruitment | ErpHrRecruitmentBizModel.reject | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpHrRecruitment | ErpHrRecruitmentBizModel.scheduleInterview | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpHrSalary | ErpHrSalaryBizModel.voidSalary | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpHrSalarySimulation | ErpHrSalarySimulationBizModel.approve | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpHrSalarySimulation | ErpHrSalarySimulationBizModel.reject | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpHrSalarySimulation | ErpHrSalarySimulationBizModel.submitForReview | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpHrShiftSwapRequest | ErpHrShiftSwapRequestBizModel.cancel | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpHrShiftSwapRequest | ErpHrShiftSwapRequestBizModel.reject | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpHrTimesheet | ErpHrTimesheetBizModel.submit | :46 单步状态翻转+守卫/审计字段（无跨域编排） |

## inventory（3）

| Entity | BizModel.method | 豁免依据 |
|--------|-----------------|---------|
| ErpInvStockTake | ErpInvStockTakeBizModel.cancelTake | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpInvStockTake | ErpInvStockTakeBizModel.completeTake | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpInvStockTake | ErpInvStockTakeBizModel.startTake | :46 单步状态翻转+守卫/审计字段（无跨域编排） |

## maintenance（1）

| Entity | BizModel.method | 豁免依据 |
|--------|-----------------|---------|
| ErpMntEquipment | ErpMntEquipmentBizModel.changeStatus | :46 单步状态翻转+守卫/审计字段（无跨域编排） |

## manufacturing（2）

| Entity | BizModel.method | 豁免依据 |
|--------|-----------------|---------|
| ErpMfgForecast | ErpMfgForecastBizModel.approve | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpMfgForecast | ErpMfgForecastBizModel.cancel | :46 单步状态翻转+守卫/审计字段（无跨域编排） |

## projects（8）

| Entity | BizModel.method | 豁免依据 |
|--------|-----------------|---------|
| ErpPrjProject | ErpPrjProjectBizModel.cancelProject | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpPrjProject | ErpPrjProjectBizModel.requireReferenceable | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpPrjProject | ErpPrjProjectBizModel.startProject | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpPrjTask | ErpPrjTaskBizModel.blockTask | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpPrjTask | ErpPrjTaskBizModel.completeTask | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpPrjTask | ErpPrjTaskBizModel.startTask | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpPrjTask | ErpPrjTaskBizModel.unblockTask | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpPrjTimesheet | ErpPrjTimesheetBizModel.reject | :46 单步状态翻转+守卫/审计字段（无跨域编排） |

## purchase（2）

| Entity | BizModel.method | 豁免依据 |
|--------|-----------------|---------|
| ErpPurQuotation | ErpPurQuotationBizModel.cancel | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpPurRfq | ErpPurRfqBizModel.cancel | :46 单步状态翻转+守卫/审计字段（无跨域编排） |

## quality（7）

| Entity | BizModel.method | 豁免依据 |
|--------|-----------------|---------|
| ErpQaAction | ErpQaActionBizModel.completeAction | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpQaAction | ErpQaActionBizModel.startAction | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpQaAction | ErpQaActionBizModel.verifyAction | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpQaNonConformance | ErpQaNonConformanceBizModel.cancel | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpQaNonConformance | ErpQaNonConformanceBizModel.escalateToRecall | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpQaNonConformance | ErpQaNonConformanceBizModel.submitReview | :46 单步状态翻转+守卫/审计字段（无跨域编排） |
| ErpQaRecall | ErpQaRecallBizModel.cancel | :46 单步状态翻转+守卫/审计字段（无跨域编排） |

## B. 类别 A facade 豁免（:45 只读查询 + :46 单步状态翻转）

> 这些是 facade Processor 中以查询语义命名（`find*`/`get*`/`*Preview`/`check*`）的公共入口方法（属纯查询/只读计算，对齐 `:45`），或单步状态翻转（`require`+守卫+`setStatus`+`updateEntity`，对齐 `:46`）。facade 拆分时保留 facade 或迁回 BizModel，不强制 per-mutation Processor。

| 域 | Facade | 方法 | 处置 |
|----|--------|------|------|
| manufacturing | ErpMfgWorkOrderProcessor | checkAvailability | :45 只读可用性校验，保留 facade |
| manufacturing | ErpMfgScheduleToJobCardProcessor | findWorkOrdersPendingJobCards | :45 只读查询，保留 facade |
| manufacturing | ErpMfgWorkOrderProcessor | cancel | :46 单步状态翻转（DRAFT/SUBMITTED/NOT_STARTED→CANCELLED，零副作用），保留 facade（R6.2 新登记） |
| manufacturing | ErpMfgSubcontractOrderProcessor | cancel | :46 单步状态翻转（DRAFT/SUBMITTED/APPROVED→CANCELLED，零副作用），保留 facade（R6.2 新登记） |
| assets | ErpAstCipProcessor | findCostItems | :45 只读子实体查询，保留 facade |
| assets | ErpAstCipProcessor | findProgressBillings | :45 只读子实体查询，保留 facade |
| crm | ErpCrmConversionProcessor | getCreatedOpportunity | :45 只读关联查询，保留 facade |
| inventory | ErpInvStockMoveProcessor | findByRelatedBill | :45 只读溯源查询，保留 facade |
| inventory | ErpInvLandedCostProcessor | allocatePreview | :45 只读分摊预览（无持久化），保留 facade |
| inventory | ErpInvOwnershipTransferProcessor | cancel | :46 单步状态翻转（DRAFT/CONFIRMED→CANCELLED，无跨实体写），保留 facade（R6.4 新登记） |

## C. 边界争议 adjudication 记录

以下方法经 R6.0 逐方法审查，落在 2-3 步边界，裁决理由记录如下（R6.x 执行期可复议，复议结果回填）：

| 域 | 方法 | 裁决 | 理由 |
|----|------|------|------|
| cs | ErpCsTicket.assign/start/cancel/close | 豁免 | require+守卫+setStatus+updateEntity+writeAction(审计)，纯状态翻转+审计簿记，无副作用 |
| cs | ErpCsTicket.reopen | 须拆 | 翻转+`cancelUnrespondedSurvey`（可分离副作用：取消未响应调查） |
| cs | ErpCsEntitlement.consumeEntitlement | 豁免 | 配额守卫+`setUsedTickets(used+1)`+update，单实体配额递减（int 算术非 BigDecimal），单逻辑步 |
| finance | ErpFinPostingException.manualEntry | 豁免 | require+守卫+多审计字段 setStatus/setResolution/setResolvedBy+update，单实体状态翻转+审计 |
| hr | ErpHrSalarySimulation.approve | 豁免 | require+守卫+setStatus+setReviewerId+setReviewedAt+update，翻转+审计字段 |
| hr | ErpHrEmployeeAssessment.submitAssessment | 豁免 | require+守卫+findDetails(校验查询)+setStatus+update，翻转+校验守卫 |
| hr | ErpHrEmploymentContract.renew | 豁免 | require+守卫+setStatus+setEndDate+update，翻转+字段更新 |
| projects | ErpPrjTask.blockTask/startTask/completeTask/unblockTask | 豁免 | require+守卫+setStatus(+setBlockReason)+update，任务状态翻转 |
| quality | ErpQaAction.verifyAction | 豁免 | require+守卫+setVerificationPerson+setVerificationDate+update，翻转+验证审计 |
| quality | ErpQaRecall.register | 须拆 | `newEntity`+applyRecallFields+多字段默认+save，含字段拷贝（多步初始化） |
| projects | ErpPrjProject.requireReferenceable | 豁免（:45） | 非变异：require+状态守卫+return，本质是前置条件守卫（建议改注 `@BizQuery`/`@BizAction`，非 MR6 范围） |
| b2b | ErpB2bEdiDoc.cancel/markSent/markAcknowledged/markError/retry/archive | 豁免 | require+状态守卫+setState+save+writeLog(审计)，翻转+EDI 日志簿记 |
| aps | ErpApsSchedule.archive/publish | 豁免 | require+守卫+setStatus+update，排程状态翻转 |
| notify | ErpSysNotification.markRead | 须拆 | 创建独立实体 `ErpSysNotificationRead`（`readDao.newEntity`+save），非纯翻转 |

## D. 类别 B 非变异守卫/伪 mutation（已从须拆计数排除）

R6.0 扫描发现个别 `@BizMutation` 注解实际是非变异守卫/查询（如 `ErpPrjProject.requireReferenceable`）或扫描伪影（`LoggerFactory.getLogger` 被误识别为方法，已剔除）。这些既不须拆也不属真正豁免——建议各域后续改注解（非 MR6 范围），R6.8 grep 核验时按"无 ≥3 步内联 mutation"判据忽略。

