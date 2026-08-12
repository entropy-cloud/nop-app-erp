# 2026-08-13-0945-2-sales-approvestatus-state-machine-bean 销售单据 approveStatus 审批轴 StateMachine Bean 迁移（M3.6–M3.7）

> Plan Status: active
> Last Reviewed: 2026-08-13
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M3.6 / M3.7（均 todo）
> Related: 前置 `2026-08-12-0918-2-sales-docstatus-state-machine-bean.md`（M2.9–M2.10 done，其 Related 显式「本计划解阻 M3.6/M3.7」，触发条件已满足）；姊妹计划 `2026-08-13-0945-1-purchase-approvestatus-state-machine-bean.md`（N=1，采购同轴迁移，跨实体 Decision 同源）；M0.1 契约 + M1.3 批量迁移模板固化于 `docs/architecture/entity-state-machine-bean.md §11`
> Mission: entity-state-machine
> Work Item: M3.6 + M3.7
> Audit: required

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md`（SAL-2/20/21 行）+ 实仓核实。approveStatus 是销售单据三轴分离中的**审批轴**（`sales/state-machine.md` §三轴状态分离 + §审批轴），与 docStatus 业务生命周期轴（M2.9–M2.10 已 done）独立。本计划在 docStatus Bean 落地后接续迁移审批轴。

- **轴语义（wf/approve-status 审批轴，5 动作）**：`UNSUBMITTED`（初始态，创建时写入）→(submit)→ `SUBMITTED` →(approve)→ `APPROVED` / →(reject)→ `REJECTED`；`REJECTED` →(submit 重提)→ `SUBMITTED`；`SUBMITTED` →(withdraw)→ `UNSUBMITTED`；`APPROVED` →(reverseApprove)→ `SUBMITTED`（销售域骨架 `AbstractReverseApproveProcessor`→SUBMITTED；与采购 Quotation/Rfq xbiz→REJECTED 不同——销售 Order/Quotation 均经骨架 PROC 路径，目标态一致 SUBMITTED）。dict `wf/approve-status`（平台标准审核状态字典，与采购单据头共享）。SAL-2/20/21 八属性登记均为「纳入 / **无**财务影响 / approve 仅状态推进（可用量只读预检）」。属模板 §11「M3 审批轴」类别。5 动作 = submit/approve/reject/reverseApprove/withdraw。
- **关键澄清（SAL-2 误读纠正）**：M2.9–M2.10 计划 Non-Goal 注「approveStatus APPROVED 才触发过账，归 M4」指的是 **Delivery/Invoice/Receipt/Return** 审批轴（M4.22/24/26/28，approve→出库/凭证，**是**财务影响）；**Order/Quotation 的 approveStatus 不触发过账**（M0.2 SAL-2/20/21 明确裁定「approve 仅状态推进」「可用量只读预检」「无库存/凭证」）。Order approve 的 commitment-commit / intercompany-approve 是 **approve 动作的动态跨域副作用**（保留 Processor 原位），可用量预检为只读守卫（保留原位），不改变审批轴矩阵本身的无财务影响分类。
- **固定迁移判断当前所在位置（实仓核实）**：审批四动作（submit/approve/reverseApprove/withdraw）的固定来源态/目标态守卫在共享骨架 `module-common-service/.../Abstract{SubmitForApproval,Approve,Reject,ReverseApprove,Withdraw}Processor.validateTransitionForXxx`（`AbstractApproveProcessor.validateTransitionForApprove:37-42` 内联 `Objects.equals(status, submittedStatus())` → 抛 `illegalStatusException`；`doApprove:49` `setApproveStatus(approvedStatus())`）。这是本计划替换为 Bean 调用的固定判断。`module-common-service` 零改动（与采购 N=1 计划一致，迁移经各域 per-mutation Processor 覆写委托）。
- **逐实体 writer 盘点（实仓核实，均 PROC 路径，预期无漂移发现项）**：
  - **M3.7 ErpSalOrder（PROC 路径，5 Processor）**：`ErpSalOrder{SubmitForApproval,Approve,Reject,ReverseApprove,Withdraw}Processor` 各 extends 对应骨架（含 `AbstractRejectProcessor`）。守卫经骨架；动态业务守卫/副作用（commitment-commit/intercompany-approve/可用量只读预检）委托 facade `ErpSalOrderProcessor`，保留原位；SoD 守卫 `sodErrorCode()` 保留。领域码 `ERR_ORDER_ILLEGAL_STATUS_TRANSITION`（参数 `orderCode`/`currentStatus`/`expectedStatus`）。初始 UNSUBMITTED 由 `QuotationToOrderConverter`/创建路径写入。固定守卫 `validateTransitionForXxx` 未覆写（继承骨架，5 动作同）。
  - **M3.6 ErpSalQuotation（PROC 路径，5 Processor）**：`ErpSalQuotation{SubmitForApproval,Approve,Reject,ReverseApprove,Withdraw}Processor` 结构同 Order（领域码 `ERR_QUOTATION_ILLEGAL_STATUS_TRANSITION`，参数 `quotationCode`/...）。Quotation 无 commitment/intercompany 副作用（无库存/凭证）。
- **与采购计划 N=1 的关键差异（无 INLINE/缺失漂移）**：销售 Quotation/Order 审批**均经 4 Processor + 骨架守卫**（grep 确证 4 Processor 文件均存在），**无**采购 Quotation/Rfq 那种「零审批 Processor」漂移。本计划 layer-2 四方对照预期不产生 Fix（与销售 docStatus 计划一致——docStatus 阶段销售 Quotation/Order cancel 亦均经 CancelProcessor 无漂移）；若发现意外漂移，按规则 5 登记 + successor，禁止静默排除。
- **common 层非法迁移码已存在（参数形状已裁定）**：`ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（参数 `currentStatus`/`expectedStatus`），cs 试点 M1.1 Decision Option A + 采购 docStatus/审批计划裁定复用 + `action` 补充参数。本计划沿用。
- **Bean 命名约定（双轴预留）**：docStatus Bean 用 `Document` 后缀（`ErpSal<Entity>DocumentStateMachine`，M2.9–M2.10 已落地），本计划审批轴 Bean 用 `Approval` 后缀（`ErpSal<Entity>ApprovalStateMachine`），一 Bean 对一实体一轴（§1 双轴约定）。
- **Bean 注册范式已存在**：`module-sales/erp-sal-service/src/main/resources/_vfs/erp/sal/beans/app-service.beans.xml` 已注册既有 docStatus StateMachine Bean 与 per-mutation Processor（已核实 `ErpSalOrderDocumentStateMachine`/`ErpSalQuotationDocumentStateMachine` 注册）。
- **层 3 回归基线已存在（非 greenfield）**：销售域无既有 `TestErpSal*ApprovalStateMachine` 矩阵测试，层 1 为 greenfield；但存在覆盖审批全生命周期的集成测试 = 层 3 基线：`TestErpSalOrderApproval`、`TestErpSalQuotationCrudSmoke`、`TestErpSalQuotationToOrder`、`TestErpSalOrderToCashEnd` 等。
- **合规基线**：`docs/audits/compliance-baseline.md` R5=0、R11=0。本计划保持 R5=0、R11 不增。

