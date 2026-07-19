# 合同管理域（Contract）视图按钮需求覆盖分析

## 分析范围

本域共 15 个实体，按业务用途分类：

| 核心主实体 | 子实体/明细 | 配置/辅助实体 |
|-----------|------------|-------------|
| ErpCtContract | ErpCtContractLine | ErpCtTemplate |
| — | ErpCtContractVersion | ErpCtApprovalMatrix |
| — | ErpCtInvoicePlan | ErpCtApprovalRecord |
| — | ErpCtConsumptionLine | ErpCtDocument |
| — | ErpCtVolumeDiscount | ErpCtSignatureRequest |
| — | ErpCtRebateAgreement | — |
| — | ErpCtRebateTier | — |
| — | ErpCtRebateAccrual | — |
| — | ErpCtRebateSettlement | — |

**映射说明**：任务所述的 "Amendment" 无独立实体，由 Contract.parentContractId + ContractVersion 机制实现（`state-machine.md` 场景 B）；"Milestone" 是 InvoicePlan 字典值 `erp-ct/invoice-term` 的选项，非实体；"Clause" 嵌入 Template.contentTemplate 字段，无独立实体；"Approval" 对应 ErpCtApprovalMatrix（审批规则配置）和 ErpCtApprovalRecord（审批日志）。

## 期望按钮推导依据

1. **CRUD 基线**（METHODOLOGY §1.1）：所有实体默认期望 `add-button`、`batch-delete-button`、`row-view-button`、`row-update-button`、`row-delete-button`。
2. **工作流按钮**（domain-design-guidelines.md §1.2 + §16.2 + state-machine.md）：ErpCtContract 有完整生命周期 `DRAFT→NEGOTIATION→ACTIVE→SUSPENDED/EXPIRED/TERMINATED`，预期拥有审批流按钮（submit/approve/reject/cancel）和状态迁移专用按钮（suspend/terminate/resume）。
3. **域专用按钮**（ui-patterns.md §详情页）：合同详情页应包含 `[提交审批]`、`[签署]`、`[中止]`、`[终止]`、`[打印]`。版本历史 Tab 应有 `[+新建版本]`。开票计划应有 `[+新建开票计划]`、`[批量生成]`。
4. **版本审批**（state-machine.md §版本管理 + ui-patterns.md §版本历史）：ErpCtContractVersion 有 DRAFT/FINALIZED/SIGNED 三态，预期 submit/approve 流程。
5. **电子签章**（state-machine.md §电子签章 + ORM 字典 `erp-ct/sign-status`）：ErpCtSignatureRequest 有 6 态签章状态机，预期有签章发起/完成/拒签等动作。
6. **返利结算**（ORM）：ErpCtRebateSettlement 有 posted 字段和 DRAFT/POSTED/CANCELLED 三态，预期有过账操作按钮。
7. **[导出]** 按 ui-patterns.md 列表页模板列出，属 info 级差距。

## 逐实体分析

