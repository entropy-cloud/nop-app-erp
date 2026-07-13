# 2026-07-14-0508-1-aps-b2b-logistics-direct-action-e2e aps + b2b + logistics 域 DIRECT 业务动作浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-14
> Source: `docs/plans/2026-07-14-0215-1-assets-direct-action-e2e.md` Deferred「aps / b2b / logistics 域 DIRECT 业务动作 E2E」(Successor Required: yes, 触发条件「按域推进剩余 DIRECT 业务动作浏览器层覆盖时」——**已满足**)；同形 Deferred 亦见 `2026-07-14-0215-2` / `2026-07-14-0215-3`。AGENTS.md 当前项目阶段重点「各域细化端到端验证」。
> Related: `2026-07-14-0215-2`（DIRECT 域扩展范式源 contract+drp）、`2026-07-09-2004-1`（业务动作套件总源）、`docs/testing/e2e-runbook.md`（业务动作套件 + 调用范式）
> Audit: required

## Current Baseline

**aps 域**后端业务逻辑已全部落地（extended-roadmap M3 3.10/3.11 全 done）：

- **排产方案**（`ErpApsScheduleBizModel`）：`publish(id)` / `archive(id)` — 排产方案状态机 DRAFT→PUBLISHED→ARCHIVED。DIRECT，无 useWorkflow / 无 useApproval（`rg useWorkflow|useApproval module-aps/model/` 零命中）。
- **工序排产引擎**（`ErpApsOperationOrderBizModel`）：`scheduleForward(scheduleId)` / `scheduleBackward(scheduleId)` — 按 `ErpApsSchedule.horizonStart/horizonEnd` 拉取 DRAFT 工序排产，写回 plannedStart/EndDateT 并置 PLANNED。`insertRushOrder(operationOrderId)` — 插单区间重排。
- **ATP/CTP 查询**（`@BizQuery`）：`earliestCompletionDate` / `checkFeasibility` — 非 @BizMutation，归 Non-Goal（非业务动作 mutation）。

**b2b 域**后端业务逻辑已全部落地（extended-roadmap M3 3.19/3.20/3.21 全 done）：

- **EDI 信封状态机**（`ErpB2bEdiDocBizModel`）：出站 `createOutbound(relatedBillType,relatedBillCode)`→TO_SEND → `markSent`→SENT → `markAcknowledged`→ACKNOWLEDGED（终态）；失败 `markError`→ERROR → `retry`→TO_SEND；取消 `cancel`→CANCELLED（终态）。入站 `createInbound`→RECEIVED → `archive`→ARCHIVED（终态）。DIRECT。
  - **输入约束**：`createOutbound` 经 `ErpB2bEdiRegistry.findOutboundProviders(relatedBillType)` 查注册的 `IErpB2bEdiProvider`；无 provider 时静默返回 null（log info）。`createOutbound` 还需 `ErpB2bEdiFormat` 配置记录存在（`findFormatByCode`）。**Explore 须核实**：仓库中是否有注册的 outbound EDI Provider bean + 种子 `ErpB2bEdiFormat` 行，否则 createOutbound 返回 null 不可测——经 `__save` 预置 TO_SEND 态 ErpB2bEdiDoc 可绕过 createOutbound 直接测 markSent/markAcknowledged/cancel/markError/retry 状态迁移（对齐 0215-2 contract 经 `__save` 置入口范式）。
- **ASN 入站处理**（`ErpB2bAsnBizModel`）：`handleInboundWebhook`（webhook 非浏览器面）→ `matchPurchaseOrder(asnId)` RECEIVED→MATCHED → `createReceiveFromAsn(asnId)` config-gated MATCHED→RECEIVED_TO_STOCK（跨域建 ErpPurReceive） → `retryMatch(asnId)`（幂等）。
  - **输入约束**：`matchPurchaseOrder` 需 ASN 处于 RECEIVED + 存在匹配的 `ErpPurOrder`/`ErpPurOrderLine`（跨域 purchase 只读）。**Explore 须核实**：经 `__save` 预置 RECEIVED 态 ASN + 匹配 PO 的自包含 setup 是否可行，还是匹配逻辑依赖 posted/状态守卫。

**logistics 域**后端业务逻辑已全部落地（extended-roadmap M3 3.17/3.18 全 done）：

