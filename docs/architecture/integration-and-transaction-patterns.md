# 集成与事务模式

> **本文定位**：跨系统集成的通用事务模式（本地优先 + 幂等 + 外部结果所有权）。本文是集成层的**通用原则补充**，不是模板占位。
>
> **覆盖关系**：
> - "本地优先 + afterCommit"在业财过账场景的具体落地见 `data-dependency-matrix.md §4.1/§4.4` 与 `docs/design/finance/posting.md §总体架构`（三层模型第②层 ASYNC 经 `txn().afterCommit()` 解耦）。
> - "幂等性"在过账场景的落地见 `posting.md §posted 标志兜底`。
> - 本文聚焦**外部系统集成**（税控/银行/物流/电商等）的通用约束，是上述业财场景之外的补充。

## 本地优先规则

1. 影响本地状态、任务和日志的操作必须在任何外部写入之前通过本地验证并持久化。
2. 依赖成功本地事务的外部写入应在提交后钩子中运行（`txn().afterCommit()`，平台机制见 `transaction-boundaries.md`）。不要将此机制滥用于查询、轮询、加载数据或文档下载操作。
3. 如果外部操作必须在本地持久化之前返回 ID，则失败/回滚策略必须在同一业务方法中明确。切勿先推进流程然后稍后回填 ID。

## 幂等性

- 轮询和下载操作必须是幂等的。重复执行不得创建重复任务、关闭案例两次或附加重复项。
- 重复触发器（轮询重启、重复回调）必须可以安全重试。

## 外部结果所有权

- 当结果源自外部系统响应或外部决策时，本地系统负责启动、查询、下载、补充文档，并仅在外部结果明确后做出本地后续决策。本地页面不伪造外部行为。
- 当存在升级/升级的外部系统原生语义时（例如，在升级过程中重用相同的外部案例 ID），优先使用外部系统的原生语义。

## 相关文档

- `data-dependency-matrix.md §4` — 业财一体闭环的事务边界（本地优先在过账场景的落地）
- `docs/design/finance/posting.md` — 过账三层模型（afterCommit ASYNC + posted 兜底幂等）
- `cross-domain-constraints.md` — 跨域事务约束（库存同事务、凭证可配时序）
- `../nop-entropy/docs-for-ai/02-core-guides/concurrency-and-transactions.md` — 平台事务机制

## API client 事务边界（D1，plan `2026-07-21-1206-3`）

> 第三方 API client 调用与业务事务边界（auth pattern + rate limiting + client lifecycle 完整范式），见
> [`external-api-integration-pattern.md §6 API Client Lifecycle`](./external-api-integration-pattern.md)。

要点（D1 §6.3）：

- **本地优先**：影响本地状态的操作必须在任何外部写入之前持久化；外部写入在 `txn().afterCommit()` 钩子中运行（避免事务回滚后外部已生效）。
- **API client 不可在事务内阻塞**：长时间外部调用应异步（logistics tracking 经 cron 轮询，不在用户事务内同步等待）。
- **外部结果所有权**：结果源自外部系统响应时，本地系统负责启动/查询/下载，**不**伪造外部行为。
- **重试与幂等**：见 [`idempotency-pattern.md §API client 重试与幂等`](./idempotency-pattern.md) + [`external-api-integration-pattern.md §6.2`](./external-api-integration-pattern.md)。
