# 2026-07-04-2200-2-contract-e-signature-spi 合同电子签章 SPI + 签署生命周期

> Plan Status: completed
> Mission: erp
> Work Item: 3.13 合同电子签章（`IErpCtSignatureProvider` SPI + 多供应商 + webhook 回调 + ContractVersion 集成）
> Last Reviewed: 2026-07-05
> Source: `docs/backlog/extended-roadmap.md` §M3 工作项 3.13；`docs/design/contract/e-signature.md`；`docs/design/contract/state-machine.md`（ContractVersion 签署状态机）
> Related: `2026-07-04-1115-1-contract-version-invoiceplan-volume-discount-rebate.md`（ContractVersion `signVersion` 已落地——本计划签署完成回写接入点；其 Deferred「合同电子签章 3.13」Successor Required: yes，本计划承接）、`2026-07-04-1115-3-logistics-carrier-gateway-spi-freight-posting.md`（三层 SPI + webhook/HMAC 范式）、`2026-07-04-2200-1-b2b-edi-format-spi-asn-inbound-mft.md`（同批，webhook 回调范式）
> Audit: required

## Current Baseline

- **合同域 CRUD + 版本/签署状态机已落地**（`crud-roadmap.md` Milestone 3 `done`；core-business 3.12 done 计划 1115-1）。`module-contract/model/app-erp-contract.orm.xml`（590 行）已定义本计划触及实体：
  - `ErpCtContractVersion`（`:180`，字典 `erp-ct/version-status`：DRAFT/FINALIZED/SIGNED，`isCurrent`）——`signVersion()`（FINALIZED→SIGNED + 置 isCurrent）已由 1115-1 落地（`ErpCtContractVersionBizModel`），是本计划签署完成回写的接入点。
  - `ErpCtSignatureRequest`（`:485`，全部字段已齐备）：`contractVersionId`/`provider`（VARCHAR 50，**无 `ext:dict` 绑定**）/`providerRequestId`/`status`（VARCHAR 50，**无 `ext:dict` 绑定**）/`signers`（JSON 字符串）/`signingDeadline`/`completedAt`/`certificateUrl`/`evidenceNo`/`attachmentFileId`（`stdDomain="file"`）/`errorMsg`。
- **字典缺口（Phase 1 Decision）**：设计文档 `e-signature.md` 引用的 `erp-ct/sign-status`（PENDING_SIGNATURE/PARTIALLY_SIGNED/FULLY_SIGNED/REJECTED/EXPIRED/CANCELLED）与 `erp-ct/sign-provider`（ESIGN_BAO/DOCUSIGN/TSIGN）字典**在 ORM 中不存在**；`ErpCtSignatureRequest.status`/`provider` 列为裸 VARCHAR 无 `ext:dict` 绑定。新增两字典 + 绑定为加性变更（不改既有列类型）。
- **BizModel 仅为生成空壳**：`ErpCtSignatureRequestBizModel.java` 为 15 行 `CrudBizModel<T>` 空壳；`IErpCtSignatureRequestBiz` 仅 `extends ICrudBiz<T>`。**无 `*.xbiz.xml` 覆盖、无 `spi/` 包、无 Provider、无 webhook 回调、无 ContractVersion 集成、无 ErrorCode/Config**。
- **webhook 回调范式已验证**：logistics `ErpLogShipmentBizModel.handleTrackingWebhook` 以 `@BizMutation` 暴露（经 GraphQL 接收外部回调，HMAC + 幂等）。本计划签名回调 `handleSignatureCallback(providerCode, signature, payload)` 镜像此范式。
- **三层 SPI Registry 范式已验证**：logistics `ErpLogCarrierGatewayRegistry`（List 注入建图，镜像 finance `ErpFinAcctDocRegistry`）。本计划 `ErpCtSignatureProviderRegistry` 同范式。
- **平台能力**：`EncryptionHelper`（凭证加解密）、HMAC 工具、`AppConfig.var` 配置门控、附件 `stdDomain="file"` 均可复用。
- **本计划不新增 ORM 列**（ErpCtSignatureRequest 字段已齐备）；仅加性字典 + dict 绑定。

