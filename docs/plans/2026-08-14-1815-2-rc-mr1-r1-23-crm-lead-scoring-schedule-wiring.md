# 2026-08-14-1815-2-rc-mr1-r1-23-crm-lead-scoring-schedule-wiring RC-R1.23 — crm 线索评分调度接线（MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-14
> Mission: requirement-compliance
> Work Item: RC-R1.23（P1-RC-035 crm 线索评分 SCHEDULED 触发器接线 + owner doc lead-scoring.md 失实声明纠正）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.23 行 + `docs/audits/arm-index.md` P1-RC-035 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（纯 job 接线 + config + owner doc 修订）
> Related: `docs/design/crm/use-cases.md`（L1 UC-CRM-09）；`docs/design/crm/lead-scoring.md`（§配置/L2 失实声明 :157）；`docs/audits/2026-08-05-1030-rc-ma1-a1-28-crm-f1-lead-lifecycle.md`（§5 UC-CRM-09 + §6 finding 表）；`docs/plans/2026-08-08-0424-1-rc-mr1-r1-5-hr-attendance-last-wins.md`（job bean + job.yaml 接线范式参照）；`docs/plans/2026-08-07-2340-3-rc-mr1-r1-4-hr-leave-approver-timeout.md`（Job bean + job.yaml + config 门控范式参照）
> Audit: required

## Current Baseline

