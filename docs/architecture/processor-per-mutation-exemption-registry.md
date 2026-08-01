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

> R6.9（plan 2026-08-01-0803-1）消解 §E 全部 4 处遗漏后更新：类别 B +7（InvCosting 须拆 +1 / suspendByPartner 须拆 +1 / MdSupplierApproval 6 项豁免 +6），类别 A 须拆 +2（BudgetScenario rollForward/carryForward），Posting engine 豁免登记（§Engine）。

| 类别 | 总计 | 须拆 | 合法豁免 |
|------|------|------|---------|
| 类别 B（BizModel 内联 `@BizMutation`） | 242 | 166 | **76** |
| 类别 A（facade D-mutation 入口） | 94 D-mutation + 7 D-query | 94 | **7**（≤2 步查询 :45）+ 1 engine 豁免（§Engine） |
| 类别 A 单 D-mutation facade（R6.8 backstop） | 1 | 1（`ErpPurRequisition.convertToOrder`） | — |
| **合计须拆** | — | **261**（166+94+1） | — |

---

## A. 类别 B 合法豁免清单（76，按域分组）

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

## master-data（6）

> R6.9 补登记（plan 2026-08-01-0803-1 Phase 2）：R6.8 核验发现 master-data 无 §A 段（R6.7 master-data 仅拆 ErpMdCurrency.refreshRatesFromApi）。6 项 `:46` 单步状态翻转（require+守卫+setStatus+审计字段+updateEntity），approve 含 `requireQualificationValid` 属 `validate*` 允许。`suspendByPartner`（批量循环内含写）不在此列——R6.9 已拆为 `ErpMdSupplierApprovalSuspendByPartnerProcessor`。

| Entity | BizModel.method | 豁免依据 |
|--------|-----------------|---------|
| ErpMdSupplierApproval | ErpMdSupplierApprovalBizModel.apply | :46 单步状态翻转+守卫（require+status 守卫+setStatus(APPLIED)+updateEntity） |
| ErpMdSupplierApproval | ErpMdSupplierApprovalBizModel.approve | :46 单步状态翻转+守卫+审计字段（require+status 守卫+requireQualificationValid[validate* 允许]+setStatus(APPROVED)+setApprovedBy/setApprovedAt+updateEntity） |
| ErpMdSupplierApproval | ErpMdSupplierApprovalBizModel.probate | :46 单步状态翻转+守卫（require+status 守卫+setStatus(PROBATION)+updateEntity） |
| ErpMdSupplierApproval | ErpMdSupplierApprovalBizModel.suspend | :46 单步状态翻转+守卫（require+doSuspend[status 守卫+setStatus(SUSPENDED)+updateEntity]） |
| ErpMdSupplierApproval | ErpMdSupplierApprovalBizModel.reinstate | :46 单步状态翻转+守卫+审计字段（require+status 守卫+setStatus(APPROVED)+setApprovedBy/setApprovedAt+updateEntity） |
| ErpMdSupplierApproval | ErpMdSupplierApprovalBizModel.reject | :46 单步状态翻转+守卫（require+status 守卫+setStatus(REJECTED)+updateEntity） |

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

> §Engine 段（业财过账引擎例外，R6.9 裁决）紧随本段之后。

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

## Engine. 业财过账引擎例外（R6.9 裁决，plan 2026-08-01-0803-1 Phase 1 Decision）

> `ErpFinPostingProcessor` 持有 2 个 D-mutation 入口（`process` 正向过账 + `reverseProcess` 红冲），经 `ErpFinVoucherBizModel.post/reverse`（`@BizMutation` @Transactional REQUIRES_NEW）路由。**裁决=engine 豁免登记**（方案 a），零生产代码变更。

**真相源背书**：`processor-extension-pattern.md:66` 明示「业财过账引擎（`IErpFinVoucherBiz` + `ErpFinPostingProcessor`）」为 canonical Facade+Processor 范例（事务/Session 分层参照实例）。pattern doc 自身以本类（含 process+reverseProcess 两入口）为 canonical Processor，非 :42 所指"无关 mutation 拼装"。

**裁决理由**：
1. **forward/reverse 对称逆操作**：process（正向过账编排）与 reverseProcess（红冲编排）是同一过账引擎的正/逆操作，非独立无关 mutation 的拼装。
2. **共享 ~95% protected step**：resolveProvider/resolveOpenPeriod/generateFacts/resolveSubjects/balanceTotals/persistVoucher 等被两入口共享。拆分将产生重复或须抽 `AbstractErpFinPostingProcessor` 共享基类（违背 self-contained per-mutation 目标）。
3. **会计保护区域零风险**：方案 a 零生产代码变更，不触碰 @SingleSession/@Transactional REQUIRES_NEW 边界。

