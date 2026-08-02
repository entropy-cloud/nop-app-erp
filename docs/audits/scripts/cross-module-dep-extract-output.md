# Cross-Module Dependency Audit — Automated Extraction

Repository: /Users/abc/app/nop-app-erp
ORM files scanned: 19

## 1. Cross-module edge inventory (full list)

| # | src | src_entity | -> | tgt | tgt_entity | rel | kind | file:line |
|---|-----|------------|----|-----|------------|-----|------|-----------|
| 1 | aps | ErpApsConstraint | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-aps/model/app-erp-aps.orm.xml:178 |
| 2 | aps | ErpApsDispatchLog | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-aps/model/app-erp-aps.orm.xml:292 |
| 3 | aps | ErpApsDispatchRule | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-aps/model/app-erp-aps.orm.xml:256 |
| 4 | aps | ErpApsOpRouting | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-aps/model/app-erp-aps.orm.xml:214 |
| 5 | aps | ErpApsOperationOrder | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-aps/model/app-erp-aps.orm.xml:90 |
| 6 | aps | ErpApsSchedule | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-aps/model/app-erp-aps.orm.xml:140 |
| 7 | ast | ErpAstAsset | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-assets/model/app-erp-assets.orm.xml:221 |
| 8 | ast | ErpAstAssetCapitalization | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-assets/model/app-erp-assets.orm.xml:686 |
| 9 | ast | ErpAstCip | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-assets/model/app-erp-assets.orm.xml:754 |
| 10 | ast | ErpAstCipCostItem | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-assets/model/app-erp-assets.orm.xml:825 |
| 11 | ast | ErpAstCipProgressBilling | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-assets/model/app-erp-assets.orm.xml:877 |
| 12 | ast | ErpAstDepreciationSchedule | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-assets/model/app-erp-assets.orm.xml:364 |
| 13 | ast | ErpAstDisposal | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-assets/model/app-erp-assets.orm.xml:616 |
| 14 | ast | ErpAstInventory | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-assets/model/app-erp-assets.orm.xml:1179 |
| 15 | ast | ErpAstMaintenance | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-assets/model/app-erp-assets.orm.xml:1331 |
| 16 | ast | ErpAstMaintenanceCost | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-assets/model/app-erp-assets.orm.xml:1391 |
| 17 | ast | ErpAstMerge | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-assets/model/app-erp-assets.orm.xml:996 |
| 18 | ast | ErpAstMovement | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-assets/model/app-erp-assets.orm.xml:459 |
| 19 | ast | ErpAstSplit | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-assets/model/app-erp-assets.orm.xml:931 |
| 20 | ast | ErpAstValueAdjustment | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-assets/model/app-erp-assets.orm.xml:547 |
| 21 | ast | ErpAstAsset | -> | md | app.erp.md.dao.entity.ErpMdEmployee | employee | to-one | module-assets/model/app-erp-assets.orm.xml:218 |
| 22 | ast | ErpAstAsset | -> | md | app.erp.md.dao.entity.ErpMdEmployee | staff | to-one | module-assets/model/app-erp-assets.orm.xml:225 |
| 23 | ast | ErpAstInventory | -> | md | app.erp.md.dao.entity.ErpMdEmployee | responsibleBy | to-one | module-assets/model/app-erp-assets.orm.xml:1176 |
| 24 | ast | ErpAstMovement | -> | md | app.erp.md.dao.entity.ErpMdEmployee | fromStaff | to-one | module-assets/model/app-erp-assets.orm.xml:444 |
| 25 | ast | ErpAstMovement | -> | md | app.erp.md.dao.entity.ErpMdEmployee | handler | to-one | module-assets/model/app-erp-assets.orm.xml:456 |
| 26 | ast | ErpAstMovement | -> | md | app.erp.md.dao.entity.ErpMdEmployee | toStaff | to-one | module-assets/model/app-erp-assets.orm.xml:447 |
| 27 | ast | ErpAstAsset | -> | md | app.erp.md.dao.entity.ErpMdLocation | location | to-one | module-assets/model/app-erp-assets.orm.xml:215 |
| 28 | ast | ErpAstInventory | -> | md | app.erp.md.dao.entity.ErpMdLocation | rangeLocation | to-one | module-assets/model/app-erp-assets.orm.xml:1173 |
| 29 | ast | ErpAstMovement | -> | md | app.erp.md.dao.entity.ErpMdLocation | fromLocation | to-one | module-assets/model/app-erp-assets.orm.xml:450 |
| 30 | ast | ErpAstMovement | -> | md | app.erp.md.dao.entity.ErpMdLocation | toLocation | to-one | module-assets/model/app-erp-assets.orm.xml:453 |
| 31 | ast | ErpAstAsset | -> | md | app.erp.md.dao.entity.ErpMdOrganization | department | to-one | module-assets/model/app-erp-assets.orm.xml:212 |
| 32 | ast | ErpAstAsset | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-assets/model/app-erp-assets.orm.xml:224 |
| 33 | ast | ErpAstAssetCapitalization | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-assets/model/app-erp-assets.orm.xml:683 |
| 34 | ast | ErpAstCip | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-assets/model/app-erp-assets.orm.xml:757 |
| 35 | ast | ErpAstCipCostItem | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-assets/model/app-erp-assets.orm.xml:822 |
| 36 | ast | ErpAstCipProgressBilling | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-assets/model/app-erp-assets.orm.xml:874 |
| 37 | ast | ErpAstDepreciationSchedule | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-assets/model/app-erp-assets.orm.xml:361 |
| 38 | ast | ErpAstDisposal | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-assets/model/app-erp-assets.orm.xml:613 |
| 39 | ast | ErpAstInventory | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-assets/model/app-erp-assets.orm.xml:1164 |
| 40 | ast | ErpAstInventory | -> | md | app.erp.md.dao.entity.ErpMdOrganization | rangeDepartment | to-one | module-assets/model/app-erp-assets.orm.xml:1167 |
| 41 | ast | ErpAstInventoryLine | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-assets/model/app-erp-assets.orm.xml:1250 |
| 42 | ast | ErpAstMaintenance | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-assets/model/app-erp-assets.orm.xml:1328 |
| 43 | ast | ErpAstMaintenanceCost | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-assets/model/app-erp-assets.orm.xml:1388 |
| 44 | ast | ErpAstMerge | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-assets/model/app-erp-assets.orm.xml:993 |
| 45 | ast | ErpAstMergeLine | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-assets/model/app-erp-assets.orm.xml:1106 |
| 46 | ast | ErpAstMovement | -> | md | app.erp.md.dao.entity.ErpMdOrganization | fromDepartment | to-one | module-assets/model/app-erp-assets.orm.xml:438 |
| 47 | ast | ErpAstMovement | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-assets/model/app-erp-assets.orm.xml:435 |
| 48 | ast | ErpAstMovement | -> | md | app.erp.md.dao.entity.ErpMdOrganization | toDepartment | to-one | module-assets/model/app-erp-assets.orm.xml:441 |
| 49 | ast | ErpAstSplit | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-assets/model/app-erp-assets.orm.xml:928 |
| 50 | ast | ErpAstSplitLine | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-assets/model/app-erp-assets.orm.xml:1054 |
| 51 | ast | ErpAstValueAdjustment | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-assets/model/app-erp-assets.orm.xml:544 |
| 52 | ast | ErpAstAssetCategory | -> | md | app.erp.md.dao.entity.ErpMdSubject | cipSubject | to-one | module-assets/model/app-erp-assets.orm.xml:297 |
| 53 | ast | ErpAstAssetCategory | -> | md | app.erp.md.dao.entity.ErpMdSubject | depreciationSubject | to-one | module-assets/model/app-erp-assets.orm.xml:288 |
| 54 | ast | ErpAstAssetCategory | -> | md | app.erp.md.dao.entity.ErpMdSubject | disposalGainLossSubject | to-one | module-assets/model/app-erp-assets.orm.xml:294 |
| 55 | ast | ErpAstAssetCategory | -> | md | app.erp.md.dao.entity.ErpMdSubject | expenseSubject | to-one | module-assets/model/app-erp-assets.orm.xml:291 |
| 56 | ast | ErpAstAssetCategory | -> | md | app.erp.md.dao.entity.ErpMdSubject | subject | to-one | module-assets/model/app-erp-assets.orm.xml:285 |
| 57 | b2b | ErpB2bAsnLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-b2b/model/app-erp-b2b.orm.xml:286 |
| 58 | b2b | ErpB2bAsn | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-b2b/model/app-erp-b2b.orm.xml:240 |
| 59 | b2b | ErpB2bCodeMapping | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-b2b/model/app-erp-b2b.orm.xml:318 |
| 60 | b2b | ErpB2bEdiDoc | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-b2b/model/app-erp-b2b.orm.xml:191 |
| 61 | b2b | ErpB2bEdiFormat | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-b2b/model/app-erp-b2b.orm.xml:155 |
| 62 | b2b | ErpB2bEdiLog | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-b2b/model/app-erp-b2b.orm.xml:352 |
| 63 | b2b | ErpB2bMftCertificate | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-b2b/model/app-erp-b2b.orm.xml:597 |
| 64 | b2b | ErpB2bMftConfig | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-b2b/model/app-erp-b2b.orm.xml:547 |
| 65 | b2b | ErpB2bMftLog | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-b2b/model/app-erp-b2b.orm.xml:646 |
| 66 | b2b | ErpB2bPartnerProfile | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-b2b/model/app-erp-b2b.orm.xml:395 |
| 67 | b2b | ErpB2bAsn | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-b2b/model/app-erp-b2b.orm.xml:239 |
| 68 | b2b | ErpB2bCodeMapping | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-b2b/model/app-erp-b2b.orm.xml:319 |
| 69 | b2b | ErpB2bMftCertificate | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-b2b/model/app-erp-b2b.orm.xml:596 |
| 70 | b2b | ErpB2bMftConfig | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-b2b/model/app-erp-b2b.orm.xml:546 |
| 71 | b2b | ErpB2bPartnerProfile | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-b2b/model/app-erp-b2b.orm.xml:394 |
| 72 | crm | ErpCrmForecast | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-crm/model/app-erp-crm.orm.xml:846 |
| 73 | crm | ErpCrmPriceRule | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-crm/model/app-erp-crm.orm.xml:1270 |
| 74 | crm | ErpCrmQuota | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-crm/model/app-erp-crm.orm.xml:1073 |
| 75 | crm | ErpCrmBundlePricingLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | product | to-one | module-crm/model/app-erp-crm.orm.xml:1216 |
| 76 | crm | ErpCrmPriceRule | -> | md | app.erp.md.dao.entity.ErpMdMaterial | product | to-one | module-crm/model/app-erp-crm.orm.xml:1264 |
| 77 | crm | ErpCrmActivity | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:562 |
| 78 | crm | ErpCrmBundlePricing | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:1186 |
| 79 | crm | ErpCrmBundlePricingLine | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:1215 |
| 80 | crm | ErpCrmCampaign | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:440 |
| 81 | crm | ErpCrmConfigRule | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:1151 |
| 82 | crm | ErpCrmEvent | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:485 |
| 83 | crm | ErpCrmForecast | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:845 |
| 84 | crm | ErpCrmForecastAccuracy | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:936 |
| 85 | crm | ErpCrmForecastLine | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:894 |
| 86 | crm | ErpCrmForecastPeriod | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:800 |
| 87 | crm | ErpCrmFunnelStageMetrics | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:1513 |
| 88 | crm | ErpCrmLead | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:239 |
| 89 | crm | ErpCrmLeadConvLog | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:598 |
| 90 | crm | ErpCrmLeadFunnel | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:1464 |
| 91 | crm | ErpCrmLeadScore | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:727 |
| 92 | crm | ErpCrmLeadScoreConfig | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:659 |
| 93 | crm | ErpCrmLeadScoreConfigLine | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:692 |
| 94 | crm | ErpCrmLeadScoreLine | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:765 |
| 95 | crm | ErpCrmLeadSequenceProgress | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:1413 |
| 96 | crm | ErpCrmPriceRule | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:1263 |
| 97 | crm | ErpCrmProductConfigurator | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:1119 |
| 98 | crm | ErpCrmQuota | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:1072 |
| 99 | crm | ErpCrmSequence | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:1315 |
| 100 | crm | ErpCrmSequenceAssignment | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:1380 |
| 101 | crm | ErpCrmSequenceStep | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:1348 |
| 102 | crm | ErpCrmStage | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:328 |
| 103 | crm | ErpCrmTeam | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:403 |
| 104 | crm | ErpCrmTerritory | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:991 |
| 105 | crm | ErpCrmTerritoryAssignmentRule | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-crm/model/app-erp-crm.orm.xml:1033 |
| 106 | crm | ErpCrmEvent | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-crm/model/app-erp-crm.orm.xml:483 |
| 107 | crm | ErpCrmLead | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-crm/model/app-erp-crm.orm.xml:231 |
| 108 | crm | ErpCrmPriceRule | -> | md | app.erp.md.dao.entity.ErpMdPartner | customer | to-one | module-crm/model/app-erp-crm.orm.xml:1267 |
| 109 | crm | ErpCrmEvent | -> | md | app.erp.md.dao.entity.ErpMdPartnerContact | contact | to-one | module-crm/model/app-erp-crm.orm.xml:487 |
| 110 | cs | ErpCsAgentRate | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-cs/model/app-erp-cs.orm.xml:531 |
| 111 | cs | ErpCsCannedCategory | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-cs/model/app-erp-cs.orm.xml:417 |
| 112 | cs | ErpCsCannedResponse | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-cs/model/app-erp-cs.orm.xml:458 |
| 113 | cs | ErpCsCatalogCategory | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-cs/model/app-erp-cs.orm.xml:668 |
| 114 | cs | ErpCsCatalogFulfillment | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-cs/model/app-erp-cs.orm.xml:765 |
| 115 | cs | ErpCsContract | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-cs/model/app-erp-cs.orm.xml:569 |
| 116 | cs | ErpCsEntitlement | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-cs/model/app-erp-cs.orm.xml:626 |
| 117 | cs | ErpCsServiceCatalogItem | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-cs/model/app-erp-cs.orm.xml:715 |
| 118 | cs | ErpCsSurvey | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-cs/model/app-erp-cs.orm.xml:500 |
| 119 | cs | ErpCsTicket | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-cs/model/app-erp-cs.orm.xml:185 |
| 120 | cs | ErpCsTimeEntry | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-cs/model/app-erp-cs.orm.xml:810 |
| 121 | cs | ErpCsContract | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-cs/model/app-erp-cs.orm.xml:568 |
| 122 | cs | ErpCsEntitlement | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-cs/model/app-erp-cs.orm.xml:623 |
| 123 | cs | ErpCsTicket | -> | md | app.erp.md.dao.entity.ErpMdPartner | contact | to-one | module-cs/model/app-erp-cs.orm.xml:182 |
| 124 | cs | ErpCsTicket | -> | md | app.erp.md.dao.entity.ErpMdPartner | customer | to-one | module-cs/model/app-erp-cs.orm.xml:181 |
| 125 | ct | ErpCtContract | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-contract/model/app-erp-contract.orm.xml:159 |
| 126 | ct | ErpCtContractLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-contract/model/app-erp-contract.orm.xml:219 |
| 127 | ct | ErpCtApprovalMatrix | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-contract/model/app-erp-contract.orm.xml:383 |
| 128 | ct | ErpCtApprovalRecord | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-contract/model/app-erp-contract.orm.xml:415 |
| 129 | ct | ErpCtContract | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-contract/model/app-erp-contract.orm.xml:156 |
| 130 | ct | ErpCtDocument | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-contract/model/app-erp-contract.orm.xml:711 |
| 131 | ct | ErpCtRebateAccrual | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-contract/model/app-erp-contract.orm.xml:573 |
| 132 | ct | ErpCtRebateAgreement | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-contract/model/app-erp-contract.orm.xml:493 |
| 133 | ct | ErpCtRebateSettlement | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-contract/model/app-erp-contract.orm.xml:609 |
| 134 | ct | ErpCtSignatureRequest | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-contract/model/app-erp-contract.orm.xml:654 |
| 135 | ct | ErpCtVolumeDiscount | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-contract/model/app-erp-contract.orm.xml:455 |
| 136 | ct | ErpCtContract | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-contract/model/app-erp-contract.orm.xml:153 |
| 137 | ct | ErpCtRebateAgreement | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-contract/model/app-erp-contract.orm.xml:494 |
| 138 | drp | ErpInvDrpCrossDock | -> | inv | app.erp.inv.dao.entity.ErpInvStockMove | inboundMove | to-one | module-drp/model/app-erp-drp.orm.xml:297 |
| 139 | drp | ErpInvDrpCrossDock | -> | inv | app.erp.inv.dao.entity.ErpInvStockMove | outboundMove | to-one | module-drp/model/app-erp-drp.orm.xml:298 |
| 140 | drp | ErpInvDrpCrossDock | -> | md | app.erp.md.dao.entity.ErpMdLocation | stagingLocation | to-one | module-drp/model/app-erp-drp.orm.xml:296 |
| 141 | drp | ErpDrpLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-drp/model/app-erp-drp.orm.xml:144 |
| 142 | drp | ErpDrpParameter | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-drp/model/app-erp-drp.orm.xml:197 |
| 143 | drp | ErpDrpScenarioParam | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-drp/model/app-erp-drp.orm.xml:510 |
| 144 | drp | ErpInvDrpCrossDock | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-drp/model/app-erp-drp.orm.xml:295 |
| 145 | drp | ErpInvDrpLeadTimeRecord | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-drp/model/app-erp-drp.orm.xml:400 |
| 146 | drp | ErpInvDrpSafetyStockCalc | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-drp/model/app-erp-drp.orm.xml:246 |
| 147 | drp | ErpDrpLine | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-drp/model/app-erp-drp.orm.xml:147 |
| 148 | drp | ErpDrpParameter | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-drp/model/app-erp-drp.orm.xml:200 |
| 149 | drp | ErpDrpPlan | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-drp/model/app-erp-drp.orm.xml:93 |
| 150 | drp | ErpDrpScenario | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-drp/model/app-erp-drp.orm.xml:437 |
| 151 | drp | ErpInvDrpCrossDock | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-drp/model/app-erp-drp.orm.xml:299 |
| 152 | drp | ErpInvDrpDockAppointment | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-drp/model/app-erp-drp.orm.xml:354 |
| 153 | drp | ErpInvDrpLeadTimeRecord | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-drp/model/app-erp-drp.orm.xml:401 |
| 154 | drp | ErpInvDrpSafetyStockCalc | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-drp/model/app-erp-drp.orm.xml:248 |
| 155 | drp | ErpDrpParameter | -> | md | app.erp.md.dao.entity.ErpMdPartner | preferredSupplier | to-one | module-drp/model/app-erp-drp.orm.xml:199 |
| 156 | drp | ErpInvDrpLeadTimeRecord | -> | md | app.erp.md.dao.entity.ErpMdPartner | supplier | to-one | module-drp/model/app-erp-drp.orm.xml:399 |
| 157 | drp | ErpDrpLine | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | sourceWarehouse | to-one | module-drp/model/app-erp-drp.orm.xml:146 |
| 158 | drp | ErpDrpLine | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-drp/model/app-erp-drp.orm.xml:145 |
| 159 | drp | ErpDrpParameter | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | preferredSourceWarehouse | to-one | module-drp/model/app-erp-drp.orm.xml:198 |
| 160 | drp | ErpDrpParameter | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-drp/model/app-erp-drp.orm.xml:196 |
| 161 | drp | ErpDrpScenarioParam | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-drp/model/app-erp-drp.orm.xml:511 |
| 162 | drp | ErpInvDrpDockAppointment | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-drp/model/app-erp-drp.orm.xml:351 |
| 163 | drp | ErpInvDrpSafetyStockCalc | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-drp/model/app-erp-drp.orm.xml:247 |
| 164 | fin | ErpFinAccountingPeriodStatus | -> | md | app.erp.md.dao.entity.ErpMdAcctSchema | acctSchema | to-one | module-finance/model/app-erp-finance.orm.xml:717 |
| 165 | fin | ErpFinArApItem | -> | md | app.erp.md.dao.entity.ErpMdAcctSchema | acctSchema | to-one | module-finance/model/app-erp-finance.orm.xml:764 |
| 166 | fin | ErpFinBadDebt | -> | md | app.erp.md.dao.entity.ErpMdAcctSchema | acctSchema | to-one | module-finance/model/app-erp-finance.orm.xml:1692 |
| 167 | fin | ErpFinBudgetLine | -> | md | app.erp.md.dao.entity.ErpMdAcctSchema | acctSchema | to-one | module-finance/model/app-erp-finance.orm.xml:1839 |
| 168 | fin | ErpFinBudgetScenario | -> | md | app.erp.md.dao.entity.ErpMdAcctSchema | acctSchema | to-one | module-finance/model/app-erp-finance.orm.xml:1776 |
| 169 | fin | ErpFinGlBalance | -> | md | app.erp.md.dao.entity.ErpMdAcctSchema | acctSchema | to-one | module-finance/model/app-erp-finance.orm.xml:935 |
| 170 | fin | ErpFinGlMappingRule | -> | md | app.erp.md.dao.entity.ErpMdAcctSchema | acctSchema | to-one | module-finance/model/app-erp-finance.orm.xml:2044 |
| 171 | fin | ErpFinPostingException | -> | md | app.erp.md.dao.entity.ErpMdAcctSchema | acctSchema | to-one | module-finance/model/app-erp-finance.orm.xml:1656 |
| 172 | fin | ErpFinReconciliation | -> | md | app.erp.md.dao.entity.ErpMdAcctSchema | acctSchema | to-one | module-finance/model/app-erp-finance.orm.xml:830 |
| 173 | fin | ErpFinTrialBalance | -> | md | app.erp.md.dao.entity.ErpMdAcctSchema | acctSchema | to-one | module-finance/model/app-erp-finance.orm.xml:1010 |
| 174 | fin | ErpFinVoucher | -> | md | app.erp.md.dao.entity.ErpMdAcctSchema | acctSchema | to-one | module-finance/model/app-erp-finance.orm.xml:441 |
| 175 | fin | ErpFinVoucherLine | -> | md | app.erp.md.dao.entity.ErpMdAcctSchema | acctSchema | to-one | module-finance/model/app-erp-finance.orm.xml:510 |
| 176 | fin | ErpFinVoucherTemplate | -> | md | app.erp.md.dao.entity.ErpMdAcctSchema | acctSchema | to-one | module-finance/model/app-erp-finance.orm.xml:578 |
| 177 | fin | ErpFinBudgetControlLog | -> | md | app.erp.md.dao.entity.ErpMdCostCenter | costCenter | to-one | module-finance/model/app-erp-finance.orm.xml:1904 |
| 178 | fin | ErpFinBudgetLine | -> | md | app.erp.md.dao.entity.ErpMdCostCenter | costCenter | to-one | module-finance/model/app-erp-finance.orm.xml:1842 |
| 179 | fin | ErpFinExpenseClaimLine | -> | md | app.erp.md.dao.entity.ErpMdCostCenter | costCenter | to-one | module-finance/model/app-erp-finance.orm.xml:1347 |
| 180 | fin | ErpFinVoucherLine | -> | md | app.erp.md.dao.entity.ErpMdCostCenter | costCenter | to-one | module-finance/model/app-erp-finance.orm.xml:518 |
| 181 | fin | ErpFinArApItem | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-finance/model/app-erp-finance.orm.xml:765 |
| 182 | fin | ErpFinBadDebt | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-finance/model/app-erp-finance.orm.xml:1693 |
| 183 | fin | ErpFinBankStatementLine | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-finance/model/app-erp-finance.orm.xml:1151 |
| 184 | fin | ErpFinBudgetLine | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-finance/model/app-erp-finance.orm.xml:1848 |
| 185 | fin | ErpFinBudgetScenario | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-finance/model/app-erp-finance.orm.xml:1777 |
| 186 | fin | ErpFinEmployeeAdvance | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-finance/model/app-erp-finance.orm.xml:1401 |
| 187 | fin | ErpFinEmployeeAdvance | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-finance/model/app-erp-finance.orm.xml:1467 |
| 188 | fin | ErpFinEmployeeAdvance | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-finance/model/app-erp-finance.orm.xml:1507 |
| 189 | fin | ErpFinExpenseClaim | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-finance/model/app-erp-finance.orm.xml:1287 |
| 190 | fin | ErpFinFundAccount | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-finance/model/app-erp-finance.orm.xml:1056 |
| 191 | fin | ErpFinGlBalance | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-finance/model/app-erp-finance.orm.xml:938 |
| 192 | fin | ErpFinPostingException | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-finance/model/app-erp-finance.orm.xml:1658 |
| 193 | fin | ErpFinReconciliation | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-finance/model/app-erp-finance.orm.xml:831 |
| 194 | fin | ErpFinVoucherLine | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-finance/model/app-erp-finance.orm.xml:511 |
| 195 | fin | ErpFinEmployeeAdvance | -> | md | app.erp.md.dao.entity.ErpMdEmployee | employee | to-one | module-finance/model/app-erp-finance.orm.xml:1400 |
| 196 | fin | ErpFinExpenseClaim | -> | md | app.erp.md.dao.entity.ErpMdEmployee | claimant | to-one | module-finance/model/app-erp-finance.orm.xml:1285 |
| 197 | fin | ErpFinBudgetLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-finance/model/app-erp-finance.orm.xml:1847 |
| 198 | fin | ErpFinIntercompanyMatch | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-finance/model/app-erp-finance.orm.xml:2155 |
| 199 | fin | ErpFinIntercompanyTransferPrice | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-finance/model/app-erp-finance.orm.xml:2106 |
| 200 | fin | ErpFinVoucherLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-finance/model/app-erp-finance.orm.xml:516 |
| 201 | fin | ErpFinGlMappingRule | -> | md | app.erp.md.dao.entity.ErpMdMaterialCategory | materialCategory | to-one | module-finance/model/app-erp-finance.orm.xml:2045 |
| 202 | fin | ErpFinIntercompanyTransferPrice | -> | md | app.erp.md.dao.entity.ErpMdMaterialCategory | materialCategory | to-one | module-finance/model/app-erp-finance.orm.xml:2107 |
| 203 | fin | ErpFinAccountingPeriod | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:677 |
| 204 | fin | ErpFinArApItem | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:769 |
| 205 | fin | ErpFinBadDebt | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:1697 |
| 206 | fin | ErpFinBankReconciliation | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:1196 |
| 207 | fin | ErpFinBankStatement | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:1105 |
| 208 | fin | ErpFinBudgetCarryForwardLog | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:1998 |
| 209 | fin | ErpFinBudgetControlLog | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:1906 |
| 210 | fin | ErpFinBudgetLine | -> | md | app.erp.md.dao.entity.ErpMdOrganization | department | to-one | module-finance/model/app-erp-finance.orm.xml:1843 |
| 211 | fin | ErpFinBudgetLine | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:1849 |
| 212 | fin | ErpFinBudgetRollforwardLog | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:1957 |
| 213 | fin | ErpFinBudgetScenario | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:1778 |
| 214 | fin | ErpFinConsolidationElimination | -> | md | app.erp.md.dao.entity.ErpMdOrganization | fromOrg | to-one | module-finance/model/app-erp-finance.orm.xml:2206 |
| 215 | fin | ErpFinConsolidationElimination | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:2208 |
| 216 | fin | ErpFinConsolidationElimination | -> | md | app.erp.md.dao.entity.ErpMdOrganization | toOrg | to-one | module-finance/model/app-erp-finance.orm.xml:2207 |
| 217 | fin | ErpFinEmployeeAdvance | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:1403 |
| 218 | fin | ErpFinEmployeeAdvance | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:1468 |
| 219 | fin | ErpFinEmployeeAdvance | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:1508 |
| 220 | fin | ErpFinEmployeeAdvance | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:1541 |
| 221 | fin | ErpFinEmployeeAdvance | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:1569 |
| 222 | fin | ErpFinExpenseClaim | -> | md | app.erp.md.dao.entity.ErpMdOrganization | department | to-one | module-finance/model/app-erp-finance.orm.xml:1286 |
| 223 | fin | ErpFinExpenseClaim | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:1288 |
| 224 | fin | ErpFinFundAccount | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:1058 |
| 225 | fin | ErpFinGlBalance | -> | md | app.erp.md.dao.entity.ErpMdOrganization | department | to-one | module-finance/model/app-erp-finance.orm.xml:945 |
| 226 | fin | ErpFinGlBalance | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:939 |
| 227 | fin | ErpFinGlMappingRule | -> | md | app.erp.md.dao.entity.ErpMdOrganization | department | to-one | module-finance/model/app-erp-finance.orm.xml:2047 |
| 228 | fin | ErpFinGlMappingRule | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:2049 |
| 229 | fin | ErpFinIntercompanyMatch | -> | md | app.erp.md.dao.entity.ErpMdOrganization | apOrg | to-one | module-finance/model/app-erp-finance.orm.xml:2157 |
| 230 | fin | ErpFinIntercompanyMatch | -> | md | app.erp.md.dao.entity.ErpMdOrganization | arOrg | to-one | module-finance/model/app-erp-finance.orm.xml:2156 |
| 231 | fin | ErpFinIntercompanyMatch | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:2158 |
| 232 | fin | ErpFinIntercompanyTransferPrice | -> | md | app.erp.md.dao.entity.ErpMdOrganization | fromOrg | to-one | module-finance/model/app-erp-finance.orm.xml:2104 |
| 233 | fin | ErpFinIntercompanyTransferPrice | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:2108 |
| 234 | fin | ErpFinIntercompanyTransferPrice | -> | md | app.erp.md.dao.entity.ErpMdOrganization | toOrg | to-one | module-finance/model/app-erp-finance.orm.xml:2105 |
| 235 | fin | ErpFinPostingException | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:1655 |
| 236 | fin | ErpFinReconciliation | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:833 |
| 237 | fin | ErpFinTrialBalance | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:1013 |
| 238 | fin | ErpFinVoucher | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:444 |
| 239 | fin | ErpFinVoucherLine | -> | md | app.erp.md.dao.entity.ErpMdOrganization | department | to-one | module-finance/model/app-erp-finance.orm.xml:513 |
| 240 | fin | ErpFinVoucherLine | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-finance/model/app-erp-finance.orm.xml:515 |
| 241 | fin | ErpFinArApItem | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-finance/model/app-erp-finance.orm.xml:763 |
| 242 | fin | ErpFinBadDebt | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-finance/model/app-erp-finance.orm.xml:1691 |
| 243 | fin | ErpFinBudgetLine | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-finance/model/app-erp-finance.orm.xml:1845 |
| 244 | fin | ErpFinEmployeeAdvance | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-finance/model/app-erp-finance.orm.xml:1466 |
| 245 | fin | ErpFinEmployeeAdvance | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-finance/model/app-erp-finance.orm.xml:1506 |
| 246 | fin | ErpFinEmployeeAdvance | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-finance/model/app-erp-finance.orm.xml:1596 |
| 247 | fin | ErpFinGlBalance | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-finance/model/app-erp-finance.orm.xml:942 |
| 248 | fin | ErpFinReconciliation | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-finance/model/app-erp-finance.orm.xml:829 |
| 249 | fin | ErpFinVoucherLine | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-finance/model/app-erp-finance.orm.xml:512 |
| 250 | fin | ErpFinBudgetControlLog | -> | md | app.erp.md.dao.entity.ErpMdSubject | subject | to-one | module-finance/model/app-erp-finance.orm.xml:1903 |
| 251 | fin | ErpFinBudgetLine | -> | md | app.erp.md.dao.entity.ErpMdSubject | subject | to-one | module-finance/model/app-erp-finance.orm.xml:1841 |
| 252 | fin | ErpFinExpenseClaimLine | -> | md | app.erp.md.dao.entity.ErpMdSubject | subject | to-one | module-finance/model/app-erp-finance.orm.xml:1348 |
| 253 | fin | ErpFinFundAccount | -> | md | app.erp.md.dao.entity.ErpMdSubject | subject | to-one | module-finance/model/app-erp-finance.orm.xml:1057 |
| 254 | fin | ErpFinGlBalance | -> | md | app.erp.md.dao.entity.ErpMdSubject | subject | to-one | module-finance/model/app-erp-finance.orm.xml:937 |
| 255 | fin | ErpFinTrialBalance | -> | md | app.erp.md.dao.entity.ErpMdSubject | subject | to-one | module-finance/model/app-erp-finance.orm.xml:1012 |
| 256 | fin | ErpFinVoucherLine | -> | md | app.erp.md.dao.entity.ErpMdSubject | subject | to-one | module-finance/model/app-erp-finance.orm.xml:509 |
| 257 | fin | ErpFinBudgetLine | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-finance/model/app-erp-finance.orm.xml:1846 |
| 258 | fin | ErpFinGlBalance | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-finance/model/app-erp-finance.orm.xml:948 |
| 259 | fin | ErpFinGlMappingRule | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-finance/model/app-erp-finance.orm.xml:2046 |
| 260 | fin | ErpFinVoucherLine | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-finance/model/app-erp-finance.orm.xml:514 |
| 261 | fin | ErpFinBudgetLine | -> | prj | app.erp.prj.dao.entity.ErpPrjProject | project | to-one | module-finance/model/app-erp-finance.orm.xml:1844 |
| 262 | fin | ErpFinEmployeeAdvance | -> | prj | app.erp.prj.dao.entity.ErpPrjProject | project | to-one | module-finance/model/app-erp-finance.orm.xml:1402 |
| 263 | fin | ErpFinExpenseClaimLine | -> | prj | app.erp.prj.dao.entity.ErpPrjProject | project | to-one | module-finance/model/app-erp-finance.orm.xml:1346 |
| 264 | fin | ErpFinGlBalance | -> | prj | app.erp.prj.dao.entity.ErpPrjProject | project | to-one | module-finance/model/app-erp-finance.orm.xml:951 |
| 265 | fin | ErpFinGlMappingRule | -> | prj | app.erp.prj.dao.entity.ErpPrjProject | project | to-one | module-finance/model/app-erp-finance.orm.xml:2048 |
| 266 | fin | ErpFinVoucherLine | -> | prj | app.erp.prj.dao.entity.ErpPrjProject | project | to-one | module-finance/model/app-erp-finance.orm.xml:517 |
| 267 | hr | ErpHrEmployee | -> | md | app.erp.md.dao.entity.ErpMdBankAccount | bankAccount | to-one | module-hr/model/app-erp-hr.orm.xml:314 |
| 268 | hr | ErpHrPayrollBankFile | -> | md | app.erp.md.dao.entity.ErpMdBankAccount | bank | to-one | module-hr/model/app-erp-hr.orm.xml:1118 |
| 269 | hr | ErpHrDepartment | -> | md | app.erp.md.dao.entity.ErpMdCostCenter | costCenter | to-one | module-hr/model/app-erp-hr.orm.xml:365 |
| 270 | hr | ErpHrEmployee | -> | md | app.erp.md.dao.entity.ErpMdCostCenter | costCenter | to-one | module-hr/model/app-erp-hr.orm.xml:313 |
| 271 | hr | ErpHrEmploymentContract | -> | md | app.erp.md.dao.entity.ErpMdCurrency | salaryCurrency | to-one | module-hr/model/app-erp-hr.orm.xml:452 |
| 272 | hr | ErpHrAttendance | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:693 |
| 273 | hr | ErpHrCompetency | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:1541 |
| 274 | hr | ErpHrDepartment | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:366 |
| 275 | hr | ErpHrDevelopmentPlan | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:1749 |
| 276 | hr | ErpHrEmployee | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:315 |
| 277 | hr | ErpHrEmployeeAssessment | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:1639 |
| 278 | hr | ErpHrEmploymentContract | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:454 |
| 279 | hr | ErpHrLeaveBalance | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:556 |
| 280 | hr | ErpHrLeaveRequest | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:509 |
| 281 | hr | ErpHrPayrollBankFile | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:1119 |
| 282 | hr | ErpHrPosition | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:406 |
| 283 | hr | ErpHrRecruitment | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:821 |
| 284 | hr | ErpHrSalary | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:763 |
| 285 | hr | ErpHrSalaryItem | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:971 |
| 286 | hr | ErpHrSalarySimulation | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:883 |
| 287 | hr | ErpHrSalarySimulationItemAdjustment | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:935 |
| 288 | hr | ErpHrShift | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:1162 |
| 289 | hr | ErpHrShiftAssignment | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:1201 |
| 290 | hr | ErpHrShiftRotationPattern | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:1253 |
| 291 | hr | ErpHrShiftSwapRequest | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:1294 |
| 292 | hr | ErpHrSocialInsuranceBase | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:1029 |
| 293 | hr | ErpHrSocialInsuranceConfig | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:1002 |
| 294 | hr | ErpHrSurvey | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:1362 |
| 295 | hr | ErpHrSurveyResponse | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:1436 |
| 296 | hr | ErpHrTaxConfig | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:1058 |
| 297 | hr | ErpHrTaxSpecialDeduction | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:1085 |
| 298 | hr | ErpHrTimesheet | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-hr/model/app-erp-hr.orm.xml:601 |
| 299 | hr | ErpHrTimesheetLine | -> | prj | app.erp.prj.dao.entity.ErpPrjProject | project | to-one | module-hr/model/app-erp-hr.orm.xml:645 |
| 300 | hr | ErpHrTimesheetLine | -> | prj | app.erp.prj.dao.entity.ErpPrjTask | task | to-one | module-hr/model/app-erp-hr.orm.xml:646 |
| 301 | inv | ErpInvCostLayer | -> | md | app.erp.md.dao.entity.ErpMdAcctSchema | acctSchema | to-one | module-inventory/model/app-erp-inventory.orm.xml:559 |
| 302 | inv | ErpInvStockLedger | -> | md | app.erp.md.dao.entity.ErpMdAcctSchema | acctSchema | to-one | module-inventory/model/app-erp-inventory.orm.xml:325 |
| 303 | inv | ErpInvCostAdjust | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-inventory/model/app-erp-inventory.orm.xml:1238 |
| 304 | inv | ErpInvCostAdjustLine | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-inventory/model/app-erp-inventory.orm.xml:1288 |
| 305 | inv | ErpInvCostLayer | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-inventory/model/app-erp-inventory.orm.xml:556 |
| 306 | inv | ErpInvLandedCost | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-inventory/model/app-erp-inventory.orm.xml:1335 |
| 307 | inv | ErpInvOwnershipTransfer | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-inventory/model/app-erp-inventory.orm.xml:1019 |
| 308 | inv | ErpInvStockBalance | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-inventory/model/app-erp-inventory.orm.xml:399 |
| 309 | inv | ErpInvStockLedger | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-inventory/model/app-erp-inventory.orm.xml:323 |
| 310 | inv | ErpInvStockMoveLine | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-inventory/model/app-erp-inventory.orm.xml:253 |
| 311 | inv | ErpInvPickingOrder | -> | md | app.erp.md.dao.entity.ErpMdEmployee | picker | to-one | module-inventory/model/app-erp-inventory.orm.xml:808 |
| 312 | inv | ErpInvOwnershipTransfer | -> | md | app.erp.md.dao.entity.ErpMdLocation | destLoc | to-one | module-inventory/model/app-erp-inventory.orm.xml:1018 |
| 313 | inv | ErpInvOwnershipTransfer | -> | md | app.erp.md.dao.entity.ErpMdLocation | sourceLoc | to-one | module-inventory/model/app-erp-inventory.orm.xml:1017 |
| 314 | inv | ErpInvPickingOrderLine | -> | md | app.erp.md.dao.entity.ErpMdLocation | sourceLocation | to-one | module-inventory/model/app-erp-inventory.orm.xml:860 |
| 315 | inv | ErpInvReservationLine | -> | md | app.erp.md.dao.entity.ErpMdLocation | location | to-one | module-inventory/model/app-erp-inventory.orm.xml:500 |
| 316 | inv | ErpInvSerialNumber | -> | md | app.erp.md.dao.entity.ErpMdLocation | location | to-one | module-inventory/model/app-erp-inventory.orm.xml:959 |
| 317 | inv | ErpInvStockBalance | -> | md | app.erp.md.dao.entity.ErpMdLocation | location | to-one | module-inventory/model/app-erp-inventory.orm.xml:400 |
| 318 | inv | ErpInvStockLedger | -> | md | app.erp.md.dao.entity.ErpMdLocation | location | to-one | module-inventory/model/app-erp-inventory.orm.xml:322 |
| 319 | inv | ErpInvStockMove | -> | md | app.erp.md.dao.entity.ErpMdLocation | destLocation | to-one | module-inventory/model/app-erp-inventory.orm.xml:183 |
| 320 | inv | ErpInvStockMove | -> | md | app.erp.md.dao.entity.ErpMdLocation | sourceLocation | to-one | module-inventory/model/app-erp-inventory.orm.xml:182 |
| 321 | inv | ErpInvStockMoveLine | -> | md | app.erp.md.dao.entity.ErpMdLocation | destLocation | to-one | module-inventory/model/app-erp-inventory.orm.xml:255 |
| 322 | inv | ErpInvStockMoveLine | -> | md | app.erp.md.dao.entity.ErpMdLocation | sourceLocation | to-one | module-inventory/model/app-erp-inventory.orm.xml:254 |
| 323 | inv | ErpInvStockTakeLine | -> | md | app.erp.md.dao.entity.ErpMdLocation | location | to-one | module-inventory/model/app-erp-inventory.orm.xml:762 |
| 324 | inv | ErpInvBatch | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-inventory/model/app-erp-inventory.orm.xml:907 |
| 325 | inv | ErpInvCostAdjustLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-inventory/model/app-erp-inventory.orm.xml:1286 |
| 326 | inv | ErpInvCostLayer | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-inventory/model/app-erp-inventory.orm.xml:553 |
| 327 | inv | ErpInvOwnershipTransferLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-inventory/model/app-erp-inventory.orm.xml:1078 |
| 328 | inv | ErpInvPickingOrderLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-inventory/model/app-erp-inventory.orm.xml:857 |
| 329 | inv | ErpInvReservationLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-inventory/model/app-erp-inventory.orm.xml:497 |
| 330 | inv | ErpInvSerialNumber | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-inventory/model/app-erp-inventory.orm.xml:955 |
| 331 | inv | ErpInvStockBalance | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-inventory/model/app-erp-inventory.orm.xml:396 |
| 332 | inv | ErpInvStockLedger | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-inventory/model/app-erp-inventory.orm.xml:319 |
| 333 | inv | ErpInvStockMoveLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-inventory/model/app-erp-inventory.orm.xml:250 |
| 334 | inv | ErpInvStockTakeLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-inventory/model/app-erp-inventory.orm.xml:759 |
| 335 | inv | ErpInvTransferOrderLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-inventory/model/app-erp-inventory.orm.xml:664 |
| 336 | inv | ErpInvBatch | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-inventory/model/app-erp-inventory.orm.xml:908 |
| 337 | inv | ErpInvCostLayer | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-inventory/model/app-erp-inventory.orm.xml:554 |
| 338 | inv | ErpInvOwnershipTransferLine | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-inventory/model/app-erp-inventory.orm.xml:1079 |
| 339 | inv | ErpInvPickingOrderLine | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-inventory/model/app-erp-inventory.orm.xml:858 |
| 340 | inv | ErpInvReservationLine | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-inventory/model/app-erp-inventory.orm.xml:498 |
| 341 | inv | ErpInvSerialNumber | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-inventory/model/app-erp-inventory.orm.xml:956 |
| 342 | inv | ErpInvStockBalance | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-inventory/model/app-erp-inventory.orm.xml:397 |
| 343 | inv | ErpInvStockLedger | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-inventory/model/app-erp-inventory.orm.xml:320 |
| 344 | inv | ErpInvStockMoveLine | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-inventory/model/app-erp-inventory.orm.xml:251 |
| 345 | inv | ErpInvStockTakeLine | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-inventory/model/app-erp-inventory.orm.xml:760 |
| 346 | inv | ErpInvTransferOrderLine | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-inventory/model/app-erp-inventory.orm.xml:665 |
| 347 | inv | ErpInvBatch | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-inventory/model/app-erp-inventory.orm.xml:910 |
| 348 | inv | ErpInvCostAdjust | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-inventory/model/app-erp-inventory.orm.xml:1237 |
| 349 | inv | ErpInvCostLayer | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-inventory/model/app-erp-inventory.orm.xml:557 |
| 350 | inv | ErpInvLandedCost | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-inventory/model/app-erp-inventory.orm.xml:1334 |
| 351 | inv | ErpInvOwnershipTransfer | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-inventory/model/app-erp-inventory.orm.xml:1014 |
| 352 | inv | ErpInvPickingOrder | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-inventory/model/app-erp-inventory.orm.xml:807 |
| 353 | inv | ErpInvReservation | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-inventory/model/app-erp-inventory.orm.xml:449 |
| 354 | inv | ErpInvSerialNumber | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-inventory/model/app-erp-inventory.orm.xml:957 |
| 355 | inv | ErpInvStockBalance | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-inventory/model/app-erp-inventory.orm.xml:401 |
| 356 | inv | ErpInvStockLedger | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-inventory/model/app-erp-inventory.orm.xml:324 |
| 357 | inv | ErpInvStockMove | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-inventory/model/app-erp-inventory.orm.xml:181 |
| 358 | inv | ErpInvStockTake | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-inventory/model/app-erp-inventory.orm.xml:710 |
| 359 | inv | ErpInvTransferOrder | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-inventory/model/app-erp-inventory.orm.xml:614 |
| 360 | inv | ErpInvLandedCost | -> | md | app.erp.md.dao.entity.ErpMdPartner | supplier | to-one | module-inventory/model/app-erp-inventory.orm.xml:1336 |
| 361 | inv | ErpInvLandedCostLine | -> | md | app.erp.md.dao.entity.ErpMdPartner | apPartner | to-one | module-inventory/model/app-erp-inventory.orm.xml:1373 |
| 362 | inv | ErpInvOwnershipTransfer | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-inventory/model/app-erp-inventory.orm.xml:1015 |
| 363 | inv | ErpInvReservation | -> | md | app.erp.md.dao.entity.ErpMdPartner | reservedForPartner | to-one | module-inventory/model/app-erp-inventory.orm.xml:450 |
| 364 | inv | ErpInvStockBalance | -> | md | app.erp.md.dao.entity.ErpMdPartner | owner | to-one | module-inventory/model/app-erp-inventory.orm.xml:402 |
| 365 | inv | ErpInvStockLedger | -> | md | app.erp.md.dao.entity.ErpMdPartner | owner | to-one | module-inventory/model/app-erp-inventory.orm.xml:326 |
| 366 | inv | ErpInvPickingOrderLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-inventory/model/app-erp-inventory.orm.xml:859 |
| 367 | inv | ErpInvReservationLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uom | to-one | module-inventory/model/app-erp-inventory.orm.xml:501 |
| 368 | inv | ErpInvStockMoveLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-inventory/model/app-erp-inventory.orm.xml:252 |
| 369 | inv | ErpInvStockTakeLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-inventory/model/app-erp-inventory.orm.xml:761 |
| 370 | inv | ErpInvTransferOrderLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-inventory/model/app-erp-inventory.orm.xml:666 |
| 371 | inv | ErpInvBatch | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-inventory/model/app-erp-inventory.orm.xml:909 |
| 372 | inv | ErpInvCostAdjustLine | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-inventory/model/app-erp-inventory.orm.xml:1287 |
| 373 | inv | ErpInvCostLayer | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-inventory/model/app-erp-inventory.orm.xml:555 |
| 374 | inv | ErpInvOwnershipTransfer | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-inventory/model/app-erp-inventory.orm.xml:1016 |
| 375 | inv | ErpInvPickingOrder | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-inventory/model/app-erp-inventory.orm.xml:806 |
| 376 | inv | ErpInvReservationLine | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-inventory/model/app-erp-inventory.orm.xml:499 |
| 377 | inv | ErpInvSerialNumber | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-inventory/model/app-erp-inventory.orm.xml:958 |
| 378 | inv | ErpInvStockBalance | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-inventory/model/app-erp-inventory.orm.xml:398 |
| 379 | inv | ErpInvStockLedger | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-inventory/model/app-erp-inventory.orm.xml:321 |
| 380 | inv | ErpInvStockMove | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | destWarehouse | to-one | module-inventory/model/app-erp-inventory.orm.xml:180 |
| 381 | inv | ErpInvStockMove | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | sourceWarehouse | to-one | module-inventory/model/app-erp-inventory.orm.xml:179 |
| 382 | inv | ErpInvStockTake | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-inventory/model/app-erp-inventory.orm.xml:709 |
| 383 | inv | ErpInvTransferOrder | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | fromWarehouse | to-one | module-inventory/model/app-erp-inventory.orm.xml:611 |
| 384 | inv | ErpInvTransferOrder | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | inTransitWarehouse | to-one | module-inventory/model/app-erp-inventory.orm.xml:613 |
| 385 | inv | ErpInvTransferOrder | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | toWarehouse | to-one | module-inventory/model/app-erp-inventory.orm.xml:612 |
| 386 | log | ErpLogShipment | -> | md | app.erp.md.dao.entity.ErpMdCurrency | freightCurrency | to-one | module-logistics/model/app-erp-logistics.orm.xml:219 |
| 387 | log | ErpLogShipment | -> | md | app.erp.md.dao.entity.ErpMdEmployee | shipper | to-one | module-logistics/model/app-erp-logistics.orm.xml:215 |
| 388 | log | ErpLogShipmentLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-logistics/model/app-erp-logistics.orm.xml:272 |
| 389 | log | ErpLogCarrier | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-logistics/model/app-erp-logistics.orm.xml:111 |
| 390 | log | ErpLogCarrierConfig | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-logistics/model/app-erp-logistics.orm.xml:155 |
| 391 | log | ErpLogDeliveryWindow | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-logistics/model/app-erp-logistics.orm.xml:384 |
| 392 | log | ErpLogShipment | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-logistics/model/app-erp-logistics.orm.xml:214 |
| 393 | log | ErpLogShipmentLog | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-logistics/model/app-erp-logistics.orm.xml:345 |
| 394 | log | ErpLogCarrier | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-logistics/model/app-erp-logistics.orm.xml:110 |
| 395 | log | ErpLogDeliveryWindow | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-logistics/model/app-erp-logistics.orm.xml:383 |
| 396 | mfg | ErpMfgBatchGenealogy | -> | inv | app.erp.inv.dao.entity.ErpInvBatch | inputLot | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1495 |
| 397 | mfg | ErpMfgBatchGenealogy | -> | inv | app.erp.inv.dao.entity.ErpInvBatch | outputLot | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1497 |
| 398 | mfg | ErpMfgCostRollupLine | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1286 |
| 399 | mfg | ErpMfgMaterialIssue | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1012 |
| 400 | mfg | ErpMfgSubcontractOrder | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1138 |
| 401 | mfg | ErpMfgWorkOrder | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:625 |
| 402 | mfg | ErpMfgJobCardTimeLog | -> | md | app.erp.md.dao.entity.ErpMdEmployee | operator | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1388 |
| 403 | mfg | ErpMfgMaterialIssueLine | -> | md | app.erp.md.dao.entity.ErpMdLocation | location | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1073 |
| 404 | mfg | ErpMfgBatchGenealogy | -> | md | app.erp.md.dao.entity.ErpMdMaterial | inputMaterial | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1492 |
| 405 | mfg | ErpMfgBatchGenealogy | -> | md | app.erp.md.dao.entity.ErpMdMaterial | outputMaterial | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1493 |
| 406 | mfg | ErpMfgBom | -> | md | app.erp.md.dao.entity.ErpMdMaterial | product | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:217 |
| 407 | mfg | ErpMfgBomByproduct | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:354 |
| 408 | mfg | ErpMfgBomLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | alternativeMaterial | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:263 |
| 409 | mfg | ErpMfgBomLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:258 |
| 410 | mfg | ErpMfgCostRollupLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1284 |
| 411 | mfg | ErpMfgCostVariance | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1436 |
| 412 | mfg | ErpMfgForecastLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:955 |
| 413 | mfg | ErpMfgMaterialIssueLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1069 |
| 414 | mfg | ErpMfgMrpDemand | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:879 |
| 415 | mfg | ErpMfgMrpPlanLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:835 |
| 416 | mfg | ErpMfgMrpScenarioParam | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1627 |
| 417 | mfg | ErpMfgProductionVersion | -> | md | app.erp.md.dao.entity.ErpMdMaterial | product | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:747 |
| 418 | mfg | ErpMfgSubcontractOrder | -> | md | app.erp.md.dao.entity.ErpMdMaterial | product | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1143 |
| 419 | mfg | ErpMfgSubcontractOrderLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1204 |
| 420 | mfg | ErpMfgWorkOrder | -> | md | app.erp.md.dao.entity.ErpMdMaterial | product | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:624 |
| 421 | mfg | ErpMfgWorkOrderLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:694 |
| 422 | mfg | ErpMfgWorkcenterCapacity | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:521 |
| 423 | mfg | ErpMfgBomByproduct | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:355 |
| 424 | mfg | ErpMfgBomLine | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:259 |
| 425 | mfg | ErpMfgMaterialIssueLine | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1070 |
| 426 | mfg | ErpMfgWorkOrderLine | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:695 |
| 427 | mfg | ErpMfgCostRollup | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1241 |
| 428 | mfg | ErpMfgCrpLoad | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:557 |
| 429 | mfg | ErpMfgForecast | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:917 |
| 430 | mfg | ErpMfgMaterialIssue | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1011 |
| 431 | mfg | ErpMfgMrpPlan | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:789 |
| 432 | mfg | ErpMfgMrpScenario | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1555 |
| 433 | mfg | ErpMfgSubcontractOrder | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1140 |
| 434 | mfg | ErpMfgWorkOrder | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:628 |
| 435 | mfg | ErpMfgWorkcenterCalendar | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:488 |
| 436 | mfg | ErpMfgWorkcenterCapacity | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:522 |
| 437 | mfg | ErpMfgSubcontractOrder | -> | md | app.erp.md.dao.entity.ErpMdPartner | supplier | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1136 |
| 438 | mfg | ErpMfgBatchGenealogy | -> | md | app.erp.md.dao.entity.ErpMdUoM | inputUoM | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1496 |
| 439 | mfg | ErpMfgBatchGenealogy | -> | md | app.erp.md.dao.entity.ErpMdUoM | outputUoM | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1498 |
| 440 | mfg | ErpMfgBomByproduct | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:356 |
| 441 | mfg | ErpMfgBomLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:260 |
| 442 | mfg | ErpMfgCostRollupLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1285 |
| 443 | mfg | ErpMfgForecastLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:957 |
| 444 | mfg | ErpMfgMaterialIssueLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1071 |
| 445 | mfg | ErpMfgMrpDemand | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:880 |
| 446 | mfg | ErpMfgMrpPlanLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:836 |
| 447 | mfg | ErpMfgSubcontractOrderLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1205 |
| 448 | mfg | ErpMfgWorkOrderLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:696 |
| 449 | mfg | ErpMfgBomLine | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:262 |
| 450 | mfg | ErpMfgForecastLine | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:956 |
| 451 | mfg | ErpMfgMaterialIssue | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:1009 |
| 452 | mfg | ErpMfgWorkOrderLine | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | destWarehouse | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:698 |
| 453 | mfg | ErpMfgWorkOrderLine | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | sourceWarehouse | to-one | module-manufacturing/model/app-erp-manufacturing.orm.xml:697 |
| 454 | mnt | ErpMntEquipment | -> | ast | app.erp.ast.dao.entity.ErpAstAsset | asset | to-one | module-maintenance/model/app-erp-maintenance.orm.xml:148 |
| 455 | mnt | ErpMntCalibration | -> | md | app.erp.md.dao.entity.ErpMdEmployee | calibratedByEmployee | to-one | module-maintenance/model/app-erp-maintenance.orm.xml:627 |
| 456 | mnt | ErpMntMaintenanceTeam | -> | md | app.erp.md.dao.entity.ErpMdEmployee | leader | to-one | module-maintenance/model/app-erp-maintenance.orm.xml:440 |
| 457 | mnt | ErpMntMaintenanceTeamMember | -> | md | app.erp.md.dao.entity.ErpMdEmployee | employee | to-one | module-maintenance/model/app-erp-maintenance.orm.xml:476 |
| 458 | mnt | ErpMntEquipment | -> | md | app.erp.md.dao.entity.ErpMdLocation | location | to-one | module-maintenance/model/app-erp-maintenance.orm.xml:146 |
| 459 | mnt | ErpMntSparePartUsageLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-maintenance/model/app-erp-maintenance.orm.xml:580 |
| 460 | mnt | ErpMntCalibration | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-maintenance/model/app-erp-maintenance.orm.xml:628 |
| 461 | mnt | ErpMntEquipment | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-maintenance/model/app-erp-maintenance.orm.xml:149 |
| 462 | mnt | ErpMntMaintenanceTeam | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-maintenance/model/app-erp-maintenance.orm.xml:442 |
| 463 | mnt | ErpMntSparePartUsage | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-maintenance/model/app-erp-maintenance.orm.xml:522 |
| 464 | mnt | ErpMntVisit | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-maintenance/model/app-erp-maintenance.orm.xml:285 |
| 465 | mnt | ErpMntSparePartUsageLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-maintenance/model/app-erp-maintenance.orm.xml:581 |
| 466 | mnt | ErpMntSparePartUsage | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-maintenance/model/app-erp-maintenance.orm.xml:525 |
| 467 | prj | ErpPrjBilling | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-projects/model/app-erp-projects.orm.xml:648 |
| 468 | prj | ErpPrjBudget | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-projects/model/app-erp-projects.orm.xml:400 |
| 469 | prj | ErpPrjCostCollection | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-projects/model/app-erp-projects.orm.xml:503 |
| 470 | prj | ErpPrjProject | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-projects/model/app-erp-projects.orm.xml:121 |
| 471 | prj | ErpPrjProjectPnl | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-projects/model/app-erp-projects.orm.xml:764 |
| 472 | prj | ErpPrjProjectSettlement | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-projects/model/app-erp-projects.orm.xml:828 |
| 473 | prj | ErpPrjTimesheet | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-projects/model/app-erp-projects.orm.xml:249 |
| 474 | prj | ErpPrjProject | -> | md | app.erp.md.dao.entity.ErpMdEmployee | manager | to-one | module-projects/model/app-erp-projects.orm.xml:120 |
| 475 | prj | ErpPrjProjectUser | -> | md | app.erp.md.dao.entity.ErpMdEmployee | user | to-one | module-projects/model/app-erp-projects.orm.xml:361 |
| 476 | prj | ErpPrjTimesheet | -> | md | app.erp.md.dao.entity.ErpMdEmployee | user | to-one | module-projects/model/app-erp-projects.orm.xml:247 |
| 477 | prj | ErpPrjBilling | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-projects/model/app-erp-projects.orm.xml:651 |
| 478 | prj | ErpPrjBudget | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-projects/model/app-erp-projects.orm.xml:403 |
| 479 | prj | ErpPrjCostCollection | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-projects/model/app-erp-projects.orm.xml:506 |
| 480 | prj | ErpPrjProject | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-projects/model/app-erp-projects.orm.xml:130 |
| 481 | prj | ErpPrjProjectPnl | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-projects/model/app-erp-projects.orm.xml:765 |
| 482 | prj | ErpPrjProjectSettlement | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-projects/model/app-erp-projects.orm.xml:829 |
| 483 | prj | ErpPrjTimesheet | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-projects/model/app-erp-projects.orm.xml:250 |
| 484 | prj | ErpPrjBilling | -> | md | app.erp.md.dao.entity.ErpMdPartner | customer | to-one | module-projects/model/app-erp-projects.orm.xml:645 |
| 485 | prj | ErpPrjProject | -> | md | app.erp.md.dao.entity.ErpMdPartner | customer | to-one | module-projects/model/app-erp-projects.orm.xml:127 |
| 486 | prj | ErpPrjProjectSettlement | -> | md | app.erp.md.dao.entity.ErpMdPartner | customer | to-one | module-projects/model/app-erp-projects.orm.xml:825 |
| 487 | prj | ErpPrjTask | -> | md | app.erp.md.dao.entity.ErpMdPartner | assignee | to-one | module-projects/model/app-erp-projects.orm.xml:189 |
| 488 | prj | ErpPrjActivityType | -> | md | app.erp.md.dao.entity.ErpMdSubject | subject | to-one | module-projects/model/app-erp-projects.orm.xml:329 |
| 489 | prj | ErpPrjBillingLine | -> | md | app.erp.md.dao.entity.ErpMdSubject | subject | to-one | module-projects/model/app-erp-projects.orm.xml:704 |
| 490 | prj | ErpPrjBudgetLine | -> | md | app.erp.md.dao.entity.ErpMdSubject | subject | to-one | module-projects/model/app-erp-projects.orm.xml:452 |
| 491 | prj | ErpPrjCostCollectionLine | -> | md | app.erp.md.dao.entity.ErpMdSubject | subject | to-one | module-projects/model/app-erp-projects.orm.xml:555 |
| 492 | prj | ErpPrjProjectSettlementLine | -> | md | app.erp.md.dao.entity.ErpMdSubject | subject | to-one | module-projects/model/app-erp-projects.orm.xml:877 |
| 493 | prj | ErpPrjProjectType | -> | md | app.erp.md.dao.entity.ErpMdSubject | defaultSubject | to-one | module-projects/model/app-erp-projects.orm.xml:298 |
| 494 | pur | ErpPurPayment | -> | md | app.erp.md.dao.entity.ErpMdBankAccount | bankAccount | to-one | module-purchase/model/app-erp-purchase.orm.xml:961 |
| 495 | pur | ErpPurPayment | -> | md | app.erp.md.dao.entity.ErpMdBankAccount | partnerBankAccount | to-one | module-purchase/model/app-erp-purchase.orm.xml:962 |
| 496 | pur | ErpPurInvoice | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-purchase/model/app-erp-purchase.orm.xml:849 |
| 497 | pur | ErpPurOrder | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-purchase/model/app-erp-purchase.orm.xml:578 |
| 498 | pur | ErpPurPayment | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-purchase/model/app-erp-purchase.orm.xml:958 |
| 499 | pur | ErpPurQuotation | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-purchase/model/app-erp-purchase.orm.xml:315 |
| 500 | pur | ErpPurReceive | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-purchase/model/app-erp-purchase.orm.xml:723 |
| 501 | pur | ErpPurReturn | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-purchase/model/app-erp-purchase.orm.xml:1065 |
| 502 | pur | ErpPurSupplierPriceList | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-purchase/model/app-erp-purchase.orm.xml:412 |
| 503 | pur | ErpPurRequisition | -> | md | app.erp.md.dao.entity.ErpMdEmployee | requester | to-one | module-purchase/model/app-erp-purchase.orm.xml:127 |
| 504 | pur | ErpPurInvoiceLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-purchase/model/app-erp-purchase.orm.xml:900 |
| 505 | pur | ErpPurOrderLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-purchase/model/app-erp-purchase.orm.xml:650 |
| 506 | pur | ErpPurQuotationLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-purchase/model/app-erp-purchase.orm.xml:367 |
| 507 | pur | ErpPurReceiveLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-purchase/model/app-erp-purchase.orm.xml:783 |
| 508 | pur | ErpPurRequisitionLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-purchase/model/app-erp-purchase.orm.xml:176 |
| 509 | pur | ErpPurReturnLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-purchase/model/app-erp-purchase.orm.xml:1123 |
| 510 | pur | ErpPurRfqLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-purchase/model/app-erp-purchase.orm.xml:268 |
| 511 | pur | ErpPurSupplierPriceList | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-purchase/model/app-erp-purchase.orm.xml:410 |
| 512 | pur | ErpPurOrderLine | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-purchase/model/app-erp-purchase.orm.xml:651 |
| 513 | pur | ErpPurReceiveLine | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-purchase/model/app-erp-purchase.orm.xml:784 |
| 514 | pur | ErpPurReturnLine | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-purchase/model/app-erp-purchase.orm.xml:1124 |
| 515 | pur | ErpPurInvoice | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-purchase/model/app-erp-purchase.orm.xml:850 |
| 516 | pur | ErpPurOrder | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-purchase/model/app-erp-purchase.orm.xml:580 |
| 517 | pur | ErpPurPayment | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-purchase/model/app-erp-purchase.orm.xml:960 |
| 518 | pur | ErpPurQuotation | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-purchase/model/app-erp-purchase.orm.xml:316 |
| 519 | pur | ErpPurReceive | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-purchase/model/app-erp-purchase.orm.xml:722 |
| 520 | pur | ErpPurRequisition | -> | md | app.erp.md.dao.entity.ErpMdOrganization | department | to-one | module-purchase/model/app-erp-purchase.orm.xml:128 |
| 521 | pur | ErpPurRequisition | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-purchase/model/app-erp-purchase.orm.xml:126 |
| 522 | pur | ErpPurReturn | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-purchase/model/app-erp-purchase.orm.xml:1064 |
| 523 | pur | ErpPurRfq | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-purchase/model/app-erp-purchase.orm.xml:225 |
| 524 | pur | ErpPurSupplierScorecard | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-purchase/model/app-erp-purchase.orm.xml:462 |
| 525 | pur | ErpPurInvoice | -> | md | app.erp.md.dao.entity.ErpMdPartner | supplier | to-one | module-purchase/model/app-erp-purchase.orm.xml:848 |
| 526 | pur | ErpPurOrder | -> | md | app.erp.md.dao.entity.ErpMdPartner | supplier | to-one | module-purchase/model/app-erp-purchase.orm.xml:576 |
| 527 | pur | ErpPurPayment | -> | md | app.erp.md.dao.entity.ErpMdPartner | supplier | to-one | module-purchase/model/app-erp-purchase.orm.xml:957 |
| 528 | pur | ErpPurQuotation | -> | md | app.erp.md.dao.entity.ErpMdPartner | supplier | to-one | module-purchase/model/app-erp-purchase.orm.xml:314 |
| 529 | pur | ErpPurReceive | -> | md | app.erp.md.dao.entity.ErpMdPartner | supplier | to-one | module-purchase/model/app-erp-purchase.orm.xml:720 |
| 530 | pur | ErpPurRequisitionLine | -> | md | app.erp.md.dao.entity.ErpMdPartner | suggestedSupplier | to-one | module-purchase/model/app-erp-purchase.orm.xml:178 |
| 531 | pur | ErpPurReturn | -> | md | app.erp.md.dao.entity.ErpMdPartner | supplier | to-one | module-purchase/model/app-erp-purchase.orm.xml:1062 |
| 532 | pur | ErpPurSupplierPriceList | -> | md | app.erp.md.dao.entity.ErpMdPartner | supplier | to-one | module-purchase/model/app-erp-purchase.orm.xml:409 |
| 533 | pur | ErpPurSupplierScorecard | -> | md | app.erp.md.dao.entity.ErpMdPartner | supplier | to-one | module-purchase/model/app-erp-purchase.orm.xml:461 |
| 534 | pur | ErpPurOrder | -> | md | app.erp.md.dao.entity.ErpMdSettlementMethod | settlementMethod | to-one | module-purchase/model/app-erp-purchase.orm.xml:579 |
| 535 | pur | ErpPurPayment | -> | md | app.erp.md.dao.entity.ErpMdSettlementMethod | settlementMethod | to-one | module-purchase/model/app-erp-purchase.orm.xml:959 |
| 536 | pur | ErpPurOrderLine | -> | md | app.erp.md.dao.entity.ErpMdTaxRate | taxRateMd | to-one | module-purchase/model/app-erp-purchase.orm.xml:653 |
| 537 | pur | ErpPurInvoiceLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-purchase/model/app-erp-purchase.orm.xml:902 |
| 538 | pur | ErpPurOrderLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-purchase/model/app-erp-purchase.orm.xml:652 |
| 539 | pur | ErpPurQuotationLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-purchase/model/app-erp-purchase.orm.xml:368 |
| 540 | pur | ErpPurReceiveLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-purchase/model/app-erp-purchase.orm.xml:785 |
| 541 | pur | ErpPurRequisitionLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-purchase/model/app-erp-purchase.orm.xml:177 |
| 542 | pur | ErpPurReturnLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-purchase/model/app-erp-purchase.orm.xml:1125 |
| 543 | pur | ErpPurRfqLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-purchase/model/app-erp-purchase.orm.xml:269 |
| 544 | pur | ErpPurSupplierPriceList | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-purchase/model/app-erp-purchase.orm.xml:411 |
| 545 | pur | ErpPurOrder | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-purchase/model/app-erp-purchase.orm.xml:577 |
| 546 | pur | ErpPurOrderLine | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-purchase/model/app-erp-purchase.orm.xml:654 |
| 547 | pur | ErpPurReceive | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-purchase/model/app-erp-purchase.orm.xml:721 |
| 548 | pur | ErpPurReceiveLine | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-purchase/model/app-erp-purchase.orm.xml:787 |
| 549 | pur | ErpPurReturn | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-purchase/model/app-erp-purchase.orm.xml:1063 |
| 550 | pur | ErpPurOrderLine | -> | prj | app.erp.prj.dao.entity.ErpPrjProject | project | to-one | module-purchase/model/app-erp-purchase.orm.xml:655 |
| 551 | pur | ErpPurRequisitionLine | -> | prj | app.erp.prj.dao.entity.ErpPrjProject | project | to-one | module-purchase/model/app-erp-purchase.orm.xml:179 |
| 552 | qa | ErpQaCalibration | -> | md | app.erp.md.dao.entity.ErpMdEmployee | calibratedByEmployee | to-one | module-quality/model/app-erp-quality.orm.xml:633 |
| 553 | qa | ErpQaInspection | -> | md | app.erp.md.dao.entity.ErpMdEmployee | inspector | to-one | module-quality/model/app-erp-quality.orm.xml:209 |
| 554 | qa | ErpQaQualityGoal | -> | md | app.erp.md.dao.entity.ErpMdEmployee | responsiblePerson | to-one | module-quality/model/app-erp-quality.orm.xml:515 |
| 555 | qa | ErpQaSpcSample | -> | md | app.erp.md.dao.entity.ErpMdEmployee | inspector | to-one | module-quality/model/app-erp-quality.orm.xml:846 |
| 556 | qa | ErpQaInspection | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-quality/model/app-erp-quality.orm.xml:205 |
| 557 | qa | ErpQaInspectionTemplate | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-quality/model/app-erp-quality.orm.xml:300 |
| 558 | qa | ErpQaNonConformance | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-quality/model/app-erp-quality.orm.xml:387 |
| 559 | qa | ErpQaRecall | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-quality/model/app-erp-quality.orm.xml:684 |
| 560 | qa | ErpQaSpcChart | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-quality/model/app-erp-quality.orm.xml:789 |
| 561 | qa | ErpQaCalibration | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-quality/model/app-erp-quality.orm.xml:634 |
| 562 | qa | ErpQaInspection | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-quality/model/app-erp-quality.orm.xml:211 |
| 563 | qa | ErpQaReview | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-quality/model/app-erp-quality.orm.xml:559 |
| 564 | qa | ErpQaSpcCapability | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-quality/model/app-erp-quality.orm.xml:899 |
| 565 | qa | ErpQaSpcChart | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-quality/model/app-erp-quality.orm.xml:791 |
| 566 | qa | ErpQaSpcSample | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-quality/model/app-erp-quality.orm.xml:847 |
| 567 | qa | ErpQaInspection | -> | md | app.erp.md.dao.entity.ErpMdPartner | supplier | to-one | module-quality/model/app-erp-quality.orm.xml:207 |
| 568 | qa | ErpQaNonConformance | -> | md | app.erp.md.dao.entity.ErpMdPartner | supplier | to-one | module-quality/model/app-erp-quality.orm.xml:390 |
| 569 | qa | ErpQaRecallTarget | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-quality/model/app-erp-quality.orm.xml:735 |
| 570 | qa | ErpQaInspection | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-quality/model/app-erp-quality.orm.xml:208 |
| 571 | sal | ErpSalReceipt | -> | md | app.erp.md.dao.entity.ErpMdBankAccount | bankAccount | to-one | module-sales/model/app-erp-sales.orm.xml:782 |
| 572 | sal | ErpSalReceipt | -> | md | app.erp.md.dao.entity.ErpMdBankAccount | partnerBankAccount | to-one | module-sales/model/app-erp-sales.orm.xml:783 |
| 573 | sal | ErpSalContract | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-sales/model/app-erp-sales.orm.xml:257 |
| 574 | sal | ErpSalDelivery | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-sales/model/app-erp-sales.orm.xml:507 |
| 575 | sal | ErpSalInvoice | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-sales/model/app-erp-sales.orm.xml:650 |
| 576 | sal | ErpSalOrder | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-sales/model/app-erp-sales.orm.xml:335 |
| 577 | sal | ErpSalPriceList | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-sales/model/app-erp-sales.orm.xml:1026 |
| 578 | sal | ErpSalPricingRule | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-sales/model/app-erp-sales.orm.xml:1153 |
| 579 | sal | ErpSalQuotation | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-sales/model/app-erp-sales.orm.xml:136 |
| 580 | sal | ErpSalReceipt | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-sales/model/app-erp-sales.orm.xml:775 |
| 581 | sal | ErpSalReturn | -> | md | app.erp.md.dao.entity.ErpMdCurrency | currency | to-one | module-sales/model/app-erp-sales.orm.xml:904 |
| 582 | sal | ErpSalDeliveryLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-sales/model/app-erp-sales.orm.xml:573 |
| 583 | sal | ErpSalInvoiceLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-sales/model/app-erp-sales.orm.xml:710 |
| 584 | sal | ErpSalOrderLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-sales/model/app-erp-sales.orm.xml:421 |
| 585 | sal | ErpSalPriceListLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-sales/model/app-erp-sales.orm.xml:1082 |
| 586 | sal | ErpSalPricingRule | -> | md | app.erp.md.dao.entity.ErpMdMaterial | giftMaterial | to-one | module-sales/model/app-erp-sales.orm.xml:1147 |
| 587 | sal | ErpSalPricingRule | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-sales/model/app-erp-sales.orm.xml:1144 |
| 588 | sal | ErpSalQuotationLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-sales/model/app-erp-sales.orm.xml:199 |
| 589 | sal | ErpSalReturnLine | -> | md | app.erp.md.dao.entity.ErpMdMaterial | material | to-one | module-sales/model/app-erp-sales.orm.xml:969 |
| 590 | sal | ErpSalDeliveryLine | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-sales/model/app-erp-sales.orm.xml:576 |
| 591 | sal | ErpSalOrderLine | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-sales/model/app-erp-sales.orm.xml:424 |
| 592 | sal | ErpSalPriceListLine | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-sales/model/app-erp-sales.orm.xml:1085 |
| 593 | sal | ErpSalPricingRule | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | giftSku | to-one | module-sales/model/app-erp-sales.orm.xml:1150 |
| 594 | sal | ErpSalReturnLine | -> | md | app.erp.md.dao.entity.ErpMdMaterialSku | sku | to-one | module-sales/model/app-erp-sales.orm.xml:972 |
| 595 | sal | ErpSalContract | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-sales/model/app-erp-sales.orm.xml:260 |
| 596 | sal | ErpSalDelivery | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-sales/model/app-erp-sales.orm.xml:506 |
| 597 | sal | ErpSalInvoice | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-sales/model/app-erp-sales.orm.xml:653 |
| 598 | sal | ErpSalOrder | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-sales/model/app-erp-sales.orm.xml:344 |
| 599 | sal | ErpSalQuotation | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-sales/model/app-erp-sales.orm.xml:139 |
| 600 | sal | ErpSalReceipt | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-sales/model/app-erp-sales.orm.xml:781 |
| 601 | sal | ErpSalReturn | -> | md | app.erp.md.dao.entity.ErpMdOrganization | org | to-one | module-sales/model/app-erp-sales.orm.xml:903 |
| 602 | sal | ErpSalContract | -> | md | app.erp.md.dao.entity.ErpMdPartner | customer | to-one | module-sales/model/app-erp-sales.orm.xml:254 |
| 603 | sal | ErpSalDelivery | -> | md | app.erp.md.dao.entity.ErpMdPartner | customer | to-one | module-sales/model/app-erp-sales.orm.xml:500 |
| 604 | sal | ErpSalInvoice | -> | md | app.erp.md.dao.entity.ErpMdPartner | customer | to-one | module-sales/model/app-erp-sales.orm.xml:647 |
| 605 | sal | ErpSalOrder | -> | md | app.erp.md.dao.entity.ErpMdPartner | customer | to-one | module-sales/model/app-erp-sales.orm.xml:329 |
| 606 | sal | ErpSalPriceList | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-sales/model/app-erp-sales.orm.xml:1029 |
| 607 | sal | ErpSalPricingRule | -> | md | app.erp.md.dao.entity.ErpMdPartner | partner | to-one | module-sales/model/app-erp-sales.orm.xml:1156 |
| 608 | sal | ErpSalQuotation | -> | md | app.erp.md.dao.entity.ErpMdPartner | customer | to-one | module-sales/model/app-erp-sales.orm.xml:133 |
| 609 | sal | ErpSalReceipt | -> | md | app.erp.md.dao.entity.ErpMdPartner | customer | to-one | module-sales/model/app-erp-sales.orm.xml:772 |
| 610 | sal | ErpSalReturn | -> | md | app.erp.md.dao.entity.ErpMdPartner | customer | to-one | module-sales/model/app-erp-sales.orm.xml:897 |
| 611 | sal | ErpSalOrder | -> | md | app.erp.md.dao.entity.ErpMdSettlementMethod | settlementMethod | to-one | module-sales/model/app-erp-sales.orm.xml:338 |
| 612 | sal | ErpSalReceipt | -> | md | app.erp.md.dao.entity.ErpMdSettlementMethod | settlementMethod | to-one | module-sales/model/app-erp-sales.orm.xml:778 |
| 613 | sal | ErpSalOrderLine | -> | md | app.erp.md.dao.entity.ErpMdTaxRate | taxRateMd | to-one | module-sales/model/app-erp-sales.orm.xml:430 |
| 614 | sal | ErpSalDeliveryLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-sales/model/app-erp-sales.orm.xml:579 |
| 615 | sal | ErpSalInvoiceLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-sales/model/app-erp-sales.orm.xml:714 |
| 616 | sal | ErpSalOrderLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-sales/model/app-erp-sales.orm.xml:427 |
| 617 | sal | ErpSalPriceListLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-sales/model/app-erp-sales.orm.xml:1088 |
| 618 | sal | ErpSalQuotationLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-sales/model/app-erp-sales.orm.xml:202 |
| 619 | sal | ErpSalReturnLine | -> | md | app.erp.md.dao.entity.ErpMdUoM | uoM | to-one | module-sales/model/app-erp-sales.orm.xml:975 |
| 620 | sal | ErpSalDelivery | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-sales/model/app-erp-sales.orm.xml:503 |
| 621 | sal | ErpSalDeliveryLine | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-sales/model/app-erp-sales.orm.xml:583 |
| 622 | sal | ErpSalOrder | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-sales/model/app-erp-sales.orm.xml:332 |
| 623 | sal | ErpSalOrderLine | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-sales/model/app-erp-sales.orm.xml:431 |
| 624 | sal | ErpSalReturn | -> | md | app.erp.md.dao.entity.ErpMdWarehouse | warehouse | to-one | module-sales/model/app-erp-sales.orm.xml:900 |
| 625 | sal | ErpSalOrderLine | -> | prj | app.erp.prj.dao.entity.ErpPrjProject | project | to-one | module-sales/model/app-erp-sales.orm.xml:432 |

