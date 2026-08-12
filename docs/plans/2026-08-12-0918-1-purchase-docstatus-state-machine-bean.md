# 2026-08-12-0918-1-purchase-docstatus-state-machine-bean 采购单据 docStatus 最小生命周期 StateMachine Bean 迁移（M2.5–M2.8）

> Plan Status: completed
> Last Reviewed: 2026-08-12
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M2.5 / M2.6 / M2.7 / M2.8（均 todo）
> Related: 前置 `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（M1.3 done，go 裁定，模板固化于 `entity-state-machine-bean.md §11`）；本计划解阻 M3.2/M3.3/M3.4/M3.5（采购 approveStatus 轴，各自 deps 含 M2.5/M2.6/M2.7/M2.8）；姊妹计划 `2026-08-12-0918-2-sales-docstatus-state-machine-bean.md`
> Mission: entity-state-machine
> Work Item: M2.5 + M2.6 + M2.7 + M2.8
> Audit: required

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md`（PUR-1/20/21/22/23/24/25 行）+ 实仓核实。docStatus 轴是采购单据三轴分离中的**业务生命周期轴**（`purchase/state-machine.md` §三轴状态分离 + §2），与 approveStatus 审批轴独立。

- **轴语义（最小生命周期）**：`DRAFT`（初始态，创建时写入）→ `CANCELLED`（终态，作废）。dict `erp/doc-status`（共享字典，purchase/sales 单据头共用）。PUR-1/20/21/22/23/25 八属性登记均为「纳入 / 无财务影响 / 最小生命周期」。这是模板 §11.3「M2.8 ErpPurOrder.docStatus（DRAFT→CANCELLED 最小 2 态）」代表样例所覆盖的最简单类别。
- **固定迁移判断当前所在位置（实仓核实，关键）**：cancel 动作的「非已作废」守卫**不在采购域代码内联**，而在**共享骨架** `module-common-service/.../AbstractCancelProcessor.validateTransitionForCancel`（`AbstractCancelProcessor.java:30-35`）：内联 `Objects.equals(docStatus, cancelledDocStatus())` → 抛 `illegalStatusException`。这是本计划要替换为 Bean 调用的「固定来源态/目标态判断」。
- **逐实体 writer 盘点（实仓核实）**：
  - **M2.8 ErpPurOrder（PROC 路径）**：`ErpPurOrderCancelProcessor` extends `AbstractCancelProcessor<ErpPurOrder>`（`ErpPurOrderCancelProcessor.java`）。守卫经骨架 `validateTransitionForCancel`；`beforeCancel` 承载 `runCommitmentReleaseHook` + `runIntercompanyReverseHook`（**动态业务守卫/副作用，保留原位**）；`cancelledDocStatus()` 返回 `ErpPurConstants.DOC_STATUS_CANCELLED`；`illegalStatusException` 映射领域码 `ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION`（参数 `orderCode`/`currentDocStatus`/`expectedDocStatus`）。初始 DRAFT 由创建路径写入。
  - **M2.7 ErpPurRequisition（PROC 路径）**：`ErpPurRequisitionCancelProcessor` extends `AbstractCancelProcessor`，结构同 Order（领域码 `ERR_REQ_ILLEGAL_DOC_STATUS_TRANSITION`，参数 `requisitionCode`/...）；无 beforeCancel hook（`ErpPurRequisitionProcessor` 另有 cancel-after-convert 逻辑，非本 Processor）。
  - **⚠️ 残留重复守卫（层 2 须核实非 live writer）**：`ErpPurOrderProcessor.validateTransitionForCancel`（`ErpPurOrderProcessor.java:153`）与 `ErpPurRequisitionProcessor.validateTransitionForCancel`（`ErpPurRequisitionProcessor.java:147`）也存在 docStatus 守卫覆写（per-mutation 拆分前的 facade 残留）。**live cancel 路径经 `*CancelProcessor` 子类**（BizModel 委托 CancelProcessor），facade 覆写非 live writer；层 2 四方对照须确认其为非 live，若仍 live 则一并接线。
  - **M2.5 ErpPurQuotation（INLINE/BizModel 路径，⚠️ 发现项）**：cancel 在 `ErpPurQuotationBizModel.cancel`（`ErpPurQuotationBizModel.java:63-67`）：`requireEntity → setDocStatus(DOC_STATUS_CANCELLED) → updateEntity`，**无任何 docStatus 守卫**（无 CancelProcessor，全仓无 `ErpPurQuotationCancelProcessor`，已核实）。即当前允许任意态（含已 CANCELLED）→ CANCELLED（幂等 no-op）。
  - **M2.6 ErpPurRfq（INLINE/BizModel 路径，⚠️ 发现项）**：cancel 在 `ErpPurRfqBizModel.cancel`（`ErpPurRfqBizModel.java:22-27`）：结构同 Quotation，**无 docStatus 守卫**（无 `ErpPurRfqCancelProcessor`）。同样允许幂等 CANCELLED→CANCELLED。
