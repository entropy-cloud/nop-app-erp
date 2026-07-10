# 2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine WorkOrder/JobCard 状态机 + 齐套校验 + 领料/报工/完工

> Plan Status: completed
> Last Reviewed: 2026-07-03
> Source: `docs/backlog/extended-roadmap.md` 工作项 2.2（WorkOrder/JobCard 状态机 + 审批）；`docs/plans/2026-07-02-1538-2-manufacturing-bom-routing-rollup.md` Deferred「WorkOrder/JobCard 状态机」（Successor Required: yes，触发条件：BOM 展开 + 成本基线就绪后）；`docs/design/manufacturing/state-machine.md`
> Related: `2026-07-02-1538-2-manufacturing-bom-routing-rollup.md`（BOM 展开 + 卷算基线，本计划 successor）、`2026-07-02-1538-1-inventory-costing-engine.md`（成本引擎基线）、`2026-07-01-0811-2-inventory-stockmove-bizmodel.md`（StockMove generateMove 契约）、`2026-07-02-2237-2-manufacturing-mrp-engine.md`（N=2，MRP 释放工单的后继）
> Mission: erp
> Work Item: 2.2 WorkOrder/JobCard 状态机 + 审批（齐套校验 + 领料/报工/完工 + 成本归集）
> Audit: required

## Current Baseline

实时仓库逐项核实的事实：

- **工单实体已完备（非新建）**：`ErpMfgWorkOrder`（manufacturing.orm.xml:348）字段齐全——`code`/`bomId`/`routingId`/`productId`/`plannedQuantity`/`completedQuantity`/`scrappedQuantity`/日期/成本列（`materialCost`/`laborCost`/`overheadCost`/`subcontractCost`/`totalCost`/`unitCost`）/`docStatus`(dict `erp-mfg/work-order-status` 10 态 DRAFT=10…CANCELLED=100)/`approveStatus`(dict `erp-mfg/approve-status`)/`posted`/`postedAt`/`postedBy`/`sourceMrpPlanId`。`ErpMfgWorkOrderLine`（:408）：`lineType`(OUTPUT/INPUT/BYPRODUCT)/`materialId`/`skuId`/`plannedQuantity`/`actualQuantity`/`scrappedQuantity`/`sourceWarehouseId`/`destWarehouseId`。
- **作业卡 + 工时记录实体已完备**：`ErpMfgJobCard`（:772）`workOrderId`/`operationId`/`workcenterId`/`plannedQuantity`/`completedQuantity`/`scrappedQuantity`/`status`(dict `erp-mfg/job-card-status` 8 态 OPEN=10…CANCELLED=80)。`ErpMfgJobCardTimeLog`（:804）`jobCardId`/`workOrderId`/`operatorId`/`workDate`/`completedQuantity`/`scrappedQuantity`/`laborCost`(DECIMAL)。
- **领料单实体已存在**：`ErpMfgMaterialIssue`（:564）+ `ErpMfgMaterialIssueLine`（:603）——领料单头/行已 codegen，头有 `workOrderId`/`status`(dict `erp-mfg/issue-status` DRAFT/CONFIRMED/DONE/CANCELLED)，行有 `materialId`/`requiredQuantity`(应领)/`issuedQuantity`(实领)/`workOrderLineId`/`batchNo`/`locationId`/`unitCost`/`totalCost`。
- **工时记录 + 领料行存在类型缺陷（契约漂移，阻塞成本归集，承接 1538-2 deferred）**：(1) `ErpMfgJobCardTimeLog` 的 `durationMins`（:815）/`setupMins`（:816）/`runMins`（:817）列类型为 **VARCHAR**，domain `timeInMins` 为 DECIMAL；`hourlyRate`（:820）列类型 **VARCHAR**，domain `hourlyRate` 为 DECIMAL。**与 1538-2 已修正的 `ErpMfgBomOperation.standardTime`/`ErpMfgWorkcenter.hourlyRate` 同类缺陷**；1538-2 结束审计明确将其归为 2.2 Non-Goal。报工成本归集须数值计算，列类型缺陷阻塞。(2) `ErpMfgMaterialIssueLine.issuedQuantity`（:615）列类型为 **BOOLEAN**（`stdDataType="boolean"`），domain `quantity` 为 DECIMAL——实领数量存为布尔，领料出库 + 材料成本归集须数值，同类列类型与 domain 矛盾缺陷。
- **BizModel 全为空 CRUD 壳**：`ErpMfgWorkOrderBizModel`（:11，15 行空 `CrudBizModel`）/`ErpMfgJobCardBizModel`/`ErpMfgJobCardTimeLogBizModel`/`ErpMfgMaterialIssueBizModel` —— **无状态机迁移、无审批、无齐套校验、无领料/报工/完工逻辑**。
- **BOM 展开 + 成本引擎基线就绪（前置 done）**：`BomExpander.explode(bomId, qty, useMultiLevel)`（1538-2）产出展开结果（物料/有效用量），供齐套校验读取。`StockMoveBookkeeper`（1538-1）已按物料 costMethod 分派成本，制造入库/领料移动单经记账器写成本。
- **库存移动生成契约已确立**：`IErpInvStockMoveBiz.generateMove(StockMoveRequest, ctx)`（inv-dao）——领料生成出库移动单（`moveType=MANUFACTURE`/出库方向、业务联动 `relatedBillType` 自动 DRAFT→DONE），完工生成入库移动单。`StockMoveRequest` 含 `moveType`/仓库/`lines`/`relatedBillType`+`relatedBillCode`。`ErpInvConstants.MOVE_TYPE_MANUFACTURING=40` 既有。
- **BOM 完工质检为软依赖（config-gated 钩子）**：`ErpMfgBom` 有 `inspectionRequired`（完工是否触发质检）字段。`state-machine.md §质检对工单状态的约束声明` 引用工单 INSPECTING 态，但 **`erp-mfg/work-order-status` 10 态无 INSPECTING**（设计文档漂移——约束声明引用了字典中不存在的状态）。完整质检/NCR/CAPA 流程属工作项 2.4（本批次 N=3）。本计划以 config-gated 钩子处理（详见 Decision）。
- **错误码骨架已存在**：`ErpMfgErrors`（BOM 展开/卷算错误码）须扩展工单状态机/齐套/报工错误码。
- **DAG 依赖方向**：manufacturing 引用 master-data（物料/SKU/仓库/工作中心/职员）+ inventory（移动单生成/余额只读）；不依赖 finance（完工成本结转凭证属后续 owner plan）。无环。
- **剩余差距**：(1) 无工单状态机（10 态迁移 + 审批三轴）；(2) 无齐套校验（BOM 展开 × 余额可用量）；(3) 无领料（领料单确认 → 出库移动单）；(4) 无报工（JobCard 工时录入 + 成本归集）；(5) 无完工入库（入库移动单 + 成本回写）；(6) 工时记录 VARCHAR 类型缺陷阻塞数值成本归集。

