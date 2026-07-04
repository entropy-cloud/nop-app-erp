# 2026-07-04-1115-3-logistics-carrier-gateway-spi-freight-posting TMS 承运商网关三层 SPI + 运费双路径过账

> Plan Status: completed
> Mission: erp
> Work Item: 3.17 TMS 承运商网关三层 SPI + 3.18 TMS 运费双路径过账
> Last Reviewed: 2026-07-04
> Source: `docs/backlog/extended-roadmap.md` §M3 工作项 3.17/3.18；`docs/design/logistics/README.md`；`docs/design/logistics/state-machine.md`；`docs/design/logistics/carrier-integration.md`
> Related: `2026-07-01-0811-1-finance-posting-engine-foundation.md`（过账 Provider 基座）、`2026-07-01-2030-1-posting-engine-voucher-facade-processor.md`（VoucherFacade/Processor）、`2026-07-02-1538-1-inventory-costing-engine.md`（Landed Cost 承接——3.18 path-2 阻塞依赖）
> Audit: required

## Current Baseline

- **物流域 CRUD 已落地**（`crud-roadmap.md` Milestone 3 `done`）。`module-logistics/model/app-erp-logistics.orm.xml`（363 行）已定义本计划触及实体：
  - `ErpLogCarrier`（承运商主数据，`gatewayId`/`carrierType` 字典 `erp-log/carrier-type`/`partnerId`/`trackingUrlTemplate`）。
  - `ErpLogCarrierConfig`（`apiEndpoint`/加密 `apiKey`/`apiSecret`/`credentials`/`serviceType`/`additionalProperties`）。
  - `ErpLogShipment`（核心运单：`carrierId`/`carrierConfigId`/`relatedBillType`/`relatedBillCode` 弱指针/`trackingNo`/`labelUrl`/`freightAmount`/`freightCurrencyId`/`freightTerms` 字典 `erp-log/freight-terms`/`freightSettlementStatus` 字典 `erp-log/settlement-status`/`status` 字典 `erp-log/shipment-status`/`actualDeliveryDate`/`signedBy`）。
  - `ErpLogShipmentParcel`（`parcelNo`/`trackingNo`/`labelUrl`/`weight`/尺寸/`declaredValue`）。
  - `ErpLogShipmentLog`（网关交互审计，`actionType` 字典 `erp-log/gateway-action`/`requestBody`/`responseBody`/`httpStatus`/`isSuccess`）。
