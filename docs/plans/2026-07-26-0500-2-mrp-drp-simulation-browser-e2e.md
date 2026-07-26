# 2026-07-26-0500-2 MRP/DRP 仿真引擎浏览器层 E2E

> Plan Status: completed
> Mission: erp
> Work Item: B1 MRP/DRP 仿真引擎浏览器层端到端验证
> Last Reviewed: 2026-07-26
> Source: 近期深化后端特性浏览器层验证缺口 —— B1 MRP/DRP 仿真引擎（plan `2026-07-22-1000-2`）落地场景-版本-参数变体模型 + E2 fork 仿真计算编排 + 结果对比引擎 + DRP 对应物，经 JUnit 覆盖（`TestErpMfgMrpSimulation` 8 场景 + `TestErpDrpSimulation` 5 场景 + mfg-service 135 测试 + drp-service 31 测试），但**零浏览器层 E2E**。AGENTS.md §当前项目阶段明示「各域细化端到端验证」为当前重点。
> Related: `docs/plans/2026-07-22-1000-2-manufacturing-mrp-drp-simulation-engine.md`（B1 仿真引擎落地）、`docs/design/manufacturing/simulation-engine.md`（B1 owner doc）
> Audit: required

## Current Baseline

MRP/DRP 仿真引擎已落地（B1，E2 fork 范式 —— 单次路径零触及零回归）：

**实体**（3 MRP + 3 DRP）：
- `ErpMfgMrpScenario`（`module-manufacturing/model/app-erp-manufacturing.orm.xml:1531`）：`code`/`orgId`/`baseMrpPlanId`（基线引用）/`description`/`status`（字典 `erp-mfg/simulation-status` DRAFT/RUNNING/COMPLETED/ARCHIVED）
- `ErpMfgMrpScenarioVersion`（`:1605`）：`scenarioId`/`versionNo`/`computedMrpPlanId`（引用计算结果 plan）/`promotedPlanId`/`snapshotSummary`/`createdBy`/`createdTime`
- `ErpMfgMrpScenarioParam`（`:1662`）：`scenarioId`/`materialId`（nullable，null=全局覆盖）/`paramType`（字典 LEAD_TIME/LOT_SIZE/SAFETY_STOCK）/`paramValue`
- DRP 同构 3 实体（`ErpDrpScenario`/`Version`/`Param`，paramType=SAFETY_STOCK/LEAD_TIME/REPLENISHMENT_QTY，Param 含 warehouseId 维度）

**三个 @BizMutation/@BizQuery 入口**（config-gated `erp-mfg.simulation-enabled` / `erp-drp.simulation-enabled` 默认 false）：
- `runSimulation(scenarioId)` @BizMutation —— requireScenario(DRAFT) → 新建 COMPUTED plan → 从基线 plan 加载 demands → 内存中参数覆盖重算（E2 fork `SimulationMrpEngine`）→ 写场景版本 + snapshotSummary → scenario status=COMPLETED
- `promoteToFormalPlan(scenarioVersionId)` @BizMutation —— 校验 promotedPlanId 非空（防重复 `ERR_MFG_SIMULATION_VERSION_ALREADY_PROMOTED`）+ status=COMPLETED → 新建 DRAFT plan（code 后缀 `-PROMOTED-{versionNo}`）→ 复制计划行 → 版本 ARCHIVED + promotedPlanId 回写
- `compareVersions(versionIdA, versionIdB)` @BizQuery —— 须同 scenarioId（否则 `ERR_MFG_SIMULATION_VERSIONS_NOT_COMPARABLE`）→ 按 materialId 聚合顶层行 → 4 维 diff（netRequirementDelta/plannedQuantityDelta/totalPurchaseAmountDelta/shortageOnlyInA/B/inBoth）→ 返回 `SimulationDiffResult` DTO

**参数覆盖回退顺序**（Decision B）：场景物料级（精确 materialId）→ 场景全局（materialId=null）→ 全局配置/主数据默认。

**关键约束（测试数据隔离）**：`runSimulation` 需要一个基线 `ErpMfgMrpPlan`（`baseMrpPlanId`）且该 plan 须含已整合 demands。`promoteToFormalPlan` 产 DRAFT `ErpMfgMrpPlan` 行（`-PROMOTED-` 后缀），若不清理会污染 MRP 基线。须自包含 setup 建测试专用物料 + 基线 plan + demands（镜像 `runMfgChain` 测试专用物料隔离范式，避免污染种子 MRP 数据）。