**Total cross-module relations (edges):** 625 (to-one + to-many combined)
- to-one: **625**
- to-many: **0** (within-module to-many excluded; these are cross-module to-many, e.g. finance referencing master-data M:N)

**Unique DAG edges (src,tgt) pairs:** 24

## 2. DAG edges (src -> tgt)

| src | -> | tgt | count(relations) |
|-----|----|-----|------------------|
| aps | -> | md | 6 |
| ast | -> | md | 50 |
| b2b | -> | md | 15 |
| crm | -> | md | 38 |
| cs | -> | md | 15 |
| ct | -> | md | 13 |
| drp | -> | inv | 2 |
| drp | -> | md | 24 |
| fin | -> | md | 97 |
| fin | -> | prj | 6 |
| hr | -> | md | 32 |
| hr | -> | prj | 2 |
| inv | -> | md | 85 |
| log | -> | md | 10 |
| mfg | -> | inv | 2 |
| mfg | -> | md | 56 |
| mnt | -> | ast | 1 |
| mnt | -> | md | 12 |
| prj | -> | md | 27 |
| pur | -> | md | 56 |
| pur | -> | prj | 2 |
| qa | -> | md | 19 |
| sal | -> | md | 54 |
| sal | -> | prj | 1 |

## 3. Cycle detection

