# 2026-08-01-1121-1-mq-q3-property-based-testing-design-doc 属性测试 Phase 1 设计文档

> Plan Status: completed
> Last Reviewed: 2026-08-01
> Mission: audit-remediation
> Work Item: MQ Q3
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q3（line 676, 785）；`docs/architecture/quality-engineering/README.md` §实施顺序裁决（位 4，Q1/Q4 完成后可复用其测试基础设施决策）
> Related: `docs/plans/2026-08-01-1121-1-mq-q0-quality-gap-analysis-readme.md`（Q0 顺序基线，前置 done）；`docs/plans/2026-08-01-1158-2-mq-q1-mutation-testing-design-doc.md`（Q1，属性测试可复用 Q1 测试基础设施决策 + 盲区类清单）；`docs/plans/2026-08-01-1357-2-mq-q1-mutation-testing-impl.md`（Q1 Phase 2 基线，属性不变量选择可对齐 Q1 盲区分布）；`docs/plans/2026-08-01-1158-3-mq-q4-fault-injection-design-doc.md`（Q4，同为测试有效性维度，文档结构参照）
> Audit: required

## Current Baseline

> 本计划是 MQ 文档先行工作流的 **Phase 1**：产出审查收敛的设计文档 `docs/architecture/quality-engineering/property-based-testing.md`，**不改任何代码/ORM/CI**。MQ roadmap（line 843-862）与 Q0 README（line 20-22）明确：Phase 1 设计文档经独立子代理 ≥2 轮审查收敛后，方可编写 Phase 2 实现 plan。

**audit-remediation 主线状态**：M0 / MA1-MA7 / MR1-MR6 / MV / MG 全 done；MR6 milestone CLOSED。MQ 进行中：Q0/Q1/Q4/Q6 已 done，Q2/Q3/Q5/Q7 待办。验证基线 `mvn clean install -DskipTests` 156 模块全绿；`mvn test` 全绿。

**Q3 现状（NOT FOUND 证据，引用 Q0 README §范围矩阵 §Q3 + roadmap line 785 + 实仓复核，核验日期 2026-08-01）**：

- 全仓零属性测试依赖：`rg "jqwik|quickcheck" --glob '*.xml'` 零命中（2026-08-01 复核确认，无 jqwik / junit-quickcheck / 任何 property-based 库）。
- 当前测试均为**黄金路径具体断言**：~1900+ JUnit 测试 + 260+ E2E spec 均为单次固定输入 → 固定期望输出。无法证明 ERP 强不变量在**任意操作序列**下恒成立——这是黄金路径测试的结构性盲区。
- ERP 拥有大量**强不变量**（形式化验证的理想对象），均已在 owner doc 中定义（设计文档 §不变量枚举须引用这些真相源，不重新推导）：
  - **借贷平衡**（`docs/design/finance/posting.md`）：每张凭证 `Σ debit == Σ credit`，过账后总账余额满足 `资产 = 负债 + 权益`。
  - **期间结账后余额归零**（`docs/design/finance/period-close.md`）：损益类账户结转后 TEMPORARY 账户余额 = 0；永久账户余额跨期累计。
  - **成本层累加 = 余额表**（`docs/design/finance/costing-methods.md`）：任意时刻 `Σ ErpInvCostLayer.remaining × unitCost == ErpInvStockBalance.totalCost`（layer-based costMethod 适用；具体 scope 由设计文档引用 costing-methods.md 裁决，不预设全部 costMethod）。
  - **costMethod 切换前后总成本不变**（`docs/design/finance/costing-methods.md`）：STANDARD_REVALUATION 红冲跨重估时 `balance.totalCost` 恢复不变量（line 74 P1-MA2-024）。
  - **承付释放不超余量**（`docs/design/finance/budget.md` §承付）：任意操作序列后 `releasedAmount ≤ budgetAmount`，部分开票/退货/冲销后承付正确释放/恢复。
  - 红冲成本不变量（FIFO/LIFO/STANDARD，`costing-methods.md` line 37/74/472）。
- **关键风险（roadmap line 785 明示）**：Nop 测试栈 `JunitAutoTestCase` + `RECORDING`→`CHECKING` 快照录制与 jqwik 多迭代语义冲突——快照录制一次（RECORDING）但 jqwik 回放多次（每属性 100+ 次迭代），录制/回放不对称。Phase 1 须裁决属性测试是否绕过 `JunitAutoTestCase`（用纯 JUnit 5 + jqwik）及夹具重置策略。

