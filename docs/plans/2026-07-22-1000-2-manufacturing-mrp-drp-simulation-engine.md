# 2026-07-22-1000-2 manufacturing-mrp-drp-simulation-engine

> Plan Status: active
> Last Reviewed: 2026-07-22
> Source: `docs/backlog/deepening-roadmap.md` §Milestone B §B1（line 59/85 — MRP/DRP Simulation Engine，**需要仿真场景/参数实体 ORM 变更**）；`docs/design/manufacturing/mrp.md`（既有单次确定性 MRP 计算）；`docs/analysis/erp-survey/2026-07-20-post-survey-strategic-gaps.md`（对照 OFBiz/Wimoor 多场景仿真能力识别的应用层增强项）
> Related: `2026-07-02-2237-2-manufacturing-mrp-engine.md`（单次 MRP 引擎基线）；`2026-07-05-0427-1-demand-forecast-entity-mrp-drp-source.md`（需求来源）；DRP 单次引擎 `2026-07-04-1115-2-drp-net-requirement-safety-stock.md`
> Audit: required

## Current Baseline

**单次确定性 MRP（已就绪）**：
- `ErpMfgMrpPlan`（`module-manufacturing/model/app-erp-manufacturing.orm.xml:768`）：`code`/`orgId`/`businessDate`/`planningHorizonDays`/`status`（字典 `erp-mfg/mrp-status`）/`remark`，含 to-many `lines`（`ErpMfgMrpPlanLine`）+ `demands`（`ErpMfgMrpDemand`）。
- `ErpMfgMrpPlanLine`（`:807`）：`materialId`/`uoMId`/`orderType`/`grossRequirement`/`netRequirement`/`plannedQuantity`/`plannedDate`/`parentLineId`（多级 BOM 展开）/`isFirmed`/`convertedBillCode`。
- `MrpEngine`（plan `2026-07-02-2237-2`）：单次确定性计算（需求整合 → 可用量 → BOM 多级展开 → 净需求 → lot sizing → 建议单）。
- 配置项（`mrp.md` §配置选项 line 72-82）：`erp-mfg.default-lot-size`（全局 lot sizing）、`erp-mfg.mfg-leadtime-days-per-routing-hour`（提前期换算）、`erp-mfg.forecast-consume-enabled`。物料级 `fixedLotSize`/`minOrderQty`/`maxOrderQty`/`safetyStock` 列 **不存在**（mrp.md §实现偏离补注 line 89：本期 lot-for-lot 为主，物料级批量精细化须 ask-first 加列）。

**单次确定性 DRP（已就绪）**：
- `ErpDrpPlan`（`module-drp/model/app-erp-drp.orm.xml:68`）+ `ErpDrpLine`（`:111`）+ `ErpDrpParameter`（`:170`，仓库补货参数，含 safetyStock 可写回）。`SafetyStockEngine`（plan `2026-07-04-1115-2`）单次计算。

**仿真缺口（核心）**：
- **无场景/版本实体**：无法在不修改主数据的前提下计算"如果提前期 +3 天 / 安全库存翻倍 / 批量上调"的 what-if 结果。
- **无参数变体模型**：lead time / lot size / safety stock 的场景级覆盖值无处承载（mrp.md line 89 明确物料级批量列缺失）。
- **无结果对比**：单次 plan 间无结构化 diff（净需求差 / 建议量差 / 缺料差）。
- **DRP 无对应场景**：DRP 单次计算同样无仿真包装。

**`MrpEngine` 代码形状（关键约束，经仓库核实）**：仿真"参数注入不改主数据"的可行性取决于单次引擎是否暴露每调用覆盖钩子，实际**不暴露**：
- 唯一公共入口 `MrpEngine.runMrp(Long planId, List<ErpMfgMrpDemand> demands)`（`module-manufacturing/erp-mfg-service/.../mrp/MrpEngine.java:77`）—— 签名**无覆盖上下文参数**。
- lot size 读全局 `AppConfig.var(ErpMfgConstants.CONFIG_MRP_DEFAULT_LOT_SIZE)`（`lotSize():159-163`）；制造提前期读全局 `AppConfig.var(CONFIG_MFG_LEADTIME_DAYS_PER_ROUTING_HOUR)`（`mfgLeadDays():173-185`）；采购提前期读主数据 `ErpMdMaterial.getLeadTimeDays()`（`purLeadDays():191-196`）—— 三者均绑定私有方法内部的全局配置/主数据，非经入参线程化。
- safety stock **不在 MrpEngine**，而在上游 `DemandAggregator.aggregate` 整合独立需求行（mrp.md:19 + MrpEngine.java:37 类注释）—— 即"safety stock 场景级覆盖"目标是 `DemandAggregator` 而非 MrpEngine。

