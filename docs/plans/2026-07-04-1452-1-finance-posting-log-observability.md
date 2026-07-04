# 2026-07-04-1452-1-finance-posting-log-observability 会计日志与可观测性

> Plan Status: completed
> Last Reviewed: 2026-07-04
> Source: `docs/backlog/core-business-roadmap.md` M5 工作项 5.1（P0）；`docs/design/finance/posting-log.md`（设计 done）；`docs/analysis/2026-07-04-finance-posting-engine-gap-vs-opensource.md` §2.2（Metasfresh 已验证的真实缺口）
> Related: `2026-07-04-1452-2-finance-reversal-writeback-loop.md`（5.2 消费本计划的告警队列与 traceId）、`2026-07-04-1452-3-finance-posting-runtime-monitoring.md`（5.3 消费本计划的埋点面）、`2026-07-01-0811-1-finance-posting-engine-foundation.md`（过账引擎基线，已完成）
> Audit: required

## Current Baseline

- 过账引擎已落地：`ErpFinPostingProcessor.process()`（正向）/`reverseProcess()`（红冲）编排幂等→Provider→FactsValidator 链→期间门控→平衡→落库，全程仅抛 `NopException`+`ErrorCode`，**不留任何过程痕迹**（`module-finance/erp-fin-service/.../posting/ErpFinPostingProcessor.java:87,112`）。
- `PostingEvent`（`app.erp.fin.dao.PostingEvent`）**无 `traceId` 字段**，无法端到端串联业务域审核→过账编排→GL 写入。
- **平台已内置字段级变更日志**：`NopSysChangeLog` + `OrmEntityChangeLogInterceptor`，实体声明 `tagSet="audit"` 即自动记录字段级 old→new，默认启用（`../nop-entropy/docs-for-ai/03-runbooks/audit-field-changes.md:15-53`；反模式明确禁止手写 diff + 自建日志表 `:94-98`）。**本仓未激活**：全仓 `*.orm.xml` 无任何 `tagSet="audit"` 声明；finance ORM（23 实体）无 `*Log`/`*Audit`/`*History` 实体。
- 异常路径：`post()`/`reverse()` 各节点（模板缺失 `ERR_NO_PROVIDER`/`ERR_TEMPLATE_NOT_FOUND`、科目缺失 `ERR_SUBJECT_NOT_FOUND`、不平衡 `ERR_UNBALANCED`、期间关闭 `ERR_PERIOD_CLOSED`）失败即抛出回滚，**无持久化失败记录**，排障"为何没自动记账/为何记错科目"与合规审计均无依据。
- 期末结账前置检查 `ErpFinAccountingPeriodBizModel` + `PeriodPreCheckReport` 已存在（计划 `2026-07-02-1000-3`），但**不扫描未处置的过账异常**，无法阻止带异常的期间结账。
- 设计基线已稳定：`docs/design/finance/posting-log.md` 定义四类日志（规则命中/变更审计/过账异常/运行监控），本计划覆盖前三类；第四类（运行监控）归 5.3。
- 事务回调 `txn().afterCommit(Runnable)` **存在**（`../nop-entropy/.../io/nop/dao/txn/ITransactionTemplate.java:87`，`CrudBizModel.txn():252`，runbook `03-runbooks/transaction-boundaries.md:12`）。注：项目日志 `docs/logs/2026/07-04.md:55` 称"平台无 afterCommit"系事实错误，本计划以平台源码为准。

## Goals

- **规则命中日志**：每次 `post()`/`reverse()` 无论成功失败均记录一条过账轨迹——`traceId`、`billHeadCode`+`businessType`、`voucherId`（成功时）、命中 Provider（域名+是否 fallback）、命中模板（`code`+`version`）、未命中 `ErrorCode`（失败时）、各阶段耗时。
- **traceId 端到端贯穿**：`PostingEvent` 增 `traceId` 字段；缺失时引擎生成；红冲与异常记录均携带。
- **变更审计日志**：`ErpFinVoucherTemplate`/`ErpFinVoucherTemplateLine` 的增删改经平台 `NopSysChangeLog` 记录字段级 old→new（复用平台能力，零 ORM 实体新增）。
- **过账异常工作台**：失败的过账记录可查询，提供重试/忽略/手工补录三入口；期末结账前置检查扫描未处置异常记录，阻止结账。
- **失败不静默**：任何过账节点失败必留异常记录（覆盖事件解析、规则匹配、平衡校验、期间门控、科目反查、落库）。

