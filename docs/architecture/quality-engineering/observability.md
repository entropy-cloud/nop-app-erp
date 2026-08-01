# 可观测性补全评估（MQ Q7）—— Phase 1 设计文档

> Owner Doc for Milestone MQ Q7（可观测性补全评估）
> 创建日期：2026-08-02
> Plan：`docs/plans/2026-08-02-1121-3-mq-q7-observability-design-doc.md`
> 单一真相源依赖：本文档是 MQ 文档先行工作流 **Phase 1** 产物（设计/策略文档），**不实现任何代码/ORM/CI 变更**。Phase 2 实现 plan（视本文档 §3 + §6 Decision 结论，实现业务指标埋点 / `ErpFinPostingMetrics` SPI 迁移 / 端点验证 / 部署侧 Prometheus+Grafana 接线，或裁决 deferred）须在本文档审查收敛后方可起草。
> 上游真相源（**只引用**，不重推导，避免双真相源漂移）：
> - `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q7（line 680 工作项表 + line 789 维度说明 + §横切关注点 §文档先行工作流 line 843-862 + 依赖图 line 825 `Q0 --> Q7`）
> - `docs/architecture/quality-engineering/README.md`（Q0 范围矩阵 + 复杂度分级 + 实施顺序裁决基线，Q7 位 7）
> - `docs/plans/2026-08-01-1121-1-mq-q0-quality-gap-analysis-readme.md` §Current Baseline（Q7 单文件命中证据已核验）
> - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostingMetrics.java`（现状证据——自建进程内 ring-buffer，line 18 javadoc 自认须接 Micrometer）
> - `../nop-entropy/nop-kernel/nop-commons/`（平台 Micrometer SPI 真相源：`GlobalMeterRegistry` / `MeterPrinter` / pom `micrometer-registry-prometheus` 声明）
> - `../nop-entropy/nop-quarkus/nop-quarkus-web-starter/pom.xml` + `nop-quarkus-core-starter/.../QuarkusIntegration.java`（Quarkus 集成层真相源：`quarkus-micrometer-registry-prometheus` + `quarkus-smallrye-health` 声明 + `MeterRegistry` 自动桥接证据）
> - `../nop-entropy/nop-persistence/nop-dao/.../metrics/DaoMetricsImpl.java` + `nop-task-core/.../metrics/TaskFlowMetricsImpl.java` + `nop-graphql-core/.../engine/MetricsGraphQLHook.java`（平台 metrics 实现范式真相源，app 层业务指标埋点的模板）
> - sibling docs：`docs/architecture/quality-engineering/performance-baseline.md`（Q5，CI 回归基线，与 Q7 生产运行时正交，§8 边界声明）/ `security-scanning.md`（Q2，文档结构参照）

## 1. 现状评估

> 本节**引用**（非重推导）上游真相源已核验事实，每条标注可复现核验命令 + 核验日期，便于 Phase 2 plan 与独立审查复核。证据核验日期：2026-08-02（HEAD 含 Q2/Q5 Phase 2 收口）。

### 1.1 Q0/roadmap 的「nop-platform 无可观测性」假设已被实仓复核**大幅校正**

Q0 README §范围矩阵 §Q7 + roadmap line 697 + line 789 原文均断言「nop-platform 无 Micrometer/Prometheus/OTel/metrics API」「app-erp 根 pom 零可观测性依赖」。2026-08-02 实仓复核**逐层证伪**该断言——平台不仅有 Micrometer + Prometheus 注册表依赖，Quarkus 运行时扩展亦**已传递到 app-erp-all**，且自动桥接机制已实现。本节给出完整证据链。

#### 1.1.1 app-erp 关键字命中（单文件，符合 Q0 单文件命中结论）

- 核验命令（2026-08-02 复核单文件命中）：`rg -il "micrometer|prometheus|opentelemetry|otel" --glob '*.java'`（工作目录 = nop-app-erp）→ **EXIT=0，命中 1 文件**：`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostingMetrics.java`。
- 命中原因：该文件 line 18 javadoc 注释「残留风险……生产趋势须接 Micrometer + 时序库（Follow-up）」。其 import 仅 `io.nop.api.core.time.CoreMetrics`（Nop 平台进程内时间 API，非 Micrometer）。
- **关键观察**：`ErpFinPostingMetrics` 是**自建进程内 ring-buffer 采样器**（`volatile long[] samples` + `synchronized recordLatency/p99LatencyMillis`），**完全绕过** §1.2 将证实的平台 Micrometer SPI——这是一个 app 层「自造轮子」缺口，不是平台能力缺口。

#### 1.1.2 nop-entropy 平台层已有完整 Micrometer SPI（证伪「平台无 metrics API」）

- 核验命令（2026-08-02 复核）：`rg "micrometer" ../nop-entropy/nop-kernel/nop-commons/pom.xml` → 命中 line 50-56：
  - `<artifactId>micrometer-registry-prometheus</artifactId>`（groupId `io.micrometer`，非 optional）
  - `<artifactId>micrometer-core</artifactId>`（非 optional）
- 平台 SPI 真相源（2026-08-02 复核实存）：
  - `../nop-entropy/nop-kernel/nop-commons/src/main/java/io/nop/commons/metrics/GlobalMeterRegistry.java`——全局 `MeterRegistry` 单例（默认 `SimpleMeterRegistry`，可经 `registerInstance(MeterRegistry)` 替换）。
  - `../nop-entropy/nop-kernel/nop-commons/src/main/java/io/nop/commons/metrics/MeterPrinter.java`——通用 registry 文本导出器（copy 自 LoggingMeterRegistry）。
  - `../nop-entropy/nop-kernel/nop-commons/src/main/java/io/nop/commons/metrics/MeterPrintConfig.java`——命名约定 + step 配置。
- 平台 metrics 实现范式（已落地，app 层埋点的模板）：
  - `../nop-entropy/nop-persistence/nop-dao/src/main/java/io/nop/dao/metrics/DaoMetricsImpl.java`——DAO 层指标（事务 open/success/fail 计数 + query/update/batch Timer + 行读写计数），构造器注入 `MeterRegistry`，无参构造默认 `GlobalMeterRegistry.instance()`。
  - `../nop-entropy/nop-task/nop-task-core/src/main/java/io/nop/task/metrics/TaskFlowMetricsImpl.java`——task/step 成功/失败 Timer（`Timer.start(registry)` + `sample.stop(timer)` 范式）。
  - `../nop-entropy/nop-service-framework/nop-graphql/nop-graphql-core/src/main/java/io/nop/graphql/core/engine/MetricsGraphQLHook.java`——GraphQL 请求 metrics。
  - `../nop-entropy/nop-batch/.../metrics/BatchTaskMetricsImpl.java` + `../nop-entropy/nop-job/.../metrics/JobWorkerMetricsImpl.java` 等。
- **裁决（Q0 假设校正）**：Q0/roadmap「nop-platform 无 metrics API」断言**不成立**。平台已有完整的 Micrometer-based SPI（`GlobalMeterRegistry` 单例 + `*MetricsImpl` 范式），app 层只需**复用 `GlobalMeterRegistry.instance()` 创建 `Counter`/`Timer`**（对齐 `DaoMetricsImpl` 范式）即可埋业务指标，无需新建任何抽象。本校正是 §3 技术选型的关键输入。

#### 1.1.3 app-erp-all 运行时为 Quarkus，且 Quarkus 可观测性扩展**已传递到位**（证伪「app-erp 零可观测性依赖」）