- **owner doc 对 cancel 守卫的表述**：`purchase/state-machine.md` §2 迁移表「任意非终态 → 作废」（前置「单据未到终态」）+ §实现模式与守卫边界「isCancelled + src 状态校验」。即 owner doc 要求 cancel 守卫「非已作废」。Quotation/Rfq 当前**无此守卫** → 构成 **代码-文档漂移**，按路线图规则 5 必须在本计划层 2 四方对照中**显式裁定**（implementation drift → Fix 加守卫；或 intentional legacy behavior → owner doc 补注 + Bean 保持幂等），**禁止静默折叠**。
- **common 层非法迁移码已存在（参数形状已裁定）**：`ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（`nop.err.erp.common.illegal-status-transition`，参数 `currentStatus`/`expectedStatus`，`ErpCommonErrors.java:23-27`），经 `AbstractProcessor.defaultIllegalStatusException` 使用。cs 试点 M1.1 Decision（Option A）已裁定：**复用既有 common 码 + `action` 作补充诊断参数**（语义一致：currentStatus=fromStatus）。本计划沿用，不新增 common 码、不改保护区架构 doc。
- **Bean 注册范式已存在**：`module-purchase/erp-pur-service/src/main/resources/_vfs/erp/pur/beans/app-service.beans.xml` 已以 `<bean id="<FQN>" class="<FQN>"/>` 注册既有 per-mutation Processor。StateMachine Bean 沿用此范式。cs 试点 `app.erp.cs.service.statemachine.ErpCsTicketStateMachine` 为落地范本。
- **层 3 回归基线已存在（非 greenfield）**：采购域无既有 `TestErpPur*StateMachine` 矩阵测试（docStatus 轴无），故层 1 矩阵测试为 greenfield；但存在大量覆盖 cancel 全生命周期的集成测试 = 层 3 回归基线：`TestErpPurOrderApproval`、`TestErpPurRequisitionApproval`、`TestErpPurQuotationRfqReverseApprove`、`TestErpPurRequisitionCrudSmoke`、`TestErpPurProcureToPayEnd` 等（经 BizModel/IGraphQLEngine 入口断言 cancel 副作用、终态、错误码）。执行者不得将层 3 当空白重建，也不得用层 3 冒充层 1 矩阵完备性。
- **合规基线**：`docs/audits/compliance-baseline.md` R5（`@Inject private`）= 0、R11（Processor 重复状态判断方法）= 0。本计划新增 Bean 注册 + `@Inject`（非 private）保持 R5=0；接线后骨架守卫收敛到 Bean，R11 不增。

## Goals

- 为采购 4 个单据实体的 docStatus 轴各落地一个实体级 `ErpPur<Entity>DocumentStateMachine` Bean（一 Bean 对一实体一轴），承载 DRAFT→CANCELLED 最小迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态（§2）。
- 将 cancel 路径的**固定来源态/目标态判断**改调 Bean（`assertCanCancel(docStatus)` + `cancelTargetStatus()`）：PROC 路径（Order/Requisition）经 `*CancelProcessor.validateTransitionForCancel` 覆写委托 Bean；INLINE/BizModel 路径（Quotation/Rfq）在 BizModel cancel 方法内显式调 Bean。**动态业务守卫与副作用保留原位**（Order 的 commitment-release/intercompany-reverse、Quotation 的供应商资格检查等）。
- 层 2 四方对照（dict ↔ `purchase/state-machine.md` ↔ Bean 元数据 ↔ 全部 writer 含 CRUD 路径）逐实体裁定，**显式处置 Quotation/Rfq 缺失守卫漂移**（Fix 或 intentional legacy + owner doc 补注），禁止静默排除。
- 新增层 1 矩阵完备性表驱动测试（greenfield，4 个 Bean 各一）；层 3 既有集成测试全绿回归。
- 保持全部既有外部行为不变（错误码值、参数、审计、commitment/intercompany 副作用时序），唯一允许的行为变化是经层 2 裁定为 implementation drift 后**按 Fix 加守卫**的 Quotation/Rfq（须显式 Decision 记录）。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（路线图 Non-Goal；docStatus 字段语义/dict 不变）。
- 不迁移 `approveStatus` 轴（归 M3.2–M3.5，各自独立 Bean，依赖本计划对应项 done 后启动）。
- 不迁移 `paidStatus`/`receiveStatus`/`writtenOffStatus`（PUR-3/7/11 等已裁定排除-技术/派生）。
- 不触碰 `posted`（boolean，业财过账/红冲契约，不作迁移轴，§3）；采购 docStatus cancel 不直接触发过账（approveStatus APPROVED 才触发，归 M4）。
- 不修改共享骨架 `AbstractCancelProcessor`（module-common-service）——迁移经各域 `*CancelProcessor` 覆写 `validateTransitionForCancel` 委托 Bean，骨架保持原样供未迁移实体继续使用。`module-common-service` 零改动。
- 不引入全局 CRUD 写锁 / xmeta `notUpload`（CRUD 边界已在 M0.1 §9 裁定为选项 c；更强写锁是 successor）。
- 不在本计划证 Delta 覆盖（M2 非保护域，Delta 可选；cs 试点 M1.2 已运行时实证业务级 Delta 同名覆盖机制，本计划不重复证明。Delta 覆盖回归归 M5.3 最终跨域回归统一证）。
- 不改变 cancel 的错误码值/参数形状/审计 actionType（领域码 `ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION` 等保持；common 码作 cause）。
- 不构建反射型/泛型全局 `IStateMachine` 调度器（路线图 Non-Goal）。

## Task Route

- Type: `implementation-only change`（消费已定稿 M0.1 契约 + M0.2 清单 + M1.3 批量迁移模板 §11，落地 4 个单实体单轴 Bean + 接线 + 测试 + 四方对照；不改契约/模型/公共 API/共享骨架）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 批量迁移模板）、`docs/design/purchase/state-machine.md`（业务状态语义 + §三轴分离 + §实现模式与守卫边界）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（PUR-1/20–25 八属性）、`docs/architecture/processor-extension-pattern.md`（Bean 嵌入 Processor 编排点）、`docs/skills/state-machine-business-review-prompt.md`（层 2 四方对照 10 维度）
- Skill Selection Basis: 路线图 M2.5–M2.8 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「BizModel/Processor 接线、Bean 注册、`@Inject` 非 private、跨实体调用边界、错误码、事务边界、产品化可定制性自检」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成测试回归」。其必需输入（owner doc + M0.1 契约 + M1.3 模板 + 既有测试）均已就绪。`state-machine-business-review-prompt.md` 匹配层 2 四方对照（步骤 5 标配）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯后端 Java + 既有 pur-service 测试容器）。
- 前置依赖：M1.3 done（go 裁定 + §11 模板固化）已满足；M2.5–M2.8 deps = M1.3，门控已解除。
- 无 data-deletion / 财务过账 / ORM 保护区域触发（docStatus cancel 不触发过账；approveStatus 才触发，归 M4）。

## Execution Plan

### Phase 1 - ErpPurOrder docStatus Bean（M2.8）+ 跨实体 Decision 固化

Status: completed
Targets: `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/statemachine/ErpPurOrderDocumentStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpPurOrderCancelProcessor.java`、`module-purchase/erp-pur-service/src/test/.../TestErpPurOrderDocumentStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done（已满足）

