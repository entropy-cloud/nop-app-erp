# 2026-08-14-2304-3-rc-mr1-r1-27-prj-pnl-schedule-wiring RC-R1.27 — projects 损益汇总 nop-job 调度接线（MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-14
> Mission: requirement-compliance
> Work Item: RC-R1.27（P1-RC-053 projects UC-PRJ-06 ① PnL 自动调度接线——config key 已声明但消费缺失 / 接线语义核对）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.27 行 + `docs/audits/arm-index.md` P1-RC-053 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（纯调度接线 + BizModel 调用）
> Related: `docs/design/projects/use-cases.md`（L1 UC-PRJ-06 ①）；`docs/design/projects/profitability.md`（§关键流程 :82）；`docs/audits/2026-08-07-2359-rc-ma4-a4-2-113-123-projects-f1-f2-f3-runtime.md`（A4.2.121 运行时证据）；`docs/plans/2026-08-14-1815-2-rc-mr1-r1-23-crm-lead-scoring-schedule-wiring.md`（**同型范式参照**——batch-task 式接线 + config 键对齐 + 失败隔离 + 测试）；`docs/architecture/job-scheduling.md`（§3.14 Projects 行）
> Audit: required

## Current Baseline

- **finding P1-RC-053（arm-index 行，UC-PRJ-06 ① 损益汇总 nop-job 调度未接线）**：L1（`use-cases.md:102`）逐字「定时任务(nop-job) →」要求 nop-job 自动触发路径。L3 实仓（HEAD 核查）：config key `erp-prj.pnl-calc-cron`（`ErpPrjConstants.java:20`）+ `erp-prj.pnl-auto-calc-enabled`（`ErpPrjConstants.java:22` + `ErpPrjConfigs.java:16` DEFAULT false + `:70-74 pnlAutoCalcEnabled()` 读值 + `:82-84 pnlCalcCron()` 读值）**已声明**；grep 全 `module-projects` `nop-job|IJobInvoker|JobBean|@Scheduled|IScheduler` **零 Job bean 消费**。`IErpPrjProjectPnlBiz.java:20` Javadoc「经 nop-job（erp-prj-pnl-calc）周期触发」为**期望非实现**。A4.2.121 运行时证实同型。§2 P1①（功能实质偏离验收标准——L1 字面"定时任务(nop-job)"自动触发路径零实现，仅手动可达）。
- **⚠ 2026-08-14 HEAD 关键发现（计划起草复核——接线形态与 A4.2.121 审计结论不同）**：**`erp-prj-pnl-calc.job.yaml` 已存在于 app-erp-all job conf**（`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-prj-pnl-calc.job.yaml`，2026-07-18 nop-batch 迁移 commit 940b43184 创建）：jobName=erp-prj-pnl-calc，enabled `@cfg:nop.job.erp-prj-pnl-calc.enabled|false`，cronExpr `@cfg:nop.job.erp-prj-pnl-calc.cron-expr|0 0 1 * * ?`，invoker=nopBatchTaskRunner + taskPath `/nop/batch-task/prj/pnl-calc.batch.xml`；`pnl-calc.batch.xml` 亦存在（loader = `ErpPrjProject status in [DRAFT,OPEN,ON_HOLD]`，processor = `inject('IErpPrjProjectPnlBiz').refreshPnl(item.id, null, null, batchChunkCtx.serviceContext)`）——**接线已以 batch-task 形态存在**（与 RC-R1.23 crm lead-scoring 完全同型：审计按 Java Job bean + scheduler.yaml 判据搜索，未识别 2026-07-18 迁移后的 batch-task 式接线；`scheduler.yaml` 仅 `enabled: true` 空壳）。**本行须先 Explore 现有接线是否运行时可达/正确，再裁决修法**（对齐 RC-R1.23 Phase 1 Explore 范式 + R1.2 bank-recon 已验证 nopBatchTaskRunner 范式）。
- **实仓（HEAD 核查）**：
  - `ErpPrjConfigs.java:70-74`：`pnlAutoCalcEnabled()`/`pnlCalcCron()`（:82-84）声明但**零消费方**（grep 全仓仅 Constants/Configs 自身命中）——`erp-prj.pnl-calc-cron` + `erp-prj.pnl-auto-calc-enabled` 为 **dead config**（job.yaml 实际用 `nop.job.erp-prj-pnl-calc.cron-expr` 而非业务键；`pnlAutoCalcEnabled` 门控未接）。
  - `pnl-calc.batch.xml`：`transactionScope="chunk"`（chunk 级事务，batchSize=100）；processor 裸调 `biz.refreshPnl(...)` 无 try/catch——**无 per-item 失败隔离**（镜像 R1.23 发现的 nop-batch 语义：chunk 事务下任一项失败回滚整 chunk；对照 R1.23 裁决 = REQUIRES_NEW helper 或显式调整声明）。
  - **`batchChunkCtx.serviceContext` null 缺陷（R1.23 已发现，本行同型潜伏）**：R1.23 执行中实测 `BatchTaskRunner` 下 `batchChunkCtx.serviceContext` 为 null，`IErpCrmLeadScoreBiz` 代理调用需非 null ctx → helper 内 `new ServiceContextImpl()` 兜底；**pnl-calc.batch.xml 的 `refreshPnl(item.id, null, null, batchChunkCtx.serviceContext)` 同型潜伏该缺陷**（R1.23 closure Follow-up 已注记「batchChunkCtx.serviceContext null ctx 缺陷同样潜伏于 pnl-calc 等既有 batch job（非本行范围，后续 batch job 接线行可一并处理）」——本行正是该后续接线行，须一并处理）。
  - `IErpPrjProjectPnlBiz` bean 注册：`_service.beans.xml:70-73` `biz_ErpPrjProjectPnl`（`ioc:type="app.erp.prj.biz.IErpPrjProjectPnlBiz"`，BizProxyFactoryBean）——`inject('IErpPrjProjectPnlBiz')` 按 ioc:type 解析（同型 R1.23 `IErpCrmLeadScoreBiz` 已证实）。
  - 测试基线：`TestErpPrjProjectPnl.java`（BizModel 层 refreshPnl 数值强测）+ `TestErpPrjProjectSettlement.java`（调 `pnlBiz.refreshPnl` 直连）——**无 job 层/批任务级测试**（无 TestErpPrjPnlCalcJob）。
  - `job-scheduling.md` §3.14 Projects 行：`erp-prj-pnl-aggregation` 标「（待实现）/ DESIGN / batch-candidate」——**stale 注记**（实际 job 名 `erp-prj-pnl-calc` 已存在，2026-07-18 迁移已落地）；须同步。
  - `TestErpAllJobYamlLoading` 断言 21 个 job.yaml（当前 22 文件含 scheduler.yaml = 21 job.yaml，`erp-prj-pnl-calc.job.yaml` 在列）——本行不改 job 文件数量。