- app-erp 运行时为 Quarkus（决定性证据）：`rg -n "nop-quarkus|nop-spring" app-erp-all/pom.xml` → line 27 `<artifactId>nop-quarkus-web-orm-starter</artifactId>`。nop-entropy 是**多目标平台**（`ls -d ../nop-entropy/nop-quarkus ../nop-entropy/nop-spring` 并存），故 roadmap line 789 / Q0 README §Q7「nop-entropy 是否 Spring-based 可直接注入」是**伪问题**——决定性证据是 app-erp 自身运行时选择（`app-erp-all/pom.xml:27 → nop-quarkus-web-orm-starter`），非 nop-entropy 是否 Spring-based。roadmap line 789 的「Spring Boot Actuator」评估方向经此实仓证据校正为「Quarkus 原生」（§3 落地）。
- nop-quarkus-web-starter **已声明** Quarkus 可观测性扩展（2026-08-02 复核 `../nop-entropy/nop-quarkus/nop-quarkus-web-starter/pom.xml`）：
  - line 47 `<artifactId>quarkus-micrometer-registry-prometheus</artifactId>`
  - line 52 `<artifactId>quarkus-smallrye-health</artifactId>`
- 传递链：`app-erp-all` → `nop-quarkus-web-orm-starter` → `nop-quarkus-web-starter`（声明上述两 Quarkus 扩展）+ `nop-quarkus-core-starter` → `nop-commons`（声明 `micrometer-core` + `micrometer-registry-prometheus`）。
- **逐层实证**（2026-08-02 复核 `mvn -pl app-erp-all dependency:list -o`，离线模式已构建产物可解析）→ 命中传递依赖（节选）：
  ```
  io.quarkus:quarkus-micrometer:jar:3.35.1:compile
  io.quarkus:quarkus-micrometer-registry-prometheus:jar:3.35.1:compile
  io.quarkus:quarkus-smallrye-health:jar:3.35.1:compile
  io.micrometer:micrometer-core:jar:1.16.5:compile
  io.micrometer:micrometer-registry-prometheus:jar:1.16.5:compile
  io.micrometer:micrometer-registry-prometheus-simpleclient:jar:1.16.5:compile
  io.prometheus:prometheus-metrics-core:jar:1.3.10:compile
  io.prometheus:simpleclient:jar:0.16.0:compile
  ```
- **裁决（Q0 假设校正，第二项）**：Q0/roadmap「app-erp 零可观测性依赖」断言**在直接声明层面成立**（`rg "quarkus|micrometer|prometheus|actuator" pom.xml` 在根 pom EXIT=1 零命中），**但在传递依赖层面完全不成立**——Quarkus metrics + health 扩展经 `nop-quarkus-web-starter` 已传递到 `app-erp-all` 编译类路径（compile scope）。这是 §3 + §6 Decision 的决定性输入：基础设施接入成本接近零（依赖已在位），只需验证运行时端点暴露 + 埋业务指标。

#### 1.1.4 Quarkus → 平台 SPI 自动桥接机制已实现

- 真相源：`../nop-entropy/nop-quarkus/nop-quarkus-core-starter/src/main/java/io/nop/quarkus/core/QuarkusIntegration.java` line 48-51：
  ```java
  if (BeanContainer.instance().containsBeanType(MeterRegistry.class)) {
      MeterRegistry meterRegistry = BeanContainer.instance().getBeanByType(MeterRegistry.class);
      GlobalMeterRegistry.registerInstance(meterRegistry);
  }
  ```
- 机制：`QuarkusIntegration.start()`（nop-quarkus-core-starter 启动钩子）在 IoC 容器初始化后，若 Quarkus 已注册 `MeterRegistry` bean（`quarkus-micrometer` 扩展自动注册 `PrometheusMeterRegistry`），即将其桥接到平台全局 `GlobalMeterRegistry`。此后**所有平台 metrics**（`DaoMetricsImpl` / `TaskFlowMetricsImpl` / `MetricsGraphQLHook` 等）经无参构造默认取 `GlobalMeterRegistry.instance()` 的指标，**自动流入 Quarkus 管理的 `PrometheusMeterRegistry`**，经 `/q/metrics` 端点暴露。
- **关键含义**：app 层业务指标只需经 `GlobalMeterRegistry.instance()` 创建（对齐 `DaoMetricsImpl` 范式），无需感知运行时是 Quarkus 还是 Spring，无需手动接线。桥接是平台已实现的能力。

#### 1.1.5 健康检查端点已实现（Quarkus SmallRye Health）

- `../nop-entropy/nop-quarkus/nop-quarkus-web-starter/src/main/java/io/nop/quarkus/web/health/QuarkusReadyCheck.java` 实现 MicroProfile Health `@Readiness` `HealthCheck`，检查 `CoreInitialization.isInitialized()` / `isSuspended()`。
- 平台另有 `../nop-entropy/nop-cluster/nop-cluster-core/.../health/`（`IHealthChecker` / `CompositeHealthChecker` / `HealthCheckResult`）作为集群层健康检查抽象。
- `quarkus-smallrye-health` 扩展传递到位（§1.1.3 实证）→ `/q/health` / `/q/health/live` / `/q/health/ready` 端点在运行时默认暴露（Quarkus 默认主端口，非 management 独立端口）。

### 1.2 app-erp 当前运行时配置（无可观测性显式配置）

- 核验命令（2026-08-02 复核）：`rg -n "quarkus\.(micrometer|management|health|smallrye)" app-erp-all/src/main/resources/application.yaml` → EXIT=1（零命中）。
- `app-erp-all/src/main/resources/application.yaml` 仅配置 `nop.*` 业务键 + `quarkus.http/datasource/log/dev`（端口 8011、H2、INFO 日志），**未显式配置 `quarkus.micrometer.*` 或 `quarkus.health.*` 或 management 接口**。
- **含义**：运行时依赖 Quarkus 默认行为——`quarkus-micrometer` 默认 `enabled=true`，`/q/metrics` 默认在主端口（8011）暴露；`quarkus-smallrye-health` 默认 `/q/health/*` 在主端口暴露。app-erp 未做任何屏蔽配置。**结论：可观测性端点在运行时极可能已默认可用，但从未经端到端验证**（Phase 2 §5 须实测 `/q/metrics` + `/q/health/ready` 返回体确认）。

### 1.3 app-erp 业务指标现状（仅 finance 过账 ring-buffer，绕过 SPI）

- `ErpFinPostingMetrics`（§1.1.1）是 app 层唯一业务指标实现，存在三处缺陷：
  1. **绕过平台 SPI**：自建 `volatile long[] samples` ring-buffer，未用 `GlobalMeterRegistry.instance()`，无法流入 Prometheus 端点，与 `DaoMetricsImpl` 范式背离。
  2. **重启即失**：进程重启采样清零，无历史趋势（line 18 javadoc 自认）。
  3. **覆盖面极窄**：仅凭证过账时延 P99，无过账成功率 / 期间结账耗时 / 并发冲突率 / posting-exception 堆积量 / 关键路径吞吐（roadmap line 789 全部要求维度均未实现）。
- **裁决**：app 层业务指标是**真实缺口**（非平台缺口，是 app 未使用已提供的能力）。

### 1.4 部署侧（Prometheus + Grafana）现状

- 核验命令（2026-08-02 复核）：仓库内无任何 Prometheus scrape config / Grafana dashboard JSON / docker-compose observability stack。
- app-erp 是**应用层仓库**，部署侧（Prometheus + Grafana + 告警规则）属运维/部署域，不在 app 仓库范围（§2.2 非目标 + §6 裁决）。

### 1.5 关键风险（roadmap line 789 校正后）

- **roadmap line 789 的两个核心技术假设均经实仓复核证伪**——(a) 「nop-platform 无 Micrometer/Prometheus/OTel」不成立（平台有完整 SPI）；(b) 「Spring Boot Actuator 引入评估」是伪方向（app-erp 运行时为 Quarkus，且 Quarkus 可观测性扩展已传递到位）。本设计文档 §3 落地校正后的评估。
- 真实风险转为：(a) 业务指标埋点零基础（仅 1 个绕过 SPI 的 ring-buffer）；(b) 运行时端点未验证；(c) 部署侧零基础设施。