**考虑的替代方案（方案 b，已否决）**：拆 `ErpFinPostingProcessProcessor` + `ErpFinPostingReverseProcessProcessor` + 共享基类。语义不变但触碰全域过账引擎核心，@SingleSession/@Transactional REQUIRES_NEW 边界须重新验证，风险最高，且与真相源 :66 背书的 canonical 范例冲突。

**残留风险**：无。process/reverseProcess 的 productization 配置余地已由各自 protected step 方法 + 派生 bean 覆盖机制提供（pattern doc §配置余地）。

| 域 | Facade/Processor | 方法 | 处置 |
|----|------------------|------|------|
| finance | ErpFinPostingProcessor | process / reverseProcess | **engine 豁免**（pattern doc :66 canonical 业财过账引擎 forward/reverse 对称逆操作 + 共享 ~95% protected step；拆分违背 self-contained 目标）。R6.8 完成判据 grep 核验时按 :42 例外处理（已登记在案）。 |

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

## E. R6.8 完成判据核验发现的登记缺口与 successor（plan 2026-08-01-0656-1）— R6.9 已全部消解

> R6.8 全量 grep 核验（2026-08-01）发现 R6.0 triage 的 2 处误移除 + 1 处域外遗漏 + 1 处登记缺口。**R6.9（plan 2026-08-01-0803-1）已全部消解**——4 处遗漏拆为独立 Processor（BudgetScenario 2 + InvCosting 1 + suspendByPartner 1），Posting 裁决为 engine 豁免（§Engine），MdSupplierApproval 6 项 :46 补登记（§A master-data 段）。MR6 milestone 闭合。

| 域 | 项 | R6.8 核验结论 | R6.9 消解处置 ✅ |
|----|----|--------------|---------------|
| finance | `ErpFinBudgetScenarioProcessor`（rollForward `:104` + carryForward `:136`）| 类别 A :42 违规——2 个内联 D-mutation 入口（R6.0 triage plan 2026-07-31-2109-1 line 100 误移除，理由"同因 S-mutation 纯委托 D=0"实误）| ✅ **已拆**为 `ErpFinBudgetScenarioRollForwardProcessor` + `ErpFinBudgetScenarioCarryForwardProcessor`（facade 按方案 A 保留 shared helper：requireScenario/save/loadBudgetLines/resolveUserId） |
| finance | `ErpFinPostingProcessor`（process `:126` + reverseProcess `:209`）| 类别 A 边界 :42——2 个 D-mutation 入口（R6.0 triage 同 line 100 误移除）；但本类是 `processor-extension-pattern.md:66` canonical 业财过账引擎（forward/reverse 对称逆操作 + 全域过账共享）| ✅ **裁决=engine 豁免登记**（§Engine 段；pattern doc :66 背书；forward/reverse 对称逆操作 + 共享 ~95% protected step；零生产代码变更） |
| inventory | `ErpInvCostingBizModel.reclosePeriodCosts`（`:74-116`，`@BizModel("ErpInvCosting")`）| 类别 B :5/:7 违规——≥3 步内联（嵌套循环 + BigDecimal 成本法分支 + 实体写），无 Processor（R6.0 triage 未扫 `costing/` 包，仅扫 `entity/`）| ✅ **已拆**为 `ErpInvCostingReclosePeriodCostsProcessor`（BizModel 保留 @BizMutation 入口单行委托） |
| master-data | `ErpMdSupplierApprovalBizModel`（apply/approve/probate/suspend/reinstate/reject + suspendByPartner）| **登记缺口**：6 项判 :46 合法豁免但未入 §A；**suspendByPartner** 批量循环边界 | ✅ **6 项 :46 补登记**入 §A master-data 段；✅ **suspendByPartner 已拆**为 `ErpMdSupplierApprovalSuspendByPartnerProcessor`（循环内含写违反条件 4，与 R6.7 批量先例一致） |

> 本节为 R6.8 核验产出的诚实登记。R6.0–R6.7 的 256 须拆 + 77 已登记豁免结论不变；本节是 R6.8 全量核验暴露的 triage 边界遗漏。**R6.9 已全部消解**（4 处拆 Processor + 1 处 engine 豁免 + 6 项补登记），MR6 完成判据闭合。

