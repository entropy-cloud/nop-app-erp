# 2026-08-13-1146-2-erpfin-expense-claim-state-machine-beans 费用报销 ErpFinExpenseClaim.docStatus + approveStatus 实体级状态机 Bean（M4.4 + M4.5）

> Plan Status: draft
> Review Hold: §11.2 M4 (i) 人工/owner-doc 门控待确认——本计划触及受保护报销过账行为（approve 触发 `ExpenseClaimPostingDispatcher.tryPost`→`FinPostingExecutor` 生成 EXPENSE_CLAIM 凭证 + `ErpFinArApItem`（PAYABLE）+ `AdvanceOffsetOrchestrator` 员工借款冲抵，已由起草者经 live code 实证：`ErpFinExpenseClaimProcessor:253` tryPost + `:262` offset + `ExpenseClaimAcctDocProvider` implements `IErpFinAcctDocProvider`）。M4 plan-first 门控成立；该人工裁定非起草者可自主解除（project-context.md 财务保护域硬停止）。计划格式/完备性/范围/结束证据就绪后，保持 `draft` 直至门控确认。
> Last Reviewed: 2026-08-13
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.4（ErpFinExpenseClaim.docStatus）+ M4.5（ErpFinExpenseClaim.approveStatus），plan-first；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 finance M4.4/5`（FIN-10/11 纳入，报销凭证+SoD）
> Related: M4 plan-first 先例 `2026-08-13-2045-3-erpfin-voucher-state-machine-bean.md`（M4.1）+ `2026-08-13-2045-1-erpfin-period-state-machine-bean.md`（M4.2）；M0.1 契约 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（done）+ M1.3 模板 `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（done）；双轴（docStatus+approveStatus）先例 `2026-08-13-0805-2-erpast-movement-state-machine-beans.md`（M3.15+M3.16）+ `2026-08-13-0945-1-purchase-approvestatus-state-machine-bean.md`（M3.2-M3.5 approve 轴）；姊妹计划 `2026-08-13-1146-3-erpfin-employee-advance-state-machine-beans.md`（M4.6+M4.7，近同构 sibling）
> Mission: entity-state-machine
> Work Item: M4.4 + M4.5
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。费用报销 approve 触发业财过账（凭证 + ArApItem PAYABLE + 员工借款冲抵），reverseApprove/cancel 触发红冲。声明 §11.2 M4 硬约束：(i) plan-first + 受保护行为人工/owner-doc 门控；(ii) 过账/冲抵时序/编排/失败兜底不改，继续由 `ExpenseClaimPostingDispatcher`→`FinPostingExecutor` + `AdvanceOffsetOrchestrator` + `posted` 标志契约管理；(iii) `posted` 不入轴（§3；实体有 `posted` boolean，排除-posted）；(iv) 跨域副作用（ArApItem 生成、员工借款冲抵、红冲）保留原 Processor 路径；(v) SoD（approver-is-creator）作为动态业务守卫保留原位（非 Bean 范畴）。本计划是 plan-first 产物，人工/owner-doc 确认门控未满足前保持 `draft`。
>
> **规则 14 合并声明（M4.4 + M4.5 同计划）**：docStatus 与 approveStatus 为同一实体 `ErpFinExpenseClaim` 的两条独立状态轴，同一 owner-doc 义务、同一结果表面（报销单双轴状态机）、同一验证路径。按 §3 双轴分离（不合并笛卡尔积），落地**两个独立 Bean**（`ErpFinExpenseClaimApprovalStateMachine` + `ErpFinExpenseClaimDocumentStateMachine`），合为单计划两 Phase 组。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 finance M4.4/5` + 实仓核实。

- **实体**：`ErpFinExpenseClaim`（`module-finance/model/app-erp-finance.orm.xml:1258`，`useLogicalDelete="true"` `deleteFlagProp="delVersion"` `createrProp="createdBy"`）。双轴：
  - `docStatus` propId=17 `:1278` `ext:dict="erp-fin/expense-claim-status"`。dict 5 值（orm.xml:266-272）：`DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED`。
  - `approveStatus` propId=18 `:1279` `ext:dict="wf/approve-status"`（**平台共享 dict**，定义于 `../nop-entropy/nop-wf/nop-wf-meta/.../dict/wf/approve-status.dict.yaml`，4 值：`UNSUBMITTED/SUBMITTED/APPROVED/REJECTED`）。
  - `posted` boolean propId=21 `:1282`（排除-posted）；`approvedBy`(`:1280`)/`approvedAt`(`:1281`)/`postedBy`(`:1283`)/`postedAt`(`:1284`)；`createdBy` propId=27 `:1288`（SoD 源）。
- **常量**：service 层 `ErpFinConstants extends ErpFinDocStatus`（`ErpFinConstants.java:12`），状态常量在 dao 层 `ErpFinDocStatus.java`（`module-finance/erp-fin-dao/.../constants/`）：`APPROVE_STATUS_UNSUBMITTED/SUBMITTED/APPROVED/REJECTED`（`:14-17`）、`DOC_STATUS_DRAFT/CANCELLED`（`:20-21`）。**注意**：docStatus 轴代码只写 `DRAFT`（seed）与 `CANCELLED`（cancel）——SUBMITTED/APPROVED/REJECTED 在 docStatus dict 中为**残余值**（生命周期推进由 approveStatus 轴承载）。
- **approveStatus 现状 writer（5 命名动作，全部 facade `do*` 写，实仓核实）**：
  - `submitForApproval`（xbiz mutation）：`doSubmit`（`ErpFinExpenseClaimProcessor:242`）approveStatus `UNSUBMITTED/REJECTED→SUBMITTED`，守卫 `validateTransitionForSubmit`（`:94-101`）。
  - `withdrawApproval`（xbiz）：`doWithdrawSubmit`（`:247`）`SUBMITTED→UNSUBMITTED`，守卫 `validateTransitionForWithdraw`（`:103-108`）。
  - `approve`（xbiz，auth `ErpFinExpenseClaim:approve`）：`doApprove`（`:255`）`SUBMITTED→APPROVED`，守卫 `validateTransitionForApprove`（`:110-115`）；含 **SoD**（`:252` `SoDGuard.assertApproverNotCreator`）+ tryPost（`:253`）+ markPosted（`:259-261`）+ offset（`:262`）。
  - `reject`（xbiz）：`doReject`（`:270`）`SUBMITTED→REJECTED`，守卫 `validateTransitionForReject`（`:117-122`）。
  - `reverseApprove`（xbiz，auth `ErpFinExpenseClaim:reverseApprove`）：`doReverseApprove`（`:284`）`APPROVED→REJECTED`，守卫 `validateTransitionForReverseApprove`（`:124-129`）；含 reverse posting + clearPosted（`:279-281`）+ reverseOffset（`:276`）。
- **docStatus 现状 writer（1 命名动作）**：
  - `cancel`（Java `@BizMutation` `ErpFinExpenseClaimBizModel.cancel:31-35`→`ErpFinExpenseClaimCancelProcessor`→facade）：`doCancel`（`:303`）`*→CANCELLED`，守卫 `validateTransitionForCancel`（`:131-135`，已 CANCELLED 则拒绝）；含 reverse posting if posted。
- **per-mutation Processors（6 个）**：SubmitForApproval/Approve/Reject/ReverseApprove/WithdrawApproval/Cancel，均 `inject(...)` 委托 facade `do*`，其 `setApproveStatus`/`setDocStatus` override 为 abstract base 桥接（非独立 writer）。xbiz 5 mutation + BizModel 1 Java mutation。**净 live writer = 6，全在 `ErpFinExpenseClaimProcessor.do*`**。
- **reverseApprove 目标 = REJECTED**（`:284`，已合规于 domain-design-guidelines §16.4，与 purchase 一致，无 drift）。
- **错误码**：`ERR_EXPENSE_CLAIM_ILLEGAL_STATUS_TRANSITION`（`ErpFinErrors:151-153`，参数 `ARG_CLAIM_CODE`/`ARG_CURRENT_STATUS`/`ARG_EXPECTED_STATUS`）；`ERR_EXPENSE_CLAIM_ILLEGAL_DOC_STATUS_TRANSITION`（`:155-157`，参数 `ARG_CLAIM_CODE`/`ARG_CURRENT_DOC_STATUS`/`ARG_EXPECTED_DOC_STATUS`）。SoD 码 `ERR_FIN_APPROVER_IS_CREATOR`（`:482-484`，参数 `ARG_USER_ID`）。common 层码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION` 已存在。
- **过账 + 冲抵副作用**：`ExpenseClaimPostingDispatcher.tryPost`（buildEvent EXPENSE_CLAIM → `FinPostingExecutor.postEvent`）；`ExpenseClaimAcctDocProvider`（implements `IErpFinAcctDocProvider`，Dr 6602 管理费用 / Dr 2221 进项税 / Cr 2241 应付-员工 或 1002 银行存款，bean 注册 `app-service.beans.xml:94-95`）；ArApItem（`ErpFinArApItemGenerator` EXPENSE_CLAIM→PAYABLE）；`AdvanceOffsetOrchestrator.offset/reverseOffset`（员工借款冲抵，bean `:112-113`）。SoD = 动态业务守卫（非 Bean 范畴，架构 `entity-state-machine-bean.md:274`）。
- **生产 Bean 注册**：`module-finance/erp-fin-service/src/main/resources/_vfs/erp/fin/beans/app-service.beans.xml`（374 行）：facade `ErpFinExpenseClaimProcessor`（`:149-150`）+ 6 per-mutation Processor（`:256-267`）+ dispatcher/provider/orchestrator。StateMachine Bean 追加于文件末尾。finance 当前无 `statemachine/` 包（本计划与 M4.8/M4.9/M4.1/M4.2 并行创建，互不依赖）。
- **既有测试（layer-3 回归基线，非 greenfield）**：
  - `TestErpFinExpenseClaimApproval`（`module-finance/erp-fin-service/src/test/.../entity/`，267 行，9 `@Test`）：submit/approve/reject/reverseApprove/withdraw/resubmit/cancel + SoD 自审拒绝（`:140-144`）+ 校验守卫（claimant-inactive/partner-missing/lines-empty/amount-mismatch/非法 approve-from-unsubmitted）。经 `IGraphQLEngine` RPC。
  - 过账集成：`TestErpFinExpenseClaimPosting`（approve→posting EXPENSE_CLAIM 凭证 + reverseApprove→红冲）、`TestErpFinExpenseOffsetAdvance`（approve/reverseApprove 员工借款冲抵）、`TestErpFinPartnerIdResolution`、`TestErpFinAcctDocProviderAccountKey`（Provider 单元）。
  - **M0.2 §3.5 finance M4.4/5 标「测试：无」与实仓漂移**——实有上述 layer-3 测试。layer-1 矩阵测试为 greenfield（新增）。Phase 4 登记此漂移。