**剩余差距**：无 Q7 设计 owner doc；业务指标定义 + `ErpFinPostingMetrics` SPI 迁移路径 + 端点验证 + 部署侧边界均未裁决。本文档经独立审查收敛后定夺。

## 2. 目标与非目标

### 2.1 目标（Phase 1 = 本文档；Phase 2 实现见 §5 + §7）

1. **校正 Q0/roadmap 可观测性假设**（§1.1 + §3）：以实仓证据链证伪「平台无 metrics API」+「app-erp 零可观测性依赖」断言，定位真实缺口（app 层未用平台能力 + 端点未验证 + 业务指标零基础）。
2. **裁决可观测性技术选型**（§3 Decision）：评估候选并裁决——Quarkus 原生扩展（`quarkus-micrometer` + Prometheus / `quarkus-smallrye-health`）**复用平台 `GlobalMeterRegistry` SPI** vs 自建指标导出 vs deferred。给出候选、考虑的替代、残留风险三要素（plan authoring guide §规则 9）。**显式校正** roadmap line 789 / Q0 README §Q7「Spring Boot Actuator」假设为「Quarkus 原生」评估方向。
3. **裁决 nop-entropy metrics SPI 可复用性**（§4）：核实 `GlobalMeterRegistry` + `DaoMetricsImpl`/`TaskFlowMetricsImpl` 范式是否可直接被 app 层复用，裁决 app 接入路径（复用平台 SPI vs app 层独立接入）。
4. **定义业务指标**（§5）：roadmap line 789 要求——过账成功率 / 期间结账耗时 / 并发冲突率（乐观锁失败率）/ posting-exception 堆积量 / 业务关键路径吞吐；每指标给出采集点（生产代码位置）+ 维度（tag）+ 导出频率。
5. **裁决持久化与展示方案**（§6 Decision）：Prometheus + Grafana（部署侧时序库）vs ring-buffer 落库 vs deferred；评估是否引入展示层。
6. **提供 Phase 2 实施契约**（§7）：若裁决为实现，给出实施步骤骨架 + 验收判据；若 deferred，给出 successor 触发条件。
7. **声明与 Q5（性能基线）/ Q4（故障注入）的正交边界**（§8）。

### 2.2 非目标

- **不实现任何代码/ORM/CI 变更**——本文档仅产出设计（MQ 文档先行工作流 Phase 1 硬约束）。Phase 2 实现（端点验证 / 业务指标埋点 / `ErpFinPostingMetrics` 迁移 / 部署侧接线）须在本文档审查收敛后方可起草，是独立后续 plan。
- **不修改 nop-entropy 源码**——`GlobalMeterRegistry` + `QuarkusIntegration` 桥接 + `DaoMetricsImpl` 范式**已满足** app 层接入需求（§4 证实），无平台改造 successor。
- **不覆盖部署侧运维实现**——Prometheus + Grafana + 告警规则的部署配置属运维/部署域，本文档仅裁决**是否引入**及**接口契约**（§6），具体部署 manifest 不在 app 仓库范围（§1.4）。
- **不覆盖前端/浏览器端可观测性**（RUM / 前端 error tracking）——前端工程域，Q0 README §候选排除裁决已排除「前端可访问性」类前端关注点；Q7 聚焦后端运行时。
- **不重复 Q5（CI 性能基线）/ Q4（故障注入测试）**——三者正交（生产运行时 vs CI 回归 vs 故障测试），§8 显式声明。
- **不预先承诺实现**——若评估结论为 deferred（业务场景未明确需生产 metrics），诚实记录 + successor 触发条件，不强行承诺 Phase 2（roadmap line 680 明示「视评估结论实现或 deferred」）。
- **不动 Q0/roadmap 的「Spring Boot Actuator」原文**——纯文档 Phase 1 不改 roadmap/Q0 README；本文档 §1.1 + §3 以实仓证据落地校正，未来若 roadmap 维护者清理，以本文档裁决为准。
- **不覆盖 Q2/Q5 同批工作**（各有独立计划）。

## 3. 可观测性技术选型（Decision）

> Decision 项：记录候选 + 考虑的替代 + 残留风险三要素（plan authoring guide §规则 9）。本裁决对应 roadmap line 789 明示的「Spring Boot Actuator 引入可行性评估」关键决策，并**以 §1 实仓证据大幅校正**该评估方向。

### 3.1 假设校正（roadmap line 789 / Q0 README §Q7）

roadmap line 789 原文：「Spring Boot Actuator 引入可行性评估（nop-entropy 是否 Spring-based 可直接注入）+ 业务指标定义 + 仪表盘设计 + 持久化方案」。

**2026-08-02 实仓复核校正证据链**（§1.1 已详述，此处汇总裁决）：

1. **校正方向**：「Spring Boot Actuator」评估方向校正为「Quarkus 原生」。决定性证据：`app-erp-all/pom.xml:27 → nop-quarkus-web-orm-starter`，即 app-erp 运行时为 Quarkus。
2. **校正伪问题**：「nop-entropy 是否 Spring-based」是伪问题——nop-entropy 是多目标平台（`nop-quarkus/` + `nop-spring/` 并存），同时支持 Quarkus 和 Spring 运行时。校正证据须用 **app-erp 自身运行时选择**，不得误述「nop-entropy 非 Spring-based」。
3. **校正能力假设**：roadmap line 697 / Q0 README §Q7「nop-platform 无 Micrometer/Prometheus/OTel/metrics API」**不成立**——`nop-commons` 声明 `micrometer-core` + `micrometer-registry-prometheus`（§1.1.2），`nop-quarkus-web-starter` 声明 `quarkus-micrometer-registry-prometheus` + `quarkus-smallrye-health`（§1.1.3），均经 `mvn dependency:list` 实证传递到 `app-erp-all` 编译类路径（compile scope）。

> 校正**方向**有效（app-erp 运行时确为 Quarkus，Quarkus 原生评估是对的）；校正**证据**须用 §1.1.3 app-erp 运行时选择 + §1.1.2/§1.1.3 平台已提供能力，不得误述「nop-entropy 非 Spring-based」或「平台无 metrics API」。

### 3.2 候选路径

- **路径 A — Quarkus 原生扩展，复用平台 `GlobalMeterRegistry` SPI**：app 层业务指标经 `GlobalMeterRegistry.instance()` 创建 `Counter`/`Timer`（对齐 `DaoMetricsImpl` 范式）；Quarkus 经 `QuarkusIntegration.start()` 自动桥接到 `PrometheusMeterRegistry`，`/q/metrics` 端点默认暴露；健康检查经 `quarkus-smallrye-health` `/q/health/*`。**关键优势**：依赖已传递到位（§1.1.3）+ 桥接机制已实现（§1.1.4）+ 平台 metrics（DAO/GraphQL/task/job）自动接入，**基础设施接入成本接近零**。
- **路径 B — app 层自建指标导出**：不经平台 SPI，app 自建 metrics 端点（如自定义 `/api/metrics` servlet + 自管理 Prometheus client）。**缺陷**：与平台已有 SPI 背离 + `ErpFinPostingMetrics` ring-buffer 重复造轮子的延续 + 平台 metrics（DAO/GraphQL）须二次接线才能导出。
- **路径 C — deferred（接受现状 ring-buffer）**：不引入任何运行时导出，仅保留 `ErpFinPostingMetrics` 进程内 ring-buffer，依赖人工查接口呈现 P99。**缺陷**：重启即失 + 无历史趋势 + 无生产可观测性（line 18 javadoc 自认须接 Micrometer）。

### 3.3 裁决

**裁决：路径 A（Quarkus 原生 + 复用平台 `GlobalMeterRegistry` SPI）为实现方向。Phase 2 实现范围聚焦「端点验证 + 业务指标埋点 + `ErpFinPostingMetrics` SPI 迁移」，部署侧 Prometheus+Grafana 为独立 successor（§6 裁决）。**