- **预授权判据**（第一批纯预授权）：纯调度接线（job.yaml config 键对齐 + batch.xml/helper 修正 + 测试 + doc 同步），**不触 ORM 结构/会计过账/删除**（refreshPnl 调既有入口，PnL 为管理会计汇总非 GL 凭证）；roadmap RC-R1.27 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-prj-pnl-calc.job.yaml`（config 键对齐）；`module-projects/erp-prj-service/src/main/resources/_vfs/nop/batch-task/prj/pnl-calc.batch.xml`（确认或修正）；`module-projects/erp-prj-service/pom.xml`（nop-batch-dsl test-scope，参照 R1.23）；`ErpPrjConstants.java`/`ErpPrjConfigs.java`（config key 消费，如需）；新增 helper（如裁决 REQUIRES_NEW，镜像 R1.23 `ErpCrmLeadScoringRecalcHelper` + **`module-projects/erp-prj-service/src/main/resources/_vfs/erp/prj/beans/app-service.beans.xml` 或 `_service.beans.xml` 注册**）；`docs/design/projects/profitability.md`（:82 接线注记）；`docs/architecture/job-scheduling.md`（§3.14 :220 + §7 :345 stale 行修正）；`docs/audits/compliance-baseline.md`（R10 基线上调注记，条件 F1=A）；新增测试类 1 个。

## Goals

- **PnL 自动调度路径运行时成立（P1-RC-053 核心）**：确认或修正既有 batch-task 接线（job.yaml + batch.xml）使定时链路**运行时可达且正确**——nop-job 调度 → nopBatchTaskRunner → batch.xml loader（DRAFT/OPEN/ON_HOLD 项目）→ per-item `IErpPrjProjectPnlBiz.refreshPnl` → ProjectPnlCalculator 聚合。
- **config 键对齐（dead → active）**：`erp-prj.pnl-calc-cron`（job.yaml cronExpr 消费，`@cfg:erp-prj.pnl-calc-cron|0 0 1 * * ?` 或等效对齐——**Decision 项**，参照 R1.23 选项 A：job.yaml 消费业务键）+ `erp-prj.pnl-auto-calc-enabled` 门控（job.yaml 或 helper 消费——**Decision 项**：门控语义 = enabled 双层门控 vs 业务键门控，参照 R1.23/RC-R1.4 先例）。
- **null ctx 兜底修复（R1.23 潜伏缺陷收口）**：`batchChunkCtx.serviceContext` null → helper 或 batch.xml 内 `new ServiceContextImpl()` 兜底（对齐 R1.23 执行发现），使批任务运行时 refreshPnl 调用不 NPE。
- **失败隔离语义裁决**：chunk 级事务 vs per-item REQUIRES_NEW helper（镜像 R1.23 F1 裁决——**Decision 项**；L2 `profitability.md:82` 未声明 per-item 隔离，选项 A = REQUIRES_NEW helper / 选项 B = 显式声明 chunk 级隔离）。
- **测试**：新增批任务级测试（镜像 R1.23 `TestErpCrmLeadScoringRecalcJob`：`IBatchTaskRunner.execute(taskPath)`）——断言 cron 门控 + 批任务执行后 ErpPrjProjectPnl 记录生成 + null ctx 兜底路径 + 失败隔离。
- **owner doc 收敛**：`profitability.md:82` 接线注记（job.yaml + batch.xml + refreshPnl 全链 + config 门控语义）；`job-scheduling.md` §3.14 stale 行修正（`erp-prj-pnl-aggregation` → `erp-prj-pnl-calc` + SCHEDULED 状态）；不修改需求契约段（use-cases L1 不动）。
- **零回归**：既有 projects 测试全绿（`TestErpPrjProjectPnl` 等）+ 全仓构建。
- **回填**：arm-index P1-RC-053 → `done (RC-R1.27)` + roadmap 行 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不实现 P2-RC-050 多币种折算**（`ProjectPnlCalculator:105 setExchangeRate(ONE)` 硬编码，独立 P2 finding + P1-MA1-010 协同，非本行范围）。
- **不实现 P1-RC-052 质保金逻辑**（独立 finding，触 Provider/VoucherFact ask-first，非本行范围）。
- **不触 ORM 结构**（零列/零索引——Pnl 表已存在）。
- **不改真相源契约段落**（use-cases L1 不动）。
- **不重写 batch 框架机制**（nopBatchTaskRunner 模式保持——2026-07-18 迁移确立的既有标准，本行对齐而非另起炉灶）。
- **不创建新 job.yaml**（既有 `erp-prj-pnl-calc.job.yaml` 在列，本行修正/确认而非新增——`TestErpAllJobYamlLoading` 21 数保持）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权调度接线修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/projects/use-cases.md`（L1 UC-PRJ-06 ①）+ `docs/design/projects/profitability.md`（§关键流程 :82 + §机制 :95）+ `docs/audits/2026-08-07-2359-rc-ma4-a4-2-113-123-projects-f1-f2-f3-runtime.md`（A4.2.121 运行时证据）
- Skill Selection Basis: 实现面 = job.yaml/batch.xml 接线 + config 门控消费 + null ctx 兜底（`nop-backend-dev`：job 接线范式、config 范式、nop-batch 任务机制、REQUIRES_NEW helper 范式）；测试（`nop-testing`：JunitAutoTestCase + IBatchTaskRunner 批任务级测试范式）。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- config key `erp-prj.pnl-calc-cron`（默认 cron 表达式；空值=禁用语义——**Decision 项**对齐 R1.23 schedule-cron 语义）+ `erp-prj.pnl-auto-calc-enabled`（默认 false）+ `nop.job.erp-prj-pnl-calc.enabled`（默认 false 保持——部署启用决策，对齐 A4.2.121 config-gate 范式）。无需 .env/外部服务。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-projects/erp-prj-service`。

## Execution Plan

### Phase 1 - Explore 既有 batch-task 接线运行时状态（Decision）

Status: completed
Targets: `erp-prj-pnl-calc.job.yaml`；`pnl-calc.batch.xml`；`ErpPrjConstants.java`；`ErpPrjConfigs.java`
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [x] `Decision` **接线形态裁决**：**选项 A（采用）** = 确认/修正既有 batch-task 式接线（job.yaml + batch.xml + nopBatchTaskRunner——2026-07-18 迁移标准模式，参照 R1.23/R1.2 已验证先例）；选项 B（否决） = 新建 Java Job bean `ErpPrjProjectPnlCalcJob`（镜像 ForecastRecalc 范式，弃——重复接线）。**Explore 证据（HEAD 实测）**：①`inject('IErpPrjProjectPnlBiz')` 在 batch source 上下文解析可达——`biz_ErpPrjProjectPnl` bean 注册于 `_service.beans.xml:70-73`（`ioc:type="app.erp.prj.biz.IErpPrjProjectPnlBiz"`，BizProxyFactoryBean），同型 R1.23 `IErpCrmLeadScoreBiz` 已运行证实；②`batchChunkCtx.serviceContext` 变量在 processor source 可用但 **null 缺陷潜伏**（R1.23 实测 `BatchTaskRunner.executeAsync → newBatchTaskContext()` 无绑定上下文，本行须兜底）；③`nopBatchTaskRunner` bean 注册于 nop-batch-dsl `batch-dsl.beans.xml:6`（`io.nop.batch.dsl.runner.BatchTaskRunner`）；④job.yaml 加载链——`TestErpAllJobYamlLoading` 断言 `/nop/job/conf` 下 21 个 `.job.yaml` 经 `JsonTool.loadDeltaBeanFromResource` 反序列化为 `LocalJobConfig`（`erp-prj-pnl-calc` 在列），`@cfg:` 经 `ConfigValueResolver` 解析（enabled=false 默认不注册，部署 opt-in）。→ **选项 A**（最小改动 + 既有模式）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **config 门控键对齐（G1）**：**选项 A（采用）** = job.yaml cronExpr 消费 `erp-prj.pnl-calc-cron`（`@cfg:erp-prj.pnl-calc-cron|0 0 1 * * ?`，常量由 dead 转活跃）+ `erp-prj.pnl-auto-calc-enabled` 门控（helper 消费——关闭=跳过）；选项 B（否决） = 常量改 `nop.job.*` 键（跟随 job.yaml 现状——业务键仍 dead，弃）。**理由（选项 A）**：①`CONFIG_PNL_CALC_CRON`/`CONFIG_PNL_AUTO_CALC_ENABLED` 从 dead 转活跃（grep 消费点 = job.yaml + helper）；②业务 config 键范式对齐 R1.23/R1.4/R1.5（job 层 enabled 门控 + 业务键门控双层）；③`job-scheduling.md:221` stale 行同步为 `erp-prj-pnl-calc` + 配置键。`enabled` 保持 `@cfg:nop.job.erp-prj-pnl-calc.enabled|false`（部署 opt-in，对齐 A4.2.121 config-gate 范式）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **失败隔离语义裁决（F1）**：**选项 A（采用）** = REQUIRES_NEW helper（镜像 R1.23 `ErpCrmLeadScoringRecalcHelper`/R1.2 `ErpFinBankReconAutoReverseHelper` 范式——逐条独立事务 + try/catch WARN + 返回 boolean + null ctx 兜底 ServiceContextImpl）；选项 B（否决） = 保持 chunk 级（batch.xml `transactionScope="chunk"` 现状，任一项失败回滚整 chunk）并显式声明。**Explore 证据**：nop-batch chunk 事务无 per-item 隔离（R1.23 已实测 `BatchTaskBuilder.buildChunkProcessor` + `InvokerBatchConsumer`——process/chunk 均整 chunk 单事务，任一项抛异常回滚整 chunk）；L2 `profitability.md:82` 未声明 per-item 隔离 → 选项 A 更鲁棒（对齐 R1.23 裁决先例 + `ErpPrjErrors.ERR_PROJECT_NOT_REFERENCEABLE` 单项目失败天然可隔离）。
      - Skill: `nop-backend-dev`
- [x] `Proof` **运行时验证前置**：job.yaml 加载链核实——①`TestErpAllJobYamlLoading` 断言 21 个 `.job.yaml` 可解析（`erp-prj-pnl-calc` 在列）；②taskPath `/nop/batch-task/prj/pnl-calc.batch.xml` 文件存在（XML 实解析通过）；③job.yaml YAML 实解析通过（jobName/invoker.bean=nopBatchTaskRunner/taskPath/cronExpr `@cfg:nop.job.erp-prj-pnl-calc.cron-expr|0 0 1 * * ?` 逐字确认）；④`inject('IErpPrjProjectPnlBiz')` 解析可达（`biz_ErpPrjProjectPnl` ioc:type 注册，同型 R1.23 成功先例）。运行时可达性最终由 Phase 3 批任务级测试（`IBatchTaskRunner.execute(taskPath)`）给出。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 接线形态裁决记录（A 采用 + 理由）+ config 键对齐裁决（G1 A 采用）+ 隔离语义裁决（F1 A 采用），Explore 证据落盘计划
- [x] 既有 job.yaml/batch.xml 无语法/引用错误（XML/YAML 实解析通过 + taskPath 命中文件）

### Phase 2 - 接线落地 + config 对齐 + null ctx 兜底（P1-RC-053 核心）

Status: completed
Targets: `erp-prj-pnl-calc.job.yaml`；`pnl-calc.batch.xml`；`ErpPrjConstants.java`；`ErpPrjConfigs.java`（如需）；新增 helper（如 F1=A）
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision`
- Prereqs: Phase 1 完成

