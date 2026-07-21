# MRP/DRP 仿真引擎（Simulation Engine）

> Owner Doc（NEW，plan `2026-07-22-1000-2`）。落地于 `deepening-roadmap.md §Milestone B §B1`。
> 权威 ORM 源：`module-manufacturing/model/app-erp-manufacturing.orm.xml`、`module-drp/model/app-erp-drp.orm.xml`。
> 单次确定性引擎基线：[`mrp.md`](mrp.md)、`drp/safety-stock-optimization.md`、`drp/use-cases.md`。

## 目的与范围

在不修改主数据的前提下，对 MRP/DRP 单次确定性引擎做**确定性参数变体 what-if 仿真**：用户为同一基线计划创建多个场景版本，每个版本承载一组参数覆盖（提前期偏移 / 批量 / 安全库存等），运行后产出可对比的计划结果快照，并支持结构化 diff 与转正式计划。

本域负责：场景-版本实体、参数变体覆盖模型、仿真计算编排、结果对比引擎、DRP 同构对应物。

本域不负责：产能仿真（CRP 仿真归 APS successor）、概率/蒙特卡洛仿真、物料级 fixedLotSize/minOrderQty/maxOrderQty 主数据列新增（须 ask-first；本计划以场景级覆盖值承载）、仿真结果自动释放为正式采购单/工单（既有单次引擎释放路径不变）。

## 场景-版本模型

### 概念分层（Decision A 裁决：场景 1:N 版本）

```
ErpMfgMrpScenario（场景，1）           ErpDrpScenario（DRP 同构）
  ├─ code / orgId / baseMrpPlanId      ├─ code / orgId / baseDrpPlanId
  ├─ description / status              ├─ description / status
  └─ params（参数变体覆盖集，N）        └─ params（参数变体覆盖集，N）
       │                                    │
       ▼                                    ▼
  ErpMfgMrpScenarioVersion（版本，N）   ErpDrpScenarioVersion
  ├─ scenarioId / versionNo            ├─ scenarioId / versionNo
  ├─ computedMrpPlanId（结果引用）     ├─ computedDrpPlanId（结果引用）
  ├─ snapshotSummary                   ├─ snapshotSummary
  └─ status / promotedPlanId           └─ status / promotedPlanId
```

**Decision A 裁决**：选「场景 1:N 版本」而非「扁平场景每次计算即一版本」。理由：

1. **successor 可追溯性**：同一业务假设（如"提前期 +3 天"）下可有多版本（粗调/细调/最终），场景作为语义分组便于历史回溯。
2. **对比基线稳定性**：对比发生在同场景不同版本之间（A.v1 vs A.v2），基线（basePlan + params）一致；跨场景对比为 successor（业务语义不同）。
3. **DRP 同构性**：DRP 场景版本结构与 MRP 一一对应，便于跨引擎复用对比算法。
4. **替代方案被否决理由**：扁平场景丢失分组语义，每次计算后无法回答"这组 what-if 属于哪个假设"。

**版本引用而非冗余**：`computedMrpPlanId` / `computedDrpPlanId` 引用既有 COMPUTED 结果 `ErpMfgMrpPlan` / `ErpDrpPlan`，不重复行数据（既有 plan-line 实体已可承载快照，且保留完整 pegging/scheduledReceipt 字段）。`snapshotSummary` 仅存聚合摘要（总净需求 / 总建议量 / 缺料物料数）便于列表渲染。

## 参数变体覆盖语义

### 覆盖范围（Decision B 裁决）

本期覆盖 3 类参数子集（MRP 侧），DRP 侧覆盖 3 类同构子集：

| 参数类型 | MRP（`erp-mfg/simulation-param-type`） | DRP（`erp-drp/simulation-param-type`） | 覆盖目标（替代的单次引擎读取点） |
|---------|----------------------------------------|----------------------------------------|----------------------------------|
| 提前期 | `LEAD_TIME`（偏移天数，整数） | `LEAD_TIME` | MrpEngine.purLeadDays / mfgLeadDays；DrpEngine.roundToMultiple 不读提前期，仅 DrpParameter.replenishmentLeadTime |
| 批量 | `LOT_SIZE`（全局整数） | `REPLENISHMENT_QTY`（订货倍数） | MrpEngine.lotSize 读 `AppConfig.var(CONFIG_MRP_DEFAULT_LOT_SIZE)`；DrpEngine.roundToMultiple 读 ErpDrpParameter.orderMultiple |
| 安全库存 | `SAFETY_STOCK`（数量） | `SAFETY_STOCK` | DemandAggregator.collectSafetyStockDemands 读 ErpMdMaterial.safetyStock；DrpEngine 读 ErpDrpParameter.safetyStock |

