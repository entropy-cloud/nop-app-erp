# ck-mfg-bom-mrp — manufacturing「BOM/MRP/CRP/仿真」切片实现代码检查报告

> 工作项：C4.2。执行日期：2026-08-25。执行者：域检查子代理（ZCode 会话，read-only）。
> 范围：`module-manufacturing/erp-mfg-service/src/main/java` BOM/MRP/CRP/仿真/成本卷算切片 41 个手写生产文件——BOM 链 6 类（`bom/BomExpander` + `entity/ErpMfgBom(BizModel|LineBizModel|OperationBizModel|ByproductBizModel)` + `processor/ErpMfgBomRollupCostProcessor`）、工艺路线 3 类（`entity/ErpMfgRouting(BizModel|OperationBizModel)` + `ErpMfgProductionVersionBizModel`，均为裸 CrudBizModel 抽查）、MRP 链 10 类（`mrp/MrpEngine`/`DemandAggregator`/`MrpReleaseService` + `entity/ErpMfgMrpPlan(BizModel|LineBizModel)`/`ErpMfgMrpDemandBizModel` + `processor/ErpMfgMrpPlanRunMrpProcessor` + 3 个 Release Processor + `statemachine/ErpMfgMrpPlanStateMachine`）、预测链 3 类（`entity/ErpMfgForecast(BizModel|LineBizModel)` + `statemachine/ErpMfgForecastStateMachine`）、CRP 链 7 类（`crp/CrpLoadCalculator` + `entity/ErpMfgCrpLoadBizModel` + `ErpMfgWorkcenter(BizModel|CalendarBizModel|CapacityBizModel)` + `job/ErpMfgCrpRunJob` + `processor/ErpMfgCrpLoadCalculateLoadProcessor`）、仿真链 8 类（`simulation/SimulationMrpEngine`/`ErpMfgSimulationParamResolver`/`IErpMfgSimulationParamResolver`/`SimulationVersionComparator` + `entity/ErpMfgMrpScenario(BizModel|ParamBizModel|VersionBizModel)` + 2 Processor）、成本卷算 4 类（`costing/CostRollupService`/`CostBandClassifier` + `entity/ErpMfgCostRollup(BizModel|LineBizModel)`）+ D4 接线面（`app-service.beans.xml`、`erp-mfg-crp-run.job.yaml`、`scheduler.yaml`）。跨域/平台实证：`module-drp/.../DrpDemandAggregator`（预测消费契约对读）、`app-erp-manufacturing.orm.xml`（BOM/MRP/Scenario/WorkOrder/CostRollup/Forecast 列与 UK）、`app-erp-master-data.orm.xml`（material 无 standardCost 列、SKU 列）、`app-erp-sales.orm.xml`/`app-erp-purchase.orm.xml`（docStatus/currencyId 列）、nop-entropy `QueryBean.addOrderField`/`OrderFieldBean`（desc 参数语义实证）、nop-entropy `GraphQLTransactionOperationInvoker` + `biz-defaults.beans.xml`（query 无事务包装实证）、`OrmSessionImpl.flush/close`（脏检查语义）、nop-job `BeanMethodJobInvoker`（job 直调无事务）。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。引擎/服务/比较器/Job 逐行深读 + 裸 BizModel 抽查确认 + 平台 API 布尔/事务语义先查 nop-entropy 源码再定性 + 测试面交叉验证（TestErpMfgMrpEngine/TestErpMfgMrpSimulation 断言面核实）+ arm-index 复用裁决。
> 切片边界：工单/报工/齐套/预留归 C4.1（已查，本切片仅消费其交点——`KitAvailabilityChecker.explodeRequirements` 的 BOM 展开参数属本切片）；委外释放 `releaseSubcontractRequest` 的 O-4 豁免/并发键归 C4.1/P1-MA2-090（已裁决，仅读侧核验）；`ProductionVarianceCalculator`/CostVariance/批次追溯归 C4.3。
> 注：按 mission 硬约束本报告只落本文件；`ai-check-index.md` 的 finding 行登记由主 agent 执行。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-mfg2-001（D6）SAFETY_STOCK 需求可用量双扣——聚合器已按「安全库存−可用量」求缺口作为需求量，引擎再减一次可用量，安全库存补货系统性低估（可用量 ≥ 安全库存一半时净需求恒 0，永不补货）

- **控制点**：`app/erp/mfg/service/mrp/DemandAggregator.java#collectSafetyStockDemands`（L132-143：`BigDecimal available = availableQuantity(material.getId(), plan.getOrgId()); BigDecimal shortfall = safety.subtract(available); ... demand.setQuantity(shortfall);`——**需求行数量已经是净缺口**）+ `app/erp/mfg/service/mrp/MrpEngine.java#processMaterial`（L116-121：`BigDecimal available = availableQuantity(materialId, plan.getOrgId()); BigDecimal net = grossQty.subtract(available).subtract(scheduled); if (net.signum() < 0) net = ZERO;`——**对同物料同口径的 available 再扣一次**）
- **证据**：两处 `availableQuantity` 实现逐位相同（同事务同快照读 `ErpInvStockBalance` 合计 availableQuantity，null 回退 total−reserved−locked）。算例：safety=100、available=10 → 聚合器产出需求 90；引擎 net = 90−10 = 80 → 补货 80 后库存 90 < 100，**少补恰等于 available**；safety=100、available=99 → 需求 1 → net = 1−99 < 0 → **planned=0，缺口 1 永不补**（可用量 ≥ safety/2 时恒为 0）。对照 SALES_ORDER/FORECAST 来源（gross 未预扣库存）引擎公式正确——缺陷是「预净额的需求行」与「引擎再净额」的组合错配。测试面：`TestErpMfgMrpEngine#testDemandAggregationExpansionOrderTypePegging` L77/L93/L127-130——safetyStock=8、available=3、断言止于 demand 行 quantity=5 与 M2 行 grossRequirement=5，**未断言 M2 行 netRequirement/plannedQuantity**（真值应为 5，实算 2），双扣对测试不可见。
- **同型控制点**：`app/erp/mfg/service/simulation/SimulationMrpEngine.java#applySafetyStockOverride`（L285-297 同样 `shortfall = safety.subtract(available)` 后交给 fork 的 `processMaterial` L316-321 再扣）——仿真链完全继承同缺陷。
- **问题**：mrp.md §MRP 流程「净需求 = 毛需求 − 可用量」只对未预扣的毛需求成立；安全库存补货量被低估直接少生成采购/工单建议，且随库存接近安全库存误差占比趋近 100%（差一个 available），用户可见的 MRP 正确性错误。
- **建议修复方向**：二选一——(a) 聚合器对 SAFETY_STOCK 写全量 safety 作为毛需求（引擎公式不动，对齐流程图字面）；(b) 引擎对 `demandSource=SAFETY_STOCK` 的 top 需求跳过 available 扣减（需求行已是净额）。修复须同步 fork 的 `SimulationMrpEngine`，并补「available>0 且 <safety」断言 netRequirement 的回归测试。
- **arm-index 裁决**：新增（grep「安全库存 双扣/safety stock double/shortfall available」零命中；A4.2b 审计声明「MrpEngine 净需求……扎实」未覆盖 SAFETY_STOCK 来源的组合口径）。

### P1-CK-mfg2-002（D6）MRP 无低阶码净额归集——同一物料多次出现（多个父件共享子件 / 既是独立需求又是子件）时每次出现独立扣减全部可用量，净需求系统性低估

