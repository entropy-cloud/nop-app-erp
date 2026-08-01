# 质量深化（MQ）范围矩阵与实施基线

> Owner Doc for Milestone MQ（audit-remediation §质量深化）
> 创建日期：2026-08-01
> 单一真相源依赖：本 README 是 **Q0 工作项**的产物，为 Q1-Q7 各自 Phase 1 设计文档提供**范围矩阵 + 复杂度分级 + 实施顺序裁决**的引用基线。
> 上游真相源：`docs/backlog/audit-remediation-roadmap.md` §Milestone MQ（line 660-789 工作项表 + line 791-827 依赖图 + line 843-862 文档先行工作流）。本 README **只引用** roadmap 与计划核验事实，不重新推导，避免双真相源漂移。

## 目的

`nop-app-erp` 在 audit-remediation MR1-MR6 全部 done + MR6 milestone CLOSED 之后，系统性 gap analysis 确认 6 个质量维度**完全空白**（变异测试 / 属性测试 / 安全扫描 / 故障注入 / 性能基线 / 可观测性）+ 1 个**已知反复痛点**（时钟测试基础设施并行不安全）。这 7 个维度是审计-修复未触及的新领域，缺乏既有 owner doc 和实施先例。

本 README 解决一个具体问题：**Q1-Q7 每个工作项独立决定实施顺序与技术选型时会漂移**。Q0 的产出是稳定的范围矩阵 + 复杂度分级 + 实施顺序裁决，使后续各维度的 Phase 1 设计文档（`<dimension>.md`）与 DRAFT_PLANS 起草的执行计划有一个共同的引用基线。

本 README **不实现任何质量维度本身**——每个维度的技术选型、实施步骤、验收判据属于各自 Phase 1 设计文档（`mutation-testing.md` / `security-scanning.md` / `property-based-testing.md` / `fault-injection.md` / `performance-baseline.md` / `clock-test-infrastructure.md` / `observability.md`）。

## 文档先行工作流（引用回 roadmap）

本 README 是 MQ 里程碑**文档先行工作流**的范围与顺序基线。完整工作流定义在 `docs/backlog/audit-remediation-roadmap.md` §横切关注点 §文档先行工作流（line 843-862），摘要如下：

1. **Phase 1 — 设计文档编写 + 独立子代理反复审查循环（≥2 轮）**：为该质量维度编写设计/策略文档（落盘本目录 `<dimension>.md`），覆盖现状评估（引用实仓证据）/ 目标与非目标 / 技术选型（含替代方案与裁决理由）/ 实施步骤 / 验收判据 / CI 门控设计（如适用）。文档须经独立子代理反复审查至收敛（第 1 轮规范合规审查 + 第 2 轮覆盖面/可执行性审查），每轮输出修改意见，作者修订后重审。审查记录持久化在文档 `## Review Record` 节。**审查通过后方可编写实现 plan。**
2. **Phase 2 — 按文档实现**：以审查通过的文档为实施契约，编写 plan 并执行（plan 引用文档作为范围与验收依据）。
3. **Phase 3 — 独立 closure audit**：由独立子代理验证实现与设计文档一致 + 验证命令全绿 + 设计文档 Review Record 完整。

> 审查者多样性：同一文档的 2 轮审查应由不同子代理会话执行（不同 task id），审查者不可与作者为同一会话。

本 README 的 §实施顺序裁决 决定 Q1-Q7 各 Phase 1 设计文档计划的起草顺序。

## 范围矩阵（7 维度 NOT FOUND 证据引用）

> 证据核验日期：2026-08-01（计划 `docs/plans/2026-08-01-1121-1-mq-q0-quality-gap-analysis-readme.md` §Current Baseline 已核验；本节在落盘 README 时由执行者复核各命令零命中/单命中，确保可复现）。
> 证据**引用**自 roadmap §当前基线（line 697）+ plan §Current Baseline，不重新推导。每条标注可复现核验命令，便于后续 plan 复验。

### Q1 — 变异测试有效性（pitest）

- **现状**：全仓零 pitest 配置。审计文档 MA5 反复写"需 pitest 运行"但从未执行 → 1903 个测试的 mutation score 完全未知。
- **核验命令**（2026-08-01 复核零命中）：`rg "pitest" --glob '*.xml'`
- **引用源**：roadmap line 697 + line 783-784（Q1 维度说明）。

