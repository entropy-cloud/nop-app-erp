# B2B/EDI 域状态机

> 本状态机按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度组织。

> **EDI 出站自动化整体 Deferred 注记**：
> 本状态机涉及的出站自动化控制点——**自动发送（TO_SEND→SENT）/ ACK 超时→ERROR / 自动重试（指数退避）/ ERROR>24h 升级通知**——均**未就绪**（全手工可达），属 **Deferred**。
> **理由**：(1) 整个 b2b 子系统 **config-gated OFF 默认**（`erp-b2b.enabled` default false，`ErpB2bConfigs.java:9,27`）→ 默认 config 零生产暴露；(2) MFT transport 是 **Mock-only Deferred SPI**（`MockTransportAdapter` 唯一 impl，真实 AS2/SFTP/FTPS = `managed-file-transfer.md §Non-Goal` follow-up）→ 出站自动化属 Deferred transport 集成范畴；(3) 所有状态迁移方法（`markSent`/`markAcknowledged`/`retry`/`cancel`/`archive`）手工可达（状态机不破坏，仅未自动化）；(4) `LOG.warn`/`error` 提供运维可见性；(5) 与同批（contract EXPIRED Job）missing-automation Deferred 范式一致（方案 A 采纳）。
> **伴随**：本文件自动化承诺 vs `README.md`/`managed-file-transfer.md` transport Deferred 文档内部不一致（本注记收敛）+ `TO_CANCEL` dict 死状态（见 §2 注）。
> **Successor**：MFT transport 真实对接（AS2/SFTP/FTPS）上线时实现 `ErpB2bEdiOutboundJob`（cron-gated，调用 `TransportManager.send` 推进 TO_SEND→SENT + ACK-timeout 扫描 SENT→ERROR + 自动重试 3 次指数退避 + ERROR>24h 升级经 `IErpSysNotificationBiz` + `erp-b2b.ack-timeout-seconds` config），config-gated `erp-b2b.enabled` + transport-enabled。各控制点落点见 §L-8/§4/§6/§8/§9 场景 C 详细注记；实现约定见文末「实现约定」；残留风险见文末「残留风险」。

## 适用对象：ErpB2bEdiDoc（EDI 信封/事务）

### 1. 状态定义

| 状态 | 业务含义（等待什么） | 业务单据影响 |
|------|----------------------|--------------|
| TO_SEND | 业务单据已审核，等待 EDI 发送 | 无阻塞（异步发送不阻塞审核） |
| SENT | EDI 报文已发出，等待对方接收确认 | 无（出站方向等待确认） |
| TO_CANCEL | 已发送的 EDI 需取消，等待取消确认（**dict 预留语义入口——两步取消 Deferred，见 §2 注**） | 无（业务单据可能已作废） |
| CANCELLED | EDI 事务已取消，终态 | 业务单据已经作废或取消 |
| ERROR | 发送/接收/解析失败 | 若 blocking_level=ERROR 且 `erp-b2b.error-blocks-flow=true` 则阻断 |
| RECEIVED | 入站 EDI 报文已接收，等待解析处理 | 无 |
| ACKNOWLEDGED | 对方已确认收到，终态（出站成功闭环）| 无 |
| ARCHIVED | 入站 EDI 已处理归档，终态 | 无 |

### 2. 迁移完整性

```
                          ┌──────────────────────────────────────────┐
                          │                                          │
                          ▼                                          │
TO_SEND ──(发送成功)──→ SENT ──(对方确认)──→ ACKNOWLEDGED(终态)
  │                      │
  │                      ├──(取消请求)──→ TO_CANCEL ──(确认取消)──→ CANCELLED(终态)
  │                      │
  │                      ├──(对方拒绝/超时未确认)──→ ERROR ──(重试)──→ SENT
  │                      │                                  │
  │                      └──(取消确认收到)──→ CANCELLED(终态) │
  │                                                         └──(放弃)──→ CANCELLED(终态)
  │
  └──(发送失败)──→ ERROR ──(重试)──→ TO_SEND
                        │
                        └──(放弃)──→ CANCELLED(终态)

RECEIVED ──(解析处理)──→ ARCHIVED(终态)
    │
    └──(解析失败)──→ ERROR ──(重试)──→ RECEIVED
```