**✅ NO CYCLES** — DAG verified acyclic (cross-module graph).

## 4. DAG compliance (allowed vs forbidden cross-BIZ edges)

Note: edges to `md` (master-data, root) are ALWAYS allowed (one-way business -> master-data).
Cross-business-domain edges (non-md) checked against module-boundaries.md allow-list:

| src | tgt | count | status |
|-----|-----|-------|--------|
| drp | inv | 2 | ✅ allowed |
| fin | prj | 6 | ✅ allowed |
| hr | prj | 2 | ✅ allowed |
| mfg | inv | 2 | ✅ allowed |
| mnt | ast | 1 | ✅ allowed |
| pur | prj | 2 | ✅ allowed |
| sal | prj | 1 | ✅ allowed |

Allowed cross-biz edges: 7; forbidden: 0

## 5. External entity declarations (`<entity notGenCode="true">`) per module

| declaring module | external entities declared |
|------------------|-----------------------------|
| aps | 1 |
| ast | 6 |
| b2b | 3 |
| crm | 5 |
| cs | 2 |
| ct | 4 |
| drp | 6 |
| fin | 12 |
| hr | 6 |
| inv | 10 |
| log | 5 |
| mfg | 10 |
| mnt | 8 |
| prj | 5 |
| pur | 12 |
| qa | 5 |
| sal | 11 |
| **TOTAL** | **111** |