### 回退顺序（Decision B 裁决）

```
解析某 materialId 的某参数类型 paramType：
  1. 场景参数中 materialId 精确匹配的覆盖值     ← 最优先
  2. 场景参数中 materialId=null（全局覆盖）值    ← 次优先
  3. 全局配置 / 单次引擎默认值                  ← 兜底（lotSize=single-run 配置；leadTime=主数据；safetyStock=主数据）
```

### 覆盖粒度（Decision B 裁决）

- **本期粒度**：按物料（`materialId` 非空）+ 全局（`materialId=null`）。物料类别粒度（`materialCategoryId`）为 successor，触发条件：业务方明确按类别批量覆盖需求。
- **理由**：物料级覆盖是最小有意义粒度，可精确表达"仅对 M1 提前期 +3 天"；全局覆盖便于"所有物料 lot size=10"。类别粒度增加解析复杂度且本期无明确业务驱动。

### 物料级列缺失的处理（Decision B 记录）

`mrp.md §实现偏离补注 line 89` 明确：物料级 `fixedLotSize`/`minOrderQty`/`maxOrderQty`/`safetyStock` 列在 ORM 不存在（lot sizing 简化为全局配置；safety stock 在 master-data 有列，lot size 列无）。

**本计划处理**：
- 仿真场景的 lot size 覆盖值承载于 `ErpMfgMrpScenarioParam.paramValue`（场景级），不修改 `ErpMdMaterial` 主数据
- safety stock 覆盖值同样承载于场景参数（master-data 列虽存在但不修改，保证单次引擎读到的主数据不变）
- 这与 Non-Goal「不动主数据」一致；物料级批量精细化列加列须 ask-first（Deferred successor）

### 参数变体解析（Strategy+registry 范式）

`IErpMfgSimulationParamResolver` SPI（MRP）+ `IErpDrpSimulationParamResolver`（DRP），对齐 D3 plan `2026-07-21-2225-2` 的 `CostingStrategy` + A3 plan `2026-07-22-1000-1` 的 `IErpFinTransferPriceResolver` 范式：

- 输入：`scenarioId` + `materialId`（nullable）+ `paramType`
- 输出：覆盖值（如未覆盖返回 `null`，调用方回退全局配置/主数据）
- 实现：进程内一次性加载场景参数到 `Map<ParamKey, BigDecimal>` 缓存，O(1) 查询；CRUD 不主动失效（场景版本一旦生成即不可变，参数变更须新建版本）

## 结果对比算法

### 对比维度（Decision C 裁决）

**MRP 版本对比**（4 维）：

| 维度 | 计算方式 | 输出字段 |
|------|---------|---------|
| 净需求差 | 同 materialId 的 netRequirement(A) − netRequirement(B) | `netRequirementDelta` |
| 建议量差 | 同 materialId 的 plannedQuantity(A) − plannedQuantity(B) | `plannedQuantityDelta` |
| 缺料物料集差 | netRequirement > 0 的物料集 A\B（仅 A 缺料）/ B\A（仅 B 缺料）/ A∩B（两者均缺料） | `shortageOnlyInA` / `shortageOnlyInB` / `shortageInBoth` |
| 总采购额差 | Σ(PURCHASE_REQUEST.plannedQuantity × material.standardCost) A − B | `totalPurchaseAmountDelta` |

**DRP 版本对比**（2 维）：

| 维度 | 计算方式 | 输出字段 |
|------|---------|---------|
| 补货量差 | 同 (materialId, warehouseId) 的 suggestedQty(A) − suggestedQty(B) | `replenishmentQtyDelta` |
| 安全库存差 | 同 (materialId, warehouseId) 的 safetyStock(A) − safetyStock(B) | `safetyStockDelta` |

### diff 结果载体（Decision C 裁决）

选「临时查询返回 DTO」而非「新对比结果实体」。理由：

