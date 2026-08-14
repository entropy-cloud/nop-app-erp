# 2026-08-14-0456-1-erpfin-reconciliation-baddebt-state-machine-beans 财务域 ErpFinReconciliation.docStatus + ErpFinBadDebt.approveStatus 实体级状态机 Bean（M4.3 + M4.10）

> Plan Status: active
> Review Hold: §11.2 M4 (i) plan-first 人工/owner-doc 门控**已于 2026-08-14 经人工确认解除**（见 Draft Review Record 门控确认记录）（Reconciliation post/reverse 触发 ArApItem 联动；BadDebt approve 触发 BAD_DEBT_WRITE_OFF/RECOVERY 凭证 + ArApItem 对称回滚）。门控非起草者/审查者可自主解除——经人工确认解除；已转 `active` 进入实施。
> Last Reviewed: 2026-08-14
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.3（ErpFinReconciliation.docStatus）+ M4.10（ErpFinBadDebt.approveStatus），均 plan-first；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md` FIN-5（:153）+ FIN-23（:167）+ M4 表展开（:275,:282）+ 风险展开（:432,:437）
> Related: M4 同域先例 `2026-08-13-2045-3-erpfin-voucher-state-machine-bean.md`（M4.1 Voucher done，docStatus 轴 facade 范式）+ `2026-08-13-1950-3-erpfin-budget-scenario-state-machine-beans.md`（M4.11/12 BudgetScenario done，docStatus+approveStatus 双轴）+ `2026-08-13-2045-1-erpfin-period-state-machine-bean.md`（M4.2 Period done）；M0.1 契约 + M1.3 批量迁移模板固化于 `docs/architecture/entity-state-machine-bean.md §11`
> Mission: entity-state-machine
> Work Item: M4.3 + M4.10
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。Reconciliation post 触发核销 ArApItem 联动（settled/openAmount 回写）；reverse 是 POSTED→REVERSED 红冲侧。BadDebt approve 触发 BAD_DEBT_WRITE_OFF 凭证（config-gated `erp-fin.bad-debt-write-off-require-approval`）；reverseApprove 红冲凭证 + ArApItem 对称回滚。声明 §11.2 M4 硬约束：(i) plan-first + 受保护行为人工/owner-doc 门控；(ii) 过账时序/编排/失败回退不改，继续由 Processor + `posted` 契约管理；(iii) `posted` 不入轴；(iv) 跨域副作用保留原 Processor/`I*Biz` 路径；(v) 既有红冲闭环不改。
>
> **规则 14 bundling 声明**：M4.3 + M4.10 属同一组件（同一 owner doc `docs/design/finance/state-machine.md`、同一域 `erp-fin`、同一结果表面 = 财务域二实体状态轴矩阵集中化），按指南规则 14 合并为单计划。两实体不同轴（Reconciliation docStatus 单轴、BadDebt approveStatus 单轴），分阶段落地。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md` FIN-5（:153）+ FIN-23（:167）+ 风险展开（:432,:437）+ 实仓核实。二实体状态轴分离如下。M4.1 Voucher + M4.2 Period + M4.11/12 BudgetScenario Bean 已落地 done，是本计划的**直接接线模板**。

- **ErpFinReconciliation**（M4.3 docStatus，单轴，本地 abstract 骨架）：
  - **docStatus 3 态**（`erp-fin/reconciliation-status`，`app-erp-finance.orm.xml:235-239`）：DRAFT/POSTED/REVERSED。
  - **writer（3 Processor + BizModel guard）**：`ErpFinReconciliationCreateProcessor:48` 写 DRAFT（创建初始态）；`ErpFinReconciliationPostProcessor:23` 守卫 require DRAFT + `:43` 写 POSTED；`ErpFinReconciliationReverseProcessor:19` 守卫 require POSTED + `:26` 写 REVERSED。`ErpFinReconciliationBizModel:98` previewReverse 守卫读 POSTED。
  - **固定守卫在 Processor 本地**：PostProcessor `:23` 内联 `RECON_STATUS_DRAFT.equals(head.getDocStatus())` 判断；ReverseProcessor `:19` 内联 `RECON_STATUS_POSTED.equals(...)`。`AbstractErpFinReconciliationProcessor:164-168` `statusError()` helper 抛 `ERR_RECONCILIATION_STATUS_INVALID`（`ErpFinErrors:118-119`，参数 reconciliationId + docStatus）。BizModel `:196-200` 有 `statusError()` 重复副本。
  - **无 `@Inject` 任何 SM Bean**——与 M4.1 已迁移的 Voucher（`ErpFinVoucherDocumentStateMachine` + VoucherProcessor 注入）形成对比。
  - **领域错误码**：`ERR_RECONCILIATION_STATUS_INVALID`（`erp.err.fin.reconciliation.status-invalid`，`ErpFinErrors:118-119`，参数 `ARG_RECONCILIATION_ID` + `ARG_DOC_STATUS`）。
  - **既有测试**：`TestErpFinReconciliation`（断言 post 后 POSTED :69、reverse 后 REVERSED :144、negative :164）+ `TestErpFinAutoReconciliation` + `TestErpFinReconciliationReversePreview` + `TestErpFinDualSideConsistency` + `TestErpFinPartnerBalance`。
  - **副作用**：post 触发 ArApItem 核销联动（`ReconciliationSettler`，beans.xml L72-73）；reverse 回写 ArApItem openAmount。Reconciliation 无 `posted` boolean 列（仅有 `postedBy`/`postedAt` 审计字段），核销状态经 `docStatus` 表达。
  - **无矩阵测试**（仅集成覆盖）。

