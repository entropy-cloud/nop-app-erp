# 2026-08-12-1841-2-erpb2b-asn-edidoc-state-machine-beans B2B ErpB2bAsn + ErpB2bEdiDoc 实体级状态机 Bean（M2.16 + M2.17）

> Plan Status: completed
> Last Reviewed: 2026-08-12
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M2.16（todo）+ M2.17（todo）
> Related: 前置 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（M0.1 done）+ `2026-08-12-0617-2-entity-state-machine-m0-2-inventory.md`（M0.2 done）+ `2026-08-12-0738-1-cs-ticket-state-machine-bean-pilot.md`（M1.1 范式）+ `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（M1.3 模板 done）；姊妹范式 `2026-08-12-0918-1-purchase-docstatus-state-machine-bean.md`（Quotation/Rfq 缺失守卫漂移裁定 + INLINE/PROC 双路径范本）
> Mission: entity-state-machine
> Work Item: M2.16 + M2.17
> Audit: required

## Current Baseline

- **M0.1 契约 + M1.3 模板已就绪**（`docs/architecture/entity-state-machine-bean.md` §1-§11）。M1.3 已裁定 **go**，M2 各项 Deps（M1.3）门控解除；`todo → ready` 仍需独立 plan 草案审查（路线图规则 1）。
- **本计划按规则 14 将同组件（同 owner doc `docs/design/b2b/state-machine.md`、同结果表面「B2B 域生命周期 StateMachine Bean」、同验证路径）的两条状态轴合并为一个计划的两个阶段**：EdiDoc（`state` 字段，§1，owner doc 主体）+ Asn（`status` 字段，§2，owner doc 薄段）。二者均 M2 简单生命周期（§11.2），非保护域、无审批子矩阵、无跨域过账副作用（EdiDoc/Asn 状态本身不触发业财过账；Asn createReceiveFromAsn 触发 ErpPurReceive 是独立实体独立轴）。
- **EdiDoc（ErpB2bEdiDoc.state）语义**（owner doc `state-machine.md` §1-§3 `:11-101`）：8 态 TO_SEND/SENT/TO_CANCEL/CANCELLED/ERROR/RECEIVED/ACKNOWLEDGED/ARCHIVED；初始 = TO_SEND（出站）/ RECEIVED（入站）；终态 = CANCELLED/ACKNOWLEDGED/ARCHIVED（ERROR 可恢复）。命名动作：markSent(TO_SEND→SENT)、markAcknowledged(SENT→ACKNOWLEDGED)、markError(TO_SEND|SENT→ERROR 出站 / RECEIVED→ERROR 入站)、retry(ERROR→TO_SEND 出站 / ERROR→RECEIVED 入站)、cancel(TO_SEND|SENT|ERROR→CANCELLED，多源)、createInbound(→RECEIVED)、archive(RECEIVED→ARCHIVED)。**状态推进完全由命名 @BizMutation 驱动**（无异步/Cron 自动推进——owner doc `:5-9,179` 明示出站自动化 Deferred，`TransportManager` wired-but-uncalled）。
- **Asn（ErpB2bAsn.status）语义**（owner doc `:189-213`）：4 态 RECEIVED/MATCHED/RECEIVED_TO_STOCK/CANCELLED；初始 = RECEIVED；终态 = RECEIVED_TO_STOCK/CANCELLED。owner doc 列命名动作：matchPurchaseOrder(RECEIVED→MATCHED)、createReceiveFromAsn(MATCHED→RECEIVED_TO_STOCK)、cancel(RECEIVED|MATCHED→CANCELLED，多源)、retryMatch（幂等重置+重匹配）。**owner doc ASN 段无 §3-§10**（仅 §迁移图），显著薄于 EdiDoc 段。
- **dict 实况（含死状态）**：`erp-b2b/edi-doc-state`（`module-b2b/model/app-erp-b2b.orm.xml:27-36`）= 8 值，其中 **`TO_CANCEL` 为预留死状态**（owner doc `:57` 明示 Deferred，生产零 writer，单步 SENT→CANCELLED 替代两步）；`erp-b2b/asn-status`（`:37-42`）= 4 值，其中 **`CANCELLED` 为预留死状态**（常量定义于 `ErpB2bConstants.java:22`，生产零 writer——cancel 动作未落地，见下）。
- **生产 writer 实况（固定迁移判断散布，已核实）**：
  - **EdiDoc（INLINE BizModel 路径）**：`ErpB2bEdiDocBizModel`（`entity/`）：markSent `:65-77`（守卫 `:68 !equals(TO_SEND)`→SENT `:71`，副作用写 sentAt `:72`）、markAcknowledged `:81-93`（守卫 `:84 !equals(SENT)`→ACKNOWLEDGED `:87`，副作用写 acknowledgedAt `:88`）、**markError `:97-108`（⚠️ 无任何状态守卫，任意态→ERROR `:101`）**、retry `:112-126`（守卫 `:115 !equals(ERROR)`→**TO_SEND `:118` 单目标 unconditional**——`:118` 无条件 `setState(TO_SEND)`、`:123` 无条件写 OUTBOUND 日志；**owner doc §2 入站 ERROR→RECEIVED retry 路径未实现**，见 D-B2B-6；retryCount++/error/blockingLevel 清除 `:119-121`）、cancel `:130-143`（多源守卫 `:133-135 !{TO_SEND,SENT,ERROR}`→CANCELLED `:138`）、archive `:157-168`（守卫 `:160 !equals(RECEIVED)`→ARCHIVED `:163`）；createOutbound/createInbound 经 Processor 写初始态（TO_SEND/RECEIVED）。**私有 `illegalTransition(doc,current,expected)` helper `:181-186`** 抛 `ERR_B2B_EDI_DOC_ILLEGAL_TRANSITION`（ediDocCode/currentState/expectedState）。
  - **Asn（per-mutation Processor 路径）**：`ErpB2bAsnMatchPurchaseOrderProcessor.matchPurchaseOrder`（守卫 `:41 !equals(RECEIVED)`→MATCHED `:84`）、`ErpB2bAsnCreateReceiveFromAsnProcessor.createReceiveFromAsn`（config-gate `erp-b2b.asn-auto-create-receive` `:42-47` + 守卫 `:51 !equals(MATCHED)`→RECEIVED_TO_STOCK `:82`）、`ErpB2bAsnRetryMatchProcessor.retryMatch`（幂等短路 `:27-30` + 重置 RECEIVED `:32-35` + 委托 match）、`ErpB2bAsnHandleInboundWebhookProcessor`（初始写 RECEIVED `:126`）。**无 ASN cancel Processor/动作**（grep 零 `setStatus(ASN_STATUS_CANCELLED)` writer）。三 Processor 各自内联 `new NopException(ERR_B2B_ASN_ILLEGAL_TRANSITION)`，无私有 helper；各 `requireAsn` 复用同码抛 not-found。
  - **EdiDoc vs Asn 接线不对称**：EdiDoc 状态迁移 mutation INLINE 在 BizModel（仅 create-* 经 Processor）；Asn 全部 mutation 经 per-mutation Processor。本计划保持此不对称（参照 purchase INLINE/PROC 双路径先例，不强制归一）。
- **关键漂移（layer-2 须裁定，非本重构静默折叠）**：
  - **D-B2B-1 EdiDoc `TO_CANCEL` 死状态**：dict 有值（`:30`），owner doc `:57` 已标 Deferred（两步取消未落地，生产单步 SENT→CANCELLED），零 writer。裁定 = intentional legacy / Deferred（保留 dict 值作预留，Bean 不编码 TO_CANCEL 边，successor = 两步取消业务流落地时）。已与 owner doc 一致，无需 Fix owner doc（已标注）。
  - **D-B2B-2 Asn `CANCELLED` 死状态 + cancel 动作未落地**：owner doc `:200-206` 列 cancel 边（RECEIVED|MATCHED→CANCELLED），但**全域零 cancel writer、零 cancel mutation**（`IErpB2bAsnBiz` 无 cancel 方法）。裁定 = **doc drift**（owner doc 描述了未实现的边）→ Fix owner doc ASN 段补注「cancel 边未落地（设计保留），DRAFT 废弃/取消当前经 CRUD」+ successor（PM 要求 ASN cancel 命名动作时开独立 plan 新增 mutation，可能触及 dict 行为 ask-first）。Bean **不编码 cancel 边**（无实现）；CANCELLED 不纳入 Bean 终态集（不可达）。**不在此重构新增 cancel mutation**（业务行为变更，归 successor）。
  - **D-B2B-3 EdiDoc `markError` 无守卫**：代码 `:97-108` 任意态→ERROR（含终态 CANCELLED/ACKNOWLEDGED/ARCHIVED→ERROR），但 owner doc `:28-48` 限定来源 TO_SEND|SENT（出站）/ RECEIVED（入站）。参照 purchase Quotation/Rfq cancel 缺失守卫先例，**初步裁定为 implementation drift → Fix**（Bean 守卫 markError 来源为 {TO_SEND, SENT, RECEIVED}，接线后从终态 markError 抛领域码；行为变化：原任意态→ERROR 收紧为文档来源）。**层 2 须以实仓证据复核**：若发现 markError 任意态是有意运维逃生舱（如人工强制标错），则反转为 intentional legacy + owner doc 补注。无论哪分支，须显式裁定 + 记录，禁止静默。
  - **D-B2B-4 错误码过载（已存在反模式）**：`ERR_B2B_EDI_DOC_ILLEGAL_TRANSITION`/`ERR_B2B_ASN_ILLEGAL_TRANSITION` 被 `requireDoc`/`requireAsn` 复用于 not-found（专用 `ERR_B2B_EDI_DOC_NOT_FOUND`/`ERR_B2B_ASN_NOT_FOUND` 存在但未使用）。pre-existing，登记 watch-only residual，不在本重构修复（修复需改 not-found 抛码路径，超出状态机集中范围）。
  - **D-B2B-5 TransportManager 潜在双 writer**：`TransportManager.markEdiDocSent`（`spi/transport/TransportManager.java:122-123`）有独立 `setState(SENT)` writer + 内联 TO_SEND 守卫，但**生产零调用方**（仅测试引用，owner doc `:179` 已注 wired-but-uncalled）。Bean 落地后成为 SENT 边的矩阵权威；若 TransportManager 未来激活，须路由经 Bean。登记 watch-only residual + successor（触发条件 = 出站自动化 `ErpB2bEdiOutboundJob` 落地时）。
- **错误码**：`ERR_B2B_EDI_DOC_ILLEGAL_TRANSITION`（`ErpB2bErrors.java:37-39`，ediDocCode/currentState/expectedState）、`ERR_B2B_ASN_ILLEGAL_TRANSITION`（`:48-50`，asnCode/currentState/expectedState）。common 层码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION` 已存在（M1.1 Option A 复用 + `action` 补充参数范式）。
- **生产 Bean 注册范式已存在**：`module-b2b/erp-b2b-service/src/main/resources/_vfs/erp/b2b/beans/app-service.beans.xml:55-66` 已以 `<bean id="<FQN>" class="<FQN>"/>` 注册 6 个 per-mutation Processor。StateMachine Bean 沿用此范式。
- **既有层 3 回归基线（非 greenfield）**：`TestErpB2bEdiEnvelope`（出站 happy path TO_SEND→SENT→ACKNOWLEDGED + `testErrorAndRetry` + `testCancelFromToSend` + `testInboundReceivedToArchived` + `testIllegalTransitionThrows` 单负例）、`TestErpB2bEdiPosting`、`TestErpB2bAsnInbound`、`TestErpB2bAsnInventoryIntegration`、`TestErpB2bAsnCrudSmoke`。M0.1 §10 基线不含 b2b——b2b 域层 3 = 上述既有集成测试。**已知覆盖缺口**（层 1 矩阵补）：无 EdiDoc 全 N×M 矩阵、无 ASN 非法迁移矩阵、cancel 多源 {SENT, ERROR} 未直接覆盖、markError 守卫（当前无）无断言。测试多用 `assertThrows(NopException.class)` 不断言具体码——层 1 矩阵将补码断言。
- **合规基线**：R5（`@Inject private`）= 0（已核实 module-b2b service 零违例）、R11= 0。本计划新增 2 Bean 注册 + 注入须保持 R5=0；接线后内联守卫收敛至 Bean，R11 不增。