> L-8 补：早期版本图中 SENT 只有 ACKNOWLEDGED/TO_CANCEL/CANCELLED 出边，
> 但 §6 角色权限表已列 `SENT→ERROR`，§4 异常路径也已隐含（"Trading Partner 返回错误响应→state=ERROR"）。
> 此处图与表对齐：补充 `SENT ──(对方拒绝/超时未确认)──→ ERROR` 出边及其重试/放弃路径。
> **触发条件**：对方拒绝（NACK）或超时未收到 ACK（`erp-b2b.ack-timeout-seconds` 默认 24h）。
>
> **L-8 自动化 Deferred 注**：上句「自动重试最多 3 次（指数退避）」+ ACK 超时触发均为**设计意图**，当前**Deferred**——无 `erp-b2b.ack-timeout-seconds` config（`ErpB2bConfigs` 无此键）+ 无 Job 扫描 SENT 超时。`SENT→ERROR` 仅由运营手工 `markError` 触发。**Deferred + Successor** 见文件顶部整体 Deferred 注记（`ErpB2bEdiOutboundJob` 接入 ACK-timeout 扫描 + 自动重试指数退避）。

> **§2 TO_CANCEL 中间态 Deferred 注**：图中 `SENT ──(取消请求)──→ TO_CANCEL ──(确认取消)──→ CANCELLED` 两步取消为**设计意图**，当前**Deferred**——实际取消经单步 `SENT→CANCELLED`（功能等价简化，`cancel` @BizMutation 直接迁移），`TO_CANCEL` 中间态在生产代码中**无独立迁移写入**。**Deferred**：单步取消是功能等价简化（`TO_CANCEL` 非死状态，而是预留语义入口），危害仅为取消确认语义缺失非数据破坏。dict `TO_CANCEL` 项**保留为预留**。**Successor**：业务要求两步取消确认（`SENT→TO_CANCEL→CANCELLED`）时实现 TO_CANCEL 迁移 + 确认动作（`markCancelConfirmed`）。

### 3. 终态与恢复

| 终态 | 可恢复？ | 备注 |
|------|----------|------|
| CANCELLED | 否 | 已取消的 EDI 事务不可恢复，需新建 |
| ACKNOWLEDGED | 否 | 已确认的出站 EDI 不可恢复 |
| ARCHIVED | 否 | 已归档的入站 EDI 不可恢复 |
| ERROR | 是 | ERROR 可重试回到 TO_SEND 或 RECEIVED |

### 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| EDI 发送超时（TO_SEND→ERROR） | 设 state=ERROR，blocking_level=WARN，记录错误信息 |
| **对方拒绝 NACK 或超时未确认（SENT→ERROR，L-8 补）** | 设 state=ERROR，blocking_level=WARN；超时阈值由 `erp-b2b.ack-timeout-seconds` 控制（默认 24h）；~~自动重试最多 3 次（指数退避），耗尽后保留 ERROR 等待人工介入~~（**自动化 Deferred**——见 §L-8 自动化 Deferred 注） |
| Trading Partner 返回错误响应 | 设 state=ERROR，blocking_level=根据错误严重程度设 WARN/ERROR |
| 同一业务单重复发送 | UNIQUE(formatId, relatedBillType, relatedBillCode) 防重复，返回已有记录 |
| ASN 报文解析失败 | 设 state=ERROR，保留原始报文到 EdiLog |
| Webhook 签名验证失败 | 拒绝请求，记录日志，设 blocking_level=ERROR |
| 已取消的 EDI 又收到确认 | 忽略确认，记录日志（已过终态） |
| 并发发送同一 EDI | 乐观锁 |

### 5. 可达性

- 从 TO_SEND 可达所有出站终态（ACKNOWLEDGED/CANCELLED）。
- 从 RECEIVED 可达入站终态（ARCHIVED）。
- 从 ERROR 可回到 TO_SEND 或 RECEIVED（重试），或进入 CANCELLED（放弃）。
- 无不可达状态，无死锁。终态（CANCELLED/ACKNOWLEDGED/ARCHIVED）无出边。

### 6. 角色与权限

| 迁移 | 执行角色 |
|------|----------|
| TO_SEND→SENT | ~~系统（EDI 发送器）/ 异步队列~~（**自动化 Deferred**，当前手工 `markSent`） | 
| SENT→ACKNOWLEDGED | ~~系统（收到对方确认）~~（**当前手工 `markAcknowledged`**；异步回调自动化随 `ErpB2bEdiOutboundJob` successor 落地） |
| **SENT→ERROR（L-8 补）** | ~~系统（对方拒绝 NACK 或 ACK 超时 `erp-b2b.ack-timeout-seconds` 默认 24h 触发）~~（**自动化 Deferred**——见 §L-8 自动化 Deferred 注；当前仅手工 `markError`） |
| SENT→TO_CANCEL | B2B 管理员（发起取消请求） |
| TO_CANCEL→CANCELLED | 系统（收到取消确认） |
| TO_SEND→ERROR / SENT→ERROR / RECEIVED→ERROR | 系统（发送/接收/解析异常） |
| ERROR→TO_SEND / ERROR→RECEIVED | B2B 管理员（手动重试 `retry`）或 ~~系统（自动重试策略）~~（**自动化 Deferred**——见 §L-8 自动化 Deferred 注） |
| ERROR→CANCELLED | B2B 管理员（确认放弃） |
| RECEIVED→ARCHIVED | 系统（ASN 处理完成） |

