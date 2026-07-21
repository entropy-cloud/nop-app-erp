# 成本核算详细设计

## 目的

说明 ERP 系统中存货成本核算的多种方法、成本计算逻辑、成本调整机制。参考 iDempiere 的 CostingMethod 设计，为 nop-app-erp 提供灵活的成本核算能力。

本文件是 `idempiere.md` 调研结论的落地设计，是 `finance/period-close.md` 的详细展开。

## 设计背景

### 调研发现

从 iDempiere 的调研中发现：

| 发现 | 说明 |
|------|------|
| 多成本核算方法 | AveragePO/Fifo/AverageInvoice |
| 成本要素分离 | CurrentCostPrice/CurrentCostPriceLL（landed cost） |
| 累计金额追踪 | CumulatedAmt |
| 成本类型配置 | M_CostType_ID |

### 核心价值

- **多方法支持**：移动加权平均、FIFO、批次成本
- **到岸成本**：支持 landed cost 独立核算
- **成本追溯**：成本变化可追溯到具体移动单
- **成本调整**：支持手工调整成本差异

## 实现注记（计划 `2026-07-02-1538-1`）

本设计已部分落地（MOVING_AVERAGE + FIFO），由 inventory 域记账器按物料 `costMethod` **策略分派**实现，权威源码：

- **记账器分派**：`StockMoveBookkeeper.bookCompletion`（inventory DONE 同事务记账器）按物料 costMethod 分派到 `CostingStrategy`；`CostMethodResolver` 解析顺序：`ErpMdMaterial.costMethod` → `ErpMdAcctSchema.costingMethod` → 配置 `erp-inv.default-cost-method`（默认 MOVING_AVERAGE）；`erp-inv.costing-enabled=false` 时退化为既有硬编码移动加权平均行为（向后兼容总开关）。
- **MOVING_AVERAGE（`MovingAverageCostingStrategy`）**：抽取既有记账逻辑（入库重算 `balance.avgCost`/出库 `unitCost=avgCost`/写 `ledger.unitCost+totalCost`），行为字节级不变——移动加权平均端到端既有套件全绿为回归门控。
- **FIFO（`FifoCostingStrategy`）**：以 `ErpInvCostLayer` 表为 FIFO 队列——入库追加层（`incomingQuantity=remainingQuantity`、`unitCost`、`incomingDate`、`incomingMoveId`、`costMethod=FIFO`），余额 `avgCost` 置空（仅移动加权平均语义）、`totalCost` 累加；出库按 `incomingDate` 升序多层消耗各层 `remainingQuantity`，汇总加权出库 `unitCost`；首次出库无 `remainingQuantity>0` 的层抛 `ERR_COST_NOT_AVAILABLE`（对齐移动加权平均余额为 0 的同等语义）。
- **COGS 通道**：FIFO 策略写 `ErpInvStockLedger.unitCost/totalCost`（与移动加权平均同一通道），既有 `InvPostingDispatcher` 读 `ledger.totalCost` 汇总为 `TOTAL_COST` 零改动拾取——FIFO 物料 COGS 经既有 SALES_OUTPUT/PURCHASE_INPUT 过账通道流动。
- **FIFO 红冲**：`ErpInvStockMoveBizModel.reverse` 生成反向移动单重走正常 DONE 流程，反向入库按原出库刷新的加权 `unitCost` 追加新 cost layer（Decision (a)，避免直接恢复被消耗层致双计），保证红冲后成本不变量（Σ layer remaining×unitCost 恢复至原出库前）。
- **期末成本兜底（period-close §步骤2）**：`IErpInvCostingBiz.reclosePeriodCosts(periodId,startDate,endDate)` 扫描本期 DONE 的 FIFO 移动单，对成本层缺失的入库补建、对 COGS 异常（`ledger.unitCost` 空/零）的出库按 FIFO 重算并刷新流水（正常路径补算数为 0；非 0 为历史/异常单据兜底修复）。finance 期末结账 INV 模块关账（`ErpFinAccountingPeriodBizModel.closeInvModule`）经 `IBizObjectManager` 跨模块调用（finance→inventory R，DAG 合法），config-gated `erp-fin.inv-costing-reclose-on-close`（默认 true），单域 finance 测试无 inv-service 时 try/catch 告警跳过。

**Non-Goal（计划 `1538-1` Deferred But Adjudicated，已裁定留后继）**：BATCH（批次成本）/ INDIVIDUAL（个别计价，需出库指定批次 + FEFO 效期路由 + `ErpInvCostLayer.batchNo` 维度）、~~STANDARD（标准成本，依赖制造域 cost rollup N=2 产出到 `ErpMfgCostRollupLine.unitCost`）~~（**已收口，见下文 plan 2026-07-05-0427-2 实现注记**）、~~成本调整单 + 成本差异凭证（采购价格调整/成本差异/标准成本重估）~~（**已收口，见 plan 2026-07-05-2352-3 实现注记**）、全月一次加权平均（dict 20）/ LIFO（dict 40）、~~到岸成本（Landed Cost）分摊算法（成本调整单已预留 `adjustType=LANDED_COST_SUPPLEMENT` 码值供 successor）~~（**已收口，见 plan 2026-07-10-1100-3 实现注记**）、默认最低价/折扣叠加规则（取较低值的保守估计）、成本报表（存货成本明细/FIFO 队列/差异表，属 nop-report 报表面）、多账套并行成本、存货减值（成本与可变现净值孰低）。各 Non-Goal 均已命名 successor 触发条件，见计划 Deferred 章节。

## 实现注记（计划 `2026-07-10-1100-3`）

本节承接 `1538-1` Deferred「到岸成本（Landed Cost）分摊算法」，触发条件「costing-methods.md §到岸成本设计落地需求」已满足。

- **到岸成本单实体（`ErpInvLandedCost`/`ErpInvLandedCostLine`）**：头-行结构（头携带 code/receiveId/supplierId/allocationMethod/docStatus/approveStatus/posted；行携带 costElement/amount/apPartnerId）。审核时关联采购入库单（`ErpPurReceive`，跨域只读 DAO 访问）。
- **分摊引擎（`LandedCostAllocationEngine`）**：纯函数式——输入入库行 + 费用要素合计 + allocationMethod(BY_AMOUNT/BY_QUANTITY/BY_WEIGHT)，输出每入库行的分摊金额与新单位成本。末行吸收舍入差保证 Σ=totalCost。
- **审核编排（`ErpInvLandedCostProcessor`）**：approve 步骤——(1) 加载到岸成本 + 费用行 + 入库单 + 入库行；(2) 校验入库单已审核 + 防重复分摊；(3) 调引擎分摊；(4) 创建 `ErpInvCostAdjust`(type=LANDED_COST_SUPPLEMENT) + 行；(5) 调 `CostAdjustmentService.applyCostAdjust` **直接更新成本层**（不走 `ErpInvCostAdjustProcessor.applyCostAdjust` 完整链，避免 COST_ADJUSTMENT(420) 与 LANDED_COST(490) 双重入账）；(6) LANDED_COST 过账。
- **过账（`LandedCostAcctDocProvider` + `LandedCostPostingDispatcher`）**：业务类型 `LANDED_COST`(490)。借：每入库行分摊金额 → 存货(1401)；贷：每费用要素 → 应付账款(2202, partnerId=费用行应付对象或采购供应商)。
- **本期 Non-Goal**：多段到岸成本累计管理（同一入库单多次分摊）、到岸成本预估、logistics path-2 运费自动创建到岸成本单的完整编排——各归 successor。

## logistics path-2 到岸成本自动创建衔接点（plan `2026-07-11-2329-1` 后端 + `2026-07-19-0849-2` 浏览器层）

到岸成本单可由 logistics 域 DELIVERED 事件自动创建（与销售域/采购域/财务域人工创建并列的**第 4 入口**）：

