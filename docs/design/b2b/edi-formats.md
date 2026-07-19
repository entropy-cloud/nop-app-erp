# B2B/EDI 域 — EDI 格式支持设计

## 目的

详细说明 EDI 格式 SPI（IErpB2bEdiProvider）的接口契约、适用性派发机制、各标准格式（UBL/X12/EDIFACT）的设计要点、内外代码映射、以及 EDI 信封事务状态机。本设计是 `b2b/README.md` §SPI 契约 和 `architecture/b2b-integration.md` 的实施级深化。

> **实现偏离补注（2026-07-04, plan 2200-1）**：
> - `ErpB2bEdiLog` ORM 无 `actionType`/`httpStatus` 列（§8.1 列出但未落地）→ 动作语义编码到既有 `direction`+`resultCode`+`resultMsg` 列，不新增列。
> - `ErpB2bEdiDoc.attachmentFileId` 为 OrmFileComponent（文件引用），不可存 raw payload → rawPayload 改存 `ErpB2bEdiLog.requestPayload`（CLOB）。
> - X12/EDIFACT Provider 为 Non-Goal（本期仅 UBL 样例）；真实 AS2/SFTP/FTPS 协议库集成归 follow-up。

---

## 一、Format SPI：IErpB2bEdiProvider

### 1.1 接口定义

参考 🟢 Odoo `account_edi_format.py:58-68`。

```java
public interface IErpB2bEdiProvider {

    // 格式唯一标识，对应 ErpB2bEdiFormat.code
    String getCode();

    // 适用性派发（核心）：判断本格式是否处理某类业务单据
    EdiApplicability getApplicability(String relatedBillType);

    // 导出（builder）：业务单 → EDI 报文
    String generatePayload(String relatedBillType, String relatedBillCode);

    // 导入（decoder）：EDI 报文 → 业务数据
    ParsedPayload parsePayload(String formatCode, String payload);

    // 是否需要 web service（true 走异步队列）
    boolean needsWebService();

    // 对方确认处理的回调（可选）
    void handleAcknowledgement(String relatedBillType, String relatedBillCode, Acknowledgement ack);
}
```

### 1.2 EdiApplicability

```java
public class EdiApplicability {
    boolean outbound;    // 本格式是否处理该单据类型的导出
    boolean inbound;     // 本格式是否处理该单据类型的导入
    boolean batchable;   // 是否可批量发送（合并多个单据为一个报文）
}
```

### 1.3 ParsedPayload

```java
public class ParsedPayload {
    String ediFormatCode;           // 格式标识
    String relatedBillType;         // 关联单据类型（PO_ORDER / ASN / INVOICE）
    String relatedBillCode;         // 关联单据编号
    String partnerCode;             // 业务伙伴编码
    List<ParsedLine> lines;         // 明细行
    Map<String, Object> headers;    // 头信息（日期、单号、总金额等）
    String rawPayload;              // 原始报文（用于日志追溯）

    public static class ParsedLine {
        String lineNo;
        String supplierPartNo;      // 供应商物料编码
        String materialCode;        // 内部物料编码（映射后）
        BigDecimal quantity;
        BigDecimal shippedQty;
        BigDecimal price;
        String unit;
        Map<String, Object> extensions;
    }
}
```

---

## 二、适用性派发机制

### 2.1 派发流程

```
业务单据审核事件（relatedBillType）
        │
        ▼
ErpB2bEdiRegistry.discover(relatedBillType)
        │
        ├─► 遍历所有 active 的 ErpB2bEdiFormat
        │           │
        │           └─► 按 formatId 找 Provider
        │                       │
        │                       └─► 调用 provider.getApplicability(relatedBillType)
        │                                   │
        │                                   ├─► outbound=true → 创建出站 TO_SEND EdiDoc
        │                                   │
        │                                   └─► inbound=true  → 准备接收/解析
        │
        └─► 无适用 Provider → 跳过，日志记录"无适用 EDI 格式"
```