- **发运单状态机**（`ErpLogShipmentBizModel`）：`advise(shipmentId)` DRAFT→ADVISED → `completeShipment(shipmentId)` ADVISED→DISPATCHED（经 `GatewayDispatcher` 承运商网关下单） → `cancelShipment(shipmentId)` ADVISED/DISPATCHED→CANCELLED。`handleTrackingWebhook`（webhook 非浏览器面）/ `scanForPolling`（nop-job 批量扫描非浏览器面）。
  - **输入约束**：`advise` / `completeShipment` 经 `GatewayDispatcher` 委派承运商网关 SPI（`client.adviseShipment` / `client.completeDeliveryOrder`）。**Explore 须核实**：是否存在注册的 mock/in-memory 承运商网关实现，还是需预置 `ErpLogCarrier` + `ErpLogCarrierConfig` 配置链。若网关 SPI 无默认实现致 advise 抛错，经 `__save` 预置 ADVISED 态可测 completeShipment/cancelShipment（部分绕过）。

**浏览器层 E2E 缺口**：aps 0 个、b2b 0 个、logistics 0 个 business-action spec。三域均无 useWorkflow / 无 useApproval 标记（`rg` 核实零命中），全部 @BizMutation 浏览器层 DIRECT 可达（webhook/批量扫描动作除外）。

**E2E 基础设施就绪**：`tests/e2e/business-actions/_helper.ts` 三原语（createViaSave / callMutation / verifyState）经 14 域 38 spec 验证可复用。

> 注：aps/b2b/logistics 域交易单据**未 seed**（e2e-runbook 明示 Non-Goal 按域逐批补充）。business-action spec 全部自包含 setup（createViaSave 建测试实体），不依赖 seed 行，故无 seed 阻塞（同 0215-2 contract/drp 范式）。

## Goals

- aps + b2b + logistics 三域核心 DIRECT 业务动作经 GraphQL `/graphql` 浏览器层全栈可达性 + 状态机迁移验证
- aps 覆盖：排产方案状态机 publish/archive + 工序排产引擎 scheduleForward/scheduleBackward（若 setup 可达）
- b2b 覆盖：EDI 信封状态机出站生命周期（markSent/markAcknowledged/cancel + markError/retry）+ 入站（createInbound/archive）
- logistics 覆盖：发运单状态机 advise→completeShipment→cancelShipment 生命周期（若网关可达）
- 复用既有三原语范式验证在排产引擎型 / EDI 信封型 / 承运商网关型 BizModel 下的可复用性
- 完成 18 域 DIRECT 业务动作浏览器层覆盖里程碑（解除 2004-1 Deferred「全 18 域全业务动作覆盖（DIRECT 域剩余）」最终子集）

## Non-Goals

- **b2b `handleInboundWebhook`**——webhook 入站端点（HMAC 校验 + 异步），系统驱动非浏览器面动作，排除（同 0215-2 排除 webhook 回调口径）
- **b2b `createReceiveFromAsn`**——config-gated MATCHED→RECEIVED_TO_STOCK 跨域建 `ErpPurReceive`（采购入库草稿），属跨域编排链，归 successor（触发条件：ASN→入库编排浏览器层 E2E 需求落地时）
- **logistics `handleTrackingWebhook` / `scanForPolling`**——webhook 追踪回调 + nop-job 定时批量扫描，系统驱动非浏览器面动作，排除
- **aps ATP/CTP 查询**（`earliestCompletionDate` / `checkFeasibility`）——`@BizQuery` 非 `@BizMutation`，非业务动作 mutation，归数值断言层 successor
- **logistics DELIVERED 触发运费过账 / path-2 到岸成本自动创建**——经 webhook/轮询驱动 DELIVERED 后触发 `onDelivered`（`IErpFinVoucherBiz.post` / `IErpInvLandedCostBiz`），非浏览器面 mutation 入口，归跨域编排 successor
- **aps insertRushOrder**——插单区间重排需 PLANNED 工序前置（scheduleForward 产物）+ 急单窗口配置，复杂度高，归 successor（触发条件：插单浏览器层 E2E 需求落地时）

## Task Route