理由：

1. **基础设施已在位，接入成本接近零（决定性）**：`quarkus-micrometer` + `quarkus-micrometer-registry-prometheus` + `quarkus-smallrye-health` + `micrometer-core` + `micrometer-registry-prometheus` 经 `mvn dependency:list` 实证传递到 `app-erp-all` compile 类路径（§1.1.3）。`QuarkusIntegration.start()` 自动桥接 `MeterRegistry` bean → `GlobalMeterRegistry`（§1.1.4）。app 层**无需新增任何 Maven 依赖**即可启用 `/q/metrics` + `/q/health/*` 端点。这彻底改变 roadmap line 789 假设的「Actuator 引入可行性评估」——可行性不是「待评估」而是「已传递到位，待验证」。
2. **平台 SPI 完全满足 app 业务指标需求（§4 详述）**：`GlobalMeterRegistry.instance()` 返回 `MeterRegistry`，app 经 `Counter`/`Timer`/`Gauge` API 创建业务指标（过账成功率 Counter、期间结账耗时 Timer 等），与 `DaoMetricsImpl`/`TaskFlowMetricsImpl` 范式一致。无需新建任何抽象。
3. **路径 B 重复造轮子**：`ErpFinPostingMetrics` 的 ring-buffer 正是路径 B 思路的产物，已被 line 18 javadoc 自认是「残留风险……须接 Micrometer」。延续路径 B 只会扩大与平台 SPI 的背离。
4. **路径 C 拒绝生产可观测性**：roadmap line 789 明示「业务指标定义 + 仪表盘设计 + 持久化方案」是 Q7 评估要求，deferred 仅在「平台无可用 metrics API + 引入成本超收益」时才合理——现两项前提均不成立（平台有 SPI，成本接近零），故 deferred 不合理。

### 3.4 考虑的替代

- **纯 deferred（路径 C）**：否决——前提（平台无 SPI + 成本超收益）经 §1 实仓复核均不成立。
- **路径 B 自建导出**：否决——重复造轮子，与平台已有 SPI 背离，且 `ErpFinPostingMetrics` 已证明此路不通。
- **Spring Boot Actuator（roadmap line 789 原方向）**：否决——app-erp 运行时为 Quarkus（§1.1.3 决定性证据），Spring Boot Actuator 仅在 app-erp 运行时迁移到 Spring 时才相关（§Deferred）。且 nop-entropy 是多目标平台，「nop-entropy 是否 Spring-based」是伪问题。

### 3.5 残留风险

- **运行时端点未端到端验证**：§1.2 显示 `application.yaml` 无显式 `quarkus.micrometer/health` 配置，依赖 Quarkus 默认行为。`/q/metrics` + `/q/health/ready` 在运行时是否真正返回预期内容（Prometheus 文本格式 / `{"status":"UP"}` JSON）未经实测。**缓解**：Phase 2 §5 首要任务 = 启动 app-erp-all 实测两端点返回体（curl 命令记录），若默认未暴露则补 `quarkus.micrometer.enabled=true` + `quarkus.health.enabled=true`（默认值，显式声明消除歧义）。
- **Prometheus 抓取路径未配置**：app 层暴露 `/q/metrics` 不等于 Prometheus 自动抓取——须部署侧配置 scrape config 指向 app-erp:8011。属部署域（§6 裁决为 successor）。
- **OTel 分布式追踪不在本裁决范围**：路径 A 聚焦 metrics + health，trace（`quarkus-opentelemetry`）未传递到位（`mvn dependency:list` 无 `opentelemetry` 命中）。trace 是独立 successor（§9），本裁决不承诺。

## 4. nop-entropy metrics 抽象点可复用性评估

> 本节核实 §1.1.2 引用的平台 metrics 抽象点是否暴露可被 app 层复用的 API/SPI，裁决 app 接入路径。

### 4.1 平台 SPI 真相源（2026-08-02 复核实存）

| 抽象点 | 路径 | 性质 | app 可复用性 |
|--------|------|------|--------------|
| `GlobalMeterRegistry` | `nop-commons/.../metrics/GlobalMeterRegistry.java` | 全局 `MeterRegistry` 单例（默认 `SimpleMeterRegistry`，经 `registerInstance` 可替换） | **直接复用**：`GlobalMeterRegistry.instance()` 返回 `MeterRegistry`，app 经 Micrometer 原生 `Counter.builder(...).register(registry)` / `Timer.builder(...).register(registry)` / `Gauge` API 创建业务指标。 |
| `MeterRegistry` 桥接（Quarkus） | `nop-quarkus-core-starter/.../QuarkusIntegration.java:48-51` | 启动时将 Quarkus `MeterRegistry` bean 桥接到 `GlobalMeterRegistry` | **自动生效**：app 无需接线，平台启动钩子自动完成。 |
| `MeterPrinter` / `MeterPrintConfig` | `nop-commons/.../metrics/MeterPrinter.java` | 通用 registry 文本导出（用于日志/调试） | 可复用（非必需，Prometheus 端点已覆盖导出）。 |
| `DaoMetricsImpl` | `nop-dao/.../metrics/DaoMetricsImpl.java` | DAO 层指标实现（事务 Counter + query/update Timer + 行计数） | **范式模板**：app 业务指标类照此构造器注入 `MeterRegistry`（或无参默认 `GlobalMeterRegistry.instance()`）+ `Counter`/`Timer` 字段 + 业务事件回调。 |
| `TaskFlowMetricsImpl` | `nop-task-core/.../metrics/TaskFlowMetricsImpl.java` | task/step 成功/失败 Timer | **范式模板**（`Timer.start(registry)` + `sample.stop(timer)` 计时窗口范式）。 |
| `MetricsGraphQLHook` | `nop-graphql-core/.../engine/MetricsGraphQLHook.java` | GraphQL 请求 metrics | 平台内部接入，app 不直接用，但其存在证明平台 metrics 覆盖面已含 GraphQL 层。 |
| `BatchTaskMetricsImpl` / `JobWorkerMetricsImpl` 等 | `nop-batch` / `nop-job` | 批处理/作业指标 | 范式模板，且其指标在 Phase 2 端点验证时应在 `/q/metrics` 输出中可见（佐证桥接生效）。 |

### 4.2 接入路径裁决

**裁决：app 层完全复用平台 `GlobalMeterRegistry` SPI，不新建任何 metrics 抽象。**

- **业务指标类范式**（对齐 `DaoMetricsImpl`）：
  ```java
  public class ErpFinPostingMetrics {  // 重构后
      private final MeterRegistry registry;
      private final Counter postingSuccess;
      private final Counter postingFailure;
      private final Timer postingDuration;
      // 构造器注入（IoC）或无参默认 GlobalMeterRegistry.instance()
      // postingSuccess = Counter.builder("erp.fin.posting").tag("result","success").register(registry);
      // ... 业务事件回调内 postingSuccess.increment() / Timer.start(registry).stop(postingDuration)
  }
  ```
