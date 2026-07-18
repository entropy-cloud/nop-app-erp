# 生产成本差异分析（Variance Analysis）

> 标准成本 vs 实际成本逐工单比较，按差异类型、工作中心、产品、期间进行分析。
> 参考：Odoo manufacturing cost analysis, SAP CO variance

## 业务目标

- 量化工单层生产成本偏差（标准 vs 实际）
- 差异类型分类：价格差异、用量差异、效率差异、产量差异
- 多维度报表：按工作中心、产品、期间、差异类型下钻
- 为成本控制提供数据驱动决策依据

## 差异分类

| 差异类型 | 公式 | 说明 |
|---------|------|------|
| **材料价格差异** | (实际单价 - 标准单价) × 实际用量 | 采购价格波动 |
| **材料用量差异** | (实际用量 - 标准用量) × 标准单价 | 超耗/节约 |
| **人工效率差异** | (实际工时 - 标准工时) × 标准费率 | 效率高低 |
| **人工费率差异** | (实际费率 - 标准费率) × 实际工时 | 工资变动 |
| **制造费用差异** | 实际制造费用 - 标准制造费用 | 间接费偏差 |
| **产量差异** | (实际产出 - 标准产出) × 标准单位成本 | 产出量影响 |
| **委外费差异** | 实际委外费 - 标准委外费 | 委外加工费偏差（plan 2026-07-14-0035-1） |

## 数据模型

### ErpMfgCostVariance（成本差异记录）

每条记录表示一个工单在某差异类别上的差异分析结果。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| workOrderId | BIGINT | 工单ID |
| lineNo | INT | 行号 |
| varianceType | VARCHAR(50) | 差异类型：MATERIAL_USAGE / LABOR_EFFICIENCY / LABOR_RATE / OVERHEAD / VOLUME / SUBCONTRACT（实现偏离补注：材料价格差异归 PPV 在采购入库捕获，本期材料段仅算用量差异避免重复计入；SUBCONTRACT 委外费差异 plan 2026-07-14-0035-1 落地） |
| costElement | VARCHAR(50) | 成本要素：MATERIAL / LABOR / OVERHEAD / SUBCONTRACT |
| materialId | BIGINT | 物料（如涉及） |
| operationId | BIGINT | 工序（如涉及） |
| standardAmount | DECIMAL(20,4) | 标准金额 |
| actualAmount | DECIMAL(20,4) | 实际金额 |
| varianceAmount | DECIMAL(20,4) | 差异金额（actual - standard） |
| variancePercent | DECIMAL(10,4) | 差异百分比 |
| standardQty | DECIMAL(20,4) | 标准数量 |
| actualQty | DECIMAL(20,4) | 实际数量 |
| standardPrice | DECIMAL(20,4) | 标准单价 |
| actualPrice | DECIMAL(20,4) | 实际单价 |
| workcenterId | BIGINT | 工作中心 |
| businessDate | DATE | 业务日期 |
| posted | BOOLEAN | 已过账标志 |
| remark | VARCHAR(1000) | 备注 |
| delVersion | BIGINT | 逻辑删除版本 |
| version | INT | 数据版本 |
| createdBy | VARCHAR(50) | 创建人 |
| createTime | TIMESTAMP | 创建时间 |
| updatedBy | VARCHAR(50) | 修改人 |
| updateTime | TIMESTAMP | 修改时间 |

### 核心计算逻辑

```
材料用量差异 = (实际用量 - 标准用量) × 标准单价
材料价格差异 = (实际单价 - 标准单价) × 实际用量
人工效率差异 = (实际工时 - 标准工时) × 标准小时费率
人工费率差异 = (实际小时费率 - 标准小时费率) × 实际工时
制造费用差异 = 实际制造费用 - 标准制造费用
产量差异     = (实际产出 - 计划产出) × 标准单位成本
委外费差异   = 实际委外费 - 标准委外费（标准 = rollupLine.subcontractCost × 完工量；实际 = wo.subcontractCost）
```

## 报表维度

| 维度 | 聚合粒度 | 过滤条件 |
|------|---------|---------|
| 工单 | 按工单汇总全部差异 | 工单号、产品、期间 |
| 工作中心 | 按工作中心汇总 | 工作中心、时间段 |
| 产品 | 按产品汇总 | 产品编码、期间 |
| 差异类型 | 按类型分类 | 差异类别、阈值（>5%标红） |
| 期间 | 月/季/年趋势 | 会计期间 |

## 使用流程

> **当前实现范围**：PPV（采购价差）在采购入库 DONE 时由 inventory 域 `InvPostingDispatcher.dispatchPurchasePriceVariance` 捕获并过账（`PURCHASE_PRICE_VARIANCE` 业务类型，plan 2026-07-05-0427-2）。生产差异（工单完工→差异计算→差异入账）由 plan 2026-07-05-1838-2 交付：`ErpMfgCostVariance` 实体已落地，差异引擎（`ProductionVarianceCalculator`）+ 完工触发（config-gated `erp-mfg.variance-auto-calc-enabled`）+ 过账（`ProductionVarianceDispatcher` + `PRODUCTION_VARIANCE` 业务类型）+ 手动入口（`ErpMfgCostVariance__calculateVariances`）均已实现。

