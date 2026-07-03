# Extended Domains Business Logic Roadmap

> 最后更新：2026-07-03
> 本路线图覆盖**核心 5 域之外**的 13 域自定义 BizModel 方法与编排逻辑。
> 前置条件：`crud-roadmap.md` 中对应域的 CRUD 已完成。

## Work Item Status

> 状态在工作项上；Milestone 仅为分组。

### Milestone M2 — 扩展 5 域
- 2.5：✅ done（资产折旧/处置/资本化 BizModel + 业财过账，2026-07-02，`docs/plans/2026-07-02-1000-2-assets-depreciation-disposal-capitalization.md`）
- 2.1：✅ done（BOM/工艺路线 BizModel：默认 BOM 选择 + 多级展开 phantom/环/深度 + 成本卷算 → ErpMfgCostRollup/Line；含工时/费率列类型修正，2026-07-02，`docs/plans/2026-07-02-1538-2-manufacturing-bom-routing-rollup.md`）
- 2.2：✅ done（WorkOrder/JobCard 状态机：10 态工单状态机 + 三轴审批 + 齐套校验 + 领料出库/报工/完工入库 + 成本归集 + 完工质检 config-gated 钩子；含工时/费率/实领数量列类型修正，2026-07-03，`docs/plans/2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md`）
- 2.3：✅ done（MRP 计算引擎：需求整合(销售订单/安全库存/手工)→BOM 多级展开→净需求→按期分单(lot-for-lot/固定批量)→计划订单(WORK_ORDER_REQUEST/PURCHASE_REQUEST)→释放转采购订单/工单，2026-07-03，`docs/plans/2026-07-02-2237-2-manufacturing-mrp-engine.md`）
- 2.4, 2.6–2.11：`todo`

### Milestone M3 — 新增 8 域
- 3.1–3.21：`todo`

## Implementation Order

### M2 — 扩展 5 域

| # | 工作项 | 域 | 设计文档 |
|---|--------|-----|---------|
| 2.1 | BOM/工艺路线 BizModel | manufacturing | `bom-and-routing.md` |
| 2.2 | WorkOrder/JobCard 状态机 + 审批 | manufacturing | `manufacturing/state-machine.md` |
| 2.3 | MRP 计算引擎 | manufacturing | `manufacturing/mrp.md` |
| 2.4 | 质检触发 + NCR/CAPA 流程 | quality | `quality/state-machine.md` |
| 2.5 | Asset 折旧/处置/资本化 | assets | `assets/state-machine.md` |
| 2.6 | Project 成本归集 | projects | `projects/cost-collection.md` |
| 2.7 | 维护计划/停机/备件消耗 | maintenance | `maintenance/state-machine.md` |
| 2.8 | CRP 负荷计算 | manufacturing | `manufacturing/crp.md` |
| 2.9 | 供应商评分卡计算 | purchase | `purchase/supplier-evaluation.md` |
| 2.10 | VMI 所有权转移 | inventory | `inventory/consignment.md` |
| 2.11 | 批次召回事件 | quality | `quality/recall.md` |

### M3 — 新增 8 域

| # | 工作项 | 域 | 设计文档 |
|---|--------|-----|---------|
| 3.1 | CRM Lead→Opportunity→Quotation 转化 | crm | `crm/README.md` |
| 3.2 | CRM 活动日历/事件提醒 | crm | `crm/README.md` |
| 3.3 | CRM 线索评分引擎 | crm | `crm/lead-scoring.md` |
| 3.4 | CRM 销售预测 | crm | `crm/sales-forecast.md` |
| 3.5 | 客服 Ticket + SLA 计时 | customer-service | `customer-service/README.md`, `customer-service/sla.md` |
| 3.6 | 客服满意度调查 | customer-service | `customer-service/csat.md` |
| 3.7 | HR 薪酬核算 + 个税计算 | human-resource | `human-resource/payroll.md` |
| 3.8 | HR 排班管理 | human-resource | `human-resource/shift-scheduling.md` |
| 3.9 | HR 薪酬模拟 | human-resource | `human-resource/payroll-simulation.md` |
| 3.10 | APS OperationOrder 排产引擎 | aps | `aps/scheduling.md` |
| 3.11 | APS ATP/CTP 交期承诺 | aps | `aps/scheduling.md` |
| 3.12 | 合同版本管理 + InvoicePlan 触发发票 | contract | `contract/README.md` |
| 3.13 | 合同电子签章 | contract | `contract/e-signature.md` |
| 3.14 | 合同批量折扣/返利计算 | contract | `contract/volume-discount.md` |
| 3.15 | DRP 净需求计算 | drp | `drp/README.md` |
| 3.16 | DRP 安全库存优化 | drp | `drp/safety-stock-optimization.md` |
| 3.17 | TMS 承运商网关三层 SPI | logistics | `logistics/carrier-integration.md` |
| 3.18 | TMS 运费双路径过账 | logistics | `logistics/README.md` |
| 3.19 | B2B EDI 格式 SPI + 信封状态机 | b2b | `b2b/edi-formats.md` |
| 3.20 | B2B ASN 入站处理 | b2b | `b2b/asn-processing.md` |
| 3.21 | B2B MFT AS2/SFTP 传输 | b2b | `b2b/managed-file-transfer.md` |
