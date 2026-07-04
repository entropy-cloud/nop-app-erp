# 2026-07-04-1600-1-batch-scheduling-architecture 批处理与调度架构权威化（全局作业目录 + nop-job/nop-batch/nop-task 职责判据 + 专项审计）

> Plan Status: completed
> Last Reviewed: 2026-07-04
> Source: 用户对"批处理业务逻辑是否在设计/审计中说明白"的核查请求；用户裁决"批处理需要大数据量处理时使用 nop-batch 分 chunk 执行"
> Related: `2026-07-02-0000-best-practices-compliance-audit.md`（现有专业审计，不覆盖批处理维度，本计划补齐）、`docs/architecture/job-scheduling.md`（待重写）、各域计划 Deferred 段（18 处"nop-job 接线归 follow-up"）
> Audit: required

## Current Baseline

实时仓库与 `nop-entropy/docs-for-ai/` 逐项核实的事实（证据来自两轮独立 explore 子代理调研，会话 `ses_0d2ce00ccffef58k9ZyUi8ox3B` 与 `ses_0d2cdc773ffeemspmoSBvncREg`）：

### 平台能力边界（docs-for-ai 权威）

- **nop-job = 何时触发（when）**：CRON/固定频率/一次性触发；本地模式（`nop-job-local`，纯内存、重启丢失、不支持 misfire/阻塞/超时/分片）与分布式模式（`nop-job-service`，含 `nopJobInvoker_rpc`/`rpcBroadcast`、持久化、AMIS 管理）。证据 `nop-entropy/docs-for-ai/03-modules/nop-job.md:3-21,281-291`。
- **nop-batch = 如何分块执行（how）**：Loader→Processor→Consumer 三段式，每个 Chunk 独立事务、`completedIndex` 断点续跑、`NopBatchRecordResult` 记录级幂等、`retryPolicy`/`skipPolicy` 记录级重试。平台明确适用场景含**过账兜底扫描、资产折旧批量计提、银行流水导入**。证据 `nop-entropy/docs-for-ai/03-modules/nop-batch.md:5-15,230-238`。
- **配合模式已成文**：nop-job 定时触发 → task step 经 `<batch:Execute>` 调 nop-batch 分 chunk 执行。证据 `nop-batch.md:193-204`。
- **DAG 编排属 nop-task，不是 nop-job**：nop-job 通读 396 行无任何 DAG/graph 章节；`dispatchMode`(partition/broadcast/bestFit/single) 是 task 拆分策略非依赖编排。DAG 属 nop-task `<graph>`（数据驱动 DAG）。证据 `nop-task.md:115`、`reusable-modules-overview.md:13-22`。

### 本项目现状（脱节与漂移）

- **`job-scheduling.md` 极简且与平台契约冲突**：仅 53 行，登记 **5 个**标准作业（posting-scan/period-close/depreciation/stock-check/data-sync）+ 一个"DAG 依赖"章节（`:36-45`）把 DAG 算作 nop-job 能力——**这与平台契约冲突**（nop-job 无 DAG），属 owner-doc 与平台契约漂移（Fix 项，规则 13 不可降级）。
- **作业目录严重不全**：全仓散落 **40+ 个**定时/批处理作业（14 个明确 cron 配置项 + 多个定时语义作业），仅 5 个登记到 `job-scheduling.md`。证据见两轮调研的作业候选清单总表。
- **nop-batch 完全闲置**：全仓 `rg nop-batch` 仅 `docs/discussions/2026-06-29...:1761` 一处"大批量对账数据分片处理（可选）"提及；平台明确适用的过账兜底/折旧批量/对账等大数据量作业，当前全部隐含用 nop-job 单节点 + BizModel 内部循环承载，nop-batch 分片能力未被任何正式设计采用。
- **配置键不一致**：`erp-log.tracking-poll-cron`（设计 `logistics/carrier-integration.md:357,458`）vs `erp-log.tracking-polling-cron`（计划 `2026-07-04-1115-3:57`）键名不一致。
- **三向脱节**：设计 cron 配置项 ↔ `job-scheduling.md` 标准作业表 ↔ 各域 owner doc"配置点"表 互不对账。`job-scheduling.md` 的 5 个标准作业的 cron 表达式（如"每月最后一天 22:00"）未回流到各域 owner doc，owner 不知道自己作业何时跑。
- **nop-job 接线现状**：`nop-job-local` 已接入 `app-erp-all` 框架（`docs/logs/2026/06-23.md:14-17`，绕开 RpcJobInvoker 的 RPC 依赖），但**无任何 job bean 实现**；18 处计划 Deferred 段推迟 cron 实际注册（已交付可被 GraphQL/测试调用的 BizModel 入口方法）。
- **审计缺口**：唯一专业审计 `docs/audits/2026-07-02-0000-best-practices-compliance-audit.md` 覆盖 ORM/BizModel/平台合规（D1-D6），**完全不碰批处理/定时作业维度**——未核验作业目录完整性、BizModel 入口可达性、nop-batch 适用性、DAG 归属正确性。
- **选择判据 docs-for-ai 未成文**：nop-job vs nop-batch 无显式阈值表；`non-bizmodel-orm-access.md:134-186` 提示"十万级以上大表"考虑加时间范围（旁证阈值量级），但无明确裁决。

