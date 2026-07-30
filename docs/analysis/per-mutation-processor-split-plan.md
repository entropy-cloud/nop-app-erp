# Per-mutation Processor 拆分计划

> Plan: `docs/plans/2026-07-24-2200-1-cross-domain-code-abstraction.md` Phase 2 / `docs/plans/2026-07-25-1057-2-per-mutation-processor-file-split.md`
> 状态：执行中（plan 1057-2 Phase 1 Explore 产出，权威拆分清单）

## 当前策略

Phase 2 的核心行为变更是：**xbiz `<source>` 委托 → BizModel Java `@BizMutation` 方法 + per-mutation Processor 文件**。
拆分后委托链：BizModel `@BizMutation approve(...)` → `@Inject ErpXxxApproveProcessor` → `approveProcessor.approve(id, context)` → `AbstractApproveProcessor.approve()` 编排骨架 → 子类 hook 实现。

## 决策记录（plan 1057-2 Phase 1 Decision）

### Decision #1：per-mutation 文件命名 + 包结构

**裁决**：选项 (A) `module-<domain>/erp-<short>-service/.../processor/<Entity><Method>Processor.java`（与既有 monolithic 同包）。

**理由**：
1. 与既有命名链一致（`ErpPurOrderProcessor` → `ErpPurOrderApproveProcessor`）
2. checker R8 regex 兼容（`class Erp.*Processor` 仍命中）
3. IDE 检索友好（同包内 `<Entity>` 前缀分组）
4. 避免 per-entity 子包爆炸（42 实体 × 6 mutation = 250 文件平铺比 42 子包更易导航）

### Decision #2：BizModel @BizMutation 委托模式

**裁决**：选项 (A) BizModel `@Inject` N 个 per-mutation Processor（每个 mutation 一个 @Inject 字段）。

**理由**：
1. Nop `@Inject` 非 private 字段已支持多注入
2. 与抽象基类设计意图一致（每个 mutation 独立 bean，独立可 Delta 覆盖）
3. 避免 facade 层冗余（保留原 monolithic Processor 作 facade 会重复 delegation 链）
4. 每个 per-mutation Processor 是独立 IoC bean，下游可经 Delta beans.xml 同名 id 覆盖单个 mutation 行为

**@BizMutation 重载消歧说明**（M4 修订）：
- BizModel 持有唯一 `@BizMutation` 方法名（`approve`/`reject`/`submitForApproval`/`reverseApprove`/`withdrawApproval`/`cancel`）
- per-mutation Processor 同名 public 方法经 `@Inject` 字段名消歧：
  ```java
  @Inject ErpPurOrderApproveProcessor approveProcessor;  // 字段名 = approveProcessor
  @Inject ErpPurOrderRejectProcessor rejectProcessor;    // 字段名 = rejectProcessor

  @Override
  @BizMutation
  public ErpPurOrder approve(@Name("id") String id, IServiceContext context) {
      return approveProcessor.approve(id, context);  // approveProcessor.approve(...) 一行委托
  }
  ```
- Nop GraphQL resolver 按 BizModel 方法名绑定（`ErpPurOrder__approve`），不经 Processor 类名，无 GraphQL 重载冲突
- per-mutation Processor 的同名 `approve(...)` 方法是 Java 级别的方法名，不是 GraphQL 接口——无 IoC 名字冲突（每个 Processor 是不同类）

### Decision #3：xbiz `<source>` 清理策略

**裁决**：BizModel `@BizMutation` 接管后，xbiz `<mutation>` 块整块移除（不保留空 `<mutation>` 壳）。

**理由**：
1. Nop xbiz 是增量覆盖层：Java BizModel 已声明 `@BizMutation` 方法 + `@Name` 参数，GraphQL schema 经 `_ErpPurOrder.xbiz` 的 `biz-gen:DefaultBizGenExtends` 自动从 Java 反射生成
2. 保留空 `<mutation>` 壳无意义（无 `<source>`、无 `<auth>`、无 `<task:name>`）
3. xbiz 仅保留 `<actions>` 中实际有定制内容的块（auth / task / source 覆盖）

**例外**：BizModel 未接管的方法（如 `batchApprove`、`settle`、`convertToOrder` 等域特定 mutation）保留原状。