- **合规基线**：`@Inject private` 须保持 R5=0（fin-service grep 证实当前满足）。本计划保持 R5=0、R11 不增。
- **owner doc 覆盖**：`docs/design/finance/state-machine.md` §适用对象（`:7-10`）仅列 Voucher + AccountingPeriod。ExpenseClaim 仅在 §职责分离（`:264`）作为 SoD 对象被提及，**无状态机章节**。业务设计在 `docs/design/finance/expense-claim.md`。**owner-doc 缺口**：需在 state-machine.md 补 §对象五（ExpenseClaim 双轴）。

## Goals

- 落地两个无状态 Bean（§2 无状态约束，§3 双轴分离）：
  - `ErpFinExpenseClaimApprovalStateMachine`（approveStatus 轴，`Approval` 后缀）：矩阵 `submitForApproval` {UNSUBMITTED,REJECTED}→SUBMITTED；`withdrawApproval` {SUBMITTED}→UNSUBMITTED；`approve` {SUBMITTED}→APPROVED；`reject` {SUBMITTED}→REJECTED；`reverseApprove` {APPROVED}→REJECTED。initial=`{UNSUBMITTED}`，terminal=`{APPROVED,REJECTED}`。
  - `ErpFinExpenseClaimDocumentStateMachine`（docStatus 轴，`Document` 后缀）：矩阵 `cancel` {非 CANCELLED}→CANCELLED（`assertCanCancel` 校验 `!isCancelled`）。initial=`{DRAFT}`，terminal=`{CANCELLED}`。**docStatus dict 中 SUBMITTED/APPROVED/REJECTED 残余值不纳入任一集合**（intentional reserved，生命周期推进由 approveStatus 承载）。
  - 两 Bean 均可经 Delta 同名覆盖。
