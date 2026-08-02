# 2026-08-02-0650-1 MQ Q3 属性测试不变量扩展（P5-P8 纯算术不变量）

> Plan Status: completed
> Last Reviewed: 2026-08-02
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q3（line 676）+ Q3 设计文档 §9 successor（`property-based-testing.md:474`「全不变量穷举…optimization candidate…核心不变量属性 test harness 沉淀后扩展」）
> Related: `docs/plans/2026-08-02-1400-1-mq-q3-property-based-testing-impl.md`（Q3 Phase 2 首批 P1-P3，已 completed + 独立 closure audit PASS）；`docs/plans/2026-08-01-1121-1-mq-q3-property-based-testing-design-doc.md`（Phase 1 设计文档）
> Audit: required

## Current Baseline

> 本计划是 Q3 属性测试的**后继扩展**，非新质量维度。Q3 设计文档（`property-based-testing.md`）经 2 轮独立审查收敛，技术选型已裁决（jqwik + 路径 C 混合 + 策略 F2 纯内存），首批 P1-P3 已落地并经独立 closure audit。本计划在同一设计契约下扩展**纯算术类一不变量**（设计文档 §4.2 候选 P5 + §9 successor 清单），不引入新设计维度、不重做 Phase 1 文档先行循环（触发条件是「harness 沉淀后扩展」，现 harness 已沉淀）。

**Q3 首批已落地（2026-08-02 复核实仓）：**
- 3 个核心不变量属性 test 类落盘（设计文档 §6.2 命名）：
  - `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/PropertyErpFinDebitCreditBalance.java`（P1 借贷平衡，类一直调生产 `balanceTotals`/`assertBalanced`）
  - `module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/property/PropertyErpInvCostLayerAccumulation.java`（P2 成本层累加，类一内存 FIFO 模型 + golden 交叉校验锚定生产）
  - `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/budget/property/PropertyErpFinBudgetCommitmentRelease.java`（P3 承付释放，类一三通道预算模型镜像生产 `ErpFinBudgetControlBiz:81` available 公式）
  - `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/property/JqwikSmokeTest.java`（jqwik 接入冒烟）
- jqwik 1.10.1 test scope 接入 `erp-fin-service` + `erp-inv-service` pom（设计文档 §3.1.1 原 1.8.x 经实施期裁决升 1.10.1 兼容 JUnit Jupiter/Platform 6.0.3，已回填设计文档 Review Record）。
- `compliance.yml` `property-test-coverage` job（C-3 裁决引入，finance+inventory 两域 grep 门控，单向收紧）。
- 保真度硬约束满足（P1 直调生产；P2/P3 经 Decision (b) 内存模型镜像 + golden 交叉校验锚定生产实测数字）；tautology 自检通过（每域注入变异反向证明敏感）；种子固化。
- Q3 plan `Plan Status: completed` + 独立 closure audit PASS（冷重播 closure 契约逐条对齐 LIVE 仓库，plan-check.mjs --strict passed: true 26/26）。

**successor 触发条件已满足：** 设计文档 §9（line 474）「全不变量穷举（多币种折算平衡 / 合并抵消归零 / 资产折旧残值非负 / i18n locale 正确性）optimization candidate — 核心不变量属性 test harness 沉淀后扩展」。harness（3 属性 test 类 + jqwik 依赖 + CI 门控 + 收缩/保真度范式）已沉淀且经独立 closure 验证。

**P5-P8 生产目标纯度实仓核验（保真度硬约束 §5.1 的前置评估，2026-08-02 复核）：** 设计文档 §5.1 faithfulness 硬约束要求每属性 test 调用/交叉校验**生产纯函数算术**。但 P5-P8 的生产目标**均非纯函数**（DB-entangled）——这与 Q3 首批 P2/P3 同型（predecessor 独立草案审查 BLOCKER-2 同源：生产函数含 `@Inject IDaoProvider` + DB 写入，faithfulness 约束不可直接满足，predecessor 经 per-phase `Decision` 裁决路径 (b) 内存模型镜像 + golden 交叉校验锚定生产解决）。本计划沿用同型裁决，每不变量须有 faithfulness `Decision` 项：

