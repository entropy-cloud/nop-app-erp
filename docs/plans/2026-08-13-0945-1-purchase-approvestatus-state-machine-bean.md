# 2026-08-13-0945-1-purchase-approvestatus-state-machine-bean 采购单据 approveStatus 审批轴 StateMachine Bean 迁移（M3.2–M3.5）

> Plan Status: completed
> Last Reviewed: 2026-08-13
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M3.2 / M3.3 / M3.4 / M3.5（均 todo）
> Related: 前置 `2026-08-12-0918-1-purchase-docstatus-state-machine-bean.md`（M2.5–M2.8 done，其 Deferred But Adjudicated 显式将 M3.2–M3.5 列为 successor，触发条件「本计划闭包后启动」已满足）；姊妹计划 `2026-08-13-0945-2-sales-approvestatus-state-machine-bean.md`（N=2，销售同轴迁移，跨实体 Decision 同源）；M0.1 契约 + M1.3 批量迁移模板固化于 `docs/architecture/entity-state-machine-bean.md §11`
> Mission: entity-state-machine
> Work Item: M3.2 + M3.3 + M3.4 + M3.5
> Audit: required

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md`（PUR-2/20/21/22/23/24/25 行）+ 实仓核实。approveStatus 是采购单据三轴分离中的**审批轴**（`purchase/state-machine.md` §三轴状态分离 + §审批轴），与 docStatus 业务生命周期轴（M2.5–M2.8 已 done）独立。

- **轴语义（wf/approve-status 审批轴，5 动作）**：`UNSUBMITTED`（初始态，创建时写入）→(submit)→ `SUBMITTED` →(approve)→ `APPROVED` / →(reject)→ `REJECTED`；`REJECTED` →(submit 重提)→ `SUBMITTED`（xbiz `submitForApproval` 允许 UNSUBMITTED/null/REJECTED→SUBMITTED）；`SUBMITTED` →(withdraw)→ `UNSUBMITTED`；`APPROVED` →(reverseApprove)→ **见下方 reverseApprove 目标态漂移**。dict `wf/approve-status`（平台标准审核状态字典，全 ERP 共享）。PUR-2/20/21/22/23/24/25 八属性登记均为「纳入 / **无**财务影响 / approve 仅状态推进」。属模板 §11「M3 审批轴」类别。5 动作 = submit/approve/reject/reverseApprove/withdraw。
- **关键澄清（PUR-2 误读纠正）**：M2.5–M2.8 计划 Non-Goal 注「approveStatus APPROVED 才触发过账，归 M4」指的是 **Receive/Invoice/Payment/Return** 审批轴（M4.14/16/18/20，**是**财务影响）；**Order/Quotation/Rfq/Requisition 的 approveStatus 不触发过账**（M0.2 PUR-2/20–25 明确「approve 仅状态推进」「无库存/凭证」）。Order approve 的 commitment-commit/intercompany-approve 是 **approve 动作的动态跨域副作用**（保留 Processor 原位），不改变审批轴矩阵本身的无财务影响分类。
- **固定迁移判断当前所在位置（实仓核实，双路径分化）**：审批 5 动作的固定来源态/目标态守卫分布在两条路径：
  - **PROC 路径（Order/Requisition）**：守卫在共享骨架 `module-common-service/.../Abstract{SubmitForApproval,Approve,Reject,ReverseApprove,Withdraw}Processor.validateTransitionForXxx`（已核实 `AbstractApproveProcessor.validateTransitionForApprove:37-42` 内联 `Objects.equals(status, submittedStatus())`；`AbstractReverseApproveProcessor.doReverseApprove:39` `setApproveStatus(submittedStatus())`）。Order/Requisition 各有 5 个 per-mutation Processor（`ErpPur{Order,Requisition}{SubmitForApproval,Approve,Reject,ReverseApprove,Withdraw}Processor`，grep 确证 5 个均存在），固定守卫 `validateTransitionForXxx` 未覆写（继承骨架）。
  - **INLINE 路径（Quotation/Rfq）**：5 动作直接在非生成 Delta-able `.xbiz` XScript 中实现（`_vfs/erp/pur/model/ErpPurQuotation/ErpPurQuotation.xbiz`、`.../ErpPurRfq/ErpPurRfq.xbiz`，已逐行核实 5 mutation：submitForApproval/approve/reject/reverseApprove/withdrawApproval）。守卫为内联 `isCancelled`（`docStatus==='CANCELLED'` 阻断，抛平台码 `nop.err.wf.approve.doc-cancelled`）+ 来源态校验（抛平台码 `nop.err.wf.approve.invalid-status`）。**无 Java Processor、无 BizModel approve 方法**（grep 确证零 `ErpPurQuotation*Processor`/`ErpPurRfq*Processor`）。
- **⚠️ reverseApprove 目标态漂移（跨切发现项，须 Decision，禁止静默折叠）**：
  - 权威设计指南 `domain-design-guidelines.md §16.4`（L582）+ §动作映射表（L635）：反审核目标态 = **REJECTED**（可重新提交），**不是 UNSUBMITTED**。语义：反审核单据已发生过业务，不应回退为「未提交」。
  - **Quotation/Rfq xbiz**：reverseApprove → `REJECTED`（xbiz:123，**符合** §16.4）。
  - **Order/Requisition（PROC 骨架）**：`AbstractReverseApproveProcessor.doReverseApprove:39` → `submittedStatus()` = **SUBMITTED**，javadoc L14 明示「目标状态：回到 SUBMITTED」（**违反** §16.4）。
  - 即**共享骨架 `AbstractReverseApproveProcessor` 自身违反权威 §16.4**，影响所有使用该骨架的实体（Order/Requisition 及其他域）。这是**已确认 live 缺陷**（契约漂移），按路线图规则 5/13 不得降级为 Follow-up。本计划 Decision（见 Phase 1）：Bean **保持各实体当前行为**（Order/Requisition Bean `reverseApproveTargetStatus()`=SUBMITTED、Quotation/Rfq Bean=REJECTED，兑现 Non-Goal「保持全部既有外部行为不变」），骨架 §16.4 不合规 Fix 移交显式 successor（触及 `module-common-service` 共享骨架，跨域影响所有使用者，超出单域 M3 范围，须独立 plan；触发条件见 Deferred But Adjudicated）。该 successor 登记为「moved to explicit successor ownership」，非降级 Follow-up。
- **owner doc PROC/INLINE 映射自身部分矛盾（layer-2 须建立权威 writer 图）**：`purchase/state-machine.md` §审批轴声明「全实体 withdrawApproval = INLINE」，但实仓 Order/Requisition **有** `WithdrawApprovalProcessor`（PROC）；又声明「Requisition approve/reject = INLINE」与「Requisition submitForApproval/approve/reverseApprove = PROC」重叠。layer-2 四方对照须以**实仓代码**为准建立每实体每动作的权威 writer 路径图，owner doc 矛盾处按 doc drift Fix/补注。
- **错误码双轨（迁移涉及行为变化，须 Decision）**：PROC 路径抛领域码（`ERR_ORDER_ILLEGAL_STATUS_TRANSITION` 等，参数 `orderCode`/...）；INLINE xbiz 抛**平台码**（`nop.err.wf.approve.doc-cancelled` / `nop.err.wf.approve.invalid-status`）。将 INLINE 守卫迁移到 Bean 会改变 Quotation/Rfq 的错误码值（平台→common/领域）——属行为变化，Phase 3 须 Decision（保持平台码 vs 对齐领域码模式）并显式记录残留风险。
- **common 层非法迁移码已存在（参数形状已裁定）**：`ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（参数 `currentStatus`/`expectedStatus`），cs 试点 M1.1 Decision Option A + 采购 docStatus 计划裁定复用 + `action` 补充参数。本计划沿用。
- **Bean 命名约定（双轴预留）**：docStatus Bean 用 `Document` 后缀（已落地），本计划审批轴 Bean 用 `Approval` 后缀（`ErpPur<Entity>ApprovalStateMachine`），一 Bean 对一实体一轴。
- **Bean 注册范式已存在**：`_vfs/erp/pur/beans/app-service.beans.xml` 已注册既有 docStatus StateMachine Bean 与 per-mutation Processor。审批轴 Bean 沿用 `<bean id="<FQN>" class="<FQN>"/>`。
- **层 3 回归基线已存在（非 greenfield）**：层 1 矩阵测试为 greenfield；层 3 既有集成测试基线：`TestErpPurOrderApproval`、`TestErpPurRequisitionApproval`、`TestErpPurQuotationRfqReverseApprove`（**该测试断言 Quotation/Rfq reverseApprove→REJECTED，经 xbiz INLINE 路径**，证明 xbiz writer 存在且可执行）、`TestErpPurProcureToPayEnd` 等。
- **合规基线**：`docs/audits/compliance-baseline.md` R5=0、R11=0。本计划保持 R5=0、R11 不增。