- 将 6 个 `do*` 的固定 `validateTransitionFor*` 内联守卫改调对应 Bean `assertCan<Action>(from)` + 目标态回写。**动态业务守卫与副作用保留原位**：SoD（`SoDGuard.assertApproverNotCreator`）、过账（tryPost/reverse）、冲抵（offset/reverseOffset）、posted/approvedBy/approvedAt 写入、业务规则校验（claimant-inactive/partner-missing/lines-empty/amount-mismatch）、乐观锁。
- 保持全部既有外部行为不变（错误码 + 参数、5 approve 边 + 1 doc 边、reverseApprove→REJECTED、approve 过账/冲抵时序、cancel 红冲闭环、SoD 拒绝）。
- 新增 layer-1 矩阵完备性表驱动测试（两 Bean 各覆盖合法/非法边 + initial/terminal + docStatus 残余值排除）；layer-3 既有集成测试回归全绿。
- layer-2 四方对照：确认双轴矩阵 + docStatus 残余值漂移 + M0.2 测试名漂移登记。

## Non-Goals

- 不迁移 `posted`（boolean，§3 不入轴）、不迁移 `approvedBy`/`approvedAt`/`postedBy`/`postedAt` 为状态轴。
- 不改变过账/冲抵编排（`ExpenseClaimPostingDispatcher`/`AdvanceOffsetOrchestrator` 时序、失败兜底）、不改变 SoD 语义（`SoDGuard` 保留原位，非 Bean 范畴）、不改变 reverseApprove→REJECTED 目标（已合规）。
- 不修改 `model/*.orm.xml`、字典值或 API 契约（docStatus dict 5 值保留，残余值不删；approveStatus 用平台 dict 不动）。
- 不迁移 `ErpFinEmployeeAdvance`（M4.6+M4.7 姊妹计划）、`ErpFinVoucher`（M4.1）或 finance 其余轴。
- 不引入通用 CRUD 对 docStatus/approveStatus 写入的运行时禁止（M0.1 successor）。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）：门控未确认前计划保持 `draft`。
- 不证 Delta 覆盖（M4 保护域单项，归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M1.3 模板 + M0.2 清单；落地双轴两 Bean + 接线 + 三层测试 + 四方对照；不改契约/模型/公共 API；**M4 plan-first**——报销过账 + 冲抵，finance 保护域）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 迁移模板 + §11.2 M4 变体 + §3 posted 不入轴 + §双轴分离）、`docs/design/finance/state-machine.md`（**§适用对象 缺口需补 §对象五**）、`docs/design/finance/expense-claim.md`（报销业务设计 §源）、`docs/design/finance/posting.md`（业财打通）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M0.2 清单 finance M4.4/5）、`docs/plans/2026-08-13-0805-2-erpast-movement-state-machine-beans.md`（双轴先例）、`docs/plans/2026-08-13-0945-1-purchase-approvestatus-state-machine-bean.md`（approve 轴 + reverseApprove→REJECTED + SoD 保留先例）
- Skill Selection Basis: 路线图 M4.4/M4.5 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「facade do* 接线、双轴分离 Bean、approve 轴 5 边 + doc 轴 cancel、SoD/过账/冲抵副作用保留、错误码映射、`@Inject` 非 private、过账吞异常自检」；`nop-testing` 匹配「双轴矩阵表驱动测试 + 既有 Approval/Posting 集成测试回归」。层 2 引用 `state-machine-business-review-prompt.md`。必需输入均已就绪。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护报销过账 + 员工借款冲抵行为。在人工/owner-doc 确认「以行为保持的双轴矩阵集中化方式迁移、过账/冲抵/SoD 完整保留、posted 不入轴、docStatus 残余值不纳入 Bean」可接受前，计划保持 `draft`，不得进入实施。门控记录须写入本计划 Draft Review Record。
- SoD config `erp-common.sod-enabled`（默认 true，`%test` profile 关闭以允许单账号 E2E）保留不动。
- 无新增端口/环境变量/CORS/密钥/.env/外部服务依赖。无数据迁移。

