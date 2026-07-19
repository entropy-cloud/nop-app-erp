# 2026-07-14-0941-2-b2b-logistics-aps-orchestration-e2e B2B ASN 匹配+建收 + Logistics DELIVERED 运费过账 + APS 插单跨域编排浏览器层 E2E

> Plan Status: completed
> Mission: erp
> Work Item: 扩展域跨域编排 E2E（b2b + logistics + aps）
> Last Reviewed: 2026-07-14
> Source: `docs/plans/2026-07-14-0508-1-aps-b2b-logistics-direct-action-e2e.md` Deferred But Adjudicated 四项
> Related: `docs/plans/2026-07-14-0508-1-aps-b2b-logistics-direct-action-e2e.md`（前置计划，状态机 E2E 已落地，本计划承接其 Deferred 跨域编排项）、`docs/plans/2026-07-14-0941-1-contract-drp-orchestration-e2e.md`（同批次计划 1）
> Audit: required

## Current Baseline

aps + b2b + logistics 三域 DIRECT 状态机动作浏览器层 E2E 已由 plan 0508-1 全量落地（aps publish/archive 状态机 + scheduleForward/scheduleBackward 引擎 PLANNED 翻转；b2b EDI 信封出站/入站生命周期 + retry retryCount++ + 非法守卫；logistics advise→completeShipment→cancelShipment + mock 承运商网关），合计 14 测试全绿。零生产代码/契约/ORM/种子/config 变更。

0508-1 的四项 Deferred（跨域编排面）仍未覆盖：

1. **b2b ASN→PO 匹配编排 E2E（`matchPurchaseOrder` / `retryMatch`）**：`ErpB2bAsnBizModel.matchPurchaseOrder(asnId)` `@BizMutation`（module-b2b/erp-b2b-service/.../ErpB2bAsnBizModel.java:131）——ASN RECEIVED 态跨域读 `ErpPurOrder`/`ErpPurOrderLine` 匹配（b2b→purchase 跨域只读），匹配成功→MATCHED。`retryMatch(asnId)` `@BizMutation`（:245）重试匹配。`findUnmatchedAsns(asOfDate)` `@BizQuery`（:262）。

2. **b2b `createReceiveFromAsn` 跨域建入库草稿**：`ErpB2bAsnBizModel.createReceiveFromAsn(asnId)` `@BizMutation`（:197）——config-gated MATCHED→RECEIVED_TO_STOCK 跨域建 `ErpPurReceive` 草稿（b2b→purchase 跨域写）。

3. **aps `insertRushOrder` 插单区间重排**：`ErpApsOperationOrderBizModel.insertRushOrder(operationOrderId)` `@BizMutation`（module-aps/erp-aps-service/.../ErpApsOperationOrderBizModel.java:68）——需 PLANNED 工序前置（scheduleForward 产物），急单插入后区间重排，返回 `SchedulingResult`。

4. **logistics DELIVERED 运费过账 / path-2 到岸成本自动创建**：`ErpLogShipmentBizModel.handleTrackingWebhook(carrierCode, signature, payload)` `@BizMutation`（module-logistics/erp-log-service/.../ErpLogShipmentBizModel.java:108）——payload 为 JSON 字符串（`{"trackingNo":"...","eventType":"DELIVERED","signedBy":"..."}`），signature 为 HMAC-SHA256（secret=carrier code，`DEFAULT_WEBHOOK_SIGNATURE_REQUIRED=true`，经 config `erp-log.webhook-signature-required` 门控）。方法内解析 payload → `advanceTracking` 推进 DISPATCHED→DELIVERED → `onDelivered`（:178）：path-1（SALES_DELIVERY）经 `IErpFinVoucherBiz.post` FREIGHT 过账 / path-2（PURCHASE_RECEIPT）经 `IErpInvLandedCostBiz` 自动建到岸成本单。`scanForPolling` `@BizMutation`（:147）轮询驱动同路径。

四方法均为 DIRECT `@BizMutation`（不经 useWorkflow/useApproval xwf），浏览器层经 GraphQL `/graphql` 可达。后端逻辑已由 JUnit 集成测试覆盖（`TestErpB2bAsnInbound` / `TestErpB2bAsnInventoryIntegration` / `TestErpLogFreightPosting` / `TestErpLogPath2LandedCost` / `TestErpApsSchedulingEngine`），本计划叠加浏览器层 E2E 面。

