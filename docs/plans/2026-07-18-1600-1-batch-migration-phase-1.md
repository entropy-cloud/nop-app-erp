# 2026-07-18-1600-1-batch-migration-phase-1 nop-batch 迁移：全作业改造

> Plan Status: draft
> Last Reviewed: 2026-07-18
> Source: `docs/analysis/batch-processing-audit-and-migration-analysis.md`
> Related: `docs/plans/2026-07-04-1600-1-batch-scheduling-architecture.md`、`docs/architecture/job-scheduling.md`
> Audit: required

## Current Baseline

- **`nop-batch` 依赖**：零，`app-erp-all/pom.xml` 中无任何 nop-batch 模块
- **19 个 Java Job Bean**：全量已接线（bean 注册 + `scheduler.yaml` 条目 + bean 实现），覆盖 9 个域。全部使用 `findList()` + for-each 内存迭代反模式，无分 chunk/断点续传/记录级幂等
- **`scheduler.yaml`（旧机制）**：内联 19 个作业，零 `.job.yaml` 独立文件，无 per-job `enabled` 字段
- **`nopBatchTaskRunner`**：`nop-batch-dsl` 内置 Bean，引入依赖后自动可用
- **`IBatchChunkContext.getServiceContext()`**：已补充 default 委派方法（与 `getTaskName()`/`getTaskId()`/`getTaskKey()` 模式一致）
- **12 个 batch-candidate**：`job-scheduling.md` §7 已识别，但处于 deferred 状态

### 19 个待迁移作业一览

| # | jobName | 域 | 量级 | 执行模式 | 迁移类型 |
|---|---------|-----|------|----------|---------|
| 1 | `erp-fin-ar-ap-auto-recon` | fin | 大 | batch-candidate | A: batch.xml + .job.yaml，删 Java Bean |
| 2 | `erp-ast-depreciation` | ast | 大 | batch-candidate | A: batch.xml + .job.yaml，删 Java Bean |
| 3 | `erp-fin-cash-forecast-refresh` | fin | 中 | batch-candidate | A: batch.xml + .job.yaml，删 Java Bean |
| 4 | `erp-crm-lead-scoring-recalc` | crm | 小-中 | job → batch-candidate | A: batch.xml + .job.yaml，删 Java Bean |
| 5 | `erp-qa-spc-sampling` | qa | 中-大 | batch-candidate | A: batch.xml + .job.yaml，删 Java Bean |
| 6 | `erp-qa-spc-capability` | qa | 小-中 | job → batch-candidate | A: batch.xml + .job.yaml，删 Java Bean |
| 7 | `erp-prj-pnl-calc` | prj | 中-大 | batch-candidate | A: batch.xml + .job.yaml，删 Java Bean |
| 8 | `erp-fin-deferred-posting-sweep` | fin | 中 | job → batch-candidate | A: batch.xml + .job.yaml，删 Java Bean |
| 9 | `erp-mfg-jobcard-auto-generate` | mfg | 中 | job → batch-candidate | A: batch.xml + .job.yaml，删 Java Bean |
| 10 | `erp-mfg-crp-run` | mfg | 中 | job | B: .job.yaml 转换，保留 Java Bean |
| 11 | `erp-mnt-due-visit-generation` | mnt | 小-中 | job | B: .job.yaml 转换，保留 Java Bean |
| 12 | `erp-cs-sla-scan` | cs | 小 | job | B: .job.yaml 转换，保留 Java Bean |
| 13 | `erp-crm-forecast-recalc` | crm | 中 | job | B: .job.yaml 转换，保留 Java Bean |
| 14 | `erp-crm-event-reminder` | crm | 小 | job | B: .job.yaml 转换，保留 Java Bean |
| 15 | `erp-crm-sequence-overdue` | crm | 小 | job | B: .job.yaml 转换，保留 Java Bean |
| 16 | `erp-crm-funnel-aggregation` | crm | 中 | job | B: .job.yaml 转换，保留 Java Bean |
| 17 | `erp-cs-csat-reminder` | cs | 小 | job | B: .job.yaml 转换，保留 Java Bean |
| 18 | `erp-cs-entitlement-expiry` | cs | 小 | job | B: .job.yaml 转换，保留 Java Bean |
| 19 | `erp-hr-contract-expiry` | hr | 小 | job | B: .job.yaml 转换，保留 Java Bean |

迁移类型 A = 改造为 nop-batch chunk 处理（生成 `batch.xml` + `job.yaml`，删除 Java Job Bean）
迁移类型 B = `scheduler.yaml` → `.job.yaml` 格式转换（保留现有 Java Job Bean，只改调度声明方式）

## Goals

