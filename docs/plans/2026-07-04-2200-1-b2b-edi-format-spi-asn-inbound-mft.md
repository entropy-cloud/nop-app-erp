# 2026-07-04-2200-1-b2b-edi-format-spi-asn-inbound-mft B2B EDI 格式 SPI + ASN 入站 + MFT 传输

> Plan Status: completed
> Mission: erp
> Work Item: 3.19 B2B EDI 格式 SPI + 信封状态机 / 3.20 B2B ASN 入站处理 / 3.21 B2B MFT AS2/SFTP 传输
> Last Reviewed: 2026-07-04
> Source: `docs/backlog/extended-roadmap.md` §M3 工作项 3.19/3.20/3.21；`docs/design/b2b/edi-formats.md`；`docs/design/b2b/asn-processing.md`；`docs/design/b2b/managed-file-transfer.md`
> Related: `2026-07-04-1115-3-logistics-carrier-gateway-spi-freight-posting.md`（三层 SPI + webhook/HMAC + 重试/死信范式，本计划镜像）、`2026-07-02-0300-1-purchase-invoice-payment-three-way-match.md`（采购入库 `IErpPurReceiveBiz` 承接 ASN→入库）、`2026-07-04-1452-2-finance-reversal-writeback-loop.md`（SPI Registry + 事件范式）
> Audit: required

## Current Baseline

- **B2B 域 CRUD 已落地**（`crud-roadmap.md` Milestone 3 `done`）。`module-b2b/model/app-erp-b2b.orm.xml`（548 行）已定义本计划触及的全部实体，字段已齐备，**本计划不新增 ORM 列**：
  - `ErpB2bEdiFormat`（`:116`，`code`/`formatStandard` 字典 `erp-b2b/edi-standard`/`direction` 字典 `erp-b2b/edi-direction`/`needsWebService`/`isActive`）。
  - `ErpB2bEdiDoc`（`:142`，核心状态机表：`formatId`/`relatedBillType`/`relatedBillCode`/`state` 字典 `erp-b2b/edi-doc-state`（8 态 TO_SEND/SENT/TO_CANCEL/CANCELLED/ERROR/RECEIVED/ACKNOWLEDGED/ARCHIVED 全在）/`blockingLevel` 字典 `erp-b2b/blocking-level`/`error`/`retryCount`/`attachmentFileId`/`sentAt`/`acknowledgedAt`；UNIQUE`(formatId,relatedBillType,relatedBillCode)`）。
  - `ErpB2bAsn`（`:178`，`sourceEdiDocId`/`partnerId`/`shipmentDate`/`estimatedArrivalDate`/`trackingNo`/`relatedBillType`/`relatedBillCode`/`status` 字典 `erp-b2b/asn-status`（RECEIVED/MATCHED/RECEIVED_TO_STOCK/CANCELLED））+ `ErpB2bAsnLine`（`:210`，`materialId`/`supplierPartNo`/`quantity`/`shippedQty`）。
  - `ErpB2bCodeMapping`（`:236`，`mappingType` 字典 `erp-b2b/mapping-type`（MATERIAL/PARTNER/UOM）/`internalCode`/`externalCode`/`partnerId`）。
  - `ErpB2bEdiLog`（`:261`，`ediDocId`/`direction`/`requestPayload`/`responsePayload`/`resultCode`/`resultMsg`/`logTime`）。
  - `ErpB2bPartnerProfile`（`:289`，`webhookSecret`（HMAC 密钥载体）/`partnerId`/`protocol`/`authMethod`/`certFingerprint`）+ `ErpB2bPartnerCredential`（`:327`，加密凭证）。
  - MFT 三件套：`ErpB2bMftConfig`（`:410`，`protocol`/`transportEndpoint`/`localAs2Id`/`remoteAs2Id`/`sftpUsername`/`sftpPort`/`ftpsPort`/`certId`/`maxRetries`/`retryIntervalMin`/`deadLetterEnabled`/`monitorDirectory`/`monitorIntervalSec`）、`ErpB2bMftCertificate`（`:455`，`certName`/`certType`/`algorithm`/`keySize`/`fingerprintSha256`/`expiresAt`/`isActive`）、`ErpB2bMftLog`（`:488`，`configId`/`direction`/`fileName`/`fileHash`/`messageId`/`mdnStatus`/`protocol`/`status`/`startTime`/`endTime`/`durationMs`/`errorCode`/`retryCount`/`isCompressed`/`isEncrypted`/`isSigned`）。
