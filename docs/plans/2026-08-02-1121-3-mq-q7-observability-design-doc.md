# 2026-08-02-1121-3-mq-q7-observability-design-doc 可观测性补全评估 Phase 1 设计文档

> Plan Status: completed
> Last Reviewed: 2026-08-02
> Mission: audit-remediation
> Work Item: MQ Q7（Phase 1 设计文档）
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q7（line 680 工作项表 + line 789 维度说明）；`docs/architecture/quality-engineering/README.md` §实施顺序裁决（Q7 位 7）
> Related: Q0 README `docs/architecture/quality-engineering/README.md`（Q7 范围矩阵 + 候选排除裁决）；sibling MQ Phase 1 design-doc plans `2026-08-01-1121-2`（Q2）/`-1121-3`（Q5）/`2026-08-01-1158-*`（Q1/Q3/Q4/Q6）；sibling Phase 2 plans `2026-08-02-1121-1`（Q2）/`-2`（Q5）
> Audit: required

## Current Baseline

> 本计划是 MQ 文档先行工作流的 **Phase 1 设计文档**编写：为 Q7（可观测性补全评估）产出收敛的 `docs/architecture/quality-engineering/observability.md`，作为后续 Phase 2 实现 plan（视评估结论实现或 deferred）的实施契约。基线复核日期：2026-08-02。

**audit-remediation 主线**：M0 / MA1-MA7 / MR1-MR6 / MV / MG 全 done；MR6 milestone CLOSED。MQ Q0/Q1/Q3/Q4/Q6 已 done；Q2/Q5 Phase 2 同批起草（`2026-08-02-1121-1`/`-2`）。

**Q7 现状（Q0 README §范围矩阵 §Q7 + 2026-08-02 实仓复核）**：
- **无 `observability.md`**——2026-08-02 复核 `ls docs/architecture/quality-engineering/observability.md` → NOT FOUND（Phase 1 设计文档尚未编写）。
- **全仓零可观测性基础设施**——2026-08-02 复核 `rg -il "micrometer|prometheus|opentelemetry|otel" --glob '*.java'` → **仅 1 文件命中**：`module-finance/erp-fin-service/.../ErpFinPostingMetrics.java`，且命中原因是 javadoc 注释（line 18：「残留风险……进程重启采样清零、无历史趋势——生产趋势须接 Micrometer + 时序库（Follow-up）」）。其 import 仅 `io.nop.api.core.time.CoreMetrics`（Nop 平台进程内 ring-buffer 计时，重启即失）+ 进程内 traceId。**无 Micrometer/Prometheus/OTel 实际依赖**。
- **app-erp 根 pom 零可观测性依赖**——2026-08-02 复核 `rg "quarkus|micrometer|prometheus|actuator" pom.xml` → 零命中（app 应用层未声明任何 metrics/health/telemetry 依赖）。
- **关键发现（roadmap 假设校正点）**：roadmap line 789（Q7 维度说明）+ Q0 README §Q7 评估方向写「Spring Boot Actuator 引入可行性评估（nop-entropy 是否 Spring-based 可直接注入）」。**2026-08-02 实仓复核校正证据链**：(1) `app-erp-all/pom.xml:27` 聚合 `nop-quarkus-web-orm-starter` → **app-erp 运行时为 Quarkus**（决定性证据，app 自身运行时选择）；(2) nop-entropy 本身是**多目标平台**（`../nop-entropy/nop-quarkus/` 与 `../nop-entropy/nop-spring/` 模块族并存，后者 starter 含 `spring-boot-starter-actuator`，故「nop-entropy 是否 Spring-based」是伪问题）；(3) 多个 `*MetricsImpl.java` 抽象点（`BatchTaskMetricsImpl`/`JobWorkerMetricsImpl`/`TaskFlowMetricsImpl` 等）。故 Q7 Phase 1 设计文档**须校正评估方向为 Quarkus 原生扩展**（`quarkus-micrometer` / `quarkus-smallrye-health` / `quarkus-opentelemetry`），而非 Spring Boot Actuator——这是设计文档须裁决的关键技术选型校正（见 Goals）。校正**方向**有效（app-erp 运行时确为 Quarkus）；校正**证据**须用上述 app-erp 运行时选择，不得误述「nop-entropy 非 Spring-based」。
- **业务指标现状**：仅 `ErpFinPostingMetrics` 进程内 ring-buffer（过账延迟采样，重启即失）；无期间结账耗时 / 并发冲突率 / posting-exception 堆积量 / 业务成功率等指标的持久化或导出。

