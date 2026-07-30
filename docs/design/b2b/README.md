# B2B 集成 / EDI / ASN（b2b）

## 目的

描述 module-b2b 的业务语义、工作流、状态含义和跨域协作。B2B 集成模块提供电子数据交换（EDI）、提前发货通知（ASN）、代码映射和 EDI 日志功能。

本模块定位为**独立扩展工程**（18 域产品基线第二批扩展域之一），按需组装。

## 模块定位（Decision：独立扩展工程）

- **决策**：独立扩展工程 `module-b2b`（逻辑工程名 `app-erp-b2b`），是 `../requirements/product-scope.md` 18 域正式基线之一（第二批扩展域）。
- **核心零污染**：全程弱指针反查 purchase/sales/inventory，核心域零字段新增（凭证指针模式）。
- **双层分工**：本文拥有**业务语义**（状态机、用例、页面）；`docs/architecture/b2b-integration.md` 拥有**集成契约**（EDI SPI、Webhook、技术边界）。两者相互引用。

## 边界

- 本模块负责：EDI 格式管理、EDI 事务（信封/状态机）、ASN 入站处理、内外代码映射、EDI 交互日志。
- 本模块不负责：库存写入（ASN 只是通知，入库由 purchase 域决定）；核心业务单据的审核与过账（核心域负责）；Webhook 通道本身（复用 `integration-pattern.md` 的 Webhook 配置/日志）。
- 持久化字段、字典、状态码以 `module-b2b/model/app-erp-b2b.orm.xml` 为权威源。
- 跨域协作规则见 `../domain-design-guidelines.md`，全局流程见 `../flow-overview.md`。

## 工程与模型

| 项 | 值 |
|----|----|
| Maven 工程 | `app-erp-b2b` |
| appName | `erp-b2b`（两级） |
| 权威模型 | `module-b2b/model/app-erp-b2b.orm.xml` |
| 实体包 | `app.erp.b2b.dao.entity` |
| 表前缀 | `erp_b2b_` |
| 类名前缀 | `ErpB2b*` |
| 字典命名空间 | `erp-b2b/*` |

## 核心业务对象

| 对象 | 业务含义 |
|------|----------|
| EDI 格式（ErpB2bEdiFormat） | EDI 格式配置：标准（UBL/X12/EDIFACT/CUSTOM）、方向（出站/入站/双向）、是否需 web service（决定是否走异步队列）。一个格式对应一个可插拔 Provider 实现 |
| EDI 事务/信封（ErpB2bEdiDoc） | 一条 EDI 事务的状态跟踪：方向、关联业务单（弱指针）、报文附件、状态机（待发送/已发送/待取消/已取消/错误/已接收/已确认/已归档）、blocking_level（INFO/WARN/ERROR） |
| ASN 头（ErpB2bAsn） | 提前发货通知：来源 EDI 报文、关联采购订单（弱指针）、ASN 状态、收货信息 |
| ASN 明细（ErpB2bAsnLine） | ASN 下的发运明细行：物料、数量、包装 |
| 代码映射（ErpB2bCodeMapping） | 内外系统代码映射（物料/伙伴/单位）：内部编码 ↔ 外部编码 + 映射类型 |
| EDI 日志（ErpB2bEdiLog） | EDI 交互日志：请求/响应报文、状态、错误信息，供审计与排错 |

字段、类型、精度、字典码以 `module-b2b/model/app-erp-b2b.orm.xml` 为权威源。

## 状态机

### EDI 事务状态（edi-doc-state）

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

详细迁移规则见 [`state-machine.md`](state-machine.md)。

### 字典值

- **EDI 方向（edi-direction）**：OUTBOUND（出站）/ INBOUND（入站）/ BOTH（双向）
- **EDI 标准（edi-standard）**：UBL / X12 / EDIFACT / CUSTOM
- **Blocking Level（blocking-level）**：INFO（信息性）/ WARN（警告，不阻断）/ ERROR（错误，可阻断业务单据流转）

## 跨域协作

| 协作场景 | 对端域 | 协作方式 |
|----------|--------|----------|
| ASN 入站关联采购 | purchase | ASN 弱指针关联采购订单/入库单（ASN 不直接写库存） |
| 销售单据 EDI 导出 | sales | 销售发票/订单 EDI 导出（弱指针反查） |
| 库存写入 | inventory | 不直接协作（ASN 经 purchase 入库才写库存） |
| 主数据引用 | master-data | 引用 ErpMdPartner（供应商/客户）、ErpMdMaterial |
| Webhook 通道 | integration-pattern | 复用 ErpSysWebhookConfig/Log + HMAC 验签 |