## Goals

- 实现**签名提供商 SPI**：`IErpCtSignatureProvider`（`getProviderCode`/`initSignature(SignatureInitRequest)→SignatureInitResponse`/`queryStatus(providerRequestId)→SignatureStatusQueryResponse`/`getSignUrl(providerRequestId, signerEmail)`/`retrieveCertificate(providerRequestId)→byte[]`）+ 中立 DTO（`SignatureInitRequest`/`SignatureInitResponse`/`SignatureStatusQueryResponse`/`Signer`）。
- 实现 `ErpCtSignatureProviderRegistry`（List 注入建图 + `getProvider(providerCode)` 派发，未注册抛 ErrorCode）。
- 实现**一个 stub/mock Provider**（`providerCode="mock"`，内联可测试，无外部 HTTP），覆盖 init→query→getSignUrl→retrieveCertificate 全契约。
- 实现 `ErpCtSignatureRequest` **签署状态机**：PENDING_SIGNATURE→PARTIALLY_SIGNED（首签）→FULLY_SIGNED（全部）/→REJECTED/→EXPIRED/→CANCELLED；非法迁移抛 ErrorCode。
- 实现**签署生命周期**：`initSignatureRequest(contractVersionId, signers, provider)`（FINALIZED 版本→建 SignatureRequest + 调 Provider.initSignature + 回填 providerRequestId）、`handleSignatureCallback`（webhook HMAC + 幂等 + 按 event 推进状态 + 写 signer 记录）、`queryAndUpdateStatus`（主动轮询）、`cancelSignatureRequest`/`rejectSignature`。
- 实现 **ContractVersion 集成**：FULLY_SIGNED 时 `retrieveCertificate` + 存附件/证书 + 调既有 `IErpCtContractVersionBiz.signVersion()`（FINALIZED→SIGNED + isCurrent）；config-gated（`erp-ct.e-signature-enabled`，默认关，未启用走线下签署附件上传 + 手动确认 SIGNED）。

## Non-Goals

- **真实三方供应商 HTTP 集成**（e签宝/DocuSign/Tsign 具体 SDK/CA/存证）——需外部凭证/沙箱/合规审计；本期仅 `mock` stub + SPI 契约，真实 Provider 归 follow-up（触发条件：具体供应商接入需求 + 凭证/合规就绪）。
- **合同审批工作流**（`ErpCtApprovalMatrix`/`ErpCtApprovalRecord`，金额阈值路由/驳回循环，`approval-workflow.md`）——独立结果表面（实体已存在）。
- **合同文档仓库 / OCR / 全文检索**（`ErpCtDocument`，`contract-repository.md`）——独立切片。
- **签署过期自动 cron 轮询 + 超时通知**——本期提供 `queryAndUpdateStatus` 主动查询 + `findExpiringRequests` 查询入口，cron 注册归部署 follow-up。
- **签署前端可视化/签署链接嵌入手持端**——归前端计划。
- **核心零污染**：不在 `ErpCtContractVersion` 加 `signatureRequestId` 列（反模式）；SignatureRequest→ContractVersion 单向 `contractVersionId` 引用（弱指针）。

## Task Route

- Type: `implementation-only change`（含 Phase 1 加性字典 + dict 绑定——contract 域内，需记录；不触及 finance 保护区域）
- Owner Docs: `docs/design/contract/e-signature.md`（SPI + 状态机 + ContractVersion 集成）、`docs/design/contract/state-machine.md`（ContractVersion 签署状态机）、`docs/design/contract/README.md`（边界/反模式）、`docs/architecture/integration-pattern.md`（webhook + HMAC 复用）
- Skill Selection Basis: 全部阶段为 Nop 后端 BizModel/SPI/跨实体开发——`nop-backend-dev` 匹配（决策门、xbiz 动作、跨实体 I*Biz 注入、ErrorCode、事务边界、产品化可定制性自检）。Phase 1 触及 contract 加性字典。Phase 5 测试用 `nop-testing`。