- [x] `Fix` 按 Phase 1 裁决落地（选项 A + G1 A + F1 A）：`erp-prj-pnl-calc.job.yaml` cronExpr 改 `@cfg:erp-prj.pnl-calc-cron|0 0 1 * * ?`（业务键消费，弃 `nop.job...cron-expr`）+ description 同步 helper/门控语义；`pnl-calc.batch.xml` processor 改调 helper（`inject('erpPrjProjectPnlCalcHelper').recalculateOne(item.id, batchChunkCtx.serviceContext)`，bean id 即简单名，对齐 R1.23 成功先例；`transactionScope="chunk"` 保持——per-item 隔离由 helper REQUIRES_NEW 承载）+ loader 过滤（DRAFT/OPEN/ON_HOLD）保持。
      - Skill: `nop-backend-dev`
- [x] `Fix` **null ctx 兜底**：helper 内 `serviceContext == null → new ServiceContextImpl()` 兜底（`recalculateOne(projectId, ctx)` 入口判空；`refreshPnl(projectId, null, null, svcCtx)` 经 BizProxy 代理需非 null ctx——R1.23 缺陷回归收口，Phase 3 批任务真实执行证实不 NPE）。
      - Skill: `nop-backend-dev`
- [x] `Fix` **helper bean 注册**（F1=A）：新增 `ErpPrjProjectPnlCalcHelper`（`module-projects/erp-prj-service/.../job/`，镜像 R1.23 `ErpCrmLeadScoringRecalcHelper`）在 `app-service.beans.xml` 显式注册（`erpPrjProjectPnlCalcHelper` bean id = 简单名，batch.xml `inject` 直接命中）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `ErpPrjConstants`/`ErpPrjConfigs`：config key 由 dead 转活跃——`CONFIG_PNL_CALC_CRON`/`CONFIG_PNL_AUTO_CALC_ENABLED` javadoc 更新说明消费点（job.yaml + helper）；`ErpPrjConfigs` 增 `DEFAULT_PNL_CALC_CRON = "0 0 1 * * ?"` + `pnlCalcCron()` 默认值改由该常量提供（显式置空=禁用「空值=跳过」语义，对齐 job.yaml cronExpr @cfg 默认）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `module-projects/erp-prj-service/pom.xml` 补 nop-batch-dsl test-scope 依赖（镜像 R1.23 erp-crm-service pom:81-87，紧邻 nop-autotest-junit）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 接线落地且 config 键消费闭环（`erp-prj.pnl-calc-cron`/`erp-prj.pnl-auto-calc-enabled` 非 dead——grep 显示 job.yaml + helper 消费点）
- [x] null ctx 兜底落地（批任务运行时 refreshPnl 不 NPE——Phase 3 测试证实）
- [x] job.yaml/batch.xml 与最终文档声明一致（Phase 4 文档同步后三方一致）

