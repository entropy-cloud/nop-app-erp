# 2026-08-13-1950-2-sales-m4-approvestatus-state-machine-bean 销售出库/发票/收款/退货单 ErpSalDelivery/Invoice/Receipt/Return.approveStatus 实体级状态机 Bean（M4.22 + M4.24 + M4.26 + M4.28）

> Plan Status: active
> Review Hold: §11.2 M4 (i) 人工/owner-doc 门控**已于 2026-08-13 经人工确认解除**——本计划触及受保护销售业财过账行为（approve 触发出库移动/凭证过账：Delivery→`IErpInvStockMoveBiz` 出库；Invoice→AR_INVOICE 凭证；Receipt→RECEIPT 凭证+核销；Return→入库+红字发票。reverseApprove 经 `SalReversalListener` 回写 posted=false + APPROVED→REJECTED，已由起草者经 live code 实证）。M4 plan-first 门控成立；该人工裁定非起草者可自主解除（project-context.md 会计/财务保护域硬停止）。计划本身格式/完备性/范围/结束证据就绪 + 人工门控已确认，已转 `active` 进入实施。
> Last Reviewed: 2026-08-13
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.22（ErpSalDelivery.approveStatus）+ M4.24（ErpSalInvoice.approveStatus）+ M4.26（ErpSalReceipt.approveStatus）+ M4.28（ErpSalReturn.approveStatus），均 plan-first；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 sales`（442 行段）
> Related: 前置姊妹计划 `2026-08-13-0810-2-sales-docstatus-m4-state-machine-bean.md`（M4.21+M4.23+M4.25+M4.27 docStatus 轴 draft）；M3 同轴先例 `2026-08-13-0945-2-sales-approvestatus-state-machine-bean.md`（M3.6–M3.7 approveStatus done）；姊妹 M4 计划 `2026-08-13-1950-1-purchase-m4-approvestatus-state-machine-bean.md`（N=1，采购同轴迁移，跨实体 Decision 同源）；M0.1 契约 + M1.3 模板 `docs/architecture/entity-state-machine-bean.md §11`
> Mission: entity-state-machine
> Work Item: M4.22 + M4.24 + M4.26 + M4.28
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。4 实体 approve 动作触发受保护业财过账行为（Delivery→出库 stock move；Invoice→AR_INVOICE 凭证；Receipt→RECEIPT 凭证+核销；Return→入库+红字发票），reverseApprove 经 `SalReversalListener` 逆转上述副作用并回写 posted=false + APPROVED→REJECTED。声明 §11.2 M4 硬约束：(i) plan-first + 受保护行为人工/owner-doc 门控；(ii) 过账时序/编排/失败回退（posted 回写）/红冲闭环不改；(iii) `posted` 不入轴；(iv) 跨域副作用保留原 Processor/`I*Biz` 路径；(v) 既有红冲/reversal-listener 回写闭环以 `posted`+`approveStatus` 为契约不改。本计划是 plan-first 产物，人工/owner-doc 确认门控已于 2026-08-13 解除，转 `active` 进入实施。
>
> **规则 14 bundling 声明**：M4.22（Delivery）+ M4.24（Invoice）+ M4.26（Receipt）+ M4.28（Return）属同一组件（同一 owner doc `docs/design/sales/state-machine.md`、同一 `wf/approve-status` dict、同一审批 5 动作行为契约、同一结果表面 = 销售单据 approveStatus 审批轴），按指南规则 14 合并为单计划。approveStatus 轴与 docStatus 轴（M4.21/23/25/27）结果表面不同，按既定 M2/M3 先例分计划。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 sales`（442 行段）+ 实仓核实。approveStatus 是销售单据三轴分离中的**审批轴**（`sales/state-machine.md` §三轴状态分离 + §审批轴），与 docStatus 业务生命周期轴（M4.21/23/25/27 draft）独立。