## Goals

- 为销售 2 个单据实体的 approveStatus 轴各落地 `ErpSal<Entity>ApprovalStateMachine` Bean，承载 submit/approve/reverseApprove/withdraw（及 reject 若 owner doc 声明）迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态（§2）。
- 将 4 个 per-mutation Processor 的 `validateTransitionForXxx` 覆写为委托 Bean（`assertCanXxx` + `*TargetStatus()`），动态业务守卫/副作用（Order 的 commitment-commit/intercompany-approve/可用量只读预检、SoD 守卫）保留原位。
- 层 2 四方对照（dict `wf/approve-status` ↔ `sales/state-machine.md` §审批轴 ↔ Bean 元数据 ↔ 全部 writer 含 CRUD 路径）逐实体裁定，发现项按规则 5 登记。
- 新增层 1 矩阵完备性表驱动测试（greenfield，2 个 Bean 各一）；层 3 既有集成测试全绿回归。
- 保持全部既有外部行为不变（错误码值/参数/审计/SoD/commitment/intercompany/可用量预检副作用时序）。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（路线图 Non-Goal）。
- 不迁移 `docStatus` 轴（M2.9–M2.10 已 done）。
- 不迁移 `deliveryStatus`/`receivedStatus`/`writtenOffStatus`（SAL-3/4/15 等已裁定排除-技术/派生）。
- 不触碰 `posted`（业财过账/红冲契约，不作迁移轴）；Order/Quotation approve 不触发过账（触发过账的 Delivery/Invoice/Receipt/Return 审批轴归 M4.22/24/26/28，各自独立 plan-first）。
- 不修改共享骨架 `Abstract{SubmitForApproval,Approve,Reject,ReverseApprove,Withdraw}Processor`（module-common-service 零改动）。
- 不引入全局 CRUD 写锁（M0.1 §9 选项 c）。
- 不在本计划证 Delta 覆盖（M3 非保护域可选；cs 试点 M1.2 已实证机制；Delta 覆盖回归归 M5.3）。
- 不改变审批的错误码值/参数形状/审计 actionType/SoD 语义。
- 不构建反射型/泛型全局 `IStateMachine` 调度器。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M0.2 清单 + M1.3 模板 §11 + 采购 N=1 审批计划跨实体 Decision，落地 2 个单实体单轴审批 Bean + 接线 + 测试 + 四方对照）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 模板 + §1 双轴约定）、`docs/design/sales/state-machine.md`（§三轴分离 + §审批轴）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（SAL-2/20/21 八属性）、`docs/architecture/processor-extension-pattern.md`、`docs/skills/state-machine-business-review-prompt.md`、`docs/plans/2026-08-12-0918-2-sales-docstatus-state-machine-bean.md`（跨实体 Decision 同源）
- Skill Selection Basis: 路线图 M3.6/M3.7 指定 `nop-backend-dev` + `nop-testing`（理由同采购 N=1 计划）；`state-machine-business-review-prompt.md` 匹配层 2 四方对照。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯后端 Java + 既有 sal-service 测试容器）。
- 前置依赖：M1.3 done（已满足）；M3.6/M3.7 deps = M1.3 + M2.9/M2.10（均 done），门控已解除。
- 无 data-deletion / 财务过账 / ORM 保护区域触发。

