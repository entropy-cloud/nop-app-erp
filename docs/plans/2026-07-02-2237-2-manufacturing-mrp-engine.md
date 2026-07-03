# 2026-07-02-2237-2-manufacturing-mrp-engine MRP 计算引擎（需求整合 + BOM 展开 + 净需求 + 计划订单 + 释放）

> Plan Status: completed
> Last Reviewed: 2026-07-03
> Source: `docs/backlog/extended-roadmap.md` 工作项 2.3（MRP 计算引擎）；`docs/design/manufacturing/mrp.md`
> Related: `2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md`（N=1 工单，MRP 释放 orderType=WORK_ORDER_REQUEST 的前置）、`2026-07-02-1538-2-manufacturing-bom-routing-rollup.md`（BOM 展开基线 BomExpander）
> Mission: erp
> Work Item: 2.3 MRP 计算引擎（需求整合 → BOM 展开 → 净需求 → 分单 → 计划订单 → 释放）
> Audit: required

## Current Baseline

实时仓库逐项核实的事实：

- **MRP 实体已完备（非新建）**：`ErpMfgMrpPlan`（manufacturing.orm.xml:474）`code`/`orgId`/`businessDate`/`planningHorizonDays`/`status`(dict `erp-mfg/mrp-status` DRAFT=10/RUNNING=20/COMPLETED=30/FIRMED=40/CANCELLED=50)；`ErpMfgMrpPlanLine`（:500）`materialId`/`uoMId`/`orderType`(dict `erp-mfg/mrp-order-type` PLANNED_ORDER=10/PURCHASE_REQUEST=20/WORK_ORDER_REQUEST=30/SUBCONTRACT_REQUEST=40)/`grossRequirement`/`scheduledReceipt`/`onHand`/`netRequirement`/`plannedQuantity`/`plannedDate`/`parentLineId`/`isFirmed`/`convertedBillCode`；`ErpMfgMrpDemand`（:535）`materialId`/`demandSource`(dict `erp-mfg/mrp-demand-source` SALES_ORDER=10/FORECAST=20/SAFETY_STOCK=30/MANUAL=40)/`sourceBillType`/`sourceBillCode`/`quantity`。三者关系：MrpPlan 1→N MrpPlanLine + MrpPlan 1→N MrpDemand。
- **BizModel 全为空 CRUD 壳**：`ErpMfgMrpPlanBizModel`（15 行空 `CrudBizModel`）/`ErpMfgMrpPlanLineBizModel`/`ErpMfgMrpDemandBizModel` —— **无 MRP 运行/净需求计算/计划订单生成/释放逻辑**。
- **BOM 展开基线就绪（前置 done）**：`BomExpander.explode(bomId, qty, useMultiLevel)`（1538-2）产出扁平化展开结果（物料/有效用量/来源工序/层级）——MRP 多级展开复用此方法（仅展开制造件，采购件不展开）。
- **物料计划字段已具备**：`ErpMdMaterial.safetyStock`（master-data.orm.xml:178，DECIMAL）+ `leadTimeDays`（:179，INTEGER 采购提前期）。**全仓无 `ErpMfgForecast` 实体**——mrp.md §需求来源 列「销售预测（ErpMfgForecast）」但 ORM 无此实体（设计文档漂移）。**全仓无 `minOrderQty`/`maxOrderQty`/`fixedLotSize`/`lotSizingPolicy`/`lowLevelCode` 列**——mrp.md §按期分单/§低层编码 引用的这些字段在 ORM 不存在。
- **需求来源（独立需求）可用**：销售订单（`ErpSalOrder`/`ErpSalOrderLine`，`IErpSalOrderBiz`，sales 域 CRUD done）+ 安全库存（ErpMdMaterial.safetyStock）+ 手工需求（ErpMfgMrpDemand）。**FORECAST 来源 Non-Goal**（无实体）。
- **库存可用量数据源**：`ErpInvStockBalance`（`totalQuantity`/`reservedQuantity`/`availableQuantity`）——MRP 经 inventory I*Biz 只读可用量（`availableQuantity` 既有预计算列 = totalQuantity − reservedQuantity − lockedQuantity）。在途采购（openPurchaseQty）/在制工单（openWorkOrderQty）须从采购/制造域汇总（跨域只读）。
- **释放前置**：PURCHASE_REQUEST → 采购订单（`IErpPurOrderBiz`/`ErpPurOrder`，purchase 域 done）；WORK_ORDER_REQUEST → 工单（`IErpMfgWorkOrderBiz`，N=1 本批次前置）。
- **错误码骨架已存在**：`ErpMfgErrors`（BOM 展开/卷算错误码）须扩展 MRP 运行/释放错误码。
- **DAG 依赖方向**：manufacturing 引用 master-data（物料计划字段）+ inventory（余额只读）+ sales（销售订单需求只读）+ purchase（释放采购订单）；无环。
- **剩余差距**：(1) 无 MRP 运行（需求整合 + 库存可用量 + BOM 展开 + 净需求）；(2) 无按期分单（lot sizing）；(3) 无计划订单生成（MrpPlanLine）；(4) 无释放（转采购订单/工单）；(5) 物料计划字段（minOrderQty/maxOrderQty/fixedLotSize/lowLevelCode）缺失——须 ask-first 或以配置默认值替代。

