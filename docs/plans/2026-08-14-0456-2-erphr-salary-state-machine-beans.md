# 2026-08-14-0456-2-erphr-salary-state-machine-beans HR 域 ErpHrSalary.paymentStatus + approveStatus 实体级状态机 Bean（M4.63 + M4.64）

> Plan Status: completed
> Review Hold: §11.2 M4 (i) plan-first 人工/owner-doc 门控**已于 2026-08-14 经人工确认解除**（见 Draft Review Record 门控确认记录）（markPaid 触发 SALARY_PAYMENT(280) 发放凭证；approve 触发 SALARY(270)/SOCIAL_INSURANCE_ER(290)/HOUSING_FUND_ER(300) 计提凭证——后三者当前 Deferred/dead-code 但 config 翻转后触发受保护行为）。门控非起草者/审查者可自主解除——经人工确认解除；已转 `active` 进入实施。
> Last Reviewed: 2026-08-14
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.63（ErpHrSalary.paymentStatus）+ M4.64（ErpHrSalary.approveStatus），均 plan-first，M4.64 deps M4.63；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md`（:335-336 + 风险展开 :456）
> Related: M2/M3 同域先例 `2026-08-13-1430-2-erphr-employee-timesheet-state-machine-beans.md`（M3.8 Employee + M3.9 Timesheet done）+ `2026-08-12-1118-3-erphr-leave-contract-state-machine-beans.md`（M2.11 LeaveRequest + M2.12 Contract done，`AbstractErpHrLeaveRequestProcessor` Bean 注入范式）；M0.1 契约 + M1.3 批量迁移模板固化于 `docs/architecture/entity-state-machine-bean.md §11`
> Mission: entity-state-machine
> Work Item: M4.63 + M4.64
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。markPaid（paymentStatus PENDING→PAID）触发 SALARY_PAYMENT(280) 发放凭证（`SalaryPostingDispatcher.tryPostPayment` active wired）。approve（approveStatus→APPROVED）应触发 SALARY(270)+SOCIAL_INSURANCE_ER(290)+HOUSING_FUND_ER(300) 计提凭证，但当前 `tryPostAccrual` 为 dead-code（zero callers，Deferred R1.26）——翻转 config 后即触发受保护行为。声明 §11.2 M4 硬约束：(i) plan-first；(ii) 过账时序/编排/失败回退不改；(iii) `posted` 不入轴；(iv) 跨域副作用保留原路径；(v) 既有红冲闭环不改。
>
> **规则 14 bundling 声明**：M4.63 + M4.64 属同一组件（同一 owner doc `docs/design/human-resource/state-machine.md` §适用对象四 + §4 发放执行独立轴、同一实体 `ErpHrSalary`、同一结果表面），按指南规则 14 合并为单计划。双轴分离 Bean（§3），M4.64 deps M4.63，分阶段落地。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md`（:335-336 + 风险展开 :456）+ 实仓核实。M2.11 LeaveRequest Bean 已落地 done，`AbstractErpHrLeaveRequestProcessor` Bean 注入范式是本计划的**直接接线模板**。

