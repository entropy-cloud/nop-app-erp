# B2B 集成 / EDI / ASN 设计文档

## 目的

本设计文档描述 module-b2b 的业务语义、工作流、状态含义和跨域协作契约。B2B 集成模块提供电子数据交换（EDI）、提前发货通知（ASN）、代码映射和 EDI 日志功能。

本模块定位为**独立扩展工程**（18 域产品基线第二批扩展域之一），按需组装。

## 模块定位

- **决策**：独立扩展工程 `module-b2b`（逻辑工程名 `app-erp-b2b`），是 `product-scope.md` 18 域正式基线之一（第二批扩展域）。
- **命名约定**：实体 `ErpB2b*`，表名 `erp_b2b_*`，字典 `erp-b2b/*`，appName `erp-b2b`（两级）。
- **核心零污染**：全程弱指针反查 purchase/sales/inventory，核心域零字段新增（凭证指针模式）。
- **双层分工**：本文拥有**业务语义**（状态机、用例、页面）；`docs/architecture/b2b-integration.md` 拥有**集成契约**（EDI SPI、Webhook、技术边界）。两者相互引用。

## 设计依据

### 主证：Odoo account_edi（源码全读）

1. **EDI 格式 = 可插拔 SPI + 适用性派发**：`IErpB2bEdiProvider.getApplicability(relatedBillType)` 返回 {outbound, inbound, batchable}（Odoo `account_edi_format.py:58-68`）。
2. **EDI 信封表 + 状态机**：to_send/sent/to_cancel/cancelled + error + blocking_level + attachment + UNIQUE(format, source)（Odoo `account_edi_document.py:14-58`）。
3. **双向**：导出 builder（generatePayload）+ 导入 decoder（parsePayload）。
4. **异步发送**：web service 类走异步队列/cron。
5. **Webhook 出站复用** `integration-pattern.md` 的 `ErpSysWebhookConfig`/`ErpSysWebhookLog`（不另造）。

### 弱参考：ERPNext edi

ERPNext `erpnext/edi/doctype/` 仅 `code_list`/`common_code` 两 doctype，是代码映射表，非完整 EDI 引擎。仅借鉴"代码映射"概念。

## 实体总览

| 实体 | 表名 | 职责 |
|------|------|------|
| ErpB2bEdiFormat | erp_b2b_edi_format | EDI 格式配置（UBL/X12/EDIFACT/CUSTOM） |
| ErpB2bEdiDoc | erp_b2b_edi_doc | EDI 信封/事务——核心状态机表 |
| ErpB2bAsn | erp_b2b_asn | 提前发货通知（ASN）头 |
| ErpB2bAsnLine | erp_b2b_asn_line | ASN 明细行 |
| ErpB2bCodeMapping | erp_b2b_code_mapping | 内外代码映射（物料/伙伴/单位） |
| ErpB2bEdiLog | erp_b2b_edi_log | EDI 交互日志（请求/响应报文） |

## 业务规则

### 规则 1：异步发送

`needsWebService=true` 的 EDI 格式走异步队列，不阻塞业务单据审核。

### 规则 2：信封状态机事务跟踪

每条 EDI 事务在 `ErpB2bEdiDoc` 有独立状态机，失败可重试，状态可查。

### 规则 3：blocking_level 阻断流转

EDI 发送/接收的严重错误设 blocking_level=ERROR，可阻断业务单据继续流转（视配置 `erp-b2b.error-blocks-flow`）。

### 规则 4：ASN 必须挂 sourceEdiDocId

每条 ASN 可追溯到来源 EDI 报文（审计追溯）。

### 规则 5：Webhook 出站复用

复用 `ErpSysWebhookConfig`/`ErpSysWebhookLog`（见 `architecture/integration-pattern.md`），不另造 webhook 表。

### 规则 6：核心零污染

全程弱指针反查 purchase/sales/inventory，核心域零字段新增。凭证指针模式指本模块持有 `relatedBillType` + `relatedBillCode` 但不持有外键。

### 规则 7：ASN 不直接写库存

ASN 只是"通知"，库存写入由 purchase 域的采购入库单决定（可部分收货、质检、拒收）。ASN 与采购入库单是 1:N 弱关联。

### 规则 8：SPI 适用性派发

EDI 格式通过 `IErpB2bEdiProvider.getApplicability(relatedBillType)` 判断是否处理某类业务单据。新增 EDI 格式 = 1 个 `@Service` Provider bean + 对应 ErpB2bEdiFormat 配置记录，零改核心。

## 状态定义

### ErpB2bEdiDoc 状态（edi-doc-state）

| 状态 | 业务含义 |
|------|----------|
| TO_SEND | 待发送（业务单据已审核，等待 EDI 发送） |
| SENT | 已发送（EDI 报文已发出，等待对方确认） |
| TO_CANCEL | 待取消（已发送的 EDI 需取消） |
| CANCELLED | 已取消（EDI 已取消，终态） |
| ERROR | 错误（发送/接收失败） |
| RECEIVED | 已接收（入站 EDI 报文已接收，等待处理） |
| ACKNOWLEDGED | 已确认（对方已接收确认，终态） |
| ARCHIVED | 已归档（入站处理完成，终态） |

### ASN 状态（asn-status）

| 状态 | 业务含义 |
|------|----------|
| RECEIVED | 已接收 ASN 通知 |
| MATCHED | 已匹配采购订单 |
| RECEIVED_TO_STOCK | 已入库（purchase 域完成入库） |
| CANCELLED | 已取消 |

### EDI 方向（edi-direction）