## Goals

- 落地 `ErpB2bEdiDocStateMachine`（state 字段轴）+ `ErpB2bAsnStateMachine`（status 字段轴）两个独立 Bean，各承载**已实现**迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态、可经 Delta 同名覆盖。
- 将 EdiDoc BizModel（markSent/markAcknowledged/markError/retry/cancel/archive）与 Asn Processors（MatchPurchaseOrder/CreateReceiveFromAsn/RetryMatch）的**固定来源态/目标态判断**改调 Bean；**动态业务守卫保留原位**（EdiDoc retry 的出/入站方向判定、retryCount++/error 清除；Asn 的 HMAC 校验、PO 匹配/超量、config-gate `erp-b2b.asn-auto-create-receive`、幂等 eventId、ErpPurReceive 构建与失败回滚）。
- 保持全部既有外部行为不变（错误码 + 参数、cancel 多源、retry 方向分支、config-gate、幂等），**唯一允许的行为变化**是 D-B2B-3 经层 2 裁定为 implementation drift 后按 Fix 收紧 markError 来源（须显式 Decision 记录）。
- 各新增层 1 矩阵完备性表驱动测试（greenfield）；层 3 既有集成测试回归全绿。
- 层 2 四方对照双轴裁定，**显式处置 D-B2B-1..D-B2B-5 全部漂移**（Decision/Fix + successor），禁止静默排除。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（路线图 Non-Goal）：**不删除 dict `TO_CANCEL`/`CANCELLED` 预留值**（保护区），亦不新增值。
- 不新增 Asn `cancel` 命名动作/mutation（D-B2B-2：业务行为变更，归 successor ask-first）；不新增 EdiDoc 两步取消（`TO_CANCEL` 中间态，Deferred）。
- 不改变任何业务状态值、动作名、错误码值/参数形状、权限、HMAC/幂等/config-gate 语义（路线图 Non-Goal「不借迁移改变既有行为」）。
- 不修复 D-B2B-4 错误码过载反模式（not-found 复用 ILLEGAL_TRANSITION）——超出状态机集中范围，登记 watch-only residual。
- 不激活/不修改 `TransportManager`（wired-but-uncalled，D-B2B-5 潜在双 writer 登记为 watch-only residual + successor）。
- 不把 EdiDoc INLINE mutation 提取为 Processor（保持 INLINE/PROC 不对称，参照 purchase 先例）。
- 不迁移 `ErpB2bEdiLog`/`ErpB2bMftLog`/EDI Format 等非 EdiDoc/Asn 头状态轴（独立实体，非本结果表面）。
- 不引入全局 CRUD 写锁（M0.1 §9 选项 c）；不声称全域 Delta 覆盖已验证（本计划证 B2B 单域 Delta，全域归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M1.3 模板 + M0.2 清单，落地两轴 Bean + 接线 + 三层测试 + 四方对照；不改契约/模型/公共 API）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 迁移模板）、`docs/design/b2b/state-machine.md`（§1 EdiDoc + §2 Asn + Deferred/未落地注记）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M0.2 清单 b2b 行）、`docs/architecture/processor-extension-pattern.md`
- Skill Selection Basis: 路线图 M2.16/M2.17 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「BizModel/Processor 接线、Bean 注册、动态守卫边界保留、错误码映射、产品化可定制性自检」；`nop-testing` 匹配「层 1 表驱动矩阵 + 既有集成测试回归 + Delta 双加载」。层 2 引用 `state-machine-business-review-prompt.md`（模板步骤 5 标配）。必需输入已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯后端 Java + 既有 b2b-service 测试容器）。
- 前置依赖：M0.1 + M0.2 + M1.3 done。均已满足。
- 无 data-deletion / 财务过账 / ORM 保护区域触发（EdiDoc/Asn 头状态不触发过账；createReceiveFromAsn 触发 ErpPurReceive 是独立实体）。