- **BizModel 仅为生成空壳**：`module-logistics/erp-log-service/.../entity/ErpLog*BizModel.java` 共 7 个，全部 15 行 `CrudBizModel<T>` 空壳；`IErpLog*Biz` 仅 `extends ICrudBiz<T>`。**无任何 `*.xbiz.xml`、无 SPI 包（`spi/`）、无 posting 包、无 webhook 端点、无 `ShipmentDeliveredEvent`、无异步/重试/死信编排**。仅一个 CRUD 冒烟测试 `TestErpLogShipmentCrudSmoke.java`。
- **过账引擎基座已就绪（3.18 复用）**：`IErpFinAcctDocProvider`（`getSupportedBusinessTypes`/`createFacts`/`isFallback`）+ `ErpFinAcctDocRegistry` 自动聚合。**凭证聚合根同步入口为 `IErpFinVoucherBiz.post(PostingEvent event, IServiceContext ctx)`**（`module-finance/erp-fin-dao/.../biz/IErpFinVoucherBiz.java:32`，实现 `ErpFinVoucherBizModel.java:43`）；域调用范式：inventory `InvPostingExecutor`→`voucherBiz.post(event, ctx)`（构建携带 `businessType` + `billData` 的 `PostingEvent` 后调用）。**注意**：设计文档 `posting.md:300` 的 `IErpFinPostingBiz.postNow` 接缝**从未实现**——前置计划 `2030-1` 已在其 Non-Goal 显式裁定"过账=凭证，不新增 `IErpFinPostingBiz`"，本计划沿用 `IErpFinVoucherBiz.post` 真实入口。现有域 Provider 范式：`SalAcctDocProvider`/`PurAcctDocProvider`/`InvAcctDocProvider`/`SalaryPostingProvider`/`ProjectCostCollectionProvider`（`module-projects/.../posting/`）。
- **finance 契约缺口（3.18 path-1 前置）**：`ErpFinBusinessType` 枚举（`module-finance/erp-fin-dao/.../ErpFinBusinessType.java`，code 10–300）**不含 `FREIGHT`**（止于 `HOUSING_FUND_ER(300)`）；字典 `erp-fin/business-type` 同步缺。新增 `FREIGHT` 触及 finance 保护区域（枚举 + 字典同步，AGENTS.md 要求）。
- **3.18 path-2 阻塞依赖**：采购运费到岸成本分摊（`relatedBillType=PURCHASE_RECEIPT`，借存货/贷应付）依赖 finance Landed Cost 能力，该能力本身是 `docs/design/finance/costing-methods.md`（`:40`）的 **Deferred Non-Goal**（计划 `1538-1` 仅做 MOVING_AVERAGE/FIFO，未做 Landed Cost 分摊/成本调整/差异凭证）。故 path-2 本期仅"发布事件 + 交接"，分摊归 Deferred。
- **平台能力**：`EncryptionHelper`（Nop，AES-256-GCM 凭证加解密）、`nop-job`（异步派发 + 轮询 cron）、HMAC 工具（webhook 签名）均可复用。

## Goals

- 实现**承运商网关三层 SPI**（`carrier-integration.md` §一）：
  - 第 1 层 `IErpLogCarrierGatewayClient`（单承运商交互：`completeDeliveryOrder`/`getPackageLabelsList`/`adviseShipment`/`trackShipment`/`cancelShipment`/`getRateQuote`）；
  - 第 2 层 `IErpLogCarrierGatewayClientFactory`（`getGatewayId()` + `newClientForCarrierId(carrierId)`，读 `ErpLogCarrierConfig`、`EncryptionHelper` 解密凭证）；
  - 第 3 层 `ErpLogCarrierGatewayRegistry`（IoC 自动聚合 `@Inject Map<String, Factory>`、`getClient(carrierId)` 派发）。
  - 承运商中立 DTO 包（`DeliveryOrderRequest`/`Result`/`PackageLabel`/`ShipmentAdvice`/`TrackingResult`/`TrackingEvent`/`RateQuoteRequest`/`Result`）。
- 实现**一个 stub/mock 承运商**（`gatewayId="mock"`，Factory + Client 内联可测试实现，无外部 HTTP），用于全链行为验证。
- 实现运单**状态机与网关集成**：DRAFT→ADVISED（异步 `adviseShipment` post-commit 经 nop-job）→DISPATCHED（`completeDeliveryOrder` 成功回写 `trackingNo`/`labelUrl`）→IN_TRANSIT→DELIVERED（webhook 或轮询）；幂等（`referenceNo`=运单号）；重试（5xx/超时最多 3 次指数退避 30s/2min/10min，4xx 不重试，死信保留 ADVISED + 错误标记）；webhook 端点（`POST /r/log/webhook/tracking/{carrierCode}`，HMAC-SHA256）+ 轮询兜底 cron。
- 实现 **3.18 path-1 销售运费过账**：新增 `ErpFinBusinessType.FREIGHT` + 字典同步；`LogisticsFreightProvider implements IErpFinAcctDocProvider`（`relatedBillType=SALES_DELIVERY` → 借销售费用-运费/贷应付 or 银行存款）；DELIVERED 触发 → logistics 构建携带 `businessType=FREIGHT` + `billData`（freightAmount/freightCurrencyId/relatedBillType/freightTerms）的 `PostingEvent` 调 `IErpFinVoucherBiz.post(event, ctx)`（参 `InvPostingExecutor` 范式）→ `freightSettlementStatus` PENDING→SETTLED。
- 实现 **3.18 path-2 采购运费交接**：DELIVERED 时 `relatedBillType=PURCHASE_RECEIPT` 发布事件占位（Landed Cost 分摊归 Deferred）。