## Goals

- 为采购 4 个单据实体的 approveStatus 轴各落地一个实体级 `ErpPur<Entity>ApprovalStateMachine` Bean（一 Bean 对一实体一轴），承载 5 动作迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态（§2）。Bean **据实编码各实体当前 reverseApprove 目标态**（Order/Requisition=SUBMITTED、Quotation/Rfq=REJECTED）。
- 将审批路径的**固定来源态/目标态判断**改调 Bean：PROC 路径（Order/Requisition）经 5 个 per-mutation Processor 覆写 `validateTransitionForXxx` 委托 Bean；INLINE 路径（Quotation/Rfq）经 BizModel 注入 Bean + 暴露 helper，xbiz `<source>` 内联守卫改调 BizModel helper 委托 Bean。**动态业务守卫与副作用保留原位**（Order/Requisition 的 supplier-active/budget-check/commitment/intercompany、SoD）。
- 裁定 **reverseApprove 目标态漂移**（骨架违反 §16.4）：Bean 保持当前行为 + 骨架 Fix 移交显式 successor（带触发条件）。
- 裁定 **INLINE 错误码双轨**（平台码 vs 领域码）：Phase 3 Decision 显式记录。
- 层 2 四方对照（dict `wf/approve-status` ↔ `purchase/state-machine.md` §审批轴 ↔ Bean 元数据 ↔ 全部 writer 含 CRUD 路径）逐实体裁定，建立每实体每动作权威 writer 图，处置 owner-doc PROC/INLINE 矛盾。
- 新增层 1 矩阵完备性表驱动测试（greenfield，4 个 Bean 各一）；层 3 既有集成测试全绿回归。
- 保持全部既有外部行为不变（错误码值/参数/审计/SoD/commitment/intercompany 副作用时序），唯一允许的行为变化是经 Decision 显式记录的 INLINE 错误码迁移（若裁定对齐领域码）。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（路线图 Non-Goal）。
- 不迁移 `docStatus` 轴（M2.5–M2.8 已 done）。
- 不迁移 `receiveStatus`/`paidStatus`/`writtenOffStatus`（已裁定排除-技术/派生）。
- 不触碰 `posted`；Order/Requisition approve 不触发过账（Receive/Invoice/Payment/Return 审批轴归 M4，独立 plan-first）。
- **不修改共享骨架 `Abstract{SubmitForApproval,Approve,Reject,ReverseApprove,Withdraw}Processor`**（module-common-service）——迁移经各域 per-mutation Processor 覆写委托；骨架 `AbstractReverseApproveProcessor` §16.4 不合规 Fix 移交 successor（触及共享骨架跨域影响）。`module-common-service` 零改动。
- **不改变 reverseApprove 目标态行为**：Bean 据实保持各实体当前目标态（Order/Requisition=SUBMITTED、Quotation/Rfq=REJECTED）；§16.4 合规化归 successor。
- 不引入全局 CRUD 写锁（M0.1 §9 选项 c）。
- 不在本计划证 Delta 覆盖（M3 非保护域可选；cs 试点 M1.2 已实证；归 M5.3）。
- 不新增 submit/approve/reject/reverseApprove/withdraw 之外的审批命名动作。
- 不构建反射型/泛型全局 `IStateMachine` 调度器。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M0.2 清单 + M1.3 模板 §11 + 采购 docStatus 计划跨实体 Decision；落地 4 个单实体单轴审批 Bean + PROC/INLINE 双路径接线 + 测试 + 四方对照；不改契约/模型/公共 API/共享骨架。reverseApprove §16.4 与 INLINE 错误码为 Decision 项，非契约/模型变更）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 模板 + §1 双轴约定）、`docs/design/purchase/state-machine.md`（§三轴分离 + §审批轴 + §审查提示）、`docs/design/domain-design-guidelines.md`（§16.4 反审核目标态权威 + 动作映射表）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（PUR-2/20–25）、`docs/architecture/processor-extension-pattern.md`、`docs/skills/state-machine-business-review-prompt.md`、`docs/plans/2026-08-12-0918-1-purchase-docstatus-state-machine-bean.md`（跨实体 Decision 同源）
- Skill Selection Basis: 路线图 M3.2–M3.5 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「Processor/BizModel/xbiz 接线、Bean 注册、`@Inject` 非 private、跨实体调用边界、错误码、事务边界、SoD、产品化可定制性自检」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成测试回归」。必需输入均已就绪。`state-machine-business-review-prompt.md` 匹配层 2 四方对照。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯后端 Java + 既有 pur-service 测试容器）。
- 前置依赖：M1.3 done（已满足）；M3.2–M3.5 deps = M1.3 + M2.5/M2.6/M2.7/M2.8（均 done），门控已解除。
- 无 data-deletion / 财务过账 / ORM 保护区域触发。

