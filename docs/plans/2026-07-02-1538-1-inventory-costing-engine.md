# 2026-07-02-1538-1-inventory-costing-engine 存货成本核算（FIFO + 成本方法分派 + 期末成本重算）

> Plan Status: completed
> Last Reviewed: 2026-07-02
> Source: `docs/backlog/core-business-roadmap.md`（M4 前置 + deferred item）；`docs/plans/2026-07-02-1000-3-finance-period-close.md` Deferred「存货成本核算集成（period-close §步骤2）」（Successor Required: yes，触发条件：inventory 成本核算落地时接线 step2）；`docs/design/finance/costing-methods.md`
> Related: `2026-07-02-1000-3-finance-period-close.md`（本计划承接其 step2 deferred）、`2026-07-01-0811-2-inventory-stockmove-bizmodel.md`（StockMove 1.3 基线，`StockMoveBookkeeper` 即其记账器）、`2026-07-02-1538-2-manufacturing-bom-routing-rollup.md`（N=2 rollup 产出标准成本供本计划 STANDARD 后继）
> Mission: erp
> Work Item: 存货成本核算（FIFO + 成本方法分派；解除 period-close step2 阻塞）
> Audit: required

## Current Baseline

实时仓库逐项核实的事实：

- **移动加权平均成本已由 `StockMoveBookkeeper` 实现（非空）**：`module-inventory/erp-inv-service/.../stock/StockMoveBookkeeper.java` 在移动单 DONE 时同事务记账——`bookIncoming`（:98）重算 `balance.avgCost = (旧totalCost + 入库totalCost)/(旧totalQty + 入库qty)` 并更新 `ErpInvStockBalance.totalCost/avgCost`；`bookOutgoing`（:123）取 `unitCost = balance.getAvgCost()`、`lineTotalCost = unitCost×qty` 扣减余额；`writeLedger`（:150）写 `ErpInvStockLedger.unitCost`/`totalCost`/`balanceQuantity`/`balanceTotalCost`。`ErpInvStockMoveBizModel` DONE 路径先调 `bookkeeper.bookCompletion` 再调 `InvPostingDispatcher.dispatchIfApplicable`。**故 MOVING_AVERAGE 已端到端生效，COGS 经移动加权平均计算。**
- **记账器硬编码 costMethod=MOVING_AVERAGE，无方法分派**：`bookCompletion`（:50）/`upsertBalance`（:88，`balance.setCostMethod(COST_METHOD_MOVING_AVERAGE)`）/`writeLedger`（:168）恒置移动加权平均；**不读 `ErpMdMaterial.costMethod`/`ErpMdAcctSchema.costingMethod`**——即便物料配 FIFO/BATCH 也走移动加权平均。此为 FIFO 缺失的根因。
- **COGS 过账通道已确立（无需新增字段）**：`InvPostingDispatcher.buildEvent`（`InvPostingDispatcher.java:104-106`）从 `ErpInvStockLedger.getTotalCost()` 汇总为 `PostingEvent.billData.TOTAL_COST`；`InvAcctDocProvider.readTotalCost`（:73）读之。**故 COGS 经「记账器写 ledger.totalCost → 派发器读 ledger.totalCost」通道流动**——FIFO 只须让记账器对 FIFO 物料写出经 FIFO 计算的 ledger.unitCost/totalCost，派发器零改动拾取。
- **成本层表已存在但无维护者（FIFO 载体积空置）**：`ErpInvCostLayer`（`module-inventory/model/app-erp-inventory.orm.xml:338`）：`materialId`/`skuId`/`warehouseId`/`batchNo`/`costMethod`/`incomingQuantity`/`remainingQuantity`/`unitCost`/`totalCost`/`currencyId`/`incomingDate`/`incomingMoveId`/`acctSchemaId`。**表结构足以承载 FIFO 层，但全仓无任何服务追加/消耗它**——FIFO 出库无层可消耗，COGS 回退移动加权平均。
- **余额/流水的真实列名**：`ErpInvStockBalance` 为 `avgCost`/`totalCost`/`totalQuantity`/`costMethod`（**无 unitCost 列**）；`ErpInvStockLedger` 为 `unitCost`/`totalCost`/`costMethod`/`balanceQuantity`/`balanceTotalCost`（**无 stockValue 列**）。
- **批次台账无成本列**：`ErpInvBatch`（inventory.orm.xml:569）仅 `totalQuantity`/`availableQuantity`/`productionDate`/`expiryDate`/`status`——批次成本须挂 `ErpInvCostLayer.batchNo`（cost layer 有 batchNo 列），属 BATCH 方法（Non-Goal）。
- **计价方法配置已具备，字典为 int 码（须核实映射）**：`ErpMdMaterial.costMethod`（master-data.orm.xml:172）+ `ErpMdAcctSchema.costingMethod`（:738），列类型 INTEGER，dict `erp-md/cost-method`。**字典定义在独立 yaml（非 ORM inline），且实测含设计文档未列的码值（20=全月一次加权、40=LIFO）**——int↔方法映射须 Phase 1 核实（与 period-status CLOSED_FINAL 同类「字典码值」契约确认）。
- **period-close 实现类 + INV 模块关账无领域钩子（Phase 3 须新增）**：closePeriod 的具体实现是 `ErpFinAccountingPeriodBizModel.closePeriod`（`module-finance/erp-fin-service/.../entity/ErpFinAccountingPeriodBizModel.java:107`，`implements IErpFinAccountingPeriodBiz extends IErpFinPeriodCloseBiz`——**无 `ErpFinPeriodCloseBizModel` 类**）。模块关账推进：AR/AP/**INV** 经裸 `advanceModule(status, Module.X)`（:120-122，仅翻 `ErpFinAccountingPeriodStatus` 状态位，无领域逻辑）；仅 **AST 有专用 `closeAssetModule`（:178，调 `runDepreciation` 后 advanceModule AST）+ GL 有专用 `closeGlModule`（:185）**，**INV 无 `closeInvModule` 钩子**。故 Phase 3 接线须**新增 `closeInvModule`（镜像 closeAssetModule/runDepreciation 范式：注入 `IErpInvCostingBiz.reclosePeriodCosts` 后 advanceModule INV）并将 closePeriod:122 的 `advanceModule(status, Module.INV)` 改调 `closeInvModule`**。
- **period-close step2 当前 config-gated 跳过**：`2026-07-02-1000-3` 的 closePeriod INV 模块关账段无成本重算；其 Deferred 明示「Successor Required: yes（触发条件：inventory 成本核算落地时接线 step2）」。本计划即该 successor。
- **DAG 依赖方向**：`docs/architecture/data-dependency-matrix.md` finance 处 DAG 顶，inventory 在其下；finance→inventory 为合法 R 只读依赖（period-close INV 模块关账注入 `IErpInvCostingBiz` 调成本重算，无环——对齐 1000-3 finance→assets 折旧注入范式）。
- **剩余差距**：(1) 记账器硬编码移动加权平均，无按物料 costMethod 分派；(2) FIFO 不维护/消耗 `ErpInvCostLayer`，FIFO 物料 COGS 错走移动加权平均；(3) FIFO 红冲不恢复被消耗层；(4) period-close step2 未接线成本重算。

## Goals

- **重构 `StockMoveBookkeeper` 为成本方法策略分派**（不新增并行引擎）：引入 `CostMethodResolver`（`ErpMdMaterial.costMethod` → 回退 `ErpMdAcctSchema.costingMethod` → 回退 `erp-inv.default-cost-method`）+ 策略接口；**MOVING_AVERAGE 策略 = 抽取既有 bookkeeping 逻辑（行为不变）**，FIFO 策略 = 新增。记账器按物料方法分派，仍写同一 `ErpInvStockLedger.unitCost/totalCost` 通道（派发器零改动拾取）。
- **FIFO 入库**：`FifoCostingStrategy.onIncoming` —— 追加 `ErpInvCostLayer`（incomingQuantity=remainingQuantity=入库量、unitCost=入库单价、totalCost、incomingDate、incomingMoveId、acctSchemaId、costMethod=FIFO）；余额 totalQuantity/totalCost 累加（avgCost 不适用于 FIFO 物料，置空或保留入库快照）。
- **FIFO 出库 COGS**：`FifoCostingStrategy.onOutgoing` —— 按 `incomingDate` 升序消耗 `ErpInvCostLayer`（remainingQuantity>0），多层跨消耗汇总加权出库 unitCost（`costing-methods.md §FIFO 出库逻辑`）；更新各层 remainingQuantity；写 `ErpInvStockLedger.unitCost/totalCost`（同移动加权平均通道），使 `InvPostingDispatcher` 的 `TOTAL_COST` 反映 FIFO 计算成本。
- **FIFO 红冲对称**：反向移动单（reverse）恢复被消耗 cost layer 的 remainingQuantity（按反向量恢复最近消耗层），保证成本与库存红冲一致。
- **首次出库无成本前置检查**：FIFO 物料出库时若无 remainingQuantity>0 的 cost layer（未入库），抛 `ErpInvErrors.ERR_COST_NOT_AVAILABLE` 提示先入库（对齐移动加权平均余额为 0 时的同等语义）。
- **period-close step2 接线**：finance `closePeriod` INV 模块关账段注入 `IErpInvCostingBiz.reclosePeriodCosts(periodId)`（finance→inventory R），扫描本期 costMethod=FIFO 但无 cost layer 维护的已过账移动单并补算；解除 1000-3 step2 deferred。
- 行为测试覆盖：方法分派（物料配 FIFO 走 FIFO、配 MOVING_AVERAGE 走既有路径）、FIFO 多层消耗加权 COGS、FIFO 红冲恢复、首次出库无成本拒绝、period-close step2 补算；既有移动加权平均套件无回归。

## Non-Goals

- **BATCH（批次成本）/ INDIVIDUAL（个别计价）方法**：`costing-methods.md §批次成本法/§个别计价法`；需出库指定批次 + FEFO 效期路由 + `ErpInvCostLayer.batchNo` 维度，属独立结果表面。**触发条件**：批次成本管理需求落地时（successor）。
- **STANDARD（标准成本）方法**：标准成本由制造域 cost rollup（`2026-07-02-1538-2` N=2）产出到 `ErpMfgCostRollupLine.unitCost`；本计划不动 STANDARD。**触发条件**：N=2 rollup 落地后（successor）。
- **全月一次加权平均（dict 20）/ LIFO（dict 40）**：字典含此二方法码值但非本期；**触发条件**：相应计价需求落地时。
- **到岸成本（Landed Cost）分摊 + 成本调整单 + 成本差异凭证**：`costing-methods.md §到岸成本/§成本调整`；各为独立结果表面。**触发条件**：到岸成本模块 / 成本调整+PPV 核算需求时。
- **默认最低价策略 / 折扣叠加规则**（`costing-methods.md` 注）：保守估计取较低值，本计划不实现。
- **成本报表**（存货成本明细表/FIFO 队列报表/成本差异表，`§成本报表`）：属 nop-report 报表面。
- **多会计科目表并行成本 / 存货减值（成本与可变现净值孰低）**：属后续。

## Task Route

- Type: `architecture change`（重构既有记账器为成本方法策略分派 + 新增 FIFO 维护/消耗 `ErpInvCostLayer`；改变 FIFO 物料出库 COGS 来源）。**纯服务层 + 既有表，不新增实体/列/字典，不触及 model/*.orm.xml**——`ErpInvCostLayer`/`ErpInvStockBalance`/`ErpInvStockLedger` 表与列均已存在（avgCost/totalCost/unitCost 等）。
- Owner Docs: `docs/design/finance/costing-methods.md`（方法/FIFO 队列/出库逻辑）、`docs/design/inventory/state-machine.md`（DONE 记账挂接点）、`docs/design/inventory/cross-domain.md`（余额/流水同事务 + 移动加权平均由流水维护）、`docs/design/finance/period-close.md`（step2 成本核算）、`docs/architecture/data-dependency-matrix.md`（finance→inventory R）。
- Skill Selection Basis: 重构既有 BizModel 协作类（StockMoveBookkeeper）+ 跨实体（移动单/成本层/余额/流水）+ 事务边界（DONE 同事务）+ 错误码 + 跨域（finance period-close 注入 inventory I*Biz）→ 加载 `nop-backend-dev`。
- **Decision（扩展 vs 并行）**：**选择**重构 `StockMoveBookkeeper` 为策略分派（MOVING_AVERAGE 策略=抽取既有逻辑，FIFO 策略=新增），单一记账入口、单一 ledger 写通道。**替代**：新建并行 `IErpInvCostingBiz` 引擎与记账器并存（双写 ledger/COGS、与既有移动加权平均冲突，rejected）。**残留风险**：重构须保证 MOVING_AVERAGE 路径行为字节级不变（既有套件全绿为门控）。
- **Decision（COGS 通道）**：**选择** FIFO 策略写 `ErpInvStockLedger.unitCost/totalCost`（与移动加权平均同一通道），既有 `InvPostingDispatcher`（读 `ledger.getTotalCost()`）零改动拾取。**替代**：改派发器查成本引擎（耦合过账与成本计算，rejected）。**残留风险**：无。
- **Decision（FIFO 余额表示）**：**选择** FIFO 物料的 `ErpInvStockBalance.avgCost` 置空（avgCost 仅移动加权平均语义），数量/totalCost 仍累加（totalCost=Σ cost layer remaining×unitCost 用于对账）。**替代**：FIFO 也维护 avgCost（语义错误，rejected）。**残留风险**：avgCost 为空的余额由前端/报表按 costMethod 区分展示（报表 Non-Goal）。
- **Decision（方法分派来源）**：**选择** `ErpMdMaterial.costMethod` 优先、回退 `ErpMdAcctSchema.costingMethod`、再回退配置默认（`erp-inv.default-cost-method`，默认 MOVING_AVERAGE）。**替代**：仅 acctSchema 级单一方法（同账套物料计价可能不同，rejected）。
- **Decision（period-close 接线耦合度）**：**选择** `erp-fin-service` 加 `app-erp-inventory-dao`（或 -api，按 `IErpInvCostingBiz` 声明层裁决）compile 依赖，INV 模块关账注入 `IErpInvCostingBiz.reclosePeriodCosts`（finance→inventory R，DAG 合法，对齐 1000-3 折旧门控范式）。**替代**：SPI 反向调用（finance 已可正向 R 依赖 inventory，无需反向，rejected）；不接线（step2 deferred 未解除，rejected）。**残留风险**：单域 finance 测试无 inv-service 时解析失败→config-gated 告警跳过。

## Infrastructure And Config Prereqs

- 配置项：`erp-inv.default-cost-method`（默认 MOVING_AVERAGE，物料/账套均未配时回退）、`erp-inv.costing-enabled`（默认 true，总开关；false 时记账器退化为既有硬编码移动加权平均行为）。经 `AppConfig.var(..., defaultValue)` 读取，无 .env。（`erp-inv.fifo-expiry-priority` 不在本期——FIFO 仅按 incomingDate，FEFO 属 BATCH 后继，见 Deferred。）
- 模块依赖：`erp-inv-service` 已 compile 依赖 master-data-dao（物料/账套/币种）；**新增** finance→inv 方向（period-close 接线）：`erp-fin-service` 加 `app-erp-inventory-dao`（或 -api）compile 依赖（DAG 合法 R）。
- **无 ORM 变更**（不加实体/列/字典）：`ErpInvCostLayer`/`ErpInvStockBalance`/`ErpInvStockLedger` 表列齐备。**故无 ask-first 保护区域门控**（仅 model/*.orm.xml 编辑触发 ask-first；本计划纯服务层 + 既有表 + 重构既有记账器）。
- 无数据迁移；无新增端口/密钥/外部服务。

## Execution Plan

### Phase 1 — 记账器策略分派重构（MOVING_AVERAGE 抽取 + CostMethodResolver）+ 字典码值核实

Status: completed
Targets: `module-inventory/erp-inv-service/.../stock/StockMoveBookkeeper.java`(重构)、`.../costing/CostMethodResolver.java`(新)、`.../costing/CostingStrategy.java`(新接口)、`.../costing/MovingAverageCostingStrategy.java`(新=抽取既有逻辑)、`ErpInvConstants.java`(扩 cost-method 常量)、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: 无（既有 StockMoveBookkeeper + ErpInvCostLayer 表为基线）。

- [x] `Explore`：核实 `erp-md/cost-method` 字典 int 码值↔方法映射（列 INTEGER，字典在独立 yaml，实测含 20=全月一次加权/40=LIFO；确认 MOVING_AVERAGE/FIFO 的 int code，对齐 `ErpInvConstants.COST_METHOD_MOVING_AVERAGE` 既有常量值）。产出码值映射供 CostMethodResolver + 策略分派。
  - Skill: none
- [x] `Add`：`CostingStrategy` 接口（onIncoming/onOutgoing/onReverse，写 `ErpInvStockLedger` 同通道）+ `CostMethodResolver`（material.costMethod → acctSchema.costingMethod → `erp-inv.default-cost-method`；`erp-inv.costing-enabled=false` 返回 MOVING_AVERAGE 退化）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`MovingAverageCostingStrategy` —— **抽取 `StockMoveBookkeeper.bookIncoming/bookOutgoing/writeLedger` 既有逻辑**（avgCost 重算 / unitCost=avgCost / ledger.unitCost+totalCost），行为字节级不变。
  - Skill: `nop-backend-dev`
- [x] `Decision`：重构 `StockMoveBookkeeper.bookCompletion` 为按物料 costMethod 分派到策略（MOVING_AVERAGE→MovingAverageCostingStrategy；FIFO→Phase 2 FifoCostingStrategy）；保留 upsertBalance/findBalance 共享。记录「MOVING_AVERAGE 路径既有套件全绿」门控。理由见 Task Route Decision（扩展 vs 并行）。
  - Skill: none
- [x] `Proof`：`TestErpInvCostingDispatch`（物料配 MOVING_AVERAGE 走既有路径——既有 inventory 套件全绿为回归门控；costing-enabled=false 退化硬编码行为；字典码值映射正确）。回归命令 `mvn test -pl module-inventory/erp-inv-service -am`（既有 30 测试 0 回归）。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 1 交付记账器策略分派重构（MOVING_AVERAGE 行为不变）。解除 Phase 2 FIFO 挂接的分派骨架。

- [x] 记账器按物料 costMethod 分派；MOVING_AVERAGE 路径既有套件全绿（0 回归）；字典码值映射已确认

### Phase 2 — FIFO 策略（cost layer 维护/消耗 + COGS + 红冲恢复）

Status: completed
Targets: `.../costing/FifoCostingStrategy.java`(新)、`ErpInvErrors.java`(扩)、`ErpInvStockMoveBizModel`(reverse 路径挂接 FIFO 恢复)
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（分派骨架 + 字典码值）。

- [x] `Add`：`FifoCostingStrategy.onIncoming` —— 追加 `ErpInvCostLayer`（incomingQuantity=remainingQuantity=入库量、unitCost=入库单价、totalCost、incomingDate、incomingMoveId、acctSchemaId、costMethod=FIFO）；余额 totalQuantity/totalCost 累加、avgCost 置空（FIFO 语义）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`FifoCostingStrategy.onOutgoing` —— 按 `incomingDate` 升序消耗 `ErpInvCostLayer`（remainingQuantity>0），多层跨消耗汇总加权出库 unitCost（`§FIFO 出库逻辑` 步骤 2-3）；更新各层 remainingQuantity；写 `ErpInvStockLedger.unitCost/totalCost`（同移动加权平均通道，派发器零改动）；扣减余额 totalQuantity/totalCost。
  - Skill: `nop-backend-dev`
- [x] `Add`：首次出库无成本前置检查——FIFO 物料出库时无 remainingQuantity>0 的 cost layer 抛 `ErpInvErrors.ERR_COST_NOT_AVAILABLE`（提示先入库）。
  - Skill: `nop-backend-dev`
- [x] `Add`：FIFO 红冲对称——`ErpInvStockMoveBizModel.reverse`（:133）**生成反向移动单重走正常 DONE 流程**（反向入库 move 会触发 `onIncoming` 追加 cost layer，而非直接恢复被消耗层），故 FIFO 红冲须裁决语义并防双计：(a) 反向入库按原出库消耗的加权 unitCost 追加新层（成本回加对齐），或 (b) 直接恢复被消耗层 remainingQuantity——**执行者须在 Proof 中验证成本不变量（红冲后 cost layer 总量/余额恢复至原出库前，无重复计入）**，二者择一并记录为 Decision。
  - Skill: `nop-backend-dev`
  - **Decision (a) 采纳**：反向入库按原出库刷新的加权 unitCost 追加新层（`FifoCostingStrategy.onOutgoing` 末尾刷回 `line.unitCost`，reverse 透传至反向入库行）。Proof `testReverseRestoresCostInvariant` 验证红冲后 Σ layer remaining×unitCost 恢复至原出库前（容差 0.01 舍入残差）。
- [x] `Decision`：FIFO 余额表示（avgCost 置空）+ COGS 通道（写 ledger.unitCost/totalCost），见 Task Route Decision。
  - Skill: none
- [x] `Proof`：`TestErpInvFifoCosting`（入库追加 cost layer remainingQuantity/unitCost；出库多层跨消耗加权 unitCost——队列1 50@10 消耗 30→300、出库 60 跨队列 20@10+40@12→680；首次出库无成本抛 ERR_COST_NOT_AVAILABLE；红冲恢复 cost layer/余额；COGS 经 ledger.totalCost 流入派发器 TOTAL_COST）。`mvn test -pl module-inventory/erp-inv-service -am -Dtest=TestErpInvFifoCosting*`。
  - Skill: `nop-backend-dev`
  - **验证**：6 测试全绿（入库追加层 / 单层消耗 / 多层加权 620 / 首次无成本拒绝 / 红冲不变量恢复 / COGS ledger.totalCost=-680）；全量 inventory 42 测试 0 回归。

Exit Criteria:

> Phase 2 交付 FIFO 维护/消耗 cost layer + COGS 经既有通道 + 红冲恢复。解除 Phase 3 period-close 接线的成本基线。

- [x] FIFO 入库追加层 / 出库多层消耗加权 COGS + 写 ledger.totalCost + 红冲恢复单测通过

### Phase 3 — period-close step2 接线（finance→inventory）+ 端到端 + 文档/日志

Status: completed
Targets: `.../costing/IErpInvCostingBiz.java`(新)、`.../costing/ErpInvCostingBizModel.java`(新, reclosePeriodCosts)、`module-finance/erp-fin-service/.../entity/ErpFinAccountingPeriodBizModel.java`(新增 closeInvModule + 改 closePeriod:122)、`erp-fin-service/pom.xml`(加 inv 依赖)、`docs/logs/2026/{执行当日}.md`、`docs/backlog/core-business-roadmap.md`、`docs/design/finance/costing-methods.md`(偏离补注)、`docs/design/finance/period-close.md`(step2 接线补注)
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 + Phase 2。

- [x] `Add`：`IErpInvCostingBiz.reclosePeriodCosts(periodId)` —— 扫描本期已过账但 cost layer 缺失/COGS 异常（unitCost 空/零）的 FIFO 移动单，按 FIFO 补算 COGS/入库成本（兜底）；返回补算报告。
  - Skill: `nop-backend-dev`
  - **验证**：`IErpInvCostingBiz`（dao 层声明使 finance 可编译依赖 + `@BizModel("ErpInvCosting")` 独立服务型 BizObject，经 IBizObjectManager 跨模块解析）+ `CostingRecloseReport` DTO 已落地。日期窗口由 finance 从会计期间解析后传入（inventory 不反向依赖 finance，DAG finance→inventory R）。
- [x] `Add`：period-close INV 模块关账接线——`ErpFinAccountingPeriodBizModel` **新增 `closeInvModule`（镜像 `closeAssetModule`/`runDepreciation` 范式：注入 `IErpInvCostingBiz` 调 `reclosePeriodCosts` 后 `advanceModule(status, Module.INV)`），并将 `closePeriod:122` 的 `advanceModule(status, Module.INV)` 改调 `closeInvModule`**（finance→inventory R，DAG 合法）；单域 finance 测试无 inv-service 时 try/catch 告警跳过（对齐 1000-3 折旧门控）；`erp-fin-service/pom.xml` 加 `app-erp-inventory-dao`（或 -api）compile 依赖。
  - Skill: `nop-backend-dev`
  - **验证**：`closeInvModule`（:180 调 `recloseInvCosts` + advanceModule INV）+ `recloseInvCosts`（:225 config-gated `erp-fin.inv-costing-reclose-on-close` + try/catch）已落地；closePeriod:124 改调 closeInvModule；pom 加 `app-erp-inventory-dao` compile 依赖；finance 93 测试 0 回归。
- [x] `Proof`：端到端 `TestErpInvFifoCostingEndToEnd`（采购入库 FIFO 物料 @10×50 + @12×40 → 两 cost layer；销售出库 60 → FIFO 跨层消耗 COGS=20×10+40×12=680，SALES_OUTPUT 过账 TOTAL_COST=680；period-close reclosePeriodCosts 兜底补算未成本化单据）。`mvn test -pl module-inventory/erp-inv-service -am -Dtest=TestErpInvFifoCostingEndToEnd*`；`mvn test -pl module-finance/erp-fin-service -am`（period-close 无回归）。
  - Skill: `nop-backend-dev`
  - **验证**：3 测试全绿（FIFO 全链 cost layer→COGS=680→SALES_OUTPUT 凭证 680/680；reclosePeriodCosts 正常数据 no-op；reclosePeriodCosts 补建缺失层 25@8）；`mvn test -pl module-inventory/erp-inv-service -am` = 45 测试 0 回归；`mvn test -pl module-finance/erp-fin-service` = 93 测试 period-close 无回归。
- [x] `Add`：`docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`core-business-roadmap.md` 补注 1000-3 step2 deferred 承接完成；`costing-methods.md`/`period-close.md` 偏离（BATCH/STANDARD/全月一次/LIFO/Landed Cost Non-Goal + 记账器策略分派实现注记）补注。
  - Skill: none
  - **验证**：`docs/logs/2026/07-02.md` 加 1538-1 条目（含全绿验证状态）；`core-business-roadmap.md` M4.3 标注 + 存货成本核算引擎 done 补注（step2 deferred 承接）；`docs/design/finance/costing-methods.md` 加「实现注记（计划 1538-1）」节（策略分派/COGS 通道/红冲 Decision/step2 接线/Non-Goal）；`docs/design/finance/period-close.md` line 9 step2 接线注记既有。

Exit Criteria:

> Phase 3 交付 period-close step2 接线 + 端到端全链。完整仓库验证属 Closure Gates。

- [x] period-close INV 段调 reclosePeriodCosts（兜底补算）+ 端到端（FIFO 入库→cost layer→出库 COGS→过账）全链单测通过；finance period-close 无回归

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0de34f9b3ffes692gLKngyjNE7`，独立 general 子代理）。2 BLOCKER：(B1) 既有 `StockMoveBookkeeper` 已实现移动加权平均（bookIncoming 重算 avgCost/bookOutgoing 取 avgCost 为 COGS/writeLedger），原计划误称「unitCost 不由任何成本方法驱动」并拟新建并行 `MovingAverageCostingStrategy` 致双写冲突——真实缺口为 FIFO/cost layer + 按物料 costMethod 分派（现硬编码）；(B2) 列名错误——`ErpInvStockBalance` 为 `avgCost`（非 unitCost）、`ErpInvStockLedger` 为 `unitCost/totalCost`（无 stockValue），且 `InvPostingDispatcher` 从 `ledger.getTotalCost()`（:104-106）组装 TOTAL_COST（非移动单行 unitCost×qty）。**已修订**：Current Baseline 全面重写（承认既有 bookkeeper + 真实列名 + 派发器 ledger.totalCost 通道）；Goals/Task Route 改为「重构既有记账器为策略分派（MOVING_AVERAGE 抽取行为不变 + FIFO 新增）」而非并行引擎；COGS 通道改为写 ledger.unitCost/totalCost（派发器零改动）；去掉错误的 stockValue/unitCost 列引用与「写回移动单行」假设。S 级 nit（cost-method 字典 int 码含 20/40、config 命名空间）已吸收。
- Independent draft review iteration 2: **needs revision（1 新 BLOCKER）**（`ses_0de27b341ffev6x7aBqZx29w1L`，独立 general 子代理）。iter-1 B1（既有 bookkeeper）/B2（列名+ledger.totalCost 通道）**确认已解决**。新提 1 BLOCKER（B3）：Phase 3 目标误称 `ErpFinPeriodCloseBizModel.java`（不存在；具体实现为 `ErpFinAccountingPeriodBizModel` :107 `implements IErpFinAccountingPeriodBiz extends IErpFinPeriodCloseBiz`），且未披露 INV 模块关账现为裸 `advanceModule(status, Module.INV)`（:122，仅翻状态位、无领域钩子），仅 AST（`closeAssetModule` :178）/ GL（`closeGlModule` :185）有专用关账方法，**INV 无 `closeInvModule`**。**已修订**：Current Baseline 补 period-close 实现类 + INV 无领域钩子披露；Phase 3 目标改 `ErpFinAccountingPeriodBizModel`；Phase 3 执行项改为「新增 `closeInvModule`（镜像 closeAssetModule/runDepreciation）并将 closePeriod:122 改调之」。S 级 nit（`fifo-expiry-priority` 配置无 phase 引用）已吸收（移出本期 config，归 BATCH Deferred 触发）。
- Independent draft review iteration 3: **accept / consensus**（`ses_0de2175a2ffeAb2cPJYB7YbJj7`，独立 general 子代理）。iter-2 B3（Phase 3 目标类 `ErpFinPeriodCloseBizModel` 不存在 + INV 无 closeInvModule 钩子未披露）**确认已解决**（核实 `ErpFinAccountingPeriodBizModel:107` closePeriod / :122 裸 advanceModule INV / :178 closeAssetModule / :185 closeGlModule；Phase 3 目标改正确类 + 新增 closeInvModule + 改调 closePeriod；Current Baseline 已披露缺失钩子）。iter-1 B1/B2 仍成立（bookkeeper 硬编码 :88/:168、列名 avgCost/totalCost/unitCost、dispatcher 读 ledger.getTotalCost :105、无 ORM 变更）。`fifo-expiry-priority` 已移出本期 config。**无 BLOCKER**。非阻塞 nit（FIFO 红冲：reverse 重走 DONE 流程致反向入库触发 onIncoming 追加层，须防双计）**已吸收**：Phase 2 红冲项改为要求执行者裁决红冲语义（追加层 vs 恢复消耗层）并在 Proof 验证成本不变量（无重复计入）。反松弛合规、Deferred 均命名触发。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成：记账器按物料 costMethod 分派（MOVING_AVERAGE 既有行为不变 + FIFO 新增）+ FIFO cost layer 维护/消耗 + COGS 经既有通道 + 红冲恢复 + period-close step2 接线，行为测试通过
- [x] 相关文档对齐：`core-business-roadmap.md` step2 deferred 承接标注；当日日志已记；`costing-methods.md`/`period-close.md` Non-Goal 偏离补注
- [x] 已运行验证：`mvn test -pl module-inventory/erp-inv-service -am`（既有移动加权平均套件 0 回归 + 新增 FIFO）+ `mvn test -pl module-finance/erp-fin-service -am`（period-close 无回归）；根 `mvn clean install -DskipTests`
- [x] 无范围内项目静默降级（BATCH/STANDARD/全月一次/LIFO/Landed Cost/成本调整/报表 均为计划内 Non-Goal）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### BATCH（批次成本）/ INDIVIDUAL（个别计价）方法

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 需出库指定批次 + FEFO 效期路由 + ErpInvCostLayer.batchNo 维度；本计划仅 MOVING_AVERAGE（既有）+ FIFO。
- Successor Required: yes（触发条件：批次成本管理需求落地时）

### STANDARD（标准成本）方法

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 标准成本由制造域 cost rollup（N=2）产出到 ErpMfgCostRollupLine.unitCost；本计划不动 STANDARD。
- Successor Required: yes（触发条件：N=2 rollup 落地后）

### 全月一次加权平均（dict 20）/ LIFO（dict 40）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 字典含此二方法码值但非本期；本计划仅 MOVING_AVERAGE + FIFO。
- Successor Required: yes（触发条件：相应计价需求落地时）

### 到岸成本（Landed Cost）+ 成本调整单 + 成本差异凭证 + 成本报表

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 各为独立结果表面（到岸成本单/调整单 + 审批 + 凭证 / nop-report 报表）。
- Successor Required: yes（触发条件：到岸成本模块 / 成本调整+PPV / 报表需求时）

## Closure

Status Note: 三阶段全部完成并验证。`StockMoveBookkeeper` 重构为按物料 costMethod 策略分派（`CostMethodResolver`：material→acctSchema→`erp-inv.default-cost-method`；`costing-enabled=false` 退化）；`MovingAverageCostingStrategy` 抽取既有逻辑行为不变（既有移动加权平均套件全绿为回归门控）；`FifoCostingStrategy` 以 `ErpInvCostLayer` 为 FIFO 队列（入库追加层/出库按 incomingDate 升序多层消耗加权 unitCost/写 `ledger.unitCost+totalCost` 使 `InvPostingDispatcher` 零改动拾取/首次无成本抛 `ERR_COST_NOT_AVAILABLE`）；FIFO 红冲 Decision (a) 反向入库按原出库加权 unitCost 追加新层（成本不变量经测试断言）；period-close step2 接线 `IErpInvCostingBiz.reclosePeriodCosts`（inv-dao 声明 + `@BizModel("ErpInvCosting")` impl）+ finance `closeInvModule`/`recloseInvCosts`（config-gated `erp-fin.inv-costing-reclose-on-close`，try/catch 跳过）+ `erp-fin-service/pom.xml` 加 `app-erp-inventory-dao`（finance→inventory R，DAG 合法）。无 ORM 变更。解除 1000-3 step2 deferred。`mvn test -pl module-inventory/erp-inv-service -am` = 45 测试 0 失败；`mvn test -pl module-finance/erp-fin-service` = 93 测试 0 失败；根 `mvn clean install -DskipTests` BUILD SUCCESS（146 模块）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 `ses_0dd28ffeaffed9F10lRt6FL0ua`（fresh session，read-only，未实现本工作，执行者未自我审计）
- Verdict: **passes closure audit**（0 BLOCKERs）
- Evidence:
  - 产物：`StockMoveBookkeeper`（:75-94 分派）+ `CostMethodResolver`/`CostingStrategy`/`MovingAverageCostingStrategy`/`FifoCostingStrategy`/`BookingContext`（inv-service/.../costing/）+ `ErpInvErrors.ERR_COST_NOT_AVAILABLE` + `ErpInvConstants` 扩 cost-method/config 常量 + `IErpInvCostingBiz`/`CostingRecloseReport`（inv-dao）+ `ErpInvCostingBizModel`（@BizModel("ErpInvCosting")）+ `ErpFinAccountingPeriodBizModel.closeInvModule`(:180)/`recloseInvCosts`(:225) + `ErpFinConstants.CONFIG_INV_COSTING_RECLOSE_ON_CLOSE` + `erp-fin-service/pom.xml`（app-erp-inventory-dao compile）+ `app-service.beans.xml`。
  - COGS 通道零改动核实：`InvPostingDispatcher.buildEvent`（:104-106）读 `ledger.getTotalCost().abs()` → FIFO 策略写 `ledger.unitCost/totalCost` 即流入，无派发器改动。
  - 测试（独立重跑，非采信声明）：`TestErpInvFifoCostingEndToEnd` 3（FIFO 全链 cost layer→COGS=680→SALES_OUTPUT 凭证 / reclose no-op / reclose 补建缺失层）/ `TestErpInvFifoCosting` 6（含 `testReverseRestoresCostInvariant` 红冲成本不变量）/ `TestErpInvCostingDispatch` 6；既有 `TestErpInvStockMoveBookkeeping` 移动加权平均 5 全绿（0 回归）。
  - 验证：inv-service = 45 测试 0 失败；erp-fin-service = 93 测试 0 失败（period-close 套件无回归）；根 `mvn clean install -DskipTests` BUILD SUCCESS（146 reactor）。
  - DAG：finance→inventory R（IErpInvCostingBiz 声明于 inv-dao，finance compile 依赖 dao，无环）。Nop 约定合规（@Inject 非 private / NopException+ErrorCode / @BizMutation 不叠 @Transactional / CoreMetrics+StringHelper）。
  - Non-Goal：BATCH/INDIVIDUAL/STANDARD/全月一次/LIFO/Landed Cost/成本调整/报表 均未半实现（`CostMethodResolver.isSupported` 仅 10/30，其余回退默认）。
  - 非阻塞 nit：(N1) `CostingStrategy` 接口无 `onReverse`（Phase 1 item 文本陈述，已由 Phase 2 Decision 落地——reverse 重走 DONE 流程经 onIncoming/onOutgoing，owner doc 准确）；(N2)/(N3) 关账簿记（本次已 flip Plan Status completed + 勾选门控 6/7/8 + 填本节）。

Follow-up:

- BATCH / INDIVIDUAL 方法（见上方 Deferred）
- STANDARD 方法（见上方 Deferred，依赖 N=2 rollup）
- 全月一次 / LIFO / 到岸成本 / 成本调整 / 成本报表（见上方 Deferred）