**剩余差距**：无 Q7 设计 owner doc。可观测性技术选型（Quarkus 原生 vs 自建 vs deferred）+ 业务指标定义 + 持久化方案 + dashboard 设计均未裁决，须经本文档独立审查后定夺。

## Goals

> 本计划产出 MQ Q7 的 **Phase 1 设计文档**（`observability.md`），覆盖 Q0 README §Q7 + roadmap line 789 要求的评估面。Phase 2 实现（视评估结论实现或 deferred）是独立后续 plan。

- **裁决可观测性技术选型**（设计文档 §技术选型）：评估候选并裁决——Quarkus 原生扩展（`quarkus-micrometer` + Prometheus exporter / `quarkus-smallrye-health` / `quarkus-opentelemetry`）vs 自建指标导出 vs deferred（接受现状 ring-buffer）。给出候选、考虑的替代、残留风险三要素（plan authoring guide §规则 9）。**显式校正** roadmap/Q0 的「Spring Boot Actuator」假设为「Quarkus 原生」评估方向，记录校正理由（实仓证据：app-erp 运行时为 Quarkus，`app-erp-all/pom.xml:27 → nop-quarkus-web-orm-starter`；nop-entropy 本身多目标非「非 Spring-based」）。
- **评估 nop-entropy 既有 metrics 抽象点可复用性**（设计文档 §现状评估）：核实 `../nop-entropy/` 的 `*MetricsImpl` 抽象（BatchTaskMetrics/JobWorkerMetrics/TaskFlowMetrics 等）是否暴露可被 app 层复用的 metrics API/SPI，还是平台内部抽象。评估 app 层接入路径（复用平台 SPI vs app 层独立 Micrometer 接入）。
- **定义业务指标**（设计文档 §业务指标）：roadmap line 789 要求的业务指标定义——过账成功率 / 期间结账耗时 / 并发冲突率（乐观锁失败率）/ posting-exception 堆积量 / 业务关键路径吞吐；每指标给出采集点（生产代码位置）+ 维度（tag）+ 导出频率。
- **裁决持久化与展示方案**（设计文档 §持久化与展示）：roadmap line 789 要求的持久化方案 + 仪表盘设计——评估 Prometheus + Grafana（时序库）vs 接入既有 ring-buffer 落库 vs deferred；评估是否引入展示层。
- **提供 Phase 2 实施契约**（设计文档 §实施步骤）：若裁决为实现，给出实施步骤骨架 + 验收判据 + CI 门控设计（如适用）；若裁决为 deferred，给出 successor 触发条件。
- **声明与 Q5（性能基线）/ Q4（故障注入）的边界**（设计文档 §边界）：Q7 是**生产运行时可观测性**（metrics/health/trace 导出），Q5 是 CI 回归性能基线（test scope），Q4 是故障注入测试——三者正交，设计文档显式声明边界避免重叠。
- **独立子代理审查收敛**：本文档须经 ≥2 轮独立子代理审查（第 1 轮规范合规 + 第 2 轮覆盖面/可执行性），不同 task id，迭代至 0 BLOCKER / 0 残留 MAJOR，Review Record 持久化。

## Non-Goals

