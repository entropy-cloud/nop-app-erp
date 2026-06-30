# B2B/EDI 域 — ASN 入站处理设计

## 目的

详细说明提前发货通知（ASN）的入站处理全流程：Webhook 接收 → HMAC 验签 → 报文解析 → ASN 实体创建 → 采购订单匹配 → 仓库准备 → 集成采购入库。本设计是 `b2b/README.md` §ASN 入站处理流程 和 `architecture/b2b-integration.md` §ASN 入站处理流程 的实施级深化，参考 🟢 Odoo `purchase_edi_ubl_bis3` 范式。

---

## 一、ASN 入站全流程

```
供应商/伙伴推送 EDI（ASN 报文）到 Webhook 入站端点
        │
        ▼
┌─────────────────────────────────────────────────────┐
│  Phase 1: Webhook 接收 (同步, 返回 202)              │
│   ├─ HMAC 签名验证                                    │
│   ├─ 幂等检查 (eventId / payload hash)                │
│   └─ 入异步队列                                       │
└─────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────┐
│  Phase 2: 报文解析 (异步)                             │
│   ├─ 格式识别 → IErpB2bEdiProvider.identifyProvider() │
│   ├─ parsePayload() → ParsedPayload                   │
│   ├─ 建 ErpB2bEdiDoc(state=RECEIVED)                 │
│   └─ 建 ErpB2bAsn(sourceEdiDocId) + 明细             │
└─────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────┐
│  Phase 3: 采购订单匹配                                │
│   ├─ ParsedPayload 中的 OrderReference → 查 PO       │
│   ├─ 物料匹配 (代码映射)                              │
│   ├─ 数量/日期验证                                    │
│   └─ ASN 状态: RECEIVED → MATCHED                    │
└─────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────┐
│  Phase 4: 仓库准备 (通知仓管)                         │
│   ├─ 预占 dock/appointment                           │
│   ├─ 生成待收货清单                                   │
│   └─ 通知对应采购员/仓管员                            │
└─────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────┐
│  Phase 5: 集成采购入库 (purchase 域)                  │
│   ├─ 仓管员基于 ASN 创建采购入库单                    │
│   ├─ 支持部分收货/质检/拒收                           │
│   └─ ASN 状态: MATCHED → RECEIVED_TO_STOCK          │
└─────────────────────────────────────────────────────┘
```

---

## 二、Phase 1：Webhook 接收

### 2.1 入站端点

```
POST /r/b2b/webhook/inbound/{formatCode}
```

| 路径参数 | 说明 |
|----------|------|
| formatCode | EDI 格式代码（ubl-asn / x12-856 / edifact-desadv） |

### 2.2 HMAC 签名验证

参照 🟢 本项目 `integration-pattern.md` 入站签名机制。

| 请求头 | 说明 | 必填 |
|--------|------|------|
| `X-Event-Id` | 事件唯一 ID（用于幂等） | 推荐 |
| `X-Signature` | HMAC-SHA256 签名 | 是 |
| `X-Timestamp` | 时间戳（防止重放攻击） | 推荐 |
| `X-Partner-Code` | 合作伙伴代码 | 是 |

**验证逻辑**：

```java
boolean verifyWebhook(HttpRequest request, ErpSysWebhookConfig config) {
    // 1. 查找 Partner → 获取 webhookSecret
    String secret = getPartnerWebhookSecret(request.getHeader("X-Partner-Code"));

    // 2. 取到原始 body
    String payload = request.getBody();

    // 3. 计算签名
    String expected = HmacUtils.hmacSha256Hex(secret, payload);

    // 4. 对比
    return MessageDigest.isEqual(expected.getBytes(),
        request.getHeader("X-Signature").getBytes());
}
```

### 2.3 幂等检查

