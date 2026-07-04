# 计划作业调度（权威全局作业目录）

> 本文是 `nop-app-erp` 全部定时/批处理作业的**唯一权威目录**与 nop-job / nop-batch / nop-task / nop-message 四模块**职责划分与选择判据**的归属文档。
>
> 修订日期：2026-07-04（由计划 `docs/plans/2026-07-04-1600-1-batch-scheduling-architecture.md` 权威化重写）。原文档仅 53 行、登记 5 个作业、误把 DAG 算作 nop-job 能力——已据平台契约重写。
>
> **真相优先级**：本文档登记作业的"何时跑/调谁/配置键是什么"；各作业的**业务规则**归各域 owner doc（`docs/design/<domain>/`），不在本文重复。

---

## 0. 三项裁决（决策记录）

裁决依据：计划 `2026-07-04-1600-1` Task Route §三项 Decision 的 选择 / 替代 / 残留风险。平台契约证据来自 `nop-entropy/docs-for-ai/03-modules/nop-job.md`、`nop-batch.md`、`nop-task.md`。

| # | 裁决 | 选择 | 被拒替代 | 残留风险 |
|---|------|------|----------|----------|
| D1 | **nop-job vs nop-batch 选择判据** | 按"单次预估处理量 + 事务边界需求 + 断点续跑需求 + 失败重试粒度"四维裁决：单次处理量 ≥ 1万记录、或全表/大批量扫描、或需断点续跑/记录级幂等/记录级重试 → **nop-job（触发）+ nop-batch（执行）双层配合**；小数据量、定点查询少量记录、整作业一个事务可接受 → **nop-job 直接调 BizModel** | ① 全部用 nop-job + BizModel 内部分页循环（`non-bizmodel-orm-access.md:134-186` 纯 Java 方案）——无法满足断点续跑/记录级幂等/独立事务，失败需整体重跑，对折旧批量/对账等长作业不可接受，rejected；② 全部用 nop-batch（小作业如 CSAT 提醒也走分片，过度工程，rejected） | 阈值"1万"为推断（平台 docs-for-ai 未成文），实际应结合单记录处理耗时动态判断——审计需复核每个 nop-batch candidate 的量级标注有证据支撑 |
| D2 | **DAG 归属漂移修正** | 删除原"DAG 依赖"章节，改为"作业逻辑前置关系"（§4）：(1) 期末结账的 AR→AP→INV→AST→GL 模块顺序已由 `ErpFinAccountingPeriodBizModel.closePeriod` 内部编排承载（计划 `2026-07-02-1000-3` Phase 1 已实现），**不依赖跨作业 DAG**；(2) 真正的跨作业 DAG 编排（如归档→对账→报表链）若未来需要，引入 **nop-task `<graph>`**，而非 nop-job | ① 引入 nop-task 重构期末结账为跨作业 DAG（已实现的 BizModel 内部编排工作良好，无需重构，rejected）；② 保留 nop-job DAG 说法（与平台契约冲突，属漂移不可保留，rejected） | nop-task 引入门槛（运维需理解第三模块），但当前无真实跨作业 DAG 需求，仅登记演进路径 |
| D3 | **nop-job-local vs nop-job-service 演进路径** | 当前维持 **nop-job-local**（单机、作业定义内存态、适合 bootstrap 阶段；已接入 `app-erp-all`，见 `docs/logs/2026/06-23.md:14-17`）；迁移触发条件：多实例部署 / 作业需持久化与 AMIS 管理 / 需 misfire 补偿与阻塞策略时 → 引入 `nop-job-service` | 立即迁移 nop-job-service：`RpcJobInvoker` 单机 RPC 配置复杂（`docs/logs/2026/06-23.md:15` 已验证单机启动困难），bootstrap 阶段无必要，rejected | nop-job-local 重启丢失作业定义——本文登记的所有作业 cron 需在 `scheduler.yaml` 声明以保证重启重建（属各域 follow-up 接线工作，见 §8） |

---

## 1. 四模块职责与选择判据

### 1.1 模块边界（平台契约）

| 模块 | 职责 | 关键能力 | 平台证据 |
|------|------|----------|----------|
| **nop-job** | **何时触发（when）** | CRON / 固定频率 / 固定延迟 / 一次性触发；本地模式（`nop-job-local`，纯内存、重启重建、不支持 misfire/阻塞/超时/分片）与分布式模式（`nop-job-service`，持久化 + AMIS 管理 + misfire + 阻塞 + 超时 + 分片 + nop-retry 集成）。**nop-job 无 DAG 能力**（396 行通读无任何 DAG/graph 章节） | `nop-entropy/docs-for-ai/03-modules/nop-job.md:3-21,281-291` |
| **nop-batch** | **如何分块执行（how）** | Loader→Processor→Consumer 三段式，每个 Chunk 独立事务、`completedIndex` 断点续跑、`NopBatchRecordResult` 记录级幂等、`retryPolicy`/`skipPolicy` 记录级重试。平台明确适用场景含**过账兜底扫描、资产折旧批量计提、银行流水导入** | `nop-entropy/docs-for-ai/03-modules/nop-batch.md:5-15,230-238` |
| **nop-task** | **跨作业 DAG 编排** | `<graph>` 数据驱动 DAG；`dispatchMode`(partition/broadcast/bestFit/single) 是 task 拆分策略非作业依赖。用于编排多步骤/多作业的依赖链 | `nop-entropy/docs-for-ai/03-modules/nop-task.md:115`；`reusable-modules-overview.md:13-22` |
| **nop-message** | **跨进程推送** | 异步消息投递（post-commit 派发、跨进程解耦），区别于同进程的 nop-job 触发 | `nop-entropy/docs-for-ai/03-modules/reusable-modules-overview.md` |

**配合模式（平台已成文）**：nop-job 定时触发 → task step 经 `<batch:Execute>` 调 nop-batch 分 chunk 执行（`nop-batch.md:193-204`）。大数据量作业 = nop-job（触发）+ nop-batch（执行）双层。