- **ErpFinBadDebt**（M4.10 approveStatus，单轴，monolithic facade 骨架）：
  - **approveStatus 4 态**（`wf/approve-status` 平台 dict，4 值 UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）。**注意**：ORM 列名为 `approvalStatus`（单 p，`app-erp-finance.orm.xml:1688`，`ext:dict="wf/approve-status"`），Bean 命名用 `Approval` 后缀（§1 约定）。
  - **writer（monolithic facade `ErpFinBadDebtProcessor` 402 行）**：`:321` newBadDebt 写 UNSUBMITTED（初始）；`:244` doSubmit 写 SUBMITTED；`:154` approveInternal 写 APPROVED；`:249` doReject 写 REJECTED；`:141` executeReverseApprove 写 REJECTED（逆转 APPROVED）。另有 2 auto-approve 旁路：`ErpFinBadDebtWriteOffProcessor:25` 写 APPROVED（config `erp-fin.bad-debt-write-off-require-approval=false` 时）；`ErpFinBadDebtRecoverProcessor:25` 同理。
  - **固定守卫在 facade**：`ErpFinBadDebtProcessor` `validateTransitionForSubmit():218-223`（require UNSUBMITTED）、`validateTransitionForApprove():225-231`（require SUBMITTED or UNSUBMITTED）、`validateTransitionForReject():233-239`（require SUBMITTED or UNSUBMITTED）。`illegalTransition():369-374` 抛 `ERR_BAD_DEBT_ILLEGAL_APPROVAL_TRANSITION`。
  - **per-mutation stubs（关键架构事实）**：`ErpFinBadDebtSubmitForApprovalProcessor`/`ApproveProcessor`/`RejectProcessor`/`ReverseApproveProcessor` 继承 common `Abstract*Processor<ErpFinBadDebt>`，但**覆盖 main entry 委托回 monolithic `ErpFinBadDebtProcessor`**——它们的 `setApproveStatus`/`getApproveStatus`/status 枚举 hook 为 dead-code stub（标注 `// not reached`）。故 common `AbstractApproveProcessor` 转换守卫路径**当前被旁路**，所有真实转换经 facade。Bean 接入点是 facade 的 `validateTransitionFor*` 方法。
  - **领域错误码**：`ERR_BAD_DEBT_ILLEGAL_APPROVAL_TRANSITION`（`erp.err.fin.bad-debt.illegal-approval-transition`，`ErpFinErrors:362-364`，参数 `ARG_BAD_DEBT_CODE` + `ARG_CURRENT_STATUS` + `ARG_EXPECTED_STATUS`）；`ERR_BAD_DEBT_NOT_APPROVED_OR_NOT_POSTED`（`ErpFinErrors:227-229`，`ErpFinBadDebtReverseApproveProcessor:28` 终态守卫）。
  - **既有测试**：`TestErpFinBadDebt`（:157 断言 auto-approve APPROVED）+ `TestErpFinBadDebtReversal`（:106,:169 断言 reverseApprove REJECTED）+ `TestErpFinBadDebtProvisionReversal`。
  - **副作用**：approve 触发 BAD_DEBT_WRITE_OFF 凭证（config-gated `erp-fin.bad-debt-write-off-require-approval`）；reverseApprove 红冲凭证 + ArApItem 对称回滚（`ErpFinBadDebtProcessor:87-110` 代码注释标注会计保护区）。
  - **无矩阵测试**。