- **不实现任何代码/ORM/CI 变更**——本计划仅产出设计文档（MQ 文档先行工作流 Phase 1 硬约束）。Phase 2 实现（Micrometer 接线 / 业务指标埋点 / Prometheus 导出 / dashboard）须在本文档审查收敛后方可起草，是独立后续 plan。
- **不修改 nop-entropy 源码**——若评估结论须改平台 metrics SPI，那是跨仓库 successor（设计文档登记），不在本 Phase 1。
- **不覆盖前端/浏览器端可观测性**（RUM / 前端 error tracking）——属于前端工程域，Q0 README §候选排除裁决已排除「前端可访问性」类前端关注点；可观测性聚焦后端运行时。
- **不重复 Q5（CI 性能基线）/ Q4（故障注入测试）**——三者正交（生产运行时 vs CI 回归 vs 故障测试），设计文档 §边界显式声明。
- **不预先承诺实现**——若评估结论为 deferred（如 nop-platform 无可用 metrics API + 引入成本超收益），设计文档诚实记录 deferred + successor 触发条件，不强行承诺 Phase 2（roadmap line 680 明示「Phase 2 视评估结论实现或 deferred」）。
- **不覆盖 Q2/Q5 同批工作**（各有独立计划）。

## Task Route

- Type: `app-layer design change`（产出 `docs/architecture/quality-engineering/observability.md` 稳定架构 owner doc；MQ 文档先行工作流 Phase 1）。
- Owner Docs: `docs/architecture/quality-engineering/README.md`（Q0 范围矩阵 + 候选排除裁决）；roadmap §Milestone MQ Q7（line 680 + 789）；`module-finance/erp-fin-service/.../ErpFinPostingMetrics.java`（现状证据）；`../nop-entropy/docs-for-ai/`（平台 metrics/运行时机制权威参考）；`../nop-entropy/nop-quarkus/`（Quarkus 集成层评估）。
- Skill Selection Basis: AGENTS.md 强制技能扫描完成。本工作面为架构设计文档编写 + 平台运行时机制调研——不写 BizModel/Processor（`nop-backend-dev` 不匹配）、不写 view.xml（`nop-frontend-dev` 不匹配）、不写测试类（`nop-testing` 不匹配）、非 bug 调试（`nop-debugging` 不匹配）。Skill: **none**（设计/调研文档；技能选择工作方法而非业务真相，无可复用技能匹配）。

## Infrastructure And Config Prereqs

- No infra prereqs. 本计划纯文档产出（`docs/architecture/quality-engineering/observability.md`），零代码/ORM/CI/端口/密钥/.env/外部服务变更。Phase 2 如裁决实现再评估 infra prereq（Prometheus/Grafana 部署等）。

## Execution Plan

> MQ 文档先行工作流 Phase 1：编写设计文档 → ≥2 轮独立子代理审查 → 收敛。本计划的 closure = 文档收敛（非代码落地）。

### Phase 1 - 编写 observability.md 设计文档草案

Status: completed
Targets: `docs/architecture/quality-engineering/observability.md`（新建）
Skill: none

- Item Types: `Add`
- Prereqs: Q0 README（已存在）；nop-entropy 兄弟目录可访问（调研平台 metrics 抽象）

- [x] Add: 编写 `observability.md` 草案，覆盖以下各节（对齐 sibling 设计文档结构 `security-scanning.md`/`performance-baseline.md`）：
      - **§1 现状评估**（引用 Q0 README §Q7 + 2026-08-02 实仓复核证据，每条带可复现命令）：仅 `ErpFinPostingMetrics` 进程内 ring-buffer（重启即失，javadoc 自认须接 Micrometer）；app-erp 根 pom 零可观测依赖（`rg "quarkus|micrometer|prometheus|actuator" pom.xml` 零命中）；**app-erp 运行时为 Quarkus**（`rg -n "nop-quarkus|nop-spring" app-erp-all/pom.xml` → line 27 `nop-quarkus-web-orm-starter`，决定性证据）；nop-entropy 多目标平台（`ls -d ../nop-entropy/nop-quarkus ../nop-entropy/nop-spring` 并存）+ `*MetricsImpl` 抽象点清单
      - **§2 目标与非目标**
      - **§3 技术选型（Decision）**：候选——Quarkus 原生扩展（`quarkus-micrometer`+Prometheus / `quarkus-smallrye-health` / `quarkus-opentelemetry`）vs 自建指标导出 vs deferred。**显式校正** roadmap line 789 / Q0 README §Q7 的「Spring Boot Actuator」假设为「Quarkus 原生」评估方向（决定性实仓证据：`app-erp-all/pom.xml:27 → nop-quarkus-web-orm-starter` 即 app-erp 运行时为 Quarkus；nop-entropy 本身多目标不构成「非 Spring-based」），记录校正理由。给出候选/替代/残留风险三要素
      - **§4 nop-entropy metrics 抽象点可复用性评估**：核实 `*MetricsImpl`（BatchTaskMetrics/JobWorkerMetrics/TaskFlowMetrics）是平台内部抽象还是暴露 app 可复用 SPI；裁决 app 接入路径（复用平台 SPI vs app 层独立接入）
      - **§5 业务指标定义**：roadmap line 789 要求——过账成功率 / 期间结账耗时 / 并发冲突率 / posting-exception 堆积量 / 关键路径吞吐；每指标采集点 + tag 维度 + 导出频率
      - **§6 持久化与展示方案（Decision）**：Prometheus + Grafana vs ring-buffer 落库 vs deferred
      - **§7 实施步骤（Phase 2 契约）**：若裁决实现，给出步骤骨架 + 验收判据；若 deferred，给出 successor 触发条件
      - **§8 与 Q5/Q4 的正交边界声明**
      - **§9 残留风险与 successor**
      - Skill: none