### 1.2 四维裁决表

> 阈值"1万记录"为**推断值**（平台 docs-for-ai 未成文），旁证见 `nop-entropy/docs-for-ai/04-reference/non-bizmodel-orm-access.md:134-186`（"十万级以上大表"考虑加时间范围）。实际应结合单记录处理耗时动态判断。

| 维度 | nop-job 直调 BizModel | nop-job + nop-batch 分 chunk |
|------|----------------------|------------------------------|
| 单次预估处理量 | 小（< 1万）或定点查询少量记录 | 大（≥ 1万）或全表/大批量扫描 |
| 事务边界 | 整作业一个事务可接受 | 每 Chunk 独立事务 |
| 断点续跑 | 不需要（失败整体重跑可接受） | 需要（中断后从 `completedIndex` 恢复） |
| 失败重试粒度 | 整作业级 | 记录级（`retryPolicy`/`skipPolicy`） |
| 典型作业 | CSAT 提醒、合同到期提醒、汇率拉取、报价过期扫描 | 折旧批量计提、过账兜底扫描、AR-AP 核销、存货对账、合并报表 |

### 1.3 nop-job-local vs nop-job-service 演进路径（D3）

| 阶段 | 模式 | 适用 | 迁移触发条件（任一满足即考虑迁移） |
|------|------|------|------------------------------------|
| 当前（bootstrap） | `nop-job-local` | 单实例、作业定义可丢失（重启从 `scheduler.yaml` 重建） | — |
| 未来 | `nop-job-service` | 多实例、需持久化与 AMIS 管理、需 misfire 补偿/阻塞策略/超时控制/分片 | ① 多实例部署；② 作业需持久化与 AMIS 管理；③ 需 misfire 补偿与阻塞策略 |

---

## 2. 调度架构（当前实施状态）

```
nop-job-local（已接入 app-erp-all 框架，docs/logs/2026/06-23.md:14-17）
    ├─ JobDefinition（作业定义，由 scheduler.yaml 声明 —— 当前为空，待各域接线）
    │      ├─ jobId / jobName / cronExpression / jobType（JAVA/SCRIPT）
    │      └─ （无 dependencies —— nop-job 不支持 DAG，跨作业依赖见 §4）
    │
    └─ JobExecution（执行记录）
           ├─ executionId / jobId / startTime / endTime
           ├─ status（SUCCESS/FAILED/RUNNING）
           └─ errorMessage
```

**当前状态（2026-07-04）**：

- `nop-job-local` 已作为系统级依赖接入 `app-erp-all/pom.xml:149-154`（`BeanMethodJobInvoker`，本地反射，无 RPC）。
- **无任何 job bean 实现**：全仓零 `IJob` / `IJobInvoker` 实现；无 `scheduler.yaml`；无 `batch.xml`；无 `@Scheduled`。
- 各域 BizModel 入口方法（§3 表中"调用入口"列）已交付，可经 GraphQL / 测试调用，但**未接线到调度器**——cron 实际注册归各域 follow-up（见 §8 汇总，约 20 个计划 Deferred 段）。
- `nop-batch` 完全闲置：全仓仅 `docs/discussions/2026-06-29...:1761` 一处"大批量对账数据分片处理（可选）"提及，无正式设计采用。

---

## 3. 全局作业目录

> 状态（Status）取值：
> - **REGISTERED** = 原文档登记的 5 个标准作业（已纳入本目录）
> - **WIRED** = BizModel 入口方法已交付（可被 GraphQL/测试调用），cron 注册 deferred
> - **DEFERRED** = 在某计划 `Deferred But Adjudicated` 段，触发条件已记录
> - **DESIGN** = 仅设计文档提及，无计划/实现
>
> 数据量级：**小**（< 1万 / 定点查询）/ **中**（数千~数万）/ **大**（全表扫描或 ≥ 数万）。
> 执行模式：**job** = nop-job 直调 BizModel；**batch-candidate** = nop-job 触发 + nop-batch 分 chunk（见 §7 汇总）。

### 3.1 Finance（财务）