- **控制点**：`app/erp/mfg/service/mrp/MrpEngine.java#processMaterial`（L116：每次递归进入都重新 `availableQuantity(materialId, ...)` 全额扣减；L149-159：`explode(bom.getId(), planned, false)` 单级展开后逐 child 递归 `processMaterial`，跨分支无任何「已消耗可用量」累计；L112-114 `path.contains` 仅防环不防重复物料）+ `#topDemandsByMaterial`（L229-245：仅顶层需求按物料合并，子件展开层永不与顶层或其他分支合并）
- **证据**：算例——顶层需求 A(100)、B(100) 均为制造件，共用子件 C（单件用量 1），available(C)=50、A/B 无库存：处理 A 的子件 C → net=100−50=50；处理 B 的子件 C → net=100−50=50；**C 总建议量 100，真值 = 200−50 = 150，少 50（= (出现次数−1)×available）**。C 同时有独立需求时低估进一步放大。`BomExplosionNode.level` 字段（BomExpander L191 `n.setLevel(level)`）**写入了但 MrpEngine 从不消费**——owner doc `mrp.md §实现约定 L92` 声称「低层码：经 BomExpander DFS 层级标记实现（同物料取最低层级展开基准）」与实现不符（零 `getLevel` 消费点，grep 实证）。测试面：`TestErpMfgMrpEngine` M1 既是 P 的子件（gross 20）又有手工需求（gross 3）但 **M1 未 seed 余额（available=0）** → 双扣不可见。
- **问题**：经典 MRP 低阶码（low-level-code）需求未落地但 owner doc 声明已落地；共享件越普遍、库存越多，MRP 采购/工单建议量低估越严重——用户可见正确性错误。
- **建议修复方向**：按物料聚合净额——先全树收集各物料毛需求（含顶层与全部展开层），按 level 升序（低阶码序）逐物料一次净额计算（`Σgross − available`），或至少维护本次 run 内 per-material 已消耗可用量累计。修复须同步 fork 的 `SimulationMrpEngine`；owner doc `mrp.md` L92 声明同步修正。
- **arm-index 裁决**：新增（与 arm-index 冲突项——A4.2b 摘要声明「MrpEngine 净需求/低层码递归……扎实」，本 finding 以代码+算例证伪该结论的低阶码维度；建议主 agent 复核，见 §剩余风险）。

### P1-CK-mfg2-003（D6）SimulationMrpEngine.nextVersionNo 以 ASC 取最小 versionNo+1——同场景第 3 次仿真运行 versionNo 与既有 v2 冲突（UK (scenarioId,versionNo) 原始约束违例），设计声明的「粗调/细调/最终」多版本迭代循环第 3 次起必炸

- **控制点**：`app/erp/mfg/service/simulation/SimulationMrpEngine.java#nextVersionNo`（L483-493：`q.addOrderField("versionNo", false); q.setLimit(1); ... return top.get(0).getVersionNo() + 1;`——**第二参 desc=false 为升序（nop-entropy 实证：`QueryBean.addOrderField(name, desc)` → `OrderFieldBean.forField(name, desc)`，desc=false 生成 `" name asc"`），取的是最小 versionNo**）+ ORM `app-erp-manufacturing.orm.xml` L1736 `UK_MFG_MRP_SCENARIO_VERSION_SCN_VER (scenarioId, versionNo)`
- **证据**：运行序列——run1 → versions {v1}，next = min(1)+1 = 2 ✓（巧合正确）；run2（场景重置 DRAFT 后）→ {v1,v2}；run3 → min(1)+1 = **2，与既有 v2 撞 UK** → `saveEntity` 抛原始约束违例（无 `flushReleaseOrThrow` 式翻译，对照 MrpReleaseService L289-299 已有翻译范式），场景迭代中断。仓内正确对照：`DemandAggregator#nextLineNo` L276 用 `addOrderField("lineNo", true)`=DESC 取最大值。设计依据：`simulation-engine.md §Decision A`「同一业务假设下可有多版本（粗调/细调/最终）」——3+ 版本是设计主路径。测试面：`TestErpMfgMrpSimulation` 全部场景最多 run 2 次（`testCompareVersionsProducesStructuredDiff` L254-270 恰好 v1/v2），第 3 次运行零覆盖。
- **问题**：多版本迭代循环断裂（第 3 次起），错误为未翻译的 DB 约束异常（`ERR_ORM_DATA_EXCEPTION` 级），用户无法理解也无法继续。
- **建议修复方向**：`addOrderField("versionNo", true)` 改 DESC 取 max；可顺带对 UK 冲突补友好错误码翻译（对齐 flushReleaseOrThrow 范式）；补第 3 次运行的回归测试。
- **arm-index 裁决**：新增（grep「nextVersionNo/versionNo 升序」零命中；A4.2b 报告对 versionNo/compareVersions/standardCost 零覆盖——`2026-07-29-0024-arm-ma4-mfg-mrp-quality-code-quality.md` grep 计数 0）。

### P2-CK-mfg2-004（D6）MRP 消费预测行不过滤 warehouseId——仓级预测行被 MRP 与 DRP 双引擎重复计入需求，MRP 毛需求虚高

- **控制点**：`app/erp/mfg/service/mrp/DemandAggregator.java#collectForecastDemands`（L189-195：行查询仅 `in("forecastId", headIds)` + 区间相交两过滤，**无 `eq("warehouseId", null)` 产品级行过滤**；L198-228 全量按物料聚合 forecastQty）对照契约两源——ORM `app-erp-manufacturing.orm.xml` L1075 列注释「仓库（可选，**空=产品级 MRP 消费，填=仓级 DRP 消费**）」+ `module-drp/erp-drp-service/.../DrpDemandAggregator.java#indexForecastByMaterialWarehouse`（L146-147：`lq.addFilter(ne("warehouseId", null));` 注释「仅消费仓级预测（warehouseId 非空），**产品级（warehouseId 为 null）由 MRP 消费**」——`ne(name,null)`=IS NOT NULL 平台语义实证）
- **证据**：一条 `warehouseId=W1, forecastQty=100` 的预测行：DRP 聚合器按 (material,W1) 消费 → W1 补货 ~100；MRP 聚合器同样消费（产品级聚合含仓级行）→ 同物料毛需求 +100 → 净需求 → 采购/工单建议。两计划均释放时**同一预测被补两次**。仅产品级行（warehouseId=null）时无问题；混合使用（列设计明确支持「同时服务 MRP/DRP」）即触发。
- **问题**：模型真相源（ORM 列注释）与 DRP 侧实现均声明「填=仓级 DRP 消费」，MRP 侧违反分工；owner doc `mrp.md` L43「warehouseId 留空（MRP 为产品级需求）」仅描述自身输出行、未裁决输入过滤——按 §检查纪律记为「确认问题 + 需求分歧注记」（若业务意图是 MRP 聚合一切预测行，则需修订 ORM 列注释与 DRP javadoc 的分工声明）。
- **建议修复方向**：`collectForecastDemands` 行查询补 `eq("warehouseId", null)`（与 DRP 的 `ne` 对偶）；或三处契约（ORM 注释/DRP javadoc/mrp.md）统一裁决「MRP 全量聚合」并改其余两处。
- **arm-index 裁决**：新增（grep「forecast warehouseId MRP DRP 双」零命中）。

### P2-CK-mfg2-005（D8）MRP→工单→CRP 负荷链断裂——释放的工单不写 routingId/plannedEndDate，且 CRP 窗口查询 `ge("plannedEndDate",…)` 排除 NULL 完工日期工单、`distributeByWorkOrder` 要求 routingId 非空，MRP 释放工单对 CRP 永不可见