| 不变量 | 生产目标 | 纯度核验（实仓证据） | faithfulness 路径（须每相 Decision 裁决） |
|--------|---------|---------------------|------------------------------------------|
| P5 STANDARD 重估 | `StandardCostingStrategy.onOutgoing`/`onIncoming`（+ `FifoCostingStrategy` 红冲） | `FifoCostingStrategy:49-50` `@Inject IDaoProvider daoProvider` + `BookingContext` DB 写入 → **非纯** | 同 predecessor P2：内存成本层模型镜像生产策略 + golden 交叉校验（锚定 `TestErpInvFifoCosting` 生产实测数字） |
| P6 多币种折算平衡 | `ErpFinPostingProcessor.balanceTotals:722-726` | **`balanceTotals:726` 用 `fact.getAmount()`（文币额），不累加 `amountFunctional`** → 无现成功能币额平衡累加入口；`VoucherFact` 有 `amountFunctional` 字段（:26/:88）但 `balanceTotals` 不消费它 | 须 Decision 裁决：(a) 抽取功能币额累加纯函数（提取 `sumFunctionalBalance` 并在生产侧复用以防 drift）/ (b) 内存模型镜像折算 + golden 锚定既有功能币额 E2E 断言 / (c) 直调既有 Provider 折算算术 + 测试侧累加 amountFunctional |
| P7 资产折旧残值 | `ErpAstDepreciationScheduleExecuteDepreciationProcessor`（+ facade `ErpAstDepreciationScheduleProcessor` 持有共享算术） | concrete `:32-33` `@Inject IDaoProvider` + `saveOrUpdateEntity :96/100/118`；facade `:46-47` `@Inject IDaoProvider` + `:135` `IOrmTemplate orm()` → **非纯** | Decision (b) 内存折旧模型镜像生产每期折旧额算术 + golden 交叉校验锚定既有 assets 折旧 E2E |
| P8 合并抵消归零 | `ErpFinConsolidationEliminationGenerateEliminationCandidatesProcessor`/`PostEliminationProcessor` | `:33-34`/`:37-38` `@Inject IDaoProvider` + `:68/85/103`/`:90/115/139/147` `saveEntity` → **非纯** | Decision (b) 内存抵消模型镜像生产抵消配对算术 + golden 交叉校验锚定既有合并抵消 E2E |

> **结论**：P5-P8 生产目标全部 DB-entangled，本计划**不声称「类一直调生产纯函数」**（P1 借贷平衡是首批唯一生产纯函数可直接调用的特例）。每相须有 faithfulness `Decision` 项记录 (a) 抽取纯函数 / (b) 内存模型镜像 + golden 锚定 / (c) 直调既有算术 三候选的裁决 + golden anchor + R2 残留风险（对齐 predecessor P2/P3 范式 + 设计文档 §5.1）。

**assets 域 jqwik 缺失（实仓核验）：** `erp-ast-service/pom.xml` 无 jqwik 依赖（grep 零命中）→ P7 须先补 jqwik 1.10.1 test scope Add（对齐 finance/inventory 范式）。

**剩余差距：** 设计文档 §4.2 候选 P5（STANDARD 重估总成本不变）+ §9 successor 清单 3 项纯算术不变量（多币种折算平衡 / 合并抵消归零 / 资产折旧残值守恒）均未落地。这些不变量当前仅由黄金路径单测覆盖具体场景，无随机化证明「任意输入下恒成立」。

## Goals