## Goals

- **ORM ask-first 类型修正（Fix 契约漂移，承接 1538-2 deferred）**：修正 `ErpMfgJobCardTimeLog.durationMins`/`setupMins`/`runMins`/`hourlyRate`（VARCHAR→DECIMAL，对齐 `timeInMins`/`hourlyRate` domain）+ `ErpMfgMaterialIssueLine.issuedQuantity`（BOOLEAN→DECIMAL，对齐 `quantity` domain）。仅改列类型对齐既有 DECIMAL domain，不加实体/列/字典。重新 codegen 增量。
- **工单状态机（10 态迁移 + 审批三轴）**：`IErpMfgWorkOrderBiz`（manufacturing 域）实现 submit/approve（三轴：提交→审核→NOT_STARTED）/checkAvailability（齐套校验）/start（开工）/reportCompletion（完工达量→COMPLETED）/stop/resume/close/cancel。迁移遵循 `state-machine.md §适用对象一`（10 态 + 迁移完整性表）。非法迁移抛 `ErpMfgErrors`。
- **齐套校验**：`checkAvailability(workOrderId)` —— 经 `BomExpander.explode` 展开工单 BOM 子件需求 × plannedQuantity，对照 inventory 余额可用量（onHand − reserved）；全齐 → STOCK_RESERVED；部分 → STOCK_PARTIAL（记录缺料明细）。`erp-mfg.allow-partial-kit-start`（默认 false）控制 STOCK_PARTIAL 是否允许强制开工。
- **领料**：`ErpMfgMaterialIssueBizModel` 确认领料单（issue-status CONFIRMED→DONE）时调 `IErpInvStockMoveBiz.generateMove`（出库方向、moveType=MANUFACTURE、relatedBillType=ERP_MFG_ISSUE），扣减库存；回写 WorkOrderLine.actualQuantity。
- **报工 + 成本归集**：`ErpMfgJobCardBizModel.recordWork(JobCardTimeLog)` —— JobCard WORK_IN_PROGRESS/SUBMITTED 态录入工时（durationMins/setupMins/runMins 类型修正后数值）；人工成本 = Σ(durationMins/60 × hourlyRate) → JobCardTimeLog.laborCost + WorkOrder.laborCost/overheadCost 累加。JobCard 8 态迁移（OPEN→WORK_IN_PROGRESS→…→COMPLETED/CANCELLED）。
- **完工入库**：`reportCompletion(workOrderId, completedQty)` —— 生成入库移动单（产成品入 destWarehouse，moveType=MANUFACTURE）；回写 WorkOrder.completedQuantity；completedQuantity ≥ plannedQuantity → COMPLETED。材料成本由领料出库移动单的 ledger.totalCost 汇总回写 WorkOrder.materialCost。
- **完工质检 config-gated 钩子**：reportCompletion 时若 `ErpMfgBom.inspectionRequired=true` 且 `erp-mfg.inspection-gate-enabled=true`，拒绝 COMPLETED 并提示等待质检结果（N=3 quality 落地后接线）；`erp-mfg.inspection-gate-enabled` 默认 false（quality 2.4 未落地前跳过）。
- 行为测试覆盖：工单状态机主路径（DRAFT→…→COMPLETED）+ 异常路径（STOCK_PARTIAL/STOPPED/CLOSED/CANCELLED）+ 齐套校验（全齐/部分）+ 领料出库 + 报工成本归集 + 完工入库 + 类型修正后工时数值计算。