- **字典缺口（Phase 1 Decision）**：MFT 实体列 `ErpB2bMftConfig.protocol`/`ErpB2bMftLog.protocol`/`ErpB2bMftLog.status` **无 `ext:dict` 绑定**（裸 VARCHAR），设计文档 `managed-file-transfer.md` 引用的 `erp-b2b/mft-protocol`（AS2/SFTP/FTPS/HTTP/HTTPS）与 `erp-b2b/mft-status`（PENDING/SENT/RECEIVED/FAILED/RETRYING/DEAD_LETTER）字典**在 ORM 中不存在**。既有 `erp-b2b/protocol`（AS2/SFTP/HTTP/HTTPS/OFTP2，`:72`）与 `erp-b2b/blocking-level` 等已存在。新增两个字典为加性变更（不改既有列类型）。
- **BizModel 仅为生成空壳**：`module-b2b/erp-b2b-service/.../entity/ErpB2b*BizModel.java` 共 13 个，全部 15 行 `CrudBizModel<T>` 空壳；13 个 `IErpB2b*Biz` 仅 `extends ICrudBiz<T>`。**无任何 `*.xbiz.xml` 用户覆盖、无 `spi/` 包、无 posting/event/webhook 包、无 EDI Provider、无 TransportManager、无入站端点、无 EdiDoc 状态迁移动作**。仅一个 CRUD 冒烟测试。
- **入站 webhook 范式已验证**：logistics 域 `ErpLogShipmentBizModel.handleTrackingWebhook(carrierCode, signature, payload)` 以 `@BizMutation` 暴露（非原生 servlet，`module-logistics/erp-log-service/.../entity/ErpLogShipmentBizModel.java:89`），经 GraphQL 入口接收外部回调，HMAC-SHA256 校验 + 幂等。本计划 ASN 入站 `handleInboundWebhook(formatCode, ...)` 镜像此范式。
- **三层 SPI Registry 范式已验证**：logistics `ErpLogCarrierGatewayRegistry`（`setFactories(List<...>)` setter 注入 + 内部按 `getGatewayId()` 建图，镜像 finance `ErpFinAcctDocRegistry`，避免 `@Inject Map<String,T>` bean-name 脆弱耦合）。本计划 `ErpB2bEdiRegistry`/`ErpB2bMftTransportRegistry` 同范式。
- **跨域采购入库承接已就绪**：`IErpPurReceiveBiz`（`module-purchase/erp-pur-dao/.../biz/`）经 1132-1/0300-1 落地（入库审核→库存触发→AP 过账）。ASN→采购入库为 config-gated 可选集成（`erp-b2b.asn-auto-create-receive`，默认关）。
- **平台能力**：`EncryptionHelper`（Nop，AES-256-GCM 凭证加解密）、HMAC 工具、`nop-job`（异步/轮询）、`AppConfig.var(..., defaultValue)` 配置门控均可复用。`IDaoProvider` 跨域只读查询范式（参 contract 1115-1 InvoicePlan）。

## Goals

- 实现 **EDI 格式 SPI（3.19）**：`IErpB2bEdiProvider`（`getCode`/`getApplicability(relatedBillType)`/`generatePayload`/`parsePayload`/`needsWebService`/`handleAcknowledgement`）+ `EdiApplicability`/`ParsedPayload`/`ParsedLine` 中立 DTO；`ErpB2bEdiRegistry`（List 注入建图 + `findOutboundProviders`/`findInboundProviders`/`identifyProvider`）。
- 实现 **一个 UBL Provider 样例**（`UblDespatchAdviceEdiProvider`，入站 ASN DespatchAdvice 解析 + 一个出站 `UblInvoiceEdiProvider` 或 `UblOrderEdiProvider` 生成，纯 XML 文本处理无外部 HTTP），覆盖 builder+decoder 双向契约；X12/EDIFACT 归 Non-Goal。
- 实现 **EDI 信封状态机（3.19）**：`ErpB2bEdiDocBizModel` 状态迁移动作（→TO_SEND/TO_SEND→SENT/SENT→ACKNOWLEDGED/→ERROR/ERROR→TO_SEND 重试/→RECEIVED/RECEIVED→ARCHIVED/→CANCELLED），非法迁移抛 ErrorCode；每次迁移写 `ErpB2bEdiLog`；`UNIQUE(formatId,relatedBillType,relatedBillCode)` 守门防重。
- 实现 **代码映射解析**：`CodeMappingResolver`（出站 internal→external / 入站 external→internal，partnerId+mappingType+code 查 `ErpB2bCodeMapping`，未找到保留原值 + WARN，不阻断）。
- 实现 **MFT 传输层（3.21）**：`IErpB2bTransportAdapter` SPI（`getSupportedProtocol`/`send`/`pullInbound`）+ `ErpB2bMftTransportRegistry` + `TransportManager`（路由/重试/死信/写 `ErpB2bMftLog`）；一个 `MockTransportAdapter`（`protocol=HTTPS`，内联可测试，无外部网络）覆盖全链；`ErpB2bMftCertificate` 读写经 `EncryptionHelper`；`ErpB2bEdiDoc` 出站 SENT 经 `TransportManager.send` 接线（config-gated，`erp-b2b.transport-mode` AUTO/MANUAL）。
- 实现 **ASN 入站处理（3.20）**：`ErpB2bAsnBizModel.handleInboundWebhook`（HMAC 校验 + 幂等 `X-Event-Id`+`formatCode`）→ `identifyProvider`→`parsePayload`→ 建 `ErpB2bAsn`+`AsnLine`（代码映射）→ PO 匹配（`relatedBillCode` 查采购订单 + 物料匹配 + 数量/日期校验，`RECEIVED→MATCHED`，不匹配保留 RECEIVED）；config-gated ASN→`IErpPurReceiveBiz` 创建采购入库草稿（`MATCHED→RECEIVED_TO_STOCK`）；未匹配 PO 定时重试与超时升级归 Non-Goal（仅留查询入口 + config-gated 提示）。

