# 2026-08-13-1146-3-erpfin-employee-advance-state-machine-beans 员工借款 ErpFinEmployeeAdvance.docStatus + approveStatus 实体级状态机 Bean（M4.6 + M4.7）

> Plan Status: draft
> Review Hold: §11.2 M4 (i) 人工/owner-doc 门控待确认——本计划触及受保护借款过账行为（approve 触发 `EmployeeAdvancePostingDispatcher.tryPost`→`FinPostingExecutor` 生成 EMPLOYEE_ADVANCE 凭证 + `ErpFinArApItem`（RECEIVABLE，1221 应收-员工预支），已由起草者经 live code 实证：`ErpFinEmployeeAdvanceProcessor:165` tryPost + `:171` markPosted + `EmployeeAdvanceAcctDocProvider` implements `IErpFinAcctDocProvider` + `ErpFinArApItemGenerator:168-170` EMPLOYEE_ADVANCE→RECEIVABLE）。M4 plan-first 门控成立；该人工裁定非起草者可自主解除（project-context.md 财务保护域硬停止）。计划格式/完备性/范围/结束证据就绪后，保持 `draft` 直至门控确认。
> Last Reviewed: 2026-08-13
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

Status: planned
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/statemachine/ErpFinEmployeeAdvanceApprovalStateMachine.java`、`.../ErpFinEmployeeAdvanceDocumentStateMachine.java`（新建）、`app-service.beans.xml`（注册）、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/statemachine/TestErpFinEmployeeAdvanceApprovalStateMachineMatrix.java`、`.../TestErpFinEmployeeAdvanceDocumentStateMachineMatrix.java`（新建）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（layer-1 表驱动测试）

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done

- [ ] 新建无状态 `ErpFinEmployeeAdvanceApprovalStateMachine`：矩阵 `assertCanSubmit({UNSUBMITTED,REJECTED})`→`submitTargetStatus()=SUBMITTED`；`assertCanWithdraw(SUBMITTED)`→UNSUBMITTED；`assertCanApprove(SUBMITTED)`→APPROVED；`assertCanReject(SUBMITTED)`→REJECTED；`assertCanReverseApprove(APPROVED)`→REJECTED。`initialStatuses()={UNSUBMITTED}`、`terminalStatuses()={APPROVED,REJECTED}`、`isTerminal`。`transitions()` 编码 5 命名边。`normalize(null)`→UNSUBMITTED。非法来源态抛 common 码携带 `action`/`fromStatus`。grep 证实无 DAO/IBiz/事务 import。**形状与姊妹 `ErpFinExpenseClaimApprovalStateMachine` 一致**（同 4 态 approve 轴），但为独立 Bean（§3 每实体每轴一 Bean，不跨实体复用）。
  - Skill: `nop-backend-dev`
- [ ] 新建无状态 `ErpFinEmployeeAdvanceDocumentStateMachine`：矩阵 `assertCanCancel({非 CANCELLED})`→CANCELLED。`initialStatuses()={DRAFT}`、`terminalStatuses()={CANCELLED}`。**docStatus dict 残余值 SUBMITTED/APPROVED/REJECTED 不纳入任一集合**（javadoc 标注 intentional reserved，同 ExpenseClaim）。`transitions()` 编码 1 命名边（cancel）。非法来源态（CANCELLED）抛 common 码。
  - Skill: `nop-backend-dev`
- [ ] Decision（前置）：记录 (a) docStatus 残余值分类（intentional legacy dict，同 ExpenseClaim）；(b) `ERR_EMPLOYEE_ADVANCE_NOT_REVERSED_BEFORE_CANCEL`（`:206`）已定义但 `doCancel`（`:195-206`）不抛——cancel 自动红冲已过账凭证（intentional auto-reverse），该错误码疑似 dead/vestigial——供 Phase 3 裁定（layer-2 须独立 grep 确认该码零 throw 引用）。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] 在 `app-service.beans.xml` 以 FQN id 注册两 Bean。
  - Skill: `nop-backend-dev`
