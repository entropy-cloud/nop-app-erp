# 2026-08-13-1950-1-purchase-m4-approvestatus-state-machine-bean 采购入库/发票/付款/退货单 ErpPurReceive/Invoice/Payment/Return.approveStatus 实体级状态机 Bean（M4.14 + M4.16 + M4.18 + M4.20）

> Plan Status: active
> Review Hold: §11.2 M4 (i) 人工/owner-doc 门控**已于 2026-08-13 经人工确认解除**——本计划触及受保护采购业财过账行为（approve 触发入库移动/凭证过账：Receive→`IErpInvStockMoveBiz` 入库；Invoice→AP_INVOICE 凭证；Payment→PAYMENT 凭证+核销；Return→出库+红字发票。reverseApprove 逆转上述副作用经 `PurReversalListener` 回写 posted=false + APPROVED→REJECTED，已由起草者经 live code 实证）。M4 plan-first 门控成立；该人工裁定非起草者可自主解除（project-context.md 会计/财务保护域硬停止）。计划格式/完备性/范围/结束证据就绪 + 人工门控已确认，已转 `active` 进入实施。
> Last Reviewed: 2026-08-13
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.14（ErpPurReceive.approveStatus）+ M4.16（ErpPurInvoice.approveStatus）+ M4.18（ErpPurPayment.approveStatus）+ M4.20（ErpPurReturn.approveStatus），均 plan-first；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 purchase`（440 行段，PUR-5/6/9/10/13/14/17/18 行）
> Related: 前置姊妹计划 `2026-08-13-0810-1-purchase-docstatus-m4-state-machine-bean.md`（M4.13+M4.15+M4.17+M4.19 docStatus 轴 draft）；M3 同轴先例 `2026-08-13-0945-1-purchase-approvestatus-state-machine-bean.md`（M3.2–M3.5 approveStatus done，跨实体 Decision + reverseApprove 实仓纠正 + INLINE 错误码 Decision 同源）；M0.1 契约 + M1.3 批量迁移模板固化于 `docs/architecture/entity-state-machine-bean.md §11`；姊妹 M4 计划 `2026-08-13-1950-2-sales-m4-approvestatus-state-machine-bean.md`
> Mission: entity-state-machine
> Work Item: M4.14 + M4.16 + M4.18 + M4.20
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。4 实体 approve 动作触发受保护业财过账行为（Receive→入库 stock move；Invoice→AP_INVOICE 凭证；Payment→PAYMENT 凭证+核销；Return→出库+红字发票），reverseApprove 经 `PurReversalListener` 逆转上述副作用并回写 posted=false + APPROVED→REJECTED。声明 §11.2 M4 硬约束：(i) plan-first + 受保护行为人工/owner-doc 门控；(ii) 过账时序/编排/失败回退（posted 回写）/红冲闭环不改，继续由 Processor + `PurReversalListener` + `posted` 契约管理；(iii) `posted` 不入轴；(iv) 跨域副作用（`IErpInvStockMoveBiz`、`IErpFinVoucherBiz`、commitment-restore）保留原 Processor/`I*Biz` 路径；(v) 既有红冲/reversal-listener 回写闭环以 `posted`+`approveStatus` 为契约不改。本计划是 plan-first 产物（满足 (i) 的 plan 要件），人工/owner-doc 确认门控已于 2026-08-13 解除，转 `active` 进入实施。
>
> **规则 14 bundling 声明**：M4.14（Receive）+ M4.16（Invoice）+ M4.18（Payment）+ M4.20（Return）属同一组件（同一 owner doc `docs/design/purchase/state-machine.md`、同一 `wf/approve-status` dict、同一审批 5 动作行为契约、同一结果表面 = 采购单据 approveStatus 审批轴），按指南规则 14 合并为单计划。approveStatus 轴与 docStatus 轴（M4.13/15/17/19）结果表面不同（不同字段/矩阵/Bean），按既定 M2/M3 先例分计划。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 purchase`（440 行段）+ 实仓核实。approveStatus 是采购单据三轴分离中的**审批轴**（`purchase/state-machine.md` §三轴状态分离 + §审批轴），与 docStatus 业务生命周期轴（M4.13/15/17/19 draft）独立。

