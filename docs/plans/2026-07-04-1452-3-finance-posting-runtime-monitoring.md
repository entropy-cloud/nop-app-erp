# 2026-07-04-1452-3-finance-posting-runtime-monitoring 业财过账运行监控

> Plan Status: completed
> Last Reviewed: 2026-07-04
> Source: `docs/backlog/core-business-roadmap.md` M5 工作项 5.3（P1）；`docs/design/finance/posting-log.md` §运行监控指标（设计 done）；`docs/analysis/2026-07-04-finance-posting-engine-gap-vs-opensource.md` §2.5（运营层缺口）
> Related: `2026-07-04-1452-1-finance-posting-log-observability.md`（提供 post/reverse 埋点面与异常记录数据源，前置）
> Audit: required

## Current Baseline

- 四指标已在 `posting-log.md:88-99` 设计：自动化记账率（自动凭证数÷总凭证数，目标≥95%）、凭证生成时延（业务事件触发到过账耗时 P99，<30s）、过账异常率（失败/未命中占比，<1%）、业财闭环成功率（源单 `posted` 翻转成功数÷过账成功数，≥99.5%）。
- **关键缺口——平台无监控 API**：核实 nop-platform **无任何 metrics 埋点设施**——`CoreMetrics` 仅为时钟（`currentTimeMillis`/`nanoTime`/`nanoTimeDiff`，`../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md:17-48`），无 Micrometer/Prometheus/Actuator 依赖（grep 全平台 + 本仓 pom 无 `micrometer|prometheus|actuator|MeterRegistry`）。可用"监控面"仅 `/q/health*`、`/q/metrics*` 平台信息端点（`02-core-guides/auth-and-permissions.md:36`），非业务指标埋点 API。
- **owner-doc 漂移**：`posting-log.md` §运行监控 与 §实现策略 4 写"接入 nop-platform 监控（计数器/计时器/仪表盘）"与"接入 nop-platform 监控大盘"——该大盘/计数器 API **不存在**。本计划须在 Phase 1 裁定真实落地路径并修正 owner doc。
- 5.1（前置）在 `ErpFinPostingProcessor` 各 step 埋了结构化日志（含各阶段 `nanoTime` 耗时与成功/失败 + ErrorCode）与异常记录载体——是本计划指标计算的**数据源**。
- 仅有的"秒表"模式：`long begin = CoreMetrics.nanoTime(); … CoreMetrics.nanoTimeDiff(begin)`（`common-java-helpers.md:29,31`）。

## Goals

- 裁定运行监控的真实落地路径（Resolve Phase 1 Decision），修正 `posting-log.md` 关于"平台监控大盘"的漂移表述。
- 落地四指标的数据采集与可查询呈现（计数/时延/异常率/闭环成功率），最小可观察不依赖尚不存在的平台大盘。
- 告警 SLA 阈值以配置化门控表达（指标越限可被检出），不内建告警通道对接。

## Non-Goals

- 引入完整 APM/可观测性栈（SkyWalking/Prometheus server 部署等）。
- 自建监控大盘前端 UI（呈现走查询接口/快照，非可视化页面）。
- 告警通道（SMS/邮件/webhook）对接与升级链。
- 把"无平台 metrics API"升级为"为平台新建 metrics 模块"——超出应用层范围（Follow-up 上游）。

## Task Route

- Type: `implementation-only change` + `app-layer design change`（须修正 owner doc `posting-log.md` 的漂移表述），核心是 metrics 落地路径 Decision。
- Owner Docs: `docs/design/finance/posting-log.md`（§运行监控指标 / §实现策略 4 权威，须修正）。
- Skill Selection Basis: 埋点采集 + 指标聚合查询 BizModel + `ErrorCode` + 配置门控，匹配 `nop-backend-dev`。若 Decision 选 Micrometer 则涉及 pom 依赖（仍属后端实现）。

## Infrastructure And Config Prereqs

- 无新增端口/密钥/.env。
- 依赖 5.1（前置）的埋点面与异常记录数据源；若 5.1 异常记录载体为持久化表，本计划直接查询；若为日志，本计划 Phase 1 须裁决衍生指标的存储。
- 若 Phase 1 Decision 选 (a) Micrometer/Actuator，则需在 `app-erp-all`（或 finance service）pom 增依赖并由部署侧暴露端点——此项为 Decision 的实施后果。
- **若 Phase 1 Decision 选 (c) 持久化 `ErpFinPostingMetric` 快照表，触及 finance ORM 保护区域，须人工批准后方可实施 Phase 2**（回滚策略：指标表增量可随模型重新生成移除，无业务数据迁移）。