- Type: `verification work`（扩展现有 Playwright E2E 套件覆盖至 aps + b2b + logistics 域 DIRECT 业务动作）
- Owner Docs: `docs/testing/e2e-runbook.md`（业务动作套件 + 调用范式）、`docs/design/aps/scheduling.md`、`docs/design/b2b/edi-formats.md`、`docs/design/b2b/asn-processing.md`、`docs/design/logistics/README.md`
- Skill Selection Basis: 浏览器层 E2E 测试编写 → 无匹配技能（Playwright 浏览器层非 `nop-testing` 后端快照范畴）；沿用 `_helper.ts` 既有范式 → `Skill: none`

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。复用现有 Playwright 配置 + webServer JVM 参数。三域均无 config 门控开关需启用（b2b `createReceiveFromAsn` config-gated 但归 Non-Goal）。

> 若 Explore 发现 logistics 网关需 config 启用或 carrier 配置预置，在对应 Phase 内以 Decision 记录并补充 webServer JVM arg 或自包含 setup。

## Execution Plan

### Phase 1 - aps 域排产方案状态机 + 工序排产引擎 E2E

Status: completed
Targets: `tests/e2e/business-actions/aps-schedule.action.spec.ts`（新建）、`tests/e2e/business-actions/aps-operation-order.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: 无（自包含 setup）

- [x] `Decision | Explore`: **工序排产引擎 setup 可达性核实**
  - Explore `scheduleForward(scheduleId)` 的前置依赖：`ErpApsSchedule`（horizonStart/horizonEnd）+ DRAFT 态 `ErpApsOperationOrder`（materialId/workcenterId/plannedDurationMins/earliestStartDateT）是否足够，还是需 workcenter 配置链（calendar/capacity，种子 0628-1 已有 WC-001 链）。
  - Decision：**scheduleForward/scheduleBackward 经自包含 setup 可达 PLANNED 翻转**。理由：(1) ErpApsOperationOrder.workOrderId/machineId 列无 FK 约束（仅 BIGINT），Java 集成测试 TestErpApsSchedulingEngine 用任意 1L/100L 即可；(2) ErpApsSchedulingEngine 纯算法 POJO，capacity=1 单工位，无工作中心日历/产能配置依赖；(3) loadPendingOrders 按 status=DRAFT + earliestStartDateT ∈ [horizonStart,horizonEnd] 过滤，horizon 内工序必被纳入。5 测试全绿（publish/archive/illegal guard + scheduleForward/scheduleBackward）证实裁定准确。
  - Skill: none
- [x] `Add`: **排产方案状态机 spec** `aps-schedule.action.spec.ts`
  - `publish(id)`：自包含建 `ErpApsSchedule`（DRAFT，经 `__save`）→ `publish` → `verifyState` 断言 status=PUBLISHED
  - `archive(id)`：PUBLISHED 态 → `archive` → status=ARCHIVED
  - 非法迁移守卫（ARCHIVED→publish 抛 ErrorCode message token；注意 archive 允许 DRAFT|PUBLISHED 态，故 DRAFT→archive 为合法迁移非守卫路径）
  - Skill: none
- [x] `Add`: **工序排产引擎 spec** `aps-operation-order.action.spec.ts`（条件性——取决于 Explore Decision）
  - `scheduleForward(scheduleId)`：自包含建 Schedule + DRAFT OperationOrder（若可达）→ 调 `scheduleForward` → 断言 SchedulingResult 结构非空 + OperationOrder status→PLANNED + plannedStart/EndDateT 写回非空
  - `scheduleBackward(scheduleId)`：另建 Schedule + DRAFT OperationOrder → `scheduleBackward` → 断言状态翻转
  - 若 Explore 裁定不可达：本 item 移出范围记录理由，operation-order spec 不创建
  - Skill: none

Exit Criteria:

- [x] 排产方案状态机 spec 经 `npx playwright test tests/e2e/business-actions/aps-*.action.spec.ts --workers=1` 全绿（publish/archive 状态翻转经 verifyState `__get` 独立断言）
- [x] Explore Decision 已落地（scheduleForward 可达性裁定有记录），工序排产 spec 存在或已记录降级理由

### Phase 2 - b2b 域 EDI 信封状态机 E2E

Status: completed
Targets: `tests/e2e/business-actions/b2b-edi-doc.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 1 范式验证

