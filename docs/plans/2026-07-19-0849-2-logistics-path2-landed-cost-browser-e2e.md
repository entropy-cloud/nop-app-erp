# 2026-07-19-0849-2-logistics-path2-landed-cost-browser-e2e logistics path-2 采购运费→到岸成本自动创建浏览器层 E2E

> Plan Status: active
> Last Reviewed: 2026-07-19
> Source: `docs/plans/2026-07-17-1005-1-b2b-aps-inbound-scheduling-orchestration-e2e.md` Deferred But Adjudicated「logistics path-2 / 运费过账浏览器层」(l.177-181，1005-1 cancelled 后该项仍开放) + `docs/plans/2026-07-14-0941-2-b2b-logistics-aps-orchestration-e2e.md` Deferred But Adjudicated「logistics path-2 到岸成本自动创建 E2E（若 Explore 裁决降级）」(l.156-160)
> Related: `2026-07-14-0941-2`（b2b/aps/logistics 跨域编排 E2E，已 completed；本计划承接其 path-2 successor——0941-2 Phase 3 path-1 运费过账已作代表验证，path-2 经 config-gate 默认关闭 + 后端单测覆盖降级 Deferred）、`2026-07-11-2329-1`（logistics path-2 后端落地，已 completed）、`2026-07-14-0606-2`（inventory 到岸成本独立 E2E，已 completed）、`docs/design/logistics/state-machine.md`（path-2 业务语义已落地 §l.103）、`docs/design/finance/costing-methods.md`（landed-cost 设计权威源，`ErpInvLandedCostBizModel.java:24` 引用）、`docs/testing/e2e-runbook.md`
> Audit: required

## Current Baseline

### 已落地（不动）

- **`ErpLogShipmentBizModel.handlePurchaseReceiptDelivered`**（`module-logistics/erp-log-service/src/main/java/app/erp/log/service/entity/ErpLogShipmentBizModel.java:213`）经 config-gated `erp-log.path2-landed-cost-auto-create`（默认 false）：
  - **关闭路径**（`:217-221`）→ `publishDeliveredEvent(shipment)` + `markSettled(shipment)`（向后兼容）。
  - **开启路径** + `freightAmount > 0`（`:230-237`）→ 调 `IErpInvLandedCostBiz.generateFreightLandedCost(receiveCode, freightAmount, freightCurrencyId, null, ctx)` → 创建 DRAFT `ErpInvLandedCost`（FREIGHT 费用行）→ `publishDeliveredEvent` + `markSettled`。
  - **开启路径** + `freightAmount ≤ 0/null`（`:223-228`）→ mark SETTLED（无可分摊运费）。
  - **失败路径**（`:238-246`）→ 保持 PENDING，允许 scanForPolling/webhook 重入重试。
- **`ErpInvLandedCostBizModel.generateFreightLandedCost`** `@BizMutation`（`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/entity/ErpInvLandedCostBizModel.java:52`）委派 `ErpInvLandedCostProcessor.generateFreightLandedCost` 创建 DRAFT 到岸成本单（FREIGHT 费用行）。
- **触发链**：`handleTrackingWebhook` → `advanceTracking(DELIVERED)`（DISPATCHED → DELIVERED）→ `onDelivered`（`ErpLogShipmentBizModel:167`）→ 按 `relatedBillType` 分派 → PURCHASE_RECEIPT 走 `handlePurchaseReceiptDelivered`。
- **既有 spec**：`tests/e2e/business-actions/log-delivered-freight-posting.action.spec.ts`（0941-2 落地）已覆盖 path-1 SALES_DELIVERY FREIGHT 过账（Dr 6601/Cr 1002 + freightSettlementStatus PENDING→SETTLED）。**path-2 PURCHASE_RECEIPT 浏览器层零覆盖**。path-1 spec cleanup 仅删 Shipment + Carrier + 凭证（经 `cleanupVoucherByBillCode`），不触 SalInvoice/SalDelivery（self-contained）。
- **既有 backend 单测**：`module-logistics/erp-log-service/src/test/java/app/erp/log/service/TestErpLogPath2LandedCost.java`（0941-2 l.16 注记 + 11-2329-1 落地，cases 在 `_cases/app/erp/log/service/TestErpLogPath2LandedCost/`）覆盖 path-2 后端 generateFreightLandedCost 调用。
- **既有 webServer JVM args**（`playwright.config.ts`）：`-Derp-log.webhook-signature-required=false`（已启用，跳过 webhook 验签）+ `-Derp-log.path2-landed-cost-auto-create` **当前未启用**（默认 false）。
- **既有 path-1 spec cleanup 范式**（0941-2）：删 Shipment + Carrier + 凭证（经 `cleanupVoucherByBillCode(shipmentCode)`）；**不触 SalInvoice/SalDelivery**（path-1 spec self-contained，webhook 入口不联动 sales 域实体）。