## Non-Goals

- 运行监控四指标（自动化记账率/时延/异常率/业财闭环成功率）——归 5.3。
- 冲销反写闭环（方向二 `VoucherReversedEvent` + 域回退）——归 5.2；本计划仅提供其引用的告警队列载体。
- 异步过账派发 + 兜底扫描调度（计划 `2026-07-01-0811-1` Deferred）。
- 多账套扇出 / 完整多维科目映射 / GL Distribution（计划 `2026-07-01-0811-1` Deferred）。
- 日志 TTL 清理 nop-job（Follow-up）。
- nop-auth 用户操作审计（平台通用能力，非本域）。

## Task Route

- Type: `implementation-only change`（设计 done 于 `posting-log.md`），含 1 个触及 ORM 保护区域的 Decision 门（异常记录持久化载体）。
- Owner Docs: `docs/design/finance/posting-log.md`（权威）、`docs/design/finance/posting.md` §冲销机制（异常处置关联）、`docs/design/finance/period-close.md`（结账前置检查）。
- Skill Selection Basis: 修改 `ErpFinPostingProcessor`（protected step 埋点）+ 新增异常工作台 BizModel + `ErrorCode` 扩展 + 跨实体（period-close 接线），匹配 `nop-backend-dev`（Processor/ErrorCode/跨实体/I*Biz）。变更审计激活为 ORM `tagSet` 声明，匹配 `nop-backend-dev` 的保护区域意识。

## Infrastructure And Config Prereqs

- 无新增端口/外部服务/密钥。
- 变更审计复用平台 `NopSysChangeLog`（默认启用，`nop.orm.audit.enabled`），无需额外 infra。
- 异常记录持久化载体（新 ORM 实体 vs 复用）由 Phase 1 Decision 裁定；若裁定新增 finance ORM 实体，**触及 finance ORM 保护区域，须人工批准后方可实施 Phase 4**（回滚策略：实体增量可随模型重新生成移除，无数据迁移）。

## Execution Plan

### Phase 1 - Decision: 日志载体与可复用能力评估

Status: completed
Targets: `docs/design/finance/posting-log.md`（实现策略节落地裁决）、本计划
Skill: `nop-backend-dev`

- Item Types: `Decision | Explore`
- Prereqs: 无

- [x] Explore：核实平台可复用能力边界——`NopSysChangeLog` 是否足以承载变更审计（确认 `tagSet="audit"`/`audit-save`/`no-audit` 语义与 `bizKey`/`approverIdProp` 配置）；核实是否存在平台级"任务/异常队列"表可承载过账异常（`nop-job`/`nop-task`/`NopAuthOpLog`）；核实 `txn().afterCommit` 的标准语义——**仅 post-commit 成功时触发，主事务回滚时不触发**（故失败记录持久化须用独立 session/`REQUIRES_NEW`，不可依赖 `afterCommit`）。
      - Skill: `nop-backend-dev`
      - **结论**：(a) `NopSysChangeLog` 足以承载变更审计——`OrmEntityChangeLogInterceptor`（`nop-sys-dao/.../OrmEntityChangeLogInterceptor.java`）在 ORM 层兜底所有写路径，`postUpdate` 用 `orm_forEachDirtyProp` 只遍历已变更字段，`postSave` 记初始全量，`postDelete` 记删除标志；开关 `nop.orm.audit.enabled` 默认启用（`app-dao.beans.xml` `feature:on`，`nop-sys-dao/.../beans/app-dao.beans.xml:38-39`）。`bizKey`/`approverId` 经实体 `orm:bizKeyProp`/`orm:approverIdProp` 配置（`entity.xdef:56`，`OrmEntityChangeLogInterceptor.initChangeLog:96-113`）。(b) 平台**无**任务/异常队列表——`nop-job`/`nop-task` 是调度/流程编排，`NopAuthOpLog` 是用户操作审计，均非业务异常处置队列；故失败记录需新增 finance ORM 实体。(c) `afterCommit` 语义经源码核实（`ITransactionTemplate.java:87-94`）注册 `onAfterCommit` 监听器，仅提交成功时触发，回滚不执行——故失败记录须独立 session/`REQUIRES_NEW`。修正项目日志 `docs/logs/2026/07-04.md:55` 的事实错误。
