# 时钟测试基础设施硬化（MQ Q6）—— Phase 1 设计文档

> Owner Doc for Milestone MQ Q6（时钟测试基础设施硬化）
> 创建日期：2026-08-01
> Plan：`docs/plans/2026-08-01-1158-1-mq-q6-clock-test-infrastructure-design-doc.md`
> 单一真相源依赖：本文档是 MQ 文档先行工作流 **Phase 1** 产物（设计/策略文档），**不实现任何代码/ORM/CI 变更**。Phase 2 实现 plan 须在本文档审查收敛后方可起草。
> 上游真相源（**只引用**，不重推导，避免双真相源漂移）：
> - `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q6（line 679 工作项表 + line 789 维度说明）+ §横切关注点 §文档先行工作流（line 843-862）
> - `docs/architecture/quality-engineering/README.md`（Q0 范围矩阵 + 复杂度分级 + 实施顺序裁决基线，Q6 位 1）
> - `docs/plans/2026-08-01-1121-1-mq-q0-quality-gap-analysis-readme.md` §Current Baseline（7 维度 NOT FOUND 实仓证据已核验）
> - bug `docs/bugs/2026-08-01-finance-period-resolution-month-rollover.md`（月初翻车税根因证据 + 诊断陷阱）
> - plan `docs/plans/2026-08-01-0803-1-r6-9-mr6-completion-gaps-and-finance-test-date-hardening.md`（R6.9 单点日期硬化边界——Q6 是其系统性根治超集）

## 1. 现状评估

> 本节**引用**（非重推导）上游真相源已核验事实，每条标注可复现核验命令 + 核验日期，便于 Phase 2 plan 与独立审查复核。证据核验日期：2026-08-01（HEAD 含 R6.9 收口）。

### 1.1 生产代码时钟入口（过账链路已合规；存在少量 `LocalDate.now()` 残留，本期范围外）

ERP 过账链路生产代码已统一通过 nop-entropy 平台 API 获取「今天」，而非直接 `LocalDate.now()` / `System.currentTimeMillis()`。时钟入口为平台 API：

- `io.nop.api.core.time.CoreMetrics`（`../nop-entropy/nop-kernel/nop-api-core/src/main/java/io/nop/api/core/time/CoreMetrics.java`）：
  - `CoreMetrics.today()` / `CoreMetrics.currentDate()`（line 60-66）委托静态 `s_clock`
  - `CoreMetrics.currentDateTime()`（line 68-70）委托静态 `s_clock`
  - `CoreMetrics.currentTimeMillis()` / `CoreMetrics.nanoTime()`（line 52-53, 72-74）委托静态 `s_clock`
  - `CoreMetrics.registerClock(IClock)`（line 44-46）**替换全局静态 `s_clock`**（`s_clock = clock`）
  - `CoreMetrics.defaultClock()`（line 48-50）返回**常量** `DEFAULT_CLOCK`（line 21-40，包装 `System.currentTimeMillis()` / `LocalDate.now()`），**非**当前 `s_clock`
  - `static IClock s_clock = DEFAULT_CLOCK`（line 42）—— **进程级全局静态单槽**；**`private`，无 getter**（仅 `registerClock` setter + `defaultClock()` 返回常量 + 各 read 方法）——应用代码无法内省当前 `s_clock` 类型（此约束影响 §3.3 / §4.1 的实现机制选型）
- 合规生产用法示例（finance 过账链路）：
  - `module-finance/erp-fin-service/.../posting/NotesPostingDispatcher.java:79,108` — `voucherDate = note.getIssueDate() != null ? issueDate : CoreMetrics.today()`
  - `module-finance/erp-fin-service/.../posting/ErpFinPostingProcessor.java:495` — `resolveOpenPeriod(voucherDate, orgId, ...)` 要求一个包含该日期的 OPEN 期间
- 核验命令（2026-08-01 复核）：
  - `rg -n "CoreMetrics\.today\(\)|CoreMetrics\.currentDate\(\)" module-finance/erp-fin-service/src/main/java` → 命中合规生产用法
  - `rg -n "class CoreMetrics" ../nop-entropy/nop-kernel/nop-api-core/src/main/java/io/nop/api/core/time/CoreMetrics.java` → 平台时钟入口

**残留 `LocalDate.now()` 直调（范围外，登记 successor）**：本期 Q6 聚焦**测试侧时钟基础设施**；生产侧仍有少量 `LocalDate.now()` 直调（非过账链路），属同族日期敏感问题但不在 Q6 范围：

- `module-finance/erp-fin-service/.../processor/ErpFinVoucherTemplateRenderTemplateProcessor.java:75` — `LocalDate today = LocalDate.now();` 用于按有效期过滤凭证模板（日期敏感，潜在月初税同族）
- `module-master-data/erp-md-service/.../processor/ErpMdCurrencyRefreshRatesFromApiProcessor.java` — 同 `LocalDate.now()` 模式
- 核验命令（2026-08-01 复核）：`rg -rn "LocalDate\.now\(\)" --glob '*.java' module-*/erp-*-service/src/main/java` → 命中上述 2 处

> 范围裁决：上述 2 处生产 `LocalDate.now()` 残留登记为 watch-only successor（见 §7），不在 Q6 Phase 2 范围。理由：Q6 的月初翻车税根因在**测试侧未冻结时钟**（§1.4）+ 全局静态并行不安全（§1.2），非生产 `LocalDate.now()`；且改生产代码须独立保护区域评估（会计期间/模板有效期）。但 Phase 2 须在 §4.1 step 4 的 4 模块审计中复核这 2 处是否已被现有测试覆盖（避免遗漏）。

> **闭合回填（2026-08-02，plan `2026-08-02-0650-1`）**：上述 2 处生产 `LocalDate.now()` 残留**已消解**。本期 successor plan 将两处直调替换为 `CoreMetrics.today()`（生产运行时行为零变更——非冻结时 `CoreMetrics.today()` 委托 `DEFAULT_CLOCK` == `LocalDate.now()`；唯一效果是使这两条路径在测试中可被 `ThreadLocalFrozenClock` 冻结为确定性日期），并补 2 个聚焦日期边界测试（`TestErpFinVoucherTemplateActiveByDate` / `TestErpMdCurrencyRefreshRatesDate`）覆盖此前零直接测试的日期敏感分支。闭合核验：`rg -rn "LocalDate\.now\(\)" --glob '*.java' module-finance/erp-fin-service/src/main/java module-master-data/erp-md-service/src/main/java` 返回 0 命中。§7 successor 对应行已翻转为「已闭合」。

### 1.2 测试侧冻结机制存在，但全局静态并行不安全

冻结机制基类已存在，15 域子类复用：

- `module-common-test/src/main/java/app/erp/common/test/AbstractFrozenClockExtension.java`：
  - 实现 `BeforeAllCallback, AfterAllCallback`（**类级**生命周期，非每测试方法）
  - `beforeAll`（line 62-63）：`frozenClock = new ICClock(referenceDate); CoreMetrics.registerClock(frozenClock);` —— 调用全局静态 `registerClock`
  - `afterAll`（line 67-68）：`CoreMetrics.registerClock(CoreMetrics.defaultClock());` —— 恢复系统时钟
  - 内部 `ICClock`（line 31-58）**仅冻结日期**（`currentDate()` / `currentDateTime()` 返回 `referenceDate`），保留 `currentTimeMillis()` / `nanoTime()` 走真实系统时钟——使 `ContextProvider` 等依赖时间单调推进的设施不受影响
- **15 域子类**（每域 `*FrozenClockExtension extends AbstractFrozenClockExtension`，仅声明 `REFERENCE_DATE`）：`FinFrozenClockExtension` / `InvFrozenClockExtension` / `PurFrozenClockExtension` / `SalFrozenClockExtension` / `MfgFrozenClockExtension` / `HrFrozenClockExtension` / `QaFrozenClockExtension` / `MntFrozenClockExtension` / `PrjFrozenClockExtension` / `CrmFrozenClockExtension` / `CsFrozenClockExtension` / `LogFrozenClockExtension` / `B2bFrozenClockExtension` / `CtFrozenClockExtension` / `DrpFrozenClockExtension`（共 15，核验：`rg -l "extends AbstractFrozenClockExtension" --glob '*.java'` 命中 15 文件）
- 19 模块中**未建** frozen-clock 子类的 4 个：`master-data` / `assets` / `aps` / `notify`（其日期敏感测试若存在则未走此基类冻结机制，属路径 C 迁移时须一并审计的对象——见 §4.1 step 3 与 §5 验收 4）
- 核验命令（2026-08-01 复核）：
  - `rg -l "extends AbstractFrozenClockExtension" --glob '*.java'` → 命中 15 个子类文件
  - `rg -n "registerClock" module-common-test/` → 命中 `AbstractFrozenClockExtension.java:63,68` 两行

**结构性缺陷（根因）**：所有 15 域子类经基类共用 `CoreMetrics.s_clock` **一个进程级全局静态时钟槽**。`registerClock` 是整体替换，非叠加/线程隔离。这导致：

1. **并行不安全（latent）**：`s_clock` 是进程级单例。任何同一 JVM 内并发的测试类（无论 JUnit `parallel=methods` 还是多线程 `@RegisterExtension`）只要其中一个调用 `registerClock`，另一个读 `CoreMetrics.today()` 即被污染。当前 surefire 配置（见 §1.3）恰好串行化了 fork 内执行，使污染**未显性爆发**，但设计本身依赖该外部配置不变——是结构性技术债，非已修复。
2. **状态泄漏向量**：若某测试类在 `beforeAll` 冻结后因硬故障（OOM / 强制 kill / `Assumptions` 中止）未触发 `afterAll` 恢复，泄漏的冻结时钟会污染同一 reused fork 中的下一个测试类。
3. **阻塞未来并行化**：任何提升 `threadCount` 或切换 `parallel=methods` 的尝试都会激活上述污染，使测试执行时间无法通过并行化优化。

**子类静态旁路（迁移面，非"零改动"）**：经核验，**全部 15 个子类各自携带** `public static void installFrozenClock()` + `public static void restoreSystemClock()`（每域文件 line 20-31），二者直接 `CoreMetrics.registerClock(...)`（绕过基类 `beforeAll/afterAll`）。这构成第二条全局替换路径：

- 核验命令（2026-08-01 复核）：`rg -n "public static void installFrozenClock" --glob '*.java'` → 15 文件命中；`rg -n "\.installFrozenClock\(\)|\.restoreSystemClock\(\)" --glob '*.java'` → 仅 1 处活跃调用 `module-hr/.../TestErpHrPayrollSimulation.java:444,477`（per-method try/finally 冻结模式）
- 影响：任何根治方案（§3 路径 C）若仅改基类、声明"子类零改动"，则这 15×2 静态旁路仍在，HR 的活跃调用会再次全局替换 `s_clock`——根治被绕过。故子类静态旁路是**显性迁移面**，§4.1 step 3 须给出每子类的调和方案（重实现为线程本地委托 / 删除并迁移调用点）。

### 1.3 当前 surefire 配置（污染为何「尚未显性爆发」）

nop-entropy 父 pom 定义 surefire（nop-app-erp 经父 pom 继承，无覆盖）：

- `../nop-entropy/pom.xml`（line 209-233）：
  - `<forkCount>1C</forkCount>` —— 每个可用 CPU 核启动一个 forked JVM
  - `<reuseForks>true</reuseForks>` —— 重用 forked JVM 跑多个测试类
  - `<parallel>classes</parallel>` —— 以测试类为分发单位跨 fork 分配
  - `<threadCount>1</threadCount>` —— **每个 fork 内单线程**（fork 内测试类串行）
- 核验命令（2026-08-01 复核）：`rg -n "forkCount|reuseForks|<parallel|threadCount" ../nop-entropy/pom.xml` → 命中上述四项。nop-app-erp 并行相关参数（forkCount/reuseForks/parallel/threadCount）经父 pom 继承、应用层无覆盖（`app-erp-all/pom.xml` 仅含 `systemPropertyVariables` 配置块，不动并行参数）

> 解读：当前 fork 内 `threadCount=1` 串行执行，使得「同一 fork 内两个冻结时钟扩展并发触发」的场景不发生——这是污染**未显性爆发**的原因，而非设计已安全。fork 间（多 JVM）静态状态天然隔离。故 Q6 的「并行不安全」是**latent 结构性缺陷 + 已记录的 future risk**（bug doc line 60 明示：「若未来引入并行测试执行，全局 `CoreMetrics.registerClock` 的静态副作用需改为线程级/上下文级时钟」），不是当前 CI 红的活跃源。当前活跃 CI 红源是 §1.4 的月初翻车税。

### 1.4 月初翻车税（唯一反复发作痛点，活跃 CI 红）

每月月初（系统时钟跨月，如 2026-07-31 → 2026-08-01），finance 域 4 个测试类的部分用例由绿转红，合计 **1 failure + 10 errors**：

| 测试类 | 失败数 | 失败模式 | 根因（引用 bug doc line 19） |
|--------|--------|----------|------------------------------|
| `TestErpFinBadDebtReversal` | 3 errors | `erp_fin_accounting_period` 断言 `MONTH=8/START_DATE=2026-08-01`，期望 7 | 过账链路 `voucherDate = CoreMetrics.today()` = 8/1，落在 seed 7 月期间外 |
| `TestErpFinEmployeeAdvanceCashRepayReversal` | 3 errors | 同上期间错配（`NAME=2026-08` 等） | 同上 |
| `TestErpFinNotesPayableStateMachine` | 4 errors | `erp_fin_notes_payable` `POSTED=false` 期望 `true`、`VERSION` 偏低 | 不设 `issueDate` → `voucherDate = 今天` 落在 seed 期间外 → 过账失败（无 OPEN 期间）→ `POSTED=false` |
| `TestErpFinDashboard.testTrendMonthlySeries` | 1 failure | `趋势包含 6 月` 期望 `true` 实际 `false` | 趋势窗口相对「今天」滚动 |

- **诊断陷阱（引用 bug doc line 24-26）**：失败**形似**「过账吞异常致 posted 悬挂」回归。决定性判别实验：将工作树变更全部 `git stash -u`（回到 HEAD）运行同一组 4 类 → 结果完全一致（`Tests run: 17, Failures: 1, Errors: 10`），逐条 error message 字段级相同 → 失败与最近代码变更**完全无关**，真正变量是「系统日期从 7/31 → 8/1」。
- **不应采用的「修复」（引用 bug doc line 40）**：按新月度重录快照——只是把时间炸弹推到下个月 1 号，且改变快照语义。
- 核验命令（2026-08-01 复核）：月初当日 `mvn test -pl module-finance/erp-fin-service -am` 红；月内其余时段绿。`docs/testing/known-good-baselines.md` 最近一次全量绿基线 2026-07-31 与「月初滚动致红」时间点吻合。

### 1.5 R6.9 单点日期硬化的边界（Q6 是其系统性超集）

plan `2026-08-01-0803-1` Phase 3 已对 5 处 pre-existing date-fragility 做**单点**硬化（非根治）：

- `TestErpFinNotesPayableStateMachine` 4 方法：`seedBase()` 改按 `YearMonth.now()` seed 当前运行月 OPEN 期间 + `@EnableSnapshot(checkOutput=false)`
- `TestErpFinDashboard.testTrendMonthlySeries`：seed 改相对月 + `@EnableSnapshot(checkOutput=false)`

**边界（为何非根治）**：R6.9 用「seed 相对当前月 + 关闭输出校验」消除单点红，但：
1. 未触碰 `CoreMetrics.s_clock` 全局静态并行不安全根因（§1.2 结构性缺陷仍在）
2. `YearMonth.now()` 仍是系统时钟读取——只是把「seed 期间」与「过账 voucherDate」对齐到同一墙钟月，而非冻结时钟
3. `BadDebtReversal` / `EmployeeAdvanceCashRepayReversal` 两类 R6.9 未覆盖（仍按 8 月重录快照，时间炸弹推到 9/1）
4. 关闭 `checkOutput` 是退让，非加固

> Q6 = R6.9 的系统性根治超集：根因层（全局静态时钟）+ 4 类全覆盖 + 不依赖 `checkOutput` 退让。

## 2. 目标与非目标

### 2.1 目标（Phase 1 = 本文档；Phase 2 实现见 §4）

1. **裁决时钟测试基础设施的技术选型**（路径 A / B / C，见 §3），给出候选、考虑的替代、残留风险三要素——满足 plan authoring guide §规则 9（Decision 项记录理由）。
2. **为 Phase 2 实现 plan 提供实施契约**：15 域子类兼容迁移方案 + 跨 nop-entropy 改造边界声明 + finance 4 类回归验证步骤（见 §4）。
3. **定义可验证的验收判据**（见 §5）：月初翻车税消除 + 并行测试不污染 + 15 域子类迁移完成性 + 全量回归绿。
4. **裁决 CI 门控**（见 §6）：是否引入「月初时钟滚动 CI 矩阵」作为回归层。

### 2.2 非目标

- **不实现任何代码/ORM/CI 变更**——本文档仅产出设计。Phase 2 实现（`CoreMetrics` 改造或测试侧迁移）是独立后续 plan，须在本文档审查收敛后方可起草（MQ 文档先行工作流硬约束）。
- **不修改 `nop-entropy` 源码**——若 §3 裁决路径 A，其实施属 Phase 2 + 须遵守 AGENTS.md「对 nop-entropy 的更改记录在 `nop-entropy/ai-dev/logs/`」。
- **不重录 finance 测试快照**（bug doc line 40 明示：按新月度重录快照只是把时间炸弹推到下个月，非修复）。
- **不覆盖 Q1-Q7 其他维度**——各有独立 Phase 1 设计文档计划（Q0 README §实施顺序裁决）。
- **不重新推导 NOT FOUND 证据**——§1 引用 Q0 README + bug doc + R6.9 plan，避免双真相源。
- **不改动生产过账链路时钟入口**——过账链路生产侧已合规（§1.1）；少量非过账链路 `LocalDate.now()` 残留登记 successor（§7），不在 Q6 范围。

## 3. 技术选型

> 本节裁决三路径。每路径记录：机制 / 优点 / 缺点 / 与现有基础设施冲突点。裁决（§3.4）记录候选、考虑的替代、残留风险（plan authoring guide §规则 9）。

### 3.1 路径 A —— nop-entropy `CoreMetrics` 加 thread-local / scoped clock 支持

**机制**：修改平台 `CoreMetrics`（`../nop-entropy/.../CoreMetrics.java`），将 `s_clock` 从「进程级静态单槽」改为「thread-local 或可叠加 scope」。两种子变体：

- A1：`s_clock` 改为 `ThreadLocal<IClock>`（默认回系统时钟）。`registerClock` 设置当前线程的时钟。优点：根治并行。缺点：所有跨线程异步代码（`ContextProvider` / dispatcher 线程池）需在线程切换时传播 thread-local，否则子线程读不到测试冻结值——侵入性大，且 nop-entropy 的 `ICoreLib` 异步设施需配合。
- A2：新增 scoped API `CoreMetrics.withClock(IClock, Supplier<T>)`（栈式 / try-with-resources），保留 `registerClock` 全局静态不变（向后兼容）。测试扩展改用 `withClock` 包裹测试体。优点：不破坏现有 `registerClock` 语义、作用域明确。缺点：测试扩展须改写为包裹每个测试方法（`InvocationInterceptor` 而非 `BeforeAllCallback`），迁移面比 A1 大；且仍依赖平台接受 PR。

**优点**：
- 平台原生根治，惠及**所有** nop-entropy 消费方（不止本应用）
- 与平台时钟抽象一致（时钟是平台概念，平台提供 scoped override 最自然）

**缺点 / 风险**：
- **跨仓库改动保护区域**：nop-entropy 是兄弟目录锁定的平台依赖。AGENTS.md 明示「对 nop-entropy 的更改记录在 `nop-entropy/ai-dev/logs/`」，且 nop-entropy 有独立 CI / 发布节奏。改动须以 PR 形式上游，merge 前 nop-app-erp 无法消费 → **升级耦合**阻塞 Phase 2 交付
- **回归面大**：`CoreMetrics` 是 nop-entropy 全域时间入口，A1 thread-local 改造触及所有异步路径，回归测试成本高、平台方接受门槛高
- **A2 不根治全局静态**：`registerClock` 仍是全局静态，仅新增 scoped 旁路——本应用 15 域子类仍走旧 `registerClock` 则根因仍在；若全量改走 `withClock` 则等同路径 B 的迁移工作量

### 3.2 路径 B —— 测试侧日期参数化（`@ParameterizedTest` 多日期），不动平台

**机制**：废弃 `AbstractFrozenClockExtension` 的全局 `registerClock`，改为每个日期敏感测试用 JUnit 5 `@ParameterizedTest` 跨多个固定日期（如月初 / 月中 / 月末 / 跨月边界）跑，断言相对日期而非绝对值。种子数据显式 `setIssueDate(fixedDate)` 使凭证日期不依赖「今天」。

**优点**：
- **应用层闭环**：零 nop-entropy 改动，无跨仓库 / 升级耦合
- 直接针对月初翻车税（跨月边界是参数之一）
- 与「测试应显式声明其日期假设」的测试纯洁性原则一致

**缺点 / 风险**：
- **不解全局静态并行污染（§1.2 结构性缺陷仍在）**：本路径聚焦「日期参数化」，但 `CoreMetrics.s_clock` 全局静态槽与 15 域 `registerClock` 调用仍在——只要任一测试还用 `registerClock`，并行不安全根因未除
- **与 `JunitAutoTestCase` + `RECORDING/CHECKING` 快照语义冲突**（同 Q3 属性测试的关键风险）：Nop 测试栈快照是「录制一次 → 回放多次」。`@ParameterizedTest` 多日期会产生多组输出，与单一快照文件冲突。要么 (a) 关 `checkOutput`（R6.9 已用此退让，非加固），要么 (b) 每个日期值一套快照（快照爆炸），要么 (c) 绕过 `JunitAutoTestCase` 用纯 JUnit 5（丢失平台测试基类能力）。三选一都有显著代价
- **迁移面大**：15 域子类 + 4 finance 类 + 所有日期敏感断言都要参数化，工作量大且易遗漏（遗漏即留时间炸弹）

### 3.3 路径 C —— 混合：应用层 thread-local delegating clock（根治并行）+ 按需日期参数化（治月初税）

**机制**：在应用测试层（`module-common-test`）引入一个 **thread-local delegating clock**，经 per-fork 一次性全局注册挂到 `CoreMetrics.s_clock`；各测试扩展只 set/clear 线程本地引用日期，不再整体替换全局槽。

核心设计（应用层，零平台改动）：

```
module-common-test/.../ThreadLocalFrozenClock.java   (新建)
  implements IClock:
    - static ThreadLocal<LocalDate> REF_DATE  (默认 unset)
    - static volatile boolean INSTALLED = false   ← 应用层幂等标志（平台无 s_clock 内省 API，见 §1.1）
    - currentTimeMillis() / nanoTime()  → 始终委托 CoreMetrics.defaultClock()（保单调时间真实）
    - currentDate() / currentDateTime() → REF_DATE 有值则返回冻结日期，否则委托 defaultClock()
    - install(LocalDate) / clear()  —— 线程本地 set/remove
    - ensureRegistered() —— if (!INSTALLED) { CoreMetrics.registerClock(this); INSTALLED = true; }
      （用应用层 volatile 标志替代"读 s_clock 比较类型"，因 CoreMetrics.s_clock private 无 getter）