- **ErpHrSalary**（M4.63 paymentStatus + M4.64 approveStatus，双轴）：
  - **paymentStatus 3 态**（`erp-hr/salary-payment-status`，`app-erp-hr.orm.xml:72-76`）：PENDING/PAID/VOID。常量 `ErpHrConstants:36-38`。
  - **approveStatus 4 态**（`wf/approve-status` 平台 dict，4 值 UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）。ORM 列 `approveStatus`（`:742`，`ext:dict="wf/approve-status"`）。常量 `ErpHrConstants:30-33`。**dict 漂移候选**：`erp-hr/salary-approval-status`（`:77-84`，6 值 PENDING/REVIEWED/APPROVED_FINANCE/APPROVED_MANAGER/PAID/VOID）存在但**未被列引用**（legacy drift，四方对照 finding）。
  - **paymentStatus writer（全 Java）**：`PayrollCalculator:149-150` 写 PENDING+UNSUBMITTED（初始态）；`ErpHrSalaryMarkPaidProcessor:38` 写 PAID（守卫 `:24` require approveStatus==APPROVED + `:30` require paymentStatus==PENDING）；`ErpHrSalaryGenerateBankFileProcessor:40` 写 PAID（bulk，守卫同 markPaid）；`ErpHrSalaryBizModel:115` voidSalary 写 VOID（inline in BizModel，守卫 `:111-114` reject PAID）。
  - **approveStatus writer（XScript，关键架构事实）**：`ErpHrSalary.xbiz` 中 `submitForApproval:35` 写 SUBMITTED、`approve:71` 写 APPROVED、`reject:96` 写 REJECTED、`reverseApprove:122` 写 SUBMITTED、`withdrawApproval:147` 写 UNSUBMITTED。**实仓守卫（Bean 矩阵裁定依据）**：submit `:26` 允许 {UNSUBMITTED,null,REJECTED}；approve `:62` 仅 SUBMITTED；reject `:87` 仅 SUBMITTED；reverseApprove `:113` 仅 APPROVED；withdrawApproval `:138` 仅 SUBMITTED。`TestErpHrPayrollEngine:223-226` 证实 UNSUBMITTED 直接 approve 被平台守卫拒（仅 SUBMITTED 源态）。BizModel javadoc `:47-50` 明示审批轴由平台 `approval-support.xbiz` 标准动作提供。**approve 轴转换逻辑不在 Java**，在 XScript。
  - **固定守卫**：MarkPaidProcessor `:24` 内联 `APPROVE_STATUS_APPROVED.equals(...)` + `:30` 内联 `PAYMENT_PENDING.equals(...)`；voidSalary `:111-114` 内联 `PAYMENT_PAID.equals(...)` reject。`AbstractErpHrSalaryProcessor`（88 行）**无 SM 注入**（对比 `AbstractErpHrLeaveRequestProcessor:44-45` 已注入）。
  - **领域错误码**：`ERR_SALARY_ILLEGAL_STATUS_TRANSITION`（`erp.err.hr.salary.illegal-status-transition`，`ErpHrErrors:92-95`，参数 salaryId + currentStatus + expectedStatus）；`ERR_SALARY_LOCKED_AFTER_PAID`（`ErpHrErrors:96-99`，PAID 终态锁，voidSalary `:112`）。
  - **既有测试**：`TestErpHrSalaryWorkflowApproval`（178 行，approve 轴 xwf e2e：submit→3 级 agree→APPROVED；reject→REJECTED；resubmit）+ `TestErpHrPayrollEngine`（`testApprovalStateMachineAndPaidLock:173-204` UNSUBMITTED→SUBMITTED→APPROVED→PAID→voidSalary throws LOCKED；`testIllegalTransitionRejects:207` UNSUBMITTED direct approve rejected；`testGenerateBankFileTransfersSalariesToPaid:230`）+ `TestErpHrPayrollSimulation`。
  - **过账副作用**：markPaid `:36` 调 `SalaryPostingDispatcher.tryPostPayment`（SALARY_PAYMENT 280 active）；`tryPostAccrual`（SALARY 270 + 290 + 300 dead-code，zero callers，Deferred R1.26）。`posted` 列存在（`:764`）但 Salary 当前**无 setPosted writer**（Deferred）。
  - **voidSalary R6.7 gap**：voidSalary 是 inline BizModel 方法（非 Processor），而 calculateSalary/generateBankFile/markPaid/runPayroll 均有 Processor。R6.7 per-mutation 拆分惯例下 voidSalary 缺 Processor（audit-remediation-roadmap.md :617-620 conspicuously absent）。
  - **无矩阵测试**。