| 策略 | 实现 |
|------|------|
| 幂等键 | `X-Event-Id` + `formatCode` |
| 存储 | Redis SETNX 或 DB UNIQUE(eventId, formatCode) |
| TTL | 24 小时后自动过期 |
| 重复请求 | 返回 200（已处理），不重复创建 EdiDoc |

### 2.4 接收响应

| 结果 | HTTP 状态 | 响应体 |
|------|-----------|--------|
| 验签通过 | 202 Accepted | `{ "status": "accepted", "ediDocId": null（异步） }` |
| 验签失败 | 401 Unauthorized | `{ "error": "signature verification failed" }` |
| 幂等命中 | 200 OK | `{ "status": "duplicate", "ediDocId": "已有 ID" }` |
| 格式不支持 | 400 Bad Request | `{ "error": "unsupported format: {formatCode}" }` |

---

## 三、Phase 2：报文解析

### 3.1 解析流程（异步队列）

```
Webhook 接收 → 入异步队列
        │
        ▼
异步消费者
        │
        ├─► 1. 根据 formatCode 找 IErpB2bEdiProvider
        │
        ├─► 2. 调用 provider.parsePayload(formatCode, rawPayload)
        │           │
        │           ├─► 成功 → 返回 ParsedPayload
        │           │
        │           └─► 失败 → 建 EdiDoc(state=ERROR)
        │                       保留报文到 EdiLog
        │                       通知管理员
        │
        ├─► 3. 建 ErpB2bEdiDoc
        │     state = RECEIVED
        │     formatId = formatCode 对应的 ErpB2bEdiFormat.id
        │     relatedBillType = ParsedPayload.relatedBillType
        │     relatedBillCode = ParsedPayload.relatedBillCode（采购订单号）
        │     UNIQUE(formatId, relatedBillType, relatedBillCode)
        │
        ├─► 4. 建 ErpB2bAsn
        │     sourceEdiDocId = 刚创建的 EdiDoc.id
        │     partnerId = 映射后的伙伴 ID
        │     shipmentDate / estimatedArrivalDate
        │     trackingNo
        │     relatedBillType = PO_ORDER
        │     relatedBillCode = 采购订单号（ParsedPayload.relatedBillCode）
        │     status = RECEIVED
        │
        ├─► 5. 建 ErpB2bAsnLine(s) (per ParsedLine)
        │     materialId = 映射后的物料 ID（通过代码映射）
        │     supplierPartNo = ParsedLine.supplierPartNo（原值保留）
        │     orderQty = ParsedLine.quantity
        │     shippedQty = ParsedLine.shippedQty
        │     unit = 映射后的单位
        │
        ├─► 6. 建 ErpB2bEdiLog
        │     direction = INBOUND
        │     actionType = RECEIVE
        │     requestPayload = rawPayload（收到的原始报文）
        │     resultCode = SUCCESS
        │
        └─► 7. EdiDoc state → ARCHIVED
              ASN 状态 = RECEIVED（等待 Phase 3 匹配）
```

### 3.2 无匹配格式的处理

```
formatCode 无对应 Provider
        │
        ├─► 建 EdiDoc(state=RECEIVED, 但无 formatId)
        ├─► blocking_level = ERROR
        ├─► 保留 rawPayload 到 EdiLog
        └─► 通知管理员：未识别的 EDI 格式，需人工处理
```

### 3.3 解析失败的处理

```
parsePayload 抛出异常
        │
        ├─► EdiDoc state = ERROR
        ├─► blocking_level = ERROR
        ├─► error = 异常消息
        ├─► 保留 rawPayload 到 EdiLog（responsePayload = 错误详情）
        └─► 通知 B2B 管理员：EDI 报文解析失败
```

---

## 四、Phase 3：采购订单匹配

### 4.1 匹配流程

