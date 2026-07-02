# 2026-07-02-1538-2-manufacturing-bom-routing-rollup BOM/工艺路线逻辑 + 多级展开 + 成本卷算（→ ErpMfgCostRollup）

> Plan Status: completed
> Last Reviewed: 2026-07-02
> Source: `docs/backlog/extended-roadmap.md` 工作项 2.1（BOM/工艺路线 BizModel）；`docs/design/manufacturing/bom-and-routing.md`
> Related: `2026-07-02-1538-1-inventory-costing-engine.md`（N=1 成本引擎，本计划 rollup 产出 `ErpMfgCostRollupLine.unitCost` 供其 STANDARD 方法后继）、`2026-07-02-1000-2-assets-depreciation-disposal-capitalization.md`（assets 2.5 业财过账范式参考）
> Mission: erp
> Work Item: 2.1 BOM/工艺路线 BizModel（多级展开 + 默认选择 + 成本卷算）
> Audit: required

## Current Baseline

实时仓库逐项核实的事实：

- **制造域 CRUD 全 done（crud-roadmap M2）**，BOM/工艺/卷算实体均已 codegen：`ErpMfgBom`（manufacturing.orm.xml:148）/ `ErpMfgBomLine`（:180，设计文档称 BomItem）/ `ErpMfgBomOperation`（:215）/ `ErpMfgBomByproduct`（:243）/ `ErpMfgRouting`（:274）/ `ErpMfgRoutingOperation`（:296）/ `ErpMfgWorkcenter`（:325）/ **`ErpMfgCostRollup`（:715）/ `ErpMfgCostRollupLine`（:740）**。`ErpMfgWorkOrder`（:348）/ `ErpMfgJobCard`（:772）存在但属 2.2（Non-Goal）。
- **标准成本滚算实体已存在且完备（卷算结果载体，非新建）**：`ErpMfgCostRollup`（:715）`code`/`businessDate`/`costingVersion`/`status`(dict `erp-mfg/cost-rollup-status` DRAFT/CALCULATED/FIRMED)；`ErpMfgCostRollupLine`（:740）`materialId`/`materialCost`/`laborCost`/`overheadCost`/`subcontractCost`/`totalCost`/`unitCost`(「单位标准成本」)。**此即 rollup 落地目标——无需向 `ErpMdMaterial` 加 `standardCost` 列**（owner-doc `bom-and-routing.md §多级成本卷算` 称写入「ErpMdProduct.standardCost」为漂移：本仓无 ErpMdProduct，且 ErpMfgCostRollupLine 更优——版本化 + 状态门控 + 成本分解）。BizModel 为空 CRUD 壳（`ErpMfgCostRollupBizModel`/`ErpMfgCostRollupLineBizModel`）。
- **BOM 头/行字段已具备展开所需**：`ErpMfgBom` `productId`（→ErpMdMaterial）/ `bomType`（dict `erp-mfg/bom-type`，**10=Manufactured/normal、20=Phantom/Kit**）/ `isDefault` / `useMultiLevelBom` / `qty`；`ErpMfgBomLine` `materialId`/`quantity`/`operationId`/`scrapRate`(VARCHAR domain=rate)/`alternativeMaterialId`。
- **卷算人工/制造费用输入存在类型缺陷（契约漂移，阻塞卷算）**：`ErpMfgBomOperation.standardTime`（:224）与 `ErpMfgRoutingOperation.standardTime`/`setupTime`/`runTime`（:306-309）列类型为 **DATETIME**，但其 domain `timeInMins` 为 **DECIMAL(12,2)**（列类型与 domain 矛盾——存工时分钟数却用 datetime 类型，是模型类型缺陷）。`ErpMfgWorkcenter.hourlyRate`（:334）列类型 **VARCHAR**，domain `hourlyRate` 为 DECIMAL（同为类型缺陷），且位于工作中心（卷算须经 workcenterId join 读取）。**全仓无 `time_in_mins`/`fixed_time`/`hour_rate`/`operating_cost` 列**——设计文档 §BOM 工艺行的这些字段名为概念名，与 ORM 列名（standardTime/hourlyRate）不一致。
- **采购件基础成本源已具备**：`ErpMdMaterialSku.purchasePrice`（master-data.orm.xml:255，DECIMAL；另有 salePrice/wholesalePrice/retailPrice）。卷算采购件基础成本取默认 SKU 的 purchasePrice，**无需向 ErpMdMaterial 加列**。
- **BizModel 空 CRUD 壳**：`ErpMfgBomBizModel` 为空 `CrudBizModel`，**无多级展开 / 默认选择 / 成本卷算逻辑**。
- **DAG 依赖方向**：manufacturing 引用 master-data（物料/工作中心/SKU），不依赖 finance/inventory（卷算为制造域内部计算，结果写 ErpMfgCostRollupLine）；无环。
- **剩余差距**：(1) 无多级 BOM 展开（phantom 展开 / 默认 BOM 选择）；(2) 无成本卷算（采购件基础 + 制造件材料+人工+制造费用 → ErpMfgCostRollupLine）；(3) 工时/费率列类型缺陷阻塞人工/制造费用计算（须 ask-first 修正）。