- **控制点**：`app/erp/mfg/service/mrp/MrpReleaseService.java#releaseToWorkOrder`（L171-190：仅 set productId/bomId(默认 BOM)/plannedQuantity/**plannedStartDate**=line.plannedDate/businessDate/orgId/docStatus/approveStatus——**不写 routingId、不写 plannedEndDate**）+ `app/erp/mfg/service/crp/CrpLoadCalculator.java#findWorkOrdersInWindow`（L364-370：`q.addFilter(ge("plannedEndDate", periodFrom));`——SQL NULL 比较为假，**plannedEndDate 为 NULL 的工单整行被排除**）+ `#distributeByWorkOrder`（L258-263：`if (routingId == null) return 0;`——无工艺路线零负荷）+ ORM 实证（`ErpMfgWorkOrder.plannedStartDate/plannedEndDate/routingId` 三列均**非必填**，orm L583-590 一带）
- **证据**：三重门叠加——MRP 释放工单（`WO-MRP-{lineId}`）plannedEndDate=NULL → 窗口查询排除；即使补日期，routingId=NULL → distribute 返回 0；routingId 全仓仅 `CrpLoadCalculator:258` 一个读点、无任何自动填充 writer（grep `setRoutingId` 零命中，BOM→routing 绑定不经由工单传递）。`#woEnd` L396-408 对 NULL plannedEndDate 的 plannedStartDate/businessDate 回退分支**经 calculateLoad 路径不可达**（查询已排除 NULL 行）——死回退自证缺口。CRP WORK_ORDER 源只能看到手工完整填写 routingId+双日期的工单；crp.md §负荷来源双源声称的「WorkOrder（plannedStartDate~plannedEndDate）」对计划链自动生成的工单失效。
- **问题**：MRP 释放与 CRP 负荷计算两个特性之间的数据衔接断链（D8 跨特性闭环缺失）——产能计划失真（负荷系统性低估），且为静默缺口（零日志零告警）。
- **建议修复方向**：releaseToWorkOrder 从默认 BOM 解析 routing（或 plannedEndDate=plannedDate+提前期估算）；或 CRP 窗口查询放宽为 `(plannedEndDate IS NULL AND plannedStartDate<=periodTo) OR …` 并让 woEnd 回退真正生效；需 owner doc crp.md 同步裁决「无日期/无路线工单的负荷口径」。
- **arm-index 裁决**：新增（grep「releaseToWorkOrder routingId/plannedEndDate CRP」零命中；A4.2b 声明「CrpLoad 双源负荷扎实」未覆盖 MRP 释放工单的输入完整性）。

### P2-CK-mfg2-006（D4）仿真对比第 4 维「总采购额差」恒为 0——lookupStandardCost 硬编码返回 ZERO（ErpMdMaterial 无 standardCost 列，设计公式依赖不存在的列），owner doc 声明的 4 维 diff 实际 3 维

- **控制点**：`app/erp/mfg/service/simulation/SimulationVersionComparator.java#lookupStandardCost`（L169-176：`ErpMdMaterial m = daoProvider...getEntityById(materialId); if (m == null) return ZERO; // ErpMdMaterial 无 standardCost 列；本期以 0 兜底，successor 接入采购价或标准成本时填充 return BigDecimal.ZERO;`——**无条件返回 0**）+ `#compareMrpVersions` L98-101（`plannedA/B.multiply(stdCost)` → `totalPurchaseAmountDelta` 恒 0）
- **证据**：`module-master-data/model/app-erp-master-data.orm.xml` grep 实证——ErpMdMaterial 仅有 safetyStock/leadTimeDays 等列，**无 standardCost 列**（列不存在，`m.getStandardCost()` 无法编译，故硬编码 0）。owner doc `simulation-engine.md §结果对比算法 Decision C` 表格声明「总采购额差 = Σ(PURCHASE_REQUEST.plannedQuantity × material.standardCost)」且 diff DTO 暴露该字段——用户对比两版本看到的总采购额差永远是 0，字段存在但功能未落地。仓内存在可替代源：`ErpMfgCostRollupLine.unitCost`（FIRMED，`ErpMfgCostRollupBizModel#findLatestFirmedStandardCost` 既有读路径）或默认 SKU `purchasePrice`。
- **问题**：声明特性未落地（D4 家族）+ 对比结果误导（非零采购计划显示 0 差异）。代码注释自知（successor 注记）但 owner doc 未同步降级声明。
- **建议修复方向**：接 `ErpMfgCostRollupLine`（FIRMED 最新）或 SKU purchasePrice 作为价源；或 owner doc simulation-engine.md 显式标注第 4 维 Deferred（触发条件=标准成本价源接入）。
- **arm-index 裁决**：新增（grep「totalPurchaseAmount/standardCost 恒 0」零命中）。

### P2-CK-mfg2-007（D7）SimulationVersionComparator.indexLines 对 session 托管实体直接写值——@BizQuery 无事务使今日不落库，但污染同请求会话缓存，且任何事务上下文（job/mutation 复用）调用即静默持久化、破坏仿真快照不可变（AP-06）

- **控制点**：`app/erp/mfg/service/simulation/SimulationVersionComparator.java#indexLines`（L142-163：`findAllByQuery` 载入**托管实体**后，同物料多顶层行合并分支 L157-158 `existing.setNetRequirement(nz(existing.getNetRequirement()).add(nz(l.getNetRequirement()))); existing.setPlannedQuantity(...)`——对 ORM session 缓存内实体直接 setter 写入）+ 平台实证链：nop-entropy `GraphQLTransactionOperationInvoker#invokeAsync`（仅 `operationType == mutation` 时进 `transactionalInvoker`，query 裸跑）+ `biz-defaults.beans.xml` L40-47（该 invoker 挂入全局 operation 链）+ `OrmSessionImpl#flush`（L176-211：session `dirty` 标志 + `CascadeFlusher(this, cache)` 扫缓存提交脏实体——托管实体 setter 变更在 flush 时会写库）/`#close`（L752-773：无 flush，丢弃）
- **证据**：当前唯一入口 `ErpMfgMrpScenarioBizModel#compareVersions` 为 `@BizQuery`（L58-63）→ 无事务 → session close 丢弃脏变更，**今日无持久化损坏**；但 (a) 同请求后续任何经同 session 的读会看到被改写的实体值（响应序列化本身消费之，属侥幸自洽）；(b) 该 public bean 方法被未来 mutation/job 编排复用（对齐 `flushReleaseOrThrow`/`buildSnapshotSummary` 等既有复用惯例）时，脏 flush 即写 `ErpMfgMrpPlanLine`——静默破坏 simulation-engine.md AP-06「版本一旦生成即不可变」与对比确定性。合并分支触发条件=同物料多条 parentLineId=null 的顶层行（手工 CRUD 建行或未来引擎变更可触发）。
- **问题**：读路径写托管实体的潜伏数据损坏面 + AP-06 不可变约束的架构性脆弱点（同型范式教训：P2-MA4-004「markIssuePosted 依赖脏跟踪」watch-only 的读侧镜像）。
- **建议修复方向**：合并逻辑改为本地累加值（`BigDecimal net = byNet.merge(materialId, ...)` 聚合到独立 Map / DTO），不对实体 setter；或读侧用 `ormTemplate` 只读会话/投影查询。
- **arm-index 裁决**：新增（部分注记——与 P2-MA4-004「脏跟踪依赖」watch-only 同族方向相反（彼为 mutation 依赖正确性、此为 query 路径潜伏写入），控制点不同）。

### P2-CK-mfg2-008（D3/D5）cost-rollup-status 状态机缺失——FIRMED 无命名 writer（仅裸 CRUD 直改 status 可达），DRAFT/CANCELLED 为零 writer 死值且无迁移守卫；而标准成本读侧（findLatestFirmedStandardCost/差异计算）以 FIRMED 为消费前提

- **控制点**：`app/erp/mfg/service/costing/CostRollupService.java#createHead`（L111：`head.setStatus(COST_ROLLUP_STATUS_CALCULATED)` 直写——**CALCULATED 是唯一 Java writer 产出值**）+ grep 实证（全仓生产代码 `setStatus(COST_ROLLUP_STATUS_FIRMED/DRAFT/CANCELLED)` 零命中；仅测试直接置 FIRMED）+ `app/erp/mfg/service/entity/ErpMfgCostRollupBizModel`（裸 `CrudBizModel`，无 firm/cancel purpose-built mutation——`STATUS_FIRMED` 常量 L31 仅读侧 `findLatestFirmedStandardCost` L53 使用）+ 消费面：`ErpMfgCostRollupBizModel#findLatestFirmedStandardCost`（L52-54 `eq("status", "FIRMED")`）与 `costing/ProductionVarianceCalculator.java:351`（`eq("status", COST_ROLLUP_STATUS_FIRMED)`——FIRMED 卷算作为差异标准价源）+ dict `erp-mfg/cost-rollup-status` 4 值（orm L110-115 DRAFT/CALCULATED/FIRMED/CANCELLED）
- **证据**：`bom-and-routing.md §实现注记` 声明「FIRMED 由人工动作置位（Non-Goal）」——但「人工动作」实际不存在，唯一路径是 `ErpMfgCostRollup__update_` 通用字段更新直改 status：① 无 CALCULATED→FIRMED 迁移守卫（任意状态可跳 FIRMED/CANCELLED，可反复横跳）；② 通用 update 是 P1-CK-pur-003 族旁路面。生产运行中 `findLatestFirmedStandardCost` 恒返回 null、差异计算恒回退（除非运维手工改库/CRUD 置 FIRMED）——「已审核工单成本正确性由 FIRMED 材料标准保证」三件套（bom-and-routing.md §实现注记 D2 裁决）在无人工 SQL/CRUD 干预下不闭合。
- **问题**：声明状态机（4 值 dict + FIRMED 门控读侧）零写入编排——D3 死状态家族（对照 P1-MA2-036 MRP CANCELLED 有 Deferred 裁决、P1-MA2-035 作业卡 TRANSFERRED 有 Deferred 裁决，cost-rollup-status 的 DRAFT/CANCELLED/FIRMED **无任何 owner doc Deferred 标注**）+ D5 守卫缺失。
- **建议修复方向**：补 `firm`（CALCULATED→FIRMED）/`cancel`（CALCULATED→CANCELLED）purpose-built mutation + 状态机守卫（对齐 Forecast approve/cancel 范式）；或 owner doc 显式 Deferred 标注 DRAFT/CANCELLED 死值 + FIRMED 经 CRUD 置位的操作口径。
- **arm-index 裁决**：新增（grep「CostRollup FIRMED writer/cost-rollup-status 死状态」零命中；MA2 A2.6b 状态机审查范围=MRP+预测+建议单+BOM+仿真，不含 cost-rollup-status）。

### P2-CK-mfg2-009（D4）ErpMfgBom.useMultiLevelBom 列零消费——owner doc「工单审核时可配置 use_multi_level_bom 控制展开深度」未落地，齐套/预留展开恒多级硬编码

- **控制点**：ORM `app-erp-manufacturing.orm.xml` L206 `<column name="useMultiLevelBom" ... defaultValue="false">` 对照 grep 全 `erp-mfg-service/src/main/java` + `erp-mfg-web` 源——`getUseMultiLevelBom`/`useMultiLevelBom` **仅命中 view.xml 展示字段两处**（`ErpMfgBom.view.xml:29,43`），Java 零消费；实际展开点 `app/erp/mfg/service/workorder/KitAvailabilityChecker.java#explodeRequirements`（L111-141 三个调用点全部 `bomExpander.explode(bomId, requestedQty, true)` / `explodeFromSnapshot(snap, requestedQty, true)`——**硬编码多级**）；`ErpMfgBomBizModel#explode/findBomTree`（L62-75）由调用方传参（GraphQL 参数），不读列
- **证据**：C4.1 报告已记录该事实但归本切片定性：bom-and-routing.md §多级 BOM 展开「工单审核时可配置 use_multi_level_bom 控制展开深度：单级展开（false）/多级展开（true）」——声明配置列无运行时消费。用户在 BOM 上配 false 无任何效果，齐套/预留按多级展开（对含深层 BOM 的产品齐套判定口径与用户配置意图相悖）。
- **问题**：D4「声明配置未落地」家族（对照 P2-CK-mfg-008 consumption 零消费同型）。
- **建议修复方向**：齐套/预留展开读取 `bom.useMultiLevelBom`（工单未指定时回退 BOM 配置）；或 owner doc 显式裁决「展开深度仅由调用方参数控制，列保留展示」并从 §多级 BOM 展开移除「可配置」措辞。
- **arm-index 裁决**：新增（C4.1 遗留未定性项落地；grep「useMultiLevelBom」arm-index 零命中）。

### P2-CK-mfg2-010（D6/D10）BOM.qty 为 0/空时展开静默归零——divide 除数为零返回 ZERO 无错误，qty=0 的 BOM 全部子件有效用量=0，齐套空过（假齐套）/MRP 子件零需求/成本卷算材料成本 0，全链静默

- **控制点**：`app/erp/mfg/service/bom/BomExpander.java#divide`（L227-232：`if (b.signum() == 0) { return BigDecimal.ZERO; } return a.divide(b, SCALE, HALF_UP);`）+ `#expandLines` L124（`BigDecimal scale = divide(qty(requestedQty), nz(bom.getQty()));`——qty null→nz→0 或显式 0 → scale=0 → L126 全部 `effQty = quantity × 0 = 0`）+ `app/erp/mfg/service/costing/CostRollupService.java#divide`（L358-363 同型：`bomQty=0` → `qtyPerUnit=0` → 材料成本静默 0）+ ORM 实证（`ErpMfgBom.qty` L210：`defaultValue="1"` 但**非 mandatory**，且无 >0 域约束；裸 `ErpMfgBomBizModel` CRUD 可写 0/负值）
- **证据**：qty=0（或 null 被旧数据/导入置空）的默认 BOM：KitAvailabilityChecker 需求全 0 → 空需求集全满足 → STOCK_RESERVED 假齐套 → 开工领料才暴露缺料；MrpEngine 子件 gross=0 → `processMaterial` L109 `signum()<=0` return → 子件零建议（静默断链）；CostRollup 材料成本 0 → 标准成本失真。三消费面全静默（无 LOG 无异常），触发条件=主数据质量（CRUD 无校验）。
- **问题**：除零守卫退化为静默 ZERO——比抛错更危险（drp SafetyStockEngine 空列表除零先例的反向变体：彼为崩溃、此为静默错算）；D6 数量守恒 + D10 边界缺失。
- **建议修复方向**：`divide` 除数 ≤0 时抛 `ERR_BOM_QTY_INVALID`（新增错误码）或展开前校验 `bom.qty.signum() > 0`；`ErpMfgBomBizModel` 补 save/update 的 qty>0 与 line quantity>0 校验（defaultPrepareSave/Update）。
- **arm-index 裁决**：新增（grep「BOM qty 0 静默/divide ZERO」零命中；P1-MA4-011(c) 的 CostRollup 成环测试缺口为相邻维度）。

### P2-CK-mfg2-011（D6）同物料多需求日聚合取最晚 requirementDate——整批毛需求按最晚日期倒排提前期，最早需求的供给系统性延迟

- **控制点**：`app/erp/mfg/service/mrp/MrpEngine.java#topDemandsByMaterial`（L240-242：`if (d.getRequirementDate() != null && (t.requirementDate == null || d.getRequirementDate().isAfter(t.requirementDate))) { t.requirementDate = d.getRequirementDate(); }`——**isAfter=取最大/最晚**）+ `#processMaterial` L129-130（`plannedDate = requirementDate.minusDays(leadDays)`）
- **证据**：物料 X 有需求 day1=10、day100=10 → 聚合 gross=20、requirementDate=day100 → plannedDate=day100−lead。day1 的 10 件供给被排到 day100 前才下单 → day1 需求必然延误；正确保守口径应取最早需求日（或按时桶分单）。`plannedDate` 直接落为释放产物的 `ErpPurOrder.deliveryDate`/`ErpMfgWorkOrder.plannedStartDate`（MrpReleaseService L150/L182）——延迟传导到执行单据。mrp.md §实现约定声明「不区分需求时界」（time fence Non-Goal），但**未声明需求日坍缩到单行且取最晚**——坍缩是实现简化、方向选错是缺陷。
- **问题**：D6 日期边界——多需求日物料的 MRP 建议下达日系统性偏晚。
- **建议修复方向**：聚合取最早 requirementDate（最小满足保守约束）；分桶 lot-sizing 归 successor（对齐 owner doc 按期分单愿景）。修复同步 fork 的 `SimulationMrpEngine#topDemandsByMaterial`（L456-458 同型复制）。
- **arm-index 裁决**：新增（grep「requirementDate 最晚/isAfter 聚合」零命中）。

### P2-CK-mfg2-012（D5，同型 P2-CK-mfg-006 / P1-CK-pur-003 族）切片内 20 个裸 CrudBizModel 无状态/不可变守卫——计划行 isFirmed/convertedBillCode、卷算成本行、负荷快照可经通用 update 直改

- **控制点**：`app/erp/mfg/service/entity/` 下本切片实体 BizModel 全部为 15-18 行裸 `CrudBizModel<T>` 零覆写——`ErpMfgMrpPlanBizModel`/`ErpMfgMrpPlanLineBizModel`/`ErpMfgMrpDemandBizModel`/`ErpMfgBomBizModel`/`ErpMfgBomLineBizModel`/`ErpMfgBomOperationBizModel`/`ErpMfgBomByproductBizModel`/`ErpMfgRoutingBizModel`/`ErpMfgRoutingOperationBizModel`/`ErpMfgProductionVersionBizModel`/`ErpMfgWorkcenterBizModel`/`ErpMfgWorkcenterCalendarBizModel`/`ErpMfgWorkcenterCapacityBizModel`/`ErpMfgCrpLoadBizModel`/`ErpMfgCostRollupBizModel`/`ErpMfgCostRollupLineBizModel`/`ErpMfgForecastLineBizModel`/`ErpMfgMrpScenarioBizModel`/`ErpMfgMrpScenarioParamBizModel`/`ErpMfgMrpScenarioVersionBizModel`
- **证据（加重面，超出 mfg-006 已登记的快照族）**：① `ErpMfgMrpPlanLine__update_` 可直改 `isFirmed`/`convertedBillCode`——**破坏释放幂等闭环**（un-firm 后可再释放生成重复 PO/WO，绕过 `ERR_MRP_LINE_ALREADY_FIRMED` 与 P1-MA2-090 修复）；② FIRMED 计划行/COMPLETED 计划可改数量——破坏 pegging 与已释放建议量的一致性；③ `ErpMfgCostRollupLine` 可直改 `unitCost` 等成本值（FIRMED 后无守卫）——标准成本真相可被静默覆写；④ `ErpMfgMrpScenarioVersion` 可直改 `computedMrpPlanId`/`promotedPlanId`——破坏 AP-06 版本不可变与防重复转正守卫；⑤ COMPLETED scenario 的 params 仍可改而 ParamResolver 缓存不失效（设计 AP-06 依赖「版本不可变」前提，CRUD 旁路使其落空）。
- **问题**：全域同型（P1-CK-pur-003 族）在本切片的新站点，按 mission 约定同型登记不重复展开。
- **建议修复方向**：随族统一裁决——释放后/终态实体的 update/delete 守卫（`defaultPrepareUpdate` 校验 isFirmed=false / plan 非 FIRMED / version 非 COMPLETED）；scenario 族 params 在 scenario COMPLETED 后禁改。
- **arm-index 裁决**：同型登记（P1-CK-pur-003 / P2-CK-mfg-006 族，本切片站点新增计数）。

### P3-CK-mfg2-013（D9）MRP 需求聚合与安全库存扫描 N+1 + 全表加载——销售订单全表载入+逐单调行、物料全表扫描+逐物料查余额、CRP 工作中心编码全表内存过滤、FIRMED 卷算头全载+逐头查行

- **控制点**：`app/erp/mfg/service/mrp/DemandAggregator.java#collectSalesOrderDemands`（L89-99：`findAllByQuery(oq)` 全量订单【仅 orgId+CANCELLED 过滤，无状态/日期窗】→ `for (order) { lineDao.findAllByQuery(lq) }` **逐单 N+1**）+ `#collectSafetyStockDemands`（L125-132：`matDao.findAllByQuery(new QueryBean())` **全物料表** + `availableQuantity` 逐物料查余额 N+1）+ `simulation/SimulationMrpEngine.java#applySafetyStockOverride`（L278-286 同型：全物料扫描 + 逐物料 `availableQuantity`）+ `crp/CrpLoadCalculator.java#workcenterCodes`（L477-485：`findAllByQuery(new QueryBean())` 全工作中心载入后内存 `wcSet.contains` 过滤——可用 `in("id", ...)` 下推）+ `entity/ErpMfgCostRollupBizModel#findLatestFirmedStandardCost`（L52-71：全 FIRMED 头载入+内存排序+逐头查行直到命中）
- **问题**：每次 runMrp/runSimulation 的固定成本随订单/物料量线性放大（同型 P3-CK-mfg-014 dashboard 无界加载家族）；结果正确，纯性能。
- **建议修复方向**：行查询改 `in("orderId", ids)` 批量 + 内存按 order 分组；safety stock 扫描下推 `gt("safetyStock", 0)` 过滤 + 余额聚合一次 `in` 查询分组；workcenterCodes/rollup 查询下推过滤。
- **arm-index 裁决**：新增（同型家族注记——P3-CK-pur-011/sal-026/inv-020/mfg-014 族新站点）。

### P3-CK-mfg2-014（D3）mrp-order-type 字典 PLANNED_ORDER 死值 + SUBCONTRACT_REQUEST 引擎零产出——仅 PURCHASE/WORK 两类有引擎 writer

- **控制点**：dict `erp-mfg/mrp-order-type`（orm L79-84 四值 PLANNED_ORDER/PURCHASE_REQUEST/WORK_ORDER_REQUEST/SUBCONTRACT_REQUEST）对照 grep `MRP_ORDER_TYPE_PLANNED_ORDER`（仅 `ErpMfgConstants.java:122` 常量声明，零使用）+ `MrpEngine#processMaterial` L126-128（orderType 仅两值：`manufactured ? WORK_ORDER_REQUEST : PURCHASE_REQUEST`）+ `SimulationMrpEngine` L326-328 同型
- **问题**：PLANNED_ORDER 完全死值（无 writer 无 reader 语义）；SUBCONTRACT_REQUEST 无引擎 writer——`releaseSubcontractRequest`（config-gated）只能释放**手工经 CRUD 建出的** SUBCONTRACT_REQUEST 行（mrp.md L95 注记「本期不支持释放」已被 P2-MA2-046 裁决过时，但引擎产出维度仍空）。D3 dict 可达性家族（P1-MA2-036 同型、轻量）。
- **建议修复方向**：PLANNED_ORDER 删除或 Deferred 标注；委外建议生成归委外 successor（与 mrp.md L95 触发条件对齐）。
- **arm-index 裁决**：新增（P1-MA2-036 覆盖 mrp-status/forecast-status，未覆盖 mrp-order-type）。

### P3-CK-mfg2-015（D7/D2）ErpMfgCrpRunJob 直调 BizModel 无事务包裹 + 失败仅 LOG.error 无告警——清区间与重写非原子，job 中途失败留部分负荷快照

- **控制点**：`app/erp/mfg/service/job/ErpMfgCrpRunJob.java#execute`（L45 `new ServiceContextImpl()` 后 L59 `crpLoadBiz.calculateLoad(...)` **直接 Java 调用注入的 BizModel bean**；L53-55 `catch (Exception e) { LOG.error(...); }` 吞全异常无告警）+ 平台实证：nop-job `BeanMethodJobInvoker`（反射调用 execute，无事务模板；grep nop-job 模块零 ITransactionTemplate）与 nop-entropy `GraphQLTransactionOperationInvoker`（事务仅在 GraphQL mutation 操作层包装——direct bean call 绕过）对照 `ErpMfgCrpLoadBizModel#calculateLoad`（仅 `@BizMutation` 注解，注解本身不给直调加事务）+ `CrpLoadCalculator#calculateLoad`（L97-98 `clearExisting` 逐行 deleteEntity + L114-125 逐单 saveEntity——多语句写序列）
- **证据**：GraphQL 入口调用 calculateLoad = 单事务原子；**job 入口同一逻辑 = 无事务逐语句自动提交**——job 中途失败（如某工单数据异常）留下「旧负荷已删、新负荷写了一半」的部分快照，期间 `getLoadReport` 读到失真数据；下次成功运行自愈。cron 并发重复副作用已由 P1-MA2-086 族（nop-job-local 非分布式裁决）覆盖不重复。`#resolveWindowMonths` L73-77 `catch (NumberFormatException) return 默认` 静默配置回退（同 mfg-016 家族，并入本条）。
- **建议修复方向**：execute 内包事务模板（`ITransactionTemplate.runInTransaction`）或 job 调 GraphQL RPC 通道；失败补 `IErpSysNotificationBiz` 告警（对齐 G3 分级）。
- **arm-index 裁决**：新增（事务边界维度；MA2-086 覆盖并发重复、未覆盖 job 直调无事务原子性）。

### P3-CK-mfg2-016（D6）CRP 产能聚合三处近似——efficiencyFactor 取 per-material 最大值（最乐观）、重叠班次时段双计、shift 起止解析异常静默 0 产能（假超载）

- **控制点**：`app/erp/mfg/service/crp/CrpLoadCalculator.java#efficiencyByWorkcenter`（L456-462：跨多条 WorkcenterCapacity（按物料）行取 `eff.compareTo(existing) > 0` **max**——报表是 workcenter 级，取最乐观物料效率高估产能）+ `#availableHours`（L487-502：逐 calendar `shiftHours` 直接累加——**班次时间重叠时段双计**）+ `#shiftHours`（L518-533：`catch (Exception ex) { return ZERO; }`——"9:00" 等非 HH:mm 格式静默 0 产能 → `computeLoadRate` L535-540 除零守卫返回 9999 → **全域假超载告警**）
- **问题**：crp.md §核心设计点「按产品并行产能/效率系数」未裁决 workcenter 级报表的聚合口径；数据质量边缘（重叠班次/格式错误时间）产生方向相反的失真。CRP 为只读报表（降 P3 依据）。
- **建议修复方向**：效率取产能加权或最小值（保守）；重叠时段区间合并；shift 解析失败 LOG.warn + 报表行标记 invalid。
- **arm-index 裁决**：新增（A4.2b「CrpLoad 双源负荷扎实」结论未覆盖产能聚合口径维度）。

### P3-CK-mfg2-017（D5）releasePurchaseRequest/releaseSubcontractRequest 缺 currencyId null 守卫——必填列触发原始 DB 错误而非业务错误码

- **控制点**：`app/erp/mfg/service/mrp/MrpReleaseService.java#releasePurchaseRequest`（L73-76 仅 `if (supplierId == null) throw ERR_MRP_RELEASE_MISSING_SUPPLIER`——**currencyId 无对偶检查**）+ `#releaseToPurchaseOrder` L148（`order.setCurrencyId(currencyId)` 直写 null）+ ORM 实证 `app-erp-purchase.orm.xml` L294 `currencyId ... mandatory="true"`（`releaseSubcontractRequest` L108-111 同型，`ErpMfgSubcontractOrder.currencyId` 亦必填）
- **问题**：javadoc 自述「currencyId 为必填，由调用方提供」但入参无守卫——null 时 insert 撞必填约束抛 `ERR_ORM_DATA_EXCEPTION` 级原始错误（对照 supplierId 的友好错误码不对称）。
- **建议修复方向**：补 `ERR_MRP_RELEASE_MISSING_CURRENCY` 对称守卫（两处）。
- **arm-index 裁决**：新增（P1-MA2-090 覆盖并发释放键翻译、未覆盖 currencyId 边界）。

### P3-CK-mfg2-018（D2/D6）CostRollupService 配置静默降级 + 委外成本归集分子分母口径错配——overhead 费率解析失败静默 0；委外单位成本 = 整单加工费 / 本物料行数量

- **控制点**：`app/erp/mfg/service/costing/CostRollupService.java#overheadAllocationRate`（L247-258：`catch (NumberFormatException e) { return BigDecimal.ZERO; }`——配置值类型错误静默关闭 overhead 分配，卷算人工/制费分解失真无信号，同型 P3-CK-mfg-016 全域静默 catch 族）+ `#aggregateSubcontractCost`（L265-288：`totalFee` 累加**整单** `order.getProcessingFee()`（L279），`totalQty` 只累加 `loadSubcontractLines(order.getId(), materialId)` **本物料行**数量（L280-282）→ 多物料委外单的单位委外成本分子含其他物料加工费、分母仅本物料量，**系统性虚高**；且 `divide(totalFee, totalQty)` 除数守卫同 L284 归零）
- **问题**：两处均 config-gated（overhead 默认 false / subcontract aggregation 默认 false——降 P3 依据），启用后正确性缺陷即时可见。
- **建议修复方向**：费率解析失败 LOG.warn + 保持默认；委外归集改按行级 unitProcessingFee×quantity 或分子分母同口径（仅本物料行费用）。
- **arm-index 裁决**：新增（catch 族同型注记 P3-CK-mfg-016；费率/归集口径维度 grep 零命中）。

### P3-CK-mfg2-019（D8）plan.orgId 为空时跨组织合并需求/库存 + CRP 全程无组织维度——违反 mrp.md 关键业务规则 5「不跨公司合并需求」的边界未裁决

- **控制点**：`app/erp/mfg/service/mrp/DemandAggregator.java#collectSalesOrderDemands`（L91-93 `if (plan.getOrgId() != null) oq.addFilter(eq("orgId", ...))`）+ `MrpEngine#availableQuantity`（L214-216 同型 if-null-skip）+ `SimulationMrpEngine#availableQuantity`（L430-432 同型）+ ORM 实证 `ErpMfgMrpPlan.orgId` **非必填**（orm L911 无 mandatory）+ `crp/CrpLoadCalculator`（`findWorkOrdersInWindow`/`calculateLoad`/`getLoadReport` 全程零 orgId 维度，`ErpMfgWorkOrder.orgId` 存在但不过滤）
- **问题**：orgId=null 的计划聚合**全部组织**的销售订单/预测/库存——「按公司独立运行」规则对 null-org 计划未定义（orgId 非必填使其可达）；CRP 负荷/报表跨组织混算（多组织部署）。单组织部署无影响（降 P3）。同族注记：P2-CK-mfg-007（C4.1 齐套/看板 orgId）、P2-RC-086（看板行级权限族）。
- **建议修复方向**：runMrp 前置校验 orgId 非空（或 owner doc 裁决 null=全组织计划语义）；CRP 计算补 orgId 参数（job 按 org 迭代）。
- **arm-index 裁决**：新增（P2-CK-mfg-007 族同族不同站点——MRP 计划入口 + CRP 面）。

### P3-CK-mfg2-020（D2）仿真链错误码语义漂移——场景/版本「不存在」抛「无基线计划」/「已转正式计划」错误码，诊断误导

- **控制点**：`app/erp/mfg/service/simulation/SimulationMrpEngine.java#requireScenario`（L495-506：`getEntityById` null → 抛 `ERR_MFG_SIMULATION_NO_BASELINE_PLAN`——场景不存在≠无基线计划）+ `#requireVersion`（L508-515：版本不存在 → 抛 `ERR_MFG_SIMULATION_VERSION_ALREADY_PROMOTED`——不存在≠已转正）+ `simulation/SimulationVersionComparator.java#requireVersion`（L124-131 同型复用）
- **问题**：前端/运维按错误码语义排查方向被误导（传错 scenarioId 得到「无基线计划」、传错 versionId 得到「已转正式计划」）。P3 诊断质量。
- **建议修复方向**：补 `ERR_MFG_SIMULATION_SCENARIO_NOT_FOUND`/`ERR_MFG_SIMULATION_VERSION_NOT_FOUND` 错误码。
- **arm-index 裁决**：新增（grep「错误码 误导/NOT_FOUND simulation」零命中）。

### P3-CK-mfg2-021（D5）MRP 销售订单需求来源含未审核单据——仅排除 CANCELLED，DRAFT/未审批订单进入毛需求（幻影需求）

- **控制点**：`app/erp/mfg/service/mrp/DemandAggregator.java#collectSalesOrderDemands`（L90 `oq.addFilter(ne("docStatus", ErpMfgConstants.SAL_DOC_STATUS_CANCELLED))`——DRAFT/SUBMITTED 等全部纳入；ErpSalOrder 另有 approveStatus 轴（orm L125）完全未消费）
- **问题**：草稿/被驳回订单（docStatus 非 CANCELLED 但 approveStatus 未过）产生 MRP 采购/工单建议，订单作废后建议变冗余。javadoc 明示「排除作废订单」为当前语义——**只登记不裁决**（owner doc mrp.md 需求来源树未指定状态门，属设计层口径缺口）。
- **建议修复方向**：需求来源收紧为「已审批未完结」或 owner doc 显式裁决 DRAFT 纳入口径。
- **arm-index 裁决**：新增（疑似需求分歧，登记）。

### P3-CK-mfg2-022（D5/D6）仿真/转正 code 后缀拼接无长度守卫——base plan code 接近 50 列宽时 `-SIM-V{n}`/`-PROMOTED-{n}` 溢出触发原始 DB 错误

- **控制点**：`app/erp/mfg/service/simulation/SimulationMrpEngine.java#runSimulation` L123（`computed.setCode(basePlan.getCode() + "-SIM-V" + nextVersionNo)`）+ `#promoteToFormalPlan` L207-209（`computed.getCode() + suffix`，suffix=`-PROMOTED-{n}` 最长 11 字符）+ ORM `ErpMfgMrpPlan.code domain="workOrderCode"`（precision 50）——基线 code ≥ 40 时转正 code 溢出，DB varchar 截断/长度错误原始报错
- **证据**：`simulation-engine.md §Code 长度约束` 已知约束（E2E 用短前缀绕开）但生产代码零守卫零友好错误。
- **建议修复方向**：拼接前长度校验（>50−suffix 抛 `ERR_SIMULATION_CODE_TOO_LONG` 类业务码）。
- **arm-index 裁决**：新增（grep「code 长度 后缀 溢出」零命中）。

### P3-CK-mfg2-023（D8/D2）CostRollupService 不写 orgId + 同日重复卷算无去重/无废止——rollup 头 orgId 恒 null，同 BOM 同日多次卷算累积重复 CALCULATED 头

- **控制点**：`app/erp/mfg/service/costing/CostRollupService.java#createHead`（L105-115：set code/businessDate/status/costingVersion——**无 setOrgId**；L109 `code = "ROLLUP-" + today + "-" + bom.getId()`）+ ORM `ErpMfgCostRollup.orgId` 存在（orm L1364，UK `UK_MFG_COST_ROLLUP_CODE_ORG (code, orgId)` 含 NULL 列——多数 DB NULL 互不冲突，**同 code 多行可并存**）+ `entity/ErpMfgCostRollupBizModel#findLatestFirmedStandardCost`（L52-71 亦无 orgId 过滤，跨组织读 FIRMED 价）
- **问题**：同 BOM 同日重跑卷算每次新建 CALCULATED 头（无「未 FIRMED 旧版作废」逻辑）——CALCULATED 头无界累积、最新有效版需人工甄别；orgId 空置使跨组织卷算互相可见（物料为全局主数据，影响有限——降 P3）。
- **建议修复方向**：createHead 写 orgId（经 context）+ 重跑时将既有未 FIRMED 同 BOM 头置 CANCELLED（依赖 008 的 cancel writer）；findLatestFirmedStandardCost 补 orgId 参数。
- **arm-index 裁决**：新增（grep「CostRollup orgId/重复卷算」零命中）。

## 跨域关联影响面注记（不新建 finding）

| 已登记 finding | 本切片受影响面 | 结论 |
| --- | --- | --- |
| `P0-CK-mfg-001`（完工入库幂等键 (ERP_MFG_WORK_ORDER, code) 吞增量报工） | **同型检查结论：非同型**——MRP 释放产物幂等键为 `PO-MRP-{lineId}`/`WO-MRP-{lineId}`（`MrpReleaseService` L144/L174），设计语义是**计划行级单次释放**（`requireReleasable` isFirmed 守卫 + UK 翻译 `flushReleaseOrThrow` L289-299 → `ERR_MRP_LINE_ALREADY_RELEASED`，P1-MA2-090 修复在位），无增量语义冲突 | 无需新 finding；释放幂等验证为正确 |
| `P1-CK-inv-001`（出库策略 locationId 回退误用 warehouseId） | 本切片不构造库存移动单——MRP 释放产物为 PO/WO 骨架（无 location 维度），BOM/CRP/仿真/卷算均为只读或计划数据 | **无传导面** |
| `P1-CK-fin-003`（post() 幂等命中返回 null 与 dispatcher null=失败语义冲突） | 本切片零 PostingDispatcher（差异/委外 dispatcher 归 C4.1/C4.3 已查） | 无传导面 |
| `P1-CK-pur-003`/`P2-CK-mfg-006`（CRUD update 无守卫全域族） | 本切片 20 个裸 BizModel 站点 + 计划行 isFirmed/成本行/场景版本加重面 | **同型登记为 P2-CK-mfg2-012** |
| `P2-CK-mfg-013`（cron 键漂移家族） | CRP job 无死键——`erp-mfg.crp-run-cron` 为实际消费键（`ErpMfgCrpRunJob` L64），旧键 `erp-mfg.crp-run-schedule` 代码零残留（owner doc 已标注「已取代」）；双层门控（nop.job enabled 默认 false + cron 空跳过）与 crp.md 描述一致 | 无新 finding；接线验证为正确 |

## 验证为正确（显式排除，防误报）

- **D1 机械扫描全零**：本切片 41 文件 `@Inject private`=0、`System.currentTimeMillis/LocalDateTime.now/new Date()`=0（全部 `CoreMetrics.today()/currentTimestamp`）、`extends RuntimeException/Exception`=0、字典字符串 `==` 比较=0、无手编生成产物。
- **daoFor 跨域读站点已裁决**：`BomExpander`/`MrpEngine`/`DemandAggregator`/`CostRollupService`/`CrpLoadCalculator`/`SimulationMrpEngine`/`SimulationVersionComparator` 的 `daoFor(ErpMd*/ErpInv*/ErpSal*)` 只读直访经 **P1-MA4-012**（resolved，`data-dependency-matrix.md §9` 豁免）裁决，不按 D1 反模式登记；`MrpReleaseService` 跨域**写**（ErpPurOrder/ErpMfgWorkOrder/ErpMfgSubcontractOrder 骨架）有 javadoc 实现说明 + O-4 架构豁免登记（`posting-exemptions.md`，P1-MA2-038 resolved），不重复登记。
- **MRP CANCELLED / 预测 CONSUMED 死状态已裁决**：`P1-MA2-036`（resolved）——owner doc `mrp.md §实现约定` Deferred 标注在位（L89/L90），状态机 Bean 显式 refuse-dead-state，不重复登记（`ErpMfgMrpPlanStateMachine` L26-27/L44-52 javadoc 自证）。
- **MRP 释放幂等与并发**：`requireReleasable`（isFirmed 守卫 + orderType 校验）+ `flushReleaseOrThrow`（UK 违例翻译 `ERR_MRP_LINE_ALREADY_RELEASED`）+ `advancePlanToFirmedIfComplete`（全行 firmed 才 FIRMED，空行集不误升）——`P1-MA2-090` 修复在位验证。
- **runMrp 状态机与行清理安全**：`assertCanRun` 仅 null/DRAFT（`ErpMfgMrpPlanStateMachine` L49-53）→ COMPLETED/FIRMED 计划不可重跑 → `clearLines` 只会在 DRAFT 计划执行，firmed 行无被删路径；promoted DRAFT 计划重算清行属设计内（simulation-engine.md §转正后重算语义注记）。
- **BOM 展开环检测/深度上限/phantom 语义正确**：`expandLines` DFS path 回溯（L111-141 finally remove）+ `ERR_BOM_MAX_DEPTH_EXCEEDED`（默认 15）+ phantom 并入父层级不产节点（L128-131）——与 bom-and-routing.md §多级 BOM 展开一致；`CostRollupService#computeUnit` 自有 path 环检测 + 按物料记忆化（`computed` map）正确（成环路径测试缺口已由 P1-MA4-011(c) 登记）。
- **乐观锁在位**：切片全部实体（Bom/BomLine/BomOperation/MrpPlan/MrpPlanLine/MrpDemand/Forecast 族/CrpLoad/Workcenter 族/Scenario 族/CostRollup 族）`versionProp="version"`（orm 逐实体实证）——`markFirmed`/状态推进 updateEntity 受版本守护。
- **事务边界**：runMrp/runSimulation/release*/promote* 全部经 Facade `@BizMutation`（GraphQL mutation 操作层事务包装，平台实证），引擎异常整体回滚无 RUNNING 悬挂；`SimulationMrpEngine` 仿真隔离性成立——只写 computed plan/lines/scenario/version，不触碰主数据与单次引擎（E2 零触及，`MrpEngine`/`DemandAggregator` 类文件未被仿真修改）。
- **`addOrderField(name, desc)` 使用对照**：`DemandAggregator#nextLineNo` `addOrderField("lineNo", true)`=DESC 取 max 正确（与 003 的 `false` 形成仓内对照）；`BomExpander.findDefaultBomOrNull/loadLines/loadOperations` 的 `false`=ASC 为确定性 tiebreaker（同物料多默认 BOM 时取最小 id，语义中性）非缺陷。
- **Forecast 消费区间相交正确**：`le("periodStart", planEnd) + ge("periodEnd", planStart)` 区间相交判定正确；horizon 空按当天匹配与 mrp.md L80 注记一致；config-gate `forecast-consume-enabled` 在位。
- **需求时界 / AUTO_SCHEDULED / CRP 停机扣减 / 班次级粒度 Non-Goal** 与实现一致（mrp.md L96 / crp.md §实现约定）——D4 无虚假接线。
- **CRP job 接线完整**：`erpMfgCrpRunJob` bean（app-service.beans.xml）+ `erp-mfg-crp-run.job.yaml`（invoker bean/method 对上）+ scheduler.yaml enabled + `erp-mfg.crp-run-cron` 空值跳过——双层门控与 crp.md §配置点描述一致（对照 mfg-013 键漂移家族，本处无漂移）。
- **CRP 双源切换与 SPI 降级正确**：`isApsLoadSourceEnabled`（config 非 APS 返回 false；APS 但 provider 空 list 时 warn + 回退 WORK_ORDER）+ `ioc:collect-beans` 接线（beans.xml）——crp.md §负荷来源双源落地一致；APS 模式 fallback 日志记录来源分布（L126-129）在位。
- **ParamResolver 缓存不失效**：AP-06 设计裁决（版本不可变，参数变更须新建版本）——owner doc §ParamResolver 缓存约束显式裁决，非缺陷（CRUD 旁路面已并入 P2-CK-mfg2-012⑤）。
- **E3.1/E4.1 成本脱敏链在位**：`CostRollupLineBizModel` totalCost/unitCost masking + 4 要素 band 代理 + `findLatestFirmedStandardCost` masking + `TestErpMfgCostRollupValueExemptionInvariant` 守卫引用——本切片未发现脱敏回归（plan 2026-08-25-1956-1 新近落地，未深查 xmeta 层，见剩余风险）。
- **委外释放 config-gate**：`releaseSubcontractRequest` 双守卫（enabled + supplierId）+ APPROVED 直置 + postedStatus DRAFT 的 O-4 豁免 javadoc 在位（P1-MA2-038 resolved），不重复登记。

