# 属性测试（MQ Q3）—— Phase 1 设计文档

> Owner Doc for Milestone MQ Q3（属性测试）
> 创建日期：2026-08-01
> Plan：`docs/plans/2026-08-01-1121-1-mq-q3-property-based-testing-design-doc.md`
> 单一真相源依赖：本文档是 MQ 文档先行工作流 **Phase 1** 产物（设计/策略文档），**不实现任何代码/ORM/CI 变更**。Phase 2 实现 plan（jqwik 接入 + 3-5 个核心不变量属性 test）须在本文档审查收敛后方可起草。
> 上游真相源（**只引用**，不重推导，避免双真相源漂移）：
> - `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q3（line 676 工作项表 + line 785 维度说明 + §横切关注点 §文档先行工作流 line 843-862）
> - `docs/architecture/quality-engineering/README.md`（Q0 范围矩阵 + 复杂度分级 + 实施顺序裁决基线，Q3 位 4）
> - `docs/plans/2026-08-01-1121-1-mq-q0-quality-gap-analysis-readme.md` §Current Baseline（Q3 NOT FOUND 实仓证据已核验：全仓零属性测试依赖）
> - ERP 强不变量 owner doc 真相源（**不变量定义不在此重新推导**，只引用）：
>   - `docs/design/finance/posting.md`（借贷平衡不变量真相源）
>   - `docs/design/finance/period-close.md`（期间结账后余额归零不变量真相源）
>   - `docs/design/finance/costing-methods.md`（成本层累加 = 余额表 + costMethod 切换前后总成本不变 + 红冲成本不变量真相源）
>   - `docs/design/finance/budget.md`（承付释放不超余量不变量真相源）
> - sibling Q1 `docs/architecture/quality-engineering/mutation-testing.md`（Q1↔Q3 协同——Q1 盲区类清单可指导 Q3 属性 test 目标选择）+ `mutation-baseline.md`（Phase 2 实测基线，Q3 可复用其 finance 顶盲区 `ErpFinPostingProcessor` 作为属性目标）
> - sibling Q4 `docs/architecture/quality-engineering/fault-injection.md`（同为测试有效性维度，文档结构参照）
> - sibling Q6 `docs/architecture/quality-engineering/clock-test-infrastructure.md`（并行隔离协同——Q3 属性 test 多迭代须与 Q6 thread-local clock 并行隔离无冲突）

## 1. 现状评估

> 本节**引用**（非重推导）上游真相源已核验事实，每条标注可复现核验命令 + 核验日期，便于 Phase 2 plan 与独立审查复核。证据核验日期：2026-08-01（HEAD 含 R6.9 收口）。

### 1.1 全仓零属性测试依赖（property-based testing 完全空白）

ERP 全仓无任何属性测试库接入——无 jqwik、无 junit-quickcheck、无任何 property-based 库。约 1900+ JUnit 测试 + 260+ E2E spec（计数引用 Q0 README + roadmap line 697，本节不单独核验）均为**黄金路径具体断言**（单次固定输入 → 固定期望输出）。

- 核验命令（2026-08-01 复核零命中）：`rg "jqwik|quickcheck" --glob '*.xml'`（工作目录 = nop-app-erp）→ **EXIT=1（零命中）**。覆盖范围：nop-app-erp 工作树内全部 `pom.xml`——无 `jqwik` 依赖声明、无 `junit-quickcheck` 依赖、无任何 property-based 库。
  - **nop-entropy 父 pom 传递依赖复核**：上述命令搜 nop-app-erp 工作树，不搜兄弟目录 `../nop-entropy/`。父 pom（`../nop-entropy/pom.xml`，经本项目根 pom `<parent>` 继承）的传递依赖由 Phase 2 接入时经 `mvn dependency:tree -pl module-finance/erp-fin-service` 复核（届时确认父 pom 无既有 jqwik/quickcheck 传递依赖）。本期 §现状评估 的"零属性测试依赖"主张限定于 nop-app-erp 工作树。
- 引用源：roadmap line 697 + line 785（Q3 维度说明）；Q0 README §范围矩阵 §Q3（核验日期 2026-08-01）；Q0 plan §Current Baseline NOT FOUND 证据第 3 条。
- 后果：当前「不变量恒成立」**无随机化验证**。黄金路径具体断言只能证明「特定输入下不变量成立」，**不能证明 ERP 强不变量在任意操作序列下恒成立**——这是黄金路径测试的结构性盲区（§1.2）。

### 1.2 黄金路径具体断言的结构性盲区

当前 ~1900+ JUnit 测试 + 260+ E2E spec 的测试范式统一为：

```
固定输入（手工设计的代表性场景）→ 触发业务动作 → 断言固定期望输出
```

此范式（golden-path concrete assertion）能证明**被测路径在具体场景下成立**，但**不能证明不变量在任意操作序列下恒成立**。典型盲区：

- **借贷平衡**：单测断言「这张发票过账后借贷相等」，但无法证明「任意金额组合 / 任意行数凭证模板下借贷恒等」。
- **成本层累加 = 余额表**：单测断言「这次出库后成本层累加等于余额表 totalCost」，但无法证明「任意出库序列（部分消耗 / 跨层 / 红冲）下累加恒等」。
- **承付释放不超余量**：单测断言「这次发票审核后承付正确释放」，但无法证明「任意部分开票 / 退货 / 冲销组合序列下预算余量 `available = budget − actual − commitment ≥ 0` 恒成立」（owner doc `budget.md:18` 三通道分离语义）。

属性测试（property-based testing）通过**随机生成输入**（jqwik 每属性默认 100 次迭代）+ **每步断言不变量**弥补此盲区：它不预先列举所有场景，而是让随机输入「攻击」不变量，发现的失败用例经**收缩（shrinking）**缩小到最小可复现输入。这正是 Q3 要弥补的结构性盲区。

### 1.3 ERP 强不变量清单（引用 owner doc 真相源，不重新推导）

> **重要**：本节**引用** owner doc 已定义的不变量作为属性测试目标候选，**不重新推导不变量定义**（避免双真相源漂移）。每条标注真相源 + 不变量的形式化表达 + owner doc 成熟度（用于 §4 优先级排序）。

ERP 拥有大量**强不变量**（形式化验证的理想对象），均已在 owner doc 中定义：

| 不变量 | 形式化表达 | 真相源 | owner doc 成熟度 |
|--------|-----------|--------|-----------------|
| **借贷平衡** | 每张凭证 `Σ debitAmount == Σ creditAmount`；过账后总账余额满足 `资产 = 负债 + 权益` | `docs/design/finance/posting.md`（凭证模板机制 + `VoucherFact.balanceTotals`/`assertBalanced` 本位币为准） | 高（业财一体核心，既有 `VoucherFact.balanceTotals` 引擎内置校验） |
| **期间结账后余额归零** | 损益结转后收入/费用类账户本期发生额清零（净额结转至本年利润）；年度结转时本年利润科目余额清零（结转至未分配利润）；永久（资产负债类）账户余额跨期累计 | `docs/design/finance/period-close.md`（步骤5 损益结转 + 年度结转本年利润清零，line 9/89-93/258-259） | 中-高（月度/年度结账已落地，含反结账红冲闭环） |
| **成本层累加 = 余额表** | 任意时刻 `Σ ErpInvCostLayer.remainingQuantity × unitCost == ErpInvStockBalance.totalCost`（layer-based costMethod 适用） | `docs/design/finance/costing-methods.md`（FIFO 队列结构 + `StockMoveBookkeeper` 经 `BookingContext` 写层与余额） | 中（FIFO/MOVING_AVERAGE 已落地；具体 scope 由 §4 裁决引用 costing-methods.md） |
| **costMethod 切换前后总成本不变** | STANDARD_REVALUATION 红冲跨重估时 `balance.totalCost` 恢复不变量；红冲后 `Σ layer.remaining × unitCost` 恢复至原出库前 | `docs/design/finance/costing-methods.md` line 37/74/472（FIFO/STANDARD 红冲成本不变量，P1-MA2-024） | 中（FIFO/STANDARD 红冲不变量已修，覆盖 P1-MA2-024） |
| **承付释放不超余量** | 预算余量非负：`availableAmount = budget − actual − commitment ≥ 0`（三通道分离）；commit/release 后 commitment 通道余额 = Σ 未红冲 COMMITMENT 凭证一致 | `docs/design/finance/budget.md` §设计范式（line 18 三通道分离 `available = budgetBalance − actualBalance − commitmentBalance`）+ §承付（业务规则3 + 承付会计 A2 + `IErpFinBudgetCommitmentBiz` commit/release） | 中-高（采购承付 commit/release 3 接入点已落地 + 销售承付扩展） |

> **i18n locale 正确性（部分折入 Q3，owner doc = Q0 README §候选维度排除裁决 §5）**：i18n **locale 正确性**（日期/数字/货币格式化在不同 locale 下不变量成立）可部分折入 Q3 属性测试作为属性用例。本设计文档不列为 Phase 2 首批核心不变量（优先财务/库存强不变量），作为 successor（§7）。

### 1.4 关键风险：Nop 测试栈 JunitAutoTestCase + 快照录制与 jqwik 多迭代语义冲突（roadmap line 785 明示）

> **roadmap line 785 关键风险原文**：「Nop 测试栈 `JunitAutoTestCase` + `RECORDING`→`CHECKING` 快照录制与 jqwik 多迭代语义冲突——快照录制一次（RECORDING）但 jqwik 回放多次（每属性 100+ 次迭代），录制/回放不对称。Phase 1 须裁决属性测试是否绕过 `JunitAutoTestCase`（用纯 JUnit 5 + jqwik）及夹具重置策略。」

**冲突根因（实仓核验）**：Nop 测试栈 `JunitAutoTestCase` 的快照机制（`RECORDING`→`CHECKING`，见 sibling Q1/Q4 plan 引用的 nop-testing skill + `testing-strategy.md`）是**录制一次 / 回放校验**语义：首次运行录制输出快照（`RECORDING`），后续运行比对快照（`CHECKING`）。这与 jqwik 的**多迭代随机回放**语义根本冲突——

- **录制一次 vs 回放多次**：快照录制产生**一份**期望输出；jqwik 同一属性方法跑 100+ 次迭代，每次迭代输入不同→输出不同。若属性 test 继承 `JunitAutoTestCase` + 启用快照，第一次迭代的输出被录制为快照，第 2-100 次迭代因输入不同输出不同 → CHECKING 阶段全部比对失败（假失败）。
- **确定性 vs 随机性**：快照机制要求**确定性**（同输入同输出）；jqwik 是**随机性**生成（种子可固定但每次迭代输入不同）。二者哲学对立。

**结论**：属性测试**必须绕过** `JunitAutoTestCase` 的快照机制（不能让 jqwik 属性方法参与快照录制/校验）。§3 裁决具体绕过路径（纯 JUnit 5 + jqwik / 适配 / 混合）。

- 核验命令（2026-08-01 复核）：`rg -l "JunitAutoTestCase" --glob '*.java' module-finance/erp-fin-service/src/test/` → 命中 finance 域既有测试基类继承范式（确认 `JunitAutoTestCase` 是 Nop 测试栈基类，属性 test 若继承它则触发快照机制）。
- 引用源：roadmap line 785（关键风险声明）；Q0 README §复杂度分级 §Q3 行（平台依赖中——须裁决与 `JunitAutoTestCase` 快照冲突）；sibling Q6 `clock-test-infrastructure.md` §1.2（Nop 测试栈基类 + `AbstractFrozenClockExtension` 复用范式参照）。

## 2. 目标与非目标

### 2.1 目标（Phase 1 = 本文档；Phase 2 实现见 §6）

1. **裁决属性测试的技术选型**（框架 jqwik vs junit-quickcheck + 与 Nop 测试栈兼容路径 A/B/C + 夹具重置策略），给出候选、考虑的替代、残留风险三要素——满足 plan authoring guide §规则 9（Decision 项记录理由）。
2. **枚举并优先级排序 ERP 强不变量**（§4），引用 owner doc 真相源（§1.3），Phase 2 首批聚焦 3-5 个核心不变量。
3. **定义属性 test 设计模式**（§5）：随机操作序列生成器 + 每步不变量断言 + 收缩策略，为 Phase 2 实现 plan 提供可执行契约。
4. **裁决 CI 门控形态**（§7）：属性测试是否纳入 mandatory 回归层 + jqwik 迭代次数与 CI 稳定性权衡 + 与 `.github/workflows/compliance.yml` 集成。
5. **声明与 Q1 的协同接口**（§9）：消费 Q1 输出的盲区类清单（哪些类的存活变异体指向不变量违反），作为 Q3 Phase 2 优先覆盖目标。

### 2.2 非目标

- **不实现任何代码/ORM/CI 变更**——本文档仅产出设计。Phase 2 实现（jqwik 接入 + 3-5 个不变量属性 test）是独立后续 plan，须在本文档审查收敛后方可起草（MQ 文档先行工作流硬约束）。
- **不修改 `nop-entropy` 源码**——若 §3 裁决路径 A/C（应用层绕过/混合），其实施零平台改动；路径 B（适配）若被选中其实施属 Phase 2 + 须遵守 AGENTS.md nop-entropy 日志规则（但 §3 裁决否决路径 B）。
- **不追求全不变量穷举**——Q3 首轮聚焦 3-5 个核心不变量（§4 优先级排序）；其余不变量（多币种折算平衡 / 合并抵消归零 / 资产折旧残值非负 / i18n locale 正确性）作为 successor（§7）。
- **不重新推导 NOT FOUND 证据**——§1 引用 Q0 README + roadmap + owner doc 不变量定义，避免双真相源。
- **不重新定义 ERP 不变量**——§1.3 / §4 引用 owner doc 真相源（`posting.md` / `period-close.md` / `costing-methods.md` / `budget.md`），不重新推导不变量定义。
- **不动用 `nop-testing` skill 写测试**——本期纯设计文档；`nop-testing`（JunitAutoTestCase/快照）正是 Q3 须裁决的冲突对象，留待 Phase 2 实现 plan 加载。
- **不编写 Q2/Q5 设计**——同批独立 sibling plan。

## 3. 技术选型

> 本节裁决三轴：框架（§3.1）/ 与 Nop 测试栈兼容路径（§3.2）/ 夹具重置策略（§3.3）。每轴记录候选 + 优缺点 + 与现有基础设施冲突点。裁决（§3.4）记录候选、考虑的替代、残留风险（plan authoring guide §规则 9）。

### 3.1 框架选型：jqwik（候选首选）vs junit-quickcheck

#### 3.1.1 jqwik —— 候选首选

- **机制**：开源 JVM 属性测试框架，原生 JUnit 5 集成（`@Property` 注解 + `@Provide` Arbitraries 生成器）。每次属性方法默认 100 次迭代（可配置），每次用随机生成的输入运行；失败时内置**收缩（shrinking）**自动缩小到最小可复现失败输入。
- **优点**：
  - **原生 JUnit 5 集成**：`@Property` 是 JUnit 5 测试注解的扩展，与本项目 JUnit 5（surefire）测试栈天然契合，无须额外测试运行器。
  - **自动收缩（shrinking）**：jqwik 内置 integrated shrinking，失败用例自动收缩到最小（如 `BigDecimal("9999.99")` 收缩为 `BigDecimal("1")` 暴露边界 off-by-one），这是属性测试相对随机 fuzzing 的核心价值。
  - **Arbitraries 生成器丰富**：内置 `BigDecimal` / `LocalDate` / 列表 / 有界范围 / 组合（`Arbitraries.combine`）/ 频率分布（`frequency`）等，可直接表达 ERP 金额/日期/操作序列的随机生成。
  - **活跃维护 + JDK 21 兼容**：jqwik 1.8+ 支持 JDK 21（本项目 CI `setup-java java-version: '21'`），社区活跃度高于 junit-quickcheck。
  - **种子可固定（reproducibility）**：`@Property(seed=...)` 固定种子使失败用例可复现——CI 稳定性的关键（§7）。
- **缺点 / 风险**：
  - **与 Nop 测试栈快照机制冲突**（§1.4）：jqwik 多迭代随机回放与 `JunitAutoTestCase` 录制一次/校验语义对立，须 §3.2 裁决绕过路径。
  - **收缩质量依赖生成器设计**：自定义操作序列生成器的收缩效果取决于实现质量（jqwik 对内建类型收缩好，自定义复杂对象收缩可能不够精炼）。
  - **多迭代耗时**：每属性 100+ 次迭代，若每次迭代触发 DB（localDb）则耗时显著（§3.3 夹具重置 + §7 CI 迭代次数权衡）。

#### 3.1.2 考虑的替代框架

- **junit-quickcheck**：评估后否决作为首选。(1) 社区活跃度低于 jqwik（GitHub star / release 频率 / 文档完整度）；(2) junit-quickcheck 基于 JUnit 4 theorists 或独立 runner，与本项目 JUnit 5 原生集成不如 jqwik 的 `@Property`；(3) junit-quickcheck 的收缩（shrinking）支持不如 jqwik integrated shrinking 完善（部分场景需手写）。保留为 successor 候选（触发：jqwik 收缩质量在特定不变量上不可接受时，评估 junit-quickcheck 替换）。
- **手写随机 fuzzing（`Math.random` + 循环）**：否决。(1) 无自动收缩——失败用例是随机大数，难以缩小到最小可复现；(2) 无生成器抽象——每次重复手写随机逻辑；(3) 不可规模化到 3-5 个不变量 × 每属性 100+ 迭代。
- **仅用黄金路径单测（维持现状）**：否决——§1.2 已论证黄金路径具体断言无法证明不变量在任意操作序列下恒成立，这是结构性盲区。

### 3.2 与 Nop 测试栈兼容路径（绕过 vs 适配 vs 混合）—— roadmap line 785 关键风险裁决

> roadmap line 785 明示关键风险：属性测试与 `JunitAutoTestCase` + `RECORDING`/`CHECKING` 快照机制语义冲突（§1.4）。本节裁决三路径 A/B/C。

#### 3.2.1 路径 A —— 绕过 `JunitAutoTestCase`，用纯 JUnit 5 + jqwik

**机制**：属性 test 类**不继承** `JunitAutoTestCase`，而是普通 JUnit 5 测试类（`@Property` 注解 + `@Provide` 生成器 + 纯 JUnit 5 `@BeforeEach`/`@AfterEach` 夹具重置）。属性 test 独立夹具重置，每迭代重建测试数据，完全不触发快照录制/校验。

**优点**：
- **彻底规避快照语义冲突**（§1.4 根因）：不继承 `JunitAutoTestCase` → 不触发 `RECORDING`/`CHECKING` → 无录制一次/回放多次的假失败。
- **应用层内闭环**：纯 JUnit 5 + jqwik 全部在 `src/test`，零 nop-entropy 改动，无跨仓库/升级耦合。
- **夹具重置自由度高**：`@BeforeEach`（per-trial，jqwik 支持 `@Property` 的 per-trial lifecycle）每迭代重建测试数据，确保迭代间状态隔离。

**缺点 / 风险**：
- **失去平台 localDb 便利**：`JunitAutoTestCase` 提供 `@NopTestConfig(localDb=true)` 的 H2 内存库自动启动 + ORM 初始化 + GraphQL 引擎。路径 A 须自行启动测试基础设施（或经更轻量的 `@NopTestConfig` 但不继承快照基类——须 Phase 2 核验是否可拆分 localDb 能力与快照能力）。
- **轻量测试 vs 真实链路的保真度权衡**：若属性 test 仅测纯函数（如 `VoucherFact.balanceTotals` 纯算术），无需 localDb，路径 A 完美；若须触发端到端过账链路（localDb + Provider + 凭证引擎），路径 A 须自行接线，保真度可能低于 `JunitAutoTestCase`。

#### 3.2.2 路径 B —— 适配 `JunitAutoTestCase`（jqwik 嵌套于 Nop 测试基类内，禁用快照）

**机制**：属性 test 类继承 `JunitAutoTestCase`（复用 localDb + ORM + GraphQL），但禁用快照录制（如设置 `@NopTestConfig` 的快照模式为 DISABLE 或仅 RECORDING 不 CHECKING），属性方法用 `@Property`。

**优点**：
- 复用平台 localDb / ORM / GraphQL 基础设施，端到端链路保真度高。

**缺点 / 风险**：
- **快照语义冲突未根治**（§1.4 根因）：即使"禁用快照"，jqwik 多迭代与 `JunitAutoTestCase` 的测试生命周期（`@BeforeEach` class-level vs jqwik per-trial）仍可能冲突。禁用快照的配置点须 Phase 2 核验是否存在（`JunitAutoTestCase` 是否支持 `RECORDING=DISABLE` 模式）——若无，路径 B 须改 `JunitAutoTestCase`（触及平台）或 fork，违反 AGENTS.md「应用层闭环优先，不动平台」。
- **跨仓库耦合**：若 `JunitAutoTestCase` 不支持禁用快照，路径 B 须 nop-entropy PR + 独立发布节奏对齐，Phase 2 交付被阻塞（与 Q6 路径 A 同源的否决理由）。
- **`JunitAutoTestCase` 与 jqwik `@Property` lifecycle 兼容性未知**：`JunitAutoTestCase` 可能有 `@BeforeEach`/`@AfterEach` 的 class-level 初始化，与 jqwik per-trial lifecycle 冲突——须 Phase 2 实测。

#### 3.2.3 路径 C —— 混合：纯函数不变量用纯 jqwik（路径 A），端到端不变量用 `JunitAutoTestCase` 单次固定输入

**机制**：按不变量的**触发方式**分两类，分别用不同测试范式：
- **类一：纯函数/纯算术不变量**（如 `VoucherFact.balanceTotals` 借贷平衡、成本层累加算术）——用纯 jqwik（路径 A），随机生成金额/行数/操作序列，断言纯算术不变量，无需 localDb。
- **类二：端到端不变量**（如过账后凭证借贷平衡、结账后余额归零——须触发 localDb + Provider + 凭证引擎）——用 `JunitAutoTestCase` 单次/少量固定输入（传统黄金路径测试），**不用 jqwik 多迭代**（规避快照冲突）。

**优点**：
- **按不变量性质分工**：纯算术不变量（占多数）用 jqwik 获得随机化收益；端到端不变量用既有黄金路径范式，规避快照冲突。
- **应用层闭环**：纯 jqwik 部分（类一）零平台改动；`JunitAutoTestCase` 部分（类二）是既有范式，无新风险。
- **保真度与随机化的平衡**：类一获得 100+ 次随机迭代的属性验证；类二保留端到端真实链路保真度。

**缺点 / 风险**：
- **两套夹具维护成本**：类一纯 jqwik 夹具 + 类二 `JunitAutoTestCase` 夹具，Phase 2 须维护两套。
- **类二失去随机化**：端到端不变量仍是黄金路径单次输入，无法证明"任意操作序列下恒成立"——但端到端 jqwik 化须先解决路径 A/B 的 localDb 接线，Phase 2 首批可接受此限制（successor 推进端到端 jqwik 化）。
- **类划分边界须裁决**：哪些不变量算"纯算术"（类一）vs "端到端"（类二）须 §4 / §6 明确，避免边界模糊。

### 3.3 夹具重置策略（与路径选择配套）

> jqwik 每属性 100+ 次迭代，迭代间状态隔离是正确性前提。本节裁决夹具重置策略。

- **策略 F1：per-trial `@BeforeEach` 每迭代重建**（jqwik 支持 `@Property` 的 per-trial JUnit 5 lifecycle）——每迭代 `@BeforeEach` 清空 + 重建测试数据（成本层/余额/凭证），确保迭代间状态隔离。适用于纯 jqwik（路径 A/C 类一）。缺点：每迭代重建开销大（若涉及 localDb 则 100+ 次 localDb 重建不可接受）。
- **策略 F2：纯内存状态重置（不触 DB）**——属性 test 在内存中维护可变状态（如 `Map<materialId, List<CostLayer>>`），每迭代 reset 内存状态，断言不变量。适用于纯算术不变量（路径 C 类一），不触 DB 故 100+ 迭代无开销。缺点：不验证 DB 持久化层的不变量（须假设 ORM 写入忠实）。
- **策略 F3：种子固定的确定性序列 + 状态快照对比**——固定 jqwik 种子，每迭代用确定性输入运行，对比状态快照。适用于回归（种子固定可复现）。缺点：种子固定削弱随机发现能力（每次跑同一种子）。

### 3.4 裁决（Decision）

> 决策输入：§1 现状（零属性测试 + 黄金路径盲区 + 5 个强不变量 + Nop 快照冲突关键风险）+ §3.1-3.3 候选优缺点 + Q0 README §复杂度分级（Q3 平台依赖中——须裁决与 `JunitAutoTestCase` 快照冲突）+ AGENTS.md「应用层闭环优先，不动平台」+ sibling Q6 路径 C（应用层 thread-local clock）同源的应用层闭环先例。

**裁决：**

1. **框架：选 jqwik**（§3.1.1）。junit-quickcheck / 手写 fuzzing / 仅黄金路径否决（§3.1.2）。
2. **与 Nop 测试栈兼容路径：选路径 C（混合）**——纯函数/纯算术不变量用纯 jqwik（路径 A 子集，策略 F2 纯内存状态重置）；端到端不变量用 `JunitAutoTestCase` 单次固定输入（既有黄金路径范式，Phase 2 首批不 jqwik 化）。路径 A（全绕过）+ 路径 B（适配）否决作为本期主路径。
3. **夹具重置策略：选策略 F2（纯内存状态重置，不触 DB）**配套路径 C 类一（纯 jqwik）。策略 F1（per-trial 重建）否决作为主路径（每迭代 localDb 重建不可接受）；策略 F3（种子固定）作为 CI 稳定性保障手段（§7），非主夹具策略。

**裁决理由：**

1. **jqwik 是本项目约束下的最优选择**：原生 JUnit 5 集成 + integrated shrinking + Arbitraries 丰富 + JDK 21 兼容 + 种子可固定。junit-quickcheck 社区活跃度低且 JUnit 5 集成弱；手写 fuzzing 无收缩不可规模化；仅黄金路径无法证明不变量恒成立。
2. **路径 C 混合规避快照冲突 + 保留随机化收益 + 应用层闭环**：
   - **路径 C 类一（纯 jqwik）**直接绕过 `JunitAutoTestCase` 快照机制（不继承 → 不触发录制/校验），彻底规避 §1.4 根因，且纯内存状态重置（策略 F2）使 100+ 迭代无 DB 开销——这是借贷平衡、成本层累加等**纯算术不变量**的理想验证范式。
   - **路径 C 类二（`JunitAutoTestCase` 单次）**保留端到端真实链路保真度（过账/结账经 localDb + Provider + 凭证引擎），用既有黄金路径范式，规避快照冲突（单次输入无录制/回放不对称）。Phase 2 首批可接受类二无随机化（端到端 jqwik 化须先解决路径 A/B 的 localDb 接线，列为 successor §7）。
   - **应用层闭环**：路径 C 两类均在应用层（`src/test`），零 nop-entropy 改动（路径 C 类一纯 jqwik 无平台依赖；类二是既有 `JunitAutoTestCase` 范式）。路径 B 须改 `JunitAutoTestCase` 支持禁用快照（触及平台），违反 AGENTS.md，否决。
3. **策略 F2 纯内存状态重置使随机化可行**：若每迭代触 localDb（策略 F1），100+ 迭代的 localDb 重建开销使 CI 不可接受。策略 F2 在内存中维护状态（如成本层队列内存模型），每迭代 reset，断言纯算术不变量——开销极低，随机化可行。代价是不验证 DB 持久化层（须假设 ORM 写入忠实，由既有 `JunitAutoTestCase` 端到端测试覆盖 DB 层）。

**考虑的替代（记录为何否决）：**

- **路径 A（全绕过 `JunitAutoTestCase`，端到端也用纯 jqwik）**：否决作为本期主路径——端到端不变量（过账后凭证借贷平衡）须 localDb + Provider + 凭证引擎，纯 jqwik 自行接线保真度低且工作量大。保留为 successor（触发：路径 C 类二端到端不变量需随机化时，评估纯 jqwik 端到端接线）。
- **路径 B（适配 `JunitAutoTestCase` 禁用快照）**：否决——快照语义冲突未根治 + 须改平台基类（跨仓库耦合），违反 AGENTS.md「应用层闭环优先」。保留为 successor 候选（触发：路径 C 两套夹具维护成本过高，或 nop-entropy 未来原生支持禁用快照时）。
- **策略 F1（per-trial localDb 重建）**：否决——100+ 迭代 × localDb 重建开销使 CI 不可接受（估算每迭代 localDb 启动秒级 × 100 = 分钟级/属性）。
- **jqwik 全域首跑（不限 3-5 核心不变量）**：否决——5+ 不变量 × 100+ 迭代 × 生成器设计，Phase 2 首跑规模爆炸，无分类工作流支撑。3-5 核心不变量优先沉淀生成器/断言范式后扩展。

**残留风险：**

- **R1（路径 C 类一边界裁决模糊）**：哪些不变量算"纯算术"（类一 jqwik）vs "端到端"（类二 `JunitAutoTestCase`）须 §4 / §6 明确。借贷平衡可拆为「纯算术（VoucherFact.balanceTotals）」+「端到端（过账后凭证）」两层——类一覆盖纯算术层，类二覆盖端到端层。Phase 2 须给出每不变量的类划分。
- **R2（策略 F2 不验证 DB 持久化层）**：纯内存状态重置假设 ORM 写入忠实，不验证 DB 层不变量（如 `ErpInvCostLayer` 实际落库后的累加）。须由既有 `JunitAutoTestCase` 端到端测试覆盖 DB 层（双层互补：jqwik 覆盖算术层，单测覆盖 DB 层）。
- **R3（jqwik 收缩质量依赖生成器设计）**：自定义操作序列生成器（如随机出库序列）的收缩效果取决于实现。Phase 2 须对失败用例抽样复核收缩质量（是否收缩到最小可复现）。
- **R4（端到端不变量随机化 successor 工作量）**：路径 C 类二（端到端）Phase 2 首批无随机化，端到端 jqwik 化须先解决路径 A 的 localDb 接线，工作量未估。successor（§7）。
- **R5（与 Q6 thread-local clock 并行隔离协同）**：jqwik 多迭代若触 `CoreMetrics.today()`（如 voucherDate 默认 `CoreMetrics.today()`），须与 Q6 thread-local clock 协同，避免迭代间时钟污染。路径 C 类一纯内存若不触 CoreMetrics 则无冲突；类二 `JunitAutoTestCase` 复用 Q6 frozen clock 子类。§8 验收 5 闭环核验。

## 4. 不变量枚举与优先级

> 本节**引用** §1.3 owner doc 真相源已定义的不变量，按「不变量强度 × 实现复杂度 × owner doc 成熟度」三轴排序 Phase 2 首批 3-5 个核心不变量。

### 4.1 优先级排序轴定义

- **不变量强度**：违反后果的严重性（数据腐败 / 财务失真 / 跨期污染）。强不变量（违反即财务错误）优先。
- **实现复杂度**：属性 test 实现的复杂度（生成器设计难度 / 断言难度 / 是否需端到端链路）。低复杂度优先（Phase 2 首跑降噪）。
- **owner doc 成熟度**：不变量定义在 owner doc 的稳定程度（是否已落地 + 有无歧义 + 真相源清晰）。高成熟度优先（避免对模糊不变量做属性测试）。

### 4.2 Phase 2 首批核心不变量裁决（3-5 个）

| 优先级 | 不变量 | 真相源 | 强度 | 实现复杂度 | owner doc 成熟度 | 类划分（路径 C） | 裁决理由 |
|--------|--------|--------|------|-----------|-----------------|-----------------|----------|
| **P1** | **借贷平衡**（纯算术层：`Σ debit == Σ credit`） | `posting.md`（凭证模板 + `VoucherFact.balanceTotals`） | 极高（违反即财务失真） | 低（随机金额/行数，纯算术断言） | 高（引擎内置 `balanceTotals` 校验） | 类一（纯 jqwik） | 业财一体核心不变量；纯算术层易测（随机 BigDecimal 金额 + 行数，断言 `Σ debit == Σ credit`）；引擎已有 `balanceTotals` 作参照；Q1 finance 顶盲区 `ErpFinPostingProcessor` 直接受益。**首选** |
| **P2** | **成本层累加 = 余额表**（layer-based costMethod） | `costing-methods.md`（FIFO 队列 + `BookingContext`） | 高（违反即库存成本腐败） | 中（须模拟 FIFO 出库序列多层消耗） | 中（FIFO/MOVING_AVERAGE 已落地） | 类一（纯 jqwik 内存成本层模型） | 库存核算核心不变量；可在内存维护 `List<CostLayer>` 模型，随机出库序列断言 `Σ layer.remainingQuantity × unitCost == balance.totalCost`；Q1 inv 域盲区协同；scope 限 layer-based costMethod（FIFO），MOVING_AVERAGE 无 cost layer 不适用 |
| **P3** | **承付释放不超余量** | `budget.md` §设计范式（line 18 三通道）+ §承付（commit/release + 部分开票/退货） | 高（违反即预算超支） | 中-高（须模拟 commit/release/部分开票序列） | 中-高（采购承付 3 接入点已落地） | 类一（纯 jqwik 内存预算模型） | 预算控制核心；可在内存维护 budget/actual/commitment 三通道，随机 commit/release/部分开票序列断言 `available = budget − actual − commitment ≥ 0`（余量非负，对齐 budget.md:18 三通道）；commit/release 后 commitment 通道余额与 Σ 未红冲 COMMITMENT 凭证一致 |
| **P4**（候选） | **期间结账后余额归零**（损益类 TEMPORARY 账户） | `period-close.md`（步骤5 损益结转 + 年度本年利润清零） | 高（违反即跨期污染） | 高（须模拟多期间 + 结转 + 反结账序列） | 中-高（月度/年度结账已落地） | 类二（`JunitAutoTestCase` 端到端，首批无随机化） | 期末结账核心；端到端须 localDb 触发结转凭证，Phase 2 首批用 `JunitAutoTestCase` 单次固定输入（路径 C 类二）；随机化（多期间序列）列为 successor |
| **P5**（候选） | **costMethod 切换前后总成本不变**（STANDARD_REVALUATION 红冲） | `costing-methods.md` line 74（P1-MA2-024） | 中-高（违反即重估后成本失真） | 高（须模拟 STANDARD 重估 + 红冲跨重估） | 中（P1-MA2-024 已修） | 类一（纯 jqwik 内存模型） | STANDARD 成本法重估不变量；P1-MA2-024 已修但无随机化回归；Phase 2 视前三优先级完成情况决定是否纳入首批 |

**Phase 2 首批裁决：P1 + P2 + P3 为首批必做（3 个核心不变量，均为路径 C 类一纯 jqwik），P4/P5 为首批候选（视 Phase 2 工作量决定纳入或 successor）。**

- **P1-P3 均为路径 C 类一（纯 jqwik + 策略 F2 纯内存）**：低实现复杂度（不触 DB，无快照冲突），高随机化收益（100+ 迭代攻击纯算术不变量），是 Phase 2 首跑的理想起点。
- **P4 端到端（路径 C 类二）首批无随机化**：期间结账须 localDb，Phase 2 首批用 `JunitAutoTestCase` 单次固定输入（既有范式），随机化（多期间序列）列为 successor。
- **P5 视前三完成情况**：STANDARD 重估红冲不变量实现复杂度高（须模拟重估时点 + 红冲跨重估），首批可选。

### 4.3 不变量与 Q1 盲区类的协同（Q1↔Q3）

> Q1↔Q3 协同（Q0 README §实施顺序裁决 line 153：「Q3 排在 Q1/Q4 后可复用其测试基础设施决策 + 盲区类清单」）。Q1 Phase 2 已产出盲区类清单（`mutation-baseline.md`）。

> **重要边界——published baseline 是 posting-filtered 视图**：Q1 `mutation-baseline.md` §4.2 的盲区类清单经 **posting dispatcher/Processor 过滤**（为 Q4 消费裁剪，line 77 明示 scope = 「过账 dispatcher/Processor 类清单」）。该 published 视图**直接可消费**的仅限过账类；非过账的 costing/commitment 算术类（如 `FifoCostingStrategy`/`StockMoveBookkeeper`/`CommitmentVoucherGenerator`）的存活变异体存于原始 `mutations.xml` + `classify_mutations.py`（baseline line 79 明示），**不在 published baseline**。故协同消费须区分两视图（§10.2 给出 grep 命令定位原始视图）。

- **P1 借贷平衡**：published baseline 直接命中 finance 顶盲区 `ErpFinPostingProcessor`（92 存活变异体，baseline line 84）——属性 test 随机攻击借贷平衡算术（`balanceTotals`），可暴露该过账 Processor 的算术盲区。**直接可消费**。
- **P2 成本层累加**：published baseline 含 inv 域过账类 `ErpInvCostingReclosePeriodCostsProcessor`（69，baseline line 109）+ `InvPostingDispatcher`（17，line 113）；但 FIFO 算术核心 `FifoCostingStrategy`/`StockMoveBookkeeper`（非过账 dispatcher）的存活变异体须查原始 `mutations.xml`（非 published baseline）。**部分直接可消费 + 部分须原始视图**。
- **P3 承付释放**：`CommitmentVoucherGenerator`（非过账 dispatcher）的存活变异体须查原始 `mutations.xml`（非 published baseline）。**须原始视图**。
- **§10 协同接口**声明 Q3 消费 Q1 盲区类清单的具体方式 + 原始视图定位命令。

## 5. 属性 test 设计模式

> 本节定义属性 test 的可执行设计模式，为 Phase 2 实现 plan 提供契约。核心要素：随机操作序列生成器 + 每步不变量断言 + 收缩策略。

### 5.1 模式骨架（路径 C 类一纯 jqwik）

> **保真度硬约束（faithfulness，R2 补强）**：`@Property` 方法**必须**调用或交叉校验**生产纯函数算术**，而非仅断言一个测试侧并行 reimplementation（否则 P1 可能成为恒等式「自证平衡」、P2 内存模型可能正确而生产 `FifoCostingStrategy` 有 bug 却不被发现——model/production drift）。具体保真度机制：
> - **P1 借贷平衡**：`@Property` 方法直接调用生产 `ErpFinPostingProcessor.balanceTotals(List<VoucherFact>, ...)`（`:709`，已核实纯算术）+ `assertBalanced(totalDebit, totalCredit, ...)`（`:723`），断言其结果——而非测试侧自写 `sumDebit/sumCredit`。若 `balanceTotals` 经 DI 难以独立实例化，Phase 2 抽取其累加算术为可独立调用的纯函数（或经轻量 `new ErpFinPostingProcessor()` + 只设依赖字段）。
> - **P2 成本层累加**：内存 `CostLayerModel` 须**交叉校验**生产 `FifoCostingStrategy.onIncoming/onOutgoing`（已核实存在）——在共享 golden 输入上，内存模型与生产策略产出一致（model 跟随 production，非独立实现）。每属性迭代可调生产策略记账，断言 layer 累加。
> - **保真度 vs tautology 自检**：Phase 2 每属性 test 须自检「若故意在生产算术中注入一个变异（如 `i++`→`i--`），本属性 test 是否能发现」——若不能，说明属性 test 是 tautology（仅测测试侧实现），须改为调生产算术。

```
@Property(tries = 100, shrinking = BOUNDED)  // jqwik 每属性 100 次迭代，种子可固定 @Property(seed=...)
void debitCreditBalanceHoldsForRandomAmounts(
    @ForAll("validAmounts") BigDecimal amount,        // @Provide 生成器：合法金额（正数，scale=4）
    @ForAll("randomLineCount") int lineCount          // @Provide 生成器：行数（1-10）
) {
    // 策略 F2 纯内存状态：每迭代 reset 内存状态（jqwik per-trial lifecycle 自动隔离）
    // 1. 构造随机凭证（amount × lineCount 行）
    List<VoucherFact> facts = buildRandomVoucher(amount, lineCount);
    // 2. 调用【生产】balanceTotals 算术（纯函数，不触 DB）—— 非测试侧自写累加
    BigDecimal[] totals = productionProcessor.balanceTotals(facts, ...);  // [totalDebit, totalCredit]
    // 3. 每步不变量断言（对齐生产 assertBalanced 语义）
    assertEquals(0, totals[0].compareTo(totals[1]));   // Σ debit == Σ credit
}