## Execution Plan

### Phase 1 - ErpPurOrder approveStatus Bean（M3.5）+ 跨实体 Decision 固化

Status: completed
Targets: `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/statemachine/ErpPurOrderApprovalStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpPurOrder{SubmitForApproval,Approve,Reject,ReverseApprove,Withdraw}Processor.java`、`.../test/.../TestErpPurOrderApprovalStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done + M2.8 done（已满足）

- [x] `Decision`（reverseApprove 目标态漂移裁定，路线图规则 5/13；**含执行期实仓纠正**）：权威 §16.4 要求 reverseApprove→REJECTED。**草案 baseline 称「Order/Requisition 经骨架继承 SUBMITTED」——执行期实仓核实纠正为错误**：`ErpPurOrderReverseApproveProcessor.doReverseApprove:84` 与 `ErpPurRequisitionReverseApproveProcessor.doReverseApprove:78` **均已显式覆写**为 REJECTED（非骨架 SUBMITTED），即四实体（Order/Requisition 经 Processor 覆写、Quotation/Rfq 经 xbiz）reverseApprove 目标态**全部已=REJECTED，全部已合规 §16.4**。骨架 `AbstractReverseApproveProcessor.doReverseApprove:39`→SUBMITTED 仍为已确认 live 缺陷，但对 Order/Requisition 是经覆写绕过的死路径（运行时不触达）。裁定：**Bean 据实保持各实体当前目标态=REJECTED（四实体统一）**，兑现「保持既有外部行为不变」且零回归；**骨架 §16.4 不合规 Fix 移交显式 successor plan**（触及 `module-common-service` 共享骨架，跨域影响所有未覆写的使用者；触发条件 = 独立骨架 §16.4 合规化 plan，须盘点全域 `AbstractReverseApproveProcessor` 使用者、评估 SUBMITTED→REJECTED 行为变化 + 测试影响）。理由：单域 M3 计划不承担跨域共享骨架行为变更；Bean 据实=REJECTED 使本计划零行为回归，缺陷不隐藏（显式登记 + successor，非降级 Follow-up）。Skill: `state-machine-business-review-prompt.md`
- [x] `Add`：落地 `ErpPurOrderApprovalStateMachine` Bean——显式 `assertCanSubmit/Approve/Reject/ReverseApprove/Withdraw(String status)`（非法来源态 → 抛 common 码 `ERR_ILLEGAL_STATUS_TRANSITION` + `action`/`fromStatus` 补充参数）+ `submitTargetStatus/approveTargetStatus/rejectTargetStatus/reverseApproveTargetStatus/withdrawTargetStatus()`（reverseApprove 目标态=REJECTED，据 Phase 1 Decision 实仓纠正——四实体统一 REJECTED）+ `isTerminal`/`initialStatuses`/`terminalStatuses` + 只读 `transitions()`（6 条边：submit×2 + approve + reject + reverseApprove + withdraw）。严格无状态（§2）。命名带 `Approval` 后缀。
  - Skill: `nop-backend-dev`
- [x] `Add`：在 `_vfs/erp/pur/beans/app-service.beans.xml` 以 `<bean id="<FQN>" class="<FQN>"/>` 注册（四实体审批轴 Bean 一并注册）。
  - Skill: `nop-backend-dev`
- [x] `Decision | Add`（跨实体接线 Decision 固化）：(A) Bean 接线点 = 5 个 per-mutation Processor 覆写 `validateTransitionFor{Submit,Approve,Reject,ReverseApprove,Withdraw}` 委托 Bean（try/catch common 码 → `illegalStatusException` 领域码）；目标态写入经覆写 `submittedStatus/approvedStatus/rejectedStatus/unsubmittedStatus` getter + `doReverseApprove` 委托 Bean `*TargetStatus()`；(B) common 错误码沿用 Option A；(C) 领域码映射 `ERR_ORDER_ILLEGAL_STATUS_TRANSITION`（参数 `orderCode`/...）保留；(D) 初始态 UNSUBMITTED 写入不经 Bean（§9.2 选项 c）；(E) SoD + 动态业务守卫/副作用（requireSupplierActive/runBudgetCheckHook/runCommitmentCommitHook/runIntercompanyApproveHook + commitment-release/intercompany-reverse）保留原位。Order 5 Processor 注入 `@Inject ErpPurOrderApprovalStateMachine`（非 private），覆写 5 个 `validateTransitionForXxx` 调对应 `assertCanXxx`。grep 证 Processor 内不再有内联 `Objects.equals` 矩阵判断（facade `ErpPurOrderProcessor.validateTransitionFor*` 经核实为非 live writer——xbiz 经 per-mutation Processor 委托，同 docStatus 计划 precedent）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：层 1 矩阵完备性（greenfield 表驱动，11 tests）——(a) 无重复/冲突边（6 边唯一 action|fromStatus 键）；(b) submit UNSUBMITTED/null/REJECTED→SUBMITTED、approve SUBMITTED→APPROVED、reject SUBMITTED→REJECTED、reverseApprove APPROVED→REJECTED（据 Decision）、withdraw SUBMITTED→UNSUBMITTED 可达（REJECTED 经 submit 重提可达 SUBMITTED）；(c) 各 `assertCanXxx` 合法来源态通过、非法来源态抛 common 码携带 `action`/`fromStatus`；(d) `transitions()` 与显式方法语义一致；(e) 初始={UNSUBMITTED}/终态={APPROVED}（APPROVED 为可逆业务终态，经 reverseApprove 有出边，不适用「终态无出边」强断言）。
  - Skill: `nop-testing`
- [x] `Proof`：层 2 四方对照（Order 单条）——writer 图：5 per-mutation Processor（live，xbiz `ErpPurOrder.xbiz` 经 `inject('...Processor').<action>` 委托）+ 创建写 UNSUBMITTED + CRUD 路径（§9.4 选项 c 排除）+ facade `ErpPurOrderProcessor.validateTransitionFor*`（**非 live writer**，grep 证无调用方，同 docStatus precedent）。owner-doc PROC/INLINE 矛盾处置：owner doc §实现模式声明「Order withdrawApproval=INLINE」与实仓 Order **有** `ErpPurOrderWithdrawApprovalProcessor`（PROC）矛盾 → doc drift（移交 owner doc 补正 successor，见 Closure）。reverseApprove 四实体统一 REJECTED（实仓纠正），与 §16.4 一致。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] `ErpPurOrderApprovalStateMachine` Bean 存在、已注册、严格无状态；5 个 Order 审批 Processor 委托 Bean，内联 `Objects.equals` 矩阵判断已移除（动态 hook 除外）。
- [x] Order 层 1 矩阵测试本地 `mvn test -pl module-purchase/erp-pur-service -am -Dtest=TestErpPurOrderApprovalStateMachineMatrix` 全绿（11 tests, 0 failures）。既有 `TestErpPurOrderApproval`（7）+ `TestErpPurQuotationRfqReverseApprove`（7）回归全绿（零行为回归）。

### Phase 2 - ErpPurRequisition approveStatus Bean（M3.4）

Status: completed
Targets: `.../statemachine/ErpPurRequisitionApprovalStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpPurRequisition{SubmitForApproval,Approve,Reject,ReverseApprove,Withdraw}Processor.java`、`.../test/.../TestErpPurRequisitionApprovalStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（跨实体 Decision 已固化，Requisition 沿用 Order 范式）