### ErpCtContract — CRUD+WF（严重差距）
- **期望按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`, `row-submit-button`, `row-approve-button`（签署）, `row-reject-button`, `row-cancel-button`（作废）, `row-suspend-button`（中止）, `row-terminate-button`（终止）, `row-resume-button`（中止恢复）, `row-reverse-approve-button`（反审核）
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：
  - `row-submit-button`: missing (**blocker**) — ui-patterns.md §详情页明确列出 `[提交审批]`，state-machine §迁移 DRAFT→NEGOTIATION 需此按钮
  - `row-approve-button`（签署）: missing (**blocker**) — ui-patterns.md §详情页明确列出 `[签署]`，state-machine §迁移 NEGOTIATION→ACTIVE 需此按钮
  - `row-reject-button`: missing (**blocker**) — domain-design-guidelines.md §1.2 标准 WF 按钮，合同谈判阶段需驳回功能
  - `row-cancel-button`: missing (**blocker**) — domain-design-guidelines.md §1.2 标准 WF 按钮，state-machine §迁移 DRAFT→CANCELLED 需此按钮
  - `row-suspend-button`: missing (**major**) — ui-patterns.md §详情页明确列出 `[中止]`，state-machine §迁移 ACTIVE→SUSPENDED
  - `row-terminate-button`: missing (**major**) — ui-patterns.md §详情页明确列出 `[终止]`，state-machine §迁移 ACTIVE/NEGOTIATION→TERMINATED
  - `row-resume-button`: missing (**major**) — state-machine §迁移 SUSPENDED→ACTIVE
  - `row-reverse-approve-button`: missing (**minor**) — domain-design-guidelines.md §1.2 标准 WF 按钮（反审核）
  - `row-more-button`: present — but actions group only contains update/delete, WF actions would slot in here
  - `打印`: missing (**info**) — ui-patterns.md §详情页 toolbar 列出 `[打印]`
  - `导出`: missing (**info**) — ui-patterns.md §列表页列出 `[导出]`
- **判定**：**blocker** — 合同作为本域核心主实体，缺失全部 7 个 WF 按钮，无任何状态迁移能力

### ErpCtContractLine — CRUD
- **期望按钮**：CRUD 基线（明细行通常由主子表管理，独立 CRUD 页只需基线）
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：无
- **判定**：**clean**

### ErpCtContractVersion — CRUD+WF
- **期望按钮**：CRUD 基线 + `row-submit-button`（DRAFT→FINALIZED）、`row-approve-button`（FINALIZED→SIGNED）。state-machine.md §版本管理："版本审批：草稿→定稿→已签署"，ui-patterns.md §版本历史：`[设为当前]`/`[对比]` 为域专用按钮。
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：
  - `row-submit-button`: missing (**major**) — 版本有 DRAFT→FINALIZED 状态迁移（state-machine.md §版本审批 + ui-patterns.md "草稿→定稿→已签署"）
  - `row-approve-button`（定稿→签署）: missing (**major**) — 版本终态签署需审批动作
  - `row-set-current-button`（域专用）: missing (**minor**) — ui-patterns.md §版本历史 `[设为当前]`
  - `row-compare-button`（域专用）: missing (**info**) — ui-patterns.md §版本历史 `[对比]`，属导航/信息面板，非核心 action
- **判定**：**major**

### ErpCtInvoicePlan — CRUD
- **期望按钮**：CRUD 基线 + `row-generate-invoice-button`（生成发票，跨域操作）。ui-patterns.md §开票计划也有 `[批量生成]`。
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：
  - `row-generate-invoice-button`: missing (**info**) — ui-patterns.md §开票计划提到"批量生成"，属跨域发票生成
  - `批量生成`: missing (**info**) — ui-patterns.md §开票计划明确列出 `[批量生成]` 按钮
- **判定**：**minor**（CRUD 完整，缺少批量和触发操作属可增强点）

### ErpCtConsumptionLine — CRUD
- **期望按钮**：CRUD 基线（ui-patterns.md §消耗记录描述为外部系统写入，只读查看为主，但仍需基线）
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：无（尽管消耗记录为外部系统写入，CRUD 基线可作为管理入口保留）
- **判定**：**clean**

### ErpCtTemplate — CRUD
- **期望按钮**：CRUD 基线（配置类实体）
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：无
- **判定**：**clean**

### ErpCtApprovalMatrix — CRUD
- **期望按钮**：CRUD 基线（配置类实体，审批规则配置表）
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：无
- **判定**：**clean**

### ErpCtApprovalRecord — CRUD
- **期望按钮**：CRUD 基线（审计日志类实体，理论上不应手动增删改，但 CRUD 基线可作为管理入口）
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：无（注意：此实体有 add/update/delete 按钮，但审批记录属于系统写入的审计日志，手动编辑应受限制。当前不视作按钮差距，但建议业务层面禁止手动新增/编辑审批记录）
- **判定**：**clean**（业务约束层面建议关注，但不属于按钮覆盖问题）

### ErpCtDocument — CRUD
- **期望按钮**：CRUD 基线（文档仓库实体）
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：无
- **判定**：**clean**

### ErpCtVolumeDiscount — CRUD
- **期望按钮**：CRUD 基线（ContractLine 子配置表）
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：无
- **判定**：**clean**

### ErpCtRebateAgreement — CRUD+WF
- **期望按钮**：CRUD 基线 + `row-submit-button`（DRAFT→ACTIVE）、`row-approve-button`、`row-cancel-button`（→CANCELLED）。ORM 显示其有 DRAFT/ACTIVE/EXPIRED/SETTLED 四态状态机。
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：
  - `row-submit-button`: missing (**major**) — 返利协议有 DRAFT→ACTIVE 状态迁移
  - `row-approve-button`: missing (**minor**) — 协议生效需审批确认
  - `row-cancel-button`: missing (**minor**) — 协议取消场景
- **判定**：**major**

### ErpCtRebateTier — CRUD
- **期望按钮**：CRUD 基线（RebateAgreement 子表）
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：无
- **判定**：**clean**

### ErpCtRebateAccrual — CRUD
- **期望按钮**：CRUD 基线（计提明细，系统写入）
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：无
- **判定**：**clean**

### ErpCtRebateSettlement — CRUD+Custom
- **期望按钮**：CRUD 基线 + `row-post-button`（DRAFT→POSTED）。ORM 显示有 posted 字段和 DRAFT/POSTED/CANCELLED 三态。
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：
  - `row-post-button`: missing (**minor**) — 结算单有过账需求与 posted 字段
  - `row-cancel-button`: missing (**minor**) — CANCELLED 状态迁移
- **判定**：**minor**

### ErpCtSignatureRequest — CRUD+Custom
- **期望按钮**：CRUD 基线 + `row-sign-button`（发起签章）、`row-recall-button`（撤销）、`row-view-certificate-button`（查看证书）。ORM 字典 `erp-ct/sign-status` 有 PENDING_SIGNATURE / PARTIALLY_SIGNED / FULLY_SIGNED / REJECTED / EXPIRED / CANCELLED 六态签章状态机。
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：
  - `row-sign-button`: missing (**major**) — 签章发起的核心操作
  - `row-recall-button`: missing (**minor**) — 签章撤销
  - `row-view-certificate-button`: missing (**info**) — 查看完成证书，属导航
- **判定**：**major**

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| CRUD+WF | ErpCtContract | 9 | **blocker** | 缺失全部 7 个 WF 按钮，合同无任何状态迁移能力 |
| CRUD+WF | ErpCtContractVersion | 4 | **major** | 版本审批流程 DRAFT→FINALIZED→SIGNED 无对应按钮 |
| CRUD+WF | ErpCtRebateAgreement | 3 | **major** | DRAFT→ACTIVE 状态迁移无按钮 |
| CRUD+Custom | ErpCtSignatureRequest | 3 | **major** | 签章 6 态状态机无对应操作按钮 |
| CRUD+Custom | ErpCtRebateSettlement | 2 | **minor** | 过账/取消操作无按钮 |
| CRUD | ErpCtInvoicePlan | 2 | **minor** | 缺少批量生成/开票按钮 |
| CRUD | ErpCtContractLine | 0 | clean | — |
| CRUD | ErpCtConsumptionLine | 0 | clean | — |
| CRUD | ErpCtTemplate | 0 | clean | — |
| CRUD | ErpCtApprovalMatrix | 0 | clean | — |
| CRUD | ErpCtApprovalRecord | 0 | clean | [注] 业务层面应限制手动编辑审批记录 |
| CRUD | ErpCtDocument | 0 | clean | — |
| CRUD | ErpCtVolumeDiscount | 0 | clean | — |
| CRUD | ErpCtRebateTier | 0 | clean | — |
| CRUD | ErpCtRebateAccrual | 0 | clean | — |

### 总评
- 总实体数：15
- 无差距实体：9（60.0%）
- Blocker 差距：1（ErpCtContract）
- Major 差距：3（ErpCtContractVersion、ErpCtRebateAgreement、ErpCtSignatureRequest）
- Minor/Info 差距：3（ErpCtInvoicePlan、ErpCtRebateSettlement + ErpCtContract 的 info 项）

**核心发现**：ErpCtContract 作为本域核心主实体，虽有完整 6 态生命周期（DRAFT→NEGOTIATION→ACTIVE→SUSPENDED/EXPIRED/TERMINATED）和 ui-patterns.md 明确列举的 `[提交审批]`/`[签署]`/`[中止]`/`[终止]` 按钮，当前 view.xml 仅具有 CRUD 基线，无任何工作流/状态迁移按钮。这导致合同生命周期管理完全不可操作——合同创建后无法提交审批、签署、中止或终止。同时，3 个拥有独立状态机的实体（ContractVersion、RebateAgreement、SignatureRequest）也缺失对应的状态迁移按钮，需在下一阶段统一补充。