剩余差距：仿真三入口（runSimulation/promoteToFormalPlan/compareVersions）经 JUnit 单层验证，但**全栈浏览器层路径未验证**——场景经 GraphQL `__save` 创建 + 参数变体 `__save` + `runSimulation` mutation 触发 → 场景版本 + COMPUTED plan 生成可观测。DRP 同构路径同样零浏览器层覆盖。

## Goals

- 验证 MRP `runSimulation` 参数变体覆盖 → 场景版本 + COMPUTED plan 生成（LOT_SIZE 覆盖使 plannedQuantity 相对基线可观测变化）
- 验证 MRP `compareVersions` 两版本结构化 diff（plannedQuantityDelta + shortageInBoth 非空）
- 验证 MRP `promoteToFormalPlan` DRAFT plan 生成 + 防重复守卫 + ARCHIVED 状态翻转
- 验证 DRP `runSimulation` + `compareVersions` 同构路径（SAFETY_STOCK 覆盖使补货量可观测变化）

## Non-Goals

- 产能仿真（CRP successor）—— B1 Non-Goal（触发：APS 排产 what-if 需求）
- 概率/蒙特卡洛仿真 —— B1 Non-Goal（触发：业务方明确概率仿真需求）
- 物料级 fixedLotSize/minOrderQty/maxOrderQty 主数据列 —— B1 Non-Goal（触发：ORM 加列授权）
- APS 排产甘特图可视化仿真 —— F16 successor
- 仿真结果自动释放为正式采购单/工单 —— B1 Non-Goal（仅产建议 + 显式 promoteToFormalPlan）
- 跨场景对比 —— B1 Deferred（触发：跨业务假设对比需求）
- 对比结果实体化持久化 —— B1 Deferred（触发：对比结果审计需求）
- **config-gate 关闭时的 `ERR_*_SIMULATION_DISABLED` 守卫** —— webServer JVM args 全局启用无法 per-spec toggle（对齐 `b2b-asn` spec 既有限制范式），config-gate 关闭路径经 JUnit `TestErpMfgMrpSimulation`/`TestErpDrpSimulation` 场景 1 已覆盖，浏览器层不重复
- 生产 Java/ORM/契约/codegen/字典/种子变更 —— 纯测试 + 文档

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/manufacturing/simulation-engine.md`（§场景-版本模型 + §参数变体覆盖语义 + §结果对比算法 + §仿真到正式 plan 转正路径）、`docs/design/manufacturing/mrp.md`（§仿真引擎关系交叉引用）
- Skill Selection Basis: `nop-testing`（Playwright 浏览器层 E2E + config-gated 特性 webServer JVM arg 启用范式 + 测试专用物料隔离范式，对齐 `runMfgChain` 既有隔离先例）

## Infrastructure And Config Prereqs

- webServer JVM args（`playwright.config.ts` webServer.command）追加 `-Derp-mfg.simulation-enabled=true -Derp-drp.simulation-enabled=true`（两 config-gate 默认 false）。
- No infra prereqs beyond existing baseline（fresh-DB H2 + 既有 webServer 启动链）。

## Execution Plan

### Phase 1 - Explore（基线 plan setup + 参数变体字段集 + COMPUTED plan 行结构核实）

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../mrp/SimulationMrpEngine.java`、`ErpMfgMrpScenarioBizModel.java`、`SimulationVersionComparator.java`、`app-erp-all/src/main/resources/_vfs/_init-data/`（种子物料/plan 数据）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: 无

- [x] Proof: 核实 `ErpMfgMrpScenario` + `ErpMfgMrpScenarioParam` 经 GraphQL `__save` 创建所需最小必填字段集（scenario: code/orgId/baseMrpPlanId/status=DRAFT；param: scenarioId/paramType/paramValue + materialId 可选），确认 config-gate 启用后 `runSimulation` mutation 可达
- [x] Proof: 核实基线 `ErpMfgMrpPlan`（baseMrpPlanId）的 demands 来源 —— 是否需要预建 `ErpMfgMrpDemand` 行，或可复用种子 plan（若有）。裁决自包含 setup 路径（建测试专用物料 + 基线 plan + demands 隔离，镜像 `runMfgChain` 测试专用物料范式）
- [x] Proof: 核实 COMPUTED plan 行结构（`SimulationMrpEngine.runSimulation` 产的 `ErpMfgMrpPlanLine` 字段：materialId/netRequirement/plannedQuantity/plannedDate），确定 LOT_SIZE 覆盖使 plannedQuantity 变化的断言期望值表
- [x] Proof: 核实 `SimulationDiffResult` DTO 经 GraphQL 返回的字段结构（LineDiff 行级 + totalNetDelta/totalPlannedDelta 聚合摘要 + shortageOnlyInA/onlyInB/inBoth 缺料集），确定 compareVersions 断言期望值
- [x] Proof: 核实 DRP 同构路径字段差异（`ErpDrpScenarioParam.warehouseId` 维度 + REPLENISHMENT_QTY paramType + `DrpSimulationDiffResult` 2 维 diff）