### Phase 3 - 测试矩阵

Status: completed
Targets: `module-projects/erp-prj-service/src/test/java/app/erp/prj/service/`（新增 `TestErpPrjPnlCalcJob.java`）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 完成

- [x] `Add` 批任务级测试（`TestErpPrjPnlCalcJob`，`IBatchTaskRunner.execute(taskPath)`——nop-batch-dsl 执行入口，镜像 R1.23 `TestErpCrmLeadScoringRecalcJob`）5 组全绿：① 批任务执行后 OPEN/DRAFT 项目生成 `ErpPrjProjectPnl`（calcStatus=CALCULATED + 收入 10000 + 四类成本 2000/1500/1000/1500 + 毛利 4000 + 毛利率 40.0000 + EAC 23000 数值断言，镜像 `TestErpPrjProjectPnl`；**真实执行路径 `batchChunkCtx.serviceContext`=null → helper 兜底 `ServiceContextImpl` 不 NPE——R1.23 缺陷回归证实**）；② loader 过滤（COMPLETED/CANCELLED 终态项目排除零 Pnl）；③ cron 空值跳过语义（`assignConfigValue` 置空 → helper 跳过 INFO + 零 Pnl 更新）；④ `erp-prj.pnl-auto-calc-enabled` 门控关闭（默认 false）跳过语义（INFO + 零 Pnl）；⑤ 失败隔离断言（F1=A：不存在项目 `ERR_PROJECT_NOT_REFERENCEABLE` → REQUIRES_NEW 回滚 + WARN 含 projectId + 返回 false + 随后正常项目继续汇总）。
      - Skill: `nop-testing`