- **轴语义（wf/approve-status 审批轴，5 动作）**：`UNSUBMITTED`（初始态）→(submit)→ `SUBMITTED` →(approve)→ `APPROVED` / →(reject)→ `REJECTED`；`REJECTED` →(submit 重提)→ `SUBMITTED`；`SUBMITTED` →(withdraw)→ `UNSUBMITTED`；`APPROVED` →(reverseApprove)→ `REJECTED`（实仓预期，同 M3 先例）。dict `wf/approve-status`。SAL 行属性登记均为「纳入 / **是**（approve→出库/凭证/收款/退货）」。属模板 §11「M4 审批轴」类别。5 动作 = submit/approve/reject/reverseApprove/withdraw。
- **关键差异（与 M3.6–M3.7 Order/Quotation 的对比）**：M3 审批轴 approve **仅状态推进**（可用量只读预检）；M4 审批轴 approve **触发业财过账 + 库存移动**（Delivery→`triggerOutgoingMove`→`IErpInvStockMoveBiz`；Invoice→AR_INVOICE 凭证经 `SalInvoicePostingDispatcher`；Receipt→RECEIPT 凭证经 `SalReceiptPostingDispatcher`+核销；Return→入库 stock move+`SalReturnPostingDispatcher`）。**副作用保留 Processor 原位**（Bean 只集中固定迁移矩阵）。
- **固定迁移判断当前所在位置（实仓核实，全部 PROC 路径）**：审批 5 动作守卫在共享骨架 `Abstract{SubmitForApproval,Approve,Reject,ReverseApprove,Withdraw}Processor.validateTransitionForXxx`。**4 实体均经 per-mutation PROC 路径**（grep 确证各 5 Processor 存在），无 INLINE xbiz 路径（同 M3 Sales 先例）。
- **逐实体 writer 盘点（实仓核实）**：
  - **M4.22 ErpSalDelivery**（PROC，5 Processor）：`ErpSalDelivery{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval}Processor`。approve 编排 triggerOutgoingMove + posting。**SALES_OUTPUT 凭证由库存域 `InvAcctDocProvider` 持有**（sales 域无 DeliveryPostingDispatcher）。错误码 `ERR_ILLEGAL_STATUS_TRANSITION`（**泛型**，无 DELIVERY_ 前缀，同 Receive 先例）。
  - **M4.24 ErpSalInvoice**（PROC，5 Processor）：approve 编排 `SalInvoicePostingDispatcher`（AR_INVOICE）+ commitment-release hook（config-gated）。领域码 `ERR_INVOICE_ILLEGAL_STATUS_TRANSITION`（`ErpSalErrors.java:138`）。
  - **M4.26 ErpSalReceipt**（PROC，5 Processor）：approve 编排 `SalReceiptPostingDispatcher`（RECEIPT）+ 核销。Receipt 有 `nopFlowId`（`useWorkflow="true"`）。Receipt 另有 `Settle`/`ReverseSettlement` Processor（走 `writtenOffStatus` 轴，不迁移）。领域码 `ERR_RECEIPT_ILLEGAL_STATUS_TRANSITION`（`:155`）。
  - **M4.28 ErpSalReturn**（PROC，5 Processor）：approve 编排入库 stock move + `SalReturnPostingDispatcher`（SALES_RETURN，gated by `isEstimatedReceivableOutstanding`）。领域码 `ERR_RETURN_ILLEGAL_STATUS_TRANSITION`（`:185`）。