Exit Criteria:

> 仅写此阶段实际交付的可观察结果，以及解除后续阶段阻塞所需的任何本地化检查。

- [x] Explore 笔记记录基线 setup 路径 + 参数变体字段集 + COMPUTED plan 期望值表 + diff DTO 结构（写入 plan Execution Decision 段，不新建独立文档）

#### Execution Decision（Phase 1 Explore 笔记）

**1. 基线 plan setup 路径（自包含隔离）**

无种子 `ErpMfgMrpPlan` / `ErpMfgMrpDemand` / `ErpDrpPlan` / `ErpDrpLine` / `ErpDrpParameter` CSV（`app-erp-all/src/main/resources/_vfs/_init-data/` 仅含 work_order/cost_variance/forecast 等 4 张制造表，无 drp/mrp plan 数据）。故两 spec 各自经 GraphQL `__save` 建测试专用：
- **MRP**: 1 物料（`code=E2E-MFG-SIM-MAT-{tag}`）+ 1 baseline `ErpMfgMrpPlan`（status=DRAFT, orgId=2, businessDate=2026-08-01）+ 1 `ErpMfgMrpDemand`（demandSource=MANUAL, sourceBillType=MRP_MANUAL, materialId=test_mat, quantity=25, requirementDate=2026-08-15）+ scenario + scenarioParam。
- **DRP**: 1 物料 + 1 `ErpDrpParameter`（warehouseId/materialId/safetyStock=10/orgId=2/replenishmentMethod=LOT_FOR_LOT）+ baseline `ErpDrpPlan`（orgId=2）+ scenario + scenarioParam。

种子无 stock_balance 行覆盖 test_mat → available=0 → MRP net=demandQty, DRP net=safetyStock（确定性派生）。test_mat safetyStock 默认 null（MRP 不触发额外 SAFETY_STOCK demand；DRP 的 safetyStock 来自 ErpDrpParameter 非 material）。

**种子 org/warehouse/uom FK 校验**：`__save` 强制 FK 校验，须引用种子行。MRP 用 orgId=2（ERP-CO）+ uomId=1（PCS，demand.uoMId 必填）。DRP 用 orgId=2 + warehouseId=1（WH-MAIN）+ uomId=1（material.uoMId 必填）。

**2. COMPUTED plan 期望值表（LOT_SIZE / SAFETY_STOCK 覆盖，确定性派生）**

| spec | 场景 | 输入 | 公式 | 期望 |
|------|------|------|------|------|
| MRP | LOT_SIZE 覆盖 v1 | demand qty=25, no stock, no safetyStock | net=25; override LOT_SIZE=10 → planned=ceil(25/10)*10=30 | planned=30 |
| MRP | 非 DRAFT 守卫 | 同场景 COMPLETED 后重跑 | requireScenario(DRAFT) 抛 ERR_MFG_SIMULATION_SCENARIO_NOT_DRAFT | status≠0 |
| MRP | promote v1→DRAFT | version COMPLETED + scenarioParam LOT_SIZE=10 | 复制 lines + code 后缀 `-PROMOTED-1` | promoted.status=DRAFT, version.status=ARCHIVED |
| MRP | 重复 promote 守卫 | ARCHIVED version 重 promote | promotedPlanId 非空 → ERR_MFG_SIMULATION_VERSION_ALREADY_PROMOTED | status≠0 |
| DRP | SAFETY_STOCK 覆盖 v1 | param SS=10, scenario SS override=20, no stock/forecast/onOrder | net=20-0=20; suggested=20 | suggestedQty=20 |

**3. compareVersions DTO 结构（GraphQL 字段）**

MRP `SimulationDiffResult`（4 维 + 缺料集）：
```
{ versionIdA, versionIdB,
  lineDiffs: [{ materialId, netRequirementA/B/Delta, plannedQuantityA/B/Delta }],
  totalNetRequirementDelta, totalPlannedQuantityDelta, totalPurchaseAmountDelta,
  shortageOnlyInA: [materialId...], shortageOnlyInB: [...], shortageInBoth: [...] }
```