- [x] `Decision | Explore`: **createOutbound provider 可达性核实**
  - Explore 仓库中是否有注册的 outbound `IErpB2bEdiProvider` bean（`ErpB2bEdiRegistry.findOutboundProviders`）+ 种子/可建 `ErpB2bEdiFormat` 记录。
  - Decision：(a) 若 provider + format 可达 → createOutbound 正路径测 TO_SEND 入口；(b) 若无 provider 致 createOutbound 返回 null → 经 `__save` 预置 TO_SEND 态 ErpB2bEdiDoc（含 formatId FK）直接测后续状态迁移，createOutbound 入口降级为 watch-only residual（记录理由）。
  - **执行裁定（b）**：provider（UblInvoiceEdiProvider for AR_INVOICE）已注册，但 createOutbound happy-path 经 `provider.generatePayload` 跨域查 `ErpSalInvoice` by code（缺失抛 ERR_B2B_EDI_PARSE_FAILED）。跨域 ErpSalInvoice 自包含 setup 耦合度高违反单域隔离原则，createOutbound 入口降级为 watch-only residual。经 `__save` 预置 TO_SEND 态 ErpB2bEdiDoc（formatId=null）直接测后续状态迁移，createInbound 无跨域依赖落 spec 测 RECEIVED→archive 正路径。5 测试全绿证实裁定准确。
  - Skill: none
- [x] `Add`: **EDI 信封出站生命周期 spec** `b2b-edi-doc.action.spec.ts`
  - 出站正向链：建 TO_SEND 态 ErpB2bEdiDoc（createOutbound 或 `__save` 预置，取决于 Explore Decision）→ `markSent` → `verifyState` 断言 state=SENT → `markAcknowledged` → state=ACKNOWLEDGED
  - 失败重试路径：建 TO_SEND/SENT 态 → `markError(error)` → state=ERROR → `retry` → state=TO_SEND + retryCount++
  - 取消路径：TO_SEND/SENT/ERROR 态 → `cancel` → state=CANCELLED
  - 入站路径：`createInbound(relatedBillType,relatedBillCode,rawPayload,formatCode)` → state=RECEIVED → `archive` → state=ARCHIVED
  - 非法迁移守卫（ACKNOWLEDGED→markSent 抛 ErrorCode message token；CANCELLED→retry 抛守卫）
  - state 翻转均经 verifyState `__get` 独立断言（ErpB2bEdiDoc 状态字段为 `state` 非 `status`，ORM column `name="state"`）
  - Skill: none

Exit Criteria:

- [x] EDI 信封 spec 经 `npx playwright test tests/e2e/business-actions/b2b-*.action.spec.ts --workers=1` 全绿（出站/入站状态翻转 + 重试 retryCount 递增经 verifyState 独立断言）
- [x] createOutbound Explore Decision 已落地（provider 可达性裁定有记录）

### Phase 3 - logistics 域发运单状态机 E2E