**约束含义**：仿真"参数覆盖不改主数据、不动单次引擎代码路径"**无法经纯包装实现**。可行路径有二（归 Phase 0 Decision E 裁决）：(E1) 重构 `MrpEngine`/`DemandAggregator` 引入可选覆盖上下文（缺省时与现状行为逐位等价 → 单次路径语义不变但触及共享代码，回归测试负担）；(E2) fork 出 `SimulationMrpEngine` 复用算法但替换全局/主数据读取为覆盖上下文（单次路径零触及 → 零回归，但算法重复）。二者取舍为本计划核心开放裁决，Phase 0 必须先 resolve。

**与既有 Forecast 的关系**：`ErpMfgForecast`（运营需求预测，产品×时间桶）已落地；仿真场景的需求数据可引用既有 Forecast 行作为输入，不重复预测维度（mrp.md §CRM 销售预测 vs 运营需求预测 line 97-104 已裁决语义边界）。

## Goals

- 新建 `docs/design/manufacturing/simulation-engine.md` owner doc（NEW），定义仿真场景模型、参数变体覆盖语义、结果对比算法、与单次 MRP/DRP 引擎的关系、与既有 Forecast 的输入边界、反模式自检表。
- 落地**仿真场景 + 场景版本**实体（MRP 侧 + DRP 对应物），承载可重复、可对比的场景实例。
- 落地**参数变体模型**：场景级覆盖 lead time（提前期偏移天数）/ lot size（批量）/ safety stock（安全库存）而不修改主数据；覆盖未设置时回退既有全局配置/单次引擎默认值。**实现机制（重构 MrpEngine 接受覆盖上下文 vs fork SimulationMrpEngine）由 Phase 0 Decision E 裁决**，非本 Goals 预设。
- 落地**结果对比引擎**：两场景版本间的结构化 diff（净需求差 / 建议量差 / 缺料物料集差），产出可读对比结果。
- 落地 DRP 场景对应物（同构包装单次 DRP，仓库补货参数场景级覆盖）。
- 复用既有 Strategy+registry 范式（见 D3 plan `2026-07-21-2225-2`）承载参数变体覆盖解析，保持与 CostingStrategy / TransferPriceResolver 一致性。

## Non-Goals

- 产能仿真（CRP 仿真归 APS successor；本计划仅 MRP/DRP 物料层 what-if）
- 多场景并行蒙特卡洛/概率分布仿真（本计划为确定性参数变体对比，非概率仿真）
- 物料级 `fixedLotSize`/`minOrderQty`/`maxOrderQty`/`safetyStock` 主数据列新增（mrp.md line 89 明确须 ask-first；本计划以场景级覆盖值承载，不动主数据）
- 仿真结果自动释放为正式采购单/工单（既有单次引擎的释放路径不变；仿真场景仅产建议，释放经用户显式从选定场景版本转正式 plan）
- APS 排产甘特图/可视化仿真（归 F16 APS 复杂页面 successor）
- 跨公司需求合并仿真（mrp.md §关键业务规则 line 60：MRP 按 orgId 独立运行不跨公司；本计划遵守同约束）

## Task Route