- **轴语义（wf/approve-status 审批轴，5 动作）**：`UNSUBMITTED`（初始态，创建时写入）→(submit)→ `SUBMITTED` →(approve)→ `APPROVED` / →(reject)→ `REJECTED`；`REJECTED` →(submit 重提)→ `SUBMITTED`；`SUBMITTED` →(withdraw)→ `UNSUBMITTED`；`APPROVED` →(reverseApprove)→ `REJECTED`（实仓核实，见下方）。dict `wf/approve-status`（平台标准审核状态字典，全 ERP 共享）。PUR-5/6/9/10/13/14/17/18 八属性登记均为「纳入 / **是**（approve→入库/凭证/付款/退货）」。属模板 §11「M4 审批轴」类别（M3 审批轴变体：approve/reverseApprove 触发业财过账/库存移动，属保护区）。5 动作 = submit/approve/reject/reverseApprove/withdraw。
- **关键差异（与 M3.2–M3.5 Order/Quotation/Rfq/Requisition 的对比）**：M3 审批轴的 approve **仅状态推进**（无库存/凭证副作用）；M4 审批轴的 approve **触发业财过账 + 库存移动**（Receive→`triggerIncomingMove`→`IErpInvStockMoveBiz`；Invoice→AP_INVOICE 凭证经 `PurInvoicePostingDispatcher`；Payment→PAYMENT 凭证+核销经 `PurPaymentPostingDispatcher`；Return→出库+`PurReturnPostingDispatcher`）。**这些副作用保留在 Processor 原位**（Bean 只集中固定迁移矩阵，不触碰过账编排/stock move）。reverseApprove 经 `PurReversalListener` 逆转上述副作用。
- **固定迁移判断当前所在位置（实仓核实，双路径分化，影响接线策略）**：审批 5 动作的固定来源态/目标态守卫分布在两条路径（须 Phase 1 逐实体核实并分类）：
  - **skeleton 路径（Receive/Return 全部 5 动作 + Invoice/Payment 的 submit/reject/withdraw）**：守卫在共享骨架 `module-common-service/.../Abstract{SubmitForApproval,Approve,Reject,ReverseApprove,Withdraw}Processor.validateTransitionForXxx`（`AbstractApproveProcessor.validateTransitionForApprove` 内联 `Objects.equals(status, submittedStatus())`）。Receive/Return per-mutation Processor 经 `this.validateTransitionForXxx` 调骨架（Receive `ApproveProcessor:33` 实证）。Bean 接线 = 覆写 per-mutation Processor 的 `validateTransitionForXxx` 委托 Bean（同 M3 Order/Requisition 先例）。
  - **facade 路径（Invoice/Payment 的 approve + reverseApprove）**：Invoice/Payment 的 Approve/ReverseApprove per-mutation Processor **覆写整个 `approve()`/`reverseApprove()` 方法**，编排内调 **facade** `ErpPurInvoiceProcessor.validateTransitionForApprove`/`ErpPurPaymentProcessor.validateTransitionForReverseApprove`（facade 内联 `Objects.equals` 守卫 + `doApprove`/`doReverseApprove` 写状态）。Bean 接线 = **facade `validateTransitionForXxx`/`doApprove`/`doReverseApprove` 改调 Bean**（或 per-mutation Processor 注入 Bean 改调 Bean 而非 facade），须 Decision 选定。**此差异是本计划与 M3 先例的关键不同**——M3 Order/Requisition 全部经 skeleton，M4 Invoice/Payment 的 approve/reverseApprove 经 facade。
  - **无 INLINE xbiz 路径**（与 M3 Quotation/Rfq 不同）。
