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
- **全域落地状态（2026-07-05，plan `2026-07-05-1000-1-unique-key-constraints.md`）**：18 域共 154 个 `<unique-key>` 已补齐——所有业务单据/主数据的 `code`（业务自然键）均配 `(code, orgId)` 或 `(code)` 全局唯一约束；单品追踪 `serialNo`（ErpInvSerialNumber/ErpB2bMftCertificate/ErpMntEquipment）配全局唯一。异步入口的 DB 级 UNIQUE 兜底已具备模型层基础（具体异步入口的组合幂等键，如 webhook eventId，仍按本规则在对应入口实体上补建）。

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