1. **不可变快照语义**：版本一旦生成即不可变，对比结果可由两版本快照**确定性派生**，无需持久化。
2. **避免实体膨胀**：持久化对比结果需新建实体 + 字典 + codegen + view，本期 ROI 低。
3. **使用模式**：对比经 `@BizQuery compareVersions(versionIdA, versionIdB)` 即时返回 DTO 列表，前端 dialog 渲染。
4. **替代方案被否决理由**：实体化便于历史审计，但本期无明确"对比结果审计"业务驱动；successor 可加实体（触发条件：业务方明确对比结果需长期留存）。

### 对比算法实现

```java
public class SimulationVersionComparator {
    public SimulationDiffResult compareMrpVersions(Long versionIdA, Long versionIdB) {
        // 1. 加载两版本的 computedMrpPlanId → ErpMfgMrpPlanLine 集合
        // 2. 按 materialId 对齐（union 键集）
        // 3. 逐物料计算 4 维 delta
        // 4. 计算缺料物料集差（set difference）
        // 5. 累加总采购额（查 material.standardCost）
        // 6. 返回 SimulationDiffResult（含行级 diff 列表 + 聚合摘要）
    }
}
```

### 可比性校验

- MRP 两版本须同 orgId（不跨公司对比，对齐 `mrp.md §关键业务规则 line 60`）
- 不同 orgId 或不同基线 plan 抛 `ERP_MFG_SIMULATION_VERSIONS_NOT_COMPARABLE`
- DRP 同型（`ERP_DRP_SIMULATION_VERSIONS_NOT_COMPARABLE`）

## 与单次引擎关系（Decision E 裁决：E2 fork）

### 核心约束（Current Baseline 经仓库核实）

`MrpEngine.runMrp(Long planId, List<ErpMfgMrpDemand> demands)`（`MrpEngine.java:77`）签名**无覆盖上下文参数**。lot size 读全局 `AppConfig.var(ErpMfgConstants.CONFIG_MRP_DEFAULT_LOT_SIZE)`（`lotSize():159-163`）；制造提前期读全局 `AppConfig.var(CONFIG_MFG_LEADTIME_DAYS_PER_ROUTING_HOUR)`（`mfgLeadDays():173-185`）；采购提前期读主数据 `ErpMdMaterial.getLeadTimeDays()`（`purLeadDays():191-196`）。safety stock 在上游 `DemandAggregator.collectSafetyStockDemands:123-150` 整合（读 `ErpMdMaterial.getSafetyStock()`）。

**约束含义**：仿真"参数覆盖不改主数据、不动单次引擎代码路径"**无法经纯包装实现**。

### Decision E 裁决：选 E2（fork）

在两个候选间裁决：

| 候选 | 机制 | 单次路径触及 | 回归暴露面 | 算法重复 |
|------|------|------------|-----------|---------|
| **E1** | 重构 MrpEngine+DemandAggregator 引入可选覆盖上下文，缺省与现状逐位等价 | 是（共享代码被触及） | 高（缺省须证明逐位等价） | 无 |
| **E2（选中）** | fork `SimulationMrpEngine` 复用算法但替换全局/主数据读取为覆盖上下文 | 否（零触及） | 零（既有 200+ manufacturing 测试不变） | 有（~150 行 MRP 算法 fork） |

**选 E2 的理由**：

1. **零回归**：单次 MRP/DRP 路径完全不被触及，既有 200+ manufacturing service 测试 + 50+ drp service 测试无需重新证明等价性。E1 的"缺省等价"虽可经既有测试验证，但任何 MrpEngine 后续改动须同步考虑覆盖上下文分支，长期维护负担。
2. **算法稳定**：MRP 算法是稳定领域逻辑（BOM 展开 + 净需求 + lot sizing + 提前期），Non-Goals 明确不引入新算法维度（CRP/概率仿真/物料级批量）。算法漂移风险低。
3. **DRP 同构**：DRP 侧沿用同一 fork 范式（`SimulationDrpEngine`），耦合关闭风险一致（MRP pivot 则 DRP 同步 pivot，不脱离）。
4. **代价可接受**：fork 仅重复 ~150 行 MRP 算法（processMaterial / lotSize / mfgLeadDays / purLeadDays / availableQuantity / topDemandsByMaterial）。DemandAggregator 的 SALES_ORDER/FORECAST/MANUAL 部分无需 fork（不读覆盖参数），仅 SAFETY_STOCK 段需场景化重算。