- **逐实体 writer 盘点（实仓核实）**：
  - **M4.14 ErpPurReceive**（PROC，5 Processor）：`ErpPurReceive{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval}Processor`。`ApproveProcessor.approve()` **整体覆写**（`:26-49`，编排 SoD→validateNotCancelled→validateTransitionForApprove→validateBusinessRulesForApprove→enforceInspectionGate→triggerIncomingMove→setApproveStatus(APPROVED)→posting→setReceiveStatus(RECEIVED)→updateEntity）。`validateTransitionForApprove` 未覆写（继承骨架守卫）。reverseApprove 目标态 = `APPROVE_STATUS_REJECTED`（`ErpPurReceiveReverseApproveProcessor:32` `receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED)`，**已合规 §16.4**）。错误码：`ERR_ILLEGAL_STATUS_TRANSITION`（**泛型命名**，无 RECEIVE_ 前缀，`:64`，参数 `receiveCode`/`currentStatus`/`expectedStatus`）。
  - **M4.16 ErpPurInvoice**（PROC，5 Processor）：`ErpPurInvoice{...}Processor`。approve 整体覆写编排过账 dispatcher reverse。领域码 `ERR_INVOICE_ILLEGAL_STATUS_TRANSITION`（`ErpPurErrors.java:130`，`erp.err.pur.invoice-illegal-status-transition`，参数 `invoiceCode`/...）。reverseApprove 目标态预期 = REJECTED（须实仓核实，同 M3 先例）。
  - **M4.18 ErpPurPayment**（PROC，5 Processor）：`ErpPurPayment{...}Processor`。Payment 有 `nopFlowId`（`useWorkflow="true"`），approve 经 workflow。领域码 `ERR_PAYMENT_ILLEGAL_STATUS_TRANSITION`（`:158`，`erp.err.pur.payment-illegal-status-transition`）。Payment 另有 `Settle`/`ReverseSettlement` Processor（走 `writtenOffStatus` 轴，非 approveStatus，不迁移）。
  - **M4.20 ErpPurReturn**（PROC，5 Processor）：`ErpPurReturn{...}Processor`。领域码 `ERR_RETURN_ILLEGAL_STATUS_TRANSITION`（`:194`，`erp.err.pur.return-illegal-status-transition`）。
