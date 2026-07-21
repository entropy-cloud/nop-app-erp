# 幂等模式

## 定位

定义 nop-app-erp 中**幂等保证的横切机制目录**。本文是 `domain-design-guidelines.md §1.3`（原则声明："所有对外暴露的接口必须保证幂等"）的技术落位：为设计人员和实施者提供可选的幂等机制、选择矩阵和实现约束。

本文不重复各域设计文档中的域特定幂等细节。域设计文档引用本文的机制名称 + 幂等键即可，具体判断逻辑和异常处理见各域设计。

## 幂等机制目录

当前项目在 11 个域中使用以下 6 种幂等机制：

| # | 机制 | 本质 | 适用层 | 幂等键类型 | DB 约束需求 |
|---|------|------|--------|-----------|------------|
| M1 | **状态机守卫** | 状态跃迁前检查当前状态，目标状态已达成则空操作返回 | 同步操作（审核/审批/确认） | `(entityId, 目标状态)` | 不需要 |
| M2 | **Posted-flag 标志守卫** | 操作前检查 `posted=true` 时跳过，防止重复过账 | 业财过账 | `billHeadCode`（业务单号） | 不需要（标志位本身防重） |
| M3 | **EventId 去重** | 入口以事件 ID 查重，重复返回已有结果 | Webhook 入站（异步） | `(eventId, providerCode)` | **需要 UNIQUE** |
| M4 | **导入键去重** | 导入时按业务组合键查重，已存在则跳过 | 数据导入（银行对账单等） | `(账户, 日期, 交易编码)` 或 `refNo` | **需要 UNIQUE** |
| M5 | **冲销+重新生成** | 先红冲已执行操作再重新生成，保证期内只执行一次 | 周期运算（折旧/分摊） | `(资产编码, 期间)` 或类似 | 不需要 |
| M6 | **转换守卫** | 来源单已转换为目标单时阻止重复转换 | 单据转换 | 来源单 ID | 不需要（业务校验） |

## 选择矩阵

| 业务场景特征 | 推荐机制 | 可选兜底 |
|-------------|---------|----------|
| 用户主动触发的审核/确认/过账 | M1 状态机守卫 | M2 posted-flag（过账场景） |
| 外部系统回调/post-commit 事件 | M3 EventId 去重 | M1 状态机守卫（二次确认） |
| 第三方数据文件导入 | M4 导入键去重 | M3 的 eventId 变体 |
| 周期性批量运算（折旧/计提/汇总） | M5 冲销+重新生成 | M2 标志守卫（记录执行标记） |
| 单据转换（询价→订单、报价→订单） | M6 转换守卫 | M1 状态机守卫（来源单状态锁定） |

### 反模式

| 做法 | 为什么错 | 正确做法 |
|------|----------|---------|
| 唯一依赖 DB 事务原子性防重 | 重试时同一条数据已提交，事务不会阻止业务重复 | 用业务自然键做显式幂等检查 |
| 所有幂等用相同机制 | 异步 webhook 和同步审核的幂等方式完全不同 | 按选择矩阵匹配场景 |
| 幂等检查放在事务末尾 | 幂等检查应在操作入口第一件事做，而非计算完成后再检查 | 入口即检查 |
| 用 surrogate id（自增/雪花）作为幂等键 | 同一业务单号两次调用会生成不同 id，无法去重 | 用业务自然键（`code`、`refNo`、`eventId`） |

## 幂等键设计规则

### 规则 1：幂等键必须是业务自然键

| 场景 | 正确的幂等键 | 错误的幂等键 |
|------|------------|------------|
| 审核采购订单 | `purOrder.code`（单号） | `purOrder.id`（自增 id） |
| 过账凭证 | `voucher.billHeadCode`（凭证号） | `voucher.id` |
| 银行对账单导入 | `(fundAccountId, statementDate, bankTxnCode)` | 入库时间戳 |
| B2B webhook | `(eventId, formatCode)` | 消息体 hash |
| 资产折旧 | `(assetCode, period)` | 折旧批次 id |

### 规则 2：异步入口必须 DB 级 UNIQUE 兜底

- Webhook 入站、消息队列消费等异步路径，**不能只靠业务代码查重**（存在并发窗口）
- 必须在 DB 层面建 `<unique-key>`，接收事件时先 insert，冲突时 catch 唯一约束异常返回已有结果
- 参见 `integration-pattern.md`：Webhook 入站幂等处理
- **全域落地状态（2026-07-05，plan `2026-07-05-1000-1-unique-key-constraints.md`）**：18 域共 154 个 `<unique-key>` 已补齐——所有业务单据/主数据的 `code`（业务自然键）均配 `(code, orgId)` 或 `(code)` 全局唯一约束；单品追踪 `serialNo`（ErpInvSerialNumber/ErpB2bMftCertificate/ErpMntEquipment）配全局唯一。异步入口的 DB 级 UNIQUE 兜底已具备模型层基础。
- **异步入口组合幂等键核查（2026-07-06，plan `2026-07-05-2352-1-db-index-design.md` Decision 5）**：全仓核查确认无独立 webhook-eventId 入口实体；唯一异步入口列 `ErpB2bMftLog.messageId`（AS2 Message-ID）的去重查重路径由非唯一过滤索引 `IDX_B2B_MFT_LOG_MESSAGE_ID` 支撑（见下「过滤索引全域落地」）。结论：无需额外组合唯一键；本规则的异步入口 UNIQUE 兜底范围以现存 `code`/`serialNo` 唯一键 + 异步入口列过滤索引为完整覆盖。
- **过滤索引全域落地（2026-07-06，plan `2026-07-05-2352-1-db-index-design.md`）**：18 域共 916 个非唯一 `<index>` 已补齐——覆盖多租户前缀复合（`(orgId, docStatus)`/`(orgId, businessDate)`）、外键导航单列（所有 `*Id`）、状态/业务日期过滤。命名 `IDX_{TABLE_NO_ERP_PREFIX}_{COLS}`，与 `UK_` 范式一致。索引是性能优化（不改业务语义），运行时由 `ddl.xlib AddIndex` 从模型自动应用；手工生产部署 DDL 由各域 `deploy/sql/{dialect}/_create_index.sql` 提供（平台 `_create_*.sql.xgen` 模板不输出 CREATE INDEX）。索引策略 Decision（orgId 复合前缀 / delVersion 不入索引 / 选择性门槛）记录于该 plan Phase 1。

