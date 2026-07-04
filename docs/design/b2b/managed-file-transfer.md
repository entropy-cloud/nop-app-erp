# 托管文件传输（MFT）设计 — AS2/SFTP

## 目的

设计 B2B 域的文件传输能力，支持 AS2（适用 EDI 传输）和 SFTP/FTPS 协议，包括证书管理、加密/解密、重试机制、死信队列和传输审计日志。与 EDI 信封事务（ErpB2bEdiDoc）协作，提供可靠的文件级传输层。

> **实现偏离补注（2026-07-04, plan 2200-1）**：
> - 本期仅 `MockTransportAdapter`（`protocol=HTTPS`，内联可测试，无外部网络）+ SPI 契约；真实 AS2/SFTP/FTPS 协议库集成归 follow-up（触发条件：具体传输伙伴接入 + 证书就绪）。
> - `ErpB2bMftCertificate` ORM 仅存储证书元数据（指纹/算法/过期日期），无 `certificatePem`/`privateKeyRef` 列（§证书管理 列出但 ORM 未落地）→ 真实私钥加密存储（EncryptionHelper）归 follow-up。
> - SFTP 入站轮询（`SftpPoller` 定时任务）生产注册为 Non-Goal（ASN 入站走 webhook 主动推送为主路径）。

## 设计依据

> 参考 **Cleo Clarify**：多协议 MFT 平台（AS2/SFTP/FTPS/HTTP），证书管理和传输监控。
>
> 参考 **IBM B2B Integrator**：端到端 B2B 集成，协议适配器和加密解密映射。
>
> 参考 **OpenAS2**：开源 AS2 实现，MDN 签名验证。
>
> 参考 **PGP 加密标准**（RFC 4880）和 **S/MIME**（RFC 5751）。

## 协议支持

### AS2（Applicability Statement 2）

| 特性 | 支持 | 说明 |
|------|------|------|
| 传输协议 | HTTP/HTTPS | AS2 基于 HTTP |
| 消息封装 | S/MIME | 数字签名 + 加密 |
| MDN（Message Disposition Notification） | 同步/异步 | 签名回执确认 |
| 消息压缩 | 可选 | `Content-Encoding: x-compress` |
| 消息 ID | 必选 | 唯一跟踪标识 |
| MIC（Message Integrity Check） | SHA-1 / SHA-256 | 用于 MDN 验证 |

### SFTP / FTPS

| 特性 | SFTP (SSH File Transfer) | FTPS (FTP over TLS) |
|------|--------------------------|---------------------|
| 认证 | 用户名+密码 / SSH Key | 用户名+密码 / 证书 |
| 加密 | SSH 隧道 | TLS/SSL |
| 端口 | 22 | 990 (隐式) / 21 (显式) |
| 被动模式 | 全部被动 | 支持 PASV/EPSV |
| 文件推送 | 服务端上传 | 客户端下载/上传 |
| 目录监控 | 轮询 | 轮询 |

## 架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                         MFT 传输层                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────┐   ┌───────────────────────────────┐    │
│  │   协议适配器          │   │   传输管道                     │    │
│  │  ┌─────┐ ┌──────┐   │   │  ┌──────┐ ┌──────┐ ┌──────┐ │    │
│  │  │AS2  │ │SFTP  │   │   │  │签名  │ │加密  │ │压缩  │ │    │
│  │  │适配器│ │适配器│   │   │  │Verify│ │Decrypt│ │Decom │ │    │
│  │  └─────┘ └──────┘   │   │  └──────┘ └──────┘ └──────┘ │    │
│  │  ┌──────┐ ┌──────┐  │   └───────────────────────────────┘    │
│  │  │FTPS  │ │HTTP  │  │                                        │
│  │  │适配器│ │适配器│  │                                        │
│  │  └──────┘ └──────┘  │                                        │
│  └─────────────────────┘                                        │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              传输管理器 (TransportManager)                  │   │
│  │  路由 | 重试 | 死信队列 | 审计日志 | 证书查找               │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 实体定义

### ErpB2bMftConfig（MFT 配置）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/orgId | 标准 | |
| partnerId | Trading Partner | → ErpMdPartner |
| protocol | dict `erp-b2b/mft-protocol`：AS2 / SFTP / FTPS / HTTP / HTTPS | 🟢 Cleo 协议配置 |
| transportEndpoint | 传输端点 URL/主机 | AS2: `https://partner.com/as2` / SFTP: `sftp://partner.com:22/inbox` |
| localAs2Id | 本地 AS2 ID（AS2 协议用） | 🟢 RFC 4130 AS2 标识 |
| remoteAs2Id | 对方 AS2 ID（AS2 协议用） | |
| sftpUsername | SFTP 用户名 | |
| sftpPort | SFTP 端口（默认 22） | |
| ftpsPort | FTPS 端口（默认 990） | |
| ftpsImplicitTls | 是否隐式 TLS | |
| ftpsProtectionLevel | 保护级别：CLEAR / SAFE / CONFIDENTIAL / PRIVATE | |
| certId | 关联证书 ID | → ErpB2bMftCertificate |
| compression | 是否启用压缩 | |
| compressionAlgo | 压缩算法：ZLIB / GZIP | |
| encryption | 是否启用加密 | |
| encryptionAlgo | 加密算法：PGP / S/MIME | |
| encryptionKeyId | 加密密钥 ID | |
| signature | 是否启用签名 | |
| signatureAlgo | 签名算法：SHA-256 / SHA-384 / SHA-512 | |
| active | 是否启用 | |
| maxRetries | 最大重试次数 | |
| retryIntervalMin | 重试间隔（分钟，默认 15） | |
| deadLetterEnabled | 启用死信队列 | |
| monitorDirectory | SFTP 监控目录（入站轮询） | |
| monitorIntervalSec | 监控间隔（秒，默认 60） | |
| remark | 备注 | |
| 标准审计字段 | | |