- Type: `app-layer design change`（owner doc NEW + ORM 实体 + 仿真引擎，manufacturing 域）
- Owner Docs: `docs/design/manufacturing/simulation-engine.md`（NEW）、`docs/design/manufacturing/mrp.md`（回链：仿真与单次引擎关系）、`docs/design/manufacturing/README.md`（回链：本域文档表 + 仿真段）、DRP 域 owner doc（回链：DRP 场景对应物）
- Skill Selection Basis: `nop-backend-dev`（BizModel + Processor + Strategy+registry + 错误码）、`nop-frontend-dev`（view.xml 场景/版本/对比页）。非 Bug 调试故不加载 `nop-debugging`；非测试基础设施新建故不加载 `nop-testing`。
- **MRP + DRP 单计划捆绑裁决（规则 4 vs 14）**：MRP 仿真（module-manufacturing）与 DRP 对应物（module-drp）虽跨模块，但共享同一「仿真场景-版本-参数变体-结果对比」结果面与 4 个共享 Decision（A-D），属同一概念（物料层 what-if 仿真引擎）的两个应用面。故按规则 14「同概念多应用面 = 单 owner plan」捆绑。**耦合关闭风险**：若 Phase 0 Decision E 在 MRP 侧被迫 fork（E2），DRP 侧沿用同一 fork 范式；DRP 不脱离 MRP 独立关闭。DRP 不会因 MRP pivot 被丢弃，仅同步 pivot。

## Infrastructure And Config Prereqs

- 复用既有单次 `MrpEngine` / `SafetyStockEngine` / `ErpMfgForecast` 数据通路，无需新数据源/端口。
- 配置门控：新增 `erp-mfg.simulation-enabled`（默认 false），门控对象为**新增仿真入口**（`runSimulation`/`promoteToFormalPlan` @BizMutation），使其在未启用时不执行。该门控**不保护单次 MRP 路径回归** —— 若 Phase 0 Decision E 选 E1（重构 MrpEngine 接受覆盖上下文），单次共享代码被触及，回归须由"缺省上下文 = 现状行为逐位等价"+ 既有单次 MRP 测试套件保证（非 config-gate 保证）；若选 E2（fork），单次路径零触及、零回归。
- 无外部服务/密钥依赖。
- 回滚策略：ORM 变量为加性新增实体（`mandatory="false"` 默认 null），向后兼容；仿真入口 config-gated 默认关闭。**单次 MRP/DRP 路径是否被触及取决于 Decision E**：E1 = 触及共享代码（须保持缺省等价）；E2 = 零触及。该不确定在 Phase 0 resolve 前，不得在本节声明"单次路径完全不变"。

## Execution Plan

### Phase 0 - Explore + Owner Doc NEW + Decisions

Status: planned
Targets: `docs/design/manufacturing/simulation-engine.md`、`docs/design/manufacturing/mrp.md`（回链）
Skill: `none`

- Item Types: `Decision | Add | Proof`（Explore 项作 Decision 前置 Proof，规则 9）
- Prereqs: 无（单次 MRP/DRP 引擎 + Forecast 已 done）

- [ ] Proof（Explore，规则 9 Decision 前置）：核实 `ErpMfgForecast` 行作为场景需求输入的复用路径（`MrpEngine` 入参契约已在本计划 Current Baseline 经仓库证据确认无覆盖钩子，此处仅补 Forecast 输入复用证据）
  - Skill: `none`
- [ ] Decision A — 场景与版本关系模型：裁决「场景（Scenario）1:N 版本（Version），版本承载一次计算快照」vs「扁平场景每次计算即一版本」。考虑 successor 可追溯性与对比基线稳定性
  - Skill: `none`
- [ ] Decision B — 参数变体覆盖范围与回退语义：裁决本期覆盖 lead time/lot size/safety stock 三参数子集；裁决覆盖未设置时回退顺序（场景覆盖 → 全局配置 → 单次引擎默认）；裁决覆盖粒度（按物料/按物料类别/全局）。记录物料级列缺失（mrp.md line 89）下以场景级覆盖值承载的理由
  - Skill: `none`
- [ ] Decision C — 结果对比算法与 diff 结构：裁决对比维度（净需求差/建议量差/缺料物料集差/总采购额差）与 diff 结果载体（新对比结果实体 vs 临时查询返回 DTO）。裁决 DRP 对比维度（补货量差/安全库存差）
  - Skill: `none`
- [ ] Decision D — 仿真到正式 plan 的转正路径：裁决本期仅产建议（Non-Goal 自动释放）；用户从选定场景版本显式「转正式 plan」的入口与语义（生成新 `ErpMfgMrpPlan` DRAFT 引用场景版本）
  - Skill: `none`