- **既有 Bean 注册**：`_vfs/erp/fin/beans/app-service.beans.xml`——Voucher SM Bean（L379-380）、BudgetScenario Document+Approval SM Bean（L386-389）、AccountingPeriod SM Bean（L395-396）已注册。Reconciliation 4 Processor（L364-371）+ BadDebt facade（L165-166）+ per-mutation 4 Processor（L268-275）+ WriteOff/Recover Processor（L304-307）已注册。**Reconciliation/BadDebt SM Bean 未注册**（greenfield）。
- **M4.1 接线模板（直接范本）**：`ErpFinVoucherDocumentStateMachine`（146 行）严格无状态，`assertCanPost(DRAFT)` + `postVoucherTargetStatus()`，非法边抛 common `ERR_ILLEGAL_STATUS_TRANSITION` + `action`/`fromStatus` 参数；VoucherProcessor try/catch common 码 → cause-chain 领域码。
- **common 层非法迁移码**：`ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（`nop.err.erp.common.illegal-status-transition`），M4.1/M4.2/M4.11-12 已复用。
- **合规基线**：`docs/audits/compliance-baseline.md` R5=0、R11=0。
- **owner doc 覆盖**：`docs/design/finance/state-machine.md` 当前**仅覆盖 Voucher（对象一）+ AccountingPeriod（对象二）**——**无 Reconciliation 章节、无 BadDebt 章节**。Phase 2 四方对照须以代码为权威建立语义，并将 owner doc 缺口作为 finding 登记（Decision 裁定补 owner doc 章节，对齐 maintenance SparePartUsage 先例 `2026-08-14-0930-3` + inventory StockTake 先例 `2026-08-13-2045-2` Deferred）。

## Goals

- 为 2 个财务实体的各 1 条状态轴落地一个实体级 `ErpFin*StateMachine` Bean（一 Bean 对一实体一轴），承载命名动作迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态。**直接镜像 M4.1 `ErpFinVoucherDocumentStateMachine` + M4.11/12 BudgetScenario 范式**。
  - `ErpFinReconciliationDocumentStateMachine`（docStatus 单轴，post DRAFT→POSTED、reverse POSTED→REVERSED）
  - `ErpFinBadDebtApprovalStateMachine`（approveStatus 单轴，submit UNSUBMITTED→SUBMITTED、approve SUBMITTED/UNSUBMITTED→APPROVED、reject SUBMITTED/UNSUBMITTED→REJECTED、reverseApprove APPROVED→REJECTED）
- 将固定来源态/目标态判断改调 Bean：Reconciliation Processor + BadDebt facade `validateTransitionFor*` 内联 `Objects.equals` 守卫 → Bean `assertCanXxx`（try/catch common 码 → cause-chain 领域码），目标态改调 `*TargetStatus()`。**动态业务守卫与副作用保留原位**（ArApItem 核销联动、凭证过账、reverseApprove 红冲、config-gate、SoD 审批人≠创建人）。
- 层 2 四方对照（dict ↔ owner-doc 迁移图 ↔ Bean 元数据 ↔ 全部 writer）逐实体裁定；owner doc 缺口须 Decision 裁定补章节。
- 新增层 1 矩阵完备性表驱动测试（greenfield，2 Bean）；层 3 既有集成测试全绿回归。
- 保持全部既有外部行为不变（错误码值/参数、ArApItem 联动、过账时序/失败回退、config-gate）。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml。
- 不迁移 `posted`（§11.2 M4 (iii)）；过账编排保留在 Processor 原位。
- 不修改共享骨架 `Abstract*Processor`（module-common-service 零改动）——BadDebt per-mutation stubs 当前已旁路 common 骨架，Bean 接入 facade 而非骨架。
- 不改变 config-gate（`erp-fin.bad-debt-write-off-require-approval` 保持）。
- 不重构 BadDebt per-mutation stubs（submit/approve/reject/reverseApprove 继续委托 facade——这是既有 Pattern B 设计，非本计划范围）。
- 不改变 writeOff/recover auto-approve 旁路（config-gated 初始写入路径，§9.2 选项 c）。
- 不引入全局 CRUD 写锁（M0.1 successor）。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）。
- 不证 Delta 覆盖（M4 保护域单项，归 M5.3）。
- 不迁移 Reconciliation `ErpFinBankReconciliation`（银行对账，独立实体，非核销单）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M0.2 清单 + M1.3 模板 §11 + **M4.1/M4.11-12 同域直接范本**；落地 2 个单实体单轴 Bean + Processor/facade 接线 + 测试 + 四方对照；不改契约/模型/公共 API/共享骨架。**M4 plan-first**——post/approve/reverse 触发业财过账）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 模板 + §11.2 M4 变体 + §1 单轴命名）、`docs/design/finance/state-machine.md`（§Voucher + §Period + owner doc 缺口 Reconciliation/BadDebt）、`docs/design/finance/ar-ap-reconciliation.md`（核销语义）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（FIN-5/FIN-23）、`docs/architecture/processor-extension-pattern.md`、`docs/skills/state-machine-business-review-prompt.md`、`docs/plans/2026-08-13-2045-3-erpfin-voucher-state-machine-bean.md`（M4.1 同域直接范本）
- Skill Selection Basis: 路线图 M4.3/M4.10 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「Processor/facade 接线、Bean 注册、`@Inject` 非 private、cause-chaining 错误码、过账副作用保留、产品化可定制性自检」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成测试回归」。`state-machine-business-review-prompt.md` 匹配层 2 四方对照。M4.1/M4.11-12 范本可直接镜像，必需输入均已就绪。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护财务过账行为（Reconciliation post 触发 ArApItem 核销联动；BadDebt approve 触发 BAD_DEBT_WRITE_OFF 凭证；reverseApprove 红冲凭证 + ArApItem 对称回滚）。在人工/owner-doc 确认前为阻塞前置。
- 无端口/环境变量/CORS/密钥/.env/外部服务依赖。无数据迁移。

## Execution Plan

### Phase 1 - ErpFinReconciliation docStatus Bean（M4.3）

Status: planned
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/statemachine/ErpFinReconciliationDocumentStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpFinReconciliationPostProcessor.java`、`.../processor/ErpFinReconciliationReverseProcessor.java`、`.../processor/AbstractErpFinReconciliationProcessor.java`、`.../entity/ErpFinReconciliationBizModel.java`、`.../test/.../statemachine/TestErpFinReconciliationStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done（已满足）；M4.1 `ErpFinVoucherDocumentStateMachine` 范本已 done

- [ ] `Add`：落地 `ErpFinReconciliationDocumentStateMachine` Bean——2 动作矩阵（post DRAFT→POSTED、reverse POSTED→REVERSED）+ `assertCanPost(String docStatus)` + `assertCanReverse(String docStatus)` + `postTargetStatus()`/`reverseTargetStatus()` + `isTerminal`/`initialStatuses`/`terminalStatuses` + `transitions()`（2 边）。严格无状态（§2）。非法边抛 common 码 `ERR_ILLEGAL_STATUS_TRANSITION` + `action`/`currentStatus`/`expectedStatus` 参数。直接镜像 `ErpFinVoucherDocumentStateMachine` 结构（M4.1 范本）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：在 `_vfs/erp/fin/beans/app-service.beans.xml` 以 `<bean id="<FQN>" class="<FQN>"/>` 注册（紧邻既有 Voucher SM Bean L379-380）。
  - Skill: `nop-backend-dev`
- [ ] `Add`（接线，镜像 M4.1 Voucher 范式）：PostProcessor `:23` 守卫改调 `stateMachine.assertCanPost(docStatus)`（try/catch common 码 → cause-chain `statusError()` 领域码 `ERR_RECONCILIATION_STATUS_INVALID`）；`:43` 目标态改调 `stateMachine.postTargetStatus()`。ReverseProcessor `:19` 守卫同理改调 `assertCanReverse`；`:26` 目标态改调 `reverseTargetStatus()`。BizModel previewReverse `:98` 守卫改调 Bean `assertCanReverse`（一致性）。`AbstractErpFinReconciliationProcessor:164-168` + BizModel `:196-200` 的 `statusError()` 重复副本统一路由 cause-chain。ArApItem 核销联动 + `posted` 编排保留原位。
  - Skill: `nop-backend-dev`
- [ ] `Decision`（owner doc 缺口）：finance/state-machine.md 当前无 Reconciliation 章节。四方对照须以代码为权威建立语义（post DRAFT→POSTED + reverse POSTED→REVERSED），并 Decision 裁定补 owner doc Reconciliation 章节（对齐 maintenance SparePartUsage 先例 + inventory StockTake 先例）。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] `Proof`：层 1 矩阵完备性（greenfield 表驱动，镜像 `TestErpFinVoucher*Matrix` 范式）——(a) 无重复/冲突边；(b) post DRAFT→POSTED、reverse POSTED→REVERSED 可达；(c) 各 `assertCanXxx` 合法来源态通过、非法来源态抛 common 码携带 `action`/`fromStatus`；(d) `transitions()` 与显式方法语义一致；(e) 初始={DRAFT}/终态={REVERSED}（POSTED 为中间态）。
  - Skill: `nop-testing`
- [ ] `Proof`：层 2 四方对照——dict `erp-fin/reconciliation-status`（3 值）↔ owner-doc 迁移图（待补 Reconciliation 章节）↔ Bean 元数据 ↔ 全部 writer（Create 写 DRAFT + Post 写 POSTED + Reverse 写 REVERSED + BizModel previewReverse 守卫 + CRUD 路径排除）。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] `ErpFinReconciliationDocumentStateMachine` Bean 存在、已注册、严格无状态；Post/Reverse Processor 委托 Bean，内联 `Objects.equals` 矩阵判断已移除。
- [ ] Reconciliation 层 1 矩阵测试本地 `mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinReconciliationStateMachineMatrix` 全绿。

### Phase 2 - ErpFinBadDebt approveStatus Bean（M4.10）

Status: planned
Targets: `.../statemachine/ErpFinBadDebtApprovalStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpFinBadDebtProcessor.java`、`.../processor/ErpFinBadDebtReverseApproveProcessor.java`、`.../test/.../statemachine/TestErpFinBadDebtStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（Reconciliation Bean + 接线范式已固化）