- **finding P1-RC-035（arm-index 行，UC-CRM-09 SCHEDULED 触发器完全缺失 + L2 失实）**：L1（`use-cases.md:184`）逐字「评分触发（MANUAL / LEAD_UPDATE / SCHEDULED）」。L3 实仓：MANUAL = `ErpCrmLeadScoreBizModel.recalculateScore:35-41`（@BizMutation）+ LEAD_UPDATE = `ErpCrmLeadBizModel.defaultPrepareUpdate:227-240`（config-gated `erp-crm.lead-scoring.recalc-on-lead-update` 默认 true）均已实现；**SCHEDULED 触发器 = 无 Java Job bean**（`ErpCrmLeadScoringRecalcJob` 类不存在，grep `app-service.beans.xml` 仅注册 4 job bean[ForecastRecalc/EventReminder/SequenceOverdue/FunnelAggregation]，无评分 job；module-crm 无 scheduler.yaml）。**L2 `lead-scoring.md:157` 逐字失实声称**「SCHEDULED：`ErpCrmLeadScoringRecalcJob` + `scheduler.yaml` 已接线，空值=跳过；非空时迭代 active 线索逐条 `IErpCrmLeadScoreBiz.recalculateScore()`（triggerEvent=SCHEDULED，单线索失败隔离）」——实际无此 job、无 scheduler.yaml 接线。§2 P1①（功能完全缺失）+ §2 P1②（L2 失实——owner doc 向实现妥协/虚报，对齐 §4 根因）。**非 P0**（评分缺失仅影响定时批量重算，MANUAL/LEAD_UPDATE 触发路径可用，auto-qualify 阈值逻辑独立可用）。
- **实仓（HEAD 核查）**：
  - `ErpCrmConstants.java:57-70`：`CONFIG_LEAD_SCORING_AUTO_QUALIFY = "erp-crm.lead-scoring.auto-qualify"` / `CONFIG_LEAD_SCORING_RECALC_ON_LEAD_UPDATE = "erp-crm.lead-scoring.recalc-on-lead-update"` / `CONFIG_LEAD_SCORING_SCHEDULE_CRON = "erp-crm.lead-scoring.schedule-cron"`（:60，**声明但生产代码零消费——dead config**；grep 命中仅限 doc/roadmap/arm-index 引用）/ `TRIGGER_EVENT_MANUAL/LEAD_UPDATE/SCHEDULED`（:68-70）。
  - `ErpCrmLeadScoreBizModel.recalculateScore(leadId, triggerEvent, context)`：@BizMutation → `ErpCrmLeadScoreRecalculateScoreProcessor.recalculateScore` → `LeadScoringEngine.recalculateScore`（LOOKUP/FORMULA/BOOLEAN 评分 + 归一化 + append-only + auto-qualify + 只追加历史）。**评分引擎本身完整且强测**（`TestErpCrmForecastAndScoring.testScoringAndAutoQualify`）。
  - **job 接线现状**：`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-crm-lead-scoring-recalc.job.yaml` **已存在**（jobName=erp-crm-lead-scoring-recalc，enabled 默认 false，cronExpr 默认 `0 2 * * *`，invoker = `nopBatchTaskRunner` + taskPath `/nop/batch-task/crm/lead-scoring-recalc.batch.xml`）+ `module-crm/.../_vfs/nop/batch-task/crm/lead-scoring-recalc.batch.xml` **已存在**（**transactionScope="process"**，loader = `ErpCrmLead` docStatus `notIn [CONVERTED,LOST,CANCELLED]`，processor source = `inject('IErpCrmLeadScoreBiz')` → `recalculateScore(item.id, 'SCHEDULED', batchChunkCtx.serviceContext)`）——**2026-07-18 nop-batch 迁移（commit 940b43184）已建 batch-task 式接线，但 A1.28 审计（2026-08-05）未识别该接线形态**（审计按 Java Job bean + scheduler.yaml 判据搜索，scheduler.yaml 已被迁移清空为 `enabled: true`；`ErpCrmLeadScoringRecalcJob` Java 类在迁移 plan 2026-07-18-1600 中删除）→ **本行须先 Explore 现有 batch-task 接线是否运行时可达/正确，再决定修法**（对齐 RC-R1.2 bank-recon-auto-reverse 已验证的 nopBatchTaskRunner + batch.xml 范式）。
  - **失败隔离语义（关键差异——L2 声明「单线索失败隔离」与现有接线不符）**：batch.xml `transactionScope="process"`（非 chunk）——nop-batch-dsl 语义（`docs-for-ai/02-core-guides/batch-dsl.md:117-126`）：chunk = 每 chunk 一事务 / process = 每 processor 阶段一事务；processor 为裸 `biz.recalculateScore(...)` 调用无 try/catch，`@BizMutation` REQUIRED 加入 process 事务 → **现接线无 per-item 失败隔离**（任一线索抛异常将失败整个 processor 阶段）。对照 bank-recon 先例：隔离由 helper 内部 REQUIRES_NEW + try/catch 承载（`erpFinBankReconAutoReverseHelper`），非 batch 事务承担。**L2 `lead-scoring.md:157` 声称「单线索失败隔离」在现接线下不成立** → 本行须裁决：方案 A（推荐）= 修复接线实现真隔离（`transactionScope="chunk"` + per-item try/catch 或 REQUIRES_NEW helper 镜像 bank-recon，batchSize 200 = chunk 级隔离粒度权衡**Decision 项**）；方案 B = 显式调整 L2 声明为「chunk 级隔离」非 per-item。**这决定本行是「接线修正 + 隔离落地」而非仅「确认接线」**。
  - **config 门控现状**：job.yaml 用 `nop.job.erp-crm-lead-scoring-recalc.enabled|false` + `nop.job.erp-crm-lead-scoring-recalc.cron-expr|0 2 * * *` 两级 @cfg 门控（**非** `erp-crm.lead-scoring.schedule-cron`）；`CONFIG_LEAD_SCORING_SCHEDULE_CRON` 声明但零消费 → L2 `lead-scoring.md:157` 声称的「schedule-cron 空值=跳过；非空触发」配置键与 job.yaml 实际键不一致 → **L2 失实第二层**（配置键名不符）。
  - `batchChunkCtx.serviceContext` 变量在 nop-batch-dsl processor source 上下文可用性：参照 RC-R1.2 已验证 bank-recon-auto-reverse.batch.xml（同构 `inject('erpFinBankReconAutoReverseHelper').reverseOne(item.id, batchChunkCtx.serviceContext)`）+ pnl-calc.batch.xml（`inject('IErpPrjProjectPnlBiz').refreshPnl(...)`）——同构模式已落地多个 job（erp-fin-deferred-posting-sweep / erp-prj-pnl-calc / erp-qa-spc-sampling / erp-mfg-jobcard-auto-generate 均 invoker=nopBatchTaskRunner），**batch-task 式接线是本仓既有标准模式**（2026-07-18 迁移），非 Java Job bean 范式。**Explore 项**：核实 `inject('IErpCrmLeadScoreBiz')` 在 batch source 上下文解析（`biz_ErpCrmLeadScore` bean ioc:type 已注册，`IErpCrmLeadScoreBiz` 为接口名——参照 pnl-calc 用 `IErpPrjProjectPnlBiz` 同型成功先例）。
  - **测试基线**：`TestErpCrmForecastAndScoring`（引擎层强测）+ `TestErpCrmEventReminderJob`/`TestErpCrmForecastRecalcJob`（Job bean 层测试范式）——**无 batch-task 式 job 的 dedicated 测试**（无 TestErpCrmLeadScoringRecalcJob、无 batch 任务级测试）。