Status: completed
Targets: `tests/e2e/business-actions/log-shipment.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 1/2 范式验证

- [x] `Decision | Explore`: **承运商网关可达性核实**
  - Explore `GatewayDispatcher.advise/completeShipment` 是否依赖注册的承运商网关 SPI 实现（`client.adviseShipment`/`client.completeDeliveryOrder`）。核实仓库中是否存在 mock/in-memory 承运商实现，还是需预置 `ErpLogCarrier` + `ErpLogCarrierConfig` 配置链。
  - Decision：(a) 若网关经自包含 setup（建 Carrier + CarrierConfig + Shipment）可达状态翻转 → 完整测 advise→completeShipment→cancelShipment；(b) 若网关 SPI 无默认实现致 advise/completeShipment 抛错 → 经 `__save` 预置对应态测可达的后续动作，不可达动作降级为 watch-only residual（记录理由）。
  - **执行裁定（a）**：MockCarrierGatewayClientFactory（gatewayId="mock"）已注册为 bean（_vfs/erp/log/beans/app-service.beans.xml），默认 failureMode=SUCCESS。自包含建 ErpLogCarrier（gatewayId="mock"，无 CarrierConfig 依赖——mock 不读凭证）+ ErpLogShipment 即可让 MockClient.completeDeliveryOrder 返回 success + 写回 trackingNo/labelUrl。advise 不调 client（仅 DRAFT→ADVISED 状态迁移），completeShipment 经 client.completeDeliveryOrder（mock success），cancelShipment 经 client.cancelShipment（mock 无副作用）。三链完整可达，不降级。4 测试全绿证实裁定准确。
  - Skill: none
- [x] `Add`: **发运单状态机 spec** `log-shipment.action.spec.ts`
  - 正向链：自包含建 `ErpLogShipment`（DRAFT，含 carrierId/shipperId 等 FK 若网关需要）→ `advise` → `verifyState` 断言 status=ADVISED → `completeShipment` → status=DISPATCHED
  - 取消路径：ADVISED 态 → `cancelShipment` → status=CANCELLED；DISPATCHED 态 → `cancelShipment` → status=CANCELLED（若承运商支持）
  - 非法迁移守卫（CANCELLED→advise 抛 ErrorCode message token；DRAFT→completeShipment 抛守卫）
  - status 翻转均经 verifyState `__get` 独立断言
  - 条件性：advise/completeShipment 的可达性取决于 Explore Decision；若网关不可达则降级测可达子集
  - Skill: none

Exit Criteria:

- [x] 发运单 spec 经 `npx playwright test tests/e2e/business-actions/log-*.action.spec.ts --workers=1` 全绿（可达状态翻转经 verifyState 独立断言）
- [x] 承运商网关 Explore Decision 已落地（网关可达性裁定有记录），不可达动作已记录降级理由

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_0a2aed9f6ffeW66Dr0yHIUOdkZ) — 1 BLOCKER: B1 b2b `ErpB2bEdiDoc` 状态字段为 `state` 非 `status`（ORM column `name="state"` app-erp-b2b.orm.xml:173，BizModel 全文 getState/setState），计划 Phase 2 verifyState 断言误写 `status=`，会导致 `__get` 字段不解析返回 null 断言全断。3 NOTE 非阻塞：N3 aps DRAFT→archive 为合法迁移（archive 允许 DRAFT|PUBLISHED）条件描述混淆已修正；N4 b2b cancel 亦允许 ERROR 态已补入；N1/N2 MockCarrierGatewayClientFactory + UBL EDI Provider 已注册（Explore 大概率走 path a，fallback 保留稳妥）。
- Independent draft review iteration 2: accept (ses_0a2ac2589ffeDn47701nW4ZYNs) — B1 修复经实时仓库核实（app-erp-b2b.orm.xml:173 column `name="state"`，Phase 2 全部 `state=` 断言，BizModel getState/setState + cancel guard 允许 TO_SEND/SENT/ERROR）；aps/logistics 字段名 `status` 未误改；N3/N4 修复准确；反松弛零违规；退出标准均指定可观察行为 + verifyState 方法非空壳；Deferred 4 项均分类 + 触发条件；模板合规。草案审查已收敛，计划可作为执行契约进入实施。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。

- [x] 范围内行为完成：3 域核心 DIRECT 业务动作状态机经 GraphQL 浏览器层全栈可达 + 状态翻转 verifyState 独立断言
- [x] 相关文档对齐：`docs/testing/e2e-runbook.md` 业务动作表 +aps/b2b/logistics 行 + 套件计数更新；`docs/backlog/README.md` +done 行
- [x] 已运行验证：`npx playwright test tests/e2e/business-actions/aps-*.action.spec.ts tests/e2e/business-actions/b2b-*.action.spec.ts tests/e2e/business-actions/log-*.action.spec.ts --workers=1` 全绿 + 全套件回归无新增失败（纯测试新增/种子/config，不触及其他域 E2E）
- [x] 无范围内项目降级为 deferred/follow-up（Explore 裁定的降级须记录理由并归类，非范围内项目静默移除）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

> 草案期预登记执行期可能遇到的降级项（取决于 Explore 结果）。执行期确认后分类。

### b2b ASN→PO 匹配编排 E2E（matchPurchaseOrder / retryMatch）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `matchPurchaseOrder` 需 ASN 处于 RECEIVED + 跨域读 `ErpPurOrder`/`ErpPurOrderLine` 匹配（b2b→purchase 跨域只读）。自包含 setup 需建 ASN + 匹配 PO 双域实体，耦合度高。本计划聚焦 EDI 信封状态机（自包含单域）。
- Successor Required: `yes`（触发条件：ASN→PO 匹配浏览器层 E2E 需求落地时，或 ASN 入站编排 successor 落地时）

### b2b createReceiveFromAsn 跨域建入库草稿

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: config-gated MATCHED→RECEIVED_TO_STOCK 跨域建 `ErpPurReceive`（采购入库草稿），属跨域编排链。本计划聚焦 EDI 信封状态机。
- Successor Required: `yes`（触发条件：ASN→入库编排浏览器层 E2E 需求落地时）

### aps insertRushOrder 插单区间重排

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 需 PLANNED 工序前置（scheduleForward 产物）+ 急单窗口配置，复杂度高。本计划聚焦 publish/archive 状态机 + 正向/反向排产引擎。
- Successor Required: `yes`（触发条件：插单浏览器层 E2E 需求落地时）

### logistics DELIVERED 运费过账 / path-2 到岸成本自动创建

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 经 webhook/轮询驱动 DELIVERED 后触发 `onDelivered`（`IErpFinVoucherBiz.post` FREIGHT 过账 / `IErpInvLandedCostBiz` path-2），非浏览器面 mutation 入口（handleTrackingWebhook/scanForPolling）。本计划聚焦 advise→completeShipment→cancelShipment 浏览器面状态机。
- Successor Required: `yes`（触发条件：运费过账/path-2 到岸成本浏览器层 E2E 需求落地时）

## Closure

Status Note: 计划全 3 Phase 落地完成。3 域（aps + b2b + logistics）核心 DIRECT 业务动作状态机经 GraphQL `/graphql` 浏览器层全栈可达性 + 状态翻转 verifyState 独立断言已交付。4 新 spec 文件（14 测试）全绿：aps-schedule（publish/archive 状态机 + 守卫）、aps-operation-order（scheduleForward/scheduleBackward 引擎 PLANNED 翻转）、b2b-edi-doc（EDI 信封出站/入站生命周期 + retry retryCount++ + 非法守卫）、log-shipment（advise→completeShipment→cancelShipment + MockCarrierGatewayClientFactory 网关 + 非法守卫）。3 个 Explore Decision 全部裁定有据：Phase 1 scheduleForward 自包含 setup 可达不降级；Phase 2 createOutbound 因跨域 ErpSalInvoice 依赖降级为 watch-only residual（经 __save 预置 TO_SEND 入口）；Phase 3 mock 网关完整可达不降级。e2e-runbook 业务动作表 +aps/b2b/logistics 行 + 套件计数 38→42 spec / 284→298 测试 / 14→17 域；backlog/README.md +1 done 行。零生产代码/契约/ORM/种子/config 变更（纯测试+文档）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（ses_0a28e9ae5ffeZeKJUhq05yFVGj，新会话）
- Verdict: PASS
- Evidence:
  - 计划内部一致性：Plan Status=completed / 3 Phase Status=completed / 7 Phase items 全 [x] / 6 Exit Criteria 全 [x] / 8 Closure Gates 全 [x]，无遗留 [ ]
  - 4 spec 文件存在且测试数符合（aps-schedule=3 / aps-operation-order=2 / b2b-edi-doc=5 / log-shipment=4）
  - 关键正确性经源码核实：b2b 字段名 `state`（app-erp-b2b.orm.xml:173 column name="state"，spec verifyState 全用 `state` 无 B1 回归）；logistics MockCarrierGatewayClientFactory 注册为 bean + gatewayId="mock"（beans.xml:22-23 + ErpLogConstants.GATEWAY_ID_MOCK）；aps SchedulingResult @DataBean 暴露 feasible/scheduledOperationIds（SchedulingResult.java:14/29/37）
  - 3 Explore Decision 均落地记录，含具体理由
  - 4 Deferred 项目（b2b ASN→PO / b2b createReceiveFromAsn / aps insertRushOrder / logistics DELIVERED）全部分类 + 触发条件，无静默移除
  - 无反模式：执行者未自我审计（Closure section 占位符由本审计回填）；watch-only residual（createOutbound）三处记录（plan/spec/runbook）；无范围蔓延（Non-Goal 动作 matchPurchaseOrder/createReceiveFromAsn/insertRushOrder/onDelivered/handleTrackingWebhook 等经 grep 确认全部不出现在 4 spec 中）

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