@Provide                                       // 自定义 Arbitraries 生成器
Arbitrary<BigDecimal> validAmounts() {
    return Arbitraries.bigDecimals(BigDecimal.ZERO, MAX_AMOUNT)
        .filter(a -> a.scale() <= 4)            // 过滤合法精度
        .edgeCases(edge -> edge.include(BigDecimal.ZERO, BigDecimal.ONE)); // 边界用例
}
```

### 5.2 随机操作序列生成器（@Provide Arbitraries）

> 对于不变量涉及"操作序列"的场景（如成本层累加须随机出库序列、承付须随机 commit/release/开票序列），生成器须生成合法操作序列。

**成本层累加生成器示例（P2 FIFO）**：

```
@Provide
Arbitrary<List<MoveOp>> fifoMoveSequence() {
    return Arbitraries.integers(1, 20).flatMap(len ->  // 序列长度 1-20
        Arbitraries.sequence(
            Arbitraries.constant(OpType.INCOMING),      // 首操作必为入库
            Arbitraries.frequency(
                Tuple.of(5, Arbitraries.constant(OpType.INCOMING)),  // 60% 入库
                Tuple.of(3, Arbitraries.constant(OpType.OUTGOING))   // 40% 出库（须 remaining>0）
            )
        ).list().ofSize(len)
    );
}
```

**设计要点**：
- **合法性约束（状态依赖）**：生成器须过滤非法序列（如出库时 remaining≤0 的负库存场景——`erp-inv.allow-negative-stock` 默认 false，`ErpInvConstants.CONFIG_ALLOW_NEGATIVE_STOCK`，生成器须保证出库不超余量，否则属性 test 因非法输入假失败）。**注意**：OUTGOING 合法性依赖**累计状态**（remaining 是前序操作的函数），stateless `Arbitraries.frequency`/`sequence` 无法在生成期强制。Phase 2 须用 jqwik **`ActionSequence` / stateful arbitrary** 模式（`Arbitraries.sequences(Action)`，每 step 的 transformer 在当前状态上判定 OUTGOING 可行性，infeasible 则生成 INCOMING 或跳过）；或用 post-hoc `.filter(...)` 过滤（长序列时昂贵，仅作兜底）。上方骨架为示意，Phase 2 须落地 stateful 模式。
- **边界用例**：`edgeCases` 显式包含边界（空序列、单次入库、单次出库、刚好消耗完）。
- **收缩友好**：生成器用 jqwik 内建组合（`flatMap`/`sequence`/`list`/`ActionSequence`）而非完全自定义对象，使收缩生效（R3）。

### 5.3 每步不变量断言

> 对于操作序列类属性，须在**每步操作后**断言不变量，而非仅末尾断言（暴露中间状态违反）。**保真度（§5.1）**：每步记账调用**生产** `FifoCostingStrategy.onIncoming/onOutgoing`（或交叉校验内存模型与生产策略一致），内存模型跟随生产非独立实现。

```
@Property(tries = 100)
void costLayerAccumulationHoldsEveryStep(
    @ForAll("fifoMoveSequence") List<MoveOp> sequence
) {
    FifoCostingStrategy strategy = new FifoCostingStrategy();  // 生产算术（或经 DI）
    CostLayerModel model = new CostLayerModel();               // 策略 F2 纯内存模型（跟随生产）
    for (MoveOp op : sequence) {
        strategy.applyTo(model, op);                           // 生产策略记账到内存模型
        // 每步断言不变量
        BigDecimal layerSum = model.layers().stream()
            .map(l -> l.remainingQuantity.multiply(l.unitCost))
            .reduce(ZERO, BigDecimal::add);
        assertEquals(0, layerSum.compareTo(model.balance.totalCost)); // Σ layer == balance
    }
}
```

### 5.4 收缩策略（shrinking）

jqwik integrated shrinking 自动缩小失败用例。设计要点：

- **失败用例最小化**：若某 20 步序列的第 7 步触发不变量违反，jqwik 收缩到最小可复现子序列（如 3 步），暴露精确边界。
- **种子固定复现**：失败时 jqwik 输出种子（如 `seed=12345`），`@Property(seed=12345)` 固定后可复现——CI 稳定性关键（§7）。
- **收缩质量复核**：Phase 2 须对失败用例抽样复核收缩是否到最小（R3）——若收缩不精炼（如仍 20 步），评估生成器重构或 junit-quickcheck 替换（§3.1.2 successor）。

### 5.5 与 Q1 盲区类清单的消费方式

> Q3 属性 test 目标选择可由 Q1 盲区类清单指导（§10）。Q1 published `mutation-baseline.md` 是 **posting-filtered** 视图（§4.3 边界）：finance 顶盲区 `ErpFinPostingProcessor`（92 存活）**直接命中** published baseline；非过账 costing/commitment 算术类（`FifoCostingStrategy`/`StockMoveBookkeeper`/`CommitmentVoucherGenerator`）须查原始 `mutations.xml`（§10.2 定位命令）。

- **借贷平衡（P1）**属性 test 直接攻击 published baseline finance 顶盲区 `ErpFinPostingProcessor` 的算术（`balanceTotals`）——若 Q1 存活变异体指向金额累加算术，属性 test 随机金额应杀死此类变异（暴露 Q1 盲区）。
- **成本层累加（P2）**属性 test 攻击 inv 域 costing 算术——`ErpInvCostingReclosePeriodCostsProcessor`（69）+ `InvPostingDispatcher`（17）在 published baseline；`FifoCostingStrategy`/`StockMoveBookkeeper` 须原始 `mutations.xml`（§10.2）。
- **协同价值双向**：Q3 属性 test 杀死 Q1 存活变异体 → Q1 mutation score 提升；Q1 盲区类清单指导 Q3 目标 → Q3 覆盖高价值不变量。

## 6. 实施步骤（Phase 2 实现 plan 的范围契约）

> 本节为 Phase 2 实现 plan 提供步骤骨架与边界声明。Phase 2 plan 起草时（加载 `nop-testing` skill）以本节为实施契约，可细化但不得偏离已裁决的 jqwik + 路径 C + 策略 F2 + 3-5 核心不变量范围。

### 6.1 jqwik 依赖接入（test scope）

1. 在目标域 service 模块 pom（或经根 pom profile）声明 jqwik test scope 依赖：
   - `<dependency><groupId>net.jqwik</groupId><artifactId>jqwik</artifactId><version>1.8.x（JDK 21 兼容版本，Phase 2 锁定具体版本）</version><scope>test</scope></dependency>`
   - **接入位置裁决（Phase 2）**：候选 P1 per-module（finance/inv service pom 各声明）vs P2 根 pom + profile 激活（对齐 Q1 pitest profile 范式）vs P3 仅 finance/inv service pom（属性 test 首批聚焦此二域）。Phase 2 视 jqwik 依赖传递性裁决（jqwik 是 test scope，不污染生产 classpath）。
2. 复核 jqwik 与 JUnit 5 / surefire 版本兼容性（jqwik 1.8+ 须 JUnit Platform 1.8+，本项目 CI `setup-java 21` 应满足）。

### 6.2 核心不变量属性 test 落地清单（§4.2 P1-P3 首批必做）

| 不变量 | 属性 test 类（建议命名） | 生成器 | 断言 | 类划分 |
|--------|--------------------------|--------|------|--------|
| **借贷平衡（P1）** | `PropertyErpFinDebitCreditBalance` | 随机金额（BigDecimal 正数 scale≤4）+ 行数（1-10）+ 科目方向 | `Σ debit == Σ credit`（纯算术） | 类一纯 jqwik |
| **成本层累加 = 余额表（P2）** | `PropertyErpInvCostLayerAccumulation` | 随机 FIFO 操作序列（入库/出库，合法性约束 remaining>0） | 每步 `Σ layer.remaining × unitCost == balance.totalCost` | 类一纯 jqwik 内存模型 |
| **承付释放不超余量（P3）** | `PropertyErpFinBudgetCommitmentRelease` | 随机 commit/release/部分开票/退货序列 | `available = budget − actual − commitment ≥ 0`（余量非负，三通道分离）+ commitment 通道余额与 Σ 未红冲 COMMITMENT 凭证一致 | 类一纯 jqwik 内存模型 |

- **P4/P5 候选**（视 Phase 2 工作量）：P4 期间结账余额归零（类二 `JunitAutoTestCase` 单次）；P5 STANDARD 重估红冲总成本不变（类一纯 jqwik）。
- **属性 test 类位置**：各域 `erp-*-service/src/test/.../property/`（新建 property 子包，与既有 `posting/` 等测试包隔离，明确标识属性 test）。

### 6.3 夹具重置策略落地（策略 F2 纯内存）

- **纯内存状态模型**：每属性 test 维护内存状态对象（如 `CostLayerModel` / `BudgetCommitmentModel`），每迭代（jqwik per-trial）由 `@Provide` 生成器产生新输入，内存模型 reset（new 实例），断言不变量。
- **不触 localDb**：类一纯 jqwik 属性 test 不依赖 `@NopTestConfig(localDb=true)`，纯 JUnit 5 + jqwik，避免快照机制（§3.2 路径 C 类一）。
- **双层互补**：jqwik（算术层随机化）+ 既有 `JunitAutoTestCase`（端到端 DB 层单次）双层覆盖同一不变量——jqwik 证明算术恒成立，单测证明 DB 持久化忠实（R2）。

### 6.4 跨 nop-entropy 改造边界声明

| 改动面 | 位置 | Q3 Phase 2 是否触碰 | 说明 |
|--------|------|----------------------|------|
| jqwik 依赖声明 | 目标域 service pom（应用层） | **是** | test scope，不污染生产 classpath |
| 属性 test 类 | 各域 `erp-*-service/src/test/.../property/`（应用层） | **是** | 新建属性 test 类（路径 C 类一纯 jqwik） |
| 内存状态模型 | 各域 `erp-*-service/src/test/.../property/`（应用层） | **是** | 新建纯内存模型（CostLayerModel 等） |
| 生产业务代码 | `module-*/erp-*-service/src/main` | **否** | 属性 test 经内存模型或 I*Biz 只读调用，不改生产代码 |
| nop-entropy 平台源码 | `../nop-entropy/` | **否** | 路径 C 零平台改动（类一纯 jqwik + 类二既有 JunitAutoTestCase） |
| `JunitAutoTestCase` 基类 | `../nop-entropy/` 或 `module-common-test` | **否** | 路径 C 类一绕过（不继承）；类二复用既有（不改）。路径 B 须改，已否决 |
| CI workflow | `.github/workflows/` | **是**（§7 裁决若纳入回归层） | 新建 property-test job 或依赖 maven.yml 自动包含 |
| ORM / model | `<domain>/model/*.orm.xml` | **否** | 零 ORM 变更 |

> 边界裁决：Q3 Phase 2 **零 nop-entropy 改动 + 零 ORM 变更**，全部在应用层测试代码 + 可选 CI workflow。Phase 2 无须在 `nop-entropy/ai-dev/logs/` 记日志。

### 6.5 Phase 2 执行顺序建议

1. 6.1 step 1-2（jqwik 依赖接入 + JUnit 5 兼容复核）——基础设施，先行
2. 6.2 P1 借贷平衡属性 test（最简单，纯算术，验证 jqwik 范式可行）
3. 6.2 P2 成本层累加属性 test（须内存 FIFO 模型，验证操作序列生成器可行）
4. 6.2 P3 承付释放属性 test（须内存预算模型，验证多操作序列）
5. §5.4 收缩质量复核（失败用例抽样）
6. §8 全量验收 + §9 Q1 协同产物消费
7. §7 CI 门控接线（若 §7 裁决纳入）
8. P4/P5 候选决定（视前三完成情况）

## 7. 验收判据（Phase 2 closure gate 契约）

> 每条须在 Phase 2 closure audit 时由独立子代理在 live repo 核验。每条给出具体可执行机制。

1. **jqwik 依赖接入（test scope）**：目标域 service pom 声明 jqwik test scope 依赖。**可执行核验**：`rg "jqwik" module-{finance,inventory}/erp-*-service/pom.xml`（或根 pom profile）命中 jqwik 依赖声明 + scope=test。
2. **3 个核心不变量属性 test 落地（P1-P3）**：借贷平衡 / 成本层累加 / 承付释放 各 ≥1 属性 test 类，每类 ≥1 `@Property` 方法（默认 100 次迭代）。**可执行核验**：`rg "@Property" module-{finance,inventory}/erp-*-service/src/test/.../property/` 命中 ≥3 属性方法。
3. **核心不变量属性 test 在 ≥100 轮随机输入下恒成立**：每 `@Property(tries=100)`（或默认 100）跑 100 次迭代，不变量恒成立（无失败）。**可执行核验**：`mvn test -Dtest='Property*'` 属性 test 全绿（100 迭代无失败）。
4. **收缩暴露的失败用例可复现**：若属性 test 发现失败（Phase 2 首跑或后续变异），jqwik 输出种子，`@Property(seed=...)` 固定后可复现最小失败用例。**可执行核验**：Phase 2 记录首个失败用例（若有）的种子 + 收缩后最小用例 + 复现命令。
5. **不污染并行测试（与 Q6 时钟硬化并行隔离协同）**：属性 test（路径 C 类一纯内存）不经 `CoreMetrics.registerClock` 或全局 bean 替换；类二 `JunitAutoTestCase` 复用 Q6 thread-local frozen clock 子类，确保与 Q6 thread-local clock 并行隔离无冲突。**可执行核验**：属性 test 不含 `registerClock` 全局静态调用；与 Q6 `TestThreadLocalFrozenClockParallel` 范式协同（属性 test 可在并行 surefire 下跑）。
6. **全量回归绿**：`mvn clean install -DskipTests` 156 模块 BUILD SUCCESS；`mvn test` 0 failures / 0 errors（属性 test 本身绿 + 不破坏既有测试基线 + jqwik 依赖 test scope 不影响生产 classpath）。测试计数基线以 `docs/testing/known-good-baselines.md` 最近全量绿基线为准。
7. **无双真相源**：本文档 §1 / §4 引用 owner doc 不变量定义（不重新推导）；Phase 2 plan 引用本文档，不重推导不变量。

## 8. CI 门控设计

> 裁决属性测试是否纳入 mandatory 回归层 + jqwik 迭代次数与 CI 稳定性权衡。记录候选、考虑的替代、残留风险。

### 8.1 现状

- 现有 CI（`.github/workflows/`）：`maven.yml`（`mvn -B package` 全量构建/测试，timeout 30 分钟）+ `compliance.yml`（F8 反模式基线单向收紧 + F15 i18n + Q4 fault-injection-coverage grep 门控）+ `e2e.yml` + `mutation.yml`（Q1 nightly）+ `clock-rollover.yml`（Q6 nightly）。
- 属性测试（Phase 2 落地后）若是标准 JUnit 测试（`@Property` 经 JUnit Platform 运行），会被 `maven.yml` 的 `mvn test` 自动包含——**无需单独 CI job 即纳入 per-commit 回归**（区别于 Q1 pitest 须 nightly）。

### 8.2 候选

- **C-1：依赖 `maven.yml` 自动包含（无单独 job）**：属性 test 是 JUnit 5 `@Property`，`mvn test` 自动跑。残留：无显式「属性 test 覆盖率」门控（如 3 核心不变量是否全覆盖无 CI 检查）+ jqwik 随机种子导致的间歇性失败风险（flaky）。
- **C-2：新建 `.github/workflows/property-test.yml` 显式 job**：独立 job 跑属性 test + 固定种子（消除 flaky）+ 报告覆盖率。残留：与 `maven.yml` 重复跑同样的 `@Property`，CI 资源浪费。
- **C-3：加 job 到 `compliance.yml`**：在 compliance workflow 增属性 test 覆盖率 grep 检查（grep 3 核心不变量属性 test 存在性），对齐 compliance-baseline 单向收紧模式（同 Q4 fault-injection-coverage 范式）。

### 8.3 裁决（Decision）

> 决策输入：属性 test 是标准 JUnit 5（`maven.yml` 自动包含）+ Q3 首轮仅 3-5 个不变量（规模小）+ jqwik 种子可固定（消除 flaky）+ compliance-baseline 单向收紧先例（Q4 fault-injection-coverage）。

**裁决：Phase 2 采用 C-1（依赖 `maven.yml` 自动包含）作为主路径 + C-3（`compliance.yml` 加覆盖率 grep 检查）作为可选增强。C-2（独立 job）否决（资源浪费）。**

**裁决理由：**

1. **标准 JUnit 自动纳入 per-commit 回归**：`@Property` 经 JUnit Platform 运行，`maven.yml` 的 `mvn test` 自动跑。这是 Q3 相比 Q1（pitest 须 nightly）的 CI 集成优势——属性 test 反馈快（per-commit 即跑）。
2. **首轮规模小（3-5 不变量）无独立 job 必要**：3-5 个属性 test × 100 迭代，远小于 Q1 三域 pitest 资源开销。独立 job（C-2）重复跑 `maven.yml` 已跑的 `@Property`，浪费 CI 资源。
3. **jqwik 种子固定消除 flaky（CI 稳定性关键）**：`@Property(seed=...)` 固定种子使属性 test 确定性可复现——CI 每次跑同一种子，无随机失败（flaky）。**Phase 2 首跑绿后须记录种子**，CI 用固定种子（避免间歇性失败）。若需发现新失败，开发者本地跑随机种子（`mvn test -Djqwik.seed=-1` 或不固定），CI 回归用固定种子。
4. **C-3 可选增强防覆盖回潮**：若需显式门控「3 核心不变量均有属性 test」（防新增不变量时遗漏），可在 `compliance.yml` 加 grep 检查（对齐 Q4 fault-injection-coverage 范式：grep 3 不变量属性 test 存在性，命中数 ≥ 基线）。Phase 2 视团队需求裁决是否引入。

**考虑的替代：**

- **C-2（独立 workflow + 固定种子）**：否决——重复跑 `maven.yml` 已含 `@Property`，资源浪费。保留为 successor（触发：属性 test 规模显著增长或需独立报告时）。
- **per-commit 全量 + nightly 随机种子发现**：否决——Q3 首轮 3-5 不变量规模无需 nightly 随机种子发现；固定种子 per-commit 足够。
- **jqwik 迭代次数提升（tries=1000）**：CI 稳定性权衡。默认 100 迭代平衡覆盖与耗时；Phase 2 视耗时裁决是否夜间跑 1000 迭代（nightly 增强发现能力，per-commit 100 迭代）。

**残留风险：**

- **R6（无显式覆盖率门控则覆盖回潮无预警）**：C-1 依赖 `maven.yml` 跑测试，但不检查「3 核心不变量是否全覆盖」。若新增不变量时遗漏属性 test，无 CI 显式拦截（除非 C-3 grep 门控）。接受（首轮 3-5 不变量 + Phase 2 plan 明确覆盖清单）；C-3 作可选增强。
- **R7（jqwik 种子固定削弱发现能力 + 首跑发现 bug 的再固化循环）**：CI 固定种子消除 flaky 但每次跑同一种子，不发现新失败。缓解：开发者本地跑随机种子；或 nightly 跑随机种子（successor，触发：团队需要持续发现新失败时）。**首跑发现真实不变量违反（bug）时的再固化循环**：若 Phase 2 首跑（或后续）发现属性 test 失败（这正是属性测试的价值——发现 bug），固定种子复现的是**失败**而非绿，CI 无法稳定在该种子直至 bug 修复。处置闭环：记录失败种子 + 收缩后最小用例（§7 验收 4）→ 修复 bug → 重跑随机种子确认绿 → 固化新绿种子到 CI。即「发现失败 → 记录 → 修复 → 再固化绿种子」是预期工作流，非 CI 不稳定。
- **R8（属性 test 多迭代耗时）**：3-5 不变量 × 100 迭代，若每迭代触 localDb（类二）则耗时显著。路径 C 类一纯内存（策略 F2）使 100 迭代无 DB 开销；类二首批无随机化（单次）避免耗时。Phase 2 须实测属性 test 耗时并记录。

### 8.4 与现有 CI 的集成方式（Phase 2 落地）

- **主路径（C-1）**：零 CI 改动——属性 test 落盘到各域 `src/test/.../property/`，`maven.yml` 的 `mvn -B package` 自动包含。**种子固定**：Phase 2 首跑绿后记录每属性 test 的种子，`@Property(seed=...)` 固化到测试代码。
- **可选增强（C-3）**：若引入，在 `compliance.yml` 加 job，对齐 Q4 `fault-injection-coverage` 的**域覆盖**范式（resilient to class renames，非硬编码类名）：grep 目标域（finance/inventory）属性 test 存在性（如 `rg -l "@Property" module-{finance,inventory}/erp-*-service/src/test/.../property/`），命中域数 ≥ 2（单向收紧，对齐 Q4 6-domain 范式但 Q3 首批仅 finance+inventory 两域）。对齐 F8/Q4 架构（checker=pure reporter + gate 逻辑在 CI），Phase 2 须新写域覆盖 grep 检查逻辑（避免硬编码类名导致 rename 后门控被静默绕过）。

## 9. 残留风险汇总与 successor

> 汇总 §3.4 + §8.3 残留风险，登记 successor 触发条件（plan authoring guide §反松弛规则：Follow-up 须命名触发条件）。

| 风险 ID | 描述 | 分类 | successor 触发条件 |
|---------|------|------|--------------------|
| R1 | 路径 C 类一/类二边界裁决模糊 | Phase 2 实施约束 | Phase 2 给出每不变量的类划分（纯算术 vs 端到端） |
| R2 | 策略 F2 不验证 DB 持久化层；内存模型 vs 生产算术 drift（算术保真度） | 契约边界 | DB 层：jqwik 算术层 + 既有 JunitAutoTestCase DB 层双层互补；**算术保真度**：§5.1 保真度硬约束强制 @Property 调用/交叉校验生产纯函数（balanceTotals/FifoCostingStrategy），非测试侧并行 reimplementation；端到端随机化属 successor |
| R3 | jqwik 收缩质量依赖生成器设计 | Phase 2 实施约束 | Phase 2 失败用例抽样复核收缩质量；不精炼则重构生成器或评估 junit-quickcheck 替换 |
| R4 | 端到端不变量随机化 successor 工作量 | out-of-scope | 路径 C 类二端到端须随机化时，评估纯 jqwik 端到端接线（路径 A） |
| R5 | 与 Q6 thread-local clock 并行隔离协同 | Phase 2 实施约束 | §7 验收 5 核验；属性 test 不触 registerClock，类二复用 Q6 frozen clock |
| R6 | 无显式覆盖率门控则覆盖回潮 | CI 门控 | C-3 grep 门控作可选增强；或 successor（覆盖规模增长时） |
| R7 | jqwik 种子固定削弱发现能力 | CI 稳定性权衡 | 开发者本地跑随机种子；nightly 随机种子 successor（触发：团队需持续发现新失败） |
| R8 | 属性 test 多迭代耗时 | Phase 2 实施约束 | Phase 2 实测耗时；类一纯内存低开销，类二首批无随机化 |
| —（successor） | Q3 Phase 2 实现 plan（jqwik 接入 + 3-5 不变量属性 test） | out-of-scope（本文档 Phase 1） | 本文档经 ≥2 轮独立审查收敛（§Review Record）+ 技术选型裁决落定（§3.4）→ DRAFT_PLANS 起草 |
| —（successor） | 端到端不变量随机化（路径 A 纯 jqwik 端到端） | out-of-scope improvement | 路径 C 类二端到端须随机化时；须解决 localDb 接线 |
| —（successor） | 全不变量穷举（多币种折算平衡 / 合并抵消归零 / 资产折旧残值非负 / i18n locale 正确性） | optimization candidate | 核心不变量属性 test harness 沉淀后扩展 |
| —（successor） | junit-quickcheck 替换评估 | watch-only successor | jqwik 收缩质量在特定不变量不可接受时 |

## 10. 与 Q1 协同接口

> Q1↔Q3 协同（Q0 README §实施顺序裁决 line 153：「Q3 排在 Q1/Q4 后可复用其测试基础设施决策 + 盲区类清单」）。本节声明 Q3 消费 Q1 输出的盲区类清单的方式。

### 10.1 协同契约

- **Q1 产出**：`mutation-baseline.md`（Q1 Phase 2 实测基线）。**published baseline 是 posting-filtered 视图**（baseline line 77 明示 scope = 过账 dispatcher/Processor 类清单，为 Q4 消费裁剪）：
  - **直接可消费（published baseline）**：finance 顶盲区 `ErpFinPostingProcessor`（92 存活，baseline line 84）+ inv 过账类 `ErpInvCostingReclosePeriodCostsProcessor`（69，line 109）/ `InvPostingDispatcher`（17，line 113）。
  - **须原始视图（非 published）**：非过账的 costing/commitment 算术类（`FifoCostingStrategy`/`StockMoveBookkeeper`/`CommitmentVoucherGenerator`）的存活变异体存于原始 `mutations.xml` + `classify_mutations.py`（baseline line 79 明示 full list 不在 published baseline）。定位命令：`rg -o '<mutatedClass>[^<]*</mutatedClass>' module-{finance,inventory}/erp-*-service/target/pit-reports/**/mutations.xml | sort | uniq -c`（Phase 2 复核实际 pit-reports 路径）。
- **Q3 消费**：Q3 Phase 2（3-5 核心不变量属性 test）以 Q1 盲区类清单作为**优先覆盖目标**——
  - **P1 借贷平衡** ↔ published baseline `ErpFinPostingProcessor`（过账 Processor，借贷平衡算术核心）——**直接可消费**。
  - **P2 成本层累加** ↔ inv 域 costing 算术——published baseline 仅过账类，FIFO 算术核心 `FifoCostingStrategy`/`StockMoveBookkeeper` 须原始 `mutations.xml`（§10.2 命令）。
  - **P3 承付释放** ↔ finance commitment 算术——`CommitmentVoucherGenerator` 须原始 `mutations.xml`（非 published）。
- **协同价值双向**：
  - **Q3 → Q1**：Q3 属性 test 随机攻击不变量，杀死 Q1 存活变异体 → Q1 mutation score 提升（属性 test 是更强的测试，能杀死变异体）。
  - **Q1 → Q3**：Q1 盲区类清单指导 Q3 目标选择 → Q3 覆盖高价值不变量（盲区类指向的算术路径）。
- **协同时序**：Q1 Phase 2 已产出盲区类清单（`mutation-baseline.md` 已落盘），Q3 Phase 2 起草时可直接消费（无时序阻塞）。

### 10.2 Q3 消费格式（Q1 输出 → Q3 优先覆盖候选）

Q3 Phase 2 起草时，从 Q1 盲区类清单提取「Q3 优先覆盖候选」：

```
# Q3 优先覆盖候选（交集：Q1 盲区类 ∩ 不变量算术路径）
# 来源 P1：published mutation-baseline.md（posting-filtered）
# 来源 P2/P3：原始 mutations.xml（非 posting-filtered，定位命令见下）