- [ ] Proof（layer-1 矩阵，表驱动，两 Bean 各一份）：Approval 覆盖 submit（UNSUBMITTED & REJECTED 合法、SUBMITTED/APPROVED 非法）/withdraw（SUBMITTED 合法）/approve（SUBMITTED 合法、其余非法）/reject（SUBMITTED 合法）/reverseApprove（APPROVED 合法、其余非法）+ 终态无出边 + transitions(5) + initial/terminal。Document 覆盖 cancel（DRAFT 合法、CANCELLED 非法）+ **断言 SUBMITTED/APPROVED/REJECTED 不在任一集合**（残余值排除）+ transitions(1)。不经 Processor 入口。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 两 Bean 无状态、矩阵完整；docStatus 残余值排除；残余值 + 死码 Decision 记录在案
- [ ] 两 Bean 已在 `app-service.beans.xml` 注册（FQN id）；`@Inject` 字段非 private（合规 R5）
- [ ] layer-1 矩阵测试通过；本地化编译 `mvn compile -pl module-finance/erp-fin-service -am` 通过（解除 Phase 2 接线依赖）

### Phase 2 - Processor 接线（行为保持，SoD/过账副作用保留）+ layer-3 回归

Status: planned
Targets: `ErpFinEmployeeAdvanceProcessor`（6 个 do* 守卫委托）
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1 Bean 落地

- [ ] `ErpFinEmployeeAdvanceProcessor` 注入两 Bean（`@Inject` 非 private），将 6 个 `validateTransitionFor*` 内联固定 `Objects.equals` 判断替换为对应 Bean `assertCan<Action>(from)` + 目标态写回。5 approve 守卫（`validateTransitionForSubmit:74`/`Withdraw:82`/`Approve:89`/`Reject:96`/`ReverseApprove:103`）委托 ApprovalStateMachine；1 doc 守卫（`validateTransitionForCancel:110`）委托 DocumentStateMachine。common→领域码映射（Approval→`ERR_EMPLOYEE_ADVANCE_ILLEGAL_STATUS_TRANSITION`，Document→`ERR_EMPLOYEE_ADVANCE_ILLEGAL_DOC_STATUS_TRANSITION`，common 作 cause）+ 参数对外不变。**完整保留**：SoD（`:164` `SoDGuard`）、tryPost（`:165`）/reverse（`:184`）、markPosted（`:171`/helper `:227`）/clearPosted（`:186,201`/helper `:233`）、approvedBy/approvedAt 写入、业务规则校验、reverseApprove→REJECTED、cancel 自动红冲 if APPROVED+posted、乐观锁。**cashRepay/reverseCashRepay（BizModel）不触碰**。
  - Skill: `nop-backend-dev`
- [ ] Proof（layer-3 回归）：`mvn test -pl module-finance/erp-fin-service -am` 全绿——重点 `TestErpFinEmployeeAdvanceApproval`（7 @Test：submit→approve→reverse + reject/resubmit + cancel + 非法 approve-from-unsubmitted + employee-inactive/partner-missing/amount 守卫）、`TestErpFinEmployeeAdvancePosting`（approve→EMPLOYEE_ADVANCE 凭证 + ArApItem RECEIVABLE `:71-75` + reverseApprove→红冲不变）、`TestErpFinEmployeeAdvanceCashRepay`/`...CashRepayReversal`（cashRepay 不受影响）。证明 5 approve 边 + 1 doc 边 + 过账/SoD/cashRepay 行为不变。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 6 守卫改调 Bean 委托，grep 证实相关方法体内不再有内联固定状态矩阵判断（动态副作用 SoD/tryPost/markPosted/clearPosted/业务规则除外）；cashRepay/reverseCashRepay 方法体未触碰
- [ ] 领域错误码 + 参数对外不变（layer-3 断言证实）；5 approve 边 + 1 doc 边 + reverseApprove→REJECTED + 过账/SoD + cancel 自动红冲行为不变
- [ ] layer-3 `mvn test -pl module-finance/erp-fin-service -am` 全绿

### Phase 3 - layer-2 四方对照 + owner doc 补章节 + 漂移 Decision

Status: planned
Targets: 四方对照审计记录（写入本计划 Closure 段）；`docs/design/finance/state-machine.md`（新增 §对象六 EmployeeAdvance 双轴）；本计划 Closure
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度）

- Item Types: `Proof | Decision | Add`
- Prereqs: Phase 2 接线完成