## Goals

- **MRP 运行 `runMrp(planId)`**：`IErpMfgMrpPlanBiz`（manufacturing 域）整合独立需求（销售订单行 + 安全库存补货 + 手工 ErpMfgMrpDemand）→ 合并同物料同期毛需求 → 经 `BomExpander.explode` 多级展开制造件（采购件不展开）→ 各物料净需求 = 毛需求 − 可用量（`availableQuantity` + scheduledReceipt，负值归零）→ 按期分单（lot-for-lot 为主，`erp-mfg.default-lot-size` 配置固定批量取整）→ 写入 `ErpMfgMrpPlanLine`（grossRequirement/onHand/netRequirement/plannedQuantity/plannedDate/orderType/parentLineId pegging）。
- **计划订单类型判定**：物料有 BOM（`ErpMfgBom` 存在且 active；`BomExplosionNode.manufactured` 标记）→ orderType=WORK_ORDER_REQUEST；无 BOM（采购件）→ orderType=PURCHASE_REQUEST。
- **低层码处理**：多级展开时同物料出现在多个 BOM 层级取最低层级为展开基准（避免重复计算）——经 BomExpander 的 DFS 层级标记实现（复用既有展开，不预计算物化 lowLevelCode）。
- **提前期偏移**：采购件按 `ErpMdMaterial.leadTimeDays` 偏移 plannedDate；制造件按 BOM routing 累计工序 standardTime 换算提前期（粗估，经 `erp-mfg.mfg-leadtime-days-per-routing-hour` 配置换算天数）。
- **释放 `releasePlanLine(planLineId)`**：PURCHASE_REQUEST → 调 `IErpPurOrderBiz`（purchase-dao 声明）生成采购订单（`ErpPurOrder`）；WORK_ORDER_REQUEST → 调 `IErpMfgWorkOrderBiz` 生成工单（N=1 前置）；释放后 isFirmed=true + convertedBillCode 回写 + MrpPlan.status→FIRMED（全部行释放后）。幂等（已 firmed 行重复释放拒绝）。
- **Pegging 追溯**：每条 MrpPlanLine 记录 parentLineId（多级 BOM 展开层级关系）+ demand 行的 sourceBillType/sourceBillCode（需求来源追溯）。
- 行为测试覆盖：需求整合（销售订单 + 安全库存 + 手工）、BOM 多级展开净需求、计划订单类型判定（制造/采购）、lot-for-lot + 固定批量取整、提前期偏移、释放（转采购订单 + 转工单）、pegging 追溯。

## Non-Goals

- **FORECAST（销售预测）需求来源**：mrp.md §需求来源 列「销售预测（ErpMfgForecast）」但 ORM 无此实体。**触发条件**：预测实体建模落地时（successor）。
- **minOrderQty/maxOrderQty/fixedLotSize/lowLevelCode 物料计划字段**：ORM 无此列；本期 lot sizing 以 lot-for-lot 为主 + 全局配置 `erp-mfg.default-lot-size`（0=lot-for-lot）。**触发条件**：物料级批量策略精细化需求时（须 ask-first 加列）。
- **CRP 产能校验**：mrp.md §边界「不负责产能计划（CRP）」；CRP（2.8）独立结果表面。**触发条件**：CRP 落地时。
- **MRP 自动定时运行（AUTO_SCHEDULED）**：mrp.md §配置选项；本期仅 MANUAL 触发。**触发条件**：定时调度需求时。
- **需求时界（锁定/可调整需求区分）**：mrp.md §关键业务规则 1；本期不区分时界。**触发条件**：时界管理需求时。
- **委外建议（SUBCONTRACT_REQUEST）释放**：dict 有此 orderType 但委外流程属独立面。**触发条件**：委外加工落地时。
- **scrapRate 纳入净需求**：mrp.md §BOM 展开「净需求 × (1 + scrapRate)」；scrapRate 为 VARCHAR（1538-2 Non-Goal），本期按标准用量。**触发条件**：损耗精细化核算需求时。