- [x] `Proof` 既有 `TestErpPrjProjectPnl`/`TestErpPrjProjectSettlement` 零回归 + `TestErpAllJobYamlLoading`（job.yaml cronExpr 改动后 21 个文件仍可解析）：`mvn test -pl module-projects/erp-prj-service` **138 tests 全绿**（133 基线 + 5 新增，BUILD SUCCESS）+ `mvn test -pl app-erp-all -Dtest=TestErpAllJobYamlLoading` 1/1 绿。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新增批任务级测试全绿 + 既有 projects 测试零回归（`mvn test -pl module-projects/erp-prj-service` BUILD SUCCESS，138 tests）
- [x] 自动调度路径有运行时断言证据（非仅静态接线——`IBatchTaskRunner.execute` 真实执行 loader+processor+helper+refreshPnl 全链）+ null ctx 兜底证实

### Phase 4 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/design/projects/profitability.md`；`docs/architecture/job-scheduling.md`；`docs/audits/compliance-baseline.md`（F1=A 条件）；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-14.md`
Skill: none

- Item Types: `Add | Fix`
- Prereqs: Phase 1-3 完成

- [x] `Add` owner doc 注记：`profitability.md:82` 关键流程 1 补接线实现注记（job.yaml → nopBatchTaskRunner → batch.xml → `ErpPrjProjectPnlCalcHelper.recalculateOne()` → `IErpPrjProjectPnlBiz.refreshPnl` 全链 + config 门控语义 + null ctx 兜底 + 测试证据 + P2-RC-050 successor 边界声明）；不修改需求契约段（use-cases L1 不动）。
      - Skill: none