- **reverseApprove 目标态（§16.4 合规性，同 M3 先例）**：M3 计划 Phase 1 Decision 实仓纠正——所有使用 `AbstractReverseApproveProcessor` 的 per-mutation Processor 均**已显式覆写** `doReverseApprove`/`reverseApprove` 为 REJECTED（非骨架 SUBMITTED）。Receive 已实证（`:32`）。Invoice/Payment/Return 须 Phase 1 实仓核实，预期同样已覆写=REJECTED（同 M3 Order/Requisition/Sales 先例）。若核实确认全部=REJECTED，则 Bean 统一 `reverseApproveTargetStatus()`=REJECTED，零行为回归，骨架 §16.4 不合规 Fix 移交 successor（与 M3 计划同源 successor，非新 successor）。
- **common 层非法迁移码已存在**：`ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（参数 `currentStatus`/`expectedStatus`），cs 试点 M1.1 Decision Option A + M3 采购审批计划裁定复用 + `action` 补充参数。本计划沿用。
- **Bean 命名约定（双轴预留）**：docStatus Bean 用 `Document` 后缀（M4.13/15/17/19 draft），本计划审批轴 Bean 用 `Approval` 后缀（`ErpPur<Entity>ApprovalStateMachine`），一 Bean 对一实体一轴（§1 双轴约定）。
- **Bean 注册范式已存在**：`_vfs/erp/pur/beans/app-service.beans.xml` 已注册 4 Document SM（Order/Requisition/Quotation/Rfq）+ 4 Approval SM（M3 已落地）+ 4 M4 实体各 6 per-mutation Processor。**4 实体 approveStatus SM Bean 未注册**（greenfield）。新 4 Bean 追加于 Approval SM 段。
- **既有测试（层 3 回归基线）**：`TestErpPurReceiveApproval`、`TestErpPurInvoiceApproval`、`TestErpPurPaymentApproval`、`TestErpPurReturnApproval` + 跨域 `TestErpPurProcureToPayEnd`、`TestPurReversalListenerReceiveRollback`、`TestErpPurFinanceReversalWriteback`。**无矩阵测试**（4 实体均无 `TestErpPur*ApprovalStateMachineMatrix`）。
- **合规基线**：`docs/audits/compliance-baseline.md` R5=0、R11=0。本计划保持 R5=0、R11 不增。
- **owner doc 覆盖**：`docs/design/purchase/state-machine.md` §适用对象（5 类采购单据，含 Receive/Invoice/Payment/Return）+ §三轴状态分离（approveStatus=审批轴）+ §审批轴。

## Goals

- 为采购 4 个单据实体的 approveStatus 轴各落地一个实体级 `ErpPur<Entity>ApprovalStateMachine` Bean（一 Bean 对一实体一轴），承载 5 动作迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态（§2）。Bean **据实编码 reverseApprove 目标态=REJECTED**（预期四实体统一，Phase 1 实仓确认）。
- 将审批路径的**固定来源态/目标态判断**改调 Bean：**双路径接线**——(A) skeleton 路径（Receive/Return 全部 5 动作 + Invoice/Payment 的 submit/reject/withdraw）经 per-mutation Processor 覆写 `validateTransitionForXxx` 委托 Bean；(B) facade 路径（Invoice/Payment 的 approve/reverseApprove）经 facade `validateTransitionForXxx`/`doApprove`/`doReverseApprove` 改调 Bean（Phase 1 Decision 选定确切注入点）。**动态业务守卫与副作用保留原位**（Receive 的 triggerIncomingMove/enforceInspectionGate/SoD；Invoice/Payment 的 PostingDispatcher/commitment-restore；Return 的出库 stock move/红字发票；Payment 的 workflow；全部 `PurReversalListener` 回写）。
- 裁定 **reverseApprove 目标态漂移**（骨架违反 §16.4）：Bean 保持各实体当前行为 + 骨架 Fix 移交既有 successor（与 M3 计划同源）。
- 层 2 四方对照（dict `wf/approve-status` ↔ `purchase/state-machine.md` §审批轴 ↔ Bean 元数据 ↔ 全部 writer 含 CRUD 路径）逐实体裁定。
- 新增层 1 矩阵完备性表驱动测试（greenfield，4 个 Bean）；层 3 既有集成测试全绿回归。
- 保持全部既有外部行为不变（错误码值/参数、审计 fromStatus/toStatus、SoD、过账时序/失败回退/红冲 listener 回写、stock move 时序）。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（路线图 Non-Goal）。
- 不迁移 `docStatus` 轴（M4.13/15/17/19 draft，另计划）。
- 不迁移 `receiveStatus`/`paidStatus`/`writtenOffStatus`（已裁定排除-技术/派生）。
- 不触碰 `posted`；approve 触发的过账编排保留在 `*PostingDispatcher` + Processor 原位（§11.2 M4 (ii)）。
- 不修改共享骨架 `Abstract{SubmitForApproval,Approve,Reject,ReverseApprove,Withdraw}Processor`（module-common-service 零改动）——迁移经各域 per-mutation Processor 覆写委托；骨架 `AbstractReverseApproveProcessor` §16.4 不合规 Fix 移交既有 successor（与 M3 计划同源）。
- 不改变 `*PostingDispatcher` 过账编排、`PurReversalListener` 回写语义、stock move 生成/逆转时序（§11.2 M4 (ii)/(iv)/(v)）。
- 不重命名 Receive 的泛型错误码 `ERR_ILLEGAL_STATUS_TRANSITION`（路线图 Non-Goal「不借迁移改变既有错误码」）。
- 不引入全局 CRUD 写锁（M0.1 successor）。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）。
- 不证 Delta 覆盖（M4 保护域单项，归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M0.2 清单 + M1.3 模板 §11 + M3 审批计划跨实体 Decision；落地 4 个单实体单轴审批 Bean + PROC 路径接线 + 测试 + 四方对照；不改契约/模型/公共 API/共享骨架。**M4 plan-first**——approve 触发采购业财过账/存货移动）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 模板 + §11.2 M4 变体 + §1 双轴约定）、`docs/design/purchase/state-machine.md`（§三轴分离 + §审批轴 + §审查提示）、`docs/design/domain-design-guidelines.md`（§16.4 反审核目标态权威）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（PUR-5/6/9/10/13/14/17/18）、`docs/architecture/processor-extension-pattern.md`、`docs/skills/state-machine-business-review-prompt.md`、`docs/plans/2026-08-13-0945-1-purchase-approvestatus-state-machine-bean.md`（M3 同轴先例，跨实体 Decision 同源）
- Skill Selection Basis: 路线图 M4.14/16/18/20 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「Processor 接线、Bean 注册、`@Inject` 非 private、跨实体调用边界、错误码、事务边界、SoD、过账副作用保留、产品化可定制性自检」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成测试回归」。`state-machine-business-review-prompt.md` 匹配层 2 四方对照。必需输入均已就绪。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护采购业财过账行为（approve 触发入库/凭证/付款/退货 + reverseApprove 经 `PurReversalListener` 逆转）。在人工/owner-doc 确认「以行为保持的矩阵集中化方式迁移此 4 轴、过账/stock move/reversal-listener 路径完整保留」可接受前为阻塞前置。**[此门控已于 2026-08-13 经人工确认解除，见 Draft Review Record 门控确认记录]**
- 无端口/环境变量/CORS/密钥/.env/外部服务依赖（除既有 commitment/budget/workflow 配置，保留不动）。无数据迁移。

## Execution Plan

### Phase 1 - ErpPurReceive approveStatus Bean（M4.14）+ 跨实体 Decision 固化

Status: planned
Targets: `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/statemachine/ErpPurReceiveApprovalStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpPurReceive{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval}Processor.java`、`.../test/.../TestErpPurReceiveApprovalStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done（已满足）；M4.14 deps = M1.3 + M4.13（draft，docStatus 轴另计划，approveStatus 迁移不阻塞于 docStatus 执行——双轴独立）

