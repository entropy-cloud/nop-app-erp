# 2026-08-14-2304-2-rc-mr1-r1-26-qa-spc-evaluate-wiring RC-R1.26 — quality SPC 自动调度链接线（MR1 第一批纯预授权）

> Plan Status: active
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

Status: planned
Targets: `spc-sampling.batch.xml`；`SpcRuleEngine.java`；`SpcOutOfControlHandler.java`
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [ ] `Decision` **evaluate 调用位置**：选项 A（推荐）= collectSamples → recalculate → evaluate（evaluate 依赖 recalculate 后的控制限 UCL/LCL/CL——`evaluate:88-92` 控制限缺失返回 0；recalculate 先算限）；选项 B = evaluate 独立阶段/独立批任务（多一次加载，弃）。记录理由。
      - Skill: `nop-backend-dev`
- [ ] `Decision` **batchChunkCtx.serviceContext null 兜底**：`inject('spcRuleEngine').evaluate(item.id, batchChunkCtx.serviceContext)`——对齐 R1.23 实测（BatchTaskRunner 下 serviceContext 可能为 null）；SpcRuleEngine.evaluate 的 context 仅透传 afterCommit 回调不实际消费 → null 安全，无需 helper 包装（记录证据）；若 Explore 证实 context 实际消费则改 helper 兜底 ServiceContextImpl（R1.23 同型）。记录理由。
      - Skill: `nop-backend-dev`
- [ ] `Proof` **inject('spcRuleEngine') 简单名解析**：核实 batch source 上下文 `inject` 按简单名解析 Bean（`app.erp.qa.service.spc.SpcRuleEngine` 注册于 `app-service.beans.xml:63-64`，同型 `inject('spcSamplingService')`/`inject('spcControlLimitCalculator')` 已运行）；若简单名解析不成立则改全限定 Bean id（记录证据）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 三项 Explore 证据落盘（evaluate 位置/ctx null 兜底/简单名注入解析）；接线方案确定

### Phase 2 - batch.xml 接线落地（P1-RC-042 核心）

Status: planned
Targets: `spc-sampling.batch.xml`
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1 完成

- [ ] `Fix` `spc-sampling.batch.xml` processor source 追加 `const ruleEngine = inject('spcRuleEngine'); ruleEngine.evaluate(item.id, batchChunkCtx.serviceContext);`（collectSamples → recalculate → evaluate 顺序，按 Phase 1 裁决）。
      - Skill: `nop-backend-dev`
- [ ] `Fix` `erp-qa-service/pom.xml` 补 nop-batch-dsl test-scope 依赖（镜像 R1.23 erp-crm-service pom:81-87 注释+依赖形态）。
      - Skill: `nop-backend-dev`
- [ ] `Fix` `erp-qa-spc-sampling.job.yaml` description 同步（:4 现为「collectSamples + recalculate」→ 补 evaluate 步骤描述，保持 job 注册表与接线一致）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] batch.xml processor 三段调用链完整（collectSamples + recalculate + evaluate）且 XML 可解析
- [ ] pom 依赖落地且 `mvn install -DskipTests` 分域编译通过

### Phase 3 - 端到端测试矩阵

Status: planned
Targets: `module-quality/erp-qa-service/src/test/java/app/erp/qa/service/spc/`（新增 `TestErpQaSpcSamplingEvaluateBatch.java`）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 完成

- [ ] `Add` 批任务级测试（`IBatchTaskRunner.execute(taskPath)` 真实执行）：**seed 确定性配方（M1 裁决固化）**——控制图 seed **< 20 样本**（recalculate no-op，防控制限被数据重算覆盖）+ **显式 cl/ucl/lcl**（非 AUTO_FROM_DATA 依赖）+ **parameterId 非空**（防 `ERR_QA_SPC_PARAMETER_NOT_FOUND`）+ **不 seed 匹配该 parameterId 的 APPROVED 检验行**（防批内 collectSamples 追加样本致总量 ≥20 触发重算失稳——镜像 `TestErpQaSpcSampling:211` 反例）+ 含「均值超 UCL → 规则 1」失控样本 → batch 执行 → 断言：① 样本 isOutOfControl=true + violatedRules 非空；② NCR(sourceType=SPC) + CAPA Action 创建（afterCommit 触发机制经批任务 chunk 提交证实——**Proof 项**：若 JunitAutoTestCase 下 afterCommit 未在批任务内触发，则 NCR/CAPA 证据经 handler 同步路径测试保留（镜像 TestErpQaSpcOutOfControl :87-89 设计决策），批任务级断言聚焦 isOutOfControl/violatedRules 确定性面，两路径均给出明确失败模式）；③ 无失控样本 → 零 NCR 零 CAPA；④ config `erp-qa.spc-auto-ncr-enabled=false` → 仅标记不建 NCR；⑤ 幂等：同 chart 二次执行不重复 NCR。
      - Skill: `nop-testing`
