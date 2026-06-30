# B2B/EDI 用例

## UC-B2B-001 EDI 格式配置

| 项目 | 内容 |
|------|------|
| 用例编号 | UC-B2B-001 |
| 名称 | 配置 EDI 格式 |
| 参与者 | B2B 管理员 |
| 前置条件 | `erp-b2b.enabled=true` |
| 后置条件 | 格式配置保存，可被 ErpB2bEdiRegistry 发现 |

### 基本流

1. B2B 管理员进入「EDI 格式」菜单。
2. 新增格式记录 指定格式名称（如 UBL Invoice）、标准（UBL）、方向（OUTBOUND）、是否需 WebService。
3. 保存 → 生成 ErpB2bEdiFormat 记录。
4. （可选）若该格式有新 Provider bean 需部署，另行部署 Provider 实现。

### 业务规则

- 每种格式对应一个 `IErpB2bEdiProvider` bean（按 `getCode()` 匹配）。
- `isActive=false` 的格式不参与适用性派发。
- 格式配置是 Provider 的元数据，Provider 实现负责实际的报文生成和解析。

### 证据

| 证据 | 来源 |
|------|------|
| 格式可插拔 SPI | Odoo `account_edi_format.py` |
| 适用性派发 | Odoo `_get_move_applicability` |

---

## UC-B2B-002 EDI 出站发送

| 项目 | 内容 |
|------|------|
| 用例编号 | UC-B2B-002 |
| 名称 | 出站 EDI 文档发送 |
| 参与者 | 系统（异步队列）/ B2B 管理员 |
| 前置条件 | 业务单据已审核（如销售发票已审核） |
| 后置条件 | ErpB2bEdiDoc.state=SENT 或 ERROR |

### 基本流

1. 业务单据审核事件触发（如销售发票 AR_INVOICE 审核）。
2. ErpB2bEdiRegistry 按 relatedBillType 查适用格式 Provider。
3. 生成 ErpB2bEdiDoc(code=格式自动生成, state=TO_SEND, formatId, relatedBillType, relatedBillCode)。
4. Provider.generatePayload() 生成 EDI 报文。
5. Provider.needsWebService() 判断 
   - true → 入异步发送队列，cron 轮询发送。
   - false → 同步 HTTP POST 发送。
6. 发送成功 → state=SENT。
7. 记录 ErpB2bEdiLog（requestPayload, responsePayload）。

### 备选流

- **格式不匹配** 无适用 Provider → 设 blocking_level=INFO，状态不变，日志记录"无适用格式"。
- **发送失败** state=ERROR → 管理员处理（见用例六）。

### 业务规则

- UNIQUE(formatId, relatedBillType, relatedBillCode) 保证同一格式同一业务单仅一条 EDI 事务。
- 已存在的 ediDoc（非终态）返回已有记录，不重复创建。

---

## UC-B2B-003 ASN 入站接收

| 项目 | 内容 |
|------|------|
| 用例编号 | UC-B2B-003 |
| 名称 | 入站 ASN 报文接收 |
| 参与者 | 供应商（推送）/ 系统（自动处理） |
| 前置条件 | 供应商已配置 EDI 格式和 Webhook URL |
| 后置条件 | ErpB2bEdiDoc.state=RECEIVED→ARCHIVED，ErpB2bAsn 创建 |

### 基本流

1. 供应商推送 EDI 报文到 Webhook 入站端点。
2. HMAC 签名验证（复用 `integration-pattern.md` 的入站签名机制）。
3. 根据报文头部/内容识别格式 → 找对应的 `IErpB2bEdiProvider`。
4. Provider.parsePayload() 解析报文 → 返回 ParsedPayload。
5. 建 ErpB2bEdiDoc(state=RECEIVED, formatId, relatedBillType=PO_ORDER, relatedBillCode=采购订单号)。
6. 建 ErpB2bAsn(sourceEdiDocId, partnerId, shipmentDate, trackingNo, relatedBillType, relatedBillCode, status=RECEIVED)。
7. 建 ErpB2bAsnLine（materialId, supplierPartNo, quantity, shippedQty）。
8. 记录 ErpB2bEdiLog（requestPayload, responsePayload）。
9. EdiDoc state → ARCHIVED。

### 备选流

- **签名失败** 拒绝请求，记录日志，blocking_level=ERROR。
- **解析失败** EdiDoc state=ERROR，保留报文到 EdiLog，通知管理员。
- **无匹配采购订单** ASN 仍创建，relatedBillCode 为空，状态为 RECEIVED，由管理员手工匹配。