**残留风险（E2）**：
- 算法漂移：若 MrpEngine 算法后续变更（如引入 scrapRate / 需求时界），SimulationMrpEngine 须同步。**缓解**：本类头部注释明确"算法对齐 MrpEngine.runMrp，任何算法变更须同步"；Non-Goals 已限定本期不引入这些维度。
- 代码重复：~150 行 MRP + ~80 行 DRP。**缓解**：复用 BomExpander（既有独立 bean）；DRP 算法本身较短（DrpEngine ~190 行）。

### E2 fork 的具体边界

**SimulationMrpEngine（NEW，~200 行）**：
- 复用：`BomExpander`（既有 bean，注入）；`DemandAggregator.aggregate()`（既有，注入）—— 用于 SALES_ORDER/FORECAST/MANUAL 需求整合
- fork：MRP 核心算法（processMaterial / lotSize / mfgLeadDays / purLeadDays / availableQuantity / topDemandsByMaterial）
- 新增：SAFETY_STOCK 场景化重算（移除 DemandAggregator 产出的 SAFETY_STOCK 行，按覆盖值重算）
- 入口：`runSimulation(scenarioId)` 编排全流程

**SimulationDrpEngine（NEW，~150 行）**：
- 复用：`DrpDemandAggregator`（既有，注入，但需按覆盖值重算 safetyStock / replenishmentLeadTime / orderMultiple）
- fork：DRP 净需求算法（`DrpEngine.runDrp` 的核心循环 + `roundToMultiple` + `decideReplenishmentType`）
- 入口：`runSimulation(scenarioId)`

**单次路径保证**：
- `MrpEngine` / `DemandAggregator` / `DrpEngine` / `DrpDemandAggregator` / `SafetyStockEngine` 类文件**零修改**
- 既有 200+ manufacturing + 50+ drp 测试无需改动，零回归

## 与既有 Forecast 输入边界

`ErpMfgForecast`（运营需求预测，产品×时间桶）已落地（plan `2026-07-05-0427-1`）。仿真场景的需求数据**直接复用**既有 Forecast 行作为输入，不重复预测维度（`mrp.md §CRM 销售预测 vs 运营需求预测 line 97-104` 已裁决语义边界）。

**复用路径**：
1. 仿真场景的基线 plan（`baseMrpPlanId`）在创建时已通过 `DemandAggregator.collectForecastDemands` 整合 APPROVED 预测行
2. 仿真运行时基于基线 plan 的 demands 快照，不重新触发 Forecast 消费（避免预测状态 CONSUMED 漂移）
3. 若用户需测试"无预测"场景，可在场景参数中显式置 Forecast 系数为 0（successor，本期 paramType 不含 FORECAST_SCALE）

**Decision 记录**：本期不引入 `FORECAST_SCALE` 参数类型（覆盖预测倍数），因 Forecast 消费在 DemandAggregator 上游且涉及状态机（CONSUMED 回写）；场景级预测倍数为 successor，触发条件：业务方明确"按预测敏感度分组仿真"需求。

## DRP 对应物（同构包装）

DRP 侧与 MRP 侧完全同构，仅参数类型字典与覆盖目标不同：

| MRP | DRP | 同构点 |
|-----|-----|--------|
| `ErpMfgMrpScenario` | `ErpDrpScenario` | 字段结构、状态机一致 |
| `ErpMfgMrpScenarioVersion` | `ErpDrpScenarioVersion` | computedXxxPlanId 引用对应实体 |
| `ErpMfgMrpScenarioParam` | `ErpDrpScenarioParam` | paramType 字典不同（DRP 含 REPLENISHMENT_QTY） |
| `SimulationMrpEngine` | `SimulationDrpEngine` | fork 对应单次引擎 |
| `IErpMfgSimulationParamResolver` | `IErpDrpSimulationParamResolver` | SPI 同型 |
| `compareVersions` MRP | `compareVersions` DRP | 对比维度不同（DRP 2 维 vs MRP 4 维） |

**DRP 仓库补货参数场景级覆盖**：`ErpDrpParameter`（仓库补货参数，含 safetyStock / replenishmentLeadTime / orderMultiple）经场景参数覆盖；不修改 `ErpDrpParameter` 主数据。

## 仿真到正式 plan 的转正路径（Decision D 裁决）

### 裁决：仅产建议 + 显式转正

**本期仅产建议**（Non-Goal 自动释放）：仿真场景版本产 COMPUTED 结果 plan（ErpMfgMrpPlan/ErpDrpPlan），不自动释放为采购单/工单。