- **接入步骤**（Phase 2 实施细节，此处仅裁决路径）：app 业务指标类构造器注入 `MeterRegistry`（IoC 容器自动注入 Quarkus 桥接后的 `PrometheusMeterRegistry`，或无参默认 `GlobalMeterRegistry.instance()`），按 `DaoMetricsImpl` 范式声明 `Counter`/`Timer` 字段，在业务事件回调（过账成功/失败、期间结账开始/结束、乐观锁异常等）内调用 `increment()`/`Timer.start().stop()`。零平台改造。
- **`ErpFinPostingMetrics` 现状裁决**：现存 ring-buffer（§1.1.1 + §1.3）在 Phase 2 **重构为 SPI 接入**——`p99LatencyMillis()` / `sampleCount()` 呈现接口**必须保留签名并转发** `Timer.takeSnapshot()`，因这两个方法支撑**公开 `@BizQuery` 跨层契约** `IErpFinPostingExceptionBiz.getRuntimeMetrics`（`module-finance/erp-fin-dao/.../biz/IErpFinPostingExceptionBiz.java:70`）→ `ErpFinPostingMetricsSnapshot`（finance-dao 跨层契约 DTO，含 `latencyP99Millis` / `latencySampleCount` 字段 + 阈值门控 `erp-fin.metric.latency-p99-threshold-millis`），由 `ErpFinPostingExceptionBizModel.getRuntimeMetrics`（line 158/175/183）调用，并有 `TestErpFinPostingMetrics` 断言。迁移是**契约保留**（非条件性「若被依赖」），删除 `volatile long[] samples`，`recordLatency()` 替换为 `Timer.start().stop()`。**P99 语义偏移注记**：现存 `p99LatencyMillis()` 返回窗口内**精确** P99（排序后 `ceil(n*0.99)-1` 位）；迁移后转发 `Timer.takeSnapshot().percentile(0.99)` 返回 Micrometer 直方图**估计值**（插值），数值有偏移。若阈值门控按精确 P99 校准，迁移后估计值可能跨阈方向不同——Phase 2 须评估是否需调整 `publish-percentiles` / `histogram` 配置以保持判定一致性。具体迁移在 Phase 2 plan 裁决，本节仅裁决方向（迁移 + 契约保留）。

### 4.3 考虑的替代

- **app 层独立 `Micrometer` 接入（不经 `GlobalMeterRegistry`）**：app 直接 `@Inject MeterRegistry`（Quarkus 注入）。可行但**多此一举**——`GlobalMeterRegistry.instance()` 已是同一 `MeterRegistry`（经 `QuarkusIntegration` 桥接），直接用全局单例与 `DaoMetricsImpl`/`TaskFlowMetricsImpl` 范式一致，且无需 `@Inject` 装配。仅在 app 指标类需按域前缀隔离时才考虑构造器注入 + prefix 参数（对齐 `DaoMetricsImpl(MeterRegistry, ..., String prefix)`）。
- **新建 app 层 metrics 抽象（如 `IErpMetricsRegistry`）**：否决——平台 `GlobalMeterRegistry` + Micrometer 原生 API 已满足，新建抽象是过度设计。

## 5. 业务指标定义

> roadmap line 789 要求：业务指标定义（过账成功率 / 期间结账耗时 / 并发冲突率 / posting-exception 堆积量 / 关键路径吞吐）。本节定义每指标的采集点（生产代码位置）+ Micrometer meter 类型 + 维度（tag）+ 导出语义。**采集点位置在 Phase 2 plan 落地时精确到方法签名**，本节给出域 + 类级定位。

### 5.1 指标清单

| # | 指标名（Prometheus） | 类型 | 语义 | 采集点（域/类） | tag 维度 | 导出 |
|---|----------------------|------|------|------------------|----------|------|
| 1 | `erp_fin_posting_total` | Counter | 凭证过账次数（成功/失败分桶） | finance：`ErpFinPostingProcessor`（编排方法 success/failure 路径） | `result`={success,failure}, `biz_type`（业务单据类型，如 PUR_RCV/SAL_SHP） | 累计计数，Prometheus 自动算速率 |
| 2 | `erp_fin_posting_duration_seconds` | Timer | 单次过账端到端耗时（替代 `ErpFinPostingMetrics` ring-buffer） | finance：`ErpFinPostingProcessor` 编排方法 `Timer.start()`/`stop()` 包裹 | `biz_type` | 直方图（count + sum + percentile），Prometheus 自动 |
| 3 | `erp_fin_period_close_duration_seconds` | Timer | 单期间结账端到端耗时 | finance：`ErpFinAccountingPeriodBizModel` 结账链路 | `fiscal_year`, `period_no` | 直方图 |
| 4 | `erp_concurrency_optimistic_lock_failure_total` | Counter | 乐观锁 / 并发冲突失败次数 | 并发热点服务类的 tryLock+retry 失败路径（inventory 域 costing 系列 `FifoCostingStrategy`/`LifoCostingStrategy`/`StandardCostingStrategy`/`BatchCostingStrategy` 经 `BookingContext` 内部重试循环）+ 平台 DAO 层（`DaoMetricsImpl.transactionFail` 已有 `nop.dao.transactions.failure`，但语义宽泛无业务路径 tag）。**注**：app-erp 全仓经 `rg "OptimisticLockException\|StaleObjectState" --glob '*.java'` 复核零命中——无 BizModel catch 此类异常，并发处理范式是 tryLock+retry 循环而非异常 catch，故 Phase 2 须先 grep 定位所有 tryLock+retry 范式位置再插桩 | `domain`（finance/inventory/...）, `operation`（posting/close/update） | 累计计数 |
| 5 | `erp_fin_posting_exception_backlog` | Gauge | 待处理 posting-exception 堆积量 | finance：定期查询 `ErpFinPostingException` 表，按 `status in (PENDING, RETRYING, MANUAL) and voucherId is null` 计数——**复用 `ErpFinPostingExceptionBizModel.countUnresolved`（line 137）既有语义**（期末结账前置检查已用同一查询阻止结账），定时任务刷新 Gauge。**校正**：原设计引用 `ErpFinVoucherBillR` + `posted=false` + `exceptionLogged=true` 经实仓复核错误（`ErpFinVoucherBillR` 是凭证-单据回链表无 `posted`；`exceptionLogged` 字段全仓不存在——`rg "exceptionLogged" --glob '*.java' --glob '*.xml'` 零命中）。真实 backlog 数据源是 `ErpFinPostingException` 表（PENDING/RETRYING/MANUAL 终态机） | `biz_type`（源自 `ErpFinPostingException.businessType`） | 瞬时值（定时刷新，建议每 5 分钟） |
| 6 | `erp_business_path_throughput_total` | Counter | 关键业务路径吞吐（过账/结账/成本核算/报表渲染四路径，对齐 Q5 §4） | 各域 BizModel 关键方法入口 | `path`={posting,period_close,costing_reclose,report_render} | 累计计数 |

### 5.2 指标定义约定

- **命名约定**：`erp_<domain>_<metric>_<unit>` 前缀（snake_case，对齐 `MeterPrintConfig.namingConvention = snakeCase` + `nop.dao.*` / `nop.task.*` 平台先例）。unit 后缀（`_total` 计数 / `_seconds` 计时 / `_backlog` 瞬时值）对齐 Prometheus 命名惯例。
- **tag 维度最小化原则**：每指标 tag 数 ≤ 3，避免高基数 tag（如不使用 `voucher_id` 这类无限基数 tag，否则 Prometheus cardinality 爆炸）。`biz_type` / `domain` / `path` 均为有限枚举。
- **导出频率**：Counter/Timer 由 Prometheus scrape 频率决定（默认 15s，部署侧配置）；Gauge（指标 5）须 app 层定时刷新（建议 5 分钟，避免 scrape 时才查 DB 引入抓取延迟）。
- **采集点精确化（Phase 2 plan 责任）**：本表给出域/类级定位，Phase 2 plan 须精确到方法签名 + 插入位置（`Timer.start()` 在方法首行、`stop()` 在 finally），并补单测验证 meter 注册。

### 5.3 与平台 metrics 的关系

- 平台 metrics（`nop.dao.*` / `nop.graphql.*` / `nop.task.*`）经 `GlobalMeterRegistry` 桥接后**自动出现在 `/q/metrics`**，覆盖基础设施层（DB 连接、事务、SQL 计时、GraphQL 请求）。
- app 业务指标（本节 §5.1 六项）覆盖**业务语义层**（过账成功/失败、期间结账耗时等），与平台 metrics 互补不重叠。Phase 2 端点验证时 `/q/metrics` 应同时含 `nop.dao_*` + `erp_*` 两族指标。