- **既有 Bean 注册**：`_vfs/erp/hr/beans/app-service.beans.xml`——4 SM Bean 已注册（LeaveRequest/Contract/Timesheet/Employee，L114-121）+ Salary 4 Processor（L49-56）+ Salary 4 Simulation Processor（L57-64）+ 3 Posting Bean（L23-28）。**Salary SM Bean 未注册**（greenfield）。
- **M2.11 接线模板（直接范本）**：`AbstractErpHrLeaveRequestProcessor:9,44-45` 注入 `@Inject ErpHrLeaveRequestStateMachine`（非 private）；`assertCanSubmit/Approve/Cancel` 方法 try/catch Bean common 码 → cause-chain 领域码 `ERR_LEAVE_ILLEGAL_STATUS_TRANSITION`（`:92-97`）。`ErpHrLeaveRequestStateMachine`（严格无状态，5-edge 矩阵）。层 1 矩阵测试 `TestErpHrLeaveRequestStateMachineMatrix`（235 行）。
- **common 层非法迁移码**：`ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（M2.11+ 已复用）。
- **合规基线**：`docs/audits/compliance-baseline.md` R5=0、R11=0。
- **owner doc 覆盖**：`docs/design/human-resource/state-machine.md` §适用对象四（ErpHrSalary.approveStatus）+ §4 发放执行独立轴（paymentStatus）+ §6 外部依赖（4 businessType）。**owner doc 已覆盖两轴**（与 finance Reconciliation/BadDebt 缺口不同）——四方对照直接对照 §适用对象四。

## Goals

- 为 ErpHrSalary 的 2 条状态轴各落地一个实体级 `ErpHrSalary*StateMachine` Bean，严格无状态。
  - `ErpHrSalaryPaymentStateMachine`（paymentStatus 单轴，markPaid PENDING→PAID、voidSalary {PENDING}→VOID；PAID 终态）
  - `ErpHrSalaryApprovalStateMachine`（approveStatus 单轴，submit {UNSUBMITTED,null,REJECTED}→SUBMITTED、approve {SUBMITTED}→APPROVED、reject {SUBMITTED}→REJECTED、reverseApprove APPROVED→SUBMITTED、withdrawApproval SUBMITTED→UNSUBMITTED）
- 将固定来源态/目标态判断改调 Bean：payment 轴 MarkPaidProcessor/GenerateBankFileProcessor/voidSalary 内联守卫 → Bean；approve 轴 XScript writer → Bean（经 xbiz inject 调用）。**动态业务守卫与副作用保留原位**（SALARY_PAYMENT 过账、xwf 多级审批链、SoD、银行文件生成）。
- 层 2 四方对照裁定 dict 漂移（`erp-hr/salary-approval-status` legacy 6 值 vs `wf/approve-status` 实际 4 值）。
- 新增层 1 矩阵完备性测试（2 Bean）；层 3 既有集成测试全绿回归。
- 保持全部既有外部行为不变（错误码、过账时序、PAID 锁、xwf 审批链）。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml。
- 不迁移 `posted`（§11.2 M4 (iii)）；过账编排保留在 `SalaryPostingDispatcher` 原位。
- 不修改共享骨架 `Abstract*Processor`（module-common-service 零改动）。
- 不重构 voidSalary 为独立 Processor（R6.7）——本计划在 BizModel 内直接注入 Bean 接线（最小变更原则）。voidSalary 的 Processor 提取归 successor。
- 不改变 dead-code `tryPostAccrual`（SALARY 270/290/300）状态——保留 Deferred R1.26。
- 不引入全局 CRUD 写锁（M0.1 successor）。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）。
- 不证 Delta 覆盖（M4 保护域单项，归 M5.3）。
- 不迁移 SalarySimulation（独立实体）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M0.2 清单 + M1.3 模板 §11 + **M2.11 同域直接范本**；落地 2 Bean + Processor/BizModel/xbiz 接线 + 测试 + 四方对照。**M4 plan-first**——markPaid 触发 SALARY_PAYMENT 过账；approve 触发 Deferred SALARY 计提过账域）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 模板 + §11.2 M4 变体 + §1 双轴命名 + §3 双轴分离）、`docs/design/human-resource/state-machine.md`（§适用对象四 + §4 发放执行独立轴 + §6 外部依赖）、`docs/design/human-resource/payroll.md`、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（:335-336,:456）、`docs/architecture/processor-extension-pattern.md`、`docs/skills/state-machine-business-review-prompt.md`、`docs/plans/2026-08-12-1118-3-erphr-leave-contract-state-machine-beans.md`（M2.11 同域直接范本）
- Skill Selection Basis: 路线图 M4.63/M4.64 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「Processor/BizModel/xbiz 接线、Bean 注册、`@Inject` 非 private、cause-chaining 错误码、过账副作用保留」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成测试回归」。`state-machine-business-review-prompt.md` 匹配层 2 四方对照。M2.11 范本可直接镜像 payment 轴；approve 轴 XScript 接线有 **`ErpAstMovement.xbiz`（M3.15/M3.16 done）直接范本**（`inject('...StateMachine')` + `assertCanXxx` + `*TargetStatus()` 模式已验证），非新模式。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护薪酬过账行为（markPaid 触发 SALARY_PAYMENT(280) active 发放凭证；approve 触发 SALARY(270)+290+300 Deferred 计提凭证 dead-code）。在人工/owner-doc 确认前为阻塞前置。
- 无端口/环境变量/CORS/密钥/.env/外部服务依赖。无数据迁移。

## Execution Plan

### Phase 1 - ErpHrSalary paymentStatus Bean（M4.63）

Status: completed
Targets: `module-hr/erp-hr-service/src/main/java/app/erp/hr/service/statemachine/ErpHrSalaryPaymentStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpHrSalaryMarkPaidProcessor.java`、`.../processor/ErpHrSalaryGenerateBankFileProcessor.java`、`.../processor/AbstractErpHrSalaryProcessor.java`、`.../entity/ErpHrSalaryBizModel.java`、`.../test/.../statemachine/TestErpHrSalaryPaymentStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done（已满足）；M2.11 `ErpHrLeaveRequestStateMachine` 范本已 done

- [x] `Add`：落地 `ErpHrSalaryPaymentStateMachine` Bean——2 动作矩阵（markPaid PENDING→PAID、voidSalary PENDING→VOID）+ `assertCanMarkPaid(String paymentStatus)` + `assertCanVoid(String paymentStatus)` + `markPaidTargetStatus()`/`voidTargetStatus()` + `isTerminal`/`initialStatuses`/`terminalStatuses` + `transitions()`。严格无状态。非法边抛 common 码。镜像 `ErpHrLeaveRequestStateMachine` 结构。
  - Skill: `nop-backend-dev`
- [x] `Add`：在 `_vfs/erp/hr/beans/app-service.beans.xml` 注册（紧邻既有 4 SM Bean L114-121 之后）。
  - Skill: `nop-backend-dev`