## arm-index 复用 or 新增裁决（汇总）

- 关键符号 `安全库存 双扣`/`低阶码`/`nextVersionNo`/`forecast warehouseId MRP DRP`/`releaseToWorkOrder routingId CRP`/`totalPurchaseAmountDelta`/`useMultiLevelBom`/`cost-rollup-status FIRMED writer`/`BOM qty 0 静默`/`requirementDate isAfter` 在 `docs/audits/arm-index.md` **零命中 → 新增**。
- **复用（不重复登记，报告中注记）**：
  - MRP CANCELLED / Forecast CONSUMED 死状态 → **P1-MA2-036**（resolved，Deferred 裁决在位）——不登记。
  - daoFor 跨域读 / MrpRelease 跨域写豁免 → **P1-MA4-012 / P1-MA2-038**（resolved，§9 豁免 + posting-exemptions）——不登记。
  - MRP 释放并发/幂等 → **P1-MA2-090**（resolved，flushReleaseOrThrow 在位）——验证为正确。
  - CostRollup 成环测试缺口 → **P1-MA4-011(c)**（resolved 范畴）——不登记。
  - cron 并发重复副作用 → **P1-MA2-086**（nop-job-local 非分布式族）——P3-CK-mfg2-015 仅登记 job 直调无事务原子性新维度。
  - 静默 catch 配置回退族 → **P3-CK-mfg-016 族**——P3-CK-mfg2-018 同型注记。
  - CRUD 无守卫 → **P1-CK-pur-003 / P2-CK-mfg-006 族**——P2-CK-mfg2-012 同型登记。
  - orgId 隔离缺失 → **P2-CK-mfg-007 / P2-RC-086 族**——P3-CK-mfg2-019 同族新站点。
  - 无界加载 → **P3-CK-mfg-014 家族**——P3-CK-mfg2-013 同型登记。