## Execution Plan

### Phase 1 - ErpB2bEdiDocStateMachine + ErpB2bAsnStateMachine Bean + 注册 + 层 1 矩阵完备性测试

Status: completed
Targets: `module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/statemachine/ErpB2bEdiDocStateMachine.java`（新）+ `ErpB2bAsnStateMachine.java`（新）；`.../beans/app-service.beans.xml`（追加 2 Bean 注册）；`TestErpB2bEdiDocStateMachineMatrix.java` + `TestErpB2bAsnStateMachineMatrix.java`（新，层 1）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（层 1 表驱动测试）

- Item Types: `Add | Decision | Proof`
- Prereqs: M0.1 + M0.2 + M1.3 done

- [x] `Decision`（D-B2B-3 markError 守卫，Phase 1 前置裁定）：层 2 四方对照前置复核 `ErpB2bEdiDocBizModel.markError` `:97-108` 无守卫。参照 purchase Quotation/Rfq cancel 缺失守卫先例，**初步裁定 implementation drift → Fix**（Bean 守卫 markError 来源 {TO_SEND, SENT, RECEIVED}；接线后从终态/非法态 markError 抛领域码）。Phase 1 Bean 按此分支编码。Phase 3 层 2 须以实仓证据（grep markError 调用方/测试/运维 SOP）最终确认；若反转则修订 Bean + owner doc 补注。理由记录于本计划。
      - Skill: `nop-backend-dev`