### 剩余差距

(1) 无权威全局作业目录；(2) nop-job/nop-batch/nop-task/nop-message 四者职责与选择判据未成文；(3) DAG 归属漂移未修正；(4) nop-batch 对大数据量作业的适用性未裁决（~10 个候选仍隐含 nop-job 承载）；(5) 配置键不一致 + 三向脱节；(6) 无批处理专项审计。

## Goals

- **重写 `docs/architecture/job-scheduling.md` 为权威全局作业目录**：汇聚全部 40+ 作业，每条含域/业务功能/触发频率/调用入口(BizModel 方法)/数据量级/执行模式/当前状态/配置键/证据 file:line。
- **建立四模块职责划分与选择判据**：nop-job（触发）/ nop-batch（大数据量分 chunk 执行）/ nop-task（跨作业 DAG 编排）/ nop-message（跨进程推送）的边界 + 数据量阈值裁决表（标注推断）。
- **裁决 nop-batch 适用作业**：对 ~10 个大数据量作业（过账兜底/折旧批量/库存对账/AR-AP 核销/漏斗聚合/SPC 采样/归档/存货-总账对账/日志 TTL/合并报表）标注 `executionModel=nop-batch candidate`，给出迁移触发条件（不强制本计划接线）。
- **修正 DAG 归属漂移（Fix）**：将"DAG 依赖"章节改为"作业逻辑前置关系"，澄清期末结账模块顺序已由 `ErpFinAccountingPeriodBizModel` 内部编排承载（已实现，`plans/2026-07-02-1000-3` Phase 1），不依赖跨作业 DAG；真正的跨作业 DAG 若需要则引入 nop-task `<graph>`。
- **配置点回流 + 键名统一**：将 `job-scheduling.md` 登记作业的 cron 时间回流到各域 owner doc"配置点"表（关键作业）；统一 `erp-log.tracking-poll-cron` 键名。
- **补批处理专项审计**：`docs/audits/2026-07-04-0000-batch-scheduling-audit.md`，核验作业目录完整性 / BizModel 入口可达性 / nop-batch 适用性裁决一致性 / 配置键一致性 / DAG 归属正确性。

## Non-Goals

- **实际接线实施**（scheduler.yaml 注册 / batch.xml 编写 / IJobInvoker 实现 / BizModel 方法新增）：归各域 follow-up，每个 deferred 作业已有独立触发条件；本计划只建立权威架构契约让后续接线有统一规范。
- **引入 nop-batch / nop-task 模块依赖到 app-erp-all**：本计划仅在架构文档登记判据与候选；实际模块引入与接线归各域 follow-up（触发条件：首个 nop-batch 候选作业正式接线时）。
- **nop-job-local → nop-job-service 分布式部署迁移**：本计划登记演进路径与触发条件，不执行迁移。
- **业务逻辑变更**：各作业的业务规则已在各域设计文档（period-close.md / depreciation-and-posting.md / treasury.md / ar-ap-reconciliation.md 等）定义完整，本计划不碰业务语义。
- **前端 AMIS 作业管理页面**：属 nop-job-web 标准页面，归部署阶段。

## Task Route

