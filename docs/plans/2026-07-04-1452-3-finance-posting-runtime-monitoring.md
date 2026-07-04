# 2026-07-04-1452-3-finance-posting-runtime-monitoring 业财过账运行监控

> Plan Status: active
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

Status: planned
Targets: `docs/design/finance/posting-log.md`（§运行监控/§实现策略修正）、本计划
Skill: `nop-backend-dev`

- Item Types: `Decision | Explore`
- Prereqs: 5.1 active（埋点面与异常记录数据源确定）

- [ ] Explore：核实 app-erp-all 运行态是否已带 Spring Boot Actuator（pom/配置）；核实 5.1 异常记录载体形态（表/日志）以确定"异常率/自动化记账率"可否由 SQL 聚合得出；核实 `posted` 翻转可观测性（闭环成功率分子分母来源）。
      - Skill: `nop-backend-dev`
- [ ] Decision（监控落地路径），四选一并记录替代方案与残留风险：
  (a) 引入 Micrometer + Actuator，在 post/reverse 入口出口埋 counter/timer/gauge，经 `/q/metrics` 暴露（需 pom 依赖 + 部署侧抓取）；
  (b) 应用级指标快照——按窗口聚合 5.1 日志/异常记录到查询接口（零新依赖，但无实时推流）；
  (c) 持久化 `ErpFinPostingMetric` 快照表 + 定时 rollup（触及 finance 保护区域，需人工批准）；
  (d) 部分指标（自动化记账率/异常率/闭环成功率）走 (b) 日志衍生查询，时延走 (a) 或 `nanoTimeDiff` 采样。
      - Skill: `nop-backend-dev`
- [ ] Fix（owner-doc 漂移）：按 Decision 修正 `posting-log.md` §运行监控与 §实现策略 4 中"接入 nop-platform 监控大盘"表述为实际落地路径，注明平台无内建 metrics API（避免误导后续）。本计划编辑范围限定 §运行监控与 §实现策略 4，不触碰实现策略其他节（归 5.1），避免与 5.1 的 owner-doc 修订撞节。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 监控落地路径 Decision 写入 `posting-log.md`（修正后）与本计划，含替代方案/残留风险；owner-doc 漂移已修正

### Phase 2 - 指标采集与呈现

Status: planned
Targets: 按 Phase 1 Decision 选定（`ErpFinPostingProcessor` 埋点 / 指标聚合 BizModel / Micrometer 注册）、`posting-log.md`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1；若 Decision 选 (c) 持久化指标表，须 finance ORM 保护区域人工批准后方可实施

- [ ] Add：按 Decision 落地四指标采集——自动化记账率（自动凭证数÷总凭证数）、凭证生成时延（P99，复用 5.1 各阶段 `nanoTimeDiff`）、过账异常率（失败÷总尝试，复用 5.1 异常记录）、业财闭环成功率（`posted` 翻转成功÷过账成功）。
      - Skill: `nop-backend-dev`
- [ ] Add：指标呈现查询接口（`@BizQuery`）返回当前/窗口四指标值与阈值比对（≥95%/<30s/<1%/≥99.5%），阈值 config-gated。
      - Skill: `nop-backend-dev`
- [ ] Proof：测试——构造成功/失败/未过账样本，断言四指标值与阈值门控判定正确（达标/越限）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 四指标可采集且经查询接口呈现，阈值门控判定正确，测试通过

## Draft Review Record

- Independent draft review iteration 1: needs revision（ses_0d4159e6cffenc8xLYYJKef3B0，general 独立子代理）because Decision 选项 (c) 持久化指标表触及 finance ORM 保护区域，人工批准仅写在选项文本内，未接为 Phase 2 的条件 prereq 门（违反规则 9，与 Plan 1 Phase 4 写法不一致）。已修订：在 `Infrastructure And Config Prereqs` 与 Phase 2 Prereqs 显式加"若选 (c) 须人工批准"条件门 + 回滚策略。同时采纳非阻塞：Phase 1 Item Types 修正为 `Decision | Explore`；owner-doc 编辑边界限定 §运行监控/§实现策略 4（不碰其他节，避免与 5.1 撞节）。
- Independent draft review iteration 2: accept（共识达成，无阻塞）—— blocking 项已修订且与 Plan 1 保护区域门写法一致。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [ ] 范围内行为完成（四指标采集 + 呈现 + 阈值门控；owner-doc 漂移修正）
- [ ] 相关文档对齐（`posting-log.md` §运行监控修正；`core-business-roadmap.md` 5.3 标进展；当日日志）
- [ ] 已运行验证：`mvn clean install -DskipTests` + `mvn test -pl module-finance/erp-fin-service -am`（或按 Decision 受影响模块）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [ ] 结束证据存在于文件中

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

Status Note: 待实施完成后填写。若 Phase 1 Decision 裁定"全部指标可由 5.1 日志衍生查询零新依赖落地"，则以该最小落地 + owner-doc 修正关闭；Micrometer/大盘采纳按 Deferred 触发条件后继。

Closure Audit Evidence:

- 待独立结束审计。

Follow-up:

- 告警通道对接与升级链（见上方 Deferred）
- 监控大盘可视化（见上方 Deferred）
- 上游 metrics 模块回迁（见上方 Deferred）