- **衔接链路**：logistics `ErpLogShipmentBizModel.handleTrackingWebhook` 推进至 DELIVERED → `onDelivered` 按 `relatedBillType` 分派 → **PURCHASE_RECEIPT** 走 `handlePurchaseReceiptDelivered`（config-gated `erp-log.path2-landed-cost-auto-create`，默认 false 向后兼容）→ 调 `IErpInvLandedCostBiz.generateFreightLandedCost(receiveCode, freightAmount, freightCurrencyId, null, ctx)` → 委派 `ErpInvLandedCostProcessor.generateFreightLandedCost` 创建 DRAFT 到岸成本单（FREIGHT 费用行，apPartnerId=receive.supplierId，totalCostAmount=freightAmount）。
- **结果面**：仅创建 DRAFT + UNSUBMITTED 单据，**不**触发分摊/CostAdjust/`LANDED_COST(490)` 过账（这些归人工审核入口 `approve`，由 plan `2026-07-10-1100-3` 提供）。logistics 侧成功后 `freightSettlementStatus` 翻 SETTLED。
- **浏览器层 E2E**：经 `tests/e2e/business-actions/log-path2-landed-cost-auto-create.action.spec.ts`（plan `2026-07-19-0849-2`）覆盖正路径（DRAFT 头+行字段精确数值断言）+ freightAmount=0 边界（显式断言无 LandedCost 创建）。后端单测覆盖 path-2 失败重试/幂等（`TestErpLogPath2LandedCost`）。

## 到岸成本红冲实现注记（计划 `2026-07-18-1745-2`）

到岸成本审核过账后如需回滚（"错误审核"纠错路径），新增 `ErpInvLandedCost.reverseApprove(@BizMutation)` 入口闭环：

- **红冲编排（`ErpInvLandedCostProcessor.reverseApprove`）**：(1) 守卫 `posted=true + approveStatus=APPROVED`（未过账抛 `ERR_LANDED_COST_NOT_POSTED`）；(2) 调 `LandedCostPostingDispatcher.reverse(landedCost)` 红冲 `LANDED_COST(490)` 凭证（billHeadCode=`landedCost.code` 与正向对称，委派 `InvPostingExecutor.reverse` → `IErpFinVoucherBiz.reverse` 生成红字凭证 + 原凭证 `isReversed=true`）；(3) 按 `LANDED_COST-{code}` 命名约定反查关联 `ErpInvCostAdjust(LANDED_COST_SUPPLEMENT)` + 行 → 调 `CostAdjustmentService.reverseCostAdjust` 反向应用成本层（MOVING_AVERAGE：`balance.avgCost = line.oldUnitCost` 回退、`totalCost -= adjustAmount`；FIFO：按 `-line.id` 哨兵删调整层；STANDARD_REVALUATION：删 FIRMED rollup）；(4) 翻 `posted=false / approveStatus=REJECTED / docStatus=CANCELLED` + 同步 CostAdjust 单 `posted=false`。
- **红字凭证行**：`LANDED_COST` 红字凭证行同向取负（Dr 1401=-X / Cr 2202=-X，dcDirection 不变），与原凭证共用 billHeadCode（`voucher_bill_r` 回链按 `postingType=NORMAL|REVERSAL` 区分）。
- **残留风险（Deferred）**：FIFO 调整层已部分被后续出库消耗时 `removeFifoAdjustLayer` 直接物理删除可能破坏已扣减层——已由 Phase 4 单测覆盖 MOVING_AVERAGE 主路径（FIFO 边界场景归 successor，触发条件：实际启用 FIFO 物料的到岸成本红冲遇此场景时）。

## 实现注记（计划 `2026-07-05-0427-2`）

本节承接 `1538-1` Deferred「STANDARD（标准成本）方法」，触发条件「N=2 BOM/工艺成本卷算 rollup 落地后」已满足（`2026-07-02-1538-2-manufacturing-bom-routing-rollup.md` 已完成并产出 `ErpMfgCostRollupLine.unitCost`）。

- **STANDARD（`StandardCostingStrategy`）**：入库按标准成本写 `ledger.unitCost/totalCost`（实际成本经 PPV 通道分离），出库 `unitCost=标准成本` 写 `ledger` 走既有 `InvPostingDispatcher` 拾取（COGS 通道零改动，同 FIFO 范式）。标准成本来源经 `StandardCostResolver` 解析：(1) 最近一条 `status=FIRMED` 的 `ErpMfgCostRollupLine.unitCost`（直接查 mfg-dao 实体，inventory→manufacturing 经 mfg-dao 编译期依赖）；(2) config-gated `erp-inv.standard-cost-fallback-to-material-master=true`（默认）时回退物料主数据 `standardCost` 列（当前 `ErpMdMaterial` 无此列，本路径恒 null，后续冗余发布列落地后自动生效）；均无抛 `ERR_STANDARD_COST_NOT_AVAILABLE`。`CostMethodResolver.isSupported` 增 STANDARD 码值识别与分派。
- **采购价差（PPV）捕获**：采购入库 DONE 时（`InvPostingDispatcher.dispatchPurchasePriceVariance`），STANDARD 物料的实际入库 `line.unitCost` 与标准 `ledger.unitCost` 差额 × qty = PPV，config-gated `erp-inv.standard-cost-ppv-enabled`（默认 true）。PPV 经新业务类型 `PURCHASE_PRICE_VARIANCE` 过账（`PurchasePriceVarianceAcctDocProvider`）：实际>标准→借材料成本差异(1404)/贷暂估应付(2202)；实际<标准→借暂估应付(2202)/贷材料成本差异(1404)；金额=|实际−标准|×qty。
- **本期 Non-Goal**：~~生产差异（材料用量/人工效率/费率/产量/制造费用）归 `variance-analysis.md` 工单完工触发面~~（**已收口，见 plan 2026-07-05-1838-2 实现注记**：`ProductionVarianceCalculator` + 完工触发 + `ProductionVarianceDispatcher` 过账已落地）；~~标准成本更新/重估流程（成本调整单+审批+重估凭证）归 1538-1 Deferred「成本调整单」~~（**已收口，见 plan 2026-07-05-2352-3 实现注记**：`ErpInvCostAdjust` 头-行实体 + `CostAdjustmentService` 引擎 + 审批门控 + `CostAdjustmentAcctDocProvider` 过账 + `STANDARD_REVALUATION` 发布 FIRMED rollup 已落地；制造件标准成本重估归制造域 `rollupCost` successor）。

## 实现注记（计划 `2026-07-05-1838-2`）

本节承接 `0427-2` Deferred「生产差异」，触发条件「工单完工差异分析需求落地」（依据：`variance-analysis.md` 设计权威指定 + 技术前置就绪）。

- **生产差异计算引擎（`ProductionVarianceCalculator`）**：工单完工（COMPLETED）时，按 5 类差异（材料用量/人工效率/人工费率/制造费用/产量）逐项对比标准成本（FIRMED cost rollup × 完工数量）vs 实际成本（WorkOrder 四要素累加），写 `ErpMfgCostVariance` 行。材料段仅算用量差异（价格差异归 PPV 避免重复计入）。
- **完工触发**：`ErpMfgWorkOrderProcessor.reportCompletion` 的 `willFinish` 分支调用差异计算，config-gated `erp-mfg.variance-auto-calc-enabled`（默认关）。失败隔离仅记 ERROR 日志，不阻断完工。
- **差异过账（`ProductionVarianceDispatcher`）**：差异计算后按成本要素（材料/人工/制造费用）汇总净差异，组装 PostingEvent 经 `IErpFinVoucherBiz.post` 提交（`PRODUCTION_VARIANCE` 业务类型，`ProductionVarianceAcctDocProvider` 方向相关借贷分解到差异科目/WIP 科目），成功回写 `posted=true`。
- **手动入口**：`ErpMfgCostVariance__calculateVariances` @BizMutation（幂等：先删旧行再重算，仅 COMPLETED 工单允许）+ `findByWorkOrder` / `aggregateByType` @BizQuery 查询。

## 实现注记（计划 `2026-07-13-0455-2`）

本节承接 `2026-07-12-1504-1` 裁决 M-2（成本要素拆分 successor），闭合 `CostRollupService` overhead/subcontract 两要素恒 0 缺口。依赖 N=1（`2026-07-13-0455-1` 委外引擎）提供已过账委外订单加工费归集源。