## 全 42 Processor mutation 矩阵权威清单

> 数据来源：`rg 'public .+\(' module-*/erp-*-service/.../processor/*.java`（2026-07-25 实测）
> mutation 类型：S=标准审批 mutation（有抽象基类，强制拆分）；D=域特定 mutation（无抽象基类，保留在 monolithic 残留或 BizModel）

### 标准审批 mutation 抽象基类映射

| mutation | 抽象基类 | 编排骨架 |
|----------|---------|---------|
| submitForApproval | `AbstractSubmitForApprovalProcessor<T>` | requireEntity → validateNotCancelled → validateTransitionForSubmit → validateBusinessRules → beforeStateChange → doSubmit → afterStateChange → save → maybeStartWorkflow |
| approve | `AbstractApproveProcessor<T>` | requireEntity → (isApproved 早退) → validateNotCancelled → validateTransitionForApprove → validateBusinessRules → beforeStateChange → doApprove → afterStateChange → save |
| reject | `AbstractRejectProcessor<T>` | requireEntity → (isRejected 早退) → validateNotCancelled → validateTransitionForReject → beforeStateChange → doReject → afterStateChange → save |
| reverseApprove | `AbstractReverseApproveProcessor<T>` | requireEntity → (isRejected 早退) → validateTransitionForReverseApprove → beforeStateChange → doReverseApprove → afterStateChange → save |
| withdrawApproval | `AbstractWithdrawApprovalProcessor<T>` | requireEntity → validateNotCancelled → validateTransitionForWithdraw → beforeStateChange → doWithdraw → afterStateChange → save |
| cancel | `AbstractCancelProcessor<T>` | requireEntity → validateCanCancel → validateTransitionForCancel → beforeCancel → doCancel → afterCancel → save |

### 域 processor × mutation 矩阵