```
ErpB2bAsn(status=RECEIVED) 创建完成
        │
        ▼
异步触发匹配逻辑
        │
        ├─► 1. 根据 relatedBillType(=PO_ORDER) + relatedBillCode 查采购订单
        │           │
        │           ├─► 找到采购订单 → 继续
        │           │
        │           └─► 未找到 → 保留 ASN(status=RECEIVED)
        │                         ASN.relatedBillCode 保留原值
        │                         通知管理员：采购订单不存在，需手工匹配
        │
        ├─► 2. 逐行匹配物料
        │     for each (ErpB2bAsnLine line) {
        │         line.materialId（已代码映射） vs purchaseOrderLine.materialId
        │     }
        │
        ├─► 3. 数量验证
        │     ┌──────────────────────┬───────────────┐
        │     │ 场景                 │ 行为           │
        │     ├──────────────────────┼───────────────┤
        │     │ ASN 数量 ≤ 订单数量  │ 正常匹配       │
        │     │ ASN 数量 > 订单数量  │ 标记超额        │
        │     │                      │ blocking_level=WARN │
        │     │ ASN 部分交货          │ 标记部分交货    │
        │     └──────────────────────┴───────────────┘
        │
        ├─► 4. 日期验证
        │     estimatedArrivalDate > PO 要求日期
        │         → blocking_level=WARN，通知采购员
        │
        └─► 5. ASN 状态 → MATCHED
              EdiDoc 状态 → ARCHIVED
```

### 4.2 物料代码映射

```
ErpB2bAsnLine.supplierPartNo ("SUP-001")
        │
        ▼
查 ErpB2bCodeMapping(partnerId = ASN.partnerId, mappingType = MATERIAL, externalCode = "SUP-001")
        │
        ├─► 找到 → internalCode = "MAT-001"
        │           ErpB2bAsnLine.materialId = MAT-001
        │
        └─► 未找到 → materialId = null
                      line 标记"未映射"
                      系统创建"待映射"任务
```

### 4.3 无匹配 PO 的处理

| 场景 | 处理 |
|------|------|
| PO 未到达但 ASN 先到 | ASN 创建，status=RECEIVED，relatedBillCode 保留原值，系统定时重试匹配 |
| 无 PO（直接补货）| ASN 创建，relatedBillCode 为空，管理员手工匹配或创建新 PO |
| PO 已关闭/取消 | ASN 创建，blocking_level=ERROR，通知管理员 |

### 4.4 匹配超时升级

```
ASN(status=RECEIVED) 超过 48 小时未匹配到 PO
        │
        ├─► 升级通知采购主管
        └─► 每 24 小时重复通知
```

---

## 五、Phase 4：仓库准备

### 5.1 预收货通知

```
ASN 匹配成功 (status=MATCHED)
        │
        ▼
生成预收货通知
        │
        ├─► 创建 Warehousing Preparation 记录（模块内部或通知 inventory 域）
        │
        ├─► 通知内容：
        │     ├─ ASN 编号 / 采购订单号
        │     ├─ 供应商名称
        │     ├─ 预计到货日期 / 时间
        │     ├─ 物料清单（物料编码 + 名称 + 预计数量）
        │     ├─ 包裹数 / 托盘数
        │     └─ 特殊处理要求（温度控制、危险品、高价值）
        │
        └─► 递送到仓管员工作台
```

### 5.2 Dock / 收货月台预约

| 功能 | 说明 |
|------|------|
| 月台管理 | 根据预计到货日期和仓库，自动推荐可用月台 |
| 时间窗口 | 供应商可选择或系统分配收货时间窗口 |
| 冲突检测 | 同一月台同一时间不可重复预约 |
| 状态 | PENDING / CONFIRMED / ARRIVED / COMPLETED |

### 5.3 预收货清单

