# 销售退货详细流程

## 目的

说明销售退货的完整业务流程、状态机、与库存/财务域的协作、以及退货与红字发票的处理机制。

## 退货场景

### 退货触发场景

| 场景 | 说明 | 处理方式 |
|------|------|----------|
| 质量问题 | 客户收到货物后发现质量问题 | 退货退款或换货 |
| 数量不符 | 实际发货数量与订单不符 | 补发或退货 |
| 规格不符 | 物料规格/型号与订单不符 | 退货或换货 |
| 客户拒收 | 物流问题或客户拒收 | 退货入库 |
| 销售取消 | 订单取消后已发货 | 退货 |
| 发票错误 | 开具发票与实际不符 | 退货+重新开票 |

### 退货类型

| 类型 | 说明 | 影响 |
|------|------|------|
| 部分退货 | 退货数量小于原出库数量 | 仅冲销部分库存和应收 |
| 全额退货 | 退货数量等于原出库数量 | 冲销全部库存和应收 |
| 换货 | 退货同时重新发货 | 退货+新销售出库 |

> **实现注记（RC-R1.51 / P1-RC-025，UC-SAL-06 四断言）**：
> - **returnType 列**：`ErpSalReturn.returnType`（propId 29，dict `erp-sal/return-type` 两值 RETURN/EXCHANGE，defaultValue="RETURN"）——既有退货零行为变化（RETURN 类型既有 approve/posting/refund 链零改动，纯加性 ORM 变更）。
> - **换货触发点（D1 选项 A）**：退货审核（EXCHANGE 类型走既有 APPROVED 主路径：INCOMING 入库移动 + SALES_RETURN 过账 + 退款编排，断言①库存恢复天然成立）后，操作员显式调 `ErpSalReturn__generateExchangeDelivery(returnId, lines[], context)`（@BizMutation + per-mutation `ErpSalReturnGenerateExchangeDeliveryProcessor`）。换货商品/数量由操作员决策（L1「换发等值或不同货物」），`lines` 入参缺省复制退货行。
> - **双向关联（D2 选项 A + 断言④）**：两 FK 列互指——`ErpSalReturn.exchangeDeliveryId`（propId 30）+ `ErpSalDelivery.exchangeReturnId`（propId 29），同事务双写（生成时回写双方）。域内无 sourceBillType/sourceBillCode 字符串模式，FK 即关联契约。codegen 循环依赖防护：`ErpSalDelivery.exchangeReturn` to-one 标 `ignoreDepends="true"`（拓扑序剔除反向边，运行时关联保留）。
> - **换货出库单**：复制退货头（customer/warehouse/currency/businessDate）新建 `ErpSalDelivery`（DRAFT + UNSUBMITTED，code 前缀 `EX-`），行金额 = quantity × unitPrice（scale 4 HALF_UP）聚合头金额；走**既有出库状态机**（DRAFT→SUBMITTED→APPROVED，不自动审核）——审核经既有 `DeliveryStockMoveBuilder` 生成 OUTGOING 移动单（relatedBillType=ERP_SAL_DELIVERY + 新出库单 code，与退货 INCOMING 移动单键不冲突）扣库存（断言②运行时成立）。
> - **价差语义（D3 选项 A）**：头级口径 Δ = 换货出库单 totalAmountWithTax − 退货单 totalAmountWithTax——Δ>0 补差价开票（经既有 `IErpSalInvoiceBiz.save` 建 DRAFT 发票，code 前缀 `EXDIFF-`，remark 记录价差来源，操作员经既有发票审核流提交过账）；Δ<0 退款（复用 `ReturnRefundOrchestrator.orchestrateRefund` 既有 reverse-settlement 能力；**边界**：退货审核时已先行反转客户已核销发票，换货生成时点通常无已核销发票 → 该分支为退款兜底 + remark 审计记录）；Δ=0 无动作。价差金额 + 方向记录换货出库单 remark（审计可追溯）。
> - **守卫族 + 幂等（D4 选项 A）**：generateExchangeDelivery 前置守卫 = returnType==EXCHANGE 且已 APPROVED + 源出库已审核 + 期间 OPEN + 发票未核销（复用 R1.19 守卫族 helper）；`exchangeDeliveryId` 非空重复调用抛 `ERR_EXCHANGE_DELIVERY_ALREADY_GENERATED`（erp.err.sal.exchange-delivery-already-generated）幂等拒绝；非换货类型抛 `ERR_EXCHANGE_RETURN_TYPE_INVALID`。
> - **操作引导**：审核通过但未生成换货单的运营跟踪 = `ErpSalReturn.exchangeDeliveryId` 空/非空判定；重复调用需先作废/删除既有换货单再重新生成。
> - **多轮换货链**（一退货单 → 一换货出库单）与换货出库独立审批流属 Non-Goal（见 plan `2026-08-16-0904-2`）。