## 6. 持久化与展示方案（Decision）

> Decision 项：记录候选 + 考虑的替代 + 残留风险三要素。本裁决对应 roadmap line 789 明示的「仪表盘设计 + 持久化方案」关键决策。

### 6.1 候选路径

- **路径 A — Prometheus + Grafana（部署侧时序库）**：app 经 `/q/metrics` 暴露，部署侧 Prometheus 定时 scrape 入时序库，Grafana 读取展示 + 告警。业界标准方案。
- **路径 B — ring-buffer 落库（应用侧持久化）**：app 定期将 metrics 快照写入业务表（如 `erp_sys_metrics_snapshot`），自带历史趋势，无外部组件。**缺陷**：与 §3 裁决的 SPI 接入背离（ring-buffer 思路延续）+ 应用侧承担时序库职责（非业务关注点）+ 查询/展示能力远弱于 Grafana。
- **路径 C — deferred（仅进程内 + 日志）**：不持久化，依赖 `MeterPrinter.print()` 定期打日志（`GlobalMeterRegistry.print()` 已实现），人工 grep 日志看趋势。**缺陷**：无结构化查询 + 无可视化 + 无告警。

### 6.2 裁决

**裁决：路径 A（Prometheus + Grafana）为持久化与展示方向。app 仓库范围仅负责「`/q/metrics` 端点暴露 + 业务指标埋点」，Prometheus + Grafana 部署 manifest 与 scrape 配置属部署/运维域 successor（§9），不在 app 仓库 Phase 2 范围。**

理由：

1. **职责分离**：app 仓库是应用层，时序库 + 可视化属运维/部署域。app 只须保证 `/q/metrics` 按 Prometheus 文本格式正确暴露（路径 A §3 已裁决 Quarkus 原生支持），部署侧接线是独立工作面。
2. **业界标准 = 最低长期成本**：Prometheus + Grafana 是云原生事实标准，社区 dashboard 模板丰富，告警规则（PromQL）表达力强。路径 B/C 是短视方案，长期会被推倒重做。
3. **`MeterPrinter` 日志兜底已内置**：`GlobalMeterRegistry.print()` + `MeterPrinter.scrape()` 平台已实现，可作为部署侧 Prometheus 未就绪时的临时观察手段（路径 C 的有限价值被平台内建吸收，无需独立选 C）。

### 6.3 app 仓库 Phase 2 持久化/展示边界（裁决）

| 工作面 | 归属 | Phase 2 是否含 |
|--------|------|----------------|
| `/q/metrics` Prometheus 文本格式端点暴露 | app 仓库（Quarkus 默认 + 验证） | **是**（§7 实施步骤 1） |
| `/q/health/*` 健康检查端点 | app 仓库（Quarkus 默认 + 验证） | **是**（§7 步骤 1） |
| 业务指标埋点（§5 六项） | app 仓库（业务代码） | **是**（§7 步骤 2-4） |
| `ErpFinPostingMetrics` SPI 迁移 | app 仓库（重构） | **是**（§7 步骤 5） |
| Prometheus scrape config | **部署/运维域** | 否（successor） |
| Grafana dashboard JSON | **部署/运维域** | 否（successor） |
| Prometheus alerting rules | **部署/运维域** | 否（successor） |

### 6.4 考虑的替代

- **路径 B（ring-buffer 落库）**：否决——与 §3 SPI 接入裁决背离 + 应用侧承担非业务职责。
- **路径 C（deferred + 日志）**：部分采纳为兜底（`MeterPrinter` 平台内建），但非主裁决。

### 6.5 残留风险

- **部署侧未就绪时 metrics 无消费者**：app 暴露 `/q/metrics` 但无 Prometheus 抓取 = 端点空转。**缓解**：本裁决将部署侧明确为 successor（§9），Phase 2 验收以「端点返回预期格式 + 业务指标可见」为准，不依赖部署侧实际抓取。`MeterPrinter.print()` 可作为开发期观察手段。
- **Prometheus cardinality 风险**：若 Phase 2 业务指标的 tag 设计不当（高基数 tag），生产 scrape 会撑爆时序库。**缓解**：§5.2 已约束 tag 数 ≤ 3 + 有限枚举；Phase 2 plan 须评审每指标 tag 的基数。

## 7. 实施步骤（Phase 2 契约）

> 本节是 Phase 2 实现 plan 的实施契约（骨架 + 验收判据）。Phase 2 须在本文档审查收敛后方可起草。

### 7.1 Phase 2 实施步骤骨架

1. **运行时端点验证（零代码或最小配置）**：
   - 启动 `app-erp-all`（`java -jar` 或 `mvn quarkus:dev`），curl `http://localhost:8011/q/metrics` + `/q/health/ready`。
   - 预期：`/q/metrics` 返回 Prometheus 文本格式含 `nop.dao_*` / `nop.graphql_*` 平台指标（佐证 `QuarkusIntegration` 桥接生效）；`/q/health/ready` 返回 `{"status":"UP","checks":[...]}`（`QuarkusReadyCheck`）。
   - 若默认未暴露：补 `application.yaml` `quarkus.micrometer.enabled=true` + `quarkus.health.enabled=true`（显式声明，默认值，消除歧义）。
2. **业务指标埋点（§5 六项，按域分批）**：
   - finance 域优先（指标 1/2/3/5）：在 `ErpFinPostingProcessor` + `ErpFinAccountingPeriodBizModel` 插桩。
   - 共享层指标 4（乐观锁失败）：在 `module-common` 或各域 BizModel catch 乐观锁异常处插桩。
   - 指标 6（吞吐）：各域关键方法入口插桩（对齐 Q5 §4 四路径）。
   - 每指标类对齐 `DaoMetricsImpl` 范式（构造器注入 `MeterRegistry` 或无参默认 `GlobalMeterRegistry.instance()`）。
3. **`ErpFinPostingMetrics` SPI 迁移**：重构 ring-buffer → `Timer`（§4.2 裁决），保留 `p99LatencyMillis()`/`sampleCount()` 呈现接口转发 `Timer.takeSnapshot()`，删除 `volatile long[] samples`。
4. **单测验证 meter 注册**：每业务指标类补单测，断言 `GlobalMeterRegistry.instance().find("erp_...").meter()` 非空 + 业务事件后计数/计时正确。
5. **端点回归验证**：重复步骤 1，确认 `/q/metrics` 现含 `erp_*` 业务指标族 + 平台指标族共存。

### 7.2 验收判据

1. **端点暴露**：`curl /q/metrics` 返回 HTTP 200 + Prometheus 文本格式 + 含 `nop.dao_*` 平台指标 + `erp_*` 业务指标；`curl /q/health/ready` 返回 `{"status":"UP"}`。
2. **业务指标可观测**：§5 六项指标均在 `/q/metrics` 输出可见，tag 维度符合 §5.2 约束（≤3 + 有限枚举）。
3. **`ErpFinPostingMetrics` 无 ring-buffer 残留**：`rg "volatile long\[\] samples" module-finance/` EXIT=1；`ErpFinPostingMetrics` 改用 `Timer`。
4. **平台桥接生效**：`/q/metrics` 含 `nop.dao_transactions_*` / `nop.graphql_*`，佐证 `QuarkusIntegration.start()` 桥接 + `GlobalMeterRegistry` 单例工作。
5. **mvn test 全绿**：业务指标类单测通过 + 既有测试无回归（ring-buffer 迁移不破坏调用方）。
6. **CI 门控（如适用）**：业务指标埋点的性能影响分两类评估——(a) Counter/Timer（指标 1/2/3/4/6）在请求路径上的 `increment()`/`Timer.start()` 是 O(1) 原子操作，无 IO，不拖慢业务路径，豁免 per-commit 性能门控；(b) Gauge（指标 5）的定时刷新是后台定时任务的 DB 查询（**不在请求路径上**），同样豁免 per-commit 性能门控，但理由是「离请求路径」而非「O(1)」。两类结论一致（均豁免）但理由不同，须分别陈述避免误导 Phase 2。具体是否加 CI 检查（如 `/q/metrics` smoke）由 Phase 2 plan 裁决。