### Q2 — 安全扫描流水线

- **现状**：全仓零安全扫描依赖/CI job。CI `.github/workflows/` 零安全扫描 job；MA6 仅做 RBAC 注解审计，传递依赖 CVE 与静态安全规则完全无人看。
- **核验命令**（2026-08-01 复核零命中）：`rg -il "owasp|dependency-check|spotbugs|findsecbugs|sonarqube|snyk" --glob '*.xml' --glob '*.yml' --glob '*.yaml'`
- **引用源**：roadmap line 697 + line 785（Q2 维度说明）。

### Q3 — 属性测试（jqwik）

- **现状**：全仓零属性测试依赖。当前测试均为黄金路径具体断言，无法证明 ERP 强不变量（借贷必相等 / 期间结账余额归零 / 成本层累加 = 余额表）在任意操作序列下恒成立。
- **核验命令**（2026-08-01 复核零命中）：`rg "jqwik|quickcheck" --glob '*.xml'`
- **引用源**：roadmap line 697 + line 786（Q3 维度说明）。

### Q4 — 故障注入测试

- **现状**：零故障注入基础设施。审计反复发现 tryPost 吞异常 → posted=false 静默悬挂（finance P1-MA2-032 / hr P1-MA2-048 / assets P1-MA2-060 / qa P1-MA2-064 / projects P1-MA2-068 / maintenance P1-MA2-074 同型根因跨 6 域），MR1.16 已修单点但无系统性回归保护。
- **核验命令**（2026-08-01 复核零命中）：`rg -il "fault.injection|FaultInjector|@InjectFault" --glob '*.java' --glob '*.xml'`
- **引用源**：roadmap line 697 + line 787（Q4 维度说明）。

### Q5 — 性能基线与回归门控

- **现状**：全仓零性能测试基础设施。性能测试被显式 defer 到"首次生产数据规模"（`db-index-design.md`、`posting.md` 等）但无任何基础设施。
- **核验命令**（2026-08-01 复核零命中）：`rg "PerfTest|Benchmark|JMH|Gatling" --glob '*.java' --glob '*.xml'`
- **引用源**：roadmap line 697 + line 788（Q5 维度说明）。
- **硬依赖**：**Q6 先于 Q5**（Q5 性能基线的测量确定性依赖 Q6 时钟硬化——性能计时/期间数据若随墙钟漂移，基线不可复现）。

### Q6 — 时钟测试基础设施硬化（已知反复痛点）

- **现状**：`CoreMetrics.registerClock` 全局静态（`module-common-test/.../AbstractFrozenClockExtension.java:63,68`），15 域子类共用一个全局静态时钟槽 → 并行不安全。
- **核验命令**（2026-08-01 复核命中两行）：`rg -n "registerClock" module-common-test/`
- **痛点**：bug `docs/bugs/2026-08-01-finance-period-resolution-month-rollover.md` 记录月初翻车重录快照税——每月月初 finance 域 4 个测试类（`TestErpFinBadDebtReversal` / `TestErpFinEmployeeAdvanceCashRepayReversal` / `TestErpFinNotesPayableStateMachine` / `TestErpFinDashboard`）由绿转红（1 failure + 10 errors），极易被误判为最近变更回归。
- **引用源**：roadmap line 697 + line 789（Q6 维度说明）+ bug doc。

### Q7 — 可观测性补全评估

- **现状**：nop-platform 无 Micrometer/Prometheus/OTel/metrics API。仅 `module-finance/erp-fin-service/.../ErpFinPostingMetrics.java` 命中可观测性关键字，其 import 仅 Nop `CoreMetrics`（进程内 ring-buffer，重启即失）+ 进程内 traceId。
- **核验命令**（2026-08-01 复核单文件命中）：`rg -il "micrometer|prometheus|opentelemetry|otel" --glob '*.java'`
- **引用源**：roadmap line 697 + line 789（Q7 维度说明）。

## 复杂度分级

