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

### ErpB2bAsnBizModel（b2b → purchase）

- **位置**：`module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/entity/ErpB2bAsnBizModel.java:215,226`（头创建 + 行级回填 `fillReceiveLinesFromAsn` 内 :266 `daoFor(ErpPurReceiveLine.class)`）
- **写入目标**：`ErpPurReceive` + `ErpPurReceiveLine`（purchase 域实体）
- **触发场景**：ASN 匹配采购订单后，config-gated 自动创建采购入库草稿（`createReceiveFromAsn`，ASN `MATCHED`→`RECEIVED_TO_STOCK`）
- **config-gated**：`erp-b2b.asn-auto-create-receive`（默认 `false`，见 `ErpB2bConfigs.CONFIG_ASN_AUTO_CREATE_RECEIVE`/`DEFAULT_ASN_AUTO_CREATE_RECEIVE`）；关闭时跳过创建、不阻断业务事实
- **理由**：
  - ASN→采购入库是 B2B 入站自动化场景，生成的接收单为骨架草稿（`docStatus=UNSUBMITTED`、`approveStatus=UNSUBMITTED`、`receiveStatus=NOT_RECEIVED`），不进入审批流
  - **核心零污染**：不在 `ErpPurReceive` 加 `asnId` 列，仅弱指针 `orderId`（→PO→ASN 经 `relatedBillCode` 反查），避免污染采购域实体
  - 避免服务依赖级联（b2b→purchase service 依赖），保持 b2b 单模块测试独立性（见 `docs/analysis/governed-path-cost-evaluation.md`）
- **风险**：
  - 绕过采购域的审批管道和数据校验（如接收单字段完整性、仓库有效性）
  - 生成的接收单单 价/金额经 PO 行反查派生（`fillReceiveLinesFromAsn`），非采购员手填
- **补偿机制**：
  - 生成的接收单状态为 `UNSUBMITTED`/`NOT_RECEIVED`，须采购员人工审核后提交，不自动入库
  - config-gated 默认关闭（`erp-b2b.asn-auto-create-receive=false`），仅在显式启用时触发
  - 权限校验在 `ErpB2bAsnBizModel.createReceiveFromAsn` 的 `@BizMutation` 入口完成
  - `@BizMutation` 自动事务保证 ASN 状态翻转（`MATCHED`→`RECEIVED_TO_STOCK`）与目标单据生成原子；任一行守卫触发（如物料缺失 `ERR_B2B_ASN_LINE_MATERIAL_REQUIRED`）即整体回滚
- **收敛条件（Successor）**：待采购域提供 purpose-built `createFromAsn` I*Biz 时收敛为 I*Biz 调用（前置条件：nop-entropy 平台层 lazy/SPI 解耦，避免破坏 b2b 单模块测试）

## 审计追踪

- 计划来源：`docs/plans/2026-07-07-2359-1-open-ended-audit-remediation.md` Phase 3（O-4）
- 审计发现：开放式对抗审计 O-4（跨模块 IDaoProvider 写入绕过审批）
- 状态：已记录豁免 + 补偿机制就位
- 补登（2026-07-24）：`ErpB2bAsnBizModel`（b2b→pur）半治理豁免补登，源自架构治理审查 `docs/audits/2026-07-23-0000-architecture-governance-review.md` F1 闭包项 #2（计划 `2026-07-24-0930-3` Phase 2）