## Non-Goals

- **X12（810/850/856）/ EDIFACT（INVOIC/ORDERS/DESADV）Provider 实现**——设计文档 `edi-formats.md` §四/§五 标 ⚪ 延迟到客户需求；本期仅 UBL 样例 + SPI 契约，真实多标准归 follow-up（触发条件：具体客户/伙伴 EDI 标准接入需求）。
- **真实 AS2/SFTP/FTPS 协议库集成**——需外部证书/端点/沙箱；本期仅 `MockTransportAdapter`（HTTPS 无外部网络）+ SPI 契约，真实协议适配器归 follow-up（触发条件：具体传输伙伴接入 + 证书就绪）。AS2 MDN 异步、PGP/S-MIME 加密签名管道归此 follow-up。
- **SFTP 入站轮询（`SftpPoller` 定时任务）生产注册**——设计文档 `managed-file-transfer.md` 描述的入站轮询；本期 ASN 入站走 webhook 主动推送路径（主路径），SFTP 轮询为备用路径，提供可调用方法 + cron 表达式，注册归部署 follow-up。
- **`ErpB2bEdiFormat` 自动建议/一键映射 UI、批量 CSV 导入**（`edi-formats.md` §6.3）——独立前端切片。
- **ASN 未匹配 PO 的自动定时重试匹配 + 超时升级通知**（`asn-processing.md` §4.4「48h 升级/24h 重复通知」）——本期提供 `findUnmatchedAsns` 查询入口 + config-gated 手动重试 `retryMatch`；自动 cron 注册归部署 follow-up。
- **月台/dock 预约（UC-B2B Phase 4）**——独立 WMS 结果表面。
- **`erp-b2b.error-blocks-flow=true` 阻断业务单据流转的强一致联动**——本期 ERROR 仅落 `ErpB2bEdiDoc.blockingLevel=ERROR` + 日志，不回写阻断源业务单（弱耦合弱指针原则）；强一致归 follow-up（触发条件：生产 EDI 合规要求）。
- **核心零污染**：不在 `ErpPurReceive`/`ErpSalInvoice` 加 `ediDocId`/`asnId`（反模式）；ASN/EDI 与业务单仅弱指针 `relatedBillType/relatedBillCode`。

## Task Route

- Type: `implementation-only change`（含 Phase 1 加性字典新增——b2b 域内，需记录；不触及 finance 保护区域）
- Owner Docs: `docs/design/b2b/edi-formats.md`（Format SPI + 适用性派发 + 信封状态机 + 代码映射）、`docs/design/b2b/asn-processing.md`（ASN 入站全流程）、`docs/design/b2b/managed-file-transfer.md`（MFT 协议/实体/重试死信）、`docs/design/b2b/README.md`（边界/反模式）、`docs/architecture/integration-pattern.md`（webhook + HMAC 复用）
- Skill Selection Basis: 全部阶段为 Nop 后端 BizModel/SPI/跨实体开发——`nop-backend-dev` 匹配（决策门、xbiz 动作、跨实体 I*Biz 注入、ErrorCode、事务边界、产品化可定制性自检）。Phase 1 触及 b2b 加性字典。Phase 5 测试用 `nop-testing`。

## Infrastructure And Config Prereqs

