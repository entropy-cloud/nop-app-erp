# CJK 硬编码检测基线（CJK Baseline）

> Owner: `tools/check-hardcoded-cjk.mjs`（checker）+ `docs/architecture/i18n-compliance.md`（判定准绳唯一权威 owner doc，ai-check-r3 M0.1 产出）
> 基线落盘日期: 2026-09-06
> 基线来源: plan `docs/plans/2026-09-06-1451-2-cjk-detection-baseline.md` M0.2/M0.3 实测
> 对照范式: `docs/audits/compliance-baseline.md`（数值基线 + 单向收紧 + 调高须独立计划裁决）
> 消费方: ai-check-r3 MI.x 清剿批（红线/验收）、MV.1 全量回归（`--strict` 全绿门控）

## 用途

本文件是 CJK 硬编码检测的**回归门控基线**：`node tools/check-hardcoded-cjk.mjs --strict` 将实际扫描结果与本文件 `## SNAPSHOT (machine-readable)

> 本块由 `node tools/check-hardcoded-cjk.mjs --baseline` 自动整体重生成（勿手改）。
> 门控方向：单向收紧（actual 只降不升）；调高须独立计划裁决 + per-site 证据（对齐 docs/audits/compliance-baseline.md 范式）。
> `--strict` 解析本块做 per-file per-CAT 计数比对：任何文件任一 CAT 计数高于快照 = 新增违规 = 非零退出。

```yaml
generated: 2026-09-07T13:23:28.378Z
scanned: { javaFiles: 3430, yamlFiles: 886 }
totals: { CAT1: 0, CAT2: 0, CAT3: 209, CAT4: 1318 }
domains:
  app-erp-all: { CAT1: 0, CAT2: 0, CAT3: 0, CAT4: 0 }
  aps: { CAT1: 0, CAT2: 0, CAT3: 20, CAT4: 35 }
  assets: { CAT1: 0, CAT2: 0, CAT3: 0, CAT4: 87 }
  b2b: { CAT1: 0, CAT2: 0, CAT3: 15, CAT4: 91 }
  common-service: { CAT1: 0, CAT2: 0, CAT3: 8, CAT4: 0 }
  common-test: { CAT1: 0, CAT2: 0, CAT3: 0, CAT4: 0 }
  contract: { CAT1: 0, CAT2: 0, CAT3: 6, CAT4: 50 }
  crm: { CAT1: 0, CAT2: 0, CAT3: 2, CAT4: 79 }
  cs: { CAT1: 0, CAT2: 0, CAT3: 0, CAT4: 120 }
  drp: { CAT1: 0, CAT2: 0, CAT3: 0, CAT4: 41 }
  finance: { CAT1: 0, CAT2: 0, CAT3: 0, CAT4: 0 }
  hr: { CAT1: 0, CAT2: 0, CAT3: 28, CAT4: 118 }
  inventory: { CAT1: 0, CAT2: 0, CAT3: 28, CAT4: 93 }
  logistics: { CAT1: 0, CAT2: 0, CAT3: 3, CAT4: 18 }
  maintenance: { CAT1: 0, CAT2: 0, CAT3: 13, CAT4: 122 }
  manufacturing: { CAT1: 0, CAT2: 0, CAT3: 28, CAT4: 96 }
  master-data: { CAT1: 0, CAT2: 0, CAT3: 2, CAT4: 74 }
  notify: { CAT1: 0, CAT2: 0, CAT3: 1, CAT4: 42 }
  projects: { CAT1: 0, CAT2: 0, CAT3: 15, CAT4: 0 }
  purchase: { CAT1: 0, CAT2: 0, CAT3: 14, CAT4: 99 }
  quality: { CAT1: 0, CAT2: 0, CAT3: 13, CAT4: 114 }
  sales: { CAT1: 0, CAT2: 0, CAT3: 13, CAT4: 39 }