| 域 | Processor | S-mutations | D-mutations | per-mutation 文件数 |
|----|-----------|------------|-------------|---------------------|
| purchase | ErpPurOrderProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval, cancel | （无） | 6 |
| purchase | ErpPurRequisitionProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval, cancel | convertToOrder | 6 |
| purchase | ErpPurReceiveProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval, cancel | confirm | 6 |
| purchase | ErpPurReturnProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval, cancel | （无） | 6 |
| purchase | ErpPurInvoiceProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval, cancel | （无） | 6 |
| purchase | ErpPurPaymentProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval, cancel | settle, reverseSettlement | 6 |
| sales | ErpSalOrderProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval, cancel | （无） | 6 |
| sales | ErpSalQuotationProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval, cancel | confirmCustomerAccepted, convertToOrder | 6 |
| sales | ErpSalDeliveryProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval, cancel | （无） | 6 |
| sales | ErpSalReceiptProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval, cancel | settle, reverseSettlement | 6 |
| sales | ErpSalReturnProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval, cancel | （无） | 6 |
| sales | ErpSalInvoiceProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval, cancel | （无） | 6 |
| finance | ErpFinEmployeeAdvanceProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval, cancel | （无） | 6 |
| finance | ErpFinBadDebtProcessor | submit, approve, reject, reverseApprove | writeOff, recover | 4（submit/reverseApprove 缺 withdrawApproval/cancel） |
| finance | ErpFinExpenseClaimProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval, cancel | （无） | 6 |
| finance | ErpFinNotesPayableProcessor | （无标准审批 mutation） | issue, honor, dishonor, writeOff | 0（无 S-mutation，本计划不拆分） |
| finance | ErpFinNotesReceivableProcessor | （无标准审批 mutation） | receive, discount, endorse, collect, honor, dishonor, writeOff | 0（无 S-mutation，本计划不拆分） |
| finance | ErpFinAccountingPeriodProcessor | （无标准审批 mutation） | preCheck, closePeriod, finalizePeriod, generateNextYearPeriods, reverseClose | 0（无 S-mutation，本计划不拆分） |
| finance | ErpFinPostingProcessor | （无标准审批 mutation） | process, reverseProcess | 0（无 S-mutation，本计划不拆分） |
| finance | ErpFinBudgetScenarioProcessor | submit, approve, reject, cancel | rollForward, carryForward | 4（submit 替代 submitForApproval；缺 reverseApprove/withdrawApproval） |
| assets | ErpAstCipProcessor | （无标准审批 mutation） | startConstruction, addCostItem, addProgressBilling, findCostItems, findProgressBillings, transferToAsset, reverseTransfer | 0（无 S-mutation，本计划不拆分） |
| assets | ErpAstSplitProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval, cancel | （无） | 6 |
| assets | ErpAstAssetCapitalizationProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval | （缺 cancel） | 5 |
| assets | ErpAstInventoryProcessor | approve | submitForCount, reconcile, processVariance, post, cancel, reverse | 1（仅 approve 是 S-mutation；cancel 是 D） |
| assets | ErpAstMaintenanceProcessor | approve | createMaintenance, submit, startWork, completeWork, decideTreatment, post, cancel, reverse | 1（仅 approve 是 S-mutation） |
| assets | ErpAstDisposalProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval | （缺 cancel） | 5 |
| assets | ErpAstDepreciationScheduleProcessor | （无标准审批 mutation） | executeDepreciation, executeBatchDepreciation, reverseDepreciation, recalculateForCapitalizationMaintenance | 0（无 S-mutation，本计划不拆分） |
| assets | ErpAstValueAdjustmentProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval, cancel | （无） | 6 |
| assets | ErpAstMergeProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval, cancel | （无） | 6 |
| manufacturing | ErpMfgWorkOrderProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval | checkAvailability, start, stop, resume, close, cancel, reportCompletion | 5（cancel 是 D-mutation，因签名 `Long workOrderId` 非 `String id`） |
| manufacturing | ErpMfgScheduleToJobCardProcessor | （无标准审批 mutation） | generateJobCardsFromSchedule, findWorkOrdersPendingJobCards, generatePendingJobCards | 0（无 S-mutation，本计划不拆分） |
| manufacturing | ErpMfgJobCardProcessor | （无标准审批 mutation） | startJob, recordWork, submitJob, completeJob, holdJob, resumeJob, cancelJob | 0（无 S-mutation，本计划不拆分） |
| manufacturing | ErpMfgSubcontractOrderProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval | issueMaterials, receiveFinished, postProcessingFee, reverseCompletion, cancel | 5（cancel 是 D-mutation，签名 `Long`） |
| inventory | ErpInvCostAdjustProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval | applyCostAdjust, reverseCostAdjust, cancel | 5（cancel 是 D-mutation） |
| inventory | ErpInvLandedCostProcessor | approve, reverseApprove | allocatePreview, generateFreightLandedCost | 2（仅 approve + reverseApprove 是 S-mutation） |
| inventory | ErpInvOwnershipTransferProcessor | （无标准审批 mutation） | confirm, done, cancel | 0（无 S-mutation，本计划不拆分） |
| inventory | ErpInvStockMoveProcessor | （无标准审批 mutation） | generateMove, confirm, complete, cancel, reverse, findByRelatedBill, forwardTrace, backwardTrace, returnTrace, batchTrace | 0（无 S-mutation，本计划不拆分） |
| crm | ErpCrmLeadProcessor | cancel | qualify, lose, moveStage | 1（仅 cancel 是 S-mutation；签名 `Long leadId` 视为 D，仅 String id 的 cancel 才 S） |
| crm | ErpCrmConversionProcessor | （无标准审批 mutation） | convertToCustomer, convertToQuotation, getCreatedOpportunity | 0（无 S-mutation，本计划不拆分） |
| quality | ErpQaRecallProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval | （无 cancel） | 5 |
| aps | ErpApsSchedulingProcessor | （无标准审批 mutation） | scheduleForward, scheduleBackward, insertRushOrder | 0（无 S-mutation，本计划不拆分） |
| projects | ErpPrjProjectSettlementProcessor | submit, approve, reject, cancel | createSettlement, reverseSettlement | 4（submit 替代 submitForApproval；缺 reverseApprove/withdrawApproval） |

### 拆分目标统计

