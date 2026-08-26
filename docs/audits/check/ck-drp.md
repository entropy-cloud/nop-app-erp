# ck-drp — drp（分销需求计划 / 补货 / 安全库存 / 越库 / 提前期）实现代码检查报告

> 工作项：C7.4。执行日期：2026-08-26。执行者：域检查子代理（ZCode 会话，read-only）。
> 范围：`module-drp/erp-drp-service/src/main/java` 全部 38 个手写生产文件——净需求链 3 类（`drp/DrpDemandAggregator`/`DrpEngine`/`DrpReleaseService`）、安全库存 1 类（`safetystock/SafetyStockEngine`）、仿真链 4 类（`simulation/SimulationDrpEngine`/`ErpDrpSimulationParamResolver`/`IErpDrpSimulationParamResolver`/`DrpSimulationVersionComparator`）、状态机 Bean 2 类（`statemachine/ErpDrpPlanStateMachine`/`ErpDrpLineStateMachine`）、per-mutation Processor 12 类（Plan×3/Line×4/Scenario×2/SafetyStockCalc×2/CrossDock/LeadTime）、越库 Job 1 类（`job/ErpDrpCrossDockStagingTimeoutJob`）、实体 BizModel 12 类、Configs/Constants/Errors 3 类。跨文件核实：`module-drp/model/app-erp-drp.orm.xml`（12 实体 versionProp/UK/索引/列集）、`erp-drp-service/_vfs/erp/drp/beans/app-service.beans.xml`（含 job bean 接线）、`app-erp-all/_vfs/nop/job/conf/erp-drp-xdock-staging-timeout.job.yaml`、`erp-drp-meta` dict（drp-plan-status/drp-line-status/xdock-status/simulation-status/simulation-param-type 等）、`erp-drp-web/auth/erp-drp.action-auth.xml`（手写 0 FNPT vs 生成基线 `_erp-drp.action-auth.xml` 44 FNPT）、测试 19 文件断言面抽查（TestErpDrpEngine/TestErpDrpSafetyStock/TestErpDrpSimulation 等）。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。38 文件全量通读（无抽样）+ 平台 API 布尔/语义参数先查 nop-entropy 源码再定性（`QueryBean.addOrderField(name, desc)`→`OrderFieldBean.forField` desc=false 生成 `asc`，nop-entropy `QueryBean.java` L433-437 + `OrderFieldBean.java` L53-58 实证）+ 跨域实体字段实证（`StockMoveBookkeeper.recomputeAvailable` available=total−reserved−locked；`ErpPurOrder.warehouseId` 收货仓库列存在；`ErpInvTransferOrderLine` 无 received 列；`erp-inv/move-status`={DRAFT,CONFIRMED,DONE,CANCELLED}）+ ORM UK/乐观锁逐实体核对 + arm-index drp 条目（P2-RC-069..072、P1-RC-081/082、P1-MA1-022）逐条裁决 + purchase 侧 Facade 调用点（`ErpPurReceiveProcessor` L310-330/L368-390）隔离语义核验。
> 切片边界：`erp-drp-api` 骨架与 `erp-drp-web` AMIS/flux 页面契约 drift 不深查（仅抽查 dict 死值消费、dashboard 净需求页、action-auth 注册面）；测试代码仅用于行为语义交叉验证；`_gen/` 产物不查。
> 注：按 mission 硬约束本报告只落本文件；`ai-check-index.md` 的 finding 行登记由主 agent 执行。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-drp-001（D6，跨 D8）净需求公式可用量双计——currentStock 取 available（已扣 reserved+locked），allocatedQty 再加回 reserved：reserved 计两次、locked 多扣一次，净需求系统性虚高 → 过量补货

- **控制点 A**：`app/erp/drp/service/drp/DrpDemandAggregator.java#sumAvailable`（L175-188：`ctx.currentStock = sumAvailable(...)` 读 `ErpInvStockBalance.availableQuantity`，null 回退 `total−reserved−locked`）+ `#sumReserved`（L190-199：`ctx.allocatedQty = sumReserved(...)` 读同一批 balance 行的 `reservedQuantity`）
- **控制点 B**：`app/erp/drp/service/drp/DrpEngine.java#runDrp`（L84-92：`BigDecimal net = safetyStock.add(nz(ctx.forecastDemand)).subtract(nz(ctx.currentStock)).add(nz(ctx.allocatedQty)).subtract(nz(ctx.onOrderQty));`）
- **证据**：跨域语义实证——`module-inventory/.../stock/StockMoveBookkeeper.java#recomputeAvailable`（L224-228：`balance.setAvailableQuantity(total.subtract(reserved).subtract(locked))`）。代入：net = SS + F − (T−R−L) + R − O = **SS + F − T + 2R + L − O**。设计公式（`docs/design/drp/README.md §关键业务规则 1`、UC-DRP-02 步骤 4 逐字）`netRequirement = max(0, safetyStock + forecastDemand - currentStock + allocatedQty - onOrderQty)` 中 currentStock 语义为**在手总量**（当前库存）——经典净需求 = SS + F − (T−R) − O 恰等于公式字面（−T+R）；实现把 available 填入 currentStock 槽位后 reserved 被扣两次（available 内一次 + allocatedQty 加回一次）+ locked 额外多扣一次。算例：T=100、R=30、L=0、SS=50、F=0、O=0 → 设计 net = 50−100+30 = −20 → 0（不补货）；实现 net = 50−70+30 = **10 → 补 10**。R 越大过量补货越严重（净需求虚高恰等于 R+L）。测试遮蔽实证：`TestErpDrpEngine#seedBalance`（L360-372）只设 total=available、reserved 未设（默认 0）→ 双计对全部引擎测试不可见。同型控制点：`simulation/SimulationDrpEngine.java#runSimulation`（L113-117 同公式 fork，完全继承）。
- **问题**：D6 业务计算正确性——reserved/locked 非零的每个仓×物料组合净需求虚高，直接多生成补货建议/采购单。与 `P1-CK-mfg2-001`（MRP 可用量双扣，方向为低估）属同一「可用量重复扣减」家族的 drp 站点，方向相反（drp 高估→过量补货）。
- **建议修复方向**：`sumAvailable` 改聚合 `totalQuantity`（或在 DrpEngine 中改用 `total−locked` 作 currentStock），保持「−currentStock + allocatedQty」公式字面语义；同步 fork 的 `SimulationDrpEngine`；补「reserved>0 时断言 netRequirement」回归测试（现 seed 全部 reserved=0）。
- **arm-index 裁决**：新增（grep arm-index「available/双计/reserved 双扣」零命中；P2-RC-069 是 net≤0 生成行维度、A4.2.170 探针也未覆盖 available 口径——其断言基线建立在 reserved=0 种子上）。

### P1-CK-drp-002（D6，跨 D8）在途调拨量不过滤 DONE 完成单——已完成调拨货物已入 currentStock 仍永久计入 onOrderQty：供给双计 → 净需求系统性低估，DRP 释放的调拨单自身完成后即成为永久幻影供给（释放闭环自噬）

- **控制点**：`app/erp/drp/service/drp/DrpDemandAggregator.java#inboundTransferQty`（L212-232：`oq.addFilter(eq("toWarehouseId", warehouseId)); oq.addFilter(ne("docStatus", "CANCELLED"));`——**唯一排除项是 CANCELLED**；L229-231 行级无条件 `total.add(nz(l.getQuantity()))`）
- **证据**：`erp-inv/move-status` dict（`module-inventory/erp-inv-meta/_vfs/dict/erp-inv/move-status.dict.yaml`）值域 {DRAFT, CONFIRMED, **DONE**, CANCELLED}；`ErpInvTransferOrder.docStatus` 绑定该 dict（`app-erp-inventory.orm.xml` L608）。调拨完成后目标仓 stock balance 已含到货量（currentStock 读到），而该 DONE 调拨单行 quantity 仍全额计入 onOrderQty → 同一批货算两次供给 → net = SS + F − stock − onOrder 被永久压低。**DRP 自释放链自噬**：`DrpReleaseService#releaseToTransferOrder`（L187-209）生成 `DRP-TO-{lineId}` 调拨单（DRAFT）→ 下次 runDrp 正确计入在途（防重复补货，闭环设计意图）；该单经 inventory 域走完 DRAFT→CONFIRMED→DONE 后，onOrderQty **仍然**全额计它（DONE 未过滤）→ 从此每次 runDrp 供给双计。行级无 received 列可依赖（`ErpInvTransferOrderLine` 列集 orm L663-680 实证仅 quantity/batchNo 等）→ 头级 docStatus 是唯一可过滤维度。
- **问题**：D6 在途口径 + D8 释放闭环断裂——README 反模式警示 3「净需求计算忽略在途库存」的对偶缺陷（在途**永不过期**）；持续运行下补货触发被系统性抑制（库存越充足越接近「永不补货」）。
- **建议修复方向**：`inboundTransferQty` 头查询过滤改为排除终态（`docStatus IN (DRAFT, CONFIRMED)` 或至少排除 DONE——若 XMeta 过滤操作集不支持 notIn，参考 CrossDockProcessor 的 Java 侧过滤先例）；补「DONE 调拨不计在途」回归测试。
- **arm-index 裁决**：新增（grep「DONE 调拨/onOrder 完成单」arm-index 零命中；P2-RC-069/070 均未覆盖在途口径）。