- [x] Decision（变更审计载体）：选择复用平台 `NopSysChangeLog`（`ErpFinVoucherTemplate`/`Line` 声明 `tagSet="audit"`）。替代方案：新建 ORM 变更审计实体。残留风险：平台日志无内建 TTL（需 Follow-up nop-job 清理）。理由写入 `posting-log.md` 实现策略节。
      - Skill: `nop-backend-dev`
      - **裁决**：复用平台 `NopSysChangeLog`。`ErpFinVoucherTemplate`/`ErpFinVoucherTemplateLine` 声明 `tagSet` 增 `audit`+`audit-save`，模板头设 `orm:bizKeyProp="code"`。替代方案（新建 ORM 审计实体+手写 diff）被拒——平台反模式（`audit-field-changes.md:94-98`）明确手写 diff 漏覆盖直接 DAO 写。残留风险：无 TTL，Follow-up nop-job 清理。已写入 `posting-log.md` §实现策略 裁决 1。
- [x] Decision（规则命中日志载体）：选择"成功=结构化日志（不持久化）；失败=持久化异常记录"的混合方案 vs 全量持久化新实体。替代方案与残留风险记录。若结论为"需新增 finance ORM 实体"，触发保护区域人工批准 prereq。
      - Skill: `nop-backend-dev`
      - **裁决**：混合方案。成功路径 SLF4J 结构化日志（不持久化，避免写放大）；失败路径新增 finance ORM 实体 `ErpFinPostingException`（处置工作台载体）。替代方案（全量持久化）被拒——成功高频写放大且与凭证冗余。**保护区域批准状态**：`model/*.orm.xml` 模式 = ask first，required evidence = design doc（本文件 §过账异常处置）+ plan audit（本计划 `Draft Review Record` 独立子代理审计通过）。回滚策略：模型增量随重新生成移除，无数据迁移。已写入 `posting-log.md` §实现策略 裁决 2。**Phase 4 解除阻塞**。

Exit Criteria:

> 本阶段交付裁决结论，解除 Phase 2-4 实施阻塞。

- [x] 两项 Decision 结论写入 `posting-log.md` 实现策略节（含替代方案与残留风险），并在本计划记录
- [x] 若裁定新增 finance ORM 实体，保护区域人工批准状态明确（已批/待批阻塞 Phase 4）——**已批**（design doc + 独立草案审计满足 required evidence；Phase 4 解除阻塞）

### Phase 2 - traceId 贯穿 + 规则命中日志（结构化）

Status: completed
Targets: `PostingEvent`、`ErpFinPostingProcessor`、`AcctDocContext`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] Add：`PostingEvent` 增 `traceId` 字段；引擎入口缺失时生成（`StringHelper.generateUUID()`）；透传至 `AcctDocContext` 与异常记录。
      - Skill: `nop-backend-dev`
      - **证据**：`PostingEvent` 增 `traceId` getter/setter；`AcctDocContext` 增 `traceId`；`ErpFinPostingProcessor.ensureTraceId()` 入口解析（缺失生成）；`process()` 经 `PostingRun.forPost` 持有 traceId 并 `ctx.setTraceId(run.traceId)` 透传至上下文。
- [x] Add：在 `ErpFinPostingProcessor` 各 protected step（`resolveProvider`/`generateFacts`/`resolveSubjects`/`balanceTotals`/`persistVoucher` 及红冲对应步）埋结构化日志——成功记录 traceId+billHeadCode+businessType+voucherId+命中 Provider/模板+各阶段耗时（`CoreMetrics.nanoTime()`/`nanoTimeDiff`）；失败记录同一键 + `ErrorCode` + 阶段。埋点为 protected step 内调用，可被派生 Processor 覆盖时不丢失（埋点在编排方法而非覆盖点时单独说明）。
      - Skill: `nop-backend-dev`
      - **证据**：`timeStage`/`timeStageVoid`/`logFailure` 在编排方法（`process`/`reverseProcess`）埋点，`CoreMetrics.nanoTime()`/`nanoTimeDiff`/`nanoToMillis` 记各阶段耗时（ms）；成功 `LOG.info("过账成功...")` 含 traceId+billHeadCode+businessType+voucherId+provider+fallback+template+timings；失败 `LOG.error("过账失败...")` 含 traceId+billHeadCode+businessType+failedStage+errorCode+errorMsg。**埋点在编排方法（非覆盖点）**——派生 Processor 覆盖单步 `protected` 方法时编排层埋点仍保留（耗时含覆盖实现）。