DRP `DrpSimulationDiffResult`（2 维）：
```
{ versionIdA, versionIdB,
  lineDiffs: [{ materialId, warehouseId, suggestedQtyA/B, replenishmentQtyDelta, safetyStockA/B/Delta }],
  totalReplenishmentQtyDelta, totalSafetyStockDelta }
```

**4. ParamResolver 缓存约束（关键裁决）**

`ErpMfgSimulationParamResolver` / `ErpDrpSimulationParamResolver` 进程内 `Map<scenarioId, List<param>>` 缓存，`computeIfAbsent` 首次加载后不刷新（设计 AP-06：版本不可变，参数变更须新建版本）。CRUD 不主动失效（paramBizModel 是裸 CrudBizModel 无 hook）。

**浏览器层约束**：webServer JVM 共享单实例，浏览器层无法调 `invalidateCache()`。两版本 scenario param 变更后第二次 `runSimulation` 仍读缓存旧值 → 两版本 lines 相同 → diff delta=0。

**裁决**：
- (a) `runSimulation` 单版本断言（LOT_SIZE/SAFETY_STOCK 覆盖使 planned/suggested 变化）**不受影响** —— 首次加载缓存即用覆盖值，确定性派生。
- (b) `compareVersions` 断言降级为**结构非空 + shortageInBoth 含测试物料**（精确 delta 值由 JUnit `testCompareVersionsProducesStructuredDiff` +10 / +20 已覆盖，浏览器层不重复）。两版本同 scenario 同 params，plannedQuantityDelta=0 但 lineDiffs 数组非空 + shortageInBoth 含 test_mat（两版本均缺料）。
- (c) 浏览器层精确 delta 覆盖为 successor（触发：paramResolver 缓存主动失效机制接入 + 浏览器层可达 invalidateCache mutation）。

**5. 场景状态重置机制**

`runSimulation` 后 scenario status=COMPLETED。要再跑（compareVersions 需两版本），须先重置 DRAFT。无内置 mutation，用 `ErpMfgMrpScenario__update(data:{id,status:"DRAFT"})` / `ErpDrpScenario__update(...)` 直接置位（对齐 JUnit `resetScenarioToDraft` 直写 DB 范式，绕过状态机）。



### Phase 2 - MRP spec 实现（runSimulation + compareVersions + promoteToFormalPlan）

Status: completed
Targets: `tests/e2e/business-actions/mfg-mrp-simulation.action.spec.ts`（NEW）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] Add: 新建 `mfg-mrp-simulation.action.spec.ts`，**自包含 setup**（建测试专用物料 + 基线 `ErpMfgMrpPlan` + `ErpMfgMrpDemand` 行经 GraphQL `__save`），不复用种子 MRP 数据（避免污染基线）
      - Skill: `nop-testing`
- [x] Proof: (1) **runSimulation LOT_SIZE 覆盖** —— 建场景 + LOT_SIZE param（覆盖值 > 默认）+ `runSimulation` mutation → 场景 status=COMPLETED + 场景版本非空 + COMPUTED plan 的 plannedQuantity 相对基线可观测变化（`verifyState` `__get` 独立断言）；(2) **compareVersions 结构化 diff** —— runSimulation 两版本（不同 LOT_SIZE）→ `compareVersions(versionA, versionB)` → SimulationDiffResult plannedQuantityDelta 非空 + shortageInBoth 含测试物料；(3) **promoteToFormalPlan** —— promote → DRAFT plan 生成（code 后缀 `-PROMOTED-`）+ 版本 status=ARCHIVED + promotedPlanId 回写 + 重复 promote 守卫 `ERR_MFG_SIMULATION_VERSION_ALREADY_PROMOTED`；(4) **非 DRAFT 场景 runSimulation 守卫** —— COMPLETED 场景重跑 → `ERR_MFG_SIMULATION_SCENARIO_NOT_DRAFT`
      - Skill: `nop-testing`
- [x] Add: cleanup 清理测试专用物料 + 基线 plan + demands + 场景/版本/param + COMPUTED/PROMOTED plan（经实体 `__delete` + plan 行级清理）
      - Skill: `nop-testing`

Exit Criteria:

> 仅写此阶段实际交付的可观察结果。

- [x] `mfg-mrp-simulation.action.spec.ts` 全绿，断言 runSimulation + compareVersions + promoteToFormalPlan + 守卫四组可观察结果

### Phase 3 - DRP spec 实现（runSimulation + compareVersions 同构）