## Execution Plan

### Phase 1 - 双轴 Bean + 注册 + layer-1 矩阵测试

Status: planned
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/statemachine/ErpFinExpenseClaimApprovalStateMachine.java`、`.../ErpFinExpenseClaimDocumentStateMachine.java`（新建）、`app-service.beans.xml`（注册）、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/statemachine/TestErpFinExpenseClaimApprovalStateMachineMatrix.java`、`.../TestErpFinExpenseClaimDocumentStateMachineMatrix.java`（新建）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（layer-1 表驱动测试）

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done

- [ ] 新建无状态 `ErpFinExpenseClaimApprovalStateMachine`：矩阵 `assertCanSubmit({UNSUBMITTED,REJECTED})`→`submitTargetStatus()=SUBMITTED`；`assertCanWithdraw(SUBMITTED)`→UNSUBMITTED；`assertCanApprove(SUBMITTED)`→APPROVED；`assertCanReject(SUBMITTED)`→REJECTED；`assertCanReverseApprove(APPROVED)`→REJECTED。`initialStatuses()={UNSUBMITTED}`、`terminalStatuses()={APPROVED,REJECTED}`、`isTerminal`。`transitions()` 编码 5 命名边。`normalize(null)`→UNSUBMITTED。非法来源态抛 common 码携带 `action`/`fromStatus`。grep 证实无 DAO/IBiz/事务 import。
  - Skill: `nop-backend-dev`