- **overhead 制造费用（config-gated 分配率）**：`CostRollupService` 经 `erp-mfg.overhead-allocation-enabled`（默认 false 向后兼容）控制。关时 `overheadCost`=0（行为不变）；开时按 `erp-mfg.overhead-allocation-mode` 选择分配模式：`MACHINE_HOUR`=Σ(工序 standardTime/60)×`erp-mfg.overhead-allocation-rate`（机器工时×费率）；`LABOR_RATIO`=laborCost×rate（人工成本比例）。工作中心 schema 拆分（`ErpMfgWorkcenter` laborRate/overheadRate 分列）为 successor（ask-first ORM 保护区域，触发条件：产品要求工作中心级精确费率）。
- **subcontract 委外费（归集源 = N=1 已过账委外订单）**：`CostRollupService` 经 `erp-mfg.subcontract-cost-aggregation-enabled`（默认 false）控制。关时 `subcontractCost`=0；开时按物料聚合 `docStatus=COMPLETED` 委外订单（`ErpMfgSubcontractOrder.productId`）的 `processingFee`，按委外行产量（`ErpMfgSubcontractOrderLine.quantity`）分摊为单位委外成本。
- **CostBreakdown 四要素**：`unitCost = material + labor + overhead + subcontract`（`CostRollupLineView` 补 `subcontractCost` 字段，`ErpMfgCostRollupLine` schema 四要素列已存在）。FIRMED rollup 行 unitCost 含四要素后经 `StandardCostResolver` 传播进存货 STANDARD 成本法（costing-methods.md:56 链路）。
- **本期 Non-Goal**：工作中心 laborRate/overheadRate schema 拆分（精确工作中心级费率，ask-first successor）。~~subcontract 委外差异（`ProductionVarianceCalculator` SUBCONTRACT 差异类型 successor，5 类差异未含 SUBCONTRACT）~~（**已收口，见 plan 2026-07-14-0035-1 实现注记**：`ProductionVarianceCalculator` 第 6 类差异 SUBCONTRACT + `ProductionVarianceDispatcher` 第 4 要素桶 + 1416/1417 科目对已落地）。

## 实现注记（计划 `2026-07-14-0035-1`）

本节承接 `0455-2` Deferred「subcontract 委外差异」，触发条件「委外差异分析业务需求落地」已满足（委外引擎 0455-1 + 委外费归集 0455-2 已落地，标准侧 `ErpMfgCostRollupLine.subcontractCost` 与实际侧 `ErpMfgWorkOrder.subcontractCost` 列均就位，差异引擎此前从未消费）。

- **第 6 类差异 SUBCONTRACT（`ProductionVarianceCalculator`）**：标准 = `rollupLine.subcontractCost × 完工量`；实际 = `wo.subcontractCost`；`costElement = SUBCONTRACT` / `varianceType = SUBCONTRACT`。沿用既有「零差异不生成行」范式——仅当标准侧或实际侧 subcontractCost 非零时生成行，两侧均为零时跳过（不污染既有 5 类差异输出）。
- **字典 + 常量**：`erp-mfg/variance-type` 字典补 `SUBCONTRACT`（委外费差异）码；`ErpMfgConstants.VARIANCE_TYPE_SUBCONTRACT` 同步。
- **过账第 4 要素桶（`ProductionVarianceDispatcher`）**：按 `costElement=SUBCONTRACT` 聚合净差异，组装进 `PRODUCTION_VARIANCE` PostingEvent。`ProductionVarianceAcctDocProvider` 新增科目对 1416（制造差异-委外）/ 1417（在制品-委外），方向与既有 3 要素一致（unfavorable Dr 差异/Cr 在制品，favorable Dr 在制品/Cr 差异）。

## 成本核算方法

### 方法类型

| 方法 | 编码 | 说明 | 适用场景 |
|------|------|------|----------|
| 移动加权平均法 | MOVING_AVERAGE | 每次入库后重新计算加权平均成本 | 价格波动较小 |
| FIFO（先进先出） | FIFO | 按入库顺序出库，先入库先出库 | 保质期管理、批次追溯 |
| 批次成本法 | BATCH | 按特定批次跟踪成本 | 高价商品、定制产品 |
| 标准成本法 | STANDARD | 使用预设标准成本 | 成本控制、差异分析 |
| 个别计价法 | INDIVIDUAL | 每次出库指定具体入库批次的成本 | 贵重物品、唯一标识商品 |

> 默认最低价策略：当多种计价方法计算结果差异较大时，取较低值作为保守估计（可配置关闭）。折扣叠加规则：折扣在源币种金额上扣减后再按汇率转换本位币。

### 方法配置

成本核算方法在会计科目表层面配置：

```xml
<entity name="ErpMdAcctSchema">
    <column name="costingMethod" dict="erp-md/cost-method" mandatory="true"/>
    <column name="costType" dict="erp/cost-type" mandatory="true"/>
</entity>

<dict name="erp-md/cost-method">
    <option value="MOVING_AVERAGE" label="移动加权平均"/>
    <option value="FIFO" label="先进先出"/>
    <option value="BATCH" label="批次成本"/>
    <option value="STANDARD" label="标准成本"/>
    <option value="INDIVIDUAL" label="个别计价"/>
</dict>
```

## 移动加权平均法

### 计算公式

```
移动加权平均成本计算
        │
        ├─► 公式：
        │      加权平均单位成本 = (期初金额 + 本期入库金额) / (期初数量 + 本期入库数量)
        │
        ├─► 示例：
        │      期初：数量100，金额1000，单位成本10
        │      入库：数量50，金额600，单位成本12
        │      加权平均单位成本 = (1000 + 600) / (100 + 50) = 1600 / 150 = 10.67
        │
        └─► 出库成本：
               出库成本 = 出库数量 × 加权平均单位成本
```

### 计算时机

每次入库移动单完成时重新计算：

```
入库触发成本重算
        │
        ├─► 入库移动单完成
        │      ├─ 获取入库数量、入库金额
        │      ├─ 获取当前库存余额（数量、金额）
        │      └─ 计算新的加权平均成本
        │
        ├─► 更新库存余额
        │      ├─ 新数量 = 当前数量 + 入库数量
        │      ├─ 新金额 = 当前金额 + 入库金额
        │      └─ 新单位成本 = 新金额 / 新数量
        │
        └─► 更新库存流水
               ├─ 入库流水的 unitCost = 入库单价
               └─ 入库流水的 stockValue = 入库数量 × 入库单价
```

### 出库成本计算

```
出库成本计算
        │
        ├─► 获取当前加权平均单位成本
        │      └─ 从库存余额表读取
        │
        ├─► 计算出库成本
        │      ├─ 出库成本 = 出库数量 × 当前单位成本
        │      └─ 记录到出库流水
        │
        └─► 更新库存余额
               ├─ 新数量 = 当前数量 - 出库数量
               ├─ 新金额 = 当前金额 - 出库成本
               └─ 单位成本不变（移动加权平均）
```

## FIFO（先进先出）

### FIFO 队列

库存余额表维护 FIFO 队列：

```
FIFO 队列结构
        │
        ├─► 队列记录（StockQueue）
        │      ├─ queueId
        │      ├─ materialId
        │      ├─ warehouseId
        │      ├─ moveId（入库移动单ID）
        │      ├─ moveLineId（入库移动单行ID）
        │      ├─ batchId（批次ID）
        │      ├─ incomingQty（入库数量）
        │      ├─ outgoingQty（已出库数量）
        │      ├─ remainingQty（剩余数量）
        │      ├─ unitCost（入库单价）
        │      ├─ totalCost（入库总成本）
        │      └─ incomingDate（入库日期）
        │
        └─► 队列规则
               ├─ 按 incomingDate 排序（最早的在前）
               ├─ 出库时从最早队列开始消耗
               └─ 队列消耗完毕后删除
```

### FIFO 出库逻辑

```
FIFO 出库逻辑
        │
        ├─► 步骤1：查询 FIFO 队列
        │      ├─ 按 incomingDate 升序排序
        │      └─ 只查询 remainingQty > 0 的队列
        │
        ├─► 步骤2：从最早队列开始消耗
        │      ├─ 队列1：remainingQty = 50，unitCost = 10
        │      ├─ 出库数量 = 30
        │      ├─ 从队列1消耗30，remainingQty = 20
        │      └─ 出库成本 = 30 × 10 = 300
        │
        ├─► 步骤3：队列不足时继续下一队列
        │      ├─ 出库数量 = 60（超过队列1剩余）
        │      ├─ 队列1消耗20，成本 = 20 × 10 = 200
        │      ├─ 队列2消耗40，成本 = 40 × 12 = 480
        │      └─ 总出库成本 = 200 + 480 = 680
        │
        └─► 步骤4：更新队列和流水
               ├─ 更新队列 remainingQty
               ├─ 删除 remainingQty = 0 的队列
               └─ 记录出库流水的 unitCost（加权平均）
```