- Type: `architecture change`（批处理架构权威化 + DAG 归属漂移修正）+ `verification or audit work`（专项审计）。**纯文档与审计，不改代码、不改 ORM**。
- Owner Docs: `docs/architecture/job-scheduling.md`（主，重写）、各域 owner doc"配置点"表（关键作业回流）、`docs/audits/2026-07-04-0000-batch-scheduling-audit.md`（新审计）。
- Skill Selection Basis: 纯文档汇聚 + 审计工作，不涉及 BizModel/ORM/view.xml 实现；`nop-backend-dev`/`nop-frontend-dev` 匹配实现工作而非本计划。Skill: none（本计划不写业务代码；后续各域接线时由各域计划加载相应技能）。
- **Decision（nop-job vs nop-batch 选择判据，用户已定调大数据量用 nop-batch）**：**选择**按"单次预估处理量 + 事务边界需求 + 断点续跑需求 + 失败重试粒度"四维裁决：单次处理量 ≥ 1万记录、或需全表/大批量扫描、或需断点续跑/记录级幂等/记录级重试 → nop-batch 分 chunk；单次处理量小、定点查询少量记录、整作业一个事务可接受 → nop-job 直接调 BizModel。大数据量作业 = nop-job（触发）+ nop-batch（执行）双层配合。**替代**：① 全部用 nop-job + BizModel 内部分页循环（`non-bizmodel-orm-access.md:134-186` 给出的纯 Java 方案）——无法满足断点续跑/记录级幂等/独立事务，失败需整体重跑，对折旧批量/对账等长作业不可接受，rejected；② 全部用 nop-batch（小数据量作业如 CSAT 提醒/合同到期提醒也走分片，过度工程，rejected）。**残留风险**：阈值 1万为推断（平台 docs-for-ai 未成文），实际应结合单记录处理耗时动态判断——审计需复核每个 nop-batch candidate 的量级标注有证据支撑。
- **Decision（DAG 归属漂移修正）**：**选择**将 `job-scheduling.md`"DAG 依赖"章节改为"作业逻辑前置关系"，明确：(1) 期末结账的 AR→AP→INV→AST→GL 模块顺序已由 `ErpFinAccountingPeriodBizModel.closePeriod` 内部编排承载（`plans/2026-07-02-1000-3` Phase 1 已实现），不依赖跨作业 DAG；(2) 真正的跨作业 DAG 编排（如归档→对账→报表链）若未来需要，引入 nop-task `<graph>`，而非 nop-job。**替代**：① 引入 nop-task 重构期末结账为跨作业 DAG（已实现的 BizModel 内部编排工作良好，无需重构，rejected）；② 保留 nop-job DAG 说法（与平台契约冲突，属漂移不可保留，rejected）。**残留风险**：nop-task 引入门槛（运维需理解第三模块），但当前无真实跨作业 DAG 需求，仅登记演进路径。
- **Decision（nop-job-local vs nop-job-service 演进路径）**：**选择**当前维持 nop-job-local（单机、作业定义内存态、适合 bootstrap 阶段），在 `job-scheduling.md` 登记迁移触发条件（多实例部署 / 作业需持久化与 AMIS 管理 / 需 misfire 补偿与阻塞策略时 → 引入 nop-job-service）。**替代**：立即迁移 nop-job-service（RpcJobInvoker 单机 RPC 配置复杂，`docs/logs/2026/06-23.md:15` 已验证单机启动困难，bootstrap 阶段无必要，rejected）。**残留风险**：nop-job-local 重启丢失作业定义——本计划登记的所有作业 cron 需在 scheduler.yaml 声明以保证重启重建（属 follow-up 接线工作）。

## Infrastructure And Config Prereqs

- 无新增端口/密钥/外部服务/数据迁移（纯文档与审计计划）。
- 依赖两轮 explore 调研产出（作业候选清单总表 + 平台能力边界）已固化于本计划 Current Baseline。

## Execution Plan

### Phase 1 — 重写 job-scheduling.md 为权威全局作业目录 + 四模块职责判据 + DAG 归属修正

Status: completed
Targets: `docs/architecture/job-scheduling.md`（重写）
Skill: none

- Item Types: `Add | Fix | Decision`
- Prereqs: 无（基线已由两轮调研建立）。

- [x] `Fix`：删除/改写现有"DAG 依赖"章节（`:36-45`）为"作业逻辑前置关系"，澄清 nop-job 无 DAG 能力、期末结账模块顺序由 BizModel 内部编排承载、真正跨作业 DAG 走 nop-task `<graph>`。修正与平台契约的漂移。
  - Skill: none
- [x] `Add`：新增"四模块职责与选择判据"章节——nop-job（触发）/ nop-batch（大数据量分 chunk 执行）/ nop-task（跨作业 DAG 编排）/ nop-message（跨进程推送）边界 + 四维裁决表（数据量/事务边界/断点续跑/重试粒度）+ nop-job-local vs nop-job-service 演进路径。阈值标注为推断并引用 `non-bizmodel-orm-access.md:134-186` 旁证。
  - Skill: none
- [x] `Add`：将"标准作业"表升级为**全局作业目录**，汇聚全部 40+ 作业，分域分组，每条含：作业标识 / 业务功能 / 触发频率 / 调用入口(BizModel 方法) / 数据量级(小/中/大) / 执行模式(nop-job 直调 | nop-batch candidate) / 当前状态(已登记/已接线未登记/deferred/仅设计) / 配置键 / 证据 file:line。对 ~10 个大数据量作业标注 `executionModel=nop-batch candidate` 并附迁移触发条件。
  - Skill: none