### 缺失（本计划对象）

1. **path-2 浏览器层零覆盖**——后端 `handlePurchaseReceiptDelivered` 已落地 + 单测覆盖，但浏览器层无 spec 验证 DELIVERED → 自动创建 `ErpInvLandedCost` 链路。
2. **path-2 cleanup 范式缺失**——path-1 spec cleanup 专为 SALES_DELIVERY 设计，path-2 须扩展支持 PURCHASE_RECEIPT 关联单据（ErpPurReceive + ErpInvLandedCost）清理。

### 既有验证范式（本计划复用）

- `tests/e2e/business-actions/_helper.ts`：`createViaSave` / `callMutation` / `verifyState`。
- `tests/e2e/business-actions/log-delivered-freight-posting.action.spec.ts`（0941-2）：webhook payload 构造 + handleTrackingWebhook 调用 + freight 凭证反查范式。
- `tests/e2e/orchestration/_helper.ts`：`runP2pChain`（建 PO + Receive 链，本计划复用建 PURCHASE_RECEIPT 前置）。
- `findFirst` GraphQL 反查原语。

### 剩余差距

- 新建 1 spec 覆盖 path-2 完整链路：自包含建 ErpPurReceive（DRAFT，作为 relatedBillCode）+ Shipment（relatedBillType=PURCHASE_RECEIPT，freightAmount > 0）→ advise → completeShipment（DISPATCHED）→ handleTrackingWebhook（DELIVERED）→ 断言：
  - shipment status=DELIVERED + freightSettlementStatus=SETTLED；
  - ErpInvLandedCost 自动创建（经 `findFirst` 按 receiveCode 反查，DRAFT 状态，FREIGHT 费用行 amount=freightAmount）。
- `playwright.config.ts` webServer JVM args 须追加 `-Derp-log.path2-landed-cost-auto-create=true`。
- **`ErpInvLandedCost` cleanup 范式**——`generateFreightLandedCost` 产物须在 spec cleanup 删除（DRAFT 状态可直删，对齐 inv-landed-cost.action.spec.ts cleanup 范式）。

## Goals

- 浏览器层 E2E 1 spec（≥2 用例）：覆盖 path-2 完整链路正路径（DELIVERED → ErpInvLandedCost 自动创建 + 字段精确数值断言）+ `freightAmount ≤ 0/null` 边界（mark SETTLED 无 LandedCost 创建）。
- `playwright.config.ts` webServer JVM args 追加 config-gate 启用项。
- e2e-runbook 业务动作表 + 套件计数更新 + 0941-2/1005-1 Deferred RELEASED 登记。

## Non-Goals

- **不动后端 Java/契约/ORM/字典/种子**——path-2 后端齐备（11-2329-1 落地 + 单测覆盖），本计划纯消费侧 spec + config JVM arg + 文档。若 Explore 发现后端 bug 须 ask-first / 开 successor。
- **不做 path-1 SALES_DELIVERY 回归**——0941-2 path-1 已代表验证 onDelivered 触发面；本计划聚焦 path-2 PURCHASE_RECEIPT。
- **不做 LandedCost 后续 approve/allocate 链 E2E**——0606-2 已覆盖 LandedCost 独立 approve + CostAdjust + LANDED_COST 凭证链；本计划仅覆盖 path-2 自动创建产物（DRAFT 状态）。
- **不做 path-2 失败重试 / scanForPolling 轮询驱动 DELIVERED E2E**——0941-2 Deferred 已显式登记为 successor（不同结果面，触发条件：scanForPolling 浏览器层 E2E 需求落地时）。
- **不做 path-2 多币种 freight 汇兑**——本计划 freightAmount 用本位币；外币 freight 汇兑分支（多币种 FREIGHT 业务）属不同结果面 successor。

## Task Route

- Type: `implementation-only change`（纯消费侧浏览器层 E2E + config JVM arg）
- Owner Docs:
  - `docs/design/logistics/state-machine.md`（path-2 业务语义已落地 §l.103，本计划补 path-2 浏览器层覆盖注记）
  - `docs/design/finance/costing-methods.md`（landed-cost 设计权威源；path-2 自动创建衔接点注记：logistics DELIVERED → generateFreightLandedCost → inventory DRAFT）
  - `docs/testing/e2e-runbook.md`（业务动作表 + 套件计数）