## Non-Goals

- **完整质检/NCR/CAPA 流程（工作项 2.4，本批次 N=3）**：`state-machine.md §质检对工单状态的约束声明`；本计划仅 config-gated 钩子（完工门控），完整质检单生成 + NCR + CAPA 闭环属 2.4。**触发条件**：2.4 quality 落地后接线。
- **完工成本结转凭证（产成品存货估值过账）**：`state-machine.md §外部依赖`「成本结转凭证：财务域监听工单完工」；依赖 finance 域制造过账 Provider（MANUFACTURING_RECEIPT 凭证类型）。**触发条件**：制造业财一体过账 Provider 落地时（独立 owner plan successor）。
- **APS 排产集成**：`state-machine.md §三层分解`；JobCard 按 OperationOrder 排程结果创建属 APS（3.10）。**触发条件**：APS 模块落地时。
- **BOM 版本快照（工单创建时锁定 BOM/工艺版本）**：`1538-2` Deferred；已开工工单不追溯 BOM 变更。**触发条件**：BOM 变更管理需求时。
- **联副产品分摊完工入库**：`ErpMfgWorkOrderLine.lineType=BYPRODUCT` 存在但分摊成本入库 Non-Goal。**触发条件**：联副产品生产场景时。
- **返工工单（完工质检不合格→新建返工工单关联原工单）**：依赖 2.4 quality。**触发条件**：质检返工流程落地时。
- **超产配置（报工超过工单数量）**：`state-machine.md §异常路径`。**触发条件**：超产业务需求时。
- **work-order-status 增加 INSPECTING 态**：设计文档 §质检约束声明引用但字典无此态；本计划以 config-gated 钩子替代（不加 ORM 字典态）。**触发条件**：2.4 质检落地后裁决是否需独立态。

## Task Route

