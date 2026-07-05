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

**Non-Goal（计划 `1538-1` Deferred But Adjudicated，已裁定留后继）**：BATCH（批次成本）/ INDIVIDUAL（个别计价，需出库指定批次 + FEFO 效期路由 + `ErpInvCostLayer.batchNo` 维度）、~~STANDARD（标准成本，依赖制造域 cost rollup N=2 产出到 `ErpMfgCostRollupLine.unitCost`）~~（**已收口，见下文 plan 2026-07-05-0427-2 实现注记**）、~~成本调整单 + 成本差异凭证（采购价格调整/成本差异/标准成本重估）~~（**已收口，见 plan 2026-07-05-2352-3 实现注记**）、全月一次加权平均（dict 20）/ LIFO（dict 40）、到岸成本（Landed Cost）分摊算法（成本调整单已预留 `adjustType=LANDED_COST_SUPPLEMENT` 码值供 successor）、默认最低价/折扣叠加规则（取较低值的保守估计）、成本报表（存货成本明细/FIFO 队列/差异表，属 nop-report 报表面）、多账套并行成本、存货减值（成本与可变现净值孰低）。各 Non-Goal 均已命名 successor 触发条件，见计划 Deferred 章节。

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