### FIFO 成本追溯

```
FIFO 成本追溯
        │
        ├─► 从出库流水追溯到入库队列
        │      ├─ 出库流水记录消耗的队列ID
        │      └─ 可追溯到具体入库移动单
        │
        └─► 从入库队列追溯到采购订单
               ├─ 队列记录 moveId
               └─ moveId → sourceBillId → 采购订单
```

## 批次成本法

### 批次成本

每个批次独立记录成本：

```
批次成本结构
        │
        ├─► 批次台账（Batch）
        │      ├─ batchId
        │      ├─ materialId
        │      ├─ batchNo
        │      ├─ productionDate
        │      ├─ expirationDate
        │      ├─ incomingQty（入库数量）
        │      ├─ outgoingQty（已出库数量）
        │      ├─ remainingQty（剩余数量）
        │      ├─ unitCost（批次单位成本）
        │      └─ totalCost（批次总成本）
        │
        └─► 批次成本规则
               ├─ 每个批次独立成本
               ├─ 出库时指定批次
               └─ 出库成本 = 出库数量 × 批次单位成本
```

### 批次出库逻辑

```
批次出库逻辑
        │
        ├─► 步骤1：选择出库批次
        │      ├─ 手工指定批次
        │      ├─ 或按效期规则自动选择（最早效期优先）
        │      └─ 校验批次剩余数量
        │
        ├─► 步骤2：计算出库成本
        │      ├─ 出库成本 = 出库数量 × 批次单位成本
        │      └─ 记录到出库流水
        │
        └─► 步骤3：更新批次台账
               ├─ outgoingQty += 出库数量
               ├─ remainingQty -= 出库数量
               └─ totalCost -= 出库成本
```

## 个别计价法（INDIVIDUAL）

### 适用场景

贵重物品（珠宝、贵金属）、唯一标识商品（艺术品、定制设备）、或需要精确追溯每笔出入库成本的场景。

### 出库成本计算

```
出库成本计算
        │
        ├─► 出库时指定具体入库批次
        │      ├─ 从出库单行获取 referencedMoveId（引用的入库移动单ID）
        │      └─ 查询该入库批次的 unitCost
        │
        ├─► 计算出库成本
        │      ├─ 出库成本 = 出库数量 × 指定批次的 unitCost
        │      └─ 记录到出库流水
        │
        └─► 更新库存余额
               ├─ 新数量 = 当前数量 - 出库数量
               ├─ 新金额 = 当前金额 - 出库成本
               └─ 更新对应批次的 remainingQty
```

### 与其他方法的区别

| 特性 | 个别计价 | FIFO | 移动加权平均 |
|------|---------|------|-------------|
| 出库成本来源 | 指定批次 | 自动取最早批次 | 加权平均 |
| 需要批次管理 | 是 | 是 | 否 |
| 出库时需指定引用 | 是 | 否 | 否 |
| 成本追溯精度 | 最高 | 高 | 中 |

## 到岸成本（Landed Cost）

### 到岸成本定义

到岸成本是采购商品到达仓库的总成本，包括：

| 成本要素 | 说明 |
|----------|------|
| 采购价格 | 商品采购单价 |
| 运费 | 运输费用 |
| 保险费 | 运输保险 |
| 关税 | 进口关税 |
| 清关费 | 清关手续费 |
| 其他费用 | 其他到岸费用 |

### 到岸成本分摊

```
到岸成本分摊
        │
        ├─► 步骤1：录入到岸成本单
        │      ├─ 关联采购入库单
        │      ├─ 录入各项费用
        │      └─ 按金额/数量/重量分摊
        │
        ├─► 步骤2：分摊计算
        │      ├─ 按入库金额比例分摊
        │      ├─ 按入库数量比例分摊
        │      └─ 按重量比例分摊
        │
        ├─► 步骤3：更新入库成本
        │      ├─ 入库行成本 += 分摊费用
        │      └─ 更新库存余额成本
        │
        └─► 步骤4：生成凭证
               ├─ 借：存货（到岸成本）
               └─ 贷：应付账款（运费等）
```

### 到岸成本分摊示例

```
到岸成本分摊示例
        │
        ├─► 采购入库单
        │      ├─ 入库行1：物料A，数量100，采购金额1000
        │      ├─ 入库行2：物料B，数量50，采购金额500
        │      └─ 总采购金额：1500
        │
        ├─► 到岸成本
        │      ├─ 运费：150
        │      ├─ 保险费：30
        │      └─ 总到岸成本：180
        │
        ├─► 按金额比例分摊
        │      ├─ 物料A分摊 = 180 × (1000/1500) = 120
        │      ├─ 物料B分摊 = 180 × (500/1500) = 60
        │      └─ 物料A新成本 = 1000 + 120 = 1120
        │      └─ 物料B新成本 = 500 + 60 = 560
        │
        └─► 单位成本更新
               ├─ 物料A：1120 / 100 = 11.20
               └─ 物料B：560 / 50 = 11.20
```

## 成本调整

### 成本调整场景

| 场景 | 说明 |
|------|------|
| 采购价格调整 | 供应商调整价格，需调整已入库成本 |
| 到岸成本补录 | 后续补充录入到岸成本 |
| 成本差异调整 | 发现成本计算错误 |
| 标准成本调整 | 标准成本法下的标准成本更新 |

### 成本调整流程

```
成本调整流程
        │
        ├─► 步骤1：创建成本调整单
        │      ├─ 选择调整物料/批次
        │      ├─ 录入原成本、新成本
        │      └─ 录入调整原因
        │
        ├─► 步骤2：审核调整单
        │      └─ 校验调整合理性
        │
        ├─► 步骤3：执行成本调整
        │      ├─ 更新库存余额成本
        │      ├─ 更新批次成本（如适用）
        │      └─ 记录成本调整流水
        │
        └─► 步骤4：生成调整凭证
               ├─ 借：存货（成本增加）
               └─ 贷：成本差异（或相反）
```

### 成本调整记录

```xml
<entity name="ErpInvCostAdjust">
    <column name="materialId" type="Long" mandatory="true"/>
    <column name="batchId" type="Long"/>
    <column name="warehouseId" type="Long" mandatory="true"/>
    <column name="oldUnitCost" type="Decimal" mandatory="true"/>
    <column name="newUnitCost" type="Decimal" mandatory="true"/>
    <column name="adjustQty" type="Decimal" mandatory="true"/>
    <column name="adjustAmount" type="Decimal" mandatory="true"/>
    <column name="adjustReason" type="String" length="200"/>
    <column name="docStatus" dict="erp/doc-status"/>
</entity>
```

### 实现注记（plan 2026-07-05-2352-3）

成本调整已落地，与上文设计草稿的偏离记录如下（权威实现 = `<domain>/model/*.orm.xml` + BizModel）：

- **实体结构（Decision）**：采用头-行（`ErpInvCostAdjust` 头 + `ErpInvCostAdjustLine` 行），而非上文草稿的单层结构——成本调整常涉及多物料/多仓库一次调整，头-行 cascade 范式与全域头-行单据（采购订单/退货单）一致。头携带 code/adjustType(dict `erp-inv/adjust-type`)/docStatus/approveStatus/posted + 审计列；行携带 materialId/warehouseId/batchNo/oldUnitCost/newUnitCost/adjustQty/adjustAmount/adjustReason。
- **adjustType 字典**：`PURCHASE_PRICE_ADJUST`（采购价格调整）/`COST_DIFFERENCE`（成本差异）/`STANDARD_REVALUATION`（标准成本重估）/`LANDED_COST_SUPPLEMENT`（到岸成本补录，本期 Non-Goal 预留码值）。
- **纯成本变更（Non-Goal 边界）**：成本调整不生成 `ErpInvStockMove`（数量不变），仅更新 `ErpInvStockBalance.avgCost/totalCost` + 写 `ErpInvStockLedger` 流水（quantity=0，moveId=0 哨兵标识非移动单来源）+ 按计价方法处理成本层 + 过账。
- **FIFO 层处理（Decision a）**：FIFO 物料追加「delta 调整层」（`ErpInvCostLayer`，unitCost=新旧单位成本差，incomingMoveId=-行ID 负值哨兵区别于正常移动单正 ID），保持 FIFO 队列先进先出不变量；reverse 据此精确删除。残留风险：FIFO 队列长度增长。
- **标准成本重估（Decision a）**：`adjustType=STANDARD_REVALUATION` 时 apply 创建新 `ErpMfgCostRollup`(FIRMED, materialId 采购件, newUnitCost) 行，后续 `StandardCostResolver` 读最新 FIRMED。制造件标准成本重估归制造域 `rollupCost` successor（本期不覆盖）。
- **审批门控**：`erp-fin.cost-adjust-approval=true`（默认）时 `applyCostAdjust` 前置 approveStatus=APPROVED（DIRECT 审批状态机标准 5 action）；config 关闭时允许 UNSUBMITTED 直接 apply。
- **过账**：`CostAdjustmentAcctDocProvider`（业务类型 `COST_ADJUSTMENT`）方向相关——成本增加 借存货(1401)/贷成本差异(6603)；成本减少 借成本差异(6603)/贷存货(1401)。金额 = Σ 行 adjustAmount（带符号判定方向）。
- **reverse 红冲**：`reverseCostAdjust` 回退余额/层至 oldUnitCost + 红字凭证（`IErpFinVoucherBiz.reverse`）。