- [ ] Decision E（核心）— 仿真覆盖机制：在 E1（重构 `MrpEngine`+`DemandAggregator` 引入可选覆盖上下文，缺省与现状逐位等价 → 单次路径语义不变但触及共享代码，回归测试负担）vs E2（fork `SimulationMrpEngine` 复用算法但替换全局/主数据读取为覆盖上下文 → 单次路径零触及零回归，算法重复）间裁决。记录选择、替代方案、残留风险（E1=回归暴露面；E2=算法漂移维护负担）。**此 Decision resolve 前，后续 Phase 2 架构不定型**
  - Skill: `nop-backend-dev`
- [ ] NEW `docs/design/manufacturing/simulation-engine.md`：目的与范围 / 场景-版本模型 / 参数变体覆盖语义与回退顺序 / 结果对比算法 / 与单次引擎关系（含 Decision E 选择与理由）/ 与 Forecast 输入边界 / DRP 对应物 / 反模式自检表
  - Skill: `none`

Exit Criteria:

> 仅证明此阶段交付可执行 owner doc 与裁决，解除后续 ORM/引擎阶段设计阻塞。

- [ ] `simulation-engine.md` 含场景-版本/参数变体/对比算法/DRP 对应物 4 语义段 + 5 Decision（A-E）落地理由段，含 Decision E 的 E1/E2 取舍与残留风险
- [ ] `MrpEngine` 覆盖钩子缺失已在本计划 Current Baseline 经仓库证据确认（line 见 `MrpEngine.java:77/159/173/191`），Phase 0 不重复验证钩子存在性

### Phase 1 - ORM 实体 + 字典 + Codegen

Status: planned
Targets: `module-manufacturing/model/app-erp-manufacturing.orm.xml`、`module-drp/model/app-erp-drp.orm.xml`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 0 Decisions 落地

- [ ] MRP 侧：新建仿真场景实体（如 `ErpMfgMrpScenario`：code/orgId/baseMrpPlanId（基线引用）/description/status 字典 DRAFT/RUNNING/COMPLETED/ARCHIVED）+ 场景版本实体（如 `ErpMfgMrpScenarioVersion`：scenarioId/versionNo/computedMrpPlanId（引用计算结果 plan）/snapshotSummary/createdBy/createdTime）。场景版本经 `computedMrpPlanId` 引用既有 `ErpMfgMrpPlan` 承载计算结果，不冗余行数据
  - Skill: `nop-backend-dev`
- [ ] MRP 侧：新建参数变体覆盖实体（如 `ErpMfgMrpScenarioParam`：scenarioId/materialId（nullable，null 表示全局覆盖）/paramType 字典 LEAD_TIME/LOT_SIZE/SAFETY_STOCK/paramValue/overrideScope）。承载 Decision B 覆盖语义
  - Skill: `nop-backend-dev`
- [ ] DRP 侧：新建对应场景/版本/参数变体实体（如 `ErpDrpScenario`/`ErpDrpScenarioVersion`/`ErpDrpScenarioParam`，paramType=SAFETY_STOCK/LEAD_TIME/REPLENISHMENT_QTY），同构包装单次 DRP。仓库补货参数经 `ErpDrpParameter` 场景级覆盖
  - Skill: `nop-backend-dev`
- [ ] 字典扩展：新字典 `erp-mfg/simulation-status`（4 键）、`erp-mfg/simulation-param-type`（3 键）、`erp-drp/simulation-param-type`（3 键）
  - Skill: `nop-backend-dev`
- [ ] 经 `mvn clean install -DskipTests` 触发增量 codegen；核对 `_gen` 实体/IBiz/BizModel/xmeta/xbiz/view.yaml 全套产物（manufacturing + drp）
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] MRP 3 + DRP 3 新实体 + 字典 codegen 产物落地，`xmllint --noout` 通过，manufacturing + drp 模块编译期类型检查通过（解除后续引擎阶段阻塞）

### Phase 2 - 参数变体解析 + 仿真计算编排（MRP）

Status: planned
Targets: `module-manufacturing/erp-mfg-service`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 codegen 产物 + **Phase 0 Decision E 已 resolve**

