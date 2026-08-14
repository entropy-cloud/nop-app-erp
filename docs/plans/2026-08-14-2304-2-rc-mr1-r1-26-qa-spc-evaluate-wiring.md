# 2026-08-14-2304-2-rc-mr1-r1-26-qa-spc-evaluate-wiring RC-R1.26 — quality SPC 自动调度链接线（MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-14
> Mission: requirement-compliance
> Work Item: RC-R1.26（P1-RC-042 quality UC-QA-09 spc-sampling.batch.xml 漏调 spcRuleEngine.evaluate → 自动 NCR/CAPA 级联断裂）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.26 行 + `docs/audits/arm-index.md` P1-RC-042 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（纯调度接线：batch.xml 追加 evaluate 调用）
> Related: `docs/design/quality/use-cases.md`（L1 UC-QA-09 AC-4/5）；`docs/design/quality/spc.md`（§关键流程）；`docs/audits/2026-08-07-2359-rc-ma4-a4-2-101-112-quality-f1-f2-f3-runtime.md`（A4.2.110 运行时证据）；`docs/plans/2026-08-14-1815-2-rc-mr1-r1-23-crm-lead-scoring-schedule-wiring.md`（batch-task 接线范式参照）
> Audit: required

## Current Baseline

- **finding P1-RC-042（arm-index 行，UC-QA-09 自动调度链断裂）**：L1（`use-cases.md:155-157`）逐字「检查 violatedRules → 子组.isOutOfControl == true → 事件驱动创建 NCR(sourceType=SPC)」——"事件驱动"隐含自动链路。L3 实仓：`spc-sampling.batch.xml:23-24` processor 段仅调 `samplingService.collectSamples(item.id, ...)` + `controlLimitCalculator.recalculate(item.id)`，**未调 `spcRuleEngine.evaluate(item.id, ...)`** → 生产调度路径（`erp-qa-spc-sampling.job.yaml` cron `0 0 * * * ?` → nopBatchTaskRunner → batch.xml）**永不触达**规则评估 → 失控样本不会自动 `isOutOfControl=true` → 不触发 `SpcOutOfControlHandler.cascadeNcrAndCapa` afterCommit → NCR/CAPA 自动级联断裂。规则评估仅经手动 @BizMutation `ErpQaSpcChartBizModel.evaluateRules:63-65`（→ `ErpQaSpcChartEvaluateRulesProcessor`）可达。A4.2.110 运行时证实同型。§2 P1①+P1②。**非 P0**（失控样本不建 NCR 仅告警延迟；质检主路径 + 手动闭环完整；非会计过账破坏）。
- **实仓（HEAD 核查）**：
  - `spc-sampling.batch.xml`（27 行）：loader = `ErpQaSpcChart isActive=true`；processor source `:20-25` = `inject('spcSamplingService').collectSamples(...)` + `inject('spcControlLimitCalculator').recalculate(...)`（**漏 evaluate**）。`transactionScope="chunk"`。
  - `erp-qa-spc-sampling.job.yaml`（app-erp-all job conf）：已注册（jobName=erp-qa-spc-sampling，enabled `@cfg:nop.job.erp-qa-spc-sampling.enabled|false`，cronExpr `@cfg:nop.job.erp-qa-spc-sampling.cron-expr|0 0 * * * ?`，invoker=nopBatchTaskRunner + taskPath）——**接线存在，仅 batch.xml processor 漏调 evaluate**。
  - `SpcRuleEngine.evaluate(Long chartId, IServiceContext context)`（`spc/SpcRuleEngine.java:82-133`）：控制限缺失返回 0 / ruleSet 空返回 0 / 无样本返回 0 / 回写 violatedRules + isOutOfControl + 失控样本经 `outOfControlHandler.cascadeNcrAndCapa(chart, s, violated, context)`（afterCommit 模式 B post-commit 建 NCR+CAPA，config-gated `erp-qa.spc-auto-ncr-enabled` 默认 true + 幂等双重预检）。
  - **⚠ 测试确定性关键事实（M1 裁决依据）**：`SpcControlLimitCalculator.recalculate` 在样本数 ≥ 20 时按 `AUTO_FROM_DATA` clCenterType **重写 cl/ucl/lcl**（`SpcControlLimitCalculator.java:159-161`），而既有测试均 seed `AUTO_FROM_DATA`（`TestErpQaSpcOutOfControl:188`、`TestErpQaSpcSampling:172`）→ **批任务测试若 seed ≥ 20 样本，recalculate 会从数据（含失控点）重算控制限，使「均值超 UCL → 规则 1」断言失稳**。确定性配方：seed **< 20 样本** + 显式 cl/ucl/lcl（recalculate 对 <20 样本 no-op——`TestErpQaSpcSampling.recalculateKeepsPendingWhenLessThan20` 实证）+ `parameterId` 非空（否则 `collectSamples` 抛 `ERR_QA_SPC_PARAMETER_NOT_FOUND`，`SpcSamplingService.java:117-121`）。此配方固化进 Phase 3 测试项 ①。
  - Bean 注册：`app-service.beans.xml:63-64` `id="app.erp.qa.service.spc.SpcRuleEngine"`；batch source 用 `inject('spcRuleEngine')`（同 `inject('spcSamplingService')` 简单名注入范式——**Explore 项**：核实简单名注入解析；先例 = R1.23 已执行 `inject('erpCrmLeadScoringRecalcHelper')`（bean id 为全限定类名，`module-crm/.../app-service.beans.xml:62-63`）成功）。
  - `batchChunkCtx.serviceContext` 变量在 processor source 可用性：同型 spc-capability.batch.xml 已用（`biz.calculateCapability(item.id, periodFrom, periodTo, batchChunkCtx.serviceContext)`）；`SpcRuleEngine.evaluate` 的 context 仅透传给 afterCommit 回调（`cascadeNcrAndCapa:83` 注册 afterCommit 时 context 不参与事务，`createNcrAndAction` 体**不读 context**）→ null 兜底可安全接受——**Decision 项**（对齐 R1.23 发现的 batch 运行时 ctx null 缺陷：`BatchTaskRunner` 下 `batchChunkCtx.serviceContext` 可能为 null；本行 evaluate 路径 context 不实际消费，null 安全，无需 helper 包装）。
  - 测试基线：`TestErpQaSpcOutOfControl.java`（afterCommit 时序绕过——直接同步调 handler 内建单逻辑 `:87-89`，设计决策 javadoc `:46-48`；快照 `_cases/`）；`TestErpQaSpcSampling`/`TestSpcRuleEnginePure`/`TestErpQaSpcCapability` 等。
  - **job 层测试范式**：R1.23 已建 `TestErpCrmLeadScoringRecalcJob`（`IBatchTaskRunner.execute(taskPath)` 批任务级执行 + nop-batch-dsl test-scope 依赖 erp-crm-service pom:81-87）；erp-qa-service pom 无 nop-batch-dsl 依赖——**须补 test-scope 依赖**（镜像 R1.23）。
  - **job-scheduling.md 现状**：无 `erp-qa-spc-sampling` 行（作业在 2026-07-18 迁移后未登记）；§3.x quality 段存在 stale 行 `erp-qa-spc-sample-aggregation`（:206-207，DESIGN/待实现，Java Job bean 命名）——**本行 Phase 4 增补 spc-sampling 行 + stale 行标注 superseded**（对齐 R1.23 job-scheduling.md 同步义务）。