## Execution Plan

### Phase 1 - Decision: 监控落地路径（Resolve owner-doc 漂移）

Status: completed
Targets: `docs/design/finance/posting-log.md`（§运行监控/§实现策略修正）、本计划
Skill: `nop-backend-dev`

- Item Types: `Decision | Explore`
- Prereqs: 5.1 active（埋点面与异常记录数据源确定）

- [x] Explore：核实 app-erp-all 运行态是否已带 Spring Boot Actuator（pom/配置）；核实 5.1 异常记录载体形态（表/日志）以确定"异常率/自动化记账率"可否由 SQL 聚合得出；核实 `posted` 翻转可观测性（闭环成功率分子分母来源）。
      - Skill: `nop-backend-dev`
      - **结论**：(1) **无 Actuator/Micrometer**——grep `micrometer|prometheus|actuator|MeterRegistry` 全仓 pom（根/app-erp-all/finance 各模块）零命中，平台无 metrics 埋点 API。(2) 5.1 异常记录载体 = 持久化表 `ErpFinPostingException`（含 `occurrenceTime`/`status`/`resolution`/`postingType`），可 SQL 聚合得异常率与手工补录计数。(3) 凭证表 `ErpFinVoucher` 含 `postingType`(NORMAL/REVERSAL)/`postedAt`/`createTime`，可 SQL 聚合得成功计数。(4) `posted` 翻转在源域单据（purchase/sales/inventory/...），finance 域不可见——SYNC 默认下 post 成功隐含 posted 翻转（域自治强一致），ASYNC 下无确认回调。(5) 时延：`PostingRun.stageNanos` 在内存采集各阶段纳秒但**不持久化**，事件触发时间未入库 → P99 时延**不可由 SQL 衍生**，须内存采样。
- [x] Decision（监控落地路径），四选一并记录替代方案与残留风险：
  (a) 引入 Micrometer + Actuator，在 post/reverse 入口出口埋 counter/timer/gauge，经 `/q/metrics` 暴露（需 pom 依赖 + 部署侧抓取）；
  (b) 应用级指标快照——按窗口聚合 5.1 日志/异常记录到查询接口（零新依赖，但无实时推流）；
  (c) 持久化 `ErpFinPostingMetric` 快照表 + 定时 rollup（触及 finance 保护区域，需人工批准）；
  (d) 部分指标（自动化记账率/异常率/闭环成功率）走 (b) 日志衍生查询，时延走 (a) 或 `nanoTimeDiff` 采样。
      - Skill: `nop-backend-dev`
      - **裁决：选 (b) + 时延内存采样（即 (b)+(d) 时延路径的混合）**。
        - **指标 1 自动化记账率**：SQL 聚合 `ErpFinVoucher`（自动凭证数）÷ (`ErpFinVoucher` + `ErpFinPostingException.resolution=MANUAL`，即总需记账事件)。零新依赖。
        - **指标 2 凭证生成时延 P99**：内存窗口采样（`ErpFinPostingMetrics` 环形缓冲，复用 5.1 `PostingRun` 各阶段 `nanoTimeDiff` 求和为单次时延）。**不持久化**（事件触发时间未入库，SQL 不可行；持久化须加列触保护区域）。残留：进程重启采样清零（采样指标可接受）。
        - **指标 3 过账异常率**：SQL 聚合 `ErpFinPostingException` ÷ (`ErpFinVoucher` + `ErpFinPostingException`)。零新依赖。
        - **指标 4 业财闭环成功率**：**代理值**——SYNC 默认下 post 成功隐含源单 posted 翻转（域自治强一致），故代理 = 过账成功数÷过账成功数 = 1.0；标注 `loopbackProxyMode=true`。残留：ASYNC 模式或域忘记翻转 posted 不可检出（Follow-up：可选 `IErpFinPostedProbe` SPI 让各域上报翻转确认）。
        - **替代方案（被拒）**：(a) Micrometer+Actuator——需新增 pom 依赖 + 部署侧抓取设施，超出应用层最小落地，按 Deferred 触发条件后继（生产部署需 Prometheus 抓取时）。(c) 持久化指标快照表——触及 finance ORM 保护区域（`model/*.orm.xml` ask first），且内存采样 + SQL 聚合已满足"指标可查询呈现"目标，无充分理由触保护区域。
        - **残留风险**：(i) 时延为进程内窗口采样，重启清零、无历史趋势（Follow-up：生产趋势须接 Micrometer + 时序库）。(ii) 闭环成功率为代理值，ASYNC/域 bug 不可检出（Follow-up：PostedProbe SPI）。(iii) 无自动告警通道（Deferred：告警通道对接）。