- [ ] `Decision`（BadDebt facade 接入点裁决）：(A) **per-mutation stubs 旁路 common 骨架**——submit/approve/reject/reverseApprove Processor 覆盖 main entry 委托 facade `ErpFinBadDebtProcessor`，common `AbstractApproveProcessor` 转换守卫路径被旁路。Bean 接入点 = facade 的 `validateTransitionForSubmit():218-223` / `validateTransitionForApprove():225-231` / `validateTransitionForReject():233-239` + `executeReverseApprove:141` 状态守卫，不接入 per-mutation stubs（它们是 dead-code）。(B) **writeOff/recover auto-approve 旁路**（`ErpFinBadDebtWriteOffProcessor:25` / `RecoverProcessor:25`，config-gated）= §9.2 选项 c 初始/生成写入路径，不经 Bean `assertCanApprove`（与 Voucher 生成路径先例一致，Bean 不覆盖）。(C) **ORM 列名 `approvalStatus`（单 p）vs Bean 命名 `Approval`（双 p）**——按 §1 约定 Bean 用 `Approval` 后缀对齐 `wf/approve-status` dict 语义，ORM 列名不改（非本计划保护区）。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] `Add`：落地 `ErpFinBadDebtApprovalStateMachine` Bean——4 动作矩阵（submit UNSUBMITTED→SUBMITTED、approve {SUBMITTED,UNSUBMITTED}→APPROVED、reject {SUBMITTED,UNSUBMITTED}→REJECTED、reverseApprove APPROVED→REJECTED）+ `assertCanSubmit/Approve/Reject/ReverseApprove(String approvalStatus)` + 对应 `*TargetStatus()` + `isTerminal`/`initialStatuses`/`terminalStatuses` + `transitions()`。非法边抛 common 码。镜像 M4.12 `ErpFinBudgetScenarioApprovalStateMachine` 范式。
  - Skill: `nop-backend-dev`