Exit Criteria:

> 本阶段交付文档草案可进入审查。无代码 → 无 mvn 验证门控（plan authoring guide：无代码更改的计划删除验证命令门控）。

- [x] `observability.md` 草案落盘，含上述 9 节 + 单一真相源依赖声明（引用 Q0 README + roadmap + 实仓证据，不重推导）

### Phase 2 - 独立子代理 R1 规范合规审查 + 修订

Status: completed
Targets: `docs/architecture/quality-engineering/observability.md`（修订）
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 1 草案落盘

- [x] Proof: 独立子代理 R1（规范合规审查，fresh session cold context，不同 task id）审查文档——结构完整性 / 与项目约定一致（AGENTS.md + roadmap authoring guide + plan authoring guide）/ 反模式检查 / 引用正确 owner doc / **Quarkus vs Spring 假设校正的证据准确性** / 单一真相源不重推导。输出 BLOCKER/MAJOR/MINOR 分级意见，作者修订
      - R1 task id: `ses_0410de8f4ffeskYmXll7CeAiCz`；结论：**accept**（0 BLOCKER / 0 MAJOR / 0 formal MINOR）；独立核验全 PASS（含 `mvn dependency:list` 复核 + Quarkus/Spring 校正一致性）；无需修订。Review Record 已持久化 R1 轮。
      - Skill: none

Exit Criteria:

- [x] R1 审查完成（task id 记录），R1 意见全部修订（0 残留 BLOCKER/MAJOR 或降至 MINOR）；修订摘要落盘 §Review Record

### Phase 3 - 独立子代理 R2 覆盖面/可执行性审查 + 修订 + 收敛

Status: completed
Targets: `docs/architecture/quality-engineering/observability.md`（修订 + 收敛）
Skill: none

- Item Types: `Proof | Decision`
- Prereqs: Phase 2 R1 修订完成

- [x] Proof: 独立子代理 R2（覆盖面/可执行性审查，**另一个** fresh session，不同 task id，与 R1 不同会话）审查——目标可达 / 评估步骤可执行 / 业务指标采集点可定位 / 技术选型替代充分评估 / 与 Q5/Q4 边界无重叠 / 是否与现有基础设施冲突（nop-quarkus 集成层）。输出 BLOCKER/MAJOR/MINOR，作者修订
      - R2 task id: `ses_0410acaebffeb4r2GVtZKs7gmQ`；结论：**accept-after-revision**（0 BLOCKER / 4 MAJOR / 2 MINOR）。4 MAJOR 均为 §5/§7/§9 局部问题（Metric 5 采集点事实错误 / Metric 4 catch 点不存在 / ErpFinPostingMetrics 迁移契约面 / CI 门控理由自相矛盾），**不触及** §3/§6 核心 Decision。作者修订全部应用（4 MAJOR + 2 MINOR），§Review Record 持久化 R2 + 收敛结论。
      - Skill: none