- 无新增端口/外部服务/密钥/.env；webhook 入站端点复用平台 GraphQL 路由（`@BizMutation`，参 logistics 范式）。
- **模块编译依赖**：`erp-b2b-service` 需 compile 依赖 `erp-pur-dao`（`IErpPurReceiveBiz`，ASN→入库 config-gated 集成；`IErpPurOrderBiz`，PO 匹配只读）+ `erp-sal-dao`（`IErpSalInvoiceBiz`，出站 UBL Invoice Provider 只读），类比 contract 1115-1 跨域 I*Biz。若硬注入导致服务依赖级联破坏隔离单测，降级为 `IDaoProvider` 只读（参 contract 1115-1 InvoicePlan 偏离记录）。
- 配置项经 `AppConfig.var(..., defaultValue)`（新建 `ErpB2bConfigs.java`，键名对齐 `asn-processing.md` §九 + `managed-file-transfer.md`）：`erp-b2b.enabled`（默认 false）、`erp-b2b.asn.auto-match-retry-interval`（默认 30，分钟）、`erp-b2b.asn.match-timeout-hours`（默认 48）、`erp-b2b.asn.partial-receipt-enabled`（默认 true）、`erp-b2b.asn-auto-create-receive`（默认 false，核心零污染门控）、`erp-b2b.transport-mode`（AUTO/MANUAL 默认 MANUAL）、`erp-b2b.mft-max-retries`（默认 3）、`erp-b2b.webhook-signature-required`（默认 true）、`erp-b2b.error-blocks-flow`（默认 false）。
- 无数据迁移；不新增 ORM 列（实体字段已齐备）。Phase 1 新增 `erp-b2b/mft-protocol`/`erp-b2b/mft-status` 字典为加性变更。

## Execution Plan

### Phase 1 - EDI Format SPI + Registry + UBL Provider + 代码映射 + 字典 + ErrorCode/Config