## 成本核算与凭证

### 存货估值凭证

入库移动单完成触发存货估值凭证：

```
存货估值凭证生成
        │
        ├─► 入库移动单完成
        │      ├─ 获取入库成本
        │      └─ 触发凭证生成
        │
        ├─► 凭证模板匹配
        │      ├─ 模板：STOCK_INPUT
        │      ├─ 借：存货科目
        │      └─ 贷：暂估应付（采购未开票）或应付账款（已开票）
        │
        └─► 凭证生成
               ├─ 金额 = 入库数量 × 入库单位成本
               └─ 关联移动单（业财回链）
```

### 成本结转凭证

销售出库触发成本结转凭证：

```
成本结转凭证生成
        │
        ├─► 销售出库移动单完成
        │      ├─ 获取出库成本
        │      └─ 触发凭证生成
        │
        ├─► 凭证模板匹配
        │      ├─ 模板：STOCK_OUTPUT_SALES
        │      ├─ 借：主营业务成本
        │      └─ 贷：存货
        │
        └─► 凭证生成
               ├─ 金额 = 出库数量 × 出库单位成本
               └─ 关联移动单（业财回链）
```

## 成本报表

### 成本报表清单

| 报表 | 说明 |
|------|------|
| 存货成本明细表 | 按物料展示当前成本 |
| 成本变动明细表 | 成本变动历史记录 |
| FIFO 队列报表 | FIFO 队列状态 |
| 批次成本报表 | 批次成本明细 |
| 成本差异分析表 | 标准成本与实际成本差异 |

### 成本追溯查询

```java
/**
 * 查询物料成本历史
 */
public List<CostHistory> findCostHistory(Long materialId, Date fromDate, Date toDate) {
    // 查询库存流水，获取成本变化记录
    return stockLedgerDao.findCostChanges(materialId, fromDate, toDate);
}

/**
 * 查询入库成本来源
 */
public BigDecimal findIncomingCost(Long moveId) {
    // 从凭证追溯到移动单，获取入库成本
    FinVoucherLine line = voucherLineDao.findBySourceBill("STOCK_MOVE", moveId, "DR");
    return line != null ? line.getDrAmount() : null;
}
```

## 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-fin.landed-cost-enabled` | true | 是否启用到岸成本 |
| `erp-fin.cost-adjust-approval` | true | 成本调整是否需要审批 |
| `erp-fin.fifo-expiry-priority` | true | FIFO 是否优先按效期出库 |

## 开源参考

| 项目 | 参考维度 | 具体借鉴 |
|------|----------|----------|
| iDempiere | 多成本核算方法 | CostingMethod（A/F/I） |
| iDempiere | 到岸成本字段 | CurrentCostPriceLL |
| Odoo | FIFO 队列 | stock_queue 字段 |
| ERPNext | FIFO 队列实现 | stock_ledger_entry.json 的 stock_queue |
| 赤龙 | 库存流水成本 | InvStock.stockNumber 正负数明细 |

## 子计算器注入模式（D3）

> 来源：`docs/plans/2026-07-21-2225-2-costing-sub-calculator-injection-doc.md`（D3 文档化）；权威实现 = `module-inventory/erp-inv-service` 代码。本节抽象既有「策略 + 注入器 + resolver + context」四要素为可复用范式，文档化「何时选用」+「如何新增策略」+「同型/异型分类裁决」+「反模式自检表」。**不引入新代码**（D3 = 纯文档扩展）。

### 1. 模式定义

**子计算器注入模式（Sub-calculator Injection Pattern）**：把同一操作的多种算法实现各自封装为独立的 Strategy Bean，由统一的**注入器/分派器**在启动期收集全部 Strategy 实例建立 registry，运行期按数据/配置携带的**分派键**路由到对应 Strategy，所有 Strategy 经共享 **Context** 访问记账基础设施、写**统一输出通道**。该模式由四要素组成：

```
                                子计算器注入模式四要素
        ┌──────────────────────────────────────────────────────────────────┐
        │                                                                  │
        │  (1) Strategy 接口         ──→  多 Algorithm 实现                 │
        │      costMethod()               (MOVING_AVERAGE/FIFO/...)        │
        │      onIncoming/onOutgoing                                         │
        │                  ▲                                                 │
        │                  │ @Inject + 启动期 register                       │
        │                  │                                                 │
        │  (2) 注入器/分派器  ──────→  Map<key, Strategy> registry           │
        │      bookCompletion               @PostConstruct initRegistry      │
        │      resolveStrategy(method)      按键 O(1) 路由                   │
        │                  ▲                                                 │
        │                  │ resolve(line, acctSchemaId)                    │
        │                  │                                                 │
        │  (3) Resolver              ──→  解析链 + 兜底总开关                 │
        │      Material.costMethod           erp-inv.costing-enabled        │
        │      → AcctSchema.costingMethod    erp-inv.default-cost-method    │
        │      → config default                                               │
        │                                                                      │
        │  (4) Context（共享记账上下文）—— Strategy 经此访问基础设施           │
        │      upsertBalance / writeLedger / recomputeAvailable              │
        │      updateBalanceWithRetry / daoProvider / ormTemplate            │
        │                                                                      │
        │  统一输出通道：策略只写 ErpInvStockLedger.unitCost/totalCost       │
        │  既有 InvPostingDispatcher 读 ledger.totalCost 零改动拾取 COGS     │
        └──────────────────────────────────────────────────────────────────┘
```

抽象定义：

| 要素 | 角色 | 仓库实例 |
|------|------|---------|
| **Strategy 接口** | 算法契约；每个实现自描述分派键 + 实现该键对应的算法 | `CostingStrategy`（`costing/CostingStrategy.java:18`）|
| **注入器/分派器** | 持有全部 Strategy Bean；启动期建 registry；运行期按键分派 | `StockMoveBookkeeper`（`stock/StockMoveBookkeeper.java:56`）|
| **Resolver** | 按数据/配置解析分派键；提供兜底与总开关 | `CostMethodResolver`（`costing/CostMethodResolver.java:22`）|
| **Context** | 共享记账基础设施接口；策略经此访问余额/流水/dao，避免循环耦合 | `BookingContext`（`costing/BookingContext.java:20`，由 `StockMoveBookkeeper` 实现）|

### 2. 结构组成

逐一说明四要素的实现细节（权威 = 代码）：

**(2.1) Strategy 接口（`CostingStrategy.java:18`，38 行）** —— 3 方法：
- `String costMethod()`（`:21`）—— 自描述分派键，对应字典 `erp-md/cost-method` 的码值。**由实现者声明**，registry 据此入键，避免分派器手写 `if/switch` 链。
- `BigDecimal onIncoming(move, line, acctSchemaId, unitCost, ctx)`（`:29-30`）—— 入库记账：维护成本层（FIFO/BATCH/LIFO/SPECIFIC）/ 重算 avgCost（MOVING_AVERAGE/WEIGHTED_AVERAGE）+ 更新余额 + 写不可变流水；返回实际记入流水的单位成本。
- `BigDecimal onOutgoing(move, line, acctSchemaId, ctx)`（`:37`）—— 出库记账：消耗成本层 / 取 avgCost + 扣减余额 + 写不可变流水；返回实际记入流水的单位成本。