- **P5 STANDARD 重估前后总成本不变**（类一纯 jqwik 内存模型）：随机 STANDARD 成本法重估 + 红冲跨重估序列，断言红冲后 `Σ layer.remaining × unitCost` 恢复至原出库前 + `balance.totalCost` 不变量恢复（真相源 `costing-methods.md` line 74/472，P1-MA2-024 已修但无随机化回归）。faithfulness 经内存模型镜像生产 `StandardCostingStrategy`/`FifoCostingStrategy` 红冲算术 + golden 交叉校验（生产目标 DB-entangled，详见 Current Baseline 纯度表）。
- **P6 多币种折算借贷平衡**（类一纯 jqwik）：随机本位币折算率 + 多币种凭证行，断言折算后功能币额仍满足 `Σ debitFunctional == Σ creditFunctional`（真相源 `posting.md` `VoucherFact` 本位币为准 + `flow-overview.md` 多币种折算路径）。**注意**：生产 `balanceTotals:726` 累加 `getAmount()`（文币）非 `amountFunctional`——无现成功能币额平衡入口，P6 须 faithfulness Decision 裁决抽取/镜像路径（详见 Current Baseline 纯度表 + Phase 2）。
- **P7 资产折旧残值守恒**（类一纯 jqwik 内存模型）：随机折旧序列（直线法/加速折旧），断言每期 `netBookValue = cost − accumulatedDepreciation ≥ residualValue` 且 `accumulatedDepreciation ≤ (cost − residualValue)`（真相源 `docs/design/assets/depreciation-and-posting.md`）。faithfulness 经内存折旧模型镜像生产每期折旧额算术 + golden 交叉校验（生产 Processor DB-entangled）。
- **P8 合并抵消归零**（类一纯 jqwik）：随机公司间交易对（内部销售/采购/应收/应付），断言抵消分录 `Σ eliminationDebit == Σ eliminationCredit` 且抵消后合并净额 = 外部交易净额（真相源 `docs/design/finance/intercompany-consolidation.md`）。faithfulness 经内存抵消模型镜像生产配对算术 + golden 交叉校验（生产 Processor DB-entangled）。
- **保真度硬约束延续**（设计文档 §5.1 + predecessor BLOCKER-2 先例）：每属性 test 须 faithfulness Decision 裁决 (a) 抽取生产纯函数 / (b) 内存模型镜像 + golden 交叉校验锚定生产 / (c) 直调既有算术 三候选之一，记录 golden anchor + R2 残留风险；tautology 自检（注入变异应被发现）。
- **CI 门控基线裁决**（设计文档 §8.4 C-3 单向收紧）：Phase 4 `Decision` 裁决 `compliance.yml` `property-test-coverage` 门控形态——域覆盖基线扩到含 assets（若 P7 引入）OR 保持 finance+inventory 域级门控 + 新增属性 test 计数单向收紧。裁决须选定单一方案（见 Phase 4）。

## Non-Goals

- **不新增质量维度**（本计划是 Q3 已裁决维度的不变量扩展，非新里程碑；不重做 Phase 1 文档先行审查循环——设计文档 §3/§4/§5 技术选型与设计模式已收敛，本计划沿用）。
- **不实现 P4 期间结账余额归零随机化**（设计文档 §4.2 P4 是**类二端到端**不变量，须 localDb 触发结转凭证；其随机化属设计文档 §9 R4 successor「端到端不变量随机化」，须先解决路径 A localDb 接线，是独立后续，非本计划范围）。本计划仅覆盖**类一纯算术**不变量。
- **不引入 junit-quickcheck**（设计文档 §3.1.2 否决作首选，successor 触发 = jqwik 收缩质量在特定不变量不可接受；本计划沿用 jqwik）。
- **不修改 nop-entropy 源码 / 零 ORM / 零生产 Java 变更**（设计文档 §6.4 边界：全部在应用层 test scope；属性 test 经内存模型或只读调用生产纯函数）。
- **不重跑 Q1 变异测试**（Q1↔Q3 协同是消费关系，Q3 属性 test 杀死存活变异体→Q1 score 提升属 Q1 回归验证范围；本计划不跑 pitest）。
- **i18n locale 正确性**不纳入本计划（设计文档 §1.3 注记：i18n locale 可部分折入 Q3 作属性用例，但首期优先财务/库存强不变量；列为 successor）。

## Task Route

- Type: `implementation-only change`（在已裁决的 Q3 设计契约下扩展测试，不改契约/模型/架构）
- Owner Docs: `docs/architecture/quality-engineering/property-based-testing.md`（设计契约 §4.2/§5/§6/§9）+ 各不变量真相源 owner doc（`costing-methods.md` / `posting.md` / `depreciation-and-posting.md` / `intercompany-consolidation.md`）
- Skill Selection Basis: `nop-testing`（jqwik 属性 test 编写 + 与既有测试栈隔离）；本计划沿用 Q3 首批已验证的「类一纯 jqwik + 策略 F2 纯内存 + 保真度硬约束」范式，无新 skill 需求。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（jqwik 1.10.1 test scope 已在 finance/inventory service pom；P7 须在 `erp-ast-service` pom 补 jqwik 1.10.1 test scope——Phase 3 Add 项）。

## Execution Plan

### Phase 1 - P5 STANDARD 重估总成本不变属性 test