## Goals

- b2b `matchPurchaseOrder` + `retryMatch` 浏览器层 E2E：自包含 ASN + 匹配 PO 前置 → `matchPurchaseOrder` → MATCHED 断言 + `createReceiveFromAsn` → `ErpPurReceive` 草稿创建断言 + 非法守卫。
- aps `insertRushOrder` 浏览器层 E2E：自包含 schedule + operation order → scheduleForward → PLANNED 前置 → `insertRushOrder` → `SchedulingResult` 可观测断言。
- logistics DELIVERED 运费过账浏览器层 E2E：自包含 shipment（SALES_DELIVERY）→ advise → completeShipment → `handleTrackingWebhook(DELIVERED)` → FREIGHT 凭证创建断言。path-2 到岸成本自动创建对照或归 Deferred（Explore 裁决）。
- `docs/testing/e2e-runbook.md` 业务动作表新增 3 行 + 套件计数更新。

## Non-Goals

- b2b ASN 入站完整处理链（`handleInboundWebhook` 从 EDI 文档驱动 ASN 创建；本计划聚焦 ASN 已存在后的匹配+建收面）。
- logistics `scanForPolling` 轮询驱动 DELIVERED（与 `handleTrackingWebhook` 共用 `advanceTracking` → DELIVERED 路径，本计划以 webhook 入口作代表验证；轮询 successor）。
- logistics path-2 到岸成本自动创建深度编排（若 Explore 裁决 setup 复杂度过高，降级为 Deferred But Adjudicated，path-1 运费过账已作代表验证 `onDelivered` 触发面）。
- aps `earliestCompletionDate` / `checkFeasibility` CTP 查询（@BizQuery 只读，非编排面）。
- 零生产代码/契约/ORM/种子/config 变更（纯测试 + 文档）。

## Task Route

- Type: `仅实现变更`（浏览器层 E2E spec 新增，无后端/模型/契约变更）
- Owner Docs: `docs/design/b2b/asn-processing.md`、`docs/design/logistics/carrier-integration.md`（DELIVERED → 运费过账/path-2）、`docs/design/aps/scheduling.md`（insertRushOrder）、`docs/testing/e2e-runbook.md`
- Skill Selection Basis: `nop-testing`——Playwright 浏览器层 E2E spec 编写遵循已确立的三原语 helper 范式。`nop-frontend-dev` 不适用（纯 GraphQL 层无 AMIS 定制）。
- Protected Areas: 无 ORM/ask-first 变更。webServer JVM arg 若需 config flag 变更（logistics `freight-settlement-mode` / b2b `asn-auto-create-receive`）须记录理由。

## Infrastructure And Config Prereqs

webServer `playwright.config.ts` 现有 JVM arg 覆盖基础测试需求。以下 config-gated 项需新增 webServer JVM arg（经实时核实确认）：

- **b2b `createReceiveFromAsn`**：config `erp-b2b.asn-auto-create-receive` 默认 **false**（`ErpB2bConfigs.DEFAULT_ASN_AUTO_CREATE_RECEIVE`），须追加 `-Derp-b2b.asn-auto-create-receive=true` 启用，否则方法返回 null 跳过。
- **logistics `handleTrackingWebhook` 签名验证**：config `erp-log.webhook-signature-required` 默认 **true**（`ErpLogConfigs.DEFAULT_WEBHOOK_SIGNATURE_REQUIRED`），须追加 `-Derp-log.webhook-signature-required=false` 跳过 HMAC-SHA256 验签，或在 spec 内计算 HMAC（secret=carrier code）。裁决：webServer 追加 `-Derp-log.webhook-signature-required=false` 降低 setup 复杂度。

种子 FREIGHT 业务类型科目码若缺失须补种子 COA（对齐 0215-1/0742-2 范式）。

## Execution Plan

### Phase 1 - b2b ASN→PO 匹配 + 跨域建收 E2E（matchPurchaseOrder / retryMatch / createReceiveFromAsn）