- Skill Selection Basis: 纯浏览器层 E2E + config-gate 启用——加载 `nop-testing`（spec 范式 + GraphQL 反查 + cleanup 范式）。

## Infrastructure And Config Prereqs

- 无新基础设施。复用 0941-2 既有 webServer JVM args + 追加 `-Derp-log.path2-landed-cost-auto-create=true`。
- 独立子代理审计会话用于草案审查 + 结束审计。

## Execution Plan

### Phase 1 - Explore：后端 path-2 冷核实 + setup 工程化裁决

Status: planned
Targets: 探索笔记（不落仓库）+ plan Decision 落地
Skill: `nop-testing`

- Item Types: `Decision | Proof`
- Prereqs: 无

- [ ] `Proof`：逐行核实后端 path-2 调用栈 —— `ErpLogShipmentBizModel.handlePurchaseReceiptDelivered:213-247` + `generateFreightLandedCost` 调用参数（`shipment.relatedBillCode` 作 receiveCode + `shipment.freightAmount` + `shipment.freightCurrencyId` + null exchangeRate）+ `IErpInvLandedCostBiz.generateFreightLandedCost:52` 委派链 + `ErpInvLandedCostProcessor.generateFreightLandedCost` 产物字段（DRAFT 状态 + FREIGHT 费用行 + amount 字段）。
  - Skill: `nop-testing`
- [ ] `Proof`：核实 `ErpInvLandedCost` 字段集（`module-inventory/model/app-erp-inventory.orm.xml:1310-1376`）—— 头字段：`code` / `receiveId`（Long FK 至 ErpPurReceive，**非** `receiveCode`/`relatedBillCode`）/ `docStatus` + `approveStatus`（两字段拆分，无单 `status`）/ `totalCostAmount`（**非** `totalAmount`）/ `currencyId`；行字段 `ErpInvLandedCostLine.costElement`（**非** `costType`，常量 `ErpInvConstants.COST_ELEMENT_FREIGHT="FREIGHT"`）+ `amount`。**裁决依据**：spec 断言字段集 + GraphQL 反查路径。注意：`ErpInvLandedCostProcessor.loadReceiveByCode:271-275` 内部 resolve receiveCode→receiveId，但 spec 须按 receiveId 直查（GraphQL `findFirst` 层无 receiveCode 字段）。
  - Skill: `nop-testing`
- [ ] `Decision`：setup 工程化（须裁决项）：
  - **(a) ErpPurReceive 前置创建路径**：① 复用 `runP2pChain`（建 PO + Receive approve 完整链）；② 直 `__save` UNSUBMITTED Receive（轻量，避免 approve/入库触发污染共享 DB）。**裁决依据**：path-2 仅需 Receive.code（shipment.relatedBillCode）+ 后端 `loadReceiveByCode` 解析，不依赖 Receive approve/入库状态；优先 ② 隔离 + 减少污染。
  - **(b) Shipment setup 字段**：relatedBillType=PURCHASE_RECEIPT + relatedBillCode=Receive.code + freightAmount > 0 + freightCurrencyId（本位币）+ carrierCode（mock 网关）+ status=DRAFT 入口。
  - **(c) webhook payload 构造**：复用 0941-2 范式 `{"trackingNo":"{shipment.trackingNo}","eventType":"DELIVERED","signedBy":"E2E"}` + signature 任意（webhook-signature-required=false）。
  - **(d) ErpInvLandedCost 反查路径**（承接 M1/M2）：spec 须捕获 setup 时 `receive.id`（Long），用 GraphQL `findFirst ErpInvLandedCost(filter:{ receiveId:{ $eq: receive.id }})` 直查（**非** 按 receiveCode 反查——GraphQL 层无 receiveCode 字段）；或用 nested filter `findFirst ErpInvLandedCost(filter:{ receive:{ code:{ $eq: receive.code }}})`。**裁决依据**：GraphQL schema 实测可用性（Phase 1 Explore 核实）。
  - **(e) cleanup 范围**：删 Shipment + Carrier + ErpInvLandedCost（path-2 产物）+ ErpPurReceive（UNSUBMITTED 前置）；不删 PO（如 setup 选 ② 则无 PO）+ 不删 SalInvoice/SalDelivery（path-2 不触 sales 域，与 path-1 cleanup 范式不同——path-1 spec 仅 cleanup Shipment+Carrier+voucher，无 SalInvoice 联动）。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 后端 path-2 调用栈 4 项 file:line 锚点核实 + setup 工程化 4 Decisions 落地，可指导 Phase 2 编码。