- [ ] 新建无状态 `ErpFinExpenseClaimDocumentStateMachine`：矩阵 `assertCanCancel({非 CANCELLED})`→CANCELLED（校验 `!isCancelled(from)`）。`initialStatuses()={DRAFT}`、`terminalStatuses()={CANCELLED}`。**docStatus dict 残余值 SUBMITTED/APPROVED/REJECTED 不纳入任一集合**（javadoc 标注 intentional reserved——生命周期推进由 approveStatus 承载，docStatus 仅 DRAFT→CANCELLED）。`transitions()` 编码 1 命名边（cancel）。非法来源态（CANCELLED）抛 common 码。
  - Skill: `nop-backend-dev`
- [ ] Decision（前置）：记录 docStatus 残余值分类——dict 5 值但代码仅写 DRAFT/CANCELLED，SUBMITTED/APPROVED/REJECTED 为残余（intentional，workflow 轴 = approveStatus）；Bean DocumentStateMachine 仅建模 DRAFT→CANCELLED，残余值不纳入任一集合，dict 项保留不删。供 Phase 4 owner-doc/Decision 引用。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] 在 `app-service.beans.xml` 以 FQN id 注册两 Bean。
  - Skill: `nop-backend-dev`
- [ ] Proof（layer-1 矩阵，表驱动，两 Bean 各一份）：Approval 覆盖 submit（UNSUBMITTED & REJECTED 合法、SUBMITTED/APPROVED 非法）/withdraw（SUBMITTED 合法）/approve（SUBMITTED 合法、其余非法）/reject（SUBMITTED 合法）/reverseApprove（APPROVED 合法、其余非法）+ 终态无出边 + transitions(5) + initial/terminal。Document 覆盖 cancel（DRAFT 合法、CANCELLED 非法）+ **断言 SUBMITTED/APPROVED/REJECTED 不在 initial/terminal/transitions 任一集合**（残余值排除）+ transitions(1)。不经 Processor 入口。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 两 Bean 无状态、矩阵完整；docStatus 残余值排除；残余值 Decision 记录在案
- [ ] 两 Bean 已在 `app-service.beans.xml` 注册（FQN id）；`@Inject` 字段非 private（合规 R5）
- [ ] layer-1 矩阵测试通过；本地化编译 `mvn compile -pl module-finance/erp-fin-service -am` 通过（解除 Phase 2 接线依赖）

### Phase 2 - Processor 接线（行为保持，SoD/过账/冲抵副作用保留）+ layer-3 回归

Status: planned
Targets: `ErpFinExpenseClaimProcessor`（6 个 do* 守卫委托）
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1 Bean 落地

- [ ] `ErpFinExpenseClaimProcessor` 注入两 Bean（`@Inject` 非 private），将 6 个 `validateTransitionFor*` 内联固定 `Objects.equals` 判断替换为对应 Bean `assertCan<Action>(from)` + 目标态写回。5 approve 守卫（`validateTransitionForSubmit:94`/`Withdraw:103`/`Approve:110`/`Reject:117`/`ReverseApprove:124`）委托 ApprovalStateMachine；1 doc 守卫（`validateTransitionForCancel:131`）委托 DocumentStateMachine。common→领域码映射（Approval→`ERR_EXPENSE_CLAIM_ILLEGAL_STATUS_TRANSITION`，Document→`ERR_EXPENSE_CLAIM_ILLEGAL_DOC_STATUS_TRANSITION`，common 作 cause）+ 参数对外不变。**完整保留**：SoD（`:252` `SoDGuard`）、tryPost（`:253`）/reverse、offset（`:262`）/reverseOffset（`:276`）、posted/approvedBy/approvedAt 写入、业务规则校验、reverseApprove→REJECTED、cancel 红冲 if posted、乐观锁。
  - Skill: `nop-backend-dev`