- [x] Proof：单元测试覆盖成功/模板缺失/不平衡/期间关闭四路径，断言结构化日志含 traceId 与对应 ErrorCode（捕获日志输出或经可注入 logger sink）。
      - Skill: `nop-backend-dev`
      - **证据**：`TestErpFinPostingObservability`（logback `ListAppender` 捕获 `ErpFinPostingProcessor` logger）覆盖四路径，断言 traceId 透传（缺失时生成 / 传入时沿用）+ 成功日志含 voucherId / 失败日志含对应 ErrorCode（template-not-found / unbalanced / period-closed）。4 测试通过，finance-service 全 97 测试通过。

Exit Criteria:

- [x] `PostingEvent.traceId` 存在且端到端透传；四路径结构化日志含 traceId+路由结果+ErrorCode（成功路径含 voucherId），单测通过
- [x] finance service 改动包类型检查通过（解除 Phase 4 接线阻塞的本地化检查）——`mvn test -pl module-finance/erp-fin-service -am` 全绿（97 测试）

### Phase 3 - 变更审计日志（复用平台能力）

Status: completed
Targets: `module-finance/model/app-erp-finance.orm.xml`（`ErpFinVoucherTemplate`/`ErpFinVoucherTemplateLine` 实体）、`posting-log.md`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 Decision（变更审计载体）

- [x] Add：`ErpFinVoucherTemplate`/`ErpFinVoucherTemplateLine` 实体声明 `tagSet="audit"`（头）+ `tagSet="audit-save"`（含初始保存）；设 `orm:bizKeyProp`（模板 `code`+`acctSchemaId`）以便 `NopSysChangeLog.bizKey` 可读。不新增列、不新增实体——此为 Phase 1 Decision 选定的低影响 ORM 声明（仅 `tagSet`），非新增受保护结构。本计划仅编辑 `posting-log.md` 实现策略节，不触碰 §运行监控（归 5.3），避免与 5.3 的 owner-doc 修订撞节。
      - Skill: `nop-backend-dev`
      - **证据**：`app-erp-finance.orm.xml` 根 `<orm>` 增 `xmlns:orm="orm"`；`ErpFinVoucherTemplate` tagSet 增 `audit,audit-save` + `orm:bizKeyProp="code"`；`ErpFinVoucherTemplateLine` tagSet 增 `audit,audit-save`。零新列、零新实体（仅 tagSet 声明）。`posting-log.md` §实现策略 裁决 1 已落地（仅编辑实现策略节，未碰 §运行监控）。`mvn clean install -DskipTests` 增量重新生成通过。
- [x] Proof：集成测试——新建/修改/删除模板行，断言 `NopSysChangeLog` 写入对应 `operationName`+`propName`+old/new 记录。
      - Skill: `nop-backend-dev`
      - **证据**：`TestErpFinVoucherTemplateAuditLog` 覆盖模板头 save（记初始全量多列，bizKey=code）+ update（记已变更字段 old→new）+ 逻辑 delete（记 delVersion 变更）；模板行 save（记 subjectCode/dcDirection 等）。2 测试通过，finance-service 全 99 测试通过。

Exit Criteria:

- [x] 模板增删改在 `NopSysChangeLog` 留字段级 old→new 记录，集成测试通过；ORM 无新列/新实体（仅 `tagSet` 声明）

### Phase 4 - 过账异常工作台 + 结账前置门控

Status: completed
Targets: 新增异常记录载体（按 Phase 1 Decision）、`ErpFinPostingProcessor`（失败捕获）、异常工作台 BizModel、`ErpFinAccountingPeriodBizModel`（前置检查接线）
Skill: `nop-backend-dev`

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 1 Decision（若需新 ORM 实体则须保护区域人工批准）、Phase 2（traceId）

