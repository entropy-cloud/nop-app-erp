# 性能基线与回归门控（MQ Q5）—— Phase 1 设计文档

> Owner Doc for Milestone MQ Q5（性能基线与回归门控）
> 创建日期：2026-08-01
> Plan：`docs/plans/2026-08-01-1121-3-mq-q5-performance-baseline-design-doc.md`
> 单一真相源依赖：本文档是 MQ 文档先行工作流 **Phase 1** 产物（设计/策略文档），**不实现任何代码/ORM/CI 变更**。Phase 2 实现 plan（基线测量 harness + 4 关键路径基线数据 + CI 软门控）须在本文档审查收敛后方可起草。
> 上游真相源（**只引用**，不重推导，避免双真相源漂移）：
> - `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q5（line 678 工作项表 + line 787 维度说明 + §横切关注点 §文档先行工作流 line 843-862 + 依赖图 line 824 `Q6 --> Q5`）
> - `docs/architecture/quality-engineering/README.md`（Q0 范围矩阵 + 复杂度分级 + 实施顺序裁决基线，Q5 位 6，硬依赖 Q6 已在 README 依赖图与实施顺序裁决体现）
> - `docs/plans/2026-08-01-1121-1-mq-q0-quality-gap-analysis-readme.md` §Current Baseline（Q5 NOT FOUND 实仓证据已核验：零性能测试基础设施）
> - `docs/plans/2026-08-01-1357-1-mq-q6-clock-test-infrastructure-impl.md`（Q6 Phase 2 实现，Status: completed——`ThreadLocalFrozenClock` 落地，Q5 测量确定性硬依赖已满足）
> - `docs/architecture/quality-engineering/clock-test-infrastructure.md`（Q6 设计文档——时钟硬化产物真相源，Q5 测量确定性依赖）
> - `docs/design/finance/posting.md` + `posting-log.md`（凭证过账关键路径真相源 + `ErpFinVoucherBillR` 索引热查询路径）
> - `docs/design/finance/period-close.md`（期间结账关键路径真相源）
> - `docs/design/finance/costing-methods.md`（库存核算 reclose 关键路径真相源）
> - sibling plan `docs/plans/2026-08-01-1158-3-mq-q4-fault-injection-design-doc.md`（Q4，本文档结构参照）

## 1. 现状评估

> 本节**引用**（非重推导）上游真相源已核验事实，每条标注可复现核验命令 + 核验日期，便于 Phase 2 plan 与独立审查复核。证据核验日期：2026-08-01（HEAD 含 R6.9 收口 + Q6 Phase 2 落地）。

### 1.1 全仓零性能测试基础设施（关键字零命中）

ERP 全仓无任何性能测试基础设施——无 `*PerfTest*` 类、无 `*Benchmark*` 类、无 JMH 配置、无 Gatling 脚本、无任何 `@Benchmark` 注解。性能测试被显式 defer 到"首次生产数据规模"，但**无任何基础设施承载该 defer**。

- 核验命令（2026-08-01 复核零命中）：`rg "PerfTest|Benchmark|JMH|Gatling" --glob '*.java' --glob '*.xml'`（工作目录 = nop-app-erp）→ **EXIT=1（零命中）**。覆盖范围：nop-app-erp 工作树内全部 Java + XML——无性能测试类、无 JMH 基准、无 JMH 插件配置、无 Gatling 脚本。
- 引用源：roadmap line 697 + line 787（Q5 维度说明：零 `*PerfTest*`/`*Benchmark*`/JMH/Gatling）；Q0 README §范围矩阵 §Q5（核验日期 2026-08-01）；Q0 plan §Current Baseline NOT FOUND 证据第 5 条。

### 1.2 roadmap 引用的 `db-index-design.md` 为陈旧引用（stale reference 标注）

roadmap line 787（Q5 维度说明）原文：「性能测试被显式 defer 到"首次生产数据规模"（`db-index-design.md`、`posting.md` 等），但无任何基础设施」。经实仓复核，`db-index-design.md` **在实仓与 `docs/archive/` 下均未找到**（NOT FOUND）——这是 roadmap 沿用的陈旧引用。

- 核验命令（2026-08-01 复核）：
  - `rg -l "db-index-design"` → 命中 9 个**引用/同名文件**，但**无设计 owner doc 本体**：含 roadmap line 787 + Q0 README §Q5 + 本文档（**引用**）、`docs/architecture/idempotency-pattern.md` + `docs/logs/2026/07-06.md` + `scripts/add-orm-indexes.js` + `docs/plans/2026-07-01-1900-1-platform-compliance-remediation.md`（**旁证引用**）、以及**同名 plan** `docs/plans/2026-07-05-2352-1-db-index-design.md`（Plan Status: completed——索引设计**工作已做且落地为 orm.xml 索引**，但产出是 plan 而非 owner doc）。
  - `ls docs/design/*/db-index-design.md docs/design/db-index-design.md docs/architecture/*/db-index-design.md` → NOT FOUND（无设计/架构 owner doc 本体）。
  - `ls docs/archive/**/db-index-design.md` → NOT FOUND。
- **裁决（stale reference 处置）**：本设计文档不沿用 `db-index-design.md`。索引设计工作已完成（同名 plan completed，索引落地为各域 `<domain>/model/*.orm.xml` 中的 `<index>`，如 `IDX_FIN_VOUCHER_BILL_R_BILL_CODE_BIZ_TYPE`），但无独立 owner doc。真相源改为实存 owner doc：
  - 过账回链热查询索引：`docs/design/finance/posting-log.md` §ErpFinVoucherBillR 索引与过账性能（`(billCode, businessType)` 前缀索引，实仓 `IDX_FIN_VOUCHER_BILL_R_BILL_CODE_BIZ_TYPE`，R3.6 已加）。
  - 持久化模型字段/索引权威源：各域 `<domain>/model/*.orm.xml`（如 `module-finance/model/app-erp-finance.orm.xml`）。
- **回填提示（非本计划动作）**：roadmap line 787 的 `db-index-design.md` 引用属 stale（指向不存在的 owner doc），但本计划是纯文档 Phase 1，不改 roadmap（roadmap 工作项表结构由 Q0 Non-Goal 约束不动）。本文档以实存 owner doc 替代该 stale 引用，未来若 roadmap 维护者清理该引用，以本文档 §1.2 裁决为准。

### 1.3 Q6 硬依赖已满足（被测路径数据确定性可复现）

Q5 的测量确定性（被测路径的**日期/期间数据**不随墙钟漂移）硬依赖 Q6 时钟硬化。Q6 已 done，硬依赖解除：

- Q6 plan `docs/plans/2026-08-01-1357-1-mq-q6-clock-test-infrastructure-impl.md`：`Plan Status: completed`。
- Q6 落地产物（2026-08-01 复核实仓存在）：
  - `module-common-test/src/main/java/app/erp/common/test/ThreadLocalFrozenClock.java`（thread-local delegating clock，per-fork 静态初始化挂载，消除全局静态并行污染）—— 核验：`rg -l "ThreadLocalFrozenClock" --glob '*.java'` 命中。
  - `AbstractFrozenClockExtension` 改写为委托 `ThreadLocalFrozenClock`（beforeAll/afterAll 不再全局替换 `CoreMetrics.s_clock`）。
  - 16 域子类迁移完成（15 + assets 补建 `AstFrozenClockExtension`，见 Q6 plan closure §验收；核验：`rg -l "extends AbstractFrozenClockExtension" --glob '*.java'` 命中 16 文件）。
  - 跨月模拟回归 `TestClockRolloverFinance`（`module-finance/erp-fin-service/src/test/.../TestClockRolloverFinance.java`）。
  - nightly CI job `.github/workflows/clock-rollover.yml`。
- **对 Q5 的意义（数据确定性 vs 计时确定性，须辨析）**：`ThreadLocalFrozenClock` **仅冻结日期**（`currentDate()` / `currentDateTime()` 返回冻结 `referenceDate`）；`currentTimeMillis()` / `nanoTime()` **仍委托真实系统时钟**（`CoreMetrics.defaultClock()`）。故 Q6 使**被测路径的数据/分支确定性**成立（同一冻结日期 + 同一 seed 期间 → 同一期间解析、同一模板匹配、同一分支），**而非冻结计时本身**。计时方差仍来自 GC/JIT/IO 噪声，须经 untimed warmup + 多轮测量 + 离群值裁剪收敛（§4.x + §3.4）。即便如此，Q6 解除的是"日期漂移致期间/seed 错配"这一**系统性**噪声源（原月初翻车税根因），是 Q5 可复现性的硬前提，现已满足。
- 引用源：roadmap line 824（依赖图 `Q6 --> Q5`）+ line 788（Q6 维度说明）+ Q0 README §实施顺序裁决（Q6 位 1，Q5 位 6）。

### 1.4 4 关键路径候选（owner doc 对齐）

性能基线须覆盖**真实关键路径**，而非合成计算。基于 roadmap line 787 关键路径建议 + 实存 owner doc 对齐，4 条候选路径全部有实存 owner doc 支撑其路径真实性：

| # | 关键路径 | owner doc 真相源 | 实仓入口 | roadmap 数据规模建议 |
|---|----------|------------------|----------|----------------------|
| 1 | **凭证过账**（批量凭证过账吞吐） | `docs/design/finance/posting.md`（三层模型 + SYNC 默认）+ `posting-log.md`（`ErpFinVoucherBillR` 索引热查询） | `IErpFinVoucherBiz.post(PostingEvent, ...)` → Provider.createFacts → 凭证分录 + `ErpFinVoucherBillR` 回链 | 1000 凭证（roadmap line 678 工作项表） |
| 2 | **期间结账**（大规模 GL 行结账） | `docs/design/finance/period-close.md`（前置检查 + AR/AP/INV/AST/GL 按序关账 + 损益结转 + 试算平衡） | `ErpFinAccountingPeriodBizModel` 结账链路 | 1 万行 GL（roadmap line 678 工作项表） |
| 3 | **库存核算 reclose**（成本层重算） | `docs/design/finance/costing-methods.md`（FIFO 成本层 + 期末成本兜底 reclose） | `IErpInvCostingBiz.reclosePeriodCosts(periodId, startDate, endDate)`（R6.9 已拆 `ErpInvCostingReclosePeriodCostsProcessor`） | （本文档 §4 裁决，roadmap 未给具体数） |
| 4 | **报表渲染** | nop-report 子系统（各域种子报表） | `IReportEngine.getHtmlRenderer(path)` / `getRenderer(path, renderType)`（`ErpFinReportBizModel` 等各域 ReportBizModel） | （本文档 §4 裁决） |

- 核验命令（2026-08-01 复核实仓入口存在）：
  - 过账：`rg -n "getHtmlRenderer|getRenderer" module-finance/erp-fin-service/src/main/java/app/erp/fin/service/report/ErpFinReportBizModel.java` → 命中。
  - reclose：`rg -l "reclosePeriodCosts" --glob '*.java'` → 命中 `IErpInvCostingBiz.java` + `ErpInvCostingBizModel.java` + `ErpInvCostingReclosePeriodCostsProcessor.java`。
  - 索引：`rg -n "IDX_FIN_VOUCHER_BILL_R_BILL_CODE_BIZ_TYPE" module-finance/model/app-erp-finance.orm.xml` → 命中（R3.6 已加 `(billCode, businessType)` 索引，过账幂等判定与红冲反查的热路径已优化）。
- **owner doc 对齐结论**：4 路径均为 ERP 真实业务链路，非合成 micro-benchmark；性能基线测的是这些链路的端到端回归成本。

### 1.5 CI 现状（5 个既有 job，无性能 job）

当前 `.github/workflows/` 有 5 个 CI job，均非性能测量：

- 核验命令（2026-08-01 复核）：`ls .github/workflows/` → `clock-rollover.yml` / `compliance.yml` / `e2e.yml` / `maven.yml` / `mutation.yml`（共 5）。
- 无任何性能/基准测量 job。Q5 Phase 2 须新增第 6 个 job（§6 裁决形态），须与既有 5 job 不冲突（命名隔离 + 调度隔离，详见 §6）。

### 1.6 关键风险（roadmap line 787 明示）

- **测量方法选型**（JMH 微基准 vs 简单 `@Test` timing）—— §3 裁决。
- **CI runner 硬件方差下的退化阈值设计**（绝对阈值 vs 相对退化比）—— §6 裁决。Q6 时钟硬化解决了"计时确定性"，但 CI runner 硬件方差（runner 升级/降级、邻居负载）仍须门控设计裁决，避免假阳性/假阴性。

**剩余差距**：无 Q5 设计 owner doc。关键路径选择 + 基线定义（数据规模 × 时间阈值）+ 测量方法 + CI 软门控设计均未裁决，须在本文档独立审查后定夺。

## 2. 目标与非目标

### 2.1 目标（Phase 1 = 本文档；Phase 2 实现见 §5）

1. **裁决测量方法**（§3）：JMH 微基准 vs 简单 `@Test` timing vs 混合，给出候选、考虑的替代、残留风险三要素——满足 plan authoring guide §规则 9（Decision 项记录理由）。这是 roadmap line 787 明示的关键决策。
2. **裁决 CI 软门控退化阈值形态**（§6）：绝对阈值 vs 相对退化比 vs nightly 趋势，给出候选、考虑的替代、残留风险——解决 roadmap line 787 明示的 CI runner 硬件方差风险。
3. **定义 4 关键路径基线**（§4）：每路径数据规模 × 时间阈值 + seed 数据生成策略，引用 owner doc 确认路径真实性。
4. **提供 Phase 2 实施契约**（§5）：基线测量 harness 落地位置 + 4 路径基线数据生成 + 首次基线落盘载体。
5. **设计 CI 门控**（§6）：nightly 测量 job + 基线对比 + 退化阈值 + 基线存储 + 与既有 5 CI job 不冲突。
6. **显式声明 Q6 依赖确认**（§7）：Q6 `ThreadLocalFrozenClock` 是 Q5 测量确定性的硬前提（已满足）。

### 2.2 非目标

- **不实现任何代码/ORM/CI 变更**——本文档仅产出设计。Phase 2 实现（harness + 4 路径基线数据 + CI 软门控）是独立后续 plan，须在本文档审查收敛后方可起草（MQ 文档先行工作流硬约束）。
- **不追求生产级压测**——无生产数据规模、无真实负载模拟、无并发压力模型；仅建立 CI 可复现的回归基线。生产级压测为独立 successor（见 §8）。
- **不修改 `nop-entropy` 源码**——性能测量在应用层（test scope），不动平台生产代码。
- **不重新推导 NOT FOUND 证据**——§1 引用 Q0 README + roadmap + Q6 plan，避免双真相源。
- **不优化既有代码性能**——性能基线是回归门控，不是性能优化任务。基线测量发现的热点是否优化是独立 successor 决策。
- **不编写 Q2/Q3/Q7 设计**——同批独立 sibling plan。
- **不动 roadmap `db-index-design.md` stale 引用**——纯文档 Phase 1 不改 roadmap；本文档 §1.2 已以实存 owner doc 替代。

## 3. 测量方法选型（Decision）

> Decision 项：记录候选 + 考虑的替代 + 残留风险三要素（plan authoring guide §规则 9）。本裁决对应 roadmap line 787 明示的"测量方法（JMH vs 简单 `@Test` timing）"关键决策。

### 3.1 候选路径

- **路径 A — JMH 微基准**：科学级微基准（JVM warmup 控制、Blackhole、@State、fork 隔离）。适合**纯计算密集**路径（如成本核算的纯内存层计算）。重——需独立 harness + JMH Maven 插件 + 单独 build profile + CI 集成复杂度。
- **路径 B — 简单 `@Test` timing（集成级）**：复用既有 `JunitAutoTestCase` + localDb（H2/本地库）+ Q6 `ThreadLocalFrozenClock`，在端到端测试方法内用 `System.nanoTime()`（或 `CoreMetrics.nanoTime()`）包裹被测链路测墙钟。轻——零新依赖，复用既有测试栈。适合**端到端集成路径**（涉及 DB 事务/ORM 缓存/过账编排）。
- **路径 C — 混合**：端到端路径用 `@Test` timing，纯计算路径用 JMH。

### 3.2 裁决

**裁决：路径 B（简单 `@Test` timing）为首选，路径 C（混合）作为显式 successor hook。**

理由：

1. **4 关键路径中 3 条是端到端集成路径，JMH 微基准无法干净隔离**：
   - 凭证过账、期间结账、报表渲染均涉及 DB 事务提交 + ORM 缓存 + 跨模块编排。这些路径的回归成本主要被 **I/O（DB）+ 事务提交 + ORM 序列化** 主导，而非 JVM 内计算。JMH 设计目标是 JVM 内计算热点微基准（控制 warmup/fork/Blackhole 消除 JIT/GC 噪声），对 I/O-bound DB 路径反而引入"测量的是 DB 而非代码"的解释困难——JMH 的 fork 隔离在每次 fork 重建 DB 连接/warmup，反而放大 DB 冷启动噪声。
   - 仅库存核算 reclose 偏计算（扫 FIFO 成本层 + 重算），但其本质仍是 DB-heavy（扫 `ErpInvCostLayer` + 刷 `ErpInvStockLedger`）。`@Test` timing 捕获的是真实端到端成本，这正是回归门控关心的语义。
2. **Q6 时钟硬化使被测路径数据确定性可复现**：Q6 `ThreadLocalFrozenClock`（done）冻结日期，使同一 seed 数据 + 同一冻结期间的多轮测量，**被测路径的期间/日期数据不随墙钟漂移**（同一期间解析、同一模板匹配、同一分支）——`@Test` timing 的可复现性硬前提（消除日期漂移致 seed 错配的系统性噪声）已满足。这是 roadmap line 787 "Q5 硬依赖 Q6" 的本质：Q6 解除阻塞前，简单 timing 因墙钟漂移（期间/日期数据依赖 `CoreMetrics.today()`）而不可复现；Q6 done 后，此障碍消除。**计时本身的 GC/JIT/IO 噪声仍须经 warmup + 多轮测量收敛（§4 复现性方法）**，Q6 不消除计时随机噪声，只消除日期系统性噪声。
3. **JMH CI 集成成本高、对回归门控边际收益低**：JMH 需独立 build profile + 单独 surefire/插件配置 + fork 隔离使 CI 耗时显著上升；而回归门控只需"退化检测"，不需纳秒级科学精度。`@Test` timing 经多轮测量 + 离群值裁剪即可达到退化检测所需精度。

### 3.3 考虑的替代

- **纯 JMH（路径 A）**：否决——3/4 路径是 DB-bound 集成路径，JMH 微基准对 DB I/O 噪声的控制反而不如端到端 timing 直观；且独立 harness + CI 集成成本高，对回归门控边际收益低。
- **全混合（路径 C 首期即落地）**：否决——首期 4 路径均无纯计算子路径需要 JMH 级精度；维护两套 harness（JMH + @Test）的成本首期不值。仅当 Phase 2 数据显示某路径在 `@Test` timing 下多轮方差仍超阈值（GC/JIT 噪声主导），才对该路径升级为 JMH（路径 C successor，见 §3.4）。

### 3.4 残留风险

- **`@Test` timing 的 GC/JIT 噪声（须辨析两类噪声源）**：
  - **系统性噪声（JIT warmup）**：首轮测量在解释器/C1 下执行，后续轮经 C2 编译后显著加速——这是**系统性首 invocation bias**，非随机噪声，单纯离群值裁剪未必能剔除。**缓解（硬约束）**：每路径须先跑 **K 轮 untimed warmup**（不计入测量，建议 K=2，使 JIT 稳定 + DB/ORM 缓存预热），**再**跑 N 轮 timed 测量。
  - **随机噪声（GC pause / IO 抖动）**：缓解：N 轮 timed 测量 + 离群值裁剪 + 取中位数；Q6 时钟冻结已消除日期漂移系统性噪声。
- **localDb（H2）与生产 DB（MySQL/PostgreSQL）差异**：基线测的是 H2 端到端成本，非生产真实成本。缓解：明确基线语义为"**回归门控基线**"（检测退化趋势），非"生产性能指标"（见 §2.2 非目标）；生产级压测是独立 successor（§8）。
- **路径升级触发条件未经验证**：路径 C successor 触发条件（"`@Test` timing 多轮方差超阈值"）的"阈值"在 Phase 2 首次基线后才能定标。缓解：Phase 2 首次基线须记录每路径多轮方差，若方差超 §4 定义的复现性阈值，该路径登记为路径 C 升级候选。

## 4. 关键路径选择与基线定义

> 4 关键路径（§1.4）各定义数据规模 × 时间阈值 + seed 数据生成策略 + 复现性阈值。时间阈值的**绝对数值**在 Phase 2 首次基线落盘时定标（本文档定义数据规模目标 + 复现性判据，不预设绝对毫秒数——避免在 Q6 时钟冻结尚未实测多轮方差前拍脑袋设阈值）。
>
> **复现性方法（统一约定，4 路径共用）**：每路径须先跑 **K=2 轮 untimed warmup**（JIT 稳定 + DB/ORM 缓存预热，不计入测量），再跑 **N=10 轮 timed 测量**（N≥10 保证方差估计有足够样本；不采用"5 轮去高低"——去高低后仅余 3 点，方差估计误差过大）。
>
> **复现性阈值定义（方差比，统一定义，消除 MAJOR：方差度量须精确）**：`方差比 = (max(timed) − min(timed)) / median(timed)`，即极差/中位数比。该度量直观、稳健（不受单点 outlier 主导），且无需假设正态分布。验收判据：凭证过账/报表渲染 方差比 < 15%；期间结账/reclose 方差比 < 20%。若某路径首基线方差比超阈值，登记为 §3 路径 C 升级（JMH）候选。
>
> **计时窗口纪律（统一约定，seed-gen 必须排除在计时外）**：所有路径的 seed 数据生成 / 夹具构造必须在 `nanoTime()` 计时窗口**之外**完成——计时窗口仅包裹被测业务链路本身（如路径 1 仅包裹过账循环，不包裹 1000 张单据的构造）。否则 seed-gen 成本会污染被测墙钟。

### 4.1 路径 1 — 凭证过账（批量凭证过账吞吐）

- **被测链路**：`IErpFinVoucherBiz.post(PostingEvent, ...)` per voucher（SYNC 默认，业务+库存+凭证同事务），批量 1000 张。
- **数据规模**：1000 凭证（roadmap line 678 工作项表）。每凭证 = 1 PostingEvent → AcctDocProvider.createFacts → 凭证分录（借/贷行）+ `ErpFinVoucherBillR` 回链插入。`ErpFinVoucherBillR` 已加 `(billCode, businessType)` 索引（R3.6 `IDX_FIN_VOUCHER_BILL_R_BILL_CODE_BIZ_TYPE`），过账幂等判定 `findBillLinks` 与红冲反查的热路径已优化。
- **seed 数据生成策略**：在 localDb 构造 1000 张已审核业务单据（如采购入库单 `ErpPurReceive`，approveStatus=APPROVED）——**此批量 seed 构造在计时窗口外完成**；随后计时窗口内仅循环触发过账（首张 + 末张 nanoTime 差 / 1000 = 单凭证均摊成本）。单据类型混合（建议采购入库/销售出库/付款凭证各占 1/3，覆盖不同 Provider 路由），具体混合比例在 Phase 2 plan 裁决。
- **owner doc 引用**：`posting.md` §总体架构（三层模型 + SYNC 默认）+ `posting-log.md` §ErpFinVoucherBillR 索引与过账性能。
- **复现性阈值**：方差比 < 15%（复现性方法见 §4 统一约定：K=2 untimed warmup + N=10 timed，方差比 = 极差/中位数）。Phase 2 首基线实测后可调。

### 4.2 路径 2 — 期间结账（大规模 GL 行结账）

- **被测链路**：`ErpFinAccountingPeriodBizModel` 结账链路（前置检查 → AR/AP/INV/AST/GL 按序关账 → 损益结转 → 试算平衡表快照）。
- **数据规模**：1 期含 1 万行 GL（`ErpFinGlEntry`，roadmap line 678 工作项表）。经足够凭证过账累积到 1 万 GL 行后，对 1 个期间执行结账。
- **seed 数据生成策略**：先经路径 1 的过账 harness 灌注足够凭证累积到 ≥1 万 GL 行（或直接批量插 GL 行 seed）——**批量 seed 构造在计时窗口外**；计时窗口内仅包裹结账链路。须确保期间所有单据 posted=true（结账前置检查不阻断）。每轮测量前须重置期间到可结账状态（反结账或重建 seed），否则后续轮因已 CLOSED 无法重测。
- **owner doc 引用**：`period-close.md` §期末结账步骤（多阶段关账 + 试算平衡）。
- **复现性阈值**：方差比 < 20%（结账多阶段，方差源多于单凭证过账，阈值放宽；复现性方法见 §4 统一约定）。

### 4.3 路径 3 — 库存核算 reclose（成本层重算）

- **被测链路**：`IErpInvCostingBiz.reclosePeriodCosts(periodId, startDate, endDate)`（R6.9 已拆 `ErpInvCostingReclosePeriodCostsProcessor`），扫描本期 DONE 的 FIFO 移动单，对成本层缺失/COGS 异常重算。
- **数据规模**：本期 N 条 FIFO 移动单跨 M 物料。建议 N = 5000（移动单），M = 50（物料），具体在 Phase 2 plan 裁决（roadmap 未给 reclose 具体数，本文档定义）。
- **seed 数据生成策略**：构造 M 个 FIFO 物料的 N 条入库/出库移动单（DONE 状态，含正常路径 + 少量成本层缺失/COGS 异常以触发 reclose 补算）——**批量 seed 构造在计时窗口外**；计时窗口内仅包裹 `reclosePeriodCosts` 调用。正常路径补算数为 0，须显式注入异常 seed 以测非零补算路径成本（否则仅测空扫描成本）。每轮测量前须重置成本层到 reclose 前状态（重建 seed 或回滚 reclose 写入）。
- **owner doc 引用**：`costing-methods.md` §实现注记（FIFO 成本层 + 期末成本兜底 reclose）。
- **复现性阈值**：方差比 < 20%（复现性方法见 §4 统一约定）。
- **注意**：本路径偏计算，是 §3 路径 C 升级（JMH）的最可能候选。Phase 2 首基线若本路径方差比 > 阈值，登记为 JMH 升级候选。

### 4.4 路径 4 — 报表渲染

- **被测链路**：`IReportEngine.getHtmlRenderer(path)` / `getRenderer(path, renderType)`，各域种子报表（如财务试算平衡表、总账、库存成本明细）渲染。
- **数据规模**：渲染 K 份种子报表（每域 1-2 份代表性报表），每份基于含 L 行数据的报表数据集。建议 K = 8（覆盖多域），L 按报表类型（试算平衡表 ~科目数；总账 ~凭证行数）。具体在 Phase 2 plan 裁决。
- **seed 数据生成策略**：先经路径 1/2 灌注数据集——**批量 seed 构造在计时窗口外**；计时窗口内仅包裹单报表 `getHtmlRenderer`/`getRenderer` 调用。每报表独立计时，避免聚合掩盖单报表异常。
- **owner doc 引用**：nop-report 子系统（各域 `Erp*ReportBizModel`）。
- **复现性阈值**：方差比 < 15%（复现性方法见 §4 统一约定）。

### 4.5 基线定义汇总

| 路径 | 数据规模 | 复现性阈值（方差比） | seed 策略（计时窗口外构造） |
|------|----------|----------------------|------------------------------|
| 1 凭证过账 | 1000 凭证 | 方差比 < 15% | 构造 1000 已审核业务单据，计时窗口仅过账循环 |
| 2 期间结账 | 1 万 GL 行/期 | 方差比 < 20% | 过账累积 ≥1 万 GL 行，每轮重置期间；计时窗口仅结账链路 |
| 3 库存核算 reclose | 5000 移动单/50 物料 | 方差比 < 20% | FIFO 物料移动单 + 注入异常 seed；每轮重置成本层；计时窗口仅 reclose 调用 |
| 4 报表渲染 | 8 份种子报表 | 方差比 < 15% | 灌注数据集后逐报表渲染，计时窗口仅 render 调用 |

> 所有路径共用复现性方法：K=2 untimed warmup + N=10 timed 测量，方差比 = (max−min)/median。时间阈值（绝对毫秒数）**不在本文档预设**——须在 Phase 2 Q6 冻结时钟下实测多轮后定标，作为首次基线落盘。本文档定义的是数据规模目标 + 复现性判据（回归门控的"可复现"语义），而非拍脑袋的绝对数。

## 5. 实施步骤（Phase 2 契约）

> 本节是 Phase 2 实现 plan 的实施契约。Phase 2 须在本文档审查收敛后方可起草。

### 5.1 基线测量 harness 落地位置（裁决）

**裁决：复用 `module-common-test` 基类 + 各域 test 源码下的 perf 测试类**（不新建独立 perf module）。

- harness 基类：`module-common-test`（复用 `JunitAutoTestCase` + `ThreadLocalFrozenClock` + localDb 测试栈），新增 perf 计时辅助（如 `PerfTiming` 工具类：K warmup + N timed + 方差比计算 + 中位数）。
- 各域 perf 测试类：在被测域 `erp-<short>-service/src/test/` 下新建 `Test*Perf.java`（如 `TestErpFinVoucherPostingPerf`、`TestErpFinPeriodClosePerf`、`TestErpInvCostingReclosePerf`、`TestErpFinReportRenderPerf`），复用各域 frozen clock extension。
- **考虑的替代**：独立 `module-perf-test` 模块。否决——首期 4 路径分布在 finance/inventory 两域，独立模块的 build profile + 依赖接线成本高于复用既有 test 源码；且 perf 测试须与被测域同 classpath（访问 BizModel + seed 夹具），独立模块反而引入可见性问题。若 Phase 2 后 perf 测试规模膨胀（>10 域），再升级为独立模块（successor）。
- **`mvn test` 隔离（机制已证可行，非从零设计）**：perf 测试类默认**不进 per-commit `mvn test`**（避免拖慢既有 ~1900 测试的全量构建）。机制经核验有先例：既有 `@Tag("full-app")` + 各 `erp-*-web/pom.xml` 内 `<excludedGroups>full-app</excludedGroups>`（核验：`rg -n "excludedGroups" --glob 'pom.xml'` 命中各 web pom）已实现 per-commit 排除 full-app 测试。perf 测试沿用同一模式：`@Tag("perf")` 标注 + 在被测域 `erp-*-service/pom.xml`（默认 surefire 配置）加 `<excludedGroups>perf</excludedGroups>`，nightly CI job 用 `-Dgroups=perf`（或 `-DexcludedGroups=` 覆盖）激活。父 `nop-entropy/pom.xml` surefire 无全局 `<groups>`/`<excludedGroups>`，隔离配置在**各模块 pom**（per-module/profile），非继承——与 full-app 先例一致。

### 5.2 4 关键路径基线数据生成

- **批量 seed 生成器是 Phase 2 主要工作量（非"复用既有夹具"）**：经核验，既有 finance/inventory 测试夹具均为**单记录**（一个 `ErpFinVoucherTemplate`、一张 `ErpFinVoucher`、一行 GL），**无** 1000/1 万/5000 规模的批量生成器（核验：`rg "for\s*\(.{0,4}(<|<=)\s*(1000|5000|10000)" --glob '*Test*.java'` 零命中）。故 4 路径均须**从零新建批量 seed 生成器**，是 Phase 2 的主要实现工作量，非可省略的复用。
- **复用边界（仅夹具机制，非数据规模）**：可复用各域 frozen clock extension、`JunitAutoTestCase` localDb 初始化、单记录夹具的**构造模式**（如建 voucher template 的样板），但**数据规模必须新建批量循环**。
- **计时窗口纪律（硬约束，重申 §4 统一约定）**：批量 seed 生成必须在 `nanoTime()` 计时窗口**之外**完成，计时窗口仅包裹被测业务链路。否则 seed-gen 成本污染被测墙钟。
- seed 数据生成须在 Q6 `ThreadLocalFrozenClock` 冻结日期下进行，确保期间/日期数据确定性（§7）。

### 5.3 首次基线落盘（基线数据载体裁决）

**裁决：独立 JSON 文件提交到仓库 `docs/architecture/quality-engineering/perf-baselines/`。**

- 载体：`perf-baselines/baseline-{date}.json`（如 `baseline-2026-XX-XX.json`），结构含每路径的 `{dataScale, rounds:[t1..tN], median, p95, varianceRatio, clockRef, seedProfile}`。
- 复用既有基线文件（如 `docs/testing/known-good-baselines.md` 模式）：否决——known-good-baselines 是构建基线（模块数/测试数），语义与性能基线（耗时/方差）不同，混用模糊语义。独立 JSON 文件便于 §6 nightly job 对比（机器可读）。
- **LATEST 指针**：`perf-baselines/LATEST.json` 软指向最新基线（或 CI job 读最新日期文件），供退化对比。

### 5.4 Phase 2 实施顺序建议

1. harness 基类（`PerfTiming` + surefire/profile 隔离）。
2. 路径 1（凭证过账）首基线 —— 最简单、最直接验证 Q6 时钟冻结效果。
3. 路径 4（报表渲染）—— 独立性强，与路径 1 seed 部分复用。
4. 路径 3（库存核算 reclose）—— 偏计算，验证 §3 路径 C 升级触发条件。
5. 路径 2（期间结账）—— 最复杂（多阶段），依赖路径 1 seed 累积。
6. nightly CI job 接线（§6）。

## 6. CI 软门控设计（Decision）

> Decision 项：记录候选 + 考虑的替代 + 残留风险三要素。本裁决对应 roadmap line 787 明示的"CI 软门控设计（退化阈值 + nightly vs per-commit）"关键决策 + CI runner 硬件方差风险。

### 6.1 候选路径

- **路径 A — 绝对阈值**：基线 × N（如基线 × 1.5）。简单。但 CI runner 升级/降级/邻居负载变化致假阳性/假阴性——硬件敏感。
- **路径 B — 相对退化比**：与上次基线对比，退化 > X% 告警。容忍 runner 方差，但需历史基线存储（首基线冷启动问题）。
- **路径 C — nightly 测量 + 趋势记录（非阻塞）**：nightly 跑 perf 测量、记录趋势、非阻塞告警；积累数据后转阻塞门控。

### 6.2 裁决

**裁决：路径 C 为首期形态（nightly 非阻塞测量 + 趋势记录），并定义明确晋升条件至路径 B（相对退化比阻塞门控）。**

裁决的演进式门控：

1. **首期（Phase 2 落地后）**：nightly CI job（§6.3）跑 4 路径 perf 测量，记录每路径 `{median, varianceRatio}` 到 `perf-baselines/nightly-{date}.json`，**非阻塞**（仅趋势记录 + PR 评论摘要，不阻断合并）。
2. **晋升条件**：积累 ≥ N 个 nightly 测量（建议 N = 14，约 2 周）后，计算每路径的方差包络（中位数 ± 容差）。若 nightly 方差稳定（路径方差比 < §4 复现性阈值），晋升为路径 B：
   - 退化 > X%（建议 X = 20%，相对最近 N=14 nightly 中位数）→ nightly job 标记 `perf-regression` 并开 issue（仍非阻塞合并，但显式登记）。
   - 积累更长历史（如 N = 30）后，可选晋升为**阻塞门控**（PR 触发 perf 检测 + 退化 > X% 阻断合并）——此为最终态，须团队明确同意（避免误阻断）。

理由：

1. **路径 B 冷启动问题**：Phase 2 首基线无历史，相对退化比无处对比。路径 C 先积累 nightly 趋势，解决冷启动。
2. **路径 A 硬件敏感**：CI runner 升级（CPU 代际提升）会使绝对阈值过松（假阴性）；降级或邻居负载升高会过紧（假阳性）。相对退化比（路径 B）+ nightly 固定 runner 槽位，容忍 runner 代际方差。
3. **nightly 而非 per-commit**：156 模块构建 + 4 路径 perf 测量（每路径 5 轮）耗时显著（预估单次 >10 分钟），per-commit 跑会拖慢 PR 反馈 + 与既有 5 CI job 争抢 runner。nightly 隔离在低峰时段，且 perf 回归通常不急于单次提交（趋势性退化才是门控目标）。

### 6.3 nightly CI job 设计

- **新 job**：`.github/workflows/perf-baseline.yml`（第 6 个 CI job）。
- **触发**：`schedule: cron`（nightly，如 `0 2 * * *`），加 `workflow_dispatch`（手动触发首基线）。
- **执行**：`mvn test -Pperf`（或 `-Dgroups=perf`，激活 §5.1 surefire/profile 隔离的 perf 测试），跑 4 路径 × 5 轮，生成 `perf-baselines/nightly-{date}.json`。
- **对比**：读 `perf-baselines/LATEST.json`（或最近 nightly），计算每路径相对退化比，超 §6.2 X% 则开 issue（GHA `actions/github-script` 或 `gh issue create`）。
- **基线更新**：首基线或基线主动刷新时，commit 新 `baseline-{date}.json` + 更新 `LATEST` 指针（基线刷新须显式人工/计划触发，非 nightly 自动覆盖——避免把退化悄悄写进基线）。

### 6.4 与既有 5 CI job 不冲突

| 既有 job | 触发 | 冲突分析 |
|----------|------|----------|
| `maven.yml` | per-commit | perf 测试经 profile 隔离不进 per-commit `mvn test`，零冲突 |
| `compliance.yml` | per-commit/nightly | 跑 compliance checker，与 perf 测量独立，零冲突 |
| `e2e.yml` | per-commit | 浏览器 E2E，独立 runner，零冲突 |
| `mutation.yml` | （pitest）| 变异测试，独立 build profile，零冲突 |
| `clock-rollover.yml` | nightly | nightly 但测的是时钟跨月回归，独立 job 名 + 独立测试集，零冲突 |

- **命名隔离**：`perf-baseline.yml` job 名独立，不与既有 job 重名。
- **调度隔离**：nightly 独立 cron 槽位（如 02:00，与 clock-rollover 错开），避免同一 runner 并发争抢。
- **runner 假设**：nightly 假设 GitHub-hosted runner（或自建固定 runner）。退化比是相对的，runner 代际方差经相对对比容忍；若 runner 池异构（部分 fast 部分 slow），nightly 单次可能落在不同 runner 致方差放大——晋升阻塞门控前须确认 nightly runner 同构（或接受更大 X% 容差）。此为 §6.5 残留风险。

### 6.5 残留风险

- **nightly 延迟发现**：白天引入的性能退化要到次日 nightly 才被发现。缓解：性能退化通常是趋势性（多次提交累积）而非单次突变；nightly 趋势能捕获。若需 per-commit 快速 smoke，可在 §6.2 最终态加 per-commit 单路径快速 smoke（successor，非首期）。
- **基线漂移**：若 nightly 自动覆盖 LATEST 基线，退化会被悄悄写进基线。缓解：基线更新须显式触发（§6.3），nightly 仅记录 + 对比，不自动覆盖基线。
- **runner 异构方差**：GitHub-hosted runner 池可能异构。缓解：相对退化比容忍代际方差；晋升阻塞门控前确认 nightly runner 同构或调大 X%。
- **H2 vs 生产 DB 差异**：基线测 H2，生产 DB 性能可能不同方向退化（如 H2 没暴露的索引问题）。缓解：基线语义明确为"回归门控"非"生产指标"；生产 DB 性能问题由 Q7 可观测性（生产 metrics）+ 生产级压测 successor 覆盖。

## 7. 与 Q6 的依赖确认

**显式声明**：Q6 `ThreadLocalFrozenClock` 是 Q5 被测路径**数据确定性**的**硬前提**（roadmap line 824 依赖图 `Q6 --> Q5` + line 788 + Q0 README §实施顺序裁决位序）。

- **依赖语义（数据确定性 vs 计时确定性，须辨析）**：性能基线的**期间/日期数据**若随墙钟漂移（`CoreMetrics.today()` 返回真实日期，seed 期间数据与运行日期错配），基线不可复现。Q6 `ThreadLocalFrozenClock` **冻结日期**（`currentDate`/`currentDateTime` 返回冻结值；`currentTimeMillis`/`nanoTime` 仍委托真实系统时钟），使同一 seed 数据 + 同一冻结期间的多轮测量，**被测路径的期间/日期数据确定性成立**（同一期间解析、同一模板匹配、同一分支）——这是消除"日期漂移致 seed 错配"**系统性噪声**的硬前提。**计时本身的 GC/JIT/IO 随机噪声不因 Q6 消除**，须经 K warmup + N timed + 方差比收敛（§4 统一约定）。
- **依赖状态：已满足**（2026-08-01 复核）：
  - Q6 plan `docs/plans/2026-08-01-1357-1-mq-q6-clock-test-infrastructure-impl.md` `Plan Status: completed`。
  - `ThreadLocalFrozenClock.java` 实仓存在（`module-common-test/src/main/java/app/erp/common/test/`），核验其仅冻结日期、时间仍走系统时钟。
  - 16 域子类迁移（15 + assets 补建 `AstFrozenClockExtension`）+ `TestClockRolloverFinance` 跨月模拟 + `clock-rollover.yml` nightly 均落地。
- **对 Phase 2 的约束**：Phase 2 perf 测试类**必须**在各域 frozen clock extension 下运行（继承既有 `*FrozenClockExtension`），不得直连真实墙钟。否则日期漂移系统性噪声回潮，基线不可复现，违反 Q5 验收判据（§8）。

## 8. 验收判据（Phase 2 验收依据）

> 本节定义 Phase 2 实现 plan 的验收依据。Phase 1（本文档）验收 = 文档审查收敛（§Review Record）。

1. **4 关键路径均有可复现基线**：每路径在 Q6 冻结时钟下，经 K=2 untimed warmup + N=10 timed 测量，**方差比 = (max−min)/median < §4 复现性阈值**（凭证过账/报表渲染 < 15%；期间结账/reclose < 20%）。方差比超阈值的路径登记为 §3 路径 C 升级候选。
2. **CI 软门控在退化 > 阈值时告警**：nightly job 对比 LATEST 基线，退化 > §6.2 X% 时开 issue（或晋升后阻断）。
3. **基线数据落盘可追溯**：`perf-baselines/baseline-{date}.json` 提交到仓库，含每路径 `{dataScale, warmupRounds, timedRounds:[t1..tN], median, varianceRatio, clockRef, seedProfile}`，可被 nightly job 机器读取对比。
4. **Q6 数据确定性支撑可复现**：所有 perf 测试类在各域 frozen clock extension 下运行，被测路径期间/日期数据不随墙钟漂移（§7 约束）；计时随机噪声经 warmup + 多轮收敛。
5. **与既有 5 CI job 零冲突**：perf 测试经 `@Tag("perf")` + 各域 pom `<excludedGroups>` 隔离不进 per-commit `mvn test`；nightly job 独立命名 + 调度错开（§6.4）。
6. **路径 C 升级触发判定**：若路径 3（reclose）或任一路径在 `@Test` timing 下方差比超 §4 阈值，登记为 JMH 升级候选（§3.4 successor hook）。

## 9. Deferred But Adjudicated（successor）

- **Q5 Phase 2 实现**（基线测量 harness + 4 路径基线数据 + CI 软门控）：触发条件 = 本设计文档审查收敛（§Review Record 两轮无 BLOCKER/MAJOR）+ §3 测量方法 + §6 CI 门控 Decision 落定。届时 DRAFT_PLANS 起草 Phase 2 实现 plan（加载 `nop-testing` skill），plan 引用本文档作为范围与验收依据。
- **生产级压测**：触发条件 = 首次生产部署 + 真实负载数据可用。本文档 §2.2 明确非目标（仅 CI 可复现回归基线）。
- **路径 C 升级（JMH）**：触发条件 = Phase 2 首基线显示某路径 `@Test` timing 方差超 §4 阈值（GC/JIT 噪声主导）。仅升级该路径，非全路径。
- **独立 perf module**：触发条件 = perf 测试规模膨胀（>10 域 / >20 测试类），`module-common-test` + 各域 test 源码的复用模式可见性下降。
- **per-commit 阻塞门控**：触发条件 = nightly 累积 ≥30 测量 + runner 同构确认 + 团队同意（避免误阻断）。
- **roadmap `db-index-design.md` stale 引用清理**：触发条件 = roadmap 维护者清理工作项表引用。本文档 §1.2 已以实存 owner doc 替代，非本计划动作。

## Review Record

> 本节持久化独立子代理审查记录（含审查者 task id + 轮次 + 结论 + 修改摘要）。审查由独立子代理（新会话，fresh cold context）执行，审查者不可与作者为同一会话。两轮审查由不同子代理会话执行（不同 task id）。

### 第 1 轮 — 规范合规审查（independent-reviewer-r1 / `ses_041fd4fb8ffem79qJcmYMrtNPS`）

- 结论：**accept-after-revision**（0 BLOCKER / 4 MAJOR / 2 MINOR，全部为可复现证据引用精度问题，非设计结论缺陷）。
- 核验全 PASS：6 节结构完整 / 两个 Decision 三要素齐备 / §1 引用非重推导（无双真相源）/ 4 关键路径 owner doc 全部实存且节标题准确 / stale `db-index-design.md` 已显式标注 + 替换 / Q6 硬依赖真实（plan completed + ThreadLocalFrozenClock 实仓）/ 零 perf 基础设施命令零命中复现 / 5 CI job 数量准确。
- MAJOR 修订（已全部应用）：
  - M1 §1.2：`rg -l "db-index-design"` 结果重述——实命中 9 个引用/同名文件（含同名 plan `2026-07-05-2352-1-db-index-design.md` Status: completed，索引工作已落地为 orm.xml 索引），但无设计/架构 owner doc 本体。
  - M2 §1.3 + §7：Q6 维度说明行号 789 → 788（789 实为 Q7）。
  - M3 §1.4 表 + §4.1 + §4.2：数据规模引用 787 → 678（678 是工作项表含"1000 凭证/1 万 GL 行"；787 维度说明仅列路径名无数值）。
  - M4 §1.3 + §7：15 域子类 → 16（15 + assets 补建 `AstFrozenClockExtension`，对齐 Q6 plan closure）。
- MINOR：m1 "1903 测试"改为"~1900"（illustrative）；m2 §1.2 补充同名 plan completed 上下文（索引设计已做）。

### 第 2 轮 — 覆盖面与可执行性审查（independent-reviewer-r2 / `ses_041fd1f76ffeTQSaqQhy82LHrS`）

- 结论：**accept-after-revision**（0 BLOCKER / 3 MAJOR / 4 MINOR，全部经文档编辑修复，结构可执行性 PASS）。
- 可执行性核验全 PASS：§3 path-B 决策对 DB-bound 路径成立 / §4 数据规模适中（H2 in-process 不爆内存）/ §5 harness 位置可行（JunitAutoTestCase + frozen clock extension 实存）/ §6 path-C-first 解决冷启动 + N=14 合理 + 5 job 无冲突（cron 02:00 vs clock-rollover 03:00 错开）/ §7 ThreadLocalFrozenClock 支撑数据可复现 / `@Tag("full-app")` + `<excludedGroups>` 先例证 `@Tag("perf")` 可行 / 父 surefire `threadCount=1` 使 thread-local clock 安全。
- MAJOR 修订（已全部应用）：
  - MAJOR-1 §4 + §8：复现性度量"方差"未定义 + 5 轮去高低仅余 3 点统计量过薄。修复：统一定义**方差比 = (max−min)/median**（直观稳健、无正态假设）；测量轮数 N=5 → **N=10**；§4 加统一约定块（K=2 untimed warmup + N=10 timed + 方差比定义 + 计时窗口纪律）；§4.5 表 + §8 criterion 1/3 同步更新。
  - MAJOR-2 §3.4 + §4.x：JIT warmup 是系统性首 invocation bias 非随机噪声，单纯离群值裁剪未必剔除。修复：§3.4 辨析系统性（JIT warmup，K untimed warmup 缓解）vs 随机（GC/IO，多轮+中位数缓解）两类噪声源；§4 统一约定 + 各路径显式 K=2 warmup。
  - MAJOR-3 §5.2：既有夹具均为单记录，无批量生成器，"优先复用"为误述。修复：§5.2 重述为"批量 seed 生成器是 Phase 2 主要工作量，须从零新建"，仅复用夹具机制非数据规模；计时窗口纪律（seed-gen 排除在计时外）在 §4 统一约定 + §5.2 + 各路径 §4.x 重申。
- MINOR：MINOR-1（15→16，同 R1-M4）；MINOR-2（§3.2 reason#2 + §7 标题"timing 确定性可复现"overstate——ThreadLocalFrozenClock 仅冻结日期非 nanoTime；重述为"被测路径数据确定性可复现"，计时随机噪声须 warmup+多轮收敛）；MINOR-3（§5.1 `@Tag("perf")` 机制具体化为 per-module pom `<excludedGroups>`，引用 full-app 先例）；MINOR-4（"1903 测试/156 模块"改 illustrative/~）。

### 收敛结论

两轮独立子代理审查（不同 task id，fresh cold context）均 **0 BLOCKER / 0 残留 MAJOR**。所有 MAJOR/MINOR 已修订应用。文档结构完整、双 Decision 三要素齐备、无双真相源、owner doc 引用实存且行号准确、测量方法与 CI 门控 Decision 可执行、Q6 数据确定性依赖真实满足。**Phase 1 设计文档审查收敛**，可转 Phase 2 实现 plan 起草。