### 7.3 CI 门控设计（可选）

- 业务指标埋点性能影响分两类（与 §7.2 criterion 6 一致）：
  - **Counter/Timer（指标 1/2/3/4/6）**：请求路径上的 `Counter.increment()` / `Timer.start()`——O(1) 原子操作，无 IO，不拖慢业务路径。
  - **Gauge（指标 5）**：后台定时任务刷新（DB 查询 `ErpFinPostingException` 表），**不在请求路径上**，故同样不拖慢业务路径（理由是「离请求路径」而非「O(1)」）。
- 两类均豁免 per-commit 性能门控。
- 可选 CI 检查：`curl /q/metrics` smoke（启动 app + curl 断言 `erp_` 前缀存在）——但 Quarkus 启动开销大，建议归入 e2e.yml 或独立 smoke job，非 per-commit maven.yml。具体由 Phase 2 plan 裁决。

## 8. 与 Q5 / Q4 的正交边界声明

> 三者正交，须显式声明避免重叠（plan §Goals 要求）。

| 维度 | Q7（可观测性） | Q5（性能基线） | Q4（故障注入） |
|------|----------------|----------------|------------------|
| **关注点** | 生产运行时可观测性（metrics 导出 + health 端点） | CI 回归性能基线（关键路径耗时退化检测） | 故障可恢复性（tryPost 吞异常同型根因回归） |
| **运行时序** | 生产运行时持续导出 | CI 期定时测量（nightly） | 测试期故障注入断言 |
| **范围** | `/q/metrics` + `/q/health` + 业务指标埋点 | 4 关键路径 timing 基线 + CI 软门控 | 6 域过账悬挂路径故障注入 harness |
| **truth source** | 本文档（observability.md） | `performance-baseline.md` | `fault-injection.md` |
| **依赖** | Q0（独立） | Q0 + Q6（时钟硬化） | Q0（独立，协同 Q1） |

**正交性裁决**：

- **Q7 vs Q5**：Q5 是 CI 期定时测量（被测路径在测试栈内 timing），Q7 是生产运行时持续导出（业务代码内埋点）。Q5 的 perf 测试类**不**埋 Q7 业务指标（test scope，不进生产）；Q7 的业务指标**不**参与 Q5 退化门控（生产 metrics 非 CI 基线）。二者共享「关键路径」概念但实现正交（Q5 §4 四路径用于 timing，Q7 §5 指标 6 throughput 复用同四路径名仅作 tag 值，不共享测试类）。
- **Q7 vs Q4**：Q4 是测试期故障注入（harness + stub），Q7 是生产 metrics 导出。Q4 的故障注入测试**不**依赖 Q7 metrics（断言是业务状态 posted=false/告警发出，非 metrics 计数）；Q7 的并发冲突率指标（§5 指标 4）在生产捕获乐观锁失败，与 Q4 测试期注入的故障路径**语义不同**（生产真实冲突 vs 测试注入故障），不重复。**Metric 5（backlog gauge）与 Q4 的 posted=false 关注点概念邻近但实现正交**：Q4 = 测试期故障注入后断言业务状态（test scope）；Q7 Metric 5 = 生产期 DB 查询 `ErpFinPostingException` 表 gauge（prod code，定时刷新）；无共享测试类。
- **共享约束**：三者均**不修改 nop-entropy 源码**（Q7 §4 证实平台 SPI 已满足，Q5/Q4 均应用层方案）。

## 9. 残留风险与 successor

### 9.1 残留风险

- **运行时端点默认行为未实测**（§3.5）：`application.yaml` 无显式 `quarkus.micrometer/health` 配置，依赖 Quarkus 默认。Phase 2 §7.1 步骤 1 首要验证。
- **Prometheus cardinality**（§6.5）：业务指标 tag 设计须严控基数，Phase 2 plan 须评审。
- **`ErpFinPostingMetrics` 迁移的契约面**（§4.2 已详述）：`p99LatencyMillis()`/`sampleCount()` 支撑**公开 `@BizQuery` 契约** `IErpFinPostingExceptionBiz.getRuntimeMetrics` → `ErpFinPostingMetricsSnapshot`（finance-dao 跨层契约 DTO，含阈值门控 `erp-fin.metric.latency-p99-threshold-millis`），迁移**必须**保留签名并转发 `Timer.takeSnapshot()`，并有 `TestErpFinPostingMetrics` 断言须同步验证迁移后数值语义（P99 由精确窗口值变为直方图估计值，见 §4.2 P99 语义偏移注记）。
- **Metric 4（乐观锁失败）采集点须先勘探**：app-erp 全仓无 BizModel catch `OptimisticLockException`（§5.1 已校正），真实并发处理范式是 tryLock+retry 循环（inventory costing 系列）。Phase 2 plan 须先 grep 定位所有 tryLock+retry 范式位置再插桩，否则埋点位置遗漏。
- **OTel trace 未覆盖**（§3.5）：本裁决不含分布式追踪（`quarkus-opentelemetry` 未传递到位），生产排障的请求级 trace 是独立 successor。

### 9.2 Deferred But Adjudicated（successor）

- **Q7 Phase 2 实现**（端点验证 + 业务指标埋点 + `ErpFinPostingMetrics` SPI 迁移）：Classification = `实现 successor`（本裁决为「实现」非 deferred）。触发条件 = 本文档审查收敛（§Review Record 两轮无 BLOCKER/MAJOR）+ §3 + §6 Decision 落定。届时 DRAFT_PLANS 起草 Phase 2 实现 plan（加载 `nop-backend-dev` skill——业务指标埋点触及 BizModel/Processor），plan 引用本文档作为范围与验收依据。
- **部署侧 Prometheus + Grafana + 告警规则**：Classification = `部署/运维域 successor`。触发条件 = 首次生产部署 + 运维团队接收可观测性交接。app 仓库仅负责 `/q/metrics` 端点契约（§6.3）。
- **OTel 分布式追踪**（`quarkus-opentelemetry`）：Classification = `watch-only successor`。触发条件 = 生产排障需请求级 trace（跨模块调用链）+ 微服务化（当前单体无需跨进程 trace）。本裁决不含，`quarkus-opentelemetry` 未传递到位。
- **Spring Boot Actuator 评估方向**（roadmap line 789 原文）：Classification = `watch-only residual`。2026-08-02 实仓复核证实 app-erp 运行时为 Quarkus（`app-erp-all/pom.xml:27 → nop-quarkus-web-orm-starter`），Spring Boot Actuator 仅在 app-erp 运行时迁移到 Spring（`nop-spring-*-starter`）时才相关。
- **高基数业务指标（如按 tenant/warehouse 细分）**：Classification = `watch-only successor`。触发条件 = 多租户/多仓实际部署 + 运维确认 Prometheus cardinality 容量。当前 §5.2 tag ≤ 3 有限枚举约束已规避。

## Review Record

> 本节持久化独立子代理审查记录（含审查者 task id + 轮次 + 结论 + 修改摘要）。审查由独立子代理（新会话，fresh cold context）执行，审查者不可与作者为同一会话。两轮审查由不同子代理会话执行（不同 task id）。

### 第 1 轮 — 规范合规审查（independent-reviewer-r1 / `ses_0410de8f4ffeskYmXll7CeAiCz`）

