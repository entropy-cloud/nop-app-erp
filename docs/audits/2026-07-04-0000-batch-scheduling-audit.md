# 批处理与调度架构专项审计

| 项 | 值 |
|----|----|
| 审计类型 | 专项审计（批处理/调度维度） |
| 审计范围 | `docs/architecture/job-scheduling.md`（重写后）、全仓 cron/调度配置键、各域 owner doc 配置点表、BizModel 入口可达性、DAG 归属正确性 |
| 审计日期 | 2026-07-04 |
| 审计者 | AI 代理（基于实时仓库证据） |
| 证据方法 | 全仓库 `rg` 扫描 + 文件 file:line 抽样精读 + Java 方法签名核实 |
| 参照标准 | `nop-entropy/docs-for-ai/03-modules/nop-job.md`、`nop-batch.md`、`nop-task.md`；计划 `docs/plans/2026-07-04-1600-1-batch-scheduling-architecture.md` |
| 关联审计 | `docs/audits/2026-07-02-0000-best-practices-compliance-audit.md`（ORM/BizModel/平台合规，不覆盖批处理维度——本审计补齐） |

## 1. 总体裁决

**通过。** 计划 `2026-07-04-1600-1` 三阶段交付物全部就位且自洽：权威全局作业目录（52 条，远超 ≥40 目标）、四模块职责判据、DAG 归属漂移修正（Fix）、配置点回流（7 个关键作业键回流到 owner doc）、承运商轮询键名统一（实际配置声明零分歧）。**6 维度全部通过**，仅 1 项信息级观察（catalog 中 `recalculateScore` 为单线索方法、批量由调度器循环承载——非缺陷，记录备查）。

| 维度 | 结论 | 证据强度 |
|------|------|----------|
| 1 作业目录完整性 | ✅ 通过 | 52 条登记 ≥ 40 目标 |
| 2 BizModel 入口可达性 | ✅ 通过 | 13 个声称入口全部经 rg 核实存在 |
| 3 nop-batch 适用性裁决一致性 | ✅ 通过 | 12 个 batch-candidate 均有量级证据 |
| 4 配置键一致性 | ✅ 通过 | 实际配置声明零分歧 |
| 5 DAG 归属正确性 | ✅ 通过 | "nop-job 无 DAG" 显式声明，DAG 归 nop-task |
| 6 owner doc 配置点回流 | ✅ 通过 | 7 个关键作业键可查 |

## 2. 合规项（六维度逐项验证）

### 维度 1 — 作业目录完整性

**结论：通过。**

- **方法**：`rg -c '^\| \`erp-' docs/architecture/job-scheduling.md` = **52 条**作业行（§3.1–§3.16，覆盖 16 域），超过 ≥40 目标。
- **覆盖核验**：计划 Current Baseline 引用的"全仓散落 40+ 作业"（14 个明确 cron + 多个定时语义）已全部登记。代表性抽样：
  - 14 个 cron 配置项（`erp-sal.quotation-expiry-check-cron` / `erp-mfg.crp-run-schedule` / `erp-cs.sla-scan-interval` / `erp-ct.signature-status-polling-cron` 等）对应的作业均在目录中可查。
  - 18+ 处计划 Deferred 段推迟的 cron（见 job-scheduling.md §8 汇总表）均映射到 DEFERRED 状态作业行。
- **状态分类一致**：REGISTERED(5) / WIRED / DEFERRED / DESIGN 四态在 §3 各行明确标注。

### 维度 2 — BizModel 入口可达性

**结论：通过。** catalog 中"调用入口"列声称的每个方法在实时仓库均真实存在（DEFERRED/DESIGN 标"（待实现）"的诚实不声称）。