| 作业标识 | 业务功能 | 触发频率 | 调用入口 | 量级 | 执行模式 | 状态 | 配置键 | 证据 |
|----------|----------|----------|----------|------|----------|------|--------|------|
| `erp-fin-posting-scan` | 扫描 `posted=false` 已审核单据触发过账（兜底） | 每分钟 | （无独立 BizModel 方法，由过账引擎 `post(PostingEvent)` 承载） | 大 | **batch-candidate** | REGISTERED | `erp-fin.posting-scan-cron`（`0 * * * * ?`） | `job-scheduling.md`(原):30`；`docs/design/finance/posting.md:297-298` |
| `erp-fin-period-close` | 期末结账：AR→AP→INV→AST→GL 模块顺序 + 损益结转 + 试算快照 | 每月最后一天 22:00（次月 1 日） | `ErpFinAccountingPeriodBizModel.closePeriod()` | 大 | **batch-candidate** | REGISTERED（编排已实现） | `erp-fin.period-close-cron`（`0 0 22 L * ?`） | `docs/design/finance/period-close.md:19-21`；`module-finance/erp-fin-service/.../ErpFinAccountingPeriodBizModel.java:45` |
| `erp-fin-cash-forecast-refresh` | 聚合未清 AR/AP + 票据到期 → 现金预测 | 未定（deferred） | `ErpFinCashForecastBizModel.refreshForecast()` | 中 | batch-candidate | DEFERRED | `erp-fin.cash-forecast-cron` | `docs/design/finance/treasury.md:181-182`；`plans/2026-07-02-1000-1:178-182` |
| `erp-fin-credit-facility-interest` | 授信利息自动计提（CREDIT_FACILITY_INTEREST 过账类型） | 未定 | （仅手动触发） | 中 | job | DEFERRED | — | `plans/2026-07-02-1000-1:43` |
| `erp-fin-ar-ap-auto-recon` | 定时自动核销（按比例/账龄/到期日） | 每日凌晨 | `ErpFinAutoReconJob.execute()` → `IErpFinReconciliationBiz.runAutoReconciliation()` | 大 | **batch-candidate** | WIRED | `erp-fin.ar-ap-auto-recon-cron` | `docs/design/finance/ar-ap-reconciliation.md:132`；`module-finance/erp-fin-service/.../job/ErpFinAutoReconJob.java`；`app-erp-all/src/main/resources/_vfs/nop/job/conf/scheduler.yaml` |
| `erp-fin-bank-recon` | 月末银行对账、自动匹配、暂记调整凭证 | 月末 | （待实现） | 中 | job | DESIGN | — | `docs/design/finance/bank-reconciliation.md:23,103,108` |
| `erp-fin-bank-recon-adj-reverse` | 下月初自动红冲上月银行对账调整凭证 | 下月初 | （待实现） | 小 | job | DESIGN | — | `docs/design/finance/bank-reconciliation.md:108` |
| `erp-fin-fund-account-recon` | 资金账户余额 = Σ 总账分录 定期核对 | 定期 | （待实现） | 中 | job | DESIGN | — | `docs/design/finance/bank-reconciliation.md:110` |
| `erp-fin-bad-debt-provision` | 月末坏账准备计提（AGING_BUCKET，分段损失率） | 月末 | （待实现） | 中 | job | DESIGN | `erp-fin.bad-debt-method` 等 | `docs/design/finance/bad-debt.md:225-238` |
| `erp-fin-consolidation` | 合并报表（抵销内部交易/存货利润/投资） | MONTHLY（可配 QUARTERLY/YEARLY） | （待实现） | 大 | **batch-candidate** | DESIGN | `erp-fin.consolidation-schedule` 等 | `docs/design/finance/intercompany-consolidation.md:143-166` |
| `erp-fin-period-auto-open` | 会计期间到开始日自动 OPEN | 按期间开始日 | （待实现） | 小 | job | DESIGN | — | `docs/design/finance/state-machine.md:149` |
| `erp-fin-posting-async-sweep` | 过账引擎异步派发 + 兜底扫描重试 | 未定 | `post(PostingEvent)` | 中 | job | DEFERRED | — | `plans/2026-07-01-0811-1:53,186` |
| `erp-fin-posting-exception-precheck` | 期末结账前置门控：扫 `ErpFinPostingException` 未决项 | （结账内部步骤） | `ErpFinAccountingPeriodProcessor.preCheck()` | 中 | job | WIRED（结账内部） | — | `module-finance/.../ErpFinAccountingPeriodBizModel.java:38`；`docs/design/finance/posting-log.md:168` |
| `erp-fin-changelog-ttl` | `NopSysChangeLog` 过期清理（平台无内建 TTL） | 定期 | （待实现） | 大 | **batch-candidate** | DEFERRED | — | `docs/design/finance/posting-log.md:119`；`plans/2026-07-04-1452-1` |

### 3.2 Assets（资产）

| 作业标识 | 业务功能 | 触发频率 | 调用入口 | 量级 | 执行模式 | 状态 | 配置键 | 证据 |
|----------|----------|----------|----------|------|----------|------|--------|------|
| `erp-ast-depreciation` | 批量折旧计提（直线/双倍余额/工作量，残值底线，红冲+重生幂等，单资产错误隔离） | 每月 1 日 02:00（也由结账 AST 步骤触发） | `ErpAstDepreciationScheduleBizModel.executeBatchDepreciation()` | 大 | **batch-candidate** | REGISTERED（BizModel live） | `erp-ast.depreciation-cron`（`0 0 2 1 * ?`） | `docs/design/assets/depreciation-and-posting.md:206-250`；`module-assets/.../ErpAstDepreciationScheduleBizModel.java:45` |
| `erp-ast-impairment-test` | 定期减值测试（可收回金额 < NBV 则计提减值准备） | 每年末或减值指示触发 | （待实现） | 中 | job | DESIGN | — | `docs/design/assets/depreciation-and-posting.md:190-202` |

### 3.3 Inventory / DRP（库存 / 分销需求计划）

| 作业标识 | 业务功能 | 触发频率 | 调用入口 | 量级 | 执行模式 | 状态 | 配置键 | 证据 |
|----------|----------|----------|----------|------|----------|------|--------|------|
| `erp-inv-stock-check` | 库存余额对账（账实核对） | 每日 03:00 | （待实现） | 大 | **batch-candidate** | REGISTERED | `erp-inv.stock-check-cron`（`0 0 3 * * ?`） | `job-scheduling.md`(原):33`；`docs/design/inventory/README.md` 定时作业 |
| `erp-inv-costing-reclose` | 期末 FIFO 兜底重算（扫本期 DONE 移动，重建缺失成本层，重算 COGS） | （结账 INV 步骤） | `ErpInvCostingBizModel.reclosePeriodCosts()` | 大 | **batch-candidate** | WIRED（结账内部） | `erp-fin.inv-costing-reclose-on-close` | `docs/design/finance/period-close.md:9`；`module-inventory/.../ErpInvCostingBizModel.java:61` |
| `erp-drp-run` | DRP 净需求计算（分销网络） | 未定 | `ErpDrpPlanBizModel.runDrp()` | 中 | job | DEFERRED | `erp-inv.drp-run-schedule` | `docs/design/drp/README.md:99`；`plans/2026-07-04-1115-2:202-205` |
| `erp-drp-ss-recompute` | 安全库存重算（min-max / 周期 / 按需） | 未定 | （待实现） | 中 | job | DEFERRED | `erp-inv.drp-ss-schedule-cron` | `docs/design/drp/safety-stock-optimization.md:200` |

### 3.4 Master-Data（主数据）