- [x] `Decision`（voidSalary 接线方式）：voidSalary 是 inline BizModel 方法（非 Processor）。Decision：(A) **在 BizModel 内直接注入 Bean**——`ErpHrSalaryBizModel` 注入 `@Inject ErpHrSalaryPaymentStateMachine`（非 private），voidSalary `:111-114` 守卫改调 `assertCanVoid`（try/catch common 码 → cause-chain `ERR_SALARY_LOCKED_AFTER_PAID` 领域码，§11.4 终态领域异常重叠模式）；`:115` 目标态改调 `voidTargetStatus()`。**(B) 不提取 voidSalary 为 Processor**（R6.7 successor）——最小变更原则，不重构 BizModel→Processor 架构。
  - Skill: `nop-backend-dev`
- [x] `Add`（接线，镜像 M2.11 范式）：`AbstractErpHrSalaryProcessor` 注入 `@Inject ErpHrSalaryPaymentStateMachine`（非 private，对齐 `AbstractErpHrLeaveRequestProcessor:44-45`）；MarkPaidProcessor `:24,:30` 双轴守卫中的 payment 轴部分改调 `stateMachine.assertCanMarkPaid(paymentStatus)`（try/catch common 码 → cause-chain `ERR_SALARY_ILLEGAL_STATUS_TRANSITION`）；`:38` 目标态改调 `markPaidTargetStatus()`。GenerateBankFileProcessor `:40` 同理。SALARY_PAYMENT 过账 + 银行文件生成 + approve 轴守卫保留原位。
  - Skill: `nop-backend-dev`
- [x] `Proof`：层 1 矩阵完备性 + 层 2 四方对照（dict `erp-hr/salary-payment-status`（3 值）↔ owner-doc §4 ↔ Bean 元数据 ↔ 全部 writer：PayrollCalculator 初始 + MarkPaid + GenerateBankFile + voidSalary + SimulationConvert 初始 + CRUD 路径排除）。
  - Skill: `nop-testing` + `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] `ErpHrSalaryPaymentStateMachine` Bean 存在、已注册、严格无状态；MarkPaid/GenerateBankFile Processor + voidSalary BizModel 委托 Bean。
- [x] Salary payment 层 1 矩阵测试本地 `mvn test -pl module-hr/erp-hr-service -am -Dtest=TestErpHrSalaryPaymentStateMachineMatrix` 全绿（实测 7/7 green）。

### Phase 2 - ErpHrSalary approveStatus Bean（M4.64）

Status: completed
Targets: `.../statemachine/ErpHrSalaryApprovalStateMachine.java`、`.../beans/app-service.beans.xml`、`.../resources/_vfs/erp/hr/model/ErpHrSalary/ErpHrSalary.xbiz`、`.../processor/ErpHrSalaryMarkPaidProcessor.java`、`.../test/.../statemachine/TestErpHrSalaryApprovalStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（payment Bean + abstract→Bean 范式已固化）

- [x] `Decision`（XScript approve 轴接线方式）：approve 轴转换逻辑在 `ErpHrSalary.xbiz` XScript（submit/approve/reject/reverseApprove/withdrawApproval 5 动作），不在 Java。Decision：(A) **xbiz 内 inject Bean + 调用 `assertCanXxx`**——XScript `inject` 指令注入 `ErpHrSalaryApprovalStateMachine`，各动作前置 `stateMachine.assertCanXxx(entity.approveStatus)` 守卫，目标态改调 `*TargetStatus()`（try/catch common NopException → cause-chain 领域码 `ERR_SALARY_ILLEGAL_STATUS_TRANSITION` + salaryId）。**机制替代注记（执行期）**：XLang 引擎不支持 `try/catch` 语句（`TryStatement` 语法节点被 `BuildExecutableProcessor` 拒绝，`nop.err.xlang.exec.not-supported-node`）——「try/catch 映射」下沉到 Java 侧 `ErpHrSalaryApprovalGuard` Bean（契约 §7 接线层职责：调用 Bean `assertCanXxx`，非法边 Bean 抛 common 码，Guard Java try/catch 映射领域码 + salaryId/currentStatus/expectedStatus，common 码作 cause 保留），XScript 仅 inject Guard 调 `assertCanXxx(entity)` + 经 Bean `*TargetStatus()` 写回。行为与错误码契约不变（详见 Guard javadoc）。实际落地：xbiz 5 动作经 Guard 委托 Bean（submit/approve/reject/reverseApprove/withdrawApproval）；非法迁移错误码由原平台 `nop.err.wf.approve.invalid-status` 改为领域 `ERR_SALARY_ILLEGAL_STATUS_TRANSITION`（计划既定改进，守卫/驳回场景域内错误码约定）。(B) MarkPaidProcessor `:24` 的 approve 轴守卫（require APPROVED）改调 approval Bean `assertCanMarkPaid` 的交叉守卫（一致性，非迁移边——markPaid 的迁移边在 payment Bean）。(C) **dict 漂移裁定**：`erp-hr/salary-approval-status`（6 值 legacy）vs `wf/approve-status`（4 值实际）——Bean 按 `wf/approve-status` 4 值编码，legacy dict 登记 doc drift + successor（不从 ORM 删除，PM 要求 6 态审批链时重开）。
  - Skill: `state-machine-business-review-prompt.md`