- [x] `Fix` `job-scheduling.md` Projects 段 stale 修正：§3.14 行 `erp-prj-pnl-aggregation`（（待实现）/DESIGN/batch-candidate）→ `erp-prj-pnl-calc`（SCHEDULED + job.yaml 路径 + config 键 + 证据链接）；**§7 候选作业汇总表同型 stale 行一并修正**（`erp-prj-pnl-calc` + REQUIRES_NEW 单条失败隔离 + 记录级幂等，对齐 R1.23 job-scheduling.md 全量同步先例）。
      - Skill: none
- [x] `Add` **R10 基线漂移登记（F1=A 落地）**：`docs/audits/compliance-baseline.md` 新增 R10 8→9 基线上调注记（per-site 证据：`ErpPrjProjectPnlCalcHelper.recalculateOne` 1 处 REQUIRES_NEW，镜像 R1.23 `ErpCrmLeadScoringRecalcHelper` 同型站点）+ 机器可读块 `R10: 9`。
      - Skill: none
- [x] `Add` arm-index P1-RC-053 → `done (RC-R1.27)` + 修复落地摘要（含接线形态确认 + config 键对齐 + null ctx 兜底 + 失败隔离）；roadmap RC-R1.27 → done ✅（含落地摘要）；`docs/logs/2026/08-14.md` 日志条目写入。
      - Skill: none

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + owner doc 注记落盘 + job-scheduling.md 双 stale 行修正 + R10 漂移登记（F1=A）；日志条目写入