- [x] Decision: 收敛判定——若 R2 后 0 残留 BLOCKER/MAJOR，文档收敛；若仍有 MAJOR，继续修订重审直至收敛（对齐 sibling 设计文档 2-3 轮收敛先例）
      - 收敛判定：R1 = accept（0/0/0）+ R2 修订后 0 残留 BLOCKER / 0 残留 MAJOR → **文档收敛**。两轮不同 task id（`ses_0410de8f4…` / `ses_0410acae…`）+ 两轮均 fresh cold context。
      - Skill: none

Exit Criteria:

- [x] R2 审查完成（不同 task id），0 残留 BLOCKER / 0 残留 MAJOR；§Review Record 持久化（R1 + R2 两轮 task id + 结论 + 修改摘要）；审查者多样性满足（两会话不同 task id，均非作者会话）

## Draft Review Record

- Independent draft review iteration 1: **needs-revision**（`ses_04179c909ffeD2lxDTzzvkqZy`，独立子代理 fresh session cold context）— 0 BLOCKER / 1 MAJOR / 2 MINOR。全面 PASS：doc-only plan 正确删除 mvn gate + 说明理由 / 文档先行工作流（Phase 1 编写 → Phase 2 R1 规范合规 → Phase 3 R2 覆盖面/可执行性，不同 task id，收敛 0 BLOCKER/0 MAJOR）/ Quarkus 校正**方向**正确且经 AGENTS.md 授权 / Phase 2 实现正确为命名 successor。**MAJOR-1（证据链不精确）**：Current Baseline + Deferred 断言「nop-entropy 是 Quarkus-based（非 Spring-based）」证据不足且事实上不精确——`../nop-entropy/nop-spring/` 亦存在（starter 含 `spring-boot-starter-actuator`），nop-entropy 是多目标平台；决定性证据是 app-erp 自身运行时选择 `app-erp-all/pom.xml:27 → nop-quarkus-web-orm-starter`。MINOR：Phase 2/3 split 与 sibling 结构分歧（非错）；§1 缺 app-erp 根 pom 复验命令。
- Independent draft review iteration 2: **needs-revision**（`ses_0417558f1ffeARzkmpZ7Ci1mUP`，独立子代理 fresh session cold context）— MAJOR-RESOLVED=partial。两处显式命名位置（Current Baseline L21 + Deferred L150-154）+ Phase 1 §3 todo 已正确修正（app-erp 运行时 = Quarkus 决定性证据 + 多目标平台框架）。**REMAINING-MAJOR-1**：Goals §技术选型 L30 仍残留原始不精确框架 `（实仓证据：nop-entropy Quarkus-based）`，与已修正的 Current Baseline 自相矛盾（同名 MAJOR 类的遗漏位置）。MINOR：Phase 1 §1 todo 的 Quarkus 证据为 file:line 引用而非可复现命令。
- Independent draft review iteration 3: **accept / converged**（`ses_041737c58ffeAREKHY6kbOpFn6`，独立子代理 fresh session cold context）— 0 BLOCKER / 0 MAJOR / 0 MINOR。**MAJOR-CLASS-FULLY-RESOLVED=yes**：全部 5 处 Quarkus/Spring 校正位置（Current Baseline L21 / Goals L30 / Phase 1 §1 L71 / Phase 1 §3 L73 / Deferred L150-154）一致使用决定性证据 `app-erp-all/pom.xml:27 → nop-quarkus-web-orm-starter`（app-erp 运行时 = Quarkus）+ nop-entropy 多目标框架；「非 Spring-based」仅以 debunk 形式出现（"不得误述"/"是伪问题"），无任何位置将其作为证据断言。实仓复核全部 PASS。holistic：doc-only mvn gate 删除 + 理由 / ≥2 轮独立审查不同 task id / Phase 2 实现 successor / anti-looseness（Follow-up 命名触发条件）。iter2 REMAINING-MAJOR（L30）+ MINOR（§1 可复现命令）均已修订。→ converged → 转 active。

> 审查者多样性：iter1（ses_04179c909…）/ iter2（ses_0417558f1…）/ iter3（ses_041737c58…）三会话 task id 不同，均独立 fresh cold context，未复用作者上下文。

## Closure Gates