Status: completed
Targets: `module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/property/PropertyErpInvStandardRevaluation.java`
Skill: `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: Q3 首批 P2 `PropertyErpInvCostLayerAccumulation` 内存 FIFO 模型可复用（同包内存模型/生成器范式）

- [x] Decision: P5 faithfulness 路径裁决——选 **(b)** 内存 STANDARD 成本模型镜像生产 `StandardCostingStrategy.onOutgoing:86-99`（扣 currentStandard×qty + `:92` 捕获 line.unitCost=roundCost(standard)）+ `onIncoming:55-56` 红冲路径（originReturnedMoveId!=null && unitCost>0 → 沿用透传旧标准）+ `RevalueAction` 镜像 STANDARD_REVALUATION 重估。否决 (a)：onOutgoing 消耗交织 `saveOrUpdateEntity`+`ctx` 回调，抽取需重组 DB 副作用非纯行为保持。golden 锚定 `TestErpInvStandardCosting.testReverseOutgoingRestoresBalanceAcrossRevaluation`（入库 20@10→出库 8 扣 80→重估 10→15→红冲加回 80 恢复 200）。R2 残留：纯内存不验证 DB 持久化，由 `TestErpInvStandardCosting` 端到端互补；模型不建模重估重定基故仅断言 totalQuantity 非负 + 红冲恢复。
  - Skill: `nop-testing`
- [x] Add: `PropertyErpInvStandardRevaluation` 属性 test 类——2 个 `@Property(tries=100, seed=20260809/20260810)` + golden 交叉校验 + tautology 自检。核心属性 `outgoingThenReverseRestoresCostRegardlessOfInterveningRevaluation`（单次出库+任意重估+红冲，断言 totalCost/totalQuantity 恢复）+ 序列属性 `reverseRestoresBalanceAcrossRevaluation`（随机 incoming/outgoing/revalue/reverse 序列，每步断言恢复不变量）。
  - Skill: `nop-testing`
- [x] Proof: tautology 自检——`tautologySelfCheck_reverseUsingCurrentStandardIsDetected` 注入「红冲误用当前标准（15）而非捕获标准（10）」变异（P1-MA2-024 修复的 bug），断言 totalCost=240≠200 不可恢复，证明属性非恒等式。
  - Skill: `nop-testing`

Exit Criteria:

- [x] P5 属性 test `mvn test -Dtest='PropertyErpInvStandardRevaluation'` 100 迭代全绿（种子固化），inventory 域常规 `mvn test` 零回归
  - 验证：P5 类 4 tests/0 failures（2 `@Property`×100 tries 绿 + golden + tautology）；inventory 模块 `mvn test` 134 tests/0 failures/0 errors BUILD SUCCESS（零回归）。

### Phase 2 - P6 多币种折算借贷平衡属性 test

Status: completed
Targets: `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/PropertyErpFinMultiCurrencyBalance.java`（包位置裁决改同包 `app.erp.fin.service.posting`，见下 Decision；非 plan header 笔误的 `.../posting/property/`）
Skill: `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（复用 P1 借贷平衡生成器范式）

- [x] Decision: P6 faithfulness 路径裁决——选 **(a) 直调生产纯函数**（最强保真度）。关键事实核验：生产 `balanceTotals:726` 累加 `fact.getAmount()`（非 `getAmountFunctional()`），但 per `posting.md:449` 「amount 字段保留作功能金额（balanceTotals/assertBalanced 以本位币为准）」+ `:452` 「amountFunctional = source × rate」。故多币种折算后 `amount = functional`（= source × rate，由 Provider 计算），balanceTotals 消费 amount 即消费本位币额——与「折算后 Σ debitFunctional == Σ creditFunctional」不变量一致。「无现成功能币额平衡累加入口」经 amount=功能币 语义桥梁解决，无需抽取新函数。包位置：同 P1 改同包 `app.erp.fin.service.posting`（protected balanceTotals/assertBalanced 同包访问，避免 @SingleSession 子类触发 gen-aop-proxy-for-test NoClassDefFoundError）。否决 (b) 内存折算镜像（balanceTotals 是可独立实例化纯函数，直调即可，避免 drift）；否决 (c) 测试侧累加 amountFunctional（与生产消费 getAmount 路径解耦 drift）。R2 残留：纯内存不验证 DB 持久化，由既有 P2P/O2C Provider 端到端互补。
  - Skill: `nop-testing`