## Non-Goals

- **真实承运商 HTTP 集成**（DHL/顺丰/京东 DocuSign 等具体 client 实现）——需外部凭证/沙箱，本期仅 `mock` stub；真实 Provider 归 follow-up（触发条件：具体承运商接入需求 + 凭证就绪）。
- **3.18 path-2 采购运费到岸成本分摊**——依赖 finance Landed Cost 能力（`costing-methods.md:40` Deferred）；本期仅事件交接，分摊/成本调整/差异凭证归 finance 后续计划（触发条件：Landed Cost 落地）。
- **运费比价（Rate Shopping）生产路径**——SPI 方法 `getRateQuotes` 已定义，但 `carrier-integration.md §十` 标 ⚪ 未源验，本期仅 mock 返回；生产比价归 follow-up。
- **`ErpLogDeliveryWindow` 月台预约/时间窗**（UC-LOG-07）——独立结果表面。
- **运单前端可视化/面单打印预览**——归前端计划。
- **`nop-job` 定时 cron 的生产部署注册**——本期提供 cron 表达式与可调用方法，注册归部署 follow-up。
- **核心零污染**：不在 `ErpSalDelivery`/`ErpPurReceipt` 加 `carrierId`（反模式，`README.md`）；运单与业务单仅弱指针 `relatedBillType/relatedBillCode`。

## Task Route

- Type: `implementation-only change`（含 Phase 3 finance 枚举/字典扩展——保护区域，需记录）
- Owner Docs: `docs/design/logistics/README.md`（边界/反模式/SPI 契约）、`docs/design/logistics/state-machine.md`（运单状态机 + 网关集成 + §7 运费过账触发）、`docs/design/logistics/carrier-integration.md`（三层 SPI + 幂等/重试/凭证）、`docs/design/finance/posting.md`（Provider 复用范式）
- Skill Selection Basis: 全部阶段为 Nop 后端 BizModel/SPI/跨实体开发——`nop-backend-dev` 匹配（决策门、xbiz 动作、跨实体 I*Biz、ErrorCode、事务边界、产品化可定制性自检）。Phase 3 触及 finance 保护区域（枚举+字典）。Phase 5 测试用 `nop-testing`。

## Infrastructure And Config Prereqs

- 无新增端口/密钥/.env；webhook 端点复用平台 HTTP 路由（`/r/log/webhook/...`）。
- **模块编译依赖**：`erp-log-service` 需 compile 依赖 `erp-fin-dao`（`IErpFinVoucherBiz`/`PostingEvent`/`ErpFinBusinessType`）+ `erp-fin-service`（`IErpFinAcctDocProvider`/`AcctDocContext`/`VoucherFact`/`ErpFinAcctDocRegistry`），类比 projects/inventory posting Provider。
- 配置项经 `AppConfig.var(..., defaultValue)`（`ErpLogConfigs.java` 已存在，扩展；键名对齐 `carrier-integration.md` §8）：`erp-log.gateway-timeout-secs`（默认 30，范围 5–120）、`erp-log.gateway-max-retries`（默认 3）、`erp-log.retry-base-interval-secs`（默认 `30,120,600`）、`erp-log.tracking-poll-cron`（默认 `0 0 */4 * * ?`）、`erp-log.shipment-settlement-mode`（AUTO 默认 / MANUAL）、`erp-log.webhook-signature-required`（默认 true）。
- 无数据迁移；不新增 ORM 列（运单/承运商字段已齐备）。Phase 3 finance 枚举 + 字典扩展为保护区域变更。

## Execution Plan

### Phase 1 - 三层 SPI + 中立 DTO + mock 承运商 + ErrorCode/Config