### P1-CK-drp-003（D6，跨 D8）未到货采购量不过滤收货仓库与组织——仓库 A 的净需求计入发往仓库 B 的在途采购：跨仓在途污染 → 欠补；orgId 亦未过滤（跨组织混算）

- **控制点**：`app/erp/drp/service/drp/DrpDemandAggregator.java#unreceivedPurchaseQty`（L235-260：`oq.addFilter(ne("docStatus", "CANCELLED"));`——**仅有此一过滤**，`warehouseId` 入参全程未使用（方法签名 `unreceivedPurchaseQty(String materialId, String warehouseId)` L235 但查询体零引用），orgId 同样零引用；L249-251 行级仅 `eq("materialId") + in("orderId")`）
- **证据**：`ErpPurOrder` 存在「收货仓库」列 `warehouseId`（`module-purchase/model/app-erp-purchase.orm.xml` L544，propId 7；行级 L638 同名列）——发往仓库 B 的在途采购全部计入仓库 A 的 onOrderQty。算例：物料 X 在仓 A 无任何本地采购，但存在发往仓 B 的未到货 PO 100 → 仓 A 的 DRP onOrderQty=100 → net 被压低 100 → **A 仓缺货不触发补货**。多组织部署下不同 org 的 PO 同样混入（`ErpPurOrder.orgId` 列存在，`loadParametersInScope` 对参数按 plan.orgId 过滤而采购/调拨/库存查询全部不过滤——跨查询口径不一致）。对照正确先例：同文件 `indexForecastByMaterialWarehouse` 对预测头做了 `eq("orgId", plan.getOrgId())`（L129-130）。
- **问题**：D6 在途口径正确性 + D8 orgId 透传——净需求公式的 onOrderQty 分量对多仓/多组织数据系统性失真（方向：供给虚高 → 欠补，与 002 同向叠加）。
- **建议修复方向**：`unreceivedPurchaseQty` 补 `eq("warehouseId", warehouseId)`（头级收货仓库）+ orgId 过滤（与 008 联动修复）；补「跨仓 PO 不计入本仓在途」回归测试。
- **arm-index 裁决**：新增（grep「采购在途 仓库过滤/onOrder warehouse」arm-index 零命中）。

### P2-CK-drp-004（D3，跨 D8）resetToDraft 行级联与设计偏差——仅清 SUGGESTED 行：①APPROVED 行在 DRAFT 计划下仍可释放（释放无 plan 状态守卫，终点靠 advanceToExecuted 断言兜底回滚、错误码误导）；②重跑后新行 lineNo 从 10 起与存续行重复；③totalReplenishmentQty 置 null 抹掉已释放事实

- **控制点 A**：`app/erp/drp/service/drp/DrpEngine.java#resetToDraft`（L129-141：`clearSuggestedLines(lineDao, planId)`——`#clearSuggestedLines` L165-173 仅删 `status=SUGGESTED`；javadoc 自述「保留 APPROVED/ORDERED/CANCELLED 终态或已审批行不动」）
- **控制点 B**：`app/erp/drp/service/drp/DrpReleaseService.java#releaseLine`（L67-98：仅 `requireReleasable` 查行状态——**不校验 line.planId 对应计划的 status**）+ `#advancePlanToExecutedIfComplete`（L139-143：全行终态时 `planStateMachine.assertCanAdvanceToExecuted(plan.getStatus())`——plan 处于 DRAFT 时在此抛 common 码异常回滚，兜底在但报错为「状态机非法迁移」而非业务语义）
- **证据**：`docs/design/drp/state-machine.md §2 迁移表` APPROVED→DRAFT 行：「清除计算结果的明细行（同 COMPUTED→DRAFT）」——存续 APPROVED 行未清除即偏离设计。传导链：APPROVED 计划 resetToDraft → 存续 APPROVED 行仍可 releaseLine（行级守卫通过）→ 下游单据创建成功 → advance 断言 DRAFT 不可 EXECUTED → 事务回滚（自洽但错误码误导且整次释放作废）；部分释放场景（行未全部终态）则**不触发 advance** → DRAFT 计划下产生 ORDERED 行（下游单据已出库流程）→ 再 runDrp 时 clearSuggestedLines 不清 ORDERED 行、新行 lineNo=10 与存续行 lineNo 撞号（`ErpDrpLine` 无 (planId,lineNo) UK，orm L168-184 实证——无 DB 拦截，页面行号重复/排序混乱）→ `totalReplenishmentQty=null`（L139）抹掉已释放量口径。owner doc §审查提示明列「参数调整后回退 DRAFT 是否清理旧建议行」为本域设计关注点。
- **问题**：D3 状态机级联完整性 + D8 计划-行状态一致性。
- **建议修复方向**：resetToDraft 拒绝存在 APPROVED 行的计划（或级联取消 APPROVED 行/阻止 reset）；releaseLine 补「plan.status=APPROVED」前置守卫（提前失败优于终点断言回滚）；runDrp 的 lineNo 起点改为「该计划现有最大 lineNo+10」。
- **arm-index 裁决**：新增（grep「resetToDraft 保留 APPROVED/lineNo 撞号」arm-index 零命中；A1.48 UC-DRP-03 接受声明未覆盖 reset 级联维度）。

### P2-CK-drp-005（D5，跨 D3）approveLine 单行批准不回填 approvedQty——正建议量行经 approveLine + releaseLine 生成 0 数量调拨单/采购单（P2-RC-069「0 值释放」家族的加重控制点：不再限于 net≤0 行）

- **控制点 A**：`app/erp/drp/service/entity/ErpDrpLineBizModel.java#approveLine`（L68-82：`line.setStatus(lineStateMachine.approveLineTargetStatus()); updateEntity(line, null, context);`——**无 approvedQty 回填**）对照 `processor/ErpDrpPlanApprovePlanProcessor.java#approvePlan`（L60-63：`if (line.getApprovedQty() == null || line.getApprovedQty().signum() <= 0) { line.setApprovedQty(line.getSuggestedQty()); }`——计划级批量批准有回填）
- **控制点 B**：`app/erp/drp/service/drp/DrpReleaseService.java#releaseLine`（L67-98：无 `approvedQty > 0` 守卫）→ `#releaseToTransferOrder` L206 / `#releaseToPurchaseOrder` L231：`toLine.setQuantity(nz(line.getApprovedQty()))`
- **证据**：`DrpEngine#runDrp` L109 生成行时 `line.setApprovedQty(BigDecimal.ZERO)`——单行路径 approveLine 不回填则 approvedQty 恒 0。触发链（全部经设计内命名 mutation + 状态机 Bean 合法边）：runDrp（行 suggestedQty=70，approvedQty=0）→ approveLine（SUGGESTED→APPROVED，approvedQty 仍 0）→ releaseLine（APPROVED→ORDERED，生成 `DRP-TO-{lineId}` 数量 **0** 的调拨单 + `DRP-PO-{lineId}` 路径同理）。下游 0 量单可被接受已被 arm MA4 运行时探针实证（A4.2.170：`ErpInvTransferOrderConfirmProcessor#confirm` 仅 DRAFT 守卫、`ErpPurOrderSubmitForApprovalProcessor` 仅非空行校验，`TestErpDrpScheduleRelease#testZeroValueLineReleaseGeneratesZeroQuantityOrder` PASS）。与 P2-RC-069 的差异：该 finding 覆盖「net≤0 生成 0 值行 → 批量批准回填 0 → 释放 0 量单」；本控制点是**净需求为正的建议行**走单行批准路径同样产出 0 量单——修复面扩大（releaseLine 守卫可同时覆盖两站点）。
- **问题**：D5 守卫完整性——两条设计内批准路径行为不一致，单行路径产出错误数量的下游单据。
- **建议修复方向**：approveLine 与 approvePlan 对齐回填 approvedQty（或统一在 releaseLine 补 `approvedQty>0` 守卫——一处修复覆盖 P2-RC-069 + 本条两站点）。
- **arm-index 裁决**：P2-RC-069 家族新增控制点（独立登记供修复追踪；修复方向与其 todo 项 (a)(b) 合并实施）。

