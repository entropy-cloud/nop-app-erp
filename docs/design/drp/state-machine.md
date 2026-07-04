# DRP（分销需求计划）域状态机

> **设计要点依据**：本状态机按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度组织。审查本状态机时使用该提示词。
>
> DRP 域有两类状态对象：**DRP 计划头**（DrpPlan）与 **DRP 明细行**（DrpLine）。

## 适用对象一：DRP 计划头（DrpPlan）

### 1. 状态定义

| 状态 | 业务含义（等待什么） | 业务单据影响 |
|------|----------------------|--------------|
| 草稿（DRAFT） | 计划已创建但未运算，等待运行 DRP 计算 | 无 |
| 已计算（COMPUTED） | DRP 引擎已完成净需求计算，建议补货量已生成，等待人工审批 | 明细行处于 SUGGESTED 状态 |
| 已批准（APPROVED） | 终态：计划员已审批，等待执行生成补货单 | 可自动或手动生成 TransferOrder/PurchaseOrder |
| 已执行（EXECUTED） | 终态：补货单已全部生成完毕 | — |

### 2. 迁移完整性

```
草稿 (DRAFT)
  └─ 运行 DRP 计算 → 已计算 (COMPUTED)

已计算 (COMPUTED)
  ├─ 批准 → 已批准 (APPROVED)
  └─ 回退 → 草稿 (DRAFT)  [参数调整后重新计算]

已批准 (APPROVED)
  └─ 生成补货单并执行 → 已执行 (EXECUTED)
```

| 迁移 | 触发人 | 前置条件 | 结果 |
|------|--------|----------|------|
| DRAFT→COMPUTED | 系统（DRP 引擎）/ 计划员 | 仓库补货参数已配置；物料主数据完整 | 生成 SUGGESTED 的 DrpLine |
| COMPUTED→APPROVED | 计划员（审批） | 明细行已审查，approvedQty 已确认 | 明细行标记 APPROVED |
| COMPUTED→DRAFT | 计划员 | 参数调整或数据更新 | 清除计算结果的明细行 |
| APPROVED→EXECUTED | 系统（自动）/ 计划员（手动） | 所有 APPROVED 行已生成对应补货单 | 明细行→ORDERED |

### 3. 终态与恢复

- 终态：`已批准（APPROVED）`、`已执行（EXECUTED）`。
- APPROVED 可直接回退到 DRAFT（人工撤回）。
- EXECUTED 不可回退；补货单已生成则走补货单的撤销流程。

### 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| DRP 计算时发现物料参数不完整 | 跳过该物料行并在运行日志中记录告警 |
| 生成补货单时目标仓库不可用 | 暂停该行生成，标记错误 |
| 计划员批准前发现库存已变化 | 重新计算（COMPUTED→DRAFT 再运行） |
| 并发多人审批同一计划 | 乐观锁，后提交者失败 |

### 5. 可达性

- DRAFT → COMPUTED → APPROVED → EXECUTED 主线流畅。
- COMPUTED → DRAFT 回退路径合法且必要。
- 无不可达状态。

### 6. 角色与权限

| 迁移 | 执行角色 |
|------|----------|
| DRAFT→COMPUTED | 系统 / 计划员 |
| COMPUTED→APPROVED | 计划主管（审批权） |
| APPROVED→EXECUTED | 系统（自动）或计划员（手动触发） |
| COMPUTED→DRAFT | 计划员（回退权限） |

危险操作：
- **批量批准补货建议**：需计划主管审批，因涉及采购/调拨资金占用。
- **EXECUTED 状态回退**：不允许，补货单已生成需走补货单流程。

### 7. 外部依赖

| 外部场景 | 内部处理 |
|----------|----------|
| 库存数据更新 | DRP 计算时读取当前库存/已分配/在单量 |
| 预测数据 | 从销售预测模块读取 forecastDemand（或手动录入） |
| TransferOrder/PurchaseOrder 生成 | DRP 批准后调用 inventory/purchase 域接口 |
| 仓库/物料主数据 | 从 master-data 读取 |

外部触发渠道：
- 计划员手工运行（主要渠道）。
- 定时任务自动运行（按配置的 cron 表达式）。

### 8. TODO / 任务策略

| 状态（Plan） | 是否产生 TODO | TODO 类型 |
|-------------|---------------|-----------|
| DRAFT | 否 | — |
| COMPUTED | 是 | assigned（计划主管）—— 待审批 DRP 计划 |
| APPROVED | 是 | assigned（计划员）—— 待生成补货单 |
| EXECUTED | 否 | — |

避免"已计算计划长期未审批"：COMPUTED 超过 48h 产生催办。

### 9. 场景演练

#### 场景 A：月度 DRP 计划（完整流程）

1. 计划员创建 7 月 DRP 计划（DRAFT），选择期间 2026-07-01 ~ 2026-07-31。
2. 运行 DRP 计算引擎 → COMPUTED。
3. 系统为每个物料+仓库组合计算净需求，生成 SUGGESTED 行。
4. 计划主管审查，调整 approvedQty，批准 → APPROVED。
5. 系统自动生成补货单（TransferOrder for TRANSFER, PurchaseOrder for PURCHASE）。
6. 所有补货单生成后 → EXECUTED。

#### 场景 B：COMPUTED 后参数调整重新计算

1. DRP 计划已 COMPUTED，部分行建议补货量异常（安全库存参数刚更新）。
2. 计划员回退到 DRAFT（COMPUTED→DRAFT）。
3. 修改仓库补货参数（ErpDrpParameter.safetyStock）。
4. 重新运行计算，生成新的 SUGGESTED 行。

### 10. 与设计文档一致性

- 状态定义见 `drp/README.md` §ErpDrpPlan 和 §ErpDrpLine。
- 状态码归 `model/app-erp-drp.orm.xml` dict `erp-drp/drp-plan-status` 和 `erp-drp/drp-line-status`。

---

## 适用对象二：DRP 明细行（DrpLine）

### 1. 状态定义

| 状态 | 业务含义 |
|------|----------|
| 建议（SUGGESTED） | DRP 计算生成的原始建议补货量，等待审批 |
| 已批准（APPROVED） | 计划员确认 approvedQty，等待生成补货单 |
| 已下单（ORDERED） | 终态：已生成 TransferOrder 或 PurchaseOrder |
| 已取消（CANCELLED） | 终态：手动取消该行（不生成补货单） |

### 2. 迁移

```
SUGGESTED → APPROVED（计划审批通过）
         → CANCELLED（人工取消）

APPROVED → ORDERED（补货单已生成）
        → CANCELLED（生成前取消）
```

### 3. 场景演练

#### 场景 C：自动生成补货单

1. DrpLine 处于 APPROVED（计划头已批准）。
2. 系统为每条 APPROVED 行检查 replenishmentType：
   - TRANSFER → 调用 inventory 域创建 TransferOrder。
   - PURCHASE → 调用 purchase 域创建 PurchaseOrder。
3. 补货单创建成功后回写 orderBillType/orderBillCode。
4. DrpLine.status → ORDERED。
5. 所有 APPROVED 行都完成后，DrpPlan.status → EXECUTED。

## 审查提示

审查本状态机时，使用 `docs/skills/state-machine-business-review-prompt.md`，重点检查：
- 计划批审（COMPUTED→APPROVED）的权限分离（计划员 vs 计划主管）。
- 补货单自动生成的可靠性（APPROVED→ORDERED 事务性）。
- 参数调整后回退 DRAFT 是否清理旧建议行。
- EXECUTED 状态的不可逆性保证。
