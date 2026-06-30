# B2B 集成 / EDI / ASN（集成层）

## 目的

设计电子数据交换（EDI）、提前发货通知（ASN）与 B2B 集成的契约层：EDI 格式 SPI + 信封状态机 + ASN 入站处理。补齐 P1 独立扩展模块缺口。

## 模块定位（Decision：独立扩展工程）

> **裁决**：B2B 集成定位为**可选独立扩展工程 `module-b2b`**，**不纳入 product-scope 的 10 域基线**（`product-scope.md:49-52` 延迟范围），作为可选模块按需组装。

- 工程范式参考 `docs/design/l10n/cn-golden-tax.md`（独立工程 + 凭证指针反查核心域）。
- 命名：实体 `ErpB2b*`，表名 `erp_b2b_*`，字典 `erp-b2b/*`，appName `app-erp-b2b`。
- **本文档位置**：本架构文档描述 B2B 集成契约（集成层语义）；ASN 等实体表前缀归 `module-b2b` 工程。
- **考虑的替代方案**：纳入核心域子模块（拒绝，EDI 引擎烘焙进核心域是反模式，见下）。

**实施级设计延迟声明**：本文为**设计骨架**（模块定位 + 最小实体清单 + **SPI 适用性派发 + 信封状态机契约**），深化到实施级（X12/EDIFACT 标准细节、具体格式实现）延迟到**客户外部集成需求确认**时触发。

## 边界

- 本模块负责：EDI 格式配置、EDI 信封/事务跟踪（发送/接收状态机）、ASN 入站、内外代码映射、EDI 日志。
- **核心零污染**：全程弱指针反查 purchase/sales/inventory，核心域零字段新增（凭证指针模式，见 `cn-golden-tax.md:109`）。
- 本模块不负责：业务单据本身（purchase/sales/inventory 域）；库存写入（ASN 不直接写库存，由 purchase 决定）。

## 设计依据