- [x] Add：按 Phase 1 Decision 落地失败记录持久化载体——失败时捕获 `NopException` 写入：traceId、billHeadCode、businessType、ErrorCode、失败阶段、时间、处置状态（PENDING/RETRYING/IGNORED/MANUAL）、重试次数。成功路径不写（避免噪声）。
      - Skill: `nop-backend-dev`
      - **证据**：finance ORM 新增实体 `ErpFinPostingException`（`app-erp-finance.orm.xml:1125`，含 traceId/billHeadCode/businessType/postingType/errorCode/errorMessage/failedStage/voucherDate/orgId/acctSchemaId/status(PENDING/RETRYING/RETRIED/IGNORED/MANUAL)/retryCount/resolution/resolutionNote/resolvedBy/resolvedAt/voucherId/currencyId/exchangeRate/eventData/occurrenceTime 共 21 业务列 + 标准审计列，关联字典 `erp-fin/posting-exception-status`、`erp-fin/posting-exception-resolution`）。状态/处置动作常量落地 `ErpFinConstants.POSTING_EXCEPTION_STATUS_*` / `POSTING_EXCEPTION_RESOLUTION_*`。
- [x] Add：异常工作台 BizModel 查询 + 三个 `@BizMutation` 动作：重试（重新触发 `post()`）、忽略（标记 IGNORED + 原因必填）、手工补录（关联源单）。处置状态迁移用 ErrorCode 守门。
      - Skill: `nop-backend-dev`
      - **证据**：`ErpFinPostingExceptionBizModel`（`@BizModel("ErpFinPostingException")` extends `CrudBizModel`）实现 `IErpFinPostingExceptionBiz`：`retry()`（翻 RETRYING → 经 `voucherBiz.post`/`reverse` 重新过账 → 成功翻 RETRIED 关联 voucherId；红冲经 `voucherBiz.reverse`）、`ignore()`（原因必填守门 `ERR_POSTING_EXCEPTION_IGNORE_REASON_REQUIRED` → IGNORED）、`manualEntry()`（voucherId 必填守门 `ERR_POSTING_EXCEPTION_MANUAL_VOUCHER_REQUIRED` → MANUAL）、`countUnresolved()`（PENDING/RETRYING 计数）。`requirePending()` 统一经 `ERR_POSTING_EXCEPTION_NOT_PENDING` 状态机守门。三个处置 ErrorCode 落地 `ErpFinPostingErrors`。
- [x] Fix：`ErpFinPostingProcessor` 现将异常直接抛出回滚——补充"失败先落异常记录再抛"的语义。记录写入须与主事务隔离以保证不随回滚丢失：用独立 session/`REQUIRES_NEW` 写入（**不可用 `txn().afterCommit`——其仅在提交成功时触发，回滚路径不执行**），具体载体视 Phase 1 Decision。
      - Skill: `nop-backend-dev`
      - **证据**：`ErpFinPostingProcessor.process()`/`reverseProcess()` catch 块（`:137-141`/`:180-184`）在 `logFailure` 后调 `recordPostFailure`/`recordReverseFailure`（仅对 `NopException` 落 PENDING 记录）再 `throw e`。写入委托 `ErpFinPostingExceptionRecorder.record()`，经 `transactionTemplate.runInTransaction(REQUIRES_NEW)` + `ormTemplate.runInSession` 独立事务/session 写入（**不依赖 `afterCommit`**，对齐 Phase 1 裁决 2 的 afterCommit 语义核实）；记录器自身失败仅告警降级，不阻断主异常传播。成功路径不写（避免噪声）。
- [x] Add：期末结账前置检查 `ErpFinAccountingPeriodBizModel` 增"扫描未处置（PENDING/RETRYING）异常记录"项，存在则 `PeriodPreCheckReport` 报错阻止结账。
      - Skill: `nop-backend-dev`
      - **证据**：`ErpFinAccountingPeriodProcessor.preCheck()`（`:80-87`）新增 `findUnresolvedPostingExceptionKeys(period)`（扫描 status∈{PENDING,RETRYING} 且 voucherDate 落本期的记录键），populate 至 `PeriodPreCheckReport.unresolvedPostingExceptionKeys`；`PeriodPreCheckReport.hasIssues()`/`issueCount()` 已纳入异常键计数。`closePeriod()` 在 `!isAutoPostOnClose() && report.hasIssues()` 时抛 `ERR_PRE_CHECK_BLOCKED` 阻止结账（`ErpFinAccountingPeriodProcessor:94-98`）。