**剩余差距**：无 Q3 设计 owner doc。属性测试框架选型（jqwik vs junit-quickcheck）、与 Nop 测试栈冲突的裁决、不变量优先级排序、属性 test 设计模式（随机操作序列生成器 + 每步不变量断言）均未裁决，须在 Phase 1 文档中独立审查后定夺。

## Goals

- 产出 MQ Q3 的 Phase 1 设计文档 `docs/architecture/quality-engineering/property-based-testing.md`，经独立子代理 ≥2 轮审查收敛（第 1 轮规范合规 + 第 2 轮覆盖面/可执行性），覆盖 MQ 文档先行工作流要求的 6 节：
  1. **现状评估**（引用实仓证据：零属性测试依赖 + 黄金路径结构性盲区 + 不变量 owner doc 真相源清单）
  2. **目标与非目标**（覆盖 3-5 个核心不变量的属性测试；不追求全不变量穷举）
  3. **技术选型**——jqwik（vs junit-quickcheck）+ 与 `JunitAutoTestCase`/快照冲突的裁决（绕过 vs 适配）+ 夹具重置策略的替代评估与裁决理由
  4. **实施步骤**（不变量优先级排序 + 属性 test 设计模式：随机操作序列生成器 + 每步不变量断言 + 收缩策略）
  5. **验收判据**（核心不变量属性 test 成立 + 在多轮随机输入下恒成立 + 不污染并行测试）
  6. **CI 门控设计**（属性测试是否纳入 mandatory 回归层 + 与 `.github/workflows/compliance.yml` 集成）
- 文档须显式声明与 `JunitAutoTestCase`/快照机制的兼容裁决（绕过纯 JUnit 5 还是适配 Nop 测试基类），这是 roadmap line 785 明示的关键风险。
- 文档须显式声明不变量优先级（哪些不变量 Phase 2 优先实现），引用 owner doc 真相源。

## Non-Goals

- **不实现任何代码/ORM/CI 变更**——本计划仅产出设计文档。Phase 2 实现（jqwik 接入 + 3-5 个不变量属性 test）是**独立的后续 plan**，须在本设计文档审查收敛后方可起草（MQ 文档先行工作流）。
- 不修改 `nop-entropy` 源码（若选型触及平台测试基类改造，其实施属 Phase 2 + 须遵守 AGENTS.md nop-entropy 日志规则）。
- 不追求全不变量穷举（仅 3-5 个核心不变量；其余 successor）。
- 不重新推导 NOT FOUND 证据（引用 Q0 README，避免双真相源）。
- 不重新定义 ERP 不变量（引用 owner doc 真相源：`posting.md` / `period-close.md` / `costing-methods.md` / `budget.md`）。
- 不编写 Q2/Q5 设计（同批独立 plan）。

## Task Route