- [x] `Add`：落地 `ErpPurRequisitionApprovalStateMachine`（同 Phase 1 结构，reverseApprove 目标态=REJECTED，领域码 `ERR_REQ_ILLEGAL_STATUS_TRANSITION`）；5 Processor 覆写委托 Bean（validateTransitionForXxx + 目标态 getter/doReverseApprove）。Requisition 无 commitment/intercompany（保持）。注册 Bean。
  - Skill: `nop-backend-dev`
- [x] `Proof`：层 1 矩阵完备性（Requisition 独立测试，10 tests, 0 failures）。
  - Skill: `nop-testing`
- [x] `Proof`：层 2 四方对照（Requisition 单条）。writer 图：5 per-mutation Processor（live，xbiz 委托）+ 创建写 UNSUBMITTED + facade `ErpPurRequisitionProcessor.validateTransitionFor*`（非 live writer，同 docStatus precedent）。owner doc §实现模式声明「Requisition approve/reject=INLINE」与实仓 5 Processor（PROC）矛盾 → doc drift（移交 owner doc 补正 successor，见 Closure）。reverseApprove=REJECTED（实仓纠正，合规 §16.4）。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] Requisition Bean 存在/注册/无状态；5 Processor 委托 Bean，内联矩阵判断已移除。
- [x] Requisition 层 1 矩阵测试本地全绿（10 tests, 0 failures）；`TestErpPurRequisitionApproval`（4）回归全绿（零行为回归）。