- [ ] Proof（layer-2 四方对照，§11.1 步骤 5，10 维度，双轴各一份）：dict（advance-status 5 值 + wf/approve-status 4 值）↔ owner doc（新增 §对象六 + expense-claim.md §源）↔ Bean 元数据 ↔ writer。重点：(a) 6 命名动作 writer 全集（5 approve do* + 1 cancel do*，须独立 grep 重核）；(b) reverseApprove→REJECTED 合规；(c) docStatus 残余值；(d) SoD 边界（动态守卫）；(e) 过账副作用边界（ArApItem RECEIVABLE，非 Bean）；(f) cashRepay/reverseCashRepay 非状态轴（排除确认）；(g) **`ERR_EMPLOYEE_ADVANCE_NOT_REVERSED_BEFORE_CANCEL`（`:206`）throw 引用零信任 grep**——确认是否 dead code（cancel 自动红冲，不抛此码）；(h) 与 ExpenseClaim 的冲抵引用关系（被冲抵方，无 offset 调用）。writer 盘点含命名动作 + per-mutation 桥接（非独立 writer，排除）+ 过账路径（非状态 writer）+ cashRepay（非状态轴，排除）+ 框架入口 + 测试 fixture。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] Add owner doc：在 `docs/design/finance/state-machine.md` §适用对象增列员工借款，新增 **§对象六：员工借款单状态机（双轴）**：approveStatus 轴（5 边 + initial/terminal）+ docStatus 轴（1 边 cancel + 残余值说明 + initial/terminal）+ SoD 声明 + 过账副作用引用（ArApItem RECEIVABLE）+ cashRepay 非状态轴声明 + 与费用报销冲抵引用关系（被冲抵方）。交叉引用 `expense-claim.md`/`posting.md`。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] Decision（漂移裁定，路线图规则 5）：(a) docStatus 残余值 = `intentional legacy dict`（同 ExpenseClaim）；(b) M0.2 §3.5 finance M4.6/7 标「测试：无」**与实仓漂移**——实有 `TestErpFinEmployeeAdvanceApproval`（7 @Test）+ 过账/cashRepay 测试，登记建议 reconcile；(c) `ERR_EMPLOYEE_ADVANCE_NOT_REVERSED_BEFORE_CANCEL` 死码裁定（依 layer-2 grep 结果：若零 throw 引用 = `dead error code`，登记 successor 清理；若 cancel 自动红冲为 intentional = `intentional auto-reverse behavior`，错误码保留为未来「要求先反审核」语义入口）；(d) reverseApprove→REJECTED = 已合规（非 drift）；(e) SoD = 动态守卫（非 Bean）。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] 四方对照无未裁决漂移（6 writer 全集 + docStatus 残余值 + reverseApprove 合规 + SoD/过账/cashRepay 边界 + NOT_REVERSED_BEFORE_CANCEL 死码裁定 + 测试名漂移均裁定并落入 owner doc/计划）
- [ ] owner doc §对象六 双轴矩阵与 dict/Bean/代码一致

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（draft pending M4 gate）（`ses_006be3b10ffeInKpwpjDoOCIvx`，新会话零信任实仓复核 + 穷尽 writer/error-code 扫描）— 零 BLOCKER / 零 MAJOR / 2 MINOR(trivial)。全部 load-bearing 基线 CLAIM CONFIRMED：实体/dict（advance-status 5 值，残余值证实）/字段、6 writer（doSubmit:154/doWithdrawSubmit:159/doApprove:167/doReject:178/doReverseApprove:188/doCancel:203）+ 6 守卫行号、**reverseApprove→REJECTED @ :188 已合规**、markPosted:227/clearPosted:233 helper 抽取、net live writers=6 全在 facade do*、SoD @ :164、错误码 + 参数、**NOT_REVERSED_BEFORE_CANCEL 死码证实**（:206 定义，grep 全 module-finance 仅 2 定义零 throw——doCancel 自动红冲 :199 不抛此码，分类 watch-only residual 正确）、EmployeeAdvanceAcctDocProvider implements IErpFinAcctDocProvider + approve→tryPost、ArApItem RECEIVABLE（:168-170）、**无 AdvanceOffsetOrchestrator 调用证实**（grep offset 仅命中 ExpenseClaimProcessor，冲抵由 ExpenseClaim 侧驱动）、cashRepay/reverseCashRepay 非状态轴（更新 settledAmount/outstandingAmount，证实排除）、bean 注册行号、owner doc 缺口（需补 §对象六）、双轴矩阵匹配守卫、docStatus 残余值排除、M0.2 §3.5「测试：无」漂移（实有 TestErpFinEmployeeAdvanceApproval 7 @Test + 过账/cashRepay 测试）、xbiz 5 mutation + BizModel 1 Java mutation（含 auth）、§11.2 M4 (i)-(v) 全声明、R5=0。2 MINOR(trivial)：(MINOR-1) do* 引用为 status-write 行（非方法签名行），约定统一可辩护，建议加「status-write line」注脚（已在此记录注明）；(TRIVIAL-1) EmployeeAdvanceAcctDocProvider 基线引用省略 `/provider/` 子包，但 bean 注册用正确 FQN，不影响 Phase 1。anti-slack 全 PASS。草案审查收敛。
- **M4 plan-first 人工/owner-doc 门控状态：pending**（§11.2 M4 (i)）。本计划触及受保护借款过账行为。草案审查虽已收敛（acceptable as draft），但在人工/owner-doc 确认「以行为保持的双轴矩阵集中化方式迁移、过账/SoD 完整保留、posted 不入轴、cashRepay 非状态轴不触碰、docStatus 残余值不纳入 Bean」前保持 `Plan Status: draft`（对齐 M4.1/M4.2/M4.4/M4.5 plan-first 先例）。确认后在此追加记录（日期 + 批准范围），方可转 `active`。
- Independent draft review iteration 2: `acceptable as draft, held for M4 gate`（format/completeness/scope/closure 复核 + 跨计划一致性核实）。零 BLOCKER / 零 MAJOR。复核结论：(1) 格式合规——必需段全在、字段名/Phase 结构有效；(2) 完备性——各阶段 Exit Criteria 清晰可测，Execution Plan 覆盖全部 checklist；(3) 范围——M4.6+M4.7 单实体双轴、规则 14 合并声明充分、Non-Goals 清晰无 scope creep；(4) 结束证据——Closure Gates 定义验证命令/独立子代理审计/evidence-in-file。M4 hold 经跨计划核实为 **batch-consistent escape-hatch**：M4.1/M4.2/M4.4/M4.5 全部 `draft` + Review Hold，非财务域 M3 计划为 `completed`；该门控为财务保护域人工上游裁定（project-context.md §AI 阻塞条件硬停止），不可由审查者自主解除。保持 `draft` + Review Hold，无需修改。
- Independent draft review iteration 3 (mission-driver): `acceptable as draft, held for M4 gate`（format/completeness/scope/closure 复核 + escape-hatch 复核）。零 BLOCKER / 零 MAJOR。四维复核：(1) 格式合规——必需段全在（Current Baseline/Goals/Non-Goals/Task Route/Infrastructure And Config Prereqs/3 Phases/Draft Review Record/Closure Gates/Deferred But Adjudicated/Closure），字段名正确，Phase 结构有效，Item Types 用合法集（Add/Fix/Decision/Proof）；(2) 完备性——各阶段 Exit Criteria 清晰可测（localised compile、layer-1 矩阵绿、layer-3 绿、grep proof、四方对照），Execution Plan 覆盖全部 checklist；(3) 范围——M4.6+M4.7 单实体双轴，规则 14 合并声明充分（同实体/同 owner-doc/同结果表面/同验证路径），Non-Goals 清晰，cashRepay/posted/ORM 变更显式排除且带理由，无 scope creep；(4) 结束证据——Closure Gates 定义验证命令（`mvn test -pl module-finance/erp-fin-service -am` + `mvn clean install -DskipTests` + compliance checker）、M4 门控确认要求、独立子代理审计、evidence-in-file。M4 hold 跨计划复核（M4.1/M4.2/M4.4/M4.5 实证全 `draft`+Review Hold）确认为 batch-consistent escape-hatch；该门控为财务保护域人工上游裁定（project-context.md §AI 阻塞条件硬停止：会计/财务保护区域须有 owner doc），非审查者可自主解除。fix-forward 转义口适用：保持 `draft` + Review Hold（已存在），无需修改。
- Independent draft review iteration 4 (mission-driver): `acceptable as draft, held for M4 gate`（四维复核：format/completeness/scope/closure）。零 BLOCKER / 零 MAJOR / 零需修改项。复核结论与 iteration 2/3 一致：(1) 格式合规——必需段全在、字段名正确、Phase 结构有效、Item Types 用合法集；(2) 完备性——Exit Criteria 清晰可测，Execution Plan 覆盖全部 checklist；(3) 范围——M4.6+M4.7 单实体双轴，规则 14 合并充分，Non-Goals 清晰无 scope creep；(4) 结束证据——Closure Gates 定义验证命令/M4 门控确认/独立子代理审计/evidence-in-file。M4 plan-first 人工/owner-doc 门控为财务保护域人工上游裁定（project-context.md §AI 阻塞条件硬停止），非审查者可自主解除——fix-forward 转义口适用：保持 `draft` + Review Hold（已存在于 line 4），无需修改。
- Independent draft review iteration 5 (mission-driver 2026-08-13-193118): `acceptable as draft, held for M4 gate`（四维复核：format/completeness/scope/closure + 门控现态零信任核实）。零 BLOCKER / 零 MAJOR / 零需修改项。(1) 格式合规——必需段全在（Current Baseline/Goals/Non-Goals/Task Route/Infrastructure And Config Prereqs/3 Phases/Draft Review Record/Closure Gates/Deferred But Adjudicated/Closure），字段名正确，Phase 结构（Status/Targets/Skill/Item Types/Prereqs/Exit Criteria）有效，Item Types 用合法集（Add/Decision/Proof/Fix）；(2) 完备性——各阶段 Exit Criteria 清晰可测（localised compile `mvn compile -pl module-finance/erp-fin-service -am`、layer-1 矩阵绿、layer-3 `mvn test` 绿、grep proof、四方对照 10 维度），Execution Plan 覆盖全部 checklist（双轴两 Bean + 注册 + layer-1 矩阵 + 6 守卫接线 + layer-3 回归 + layer-2 四方对照 + owner doc §对象六 + 3 漂移 Decision）；(3) 范围——M4.6+M4.7 单实体双轴，规则 14 合并充分（同实体/同 owner-doc/同结果表面/同验证路径），Non-Goals 显式排除 cashRepay/posted/ORM 变更/残余值裁剪/sibling/Delta(M5.3)/CRUD 禁止(M0.1) 且带理由，无 scope creep；(4) 结束证据——Closure Gates 完备（验证命令 + M4 门控确认项 + 独立子代理审计占位 + evidence-in-file）。门控现态核实：roadmap `entity-state-machine-migration-roadmap.md` M4.6/M4.7 行 Status=`todo`；两批人工门控确认批次（M4.1/M4.2/M4.29/M4.30 + M4.11-M4.28）**均未纳入 M4.6/M4.7**——门控确为 pending，非可自解除。该门控为财务保护域人工上游裁定（project-context.md §AI 阻塞条件硬停止：会计/财务保护区域须有 owner doc + 人工确认；ai-autonomy-policy.md accounting/finance postings=plan-first），审查子代理无权解除。fix-forward 转义口适用：保持 `Plan Status: draft` + Review Hold（已存在于 line 4），无需修改。

