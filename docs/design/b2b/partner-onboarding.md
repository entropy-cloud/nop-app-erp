# B2B 合作伙伴上线设计

## 目的

设计 Trading Partner 从注册到正式上线的完整流程：伙伴档案管理、上线阶段管控、测试消息交换、认证清单检查、生产切换与归档。确保每位 Trading Partner 在生产环境对接前通过必要的测试和认证。

## 设计依据

> 参考 **行业 EDI 上线最佳实践**：注册 → 测试 → 认证 → 生产 四阶段标准上线流程。
>
> 参考 **SAP Trading Partner Management**：伙伴配置（协议/证书/凭证）、测试消息交换、认证清单管理。
>
> 来源 `docs/analysis/erp-survey/2026-06-30-0001-advanced-scenario-design-comparison.md` §B2B 集成。

## 上线阶段总览

```
REGISTERED ──→ TESTING ──→ CERTIFIED ──→ PRODUCTION
    │              │            │              │
    │              │            │              └─ 终态（生产运行中）
    │              │            │
    │              │            └─ 测试完成，认证合格
    │              │
    │              └─ 开始测试消息交换
    │
    └─ 注册基础信息
```

## ErpB2bPartnerProfile（Trading Partner 档案）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/orgId | 标准 | |
| partnerId | 关联 ErpMdPartner（我方供应商/客户） | |
| partnerName | 伙伴名称（从 ErpMdPartner 同步或独立维护） | |
| status | dict `erp-b2b/partner-status`：REGISTERED / TESTING / CERTIFIED / PRODUCTION / SUSPENDED / TERMINATED | |
| protocol | dict `erp-b2b/protocol`：AS2 / SFTP / HTTP / HTTPS / OFTP2 | 🟢 行业 EDI 协议标准 |
| transportEndpoint | 传输端点 URL / 地址 | |
| authMethod | dict：HMAC / BASIC_AUTH / CERTIFICATE / OAUTH2 | |
| webhookSecret | Webhook 签名密钥（加密存储） | |
| certExpiry | 证书过期日期 | |
| certFingerprint | 证书指纹（SHA256） | |
| allowedFormats | 支持的 EDI 格式列表（JSON array of formatCodes） | |
| timezone | 时区（用于预约/调度） | |
| contactName/contactEmail/contactPhone | 联系人信息 | |
| notes | 备注（对接注意事项） | |
| goLiveDate | 上线日期 | |
| archivedAt | 归档日期（从 PRODUCTION 下线时） | |
| 标准审计字段 | | |

### ErpB2bPartnerCredential（伙伴凭证）

| 字段 | 含义 |
|------|------|
| id/partnerProfileId | 标准 |
| credentialType | dict：API_KEY / USERNAME_PASSWORD / CERTIFICATE / SSH_KEY |
| credentialKey | 凭证标识（如用户名、keyId） |
| credentialValue | 凭证值（AES 加密存储） |
| issuedAt | 颁发日期 |
| expiresAt | 过期日期 |
| isActive | 是否启用 |

## 上线阶段

### Stage 1: REGISTERED（已注册）

#### 触发条件

B2B 管理员手动注册新的 Trading Partner，或从 ErpMdPartner 同步创建。

#### 完成事项

- [x] 填写/同步 ErpB2bPartnerProfile 基础信息
- [x] 配置传输协议和端点
- [x] 配置认证方法并交换凭证
- [x] 选择支持的 EDI 格式（从 ErpB2bEdiFormat 中选择）

#### 后置条件

- 状态 → REGISTERED
- 通知 B2B 管理员进入下一阶段

---

### Stage 2: TESTING（测试中）

#### 触发条件

B2B 管理员确认伙伴信息完整后，手动推进到 TESTING 阶段。

#### 测试消息交换

| 方向 | 测试内容 | 验证目标 |
|------|----------|----------|
| 出站 | 系统发送测试 EDI 报文到伙伴端点 | 伙伴正确接收并返回 ACK |
| 入站 | 伙伴推送测试 EDI 报文到系统 Webhook | 系统正确解析并返回 ACK |
| 双向 | 循环测试：发送 → 接收 → 验证 → 反馈 | 双方报文格式和内容一致 |

### ErpB2bTestExchange（测试消息交换记录）

| 字段 | 含义 |
|------|------|
| id/partnerProfileId | 标准 |
| direction | dict：OUTBOUND / INBOUND |
| formatCode | 测试的 EDI 格式 |
| testCaseCode | 测试用例编号（如 TC-001：标准采购订单） |
| sentPayload / receivedPayload | 发送/接收报文内容 |
| expectedResult | 预期结果 |
| actualResult | 实际结果 |
| passed | 测试是否通过 |
| testedBy | 测试人 |
| testedAt | 测试时间 |
| notes | 测试备注 |