- [ ] `Proof` 既有 `TestErpQaSpcOutOfControl`/`TestErpQaSpcSampling`/`TestSpcRuleEnginePure`/`TestErpQaSpcCapability` 零回归：`mvn test -pl module-quality/erp-qa-service`（BUILD SUCCESS）+ `_cases/` 快照录制。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 新增批任务级测试全绿 + 既有 quality 测试零回归（`mvn test -pl module-quality/erp-qa-service` BUILD SUCCESS）
- [ ] 自动链路（batch→evaluate→isOutOfControl→NCR/CAPA）有运行时断言证据（非仅静态接线）

### Phase 4 - 文档回填 + arm-index/roadmap 状态

Status: planned
Targets: `docs/design/quality/spc.md`；`docs/architecture/job-scheduling.md`；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-14.md`
Skill: none

- Item Types: `Add | Fix`
- Prereqs: Phase 1-3 完成

- [ ] `Add` owner doc 注记：`spc.md` §关键流程补 batch 接线注记（collectSamples → recalculate → evaluate → afterCommit NCR 全链 + config-gated 语义 + <20 样本 seed 配方说明）；不修改需求契约段。
      - Skill: none
- [ ] `Fix` `job-scheduling.md` §3.x quality 段：增补 `erp-qa-spc-sampling` 行（SCHEDULED + job.yaml 路径 + config 键 + 证据链接）+ stale 行 `erp-qa-spc-sample-aggregation`（:206-207）标注 superseded。
      - Skill: none
- [ ] `Add` arm-index P1-RC-042 → `done (RC-R1.26)` + 修复落地摘要；roadmap RC-R1.26 → done；`docs/logs/2026/08-14.md` 日志条目。
      - Skill: none

Exit Criteria:

- [ ] arm-index/roadmap 状态回填 + owner doc 注记落盘；日志条目写入

## Draft Review Record

- Independent draft review iteration 1: acceptable with 1 MAJOR + MINORs（独立子代理 ses_fff249896ffeyJDkA41SMH8bUs）— 0 BLOCKER；1 MAJOR（M1：Phase 3 测试① 确定性不足——recalculate 在 ≥20 样本时按 AUTO_FROM_DATA 重写 cl/ucl/lcl 使失控断言失稳 → 已修正：seed 配方固化 <20 样本 + 显式 cl/ucl/lcl + parameterId 非空 + afterCommit 断言双路径失败模式明确化）；MINORs 已修正：行号漂移（evaluate :82-133 / try-catch :124-129 / BizModel :62-64 / pom :81-87 / javadoc :46-48）、「如需」hedge 移除（job-scheduling.md 增补改为显式 Fix 项）、Phase 3 目标钉死新类名、job.yaml description 同步、inject 先例记录（R1.23 已执行同型）。无 Blocker，共识达成，可转 active。
- Independent draft review iteration 2: `accept`（独立子代理 ses_fff1af439ffeX04DCmw3PykBy1）— 0 BLOCKER / 0 MAJOR。全部 iteration-1 项确认解决（M1 seed 配方 <20 样本 + 显式 cl/ucl/lcl + parameterId 非空实证 `SpcControlLimitCalculator.java:101-104,159-161` + `ErpQaConstants.java:178` SPC_MIN_SUBGROUPS_FOR_CONTROL_LIMIT=20 + `SpcSamplingService.java:117-121`；行号漂移全确认；「如需」hedge 移除为显式 Phase 4 Fix；新类名钉死；afterCommit 双路径失败模式具名）。2 个新 MINOR 已顺手修订：① seed 配方补「不 seed 匹配 parameterId 的 APPROVED 检验行」（防批内 collectSamples 追加样本致 ≥20 重算失稳，镜像 `TestErpQaSpcSampling:211` 反例）；② `erp-qa-spc-sampling.job.yaml` description 同步补为显式 Phase 2 Fix 项（:4 现为「collectSamples + recalculate」）。**计划可标记 active。**

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。**完整仓库验证在此处**：结束时运行一次全量验证。

- [ ] 范围内行为完成
- [ ] 相关文档对齐
- [ ] 已运行验证（`mvn test -pl module-quality/erp-qa-service` 全绿 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### P2-RC-044 CAPA 1:1 非 per-ruleSet

- Classification: `watch-only residual`
- Why Not Blocking Closure: 独立 P2 finding（`createNcrAndAction:116-123` 1 NCR 1 CAPA 1:1 非 per-ruleSet + actionType 硬编码 + SEVERITY_LOW 死分支），登记 watch-only 不随本行落地（本行 = 自动调度链接线，CAPA 结构 = 独立控制点）；P2 登记不强制。
- Successor Required: `no`

## Closure

Status Note: pending

Closure Audit Evidence:

- Auditor / Agent: <待独立审计>

Follow-up:

- <待定>