> 来源 `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §3.3。**⚠️ 主证修正 + SPI 深度补充**：初版单 `IErpB2BIntegrationProvider` → **补"适用性派发 + 信封状态机表"**。

### 主证：🟢 Odoo account_edi（源码全读）

**⚠️ ERPNext edi 不可作主证**：🟢 源码确认 `erpnext/edi/doctype/` 仅 `code_list`/`common_code` 两 doctype，**是代码映射表，非完整 EDI 引擎**。仅可借鉴"代码映射"概念，弱参考。

### 核心设计点（🟢 Odoo account_edi）

1. **EDI 格式 = 可插拔 SPI + 适用性派发**：`IErpB2bEdiProvider.getApplicability(relatedBillType)` 返回 {outbound, inbound, batchable}（🟢 `account_edi_format.py:58-68` `_get_move_applicability`）。
2. **EDI 信封表 + 状态机**：to_send/sent/to_cancel/cancelled + error + blocking_level + attachment + UNIQUE(format, source)（🟢 `account_edi_document.py:14-58`）—— ASN 必须有此事务跟踪。
3. **双向**：导出 builder（generatePayload）+ 导入 decoder（parsePayload）（🟢 `sale_edi_ubl/models/sale_order.py:7-30`）。
4. **异步发送**：web service 类走异步队列/cron（🟢 `account_edi_format.py:40-42`）。
5. **Webhook 出站复用** `integration-pattern.md` 的 `ErpSysWebhookConfig`/`ErpSysWebhookLog`（不另造）。

## 实体清单（最小骨架，标注延迟）

> 表前缀 `erp_b2b_`、类名 `ErpB2b*`、字典 `erp-b2b/*`。以下为建议命名，待客户需求触发后落地 ORM。

### ErpB2bEdiFormat（EDI 格式配置，表 `erp_b2b_edi_format`）

| 字段 | 含义 |
|---|---|
| id/code/orgId | 标准 |
| formatName | 格式名（UBL Invoice / X12 850 / EDIFACT ORDERS） |
| formatStandard | dict `erp-b2b/edi-standard`：UBL/X12/EDIFACT/CUSTOM |
| direction | dict `erp-b2b/edi-direction`：OUTBOUND/INBOUND/BOTH |
| needsWebService | 是否需要 web service 调用（true 走异步队列） |
| isActive | 是否启用 |
| 标准审计字段 | |

### ErpB2bEdiDoc（EDI 信封/事务，表 `erp_b2b_edi_doc`）— 核心状态机表

对应 🟢 Odoo `account_edi_document.py:14-58`。每条业务单据的 EDI 事务跟踪。

| 字段 | 含义 |
|---|---|
| id/orgId | 标准 |
| formatId | EDI 格式（→ErpB2bEdiFormat） |
| relatedBillType/relatedBillCode | 关联业务单弱指针（AR_INVOICE/SALES_ORDER/PO_RECEIVE 等，核心零污染） |
| state | dict `erp-b2b/edi-doc-state`：见状态机 |
| blockingLevel | dict `erp-b2b/blocking-level`：INFO/WARN/ERROR（阻断业务流转的错误级别） |
| error | 错误信息（state=error 时） |
| attachmentId | 附件（EDI 报文文件） |
| 唯一键 | UNIQUE(formatId, relatedBillType, relatedBillCode)（同一格式同一业务单只一条） |
| 标准审计字段 | |

**状态机**（对应 🟢 `account_edi_document.py:14-58`）：

```
待发送 (TO_SEND)
  ├─ 发送成功 → 已发送 (SENT)
  │              ├─ 取消请求 → 待取消 (TO_CANCEL) → 已取消 (CANCELLED, 终态)
  │              └─ 对方接收确认 → 已确认 (ACKNOWLEDGED, 终态)
  ├─ 发送失败 → 错误 (ERROR) → 重试 → TO_SEND
  (入站方向) 已接收 (RECEIVED) → 解析处理 → ARCHIVED
```

### ErpB2bAsn（提前发货通知，表 `erp_b2b_asn`）

| 字段 | 含义 |
|---|---|
| id/code/orgId | 标准 |
| sourceEdiDocId | 来源 EDI 文档（→ErpB2bEdiDoc，**ASN 必须挂来源 EDI**） |
| partnerId | 发货方（供应商，→ErpMdPartner） |
| shipmentDate/estimatedArrivalDate | 发货日/预计到货日 |
| trackingNo | 物流单号 |
| relatedBillType/relatedBillCode | 关联采购订单弱指针（PO_ORDER，核心零污染） |
| status | dict `erp-b2b/asn-status`：RECEIVED/MATCHED/RECEIVED_TO_STOCK/CANCELLED |
| 标准审计字段 | |

#### ErpB2bAsnLine（ASN 明细，表 `erp_b2b_asn_line`）

| 字段 | 含义 |
|---|---|
| id/asnId/lineNo | 标准 |
| materialId/supplierPartNo | 物料/供应商料号 |
| quantity/shippedQty | 订单数/发货数 |
| 标准审计字段 | |

### ErpB2bCodeMapping（内外代码映射，表 `erp_b2b_code_mapping`）

借鉴 🟢 ERPNext `code_list` 概念（弱参考，非主证）。

| 字段 | 含义 |
|---|---|
| id/orgId | 标准 |
| mappingType | dict：MATERIAL/PARTNER/UOM |
| internalCode | 内部代码 |
| externalCode | 外部代码（ Trading Partner 的代码体系） |
| partnerId | Trading Partner（不同伙伴不同映射） |
| 标准审计字段 | |

### ErpB2bEdiLog（EDI 交互日志，表 `erp_b2b_edi_log`）

| 字段 | 含义 |
|---|---|
| id/ediDocId/orgId | 标准 |
| direction | OUTBOUND/INBOUND |
| requestPayload/responsePayload | 请求/响应报文 |
| resultCode/resultMsg | 结果码/消息 |
| logTime | 日志时间 |
| 标准审计字段 | |

## SPI 契约（核心交付物：适用性派发 + 信封状态机）

### IErpB2bEdiProvider（EDI 格式提供者，可插拔）

对应 🟢 `account_edi_format.py:58-68`。适用性派发是核心——不是所有格式都处理所有单据，按 relatedBillType 派发。

```
interface IErpB2bEdiProvider {
    String getCode();                                    // 格式标识，对应 ErpB2bEdiFormat.code

    // 适用性派发（核心）：判断本格式是否处理某类业务单据
    Applicability getApplicability(String relatedBillType);
        // 返回 { outbound: true/false, inbound: true/false, batchable: true/false }

    // 导出（builder）：业务单 → EDI 报文
    String generatePayload(String relatedBillType, String relatedBillCode);

    // 导入（decoder）：EDI 报文 → 业务数据（ASN 等）
    ParsedPayload parsePayload(String formatCode, String payload);

    // 是否需要 web service（true 走异步队列，🟢 account_edi_format.py:40-42）
    boolean needsWebService();
}
```

```
class Applicability {
    boolean outbound;    // 本格式是否处理该单据类型的导出
    boolean inbound;     // 本格式是否处理该单据类型的导入
    boolean batchable;   // 是否可批量发送（合并多个单据一个报文）
}
```

### 注册中心（SPI 提供者注册中心统一范式）

```
@Component
class ErpB2bEdiRegistry {
    @Inject Map<String, IErpB2bEdiProvider> providers;  // 按 code 自动聚合

    // 按 formatCode + relatedBillType 找适用 Provider（适用性派发）
    IErpB2bEdiProvider getProvider(String formatCode, String relatedBillType) {
        IErpB2bEdiProvider p = providers.get(formatCode);
        if (p.getApplicability(relatedBillType) ... ) return p;
        ...
    }
}
```

> 新增 EDI 格式 = 1 个 `@Service` Provider bean + 对应 ErpB2bEdiFormat 配置记录，零改核心。

## ASN 入站处理流程

```
供应商发货 → 推送 EDI（ASN 报文）到 Webhook 入站
      │
      ▼
Webhook HMAC 验签（integration-pattern.md 入站签名验证）
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

**关键规则：ASN 不直接写库存**——ASN 只是"通知"，库存写入由 purchase 域的采购入库单决定（可部分收货、质检、拒收）。ASN 与采购入库单是 1:N 弱关联。

## 业务规则

1. **异步发送**：`needsWebService=true` 的 EDI 格式走异步队列（🟢 `account_edi_format.py:40-42`），不阻塞业务单据审核。
2. **信封状态机事务跟踪**：每条 EDI 事务在 `ErpB2bEdiDoc` 有独立状态机（TO_SEND/SENT/ERROR...），失败可重试，状态可查。
3. **blocking_level=ERROR 阻断流转**：EDI 发送/接收的严重错误（如签名失败、格式错误）设 blocking_level=ERROR，可阻断业务单据继续流转（视配置）。
4. **ASN 必须挂 sourceEdiDocId**：每条 ASN 可追溯到来源 EDI 报文（审计）。
5. **Webhook 出站复用** `ErpSysWebhookConfig`/`ErpSysWebhookLog`（`integration-pattern.md`），不另造 webhook 表。
6. **核心零污染**：全程弱指针反查 purchase/sales/inventory，核心域零字段新增。

## 跨域协作

| 对端 | 协作方式 |
|---|---|
| purchase | ASN 入站弱指针关联采购订单/入库单（ASN 不直接写库存） |
| sales | 销售发票/订单 EDI 导出（弱指针反查） |
| inventory | 不直接协作（ASN 经 purchase 入库才写库存） |
| integration-pattern.md | 复用 ErpSysWebhookConfig/Log + HMAC 验签 |

## 配置点

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `erp-b2b.enabled` | false | B2B 集成模块是否启用 |
| `erp-b2b.async-send-cron` | — | 异步发送 cron（needsWebService=true 的格式） |
| `erp-b2b.error-blocks-flow` | false | EDI ERROR 是否阻断业务单据流转 |

## 反模式警示

- ⛔ **单层 `IErpB2BIntegrationProvider`**（无适用性派发）——不同格式处理不同单据，必须按 relatedBillType 派发（🟢 `account_edi_format.py:58-68`）。
- ⛔ **把 ERPNext edi 当 EDI 引擎主证**——🟢 源码确认仅 code_list/common_code 映射表，非引擎。仅借鉴"代码映射"概念。
- ⛔ **EDI 引擎烘焙进核心域**——EDI 是集成层，必须独立工程 + 弱指针反查。
- ⛔ **ASN 直接写库存**——ASN 是通知，库存写入由 purchase 入库单决定（可部分收货/质检/拒收）。

## 菜单归属

新增 b2b 域 TOPM「B2B 集成」（可选），分组：EDI 格式、EDI 事务、ASN、代码映射、EDI 日志。

## 证据强度标注

| 证据 | 强度 | 说明 |
|---|---|---|
| EDI 格式适用性派发 | 🟢 | Odoo `account_edi_format.py:58-68` `_get_move_applicability` 源码实测 |
| EDI 信封状态机 | 🟢 | Odoo `account_edi_document.py:14-58` 源码实测（state+blocking_level+attachment+UNIQUE） |
| EDI 双向（builder+decoder） | 🟢 | Odoo `sale_edi_ubl/models/sale_order.py:7-30` 源码实测 |
| 异步发送 | 🟢 | Odoo `account_edi_format.py:40-42` 源码实测 |
| ERPNext edi 非引擎（仅映射） | 🟢 | `erpnext/edi/doctype/` 仅 code_list/common_code，源码实测 |
| Webhook 复用 integration-pattern | 🟢 | 本项目 `architecture/integration-pattern.md` ErpSysWebhookConfig/Log |
| X12/EDIFACT 标准细节 | ⚪ | 领域常识，延迟到客户需求 |

## 参考

- `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §3.3（设计依据）
- `docs/design/l10n/cn-golden-tax.md`（独立工程范式样板）
- `docs/architecture/integration-pattern.md`（Webhook 出站/入站复用）
- `docs/requirements/product-scope.md:49-52`（延迟范围）