- **reverseApprove 目标态（§16.4 合规性）**：M3 Sales 先例（2026-08-13-0945-2）实仓纠正——`ErpSalOrder/QuotationReverseApproveProcessor.doReverseApprove` 均已覆写=REJECTED（合规 §16.4）。M4 四实体（Delivery/Invoice/Receipt/Return）预期同覆写=REJECTED，Phase 1 须实仓核实。若全部=REJECTED，Bean 统一 `reverseApproveTargetStatus()`=REJECTED。骨架 §16.4 Fix 移交既有 successor（与 M3 同源）。
- **SalReversalListener（跨域红冲）**（`erp-sal-service/.../posting/SalReversalListener.java`）：`VoucherReversedEvent` 回写 posted + approveStatus（AR_INVOICE/RECEIPT/SALES_RETURN→APPROVED→REJECTED；SALES_OUTPUT→仅 posted，approveStatus 不变）。**4 业务类型均不触 approveStatus Bean**（reversal-listener 为 finance→sales 跨域回写，非命名业务动作，保留原位不改）。
- **common 层非法迁移码已存在**：`ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（参数 `currentStatus`/`expectedStatus` + `action`）。本计划沿用。
- **Bean 命名约定**：审批轴 Bean 用 `Approval` 后缀（`ErpSal<Entity>ApprovalStateMachine`）。
- **Bean 注册范式已存在**：`_vfs/erp/sal/beans/app-service.beans.xml` 已注册 2 Document SM（Order/Quotation）+ 2 Approval SM（M3 已落地）+ 4 M4 实体各 6-7 per-mutation Processor。**4 实体 approveStatus SM Bean 未注册**（greenfield）。
- **既有测试（层 3 回归基线）**：`TestErpSalDeliveryApproval`/`TestErpSalDeliveryStockMove`/`TestErpSalInvoiceApproval`/`TestErpSalInvoicePosting`/`TestErpSalReceiptApproval`/`TestErpSalReceiptWorkflowApproval`/`TestErpSalReturnApproval`/`TestErpSalReturnPosting` + 跨域 `TestSalReversalListenerRollback`/`TestErpSalPostingDispatcherFailureHangs`/`TestErpSalOrderToCashEnd`。**无矩阵测试**。
- **合规基线**：R5=0、R11=0。本计划保持。
- **owner doc 覆盖**：`docs/design/sales/state-machine.md`（§适用对象覆盖出库/发票/收款/退货 + §三轴分离 + §审批轴）。

## Goals

- 为销售 4 个单据实体的 approveStatus 轴各落地 `ErpSal<Entity>ApprovalStateMachine` Bean，承载 5 动作迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态（§2）。reverseApprove 目标态=REJECTED（Phase 1 实仓确认）。
- 将 4 实体审批 5 Processor 的 `validateTransitionForXxx` 覆写为委托 Bean。**动态业务守卫与副作用保留原位**（Delivery 的 triggerOutgoingMove；Invoice 的 PostingDispatcher/commitment-release；Receipt 的 workflow/核销；Return 的入库 stock move；全部 `SalReversalListener` 回写）。
- 裁定 reverseApprove 目标态（骨架违反 §16.4）：Bean 保持当前行为 + 骨架 Fix 移交既有 successor。
- 层 2 四方对照逐实体裁定。新增层 1 矩阵测试；层 3 既有集成测试全绿回归。
- 保持全部既有外部行为不变。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml。
- 不迁移 `docStatus` 轴（M4.21/23/25/27 draft）。
- 不迁移 `receivedStatus`/`writtenOffStatus`。
- 不触碰 `posted`；approve 触发的过账编排保留在 `*PostingDispatcher` + Processor 原位（§11.2 M4 (ii)）。
- 不修改共享骨架（module-common-service 零改动）。
- 不改变 `*PostingDispatcher` 过账编排、`SalReversalListener` 回写语义、stock move 时序、commitment-release（§11.2 M4 (ii)/(iv)/(v)）。
- 不重命名 Delivery 的泛型错误码 `ERR_ILLEGAL_STATUS_TRANSITION`。
- 不引入全局 CRUD 写锁。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控。
- 不证 Delta 覆盖（M4 保护域单项，归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M0.2 清单 + M1.3 模板 §11 + M3 审批计划跨实体 Decision；**M4 plan-first**——approve 触发销售业财过账/存货移动）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 模板 + §11.2 M4 变体 + §1 双轴约定）、`docs/design/sales/state-machine.md`（§三轴分离 + §审批轴）、`docs/design/domain-design-guidelines.md`（§16.4）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（SAL M4.22/24/26/28 行）、`docs/architecture/processor-extension-pattern.md`、`docs/skills/state-machine-business-review-prompt.md`、`docs/plans/2026-08-13-0945-2-sales-approvestatus-state-machine-bean.md`（M3 同轴先例）
- Skill Selection Basis: 路线图 M4.22/24/26/28 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「Processor 接线、过账/stock move 副作用保留、错误码 common→域映射、`@Inject` 非 private」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成回归（含 posting-failure-hangs）」。层 2 引用 `state-machine-business-review-prompt.md`。必需输入均已就绪。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护销售业财过账行为。在人工/owner-doc 确认可接受前为阻塞前置。**[此门控已于 2026-08-13 经人工确认解除，见 Draft Review Record 门控确认记录]**
- 无端口/环境变量/CORS/密钥/.env/外部服务依赖。无数据迁移。

## Execution Plan

### Phase 1 - ErpSalDelivery approveStatus Bean（M4.22）+ 跨实体 Decision 固化

Status: planned
Targets: `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/statemachine/ErpSalDeliveryApprovalStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpSalDelivery{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval}Processor.java`、`.../test/.../TestErpSalDeliveryApprovalStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done；M4.22 deps = M1.3 + M4.21（draft，双轴独立）