- [ ] `Decision`（reverseApprove 目标态实仓确认 + **wiring 路径逐实体分类**，复用 M3 先例）：(A) 核实 4 实体 `*ReverseApproveProcessor` 的 reverseApprove 目标态。Receive 已实证=REJECTED（`:32`）。Invoice/Payment/Return 须实仓核实，预期同覆写=REJECTED。骨架 `AbstractReverseApproveProcessor.doReverseApprove:39`→SUBMITTED 仍为已确认 live 缺陷（经覆写绕过的死路径），§16.4 合规化移交既有 successor。(B) **逐实体逐动作 wiring 路径核实**：Receive/Return 全部经 skeleton `validateTransitionForXxx`；Invoice/Payment 的 approve/reverseApprove 经 **facade** `ErpPurInvoice/PaymentProcessor.validateTransitionForXxx`（非 skeleton）。Bean 接线策略按路径分化：(A) skeleton 路径 → per-mutation Processor 覆写委托 Bean；(B) facade 路径 → facade 方法改调 Bean（或 per-mutation Processor 改调 Bean 替代 facade 调用）。Phase 2 须按此分化执行，**不得假设 Invoice/Payment 沿用 Receive 范式**。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] `Add`：落地 `ErpPurReceiveApprovalStateMachine` Bean——显式 `assertCanSubmit/Approve/Reject/ReverseApprove/Withdraw(String status)`（非法来源态 → 抛 common 码 `ERR_ILLEGAL_STATUS_TRANSITION` + `action`/`fromStatus` 补充参数）+ `submitTargetStatus/approveTargetStatus/rejectTargetStatus/reverseApproveTargetStatus/withdrawTargetStatus()`（reverseApprove=REJECTED）+ `isTerminal`/`initialStatuses`/`terminalStatuses` + 只读 `transitions()`（6 条边：submit×2 + approve + reject + reverseApprove + withdraw）。严格无状态（§2）。命名带 `Approval` 后缀。
  - Skill: `nop-backend-dev`
- [ ] `Add`：在 `_vfs/erp/pur/beans/app-service.beans.xml` 以 `<bean id="<FQN>" class="<FQN>"/>` 注册（四实体审批轴 Bean 一并注册）。
  - Skill: `nop-backend-dev`
- [ ] `Decision | Add`（跨实体接线 Decision 复用 M3 先例）：(A) Bean 接线点 = 5 个 per-mutation Processor 覆写 `validateTransitionFor{Submit,Approve,Reject,ReverseApprove,Withdraw}` 委托 Bean（try/catch common 码 → `illegalStatusException` 领域码）；目标态写入经覆写 `submittedStatus/approvedStatus/rejectedStatus/unsubmittedStatus` getter 委托 Bean `*TargetStatus()`；(B) common 错误码沿用 Option A；(C) 领域码映射 `ERR_ILLEGAL_STATUS_TRANSITION`（Receive 泛型）/ `ERR_INVOICE/PAYMENT/RETURN_ILLEGAL_STATUS_TRANSITION`（各自领域码）保留；(D) 初始态 UNSUBMITTED 写入不经 Bean（§9.2 选项 c）；(E) SoD + 动态业务守卫/副作用（triggerIncomingMove/enforceInspectionGate/PostingDispatcher/commitment-restore/workflow）保留原位。Receive 5 Processor 注入 `@Inject ErpPurReceiveApprovalStateMachine`（非 private），覆写 5 个 `validateTransitionForXxx` 调对应 `assertCanXxx`。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：层 1 矩阵完备性（greenfield 表驱动）——(a) 无重复/冲突边（6 边唯一 action|fromStatus 键）；(b) submit UNSUBMITTED/null/REJECTED→SUBMITTED、approve SUBMITTED→APPROVED、reject SUBMITTED→REJECTED、reverseApprove APPROVED→REJECTED、withdraw SUBMITTED→UNSUBMITTED 可达；(c) 各 `assertCanXxx` 合法来源态通过、非法来源态抛 common 码携带 `action`/`fromStatus`；(d) `transitions()` 与显式方法语义一致；(e) 初始={UNSUBMITTED}/终态={APPROVED}（APPROVED 经 reverseApprove 有出边，为可逆业务终态）。
  - Skill: `nop-testing`