| 指标 | 数值 |
|------|------|
| 42 Processor 中含至少 1 个标准审批 S-mutation | 27 |
| 42 Processor 中无标准审批 S-mutation（纯域特定，本计划不拆分） | 15 |
| 27 拆分候选 Processor 的 per-mutation 文件总数 | ~140（低于原估 ~250，因部分 Processor 仅有 1-5 个 S-mutation） |

> **修正说明**：原 plan 估算 ~250 文件基于「42 × 6」假设。实测后 15 Processor 无标准审批 mutation（纯域特定 D-mutation），27 Processor 有 1-6 个 S-mutation。实际拆分目标 ~140 per-mutation 文件。

## `<source>` delegation vs inline-script 分类

> 数据来源：逐 xbiz 文件统计 `<source>` 块类型（2026-07-25 实测）
> 分类规则：(a) **delegation** = `<source>` 仅 `inject(xxxProcessor).method(...)` 一行；(b) **inline-script** = `<source>` 内联 XLang 脚本（含 `NopScriptError` / 状态迁移逻辑 / ErrorCode 参数）

### 分类总表（拆分候选 27 Processor）

| 域 | 实体 xbiz | sources | delegation | inline-script | inline 详情 |
|----|----------|---------|-----------|---------------|------------|
| purchase | ErpPurOrder | 5 | 4 | 1 | withdrawApproval: NopScriptError invalid-status + 状态迁移 |
| purchase | ErpPurRequisition | 5 | 2 | 3 | approve/reject/withdrawApproval: 含 NopScriptError |
| purchase | ErpPurReceive | 5 | 3 | 2 | approve/withdrawApproval: 含状态迁移逻辑 |
| purchase | ErpPurReturn | 5 | 3 | 2 | approve/withdrawApproval |
| purchase | ErpPurInvoice | 5 | 3 | 2 | approve/withdrawApproval |
| purchase | ErpPurPayment | 5 | 4 | 1 | withdrawApproval |
| sales | ErpSalOrder | 5 | 4 | 1 | withdrawApproval |
| sales | ErpSalQuotation | 5 | 4 | 1 | withdrawApproval |
| sales | ErpSalDelivery | 5 | 4 | 1 | withdrawApproval |
| sales | ErpSalReceipt | 5 | 5 | 0 | （全部 delegation） |
| sales | ErpSalReturn | 5 | 4 | 1 | withdrawApproval |
| sales | ErpSalInvoice | 5 | 4 | 1 | withdrawApproval |
| finance | ErpFinEmployeeAdvance | 5 | 5 | 0 | （全部 delegation） |
| finance | ErpFinExpenseClaim | 5 | 5 | 0 | （全部 delegation） |
| finance | ErpFinBadDebt | 5 | 0 | 5 | （全部 inline-script，无 Processor delegation） |
| finance | ErpFinBudgetScenario | 5 | 0 | 5 | （全部 inline-script） |
| assets | ErpAstAssetCapitalization | 5 | 5 | 0 | （全部 delegation） |
| assets | ErpAstDisposal | 5 | 6 | 0 | （6 injects，含 1 helper inject） |
| assets | ErpAstMerge | 5 | 5 | 0 | （全部 delegation） |
| assets | ErpAstSplit | 5 | 5 | 0 | （全部 delegation） |
| assets | ErpAstValueAdjustment | 5 | 5 | 0 | （全部 delegation） |
| assets | ErpAstInventory | 5 | 0 | 0 | （无 Processor delegation，default approval-support） |
| assets | ErpAstMaintenance | 5 | 0 | 0 | （无 Processor delegation） |
| manufacturing | ErpMfgWorkOrder | 5 | 4 | 1 | withdrawApproval inline（R5.5 已验证分类准确，inline 已提取为 Java hook + xbiz delegation，见 plan 2026-07-30-1909-2） |
| manufacturing | ErpMfgSubcontractOrder | 5 | 4 | 1 | withdrawApproval inline（R5.5 已验证分类准确，inline 已提取为 Java hook + xbiz delegation，见 plan 2026-07-30-1909-2） |
| inventory | ErpInvCostAdjust | 5 | 5 | 0 | （全部 delegation） |
| inventory | ErpInvLandedCost | 5 | 0 | 0 | （无 Processor delegation） |
| quality | ErpQaRecall | 5 | 4 | 1 | withdrawApproval inline |
| crm | ErpCrmLead | 5 | 0 | 0 | （无 Processor delegation） |