**危险操作：**
- **重试 ERROR**：确认错误已修复才执行，否则反复重试产生大量日志。
- **取消已发送（SENT→TO_CANCEL）**：需确认 Trading Partner 支持取消。
- **放弃（ERROR→CANCELLED）**：放弃后需人工确认业务单据的 EDI 未完成处理。

### 7. 外部依赖

| 外部场景 | 内部处理 |
|----------|----------|
| 业务单据审核触发 EDI 发送 | 业务域发布事件，本域订阅生成 ErpB2bEdiDoc(state=TO_SEND) |
| Trading Partner 推送入站 EDI | Webhook 入站 → 验签 → parsePayload → ErpB2bEdiDoc(state=RECEIVED) |
| Trading Partner 返回确认 | 异步回调 → 更新状态为 ACKNOWLEDGED |
| ASN 关联采购订单 | 弱指针 relatedBillType=PO_ORDER，purchase 域决定入库 |
| 业务单据作废联动 | 业务域发布事件，本域设 EDI 为 TO_CANCEL |

外部触发渠道：
- 业务单据审核事件（发送 EDI）。
- Webhook 入站（接收 EDI）。
- 异步回调（Trading Partner 确认）。
- B2B 管理员手工触发（重试/取消）。

### 8. TODO / 任务策略

| 状态 | 是否产生 TODO | TODO 类型 |
|------|---------------|-----------|
| TO_SEND | 否（系统异步处理） | — |
| SENT | 否 | — |
| TO_CANCEL | 是 | assigned（B2B 管理员）—— 等待取消确认 |
| CANCELLED | 否 | — |
| ERROR | 是 | assigned（B2B 管理员）—— 需人工介入处理错误 |
| RECEIVED | 否（系统处理解析）| — |
| ACKNOWLEDGED | 否 | — |
| ARCHIVED | 否 | — |

避免"ERROR 长期滞留"：~~ERROR 超过 24 小时未处理升级通知~~（**自动化 Deferred**——见 §L-8 自动化 Deferred 注；无 Job 扫描 ERROR 滞留 + 无 `IErpSysNotificationBiz` 派发；ERROR>24h 升级随 `ErpB2bEdiOutboundJob` successor 落地）。当前 ERROR 仅经 `LOG.warn`/`error` 提供运维可见性，需运营主动巡检。

### 9. 场景演练

#### 场景 A：出站销售发票 EDI（UBL Invoice）

1. 销售发票审核 → 触发事件。
2. 本域订阅 → 根据发票属性（relatedBillType=AR_INVOICE）查适用 EDI 格式（UBL Invoice）。
3. 建 ErpB2bEdiDoc(format=UBL Invoice, relatedBillType=AR_INVOICE, relatedBillCode=INV-001, state=TO_SEND)。
4. UblInvoiceEdiProvider.generatePayload() 生成 UBL XML。
5. 若需要 WebService → 入异步队列；否则直接 HTTP POST。
6. 发送成功 → state=SENT。
7. Trading Partner 返回 ApplicationResponse（确认）→ state=ACKNOWLEDGED。
8. 若对方拒绝 → state=ERROR，blocking_level=WARN。

#### 场景 B：入站 ASN 处理

1. 供应商推送 EDI 报文到 Webhook → HMAC 验签。
2. 根据报文格式找对应 Provider → parsePayload()。
3. 建 ErpB2bEdiDoc(state=RECEIVED)。
4. 建 ErpB2bAsn(sourceEdiDocId, partnerId, shipmentDate, trackingNo, relatedBillType=PO_ORDER, status=RECEIVED)。
5. 建 ErpB2bAsnLine（物料、数量、供应商料号）。
6. ASN 处理后 → state=ARCHIVED（不直接写库存）。
7. Purchase 域读取 ASN → 决定是否创建采购入库单。

#### 场景 C：错误重试

1. EDI 发送失败（网络超时）→ state=ERROR，blocking_level=WARN。
2. B2B 管理员检查错误：确认凭证无误、网络恢复。
3. 点击"重试" → state 回到 TO_SEND → 重新发送。
4. 若重试 3 次仍失败 → blocking_level=ERROR，需人工介入。
5. 若确认不再发送 → 管理员"放弃" → state=CANCELLED。

