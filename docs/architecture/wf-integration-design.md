# 审批流集成（ERP 应用层）

## 定位

本文是 nop-app-erp 应用层对接平台审批能力的**落位说明**：哪些 ERP 单据使用审批、ERP 业务联动如何接、首批落地范围。

**平台级设计权威在 nop-entropy**（不在本文）：
- 平台设计：`../nop-entropy/ai-dev/design/nop-wf/approvable-entity-design.md`（`use-approval` tag、`IApprovableBiz` 接口、objMeta 流程配置、codegen 骨架、两流正交分离、状态归业务处理、wf 回调串联、DIRECT/WORKFLOW 双模）
- 使用指南：`../nop-entropy/docs-for-ai/03-runbooks/enable-approval-on-entity.md`（让实体具备审批能力的分步手册）

本文只记录 ERP 特有的落位决策。审批四态语义、模式策略、委托规则归 `approval-framework.md`。

## ERP 审批能力使用方式

ERP 单据复用平台 `use-approval` 能力，无需自建：

1. ORM 实体标 `tagSet="use-approval"`（codegen 自动生成 `I*Biz extends IApprovableBiz` + 标准 action + `flowInstanceId` 字段）。
2. objMeta（xmeta）配 `wf:wfName`（`wf:` 命名空间属性）——有 `wfName` 走 WORKFLOW、无则 DIRECT；`wfVersion` 不配，默认用最新版本；**不用配置表**，随 xmeta 走 Delta 定制。
3. 在 xbiz 的 `approve` action source 用 `x:override="append"`（或 `<observes>`）注入 ERP 业务联动。
4. WORKFLOW 模式单据另配 `.xwf` + listener 回调（见平台使用指南）。

具体步骤见平台 runbook `enable-approval-on-entity.md`。

## ERP 审批实体落位

审批模式与适用单据的策略归 `approval-framework.md §按单据类型配置`。首批接入 **WORKFLOW**（多级审批）的实体需加 `flowInstanceId` 字段：

| 实体 | 域 | 模式（由 `wf:wfName` 有无决定） | 待加字段 |
|------|----|------------------------------|---------|
| 付款单 | finance | WORKFLOW（配 wfName） | `flowInstanceId` |
| 收款单 | finance | WORKFLOW（配 wfName） | `flowInstanceId` |
| 资产处置 | assets | WORKFLOW（配 wfName） | `flowInstanceId` |

DIRECT 模式单据（采购订单、销售订单等）标 `use-approval`、不配 `wf:wfName`，即获得标准审批 action，无需 `flowInstanceId`。无审批单据（库存移动单等）不标 `use-approval`。

> 现状：全量扫描 `<domain>/model/*.orm.xml`，**无任何实体已标 `use-approval` 或含 `flowInstanceId`**。首批接入需补。

## ERP 业务联动约定

ERP 审批通过后的业务联动**不写 Java 钩子，在 xbiz 层注入**（平台机制见 nop-entropy 使用指南）：在 `approve` action 的 source 用 `x:override="append"` 追加，或用 `<observes>` 监听 approve 触发。对接既有业财/库存机制：

| 联动 | 机制 | 文档 |
|------|------|------|
| 业财过账 | `PostingEvent` + `*PostingDispatcher`（单据 APPROVED 后组装 event 过账） | `docs/design/finance/posting.md` |
| 库存写入 | `IErpInvStockMoveBiz`（同事务强一致） | `docs/design/inventory/state-machine.md` |
| 单据状态机 | 三轴状态（docStatus/approveStatus/postedStatus） | `document-engine.md` + 各域 `state-machine.md` |

联动代码（xbiz source）内可调 task（`service-layer-orchestration.md`）或 `I*Biz`。审批状态（`approveStatus`）由平台标准 action source 迁移，**联动代码只做业务，不改 approveStatus**。

## 现有 DIRECT 实现的迁移

现有 DIRECT 模式审批（如 `ErpPurReceive/approve.task.xml`）在 task 内混合了状态迁移与业务联动。接入 `use-approval` 后：
- 通用状态迁移由平台标准 action source 处理（task 不再重复）
- 业务联动改为在 xbiz 的 `approve` action source 用 `x:override="append"`（或 `<observes>`）注入，注入的代码内可继续用 task 编排差异化逻辑

首批切片时重构既有 DIRECT 实体对齐此模式。

## 相关文档

- 平台设计：`../nop-entropy/ai-dev/design/nop-wf/approvable-entity-design.md`
- 平台使用指南：`../nop-entropy/docs-for-ai/03-runbooks/enable-approval-on-entity.md`
- `approval-framework.md` — ERP 审批策略（模式/单据配置/委托/四态语义）
- `document-engine.md` — 单据三轴状态与流转基线
- `service-layer-orchestration.md` — task.xml 编排约定
- `docs/design/finance/posting.md` — 业财过账机制