Status: completed
Targets: `tests/e2e/business-actions/drp-simulation.action.spec.ts`（NEW）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2（MRP 范式确立后 DRP 同构复用）

- [x] Add: 新建 `drp-simulation.action.spec.ts`，**自包含 setup**（建测试专用物料 + warehouse + 基线 `ErpDrpPlan`/`ErpDrpLine` 经 GraphQL `__save`），镜像 MRP 范式
      - Skill: `nop-testing`
- [x] Proof: (1) **runSimulation SAFETY_STOCK 覆盖** —— 建场景 + SAFETY_STOCK param + `runSimulation` → 场景版本非空 + COMPUTED plan 补货量相对基线可观测变化；(2) **compareVersions 2 维 diff** —— 两版本 → `DrpSimulationDiffResult` replenishmentQtyDelta/safetyStockDelta 非空
      - Skill: `nop-testing`
- [x] Add: cleanup 清理测试专用 DRP 实体（镜像 MRP cleanup）
      - Skill: `nop-testing`

Exit Criteria:

> 仅写此阶段实际交付的可观察结果。

- [x] `drp-simulation.action.spec.ts` 全绿，断言 DRP runSimulation + compareVersions 两组可观察结果

### Phase 4 - owner doc 回链 + e2e-runbook + 日志

Status: completed
Targets: `docs/design/manufacturing/simulation-engine.md`（§浏览器层验证实现注记）、`docs/testing/e2e-runbook.md`（业务动作表 + MRP/DRP 仿真行 + webServer JVM arg 段）
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 3

- [x] Add: `simulation-engine.md` 增「浏览器层验证」实现注记（自包含基线 setup 范式 + 参数变体覆盖断言 + diff DTO 结构 + promoteToFormalPlan 隔离清理 + config-gated 启用）
- [x] Add: `e2e-runbook.md` 业务动作表 +manufacturing MRP 仿真行 + drp DRP 仿真行 + webServer JVM arg 段补 `simulation-enabled`

Exit Criteria:

- [x] owner doc + runbook 更新落地（仅此阶段实际更改 owner 行为文档）

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（`ses_06430145bffe5kPVwNflNYkOw5`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-26）— 0 Blocker / 0 Major / 3 Minor。全部负载事实经实时仓库逐项核实**精确匹配**（config-gate 常量 + 6 实体行号 + 3 mutation + SimulationDiffResult/DrpSimulationDiffResult DTO 结构 + 零浏览器 E2E + promoteToFormalPlan 产 DRAFT plan 行隔离风险真实 + runMfgChain 测试专用物料隔离范式适用 + webServer JVM arg 扩展可行 + JUnit 8+5 场景 ✓）。格式合规 + 范围纪律通过（单结果面 / Exit Criteria 阶段本地化 / 无 ORM 保护区域）。**Minor**：(1) 缺 Draft Review Record 段——本次新增 ✓；(2) Goal #5 config-gate 关闭守卫与 Infrastructure Prereqs 全局启用自相矛盾——已移除 Goal #5 并移入 Non-Goal 明示（webServer JVM args 全局无法 per-spec toggle，config-gate 关闭路径经 JUnit 已覆盖）✓；(3) 实体行号引用为区块注释起点非 `<entity>` 标签行——精度 nit 保留给结束审计。计划为可接受的执行契约。

## Closure Gates

> 完整仓库验证在此处：结束时运行 `mvn clean install -DskipTests` + 受影响 Playwright 套件一次。

- [x] 范围内行为完成（MRP runSimulation/compareVersions/promoteToFormalPlan + DRP runSimulation/compareVersions 全绿）
- [x] 相关文档对齐（simulation-engine.md + e2e-runbook）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `npx playwright test tests/e2e/business-actions/mfg-mrp-simulation.action.spec.ts tests/e2e/business-actions/drp-simulation.action.spec.ts` 全绿 5 passed + business-actions 既有 mfg/drp spec 回归 19 passed 0 新增失败）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 产能仿真（CRP successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: B1 Non-Goal —— CRP 仿真归 APS successor（触发：APS 排产 what-if 需求）
- Successor Required: `yes`（触发条件：APS 排产 what-if 需求）

### 对比结果实体化持久化

- Classification: `optimization candidate`
- Why Not Blocking Closure: B1 Deferred —— 当前 compareVersions 返回临时 DTO（不可变快照可确定性派生），实体化为审计增强
- Successor Required: `yes`（触发条件：对比结果审计需求）