### P2-CK-drp-006（D6，跨 D8）安全库存三级优先链（override > calculated > parameter）未接入 runDrp——DrpEngine 直接读 ErpDrpParameter.safetyStock，findEffectiveSafetyStock 生产零消费（仅 BizQuery 暴露）

- **控制点 A**：`app/erp/drp/service/drp/DrpEngine.java#runDrp`（L84：`BigDecimal safetyStock = nz(param.getSafetyStock());`——直接读参数原值）
- **控制点 B**：`app/erp/drp/service/safetystock/SafetyStockEngine.java#findEffectiveSafetyStock`（L167-187：三级链 `overrideSafetyStock > calculatedSafetyStock > param.safetyStock` 完整实现）——grep 实证生产代码消费点仅 `ErpInvDrpSafetyStockCalcBizModel#findEffectiveSafetyStock`（@BizQuery，L50-52）与 javadoc 注释（`DrpDemandAggregator` L47-48 声称「SS 优化结果经 findEffectiveSafetyStock 注入，见 DrpEngine」——**注释与实现相反**，DrpEngine 无任何注入）
- **证据**：`docs/design/drp/safety-stock-optimization.md §业务规则 1` 逐字：「**计算结果仅是建议**…DRP 运行时优先取 overrideSafetyStock，其次取 calculatedSafetyStock，最后取 ErpDrpParameter.safetyStock 原值」。现状：用户配置 overrideSafetyStock 或刚 calculate 完的 calculatedSafetyStock，在下一次 runDrp 中**不生效**（除非显式 confirmWriteback 回写参数）——三级链中前两级对 DRP 主路径为死链。设计同时声明「计算结果直接覆写 DRP 参数」为反模式（须人工确认），但规则 1 的运行时优先链与人工门并不冲突（读侧优先链 + 写侧人工门）。SimulationDrpEngine fork 同样不消费（且其 SAFETY_STOCK 场景覆盖走 ScenarioParam 通道，语义独立）。
- **问题**：D6 设计业务规则未落地 + 注释失真（DrpDemandAggregator javadoc 声称的注入链不存在，审计追溯会被误导）。
- **建议修复方向**：DrpEngine（及 fork）取安全库存改为经 `findEffectiveSafetyStock(materialId, warehouseId, orgId)`；或 owner doc 修订规则 1 为「仅经 confirmWriteback 单通道生效」并删除 DrpDemandAggregator 失真注释（需求分歧需 owner doc 裁决——按检查纪律登记不裁决）。
- **arm-index 裁决**：新增（grep「overrideSafetyStock 优先/findEffectiveSafetyStock 消费」arm-index 零命中；A1.48 UC-DRP-06 接受声明「override/calculated/parameter 三级」未验证其接入 DRP 运行）。

### P2-CK-drp-007（D9）聚合器 N+1 + 全表扫描——每个参数行触发 5 次查询，其中未到货采购量每次全表加载非作废采购订单、在途调拨每次加载全仓调拨单：P 个参数 ≈ P×全表

- **控制点**：`app/erp/drp/service/drp/DrpDemandAggregator.java#aggregate`（L83-96：`for (ErpDrpParameter param : parameters) { ctx.currentStock = sumAvailable(...); ctx.allocatedQty = sumReserved(...); ctx.onOrderQty = onOrderQty(...); ... }`）——`#sumAvailable`（L175-188）与 `#sumReserved`（L190-199）**各自**全量加载同批 balance 行（同查询跑两次）；`#inboundTransferQty`（L208-233）每次加载该仓全部非作废调拨单+行；`#unreceivedPurchaseQty`（L235-260）每次加载**全组织全部**非作废采购订单（头查询无任何业务过滤，仅行级 materialId）
- **证据**：参数量 = 仓×物料组合数（典型 ERP 数百至数千）。1000 参数 ×（2×balance 查询 + 2×调拨查询 + 2×采购查询，含头+行）≈ 6000 次查询，其中 2000 次为采购单头全表 findAllByQuery——runDrp 在真实数据量下退化为分钟级。对照聚合正确先例：同文件 forecast 索引即「预聚合 Map 一次构建循环内查」（L80、L92-93）——stock/onOrder 三类输入未采用同模式。
- **问题**：D9 性能——净需求计算主路径在数据量增长下不可用级退化。
- **建议修复方向**：仿 forecast 索引模式批量预聚合（balance 按 material+warehouse 一次 group、采购/调拨按头状态+仓库过滤后一次加载行级 in(materials) 建 Map）；sumAvailable/sumReserved 合并为单次遍历。
- **arm-index 裁决**：新增（grep「DrpDemandAggregator N+1/全表」arm-index 零命中；P1-MA1-022 resolved 覆盖的是跨域 daoFor 架构合法性非查询效率）。

### P2-CK-drp-008（D8）跨域读查询零 orgId 过滤——stock balance/调拨单/采购单/出库移动四类查询均不滤组织（plan.orgId 只过滤参数与预测头）：多组织部署跨组织混算（orgId 隔离族 drp 读侧站点）

- **控制点**：`DrpDemandAggregator#loadParametersInScope`（L167-173：`if (plan.getOrgId() != null) q.addFilter(eq("orgId", plan.getOrgId()))`——参数有滤）对照同文件 `#sumAvailable`/`#sumReserved`（L176-199 无 orgId）、`#inboundTransferQty`（L212-215 无）、`#unreceivedPurchaseQty`（L240 无）、`#indexForecastByMaterialWarehouse` 头查询有滤（L129-130）但行查询按 headIds 已间接隔离；`SafetyStockEngine#monthlyDemands`（L277-285：仅 moveType/posted/warehouseId/businessDate——无 orgId）与 `#leadTimeSample`（L243-250：无 orgId）
- **证据**：`ErpInvStockBalance.orgId` 列存在且写入（`StockMoveBookkeeper#buildNewBalanceForMove` L172 `balance.setOrgId(move.getOrgId())`）；`ErpInvTransferOrder.orgId`（orm L603）、`ErpPurOrder.orgId`、`ErpInvStockMove.orgId` 均在。org1 的 DRP 计划会把 org2 的库存/在途/出库历史聚合进净需求与安全库存统计。注：orgId=null 的 plan 当前会加载全组织参数（filter 跳过）——同根因。
- **问题**：D8 orgId 透传缺失（读侧 filter 形态；对照 b2b 写侧 writer 形态）——多组织数据互相污染净需求/安全库存口径。
- **建议修复方向**：四类跨域查询补 orgId 过滤（与 003 联动）；plan.orgId 为 null 时的口径（全组织 or 拒绝）需 owner doc 裁决。
- **arm-index 裁决**：orgId 隔离族（`P2-CK-fin2-007`/`P2-CK-mfg-007`/`P2-CK-hr-003`④/`P2-CK-b2b-003` 写侧）drp 读侧站点，独立登记。

### P2-CK-drp-009（D6）安全库存月度需求序列不零填充——仅聚合「有出库记录的月份」，零需求月永不参与均值/标准差（设计 KEEP 语义未落地），σ 低估 → 安全库存建议偏低

- **控制点**：`app/erp/drp/service/safetystock/SafetyStockEngine.java#monthlyDemands`（L303-316：`Map<YearMonth, BigDecimal> byMonth = new HashMap<>(); for (ErpInvStockMoveLine l : lines) { ... byMonth.merge(ym, nz(l.getQuantity()), BigDecimal::add); } List<BigDecimal> result = new ArrayList<>(byMonth.values());`——**只有出现过的月份**进入序列）+ `#applyZeroDemandPolicy`（L319-332：EXCLUDE 仅过滤「和 ≤0 的已存在条目」，无法创造零月条目——策略对「无出库月」两态均无效果）
- **证据**：`docs/design/drp/safety-stock-optimization.md §数据清洗规则` 逐字：「历史月份中有 0 需求（停产/断供）| **保留 0 值在标准差计算中（反映真实变异）**，或排除（可配置）」。算例（historyMonths=6，实际 3 个月有出库 [100, 120, 110]，3 个月停产）：设计 KEEP 口径 σ(mean=110) over [100,120,110,0,0,0] ≈ 49.9；实现 σ over [100,120,110] ≈ 8.16——**σ 低估 ~6 倍**，SS 建议按比例偏低（低服务水准）。零月缺失同时使「历史月份不足配置月数」的降级判定失真：`doCalculate` L112 `monthlyDemands.size() < 2` 用观察月数而非日历覆盖月数（6 个月窗口只有 1 个月有出库 → size=1 → 降级 SIMPLE 是侥幸正确；有 2 个月 → STATISTICAL 但样本口径与配置 6 月不符无提醒）。
- **问题**：D6 统计口径正确性——间歇性需求物料（行业常见）安全库存建议系统性偏低。
- **建议修复方向**：按 `historyMonths` 生成完整日历月序列（YearMonth.range），无出库月填 0 后再交 zero-policy（KEEP 保留/EXCLUDE 剔除——此时 EXCLUDE 才有真实语义）；降级判定改用日历覆盖月数。
- **arm-index 裁决**：新增（grep「零需求月/zero-demand 填充」arm-index 零命中；P2-RC-072 是 20% 告警维度）。