Status: completed
Targets: `tests/e2e/business-actions/b2b-asn-match-receive.action.spec.ts`
Skill: `nop-testing`

- Item Types: `Add | Proof | Explore`
- Prereqs: 无（自包含 setup）

- [x] `Explore`: 核实 ASN RECEIVED 态前置路径——经 `__save` 直置 ASN status=RECEIVED 绕过 `handleInboundWebhook` 编排（同 0508-1 b2b-edi-doc 范式）。`matchPurchaseOrder` 按 `asn.relatedBillCode`（PO code）经 `findPurchaseOrder(relatedBillCode)` 查 PO（非 partnerId 匹配），逐行按 `materialId` 匹配 + `shippedQty` vs PO line `remaining` 数量校验。`createReceiveFromAsn` config-gate `erp-b2b.asn-auto-create-receive` 默认 false，webServer JVM arg =true 启用。PO 经 `__save` 直置 approveStatus=APPROVED + docStatus=ACTIVE（matchPurchaseOrder 仅检查 isPoClosedOrCancelled，无审批状态守卫）。
  - Skill: `nop-testing`
- [x] `Add`: 新建 `b2b-asn-match-receive.action.spec.ts`——自包含 setup：建 Partner + ErpPurOrder（APPROVED）+ ErpPurOrderLine + ASN（RECEIVED, **relatedBillCode=PO.code**）+ AsnLine（materialId 匹配 PO line materialId）→ `ErpB2bAsn__matchPurchaseOrder(asnId)` → 断言 ASN status=MATCHED。
  - Skill: `nop-testing`
- [x] `Add`: `createReceiveFromAsn`——MATCHED 后 → `createReceiveFromAsn(asnId)` → 断言 `ErpPurReceive` 草稿头创建（code=`RCV-FROM-ASN-{asnCode}` + orderId=PO.id + supplierId + warehouseId + currencyId + docStatus=UNSUBMITTED + approveStatus=UNSUBMITTED + receiveStatus=NOT_RECEIVED，**仅头无行**）+ ASN status=RECEIVED_TO_STOCK。须 webServer JVM arg `erp-b2b.asn-auto-create-receive=true` 启用。
  - Skill: `nop-testing`
- [x] `Add`: 非法守卫——无匹配 PO（`relatedBillCode` 指向不存在 PO）`matchPurchaseOrder` 保持 RECEIVED 不迁移；非 RECEIVED 态 ASN `matchPurchaseOrder` 抛 `ERR_B2B_ASN_ILLEGAL_TRANSITION`；`retryMatch` 路径（先失败再重试或直接对 UNMATCHED 调用）。
  - Skill: `nop-testing`
- [x] `Proof`: `npx playwright test tests/e2e/business-actions/b2b-asn-match-receive.action.spec.ts --workers=1` 全绿（3 测试通过）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] `matchPurchaseOrder` 经 GraphQL 浏览器层可达，ASN MATCHED + 跨域读 PO 匹配可观测
- [x] `createReceiveFromAsn` 跨域建 `ErpPurReceive` 草稿可观测，或 Explore 裁决 config-gated 降级已记录

### Phase 2 - aps insertRushOrder 插单区间重排 E2E

Status: completed
Targets: `tests/e2e/business-actions/aps-rush-order.action.spec.ts`
Skill: `nop-testing`

- Item Types: `Add | Proof | Explore`
- Prereqs: 无（自包含 setup）

- [x] `Explore`: 核实 `insertRushOrder` 前置——需 PLANNED 工序（scheduleForward 产物）+ 急单窗口配置最小集。返回 `SchedulingResult` 仅含三字段：`feasible`(boolean) + `scheduledOperationIds`(List\<Long\>) + `conflicts`(List\<ConflictReport\>)。`plannedStart/plannedEnd` 在 `ErpApsOperationOrder` 实体上（非返回值），须排产后 `__get` 独立断言。区间重排效果验证：背景工序（priority=50）经 scheduleForward 排定 → 建急单（priority=10, 同 machineId, earliestStartDateT 重叠）→ insertRushOrder 将背景工序回退 DRAFT 后重排 → plannedStart 被推移。排程不可行路径（feasible=false）不可达（insertRushOrder 内部调 engine.scheduleForward 正向排产，总是 feasible=true）。非 DRAFT 工序无状态守卫（方法内置置 DRAFT）。
  - Skill: `nop-testing`