```
┌────────────────────────────────────────────────────┐
│ 预收货清单: ASN-2026-001                            │
│ 供应商: 某某五金 (VENDOR-001)                       │
│ 预计到货: 2026-07-01 14:00                          │
│ 月台: DOCK-03                                       │
├────────────────────────────────────────────────────┤
│ 订单行 │ 物料       │ 订单数 │ ASN 数 │ 待收货 │
│ 1      │ MAT-001    │ 1000  │ 1000   │ 1000   │
│ 2      │ MAT-002    │ 500   │ 500    │ 500    │
│ 总计   │            │ 1500  │ 1500   │ 1500   │
├────────────────────────────────────────────────────┤
│ 操作: [创建采购入库单] [调整数量] [标记到达]        │
└────────────────────────────────────────────────────┘
```

---

## 六、Phase 5：集成采购入库

### 6.1 ASN → 采购入库单

```
仓管员确认预收货清单
        │
        ▼
点击"创建采购入库单"
        │
        ▼
系统创建 ErpPurReceive（purchase 域）
        │
        ├─► 自动填充：
        │     ├─ vendorId = ASN.partnerId
        │     ├─ sourceBillType = "B2B_ASN"
        │     ├─ sourceBillCode = ASN.code
        │     ├─ warehouseId = 预约月台所属仓库
        │     └─ lines = ASN lines（可调整数量）
        │
        └─► 仓管员确认后提交审核
              → 入库审核 → 库存增加
```

### 6.2 部分收货流程

```
ASN 发货 1000 PCS
        │
├─► 第 1 次收货 600 PCS
│     ErpPurReceive 行数量 = 600
│     ASN 行标记已收 600 / 未收 400
│
├─► 第 2 次收货 400 PCS
│     ErpPurReceive 行数量 = 400
│     ASN 行标记已收 1000 / 全部完成
│
└─► ASN 状态 → RECEIVED_TO_STOCK（全部完成）
```

### 6.3 质检拦截点

```
ASN 入站 → 匹配 PO → 创建预收货
        │
        ├─► 物料 inspection_required = true
        │           │
        │           └─► 创建采购入库单后触发质检流程
        │                 质检合格 → 正式入库
        │                 质检不合格 → 退货/拒收
        │
        └─► 物料 inspection_required = false
                    │
                    └─► 直接入库
```

🟢 质检触发见 `quality/inspection-integration.md` §1.1。

### 6.4 ASN 状态与采购入库关系

```
ASN 状态机：
RECEIVED (Phase 2) → MATCHED (Phase 3) → RECEIVED_TO_STOCK (Phase 5, 全部入库)
     │                      │                      │
     └── CANCELLED          └── CANCELLED          └── (终态)

                     采购入库单 1 (600 PCS)
                         ↓ 入库审核
ASN line 1 (1000 PCS) → 已收 600
                         ↓
                     采购入库单 2 (400 PCS)
                         ↓ 入库审核
ASN line 1 → 已收 1000 / 全部完成 → ASN 状态 RECEIVED_TO_STOCK
```

### 6.5 超收与短收处理

| 场景 | 处理 |
|------|------|
| ASN 发货 > PO 数量（超收） | 创建入库单时提示超额，由仓管员决定是否超收（受 PO 超容差控制） |
| ASN 发货 < PO 数量（短收） | 正常部分收货，ASN 标记部分交货，PO 剩余数量等待后续 ASN |
| ASN 完全未到货（超期） | 超过预计到货日期 7 天 → ASN 标记异常，通知采购员跟进 |

---

## 七、SourceEdiDocId 追溯链

### 7.1 完整追溯路径

```
原始 EDI 报文 (rawPayload)
        │
        ▼
ErpB2bEdiLog (ediDocId = EDI-001)
        │
        ▼
ErpB2bEdiDoc (id = EDI-001, state = ARCHIVED)
        │
        ▼
ErpB2bAsn (sourceEdiDocId = EDI-001, code = ASN-001)
        │
        ▼
ErpPurReceive (sourceBillType = "B2B_ASN", sourceBillCode = "ASN-001")
        │
        ▼
库存移动记录 / 质检单 / 凭证
```

### 7.2 审计查询