Detail of every external entity declaration (declaring module, entity, tableName):

| declaring | entity | tableName | file:line |
|-----------|--------|-----------|-----------|
| aps | ErpMdOrganization | erp_md_organization | module-aps/model/app-erp-aps.orm.xml:308 |
| ast | ErpMdCurrency | erp_md_currency | module-assets/model/app-erp-assets.orm.xml:1422 |
| ast | ErpMdEmployee | erp_md_employee | module-assets/model/app-erp-assets.orm.xml:1434 |
| ast | ErpMdLocation | erp_md_location | module-assets/model/app-erp-assets.orm.xml:1445 |
| ast | ErpMdMaterialCategory | erp_md_material_category | module-assets/model/app-erp-assets.orm.xml:1456 |
| ast | ErpMdOrganization | erp_md_organization | module-assets/model/app-erp-assets.orm.xml:1410 |
| ast | ErpMdSubject | erp_md_subject | module-assets/model/app-erp-assets.orm.xml:1466 |
| b2b | ErpMdMaterial | erp_md_material | module-b2b/model/app-erp-b2b.orm.xml:677 |
| b2b | ErpMdOrganization | erp_md_organization | module-b2b/model/app-erp-b2b.orm.xml:670 |
| b2b | ErpMdPartner | erp_md_partner | module-b2b/model/app-erp-b2b.orm.xml:663 |
| crm | ErpMdCurrency | erp_md_currency | module-crm/model/app-erp-crm.orm.xml:1560 |
| crm | ErpMdMaterial | erp_md_material | module-crm/model/app-erp-crm.orm.xml:1569 |
| crm | ErpMdOrganization | erp_md_organization | module-crm/model/app-erp-crm.orm.xml:1526 |
| crm | ErpMdPartner | erp_md_partner | module-crm/model/app-erp-crm.orm.xml:1538 |
| crm | ErpMdPartnerContact | erp_md_partner_contact | module-crm/model/app-erp-crm.orm.xml:1550 |
| cs | ErpMdOrganization | erp_md_organization | module-cs/model/app-erp-cs.orm.xml:841 |
| cs | ErpMdPartner | erp_md_partner | module-cs/model/app-erp-cs.orm.xml:834 |
| ct | ErpMdCurrency | erp_md_currency | module-contract/model/app-erp-contract.orm.xml:742 |
| ct | ErpMdMaterial | erp_md_material | module-contract/model/app-erp-contract.orm.xml:751 |
| ct | ErpMdOrganization | erp_md_organization | module-contract/model/app-erp-contract.orm.xml:735 |
| ct | ErpMdPartner | erp_md_partner | module-contract/model/app-erp-contract.orm.xml:728 |
| drp | ErpInvStockMove | erp_inv_stock_move | module-drp/model/app-erp-drp.orm.xml:541 |
| drp | ErpMdLocation | erp_md_location | module-drp/model/app-erp-drp.orm.xml:549 |
| drp | ErpMdMaterial | erp_md_material | module-drp/model/app-erp-drp.orm.xml:558 |
| drp | ErpMdOrganization | erp_md_organization | module-drp/model/app-erp-drp.orm.xml:533 |
| drp | ErpMdPartner | erp_md_partner | module-drp/model/app-erp-drp.orm.xml:567 |
| drp | ErpMdWarehouse | erp_md_warehouse | module-drp/model/app-erp-drp.orm.xml:576 |
| fin | ErpAstAsset | erp_ast_asset | module-finance/model/app-erp-finance.orm.xml:2355 |
| fin | ErpMdAcctSchema | erp_md_acct_schema | module-finance/model/app-erp-finance.orm.xml:2241 |
| fin | ErpMdCostCenter | erp_md_cost_center | module-finance/model/app-erp-finance.orm.xml:2288 |
| fin | ErpMdCurrency | erp_md_currency | module-finance/model/app-erp-finance.orm.xml:2252 |
| fin | ErpMdEmployee | erp_md_employee | module-finance/model/app-erp-finance.orm.xml:2276 |
| fin | ErpMdMaterial | erp_md_material | module-finance/model/app-erp-finance.orm.xml:2322 |
| fin | ErpMdMaterialCategory | erp_md_material_category | module-finance/model/app-erp-finance.orm.xml:2334 |
| fin | ErpMdOrganization | erp_md_organization | module-finance/model/app-erp-finance.orm.xml:2298 |
| fin | ErpMdPartner | erp_md_partner | module-finance/model/app-erp-finance.orm.xml:2264 |
| fin | ErpMdSubject | erp_md_subject | module-finance/model/app-erp-finance.orm.xml:2229 |
| fin | ErpMdWarehouse | erp_md_warehouse | module-finance/model/app-erp-finance.orm.xml:2310 |
| fin | ErpPrjProject | erp_prj_project | module-finance/model/app-erp-finance.orm.xml:2344 |
| hr | ErpMdBankAccount | erp_md_bank_account | module-hr/model/app-erp-hr.orm.xml:1823 |
| hr | ErpMdCostCenter | erp_md_cost_center | module-hr/model/app-erp-hr.orm.xml:1832 |
| hr | ErpMdCurrency | erp_md_currency | module-hr/model/app-erp-hr.orm.xml:1841 |
| hr | ErpMdOrganization | erp_md_organization | module-hr/model/app-erp-hr.orm.xml:1815 |
| hr | ErpPrjProject | erp_prj_project | module-hr/model/app-erp-hr.orm.xml:1850 |
| hr | ErpPrjTask | erp_prj_task | module-hr/model/app-erp-hr.orm.xml:1859 |
| inv | ErpMdAcctSchema | erp_md_acct_schema | module-inventory/model/app-erp-inventory.orm.xml:1179 |
| inv | ErpMdCurrency | erp_md_currency | module-inventory/model/app-erp-inventory.orm.xml:1155 |
| inv | ErpMdEmployee | erp_md_employee | module-inventory/model/app-erp-inventory.orm.xml:1200 |
| inv | ErpMdLocation | erp_md_location | module-inventory/model/app-erp-inventory.orm.xml:1133 |
| inv | ErpMdMaterial | erp_md_material | module-inventory/model/app-erp-inventory.orm.xml:1099 |
| inv | ErpMdMaterialSku | erp_md_material_sku | module-inventory/model/app-erp-inventory.orm.xml:1110 |
| inv | ErpMdOrganization | erp_md_organization | module-inventory/model/app-erp-inventory.orm.xml:1167 |
| inv | ErpMdPartner | erp_md_partner | module-inventory/model/app-erp-inventory.orm.xml:1190 |
| inv | ErpMdUoM | erp_md_uom | module-inventory/model/app-erp-inventory.orm.xml:1144 |
| inv | ErpMdWarehouse | erp_md_warehouse | module-inventory/model/app-erp-inventory.orm.xml:1121 |
| log | ErpMdCurrency | erp_md_currency | module-logistics/model/app-erp-logistics.orm.xml:422 |
| log | ErpMdEmployee | erp_md_employee | module-logistics/model/app-erp-logistics.orm.xml:408 |
| log | ErpMdMaterial | erp_md_material | module-logistics/model/app-erp-logistics.orm.xml:415 |
| log | ErpMdOrganization | erp_md_organization | module-logistics/model/app-erp-logistics.orm.xml:401 |
| log | ErpMdPartner | erp_md_partner | module-logistics/model/app-erp-logistics.orm.xml:394 |
| mfg | ErpInvBatch | erp_inv_batch | module-manufacturing/model/app-erp-manufacturing.orm.xml:1741 |
| mfg | ErpMdCurrency | erp_md_currency | module-manufacturing/model/app-erp-manufacturing.orm.xml:1718 |
| mfg | ErpMdEmployee | erp_md_employee | module-manufacturing/model/app-erp-manufacturing.orm.xml:1750 |
| mfg | ErpMdLocation | erp_md_location | module-manufacturing/model/app-erp-manufacturing.orm.xml:1695 |
| mfg | ErpMdMaterial | erp_md_material | module-manufacturing/model/app-erp-manufacturing.orm.xml:1650 |
| mfg | ErpMdMaterialSku | erp_md_material_sku | module-manufacturing/model/app-erp-manufacturing.orm.xml:1661 |
| mfg | ErpMdOrganization | erp_md_organization | module-manufacturing/model/app-erp-manufacturing.orm.xml:1706 |
| mfg | ErpMdPartner | erp_md_partner | module-manufacturing/model/app-erp-manufacturing.orm.xml:1730 |
| mfg | ErpMdUoM | erp_md_uom | module-manufacturing/model/app-erp-manufacturing.orm.xml:1672 |
| mfg | ErpMdWarehouse | erp_md_warehouse | module-manufacturing/model/app-erp-manufacturing.orm.xml:1683 |
| mnt | ErpAstAsset | erp_ast_asset | module-maintenance/model/app-erp-maintenance.orm.xml:732 |
| mnt | ErpMdEmployee | erp_md_employee | module-maintenance/model/app-erp-maintenance.orm.xml:688 |
| mnt | ErpMdLocation | erp_md_location | module-maintenance/model/app-erp-maintenance.orm.xml:667 |
| mnt | ErpMdMaterial | erp_md_material | module-maintenance/model/app-erp-maintenance.orm.xml:699 |
| mnt | ErpMdMaterialCategory | erp_md_material_category | module-maintenance/model/app-erp-maintenance.orm.xml:678 |
| mnt | ErpMdOrganization | erp_md_organization | module-maintenance/model/app-erp-maintenance.orm.xml:655 |
| mnt | ErpMdUoM | erp_md_uom | module-maintenance/model/app-erp-maintenance.orm.xml:710 |
| mnt | ErpMdWarehouse | erp_md_warehouse | module-maintenance/model/app-erp-maintenance.orm.xml:721 |
| prj | ErpMdCurrency | erp_md_currency | module-projects/model/app-erp-projects.orm.xml:906 |
| prj | ErpMdEmployee | erp_md_employee | module-projects/model/app-erp-projects.orm.xml:918 |
| prj | ErpMdOrganization | erp_md_organization | module-projects/model/app-erp-projects.orm.xml:894 |
| prj | ErpMdPartner | erp_md_partner | module-projects/model/app-erp-projects.orm.xml:929 |
| prj | ErpMdSubject | erp_md_subject | module-projects/model/app-erp-projects.orm.xml:941 |
| pur | ErpMdBankAccount | erp_md_bank_account | module-purchase/model/app-erp-purchase.orm.xml:1255 |
| pur | ErpMdCurrency | erp_md_currency | module-purchase/model/app-erp-purchase.orm.xml:1209 |
| pur | ErpMdEmployee | erp_md_employee | module-purchase/model/app-erp-purchase.orm.xml:1275 |
| pur | ErpMdMaterial | erp_md_material | module-purchase/model/app-erp-purchase.orm.xml:1152 |
| pur | ErpMdMaterialSku | erp_md_material_sku | module-purchase/model/app-erp-purchase.orm.xml:1163 |
| pur | ErpMdOrganization | erp_md_organization | module-purchase/model/app-erp-purchase.orm.xml:1221 |
| pur | ErpMdPartner | erp_md_partner | module-purchase/model/app-erp-purchase.orm.xml:1174 |
| pur | ErpMdSettlementMethod | erp_md_settlement_method | module-purchase/model/app-erp-purchase.orm.xml:1244 |
| pur | ErpMdTaxRate | erp_md_tax_rate | module-purchase/model/app-erp-purchase.orm.xml:1233 |
| pur | ErpMdUoM | erp_md_uom | module-purchase/model/app-erp-purchase.orm.xml:1198 |
| pur | ErpMdWarehouse | erp_md_warehouse | module-purchase/model/app-erp-purchase.orm.xml:1186 |
| pur | ErpPrjProject | erp_prj_project | module-purchase/model/app-erp-purchase.orm.xml:1266 |
| qa | ErpMdEmployee | erp_md_employee | module-quality/model/app-erp-quality.orm.xml:964 |
| qa | ErpMdMaterial | erp_md_material | module-quality/model/app-erp-quality.orm.xml:930 |
| qa | ErpMdOrganization | erp_md_organization | module-quality/model/app-erp-quality.orm.xml:918 |
| qa | ErpMdPartner | erp_md_partner | module-quality/model/app-erp-quality.orm.xml:941 |
| qa | ErpMdWarehouse | erp_md_warehouse | module-quality/model/app-erp-quality.orm.xml:953 |
| sal | ErpMdBankAccount | erp_md_bank_account | module-sales/model/app-erp-sales.orm.xml:1284 |
| sal | ErpMdCurrency | erp_md_currency | module-sales/model/app-erp-sales.orm.xml:1238 |
| sal | ErpMdMaterial | erp_md_material | module-sales/model/app-erp-sales.orm.xml:1181 |
| sal | ErpMdMaterialSku | erp_md_material_sku | module-sales/model/app-erp-sales.orm.xml:1192 |
| sal | ErpMdOrganization | erp_md_organization | module-sales/model/app-erp-sales.orm.xml:1250 |
| sal | ErpMdPartner | erp_md_partner | module-sales/model/app-erp-sales.orm.xml:1203 |
| sal | ErpMdSettlementMethod | erp_md_settlement_method | module-sales/model/app-erp-sales.orm.xml:1273 |
| sal | ErpMdTaxRate | erp_md_tax_rate | module-sales/model/app-erp-sales.orm.xml:1262 |
| sal | ErpMdUoM | erp_md_uom | module-sales/model/app-erp-sales.orm.xml:1227 |
| sal | ErpMdWarehouse | erp_md_warehouse | module-sales/model/app-erp-sales.orm.xml:1215 |
| sal | ErpPrjProject | erp_prj_project | module-sales/model/app-erp-sales.orm.xml:1295 |