## Execution Plan

### Phase 1 - ErpSalOrder approveStatus Bean（M3.7）

Status: planned
Targets: `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/statemachine/ErpSalOrderApprovalStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpSalOrder{SubmitForApproval,Approve,Reject,ReverseApprove,Withdraw}Processor.java`、`module-sales/erp-sal-service/src/test/.../TestErpSalOrderApprovalStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done + M2.10 done（已满足）

- [ ] `Add`：落地 `ErpSalOrderApprovalStateMachine` Bean（一实体一轴 approveStatus）——显式 `assertCanSubmit/Approve/Reject/ReverseApprove/Withdraw(String status)`（非法来源态 → 抛 common 码 `ERR_ILLEGAL_STATUS_TRANSITION` + `action`/`fromStatus` 补充参数；reject 来源态=SUBMITTED，与 approve 同源态）+ `submitTargetStatus/approveTargetStatus/rejectTargetStatus/reverseApproveTargetStatus/withdrawTargetStatus()` + `isTerminal`/`initialStatuses`/`terminalStatuses` + 只读 `transitions()`。严格无状态（§2）。命名带 `Approval` 后缀（§1 双轴约定）。矩阵边以 layer-2 四方对照核实的 owner doc §审批轴 + 共享骨架行为为准。
  - Skill: `nop-backend-dev`
- [ ] `Add`：在非生成 `_vfs/erp/sal/beans/app-service.beans.xml` 以 `<bean id="<FQN>" class="<FQN>"/>` 注册（§5）。
  - Skill: `nop-backend-dev`
- [ ] `Decision | Add`：跨实体接线 Decision 复用采购 N=1 计划——(A) Bean 接线点 = 5 个 per-mutation Processor（含 Reject）覆写 `validateTransitionForXxx` 委托 Bean；目标态写入委托 Bean；(B) common 码沿用 Option A；(C) 领域码映射 `ERR_ORDER_ILLEGAL_STATUS_TRANSITION`（参数 `orderCode`/...）保留；(D) 初始态 UNSUBMITTED 写入不经 Bean（§9.2 选项 c）；(E) SoD + commitment-commit/intercompany-approve/可用量只读预检保留原位。在 Order 上落地：5 Processor 注入 `@Inject ErpSalOrderApprovalStateMachine`（非 private），覆写 5 个 `validateTransitionForXxx` 调对应 `assertCanXxx`。grep 证内联 `Objects.equals` 矩阵判断已移除（动态 hook 除外）。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：层 1 矩阵完备性（greenfield 表驱动，不经 BizModel）——(a) 无重复/冲突边；(b) submit UNSUBMITTED/REJECTED→SUBMITTED、approve/reject SUBMITTED→APPROVED/REJECTED、reverseApprove APPROVED→SUBMITTED 可逆、withdraw SUBMITTED→UNSUBMITTED 可逆、终态无非法出边；(c) 各 `assertCanXxx` 合法来源态通过、非法来源态抛 common 码携带 `action`/`fromStatus`；(d) `transitions()` 与显式方法语义一致；(e) 初始/终态集合正确。
  - Skill: `nop-testing`
- [ ] `Proof`：层 2 四方对照（Order 单条）——dict `wf/approve-status` ↔ `sales/state-machine.md` §审批轴 ↔ Bean 元数据 ↔ 全部 writer（5 Processor + 创建路径写 UNSUBMITTED + 通用 CRUD 路径 §9.4）。预期无漂移；若发现按规则 5 Fix/Decision + successor。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] `ErpSalOrderApprovalStateMachine` Bean 存在、已注册、严格无状态；5 个 Order 审批 Processor 委托 Bean，内联 `Objects.equals` 矩阵判断已移除（动态 hook 除外）。
- [ ] Order 层 1 矩阵测试本地 `mvn test -pl module-sales/erp-sal-service -am -Dtest=TestErpSalOrderApprovalStateMachineMatrix` 全绿。

### Phase 2 - ErpSalQuotation approveStatus Bean（M3.6）+ 两实体一致性复核

Status: planned
Targets: `.../statemachine/ErpSalQuotationApprovalStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpSalQuotation{SubmitForApproval,Approve,Reject,ReverseApprove,Withdraw}Processor.java`、`.../test/.../TestErpSalQuotationApprovalStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（跨实体 Decision 已固化，Quotation 沿用 Order 范式）