- [x] Fix（owner-doc 漂移）：按 Decision 修正 `posting-log.md` §运行监控与 §实现策略 4 中"接入 nop-platform 监控大盘"表述为实际落地路径，注明平台无内建 metrics API（避免误导后续）。本计划编辑范围限定 §运行监控与 §实现策略 4，不触碰实现策略其他节（归 5.1），避免与 5.1 的 owner-doc 修订撞节。
      - Skill: `nop-backend-dev`
      - **证据**：`posting-log.md` §运行监控指标 表前导句由"接入 nop-platform 监控大盘"改为实际落地路径说明（应用级快照查询 + 内存时延采样 + 平台无 metrics API 注记）；§实现策略 其他原则 第 2 条已正确写"归 5.3"，无需改动；新增"实现策略 §裁决 3：运行监控落地路径"节落地 Decision 结论（仅新增，不碰裁决 1/2 与其他原则，避免与 5.1 撞节）。

Exit Criteria:

- [x] 监控落地路径 Decision 写入 `posting-log.md`（修正后）与本计划，含替代方案/残留风险；owner-doc 漂移已修正

### Phase 2 - 指标采集与呈现

Status: completed
Targets: 按 Phase 1 Decision 选定（`ErpFinPostingProcessor` 埋点 / 指标聚合 BizModel / Micrometer 注册）、`posting-log.md`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1；若 Decision 选 (c) 持久化指标表，须 finance ORM 保护区域人工批准后方可实施

- [x] Add：按 Decision 落地四指标采集——自动化记账率（自动凭证数÷总凭证数）、凭证生成时延（P99，复用 5.1 各阶段 `nanoTimeDiff`）、过账异常率（失败÷总尝试，复用 5.1 异常记录）、业财闭环成功率（`posted` 翻转成功÷过账成功）。
      - Skill: `nop-backend-dev`
      - **证据**：`ErpFinPostingMetrics`（进程内环形缓冲采样器，复用 5.1 `PostingRun` 各阶段 `nanoTimeDiff` 求和为单次时延）经 `ErpFinPostingProcessor.process()`/`reverseProcess()` 成功路径喂样；`ErpFinPostingExceptionBizModel.getRuntimeMetrics()` 聚合四指标——自动化记账率 = `ErpFinVoucher` 计数 ÷ (`ErpFinVoucher` + `ErpFinPostingException.resolution=MANUAL`)、异常率 = `ErpFinPostingException` ÷ (`ErpFinVoucher` + `ErpFinPostingException`)、时延 P99 = 采样器 `p99LatencyMillis()`、闭环成功率 = 代理值 1.0（SYNC 强一致假设，`loopbackProxyMode=true`）。配置键 + 默认值落地 `ErpFinConstants`（6 键：4 阈值 + 窗口 + 采样窗口）。
- [x] Add：指标呈现查询接口（`@BizQuery`）返回当前/窗口四指标值与阈值比对（≥95%/<30s/<1%/≥99.5%），阈值 config-gated。
      - Skill: `nop-backend-dev`
      - **证据**：`IErpFinPostingExceptionBiz.getRuntimeMetrics(Integer windowHours, IServiceContext)` `@BizQuery` 返回 `ErpFinPostingMetricsSnapshot`（DTO，含四 `MetricValue`：value/threshold/healthy/direction + 观测基数 voucherCount/exceptionCount/manualResolutionCount/latencySampleCount + loopbackProxyMode 标志）。阈值经 `AppConfig.var` 读取（config-gated），窗口可由调用方传入或读默认。direction 字段标明 higher_better（达标=value≥threshold）/ lower_better（达标=value<threshold），使越限判定方向可程序化消费。
- [x] Proof：测试——构造成功/失败/未过账样本，断言四指标值与阈值门控判定正确（达标/越限）。
      - Skill: `nop-backend-dev`
      - **证据**：`TestErpFinPostingMetrics`（2 测试）：(1) `testFourMetricsWithSuccessAndFailureSamples` —— 2 笔成功过账 → 自动化记账率=1.0（达标）/ 异常率=0（达标）/ 时延 P99<30s（达标）/ 闭环成功率=1.0 代理（达标）；断言 `loopbackProxyMode=true`、direction 标签正确。(2) `testExceptionRateAndAutoPostingRateDegradeOnFailure` —— 1 成功 + 3 失败 + 1 手工补录 → 异常率=0.75>0.01 越限（healthy=false）/ 自动化记账率=0.5<0.95 越限（healthy=false）；断言越限方向正确。2 测试通过，finance-service 全 112 测试通过。