### P2-CK-drp-010（D3，跨 D5）仿真结果计划混入正式计划面——runSimulation 产出真实 ErpDrpPlan（COMPUTED + SUGGESTED 行），可被标准 approvePlan/releaseApproved 直接审批释放：绕过 promoteToFormalPlan 通道 → 与转正计划双份补货

- **控制点**：`app/erp/drp/service/simulation/SimulationDrpEngine.java#runSimulation`（L85-95：`ErpDrpPlan computed = daoProvider.daoFor(ErpDrpPlan.class).newEntity(); computed.setCode(basePlan.getCode() + "-SIM-V" + nextVersionNo); ... computed.setStatus(DRP_PLAN_STATUS_DRAFT)` → L146 `computed.setStatus(DRP_PLAN_STATUS_COMPUTED)`——**真实计划实体落库且终态为 COMPUTED**）对照 `#promoteToFormalPlan`（L168-229：设计的正式化通道，复制行到新 DRAFT 计划 + 版本 ARCHIVED）
- **证据**：SIM 计划与正式计划同表同状态机：计划员在计划列表对 `-SIM-V1` 计划调用 approvePlan（COMPUTED 合法边）→ releaseApproved → 生成调拨/采购单；同场景 promoteToFormalPlan 转正的计划再走一遍 → **同物料同仓双份补货**（runSimulation 聚合与正式 runDrp 输入相同，净需求一致）。计划无任何「仿真」标记列（`ErpDrpPlan` 列集 orm L90-126 无 scenario 关联/标志），列表/审批/释放路径无法区分。缓解：`erp-drp.simulation-enabled` 默认 false（config-gated）；`DrpSimulationVersionComparator`/转正链路本身正确。同构注记：mfg `SimulationMrpEngine` 同范式（ck-mfg-bom-mrp 未单列此维度——MRP 释放产物为 planned order 建议非直接采购单，暴露面不同）。
- **问题**：D3 状态机治理——仿真产物与正式产物无隔离，设计「转正式计划走 promoteToFormalPlan」的人工门可被旁路。
- **建议修复方向**：approvePlan（或 approveLine/releaseLine）拒绝 code 含 `-SIM-V` 的计划属脆弱方案——正道是 ORM 加仿真标志列（dual-agent）或在 ErpDrpScenarioVersion.completedDrpPlanId 反查拒绝；或 owner doc 裁决「SIM 计划可直接审批」为允许并登记双补货风险。
- **arm-index 裁决**：新增（grep「SIM 计划 旁路/promote 绕过」arm-index 零命中）。

### P2-CK-drp-011（D2）仿真参数解析器进程级缓存永不失效——ErpDrpSimulationParamResolver.cache 无 TTL 无失效钩子，invalidateCache 仅测试调用：场景参数经 CRUD 修改后仿真永远用旧值

- **控制点**：`app/erp/drp/service/simulation/ErpDrpSimulationParamResolver.java`（L29 `private final Map<String, List<ErpDrpScenarioParam>> cache = new HashMap<>();`——单例 bean 字段；L75-83 `loadParams` computeIfAbsent；L84-88 `invalidateCache()`）——grep 实证 `invalidateCache` 生产调用零命中（仅 `TestErpDrpSimulation` 4 处测试调用）
- **证据**：`ErpDrpScenarioParamBizModel` 为裸 CrudBizModel（L11-15，无 defaultPrepareSave/Update 钩子）——参数行增删改不触任何缓存失效。触发链：runSimulation（场景 S 首次加载参数缓存）→ 用户经 CRUD 修改 S 的 SAFETY_STOCK 覆盖值 → 再次 runSimulation（场景重置 DRAFT 后）→ resolver 命中**进程缓存旧值** → 仿真结果与界面显示的参数不一致，且无任何告警。应用重启前永久陈旧。缓解：simulation 默认关闭 + 场景重置本身需 CRUD 直改（见 012 可达性注记）。
- **问题**：D2 数据闭环——配置变更对消费路径静默不可见（陈旧读）。
- **建议修复方向**：去掉缓存（每次 runSimulation 一次查询，成本可忽略——参数表按 scenarioId 单查）；或 ScenarioParam BizModel 的 save/update/delete 钩子调 invalidateCache。
- **arm-index 裁决**：新增（grep「param resolver 缓存/invalidateCache」arm-index 零命中）。

### P2-CK-drp-012（D6，同型 P1-CK-mfg2-003）nextVersionNo 升序取最小 versionNo+1——同场景第 3 次仿真运行 versionNo 与既有 v2 撞 UK_DRP_SCENARIO_VERSION_SCN_VER（未翻译约束异常中断）

- **控制点**：`app/erp/drp/service/simulation/SimulationDrpEngine.java#nextVersionNo`（L257-265：`q.addOrderField("versionNo", false); q.setLimit(1); ... return top.get(0).getVersionNo() + 1;`——平台实证 `addOrderField(name, desc)` 第二参 desc=false 生成 `versionNo asc`（nop-entropy `QueryBean.java` L433-437→`OrderFieldBean.forField` L53-58），**取的是最小 versionNo**）+ ORM `module-drp/model/app-erp-drp.orm.xml` L552-554 `UK_DRP_SCENARIO_VERSION_SCN_VER (scenarioId, versionNo)`
- **证据**：与 `P1-CK-mfg2-003` 逐位同型（同代码模式 + 同 UK 形态）：run1 → {v1}，next = min(1)+1 = 2 ✓（巧合正确）；run2 → {v1,v2}；run3 → min(1)+1 = **2 与既有 v2 撞 UK** → `saveEntity(version)` 抛原始约束违例（无友好翻译），且 plan code `-SIM-V2` 同步撞 `UK_DRP_PLAN_CODE_ORG`。可达性较 mfg 略低：runSimulation 要求 scenario=DRAFT，跑完即 COMPLETED，重跑须经 CRUD 直改场景状态回 DRAFT（无命名 reset mutation）——但参数修改后重仿真是 `simulation-engine.md §Decision A`「同一业务假设下可有多版本（粗调/细调/最终）」的设计主路径，场景重置是必然前置操作。
- **问题**：同型 P1-CK-mfg2-003（drp 站点登记；因可达性多一道 CRUD 直改门槛，本域定级 P2，修复应与 mfg 侧同批同改）。
- **建议修复方向**：`addOrderField("versionNo", true)` 改 DESC 取 max；补第 3 次运行回归测试。
- **arm-index 裁决**：同型登记（P1-CK-mfg2-003 家族，drp 站点新增计数）。

### P2-CK-drp-013（D5，跨 D8）ErpDrpParameter 无业务唯一键——同物料同仓多参数行物理可能：聚合器逐行独立生成补货行（重复补货）+ requireParameter/findParameter limit(1) 任取一行；ErpInvDrpSafetyStockCalc 同缺 (material,warehouse,org) UK