## Infrastructure And Config Prereqs

- 无新增端口/外部服务/密钥/.env；webhook 回调复用平台 GraphQL 路由（`@BizMutation`，参 logistics 范式）。
- **模块编译依赖**：`erp-ct-service` 内自包含（`IErpCtContractVersionBiz` 同模块，注入无跨域级联）。
- 配置项经 `AppConfig.var(..., defaultValue)`（新建 `ErpCtSignatureConfigs.java` 或扩 `ErpCtConfigs.java`）：`erp-ct.e-signature-enabled`（默认 false）、`erp-ct.signature-default-provider`（默认 mock）、`erp-ct.signature-callback-signature-required`（默认 true）、`erp-ct.signature-status-polling-cron`（默认 `0 0 */2 * * ?`，注册归 Non-Goal）、`erp-ct.signature-deadline-default-days`（默认 15）。
- 无数据迁移；不新增 ORM 列。Phase 1 加性字典 + dict 绑定为元数据变更。

## Execution Plan

### Phase 1 - 签名 SPI + Registry + mock Provider + 字典 + ErrorCode/Config

Status: completed
Targets: `module-contract/erp-ct-service/.../spi/`（`IErpCtSignatureProvider`/DTO）、`.../spi/registry/ErpCtSignatureProviderRegistry.java`、`.../spi/mock/MockSignatureProvider.java`、`module-contract/model/app-erp-contract.orm.xml`（加性字典 + dict 绑定）、`ErpCtErrors.java`（扩展）、`ErpCtConfigs.java`（扩展）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无

- [x] `Decision`：签名字典裁定。新增 `erp-ct/sign-status`（PENDING_SIGNATURE/PARTIALLY_SIGNED/FULLY_SIGNED/REJECTED/EXPIRED/CANCELLED，value 10–60）+ `erp-ct/sign-provider`（ESIGN_BAO/DOCUSIGN/TSIGN，value 10–30，外加 MOCK 占位）字典；`ErpCtSignatureRequest.status` 绑 `erp-ct/sign-status`、`provider` 绑 `erp-ct/sign-provider`。替代方案——以裸 VARCHAR 硬编码（rejected：字典提供 i18n + 校验 + 前端枚举，符合平台范式）。残留风险：dict 绑定属加性列元数据，需重新 codegen 该列渲染（不影响数据）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpCtSignatureProvider`（5 方法签名）+ `SignatureInitRequest`（contractVersionId/文档 attachmentFileId/`List<Signer>` signers/signingOrder/callbackUrl）/`SignatureInitResponse`（providerRequestId/signUrl/initiated）/`SignatureStatusQueryResponse`（status/已签签署人列表/certificateAvailable/errorMsg）/`Signer`（name/email/phone）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpCtSignatureProviderRegistry`——**镜像 logistics `ErpLogCarrierGatewayRegistry` / finance `ErpFinAcctDocRegistry` List 注入建图范式**（`setProviders(List<IErpCtSignatureProvider>)` setter，内部按 `getProviderCode()` 建图）；`getProvider(providerCode)` 未注册抛 `ERR_CT_SIGNATURE_PROVIDER_NOT_REGISTERED`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`MockSignatureProvider`（`getProviderCode="mock"`）——`initSignature` 生成确定性 providerRequestId + signUrl、`queryStatus` 按注入推进（可模拟 PARTIALLY/FULLY/REJECTED）、`retrieveCertificate` 返回占位字节；无外部 HTTP。
  - Skill: `nop-backend-dev`