- **预授权判据**（第一批纯预授权）：纯调度接线（batch.xml processor 追加 evaluate 调用）+ 测试 + pom test 依赖，**不触 ORM 结构/会计过账/删除**；roadmap RC-R1.26 行 `todo`，Deps（R1.0 done）已满足；`spcRuleEngine` 为既有 Bean（`app-service.beans.xml:63`）注入复用，零新 Bean。
- **涉及文件**：`module-quality/erp-qa-service/src/main/resources/_vfs/nop/batch-task/qa/spc-sampling.batch.xml`；`module-quality/erp-qa-service/pom.xml`（nop-batch-dsl test-scope）；测试新增 `TestErpQaSpcSamplingEvaluateBatch.java`；`docs/design/quality/spc.md`（接线注记）；`docs/architecture/job-scheduling.md`（§3.x qa 行：增补 spc-sampling 行 + stale 行 superseded 标注）。

## Goals

- **自动调度链闭合（P1-RC-042 核心）**：`spc-sampling.batch.xml` processor 段追加 `const ruleEngine = inject('spcRuleEngine'); ruleEngine.evaluate(item.id, batchChunkCtx.serviceContext);`（collectSamples → recalculate → evaluate 顺序）→ 生产调度路径失控样本自动 `isOutOfControl=true` + afterCommit 自动建 NCR(sourceType=SPC) + CAPA Action。
- **evaluate 幂等与失败隔离**：evaluate 内部既有守卫（控制限缺失/ruleSet 空/无样本返回 0 + per-sample try/catch WARN `:125-130`）保持；批任务级失败不阻断 chunk（既有 `cascadeNcrAndCapa` try/catch）。
- **端到端测试**：新增批任务级测试（镜像 R1.23 `TestErpCrmLeadScoringRecalcJob` 范式）——`IBatchTaskRunner.execute(taskPath)` 真实执行 loader+processor 全链，断言失控样本 isOutOfControl=true + NCR/CAPA 创建（afterCommit 时序经批任务提交触发——**Proof 项**：验证 afterCommit 在批任务测试中触发的机制，参照 TestErpQaSpcOutOfControl 设计决策注记）+ 手动 evaluateRules 路径回归。
- **零回归**：既有 quality 测试全绿（`TestErpQaSpcOutOfControl` 等）+ 全仓构建。
- **owner doc 收敛注记**：`spc.md` 接线注记（batch → collectSamples → recalculate → evaluate → afterCommit NCR 全链）；不修改需求契约段（use-cases L1 不动）。
- **回填**：arm-index P1-RC-042 → `done (RC-R1.26)` + roadmap 行 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不实现 P2-RC-044**（CAPA 1:1 非 per-ruleSet + actionType 硬编码 + severity LOW 死分支，独立 P2 watch-only，非本行义务）。
- **不实现 P2-RC-045**（QualityGoal 名称约定回写 + RiskRegister 硬编码值，独立 P2）。
- **不触 ORM 结构**（零列/零索引——NCR 关联列已存在）。
- **不改真相源契约段落**（use-cases L1 不动）。
- **不改 SpcRuleEngine 规则逻辑**（纯函数规则 1-4 保持，本行仅接调度触发面）。
- **不重写 batch 框架机制**（nopBatchTaskRunner + batch.xml 模式保持——2026-07-18 迁移确立的既有标准）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权调度接线修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/quality/use-cases.md`（L1 UC-QA-09）+ `docs/design/quality/spc.md`（§关键流程）+ `docs/audits/2026-08-07-2359-rc-ma4-a4-2-101-112-quality-f1-f2-f3-runtime.md`（A4.2.110 运行时证据）
- Skill Selection Basis: 实现面 = batch.xml 接线 + Bean 注入（`nop-backend-dev`：batch-task 范式、inject 解析、afterCommit 时序）；测试（`nop-testing`：JunitAutoTestCase + IBatchTaskRunner 批任务级测试范式）。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- 无新 config key（`erp-qa.spc-auto-ncr-enabled` 既有默认 true 保持；job enabled 默认 false 保持——部署启用决策，对齐 A4.2.104 config-gate 范式）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-quality/erp-qa-service`。