## 退货流程

### 退货流程总览

```
销售退货流程
        │
        ├─► 步骤1：创建退货单
        │      ├─ 手工创建退货单
        │      ├─ 或从质检不合格触发
        │      └─ 关联原销售出库单
        │
        ├─► 步骤2：填写退货信息
        │      ├─ 选择退货物料与数量
        │      ├─ 填写退货原因
        │      ├─ 选择退货仓库/库位
        │      └─ 关联原出库单行
        │
        ├─► 步骤3：提交审核
        │      └─ 状态：UNSUBMITTED → SUBMITTED
        │
        ├─步骤4：审核通过
        │      └─ 状态：SUBMITTED → APPROVED
        │
        ├─► 步骤5：库存入库
        │      ├─ 调用 IErpInvStockMoveBiz
        │      ├─ 生成入库移动单
        │      └─ 库存增加
        │
        ├─► 步骤6：应收红字凭证
        │      ├─ 销售退货触发红字发票（若已开票）
        │      ├─ 或冲减应收（若未开票）
        │      └─ 生成红字凭证
        │
        ├─► 步骤7：退款处理
        │      ├─ 已收款退款（核销收款单）
        │      └─ 未收款冲减应收
        │
        ├─► 步骤8：更新关联单据
        │      ├─ 更新原出库单未退货量
        │      ├─ 更新原销售订单未交货量
        │      └─ 更新应收余额
        │
        └─► 步骤9：完成退货
               └─ 状态：APPROVED → DONE
```

## 退货单状态机

### 三轴状态分离

销售退货单同样采用三轴状态分离：

| 状态轴 | 字段 | 说明 |
|--------|------|------|
| 单据状态 | docStatus | 草稿/生效/作废 |
| 审核状态 | approveStatus | 未提交/已提交/已审核/已驳回 |
| 退货状态 | returnStatus | 初始/部分退货/全额退货 |
| 退款状态 | refundStatus | 未退款/部分退款/已退款 |

> **实现说明**：`returnStatus`/`refundStatus` 两轴在 ORM 模型中**不存储为字段**。
> 「部分/全额退货」是源出库行累计退货进度的**派生视图**（按 `ErpSalDeliveryLine` 聚合已审核退货行 SUM 计算）；
> 「退款进度」是 AR 辅助账 `ErpFinArApItem` 的 open/reconciled 状态的**派生视图**。故实现复用现有 `docStatus`
> +`approveStatus` 两轴表达退货单生命周期（终态 = ACTIVE+APPROVED+`posted=true`），保持 implementation-only
> （无 ORM 保护区域变更）。残留风险：列表页无法直接按 returnStatus/refundStatus 筛选，触发条件满足时
> （退货/退款报表需高频筛选）再评估加 `ErpSalDeliveryLine.returnedQuantity` 冗余列 + 重新 codegen。

### 状态迁移