> **§9 场景 C 性质澄清注**：本场景描述**手工重试**（B2B 管理员点击"重试"）——经 `retry` @BizMutation 实现（手工可达）。「系统每 30 分钟自动重试」自动化承诺的**权威落点为 §6「系统（自动重试策略）」+ §4 + §L-8**（均见 Deferred 注）——本场景第 4 步「重试 3 次仍失败」的自动触发与指数退避同样属 Deferred（见文件顶部整体 Deferred 注记）。

### 10. 与设计文档一致性

- 状态定义见 `b2b/README.md`。
- ORM 模型见 `module-b2b/model/app-erp-b2b.orm.xml`。
- 使用场景见 `b2b/use-cases.md`。

### 实现约定

> **TransportManager wired-but-uncalled**：`TransportManager` bean 在 `erp-b2b-service/.../beans/app-service.beans.xml:49-50` 已 wired，但**生产代码零调用**（全 `module-b2b` 仅 test `TestErpB2bMftTransport.java:54` 引用）——`ErpB2bEdiDocBizModel` **不注入 `TransportManager`**（仅注入 `ErpB2bEdiRegistry`），出站发送委托从未接线。`createOutbound:93` 建 ErpB2bEdiDoc 后留 `state=TO_SEND`，不自动调用 `TransportManager.send`。属 Deferred transport 集成范畴（见文件顶部整体 Deferred 注记）：当前 `markSent`/`markAcknowledged`/`retry` 全手工（状态迁移方法齐全、手工可达），自动化推进留给 `ErpB2bEdiOutboundJob` successor。
>
> **bean comment vs 代码不一致（文档内部不一致收敛）**：`app-service.beans.xml:47-48` bean comment 称「被 EdiDocBizModel 出站发送委托调用」与代码不匹配（BizModel 不注入 TransportManager）。生产 beans 配置属 app 层打包，非本域 owner doc 范畴——本注记在此标注不一致以收敛，实际 bean comment 修正随 successor（TransportManager 出站接线时）收敛。
>
> **README/MFT transport 文档一致性**：`b2b/README.md`（`erp-b2b.async-send-cron` 占位）+ `managed-file-transfer.md §Non-Goal`（真实 AS2/SFTP/FTPS = follow-up）已显式 Deferred transport 层；本文件原自动化承诺（§L-8/§4/§6/§8）经本次 Deferred 标注后与之**对齐一致**——owner doc 内部不再矛盾。

### 残留风险

> **`erp-b2b.enabled`=true 时的运营依赖风险**：当 b2b 子系统被显式启用（`erp-b2b.enabled=true`，默认 false）时，出站 EDI 文档（TO_SEND/SENT/ERROR）生产环境**无自动化推进**——全依赖运营手工 `markSent`/`markAcknowledged`/`retry`。ERROR 状态文档可**静默悬挂**（仅 `LOG.warn`/`error` 可见性，无 ACK-timeout 扫描、无自动重试、无 ERROR>24h 升级通知），需运营主动巡检日志。**收敛路径**：successor `ErpB2bEdiOutboundJob` 落地后闭合异步处理闭环（TO_SEND 自动发送 + ACK-timeout 扫描 + 自动重试指数退避 + ERROR>24h 升级经 `IErpSysNotificationBiz`）。在 successor 落地前，启用 b2b 子系统（`erp-b2b.enabled=true`）的部署应配套运营巡检 SOP。

## 适用对象二：ErpB2bAsn（提前发货通知）

### 状态定义

| 状态 | 业务含义 |
|------|----------|
| RECEIVED | 已接收 ASN 通知 |
| MATCHED | 已匹配采购订单 |
| RECEIVED_TO_STOCK | 已入库（purchase 域完成入库） |
| CANCELLED | 已取消 |

### 迁移

```
RECEIVED ──(匹配采购订单)──→ MATCHED ──(入库完成)──→ RECEIVED_TO_STOCK
    │                               │
    └──(取消)──→ CANCELLED          └──(取消)──→ CANCELLED
```

### ASN 与 ErpB2bEdiDoc 的关系

- ErpB2bAsn.sourceEdiDocId → ErpB2bEdiDoc（来源 EDI 报文）。
- EdiDoc 状态是报文级别的（RECEIVED→ARCHIVED）。
- ASN 状态是业务级别的（RECEIVED→MATCHED→RECEIVED_TO_STOCK）。
- ASN 进入 MATCHED 后 EdiDoc 才可进入 ARCHIVED（但 EdiDoc 也可先 ARCHIVED，ASN 再独立流转）。
