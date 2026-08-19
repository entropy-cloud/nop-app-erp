# 提前期变异性跟踪与动态安全库存设计

## 目的

设计采购订单提前期的实际记录、统计分析、动态安全库存调整和供应商可靠性评分功能，为 DRP 补货参数（尤其是安全库存）提供基于实际数据的动态输入。

## 设计依据

> 参考 **DDMRP（Demand Driven MRP）**：提前期变异性直接影响缓冲水平和安全库存。
>
> 参考 **标准供应链统计学**：提前期的均值、标准差和分布分析用于供应商绩效评估。
>
> 参考 `docs/design/drp/safety-stock-optimization.md`：已有基于需求变异的安全库存计算方法，本设计补充提前期变异维度。
>
> 来源 `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §供应链计划。

## 架构概览

```
采购订单(PO) 收货确认
    │
    ▼
记录实际提前期: orderDate → receiptDate
    │
    ├─► 写入 ErpInvDrpLeadTimeRecord
    │       supplierId, materialId, orderDate, receiptDate, actualLeadTime
    │
    ▼
统计分析 (按供应商 + 物料)
    │
    ├─► 均值 (μ) / 标准差 (σ) / 最小值 / 最大值
    ├─► 提前期趋势 (月度/季度)
    └─► 供应商可靠性评分
    │
    ▼
动态安全库存调整
    │
    ├─► 安全库存 = Z × σ_d × √μ_lt   (需求变异 × 平均提前期)
    │    加上提前期变异因子:
    ├─► 安全库存 = Z × √(σ_d² × μ_lt + μ_d² × σ_lt²)   (需求 + 提前期联合变异)
    │
    ▼
回写补货参数
    │
    ├─► 更新 ErpDrpParameter.safetyStock
    └─► 更新 ErpDrpParameter.replenishmentLeadTime
```

## 实际提前期记录

### ErpInvDrpLeadTimeRecord（提前期记录）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/orgId | 标准 | |
| supplierId | 供应商 | → ErpMdPartner |
| materialId | 物料 | → ErpMdMaterial |
| orderDate | 订单日期（采购订单创建日期） | |
| receiptDate | 入库日期（ERp 收货确认日期） | |
| actualLeadTime | 实际提前期（天）= DATEDIFF(receiptDate, orderDate) | |
| expectedLeadTime | 预期提前期（采购订单上约定的提前期） | |
| varianceDays | 偏差天数 = actualLeadTime - expectedLeadTime | |
| purchaseOrderCode | 来源采购单号 | |
| isOnTime | 是否准时（actualLeadTime ≤ expectedLeadTime × 容差系数） | |
| earlyLateFlag | dict `erp-inv/drp-lt-flag`：ON_TIME / EARLY / LATE | |
| remark | 备注 | |
| 标准审计字段 | | |

### 提前期偏差标记字典 `erp-inv/drp-lt-flag`

| code | label | value | 条件 |
|------|-------|-------|------|
| ON_TIME | 准时 | 10 | actualLeadTime 在 expectedLeadTime ± 容差内 |
| EARLY | 提前 | 20 | actualLeadTime < expectedLeadTime × (1 - 容差) |
| LATE | 延迟 | 30 | actualLeadTime > expectedLeadTime × (1 + 容差) |

默认容差系数 = 0.1（可配置）

## 提前期统计分析

### 统计指标计算

| 指标 | 公式 | 含义 |
|------|------|------|
| 均值 (μ) | `AVG(actualLeadTime)` | 平均实际提前期 |
| 标准差 (σ) | `STDDEV(actualLeadTime)` | 提前期波动程度 |
| 最小值 | `MIN(actualLeadTime)` | 历史最快 |
| 最大值 | `MAX(actualLeadTime)` | 历史最慢 |
| 准时率 | `COUNT(isOnTime=true) / COUNT(*)` | 供应商可靠性指标之一 |
| 中位数 | `MEDIAN(actualLeadTime)` | 抗异常值 |

### 统计粒度

| 粒度 | 说明 | 用途 |
|------|------|------|
| 供应商级别 | 该供应商所有物料 | 供应商整体绩效评估 |
| 供应商+物料级别 | 特定供应商特定物料 | DRP 动态参数调整（最细粒度） |
| 物料级别 | 跨供应商 | 物料提前期基线比较 |
| 月度/季度趋势 | 时间序列分析 | 提前期趋势报告 |

### 趋势报告

```
供应商: SUP-001 (示例)
物料: MTL-001
趋势: 2025 Q1 ~ 2026 Q2

季度    │ 平均提前期 │ 标准差  │ 准时率 │ 样本数
────────┼───────────┼────────┼───────┼───────
2025 Q1 │ 14.2      │ 3.1    │ 85%   │ 120
2025 Q2 │ 15.0      │ 4.2    │ 80%   │ 115
2025 Q3 │ 16.8      │ 5.5    │ 72%   │ 108
2025 Q4 │ 15.5      │ 3.8    │ 78%   │ 112
2026 Q1 │ 13.8      │ 2.9    │ 88%   │ 118
2026 Q2 │ 14.0      │ 3.0    │ 86%   │ 125