- **预授权判据**（第一批纯预授权）：纯 job 接线（job.yaml/batch.xml 修正或确认）+ config 消费 + owner doc 修订，**不触 ORM 结构/会计过账/删除**；roadmap RC-R1.23 行 `todo`，Deps（R1.0 done）已满足；job.yaml/batch.xml 属配置与 VFS 资源文件（非 Java 代码）修改预授权。
- **涉及文件**：`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-crm-lead-scoring-recalc.job.yaml`（config 键对齐）；`module-crm/.../_vfs/nop/batch-task/crm/lead-scoring-recalc.batch.xml`（确认或修正）；`ErpCrmConstants.java`（config key 消费）；`docs/design/crm/lead-scoring.md`（L2 失实纠正）；`docs/architecture/job-scheduling.md`（:177,310 配置键/接线注记同步）；新增 job/batch 测试类 1 个。

## Goals

- **SCHEDULED 触发器运行时可达（P1-RC-035 核心）**：确认或修正既有 batch-task 接线（job.yaml + batch.xml）使定时评分链路**运行时成立**——nop-job 调度 → nopBatchTaskRunner → batch.xml loader（active 非终态线索）→ per-item `IErpCrmLeadScoreBiz.recalculateScore(item.id, 'SCHEDULED', ctx)` **且失败隔离语义与 L2 声明一致**（per-item 或经 Decision 明确调整声明）。
- **失败隔离落地（P1-RC-035 隐藏义务）**：裁决并落地隔离语义——方案 A：`transactionScope="chunk"`（+ per-item try/catch 或 REQUIRES_NEW helper 镜像 bank-recon 先例）实现 per-item 隔离；方案 B：L2 声明调整为 chunk 级隔离。**Decision 项**二选一并记录理由（倾向 A：对齐 L2「单线索失败隔离」字面 + bank-recon 已验证范式）。
- **config 键对齐**：`ErpCrmConstants.CONFIG_LEAD_SCORING_SCHEDULE_CRON = "erp-crm.lead-scoring.schedule-cron"` 由 dead config 转为被 job.yaml 消费（job.yaml cronExpr `@cfg:erp-crm.lead-scoring.schedule-cron|0 2 * * *` 或等效对齐——**Decision 项**：对齐方向 = job.yaml 消费业务键 vs 常量改 nop.job.* 键，参照 R1.4/R1.5 先例业务 config 键为门控）。
- **L2 失实纠正**：`lead-scoring.md:157` 声明改为与实际接线一致（batch-task 式 job.yaml + nopBatchTaskRunner，非「ErpCrmLeadScoringRecalcJob + scheduler.yaml」）+ config 键名更正。
- **测试**：新增 batch-task 级测试（镜像 TestErpCrmForecastRecalcJob 范式或 batch 任务执行测试——**Decision 项**：nop-batch 任务在 JUnit 下的执行入口，参照 nop-batch-dsl `TestBatchTaskRunner` 或经 job invoker 反射执行）——断言 cron 空/非空门控 + SCHEDULED 触发后 ErpCrmLeadScore 记录生成 + 失败隔离。
- **零回归**：既有 crm 测试全绿（`TestErpCrmForecastAndScoring` 等）+ 全仓构建。
- **回填**：arm-index P1-RC-035 → `done (RC-R1.23)` + roadmap 行 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不实现 NOTIFY_OWNER 派发（P2-RC-031）**（独立 P2 watch-only finding，非本行义务）。
- **不实现 LeadScoreConfigBizModel 保存时唯一性校验（P2-RC-034）**（独立 P2，非本行义务）。
- **不改评分引擎逻辑**（LeadScoringEngine/Processor 零改动——引擎已完整强测，本行仅接调度触发面）。
- **不触 ORM 结构**（零列/零索引——Option B「Lead.score 列」既有 Decision 维持派生查询方案）。
- **不改真相源契约段落**（use-cases L1 不动）。
- **不重写 batch 框架机制**（nopBatchTaskRunner 模式保持——2026-07-18 迁移确立的既有标准，本行对齐而非另起炉灶）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑/接线修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/crm/use-cases.md`（L1 UC-CRM-09）+ `docs/design/crm/lead-scoring.md`（§配置/L2 失实段）+ `docs/design/crm/README.md` + `docs/audits/2026-08-05-1030-rc-ma1-a1-28-crm-f1-lead-lifecycle.md`（§5 UC-CRM-09 裁决）
- Skill Selection Basis: 实现面 = job.yaml/batch.xml 接线 + config 门控消费（`nop-backend-dev`：job 接线范式、config 范式、nop-batch 任务机制）；测试（`nop-testing`：JunitAutoTestCase + job/batch 测试范式）。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- config key `erp-crm.lead-scoring.schedule-cron`（默认 `0 2 * * *` cron 表达式；空值=禁用语义与 L2 声明对齐）+ `nop.job.erp-crm-lead-scoring-recalc.enabled`（默认 false 保持——部署启用决策，对齐 A4.2.95 config-gate 范式）。无需 .env/外部服务。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-crm/erp-crm-service`。