### 2.2 适用性配置矩阵

| relatedBillType | UBL Invoice | UBL Order | UBL DespatchAdvice | X12 810 | X12 850 | X12 856 |
|-----------------|-------------|-----------|-------------------|---------|---------|---------|
| AR_INVOICE | outbound | — | — | outbound | — | — |
| SALES_ORDER | — | outbound | — | — | outbound | — |
| PO_ORDER | — | inbound | — | — | inbound | — |
| ASN_INBOUND | — | — | inbound | — | — | inbound |
| PURCHASE_RECEIPT | inbound | — | — | inbound | — | — |

### 2.3 注册中心：ErpB2bEdiRegistry

```java
@Component
public class ErpB2bEdiRegistry {
    @Inject Map<String, IErpB2bEdiProvider> providers;  // key = getCode()

    public IErpB2bEdiProvider getProvider(String formatCode) {
        return providers.get(formatCode);
    }

    // 按 relatedBillType 找适用的格式列表（出站方向）
    public List<IErpB2bEdiProvider> findOutboundProviders(String relatedBillType) {
        return providers.values().stream()
            .filter(p -> p.getApplicability(relatedBillType).isOutbound())
            .collect(Collectors.toList());
    }

    // 按 relatedBillType 找适用的格式列表（入站方向）
    public List<IErpB2bEdiProvider> findInboundProviders(String relatedBillType) {
        return providers.values().stream()
            .filter(p -> p.getApplicability(relatedBillType).isInbound())
            .collect(Collectors.toList());
    }

    // 根据负载内容识别格式（入站解析时使用）
    public IErpB2bEdiProvider identifyProvider(String payload) {
        // 策略：按 providers 顺序尝试，第一个 parsePayload 不抛异常的即为匹配
        for (IErpB2bEdiProvider p : providers.values()) {
            try {
                p.parsePayload(p.getCode(), payload);
                return p;
            } catch (EdiParseException e) {
                continue;
            }
        }
        return null;
    }
}
```

---

## 三、UBL 格式支持

### 3.1 UBL 标准文档

| UBL 文档类型 | 对应业务 | 方向 | 状态 |
|-------------|----------|------|------|
| UBL Invoice (Dian 2.1) | 销售发票 | OUTBOUND | 设计就绪 |
| UBL Order (OASIS Order) | 采购/销售订单 | BOTH | 设计就绪 |
| UBL DespatchAdvice | ASN（发货通知） | INBOUND | 设计就绪 |

### 3.2 UBL Invoice Provider 设计

**适用性**：`getApplicability("AR_INVOICE") → { outbound: true, inbound: false, batchable: false }`

**generatePayload 流程**：

```
ErpB2bEdiDoc(relatedBillType=AR_INVOICE, relatedBillCode=INV-001)
        │
        ▼
UblInvoiceEdiProvider.generatePayload("AR_INVOICE", "INV-001")
        │
        ├─► 查询 ErpSalInvoice(by code="INV-001")
        │
        ├─► 查询客户 ErpMdPartner（地址、税号、联系人）
        │
        ├─► 查询发票行物料代码映射 ErpB2bCodeMapping（内部物料 → 客户物料）
        │
        ├─► 构建 UBL Invoice XML：
        │     ├─ cac:AccountingSupplierParty
        │     ├─ cac:AccountingCustomerParty
        │     ├─ cac:InvoiceLine (per line) → 含 mappedItemCode
        │     ├─ cac:TaxTotal
        │     └─ cac:LegalMonetaryTotal
        │
        └─► 返回 XML 字符串
```

**关键 XML 抽象**：