> 三轴评级，作为实施顺序裁决的输入。评级依据记录在每维度后。
> 轴定义：**基础设施接入难度**（引入新工具/依赖/CI 的阻力）/ **涉及模块范围**（改动跨多少模块）/ **对 nop-entropy 平台依赖程度**（是否需要改平台或与平台机制深度耦合）。
> 评级符号：`低` / `中` / `高`。

| 维度 | 基础设施接入难度 | 涉及模块范围 | 对 nop-entropy 平台依赖程度 | 评级依据 |
|------|------------------|--------------|------------------------------|----------|
| **Q1 变异测试** | 中（pitest Maven 插件 + JVM agent） | 中（聚焦 finance/mfg/inv 三域，但全仓 build profile） | 低（测试期工具，不动平台生产代码） | pitest 是标准 Maven 插件；关键风险在配置 `excludedClasses`/`targetClasses` 排除 `_gen` 包（roadmap line 783）。平台依赖低：只读字节码，不改 nop-entropy。 |
| **Q2 安全扫描** | 高（OWASP Dependency-Check NVD 限速 + FindSecBugs 规则集 + CI 调度） | 高（156 模块传递依赖全量） | 低（工具链 + CI 层，不动平台生产代码） | 关键风险是 NVD API 限速使 per-commit 全量扫描不现实，须裁决 aggregate 模式 + nightly 调度（roadmap line 784）。平台依赖低：扫描依赖树与编译产物。 |
| **Q3 属性测试** | 中（jqwik 库 + JUnit 5 集成） | 中（finance/库存核心不变量，应用层） | 中（须裁决与 `JunitAutoTestCase` + RECORDING/CHECKING 快照的语义冲突——roadmap line 786） | 关键风险是 jqwik 多迭代与 Nop 测试栈快照录制一次 vs 回放多次冲突，须裁决是否绕过 `JunitAutoTestCase`。平台依赖中：测试夹具重置策略涉及平台测试基类。 |
| **Q4 故障注入** | 高（harness 设计 + 受控异常/超时/事务回滚注入点 + mock dispatcher） | 高（6 域过账悬挂路径跨域） | 中-高（须裁决应用层 test-scope bean 覆盖 `IPostingDispatcher` vs 平台层字节码插桩——roadmap line 787） | 关键风险是明确 nop-entropy 改造 vs 应用层边界，首选应用层方案避免跨仓库依赖。平台依赖中-高：注入点触及平台过账 dispatcher SPI。 |
| **Q5 性能基线** | 高（JMH vs 简单 timing + 数据规模 × 时间阈值 + CI 软门控） | 中（关键路径 4 条） | 中（计时确定性硬依赖 Q6 时钟硬化） | 关键路径选择 + 基线定义需慎重；平台依赖中：测量方法可能与平台事务/缓存机制耦合。**硬依赖 Q6 先完成**。 |
| **Q6 时钟硬化** | 中（两种修复路径：平台 thread-local clock vs 测试侧日期参数化） | 中-高（15 域子类兼容） | 高（路径 A 直接触及 nop-entropy `CoreMetrics`） | 关键风险是全局静态 vs thread-local 的根因裁决 + 两种修复路径评估（roadmap line 789）。平台依赖高：根治须改平台 `CoreMetrics` 或所有 15 域子类迁移。 |
| **Q7 可观测性** | 高（Spring Boot Actuator 可行性 + 业务指标定义 + 仪表盘 + 持久化） | 中-高（业务指标跨域） | 高（须评估 nop-entropy 是否 Spring-based 可直接注入——roadmap line 789） | 关键风险是 Actuator 引入可行性未确认 + nop-platform 是否暴露 metrics API。平台依赖高：深度依赖 nop-entropy 运行时框架特性。 |

### 复杂度分级综合观察

- **平台依赖最高**：Q6、Q7（均须评估/改造 nop-entropy 运行时机制）。Q6 是已知痛点且阻塞 Q5，紧迫度最高。
- **基础设施接入最高**：Q2（CVE 限速 + CI 调度）、Q4（harness 设计）、Q5（基线 + 门控）、Q7（Actuator 评估）。
- **平台依赖最低（纯测试期工具）**：Q1（pitest 仅读字节码）、Q2（扫描依赖树）——这两项可最先接入而不动平台。
- **已知痛点紧迫度**：Q6 时钟月初翻车税为**唯一反复发作的痛点**（每月初 CI 红 + 误判回归成本），其余为"从未做过"的空白。