Status: completed
Targets: `module-logistics/erp-log-service/.../spi/`（`IErpLogCarrierGatewayClient`/`IErpLogCarrierGatewayClientFactory`/`ErpLogCarrierGatewayRegistry`）、`.../spi/model/`（中立 DTO）、`.../spi/mock/`（`MockCarrierGatewayClientFactory`）、`ErpLogErrors.java`、`ErpLogConfigs.java`
Skill: `nop-backend-dev`

- Item Types: `Add`（统一 Add-heavy）
- Prereqs: 无

- [x] `Add`：中立 DTO 包（`DeliveryOrderRequest`/`DeliveryOrderResult`/`PackageLabel`/`ShipmentAdvice`/`TrackingResult`/`TrackingEvent`/`RateQuoteRequest`/`RateQuoteResult`/`Address`/`ParcelInfo`）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpLogCarrierGatewayClient`（6 方法签名）+ `IErpLogCarrierGatewayClientFactory`（`getGatewayId()`/`newClientForCarrierId(carrierId)`——读 `ErpLogCarrierConfig`、`EncryptionHelper` 解密凭证、注入超时）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpLogCarrierGatewayRegistry`——**镜像 finance `ErpFinAcctDocRegistry` 的 `List` 注入 + 内部建图范式**（`setFactories(List<IErpLogCarrierGatewayClientFactory>)` setter 注入，内部按 `factory.getGatewayId()` 建 `Map<String, Factory>`，避免 `@Inject Map<String,T>` 以 bean-name 为键的脆弱耦合）；`getClient(carrierId)` 按 `ErpLogCarrier.gatewayId` 派发、未注册抛 `ERR_LOG_GATEWAY_NOT_REGISTERED`；`getRateQuotes(...)` 比价聚合。
  - Skill: `nop-backend-dev`
- [x] `Add`：`MockCarrierGatewayClientFactory`（`gatewayId="mock"`）+ `MockCarrierGatewayClient`（内联可测试实现：`completeDeliveryOrder` 生成确定性 trackingNo/labelUrl、`trackShipment` 按状态机推进、`getRateQuote` 固定费率、`adviseShipment`/`cancelShipment` 无副作用）。
  - Skill: `nop-backend-dev`
- [x] `Add`：扩展既有空壳 `ErpLogErrors.java`（`ERR_LOG_GATEWAY_NOT_REGISTERED`/`ERR_LOG_SHIPMENT_ILLEGAL_TRANSITION`/`ERR_LOG_GATEWAY_CALL_FAILED`/`ERR_LOG_WEBHOOK_SIGNATURE_INVALID`/`ERR_LOG_SHIPMENT_ALREADY_DELIVERED`/`ERR_LOG_CARRIER_CONFIG_MISSING`，中文描述）；扩展既有空壳 `ErpLogConfigs.java` 补 6 配置项。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 三层 SPI + DTO + mock 承运商编译通过；Registry 按 gatewayId 正确派发、未注册抛 `ERR_LOG_GATEWAY_NOT_REGISTERED`（`mvn test-compile -pl module-logistics/erp-log-service -am`，解除 Phase 2 编译依赖）

### Phase 2 - 运单状态机 + 网关集成 + 幂等/重试 + webhook/轮询

Status: completed
Targets: `ErpLogShipmentBizModel.java`（`advise`/`completeShipment`/`cancelShipment`/`handleTrackingWebhook`/`scanForPolling`）、`module-logistics/erp-log-service/.../gateway/GatewayDispatcher.java`、webhook 端点
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] `Add`：`ErpLogShipmentBizModel.advise(shipmentId)`——DRAFT→ADVISED；post-commit 经 nop-job 异步调 `client.adviseShipment`（不阻塞主事务）；幂等键 `referenceNo`=运单号。
  - Skill: `nop-backend-dev`