- [ ] `Add`：在 `_vfs/erp/fin/beans/app-service.beans.xml` 注册（紧邻既有 BudgetScenario Approval SM Bean L388-389）。
  - Skill: `nop-backend-dev`
- [ ] `Add`（接线）：facade `ErpFinBadDebtProcessor` 注入 `@Inject ErpFinBadDebtApprovalStateMachine`（非 private）；`validateTransitionForSubmit/Approve/Reject` 改调 Bean `assertCanXxx`（try/catch common 码 → cause-chain `illegalTransition()` 领域码 `ERR_BAD_DEBT_ILLEGAL_APPROVAL_TRANSITION`）；`doSubmit:244`/`approveInternal:154`/`doReject:249` 目标态改调 Bean `*TargetStatus()`；`executeReverseApprove:141` 守卫改调 `assertCanReverseApprove` + 目标态改调 `reverseApproveTargetStatus()`。`ErpFinBadDebtReverseApproveProcessor:28` 终态守卫 `ERR_BAD_DEBT_NOT_APPROVED_OR_NOT_POSTED` 保留原位（含 posted 判定，动态业务守卫）。BAD_DEBT_WRITE_OFF 凭证过账 + ArApItem 对称回滚 + config-gate + SoD 保留原位。
  - Skill: `nop-backend-dev`