- [x] `Add`：落地 `ErpHrSalaryApprovalStateMachine` Bean——5 动作矩阵（submit {UNSUBMITTED,null,REJECTED}→SUBMITTED、approve {SUBMITTED}→APPROVED、reject {SUBMITTED}→REJECTED、reverseApprove APPROVED→SUBMITTED、withdrawApproval SUBMITTED→UNSUBMITTED）+ 对应 `assertCanXxx` + `*TargetStatus()` + `isTerminal`/`initialStatuses`/`terminalStatuses` + `transitions()`。严格无状态。镜像 M3.7 `ErpSalOrderApprovalStateMachine`（ErpSalOrder.approveStatus done）审批轴范式。**矩阵裁定依据**：实仓 xbiz 守卫（`ErpHrSalary.xbiz:26` submit 允许 UNSUBMITTED/null/REJECTED、`:62` approve 仅 SUBMITTED、`:87` reject 仅 SUBMITTED、`:113` reverseApprove 仅 APPROVED、`:138` withdrawApproval 仅 SUBMITTED）+ `TestErpHrPayrollEngine:223-226` testIllegalTransitionRejects 证实 UNSUBMITTED 直接 approve 被拒。
  - Skill: `nop-backend-dev`
- [x] `Add`：注册 Bean + 接线 xbiz（经 Decision (A) 裁定的 inject + assertCan 模式）。xwf 多级审批链（hr-review/finance-review/manager-approval）+ SoD + notify 保留原位。
  - Skill: `nop-backend-dev`