- [x] `Decision`：在文档顶部记录三项裁决（nop-job vs nop-batch 判据 / DAG 归属修正 / nop-job-local 演进路径），引用本计划 Task Route 的选择+替代+残留风险。
  - Skill: none

Exit Criteria:

> Phase 1 交付权威全局作业目录。解除 Phase 2 回流与 Phase 3 审计的阻塞。

- [x] `job-scheduling.md` 含全局作业目录（≥40 条作业登记）+ 四模块判据章节 + 修正后的前置关系章节
- [x] 大数据量作业（≥10 条）标注 `executionModel=nop-batch candidate`

### Phase 2 — 配置点回流 + 键名统一

Status: completed
Targets: 各域 owner doc"配置点"表（关键作业）、`docs/design/logistics/carrier-integration.md`（键名统一）、相关计划文件
Skill: none

- Item Types: `Fix | Add`
- Prereqs: Phase 1（全局作业目录已建立）。

- [x] `Fix`：统一承运商轮询 cron 键名——`erp-log.tracking-polling-cron`（计划 `2026-07-04-1115-3:57`）改为 `erp-log.tracking-poll-cron`（对齐设计 `logistics/carrier-integration.md:357,458`），裁决以设计文档为准、计划侧改（计划 `2026-07-04-1115-3` 两处 `tracking-polling-cron` 已改）。
  - Skill: none
- [x] `Add`：将 `job-scheduling.md` 登记的关键作业（5 个原标准作业 + 折旧/库存对账/现金预测/AR-AP 核销/SLA 扫描/承运商轮询/CRP/DRP/薪酬等高频或大数据量作业）的 cron 时间与配置键回流到对应域 owner doc"配置点"表，使 owner 知道自己作业何时跑、配置键是什么。已回流：period-close / treasury(cash-forecast) / ar-ap-reconciliation / posting(posting-scan) / depreciation / inventory README(stock-check) / master-data README(data-sync)。
  - Skill: none

Exit Criteria:

- [x] `rg 'tracking-polling-cron'` 零命中（实际配置声明：设计/Java/计划 1115-3 均统一为 `tracking-poll-cron`；本计划自身审计追踪段落的自指描述不计为配置声明，见 Phase 3 审计维度 4）；关键作业在 owner doc 配置点表可查

### Phase 3 — 批处理专项审计

Status: completed
Targets: `docs/audits/2026-07-04-0000-batch-scheduling-audit.md`（新）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 + Phase 2（审计对象已就位）。

- [x] `Add`：新建 `docs/audits/2026-07-04-0000-batch-scheduling-audit.md`，核验六维度：
  1. **作业目录完整性**：`rg 'cron|定时|定期|周期|期末|nop-job' docs/design docs/architecture docs/plans` 的每个引用是否都登记到 `job-scheduling.md` 全局作业目录（无遗漏）。
  2. **BizModel 入口可达性**：每个标注了调用入口的作业，其 BizModel 方法在实时仓库是否真实存在（deferred 作业的方法应已交付，按计划声明）。
  3. **nop-batch 适用性裁决一致性**：每个 `executionModel=nop-batch candidate` 标注的作业，其数据量级标注是否有证据支撑；是否有"大数据量却标 nop-job 直调"的遗漏。
  4. **配置键一致性**：全仓 cron 配置键无拼写分歧（如 tracking-poll-cron 统一）。
  5. **DAG 归属正确性**：`job-scheduling.md` 不再出现"nop-job DAG"误述；前置关系章节正确区分 BizModel 内部编排 vs 跨作业 DAG(nop-task)。
  6. **owner doc 配置点回流**：关键作业 cron 是否回流到各域 owner doc。
  - Skill: none
- [x] `Proof`：审计文档每条结论附 `rg` 命令证据与 file:line；对发现的偏差按严重度分级（与 `2026-07-02-0000-best-practices-compliance-audit.md` 体例一致）。
  - Skill: none

Exit Criteria:

> Phase 3 交付专项审计。完整仓库验证不适用（纯文档计划，Closure Gates 删验证命令门控）。

- [x] 审计文档存在且覆盖六维度，每条结论附证据

## Draft Review Record