AbstractFrozenClockExtension.java   (改写 beforeAll/afterAll)
  - beforeAll: ThreadLocalFrozenClock.ensureRegistered(); ThreadLocalFrozenClock.install(referenceDate);
               ← ensureRegistered 幂等（volatile 标志）；install 是线程本地，非全局替换
  - afterAll:  ThreadLocalFrozenClock.clear()   ← 清线程本地，全局槽保持 delegating clock 不变

15 域 *FrozenClockExtension 子类   (显式迁移面，非零改动——见 §1.2 子类静态旁路)
  - installFrozenClock() 重实现为 ThreadLocalFrozenClock.ensureRegistered(); ThreadLocalFrozenClock.install(REFERENCE_DATE);
  - restoreSystemClock() 重实现为 ThreadLocalFrozenClock.clear();
  - （或删除二静态 + 迁移 HR 的活跃调用点 TestErpHrPayrollSimulation:444,477 改用 @RegisterExtension）

per-fork 注册载体（M2 时序约束）
  - ensureRegistered() 在每个 forked JVM 内"首次类加载 ThreadLocalFrozenClock 时"触发：
    方案 a) 静态初始化块：ThreadLocalFrozenClock 的 static 块调用 CoreMetrics.registerClock(new ThreadLocalFrozenClock())
            （类被任一冻结扩展引用即加载，早于 beforeAll）
    方案 b) 专用 JVM 级 JUnit 5 扩展（@RegisterExtension Lookup 或 ServiceLoader 自动注册）
  - 关键：forkCount=1C + reuseForks=true（§1.3）→ 每个 fork 是独立 JVM，s_clock=DEFAULT_CLOCK 重置；
          "一次性"是 per-fork（每 fork JVM 首次加载触发），非全局单次。Phase 2 plan 须明示此 per-fork 时序。