- **控制点 A**：`module-drp/model/app-erp-drp.orm.xml` ErpDrpParameter（L189-235：**无 unique-keys 块**，仅 4 个普通索引——对照同文件 ErpDrpPlan/ErpDrpScenarioVersion/ErpDrpScenarioParam 均有 UK）+ `DrpDemandAggregator#aggregate`（L83-96：`for (ErpDrpParameter param : parameters)`——每行参数产出一条 AggregatedDemand → 一条 DrpLine）
- **控制点 B**：`DrpReleaseService#requireParameter`（L170-185：material+warehouse(+org) 查询 `setLimit(1)` `list.get(0)`）+ `SafetyStockEngine#findParameter`（L366-376 同构）+ `#findEffectiveSafetyStock`（L167-187 同构 limit(1)）
- **证据**：设计语义「按**仓库×物料**配置的补货策略」（README §核心业务对象 ErpDrpParameter）隐含唯一；UK 缺失下两行同 (M,W) 参数 → runDrp 生成两条同物料同仓 SUGGESTED 行（各自按自己的 safetyStock/orderMultiple 计算）→ 批量批准释放**双份补货**。错误信息 `ERR_DRP_PARAMETER_MISSING` 描述「未配置…无法计算净需求」但 limit(1) 任取也掩盖多行事实。`ErpInvDrpSafetyStockCalc` UK 仅 (code,orgId)（orm L269-271）——同物料同仓多 calc 行时三级链取行不确定。
- **问题**：D5 数据完整性约束缺失（ORM 保护区域——修复走 dual-agent-approval 加 UK）。
- **建议修复方向**：`ErpDrpParameter` 加 UK(orgId, materialId, warehouseId)、`ErpInvDrpSafetyStockCalc` 加 UK(orgId, materialId, warehouseId)（存量重复数据须先清洗）；短期可在 BizModel save 校验查重。
- **arm-index 裁决**：新增（grep「DrpParameter UK/重复参数」arm-index 零命中；A1.7/A1.8 ORM 审计未覆盖此约束维度）。

### P2-CK-drp-014（D5，同型 P1-CK-pur-003 族）CRUD update 无已审守卫——DrpLine.status/approvedQty/orderBillCode、DrpPlan.status/totalReplenishmentQty、SafetyStockCalc.overrideSafetyStock 可经 update__ 直改，终态复活/绕过状态机 Bean 全旁路

- **控制点**：`ErpDrpLineBizModel`/`ErpDrpPlanBizModel`（defaultPrepareSave 仅 businessDate 兜底，defaultPrepareUpdate 未 override）+ 其余 10 个 BizModel 同面（ErpDrpParameter/ErpInvDrpCrossDock/ErpDrpScenario 等）——`line.setStatus(...)`/`plan.setStatus(...)` 的状态轴写入仅存在于命名 mutation，但 CRUD 管道对同名属性无白名单
- **证据**：ORDERED→APPROVED 复活、EXECUTED 计划直改回 DRAFT、approvedQty 事后篡改（与 orderBillCode 回写失同步）、SafetyStockCalc.overrideSafetyStock 直写绕过 overwrittenBy 审计（设计 §业务规则 2「人工覆盖需记录 overwrittenBy」——CRUD 路径零 writer）——全部绕过本域引以为界的 StateMachine Bean 矩阵与 011-08-12-1841-1 Phase 2 守卫体系。
- **问题**：同型 P1-CK-pur-003 / P2-CK-mfg-006 族——drp 站点登记（不复用展开）。
- **建议修复方向**：defaultPrepareUpdate/Save 守卫：status 变更仅允许经命名 mutation；overrideSafetyStock 写入时强制 overwrittenBy；approvedQty/orderBillCode 拒绝 CRUD 直写。
- **arm-index 裁决**：同型登记（P1-CK-pur-003 族，drp 站点新增计数）。

### P3-CK-drp-015（D2，跨 D4）confirmWriteback Processor 死读 auto-writeback 配置 + forecast-horizon 配置定义零消费且默认值与 owner doc 漂移

- **控制点 A**：`app/erp/drp/service/processor/ErpInvDrpSafetyStockCalcConfirmWritebackProcessor.java#confirmWriteback`（L28-34：`AppConfig.var(ErpDrpConfigs.CONFIG_DRP_SS_AUTO_WRITEBACK, ErpDrpConfigs.DEFAULT_DRP_SS_AUTO_WRITEBACK);`——**读取结果直接丢弃**，无 if 分支；自动回写语义本由「calculate 不回写」隐式保证，该 config 成死读）
- **控制点 B**：`app/erp/drp/service/ErpDrpConfigs.java` L19-20（`CONFIG_DRP_DEFAULT_FORECAST_HORIZON_DAYS = "erp-inv.drp-default-forecast-horizon-days"` / 默认 **30**）——grep 实证生产零消费；对照 `docs/design/drp/README.md §配置点` 同键默认 **90**（设计漂移）
- **证据**：两处 wiring 失真：死读代码误导维护者以为存在自动回写门；horizon 键 30 vs 90 若未来接线将以错误默认生效。
- **问题**：D2/D4 声明 vs 实现一致性（低危——无运行时行为影响）。
- **建议修复方向**：删除死读（或真正实现 auto-writeback 门——与人工门设计冲突不建议）；horizon 键删除或对齐设计默认并登记消费点。
- **arm-index 裁决**：新增（P2-RC-072 覆盖 20% 告警未覆盖此两键）。

### P3-CK-drp-016（D6）STATISTICAL 法零提前期静默产出 SS=0/ROP=0——DEFAULT_REPLENISHMENT_LEAD_TIME_DAYS=0 兜底无告警（ERR_DRP_PARAMETER_LEAD_TIME_INVALID 已定义零使用）

- **控制点**：`app/erp/drp/service/ErpDrpConstants.java` L75（`int DEFAULT_REPLENISHMENT_LEAD_TIME_DAYS = 0;`）+ `SafetyStockEngine#resolveLeadTimeDays`（L342-351：calc.leadTimeDays→param.replenishmentLeadTime→0 兜底）+ `#doCalculate`（L124-137：leadTime=0 且无样本时 `sqrt(BigDecimal.ZERO)=0`（L412-417 sqrt 对 signum≤0 返回 ZERO）→ `SS = z × σ_d × 0 = 0`、`ROP = 0 + μ_d × 0 = 0`）
- **证据**：未配置提前期且提前期样本 <5 时（新物料常态），STATISTICAL 静默算出全零建议——用户在界面看到 0 而非「缺配置」提示；若随后 confirmWriteback（人工门在，但界面上 0 值易被误确认）会把 param.safetyStock 覆写为 0。错误码 `ERR_DRP_PARAMETER_LEAD_TIME_INVALID`（ErpDrpErrors L110-113「提前期非法（须为正数）」）零使用。
- **问题**：D6 边界——缺输入静默降级为零建议（对照 UC-DRP-06 异常「历史数据不足时降级使用 SIMPLE 方法」有显式降级路径，提前期缺失无对应处理）。
- **建议修复方向**：resolveLeadTimeDays 兜底 0 时抛 ERR_DRP_PARAMETER_LEAD_TIME_INVALID（或降级 SIMPLE + lastCalculatedAt 备注）。
- **arm-index 裁决**：新增（grep「lead time 缺失 SS=0」零命中）。

### P3-CK-drp-017（D3）空计划/全取消计划滞留 APPROVED——approvePlan 零行不拒、cancelLine 不触发 advance、all.isEmpty() 早退三者叠加：EXECUTED 不可达（可 resetToDraft 恢复）

- **控制点**：`ErpDrpPlanApprovePlanProcessor#approvePlan`（L50-65：`suggestedLinesOf(planId)` 为空时循环零次，plan 无条件置 APPROVED）+ `ErpDrpLineCancelLineProcessor#doCancel`（L33-47：行置 CANCELLED 后**不调** advancePlanToExecutedIfComplete——对照 releaseLine L96 有调）+ `DrpReleaseService#advancePlanToExecutedIfComplete`（L129-131：`if (all.isEmpty()) return;`）
- **证据**：两条滞留路径：① runDrp 时参数范围空（orgId 不匹配/参数未配）→ 零行 COMPUTED → approvePlan → APPROVED → releaseApproved 返回 0（无行可释放）→ EXECUTED 永不可达；② 单行计划 approve 后 cancelLine → 全行终态但无 advance 触发点 → 滞留 APPROVED。均可 resetToDraft（APPROVED→DRAFT 合法边）恢复，无数据损坏。
- **问题**：D3 状态机完备性——非终态死端（owner doc §5「所有行完成后 DrpPlan.status → EXECUTED」对取消完结路径未闭环）。
- **建议修复方向**：cancelLine 后补 advancePlanToExecutedIfComplete 调用；approvePlan 对零行计划拒绝（或允许并定义直达 EXECUTED 语义）。
- **arm-index 裁决**：新增（grep「空计划 APPROVED 滞留」零命中）。

### P3-CK-drp-018（D5）22 个命名 mutation 零 FNPT 注册——手写 action-auth 仅菜单资源，生成基线 44 FNPT 只覆盖 CRUD 操作：test profile 非管理员可 update__ 明细却不可 runDrp/approvePlan（deny-by-default 内部不一致）

