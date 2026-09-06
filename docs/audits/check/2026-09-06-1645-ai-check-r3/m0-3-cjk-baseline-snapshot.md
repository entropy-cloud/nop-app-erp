## SNAPSHOT (machine-readable)

> Dual-write copy of docs/audits/cjk-baseline.md SNAPSHOT block (identical body; generated 2026-09-06T09:12:45.292Z).

```yaml
generated: 2026-09-06T09:12:45.292Z
scanned: { javaFiles: 3430, yamlFiles: 886 }
totals: { CAT1: 335, CAT2: 204, CAT3: 390, CAT4: 1700 }
domains:
  app-erp-all: { CAT1: 0, CAT2: 0, CAT3: 0, CAT4: 0 }
  aps: { CAT1: 1, CAT2: 0, CAT3: 20, CAT4: 35 }
  assets: { CAT1: 39, CAT2: 41, CAT3: 58, CAT4: 87 }
  b2b: { CAT1: 22, CAT2: 1, CAT3: 15, CAT4: 91 }
  common-service: { CAT1: 0, CAT2: 5, CAT3: 8, CAT4: 0 }
  common-test: { CAT1: 0, CAT2: 0, CAT3: 0, CAT4: 0 }
  contract: { CAT1: 12, CAT2: 1, CAT3: 6, CAT4: 50 }
  crm: { CAT1: 3, CAT2: 2, CAT3: 2, CAT4: 79 }
  cs: { CAT1: 22, CAT2: 2, CAT3: 43, CAT4: 120 }
  drp: { CAT1: 2, CAT2: 9, CAT3: 0, CAT4: 41 }
  finance: { CAT1: 54, CAT2: 23, CAT3: 80, CAT4: 248 }
  hr: { CAT1: 20, CAT2: 0, CAT3: 28, CAT4: 118 }
  inventory: { CAT1: 26, CAT2: 11, CAT3: 28, CAT4: 93 }
  logistics: { CAT1: 19, CAT2: 0, CAT3: 3, CAT4: 18 }
  maintenance: { CAT1: 10, CAT2: 7, CAT3: 13, CAT4: 122 }
  manufacturing: { CAT1: 31, CAT2: 10, CAT3: 28, CAT4: 96 }
  master-data: { CAT1: 0, CAT2: 3, CAT3: 2, CAT4: 74 }
  notify: { CAT1: 15, CAT2: 2, CAT3: 1, CAT4: 42 }
  projects: { CAT1: 14, CAT2: 9, CAT3: 15, CAT4: 134 }
  purchase: { CAT1: 22, CAT2: 36, CAT3: 14, CAT4: 99 }
  quality: { CAT1: 1, CAT2: 11, CAT3: 13, CAT4: 114 }
  sales: { CAT1: 22, CAT2: 31, CAT3: 13, CAT4: 39 }
files:
  module-aps/erp-aps-dao/src/main/java/app/erp/aps/biz/IErpApsOperationOrderBiz.java: { CAT3: 1 }
  module-aps/erp-aps-service/src/main/java/app/erp/aps/service/atpctp/ErpApsAtpCtpServiceImpl.java: { CAT3: 3 }
  module-aps/erp-aps-service/src/main/java/app/erp/aps/service/entity/ErpApsOperationOrderBizModel.java: { CAT3: 1 }
  module-aps/erp-aps-service/src/main/java/app/erp/aps/service/processor/ErpApsSchedulingProcessor.java: { CAT1: 1 }
  module-aps/erp-aps-service/src/main/java/app/erp/aps/service/processor/ErpApsWorkOrderToOperationProcessor.java: { CAT3: 1 }
  module-aps/erp-aps-service/src/main/java/app/erp/aps/service/scheduling/ErpApsSchedulingEngine.java: { CAT3: 14 }
  module-aps/erp-aps-web/src/main/resources/_vfs/erp/aps/pages/dashboard/schedule-gantt.flux.yaml: { CAT4: 13 }
  module-aps/erp-aps-web/src/main/resources/_vfs/erp/aps/pages/dashboard/schedule-gantt.page.yaml: { CAT4: 22 }
  module-assets/erp-ast-dao/src/main/java/app/erp/ast/biz/IErpAstAssetBiz.java: { CAT3: 1 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/dashboard/ErpAstDashboardBizModel.java: { CAT3: 1 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/entity/ErpAstAssetBizModel.java: { CAT3: 6 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/AssetInventoryAcctDocProvider.java: { CAT3: 4 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/AssetInventoryPostingDispatcher.java: { CAT1: 4 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/AssetMergeAcctDocProvider.java: { CAT3: 2 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/AssetMergePostingDispatcher.java: { CAT1: 2, CAT3: 2 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/AssetSplitAcctDocProvider.java: { CAT3: 2 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/AssetSplitPostingDispatcher.java: { CAT1: 2, CAT3: 2 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/CapitalizationAcctDocProvider.java: { CAT3: 3 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/CapitalizationPostingDispatcher.java: { CAT1: 5 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/DepreciationAcctDocProvider.java: { CAT3: 3 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/DepreciationPostingDispatcher.java: { CAT1: 8 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/DisposalAcctDocProvider.java: { CAT3: 9 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/DisposalPostingDispatcher.java: { CAT1: 5 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/MaintenanceCapitalizationAcctDocProvider.java: { CAT3: 3 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/MaintenanceCapitalizationPostingDispatcher.java: { CAT1: 4 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/MaintenanceExpenseAcctDocProvider.java: { CAT3: 3 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/MaintenanceExpensePostingDispatcher.java: { CAT1: 4 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/ValueAdjustmentAcctDocProvider.java: { CAT3: 6 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/ValueAdjustmentPostingDispatcher.java: { CAT1: 4 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/processor/ErpAstAssetCapitalizationProcessor.java: { CAT2: 2 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/processor/ErpAstAssetSuspendResumeProcessor.java: { CAT2: 1, CAT3: 3 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/processor/ErpAstCipProcessor.java: { CAT3: 1 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/processor/ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor.java: { CAT1: 1 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/processor/ErpAstDisposalProcessor.java: { CAT2: 2, CAT3: 2 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/processor/ErpAstInventoryProcessor.java: { CAT2: 2, CAT3: 1 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/processor/ErpAstMaintenanceCompleteWorkProcessor.java: { CAT3: 1 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/processor/ErpAstMaintenanceProcessor.java: { CAT2: 1 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/processor/ErpAstMergeProcessor.java: { CAT2: 5, CAT3: 1 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/processor/ErpAstSplitProcessor.java: { CAT2: 5 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/processor/ErpAstValueAdjustmentProcessor.java: { CAT2: 4, CAT3: 2 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/statemachine/ErpAstAssetCapitalizationApprovalStateMachine.java: { CAT2: 1 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/statemachine/ErpAstAssetCapitalizationDocumentStateMachine.java: { CAT2: 2 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/statemachine/ErpAstAssetStateMachine.java: { CAT2: 3 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/statemachine/ErpAstDisposalApprovalStateMachine.java: { CAT2: 1 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/statemachine/ErpAstDisposalDocumentStateMachine.java: { CAT2: 1 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/statemachine/ErpAstInventoryStateMachine.java: { CAT2: 1 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/statemachine/ErpAstMaintenanceStateMachine.java: { CAT2: 1 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/statemachine/ErpAstMergeApprovalStateMachine.java: { CAT2: 1 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/statemachine/ErpAstMergeDocumentStateMachine.java: { CAT2: 1 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/statemachine/ErpAstMovementApprovalStateMachine.java: { CAT2: 1 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/statemachine/ErpAstSplitApprovalStateMachine.java: { CAT2: 1 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/statemachine/ErpAstSplitDocumentStateMachine.java: { CAT2: 1 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/statemachine/ErpAstValueAdjustmentApprovalStateMachine.java: { CAT2: 1 }
  module-assets/erp-ast-service/src/main/java/app/erp/ast/service/statemachine/ErpAstValueAdjustmentDocumentStateMachine.java: { CAT2: 3 }
  module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/asset-repair/main.page.yaml: { CAT4: 2 }
  module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/asset-stocktake/main.page.yaml: { CAT4: 14 }
  module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/dashboard/main.flux.yaml: { CAT4: 18 }
  module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/dashboard/main.page.yaml: { CAT4: 19 }
  module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/disposal-wizard/main.page.yaml: { CAT4: 20 }
  module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/report/asset-depreciation-detail.page.yaml: { CAT4: 8 }
  module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/report/asset-disposal-detail.page.yaml: { CAT4: 6 }
  module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/codemapping/CodeMappingResolver.java: { CAT1: 2 }
  module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/entity/ErpB2bEdiDocBizModel.java: { CAT3: 5 }
  module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/entity/ErpB2bPartnerProfileBizModel.java: { CAT3: 2 }
  module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/job/ErpB2bOnboardingMonitorJob.java: { CAT1: 2 }
  module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bAsnCreateReceiveFromAsnProcessor.java: { CAT1: 6, CAT3: 2 }
  module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bAsnHandleInboundWebhookProcessor.java: { CAT1: 1, CAT3: 1 }
  module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bAsnMatchPurchaseOrderProcessor.java: { CAT1: 6, CAT3: 2 }
  module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bEdiDocCreateInboundProcessor.java: { CAT3: 1 }
  module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bEdiDocCreateOutboundProcessor.java: { CAT1: 2, CAT3: 1 }
  module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/spi/transport/TransportManager.java: { CAT1: 3, CAT3: 1 }
  module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/statemachine/ErpB2bPartnerProfileStateMachine.java: { CAT2: 1 }
  module-b2b/erp-b2b-web/src/main/resources/_vfs/erp/b2b/pages/dashboard/asn-flow.flux.yaml: { CAT4: 27 }
  module-b2b/erp-b2b-web/src/main/resources/_vfs/erp/b2b/pages/dashboard/asn-flow.page.yaml: { CAT4: 32 }
  module-b2b/erp-b2b-web/src/main/resources/_vfs/erp/b2b/pages/dashboard/edi-detail.flux.yaml: { CAT4: 11 }
  module-b2b/erp-b2b-web/src/main/resources/_vfs/erp/b2b/pages/dashboard/edi-detail.page.yaml: { CAT4: 21 }
  module-common-service/src/main/java/app/erp/common/org/ErpOrgIsolationQueryTransformer.java: { CAT3: 1 }
  module-common-service/src/main/java/app/erp/common/service/AbstractApproveProcessor.java: { CAT2: 1 }
  module-common-service/src/main/java/app/erp/common/service/AbstractCancelProcessor.java: { CAT2: 1 }
  module-common-service/src/main/java/app/erp/common/service/AbstractRejectProcessor.java: { CAT2: 1 }
  module-common-service/src/main/java/app/erp/common/service/AbstractSubmitForApprovalProcessor.java: { CAT2: 1 }
  module-common-service/src/main/java/app/erp/common/service/AbstractWithdrawApprovalProcessor.java: { CAT2: 1 }
  module-common-service/src/main/java/app/erp/common/service/MaskHelper.java: { CAT3: 7 }
  module-contract/erp-ct-service/src/main/java/app/erp/ct/service/ErpCtConfigs.java: { CAT3: 1 }
  module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtContractBizModel.java: { CAT1: 3, CAT3: 1 }
  module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtDocumentBizModel.java: { CAT1: 3, CAT3: 2 }
  module-contract/erp-ct-service/src/main/java/app/erp/ct/service/job/ErpCtApprovalTimeoutEscalationJob.java: { CAT1: 2 }
  module-contract/erp-ct-service/src/main/java/app/erp/ct/service/job/ErpCtContractExpiryJob.java: { CAT1: 2 }
  module-contract/erp-ct-service/src/main/java/app/erp/ct/service/job/ErpCtDocRetentionJob.java: { CAT1: 2 }
  module-contract/erp-ct-service/src/main/java/app/erp/ct/service/processor/ErpCtSignatureRequestInitSignatureRequestProcessor.java: { CAT2: 1 }
  module-contract/erp-ct-service/src/main/java/app/erp/ct/service/spi/manual/ManualOcrEngine.java: { CAT3: 1 }
  module-contract/erp-ct-service/src/main/java/app/erp/ct/service/spi/mock/MockSignatureProvider.java: { CAT3: 1 }
  module-contract/erp-ct-web/src/main/resources/_vfs/erp/ct/pages/dashboard/version-diff.flux.yaml: { CAT4: 18 }
  module-contract/erp-ct-web/src/main/resources/_vfs/erp/ct/pages/dashboard/version-diff.page.yaml: { CAT4: 32 }
  module-crm/erp-crm-service/src/main/java/app/erp/crm/service/job/ErpCrmEventReminderJob.java: { CAT1: 1 }
  module-crm/erp-crm-service/src/main/java/app/erp/crm/service/job/ErpCrmSequenceOverdueJob.java: { CAT1: 1 }
  module-crm/erp-crm-service/src/main/java/app/erp/crm/service/processor/ErpCrmConversionProcessor.java: { CAT3: 1 }
  module-crm/erp-crm-service/src/main/java/app/erp/crm/service/processor/ErpCrmLeadProcessor.java: { CAT1: 1, CAT2: 1 }
  module-crm/erp-crm-service/src/main/java/app/erp/crm/service/statemachine/ErpCrmLeadStateMachine.java: { CAT2: 1 }
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
  module-cs/erp-cs-service/src/main/java/app/erp/cs/service/ErpCsConstants.java: { CAT3: 1 }
  module-cs/erp-cs-service/src/main/java/app/erp/cs/service/dashboard/ErpCsQualityDashboardBizModel.java: { CAT3: 2 }
  module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/ErpCsTicketBizModel.java: { CAT1: 4, CAT3: 7 }
  module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/ErpCsTimeEntryBizModel.java: { CAT1: 3, CAT2: 1, CAT3: 1 }
  module-cs/erp-cs-service/src/main/java/app/erp/cs/service/job/ErpCsCsatReminderJob.java: { CAT1: 1 }
  module-cs/erp-cs-service/src/main/java/app/erp/cs/service/job/ErpCsEntitlementExpiryJob.java: { CAT1: 1 }
  module-cs/erp-cs-service/src/main/java/app/erp/cs/service/job/ErpCsFulfillmentRetryJob.java: { CAT1: 1 }
  module-cs/erp-cs-service/src/main/java/app/erp/cs/service/job/ErpCsQualityEscalationRetryJob.java: { CAT1: 1 }
  module-cs/erp-cs-service/src/main/java/app/erp/cs/service/job/ErpCsSurveySendJob.java: { CAT1: 1 }
  module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor.java: { CAT1: 5, CAT3: 26 }
  module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsServiceCatalogItemCreateFromCatalogProcessor.java: { CAT1: 3 }
  module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsTicketEscalateToQualityProcessor.java: { CAT3: 1 }
  module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsTicketMatchAndAttachSlaProcessor.java: { CAT3: 1 }
  module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsTicketReopenProcessor.java: { CAT3: 1 }
  module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsTicketResolveProcessor.java: { CAT1: 1, CAT3: 1 }
  module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsTicketScanOverdueTicketsProcessor.java: { CAT1: 1, CAT3: 1 }
  module-cs/erp-cs-service/src/main/java/app/erp/cs/service/report/ErpCsReportBizModel.java: { CAT3: 1 }
  module-cs/erp-cs-service/src/main/java/app/erp/cs/service/statemachine/ErpCsTicketStateMachine.java: { CAT2: 1 }
  module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsQualityDashboard/main.flux.yaml: { CAT4: 21 }
  module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsQualityDashboard/main.page.yaml: { CAT4: 20 }
  module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicket/kanban.flux.yaml: { CAT4: 10 }
  module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicket/kanban.page.yaml: { CAT4: 48 }
  module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicketAction/timeline.flux.yaml: { CAT4: 6 }
  module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicketAction/timeline.page.yaml: { CAT4: 9 }
  module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/report/ticket-sla-csat-summary.page.yaml: { CAT4: 6 }
  module-drp/erp-drp-service/src/main/java/app/erp/drp/service/job/ErpDrpCrossDockStagingTimeoutJob.java: { CAT1: 2 }
  module-drp/erp-drp-service/src/main/java/app/erp/drp/service/processor/ErpInvDrpCrossDockProcessor.java: { CAT2: 4 }
  module-drp/erp-drp-service/src/main/java/app/erp/drp/service/processor/ErpInvDrpSafetyStockCalcConfirmWritebackProcessor.java: { CAT2: 1 }
  module-drp/erp-drp-service/src/main/java/app/erp/drp/service/safetystock/SafetyStockEngine.java: { CAT2: 3 }
  module-drp/erp-drp-service/src/main/java/app/erp/drp/service/statemachine/ErpDrpLineStateMachine.java: { CAT2: 1 }
  module-drp/erp-drp-web/src/main/resources/_vfs/erp/drp/pages/dashboard/net-requirement.flux.yaml: { CAT4: 20 }
  module-drp/erp-drp-web/src/main/resources/_vfs/erp/drp/pages/dashboard/net-requirement.page.yaml: { CAT4: 21 }
  module-finance/erp-fin-dao/src/main/java/app/erp/fin/biz/IErpFinApDocumentBiz.java: { CAT3: 1 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/annualclose/AnnualCloseService.java: { CAT3: 3 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/baddebt/BadDebtProvisionService.java: { CAT3: 6 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/bankrecon/BankReconAdjAcctDocProvider.java: { CAT3: 1 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/budget/BudgetVoucherGenerator.java: { CAT3: 1 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/budget/CommitmentVoucherGenerator.java: { CAT3: 2 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/budget/ErpFinBudgetCommitmentBizModel.java: { CAT1: 2 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/budget/ErpFinBudgetControlBiz.java: { CAT1: 1, CAT3: 1 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/budget/ErpFinBudgetScenarioProcessor.java: { CAT1: 2 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/classify/ErpFinApDocRuleClassifier.java: { CAT3: 5 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/close/CloseVoucherWriter.java: { CAT2: 1 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/dashboard/ErpFinDashboardBizModel.java: { CAT3: 1 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinApDocumentBizModel.java: { CAT3: 1 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinEmployeeAdvanceBizModel.java: { CAT1: 1 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/fx/ExchangeRevaluationService.java: { CAT3: 2 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/intercompany/ErpFinIntercompanyTransferBizModel.java: { CAT1: 1 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/intercompany/IntercompanyVoucherGenerator.java: { CAT1: 3, CAT3: 3 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/EmployeeAdvancePostingDispatcher.java: { CAT1: 4 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinGlMappingResolver.java: { CAT1: 3 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostedListenerRegistry.java: { CAT1: 2 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostingExceptionRecorder.java: { CAT1: 2 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostingProcessor.java: { CAT1: 7, CAT3: 1 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinReversalListenerRegistry.java: { CAT1: 2 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinTransferPriceResolver.java: { CAT1: 3 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ExpenseClaimPostingDispatcher.java: { CAT1: 2 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/NotesPostingDispatcher.java: { CAT1: 2 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/provider/CreditFacilityInterestAcctDocProvider.java: { CAT3: 2 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/provider/EmployeeAdvanceAcctDocProvider.java: { CAT3: 6 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/provider/ExpenseClaimAcctDocProvider.java: { CAT3: 3 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/provider/NotesPayableAcctDocProvider.java: { CAT3: 4 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/provider/NotesReceivableAcctDocProvider.java: { CAT3: 11 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/AbstractErpFinReconciliationProcessor.java: { CAT3: 1 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinAccountingPeriodProcessor.java: { CAT1: 12 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinApDocumentPipelineProcessor.java: { CAT2: 6, CAT3: 15 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinBadDebtProcessor.java: { CAT3: 2 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinBudgetScenarioCarryForwardProcessor.java: { CAT1: 1 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinBudgetScenarioRollForwardProcessor.java: { CAT1: 1 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinConsolidationEliminationGenerateEliminationCandidatesProcessor.java: { CAT1: 1 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinConsolidationEliminationPostEliminationProcessor.java: { CAT1: 1, CAT3: 4 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinEmployeeAdvanceProcessor.java: { CAT2: 2 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinExpenseClaimProcessor.java: { CAT2: 2 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinIntercompanyMatchRunMatchingProcessor.java: { CAT1: 1 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinNotesReceivableProcessor.java: { CAT2: 1 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinVoucherTemplateRenderTemplateProcessor.java: { CAT2: 6 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/profitloss/ProfitLossClosingService.java: { CAT3: 2 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/report/ErpFinReportBizModel.java: { CAT3: 2 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/statemachine/ErpFinEmployeeAdvanceDocumentStateMachine.java: { CAT2: 1 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/statemachine/ErpFinExpenseClaimDocumentStateMachine.java: { CAT2: 1 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/statemachine/ErpFinNotesPayableStateMachine.java: { CAT2: 1 }
  module-finance/erp-fin-service/src/main/java/app/erp/fin/service/statemachine/ErpFinNotesReceivableStateMachine.java: { CAT2: 2 }
  module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/ErpFinVoucherBillR/bills-by-voucher.page.yaml: { CAT4: 7 }
  module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/ErpFinVoucherBillR/voucher-by-bill.page.yaml: { CAT4: 11 }
  module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/bank-ledger-line/main.page.yaml: { CAT4: 2 }
  module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/bank-reconciliation/main.page.yaml: { CAT4: 15 }
  module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/bank-statement/main.page.yaml: { CAT4: 14 }
  module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/budget-control-log/main.page.yaml: { CAT4: 17 }
  module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/budget-scenario/main.page.yaml: { CAT4: 15 }
  module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/dashboard/main.flux.yaml: { CAT4: 20 }
  module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/dashboard/main.page.yaml: { CAT4: 20 }
  module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/expense-claim/main.page.yaml: { CAT4: 17 }
  module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/gl-distribution/main.page.yaml: { CAT4: 2 }
  module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/period-close-wizard/main.flux.yaml: { CAT4: 26 }
  module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/period-close-wizard/main.page.yaml: { CAT4: 53 }
  module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/report/ar-ap-aging.page.yaml: { CAT4: 5 }
  module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/report/balance-sheet.page.yaml: { CAT4: 6 }
  module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/report/cash-flow.page.yaml: { CAT4: 6 }
  module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/report/income-statement.page.yaml: { CAT4: 6 }
  module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/report/period-close-report.page.yaml: { CAT4: 6 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/ErpHrConstants.java: { CAT3: 1 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/entity/ErpHrDevelopmentPlanBizModel.java: { CAT3: 1 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/entity/ErpHrEmployeeBizModel.java: { CAT1: 1 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/entity/ErpHrRecruitmentBizModel.java: { CAT3: 2 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/entity/ErpHrSalarySimulationBizModel.java: { CAT3: 3 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/job/ErpHrContractExpiryJob.java: { CAT1: 1 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/job/ErpHrLeaveApproverTimeoutJob.java: { CAT1: 3 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/payroll/IncomeTaxCalculator.java: { CAT1: 1 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/posting/SalaryPostingDispatcher.java: { CAT1: 13, CAT3: 4 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/posting/SalaryPostingProvider.java: { CAT3: 8 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/processor/AbstractErpHrDevelopmentPlanProcessor.java: { CAT3: 1 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/processor/ErpHrDevelopmentPlanGenerateDevelopmentPlanProcessor.java: { CAT3: 1 }
  module-hr/erp-hr-service/src/main/java/app/erp/hr/service/processor/ErpHrEmployeeTransferEmployeeProcessor.java: { CAT1: 1 }
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
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/StandardCostResolver.java: { CAT1: 1 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/dashboard/ErpInvDashboardBizModel.java: { CAT3: 1 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/entity/ErpInvReservationBizModel.java: { CAT1: 5 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/entity/ErpInvStockLedgerBizModel.java: { CAT3: 2 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/CostAdjustmentAcctDocProvider.java: { CAT3: 4 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/CostAdjustmentPostingDispatcher.java: { CAT1: 4 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/InvAcctDocProvider.java: { CAT3: 6 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/InvOwnershipTransferProvider.java: { CAT3: 2 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/InvPostingDispatcher.java: { CAT1: 4 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/LandedCostAcctDocProvider.java: { CAT3: 2 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/LandedCostPostingDispatcher.java: { CAT1: 2 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/OwnershipTransferPostingDispatcher.java: { CAT1: 2 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/PurchasePriceVarianceAcctDocProvider.java: { CAT3: 4 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvCostAdjustApplyCostAdjustProcessor.java: { CAT2: 1 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvCostAdjustProcessor.java: { CAT2: 2 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvCostAdjustReverseApproveProcessor.java: { CAT2: 1 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvLandedCostProcessor.java: { CAT1: 3, CAT3: 2 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvOwnershipTransferProcessor.java: { CAT2: 2 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvStockMoveCancelProcessor.java: { CAT2: 1 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvStockMoveReverseProcessor.java: { CAT3: 1 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvStockTakeCompleteTakeProcessor.java: { CAT1: 5, CAT3: 1 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/statemachine/ErpInvCostAdjustStateMachine.java: { CAT2: 1 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/statemachine/ErpInvOwnershipTransferStateMachine.java: { CAT2: 1 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/statemachine/ErpInvStockMoveStateMachine.java: { CAT2: 1 }
  module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/statemachine/ErpInvStockTakeStateMachine.java: { CAT2: 1 }
  module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/dashboard/main.flux.yaml: { CAT4: 31 }
  module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/dashboard/main.page.yaml: { CAT4: 31 }
  module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/report/inventory-trace-report.page.yaml: { CAT4: 10 }
  module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/stock-take-flow/main.page.yaml: { CAT4: 21 }
  module-logistics/erp-log-service/src/main/java/app/erp/log/service/gateway/GatewayDispatcher.java: { CAT1: 4, CAT3: 1 }
  module-logistics/erp-log-service/src/main/java/app/erp/log/service/job/ErpLogDraftEscalationJob.java: { CAT1: 1 }
  module-logistics/erp-log-service/src/main/java/app/erp/log/service/posting/LogisticsFreightProvider.java: { CAT3: 2 }
  module-logistics/erp-log-service/src/main/java/app/erp/log/service/processor/AbstractErpLogShipmentDeliveredProcessor.java: { CAT1: 13 }
  module-logistics/erp-log-service/src/main/java/app/erp/log/service/processor/ErpLogShipmentScanForPollingProcessor.java: { CAT1: 1 }
  module-logistics/erp-log-web/src/main/resources/_vfs/erp/log/pages/dashboard/shipment-tracking.flux.yaml: { CAT4: 7 }
  module-logistics/erp-log-web/src/main/resources/_vfs/erp/log/pages/dashboard/shipment-tracking.page.yaml: { CAT4: 11 }
  module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/dashboard/ErpMntDashboardBizModel.java: { CAT3: 1 }
  module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MaintenanceIssueAcctDocProvider.java: { CAT3: 4 }
  module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MaintenanceIssuePostingDispatcher.java: { CAT1: 3 }
  module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MaintenanceLaborAcctDocProvider.java: { CAT3: 6 }
  module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MaintenanceLaborPostingDispatcher.java: { CAT1: 3 }
  module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/processor/ErpMntRequestCancelProcessor.java: { CAT2: 1 }
  module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/processor/ErpMntRequestRejectRequestProcessor.java: { CAT2: 1 }
  module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/processor/ErpMntSparePartUsageReverseConfirmProcessor.java: { CAT1: 2 }
  module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/processor/ErpMntVisitCancelProcessor.java: { CAT1: 2, CAT2: 1 }
  module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/processor/ErpMntVisitReportAdditionalFaultProcessor.java: { CAT3: 1 }
  module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/report/ErpMntReportBizModel.java: { CAT3: 1 }
  module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/statemachine/ErpMntRequestStateMachine.java: { CAT2: 2 }
  module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/statemachine/ErpMntSparePartUsageApprovalStateMachine.java: { CAT2: 1 }
  module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/statemachine/ErpMntVisitStateMachine.java: { CAT2: 1 }
  module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/dashboard/main.flux.yaml: { CAT4: 23 }
  module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/dashboard/main.page.yaml: { CAT4: 24 }
  module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/report/downtime-summary.page.yaml: { CAT4: 8 }
  module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/report/maintenance-history.page.yaml: { CAT4: 8 }
  module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/visit-wizard/main.flux.yaml: { CAT4: 25 }
  module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/visit-wizard/main.page.yaml: { CAT4: 34 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/costing/ProductionVarianceCalculator.java: { CAT1: 1 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/crp/CrpLoadCalculator.java: { CAT1: 2 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/dashboard/ErpMfgDashboardBizModel.java: { CAT3: 1 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/entity/ErpMfgBomBizModel.java: { CAT3: 1 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/genealogy/BatchGenealogyWriter.java: { CAT1: 2 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/ManufacturingIssueAcctDocProvider.java: { CAT3: 4 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/ManufacturingIssuePostingDispatcher.java: { CAT1: 2 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/ProductionVarianceAcctDocProvider.java: { CAT3: 5 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/ProductionVarianceDispatcher.java: { CAT1: 3 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/SubcontractFeeAcctDocProvider.java: { CAT3: 3 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/SubcontractIssueAcctDocProvider.java: { CAT3: 4 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/SubcontractPostingDispatcher.java: { CAT1: 5, CAT3: 3 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/SubcontractReceiptAcctDocProvider.java: { CAT3: 4 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgJobCardCancelJobProcessor.java: { CAT2: 1 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgJobCardRecordWorkProcessor.java: { CAT2: 1 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgJobCardSubmitJobProcessor.java: { CAT2: 1 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgMaterialIssueConfirmProcessor.java: { CAT1: 1 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgScheduleToJobCardProcessor.java: { CAT3: 1 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgSubcontractOrderProcessor.java: { CAT1: 5, CAT2: 3 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgWorkOrderCloseProcessor.java: { CAT2: 1 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgWorkOrderProcessor.java: { CAT1: 8, CAT2: 3 }
  module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgWorkOrderReportCompletionProcessor.java: { CAT1: 2 }
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
  module-master-data/erp-md-service/src/main/java/app/erp/md/service/entity/ErpMdSupplierApprovalBizModel.java: { CAT2: 2 }
  module-master-data/erp-md-service/src/main/java/app/erp/md/service/statemachine/ErpMdSupplierApprovalStateMachine.java: { CAT2: 1 }
  module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/cost-center/main.page.yaml: { CAT4: 14 }
  module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/dashboard/main.flux.yaml: { CAT4: 14 }
  module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/dashboard/main.page.yaml: { CAT4: 14 }
  module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/party-search/main.picker.page.yaml: { CAT4: 20 }
  module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/report/material-price-list.page.yaml: { CAT4: 6 }
  module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/report/partner-list.page.yaml: { CAT4: 6 }
  module-notify/erp-notify-service/src/main/java/app/erp/notify/service/dispatch/NoopEmailSender.java: { CAT1: 1 }
  module-notify/erp-notify-service/src/main/java/app/erp/notify/service/dispatch/NoopSmsSender.java: { CAT1: 1 }
  module-notify/erp-notify-service/src/main/java/app/erp/notify/service/dispatch/NotificationDispatcher.java: { CAT1: 9, CAT3: 1 }
  module-notify/erp-notify-service/src/main/java/app/erp/notify/service/dispatch/NotificationRecipientResolver.java: { CAT1: 2, CAT2: 2 }
  module-notify/erp-notify-service/src/main/java/app/erp/notify/service/processor/ErpSysNotificationNotifyProcessor.java: { CAT1: 2 }
  module-notify/erp-notify-web/src/main/resources/_vfs/erp/notify/pages/ErpSysNotification/inbox.page.yaml: { CAT4: 42 }
  module-projects/erp-prj-service/src/main/java/app/erp/prj/service/cost/BudgetChecker.java: { CAT1: 1 }
  module-projects/erp-prj-service/src/main/java/app/erp/prj/service/dashboard/ErpPrjDashboardBizModel.java: { CAT3: 1 }
  module-projects/erp-prj-service/src/main/java/app/erp/prj/service/entity/ErpPrjProjectBizModel.java: { CAT1: 1 }
  module-projects/erp-prj-service/src/main/java/app/erp/prj/service/entity/ErpPrjTaskBizModel.java: { CAT1: 1, CAT3: 1 }
  module-projects/erp-prj-service/src/main/java/app/erp/prj/service/posting/ProjectCostCollectionProvider.java: { CAT3: 3 }
  module-projects/erp-prj-service/src/main/java/app/erp/prj/service/posting/ProjectSettlementAcctDocProvider.java: { CAT3: 9 }
  module-projects/erp-prj-service/src/main/java/app/erp/prj/service/posting/ProjectSettlementPostingDispatcher.java: { CAT1: 6 }
  module-projects/erp-prj-service/src/main/java/app/erp/prj/service/posting/TimesheetPostingDispatcher.java: { CAT1: 4 }
  module-projects/erp-prj-service/src/main/java/app/erp/prj/service/processor/ErpPrjProjectCloseProjectProcessor.java: { CAT1: 1 }
  module-projects/erp-prj-service/src/main/java/app/erp/prj/service/processor/ErpPrjProjectSettlementCancelProcessor.java: { CAT2: 1 }
  module-projects/erp-prj-service/src/main/java/app/erp/prj/service/processor/ErpPrjProjectSettlementProcessor.java: { CAT2: 1, CAT3: 1 }
  module-projects/erp-prj-service/src/main/java/app/erp/prj/service/processor/ErpPrjProjectSettlementReturnRetentionProcessor.java: { CAT2: 4 }
  module-projects/erp-prj-service/src/main/java/app/erp/prj/service/processor/ErpPrjProjectSettlementReverseSettlementProcessor.java: { CAT2: 1 }
  module-projects/erp-prj-service/src/main/java/app/erp/prj/service/statemachine/ErpPrjProjectSettlementDocumentStateMachine.java: { CAT2: 1 }
  module-projects/erp-prj-service/src/main/java/app/erp/prj/service/statemachine/ErpPrjProjectStateMachine.java: { CAT2: 1 }
  module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/ErpPrjTask/kanban.flux.yaml: { CAT4: 6 }
  module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/ErpPrjTask/kanban.page.yaml: { CAT4: 35 }
  module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/dashboard/main.flux.yaml: { CAT4: 23 }
  module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/dashboard/main.page.yaml: { CAT4: 24 }
  module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/project-pnl/main.page.yaml: { CAT4: 15 }
  module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/project-settlement/main.page.yaml: { CAT4: 15 }
  module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/report/project-cost-summary.page.yaml: { CAT4: 8 }
  module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/report/timesheet-detail.page.yaml: { CAT4: 8 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/dashboard/ErpPurDashboardBizModel.java: { CAT3: 1 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ErpPurQuotationBizModel.java: { CAT1: 1, CAT2: 3 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ErpPurRfqBizModel.java: { CAT2: 3 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/PaymentSettler.java: { CAT3: 1 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/RequisitionToOrderConverter.java: { CAT1: 1 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ThreeWayMatcher.java: { CAT1: 4 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurAcctDocProvider.java: { CAT3: 11 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurInvoicePostingDispatcher.java: { CAT1: 4 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurPaymentPostingDispatcher.java: { CAT1: 4 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurReturnPostingDispatcher.java: { CAT1: 4 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurInvoiceProcessor.java: { CAT2: 3 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurOrderCancelProcessor.java: { CAT2: 1 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurOrderProcessor.java: { CAT2: 3 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurPaymentProcessor.java: { CAT2: 3 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveCancelProcessor.java: { CAT1: 1, CAT2: 1 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveProcessor.java: { CAT1: 3, CAT2: 3 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurRequisitionCancelProcessor.java: { CAT2: 1 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurRequisitionProcessor.java: { CAT2: 3 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReturnCancelProcessor.java: { CAT2: 1 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReturnProcessor.java: { CAT2: 3 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/statemachine/ErpPurInvoiceDocumentStateMachine.java: { CAT2: 1 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/statemachine/ErpPurOrderDocumentStateMachine.java: { CAT2: 1 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/statemachine/ErpPurPaymentDocumentStateMachine.java: { CAT2: 1 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/statemachine/ErpPurQuotationDocumentStateMachine.java: { CAT2: 1 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/statemachine/ErpPurReceiveDocumentStateMachine.java: { CAT2: 1 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/statemachine/ErpPurRequisitionDocumentStateMachine.java: { CAT2: 1 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/statemachine/ErpPurReturnDocumentStateMachine.java: { CAT2: 1 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/statemachine/ErpPurRfqDocumentStateMachine.java: { CAT2: 1 }
  module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/support/ErpPurCtDiscountApplier.java: { CAT3: 1 }
  module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/dashboard/main.flux.yaml: { CAT4: 24 }
  module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/dashboard/main.page.yaml: { CAT4: 24 }
  module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/dashboard/three-way-match.flux.yaml: { CAT4: 22 }
  module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/dashboard/three-way-match.page.yaml: { CAT4: 29 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/dashboard/ErpQaDashboardBizModel.java: { CAT3: 1 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/entity/ErpQaNonConformanceBizModel.java: { CAT2: 1 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/entity/ErpQaRecallBizModel.java: { CAT2: 1 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/entity/NcrLifecycleService.java: { CAT3: 3 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/posting/NcrPostingDispatcher.java: { CAT1: 1 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/posting/NcrReturnOrchestrator.java: { CAT3: 2 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/posting/NcrScrapAcctDocProvider.java: { CAT3: 2 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/processor/ErpQaInspectionFailInspectionProcessor.java: { CAT2: 1 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/processor/ErpQaInspectionPassInspectionProcessor.java: { CAT2: 1 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/processor/ErpQaInspectionRecordResultProcessor.java: { CAT2: 1 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/processor/ErpQaNonConformanceUpgradeToRecallProcessor.java: { CAT3: 1 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/processor/ErpQaRecallProcessor.java: { CAT2: 1 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/report/ErpQaReportBizModel.java: { CAT3: 1 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/spc/SpcCapabilityCalculator.java: { CAT3: 1 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/spc/SpcOutOfControlHandler.java: { CAT3: 2 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/statemachine/ErpQaInspectionResultStateMachine.java: { CAT2: 3 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/statemachine/ErpQaNonConformanceStateMachine.java: { CAT2: 1 }
  module-quality/erp-qa-service/src/main/java/app/erp/qa/service/statemachine/ErpQaRecallStateMachine.java: { CAT2: 1 }
  module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/dashboard/main.flux.yaml: { CAT4: 25 }
  module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/dashboard/main.page.yaml: { CAT4: 28 }
  module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/ncr-disposal/main.page.yaml: { CAT4: 2 }
  module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/report/inspection-summary.page.yaml: { CAT4: 8 }
  module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/report/ncr-capa-summary.page.yaml: { CAT4: 6 }
  module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/spc-capability/main.page.yaml: { CAT4: 12 }
  module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/spc-chart/main.page.yaml: { CAT4: 17 }
  module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/spc-sample/main.page.yaml: { CAT4: 16 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/dashboard/ErpSalDashboardBizModel.java: { CAT3: 1 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/CreditLimitChecker.java: { CAT1: 3 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ErpSalOrderBizModel.java: { CAT1: 1 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ErpSalPriceListBizModel.java: { CAT1: 1 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ReceiptSettler.java: { CAT3: 1 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ReturnCostStrategyResolver.java: { CAT1: 2 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/posting/SalAcctDocProvider.java: { CAT3: 7 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/posting/SalInvoicePostingDispatcher.java: { CAT1: 4 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/posting/SalReceiptPostingDispatcher.java: { CAT1: 4 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/posting/SalReturnPostingDispatcher.java: { CAT1: 5 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalDeliveryCancelProcessor.java: { CAT1: 1, CAT2: 1 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalDeliveryProcessor.java: { CAT2: 3 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalInvoiceCancelProcessor.java: { CAT2: 1 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalInvoiceProcessor.java: { CAT2: 3 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalOrderCancelProcessor.java: { CAT2: 1 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalOrderProcessor.java: { CAT1: 1, CAT2: 3 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalQuotationCancelProcessor.java: { CAT2: 1 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalQuotationProcessor.java: { CAT2: 3 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalReceiptCancelProcessor.java: { CAT2: 1 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalReceiptProcessor.java: { CAT2: 3 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalReturnCancelProcessor.java: { CAT2: 1 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalReturnGenerateExchangeDeliveryProcessor.java: { CAT3: 3 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalReturnProcessor.java: { CAT2: 4 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/statemachine/ErpSalDeliveryDocumentStateMachine.java: { CAT2: 1 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/statemachine/ErpSalInvoiceDocumentStateMachine.java: { CAT2: 1 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/statemachine/ErpSalOrderDocumentStateMachine.java: { CAT2: 1 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/statemachine/ErpSalQuotationDocumentStateMachine.java: { CAT2: 1 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/statemachine/ErpSalReceiptDocumentStateMachine.java: { CAT2: 1 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/statemachine/ErpSalReturnDocumentStateMachine.java: { CAT2: 1 }
  module-sales/erp-sal-service/src/main/java/app/erp/sal/service/support/ErpSalPricingRuleEngine.java: { CAT3: 1 }
  module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/dashboard/main.flux.yaml: { CAT4: 19 }
  module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/dashboard/main.page.yaml: { CAT4: 20 }
```