- [x] Add: `PropertyErpFinMultiCurrencyBalance` 属性 test 类——3 个 `@Property(tries=100, seed=20260811/20260812/20260813)` + golden + tautology。属性1 生产 balanceTotals 按方向累加功能币额匹配 oracle；属性2 功能币平衡放行/不平衡拒绝；属性3 多币种增量价值（source 平衡≠功能币平衡，不同汇率时 assertBalanced 捕获 source 平衡掩盖的失衡）。FactSpec 携 sourceAmount/exchangeRate/credit，functional = source×rate（镜像 Provider 折算）。
  - Skill: `nop-testing`
- [x] Proof: tautology 自检——`tautologySelfCheck_translationMutationIsDetected` 注入折算变异（functional = source×rate×rate，汇率乘两次），断言变异功能币累加与 oracle 不符，证明属性非恒等式。
  - Skill: `nop-testing`

Exit Criteria:

- [x] P6 属性 test `mvn test -Dtest='PropertyErpFinMultiCurrencyBalance'` 100 迭代全绿（种子固化），finance 域常规 `mvn test` 零回归
  - 验证：P6 类 5 tests/0 failures（3 `@Property`×100 tries 绿 + golden + tautology）；finance 模块 `mvn test` 322 tests/0 failures/0 errors BUILD SUCCESS（零回归；初次跑 2F/6E 经重跑 0F/0E 确认为 finance 域已知 date/clock 敏感 flaky，非 P6 引入，同 Q3 首批 closure 注记同型）。

### Phase 3 - P7 资产折旧残值守恒属性 test

Status: completed
Targets: `module-assets/erp-ast-service/pom.xml`；`module-assets/erp-ast-service/src/test/java/app/erp/ast/service/property/PropertyErpAstDepreciationResidual.java`
Skill: `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: `erp-ast-service` pom **实测无 jqwik**（2026-08-02 grep 零命中）→ 须先补 jqwik 1.10.1 test scope

- [x] Add: `erp-ast-service` pom 补 jqwik 1.10.1 test scope 依赖（对齐 finance `pom.xml` / inventory `pom.xml` 范式）
  - Skill: none
  - 落地：`module-assets/erp-ast-service/pom.xml` 增 jqwik 1.10.1 `<scope>test</scope>`（注释引用本 plan + 设计文档 §3.1.1/§6.1，对齐 finance/inventory 注记）。
- [x] Decision: P7 faithfulness 路径裁决——选 **(a) 直调生产纯函数**（最强保真度）。生产折旧算术核心 `DepreciationCalculator.calculate:25-75` 是 **public static 纯函数**（无 @Inject、无 DB、无 IoC），由 `ErpAstDepreciationScheduleExecuteDepreciationProcessor:72-73` 调用。Processor 本身 DB-entangled，但其折旧算术已封装为独立纯函数 DepreciationCalculator，可从纯内存 test 直接调用——无需镜像（无 drift 风险）。否决 (b) 内存折旧模型镜像（DepreciationCalculator 本身是 public static 纯函数，直调即可，无 DB-entangled 像 FifoCostingStrategy/StandardCostingStrategy 须镜像）；否决 (c) 抽取每期折旧额为纯函数（已存在，无需再抽）。golden 锚定 `TestDepreciationCalculator.testNonZeroResidualStraightLineConvergesToResidual`（12000/2000/6 → 6 期 nbv=2000=残值，accum=10000）。R2 残留：纯函数测试不验证 DB 持久化/过账，由既有 `TestErpAstDepreciation` 端到端互补。
  - Skill: `nop-testing`
- [x] Add: `PropertyErpAstDepreciationResidual` 属性 test 类——3 个 `@Property(tries=100, seed=20260814/20260815/20260816)` + golden + tautology。属性1 直线法每期 nbv≥残值 + accum≤cost−residual；属性2 双倍余额递减同（触发 :46 最后24月改直线法）；属性3 直线法收敛（迭代至 nbv==残值精确 + accum==cost−residual，cap=months+5 吸收舍入 undershoot）。实施期发现回填：直线法每期 (cost−residual)/months HALF_UP undershoot 时完整 months 期后 nbv 可能剩微小正余量（如 100/0/3 → 0.0001），截断分支仅在本期金额跌穿残值时触发，精确收敛需 months+1 期。
  - Skill: `nop-testing`
- [x] Proof: tautology 自检——`tautologySelfCheck_residualClampMutationIsDetected` 注入「残值截断分支移除」变异（10000/2000/3 第3期不截断，金额 2666.6667 使 nbv=1999.9999<残值 2000），证明属性 1 的「nbv≥残值」断言能发现，非恒等式。
  - Skill: `nop-testing`

Exit Criteria:

- [x] P7 属性 test `mvn test -Dtest='PropertyErpAstDepreciationResidual'` 100 迭代全绿（种子固化）；assets 域常规 `mvn test` 零回归
  - 验证：P7 类 5 tests/0 failures（3 `@Property`×100 tries 绿 + golden + tautology）；assets 模块 `mvn test` 106 tests/0 failures/0 errors BUILD SUCCESS（零回归）。

### Phase 4 - P8 合并抵消归零属性 test + CI 门控基线裁决

Status: completed
Targets: `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/property/PropertyErpFinConsolidationElimination.java`；`.github/workflows/compliance.yml`；`{finance,inventory,assets}/erp-*-service/pom.xml`（surefire includes 补全，实施期发现）
Skill: `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1-3