- **控制点**：`module-drp/erp-drp-web/src/main/resources/_vfs/erp/drp/auth/erp-drp.action-auth.xml`（全文仅 SUBM 菜单资源，`resourceType="FNPT"` 计数 **0**）对照生成基线 `_erp-drp.action-auth.xml`（FNPT 计数 44，grep runDrp/approvePlan/releaseLine/confirmWriteback/runSimulation 等命名 mutation **零命中**——44 条全为 CRUD find/save/update/delete 派生）
- **证据**：平台实证（b2b-011 同源）：无 @Auth 注解的 biz 方法自动派生默认权限 `BizObj:opType|BizObj:name` 且 deny-by-default。`%test` profile（enable-action-auth=true）下非管理员角色对全部 22 个命名 mutation（runDrp/resetToDraft/approvePlan/approveLine/rejectLine/cancelLine/releaseLine/releaseApproved/calculate/confirmWriteback/findEffectiveSafetyStock/findNetReqGroups/runSimulation/promoteToFormalPlan/compareVersions/receiveMark/match/load/complete/cancel/markReceivedFromPurchase/recordFromPurchaseReceive/findLeadTimeStats/recalculateLeadTimeStats）不可调，但同实体 CRUD 有授权可调——权限面内部倒挂（能改数据不能走流程）。`%dev`/`%prod` enable-action-auth=false 无运行时差异（与 cs 域 0 FNPT 同项目级不一致形态，但 drp 有生成基线 FNPT 存在使倒挂显式化）。
- **问题**：D5 权限注册完整性（低危——部署配置关闭 action-auth）。
- **建议修复方向**：action-auth.xml 为 22 个命名 mutation 补 FNPT（角色对齐菜单归属：计划族→计划员/计划主管，释放→计划员，SS 回写→计划主管，仿真→计划员等）。
- **arm-index 裁决**：新增（b2b-011 家族 drp 站点；cs 0 FNPT 未登记为项目级不一致注记）。

### P3-CK-drp-019（D6，跨 D4）仿真 LEAD_TIME 参数变体三处就绪零消费——dict/UK/resolveLeadTimeOverride 全在，SimulationDrpEngine fork 只消费 SAFETY_STOCK/REPLENISHMENT_QTY：用户配置 LEAD_TIME 覆盖静默无效

- **控制点**：`IErpDrpSimulationParamResolver#resolveLeadTimeOverride`（L31-33 default 方法就绪）+ ORM `erp-drp/simulation-param-type` dict 三键（orm L571 ext:dict）+ `UK_DRP_SCENARIO_PARAM_SCN_MAT_WH_TYPE`（orm L596-598 含 paramType 维度）——对照 `SimulationDrpEngine#runSimulation`（L110-119：仅 `resolveSafetyStock` + `resolveOrderMultiple`，**无 resolveLeadTimeOverride 调用**；fork 的净需求公式亦无提前期分量）
- **证据**：用户为场景配置 LEAD_TIME 覆盖值 → runSimulation 无任何读取 → 仿真结果与不配该参数完全相同，无告警。根因：DRP 净需求公式本身不含提前期维度（提前期仅在 SS 引擎消费），参数类型设计先行于消费面。
- **问题**：D6/D4 声明 vs 实现——参数维度死配置（对比同文件 mfg 侧 fork 是否消费 LEAD_TIME 未查——MRP 有时间栅栏维度可能消费）。
- **建议修复方向**：runSimulation 对 LEAD_TIME 覆盖经 `findEffectiveSafetyStock` 式通道传入 SS 计算（需先落地 006），或 owner doc 登记该参数类型 Deferred。
- **arm-index 裁决**：新增（grep「LEAD_TIME 覆盖 零消费」零命中）。

### P3-CK-drp-020（D10）not-found 误码族 + O-11 错误码群零使用——requireVersion 报 ALREADY_PROMOTED、requireScenario 报 NO_BASELINE_PLAN、requireCalc 报 METHOD_UNSUPPORTED；另有 8 个已定义错误码全域零使用