```
UNSUBMITTED
  ├─ 提交 → SUBMITTED
  │            ├─ 审核通过 → APPROVED
  │            │              ├─ [触发库存入库]
  │            │              ├─ [生成红字凭证]
  │            │              ├─ [处理退款]
  │            │              └─ [更新关联单据]
  │            └─ 驳回 → REJECTED
  │                          └─ 修改后重新提交
  └─ 作废 → CANCELLED
```

## 与库存域协作

### 退货入库

退货审核通过后调用库存域生成入库移动单：

```
退货单审核通过
        │
        ├─► 调用 IErpInvStockMoveBiz.generateReturnMove()
        │      ├─ 传入参数：
        │      │   ├─ sourceBillType = SALES_RETURN
        │      │   ├─ sourceBillId = 退货单ID
        │      │   ├─ materialId = 物料ID
        │      │   ├─ warehouseId = 退货仓库
        │      │   ├─ locationId = 退货库位
        │      │   ├─ quantity = 退货数量
        │      │   ├─ sourceDeliveryLineId = 原出库单行ID（追溯用）
        │      │   ├─ batchId = 原批次ID（若启用批次管理）
        │      │   └─ returnReason = 退货原因（影响入库批次处理）
        │      │
        │      └─ 返回：入库移动单ID
        │
        ├─► 生成入库移动单
        │      ├─ 方向：INCOMING（入库）
        │      ├─ 源单据类型：销售退货
        │      └─ 关联原出库单追溯链
        │
        └─► 更新库存
               ├─ 库存流水写入（数量为正）
               ├─ 库存余额增加
               ├─ 批次/序列号更新（如适用）
               └─ 入库成本按退货成本（可能不同于原出库成本）
```

### 退货成本处理

退货入库的成本可能有以下几种处理方式：

| 成本处理方式 | 说明 | 适用场景 |
|-------------|------|----------|
| 原出库成本 | 使用原销售出库的成本 | 成本不变 |
| 当前库存成本 | 使用退货入库时的库存成本 | 价格波动调整 |
| 退货协议价 | 按退货协议约定的成本 | 双方协商 |

> 实现注记（RC-R1.18 / P1-RC-026）：三策略由配置 `erp-sal.return-cost-method` 决定（默认 `original`）。`ReturnStockMoveBuilder.buildLines` 按配置分支设置库存移动单行 `unitCost`：`original` = 行 unitPrice（原出库成本）/ `current` = 库存域 `ErpInvStockBalance.avgCost`（按 materialId+warehouseId 查询；缺失回退 unitPrice + LOG.warn）/ `agreement` = 行 unitPrice（退货协议价语义）。`SalReturnPostingDispatcher.computeTotalCost` 经同一 `ReturnCostStrategyResolver` 同源消费，维持「库存 ledger totalCost 与 GL 凭证 TOTAL_COST 同源」不变量。非法配置值回退 `original`。

### 批次追溯

启用批次管理时，退货入库批次处理：

```
批次追溯与处理
        │
        ├─► 方案1：退回原批次
        │      ├─ 退货使用原出库的批次
        │      └─ 优先从原批次入库
        │
        ├─► 方案2：新批次入库
        │      ├─ 退货生成新批次
        │      ├─ 新批次成本按退货协议价
        │      └─ 适用于退回商品不再销售
        │
        └─方案3：批次混用
               ├─ 部分退回原批次
               └─ 部分生成新批次
```

## 与财务域协作

### 红字发票处理

退货与发票的交互有以下几种场景：

| 场景 | 说明 | 处理 |
|------|------|------|
| 未开票退货 | 发货后未开票即退货 | 仅冲减应收 |
| 已开票退货 | 已开具发票后退货 | 生成红字发票冲销 |
| 部分退货+发票 | 退货数量小于发票数量 | 部分红字发票 |

