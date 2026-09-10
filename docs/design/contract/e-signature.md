# 电子签章集成设计

## 目的

设计合同域的电子签章集成抽象层，支持多签名服务提供商（e签宝、DocuSign、Tsign），实现合同发送→签署→回调的全流程管理，以及与 ContractVersion 的集成（签署新版本）。

## 设计依据

> 参考 **Odoo Sign**：签名请求 + 模板 + 证书存储。
>
> 参考 **DocuSign API**：Envelope 创建、签名者路由、Connect webhook 回调。
>
> 参考 **e签宝 API**：中国合规电子签章（CA 证书 + 时间戳 + 证据存证）。
>
> 参考 **Tsign（腾讯电子签）**：小程序签署、合同模板。

## 签名提供商抽象

### SPI 接口

> 实现：`app.erp.ct.service.spi.IErpCtSignatureProvider` + `ErpCtSignatureProviderRegistry`
> + `MockSignatureProvider`（providerCode="MOCK"，无外部 HTTP，全链可测试）。Registry 镜像 logistics
> `ErpLogCarrierGatewayRegistry` / finance `ErpFinAcctDocRegistry` 的 List 注入 + 内部建图范式。真实三方供应商归 follow-up。

> **`SignatureInitRequest.signingOrder` 字段为本期 SPI 增量**（design 原 SPI 接口未列）：`"SEQUENTIAL"`/`"PARALLEL"`，
> 由具体 Provider 解释顺序/并行签署。

> **webhook 密钥**：本期 `WEBHOOK_SECRET` 常量供 mock 测试可控；真实部署应扩展为 Provider 凭证配置（归 follow-up）。

```java
public interface IErpCtSignatureProvider {
    String getProviderCode();                    // MOCK / ESIGN_BAO / DOCUSIGN / TSIGN
    SignatureInitResponse initSignature(SignatureInitRequest request);
    SignatureStatusQueryResponse queryStatus(String providerRequestId);
    String getSignUrl(String providerRequestId, String signerEmail);
    byte[] retrieveCertificate(String providerRequestId);
}
```

### 提供商矩阵

| 提供商 | 适用地区 | 签名方式 | CA 认证 | 证据存证 | 参考 |
|--------|----------|----------|---------|----------|------|
| e签宝 | 🇨🇳 中国 | 手机验证码 / 人脸识别 /  UKey | ✅ 国家级 CA | ✅ 司法存证 | 🟢 e签宝开放平台 |
| DocuSign | 🌍 全球 | 邮箱链接 / 手机验证 / IDV | ✅ eIDAS / ESIGN | ✅ 审计证书 | 🟢 DocuSign API |
| Tsign | 🇨🇳 中国 | 微信小程序 / 短信 / 人脸 | ✅ 合规 CA | ✅ 存证报告 | 🟢 腾讯电子签 |

## 签名请求工作流

```
┌─────────────────────────────────────────────────────────────────┐
│                      签名请求生命周期                             │
│                                                                 │
│  ErpCtContractVersion (待签署)                                    │
│         │                                                        │
│         ▼                                                        │
│  ErpCtSignatureRequest(status=PENDING_SIGNATURE)                  │
│         │                                                        │
│         ├─► Provider.initSignature()                              │
│         │      ├─ 上传合同文档（PDF/OFD）                          │
│         │      ├─ 指定签署顺序（顺序/并行）                        │
│         │      ├─ 签署人信息（姓名、手机号、邮箱）                  │
│         │      └─ 回调通知 URL                                    │
│         │                                                        │
│         ▼                                                        │
│  发送签署通知 → 签署人收到签署链接                                  │
│         │                                                        │
│         ├─► 签署人 1 签署 → Provider webhook 回调                  │
│         │     status → PARTIALLY_SIGNED                          │
│         │     signer 记录已签                                      │
│         │                                                        │
│         ├─► 签署人 2 签署                                         │
│         │     status → FULLY_SIGNED (所有签署人完成)               │
│         │                                                        │
│         ├─► 签署人拒签                                            │
│         │     status → REJECTED                                   │
│         │     recordedAt 记录拒绝原因                              │
│         │                                                        │
│         ▼                                                        │
│  签署完成 (FULLY_SIGNED)                                          │
│         │                                                        │
│         ├─► 下载已签署的合同文件（Provider.retrieveCertificate）   │
│         ├─► 存储证书证据（certificateUrl）                         │
│         └─► 更新 ErpCtContractVersion.status → SIGNED             │
│              更新 signingCompletedAt                              │
└─────────────────────────────────────────────────────────────────┘
```