## Draft Review Record

- Independent draft review iteration 1: accept with MINORs（独立子代理 ses_fff2475a9ffesawyK5TwQMaHTY）— 0 BLOCKER / 0 MAJOR；5 MINOR 已修正：① `ErpPrjConfigs` 方法行号（:70-74/:82-84）；② R1.23 pom 参照 :81-87；③ `job-scheduling.md:345` §7 候选汇总表第二处 stale 行补入 Phase 4 范围；④ F1=A helper 的 bean 注册步骤补入 Phase 2 显式项（app-service.beans.xml 或 _service.beans.xml）；⑤ `compliance-baseline.md` R10 漂移登记补入 Phase 4 Targets（F1=A 条件，R1.23 先例 :313-315）。共识达成，可转 active。
- Independent draft review iteration 2: `accept`（独立子代理 ses_fff1af439ffeX04DCmw3PykBy1）— 0 BLOCKER / 0 MAJOR。全部 5 项 iteration-1 MINOR 确认解决（ErpPrjConfigs :70-74/:82-84 实证 / R1.23 pom :81-87 / job-scheduling.md :345 第二处 stale 行入 Phase 4 / F1=A helper bean 注册显式项 / compliance-baseline.md R10 8→9 登记 Phase 4 项 + Closure Gates）。1 个新 MINOR 已顺手修订：Baseline 首段 `ErpPrjConfigs` 引注与实仓段行号统一为 :70-74/:82-84（cosmetic）。**计划可标记 active。**

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。**完整仓库验证在此处**：结束时运行一次全量验证。

- [x] 范围内行为完成
- [x] 相关文档对齐
- [x] 已运行验证（`mvn test -pl module-projects/erp-prj-service` 138 tests 全绿 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline；**R10 基线漂移已登记**——R10 8→9 per-site 证据落 `docs/audits/compliance-baseline.md`「R10 基线上调注记（plan 2026-08-14-2304-3，RC-R1.27）」块 + 机器可读块 R10: 9，按 R1.23 同型先例 + project-context 已知失败模式 #1 登记；`TestErpAllJobYamlLoading` 1/1 绿）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 审计判据「零 nop-job 消费」vs 既有 batch-task 接线的认知差

- Classification: `watch-only residual`
- Why Not Blocking Closure: A4.2.121 审计按「module-projects 内 Java Job bean + scheduler.yaml」判据搜索得出「零消费」结论，未识别 2026-07-18 nop-batch 迁移后的 app-erp-all job conf batch-task 接线（与 R1.23 lead-scoring 同型）——本行以 Explore 确认接线形态并修正 config 键消费与测试缺口，审计认知差不改变 P1 修复义务（Q4=(a) 强制实现，接线须运行时可达 + config 键活跃 + 测试覆盖）。
- Successor Required: `no`