## Execution Plan

### Phase 1 - Explore 接线语义（Decision）

Status: completed
Targets: `spc-sampling.batch.xml`；`SpcRuleEngine.java`；`SpcOutOfControlHandler.java`
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [x] `Decision` **evaluate 调用位置**：**选项 A（采用）** = collectSamples → recalculate → evaluate 同一 processor source 顺序调用。**证据**：①`SpcRuleEngine.evaluate:89-92` 当 `chart.ucl/lcl/cl` 任一 null 直接返回 0（控制限未计算无规则可评估）→ evaluate 必须在 recalculate 之后（recalculate 先算/保留控制限）；②`SpcControlLimitCalculator.recalculate:101-104` 样本 <20 时 no-op（保留显式 cl/ucl/lcl），≥20 时按 clCenterType 重算——evaluate 恒读取 chart 当前控制限，顺序依赖成立；③选项 B（evaluate 独立阶段/独立批任务）= 多一次 chart 加载 + 状态跨任务传递复杂化，弃。**残余风险**：无（同一 chunk 事务内顺序调用，样本写入即时可见——同 session flush）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **batchChunkCtx.serviceContext null 兜底**：**选项 A（采用）** = 直接透传 `batchChunkCtx.serviceContext`（可能 null），**无需 helper 包装**。**证据**：①R1.23 实测 + 本次复核 `BatchTaskRunner.executeAsync:38` → `batchTaskManager.newBatchTaskContext()` → `BatchTaskManagerImpl:86` `BatchTaskContextImpl(svcCtx=null, scope=null)` → `serviceContext==null`（nop-batch 执行路径无绑定上下文）；②`SpcRuleEngine.evaluate` 的 context 参数全函数体零读取（仅 `:125` 透传 `cascadeNcrAndCapa`）；③`SpcOutOfControlHandler.cascadeNcrAndCapa:83` context 仅传入 afterCommit 回调闭包，`createNcrAndAction:93-124` 体**不读 context**（逐行核实零引用）→ null 全程安全；④`evaluate` 内部无 `context.getEvalScope()` 类调用（对比 R1.23 的 IBiz 代理调用需要 ctx——本行不涉）。**残余风险**：无。
      - Skill: `nop-backend-dev`