分析: 2025 Q3 提前期恶化（可能受季节/假期影响），Q4 起恢复。
建议: 动态安全库存中考虑 Q3 波动，安全库存 = Z × √(σ_d² × μ_lt + μ_d² × σ_lt²)
```

## 动态安全库存调整

### 联合变异公式

标准安全库存公式仅考虑需求变异（Z × σ_d × √L）。当提前期也存在显著变异时，应采用联合变异公式：

```
安全库存 = Z × √(σ_d² × μ_lt + μ_d² × σ_lt²)

其中:
  Z    = 服务水平 Z 值
  σ_d  = 需求标准差（日/周）
  μ_lt = 平均提前期（天）
  μ_d  = 平均需求（日/周）
  σ_lt = 提前期标准差（天）
```

### 调整策略

| 提前期变异程度 | 建议策略 | 安全库存影响 |
|----------------|----------|-------------|
| 低 (σ_lt ≤ 0.2 × μ_lt) | 使用标准公式 `Z × σ_d × √μ_lt` | 与现有 SS 持平 |
| 中 (0.2 × μ_lt < σ_lt ≤ 0.5 × μ_lt) | 使用联合变异公式 | SS 增加 10~30% |
| 高 (σ_lt > 0.5 × μ_lt) | 联合变异公式 + 额外缓冲 | SS 增加 30~80% |

### 与现有安全库存优化集成

参见 `docs/design/drp/safety-stock-optimization.md` 的安全库存计算方法。本功能补充了提前期维度：

```
ErpInvDrpSafetyStockCalc
    ├─ method = STATISTICAL
    ├─ leadTimeDays = μ_lt (从 LeadTimeRecord 统计得出)
    ├─ 额外: leadTimeStdDev = σ_lt
    ├─ 额外: useJointVariation = true
    └─ calculatedSafetyStock = Z × √(σ_d² × μ_lt + μ_d² × σ_lt²)
```

## 供应商可靠性评分

### 评分模型

| 维度 | 权重 | 指标 | 数据来源 |
|------|------|------|----------|
| 准时率 | 40% | 统计期内准时交货比例 | ErpInvDrpLeadTimeRecord |
| 提前期稳定性 | 30% | 提前期变异系数 (σ/μ) | ErpInvDrpLeadTimeRecord |
| 数量准确率 | 20% | 交货数量与订单数量偏差率 | 采购模块收货记录 |
| 质量合格率 | 10% | 来料检验合格率 | 质量模块 |

### 评分计算

```
准时率得分 = 准时率 × 40
稳定性得分 = max(0, (1 - σ/μ)) × 30   （σ/μ 越小分越高）
数量准确率得分 = (1 - |偏差率|) × 20
质量合格率得分 = 合格率 × 10