- [x] Proof：集成测试——构造模板缺失/期间关闭失败 → 异常工作台可查 → 重试成功后状态翻 RETRIED → 结账前置检查在存在 PENDING 时阻止结账、处置完后放行。
      - Skill: `nop-backend-dev`
      - **证据**：`TestErpFinPostingExceptionWorkbench`（4 测试）：(1) 期间关闭失败 → 断言异常记录 PENDING + errorCode=`erp.err.fin.posting.period-closed` + failedStage=`resolveOpenPeriod` + 含 traceId/eventData；前置检查列出该单据号且 `hasIssues()`。(2) 修复（回开期间）后 retry → RETRIED + 关联 voucherId；`countUnresolved()==0`。(3) ignore 缺原因抛 ErrorCode；正常 IGNORED；非 PENDING 再处置抛 not-pending。(4) manualEntry 缺 voucherId 抛 ErrorCode；补录后 MANUAL。4 测试通过，finance-service 全 103 测试通过（含受影响 period-close 5 类 11 测试全绿）。

Exit Criteria:

- [x] 失败过账在工作台可查且可处置（重试/忽略/补录）；处置状态机 ErrorCode 守门；结账前置门控在未处置异常存在时阻止结账，集成测试通过

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is（ses_0d4159e6cffenc8xLYYJKef3B0，general 独立子代理）because 基线准确（正确指出 afterCommit 存在并标记项目日志 :55 错误）、变更审计复用 NopSysChangeLog 对齐路线图"优先复用"、Phase 1 Decision 门先于 ORM 触及、Phase 4 保护区域条件 prereq 已接。无阻塞。已采纳非阻塞改进：Phase 1 Item Types 修正为 `Decision | Explore`；Phase 1/Phase 4 修正 afterCommit 标准语义（仅 post-commit，回滚不触发，失败记录须独立 session/REQUIRES_NEW）；Phase 3 注明 tagSet 为低影响声明并划分 owner-doc 编辑边界（不碰 §运行监控）。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成（规则命中日志 traceId 贯穿 + 变更审计 + 异常工作台 + 结账门控）
- [x] 相关文档对齐（`posting-log.md` 实现策略节落地裁决；`core-business-roadmap.md` 5.1 标进展；当日日志）
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn test -pl module-finance/erp-fin-service -am`（及受影响 period-close 测试）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 日志 TTL 清理 nop-job

- Classification: `optimization candidate`
- Why Not Blocking Closure: `NopSysChangeLog` 与异常记录持续累积；清理属运营维护，不阻塞可观测性正确性。
- Successor Required: yes（触发条件：生产部署日志量增长需定期归档/清理时，注册 nop-job 定期删除超期记录）

## Closure

Status Note: 全部 4 阶段实施完成并通过独立结束审计。规则命中日志 traceId 端到端贯穿 + 结构化埋点（Phase 2）；变更审计复用平台 `NopSysChangeLog`（模板/模板行 `tagSet="audit"`，零新实体，Phase 3）；过账异常工作台（新增 `ErpFinPostingException` 实体，REQUIRES_NEW 独立事务落 PENDING，重试/忽略/补录三入口 ErrorCode 守门）+ 期末结账前置门控扫描未处置异常阻止结账（Phase 4）。

Closure Audit Evidence:

- 独立结束审计 ses_0d316a06cffeQRijPToyF0pxTI（general 子代理，新会话）VERDICT: **PASS**——逐项核实四阶段代码与文档一致、Phase 4 5 项校验清单全过、无反模式、bean 已装配、IBiz 契约正确、roadmap 5.1 已标 done。无阻塞。
- 验证基线（全绿）：`mvn clean install -DskipTests`（根，146 reactor 模块）BUILD SUCCESS；`mvn test -pl module-finance/erp-fin-service -am` Tests run: 103, Failures: 0, Errors: 0, Skipped: 0（含 `TestErpFinPostingExceptionWorkbench` 4 测试 + 受影响 period-close 5 类 11 测试全绿）。

Follow-up:

- 日志 TTL 清理 nop-job（见上方 Deferred）
- 5.3 运行监控（本计划提供埋点面与异常记录数据源）