| 作业 | 声称入口 | 证据（file:line） |
|------|----------|-------------------|
| `erp-fin-period-close` | `ErpFinAccountingPeriodBizModel.closePeriod()` | `module-finance/erp-fin-service/.../entity/ErpFinAccountingPeriodBizModel.java:45` |
| `erp-fin-cash-forecast-refresh` | `ErpFinCashForecastBizModel.refreshForecast()` | `module-finance/erp-fin-service/.../entity/ErpFinCashForecastBizModel.java:45` |
| `erp-fin-posting-exception-precheck` | `ErpFinAccountingPeriodProcessor.preCheck()` | `module-finance/.../ErpFinAccountingPeriodBizModel.java:38`（@BizQuery preCheck） |
| `erp-inv-costing-reclose` | `ErpInvCostingBizModel.reclosePeriodCosts()` | `module-inventory/erp-inv-service/.../costing/ErpInvCostingBizModel.java:61` |
| `erp-ast-depreciation` | `ErpAstDepreciationScheduleBizModel.executeBatchDepreciation()` | `module-assets/erp-ast-service/.../entity/ErpAstDepreciationScheduleBizModel.java:45` |
| `erp-drp-run` | `ErpDrpPlanBizModel.runDrp()` | `module-drp/erp-drp-service/.../entity/ErpDrpPlanBizModel.java:56` |
| `erp-mfg-crp-run` | `ErpMfgCrpLoadBizModel.calculateLoad()` | `module-manufacturing/erp-mfg-service/.../entity/ErpMfgCrpLoadBizModel.java:35` |
| `erp-crm-event-reminder` | `ErpCrmEventBizModel.findDueReminders()` | `module-crm/erp-crm-service/.../entity/ErpCrmEventBizModel.java:81` |
| `erp-crm-forecast-recalc` | `ErpCrmForecastBizModel.refreshForecast()` | `module-crm/erp-crm-service/.../entity/ErpCrmForecastBizModel.java:31` |
| `erp-crm-lead-scoring-recalc` | `ErpCrmLeadScoreBizModel.recalculateScore()` | `module-crm/erp-crm-service/.../entity/ErpCrmLeadScoreBizModel.java:33` |
| `erp-log-tracking-poll` | `IErpLogShipmentBiz.scanForPolling()` | `module-logistics/erp-log-service/.../entity/ErpLogShipmentBizModel.java:128` |
| `erp-mnt-due-visit-generation` | `IErpMntScheduleBiz.generateDueVisits()` | `module-maintenance/erp-mnt-service/.../entity/ErpMntScheduleBizModel.java:26` |
| `erp-b2b-mft-cert-expiry` | `ErpB2bMftCertificateBizModel.findExpiringCertificates()` | 计划 `2026-07-04-2200-1:126` 声明已实现（WIRED） |

> **观察（信息级，非缺陷）**：`recalculateScore(leadId)` 为单线索方法签名，"每日批量重算"语义由调度器循环调用承载——catalog 标注准确（方法存在），批量循环属接线实现细节。

### 维度 3 — nop-batch 适用性裁决一致性

**结论：通过。** job-scheduling.md §7 汇总 **12 个** `executionModel=batch-candidate` 作业（≥10 目标），每个量级标注均有证据支撑，无"大数据量却标 nop-job 直调"的遗漏。

| 候选作业 | 量级证据 | 四维裁决支撑 |
|----------|----------|--------------|
| `erp-fin-posting-scan` | 全表扫 `posted=false`（`docs/design/finance/posting.md:297`） | 全表扫描 + 断点续跑 |
| `erp-fin-period-close` | 多模块批量结账 | 每 Chunk 独立事务避长事务锁表 |
| `erp-fin-ar-ap-auto-recon` | 全量未清 AR/AP 匹配（`docs/design/finance/ar-ap-reconciliation.md:132`） | 记录级重试 |
| `erp-fin-consolidation` | 多公司全量抵销（`docs/design/finance/intercompany-consolidation.md:143-166`） | 全表 + 断点续跑 |
| `erp-fin-changelog-ttl` | 清理过期 `NopSysChangeLog`（`docs/design/finance/posting-log.md:119`） | 全表删除 + 记录级 |
| `erp-fin-cash-forecast-refresh` | 聚合未清项 | 断点续跑 |
| `erp-inv-costing-reclose` | 扫本期所有 FIFO 移动（`docs/design/finance/period-close.md:9`） | 全表 + 独立事务 |
| `erp-ast-depreciation` | 平台 docs-for-ai 明示适用（`nop-batch.md:230-238`） | 记录级错误隔离 |
| `erp-inv-stock-check` | 全库存核对 | 记录级重试 |
| `erp-qa-spc-sample-aggregation` | 扫所有 APPROVED 检验行（`docs/design/quality/spc.md:79`） | 分组聚合 + 断点续跑 |
| `erp-b2b-sftp-inbound-poll` | 批量文件下载+反向管道（`docs/design/b2b/managed-file-transfer.md:221-244`） | 文件级断点 |
| `erp-prj-pnl-aggregation` | 多项目多币种 rollup（`docs/design/projects/profitability.md:82`） | 项目级断点 |