#### 测试用例模板

| 测试用例 | 说明 | 预期结果 |
|---------|------|----------|
| TC-001: 标准采购订单 | 发送标准采购订单 EDI | 伙伴正确接收，返回 997/ACK |
| TC-002: 空数量订单 | 发送数量为 0 的订单行 | 伙伴正确拒绝或标记 |
| TC-003: 错误格式 | 发送不合法的报文 | 伙伴返回错误响应 |
| TC-004: 标准发票 | 发送标准发票 EDI | 伙伴正确接收并处理 |
| TC-005: 超大文件 | 发送 10MB+ 的报文 | 传输通道正常 |
| TC-006: ASN 回传 | 伙伴发送 ASN | 系统正确解析并创建 ErpB2bAsn |

#### 后置条件

- 测试用例通过率 ≥ 90%（可配置 `erp-b2b.onboarding-test-pass-rate`）
- 关键测试用例（TC-001、TC-004）必须通过
- 无 blocking_level=ERROR 的未处理问题
- B2B 管理员可手动推进到 CERTIFIED

---

### Stage 3: CERTIFIED（已认证）

#### 触发条件

测试阶段全部通过，B2B 管理员点击"认证完成"。

#### 认证清单

### ErpB2bCertificationChecklist（认证清单）

| 字段 | 含义 |
|------|------|
| id/partnerProfileId | 标准 |
| checklistItem | 检查项描述 |
| requiredDocType | 对应的 EDI 文档类型 |
| isMandatory | 是否必检 |
| isPassed | 是否通过 |
| checkedBy | 检查人 |
| checkedAt | 检查时间 |
| evidence | 检查证据（如日志链接、截图卷） |

#### 标准认证检查项

| # | 检查项 | 必检 | 验证方式 |
|---|--------|------|----------|
| 1 | 传输连接测试通过 | 是 | 出站测试成功发送 3 次 |
| 2 | 入站端点可达 | 是 | 伙伴成功推送测试报文 |
| 3 | 采购订单格式正确 | 是 | TC-001 通过 |
| 4 | 发票格式正确 | 是 | TC-004 通过 |
| 5 | ASN 格式正确 | 否 | TC-006 通过 |
| 6 | 证书未过期 | 是 | 检查 certExpiry |
| 7 | 凭证配置完整 | 是 | 检查 ErpB2bPartnerCredential |
| 8 | 联系人信息完整 | 是 | 检查 contactEmail/contactPhone |
| 9 | 错误处理流程确认 | 是 | 双方确认错误通知机制 |
| 10 | 安全协议确认 | 是 | 确认传输加密和签名机制 |

#### 后置条件

- 所有必检项 isPassed=true
- B2B 管理员确认后 → 状态进入 PRODUCTION

---

### Stage 4: PRODUCTION（生产中）

#### 触发条件

认证清单全部通过，B2B 管理员手动切换。

#### Go-Live 流程

```
CERTIFIED → PRODUCTION
    │
    ├─► 确认切换时间窗口（需双方协调）
    │
    ├─► 系统自动执行：
    │     ├─ 停用测试端点（可选）
    │     ├─ 更新生产端点配置
    │     ├─ 启用生产凭证
    │     ├─ 归档测试证书（保留审计）
    │     └─ 设置 goLiveDate = now()
    │
    ├─► 发送"上线通知"给伙伴联系人
    │
    ├─► 上线后监控 24 小时（验证首批业务 EDI 正常）
    │
    └─► 如发现问题 → 回退到 TESTING 阶段
```

#### 生产监控

| 监控项 | 阈值 | 通知 |
|--------|------|------|
| EDI 发送失败率 | > 5% 在 1 小时内 | 通知 B2B 管理员 |
| 入站解析失败 | 连续 3 条 | 通知 B2B 管理员 |
| 证书即将过期 | 到期前 30 天 | 通知管理员更新 |
| 伙伴端点不可达 | 连续 5 次重试失败 | 升级通知 |

## 伙伴状态机

```
REGISTERED（已注册）
  └─ 开始测试 → TESTING（测试中）
                  ├─ 测试通过 → CERTIFIED（已认证）
                  │               ├─ 上线 → PRODUCTION（生产中）
                  │               │               ├─ [上线后问题] → TESTING（回退）
                  │               │               └─ [停用] → TERMINATED（已终止）
                  │               └─ [停用] → TERMINATED
                  └─ [测试失败/放弃] → TERMINATED

REGISTERED / TESTING / CERTIFIED / PRODUCTION → [临时暂停] → SUSPENDED
SUSPENDED → [恢复] → 原阶段
TERMINATED（终态，可归档）
```