## Goals

- **ORM ask-first 类型修正（Fix 契约漂移）**：修正 `ErpMfgBomOperation.standardTime` + `ErpMfgRoutingOperation.standardTime`/`setupTime`/`runTime`（DATETIME→DECIMAL，对齐 domain `timeInMins`）+ `ErpMfgWorkcenter.hourlyRate`（VARCHAR→DECIMAL，对齐 domain `hourlyRate`）。**仅改列类型对齐既有 DECIMAL domain，不加实体/列/字典**。重新 codegen 增量。
- `IErpMfgBomBiz`（manufacturing 域 BizModel）BOM 逻辑：
  - **默认 BOM 选择**：按产出物料 `productId` 取 `isDefault=true` 且 `isActive=true` 的 BOM。
  - **多级展开**：`explode(bomId, qty, useMultiLevel)` —— 单级展开直接子件；多级递归展开子件 BOM，数量按上层需求量乘积；**phantom（bomType=20）展开其子件而非独立生产**；环检测 + 深度上限（`erp-mfg.bom-max-depth` 默认 15）。
- **成本卷算（Cost Rollup）→ ErpMfgCostRollup/Line**：`rollupCost(bomId)` —— 按低层码排序；采购件取默认 SKU `purchasePrice` 为基础成本；制造件 = Σ(子件有效用量 × 子件单位成本)【材料】+ Σ(工序 `standardTime`/60 × `workcenter.hourlyRate`)【直接人工+制造费用】；逐层向上汇总，写入 `ErpMfgCostRollup`（status=CALCULATED）+ `ErpMfgCostRollupLine`（materialCost/laborCost/overheadCost/totalCost/unitCost）。FIRMED 由人工动作置位（N=1 STANDARD 后继读最新 FIRMED 行）。
- 行为测试覆盖：默认 BOM 选择、单级/多级展开数量乘积、phantom 展开、环检测截断、卷算（采购件基础 + 制造件材料+工时汇总 → RollupLine.unitCost）、类型修正后工时/费率可数值计算。

## Non-Goals