总分 = 准时率得分 + 稳定性得分 + 数量准确率得分 + 质量合格率得分
评分等级: A (≥90) / B (≥75) / C (≥60) / D (<60)
```

### 评分影响

| 评分等级 | 安全库存调整 | 补货策略影响 |
|----------|-------------|-------------|
| A | 安全库存可取统计区间的下限值 | 可放宽审批，增加自动补货信任 |
| B | 使用均值或联合变异公式 | 正常审批流程 |
| C | 安全库存取上限（+1σ） | 加强人工审查，增加检验频率 |
| D | 安全库存显著增加（+2σ） | 触发供应商升级；考虑备选供应商 |

## 实现注记（RC-R1.82 / P1-RC-082）

> 提前期跟踪与供应商可靠性评分已落地（`ErpInvDrpLeadTimeProcessor` + `ErpInvDrpLeadTimeRecordBizModel`，
> 测试 `TestErpDrpLeadTimeStats` 13 组；联合变分接入 `SafetyStockEngine`）。与本文设计的差异与裁决记录如下。

### D4 裁决：收货确认触发模型

**选项 A（已裁决采纳）**：purchase receive approve Processor 后置直接调 drp Facade
`IErpInvDrpLeadTimeRecordBiz.recordFromPurchaseReceive(purchaseOrderCode, supplierId, orderDate, receiptDate, expectedLeadTime, materialIds)`——
actualLeadTime = DATEDIFF(receiptDate, orderDate)（orderDate=订单 businessDate，receiptDate=入库单 businessDate）；
expectedLeadTime = 订单 deliveryDate − businessDate（缺失传 null）。伴随新 Java 层 pom 边 `pur-service → drp-dao`
（data-dependency-matrix §2.4 登记，与 D1 越库 Facade 同链路）。未采纳选项 B（drp 侧事件拉取）：push 复用审批后置
事务上下文、零轮询延迟，@Nullable 注入容错 + 失败隔离（try/catch 不阻断 RECEIVED 主迁移）对齐 D1/RC-R1.85 先例。
幂等守卫：同 purchaseOrderCode + materialId 不重复落记录。订单/收货日期缺失或倒置抛
`erp.err.drp.lt.dates-invalid`（purchase 侧隔离告警 = L1「跳过告警」异常路径）。

### D5 裁决：σ_lt 持久化载体

**选项 A（已裁决采纳）：统计查询时实时计算，不留列**。SafetyStockEngine 接联合变分与评分重算均按
supplier+material（或 material 级）窗口现算 σ/μ；统计窗口 `erp-inv.drp-lt-stats-window-days` 默认 365 天
（≤0 全历史）。供应商解析自 `ErpDrpParameter.preferredSupplierId`（未配置时按物料跨供应商聚合）。
**残留风险**：实时计算的样本窗口语义 = 滚动窗口（非「最近 N 单」），大样本量下查询时延可优化（物化列
`ErpDrpParameter.leadTimeStdDev` 登记 Deferred But Adjudicated optimization candidate，万级样本触发）。

### 字典与容差口径

- dict `erp-inv/drp-lt-flag` 已物化（三值 ON_TIME/EARLY/LATE；owner doc 表中 10/20/30 整型 value 为 int 时代
  遗留，按 2026-07-03 字典整型→字符串重构落 string 码值）。
- 容差系数 config `erp-inv.drp-lt-tolerance` 默认 0.1：ON_TIME = actual ∈ [expected×(1−t), expected×(1+t)]
  （闭区间）；EARLY < 下界；LATE > 上界。
- expectedLeadTime 缺失行：varianceDays/earlyLateFlag 留空（不可判定）。isOnTime 列有 DDL 默认 true，
  **准时统计以 earlyLateFlag 非空为已判定标记**（judged 集合），未判定样本不入准时率分母——owner doc
  公式 `COUNT(isOnTime=true)/COUNT(*)` 的分母精确化为 COUNT(已判定)。

### 统计与评分实现口径

- 统计粒度：`findLeadTimeStats(supplierId?, materialId?)` 参数组合决定供应商级/供应商+物料级/物料级；
  指标 μ/σ（总体标准差）/min/max/中位数/准时率/变异系数（μ≤0 时 null）/样本数。
- 评分四维（40/30/20/10）：准时率×40；稳定性 max(0, 1−σ/μ)×30（μ≤0 且有样本视为完全稳定）；
  **数量准确率维度**（drp→pur 只读 Java 边，matrix §2.4 登记）：统计窗口内该供应商 APPROVED 采购单该物料行
  ΣreceivedQuantity/Σquantity 偏差，accuracy = max(0, 1−|Σreceived−Σordered|/Σordered)；**质量合格率维度**
  （drp→qa 只读 Java 边）：INCOMING 检验合格（ACCEPTED 或 CONDITIONAL 让步接收，与越库快检口径一致）占比。
- 无样本维度：得分记 0 且汇总行 `missingDimensions` 标注（QUANTITY/QUALITY），指标值留空（区别于真实 0 值），
  不静默忽略；汇总行 UK(supplierId, materialId) upsert，`recalculateLeadTimeStats` 幂等重算。
- 联合变分：变异分档中/高变异档（σ_lt/μ_lt > 0.2）统一采用联合变异公式 `Z × √(σ_d² × μ_lt + μ_d² × σ_lt²)`；**高档「额外缓冲」（调整策略表「SS 增加 30~80%」）无量化依据，显式简化为联合变异值**（集成口径与声明详见 `safety-stock-optimization.md §联合变分集成注记`）。
- 等级阈值：A≥90 / B≥75 / C≥60 / D<60（闭区间下界）。评分影响策略的自动执行（等级联动审批放宽/收紧）归
  successor（现无等级消费方）。
- 趋势月度/季度报表渲染归报表子系统后续（统计 API 已落地）。

## 证据强度

| 证据 | 强度 | 说明 |
|------|------|------|
| 实际提前期记录分析 | 🟢 | 供应链管理最佳实践 |
| 联合变异安全库存公式 | 🟢 | DDMRP / 库存管理经典文献 |
| 供应商可靠性评分 | 🟢 | 供应商计分卡（Balanced Scorecard） |
| 动态参数回写 | 🟢 | 本项目 safety-stock-optimization 集成 |

## 参考

- `drp/README.md`（DRP 模块总述）
- `drp/safety-stock-optimization.md`（安全库存优化，本功能的主要集成点）
- `drp/use-cases.md` §UC-DRP-08 提前期跟踪
- `model/app-erp-drp.orm.xml`（ORM 模型）
- `docs/design/purchase/README.md`（采购订单提前期）