## Execution Plan

### Phase 1 - Explore 既有 batch-task 接线运行时状态（Decision）

Status: completed
Targets: `erp-crm-lead-scoring-recalc.job.yaml`；`lead-scoring-recalc.batch.xml`；`ErpCrmConstants.java`
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [x] `Decision` **接线形态裁决**：**选项 A（采用）** = 确认/修正既有 batch-task 式接线（job.yaml + batch.xml + nopBatchTaskRunner——2026-07-18 迁移标准模式，参照 RC-R1.2 已验证 bank-recon job）；选项 B（否决）= 新建 Java Job bean `ErpCrmLeadScoringRecalcJob`（镜像 ForecastRecalc/EventReminder 范式）。**Explore 证据**：①`inject('IErpCrmLeadScoreBiz')` 在 batch source 上下文解析可达——`biz_ErpCrmLeadScore` bean 注册于 `_service.beans.xml:234-236`（`ioc:type="app.erp.crm.biz.IErpCrmLeadScoreBiz"`），同型 pnl-calc `inject('IErpPrjProjectPnlBiz')`（`IErpPrjProjectPnlBiz` 亦经 `_service.beans.xml:71` ioc:type 注册）已落地部署；②`batchChunkCtx.serviceContext` 变量在 processor source 可用（同型 bank-recon/pnl-calc batch.xml，draft review 已核实 `IBatchChunkContext` default 委派方法）；③`nopBatchTaskRunner` bean 注册于 nop-batch-dsl `batch-dsl.beans.xml:6`（`io.nop.batch.dsl.runner.BatchTaskRunner`）；④job.yaml 加载链：`LocalJobConfigLoader.scanJobConfigs("/nop/job/conf")` 扫描 `.job.yaml`（TestErpAllJobYamlLoading 断言 21 个），`@cfg:` 经 `ConfigValueResolver` 解析（`AppConfig.var` + 默认值），enabled=false 默认不注册（`LocalJobConfigLoader.registerJob` 仅注册 enabled=true；TestLocalJobConfigLoader.testDefaultEnabledIsFalse 证明）。→ **选项 A**（最小改动 + 既有模式）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **失败隔离语义裁决（F1）**：**选项 A（采用）** = 落地 per-item 隔离——新建 REQUIRES_NEW helper（镜像 bank-recon `ErpFinBankReconAutoReverseHelper` 范式，`transactionTemplate.runInTransaction(null, REQUIRES_NEW, ...)` + try/catch WARN + 返回 boolean）；选项 B（否决） = L2 声明调整为「chunk 级隔离（batchSize=200）」。**Explore 证据（nop-entropy 实测）**：`BatchTaskBuilder.buildChunkProcessor`（nop-batch-core）：`batchTransactionScope==process` 时 `InvokerBatchConsumer` 包裹整个 chunk consumer（`transactionalInvoker.invoke` 包整 chunk 单事务），`chunk` scope 时 `AddCompletedBatchConsumer` 等——**process/chunk 两 scope 均整 chunk 单事务，任一项抛异常回滚整 chunk，无 per-item 隔离**（与 draft review 已核实结论一致）；bank-recon 先例 = `transactionScope="process"` + helper 内 REQUIRES_NEW 逐条独立事务。残余风险（选项 B 弃后无）：chunk 级隔离的「同 chunk 内失败项后续项丢弃」不适用。
      - Skill: `nop-backend-dev`