## 候选维度排除裁决

> roadmap line 782 给出 5 个候选排除维度的排除方向。本节正式化裁决并补 successor 触发条件。
> 裁决原则：MQ 聚焦"7 维度深化"，避免范围爆炸；候选维度内容能折入现有 Q1-Q7 的优先折入，独立价值高且 successor 触发条件明确的登记为独立 successor。

### 1. 契约测试（Contract Testing / runtime Pact）

- **裁决**：**排除**（作为 successor 候选）。
- **理由**：19 个 api 模块 + GraphQL 契约面已由 A3.6 API 契约一致性审计提供**静态基线**（`docs/audits/2026-07-28-1953-arm-ma3-api-contract-consistency.md`，零 BLOCKER；4 项 P1-MA3-046~049 已登记 MR2）。静态契约审查已覆盖签名/参数/返回类型一致性。运行时 Pact（consumer-driven contract）的增量价值在当前单组织、无跨进程 RPC 需求（A3.6 裁决：api.xml 缺失=设计选择）的部署形态下有限。
- **Successor 触发条件**：项目引入跨进程 RPC（多服务部署）或外部 B2B 消费方需要正式契约保证时，运行时 Pact 升级为独立工作项。

### 2. 前端可访问性（Accessibility / a11y）

- **裁决**：**排除**（独立 successor）。
- **理由**：前端可访问性（WCAG / axe-core / 屏幕阅读器兼容）属于**前端工程域**，与 MQ 后端质量深化（变异/属性/安全/故障/性能/时钟/可观测性）正交。MQ 不混入前端工程关注点，避免范围失焦。
- **Successor 触发条件**：前端工程化里程碑（或产品化合规要求明确 a11y 法规遵从）启动时，作为前端独立工作项。

### 3. 数据迁移测试（Data Migration Testing）

- **裁决**：**排除**（低优先级 watch）。
- **理由**：数据库 schema 经 MR1-MR6 已稳定（audit-remediation 全 done + MR6 CLOSED），当前无活跃的 schema 迁移需求。数据迁移测试在 schema 稳定期边际收益低。
- **Successor 触发条件**：发生破坏性 schema 变更或引入版本化数据迁移脚本（如 Flyway/Liquibase）时升级。

### 4. Nop 升级兼容性（Nop Platform Upgrade Compatibility）

- **裁决**：**排除**（发布流程域）。
- **理由**：Nop 升级兼容性测试属于**发布流程域**（依赖版本管理、回归测试矩阵），而非质量深化维度。nop-app-erp 锁定 nop-entropy 兄弟目录构建，升级是显式人工决策事件而非持续质量门控。
- **Successor 触发条件**：nop-entropy 版本解耦（不再兄弟目录锁定）或引入定期平台升级节奏时，作为发布流程工作项。

### 5. i18n 深度（i18n Depth / locale correctness）

- **裁决**：**部分纳入 Q3 + 其余 successor**。
- **理由**：i18n **覆盖完整性**已由 A4.9 全域 i18n 审计 PASS（零基线，`docs/audits/2026-07-29-0749-arm-ma4-i18n-coverage.md`）+ F15 checker 覆盖。i18n **locale 正确性**（日期/数字/货币格式化在不同 locale 下不变量成立）可部分折入 Q3 属性测试作为属性用例。剩余深度（翻译质量、文化适配、复数/性数格变化）属于本地化产品化域，不在 MQ 测试有效性范围。
- **Successor 触发条件**：Q3 属性测试落地后，locale 正确性属性用例作为 Q3 子集；产品化多语言发布时，翻译质量作为本地化 successor。

> **残留风险**：候选排除维度后续可能因业务变化重新进入视野。上述 successor 触发条件已记录，DRAFT_PLANS 可据此重开任意维度为独立工作项。

## 实施顺序裁决

> 决策输入：依赖图（roadmap line 791-827）+ 复杂度分级（上节）+ 已知痛点紧迫度（Q6 时钟月初翻车税为反复痛点）。
> **硬约束**：Q6 先于 Q5（性能基线的测量确定性依赖时钟硬化）；Q1↔Q4 协同关系须体现（Q1 发现的测试盲区类正是 Q4 应优先覆盖的可恢复性路径）。

