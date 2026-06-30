# 批次召回事件（Recall）

## 目的

设计批次召回事件聚合层，复用已有追溯链（trace-chain）+ NCR + 销售退货底层能力，补齐"有追溯+NCR+退货底层、缺召回事件聚合"的 P1 缺口。

## 边界

- 本模块负责：召回事件登记、受影响目标定位（批次→销售出库→客户反查）、客户通知、触发批量销售退货。
- 本模块不负责：追溯链底层（`trace-chain.md` 已有反向追溯）；NCR 单点不合格处理（`state-machine.md` 已有）；销售退货过账（sales 域标准退货，召回触发的退货走既有流程）。
- **前置条件声明**：批次/序列号追溯必须启用（`erp-inv.trace-chain-enabled=true`，见 `trace-chain.md` 配置项），否则无法定位受影响目标。
- 实体为**建议命名，待 ORM 计划落地**（`model/app-erp-quality.orm.xml` 是 ask-first 保护区域，本文件不复述 schema）。

## 设计依据

> 来源 `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §2.4。

### 证据精确化

🟢 源码回查 `/Users/abc/sources/erp/`：

- 🟢 `grep -rli "recall"` ~30 文件，**全部技术语义或量具校准召回，无一例批次召回实体**。
- 🟢 唯一业务相关：**Carbon 量具校准召回**（`quality.ts:26514`）"量具超差→产品召回→通知客户"逻辑，写在质量文档，**无独立 Recall 实体**。
- 🟢 Carbon `NonConformanceIssue` 关联广度（`quality.models.ts:462-485`：supplierId/customerId/jobOperationId/trackedEntityId）——**召回底层能力（目标定位）已有零件**。

**结论**：召回事件实体属 ⚪ 领域常识（开源无独立 Recall 实体），本项目复用已有 NCI 关联广度 + trace-chain 底层，新增事件聚合层。

## 实体清单

> 表前缀 `erp_qa_`、类名 `ErpQa*`、字典 `erp-qa/*`。以下为建议命名，待 ORM 计划落地。

### ErpQaRecall（召回事件头，表 `erp_qa_recall`）

| 字段 | 含义 |
|---|---|
| id/code/orgId | 标准 |
| recallName | 召回事件名称 |
| triggerType | dict `erp-qa/recall-trigger-type`：见下 |
| sourceNcrId | 升级来源 NCR（若 triggerType 为 NCR 升级，→ErpQaNonConformance） |
| materialId/batchId/serialNo | 召回对象（物料/批次/序列号，至少一个） |
| rootCause | 根本原因 |
| severityLevel | dict `erp-qa/recall-severity`：LOW/MEDIUM/HIGH/CRITICAL |
| businessDate | 召回发起日期 |
| notifyCustomer | 是否已通知客户（必备动作，DONE 前必须 true） |
| status | dict `erp-qa/recall-status`：见状态机 |
| approveStatus | dict `erp-qa/approve-status`：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED |
| 标准审计字段 | |

**triggerType（召回触发双入口）**：

| 值 | 含义 | 来源 |
|---|---|---|
| MANUAL | 手动发起（质量主管） | 人工 |
| GAUGE_NCR_UPGRADE | 量具超差 NCR 升级 | 🟢 Carbon 量具校准召回逻辑 |
| BATCH_NCR_UPGRADE | 批次 NCR 升级（批次性不合格） | 裁决 D2 |
| REGULATORY | 监管机构要求 | 合规 |

**状态机**：

```
登记 (OPEN)
  ├─ 审批通过 → 已批准 (APPROVED)
  │              ├─ 目标定位 + 客户通知 → 执行中 (IN_PROGRESS)
  │              │              ├─ 批量退货完成 → 已关闭 (CLOSED, 终态)
  │              └─ 审批驳回 → 已取消 (CANCELLED, 终态)
  └─ 取消 → 已取消 (CANCELLED)
```

> 召回**强制审批**（高风险，severityLevel=CRITICAL 需高层审批），APPROVED 后才执行目标定位与通知。

### ErpQaRecallTarget（受影响目标明细，表 `erp_qa_recall_target`）

| 字段 | 含义 |
|---|---|
| id/recallId/orgId | 标准 |
| partnerId | 受影响客户（→ErpMdPartner） |
| batchId/serialNo | 受影响批次/序列号 |
| salesDeliveryId | 关联销售出库（→ErpSalDelivery，弱指针，trace-chain 反查定位） |
| shippedQty | 已发货数量 |
| notifiedAt/notifiedBy | 通知时间/人 |
| returnStatus | dict `erp-qa/recall-target-return-status`：PENDING/NOTIFIED/RETURNED |
| 标准审计字段 | |

**目标定位算法**：通过 `trace-chain.md` 反向追溯——召回对象（批次/序列号）→ 正向追溯找到销售出库移动单 → 关联客户与发货数量，生成 ErpQaRecallTarget 明细。

## 裁决 D2：NCR 升级召回实现

**裁决**：NCR 升级召回 = **新增 NCR status 值 `ESCALATED_TO_RECALL`**（给字典 `erp-qa/ncr-status` 新增值），而非不改 NCR 状态仅建 Recall。

理由：显式状态可查、便于审计。现有 `ErpQaNonConformance.status`（orm.xml:268，字典 `erp-qa/ncr-status`）现有值 OPEN/IN_REVIEW/RESOLVED/CANCELLED，新增 `ESCALATED_TO_RECALL` 表示该 NCR 已升级为召回事件（关联 ErpQaRecall）。

## 业务规则

1. **召回不过账**：召回事件本身不产生会计凭证。召回**触发**的销售退货走 sales 域标准退货过账（红字出库/退款凭证）。
2. **召回不直接改库存余额**：召回产生的库存变动通过标准销售退货移动单完成（移动单 DONE 写流水/更新余额），召回事件只登记与编排，不绕过移动单直接改余额。
3. **客户通知是必备动作**：severityLevel≥MEDIUM 的召回，召回关闭前 `notifyCustomer` 必须为 true（🟢 Carbon "customer is immediately notified"）。未通知全部受影响客户不可关闭召回。
4. **召回强制审批**：所有召回 APPROVED 才能执行（高风险，防误召回造成商誉损失）。
5. **目标定位依赖追溯链**：召回发起时若 `erp-inv.trace-chain-enabled=false`，报错提示需先启用追溯。
6. **批量退货编排**：APPROVED 后为每个 ErpQaRecallTarget 生成销售退货单（批量），走 sales 标准退货流程。

## 跨域协作

| 对端 | 协作内容 |
|---|---|
| inventory/trace-chain | 反向追溯定位受影响客户/批次（批次→销售出库→客户） |
| sales | 召回触发批量销售退货（走标准退货过账） |
| quality/NCR | NCR status=ESCALATED_TO_RECALL 升级触发召回登记 |

## 配置点

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `erp-qua.recall-require-approval` | true | 召回是否强制审批 |
| `erp-qua.recall-notify-required-to-close` | true | 关闭召回前是否强制要求通知所有受影响客户 |

## 反模式警示

- ⛔ **召回做成 NCR 的一个 status**——召回是**一对多批量**事件（一个召回影响多个客户/批次），NCR 是**单点不合格**；用 NCR.status 承载召回会丢失批量聚合。裁决 D2 是给 NCR 加"升级标记"状态值，召回本身独立成 ErpQaRecall 实体。
- ⛔ **召回直接改库存余额**——应走标准销售退货移动单（移动单 DONE 才写流水/余额），召回只登记编排。
- ⛔ **召回无客户通知即关闭**——通知是合规必备动作（🟢 Carbon 范式）。

## 工作量声明

召回属**中等扩展**（非轻量）：需新增召回实体（ErpQaRecall/Target）+ NCR 升级动作（字典加值）+ 批量退货编排（调 sales 退货）+ 客户通知机制。复用 trace-chain/NCR/退货底层降低难度，但事件聚合层是新建。

## 菜单归属

quality 域「召回管理」分组：召回事件、召回目标明细。

## 证据强度标注

| 证据 | 强度 | 说明 |
|---|---|---|
| 开源无批次召回实体 | 🟢 | `grep -rli recall` ~30 文件全技术语义/量具校准，源码实测 |
| Carbon 量具校准召回逻辑 | 🟢 | `quality.ts:26514` 源码实测（量具超差→召回→通知客户，无独立实体） |
| Carbon NCI 关联广度（目标定位零件） | 🟢 | `quality.models.ts:462-485`（supplierId/customerId/jobOperationId/trackedEntityId）源码实测 |
| 客户通知必备动作 | 🟢 | Carbon "customer is immediately notified" 源码实测 |
| 召回事件聚合实体 | ⚪ | 开源无，领域常识 |
| 本项目 ErpQaNonConformance.status（ncr-status 字典） | 🟢 | `module-quality/...orm.xml:268,46` 实测 |

## 参考

- `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §2.4（设计依据）
- `docs/design/quality/state-machine.md`（NCR 状态机、ESCALATED_TO_RECALL 新增值）
- `docs/design/inventory/trace-chain.md`（反向追溯定位受影响目标）
- `docs/design/sales/README.md`（销售退货标准流程）
