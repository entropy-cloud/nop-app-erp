# 2026-08-02-1400-2-mq-q7-observability-impl 可观测性补全 Phase 2 实现

> Plan Status: active
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

Status: planned
Targets: `app-erp-all/src/main/resources/application.yaml`（可选显式配置）
Skill: nop-backend-dev

- Item Types: `Proof | Add | Decision`
- Prereqs: 设计文档审查收敛（已满足）

- [ ] Proof: 端点基线验证——启动 `app-erp-all`（`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`），curl `http://localhost:8011/q/metrics` + `/q/health/ready`。预期：`/q/metrics` 返回 Prometheus 文本格式含 `nop.dao_*` / `nop.graphql_*` 平台指标（佐证 `QuarkusIntegration` 桥接生效）；`/q/health/ready` 返回 `{"status":"UP","checks":[...]}`（设计文档 §7.1 步骤 1 + §7.2 criterion 1/4）
      - Skill: nop-backend-dev
- [ ] Decision: application.yaml 显式配置裁决——若端点默认暴露（Quarkus 默认 `enabled=true`）则仅需验证不需添加；若默认未暴露，补 `quarkus.micrometer.enabled=true` + `quarkus.health.enabled=true`（默认值，消除歧义，设计文档 §3.5 R1 + §7.1 步骤 1）。裁决记录理由
      - Skill: nop-backend-dev
- [ ] Add（若裁决需要）: `application.yaml` 补显式 `quarkus.micrometer` / `quarkus.health` 配置
      - Skill: nop-backend-dev

Exit Criteria:

- [ ] 端点返回预期格式（`/q/metrics` Prometheus 文本含 `nop.dao_*` + `/q/health/ready` `{"status":"UP"}`）；`application.yaml` 裁决落定（显式配置 or 仅验证）

### Phase 2 - ErpFinPostingMetrics SPI 迁移 + 过账指标（1+2）