| 作业标识 | 业务功能 | 触发频率 | 调用入口 | 量级 | 执行模式 | 状态 | 配置键 | 证据 |
|----------|----------|----------|----------|------|----------|------|--------|------|
| `erp-md-data-sync` | 主数据缓存刷新 | 每小时 | （待实现） | 小 | job | REGISTERED | `erp-md.data-sync-cron`（`0 0 * * * ?`） | `job-scheduling.md`(原):34`；`docs/design/master-data/README.md` 定时作业 |
| `erp-md-exchange-rate-fetch` | 拉取央行/第三方汇率到 `ErpMdExchangeRate` | 每日 | （待实现） | 小 | job | DESIGN | — | `docs/design/master-data/exchange-rate-management.md:51-52` |
| `erp-md-exchange-rate-alert` | 汇率波动超阈值告警 / 缺汇率降级告警 | 随拉取/按需 | （待实现） | 小 | job | DESIGN | `erp-md.exchange-rate-alert-threshold` | `docs/design/master-data/exchange-rate-management.md:34-36` |

### 3.5 Logistics（物流 / 承运商）

| 作业标识 | 业务功能 | 触发频率 | 调用入口 | 量级 | 执行模式 | 状态 | 配置键 | 证据 |
|----------|----------|----------|----------|------|----------|------|--------|------|
| `erp-log-tracking-poll` | 轮询承运商 API，推进 DISPATCHED 未 DELIVERED 运单状态机 | `0 0 */4 * * ?`（每 4 小时） | `IErpLogShipmentBiz.scanForPolling()` | 小-中 | job | WIRED | `erp-log.tracking-poll-cron`（**键名已统一，见 §6**） | `docs/design/logistics/carrier-integration.md:357,458`；`module-logistics/.../ErpLogConfigs.java:15` |
| `erp-log-advice-async-retry` | post-commit `adviseShipment` 5xx 指数退避重试 / 4xx 死信 | 按重试策略 | `ErpLogShipmentBizModel.advise/completeShipment` | 小 | job | DEFERRED | `erp-log.gateway-max-retries` 等 | `docs/design/logistics/carrier-integration.md:457`；`plans/2026-07-04-1115-3:57` |
| `erp-log-shipment-log-archive` | 归档 >180 天 `ErpLogShipmentLog` | 定期 | （待实现） | 中 | batch-candidate | DESIGN | `erp-log.log-retention-days` | `docs/design/logistics/README.md:265` |

### 3.6 Sales（销售）

| 作业标识 | 业务功能 | 触发频率 | 调用入口 | 量级 | 执行模式 | 状态 | 配置键 | 证据 |
|----------|----------|----------|----------|------|----------|------|--------|------|
| `erp-sal-quotation-expiry` | 扫描过期报价单（`validTo` 已过）置 EXPIRED | `0 0 2 * * *`（每日 02:00） | （待实现，当前仅 confirm/convert 派生） | 小 | job | DEFERRED | `erp-sal.quotation-expiry-check-cron` | `docs/design/sales/quotation.md:61`；`plans/2026-07-01-1426-2:191-195` |

### 3.7 Purchase（采购）

| 作业标识 | 业务功能 | 触发频率 | 调用入口 | 量级 | 执行模式 | 状态 | 配置键 | 证据 |
|----------|----------|----------|----------|------|----------|------|--------|------|
| `erp-pur-supplier-scorecard` | 供应商周期评分 + AVL 资格更新 | 周期评估（如月度） | （评分 BizModel 已实现，cron 可选） | 中 | job | WIRED | `erp-pur.scorecard-evaluation-cron` | `docs/design/purchase/supplier-evaluation.md:120`；`plans/2026-07-03-1707-2:53` |

### 3.8 Manufacturing / APS（制造 / 高级排程）

| 作业标识 | 业务功能 | 触发频率 | 调用入口 | 量级 | 执行模式 | 状态 | 配置键 | 证据 |
|----------|----------|----------|----------|------|----------|------|--------|------|
| `erp-mfg-crp-run` | CRP 产能负荷计算（工作中心×日聚合 + 超载告警） | 未定 | `ErpMfgCrpLoadBizModel.calculateLoad()` | 中 | job | DEFERRED | `erp-mfg.crp-run-schedule` | `docs/design/manufacturing/crp.md:82,113`；`plans/2026-07-03-1707-1:173` |
| `erp-aps-auto-dispatch` | 自动派工：扫 PLANNED 工单到派工窗口，检料/人/工具可用性自动开工 | 每分钟 | （待实现） | 小-中 | job | DESIGN | `erp-aps.auto-reschedule-on-insert` 等 | `docs/design/aps/auto-dispatch.md:59,70`；`docs/design/aps/scheduling.md:479-480` |
| `erp-aps-auto-reschedule` | 插单/周期自动重排 | 未定 | （待实现） | 中 | job | DEFERRED | — | `plans/2026-07-04-0831-1:42,204` |

### 3.9 CRM（客户关系管理）

| 作业标识 | 业务功能 | 触发频率 | 调用入口 | 量级 | 执行模式 | 状态 | 配置键 | 证据 |
|----------|----------|----------|----------|------|----------|------|--------|------|
| `erp-crm-event-reminder` | 扫描 PLANNED 活动按 `reminderMinutesBefore` 发提醒 | 默认每小时 | `ErpCrmEventBizModel.findDueReminders()` | 小 | job | WIRED | `erp-crm.event-reminder-cron` | `docs/design/crm/use-cases.md:168`；`plans/2026-07-04-0700-1:155-159` |
| `erp-crm-lead-scoring-recalc` | 每日批量重算线索评分 | `0 2 * * *`（每日 02:00） | `ErpCrmLeadScoreBizModel.recalculateScore()` | 小-中 | job | WIRED | `erp-crm.lead-scoring.schedule-cron` | `docs/design/crm/lead-scoring.md:157` |
| `erp-crm-forecast-recalc` | 每日重算销售预测 | `0 3 * * *`（每日 03:00） | `ErpCrmForecastBizModel.refreshForecast()` | 中 | job | WIRED | `erp-crm.forecast.recalc-cron` | `docs/design/crm/sales-forecast.md:165` |
| `erp-crm-funnel-aggregation` | 漏斗阶段聚合 rollup | `0 0 3 * * ?`（每日 03:00） | （待实现） | 中 | batch-candidate | DESIGN | `erp-crm.funnel.aggregation-cron` | `docs/design/crm/lead-waterfall.md:191` |
| `erp-crm-sequence-step-reminder` | 销售序列步骤到期提醒 + 逾期检查 | 未定 | （待实现） | 小 | job | DESIGN | — | `docs/design/crm/sales-sequence.md:205` |

### 3.10 Customer Service（客户服务）

| 作业标识 | 业务功能 | 触发频率 | 调用入口 | 量级 | 执行模式 | 状态 | 配置键 | 证据 |
|----------|----------|----------|----------|------|----------|------|--------|------|
| `erp-cs-sla-scan` | 扫描 `adjustedDeadline < now` 工单，建 ESCALATE 动作 + 通知升级人 | 每分钟 | `scanOverdueTickets()` | 小 | job | WIRED | `erp-cs.sla-scan-interval` | `docs/design/customer-service/sla.md:281`；`plans/2026-07-04-0700-2:172-176` |
| `erp-cs-sla-warning` | 截止前 1h/30min 向经办人发预警 | 随扫描 | `findSlaWarnings()` | 小 | job | WIRED | `erp-cs.sla-warning-before` | `docs/design/customer-service/sla.md:282` |
| `erp-cs-csat-reminder` | CSAT 调查提醒 + 过期标记 | 定期 | `findSurveyReminders()`/`findExpiredSurveys()` | 小 | job | WIRED | `erp-cs.survey-reminder-hours` 等 | `docs/design/customer-service/csat.md:188,225` |
| `erp-cs-csat-delayed-send` | `survey-send-delay>0` 时延迟发送调查 | 按延迟 | （待实现） | 小 | job | DESIGN | `erp-cs.survey-send-delay` | `docs/design/customer-service/csat.md:220` |
| `erp-cs-entitlement-expiry` | 每日扫描 30/60/90 天到期权益，到期自动停用 | 每日 | （待实现） | 小 | job | DESIGN | `erp-cs.entitlement-expiry-warning-days` | `docs/design/customer-service/entitlement.md:87,195` |

### 3.11 B2B / MFT（EDI / 托管文件传输）

| 作业标识 | 业务功能 | 触发频率 | 调用入口 | 量级 | 执行模式 | 状态 | 配置键 | 证据 |
|----------|----------|----------|----------|------|----------|------|--------|------|
| `erp-b2b-async-send-poll` | `needsWebService=true` 格式的异步发送队列轮询 | 未定 | （待实现） | 中 | job | DESIGN | `erp-b2b.async-send-cron` | `docs/architecture/b2b-integration.md:226`；`docs/design/b2b/README.md:139` |
| `erp-b2b-sftp-inbound-poll` | SFTP 入站轮询：下载 → 反向管道（解密/验签/解压）→ EDI 解析 | 部署期声明 cron | （callable + cron deferred） | 大 | **batch-candidate** | DEFERRED | — | `docs/design/b2b/managed-file-transfer.md:221-244`；`plans/2026-07-04-2200-1:206-209` |
| `erp-b2b-asn-auto-retry-match` | ASN 未匹配 PO 自动重试 `matchPurchaseOrder`（48h 升级 / 24h 重复通知） | 定期 | `retryMatch()` / `findUnmatchedAsns()` | 中 | job | DEFERRED | `erp-b2b.asn.match-timeout-hours` | `docs/design/b2b/asn-processing.md:260,454`；`plans/2026-07-04-2200-1:150` |
| `erp-b2b-mft-cert-expiry` | 证书过期检查，30 天前告警，到期自动停用 | 定期 | `ErpB2bMftCertificateBizModel.findExpiringCertificates()` | 小 | job | DEFERRED | — | `docs/design/b2b/managed-file-transfer.md:278-301`；`plans/2026-07-04-2200-1:126` |
| `erp-b2b-credential-rotation` | 合作伙伴凭证轮换提醒/执行 | 周期（默认 90 天） | （待实现） | 小 | job | DESIGN | `erp-b2b.onboarding-credential-rotation-days` | `docs/design/b2b/partner-onboarding.md:286` |

### 3.12 Quality（质量）

| 作业标识 | 业务功能 | 触发频率 | 调用入口 | 量级 | 执行模式 | 状态 | 配置键 | 证据 |
|----------|----------|----------|----------|------|----------|------|--------|------|
| `erp-qa-spc-sample-aggregation` | 按 `samplingFrequency` 扫 APPROVED 检验行聚合到 `ErpQaSpcSample` | 按 `samplingFrequency` | （待实现） | 中-大 | **batch-candidate** | DESIGN | — | `docs/design/quality/spc.md:28,79,93` |
| `erp-qa-spc-capability-analysis` | 周期 Cpk 计算，< ACCEPTABLE 触发质量目标回写 + 风险登记 | 月度/周度 | （待实现） | 小-中 | job | DESIGN | — | `docs/design/quality/spc.md:85,93` |

### 3.13 Maintenance（设备维护）

| 作业标识 | 业务功能 | 触发频率 | 调用入口 | 量级 | 执行模式 | 状态 | 配置键 | 证据 |
|----------|----------|----------|----------|------|----------|------|--------|------|
| `erp-mnt-due-visit-generation` | 扫描 `nextDueDate ≤ asOfDate` 维护计划生成 DRAFT 访问 + 推进 `nextDueDate` | 定期 | `IErpMntScheduleBiz.generateDueVisits()` | 小-中 | job | WIRED | `erp-mnt.auto-generate-due-visits` | `docs/design/maintenance/use-cases.md:16-23`；`plans/2026-07-03-1018-3:30` |
| `erp-mnt-usage-based-trigger` | 基于累计运行时长/产量的维护触发 | 按累计 | （待实现） | 中 | job | DESIGN | — | `docs/design/maintenance/equipment-integration.md:196-215` |

### 3.14 Projects（项目）

| 作业标识 | 业务功能 | 触发频率 | 调用入口 | 量级 | 执行模式 | 状态 | 配置键 | 证据 |
|----------|----------|----------|----------|------|----------|------|--------|------|
| `erp-prj-pnl-aggregation` | 项目损益汇总（开票 + 成本归集 → `ErpPrjProjectPnl`，多币种 rollup） | 按月/里程碑 | （待实现） | 中-大 | **batch-candidate** | DESIGN | — | `docs/design/projects/profitability.md:82,95` |

### 3.15 Human-Resource（人力资源）

| 作业标识 | 业务功能 | 触发频率 | 调用入口 | 量级 | 执行模式 | 状态 | 配置键 | 证据 |
|----------|----------|----------|----------|------|----------|------|--------|------|
| `erp-hr-payroll-calc` | 月度薪酬核算（累计个税 + 社保公积金） | 每月（默认关） | `IErpHrSalaryBiz.runPayroll()` | 中 | job | DEFERRED | `erp-hr.auto-generate-salary` | `docs/design/human-resource/payroll.md:21,275-285`；`plans/2026-07-04-0831-2:181` |
| `erp-hr-payroll-sim-convert` | 模拟薪酬自动转正式 + 过期清理 | 定期 | `convertToFormal()` | 中 | job | DEFERRED | — | `plans/2026-07-04-2200-3:184-187` |
| `erp-hr-shift-auto-generate` | 排班自动生成 + 提醒 | 未定 | （排班 BizModel callable） | 中 | job | DEFERRED | — | `plans/2026-07-04-0831-3:182,207` |
| `erp-hr-contract-expiry-reminder` | 每日扫描到期劳动合同提醒续签/终止 | 每日 | （待实现） | 小 | job | DESIGN | — | `docs/design/human-resource/use-cases.md:75-85` |
| `erp-hr-survey-competency-reminder` | 员工调查/能力评估周期提醒 | 季/半年/年 | （待实现） | 小 | job | DESIGN | `reminderDays` | `docs/design/human-resource/employee-survey.md:38,104` |

### 3.16 Contract（合同）

| 作业标识 | 业务功能 | 触发频率 | 调用入口 | 量级 | 执行模式 | 状态 | 配置键 | 证据 |
|----------|----------|----------|----------|------|----------|------|--------|------|
| `erp-ct-expiry-reminder` | 每日扫 ACTIVE 合同 30/15/7 天分级到期提醒（可选自动建续签草稿） | 每日 | （待实现） | 小 | job | DESIGN | `erp-ct.reminder-days-before-expiry` 等 | `docs/design/contract/use-cases.md:89-107`；`docs/design/contract/README.md:123-128` |
| `erp-ct-signature-status-poll` | 主动轮询在途电子签署请求状态 | `0 0 */2 * * ?`（每 2 小时） | `queryAndUpdateStatus()` / `findExpiringRequests()` | 小 | job | DEFERRED | `erp-ct.signature-status-polling-cron` | `plans/2026-07-04-2200-2:51,147-150` |
| `erp-ct-usage-billing-rebate` | 用量计费 / 返利重估 / 多币种重估 | 定期 | （待实现） | 中 | job | DEFERRED | — | `plans/2026-07-04-1115-1:204` |

**目录汇总**：本节登记 **52 个**作业（远超 ≥40 目标），覆盖 16 个域。

---

## 4. 作业逻辑前置关系（替代原"DAG 依赖"章节）

> **修正记录（D2）**：原文档 §"DAG 依赖"（`:36-45`）把 DAG 算作 nop-job 能力——这与平台契约冲突（nop-job 通读 396 行无任何 DAG/graph 章节）。已删除该误述，改为下述"逻辑前置关系"。

### 4.1 期末结账模块顺序 = BizModel 内部编排，非跨作业 DAG

期末结账（`erp-fin-period-close`）的 AR→AP→INV→AST→GL 模块顺序**由 `ErpFinAccountingPeriodBizModel.closePeriod()` 内部顺序编排承载**（计划 `2026-07-02-1000-3` Phase 1 已实现），是**单个作业内部的步骤顺序**，不需要跨作业 DAG：

```
erp-fin-period-close（单个作业）
    内部步骤顺序（closePeriod 内部编排，非 nop-job DAG）：
        ├─ preCheck（扫 ErpFinPostingException 未决项）
        ├─ closeArModule
        ├─ closeApModule
        ├─ closeInvModule（内部调 erp-inv-costing-reclose）
        ├─ closeAstModule（内部调 erp-ast-depreciation）
        └─ closeGlModule（损益结转 + 试算快照）