> 本计划是 Phase 1 设计文档编写，**无代码/ORM/CI 变更**。按 plan authoring guide，对无代码更改的计划删除 `mvn` 验证命令门控并说明原因（本计划纯文档产出）。closure = 文档收敛 + Review Record 完整。

- [x] 范围内行为完成：`observability.md` 落盘含 §1-§9 全节 + 单一真相源依赖声明
- [x] Quarkus vs Spring 假设校正已落盘（§3 显式记录校正理由 + 实仓证据）
- [x] 技术选型 Decision 三要素齐备（候选/替代/残留风险）——裁决为实现或 deferred 均可接受（诚实记录）
- [x] 与 Q5/Q4 正交边界声明（§8）
- [x] 独立子代理审查收敛：≥2 轮（R1 规范合规 + R2 覆盖面/可执行性），不同 task id，0 残留 BLOCKER / 0 残留 MAJOR；§Review Record 持久化
- [x] 相关文档对齐：`docs/logs/2026/08-02.md` 追加日志条目；roadmap Q7 工作项 Phase 1 完成在 closure 时回填（Q7 整体 done 须 Phase 2 closure，本计划仅 Phase 1）
- [x] 无代码/ORM/CI 变更（git diff 仅 `docs/architecture/quality-engineering/observability.md` + logs + roadmap 回填）
- [x] 独立草案审查（本计划）已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留空作为人工门控占位符（见下方 Closure Audit Evidence `ses_041065a21…`：首轮 FAIL 纯 closure 簿记，实质性 gate 1-5/7-8/10 全 PASS；执行者据审计意见完成 5 项簿记整改：Plan Status active→completed + 11 Closure Gates 勾选 + Closure section 填写 + 日志追加 + roadmap 回填）
- [x] 结束证据存在于文件中（见下方 Closure Audit Evidence + Review Record R1/R2）

## Deferred But Adjudicated

### Q7 Phase 2 实现（Micrometer 接线 / 业务指标埋点 / Prometheus 导出 / dashboard）

- Classification: `out-of-scope improvement`（视 Phase 1 评估结论）
- Why Not Blocking Closure: 本计划是 Phase 1 设计文档；Phase 2 实现（roadmap line 680 明示「视评估结论实现或 deferred」）须在本文档审查收敛后方可起草，是独立后续 plan。
- Successor Required: yes —— 触发条件：本文档审查收敛 + 技术选型 Decision 落定。若裁决为实现，DRAFT_PLANS 起草 Phase 2 实现 plan（加载对应 skill）；若裁决为 deferred（如平台无可用 metrics API + 成本超收益），successor 触发条件 = nop-entropy 暴露可复用 metrics SPI 或生产可观测性需求明确化。

### Spring Boot Actuator 评估方向

- Classification: `watch-only residual`
- Why Not Blocking Closure: 2026-08-02 实仓复核证实 **app-erp 运行时为 Quarkus**（`app-erp-all/pom.xml:27 → nop-quarkus-web-orm-starter`）；nop-entropy 本身是多目标平台（nop-quarkus/ + nop-spring/ 并存），故「nop-entropy 是否 Spring-based」是伪问题。roadmap line 789 / Q0 README §Q7 的「Spring Boot Actuator」评估方向已被 §3 校正为「Quarkus 原生」。Spring Boot Actuator 仅在 app-erp 运行时迁移到 Spring 时才相关。
- Successor Required: yes —— 触发条件：app-erp 运行时框架从 Quarkus（`nop-quarkus-web-orm-starter`）迁移到 Spring（`nop-spring-*-starter`）。

## Closure

Status Note: **closed**（2026-08-02）。独立结束审计首轮（`ses_041065a21ffe4YHB1h1jzQ1SQt`）对实质性内容 gate 1-5/7-8/10 全 PASS——设计文档 9/9 节 + 双 Decision 三要素 + Quarkus/Spring 校正一致 + R1/R2 收敛 + 无代码变更 + mvn gate 正确删除 + 审查者多样性——但首轮对 closure 簿记 gate 6/9 判 **FAIL**（Plan Status 未转 completed / 11 Closure Gates 未勾选 / Closure section 未填 / 日志未追加 / roadmap 未回填）。执行者据审计意见完成 5 项簿记整改（见下），据审计报告「substantive design work is converged and sound; only the closure bookkeeping is missing」+ 全实质性 gate PASS，closure 成立。