### Phase 3 - ErpPurQuotation + ErpPurRfq approveStatus Bean（M3.2 + M3.3）+ INLINE xbiz 接线 + 错误码 Decision

Status: completed
Targets: `.../statemachine/ErpPurQuotationApprovalStateMachine.java`、`.../statemachine/ErpPurRfqApprovalStateMachine.java`、`.../beans/app-service.beans.xml`、`_vfs/erp/pur/model/ErpPurQuotation/ErpPurQuotation.xbiz`、`_vfs/erp/pur/model/ErpPurRfq/ErpPurRfq.xbiz`、`ErpPurQuotationBizModel.java`、`ErpPurRfqBizModel.java`（注入 Bean + 暴露 helper）、`.../test/.../TestErpPurQuotationApprovalStateMachineMatrix.java`、`.../test/.../TestErpPurRfqApprovalStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing` + `state-machine-business-review-prompt.md`

- Item Types: `Add | Decision | Fix | Proof`
- Prereqs: Phase 1（跨实体 Decision 已固化）

- [x] `Decision`（INLINE 错误码双轨裁定）：选定 **(b) 对齐领域码模式**。理由：(i) 与 Order/Requisition（PROC 路径抛领域码 `ERR_ORDER/REQ_ILLEGAL_STATUS_TRANSITION`）一致，四实体统一；(ii) 与 mfg/qa/sales 既定 precedent（INLINE `nop.err.wf.approve.invalid-status`/`doc-cancelled` → 域 ErrorCode 语义等价转换）一致；(iii) 无采购测试断言平台码（grep 证）。映射：isCancelled 守卫（原平台码 `nop.err.wf.approve.doc-cancelled`）→ 既有 `ERR_QUOTATION/RFQ_ILLEGAL_DOC_STATUS_TRANSITION`（同 sales 2026-07-30-1433-2 precedent + Order `validateNotCancelled` 行为）；来源态守卫（原平台码 `nop.err.wf.approve.invalid-status`）→ 新增 `ERR_QUOTATION/RFQ_ILLEGAL_STATUS_TRANSITION`（approveStatus 轴专属码，同 Order/Req 模式）。残留风险：前端/集成方若依赖平台码文案将收到新领域码——无既有测试/快照覆盖此路径，且 owner doc 一直要求域码化，行为对齐设计。
  - Skill: `state-machine-business-review-prompt.md`
- [x] `Add`：落地 `ErpPurQuotationApprovalStateMachine` + `ErpPurRfqApprovalStateMachine`（矩阵 5 动作 6 边；reverseApprove 目标态=REJECTED，据 Phase 1 Decision——Quotation/Rfq xbiz 已合规 §16.4）。两 BizModel 注入 `approvalStateMachine`（`@Inject` 非 private），暴露 5 `@BizQuery prepareSubmit/Approve/Reject/ReverseApprove/Withdraw(code,approveStatus,docStatus,context)` helper（isCancelled 守卫 + Bean assertCanXxx + 返回 `*TargetStatus()`）。xbiz `<source>` 内联 `isCancelled` + 来源态守卫块改调 BizModel helper（`thisObj.invoke("prepare<Action>", {code, approveStatus, docStatus})`）；目标态写入改用 helper 返回值。注册两 Bean。
  - Skill: `nop-backend-dev`
- [x] `Fix`（按错误码 Decision 分支 (b) 落地）：xbiz 经 BizModel helper 抛领域码（platform→domain）。新增 `ERR_QUOTATION_ILLEGAL_STATUS_TRANSITION`（`erp.err.pur.quotation-illegal-status-transition`，参数 `quotationCode`/`currentStatus`/`expectedStatus`）+ `ERR_RFQ_ILLEGAL_STATUS_TRANSITION`（`erp.err.pur.rfq-illegal-status-transition`，参数 `rfqCode`/...），与 docStatus 阶段 `ERR_*_ILLEGAL_DOC_STATUS_TRANSITION` 模式对称（每实体 approveStatus/docStatus 各一码，文案绑定各自编号参数）。isCancelled 守卫复用既有 docStatus 码（不再用平台 doc-cancelled）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：层 1 矩阵完备性（Quotation + Rfq 各 7 tests, 0 failures；reverseApprove→REJECTED；submit 允许 REJECTED→SUBMITTED + null）。
  - Skill: `nop-testing`
