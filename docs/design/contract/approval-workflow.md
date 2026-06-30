# 合同审批工作流设计

## 目的

定义合同的多级审批工作流：按金额阈值路由到不同审批层级，支持审批链配置、驳回答复循环与审批矩阵配置。本设计覆盖采购合同、销售合同、服务合同等所有合同类型的统一审批流程。

## 设计依据

> 参考 **Axelor** 合同审批的层级路由模式（多级金额阈值 + 角色链审批）。
>
> 来源 `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §合同管理。

## 工作流概述

```
合同经办人提交 → 部门经理审批 → 财务审批 → 法务审批 → 高管审批
     │              │              │           │           │
     │              │              │           │           └─ 金额 > ¥500,000
     │              │              │           └───────────── 金额 > ¥100,000
     │              │              └───────────────────────── 金额 > ¥50,000
     │              └──────────────────────────────────────── 金额 > ¥10,000
     └─────────────────────────────────────────────────────── 所有合同
```

### 路由规则

| 金额范围 | 必经审批节点 | 说明 |
|----------|-------------|------|
| ¥0 ~ ¥10,000 | 经办人直接提交 → DRAFT | 无需审批，自行归档或快速通道 |
| ¥10,000 ~ ¥50,000 | 部门经理 → 法务 | 部门经理审批后法务会签 |
| ¥50,000 ~ ¥100,000 | 部门经理 → 财务 → 法务 | CFO 参与审批 |
| ¥100,000 ~ ¥500,000 | 部门经理 → 财务 → 法务 | 法务审批后即生效 |
| > ¥500,000 | 部门经理 → 财务 → 法务 → 高管 | 需 CEO/总裁最终签批 |

## 审批链路由

### 节点定义

每个审批节点包含以下属性：

| 属性 | 含义 | 示例 |
|------|------|------|
| `approvalOrder` | 审批顺序（1-based） | 1=部门经理, 2=财务, 3=法务, 4=高管 |
| `approverRole` | 审批角色（→ ErpMdRole） | DEPT_MANAGER / FINANCE_MANAGER / LEGAL_MANAGER / EXECUTIVE |
| `minAmount` | 触发该节点的最小金额（含） | 10000 |
| `maxAmount` | 触发该节点的最大金额（含） | 50000 |
| `allowSkip` | 是否可跳过（金额未达阈值时） | true |
| `requireSignOff` | 是否需要签署确认 | false |

### 动态路由逻辑

```
1. 经办人提交合同（DRAFT → NEGOTIATION）
2. 系统读取 ErpCtApprovalMatrix，按 totalAmount 匹配适用的审批节点列表
3. 按 approvalOrder 升序排序
4. 生成 ErpCtApprovalRecord（每节点一条）
5. 第一个节点激活（approvalStatus=PENDING），其余为 WAITING
6. 前一节点审批通过后激活下一节点
7. 所有节点通过 → NEGOTIATION → 合同状态迁移至 ACTIVE 等待签署
```

## 驳回与答复循环

### 驳回流程

```
审批人驳回
    │
    ├─► 填写驳回意见（required）、驳回原因
    │
    ├─► 合同状态 → NEGOTIATION（保持谈判中状态不变）
    │
    ├─► 经办人收到通知
    │
    ├─► 经办人决策
    │     ├─ 修改合同内容后重新提交 → 当前驳回节点重新审批
    │     │   （保持原 approvalOrder，不清除已通过的节点）
    │     └─ 撤销合同 → DRAFT（放弃提交）
    │
    └─► 重新提交后通知驳回人复核