- [x] `Add`：扩展 `ErpCtErrors.java`（`ERR_CT_SIGNATURE_PROVIDER_NOT_REGISTERED`/`ERR_CT_SIGNATURE_ILLEGAL_TRANSITION`/`ERR_CT_SIGNATURE_VERSION_NOT_FINALIZED`/`ERR_CT_SIGNATURE_ALREADY_COMPLETED`/`ERR_CT_SIGNATURE_CALLBACK_SIGNATURE_INVALID`/`ERR_CT_SIGNATURE_CALLBACK_DUPLICATE_EVENT`/`ERR_CT_SIGNATURE_INIT_FAILED`，中文描述）；扩 `ErpCtConfigs.java` 补 5 配置项。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] SPI + DTO + mock Provider + Registry 编译通过；Registry 按 providerCode 派发、未注册抛 ErrorCode；sign-status/sign-provider 字典加性新增 + dict 绑定（`mvn test-compile -pl module-contract/erp-ct-service -am`，解除 Phase 2/3 编译依赖）

### Phase 2 - 签署生命周期 + 状态机 + ContractVersion 集成

Status: completed
Targets: `ErpCtSignatureRequestBizModel.java`（`initSignatureRequest`/`handleSignatureCallback`/`queryAndUpdateStatus`/`cancelSignatureRequest`/`rejectSignature`）、`ErpCtContractVersionBizModel.java`（复用既有 `signVersion`）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] `Add`：`initSignatureRequest(contractVersionId, signers, providerCode)`——校验 ContractVersion `FINALIZED`（否则 `ERR_CT_SIGNATURE_VERSION_NOT_FINALIZED`）；config-gated `erp-ct.e-signature-enabled`（关则走线下，见 Non-Goal）；建 `ErpCtSignatureRequest(status=PENDING_SIGNATURE, provider, signers JSON, signingDeadline)`；经 Registry 取 Provider 调 `initSignature`，回填 `providerRequestId`；失败抛 `ERR_CT_SIGNATURE_INIT_FAILED` + 写 errorMsg。注入 `IErpCtContractVersionBiz`（同模块 I*Biz）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`handleSignatureCallback(providerCode, signature, eventId, payload)`——`@BizMutation`（镜像 logistics webhook）；HMAC 校验（`erp-ct.signature-callback-signature-required`，失败 `ERR_CT_SIGNATURE_CALLBACK_SIGNATURE_INVALID`）；幂等 `eventId+providerCode`（重复抛/返回 `ERR_CT_SIGNATURE_CALLBACK_DUPLICATE_EVENT`）；按 event 推进——`signer.signed`→PARTIALLY_SIGNED（更新 signers JSON 已签记录）、`signing.completed`→FULLY_SIGNED（调 Phase 3 完成集成）、`signing.rejected`→REJECTED、`signing.expired`→EXPIRED；非法迁移抛 `ERR_CT_SIGNATURE_ILLEGAL_TRANSITION`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`queryAndUpdateStatus(requestId)`——主动轮询 Provider.queryStatus，按返回 status 推进（与 callback 共用状态机迁移逻辑）；`findExpiringRequests(asOfDate)` 查询入口（`signingDeadline < asOfDate` 且非终态）。
  - Skill: `nop-backend-dev`
- [x] `Add`：FULLY_SIGNED 完成集成（callback completed 与 query 共用）——`retrieveCertificate(providerRequestId)` 下载已签文档/证书 → `attachmentFileId`（`stdDomain="file"`）+ `certificateUrl`/`evidenceNo`/`completedAt`；调既有 `IErpCtContractVersionBiz.signVersion(contractVersionId)`（FINALIZED→SIGNED + isCurrent）。已 FULLY_SIGNED 重复抛 `ERR_CT_SIGNATURE_ALREADY_COMPLETED` 幂等。
  - Skill: `nop-backend-dev`
- [x] `Add`：`cancelSignatureRequest(requestId)`（PENDING_SIGNATURE/PARTIALLY_SIGNED→CANCELLED）、`rejectSignature(requestId, reason)`（→REJECTED，写 errorMsg）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 签署状态机全迁移路径正确（PENDING→PARTIALLY→FULLY / →REJECTED / →EXPIRED / →CANCELLED）、非法迁移抛错、webhook HMAC + 幂等、FULLY_SIGNED retrieveCertificate + 调 signVersion 置 ContractVersion SIGNED + isCurrent、重复完成幂等（行为测试覆盖成功 + 失败模式）