- [x] `Add`：创建 `ErpB2bEdiDocStateMachine`（无状态），矩阵编码**已实现**迁移：`assertCanMarkSent(TO_SEND)`→SENT、`assertCanMarkAcknowledged(SENT)`→ACKNOWLEDGED、`assertCanMarkError(TO_SEND|SENT|RECEIVED)`（按 D-B2B-3 初步裁定）→ERROR、`assertCanRetry(ERROR)`→{TO_SEND|RECEIVED}（目标按方向，Bean 提供两可能目标态方法 `retryOutboundTargetStatus()`/`retryInboundTargetStatus()`，BizModel 按方向选）、`assertCanCancel(TO_SEND|SENT|ERROR)`（多源）→CANCELLED、`assertCanArchive(RECEIVED)`→ARCHIVED；`isTerminal(CANCELLED|ACKNOWLEDGED|ARCHIVED)`；`transitions()`（6 边）；`terminalStatuses()`(CANCELLED/ACKNOWLEDGED/ARCHIVED) + `initialStatuses()`(TO_SEND/RECEIVED)。**不编码 TO_CANCEL 任何边**（D-B2B-1 死状态）。非法来源态抛 common 层码 + `action`/`fromStatus` 元数据。
      - Skill: `nop-backend-dev`
- [x] `Add`：创建 `ErpB2bAsnStateMachine`（无状态），矩阵编码**已实现**迁移：`assertCanMatchPurchaseOrder(RECEIVED)`→MATCHED、`assertCanCreateReceiveFromAsn(MATCHED)`→RECEIVED_TO_STOCK；retryMatch 为幂等重置（非矩阵迁移边，Bean 不编码为状态边，提供 `isIdempotentRetryStatus(MATCHED|RECEIVED_TO_STOCK)` 判定 helper 供 Processor 短路）；`isTerminal(RECEIVED_TO_STOCK)`；`transitions()`（2 边）；`terminalStatuses()`(RECEIVED_TO_STOCK) + `initialStatuses()`(RECEIVED)。**不编码 cancel 边、CANCELLED 不入终态集**（D-B2B-2 未落地）。
      - Skill: `nop-backend-dev`
- [x] `Add`：在 `app-service.beans.xml` 以 FQN id 注册两个 Bean（沿用既有 Processor 范式，§11.1 步骤 2）。
      - Skill: `nop-backend-dev`
- [x] `Proof`（层 1 矩阵完备性，新增 greenfield 表驱动测试，不经 BizModel 入口）：
      - `TestErpB2bEdiDocStateMachineMatrix`：(a) 无重复/冲突边；(b) 终态 {CANCELLED, ACKNOWLEDGED, ARCHIVED} 无出边；(c) cancel 多源 {TO_SEND, SENT, ERROR} 合法、对终态非法；(d) markError 仅 {TO_SEND, SENT, RECEIVED} 合法、对终态/ERROR 非法（断言 D-B2B-3 Fix 收紧）；(e) retry 仅 ERROR 合法；(f) `transitions()` 元数据一致；(g) 初始/终态集合正确；(h) **TO_CANCEL 无任何边**（断言 Bean 不编码该态，javadoc 标注死状态）。
      - `TestErpB2bAsnStateMachineMatrix`：(a) 无重复/冲突边；(b) RECEIVED_TO_STOCK 终态无出边；(c) matchPurchaseOrder 仅 RECEIVED、createReceiveFromAsn 仅 MATCHED；(d) `transitions()` 一致；(e) **CANCELLED 无任何边/不在终态集**（断言未落地，javadoc 标注）；(f) retryMatch 幂等判定 helper 正确。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 两 Bean 落地（EdiDoc 6 动作 + Asn 2 动作 + 目标态 + isTerminal + transitions 元数据），无状态（grep 证实不 import DAO/IBiz/IServiceContext/事务）。