### batchChunkCtx.serviceContext null ctx 缺陷在其他 batch job 的潜伏

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本行修复 pnl-calc 的 null ctx 缺陷（R1.23 注记的后续接线行收口）；其他既有 batch job（如 spc-sampling 等）若同样经 `batchChunkCtx.serviceContext` 传 ctx 可能潜伏同型缺陷——各域 batch job 接线行（如 RC-R1.26 SPC）执行时各自核实，不集中在本行。
- Successor Required: `no`

## Closure

Status Note: 执行完成并独立结束审计通过（2026-08-15）。四 Phase 全绿：接线落地（既有 batch-task 接线形态确认 + REQUIRES_NEW helper `ErpPrjProjectPnlCalcHelper` per-item 失败隔离 + config 键对齐 `erp-prj.pnl-calc-cron`/`erp-prj.pnl-auto-calc-enabled` 由 dead 转活跃 + `batchChunkCtx.serviceContext` null ctx 兜底 `ServiceContextImpl`——R1.23 Follow-up 潜伏缺陷收口）+ batch 任务级测试 5 组（TestErpPrjPnlCalcJob）+ owner doc 注记（profitability.md §关键流程 1）+ job-scheduling.md §3.14/§7 双 stale 行修正 + arm-index/roadmap/日志回填 + R10 8→9 基线漂移登记。验证：erp-prj-service 138 tests 全绿（133 基线 + 5 新增）+ TestErpAllJobYamlLoading 1/1 + 全量 `mvn clean install -DskipTests` BUILD SUCCESS + checker actual ≤ baseline（R10=9）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，只读，零文件修改）——ses_ffe56587dffeiS2DhPLA8yGOXR
- Evidence: **Verdict PASS**（0 P0 / 0 P1；1 P2 非阻塞已顺手修复[compliance-baseline 顶部汇总表 R10 6→9 同步]）——①计划状态一致性（4/4 Phase completed + 全 `[x]` + Closure Gates 7/7[结束审计门控由执行者按审计结论勾选]）；②Phase 2 接线实仓核验（job.yaml cronExpr `@cfg:erp-prj.pnl-calc-cron|0 0 1 * * ?` + enabled|false + nopBatchTaskRunner + taskPath；batch.xml processor `inject('erpPrjProjectPnlCalcHelper').recalculateOne(item.id, batchChunkCtx.serviceContext)` + loader DRAFT/OPEN/ON_HOLD + transactionScope=chunk 保持；helper REQUIRES_NEW + try/catch WARN + 双业务键门控 + null ctx 兜底 ServiceContextImpl；beans.xml 注册 + ErpPrjConstants/ErpPrjConfigs javadoc/默认值更新 + pom nop-batch-dsl test-scope）；③Phase 3 测试实跑（审计者自跑 `mvn test -pl module-projects/erp-prj-service -Dtest=TestErpPrjPnlCalcJob` 5/5 绿 + 全模块 138/138 绿 + TestErpAllJobYamlLoading 1/1 绿）；④Phase 4 文档实仓核验（profitability.md 实现注记 / job-scheduling.md §3.14+§7 双行 / compliance-baseline R10 8→9 注记 + 机器可读块 R10: 9 / arm-index P1-RC-053 done (RC-R1.27) / roadmap RC-R1.27 done ✅ / 日志条目）；⑤范围守卫（git status 仅预期文件[13 修改 + 3 新增]，零 ORM/会计/删除路径变更；审计者自跑全量 `mvn clean install -DskipTests` BUILD SUCCESS + checker actual ≤ baseline[R10=9]）。

Follow-up:

- 无范围外 follow-up；MR1 第一批后续 RC-R1.28+（cs 目录建单必填校验等）由 mission driver 继续。watch-only 维持：`batchChunkCtx.serviceContext` null ctx 缺陷在其余既有 batch job（如 spc-sampling 等）的潜伏由各域 batch job 接线行（如 RC-R1.26 已处理 SPC）执行时各自核实（本计划 Deferred But Adjudicated 已登记）。