### inline-script 提取规则

对于 `inline-script` 类 `<source>` 块（主要是 `withdrawApproval`），拆分时须将内联脚本语义提取为新 per-mutation Processor 的 hook 实现：

1. **状态守卫脚本**（`if status !== 'SUBMITTED' throw NopScriptError`）→ 由 `AbstractWithdrawApprovalProcessor.validateTransitionForWithdraw` 默认实现覆盖（语义等价）
2. **状态迁移脚本**（`entity.approveStatus = 'UNSUBMITTED'`）→ 由 `AbstractWithdrawApprovalProcessor.doWithdraw` 默认实现覆盖
3. **ErrorCode 参数**（`.param("bizObjName", ...).param("currentStatus", ...)`）→ 由 per-mutation Processor 的 `notFoundException` / `illegalStatusException` override 保留错误码语义

**关键约束**：inline-script 提取后须经既有测试覆盖验证错误码 + 状态迁移断言等价（不修改断言值，仅当快照因路径变化失配时重录）。

## 抽象基类 hook 兼容性 + side-effect 重定位映射

### side-effect 三态分类

| 类别 | 定义 | 映射到抽象基类 hook |
|------|------|---------------------|
| before-save | save 前执行的校验/计算 | `validateBusinessRules` / `validateCanCancel` / `validateTransitionForXxx` |
| after-save-inline | doXxx 方法体内 `dao().updateEntity()` 后立即执行的副作用 | `afterStateChange` / `afterCancel` |
| idempotent | 幂等副作用，重定位无语义影响 | 任意 hook |

### 关键审计点：updateEntity 时序等价性

**既有 Processor 模式**：doApprove 方法体内多次/内联调 `dao().updateEntity(entity)`（每次状态变更后立即 save）。
**抽象基类模式**：`AbstractApproveProcessor.approve()` 末尾统一调 `dao().updateEntity(entity)` 一次。

**等价性论证**（每族 Processor）：

#### purchase ErpPurOrder 族（commitment + intercompany + budget hooks）

- `runBudgetCheckHook`（在 `validateBusinessRulesForApprove` 中）：纯校验，无 save 依赖 → 映射到 `validateBusinessRules`
- `runCommitmentCommitHook`（在 `doApprove` 后）：使用 `order.getCode()` / `order.getTotalAmountWithTax()` / `order.getBusinessDate()` / `order.getOrgId()`，全部是 in-memory 字段读取，不依赖 save 后的 DB 状态 → 映射到 `afterStateChange`，时序等价
- `runIntercompanyApproveHook`（在 `doApprove` 后）：使用 in-memory 字段，config-gated try-catch 非阻塞 → 映射到 `afterStateChange`
- `runCommitmentReleaseHook` + `runIntercompanyReverseHook`（在 `doReverseApprove` / `doCancel` 前）：释放/红冲操作，使用 in-memory `order.getCode()` / `order.getId()` → 映射到 `beforeStateChange` / `beforeCancel`

**等价性结论**：所有 side-effect 读取的都是 in-memory 实体字段（非 save 后的 DB 查询），时序重定位（doApprove 内 → afterStateChange）不改变可观察语义。

#### purchase ErpPurReceive 族（confirm + posting hooks）

- `approve` 中的库存移动 + 凭证过账：在 doApprove 后执行，使用 in-memory receive 字段 → `afterStateChange`
- `reverseApprove` 中的红冲：使用 in-memory 字段 → `beforeStateChange`（红冲在状态回退前执行，对齐既有 doReverseApprove 顺序）

#### finance ErpFinEmployeeAdvance / ErpFinExpenseClaim 族

- 仅标准审批 mutation，无 side-effect hooks → per-mutation Processor 仅实现抽象方法，不 override hooks

#### finance ErpFinBadDebt / ErpFinBudgetScenario 族

- 全部 inline-script xbiz，无 Processor delegation → 拆分时 inline-script 提取为 Java hook 实现

#### assets ErpAstSplit / Merge / ValueAdjustment / AssetCapitalization / Disposal 族

- 全部 delegation xbiz → per-mutation Processor 仅实现抽象方法

### hook 兼容性结论