**当前 7 个实现**（每个 `costMethod()` 返回 `ErpInvConstants.COST_METHOD_*` 常量；常量值见 `ErpInvConstants.java:59-65`）：

| Strategy 类 | `costMethod()` 返回 | 常量实际值 | 测试类 |
|------------|---------------------|-----------|--------|
| `MovingAverageCostingStrategy`（`:31-32`）| `COST_METHOD_MOVING_AVERAGE` | `MOVING_AVERAGE` | （基线，被 `TestErpInvCostingDispatch` 与全部 E2E 套件隐式覆盖）|
| `WeightedAverageCostingStrategy`（`:37-38`）| `COST_METHOD_MONTHLY_WEIGHTED_AVERAGE` | `WEIGHTED_AVERAGE` | `TestErpInvWeightedAverageCosting` |
| `FifoCostingStrategy`（`:56-57`）| `COST_METHOD_FIFO` | `FIFO` | `TestErpInvFifoCosting` + `TestErpInvFifoCostingEndToEnd` |
| `LifoCostingStrategy`（`:48-49`）| `COST_METHOD_LIFO` | `LIFO` | `TestErpInvLifoCosting` |
| `StandardCostingStrategy`（`:32-33`）| `COST_METHOD_STANDARD` | `STANDARD` | `TestErpInvStandardCosting` |
| `SpecificCostingStrategy`（`:48-49`）| `COST_METHOD_INDIVIDUAL` | `SPECIFIC` | `TestErpInvSpecificCosting` |
| `BatchCostingStrategy`（`:53-54`）| `COST_METHOD_BATCH` | `BATCH` | `TestErpInvBatchCosting` |

> **字典 vs 常量值注意**：常量 `COST_METHOD_INDIVIDUAL = "SPECIFIC"`（个别计价法），常量 `COST_METHOD_MONTHLY_WEIGHTED_AVERAGE = "WEIGHTED_AVERAGE"`（全月一次加权平均）。新增策略时字典码值必须与常量字面量严格一致，否则 resolver `isSupported` 不识别。

**(2.2) 注入器/分派器（`StockMoveBookkeeper.java:56-128`，322 行）**：
- `@Inject` 字段注入全部 7 策略 + `CostMethodResolver`（`:58-86`，8 个 `@Inject`）。
- `Map<String, CostingStrategy> strategyByMethod = new HashMap<>()`（`:88`）—— registry。
- `@PostConstruct initStrategyRegistry()`（`:90-99`）—— 启动期逐个调 `register(strategy)`（`:101-103`，`strategyByMethod.put(strategy.costMethod(), strategy)`）建 registry。
- `bookCompletion(move, lines, acctSchemaId)`（`:109-123`）—— 入口分派器：每行经 `costMethodResolver.resolve(line, acctSchemaId)`（`:111`）取键 → `resolveStrategy(method)`（`:125-128`，registry 查 + null 回退移动加权平均）→ 按 `moveType`（INCOMING/OUTGOING/INTERNAL_TRANSFER）调对应 `onIncoming`/`onOutgoing`。

**(2.3) Resolver（`CostMethodResolver.java:22-85`，85 行）**：
- `resolve(line, acctSchemaId)`（`:32-44`）—— 解析链（短路首个非空且 supported）：
  1. `costing-enabled=false` → 立即返回 `MOVING_AVERAGE`（**总开关兜底**，`:33-35`）
  2. `readMaterialCostMethod(materialId)` → 读 `ErpMdMaterial.costMethod`（`:36`/`:57-64`）
  3. `acctSchemaId != null` → `readAcctSchemaCostingMethod` → 读 `ErpMdAcctSchema.costingMethod`（`:37-39`/`:66-73`）
  4. `method == null || !isSupported(method)` → `defaultCostMethod()` → 读 config `erp-inv.default-cost-method`，默认 `MOVING_AVERAGE`（`:40-43`/`:80-84`）
- `isSupported(method)`（`:47-55`）—— 7 码值白名单，**未识别码值回退默认**（避免记账中断，successor 接管时改策略分派表即可）。
- `isCostingEnabled()`（`:75-78`）—— 读 `ErpInvConstants.CONFIG_COSTING_ENABLED = "erp-inv.costing-enabled"`（`ErpInvConstants.java:31`），默认 true；置 false 时**一律回退移动加权平均**（向后兼容总开关，对齐既有硬编码行为）。

**(2.4) Context（`BookingContext.java:20-52`，52 行）** —— 6 方法接口，由 `StockMoveBookkeeper implements BookingContext`（`StockMoveBookkeeper.java:56`）实现，避免策略与记账器循环耦合：
- `upsertBalance(move, line, warehouseId, locationId)`（`:22-23`）—— 余额维度 upsert（物料 × 仓库 × 库位 × 批次 × owner）。
- `writeLedger(move, line, acctSchemaId, balance, warehouseId, locationId, signedQty, unitCost, signedTotalCost, costMethod)`（`:25-27`）—— 写不可变流水 `ErpInvStockLedger` + 结存快照。
- `recomputeAvailable(balance)`（`:29`）—— 重算 available = total − reserved − locked。
- `updateBalanceWithRetry(initialBaseline, applyDelta)`（`:46-47`）—— **乐观锁保护下的余额更新**（UC-INV-08 并发扣减加固，plan 2026-07-07-0024-2）：`applyDelta` 必须为纯函数，冲突时 evict + reload + 重试，重试上限 `erp-inv.concurrent-deduct-max-retry`（默认 5）。
- `daoProvider()`（`:49`）+ `ormTemplate()`（`:51`）—— 数据访问底层（策略内查 `ErpInvCostLayer` 等用）。

**(2.5) 统一输出通道（COGS 通道零改动）**：
- 策略**只写** `ErpInvStockLedger.unitCost/totalCost`（经 `BookingContext.writeLedger`）。
- 既有 `InvPostingDispatcher` 读 `ledger.getTotalCost()` 汇总为 `TOTAL_COST` 过账——**零改动拾取**，FIFO/STANDARD 等新策略的 COGS 经既有 SALES_OUTPUT/PURCHASE_INPUT 过账通道流动。
- 这是本模式的关键不变量：**新增策略不改下游过账分派器**。

### 3. 何时使用此模式

**适用判定矩阵**（三个条件同时满足时选用本模式；任一不满足请用 Processor/task.xml/聚合 helper 等替代）：

| # | 条件 | 本仓库正例 | 反例（不满足则不应用本模式）|
|---|------|-----------|------------------------|
| C1 | 同一操作有**多种算法**实现，且算法数量预期会增长（≥3）| 入库/出库记账有 7 种 costMethod | 单一算法（如 `CostRollupService.rollup` 是单一 BOM 卷算）→ 用聚合 helper，不用本模式 |
| C2 | 算法选择**由数据/配置驱动**（物料 costMethod / 账套 costingMethod / config），而非由调用方硬编码 | `ErpMdMaterial.costMethod` / `ErpMdAcctSchema.costingMethod` / config | 调用方在代码里显式选择算法 → 直接调具体类即可 |
| C3 | 所有算法需经**统一输出通道**（同一 ledger/同一过账 dispatcher），下游无需感知算法选择 | 7 策略均写 `ErpInvStockLedger`，`InvPostingDispatcher` 零改动 | 各算法输出形态不同（不同表/不同事件）→ 拆为独立 Provider + 各自过账通道 |

**与 iDempiere CostingMethod 的对照**：iDempiere 用 `MCost` + `MCostDetail` + `CostingMethod` enum + `CostingMethodFactory`（按 enum 查 strategy）实现同型模式；本仓库把 factory 简化为 `StockMoveBookkeeper.strategyByMethod` HashMap + `@PostConstruct` 装配，把 CostingMethod enum 简化为 String 码值（与字典 `erp-md/cost-method` 同源），更贴合 Nop 的字典驱动约定。

**与 Processor/task.xml 的边界**：本模式解决「同一操作的算法多态」，**不解决**「多步编排拓扑可变」。多步流程（如审批-触发-过账三段）用 Processor/task.xml；纯算法多态用本模式。两者正交：一个 Processor 的某一步可委派给本模式的分派器。

### 4. 如何新增一个策略

以「新增 INDIVIDUAL（个别计价）full 支持」为例（当前 `SpecificCostingStrategy` 已落地，此处仅作步骤示范）：