- [ ] `Decision`（reverseApprove 目标态实仓确认，复用 M3 先例）：核实 4 实体 `*ReverseApproveProcessor` 目标态。M3 Sales 先例=REJECTED。Delivery/Invoice/Receipt/Return 须实仓核实，预期同覆写=REJECTED。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] `Add`：落地 `ErpSalDeliveryApprovalStateMachine` Bean——5 assertCan + 5 TargetStatus（reverseApprove=REJECTED）+ isTerminal/initialStatuses/terminalStatuses + transitions()（6 边）。严格无状态。`Approval` 后缀。
  - Skill: `nop-backend-dev`
- [ ] `Add`：在 `_vfs/erp/sal/beans/app-service.beans.xml` 以 `<bean id="<FQN>" class="<FQN>"/>` 注册 Delivery 审批轴 Bean（Phase 2 注册其余 3 Bean）。
  - Skill: `nop-backend-dev`
- [ ] `Decision | Add`（跨实体接线 Decision 复用 M3 先例）：5 Processor 覆写 validateTransitionForXxx 委托 Bean + 目标态 getter 委托 Bean。Delivery 无 sales-side PostingDispatcher（SALES_OUTPUT 库存侧 InvAcctDocProvider），triggerOutgoingMove + posting 保留原位。SoD 保留原位。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：层 1 矩阵完备性（greenfield 表驱动）。
  - Skill: `nop-testing`
- [ ] `Proof`：层 2 四方对照（Delivery 单条）。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] `ErpSalDeliveryApprovalStateMachine` Bean 存在、已注册、严格无状态；5 Delivery Processor 委托 Bean，内联矩阵判断已移除。
- [ ] Delivery 层 1 矩阵测试本地全绿。

### Phase 2 - ErpSalInvoice + ErpSalReceipt + ErpSalReturn approveStatus Bean（M4.24 + M4.26 + M4.28）

