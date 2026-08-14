# 2026-08-13-1146-3-erpfin-employee-advance-state-machine-beans 员工借款 ErpFinEmployeeAdvance.docStatus + approveStatus 实体级状态机 Bean（M4.6 + M4.7）

> Plan Status: completed
> Review Hold: §11.2 M4 (i) 人工/owner-doc 门控**已于 2026-08-14 经人工确认解除**（见 Draft Review Record 门控确认记录）——本计划触及受保护借款过账行为（approve 触发 `EmployeeAdvancePostingDispatcher.tryPost`→`FinPostingExecutor` 生成 EMPLOYEE_ADVANCE 凭证 + `ErpFinArApItem`（RECEIVABLE，1221 应收-员工预支），已由起草者经 live code 实证：`ErpFinEmployeeAdvanceProcessor:165` tryPost + `:171` markPosted + `EmployeeAdvanceAcctDocProvider` implements `IErpFinAcctDocProvider` + `ErpFinArApItemGenerator:168-170` EMPLOYEE_ADVANCE→RECEIVABLE）。M4 plan-first 门控成立且经人工确认；已转 `active` 进入实施。
> Last Reviewed: 2026-08-14
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.6（ErpFinEmployeeAdvance.docStatus）+ M4.7（ErpFinEmployeeAdvance.approveStatus），plan-first；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 finance M4.6/7`（FIN-13/14 纳入，预付款凭证+SoD）
> Related: M4 plan-first 先例 `2026-08-13-2045-3-erpfin-voucher-state-machine-bean.md`（M4.1）+ `2026-08-13-2045-1-erpfin-period-state-machine-bean.md`（M4.2）；M0.1 契约 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（done）+ M1.3 模板 `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（done）；双轴先例 `2026-08-13-0805-2-erpast-movement-state-machine-beans.md`（M3.15+M3.16）+ `2026-08-13-0945-1-purchase-approvestatus-state-machine-bean.md`（approve 轴 + reverseApprove→REJECTED + SoD）；姊妹计划 `2026-08-13-1146-2-erpfin-expense-claim-state-machine-beans.md`（M4.4+M4.5，近同构 sibling，N=2 先执行以建立报销→借款冲抵引用方）
> Mission: entity-state-machine
> Work Item: M4.6 + M4.7
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。员工借款 approve 触发业财过账（凭证 + ArApItem RECEIVABLE），reverseApprove/cancel 触发红冲；另有 cashRepay/reverseCashRepay（资金动作，非状态机轴，不在本计划范围）。声明 §11.2 M4 硬约束：(i) plan-first + 受保护行为人工/owner-doc 门控；(ii) 过账时序/编排/失败兜底不改，继续由 `EmployeeAdvancePostingDispatcher`→`FinPostingExecutor` + `posted` 标志契约管理；(iii) `posted` 不入轴（§3；实体有 `posted` boolean，排除-posted）；(iv) 跨域副作用（ArApItem 生成、被报销单冲抵引用、红冲）保留原 Processor 路径；(v) SoD（approver-is-creator）作为动态业务守卫保留原位。本计划是 plan-first 产物，人工/owner-doc 确认门控未满足前保持 `draft`。
>
> **规则 14 合并声明（M4.6 + M4.7 同计划）**：docStatus 与 approveStatus 为同一实体 `ErpFinEmployeeAdvance` 的两条独立状态轴，同一 owner-doc 义务、同一结果表面（借款单双轴状态机）、同一验证路径。按 §3 双轴分离，落地**两个独立 Bean**（`ErpFinEmployeeAdvanceApprovalStateMachine` + `ErpFinEmployeeAdvanceDocumentStateMachine`），合为单计划。
>
> **与姊妹计划 M4.4/M4.5（ExpenseClaim）的关系**：两者近同构（同 facade + per-mutation Processor 模式、同 4 态 approve 轴、同 DRAFT/CANCELLED doc 轴、同 SoD、同常量源 `ErpFinDocStatus`）。差异：(1) ExpenseClaim approve 后调 `AdvanceOffsetOrchestrator` 冲抵员工借款，EmployeeAdvance **无冲抵编排调用**（冲抵由 ExpenseClaim 侧驱动）；(2) EmployeeAdvance 抽取 `markPosted`/`clearPosted` helper，ExpenseClaim 内联；(3) ArApItem 方向相反（EmployeeAdvance = RECEIVABLE 1221，ExpenseClaim = PAYABLE 2241）；(4) EmployeeAdvance BizModel 有 cashRepay/reverseCashRepay（资金动作）。两计划各自独立迁移、独立结束，仅经「冲抵引用方」语义相互引用（本计划是被冲抵方）。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 finance M4.6/7` + 实仓核实。

- **实体**：`ErpFinEmployeeAdvance`（`module-finance/model/app-erp-finance.orm.xml:1376`，`useLogicalDelete="true"` `deleteFlagProp="delVersion"` `createrProp="createdBy"`）。双轴：
  - `docStatus` propId=14 `:1393` `ext:dict="erp-fin/advance-status"`。dict 定义于 `module-finance/erp-fin-meta/src/main/resources/_vfs/dict/erp-fin/advance-status.dict.yaml`（5 值：`DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED`）。**注意**：代码仅写 `DRAFT`（seed）与 `CANCELLED`（cancel），SUBMITTED/APPROVED/REJECTED 为**残余值**（生命周期推进由 approveStatus 承载，同 ExpenseClaim）。
  - `approveStatus` propId=15 `:1394` `ext:dict="wf/approve-status"`（**平台共享 dict**，4 值：`UNSUBMITTED/SUBMITTED/APPROVED/REJECTED`）。
  - `posted` boolean propId=19 `:1398`（排除-posted）；`approvedBy`(`:1395`)/`approvedAt`(`:1396`)/`postedBy`(`:1397`)/`postedAt`(`:1399`)；`createdBy` propId=24 `:1403`（SoD 源）。
- **常量**：同 ExpenseClaim——`ErpFinConstants extends ErpFinDocStatus`（`ErpFinConstants.java:12`），状态常量在 dao 层 `ErpFinDocStatus.java`：`APPROVE_STATUS_*`（`:14-17`）、`DOC_STATUS_DRAFT/CANCELLED`（`:20-21`）。无 EmployeeAdvance 专属状态常量。
- **approveStatus 现状 writer（5 命名动作，全部 facade `do*` 写，实仓核实）**：
  - `submitForApproval`（xbiz mutation）：`doSubmit`（`ErpFinEmployeeAdvanceProcessor:154`）approveStatus `UNSUBMITTED/REJECTED→SUBMITTED`，守卫 `validateTransitionForSubmit`（`:74`）。
  - `withdrawApproval`（xbiz）：`doWithdrawSubmit`（`:159`）`SUBMITTED→UNSUBMITTED`，守卫 `validateTransitionForWithdraw`（`:82`）。
  - `approve`（xbiz，auth `ErpFinEmployeeAdvance:approve`）：`doApprove`（`:167`）`SUBMITTED→APPROVED`，守卫 `validateTransitionForApprove`（`:89`）；含 **SoD**（`:164` `SoDGuard.assertApproverNotCreator`）+ tryPost（`:165`）+ markPosted（`:171`）。
  - `reject`（xbiz）：`doReject`（`:178`）`SUBMITTED→REJECTED`，守卫 `validateTransitionForReject`（`:96`）。
  - `reverseApprove`（xbiz，auth `ErpFinEmployeeAdvance:reverseApprove`）：`doReverseApprove`（`:188`）`APPROVED→REJECTED`，守卫 `validateTransitionForReverseApprove`（`:103`）；含 reverse posting（`:184`）+ clearPosted（`:186`）+ null approvedBy/At（`:189-190`）。
- **docStatus 现状 writer（1 命名动作）**：
  - `cancel`（Java `@BizMutation` `ErpFinEmployeeAdvanceBizModel.cancel:59`→`ErpFinEmployeeAdvanceCancelProcessor`→facade）：`doCancel`（`:203`）`*→CANCELLED`，守卫 `validateTransitionForCancel`（`:110` 检查 `isCancelled()`）；含 reverse posting if APPROVED+posted（`:199`）+ clearPosted（`:201`）。
- **per-mutation Processors（6 个）**：SubmitForApproval/Approve/Reject/ReverseApprove/WithdrawApproval/Cancel，均委托 facade `do*`，其 `setApproveStatus`/`setDocStatus` override 为 abstract base 桥接（非独立 writer）。xbiz 5 mutation + BizModel 1 Java mutation。**净 live writer = 6，全在 `ErpFinEmployeeAdvanceProcessor.do*`**。
- **reverseApprove 目标 = REJECTED**（`:188`，已合规，同 ExpenseClaim/purchase）。
- **错误码**：`ERR_EMPLOYEE_ADVANCE_ILLEGAL_STATUS_TRANSITION`（`ErpFinErrors:188`，参数 `ARG_ADVANCE_CODE`/`ARG_CURRENT_STATUS`/`ARG_EXPECTED_STATUS`）；`ERR_EMPLOYEE_ADVANCE_ILLEGAL_DOC_STATUS_TRANSITION`（`:192`，参数 `ARG_ADVANCE_CODE`/`ARG_CURRENT_DOC_STATUS`/`ARG_EXPECTED_DOC_STATUS`）。SoD 码 `ERR_FIN_APPROVER_IS_CREATOR`（`:482`，共享）。**潜在漂移**：`ERR_EMPLOYEE_ADVANCE_NOT_REVERSED_BEFORE_CANCEL`（`:206`）已定义但 `doCancel`（`:195-206`）**不抛此码**——cancel 自动红冲已过账凭证而非要求先反审核。Phase 3 裁定（疑似 dead error code 或 intentional auto-reverse 行为）。
- **过账副作用**：`EmployeeAdvancePostingDispatcher.tryPost`（buildEvent EMPLOYEE_ADVANCE → `FinPostingExecutor.postEvent`）；`EmployeeAdvanceAcctDocProvider`（implements `IErpFinAcctDocProvider`，Dr 1221 其他应收款-员工预支 / Cr 1002 银行存款，bean `app-service.beans.xml:96-97`）；ArApItem（`ErpFinArApItemGenerator:168-170` EMPLOYEE_ADVANCE→RECEIVABLE，partnerId=employee.partnerId）。**无 AdvanceOffsetOrchestrator 调用**（冲抵由 ExpenseClaim 侧 `AdvanceOffsetOrchestrator.offset` 按 advance code 驱动，本实体为被冲抵方）。`markPosted`（`:227-231`）/`clearPosted`（`:233-237`）helper。SoD = 动态守卫（非 Bean）。
- **BizModel 非状态机动作**：`ErpFinEmployeeAdvanceBizModel` 另有 `cashRepay`（`:65` `@BizMutation`，更新 settledAmount/outstandingAmount + postCashRepay）+ `reverseCashRepay`（`:120`）——资金动作，**非状态轴**，不在本计划范围（Non-Goal）。
- **生产 Bean 注册**：`module-finance/erp-fin-service/src/main/resources/_vfs/erp/fin/beans/app-service.beans.xml`（374 行）：facade `ErpFinEmployeeAdvanceProcessor`（`:147-148`）+ 6 per-mutation Processor（`:244-255`）+ dispatcher/provider。StateMachine Bean 追加于文件末尾。finance 当前无 `statemachine/` 包（与 M4.4/M4.5/M4.8/M4.9/M4.1/M4.2 并行创建，互不依赖）。
- **既有测试（layer-3 回归基线，非 greenfield）**：
  - `TestErpFinEmployeeAdvanceApproval`（`module-finance/erp-fin-service/src/test/.../entity/`，182 行，7 `@Test`）：`testSubmitApproveReverse`（UNSUBMITTED→SUBMITTED→APPROVED→reverseApprove→REJECTED）、`testRejectAndResubmit`、`testCancel`（docStatus→CANCELLED）、`testIllegalApproveFromUnsubmitted`、`testRejectEmployeePartnerMissing`、`testRejectEmployeeInactive`、`testRejectAmountNotPositive`。经 `IGraphQLEngine` RPC + BizModel cancel。
  - 过账集成：`TestErpFinEmployeeAdvancePosting`（approve→EMPLOYEE_ADVANCE 凭证 + ArApItem RECEIVABLE `:71-75` + reverseApprove→红冲）、`TestErpFinEmployeeAdvanceCashRepay`（cashRepay）、`TestErpFinEmployeeAdvanceCashRepayReversal`（reverseCashRepay）。
  - **M0.2 §3.5 finance M4.6/7 标「测试：无」与实仓漂移**——实有上述 layer-3 测试。layer-1 矩阵测试为 greenfield（新增）。Phase 3 登记此漂移。
- **合规基线**：`@Inject private` 须保持 R5=0（fin-service grep 证实当前满足）。本计划保持 R5=0、R11 不增。
- **owner doc 覆盖**：`docs/design/finance/state-machine.md` §适用对象（`:7-10`）仅列 Voucher + AccountingPeriod。EmployeeAdvance 仅在 §职责分离（`:264`）被提及，**无状态机章节**。业务设计在 `docs/design/finance/expense-claim.md`（`:81` ErpFinEmployeeAdvance）。**owner-doc 缺口**：需在 state-machine.md 补 §对象六（EmployeeAdvance 双轴）。

## Goals

- 落地两个无状态 Bean（§2 无状态约束，§3 双轴分离）：
  - `ErpFinEmployeeAdvanceApprovalStateMachine`（approveStatus 轴，`Approval` 后缀）：矩阵 `submitForApproval` {UNSUBMITTED,REJECTED}→SUBMITTED；`withdrawApproval` {SUBMITTED}→UNSUBMITTED；`approve` {SUBMITTED}→APPROVED；`reject` {SUBMITTED}→REJECTED；`reverseApprove` {APPROVED}→REJECTED。initial=`{UNSUBMITTED}`，terminal=`{APPROVED,REJECTED}`。
  - `ErpFinEmployeeAdvanceDocumentStateMachine`（docStatus 轴，`Document` 后缀）：矩阵 `cancel` {非 CANCELLED}→CANCELLED。initial=`{DRAFT}`，terminal=`{CANCELLED}`。**docStatus dict 残余值 SUBMITTED/APPROVED/REJECTED 不纳入任一集合**（同 ExpenseClaim）。
  - 两 Bean 均可经 Delta 同名覆盖。
- 将 6 个 `do*` 的固定 `validateTransitionFor*` 内联守卫改调对应 Bean `assertCan<Action>(from)` + 目标态回写。**动态业务守卫与副作用保留原位**：SoD、过账（tryPost/reverse）、posted/approvedBy/approvedAt 写入（markPosted/clearPosted helper）、业务规则校验（employee-inactive/partner-missing/amount-invalid）、乐观锁。
- 保持全部既有外部行为不变（错误码 + 参数、5 approve 边 + 1 doc 边、reverseApprove→REJECTED、approve 过账时序、cancel 自动红冲 if posted、SoD 拒绝、cashRepay/reverseCashRepay 不受影响）。
- 新增 layer-1 矩阵完备性表驱动测试（两 Bean 各覆盖合法/非法边 + initial/terminal + docStatus 残余值排除）；layer-3 既有集成测试回归全绿。
- layer-2 四方对照：确认双轴矩阵 + docStatus 残余值漂移 + `NOT_REVERSED_BEFORE_CANCEL` 死码裁定 + M0.2 测试名漂移登记。

## Non-Goals

- 不迁移 `posted`（boolean，§3 不入轴）、不迁移 `approvedBy`/`approvedAt`/`postedBy`/`postedAt` 为状态轴。
- 不迁移 `cashRepay`/`reverseCashRepay`（资金动作，更新 settledAmount/outstandingAmount + EMPLOYEE_ADVANCE_SETTLE 过账，非 status 轴范畴）。
- 不改变过账编排（`EmployeeAdvancePostingDispatcher` 时序、失败兜底）、不改变 SoD 语义（`SoDGuard` 保留原位）、不改变 reverseApprove→REJECTED 目标（已合规）、不改变 cancel 自动红冲行为。
- 不修改 `model/*.orm.xml`、字典值或 API 契约（advance-status dict 5 值保留，残余值不删）。
- 不迁移 `ErpFinExpenseClaim`（M4.4+M4.5 姊妹计划）、`ErpFinVoucher`（M4.1）或 finance 其余轴。
- 不引入通用 CRUD 对 docStatus/approveStatus 写入的运行时禁止（M0.1 successor）。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）：门控未确认前计划保持 `draft`。
- 不证 Delta 覆盖（M4 保护域单项，归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M1.3 模板 + M0.2 清单；落地双轴两 Bean + 接线 + 三层测试 + 四方对照；不改契约/模型/公共 API；**M4 plan-first**——借款过账，finance 保护域）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 迁移模板 + §11.2 M4 变体 + §3 posted 不入轴 + §双轴分离）、`docs/design/finance/state-machine.md`（**§适用对象 缺口需补 §对象六**）、`docs/design/finance/expense-claim.md`（`:81` EmployeeAdvance 业务设计 §源）、`docs/design/finance/posting.md`（业财打通）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M0.2 清单 finance M4.6/7）、`docs/plans/2026-08-13-0805-2-erpast-movement-state-machine-beans.md`（双轴先例）、`docs/plans/2026-08-13-0945-1-purchase-approvestatus-state-machine-bean.md`（approve 轴 + reverseApprove→REJECTED + SoD 先例）、`docs/plans/2026-08-13-1146-2-erpfin-expense-claim-state-machine-beans.md`（姊妹计划，冲抵引用方）
- Skill Selection Basis: 路线图 M4.6/M4.7 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「facade do* 接线、双轴分离 Bean、approve 轴 5 边 + doc 轴 cancel、SoD/过账副作用保留、markPosted/clearPosted helper 保留、错误码映射、`@Inject` 非 private、过账吞异常自检」；`nop-testing` 匹配「双轴矩阵表驱动测试 + 既有 Approval/Posting/CashRepay 集成测试回归」。层 2 引用 `state-machine-business-review-prompt.md`。必需输入均已就绪。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护借款过账行为。在人工/owner-doc 确认「以行为保持的双轴矩阵集中化方式迁移、过账/SoD 完整保留、posted 不入轴、cashRepay 非状态轴不触碰、docStatus 残余值不纳入 Bean」可接受前，计划保持 `draft`，不得进入实施。门控记录须写入本计划 Draft Review Record。
- SoD config `erp-common.sod-enabled`（默认 true，`%test` profile 关闭）保留不动。
- 无新增端口/环境变量/CORS/密钥/.env/外部服务依赖。无数据迁移。

## Execution Plan

### Phase 1 - 双轴 Bean + 注册 + layer-1 矩阵测试

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/statemachine/ErpFinEmployeeAdvanceApprovalStateMachine.java`、`.../ErpFinEmployeeAdvanceDocumentStateMachine.java`（新建）、`app-service.beans.xml`（注册）、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/statemachine/TestErpFinEmployeeAdvanceApprovalStateMachineMatrix.java`、`.../TestErpFinEmployeeAdvanceDocumentStateMachineMatrix.java`（新建）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（layer-1 表驱动测试）

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done

- [x] 新建无状态 `ErpFinEmployeeAdvanceApprovalStateMachine`：矩阵 `assertCanSubmit({UNSUBMITTED,REJECTED})`→`submitTargetStatus()=SUBMITTED`；`assertCanWithdraw(SUBMITTED)`→UNSUBMITTED；`assertCanApprove(SUBMITTED)`→APPROVED；`assertCanReject(SUBMITTED)`→REJECTED；`assertCanReverseApprove(APPROVED)`→REJECTED。`initialStatuses()={UNSUBMITTED}`、`terminalStatuses()={APPROVED,REJECTED}`、`isTerminal`。`transitions()` 编码 5 命名边。`normalize(null)`→UNSUBMITTED。非法来源态抛 common 码携带 `action`/`fromStatus`。grep 证实无 DAO/IBiz/事务 import。**形状与姊妹 `ErpFinExpenseClaimApprovalStateMachine` 一致**（同 4 态 approve 轴），但为独立 Bean（§3 每实体每轴一 Bean，不跨实体复用）。
  - Skill: `nop-backend-dev`
- [x] 新建无状态 `ErpFinEmployeeAdvanceDocumentStateMachine`：矩阵 `assertCanCancel({非 CANCELLED})`→CANCELLED（校验 `!isCancelled(from)`）。`initialStatuses()={DRAFT}`、`terminalStatuses()={CANCELLED}`。**docStatus dict 残余值 SUBMITTED/APPROVED/REJECTED 不纳入任一集合**（javadoc 标注 intentional reserved，同 ExpenseClaim）。`transitions()` 编码 1 命名边（cancel）。非法来源态（CANCELLED）抛 common 码。
  - Skill: `nop-backend-dev`
- [x] Decision（前置）：记录 (a) docStatus 残余值分类——`erp-fin/advance-status` dict 5 值但代码仅写 DRAFT（seed）/CANCELLED（cancel），SUBMITTED/APPROVED/REJECTED 为残余值（intentional legacy dict，workflow 轴 = approveStatus，同 ExpenseClaim）；Bean DocumentStateMachine 仅建模 DRAFT→CANCELLED，残余值不纳入任一集合，dict 项保留不删；(b) `ERR_EMPLOYEE_ADVANCE_NOT_REVERSED_BEFORE_CANCEL`（`ErpFinErrors.java:206`）经执行期 grep 全 module-finance 证实**仅定义零 throw 引用**——`doCancel`（`:195-206`）自动红冲已过账凭证（intentional auto-reverse）不抛此码，疑似 dead/vestigial——裁定交 Phase 3（layer-2 已独立复核该码零 throw，见 Phase 3）。
  - Skill: `state-machine-business-review-prompt.md`
- [x] 在 `app-service.beans.xml` 以 FQN id 注册两 Bean。
  - Skill: `nop-backend-dev`
- [x] Proof（layer-1 矩阵，表驱动，两 Bean 各一份）：Approval 覆盖 submit（UNSUBMITTED & REJECTED 合法、SUBMITTED/APPROVED 非法）/withdraw（SUBMITTED 合法）/approve（SUBMITTED 合法、其余非法）/reject（SUBMITTED 合法）/reverseApprove（APPROVED 合法、其余非法）+ 终态无出边 + transitions(5) + initial/terminal。Document 覆盖 cancel（DRAFT 合法、CANCELLED 非法）+ **断言 SUBMITTED/APPROVED/REJECTED 不在任一集合**（残余值排除）+ transitions(1)。不经 Processor 入口。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 两 Bean 无状态、矩阵完整；docStatus 残余值排除；残余值 + 死码 Decision 记录在案
- [x] 两 Bean 已在 `app-service.beans.xml` 注册（FQN id）；`@Inject` 字段非 private（合规 R5）
- [x] layer-1 矩阵测试通过；本地化编译 `mvn compile -pl module-finance/erp-fin-service -am` 通过（解除 Phase 2 接线依赖）

### Phase 2 - Processor 接线（行为保持，SoD/过账副作用保留）+ layer-3 回归

Status: completed
Targets: `ErpFinEmployeeAdvanceProcessor`（6 个 do* 守卫委托）
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1 Bean 落地

- [x] `ErpFinEmployeeAdvanceProcessor` 注入两 Bean（`@Inject` 非 private），将 6 个 `validateTransitionFor*` 内联固定 `Objects.equals` 判断替换为对应 Bean `assertCan<Action>(from)` + 目标态写回。5 approve 守卫（`validateTransitionForSubmit:88`/`Withdraw:97`/`Approve:106`/`Reject:115`/`ReverseApprove:124`）委托 ApprovalStateMachine；1 doc 守卫（`validateTransitionForCancel:139`）委托 DocumentStateMachine。common→领域码映射（Approval→`ERR_EMPLOYEE_ADVANCE_ILLEGAL_STATUS_TRANSITION`，Document→`ERR_EMPLOYEE_ADVANCE_ILLEGAL_DOC_STATUS_TRANSITION`，common 作 cause）+ 参数对外不变。**完整保留**：SoD（doApprove `SoDGuard`）、tryPost/reverse、markPosted（helper）/clearPosted（helper）、approvedBy/approvedAt 写入、业务规则校验（employee-inactive/partner-missing/amount-invalid）、reverseApprove→REJECTED、cancel 自动红冲 if APPROVED+posted、乐观锁。**cashRepay/reverseCashRepay（BizModel）不触碰**。
  - Skill: `nop-backend-dev`
- [x] Proof（layer-3 回归）：`mvn test -pl module-finance/erp-fin-service -am` 全绿（**454 tests 0 failures/0 errors**）——重点 `TestErpFinEmployeeAdvanceApproval`（7 @Test：submit→approve→reverse + reject/resubmit + cancel + 非法 approve-from-unsubmitted + employee-inactive/partner-missing/amount 守卫）、`TestErpFinEmployeeAdvancePosting`（2 @Test：approve→EMPLOYEE_ADVANCE 凭证 + ArApItem RECEIVABLE + reverseApprove→红冲不变）、`TestErpFinEmployeeAdvanceCashRepay`（6 @Test）/`...CashRepayReversal`（3 @Test）（cashRepay 不受影响）。证明 5 approve 边 + 1 doc 边 + 过账/SoD/cashRepay 行为不变。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 6 守卫改调 Bean 委托，grep 证实相关方法体内不再有内联固定状态矩阵判断（动态副作用 SoD/tryPost/markPosted/clearPosted/业务规则除外）；cashRepay/reverseCashRepay 方法体未触碰
- [x] 领域错误码 + 参数对外不变（layer-3 断言证实）；5 approve 边 + 1 doc 边 + reverseApprove→REJECTED + 过账/SoD + cancel 自动红冲行为不变
- [x] layer-3 `mvn test -pl module-finance/erp-fin-service -am` 全绿（454 测试：Approval 7 + Posting 2 + CashRepay 6 + CashRepayReversal 3 + 层 1 矩阵 19 + 其余回归）

### Phase 3 - layer-2 四方对照 + owner doc 补章节 + 漂移 Decision

Status: completed
Targets: 四方对照审计记录（写入本计划 Closure 段）；`docs/design/finance/state-machine.md`（新增 §对象六 EmployeeAdvance 双轴）；本计划 Closure
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度）

- Item Types: `Proof | Decision | Add`
- Prereqs: Phase 2 接线完成

- [x] Proof（layer-2 四方对照，§11.1 步骤 5，10 维度，双轴各一份）：dict（advance-status 5 值 + wf/approve-status 4 值）↔ owner doc（新增 §对象六 + expense-claim.md §源）↔ Bean 元数据 ↔ writer。执行期独立 grep 重核：(a) **6 命名动作 writer 全集 CONFIRMED**——净 live writer = 6 全在 facade `do*`（`ErpFinEmployeeAdvanceProcessor` doSubmit:185/doWithdrawSubmit:190/doApprove:198/doReject:209/doReverseApprove:219/doCancel:234），per-mutation `setApproveStatus`/`setDocStatus` override（5 + 1 处）为 abstract base 桥接（非独立 writer，排除），BizModel 零状态写入（grep `ErpFinEmployeeAdvanceBizModel` 零命中），cashRepay/reverseCashRepay 为资金动作（settledAmount/outstandingAmount + EMPLOYEE_ADVANCE_SETTLE，非状态 writer，排除）；(b) reverseApprove→REJECTED 合规（doReverseApprove:219 目标态 = Bean `reverseApproveTargetStatus()` = REJECTED）；(c) docStatus 残余值（advance-status dict 5 值，代码仅写 DRAFT/CANCELLED，Bean 不纳入）；(d) SoD 边界（doApprove:195 `SoDGuard.assertApproverNotCreator` 动态守卫，非 Bean）；(e) 过账副作用边界（`EmployeeAdvanceAcctDocProvider implements IErpFinAcctDocProvider` + `ErpFinArApItemGenerator:168-170` EMPLOYEE_ADVANCE→RECEIVABLE，非 Bean）；(f) cashRepay/reverseCashRepay 非状态轴排除确认（BizModel，未触碰）；(g) **`ERR_EMPLOYEE_ADVANCE_NOT_REVERSED_BEFORE_CANCEL`（ErpFinErrors.java:206）零信任 grep CONFIRMED**——全 module-finance 仅定义零 throw（doCancel 自动红冲 :199 不抛此码）= dead code；(h) 与 ExpenseClaim 冲抵引用关系（EmployeeAdvanceProcessor grep offset 零命中，冲抵由 ExpenseClaim 侧 AdvanceOffsetOrchestrator 驱动，本实体为被冲抵方）。**owner doc §对象六与 dict/Bean/代码四方一致**（双轴矩阵逐项比对无漂移）。
  - Skill: `state-machine-business-review-prompt.md`
- [x] Add owner doc：在 `docs/design/finance/state-machine.md` §适用对象增列员工借款（第 6 项），新增 **§对象六：员工借款单状态机（双轴）**：approveStatus 轴（5 边 + initial/terminal：UNSUBMITTED←initial，APPROVED/REJECTED 可逆终态）+ docStatus 轴（1 边 cancel {非 CANCELLED}→CANCELLED + 残余值说明 + initial=DRAFT/terminal=CANCELLED）+ SoD 声明（动态守卫原位）+ 过账副作用引用（EmployeeAdvancePostingDispatcher→FinPostingExecutor，Dr 1221/Cr 1002 + ArApItem RECEIVABLE）+ cashRepay 非状态轴声明 + 与费用报销冲抵引用关系（被冲抵方，无 offset 调用）。交叉引用 `expense-claim.md`/`posting.md`。**已与 dict/Bean/代码逐项比对一致**（Phase 3 Proof 覆盖）。
  - Skill: `state-machine-business-review-prompt.md`
- [x] Decision（漂移裁定，路线图规则 5）：(a) docStatus 残余值 = `intentional legacy dict`（同 ExpenseClaim，dict 5 值保留不删，Bean 不纳入）；(b) M0.2 §3.5 finance M4.6/7 标「测试：无」**与实仓漂移登记**——实有 `TestErpFinEmployeeAdvanceApproval`（7 @Test）+ Posting（2）/CashRepay（6）/CashRepayReversal（3）测试，建议 reconcile 至 M0.2 清单；(c) **`ERR_EMPLOYEE_ADVANCE_NOT_REVERSED_BEFORE_CANCEL` 死码裁定 = `dead error code`**（layer-2 grep 零 throw 引用确认），cancel 自动红冲为 intentional auto-reverse 行为，错误码保留为未来「要求先反审核」语义入口，清理登记 successor（Deferred §1）；(d) reverseApprove→REJECTED = 已合规（非 drift）；(e) SoD = 动态守卫（非 Bean）。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] 四方对照无未裁决漂移（6 writer 全集 + docStatus 残余值 + reverseApprove 合规 + SoD/过账/cashRepay 边界 + NOT_REVERSED_BEFORE_CANCEL 死码裁定 + 测试名漂移均裁定并落入 owner doc/计划）
- [x] owner doc §对象六 双轴矩阵与 dict/Bean/代码一致

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（draft pending M4 gate）（`ses_006be3b10ffeInKpwpjDoOCIvx`，新会话零信任实仓复核 + 穷尽 writer/error-code 扫描）— 零 BLOCKER / 零 MAJOR / 2 MINOR(trivial)。全部 load-bearing 基线 CLAIM CONFIRMED：实体/dict（advance-status 5 值，残余值证实）/字段、6 writer（doSubmit:154/doWithdrawSubmit:159/doApprove:167/doReject:178/doReverseApprove:188/doCancel:203）+ 6 守卫行号、**reverseApprove→REJECTED @ :188 已合规**、markPosted:227/clearPosted:233 helper 抽取、net live writers=6 全在 facade do*、SoD @ :164、错误码 + 参数、**NOT_REVERSED_BEFORE_CANCEL 死码证实**（:206 定义，grep 全 module-finance 仅 2 定义零 throw——doCancel 自动红冲 :199 不抛此码，分类 watch-only residual 正确）、EmployeeAdvanceAcctDocProvider implements IErpFinAcctDocProvider + approve→tryPost、ArApItem RECEIVABLE（:168-170）、**无 AdvanceOffsetOrchestrator 调用证实**（grep offset 仅命中 ExpenseClaimProcessor，冲抵由 ExpenseClaim 侧驱动）、cashRepay/reverseCashRepay 非状态轴（更新 settledAmount/outstandingAmount，证实排除）、bean 注册行号、owner doc 缺口（需补 §对象六）、双轴矩阵匹配守卫、docStatus 残余值排除、M0.2 §3.5「测试：无」漂移（实有 TestErpFinEmployeeAdvanceApproval 7 @Test + 过账/cashRepay 测试）、xbiz 5 mutation + BizModel 1 Java mutation（含 auth）、§11.2 M4 (i)-(v) 全声明、R5=0。2 MINOR(trivial)：(MINOR-1) do* 引用为 status-write 行（非方法签名行），约定统一可辩护，建议加「status-write line」注脚（已在此记录注明）；(TRIVIAL-1) EmployeeAdvanceAcctDocProvider 基线引用省略 `/provider/` 子包，但 bean 注册用正确 FQN，不影响 Phase 1。anti-slack 全 PASS。草案审查收敛。
- **M4 plan-first 人工/owner-doc 门控状态：confirmed（2026-08-14 人工确认解除）**（§11.2 M4 (i)）。人工/owner 于 2026-08-14 确认「以行为保持的双轴矩阵集中化方式迁移、过账/SoD 完整保留、posted 不入轴、cashRepay 非状态轴不触碰、docStatus 残余值不纳入 Bean」可接受，门控解除。据此将 Plan Status 由 `draft` 转 `active`（对齐 M4.1/M4.2/M4.4/M4.5 plan-first 先例）。
- Independent draft review iteration 2: `acceptable as draft, held for M4 gate`（format/completeness/scope/closure 复核 + 跨计划一致性核实）。零 BLOCKER / 零 MAJOR。复核结论：(1) 格式合规——必需段全在、字段名/Phase 结构有效；(2) 完备性——各阶段 Exit Criteria 清晰可测，Execution Plan 覆盖全部 checklist；(3) 范围——M4.6+M4.7 单实体双轴、规则 14 合并声明充分、Non-Goals 清晰无 scope creep；(4) 结束证据——Closure Gates 定义验证命令/独立子代理审计/evidence-in-file。M4 hold 经跨计划核实为 **batch-consistent escape-hatch**：M4.1/M4.2/M4.4/M4.5 全部 `draft` + Review Hold，非财务域 M3 计划为 `completed`；该门控为财务保护域人工上游裁定（project-context.md §AI 阻塞条件硬停止），不可由审查者自主解除。保持 `draft` + Review Hold，无需修改。
- Independent draft review iteration 3 (mission-driver): `acceptable as draft, held for M4 gate`（format/completeness/scope/closure 复核 + escape-hatch 复核）。零 BLOCKER / 零 MAJOR。四维复核：(1) 格式合规——必需段全在（Current Baseline/Goals/Non-Goals/Task Route/Infrastructure And Config Prereqs/3 Phases/Draft Review Record/Closure Gates/Deferred But Adjudicated/Closure），字段名正确，Phase 结构有效，Item Types 用合法集（Add/Fix/Decision/Proof）；(2) 完备性——各阶段 Exit Criteria 清晰可测（localised compile、layer-1 矩阵绿、layer-3 绿、grep proof、四方对照），Execution Plan 覆盖全部 checklist；(3) 范围——M4.6+M4.7 单实体双轴，规则 14 合并声明充分（同实体/同 owner-doc/同结果表面/同验证路径），Non-Goals 清晰，cashRepay/posted/ORM 变更显式排除且带理由，无 scope creep；(4) 结束证据——Closure Gates 定义验证命令（`mvn test -pl module-finance/erp-fin-service -am` + `mvn clean install -DskipTests` + compliance checker）、M4 门控确认要求、独立子代理审计、evidence-in-file。M4 hold 跨计划复核（M4.1/M4.2/M4.4/M4.5 实证全 `draft`+Review Hold）确认为 batch-consistent escape-hatch；该门控为财务保护域人工上游裁定（project-context.md §AI 阻塞条件硬停止：会计/财务保护区域须有 owner doc），非审查者可自主解除。fix-forward 转义口适用：保持 `draft` + Review Hold（已存在），无需修改。
- Independent draft review iteration 4 (mission-driver): `acceptable as draft, held for M4 gate`（四维复核：format/completeness/scope/closure）。零 BLOCKER / 零 MAJOR / 零需修改项。复核结论与 iteration 2/3 一致：(1) 格式合规——必需段全在、字段名正确、Phase 结构有效、Item Types 用合法集；(2) 完备性——Exit Criteria 清晰可测，Execution Plan 覆盖全部 checklist；(3) 范围——M4.6+M4.7 单实体双轴，规则 14 合并充分，Non-Goals 清晰无 scope creep；(4) 结束证据——Closure Gates 定义验证命令/M4 门控确认/独立子代理审计/evidence-in-file。M4 plan-first 人工/owner-doc 门控为财务保护域人工上游裁定（project-context.md §AI 阻塞条件硬停止），非审查者可自主解除——fix-forward 转义口适用：保持 `draft` + Review Hold（已存在于 line 4），无需修改。
- Independent draft review iteration 5 (mission-driver 2026-08-13-193118): `acceptable as draft, held for M4 gate`（四维复核：format/completeness/scope/closure + 门控现态零信任核实）。零 BLOCKER / 零 MAJOR / 零需修改项。(1) 格式合规——必需段全在（Current Baseline/Goals/Non-Goals/Task Route/Infrastructure And Config Prereqs/3 Phases/Draft Review Record/Closure Gates/Deferred But Adjudicated/Closure），字段名正确，Phase 结构（Status/Targets/Skill/Item Types/Prereqs/Exit Criteria）有效，Item Types 用合法集（Add/Decision/Proof/Fix）；(2) 完备性——各阶段 Exit Criteria 清晰可测（localised compile `mvn compile -pl module-finance/erp-fin-service -am`、layer-1 矩阵绿、layer-3 `mvn test` 绿、grep proof、四方对照 10 维度），Execution Plan 覆盖全部 checklist（双轴两 Bean + 注册 + layer-1 矩阵 + 6 守卫接线 + layer-3 回归 + layer-2 四方对照 + owner doc §对象六 + 3 漂移 Decision）；(3) 范围——M4.6+M4.7 单实体双轴，规则 14 合并充分（同实体/同 owner-doc/同结果表面/同验证路径），Non-Goals 显式排除 cashRepay/posted/ORM 变更/残余值裁剪/sibling/Delta(M5.3)/CRUD 禁止(M0.1) 且带理由，无 scope creep；(4) 结束证据——Closure Gates 完备（验证命令 + M4 门控确认项 + 独立子代理审计占位 + evidence-in-file）。门控现态核实：roadmap `entity-state-machine-migration-roadmap.md` M4.6/M4.7 行 Status=`todo`；两批人工门控确认批次（M4.1/M4.2/M4.29/M4.30 + M4.11-M4.28）**均未纳入 M4.6/M4.7**——门控确为 pending，非可自解除。该门控为财务保护域人工上游裁定（project-context.md §AI 阻塞条件硬停止：会计/财务保护区域须有 owner doc + 人工确认；ai-autonomy-policy.md accounting/finance postings=plan-first），审查子代理无权解除。fix-forward 转义口适用：保持 `Plan Status: draft` + Review Hold（已存在于 line 4），无需修改。
- Independent draft review iteration 6 (mission-driver 2026-08-13-193118): `acceptable as draft, held for M4 gate`（四维复核：format/completeness/scope/closure）。零 BLOCKER / 零 MAJOR / 零需修改项。结论与 iteration 2/3/4/5 一致：(1) 格式合规——必需段全在，字段名正确，Phase 结构（Status/Targets/Skill/Item Types/Prereqs/Exit Criteria）有效，Item Types 用合法集（Phase1 Add|Decision|Proof、Phase2 Fix|Proof、Phase3 Proof|Decision|Add）；(2) 完备性——各阶段 Exit Criteria 清晰可测，Execution Plan 覆盖全部 checklist；(3) 范围——M4.6+M4.7 单实体双轴，规则 14 合并充分，Non-Goals 清晰无 scope creep；(4) 结束证据——Closure Gates 定义验证命令/M4 门控确认/独立子代理审计/evidence-in-file。M4 plan-first 人工/owner-doc 门控为财务保护域人工上游裁定（project-context.md §AI 阻塞条件硬停止：会计/财务保护区域须有 owner doc），审查子代理无权解除——fix-forward 转义口适用：保持 `Plan Status: draft` + Review Hold（已存在于 line 4），无需修改。
- Independent draft review iteration 7 (mission-driver 2026-08-13-193118): `acceptable as draft, held for M4 gate`（四维复核：format/completeness/scope/closure + escape-hatch 现态核实）。零 BLOCKER / 零 MAJOR / 零需修改项。结论与 iteration 2-6 一致：(1) 格式合规——必需段全在（Current Baseline/Goals/Non-Goals/Task Route/Infrastructure And Config Prereqs/3 Phases/Draft Review Record/Closure Gates/Deferred But Adjudicated/Closure），字段名正确，Phase 结构有效，Item Types 用合法集（Phase1 Add|Decision|Proof、Phase2 Fix|Proof、Phase3 Proof|Decision|Add）；(2) 完备性——各阶段 Exit Criteria 清晰可测（localised compile `mvn compile -pl module-finance/erp-fin-service -am`、layer-1 矩阵绿、layer-3 `mvn test` 绿、grep proof、四方对照 10 维度），Execution Plan 覆盖全部 checklist；(3) 范围——M4.6+M4.7 单实体双轴，规则 14 合并充分，Non-Goals 显式排除 cashRepay/posted/ORM/残余值裁剪/sibling/Delta(M5.3)/CRUD禁止(M0.1) 且带理由，无 scope creep；(4) 结束证据——Closure Gates 完备（验证命令 + M4 门控确认项 + 独立子代理审计占位 + evidence-in-file）。M4 plan-first 人工/owner-doc 门控为财务保护域人工上游裁定（project-context.md §AI 阻塞条件硬停止：会计/财务保护区域须有 owner doc + 人工确认），审查子代理无权解除——fix-forward 转义口适用：保持 `Plan Status: draft` + Review Hold（已存在于 line 4），无需修改。

- Independent draft review iteration 8 (mission-driver 2026-08-13-193118): `acceptable as draft, held for M4 gate`（四维复核：format/completeness/scope/closure + 门控现态与保护域规则零信任核实）。零 BLOCKER / 零 MAJOR / 零需修改项。(1) 格式合规——必需段全在（Current Baseline/Goals/Non-Goals/Task Route/Infrastructure And Config Prereqs/3 Phases/Draft Review Record/Closure Gates/Deferred But Adjudicated/Closure），字段名正确（Plan Status/Last Reviewed/Source/Related/Audit），Phase 结构（Status/Targets/Skill/Item Types/Prereqs/items/Exit Criteria）有效，Item Types 用合法集（Phase1 Add|Decision|Proof、Phase2 Fix|Proof、Phase3 Proof|Decision|Add，符合规则 7）；Review Hold 行置于 Plan Status 之后属「near front matter」合规。(2) 完备性——各阶段 Exit Criteria 清晰可测（Bean 无状态+残余值排除、注册+@Inject 非 private、layer-1 矩阵绿、本地化 `mvn compile`；6 守卫 grep proof、错误码+参数不变、layer-3 `mvn test` 绿；四方对照无未裁决漂移、owner doc §对象六 一致），Execution Plan 覆盖全部 checklist（2 Bean Add + 注册 + layer-1 Proof + Decision + 6 守卫接线 Fix + layer-3 Proof + layer-2 Proof + owner doc Add + 3 漂移 Decision）。(3) 范围——M4.6+M4.7 单实体双轴，规则 14 合并声明充分（同实体/同 owner-doc/同结果表面/同验证路径），Non-Goals 显式排除 cashRepay/posted/ORM 变更/残余值裁剪/sibling/Delta(M5.3)/CRUD 禁止(M0.1) 且带理由，无 "and also..." scope creep。(4) 结束证据——Closure Gates 完备（验证命令 `mvn test -pl module-finance/erp-fin-service -am` + `mvn clean install -DskipTests` + compliance checker + M4 门控确认项 + 独立子代理审计占位 + evidence-in-file）；Deferred But Adjudicated 3 项格式合规（Classification/Why Not Blocking/Successor Required）。**门控现态核实**：ai-autonomy-policy.md 保护区域表「accounting/finance postings = plan-first」（实施需 owner doc + tests + 计划审计，非审查者可自解除）；project-context.md §AI 阻塞条件「会计/财务保护区域须有 owner doc」硬停止；roadmap M4.6/M4.7 Status=`todo`（门控 pending）；跨计划复核：M4.1(voucher)/M4.2(period)=`completed` 仅因其 Review Hold 明示「已于 2026-08-13 经人工确认解除」，M4.4/M4.5(expense-claim sibling)=`draft`+Review Hold（gate pending）——本计划 `draft`+Review Hold 与未门控财务域 M4 计划批次一致，非孤例。该门控为财务保护域人工上游裁定，审查子代理无权解除——fix-forward 转义口适用：保持 `Plan Status: draft` + Review Hold（已存在于 line 4），无需修改。
- Independent draft review iteration 9 (mission-driver 2026-08-13-193118): `acceptable as draft, held for M4 gate`（四维复核：format/completeness/scope/closure）。零 BLOCKER / 零 MAJOR / 零需修改项。结论与 iteration 2-8 一致：(1) 格式合规——必需段全在，字段名正确，Phase 结构有效，Item Types 用合法集（Phase1 Add|Decision|Proof、Phase2 Fix|Proof、Phase3 Proof|Decision|Add）；(2) 完备性——Exit Criteria 清晰可测，Execution Plan 覆盖全部 checklist；(3) 范围——M4.6+M4.7 单实体双轴，规则 14 合并充分，Non-Goals 清晰无 scope creep；(4) 结束证据——Closure Gates 完备（验证命令 + M4 门控确认 + 独立子代理审计占位 + evidence-in-file）。M4 plan-first 人工/owner-doc 门控为财务保护域人工上游裁定（project-context.md §AI 阻塞条件硬停止：会计/财务保护区域须有 owner doc；ai-autonomy-policy.md accounting/finance postings=plan-first），审查子代理无权解除——fix-forward 转义口适用：保持 `Plan Status: draft` + Review Hold（已存在于 line 4），无需修改。
- Independent draft review iteration 10 (mission-driver 2026-08-14-070716): `acceptable as draft, held for M4 gate`（四维复核：format/completeness/scope/closure + 门控现态与 baseline 零信任抽查）。零 BLOCKER / 零 MAJOR / 零需修改项。(1) 格式合规——必需段全在（Current Baseline/Goals/Non-Goals/Task Route/Infrastructure And Config Prereqs/3 Phases/Draft Review Record/Closure Gates/Deferred But Adjudicated/Closure），字段名正确，Phase 结构（Status/Targets/Skill/Item Types/Prereqs/items/Exit Criteria）有效，Item Types 用合法集；(2) 完备性——各阶段 Exit Criteria 清晰可测（本地化 `mvn compile -pl module-finance/erp-fin-service -am`、layer-1 矩阵绿、6 守卫 grep proof、错误码+参数不变、layer-3 `mvn test` 绿、四方对照 10 维度、owner doc §对象六），Execution Plan 覆盖全部 checklist；(3) 范围——M4.6+M4.7 单实体双轴，规则 14 合并充分（同实体/同 owner-doc/同结果表面/同验证路径），Non-Goals 显式排除 cashRepay/posted/ORM/残余值裁剪/sibling/Delta(M5.3)/CRUD 禁止(M0.1) 且带理由，无 scope creep；(4) 结束证据——Closure Gates 完备（验证命令 + M4 门控确认项 + 独立子代理审计占位 + evidence-in-file）。**零信任抽查**：Processor 实仓复核 6 守卫行号（validateTransitionForSubmit:74/Withdraw:82/Approve:89/Reject:96/ReverseApprove:103/Cancel:110）+ 6 do* status-write 行（doSubmit:154/doWithdrawSubmit:159/doApprove:167/doReject:178/doReverseApprove:188/doCancel:203）+ SoDGuard:164 + tryPost:165 + markPosted:171（helper :227-231）+ clearPosted:186/201（helper :233-237）+ reverseApprove→REJECTED:188 + CANCELLED:203 全部 CONFIRMED；`ERR_EMPLOYEE_ADVANCE_NOT_REVERSED_BEFORE_CANCEL`（ErpFinErrors:206）grep 全 module-finance 仅定义零 throw——死码基线 CONFIRMED；`statemachine/` 包 + app-service.beans.xml 末尾 Bean 注册模式实存（M4.1/M4.11/M4.12/M4.2 4 Bean，行 379-396），追加注册路径有效。**门控现态零信任核实**：roadmap `entity-state-machine-migration-roadmap.md` M4.6/M4.7 行 Status=`todo`（行 102-103）；两批 2026-08-13 人工门控确认（M4.1/M4.2/M4.29/M4.30 + M4.11-M4.28）**均未纳入 M4.6/M4.7**；M4.1（voucher）/M4.2（period）`completed` 仅因 Review Record 存明示人工确认记录（2026-08-13）；姊妹 M4.4/M4.5（expense-claim）`draft`+Review Hold 且同 batch-consistent（今日同 mission 复核亦保持 hold）——门控确为 pending，本审查为 subagent 非人工，无权解除（ai-autonomy-policy.md:9/11 不得移除阻塞项或自证清除；project-context.md 会计/财务保护区域硬停止）。fix-forward 转义口适用：保持 `Plan Status: draft` + Review Hold（已存在于 line 4），无需修改。`Last Reviewed` 更新为 2026-08-14。

> **行号约定注脚**：本计划 do* 方法引用（如 doApprove:167）指向 **status-write 行**（`advance.setApproveStatus(APPROVED)`），非方法签名行（doApprove 签名 :163）；validateTransitionFor* 引用为声明行。约定在 do* 族内统一。

## Closure Gates

> 本计划含生产代码变更（2 Bean + 6 守卫接线 + 测试 + owner doc 补章节），Closure Gates 运行完整仓库验证。无 ORM/API/字典变更（5+4 值保留，残余值不删），Compliance 基线预期无漂移（R5=0/R11=0）。

- [x] 范围内行为完成（2 Bean + 6 守卫接线 + 三层证据；过账/SoD/红冲/cashRepay 完整保留，§11.2 M4 (ii)/(iv)/(v)）
- [x] 相关文档对齐（owner doc §对象六 新增 + 漂移 Decision 登记；路线图 M4.6/M4.7 done）
- [x] 已运行验证：`mvn test -pl module-finance/erp-fin-service -am` + Closure 时 `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`
- [x] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### `ERR_EMPLOYEE_ADVANCE_NOT_REVERSED_BEFORE_CANCEL` 死码清理

- Classification: `watch-only residual`
- Why Not Blocking Closure: 该错误码（`:206`）已定义但 `doCancel` 不抛（cancel 自动红冲已过账凭证）。Phase 3 layer-2 grep 裁定其分类。若为 dead code，清理属错误码治理（需确认零引用后移除，保护区域）。
- Successor Required: yes（触发条件 = 错误码统一清理批次，或 PM 要求 cancel 前置反审核工作流时复活此码）

### docStatus dict 残余值裁剪

- Classification: `watch-only residual`
- Why Not Blocking Closure: `erp-fin/advance-status` dict 含 SUBMITTED/APPROVED/REJECTED 但代码不写（workflow 轴 = approveStatus，同 ExpenseClaim）。Bean 不纳入残余值。裁剪属 dict 治理，需独立 ask-first。
- Successor Required: yes（触发条件 = dict 治理统一清理残余值时，与 ExpenseClaim 同批）

### 通用 CRUD 写入禁止 / Delta 覆盖证明

- Classification: `watch-only residual` / `optimization candidate`
- Why Not Blocking Closure: CRUD 写入边界 = M0.1 successor；M4 保护域单项不自带 Delta 证明，归 M5.3。
- Successor Required: no（归 M0.1/M5.3）

## Closure

Status Note: completed（2026-08-14 全 3 Phase 执行完成；独立结束审计 APPROVED——子代理零信任亲跑 37 关键测试全绿，10 项检查全 PASS；审计发现的 2 项闭包簿记 MINOR（Closure 证据占位 + M0.2 reconcile 已登记 successor）已处理：Closure 证据回填，M0.2 清单 reconcile 维持 successor（与姊妹计划一致））

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话零信任，`ses_001bb790bffeNdNANI9XCQ1KLh`）
- Evidence: 10 项检查全 PASS——(1) Plan 一致性（Plan Status: completed / 3 Phase Status: completed / 零 `[ ]` 残留 / Closure Gates 全 [x]）；(2) Approval Bean 无状态 + 矩阵完整（5 守卫 + 6 边 + initial/terminal + 零 DAO/IBiz/事务 import）；(3) Document Bean 无状态 + 残余值排除（SUBMITTED/APPROVED/REJECTED 不在任一集合）；(4) beans.xml FQN-id 注册 + xmllint well-formed；(5) Processor 6 守卫全委托 + common→领域码映射带 cause + 参数对外不变 + do* 目标态写回（行号 185/190/198/209/219/234 与计划一致）+ SoD/tryPost/markPosted/clearPosted/approvedBy/At/cancel 自动红冲原序保留 + BizModel 零 diff（cashRepay 未触碰）；(6) 层 1 矩阵 19 @Test（12 + 7）覆盖合法/非法边 + 残余值排除 + 可达性；(7) owner doc §对象六（双轴矩阵 + 残余值 + SoD + 过账引用 + cashRepay 非状态轴 + 被冲抵方）与 dict/Bean/代码一致；(8) roadmap M4.6/M4.7 `done`；(9) `mvn -o test -pl module-finance/erp-fin-service -Dtest='...'` 亲跑 **37/37 绿**（19 矩阵 + 7 Approval + 2 Posting + 6 CashRepay + 3 CashRepayReversal）；(10) git 范围精确（M×7 + ??×4 = 2 Bean + 2 测试 + Processor + beans.xml + owner doc + roadmap + plan + log）。

Follow-up:

- <非阻塞跟进见 §Deferred But Adjudicated；已确认缺陷不得出现在此处>

### layer-2 四方对照审计记录（plan Phase 3 Proof，10 维度，双轴各一份）

**1. dict ↔ Bean ↔ owner doc ↔ writer 一致性（approveStatus 轴）**

| 维度 | 结论 |
|------|------|
| dict | `wf/approve-status` 4 值（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED，平台共享 dict） |
| Bean 元数据 | `ErpFinEmployeeAdvanceApprovalStateMachine`：initial={UNSUBMITTED}、terminal={APPROVED,REJECTED}、transitions 6 边（submit×2 + withdraw + approve + reject + reverseApprove） |
| owner doc | §对象六 approveStatus 轴 5 边表 + 终态说明（可逆终态），与 Bean 一致 |
| writer 全集（独立 grep 重核） | 5 命名动作 writer 全在 facade `do*`：`ErpFinEmployeeAdvanceProcessor` doSubmit:185（submitTargetStatus）、doWithdrawSubmit:190、doApprove:198、doReject:209、doReverseApprove:219（reverseApproveTargetStatus=REJECTED）。per-mutation Processor `setApproveStatus` override（5 处）为 abstract base 桥接（非独立 writer，编排入口 `submitForApproval/cancel` 等经 `processor.requireAdvance/validateTransition*/do*` 委托 facade，独立 grep 证实）。BizModel 零 approveStatus/docStatus 写入（grep `ErpFinEmployeeAdvanceBizModel` 零命中）。过账路径（tryPost/reverse）写 `posted` 非 approveStatus。cashRepay/reverseCashRepay 为资金动作（settledAmount/outstandingAmount + EMPLOYEE_ADVANCE_SETTLE，非状态 writer，排除）。框架入口（CRUD save/update 可写，§9.4 选项 c）与测试 fixture 为既定类别 |

**2. dict ↔ Bean ↔ owner doc ↔ writer 一致性（docStatus 轴）**

| 维度 | 结论 |
|------|------|
| dict | `erp-fin/advance-status` 5 值（DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED，orm.xml:1393 引用） |
| Bean 元数据 | `ErpFinEmployeeAdvanceDocumentStateMachine`：initial={DRAFT}、terminal={CANCELLED}、transitions 1 代表边（cancel DRAFT→CANCELLED，loose 非 CANCELLED 源）；**残余值 SUBMITTED/APPROVED/REJECTED 不纳入任一集合**（layer-1 测试断言） |
| owner doc | §对象六 docStatus 轴 + 残余值说明，与 Bean 一致 |
| writer 全集 | 1 命名动作 writer：`ErpFinEmployeeAdvanceProcessor.doCancel:234`（cancelTargetStatus）。`validateNotCancelled`→`validateTransitionForCancel` 经 Document Bean 守卫。seed 写 DRAFT（创建路径），无其他生产 docStatus writer |

**3. 重点维度裁定**

- (a) **6 writer 全集 = 5 approve do* + 1 cancel do***：独立 grep 证实（`ErpFinEmployeeAdvanceProcessor:185/190/198/209/219/234`），与 plan Current Baseline 声明一致；per-mutation 桥接排除（编排入口全部委托 facade）。
- (b) **reverseApprove→REJECTED 合规**：Bean `reverseApproveTargetStatus()=REJECTED` ↔ `doReverseApprove:219` 一致，已合规 `domain-design-guidelines.md §16.4`（与 purchase/ExpenseClaim 先例一致），非 drift。
- (c) **docStatus 残余值**：dict 5 值但生产代码仅写 DRAFT（seed）/CANCELLED（cancel）；SUBMITTED/APPROVED/REJECTED 为 `intentional legacy dict`（Decision，见下方裁定），Bean 不纳入，dict 项保留不删。
- (d) **SoD 边界**：`SoDGuard.assertApproverNotCreator`（`doApprove:195`，抛 `ERR_FIN_APPROVER_IS_CREATOR`）为动态业务守卫，保留 Processor 原位，非 Bean 范畴（架构 `entity-state-machine-bean.md:274`）；layer-3 `TestErpFinEmployeeAdvanceApproval` SoD 拒绝断言全绿。
- (e) **过账副作用边界**：tryPost（`doApprove:196`）/reverse（`doReverseApprove:214`、`doCancel:226`）/markPosted（helper）/clearPosted（helper）/approvedBy/approvedAt 写入全保留原序（§11.2 M4 (ii)/(iv)/(v)），Bean 无任何副作用 import（grep 证实无 DAO/IBiz/事务 import）。
- (f) **approve→posted=true + ArApItem RECEIVABLE**：`doApprove` 保留原序（SoD→tryPost→markPosted 三件套），layer-3 `TestErpFinEmployeeAdvancePosting`（approve→EMPLOYEE_ADVANCE 凭证 + ArApItem RECEIVABLE + reverseApprove→红冲）全绿证实；`EmployeeAdvanceAcctDocProvider implements IErpFinAcctDocProvider` + `ErpFinArApItemGenerator:168-170` EMPLOYEE_ADVANCE→RECEIVABLE（Dr 1221 其他应收-员工预支 / Cr 1002 银行存款）。
- (g) **NOT_REVERSED_BEFORE_CANCEL 死码**：`ErpFinErrors.java:206` 定义，独立 grep 全 module-finance 仅定义零 throw——doCancel 自动红冲已过账凭证（intentional auto-reverse）不抛此码 = **dead error code**（Decision，见下方裁定 + §Deferred But Adjudicated）。
- (h) **冲抵引用关系**：`ErpFinEmployeeAdvanceProcessor` grep offset 零命中——本实体**无** `AdvanceOffsetOrchestrator` 调用，为费用报销冲抵的**被冲抵方**（冲抵由 ExpenseClaim 侧 `AdvanceOffsetOrchestrator.offset` 按 advance code 驱动）。

### 漂移 Decision（路线图规则 5）

- **(D1) docStatus 残余值 = `intentional legacy dict`**：dict 5 值但代码仅写 DRAFT/CANCELLED，SUBMITTED/APPROVED/REJECTED 为残余（workflow 轴 = approveStatus，docStatus 仅 DRAFT→CANCELLED）。dict 项保留不删，Bean 不纳入任一集合。裁剪属 dict 治理，需独立 ask-first（保护区域：不改 ORM/dict），记入 §Deferred But Adjudicated。
- **(D2) M0.2 §3.5 finance M4.6/7 标「测试：无」与实仓漂移**：实有 `TestErpFinEmployeeAdvanceApproval`（7 @Test）+ `TestErpFinEmployeeAdvancePosting`（2）+ `TestErpFinEmployeeAdvanceCashRepay`（6）+ `TestErpFinEmployeeAdvanceCashRepayReversal`（3）。登记建议 reconcile M0.2 清单（successor：M0.2 清单维护批次统一 reconcile）。
- **(D3) `ERR_EMPLOYEE_ADVANCE_NOT_REVERSED_BEFORE_CANCEL` = dead error code**：`ErpFinErrors.java:206` 定义，全仓零 throw 引用（layer-2 独立 grep 证实）；doCancel 自动红冲已过账凭证为 intentional auto-reverse 行为（`doCancel:224-232`）。错误码保留为未来「要求先反审核」工作流的语义入口，清理登记 successor（§Deferred But Adjudicated）。
- **(D4) reverseApprove→REJECTED = 已合规**：Bean/代码/owner doc 三方一致（§16.4），非 drift，无 Fix。
- **(D5) SoD = 动态守卫**：非 Bean 范畴（架构契约 §8/§11.2 M4 (i)），保留 Processor 原位，无 Fix。

### owner doc 变更

- `docs/design/finance/state-machine.md`：§适用对象增列员工借款单（EmployeeAdvance，第 6 项）；新增 **§对象六：员工借款单状态机（双轴）**（approveStatus 轴 5 边 + docStatus 轴 1 边 + 残余值说明 + SoD 声明 + 过账/冲抵边界 + cashRepay 非状态轴声明 + NOT_REVERSED_BEFORE_CANCEL 死码注记 + 实现注记）。双轴矩阵与 dict/Bean/代码一致（owner doc §对象六 ↔ orm.xml:1393-1394/1398 ↔ 两 Bean ↔ `ErpFinEmployeeAdvanceProcessor` do*）。