Status: completed
Targets: `module-b2b/erp-b2b-service/.../spi/`（`IErpB2bEdiProvider`/`EdiApplicability`/`ParsedPayload`/`ParsedLine`）、`.../spi/registry/ErpB2bEdiRegistry.java`、`.../spi/ubl/`（`UblDespatchAdviceEdiProvider`/出站 UBL Provider）、`.../codemapping/CodeMappingResolver.java`、`module-b2b/model/app-erp-b2b.orm.xml`（加性字典）、`ErpB2bErrors.java`、`ErpB2bConfigs.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无

- [x] `Decision`：MFT 字典裁定。新增 `erp-b2b/mft-protocol`（AS2/SFTP/FTPS/HTTP/HTTPS）+ `erp-b2b/mft-status`（PENDING/SENT/RECEIVED/FAILED/RETRYING/DEAD_LETTER）字典；`ErpB2bMftConfig.protocol`/`ErpB2bMftLog.protocol` 绑 `erp-b2b/mft-protocol`、`ErpB2bMftLog.status` 绑 `erp-b2b/mft-status`。替代方案——复用既有 `erp-b2b/protocol`（rejected：缺 FTPS，且 MFT 语义独立于伙伴档案 protocol）。残留风险：字典绑定属加性列元数据变更，需重新 codegen 该列渲染（不影响数据）。理由写入本计划。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpB2bEdiProvider`（6 方法签名）+ `EdiApplicability`（outbound/inbound/batchable）+ `ParsedPayload`/`ParsedLine` 中立 DTO（`ediFormatCode`/`relatedBillType`/`relatedBillCode`/`partnerCode`/`lines`/`headers`/`rawPayload`）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpB2bEdiRegistry`——**镜像 logistics `ErpLogCarrierGatewayRegistry` / finance `ErpFinAcctDocRegistry` 的 List 注入建图范式**（`setProviders(List<IErpB2bEdiProvider>)` setter 注入，内部按 `getCode()` 建 `Map<String, Provider>`）；`getProvider(formatCode)`/`findOutboundProviders(relatedBillType)`/`findInboundProviders(relatedBillType)`/`identifyProvider(payload)`（逐 provider 试 parsePayload，首个不抛异常者匹配）；未注册/未识别抛 `ERR_B2B_EDI_FORMAT_NOT_REGISTERED`/`ERR_B2B_EDI_FORMAT_UNIDENTIFIED`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`UblDespatchAdviceEdiProvider`（`getCode="UBL_DESPATCH_ADVICE"`，`getApplicability("ASN_INBOUND")→{inbound:true}`）——`parsePayload` 解析 UBL DespatchAdvice XML（DespatchSupplierParty→partnerCode、DespatchLine→ParsedLine、OrderReference→relatedBillCode），纯文本 XML 解析无外部依赖；`UblInvoiceEdiProvider`（`getCode="UBL_INVOICE"`，`getApplicability("AR_INVOICE")→{outbound:true}`）`generatePayload` 构建 UBL Invoice XML（查 `IErpSalInvoiceBiz` 只读 + 客户 + 代码映射）。Provider 经代码映射解析物料。
  - Skill: `nop-backend-dev`
- [x] `Add`：`CodeMappingResolver`——`resolveOutbound(partnerId, mappingType, internalCode)`/`resolveInbound(partnerId, mappingType, externalCode)` 查 `IErpB2bCodeMappingBiz`（经注入 I*Biz）；未找到返回原值 + WARN 日志（不阻断，`blockingLevel=WARN`）。
  - Skill: `nop-backend-dev`
- [x] `Add`：扩展既有空壳 `ErpB2bErrors.java`（`ERR_B2B_EDI_FORMAT_NOT_REGISTERED`/`ERR_B2B_EDI_FORMAT_UNIDENTIFIED`/`ERR_B2B_EDI_DOC_ILLEGAL_TRANSITION`/`ERR_B2B_EDI_DOC_ALREADY_PROCESSED`/`ERR_B2B_EDI_PARSE_FAILED`/`ERR_B2B_ASN_ILLEGAL_TRANSITION`/`ERR_B2B_WEBHOOK_SIGNATURE_INVALID`/`ERR_B2B_WEBHOOK_DUPLICATE_EVENT`/`ERR_B2B_MFT_CONFIG_MISSING`/`ERR_B2B_MFT_ADAPTER_NOT_REGISTERED`，中文描述）；扩展既有空壳 `ErpB2bConfigs.java` 补 9 配置项。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] SPI + DTO + UBL Provider + Registry + CodeMappingResolver 编译通过；Registry 按 formatCode/方向正确派发、未注册/未识别抛对应 ErrorCode；代码映射双向解析正确（命中→映射值、未命中→原值 + WARN）；MFT 字典加性新增（`mvn test-compile -pl module-b2b/erp-b2b-service -am`，解除 Phase 2/3/4 编译依赖）

### Phase 2 - EDI 信封状态机 + 出站生成/发送 wiring + ErpB2bEdiLog

Status: completed
Targets: `ErpB2bEdiDocBizModel.java`（`createOutbound`/`markSent`/`markAcknowledged`/`markError`/`retry`/`cancel`/`createInbound`/`archive`）、`ErpB2bEdiLogBizModel.java`（log 写入辅助）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] `Add`：`ErpB2bEdiDocBizModel.createOutbound(relatedBillType, relatedBillCode)`——按 `ErpB2bEdiRegistry.findOutboundProviders` 派发，调 `provider.generatePayload`，建 `ErpB2bEdiDoc(state=TO_SEND, formatId, attachmentFileId=生成的报文)`；`UNIQUE(formatId,relatedBillType,relatedBillCode)` 冲突抛 `ERR_B2B_EDI_DOC_ALREADY_PROCESSED`；写 `ErpB2bEdiLog`（**actionType 语义编码到既有列**：`direction=OUTBOUND`/`resultCode`/`resultMsg="SEND: ..."`——`ErpB2bEdiLog` 无 `actionType`/`httpStatus` 列，design `edi-formats.md §8.1` 列出但 ORM 未落地，本期不新增列，动作类型经 `direction`+`resultMsg` 表达）。无适用 Provider 静默跳过 + INFO 日志。
  - Skill: `nop-backend-dev`
- [x] `Add`：`markSent(ediDocId)`（TO_SEND→SENT，写 `sentAt`）、`markAcknowledged`（SENT→ACKNOWLEDGED 终态，写 `acknowledgedAt`）、`markError(ediDocId, error)`（任意→ERROR，`retryCount` 不变）、`retry(ediDocId)`（ERROR→TO_SEND，`retryCount++`）、`cancel(ediDocId)`（TO_SEND/SENT→CANCELLED 终态）；非法迁移抛 `ERR_B2B_EDI_DOC_ILLEGAL_TRANSITION`；每次迁移写 `ErpB2bEdiLog`（动作经 `direction`+`resultCode`+`resultMsg` 表达，如 RECEIVE/ACKNOWLEDGE/CANCEL）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`createInbound(relatedBillType, relatedBillCode, rawPayload, formatCode)`（→RECEIVED）、`archive(ediDocId)`（RECEIVED→ARCHIVED 终态，入站处理完成后调用）。出站 SENT 经 `TransportManager.send` 接线在 Phase 3 落地后回填（config-gated）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] EdiDoc 状态机全迁移路径正确（TO_SEND→SENT→ACKNOWLEDGED / →ERROR→TO_SEND 重试 / →CANCELLED / RECEIVED→ARCHIVED）、非法迁移抛错、UNIQUE 防重、每次迁移写 ErpB2bEdiLog（行为测试覆盖成功 + 失败模式）

### Phase 3 - MFT 传输层 SPI + MockTransportAdapter + TransportManager + 证书 + 重试/死信

Status: completed
Targets: `module-b2b/erp-b2b-service/.../spi/transport/`（`IErpB2bTransportAdapter`/`ErpB2bMftTransportRegistry`/`TransportManager`/`MockTransportAdapter`）、`ErpB2bMftCertificateBizModel.java`（证书读写加密）、`ErpB2bMftLogBizModel.java`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] `Add`：`IErpB2bTransportAdapter`（`getSupportedProtocol`/`send(config, payload)→TransportResult`/`pullInbound(config)→List<InboundFile>`）+ `TransportResult`（`messageId`/`mdnStatus`/`fileHash`/`success`/`errorCode`）DTO。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpB2bMftTransportRegistry`（镜像 `ErpB2bEdiRegistry` List 注入建图，按 `getSupportedProtocol()` 建 `Map<String, Adapter>`）；`getAdapter(protocol)` 未注册抛 `ERR_B2B_MFT_ADAPTER_NOT_REGISTERED`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`MockTransportAdapter`（`getSupportedProtocol="HTTPS"`）——`send` 计算确定性 `messageId`/`fileHash`(SHA-256 of payload)、返回 success（可注入失败模拟重试）；`pullInbound` 返回空列表（webhook 为主路径）；无外部网络。
  - Skill: `nop-backend-dev`
