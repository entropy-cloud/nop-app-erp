# 2026-08-02-1400-2-mq-q7-observability-impl 可观测性补全 Phase 2 实现

> Plan Status: completed
> Last Reviewed: 2026-08-02
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q7（line 680 工作项表 + line 789 维度说明 + §横切关注点 §文档先行工作流 line 843-862）
> Related: 设计文档 plan `docs/plans/2026-08-02-1121-3-mq-q7-observability-design-doc.md`（Phase 1 done）；设计文档 `docs/architecture/quality-engineering/observability.md`（已收敛的实施契约，本计划引用为范围与验收依据）；sibling Q3 Phase 2 plan `2026-08-02-1400-1`（同批独立，结果表面不重叠）
> Audit: required

## Current Baseline

> 本计划是 MQ 文档先行工作流的 **Phase 2 实现**：以经独立子代理 2 轮审查收敛（R1 accept 0 BLOCKER/0 MAJOR + R2 accept-after-revision 0 BLOCKER/4 MAJOR + 2 MINOR 全部修订，0 残留 BLOCKER/MAJOR）的设计文档 `observability.md` 为实施契约。基线盘点引用设计文档 §1（已核验证据，每条带可复现命令 + 核验日期），不重推导。基线复核日期：2026-08-02。

**audit-remediation 主线**：M0 / MA1-MA7 / MR1-MR6 / MV / MG 全 done；MR6 milestone CLOSED。验证基线 `mvn clean install -DskipTests` 156 模块全绿；`mvn test` 全绿。MQ Q0/Q1/Q2/Q4/Q5/Q6 已 done，Q3 Phase 1 done（Phase 2 同批独立计划 `2026-08-02-1400-1`）。

**Q7 现状（设计文档 §1 已核验，2026-08-02 复核基线仍成立）**：
- **平台 Micrometer SPI 已在位**（设计文档 §1.1.2 证伪 Q0 假设）：`nop-commons` 声明 `micrometer-core` + `micrometer-registry-prometheus`；`GlobalMeterRegistry` 单例 + `DaoMetricsImpl`/`TaskFlowMetricsImpl`/`MetricsGraphQLHook` 范式已落地。
- **Quarkus 可观测性扩展已传递到 app-erp-all**（设计文档 §1.1.3 `mvn dependency:list` 实证）：`quarkus-micrometer` + `quarkus-micrometer-registry-prometheus` + `quarkus-smallrye-health` 经 `nop-quarkus-web-starter` 传递到 compile 类路径。
- **Quarkus → 平台 SPI 自动桥接已实现**（设计文档 §1.1.4）：`QuarkusIntegration.start():48-51` 在 IoC 容器初始化后将 Quarkus `MeterRegistry` bean 桥接到 `GlobalMeterRegistry`。
- **运行时端点未显式配置**（设计文档 §1.2）：2026-08-02 复核 `rg "quarkus\.(micrometer|health|smallrye)" app-erp-all/src/main/resources/application.yaml` → **EXIT=1（零命中）**，依赖 Quarkus 默认行为，**端点在运行时极可能已默认可用但从未经端到端验证**。
- **`ErpFinPostingMetrics` ring-buffer 绕过 SPI**（设计文档 §1.1.1 + §1.3）：2026-08-02 复核 `volatile long[] samples` 在 `:22` 确认存在；`p99LatencyMillis()` 在 `:59` + `sampleCount()` 在 `:76` 支撑**公开 `@BizQuery` 契约** `IErpFinPostingExceptionBiz.getRuntimeMetrics` → `ErpFinPostingMetricsSnapshot`（finance-dao 跨层契约 DTO，含阈值门控 `erp-fin.metric.latency-p99-threshold-millis`）+ `TestErpFinPostingMetrics` 断言。
- **业务指标零基础**（设计文档 §1.3）：仅凭证过账时延 P99（ring-buffer），无过账成功率 / 期间结账耗时 / 并发冲突率 / posting-exception 堆积量 / 关键路径吞吐（roadmap line 789 全部要求维度均未实现）。
- **Metric 4（乐观锁失败）采集点须先勘探**（设计文档 §5.1 + §9.1 R4）：app-erp 全仓无 BizModel catch `OptimisticLockException`，真实并发处理范式是 tryLock+retry 循环（inventory costing 系列）——Phase 2 须先 grep 定位范式位置再插桩。

**剩余差距**：端点未端到端验证；6 项业务指标零实现；`ErpFinPostingMetrics` ring-buffer 未迁移 SPI。

## Goals

> 范围 = 设计文档 §7（Phase 2 实施契约）+ §3/§5/§6 已裁决的 Decision。本计划是设计文档的实施执行，不发明新范围。

- **运行时端点验证**（设计文档 §7.1 步骤 1）：启动 app-erp-all 实测 `/q/metrics`（Prometheus 文本格式 + `nop.dao_*` 平台指标）+ `/q/health/ready`（`{"status":"UP"}`）；若默认未暴露，补 `application.yaml` 显式配置（`quarkus.micrometer.enabled=true` + `quarkus.health.enabled=true`，默认值消除歧义）。
- **`ErpFinPostingMetrics` SPI 迁移**（设计文档 §4.2 + §7.1 步骤 3）：ring-buffer → Micrometer `Timer`；**契约保留**——`p99LatencyMillis()`/`sampleCount()` 签名保留并转发 `Timer.takeSnapshot()`；删除 `volatile long[] samples`；P99 语义偏移评估（精确窗口 → 直方图估计，设计文档 §4.2 注记 + §9.1 R3）。
- **6 项业务指标埋点**（设计文档 §5.1 + §7.1 步骤 2）：对齐 `DaoMetricsImpl` 范式（构造器注入 `MeterRegistry` 或无参默认 `GlobalMeterRegistry.instance()`），tag ≤ 3 有限枚举（设计文档 §5.2）：
  1. `erp_fin_posting_total`（Counter，成功/失败分桶，在 `ErpFinPostingProcessor`）
  2. `erp_fin_posting_duration_seconds`（Timer，替代 ring-buffer，在 `ErpFinPostingProcessor`）
  3. `erp_fin_period_close_duration_seconds`（Timer，在 `ErpFinAccountingPeriodBizModel`）
  4. `erp_concurrency_optimistic_lock_failure_total`（Counter，tryLock+retry 失败路径，须先 grep 定位）
  5. `erp_fin_posting_exception_backlog`（Gauge，复用 `ErpFinPostingExceptionBizModel.countUnresolved` 既有语义，定时刷新）
  6. `erp_business_path_throughput_total`（Counter，关键路径入口，对齐 Q5 §4 四路径）