- [x] Decision: P8 faithfulness 路径裁决——选 **(b) 内存抵消模型镜像生产配对算术**（同 predecessor P2/P3 范式）。生产 `ErpFinConsolidationEliminationGenerateEliminationCandidatesProcessor:55-106`（每 matched IntercompanyMatch 按 AR_AP+REVENUE_COST[+INV_PROFIT] 各生成 candidate，eliminationAmount=matchedAmount）+ `writeDraftEliminationVoucher:73-149`（Dr=amount/Cr=amount 平衡凭证）DB-entangled。否决 (a) 抽取抵消配对为纯函数（循环交织 saveEntity + findSubjectByCode DB 查询，抽取需重组 DB 副作用+跨实体访问，非纯行为保持）。golden 锚定 `TestErpFinIntercompanyMatchingAndElimination.testPostEliminationGeneratesDraftVoucher`（公司间配对 2000 → AR_AP+REVENUE_COST 各 amount=2000 → 每条 totalDebit=2000/totalCredit=2000 平衡凭证）。R2 残留：纯内存不验证 DB 持久化/凭证落库/subject 解析，由既有 `TestErpFinIntercompanyMatchingAndElimination` 端到端互补。
  - Skill: `nop-testing`
- [x] Add: `PropertyErpFinConsolidationElimination` 属性 test 类——3 个 `@Property(tries=100, seed=20260817/20260818/20260819)` + golden + tautology。属性1 每张抵消凭证平衡+聚合平衡；属性2 合并净额=外部净额（内部配对净额贡献零）+全量抵消；属性3 AR_AP/REVENUE_COST 类型对称性。
  - Skill: `nop-testing`
- [x] Proof: tautology 自检——`tautologySelfCheck_unbalancedVoucherMutationIsDetected` 注入「抵消凭证借贷不平衡」变异（Dr=M/Cr=M/2，模拟 writeDraftEliminationVoucher 金额算术错），证明属性 1 isBalanced/aggregateBalanced 断言能发现，非恒等式。
  - Skill: `nop-testing`
- [x] Decision: CI 门控基线单一方案裁决——选定 **(X) 域覆盖基线扩到含 assets**（finance+inventory+assets，grep 域数 ≥3，单向收紧）。理由：(X) 对齐设计文档 §8.4 C-3 域覆盖范式（resilient to class rename + 新增属性不触发基线漂移）；(Y) @Property 计数基线脆性（每新增属性须手调基线）。P7 已在 assets 落地属性 test → 域覆盖自然扩到 3。落地：`.github/workflows/compliance.yml` `property-test-coverage` job `DOMAINS=['finance','inventory','assets']` BASELINE=3，job name 改 `3 domains`，本地 Python 复跑 3/3 PASSED。
  - Skill: none