- **与 arm-index 结论冲突（建议主 agent 复核）**：A4.2b（`2026-07-29-0024-arm-ma4-mfg-mrp-quality-code-quality.md`）声称「MrpEngine 净需求/低层码递归……扎实」——**P1-CK-mfg2-001/002 以代码+算例+测试断言面证伪该结论的净额口径维度**（彼审聚焦算术结构与主路径断言，未覆盖 SAFETY_STOCK 预净额组合与跨分支可用量重复扣减；测试未断言相关字段使缺陷对彼审不可见）。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 3 | P1-CK-mfg2-001..003 |
| P2 | 10 | P2-CK-mfg2-004..012（012 为同型登记） |
| P3 | 11 | P3-CK-mfg2-013..023 |

按主维度（每 finding 唯一归属，合计 23）：D6×7（001/002/003/004/010/011/016）、D5×4（012/017/021/022）、D8×3（005/019/023）、D4×2（006/009）、D7×2（007/015）、D3×2（008/014）、D2×2（018/020）、D9×1（013）。次维度注记：007 跨 D7/D1、010 跨 D6/D10、015 跨 D7/D2、018 跨 D2/D6、008 跨 D3/D5、003 为平台 API 布尔参数（addOrderField desc）误用的正确性后果。

跨域关联注记 4 项（P0-CK-mfg-001 非同型结论 / P1-CK-inv-001 无传导面 / P1-CK-fin-003 无传导面 / pur-003 族同型登记 012）。