**0 个 Processor 发现不兼容**：所有 side-effect 均可映射到抽象基类的 pre-hook（validateBusinessRules / validateCanCancel / validateTransitionForXxx）或 post-hook（afterStateChange / afterCancel）。

**updateEntity 时序差异不阻塞**：抽象基类在末尾统一 save 一次 vs 既有 Processor 内联多次 save，对当前所有 side-effect 均无可观察影响（side-effect 全部读取 in-memory 字段，不查询 save 后的 DB 状态）。

**结论**：全域 27 拆分候选 Processor 的抽象基类 hook 与域特定 side-effect 模式兼容，无域阻塞触发条件。

## 拆分执行清单（按 plan Phase 1-4）

| Phase | 域 | Processor | per-mutation 文件数 |
|-------|----|-----------|---------------------|
| 1 | purchase | ErpPurOrder, ErpPurRequisition, ErpPurReceive, ErpPurReturn, ErpPurInvoice, ErpPurPayment | 6×6 = 36 |
| 2 | sales + finance | ErpSalOrder/Quotation/Delivery/Receipt/Return/Invoice (6×6=36) + ErpFinEmployeeAdvance/ExpenseClaim (2×6=12) + ErpFinBadDebt (4) + ErpFinBudgetScenario (4) | 56 |
| 3 | assets + mfg + inv | ErpAstSplit/Merge/ValueAdjustment/AssetCapitalization/Disposal (5×5-6=28) + ErpAstInventory/Maintenance approve (2) + ErpMfgWorkOrder/SubcontractOrder (2×5=10) + ErpInvCostAdjust (5) + ErpInvLandedCost approve+reverseApprove (2) | 47 |
| 4 | crm + qa + aps + prj | ErpCrmLead cancel (1) + ErpQaRecall (5) + ErpPrjProjectSettlement (4) | 10 |

**合计**：36 + 56 + 47 + 10 = 149 per-mutation 文件（与拆分目标 ~140 一致，因部分 Processor 拆分文件数有微调）

> Phase 4 R8 Decision 待执行期复核（per-mutation 文件按 xbiz 路由 = 不计入 R8，因 per-mutation 文件经 BizModel @BizMutation 路由非 Processor xbiz 接线）。

## MR5 完成回注（R5.1-R5.8，2026-07-30）

> 本节是 `audit-remediation-roadmap.md` MR5（S-mutation 架构合规修复）7 工作项全部 done 后的最终分类与完成证据回注。详细执行见 `docs/plans/2026-07-25-1057-2`（拆分）~`2026-07-30-2046-2`（R5.8 收尾）。

### 最终分类：149 per-mutation 文件全部自包含（零空心回委托）

- **Pattern A（抽象骨架激活）**：per-mutation Processor `extends Abstract*Processor<T>`，实现 `dao()` / `notFoundException()` / `illegalStatusException()` / 各 `*Status()` / `isCancelled()` 等抽象方法，编排走抽象基类骨架（`requireEntity` → 状态守卫 → pre-hook → `setApproveStatus`/`setDocStatus` → 持久化）。适用于域特有 hook 可映射到抽象基类 pre/post-hook 的实体（purchase/sales 多数、finance EA/EC、assets Capitalization/Disposal、mfg、inventory CostAdjust、qa、projects）。
- **Pattern B（custom public override）**：per-mutation Processor 保留 `public T method(String id, IServiceContext)` 完整编排方法体，内部委托 facade（monolithic Processor）的 protected helper（`requireXxx` / `validateTransitionForXxx` / `doXxx` / `executeXxx`）作为单一真相源，避免业务逻辑复制。适用于：过账后实体引用变更需 custom override 的 S-mutation（如 receive/invoice/return approve+reverseApprove——红冲后凭证引用变更）、跨域联动 hook 复杂的实体（purchase Order approve 的 commitment+intercompany hook）、会计保护区域语义不可复制的实体（finance BadDebt reverseApprove 的红冲凭证 + ArApItem 对称回滚）。
- **分布**：149 文件中 Pattern A 占多数，Pattern B 用于上述特殊语义实体（静态 parity 经 R5.1-R5.6 逐实体校验）。

### 休眠 → 激活路径（R5.8 完成）