- [ ] `Proof`：层 2 四方对照（Receive 单条）——dict `wf/approve-status` ↔ `purchase/state-machine.md` §审批轴 ↔ Bean 元数据 ↔ 全部 writer（5 Processor live + 创建写 UNSUBMITTED + CRUD 路径 §9.4 选项 c 排除）。owner doc §审批轴矩阵与 Bean 一致性核实。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] `ErpPurReceiveApprovalStateMachine` Bean 存在、已注册、严格无状态；5 个 Receive 审批 Processor 委托 Bean，内联 `Objects.equals` 矩阵判断已移除（动态 hook 除外）。
- [ ] Receive 层 1 矩阵测试本地 `mvn test -pl module-purchase/erp-pur-service -am -Dtest=TestErpPurReceiveApprovalStateMachineMatrix` 全绿。

### Phase 2 - ErpPurInvoice + ErpPurPayment + ErpPurReturn approveStatus Bean（M4.16 + M4.18 + M4.20）

Status: planned
Targets: `.../statemachine/ErpPur{Invoice,Payment,Return}ApprovalStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpPur{Invoice,Payment,Return}{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval}Processor.java`、`.../test/.../TestErpPur{Invoice,Payment,Return}ApprovalStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（跨实体 Decision + wiring 分类已固化，三实体按路径分化执行）

- [ ] `Add`：落地 `ErpPurInvoiceApprovalStateMachine` / `ErpPurPaymentApprovalStateMachine` / `ErpPurReturnApprovalStateMachine`（同 Phase 1 结构，reverseApprove 目标态=REJECTED，各自领域码）；**接线按 Phase 1 wiring 分类分化**：Return（skeleton 路径）5 Processor 覆写 `validateTransitionForXxx` 委托 Bean；Invoice/Payment 的 submit/reject/withdraw（skeleton）经 per-mutation Processor 覆写委托 Bean，**approve/reverseApprove（facade 路径）经 facade `ErpPurInvoice/PaymentProcessor.validateTransitionForApprove/validateTransitionForReverseApprove` 改调 Bean**（确切注入点按 Phase 1 Decision）。Invoice/Payment 的 PostingDispatcher 过账编排、commitment-restore 保留原位。Payment 的 workflow（`nopFlowId`/`useWorkflow="true"`）保留原位。注册 3 Bean。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：层 1 矩阵完备性（3 实体独立测试）。
  - Skill: `nop-testing`
- [ ] `Proof`：层 2 四方对照（Invoice/Payment/Return 各单条）。writer 图：各 5 per-mutation Processor（live，xbiz 委托）+ 创建写 UNSUBMITTED + CRUD 路径（§9.4 选项 c 排除）。owner doc 矛盾处按 doc drift 处置（如有）。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] 3 Bean 存在/注册/无状态；各 5 Processor 委托 Bean，内联矩阵判断已移除。
- [ ] 3 层 1 矩阵测试本地全绿。

### Phase 3 - 层 3 既有命名动作回归

Status: planned
Targets: `module-purchase/erp-pur-service/src/test/`（既有集成测试，零新建）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1–2（四实体 Bean + 接线已落地）

- [ ] `Proof`：层 3 既有命名动作回归——复用既有集成测试基线（`TestErpPurReceiveApproval`/`TestErpPurInvoiceApproval`/`TestErpPurPaymentApproval`/`TestErpPurReturnApproval` + 跨域 `TestErpPurProcureToPayEnd`/`TestPurReversalListenerReceiveRollback`/`TestErpPurFinanceReversalWriteback`），证明 Processor 写回、审计 fromStatus/toStatus、SoD、领域错误码 + 参数、过账 dispatcher/stock move/PurReversalListener 副作用时序不变。本地 `mvn test -pl module-purchase/erp-pur-service -am` 全绿。
  - Skill: `nop-testing`
- [ ] `Proof`：四实体一致性复核——四 Bean 命名（`Approval` 后缀）/注册（同文件）/无状态/元数据形状一致（同 6 边矩阵 + reverseApprove=REJECTED 统一）；PROC 路径接线范式可追溯。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 层 3 既有集成测试全绿（零行为回归）。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_00505efd0ffezmCFXg0xUwjqTT`) — BLOCKER：草案 baseline 误称全部 4 实体经 skeleton 路径，实仓核实 Invoice/Payment 的 approve/reverseApprove 经 **facade** `ErpPurInvoice/PaymentProcessor.validateTransitionForXxx`（非 skeleton），Bean 接线策略须分化。v2 已修正 Current Baseline 为双路径描述、Goals/Phase 1 Decision/Phase 2 接线按 skeleton vs facade 路径分化执行；其余（模板结构、§11.2 M4 治理、rule 14 bundling、错误码、reverseApprove=REJECTED、Deferred 诚实性）均 pass。
- Independent draft review iteration 2: `acceptable as draft` (`ses_004fc695fffex3u3nJ2ctp7w3W`) — BLOCKER 已解决。实仓核实双路径接线准确（Receive/Return approve+reverseApprove 经 skeleton `this.validateTransitionForXxx`；Invoice/Payment approve+reverseApprove 经 facade `processor.validateTransitionForXxx`；Invoice/Payment submit/reject/withdraw 经 skeleton）。Phase 1 Decision (B) + Phase 2 接线按 skeleton vs facade 分化执行。三项非阻塞细化移交 Phase 1 Decision 裁定：(1) facade 路径目标态集中化是否需编辑 `doApprove`/`doReverseApprove`；(2) facade `validateTransitionForSubmit/Withdraw/Reject` 疑似死代码须确认无其他调用方；(3) facade `validateTransitionForApprove/ReverseApprove` 调用方爆炸半径核实。计划保持 `draft`（§11.2 M4 plan-first 人工/owner-doc 门控未解除）。
- **M4 plan-first 人工/owner-doc 门控状态：confirmed（2026-08-13）**（§11.2 M4 (i)）。草案审查已收敛（acceptable as draft）。
- **M4 plan-first 门控确认记录（人工，2026-08-13）**：人工确认「以行为保持的矩阵集中化方式迁移此 4 轴、过账/stock move/reversal-listener 路径完整保留」可接受。门控解除，`Plan Status: draft → active`。