- [x] `Proof` **inject('spcRuleEngine') 简单名解析**：**简单名解析不成立 → 采用 plan 预案改全限定 Bean id（FQCN）**。**证据**：①`inject(name)` 为 XLang 全局函数（nop-xlang `GlobalFunctions.inject` 反编译：`scope.getBeanProvider().getBean(name)`）；②`BeanContainerImpl.getBean(name)`（nop-ioc 源码）仅按 **bean id 精确匹配** `enabledBeans`（+parent+alias normalizeAlias），无简单类名回落；③R1.23 先例 `inject('erpCrmLeadScoringRecalcHelper')` 成功是因为该 bean **id 本身即简单名**（`module-crm/.../app-service.beans.xml:62` `<bean id="erpCrmLeadScoringRecalcHelper">`），非简单名解析机制；④实测（Phase 3 首跑）：batch 任务下 `inject('spcSamplingService')`（bean id=`app.erp.qa.service.spc.SpcSamplingService`）抛 `ERR_IOC_UNKNOWN_BEAN_FOR_NAME`——**既有生产 batch.xml 简单名注入同样会在运行时失败**（job 默认 enabled=false 从未执行过，故未暴露——与 P1-RC-042「自动链断裂」同根因加深）；⑤**修复**：batch.xml processor 三段 inject 全部改 **FQCN bean id**（`app.erp.qa.service.spc.SpcSamplingService`/`...SpcControlLimitCalculator`/`...SpcRuleEngine`，均注册于 `app-service.beans.xml:57-64`），实测 4/4 测试全绿。**残余风险**：`spc-capability.batch.xml` 同型 `inject('IErpQaSpcCapabilityBiz')` 简单名注入同样不成立（bean id=`biz_ErpQaSpcCapability`）——同一 latent 缺陷，非本行范围（该 job 亦默认 disabled），Phase 4 日志登记 watch-only。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 三项 Explore 证据落盘（evaluate 位置 A 采用/ctx null 兜底 A 采用/简单名注入解析**不成立 → 预案改 FQCN bean id**）；接线方案确定（collectSamples → recalculate → evaluate 顺序 + 直接透传 ctx + **FQCN bean id inject**）

### Phase 2 - batch.xml 接线落地（P1-RC-042 核心）

Status: completed
Targets: `spc-sampling.batch.xml`
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1 完成

- [x] `Fix` `spc-sampling.batch.xml` processor source 追加 `const ruleEngine = inject('spcRuleEngine'); ruleEngine.evaluate(item.id, batchChunkCtx.serviceContext);`（collectSamples → recalculate → evaluate 顺序，按 Phase 1 裁决）。**落地**：processor source 现为 `const samplingService = inject('spcSamplingService'); const controlLimitCalculator = inject('spcControlLimitCalculator'); const ruleEngine = inject('spcRuleEngine'); samplingService.collectSamples(item.id, batchChunkCtx.serviceContext); controlLimitCalculator.recalculate(item.id); ruleEngine.evaluate(item.id, batchChunkCtx.serviceContext);`（三段顺序调用）。`xmllint --noout` 通过。
      - Skill: `nop-backend-dev`