- [ ] `Add`：落地 `ErpSalQuotationApprovalStateMachine`（同 Phase 1 结构，领域码 `ERR_QUOTATION_ILLEGAL_STATUS_TRANSITION`，参数 `quotationCode`/...）；4 个 Quotation Processor 覆写 `validateTransitionForXxx` 委托 Bean。Quotation 无 commitment/intercompany 副作用（保持）。注册 Bean。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：层 1 矩阵完备性（同 Phase 1，Quotation 独立测试）。
  - Skill: `nop-testing`
- [ ] `Proof`：层 2 四方对照（Quotation 单条，预期无漂移）。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] Quotation Bean 存在/注册/无状态；4 个 Quotation Processor 委托 Bean，内联矩阵判断已移除。
- [ ] Quotation 层 1 矩阵测试本地全绿。

### Phase 3 - 层 3 既有命名动作回归 + 两实体一致性复核

Status: planned
Targets: `module-sales/erp-sal-service/src/test/`（既有集成测试，零新建）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1–2（两实体 Bean + 接线已落地）

- [ ] `Proof`：层 3 既有命名动作回归（非 greenfield）——复用既有集成测试基线（`TestErpSalOrderApproval`/`TestErpSalQuotationCrudSmoke`/`TestErpSalQuotationToOrder`/`TestErpSalOrderToCashEnd` 等），证明 Processor 写回、审计 fromStatus/toStatus、SoD、领域错误码 + 参数、commitment-commit/intercompany-approve/可用量预检副作用时序不变。本地 `mvn test -pl module-sales/erp-sal-service -am` 全绿。
  - Skill: `nop-testing`