- Type: `app-layer design change`（设计文档编写；纯文档，零代码）
- Owner Docs: `docs/architecture/quality-engineering/README.md`（Q0 顺序基线 + 文档先行工作流引用）；`docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q3 + §横切关注点 §文档先行工作流；`docs/design/finance/posting.md`（借贷平衡不变量真相源）；`docs/design/finance/period-close.md`（期间结账余额归零真相源）；`docs/design/finance/costing-methods.md`（成本层累加=余额表 + costMethod 切换不变量真相源）；`docs/design/finance/budget.md`（承付释放不超余量真相源）；`../nop-entropy/docs-for-ai/`（Nop 测试栈 JunitAutoTestCase/快照机制参照，须裁决兼容性）
- Skill Selection Basis: AGENTS.md 强制技能扫描已完成——`nop-backend-dev`/`nop-frontend-dev`/`nop-testing`/`nop-debugging` 均不匹配"编写属性测试设计文档"。`nop-testing`（JunitAutoTestCase/快照）触及测试框架但其范围是测试编写，且其快照机制正是 Q3 须裁决的冲突对象，留待 Phase 2 实现计划加载。故 `Skill: none`（与 roadmap Q3 行 Skill 列 `none` + Q1/Q4 设计文档计划 Skill 列一致）。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. 本计划纯文档，不涉及端口/环境变量/CORS/密钥/.env/外部服务。

## Execution Plan

### Phase 1 - 编写 Q3 设计文档草稿

Status: completed
Targets: `docs/architecture/quality-engineering/property-based-testing.md`（新建）
Skill: none

- Item Types: `Add | Decision`
- Prereqs: Q0 done（已满足）；Q1 Phase 1/Phase 2 done（已满足，属性测试可复用 Q1 测试基础设施决策 + 盲区类清单）；Q0 README §实施顺序裁决落盘（已满足，Q3 位 4）

- [x] Add: 创建 `property-based-testing.md`，含 MQ 文档先行工作流要求的 6 节骨架
      - Skill: none
- [x] Add: §现状评估 —— 引用（非重推导）Q0 README §Q3 + roadmap line 785 + 本计划 Current Baseline 实仓复核：零属性测试依赖（`rg "jqwik|quickcheck" --glob '*.xml'` 零命中）、黄金路径结构性盲区、不变量 owner doc 真相源清单。标注可复现核验命令 + 核验日期。
      - Skill: none
- [x] Decision: §技术选型 —— 评估并裁决属性测试框架：
      - jqwik（候选首选——主流 JVM 属性测试库，原生 JUnit 5 集成 + 自动收缩 shrinking）
      - junit-quickcheck（替代——junit-quickcheck 与 junit4/5 兼容但社区活跃度低于 jqwik）
      - 记录候选 + 考虑的替代 + 残留风险（jqwik 收缩质量 / junit-quickcheck 生成器生态 / 两者与 Nop 测试栈兼容度差异）
      - Skill: none
- [x] Decision: §Nop 测试栈兼容裁决 —— 裁决属性测试与 `JunitAutoTestCase` + `RECORDING`/`CHECKING` 快照机制的兼容方式（roadmap line 785 明示关键风险）：
      - 路径 A：绕过 `JunitAutoTestCase`，用纯 JUnit 5 + jqwik（@Property 注解），属性 test 独立夹具重置（@BeforeEach 每迭代重建）
      - 路径 B：适配 `JunitAutoTestCase`（jqwik 嵌套于 Nop 测试基类内，禁用快照录制）
      - 路径 C：混合——不变量断言用纯 jqwik，需要 localDb/GraphQL 触发的端到端不变量用 `JunitAutoTestCase` 单次固定输入
      - 记录候选 + 考虑的替代 + 残留风险（路径 A 失去平台 localDb 便利 / 路径 B 快照语义冲突未根治 / 路径 C 两套夹具维护成本）
      - Skill: none
- [x] Add: §不变量枚举与优先级 —— 引用 owner doc 真相源枚举 ERP 强不变量（借贷平衡 / 期间结账余额归零 / 成本层累加=余额表 / costMethod 切换总成本不变 / 承付释放不超余量 / 红冲成本不变量），排序 Phase 2 优先实现 3-5 个（依据：不变量强度 × 实现复杂度 × owner doc 成熟度）
      - Skill: none
- [x] Add: §属性 test 设计模式 —— 随机操作序列生成器（@Provide Arbitraries）+ 每步不变量断言 + 收缩策略（jqwik shrinking 暴露最小失败用例）+ 与 Q1 盲区类清单的消费方式（Q1 发现的测试盲区类可指导属性 test 目标选择）
      - Skill: none
- [x] Add: §实施步骤 —— jqwik 依赖接入位置（test scope）+ 3-5 个核心不变量属性 test 落地清单 + 夹具重置策略落地
      - Skill: none
- [x] Add: §验收判据 —— 核心不变量属性 test 在 ≥100 轮随机输入下恒成立 + 收缩暴露的失败用例可复现 + 不污染并行测试（注意与 Q6 时钟硬化的并行隔离协同）
      - Skill: none
- [x] Add: §CI 门控设计 —— 裁决属性测试是否纳入 mandatory 回归层 + 与 `.github/workflows/compliance.yml` 集成方式 + jqwik 迭代次数与 CI 稳定性权衡
      - Skill: none
- [x] Add: §与 Q1 协同接口 —— 声明消费 Q1 输出的盲区类清单（哪些类的存活变异体指向不变量违反），作为 Q3 Phase 2 优先覆盖目标
      - Skill: none

Exit Criteria:

> 本计划纯文档，零代码/ORM/CI 变更。完整仓库 `typecheck`/`build`/`test` 不适用（按 plan authoring guide，无代码更改的计划删除验证命令门控）。

- [x] `property-based-testing.md` 落盘，含上述 6 节 + Nop 测试栈兼容裁决 + 不变量优先级 + Q1 协同接口，两个 Decision 记录候选+替代+残留风险三要素
- [x] §现状评估每条证据标注可复现核验命令 + 核验日期

### Phase 2 - 独立子代理设计文档审查循环（≥2 轮至收敛）

Status: completed
Targets: `docs/architecture/quality-engineering/property-based-testing.md`（`## Review Record` 节）
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 1 草稿落盘