- [x] `Fix` `erp-qa-service/pom.xml` 补 nop-batch-dsl test-scope 依赖（镜像 R1.23 erp-crm-service pom:81-87 注释+依赖形态）。**落地**：nop-autotest-junit 后追加 `nop-batch-dsl` test-scope（注释注明 plan 2026-08-14-2304-2 Phase 3 用途 + IBatchTaskRunner 注入）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `erp-qa-spc-sampling.job.yaml` description 同步（:4 现为「collectSamples + recalculate」→ 补 evaluate 步骤描述，保持 job 注册表与接线一致）。**落地**：description 改为「…逐图 collectSamples + recalculate + evaluate（失控样本自动 isOutOfControl=true + post-commit 建 NCR(sourceType=SPC)/CAPA，config-gated erp-qa.spc-auto-ncr-enabled 默认 true）」。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] batch.xml processor 三段调用链完整（collectSamples + recalculate + evaluate）且 XML 可解析
- [x] pom 依赖落地且 `mvn install -DskipTests` 分域编译通过（`-pl module-quality/erp-qa-service -am` BUILD SUCCESS）

### Phase 3 - 端到端测试矩阵

Status: completed
Targets: `module-quality/erp-qa-service/src/test/java/app/erp/qa/service/spc/`（新增 `TestErpQaSpcSamplingEvaluateBatch.java`）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 完成

- [x] `Add` 批任务级测试（`IBatchTaskRunner.execute(taskPath)` 真实执行）：**seed 确定性配方（M1 裁决固化）**——控制图 seed **< 20 样本**（recalculate no-op，防控制限被数据重算覆盖）+ **显式 cl/ucl/lcl**（非 AUTO_FROM_DATA 依赖）+ **parameterId 非空**（防 `ERR_QA_SPC_PARAMETER_NOT_FOUND`）+ **不 seed 匹配该 parameterId 的 APPROVED 检验行**（防批内 collectSamples 追加样本致总量 ≥20 触发重算失稳——镜像 `TestErpQaSpcSampling:211` 反例）+ 含「均值超 UCL → 规则 1」失控样本 → batch 执行 → 断言：① 样本 isOutOfControl=true + violatedRules 非空；② NCR(sourceType=SPC) + CAPA Action 创建（afterCommit 触发机制经批任务 chunk 提交证实——**Proof 项：JunitAutoTestCase 下 afterCommit 经 batch chunk 事务提交正常触发**（AbstractTransaction.commit → afterCommit() → onAfterCommit listener 同步执行，实测 NCR/CAPA 落库断言通过 + 快照 CSV 佐证：`output/tables/erp_qa_non_conformance.csv` 行 SOURCE_TYPE=SPC/SOURCE_CODE=CHART-BATCH-OOC#1/SEVERITY=HIGH/STATUS=OPEN + `erp_qa_action.csv` 行 NCR_ID=1/ACTION_TYPE=CAPA/STATUS=PENDING），无需降级 handler 同步路径；③ 无失控样本 → 零 NCR 零 CAPA；④ config `erp-qa.spc-auto-ncr-enabled=false` → 仅标记不建 NCR；⑤ 幂等：同 chart 二次执行不重复 NCR（`findExistingSpcNcr` 预检 + `cascadeNcrAndCapa` afterCommit 幂等）。**实施中发现并修复接线缺陷（Phase 1 Proof 深化）**：既有 batch.xml 简单名 inject（`inject('spcSamplingService')` 等）运行时解析失败（ERR_IOC_UNKNOWN_BEAN_FOR_NAME）——改 FQCN bean id 后 4/4 全绿。**快照**：4 方法 RECORDING→CHECKING 全绿；`nop_batch_task` 输出表（batch 状态存储 UUID taskKey 非确定性）从快照剔除（checker 仅校验已录制文件，R1.23 CRM job 测试同型处理——其 batch 无 saveState 故未暴露）。
      - Skill: `nop-testing`