### 业务规则

- ASN 不直接写库存（等待 purchase 域决定）。
- ASN 可追溯回来源 EDI 报文（sourceEdiDocId）。
- 无匹配采购订单时 ASN 也可创建（先收到 ASN 后下 PO 的场景）。

---

## UC-B2B-004 代码映射管理

| 项目 | 内容 |
|------|------|
| 用例编号 | UC-B2B-004 |
| 名称 | 配置内外代码映射 |
| 参与者 | B2B 管理员 |
| 前置条件 | Master Data 中的物料/伙伴已存在 |
| 后置条件 | 映射保存，EDI 转换时使用 |

### 基本流

1. B2B 管理员进入「代码映射」菜单。
2. 选择映射类型（MATERIAL/PARTNER/UOM）。
3. 选择内部代码（如物料编码 MTL-001）。
4. 选择 Trading Partner（ErpMdPartner）。
5. 输入外部代码（如供应商的物料编码 SUP-MTL-001）。
6. 保存 → 生成 ErpB2bCodeMapping 记录。

### 业务规则

- 同一 partnerId + mappingType + internalCode 唯一。
- 无映射时 出站 EDI 使用内部代码，入站 EDI 使用外部代码原值。
- 映射在 generatePayload（内部→外部）和 parsePayload（外部→内部）时双向使用。

---

## UC-B2B-005 Webhook 集成

| 项目 | 内容 |
|------|------|
| 用例编号 | UC-B2B-005 |
| 名称 | Webhook 出站/入站集成 |
| 参与者 | 系统 / Trading Partner |
| 前置条件 | Webhook 配置已在 ErpSysWebhookConfig 中定义 |
| 后置条件 | EDI 报文通过 webhook 发送/接收 |

### 说明

本模块不另造 webhook 表，复用 `integration-pattern.md` 的 `ErpSysWebhookConfig`/`ErpSysWebhookLog`。

### 出站场景

1. EDI 发送器调用 ErpSysWebhookConfig 中配置的 Trading Partner 端点。
2. 发送方式 HTTP POST + HMAC 签名（或 Trading Partner 指定的认证方式）。
3. 记录 ErpSysWebhookLog（若失败则记录错误）。

### 入站场景

1. Trading Partner 推送 EDI 报文到本系统 Webhook 入站端点。
2. HMAC 验签 → 进入 ASN 接收流程（用例三）。
3. 记录 ErpSysWebhookLog。

### 业务规则

- Webhook 出站认证方式（HMAC/BasicAuth/OAuth）由 ErpSysWebhookConfig 配置。
- 本模块的 EDI 入站端点路径约定 `/r/b2b/webhook/inbound/{formatCode}`。
- Webhook 日志用于审计和排错。

---

## UC-B2B-006 错误处理与重试

| 项目 | 内容 |
|------|------|
| 用例编号 | UC-B2B-006 |
| 名称 | EDI 错误处理与重试 |
| 参与者 | B2B 管理员 |
| 前置条件 | ErpB2bEdiDoc.state=ERROR |
| 后置条件 | 重试成功（回到 TO_SEND 或 RECEIVED）或放弃（CANCELLED） |

### 基本流

1. B2B 管理员进入「EDI 事务」菜单，筛选 state=ERROR 的记录。
2. 查看错误信息（error 字段）和 EdiLog（requestPayload/responsePayload）。
3. 判断错误原因 
   - 网络错误 → 检查网络后点击"重试"。
   - 签名错误 → 检查认证配置后点击"重试"。
   - 格式错误 → 修正报文/配置后点击"重试"。
   - 不可恢复错误 → 点击"放弃"。
4. 重试 state→TO_SEND（出站）或 RECEIVED（入站），重新走发送/解析流程。
5. 放弃 state→CANCELLED。
6. 记录操作日志。

### 自动重试策略

| 条件 | 行为 |
|------|------|
| ERROR 且 blocking_level=WARN | 系统每 30 分钟自动重试，最多 3 次 |
| ERROR 且 blocking_level=ERROR | 不自动重试，需人工介入 |
| 重试 3 次仍失败 | blocking_level 升级为 ERROR |

### 业务规则

- 重试使用新的 EdiDoc state 迁移，不修改原 error 记录（保留审计轨迹）。
- 重试时重新调用 Provider.generatePayload() 或 parsePayload()。
- 原 EdiLog 保留，重试产生新的 EdiLog 记录。

---

## 用例矩阵