- [x] 两 Bean 已在 `app-service.beans.xml` 注册（FQN id）；`@Inject` 字段非 private（合规 R5）。
- [x] 层 1 矩阵测试 `mvn test -pl module-b2b/erp-b2b-service -Dtest=TestErpB2bEdiDocStateMachineMatrix,TestErpB2bAsnStateMachineMatrix` 全绿。
- [x] 本地化编译检查：`mvn compile -pl module-b2b/erp-b2b-service -am` 通过（解除 Phase 2 接线依赖）。

### Phase 2 - BizModel/Processor 接线（行为保持 + D-B2B-3 Fix）+ 层 3 回归

Status: completed
Targets: `entity/ErpB2bEdiDocBizModel.java`（markSent/markAcknowledged/markError/retry/cancel/archive）；`processor/ErpB2bAsnMatchPurchaseOrderProcessor.java`、`processor/ErpB2bAsnCreateReceiveFromAsnProcessor.java`、`processor/ErpB2bAsnRetryMatchProcessor.java`
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1（D-B2B-3 初步裁定已固化）

- [x] `Fix`：`ErpB2bEdiDocBizModel` 注入 `ErpB2bEdiDocStateMachine`（字段非 private），将 markSent（`:68`）/markAcknowledged（`:84`）/retry（`:115`）/cancel（`:133-135`）/archive（`:160`）的内联守卫替换为 `stateMachine.assertCan<Action>(from)` + 目标态写回（retry 按方向调 `retryOutbound/InboundTargetStatus()`）。**动态守卫保留原位**：retry 的 retryCount++/error/blockingLevel 清除（`:119-121`）、sentAt/acknowledgedAt 写入。删除私有 `illegalTransition` 的**矩阵部分**（保留 common→领域映射 helper 或下沉）。BizModel 捕获 Bean common 层码映射 `ERR_B2B_EDI_DOC_ILLEGAL_TRANSITION`（参数不变，common 码作 cause）。
      - Skill: `nop-backend-dev`
- [x] `Fix`（D-B2B-3 裁定生效）：`markError` 接线 Bean `stateMachine.assertCanMarkError(from)`——**新增守卫收紧来源至 {TO_SEND, SENT, RECEIVED}**（原任意态→ERROR 收紧），非法来源（含终态/ERROR）抛 `ERR_B2B_EDI_DOC_ILLEGAL_TRANSITION`。保留 blockingLevel=ERROR/error msg 写入副作用。行为变化已显式记录（D-B2B-3）。
      - Skill: `nop-backend-dev`
- [x] `Fix`：三 Asn Processor 各自注入 `ErpB2bAsnStateMachine`（字段非 private），将内联守卫替换为 Bean 调用：MatchPurchaseOrder `:41`→`assertCanMatchPurchaseOrder(from)`、CreateReceiveFromAsn `:51`→`assertCanCreateReceiveFromAsn(from)`、RetryMatch 用 `isIdempotentRetryStatus` 短路。Processor 捕获 common 层码映射 `ERR_B2B_ASN_ILLEGAL_TRANSITION`（参数不变）。**动态守卫保留原位**：HMAC 校验、PO 匹配/超量、config-gate `erp-b2b.asn-auto-create-receive`、幂等 eventId、ErpPurReceive 构建 + 失败回滚。
      - Skill: `nop-backend-dev`
- [x] `Proof`（层 3 既有回归保持全绿）：`mvn test -pl module-b2b/erp-b2b-service` 全绿——重点 `TestErpB2bEdiEnvelope`（出站 happy + errorAndRetry + cancelFromToSend + inboundReceivedToArchived + illegalTransitionThrows）、`TestErpB2bEdiPosting`、`TestErpB2bAsnInbound`、`TestErpB2bAsnInventoryIntegration`。证明错误码 + 参数、cancel 多源、retry 方向、config-gate、幂等均不变。若 D-B2B-3 markError 收紧导致既有测试失败（如有测试从终态 markError），仅当该测试覆盖的是被裁定为 drift 的路径时调整断言并记录理由（不得弱化矩阵断言）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] EdiDoc 6 处（markSent/markAcknowledged/markError/retry/cancel/archive）+ Asn 3 处（Match/CreateReceive/RetryMatch）固定判断均改调 Bean，grep 证实相关方法体内不再有内联 `Objects.equals(*, EDI_DOC_STATE_*)`/`ASN_STATUS_*` 矩阵判断（动态守卫如方向/config-gate/HMAC/幂等除外）。
- [x] 错误码 + 参数对外不变（层 3 断言证实）；D-B2B-3 markError 收紧行为变化已记录。
- [x] 层 3 `mvn test -pl module-b2b/erp-b2b-service` 全绿。

