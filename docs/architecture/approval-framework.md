# 审批框架

## 目的

定义 nop-app-erp 的审批模式选择策略，支持按单据类型配置不同的审批方式。

## 审批模式

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| NONE | 无需审批 | 低风险单据（如库存移动单） |
| DIRECT | 直接审核（单级审批） | 中低风险单据（如普通采购订单） |
| WORKFLOW | 工作流审批（多级） | 高风险单据（如大额付款、资产处置） |

## 按单据类型配置

| 单据类型 | 默认审批模式 | 可配置 |
|----------|------------|--------|
| 采购订单 | DIRECT | 是（可升级为 WORKFLOW） |
| 采购入库单 | NONE | 是（可升级为 DIRECT） |
| 采购发票 | DIRECT | 是 |
| 付款单 | WORKFLOW | 是（大额需多级审批） |
| 销售订单 | DIRECT | 是 |
| 销售出库单 | NONE | 是 |
| 销售发票 | DIRECT | 是 |
| 收款单 | WORKFLOW | 是 |
| 资产卡片 | DIRECT | 是 |
| 资产处置 | WORKFLOW | 是 |
| 工单 | DIRECT | 是 |

## 委托规则

审批人可临时委托他人代审：

- 委托有时效（开始日期~结束日期）
- 委托期间被委托人拥有审批权限
- **责任不转移**：审批结果的最终责任人仍是原审批人
- 不可委托场景：反审核、反结账、管理员强操作

## 与 nop-wf 集成

- WORKFLOW 模式使用 nop-wf 引擎
- DIRECT 模式可使用 nop-wf 简单流程或直接状态变更
- nop-wf 完成回调更新业务单据 approveStatus
- `approveStatus` 只跟踪业务终态，严格限定为**四态**：`UNSUBMITTED`（未提交）/ `SUBMITTED`（已提交待审批）/ `APPROVED`（已批准）/ `REJECTED`（已驳回），不跟踪 wf 内部状态。`SUBMITTED` 表示单据已提交等待审批，审批过程中的场内进度（会签、转审、待阅等）完全由 nop-wf 引擎在 `NopWfStepInstance`/`NopWfWork` 表中管理，不污染业务表。**禁止**使用 `APPROVING`、`PENDING_APPROVAL` 等中间态值。