| 用例 | 触发方式 | 核心实体 | 涉及域 |
|------|----------|----------|--------|
| UC-B2B-001 | 管理员配置 | ErpB2bEdiFormat | B2B 自身 |
| UC-B2B-002 | 事件驱动 | ErpB2bEdiDoc, ErpB2bEdiLog | B2B, sales/purchase |
| UC-B2B-003 | Webhook 入站 | ErpB2bEdiDoc, ErpB2bAsn, ErpB2bAsnLine, ErpB2bEdiLog | B2B, purchase |
| UC-B2B-004 | 管理员配置 | ErpB2bCodeMapping | B2B, master-data |
| UC-B2B-005 | 系统自动 | ErpSysWebhookConfig, ErpSysWebhookLog | integration |
| UC-B2B-006 | 错误触发 | ErpB2bEdiDoc, ErpB2bEdiLog | B2B 自身 |
| UC-B2B-007 | 管理员配置 | ErpB2bPartnerProfile, ErpB2bPartnerCredential, ErpB2bTestExchange, ErpB2bCertificationChecklist | B2B, master-data |
| UC-B2B-008 | 管理员配置 | ErpB2bMftConfig, ErpB2bMftCertificate, ErpB2bMftLog | B2B, master-data |

---

## UC-B2B-007 合作伙伴上线

| 项目 | 内容 |
|------|------|
| **概述** | Trading Partner 从注册到正式上线的完整流程：档案管理、测试消息交换、认证清单检查、生产切换与归档 |
| **触发条件** | B2B 管理员注册新的 Trading Partner |
| **前置条件** | ErpMdPartner 已存在；EDI 格式已配置 |
| **基本流程** | 1. **REGISTERED**：管理员填写伙伴档案（ErpB2bPartnerProfile），配置传输协议、认证方式、支持格式<br>2. 管理伙伴凭证（ErpB2bPartnerCredential），含 API Key/证书/用户名密码等<br>3. 推进至 **TESTING**：双方按测试用例交换 EDI 报文（ErpB2bTestExchange），验证通过率≥90%<br>4. 推进至 **CERTIFIED**：逐项完成认证检查清单（ErpB2bCertificationChecklist），所有必检项通过<br>5. 推进至 **PRODUCTION**：切换生产端点与凭证，设置 goLiveDate，上线监控 24 小时<br>6. 终止合作时下线归档（status=TERMINATED，archivedAt 记录） |
| **后置条件** | 伙伴进入生产状态，可正常交换业务 EDI |
| **异常** | 测试通过率不达标或必检项未通过时无法推进至下一阶段 |
| **跨域协作** | master-data（Partner 关联）；b2b（EDI 事务关联 partnerProfileId）；notification（上线通知） |

---

## UC-B2B-008 托管文件传输（MFT）配置

| 项目 | 内容 |
|------|------|
| **概述** | 配置和管理 AS2/SFTP/FTPS 文件传输通道，包括协议端点、证书、加密/签名设置、重试策略和死信队列，以及传输审计日志查看 |
| **触发条件** | B2B 管理员需要为 Trading Partner 配置文件传输通道 |
| **前置条件** | Trading Partner 已注册（ErpB2bPartnerProfile）；证书已导入（用于签名/加密） |
| **基本流程** | 1. B2B 管理员进入「MFT配置」菜单<br>2. 新增 ErpB2bMftConfig：选择协议（AS2/SFTP/FTPS），配置传输端点、AS2 ID（AS2协议）、凭证/证书<br>3. 配置传输管道选项：压缩（GZIP/ZLIB）、加密（PGP/SMIME）、签名（SHA-256）<br>4. 配置重试策略：maxRetries、retryIntervalMin、deadLetterEnabled<br>5. SFTP 协议时可配置监控目录和轮询间隔<br>6. 保存配置（active=true），MFT 通道生效<br>7. 文件传输时系统自动记录 ErpB2bMftLog（方向、文件名、大小、哈希、状态、耗时）<br>8. 传输失败时按重试策略自动重试；重试耗尽进入 DEAD_LETTER 状态<br>9. 管理员在「MFT传输日志」查看传输记录，可人工重试或放弃死信<br>10. 证书管理：导入证书（ErpB2bMftCertificate），支持有效期监控和自动告警 |
| **后置条件** | MFT 配置保存生效；传输日志可追溯；证书按生命周期管理 |
| **异常** | 证书过期时 MFT 配置自动停用（active=false）；端点不可达时按重试策略处理；fileHash 校验失败时标记 FAILED |
| **跨域协作** | b2b（EDI 事务关联 MFT 传输）；master-data（Partner 关联）；certificate（证书管理） |