```java
// 使用 Nop XPL 模板或 Java OXM 生成 XML
// 与 nop-entropy 的 document-engine 集成时：
//   - XPL 模板定义 UBL Invoice 结构
//   - 数据模型使用 ErpSalInvoice DTO 绑定
//   - 模板路径 lib/edi/ubl/tpl/ubl-invoice.xpl
String xml = XplTemplateHelper.render("/nop/edi/ubl/tpl/ubl-invoice.xpl", data);
```

### 3.3 UBL DespatchAdvice Provider 设计

**适用性**：`getApplicability("ASN_INBOUND") → { outbound: false, inbound: true, batchable: false }`

**parsePayload 流程**：

```
收到 UBL DespatchAdvice XML
        │
        ▼
UblDespatchAdviceEdiProvider.parsePayload("UBL_ASN", xml)
        │
        ├─► 解析 cac:DespatchSupplierParty → partnerId
        ├─► 解析 cac:DeliveryCustomerParty → 收货方
        ├─► 解析 cac:Delivery / dsp:EstimatedDeliveryDate
        ├─► 解析 cac:DespatchLine (per line) → materialId:
        │     ├─► 查 ErpB2bCodeMapping(by partnerId + externalCode = cac:Item/cac:SellersItemID)
        │     └─► 映射到内部 materialId
        ├─► 关联采购订单（cac:OrderReference → relatedBillCode）
        │
        └─► 返回 ParsedPayload { relatedBillType="PO_ORDER", lines:[...] }
```

---

## 四、X12 格式支持（概览）

### 4.1 X12 标准文档

| X12 交易集 | 对应业务 | 方向 | 状态 |
|-----------|----------|------|------|
| X12 810 (Invoice) | 销售发票 | OUTBOUND | ⚪ 延迟到客户需求 |
| X12 850 (Purchase Order) | 采购/销售订单 | BOTH | ⚪ 延迟到客户需求 |
| X12 856 (ASN/Ship Notice) | 发货通知 | INBOUND | ⚪ 延迟到客户需求 |

### 4.2 X12 报文结构

```
ISA*00*          *00*          *ZZ*SENDER         *ZZ*RECEIVER       *200630*1000*^*00501*000000001*0*T*:~
GS*IN*SENDER*RECEIVER*20200630*1000*1*X*005010~
ST*810*0001~
  BIG*20200630*INV-001~
  N1*BY*BUYER NAME*92*BUYERCODE~
  N1*SE*SELLER NAME*92*SELLERCODE~
  IT1*1*10*EA*100*PE*VC*MAT-001~
  TDS*1000~
SE*10*0001~
GE*1*1~
IEA*1*000000001~
```

| 段 | 含义 |
|----|------|
| ISA | 交换信封（发送方/接收方/日期/控制号） |
| GS | 功能组（功能标识符/发送方/接收方/版本） |
| ST | 交易集头（交易集标识符/控制号） |
| BIG | 发票头（日期/发票号） |
| N1 | 名称（买方/卖方等） |
| IT1 | 发票行（行号/数量/单价/产品编码） |
| TDS | 总金额 |
| SE | 交易集尾 |
| GE | 功能组尾 |
| IEA | 交换信封尾 |

### 4.3 X12 Provider 设计要点

```java
@Component
public class X12850PurchaseOrderProvider implements IErpB2bEdiProvider {
    @Override public String getCode() { return "X12_850"; }

    @Override
    public EdiApplicability getApplicability(String relatedBillType) {
        if ("SALES_ORDER".equals(relatedBillType)) {
            return new EdiApplicability(true, false, false);   // outbound
        }
        if ("PO_ORDER".equals(relatedBillType)) {
            return new EdiApplicability(false, true, false);   // inbound
        }
        return EdiApplicability.NONE;
    }

    @Override
    public String generatePayload(String relatedBillType, String relatedBillCode) {
        // 构建 ISA + GS + ST + BIG/BEG + N1 + IT1 + TDS + SE + GE + IEA
        // 使用 X12 序列化器
    }

    @Override
    public ParsedPayload parsePayload(String formatCode, String payload) {
        // 解析 ISA→GS→ST→... 层次结构
        // 提取 N1(by=N2 发送方)、PO1/IT1(物料行)
        // 代码映射
    }
}
```