| 步骤 | 动作 | 文件位置 | 关键约束 |
|------|------|---------|---------|
| 1 | **实现 Strategy 接口** | `module-inventory/erp-inv-service/.../costing/XxxCostingStrategy.java` | `@Override costMethod()` 返回新码值常量；`onIncoming`/`onOutgoing` 经 `ctx.upsertBalance`/`ctx.writeLedger`/`ctx.updateBalanceWithRetry` 访问基础设施；**不要在策略内直接 `new ErpInvStockLedger()` 或 `daoProvider().daoFor(...)` 写库**——必须经 ctx，以保证乐观锁/审计/COGS 通道统一 |
| 2 | **声明常量** | `ErpInvConstants.java` | `String COST_METHOD_XXX = "XXX";` —— 字面量必须与字典 `erp-md/cost-method` 的 code 完全一致 |
| 3 | **字典扩码值** | `module-master-data/model/app-erp-master-data.orm.xml` 的 `<dict name="erp-md/cost-method">` | 追加 `<option value="XXX" label="..."/>`；ORM 变更触发 `mvn clean install -DskipTests` 增量重新生成 dict.yaml + DaoConstants |
| 4 | **Bean 注册（注入器）** | `module-inventory/erp-inv-service/.../beans/app-service.beans.xml` | `<bean id="...XxxCostingStrategy" class="...XxxCostingStrategy"/>`；同时在 `StockMoveBookkeeper` 增 `@Inject XxxCostingStrategy` 字段 + `initStrategyRegistry()` 内调 `register(xxxCostingStrategy)` |
| 5 | **Resolver 识别** | `CostMethodResolver.isSupported`（`:47-55`）| 增 `Objects.equals(method, ErpInvConstants.COST_METHOD_XXX)` 分支；未加则 resolver 走默认回退，策略永不分派（**静默失败**，详见反模式自检表 AP-04）|
| 6 | **测试范式（7 类 costMethod 各一）** | `module-inventory/erp-inv-service/src/test/.../TestErpInvXxxCosting.java` | 继承 `JunitAutoTestCase`；`@NopTestConfig(localDb=true, initDatabaseSchema=TRUE, enableActionAuth=FALSE)`；经 `IGraphQLExecutor` 触发移动单 DONE → 断言 `ErpInvStockLedger.unitCost/totalCost` + `ErpInvStockBalance.avgCost/totalCost` + `ErpInvCostLayer`（如适用）；至少覆盖正路径（入库+出库）+ 边界（余额 0 拒绝 / 红冲不变量）|
| 7 | **跨域解析验证（如适用）** | 若策略依赖其他域（如 STANDARD 依赖 mfg `CostRollupLine`）| 注入跨域 dao（经 `erp-mfg-dao` 编译期依赖）；config-gated 默认关，单域测试不依赖跨域 service 启动 |

> **新增策略的反模式自检**（执行步骤 1-7 后逐项确认，详见 §6）：
> - AP-01：是否在分派器里写了 `if/switch` 按码值分派？（应为 registry 自动入键）
> - AP-02：策略是否直接 `daoProvider().daoFor(ErpInvStockLedger.class).saveEntity(...)` 写库？（应经 `ctx.writeLedger`）
> - AP-03：策略是否绕过 resolver 在调用方硬编码 `costMethod`？（应让 resolver 决定）
> - AP-04：是否漏改 `CostMethodResolver.isSupported`？（漏改 = 策略永不分派 = 静默失败）
> - AP-05：是否漏写单测？（7 类 costMethod 各一，新增必须补）

### 5. 同型与异型分类裁决（Decision E）

> **Decision E（2026-07-21，Phase 1 落盘）**：经代码核实，仓库内「子计算器注入模式」的真正实例仅 2 个；另有 1 个反模式实例 + 1 个非分派 helper 必须显式区分以免误分类。所有结论附代码行号证据。

**同型实例（2 个，相同形状不同分派键）**：

| 实例 | 接口 | 分派键 | 注入器/分派器 | registry 形态 | 代码证据 |
|------|------|--------|--------------|--------------|----------|
| 存货成本计算 | `CostingStrategy`（`module-inventory/erp-inv-service/.../costing/CostingStrategy.java:18`）| `costMethod()` 自描述字符串（对应 `erp-md/cost-method` 码值）| `StockMoveBookkeeper`（`.../stock/StockMoveBookkeeper.java:56`）| `Map<String, CostingStrategy> strategyByMethod`（`StockMoveBookkeeper.java:88`），`@PostConstruct initStrategyRegistry()`（`:90-99`）逐个 `register(strategy)`（`:101-103`）按 `strategy.costMethod()` 入键 | `bookCompletion` 按 `costMethodResolver.resolve(line, acctSchemaId)`（`:111`）取键 → `resolveStrategy(method)`（`:125-128`）查 registry 分派 `onIncoming`/`onOutgoing` |
| 业财过账凭证生成 | `IErpFinAcctDocProvider`（`module-finance/erp-fin-service/.../posting/IErpFinAcctDocProvider.java:16`）| `getSupportedBusinessTypes()` 自描述 `Set<ErpFinBusinessType>`（`:21`）| `ErpFinAcctDocRegistry`（`.../posting/ErpFinAcctDocRegistry.java:28`）| `Map<ErpFinBusinessType, IErpFinAcctDocProvider> providerMap`（`:33-34`），`@PostConstruct init()`（`:45-79`）双层装配（非默认 Provider 优先 + 默认 Provider 仅填充空缺，冲突 fail-fast `:55-60`）| `getProvider(businessType)`（`:81-83`）O(1) 查 registry 分派 `createFacts` |

两实例**同型**的判定依据：均满足「接口 + 多实现 + 由实现自描述分派键（`costMethod()` / `getSupportedBusinessTypes()`）+ 注入器统一 `@Inject` 收集 + 启动期建 registry + 运行期按键 O(1) 分派」五要素。差异仅在分派键的值类型（String vs enum Set）与 registry 装配策略（后者多了 fail-fast 冲突裁决 + 默认 Provider 兜底）——这些是**同型内的实现变体**，不构成独立的模式分叉。

**反模式实例（1 个，应重构为 Strategy+registry）**：

| 实例 | 现状（代码证据）| 为什么是反模式 | 重构方向 |
|------|----------------|---------------|---------|
| `CostAdjustmentService`（`module-inventory/erp-inv-service/.../costing/CostAdjustmentService.java`，320 行）| 内联 `if (Objects.equals(costMethod, COST_METHOD_FIFO))` 分派成本层应用（`:108-112` 调 `applyFifo`/`applyAverageLike`）；内联 `if (Objects.equals(adjust.getAdjustType(), ADJUST_TYPE_STANDARD_REVALUATION))` 分派 rollup 发布（`:132`/`:189`/`:301`，3 处重复同表达式）| 用手写 `if/Objects.equals` 链做键分派，而非「接口 + registry」——新增 `adjustType` 或 `costMethod` 码值需多处改 `if` 链，扩展点散落；同表达式在 apply/reverse/resolveOldUnitCost 三处复制，违反 DRY | 抽 `CostAdjustApplier` 接口（`appliesTo(costMethod, adjustType)` + `apply`/`reverse`），各类型实现注册为 bean，`CostAdjustmentService` 持 `Map<Key, Applier>` registry。触发条件：业务方新增第 3 种 adjustType 或非 FIFO/AVERAGE 的 costMethod 调整路径 |

**非分派 helper（1 个，必须与分派模式区分）**：

| 实例 | 现状（代码证据）| 为什么不是分派模式 | 文档化目的 |
|------|----------------|---------------|-----------|
| `CostRollupService`（`module-manufacturing/erp-mfg-service/.../costing/CostRollupService.java`，379 行）| 单一递归 BOM 成本聚合服务（`rollup(bomId)` `:85-95` → `computeUnit` `:130-180` 记忆化递归）；`@Inject IDaoProvider` + `BomExpander`（`:69-72`），无接口/多实现拆分；overhead 模式选择用内联 `if/equals`（`:219` MACHINE_HOUR / `:222` LABOR_RATIO），仅是 helper 内部算法分支 | 无分派键（单一入口 `rollup(bomId)`）、无接口/多实现拆分、无 registry；`if/equals` 是聚合算法内部的策略选择（且仅 2 个 mode，config-gated 默认关），不是「按数据/配置选择算法」的多实现分派 | 防止后续维护者看到 `if/equals` 模式标签误分类为「分派模式反例」，浪费重构成本。这是聚合 helper，其 `if/equals` 是合法的内部算法选择 |