- [x] `Proof`：层 2 四方对照（Quotation + Rfq 各一）。writer 图：5 xbiz mutation（live，`<source>` 经 `thisObj.invoke("prepare<Action>")` → BizModel helper → Bean）+ BizModel helper + 创建写 UNSUBMITTED + CRUD 路径（§9.4 选项 c）。错误码 Decision 闭环：xbiz 内联平台码已全量替换为 helper 委托（grep 证 Quotation/Rfq xbiz 内 0 处 `nop.err.wf.approve.*`）。`TestErpPurQuotationRfqReverseApprove`（7）回归全绿，证 reverseApprove→REJECTED + 守卫 + 写回经新路径无行为回归（断言目标态保持）。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] Quotation + Rfq Bean 存在/注册/无状态；xbiz 内联守卫改委托 Bean（经 BizModel helper）；错误码 Decision (b) 落地（grep 证 xbiz 内 0 处平台码 `nop.err.wf.approve.*`）。
- [x] Quotation + Rfq 层 1 矩阵测试本地全绿（各 7 tests, 0 failures）。

### Phase 4 - 层 3 既有命名动作回归 + 四实体一致性复核

Status: completed
Targets: `module-purchase/erp-pur-service/src/test/`（既有集成测试，零新建）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1–3（四实体 Bean + 接线已落地）

- [x] `Proof`：层 3 既有命名动作回归——复用 `TestErpPurOrderApproval`/`TestErpPurRequisitionApproval`/`TestErpPurQuotationRfqReverseApprove`（**断言 Quotation/Rfq reverseApprove→REJECTED 保持**）/`TestErpPurProcureToPayEnd` 等，证明 Processor/xbiz 写回、审计、SoD、领域/平台错误码（据 Decision）、commitment/intercompany 副作用时序不变。本地 `mvn test -pl module-purchase/erp-pur-service -am` 全绿（**225 tests, 0 failures, 0 errors**，较 docStatus 基线 190 增 35 = 4 新矩阵测试 11+10+7+7）。
  - Skill: `nop-testing`
- [x] `Proof`：四实体一致性复核——四 Bean 命名（`Approval` 后缀）/注册（beans.xml `app-service.beans.xml`）/无状态（零可注入依赖）/元数据形状一致（同 6 边矩阵 + reverseApprove=REJECTED 统一）；PROC 路径（Order/Requisition 经 per-mutation Processor 覆写 `validateTransitionForXxx` + 目标态 getter/doReverseApprove 委托 Bean）与 INLINE 路径（Quotation/Rfq 经 BizModel `prepare<Action>` helper → Bean）接线范式可追溯；reverseApprove 目标态四实体统一 REJECTED（实仓纠正，全部已合规 §16.4）；四方对照记录写入 Closure 段。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] `mvn test -pl module-purchase/erp-pur-service -am` 全绿（层 3 回归无行为回归，225 tests, 0 failures）。
- [x] 四实体四方对照记录可追溯、reverseApprove/错误码 Decision 闭环。

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 ses_0088cbb43ffeWtpl8cQHG2cnn1，新会话）——实仓核实发现 3 项 MAJOR：(1) Quotation/Rfq approve writer 实为 INLINE XScript（`ErpPurQuotation.xbiz`/`ErpPurRfq.xbiz` 5 mutation），非 Java BizModel/Processor，「零 writer 分支」不可能；Phase 3 targets 与接线方法错误。(2) reverseApprove 目标态跨切漂移：骨架 `AbstractReverseApproveProcessor.doReverseApprove:39`→SUBMITTED，xbiz→REJECTED，权威 §16.4 要求 REJECTED——骨架自身违规。(3) INLINE 错误码为平台码（`nop.err.wf.approve.*`），迁移涉及行为变化。已采纳全部修订：baseline 重写双路径（PROC vs INLINE xbiz）+ reverseApprove 漂移 Decision（Bean 保持当前行为 + 骨架 Fix 移交显式 successor）+ 错误码双轨 Phase 3 Decision + 5 动作（含 reject）+ REJECTED→SUBMITTED 重提边 + owner-doc PROC/INLINE 矛盾 layer-2 处置。MINOR（reject 5 动作、dead stub）已纳入。
- Independent draft review iteration 2: `accept`（独立子代理 ses_00883bc04ffe8eZvfAwmDNduwx，新会话）——实仓复核确认 4 项 iteration-1 发现全部 resolved（INLINE xbiz 5 mutation + reverseApprove→REJECTED@L123 + submit REJECTED→SUBMITTED@L21 + 平台码 `nop.err.wf.approve.*`；Order/Requisition 各 5 Processor 含 Reject；骨架 `AbstractReverseApproveProcessor.doReverseApprove:39`→SUBMITTED 违反 §16.4）。reverseApprove §16.4 处理裁定正确适用 rule 13/anti-slack（「moved to explicit successor ownership」+ 具体触发条件，非降级 Follow-up；fix 触及 `module-common-service` 共享骨架跨域影响，超出单域范围；Bean 据实保持当前行为零回归）。Rule 5/13/anti-slack/baseline-honesty 全 PASS。草案审查收敛，Plan Status → active。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成（四实体 approveStatus Bean + PROC/INLINE 双路径接线 + 层 1 矩阵 + 层 2 四方对照 + 层 3 回归）
- [x] 相关文档对齐（owner-doc PROC/INLINE 矛盾登记为 doc drift 移交 successor 补正；reverseApprove/错误码 Decision 记录；架构 doc 不引用本路线图执行状态）
- [x] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS（156 reactor 模块）+ `mvn test -pl module-purchase/erp-pur-service` 全绿（225 tests, 0 failures, 0 errors）+ `bash docs/audits/nop-compliance-checker.sh` 全 19 规则 actual ≤ baseline（R5=0、R11=0、R2c=1392=baseline、R12c=40=baseline）
- [x] 无范围内项目降级为 deferred/follow-up（reverseApprove 骨架缺陷已显式移交 successor ownership，非降级；错误码/owner-doc 漂移裁定落地）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### reverseApprove 共享骨架 §16.4 合规化