---

## 五、EDIFACT 格式支持（概览）

### 5.1 EDIFACT 标准文档

| EDIFACT 消息类型 | 对应业务 | 方向 | 状态 |
|-----------------|----------|------|------|
| INVOIC (D96A) | 发票 | OUTBOUND | ⚪ 延迟到客户需求 |
| ORDERS (D96A) | 订单 | BOTH | ⚪ 延迟到客户需求 |
| DESADV (D96A) | 发货通知 | INBOUND | ⚪ 延迟到客户需求 |

### 5.2 EDIFACT 报文结构

```
UNB+UNOC:3+SENDER+RECEIVER+200630:1000+00000001'
UNH+1+INVOIC:D:96A:UN'
BGM+380+INV-001+9'
DTM+137:20200630:102'
NAD+BY::999 BUYERCODE'
NAD+SE::999 SELLERCODE'
LIN+1++MAT-001:VP'
QTY+47:10:PCE'
MOA+203:1000'
UNS+S'
MOA+79:1000'
UNT+10+1'
UNZ+1+00000001'
```

| 段 | 含义 |
|----|------|
| UNB | 交换头（语法/发送方/接收方/日期/控制号） |
| UNH | 消息头（消息类型/版本） |
| BGM | 报文开始（文档类型/编号） |
| DTM | 日期/时间 |
| NAD | 名称和地址（买方/卖方代码） |
| LIN | 行项（物料号/产品代码） |
| QTY | 数量 |
| MOA | 货币金额 |
| UNS | 分节控制 |
| UNT | 消息尾 |
| UNZ | 交换尾 |

### 5.3 EDIFACT Provider 设计要点

```java
@Component
public class EdifactDesadvProvider implements IErpB2bEdiProvider {
    @Override public String getCode() { return "EDIFACT_DESADV"; }

    @Override
    public ParsedPayload parsePayload(String formatCode, String payload) {
        // 解析 UNB → UNH → BGM → NAD → LIN+QTY+MOA → UNT → UNZ
        List<String> segments = unwrapSegmentGroups(payload);
        // NAD segment → partnerId
        // NAD+BY+::supplierCode → 查 ErpB2bCodeMapping
        // LIN → materialId 映射
        // 返回 ParsedPayload
    }
}
```

---

## 六、代码映射（内部 ↔ 外部）

### 6.1 ErpB2bCodeMapping

| 映射类型 | 内部代码 | 外部代码 | 伙伴 | 使用场景 |
|----------|----------|----------|------|----------|
| MATERIAL | MAT-001 | SUP-001 | 供应商 A | 入站 ASN 外部物料 → 内部物料 |
| MATERIAL | MAT-002 | BUY-002 | 客户 B | 出站订单内部物料 → 客户物料 |
| PARTNER | PTR-001 | VENDOR-001 | 供应商 A | 出站 EDI 发送方标识 |
| UOM | PCE | EA | 客户 B | 出站/入站单位转换 |

### 6.2 映射查找流程

```
出站方向 (generatePayload)
  内部代码（MAT-001）
        │
        ├─► 查 ErpB2bCodeMapping(by partnerId + MATERIAL + "MAT-001")
        │           │
        │           ├─► 找到 → 输出 externalCode ("SUP-001")
        │           │
        │           └─► 未找到 → 输出原值 ("MAT-001")，记录 WARN 日志
        │
        └─► 输出代码到 EDI 报文

入站方向 (parsePayload)
  外部代码（SUP-001）
        │
        ├─► 查 ErpB2bCodeMapping(by partnerId + MATERIAL + "SUP-001")
        │           │
        │           ├─► 找到 → 映射到 internalCode ("MAT-001")
        │           │
        │           └─► 未找到 → 保留外部代码，标记"未映射"，后续人工处理
        │
        └─► 写入 ParsedLine.materialCode
```