> **实现注记（RC-R1.17 运营代理条件分支，零过账核心变更）**：
> `SalReturnPostingDispatcher.tryPost` 在组装 SALES_RETURN 事件前检查源出库单 `delivery.posted`
> 作为暂估应收运营代理：`posted=true` ⇒ 视为暂估应收未清，维持 SALES_RETURN 冲减路径（credit memo）；
> `posted=false`（未暂估）⇒ 跳过事件构造（零凭证 / 零 ArApItem，下游 SalAcctDocProvider /
> ErpFinArApItemGenerator 零变更）。实仓无独立「暂估应收」凭证载体，故以 delivery.posted 近似——
> 残留风险：posted 与真实暂估应收状态存在运营近似偏差。

### 应收冲减

出库后未开票时，应收为暂估。退货冲减应收：

```
应收冲减流程
        │
        ├─► 查询原出库单的暂估应收
        │      └─ 出库单关联的应收凭证
        │
        ├─► 生成红字冲销凭证
        │      ├─ 凭证类型：红字（RED_FLUSH）
        │      ├─ 借：应收（原借方方向取反）
        │      ├─ 贷：主营业务收入（原贷方方向取反）
        │      ├─ 贷：销项税转出（如适用）
        │      └─ 关联原出库凭证
        │
        ├─► 更新出库单退货后应收余额
        │      └─ 未退货数量对应的应收
        │
        └─► 业财回链
               ├─ 新凭证关联原出库单
               └─ 标记为冲销凭证
```

### 红字发票处理

已开票情况下，退货生成红字发票：

```
红字发票流程
        │
        ├─► 创建红字发票单
        │      ├─ 关联原蓝字发票
        │      ├─ 金额取负
        │      ├─ 关联退货单
        │      └─ 原因代码（退货/折让/折扣）
        │
        ├─► 审核红字发票
        │      ├─ 状态：SUBMITTED → APPROVED
        │      └─ 触发凭证生成
        │
        ├─► 生成红字凭证
        │      ├─ 借：应收（红字，冲销原应收）
        │      ├─ 贷：主营业务收入（红字，冲销原收入）
        │      └─ 凭证关联红字发票
        │
        ├─► 更新蓝字发票状态
        │      ├─ 蓝字发票冲销标志 = true
        │      └─ 关联红字发票
        │
        └─► 更新客户应收余额
               └─ 余额 = 原余额 - 红字金额
```

### 凭证分录示例

#### 场景1：未开票退货（冲减暂估应收）

```
原出库凭证（暂估应收）：
  借：应收账款 11,300
  贷：主营业务收入 10,000
  贷：应交税费-销项税 1,300

退货红字凭证：
  借：主营业务收入 3,000 （红字）
  贷：应收账款 3,390 （红字）
  贷：应交税费-销项税 390 （红字）

结果：应收账款减少 3,390，收入和销项税相应冲减
```

#### 场景2：已开票退货（红字发票）

```
原发票凭证（确认应收）：
  借：应收账款 11,300
  贷：主营业务收入 10,000
  贷：应交税费-销项税 1,300

退货红字凭证：
  借：主营业务收入 5,000 （红字）
  借：应交税费-销项税 650 （红字）
  贷：应收账款 5,650 （红字）

结果：应收账款减少 5,650，收入和销项税相应冲减
```

## 退款处理

### 退款流程

```
退款处理流程
        │
        ├─► 场景1：已收款退款
        │      ├─ 查询原收款单
        │      ├─ 生成退款单（金额为负）
        │      ├─ 核销原收款单
        │      └─ 生成退款凭证
        │
        ├─► 场景2：未收款冲减
        │      ├─ 直接冲减应收余额
        │      └─ 生成应收冲销凭证
        │
        └─► 场景3：部分收款+部分退货
               ├─ 未收部分：冲减应收
               └─ 已收部分：生成退款
```

### 退款方式