Closure Audit Evidence:

- **独立结束审计**（`ses_041065a21ffe4YHB1h1jzQ1SQt`，fresh cold context，独立于作者/R1`ses_0410de8f4…`/R2`ses_0410acae…`/草案 3 轮）：
  - **Gate 1（§1-§9 + 单一真相源 header）PASS**：observability.md 含 §1 现状评估 L17 / §2 目标与非目标 L111 / §3 技术选型 Decision L134 / §4 metrics 抽象点可复用性评估 L179 / §5 业务指标定义 L219 / §6 持久化与展示 Decision L246 / §7 实施步骤 L288 / §8 与 Q5/Q4 正交边界 L324 / §9 残留风险与 successor L342；header L7-15 声明单一真相源依赖。
  - **Gate 2（Quarkus vs Spring 校正落盘）PASS**：§3.1 记录校正理由 + 决定性证据 `app-erp-all/pom.xml:27 → nop-quarkus-web-orm-starter`；「非 Spring-based」仅 debunk 形态，正向框架统一「多目标平台」。
  - **Gate 3（Decision 三要素）PASS**：§3 候选 L150/替代 L167/残留风险 L173；§6 候选 L250/替代 L278/残留风险 L283。
  - **Gate 4（Q5/Q4 正交）PASS**：§8 正交性表 + Q7-vs-Q5/Q7-vs-Q4/Metric 5 论证。
  - **Gate 5（独立审查收敛）PASS**：R1 `ses_0410de8f4…` accept（0/0/0）+ R2 `ses_0410acae…` accept-after-revision（0 BLOCKER/4 MAJOR+2 MINOR 全应用）；Review Record L360-389 持久化。
  - **Gate 6（文档/日志对齐）首轮 FAIL→整改 PASS**：Phase 1/2/3 items 全 `[x]` + Status completed；首轮 Plan Status 仍 active + 日志/roadmap 未回填 → 整改（Plan Status→completed + 日志追加 + roadmap 回填）。
  - **Gate 7（无代码/ORM/CI 变更）PASS**：`git status --short` = `M docs/plans/2026-08-02-1121-3-…md` + `?? docs/architecture/quality-engineering/observability.md` + 日志/roadmap 回填；零 `*.java`/`*.orm.xml`/CI `*.yml`/`pom.xml` 变更。
  - **Gate 8（mvn gate 正确删除）PASS**：plan L84 + L135 按 plan authoring guide 删除 mvn 门控并说明原因（纯文档产出）。
  - **Gate 9（文本一致性）首轮 FAIL→整改 PASS**：Plan Status active→completed + 11 Closure Gates 全勾选 + Closure section 填写；零 Status:completed 与残留空 checkbox 矛盾。
  - **Gate 10（审查者多样性）PASS**：R1/R2/草案 3 轮/结束审计 7 会话 task id 全不同，均独立 fresh cold context。
  - **scope-violation check PASS**：strictly doc-only。
- **整改完成态**：执行者据首轮审计的 5 项整改清单（Plan Status active→completed / 11 Closure Gates 勾选 / Closure section 填写 / `docs/logs/2026/08-02.md` 追加 Q7 Phase 1 条目 / roadmap line 680 回填 Phase 1 done）全部应用。实质性 gate 全 PASS + 簿记整改完成 → closure 成立。

Follow-up:

- **Q7 Phase 2 实现 successor**（见上 Deferred）：视本文档 §3 + §6 Decision（裁决为实现，非 deferred）——触发条件已满足（本文档审查收敛 + 技术选型落定）。DRAFT_PLANS 起草 Phase 2 实现 plan（加载 `nop-backend-dev` skill——业务指标埋点触及 BizModel/Processor），plan 引用本文档作范围与验收依据。Q7 工作项整体 done 须 Phase 2 closure。
- **Q2 Phase 2 / Q5 Phase 2** 各有独立计划（同批 `2026-08-02-1121-1` / `-2`，已 done）。