```

### 答复循环规则

| 场景 | 处理 |
|------|------|
| 单节点驳回后修改重新提交 | 仅重新激活该驳回节点，已通过的上级节点不重审 |
| 关键条款修改 | 如果修改内容涉及金额 > 当前阈值或条款类型变更，重置全部审批 |
| 多次驳回 | 允许驳回最多 N 次（配置 `erp-ct.approval-max-retries`，默认 3），超限后锁定需强制升级 |
| 超时未处理 | 审批节点超过 `erp-ct.approval-timeout-hours`（默认 72h）未处理，升级通知上一级 |

### 版本与审批联动

合同版本变更（Amendment）时，如果变更金额超过原合同 20%（可配置 `erp-ct.amendment-reapproval-threshold`），需要重新走审批流。否则可直接签署生效。

## 审批矩阵配置

### ErpCtApprovalMatrix（审批矩阵）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/orgId | 标准 | |
| minAmount | 最小金额（含），0 为无下限 | 🟢 Axelor 金额阈值 |
| maxAmount | 最大金额（含），null 为无上限 | 🟢 Axelor 金额阈值 |
| approverRole | 审批角色（→ ErpMdRole） | |
| approvalOrder | 审批顺序（1-based，越小越先） | |
| contractType | 适用合同类型（null 表示全部） | |
| allowSkip | 是否可跳过（金额未达阈值时跳过） | |
| isActive | 是否启用 | |

### ErpCtApprovalRecord（审批记录）

| 字段 | 含义 |
|------|------|
| id/contractId/orgId | 标准 |
| approvalMatrixId | 关联 ErpCtApprovalMatrix |
| approvalOrder | 顺序号 |
| approverId | 审批人（→ ErpMdUser） |
| approvalStatus | dict：WAITING / PENDING / APPROVED / REJECTED / SKIPPED |
| comment | 审批意见 |
| approvedAt / rejectedAt | 处理时间 |

### 配置示例

```
金额范围: ¥10,000 ~ ¥50,000
  排序1: 部门经理 (DEPT_MANAGER)
  排序2: 法务 (LEGAL_MANAGER)

金额范围: ¥50,001 ~ ¥100,000
  排序1: 部门经理 (DEPT_MANAGER)
  排序2: 财务 (FINANCE_MANAGER)
  排序3: 法务 (LEGAL_MANAGER)

金额范围: ¥10,0000 ~ ¥500,000
  排序1: 部门经理 (DEPT_MANAGER)
  排序2: 财务 (FINANCE_MANAGER)
  排序3: 法务 (LEGAL_MANAGER)

金额范围: > ¥500,000
  排序1: 部门经理 (DEPT_MANAGER)
  排序2: 财务 (FINANCE_MANAGER)
  排序3: 法务 (LEGAL_MANAGER)
  排序4: 高管 (EXECUTIVE)
```

## 业务规则

1. **金额取合同 totalAmount**：审批路由基于合同头的 `totalAmount`（含税或未税取决于合同类型配置）。
2. **多版本金额**：Amendment 版本金额以变更单金额为准；变更后若触发重新审批阈值则走独立审批流（不重置原合同已生效的版本）。
3. **紧急通道**：允许管理员跳过部分节点（配置 `allowSkip=true` 的节点可跳过），但跳过操作需记录审计日志。
4. **驳回不重置已审批**：同为"答复循环"，已通过的节点保持 APPROVED，修改后只重新激活驳回节点及其后续节点。
5. **撤回**：经办人在 `PENDING` 节点未处理时可撤回合同（NEGOTIATION → DRAFT），撤回后审批记录归档。
6. **审批人转交**：`PENDING` 的节点允许当前审批人转交给同角色其他人。

## 跨域协作

| 对端 | 协作方式 |
|------|---------|
| master-data（Role/User） | 审批人通过 ErpMdRole + 组织架构确定 |
| notification | 审批待办通知、驳回通知、超时升级通知 |
| audit-log | 审批操作日志（审批/驳回/转交/跳过） |

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-ct.approval-enabled` | false | 是否启用审批流 |
| `erp-ct.approval-max-retries` | 3 | 单节点最大驳回次数 |
| `erp-ct.approval-timeout-hours` | 72 | 审批节点超时小时数 |
| `erp-ct.approval-urgent-threshold` | 500000 | 需高管审批的金额阈值 |
| `erp-ct.amendment-reapproval-threshold` | 0.2 | 变更金额超过原合同此比例时需重新审批 |

## 反模式警示

- ⛔ **审批流硬编码在 Java 代码中**——金额阈值和审批角色必须在 ErpCtApprovalMatrix 可配置，不可硬编码。
- ⛔ **驳回后重置全部审批**——只重新激活驳回节点及其后续，已审批通过的节点保持不变。
- ⛔ **审批与签署混淆**——审批是内部流程（NEGOTIATION 中），签署是双方确认（NEGOTIATION→ACTIVE 的触发条件），两者分离。

## 证据强度

| 证据 | 强度 | 说明 |
|------|------|------|
| 多级金额阈值审批路由 | 🟢 | Axelor 合同审批多级金额阈值模式 |
| 审批矩阵配置 | 🟢 | Axelor Contract 审批链（审批角色 + 金额范围） |
| 驳回答复循环 | 🟢 | 通用审批流模式 |
| 超时升级机制 | ⚪ | 领域常识 |

## 参考

- `contract/README.md`（合同实体与状态机）
- `contract/state-machine.md`（NEGOTIATION 状态细节）
- `docs/design/roles-and-permissions.md`（角色定义）