### Phase 2 - spec 落地 + 回归

Status: planned
Targets: `tests/e2e/business-actions/log-path2-landed-cost-auto-create.action.spec.ts`（新 spec）+ `playwright.config.ts`（webServer JVM args 追加）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [ ] `Add`：`playwright.config.ts` webServer JVM args 追加 `-Derp-log.path2-landed-cost-auto-create=true`。
- [ ] `Add`：新建 `tests/e2e/business-actions/log-path2-landed-cost-auto-create.action.spec.ts`（≥2 用例）：
  - **(1) path-2 正路径**：自包含建 ErpPurReceive（docStatus=UNSUBMITTED + receiveStatus=NOT_RECEIVED，code=`E2E-RCV-{ts}`）+ ErpLogCarrier（mock 网关）+ ErpLogShipment（relatedBillType=PURCHASE_RECEIPT, relatedBillCode=Receive.code, freightAmount=100, freightCurrencyId=本位币, freightSettlementStatus=PENDING）→ advise → completeShipment（DISPATCHED）→ 构造 webhook payload → `ErpLogShipment__handleTrackingWebhook` → 断言：
    - shipment status=DELIVERED + freightSettlementStatus=SETTLED；
    - 按Phase 1 Decision (d) 反查 ErpInvLandedCost（receiveId 直查或 nested filter）→ docStatus/approveStatus + totalCostAmount + currencyId 透传；ErpInvLandedCostLine.costElement=FREIGHT + amount=100（经 nested query 或 second-level GraphQL 反查）。
  - **(2) freightAmount ≤ 0 边界**：建同上 Shipment 但 freightAmount=0（或 null）→ handleTrackingWebhook → 断言：shipment DELIVERED + freightSettlementStatus=SETTLED + 显式断言无 ErpInvLandedCost 创建（按 Decision (d) 反查返回 null）。
  - Skill: `nop-testing`
- [ ] `Proof`：`PLAYWRIGHT_PORT=8011 npx playwright test tests/e2e/business-actions/log-path2-landed-cost-auto-create.action.spec.ts --workers=1` 全绿 + business-actions 全套件回归 0 新增失败 + 抽样回归（log-delivered-freight-posting + inv-landed-cost + inv-landed-cost-reversal）。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 新 spec ≥2 用例全绿（path-2 正路径 + freightAmount 边界）+ business-actions 回归 0 新增失败。
- [ ] `playwright.config.ts` webServer JVM arg 追加项落地。

### Phase 3 - 文档对齐 + Deferred RELEASED 登记

Status: planned
Targets: `docs/testing/e2e-runbook.md` + `docs/design/logistics/state-machine.md` + `docs/design/finance/costing-methods.md` + `docs/backlog/README.md` + `docs/logs/2026/07-19.md` + `docs/plans/2026-07-17-1005-1-*.md` + `docs/plans/2026-07-14-0941-2-*.md`
Skill: `nop-testing`

- Item Types: `Add`

- [ ] `Add`：`docs/testing/e2e-runbook.md` 业务动作表 +1 logistics path-2 行 + 套件计数段补本计划增量 + webServer JVM arg 段补 path-2 启用项。
- [ ] `Add`：`docs/design/logistics/state-machine.md` §l.103 path-2 段补 path-2 浏览器层覆盖实现注记（已落地 path-2 后端语义 + 本计划补浏览器层 E2E 覆盖范围）。
- [ ] `Add`：`docs/design/finance/costing-methods.md` path-2 自动创建衔接点注记（logistics DELIVERED → generateFreightLandedCost → inventory DRAFT）。
- [ ] `Add`：`docs/backlog/README.md` +1 done 行 + `docs/logs/2026/07-19.md` 聚合日志条目（含范围/裁决/验证状态/范围纪律）。
- [ ] `Add`：0941-2 + 1005-1 Deferred 段补 `**RELEASED by 2026-07-19-0849-2**` 行 + 实施摘要（path-2 完整链路 E2E + freightAmount 边界 + config 启用）。

Exit Criteria:

- [ ] 6 处文档对齐（e2e-runbook + 2 owner-doc + backlog + logs + 2 RELEASED 登记）落地。

## Draft Review Record