- [x] `Proof` **运行时验证前置**：job.yaml 加载链核实——①`LocalJobConfigLoader` 扫描 `/nop/job/conf`（21 个 job.yaml，`TestErpAllJobYamlLoading` 断言）；②`@cfg:` 键经 `ConfigValueResolver.resolveValue`（`AppConfig.var(key)` 有值取之、null 取默认）；③`nopBatchTaskRunner` bean 注册（nop-batch-dsl `batch-dsl.beans.xml:6`）；④taskPath `/nop/batch-task/crm/lead-scoring-recalc.batch.xml` 文件存在（`module-crm/erp-crm-service/src/main/resources/_vfs/nop/batch-task/crm/`）；⑤`inject('IErpCrmLeadScoreBiz')` 解析（`biz_ErpCrmLeadScore` bean ioc:type，同型 pnl-calc 成功先例）。运行时可达性最终由 Phase 3 batch 任务级测试（`IBatchTaskRunner.execute(taskPath)`）给出。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 接线形态裁决记录（A 采用 + 理由）+ 隔离语义裁决记录（A 采用 + 理由），Explore 证据（inject 解析/batch source 上下文/transactionScope 语义）落盘计划或日志
- [x] 既有 batch.xml/job.yaml 无语法/引用错误（XML 可解析 + taskPath 命中文件）

### Phase 2 - 接线落地 + config 对齐（P1-RC-035 核心）

Status: completed
Targets: `erp-crm-lead-scoring-recalc.job.yaml`；`lead-scoring-recalc.batch.xml`；`ErpCrmConstants.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision`
- Prereqs: Phase 1 完成（裁决决定实现形态）

- [x] `Decision` **config 门控键对齐**：**选项 A（采用）** = job.yaml cronExpr 消费 `erp-crm.lead-scoring.schedule-cron`（`@cfg:erp-crm.lead-scoring.schedule-cron|0 2 * * *`），常量由 dead 转活跃（job.yaml + `ErpCrmLeadScoringRecalcHelper.isScheduleCronConfigured()` 双消费点）；选项 B（否决） = 常量改为 `nop.job.erp-crm-lead-scoring-recalc.cron-expr`（跟随 job.yaml 现状）。**理由（选项 A）**：①`CONFIG_LEAD_SCORING_SCHEDULE_CRON` 从 dead config 转活跃，grep 显示 job.yaml + helper 消费；②L2 `lead-scoring.md:157` 声明的键名 `erp-crm.lead-scoring.schedule-cron` 与实际接线一致（文档键名保持）；③`job-scheduling.md:177` 该作业行已列 `erp-crm.lead-scoring.schedule-cron` → 选项 A 下自动一致（选项 B 下须同步改键名）；④业务 config 键范式对齐 R1.4/R1.5（job 层 enabled 门控 + 业务键门控双层）。`enabled` 保持 `@cfg:nop.job.erp-crm-lead-scoring-recalc.enabled|false`（部署 opt-in，对齐 A4.2.95 config-gate 范式）。
      - Skill: `nop-backend-dev`