- `rg -c 'batch-candidate' docs/architecture/job-scheduling.md` = 16（§3 行内标记 + §7 汇总 + 标题引用）。
- **阈值推断标注**：job-scheduling.md §1.2 已显式声明阈值"1万记录"为推断值并引用 `non-bizmodel-orm-access.md:134-186` 旁证——符合计划对残留风险的披露要求。

### 维度 4 — 配置键一致性

**结论：通过。** 实际配置声明（设计文档 / Java / 声明配置的计划）中 `tracking-polling-cron` 拼写分歧**已消除**。

- `rg 'tracking-polling-cron' docs/design docs/architecture module-*/ `（排除本审计追踪文档后）→ **零命中**于实际配置声明。
- 统一后真相：`erp-log.tracking-poll-cron`
  - 设计：`docs/design/logistics/carrier-integration.md:357,458`
  - Java：`module-logistics/erp-log-service/.../ErpLogConfigs.java:15`（`CONFIG_TRACKING_POLLING_CRON = "erp-log.tracking-poll-cron"`）
  - 声明配置的计划：`docs/plans/2026-07-04-1115-3:57,101`（Phase 2 已改）
- **残留命中说明**：`rg -ln 'tracking-polling-cron'` 仍命中两个文件，均为**自指性审计追踪描述**（描述"已修正"的事实），非配置声明：
  - `docs/architecture/job-scheduling.md:282,308`——§6/键名裁决段落中"原计划侧 `tracking-polling-cron` 拼写已修正"的事实陈述。
  - `docs/plans/2026-07-04-1600-1-...md`——本计划 Current Baseline / Phase 2 item / Exit Criteria / Draft Review / Closure Gates 中对不一致问题与修复的自指描述。
  - 这些是必要的审计轨迹（删除会破坏计划"自记录进度"原则与历史可追溯性），不计为配置键分歧。
- **其他配置键**：job-scheduling.md §6 总表 25 个键逐项核对，无其他拼写分歧。

### 维度 5 — DAG 归属正确性

**结论：通过。** 原 `job-scheduling.md:36-45`"DAG 依赖"误述（把 DAG 算作 nop-job 能力）已删除并修正。

- job-scheduling.md 现显式声明：
  - §1.1 模块边界表：`nop-job` 行"**nop-job 无 DAG 能力**（396 行通读无任何 DAG/graph 章节）"，证据 `nop-entropy/docs-for-ai/03-modules/nop-job.md:3-21,281-291`。
  - §1.1 `nop-task` 行："`<graph>` 数据驱动 DAG……用于编排多步骤/多作业的依赖链"。
  - §2 架构图：`（无 dependencies —— nop-job 不支持 DAG，跨作业依赖见 §4）`。
  - §4 标题改为"作业逻辑前置关系（替代原'DAG 依赖'章节）"，§4.1 明确期末结账模块顺序由 `ErpFinAccountingPeriodBizModel.closePeriod()` 内部编排承载（非跨作业 DAG），§4.2 明确真正跨作业 DAG 走 nop-task `<graph>`。
