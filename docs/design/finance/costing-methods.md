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

## 成本核算方法

### 方法类型

| 方法 | 编码 | 说明 | 适用场景 |
|------|------|------|----------|
| 移动加权平均法 | MOVING_AVERAGE | 每次入库后重新计算加权平均成本 | 价格波动较小 |
| FIFO（先进先出） | FIFO | 按入库顺序出库，先入库先出库 | 保质期管理、批次追溯 |
| 批次成本法 | BATCH | 按特定批次跟踪成本 | 高价商品、定制产品 |
| 标准成本法 | STANDARD | 使用预设标准成本 | 成本控制、差异分析 |

### 方法配置

成本核算方法在会计科目表层面配置：

```xml
<entity name="ErpFinAcctSchema">
    <column name="costingMethod" dict="erp/costing-method" mandatory="true"/>
    <column name="costType" dict="erp/cost-type" mandatory="true"/>
</entity>

<dict name="erp/costing-method">
    <option value="MOVING_AVERAGE" label="移动加权平均"/>
    <option value="FIFO" label="先进先出"/>
    <option value="BATCH" label="批次成本"/>
    <option value="STANDARD" label="标准成本"/>
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
| `erp-fin.costing-method` | MOVING_AVERAGE | 默认成本核算方法 |
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