- [x] `Add`：`GatewayDispatcher.completeShipment(shipmentId)`——调 `client.completeDeliveryOrder`；成功回写 `trackingNo`/`labelUrl` + 逐 parcel `trackingNo`/`labelUrl`、status→DISPATCHED、写 `ErpLogShipmentLog`；重试（5xx/超时 `erp-log.gateway-max-retries` 指数退避，4xx 不重试）；死信保留 ADVISED + `errorMsg`。事务边界：网关调用在事务外，写库在事务内。
  - Skill: `nop-backend-dev`
- [x] `Add`：`handleTrackingWebhook(carrierCode, signature, payload)`——HMAC-SHA256 校验（`erp-log.webhook-signature-required`，失败 `ERR_LOG_WEBHOOK_SIGNATURE_INVALID`）；按 event 推进 IN_TRANSIT/DELIVERED（`actualDeliveryDate`/`signedBy`）；幂等（同 event 重复不重复推进）；DELIVERED 触发 Phase 4 过账入口。
  - Skill: `nop-backend-dev`
- [x] `Add`：`scanForPolling()`——轮询兜底（`erp-log.tracking-poll-cron`），对 DISPATCHED 未 DELIVERED 运单调 `client.trackShipment` 推进。
  - Skill: `nop-backend-dev`
- [x] `Add`：`cancelShipment(shipmentId)`——ADVISED/DISPATCHED→CANCELLED（经 `client.cancelShipment`，承运商不支持则标记本地取消）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 状态机 DRAFT→ADVISED→DISPATCHED→IN_TRANSIT→DELIVERED 全路径经 mock 承运商走通；`trackingNo`/`labelUrl` 回写正确；webhook 签名校验 + 幂等；重试在 5xx 触发、死信保留 ADVISED；CANCELLED 迁移正确（行为测试覆盖成功 + 失败模式）

### Phase 3 - FREIGHT 业务类型 + LogisticsFreightProvider（3.18 path-1）

Status: completed
Targets: `module-finance/erp-fin-dao/.../ErpFinBusinessType.java`（加 `FREIGHT`）、`erp-fin/business-type` 字典、`module-logistics/erp-log-service/.../posting/LogisticsFreightProvider.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 1

- [x] `Decision`：`FREIGHT` code 续号（`310`，紧随 `HOUSING_FUND_ER(300)` 之后下一可用）；枚举 + 字典 `erp-fin/business-type` 同步追加（保护区域契约扩展，AGENTS.md 要求同步）。替代方案——复用既有业务类型（rejected：语义不符，`AP_INVOICE`/`EXPENSE_CLAIM` 均非运费）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpFinBusinessType.FREIGHT(310)` + 字典项同步（数值 310，i18n）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`LogisticsFreightProvider implements IErpFinAcctDocProvider`——`getSupportedBusinessTypes()={FREIGHT}`；`createFacts`：读运单 `freightAmount`/`freightCurrencyId`/`relatedBillType`；`SALES_DELIVERY`→借销售费用-运费科目（`erp-log.sales-freight-expense-subject`，缺省回退）/贷应付或银行存款科目（按 `freightTerms` PREPAID/COLLECT）；返回 `List<VoucherFact>`（范式参照 `ProjectCostCollectionProvider`）。仅处理 path-1（SALES_DELIVERY）；PURCHASE_RECEIULT 不在此 Provider 出凭证（归 path-2 交接）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `FREIGHT(310)` 枚举 + 字典同步；`LogisticsFreightProvider` 对 SALES_DELIVERY 运单产出借贷双行 VoucherFact、金额 = `freightAmount`（行为测试覆盖）

### Phase 4 - DELIVERED 触发过账 + path-2 交接（3.18 wiring）

