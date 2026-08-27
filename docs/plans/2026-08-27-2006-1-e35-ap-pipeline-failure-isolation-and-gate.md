# 2026-08-27-2006-1-e35-ap-pipeline-failure-isolation-and-gate E3.5 AP 文档管道失败落账持久化 + 逐项隔离 + upload 门

> Plan Status: active（独立草案审查共识：iteration 1 acceptable-as-is，task `ses_fbcde3401ffeQj5NUahefnWxt7`）
> Last Reviewed: 2026-08-27
> Source: `docs/audits/2026-08-26-2226-multi-audit-erp-enhancement.md` P1-1/P1-2/P1-3（多面审计 needs revision；`docs/audits/2026-08-26-2226-open-audit-erp-enhancement.md` §独立复证记录 三项机制独立复证成立）
> Related: plan `2026-08-26-0735-2`（E3 整体实现，已关闭——本计划修复其遗留 P1）、plan `2026-08-14-1815-2`（bank-recon/crm REQUIRES_NEW 失败隔离先例）
> Audit: required

## Current Baseline

- `fail()`（`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinApDocumentPipelineProcessor.java:414-422`）先置 `FAILED` + `saveOrUpdateEntity` + `log(...,"FAIL",false,...)`，随后返回 `NopException` 由 `readFile`/`classifyAndDraft`/`draft` 抛出（:445/:455/:167/:202/:214）。
- 同步入口 `ErpFinApDocumentBizModel.processApDocument` 为 `@BizMutation`（自动事务，RuntimeException 回滚）→ `FAILED` 状态与 `FAIL` 轨迹行随外层回滚丢失；`retry()` 的 FAILED 守卫（:284）在同步路径不可达（死分支）。
- 异步载体 `_vfs/nop/batch-task/fin/ap-document.batch.xml` `transactionScope="process"`（整 processor 单事务）；`processPending`（:296-308）`for (doc : pending) process(...)` 无 per-item try/catch → 队头失败回滚整批（先行文档 PARSED/CLASSIFIED/DRAFTED 翻转全部丢失）并饿死后续 RECEIVED 文档（limit 100 每轮重扫，毒文档永久循环）。
- `upload()`（:104-129）仅 `acquireUploadPermit()` 限流，未调 `requirePipelineEnabled`；`process`/`confirmAndDraft`/`retry`/`processPending` 四入口均有门（:134/:256/:282/:297）。`ErpFinConfigs.DEFAULT_AP_DOC_PIPELINE_ENABLED = false`（ErpFinConfigs.java:6-7）。E3 计划「默认关闭零暴露」声明（2026-08-26-0735-2 :219）与实现漂移。
- 测试现状：`app-erp-all/src/test/java/io/nop/app/all/it/TestErpFinApDocumentPipeline.java`（`enableActionAuth=FALSE`）无 FAILED 持久化路径用例；`testPipelineDisabledByDefault` 仅断言 `processApDocument` 被拒，upload 在门关时成功。
- 同型先例（本仓已验证范式）：`ErpFinBankReconAutoReverseHelper`（`runInTransaction(null, REQUIRES_NEW, ...)` + try/catch WARN + 返回 boolean，:93）与 `ErpCrmLeadScoringRecalcHelper`（plan 2026-08-14-1815-2 落地：batch process scope 保持 + helper 内 REQUIRES_NEW 逐条独立事务实现 per-item 隔离）。compliance checker R10（REQUIRES_NEW 事务）基线当前 12，新增站点须按 per-site 证据登记。
- E2E：`tests/e2e/business-actions/fin-ap-document.value.spec.ts`、`tests/e2e/business-actions/ai-interface.value.spec.ts` 已存在；playwright config 与 `_tmp-server.sh` 携带 `-Derp-fin.ap-doc-pipeline-enabled=true`（开放审计已证真）。

## Goals

- **失败状态可观测持久（P1-1）**：任何 PARSE/CLASSIFY/DRAFT 步骤失败后，文档 `FAILED` 状态 + `errorMsg` + `FAIL` 轨迹行在外层事务回滚后仍存活（同步与异步两条路径）；`retry()` FAILED 守卫在同步路径可达。
- **异步批次逐项失败隔离（P1-2）**：单文档失败不回滚先行已成功文档、不阻断后续 RECEIVED 文档处理；失败文档以 FAILED 终态落账，退出 RECEIVED 扫描循环。
- **「默认关闭零暴露」成立（P1-3）**：管道关闭时 `upload()` 与其余四入口一致被拒（`ERR_AP_DOC_PIPELINE_DISABLED`）。