### MFT 协议字典 `erp-b2b/mft-protocol`

| code | label | value |
|------|-------|-------|
| AS2 | AS2 | 10 |
| SFTP | SFTP | 20 |
| FTPS | FTPS | 30 |
| HTTP | HTTP | 40 |
| HTTPS | HTTPS | 50 |

### ErpB2bMftLog（MFT 传输日志）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/orgId | 标准 | |
| configId | MFT 配置 | → ErpB2bMftConfig |
| relatedBillType | 关联单据类型 | 🟢 Cleo 传输记录 |
| relatedBillCode | 关联单据号 | |
| direction | dict `erp-b2b/edi-direction`：OUTBOUND / INBOUND | |
| fileName | 文件名 | |
| fileSize | 文件大小（字节） | |
| fileHash | 文件 SHA-256 哈希 | |
| messageId | AS2 Message-ID（AS2） | |
| mdnMessageId | MDN Message-ID（AS2） | |
| mdnStatus | MDN 状态（AS2）：processed / failed / warning | |
| mdnVerification | MDN 验签结果：PASSED / FAILED | |
| checksum | 传输校验和 | |
| protocol | 传输协议 | |
| status | dict `erp-b2b/mft-status`：PENDING / SENT / RECEIVED / FAILED / RETRYING / DEAD_LETTER | 🟢 Cleo 传输状态 |
| startTime | 传输开始时间 | |
| endTime | 传输结束时间 | |
| durationMs | 传输耗时（毫秒） | |
| errorCode | 错误码 | |
| errorMsg | 错误消息 | |
| retryCount | 当前重试次数 | |
| isCompressed | 是否压缩 | |
| isEncrypted | 是否加密 | |
| isSigned | 是否签名 | |
| rawPayload | 原始传输内容（用于排错） | |
| remark | 备注 | |
| 标准审计字段 | | |

### MFT 状态字典 `erp-b2b/mft-status`

| code | label | value |
|------|-------|-------|
| PENDING | 待传输 | 10 |
| SENT | 已发送 | 20 |
| RECEIVED | 已接收 | 30 |
| FAILED | 失败 | 40 |
| RETRYING | 重试中 | 50 |
| DEAD_LETTER | 死信 | 60 |

### ErpB2bMftCertificate（MFT 证书）

| 字段 | 含义 |
|------|------|
| id/orgId | 标准 |
| partnerId | Trading Partner（可选，共享证书也可不关联） |
| certName | 证书名称 |
| certType | dict：SIGNING / ENCRYPTION / SSL / SSH |
| algorithm | 算法：RSA / ECDSA / DSA |
| keySize | 密钥长度：2048 / 4096 |
| certificatePem | PEM 格式证书（公钥） |
| privateKeyRef | 私钥引用（加密存储，不直接存储明文） |
| issuerName | 颁发者 |
| subjectName | 主题 |
| serialNo | 证书序列号 |
| issuedAt | 颁发日期 |
| expiresAt | 过期日期 |
| fingerprintSha256 | SHA-256 指纹 |
| isActive | 是否启用 |
| remark | 备注 |
| 标准审计字段 | |

## 文件传输流程

### 出站传输

```
EDI 报文生成 (ErpB2bEdiDoc)
    │
    ▼
TransportManager.send(configId, payload)
    │
    ├─► 1. 查 ErpB2bMftConfig
    │        ├─ protocol, endpoint, certId
    │        └─ 加密/签名配置
    │
    ├─► 2. 应用传输管道
    │        ├─ 压缩 (如果启用)
    │        ├─ 签名 (使用 certId 的私钥签名)
    │        └─ 加密 (使用对方证书加密)
    │
    ├─► 3. 协议发送
    │        ├─ AS2:
    │        │    ├─ HTTP POST → 传输端点
    │        │    ├─ 等待 MDN 响应（同步/异步）
    │        │    ├─ 验证 MDN 签名 + MIC
    │        │    └─ 记录 MDN 验证结果
    │        │
    │        ├─ SFTP:
    │        │    ├─ 建立 SSH 连接
    │        │    ├─ 文件写入目标目录
    │        │    ├─ SCP 传输
    │        │    └─ 关闭连接
    │        │
    │        └─ FTPS:
    │             ├─ TLS 握手
    │             ├─ 文件上传
    │             └─ 关闭连接
    │
    ├─► 4. 记录 ErpB2bMftLog
    │
    └─► 5. 处理结果
         ├─ 成功 → 更新 EdiDoc 状态
         └─ 失败 → 进入重试流程
```