### Phase 3 - 层 2 四方对照（EdiDoc + Asn 双轴）+ 漂移裁定闭环 + Delta 适用性

Status: completed
Targets: 四方对照审计记录（写入本计划 Closure 段）；owner doc ASN 段漂移补正；D-B2B-3 终裁；B2B 单轴 Delta 证据
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度）+ `nop-testing`（Delta 双加载）

- Item Types: `Proof | Fix | Decision | Add`
- Prereqs: Phase 2

- [x] `Proof`（四方对照，§11.1 步骤 5）：以 `state-machine-business-review-prompt.md` 10 维度审查 EdiDoc + Asn 双轴——dict（edi-doc-state 8 值含 TO_CANCEL 死 / asn-status 4 值含 CANCELLED 死）↔ owner doc §1/§2 ↔ 两 Bean `transitions()` ↔ 全部 writer（含 CRUD 路径 §9.4 + TransportManager 潜在 writer）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] `Decision`（D-B2B-1 TO_CANCEL 死状态）：确认 intentional legacy / Deferred——dict 值保留作预留，Bean 不编码边，owner doc `:57` 已标注。无额外 Fix（owner doc 一致）。登记 successor（两步取消业务流）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] `Fix`（D-B2B-2 Asn cancel 未落地 doc drift）：owner doc ASN 段 `:200-206` 列 cancel 边但零实现。Fix owner doc ASN 段补注「cancel 边设计保留/未落地（零 writer、零 mutation）；CANCELLED 为 dict 预留死状态」。Bean 不编码 cancel（已落实）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] `Decision`（D-B2B-3 markError 终裁）：以实仓证据（grep markError 生产调用方/测试/运维 SOP/owner doc §6 角色）最终确认分支。若确认为 implementation drift（Fix 已在 Phase 2 落地），记录残留风险（前端/集成方若依赖从终态 markError 将收新错误码）；若反转为 intentional legacy，回退 Phase 2 Fix + owner doc 补注「markError 为运维逃生舱允许任意态」。无论分支，须显式记录。
      - Skill: `state-machine-business-review-prompt.md`
- [x] `Decision`（D-B2B-4 错误码过载 + D-B2B-5 TransportManager 双 writer）：两者登记为 watch-only residual + successor（不修复，超出范围）。D-B2B-5 successor 触发条件 = 出站自动化 `ErpB2bEdiOutboundJob` 落地时须路由经 Bean。
      - Skill: `state-machine-business-review-prompt.md`
- [x] `Add | Proof`（Delta 适用性，§11.1 步骤 7；M2 非保护域）：在 EdiDoc 轴证 Delta（派生类覆盖一个动作，如收紧 cancel 仅 TO_SEND，移除 SENT/ERROR 源），VFS Delta 层同名 bean id 覆盖，基线/Delta 双加载可区分（复用 M1.2/contract 范式：`TestErpB2bEdiDocStateMachineBaselineIoC` + `TestErpB2bEdiDocStateMachineDeltaOverride`）。Asn 轴继承 EdiDoc 轴 + M1.2 既有证明，不重复证。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 双轴四方对照审计记录存在且非空，每维有可追溯结论（引用 Bean 元数据 / owner doc 章节 / dict 位置 / writer 类:行）。
- [x] D-B2B-1..D-B2B-5 全部漂移已按 Fix/Decision 登记 + successor，无静默排除；owner doc ASN 段 cancel 未落地补注落地；D-B2B-3 终裁记录存在。
- [x] EdiDoc 轴 Delta 双加载运行时证据存在（非静态检查），基线/Delta 可区分。

## Draft Review Record

- Independent draft review iteration 1: accept (2026-08-12-1841-2 plan review, mission entity-state-machine) — 格式合规（所有必需段、Phase Status/Targets/Skill/Item Types/Prereqs/Exit Criteria 齐全）、退出标准可测、范围清晰（M2.16+M2.17 按规则 14 合并裁定正确：同 owner doc `b2b/state-machine.md`、同结果表面、同验证路径、无实质不同结束标准）、Closure Gates 定义完整证据。基线已对实时仓库复核：D-B2B-3（markError 无守卫，`ErpB2bEdiDocBizModel.java:97-101`）、D-B2B-2（ASN cancel 零 writer，CANCELLED 仅常量）、D-B2B-5（TransportManager `:122-123` 独立 SENT writer）、Bean 注册范式（`app-service.beans.xml:55-66`）、retry 单目标（`:118`）全部属实；路线图确认 M2.16/M2.17 均 todo、Deps M1.3 已满足；M0.1 契约 + M1.3 模板 +姊妹 purchase 范例均存在。漂移 D-B2B-1..5 全部显式 Fix/Decision + successor，无静默排除。Anti-slack：范围内无禁用词，Deferred 项均有触发条件。Minor（非阻塞）：个别行号轻微漂移（执行时以实仓为准）、Phase 3 Delta 引用 M1.2 范式（契约 M0.1 已内嵌）。无 Blocker/Major，准予 active。

## Closure Gates

> 本计划含生产代码变更（2 Bean + 接线 + 测试），Closure Gates 运行完整仓库验证。