## Non-Goals

- 多面审计 P2 项不在本计划范围（P2-1 重复上传幂等 / P2-8 上传输入契约 / P2-9 错误码契约 / P2-14 batch 接线与鉴权覆盖 / P2-3 ARCHIVED 归档 / P2-6/P2-7 分类与 OCR 瑕疵 / 开放审计 P2-E 新 mutation 鉴权 E2E 等）——已登记 `docs/backlog/erp-enhancement-roadmap.md §Follow-up Backlog`。
- 不改 ORM、不改 job yaml 三件套拓扑（`erp-fin-ap-doc-processing.job.yaml` cron/默认关保持）、不引入新队列基建。
- 邮件摄取与自动归档维持 E3.5 原 Non-Goal。

## Task Route

- Type: `implementation-only change`（已确认实时缺陷修复）
- Owner Docs: `docs/design/finance/document-driven-ap-automation.md`（AP-1 人工门/AP-4 处理轨迹落账语义）、`docs/design/ai-native-interface.md`（config-gate 护栏）、`../nop-entropy/docs-for-ai/02-core-guides/batch-dsl.md`（process/chunk 事务语义）
- Skill Selection Basis: `nop-backend-dev`（BizModel/Processor 事务边界、跨实体调用与错误码约定——修复面正在其决策门内）；`nop-testing`（JunitAutoTestCase 断言与 request 编写——负路径测试为本计划核心 Proof）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（E2E 本地 server 已携带 pipeline-enabled JVM 参数；无新端口/密钥/外部服务）

## Execution Plan

### Phase 1 - 失败落账持久化 + per-item 隔离 + upload 门

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinApDocumentPipelineProcessor.java`（交付载体 = 现有 Processor 注入 `ITransactionTemplate`/`IOrmTemplate`——Phase 1 Decision 裁定）；`app-erp-all/src/test/java/io/nop/app/all/it/TestErpFinApDocumentPipeline.java`（3 组 Proof）。batch.xml 未改（方案 A 保持 process scope）。
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision | Proof`（Fix 3 项 / Decision 1 项 / Proof 3 项）
- Prereqs: none

- [x] `Fix` **P1-1 失败落账独立事务持久化**：`fail()` 的 FAILED 状态写 + FAIL 轨迹行改为独立事务提交（镜像 `ErpFinBankReconAutoReverseHelper` REQUIRES_NEW 范式，:93 `runInTransaction(null, REQUIRES_NEW, ...)`）：REQUIRES_NEW 块内**按 id 在新 session 重载实体后更新**（外层事务已对同一行 staged PARSED/CLASSIFIED 写，直接 saveOrUpdate 传入实例有 session 附着/行锁自阻塞风险），保证外层 `@BizMutation`/batch process 事务回滚后失败证据存活，随后才抛出 NopException。Skill: `nop-backend-dev`
  - 落地：`fail()` 拆出 `persistFailure(documentId, step, errorMsg)`——`runInTransaction(null, REQUIRES_NEW)` + `ormTemplate.runInNewSession`（强制新 session，防外层 session 附着污染）内按 id 重载 → FAILED/errorMsg + FAIL 轨迹 + `session.flush()` 独立提交；落账自身异常仅 WARN 不掩盖原始业务异常。附带把 `readFile()` 的 `fileStore.getFile` 异常（nop-file 记录不存在时 `requireEntityById` 抛出）纳入 PARSE fail 落账（此前绕过 fail() 裸抛）。