```

### 4.2 真正的跨作业 DAG → nop-task `<graph>`

若未来出现真正的**跨作业**依赖链（如：归档 → 对账 → 合并报表 三者须顺序串接且各自是独立定时作业），应引入 **nop-task `<graph>`**（数据驱动 DAG），而非 nop-job。当前**无此类需求**，仅登记演进路径。

---

## 5. 告警

| 失败类型 | 告警方式 |
|----------|----------|
| 作业执行失败 | 站内消息 + 邮件 |
| 作业超时 | 站内消息（仅 nop-job-service 模式支持超时控制，见 §1.3） |
| 大数据量作业单 chunk 失败超阈值 | 站内消息 + 邮件（nop-batch `skipPolicy` 跳过超阈则任务失败） |

---

## 6. 配置键总表（全仓 cron / 调度相关）

> 本表是全仓 cron/调度配置键的**唯一对账表**，供配置键一致性审计（`docs/audits/2026-07-04-0000-batch-scheduling-audit.md` 维度 4）基准。

| 配置键 | 默认值 | 域 | 证据 |
|--------|--------|----|----|
| `erp-fin.posting-scan-cron` | `0 * * * * ?`（每分钟） | finance | 本目录 §3.1（标准作业 cron 键，回流 `docs/design/finance/posting.md`） |
| `erp-fin.period-close-cron` | `0 0 22 L * ?`（每月最后一天 22:00） | finance | 本目录 §3.1；`docs/design/finance/period-close.md` 配置项表 |
| `erp-fin.cash-forecast-cron` | —（deferred） | finance | 本目录 §3.1；`docs/design/finance/treasury.md` 配置点表 |
| `erp-fin.ar-ap-auto-recon-cron` | —（deferred） | finance | 本目录 §3.1；`docs/design/finance/ar-ap-reconciliation.md` 配置项表 |
| `erp-ast.depreciation-cron` | `0 0 2 1 * ?`（每月 1 日 02:00） | assets | 本目录 §3.2；`docs/design/assets/depreciation-and-posting.md` §五 |
| `erp-inv.stock-check-cron` | `0 0 3 * * ?`（每日 03:00） | inventory | 本目录 §3.3；`docs/design/inventory/README.md` 定时作业 |
| `erp-md.data-sync-cron` | `0 0 * * * ?`（每小时） | master-data | 本目录 §3.4；`docs/design/master-data/README.md` 定时作业 |
| `erp-log.tracking-poll-cron` | `0 0 */4 * * ?` | logistics | `docs/design/logistics/carrier-integration.md:357,458`；Java `ErpLogConfigs.java:15`（**已统一**，原计划侧 `tracking-polling-cron` 拼写已修正） |
| `erp-log.tracking-poll-max-days` | 30 | logistics | `docs/design/logistics/carrier-integration.md:358,459` |
| `erp-log.gateway-timeout-secs` | 30 | logistics | `plans/2026-07-04-1115-3:57`；`ErpLogConfigs.java:9` |
| `erp-log.gateway-max-retries` | 3 | logistics | `plans/2026-07-04-1115-3:57`；`ErpLogConfigs.java:11` |
| `erp-log.retry-base-interval-secs` | `30,120,600` | logistics | `docs/design/logistics/carrier-integration.md:457`；`ErpLogConfigs.java:13` |
| `erp-log.shipment-settlement-mode` | AUTO | logistics | `plans/2026-07-04-1115-3:57`；`ErpLogConfigs.java:17` |
| `erp-log.webhook-signature-required` | true | logistics | `plans/2026-07-04-1115-3:57`；`ErpLogConfigs.java:19` |
| `erp-sal.quotation-expiry-check-cron` | `0 0 2 * * *` | sales | `docs/design/sales/quotation.md:61` |
| `erp-pur.scorecard-evaluation-cron` | — | purchase | `docs/design/purchase/supplier-evaluation.md:120` |
| `erp-mfg.crp-run-schedule` | — | manufacturing | `docs/design/manufacturing/crp.md:82,113` |
| `erp-inv.drp-run-schedule` | — | drp | `docs/design/drp/README.md:99` |
| `erp-inv.drp-ss-schedule-cron` | — | drp | `docs/design/drp/safety-stock-optimization.md:200` |
| `erp-crm.event-reminder-cron` | —（默认每小时） | crm | `docs/design/crm/use-cases.md:168`；`docs/design/crm/README.md:253` |
| `erp-crm.lead-scoring.schedule-cron` | `0 2 * * *` | crm | `docs/design/crm/lead-scoring.md:157` |
| `erp-crm.forecast.recalc-cron` | `0 3 * * *` | crm | `docs/design/crm/sales-forecast.md:165` |
| `erp-crm.funnel.aggregation-cron` | `0 0 3 * * ?` | crm | `docs/design/crm/lead-waterfall.md:191` |
| `erp-cs.sla-scan-interval` | 1（分钟） | cs | `docs/design/customer-service/sla.md:281` |
| `erp-cs.survey-reminder-hours` | 48 | cs | `docs/design/customer-service/csat.md:225` |
| `erp-b2b.async-send-cron` | — | b2b | `docs/architecture/b2b-integration.md:226` |
| `erp-ct.signature-status-polling-cron` | `0 0 */2 * * ?` | contract | `plans/2026-07-04-2200-2:51` |
| `erp-fin.consolidation-schedule` | MONTHLY | finance | `docs/design/finance/intercompany-consolidation.md:150` |
| `erp-fin.inv-costing-reclose-on-close` | true | finance | `docs/design/finance/period-close.md:9` |
| `erp-hr.auto-generate-salary` | false | hr | `docs/design/human-resource/README.md:220` |
| `erp-mnt.auto-generate-due-visits` | true | maintenance | `plans/2026-07-03-1018-3:30` |
| `erp-aps.auto-reschedule-on-insert` | true | aps | `docs/design/aps/README.md:98` |

> **键名一致性裁决（D）**：承运商轮询 cron 统一为 `erp-log.tracking-poll-cron`（与设计 `carrier-integration.md` + Java `ErpLogConfigs.java` 一致）。原计划侧误拼 `erp-log.tracking-polling-cron` 已修正（见 Phase 2）。

---

## 7. nop-batch 候选作业汇总（大数据量迁移触发条件）

> 以下 **11 个**作业标注 `executionModel=batch-candidate`。本计划仅**登记判据与候选**，不强制接线；迁移触发条件满足时归各域 follow-up（首个候选正式接线时引入 `nop-batch` 模块依赖到 `app-erp-all`）。

| 作业 | 量级证据 | 为何需 nop-batch（四维裁决） | 迁移触发条件 |
|------|----------|------------------------------|--------------|
| `erp-fin-posting-scan` | 全表扫 `posted=false`（`posting.md:297`） | 全表扫描 + 需断点续跑 + 记录级幂等 | 过账数据量增长导致单节点扫描超时 |
| `erp-fin-period-close` | 大（多模块批量） | 每 Chunk 独立事务避免长事务锁表 | 单次结账耗时超窗 |
| `erp-fin-ar-ap-auto-recon` | 大（全量未清 AR/AP 匹配） | 记录级重试（单条匹配失败不阻断） | 核销数据量 ≥ 数万 |
| `erp-fin-cash-forecast-refresh` | 中（聚合未清项） | 断点续跑 | 预测范围扩大到多公司 |
| `erp-fin-consolidation` | 大（多公司全量抵销） | 全表 + 断点续跑 | 多公司合并上线 |
| `erp-fin-changelog-ttl` | 大（清理过期 `NopSysChangeLog`） | 全表删除 + 记录级 | 日志量增长致单次清理超窗 |
| `erp-inv-costing-reclose` | 大（扫本期所有 FIFO 移动） | 全表 + 独立事务 | 单期移动量 ≥ 数十万 |
| `erp-ast-depreciation` | 大（批量计提，平台 docs-for-ai 明示适用） | 记录级错误隔离 + 断点续跑 | 资产量 ≥ 数千 |
| `erp-inv-stock-check` | 大（全库存核对） | 记录级重试 | 库存 SKU ≥ 数万 |
| `erp-qa-spc-sample-aggregation` | 中-大（扫所有 APPROVED 检验行） | 分组聚合 + 断点续跑 | 检验量 ≥ 数万/期 |
| `erp-b2b-sftp-inbound-poll` | 大（批量文件下载+反向管道） | 文件级断点 + 记录级重试 | 文件量 ≥ 数千/批 |
| `erp-prj-pnl-aggregation` | 中-大（多项目多币种 rollup） | 项目级断点 | 项目数 ≥ 数百 |

---

## 8. 实施状态与接线 follow-up

**当前（bootstrap）**：`nop-job-local` 框架已接入，**零作业注册**。各域 BizModel 入口已交付（§3"调用入口"列），cron 实际注册归各域 follow-up。

**Deferred 接线汇总（约 20 个计划 Deferred 段，对应 §3 的 DEFERRED 状态作业）**：

| 计划文件 | deferred 作业 | 触发条件 |
|----------|---------------|----------|
| `2026-07-01-0811-1-finance-posting-engine-foundation.md` | 过账异步派发 + 兜底扫描 | 运营基础设施接线 |
| `2026-07-01-1426-2-sales-quotation-to-order-and-order-approval.md` | 报价过期扫描 | 接线 nop-job 时 |
| `2026-07-02-0300-3-ar-ap-settlement-subledger.md` | AR/AP 自动核销 | 接线 nop-job 实施自动核销时 |
| `2026-07-02-1000-1-finance-treasury-notes.md` | 现金预测刷新 / 授信利息计提 | nop-job 接线时 |
| `2026-07-02-1000-2-assets-depreciation-disposal-capitalization.md` | 自动折旧 cron | nop-job wiring |
| `2026-07-03-1018-3-maintenance-visit-request-sparepart-downtime.md` | 到期访问生成 | 可选部署配置 |
| `2026-07-03-1707-1-manufacturing-crp-load-engine.md` | CRP 定时运行 | calculateLoad 入口已可复用 |
| `2026-07-03-1707-2-supplier-scorecard-avl.md` | 供应商评分 | 可选部署配置 |
| `2026-07-04-0700-1-crm-event-reminder-lead-scoring-forecast.md` | 事件提醒/线索评分/预测重算 | 生产定时调度需求 |
| `2026-07-04-0700-2-cs-ticket-sla-csat.md` | SLA 扫描/预警/CSAT 提醒 | 生产定时调度需求 |
| `2026-07-04-0831-1-aps-operation-order-scheduling-engine.md` | 周期自动重排 | follow-up |
| `2026-07-04-0831-2-hr-payroll-engine-income-tax.md` | 薪酬自动核算 | follow-up |
| `2026-07-04-0831-3-hr-shift-scheduling.md` | 排班自动生成/提醒 | follow-up |
| `2026-07-04-1115-1-contract-version-invoiceplan-volume-discount-rebate.md` | 合同提醒/用量计费/返利重估 | Non-Goal |
| `2026-07-04-1115-2-drp-net-requirement-safety-stock.md` | DRP 运行 / SS 重算 | 生产部署定时调度 |
| `2026-07-04-1115-3-logistics-carrier-gateway-spi-freight-posting.md` | 承运商轮询 / 异步重试 | 生产部署 |
| `2026-07-04-1452-1-finance-posting-log-observability.md` | 日志 TTL 清理 | 见 Deferred |
| `2026-07-04-2200-1-b2b-edi-format-spi-asn-inbound-mft.md` | SFTP 轮询/ASN 匹配/证书过期 | 生产部署 |
| `2026-07-04-2200-2-contract-e-signature-spi.md` | 签署状态轮询 | 生产部署 |
| `2026-07-04-2200-3-hr-payroll-simulation.md` | 模拟转正式/过期清理 | 生产部署 |

**接线规范**：各域接线时，在本目录对应作业行的"调用入口"已就绪的前提下，新增 `scheduler.yaml` 条目（声明 cron + 调用 bean 方法）；大数据量作业改用 `<batch:Execute>` 调 nop-batch（按 §7 迁移触发条件）。

---

## 参考

- 平台契约：`nop-entropy/docs-for-ai/03-modules/nop-job.md`、`nop-batch.md`、`nop-task.md`、`reusable-modules-overview.md`
- 阈值旁证：`nop-entropy/docs-for-ai/04-reference/non-bizmodel-orm-access.md:134-186`
- 专项审计：`docs/audits/2026-07-04-0000-batch-scheduling-audit.md`
- 推动计划：`docs/plans/2026-07-04-1600-1-batch-scheduling-architecture.md`
- 既有合规审计（不覆盖批处理维度）：`docs/audits/2026-07-02-0000-best-practices-compliance-audit.md`
- 各域 owner doc 的"配置点"表（见 §6 配置键证据列）