- [ ] `IErpMfgSimulationParamResolver` SPI + 实现：Strategy+registry 范式（对齐 D3 `CostingStrategy` + A3 `TransferPriceResolver`）解析场景级覆盖值，回退顺序 Decision B（场景覆盖 → 全局配置 → 单次引擎默认）
  - Skill: `nop-backend-dev`
- [ ] MRP 仿真计算路径（按 Decision E 选择实现）：E1 = 经重构后的 `MrpEngine.runMrp(planId, demands, overrideContext)`（缺省上下文与现状逐位等价）调用；E2 = fork `SimulationMrpEngine` 以覆盖上下文替换 `AppConfig.var(...)`/`material.getLeadTimeDays()` 读取。safety stock 覆盖经 `DemandAggregator` 对应覆盖入口（Decision E 同步裁决其扩展点）。`runSimulation(scenarioId)` @BizMutation 以基线 plan 为模板 + 参数变体覆盖 → 写新 `ErpMfgMrpPlan`（COMPUTED 结果）+ 场景版本引用。config-gated `erp-mfg.simulation-enabled` 默认 false（门控仿真入口）
  - Skill: `nop-backend-dev`
- [ ] 仿真→正式转正：`promoteToFormalPlan(scenarioVersionId)` @BizMutation —— 从选定场景版本生成新 `ErpMfgMrpPlan` DRAFT（Decision D），不自动释放采购/工单（既有单次释放路径不变）
  - Skill: `nop-backend-dev`
- [ ] 错误码（`ErpMfgErrors.java`）：`ERP_MFG_SIMULATION_SCENARIO_NOT_DRAFT` / `ERP_MFG_SIMULATION_NO_BASELINE_PLAN` / `ERP_MFG_SIMULATION_VERSION_ALREADY_PROMOTED`
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] runSimulation 对参数变体覆盖（lead time +3 / lot size 上调 / safety stock 翻倍）产场景版本 + 引用 COMPUTED plan，净需求/建议量相对基线可观测变化
- [ ] 参数覆盖回退顺序单测全绿（覆盖 → 全局 → 默认）
- [ ] config-gated 默认 false 保护既有单次 MRP 测试零回归

### Phase 3 - 结果对比引擎 + DRP 对应物

Status: planned
Targets: `module-manufacturing/erp-mfg-service`、`module-drp/erp-drp-service`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 MRP 仿真可产多版本

- [ ] MRP 结果对比：`compareVersions(versionIdA, versionIdB)` @BizQuery —— 按 Decision C 维度（净需求差/建议量差/缺料物料集差/总采购额差）结构化 diff，返回对比结果（Decision C 裁决的载体：实体或 DTO）
  - Skill: `nop-backend-dev`
- [ ] DRP 对应物：`ErpDrpScenarioBizModel.runSimulation` + `compareVersions`，同构包装单次 DRP（仓库补货参数场景级覆盖 + 补货量差/安全库存差对比）
  - Skill: `nop-backend-dev`
- [ ] 错误码：`ERP_MFG_SIMULATION_VERSIONS_NOT_COMPARABLE`（不同 orgId/基线）/ `ERP_DRP_SIMULATION_*`（同型）
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] MRP compareVersions 对两版本产结构化 diff（净需求差/建议量差/缺料物料集差可观测）
- [ ] DRP runSimulation + compareVersions 同构可用，补货量差/安全库存差可观测

### Phase 4 - view.xml 定制 + Owner Doc 回链 + Roadmap 同步

Status: planned
Targets: `module-manufacturing/erp-mfg-web`、`module-drp/erp-drp-web`、owner docs、`docs/backlog/deepening-roadmap.md`
Skill: `nop-frontend-dev`

- Item Types: `Add`
- Prereqs: Phase 3 对比引擎落地

- [ ] view.xml：MRP 场景/版本/参数变体 list + form（baseInfo/params/versionHistory）；runSimulation + promoteToFormalPlan 按钮 + visibleOn status 守卫；版本对比 dialog（Decision C 维度表）。DRP 同构
  - Skill: `nop-frontend-dev`