跨域调用走 `I*Biz` 接口，不做 ORM 层跨工程 `refEntityName`。

## 关键业务规则

1. **异步发送**：`needsWebService=true` 的 EDI 格式走异步队列，不阻塞业务单据审核。
2. **信封状态机事务跟踪**：每条 EDI 事务在 `ErpB2bEdiDoc` 有独立状态机，失败可重试，状态可查。
3. **blocking_level 阻断流转**：EDI 发送/接收的严重错误设 blocking_level=ERROR，可阻断业务单据继续流转（视配置 `erp-b2b.error-blocks-flow`）。
4. **ASN 必须挂来源 EDI**：每条 ASN 可追溯到来源 EDI 报文（审计追溯）。
5. **Webhook 出站复用**：复用 `ErpSysWebhookConfig`/`ErpSysWebhookLog`（见 `architecture/integration-pattern.md`），不另造 webhook 表。
6. **核心零污染**：全程弱指针反查 purchase/sales/inventory，核心域零字段新增。凭证指针模式指本模块持有 `relatedBillType` + `relatedBillCode` 但不持有外键。
7. **ASN 不直接写库存**：ASN 只是"通知"，库存写入由 purchase 域的采购入库单决定（可部分收货、质检、拒收）。ASN 与采购入库单是 1:N 弱关联。
8. **SPI 适用性派发**：EDI 格式按业务单据类型（relatedBillType）判断是否处理。新增 EDI 格式 = 1 个 Provider 实现 + 对应 ErpB2bEdiFormat 配置记录，零改核心。

## ASN 入站处理流程

1. 供应商发货并推送 EDI（ASN 报文）到 Webhook 入站端点。
2. Webhook 进行 HMAC 验签。
3. EDI Provider 解析报文为结构化业务数据。
4. 系统创建 EDI 事务（state=RECEIVED）+ ASN 头（挂来源 EDI）+ ASN 明细。
5. ASN 通过弱指针关联采购订单（`relatedBillType=PO_ORDER`）。
6. **ASN 不直接写库存**——等待采购决定：purchase 域基于 ASN 创建采购入库单后才写库存（可部分收货/质检/拒收）。

## 业财过账

B2B 模块本身不产生会计凭证。EDI/ASN 触发的采购入库或销售出库走 purchase/sales 域的标准过账流程。

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-b2b.enabled` | false | B2B 集成模块是否启用 |
| `erp-b2b.async-send-cron` | — | 异步发送 cron（needsWebService=true 的格式） |
| `erp-b2b.error-blocks-flow` | false | EDI ERROR 是否阻断业务单据流转 |

## 菜单归属

新增 b2b 域 TOPM「B2B 集成」（可选），分组：EDI 格式、EDI 事务、ASN 管理、代码映射、EDI 日志。

## 反模式警示

- ⛔ **单层集成 Provider（无适用性派发）**——不同格式处理不同单据，必须按 relatedBillType 派发。
- ⛔ **把代码映射表当完整 EDI 引擎**——代码映射仅是映射表，非引擎。仅借鉴"代码映射"概念。
- ⛔ **EDI 引擎烘焙进核心域**——EDI 是集成层，必须独立工程 + 弱指针反查。
- ⛔ **ASN 直接写库存**——ASN 是通知，库存写入由 purchase 入库单决定（可部分收货/质检/拒收）。
- ⛔ **格式与业务单据硬编码耦合**——必须通过适用性派发解耦格式与单据类型。
- ⛔ **不记录 EDI 报文原文**——EDI 交互必须留日志以便审计和排错。
- ⛔ **同步发送阻塞业务单据审核**——`needsWebService=true` 必须走异步队列。

## 本域文档

| 文档 | 职责 |
|------|------|
| `README.md`（本文件） | 域概览、EDI/ASN 模型、跨域协作 |
| `state-machine.md` | EDI 事务/ASN 状态机 |
| `edi-formats.md` | EDI 格式管理 |
| `asn-processing.md` | ASN 入站处理 |
| `managed-file-transfer.md` | 托管文件传输 |
| `partner-onboarding.md` | 伙伴接入 |
| `use-cases.md` | 用例说明 |
| `ui-patterns.md` | 页面与交互模式 |

## 参考

- `docs/architecture/b2b-integration.md`（EDI SPI 契约、Webhook、技术边界等集成层细节）
- B2B 集成设计参考业界 EDI 适用性派发与信封状态机实践（源码分析见 `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §3.3）
- `docs/design/l10n/cn-golden-tax.md`（独立工程范式样板）
- `docs/architecture/integration-pattern.md`（Webhook 出站/入站复用）
- `docs/requirements/product-scope.md`（延迟范围）