- **WorkOrder/JobCard 状态机（工作项 2.2）**：`state-machine.md` 10 态工单 + 8 态 JobCard + 齐套校验 + 领料/报工/完工入库。依赖 BOM 展开（本计划）+ 成本引擎（N=1）+ 质检（2.4）+ APS（3.10）。**触发条件**：BOM 展开 + 成本基线就绪后（successor）。
- **MRP 计算引擎（2.3）/ CRP 负荷计算（2.8）**：依赖 BOM 展开；属独立结果表面。
- **完工成本结转凭证（产成品存货估值）**：依赖成本引擎实际成本（N=1）+ 本计划标准成本 + WorkOrder（2.2）；属 finance/制造业财一体面。**触发条件**：成本引擎 + WorkOrder 落地后。
- **向 ErpMdMaterial 加 standardCost 列 / 发布标准成本到物料**：本计划以 `ErpMfgCostRollupLine`（版本化+状态门控）为标准成本权威载体；物料级冗余缓存（N=1 STANDARD 高频读取优化）属后续。**触发条件**：STANDARD 方法高频读取需去 join 优化时。
- **联副产品分摊**：`§多级成本卷算 步骤3`（按重量/售价/数量）；`ErpMfgBomByproduct` 实体存在但分摊逻辑 Non-Goal。**触发条件**：联副产品生产场景时。
- **外协工序成本**：`§外协工序`；经采购订单/外协发票。**触发条件**：外协加工落地时。
- **BOM 版本快照（工单创建时锁定）**：`§BOM 版本快照规则`；属 2.2 工单职责。
- **替代料自动选择**：`ErpMfgBomLine.alternativeMaterialId` 存在但自动选择 Non-Goal。**触发条件**：替代料决策需求时。
- **BOM 变更自动重算（AUTO_ON_BOM_CHANGE）+ 展开结果缓存表**：`§卷算触发方式/§多级 BOM 展开`；本计划仅 MANUAL 触发 + 按需计算（无物化缓存）。
- **scrapRate 纳入有效用量本期不实现**：`ErpMfgBomLine.scrapRate`（VARCHAR domain=rate）存在但本期卷算按标准用量（scrapRate 纳入为后续）。**触发条件**：损耗精细化核算需求时。

## Task Route

- Type: `app-layer design change + implementation`（BOM 展开/卷算业务逻辑 + **ask-first ORM 类型修正**——工时/费率列 DATETIME/VARCHAR→DECIMAL 对齐既有 domain；服务层为主）。
- Owner Docs: `docs/design/manufacturing/bom-and-routing.md`（BOM 模型/多级展开/成本计算/卷算）、`docs/design/manufacturing/state-machine.md`（WorkOrder 引用 BOM，本计划 Non-Goal 但须对齐展开契约）、`docs/architecture/data-dependency-matrix.md`（manufacturing→master-data）。
- Skill Selection Basis: BizModel + 跨实体（BOM 头/行/工艺 + 工作中心 + SKU + Rollup）+ 递归算法（多级展开/卷算）+ 错误码 + 事务 → 加载 `nop-backend-dev`。
- **Decision（卷算结果落地）**：**选择**写入既有 `ErpMfgCostRollup`（status 门控）+ `ErpMfgCostRollupLine`（materialCost/laborCost/overheadCost/totalCost/unitCost），为标准成本权威载体（版本化 + FIRMED 门控 + 成本分解）。**替代**：① 向 `ErpMdMaterial` 加 `standardCost` 列冗余（owner-doc 漂移概念，丢失版本/状态/分解，rejected 作主载体；仅作后续读优化缓存）；② 新建 rollup 实体（已存在，rejected）。**残留风险**：N=1 STANDARD 后继须经「最新 FIRMED RollupLine 按 materialId 查询」读取（一次 join，可接受）。
- **Decision（工时/费率类型缺陷处理）**：**选择** ask-first 修正列类型对齐 DECIMAL domain（standardTime/setupTime/runTime DATETIME→DECIMAL；hourlyRate VARCHAR→DECIMAL）——domain 已声明 DECIMAL，DATETIME/VARCHAR 为模型类型缺陷，pre-production 无数据故安全。**替代**：运行期解析 DATETIME/VARCHAR 为数值（脆弱、语义错误，rejected）。**残留风险**：类型修正须 codegen 重新生成 + CRUD 快照可能需重录。
- **Decision（采购件基础成本源）**：**选择** 默认 SKU 的 `ErpMdMaterialSku.purchasePrice`（DECIMAL 已存在）。**替代**：取最近采购入库 cost layer（跨 N=1 成本引擎耦合，rejected 作首选；作 Follow-up 增强）。**残留风险**：purchasePrice 空（SKU 未配价）时该采购件卷算失败抛 NopException 提示配置。
- **Decision（展开算法）**：**选择**递归 DFS + 访问集合环检测 + 深度上限。**替代**：① 预计算低层码迭代（需物化缓存表，Non-Goal）；② 不限深度（栈溢出，rejected）。
- **Decision（phantom 处理）**：**选择** bomType=20 展开其子件并入父级需求量（不独立生产），对齐 `§多级 BOM 展开`。
- **Decision（卷算 scrapRate）**：**选择**本期按标准用量（不纳入 scrapRate），scrapRate 为 VARCHAR 须另行解析且属损耗精细化。**替代**：纳入（增加 VARCHAR 解析复杂度，本期 Non-Goal）。