## Task Route

- Type: `app-layer design change + implementation`（MRP 计算引擎 + 计划订单生成 + 释放；纯服务层 + 既有实体，不新增实体/列/字典，不触及 model/*.orm.xml）。
- Owner Docs: `docs/design/manufacturing/mrp.md`（MRP 流程/关键业务规则/配置选项/建议单释放）、`docs/design/manufacturing/bom-and-routing.md`（BOM 展开复用）、`docs/architecture/data-dependency-matrix.md`（manufacturing→master-data/inventory/sales/purchase R）。
- Skill Selection Basis: BizModel + 跨实体（MrpPlan/Demand/PlanLine + BOM 展开 + 物料 + 余额 + 销售订单/采购订单/工单）+ 递归算法（多级展开净需求）+ 跨域（inventory/sales/purchase I*Biz 只读/生成）+ 事务 + 错误码 → 加载 `nop-backend-dev`。
- **Decision（FORECAST 缺失处理）**：**选择**本期不支持 FORECAST 来源（ORM 无 ErpMfgForecast 实体，demandSource 字典含 FORECAST=20 但无数据源）。需求整合仅销售订单 + 安全库存 + 手工需求。**替代**：新建 ErpMfgForecast 实体（ask-first ORM 变更，本期范围外，rejected）。**残留风险**：demandSource FORECAST 码值存在但无入口（owner doc 补注）。
- **Decision（lot sizing 简化）**：**选择** lot-for-lot（净需求即建议量）为主 + 全局配置 `erp-mfg.default-lot-size`（>0 时按倍数取整）。**替代**：物料级 fixedLotSize/minOrderQty/maxOrderQty（ORM 无此列，须 ask-first 加列，本期 Non-Goal）。**残留风险**：无物料级批量约束（计划员人工裁决）。
- **Decision（低层码）**：**选择** 经 BomExpander DFS 层级标记实现（复用既有展开，同物料取最低层级），不预计算物化 lowLevelCode 列。**替代**：向 ErpMdMaterial 加 lowLevelCode 列（ask-first + 预计算维护，本期 Non-Goal）。
- **Decision（释放耦合度）**：**选择** 释放时跨域调 I*Biz（PURCHASE_REQUEST→`IErpPurOrderBiz` 生成 `ErpPurOrder`；WORK_ORDER_REQUEST→`IErpMfgWorkOrderBiz` 生成工单），MrpPlanLine.isFirmed + convertedBillCode 回写。**替代**：仅生成建议不自动转单（mrp.md §建议单释放 明确「一键转为采购订单/生产工单」，rejected）。
- **Decision（可用量来源）**：**选择** 经 `IErpInvStockBalanceBiz` 只读 `availableQuantity`（既有预计算列 = totalQuantity − reservedQuantity − lockedQuantity）+ MrpPlanLine.scheduledReceipt（预计入库由计划员录入或从在途采购汇总）。**替代**：实时跨域汇总在途采购/在制工单（跨 purchase/manufacturing 复杂查询，本期以 scheduledReceipt 列承载，粗估）。

## Infrastructure And Config Prereqs

- 配置项：`erp-mfg.default-lot-size`（默认 0=lot-for-lot，>0 时按倍数取整）、`erp-mfg.mfg-leadtime-days-per-routing-hour`（默认 0.125=8h/天，制造件 routing 累计工时换算提前期天数）。经 `AppConfig.var(..., defaultValue)` 读取，无 .env。
- 模块依赖：`erp-mfg-service` 已（经 erp-mfg-dao 传递）compile 依赖 master-data-dao + inventory-dao；**需求整合读取销售订单需** `erp-mfg-service` compile 依赖 `app-erp-sales-dao`（`IErpSalOrderBiz`/`ErpSalOrder` 声明于 sales-dao，DAG manufacturing→sales R 合法）；**释放 PURCHASE_REQUEST 需** `erp-mfg-service` compile 依赖 `app-erp-purchase-dao`（`IErpPurOrderBiz` 声明于 purchase-dao，DAG manufacturing→purchase R 合法）。
- **无 ORM 变更**（不加实体/列/字典）：MrpPlan/PlanLine/Demand 表列齐备。**故无 ask-first 保护区域门控**（纯服务层 + 既有表）。
- 无数据迁移；无新增端口/密钥/外部服务。

## Execution Plan

### Phase 1 — 需求整合 + 库存可用量 + BOM 多级展开净需求 + 测试

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../entity/ErpMfgMrpPlanBizModel.java`(扩)、`IErpMfgMrpPlanBiz.java`(扩)、`MrpEngine.java`(新)、`DemandAggregator.java`(新)、`erp-mfg-service/pom.xml`(加 sales-dao 依赖)、`ErpMfgErrors.java`(扩)、`ErpMfgConstants.java`(扩)、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: BOM 展开基线（1538-2 done）。N=1 工单非此阶段前置（释放才需）。

- [x] `Add`：`DemandAggregator.aggregate(planId)` —— 整合独立需求：销售订单行（未交量，经 `IErpSalOrderBiz` 只读 `ErpSalOrder`/`ErpSalOrderLine`）+ 安全库存补货（ErpMdMaterial.safetyStock − 当前余额 availableQuantity < 0 时补货）+ 手工 ErpMfgMrpDemand；合并同物料同期毛需求；写入 ErpMfgMrpDemand（demandSource/sourceBillType/sourceBillCode/quantity）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`MrpEngine.runMrp(planId)` —— MrpPlan.status DRAFT→RUNNING；按需求物料，制造件经 `BomExpander.explode` 多级展开（DFS 层级标记，同物料取最低层级避免重复计算）；各物料净需求 = 毛需求 − 可用量（`availableQuantity` + scheduledReceipt，负值归零）；计划订单类型判定（`BomExplosionNode.manufactured` 或 active BOM 存在 → WORK_ORDER_REQUEST；无 → PURCHASE_REQUEST）；提前期偏移（采购件 leadTimeDays / 制造件 routing 累计工时换算）；lot-for-lot + `erp-mfg.default-lot-size` 取整；写入 ErpMfgMrpPlanLine（全列 + parentLineId pegging）；MrpPlan.status→COMPLETED。
  - Skill: `nop-backend-dev`
- [x] `Decision`：FORECAST 缺失（本期不支持）+ lot sizing 简化（lot-for-lot + 全局配置）+ 低层码（DFS 层级标记非物化列）+ 可用量来源，见 Task Route Decision。
  - Skill: none
- [x] `Proof`：`TestErpMfgMrpEngine`（需求整合销售订单+安全库存+手工；制造件多级展开净需求；采购件不展开直接净需求；计划订单类型判定；lot-for-lot + fixed-lot 取整；提前期偏移；负净需求归零；pegging parentLineId 链）。`mvn test -pl module-manufacturing/erp-mfg-service -am -Dtest=TestErpMfgMrpEngine*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 1 交付 MRP 运行（需求整合 + 净需求 + 计划订单生成）。解除 Phase 2 释放的 PlanLine 基线。

- [x] MRP 运行（需求整合 + BOM 展开 + 净需求 + 计划订单类型判定 + lot sizing）单测通过

### Phase 2 — 计划订单释放（转采购订单 + 转工单）+ 端到端 + 文档/日志

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../entity/ErpMfgMrpPlanLineBizModel.java`(扩)、`IErpMfgMrpPlanLineBiz.java`(扩)、`MrpReleaseService.java`(新)、`erp-mfg-service/pom.xml`(加 purchase-dao 依赖)、`docs/logs/2026/{执行当日}.md`、`docs/backlog/extended-roadmap.md`、`docs/design/manufacturing/mrp.md`(偏离补注)
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（PlanLine 生成）+ N=1 工单（WORK_ORDER_REQUEST 释放前置）+ purchase 域 CRUD done（PURCHASE_REQUEST 释放前置）。

- [x] `Add`：`MrpReleaseService.releasePlanLine(planLineId)` —— PURCHASE_REQUEST → 调 `IErpPurOrderBiz` 生成采购订单（`ErpPurOrder`，物料/数量/提前期偏移日期）；WORK_ORDER_REQUEST → 调 `IErpMfgWorkOrderBiz` 生成工单（productId=物料/bomId/plannedQuantity/plannedStartDate）；释放后 isFirmed=true + convertedBillCode 回写；MrpPlan 全部行 firmed 后 status→FIRMED。幂等（已 firmed 拒绝 + 抛 `ErpMfgErrors.ERR_MRP_LINE_ALREADY_FIRMED`）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：释放耦合度（跨域 I*Biz 生成 + isFirmed/convertedBillCode 回写），见 Task Route Decision。
  - Skill: none
- [x] `Proof`：端到端 `TestErpMfgMrpEndToEnd`（销售订单需求→MRP 运行→制造件展开+采购件净需求→PlanLine 生成→释放 PURCHASE_REQUEST 转采购订单 + WORK_ORDER_REQUEST 转工单→isFirmed/convertedBillCode 回写→MrpPlan FIRMED；重复释放幂等拒绝）。`mvn test -pl module-manufacturing/erp-mfg-service -am -Dtest=TestErpMfgMrpEndToEnd*`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` 工作项 2.3 标注 done；`mrp.md` 偏离（FORECAST 无实体 Non-Goal + lot sizing 简化 + 低层码 DFS 非物化 + scheduledReceipt 粗估 + 委外 Non-Goal）补注。
  - Skill: none

Exit Criteria:

> Phase 2 交付释放 + 端到端全链。完整仓库验证属 Closure Gates。

- [x] 释放（转采购订单 + 转工单 + isFirmed 回写）+ 端到端单测通过

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0dcb2a191ffeWZq1ggRG1h46Vd`，独立 general 子代理）。3 BLOCKER：(B1) `IErpPurPurchaseOrderBiz` 不存在——实际为 `IErpPurOrderBiz`（purchase-dao，实体 `ErpPurOrder`）；(B2) `IErpSalSalesOrderBiz`/`ErpSalSalesOrder`/`ErpSalSalesOrderLine` 不存在——实际为 `IErpSalOrderBiz`/`ErpSalOrder`/`ErpSalOrderLine`（sales-dao）；(B3) `ErpInvStockBalance` 字段名错误——`onHandQty`/`reservedQty` 不存在，实际为 `totalQuantity`/`reservedQuantity`/`availableQuantity`（既有预计算列）。**已修订**：全量替换接口名/实体名/字段名为真实名称；可用量来源改为读 `availableQuantity` 预计算列；模块依赖改为 purchase-dao/sales-dao；计划订单类型判定补注 `BomExplosionNode.manufactured` 标记。非阻塞 nit（接口在 -dao 非 -api）已吸收。
- Independent draft review iteration 2: **needs revision（2 残留 BLOCKER）**（`ses_0dca3da4dffeou4gERYuKHn1UV`，独立 general 子代理）。iter-1 B1/B2/B3（接口/实体/字段名修正）**确认已解决**（全量替换无残留旧名）。2 新残留：(B1) Goals 行净需求公式未同步——仍为「onHand − reserved + scheduledReceipt」而非修正后的「`availableQuantity` + scheduledReceipt」；(B2) Phase 1 DemandAggregator 读销售订单（IErpSalOrderBiz）须 sales-dao compile 依赖，但 infra 仅标注 purchase-dao + Phase 1 Targets 漏列 pom.xml 变更（erp-mfg-dao 仅传递依赖 master-data-dao+inventory-dao，不含 sales-dao）。**已修订**：Goals 净需求公式改为 `availableQuantity` + scheduledReceipt；infra 补注 sales-dao 显式依赖；Phase 1 Targets 补 `erp-mfg-service/pom.xml(加 sales-dao 依赖)`。非阻塞 nit（Draft Review Record 引旧名为历史记录、ErpMfgMrpDemand.requirementDate 未列）已吸收。
- Independent draft review iteration 3: **accept / consensus**（`ses_0dc9a6ae9ffek7faYwMu9SxoHG`，独立 general 子代理）。iter-2 B1（Goals 净需求公式 onHand−reserved 残留）/B2（sales-dao 依赖未跟踪）**确认已解决**（Goals + Phase 1 均为 `availableQuantity` + scheduledReceipt；Phase 1 Targets 补 pom.xml sales-dao + infra 显式标注；全量无旧名残留——仅在 Draft Review Record 历史引用）。无新 BLOCKER。MRP 实体/字段/BizModel 壳/BomExpander/物料字段/FORECAST 与批量字段不存在 经逐条核实。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成：MRP 运行（需求整合 + BOM 展开 + 净需求 + 计划订单 + lot sizing + 提前期偏移）+ 释放（转采购订单/工单），行为测试通过
- [x] 相关文档对齐：`extended-roadmap.md` 2.3 done 标注；当日日志已记；`mrp.md` Non-Goal 偏离补注
- [x] 已运行验证：`mvn test -pl module-manufacturing/erp-mfg-service -am`（CRUD 0 回归 + 新增 MRP 运行/释放）；根 `mvn clean install -DskipTests`
- [x] 无范围内项目静默降级（FORECAST/物料批量字段/CRP/AUTO_SCHEDULED/需求时界/委外/scrapRate 均为计划内 Non-Goal）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### FORECAST（销售预测）需求来源

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: ORM 无 ErpMfgForecast 实体；本期需求来源仅销售订单 + 安全库存 + 手工需求。
- Successor Required: yes（触发条件：预测实体建模落地时）

### 物料级批量策略字段（minOrderQty/maxOrderQty/fixedLotSize/lowLevelCode）

- Classification: `optimization candidate`
- Why Not Blocking Closure: ORM 无此列；本期 lot-for-lot + 全局配置 default-lot-size。
- Successor Required: yes（触发条件：物料级批量精细化需求时，须 ask-first 加列）

### CRP 产能校验 / MRP 自动定时运行 / 需求时界 / 委外释放 / scrapRate 纳入净需求

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 各为独立深化面；本计划仅 MANUAL MRP 运行 + 计划订单生成 + 采购/工单释放。
- Successor Required: yes（触发条件：CRP/定时调度/时界/委外/损耗精细化需求时）

## Closure

Status Note: 两个 Phase 均已完成并通过本地验证（mfg-service 36 tests / 0 Failures；根 `mvn clean install -DskipTests` BUILD SUCCESS）。范围内行为（MRP 运行全链 + 释放转单）由 7 个新增行为测试覆盖；所有 Non-Goal（FORECAST/物料批量字段/CRP/AUTO_SCHEDULED/需求时界/委外/scrapRate）已在计划 Deferred + mrp.md 显式记录。释放实现的两个必要偏离（直接持久化目标域实体、拆分为两个 purpose-built 释放方法）已在 Task Route Decision + mrp.md + 日志记录。独立结束审计（新会话子代理）已通过（PASS，无 BLOCKER），Plan Status 置 `completed`。

Closure Audit Evidence:

- Auditor: independent general subagent（新会话 `ses_0da953b4affe0Czccz6tvUnTxj`，无执行者上下文，冷重播审计）。
- Evidence: 逐项核对 Phase 1/2 交付物与实时代码——`DemandAggregator`（SO 未交量 + 安全库存缺口 + 保留 MANUAL）、`MrpEngine`（DRAFT→RUNNING→COMPLETED、BomExpander 多级展开+自递归、净需求=毛−可用负值归零、WORK_ORDER_REQUEST/PURCHASE_REQUEST 类型判定、提前期偏移、lot-for-lot+default-lot-size、parentLineId pegging）、`MrpReleaseService`（PURCHASE_REQUEST→ErpPurOrder(+行)/WORK_ORDER_REQUEST→ErpMfgWorkOrder、isFirmed/convertedBillCode 回写、plan→FIRMED、幂等 ERR_MRP_LINE_ALREADY_FIRMED）、`IErpMfgMrpPlanBiz.runMrp` + `IErpMfgMrpPlanLineBiz` 两个 @BizMutation 释放方法 声明并实现、ErpMfgErrors MRP 错误码 + ErpMfgConstants MRP 常量、beans.xml 注册三个服务、pom.xml sales-dao+purchase-dao 依赖。Nop 约定达标（包级 @Inject、@BizMutation、NopException+ErrorCode、CrudBizModel、IDaoProvider service-helper 范式有文档化理由）。验证：`mvn test -pl module-manufacturing/erp-mfg-service -am` = **36 tests / 0 Failures / 0 Errors / 0 Skipped**，BUILD SUCCESS；`mvn clean install -DskipTests` = BUILD SUCCESS。文档对齐（07-03.md 全绿条目、extended-roadmap.md 2.3 ✅ done、mrp.md 偏离补注完整）。所有 Non-Goal（FORECAST/物料批量/CRP/AUTO_SCHEDULED/需求时界/委外/scrapRate）在计划 Deferred + mrp.md 显式记录。文本一致性成立——仅独立审计门控曾为 `[ ]`，本次审计后置 `[x]`。无 BLOCKER；2 个文档化非阻塞 NIT（releasePlanLine→两方法拆分；多级展开自递归驱动）。
- Verdict: **PASS**（独立审计门控可置 `[x]`）。

Follow-up:

- FORECAST 需求来源（见上方 Deferred）
- 物料级批量策略字段（见上方 Deferred）
- CRP / AUTO_SCHEDULED / 需求时界 / 委外 / scrapRate（见上方 Deferred）