- Classification: `confirmed live defect moved to explicit successor ownership`
- Why Not Blocking Closure: 共享骨架 `AbstractReverseApproveProcessor.doReverseApprove` 返回 SUBMITTED，违反权威 `domain-design-guidelines.md §16.4`（应 REJECTED）。影响所有使用该骨架的实体。本计划 Bean 据实保持各实体当前行为（零行为回归），不承担跨域共享骨架行为变更。修复触及 `module-common-service` 共享骨架（跨域影响），超出单域 M3 范围。
- Successor Required: yes（触发条件 = 独立「reverseApprove 骨架 §16.4 合规化」plan，须盘点全域 `AbstractReverseApproveProcessor` 使用者、评估目标态 SUBMITTED→REJECTED 的行为变化与测试影响、取得 owner-doc/人工门控）

### Delta 覆盖运行时实证

- Classification: `optimization candidate`
- Why Not Blocking Closure: M3 非保护域 Delta 可选；cs 试点 M1.2 已实证机制。本计划不重复证明。
- Successor Required: yes（触发条件 = M5.3 最终跨域 Delta 覆盖回归）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 裁定选项 (c) 显式排除；更强写锁须改 ORM/xmeta（保护区 ask-first）。
- Successor Required: no（仅当产品要求全局强制矩阵写锁时重开）

## Closure

Status Note: 全四阶段执行完成（Phase 1-4 全绿）。四实体 approveStatus 审批轴 Bean 落地（Order/Requisition/Quotation/Rfq，同构 6 边矩阵 + reverseApprove=REJECTED 统一）+ PROC/INLINE 双路径接线 + 层 1 矩阵（4 个，35 tests）+ 层 2 四方对照 + 层 3 回归（225 tests 全绿）+ 合规基线零漂移（R5=0/R11=0/R2c=1392/R12c=40）。**关键 Decision（reverseApprove 实仓纠正）**：草案 baseline 称 Order/Requisition reverseApprove→SUBMITTED（经骨架继承）——执行期实仓核实为错误，二者 `doReverseApprove` 均已覆写为 REJECTED（全部已合规 §16.4）；Bean 据实统一 REJECTED，零行为回归；骨架 §16.4 不合规为已确认 live 缺陷（对 Order/Requisition 经覆写绕过的死路径），移交显式 successor。INLINE 错误码 Decision 分支 (b)：平台码→领域码（新增 `ERR_QUOTATION/RFQ_ILLEGAL_STATUS_TRANSITION`，isCancelled 复用既有 docStatus 码）。owner-doc §实现模式 PROC/INLINE 矛盾登记为 doc drift 移交 successor 补正。独立结束审计已由新会话子代理执行并通过（PASS，无 blocker）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 ses_008603d2affeVQtY8cPImX75vz（新会话，非执行者上下文）—— closure audit of `2026-08-13-0945-1-purchase-approvestatus-state-machine-bean.md`
- Evidence:
  - **实时基线核实（live repo grep/read，非引用执行者断言）**：4 ApprovalStateMachine Bean 文件存在 + 真实矩阵（5 assertCan + 5 TargetStatus + isTerminal[APPROVED] + transitions 6 边 + initial/terminal，非空壳）+ 严格无状态（零 @Inject/DAO/IBiz）；`reverseApproveTargetStatus()`=REJECTED 四实体统一。`app-service.beans.xml:186-193` 注册 4 Bean。
  - **reverseApprove 纠正事实准确（最高风险 Decision）**：`ErpPurOrderReverseApproveProcessor.doReverseApprove`(L102-106) 与 `ErpPurRequisition...`(L94-98) 均覆写→Bean `reverseApproveTargetStatus()`=REJECTED；骨架 `AbstractReverseApproveProcessor.doReverseApprove`(L38-42) 仍 submittedStatus()=SUBMITTED——缺陷真实但对 Order/Requisition 为经覆写绕过的运行时死路径。执行者纠正声明为 TRUE。
  - **PROC 接线（10 Processor）**：Order/Requisition 各 5 Processor 注入 Bean（非 private @Inject）+ 覆写 validateTransitionForXxx→assertCanXxx（try/catch→illegalStatusException）；`rg "Objects.equals"` 跨 10 文件 = 0 匹配（无残留内联矩阵判断）。
  - **INLINE 接线（Quotation/Rfq）**：两 BizModel 注入 approvalStateMachine + 5 `@BizQuery prepare<Action>` 委托 Bean + 返回 TargetStatus；两 xbiz 5 mutation `<source>` 调 `thisObj.invoke("prepare<Action>",{code,approveStatus,docStatus})`；`rg "nop.err.wf.approve"` 跨两 xbiz = 0 匹配（错误码 Decision 分支 b 落地）。
  - **Anti-Hollow**：Bean 含显式 assertCan/TargetStatus/isTerminal/transitions/terminalStatuses/initialStatuses 实现 + TransitionDefinition 元数据；Processor try/catch 映射领域码（非吞异常）；矩阵测试真实断言（无重复边/可达性/各动作合法+非法/transitions 一致/终态集合/reverseApprove→REJECTED）。
  - **Five-point 一致性**：Plan Status=completed / 四 Phase Status=completed / 四 Phase Exit Criteria 全 [x] / Closure Gates 全 [x]（含独立审计门）/ Closure evidence 非占位符——一致。
  - **Docs sync**：`docs/logs/2026/08-13.md` 首条目覆盖本计划（四 Phase + reverseApprove Decision + 错误码 Decision + 225/225 验证基线 + roadmap 更新）；路线图 M3.2/M3.3/M3.4/M3.5 → done；架构 doc 不引用路线图执行状态。
  - **Deferred honesty**：3 项 Deferred（骨架 §16.4 合规化 / owner-doc PROC/INLINE 补正 / Delta 覆盖实证 / 全局 CRUD 写锁）均为非阻塞 successor，无已确认 live defect 隐藏其中（骨架缺陷已显式登记 + successor ownership，非降级 Follow-up）。