Exit Criteria:

- [x] 四指标可采集且经查询接口呈现，阈值门控判定正确，测试通过

## Draft Review Record

- Independent draft review iteration 1: needs revision（ses_0d4159e6cffenc8xLYYJKef3B0，general 独立子代理）because Decision 选项 (c) 持久化指标表触及 finance ORM 保护区域，人工批准仅写在选项文本内，未接为 Phase 2 的条件 prereq 门（违反规则 9，与 Plan 1 Phase 4 写法不一致）。已修订：在 `Infrastructure And Config Prereqs` 与 Phase 2 Prereqs 显式加"若选 (c) 须人工批准"条件门 + 回滚策略。同时采纳非阻塞：Phase 1 Item Types 修正为 `Decision | Explore`；owner-doc 编辑边界限定 §运行监控/§实现策略 4（不碰其他节，避免与 5.1 撞节）。
- Independent draft review iteration 2: accept（共识达成，无阻塞）—— blocking 项已修订且与 Plan 1 保护区域门写法一致。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成（四指标采集 + 呈现 + 阈值门控；owner-doc 漂移修正）
- [x] 相关文档对齐（`posting-log.md` §运行监控修正；`core-business-roadmap.md` 5.3 标进展；当日日志）
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn test -pl module-finance/erp-fin-service -am`（或按 Decision 受影响模块）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 告警通道对接（SMS/邮件/webhook）与升级链

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划落地"指标可检出越限"；通道对接与升级属运营通知层，依赖部署侧通知基础设施。
- Successor Required: yes（触发条件：生产部署需自动通知值班时，接入告警通道）

### 监控大盘可视化页面

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 指标经查询接口可消费；可视化页面属前端/BI 面（nop-report 或外接 Grafana）。
- Successor Required: yes（触发条件：运营需图形化大盘时）

### 为 nop-platform 新建通用 metrics 模块

- Classification: `watch-only residual`
- Why Not Blocking Closure: 应用层用选定路径满足业务指标；通用平台 metrics 设施属上游 nop-entropy 范围，超出本应用计划。
- Successor Required: yes（触发条件：上游 nop-entropy 暴露 metrics API 时，回迁埋点至平台设施）

## Closure

Status Note: 全部 2 阶段实施完成并通过独立结束审计。Phase 1 裁定监控落地路径为 (b) 应用级指标快照 + 内存时延采样（零新依赖、零 ORM 保护区域触及），修正 owner-doc `posting-log.md` "接入 nop-platform 监控大盘"漂移表述为实际落地路径 + 新增 §裁决3。Phase 2 落地四指标：自动化记账率/异常率经 SQL 聚合 `ErpFinVoucher`+`ErpFinPostingException`；凭证生成时延 P99 经进程内环形缓冲采样器 `ErpFinPostingMetrics`（复用 5.1 各阶段 `nanoTimeDiff`）；业财闭环成功率为代理值 1.0（SYNC 强一致假设，`loopbackProxyMode=true`）。阈值 config-gated，越限可经查询接口 `getRuntimeMetrics` @BizQuery 检出。Micrometer/大盘/告警通道按 Deferred 触发条件后继。

Closure Audit Evidence:

- 独立结束审计 ses_0d2b31dc1ffewKa34u3FxpaF6B（general 子代理，新会话）VERDICT: **PASS**——逐项核实：§运行监控漂移已修正 + §裁决3 新增且未碰 5.1 节；四指标实现与设计一致（公式/方向/阈值门控）；测试 2 项全过；ORM 保护区域零触及（`app-erp-finance.orm.xml` 无 diff）；无新 pom 依赖；roadmap 5.3 已标 done；Nop 平台正确性（@Inject 非 private、IServiceContext 末参、@BizQuery 双侧、跨实体 COUNT 注释说明）全过。无阻塞。
- 验证基线（全绿）：`mvn clean install -DskipTests`（根，146 reactor 模块）BUILD SUCCESS；`mvn test -pl module-finance/erp-fin-service -am` Tests run: 112, Failures: 0, Errors: 0, Skipped: 0（含 `TestErpFinPostingMetrics` 2 测试）。

Follow-up:

- 告警通道对接与升级链（见上方 Deferred）
- 监控大盘可视化（见上方 Deferred）
- 上游 metrics 模块回迁（见上方 Deferred）