- [ ] action-auth.xml：manufacturing + drp 各增「仿真」菜单分组 + 实体菜单
  - Skill: `nop-frontend-dev`
- [ ] owner doc 回链：`docs/design/manufacturing/mrp.md` 增「仿真引擎关系」段；`docs/design/manufacturing/README.md` 文档表 + 仿真段；DRP 域 owner doc 增 DRP 场景对应物段
  - Skill: `none`
- [ ] roadmap 同步：`deepening-roadmap.md` §Milestone B 表 B1 行 `todo → done` + 新增 §8.x B1 落地证据段
  - Skill: `none`

Exit Criteria:

- [ ] MRP + DRP 场景/版本页面菜单可达 + runSimulation/promoteToFormalPlan 按钮带 status 守卫 + 版本对比 dialog 渲染 diff 维度
- [ ] 3 处 owner doc 回链段落落地 + roadmap §B1 状态翻转为 done

## Draft Review Record

- Independent draft review iteration 1: `needs-revision`（独立子代理 ses_079ffdedaffei9Rwu1GCV3XoOO）—— BLOCKER B1：Current Baseline 隐藏 `MrpEngine` 无覆盖钩子（`runMrp(planId,demands)` :77，lot size/lead time 绑全局 `AppConfig`/主数据 :159/:173/:191，safety stock 在 `DemandAggregator` 上游）致"参数注入零回归"核心假设被仓库证据反驳；MAJOR M1 MRP+DRP 捆绑缺理由 + M2 config-gate 零回归声明自相矛盾；N1 Explore 未类型化 + N2 "可选" slack。
- Independent draft review iteration 2: `acceptable-as-is`（独立子代理 ses_079f9987dffepfNriePHFz8mq2）—— BLOCKER B1 resolved（基线「MrpEngine 代码形状」段精确披露钩子缺失 + 准确行号 `runMrp:77`/`lotSize:163`/`mfgLeadDays:185`/`purLeadDays:193` + safety stock 在 `DemandAggregator.collectSafetyStockDemands:128` 上游；Decision E E1/E2 取舍 + 残留风险 + Phase 2 依 Decision E 分支；核心可行性现为开放裁决而非被反驳的假设）；M1/M2/N1/N2 resolved；无新 slack；单一结果面（规则 14 捆绑正当）。基线所有断言经仓库核实。计划可推进 active。

## Closure Gates

> 完整仓库 `mvn clean install -DskipTests`（154 模块）+ manufacturing service `mvn test`（既有基线 + 新增）+ drp service `mvn test` 在此处运行一次。

- [ ] 范围内行为完成（MRP 仿真 + DRP 对应物 + 参数变体 + 结果对比）
- [ ] 相关文档对齐（simulation-engine.md NEW + 3 回链）
- [ ] 已运行验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + manufacturing/drp service `mvn test` 全绿 + 新单测全绿
- [ ] 单次 MRP/DRP 回归保证（依 Decision E）：E1 = 既有单次 MRP 测试套件全绿证明缺省上下文等价；E2 = 单次路径零触及、既有测试不变
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 产能仿真（CRP 仿真）
- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅 MRP/DRP 物料层 what-if；CRP 仿真归 APS 域 successor
- Successor Required: `yes`（触发条件：APS 排产 what-if 需求 + APS owner doc 授权）

### 概率/蒙特卡洛仿真
- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划为确定性参数变体对比；概率分布仿真为独立算法面
- Successor Required: `yes`（触发条件：业务方明确概率需求仿真需求 + 数据分布建模授权）

### 物料级 fixedLotSize/minOrderQty/maxOrderQty 主数据列
- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: mrp.md line 89 明确须 ask-first；本计划以场景级覆盖值承载不动主数据
- Successor Required: `yes`（触发条件：物料级批量精细化核算需求 + ORM 加列授权）

### APS 排产甘特图可视化仿真
- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 F16 APS 复杂页面 successor
- Successor Required: `yes`（触发条件：F16 plan 启动 + APS 可视化需求）

## Closure

Status Note: <pending — 计划完成独立草案审查并落地后填写>

Closure Audit Evidence:

- Auditor / Agent: <pending independent subagent>
- Evidence: <pending task id / log link>