**显式转正**：用户从选定场景版本调用 `promoteToFormalPlan(scenarioVersionId)` `@BizMutation`，语义：
1. 校验场景版本状态 = COMPLETED
2. 创建新 `ErpMfgMrpPlan`（DRAFT，复制基线 plan 的 code/orgId/businessDate/planningHorizonDays，code 加 `-PROMOTED-{versionNo}` 后缀避免唯一键冲突）
3. 复制场景版本的 `computedMrpPlanId` 对应 plan 的所有 `ErpMfgMrpPlanLine`（保留 materialId/netRequirement/plannedQuantity/plannedDate，重置 isFirmed=false / convertedBillCode=null）
4. 回写场景版本 `promotedPlanId` + status=ARCHIVED（防止重复转正）
5. 转正后的 DRAFT plan 走既有单次引擎释放路径（用户手动调用 `releasePurchaseRequest` / `releaseWorkRequest`）

**Decision D 理由**：
- 既有单次释放路径不变（`mrp.md §建议单释放` 的 releasePurchaseRequest/releaseWorkRequest）
- 转正生成 DRAFT 而非直接释放，给用户最终审核机会（仿真结果可能与预期不符）
- `promotedPlanId` 防重复转正（场景版本一旦转正即 ARCHIVED）

**Decision D 替代方案被否决理由**：
- 自动释放（场景运行后直接生成采购单/工单）：风险高，无人工审核
- 转正生成 FIRMED 而非 DRAFT：跳过用户审核，与既有"释放前须人工确认"模式不一致

## 反模式自检表

| # | 反模式 | 正确做法 |
|---|--------|---------|
| AP-01 | 在 `MrpEngine` / `DemandAggregator` / `DrpEngine` 中加覆盖参数 | 仿真覆盖只在 `SimulationMrpEngine` / `SimulationDrpEngine` 中处理（E2 零触及） |
| AP-02 | 修改 `ErpMdMaterial.safetyStock` / `ErpDrpParameter` 主数据承载覆盖值 | 覆盖值只存 `ErpXxxScenarioParam.paramValue` |
| AP-03 | 持久化对比结果为新实体 | 对比结果由两版本快照确定性派生，返回 DTO 即可 |
| AP-04 | 跨 orgId 对比版本 | 须同 orgId，对齐 `mrp.md §MRP 范围` |
| AP-05 | 仿真运行后自动释放采购单/工单 | 仅产 COMPUTED plan，转正须显式 `promoteToFormalPlan` |
| AP-06 | 场景版本一旦生成后仍允许修改参数 | 版本不可变；参数变更须新建版本（保证对比基线稳定） |
| AP-07 | 仿真入口未 config-gated | `erp-mfg.simulation-enabled` / `erp-drp.simulation-enabled` 默认 false，门控仿真入口 |
| AP-08 | 在 `IErpMfgSimulationParamResolver` 中写库 | 解析器只读，CRUD 经标准实体 BizModel |
| AP-09 | 物料级 fixedLotSize/minOrderQty 主数据列加列（不经 ask-first） | 场景级 paramValue 承载，主数据加列须 ask-first（Deferred successor） |
| AP-10 | 引入 FORECAST_SCALE 参数（覆盖预测倍数） | 本期 Non-Goal；Forecast 消费在 DemandAggregator 上游涉及状态机 |

## 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-mfg.simulation-enabled` | false | 门控 MRP 仿真入口（runSimulation / promoteToFormalPlan）；false 时抛错 |
| `erp-drp.simulation-enabled` | false | 门控 DRP 仿真入口；false 时抛错 |

**门控语义**：门控**仅保护仿真入口**，不保护单次 MRP/DRP 路径回归（单次路径经 E2 零触及保证；门控默认关闭是为避免仿真入口在生产环境意外触发，须显式启用）。

## 与既有 owner doc 关系

- [`mrp.md`](mrp.md) §仿真引擎关系（回链）：单次 MRP 引擎不变，仿真包装经 `SimulationMrpEngine` fork；参数变体经 `IErpMfgSimulationParamResolver`；对比经 `compareVersions` 返回 DTO
- `drp/safety-stock-optimization.md` / `drp/use-cases.md`（回链）：单次 DRP / SafetyStockEngine 不变，仿真包装经 `SimulationDrpEngine`
- `manufacturing/README.md`（回链）：本域文档表追加 `simulation-engine.md`
- DRP 域 owner doc（回链）：追加 DRP 场景对应物段