## 剩余风险（查了什么/没查什么）

- **已查**：切片 41 文件中 31 个逐行深读（BomExpander/CostRollupService/MrpEngine/DemandAggregator/MrpReleaseService/CrpLoadCalculator/SimulationMrpEngine/SimulationVersionComparator/ErpMfgSimulationParamResolver/ErpMfgCrpRunJob/2 状态机/Bom/MrpPlan/MrpPlanLine/CrpLoad/Forecast/CostRollup/MrpScenario BizModel/6 per-mutation Processor），其余 10 个裸 CrudBizModel 与 CostBandClassifier/IErpMfgSimulationParamResolver 抽查确认；平台语义实证 6 处（`QueryBean.addOrderField`+`OrderFieldBean.forField` desc 布尔、`GraphQLTransactionOperationInvoker` 仅 mutation 包装 + `biz-defaults.beans.xml` 接线、`OrmSessionImpl.flush/close` 脏检查、nop-job `BeanMethodJobInvoker` 无事务、`FilterBeans.eq/ne null` IS NULL 族沿用已验证先例、`OrmSession` 托管实体 setter 脏标记）；ORM 逐列核对 8 实体（Bom/BomLine/WorkOrder 计划列/MrpPlan 族/ScenarioVersion UK/ForecastLine warehouseId/CostRollup/MdMaterial 无 standardCost/PurOrder currencyId 必填）；跨域对读 `DrpDemandAggregator`；测试面断言核实（TestErpMfgMrpEngine 安全库存断言止于 gross、TestErpMfgMrpSimulation 最多两版本运行）；D4 接线（beans.xml/job yaml/scheduler/无 MRP batch = MANUAL 声明一致）。
- **未深查**：`erp-mfg-web` AMIS view.xml 契约 drift（归 C8.2）；`ErpMfgReportBizModel` + `crp-load-report.xpt.xml` 报表渲染（报表切片，C4.1 已列未深查）；`ProductionVarianceCalculator` 本体（C4.3，仅读其对 FIRMED 卷算的消费点 L351）；xmeta 层守卫（前端/GraphQL schema 侧对 qty>0、scenarioVersion 只读字段是否有拦截——若有则 010/012 触发面收窄但后端无守卫事实不变）；E2E spec（`mfg-mrp-simulation.action.spec.ts`）断言面；`SimulationMrpEngine` 类 javadoc 步骤 2 声明「调用 DemandAggregator.aggregate」与实际 `loadDemands(basePlan)` 直读的陈旧漂移（行为符合 simulation-engine.md §与既有 Forecast 输入边界「不重新触发消费」——设计正确，仅 javadoc 陈旧，未立案）。
- **owner doc 漂移注记（只登记不裁决）**：`mrp.md §实现约定 L94` 称「scrapRate 为 VARCHAR」——ORM 实为 DECIMAL(10,4)（orm L251），损耗率未纳入计算的原因陈述失真（主数据类型已支持，属实现 Non-Goal 而非类型限制）。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-mfg2-001/002（MRP 净需求双扣族）**——与 A4.2b 审计结论直接冲突；若产品语义接受「MRP 为粗估建议、计划员人工复核」，severity 可降 P2，但安全库存补货在 available≥safety/2 时**恒不触发**是硬性失效。建议用「safety=100, available=99」与「双父件共享子件 + 中间库存」两个集成测试实证 netRequirement。
  2. **P2-CK-mfg2-007（comparator 托管实体写入）**——「今日不落库」依赖 query 无事务的平台接线（已实证 biz-defaults 链），但若项目对 GraphQL query 另配了事务模板（web 层自定义 invoker），则升级为现实数据损坏；建议主 agent 在运行态确认 query 路径 session 只读性。
  3. **P2-CK-mfg2-004（预测仓级行 MRP 消费）**——mrp.md L43 与 ORM 列注释/DRP javadoc 存在需求分歧，修复方向（MRP 过滤 vs 契约改声明）需 owner doc 裁决，不宜仅按代码判断。