- [x] `Proof`：层 1 矩阵完备性 + 层 2 四方对照（dict `wf/approve-status`（4 值）↔ owner-doc §适用对象四 ↔ Bean 元数据 ↔ 全部 writer：xbiz 5 动作 + MarkPaid 交叉守卫 + PayrollCalculator 初始 + SimulationConvert 初始 + CRUD 路径排除）。**dict 漂移 finding**：`erp-hr/salary-approval-status` 6 值未被引用，登记 doc drift + successor。
  - Skill: `nop-testing` + `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] `ErpHrSalaryApprovalStateMachine` Bean 存在/注册/无状态；xbiz 5 动作委托 Bean（经 Guard，运行时实证：`testIllegalTransitionRejects` 断言领域码 + `TestErpHrSalaryWorkflowApproval` 3 用例全绿）。
- [x] Salary approval 层 1 矩阵测试本地 `mvn test -pl module-hr/erp-hr-service -am -Dtest=TestErpHrSalaryApprovalStateMachineMatrix` 全绿（实测 11/11 green）。

### Phase 3 - 层 3 既有命名动作回归

Status: completed
Targets: `module-hr/erp-hr-service/src/test/`（既有集成测试，零新建）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1-2（双轴 Bean + 接线已落地）

- [x] `Proof`：层 3 既有命名动作回归——复用 `TestErpHrSalaryWorkflowApproval`（178 行，approve 轴 xwf e2e：submit→3 级 agree→APPROVED；reject→REJECTED；resubmit）、`TestErpHrPayrollEngine`（`testApprovalStateMachineAndPaidLock:173-204` UNSUBMITTED→SUBMITTED→APPROVED→PAID→voidSalary throws LOCKED；`testIllegalTransitionRejects:207`；`testGenerateBankFileTransfersSalariesToPaid:230`）、`TestErpHrPayrollSimulation`，证明 xwf 审批链、PAID 锁、SALARY_PAYMENT 过账、银行文件生成不变。本地 `mvn test -pl module-hr/erp-hr-service -am` 全绿（实测 232 tests，0 failures/errors，含 TestErpHrPayrollEngine 10 + TestErpHrSalaryWorkflowApproval 3 + TestErpHrPayrollSimulation 12）。
  - Skill: `nop-testing`
- [x] `Proof`：双轴一致性复核——2 Bean 命名（Payment/Approval 后缀）/注册/无状态/元数据形状一致；abstract+BizModel→Bean 注入 + cause-chaining 范式与 M2.11 LeaveRequest 可追溯一致；xbiz inject 模式运行时验证（`testIllegalTransitionRejects` 领域码断言 + Guard 机制替代注记）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 层 3 既有集成测试全绿（零行为回归）。

## Draft Review Record

- Independent draft review iteration 1: `acceptable-as-draft` (`ses_003138cd1ffeaCMkTEcgbGd447`) — 零信任实仓核实 30+ 项 baseline 声明，除 1 处 Csr→Hr 笔误外全 pass。无 BLOCKER / MAJOR。3 MINOR 已修正：(1) `AbstractErpCsrLeaveRequestProcessor` 笔误 → `AbstractErpHrLeaveRequestProcessor`；(2) xbiz inject-StateMachine 模式**非新模式**——`ErpAstMovement.xbiz`（M3.15/M3.16 done）是直接范本，已在 Skill Selection Basis 补注；(3) M3.6（ErpSalQuotation）→ M3.7（ErpSalOrder）M-number 订正。XScript 5 动作行号 + dict 漂移（salary-approval-status 6 值未引用）+ MarkPaid 双轴守卫 + dead-code tryPostAccrual + voidSalary inline + beans.xml 注册全 pass。§11.2 M4 治理 + 规则 14 bundling（同实体双轴）+ anti-slack + Deferred 诚实性均 pass。**Review Hold 确认成立**：触及 SALARY_PAYMENT 过账 + Deferred SALARY 计提保护域。保持 `Plan Status: draft` + Review Hold。
- Independent draft review iteration 2: `acceptable-as-draft`（mission-driver review `2026-08-14-193118`）— 实仓核实审批轴矩阵发现 1 MAJOR 已就地修正：Goals §Goals + Phase 2 `ErpHrSalaryApprovalStateMachine` 矩阵原误写 approve/reject 源态含 UNSUBMITTED，与实仓 xbiz 守卫（`ErpHrSalary.xbiz:62,:87` 仅 SUBMITTED）+ 计划自引测试 `TestErpHrPayrollEngine:223-226`（UNSUBMITTED 直接 approve 被拒）矛盾；若按误写执行将引入 UNSUBMITTED→APPROVED 非法迁移边，违反 Non-Goal「保持全部既有外部行为不变」。已订正为 approve/reject `{SUBMITTED}`、submit `{UNSUBMITTED,null,REJECTED}`，并在 Current Baseline 补登记 5 守卫行号 + 测试证据。payment 轴矩阵（MarkPaid `:24,:30,:38`）核实 pass。格式/退出标准/Closure Gates/Deferred 裁定均合规。**Review Hold 维持**：§11.2 M4 (i) plan-first 人工/owner-doc 门控非审查者可自主解除，计划保持 `draft` + Review Hold。
- Independent draft review iteration 3: `acceptable-as-draft`（mission-driver review `2026-08-13-193118-mission-driver`）— 零信任实仓复核 8 项核心 baseline 声明（MarkPaidProcessor 4 守卫/写入/过账行号、ErpHrSalary.xbiz approve 轴 5 动作 11 子项行号、AbstractErpHrLeaveRequestProcessor 注入范本、AbstractErpHrSalaryProcessor 88 行无 SM 注入、beans.xml 4 SM Bean + 4 Salary Processor + greenfield SM、voidSalary inline + 守卫 + VOID 写入、ErpHrErrors 双错误码、TestErpHrPayrollEngine 双测试方法）**全 8/8 PASS**，含精确行号。格式合规（必需章节齐备、字段名正确、Phase 结构有效、Item Types `Add|Decision|Proof` 合规规则 7）。退出标准清晰可测（指定 `mvn test -Dtest=...` 命令）。范围清晰（规则 14 bundling 同实体双轴，Non-Goals 详尽无 creep）。Closure Gates 定义充分。无 BLOCKER / MAJOR。**Review Hold 确认成立**：触及 SALARY_PAYMENT(280) 过账 + Deferred SALARY 计提保护域，project-context.md §AI 阻塞条件硬停止，门控非审查者可自主解除。计划保持 `draft` + Review Hold，等待人工/owner-doc 门控确认后 promote `active`。
- Independent draft review iteration 4: `acceptable-as-draft`（mission-driver review `2026-08-13-193118-mission-driver` 复审）— 按计划指南 4 项审查清单复核：(1) 格式合规——必需章节齐备（Plan Status/Last Reviewed/Source/Related/Audit/Current Baseline/Goals/Non-Goals/Task Route 三字段/Infrastructure/Execution Plan 3 Phase/Draft Review Record/Closure Gates/Deferred But Adjudicated/Closure），字段名正确，Phase 结构有效（Status/Targets/Skill/Item Types/Prereqs/checkbox/Exit Criteria），Item Types `Add|Decision|Proof` 合规规则 7，每项 Skill 标注合规规则 8；(2) 完整性——Exit Criteria 清晰可测（指定 `mvn test -pl module-hr/erp-hr-service -am -Dtest=...` 精确命令），Execution Plan 覆盖全部检查清单项；(3) 范围——规则 14 bundling（同 owner doc `state-machine.md` + 同实体 `ErpHrSalary` + 同结果表面）裁定正确，Non-Goals 详尽无 creep，Deferred 项全分类带 successor 触发条件（反松弛规则合规）；(4) 闭环证据——Closure Gates 定义 build/test/compliance-checker 可验证证据，Closure 段占位齐备。无 BLOCKER / MAJOR。**Review Hold 维持**：触及 SALARY_PAYMENT(280) 过账 + Deferred SALARY 计提保护域，project-context.md §AI 阻塞条件（会计/财务保护区域无 owner doc 描述预期行为）硬停止，§11.2 M4 (i) plan-first 人工/owner-doc 门控非审查者可自主解除。计划保持 `draft` + Review Hold。
- Independent draft review iteration 5: `acceptable-as-draft`（mission-driver review `2026-08-14-070716-mission-driver`）— 零信任实仓复核 baseline 全部核心声明 PASS：(1) ErpHrConstants :30-33 approve 4 态 / :36-38 payment 3 态；(2) ErpHrErrors :92-95/:96-99 双错误码；(3) MarkPaidProcessor :24 approve 守卫 / :30 payment 守卫 / :36 tryPostPayment / :38 PAID 写入；(4) GenerateBankFileProcessor :40 PAID 写入（守卫经 Abstract `findPayableSalaries:75-83` APPROVED+PENDING 查询级实现，与计划「守卫同 markPaid」一致）；(5) ErpHrSalaryBizModel voidSalary :109-118 inline + 守卫 :111-114 + 写 VOID :115 + javadoc :47-50 审批轴归平台；(6) ErpHrSalary.xbiz 5 动作守卫/写入行号全数核实（submit :26/:35、approve :62/:71、reject :87/:96、reverseApprove :113/:122、withdrawApproval :138/:147）——approve/reject 仅 SUBMITTED、submit 允许 UNSUBMITTED/null/REJECTED，矩阵与实仓一致；(7) AbstractErpHrSalaryProcessor 88 行无 SM 注入、AbstractErpHrLeaveRequestProcessor :44-45 注入范本；(8) beans.xml 3 Posting L23-28 + 4 Salary Processor L49-56 + 4 Simulation Processor L57-64 + 4 SM Bean L114-121 + Salary SM greenfield；(9) ORM :72-76 payment dict 3 值 / :77-84 approval-status 6 值 legacy drift（全仓仅 i18n yaml + javadoc 引用，无列引用证实「未被引用」）/ :742 approveStatus wf/approve-status / :764 posted；(10) SalaryPostingDispatcher tryPostPayment 280 active + tryPostAccrual 270/290/300 zero-callers Deferred R1.26；(11) 测试（TestErpHrSalaryWorkflowApproval 178 行；TestErpHrPayrollEngine :173-204/:207/:223-226/:230；PayrollCalculator :149-150 初始态；SimulationConvert :79-80 初始态）；(12) ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION；(13) compliance baseline R5=0/R11=0；(14) owner doc §适用对象四（:217）+ §4 发放执行独立轴（:241）；(15) roadmap M4.63/M4.64 `todo` + plan-first；(16) M3.7 ErpSalOrderApprovalStateMachine + ErpAstMovement.xbiz（M3.15/16）先例实体存在；(17) 矩阵测试目录 `service/statemachine/` 与 Phase Targets 一致。格式/完备/范围/闭环证据四维全 PASS，无 BLOCKER / MAJOR / MINOR。**Review Hold 确认成立且不可自解**：§11.2 M4 (i) 实仓核实于 `entity-state-machine-bean.md:283`（「触及受保护行为（过账/红冲/结账）时不因 StateMachine Bean 抽象而免除人工/owner-doc 门控」）+ `ai-autonomy-policy.md:72`（`accounting/finance postings | plan-first | owner doc + tests`）+ `project-context.md:68`（会计/财务保护区域硬停止）；roadmap M4.63/M4.64 未纳入任何已确认人工门控批次。本计划 markPaid 触发 SALARY_PAYMENT(280) active 发放凭证、approve 触发 Deferred SALARY(270)/290/300 计提（config 翻转后即触发受保护行为），属「missing upstream decision」类外部依赖阻塞，审查者不可自主解除（batch-consistent：0456-1/0456-3 同 hold）。按 fix-forward 逃生舱保持 `Plan Status: draft` + Review Hold（front matter line 4），门控确认记录追加后方可转 `active`；approved 标记仅报告「审查已运行」。
- **M4 plan-first 人工/owner-doc 门控状态：confirmed（2026-08-14 人工确认解除）**（§11.2 M4 (i)）。人工/owner 于 2026-08-14 确认「以行为保持的矩阵集中化方式迁移 salary 审批/发放双轴、markPaid SALARY_PAYMENT 发放凭证 + approve 计提凭证（含 Deferred 270/290/300 config 翻转后路径）完整保留」可接受，门控解除。据此将 Plan Status 由 `draft` 转 `active`。

## Closure Gates

- [x] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)）
- [x] 范围内行为完成（双轴 Bean + Processor/BizModel/xbiz 接线 + 层 1 矩阵 + 层 2 四方对照 + 层 3 回归）
- [x] 相关文档对齐（roadmap M4.63/M4.64 → done；dict 漂移登记）
- [x] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl module-hr/erp-hr-service -am` 全绿（232 tests）+ `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### voidSalary 未提取为 Processor（R6.7 gap）

- Classification: `watch-only residual (intentional legacy)`
- Why Not Blocking Closure: voidSalary 是 inline BizModel 方法，本计划在 BizModel 内直接注入 Bean 接线（最小变更），不提取为 Processor。R6.7 per-mutation 拆分惯例下 voidSalary 缺 Processor 是既有 gap。
- Successor Required: yes（触发条件 = R6.7 voidSalary Processor 提取时同步迁移 Bean 接线点）

### dict `erp-hr/salary-approval-status` legacy drift（6 值未引用）

- Classification: `documentation drift (successor)`
- Why Not Blocking Closure: ORM 列引用 `wf/approve-status`（4 值），但 legacy dict `erp-hr/salary-approval-status`（6 值）存在未被引用。四方对照 finding 登记。不从 ORM 删除 dict（保留预留语义入口，PM 要求 6 态审批链时重开）。
- Successor Required: yes（触发条件 = PM 要求薪酬 6 态多级审批链落地时）

### SALARY(270)+SOCIAL_INSURANCE_ER(290)+HOUSING_FUND_ER(300) 计提过账 dead-code

- Classification: `watch-only residual (Deferred R1.26)`
- Why Not Blocking Closure: `SalaryPostingDispatcher.tryPostAccrual` zero callers（dead-code），Deferred R1.26。计提过账未接线不阻塞 Bean 迁移（Bean 只管 approveStatus 迁移边，不管过账编排）。
- Successor Required: yes（触发条件 = R1.26 SALARY 计提过账接线时）

### Delta 覆盖运行时实证

- Classification: `optimization candidate`
- Why Not Blocking Closure: M4 保护域单项 Delta 可选；归 M5.3。
- Successor Required: yes（触发条件 = M5.3 最终跨域 Delta 覆盖回归）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 裁定选项 (c) 显式排除。
- Successor Required: no

## Closure

Status Note: 全 3 Phase 执行完成 + 独立结束审计通过（第二轮审计 PASS）。双轴 Bean 落地：`ErpHrSalaryPaymentStateMachine`（markPaid/voidSalary 2 边）+ `ErpHrSalaryApprovalStateMachine`（5 动作 6 边 + markPaid 交叉守卫），接线覆盖 MarkPaid/GenerateBankFile Processor + voidSalary BizModel + xbiz 5 动作（经 `ErpHrSalaryApprovalGuard`，机制替代注记见 Phase 2 Decision）。层 1 矩阵 18 测试（7+11）+ 层 2 四方对照（payment 轴：dict `erp-hr/salary-payment-status` 3 值 ↔ owner-doc §适用对象四 §4 ↔ Bean 元数据 ↔ 5 writer 全对齐，无死状态；approval 轴：dict `wf/approve-status` 4 值 ↔ owner-doc §适用对象四 ↔ Bean 元数据 ↔ 8 writer 全对齐；dict 漂移 `erp-hr/salary-approval-status` 6 值 legacy 未引用已登记 Deferred）+ 层 3 回归（232 tests 全绿，xwf 审批链/PAID 锁/SALARY_PAYMENT 过账/银行文件生成零回归）。验证：`mvn clean install -DskipTests` 全仓 BUILD SUCCESS + `mvn test -pl module-hr/erp-hr-service -am` 232/232 green + compliance checker 全 19 规则 actual ≤ baseline（R5=0/R11=0/R2c=1392/R12c=40）。owner doc `human-resource/state-machine.md §适用对象四` 已覆盖双轴（无需补正）；dict 漂移 + voidSalary Processor 提取归 Deferred 段既定 successor。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话，不重用执行者上下文）——task `ses_0016e1d66ffeomTWt3Ccb8i00R`（第一轮，FAIL：文档完成步骤缺失，代码/验证全 PASS）+ task `ses_0015f7431ffe6Vm1phqEQAYj5J`（第二轮，FAIL：Closure 证据占位符未填，其余全 PASS）+ task `ses_00155532effe6WUrdseXy4dgHK`（第三轮，PASS）
- Evidence: 审计逐项核实（1）计划文档完成标记（Phase Status/items/exit criteria/Closure Gates/Closure 段/Plan Status）；（2）代码工件与实仓一致（双 Bean 无状态/beans.xml 注册/MarkPaid+GenerateBankFile+voidSalary 接线/xbiz 5 动作 inject Guard+Bean/Guard 注册）；（3）R5 @Inject 非 private、零 ORM/API/dict 修改（git status 仅 erp-hr-service 模块）；（4）roadmap M4.63/M4.64 ready→done + 最后更新 header；（5）验证复跑：`mvn test -pl module-hr/erp-hr-service -am` 232 tests 0 failures + compliance checker actual ≤ baseline。

Follow-up:

- 无新增跟进项；Deferred 项均为既定 successor（voidSalary Processor 提取 R6.7、salary-approval-status 6 态重开 PM 要求、SALARY 计提过账 R1.26、Delta 覆盖 M5.3、全局 CRUD 写锁 M0.1 §9）。
