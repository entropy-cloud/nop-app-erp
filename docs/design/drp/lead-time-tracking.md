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