Status: planned
Targets: `.../statemachine/ErpSal{Invoice,Receipt,Return}ApprovalStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpSal{Invoice,Receipt,Return}{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval}Processor.java`、`.../test/.../TestErpSal{Invoice,Receipt,Return}ApprovalStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [ ] `Add`：落地 3 Bean（同 Phase 1 结构，reverseApprove=REJECTED）。Invoice/Receipt 的 PostingDispatcher 过账编排、commitment-release 保留原位。Receipt workflow 保留原位。Return 入库 stock move + SalReturnPostingDispatcher 保留原位。注册 3 Bean。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：层 1 矩阵完备性（3 实体独立测试）。
  - Skill: `nop-testing`
- [ ] `Proof`：层 2 四方对照（Invoice/Receipt/Return 各单条）。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] 3 Bean 存在/注册/无状态；各 5 Processor 委托 Bean，内联矩阵判断已移除。
- [ ] 3 层 1 矩阵测试本地全绿。

### Phase 3 - 层 3 既有命名动作回归

Status: planned
Targets: `module-sales/erp-sal-service/src/test/`（既有集成测试，零新建）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1–2

- [ ] `Proof`：层 3 既有命名动作回归——复用 `TestErpSalDeliveryApproval`/`TestErpSalDeliveryStockMove`/`TestErpSalInvoiceApproval`/`TestErpSalInvoicePosting`/`TestErpSalReceiptApproval`/`TestErpSalReceiptWorkflowApproval`/`TestErpSalReturnApproval`/`TestErpSalReturnPosting` + 跨域 `TestSalReversalListenerRollback`/`TestErpSalPostingDispatcherFailureHangs`/`TestErpSalOrderToCashEnd`。本地 `mvn test -pl module-sales/erp-sal-service -am` 全绿。
  - Skill: `nop-testing`
- [ ] `Proof`：四实体一致性复核。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 层 3 既有集成测试全绿（零行为回归）。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_00505dd3bffepratpQqFmttm4u`) — MAJOR：Phase 1 注册 4 Bean 但仅创建 1（Delivery），Phase 2 注册 3——注册数矛盾会导致 IoC 启动失败。v2 已修正：Phase 1 仅注册 Delivery Bean（1），Phase 2 注册 3 Bean。其余全部 baseline 声明实仓核实 TRUE（错误码/行号、SalReversalListener 4-类型回写、无 DeliveryPostingDispatcher、reverseApprove 四实体已覆写=REJECTED、§16.4 死路径确认）；模板/治理/bundling/Deferred 诚实性均 pass。
- Independent draft review iteration 2: `acceptable as draft` (`ses_004fc5facffexETWKwT7Y6JSo1`) — MAJOR 已解决。Phase 1 注册数（1）== 创建数（1 Delivery），Phase 2 注册数（3）== 创建数（3 Invoice/Receipt/Return），无 phantom registration。IoC 启动安全。Per-phase Bean 创建计数与注册计数一致。计划保持 `draft`（§11.2 M4 plan-first 门控未解除）。
- **M4 plan-first 人工/owner-doc 门控状态：confirmed（2026-08-13）**（§11.2 M4 (i)）。草案审查已收敛（acceptable as draft）。
- **M4 plan-first 门控确认记录（人工，2026-08-13）**：人工确认「以行为保持的矩阵集中化方式迁移此 4 轴、过账/reversal-listener 路径完整保留」可接受。门控解除，`Plan Status: draft → active`。

## Closure Gates

- [x] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)；2026-08-13 人工确认，见 Draft Review Record 门控确认记录）
- [ ] 范围内行为完成
- [ ] 相关文档对齐
- [ ] 已运行验证：`mvn clean install -DskipTests` + `mvn test -pl module-sales/erp-sal-service -am` 全绿 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### reverseApprove 共享骨架 §16.4 合规化

- Classification: `confirmed live defect moved to explicit successor ownership`
- Why Not Blocking Closure: 同采购 N=1 计划。与 M3 采购/销售审批计划同源 successor。
- Successor Required: yes

### Delta 覆盖运行时实证

- Classification: `optimization candidate`
- Why Not Blocking Closure: M4 保护域单项 Delta 可选；归 M5.3。
- Successor Required: yes

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 选项 (c) 排除。
- Successor Required: no

## Closure

Status Note: pending execution

Closure Audit Evidence:

- Auditor / Agent: pending

Follow-up:

- <无非阻塞跟进；Deferred 项均为既定 successor>