### 6.3 映射表管理

| 功能 | 说明 |
|------|------|
| 批量导入 | 支持 CSV/Excel 导入代码映射（供应商物料对照表） |
| 自动建议 | parsePayload 时遇到未映射代码，系统记录"待映射"列表 |
| 一键映射 | 未映射列表页支持人工选择内部代码完成映射 |
| 映射审计 | 每次映射创建/修改记录操作日志 |

---

## 七、EDI 信封事务状态机

### 7.1 ErpB2bEdiDoc 状态定义

| 状态 | 业务含义 | 出站/入站 |
|------|----------|-----------|
| TO_SEND | 待发送（业务单据已审核，等待 EDI 发送） | 出站 |
| SENT | 已发送（EDI 报文已发出，等待对方确认） | 出站 |
| TO_CANCEL | 待取消（已发送的 EDI 需取消） | 出站 |
| CANCELLED | 已取消（EDI 已取消，终态） | 出站/入站 |
| ERROR | 错误（发送/接收/解析失败） | 出站/入站 |
| RECEIVED | 已接收（入站 EDI 报文已接收，等待处理） | 入站 |
| ACKNOWLEDGED | 已确认（对方已确认收到，终态） | 出站 |
| ARCHIVED | 已归档（入站处理完成，终态） | 入站 |

### 7.2 状态迁移图

```
                          ┌─────────────────────────────────────┐
                          │                                     │
                          ▼                                     │
TO_SEND ──(发送成功)──→ SENT ──(对方确认)──→ ACKNOWLEDGED(终态)
   │                      │
   │                      ├──(取消请求)──→ TO_CANCEL ──(取消确认)──→ CANCELLED(终态)
   │                      │
   │                      └──(取消确认收到)──→ CANCELLED(终态)
   │
   ├──(发送失败)──→ ERROR ──(重试)──→ TO_SEND
   │                     │
   │                     └──(放弃)──→ CANCELLED(终态)
   │
   └──(业务单取消)──→ CANCELLED(终态)

RECEIVED ──(解析处理)──→ ARCHIVED(终态)
   │
   └──(解析失败)──→ ERROR ──(重试)──→ RECEIVED
                           │
                           └──(放弃)──→ CANCELLED(终态)
```

### 7.3 状态迁移规则

| 迁移 | 触发方式 | 前置条件 | 后置动作 |
|------|----------|----------|----------|
| →TO_SEND | 业务单据审核事件 | 对应 ErpB2bEdiDoc 不存在（UNIQUE 约束） | 生成 ErpB2bEdiLog |
| TO_SEND→SENT | 系统发送（同步或异步队列） | Provider.generatePayload() 成功 + HTTP 发送成功 | 更新 SENT 时间，记录 attachmentId |
| SENT→ACKNOWLEDGED | 对方回调/异步确认 | 回调 HMAC 验签通过 | 终态，不再流转 |
| SENT→TO_CANCEL | B2B 管理员操作 | 业务单据已取消或需要作废 | 发起取消请求到对方 |
| TO_CANCEL→CANCELLED | 对方确认取消 | 取消请求成功 | 终态 |
| →ERROR | 发送/接收/解析异常 | 任何步骤失败 | 记录 error 信息 |
| ERROR→TO_SEND | 管理员重试 | 错误已修复 | 重新调用 generatePayload |
| ERROR→CANCELLED | 管理员放弃 | 确认不再重试 | 终态 |
| →RECEIVED | Webhook 入站 | HMAC 验签通过 | 写入 EdiLog |
| RECEIVED→ARCHIVED | 系统解析完成 | ASN 已创建（或业务数据已写） | 终态 |
| RECEIVED→ERROR | 解析失败 | parsePayload 异常 | 保留原始报文 |
| ERROR→RECEIVED | 管理员重试 | 修正后重新解析 | 重新调用 parsePayload |