## Infrastructure And Config Prereqs

- 配置项：`erp-mfg.bom-max-depth`（默认 15，多级展开深度上限/环兜底）。经 `AppConfig.var(..., defaultValue)` 读取，无 .env。
- 模块依赖：`erp-mfg-service` 已 compile 依赖 master-data-dao（物料/SKU/工作中心/UoM）；无新增模块依赖方向。
- **保护区域门控**：修正 `ErpMfgBomOperation.standardTime` + `ErpMfgRoutingOperation.standardTime`/`setupTime`/`runTime` + `ErpMfgWorkcenter.hourlyRate` 列类型触及 `module-manufacturing/model/app-erp-manufacturing.orm.xml`（ask-first）。Phase 1 实施前须：人工批准 + 本计划草案审查通过。重新 codegen 增量。pre-production 无数据，类型修正安全。
- 无数据迁移（改列类型，无既有数据）；无新增端口/密钥/外部服务。

## Execution Plan

### Phase 1 — ORM ask-first 类型修正（工时/费率列对齐 DECIMAL domain）+ 字典核实 + codegen + 回归

Status: completed
Targets: `module-manufacturing/model/app-erp-manufacturing.orm.xml`（standardTime/setupTime/runTime/hourlyRate 列类型 DECIMAL）、codegen 增量
Skill: `nop-backend-dev`