- 全仓 19 个作业全部完成 `.job.yaml` 迁移（`scheduler.yaml` 仅保留 `enabled: true`）
- 其中 9 个数据量偏大/全表扫描的作业改造为 nop-batch chunk 处理（迁移类型 A）
- 其余 10 个作业仅做 `.job.yaml` 格式转换，保持 `BeanMethodJobInvoker` 调用现有 Java Bean（迁移类型 B）
- 迁移后 `scheduler.yaml` 清理完毕，无内联作业

## Non-Goals

- 不动 Service/Processor 层的 `findAllByQuery()` 调用（不构成定时作业的部分）
- 不动 `nop-job-local` → `nop-job-service` 的演进
- 不改动任何业务逻辑（`I*Biz` 接口与实现不变，仅调度和 chunk 处理方式变化）

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/job-scheduling.md`、`docs/analysis/batch-processing-audit-and-migration-analysis.md`
- Skill Selection Basis: 涉及 nop-batch DSL 编写（`batch.xml` + `job.yaml`）与 `nopBatchTaskRunner` 使用，需加载 `nop-backend-dev` skill（I*Biz injection 模式）

## Infrastructure And Config Prereqs

- `nop-batch-dsl` Maven 依赖加入后编译通过即可
- 所有 `.job.yaml` 的 `enabled` 通过 `@cfg:nop.job.<jobName>.enabled|false` 绑定，缺省 `false`，生产部署时在 `application.yaml` 逐域启用

## Execution Plan

### Phase 1 — 基础设施：nop-batch-dsl 依赖

Status: planned
Targets: `app-erp-all/pom.xml`
Skill: `none`

- Item Types: `Add`

- [ ] 添加 `nop-batch-dsl` 依赖到 `app-erp-all/pom.xml`

> `nop-batch-dsl` 自动传递 `nop-batch-core`、`nop-batch-orm`、`nop-batch-dao`；`nopBatchTaskRunner` Bean 已在 `batch-dsl.beans.xml` 中注册。

Exit Criteria:

- [ ] `mvn clean install -DskipTests` 编译通过
- [ ] `nopBatchTaskRunner` Bean 可被容器识别

### Phase 2 — batch-candidate 迁移：erp-crm-lead-scoring-recalc（参考实现）

Status: planned
Targets:
  - `app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-crm-lead-scoring-recalc.job.yaml`（新建）
  - `module-crm/erp-crm-service/src/main/resources/_vfs/nop/batch-task/crm/lead-scoring-recalc.batch.xml`（新建）
  - `module-crm/erp-crm-service/src/main/java/app/erp/crm/service/job/ErpCrmLeadScoringRecalcJob.java`（删除）
  - `module-crm/erp-crm-service/src/main/resources/_vfs/` 下 `app-service.beans.xml` 中 `erpCrmLeadScoringRecalcJob` 的 bean 注册（删除）
  - `app-erp-all/src/main/resources/_vfs/nop/job/conf/scheduler.yaml`（移除 `erp-crm-lead-scoring-recalc` 条目）
Skill: `nop-backend-dev`

- Item Types: `Add | Fix`

- [ ] 创建 `erp-crm-lead-scoring-recalc.job.yaml`
- [ ] 创建 `lead-scoring-recalc.batch.xml`：`orm-reader`（`ErpCrmLead` filter `notIn(docStatus, terminal)`） + processor（`inject('IErpCrmLeadScoreBiz').recalculateScore()`），`batchSize=200`、`skipPolicy(maxSkipCount=100)`、`saveState=true`
- [ ] 删除 `ErpCrmLeadScoringRecalcJob.java`
- [ ] 清理 `app-service.beans.xml` 中 `erpCrmLeadScoringRecalcJob` 的 bean 注册
- [ ] 从 `scheduler.yaml` 移除 `erp-crm-lead-scoring-recalc` 条目
- [ ] Proof: 通过 `nopBatchTaskRunner.executeAsync` 执行 `batch.xml` 验证正确工作

Exit Criteria:

- [ ] 无 `ErpCrmLeadScoringRecalcJob` Java 类存在
- [ ] `.job.yaml` + `batch.xml` 可被 `nopBatchTaskRunner` 正确执行
- [ ] `scheduler.yaml` 无 `erp-crm-lead-scoring-recalc` 条目

### Phase 3 — batch-candidate 迁移：其余 8 个改造作业（迁移类型 A）

Status: planned
Targets: 依次处理以下 8 个作业（按域归类，避免跨域上下文切换）
Skill: `nop-backend-dev`

- Item Types: `Add | Fix`

#### fin 域（3 个）

- [ ] **erp-fin-ar-ap-auto-recon**：创建 `erp-fin-ar-ap-auto-recon.job.yaml` + `ar-ap-auto-recon.batch.xml`（`ErpFinArApItem` filter `eq(status,'OPEN')` → processor `inject('IErpFinReconciliationBiz').runAutoReconciliation()`，`batchSize=100`、`skipPolicy`、`saveState=true`）；删除 `ErpFinAutoReconJob.java` 及 bean 注册；清 `scheduler.yaml` 条目
- [ ] **erp-fin-cash-forecast-refresh**：创建 `erp-fin-cash-forecast-refresh.job.yaml` + `cash-forecast-refresh.batch.xml`（`ErpFinArApItem` + notes → processor `inject('IErpFinCashForecastBiz').refreshItem()`，`batchSize=500`）；删除 `ErpFinCashForecastJob.java` 及 bean 注册；清 `scheduler.yaml` 条目
- [ ] **erp-fin-deferred-posting-sweep**：创建 `erp-fin-deferred-posting-sweep.job.yaml` + `deferred-posting-sweep.batch.xml`（`ErpFinPostingException` filter `eq(status,'PENDING')`+ `lt(retryCount,3)` → processor `inject('IErpFinPostingBiz').retryPost()`，`batchSize=50`、`saveState=true`）；删除 `DeferredPostingSweepJob.java` 及 bean 注册；清 `scheduler.yaml` 条目

#### ast 域（1 个）

- [ ] **erp-ast-depreciation**：创建 `erp-ast-depreciation.job.yaml` + `depreciation.batch.xml`（`ErpAstDepreciationSchedule` filter `eq(status,'PENDING')`+ `lt(nextRunDate,now)` → processor `inject('IErpAstDepreciationScheduleBiz').executeSingle()`，`batchSize=50`、`retryPolicy`、`saveState=true`）；删除 `ErpAstDepreciationJob.java` 及 bean 注册；清 `scheduler.yaml` 条目

#### qa 域（2 个）

- [ ] **erp-qa-spc-sampling**：创建 `erp-qa-spc-sampling.job.yaml` + `spc-sampling.batch.xml`（`ErpQaInspectionLine` filter `eq(status,'APPROVED')` → processor `inject('IErpQaSpcSampleBiz').buildFromInspection()`，`batchSize=200`、`saveState=true`）；删除 `ErpQaSpcSamplingJob.java` 及 bean 注册；清 `scheduler.yaml` 条目
- [ ] **erp-qa-spc-capability**：创建 `erp-qa-spc-capability.job.yaml` + `spc-capability.batch.xml`（`ErpQaSpcChart` filter `eq(active,true)` → processor `inject('IErpQaSpcCapabilityBiz').calculateCpk()`，`batchSize=100`）；删除 `ErpQaSpcCapabilityJob.java` 及 bean 注册；清 `scheduler.yaml` 条目

#### prj 域（1 个）

- [ ] **erp-prj-pnl-calc**：创建 `erp-prj-pnl-calc.job.yaml` + `pnl-calc.batch.xml`（`ErpPrjProject` filter active 项目 → processor `inject('IErpPrjPnlBiz').aggregateProject()`，`batchSize=100`、`saveState=true`）；删除 `ErpPrjPnlCalcJob.java` 及 bean 注册；清 `scheduler.yaml` 条目

#### mfg 域（1 个）

- [ ] **erp-mfg-jobcard-auto-generate**：创建 `erp-mfg-jobcard-auto-generate.job.yaml` + `jobcard-auto-generate.batch.xml`（`ErpMfgWorkOrder` filter 已排程未建卡 → processor `inject('IErpMfgJobCardBiz').autoGenerate()`，`batchSize=50`、`saveState=true`）；删除 `ErpMfgJobCardAutoGenJob.java` 及 bean 注册；清 `scheduler.yaml` 条目

Exit Criteria:

- [ ] 8 个作业的 `.job.yaml` + `batch.xml` 全部创建到位
- [ ] 8 个 Java Job Bean 类已删除
- [ ] 对应的 bean 注册已清理
- [ ] 对应的 `scheduler.yaml` 条目已移除
- [ ] `mvn clean install -DskipTests` 编译通过

### Phase 4 — 非 batch 作业 `.job.yaml` 转换（迁移类型 B，10 个）

Status: planned
Targets: 依次处理以下 10 个作业，不做 chunk 改造，仅将 `scheduler.yaml` 条目外移到独立的 `.job.yaml`

Skill: `none`

- Item Types: `Add | Fix`

- [ ] **erp-mfg-crp-run**：创建 `erp-mfg-crp-run.job.yaml`（`invoker.bean: erpMfgCrpRunJob`），清 `scheduler.yaml` 条目
- [ ] **erp-mnt-due-visit-generation**：创建 `erp-mnt-due-visit-generation.job.yaml`，清 `scheduler.yaml` 条目
- [ ] **erp-cs-sla-scan**：创建 `erp-cs-sla-scan.job.yaml`，清 `scheduler.yaml` 条目
- [ ] **erp-crm-forecast-recalc**：创建 `erp-crm-forecast-recalc.job.yaml`，清 `scheduler.yaml` 条目
- [ ] **erp-crm-event-reminder**：创建 `erp-crm-event-reminder.job.yaml`，清 `scheduler.yaml` 条目
- [ ] **erp-crm-sequence-overdue**：创建 `erp-crm-sequence-overdue.job.yaml`，清 `scheduler.yaml` 条目
- [ ] **erp-crm-funnel-aggregation**：创建 `erp-crm-funnel-aggregation.job.yaml`，清 `scheduler.yaml` 条目
- [ ] **erp-cs-csat-reminder**：创建 `erp-cs-csat-reminder.job.yaml`，清 `scheduler.yaml` 条目
- [ ] **erp-cs-entitlement-expiry**：创建 `erp-cs-entitlement-expiry.job.yaml`，清 `scheduler.yaml` 条目
- [ ] **erp-hr-contract-expiry**：创建 `erp-hr-contract-expiry.job.yaml`，清 `scheduler.yaml` 条目

> 所有 B 类 `.job.yaml` 统一格式：
> ```yaml
> jobName: erp-xxx-yyy
> enabled: "@cfg:nop.job.erp-xxx-yyy.enabled|false"
> displayName: ...
> trigger:
>   cronExpr: "@cfg:nop.job.erp-xxx-yyy.cron-expr|<原 cron 值>"
> invoker:
>   bean: <原 Java Bean 名>
>   method: execute
> ```

Exit Criteria:

- [ ] 10 个 `.job.yaml` 文件全部创建到位
- [ ] 对应的 10 个 `scheduler.yaml` 条目全部移除
- [ ] `mvn clean install -DskipTests` 编译通过

### Phase 5 — scheduler.yaml 收尾与验证

Status: planned
Targets:
  - `app-erp-all/src/main/resources/_vfs/nop/job/conf/scheduler.yaml`
  - `docs/architecture/job-scheduling.md`
  - `docs/logs/2026/07-18.md`
Skill: `none`

- Item Types: `Add | Fix | Proof`

- [ ] 确认 `scheduler.yaml` 仅保留 `enabled: true`，无 `jobs:` 段落
- [ ] 确认全仓 19 个 `.job.yaml` 文件存在（按 nop-job 文档约定自 `/nop/job/conf/` 扫描）
- [ ] 确认无 `*Job.java` 残留（Phase 3 删除的 9 个 Bean 类已清理）
- [ ] 更新 `docs/architecture/job-scheduling.md` §2 的当前状态行
- [ ] 更新 `docs/logs/2026/07-18.md`
- [ ] Proof: `mvn clean install -DskipTests` 全绿
- [ ] Proof: 验证 `nop-job-local` 启动时正确加载所有 `.job.yaml`（日志确认）

Exit Criteria:

- [ ] `scheduler.yaml` 无 `jobs:` 段
- [ ] 19 个 `.job.yaml` 都在 `/nop/job/conf/` 下
- [ ] 构建全绿
- [ ] 日志确认 `.job.yaml` 全部注册

## Draft Review Record

- （待独立审查）

## Closure Gates

- [ ] 范围内行为完成（19 个作业全部完成迁移）
- [ ] 相关文档对齐（`job-scheduling.md`、`analysis` 报告、`docs/logs`）
- [ ] 已运行验证：`mvn clean install -DskipTests` 全绿
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### Service/Processor 层 `findAllByQuery()` 改造

- Classification: `optimization candidate`
- Why Not Blocking Closure: 不构成定时作业，不阻塞调度迁移
- Successor Required: `no`

### §7 中无现有 Java Bean 的 batch-candidate（DESIGN/REGISTERED/DEFERRED/WIRED 状态）

涉及 7 个作业：`erp-fin-posting-scan`、`erp-fin-period-close`、`erp-fin-consolidation`、`erp-fin-changelog-ttl`、`erp-inv-stock-check`、`erp-b2b-sftp-inbound-poll`、`erp-inv-costing-reclose`

另有 2 个设计级 batch-candidate（`erp-qa-spc-sample-aggregation`、`erp-prj-pnl-aggregation`）与现有 SCHEDULED 作业（`erp-qa-spc-sampling`、`erp-prj-pnl-calc`）语义重叠，本计划已覆盖其 SCHEDULED 版本

- Classification: `watch-only residual`
- Why Not Blocking Closure: 这些作业尚无 Java Job Bean 实现，不在 scheduler.yaml 中，无需迁移。后续实现时直接走 batch.xml + .job.yaml 模式即可
- Successor Required: `no`

## Closure

Status Note: （待完成时填写）

Closure Audit Evidence: （待完成时填写）