### 7.4 Blocking Level 流转

| blocking_level | 含义 | 行为 |
|----------------|------|------|
| INFO | 信息性，发送/接收成功 | 无阻断 |
| WARN | 警告（如代码未映射、部分失败） | 不阻断业务流转，产生通知 |
| ERROR | 严重错误（签名失败、格式错误） | 若 `erp-b2b.error-blocks-flow=true` 则阻断业务单据流转 |

---

## 八、EDI 交互日志

### 8.1 ErpB2bEdiLog

| 字段 | 说明 |
|------|------|
| ediDocId | 关联 EDI 事务（→ErpB2bEdiDoc） |
| direction | OUTBOUND / INBOUND |
| actionType | SEND / RECEIVE / CANCEL / ACKNOWLEDGE |
| requestPayload | 发送的报文（出站）或收到的报文（入站） |
| responsePayload | 对方的响应（出站）或本系统响应（入站） |
| httpStatus | HTTP 状态码 |
| resultCode | SUCCESS / ERROR |
| resultMsg | 结果描述 |
| logTime | 日志时间 |

### 8.2 日志核心作用

| 场景 | 日志用途 |
|------|----------|
| 出站发送失败 | 查看 requestPayload 排查报文错误 |
| 入站解析失败 | 查看 responsePayload 查看原始报文 |
| 审计追溯 | 按 ediDocId 查看完整交互历史 |
| 排错重试 | 对比重试前后的 payload 差异 |

---

## 九、新增 EDI 格式清单

| 步骤 | 操作 |
|------|------|
| 1 | 实现 `IErpB2bEdiProvider`（生成/解析逻辑） |
| 2 | 注册为 `@Service` bean（Nop IoC 自动发现） |
| 3 | 创建 `ErpB2bEdiFormat` 配置记录（code = getCode()） |
| 4 | 编写 `ErpB2bCodeMapping` 映射规则（如适用） |
| 5 | （可选）编写 XPL 模板（UBL 等 XML 格式） |

**新增 EDI 格式 = 1 个 `@Service` Provider bean + 配置记录，零改核心/Registry**（🟢 Odoo `account_edi_format.py` 范式）。

## 十、证据强度

| 证据 | 强度 | 说明 |
|------|------|------|
| EDI 格式适用性派发 | 🟢 | Odoo `account_edi_format.py:58-68` |
| EDI 信封状态机 | 🟢 | Odoo `account_edi_document.py:14-58` |
| EDI 双向（builder+decoder） | 🟢 | Odoo `sale_edi_ubl/models/sale_order.py:7-30` |
| 异步发送 | 🟢 | Odoo `account_edi_format.py:40-42` |
| 代码映射 | 🟢 | ERPNext `code_list` 概念（弱参考） |
| Webhook 复用 | 🟢 | 本项目 `integration-pattern.md` |
| UBL Invoice/Order/DespatchAdvice | 🟢 | OASIS UBL 2.1 规范 |
| X12 810/850/856 | ⚪ | 延迟到客户需求 |
| EDIFACT INVOIC/ORDERS/DESADV | ⚪ | 延迟到客户需求 |
| 代码映射自动建议 | ⚪ | 延迟到产品化阶段 |

## 参考

- `b2b/README.md`（模块总述）
- `b2b/state-machine.md`（状态机详情）
- `b2b/use-cases.md`（用例说明）
- `architecture/b2b-integration.md`（集成层契约）
- `architecture/integration-pattern.md`（Webhook 复用）
- `model/app-erp-b2b.orm.xml`（权威 ORM 模型）

## 附录：`createReceiveFromAsn` 行级回填实现注记（plan 2026-07-19-0849-1）

