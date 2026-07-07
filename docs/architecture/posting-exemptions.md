# 跨模块 IDaoProvider 写入豁免记录

> 本文件记录所有跨模块经 `IDaoProvider` 直接持久化目标域实体（而非注入 `I*Biz` 接口走 CRUD 管道）的架构特例。
> 这些场景绕过了目标域的审批管道和数据校验，需显式记录理由、风险和后续补偿机制。

## 豁免清单

### MrpReleaseService（manufacturing → purchase）

- **位置**：`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/mrp/MrpReleaseService.java`
- **写入目标**：`ErpPurOrder` + `ErpPurOrderLine`（purchase 域实体）
- **触发场景**：MRP 计划订单释放（`PURCHASE_REQUEST` 建议行 → 采购订单骨架草稿）
- **理由**：
  - MRP 自动释放不走人工审批，生成的采购单为骨架草稿（`docStatus=DRAFT`、`approveStatus=UNSUBMITTED`、单价/金额=0）
  - `IErpPurOrderBiz` 仅提供通用 `save(Map)`，无 purpose-built `createFromMrpLine` 方法
  - 通用 save 管道要求填齐所有必填字段并穿越 CRUD 校验，但 MRP 已知字段有限（物料/数量/日期/org）
- **风险**：
  - 生成的采购单单价比 0、金额为 0、币种由参数提供，须计划员/采购员后续完善
  - 绕过采购域的校验管道（如供应商有效性、币种匹配）
- **补偿机制**：
  - 生成的采购单状态为 `DRAFT`/`UNSUBMITTED`，不进入审批流，须采购员人工审核后提交
  - 权限校验在 `ErpMfgMrpPlanLineBizModel` 的 `@BizMutation` 入口完成
  - `@BizMutation` 自动事务保证 MRP 行 firmed 与目标单据生成原子
- **收敛条件（Successor）**：待采购域提供 purpose-built `createFromMrpLine` 时收敛为 I*Biz 调用

### ErpCtRebateSettlementBizModel（contract → purchase/sales）

- **位置**：`module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtRebateSettlementBizModel.java`
- **写入目标**：`ErpPurInvoice`/`ErpPurInvoiceLine`（purchase 域）或 `ErpSalInvoice`/`ErpSalInvoiceLine`（sales 域）
- **触发场景**：返利结算单过账时生成贷项凭证（负额发票）
- **理由**：
  - 贷项凭证以负额发票表达（`PURCHASE→AP 负额发票`、`SALES→AR 负额发票`），复用既有发票实体
  - 避免服务依赖级联（contract → purchase/sales service 依赖）
- **风险**：
  - 绕过采购/销售域的审批管道和数据校验
  - 负额发票直接以 `DRAFT`/`UNSUBMITTED` 状态生成
- **补偿机制**：
  - 贷项发票由结算单 `postSettlement` 的 `@BizMutation` 事务原子保证
  - 发票状态为 `DRAFT`，须人工审核后提交审批
  - 权限校验在 `ErpCtRebateSettlementBizModel` 的 `@BizMutation` 入口完成
- **收敛条件（Successor）**：待 purchase/sales 域提供 purpose-built `createCreditMemo` 时收敛为 I*Biz 调用

## 审计追踪

- 计划来源：`docs/plans/2026-07-07-2359-1-open-ended-audit-remediation.md` Phase 3（O-4）
- 审计发现：开放式对抗审计 O-4（跨模块 IDaoProvider 写入绕过审批）
- 状态：已记录豁免 + 补偿机制就位