- [x] `Fix` 按 Phase 1 裁决落地接线（选项 A + F1=per-item）：`erp-crm-lead-scoring-recalc.job.yaml` cronExpr 改 `@cfg:erp-crm.lead-scoring.schedule-cron|0 2 * * *`；**新建 `ErpCrmLeadScoringRecalcHelper`**（`module-crm/erp-crm-service/.../job/`，镜像 bank-recon `ErpFinBankReconAutoReverseHelper` 范式：`transactionTemplate.runInTransaction(null, REQUIRES_NEW, ...)` 逐条独立事务 + try/catch WARN + 返回 boolean；schedule-cron 空值=跳过 INFO——「空值=跳过」语义落地）+ `app-service.beans.xml` 注册 + **batch.xml processor 改调 helper**（`inject('erpCrmLeadScoringRecalcHelper').recalculateOne(item.id, batchChunkCtx.serviceContext)`；`transactionScope` 保持 "process" 与 bank-recon 同型）；loader 保持 `notIn [CONVERTED,LOST,CANCELLED]` 过滤 active 线索。**实施中发现并修复运行时缺陷（Explore 深化）**：`batchChunkCtx.serviceContext` 在 nop-batch 执行路径为 **null**（`BatchTaskRunner.executeAsync` → `batchTaskManager.newBatchTaskContext()` 无绑定上下文），而 `IErpCrmLeadScoreBiz` 代理调用需非 null ctx（`EvalServiceAction.invoke` → `context.getEvalScope()`）——helper 内空值兜底 `new ServiceContextImpl()`（对齐 R1.4/R1.5 Job bean `execute()` 自建 ctx 范式）；该缺陷同样潜伏于 pnl-calc 等既有 batch job（watch-only 记录，非本行范围）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `ErpCrmConstants`：`CONFIG_LEAD_SCORING_SCHEDULE_CRON` 常量已声明无需改值；javadoc 更新说明 job.yaml + `ErpCrmLeadScoringRecalcHelper` 消费（dead → 活跃）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 接线落地且 config 键消费闭环（`erp-crm.lead-scoring.schedule-cron` 非 dead——grep 显示 job.yaml + helper 消费）
- [x] job.yaml/batch.xml 与 L2 文档声明一致（Phase 4 文档同步后三方一致）

### Phase 3 - 测试矩阵

Status: completed
Targets: `module-crm/erp-crm-service/src/test/java/app/erp/crm/service/job/`（新增测试类）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 完成

- [x] `Add` 接线测试（`TestErpCrmLeadScoringRecalcJob`，batch 任务级执行 `IBatchTaskRunner.execute(taskPath)`——nop-batch-dsl 执行入口，参照 `TestBatchTaskRunner`；**erp-crm-service pom 补 nop-batch-dsl test-scope 依赖**）：① SCHEDULED 触发后 active 线索生成 ErpCrmLeadScore 记录（triggerEvent=SCHEDULED + totalScore=100 + append-only + 行级快照，镜像 TestErpCrmForecastAndScoring 断言）；② 终态线索（CONVERTED/LOST/CANCELLED）被 loader 排除不评分；③ schedule-cron 空值跳过语义（`assignConfigValue` 置空 → helper 跳过 INFO + 零评分记录）；④ **失败隔离断言**（F1=per-item）：不存在线索（ERR_LEAD_NOT_FOUND）→ REQUIRES_NEW 回滚 + WARN 日志（ListAppender 断言含 leadId）+ 返回 false，随后正常线索评分成功（失败不阻断批次继续）。
      - Skill: `nop-testing`
- [x] `Proof` 既有 `TestErpCrmForecastAndScoring`/`TestErpCrmEventReminderJob`/`TestErpCrmForecastRecalcJob` 零回归 + `TestErpAllJobYamlLoading`（job.yaml cronExpr 改动后 21 个文件仍可解析）——`mvn test -pl module-crm/erp-crm-service` 172 tests 全绿（168 基线 + 4 新增）+ `mvn test -pl app-erp-all -Dtest=TestErpAllJobYamlLoading` 1/1 绿。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新增测试全绿 + 既有 crm 测试零回归：`mvn test -pl module-crm/erp-crm-service`（BUILD SUCCESS，172 tests）
- [x] SCHEDULED 路径有运行时断言证据（非仅静态接线——`IBatchTaskRunner.execute` 真实执行 loader+processor+helper 全链）

### Phase 4 - L2 失实纠正 + 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/design/crm/lead-scoring.md`；`docs/architecture/job-scheduling.md`；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-14.md`
Skill: none

- Item Types: `Fix | Add`
- Prereqs: Phase 1-3 完成

- [x] `Fix` **L2 失实纠正（P1-RC-035 明示义务）**：`lead-scoring.md:157` 配置表行改为与实际接线一致（batch-task 接线形态：job.yaml → nopBatchTaskRunner → batch.xml → `ErpCrmLeadScoringRecalcHelper.recalculateOne()` REQUIRES_NEW 单线索失败隔离 → `IErpCrmLeadScoreBiz.recalculateScore()` triggerEvent=SCHEDULED；键名 `erp-crm.lead-scoring.schedule-cron` 保持 + 默认 `0 2 * * *` + 空值=跳过；门控语义 enabled 默认 false = 部署启用决策 + schedule-cron 空值=跳过）；不修改需求契约段（use-cases L1 不动）。**同步 `docs/architecture/job-scheduling.md`**：§3.9 `erp-crm-lead-scoring-recalc` 行调用入口/配置键/证据更新（RC-R1.23 接线修正）+ 配置键表 `erp-crm.lead-scoring.schedule-cron` 行补消费说明（选项 A 下键名自动一致，对齐 RC-R1.2 Phase 3 先例）。
      - Skill: none