- 结论：**accept**（0 BLOCKER / 0 MAJOR / 0 formal MINOR；2 项 optional cosmetic polish，显式非阻塞，不需修订周期）。
- 独立核验全 PASS：审查者以 cold context 重新运行所有承载性事实声明（含 `mvn -pl app-erp-all dependency:list -o` BUILD SUCCESS，22 条匹配 compile-scope 产物，逐行核对 §1.1.3 引用的 8 行 verbatim 坐标 + 版本完全一致）。9/9 节结构完整（§1 现状评估 L17 / §2 目标与非目标 L111 / §3 技术选型 Decision L134 / §4 metrics 抽象点可复用性评估 L179 / §5 业务指标定义 L219 / §6 持久化与展示 Decision L246 / §7 实施步骤 L288 / §8 与 Q5/Q4 正交边界 L321 / §9 残留风险与 successor L339）；两 Decision 节（§3/§6）三要素齐备（候选/替代/残留风险）。
- **Quarkus vs Spring 校正一致性 PASS（plan iter3 收敛要求忠实落地）**：「非 Spring-based」共出现 2 次（L145/L148），**均为 debunk 形态**（"不得误述"），零位置将其作为正向证据断言；正向框架统一为「多目标平台（nop-quarkus/ + nop-spring/ 并存）」（L49/L145/L171），两目录经 `ls -d` 实证并存。决定性证据 `app-erp-all/pom.xml:27 → nop-quarkus-web-orm-starter` 经独立 read 核实为 line 27 精确命中。
- **§1.1.2/§1.1.3 关键事实声明独立复核 PASS**：审查者独立 `mvn dependency:list` 确认 Quarkus + Micrometer + Prometheus + SmallRye Health 经传递依赖到达 `app-erp-all` compile 类路径——校正事实准确且恰当地 hedge（区分「直接声明层零命中」vs「传递依赖层全到位」），非 overstated。§1.1.4/§1.1.5 后续声明（QuarkusIntegration 桥接 + SmallRye Health）亦经源码核实。
- **OTel 缺席声明 PASS**：dep list 中 `opentelemetry` / `quarkus-opentelemetry` / `io.opentelemetry` 三关键字均 EXIT=1（无 OTel SDK；仅 Prometheus simpleclient 的 exemplar tracer 桥存在，非 OTel）。
- 引用准确性：roadmap line 680/697/789/825/843-862 + Q0 README §Q7（L71-75/L91）+ ErpFinPostingMetrics line 18 + GlobalMeterRegistry + QuarkusIntegration 48-51 + 两 pom（nop-commons 50-56 / nop-quarkus-web-starter 47/52）+ app-erp-all pom:27 全部精确。
- Optional cosmetic polish（非阻塞，未触发修订）：(1) header L12 真相源 bullet 摘要只提 `micrometer-registry-prometheus`，body §1.1.2 已完整枚举 `micrometer-core` + `micrometer-registry-prometheus`（body 完整，header 简写）；(2) §1.1.2 L38 `MeterPrinter` 描述性 aside「copy 自 LoggingMeterRegistry」非承载性，未独立溯源，但不影响任何 Decision。两者均不构成修订周期。

### 第 2 轮 — 覆盖面与可执行性审查（independent-reviewer-r2 / `ses_0410acaebffeb4r2GVtZKs7gmQ`）

- 结论：**accept-after-revision**（0 BLOCKER / 4 MAJOR / 2 MINOR；四项 MAJOR 均为 §5 指标定义 + §7 门控 + §9 风险的**局部**问题，**不触及** §3/§6 核心 Decision——Path A Quarkus 原生 + Path A Prometheus+Grafana + §4 SPI 复用经实仓复核全部 grounded 且内部一致）。
- 独立核验全 PASS：审查者以 cold context（与 R1 不同会话）复核核心事实——`app-erp-all/pom.xml:27` ✓；`nop-quarkus-web-starter/pom.xml:47/52` ✓；`nop-commons/pom.xml:51/56` ✓；`QuarkusIntegration.java:48-51` 桥接 ✓；OTel 缺席 ✓；`application.yaml:35` 端口 8011 ✓（`curl /q/metrics` 命令可执行）；§3.4/§6.4 替代非稻草人 ✓；Q5/Q4 正交 ✓；AGENTS.md `@BizMutation` 事务内 `Counter.increment()`/`Timer.start()` 安全（无 IO/无事务副作用/无跨实体）✓；Deferred 诚实 ✓。
- MAJOR 修订（已全部应用）：
  - **MAJOR-1 §5.1 Metric 5 采集点事实性错误**：原引用 `ErpFinVoucherBillR` + `posted=false` + `exceptionLogged=true` 经实仓复核错误（`ErpFinVoucherBillR` 是凭证-单据回链表无 `posted`；`exceptionLogged` 全仓零命中）。修复：采集点改写为 `ErpFinPostingException` 表 `status in (PENDING, RETRYING, MANUAL) and voucherId is null`，复用 `ErpFinPostingExceptionBizModel.countUnresolved`（line 137）既有语义，tag 改 `biz_type`（源自 `ErpFinPostingException.businessType`）。
  - **MAJOR-2 §5.1 Metric 4 采集点不存在**：`rg "OptimisticLockException|StaleObjectState"` 全仓零命中——无 BizModel catch 此类异常。真实并发处理范式是 tryLock+retry 循环（inventory costing 系列 `FifoCostingStrategy`/`LifoCostingStrategy`/`StandardCostingStrategy`/`BatchCostingStrategy` 经 `BookingContext`）。修复：采集点改写为 tryLock+retry 失败路径 + 显式声明 Phase 2 须先 grep 定位范式位置。
  - **MAJOR-3 §4.2 + §9.1 迁移契约面**：`p99LatencyMillis()`/`sampleCount()` 支撑公开 `@BizQuery` 契约 `IErpFinPostingExceptionBiz.getRuntimeMetrics` → `ErpFinPostingMetricsSnapshot`（含阈值门控），迁移是**契约保留**（非条件性「若被依赖」）。修复：§4.2 删除条件表述，改为「必须保留签名」并标注 P99 语义偏移（精确窗口 → 直方图估计）；§9.1 残留风险改写为契约保留 + TestErpFinPostingMetrics 同步验证。
  - **MAJOR-4 §7.2 criterion 6 + §7.3 CI 门控理由自相矛盾**：「O(1) 无 IO」仅适用 Counter/Timer，Gauge（Metric 5）定时刷新是 DB 查询。修复：§7.2/§7.3 区分两类埋点（Counter/Timer 请求路径 O(1) vs Gauge 后台定时任务离请求路径），结论一致（均豁免）但理由分别陈述。
- MINOR 修订（已全部应用）：
  - MINOR-1 §8 Q7-vs-Q4 正交论证补 Metric 5 与 Q4 posted=false 关注点概念邻近但实现正交声明。
  - MINOR-2 §4.2 + §9.1 标注 `Timer.takeSnapshot()` 迁移引入 P99 语义偏移（精确 → 估计），Phase 2 须评估 `publish-percentiles`/`histogram` 配置。

### 收敛结论

两轮独立子代理审查（不同 task id，fresh cold context）均 **0 BLOCKER / 0 残留 MAJOR**。R1 = accept（合规全 PASS，0 formal MINOR）；R2 = accept-after-revision（4 MAJOR + 2 MINOR 已全部修订应用，核心 Decision 不受影响）。文档结构完整（9/9 节）、双 Decision 三要素齐备（§3/§6）、无双真相源、owner doc 引用实存且行号准确、Quarkus/Spring 校正一致（debunk-form 规则满足）、§5 业务指标采集点经实仓复核可定位、§4 SPI 复用可执行、§7 Phase 2 骨架可执行、§8 正交边界成立、§9 残留风险真实 + successor 触发条件可证伪。**Phase 1 设计文档审查收敛**，可转 Phase 2 实现 plan 起草（视 §3 + §6 Decision，Q7 Phase 2 = 实现，非 deferred）。