- [x] `Add`: 新建 `aps-rush-order.action.spec.ts`——自包含 setup：建 Schedule（DRAFT）+ 背景工序（priority=50）→ scheduleForward→PLANNED → 建急单工序（priority=10, 同 machineId, earliestStartDateT 重叠, latestEndDateT 兜底窗口）→ `ErpApsOperationOrder__insertRushOrder(operationOrderId)` → 断言 `SchedulingResult` 返回（feasible=true + scheduledOperationIds 含急单 id）+ 急单实体 `__get` 断言 status=PLANNED + plannedStart/plannedEnd 写回 + 背景工序实体 `__get` plannedStart 被推移（区间重排可观测）。
  - Skill: `nop-testing`
- [x] `Add`: 非法守卫——非 DRAFT 工序 `insertRushOrder` 拒绝（若后端有状态守卫）；排程不可行路径（产能不足）`SchedulingResult.feasible=false`（若 setup 可达）。**Explore 裁决**：后端无状态守卫（方法内置置 DRAFT 统一处理），feasible=false 不可达（正向排产总是 feasible=true），两项均不测。
  - Skill: `nop-testing`
- [x] `Proof`: `npx playwright test tests/e2e/business-actions/aps-rush-order.action.spec.ts --workers=1` 全绿（1 测试通过）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] `insertRushOrder` 经 GraphQL 浏览器层可达，`SchedulingResult` 结构可观测 + 急单 PLANNED + 区间重排效果断言

### Phase 3 - logistics DELIVERED 运费过账 E2E（handleTrackingWebhook → onDelivered → FREIGHT posting）

Status: completed
Targets: `tests/e2e/business-actions/log-delivered-freight-posting.action.spec.ts`
Skill: `nop-testing`

- Item Types: `Add | Proof | Explore`
- Prereqs: 无（自包含 setup）

- [x] `Explore`: 核实 DELIVERED 触发最低成本路径——`handleTrackingWebhook(carrierCode, signature, payload)` 中 payload 为 JSON 字符串 `{"trackingNo":"...","eventType":"DELIVERED","signedBy":"..."}`，方法内解析 payload → `advanceTracking(carrierStatus=DELIVERED)` 从 DISPATCHED 推进到 DELIVERED → `onDelivered`。signature 为 HMAC-SHA256（secret=carrier.code），经 webServer JVM arg `erp-log.webhook-signature-required=false` 跳过验签。`onDelivered` path-1（SALES_DELIVERY）FREIGHT 过账经 `IErpFinVoucherBiz.post` 可达（FREIGHT 业务类型 + billData 构建签名）+ `freightSettlementStatus` 初始态 PENDING→SETTLED。FREIGHT 凭证行（LogisticsFreightProvider, PREPAID 默认）：Dr 6601 销售费用 / Cr 1002 银行存款，科目经种子 COA 可达。path-2（PURCHASE_RECEIPT）到岸成本自动创建 config-gate `erp-log.path2-landed-cost-auto-create` 默认 false（关闭），降级 Deferred But Adjudicated（path-1 运费过账已作代表验证 onDelivered 触发面，path-2 后端单测 TestErpLogPath2LandedCost 覆盖）。ERR_LOG_SHIPMENT_ALREADY_DELIVERED 由 onDelivered 内 freightSettlementStatus 已 SETTLED 时抛出，经 webhook 幂等路径不可达（advanceTracking 返回 false 不触发 onDelivered），scanForPolling 捕获不向上抛，作内部守卫文档记录。
  - Skill: `nop-testing`
- [x] `Add`: 新建 `log-delivered-freight-posting.action.spec.ts`——自包含 setup：建 Shipment（relatedBillType=SALES_DELIVERY, freightSettlementStatus=PENDING, freightAmount=100, carrierCode 匹配 mock 网关）→ advise → completeShipment（DISPATCHED）→ 构造 payload JSON `{"trackingNo":"{shipment.trackingNo}","eventType":"DELIVERED","signedBy":"E2E"}` → `ErpLogShipment__handleTrackingWebhook(carrierCode, signature, payload)` → 断言 shipment status=DELIVERED + freightSettlementStatus=SETTLED + FREIGHT 凭证创建（经 `findVoucherIdByBillCode` 反查 + 凭证行断言 Dr 6601=100/Cr 1002=100）。
  - Skill: `nop-testing`