### 规则 3：同步操作以状态守卫为第一道防线

- 同步操作的幂等以状态机守卫为主（M1），无需 DB 唯一约束
- 状态守卫失败模式是"重复操作视为成功并返回当前实体"，对用户友好

### 规则 4：Posted-flag 不可配（稳定性约束）

- `posted` 标志的幂等守卫是**稳定性约束**，不可通过配置关闭（参见 `posting.md` §3.3）
- 任何设计文档声称 `posted` 幂等可关闭的，违反稳定性约束裁定

### 规则 5：幂等键必须在接口文档中声明

- 每个对外暴露的接口在其 owner doc 中必须明确标注幂等键
- 示例格式：`Idempotency-Key: ErpPurOrder.code` 或 `Idempotency-Key: (eventId, providerCode)`

## 各域幂等清单

| 域 | 幂等场景 | 机制 | 幂等键 | owner doc |
|---|---------|------|--------|-----------|
| 采购 | 审核 | M1 状态机守卫 | `(purOrder.id, APPROVED)` | `docs/design/purchase/state-machine.md` |
| 销售 | 审核 | M1 状态机守卫 | `(salOrder.id, APPROVED)` | `docs/design/sales/state-machine.md` |
| 库存 | 触发移动单 | M1 状态机守卫 | `(relatedBillType, relatedBillCode)` | `docs/design/inventory/state-machine.md` |
| 财务 | 过账 | M2 posted-flag | `billHeadCode` | `docs/design/finance/posting.md` |
| 财务 | 银行对账导入 | M4 导入键去重 | `(fundAccountId, statementDate, bankTxnCode)` / `refNo` | `docs/design/finance/bank-reconciliation.md` |
| 资产 | 折旧执行 | M5 冲销+重新生成 | `(assetCode, period)` | `docs/design/assets/depreciation-and-posting.md` |
| 资产 | 审核 | M1 状态机守卫 | `(asset.id, APPROVED)` | `docs/design/assets/state-machine.md` |
| 生产 | 报工 | M1 状态机守卫 | `(jobCard.id, REPORTED)` | `docs/design/manufacturing/state-machine.md` |
| 生产 | JobCard 生成 | M6 转换守卫 | 工单 ID | `docs/design/manufacturing/state-machine.md` |
| 物流 | 承运商下单 | M3 EventId 去重 | `referenceNo`（发运单号） | `docs/design/logistics/carrier-integration.md` |
| B2B | EDI ASN 入站 | M3 EventId 去重 | `(eventId, formatCode)` | `docs/design/b2b/asn-processing.md` |
| 合同 | 电子签回调 | M3 EventId 去重 | `(eventId, providerCode)` | `docs/design/contract/e-signature.md` |
| CRM | 线索转换 | M6 转换守卫 | 线索 ID | `docs/design/crm/` |

## 与现有 owner docs 的关系

| 文档 | 关系 |
|------|------|
| `domain-design-guidelines.md §1.3` | 原则声明层（"必须保证幂等"）；本文是技术落位 |
| `integration-pattern.md` | Webhook 入站的 eventId 去重（M3）的入口规范 |
| `integration-and-transaction-patterns.md` | posted-flag 幂等的概要提及 |
| `document-engine.md` | 单据三轴状态（docStatus/approveStatus/postedStatus）与 M1/M2 的关系 |
| 各域 `state-machine.md` | M1 状态机守卫的域特定状态定义和跃迁规则 |
| `posting.md` | M2 posted-flag 的三层模型和稳定性约束 |
| 各域 `use-cases.md` | 幂等需求的具体场景描述 |

## API client 重试与幂等（D1，plan `2026-07-21-1206-3`）

> 第三方 API client 调用的重试策略与幂等键设计，与本文 M3（EventId 去重）+ M4（导入键去重）互补；
> 完整范式（auth + rate limiting + lifecycle）见 [`external-api-integration-pattern.md §6.2`](./external-api-integration-pattern.md)。

要点（D1 §6.2）：

- **可重试条件**：5xx / 网络超时 / 429 Too Many Requests；**不可重试**：4xx 客户端错误（除 429）。
- **重试策略**：指数退避（base × 2^n，最大 maxRetries 次），参考 logistics `GatewayDispatcher`（`erp-log.gateway-max-retries` + `erp-log.retry-base-interval-secs`）。
- **API client 幂等键约定**：每个 mutation API 必须接受 `clientRequestId` 参数（业务自然键，如 `purOrder.code`），重复请求返回已有结果。
- **缓存幂等**：参考实现 master-data `ErpMdExchangeRateApiClientFactory` 用 cacheKey = `baseCurrency|sorted(targetCurrencies)|asOfDate` 做 TTL 缓存（300 秒），同一组合重复调用走缓存（隐式幂等）。
- **应用规则**：API client 幂等键设计必须遵循本文 §规则 1（业务自然键）+ §规则 5（接口文档中声明幂等键）。