### Phase 3 - 行为测试与收尾

Status: completed
Targets: `module-contract/erp-ct-service/src/test/...`、`docs/logs/2026/07-04.md`、`docs/backlog/extended-roadmap.md`、`docs/design/contract/*`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 2

- [x] `Proof`：`TestErpCtESignature`——mock Provider 全链：initSignatureRequest（FINALIZED 守门 + 非 FINALIZED 抛错）+ callback HMAC/幂等 + 状态推进（signer.signed→PARTIALLY/completed→FULLY/rejected/expired）+ FULLY_SIGNED retrieveCertificate + ContractVersion signVersion SIGNED + isCurrent + 重复幂等 + cancel/reject + Registry 未注册抛错。JunitAutoTestCase，断言成功/失败模式。
  - Skill: `nop-testing`
- [x] `Add`：`docs/logs/2026/07-04.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` 工作项 3.13 标 done；`e-signature.md`/`state-machine.md` 偏离补注——mock Provider 非真实供应商、config-gated 默认关（线下签署兜底）、webhook 回调经 `@BizMutation` GraphQL 入口、签署完成经既有 `signVersion` 接入、**`signing.declined` 回调事件折叠为 REJECTED 状态**（design `e-signature.md` webhook 表列 DECLINED 但状态机/字典无此态，本期按权威状态机 6 态收敛 declined→REJECTED 并修正 design webhook 表）、**`sign-provider` 字典新增 MOCK 占位**（design 3 供应商之外，供 mock stub）、**`SignatureInitRequest.signingOrder` 字段为 SPI 增量**（design `e-signature.md` SPI 接口未列，本期顺序/并行签署所需）。
  - Skill: none

Exit Criteria:

- [x] 全行为测试通过（init/状态机/callback/轮询/ContractVersion 集成/cancel/reject 各路径）

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（`ses_0d29ca5cdffeyAro54x4XXY0ee`，独立 general 子代理，冷重播无执行者上下文）。全部 Current Baseline 声明经实时仓库核实为 TRUE（`ErpCtSignatureRequest` 实体+全列 `:485-518`/`status`·`provider` 无 ext:dict 绑定/`erp-ct/sign-status`·`erp-ct/sign-provider` 字典缺失/BizModel 空壳/`signVersion()` 真实存在 `:52-80`/无 spi 包/logistics webhook 是 `@BizMutation`/List 注入 Registry 范式/前置计划 1115-1 Deferred「3.13」Successor: yes）。无 BLOCKER。3 项 nit 已全部 addressed 并入 Phase 3 doc-update 偏离补注：N1（`signing.declined`→REJECTED 状态折叠 + 修正 design webhook 表）、N2（`sign-provider` 字典加 MOCK 占位）、N3（`SignatureInitRequest.signingOrder` 为 SPI 增量）。FULLY_SIGNED→`retrieveCertificate`→`signVersion()` 接线经核实正确。Plan Status 置 `active`。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处。

- [x] 范围内行为完成（签名 SPI + mock Provider + 状态机 + 生命周期 + ContractVersion 集成）
- [x] 相关文档对齐（`contract/e-signature.md`/`state-machine.md` 偏离补注、roadmap 3.13 done）
- [x] 已运行验证：`mvn clean install -DskipTests`（根）+ `mvn test -pl module-contract -am`
- [x] 无范围内项目降级为 deferred/follow-up（真实供应商 HTTP、审批工作流、文档仓库、cron 注册、前端均为计划内 Non-Goal）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 真实三方供应商 HTTP 集成（e签宝 / DocuSign / Tsign）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 需外部凭证/沙箱/CA 合规审计；本期 mock Provider 覆盖全链行为，SPI 契约已就绪。
- Successor Required: yes（触发条件：具体供应商接入需求 + 凭证/合规就绪）

### 签署过期自动 cron 轮询 + 超时通知

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期提供 `queryAndUpdateStatus` 主动查询 + `findExpiringRequests` 入口；cron 注册归部署。
- Successor Required: yes（触发条件：生产部署 / 自动化签署监控需求）