R5.1-R5.6 完成时，149 per-mutation 文件已全部填充，但 BizModel 仍 `@Inject` 单 facade、S-mutation 经 xbiz source（source-backed，已激活）或 BizModel `@BizMutation`（dormant，直调 facade）。R5.8 将全部 dormant BizModel S-mutation 调用 repoint 到 per-mutation：

- **cancel 路由（18）**：purchase 6 + sales 6 + finance 3（EA/EC/BudgetScenario）+ assets 3（Merge/Split/ValueAdjustment）的 BizModel `cancel()` 从 `facade.cancel(...)` → `cancelProcessor.cancel(String.valueOf(id), ctx)`。
- **休眠非 cancel S-mutation（11）**：finance BadDebt 4（submit/approve/reject/reverseApprove）+ BudgetScenario 3（submit/approve/reject）+ assets Inventory approve（1）+ Maintenance approve（1）+ inventory LandedCost approve+reverseApprove（2）。
- **batchApprove（2）**：ErpPurOrder/ErpSalOrder 的 `batchApprove()` 内 `facade.approve(...)` → `approveProcessor.approve(id, ctx)`。
- **R5.7 域**：projects ProjectSettlement 4 S-mutation + crm Lead cancel 经 BizModel repoint（qa Recall 经 xbiz source 已激活，无 BizModel repoint）。

激活后 29 休眠 + R5.7 孤儿 per-mutation 全部进入运行时路径，既有测试经 BizModel→per-mutation 新路径验证行为等价（MR5 7 域 + qa 1131 测试 0 failures）。

### facade 公共 S-mutation 方法精简（30 facade，单行委托）

R5.8 将 30 个含 S-mutation 的 facade Processor 公共方法体替换为 `return {per-mutation}.method(id, ctx)` 单行委托，保留方法签名作向后兼容适配器（forwarder）。protected helper 保留（per-mutation 依赖）。

- **循环依赖消解**：facade `@Inject` per-mutation（作 forwarder）+ per-mutation `@Inject` facade（调 protected helper）= 双向 field-injection 循环。Nop IoC `BeanDefinition.newObject` 在构造后、属性注入前经 `scope.add` 注册 early singleton 引用（`nop-ioc BeanDefinition.java:521`），故循环可解析（对齐 Spring early-singleton-ref 机制）。purchase Order pilot 实测 IoC 正常 + 132 测试全绿确认。
- **Long↔String 边界转换**：facade 公共方法保留原 Long 签名（向后兼容），委托体内 `String.valueOf(id)` 转 String per-mutation；finance BadDebt/BudgetScenario/projects 的 facade `submit` 方法委托 per-mutation `submitForApproval`（方法名边界转换）。
- **跨包 facade 处理**：`ErpFinBudgetScenarioProcessor` 位于 `app.erp.fin.service.budget`（非 `service.processor`），其 per-mutation Processor 位于 `app.erp.fin.service.processor`——slim 时 per-mutation `@Inject` 须显式 import（唯一跨包 facade）。

### 验证证据（R5.8）

- `mvn clean install -DskipTests`：154 模块全绿（含 app-erp-all 聚合）。
- `mvn test`：MR5 7 域 + qa 1131 测试 0 failures；mfg 1 pre-existing error（TestErpMfgCompletionPosting LOCATION_ID 漂移，与本 plan 无关）；drp 7 pre-existing error（IErpSysNotificationBiz 测试隔离，与本 plan 无关，git stash 证实 clean HEAD 同样失败）。
- compliance checker：MR5.8 引入 0 合规漂移（git stash A/B 对照证实）。pre-existing 漂移（R2a/R2b/R2c/R12c）归 successor 基线裁决计划。
- arm-index：P1-MA3-048 + P1-MA2-054 状态更新为「MR5 已填充，孤儿状态已清除」。

### successor

- R2.7（孤儿 Processor 删除）审查时增检查「跳过 MR5 填充的 Processor」——MR5 填充后 S-mutation per-mutation 全自包含，R2.7 仅处理非 S-mutation 孤儿（如有）。
- facade protected helper 重构/下沉——protected helper 是 per-mutation 经单一真相源调用的依赖，保留在 facade（MR5 Non-Goal）。