- [ ] Proof（layer-3 回归）：`mvn test -pl module-finance/erp-fin-service -am` 全绿——重点 `TestErpFinExpenseClaimApproval`（9 @Test：submit/approve/reject/reverseApprove/withdraw/resubmit/cancel + SoD 自审拒绝 `:140-144` + 非法 approve-from-unsubmitted + claimant/partner/lines/amount 守卫）、`TestErpFinExpenseClaimPosting`（approve→EXPENSE_CLAIM 凭证 + reverseApprove→红冲不变）、`TestErpFinExpenseOffsetAdvance`（冲抵 + reverse 不变）。证明 5 approve 边 + 1 doc 边 + 过账/冲抵/SoD 行为不变。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 6 守卫改调 Bean 委托，grep 证实相关方法体内不再有内联固定状态矩阵判断（动态副作用 SoD/tryPost/offset/posted 写入/业务规则除外）
- [ ] 领域错误码 + 参数对外不变（layer-3 断言证实）；5 approve 边 + 1 doc 边 + reverseApprove→REJECTED + 过账/冲抵/SoD 行为不变
- [ ] layer-3 `mvn test -pl module-finance/erp-fin-service -am` 全绿

### Phase 3 - layer-2 四方对照 + owner doc 补章节 + 漂移 Decision

Status: planned
Targets: 四方对照审计记录（写入本计划 Closure 段）；`docs/design/finance/state-machine.md`（新增 §对象五 ExpenseClaim 双轴）；本计划 Closure
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度）

- Item Types: `Proof | Decision | Add`
- Prereqs: Phase 2 接线完成