| 迁移 | 触发人 | 前置条件 |
|------|--------|----------|
| REGISTERED→TESTING | B2B 管理员 | 基本配置完整 |
| TESTING→CERTIFIED | B2B 管理员 | 测试通过率达标，必检用例通过 |
| CERTIFIED→PRODUCTION | B2B 管理员 | 认证清单全部通过 |
| PRODUCTION→TESTING | B2B 管理员 | 生产中出现协议/格式问题，需重新测试 |
| *→SUSPENDED | B2B 管理员 | 伙伴暂停合作 |
| SUSPENDED→* | B2B 管理员 | 恢复后回到暂停前阶段 |
| *→TERMINATED | B2B 管理员 | 合作关系终止 |

## 下线与归档

### 触发条件

- 合作伙伴关系终止
- 协议到期不续约
- 切换 EDI 提供商

### 下线流程

```
管理员选择：下线（Terminate）
    │
    ├─► 确认下线时间
    │
    ├─► 系统执行：
    │     ├─ 停用所有凭证（Credential.isActive=false）
    │     ├─ 标记档案状态 → TERMINATED
    │     ├─ 保留历史 EDI 记录（ErpB2bEdiDoc 只读）
    │     └─ 设置 archivedAt
    │
    └─► 可随时查看归档伙伴的历史 EDI 数据（审计追溯）
```

## 业务规则

1. **不可跳过阶段**：状态必须按 REGISTERED → TESTING → CERTIFIED → PRODUCTION 顺序流转，不允许跳过（除暂停/终止外）。
2. **测试环境隔离**：测试消息使用独立端点或测试标记，不与生产数据混淆。
3. **测试数据标记**：测试阶段产生的 ErpB2bEdiDoc 标记 `isTest=true`，不参与生产报表和统计。
4. **凭证轮换**：生产阶段的凭证建议每 90 天轮换一次，系统提前 30 天通知管理员。
5. **生产回退保护**：从 PRODUCTION 回退到 TESTING 时，系统自动停用生产凭证并归档生产配置，不丢失任何生产 EDI 记录。

## 跨域协作

| 对端 | 协作方式 |
|------|---------|
| master-data（Partner） | ErpB2bPartnerProfile 关联 ErpMdPartner（1:1） |
| b2b（EdiDoc/EdiFormat） | 伙伴上线后，EDI 事务关联 partnerProfileId |
| notification | 上线通知、证书到期提醒、测试结果通知 |

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-b2b.onboarding-test-pass-rate` | 0.9 | 测试通过率达标门槛 |
| `erp-b2b.onboarding-credential-rotation-days` | 90 | 凭证轮换周期（天） |
| `erp-b2b.onboarding-cert-expiry-reminder-days` | 30 | 证书到期提前提醒天数 |
| `erp-b2b.onboarding-test-timeout-hours` | 72 | 测试阶段超时未完成则降级提醒 |
| `erp-b2b.onboarding-production-monitor-hours` | 24 | 上线后监控窗口（小时） |

## 反模式警示

- ⛔ **跳过测试阶段直接上线**——未经测试的 EDI 对接会导致生产事故。必须经过 TESTING → CERTIFIED 阶段。
- ⛔ **测试与生产共用凭证**——测试使用独立凭证/端点，上线时切换到生产凭证。共用凭证导致审计混乱。
- ⛔ **合作伙伴下线后删除档案**——历史 EDI 记录用于审计追溯，不得物理删除。仅标记 TERMINATED 并归档。
- ⛔ **阶段回退丢失测试记录**——TESTING → CERTIFIED → TESTING 回退时，保留原有测试记录作为参考。
- ⛔ **证书过期无预警**——必须提前 30 天预警证书过期，避免生产中断。

## 证据强度

| 证据 | 强度 | 说明 |
|------|------|------|
| 四阶段上线流程（注册→测试→认证→生产） | 🟢 | 行业 EDI 上线标准实践 |
| 测试消息交换 + 测试用例 | 🟢 | 行业 EDI 测试规范 |
| 认证检查清单 | 🟢 | SAP Trading Partner Management |
| 证书管理与凭证轮换 | 🟢 | 安全传输通用要求（AS2/SFTP/HTTPS） |
| 生产回退保护机制 | ⚪ | 领域设计 |

## 参考

- `b2b/README.md`（B2B 模块总述）
- `b2b/use-cases.md`（EDI 格式配置与出站入站流程）
- `b2b/edi-formats.md`（EDI 格式 SPI 细节）
- `b2b/asn-processing.md`（ASN 入站对接）
- `b2b/state-machine.md`（ErpB2bEdiDoc 状态机）