- [x] `Add` arm-index P1-RC-035 → `done (RC-R1.23)` + 修复落地摘要（含 L2 失实纠正记录 + 隔离语义修正）；roadmap RC-R1.23 → done ✅（含落地摘要）；`docs/logs/2026/08-14.md` 日志条目。**R10 基线漂移登记**（plan Closure Gates 已知失败模式 #1，RC-R1.2 同型先例）：`compliance-baseline.md` 新增 R10 7→8 基线上调注记（per-site 证据：`ErpCrmLeadScoringRecalcHelper.recalculateOne` 1 处 REQUIRES_NEW，镜像 RC-R1.2 `ErpFinBankReconAutoReverseHelper` 同型站点）。
      - Skill: none

Exit Criteria:

- [x] `lead-scoring.md` 声明与实际接线一致（实现注记与配置表同步）；`job-scheduling.md` §3.9 行/配置键表与最终接线一致；arm-index/roadmap 状态回填 + 日志条目写入

## Draft Review Record

- Independent draft review iteration 1: `accept`（fix-forward 后；审查 pass 2026-08-14）— live-baseline 全量独立复核 CONFIRMED：①`erp-crm-lead-scoring-recalc.job.yaml`（enabled `@cfg:nop.job...|false` + cron-expr `@cfg:nop.job...|0 2 * * *` + nopBatchTaskRunner + taskPath）与 `lead-scoring-recalc.batch.xml`（transactionScope="process"、batchSize=200、loader notIn [CONVERTED,LOST,CANCELLED]、processor `inject('IErpCrmLeadScoreBiz').recalculateScore(item.id,'SCHEDULED',batchChunkCtx.serviceContext)`）逐字属实；②`ErpCrmConstants.java:57-70` 常量 + `CONFIG_LEAD_SCORING_SCHEDULE_CRON` 零消费属实（grep 仅 doc 引用）；③`lead-scoring.md:157` L2 失实逐字属实；④arm-index P1-RC-035 / roadmap RC-R1.23（todo）属实；⑤batch 事务语义实测（nop-entropy `BatchTaskBuilder.buildChunkProcessor` + `InvokerBatchConsumer`：process/chunk 均整 chunk 单事务，无 per-item 隔离——「单线索失败隔离由 batch chunk 事务承载」不成立）；⑥`TestBatchTaskRunner` 存在（nop-batch-dsl，`IBatchTaskRunner.execute(taskPath)` 执行入口）。**修正 1 MAJOR**：Phase 2 Fix 项隔离机制表述自相矛盾（「单线索失败隔离由 batch chunk 事务承载」与 Phase 1 F1 per-item 方案相反）——改为按 F1 裁决落地 + 注明 batch 事务不提供 per-item 隔离。**修正 1 MAJOR**：`docs/architecture/job-scheduling.md:177,310`（该作业现列配置键 `erp-crm.lead-scoring.schedule-cron` 与 job.yaml 实际键不符）未入 Phase 4 范围——补入 Targets/Fix 项/退出标准/涉及文件（对齐 RC-R1.2 Phase 3 先例）。**补 2 MINOR**：Phase 3 ③ 补失败隔离断言 + 执行入口确认；Closure Gates 补 checker 基线漂移登记注记（project-context 已知失败模式 #1，RC-R1.2 同型）。无 Blocker。共识达成，转 active。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。**完整仓库验证在此处**：结束时运行一次全量验证。

- [x] 范围内行为完成
- [x] 相关文档对齐
- [x] 已运行验证（`mvn test -pl module-crm/erp-crm-service` 172 tests 全绿 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline；**R10 基线漂移已登记**——R10 7→8 per-site 证据落 `docs/audits/compliance-baseline.md`「R10 基线上调注记（plan 2026-08-14-1815-2，RC-R1.23）」块，按 project-context 已知失败模式 #1 + RC-R1.2 同型先例；全仓 `mvn test` 2 项已知预存失败[ErpMfgCostRollupLine cell-not-prop + TestAuthSeedLoadingProof NPE]非本行引入，见 known-good-baselines.md:63-64 + docs/bugs/ 双注记）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-RC-035 批次接线 vs Java Job bean 的形态选择残余