- [x] `Decision` **P1-2 per-item 隔离机制与载体**：**方案 A（采用）= `processPending` 逐项独立事务 + try/catch WARN 后继续**（镜像 `ErpCrmLeadScoringRecalcHelper`，batch.xml 保持 `transactionScope="process"`，与 bank-recon 先例同型）。**方案 B（预否决）= `transactionScope="chunk"` + try/catch**——已被本仓 Explore 证据否决：plan `2026-08-14-1815-2` Phase 1 对 nop-batch-core `BatchTaskBuilder.buildChunkProcessor` 实测，process 与 chunk 两 scope 均整 chunk 单事务、无 per-item 隔离；且本 batch loader 为单哨兵行（`return [{}]`），scope 切换语义上无效果。Decision 须同时裁定交付载体（同包新 helper vs 现有 Processor 注入 `ITransactionTemplate`），理由与残留风险记入本计划；如实施中发现 batch 语义新冲突，先证后裁。Skill: `nop-backend-dev`
  - 载体裁决（执行时落定）：**现有 Processor 注入 `ITransactionTemplate` + `IOrmTemplate`（非新 helper）**——管道步骤/失败落账/逐项隔离同属 Processor 职责，新 helper 须重复 daoProvider 访问器且无复用方；beans.xml 零改动（字段按类型注入，对齐既有 `daoProvider`/`fileStore` 注入形态）。残留风险：`persistFailure` 用 `runInNewSession` 强制新 session（先例 helper 用 `runInSession` 复用/新建二选一），原因是失败落账与外层 staged 写同 session 复用会把先行 PARSED/CLASSIFIED 轨迹一并 flush 进落账事务（语义可接受但混入未审计中间态）；新 session 按 id 重载读已提交基线，落账内容最小化。未发现 batch 语义新冲突（process scope 下 REQUIRES_NEW 挂起/恢复与先例运行时同型）。
- [x] `Fix` **P1-2 落地逐项失败隔离**：按 Decision 裁决的机制与载体实现——单文档失败不影响先行已成功文档的状态翻转、不阻断后续 RECEIVED 文档；失败文档 FAILED 终态落账。Skill: `nop-backend-dev`
  - 落地：`processPending` 循环改调新增 `protected processOne(documentId, context)`——`runInTransaction(null, REQUIRES_NEW)` + `runInSession` 包 `process(documentId)` + 显式 flush，try/catch WARN 返回 boolean（失败不计数不中断）；FAILED 终态自然退出 RECEIVED 扫描。
- [x] `Fix` **P1-3 upload() 补 config 门**：`upload()` 入口调用 `requirePipelineEnabled`（默认关闭时上传被拒，错误码 `ERR_AP_DOC_PIPELINE_DISABLED`），与其余四入口对齐。Skill: `nop-backend-dev`
- [x] `Proof` **FAILED 持久化测试（同步路径）**：构造步骤失败（如 DRAFT 阶段 pur 服务不可用或 PARSE 文件读取失败），断言调用抛出后文档状态仍为 `FAILED`、errorMsg 落账、`FAIL` 轨迹行存在、`retry()` 守卫可达且可重试。Skill: `nop-testing`
  - 落地：`testFailedStatusPersistedAfterSyncFailure`——毒文档（fileId 指向不存在文件记录 → PARSE 失败）经 `processApDocument` 报错后，断言 FAILED/errorMsg/FAIL 轨迹在外层事务回滚后存活；随后修复文件引用（copyFileRef）→ `retryApDocument` 过 FAILED 守卫全管道 DRAFTED + retryCount=1 + RETRY 轨迹（守卫可达且可重试全环）。
- [x] `Proof` **逐项隔离测试（异步路径语义）**：队列含毒文档 + 正常文档，毒文档失败后正常文档仍被处理成功；毒文档 FAILED + FAIL 轨迹存活且不再被 RECEIVED 扫描命中。Skill: `nop-testing`
  - 落地：`testProcessPendingPerItemFailureIsolation`——毒文档 + 正常文档混队直调 `processPending`：processed=1、正常文档 DRAFTED（先行成功不被回滚/阻断）、毒文档 FAILED + FAIL 轨迹、二次扫描 0 命中（FAILED 终态退出 RECEIVED 循环）。
- [x] `Proof` **upload 门测试**：管道关闭时 `uploadApDocument` 被拒（更新 `testPipelineDisabledByDefault` 断言面，upload 由「成功」改为「被拒」）。Skill: `nop-testing`

Exit Criteria:

- [x] 三组负路径测试绿：`mvn test -pl app-erp-all -Dtest=TestErpFinApDocumentPipeline`
- [x] 既有正路径/人工门/幂等用例零回归（同命令全类跑绿）

### Phase 2 - 收口验证与登记

Status: completed
Targets: `docs/audits/compliance-baseline.md`（仅 R10 per-site 登记）、`docs/logs/2026/`、本计划 Closure
Skill: `nop-testing`

- Item Types: `Proof | Fix`
- Prereqs: Phase 1