```

**优点**：
- **根治并行不安全（§1.2 根因）**：冻结值在 `ThreadLocal`，不同线程测试类互不干扰——即使未来 `threadCount>1` 或 `parallel=methods` 也安全
- **零 nop-entropy 改动**：仅 `CoreMetrics.registerClock` 一次性挂载 delegating clock（API 既存，非改造平台），全部新逻辑在 `module-common-test` 应用层。无跨仓库 / 升级耦合，Phase 2 交付不被平台 PR 阻塞。**平台无 `s_clock` 内省 API**（§1.1 已核验 `s_clock` private 无 getter），故幂等用应用层 volatile 标志实现，不依赖平台
- **消除状态泄漏向量**：即使 `afterAll` 未触发，泄漏的是线程本地值（线程结束即 GC），不再污染同 fork 下一类
- **保留 `currentTimeMillis/nanoTime` 真实**（同现有 `ICClock` 设计），不破坏 `ContextProvider` 单调时间假设
- **可组合按需参数化**：对月初翻车税 4 类，叠加显式 `setIssueDate` / 相对 seed（R6.9 已示范），二者正交
- **快照语义不变**：冻结的是线程本地时钟读数，`JunitAutoTestCase`「录制一次 → 回放多次」语义不变（回放时线程本地读同一冻结值）。已核验 `AutoTestCase`/`JunitAutoTestCase` 基类**不触及时钟**（无 `CoreMetrics`/`Clock` 引用），与冻结机制无生命周期耦合——兼容性主张有据

**缺点 / 风险**：
- **15 域子类非"零改动"（§1.2 子类静态旁路）**：全部 15 子类各携 `installFrozenClock()`/`restoreSystemClock()` 静态直接 `registerClock`，须逐子类重实现为线程本地委托（或删除 + 迁移 HR 活跃调用点）。迁移面=15 子类 × 2 静态 + HR 1 调用点，但每子类改动机械（委托 ThreadLocalFrozenClock），非设计性返工
- **跨线程异步传播**：若被测生产代码把 `CoreMetrics.today()` 调用派发到子线程（如 `@Async` / dispatcher 线程池），子线程 `ThreadLocal` 未 set → 读到系统时钟而非冻结值。**评估**：finance 过账链路当前在测试线程内同步执行（`JunitAutoTestCase` 单线程同步模型 + `NotesPostingDispatcher` 同步 dispatch），无已知的测试内异步派发——此风险为 watch-only，Phase 2 须加一条「无测试内异步派发读 `CoreMetrics.today()`」的核验。若未来引入异步过账，再评估 `InheritableThreadLocal` 或 scope 传播
- **per-fork 注册时序（M2）**：delegating clock 须在每个 forked JVM 内、任何冻结扩展 beforeAll 之前挂到 `s_clock`。须保证幂等（volatile 标志）并经静态初始化块（类加载即触发，早于 beforeAll）保证时序。Phase 2 plan 须明示该 per-fork 时序约束
- **非平台原生**：本应用自管 thread-local clock，不上游贡献。其他 nop-entropy 消费方不受益（但这是应用层方案的合理边界，非缺陷）

### 3.4 裁决（Decision）

> 决策输入：§1 根因（全局静态 `s_clock` 单槽 = 结构性并行不安全 + 月初税活跃 CI 红）+ §3.1-3.3 三路径优缺点 + Q0 README §复杂度分级（Q6 平台依赖高、涉及模块中-高）+ AGENTS.md「优先 Model/Delta/定制，应用层闭环优先于改平台」+ bug doc line 60 future-risk 指引（线程级/上下文级时钟）。

**裁决：选路径 C（应用层 thread-local delegating clock 根治并行 + 按需参数化治月初税）**，路径 A 作为上游 follow-up 候选，路径 B 的参数化技术作为路径 C 的补充手段（用于 4 finance 类的显式 issueDate seed）。

**裁决理由**：

1. **根治 vs 治标对齐**：Q6 的双重目标——(a) 根除并行不安全 latent 结构性缺陷、(b) 消除月初税活跃 CI 红——只有路径 C 同时命中两者。路径 A 命中 (a) 但被升级耦合阻塞 (b) 的及时交付；路径 B 命中 (b) 但漏 (a)。
2. **应用层闭环优先（AGENTS.md 决策顺序 Model→Delta→Java，反模式表「优先定制而非新 Java」，及「应用层方案避免跨仓库依赖」与 Q4 同源原则）**：路径 C 全部新逻辑在 `module-common-test`，不触碰 nop-entropy 保护区域，无平台 PR 阻塞、无升级耦合、无独立 CI 节奏对齐成本。**且不依赖平台不存在的 `s_clock` 内省 API**（§1.1 核验），幂等用应用层 volatile 标志实现。路径 A 触及平台保护区域，与「保护区域默认阻塞」原则相悖。
3. **迁移成本（修正：子类非零改动）**：路径 C 的核心改动收敛在 `AbstractFrozenClockExtension` 基类（beforeAll/afterAll 内部）+ 15 子类的 `installFrozenClock()`/`restoreSystemClock()` 静态重实现为线程本地委托（§1.2 静态旁路，每子类改动机械）+ HR 1 活跃调用点（`TestErpHrPayrollSimulation:444,477`）迁移。相比路径 B（15 子类 + 4 finance 类 + 全部日期断言参数化 + 与快照正面冲突）与路径 A2（等同路径 B 迁移面 + 平台 PR），路径 C 的迁移面仍最小且无设计性返工。
4. **与 `JunitAutoTestCase` 快照语义无冲突（前提：install() 覆盖）**：路径 C 冻结的是线程本地时钟读数，快照仍「录制一次 → 回放多次」语义不变（回放时线程本地时钟读同一冻结值）。已核验 `AutoTestCase`/`JunitAutoTestCase` 基类不触及时钟，与冻结机制无生命周期耦合。**前提**：日期敏感测试类必须实际调用 `ThreadLocalFrozenClock.install()`（经 `@RegisterExtension *FrozenClockExtension` 或显式）——4 finance 类中 2 类（BadDebt/EmployeeAdvance）当前**未**注册冻结扩展、内联 `CoreMetrics.today()`（§1.4 表），§4.2 step 1-2 须为其补 `@RegisterExtension` 接线，否则 install() 不覆盖则冻结无效、月初税仍在。快照安全主张依赖 install() 覆盖率，列为 §5 closure-checkable 不变量。
5. **future-proof**：路径 C 使 surefire 可自由并行化（`threadCount>1` / `parallel=methods`）而不激活污染——解除测试执行时间优化阻塞，符合长期方向。HR 的 per-method 冻结模式（`installFrozenClock`/`restoreSystemClock` 包裹单个 `@Test`）在路径 C 下自然兼容：重实现后变为线程本地 install/clear，per-method 语义保留（仅作用于当前测试线程）。

**考虑的替代（记录为何否决）**：

- **路径 A1（thread-local 平台改造）**：否决作为主路径——升级耦合阻塞及时交付 + 回归面大 + 平台方接受门槛高。保留为上游 follow-up 候选（见 §7 successor）。
- **路径 A2（平台 scoped API `withClock`）**：否决——仍需平台 PR（同样升级耦合），且不根治 `registerClock` 全局静态（除非全量改走 `withClock`，等同路径 B 迁移面）。
- **路径 B（纯测试侧参数化）**：否决作为主路径——漏并行不安全根因（§1.2 结构性缺陷仍在），且与快照语义冲突。但其「显式 `setIssueDate` / 相对 seed」技术作为路径 C 的补充手段保留（用于 4 finance 类）。
- **路径 D（Mockito `mockStatic(CoreMetrics.class)` / JUnit 5 `MockedStatic`）**：评估后否决。`mockStatic` 默认线程作用域，理论上可 per-test 拦截 `CoreMetrics.today()` 而不动 `registerClock`。否决理由：(1) 须 mock 全部 `CoreMetrics` read 方法（`today`/`currentDate`/`currentDateTime`/`currentTimeMillis`/`nanoTime`/`currentTimestamp`）否则遗漏即漏网，且须显式对 `currentTimeMillis`/`nanoTime` 调 `CallingRealMethod` 保单调真实——易错；(2) 每 `@Test` try-with-resources `MockedStatic` 样板或 `@RegisterExtension` 接线，15 子类迁移样板比 delegating clock 重；(3) `mockito-inline` 依赖 + 静态 mock 运行时开销（每调用走拦截链）比 delegating clock 直接读 ThreadLocal 慢；(4) 同样须覆盖 15 子类静态旁路 + HR 调用点，迁移面不优于路径 C。结论：路径 C 的 delegating clock 在简单性/迁移成本/性能上均占优，mockStatic 不引入。
- **维持现状 + 仅扩 R6.9 单点硬化**：否决——R6.9 已证单点硬化非根治（§1.5 边界 4 条），且不触并行根因。

**残留风险**：

- **R1（跨线程异步传播，watch-only）**：若被测代码在测试内派发 `CoreMetrics.today()` 到子线程，子线程读不到冻结值。Phase 2 须核验「finance 过账链路测试内无异步派发读 today()」，若违反则评估 `InheritableThreadLocal`。当前过账同步执行，风险低。
- **R2（per-fork 注册时序）**：delegating clock 须在每个 forked JVM 内、任何冻结扩展 beforeAll 之前挂载。Phase 2 plan 须明示幂等注册（volatile 标志）+ per-fork 时序（静态初始化块，类加载即触发）。注意 `forkCount=1C` + `reuseForks=true` 下每 fork 是独立 JVM，"一次性"是 per-fork。
- **R3（路径 C 不上游贡献）**：本应用自管 thread-local clock，其他 nop-entropy 消费方不受益。接受（应用层方案合理边界），路径 A 作为上游 successor 候选保留（§7）。
- **R4（应用层 thread-local 与未来平台 scope API 共存）**：若未来 nop-entropy 原生支持 `withClock`，本应用 delegating clock 仍兼容（delegating clock 的 `install/clear` 可重实现为委托平台 scope），无锁定。
- **R7（子类静态旁路迁移完整性）**：15 子类 × 2 静态 + HR 调用点须全部迁移，遗漏任一则该子类仍全局替换 `s_clock` 绕过根治。§5 验收 4 增 grep 闭环校验。

## 4. 实施步骤（Phase 2 实现 plan 的范围契约）

> 本节为 Phase 2 实现 plan 提供步骤骨架与边界声明。Phase 2 plan 起草时（加载 `nop-testing` skill）以本节为实施契约，可细化但不得偏离已裁决路径 C 的范围。

### 4.1 应用层 thread-local delegating clock（核心根治）

1. 新建 `module-common-test/src/main/java/app/erp/common/test/ThreadLocalFrozenClock.java`：
   - `implements IClock`
   - `static ThreadLocal<LocalDate> REF_DATE`
   - `static volatile boolean INSTALLED = false`（应用层幂等标志——平台无 `s_clock` 内省 API，§1.1）
   - `currentTimeMillis()` / `nanoTime()` → 委托 `CoreMetrics.defaultClock()`（保单调时间真实）
   - `currentDate()` / `currentDateTime()` → `REF_DATE` 有值返回冻结日期，否则委托 `defaultClock()`
   - `install(LocalDate)` / `clear()` / `isInstalled()` 静态方法（线程本地 set/remove）
   - 幂等全局挂载：`ensureRegistered()` —— `if (!INSTALLED) { CoreMetrics.registerClock(new ThreadLocalFrozenClock()); INSTALLED = true; }`（用应用层 volatile 标志替代"读 s_clock 比较类型"，因 `CoreMetrics.s_clock` private 无 getter）
   - **per-fork 注册载体**：`static {}` 静态初始化块调用 `ensureRegistered()`——类被任一冻结扩展引用即加载触发，早于任何 `beforeAll`。注意 `forkCount=1C` + `reuseForks=true`（§1.3）→ 每 fork 独立 JVM、`s_clock` 重置，"一次性"是 per-fork（每 fork JVM 首次类加载触发），非全局单次
2. 改写 `AbstractFrozenClockExtension`：
   - `beforeAll`：`ThreadLocalFrozenClock.ensureRegistered(); ThreadLocalFrozenClock.install(referenceDate);`
   - `afterAll`：`ThreadLocalFrozenClock.clear();`（不再 `registerClock(defaultClock())`——全局槽保持 delegating clock）
   - 删除内部死代码 `ICClock` 内部类（被 delegating clock 取代）
3. **15 域子类静态旁路迁移（非零改动，§1.2）**：每子类的 `installFrozenClock()` / `restoreSystemClock()` 重实现为线程本地委托：
   - `installFrozenClock()` → `ThreadLocalFrozenClock.ensureRegistered(); ThreadLocalFrozenClock.install(REFERENCE_DATE);`
   - `restoreSystemClock()` → `ThreadLocalFrozenClock.clear();`
   - 备选：删除二静态 + 迁移活跃调用点（见 step 3b）
   - Phase 2 须逐域冒烟验证（每域至少一个用 `*FrozenClockExtension` 的测试类绿）
   - **3b. HR 活跃调用点迁移**：`module-hr/.../TestErpHrPayrollSimulation.java:444,477` 的 `HrFrozenClockExtension.installFrozenClock()`/`restoreSystemClock()`（per-method try/finally 冻结）——若保留二静态则改其实现即可（调用点不变）；若删除二静态则该测试改用 `@RegisterExtension HrFrozenClockExtension` 或方法级 `ThreadLocalFrozenClock.install/clear`。per-method 语义在路径 C 下保留（线程本地）
4. **未建子类的 4 模块审计**（`master-data` / `assets` / `aps` / `notify`）：Phase 2 须按命名命令逐模块核验是否存在日期敏感测试/生产代码：
   - 核验命令：`rg "CoreMetrics\.today|CoreMetrics\.currentDate|resolveOpenPeriod|LocalDate\.now" --glob '*.java' module-<域>/`
   - **`assets` 预标记为高概率候选**：经核验 assets 有 10+ posting dispatcher + processor 读 `CoreMetrics.today()`/`currentDate()`（`ValueAdjustmentPostingDispatcher` / `DepreciationPostingDispatcher` / `DisposalPostingDispatcher` / `CapitalizationPostingDispatcher` / `AssetMergePostingDispatcher` / `AssetSplitPostingDispatcher` / `AssetInventoryPostingDispatcher` / `MaintenanceCapitalizationPostingDispatcher` / `MaintenanceExpensePostingDispatcher` / `ErpAstDashboardBizModel` 等）+ 日期敏感测试 `TestErpAstDashboard`——与 finance 月初税同构。Phase 2 须为 assets 补建 `AstFrozenClockExtension` 子类或显式 `setIssueDate`，**不可默认跳过**
   - `master-data` / `aps` / `notify`：轻量审计；若存在日期敏感测试则同 assets 处理，若无则记「无」并跳过
   - 同时复核 §1.1 的 2 处生产 `LocalDate.now()` 残留（`ErpFinVoucherTemplateRenderTemplateProcessor:75` / `ErpMdCurrencyRefreshRatesFromApiProcessor`）是否已被现有测试覆盖

### 4.2 finance 月初翻车税 4 类（治标 + 根治验证）

> 路径 C 根治并行后，月初税的「凭证日期落 seed 期间外」仍需 4 类各自显式声明日期假设。4 类**现状不同**，Phase 2 须分类处理（核验：4 类的 `CoreMetrics.today()`/`@RegisterExtension` 现状已在 §1.4 表 + 本节复核）：

**A 组（2 类，内联 `CoreMetrics.today()` 未注册冻结扩展）——须补 `@RegisterExtension` 接线**：

1. `TestErpFinBadDebtReversal`（`.../entity/TestErpFinBadDebtReversal.java:69,133,178,280` 内联 `CoreMetrics.today()`）：补 `@RegisterExtension static FinFrozenClockExtension finClock = new FinFrozenClockExtension();`（冻结到 seed 期间所在月，如 2026-07-17），使 `CoreMetrics.today()` 返回冻结日 → `voucherDate` 落 seed OPEN 期间内；或改内联 `today` 为显式固定日 + seed note `setIssueDate`。恢复 `@EnableSnapshot(checkOutput=true)`（R6.9 退让回收，前提是冻结时钟使输出确定性）。
2. `TestErpFinEmployeeAdvanceCashRepayReversal`（`.../posting/TestErpFinEmployeeAdvanceCashRepayReversal.java:264` 内联 `CoreMetrics.today()`）：同 A 组策略。

**B 组（2 类，R6.9 已 seed 相对当前月，不读 CoreMetrics）——复核 checkOutput 回收**：

3. `TestErpFinNotesPayableStateMachine`（R6.9 已 `YearMonth.now()` seed 当前运行月 OPEN 期间 + `checkOutput=false`）：复核在路径 C 冻结时钟下是否可回收 `checkOutput=true`。注意：R6.9 用 `YearMonth.now()`（系统时钟）seed，路径 C 下若该测试注册冻结扩展则 `YearMonth.now()` 仍读系统时钟——Phase 2 须裁决是 (a) 该测试也补冻结扩展 + seed 改 `YearMonth.from(FinFrozenClockExtension.REFERENCE_DATE)`，还是 (b) 维持 `YearMonth.now()` + `checkOutput=false`（接受 R6.9 现状）。裁决记 Phase 2。
4. `TestErpFinDashboard.testTrendMonthlySeries`（R6.9 已相对 seed + `checkOutput=false`）：同 B 组复核 `checkOutput` 回收。

**注**：finance 域当前**仅** `TestErpFinReportRendering` 使用 `@RegisterExtension FinFrozenClockExtension`（核验：`rg -l "@RegisterExtension.*FinFrozenClockExtension" module-finance`）；4 失败类均未接线——这是月初税的直接原因（生产 `CoreMetrics.today()` 读系统时钟未冻结）。

**跨月滚动验证（§5.2 验收的本地机制，独立于 §6 CI 矩阵）**：

5. Phase 2 须给出**本地**跨月模拟证据（不依赖 §6 nightly workflow 即可 closure）：
   - 方式 a（推荐）：新增一个 dedicated 测试类 `TestClockRolloverFinance`，在测试内 `ThreadLocalFrozenClock.install(月末日)` 后跑 4 类的 seed+断言逻辑，再 `install(次月1日)` 复跑，断言全绿。
   - 方式 b：本地 `faketime '2026-08-31' mvn -pl module-finance/erp-fin-service -am test` + `faketime '2026-09-01' ...` 复跑 finance 套件。
   - 方式 c（最弱）：4 类的 seed 改用冻结扩展 + 固定 REFERENCE_DATE，则「跨月」等价于「冻结值不变」，逻辑上证明跨月确定性（但缺运行时跨月证据，仅作退路）。
   - §6 nightly workflow 作为持续回归层补充（防路径 C 实施遗漏 + 防未来新日期敏感测试回潮）。

### 4.3 跨 nop-entropy 改造边界声明

| 改动面 | 位置 | 路径 C 是否触碰 | 说明 |
|--------|------|-----------------|------|
| `CoreMetrics` 源码 | `../nop-entropy/.../CoreMetrics.java` | **否** | 仅用既存 `registerClock` API 一次性挂载 delegating clock，非改造平台 |
| `IClock` 接口 | `../nop-entropy/.../IClock.java` | **否** | 仅实现该接口 |
| `AbstractFrozenClockExtension` | `module-common-test`（应用层） | **是** | 改 beforeAll/afterAll 内部实现 + 删除死代码 `ICClock` 内部类 |
| `ThreadLocalFrozenClock` | `module-common-test`（应用层，新建） | **是** | 新增类 + per-fork 静态初始化注册 |
| 15 域 `*FrozenClockExtension` 子类 | 各域 `erp-*-service/src/test` | **是**（§1.2 静态旁路） | 每子类 `installFrozenClock()`/`restoreSystemClock()` 重实现为线程本地委托（机械改动） |
| HR `TestErpHrPayrollSimulation:444,477` 调用点 | `module-hr/erp-hr-service/src/test` | **是**（仅若删二静态） | per-method 冻结模式迁移（保留语义） |
| finance 4 失败测试类 | `module-finance/erp-fin-service/src/test` | **是**（§4.2） | A 组 2 类补 `@RegisterExtension` 接线；B 组 2 类复核 checkOutput 回收 |
| assets/aps/notify/master-data 4 模块审计 | 各域 `src/test` | **可能**（§4.1 step 4） | assets 预标记高概率须补子类；其余视审计结果 |
| 生产代码（过账链路） | `module-finance/erp-fin-service/src/main` | **否** | 过账链路已合规（§1.1），不动 |
| 生产 `LocalDate.now()` 残留（2 处） | finance/master-data `src/main` | **否**（范围外） | 登记 successor（§7），不在 Q6 Phase 2 |

> 边界裁决：路径 C **零 nop-entropy 改动**，全部在应用层。Phase 2 无须在 `nop-entropy/ai-dev/logs/` 记日志（AGENTS.md 仅在改 nop-entropy 时要求）。

### 4.4 Phase 2 执行顺序建议

1. 4.1 step 1-2（delegating clock 新建 + per-fork 静态注册 + 基类改写）——核心根治，先行
2. 4.1 step 3（**15 域子类静态旁路迁移**，非零改动——`installFrozenClock`/`restoreSystemClock` 重实现为线程本地委托）+ 3b（HR `TestErpHrPayrollSimulation:444,477` 调用点迁移）+ 逐域冒烟验证（每域至少一个用 `*FrozenClockExtension` 的测试类绿）
3. 4.1 step 4（assets 等 4 模块审计；assets 预标记高概率候选须补子类）
4. 4.2 finance 4 类（A 组 2 类补 `@RegisterExtension` 接线；B 组 2 类复核 checkOutput 回收）
5. §5 全量验收（含 `TestThreadLocalFrozenClockParallel` 并行证明 + `TestClockRolloverFinance` 跨月模拟）+ §6 CI 门控接线（若 §6 裁决纳入）
6. 回归基线更新（`docs/testing/known-good-baselines.md`）

## 5. 验收判据（Phase 2 closure gate 契约）

> 每条须在 Phase 2 closure audit 时由独立子代理在 live repo 核验。每条给出具体可执行机制（不依赖 §6 nightly workflow）。

1. **并行测试不污染（根因消除，可复现）**：`ThreadLocalFrozenClock` 落盘 + `AbstractFrozenClockExtension` 改写 + 15 域子类静态旁路迁移。**可执行核验**：
   - `rg "ThreadLocalFrozenClock" module-common-test/` 命中新建类
   - 新增 dedicated 测试 `TestThreadLocalFrozenClockParallel`：起 2 线程各 `ThreadLocalFrozenClock.install(不同日期)`，断言各线程内 `CoreMetrics.today()` 返回各自冻结日、互不串扰（这是 §3.3 根治主张的客观证明）
   - grep 闭环：`rg "registerClock" module-common-test/ module-*/erp-*-service/src/test/` 不再含"每子类静态整体替换"模式（仅 `ThreadLocalFrozenClock` 内一次性挂载 + 各子类委托 `ThreadLocalFrozenClock.install/clear`）
2. **月初翻车税消除（本地跨月模拟，独立于 §6）**：4 类在跨月模拟下全绿（不再 1 failure + 10 errors）。**可执行核验**（§4.2 方式 a/b/c 任一，推荐 a）：
   - 方式 a：`TestClockRolloverFinance` 在 `install(月末日)` + `install(次月1日)` 下复跑 4 类 seed+断言逻辑全绿
   - 方式 b：`faketime '2026-08-31' mvn -pl module-finance/erp-fin-service -am test` + `faketime '2026-09-01' ...` finance 套件全绿
   - closure 时由方式 a/b 提供运行时跨月证据；方式 c（冻结值不变逻辑证明）仅作退路
3. **`checkOutput` 退让回收**：R6.9 关闭 `checkOutput` 的 5 处，路径 C 冻结时钟后尽可能回收为 `true`。**可执行核验**：`rg "checkOutput\s*=\s*false" module-finance/erp-fin-service/src/test/` 命中数下降（理想为 0）；个别无法回收须在 Phase 2 记理由（如 B 组 NotesPayable/Dashboard 裁决维持）。
4. **15 域子类 + 静态旁路迁移完成性**：
   - `rg -l "extends AbstractFrozenClockExtension" --glob '*.java'` 仍 ≥15（assets 若补建则 +1）
   - `rg -l "public static void installFrozenClock" --glob '*.java'` 15 文件的二静态全部重实现为线程本地委托（grep 其方法体不含裸 `CoreMetrics.registerClock(new ...IClock`——实际静态用 `io.nop.api.core.time.IClock` 接口，grep `CoreMetrics.registerClock(new` 即可匹配未迁移的旧实现；迁移后改含 `ThreadLocalFrozenClock`）
   - HR 调用点 `TestErpHrPayrollSimulation:444,477` 迁移完成（per-method 语义保留）
5. **全量回归绿**：`mvn clean install -DskipTests` 156 模块 BUILD SUCCESS；`mvn test` 0 failures / 0 errors。finance 测试计数基线以 `docs/testing/known-good-baselines.md` 最近全量绿基线为准（截至 2026-07-31 finance 306 测试；Phase 2 若新增 `TestThreadLocalFrozenClockParallel` / `TestClockRolloverFinance` 则相应增加）。
6. **无双真相源**：本文档 §1 引用上游真相源（Q0 README + bug doc + R6.9 plan），Phase 2 plan 引用本文档，不重推导证据。
7. **install() 覆盖不变量（快照安全前提，§3.4 理由 4）**：所有读 `CoreMetrics.today()`/`currentDate()` 的 finance 测试类（含 4 失败类）均经 `@RegisterExtension *FrozenClockExtension` 或显式 `ThreadLocalFrozenClock.install()` 冻结。**可执行核验**：交叉 `rg "CoreMetrics\.today|CoreMetrics\.currentDate" module-finance/erp-fin-service/src/test/` 与 `rg "@RegisterExtension.*FrozenClockExtension" module-finance/...`，未冻结的 today() 读取须为 0（或登记例外理由）。

## 6. CI 门控设计

> 裁决是否引入「月初时钟滚动 CI 矩阵」作为回归层。记录候选、考虑的替代、残留风险。

### 6.1 现状

- 现有 CI（`.github/workflows/`）：`maven.yml`（构建/测试）+ `compliance.yml`（F8 反模式基线 + F15 i18n + web 页面校验）+ `e2e.yml`。**零「系统日期参数化」维度**——时间炸弹只在自然月初（每月 1 号）暴露，且依赖 CI runner 系统日期恰为月初。
- bug doc §预防差距 line 65 明示：「`mvn test` 无『月初时钟滚动』维度的 CI 矩阵，时间炸弹只在自然月初暴露」。

### 6.2 候选

- **C-1：不引入 CI 矩阵**（路径 C 落地后月初税已根治 + §5 验收 2 跨月模拟本地验证）。残留：若路径 C 实施有遗漏，月初税回归只在自然月初暴露。
- **C-2：引入「月初时钟滚动」nightly CI 矩阵**：在 `compliance.yml` 或新建 workflow 增加一个 job，用系统日期参数化（如 Linux `faketime` 或 JVM `-D` 系统属性覆盖）跑 finance 套件，模拟月初/月中/月末/跨月边界。
- **C-3：引入「冻结时钟并发」CI 矩阵**：用 `threadCount>1` / `parallel=methods` 跑 finance 套件，验证路径 C 的 thread-local 并行根治。

### 6.3 裁决（Decision）

> 决策输入：路径 C 已根治并行（§3.3）+ 治月初税（§4.2）→ CI 矩阵的边际价值下降；但「防止自然月初 CI 红 + 防路径 C 实施遗漏」有正向收益。CI 成本：`faketime`/系统日期覆盖在 GitHub Actions runner 上的可靠性 + nightly 调度对齐。

**裁决：Phase 2 引入 C-2（月初时钟滚动 nightly CI 矩阵）作为回归层；C-3 并发矩阵列为 watch-only successor（路径 C 落地后并发安全由 §5 验收 1 本地核验保证，CI 矩阵收益不足以抵 nightly 成本，待 surefire 实际并行化时再评估）。C-1 否决（仅靠本地验证不足以防自然月初 CI 红噪音 + 路径 C 实施遗漏）。**

**裁决理由**：

1. **防自然月初红噪音**：即使路径 C 根治，Phase 2 实施可能遗漏某个日期敏感测试（路径 C 无法覆盖未使用冻结扩展的测试）。nightly 跨月矩阵是「防遗漏」的最直接回归层，避免月初 CI 红再次被误判为最近变更回归（bug doc §诊断陷阱）。
2. **本地验证不可信**：开发者本地很少在月初当日跑测试；CI runner 系统日期也非可控。nightly 用 `faketime` 模拟跨月是唯一可靠的回归层。
3. **成本可控**：nightly（非 per-commit）跑 finance 套件（306 测试，单域）成本低；`faketime` 是成熟工具（GitHub Actions 可装）。

**考虑的替代**：

- **C-1（不引入）**：否决——路径 C 落地质量依赖本地一次性验证，无持续回归层，月初税易回潮。
- **C-3（并发矩阵）**：否决作为本期——路径 C 的并发根治由 §5 验收 1 本地核验（模拟并发读 `CoreMetrics.today()`）保证；nightly 并发矩阵收益不足以抵成本，列为 successor（触发：surefire 实际切 `parallel=methods` / `threadCount>1` 时）。
- **per-commit 跨月矩阵**：否决——per-commit 跑跨月模拟成本高且月初税是低频回归，nightly 足够。

**残留风险**：

- **R5（`faketime` 与 JVM 时区/`LocalDate.now` 兼容性）**：`faketime` 在 `System.currentTimeMillis()` 层拦截，`LocalDate.now()` 经其派生应一致，但须 Phase 2 实测确认。若不兼容，退而用 JVM `-Djava.time.clock.system=...` 或测试内系统日期 mock。
- **R6（nightly 与 per-commit 漂移）**：nightly 红与 per-commit 绿之间的窗口期可能引入月初税回归未被即时发现。接受（nightly 频率合理，月初税非阻塞交付）。

### 6.4 与现有 CI 的集成方式（Phase 2 落地）

- 新建 `.github/workflows/clock-rollover.yml`（或加 job 到 `compliance.yml`）：
  - `schedule: cron: '0 3 * * *'`（nightly）+ `workflow_dispatch`
  - 步骤：`sudo apt-get install faketime` → `mvn -B -pl module-finance/erp-fin-service -am test` 经 `faketime '<绝对日期如 2026-08-31>'` 或 `faketime '<相对偏移如 -1d>'` 参数化跨月边界（注：`faketime` 接受绝对 `YYYY-MM-DD HH:MM:SS` 或相对偏移，**不接受** `'last day of last month'` 等日历表达式——Phase 2 须用具体日期参数矩阵：月末当日 / 次月 1 日）
  - 失败即红，通知机制对齐现有 workflow
- Phase 2 plan 须给出该 workflow 的实际接线 + 首次 nightly 绿基线

## 7. 残留风险汇总与 successor

> 汇总 §3.4 + §6.3 残留风险，登记 successor 触发条件（plan authoring guide §反松弛规则：Follow-up 须命名触发条件）。

| 风险 ID | 描述 | 分类 | successor 触发条件 |
|---------|------|------|--------------------|
| R1 | 跨线程异步传播（测试内 `CoreMetrics.today()` 派发子线程） | watch-only | finance 过账引入测试内异步派发时；评估 `InheritableThreadLocal` |
| R2 | delegating clock 一次性注册时序 | Phase 2 实施约束 | Phase 2 plan 须明示幂等注册 + 早于任何冻结扩展 |
| R3 | 路径 C 不上游贡献（其他 nop-entropy 消费方不受益） | 应用层边界 | 上游 nop-entropy 接受路径 A scope API PR 时，delegating clock 可重实现为委托平台 scope |
| R4 | 应用层 thread-local 与未来平台 scope API 共存 | 兼容性 | 同 R3，无锁定（delegating clock 可委托平台） |
| R5 | `faketime` 与 JVM 时区兼容性 | Phase 2 实施约束 | Phase 2 实测确认；不兼容则换 JVM 系统属性 / 测试内 mock |
| R6 | nightly 与 per-commit 漂移 | 接受 | nightly 频率合理，月初税非阻塞 |
| R7 | 子类静态旁路迁移完整性（15×2 静态 + HR 调用点） | Phase 2 实施约束 | §5 验收 4 grep 闭环校验 |
| —（successor） | Q6 Phase 2 实现 plan（按路径 C 实施） | out-of-scope（本文档 Phase 1） | 本文档经 ≥2 轮独立审查收敛（§Review Record）+ 技术选型裁决落定（§3.4）→ DRAFT_PLANS 起草 |
| —（successor，**已闭合** 2026-08-02） | 生产 `LocalDate.now()` 残留 2 处（finance/master-data） | ~~out-of-scope improvement~~ → 已闭合 | **已由 plan `2026-08-02-0650-1` 消解**：2 处直调替换为 `CoreMetrics.today()` + 2 聚焦日期边界测试（`TestErpFinVoucherTemplateActiveByDate` / `TestErpMdCurrencyRefreshRatesDate`）；§1.1 闭合回填一致。原触发条件（Q6 Phase 2 落地后或月初税在同族维度复现）已满足并闭合。 |
| —（successor） | C-3 并发 CI 矩阵 | watch-only successor | surefire 实际切 `parallel=methods` / `threadCount>1` 时 |
| —（successor） | 路径 A 平台 scope API 上游贡献 | out-of-scope improvement | nop-entropy 消费方普遍需要 scoped clock 或本应用异步过账使 thread-local 不足时 |

## Review Record

> 审查记录：MQ 文档先行工作流要求 ≥2 轮独立子代理审查（第 1 轮规范合规 + 第 2 轮覆盖面/可执行性），由不同子代理会话执行（不同 task id），审查者不可与作者为同一会话。每轮输出 BLOCKER/MAJOR/MINOR 分级意见，作者修订后重审直至收敛（无残留 BLOCKER/MAJOR）。本文档经 3 轮审查收敛（R1 合规 + R2 覆盖/可执行性 + R3 收敛复核）。

- **Round 1（规范合规审查）**: `ses_04475edf2ffe6PXzEssHsfgmXR`（独立子代理 fresh session cold context）— **needs-revision**，0 BLOCKER / 1 MAJOR / 0 MINOR。
  - MAJOR-1：§1.1 绝对主张「生产侧无 `LocalDate.now()` 直调残留」被实仓证伪（finance `ErpFinVoucherTemplateRenderTemplateProcessor:75` + master-data `ErpMdCurrencyRefreshRatesFromApiProcessor` 命中），且其核验命令未覆盖 `LocalDate.now()`。
  - 修改摘要：§1.1 标题与结论收窄为「过账链路已合规；少量 `LocalDate.now()` 残留范围外」，枚举 2 处残留 + 补 `rg LocalDate.now()` 核验命令 + 登记 §7 successor；Non-Goal §2.2 同步收窄。
  - Phase 1 item 覆盖映射：6 content items + 2 exit criteria 全部 ✓（subject to MAJOR-1 证据修正，已修）。

- **Round 2（覆盖面与可执行性审查）**: `ses_04475ab77ffe1hu9213Pi5I56Z`（**另一个**独立子代理，不同 task id，新会话）— **needs-revision**，1 BLOCKER / 5 MAJOR / 6 MINOR。
  - BLOCKER B1：全部 15 子类各携 `installFrozenClock()`/`restoreSystemClock()` 静态直调 `registerClock`（绕过基类），HR `TestErpHrPayrollSimulation:444,477` 活跃调用——「15 域零改动」主张为假，路径 C 治理会被绕过。
  - MAJOR M1：`ensureRegistered()` 经内省 `s_clock` 不可实现（private 无 getter）。
  - MAJOR M2：「一次性全局注册」per-fork 不自洽（forkCount=1C + reuseForks=true，每 fork 独立 JVM）。
  - MAJOR M3：§5.1/§5.2 验收不可测（无具体 harness / 依赖未存在的 §6）。
  - MAJOR M4：§4.1 step 4 低估 assets（10+ posting dispatcher 读 `CoreMetrics.today()`，与 finance 月初税同构）。
  - MAJOR M5：快照兼容性论证不充分；HR per-method 冻结未处理；4 finance 类未接线（仅 `TestErpFinReportRendering` 用 `@RegisterExtension`）。
  - coverage gap：未评估第 4 路径 Mockito `mockStatic(CoreMetrics.class)`。
  - 修改摘要：§1.2 增「子类静态旁路」段；§3.3 重写（volatile 标志幂等 + per-fork 静态初始化注册载体 + 子类静态迁移面 + AutoTestCase 无时钟耦合证据）；§3.4 修正 reason 3（非零改动）+ 强化 reason 4（install 覆盖前提）+ reason 5（per-method 兼容）+ 增 Path D（mockStatic）评估否决 + 增 R7；§4.1 step 1（volatile 标志 + per-fork 静态块）+ step 3（子类静态迁移 + HR 调用点 3b）+ step 4（assets 预标记高概率 + 命名核验命令）；§4.2 A/B 组分类（A 组补 `@RegisterExtension` 接线）+ 跨月模拟本地机制；§4.3 边界表修正；§5 全部改可测（`TestThreadLocalFrozenClockParallel` + 跨月模拟方式 a/b/c + install 覆盖 grep 不变量）；§6.4 faketime 语法修正；§7 successor trigger 修正 + 增生产残留 successor。

- **Round 3（收敛复核）**: `ses_0446e86deffenZQR9IvI9hZBxt`（独立子代理 fresh session cold context）— 8 项前序 finding 全部 **resolved**（逐条 live 实仓复核），1 NEW MAJOR / 1 NEW MINOR。
  - NEW-MAJOR-1：§4.4 执行顺序建议 step 2 残留「零改动子类」措辞，与本档 6 处「非零改动」自相矛盾；且 §4.4 未把 §4.1 step 3（子类静态迁移）列为显式有序步骤，误导实现者。
  - NEW-MINOR-1：§5 验收 4 grep 负向 pattern 笔误（`ICClock`→实际 `IClock` 接口）。
  - 修改摘要：§4.4 执行顺序建议重写为 6 步（含显式「4.1 step 3 子类静态旁路迁移 + HR 调用点 + 逐域冒烟」有序项，去除「零改动」措辞）；§5 验收 4 grep 描述修正为 `CoreMetrics.registerClock(new`（匹配未迁移旧实现）。
  - Round 3 结论原文：「All 8 prior findings resolved with accurate live evidence... After the trivial §4.4 reword this doc should converge to accept.」

**收敛结论**：3 轮审查后无残留 BLOCKER / 无残留 MAJOR（R1/R2 的 8 项 finding 经 R3 逐条 live 复核 resolved；R3 的 1 NEW MAJOR + 1 MINOR 已修订）。文档可作为 Phase 2 实现 plan 的实施契约。MINOR 不阻塞收敛。

<!-- 审查者多样性已满足：R1（ses_04475edf...）/ R2（ses_04475ab7...）/ R3（ses_0446e86d...）三会话 task id 不同，均独立 fresh cold context，未复用作者上下文。 -->

### Phase 2 实施期回填（implementation-time revision）

> Phase 2 实现（plan `2026-08-01-1357-1`）发现 §3.3 的「per-fork 一次性注册持久 + INSTALLED volatile 标志幂等」机制假设不成立，已修订实现（非静默偏离）。回填于此以维持「实现与设计文档一致」gate。

- **发现**：平台 `NopJunitExtension.afterAll`（`../nop-entropy/nop-autotest/nop-autotest-junit/src/main/java/io/nop/autotest/junit/NopJunitExtension.java:64-66`）在每个测试类结束后执行 `CoreMetrics.registerClock(CoreMetrics.defaultClock())`，将全局静态时钟槽重置为系统真实时钟。此清理在每个 reused fork 内、每个测试类边界发生（非仅 fork 结束）。
- **影响**：§3.3 / §4.1 step 1 假设「delegating clock 经 static {} 一次性注册即 per-fork 持久」被证伪——前一类的 afterAll 重置 s_clock 后，下一类的 `beforeAll` 若依赖 `INSTALLED` 标志幂等跳过则不再重新挂载 delegating clock（`INSTALLED=true` 但 `s_clock=defaultClock`），冻结失效。实测：finance 全模块跑（reuseForks）下 `TestErpFinEmployeeAdvanceCashRepayReversal` 跨类边界后产出系统 8 月而非冻结 7 月。
- **修订**：`ThreadLocalFrozenClock.ensureRegistered()` 改为**每次调用均重新注册**（移除 `INSTALLED` 标志守卫）。安全依据：delegating clock 在 `REF_DATE` 未 set 时委托 `CoreMetrics.defaultClock()`（系统真实时钟），对非冻结测试无行为影响；`REF_DATE` 为静态 ThreadLocal，所有实例共享冻结值。各冻结扩展 `beforeAll` 调 `ensureRegistered()` 重新挂载后，`NopJunitExtension.afterAll` 的重置被下一类 beforeAll 覆盖。
- **与 §3.4 裁决的关系**：路径 C 选型与范围不变（应用层 thread-local delegating clock，零平台改动）。仅「幂等机制」由 volatile 标志改为「无条件注册」（后者在效果上亦幂等——每次调用后 s_clock 必为 delegating clock）。设计文档 §1.1 已核验「平台无 `s_clock` 内省 API」，故无条件注册是无从内省时的最稳健选择。
- **验证**：修订后 `mvn test` 全 reactor 1920 tests / 0 failures / 0 errors；finance 307 / hr 125 / assets 104 三域跨类边界均绿。