## Closure Gates

- [x] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)；2026-08-13 人工确认，见 Draft Review Record 门控确认记录）
- [ ] 范围内行为完成（四实体 approveStatus Bean + PROC 路径接线 + 层 1 矩阵 + 层 2 四方对照 + 层 3 回归）
- [ ] 相关文档对齐
- [ ] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl module-purchase/erp-pur-service -am` 全绿 + `bash docs/audits/nop-compliance-checker.sh` 全 19 规则 actual ≤ baseline
- [ ] 无范围内项目降级为 deferred/follow-up（reverseApprove 骨架缺陷已显式移交既有 successor ownership）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### reverseApprove 共享骨架 §16.4 合规化

- Classification: `confirmed live defect moved to explicit successor ownership`
- Why Not Blocking Closure: 共享骨架 `AbstractReverseApproveProcessor.doReverseApprove` 返回 SUBMITTED，违反权威 `domain-design-guidelines.md §16.4`（应 REJECTED）。本计划 Bean 据实保持各实体当前行为（预期全部=REJECTED，零行为回归）。修复触及 `module-common-service` 共享骨架（跨域影响），超出单域 M4 范围。**与 M3 采购审批计划（2026-08-13-0945-1）同源 successor**，非新 successor。
- Successor Required: yes（触发条件 = 独立「reverseApprove 骨架 §16.4 合规化」plan）

### Delta 覆盖运行时实证

- Classification: `optimization candidate`
- Why Not Blocking Closure: M4 保护域单项 Delta 可选；cs 试点 M1.2 已实证机制。归 M5.3。
- Successor Required: yes（触发条件 = M5.3 最终跨域 Delta 覆盖回归）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 裁定选项 (c) 显式排除；更强写锁须改 ORM/xmeta（保护区 ask-first）。
- Successor Required: no（仅当产品要求全局强制矩阵写锁时重开）

## Closure

Status Note: pending execution

Closure Audit Evidence:

- Auditor / Agent: pending

Follow-up:

- <无非阻塞跟进；Deferred 项均为既定 successor>