### 签署回调 Webhook

| 回调事件 | 说明 | 操作 |
|----------|------|------|
| `signing.started` | 签名流程已启动 | 记录状态变更 |
| `signer.signed` | 某签署人完成签署 | 更新 signer 记录，状态 → PARTIALLY_SIGNED |
| `signing.completed` | 全部签署人完成签署 | 状态 → FULLY_SIGNED |
| `signing.rejected` | 某签署人拒绝签署 | 状态 → REJECTED，记录原因 |
| `signing.expired` | 签署邀请超时 | 状态 → EXPIRED |
| `signing.declined` | 签署人拒签 | **折叠为 REJECTED**（状态机/字典无 DECLINED 态，按权威 6 态收敛） |

> **webhook 回调经 `@BizMutation` GraphQL 入口**（`ErpCtSignatureRequestBizModel.handleSignatureCallback`，
> 镜像 logistics `ErpLogShipmentBizModel.handleTrackingWebhook` 范式：HMAC-SHA256 校验 + eventId 幂等）。

## 签名状态跟踪

### ErpCtSignatureRequest 状态机

```
PENDING_SIGNATURE ──(首签完成)──→ PARTIALLY_SIGNED ──(全部签署)──→ FULLY_SIGNED
        │                                                     │
        ├──(拒签)──→ REJECTED                                  │
        │                                                     │
        ├──(超时)──→ EXPIRED                                   │
        │                                                     │
        └──(撤销)──→ CANCELLED                                 │
                                                              │
                                                              ▼
                                                     Certificate of Completion
                                                     证书证据存储（certificateUrl）
```

### 状态说明

| 状态 | 含义 | 终态 |
|------|------|------|
| PENDING_SIGNATURE | 等待签署人签名 | ❌ |
| PARTIALLY_SIGNED | 部分签署人已完成 | ❌ |
| FULLY_SIGNED | 全部已签署 | ✅ |
| REJECTED | 签署人拒绝 | ✅ |
| EXPIRED | 签署邀请超时 | ✅ |
| CANCELLED | 发起方撤销 | ✅ |

## 证书证据存储

### 完成证书

签名完成后，从提供商下载以下证据：

| 证据类型 | 内容 | 存储方式 |
|----------|------|----------|
| 签署完成的合同 PDF | 包含签章图形的最终合同 | attachmentId（文件系统/OSS） |
| 签名证书 | 证书链 + 数字签名摘要 | certificateUrl |
| 审计日志 | 每个签署人的时间戳、IP、设备指纹 | 存储在 ErpCtSignatureRequest 审计字段 |
| 存证报告 | 司法鉴定报告（e签宝） | 证据编号（evidenceNo） |

## 与 ContractVersion 集成

### 签署新版本的流程

```
合同版本已定稿 (FINALIZED)
        │
        ▼
检查合同是否启用电子签章
        │
        ├─► 启用 → 创建 ErpCtSignatureRequest
        │            contractVersionId = 当前版本
        │            status = PENDING_SIGNATURE
        │
        └─► 未启用 → 走传统线下签署（附件上传 + 手动确认）
                        contractVersion.status → SIGNED

签署完成后：
        ErpCtContractVersion.status → SIGNED
        ErpCtSignatureRequest.contractVersionId 指向已签署版本
```