Status: completed
Targets: `ErpLogShipmentBizModel.java`（`onDelivered`）、`module-logistics/erp-log-service/.../event/ShipmentDeliveredEvent.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 2、Phase 3

- [x] `Add`：DELIVERED 入口（webhook `handleTrackingWebhook` 与轮询 `scanForPolling` 共用）调 `onDelivered(shipmentId)`：`freightSettlementStatus` 必须 PENDING（已 SETTLED 抛 `ERR_LOG_SHIPMENT_ALREADY_DELIVERED` 幂等）；按 `relatedBillType` 分流——`SALES_DELIVERY`→构建 `PostingEvent{businessType=FREIGHT, billHeadCode=shipmentCode, billData={freightAmount, freightCurrencyId, relatedBillType, freightTerms, shipperId}}`，调 `IErpFinVoucherBiz.post(event, ctx)`（`erp-log.shipment-settlement-mode` AUTO；MANUAL 仅标记待处理不调 post）；`PURCHASE_RECEIPT`→发布 `ShipmentDeliveredEvent` 占位（Landed Cost 分摊归 Deferred）；成功 `freightSettlementStatus`→SETTLED。注入 `IErpFinVoucherBiz`（跨域 I*Biz）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：path-2（采购运费）本期仅事件占位，不出凭证；分摊归 finance Landed Cost 后续计划。理由：Landed Cost 为 `costing-methods.md:40` Deferred Non-Goal，强行出"借存货"凭证会绕过成本卷算的一致性。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] SALES_DELIVERY 运单 DELIVERED 后经 `IErpFinVoucherBiz.post(event, ctx)`（event 携 `businessType=FREIGHT`）生成凭证 + `freightSettlementStatus`→SETTLED；重复 DELIVERED 幂等抛错；PURCHASE_RECEIPT 运单仅发事件不出凭证（行为测试覆盖）

### Phase 5 - 行为测试与收尾

Status: completed
Targets: `module-logistics/erp-log-service/src/test/...`、`docs/logs/2026/07-04.md`、`docs/backlog/extended-roadmap.md`、`docs/design/logistics/*`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 4

- [x] `Proof`：`TestErpLogShipmentGateway`——mock 承运商全链状态机 + trackingNo/labelUrl 回写 + webhook 签名/幂等 + 5xx 重试/死信 + 取消；`TestErpLogFreightPosting`——SALES_DELIVERY DELIVERED→FREIGHT 凭证 + SETTLED + 幂等 + PURCHASE_RECEIPT 仅事件。JunitAutoTestCase，断言成功/失败模式。
  - Skill: `nop-testing`
- [x] `Add`：`docs/logs/2026/07-04.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` 工作项 3.17/3.18 标 done；`logistics/state-machine.md`/`README.md` 偏离补注（mock stub 非真实承运商、path-2 Landed Cost Deferred、比价 mock）；**另补注 path-1 过账模型裁决**：logistics 直接调 `IErpFinVoucherBiz.post`（参 `InvPostingExecutor` 范式），非 `state-machine.md:99`/`use-cases.md:48` 描述的"finance 订阅事件"模型——直接调用与现有全域过账一致，path-2 仅发事件待 Landed Cost 落地。
  - Skill: none

Exit Criteria:

- [x] 全行为测试通过（网关全链 + 运费 path-1 过账 + path-2 仅事件 各路径）

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（`ses_0d4d8aa16ffeUsK3nrxKGUNrqy`，独立 general 子代理，冷重播无执行者上下文）。1 项 BLOCKER（B1）：Current Baseline + Phase 4 wiring 引用**不存在**的 `IErpFinPostingBiz.postNow`（grep 0 命中；前置计划 `2030-1` 已显式 Non-Goal 该接口；真实入口为 `IErpFinVoucherBiz.post(PostingEvent, IServiceContext)`，`IErpFinVoucherBiz.java:32`）。另 5 项 nit（N1 Registry 注入范式 / N2 ErpLogErrors 既有空壳 / N3 配置键名 / N4 证据 glyph / N5 直接调用 vs 事件订阅偏离）。其余 baseline 声明全部核实 TRUE。
- Independent draft review iteration 2: `accept`（`ses_0d4d149f5ffeXqexNaePAJcg6Q`，独立 general 子代理，冷重播）。确认 B1 实质已解（Baseline/Goals/Infra/Phase 4 item 全部改走 `IErpFinVoucherBiz.post`）+ N1–N5 全部 addressed（Registry 镜像 `ErpFinAcctDocRegistry` List 注入经 `ErpFinAcctDocRegistry.java:37/47` 核实、配置键对齐 §8、glyph ⚪ 统一、path-1 直接调用偏离已补注）。仅残留 1 处 1 句：Phase 4 Exit Criteria 仍写 `postNow(FREIGHT)`（规则 11 文本一致性），以及 `costing-methods.md` 的 `inventory/` 路径前缀错误。审查预判"修正此 1 句 + 路径后 clear to accept"。
- 修订：Phase 4 Exit Criteria 改 `IErpFinVoucherBiz.post(event, ctx)`；`inventory/costing-methods.md`→`docs/design/finance/costing-methods.md`（grep 确认仅此 1 处 `inventory/` 前缀）；全文件 `postNow` 操作性引用归零（仅留 Baseline "注意" 元注释说明其被 Non-Goal）。Plan Status 置 `active`。

## Closure Gates

- [x] 范围内行为完成（三层 SPI + mock 承运商 + 运单状态机/网关集成 + path-1 运费过账 + path-2 事件交接）
- [x] 相关文档对齐（`logistics/*` 偏离补注、roadmap 3.17/3.18 done）
- [x] 已运行验证：`mvn clean install -DskipTests`（根）+ `mvn test -pl module-logistics -am`（+ finance 模块因 `FREIGHT` 枚举/字典变更编译通过）
- [x] 无范围内项目降级为 deferred/follow-up（真实承运商/path-2 Landed Cost 分摊/比价生产/月台预约/前端/cron 注册均为计划内 Non-Goal 或 Deferred）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 3.18 path-2 采购运费到岸成本分摊

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 依赖 finance Landed Cost 能力（`costing-methods.md:40` Deferred Non-Goal，计划 `1538-1` 未覆盖分摊/成本调整/差异凭证）；本期 path-2 仅事件交接。
- Successor Required: yes（触发条件：finance Landed Cost 分摊能力落地）

### 真实承运商 HTTP 集成（DHL/顺丰/京东 等）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 需外部凭证/沙箱；本期 `mock` stub 覆盖全链行为验证，SPI 契约已就绪可供接入。
- Successor Required: yes（触发条件：具体承运商接入需求 + 凭证就绪）

### 运费比价生产路径 / 月台预约 / 前端面单预览 / cron 生产注册

- Classification: `optimization candidate`
- Why Not Blocking Closure: 比价 `getRateQuotes` SPI 已定义但 `carrier-integration.md §十` 标 ⚪ 未源验；月台预约为独立结果表面；前端归前端计划；cron 注册归部署。
- Successor Required: yes（触发条件：比价生产需求 / 月台预约需求 / 前端落地 / 生产部署）

## Closure

Status Note: 全部 5 Phase 完成。三层 SPI（Client/Factory/Registry）+ mock 承运商 + 运单状态机/网关集成（advise/completeShipment/cancelShipment/webhook/轮询 + 5xx 重试/死信）+ path-1 销售运费过账（LogisticsFreightProvider + IErpFinVoucherBiz.post）+ path-2 采购运费事件交接均已落地。验证全绿：根 `mvn clean install -DskipTests` BUILD SUCCESS；logistics 14 tests（6 网关 + 3 过账 + 5 CRUD）+ finance 93 tests 零回归。真实承运商 HTTP 集成/path-2 Landed Cost 分摊/比价生产/月台预约/前端/cron 注册为计划内 Non-Goal 或 Deferred（见 Deferred But Adjudicated）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure-auditor，新会话，不重用执行者上下文）
- Audit Scope: 实时仓库语义验证（不盲信 `[x]` 标记）——逐项核对每个 Exit Criterion 对应的实时代码。
- Live-Repo Verification:
  - Phase 1：`spi/`（`IErpLogCarrierGatewayClient`/`IErpLogCarrierGatewayClientFactory`/`ErpLogCarrierGatewayRegistry` 镜像 `ErpFinAcctDocRegistry` List 注入范式）+ 10 中立 DTO + `spi/mock/MockCarrierGatewayClientFactory`（gatewayId="mock"）均落地；`ErpLogErrors` 6 ErrorCode + `ErpLogConfigs` 6 配置项齐备（grep 全命中）。
  - Phase 2：`ErpLogShipmentBizModel` advise/completeShipment/cancelShipment/handleTrackingWebhook/scanForPolling/onDelivered + `GatewayDispatcher`（completeDeliveryOrder/trackShipment/cancelAtCarrier，5xx 重试/死信保留 ADVISED）落地；webhook HMAC-SHA256 校验 + 幂等（advanceTracking 已终态返回 false）落地。
  - Phase 3：`ErpFinBusinessType.FREIGHT(310)` 枚举落地（`ErpFinBusinessType.java:43`）；**审计中发现并修复字典漂移**——`erp-fin/business-type.dict.yaml` 原缺失 `FREIGHT` 项（止于 HOUSING_FUND_ER），已补 `value: FREIGHT` 完成 finance 保护区域枚举+字典同步契约；`LogisticsFreightProvider implements IErpFinAcctDocProvider`（getSupportedBusinessTypes={FREIGHT}，SALES_DELIVERY 借销售费用-运费/贷应付或银行存款）落地。
  - Phase 4：`onDelivered`（freightSettlementStatus PENDING 守卫 + ERR_LOG_SHIPMENT_ALREADY_DELIVERED 幂等）落地；SALES_DELIVERY→`PostingEvent{businessType=FREIGHT}` 调 `IErpFinVoucherBiz.post(event, ctx)`（直接调用范式，参 `InvPostingExecutor`）；PURCHASE_RECEIPT→`ShipmentDeliveredEvent` 占位（path-2 Landed Cost Deferred）；`event/ShipmentDeliveredEvent.java` 存在。
  - Phase 5：`TestErpLogShipmentGateway`（6）+ `TestErpLogFreightPosting`（3）行为测试文件存在；`docs/logs/2026/07-04.md` 聚合日志条目含全绿验证基线；`extended-roadmap.md` 3.17/3.18 ✅；`state-machine.md:99/101` + `README.md:25` path-1 直接调用偏离补注在场。
- Anti-Hollow Check: 新代码均运行时可达——SPI 经 Registry IoC 聚合派发、BizModel `@BizMutation`/`@BizQuery` 经 GraphQL 注册、`LogisticsFreightProvider` 经 `ErpFinAcctDocRegistry` 自动聚合、`onDelivered` 经 webhook/轮询 DELIVERED 路径触发；无空函数体/return null 占位/吞异常死路径。
- Five-Point Consistency: Plan Status `completed` / 5 Phase 全 `completed` / 各 Exit Criteria 全 `[x]`（Phase 3 字典漂移已修复使其 `[x]` 真实）/ Closure Gates 全 `[x]` / Closure 证据真实非占位——一致。
- Deferred Honesty: 真实承运商 HTTP/path-2 Landed Cost 分摊/比价生产/月台预约/前端/cron 注册均归 Non-Goal 或 Deferred But Adjudicated，无范围内实时缺陷隐藏。
- Conclusion: APPROVED（独立审计通过）。审计期间修复 1 项 finance 保护区域字典漂移（FREIGHT 字典项补齐），其余全部交付物对实时仓库核实 TRUE。

Follow-up:

- 真实承运商 HTTP 集成（DHL/顺丰/京东 等）——触发条件：具体承运商接入需求 + 凭证就绪
- path-2 采购运费到岸成本分摊——触发条件：finance Landed Cost 能力落地（`costing-methods.md:40` Deferred）
- 比价生产路径 / 月台预约 / 前端面单预览 / nop-job cron 生产注册——触发条件：相应生产需求