- [x] `Proof` 既有 `TestErpQaSpcOutOfControl`/`TestErpQaSpcSampling`/`TestSpcRuleEnginePure`/`TestErpQaSpcCapability` 零回归：`mvn test -pl module-quality/erp-qa-service`（BUILD SUCCESS，全量 78 tests 全绿）+ `_cases/` 快照录制（既有快照 CHECKING 比对通过）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新增批任务级测试全绿 + 既有 quality 测试零回归（`mvn test -pl module-quality/erp-qa-service` BUILD SUCCESS，全量测试 0 失败）
- [x] 自动链路（batch→evaluate→isOutOfControl→NCR/CAPA）有运行时断言证据（非仅静态接线——4 测试 + 快照 CSV 双重证据，afterCommit 经 chunk 提交实测触发）

### Phase 4 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/design/quality/spc.md`；`docs/architecture/job-scheduling.md`；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-14.md`
Skill: none

- Item Types: `Add | Fix`
- Prereqs: Phase 1-3 完成

- [x] `Add` owner doc 注记：`spc.md` §关键流程 3 补 batch 接线注记（collectSamples → recalculate → evaluate → afterCommit NCR 全链 + config-gated 语义 + <20 样本 seed 配方说明 + FQCN inject 语义）；**不修改需求契约段**（use-cases L1 未动）。
      - Skill: none
- [x] `Fix` `job-scheduling.md` §3.12 quality 段：**增补 `erp-qa-spc-sampling` 行**（SCHEDULED + job.yaml 路径 + config 键 `nop.job.erp-qa-spc-sampling.enabled/cron-expr` + `erp-qa.spc-auto-ncr-enabled` + 证据链接）；stale 行 `erp-qa-spc-sample-aggregation`（:206-207 + §7 :343 两处）**标注 superseded**（由 erp-qa-spc-sampling 取代）。
      - Skill: none
- [x] `Add` arm-index P1-RC-042 → `done (RC-R1.26)` + 修复落地摘要（三段接线 + inject FQCN 修正 + 测试证据）；roadmap RC-R1.26 → done ✅（含落地摘要）；`docs/logs/2026/08-14.md` 日志条目（含 watch-only 登记 spc-capability.batch.xml 同型 latent 缺陷）。
      - Skill: none

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + owner doc 注记落盘；日志条目写入

## Draft Review Record

- Independent draft review iteration 1: acceptable with 1 MAJOR + MINORs（独立子代理 ses_fff249896ffeyJDkA41SMH8bUs）— 0 BLOCKER；1 MAJOR（M1：Phase 3 测试① 确定性不足——recalculate 在 ≥20 样本时按 AUTO_FROM_DATA 重写 cl/ucl/lcl 使失控断言失稳 → 已修正：seed 配方固化 <20 样本 + 显式 cl/ucl/lcl + parameterId 非空 + afterCommit 断言双路径失败模式明确化）；MINORs 已修正：行号漂移（evaluate :82-133 / try-catch :124-129 / BizModel :62-64 / pom :81-87 / javadoc :46-48）、「如需」hedge 移除（job-scheduling.md 增补改为显式 Fix 项）、Phase 3 目标钉死新类名、job.yaml description 同步、inject 先例记录（R1.23 已执行同型）。无 Blocker，共识达成，可转 active。
- Independent draft review iteration 2: `accept`（独立子代理 ses_fff1af439ffeX04DCmw3PykBy1）— 0 BLOCKER / 0 MAJOR。全部 iteration-1 项确认解决（M1 seed 配方 <20 样本 + 显式 cl/ucl/lcl + parameterId 非空实证 `SpcControlLimitCalculator.java:101-104,159-161` + `ErpQaConstants.java:178` SPC_MIN_SUBGROUPS_FOR_CONTROL_LIMIT=20 + `SpcSamplingService.java:117-121`；行号漂移全确认；「如需」hedge 移除为显式 Phase 4 Fix；新类名钉死；afterCommit 双路径失败模式具名）。2 个新 MINOR 已顺手修订：① seed 配方补「不 seed 匹配 parameterId 的 APPROVED 检验行」（防批内 collectSamples 追加样本致 ≥20 重算失稳，镜像 `TestErpQaSpcSampling:211` 反例）；② `erp-qa-spc-sampling.job.yaml` description 同步补为显式 Phase 2 Fix 项（:4 现为「collectSamples + recalculate」）。**计划可标记 active。**

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。**完整仓库验证在此处**：结束时运行一次全量验证。