## 实体定义

### ErpCtSignatureRequest（签名请求）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/orgId | 标准 | |
| contractVersionId | 待签署的合同版本 | → ErpCtContractVersion |
| provider | dict `erp-ct/sign-provider`：ESIGN_BAO / DOCUSIGN / TSIGN | |
| providerRequestId | 提供商侧的请求 ID（用于查询） | |
| status | dict `erp-ct/sign-status`：签名状态 | 🟢 Odoo Sign Request |
| signers | JSON 数组：`[{name, email, phone, signedAt, rejectedReason}]` | |
| signingDeadline | 签署截止日期 | |
| completedAt | 签署完成时间 | |
| certificateUrl | 完成证书存储 URL | |
| evidenceNo | 存证编号（e签宝） | |
| attachmentId | 已签署的合同文件 | |
| errorMsg | 错误信息 | |
| 标准审计字段 | | |

### 签名状态字典 `erp-ct/sign-status`

> **实现注记（P1-CK-ct-025-r3 修复，plan `2026-09-10-1141-2` Phase 1）**：字典已收敛为 **value==code 单轨语义编码**（对齐 `system-baseline.md` §字段与类型约定 D1）。历史数值轨（10..60）已删除；writer 全链（InitProcessor/回调/轮询）与存疑存量数据（seed/_cases）均为 code 形态，零数据迁移。

| code | label | value |
|------|-------|-------|
| PENDING_SIGNATURE | 待签署 | PENDING_SIGNATURE |
| PARTIALLY_SIGNED | 部分签署 | PARTIALLY_SIGNED |
| FULLY_SIGNED | 全部签署 | FULLY_SIGNED |
| REJECTED | 拒签 | REJECTED |
| EXPIRED | 过期 | EXPIRED |
| CANCELLED | 已撤销 | CANCELLED |

### 签名提供商字典 `erp-ct/sign-provider`

> **实现现状（P3-CK-ct-029-r3 微裁决登记）**：生产字典实含 4 值——3 个真实签署提供商 + `MOCK`（value=99），且默认 provider 为 `MOCK`（`ErpCtConfigs.DEFAULT_SIGNATURE_DEFAULT_PROVIDER = "MOCK"`，本期 stub 语义；原「MOCK 仅在测试 profile 传参使用」表述与实况不符，已修订）。**生产切换路径裁决**：`erp-ct.signature-default-provider` 为部署侧配置项，投产接入真实提供商时经该键切换（部署契约），真实 provider 通道落地与 MOCK 退役走后续需求通道（doc 降级为默认路径登记，不改需求契约段语义）。

| code | label | value |
|------|-------|-------|
| ESIGN_BAO | e签宝 | ESIGN_BAO |
| DOCUSIGN | DocuSign | DOCUSIGN |
| TSIGN | Tsign | TSIGN |
| MOCK | Mock(测试) | MOCK |

## 证据强度

| 证据 | 强度 | 说明 |
|------|------|------|
| 签名请求 + 模板 | 🟢 | Odoo Sign（sign_request + sign_template） |
| 三方提供商抽象 | 🟢 | 多提供商 SPI 设计（通用模式） |
| DocuSign API 签名流程 | 🟢 | DocuSign eSignature REST API |
| e签宝/腾讯云 CA 电子签章 | 🟢 | GB/T 38540、GB/T 25064 合规标准 |
| Webhook 回调模式 | 🟢 | DocuSign Connect / e签宝回调通知 |
| 签署证书与证据存证 | 🟢 | e签宝存证服务 |

## 参考

- `contract/README.md`（合同域概览）
- `contract/approval-workflow.md`（审批流与签署的衔接）
- `contract/use-cases.md` §UC-CT-09 电子签章
- `contract/state-machine.md`（合同版本状态机）
- `model/app-erp-contract.orm.xml`（ORM 模型）
- `b2b/README.md`（Webhook 复用模式）