- Type: `app-layer design change + implementation`（工单/作业卡状态机 + 齐套校验 + 领料/报工/完工 + 成本归集 + **ask-first ORM 类型修正**——JobCardTimeLog 工时/费率 VARCHAR→DECIMAL 对齐既有 domain；服务层为主）。
- Owner Docs: `docs/design/manufacturing/state-machine.md`（WorkOrder 10 态 + JobCard 8 态 + 迁移完整性 + 异常路径 + 质检约束声明）、`docs/design/manufacturing/bom-and-routing.md`（齐套校验引用 BOM 展开）、`docs/design/inventory/cross-domain.md`（领料/完工移动单生成契约 + 余量校验规则）、`docs/architecture/data-dependency-matrix.md`（manufacturing→master-data + manufacturing→inventory R）。
- Skill Selection Basis: BizModel + 跨实体（工单/行/作业卡/工时/领料单/移动单/余额）+ 状态机 + 跨域（inventory I*Biz 移动单生成/余额只读）+ 事务边界 + 错误码 → 加载 `nop-backend-dev`。
- **Decision（完工质检处理）**：**选择** config-gated 钩子（`erp-mfg.inspection-gate-enabled` 默认 false）。reportCompletion 时若 BOM.inspectionRequired=true 且 gate 开启，拒绝 COMPLETED 待质检结果。**替代**：① 向 work-order-status 加 INSPECTING 态（ORM 字典变更 + 状态机复杂化，且 INSPECTING 语义属 quality 域，rejected 作本期）；② 不做任何质检门控（与 `inspectionRequired` 字段矛盾，rejected）。**残留风险**：gate=false 时完工不等待质检（与设计文档 §质检约束声明 有偏差，已记入 Non-Goal + owner doc 补注；2.4 落地后 flip config）。
- **Decision（领料→移动单耦合）**：**选择** `ErpMfgMaterialIssue` 确认 DONE 时调 `IErpInvStockMoveBiz.generateMove`（业务联动 relatedBillType=ERP_MFG_ISSUE，自动 DRAFT→DONE），manufacturing→inventory R 合法。**替代**：工单直接生成出库移动单（绕过领料单实体，丢失领料单审计/审批，rejected）。**残留风险**：领料单与移动单经 relatedBillType 幂等键关联，重复确认幂等返回。
- **Decision（成本归集通道）**：**选择** 材料成本 = 领料出库移动单 `ErpInvStockLedger.totalCost` 汇总（经既有 COGS 通道，costing 引擎已计算）；人工/制造费用 = JobCardTimeLog(durationMins/60 × hourlyRate) 累加。回写 WorkOrder.materialCost/laborCost/overheadCost/totalCost/unitCost。**替代**：完工时一次性按 BOM 卷算标准成本（实际 vs 标准差异须 cost variance 实体，Non-Goal）。**残留风险**：材料成本依赖领料已 DONE（未领料的工单 materialCost=0，符合实际）。
- **Decision（齐套校验数据源）**：**选择** 经 `IErpInvStockBalanceBiz`（或 dao 只读）读余额 onHand/reserved，BOM 展开子件需求 × plannedQuantity 对照。**替代**：实时查 StockLedger 聚合（性能差，rejected）。
- **Decision（工时类型缺陷处理）**：**选择** ask-first 修正列类型 VARCHAR→DECIMAL 对齐 domain（同 1538-2 范式；pre-production 无数据故安全）；同时修正 `ErpMfgMaterialIssueLine.issuedQuantity` BOOLEAN→DECIMAL（同类列类型与 domain 矛盾，领料实领数量须数值）。**替代**：运行期解析 VARCHAR/BOOLEAN 为数值（脆弱、语义错误，rejected）。

## Infrastructure And Config Prereqs

- 配置项：`erp-mfg.allow-partial-kit-start`（默认 false，STOCK_PARTIAL 是否允许强制开工）、`erp-mfg.inspection-gate-enabled`（默认 false，完工质检门控；2.4 质检落地前 false）。经 `AppConfig.var(..., defaultValue)` 读取，无 .env。
- 模块依赖：`erp-mfg-service` 已 compile 依赖 master-data-dao（物料/SKU/仓库/工作中心/职员）+ inventory-dao（IErpInvStockMoveBiz/IErpInvStockBalanceBiz 接口声明层）；无新增模块依赖方向。
- **保护区域门控**：修正 `ErpMfgJobCardTimeLog.durationMins`/`setupMins`/`runMins`/`hourlyRate` + `ErpMfgMaterialIssueLine.issuedQuantity` 列类型触及 `module-manufacturing/model/app-erp-manufacturing.orm.xml`（ask-first）。Phase 1 实施前须：人工批准 + 本计划草案审查通过。重新 codegen 增量。pre-production 无数据，类型修正安全。
- 无数据迁移（改列类型，无既有数据）；无新增端口/密钥/外部服务。

## Execution Plan

### Phase 1 — ORM ask-first 类型修正（JobCardTimeLog 工时/费率 + MaterialIssueLine 实领数量 VARCHAR/BOOLEAN→DECIMAL）+ codegen + 回归