### 入站传输（SFTP 轮询）

```
SftpPoller (定时任务)
    │
    ├─► 读取 ErpB2bMftConfig (where protocol = SFTP AND active = true)
    │
    ├─► 建立 SFTP 连接到 partner 目录
    │
    ├─► 列出新文件（按文件名或时间戳筛选）
    │
    ├─► 下载文件到本地临时目录
    │
    ├─► 应用反向传输管道
    │        ├─ 解密 (使用本地私钥)
    │        ├─ 验签 (使用对方证书)
    │        └─ 解压缩
    │
    ├─► 路由到 EDI 解析器 → 调用 Provider.parsePayload()
    │
    ├─► 记录 ErpB2bMftLog
    │
    └─► 传输成功 → 删除源文件（或移动到 backup 目录）
```

## 重试与错误处理

### 重试策略

| 条件 | 行为 |
|------|------|
| 首次失败 | 记录失败，状态 → FAILED |
| 配置 maxRetries > 0 | 状态 → RETRYING，按 retryIntervalMin 间隔重试 |
| 重试成功 | 状态 → SENT / RECEIVED |
| 重试耗尽 | 状态 → DEAD_LETTER，通知管理员 |

### 死信队列

```
DEAD_LETTER 状态的处理:
    │
    ├─► 管理界面查看死信记录
    │      ├─ 查看原始报文 (rawPayload)
    │      ├─ 查看错误信息 (errorCode, errorMsg)
    │      └─ 查看传输日志链 (同一 configId + relatedBillCode 的所有日志)
    │
    ├─► 管理操作
    │      ├─ 人工重试（重新发送）
    │      ├─ 修改配置后重试（修正端点/证书后触发）
    │      └─ 放弃（标记为已处理，不重试）
    │
    └─► 死信告警
           └─ 通知管理员（邮件/站内信）
```

## 证书管理

### 证书生命周期

```
导入证书
    │
    ▼
证书验证
    ├─ 检查有效期
    ├─ 检查密钥长度（最低 2048）
    └─ 检查证书链（可选）
    │
    ▼
证书激活 (isActive = true)
    │
    ▼
定期检查过期
    ├─ 过期前 30 天 → 告警通知
    └─ 过期后自动停用 (isActive = false)
    │
    ▼
证书更新
    ├─ 更新证书文件
    └─ 更新 fingerprint
```

### 私钥存储安全要求

| 要求 | 说明 |
|------|------|
| 加密存储 | privateKeyRef 使用 AES-256 加密后存储 |
| 非明文日志 | 日志中禁止打印 privateKeyRef 原始值 |
| 访问控制 | 仅 B2B 管理员角色可查看/管理证书 |
| 审计 | 所有证书操作记录审计日志 |

## 传输审计日志

### 日志记录内容

| 信息类别 | 包含字段 |
|----------|----------|
| 基本传输信息 | configId, protocol, direction, fileName, fileSize, fileHash |
| 时间信息 | startTime, endTime, durationMs |
| 结果信息 | status, errorCode, errorMsg |
| 安全信息 | isEncrypted, isSigned, encryptionAlgo, signatureAlgo |
| 重试信息 | retryCount, previous attempts |
| 排错信息 | rawPayload (可选，默认不记录敏感内容) |

### 审计用途

| 场景 | 日志查询条件 |
|------|-------------|
| 某笔业务传输状态 | relatedBillType + relatedBillCode |
| 某合作伙伴传输统计 | configId (→ partnerId) |
| 最近失败传输 | status = FAILED 或 status = DEAD_LETTER |
| 传输耗时分析 | protocol, durationMs, 按时间段分组 |

## 证据强度

| 证据 | 强度 | 说明 |
|------|------|------|
| AS2 协议支持 | 🟢 | RFC 4130 AS2, RFC 3335 MIME-Based AS2 |
| SFTP/FTPS 协议支持 | 🟢 | SSH File Transfer Protocol, FTP over TLS |
| 证书管理 | 🟢 | Cleo / IBM B2Bi 证书管理模式 |
| 重试与死信 | 🟢 | Cleo 传输队列管理 |
| 传输审计日志 | 🟢 | IBM B2Bi 审计模式 |
| PGP 加密 | 🟢 | RFC 4880 OpenPGP |
| S/MIME 签名/加密 | 🟢 | RFC 5751 S/MIME 3.2 |

## 参考

- `b2b/README.md`（B2B 模块总述）
- `b2b/edi-formats.md`（EDI 格式 SPI，与 MFT 协作）
- `b2b/state-machine.md`（EDI 事务状态机）
- `b2b/use-cases.md` §UC-B2B-008 MFT 配置
- `model/app-erp-b2b.orm.xml`（ORM 模型）