- [x] 实施期发现回填（非静默偏离，设计文档 §8.1/C-1 contract 补全）：**surefire 默认 include 模式（Test*/*Test/*Tests/*TestCase）漏拾 `Property*` 类名约定的属性 test**——Q3 首批 P1/P3 + 本计划 P5-P8 在补全前**仅 `JqwikSmokeTest`（匹配 *Test）在 `mvn test` 运行**，其余 `Property*` 类只在 `-Dtest='Property*'` 运行。这违反设计文档 §8.1/C-1「@Property 经 mvn test 自动跑」契约 + 使 `maven.yml` CI 不实际强制属性 test。修复：`{finance,inventory,assets}/erp-*-service/pom.xml` surefire `<configuration>` 显式 `<includes>` 增 `**/Property*.java`（含默认 4 模式）。验证：finance 341（+19）/inventory 143（+9）/assets 111（+5）tests，P1-P8 全部在 `mvn test` 运行绿。此发现须回填设计文档 Review Record（closure 步骤执行）。
  - Skill: none

Exit Criteria:

- [x] P8 属性 test 100 迭代全绿（种子固化）；4 新增属性 test 类（P5-P8）+ Q3 首批 P1-P3 共 7 核心不变量属性 test 全绿；CI 门控单一方案落地 + 绿
  - 验证：P8 类 5 tests/0 failures（3 `@Property`×100 tries 绿 + golden + tautology）；P5-P8 共 19 tests 全绿（P5=4/P6/P7/P8 各 5；含 11 `@Property`×100 tries 种子固化）；finance 341/inventory 143/assets 111 tests/0 failures（surefire 补全后 P1-P8 全在 `mvn test` 运行）；compliance.yml property-test-coverage 门控 3/3 域 PASSED。

## Draft Review Record

- Independent draft review iteration 1: **needs-revision** (`ses_040475a6dffe785RD6uKEmUnjw`) — 1 BLOCKER + 1 MAJOR + 3 MINOR。BLOCKER-1：faithfulness 硬约束（§5.1）未带 predecessor BLOCKER-2 同型 rigor——P5-P8 生产目标实仓核验全部 DB-entangled（`@Inject IDaoProvider`/`saveEntity`）非纯函数，且 P6 声称「直调 balanceTotals 以 amountFunctional 为准」事实错误（`balanceTotals:726` 用 `getAmount()` 非 `amountFunctional`）；缺 per-phase faithfulness Decision。MAJOR-1：CI 门控 Goal「或」呈现两目标歧义。MINOR：assets jqwik 存在性未定论（实测无）/ closure gate「0 failures」未带预存 master-data 注记 / Draft Review Record 占位符。修订：Current Baseline 增 P5-P8 生产目标纯度实仓核验表 + 结论；每相增 faithfulness Decision 项（(a)/(b)/(c) + golden anchor + R2）；P6 事实纠正（balanceTotals 用 getAmount 非 amountFunctional）+ Phase 2 Decision 裁决；assets jqwik 实测无→Phase 3 增 Add 项；CI 门控改 Phase 4 单一方案 Decision；closure gate 带 master-data 注记。
- Independent draft review iteration 2: **acceptable-as-is** (`ses_0403f784effe2kDARdmbzPyprq`) — iteration-1 全 5 finding（1 BLOCKER + 1 MAJOR + 3 MINOR）经 live-repo 核验全部 RESOLVED；3 new MINOR（P7 facade 标签 / closure gate「纯函数」措辞与 DB-entangled 结论表面矛盾 / infra prereqs 冗余 boilerplate）均为措辞/标签细微，不影响执行契约。作者据 3 MINOR 修订（非阻塞，提升内部一致性）：closure gate 措辞去「纯函数」/ P7 facade 标签纠正 / infra prereqs 去冗余。共识达成，可转 active。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成（P5-P8 四个纯算术不变量属性 test 落地，各 100 迭代绿，种子固化）
- [x] 保真度硬约束满足（每属性 test 经 faithfulness Decision 裁决的机制调用/交叉校验生产算术——P5/P8 Decision (b) 内存模型镜像 + golden 锚定；P6/P7 Decision (a) 直调生产纯函数 balanceTotals/DepreciationCalculator）；tautology 自检通过
- [x] 相关文档对齐（设计文档 `property-based-testing.md` §9 successor 状态回填 + Q3 successor 实施 Review Record 回填 3 项发现；roadmap Q3 行 successor-done 注记）
- [x] 已运行验证：`mvn clean install -DskipTests` 156 模块 BUILD SUCCESS；`mvn test -fae` 155/156 模块 SUCCESS，0 本计划引入的 failures/errors（属性 test 本身绿：P5-P8 共 19 tests 全绿 P5=4/P6/P7/P8 各 5，含 11 `@Property`×100 tries 种子固化；不破坏既有测试基线——surefire include 补全后 P1-P8 全在 `mvn test` 运行；jqwik 依赖 test scope 不影响生产 classpath；排除 1 个预存 `module-master-data TestErpMdExchangeRateApiClient` 日期漂移失败——R6.9 test-hardening successor，本计划零触及零因果，对齐 Q3 首批 closure 注记）；`compliance.yml` property-test-coverage 门控 3/3 域绿
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中（Closure Audit Evidence：独立子代理 `ses_04017cfa6ffeyJbu4lXHJXyhDj` 2026-08-02 PASS）

## Deferred But Adjudicated

### P4 期间结账余额归零随机化

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 设计文档 §4.2 P4 是类二端到端不变量（须 localDb 触发结转凭证），其随机化属设计文档 §9 R4 successor「端到端不变量随机化」，须先解决路径 A localDb 接线。本计划仅覆盖类一纯算术不变量。
- Successor Required: yes — 触发条件：路径 A 纯 jqwik 端到端 localDb 接线方案落定后，扩展 P4 多期间序列随机化。

### i18n locale 正确性属性 test

- Classification: `watch-only residual`
- Why Not Blocking Closure: 设计文档 §1.3 注记 i18n locale 可部分折入 Q3，但首期优先财务/库存强不变量。
- Successor Required: yes — 触发条件：i18n locale 格式化正确性须随机化验证时。

## Closure

Status Note: 计划可关闭——4 个纯算术不变量属性 test（P5 STANDARD 重估红冲 / P6 多币种折算借贷平衡 / P7 折旧残值守恒 / P8 合并抵消归零）全部落盘且 100 迭代种子固化绿；保真度硬约束满足（P5/P8 Decision (b) 内存模型镜像 + golden 锚定生产实测数字，P6/P7 Decision (a) 直调生产纯函数 balanceTotals/DepreciationCalculator——后者经冷审计确认 public final class + public static + 零 @Inject/DB）；每域 golden 交叉校验 + tautology 自检（注入变异反向证明敏感）；CI property-test-coverage 门控扩到 3 域本地复跑 PASSED；实施期发现并修复 surefire 默认 include 漏拾 `Property*` 类（补全 §8.1/C-1 contract，使 P1-P8 全在 mvn test 运行）。零生产 Java / 零 ORM / 零 nop-entropy 变更。唯一 mvn test 失败为预存 master-data 汇率日期漂移（R6.9 successor，零因果）。

Closure Audit Evidence:

- Auditor / Agent: independent closure audit subagent (cold context, fresh session `ses_04017cfa6ffeyJbu4lXHJXyhDj`)
- Date: 2026-08-02
- Verdict: PASS（2 MINOR 非阻塞：聚合摘要数字 12→11 @Property / 各 5→P5=4,P6/P7/P8 各 5 已修订；app-erp-all 两生成资源文件系 mvn clean install 副作用已 git checkout 还原）
- Evidence: fresh-context 逐条对齐 LIVE 仓库——全文阅读 plan + 4 属性 test 类源码 + `DepreciationCalculator` 生产纯函数核验（public final class + public static + 零 @Inject/DB）+ 3 service pom.xml surefire includes + compliance.yml + 设计文档/roadmap/log diff；本地 Python 复跑 CI 门控逻辑（3/3 PASSED）；fnmatch 证明 Property* 类不匹配 surefire 默认模式（确认发现真实）；rg 精确核验 @Property 注解计数（11）与唯一 `[ ]` 项（仅 2 结束审计门，执行者正确留给审计）；git diff 排查 src/main 改动定性为生成文件副作用。10 项 checklist 全 PASS。

Follow-up:

- P4 期间结账随机化（类二端到端，须路径 A localDb 接线 successor）
- i18n locale 属性 test（successor）
- 若 P5-P8 任一暴露 jqwik 收缩质量不可接受 → junit-quickcheck 替换评估（设计文档 §3.1.2 successor）
- R6.9 master-data 汇率测试 frozen-clock 化（与本计划零因果，独立 successor）
- 若 P5-P8 任一暴露 jqwik 收缩质量不可接受 → junit-quickcheck 替换评估（设计文档 §3.1.2 successor）