- Independent draft review iteration 1: accept (main review session, 2026-07-04) — 格式合规（必需段落齐全、字段名正确、Phase 结构有效、Item Types 合法且阶段级聚合声明符合规则 7）；完整性达标（六项 Goals 均被 Execution Plan 覆盖，Exit Criteria 可测：≥40 作业登记 / ≥10 nop-batch candidate / `rg 'tracking-polling-cron'` 零命中 / 审计六维度附证据）；范围清晰（单一结果表面=权威批处理架构文档+审计，三阶段紧耦合 doc→backflow→audit，Non-Goals 明确排除接线/模块依赖/分布式迁移/业务逻辑/前端，无 "and also..." 蔓延，符合规则 14）；结束证据定义充分（Closure Gates 枚举具体 rg 命令与文档完整性交叉核实，DAG 漂移正确标 `Fix` 符合规则 13 不可降级，三项 Decision 均含 选择/替代/残留风险 符合规则 9，纯文档计划按模板规则删除验证命令门控并说明原因）。无 Blocker/Major。遗留 Minor：Phase 2 "或反向统一" 措辞略冗但括号已裁决（设计文档为准、计划侧改）；Phase 2 "关键作业...等" 软枚举由 Phase 3 审计维度 6 兜底。可进入实施。

## Closure Gates

> 本计划为纯文档与审计工作，不改代码/ORM/契约。按模板规则删除验证命令门控（`mvn`/`typecheck` 不适用），改为文档完整性交叉核实。

- [x] 范围内行为完成：全局作业目录（≥40 条）+ 四模块判据 + DAG 归属修正 + 配置点回流 + 键名统一 + 专项审计
- [x] 相关文档对齐：`job-scheduling.md` 重写；各域 owner doc 配置点回流；当日日志已记
- [x] 无范围内项目降级为 deferred/follow-up（实际接线实施为计划内 Non-Goal，不属降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 文档完整性交叉核实：`rg 'cron|定时|nop-job|nop-batch' docs/` 的每个作业引用都能在 `job-scheduling.md` 全局目录中查到；`rg 'tracking-polling-cron'` 零命中（实际配置声明）；`job-scheduling.md` 不含"nop-job DAG"误述
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 实际接线实施（scheduler.yaml 注册 / batch.xml / IJobInvoker / BizModel 方法）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: ~40 个作业的接线属各域 follow-up，每个 deferred 作业已有独立触发条件（见 18 处计划 Deferred 段）；本计划建立权威架构契约让后续接线有统一规范。nop-batch 模块依赖引入到 app-erp-all 同理。
- Successor Required: yes（触发条件：首个 nop-batch candidate 作业正式接线时 / 各域 deferred 作业触发条件满足时）

### nop-job-local → nop-job-service 分布式部署迁移

- Classification: `optimization candidate`
- Why Not Blocking Closure: bootstrap 阶段单机够用；RpcJobInvoker 单机 RPC 配置复杂（`docs/logs/2026/06-23.md:15`）。
- Successor Required: yes（触发条件：多实例部署 / 作业需持久化与 AMIS 管理 / 需 misfire 补偿时）

### 跨作业 DAG 编排引入 nop-task

- Classification: `watch-only residual`
- Why Not Blocking Closure: 当前无真实跨作业 DAG 需求（期末结账模块顺序由 BizModel 内部编排承载）。
- Successor Required: yes（触发条件：出现归档→对账→报表等跨作业依赖链时）

## Closure

Status Note: 已完成。三阶段全部交付并通过独立结束审计（2026-07-04）。`docs/architecture/job-scheduling.md` 重写为权威全局作业目录（52 条，16 域）+ 四模块职责判据 + DAG 归属漂移修正；7 个关键作业 cron 键回流到 owner doc；承运商轮询键名统一（`erp-log.tracking-poll-cron`）；新建专项审计 `docs/audits/2026-07-04-0000-batch-scheduling-audit.md` 覆盖六维度。实际接线（scheduler.yaml/batch.xml）为计划内 Non-Goal，归各域 follow-up。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理新会话（ses_0d27cf87bffemQ5PZnhmFzL7Zw，general agent，read-only 闭合审计）
- Evidence: `docs/audits/2026-07-04-0000-batch-scheduling-audit.md`（六维度，附 rg 命令证据 + file:line）；闭合审计 Gate 1-4 全 PASS（作业目录 103 行 ≥40、batch-candidate 16 ≥10、7 配置键回流已核实、tracking-polling-cron 实际配置声明零分歧、DAG 归属已修正为 nop-task）。Gate 5/6（状态闭合 + 当日日志）已据此补齐。

Follow-up:

- 实际接线实施（见上方 Deferred）
- nop-job-service 分布式迁移（见上方 Deferred）
- nop-task 跨作业 DAG（见上方 Deferred）