- [x] `Add`: 幂等守卫测试——已 DELIVERED+SETTLED 的 shipment 再次 `handleTrackingWebhook(DELIVERED)` 时 `advanceTracking` 返回 false（已 DELIVERED），`onDelivered` 不被调用，方法静默返回（无重复凭证、无 error 抛出）——断言凭证数不增加。`ERR_LOG_SHIPMENT_ALREADY_DELIVERED` 由 `onDelivered` 内 `freightSettlementStatus` 已 SETTLED 时抛出（经 `scanForPolling` 对 DELIVERED+SETTLED shipment 调用 `onDelivered` 可达）。
  - Skill: `nop-testing`
- [x] `Add`: path-2 到岸成本对照（若 Explore 裁决纳入）——**Explore 裁决降级 Deferred But Adjudicated**：config-gate `erp-log.path2-landed-cost-auto-create` 默认 false（关闭），path-1 运费过账已作代表验证 `onDelivered` 触发面，path-2 后端单测 TestErpLogPath2LandedCost 覆盖。
  - Skill: `nop-testing`
- [x] `Proof`: `npx playwright test tests/e2e/business-actions/log-delivered-freight-posting.action.spec.ts --workers=1` 全绿（2 测试通过）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] `handleTrackingWebhook(DELIVERED)` 经 GraphQL 浏览器层可达，shipment DELIVERED + FREIGHT 凭证创建可观测
- [x] path-2 到岸成本 Explore 裁决已记录（降级 Deferred 理由：config-gate 默认关闭 + path-1 已代表验证 + 后端单测覆盖）

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_0a1b2b705ffetAy27N2YGWZ7Xf) — 5 blocking issues: (B1) `handleTrackingWebhook` signature 实为 `(carrierCode, signature, payload)`，payload 为 JSON 字符串内含 trackingNo/eventType/signedBy，signature 为 HMAC-SHA256，config `erp-log.webhook-signature-required` 默认 true；(B2) `ERR_LOG_SHIPMENT_ALREADY_DELIVERED` 由 `onDelivered` 在 `freightSettlementStatus` 已 SETTLED 时抛出，非第二 DELIVERED webhook（后者 `advanceTracking` 返回 false 静默幂等）；(B3) `createReceiveFromAsn` 仅建头无行，docStatus=UNSUBMITTED 非 DRAFT；(B4) `matchPurchaseOrder` 按 `asn.relatedBillCode`（PO code）匹配非 partnerId，逐行按 materialId 匹配；(B5) `SchedulingResult` 仅 feasible/scheduledOperationIds/conflicts 三字段，plannedStart/plannedEnd 在实体上。
- Independent draft review iteration 2: accept (ses_0a1a9b964ffe6gIfItxgZujxom) after B1-B5 全部修正——handleTrackingWebhook 签名/payload/HMAC 修正 + webServer JVM arg `erp-log.webhook-signature-required=false`/`erp-b2b.asn-auto-create-receive=true` 明确为必需 + 匹配键 relatedBillCode 修正 + createReceiveFromAsn 断言仅头 docStatus=UNSUBMITTED + SchedulingResult 三字段断言 + 幂等守卫修正为 advanceTracking false 静默返回。全部 5 blocking issues 经实时仓库逐一核实已修正。

## Closure Gates