> **行号约定注脚**：本计划 do* 方法引用（如 doApprove:167）指向 **status-write 行**（`advance.setApproveStatus(APPROVED)`），非方法签名行（doApprove 签名 :163）；validateTransitionFor* 引用为声明行。约定在 do* 族内统一。

## Closure Gates

> 本计划含生产代码变更（2 Bean + 6 守卫接线 + 测试 + owner doc 补章节），Closure Gates 运行完整仓库验证。无 ORM/API/字典变更（5+4 值保留，残余值不删），Compliance 基线预期无漂移（R5=0/R11=0）。

- [ ] 范围内行为完成（2 Bean + 6 守卫接线 + 三层证据；过账/SoD/红冲/cashRepay 完整保留，§11.2 M4 (ii)/(iv)/(v)）
- [ ] 相关文档对齐（owner doc §对象六 新增 + 漂移 Decision 登记；路线图 M4.6/M4.7 done）
- [ ] 已运行验证：`mvn test -pl module-finance/erp-fin-service -am` + Closure 时 `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`
- [ ] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [ ] 结束证据存在于文件中

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

Status Note: <待执行与独立结束审计后填充>

Closure Audit Evidence:

- Auditor / Agent: <独立子代理>
- Evidence: <task id / walkthrough record>

Follow-up:

- <非阻塞跟进见 §Deferred But Adjudicated；已确认缺陷不得出现在此处>