### 候选顺序（推荐）

```
Q6 → Q1 → Q4 → Q3 → Q2 → Q5 → Q7
```

| 顺序 | 维度 | 排期理由 |
|------|------|----------|
| 1 | **Q6 时钟硬化** | 解除 Q5 硬阻塞 + 消除唯一反复发作痛点（月初翻车税）。平台依赖高但范围清晰、收益立竿见影。 |
| 2 | **Q1 变异测试** | 平台依赖最低（纯测试期工具），可最先产出测试有效性基线（mutation score），为 Q4 提供盲区类清单。 |
| 3 | **Q4 故障注入** | 与 Q1 协同相邻排期——Q1 发现的盲区类正是 Q4 应优先覆盖的可恢复性路径（tryPost 吞异常同型根因跨 6 域）。 |
| 4 | **Q3 属性测试** | ERP 强不变量形式化验证，价值高；平台依赖中（须裁决与 JunitAutoTestCase 快照冲突），排在 Q1/Q4 后可复用其测试基础设施决策。 |
| 5 | **Q2 安全扫描** | 基础设施接入高（NVD 限速 + CI 调度），独立性强、不阻塞其他维度；排序靠后因其首次扫描必然有发现、分类工作流需先沉淀。 |
| 6 | **Q5 性能基线** | 硬依赖 Q6 已满足；平台依赖中（计时与事务/缓存耦合），在质量维度基础设施（Q1/Q4/Q3）稳定后建立基线更可信。 |
| 7 | **Q7 可观测性** | 平台依赖最高（Actuator 可行性未确认 + nop-platform metrics API 评估），且本质是评估（Phase 2 视评估结论实现或 deferred），放最后避免阻塞。 |

### 考虑的替代顺序

- **纯文档顺序 Q1→Q7**：**否决**——忽视 Q6→Q5 硬依赖（Q5 在 Q6 前会建立不可复现的基线）+ 忽视 Q6 反复痛点紧迫度。
- **全并行（Q1-Q7 同时）**：**否决**——Q5 硬依赖 Q6（技术上不可并行）；且 7 维度全并行会稀释独立子代理审查资源（文档先行工作流要求每文档 ≥2 轮独立审查）。

### 残留风险

- **顺序非绝对**：本裁决为**建议顺序**，可在后续各维度 plan 起草时复议并回填此处。依赖图允许 Q1-Q4 + Q6 + Q7 在 Q0 之后并行（仅 Q5 须等 Q6）；若资源允许，Q6/Q1/Q2/Q7 等独立维度可并行推进。
- **复杂度可能在执行中浮现**：Q6/Q7 的平台依赖评估（thread-local clock 改造 / Actuator 引入）若在 Phase 1 文档审查中暴露超出预期的工作量，顺序可调整。
- **Q1↔Q4 协同假设**：协同关系基于"变异测试盲区 = 故障注入优先覆盖"的假设，若 Q1 实际盲区分布与过账悬挂路径不重合，Q4 排期可独立前移。

> 本裁决须与 roadmap 依赖图（line 791-827）一致：Q6→Q5 硬依赖已体现（Q6 在第 1，Q5 在第 6）；Q1-Q4/Q6/Q7 仅依赖 Q0（裁决中均在 Q0 之后）。如裁决与依赖图冲突，以依赖图为准并回填本节。

## 与上游真相源的对齐

- **roadmap §Milestone MQ 工作项表**（line 671-680）：Q0-Q7 八工作项的定义与依赖（Q5 deps = Q0+Q6）与本 README 一致。
- **roadmap §依赖图**（line 791-827）：Q0→Q1/Q2/Q3/Q4/Q6/Q7 + Q6→Q5 硬依赖，与本 README §实施顺序裁决一致。
- **plan §Current Baseline NOT FOUND 证据**：本 README §范围矩阵引用（非重推导）该段已核验事实，每条标注可复现核验命令。
- **无回填 roadmap 注记**：本 README 是 Q0 产物，其顺序裁决与 roadmap 依赖图天然一致（Decision 项已约束 Q6→Q5），不产生冲突，故不回填 roadmap（避免双真相源漂移）。