| 值 | 含义 |
|----|------|
| OUTBOUND | 出站（本系统发送给 Trading Partner） |
| INBOUND | 入站（从 Trading Partner 接收） |
| BOTH | 双向 |

### EDI 标准（edi-standard）

| 值 | 含义 |
|----|------|
| UBL | Universal Business Language (OASIS) |
| X12 | ANSI ASC X12（北美） |
| EDIFACT | UN/EDIFACT（欧洲/国际） |
| CUSTOM | 自定义格式 |

### Blocking Level（blocking-level）

| 值 | 含义 |
|----|------|
| INFO | 信息性，不影响业务流转 |
| WARN | 警告，不阻断流转但需关注 |
| ERROR | 错误，可阻断业务单据流转 |

## 跨域协作

| 对端 | 协作方式 |
|------|----------|
| purchase | ASN 入站弱指针关联采购订单/入库单（ASN 不直接写库存） |
| sales | 销售发票/订单 EDI 导出（弱指针反查） |
| inventory | 不直接协作（ASN 经 purchase 入库才写库存） |
| master-data | 引用 ErpMdPartner（供应商/客户）、ErpMdMaterial |
| integration-pattern | 复用 ErpSysWebhookConfig/Log + HMAC 验签 |

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-b2b.enabled` | false | B2B 集成模块是否启用 |
| `erp-b2b.async-send-cron` | — | 异步发送 cron（needsWebService=true 的格式） |
| `erp-b2b.error-blocks-flow` | false | EDI ERROR 是否阻断业务单据流转 |

## SPI 契约（技术细节见 architecture/b2b-integration.md）

### IErpB2bEdiProvider

核心方法：

- `getCode()` — 格式标识，对应 ErpB2bEdiFormat.code
- `getApplicability(relatedBillType)` — 适用性派发，返回 {outbound, inbound, batchable}
- `generatePayload(relatedBillType, relatedBillCode)` — 导出：业务单 → EDI 报文
- `parsePayload(formatCode, payload)` — 导入：EDI 报文 → 业务数据
- `needsWebService()` — 是否需 web service（true 走异步队列）

### ErpB2bEdiRegistry

SPI 注册中心，按 code 自动聚合所有 `IErpB2bEdiProvider` bean，支持按 formatCode + relatedBillType 找适用 Provider。

## 菜单归属

新增 b2b 域 TOPM「B2B 集成」（可选），分组：

- EDI 格式（ErpB2bEdiFormat）
- EDI 事务（ErpB2bEdiDoc）
- ASN 管理（ErpB2bAsn）
- 代码映射（ErpB2bCodeMapping）
- EDI 日志（ErpB2bEdiLog）

## ASN 入站处理流程

```
供应商发货 → 推送 EDI（ASN 报文）到 Webhook 入站
      │
      ▼
Webhook HMAC 验签
      │
      ▼
IErpB2bEdiProvider.parsePayload() 解析报文 → ParsedPayload
      │
      ▼
建 ErpB2bEdiDoc(state=RECEIVED) + ErpB2bAsn(sourceEdiDocId=该 EDI Doc) + 明细
      │
      ▼
弱指针关联采购订单（ErpB2bAsn.relatedBillType=PO_ORDER）
      │
      ▼ ⚠️ ASN 不直接写库存
[等待采购决定] → purchase 域基于 ASN 创建采购入库单 → 写库存
```

## 反模式警示

- ⛔ **单层 `IErpB2BIntegrationProvider`（无适用性派发）**——不同格式处理不同单据，必须按 relatedBillType 派发。
- ⛔ **把 ERPNext edi 当 EDI 引擎主证**——ERPNext edi 仅 code_list/common_code 映射表，非引擎。仅借鉴"代码映射"概念。
- ⛔ **EDI 引擎烘焙进核心域**——EDI 是集成层，必须独立工程 + 弱指针反查。
- ⛔ **ASN 直接写库存**——ASN 是通知，库存写入由 purchase 入库单决定（可部分收货/质检/拒收）。
- ⛔ **格式与业务单据硬编码耦合**——必须通过适用性派发（getApplicability）解耦格式与单据类型。
- ⛔ **不记录 EDI 报文原文**——EDI 交互必须留日志以便审计和排错。
- ⛔ **同步发送阻塞业务单据审核**——`needsWebService=true` 必须走异步队列。

## 证据强度

| 证据 | 强度 | 说明 |
|------|------|------|
| EDI 格式适用性派发 | 🟢 | Odoo `account_edi_format.py:58-68` 源码实测 |
| EDI 信封状态机 | 🟢 | Odoo `account_edi_document.py:14-58` 源码实测 |
| EDI 双向（builder+decoder） | 🟢 | Odoo `sale_edi_ubl/models/sale_order.py:7-30` 源码实测 |
| 异步发送 | 🟢 | Odoo `account_edi_format.py:40-42` 源码实测 |
| ERPNext edi 非引擎（仅映射） | 🟢 | 源码确认仅 code_list/common_code |
| Webhook 复用 | 🟢 | 本项目 `integration-pattern.md` ErpSysWebhookConfig/Log |
| ASN 不直接写库存 | 🟢 | 业务常识（可部分收货/质检/拒收） |
| X12/EDIFACT 标准细节 | ⚪ | 领域常识，延迟到客户需求 |

## 参考

- `docs/architecture/b2b-integration.md`（集成层契约和 SPI 技术细节）
- `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §3.3（设计依据）
- `docs/design/l10n/cn-golden-tax.md`（独立工程范式样板）
- `docs/architecture/integration-pattern.md`（Webhook 出站/入站复用）
- `docs/requirements/product-scope.md:49-52`（延迟范围）