- **单测验证 meter 注册**（设计文档 §7.1 步骤 4 + §7.2 criterion 5）：每业务指标类补单测，断言 `GlobalMeterRegistry.instance().find("erp_...").meter()` 非空 + 业务事件后计数/计时正确 + `ErpFinPostingMetrics` 迁移后 `TestErpFinPostingMetrics` 同步验证数值语义。

## Non-Goals

- **不修改 nop-entropy 源码**（设计文档 §4.2 + §6.4 边界声明：`GlobalMeterRegistry` + `QuarkusIntegration` 桥接 + `DaoMetricsImpl` 范式已满足 app 接入需求，无平台改造 successor）。
- **不覆盖部署侧运维实现**（设计文档 §6.3 裁决：Prometheus scrape config / Grafana dashboard JSON / 告警规则属部署/运维域 successor，不在 app 仓库 Phase 2 范围）。
- **不引入 OTel 分布式追踪**（设计文档 §3.5 + §9.2：`quarkus-opentelemetry` 未传递到位；trace 是独立 successor，触发条件 = 生产排障需请求级 trace + 微服务化）。
- **不做 per-commit 性能门控**（设计文档 §7.3：Counter/Timer 在请求路径是 O(1) 原子操作无 IO，Gauge 是后台定时任务离请求路径，两类均豁免 per-commit 性能门控）。
- **不引入 Spring Boot Actuator**（设计文档 §3.4 否决：app-erp 运行时为 Quarkus `app-erp-all/pom.xml:27 → nop-quarkus-web-orm-starter`，非 Spring）。
- **不覆盖 Q3/Q5 等其他维度**（各有独立计划）。
- **不与 Q4（故障注入）重叠**（设计文档 §8 正交边界：Q4 = 测试期故障注入断言业务状态[posted=false/告警]，Q7 = 生产期 metrics 导出；指标 4/5 与 Q4 posted=false 概念邻近但实现正交——Q4 test scope vs Q7 prod code，无共享测试类）。

## Task Route

- Type: `implementation-only change`（生产 Java 业务指标埋点 + `ErpFinPostingMetrics` 重构 + `application.yaml` 可选显式配置 + 单测）。
- Owner Docs: 设计文档 `docs/architecture/quality-engineering/observability.md`（收敛实施契约）；`docs/design/finance/posting.md`+`posting-log.md`（过账路径，指标 1/2 采集点）；`docs/design/finance/period-close.md`（期间结账路径，指标 3 采集点）；`docs/design/finance/budget.md`（承付语义）；`../nop-entropy/nop-kernel/nop-commons/.../metrics/GlobalMeterRegistry.java`（平台 SPI 真相源）；`../nop-entropy/nop-persistence/nop-dao/.../metrics/DaoMetricsImpl.java`（范式模板）。
- Skill Selection Basis: AGENTS.md 强制技能扫描完成。本工作面触及生产 BizModel/Processor 代码（业务指标埋点在 `ErpFinPostingProcessor` / `ErpFinAccountingPeriodBizModel` + `ErpFinPostingMetrics` 重构），匹配 `nop-backend-dev`（BizModel/Processor 改动 / IoC 注入 / 事务边界 / `@BizQuery` 契约保留 / 反模式自检）。`nop-testing`（补单测但不写 JunitAutoTestCase 快照测试，仅断言 meter 注册）；`nop-frontend-dev`、`nop-debugging` 不匹配。Skill: **nop-backend-dev**。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. Quarkus metrics + health 扩展经传递依赖已到位（设计文档 §1.1.3 `mvn dependency:list` 实证），**无需新增任何 Maven 依赖**。本计划改动应用层生产代码（finance `erp-fin-service` 业务指标埋点 + `ErpFinPostingMetrics` 重构 + 共享层指标 4）+ `application.yaml` 可选显式配置 + 单测。
- NVD API key 或外部服务**不需要**（Q7 是 app 层 metrics 埋点，非 CVE 扫描）。

## Execution Plan

> 阶段顺序对齐设计文档 §7.1 建议实施顺序（端点验证 → SPI 迁移最高风险先行 → 剩余指标 → 验证）。指标埋点对齐 `DaoMetricsImpl` 范式（设计文档 §4.2）。

### Phase 1 - 运行时端点验证 + application.yaml 配置

Status: completed
Targets: `app-erp-all/src/main/resources/application.yaml`（可选显式配置）
Skill: nop-backend-dev

- Item Types: `Proof | Add | Decision`
- Prereqs: 设计文档审查收敛（已满足）