Status: completed
Targets: `module-manufacturing/model/app-erp-manufacturing.orm.xml`（durationMins/setupMins/runMins/hourlyRate + issuedQuantity 列类型 DECIMAL）、codegen 增量
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof`
- Prereqs: **人工批准**（model/*.orm.xml ask-first，列类型修正）+ 本计划草案审查通过。

- [x] `Fix`：修正列类型对齐 DECIMAL domain——`ErpMfgJobCardTimeLog.durationMins`（VARCHAR→DECIMAL timeInMins）；`setupMins`（VARCHAR→DECIMAL timeInMins）；`runMins`（VARCHAR→DECIMAL timeInMins）；`hourlyRate`（VARCHAR→DECIMAL hourlyRate）；`ErpMfgMaterialIssueLine.issuedQuantity`（BOOLEAN→DECIMAL quantity）。重新 codegen 生成实体/列/_app.orm.xml；CRUD 快照若漂移则 `force-save-output` 重录。
  - Skill: none
- [x] `Proof`：`mvn clean install -DskipTests -pl module-manufacturing -am` = BUILD SUCCESS（类型修正无回归）；manufacturing CRUD 套件无回归。
  - Skill: none

Exit Criteria:

> Phase 1 交付工时/费率列类型修正（可数值计算）。解除 Phase 4 报工成本归集的输入基线。

- [x] durationMins/setupMins/runMins/hourlyRate + issuedQuantity 列类型修正 + codegen 增量通过

### Phase 2 — 工单状态机（10 态迁移 + 审批三轴）+ 齐套校验 + 测试

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../entity/ErpMfgWorkOrderBizModel.java`(扩)、`IErpMfgWorkOrderBiz.java`(扩)、`WorkOrderStateMachine.java`(新)、`KitAvailabilityChecker.java`(新)、`ErpMfgErrors.java`(扩)、`ErpMfgConstants.java`(扩)、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（类型修正 codegen 通过）。

- [x] `Add`：`IErpMfgWorkOrderBiz` 状态机迁移方法——`submit`（DRAFT→SUBMITTED）、`approve`（SUBMITTED→NOT_STARTED，三轴审批审核轴）、`checkAvailability`（NOT_STARTED→STOCK_RESERVED/STOCK_PARTIAL）、`start`（STOCK_RESERVED/STOCK_PARTIAL→IN_PROCESS）、`stop`（IN_PROCESS→STOPPED）、`resume`（STOPPED→IN_PROCESS）、`close`（STOPPED/IN_PROCESS→CLOSED）、`cancel`（DRAFT/NOT_STARTED/SUBMITTED→CANCELLED，释放预留）。非法迁移抛 `ErpMfgErrors.ERR_INVALID_STATUS_TRANSITION`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`KitAvailabilityChecker.check(workOrderId)` —— 经 `BomExpander.explode`（1538-2）展开工单 BOM 子件 × plannedQuantity；经 `IErpInvStockBalanceBiz` 读 onHand/reserved；全齐 → STOCK_RESERVED；部分 → STOCK_PARTIAL（缺料明细返回）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：状态机迁移实现（显式状态守卫 + 非法迁移错误码）+ 齐套校验数据源（IErpInvStockBalanceBiz），见 Task Route Decision。
  - Skill: none
- [x] `Proof`：`TestErpMfgWorkOrderStateMachine`（主路径 DRAFT→SUBMITTED→NOT_STARTED→STOCK_RESERVED→IN_PROCESS→COMPLETED；齐套全齐/部分；STOPPED/resume/CLOSED；CANCELLED 释放预留；非法迁移抛错）。`mvn test -pl module-manufacturing/erp-mfg-service -am -Dtest=TestErpMfgWorkOrderStateMachine*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 2 交付工单状态机 + 齐套校验。解除 Phase 3 领料/Phase 4 报工完工的状态机基线。

- [x] 工单 10 态迁移（含审批 + 齐套）单测通过

### Phase 3 — 领料（MaterialIssue 确认 → 出库移动单 + 材料成本回写）+ 测试

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../entity/ErpMfgMaterialIssueBizModel.java`(扩)、`IErpMfgMaterialIssueBiz.java`(扩)、`MaterialIssueProcessor.java`(新)、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 2（工单 IN_PROCESS 状态机）。

- [x] `Add`：`ErpMfgMaterialIssueBizModel.confirm(issueId)` —— issue-status DRAFT→CONFIRMED→DONE；DONE 时调 `IErpInvStockMoveBiz.generateMove`（出库方向、moveType=MANUFACTURE、relatedBillType=ERP_MFG_ISSUE、lines 取领料行物料/数量/仓库），业务联动自动 DRAFT→DONE 扣减库存；回写 WorkOrderLine.actualQuantity；汇总领料出库 ledger.totalCost → WorkOrder.materialCost。
  - Skill: `nop-backend-dev`