1. **工单完工** → config-gated 自动触发差异计算（`erp-mfg.variance-auto-calc-enabled` 默认关，开启时 `willFinish` 分支调用 `ProductionVarianceCalculator`）；亦可经手动入口 `ErpMfgCostVariance__calculateVariances` 重算（幂等：先红冲既有 PRODUCTION_VARIANCE 凭证 + 先删旧行再重算，仅 COMPLETED 工单允许）
2. **差异计算** → `ProductionVarianceCalculator` 聚合实际成本（WorkOrder 四要素）与标准成本（FIRMED cost rollup）逐项对比，写 `ErpMfgCostVariance` 行（6 类差异：材料用量/人工效率/人工费率/制造费用/产量/委外费）
3. **差异入账** → `ProductionVarianceDispatcher` 按成本要素汇总净差异组装 PostingEvent，经 `IErpFinVoucherBiz.post` 提交过账（`PRODUCTION_VARIANCE` 业务类型），成功回写 `posted=true`
4. **分析报表** → 按维度查看差异分布（`ErpMfgCostVariance__findByWorkOrder` / `aggregateByType` 查询入口，报表渲染归 Deferred）
5. **异常预警** → 差异超过阈值触发通知（Deferred，依赖通知派发通道）

## 重算幂等实现注记（plan 2026-07-18-2251-1）

`ErpMfgCostVariance__calculateVariances` 重算路径采用「红冲→删旧→重算→派发」四步链，确保**数据行新金额与 GL 凭证金额始终一致**（消除重算前「数据新 + 凭证旧」分叉缺陷）：

1. **`ProductionVarianceDispatcher.reverseIfExists(workOrderId)`**：构造 `billHeadCode = wo.code + "-PV"`（与正向 `buildEvent` 对称）→ 调 `MfgPostingExecutor.reverse(billHeadCode, PRODUCTION_VARIANCE)` → 平台 `IErpFinVoucherBiz.reverse` 红冲既有 NORMAL 凭证（原 `isReversed=true` + 新建 REVERSAL 红字凭证，行同向取负）。
   - **异常处理范式**：`reverseIfExists` 内部 `try { ... } catch (Exception e) { LOG.warn(...) }` 守护吞所有异常。`IErpFinVoucherBiz.reverse` 在无原已过账凭证时抛 `NopException(ERR_REVERSE_SOURCE_NOT_FOUND)`（非 no-op），由本地 catch 吞此异常 + 真实红冲失败异常（如事务冲突/网络异常），log warn 不阻断重算后续步骤。范式对齐 `dispatchIfApplicable:109-115` 过账失败 try/catch（数据一致性语义对称：过账失败=新凭证未生成，红冲失败=旧凭证未撤销，两者均吞异常保回退/前进路径继续可观测）。
   - **红冲失败孤儿凭证风险可观测**：失败时新凭证正常生成但旧凭证成孤儿；log warn 含 workOrderId + billHeadCode + 异常 message，归 finance 5.1 异常工作台兜底。
2. **`deleteByWorkOrder(workOrderId)`**：物理删除该工单全部 `ErpMfgCostVariance` 旧行。
3. **`calculateVariances(workOrderId)`**：按当前工单成本重新计算差异行。
4. **`dispatchIfApplicable(workOrderId)`**：派发新 NORMAL 凭证，成功回写数据行 `posted=true`。

两同型 call site 共享同一缺陷机制 + 同一修复 + 同一 dispatcher：

- **Call site A** `ErpMfgCostVarianceBizModel.calculateVariances:69`（手动重算入口）
- **Call site B** `ErpMfgWorkOrderProcessor.reportCompletion:231`（完工自动重算入口，config-gated `erp-mfg.variance-auto-calc-enabled`）

**一致不变量**：重算完成后，`ErpFinVoucherBillR` 反查 `{wo.code}-PV` PRODUCTION_VARIANCE 仅 1 条 `isReversed=false` NORMAL 凭证（确认无孤儿）；`ErpMfgCostVariance` 数据行与该凭证行金额一致；全部数据行 `posted=true`。

**Non-Goals**：行级红冲（`reverseLine`）归不同结果面 successor；`IErpFinVoucherBiz.reverse` 在无原凭证抛异常的平台契约修订归 nop-entropy 上游 successor（本计划已纠正 1745-1/2 传播的「无凭证安全 no-op」错误措辞）。

## 配置点

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `erp-mfg.variance-auto-calc-enabled` | `false` | 工单完工达量（willFinish）时自动触发生产差异计算 + 过账；关闭时需手动经 `calculateVariances` 入口计算 |

## 涉及的领域机制

- `../finance/costing-methods.md` — 成本计算方法（标准成本、实际成本）
- `state-machine.md` — 工单状态与完工触发
- `bom-and-routing.md` — 标准用量与标准工时来源