- [x] `Add`：落地 `ErpPurOrderDocumentStateMachine` Bean（一实体一轴 docStatus）——显式 `assertCanCancel(String docStatus)`（CANCELLED → 抛 common 码 `ERR_ILLEGAL_STATUS_TRANSITION` + `action=cancel`/`fromStatus` 补充参数）、`cancelTargetStatus()`→CANCELLED、`isTerminal`/`initialStatuses`/`terminalStatuses`、只读 `transitions()`（DRAFT→CANCELLED 一条边）。严格无状态（不注入 DAO/IBiz/IServiceContext/事务，§2）。命名带 `Document` 后缀（§1 双轴约定，为 M3.5 approveStatus Bean 预留 `ErpPurOrderApprovalStateMachine` 命名空间）。
  - Skill: `nop-backend-dev`
- [x] `Add`：在非生成 `_vfs/erp/pur/beans/app-service.beans.xml` 以 `<bean id="<FQN>" class="<FQN>"/>` 注册（§5）。
  - Skill: `nop-backend-dev`
- [x] `Decision | Add`：固化本计划跨实体接线 Decision——(A) **Bean 接线点 = 各域 `*CancelProcessor` 覆写 `validateTransitionForCancel` 委托 Bean**（不改共享骨架 `AbstractCancelProcessor`，未迁移实体不受影响）；(B) **common 错误码 = 沿用 cs 试点 Option A**（复用 `ERR_ILLEGAL_STATUS_TRANSITION` + `action` 补充参数，不新增 common 码）；(C) **领域码映射 = Processor 既有 `illegalStatusException` 保留**（Bean 抛 common 码作 cause，Processor 捕获/感知后映射 `ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION` + 实体编号/上下文，§7）。在 Order 上落地：`ErpPurOrderCancelProcessor` 注入 `@Inject ErpPurOrderDocumentStateMachine`（非 private），覆写 `validateTransitionForCancel` 调 `stateMachine.assertCanCancel(entity.getDocStatus())`；`cancelledDocStatus()` 改用 `stateMachine.cancelTargetStatus()` 写回；`beforeCancel`（commitment-release/intercompany-reverse）**保留原位**。grep 证 `ErpPurOrderCancelProcessor` 内不再有内联 `Objects.equals` 矩阵判断（动态 hook 除外）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：层 1 矩阵完备性（greenfield 表驱动，不经 BizModel）——(a) 无重复/冲突边；(b) 从 DRAFT 可达 CANCELLED、CANCELLED 终态无出边；(c) `assertCanCancel` 对 DRAFT 合法、对 CANCELLED 抛 common 码携带 `action=cancel`/`fromStatus=CANCELLED`；(d) `transitions()` 与显式方法语义一致；(e) 初始/终态集合正确。
  - Skill: `nop-testing`