- Classification: `optimization candidate`
- Why Not Blocking Closure: 接线形态（batch-task 式 vs Java Job bean）在 Phase 1 Explore 裁决，两形态均满足 L1「SCHEDULED 触发」验收标准（功能等价）；选择记录理由即可，不阻塞结束。若 Explore 证实 batch source `inject` 解析失败而需转 Java Job bean，属执行期修正非范围外新增。
- Successor Required: `no`

### P2-RC-031 NOTIFY_OWNER 派发（UC-CRM-09 边界）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 独立 P2 finding（`determineAction:163-164` 返回 NOTIFY_OWNER 但无实际派发），登记 watch-only 不随本行落地（本行 = SCHEDULED 触发器，NOTIFY_OWNER = 通知派发维度，不同控制点）；P2 登记不强制。
- Successor Required: `no`

## Closure

Status Note: 执行完成并独立结束审计通过（2026-08-14）。四 Phase 全绿：接线落地（batch-task 形态确认 + REQUIRES_NEW helper per-item 隔离 + config 键对齐 `erp-crm.lead-scoring.schedule-cron` 由 dead 转活跃 + 运行时 ctx 空值兜底修复）+ batch 任务级测试 4 组 + L2 失实纠正（lead-scoring.md:157 / job-scheduling.md §3.9 + 配置键表）+ arm-index/roadmap/日志回填 + R10 7→8 基线漂移登记。验证：erp-crm-service 172 tests 全绿 + TestErpAllJobYamlLoading 1/1 + 全量 `mvn clean install -DskipTests` BUILD SUCCESS + checker actual ≤ baseline（R10=8）。全仓 `mvn test` 2 项已知预存失败（mfg ErpMfgCostRollupLine cell-not-prop + TestAuthSeedLoadingProof NPE）非本行引入（known-good-baselines.md:63-64 + docs/bugs/ 双注记，commit 时间线早于本会话）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，只读，零文件修改）
- Evidence: **Verdict PASS**——①计划状态一致性（4/4 Phase completed + 全 `[x]` + Plan Status completed；Closure Gates 8/8 由执行者在本审计后按审计结论勾选）；②Phase 2 接线实仓核验（job.yaml cronExpr `@cfg:erp-crm.lead-scoring.schedule-cron|0 2 * * *` + enabled|false + nopBatchTaskRunner + taskPath；batch.xml loader notIn[CONVERTED,LOST,CANCELLED] + processor `inject('erpCrmLeadScoringRecalcHelper').recalculateOne(item.id, batchChunkCtx.serviceContext)` + transactionScope=process；helper REQUIRES_NEW + try/catch WARN + schedule-cron 空值跳过 + null ctx 兜底 ServiceContextImpl；beans.xml 注册 + ErpCrmConstants javadoc 更新）；③Phase 3 测试实跑（审计者自跑 `mvn test -pl module-crm/erp-crm-service -Dtest=TestErpCrmLeadScoringRecalcJob` 4/4 绿 + 全模块 172/172 绿 + TestErpAllJobYamlLoading 1/1 绿；nop-batch-dsl test-scope 依赖属实）；④Phase 4 文档实仓核验（lead-scoring.md:157 失实纠正 / job-scheduling.md §3.9 + 配置键表 / arm-index P1-RC-035 done (RC-R1.23) / roadmap RC-R1.23 done ✅ / 日志条目 / compliance-baseline R10 7→8 注记 + machine-readable R10: 8）；⑤范围守卫（git status 仅预期文件，零 ORM/会计/删除路径变更；审计者自跑全量 `mvn clean install -DskipTests` BUILD SUCCESS + checker actual ≤ baseline）；⑥预存失败核验（mfg cell-not-prop + authseed NPE 的 bug 文件 commit 时间早于本会话，与 CRM 线索评分无因果）。

Follow-up:

- 无范围外 follow-up；MR1 第一批后续 RC-R1.24+（UTM 归因族 / Forecast territory rollup / SPC 调度 / pnl-calc 调度等）由 mission driver 继续。watch-only 记录：`batchChunkCtx.serviceContext` null ctx 缺陷同样潜伏于 pnl-calc 等既有 batch job（非本行范围，后续 batch job 接线行可一并处理）。