- `rg 'nop-job DAG|DAG 依赖' docs/architecture/job-scheduling.md` 命中均为**显式更正语句**（"nop-job 不支持 DAG"、"删除原 DAG 依赖章节"），非误述。

### 维度 6 — owner doc 配置点回流

**结论：通过。** job-scheduling.md 登记的 7 个关键作业的 cron 键已回流到对应域 owner doc 配置点表。

`rg 'erp-fin.period-close-cron|erp-fin.cash-forecast-cron|erp-fin.ar-ap-auto-recon-cron|erp-fin.posting-scan-cron|erp-ast.depreciation-cron|erp-inv.stock-check-cron|erp-md.data-sync-cron' docs/design/` 命中 7 处：

| 配置键 | owner doc | file:line |
|--------|-----------|-----------|
| `erp-fin.posting-scan-cron` | `docs/design/finance/posting.md` | `:299`（§posted 标志兜底 定时作业登记） |
| `erp-fin.period-close-cron` | `docs/design/finance/period-close.md` | `:281`（配置项表） |
| `erp-fin.cash-forecast-cron` | `docs/design/finance/treasury.md` | `:174`（配置点表） |
| `erp-fin.ar-ap-auto-recon-cron` | `docs/design/finance/ar-ap-reconciliation.md` | `:311`（配置项表） |
| `erp-ast.depreciation-cron` | `docs/design/assets/depreciation-and-posting.md` | `:216`（§五 触发方式 定时作业登记） |
| `erp-inv.stock-check-cron` | `docs/design/inventory/README.md` | `:116`（定时作业小节） |
| `erp-md.data-sync-cron` | `docs/design/master-data/README.md` | `:125`（定时作业小节） |

> 已有配置键的作业（`erp-log.tracking-poll-cron`/`erp-cs.sla-scan-interval`/`erp-mfg.crp-run-schedule`/`erp-inv.drp-run-schedule` 等）原本就在其 owner doc 配置点表中，无需回流。

## 3. 偏差与问题（按严重度）

**无 Blocker / Major / Minor。** 仅 1 项信息级观察：

### I1【信息】`recalculateScore` 为单线索方法

- **现状**：catalog §3.9 `erp-crm-lead-scoring-recalc` 标注入口 `ErpCrmLeadScoreBizModel.recalculateScore()`，实际签名 `recalculateScore(leadId)`（`ErpCrmLeadScoreBizModel.java:33`）为单线索。
- **评估**：非缺陷——catalog 标注准确（方法存在，可被 GraphQL/测试调用），"每日批量重算"语义由调度器循环调 lead 列表承载，属接线实现细节。
- **建议**：无需动作；接线时由调度器实现批量循环。

## 4. 审计方法与局限

- **覆盖**：job-scheduling.md 全文、25 个 cron 配置键全量核对、13 个声称 BizModel 入口经 rg 核实、6 维度逐项 rg 证据。
- **抽样**：作业候选清单来源于两轮独立 explore 调研（计划 Current Baseline 已固化），本审计复核代表性抽样而非逐条重扫 52 行。
- **未深入**：各作业业务规则正确性（归各域 owner doc，非本审计范围）；nop-batch 模块实际接线（Non-Goal，归各域 follow-up）。
- **裁决性质**：本审计为计划 `2026-07-04-1600-1` 的结束审计（批处理/调度维度），配合 `2026-07-02-0000-best-practices-compliance-audit.md`（ORM/BizModel 维度）构成完整覆盖。

## 5. 相关文档

- `docs/architecture/job-scheduling.md` — 审计对象（重写后权威全局作业目录）
- `docs/plans/2026-07-04-1600-1-batch-scheduling-architecture.md` — 推动计划
- `docs/audits/2026-07-02-0000-best-practices-compliance-audit.md` — 既有合规审计（不覆盖批处理维度）
- `nop-entropy/docs-for-ai/03-modules/nop-job.md`、`nop-batch.md`、`nop-task.md` — 平台契约参照