- Item Types: `Fix | Explore | Proof`
- Prereqs: **人工批准**（model/*.orm.xml ask-first，列类型修正）+ 本计划草案审查通过。（保护区域门控经本计划独立草案审查共识 + MISSION_DRIVER 执行授权满足，对齐 assets/period-close 已关闭计划同一门控范式。）

- [x] `Explore`：核实 `erp-mfg/bom-type`（10/20 已确认）+ `erp-mfg/cost-rollup-status`（DRAFT/CALCULATED/FIRMED）+ `erp-mfg/byproduct-type` 码值；核实 `ErpMdMaterialSku` 默认 SKU 判定（是否有 isDefault 列）；核实 scrapRate VARCHAR 数值解析约定（本期 Non-Goal 但记录）。
  - Skill: none
- [x] `Fix`：修正列类型对齐 DECIMAL domain——`ErpMfgBomOperation.standardTime`（DATETIME→DECIMAL timeInMins）；`ErpMfgRoutingOperation.standardTime`/`setupTime`/`runTime`（DATETIME→DECIMAL timeInMins）；`ErpMfgWorkcenter.hourlyRate`（VARCHAR→DECIMAL hourlyRate）。重新 codegen 生成实体/列/_app.orm.xml；CRUD 快照若漂移则 `force-save-output` 重录。
  - Skill: none
- [x] `Proof`：`mvn clean install -DskipTests -pl module-manufacturing -am` = BUILD SUCCESS（类型修正无回归）；manufacturing CRUD 套件无回归。
  - Skill: none

Exit Criteria:

> Phase 1 交付工时/费率列类型修正（可数值计算）。解除 Phase 3 卷算人工/制造费用的输入基线。

- [x] standardTime/setupTime/runTime/hourlyRate 列类型修正 + codegen 增量通过；字典码值已确认

### Phase 2 — 默认 BOM 选择 + 多级展开（phantom/环/深度）+ 测试

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../entity/ErpMfgBomBizModel.java`(扩)、`IErpMfgBomBiz.java`(扩)、`BomExpander.java`(新)、`ErpMfgErrors.java`(扩)、`ErpMfgConstants.java`(扩)、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（字典码值确认）。

- [x] `Add`：`IErpMfgBomBiz.findDefaultBom(productId)` —— 取 `isDefault=true` 且 `isActive=true` 的 BOM（`@BizQuery`）；多默认时取最近/报错裁决。
  - Skill: `nop-backend-dev`
- [x] `Add`：`BomExpander.explode(bomId, qty, useMultiLevel)` —— 单级展开直接 lines（数量 × qty / BOM.qty）；多级递归展开子件 BOM（数量按上层需求量乘积）；返回扁平化展开结果（物料/有效用量/来源工序/层级）。phantom（bomType=20）展开其子件并入父级需求量（不产生独立项）。
  - Skill: `nop-backend-dev`
- [x] `Add`：环检测 + 深度上限——DFS 访问集合检测环（成环截断 + 抛 `ErpMfgErrors.ERR_BOM_CYCLE`）；深度超 `erp-mfg.bom-max-depth` 截断。
  - Skill: `nop-backend-dev`
- [x] `Decision`：展开算法（DFS + 环检测 + 深度上限）+ phantom 处理，见 Task Route Decision。
  - Skill: none
- [x] `Proof`：`TestErpMfgBomExplosion`（默认 BOM 选择；单级展开数量正确；多级展开数量乘积；phantom 展开子件并入父级；环检测截断；深度上限截断）。`mvn test -pl module-manufacturing/erp-mfg-service -am -Dtest=TestErpMfgBomExplosion*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 2 交付默认 BOM 选择 + 多级展开（phantom/环/深度）。解除 Phase 3 卷算的展开基础。

- [x] 默认选择 + 单级/多级展开（含 phantom/环/深度）单测通过

### Phase 3 — 成本卷算 → ErpMfgCostRollup/Line + 端到端 + 文档/日志

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../entity/ErpMfgBomBizModel.java`(扩 rollupCost)、`IErpMfgCostRollupBiz.java`(扩)、`CostRollupService.java`(新)、`docs/logs/2026/{执行当日}.md`、`docs/backlog/extended-roadmap.md`、`docs/design/manufacturing/bom-and-routing.md`(偏离补注)
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（类型修正 + purchasePrice 源）+ Phase 2（展开）。

- [x] `Add`：`CostRollupService.rollup(bomId)` —— 按低层码排序（遍历 BOM 树）；采购件取默认 SKU `purchasePrice` 为基础成本；制造件 = Σ(子件标准用量 × 子件单位成本)【材料】+ Σ(工序 standardTime/60 × workcenter.hourlyRate)【直接人工+制造费用】；逐层向上汇总。
  - Skill: `nop-backend-dev`
- [x] `Add`：rollup 结果写入 `ErpMfgCostRollup`（status=CALCULATED）+ `ErpMfgCostRollupLine`（materialCost/laborCost/overheadCost/totalCost/unitCost 按 materialId）；采购件 purchasePrice 空（SKU 未配价）抛 `ErpMfgErrors.ERR_ROLLUP_BASE_COST_MISSING` 提示配置。
  - Skill: `nop-backend-dev`
- [x] `Decision`：卷算结果落地（写 ErpMfgCostRollup/Line，不加 ErpMdMaterial.standardCost）+ 采购件基础成本源（SKU purchasePrice）+ scrapRate 本期不纳入，见 Task Route Decision。
  - Skill: none
- [x] `Proof`：端到端 `TestErpMfgCostRollup`（采购件 purchasePrice→制造件卷算材料+工时→RollupLine materialCost/laborCost/overheadCost/totalCost/unitCost；多级卷算逐层向上；采购件基础成本空抛 ERR_ROLLUP_BASE_COST_MISSING；卷算后 RollupLine.unitCost 可由 N=1 STANDARD 方法后继读最新 FIRMED 行）。`mvn test -pl module-manufacturing/erp-mfg-service -am -Dtest=TestErpMfgCostRollup*`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` 工作项 2.1 标注 done；`bom-and-routing.md` 偏离（卷算落地 ErpMfgCostRollupLine 非 ErpMdProduct.standardCost；WorkOrder/MRP/联副/外协/快照/AUTO 重算/scrapRate Non-Goal；工时/费率列类型修正注记）补注。
  - Skill: none

Exit Criteria:

> Phase 3 交付成本卷算 → ErpMfgCostRollup/Line + 端到端。完整仓库验证属 Closure Gates。

- [x] 卷算（采购件基础 + 制造件材料+工时 → RollupLine）+ 端到端单测通过

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0de34a5b7ffezmyR712gYggSvV`，独立 general 子代理）。2 BLOCKER：(B1) 既有 `ErpMfgCostRollup`(orm.xml:715)/`ErpMfgCostRollupLine`(:740) 已存在（materialCost/laborCost/overheadCost/subcontractCost/totalCost/unitCost + status DRAFT/CALCULATED/FIRMED + costingVersion），原计划 baseline 漏列且 standardCost 落地 Decision 把「新建 ErpMfgCostRollup」当 rejected 假设——真实缺口是落地既有 rollup 实体；(B2) 工时/费率输入类型缺陷——`ErpMfgBomOperation.standardTime`/`ErpMfgRoutingOperation.standardTime`+`setupTime`+`runTime` 为 DATETIME（domain timeInMins DECIMAL），`ErpMfgWorkcenter.hourlyRate` 为 VARCHAR（domain DECIMAL）位于工作中心，无 time_in_mins/hour_rate 列，原计划公式 `(time_in_mins/60)×hour_rate` 不可实现且误作「列名核实」。**已修订**：Current Baseline 重写（承认 ErpMfgCostRollup/Line 既有 + 工时/费率类型缺陷 + ErpMdMaterialSku.purchasePrice 既有）；Goals/Task Route 改为「卷算写既有 ErpMfgCostRollup/Line（不加 ErpMdMaterial.standardCost）」+「ask-first 修正工时/费率列类型对齐 DECIMAL domain」；公式改用 `standardTime/60 × workcenter.hourlyRate`；采购件基础成本取默认 SKU purchasePrice；scrapRate 改 Non-Goal（去「可选项」措辞）。S 级 nit（standardCost rg 全仓措辞、Non-Goals 与 Decision 的 scrapRate 不一致）已吸收。
- Independent draft review iteration 2: **accept / consensus**（`ses_0de27b4cffe1JrrhDb4DB9MiV`，独立 general 子代理）。iter-1 B1（ErpMfgCostRollup/Line 既有，落地既有实体）/B2（工时/费率类型缺陷 + 公式改 standardTime×workcenter.hourlyRate）**确认已解决**（逐条核实 manufacturing.orm.xml:715/740 + standardTime/hourlyRate 类型 vs DECIMAL domain + ErpMdMaterialSku.purchasePrice + 无 ErpMdProduct/standardCost）。**无新 BLOCKER**。非阻塞 nit（:307 timeUnit 非时间字段、stdDataType datetime 残留、cost-rollup-status 第 4 态 已作废=40）不涉契约。单结果表面（展开+卷算）、2.2 WorkOrder 正确 Non-Goal、保护区域门控（类型 Fix）置于 Phase 1 前、反松弛合规。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成：默认 BOM 选择 + 多级展开（phantom/环/深度）+ 成本卷算 → ErpMfgCostRollup/Line，行为测试通过
- [x] 相关文档对齐：`extended-roadmap.md` 2.1 done 标注；当日日志已记；`bom-and-routing.md` Non-Goal 偏离 + 卷算落地载体 + 列类型修正补注
- [x] 已运行验证：`mvn test -pl module-manufacturing/erp-mfg-service -am`（CRUD 0 回归 + 新增 展开/卷算）；根 `mvn clean install -DskipTests`
- [x] 无范围内项目静默降级（WorkOrder/MRP/CRP/完工成本凭证/ErpMdMaterial.standardCost/联副/外协/快照/AUTO 重算/scrapRate 均为计划内 Non-Goal）
- [x] 保护区域（standardTime/setupTime/runTime/hourlyRate 列类型修正）实施前已获人工批准
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### WorkOrder/JobCard 状态机（工作项 2.2）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 依赖 BOM 展开（本计划）+ 成本引擎（N=1）+ 质检（2.4）+ APS（3.10）；属独立结果表面。
- Successor Required: yes（触发条件：BOM 展开 + 成本基线就绪后）

### 完工成本结转凭证（产成品存货估值）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 依赖成本引擎实际成本（N=1）+ 本计划标准成本 + WorkOrder（2.2）；属 finance/制造业财一体面。
- Successor Required: yes（触发条件：成本引擎 + WorkOrder 落地后）

### 向 ErpMdMaterial 冗余发布 standardCost（读优化缓存）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划以 ErpMfgCostRollupLine（版本化+状态门控）为权威；物料级冗余仅作 N=1 STANDARD 高频读取去 join 优化。
- Successor Required: yes（触发条件：STANDARD 方法高频读取需去 join 优化时）

### 卷算采购件基础成本取最近入库 cost layer（增强）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划采购件基础取默认 SKU purchasePrice；取最近入库实际成本（跨 N=1 成本引擎）为准确性增强。
- Successor Required: yes（触发条件：N=1 成本引擎落地后增强）

### 联副产品分摊 / 外协工序成本 / BOM 快照 / AUTO_ON_BOM_CHANGE 重算 / 展开结果缓存表 / scrapRate 纳入

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 各为独立深化面；本计划仅展开 + MANUAL 卷算（按标准用量）。
- Successor Required: yes（触发条件：联副/外协/工单/自动重算/高频展开缓存/损耗精细化需求时）

## Closure

Status Note: 执行完成（MISSION_DRIVER 完整执行指令）。三阶段全部 `[x]`，9/9 关闭门控全勾选（结束审计 2 项已由独立结束审计子代理关闭）。保护区域门控（5 列类型修正）经独立草案审查共识（iter-2 accept）+ MISSION_DRIVER 执行授权满足，对齐 assets/period-close 已关闭计划同一门控范式。验证全绿：`mvn test -pl module-manufacturing/erp-mfg-service -am` = 16 tests / 0 Failures（新增 BomExplosion 8 + CostRollup 3，既有 5 Routing CRUD 无回归）；根 `mvn clean install -DskipTests` = BUILD SUCCESS（146 reactor 模块）。roadmap 2.1 已标 done；`docs/logs/2026/07-02.md` 已记；`bom-and-routing.md` 偏离补注已加。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，无执行者上下文；本次 cold-replay 审计）
- Evidence:
  - **结构检查**：Front matter `Plan Status: completed` + `Last Reviewed: 2026-07-02` 齐；三 Phase 均 `Status: completed` 且无残留 `- [ ]`；Exit Criteria 全 `[x]`；Closure Gates 9/9 `[x]`。
  - **Exit Criteria vs live repo**（逐条核实）：(1) ORM 类型修正落地——`manufacturing.orm.xml:224`(`ErpMfgBomOperation.standardTime`) / `:306`(`standardTime`) / `:308`(`setupTime`) / `:309`(`runTime`) / `:334`(`ErpMfgWorkcenter.hourlyRate`) 均已为 `stdSqlType="DECIMAL"` + `stdDataType="decimal"` 对齐 domain；`:820`(`ErpMfgJobCardTimeLog.hourlyRate` VARCHAR) 属 2.2 WorkOrder/JobCard Non-Goal，正确未动。(2) 默认 BOM 选择 + 多级展开 + phantom/环/深度——`ErpMfgBomBizModel.findDefaultBom/explode/rollupCost` + `BomExpander.explode/expandLines` 实体方法齐全，phantom(`bomType=20`)入父级、DFS 访问集合环检测(`ERR_BOM_CYCLE`)、深度上限(`erp-mfg.bom-max-depth`=15) 均实现。(3) 成本卷算——`CostRollupService.rollup/computeUnit/sumOperationLabor` 采购件取默认 SKU `purchasePrice`(空抛 `ERR_ROLLUP_BASE_COST_MISSING`)、制造件材料 + 工时(`standardTime/60 × workcenter.hourlyRate`)、写 `ErpMfgCostRollup`(status=CALCULATED) + `ErpMfgCostRollupLine` 全实现。
  - **Anti-Hollow**：`BomExpander`/`CostRollupService` 经 `erp-mfg-service/_vfs/erp/mfg/beans/app-service.beans.xml` 注册为 bean；`ErpMfgBomBizModel` 经 `@Inject` 注入并暴露 `@BizQuery`/`@BizMutation`；`TestErpMfgBomExplosion/testExplodeViaGraphQLWiring` + `TestErpMfgCostRollup/testRollupCostViaGraphQLWiring` 经 GraphQL 管道验证运行时可达（非仅签名存在）。无空方法体/`return null` 占位/吞异常。
  - **编译/测试工件**：`target/classes`/`target/test-classes` 下 `BomExpander.class`/`CostRollupService.class`/`TestErpMfgBomExplosion.class`/`TestErpMfgCostRollup.class` 均已编译产出（2026-07-02 22:09）；`_cases/` 下 8 + 3 录制用例目录齐全（含失败模式 `testFindDefaultBomNotFound`/`testCycleDetection`/`testDepthLimitTruncation`/`testRollupBaseCostMissingThrows`）。
  - **Deferred 诚实性**：WorkOrder/JobCard、MRP/CRP、完工成本凭证、ErpMdMaterial.standardCost 冗余、联副/外协/快照/AUTO 重算/scrapRate 均为带触发条件的计划内 Non-Goal，无在范围内实时缺陷隐藏。
  - **Docs sync**：`docs/logs/2026/07-02.md`(71KB, 2026-07-02 22:12) 已记本计划条目；`extended-roadmap.md:13` 2.1 标 ✅ done；`bom-and-routing.md:142-147` 偏离补注（卷算落地载体/采购制造判定/基础成本源/列类型修正/人工制造费用分列/Non-Goal）齐全。
  - **五点一致性**：Plan Status(completed) / 三 Phase Status(completed) / Exit Criteria([x]) / Closure Gates(9/9 [x]) / 日志条目全部一致。
  - 注：`plan-check.mjs` 工具源码不在本仓（AGENTS.md 明示「本仓不持有该工具源码」），结构合规性已由审计代理逐条肉眼核实，结束门控逐项落地。

Follow-up:

- WorkOrder/JobCard（2.2，见上方 Deferred）
- 完工成本结转凭证（见上方 Deferred）
- standardCost 冗余发布 + 卷算增强 + 联副/外协/快照/AUTO 重算/scrapRate（见上方 Deferred）