## Closure

Status Note: 全部 3 Phase 完成。SPI + mock Provider + 状态机 + 生命周期 + ContractVersion 集成落地；TestErpCtESignature 19 tests 全绿；根 `mvn clean install -DskipTests` + contract 模块 `mvn test -am` 全绿；roadmap 3.13 + design docs 偏离补注对齐。独立结束审计由独立子代理（新会话，无执行者上下文）完成并通过。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure-auditor，新会话，冷重播无 EXECUTE 上下文，2026-07-05）
- Audit Scope: 计划全文重读 + 每条 Exit Criterion 与 Closure Gate 对照实时仓库（`./`）核实
- Live-Repo Verification:
  - Phase 1 落地核实：`IErpCtSignatureProvider.java`（5 方法签名）+ 4 DTO（`SignatureInitRequest`/`SignatureInitResponse`/`SignatureStatusQueryResponse`/`Signer`）+ `ErpCtSignatureProviderRegistry.java`（`setProviders` List 注入建图 + `getProvider` 未注册抛 `ERR_CT_SIGNATURE_PROVIDER_NOT_REGISTERED`）+ `MockSignatureProvider.java`（providerCode="mock"）+ `ErpCtErrors.java` 7 个新 ErrorCode + `app-erp-contract.orm.xml:84/93` 新增 `erp-ct/sign-status`/`erp-ct/sign-provider` 字典 + `:509/511` `provider`/`status` 列绑定 `ext:dict`
  - Phase 2 落地核实：`ErpCtSignatureRequestBizModel.java` 实装 5 方法（`initSignatureRequest`/`handleSignatureCallback`/`queryAndUpdateStatus`/`cancelSignatureRequest`/`rejectSignature`）；注入 `IErpCtContractVersionBiz` + `ErpCtSignatureProviderRegistry`；运行时实际调用 `providerRegistry.getProvider(...).initSignature/queryStatus/retrieveCertificate`；`IErpCtSignatureRequestBiz` 接口同步声明 5 方法
  - Phase 3 落地核实：`TestErpCtESignature.java`（extends `JunitAutoTestCase`，19 tests）覆盖 init FINALIZED 守门 / disabled 抛错 / callback HMAC/幂等 / event 全推进（signer.signed/completed/rejected/declined→REJECTED 折叠/expired）/ 重复完成 ALREADY_COMPLETED / cancel/reject / 非法迁移 / Registry 未注册 / findExpiringRequests / 轮询 PARTIALLY→FULLY→signVersion；`docs/logs/2026/07-04.md` §合同电子签章条目存在
- Anti-Hollow Check: Registry.getProvider 真抛 ErrorCode（非 return null）；BizModel 方法实际调用 Provider + 调用 `signVersion`（非空壳）；测试断言真实 ErrorCode 与状态迁移（非仅类型签名）
- Five-Point Consistency: Plan Status=completed / 3 Phase Status=completed / 全 Exit Criteria `[x]` / 全 Closure Gates `[x]` / Closure evidence 实存 — 一致
- Deferred Honesty: 真实三方供应商 HTTP / cron 注册均为计划内 Non-Goal（ Goals/Non-Goals 段明示），未隐藏在 Deferred；Deferred But Adjudicated 两项均带 Successor 触发条件
- Evidence: Phase 1/2/3 全部 `[x]`；验证命令：`mvn clean install -DskipTests`（根，146 reactor 模块，BUILD SUCCESS）+ `mvn test -pl module-contract/erp-ct-service -am`（32 tests, 0 failures, 0 errors）；dev log `docs/logs/2026/07-04.md` §合同电子签章条目；roadmap `docs/backlog/extended-roadmap.md` 工作项 3.13 ✅ done
- Verdict: approved — 计划可关闭

Follow-up:

- 真实三方供应商 HTTP 集成（e签宝/DocuSign/Tsign）——触发条件：具体供应商接入需求 + 凭证/合规就绪
- 签署过期 cron + 超时通知——触发条件：生产部署