- [x] `Proof` scoped 全量复跑：`mvn test -pl module-finance/erp-fin-service` + `mvn test -pl app-erp-all`（IT 层全绿）。Skill: `nop-testing`
  - 实测：fin-service **519/519 绿**；app-erp-all **64/64 绿（1 skipped = 预存 `ErpAllWebPagesCollectTest` @Disabled JDK26/ANTLR 维持）**；另全 reactor `mvn clean install -DskipTests` BUILD SUCCESS。
- [x] `Proof` 受影响 E2E 复跑（flux 模式）：`fin-ap-document.value.spec.ts` + `ai-interface.value.spec.ts` 绿。Skill: `none`
  - 实测：`_tmp-server.sh start`（fresh DB，server JVM 携带 `-Derp-fin.ap-doc-pipeline-enabled=true`）+ `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test`（仓库根执行）——两 spec **5/5 用例绿**（含 upload→parse→classify→draft 全链 + `/r/` 通道 upload 身份落账，证明 upload 门在 enabled 部署下零回归）。
- [x] `Fix` compliance checker 复跑：新增 REQUIRES_NEW 站点按 per-site 证据登记 R10 基线上调（先例 RC-R1.23 登记法，注记 + 机器可读块同步）；如有新增 daoFor 站点（预期无）须开基线裁决 successor，不得内联。Skill: `none`
  - 实测：R10 **12→14**（+2 per-site：`ErpFinApDocumentPipelineProcessor#persistFailure:482` / `#processOne:347`，注记节 + BASELINE 机器可读块 + 人类可读基线表三处同步）；R2c/R2b/R2d/R12c 零漂移（1538/240/38/42 == 基线，无新增 daoFor 站点）；checker 复跑全 19 规则 actual == updated baseline。
- [x] `Proof` 日志条目（`docs/logs/` 当日，含验证状态与全绿注记 + 多面审计复审要求四项证据备齐供独立复审）。**翻转触发权登记**：多面审计 `Audit Status: planned → closed` 由 mission 审计闭环的独立复审执行，触发条件 = 本批三计划（`2026-08-27-2006-1/2/3`）全部 `completed`；执行计划不自行翻转审计状态。Skill: `none`
  - 落地：`docs/logs/2026/08-27.md` 顶部新增本计划条目（验证全绿注记 + 四项证据索引 + 审计翻转触发权归 mission 闭环）。

Exit Criteria:

- [x] checker actual ≤ baseline（R10 上调有 per-site 注记）
- [x] 日志条目落盘

## Draft Review Record

- Independent draft review iteration 1: **acceptable-as-is（4 MINOR 采纳修订）**（task `ses_fbcde3401ffeQj5NUahefnWxt7`，fresh session）——全部 file:line 基线主张活仓核verified 全吻合（fail()/processPending/upload()/四门位/batch.xml process scope/testPipelineDisabledByDefault/bank-recon :93/crm helper 存在/R10=12/两 E2E spec + JVM 参数）；4 MINOR 已修：①方案 B 以 2026-08-14-1815-2 nop-batch-core 实测证据预否决，Explore 门降为「发现新冲突先证后裁」；②P1-1 Fix 明确 REQUIRES_NEW 块内按 id 新 session 重载（防 session 附着/行锁自阻塞）；③交付载体改为 Decision 显式输出并同步 Targets 行；④日志项 Fix→Proof。

## Closure Gates

> 完整仓库验证在此处：结束时运行一次（对齐 `docs/context/project-context.md` 真实命令；全量 `mvn test` 归 mission VERIFY 批亦可在结审计前完成）。

- [ ] 范围内行为完成（P1-1/P1-2/P1-3 三缺陷修复且负路径测试同落——多面审计复审要求①）
- [ ] 相关文档对齐（复核 `document-driven-ap-automation.md`/`ai-native-interface.md` 中「默认关闭零暴露」「处理轨迹落账」表述与实现一致；如本计划未改变其表述则不写）
- [ ] 已运行验证（scoped mvn test + app-erp-all IT + 受影响 E2E + compliance checker——复审要求②③）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中（多面审计复审要求④的回填由独立复审/审计闭环执行，执行者只备好证据）

## Deferred But Adjudicated

（无）

## Closure

Status Note: pending

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- 无（P2 项已归 roadmap Follow-up Backlog，非本计划范围）