## 借贷平衡（P1）— published baseline 直接可消费
| 盲区类（FQCN） | 存活变异体数 | 不变量算术路径 | Q3 覆盖优先级 |
|----------------|--------------|----------------|---------------|
| app.erp.fin.service.posting.ErpFinPostingProcessor | 92 | balanceTotals/debit-credit 累加 | 高（属性 test 直接攻击） |

## 成本层累加（P2）/ 承付（P3）— 须原始 mutations.xml（非 published）
# 定位命令（Phase 2 复核 pit-reports 实际路径）：
#   rg -o '<mutatedClass>[^<]*</mutatedClass>' \
#     module-{finance,inventory}/erp-*-service/target/pit-reports/**/mutations.xml \
#     | sort | uniq -c | sort -rn
# 筛选 costing/commitment 算术类（FifoCostingStrategy/StockMoveBookkeeper/CommitmentVoucherGenerator）
# 产出同格式盲区类表
```

### 10.3 协同边界

- Q1 **不负责**写属性 test（仅产出盲区清单）；属性 test 属 Q3 覆盖。
- Q3 **不负责**重跑变异测试（仅消费盲区清单）；Q1 mutation score 基线是 Q3 补属性 test 后回归验证的参照（Q3 属性 test 杀死变异体 → Q1 mutation score 提升）。
- 若 Q1 盲区类与 Q3 不变量算术路径不重合（Q0 README §残留风险：协同假设可能不成立），Q3 排期可独立前移，协同清单为空集亦有效。

## Review Record

> 审查记录：MQ 文档先行工作流要求 ≥2 轮独立子代理审查（第 1 轮规范合规 + 第 2 轮覆盖面/可执行性），由不同子代理会话执行（不同 task id），审查者不可与作者为同一会话。每轮输出 BLOCKER/MAJOR/MINOR 分级意见，作者修订后重审直至收敛（无残留 BLOCKER/MAJOR）。本文档经 2 轮审查收敛（R1 合规 + R2 覆盖/可执行性），作者据两轮意见修订。

- **Round 1（规范合规审查）**: `ses_04219a40fffeRuOhoVzZ2YNpBl`（独立子代理 fresh session cold context）— **needs-revision**，0 BLOCKER / 1 MAJOR / 3 MINOR。
  - MAJOR-1（F1）：承付不变量形式化为 `releasedAmount ≤ budgetAmount`，但真相源 `budget.md` 不含 `releasedAmount`/裸 `budgetAmount` 术语，且语义偏离 budget.md:18 的三通道控制 `available = budget − actual − commitment ≥ 0`——违反「引用 owner doc，不重新定义不变量」反模式守则。
  - MINOR-1（F2）：「TEMPORARY 账户」术语不在 cited owner doc（period-close.md 用「损益结转/本年利润清零」）。
  - MINOR-2（F3）：代码样本列名 `remaining` 与 ORM `remainingQuantity` 不一致。
  - MINOR-3（F4）：测试计数证据缺可复现命令。
  - 修改摘要：§1.3/§1.2/§4.2/§6.2 承付不变量改为三通道 `available = budget − actual − commitment ≥ 0`（余量非负）+ commitment 通道余额一致性，引用 budget.md:18/60；§1.3 期间结账不变量改为「损益结转后收入/费用类本期发生额清零 + 年度本年利润清零」；§4.2/§5.3 列名改 `remainingQuantity`；§1.1 测试计数标注「引用 Q0 README + roadmap line 697，本节不单独核验」。全部 live-repo 核验 PASS（jqwik 零命中确认 / `balanceTotals:709`+`assertBalanced:723` 确认 / `ErpInvCostLayer.remainingQuantity:551`/`ErpInvStockBalance.totalCost:384` 确认 / `JunitAutoTestCase` RECORDING→CHECKING 冲突在 nop-entropy 源码确认 / roadmap line 785 一致）。

- **Round 2（覆盖面与可执行性审查）**: `ses_0421972b7ffeANHcDRUvTnVDa4`（**另一个**独立子代理，不同 task id，新会话）— **needs-revision**，0 BLOCKER / 2 MAJOR / 3 MINOR。R1 的 4 项 finding 经 R2 复核均 **resolved**。
  - MAJOR-1：Q1 协同接口（§4.3/§5.5/§10）引用 published `mutation-baseline.md` 中不存在的盲区类（`StockMoveBookkeeper`/`FifoCostingStrategy`/`CommitmentVoucherGenerator`）——published baseline 是 posting-filtered 视图（line 77），仅 P1 的 `ErpFinPostingProcessor`(92) 直接命中。
  - MAJOR-2：§5 属性 test 模式在生产算术 vs 测试侧并行 reimplementation 间歧义——若 `buildRandomVoucher` 自平衡则 P1 是 tautology，P2 内存模型可能正确而生产 `FifoCostingStrategy` 有 bug 却不被发现（model/production drift，R2 未覆盖算术保真度缺口）。
  - MINOR-1（C-3 grep 门控硬编码类名，rename 后被静默绕过，偏离 Q4 域覆盖范式）/ MINOR-2（种子固定假设首跑绿，未处理首跑发现 bug 的再固化循环）/ MINOR-3（FIFO 生成器 stateless `frequency`/`sequence` 无法强制状态依赖的 `remaining>0` 合法性约束）。
  - 修改摘要：§4.3/§5.5/§10.1/§10.2 重写 Q1 协同——明确 published baseline 是 posting-filtered 视图（line 77/79），P1 `ErpFinPostingProcessor`(92) 直接可消费、P2 inv 仅过账类(`ErpInvCostingReclosePeriodCostsProcessor`69/`InvPostingDispatcher`17)直接可消费 + costing 算术核心须原始 `mutations.xml`、P3 须原始 `mutations.xml`；§10.2 增原始视图定位命令；§5.1 增「保真度硬约束（faithfulness）」强制 @Property 调用/交叉校验生产纯函数（`balanceTotals:709`/`FifoCostingStrategy.onIncoming/onOutgoing`）+ tautology 自检（注入变异应被发现）；§5.3 改为调生产策略记账；§5.2 注明须 jqwik `ActionSequence`/stateful 模式强制状态依赖合法性约束；§8.4 C-3 改域覆盖范式（resilient to rename）；§8.3 R7 增首跑发现 bug 的「记录→修复→再固化绿种子」闭环；§9 R2 增算术保真度缺口交叉引用 §5.1。

**收敛结论**：2 轮审查后无残留 BLOCKER / 无残留 MAJOR（R1 的 1 MAJOR + 3 MINOR、R2 的 2 MAJOR + 3 MINOR 全部修订 resolved）。文档可作为 Phase 2 实现 plan 的实施契约。

<!-- 审查者多样性已满足：R1（ses_04219a40...）/ R2（ses_0421972b...）两会话 task id 不同，均独立 fresh cold context，未复用作者上下文。 -->

### Phase 2 实施期发现回填（plan `2026-08-02-1400-1` 执行，非静默偏离）

> Closure Gate「实现与设计文档一致」要求实施期发现回填 Review Record 而非静默偏离。以下 2 项为 Phase 2 执行期对设计文档前提假设的实测修正，不改变 §3.4 已裁决的技术选型（jqwik + 路径 C + 策略 F2）。

- **jqwik 版本：1.8.x → 1.10.1**（§3.1.1 / §6.1）。设计文档 §3.1.1 原「jqwik 1.8.x（JDK 21 兼容）」指南基于 JUnit Platform 1.x 假设。Phase 2 实施期 effective-pom 实测本项目测试栈为 **JUnit Jupiter / Platform 6.0.3**（JUnit 6，非设计文档成文时的 JUnit 5 / Platform 1.x）。jqwik-engine 1.8.x 声明依赖 platform 1.x，与 6.x 二进制兼容性未保证。Phase 2 裁决：选用 **jqwik 1.10.1**（最新稳定），经冒烟探针 `JqwikSmokeTest` 实测 jqwik-engine 经 ServiceLoader 被 surefire JUnit Platform runner 拾取、与 Platform 6.0.3 二进制兼容（20 tries × 2 属性绿）。`@Property(seed=...)` 实施期实测 seed 属性为 `String` 类型（非 long 字面量），格式为纯数字串（如 `"20260802"`）。**裁决影响**：仅版本号升级（1.8.x→1.10.1）+ API 适配（`bigDecimals().between()` 取代 `bigDecimals(min,max)`；combine API 不可用，改 flatMap/map；`frequencyOf` 用于加权 Arbitraries），零范围/技术选型变更。
- **P1 属性 test 包位置：`.property/` 子包 → 同包 `app.erp.fin.service.posting`**（§6.2）。设计文档 §6.2 建议「各域 `erp-*-service/src/test/.../property/`」。Phase 2 实施期初版 P1 放 `.../posting/property/` 子包，用测试侧子类 `AccessiblePostingProcessor extends ErpFinPostingProcessor` 暴露 protected `balanceTotals`/`assertBalanced`。实测：`ErpFinPostingProcessor` 带 `@SingleSession` AOP 注解，其测试子类触发 Nop `gen-aop-proxy-for-test` 增强失败（`NoClassDefFoundError`，测试内部类在增强器 classloader 不可达）。裁决：P1 改同包 `app.erp.fin.service.posting` 直接访问 protected 方法（Java 语义：`protected` 含**同包访问权**，同包类可在生产实例上直接调用 protected 方法，无须子类）→ 零子类 → 零 AOP 代理生成。**裁决影响**：仅 P1 包位置（P2/P3 不子类化 @SingleSession bean，保留 `.../property/` 子包）；保真度硬约束不变（仍直调生产 `balanceTotals`/`assertBalanced`）；§5.1 P1「protected 方法经同包 test 访问 or 抽取为 test-scope 纯函数辅助」的同包路径落地。
- **P2/P3 保真度裁决记录**（§5.1）：P2 选 Decision (b)（内存 `FifoCostLayerModel` 逐行镜像生产 `onOutgoing:103-129` + golden 交叉校验锚定 `TestErpInvFifoCosting` 生产实测数字），替代 (a) extract-method 否决（消耗循环内交织 `saveOrUpdateEntity`，抽取需重组保存点非纯行为保持）。P3 选 Decision (b)（内存 `BudgetCommitmentModel` 镜像生产 `ErpFinBudgetControlBiz:81` available 公式逐字符一致 + golden 交叉校验 budget.md §设计范式场景），替代 (a) extract-method 否决（available 公式仅 2 subtract，抽取收益低于触及生产类风险）。两域均满足 §5.1「model 跟随 production，共享 golden 输入产出一致」。