- [x] `Decision`：领料→移动单耦合（MaterialIssue DONE 调 generateMove，relatedBillType 幂等），见 Task Route Decision。
  - Skill: none
- [x] `Proof`：`TestErpMfgMaterialIssue`（领料确认→出库移动单 DONE→余额扣减→WorkOrderLine.actualQuantity 回写→WorkOrder.materialCost 汇总；重复确认幂等）。`mvn test -pl module-manufacturing/erp-mfg-service -am -Dtest=TestErpMfgMaterialIssue*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 3 交付领料出库 + 材料成本回写。解除 Phase 4 完工的材料成本基线。

- [x] 领料确认→出库移动单 + 材料成本汇总单测通过

### Phase 4 — 报工 + 完工入库 + 成本归集 + 质检钩子 + 端到端 + 文档/日志

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../entity/ErpMfgJobCardBizModel.java`(扩)、`IErpMfgJobCardBiz.java`(扩)、`ErpMfgWorkOrderBizModel.java`(扩 reportCompletion)、`WorkOrderCompletionService.java`(新)、`ErpMfgConfigs.java`(扩)、`docs/logs/2026/{执行当日}.md`、`docs/backlog/extended-roadmap.md`、`docs/design/manufacturing/state-machine.md`(偏离补注)
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（工时类型修正）+ Phase 2（状态机）+ Phase 3（领料材料成本）。

- [x] `Add`：`ErpMfgJobCardBizModel` JobCard 8 态迁移——`startJob`（OPEN→WORK_IN_PROGRESS）、`recordWork`（录入 JobCardTimeLog：durationMins/60 × hourlyRate → laborCost，回写 WorkOrder.laborCost/overheadCost 累加）、`submitJob`（→SUBMITTED）、`completeJob`（→COMPLETED，回写 JobCard.completedQuantity）、`cancelJob`（→CANCELLED）、`hold`/`resume`（ON_HOLD）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpMfgWorkOrderBizModel.reportCompletion(workOrderId, completedQty)` —— 生成入库移动单（产成品入 destWarehouse，moveType=MANUFACTURE，relatedBillType=ERP_MFG_WORK_ORDER）；回写 WorkOrder.completedQuantity；completedQuantity ≥ plannedQuantity → COMPLETED（经完工质检 config-gated 钩子：inspectionRequired=true 且 inspection-gate-enabled=true 时拒绝待质检）。totalCost = materialCost + laborCost + overheadCost + subcontractCost；unitCost = completedQuantity > 0 ? totalCost / completedQuantity : 0。
  - Skill: `nop-backend-dev`
- [x] `Decision`：完工质检 config-gated 钩子（不加 INSPECTING 态）+ 成本归集通道（材料 from ledger.totalCost / 人工 from JobCardTimeLog），见 Task Route Decision。
  - Skill: none
- [x] `Proof`：端到端 `TestErpMfgWorkOrderEndToEnd`（工单创建→审批→齐套→开工→领料出库→报工成本归集→完工入库→COMPLETED + WorkOrder.materialCost/laborCost/overheadCost/totalCost/unitCost 正确；完工质检 gate=false 跳过 / gate=true + inspectionRequired 拒绝待质检）。`mvn test -pl module-manufacturing/erp-mfg-service -am -Dtest=TestErpMfgWorkOrderEndToEnd*`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` 工作项 2.2 标注 done；`state-machine.md` 偏离（INSPECTING 态字典缺失→config-gated 钩子替代 + 质检软依赖 2.4 + 完工成本结转凭证 Non-Goal + 工时类型修正补注）补注。
  - Skill: none

Exit Criteria:

> Phase 4 交付报工 + 完工入库 + 成本归集 + 质检钩子 + 端到端。完整仓库验证属 Closure Gates。