- Independent draft review iteration 1: **needs revision** (`ses_0881f5906ffeUz5MIwi0LpPiSW`, 独立 general 子代理，新会话冷重播无起草者上下文，2026-07-19) — 0 BLOCKERS + 3 MAJORS + 5 MINORS。**M1** ErpInvLandedCost 字段名错（应 costElement 非 costType / totalCostAmount 非 totalAmount / receiveId 非 receiveCode / docStatus+approveStatus 拆分非单 status）；**M2** findFirst 按 receiveCode 反查错（GraphQL 层无此字段，须 receiveId 直查或 nested filter）；**M3** owner-doc `docs/design/inventory/landed-cost.md` 不存在（实际 `docs/design/finance/costing-methods.md`）；m1 Phase 3 Item 2 措辞条件化（state-machine.md:103 path-2 已存在）；m2 l.17 范围 `:238-...` 开口；m3 l.23 path-1 cleanup 误标 "SALES_DELIVERY 链"；m4 `TestErpLogPath2LandedCost` 缺全路径；m5 Receive 初始 docStatus 应 UNSUBMITTED+NOT_RECEIVED 非 DRAFT。
- **本 iter-1 修订**：依据 M1 修正字段集（receiveId/totalCostAmount/docStatus+approveStatus/costElement）；依据 M2 新增 Decision (d) 反查路径两候选（receiveId 直查 vs nested filter）；依据 M3 全文 owner-doc 替换为 `docs/design/finance/costing-methods.md`（经实时仓库核实存在 33066 bytes）；依据 m1 Phase 3 Item 2 措辞确定化；依据 m2 l.17 范围闭合 `:238-246`；依据 m3 l.23 path-1 cleanup 改"删 Shipment+Carrier+凭证"；依据 m4 `TestErpLogPath2LandedCost` 补全路径；依据 m5 Receive 初始 docStatus 改 UNSUBMITTED+NOT_RECEIVED。
- Independent draft review iteration 2: **needs revision** (`ses_088186ec8ffeXK020ERGLUqf64`, 独立 general 子代理，新会话冷重播无起草者上下文，2026-07-19) — 0 BLOCKERS / 0 MAJORS / 2 MINORS residues（内部不一致）。M1/M2/M3 全部 FIXED；m-residue-1 l.23 仍误标 SALES_DELIVERY 链；m-residue-2 l.151 Closure Gates 仍引 landed-cost.md。
- **本 iter-2 修订**：依据 m-residue-1 l.23 改"删 Shipment+Carrier+凭证（经 cleanupVoucherByBillCode）；不触 SalInvoice/SalDelivery"；依据 m-residue-2 l.151 改 costing-methods.md。
- Independent draft review iteration 3: **accept** (`ses_088167f67ffexVQjxizf6ToygS`, 独立 general 子代理，新会话冷重播无起草者上下文，2026-07-19) — 0 BLOCKERS / 0 MAJORS / 0 MINORS。m-residue-1 + m-residue-2 全部 FIXED。计划作为执行契约进入实施。

## Closure Gates

> 本计划为前端/浏览器 E2E（行为驱动结果面），纯消费侧 path-2 successor + 测试层 + config JVM arg（预期零生产 Java/契约变更）。结束前运行新增 spec + business-actions 回归 + 154 模块构建（确认 spec 变更未污染后端）。

- [ ] 范围内行为完成（path-2 完整链路 ≥2 用例 + freightAmount 边界）
- [ ] 相关文档对齐（state-machine.md + costing-methods.md + e2e-runbook + backlog/logs + 2 RELEASED）
- [ ] 已运行验证：新 spec 全绿 + business-actions 回归 0 新增失败 + `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（确认零后端污染）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

> 草案期预登记执行期可能遇到的降级项。

### path-2 失败重试 / scanForPolling 轮询驱动 DELIVERED E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 0941-2 Deferred 已显式登记；本计划仅覆盖 webhook 入口的 path-2 happy path。失败重试经 backend 单测覆盖；scanForPolling 入口同 onDelivered 路径，handleTrackingWebhook 入口已代表验证 DELIVERED + 自动创建触发面。
- Successor Required: `yes`（触发条件：scanForPolling/失败重试浏览器层 E2E 需求落地时）

### path-2 外币 freight 汇兑分支

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划 freightAmount 用本位币；外币 freight（freightCurrencyId ≠ 本位币）的汇兑分解属不同结果面 successor（对齐 EXCHANGE_GAIN_LOSS 范式）。
- Successor Required: `yes`（触发条件：外币采购运费业务需求落地时）

### LandedCost 后续 approve/allocate 链深度编排 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 0606-2 已覆盖 LandedCost 独立 approve + CostAdjust + LANDED_COST 凭证链；本计划仅覆盖 path-2 自动创建产物（DRAFT 状态）。
- Successor Required: `no`（不同结果面，由 0606-2 范式覆盖）

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立子代理（新会话）执行>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷须以显式 successor 承接，不得出现在此处>