| 退款方式 | 说明 | 适用场景 |
|---------|------|----------|
| 原路退回 | 退款至原收款账户 | 已收款退货 |
| 其他账户 | 退款至客户指定账户 | 原账户不可用 |
| 预收款抵扣 | 转为客户预收款 | 客户下次采购使用 |
| 现金退款 | 直接退还现金 | 小额退款 |

## 退货与关联单据

### 关联追溯

退货单需关联以下单据：

| 关联类型 | 关联单据 | 作用 |
|----------|----------|------|
| 源单关联 | 原销售出库单 | 追溯出库信息、批次 |
| 订单关联 | 原销售订单 | 更新未交货量 |
| 发票关联 | 原销售发票（如有） | 确定是否需要红字发票 |
| 收款关联 | 原收款单（如已收款） | 确定退款方式 |
| 凭证关联 | 原出库凭证/发票凭证 | 业财回链 |

### 未交货量更新

退货后更新原销售订单的未交货量：

```
销售订单未交货量更新
        │
        ├─► 原订单未交货量 = 原订单数量 - 原出库数量 + 退货数量
        │
        ├─► 示例：
        │      ├─ 订单数量：100
        │      ├─ 已出库数量：80
        │      ├─ 退货数量：15
        │      └─ 更新后未出库量 = 100 - 80 + 15 = 35
        │
        └─► 退货数量可重新形成新的发货需求
```

> **实现注记（RC-R1.16 方案 A，零 ORM 变更）**：`ErpSalReturnProcessor.updateUndeliveredQuantity`
> 在退货审核（approve/reverseApprove）末尾按退货行 `deliveryLineId → ErpSalDeliveryLine.orderLineId`
> 定位订单行，补写 `ErpSalOrderLine.deliveredQuantity` 的「毛口径」值（Σ APPROVED delivery-line qty
> by orderLineId，对齐 ReturnQtyValidator 聚合先例）。L1 公式 `未交货量 = quantity − deliveredQuantity +
> Σ退货量` 由读侧派生成立（退货量 Σ 从 APPROVED 退货行聚合，不在本方法写入）。幂等：按重新聚合重算。
> 与 P2-RC-019（deliveredQuantity 零 writer）同源联动——此处为首个写入口。

## 质量域协作

### 退货质检

销售退货可能触发质检：

```
退货质检流程
        │
        ├─► 退货单审核通过
        │
        ├─► 触发质检单创建
        │      ├─ 关联退货单
        │      ├─ 检验类型：出货退货（RETURN）
        │      └─ 质检模板（按物料配置）
        │
        ├─► 执行质检
        │      ├─ 外观检查
        │      ├─ 功能检查（如适用）
        │      └─ 判定：合格/不合格
        │
        └─► 质检结果处理
               ├─ 合格 → 正常入库
               ├─ 不合格 → 按不合格处理流程（NCR）
               └─ 让步接收 → 降级入库或报废
```

### NCR 触发

退货质检不合格时触发不符合项报告（NCR）：

```
不合格处理
        │
        ├─► 创建 NCR
        │      ├─ 不合格现象
        │      ├─ 不合格数量
        │      ├─ 影响分析
        │      └─ 关联退货单
        │
        ├─► NCR 评审
        │      ├─ 责任分析
        │      └─ 处理决定：退货/降级/报废
        │
        └─► 后续处理
               ├─ 退货处置：进一步与供应商协调
               ├─ 降级入库：调整库存价值
               └─ 报废处理：生成报废凭证
```

## 异常处理

### 退货数量限制

| 限制类型 | 说明 | 处理 |
|----------|------|------|
| 超额退货 | 退货数量 > 可退数量 | 拒绝，提示最大可退数量 |
| 已核销退货 | 发票已全额核销收款 | 需先撤回核销 |
| 已结账期间退货 | 退货期间已结账 | 拒绝，需先反结账 |
| 可用量不足 | 退货入库但仓库可用量限制 | 检查仓库容量配置 |