files:
  module-aps/erp-aps-dao/src/main/java/app/erp/aps/biz/IErpApsOperationOrderBiz.java: { CAT3: 1 }
  module-aps/erp-aps-service/src/main/java/app/erp/aps/service/atpctp/ErpApsAtpCtpServiceImpl.java: { CAT3: 3 }
  module-aps/erp-aps-service/src/main/java/app/erp/aps/service/entity/ErpApsOperationOrderBizModel.java: { CAT3: 1 }
  module-aps/erp-aps-service/src/main/java/app/erp/aps/service/processor/ErpApsWorkOrderToOperationProcessor.java: { CAT3: 1 }
  module-aps/erp-aps-service/src/main/java/app/erp/aps/service/scheduling/ErpApsSchedulingEngine.java: { CAT3: 14 }
  module-aps/erp-aps-web/src/main/resources/_vfs/erp/aps/pages/dashboard/schedule-gantt.flux.yaml: { CAT4: 13 }
  module-aps/erp-aps-web/src/main/resources/_vfs/erp/aps/pages/dashboard/schedule-gantt.page.yaml: { CAT4: 22 }
  module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/asset-repair/main.page.yaml: { CAT4: 2 }
  module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/asset-stocktake/main.page.yaml: { CAT4: 14 }
  module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/dashboard/main.flux.yaml: { CAT4: 18 }
  module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/dashboard/main.page.yaml: { CAT4: 19 }
  module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/disposal-wizard/main.page.yaml: { CAT4: 20 }
  module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/report/asset-depreciation-detail.page.yaml: { CAT4: 8 }
  module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/report/asset-disposal-detail.page.yaml: { CAT4: 6 }
  module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/entity/ErpB2bEdiDocBizModel.java: { CAT3: 5 }
  module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/entity/ErpB2bPartnerProfileBizModel.java: { CAT3: 2 }
  module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bAsnCreateReceiveFromAsnProcessor.java: { CAT3: 2 }
  module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bAsnHandleInboundWebhookProcessor.java: { CAT3: 1 }
  module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bAsnMatchPurchaseOrderProcessor.java: { CAT3: 2 }
  module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bEdiDocCreateInboundProcessor.java: { CAT3: 1 }
  module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bEdiDocCreateOutboundProcessor.java: { CAT3: 1 }
  module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/spi/transport/TransportManager.java: { CAT3: 1 }
  module-b2b/erp-b2b-web/src/main/resources/_vfs/erp/b2b/pages/dashboard/asn-flow.flux.yaml: { CAT4: 27 }
  module-b2b/erp-b2b-web/src/main/resources/_vfs/erp/b2b/pages/dashboard/asn-flow.page.yaml: { CAT4: 32 }
  module-b2b/erp-b2b-web/src/main/resources/_vfs/erp/b2b/pages/dashboard/edi-detail.flux.yaml: { CAT4: 11 }
  module-b2b/erp-b2b-web/src/main/resources/_vfs/erp/b2b/pages/dashboard/edi-detail.page.yaml: { CAT4: 21 }
  module-common-service/src/main/java/app/erp/common/org/ErpOrgIsolationQueryTransformer.java: { CAT3: 1 }
  module-common-service/src/main/java/app/erp/common/service/MaskHelper.java: { CAT3: 7 }
  module-contract/erp-ct-service/src/main/java/app/erp/ct/service/ErpCtConfigs.java: { CAT3: 1 }
  module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtContractBizModel.java: { CAT3: 1 }
  module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtDocumentBizModel.java: { CAT3: 2 }
  module-contract/erp-ct-service/src/main/java/app/erp/ct/service/spi/manual/ManualOcrEngine.java: { CAT3: 1 }
  module-contract/erp-ct-service/src/main/java/app/erp/ct/service/spi/mock/MockSignatureProvider.java: { CAT3: 1 }
  module-contract/erp-ct-web/src/main/resources/_vfs/erp/ct/pages/dashboard/version-diff.flux.yaml: { CAT4: 18 }
  module-contract/erp-ct-web/src/main/resources/_vfs/erp/ct/pages/dashboard/version-diff.page.yaml: { CAT4: 32 }
  module-crm/erp-crm-service/src/main/java/app/erp/crm/service/processor/ErpCrmConversionProcessor.java: { CAT3: 1 }
  module-crm/erp-crm-service/src/main/java/app/erp/crm/service/support/FunnelAggregationEngine.java: { CAT3: 1 }
  module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/ErpCrmActivity/calendar.flux.yaml: { CAT4: 6 }
  module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/ErpCrmActivity/calendar.page.yaml: { CAT4: 12 }
  module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/ErpCrmActivity/timeline.flux.yaml: { CAT4: 6 }
  module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/ErpCrmActivity/timeline.page.yaml: { CAT4: 7 }
  module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/ErpCrmLead/opportunity-kanban.flux.yaml: { CAT4: 5 }
  module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/ErpCrmLead/opportunity-kanban.page.yaml: { CAT4: 18 }
  module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/lead-conversion/main.page.yaml: { CAT4: 11 }
  module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/report/campaign-attribution.page.yaml: { CAT4: 4 }
  module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/report/forecast-accuracy.page.yaml: { CAT4: 6 }
  module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/report/lead-conversion-funnel.page.yaml: { CAT4: 4 }
  module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsQualityDashboard/main.flux.yaml: { CAT4: 21 }
  module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsQualityDashboard/main.page.yaml: { CAT4: 20 }
  module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicket/kanban.flux.yaml: { CAT4: 10 }
  module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicket/kanban.page.yaml: { CAT4: 48 }
  module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicketAction/timeline.flux.yaml: { CAT4: 6 }
  module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicketAction/timeline.page.yaml: { CAT4: 9 }
  module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/report/ticket-sla-csat-summary.page.yaml: { CAT4: 6 }
  module-drp/erp-drp-web/src/main/resources/_vfs/erp/drp/pages/dashboard/net-requirement.flux.yaml: { CAT4: 20 }
  module-drp/erp-drp-web/src/main/resources/_vfs/erp/drp/pages/dashboard/net-requirement.page.yaml: { CAT4: 21 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/ErpHrConstants.java: { CAT3: 1 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/entity/ErpHrDevelopmentPlanBizModel.java: { CAT3: 1 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/entity/ErpHrRecruitmentBizModel.java: { CAT3: 2 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/entity/ErpHrSalarySimulationBizModel.java: { CAT3: 3 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/posting/SalaryPostingDispatcher.java: { CAT3: 4 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/posting/SalaryPostingProvider.java: { CAT3: 8 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/processor/AbstractErpHrDevelopmentPlanProcessor.java: { CAT3: 1 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/processor/ErpHrDevelopmentPlanGenerateDevelopmentPlanProcessor.java: { CAT3: 1 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/processor/ErpHrRecruitmentHireProcessor.java: { CAT3: 2 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/processor/ErpHrSalaryGenerateBankFileProcessor.java: { CAT3: 1 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/processor/ErpHrSalarySimulationConvertToFormalProcessor.java: { CAT3: 2 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/report/ErpHrReportBizModel.java: { CAT3: 2 }
  module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/ErpHrLeaveRequest/team-vacation-calendar.flux.yaml: { CAT4: 4 }
  module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/ErpHrLeaveRequest/team-vacation-calendar.page.yaml: { CAT4: 9 }
  module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/dashboard/org-chart.flux.yaml: { CAT4: 7 }
  module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/dashboard/org-chart.page.yaml: { CAT4: 6 }
  module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/dashboard/payroll-approval.flux.yaml: { CAT4: 44 }
  module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/dashboard/payroll-approval.page.yaml: { CAT4: 38 }
  module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/report/employee-net-balance.page.yaml: { CAT4: 4 }
  module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/report/payroll-simulation-comparison.page.yaml: { CAT4: 6 }
  module-inventory/erp-inv-dao/src/main/java/app/erp/inv/biz/IErpInvStockLedgerBiz.java: { CAT3: 2 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/CostAdjustmentService.java: { CAT3: 1 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/dashboard/ErpInvDashboardBizModel.java: { CAT3: 1 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/entity/ErpInvStockLedgerBizModel.java: { CAT3: 2 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/CostAdjustmentAcctDocProvider.java: { CAT3: 4 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/InvAcctDocProvider.java: { CAT3: 6 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/InvOwnershipTransferProvider.java: { CAT3: 2 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/LandedCostAcctDocProvider.java: { CAT3: 2 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/PurchasePriceVarianceAcctDocProvider.java: { CAT3: 4 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvLandedCostProcessor.java: { CAT3: 2 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvStockMoveReverseProcessor.java: { CAT3: 1 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvStockTakeCompleteTakeProcessor.java: { CAT3: 1 }
  module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/dashboard/main.flux.yaml: { CAT4: 31 }
  module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/dashboard/main.page.yaml: { CAT4: 31 }
  module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/report/inventory-trace-report.page.yaml: { CAT4: 10 }
  module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/stock-take-flow/main.page.yaml: { CAT4: 21 }
  module-logistics/erp-log-service/src/main/java/app/erp/log/service/gateway/GatewayDispatcher.java: { CAT3: 1 }
  module-logistics/erp-log-service/src/main/java/app/erp/log/service/posting/LogisticsFreightProvider.java: { CAT3: 2 }
  module-logistics/erp-log-web/src/main/resources/_vfs/erp/log/pages/dashboard/shipment-tracking.flux.yaml: { CAT4: 7 }
  module-logistics/erp-log-web/src/main/resources/_vfs/erp/log/pages/dashboard/shipment-tracking.page.yaml: { CAT4: 11 }
  module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/dashboard/ErpMntDashboardBizModel.java: { CAT3: 1 }
  module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MaintenanceIssueAcctDocProvider.java: { CAT3: 4 }
  module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MaintenanceLaborAcctDocProvider.java: { CAT3: 6 }
  module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/processor/ErpMntVisitReportAdditionalFaultProcessor.java: { CAT3: 1 }
  module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/report/ErpMntReportBizModel.java: { CAT3: 1 }
  module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/dashboard/main.flux.yaml: { CAT4: 23 }
  module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/dashboard/main.page.yaml: { CAT4: 24 }
  module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/report/downtime-summary.page.yaml: { CAT4: 8 }
  module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/report/maintenance-history.page.yaml: { CAT4: 8 }
  module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/visit-wizard/main.flux.yaml: { CAT4: 25 }
  module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/visit-wizard/main.page.yaml: { CAT4: 34 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/dashboard/ErpMfgDashboardBizModel.java: { CAT3: 1 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/entity/ErpMfgBomBizModel.java: { CAT3: 1 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/ManufacturingIssueAcctDocProvider.java: { CAT3: 4 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/ProductionVarianceAcctDocProvider.java: { CAT3: 5 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/SubcontractFeeAcctDocProvider.java: { CAT3: 3 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/SubcontractIssueAcctDocProvider.java: { CAT3: 4 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/SubcontractPostingDispatcher.java: { CAT3: 3 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/SubcontractReceiptAcctDocProvider.java: { CAT3: 4 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgScheduleToJobCardProcessor.java: { CAT3: 1 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/simulation/SimulationMrpEngine.java: { CAT3: 2 }
  module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/dashboard/bom-tree.flux.yaml: { CAT4: 9 }
  module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/dashboard/bom-tree.page.yaml: { CAT4: 13 }
  module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/dashboard/main.flux.yaml: { CAT4: 24 }
  module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/dashboard/main.page.yaml: { CAT4: 26 }
  module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/report/crp-load-report.page.yaml: { CAT4: 8 }
  module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/report/forecast-variance-report.page.yaml: { CAT4: 8 }
  module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/report/production-variance-report.page.yaml: { CAT4: 8 }
  module-master-data/erp-md-dao/src/main/java/app/erp/md/dao/dto/ErpPartyType.java: { CAT3: 1 }
  module-master-data/erp-md-service/src/main/java/app/erp/md/service/dashboard/ErpMdDashboardBizModel.java: { CAT3: 1 }
  module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/cost-center/main.page.yaml: { CAT4: 14 }
  module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/dashboard/main.flux.yaml: { CAT4: 14 }
  module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/dashboard/main.page.yaml: { CAT4: 14 }
  module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/party-search/main.picker.page.yaml: { CAT4: 20 }
  module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/report/material-price-list.page.yaml: { CAT4: 6 }
  module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/report/partner-list.page.yaml: { CAT4: 6 }
  module-notify/erp-notify-service/src/main/java/app/erp/notify/service/dispatch/NotificationDispatcher.java: { CAT3: 1 }
  module-notify/erp-notify-web/src/main/resources/_vfs/erp/notify/pages/ErpSysNotification/inbox.page.yaml: { CAT4: 42 }
  module-projects/erp-prj-service/src/main/java/app/erp/prj/service/dashboard/ErpPrjDashboardBizModel.java: { CAT3: 1 }
  module-projects/erp-prj-service/src/main/java/app/erp/prj/service/entity/ErpPrjTaskBizModel.java: { CAT3: 1 }
  module-projects/erp-prj-service/src/main/java/app/erp/prj/service/posting/ProjectCostCollectionProvider.java: { CAT3: 3 }
  module-projects/erp-prj-service/src/main/java/app/erp/prj/service/posting/ProjectSettlementAcctDocProvider.java: { CAT3: 9 }
  module-projects/erp-prj-service/src/main/java/app/erp/prj/service/processor/ErpPrjProjectSettlementProcessor.java: { CAT3: 1 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/dashboard/ErpPurDashboardBizModel.java: { CAT3: 1 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/PaymentSettler.java: { CAT3: 1 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurAcctDocProvider.java: { CAT3: 11 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/support/ErpPurCtDiscountApplier.java: { CAT3: 1 }
  module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/dashboard/main.flux.yaml: { CAT4: 24 }
  module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/dashboard/main.page.yaml: { CAT4: 24 }
  module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/dashboard/three-way-match.flux.yaml: { CAT4: 22 }
  module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/dashboard/three-way-match.page.yaml: { CAT4: 29 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/dashboard/ErpQaDashboardBizModel.java: { CAT3: 1 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/entity/NcrLifecycleService.java: { CAT3: 3 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/posting/NcrReturnOrchestrator.java: { CAT3: 2 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/posting/NcrScrapAcctDocProvider.java: { CAT3: 2 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/processor/ErpQaNonConformanceUpgradeToRecallProcessor.java: { CAT3: 1 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/report/ErpQaReportBizModel.java: { CAT3: 1 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/spc/SpcCapabilityCalculator.java: { CAT3: 1 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/spc/SpcOutOfControlHandler.java: { CAT3: 2 }
  module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/dashboard/main.flux.yaml: { CAT4: 25 }
  module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/dashboard/main.page.yaml: { CAT4: 28 }
  module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/ncr-disposal/main.page.yaml: { CAT4: 2 }
  module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/report/inspection-summary.page.yaml: { CAT4: 8 }
  module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/report/ncr-capa-summary.page.yaml: { CAT4: 6 }
  module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/spc-capability/main.page.yaml: { CAT4: 12 }
  module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/spc-chart/main.page.yaml: { CAT4: 17 }
  module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/spc-sample/main.page.yaml: { CAT4: 16 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/dashboard/ErpSalDashboardBizModel.java: { CAT3: 1 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ReceiptSettler.java: { CAT3: 1 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/posting/SalAcctDocProvider.java: { CAT3: 7 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalReturnGenerateExchangeDeliveryProcessor.java: { CAT3: 3 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/support/ErpSalPricingRuleEngine.java: { CAT3: 1 }
  module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/dashboard/main.flux.yaml: { CAT4: 19 }
  module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/dashboard/main.page.yaml: { CAT4: 20 }
```




## 批注账 (MI.x Batch Ledger)

> MI.x 分批执行协议义务（`docs/backlog/ai-check-r3-roadmap.md` MI.6 行）：每批完成记入本账，MI.6 保持 todo 直至 CAT-3 = 0 或白名单全覆盖。红线→归零逐域对账以各批 plan 勾选注记与脚本实跑数字为准。

| 批次 | Plan | 范围 | 红线（批前） | 批后 | 对账 | 验证 |
|------|------|------|--------------|------|------|------|
| MI.6 批 1/2 | `docs/plans/2026-09-07-0902-1-mi6-runtime-string-cat3-batch1.md`（2026-09-07） | finance 80/24 + assets 58/21 + cs 43/11 = **181 行 / 56 文件** | 全局 CAT3=390（CAT1=0/CAT2=0/CAT4=1700） | finance **0** / assets **0** / cs **0**；全局 CAT3 **209**（CAT1/2/4 持平 0/0/1700） | 181 → 0 逐域归零；白名单登记 11 文件（fin 5 / ast 4 / cs 2，§WHITELIST）；`--strict` PASS exit 0，0 新增违规，其余域 CAT3 与 CAT1/2/4 计数不高于快照 | erp-fin 525 + erp-ast 337 + erp-cs 185 = **1047 tests 全绿 0 失败**；协议 = `docs/architecture/i18n-compliance.md` CAT-3 行（plan Phase 1 Decision：C1 `@Description` / C2 功能契约窄类 / (a) 字典回归 / (b) 英文）；快照外科变换 133 文件（消息/名称列），seed CSV 零改动 |
| MI.6 批 2/2 | `docs/plans/2026-09-07-1715-1-mi6-runtime-string-cat3-batch2.md`（2026-09-07/08） | 16 域 **209 行 / 88 文件**（hr 28 / inventory 28 / manufacturing 28 / aps 20 / projects 15 / b2b 15 / purchase 14 / sales 13 / maintenance 13 / quality 13 / common-service 8 / contract 6 / logistics 3 / crm 2 / master-data 2 / notify 1；drp 0 无批内面） | 全局 CAT3=209（CAT1=0/CAT2=0/CAT4=1318） | 16 域逐域 **0**；全局 CAT3 **0**（CAT1/2/4 持平 0/0/1318，CAT-3 收官归零） | 209 → 0 全域归零 + 白名单全覆盖：(a) AcctDocProvider 科目名 63 行置空（`resolveSubjects` isBlank 回填 seed `ErpMdSubject.getName()`）；(b) 真实运行时字符串 131 行英文化（conflict reason/memo/remark/告警 stage/report 标签/setter 默认名等）；白名单登记 16 文件（C1 `@Description` 专属 13 + C2 seed 角色名契约 3，§WHITELIST「批 2/2」段）；`--strict` PASS exit 0（0 新增违规，88 文件全部 removed-from-tree 改善项）；快照外科变换仅消息/名称列（保留 `*`/`@var:`，csv 新值含逗号按 RFC 4180 补引号），seed CSV 零改动 | 16 批域模块聚合 `mvn test -am` **2772/0/0 全绿**（hr 249 + inv 248 + mfg 308 + aps 82 + prj 179 + b2b 80 + pur 341 + sal 316 + mnt 157 + qa 184 + common 23 + ct 168 + log 66 + crm 188 + md 160 + notify 23）；app-erp-all 集成快照同批外科变换 3 case（C08 mrp_plan remark 2 cell + C09 NCR description 4 cell + C19 receive/line remark 与 SUBJECT_NAME 6 cell 均按 seed 名/新英文字面量回填）后复跑 **70/0/0/1 全绿**；`mvn clean install -DskipTests` 156 模块 BUILD SUCCESS；compliance checker exit 0（R2c=1542 零漂移）；协议 = `docs/architecture/i18n-compliance.md` CAT-3 行（沿用批 1，零修订） |
| MI.8 批 1/2 | `docs/plans/2026-09-07-0902-3-mi8-handwritten-yaml-i18nen-batch1.md`（2026-09-07） | finance 248/18 + projects 134/8 = **382 行 / 26 文件**（M0.4 矩阵 B 节 #35~#52、#87~#94） | 全局 CAT4=1700（fin 248 / prj 134；批前实跑与 SNAPSHOT 一致） | finance **0** / projects **0**；全局 CAT4 **1318** = 1700 − 382（CAT1/2/3 不高于快照，其余域 CAT4 未触碰） | 382 → 0 逐域归零（仅加性补 `i18nEn` 单承载 + flow→block 等价重序列化，行为不变式未破坏）；新词扩 `docs/design/i18n-glossary.md` 批 1 节 20 词；`--strict` PASS exit 0（0 新增违规）；SNAPSHOT/WHITELIST 冻结块零改动 | erp-fin-web 533 + erp-prj-web 179 聚合 `mvn test -am` **全绿 0 失败**；`npm run validate:flux` 整体 exit 1 = 既有外部漂移（nop-chaos-flux dist 对 codegen stub `variant=primary` 校验收紧，325 条 ERR 100% variant 类、与本批未触碰域，2026-09-03 日志在批前已记录同数预存红灯；successor: nop-chaos-flux dist 基线裁决 trigger:validate:flux exit 0 恢复）。**闭包实跑轮修订（2026-09-07 独立闭包审计后）**：首轮实跑 step [1/3] 导出 25 条错误 = 本批自身引入（1 处 `: ` plain scalar 语法错误 + 24 处同 mapping 重复 `i18nEn` 键 `nop.err.core.json.duplicate-key`，flux 可见根因 13 文件 + flux 孪生遮蔽 page.yaml 4 文件；原注记「stash 前后 325 条逐行一致 / 本批 26 页 0 ERR」被证伪）；修复 = 每 mapping 单一 `i18nEn` 承载（标量兄弟式/块承载式，checker 扩展 `blockCarrierCovers` + self-test 3 新断言）+ `: ` 表达式加引号，17 文件；修复后复跑 step [1/3] 导出 **FLUX_PAGE_ERROR_COUNT: 0**（999 页 26 批页 0 ERR）+ `--strict` PASS + 全仓 `mvn test` 8012/0/0/2 全绿 |



## WHITELIST (machine-readable)

> 文件级白名单是运行时字符串中文（CAT-3，及经独立裁决允许的其他 CAT）的**唯一豁免通道**（`docs/architecture/i18n-compliance.md §白名单登记格式`）。脚本解析本块做文件级豁免：`- file: <repo 相对路径>` + `cats: [3]`（数字或 CAT 前缀均可；省略 `cats` 行 = 豁免该文件全部 CAT-1..4）。四要素（文件路径/理由/owner doc 指针/裁决来源）以同行注释或条目下方注释行登记，缺一不可。白名单只增不删；调高（新增豁免）须附裁决来源，无裁决来源的登记无效。

```yaml
# ===== 批 1/2 finance（plan 2026-09-07-0902-1 MI.6 Phase 2；协议 = i18n-compliance.md CAT-3 行 + plan Phase 1 Decision）=====
# 文件路径（四要素1）；理由（四要素2）；owner doc（四要素3）；裁决来源（四要素4）逐条登记如下：
- file: module-finance/erp-fin-dao/src/main/java/app/erp/fin/biz/IErpFinApDocumentBiz.java
  cats: [3]
  # 理由: @Description("中文") 专属文件（1 行），BizModel meta 注解供平台 meta 消费，按 E3 计划（plan 2026-08-28-0219-3）豁免
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5 + 修复模式对照表 CAT-3 行
  # 裁决来源: plan 2026-09-07-0902-1 Phase 1 Decision C1（2026-09-07，逐簇裁决）
- file: module-finance/erp-fin-service/src/main/java/app/erp/fin/service/dashboard/ErpFinDashboardBizModel.java
  cats: [3]
  # 理由: @Description("中文") 专属文件（1 行），同 E3 豁免
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5
  # 裁决来源: plan 2026-09-07-0902-1 Phase 1 Decision C1（2026-09-07）
- file: module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinApDocumentBizModel.java
  cats: [3]
  # 理由: @Description("中文") 专属文件（1 行），同 E3 豁免
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5
  # 裁决来源: plan 2026-09-07-0902-1 Phase 1 Decision C1（2026-09-07）
- file: module-finance/erp-fin-service/src/main/java/app/erp/fin/service/classify/ErpFinApDocRuleClassifier.java
  cats: [3]
  # 理由: 混合文件——setReason 诊断散文 2 行已按 (b) 改英文（本批 Phase 2 Fix）；剩余 3 行 excerpt.contains("增值税专用发票"/"电子发票"/"收据") 为文档内容匹配，中文字面量即行为本体（改英文破坏发票类型分类行为，违行为不变式），C2 窄类豁免
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5 + 修复模式对照表 CAT-3 行（C2 功能型中文内容契约）
  # 裁决来源: plan 2026-09-07-0902-1 Phase 1 Decision C2①（2026-09-07，逐簇裁决）
- file: module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinApDocumentPipelineProcessor.java
  cats: [3]
  # 理由: 混合文件——摄取轨迹/错误消息 9 行已按 (b) 改英文（本批 Phase 2 Fix）；剩余 6 行发票 OCR 解析 Pattern（P_INVOICE_NO/P_INVOICE_DATE/P_AMOUNT_WITH_TAX/P_TAX/P_TOTAL_EX_TAX/P_SUPPLIER_NAME）为中文发票版式解析契约，改英文破坏要素抽取行为，C2 窄类豁免
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5 + 修复模式对照表 CAT-3 行（C2 功能型中文内容契约）
  # 裁决来源: plan 2026-09-07-0902-1 Phase 1 Decision C2①（2026-09-07，逐簇裁决）
# ===== 批 1/2 assets（plan 2026-09-07-0902-1 MI.6 Phase 3；协议同上）=====
- file: module-assets/erp-ast-dao/src/main/java/app/erp/ast/biz/IErpAstAssetBiz.java
  cats: [3]
  # 理由: @Description("中文") 专属文件（1 行），BizModel meta 注解供平台 meta 消费，按 E3 计划豁免
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5
  # 裁决来源: plan 2026-09-07-0902-1 Phase 1 Decision C1（2026-09-07，逐簇裁决）
- file: module-assets/erp-ast-service/src/main/java/app/erp/ast/service/dashboard/ErpAstDashboardBizModel.java
  cats: [3]
  # 理由: @Description("中文") 专属文件（1 行），同 E3 豁免
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5
  # 裁决来源: plan 2026-09-07-0902-1 Phase 1 Decision C1（2026-09-07）
- file: module-assets/erp-ast-service/src/main/java/app/erp/ast/service/entity/ErpAstAssetBizModel.java
  cats: [3]
  # 理由: 混合文件——auditRecorder 审计消息 5 行已按 (b) 改英文（本批 Phase 3 Fix）；剩余 1 行 @Description("资产操作审计时间轴…") 按 E3 豁免，整文件登记
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5 + 修复模式对照表 CAT-3 行（混合文件规则 ii）
  # 裁决来源: plan 2026-09-07-0902-1 Phase 1 Decision C1+C2③（2026-09-07）
- file: module-assets/erp-ast-service/src/main/java/app/erp/ast/service/processor/ErpAstAssetSuspendResumeProcessor.java
  cats: [3]
  # 理由: 混合文件——audit 消息 2 行已按 (b) 改英文（本批 Phase 3 Fix）；剩余 IDLE_SINCE_PREFIX="闲置自 " 为文档化字符串契约（IErpAstAssetBiz javadoc「闲置时长派生的时间基准」+ TestErpAstIdleStateMachine 断言 remark 含该前缀），改英文即破坏契约与测试，C2 窄类豁免
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5 + 修复模式对照表 CAT-3 行（C2 功能型中文内容契约③）
  # 裁决来源: plan 2026-09-07-0902-1 Phase 1 Decision C2③（2026-09-07）
# ===== 批 1/2 cs（plan 2026-09-07-0902-1 MI.6 Phase 4；协议同上）=====
- file: module-cs/erp-cs-service/src/main/java/app/erp/cs/service/ErpCsConstants.java
  cats: [3]
  # 理由: FULFILLMENT_DEFAULT_APPROVER_ROLE="客服主管" 为 seed/通知配置数据契约（运行期匹配 nop_auth_role 角色名与通知模板 roles 数组，见 _cases 中 nop_auth_role.csv 与 erp_sys_notification_template.csv 实证），改英文破坏角色解析与通知投递行为，C2 窄类豁免
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5 + 修复模式对照表 CAT-3 行（C2 功能型中文内容契约②）
  # 裁决来源: plan 2026-09-07-0902-1 Phase 1 Decision C2②（2026-09-07，逐簇裁决）
- file: module-cs/erp-cs-service/src/main/java/app/erp/cs/service/dashboard/ErpCsQualityDashboardBizModel.java
  cats: [3]
  # 理由: 混合文件——看板 teamName "(未分派)" 1 行已按 (b) 改英文（本批 Phase 4 Fix）；剩余 1 行 @Description("客服看板 KPI…") 按 E3 豁免，整文件登记
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5 + 修复模式对照表 CAT-3 行（混合文件规则 ii）
  # 裁决来源: plan 2026-09-07-0902-1 Phase 1 Decision C1（2026-09-07）
# ===== 批 2/2 hr/inventory/manufacturing/aps（plan 2026-09-07-1715-1 MI.6 Phase 2；协议同批 1）=====
- file: module-hr/erp-hr-service/src/main/java/app/erp/hr/service/ErpHrConstants.java
  cats: [3]
  # 理由: HR_ROLE_ID="HR 专员" 为 seed 角色名数据契约（运行期匹配 nop_auth_role.csv seed roleId 与 erp-hr.action-auth.xml roles="HR 专员" 字面，IUserContext.isUserInRole 守卫），改英文破坏角色解析行为，C2 窄类豁免（同 cs「客服主管」先例）
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5 + 修复模式对照表 CAT-3 行（C2 功能型中文内容契约②）
  # 裁决来源: plan 2026-09-07-1715-1 Phase 2（2026-09-07）
- file: module-aps/erp-aps-dao/src/main/java/app/erp/aps/biz/IErpApsOperationOrderBiz.java
  cats: [3]
  # 理由: @Description("中文") 专属文件（1 行），BizModel meta 注解供平台 meta 消费，按 E3 计划豁免
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5
  # 裁决来源: plan 2026-09-07-1715-1 Phase 2（2026-09-07，逐簇裁决）
- file: module-aps/erp-aps-service/src/main/java/app/erp/aps/service/entity/ErpApsOperationOrderBizModel.java
  cats: [3]
  # 理由: @Description("中文") 专属文件（1 行），同 E3 豁免
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5
  # 裁决来源: plan 2026-09-07-1715-1 Phase 2（2026-09-07）
- file: module-inventory/erp-inv-dao/src/main/java/app/erp/inv/biz/IErpInvStockLedgerBiz.java
  cats: [3]
  # 理由: @Description("中文") 专属文件（2 行），同 E3 豁免
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5
  # 裁决来源: plan 2026-09-07-1715-1 Phase 2（2026-09-07）
- file: module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/dashboard/ErpInvDashboardBizModel.java
  cats: [3]
  # 理由: @Description("中文") 专属文件（1 行），同 E3 豁免
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5
  # 裁决来源: plan 2026-09-07-1715-1 Phase 2（2026-09-07）
- file: module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/entity/ErpInvStockLedgerBizModel.java
  cats: [3]
  # 理由: @Description("中文") 专属文件（2 行），同 E3 豁免
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5
  # 裁决来源: plan 2026-09-07-1715-1 Phase 2（2026-09-07）
- file: module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/dashboard/ErpMfgDashboardBizModel.java
  cats: [3]
  # 理由: @Description("中文") 专属文件（1 行），同 E3 豁免
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5
  # 裁决来源: plan 2026-09-07-1715-1 Phase 2（2026-09-07）
- file: module-projects/erp-prj-service/src/main/java/app/erp/prj/service/dashboard/ErpPrjDashboardBizModel.java
  cats: [3]
  # 理由: @Description("中文") 专属文件（1 行），同 E3 豁免（批 2/2 Phase 3）
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5
  # 裁决来源: plan 2026-09-07-1715-1 Phase 3（2026-09-07，逐簇裁决）
- file: module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/dashboard/ErpPurDashboardBizModel.java
  cats: [3]
  # 理由: @Description("中文") 专属文件（1 行），同 E3 豁免（批 2/2 Phase 3）
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5
  # 裁决来源: plan 2026-09-07-1715-1 Phase 3（2026-09-07）
- file: module-sales/erp-sal-service/src/main/java/app/erp/sal/service/dashboard/ErpSalDashboardBizModel.java
  cats: [3]
  # 理由: @Description("中文") 专属文件（1 行），同 E3 豁免（批 2/2 Phase 3）
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5
  # 裁决来源: plan 2026-09-07-1715-1 Phase 3（2026-09-07）
- file: module-common-service/src/main/java/app/erp/common/org/ErpOrgIsolationQueryTransformer.java
  cats: [3]
  # 理由: @Description("中文") 专属文件（1 行），同 E3 豁免（批 2/2 Phase 4）
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5
  # 裁决来源: plan 2026-09-07-1715-1 Phase 4（2026-09-07，逐簇裁决）
- file: module-common-service/src/main/java/app/erp/common/service/MaskHelper.java
  cats: [3]
  # 理由: ROLE_* 7 常量为 seed 角色名数据契约（运行期匹配 nop_auth_role.csv seed roleId 与 action-auth roles 属性，审计 ck-common-app.md 实证 7 角色字面逐一存在），改英文破坏 masking 角色解析行为，C2 窄类豁免（同 cs「客服主管」先例）
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5 + 修复模式对照表 CAT-3 行（C2 功能型中文内容契约②）
  # 裁决来源: plan 2026-09-07-1715-1 Phase 4（2026-09-07）
- file: module-contract/erp-ct-service/src/main/java/app/erp/ct/service/ErpCtConfigs.java
  cats: [3]
  # 理由: DEFAULT_TERMINATE_APPROVER_ROLE="合同审批人" 为 seed 角色名数据契约（nop_auth_role.csv seed roleId + erp-ct.action-auth.xml roles 字面一致），同 C2② 窄类豁免
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5 + 修复模式对照表 CAT-3 行（C2 功能型中文内容契约②）
  # 裁决来源: plan 2026-09-07-1715-1 Phase 4（2026-09-07）
- file: module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/dashboard/ErpMntDashboardBizModel.java
  cats: [3]
  # 理由: @Description("中文") 专属文件（1 行），同 E3 豁免（批 2/2 Phase 4）
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5
  # 裁决来源: plan 2026-09-07-1715-1 Phase 4（2026-09-07）
- file: module-quality/erp-qa-service/src/main/java/app/erp/qa/service/dashboard/ErpQaDashboardBizModel.java
  cats: [3]
  # 理由: @Description("中文") 专属文件（1 行），同 E3 豁免（批 2/2 Phase 4）
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5
  # 裁决来源: plan 2026-09-07-1715-1 Phase 4（2026-09-07）
- file: module-master-data/erp-md-service/src/main/java/app/erp/md/service/dashboard/ErpMdDashboardBizModel.java
  cats: [3]
  # 理由: @Description("中文") 专属文件（1 行），同 E3 豁免（批 2/2 Phase 4）
  # owner doc: docs/architecture/i18n-compliance.md 判定准绳表 #5
  # 裁决来源: plan 2026-09-07-1715-1 Phase 4（2026-09-07）
```