- [x] Proof: 第 1 轮审查——**规范合规审查**，由独立子代理（新会话）执行。审查项：6 节结构完整性 / 与项目约定一致性 / 反模式检查（无双真相源、不变量是否误重新推导而非引用 owner doc）/ owner doc 引用正确性（posting.md 借贷平衡 / costing-methods.md 成本层 / budget.md 承付 / period-close.md 余额归零）/ JunitAutoTestCase 冲突是否真实（实仓核验快照录制一次 vs 回放多次）。输出 BLOCKER/MAJOR/MINOR。
      - Skill: none
- [x] Proof: 第 2 轮审查——**覆盖面与可执行性审查**，由**另一个**独立子代理（不同 task id，新会话）执行。审查项：jqwik vs junit-quickcheck 替代是否充分评估 / 三路径兼容裁决是否可执行 / 不变量优先级排序是否合理（强度 × 复杂度 × 成熟度）/ 属性 test 设计模式是否落地（随机序列生成器 + 每步断言 + 收缩）/ 与 Q1 协同接口是否可消费 / 与 Q6 并行隔离是否冲突 / CI 门控 jqwik 迭代稳定性。输出 BLOCKER/MAJOR/MINOR。
      - Skill: none
- [x] Add: 作者据审查意见修订文档并重审，直至两轮均无 BLOCKER/MAJOR；`## Review Record` 节持久化两轮审查者 task id + 轮次 + 结论 + 修改摘要
      - Skill: none

Exit Criteria:

- [x] §Review Record 记录 ≥2 轮审查，两轮由不同子代理会话执行，无残留 BLOCKER/MAJOR
- [x] Nop 测试栈兼容裁决经审查后可执行（或据审查修订后可执行）

## Draft Review Record

- Independent draft review iteration 1: **accept-as-is**（`ses_04220f65dffe1MZeDHaYgUSq6L`，独立子代理 fresh session cold context）— 0 BLOCKER / 0 MAJOR / 2 MINOR。M1（roadmap 行号引证 off-by-one：Q3 detail 在 line 785 而非 786，786 实为 Q4——已全文修正 785-786→785 + roadmap line 786→785）/ M2（"7 类 costMethod 均须成立"过度——costing-methods.md 仅实现 MOVING_AVERAGE/FIFO/STANDARD，layer-累加不变量仅 layer-based 方法适用——已软化为"layer-based costMethod 适用；具体 scope 由设计文档引用 costing-methods.md 裁决"）。两项 MINOR 均已修订。Baseline 核验全 PASS（jqwik 零命中确认；4 owner doc 不变量存在性确认；Q1/Q4/Q6 done 确认；Q1 协同前提成立）。Phase-1 doc-only 范围保持。converged → 转 active。

## Closure Gates

> 本计划无代码/ORM/view/CI 变更（纯设计文档）。按 plan authoring guide §Closure Gates："对于无代码更改的计划（仅文档），删除验证命令门控并说明原因"——故不设 `mvn typecheck/build/test` 门控，原因：零 Java/ORM/CI 变更，全量构建无回归面。

- [x] 范围内行为完成：`property-based-testing.md` 6 节 + Nop 测试栈兼容裁决 + 不变量优先级 + Q1 协同接口落盘且 Review Record 收敛
- [x] 相关文档对齐：文档引用 Q0 README（无双真相源）；与 roadmap §MQ Q3 + 各 owner doc 不变量定义一致
- [x] 无验证命令门控（纯文档计划，原因如上）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查（本计划本身）已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中
- [x] `docs/logs/{year}/{month}-{day}.md` 追加本计划日志条目（计划级结束步骤）

## Deferred But Adjudicated