- [ ] `Decision`（owner doc 缺口）：finance/state-machine.md 当前无 BadDebt 章节。四方对照须以代码为权威建立语义（4 动作 + auto-approve 旁路），并 Decision 裁定补 owner doc BadDebt 章节。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] `Proof`：层 1 矩阵完备性 + 层 2 四方对照（dict `wf/approve-status` ↔ owner-doc ↔ Bean ↔ 全部 writer 含 facade 5 写入点 + auto-approve 旁路 2 写入点 + CRUD 路径排除）。
  - Skill: `nop-testing` + `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] `ErpFinBadDebtApprovalStateMachine` Bean 存在/注册/无状态；facade `validateTransitionFor*` 委托 Bean，内联 `Objects.equals` 矩阵判断已移除。
- [ ] BadDebt 层 1 矩阵测试本地 `mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinBadDebtStateMachineMatrix` 全绿。

### Phase 3 - 层 3 既有命名动作回归

Status: planned
Targets: `module-finance/erp-fin-service/src/test/`（既有集成测试，零新建）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1-2（二实体 2 轴 Bean + 接线已落地）

- [ ] `Proof`：层 3 既有命名动作回归——复用 `TestErpFinReconciliation`（post/reverse happy path + negative）、`TestErpFinBadDebt`（auto-approve）、`TestErpFinBadDebtReversal`（reverseApprove REJECTED + 红冲凭证）、`TestErpFinAutoReconciliation`、`TestErpFinDualSideConsistency`、`TestErpFinPartnerBalance`，证明 Processor 写回、ArApItem 核销联动、过账副作用时序、config-gate 不变。本地 `mvn test -pl module-finance/erp-fin-service -am` 全绿。
  - Skill: `nop-testing`
- [ ] `Proof`：二轴一致性复核——2 Bean 命名（Document/Approval 后缀）/注册（同文件紧邻 Voucher/BudgetScenario Bean）/无状态/元数据形状一致；Processor/facade→Bean 注入 + cause-chaining 范式与 M4.1 Voucher 可追溯一致。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 层 3 既有集成测试全绿（零行为回归）。

## Draft Review Record

- Independent draft review iteration 1: `acceptable-as-draft` (`ses_00313a530ffeDZNpkXyeh2SSVR`) — 零信任实仓核实 24 项 baseline 声明，23/24 pass。无 BLOCKER / MAJOR。2 MINOR 已修正：(1) Reconciliation 实体无 `posted` boolean 列（仅有 postedBy/postedAt 审计字段），已修正副作用描述；(2) Reconciliation 守卫实为 `CONST.equals(...)` 非 `Objects.equals()`，已修正 API 描述。BadDebt facade 所有行号 + per-mutation stub dead-code + beans.xml 注册 + 错误码 + 测试全 pass。§11.2 M4 治理 + 规则 14 bundling + anti-slack + Deferred 诚实性均 pass。**Review Hold 确认成立且不可自解**：§11.2 M4 (i) 经核真实存在，触及核销 ArApItem + 坏账凭证 + 红冲保护域（project-context 会计/财务硬停止），与同批 M4 计划 hold 模式一致。保持 `Plan Status: draft` + Review Hold。
- Mission-driver review iteration 2: `approved (held as draft)` — 复核四项检查清单全 pass：(1) 格式合规——模板必选段齐全、字段名正确、Phase 结构有效；(2) 完整性——各阶段 Exit Criteria 可观测可测（具体 Bean 存在/无状态断言 + 精确本地化 `mvn test -Dtest=...` 命令；Phase 3 回归断言）；执行计划覆盖全部检查项；Closure Gates 枚举证据；(3) 范围——规则 14 bundling 合规（同 owner doc/同域/同结果表面），Non-Goals 完备无 scope creep，阶段退出未重复全仓验证（指南规则 7）；(4) 结束证据——结构正确，占位符待执行。无 BLOCKER/MAJOR。§11.2 M4 (i) plan-first 门控为外部依赖（需人工/owner-doc 确认，project-context.md 会计/财务硬停止），非计划缺陷，审查时不可自解。按 holding 协议保持 `Plan Status: draft` + Review Hold；`approved` 标记报告审查已运行。
- Mission-driver review iteration 4: `approved (held as draft)` (本次) — 零信任复核四项检查清单 + 实仓核验 baseline。(1) **格式合规**：模板必选段齐全（front matter Plan Status/Last Reviewed/Source/Related/Audit + Mission/Work Item/Review Hold 与同批 M4 一致、Current Baseline、Goals、Non-Goals、Task Route、Infrastructure And Config Prereqs、Execution Plan ×3 Phase、Draft Review Record、Closure Gates、Deferred But Adjudicated ×4、Closure），字段名正确，Phase 结构有效（Status/Targets/Skill/Item Types/Prereqs/checkbox/Exit Criteria 齐备）。(2) **完整性**：三阶段 Exit Criteria 可观测可测——Phase 1/2 给出 Bean 存在/无状态/委托断言 + 精确本地化 `mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFin*StateMachineMatrix` 命令；Phase 3 给出零行为回归断言。执行计划覆盖全部检查项（每实体：Add Bean→注册→接线→Decision owner-doc 缺口→Proof 矩阵→Proof 四方对照）。Closure Gates 枚举 9 项证据含完整验证命令（`mvn clean install -DskipTests` + 本地 service test + compliance checker actual ≤ baseline）。(3) **范围**：规则 14 bundling 合规——同 owner doc `docs/design/finance/state-machine.md`、同域 `erp-fin`、同结果表面（财务域二实体状态轴矩阵集中化），两实体分阶段落地。Non-Goals 10 项完备无 scope creep。阶段退出未重复全仓验证（指南执行规则 7）。(4) **结束证据**：Closure 结构正确（Status Note/Closure Audit Evidence/Follow-up 占位待执行）。**实仓核验**：`statemachine/` 目录已含 M4.1/M4.2/M4.11-12 先例 Bean（ErpFinVoucherDocumentStateMachine/ErpFinAccountingPeriodStateMachine/ErpFinBudgetScenario{Document,Approval}StateMachine）+ 既有矩阵测试 3 个证实「直接范本」声明；目标 Processor（ErpFinReconciliationPostProcessor/ReverseProcessor + ErpFinBadDebtProcessor 及 per-mutation 4 Processor）存在；beans.xml 实仓在 `module-finance/erp-fin-service/src/main/resources/_vfs/erp/fin/beans/app-service.beans.xml`（非 erp-fin-web，Phase Targets 的 `.../beans/app-service.beans.xml` 缩写从已建立的 `module-finance/erp-fin-service/` 基底正确解析）；引用技能 `docs/skills/state-machine-business-review-prompt.md` 存在；§11.2 M4 (i) plan-first 硬约束实仓核实于 `entity-state-machine-bean.md:283`。**Review Hold 确认成立且不可自解**：触及 Reconciliation post→ArApItem 核销联动、BadDebt approve→BAD_DEBT_WRITE_OFF 凭证、reverseApprove→红冲凭证+ArApItem 对称回滚，均属受保护财务过账行为（project-context.md:68 会计/财务保护区域硬停止），需人工/owner-doc 确认——审查者不可自解。无 BLOCKER/MAJOR。按 holding 协议保持 `Plan Status: draft` + Review Hold；`approved` 标记报告审查已运行。
- Mission-driver review iteration 3: `approved (held as draft)` — 零信任复核四项检查清单 + 实仓核验 baseline 声明。(1) **格式合规**：模板必选段齐全（Plan Status/Last Reviewed/Source/Related/Audit、Current Baseline、Goals、Non-Goals、Task Route、Infrastructure And Config Prereqs、Execution Plan ×3 Phase、Draft Review Record、Closure Gates、Deferred But Adjudicated、Closure），字段名正确，Phase 结构有效（Status/Targets/Skill/Item Types/Prereqs/checkbox/Exit Criteria 齐备）。额外前置字段（Mission/Work Item/Review Hold）与同批 M4 计划一致，模板未禁止。(2) **完整性**：三阶段 Exit Criteria 均可观测可测——Phase 1/2 给出 Bean 存在/无状态/委托断言 + 精确本地化 `mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFin*StateMachineMatrix` 命令；Phase 3 给出零行为回归断言。执行计划覆盖全部检查项（每实体：Add Bean→注册→接线→Decision owner-doc 缺口→Proof 矩阵→Proof 四方对照）。Closure Gates 枚举 9 项证据含完整验证命令。(3) **范围**：规则 14 bundling 合规——同 owner doc `docs/design/finance/state-machine.md`、同域 `erp-fin`、同结果表面（财务域二实体状态轴矩阵集中化），两实体分阶段落地。Non-Goals 10 项完备（不改 model/api/dict、不迁移 posted、不改共享骨架/Abstract*Processor、不改 config-gate、不重构 BadDebt stubs、不改 auto-approve 旁路、不引全局写锁、不自主跳过 M4 门控、不证 Delta、不迁移 BankReconciliation），无 scope creep。阶段退出未重复全仓验证（指南执行规则 7）。(4) **结束证据**：Closure 结构正确（Status Note/Closure Audit Evidence/Follow-up 占位待执行）。**实仓核验**：`statemachine/` 目录已含 M4.1/M4.2/M4.11-12 先例 Bean（ErpFinVoucherDocumentStateMachine/ErpFinAccountingPeriodStateMachine/ErpFinBudgetScenario{Document,Approval}StateMachine）证实「直接范本」声明；目标 Processor（ErpFinReconciliationPostProcessor/ErpFinBadDebtProcessor）存在；引用技能 `docs/skills/state-machine-business-review-prompt.md` 存在。**Review Hold 确认成立且不可自解**：§11.2 M4 (i) 实仓核实于 `entity-state-machine-bean.md:283`（plan-first 硬约束「触及受保护行为（过账/红冲/结账）时不因 StateMachine Bean 抽象而免除人工/owner-doc 门控」）+ `ai-autonomy-policy.md:72`（`accounting/finance postings | plan-first`）+ `project-context.md:68`（会计/财务保护区域硬停止）。本计划触及 Reconciliation post→ArApItem 核销联动、BadDebt approve→BAD_DEBT_WRITE_OFF 凭证、reverseApprove→红冲凭证+ArApItem 对称回滚，均属受保护财务过账行为，需人工/owner-doc 确认——审查者不可自解。无 BLOCKER/MAJOR。按 holding 协议保持 `Plan Status: draft` + Review Hold；`approved` 标记报告审查已运行。
- Mission-driver review iteration 5: `approved (held as draft)` (本次) — 零信任复核四项检查清单 + 实仓抽样核验 baseline。(1) **格式合规**：模板必选段齐全（front matter Plan Status/Last Reviewed/Source/Related/Audit + Mission/Work Item/Review Hold、Current Baseline、Goals、Non-Goals、Task Route、Infrastructure And Config Prereqs、Execution Plan ×3 Phase、Draft Review Record、Closure Gates、Deferred But Adjudicated ×4、Closure），字段名正确，Phase 结构有效（Status/Targets/Skill/Item Types/Prereqs/checkbox/Exit Criteria 齐备）。(2) **完整性**：Phase 1/2 Exit Criteria 给出 Bean 存在/无状态/委托断言 + 精确本地化 `mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFin{Reconciliation,BadDebt}StateMachineMatrix` 命令；Phase 3 零行为回归断言。执行计划覆盖全部检查项（每实体 Add→注册→接线→Decision→Proof 闭环）。Closure Gates 9 项枚举证据含 `mvn clean install -DskipTests` + compliance checker。(3) **范围**：规则 14 bundling 合规（同 owner doc `docs/design/finance/state-machine.md`、同域 `erp-fin`、同结果表面，两实体分阶段落地）；Non-Goals 10 项无 scope creep；阶段退出未重复全仓验证（指南执行规则 7）。(4) **结束证据**：Closure 结构正确（Status Note/Closure Audit Evidence/Follow-up 占位待执行）。**实仓核验**：`statemachine/` 已含 4 先例 Bean（ErpFinVoucherDocumentStateMachine/ErpFinAccountingPeriodStateMachine/ErpFinBudgetScenario{Document,Approval}StateMachine）证实「直接范本」声明；目标 Processor 全存在（Reconciliation Create/Post/Reverse + AbstractErpFinReconciliationProcessor + ErpFinBadDebtProcessor facade + per-mutation 4 Processor + WriteOff/Recover Processor）；beans.xml 实仓位于 `module-finance/erp-fin-service/src/main/resources/_vfs/erp/fin/beans/app-service.beans.xml`（ReconciliationSettler L72-73 实仓核实）；roadmap M4.3（:99）/M4.10（:106）均 `todo` + plan-first + owner doc `finance/state-machine.md`，且**不在 2026-08-13 人工确认解除门控清单内**（清单含 M4.1/M4.2/M4.11-12/M4.14... 等，M4.3/M4.10 未列）。**Review Hold 确认成立且不可自解**：§11.2 M4 (i) 实仓核实于 `entity-state-machine-bean.md:283`（「触及受保护行为（过账/红冲/结账）时不因 StateMachine Bean 抽象而免除人工/owner-doc 门控」）+ `ai-autonomy-policy.md:72`（`accounting/finance postings | plan-first | owner doc + tests`）+ `project-context.md:68`（会计/财务保护区域硬停止）。本计划触及 Reconciliation post→ArApItem 核销联动、BadDebt approve→BAD_DEBT_WRITE_OFF 凭证、reverseApprove→红冲凭证+ArApItem 对称回滚，均属受保护财务过账行为，需人工/owner-doc 确认——审查者不可自解（与同批 M4 batch-consistent hold 一致）。无 BLOCKER/MAJOR。按 holding 协议保持 `Plan Status: draft` + Review Hold；`approved` 标记报告审查已运行（held as draft 非可执行状态，待人工门控解除后 promotion 至 active）。
- **M4 plan-first 人工/owner-doc 门控状态：confirmed（2026-08-14 人工确认解除）**（§11.2 M4 (i)）。人工/owner 于 2026-08-14 确认「以行为保持的矩阵集中化方式迁移二实体 2 轴、Reconciliation post/reverse ArApItem 联动 + BadDebt approve BAD_DEBT_WRITE_OFF/RECOVERY 凭证 + ArApItem 对称回滚完整保留」可接受，门控解除。据此将 Plan Status 由 `draft` 转 `active`。

## Closure Gates

- [ ] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)）
- [ ] 范围内行为完成（二实体 2 轴 Bean + Processor/facade 接线 + 层 1 矩阵 + 层 2 四方对照 + 层 3 回归）
- [ ] 相关文档对齐（roadmap M4.3/M4.10 → done；finance/state-machine.md 补 Reconciliation + BadDebt 章节）
- [ ] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl module-finance/erp-fin-service -am` 全绿 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### BadDebt per-mutation stubs 旁路 common 骨架