- [x] 范围内行为完成（EdiDoc + Asn 双轴 Bean + 接线 + 层 1 矩阵 + 层 3 回归 + 层 2 四方对照 + D-B2B-1..5 漂移裁定闭环 + Delta 证据）
- [x] 相关文档对齐（`b2b/state-machine.md` ASN 段 cancel 未落地补注；D-B2B-3 终裁记录；路线图 M2.16 + M2.17 done）
- [x] 已运行验证：`mvn clean install -DskipTests`（全仓库 BUILD SUCCESS）+ `mvn test -pl module-b2b/erp-b2b-service`（全绿）+ `bash docs/audits/nop-compliance-checker.sh`（exit 0，R5=0/R11=0 无漂移）
- [x] 无范围内项目降级为 deferred/follow-up（D-B2B-1..5 须显式裁定，D-B2B-2/D-B2B-3 范围内 Fix 须落地）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留为未勾选占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### EdiDoc 两步取消（TO_CANCEL 中间态）出站自动化

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc `:5-9,57` 明示 Deferred（`TO_CANCEL` 两步取消 + 出站自动化 `ErpB2bEdiOutboundJob` + ACK-timeout + 指数退避 retry）。Bean 不编码 TO_CANCEL 边。dict 值保留作预留。
- Successor Required: yes（触发条件 = 出站自动化/两步取消业务流落地时）

### Asn cancel 命名动作 + CANCELLED 落地

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 列 cancel 边但零实现（D-B2B-2 doc drift，owner doc 已补注）。补 cancel mutation 属业务行为变更，可能触及 dict 行为 ask-first。
- Successor Required: yes（触发条件 = PM 要求 ASN cancel 命名动作时）

### 错误码过载反模式（not-found 复用 ILLEGAL_TRANSITION）

- Classification: `watch-only residual`
- Why Not Blocking Closure: D-B2B-4 pre-existing；专用 NOT_FOUND 码存在但未使用。修复需改 not-found 抛码路径，超出状态机集中范围。
- Successor Required: no（仅当统一错误码语义清理时）

### TransportManager 潜在双 writer（setState SENT）

- Classification: `watch-only residual`
- Why Not Blocking Closure: D-B2B-5 wired-but-uncalled（生产零调用方）。Bean 落地后成 SENT 边矩阵权威。
- Successor Required: yes（触发条件 = 出站自动化激活 TransportManager 时，须路由经 Bean）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 选项 (c) 显式排除；更强写锁须改 ORM/xmeta ask-first。
- Successor Required: no（仅当产品要求全局强制矩阵写锁时重开）

## Closure