- [x] 范围内行为完成：3 spec（b2b-asn-match-receive / aps-rush-order / log-delivered-freight-posting）经 GraphQL 浏览器层全栈可达 + 跨域建单/过账断言 + 非法守卫
- [x] 相关文档对齐：`docs/testing/e2e-runbook.md` 业务动作表 +3 行 + 套件计数更新
- [x] 已运行验证：`npx playwright test tests/e2e/business-actions/b2b-asn-match-receive.action.spec.ts tests/e2e/business-actions/aps-rush-order.action.spec.ts tests/e2e/business-actions/log-delivered-freight-posting.action.spec.ts --workers=1` 全绿（6 测试通过）+ 既有 b2b/aps/logistics 14 spec 回归无新增失败
- [x] 无范围内项目降级为 deferred/follow-up（path-2 到岸成本降级 Deferred But Adjudicated 已记录理由并归类，非范围内项目静默移除）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### logistics scanForPolling 轮询驱动 DELIVERED E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `scanForPolling` 与 `handleTrackingWebhook` 共用 `advanceTracking` → DELIVERED → `onDelivered` 路径。`handleTrackingWebhook` 入口已作代表验证 DELIVERED + FREIGHT 过账触发面。
- Successor Required: `no`

### logistics path-2 到岸成本自动创建 E2E（若 Explore 裁决降级）

- Classification: `out-of-scope improvement`（若 Explore 裁决 config-gated 关闭或 setup 复杂度过高）
- Why Not Blocking Closure: path-1 运费过账已作代表验证 `onDelivered` 触发面。path-2（PURCHASE_RECEIPT → `IErpInvLandedCostBiz` 自动建到岸成本单）经后端单测覆盖（`TestErpLogPath2LandedCost`）。
- Successor Required: `yes`（触发条件：path-2 到岸成本自动创建浏览器层 E2E 需求落地时，或 config-gate 启用时）
- **RELEASED by 2026-07-19-0849-2**：plan `2026-07-19-0849-2-logistics-path2-landed-cost-browser-e2e.md` 全 3 phase 全绿交付——1 新 spec（2 用例）`log-path2-landed-cost-auto-create.action.spec.ts` 覆盖 path-2 完整链路正路径（advise→completeShipment→handleTrackingWebhook DELIVERED → DRAFT ErpInvLandedCost 自动创建：docStatus+approveStatus+totalCostAmount+currencyId+supplierId+allocationMethod + FREIGHT 行 costElement+amount+apPartnerId 字段精确数值断言）+ freightAmount=0 边界对照（显式断言无 LandedCost 创建）；`playwright.config.ts` webServer JVM arg 追加 `-Derp-log.path2-landed-cost-auto-create=true`；0941-2 Deferred 解除。

### b2b handleInboundWebhook 完整入站处理链 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划聚焦 ASN 已存在后的匹配 + 建收面。`handleInboundWebhook` 从 EDI 文档驱动 ASN 创建属入站处理链前置面。
- Successor Required: `yes`（触发条件：b2b EDI→ASN 自动建单浏览器层 E2E 需求落地时）

## Closure

Status Note: 3 spec（b2b-asn-match-receive / aps-rush-order / log-delivered-freight-posting）全量落地，6 测试全绿 + 既有 14 spec 回归无新增失败。webServer JVM arg 追加 `-Derp-b2b.asn-auto-create-receive=true` + `-Derp-log.webhook-signature-required=false`。path-2 到岸成本降级 Deferred But Adjudicated（config-gate 默认关闭 + path-1 已代表验证 + 后端单测覆盖）。

Closure Audit Evidence:

- Auditor / Agent: Independent sub-agent (general) — closure audit passed: 3 spec files verified + JVM args confirmed + runbook updated + plan items all [x] + backlog README ✅ + daily log updated.
- 执行证据：3 spec 文件落盘（tests/e2e/business-actions/{b2b-asn-match-receive,aps-rush-order,log-delivered-freight-posting}.action.spec.ts）+ playwright.config.ts JVM arg 追加 + e2e-runbook.md 业务动作表 +3 行 + 套件计数更新（54→57 spec）
- 验证命令：`SKIP_WEBSERVER=1 PLAYWRIGHT_PORT=8011 npx playwright test tests/e2e/business-actions/b2b-asn-match-receive.action.spec.ts tests/e2e/business-actions/aps-rush-order.action.spec.ts tests/e2e/business-actions/log-delivered-freight-posting.action.spec.ts --workers=1` → 6 passed (47.0s)
- 回归命令：同上 + b2b-edi-doc/log-shipment/aps-operation-order/aps-schedule 4 spec → 14 passed (1.7m)

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