- [x] `Proof`：层 2 四方对照（Order 单条）——dict `erp/doc-status`（DRAFT/ACTIVE/CANCELLED）↔ `purchase/state-machine.md` §2/§实现模式 ↔ Bean 元数据 ↔ 全部 writer（CancelProcessor + 创建路径写 DRAFT + 通用 CRUD 路径 §9.4）。检测结果：Order 实体 docStatus 仅 DRAFT（创建）+ CANCELLED（cancel），ACTIVE 为 dict 共享值非 Order 生命周期态（非死状态，其它单据类型可能使用）；无漂移。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] `ErpPurOrderDocumentStateMachine` Bean 存在、已注册、严格无状态（无可注入依赖）；`ErpPurOrderCancelProcessor.validateTransitionForCancel` 委托 Bean，grep 证内联 `Objects.equals` 矩阵判断已移除（动态 hook 除外）。
- [x] Order 层 1 矩阵测试本地 `mvn test -pl module-purchase/erp-pur-service -am -Dtest=TestErpPurOrderDocumentStateMachineMatrix` 全绿（6 tests, 0 failures）。

### Phase 2 - ErpPurRequisition docStatus Bean（M2.7）

Status: completed
Targets: `.../statemachine/ErpPurRequisitionDocumentStateMachine.java`、`.../beans/app-service.beans.xml`、`.../processor/ErpPurRequisitionCancelProcessor.java`、`.../test/.../TestErpPurRequisitionDocumentStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（跨实体 Decision 已固化，Requisition 沿用 Order 范式）

- [x] `Add`：落地 `ErpPurRequisitionDocumentStateMachine`（同 Phase 1 结构，领域码 `ERR_REQ_ILLEGAL_DOC_STATUS_TRANSITION`）；`ErpPurRequisitionCancelProcessor` 覆写 `validateTransitionForCancel` 委托 Bean。Requisition 无 beforeCancel hook（保持）。注册 Bean。
  - Skill: `nop-backend-dev`
- [x] `Proof`：层 1 矩阵完备性（同 Phase 1 五点，Requisition 独立测试）。
  - Skill: `nop-testing`
- [x] `Proof`：层 2 四方对照（Requisition 单条，预期无漂移）。结果：Requisition docStatus 同 Order（DRAFT 创建 + CANCELLED cancel），无漂移。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] Requisition Bean 存在/注册/无状态；`ErpPurRequisitionCancelProcessor` 委托 Bean，内联矩阵判断已移除。
- [x] Requisition 层 1 矩阵测试本地全绿（6 tests, 0 failures）。

### Phase 3 - ErpPurQuotation + ErpPurRfq docStatus Bean（M2.5 + M2.6）+ 缺失守卫漂移裁定

Status: completed
Targets: `.../statemachine/ErpPurQuotationDocumentStateMachine.java`、`.../statemachine/ErpPurRfqDocumentStateMachine.java`、`.../beans/app-service.beans.xml`、`.../entity/ErpPurQuotationBizModel.java`、`.../entity/ErpPurRfqBizModel.java`、`.../test/.../TestErpPurQuotationDocumentStateMachineMatrix.java`、`.../test/.../TestErpPurRfqDocumentStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing` + `state-machine-business-review-prompt.md`

- Item Types: `Add | Decision | Fix | Proof`
- Prereqs: Phase 1（跨实体 Decision 已固化）

- [x] `Decision`（Quotation/Rfq 缺失守卫漂移裁定）：层 2 四方对照显式裁定 = **(a) implementation drift → Fix**。owner doc §2「任意非终态 → 作废」+ §实现模式「isCancelled + src 状态校验」（`entity.docStatus === 'CANCELLED'` 阻断）要求 cancel 守卫「非已作废」，但 Quotation/Rfq BizModel cancel 迁移前**无 docStatus 守卫**（允许幂等 CANCELLED→CANCELLED）。owner doc 为权威 → 代码缺守卫 = implementation drift。Bean 矩阵含「CANCELLED 非法」守卫，BizModel 接线后 cancel 对已作废单据抛领域码（行为变化：原幂等 no-op → 抛错，已显式记录）。残留风险：前端/集成方若依赖幂等 cancel 已作废报价/询价单，将收到新错误码——但无既有测试或快照覆盖此路径，且 owner doc 一直要求此守卫，行为对齐权威设计。Skill: `state-machine-business-review-prompt.md`
- [x] `Add`：按裁定分支 (a) 落地 `ErpPurQuotationDocumentStateMachine` + `ErpPurRfqDocumentStateMachine`（矩阵含「CANCELLED 非法」守卫）。两 BizModel 注入对应 Bean（`@Inject` 非 private），cancel 方法内调 `stateMachine.assertCanCancel(entity.getDocStatus())` + `stateMachine.cancelTargetStatus()` 写回；Quotation 的供应商资格检查（`defaultPrepareSave`）保留原位。注册两 Bean。
  - Skill: `nop-backend-dev`
- [x] `Fix`（Decision 裁定 (a) implementation drift 生效）：Quotation/Rfq BizModel cancel 加 docStatus 守卫，对已作废单据抛领域码。**sub-Decision**：新增 `ERR_QUOTATION_ILLEGAL_DOC_STATUS_TRANSITION`（`erp.err.pur.quotation-illegal-doc-status-transition`，参数 `quotationCode`）+ `ERR_RFQ_ILLEGAL_DOC_STATUS_TRANSITION`（`erp.err.pur.rfq-illegal-doc-status-transition`，参数 `rfqCode`），不复用通用 `ERR_ILLEGAL_DOC_STATUS_TRANSITION`（其文案绑定 `receiveCode` 不适用）。理由：与既有 Order/Req/Invoice/Payment/Return 各自专属 docStatus 错误码模式一致（每实体一码，文案绑定各自编号参数）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：层 1 矩阵完备性（Quotation + Rfq 各一，分支 a：断言 CANCELLED 抛 common 码）。
  - Skill: `nop-testing`
- [x] `Proof`：层 2 四方对照（Quotation + Rfq 各一，含缺失守卫漂移裁定闭环 + writer 含通用 CRUD 路径）。结果：两实体 docStatus = DRAFT（创建）+ CANCELLED（cancel），漂移已 Fix（守卫已加），闭环。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] Quotation + Rfq Bean 存在/注册/无状态；两 BizModel cancel 经 Bean；缺失守卫漂移裁定结论已写入（分支 a 的 Fix 已落地）。
- [x] Quotation + Rfq 层 1 矩阵测试本地全绿（各 6 tests, 0 failures）。

### Phase 4 - 层 3 既有命名动作回归 + 四实体一致性复核

Status: completed
Targets: `module-purchase/erp-pur-service/src/test/`（既有集成测试，零新建）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1–3（四实体 Bean + 接线已落地）

- [x] `Proof`：层 3 既有命名动作回归（非 greenfield）——复用既有集成测试基线（`TestErpPurOrderApproval`/`TestErpPurRequisitionApproval`/`TestErpPurQuotationRfqReverseApprove`/`TestErpPurRequisitionCrudSmoke`/`TestErpPurProcureToPayEnd` 等），证明 Processor 写回、审计 fromStatus/toStatus、领域错误码 + 参数、终态不可恢复（分支 a）、commitment-release/intercompany-reverse 副作用时序不变。本地 `mvn test -pl module-purchase/erp-pur-service -am` 全绿（190 tests, 0 failures, 0 errors）。
  - Skill: `nop-testing`
- [x] `Proof`：四实体一致性复核——四 Bean 命名/注册/无状态/元数据形状一致；PROC 路径（Order/Requisition）与 INLINE/BizModel 路径（Quotation/Rfq）接线范式可追溯；四方对照记录写入本计划 Closure 段。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] `mvn test -pl module-purchase/erp-pur-service -am` 全绿（层 3 回归无行为回归，190 tests, 0 failures）。
- [x] 四实体四方对照记录可追溯、漂移处置闭环。

## Draft Review Record

- Independent draft review iteration 1: `accept` (`ses_00c6fc87effe3yrPEuCBz1MHW3`) — 实仓逐项核实通过（`AbstractCancelProcessor.validateTransitionForCancel` 内联守卫 + protected 可覆写、Order/Requisition 经 CancelProcessor、Quotation/Rfq BizModel 无守卫、无 Quotation/Rfq CancelProcessor、common 码 + 既有领域码、层 3 测试基线均存在）。Rule 4/14 分组、§11 七步覆盖、Rule 5/13 Quotation/Rfq 缺失守卫双分支 successor、Non-Goals、退出标准、命名、common 码复用全部 PASS；无 BLOCKER/MAJOR。2 个 MINOR（Quotation/Rfq docStatus 错误码命名不对称；facade 残留 `validateTransitionForCancel` 未在 writer 盘点登记）已采纳修订：baseline 增 facade 残留守卫行（层 2 须核实非 live writer）+ Phase 3 Fix 项澄清两实体均无既有 docStatus 码 + sub-Decision。草案审查收敛，Plan Status → active。

## Closure Gates

- [x] 范围内行为完成（四实体 docStatus Bean + 接线 + 层 1 矩阵 + 层 2 四方对照 + 层 3 回归）
- [x] 相关文档对齐（分支 a Fix 不需 owner doc 补注——owner doc §2/§实现模式本就要求守卫，代码经 Fix 对齐之；架构 doc 不引用本路线图执行状态）
- [x] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS（156 reactor 模块）+ `mvn test -pl module-purchase/erp-pur-service -am` 全绿（190 tests, 0 failures, 0 errors）+ `bash docs/audits/nop-compliance-checker.sh` 全 19 规则 actual ≤ baseline（R5=0 不漂移、R11=0 不增、R2c=1392=baseline 无漂移）
- [x] 无范围内项目降级为 deferred/follow-up（Quotation/Rfq 缺失守卫漂移裁定已落地为 Fix 分支 a）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 采购 approveStatus 轴迁移（M3.2–M3.5）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: approveStatus 是独立轴（独立 Bean `ErpPur<Entity>ApprovalStateMachine`，§3 三轴分离）；本计划只迁移 docStatus。approveStatus 审批轴 deps 含本计划对应项（M2.5/6/7/8）done 后启动。
- Successor Required: yes（触发条件 = 本计划闭包后，M3.2/M3.3/M3.4/M3.5 各自独立 plan 启动）

### Delta 覆盖运行时实证

- Classification: `optimization candidate`
- Why Not Blocking Closure: M2 非保护域 Delta 为可选（模板 §11.2 M2 变体）；cs 试点 M1.2 已运行时实证业务级 Delta 同名覆盖机制（基线/Delta 双加载可区分）。本计划不重复证明。
- Successor Required: yes（触发条件 = M5.3 最终跨域 Delta 覆盖回归）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 裁定选项 (c) 显式排除；更强全局写锁须改 ORM/xmeta（保护区 ask-first），独立 successor。
- Successor Required: no（仅当产品要求全局强制矩阵写锁时重开）

## Closure

Status Note: 全四阶段执行完成（Phase 1-4 全绿）。四实体 docStatus Bean 落地 + 接线 + 层 1 矩阵 + 层 2 四方对照 + 层 3 回归 + 合规基线零漂移。Quotation/Rfq 缺失守卫漂移裁定为分支 (a) implementation drift → Fix（新增 `ERR_QUOTATION_ILLEGAL_DOC_STATUS_TRANSITION` / `ERR_RFQ_ILLEGAL_DOC_STATUS_TRANSITION`）。独立结束审计已由新会话子代理执行并通过。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，非执行者上下文）—— closure audit of `2026-08-12-0918-1-purchase-docstatus-state-machine-bean.md`
- Evidence:
  - **实时基线核实（live repo grep/read，非引用执行者断言）**：4 StateMachine Bean 文件存在（`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/statemachine/ErpPur{Order,Requisition,Quotation,Rfq}DocumentStateMachine.java`）+ 4 矩阵测试存在（`src/test/.../statemachine/TestErpPur{Order,Requisition,Quotation,Rfq}DocumentStateMachineMatrix.java`）；`app-service.beans.xml` 已注册 4 Bean（`<bean id="<FQN>" class="<FQN>"/>`，L176-183）；`ErpPurOrderCancelProcessor`/`ErpPurRequisitionCancelProcessor` 覆写 `validateTransitionForCancel` 委托 `stateMachine.assertCanCancel` + `cancelledDocStatus()` 委托 `stateMachine.cancelTargetStatus()`（grep 证 `stateMachine` 字段 + 方法调用）；`ErpPurQuotationBizModel.cancel`/`ErpPurRfqBizModel.cancel` 接线 Bean + 已作废守卫抛 `ERR_QUOTATION_ILLEGAL_DOC_STATUS_TRANSITION`/`ERR_RFQ_ILLEGAL_DOC_STATUS_TRANSITION`（Fix 分支 a 落地）；`ErpPurErrors` 新增两领域码 + `ARG_QUOTATION_CODE`/`ARG_RFQ_CODE` 参数（L269-278）。
  - **Anti-Hollow 核查**：Bean 非空壳——含显式 `assertCanCancel`/`cancelTargetStatus`/`isTerminal`/`transitions()`/`terminalStatuses`/`initialStatuses` 实现 + `TransitionDefinition` 元数据类型；Processor `validateTransitionForCancel` 内 try/catch 映射领域码（非 `return null`/吞异常）；矩阵测试 6 用例真实断言（无重复边/可达性/cancel 合法+非法抛 common 码携带 action+fromStatus 元数据/transitions 一致/初始终态集合）。
  - **Five-point 一致性**：Plan Status=completed / 四 Phase Status=completed / 四 Phase Exit Criteria 全 [x] / Closure Gates 全 [x]（含本轮自勾选的独立审计门）/ Closure evidence 非占位符——一致。
  - **层 2 漂移处置诚实性**：Quotation/Rfq cancel 缺守卫漂移裁定为分支 (a) implementation drift → Fix（非隐藏为 Deferred/Follow-up），新增领域码 + BizModel 守卫已落地；facade 残留 `ErpPurOrderProcessor.validateTransitionForCancel`/`ErpPurRequisitionProcessor.validateTransitionForCancel` 已登记为非 live writer（grep 证无调用方），不静默排除。
  - **Docs sync**：`docs/logs/2026/08-12.md` 首条目覆盖本计划（工作项 + 4 Bean + 接线路径 + 层 2 漂移裁定 + 验证命令 + 下一步 M3.2-M3.5 门控解除），符合 AGENTS.md §8 日志要求；架构/设计 owner doc 对齐（分支 a Fix 对齐 owner doc 既有守卫要求，无需补注；架构 doc 不引用路线图执行状态）。
  - **Deferred honesty**：3 项 Deferred（approveStatus 轴 M3.2-M3.5 / Delta 覆盖实证 / 全局 CRUD 写锁）均为非阻塞 follow-up，无已确认 live defect 隐藏其中。
- Verdict: **PASS**（无 blocker / 无 anti-hollow / 五点一致 / 漂移处置诚实 / docs sync 满足 / deferred honesty 满足）。

执行证据（执行者自查，非结束审计）：

1. **新增文件**（4 Bean + 4 矩阵测试）：
   - `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/statemachine/ErpPurOrderDocumentStateMachine.java`
   - `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/statemachine/ErpPurRequisitionDocumentStateMachine.java`
   - `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/statemachine/ErpPurQuotationDocumentStateMachine.java`
   - `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/statemachine/ErpPurRfqDocumentStateMachine.java`
   - `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/statemachine/TestErpPur{Order,Requisition,Quotation,Rfq}DocumentStateMachineMatrix.java`

2. **修改文件**（2 CancelProcessor + 2 BizModel + 1 beans.xml + 1 Errors）：
   - `ErpPurOrderCancelProcessor.java`：注入 Bean + 覆写 `validateTransitionForCancel` 委托 Bean + `cancelledDocStatus()` 委托 Bean
   - `ErpPurRequisitionCancelProcessor.java`：同上
   - `ErpPurQuotationBizModel.java`：注入 Bean + cancel 加 docStatus 守卫（Fix 分支 a）
   - `ErpPurRfqBizModel.java`：同上
   - `_vfs/erp/pur/beans/app-service.beans.xml`：注册 4 StateMachine Bean
   - `ErpPurErrors.java`：新增 `ARG_QUOTATION_CODE`/`ARG_RFQ_CODE` + `ERR_QUOTATION_ILLEGAL_DOC_STATUS_TRANSITION`/`ERR_RFQ_ILLEGAL_DOC_STATUS_TRANSITION`

3. **层 2 四方对照汇总**（dict `erp/doc-status` DRAFT/ACTIVE/CANCELLED ↔ `purchase/state-machine.md` §2/§实现模式 ↔ Bean 元数据 ↔ writer）：
   - **Order/Requisition**（PROC 路径）：docStatus = DRAFT（创建）+ CANCELLED（cancel 经 CancelProcessor→Bean）；ACTIVE 为 dict 共享值非生命周期态；无漂移。facade 残留 `ErpPurOrderProcessor.validateTransitionForCancel`/`ErpPurRequisitionProcessor.validateTransitionForCancel` 确认非 live writer（grep 证无调用方），不影响迁移。
   - **Quotation/Rfq**（INLINE/BizModel 路径）：迁移前 cancel 无守卫（允许幂等 CANCELLED→CANCELLED）↔ owner doc §2/§实现模式要求「非已作废」守卫 → **漂移**。裁定 = **(a) implementation drift → Fix**：Bean 含 CANCELLED 非法守卫，BizModel 接线后已作废单据 cancel 抛领域码。行为变化已显式记录。

4. **验证**：`mvn clean install -DskipTests` BUILD SUCCESS（156 模块）；`mvn test -pl module-purchase/erp-pur-service -am` → 190 tests, 0 failures, 0 errors；`bash docs/audits/nop-compliance-checker.sh` → R5=0, R11=0, R2c=1392（=baseline，零漂移），全 19 规则 actual ≤ baseline。