- [x] `Add`：`TransportManager.send(ediDocId, payload)`——查 `ErpB2bMftConfig`（按 partnerId，`active=true`，缺抛 `ERR_B2B_MFT_CONFIG_MISSING`）；按 `protocol` 经 Registry 取 Adapter；调用前可选签名/加密标记（`isSigned`/`isEncrypted` 写 log，真实密码学归 Non-Goal）；调 `adapter.send`；写 `ErpB2bMftLog(direction=OUTBOUND, status=PENDING→SENT/FAILED, startTime/endTime/durationMs)`；成功回填 Phase 2 `markSent`；重试（5xx/超时 `maxRetries` 指数退避，4xx 不重试）；耗尽 `deadLetterEnabled`→status=DEAD_LETTER + 通知。事务边界：传输在事务外，写 log 在事务内（参 logistics `GatewayDispatcher`）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpB2bMftCertificateBizModel` 证书 CRUD + 读取经 `EncryptionHelper`（私钥引用加密存储，禁明文日志）；过期检查查询入口（`findExpiringCertificates`，config-gated，cron 注册归 Non-Goal）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] TransportManager 经 MockTransportAdapter 全链：ErpB2bEdiDoc TO_SEND→SENT、ErpB2bMftLog SENT 记录 messageId/fileHash/durationMs、5xx 重试触发、耗尽 DEAD_LETTER、缺配置抛 `ERR_B2B_MFT_CONFIG_MISSING`、未注册协议抛 `ERR_B2B_MFT_ADAPTER_NOT_REGISTERED`（行为测试覆盖）

### Phase 4 - ASN 入站处理 + Webhook + PO 匹配 + 采购入库集成（3.20）

Status: completed
Targets: `ErpB2bAsnBizModel.java`（`handleInboundWebhook`/`parseToAsn`/`matchPurchaseOrder`/`createReceiveFromAsn`/`retryMatch`/`findUnmatchedAsns`）、`ErpB2bAsnLineBizModel.java`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1、Phase 2

- [x] `Add`：`handleInboundWebhook(formatCode, partnerCode, signature, eventId, payload)`——`@BizMutation`（镜像 logistics `handleTrackingWebhook`）；HMAC-SHA256 校验（`erp-b2b.webhook-signature-required`，密钥取 `ErpB2bPartnerProfile.webhookSecret`，失败 `ERR_B2B_WEBHOOK_SIGNATURE_INVALID`）；幂等键 `eventId+formatCode`（重复抛/返回 `ERR_B2B_WEBHOOK_DUPLICATE_EVENT`）；验签通过入站处理。返回 ediDocId。
  - Skill: `nop-backend-dev`
- [x] `Add`：`parseToAsn(ediDocId, formatCode, payload)`——经 `ErpB2bEdiRegistry.identifyProvider`/`getProvider(formatCode).parsePayload`；失败建 EdiDoc(state=ERROR, `ERR_B2B_EDI_PARSE_FAILED`) + 保留 rawPayload 到 ErpB2bEdiLog + 返回；成功建 `ErpB2bEdiDoc(state=RECEIVED)` + `ErpB2bAsn(sourceEdiDocId, partnerId, relatedBillType=PO_ORDER, relatedBillCode, status=RECEIVED)` + `AsnLine`（materialId 经 `CodeMappingResolver.resolveInbound`，supplierPartNo 保留原值，shippedQty/quantity）+ ErpB2bEdiLog(SUCCESS)。
  - Skill: `nop-backend-dev`
- [x] `Add`：`matchPurchaseOrder(asnId)`——按 `relatedBillType=PO_ORDER`+`relatedBillCode` 查采购订单（经 `IErpPurOrderBiz` 只读，注入 I*Biz 或 IDaoProvider 只读）；逐行物料匹配（AsnLine.materialId vs PO line materialId）+ 数量校验（超 PO 标记 `blockingLevel=WARN`）+ 日期校验；找到→`status=MATCHED` + EdiDoc ARCHIVED；未找到 PO 保留 `status=RECEIVED`（不阻断，`relatedBillCode` 保留）。PO 已关闭→`blockingLevel=ERROR` + 通知。
  - Skill: `nop-backend-dev`
- [x] `Add`：`createReceiveFromAsn(asnId)`（config-gated `erp-b2b.asn-auto-create-receive`，默认关）——MATCHED ASN → 经 `IErpPurReceiveBiz` 创建采购入库草稿（`sourceBillType="B2B_ASN"`/`sourceBillCode=ASN.code`/vendorId/partnerId/lines）；成功 `status=RECEIVED_TO_STOCK`；部分收货维护已收计数（`erp-b2b.asn.partial-receipt-enabled`）。**核心零污染**：不在 ErpPurReceive 加 asnId 列，仅弱指针 sourceBillType/Code。
  - Skill: `nop-backend-dev`
- [x] `Add`：`retryMatch(asnId)`（手动重试 `matchPurchaseOrder`，幂等）+ `findUnmatchedAsns(asOfDate)` 查询入口（超时提示，`erp-b2b.asn.match-timeout-hours`）。自动 cron 注册归 Non-Goal。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] ASN 入站全链经 UBL DespatchAdvice + Mock：webhook HMAC 校验 + 幂等、parse→建 Asn/AsnLine（代码映射）、PO 匹配 MATCHED/未匹配保留 RECEIVED/PO 关闭 ERROR、config-gated 创建入库草稿 RECEIVED_TO_STOCK、追溯链 sourceEdiDocId→EdiDoc→EdiLog 完整（行为测试覆盖成功 + 失败模式）

### Phase 5 - 行为测试与收尾

Status: completed
Targets: `module-b2b/erp-b2b-service/src/test/...`、`docs/logs/2026/07-04.md`、`docs/backlog/extended-roadmap.md`、`docs/design/b2b/*`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 4

- [x] `Proof`：`TestErpB2bEdiEnvelope`——EdiDoc 状态机全路径 + UNIQUE 防重 + ErpB2bEdiLog 写入 + Registry 派发/未注册抛错；`TestErpB2bMftTransport`——MockTransportAdapter 全链 + 重试/死信 + 缺配置/未注册抛错；`TestErpB2bAsnInbound`——webhook HMAC + 幂等 + parse→Asn + 代码映射 + PO 匹配三路径 + config-gated 入库草稿 + 追溯链。JunitAutoTestCase，断言成功/失败模式。
  - Skill: `nop-testing`
- [x] `Add`：`docs/logs/2026/07-04.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` 工作项 3.19/3.20/3.21 标 done；`b2b/edi-formats.md`/`asn-processing.md`/`managed-file-transfer.md` 偏离补注——UBL 样例非 X12/EDIFACT、MockTransportAdapter 非真实 AS2/SFTP、ASN 入站 webhook 主路径非 SFTP 轮询、`error-blocks-flow` 本期不回写源单、ASN→入库 config-gated 核心零污染弱指针。
  - Skill: none

Exit Criteria:

- [x] 全行为测试通过（信封状态机 + MFT 传输 + ASN 入站各路径）

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（`ses_0d29cd2abffeiaFO1tESUOsafM`，独立 general 子代理，冷重播无执行者上下文）。全部 Current Baseline 声明经实时仓库核实为 TRUE（13 实体行号/列/8 态 edi-doc-state 字典/UNIQUE 约束/13 空壳 BizModel/无 spi 包/logistics `handleTrackingWebhook` 是 `@BizMutation`/`IErpPurReceiveBiz`+`IErpPurOrderBiz`+`IErpSalInvoiceBiz` 存在/List 注入 Registry 范式）。无 BLOCKER。3 项 nit 已全部 addressed：N1（`ErpB2bEdiLog.actionType` 列不存在 ↔「不新增列」矛盾——改为动作语义编码到既有 `direction`+`resultCode`+`resultMsg` 列，Phase 2 两处 item 显式声明不新增列）、N2（infra prereq 补 `erp-sal-dao` compile 依赖）、N3（Errors/Configs 改「扩展既有空壳」措辞）。3 工作项 bundling 经 Rule 14 确认合理（共享 b2b owner docs + 单一结果表面 + 相互依赖阶段）。Plan Status 置 `active`。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处。

- [x] 范围内行为完成（EDI Format SPI + UBL Provider + 信封状态机 + MFT 传输层 + ASN 入站全链）
- [x] 相关文档对齐（`b2b/*` 偏离补注、roadmap 3.19/3.20/3.21 done）
- [x] 已运行验证：`mvn clean install -DskipTests`（根）+ `mvn test -pl module-b2b -am`
- [x] 无范围内项目降级为 deferred/follow-up（X12/EDIFACT、真实 AS2/SFTP、SFTP 轮询注册、自动匹配 cron、月台预约、error-blocks-flow 强一致、映射 UI 均为计划内 Non-Goal）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### X12 / EDIFACT Provider 实现

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 设计文档 `edi-formats.md` §四/§五 标 ⚪ 延迟到客户需求；本期 SPI 契约 + UBL 样例已覆盖双向范式。
- Successor Required: yes（触发条件：具体客户/伙伴 X12/EDIFACT 接入需求）

### 真实 AS2/SFTP/FTPS 协议库集成（含 AS2 MDN 异步 / PGP / S-MIME）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 需外部证书/端点/沙箱；本期 MockTransportAdapter 覆盖全链行为，SPI 契约已就绪。
- Successor Required: yes（触发条件：具体传输伙伴接入 + 证书就绪）

### SFTP 入站轮询生产注册 / ASN 自动匹配 cron / 证书过期 cron

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期提供可调用方法 + cron 表达式；webhook 为 ASN 入站主路径；注册归部署。
- Successor Required: yes（触发条件：生产部署 / SFTP 备用路径启用）

### `error-blocks-flow=true` 回写源业务单强一致联动

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本期 ERROR 仅落 EdiDoc.blockingLevel + 日志，不回写阻断源单（弱耦合弱指针原则）；强一致需评估业务合规要求。
- Successor Required: yes（触发条件：生产 EDI 合规要求强一致阻断）

### 月台/dock 预约（UC-B2B Phase 4）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 独立 WMS 结果表面。
- Successor Required: yes（触发条件：WMS 深度集成需求）

## Closure

Status Note: 全部 5 个 Phase 完成。22 个行为测试通过（7 EDI 信封 + 5 MFT 传输 + 5 ASN 入站 + 5 CRUD 冒烟）。`mvn clean install -DskipTests` 全项目 146 reactor 模块 BUILD SUCCESS。工作项 3.19/3.20/3.21 已标 done。独立结束审计已由独立子代理（新会话，冷重播无执行者上下文）执行并通过。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure auditor 子代理（新会话，未参与执行，冷重播无执行者上下文）
- Evidence: 独立审计本次会话完成。语义验证六项全部通过：(1) Phase status/items 一致——5 个 Phase 均 completed，所有 phase body 项目 `[x]`，仅结束审计门控（本审计所属）未勾选，已由本审计勾选；(2) Exit Criteria vs live repo——逐一核实 `IErpB2bEdiProvider`/`ErpB2bEdiRegistry`/`UblDespatchAdviceEdiProvider`/`UblInvoiceEdiProvider`/`CodeMappingResolver`/`ErpB2bEdiDocBizModel`(全状态迁移+UNIQUE 防重+EdiLog 写入)/`IErpB2bTransportAdapter`/`ErpB2bMftTransportRegistry`/`MockTransportAdapter`/`TransportManager`(路由/重试/死信/MftLog)/`ErpB2bAsnBizModel`(handleInboundWebhook HMAC+幂等/parseToAsn/matchPurchaseOrder 三路径/createReceiveFromAsn config-gated/retryMatch/findUnmatchedAsns) 全部落地于 `module-b2b/erp-b2b-service/src/main/java/`，无空实现；(3) Anti-hollow——所有 SPI 组件（Registry/2 Provider/TransportManager/MockAdapter/CodeMappingResolver）经 `module-b2b/erp-b2b-service/src/main/resources/_vfs/erp/b2b/beans/app-service.beans.xml` 注册为 Bean（`ioc:collect-beans by-type`），运行时可经 GraphQL 入口到达；(4) 字典加性新增——`erp-b2b/mft-protocol`/`erp-b2b/mft-status` 已加并绑列（`module-b2b/model/app-erp-b2b.orm.xml:97/105/435/521/522`）；(5) Deferred honesty——5 项 Non-Goal 全部在「Deferred But Adjudicated」带 Successor 触发条件，无可降级范围项隐藏；(6) Docs sync——`docs/logs/2026/07-04.md` 已更新，`docs/design/b2b/{edi-formats,asn-processing,managed-file-transfer}.md` 均有「实现偏离补注（2026-07-04, plan 2200-1）」，roadmap 表行 3.19/3.20/3.21 标 ✅。真实验证命令复跑：`mvn test -pl module-b2b/erp-b2b-service -DfailIfNoTests=false` → `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0` + `BUILD SUCCESS`（与 closure 声明 7+5+5+5=22 完全一致）。已知非本期残留：`extended-roadmap.md:33` 汇总行「3.9–3.21：todo」为 commit 188c2a15 遗留（早于本计划），属 roadmap 全局清理范围，非本计划引入亦非本计划结果表面。

Follow-up:

- X12/EDIFACT Provider——触发条件：具体客户/伙伴 EDI 标准接入需求
- 真实 AS2/SFTP/FTPS 协议库——触发条件：具体传输伙伴接入 + 证书就绪
- SFTP 轮询 / 自动匹配 / 证书过期 cron 生产注册——触发条件：生产部署
- error-blocks-flow 强一致——触发条件：生产 EDI 合规要求
- 月台预约——触发条件：WMS 深度集成需求