- Classification: `watch-only residual (intentional legacy Pattern B)`
- Why Not Blocking Closure: submit/approve/reject/reverseApprove per-mutation Processor 覆盖 main entry 委托 facade，common `AbstractApproveProcessor` 守卫路径被旁路（dead-code stubs）。这是既有 Pattern B 设计（per-mutation-processor-split-plan.md :253 裁定）。Bean 接入 facade 而非骨架，不改变此架构。
- Successor Required: no（仅当重构 BadDebt 为 Pattern A 时重开）

### writeOff/recover auto-approve 旁路

- Classification: `watch-only residual (intentional legacy config-gate)`
- Why Not Blocking Closure: config `erp-fin.bad-debt-write-off-require-approval=false` 时 WriteOff/Recover Processor 直接写 APPROVED（§9.2 选项 c 生成写入路径，不经 Bean `assertCanApprove`），与 Voucher 生成路径先例一致。
- Successor Required: no（仅当 PM 要求 writeOff/recover 必须经审批时重开）

### Delta 覆盖运行时实证

- Classification: `optimization candidate`
- Why Not Blocking Closure: M4 保护域单项 Delta 可选；归 M5.3。
- Successor Required: yes（触发条件 = M5.3 最终跨域 Delta 覆盖回归）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 裁定选项 (c) 显式排除；更强写锁须改 ORM/xmeta（保护区 ask-first）。
- Successor Required: no

## Closure

Status Note: _待执行后填写_

Closure Audit Evidence:

- Auditor / Agent: _待执行后填写_
- Evidence: _待执行后填写_

Follow-up:

- <待执行后填写；Deferred 项均为既定 successor>