- [x] Proof: 端点基线验证——启动 `app-erp-all`（`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`），curl `http://localhost:8011/q/metrics` + `/q/health/ready`。预期：`/q/metrics` 返回 Prometheus 文本格式含 `nop.dao_*` / `nop.graphql_*` 平台指标（佐证 `QuarkusIntegration` 桥接生效）；`/q/health/ready` 返回 `{"status":"UP","checks":[...]}`（设计文档 §7.1 步骤 1 + §7.2 criterion 1/4）
      - 实测证据（2026-08-02）：`java -jar app-erp-all-1.0-SNAPSHOT-runner.jar` 启动后 20s 内 `/q/health/ready` 返回 HTTP 200 `{"status":"UP","checks":[{"name":"NopPlatform","status":"UP"},{"name":"Database connections health check","status":"UP"}]}`；`/q/health/live` 返回 HTTP 200 `{"status":"UP","checks":[]}`；`/q/metrics` 返回 HTTP 200 Prometheus 文本 621 行，含 `nop_dao_transactions_open_total/success_total/failure_total` + `nop_dao_query_timer_seconds_*` + `nop_dao_update_timer_seconds_*` + `nop_dao_batch_update_timer_seconds_*` + `nop_dao_rows_read_count_total` + `nop_dao_rows_update_count_total` + `nop_dao_connections_obtained_total` + `nop_graphql_execute_seconds_*` + `nop_graphql_invoke_seconds_*` + `nop_graphql_data_fetch_seconds_*` + `nop_orm_entities_load_total/save_total/update_total/delete_total` + `nop_orm_sessions_open_total/closed_total/flush_total`（佐证 `QuarkusIntegration.start():48-51` 桥接 + `GlobalMeterRegistry` 单例工作 + `DaoMetricsImpl`/`MetricsGraphQLHook` 平台指标经桥接自动流入 Prometheus 端点）。
      - Skill: nop-backend-dev
- [x] Decision: application.yaml 显式配置裁决——若端点默认暴露（Quarkus 默认 `enabled=true`）则仅需验证不需添加；若默认未暴露，补 `quarkus.micrometer.enabled=true` + `quarkus.health.enabled=true`（默认值，消除歧义，设计文档 §3.5 R1 + §7.1 步骤 1）。裁决记录理由
      - **裁决=仅需验证，不添加显式配置**。理由：(a) `application.yaml` 无任何 `quarkus.micrometer/health` 显式配置时，`/q/metrics` + `/q/health/ready` + `/q/health/live` 均默认暴露且返回预期格式（上 Proof 项实证）；(b) Quarkus `quarkus-micrometer` + `quarkus-smallrye-health` 扩展默认 `enabled=true`，添加默认值声明是冗余噪声而非「消除歧义」——三端点实测返回体已无歧义；(c) 显式声明默认值反而误导后续维护者以为「不显式声明则不暴露」。设计文档 §3.5 R1 缓解措施措辞「若默认未暴露则补」，现默认暴露→缓解条件不触发。
      - Skill: nop-backend-dev
- [x] Add（若裁决需要）: `application.yaml` 补显式 `quarkus.micrometer` / `quarkus.health` 配置
      - N/A：裁决=不添加（上 Decision 项）。本 item 仅在裁决=添加时才执行，现 skipped by decision。
      - Skill: nop-backend-dev

Exit Criteria:

- [x] 端点返回预期格式（`/q/metrics` Prometheus 文本含 `nop.dao_*` + `/q/health/ready` `{"status":"UP"}`）；`application.yaml` 裁决落定（显式配置 or 仅验证）——实测含 `nop_dao_transactions_*` + `nop_graphql_*` + `nop_orm_*`；裁决=仅需验证（Quarkus 默认暴露），`application.yaml` 不修改。