### Q3 Phase 2 实现（jqwik 接入 + 3-5 个核心不变量属性 test）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: MQ 文档先行工作流强制要求 Phase 1 设计文档审查收敛后方可编写 Phase 2 实现 plan。本计划仅交付设计文档。
- Successor Required: yes —— 触发条件：本计划 done（设计文档审查收敛）+ Nop 测试栈兼容 Decision 落定。届时 DRAFT_PLANS 起草 Phase 2 实现 plan（加载 `nop-testing` skill），plan 引用本文档作为范围与验收依据。建议在 Q1 Phase 2 产出盲区类清单后启动以充分消费协同。

### 全不变量穷举

- Classification: `optimization candidate`
- Why Not Blocking Closure: Q3 首轮聚焦 3-5 个核心不变量。其余不变量（如多币种折算平衡 / 合并抵消归零 / 资产折旧残值非负）作为 successor。
- Successor Required: yes —— 触发条件：核心不变量属性 test harness 沉淀后扩展。

## Closure

Status Note: 本计划为 MQ Q3 文档先行工作流 Phase 1（纯设计文档，零代码/ORM/view/CI 变更）。Phase 1（6 节设计文档 + Nop 测试栈兼容裁决 + 不变量优先级 P1-P3 + Q1 协同接口落盘）+ Phase 2（2 轮独立子代理审查收敛：R1 规范合规 0/1/3 + R2 覆盖/可执行性 0/2/3，全部修订 resolved）均已完成。设计文档 `docs/architecture/quality-engineering/property-based-testing.md` 经审查修订后裁决：jqwik（vs junit-quickcheck）+ 路径 C 混合（纯函数不变量用纯 jqwik 绕过 JunitAutoTestCase 快照，端到端用 JunitAutoTestCase 单次）+ 策略 F2 纯内存状态重置 + 首批 P1 借贷平衡/P2 成本层累加/P3 承付释放（三通道 available≥0）+ CI 经 maven.yml 自动包含（C-1）+ 种子固定消除 flaky。独立结束审计 PASS（2026-08-01，独立子代理新会话 fresh cold context，8/8 closure gate 核验通过）。Q3 Phase 1 工作项交付完成，转 done；Phase 2 实现 plan 为独立 successor。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理，新会话 fresh cold context（task `ses_04214223fffeD8SyCB23KIDxLL`，general 类型，未复用执行者上下文，不同于 2 轮文档审查 task id）
- Evidence: PASS（8/8 closure gate 全部 live repo 核验通过）——
  1. **计划内部一致性**：Phase 1/Phase 2 Status=completed；所有 Phase 1/Phase 2 content items + exit criteria 全部 `[x]`（残留 `[ ]` 仅在 Closure 区，审计时为预期开放态）。
  2. **交付物完整**：`property-based-testing.md` 落盘（543 行），6 必需节 + §3.2 Nop 测试栈路径 A/B/C 裁决 + §3.4 Decision + §4 不变量优先级 + §10 Q1 协同接口 + Review Record 齐全。
  3. **Review Record 收敛**：2 轮审查由 2 个不同 task id 子代理会话执行（R1 `ses_04219a40...` / R2 `ses_0421972b...`），无残留 BLOCKER/MAJOR（R1 1 MAJ+3 MIN、R2 2 MAJ+3 MIN 全部修订 resolved）。
  4. **Decision 三要素**：§3.4（jqwik+路径C+F2 + 替代否决 + R1-R5）+ §8.3（C-1主+C-3可选 + 替代否决 + R6-R8）齐全。
  5. **无双真相源**：§1 引用 Q0 README + roadmap line 785 + owner doc 不变量定义；live 复核 `rg "jqwik|quickcheck" --glob '*.xml'` EXIT=1 与文档声明一致。
  6. **范围纪律（doc-only）**：`git status --porcelain` 仅 `.md` 文件（设计 doc + plan + 同批 sibling plan），零 Java/pom/yml/orm/xml 代码变更。
  7. **承付不变量对齐**：§1.3/§4.2/§6.2 均用三通道 `available = budget − actual − commitment ≥ 0`（对齐 budget.md:18），无 fabricated `releasedAmount ≤ budgetAmount`（仅 Review Record 描述旧错误形式）。
  8. **roadmap 行号**：文档正确引用 line 785（Q3），无 786 误引。
  - 无 mvn 运行（按纯文档计划 Closure Gates 正确排除在范围外）。

Follow-up:

- Q3 Phase 2 实现 plan（设计文档收敛后起草；建议在 Q1 Phase 2 盲区清单后启动）。