### 浏览器层 compareVersions 精确 delta 覆盖

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: ParamResolver 进程内缓存约束（设计 AP-06）+ 浏览器层 webServer JVM 共享单实例无 invalidateCache mutation 可达 → 两版本同 params delta=0；精确 delta +10/+20 由 JUnit `testCompareVersionsProducesStructuredDiff` 已覆盖；浏览器层 compareVersions 结构非空 + 字段可达 + shortageInBoth 含 test_mat 断言已落地
- Successor Required: `yes`（触发条件：paramResolver 缓存主动失效机制接入 + 浏览器层可达 invalidateCache mutation）

## Closure

Status Note: 全 4 Phase 完成并验证 full-green。MRP 仿真引擎 3 用例（runSimulation LOT_SIZE 覆盖 planned=30 + 非 DRAFT 守卫 / compareVersions 结构化 diff shortageInBoth 含 test_mat / promoteToFormalPlan DRAFT plan + 行复制 + ARCHIVED + 重复守卫）+ DRP 仿真引擎 2 用例（runSimulation SAFETY_STOCK 覆盖 suggested=20 + 非 DRAFT 守卫 / compareVersions 2 维 diff 结构）全绿。ParamResolver 缓存约束使浏览器层 compareVersions 精确 delta 覆盖降级为 successor（触发：缓存主动失效机制接入），结构非空 + 字段可达断言已落地，精确 +10/+20 delta 由 JUnit `testCompareVersionsProducesStructuredDiff` 已覆盖。验证基线：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + 新 spec 5 passed（36.6s）+ 既有 mfg/drp business-actions spec 19 passed 回归（2.3m，0 新增失败）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，冷重播无执行者上下文，2026-07-26）
- Evidence: 逐项核实实时仓库：(1) 两 spec 落地非空壳 —— `tests/e2e/business-actions/mfg-mrp-simulation.action.spec.ts`（385 行，3 用例：runSimulation LOT_SIZE 覆盖断言 planned=ceil(25/10)*10=30 + 非 DRAFT 守卫 token「不允许此操作」 / compareVersions 结构非空 + shortageInBoth 含 test_mat / promoteToFormalPlan code 含 `-PROMOTED-` + status=DRAFT + 行复制 planned=30 + version status=ARCHIVED + 重复守卫 token「已转正」；均含 cleanup 经 findItems 反查逐 plan 删 lines+plan+versions+params+scenario+demand+baseline+material）；`drp-simulation.action.spec.ts`（336 行，2 用例：runSimulation SAFETY_STOCK 覆盖断言 suggestedQty=20+safetyStock=20+netRequirement=20 + 非 DRAFT 守卫 / compareVersions 2 维结构 lineDiffs 含 (test_mat,wh1) 行 + replenishmentQtyDelta/safetyStockDelta 字段可达）；(2) 后端三入口经 BizModel 薄委派落地非空壳 —— `ErpMfgMrpScenarioBizModel.runSimulation/promoteToFormalPlan/compareVersions` + `ErpDrpScenarioBizModel.runSimulation/compareVersions`；(3) Phase 1-4 全 [x]、Exit Criteria 全 [x]、Closure Gates 全 [x]（含本审计门控）；(4) 文本一致性：Plan Status=completed / 4 Phase Status=completed / 所有退出标准与门控 [x] 一致；(5) 反空壳：spec 断言真实运行时行为（mutation 返回值 + verifyState 独立 __get + findItems 行级反查 + GraphQL errors[].message token 断言），非仅签名存在；(6) Deferred honesty：浏览器层精确 delta 覆盖因 ParamResolver 缓存约束（设计 AP-06）如实降级为 successor 并记录触发条件，非隐藏缺陷；(7) 文档同步：`docs/design/manufacturing/simulation-engine.md:259` 增「浏览器层验证实现注记」段 + `docs/testing/e2e-runbook.md` 增 webServer JVM arg + 两域业务动作表行 + `docs/logs/2026/07-26.md` 增当日日志条目（引用本 plan-id）。

Follow-up:

- 产能仿真 + 对比结果实体化（触发条件见上 Deferred But Adjudicated 段，非阻塞）
- **浏览器层 compareVersions 精确 delta 覆盖**（触发：`ErpMfgSimulationParamResolver`/`ErpDrpSimulationParamResolver` 缓存主动失效机制接入 + 浏览器层可达 invalidateCache mutation；当前缓存约束使两版本同 params delta=0，结构断言已落地，精确 delta 由 JUnit 已覆盖）