## 6. External entity declaration completeness (Mechanism B)

Rule: every cross-module `<to-one>/<to-many refEntityName=...>` in module S must have a corresponding
`<entity notGenCode="true">` declaration in module S (declaring the external entity to skip codegen).

### 6.1 Per (src, refEntityName) — declaration status

| src | refEntityName | declared notGenCode? |
|-----|---------------|----------------------|
| aps | ErpMdOrganization | ✅ yes |
| ast | ErpMdCurrency | ✅ yes |
| ast | ErpMdEmployee | ✅ yes |
| ast | ErpMdLocation | ✅ yes |
| ast | ErpMdOrganization | ✅ yes |
| ast | ErpMdSubject | ✅ yes |
| b2b | ErpMdMaterial | ✅ yes |
| b2b | ErpMdOrganization | ✅ yes |
| b2b | ErpMdPartner | ✅ yes |
| crm | ErpMdCurrency | ✅ yes |
| crm | ErpMdMaterial | ✅ yes |
| crm | ErpMdOrganization | ✅ yes |
| crm | ErpMdPartner | ✅ yes |
| crm | ErpMdPartnerContact | ✅ yes |
| cs | ErpMdOrganization | ✅ yes |
| cs | ErpMdPartner | ✅ yes |
| ct | ErpMdCurrency | ✅ yes |
| ct | ErpMdMaterial | ✅ yes |
| ct | ErpMdOrganization | ✅ yes |
| ct | ErpMdPartner | ✅ yes |
| drp | ErpInvStockMove | ✅ yes |
| drp | ErpMdLocation | ✅ yes |
| drp | ErpMdMaterial | ✅ yes |
| drp | ErpMdOrganization | ✅ yes |
| drp | ErpMdPartner | ✅ yes |
| drp | ErpMdWarehouse | ✅ yes |
| fin | ErpMdAcctSchema | ✅ yes |
| fin | ErpMdCostCenter | ✅ yes |
| fin | ErpMdCurrency | ✅ yes |
| fin | ErpMdEmployee | ✅ yes |
| fin | ErpMdMaterial | ✅ yes |
| fin | ErpMdMaterialCategory | ✅ yes |
| fin | ErpMdOrganization | ✅ yes |
| fin | ErpMdPartner | ✅ yes |
| fin | ErpMdSubject | ✅ yes |
| fin | ErpMdWarehouse | ✅ yes |
| fin | ErpPrjProject | ✅ yes |
| hr | ErpMdBankAccount | ✅ yes |
| hr | ErpMdCostCenter | ✅ yes |
| hr | ErpMdCurrency | ✅ yes |
| hr | ErpMdOrganization | ✅ yes |
| hr | ErpPrjProject | ✅ yes |
| hr | ErpPrjTask | ✅ yes |
| inv | ErpMdAcctSchema | ✅ yes |
| inv | ErpMdCurrency | ✅ yes |
| inv | ErpMdEmployee | ✅ yes |
| inv | ErpMdLocation | ✅ yes |
| inv | ErpMdMaterial | ✅ yes |
| inv | ErpMdMaterialSku | ✅ yes |
| inv | ErpMdOrganization | ✅ yes |
| inv | ErpMdPartner | ✅ yes |
| inv | ErpMdUoM | ✅ yes |
| inv | ErpMdWarehouse | ✅ yes |
| log | ErpMdCurrency | ✅ yes |
| log | ErpMdEmployee | ✅ yes |
| log | ErpMdMaterial | ✅ yes |
| log | ErpMdOrganization | ✅ yes |
| log | ErpMdPartner | ✅ yes |
| mfg | ErpInvBatch | ✅ yes |
| mfg | ErpMdCurrency | ✅ yes |
| mfg | ErpMdEmployee | ✅ yes |
| mfg | ErpMdLocation | ✅ yes |
| mfg | ErpMdMaterial | ✅ yes |
| mfg | ErpMdMaterialSku | ✅ yes |
| mfg | ErpMdOrganization | ✅ yes |
| mfg | ErpMdPartner | ✅ yes |
| mfg | ErpMdUoM | ✅ yes |
| mfg | ErpMdWarehouse | ✅ yes |
| mnt | ErpAstAsset | ✅ yes |
| mnt | ErpMdEmployee | ✅ yes |
| mnt | ErpMdLocation | ✅ yes |
| mnt | ErpMdMaterial | ✅ yes |
| mnt | ErpMdOrganization | ✅ yes |
| mnt | ErpMdUoM | ✅ yes |
| mnt | ErpMdWarehouse | ✅ yes |
| prj | ErpMdCurrency | ✅ yes |
| prj | ErpMdEmployee | ✅ yes |
| prj | ErpMdOrganization | ✅ yes |
| prj | ErpMdPartner | ✅ yes |
| prj | ErpMdSubject | ✅ yes |
| pur | ErpMdBankAccount | ✅ yes |
| pur | ErpMdCurrency | ✅ yes |
| pur | ErpMdEmployee | ✅ yes |
| pur | ErpMdMaterial | ✅ yes |
| pur | ErpMdMaterialSku | ✅ yes |
| pur | ErpMdOrganization | ✅ yes |
| pur | ErpMdPartner | ✅ yes |
| pur | ErpMdSettlementMethod | ✅ yes |
| pur | ErpMdTaxRate | ✅ yes |
| pur | ErpMdUoM | ✅ yes |
| pur | ErpMdWarehouse | ✅ yes |
| pur | ErpPrjProject | ✅ yes |
| qa | ErpMdEmployee | ✅ yes |
| qa | ErpMdMaterial | ✅ yes |
| qa | ErpMdOrganization | ✅ yes |
| qa | ErpMdPartner | ✅ yes |
| qa | ErpMdWarehouse | ✅ yes |
| sal | ErpMdBankAccount | ✅ yes |
| sal | ErpMdCurrency | ✅ yes |
| sal | ErpMdMaterial | ✅ yes |
| sal | ErpMdMaterialSku | ✅ yes |
| sal | ErpMdOrganization | ✅ yes |
| sal | ErpMdPartner | ✅ yes |
| sal | ErpMdSettlementMethod | ✅ yes |
| sal | ErpMdTaxRate | ✅ yes |
| sal | ErpMdUoM | ✅ yes |
| sal | ErpMdWarehouse | ✅ yes |
| sal | ErpPrjProject | ✅ yes |