- **控制点**：`SimulationDrpEngine#requireScenario`（L267-278：scenarioId 为 null/**记录不存在**均抛 `ERR_DRP_SIMULATION_NO_BASELINE_PLAN`——语义是「未设置基线计划」）、`#requireVersion`（L280-287：不存在抛 `ERR_DRP_SIMULATION_VERSION_ALREADY_PROMOTED`——语义是「已转正不可重复」）、`DrpSimulationVersionComparator#requireVersion`（L87-94 同构）、`SafetyStockEngine#requireCalc`（L378-389：不存在/ID 空抛 `ERR_DRP_SS_METHOD_UNSUPPORTED`——语义是「方法不支持」）
- **证据**：排错面：版本不存在报「已转正」、场景不存在报「未设基线」、calc 不存在报「方法不支持」——三类错误码参数（ARG_METHOD 塞「安全库存计算记录不存在: {id}」中文串）语义错位。零使用错误码群：ERR_DRP_PLAN_ALREADY_RUN/ERR_DRP_NET_REQ_NEGATIVE/ERR_DRP_STOCK_BELOW_SAFETY/ERR_DRP_STOCK_BELOW_REORDER/ERR_DRP_SS_SERVICE_LEVEL_INVALID/ERR_DRP_RELEASE_FAILED/ERR_DRP_PARAMETER_LEAD_TIME_INVALID/ERR_DRP_CALC_ENGINE_ERROR（016 已覆盖最后一个）。另 `zForServiceLevel`（L353-364）对未知 serviceLevel 静默按 95%（Z=1.645）而非抛 SERVICE_LEVEL_INVALID。
- **问题**：D10 错误归类质量（无数据损坏；b2b-012 同族）。
- **建议修复方向**：require* 族改专用 NOT_FOUND 语义码（或复用 ERR_DRP_PLAN_NOT_FOUND 模式增补）；未知 serviceLevel 抛 SERVICE_LEVEL_INVALID。
- **arm-index 裁决**：新增（b2b-012 家族 drp 站点）。

## 跨域/同型关联影响面注记（不新建 finding）

| 已登记 finding / arm 条目 | drp 受影响面 | 结论 |
| --- | --- | --- |
| `P1-CK-mfg2-001`（MRP SAFETY_STOCK 可用量双扣） | drp 侧形态不同：mfg=聚合器预净额+引擎再净额（**低估**）；drp=available 与 allocated 双计（**高估**，本报告 001）——同「可用量重复处理」家族不同站点不同方向 | drp 站点独立登记为 P1-CK-drp-001（非复用） |
| **SafetyStockEngine ↔ mrg 需求计算同型双扣核查**（任务点名） | mfg SAFETY_STOCK 需求源为 `ErpMdMaterial.getSafetyStock()`（mfg DemandAggregator L39/L132 实证）；drp 侧 confirmWriteback 只写 `ErpDrpParameter.safetyStock`——**两列互不共享，无 SafetyStockEngine→MRP 双扣通道** | **核查不成立**（无同型双扣；跨引擎双补风险在预测行分工——`P2-CK-mfg2-004` mfg 侧已登记，drp 侧 `ne("warehouseId", null)` 契约正确，见「验证为正确」） |
| `P1-CK-mfg2-003`（nextVersionNo ASC） | `SimulationDrpEngine#nextVersionNo` 同代码模式 + 同 UK 形态 | 同型登记为 P2-CK-drp-012（修复应同批） |
| `P1-CK-pur-003` 族（CRUD update 无守卫） | DrpLine/DrpPlan 状态轴 + overrideSafetyStock 审计字段全可 update__ 直改 | 同型登记为 P2-CK-drp-014 |
| orgId 隔离族（`P2-CK-fin2-007`/`P2-CK-mfg-007`/`P2-CK-hr-003`④/`P2-CK-b2b-003`） | drp 为**读侧** filter 缺失形态（数据本身有 orgId，查询不滤） | 独立登记为 P2-CK-drp-008（并入 003 联动修复） |
| `P2-RC-069`（todo — net≤0 生成 0 值行 + runBy 未回写） | 现状一致：0 值行仍生成（DrpEngine L90-92/L109）+ `setRunBy` 生产零调用（grep 实证；ORM runBy 列 orm L99 就绪无 writer，DrpEngine L120 注释声称 BizModel 注入但 Processor L29-32 未设） | 复用不展开（加重控制点另登 P2-CK-drp-005） |
| `P2-RC-070`（todo — 采购补货单 unitPrice/amount=0 + per-line 错误标记缺失） | 现状一致：releaseToPurchaseOrder L232-233 单价/金额 0 + `resolveDefaultCurrencyId` L250-256 任取首个活跃币种（残留风险 javadoc 自认）+ releaseApproved 单事务 all-or-nothing（UC-DRP-04「该行标记错误并重试」未落地） | 复用不展开 |
| `P2-RC-071`（todo — ErpDrpParameter 参数校验缺失） | 现状一致：ErpDrpParameterBizModel 裸 CRUD 无 defaultPrepareSave 校验（负 safetyStock/非正 orderMultiple/min>max 全穿透——orderMultiple≤0 在 roundToMultiple L158-159 被静默当 lot-for-lot） | 复用不展开（013 的 UK 维度独立登记） |
| `P2-RC-072`（todo — 20% 偏差告警缺失） | 20% 告警仍缺（SafetyStockEngine javadoc L58 自认 Non-Goal）；联合变分分量已由 RC-R1.82 落地（验证见下节） | 复用不展开（剩余面维持 todo） |
| `P1-MA1-022`（resolved — 跨域 daoFor） | DrpDemandAggregator/DrpReleaseService/SafetyStockEngine 跨域 daoFor 有 javadoc 理由（聚合行级批量读取/释放 purpose-built 缺失说明） | 修复在位沿用 |
| `P1-RC-081`/RC-R1.81（done — CrossDock）与 `P1-RC-082`/RC-R1.82（done — LeadTime） | 本轮全量复审其交付物（Processor/Job/统计/评分） | 修复在位验证（见下节；新发现的边缘问题已按维度登记，无回退） |

## 验证为正确（显式排除，防误报）

- **历史除零缺陷修复在位**（任务点名核查）：`SafetyStockEngine#monthlyDemands` 双兜底——moves 为空返回 `[BigDecimal.ZERO]`（L286-291，注释「避免 mean() 除零」）+ byMonth 空同样补零（L313-315）；`#mean`（L391-397）分母 values.size()≥1 恒成立；`#stddev` size<2 返回 ZERO（L399-402）；STATISTICAL 样本 <2 走 `ERR_DRP_SS_INSUFFICIENT_HISTORY` 降级 SIMPLE（L111-116 + calculate L89-99）——**无除零路径，修复确认在位**。注：该修复本身无直接单测（TestErpDrpSafetyStock 六用例均 seed ≥1 月出库），修复阶段可补。
- **P1-CK-mfg2-001 同型双扣核查结论**（任务点名）：mfg 的 SAFETY_STOCK 需求读 `ErpMdMaterial.safetyStock`（物料级），drp 的 SafetyStockEngine/confirmWriteback 只写 `ErpDrpParameter.safetyStock`（仓×物料级）——两列零共享，**不存在 SafetyStockEngine 回写喂给 MRP 双扣的通道**；跨引擎双补的实际风险点在预测行分工（MRP 未过滤仓级行，`P2-CK-mfg2-004` 已在 mfg 报告登记），drp 侧 `lq.addFilter(ne("warehouseId", null))`（DrpDemandAggregator L147）与 ORM 列注释「空=产品级 MRP 消费，填=仓级 DRP 消费」契约一致——**drp 侧无缺陷，不适用登记**。
- **平台 `addOrderField(name, desc)` 语义**（C1.2 校准先例沿用并再实证）：nop-entropy `QueryBean.java` L433-437 + `OrderFieldBean.java` L53-58（`desc=false` 生成 `name asc`）——drp 全域唯一调用点即 012 的缺陷站点本身，无其他误用。
- **`FilterBeans.ne(name, null)` → IS NOT NULL**：预测仓级过滤（L147）依赖此语义，先例沿用，正确。
- **状态机 Bean 矩阵与 owner doc 一致（B2 dict 死状态核查）**：`ErpDrpPlanStateMachine` 5 边（DRAFT→COMPUTED→APPROVED→EXECUTED + COMPUTED/APPROVED→DRAFT）与 state-machine.md §2 逐边一致；APPROVED 非终态有显式 D-DRP-1 裁决注记（owner doc §1/§3 局部「终态」表述的漂移已知，Bean 按出边如实编码）；`ErpDrpLineStateMachine` 4 边、ORDERED/CANCELLED 终态无出边；dict 死值扫描：drp-plan-status 4 值/drp-line-status 4 值/replenishment-type 2 值/xdock-status 6 值/simulation-status 4 值/drp-lt-flag 3 值/supplier-grade 4 值**全部有 writer**（唯一例外 simulation-param-type 的 LEAD_TIME 消费缺失 → P3-CK-drp-019）；dashboard 净需求页（net-requirement.flux.yaml）消费 plan.status 透传渲染，无死状态分支。
- **乐观锁全实体在位（D5）**：orm 12 实体全部 `versionProp="version"`（ErpDrpPlan L89/ErpDrpLine L132/ErpDrpParameter L191/ErpInvDrpSafetyStockCalc L240/ErpInvDrpCrossDock L285/ErpInvDrpDockAppointment L351/ErpInvDrpLeadTimeRecord L397/ErpInvDrpSupplierScore L437/ErpDrpScenario L491/ErpDrpScenarioVersion L527/ErpDrpScenarioParam L564 逐一核对）。
- **并发 runDrp / 并发 releaseLine（D7）**：均为 @BizMutation 单事务 + 实体经 getEntityById 加载（携带 version）+ updateEntity 版本检查——并发双跑后提交者在 plan 更新处乐观锁失败整体回滚（中途写入的重复行随回滚消失）；并发双释放同理（后者的下游单据创建随行更新失败回滚；`DRP-TO/PO-{lineId}` 确定性 code 提供二级防重）。state-machine.md §4「并发多人审批同一计划：乐观锁，后提交者失败」兑现。
- **D1 机械扫描零命中**：`@Inject private`=0（全包级可见）、`System.currentTimeMillis`/`new Date()`/`LocalDateTime.now()`/`LocalDate.now()` 生产代码=0（全 `CoreMetrics.*`）、`extends RuntimeException/Exception`=0（业务异常全部 NopException + ErpDrpErrors 中文描述）、`printStackTrace`=0、字符串 `==` 仅 `plan.getStatus() != null` null 判定（合法）。
- **D2 宽 catch 三处全部闭环**：`ErpInvDrpCrossDockProcessor#markReceivedFromPurchase` L188-194 catch→包装 NopException **重抛**（不吞）；`ErpDrpCrossDockStagingTimeoutJob#execute` L96-98 顶层 catch→LOG.error（job 边界，失败可见）；`#runStagingTimeoutFallback` L128-131 逐条 catch→LOG.warn 隔离继续（设计声明的失败隔离语义）。drp 无 posted 类标志位，无 B1 吞异常悬挂形态。purchase 侧 Facade 调用点（`ErpPurReceiveProcessor` L328-331/L388-391）catch+warn 隔离不阻断收货主流程——跨域失败面在调用方有日志（升级通道缺失属 purchase 域范畴）。
- **beans.xml 接线完整（D4）**：helper/engine 4 + resolver/comparator/engine 3 + Processor 12 + StateMachine 2 + job bean 全注册，与 job.yaml invoker（`bean: erpDrpCrossDockStagingTimeoutJob, method: execute`）对应；job cron 键 `erp-inv.drp-xdock-staging-timeout-cron` 为 bean 内层空值跳过门与 yaml cronExpr **同一键**（双层门控设计，javadoc 明示）——非 cron 键漂移家族形态。
- **越库 Job 主干正确**：cron 空值跳过 + xdock 总开关双门 + SCAN_LIMIT 200 + 扫描与回退同 session（MANAGED）+ 幂等键 (DRP_XDOCK_PUTAWAY, code) + 暂存仓不可解析时跳过不盲取消（留人工）+ 嵌套 biz 后独立 session 重载再更新（对齐 reload 先例）。
- **提前期链主干正确（RC-R1.82 交付复审）**：写入侧日期守卫（缺失/倒置抛 ERR_DRP_LT_DATES_INVALID）+ 幂等 existsRecord（purchaseOrderCode+materialId）+ 统计侧防御过滤（actualLeadTime/orderDate/receiptDate 任一空的行不入）+ isOnTime 判定以 earlyLateFlag 非空为已判定标记（DDL 默认 true 不污染口径，注释明示）+ 四维评分缺样维度记 0 并标 missingDimensions 不静默 + resolveFlag 容差带 [expected×(1±tolerance)] 三档实现与 owner doc 一致。
- **联合变分实现与 owner doc 注记一致（RC-R1.82）**：样本 ≥5 时 μ_lt 替换配置提前期、cv≤0.2 标准公式 / >0.2 联合公式 `Z×√(σ_d²×μ_lt + μ_d²×σ_lt²)`、高档额外缓冲显式简化已登记（safety-stock-optimization.md §联合变分集成注记）——代码（SafetyStockEngine L122-137）与文档口径逐项吻合。
- **confirmWriteback 人工门在位**：calculate 全路径不触发回写（无自动写 param 的代码路径），回写必经显式 mutation（safety-stock-optimization.md 反模式「计算结果直接覆写 DRP 参数」未违反）；override 优先语义正确（L207-209）。
- **Z 值映射与量纲换算正确**：95/97.5/99/99.5 → 1.645/1.96/2.326/2.576 与设计表一致；月→日换算 mean÷30、σ÷√30 统计学正确；SS/ROP 均 setScale(2, HALF_UP)。
- **净需求负值钳零与取整正确**：`roundToMultiple` net≤0 → 0；orderMultiple 空/≤0 → lot-for-lot；CEILING 取整到倍数（TestErpDrpEngine L100-101/L122-123 断言在位）。
- **预测消费区间相交与 orgId 过滤正确**：`periodStart <= planEnd AND periodEnd >= planStart`（le/ge 组合）+ 头 orgId 过滤 + 仅仓级行（ne null）——与 README §关键业务规则 1 forecastDemand 来源逐项一致。
- **释放幂等与守卫**：已 ORDERED 重释放抛 ERR_DRP_LINE_ALREADY_ORDERED（TestErpDrpEngine L229-230 断言）；非 APPROVED 释放经 Line Bean 拒绝（ERR_DRP_LINE_NOT_SUGGESTED 误名为 pre-existing 已知，注释登记）；TRANSFER 缺源仓/PURCHASE 缺供应商守卫在位（L73-87）；释放回写 orderBillType/orderBillCode + 行 orgId 透传下游单据在位。

## arm-index 复用 or 新增裁决（汇总）

- **新增** 15 条（arm-index 相关符号 grep 零命中）：`available/allocated 双计`/`DONE 调拨计在途`/`采购在途无仓库过滤`/`resetToDraft 级联偏差`/`approveLine 不回填`（P2-RC-069 家族加重控制点）/`SS 三级链未接入`/`聚合 N+1 全表`/`SS 零月不填充`/`SIM 计划旁路`/`仿真缓存不失效`/`业务 UK 缺失`/`死 config 双键`/`零提前期 SS=0`/`APPROVED 滞留死端`/`LEAD_TIME 变体零消费`/`not-found 误码族`（b2b-012 家族）/`FNPT 零注册`（b2b-011 家族）。
- **同型登记**：P2-CK-drp-012（P1-CK-mfg2-003）、P2-CK-drp-014（P1-CK-pur-003 族）、P2-CK-drp-008（orgId 隔离族读侧形态，独立 finding）。
- **复用（不重复登记，注记）**：P2-RC-069（0 值行 + runBy 零 writer——todo）、P2-RC-070（单价 0/币种任取/all-or-nothing——todo）、P2-RC-071（参数校验缺失——todo）、P2-RC-072（20% 告警——todo）、P1-MA1-022（跨域 daoFor——resolved）。
- **修复在位验证**：P1-RC-081/RC-R1.81（CrossDock 全链）、P1-RC-082/RC-R1.82（提前期 + 联合变分 + confirmWriteback 链）、历史除零修复（monthlyDemands [0] 兜底）。
- **同型核查不成立/不适用**：P1-CK-mfg2-001 与 SafetyStockEngine 无共享安全库存列（无双扣通道；drp 自身的双计形态独立登记 001）；跨引擎预测双补归 mfg 侧 P2-CK-mfg2-004。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 3 | P1-CK-drp-001..003 |
| P2 | 11 | P2-CK-drp-004..014 |
| P3 | 6 | P3-CK-drp-015..020 |

按主维度：D6×8（001、002、003、006、009、012、016、019）、D5×4（005、013、014、018）、D3×3（004、010、017）、D2×2（011、015）、D8×1（008）、D9×1（007）、D10×1（020）。

同型/复用裁决：同型登记 3（012 mfg2-003 族、014 pur-003 族、008 orgId 族读侧）+ arm-index 复用不登记 5 项（RC-069/070/071/072、MA1-022）+ 修复在位验证 3 项（RC-R1.81、RC-R1.82、历史除零）+ 同型核查不适用 1 项（mfg2-001↔SafetyStockEngine 无共享列）。

## 剩余风险（查了什么/没查什么）

- **已查**：erp-drp-service 38 文件全量通读（无抽样）；平台源码实证 2 处（addOrderField desc 语义、FilterBeans ne(null) 先例沿用）；跨域实体字段实证 5 处（StockMoveBookkeeper.recomputeAvailable 的 available=total−reserved−locked、ErpPurOrder.warehouseId 收货仓库列、ErpInvTransferOrderLine 无 received 列、erp-inv/move-status 值域含 DONE、ErpInvStockBalance.orgId 写入）；orm 12 实体 versionProp/UK/索引逐实体核对（ErpDrpParameter 无 UK、ScenarioVersion 有 UK、DrpLine 无 (planId,lineNo) UK、SafetyStockCalc 仅 code UK）；接线核对（beans.xml 22 bean / job.yaml 单键双层门控 / action-auth 手写 0 FNPT vs 生成 44 FNPT / dict 10 个值域全覆盖）；测试断言面抽查（TestErpDrpEngine seedBalance reserved=0 遮蔽实证、TestErpDrpSafetyStock 六用例、TestErpDrpSimulation invalidateCache 仅测试调用）；arm-index drp 条目 7 条 + A4.2.170 运行时探针结论逐条裁决；purchase 侧 Facade 调用点隔离语义核验。
- **未深查**：`erp-drp-web` AMIS/flux 页面契约 drift（归 C8.2，仅抽查 dashboard 净需求页数据源与 dict 消费）；`erp-drp-api` 骨架；`erp-drp-dao` 生成物（问题应回溯模型）；测试代码自身正确性（仅行为交叉验证）；mfg 侧 `SimulationMrpEngine` 是否消费 LEAD_TIME 覆盖（019 的对照面，归 mfg 域已闭合报告范畴）；`ErpPurOrderLine.receivedQuantity` 的 purchase 域维护正确性（跨域，purchase 报告范畴）；H2 对 in-子句超长列表的实际限制（007 的量级论证基于查询次数，未实测阈值）。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-drp-001（available/allocated 双计）**——定性依赖「currentStock 设计语义=在手总量」的推断（README 公式字面 + 经典净需求口径 + `−currentStock + allocatedQty` 仅在 total 语义下自洽）。若产品语义实为「currentStock=可用量且 allocatedQty 是**独立于 reserved 的另一分配维度**」（本项目 reserved 与销售分配是否同源未在 inv 域文档最终确认——`ErpInvReservationBizModel` L397 有 `setAvailableQuantity(ZERO)` 的预留联动证据支持同源），则缺陷降级为「locked 多扣」一项。建议修复阶段用「T=100、R=30 断言 netRequirement」集成测试实证设计真值。
  2. **P1-CK-drp-002（DONE 调拨计在途）**——若 inventory 域调拨完成时**不写**目标仓 balance 而走独立在途仓（`inTransitWarehouseId` 列存在，orm L606），则 DONE 单计入可能部分合理；但 code 侧 `inboundTransferQty` 亦未排除在途仓 balance 的影响，双计结论在任一库存记账口径下均有一侧成立。建议实证一条 DONE 调拨后 runDrp 的 netRequirement 变化。
  3. **P2-CK-drp-006（SS 三级链未接入）**——需求分歧类：若产品裁决「SS 优化仅经 confirmWriteback 单通道生效」（人工门反模式的对偶约束），则 006 降级为「删除 DrpDemandAggregator 失真注释 + owner doc 修订规则 1」的 P3 文档修正。
  4. **P2-CK-drp-010（SIM 计划可直走正式流）**——若产品接受「仿真计划就是普通计划、用户自律不审批 SIM code」，降级 P3；反之需要 ORM 加标志列（dual-agent）。
  5. **P2-CK-drp-012 可达性**——重跑仿真须经 CRUD 直改 scenario.status 回 DRAFT（无命名 reset mutation）；若 UI 不暴露该操作则第 3 次运行不可达，severity 维持 P2（同型 mfg2-003 为 P1 的差异点在此，主 agent 可视 mfg 侧同类可达性裁决统一口径）。