> 实现注记（RC-R1.19 / P1-RC-027 + P1-RC-028）：退货审核（`ErpSalReturnProcessor.validateBusinessRulesForApprove`）在源出库单审核守卫之后、数量守卫之前接入两个 pre-approve 守卫：
> - **已核销发票守卫**：经退货行 `deliveryLineId → ErpSalInvoiceLine.deliveryLineId → invoice` 链路（发票级，对齐 L1 字面「退货关联的发票」）查关联发票 `receivedStatus`，`RECEIVED`（完全核销）抛 `ERR_RETURN_INVOICE_SETTLED` 拒绝审核（提示先撤回核销）；`PARTIAL`/`OPEN` 放行（post-approve `ReturnRefundOrchestrator` 既有反向兜底承接客户级残余）。无关联发票跳过。
> - **期间 CLOSED 守卫**：按退货 `businessDate` 查对应会计期间 status（经 `ErpFinAccountingPeriod`，对齐 finance `resolveOpenPeriod` + assets `requirePeriodOpen` 严格语义），非 OPEN（含 `CLOSING`/`CLOSED`/`CLOSED_FINAL`/`NEVER_OPENED`）或无对应期间抛 `ERR_RETURN_PERIOD_CLOSED` 拒绝审核。过账侧 `ErpFinPostingProcessor.resolveOpenPeriod` 既有兜底保持，本守卫为审核侧显式前置拦截（体验层）。

### 批次处理异常

| 场景 | 处理 |
|------|------|
| 原批次已全部出库 | 允许选择其他批次或生成新批次 |
| 批次效期已过 | 提示批次效期问题，按配置处理 |
| 序列号追溯失败 | 提示序列号未找到，拒绝退货 |
| 退货批次与原批次不同 | 记录批次变更原因，更新成本 |

### 并发控制

- 同一出库单行并发退货：使用乐观锁防止超额退货
- 退货与收款并发：校验收款状态后再处理退货

## 业务规则

### 退货约束

1. **退货数量限制**：退货数量 ≤ 原出库未退货数量
2. **金额限制**：退货金额不能超过原出库金额
3. **期间限制**：退货应在合理的业务周期内完成
4. **状态限制**：原出库单必须已审核

### 仓库/库位规则

- 退货仓库默认为原出库仓库
- 允许选择其他退货仓库
- 退货库位按仓库配置默认
- 批次/序列号追溯原出库记录

### 审批规则

| 条件 | 审批要求 |
|------|----------|
| 普通退货 | 销售员提交 → 审核人审核 |
| 高额退货（>阈值） | 销售员提交 → 销售主管审核 |
| 已收款退货 | 需财务确认退款方式 |

## 与采购退货的区别

| 对比项 | 销售退货 | 采购退货 |
|--------|----------|----------|
| 库存方向 | 入库（库存增加） | 出库（库存减少） |
| 凭证方向 | 冲减应收 | 冲减应付 |
| 关联出库 | 关联销售出库 | 关联采购入库 |
| 批次追溯 | 追溯原销售批次 | 追溯原采购批次 |
| 退款处理 | 需要退款 | 收回货款 |
| 质量联动 | 可能触发 NCR | 可能触发供应商索赔 |

## 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-sal.return-qty-limit` | 出库未退货量 | 退货数量上限 |
| `erp-sal.return-period-days` | 30 | 退货有效期（天） |
| `erp-sal.auto-create-invoice` | false | 退货是否自动生成红字发票 |
| `erp-sal.return-approval-required` | true | 退货是否需要审核 |
| `erp-sal.return-reason-required` | true | 退货是否必须填写原因 |
| `erp-sal.return-quality-check` | true | 退货是否必须质检 |
| `erp-sal.refund-method` | original | 默认退款方式 |
| `erp-sal.return-cost-method` | original | 退货入库成本计算方式 |