Status: planned
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostingMetrics.java`（重构）；`module-finance/erp-fin-service/.../posting/ErpFinPostingProcessor.java`（指标 1+2 埋点）
Skill: nop-backend-dev

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1 done（端点验证基线确立）

- [ ] Decision: P99 语义偏移评估——现存 `p99LatencyMillis()` 返回窗口内**精确** P99（排序后 `ceil(n*0.99)-1` 位）；迁移后转发 `Timer.takeSnapshot().percentile(0.99)` 返回直方图**估计值**（插值）。评估是否需调整 `publish-percentiles` / `histogram` 配置以保持阈值门控 `erp-fin.metric.latency-p99-threshold-millis` 判定一致性（设计文档 §4.2 P99 语义偏移注记 + §9.1 R3）。裁决记录
      - Skill: nop-backend-dev
- [ ] Add: `ErpFinPostingMetrics` SPI 迁移——删除 `volatile long[] samples` ring-buffer；`recordLatency()` 替换为 `Timer.start(registry).stop(postingDuration)`；`p99LatencyMillis()` / `sampleCount()` **签名保留**并转发 `Timer.takeSnapshot()`（设计文档 §4.2 契约保留 + §7.1 步骤 3）；构造器注入 `MeterRegistry`（IoC）或无参默认 `GlobalMeterRegistry.instance()`（对齐 `DaoMetricsImpl` 范式）
      - Skill: nop-backend-dev
- [ ] Add: 指标 1（`erp_fin_posting_total` Counter）埋点——在 `ErpFinPostingProcessor` 过账编排方法 success/failure 路径插桩 `Counter.increment()`；tag：`result`={success,failure}, `biz_type`（设计文档 §5.1 指标 1 + §5.2 tag 约束 ≤3 有限枚举）
      - Skill: nop-backend-dev
- [ ] Add: 指标 2（`erp_fin_posting_duration_seconds` Timer）埋点——在 `ErpFinPostingProcessor` 过账编排方法插桩 `Timer.start().stop()`（替代 `ErpFinPostingMetrics` ring-buffer 的 `recordLatency()`）；tag：`biz_type` **only**（设计文档 §5.1 指标 2 明示 Timer tag = biz_type，不含 result——Timer 已内含 success/failure 计时维度）
      - Skill: nop-backend-dev
- [ ] Proof: `IErpFinPostingExceptionBiz.getRuntimeMetrics` 契约保留——迁移后 `ErpFinPostingMetricsSnapshot`（含 `latencyP99Millis` / `latencySampleCount`）仍可正确返回（转发 `Timer.takeSnapshot()`）；既有 `TestErpFinPostingMetrics` 断言同步验证迁移后数值语义（设计文档 §4.2 + §9.1 R3 + §7.2 criterion 5）
      - Skill: nop-backend-dev

Exit Criteria:

- [ ] `ErpFinPostingMetrics` 无 ring-buffer 残留（`rg "volatile long\[\]" module-finance/` EXIT=1）；`p99LatencyMillis()`/`sampleCount()` 签名保留 + 转发 `Timer.takeSnapshot()`；`TestErpFinPostingMetrics` 迁移后验证通过；指标 1（Counter tag=result+biz_type）+ 指标 2（Timer tag=biz_type only）meter 注册成功

### Phase 3 - 剩余业务指标（3+4+5+6）埋点

Status: planned
Targets: `module-finance/erp-fin-service/.../ErpFinAccountingPeriodBizModel.java`（指标 3）；inventory costing 系列（指标 4）；`module-finance/erp-fin-service/.../ErpFinPostingExceptionBizModel.java`（指标 5）；各域 BizModel 关键方法入口（指标 6）
Skill: nop-backend-dev

- Item Types: `Add-heavy`（4/4 items tagged Add）
- Prereqs: Phase 2 done（ErpFinPostingMetrics 迁移范式确立，后续指标对齐）

- [ ] Add: 指标 3（`erp_fin_period_close_duration_seconds` Timer）——在 `ErpFinAccountingPeriodBizModel` 结账链路插桩 `Timer.start().stop()`；tag：`fiscal_year`, `period_no`（设计文档 §5.1 指标 3）
      - Skill: nop-backend-dev
- [ ] Add: 指标 4（`erp_concurrency_optimistic_lock_failure_total` Counter）——**先 repo-wide grep 定位** tryLock+retry 失败范式位置（`rg "tryLock|retry|OptimisticLock" --glob '*.java'`，**全域搜索非仅 inventory**——设计文档 §5.1 指标 4 校正 + §9.1 R4 明示「Phase 2 须先 grep 定位所有 tryLock+retry 范式位置再插桩，否则埋点位置遗漏」），在所有失败路径插桩 `Counter.increment()`；tag：`domain`, `operation`
      - Skill: nop-backend-dev
- [ ] Add: 指标 5（`erp_fin_posting_exception_backlog` Gauge）——复用 `ErpFinPostingExceptionBizModel.countUnresolved`（`:137`）既有语义（PENDING/RETRYING/MANUAL 终态机计数），定时任务刷新 Gauge（**固定 5 分钟间隔**，设计文档 §5.1 指标 5 校正）；tag：`biz_type`
      - Skill: nop-backend-dev
- [ ] Add: 指标 6（`erp_business_path_throughput_total` Counter）——**先定位四路径入口方法**（posting=`IErpFinVoucherBiz.post` / period_close=`ErpFinAccountingPeriodBizModel` 结账方法 / costing_reclose=`IErpInvCostingBiz.reclosePeriodCosts` / report_render=`IReportEngine` 渲染方法，对齐 Q5 §4 四路径），在各入口插桩 `Counter.increment()`；tag：`path`（设计文档 §5.1 指标 6）；注：report_render 路径仅埋 app 层入口，不触及 nop-entropy 源码（Non-Goal）
      - Skill: nop-backend-dev

Exit Criteria:

- [ ] 指标 3-6 meter 注册成功；指标 4 tryLock+retry 范式位置经 **repo-wide** grep 定位 + 全部插桩；指标 5 复用 `countUnresolved` 语义（固定 5 分钟刷新）；指标 6 四路径入口方法定位 + 插桩

### Phase 4 - 单测 + 端点回归 + 全量验证

Status: planned
Targets: 各域 `erp-*-service/src/test/`（meter 注册单测）；设计文档 §7.2 全量验收
Skill: nop-backend-dev

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 1-3 done

- [ ] Add: 每业务指标类补单测——断言 `GlobalMeterRegistry.instance().find("erp_...").meter()` 非空 + 业务事件后计数/计时正确（设计文档 §7.1 步骤 4 + §7.2 criterion 5）
      - Skill: nop-backend-dev
- [ ] Proof: 端点回归验证——重复 Phase 1 端点验证，确认 `/q/metrics` 现含 `erp_*` 业务指标族 + `nop.dao_*` 平台指标族共存（设计文档 §7.1 步骤 5 + §7.2 criterion 1/2/4）
      - Skill: nop-backend-dev
- [ ] Proof: tag 基数复核——每指标 tag ≤ 3 + 有限枚举（设计文档 §5.2 约束 + §6.5 R），无高基数 tag（如 `voucher_id`）
      - Skill: nop-backend-dev
- [ ] Decision: `/q/metrics` smoke CI 检查裁决——设计文档 §7.3 将「是否加 `curl /q/metrics` smoke CI 检查」交 Phase 2 裁决。裁决引入（归 `e2e.yml` or 独立 smoke job，非 per-commit `maven.yml`——Quarkus 启动开销大）或 deferred（successor），须记录理由
      - Skill: nop-backend-dev

Exit Criteria:

- [ ] 6 项业务指标均有 meter 注册单测通过；端点回归含 `erp_*` + `nop.dao_*` 两族指标；tag 基数合规；smoke CI 裁决落定

## Draft Review Record

- Independent draft review iteration 1: **accept-after-revision**（`ses_040fe844fffeu64Hl5yva7vY2s`，独立子代理 fresh session cold context）— 0 BLOCKER / 3 MAJOR / 5 MINOR。MAJOR-1（Exit Criteria 预勾 `[x]` 与 `Status: planned` 矛盾）+ MAJOR-2（指标 1+2 tag spec 合并——Timer 应仅 `biz_type` 不含 `result`，设计文档 §5.1 明示）+ MAJOR-3（指标 4 grep 仅搜 `module-inventory/`——设计文档 §9.1 R4 明示须全域「所有」tryLock+retry 位置）。MINOR：M1 指标 6 入口方法缺定位步骤、M2 Phase 3 Item Types 含未用 Proof、M3 指标 5 刷新间隔用「建议」、M4 Non-Goals 缺 Q4 正交边界、M5 设计文档 §7.3 smoke CI Decision 缺失。修订：Exit Criteria 全改 `[ ]`；指标 1/2 tag spec 拆分按 metric（Counter=result+biz_type / Timer=biz_type only）；指标 4 grep 改 repo-wide；指标 6 增四路径入口定位；Phase 3 Item Types 改 Add-heavy；指标 5 间隔固定 5 分钟；Non-Goals 增 Q4 正交；Phase 4 增 smoke CI Decision。
- Independent draft review iteration 2: **accept-after-revision**（`ses_040f8e8d52dffeMDUKCMvgcrFdy9`，独立子代理 fresh session cold context，不同 task id）— 0 BLOCKER / 0 残留 MAJOR / 1 non-blocking MINOR。Round 1 全部 MAJOR + MINOR 经验证 resolved（MAJOR-1 Exit Criteria 全 `[ ]` ✓ / MAJOR-2 tag spec 按 metric 拆分 ✓ / MAJOR-3 grep repo-wide ✓ / MINOR 1-5 全 resolved ✓）。1 non-blocking MINOR（`nop-testing` skill 在 Phase 4 执行时加载——非计划内容问题，skill selection rationale 已记录）。两轮 0 BLOCKER/0 MAJOR → 收敛 → 转 active。

## Closure Gates

> 设计文档 §7.2（6 条验收判据）为本计划 closure 契约。全量 `mvn clean install -DskipTests` + `mvn test` 在此一次性运行（执行时规则 7）。

- [ ] 范围内行为完成（设计文档 §7.2 验收 1-6）
  - 端点暴露（`/q/metrics` Prometheus 文本 + `nop.dao_*` + `erp_*`；`/q/health/ready` `{"status":"UP"}`）✓
  - 业务指标可观测（§5 六项指标在 `/q/metrics` 可见，tag ≤ 3 有限枚举）✓
  - `ErpFinPostingMetrics` 无 ring-buffer 残留（`rg "volatile long\[\]" module-finance/` EXIT=1）✓
  - 平台桥接生效（`/q/metrics` 含 `nop.dao_transactions_*` / `nop.graphql_*`）✓
  - `mvn test` 全绿（业务指标类单测 + 既有测试无回归，ring-buffer 迁移不破坏 `IErpFinPostingExceptionBiz.getRuntimeMetrics` 调用方）✓
  - CI 门控（如适用，设计文档 §7.3 裁决豁免 per-commit 性能门控）✓
- [ ] 相关文档对齐：设计文档 `observability.md` Review Record 完整；`docs/logs/2026/08-02.md` 追加日志条目；roadmap Q7 工作项状态在 closure 时回填
- [ ] 已运行验证：`mvn clean install -DskipTests`（156 模块 BUILD SUCCESS）+ `mvn test`（0 failures/0 errors）
- [ ] 无范围内项目降级为 deferred/follow-up（部署侧 Prometheus+Grafana / OTel trace / Spring Boot Actuator / 高基数业务指标经设计文档 §9 显式 out-of-scope 为 successor，非范围内项目）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中（`ErpFinPostingMetrics` 重构 + 6 指标埋点 + 单测 + 端点验证记录 + 日志条目）
- [ ] **实现与设计文档一致**（无未经 `observability.md` 批准的范围偏离；任何实施期发现回填设计文档 Review Record 而非静默偏离——尤其 P99 语义偏移评估结果 + 指标 4 tryLock+retry 位置勘探结果）

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

Status Note: <why the plan can close>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- 部署侧 Prometheus+Grafana / OTel trace / 高基数业务指标 successor（见上 Deferred）。