| 查询 | SQL 模式 |
|------|----------|
| 根据采购入库单查原始 ASN 报文 | `ErpPurReceive.sourceBillCode → ErpB2bAsn.code → ErpB2bAsn.sourceEdiDocId → ErpB2bEdiDoc → ErpB2bEdiLog` |
| 根据 ASN 编号查入库进度 | `ErpB2bAsn.code → ErpPurReceive(sourceBillCode)` |
| 根据 EDI 报文 ID 查衍生业务 | `ErpB2bEdiDoc.id → ErpB2bAsn → ErpPurReceive → ...` |

---

## 八、异常场景汇总

| 异常场景 | 阶段 | 处理方式 |
|----------|------|----------|
| HMAC 验签失败 | Phase 1 | 401 拒绝，记录日志 |
| 幂等事件重复 | Phase 1 | 200 返回已有结果 |
| 格式不支持 | Phase 1 | 400 拒绝，通知管理员 |
| 报文解析失败 | Phase 2 | EdiDoc ERROR，保留报文人工资决 |
| 代码映射缺失 | Phase 2/3 | 物料行标记未映射，创建待映射任务 |
| 采购订单不存在 | Phase 3 | ASN 保留 RECEIVED，定时重试匹配 |
| 采购订单已关闭 | Phase 3 | ASN blocking_level=ERROR，通知管理员 |
| 数量超 PO | Phase 3 | 标记超额，blocking_level=WARN |
| 到货超期 | Phase 4 | ASN 异常标记，通知采购员跟进 |
| 部分入库反复 | Phase 5 | 正常，ASN 维护已收/未收计数器 |
| 质检不合格拒收 | Phase 5 | 退货处理，ASN 标记部分或全部取消 |
| 入库单数量 > ASN 数量 | Phase 5 | 以入库单为准，ASN 标记超额接收 |

## 九、配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-b2b.enabled` | false | B2B 集成模块是否启用 |
| `erp-b2b.asn.auto-match-retry-interval` | 30 min | ASN 采购订单匹配重试间隔 |
| `erp-b2b.asn.auto-match-max-retries` | 48 | 最大匹配重试次数（48×30min=24h） |
| `erp-b2b.asn.match-timeout-hours` | 48 | ASN 匹配超时（小时），超时后升级通知 |
| `erp-b2b.asn.partial-receipt-enabled` | true | 是否允许部分收货 |
| `erp-b2b.error-blocks-flow` | false | EDI ERROR 是否阻断业务单据流转 |

## 十、证据强度

| 证据 | 强度 | 说明 |
|------|------|------|
| ASN 入站全流程（Webhook→解析→匹配→入库）| 🟢 | Odoo `purchase_edi_ubl_bis3` 范式 |
| EDI 报文适用性派发 | 🟢 | Odoo `account_edi_format.py:58-68` |
| ASN 不直接写库存 | 🟢 | 业务常识（可部分收货/质检/拒收） |
| HMAC 验签（integration-pattern） | 🟢 | 本项目 `integration-pattern.md` |
| 采购订单匹配 | 🟢 | Odoo `purchase_edi_ubl_bis3/models/purchase_order.py` |
| 部分收货/分批入库 | 🟢 | 标准采购流程 |
| 月台预约管理 | ⚪ | 延迟到 WMS 深度集成时 |
| ASN → 质检触发 | 🟢 | 本项目 `quality/inspection-integration.md` |
| SourceEdiDocId 追溯 | 🟢 | 本项目 `b2b/README.md` 业务规则 §4 |

## 参考

- `b2b/README.md`（模块总述 + ASN 入站流程概述）
- `b2b/edi-formats.md`（EDI 格式 SPI + 报文解析）
- `b2b/state-machine.md`（ASN 状态机）
- `b2b/use-cases.md`（用例：入站 ASN 接收）
- `architecture/b2b-integration.md`（集成层契约）
- `architecture/integration-pattern.md`（Webhook + HMAC 验签）
- `quality/inspection-integration.md`（质检触发）