`ErpB2bAsnBizModel.createReceiveFromAsn(@Name("asnId") Long)` 早期实现仅建采购入库**头**（`ErpPurReceive`）。plan 2026-07-19-0849-1 扩展为迭代 `asn.lines` → 为每条 AsnLine 创建对应 `ErpPurReceiveLine`，使 ASN→采购入库链下游 approve/入库触发可达成（无 ReceiveLine 则后续链断裂）。

### 字段映射规则（权威）

| ReceiveLine 字段 | 来源（按优先级） | 备注 |
| --- | --- | --- |
| `receiveId` | 新建 `ErpPurReceive.id` | 头行关联 |
| `lineNo` | 透传 `AsnLine.lineNo` | Decision (c)① 透传避免重生成冲突 |
| `materialId` | 透传 `AsnLine.materialId` | null → 抛 `ERR_B2B_ASN_LINE_MATERIAL_REQUIRED` 守卫 |
| `uoMId` | `AsnLine.materialId` → `ErpMdMaterial.uoMId` 反查 | Decision (f)①——物料主数据 uoMId mandatory=true 必然有值；物料不存在 → 抛守卫 |
| `quantity` | `AsnLine.shippedQty` 优先，fallback `AsnLine.quantity` | shippedQty 优先取实际发货数量 |
| `unitPrice` | 经 `materialId` 反查 `ErpPurOrderLine.unitPrice` | Decision (a)①——PO line 价格透传，无 PO line → null |
| `taxRate` | 同 unitPrice 路径（PO line 反查） | 顺便透传 |
| `orderLineId` | 同 unitPrice 路径（PO line 反查） | 加强 ReceiveLine↔PO line 关联 |
| `amount` | `unitPrice × quantity`（HALF_UP scale=4），任一 null → null | Decision (b)① 派生 |
| `warehouseId` | 复用 `receive.warehouseId`（即 `po.warehouseId`） | Decision (d)②——AsnLine 无 warehouseId，行级统一入 Receive 头仓库 |
| `taxAmount` / `batchNo` | null | taxAmount 后续 Receive approve 时由 tax engine 计算；batchNo ASN 不携带 |

### 边界与守卫

- **空白 AsnLine 边界**（Decision (e)①）：0 行 AsnLine 合法——Receive 头仍创建 + 0 行 ReceiveLine，仅 LOG.warn 提示。理由：matchPurchaseOrder 已容许部分行未匹配，createReceiveFromAsn 保留头创建能力，行级空可后续手动补录。
- **materialId null/material 不存在守卫**：抛 `NopException(ERR_B2B_ASN_LINE_MATERIAL_REQUIRED)`，@BizMutation 事务回滚（Decision 实现策略 (b) 强一致），已写入的 Receive 头 + ReceiveLine 全部回滚。
- **迭代持久化**（Decision 实现策略 (a)①）：`for (asnLine : asnLines) { lineDao.newEntity(); setFields; lineDao.saveEntity(line); }`——简单迭代 saveEntity 模式，行数典型 ≤30，N 次 INSERT 性能可接受（对齐既有 createReceiveFromAsn 单 saveEntity + parseToAsn AsnLine 迭代 saveEntity 范式）。

### Config 门控

`erp-b2b.asn-auto-create-receive`（默认 false）。关闭时 `createReceiveFromAsn` 直接返回 null 跳过（不抛错），保持 0941-2 既有 config-gated 语义不变。

### 跨域依赖方向

- `app-erp-b2b-service` → `app-erp-purchase-dao`（pom 显式依赖，ErpPurReceive/ErpPurReceiveLine/ErpPurOrder/ErpPurOrderLine 编译期可达）。
- `app-erp-b2b-dao` → `app-erp-master-data-dao`（pom 显式依赖，ErpMdMaterial 编译期可达）。
- 反向（purchase/b2b → b2b）无依赖；purchase 经弱指针 `relatedBillType/Code` 引用 ASN（核心零污染设计：不在 `ErpPurReceive` 加 `asnId` 列）。