**对 owner doc 对照表的影响**：
- 同型表（§5）仅含 2 实例：`CostingStrategy` vs `IErpFinAcctDocProvider`。
- 反模式表（§6）含 2 仓库内具体例证：`CostAdjustmentService`（应重构为 Strategy+registry）+ `CostRollupService`（聚合 helper，**勿**误分类为分派模式反例）。

### 6. 反模式自检表

新增或修改策略 / 分派器 / resolver 后，必须逐项确认下列 **6 项反模式均不存在**（≥5 项为 plan 退出条件，本节交付 6 项含 2 个仓库内具体例证）。每项附「反模式」「应为」「仓库内例证/出处」三栏。

| ID | 反模式 | 应为 | 仓库内例证 / 出处 |
|----|--------|------|------------------|
| **AP-01** | 在分派器/调用方用 `switch/case` 或 `if/Objects.equals` 链按分派键分派算法 | 由 `Map<key, Strategy>` registry + `@PostConstruct initRegistry()` 自动入键；新增策略只需 `register(strategy)` | ❌ **反例**：`CostAdjustmentService.applyLine:108` 用 `if (Objects.equals(costMethod, COST_METHOD_FIFO))` 分派成本层应用；`applyAverageLike:132` / `reverseLine:189` / `resolveOldUnitCost:301` 三处重复 `Objects.equals(adjust.getAdjustType(), ADJUST_TYPE_STANDARD_REVALUATION)` —— 新增 adjustType 或 costMethod 需改 ≥4 处 `if`。✅ **正例**：`StockMoveBookkeeper.bookCompletion:111` 经 `resolveStrategy(method)` 查 registry，零 `if/switch` |
| **AP-02** | 策略内直接 `daoProvider().daoFor(ErpInvStockLedger.class).saveEntity(...)` 写库（绕过 Context） | 经 `BookingContext.writeLedger(...)` 写流水，保证乐观锁 + 审计 + COGS 通道统一 | ✅ **正例**：`MovingAverageCostingStrategy` / `FifoCostingStrategy` 等全部 7 策略均经 `ctx.writeLedger` / `ctx.upsertBalance` / `ctx.updateBalanceWithRetry`，无直接 `daoFor(ErpInvStockLedger.class)` 写库（grep `costing/*CostingStrategy.java` 验证）|
| **AP-03** | 调用方绕过 resolver 硬编码 `costMethod`（如 `if (material.isFifo()) strategy.onOutgoing(...)`） | 让 `CostMethodResolver.resolve(line, acctSchemaId)` 唯一决定分派键；调用方只持有 resolver 返回的 String | ✅ **正例**：`StockMoveBookkeeper.bookCompletion:111` 只调 `costMethodResolver.resolve(line, acctSchemaId)`，不读 `material.costMethod`；移动单状态机/过账分派器均不感知 costMethod |
| **AP-04** | 新增策略后漏改 `CostMethodResolver.isSupported`（`:47-55`） | 同步在 `isSupported` 增 `Objects.equals(method, COST_METHOD_NEW)` 分支 | ⚠️ **静默失败风险**：漏改时 resolver 走 `defaultCostMethod()`（`:40-43`），策略永不被分派——`bookCompletion` 不抛异常但 ledger 用错成本法。这是本模式唯一的「编译期不报错、运行期静默走默认」陷阱，单测必须覆盖（见 AP-05）|
| **AP-05** | 新增策略后不补单测 | 至少 1 个集成测试（`JunitAutoTestCase` + `IGraphQLExecutor` 触发 DONE → 断言 ledger/balance/layer），覆盖正路径 + 边界（余额 0 拒绝 / 红冲不变量） | ✅ **正例**：7 类 costMethod 均有独立测试类（`TestErpInvWeightedAverageCosting` / `TestErpInvFifoCosting` + `TestErpInvFifoCostingEndToEnd` / `TestErpInvLifoCosting` / `TestErpInvStandardCosting` / `TestErpInvSpecificCosting` / `TestErpInvBatchCosting`，外加 `TestErpInvCostingDispatch` 验证 resolver 分派链 + 总开关兜底）|
| **AP-06** | 把**聚合 helper**（单一入口、无分派键、无接口/多实现拆分）误分类为分派模式并强行重构 | 区分「分派模式」（多算法 + 数据驱动 + 统一通道，C1+C2+C3 全满足）vs「聚合 helper」（单一算法、内部含 if/equals 仅是算法分支）；聚合 helper 的内联 `if/equals` 合法，不适用本模式 | ❌ **误分类反例**：`CostRollupService`（`module-manufacturing/erp-mfg-service/.../costing/CostRollupService.java`，379 行）—— 单一入口 `rollup(bomId)`（`:85`），无 Strategy 接口，`if/equals`（`:219`/`:222`）仅是 overhead 分配 mode 选择（且仅 2 个 mode，config-gated 默认关），**不是**分派模式。强行重构为 Strategy+registry 会增加 4 类无业务价值的样板代码 |

> **反模式自检纪律**：每新增/修改 1 个策略、分派器或 resolver 方法后，必须立即逐项核对 AP-01 至 AP-06，不能批量写完所有代码后统一自检（与 `nop-backend-dev` skill 的「自检纪律」一致）。

### 7. 落地证据

> 本节为 D3 文档化的落地记录。D3 = 纯文档扩展（roadmap §6 明确 `D3 ORM 变更 = 否 — 仅为文档扩展`），无新代码、无新测试基线。

- **Plan**：`docs/plans/2026-07-21-2225-2-costing-sub-calculator-injection-doc.md`（2 Phase 全 done：Phase 1 代码核对 + 章节骨架 + Decision E 同型分类裁决；Phase 2 7 小节正文 + roadmap 同步）
- **Owner Doc**：本节（`docs/design/finance/costing-methods.md §子计算器注入模式（D3）`），EXPAND 既有 8 节正文与各 plan 实现注记不动，仅追加新章节。
- **代码核对基线**（Phase 1 完成，Phase 2 复核一致）：
  - 接口签名：`CostingStrategy.java:18`（38 行，3 方法）
  - 策略清单：7 类（见 §2.1 表，`costMethod()` 返回值与 `ErpInvConstants.java:59-65` 严格一致）
  - 注入器/分派器：`StockMoveBookkeeper.java:56-128`（322 行，`@Inject` 7 策略 + `CostMethodResolver` + `Map<String, CostingStrategy> strategyByMethod` registry + `@PostConstruct initStrategyRegistry`）
  - Resolver 解析链：`CostMethodResolver.java:22-85`（Material.costMethod → AcctSchema.costingMethod → config default；`isSupported` 7 码值白名单；`costing-enabled=false` 总开关兜底）
  - Context：`BookingContext.java:20-52`（6 方法，由 `StockMoveBookkeeper implements`）
  - Bean 注册：`module-inventory/erp-inv-service/.../beans/app-service.beans.xml`（7 策略 + Bookkeeper + Resolver 各 1 bean，共 9 bean）
  - 测试范式：7 类 costMethod 各 1 独立测试类 + `TestErpInvCostingDispatch`（分派链）
- **纯文档无测试基线**：D3 不引入新代码，无新单测/E2E/visual smoke。Phase 1/2 验证 = `mvn clean install -DskipTests` BUILD SUCCESS（154 模块）保证既有代码无回归。
- **Deferred successor**：
  - 未完工策略实现（BATCH full / INDIVIDUAL full / LIFO full / WEIGHTED_AVERAGE full 月末结账）：本节 §4 提供新增路径，触发条件 = 业务方启用对应 costMethod 需求。
  - `CostAdjustmentService` 反模式重构为 Strategy+registry（AP-01 反例）：触发条件 = 业务方新增第 3 种 `adjustType` 或非 FIFO/AVERAGE 的 costMethod 调整路径，使 `if` 链扩展成本超过重构成本。
  - 同型模式推广：本模式可作为其他「多算法 + 数据驱动 + 统一输出」场景（如多币种汇兑计算、多税制税额计算）的参考范式，触发条件 = 出现 ≥3 算法且需 config 选择的具体需求。