- [x] 范围内行为完成
- [x] 相关文档对齐
- [x] 已运行验证（`mvn test -pl module-quality/erp-qa-service` 172 tests 全绿 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline 零漂移）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P2-RC-044 CAPA 1:1 非 per-ruleSet

- Classification: `watch-only residual`
- Why Not Blocking Closure: 独立 P2 finding（`createNcrAndAction:116-123` 1 NCR 1 CAPA 1:1 非 per-ruleSet + actionType 硬编码 + SEVERITY_LOW 死分支），登记 watch-only 不随本行落地（本行 = 自动调度链接线，CAPA 结构 = 独立控制点）；P2 登记不强制。
- Successor Required: `no`

## Closure

Status Note: 执行完成并独立结束审计通过（2026-08-15）。四 Phase 全绿：Phase 1 Explore 三证据落盘（evaluate 位置 A 采用 / ctx null 兜底 A 采用 / **简单名注入解析不成立 → 预案改 FQCN bean id**——`BeanContainerImpl.getBean` 仅按 id 精确匹配，R1.23 先例成功系因 bean id 本身即简单名）；Phase 2 接线落地（batch.xml processor 三段 collectSamples → recalculate → evaluate + FQCN inject + pom nop-batch-dsl test-scope + job.yaml description 同步）；Phase 3 `TestErpQaSpcSamplingEvaluateBatch` 4 组 batch 任务级测试全绿（失控标记+NCR/CAPA afterCommit 经 chunk 提交实测触发 / 受控零 NCR / config 关闭仅标记 / 幂等二次不重复）+ 快照录制；Phase 4 文档回填（spc.md 注记 / job-scheduling.md §3.12 增补+superseded / arm-index done (RC-R1.26) / roadmap done ✅ / 日志）。验证：erp-qa-service 172 tests 全绿 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + compliance checker 零漂移。watch-only 登记：spc-capability.batch.xml 同型简单名 inject latent 缺陷（job 默认 disabled 未暴露，非本行范围）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话 ses_ffe783814ffeDzpIvM26XExbbL，只读，零文件修改）
- Evidence: **Verdict PASS**——①计划状态一致性（4/4 Phase completed + 全 `[x]` + 草案审查迭代 2 accept 记录）；②接线实仓核验（batch.xml:21-26 三段调用 + FQCN inject + beans.xml:57-64 FQCN 注册 + pom nop-batch-dsl test-scope + job.yaml description + xmllint 通过）；③测试实仓核验（4 测试覆盖 5 断言面 + `_cases` 快照含 NCR SOURCE_TYPE=SPC/SEVERITY=HIGH/STATUS=OPEN + CAPA NCR_ID=1/PENDING + sample VIOLATED_RULES=1/IS_OUT_OF_CONTROL=true + 幂等输出恰 1 NCR 行 + nop_batch_task 正确剔除）；④审计者自跑验证（`mvn test -pl module-quality/erp-qa-service -Dtest=TestErpQaSpcSamplingEvaluateBatch` 4/4 绿 CHECKING 模式 + checker EXIT=0 全 13 规则 actual ≤ baseline + git status 仅预期文件）；⑤文档回填核验（arm-index:214 done (RC-R1.26) / roadmap:418 done ✅ / spc.md:96 注记 / job-scheduling.md:206-207,344 增补+superseded / logs 条目）；⑥范围守卫（零 ORM/会计/删除变更）。0 P0 / 0 P1；2 项 P2 非阻塞（spc-capability 同型 latent 缺陷 watch-only 登记 + batchChunkCtx.serviceContext null 已证 null 安全）均在 plan 内预登记。

Follow-up:

- 无范围外 follow-up。watch-only 记录：`spc-capability.batch.xml` `inject('IErpQaSpcCapabilityBiz')` 同型简单名注入 latent 缺陷（bean id=`biz_ErpQaSpcCapability` 精确匹配失败；job 默认 disabled 未暴露）——后续 batch job 接线/启用行可一并改 FQCN bean id。MR1 第一批后续 RC-R1.27+（projects pnl-calc 调度等）由 mission driver 继续。