- [ ] `Proof`：两实体一致性复核——两 Bean 命名（`Approval` 后缀）/注册/无状态/元数据形状一致；PROC 路径接线范式可追溯；四方对照记录写入本计划 Closure 段。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] `mvn test -pl module-sales/erp-sal-service -am` 全绿（层 3 回归无行为回归）。
- [ ] 两实体四方对照记录可追溯、漂移处置闭环（预期无漂移）。

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_0088c9370ffeMN3Mj6WdhuB5Qz，新会话）——实仓逐项核实通过：`AbstractApproveProcessor.validateTransitionForApprove:37-42` 固定守卫；ErpSalOrder/ErpSalQuotation 各 5 Processor（SubmitForApproval/Approve/Reject/ReverseApprove/Withdraw）均 extends 骨架、`validateTransitionForApprove` 未覆写（继承）；beans.xml 已注册 docStatus Bean（ErpSalOrderDocumentStateMachine L74/ErpSalQuotationDocumentStateMachine L81）；`ERR_ILLEGAL_STATUS_TRANSITION` 存在；层 3 测试基线（TestErpSalOrderApproval/TestErpSalQuotationCrudSmoke/TestErpSalOrderToCashEnd）存在；Order SoD `ERR_SAL_APPROVER_IS_CREATOR` + commitment/intercompany 委托 facade 核实。核心差异化声明（两销售实体均干净 PROC 路径，无采购 Quotation/Rfq 那种 INLINE/缺失漂移）核实 TRUE。Rule 4/5/13/14、anti-slack、scope、baseline-honesty、Bean 命名（`Approval` 后缀）、跨实体 Decision 复用全部 PASS；无 BLOCKER/MAJOR。3 MINOR（reject 5 动作口径、facade dead stub、行号引用精度）已采纳：将「4 Processor/4 动作」全面更正为「5 Processor（含 Reject）」+ reject 矩阵/断言。草案审查收敛，Plan Status → active。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [ ] 范围内行为完成（两实体 approveStatus Bean + 接线 + 层 1 矩阵 + 层 2 四方对照 + 层 3 回归）
- [ ] 相关文档对齐（预期无漂移；若发现则按规则 5 登记；架构 doc 不引用本路线图执行状态）
- [ ] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl module-sales/erp-sal-service -am` 全绿 + `bash docs/audits/nop-compliance-checker.sh` 全 19 规则 actual ≤ baseline（R5=0 不漂移、R11=0 不增）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### Delta 覆盖运行时实证

- Classification: `optimization candidate`
- Why Not Blocking Closure: M3 非保护域 Delta 为可选（模板 §11.2）；cs 试点 M1.2 已运行时实证业务级 Delta 同名覆盖机制。本计划不重复证明。
- Successor Required: yes（触发条件 = M5.3 最终跨域 Delta 覆盖回归）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 裁定选项 (c) 显式排除；更强全局写锁须改 ORM/xmeta（保护区 ask-first），独立 successor。
- Successor Required: no（仅当产品要求全局强制矩阵写锁时重开）

## Closure

Status Note: <待执行与独立结束审计后填充>

Closure Audit Evidence:

- Auditor / Agent: <独立结束审计子代理（新会话，非执行者上下文）>
- Evidence: <待填充>

Follow-up:

- <无非阻塞跟进>