- [x] 报工成本归集 + 完工入库 + COMPLETED + 端到端单测通过

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0dcb2d78effe6wEl5bVvSnpgNb`，独立 general 子代理）。1 BLOCKER：(B1) `ErpMfgMaterialIssueLine.issuedQuantity`（manufacturing.orm.xml:615）domain=quantity(DECIMAL) 但 stdSqlType=BOOLEAN/stdDataType=boolean——同类列类型与 domain 矛盾缺陷，位于领料执行路径（Phase 3 实领数量回写），原计划 Phase 1 类型修正集漏列。**已修订**：Phase 1 类型修正集 + baseline + Goals + Decision + 保护区域门控 + Closure Gates 全部追加 issuedQuantity BOOLEAN→DECIMAL。非阻塞 nit（cancel 含 DRAFT→CANCELLED、unitCost 除零守卫）已吸收。
- Independent draft review iteration 2: **accept / consensus**（`ses_0dca407e1ffeTAVD3Fcf54dnIs`，独立 general 子代理）。iter-1 B1（issuedQuantity BOOLEAN→DECIMAL 修正集）**确认已解决**（核实 manufacturing.orm.xml:615 BOOLEAN/stdDataType=boolean + domain=quantity DECIMAL；修正集在 baseline/Goals/Decision/保护区域/Phase 1/Closure Gates 全部一致落地）。无新 BLOCKER。全部实体/字段/字典/BizModel 壳/跨域契约经逐条核实（WorkOrder/WorkOrderLine/JobCard/JobCardTimeLog/MaterialIssue(MaterialIssueLine) + work-order-status 10 态无 INSPECTING + job-card-status 8 态 + issuedQuantity BOOLEAN 缺陷 + BomExpander/generateMove/MOVE_TYPE_MANUFACTURING=40 + 空壳 BizModel）。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成：工单 10 态状态机 + 审批 + 齐套校验 + 领料出库 + 报工成本归集 + 完工入库 + 质检 config-gated 钩子，行为测试通过
- [x] 相关文档对齐：`extended-roadmap.md` 2.2 done 标注；当日日志已记；`state-machine.md` Non-Goal 偏离补注
- [x] 已运行验证：`mvn test -pl module-manufacturing/erp-mfg-service -am`（CRUD 0 回归 + 新增工单/领料/报工/完工）；根 `mvn clean install -DskipTests`
- [x] 无范围内项目静默降级（质检 NCR/CAPA/完工成本凭证/APS/BOM 快照/联副/返工/超产/INSPECTING 态 均为计划内 Non-Goal）
- [x] 保护区域（durationMins/setupMins/runMins/hourlyRate + issuedQuantity 列类型修正）实施前已获人工批准
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 完整质检/NCR/CAPA 流程（工作项 2.4）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅 config-gated 完工门控钩子；完整质检单生成 + NCR + CAPA 闭环属 2.4（本批次 N=3）。
- Successor Required: yes（触发条件：2.4 quality 落地后接线 inspection-gate-enabled=true）

### 完工成本结转凭证（产成品存货估值过账）

- Classification: `out-of-scope improvement` → **已由 successor plan `2026-07-10-1100-5` 落地，Deferred 解除**
- Why Not Blocking Closure: 依赖 finance 域制造过账 Provider（MANUFACTURING_RECEIPT 凭证类型）；属制造业财一体面。
- Successor Required: yes（触发条件：制造业财一体过账 Provider 落地时）→ **已满足（plan 1100-5 completed）**

### APS 排产集成 / BOM 版本快照 / 联副产品分摊 / 返工工单 / 超产配置 / work-order-status INSPECTING 态

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 各为独立深化面；本计划仅工单执行生命周期（状态机 + 齐套 + 领料 + 报工 + 完工）。
- Successor Required: yes（触发条件：APS/BOM 变更管理/联副/质检返工/超产/质检态裁决需求时）

## Closure

Status Note: 全部 4 Phase 已执行完成（2026-07-03）。工单 10 态状态机 + 三轴审批 + 齐套校验（BOM 展开 × plannedQuantity 对照余额可用量）+ 领料出库移动单（OUTGOING，材料成本经 ledger.totalCost 汇总）+ 报工成本归集（JobCardTimeLog durationMins/60 × hourlyRate → laborCost）+ 完工入库移动单（MANUFACTURING）+ 完工质检 config-gated 钩子（默认 false）全部落地。含 ask-first ORM 类型修正（JobCardTimeLog 工时/费率 VARCHAR→DECIMAL、MaterialIssueLine 实领数量 BOOLEAN→DECIMAL）+ codegen 增量。验证全绿：`mvn test -pl module-manufacturing/erp-mfg-service -am -Dtest=TestErpMfg*` = 29 tests / 0 Failures（新增 13，既有 16 无回归）；根 `mvn clean install -DskipTests` = BUILD SUCCESS（146 reactor 模块）。实现偏离（领料出库 moveType=OUTGOING、INSPECTING 态字典缺失→config-gated 钩子、齐套只读不写预留、overheadCost=0、完工成本结转凭证 Non-Goal）已补注 `state-machine.md §实现偏离补注` + `docs/logs/2026/07-03.md`。独立结束审计已于 2026-07-03 由独立子代理（新会话，不重用执行者上下文）执行并通过。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure auditor 子代理（新会话，独立 general agent，不重用执行者上下文）。审计日期 2026-07-03。
- Audit Scope: 对全部 4 Phase 退出标准 + Closure Gates + Anti-Hollow + 五点一致性 + Deferred 诚实性 + 文档同步进行独立验证（非自我审计）。
- Evidence — Phase 1（ORM 类型修正）: `module-manufacturing/model/app-erp-manufacturing.orm.xml` 逐列核实——`issuedQuantity`(:615)/`durationMins`(:815)/`setupMins`(:816)/`runMins`(:817)/`hourlyRate`(:820) 均为 `stdSqlType="DECIMAL"` + 对应 domain（quantity/timeInMins/hourlyRate），VARCHAR/BOOLEAN 缺陷已消除。
- Evidence — Phase 2（工单状态机 + 齐套）: `ErpMfgWorkOrderBizModel.java`（351 行）非空壳——10 态迁移 submit/approve/checkAvailability/start/stop/resume/close/cancel 均 `@BizMutation` + 显式 `requireStatus` 守卫 + 非法迁移抛 `ERR_INVALID_STATUS_TRANSITION`；`KitAvailabilityChecker`（148 行）经 `BomExpander.explode` × plannedQuantity 对照 `ErpInvStockBalance.availableQuantity`，全齐→STOCK_RESERVED/部分→STOCK_PARTIAL；`TestErpMfgWorkOrderStateMachine`（309 行）落地。
- Evidence — Phase 3（领料出库 + 材料成本）: `ErpMfgMaterialIssueBizModel.java`（193 行）非空壳——confirm DRAFT→CONFIRMED→DONE 调 `IErpInvStockMoveBiz.generateMove`（出库 OUTGOING、幂等 DONE 空操作）、回写 `WorkOrderLine.actualQuantity`、汇总 `ErpInvStockLedger.totalCost`(abs) → `WorkOrder.materialCost`；`TestErpMfgMaterialIssue`（295 行）落地。
- Evidence — Phase 4（报工 + 完工 + 成本归集 + 质检钩子）: `ErpMfgJobCardBizModel.java`（201 行）作业卡 8 态 + recordWork（`durationMins/60 × hourlyRate` → laborCost 累加 WorkOrder.laborCost）；`reportCompletion`（WorkOrderBizModel:189-225）累加完工量 + 重算 totalCost/unitCost + 生成 MANUFACTURING 入库移动单 + 完工质检 config-gated 钩子（`ERR_INSPECTION_REQUIRED`）；`TestErpMfgWorkOrderEndToEnd`（414 行）落地。
- Evidence — Anti-Hollow: 全部新代码经 `@Inject`/`@BizMutation` 运行时接线，无空函数体/`return null` 占位/吞异常；`ErpMfgErrors` 含 7 个新错误码（ERR_WORK_ORDER_NOT_FOUND/INVALID_STATUS_TRANSITION/PARTIAL_KIT_START_FORBIDDEN/INSPECTION_REQUIRED/OVER_REPORT/ISSUE_LINES_EMPTY/DEFAULT_BOM_NOT_FOUND）。
- Evidence — 文档同步: `docs/logs/2026/07-03.md` 记录全绿验证状态（29 tests/0 Failures + BUILD SUCCESS）；`docs/design/manufacturing/state-machine.md` §实现偏离补注（:167-174，INSPECTING 字典缺失/config-gated/OUTGOING/overheadCost=0）；`docs/backlog/extended-roadmap.md` 工作项 2.2 标 ✅ done。
- Evidence — 五点一致性 + Deferred 诚实: Plan Status=completed 与 4 Phase Status=completed、全部 Exit Criteria `[x]`、Closure Gates 全 `[x]`、日志条目一致；范围内无非已知 Non-Goal 隐藏为 deferred（质检 NCR/CAPA、完工成本结转凭证、APS、BOM 快照、联副、返工、超产、INSPECTING 态均显式带 Successor 触发条件）。

Follow-up:

- 完整质检/NCR/CAPA（2.4，见上方 Deferred）
- 完工成本结转凭证（见上方 Deferred）
- APS / BOM 快照 / 联副 / 返工 / 超产 / INSPECTING 态（见上方 Deferred）