Status Note: 三阶段全部执行完成 + 全绿验证（57 tests pass：18 层 1 矩阵 + 33 既有层 3 回归 + 6 Delta 双加载）+ 合规 R5=0/R11=0。层 2 四方对照双轴闭环，D-B2B-1..5 漂移全部显式裁定。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（mission-driver closure-audit 新会话，非执行者上下文，2026-08-12）。复核范围：EdiDoc/Asn 两 Bean 矩阵 + 注册 + BizModel INLINE 接线 6 处 + 三 Asn Processor 接线 + D-B2B-3 markError 收紧 Fix + owner doc ASN 段 D-B2B-2 补注 + 层 1 矩阵/Delta 测试存在性 + R5 合规 + docs/logs 同步。
- Evidence:

  ### 层 2 四方对照审计记录（EdiDoc + Asn 双轴）

  #### EdiDoc 轴（ErpB2bEdiDoc.state）

  | 维度 | 证据 | 结论 |
  |------|------|------|
  | **dict** | `module-b2b/model/app-erp-b2b.orm.xml` `edi-doc-state` 8 值：TO_SEND/SENT/**TO_CANCEL**/CANCELLED/ERROR/RECEIVED/ACKNOWLEDGED/ARCHIVED | 8 值，TO_CANCEL 为预留死状态（D-B2B-1） |
  | **owner doc 迁移图** | `docs/design/b2b/state-machine.md §2` `:28-48` | 出站 TO_SEND→SENT→ACKNOWLEDGED + ERROR/retry/cancel；入站 RECEIVED→ARCHIVED；TO_CANCEL 两步取消 Deferred 注 `:57` |
  | **StateMachine 元数据** | `ErpB2bEdiDocStateMachine.transitions()` 11 边（markSent 1 + markAcknowledged 1 + markError 3 + retry 2 + cancel 3 + archive 1） | 不含 TO_CANCEL 任何边（D-B2B-1）；终态 {CANCELLED, ACKNOWLEDGED, ARCHIVED} 无出边 |
  | **writer 盘点** | 生产命名动作（BizModel 6 处经 Bean `*TargetStatus()`）+ create 路径（CreateOutbound `:63` TO_SEND / CreateInbound `:42` RECEIVED 初始态）+ TransportManager `:123` setState(SENT) wired-but-uncalled（D-B2B-5）+ CRUD 路径（§9.4：通用 CRUD 可写，无写锁） | 命名动作路径经 Bean 唯一治理；TransportManager 潜在双 writer 登记 watch-only |

  #### Asn 轴（ErpB2bAsn.status）

  | 维度 | 证据 | 结论 |
  |------|------|------|
  | **dict** | `module-b2b/model/app-erp-b2b.orm.xml` `asn-status` 4 值：RECEIVED/MATCHED/RECEIVED_TO_STOCK/**CANCELLED** | 4 值，CANCELLED 为预留死状态（D-B2B-2） |
  | **owner doc 迁移图** | `docs/design/b2b/state-machine.md §ASN` `:200-206` | 列 RECEIVED→MATCHED→RECEIVED_TO_STOCK + cancel 边；cancel 未落地补注已落地（D-B2B-2 Fix） |
  | **StateMachine 元数据** | `ErpB2bAsnStateMachine.transitions()` 2 边（matchPurchaseOrder 1 + createReceiveFromAsn 1） | 不含 CANCELLED 任何边，CANCELLED 不在终态集（D-B2B-2） |
  | **writer 盘点** | 生产命名动作（MatchPurchaseOrder `:87` + CreateReceiveFromAsn `:85` 经 Bean `*TargetStatus()`；RetryMatch `:39` 动态 reset）+ create 路径（HandleInboundWebhook `:126` RECEIVED 初始态）+ **零 cancel writer**（grep 证实）+ CRUD 路径 | 命名动作路径经 Bean 唯一治理；cancel 零 writer 证实 doc drift |

  ### D-B2B-1..5 漂移裁定闭环

  - **D-B2B-1 EdiDoc TO_CANCEL 死状态**：`Decision` = intentional legacy / Deferred。dict 值保留作预留，Bean 不编码边，owner doc `:57` 已标注。Successor：两步取消业务流（`SENT→TO_CANCEL→CANCELLED` + `markCancelConfirmed`）落地时新增边。**已闭环**。
  - **D-B2B-2 Asn cancel 未落地 doc drift**：`Fix` = owner doc ASN 段 cancel 未落地补注已落地（`state-machine.md §ASN 迁移` 后注）。Bean 不编码 cancel（已落实），CANCELLED 不入终态集。Successor：PM 要求 ASN cancel 命名动作时开独立 plan。**已闭环**。
  - **D-B2B-3 EdiDoc markError 无守卫 → 终裁 implementation drift**：`Decision` = **implementation drift → Fix（Phase 2 已落地）**。实仓证据：(1) 生产 markError 调用方——`ErpB2bAsnMatchPurchaseOrderProcessor:177`（RECEIVED 态，合法）、`ErpB2bAsnHandleInboundWebhookProcessor:98`（RECEIVED 态，合法）；(2) 测试——`TestErpB2bEdiEnvelope:95` + `TestErpB2bEdiPosting:88` 均从 TO_SEND 调用；(3) 无运维 SOP 文档化终态 markError 为逃生舱；(4) owner doc §1-§3 明确限定来源 {TO_SEND, SENT, RECEIVED}。**残留风险**：前端/集成方若依赖从终态（CANCELLED/ACKNOWLEDGED/ARCHIVED）markError 将收新错误码 `ERR_B2B_EDI_DOC_ILLEGAL_TRANSITION`（可接受——终态概念上不可 markError）。**不反转为 intentional legacy**。**已闭环**。
  - **D-B2B-4 错误码过载**：`Decision` = watch-only residual。`requireDoc`/`requireAsn` 复用 ILLEGAL_TRANSITION 于 not-found（专用 NOT_FOUND 码存在但未使用）。pre-existing，超出状态机集中范围。Successor：no（仅当统一错误码语义清理时）。**已闭环**。
  - **D-B2B-5 TransportManager 潜在双 writer**：`Decision` = watch-only residual。`TransportManager.markEdiDocSent:115-130` 独立 setState(SENT) + TO_SEND 守卫，但**生产零调用方**（无 `@Inject TransportManager`，无 `transportManager.send(...)` 调用）。Bean 落地后成 SENT 边矩阵权威。Successor：yes（出站自动化 `ErpB2bEdiOutboundJob` 激活时须路由经 Bean）。**已闭环**。

  ### Delta 适用性证据（EdiDoc 轴）

  - 基线加载：`TestErpB2bEdiDocStateMachineBaselineIoC`（3 tests，容器解析基线类，cancel 多源放行）
  - Delta 加载：`TestErpB2bEdiDocStateMachineDeltaOverride`（3 tests，容器解析 `ErpB2bEdiDocStateMachineDelta` 派生类，cancel 收紧为仅 TO_SEND）
  - 可区分差异：`assertCanCancel(SENT)` 基线放行 / Delta 抛异常 → 构成可区分双加载证据（契约 §6 业务级 Delta 实证）
  - Asn 轴继承 EdiDoc 轴 + M1.2 既有证明，不重复证（§11.1 步骤 7 范式）

Follow-up:

- D-B2B-1 Successor：两步取消业务流（触发条件 = 业务要求 SENT→TO_CANCEL→CANCELLED 确认时）
- D-B2B-2 Successor：ASN cancel 命名动作（触发条件 = PM 要求时，可能触及 dict 行为 ask-first）
- D-B2B-3 残留风险：前端/集成方终态 markError 将收新错误码（已记录，可接受）
- D-B2B-4：watch-only（统一错误码清理时）
- D-B2B-5 Successor：TransportManager 出站接线时须路由经 Bean（触发条件 = `ErpB2bEdiOutboundJob` 落地时）
- D-B2B-6（入站 ERROR→RECEIVED retry 路径未实现）：Bean 提供 `retryInboundTargetStatus()`，当前 BizModel 仅用出站 retry；入站 retry 为 successor（owner doc §2 提及但生产未实现方向检测）