- [ ] Proof（layer-2 四方对照，§11.1 步骤 5，10 维度，双轴各一份）：dict（expense-claim-status 5 值 + wf/approve-status 4 值）↔ owner doc（新增 §对象五 + expense-claim.md §源）↔ Bean 元数据 ↔ writer。重点：(a) 6 命名动作 writer 全集（5 approve do* + 1 cancel do*，须独立 grep 重核）；(b) reverseApprove→REJECTED 合规；(c) docStatus 残余值 SUBMITTED/APPROVED/REJECTED（dict 有但代码不写，Bean 不纳入）；(d) SoD 边界（动态守卫，非 Bean）；(e) 过账/冲抵副作用边界（非 Bean 范畴）；(f) approve→posted=true + ArApItem PAYABLE + offset。writer 盘点含命名动作 + per-mutation 桥接（非独立 writer，排除）+ 过账路径（非状态 writer）+ 框架入口 + 测试 fixture。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] Add owner doc：在 `docs/design/finance/state-machine.md` §适用对象增列费用报销，新增 **§对象五：费用报销单状态机（双轴）**：approveStatus 轴（5 边：submit{UNSUBMITTED,REJECTED}→SUBMITTED / withdraw→UNSUBMITTED / approve→APPROVED / reject→REJECTED / reverseApprove→REJECTED + initial/terminal）+ docStatus 轴（1 边 cancel→CANCELLED + 残余值说明 + initial/terminal）+ SoD 声明（动态守卫保留 Processor）+ 过账/冲抵副作用引用 `posting.md`/`expense-claim.md`。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] Decision（漂移裁定，路线图规则 5）：(a) docStatus 残余值 = `intentional legacy dict`（workflow 轴 = approveStatus，docStatus 仅 DRAFT/CANCELLED，dict 项保留不删，Bean 不纳入）；(b) M0.2 §3.5 finance M4.4/5 标「测试：无」**与实仓漂移**——实有 `TestErpFinExpenseClaimApproval`（9 @Test）+ 4 过账测试，登记建议 reconcile；(c) reverseApprove→REJECTED = 已合规（非 drift）；(d) SoD = 动态守卫（非 Bean 范畴，架构 `entity-state-machine-bean.md:274`）。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] 四方对照无未裁决漂移（6 writer 全集 + docStatus 残余值 + reverseApprove 合规 + SoD/过账边界 + 测试名漂移均裁定并落入 owner doc/计划）
- [ ] owner doc §对象五 双轴矩阵与 dict/Bean/代码一致

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（draft pending M4 gate）（`ses_006be6595ffeuYqRoc9M7A4ZLn`，新会话零信任实仓复核 + 穷尽 writer 扫描）— 零 BLOCKER / 零 MAJOR / 1 MINOR。全部 load-bearing 基线 CLAIM CONFIRMED：实体/dict（expense-claim-status 5 值 + wf/approve-status 4 值）/字段/常量（ErpFinDocStatus dao 层 + ErpFinConstants extends）、6 writer（doSubmit:242/doWithdrawSubmit:247/doApprove:255/doReject:270/doReverseApprove:284/doCancel:303）+ 6 守卫行号、**reverseApprove→REJECTED @ :284 已合规**、net live writers=6 全在 facade do*（per-mutation = 桥接）、SoD @ :252、错误码 + 参数、ExpenseClaimAcctDocProvider implements IErpFinAcctDocProvider + approve→tryPost+offset、bean 注册行号、owner doc 缺口（需补 §对象五）、双轴矩阵匹配守卫、docStatus 残余值排除、M0.2 §3.5「测试：无」漂移（实有 TestErpFinExpenseClaimApproval 9 @Test + 过账测试）、§11.2 M4 (i)-(v) 全声明。1 MINOR：(MINOR-1) Current Baseline 引用 approvedBy/approvedAt/postedBy/postedAt orm.xml 行号误漏首位「1」（:280→:1280 等，已修正）。anti-slack 全 PASS。草案审查收敛。
- Plan review（mission-driver 2026-08-13-080540-mission-driver）：`approved (review ran); held as draft` — 四维度（格式合规性/完备性/范围/结束证据）经核对全部就绪，无除门控外的 blocker/major。双轴合并（M4.4+M4.5）符合规则 14（同实体/同 owner doc/同结果表面/同验证路径 → 两独立 Bean 两 Phase 组）；Non-Goals 显式排除 sibling/Delta(M5.3)/CRUD 禁止(M0.1)/dict 清理，无 scope creep；三层证据 + Closure Gates 完备（含 M4 门控项 + 验证命令 + 独立审计占位）。Review Hold 与 4 个姊妹 M4 计划（M4.1/M4.2/M4.6/M4.7）同构，为项目既定治理约定。唯一 Blocker = §11.2 M4 (i) 人工/owner-doc 门控，为外部依赖：本计划触及 `accounting/finance postings` 保护域（ai-autonomy-policy.md plan-first）；`entity-state-machine-bean.md:279-283` 显式 M4 全 plan-first，受保护过账/红冲/冲抵行为不因 Bean 抽象免除人工门控。撤销 mission 级、roadmap 记录的门控裁定属「放宽保护区域」，非审查子代理可自主解除。故保持 `Plan Status: draft`（holding 机制），不晋升 active。门控解除后于本记录追加（日期 + 批准范围）并转 `active`。
- Plan review（mission-driver 2026-08-13-193118-mission-driver）：`approved (review ran); held as draft` — 四维度复核全部就绪：(1) 格式合规——必需节齐全（Current Baseline/Goals/Non-Goals/Task Route/Infrastructure/Execution Plan 三 Phase/Draft Review Record/Closure Gates/Deferred But Adjudicated/Closure），字段名正确，Phase 结构（Status/Targets/Skill/Item Types/Prereqs/Exit Criteria）有效；(2) 完备性——三 Phase Exit Criteria 清晰可测，Execution Plan 覆盖全部 checklist（双轴 Bean + 注册 + layer-1 矩阵 + Processor 6 守卫接线 + layer-3 回归 + layer-2 四方对照 + owner doc §对象五 + 漂移 Decision）；(3) 范围——M4.4+M4.5 合并合规（规则 14：同实体/同 owner doc/同结果表面/同验证路径→两独立 Bean 两 Phase 组），Non-Goals 显式排除 sibling/Delta(M5.3)/CRUD 禁止(M0.1)/dict 清理，无 scope creep；(4) 结束证据——Closure Gates 完备（含 M4 门控项 + 验证命令 + 独立审计占位）。唯一 Blocker = §11.2 M4 (i) 人工/owner-doc 门控，经实仓核验为真实外部依赖：`entity-state-machine-bean.md:279-283` 显式 M4 全 plan-first + (i)「触及受保护行为时不因 Bean 抽象免除人工/owner-doc 门控」；`ai-autonomy-policy.md:72` accounting/finance postings=plan-first；line 9/11「AI 不得在无明确人工确认或人工批准的 owner-doc 证据下移除阻塞项；AI 编写 owner doc 不能自证清除阻塞」——owner doc 缺口（state-machine.md 无 ExpenseClaim 章节，Phase 3 待补）+ 无人工确认 = 门控未满足。本审查为 subagent 非人工，无权解除（line 9）。与 4 个姊妹 M4 计划（M4.1/M4.2/M4.6/M4.7）同构。保持 `Plan Status: draft`（holding 机制）。
- Plan review（mission-driver 2026-08-14-mission-driver）：`approved (review ran); held as draft` — 独立四维度复核全部就绪：(1) 格式合规——必需节齐全（Current Baseline/Goals/Non-Goals/Task Route/Infrastructure/Execution Plan 三 Phase/Draft Review Record/Closure Gates/Deferred But Adjudicated/Closure），字段名正确，Phase 结构（Status/Targets/Skill/Item Types/Prereqs/Exit Criteria）有效，Item Types 均取自允许集合；(2) 完备性——三 Phase Exit Criteria 清晰可测（含 `mvn compile/test -pl module-finance/erp-fin-service -am` + grep 验证），Execution Plan 覆盖全部 checklist（双轴 Bean + 注册 + layer-1 矩阵 + Processor 6 守卫接线 + layer-3 回归 + layer-2 四方对照 + owner doc §对象五 + 漂移 Decision）；(3) 范围——M4.4+M4.5 合并合规（规则 14），Non-Goals 显式排除 sibling/M4.1/Delta(M5.3)/CRUD 禁止(M0.1)/dict 清理/posted 迁移，无 scope creep；(4) 结束证据——Closure Gates 完备（含 M4 门控项 + 验证命令 + 独立审计占位 + 文本一致性）。唯一 Blocker = §11.2 M4 (i) 人工/owner-doc 门控，为真实外部依赖（`project-context.md:68` 会计/财务保护域硬停止；owner doc state-machine.md 无 ExpenseClaim 章节 = 门控未满足）。本审查为 subagent 非人工，无权解除保护域门控（ai-autonomy-policy.md）。与 4 个姊妹 M4 计划同构。保持 `Plan Status: draft`（holding 机制）。
- **M4 plan-first 人工/owner-doc 门控状态：pending**（§11.2 M4 (i)）。本计划触及受保护报销过账 + 员工借款冲抵行为。草案审查虽已收敛（acceptable as draft），但在人工/owner-doc 确认「以行为保持的双轴矩阵集中化方式迁移、过账/冲抵/SoD 完整保留、posted 不入轴、docStatus 残余值不纳入 Bean」前保持 `Plan Status: draft`（对齐 M4.1/M4.2 plan-first 先例）。确认后在此追加记录（日期 + 批准范围），方可转 `active`。

## Closure Gates

> 本计划含生产代码变更（2 Bean + 6 守卫接线 + 测试 + owner doc 补章节），Closure Gates 运行完整仓库验证。无 ORM/API/字典变更（5+4 值保留，残余值不删），Compliance 基线预期无漂移（R5=0/R11=0）。

- [ ] 范围内行为完成（2 Bean + 6 守卫接线 + 三层证据；过账/冲抵/SoD/红冲完整保留，§11.2 M4 (ii)/(iv)/(v)）
- [ ] 相关文档对齐（owner doc §对象五 新增 + 漂移 Decision 登记；路线图 M4.4/M4.5 done）
- [ ] 已运行验证：`mvn test -pl module-finance/erp-fin-service -am` + Closure 时 `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`
- [ ] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### docStatus dict 残余值裁剪

- Classification: `watch-only residual`
- Why Not Blocking Closure: `erp-fin/expense-claim-status` dict 含 SUBMITTED/APPROVED/REJECTED 但代码不写（workflow 轴 = approveStatus）。Bean 不纳入残余值。裁剪 dict 属 dict 治理，需独立 ask-first（保护区域：不改 ORM/dict）。
- Successor Required: yes（触发条件 = dict 治理统一清理残余值时）

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