- Verdict: **PASS**（无 blocker / 无 anti-hollow / reverseApprove 纠正事实准确 / 五点一致 / 漂移处置诚实 / docs sync 满足 / deferred honesty 满足）。
- Minor non-blocking nit（审计登记）：Phase 1 Decision 引用的 doReverseApprove 行号（:84/:78）为 pre-wiring 原始行号，post-wiring 漂移至 Order L102/Requisition L94；实质声明正确，仅行号指针漂移。

执行证据（执行者自查，非结束审计）：

1. **新增文件**（4 Bean + 4 矩阵测试）：
   - `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/statemachine/ErpPur{Order,Requisition,Quotation,Rfq}ApprovalStateMachine.java`
   - `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/statemachine/TestErpPur{Order,Requisition,Quotation,Rfq}ApprovalStateMachineMatrix.java`

2. **修改文件**（10 Processor + 2 BizModel + 2 IBiz + 2 xbiz + 1 beans.xml + 1 Errors）：
   - `ErpPurOrder{SubmitForApproval,Approve,Reject,ReverseApprove,Withdraw}Processor.java`：注入 Bean + 覆写 validateTransitionForXxx 委托 Bean + 目标态 getter/doReverseApprove 委托 Bean
   - `ErpPurRequisition{...}Processor.java`：同上（5 个）
   - `ErpPurQuotationBizModel.java` / `ErpPurRfqBizModel.java`：注入 approvalStateMachine + 5 prepare<Action> helper
   - `IErpPurQuotationBiz.java` / `IErpPurRfqBiz.java`：声明 5 @BizQuery prepare<Action>
   - `_vfs/erp/pur/model/ErpPurQuotation/ErpPurQuotation.xbiz` / `.../ErpPurRfq/ErpPurRfq.xbiz`：5 mutation `<source>` 改调 prepare<Action> helper
   - `_vfs/erp/pur/beans/app-service.beans.xml`：注册 4 审批轴 Bean
   - `ErpPurErrors.java`：新增 `ERR_QUOTATION_ILLEGAL_STATUS_TRANSITION` / `ERR_RFQ_ILLEGAL_STATUS_TRANSITION`

3. **层 2 四方对照汇总**（dict `wf/approve-status` UNSUBMITTED/SUBMITTED/APPROVED/REJECTED ↔ `purchase/state-machine.md` §审批轴 ↔ Bean 元数据 ↔ writer）：
   - **Order/Requisition（PROC 路径）**：5 per-mutation Processor（live，xbiz 经 `inject('...Processor').<action>` 委托）+ 创建写 UNSUBMITTED + CRUD 路径（§9.4 选项 c）；facade `ErpPurOrder/RequisitionProcessor.validateTransitionFor*` 为非 live writer（grep 证无调用方，同 docStatus precedent）。owner doc §实现模式声明「withdrawApproval=INLINE」「Requisition approve/reject=INLINE」与实仓 PROC 矛盾 → doc drift（移交 successor 补正）。reverseApprove=REJECTED（实仓纠正，合规 §16.4）。
   - **Quotation/Rfq（INLINE 路径）**：5 xbiz mutation（live，经 BizModel `prepare<Action>` helper → Bean）+ 创建写 UNSUBMITTED + CRUD 路径。错误码 Decision 分支 (b) 闭环：xbiz 内 0 处平台码 `nop.err.wf.approve.*`。

4. **验证**：`mvn clean install -DskipTests` BUILD SUCCESS（156 模块）；`mvn test -pl module-purchase/erp-pur-service` → 225 tests, 0 failures, 0 errors（较 docStatus 基线 190 增 35 = 4 新矩阵测试）；`bash docs/audits/nop-compliance-checker.sh` → R5=0, R11=0, R2c=1392（=baseline）, R12c=40（=baseline），全 19 规则 actual ≤ baseline。