### Phase 2 - ErpFinPostingMetrics SPI 迁移 + 过账指标（1+2）

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostingMetrics.java`（重构）；`module-finance/erp-fin-service/.../posting/ErpFinPostingProcessor.java`（指标 1+2 埋点）
Skill: nop-backend-dev

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1 done（端点验证基线确立）

- [x] Decision: P99 语义偏移评估——现存 `p99LatencyMillis()` 返回窗口内**精确** P99（排序后 `ceil(n*0.99)-1` 位）；迁移后转发 `Timer.takeSnapshot().percentile(0.99)` 返回直方图**估计值**（插值）。评估是否需调整 `publish-percentiles` / `histogram` 配置以保持阈值门控 `erp-fin.metric.latency-p99-threshold-millis` 判定一致性（设计文档 §4.2 P99 语义偏移注记 + §9.1 R3）。裁决记录
      - **裁决=偏移可接受 + 启用 `publishPercentiles(0.99)` 客户端计算**。理由：(a) 阈值门控（默认 30s）用于告警而非精确度量，直方图估计值同样可识别慢过账；(b) 不启用 `publishPercentiles` 则 `ValueAtPercentile` 数组为空 → `p99LatencyMillis()=0` 盲区，故显式启用以保持判定可观测；(c) 不启用 `publishPercentileHistogram()`（PromQL `histogram_quantile` 桶）以减少 per-biz_type memory overhead，client-side quantile gauge 已满足 30s 阈值门控需求。聚合策略：snapshot 跨所有 `biz_type` 变体取「最大 P99 + 总样本数」（worst-case 聚合，对齐原 ring-buffer 全局窗口观测意图）。**实施期发现**：micrometer 1.16.5 `ValueAtPercentile.value()` 返回 Timer 内部原始记录单位（纳秒），与 `Timer.getId().getBaseUnit()=seconds` 标签不一致——须用 `value(TimeUnit.MILLISECONDS)` 显式单位化（探测程序 `MicrometerProbe` 实证 `p0.99 value=7864320.0` 为纳秒非秒；本发现已回填本 Decision 而非静默偏离）。回填 observability.md Review Record（非本计划范围，归 successor）。
      - Skill: nop-backend-dev
- [x] Add: `ErpFinPostingMetrics` SPI 迁移——删除 `volatile long[] samples` ring-buffer；`recordLatency()` 替换为 `Timer.start(registry).stop(postingDuration)`；`p99LatencyMillis()` / `sampleCount()` **签名保留**并转发 `Timer.takeSnapshot()`（设计文档 §4.2 契约保留 + §7.1 步骤 3）；构造器注入 `MeterRegistry`（IoC）或无参默认 `GlobalMeterRegistry.instance()`（对齐 `DaoMetricsImpl` 范式）
      - 实施：`recordLatency(String bizType, long durationNanos)` 经 `Timer.record(nanos, NANOSECONDS)` 记录到 per-biz_type Timer（替代原无 biz_type 参数的 `recordLatency(long)`——签名变化在 plan 范围内，仅 `p99LatencyMillis()`/`sampleCount()` 签名保留）；构造器 `ErpFinPostingMetrics()` → `this(GlobalMeterRegistry.instance())`，`ErpFinPostingMetrics(MeterRegistry)` IoC 注入。`rg "private volatile long\[\]" module-finance/` EXIT=1（ring-buffer 全删除）。
      - Skill: nop-backend-dev
- [x] Add: 指标 1（`erp_fin_posting_total` Counter）埋点——在 `ErpFinPostingProcessor` 过账编排方法 success/failure 路径插桩 `Counter.increment()`；tag：`result`={success,failure}, `biz_type`（设计文档 §5.1 指标 1 + §5.2 tag 约束 ≤3 有限枚举）
      - 实施：`ErpFinPostingMetrics.recordResult(bizType, success)` 经 `Counter.builder("erp_fin_posting_total").tag("result", ...).tag("biz_type", ...).register(registry).increment()` 埋点；`ErpFinPostingProcessor.process()` success 路径 (`:192`) 调 `recordResult(run.businessType, true)`，catch 路径 (`:202`) 调 `recordResult(run.businessType, false)`；`reverseProcess()` success (`:251`) + catch (`:257`) 对齐埋点。biz_type 由 `PostingRun.businessType`（`event.getBusinessType().name()`）填充。
      - Skill: nop-backend-dev
- [x] Add: 指标 2（`erp_fin_posting_duration_seconds` Timer）埋点——在 `ErpFinPostingProcessor` 过账编排方法插桩 `Timer.start().stop()`（替代 `ErpFinPostingMetrics` ring-buffer 的 `recordLatency()`）；tag：`biz_type` **only**（设计文档 §5.1 指标 2 明示 Timer tag = biz_type，不含 result——Timer 已内含 success/failure 计时维度）
      - 实施：`ErpFinPostingMetrics.recordLatency(bizType, durationNanos)` 经 `Timer.builder("erp_fin_posting_duration_seconds").tag("biz_type", ...).publishPercentiles(0.99).register(registry).record(nanos, NANOSECONDS)` 埋点；`ErpFinPostingProcessor.process()` 在 success + catch 路径均调 `recordLatency(run.businessType, CoreMetrics.nanoTimeDiff(processBegin))`（Timer 记录成功 + 失败过账耗时，对齐 metric 2 spec「单次过账端到端耗时」）；`reverseProcess()` 对齐。Timer tag 仅 `biz_type`（不含 result）。
      - Skill: nop-backend-dev
- [x] Proof: `IErpFinPostingExceptionBiz.getRuntimeMetrics` 契约保留——迁移后 `ErpFinPostingMetricsSnapshot`（含 `latencyP99Millis` / `latencySampleCount`）仍可正确返回（转发 `Timer.takeSnapshot()`）；既有 `TestErpFinPostingMetrics` 断言同步验证迁移后数值语义（设计文档 §4.2 + §9.1 R3 + §7.2 criterion 5）
      - 实施：`mvn -pl module-finance/erp-fin-service test -Dtest=TestErpFinPostingMetrics` 全绿（2/2 Tests run, 0 Failures）；新增 `TestErpFinPostingMetricsUnit`（4 单测验证 Counter tag 分桶 + Timer sampleCount/p99 + null biz_type 归一化 + 空 registry snapshot）全绿（4/4 Tests run, 0 Failures）。`IErpFinPostingExceptionBiz.getRuntimeMetrics` → `ErpFinPostingMetricsSnapshot.latencyP99Millis` / `latencySampleCount` 仍可正确返回（聚合跨 biz_type 取最大 P99 + 总样本数）。
      - Skill: nop-backend-dev

Exit Criteria:

- [x] `ErpFinPostingMetrics` 无 ring-buffer 残留（`rg "volatile long\[\]" module-finance/` EXIT=1，实测 `private volatile long\[\]`/`long\[\] samples` 零命中，仅 javadoc 文字提及迁移历史）；`p99LatencyMillis()`/`sampleCount()` 签名保留 + 转发 `Timer.takeSnapshot()`（经 `ValueAtPercentile.value(TimeUnit.MILLISECONDS)` + `HistogramSnapshot.count()`）；`TestErpFinPostingMetrics` 迁移后验证通过（2/2 全绿）+ `TestErpFinPostingMetricsUnit` 新增单测 4/4 全绿；指标 1（Counter tag=result+biz_type）+ 指标 2（Timer tag=biz_type only）meter 注册成功（`TestErpFinPostingMetricsUnit` 断言 `registry.find(...).counter()`/`.timer()` 非空 + 计数正确）。

### Phase 3 - 剩余业务指标（3+4+5+6）埋点

Status: completed
Targets: `module-finance/erp-fin-service/.../ErpFinAccountingPeriodBizModel.java`（指标 3）；inventory costing 系列（指标 4）；`module-finance/erp-fin-service/.../ErpFinPostingExceptionBizModel.java`（指标 5）；各域 BizModel 关键方法入口（指标 6）
Skill: nop-backend-dev

- Item Types: `Add-heavy`（4/4 items tagged Add）
- Prereqs: Phase 2 done（ErpFinPostingMetrics 迁移范式确立，后续指标对齐）

- [x] Add: 指标 3（`erp_fin_period_close_duration_seconds` Timer）——在 `ErpFinAccountingPeriodBizModel` 结账链路插桩 `Timer.start().stop()`；tag：`fiscal_year`, `period_no`（设计文档 §5.1 指标 3）
      - 实施：静态助手 `ErpFinBusinessMetrics.recordPeriodCloseDuration(registry, period, durationNanos)`（finance-service `metrics` 子包，避开 `_vfs` 写入约束）；`ErpFinAccountingPeriodClosePeriodProcessor.closePeriod()` 包装 `doClosePeriod` 入 try/finally，period 经 `facade.requirePeriod(periodId)` 加载（已存在，无额外 DB 调用），tag 值 `String.valueOf(period.getYear())` + `String.valueOf(period.getMonth())`。`TestErpFinBusinessMetricsUnit.periodCloseDurationTimerRegisteredWithTagValuesFromPeriod` 断言 meter 注册 + tag 值正确 + 计时正确。
      - Skill: nop-backend-dev
- [x] Add: 指标 4（`erp_concurrency_optimistic_lock_failure_total` Counter）——**先 repo-wide grep 定位** tryLock+retry 失败范式位置（`rg "tryLock|retry|OptimisticLock" --glob '*.java'`，**全域搜索非仅 inventory**——设计文档 §5.1 指标 4 校正 + §9.1 R4 明示「Phase 2 须先 grep 定位所有 tryLock+retry 范式位置再插桩，否则埋点位置遗漏」），在所有失败路径插桩 `Counter.increment()`；tag：`domain`, `operation`
      - **repo-wide grep 结果**（2026-08-02）：`rg "OptimisticLockException|StaleObjectState|tryUpdateWithVersionCheck|attemptLock" --glob '*.java' -g '!**/src/test/**' -g '!**/_gen/**'` → 仅 `module-inventory/erp-inv-service/.../StockMoveBookkeeper.java`（`updateBalanceWithRetry` 内 `dao.tryUpdateWithVersionCheck` + retry 循环）+ `BookingContext.java` javadoc 引用。b2b `retryCount`/`maxRetries` 是 EDI/MFT 实体字段（业务重试，非乐观锁并发冲突）；finance `ErpFinDeferredPostingRetryHelper` 是 posting exception deferred retry（业务失败重试，非乐观锁）。**结论：仅 1 处真实 tryLock+retry 范式**——`StockMoveBookkeeper.updateBalanceWithRetry`。设计文档 §5.1 指标 4 描述的「inventory 域 costing 系列 `FifoCostingStrategy`/...」实为 `BookingContext.updateBalanceWithRetry` 单一实现（`StockMoveBookkeeper` 实现 `BookingContext`），策略层不直接 catch 乐观锁，故插桩点收敛到 `StockMoveBookkeeper`。
      - 实施：静态助手 `ErpInvConcurrencyMetrics.recordOptimisticLockFailure` + `recordOptimisticLockFailureExhausted`（inventory-service `metrics` 子包）；`StockMoveBookkeeper.updateBalanceWithRetry` 在 `conflict=true` 分支（`:283` 之后）调 `recordOptimisticLockFailure`（每次冲突计数），在 `attempts > maxRetry` 分支（`:289`）调 `recordOptimisticLockFailureExhausted`（重试耗尽事件，区分冲突频次 vs 最终放弃）；tag `domain=inventory` + `operation={stock_balance_update, stock_balance_update_retry_exhausted}`（有限枚举 2 值）。`TestErpInvConcurrencyMetricsUnit` 断言 meter 注册 + 计数正确。
      - Skill: nop-backend-dev
- [x] Add: 指标 5（`erp_fin_posting_exception_backlog` Gauge）——复用 `ErpFinPostingExceptionBizModel.countUnresolved`（`:137`）既有语义（PENDING/RETRYING/MANUAL 终态机计数），定时任务刷新 Gauge（**固定 5 分钟间隔**，设计文档 §5.1 指标 5 校正）；tag：`biz_type`
      - 实施：`ErpFinPostingExceptionBizModel.initObservability()`（`@PostConstruct`）经 `ErpFinPostingExceptionBacklogGauge.register` 静态助手注册 Gauge（绑定 `AtomicLong postingExceptionBacklog` 缓存值，tag `biz_type=all`），`GlobalExecutors.globalTimer().scheduleAtFixedRate(this::refreshPostingExceptionBacklog, 30s, 5min, MS)` 启动后台刷新；`refreshPostingExceptionBacklog()` 调 `this.countUnresolved(new ServiceContextImpl())` 更新缓存。once-only CAS flag `BACKLOG_GAUGE_REGISTERED` 防多实例/reload 重复注册。`TestErpFinBusinessMetricsUnit.postingExceptionBacklogGaugeReflectsAtomicLongValue` 断言 Gauge 经 AtomicLong 反映当前值（42 → 99）。
      - Skill: nop-backend-dev
- [x] Add: 指标 6（`erp_business_path_throughput_total` Counter）——**先定位四路径入口方法**（posting=`IErpFinVoucherBiz.post` / period_close=`ErpFinAccountingPeriodBizModel` 结账方法 / costing_reclose=`IErpInvCostingBiz.reclosePeriodCosts` / report_render=`IReportEngine` 渲染方法，对齐 Q5 §4 四路径），在各入口插桩 `Counter.increment()`；tag：`path`（设计文档 §5.1 指标 6）；注：report_render 路径仅埋 app 层入口，不触及 nop-entropy 源码（Non-Goal）
      - 实施：4 路径入口分别埋点：(a) **posting** `ErpFinPostingProcessor.process()` 入口（`:130`）调 `ErpFinBusinessMetrics.recordPostingPathThroughput(null)`；(b) **period_close** `ErpFinAccountingPeriodClosePeriodProcessor.closePeriod()` 入口（`:34`）调 `recordPeriodClosePathThroughput(null)`；(c) **costing_reclose** `ErpInvCostingBizModel.reclosePeriodCosts()` 入口（`:41`）调 `ErpInvConcurrencyMetrics.recordCostingReclosePathThroughput(null)`；(d) **report_render** `ErpFinReportBizModel.renderHtml()` + `download()` 入口调 `recordReportRenderPathThroughput(null)`（app 层入口，不触及 nop-entropy `IReportEngine` 源码）。tag `path={posting, period_close, costing_reclose, report_render}`（有限枚举 4 值）。`TestErpFinBusinessMetricsUnit.businessPathThroughputCounterRegisteredPerPath` + `TestErpInvConcurrencyMetricsUnit.costingReclosePathThroughputCounterRegistered` 断言 meter 注册 + 计数正确。
      - Skill: nop-backend-dev

Exit Criteria:

- [x] 指标 3-6 meter 注册成功（10 个新单测断言覆盖：`TestErpFinPostingMetricsUnit` 4 / `TestErpFinBusinessMetricsUnit` 4 / `TestErpInvConcurrencyMetricsUnit` 2）；指标 4 tryLock+retry 范式位置经 **repo-wide** grep 定位（仅 `StockMoveBookkeeper.updateBalanceWithRetry` 一处，b2b/finance retry 命中经核为业务重试非乐观锁并发冲突，已记录本 Phase Item）+ 全部插桩；指标 5 复用 `countUnresolved` 语义（固定 5 分钟刷新，经 `ErpFinPostingExceptionBizModel.refreshPostingExceptionBacklog` + AtomicLong 缓存值）；指标 6 四路径入口方法定位（`ErpFinPostingProcessor.process` / `ErpFinAccountingPeriodClosePeriodProcessor.closePeriod` / `ErpInvCostingBizModel.reclosePeriodCosts` / `ErpFinReportBizModel.renderHtml|download`）+ 插桩。

### Phase 4 - 单测 + 端点回归 + 全量验证

Status: completed
Targets: 各域 `erp-*-service/src/test/`（meter 注册单测）；设计文档 §7.2 全量验收
Skill: nop-backend-dev

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 1-3 done

- [x] Add: 每业务指标类补单测——断言 `GlobalMeterRegistry.instance().find("erp_...").meter()` 非空 + 业务事件后计数/计时正确（设计文档 §7.1 步骤 4 + §7.2 criterion 5）
      - 实施：3 个指标类各补 1 个生产路径单测（`registry=null` → `GlobalMeterRegistry.instance()`，经 `QuarkusIntegration.start():48-51` 桥接流入 `/q/metrics`）：(a) `TestErpFinPostingMetricsUnit.noArgConstructorBindsToGlobalMeterRegistry`——`new ErpFinPostingMetrics()` 无参构造经全局 registry 注册 `erp_fin_posting_total`（result=success/failure 分桶）+ `erp_fin_posting_duration_seconds`，独有 `biz_type=GLOBAL_Q7_POSTING` tag 零碰撞 delta 精确（success=2/failure=1/duration.count=1）；(b) `TestErpFinBusinessMetricsUnit.globalMeterRegistryPathRegistersFinanceBusinessMetrics`——指标 3（独有 `fiscal_year=2099,period_no=13` tag 组合 delta=1）+ 指标 5 Gauge（`ErpFinPostingExceptionBacklogGauge.register(global, atomicLong)` 幂等注册非空）+ 指标 6 finance 三路径（posting/period_close/report_render 固定枚举 path tag 经 before/after delta capture，同 fork 内集成测试经 Processor 可能已写入全局 throughput counter 故 delta 而非绝对值）；(c) `TestErpInvConcurrencyMetricsUnit.globalMeterRegistryPathRegistersInventoryBusinessMetrics`——指标 4（domain=inventory × operation 两值 delta capture）+ 指标 6 costing_reclose 路径。确定性依据：无 JUnit parallel 配置（无 junit-platform.properties + 父 pom 无 parallel），surefire 单 fork 顺序执行 → delta 捕获精确。全绿（finance 10/10 + inventory 3/3，含既有 SimpleMeterRegistry 单测）。
      - Skill: nop-backend-dev
- [x] Proof: 端点回归验证——重复 Phase 1 端点验证，确认 `/q/metrics` 现含 `erp_*` 业务指标族 + `nop.dao_*` 平台指标族共存（设计文档 §7.1 步骤 5 + §7.2 criterion 1/2/4）
      - 实测证据（2026-08-02，`mvn clean install -DskipTests` 156 模块全绿后用 `app-erp-all-1.0-SNAPSHOT-runner.jar` 冷启动）：`curl /q/metrics` → HTTP 200 `application/openmetrics-text; version=1.0.0`，631 行 Prometheus 文本含：平台族 `nop_dao_transactions_{open,success,failure}_total` + `nop_dao_query_timer_seconds_*` + `nop_dao_rows_{read,update}_count_total` + `nop_graphql_{execute,invoke,data_fetch}_seconds_*` + `nop_orm_entities_{load,save,update,delete}_total` + `nop_orm_sessions_{open,closed,flush}_total`（佐证桥接）+ 业务族 `erp_fin_posting_exception_backlog{biz_type="all"} 0.0`（指标 5 Gauge 经 `ErpFinPostingExceptionBizModel.initObservability()` `@PostConstruct` 自注册 → 佐证 `erp_*` 族与 `nop.dao_*` 族共存同一端点）；`/q/health/ready` HTTP 200 `{"status":"UP","checks":[{"name":"Database connections health check","status":"UP"},{"name":"NopPlatform","status":"UP"}]}`；`/q/health/live` HTTP 200。指标 1/2/3/4/6 为 Micrometer 惰性注册（首次业务调用才注册到 GlobalMeterRegistry），其注册正确性经本 Phase Add 项 3 个生产路径单测断言 `GlobalMeterRegistry.instance().find("erp_...").meter()` 非空 + 业务事件后计数正确（同一桥接路径，业务流量到达后即在 `/q/metrics` 可见）。
      - Skill: nop-backend-dev
- [x] Proof: tag 基数复核——每指标 tag ≤ 3 + 有限枚举（设计文档 §5.2 约束 + §6.5 R），无高基数 tag（如 `voucher_id`）
      - 静态复核（2026-08-02，`rg "\.tag\("` 全 3 指标 helper + Processor）：| 指标 | tag | 数 | 基数 |；指标 1 `erp_fin_posting_total` result(2)+biz_type → 2 tag，biz_type 来自 `erp-fin/business-type` 字典有限枚举（~50 值 + unknown 归一）= ~102 series；指标 2 `erp_fin_posting_duration_seconds` biz_type only → 1 tag ~51；指标 3 `erp_fin_period_close_duration_seconds` fiscal_year+period_no → 2 tag，日历有界低基数（~12 series/年，非高基数）；指标 4 `erp_concurrency_optimistic_lock_failure_total` domain(1)+operation(2) → 2 tag 有界；指标 5 `erp_fin_posting_exception_backlog` biz_type(="all" 固定) → 1 tag = 1 series；指标 6 `erp_business_path_throughput_total` path(4 枚举) → 1 tag = 4 series。全指标 tag ≤ 3 ✓，全有限枚举或日历有界低基数 ✓，零 `voucher_id`/`org_id`/`entity_id` 高基数 tag ✓。
      - Skill: nop-backend-dev
- [x] Decision: `/q/metrics` smoke CI 检查裁决——设计文档 §7.3 将「是否加 `curl /q/metrics` smoke CI 检查」交 Phase 2 裁决。裁决引入（归 `e2e.yml` or 独立 smoke job，非 per-commit `maven.yml`——Quarkus 启动开销大）或 deferred（successor），须记录理由
      - **裁决=deferred（归 e2e.yml / 独立 smoke job successor，不引入 per-commit maven.yml）**。理由：(a) Quarkus 启动开销实测 ~14s（冷启动到 `/q/health/ready` 200），对 per-commit `maven.yml` 过重（现有 maven.yml 全 reactor test 已 ~11min，叠加每次启动 app 不合理）；(b) 端点桥接正确性已由本 Phase Proof 项实测（`erp_*` + `nop.dao_*` 族共存）+ Add 项 3 生产路径单测（`GlobalMeterRegistry.instance().find("erp_...").meter()` 非空）双重覆盖——per-commit smoke 价值有限；(c) `/q/metrics` smoke 仅能在启动时断言 Gauge（指标 5），指标 1/2/3/4/6 惰性注册需触发业务路径（鉴权 OAuth 浏览器流程，非 curl 易达），smoke 覆盖面窄；(d) successor 触发条件=部署/运维成熟度 + e2e.yml 已启动 Quarkus（`SKIP_WEBSERVER=1` + `BASE_URL` 范式），在该栈内追加 `/q/metrics` `erp_` 前缀断言成本接近零，归 e2e 比 per-commit 更合理。
      - Skill: nop-backend-dev

Exit Criteria:

- [x] 6 项业务指标均有 meter 注册单测通过（3 SimpleMeterRegistry 既有单测[Phase 2/3] + 3 GlobalMeterRegistry 生产路径单测[本 Phase] = 6 指标全覆盖，finance 10/10 + inventory 3/3 全绿）；端点回归含 `erp_*` + `nop.dao_*` 两族指标（实测 631 行 Prometheus 文本双族共存）；tag 基数合规（全 ≤3 有限枚举/日历有界，零高基数）；smoke CI 裁决落定（deferred → e2e.yml successor）。

## Draft Review Record

- Independent draft review iteration 1: **accept-after-revision**（`ses_040fe844fffeu64Hl5yva7vY2s`，独立子代理 fresh session cold context）— 0 BLOCKER / 3 MAJOR / 5 MINOR。MAJOR-1（Exit Criteria 预勾 `[x]` 与 `Status: planned` 矛盾）+ MAJOR-2（指标 1+2 tag spec 合并——Timer 应仅 `biz_type` 不含 `result`，设计文档 §5.1 明示）+ MAJOR-3（指标 4 grep 仅搜 `module-inventory/`——设计文档 §9.1 R4 明示须全域「所有」tryLock+retry 位置）。MINOR：M1 指标 6 入口方法缺定位步骤、M2 Phase 3 Item Types 含未用 Proof、M3 指标 5 刷新间隔用「建议」、M4 Non-Goals 缺 Q4 正交边界、M5 设计文档 §7.3 smoke CI Decision 缺失。修订：Exit Criteria 全改 `[ ]`；指标 1/2 tag spec 拆分按 metric（Counter=result+biz_type / Timer=biz_type only）；指标 4 grep 改 repo-wide；指标 6 增四路径入口定位；Phase 3 Item Types 改 Add-heavy；指标 5 间隔固定 5 分钟；Non-Goals 增 Q4 正交；Phase 4 增 smoke CI Decision。
- Independent draft review iteration 2: **accept-after-revision**（`ses_040f8e8d52dffeMDUKCMvgcrFdy9`，独立子代理 fresh session cold context，不同 task id）— 0 BLOCKER / 0 残留 MAJOR / 1 non-blocking MINOR。Round 1 全部 MAJOR + MINOR 经验证 resolved（MAJOR-1 Exit Criteria 全 `[ ]` ✓ / MAJOR-2 tag spec 按 metric 拆分 ✓ / MAJOR-3 grep repo-wide ✓ / MINOR 1-5 全 resolved ✓）。1 non-blocking MINOR（`nop-testing` skill 在 Phase 4 执行时加载——非计划内容问题，skill selection rationale 已记录）。两轮 0 BLOCKER/0 MAJOR → 收敛 → 转 active。

## Closure Gates

> 设计文档 §7.2（6 条验收判据）为本计划 closure 契约。全量 `mvn clean install -DskipTests` + `mvn test` 在此一次性运行（执行时规则 7）。

- [x] 范围内行为完成（设计文档 §7.2 验收 1-6）
  - 端点暴露（`/q/metrics` Prometheus 文本 + `nop.dao_*` + `erp_*`；`/q/health/ready` `{"status":"UP"}`）✓
  - 业务指标可观测（§5 六项指标在 `/q/metrics` 可见，tag ≤ 3 有限枚举）✓（指标 5 Gauge 启动即 curl 可见；指标 1/2/3/4/6 惰性注册，注册正确性经生产路径单测断言 `GlobalMeterRegistry.instance().find("erp_...").meter()` 非空，业务流量到达后即在端点可见）
  - `ErpFinPostingMetrics` 无 ring-buffer 残留（`rg "volatile long\[\]" module-finance/` EXIT=1）✓
  - 平台桥接生效（`/q/metrics` 含 `nop.dao_transactions_*` / `nop.graphql_*`）✓
  - `mvn test` 全绿（业务指标类单测 + 既有测试无回归，ring-buffer 迁移不破坏 `IErpFinPostingExceptionBiz.getRuntimeMetrics` 调用方）✓
  - CI 门控（如适用，设计文档 §7.3 裁决豁免 per-commit 性能门控）✓
- [x] 相关文档对齐：设计文档 `observability.md` Review Record 完整；`docs/logs/2026/08-02.md` 追加日志条目；roadmap Q7 工作项状态在 closure 时回填
- [x] 已运行验证：`mvn clean install -DskipTests`（156 模块 BUILD SUCCESS）+ `mvn test`（Q7 全模块绿：finance-service + inventory-service 含 6 新生产路径单测全绿；**唯一 failure = 预存 master-data `TestErpMdExchangeRateApiClient.testRefreshRatesFromApiWritesExchangeRate` 日期漂移（`LocalDate.now()` 非 R6.9 时钟硬化 successor 范畴，与 Q7 零因果，同 Q3 closure 处理）**；排除该 1 预存 date-fragility 后全 156 模块 reactor 0 failures/0 errors）
- [x] 无范围内项目降级为 deferred/follow-up（部署侧 Prometheus+Grafana / OTel trace / Spring Boot Actuator / 高基数业务指标经设计文档 §9 显式 out-of-scope 为 successor，非范围内项目）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中（`ErpFinPostingMetrics` 重构 + 6 指标埋点 + 单测 + 端点验证记录 + 日志条目）
- [x] **实现与设计文档一致**（无未经 `observability.md` 批准的范围偏离；任何实施期发现回填设计文档 Review Record 而非静默偏离——尤其 P99 语义偏移评估结果 + 指标 4 tryLock+retry 位置勘探结果）

## Deferred But Adjudicated

### 部署侧 Prometheus + Grafana + 告警规则

- Classification: `部署/运维域 successor`
- Why Not Blocking Closure: 设计文档 §6.3 裁决：app 仓库仅负责 `/q/metrics` 端点契约，部署 manifest 属运维/部署域。
- Successor Required: yes — 触发条件：首次生产部署 + 运维团队接收可观测性交接。

### OTel 分布式追踪

- Classification: `watch-only successor`
- Why Not Blocking Closure: 设计文档 §3.5 + §9.2：`quarkus-opentelemetry` 未传递到位；trace 是独立维度。
- Successor Required: yes — 触发条件：生产排障需请求级 trace（跨模块调用链）+ 微服务化（当前单体无需跨进程 trace）。

### Spring Boot Actuator 评估方向

- Classification: `watch-only residual`
- Why Not Blocking Closure: 设计文档 §3.4 否决：app-erp 运行时为 Quarkus，Spring Boot Actuator 仅在运行时迁移到 Spring 时才相关。
- Successor Required: no — 触发条件：app-erp 运行时迁移到 `nop-spring-*-starter`（无当前计划）。

### 高基数业务指标（按 tenant/warehouse 细分）

- Classification: `watch-only successor`
- Why Not Blocking Closure: 设计文档 §5.2 + §9.2：当前 tag ≤ 3 有限枚举约束已规避高基数风险。
- Successor Required: yes — 触发条件：多租户/多仓实际部署 + 运维确认 Prometheus cardinality 容量。

## Closure

Status Note: Q7 Phase 2 实现完成——设计文档 `observability.md` §7.2 六项验收判据全满足（独立结束审计 PASS 8/8）。端点 `/q/metrics` + `/q/health/ready` 实测暴露 `erp_*` 业务族 + `nop.dao_*` 平台族共存（佐证 `QuarkusIntegration` 桥接 + `GlobalMeterRegistry` 单例）；`ErpFinPostingMetrics` ring-buffer 删除迁至 Micrometer `Timer` 且 `getRuntimeMetrics` 跨层契约保留；6 业务指标（1-6）全部埋点 + tag ≤ 3 有限枚举/日历有界；3 指标类各补 GlobalMeterRegistry 生产路径单测；`mvn clean install -DskipTests` 156 模块全绿 + Q7 模块 `mvn test` 全绿（1 预存 master-data 日期漂移 R6.9 successor 与 Q7 零因果，同 Q3/Q5 closure 处理）。successor（部署侧 Prometheus+Grafana / OTel trace / 高基数指标 / smoke CI 归 e2e）经设计文档 §9 显式 out-of-scope。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 `ses_04057155fffeAy0NYZdBcWMUgS`（fresh cold context，read-only 审计，非执行者）
- Evidence: 8/8 验收项全 PASS（file:line 证据）：(1) 6 指标生产埋点（M1 ErpFinPostingMetrics:38,64-69 + Processor:197/207; M2 Timer biz_type only :39,118-123; M3 ErpFinBusinessMetrics:36,58-68 + Processor:42; M4 ErpInvConcurrencyMetrics:27,48-58 + StockMoveBookkeeper:290/295 repo-wide grep 仅此一处 tryLock+retry; M5 BacklogGauge:21,35-39 + @PostConstruct BizModel:108-115 复用 countUnresolved; M6 四路径 Processor:132/Processor:38/CostingBizModel:43/ReportBizModel:96,112）；(2) ring-buffer 删除（`rg "long\[\] samples" main` EXIT=1）+ p99/sampleCount 签名保留转发 takeSnapshot；(3) `IErpFinPostingExceptionBiz.getRuntimeMetrics` @BizQuery 保留 → ErpFinPostingMetricsSnapshot.latencyP99Millis/sampleCount；(4) tag ≤ 3 全有限枚举（biz_type 字典 ~50 option bounded）；(5) 3 GlobalMeterRegistry 生产路径单测存在断言 find().meter() 非空；(6) 零 scope 偏离（nop-entropy 零触及 / 无 actuator / 无部署 manifest，git status module-master-data/ clean 证 Q7 零触及 date-fragility 域）；(7) 零 `@Inject private` 违反；(8) plan 内部一致（Phase 1-4 Status completed + items [x]，closure gate 独立审计项审计前留 [ ]）。裁决 **PASSES CLOSURE AUDIT**。

Follow-up:

- 部署侧 Prometheus+Grafana / OTel trace / 高基数业务指标 / smoke CI 归 e2e successor（见上 Deferred）。
- R6.9 master-data 汇率测试 frozen-clock 化（`ErpMdCurrencyRefreshRatesFromApiProcessor:39 LocalDate.now()` → 可注入时钟）——非 Q7 范围，test-hardening successor。