**Coverage: 108 / 108 referenced external entities have `<entity notGenCode>` declaration in source module.**

## 7. Per-domain cross-module to-one counts

Matches owner doc §5.6.2 / scope matrix §1.1 'cross-domain to-one' metric:

| src domain | to-one count | to-many count | external decl count |
|------------|--------------|---------------|---------------------|
| aps | 6 | 0 | 1 |
| ast | 50 | 0 | 6 |
| b2b | 15 | 0 | 3 |
| crm | 38 | 0 | 5 |
| cs | 15 | 0 | 2 |
| ct | 13 | 0 | 4 |
| drp | 26 | 0 | 6 |
| fin | 103 | 0 | 12 |
| hr | 34 | 0 | 6 |
| inv | 85 | 0 | 10 |
| log | 10 | 0 | 5 |
| md | 0 | 0 | 0 |
| mfg | 58 | 0 | 10 |
| mnt | 13 | 0 | 8 |
| notify | 0 | 0 | 0 |
| prj | 27 | 0 | 5 |
| pur | 58 | 0 | 12 |
| qa | 19 | 0 | 5 |
| sal | 55 | 0 | 11 |
| **TOTAL** | **625** | **0** | **111** |

**Owner-doc claimed cross-module to-one ≈ 369, external declarations ≈ 68.**
**Machine-verified:** cross-module to-one = 625, cross-module to-many = 0, external declarations = 111.
