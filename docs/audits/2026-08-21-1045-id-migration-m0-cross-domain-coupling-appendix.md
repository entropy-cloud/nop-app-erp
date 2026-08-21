# 跨域 id 类型耦合清单（机器生成，勿手改）

> 本节即主件与计划所引「附录 A」（service/dao 层跨域耦合点 + id-as-Long 证据行）；附录 B-F 为下方人工整理段。

命令: `node tools/scan-cross-domain-id-coupling.mjs --dao`

## module-aps （引用外域的耦合文件 4 个）

### module-aps/erp-aps-service/src/main/java/app/erp/aps/service/atpctp/ErpApsAtpCtpServiceImpl.java
- 外域: inventory, manufacturing  |  本文件 id-as-Long 证据行: 14
- 耦合点 L12: ErpInvReservation (inventory)
- 耦合点 L13: ErpInvReservationLine (inventory)
- 耦合点 L14: ErpInvStockBalance (inventory)
- 耦合点 L15: ErpMfgBom (manufacturing)
- 耦合点 L16: ErpMfgBomOperation (manufacturing)
- 耦合点 L273: ErpInvStockBalance.class (daoFor) (inventory)
- 耦合点 L277: ErpInvReservation.class (daoFor) (inventory)
- 耦合点 L281: ErpInvReservationLine.class (daoFor) (inventory)
- 耦合点 L285: ErpMfgBom.class (daoFor) (manufacturing)
- 耦合点 L289: ErpMfgBomOperation.class (daoFor) (manufacturing)
  - id 行 L53: `public LocalDateTime earliestCompletionDate(Long materialId, BigDecimal qty) {`
  - id 行 L62: `public CtpResult checkFeasibility(Long materialId, BigDecimal qty, LocalDateTime desiredDate) {`
  - id 行 L73: `public List<ScheduledOperationView> simulateSchedule(Long materialId, BigDecimal qty, LocalDateTime startDate) {`
  - id 行 L98: `v.setWorkcenterId(shadow.getMachineId());`
  - id 行 L110: `protected boolean atpAvailable(Long materialId, BigDecimal qty) {`
  - id 行 L116: `protected BigDecimal sumOnHand(Long materialId) {`
  - id 行 L128: `protected BigDecimal sumReserved(Long materialId) {`
  - id 行 L141: `protected boolean isReservationActive(IEntityDao<ErpInvReservation> resDao, Long reservationId) {`
  - id 行 L151: `protected CtpResult simulateCtp(Long materialId, BigDecimal qty, LocalDateTime startDate,`
  - id 行 L204: `protected List<ErpApsOperationOrder> buildShadowOps(Long materialId, BigDecimal qty) {`
  - id 行 L212: `for (ErpMfgBomOperation bo : loadBomOperations(bom.getId())) {`
  - id 行 L218: `shadow.setMachineId(bo.getWorkcenterId());`
  - id 行 L234: `protected ErpMfgBom findDefaultBom(Long materialId) {`
  - id 行 L244: `protected List<ErpMfgBomOperation> loadBomOperations(Long bomId) {`

### module-aps/erp-aps-service/src/main/java/app/erp/aps/service/loadsource/ApsLoadSourceProvider.java
- 外域: manufacturing  |  本文件 id-as-Long 证据行: 3
- 耦合点 L5: ApsLoadSlot (manufacturing)
- 耦合点 L6: IErpApsLoadSourceProvider (manufacturing)
  - id 行 L80: `slot.setOperationOrderId(op.getId());`
  - id 行 L81: `slot.setWorkOrderId(op.getWorkOrderId());`
  - id 行 L83: `slot.setWorkcenterId(op.getMachineId());`

### module-aps/erp-aps-service/src/main/java/app/erp/aps/service/processor/ErpApsAutoDispatchProcessor.java
- 外域: inventory, manufacturing, notify  |  本文件 id-as-Long 证据行: 18
- 耦合点 L10: ErpInvStockBalance (inventory)
- 耦合点 L11: ErpMfgBom (manufacturing)
- 耦合点 L12: ErpMfgBomLine (manufacturing)
- 耦合点 L13: ErpMfgWorkOrder (manufacturing)
- 耦合点 L14: ErpMfgWorkcenter (manufacturing)
- 耦合点 L15: IErpSysNotificationBiz (notify)
- 耦合点 L227: ErpMfgWorkcenter.class (daoFor) (manufacturing)
- 耦合点 L261: ErpMfgWorkOrder.class (daoFor) (manufacturing)
- 耦合点 L297: ErpMfgBom.class (daoFor) (manufacturing)
- 耦合点 L313: ErpMfgBomLine.class (daoFor) (manufacturing)
- 耦合点 L320: ErpInvStockBalance.class (daoFor) (inventory)
  - id 行 L234: `protected int countRunningOps(Long workcenterId) {`
  - id 行 L269: `List<ErpMfgBomLine> lines = loadBomLines(bom.getId());`
  - id 行 L310: `protected List<ErpMfgBomLine> loadBomLines(Long bomId) {`
  - id 行 L316: `protected BigDecimal sumAvailable(Long materialId) {`
  - id 行 L342: `op.getId());`
  - id 行 L358: `log.setOperationOrderId(op.getId());`
  - id 行 L359: `log.setWorkcenterId(op.getMachineId());`
  - id 行 L386: `log.setOperationOrderId(op.getId());`
  - id 行 L387: `log.setWorkcenterId(op.getMachineId());`
  - id 行 L406: `ctx.put("operationOrderId", op.getId());`
  - id 行 L414: `op.getId(), e.getMessage());`
  - id 行 L421: `public ErpApsOperationOrder dispatchManually(Long operationOrderId, String note, IServiceContext context) {`
  - id 行 L433: `public ErpApsOperationOrder hold(Long operationOrderId, IServiceContext context) {`
  - id 行 L449: `public ErpApsOperationOrder unhold(Long operationOrderId, IServiceContext context) {`
  - id 行 L468: `log.setOperationOrderId(op.getId());`
  - id 行 L469: `log.setWorkcenterId(op.getMachineId());`
  - id 行 L491: `protected ErpApsOperationOrder requireOp(Long operationOrderId) {`
  - id 行 L533: `m.put("ruleId", rule != null ? rule.getId() : null);`

### module-aps/erp-aps-service/src/main/java/app/erp/aps/service/processor/ErpApsWorkOrderToOperationProcessor.java
- 外域: manufacturing, notify  |  本文件 id-as-Long 证据行: 16
- 耦合点 L8: ErpMfgRoutingOperation (manufacturing)
- 耦合点 L9: ErpMfgWorkOrder (manufacturing)
- 耦合点 L10: ErpMfgWorkcenter (manufacturing)
- 耦合点 L11: IErpSysNotificationBiz (notify)
- 耦合点 L123: ErpMfgWorkOrder.class (daoFor) (manufacturing)
- 耦合点 L146: ErpMfgRoutingOperation.class (daoFor) (manufacturing)
- 耦合点 L153: ErpMfgWorkcenter.class (daoFor) (manufacturing)
- 耦合点 L161: ErpMfgWorkOrder.class (daoFor) (manufacturing)
  - id 行 L70: `public WorkOrderOperationCreationResult createOperationOrdersFromWorkOrder(Long workOrderId,`
  - id 行 L75: `result.setWorkOrderId(wo.getId());`
  - id 行 L79: `if (hasExistingOperationOrders(wo.getId())) {`
  - id 行 L89: `wo.getId(), wo.getCode(), wo.getRoutingId());`
  - id 行 L99: `wo.getId(), wo.getCode(), rop.getLineNo(), rop.getWorkcenterId());`
  - id 行 L114: `WorkOrderOperationCreationResult r = createOperationOrdersFromWorkOrder(wo.getId(), context);`
  - id 行 L122: `protected ErpMfgWorkOrder requireWorkOrder(Long workOrderId) {`
  - id 行 L131: `protected boolean hasExistingOperationOrders(Long workOrderId) {`
  - id 行 L152: `protected boolean workcenterExists(Long workcenterId) {`
  - id 行 L169: `op.setWorkOrderId(wo.getId());`
  - id 行 L173: `op.setMachineId(rop.getWorkcenterId());`
  - id 行 L179: `op.setOrgId(wo.getOrgId());`
  - id 行 L189: `String prefix = wo.getCode() != null ? wo.getCode() : ("WO" + wo.getId());`
  - id 行 L195: `protected void notify(String eventType, ErpMfgWorkOrder wo, Long workcenterId, Integer sequence,`
  - id 行 L199: `ctx.put("workOrderId", wo.getId());`
  - id 行 L210: `eventType, wo.getId(), e.getMessage());`

## module-assets （引用外域的耦合文件 12 个）

### module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/AssetInventoryPostingDispatcher.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 6
- 耦合点 L9: ErpMdSubject (master-data)
- 耦合点 L106: ErpMdSubject.class (daoFor) (master-data)
  - id 行 L70: `event.setOrgId(inventory.getOrgId());`
  - id 行 L71: `event.setAcctSchemaId(resolveAcctSchemaId(inventory.getOrgId()));`
  - id 行 L72: `event.setCurrencyId(inventory.getCurrencyId());`
  - id 行 L91: `private ErpAstAssetCategory resolveRangeCategory(Long rangeCategoryId) {`
  - id 行 L98: `private Long resolveAcctSchemaId(Long orgId) {`
  - id 行 L102: `private String resolveSubjectCode(Long subjectId, String defaultCode) {`

### module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/AssetMergePostingDispatcher.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 9
- 耦合点 L11: ErpMdSubject (master-data)
- 耦合点 L121: ErpMdSubject.class (daoFor) (master-data)
  - id 行 L49: `Long voucherId = executor.postEvent(event);`
  - id 行 L66: `Long orgId = merge.getOrgId() != null ? merge.getOrgId()`
  - id 行 L68: `event.setOrgId(orgId);`
  - id 行 L69: `event.setAcctSchemaId(resolveAcctSchemaId(orgId));`
  - id 行 L70: `Long currencyId = merge.getCurrencyId() != null ? merge.getCurrencyId()`
  - id 行 L72: `event.setCurrencyId(currencyId);`
  - id 行 L106: `private ErpAstAssetCategory loadCategory(Long categoryId) {`
  - id 行 L113: `private Long resolveAcctSchemaId(Long orgId) {`
  - id 行 L117: `private String resolveSubjectCode(Long subjectId, String defaultCode) {`

### module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/AssetPostingExecutor.java
- 外域: finance  |  本文件 id-as-Long 证据行: 0
- 耦合点 L3: IErpFinVoucherBiz (finance)

### module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/AssetSplitPostingDispatcher.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 8
- 耦合点 L11: ErpMdSubject (master-data)
- 耦合点 L118: ErpMdSubject.class (daoFor) (master-data)
  - id 行 L51: `Long voucherId = executor.postEvent(event);`
  - id 行 L68: `event.setOrgId(split.getOrgId() != null ? split.getOrgId() : source.getOrgId());`
  - id 行 L69: `event.setAcctSchemaId(resolveAcctSchemaId(event.getOrgId()));`
  - id 行 L70: `event.setCurrencyId(split.getCurrencyId() != null ? split.getCurrencyId() : source.getCurrencyId());`
  - id 行 L81: `Long categoryId = line.getCategoryId() != null ? line.getCategoryId() : source.getCategoryId();`
  - id 行 L103: `private ErpAstAssetCategory loadCategory(Long categoryId) {`
  - id 行 L110: `private Long resolveAcctSchemaId(Long orgId) {`
  - id 行 L114: `private String resolveSubjectCode(Long subjectId, String defaultCode) {`

### module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/CapitalizationPostingDispatcher.java
- 外域: master-data, notify  |  本文件 id-as-Long 证据行: 7
- 耦合点 L9: ErpMdSubject (master-data)
- 耦合点 L10: IErpSysNotificationBiz (notify)
- 耦合点 L148: ErpMdSubject.class (daoFor) (master-data)
  - id 行 L58: `Long voucherId = executor.postEvent(event);`
  - id 行 L112: `event.setOrgId(cap.getOrgId());`
  - id 行 L113: `event.setAcctSchemaId(resolveAcctSchemaId(cap.getOrgId()));`
  - id 行 L114: `event.setCurrencyId(cap.getCurrencyId());`
  - id 行 L134: `private ErpAstAssetCategory loadCategory(Long categoryId) {`
  - id 行 L140: `private Long resolveAcctSchemaId(Long orgId) {`
  - id 行 L144: `private String resolveSubjectCode(Long subjectId, String defaultCode) {`

### module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/DepreciationPostingDispatcher.java
- 外域: master-data, notify  |  本文件 id-as-Long 证据行: 12
- 耦合点 L10: ErpMdSubject (master-data)
- 耦合点 L11: IErpSysNotificationBiz (notify)
- 耦合点 L228: ErpMdSubject.class (daoFor) (master-data)
  - id 行 L81: `ctx.put("assetId", asset.getId());`
  - id 行 L128: `ctx.put("assetId", asset.getId());`
  - id 行 L148: `event.setOrgId(asset.getOrgId());`
  - id 行 L149: `event.setAcctSchemaId(resolveAcctSchemaId(event.getOrgId()));`
  - id 行 L150: `event.setCurrencyId(asset.getCurrencyId());`
  - id 行 L156: `billData.put(ErpAstConstants.BILL_DATA_ASSET_ID, asset.getId());`
  - id 行 L194: `event.setOrgId(schedule.getOrgId() != null ? schedule.getOrgId() : asset.getOrgId());`
  - id 行 L195: `event.setAcctSchemaId(resolveAcctSchemaId(event.getOrgId()));`
  - id 行 L196: `event.setCurrencyId(asset.getCurrencyId());`
  - id 行 L204: `billData.put(ErpAstConstants.BILL_DATA_ASSET_ID, asset.getId());`
  - id 行 L220: `private Long resolveAcctSchemaId(Long orgId) {`
  - id 行 L224: `private String resolveSubjectCode(Long subjectId, String defaultCode) {`

### module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/DisposalPostingDispatcher.java
- 外域: master-data, notify  |  本文件 id-as-Long 证据行: 6
- 耦合点 L10: ErpMdSubject (master-data)
- 耦合点 L11: IErpSysNotificationBiz (notify)
- 耦合点 L141: ErpMdSubject.class (daoFor) (master-data)
  - id 行 L102: `event.setOrgId(disposal.getOrgId() != null ? disposal.getOrgId() : asset.getOrgId());`
  - id 行 L103: `event.setAcctSchemaId(resolveAcctSchemaId(event.getOrgId()));`
  - id 行 L104: `event.setCurrencyId(disposal.getCurrencyId() != null ? disposal.getCurrencyId() : asset.getCurrencyId());`
  - id 行 L122: `billData.put(ErpAstConstants.BILL_DATA_ASSET_ID, asset.getId());`
  - id 行 L133: `private Long resolveAcctSchemaId(Long orgId) {`
  - id 行 L137: `private String resolveSubjectCode(Long subjectId, String defaultCode) {`

### module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/MaintenanceCapitalizationPostingDispatcher.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 5
- 耦合点 L10: ErpMdSubject (master-data)
- 耦合点 L102: ErpMdSubject.class (daoFor) (master-data)
  - id 行 L71: `event.setOrgId(maintenance.getOrgId());`
  - id 行 L72: `event.setAcctSchemaId(resolveAcctSchemaId(maintenance.getOrgId()));`
  - id 行 L73: `event.setCurrencyId(maintenance.getCurrencyId());`
  - id 行 L94: `private Long resolveAcctSchemaId(Long orgId) {`
  - id 行 L98: `private String resolveSubjectCode(Long subjectId, String defaultCode) {`

### module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/MaintenanceExpensePostingDispatcher.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 5
- 耦合点 L10: ErpMdSubject (master-data)
- 耦合点 L102: ErpMdSubject.class (daoFor) (master-data)
  - id 行 L71: `event.setOrgId(maintenance.getOrgId());`
  - id 行 L72: `event.setAcctSchemaId(resolveAcctSchemaId(maintenance.getOrgId()));`
  - id 行 L73: `event.setCurrencyId(maintenance.getCurrencyId());`
  - id 行 L94: `private Long resolveAcctSchemaId(Long orgId) {`
  - id 行 L98: `private String resolveSubjectCode(Long subjectId, String defaultCode) {`

### module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/ValueAdjustmentPostingDispatcher.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 6
- 耦合点 L10: ErpMdSubject (master-data)
- 耦合点 L102: ErpMdSubject.class (daoFor) (master-data)
  - id 行 L71: `event.setOrgId(adjustment.getOrgId() != null ? adjustment.getOrgId() : asset.getOrgId());`
  - id 行 L72: `event.setAcctSchemaId(resolveAcctSchemaId(event.getOrgId()));`
  - id 行 L73: `event.setCurrencyId(adjustment.getCurrencyId() != null ? adjustment.getCurrencyId() : asset.getCurrencyId());`
  - id 行 L82: `billData.put(ErpAstConstants.BILL_DATA_ASSET_ID, asset.getId());`
  - id 行 L94: `private Long resolveAcctSchemaId(Long orgId) {`
  - id 行 L98: `private String resolveSubjectCode(Long subjectId, String defaultCode) {`

### module-assets/erp-ast-service/src/main/java/app/erp/ast/service/processor/ErpAstDepreciationScheduleProcessor.java
- 外域: finance  |  本文件 id-as-Long 证据行: 4
- 耦合点 L10: ErpFinAccountingPeriod (finance)
- 耦合点 L101: ErpFinAccountingPeriod.class (daoFor) (finance)
  - id 行 L56: `protected String findLastExecutedPeriod(Long assetId) {`
  - id 行 L91: `protected ErpAstAsset requireAsset(Long assetId) {`
  - id 行 L117: `protected ErpAstDepreciationSchedule findSchedule(Long assetId, String period) {`
  - id 行 L126: `protected int countExecuted(Long assetId) {`

### module-assets/erp-ast-service/src/main/java/app/erp/ast/service/processor/ErpAstDisposalProcessor.java
- 外域: maintenance  |  本文件 id-as-Long 证据行: 8
- 耦合点 L14: IErpMntEquipmentBiz (maintenance)
  - id 行 L118: `cancelPendingSchedules(asset.getId());`
  - id 行 L134: `Long voucherId = postingDispatcher.tryPost(disposal, asset, category);`
  - id 行 L261: `if (mntEquipmentBiz == null || asset == null || asset.getId() == null) {`
  - id 行 L264: `mntEquipmentBiz.changeStatusForAssetDisposal(asset.getId(), disposal.getCode(), context);`
  - id 行 L297: `String lastExecuted = depreciationScheduleFacade.findLastExecutedPeriod(asset.getId());`
  - id 行 L308: `catchUpDepreciationProcessor.catchUpDepreciation(asset.getId(), disposalPeriod, missed, context);`
  - id 行 L311: `protected void cancelPendingSchedules(Long assetId) {`
  - id 行 L322: `protected void restoreCancelledSchedules(Long assetId) {`

## module-b2b （引用外域的耦合文件 4 个）

### module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/job/ErpB2bOnboardingMonitorJob.java
- 外域: notify  |  本文件 id-as-Long 证据行: 2
- 耦合点 L10: IErpSysNotificationBiz (notify)
  - id 行 L204: `ids.add(format.getId());`
  - id 行 L240: `map.put("profileId", profile.getId());`

### module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bAsnCreateReceiveFromAsnProcessor.java
- 外域: master-data, purchase  |  本文件 id-as-Long 证据行: 17
- 耦合点 L9: ErpMdMaterial (master-data)
- 耦合点 L10: ErpPurOrder (purchase)
- 耦合点 L11: ErpPurOrderLine (purchase)
- 耦合点 L12: ErpPurReceive (purchase)
- 耦合点 L13: ErpPurReceiveLine (purchase)
- 耦合点 L68: ErpPurReceive.class (daoFor) (purchase)
- 耦合点 L79: ErpPurReceive.class (daoFor) (purchase)
- 耦合点 L119: ErpPurReceiveLine.class (daoFor) (purchase)
- 耦合点 L120: ErpMdMaterial.class (daoFor) (master-data)
- 耦合点 L207: ErpPurOrder.class (daoFor) (purchase)
- 耦合点 L221: ErpPurOrderLine.class (daoFor) (purchase)
  - id 行 L49: `public ErpB2bAsn createReceiveFromAsn(Long asnId, IServiceContext context) {`
  - id 行 L70: `receive.setOrderId(po.getId());`
  - id 行 L71: `receive.setSupplierId(po.getSupplierId());`
  - id 行 L72: `receive.setWarehouseId(po.getWarehouseId());`
  - id 行 L73: `receive.setCurrencyId(po.getCurrencyId());`
  - id 行 L112: `List<ErpB2bAsnLine> asnLines = findAsnLines(asn.getId());`
  - id 行 L118: `List<ErpPurOrderLine> poLines = findPoLines(po.getId());`
  - id 行 L123: `Long materialId = asnLine.getMaterialId();`
  - id 行 L139: `receiveLine.setReceiveId(receive.getId());`
  - id 行 L141: `receiveLine.setMaterialId(materialId);`
  - id 行 L142: `receiveLine.setUoMId(material.getUoMId());`
  - id 行 L151: `receiveLine.setOrderLineId(matchedPoLine.getId());`
  - id 行 L160: `receiveLine.setWarehouseId(receive.getWarehouseId());`
  - id 行 L190: `protected ErpB2bAsn requireAsn(Long asnId) {`
  - id 行 L211: `protected List<ErpB2bAsnLine> findAsnLines(Long asnId) {`
  - id 行 L218: `protected List<ErpPurOrderLine> findPoLines(Long orderId) {`
  - id 行 L224: `protected ErpPurOrderLine findMatchingPoLine(List<ErpPurOrderLine> poLines, Long materialId) {`

### module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bAsnMatchPurchaseOrderProcessor.java
- 外域: purchase  |  本文件 id-as-Long 证据行: 8
- 耦合点 L9: ErpPurOrder (purchase)
- 耦合点 L10: ErpPurOrderLine (purchase)
- 耦合点 L138: ErpPurOrder.class (daoFor) (purchase)
- 耦合点 L157: ErpPurOrderLine.class (daoFor) (purchase)
  - id 行 L46: `public ErpB2bAsn matchPurchaseOrder(Long asnId, IServiceContext context) {`
  - id 行 L68: `List<ErpB2bAsnLine> asnLines = findAsnLines(asn.getId());`
  - id 行 L69: `List<ErpPurOrderLine> poLines = findPoLines(po.getId());`
  - id 行 L121: `protected ErpB2bAsn requireAsn(Long asnId) {`
  - id 行 L147: `protected List<ErpB2bAsnLine> findAsnLines(Long asnId) {`
  - id 行 L154: `protected List<ErpPurOrderLine> findPoLines(Long orderId) {`
  - id 行 L160: `protected ErpPurOrderLine findMatchingPoLine(List<ErpPurOrderLine> poLines, Long materialId) {`
  - id 行 L172: `protected void markEdiDocError(Long ediDocId, String error, IServiceContext context) {`

### module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/spi/ubl/UblInvoiceEdiProvider.java
- 外域: sales  |  本文件 id-as-Long 证据行: 0
- 耦合点 L53: ErpSalInvoice (sales)

## module-contract （引用外域的耦合文件 12 个）

### module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtApprovalRecordBizModel.java
- 外域: notify  |  本文件 id-as-Long 证据行: 16
- 耦合点 L13: IErpSysNotificationBiz (notify)
  - id 行 L56: `public ErpCtApprovalRecord approve(@Name("recordId") Long recordId,`
  - id 行 L75: `public ErpCtApprovalRecord reject(@Name("recordId") Long recordId,`
  - id 行 L99: `public int resubmit(@Name("contractId") Long contractId, IServiceContext context) {`
  - id 行 L144: `protected ErpCtApprovalRecord requireChainRecord(Long recordId, IServiceContext context) {`
  - id 行 L162: `.param(ErpCtErrors.ARG_APPROVAL_RECORD_ID, record.getId())`
  - id 行 L177: `.param(ErpCtErrors.ARG_APPROVAL_RECORD_ID, record.getId())`
  - id 行 L184: `protected void guardNotLocked(Long contractId, Integer approvalOrder, IServiceContext context) {`
  - id 行 L196: `protected void activateNext(Long contractId, Integer currentOrder, IServiceContext context) {`
  - id 行 L223: `record.setContractId(contract.getId());`
  - id 行 L224: `record.setOrgId(contract.getOrgId());`
  - id 行 L225: `record.setApprovalMatrixId(node.getId());`
  - id 行 L227: `record.setApproverId(engine.resolveApproverId(node.getApproverRole(), context));`
  - id 行 L231: `protected ErpCtContract findContract(Long contractId, IServiceContext context) {`
  - id 行 L249: `map.put("contractId", contract.getId());`
  - id 行 L261: `map.put("contractId", contract.getId());`
  - id 行 L272: `map.put("contractId", contract.getId());`

### module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtContractBizModel.java
- 外域: notify  |  本文件 id-as-Long 证据行: 47
- 耦合点 L40: IErpSysNotificationBiz (notify)
  - id 行 L118: `public ErpCtContract submit(@Name("contractId") Long contractId, IServiceContext context) {`
  - id 行 L162: `public ErpCtContract rejectAmend(@Name("contractId") Long contractId, IServiceContext context) {`
  - id 行 L180: `public ErpCtContract activate(@Name("contractId") Long contractId, IServiceContext context) {`
  - id 行 L186: `public ErpCtContract suspend(@Name("contractId") Long contractId, IServiceContext context) {`
  - id 行 L200: `public ErpCtContract resume(@Name("contractId") Long contractId, IServiceContext context) {`
  - id 行 L214: `public ErpCtContract terminate(@Name("contractId") Long contractId,`
  - id 行 L216: `@Optional @Name("attachmentId") Long attachmentId,`
  - id 行 L236: `record.setContractId(contractId);`
  - id 行 L237: `record.setOrgId(contract.getOrgId());`
  - id 行 L240: `record.setApproverId(approvalEngine.resolveApproverId(`
  - id 行 L252: `public ErpCtContract approveTermination(@Name("recordId") Long recordId,`
  - id 行 L261: `archiveCurrentVersion(contract.getId(), context);`
  - id 行 L262: `haltUnexecutedInvoicePlans(contract.getId(), context);`
  - id 行 L276: `public ErpCtContract rejectTermination(@Name("recordId") Long recordId,`
  - id 行 L295: `public ErpCtContract expire(@Name("contractId") Long contractId, IServiceContext context) {`
  - id 行 L309: `public ErpCtContract amend(@Name("contractId") Long contractId, IServiceContext context) {`
  - id 行 L359: `contract.getId(), ex.getMessage());`
  - id 行 L372: `List<ErpCtContractLine> lines = findLines(contract.getId(), context);`
  - id 行 L378: `lineIds.add(line.getId());`
  - id 行 L391: `contractInvoicePlanBiz.triggerInvoice(plan.getId(), context);`
  - id 行 L394: `contract.getId(), plan.getId(), ex.getMessage());`
  - id 行 L412: `q.addFilter(eq("parentContractId", contract.getId()));`
  - id 行 L420: `draft.setOrgId(contract.getOrgId());`
  - id 行 L424: `draft.setPartnerId(contract.getPartnerId());`
  - id 行 L425: `draft.setCurrencyId(contract.getCurrencyId());`
  - id 行 L432: `draft.setParentContractId(contract.getId());`
  - id 行 L437: `contract.getId(), draft.getCode());`
  - id 行 L452: `protected ErpCtContract requireContract(Long contractId, IServiceContext context) {`
  - id 行 L464: `protected ErpCtApprovalRecord requireTerminationRecord(Long recordId, IServiceContext context) {`
  - id 行 L483: `.param(ErpCtErrors.ARG_APPROVAL_RECORD_ID, record.getId())`
  - id 行 L494: `.param(ErpCtErrors.ARG_APPROVAL_RECORD_ID, record.getId())`
  - id 行 L501: `protected String buildTerminationRemark(String reason, Long attachmentId) {`
  - id 行 L516: `protected void archiveCurrentVersion(Long contractId, IServiceContext context) {`
  - id 行 L529: `protected void haltUnexecutedInvoicePlans(Long contractId, IServiceContext context) {`
  - id 行 L536: `lineIds.add(line.getId());`
  - id 行 L546: `contractInvoicePlanBiz.delete(String.valueOf(plan.getId()), context);`
  - id 行 L556: `map.put("contractId", contract.getId());`
  - id 行 L569: `map.put("contractId", contract.getId());`
  - id 行 L581: `map.put("contractId", contract.getId());`
  - id 行 L609: `protected ErpCtContractVersion findCurrentVersion(Long contractId, IServiceContext context) {`
  - id 行 L616: `protected List<ErpCtContractVersion> findVersions(Long contractId, IServiceContext context) {`
  - id 行 L644: `protected void validateContractFields(ErpCtContract contract, Long contractId, IServiceContext context) {`
  - id 行 L660: `protected void ensureVersionOnSubmit(Long contractId, IServiceContext context) {`
  - id 行 L666: `v1.setContractId(contractId);`
  - id 行 L680: `protected void restoreCurrentVersion(Long contractId, IServiceContext context) {`
  - id 行 L701: `boolean isTarget = target != null && Objects.equals(v.getId(), target.getId());`
  - id 行 L714: `protected List<ErpCtContractLine> findLines(Long contractId, IServiceContext context) {`

### module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtDocumentBizModel.java
- 外域: notify  |  本文件 id-as-Long 证据行: 15
- 耦合点 L31: IErpSysNotificationBiz (notify)
  - id 行 L94: `public ErpCtDocument setLegalHold(@Name("documentId") Long documentId,`
  - id 行 L113: `public ErpCtDocument archive(@Name("documentId") Long documentId, IServiceContext context) {`
  - id 行 L150: `public ErpCtDocument purge(@Name("documentId") Long documentId, IServiceContext context) {`
  - id 行 L182: `LOG.info("erp-ct-doc-purged: documentId={}, code={}, operator={}", doc.getId(), doc.getCode(), operator);`
  - id 行 L205: `archive(doc.getId(), context);`
  - id 行 L209: `doc.getId(), ex.getMessage());`
  - id 行 L230: `purge(doc.getId(), context);`
  - id 行 L234: `doc.getId(), ex.getMessage());`
  - id 行 L249: `public ErpCtDocument startOcr(@Name("documentId") Long documentId, IServiceContext context) {`
  - id 行 L257: `doc.getId(), doc.getAttachmentFileId(), doc.getDocName(), doc.getMimeType()));`
  - id 行 L276: `public ErpCtDocument submitOcrText(@Name("documentId") Long documentId,`
  - id 行 L300: `@Optional @Name("contractId") Long contractId,`
  - id 行 L551: `payload.put("documentId", doc.getId());`
  - id 行 L559: `doc.getId(), ex.getMessage());`
  - id 行 L563: `ErpCtDocument requireDocument(Long documentId, IServiceContext context) {`

### module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtInvoicePlanBizModel.java
- 外域: purchase, sales  |  本文件 id-as-Long 证据行: 17
- 耦合点 L32: ErpPurInvoice (purchase)
- 耦合点 L33: ErpPurInvoiceLine (purchase)
- 耦合点 L34: ErpSalInvoice (sales)
- 耦合点 L35: ErpSalInvoiceLine (sales)
- 耦合点 L138: ErpPurInvoice.class (daoFor) (purchase)
- 耦合点 L158: ErpPurInvoiceLine.class (daoFor) (purchase)
- 耦合点 L170: ErpPurInvoiceLine.class (daoFor) (purchase)
- 耦合点 L175: ErpSalInvoice.class (daoFor) (sales)
- 耦合点 L195: ErpSalInvoiceLine.class (daoFor) (sales)
- 耦合点 L207: ErpSalInvoiceLine.class (daoFor) (sales)
  - id 行 L77: `public ErpCtInvoicePlan triggerInvoice(@Name("planId") Long planId, IServiceContext context) {`
  - id 行 L83: `public int triggerDuePlans(@Name("contractId") Long contractId,`
  - id 行 L91: `public List<ErpCtInvoicePlan> generateInvoicePlansByTerm(@Name("contractId") Long contractId,`
  - id 行 L124: `.param(ErpCtErrors.ARG_INVOICE_PLAN_ID, entity.getId());`
  - id 行 L142: `invoice.setOrgId(contract.getOrgId());`
  - id 行 L144: `invoice.setSupplierId(contract.getPartnerId());`
  - id 行 L146: `invoice.setCurrencyId(contract.getCurrencyId());`
  - id 行 L159: `invLine.setInvoiceId(invoice.getId());`
  - id 行 L162: `invLine.setMaterialId(line.getMaterialId());`
  - id 行 L164: `invLine.setUoMId(line.getMaterial().getUoMId());`
  - id 行 L179: `invoice.setOrgId(contract.getOrgId());`
  - id 行 L181: `invoice.setCustomerId(contract.getPartnerId());`
  - id 行 L183: `invoice.setCurrencyId(contract.getCurrencyId());`
  - id 行 L196: `invLine.setInvoiceId(invoice.getId());`
  - id 行 L199: `invLine.setMaterialId(line.getMaterialId());`
  - id 行 L201: `invLine.setUoMId(line.getMaterial().getUoMId());`
  - id 行 L212: `protected ErpCtInvoicePlan requirePlan(Long planId, IServiceContext context) {`

### module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtRebateAgreementBizModel.java
- 外域: purchase, sales  |  本文件 id-as-Long 证据行: 3
- 耦合点 L23: ErpPurInvoice (purchase)
- 耦合点 L24: ErpSalInvoice (sales)
- 耦合点 L112: ErpPurInvoice.class (daoFor) (purchase)
- 耦合点 L115: ErpSalInvoice.class (daoFor) (sales)
  - id 行 L74: `public ErpCtRebateAgreement runAccrual(@Name("agreementId") Long agreementId,`
  - id 行 L82: `protected ErpCtRebateAgreement requireAgreement(Long agreementId, IServiceContext context) {`
  - id 行 L91: `protected Set<String> loadAccruedBillCodes(Long agreementId) {`

### module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtRebateSettlementBizModel.java
- 外域: master-data, purchase, sales  |  本文件 id-as-Long 证据行: 20
- 耦合点 L27: ErpPurInvoice (purchase)
- 耦合点 L28: ErpPurInvoiceLine (purchase)
- 耦合点 L29: ErpSalInvoice (sales)
- 耦合点 L30: ErpSalInvoiceLine (sales)
- 耦合点 L84: ErpPurInvoice.class (daoFor) (purchase)
- 耦合点 L104: ErpPurInvoiceLine.class (daoFor) (purchase)
- 耦合点 L112: ErpPurInvoiceLine.class (daoFor) (purchase)
- 耦合点 L117: ErpSalInvoice.class (daoFor) (sales)
- 耦合点 L137: ErpSalInvoiceLine.class (daoFor) (sales)
- 耦合点 L145: ErpSalInvoiceLine.class (daoFor) (sales)
- 耦合点 L201: ErpMdMaterial (master-data)
- 耦合点 L202: ErpMdMaterial.class (master-data)
  - id 行 L74: `public ErpCtRebateSettlement postSettlement(@Name("settlementId") Long settlementId, IServiceContext context) {`
  - id 行 L83: `Long currencyId, Long materialId, Long uomId, BigDecimal negativeAmount) {`
  - id 行 L88: `invoice.setOrgId(agreement.getOrgId());`
  - id 行 L90: `invoice.setSupplierId(agreement.getPartnerId());`
  - id 行 L92: `invoice.setCurrencyId(currencyId);`
  - id 行 L105: `line.setInvoiceId(invoice.getId());`
  - id 行 L107: `line.setMaterialId(materialId);`
  - id 行 L108: `line.setUoMId(uomId);`
  - id 行 L116: `Long currencyId, Long materialId, Long uomId, BigDecimal negativeAmount) {`
  - id 行 L121: `invoice.setOrgId(agreement.getOrgId());`
  - id 行 L123: `invoice.setCustomerId(agreement.getPartnerId());`
  - id 行 L125: `invoice.setCurrencyId(currencyId);`
  - id 行 L138: `line.setInvoiceId(invoice.getId());`
  - id 行 L140: `line.setMaterialId(materialId);`
  - id 行 L141: `line.setUoMId(uomId);`
  - id 行 L150: `protected ErpCtRebateSettlement requireSettlement(Long settlementId, IServiceContext context) {`
  - id 行 L159: `protected List<ErpCtRebateAccrual> findUnsettledAccruals(Long agreementId) {`
  - id 行 L170: `protected Long resolveCurrencyId(ErpCtRebateAgreement agreement) {`
  - id 行 L183: `protected Long resolveMaterialId(ErpCtRebateAgreement agreement) {`
  - id 行 L197: `protected Long resolveUoMId(Long materialId) {`

### module-contract/erp-ct-service/src/main/java/app/erp/ct/service/job/ErpCtApprovalTimeoutEscalationJob.java
- 外域: notify  |  本文件 id-as-Long 证据行: 3
- 耦合点 L10: IErpSysNotificationBiz (notify)
  - id 行 L130: `record.getId(), e.getMessage());`
  - id 行 L146: `record.getId());`
  - id 行 L185: `protected ErpCtContract findContract(Long contractId, IServiceContext ctx) {`

### module-contract/erp-ct-service/src/main/java/app/erp/ct/service/job/ErpCtContractExpiryJob.java
- 外域: notify  |  本文件 id-as-Long 证据行: 4
- 耦合点 L7: IErpSysNotificationBiz (notify)
  - id 行 L127: `contract.getId(), ex.getMessage());`
  - id 行 L161: `map.put("contractId", contract.getId());`
  - id 行 L178: `contract.getId());`
  - id 行 L185: `map.put("contractId", contract.getId());`

### module-contract/erp-ct-service/src/main/java/app/erp/ct/service/processor/ErpCtConsumptionPeriodSummarizeProcessor.java
- 外域: notify  |  本文件 id-as-Long 证据行: 9
- 耦合点 L9: IErpSysNotificationBiz (notify)
  - id 行 L63: `public ErpCtConsumptionPeriodSummarizeResult periodSummarize(Long contractLineId,`
  - id 行 L81: `result.setContractLineId(contractLineId);`
  - id 行 L97: `item.setContractLineId(contractLineId);`
  - id 行 L102: `.generateInvoicePlansByTerm(contract.getId(), List.of(item), context);`
  - id 行 L105: `result.setOveragePlanId(plan.getId());`
  - id 行 L111: `ErpCtInvoicePlan triggered = triggerInvoiceProcessor.triggerInvoice(plan.getId(), context);`
  - id 行 L120: `ctx.put("contractId", contract.getId());`
  - id 行 L143: `protected ErpCtContractLine requireLine(Long contractLineId) {`
  - id 行 L165: `protected BigDecimal sumConsumedQuantity(Long contractLineId, LocalDate fromDate, LocalDate toDate) {`

### module-contract/erp-ct-service/src/main/java/app/erp/ct/service/processor/ErpCtInvoicePlanTriggerInvoiceProcessor.java
- 外域: purchase, sales  |  本文件 id-as-Long 证据行: 15
- 耦合点 L8: ErpPurInvoice (purchase)
- 耦合点 L9: ErpPurInvoiceLine (purchase)
- 耦合点 L10: ErpSalInvoice (sales)
- 耦合点 L11: ErpSalInvoiceLine (sales)
- 耦合点 L76: ErpPurInvoice.class (daoFor) (purchase)
- 耦合点 L96: ErpPurInvoiceLine.class (daoFor) (purchase)
- 耦合点 L108: ErpPurInvoiceLine.class (daoFor) (purchase)
- 耦合点 L113: ErpSalInvoice.class (daoFor) (sales)
- 耦合点 L133: ErpSalInvoiceLine.class (daoFor) (sales)
- 耦合点 L145: ErpSalInvoiceLine.class (daoFor) (sales)
  - id 行 L35: `public ErpCtInvoicePlan triggerInvoice(Long planId, IServiceContext context) {`
  - id 行 L57: `String billCode = "CT-INV-" + plan.getId();`
  - id 行 L80: `invoice.setOrgId(contract.getOrgId());`
  - id 行 L82: `invoice.setSupplierId(contract.getPartnerId());`
  - id 行 L84: `invoice.setCurrencyId(contract.getCurrencyId());`
  - id 行 L97: `invLine.setInvoiceId(invoice.getId());`
  - id 行 L100: `invLine.setMaterialId(line.getMaterialId());`
  - id 行 L102: `invLine.setUoMId(line.getMaterial().getUoMId());`
  - id 行 L117: `invoice.setOrgId(contract.getOrgId());`
  - id 行 L119: `invoice.setCustomerId(contract.getPartnerId());`
  - id 行 L121: `invoice.setCurrencyId(contract.getCurrencyId());`
  - id 行 L134: `invLine.setInvoiceId(invoice.getId());`
  - id 行 L137: `invLine.setMaterialId(line.getMaterialId());`
  - id 行 L139: `invLine.setUoMId(line.getMaterial().getUoMId());`
  - id 行 L150: `protected ErpCtInvoicePlan requirePlan(Long planId) {`

### module-contract/erp-ct-service/src/main/java/app/erp/ct/service/processor/ErpCtRebateAgreementRunAccrualProcessor.java
- 外域: purchase, sales  |  本文件 id-as-Long 证据行: 3
- 耦合点 L9: ErpPurInvoice (purchase)
- 耦合点 L10: ErpSalInvoice (sales)
- 耦合点 L113: ErpPurInvoice.class (daoFor) (purchase)
- 耦合点 L116: ErpSalInvoice.class (daoFor) (sales)
  - id 行 L48: `public ErpCtRebateAgreement runAccrual(Long agreementId, LocalDate asOfDate, IServiceContext context) {`
  - id 行 L83: `protected ErpCtRebateAgreement requireAgreement(Long agreementId) {`
  - id 行 L92: `protected Set<String> loadAccruedBillCodes(Long agreementId) {`

### module-contract/erp-ct-service/src/main/java/app/erp/ct/service/processor/ErpCtRebateSettlementPostSettlementProcessor.java
- 外域: master-data, purchase, sales  |  本文件 id-as-Long 证据行: 25
- 耦合点 L11: ErpPurInvoice (purchase)
- 耦合点 L12: ErpPurInvoiceLine (purchase)
- 耦合点 L13: ErpSalInvoice (sales)
- 耦合点 L14: ErpSalInvoiceLine (sales)
- 耦合点 L107: ErpPurInvoice.class (daoFor) (purchase)
- 耦合点 L127: ErpPurInvoiceLine.class (daoFor) (purchase)
- 耦合点 L135: ErpPurInvoiceLine.class (daoFor) (purchase)
- 耦合点 L140: ErpSalInvoice.class (daoFor) (sales)
- 耦合点 L160: ErpSalInvoiceLine.class (daoFor) (sales)
- 耦合点 L168: ErpSalInvoiceLine.class (daoFor) (sales)
- 耦合点 L236: ErpMdMaterial (master-data)
- 耦合点 L237: ErpMdMaterial.class (master-data)
  - id 行 L47: `public ErpCtRebateSettlement postSettlement(Long settlementId, IServiceContext context) {`
  - id 行 L66: `Long currencyId = resolveCurrencyId(agreement);`
  - id 行 L68: `Long materialId = resolveMaterialId(agreement);`
  - id 行 L69: `Long uomId = resolveUoMId(materialId);`
  - id 行 L72: `String creditMemoCode = "CT-REBATE-" + settlement.getId();`
  - id 行 L106: `Long currencyId, Long materialId, Long uomId, BigDecimal negativeAmount) {`
  - id 行 L111: `invoice.setOrgId(agreement.getOrgId());`
  - id 行 L113: `invoice.setSupplierId(agreement.getPartnerId());`
  - id 行 L115: `invoice.setCurrencyId(currencyId);`
  - id 行 L128: `line.setInvoiceId(invoice.getId());`
  - id 行 L130: `line.setMaterialId(materialId);`
  - id 行 L131: `line.setUoMId(uomId);`
  - id 行 L139: `Long currencyId, Long materialId, Long uomId, BigDecimal negativeAmount) {`
  - id 行 L144: `invoice.setOrgId(agreement.getOrgId());`
  - id 行 L146: `invoice.setCustomerId(agreement.getPartnerId());`
  - id 行 L148: `invoice.setCurrencyId(currencyId);`
  - id 行 L161: `line.setInvoiceId(invoice.getId());`
  - id 行 L163: `line.setMaterialId(materialId);`
  - id 行 L164: `line.setUoMId(uomId);`
  - id 行 L179: `protected NopException illegalTransition(Long settlementId, ErpCtRebateSettlement settlement, Throwable cause) {`
  - id 行 L185: `protected ErpCtRebateSettlement requireSettlement(Long settlementId) {`
  - id 行 L194: `protected List<ErpCtRebateAccrual> findUnsettledAccruals(Long agreementId) {`
  - id 行 L205: `protected Long resolveCurrencyId(ErpCtRebateAgreement agreement) {`
  - id 行 L218: `protected Long resolveMaterialId(ErpCtRebateAgreement agreement) {`
  - id 行 L232: `protected Long resolveUoMId(Long materialId) {`

## module-crm （引用外域的耦合文件 11 个）

### module-crm/erp-crm-dao/src/main/java/app/erp/crm/biz/IErpCrmConversionBiz.java
- 外域: master-data, sales  |  本文件 id-as-Long 证据行: 4
- 耦合点 L4: ErpMdPartner (master-data)
- 耦合点 L5: ErpSalQuotation (sales)
  - id 行 L29: `ErpMdPartner convertToCustomer(@Name("leadId") Long leadId, IServiceContext context);`
  - id 行 L37: `ErpCrmLead convertToOpportunity(@Name("leadId") Long leadId, IServiceContext context);`
  - id 行 L46: `ErpSalQuotation convertToQuotation(@Name("leadId") Long leadId,`
  - id 行 L54: `ErpCrmLead getCreatedOpportunity(@Name("leadId") Long leadId, IServiceContext context);`

### module-crm/erp-crm-dao/src/main/java/app/erp/crm/biz/IErpCrmLeadBiz.java
- 外域: master-data, sales  |  本文件 id-as-Long 证据行: 11
- 耦合点 L4: ErpMdPartner (master-data)
- 耦合点 L5: ErpSalQuotation (sales)
  - id 行 L25: `ErpCrmLead qualify(@Name("leadId") Long leadId, IServiceContext context);`
  - id 行 L32: `ErpCrmLead lose(@Name("leadId") Long leadId,`
  - id 行 L33: `@Optional @Name("lostReasonId") Long lostReasonId,`
  - id 行 L38: `ErpCrmLead cancel(@Name("leadId") Long leadId, IServiceContext context);`
  - id 行 L41: `ErpCrmLead moveStage(@Name("leadId") Long leadId,`
  - id 行 L42: `@Name("toStageId") Long toStageId,`
  - id 行 L50: `List<ErpCrmLead> findDuplicates(@Name("leadId") Long leadId, IServiceContext context);`
  - id 行 L58: `ErpCrmLead assignLead(@Name("leadId") Long leadId, IServiceContext context);`
  - id 行 L65: `ErpCrmLead reassignLead(@Name("leadId") Long leadId,`
  - id 行 L66: `@Optional @Name("territoryId") Long territoryId,`
  - id 行 L67: `@Optional @Name("teamId") Long teamId,`

### module-crm/erp-crm-dao/src/main/java/app/erp/crm/biz/IErpCrmProductConfiguratorBiz.java
- 外域: sales  |  本文件 id-as-Long 证据行: 3
- 耦合点 L11: ErpSalQuotation (sales)
  - id 行 L35: `ErpSalQuotation generateQuote(@Name("configuratorId") Long configuratorId,`
  - id 行 L37: `@Optional @Name("bundlePricingId") Long bundlePricingId,`
  - id 行 L39: `@Optional @Name("leadId") Long leadId,`

### module-crm/erp-crm-service/src/main/java/app/erp/crm/service/entity/ErpCrmLeadBizModel.java
- 外域: master-data, sales  |  本文件 id-as-Long 证据行: 40
- 耦合点 L24: ErpMdPartner (master-data)
- 耦合点 L25: ErpSalQuotation (sales)
  - id 行 L105: `public ErpCrmLead qualify(@Name("leadId") Long leadId, IServiceContext context) {`
  - id 行 L111: `public ErpCrmLead lose(@Name("leadId") Long leadId,`
  - id 行 L113: `@Name("lostReasonId") Long lostReasonId,`
  - id 行 L122: `public ErpCrmLead cancel(@Name("leadId") Long leadId, IServiceContext context) {`
  - id 行 L128: `public ErpCrmLead moveStage(@Name("leadId") Long leadId,`
  - id 行 L129: `@Name("toStageId") Long toStageId,`
  - id 行 L136: `public List<ErpCrmLead> findDuplicates(@Name("leadId") Long leadId, IServiceContext context) {`
  - id 行 L145: `public ErpCrmLead assignLead(@Name("leadId") Long leadId, IServiceContext context) {`
  - id 行 L155: `lead.setTerritoryId(result.getTerritoryId());`
  - id 行 L158: `lead.setTeamId(result.getTeamId());`
  - id 行 L163: `lead.setOwnerId(result.getOwnerId());`
  - id 行 L171: `public ErpCrmLead reassignLead(@Name("leadId") Long leadId,`
  - id 行 L172: `@Optional @Name("territoryId") Long territoryId,`
  - id 行 L173: `@Optional @Name("teamId") Long teamId,`
  - id 行 L178: `lead.setTerritoryId(territoryId);`
  - id 行 L181: `lead.setTeamId(teamId);`
  - id 行 L184: `lead.setOwnerId(ownerId);`
  - id 行 L194: `public ErpMdPartner convertToCustomer(@Name("leadId") Long leadId, IServiceContext context) {`
  - id 行 L200: `public ErpSalQuotation convertToQuotation(@Name("leadId") Long leadId,`
  - id 行 L209: `public ErpCrmLead convertToOpportunity(@Name("leadId") Long leadId, IServiceContext context) {`
  - id 行 L215: `public ErpCrmLead getCreatedOpportunity(@Name("leadId") Long leadId, IServiceContext context) {`
  - id 行 L227: `if (lead.getId() == null && lead.getTerritoryId() == null) {`
  - id 行 L237: `lead.setTerritoryId(result.getTerritoryId());`
  - id 行 L240: `lead.setTeamId(result.getTeamId());`
  - id 行 L243: `lead.setOwnerId(result.getOwnerId());`
  - id 行 L252: `if (lead.getId() == null && lead.getCampaignId() != null`
  - id 行 L271: `if (lead.getId() == null) {`
  - id 行 L277: `scoringEngine.recalculateScore(lead.getId(),`
  - id 行 L284: `protected List<ErpCrmTerritoryAssignmentRule> loadActiveRules(Long orgId) {`
  - id 行 L293: `protected ErpCrmTerritoryAssignmentRule loadDefaultRule(Long orgId) {`
  - id 行 L318: `public List<String> resolveTeamMemberUserIds(Long teamId, IServiceContext context) {`
  - id 行 L336: `public String resolveLastAssignedOwner(Long teamId, IServiceContext context) {`
  - id 行 L350: `public Map<String, Integer> countActiveLeadsByOwner(Long teamId, IServiceContext context) {`
  - id 行 L406: `rootChildren.add("col-" + s.getId());`
  - id 行 L411: `String colId = "col-" + s.getId();`
  - id 行 L414: `if (l.getStageId() != null && l.getStageId().equals(s.getId())) {`
  - id 行 L415: `cardIds.add("card-" + l.getId());`
  - id 行 L420: `colData.put("stageId", s.getId());`
  - id 行 L426: `String cardId = "card-" + l.getId();`
  - id 行 L430: `cardData.put("leadId", l.getId());`

### module-crm/erp-crm-service/src/main/java/app/erp/crm/service/entity/ErpCrmProductConfiguratorBizModel.java
- 外域: sales  |  本文件 id-as-Long 证据行: 3
- 耦合点 L7: ErpSalQuotation (sales)
  - id 行 L37: `public ErpSalQuotation generateQuote(@Name("configuratorId") Long configuratorId,`
  - id 行 L39: `@Optional @Name("bundlePricingId") Long bundlePricingId,`
  - id 行 L41: `@Optional @Name("leadId") Long leadId,`

### module-crm/erp-crm-service/src/main/java/app/erp/crm/service/job/ErpCrmEventReminderJob.java
- 外域: notify  |  本文件 id-as-Long 证据行: 2
- 耦合点 L6: IErpSysNotificationBiz (notify)
  - id 行 L88: `event.getId(), e.getMessage());`
  - id 行 L102: `map.put("eventId", event.getId());`

### module-crm/erp-crm-service/src/main/java/app/erp/crm/service/job/ErpCrmSequenceOverdueJob.java
- 外域: notify  |  本文件 id-as-Long 证据行: 1
- 耦合点 L8: IErpSysNotificationBiz (notify)
  - id 行 L101: `Long leadId = toLong(row.get("leadId"));`

### module-crm/erp-crm-service/src/main/java/app/erp/crm/service/processor/ErpCrmConversionConvertToCustomerProcessor.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 1
- 耦合点 L5: ErpMdPartner (master-data)
  - id 行 L19: `public ErpMdPartner convertToCustomer(Long leadId, IServiceContext context) {`

### module-crm/erp-crm-service/src/main/java/app/erp/crm/service/processor/ErpCrmConversionConvertToQuotationProcessor.java
- 外域: sales  |  本文件 id-as-Long 证据行: 1
- 耦合点 L5: ErpSalQuotation (sales)
  - id 行 L21: `public ErpSalQuotation convertToQuotation(Long leadId, Map<String, Object> quotationData, IServiceContext context) {`

### module-crm/erp-crm-service/src/main/java/app/erp/crm/service/processor/ErpCrmConversionProcessor.java
- 外域: master-data, sales  |  本文件 id-as-Long 证据行: 11
- 耦合点 L9: IErpMdPartnerBiz (master-data)
- 耦合点 L10: ErpMdPartner (master-data)
- 耦合点 L11: IErpSalQuotationBiz (sales)
- 耦合点 L12: ErpSalQuotation (sales)
  - id 行 L58: `public ErpCrmLead getCreatedOpportunity(Long leadId, IServiceContext context) {`
  - id 行 L74: `data.put("code", "CUS-" + lead.getId());`
  - id 行 L90: `data.put("code", "SQ-" + lead.getId());`
  - id 行 L100: `opportunity.setOrgId(lead.getOrgId());`
  - id 行 L101: `opportunity.setCode("OPP-" + lead.getId());`
  - id 行 L103: `opportunity.setPartnerId(partner.getId());`
  - id 行 L109: `opportunity.setOwnerId(lead.getOwnerId());`
  - id 行 L110: `opportunity.setTeamId(lead.getTeamId());`
  - id 行 L165: `Long stageId = lead.getStageId();`
  - id 行 L196: `protected ErpCrmLead requireLead(Long leadId, IServiceContext context) {`
  - id 行 L209: `return lead.getContactName() != null ? lead.getContactName() : ("客户-" + lead.getId());`

### module-crm/erp-crm-service/src/main/java/app/erp/crm/service/processor/ErpCrmProductConfiguratorGenerateQuoteProcessor.java
- 外域: sales  |  本文件 id-as-Long 证据行: 15
- 耦合点 L14: IErpSalQuotationBiz (sales)
- 耦合点 L15: ErpSalQuotation (sales)
  - id 行 L58: `public ErpSalQuotation generateQuote(Long configuratorId,`
  - id 行 L60: `Long bundlePricingId,`
  - id 行 L62: `Long leadId,`
  - id 行 L73: `Long currencyId = readLong(priceRuleContext, "currencyId");`
  - id 行 L110: `Long leadOrgId = null;`
  - id 行 L111: `Long leadCustomerId = null;`
  - id 行 L136: `protected ErpCrmProductConfigurator requireConfiguratorActive(Long configuratorId) {`
  - id 行 L160: `protected List<ErpCrmConfigRule> loadConfigRules(Long configuratorId) {`
  - id 行 L166: `protected BundlePricingCalculator.BundleResult computeBundle(Long bundlePricingId) {`
  - id 行 L175: `Long productId = readLong(ctx, "productId");`
  - id 行 L176: `Long customerId = readLong(ctx, "customerId");`
  - id 行 L185: `protected List<ErpCrmPriceRule> loadActivePriceRules(Long productId, Long customerId, Long currencyId) {`
  - id 行 L220: `BigDecimal totalAmount, Long currencyId,`
  - id 行 L221: `Long leadOrgId, Long leadCustomerId,`
  - id 行 L225: `data.put("code", "CPQ-" + configurator.getId() + "-" + CoreMetrics.currentTimeMillis());`

## module-cs （引用外域的耦合文件 12 个）

### module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/ErpCsCannedResponseBizModel.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 14
- 耦合点 L12: IErpMdPartnerBiz (master-data)
- 耦合点 L13: ErpMdPartner (master-data)
  - id 行 L77: `public String renderTemplate(@Name("cannedResponseId") Long cannedResponseId,`
  - id 行 L78: `@Name("ticketId") Long ticketId,`
  - id 行 L89: `public List<ErpCsCannedResponse> suggestForTicket(@Name("ticketId") Long ticketId,`
  - id 行 L98: `Long ticketTypeId = ticket.getTicketTypeId();`
  - id 行 L132: `public String applyCannedResponse(@Name("cannedResponseId") Long cannedResponseId,`
  - id 行 L133: `@Name("ticketId") Long ticketId,`
  - id 行 L157: `.param(ErpCsErrors.ARG_CANNED_RESPONSE_ID, resp.getId());`
  - id 行 L161: `private Map<String, String> resolveSystemVars(ErpCsCannedResponse resp, Long ticketId, IServiceContext context) {`
  - id 行 L186: `private ErpCsTicket loadTicket(Long ticketId, IServiceContext context) {`
  - id 行 L193: `private String resolveCustomerName(Long customerId, IServiceContext context) {`
  - id 行 L217: `Long macroTicketTypeId, String macroPriority, int limit) {`
  - id 行 L226: `if (r.getId() != null && collected.contains(r.getId())) {`
  - id 行 L237: `if (r.getId() != null) {`
  - id 行 L238: `collected.add(r.getId());`

### module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/ErpCsEntitlementBizModel.java
- 外域: master-data, notify  |  本文件 id-as-Long 证据行: 8
- 耦合点 L9: IErpSysNotificationBiz (notify)
- 耦合点 L10: IErpMdPartnerBiz (master-data)
- 耦合点 L11: ErpMdPartner (master-data)
  - id 行 L80: `public ErpCsEntitlement consumeEntitlement(@Name("entitlementId") Long entitlementId,`
  - id 行 L98: `entitlement.getServiceType(), entitlement.getId(), entitlement.getMaxTickets());`
  - id 行 L105: `public ErpCsEntitlement releaseEntitlement(@Name("entitlementId") Long entitlementId,`
  - id 行 L147: `public List<Map<String, Object>> getEntitlementUsage(@Name("partnerId") Long partnerId,`
  - id 行 L184: `protected List<ErpCsEntitlement> loadActiveByPartner(Long partnerId) {`
  - id 行 L200: `public ErpCsEntitlement matchForCustomer(Long customerIdAsPartnerId) {`
  - id 行 L205: `private ErpCsEntitlement requireEntitlement(Long entitlementId, IServiceContext context) {`
  - id 行 L224: `private String resolvePartnerName(Long partnerId, IServiceContext context) {`

### module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/ErpCsTicketBizModel.java
- 外域: master-data, notify  |  本文件 id-as-Long 证据行: 35
- 耦合点 L21: IErpMdPartnerBiz (master-data)
- 耦合点 L22: ErpMdPartner (master-data)
- 耦合点 L23: IErpSysNotificationBiz (notify)
  - id 行 L135: `matchAndAttachSlaProcessor.matchAndAttachSla(ticket.getId(), context);`
  - id 行 L138: `ticket.getId(), e.getMessage());`
  - id 行 L172: `ticket.setAssignedToId(assignee);`
  - id 行 L230: `public ErpCsTicket assign(@Name("ticketId") Long ticketId,`
  - id 行 L236: `ticket.setAssignedToId(assignedToId);`
  - id 行 L246: `public ErpCsTicket start(@Name("ticketId") Long ticketId, IServiceContext context) {`
  - id 行 L261: `public ErpCsTicket resolve(@Name("ticketId") Long ticketId,`
  - id 行 L269: `public ErpCsTicket close(@Name("ticketId") Long ticketId, IServiceContext context) {`
  - id 行 L289: `public ErpCsTicket reopen(@Name("ticketId") Long ticketId, IServiceContext context) {`
  - id 行 L295: `public ErpCsTicket cancel(@Name("ticketId") Long ticketId,`
  - id 行 L328: `public ErpCsTicket adoptKnowledge(@Name("ticketId") Long ticketId,`
  - id 行 L329: `@Name("knowledgeBaseId") Long knowledgeBaseId,`
  - id 行 L351: `public ErpCsTicket escalateToQuality(@Name("ticketId") Long ticketId,`
  - id 行 L352: `@Optional @Name("materialId") Long materialId,`
  - id 行 L357: `@Optional @Name("supplierId") Long supplierId,`
  - id 行 L367: `public List<Map<String, Object>> findQualityNcrs(@Name("ticketId") Long ticketId, IServiceContext context) {`
  - id 行 L374: `public ErpCsTicket matchAndAttachSla(@Name("ticketId") Long ticketId, IServiceContext context) {`
  - id 行 L408: `public Map<String, Object> findBoardData(@Optional @Name("customerId") Long customerId, IServiceContext context) {`
  - id 行 L433: `cardIds.add("card-" + t.getId());`
  - id 行 L443: `String cardId = "card-" + t.getId();`
  - id 行 L447: `cardData.put("ticketId", t.getId());`
  - id 行 L475: `public long totalTimeSpent(@Name("ticketId") Long ticketId, IServiceContext context) {`
  - id 行 L488: `public long totalBillableTime(@Name("ticketId") Long ticketId, IServiceContext context) {`
  - id 行 L501: `public java.math.BigDecimal totalBilledAmount(@Name("ticketId") Long ticketId, IServiceContext context) {`
  - id 行 L513: `private List<ErpCsTimeEntry> findEntries(Long ticketId, List<String> approvalStatuses,`
  - id 行 L534: `ctx.put("ticketId", ticket.getId());`
  - id 行 L545: `ticket.getId(), e.getMessage());`
  - id 行 L569: `ctx.put("ticketId", ticket.getId());`
  - id 行 L576: `ticket.getId(), e.getMessage());`
  - id 行 L587: `ctx.put("ticketId", ticket.getId());`
  - id 行 L593: `ticket.getId(), e.getMessage());`
  - id 行 L597: `private String resolveCustomerName(Long customerId, IServiceContext context) {`
  - id 行 L609: `private ErpCsTicket requireTicket(Long ticketId, IServiceContext context) {`
  - id 行 L655: `action.setTicketId(ticket.getId());`
  - id 行 L660: `action.setOperatorId(context.getUserId());`

### module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/TicketAssignResolver.java
- 外域: crm  |  本文件 id-as-Long 证据行: 1
- 耦合点 L3: IErpCrmTeamBiz (crm)
- 耦合点 L4: IErpCrmTeamMemberBiz (crm)
- 耦合点 L5: ErpCrmTeam (crm)
- 耦合点 L6: ErpCrmTeamMember (crm)
  - id 行 L57: `mq.addFilter(eq("teamId", matched.get(0).getId()));`

### module-cs/erp-cs-service/src/main/java/app/erp/cs/service/job/ErpCsCsatReminderJob.java
- 外域: notify  |  本文件 id-as-Long 证据行: 3
- 耦合点 L8: IErpSysNotificationBiz (notify)
  - id 行 L111: `survey.getId(), e.getMessage());`
  - id 行 L124: `map.put("surveyId", survey.getId());`
  - id 行 L131: `private ErpCsTicket loadTicket(Long ticketId) {`

### module-cs/erp-cs-service/src/main/java/app/erp/cs/service/job/ErpCsEntitlementExpiryJob.java
- 外域: master-data, notify  |  本文件 id-as-Long 证据行: 3
- 耦合点 L6: IErpSysNotificationBiz (notify)
- 耦合点 L7: IErpMdPartnerBiz (master-data)
- 耦合点 L8: ErpMdPartner (master-data)
  - id 行 L91: `e.getId(), ex.getMessage());`
  - id 行 L108: `map.put("entitlementId", entitlement.getId());`
  - id 行 L119: `private String resolvePartnerName(Long partnerId, IServiceContext ctx) {`

### module-cs/erp-cs-service/src/main/java/app/erp/cs/service/job/ErpCsSurveySendJob.java
- 外域: notify  |  本文件 id-as-Long 证据行: 2
- 耦合点 L7: IErpSysNotificationBiz (notify)
  - id 行 L159: `survey.getId(), survey.getFailureCount(), cause.getMessage());`
  - id 行 L165: `map.put("surveyId", survey.getId());`

### module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsCannedResponseApplyCannedResponseProcessor.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 8
- 耦合点 L11: IErpMdPartnerBiz (master-data)
- 耦合点 L12: ErpMdPartner (master-data)
  - id 行 L45: `public String applyCannedResponse(Long cannedResponseId, Long ticketId,`
  - id 行 L79: `.param(ErpCsErrors.ARG_CANNED_RESPONSE_ID, resp.getId());`
  - id 行 L83: `private Map<String, String> resolveSystemVars(ErpCsCannedResponse resp, Long ticketId, IServiceContext context) {`
  - id 行 L108: `private ErpCsTicket loadTicket(Long ticketId, IServiceContext context) {`
  - id 行 L115: `private String resolveCustomerName(Long customerId, IServiceContext context) {`
  - id 行 L127: `private void writeNoteAction(Long ticketId, String content, Long cannedResponseId, IServiceContext context) {`
  - id 行 L132: `action.setTicketId(ticketId);`
  - id 行 L135: `action.setOperatorId(context == null ? null : context.getUserId());`

### module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor.java
- 外域: master-data, notify  |  本文件 id-as-Long 证据行: 28
- 耦合点 L19: IErpMdPartnerBiz (master-data)
- 耦合点 L20: ErpMdPartner (master-data)
- 耦合点 L21: IErpSysNotificationBiz (notify)
  - id 行 L102: `public List<ErpCsTicketFulfillmentStep> executeFulfillmentSteps(Long catalogItemId, Long ticketId,`
  - id 行 L130: `Long ticketId, IServiceContext context) {`
  - id 行 L139: `private ErpCsTicketFulfillmentStep materializeStep(ErpCsCatalogFulfillment template, Long ticketId,`
  - id 行 L143: `q.addFilter(eq("fulfillmentId", template.getId()));`
  - id 行 L150: `step.setTicketId(ticketId);`
  - id 行 L151: `step.setFulfillmentId(template.getId());`
  - id 行 L152: `step.setCatalogItemId(template.getCatalogItemId());`
  - id 行 L194: `ticket.getId(), step.getId(), step.getActionType(), e.getMessage());`
  - id 行 L205: `public void continueChain(Long ticketId, IServiceContext context) {`
  - id 行 L283: `ticket.setAssignedToId(assignee);`
  - id 行 L350: `ctx.put("ticketId", ticket.getId());`
  - id 行 L358: `ticket.getId(), e.getMessage());`
  - id 行 L406: `public ErpCsTicketFulfillmentStep approveFulfillmentStep(Long stepId, boolean approved, String comment,`
  - id 行 L438: `public List<ErpCsTicketFulfillmentStep> retryFulfillment(Long ticketId, IServiceContext context) {`
  - id 行 L449: `.param(ErpCsErrors.ARG_STEP_ID, step.getId())`
  - id 行 L468: `public int retryForJob(Long ticketId, IServiceContext context) {`
  - id 行 L509: `private ErpCsTicket requireTicket(Long ticketId, IServiceContext context) {`
  - id 行 L586: `ticket.setAssignedToId(operatorId(ticket, context));`
  - id 行 L644: `ctx.put("ticketId", ticket.getId());`
  - id 行 L654: `ticket.getId(), e.getMessage());`
  - id 行 L672: `ticket.getId(), e.getMessage());`
  - id 行 L681: `ctx.put("ticketId", ticket.getId());`
  - id 行 L689: `ticket.getId(), e.getMessage());`
  - id 行 L699: `action.setTicketId(ticket.getId());`
  - id 行 L704: `action.setOperatorId(context.getUserId());`
  - id 行 L741: `private ErpCsTicketFulfillmentStep requireStep(Long stepId, IServiceContext context) {`
  - id 行 L846: `private List<ErpCsTicketFulfillmentStep> loadStepsByTicket(Long ticketId, IServiceContext context) {`
  - id 行 L853: `}    private List<ErpCsCatalogFulfillment> loadTemplatesByCatalogItem(Long catalogItemId) {`

### module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsTicketEscalateToQualityProcessor.java
- 外域: quality  |  本文件 id-as-Long 证据行: 11
- 耦合点 L9: IErpQaNonConformanceBiz (quality)
- 耦合点 L10: ErpQaNonConformance (quality)
  - id 行 L76: `public ErpCsTicket escalateToQuality(ErpCsTicket ticket, Long materialId, String defectDescription,`
  - id 行 L78: `Long supplierId, IServiceContext context) {`
  - id 行 L81: `.param(ErpCsErrors.ARG_TICKET_ID, ticket.getId());`
  - id 行 L104: `ticket.getId(), e.getMessage());`
  - id 行 L162: `protected ErpQaNonConformance createNcr(ErpCsTicket ticket, Long materialId, String defectDescription,`
  - id 行 L164: `Long supplierId, IServiceContext context) {`
  - id 行 L213: `static String buildPendingContent(Long materialId, String defectDescription, String batchInfo,`
  - id 行 L214: `BigDecimal quantity, String severity, Long supplierId) {`
  - id 行 L308: `action.setTicketId(ticket.getId());`
  - id 行 L313: `action.setOperatorId(context.getUserId());`
  - id 行 L326: `.param(ErpCsErrors.ARG_TICKET_ID, ticket.getId())`

### module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsTicketResolveProcessor.java
- 外域: notify  |  本文件 id-as-Long 证据行: 8
- 耦合点 L12: IErpSysNotificationBiz (notify)
  - id 行 L52: `public ErpCsTicket resolve(Long ticketId, String resolution, IServiceContext context) {`
  - id 行 L99: `if (hasAdoptKnowledgeAction(ticket.getId(), context)) {`
  - id 行 L104: `ctx.put("ticketId", ticket.getId());`
  - id 行 L111: `ticket.getId(), e.getMessage());`
  - id 行 L115: `private boolean hasAdoptKnowledgeAction(Long ticketId, IServiceContext context) {`
  - id 行 L123: `private ErpCsTicket requireTicket(Long ticketId, IServiceContext context) {`
  - id 行 L144: `action.setTicketId(ticket.getId());`
  - id 行 L149: `action.setOperatorId(context.getUserId());`

### module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsTicketScanOverdueTicketsProcessor.java
- 外域: master-data, notify  |  本文件 id-as-Long 证据行: 8
- 耦合点 L9: IErpMdPartnerBiz (master-data)
- 耦合点 L10: ErpMdPartner (master-data)
- 耦合点 L11: IErpSysNotificationBiz (notify)
  - id 行 L79: `LOG.warn("sla-escalation-failed: ticketId={}, reason={}", ticket.getId(), ex.getMessage());`
  - id 行 L140: `ticket.getId(), level);`
  - id 行 L149: `LOG.warn("sla-escalation-l1-target-unresolved-notify-degraded: ticketId={}", ticket.getId());`
  - id 行 L203: `ctx.put("ticketId", ticket.getId());`
  - id 行 L220: `ticket.getId(), e.getMessage());`
  - id 行 L224: `private String resolveCustomerName(Long customerId, IServiceContext context) {`
  - id 行 L239: `action.setTicketId(ticket.getId());`
  - id 行 L244: `action.setOperatorId(context.getUserId());`

## module-drp （引用外域的耦合文件 6 个）

### module-drp/erp-drp-service/src/main/java/app/erp/drp/service/drp/DrpDemandAggregator.java
- 外域: inventory, manufacturing, purchase  |  本文件 id-as-Long 证据行: 12
- 耦合点 L8: ErpInvStockBalance (inventory)
- 耦合点 L9: ErpInvTransferOrder (inventory)
- 耦合点 L10: ErpInvTransferOrderLine (inventory)
- 耦合点 L11: ErpMfgForecast (manufacturing)
- 耦合点 L12: ErpMfgForecastLine (manufacturing)
- 耦合点 L13: ErpPurOrder (purchase)
- 耦合点 L14: ErpPurOrderLine (purchase)
- 耦合点 L125: ErpMfgForecast.class (daoFor) (manufacturing)
- 耦合点 L140: ErpMfgForecastLine.class (daoFor) (manufacturing)
- 耦合点 L180: ErpInvStockBalance.class (daoFor) (inventory)
- 耦合点 L195: ErpInvStockBalance.class (daoFor) (inventory)
- 耦合点 L209: ErpInvTransferOrder.class (daoFor) (inventory)
- 耦合点 L210: ErpInvTransferOrderLine.class (daoFor) (inventory)
- 耦合点 L236: ErpPurOrder.class (daoFor) (purchase)
- 耦合点 L237: ErpPurOrderLine.class (daoFor) (purchase)
  - id 行 L75: `public List<AggregatedDemand> aggregate(Long planId) {`
  - id 行 L137: `headIds.add(h.getId());`
  - id 行 L163: `private static String forecastKey(Long materialId, Long warehouseId) {`
  - id 行 L175: `private BigDecimal sumAvailable(Long materialId, Long warehouseId) {`
  - id 行 L190: `private BigDecimal sumReserved(Long materialId, Long warehouseId) {`
  - id 行 L201: `private BigDecimal onOrderQty(Long materialId, Long warehouseId) {`
  - id 行 L208: `private BigDecimal inboundTransferQty(Long materialId, Long warehouseId) {`
  - id 行 L222: `orderIds.add(o.getId());`
  - id 行 L223: `byId.put(o.getId(), o);`
  - id 行 L235: `private BigDecimal unreceivedPurchaseQty(Long materialId, Long warehouseId) {`
  - id 行 L247: `orderIds.add(o.getId());`
  - id 行 L262: `private ErpDrpPlan requirePlan(Long planId) {`

### module-drp/erp-drp-service/src/main/java/app/erp/drp/service/drp/DrpReleaseService.java
- 外域: inventory, master-data, purchase  |  本文件 id-as-Long 证据行: 24
- 耦合点 L10: ErpInvTransferOrder (inventory)
- 耦合点 L11: ErpInvTransferOrderLine (inventory)
- 耦合点 L12: ErpMdCurrency (master-data)
- 耦合点 L13: ErpMdMaterial (master-data)
- 耦合点 L14: ErpPurOrder (purchase)
- 耦合点 L15: ErpPurOrderLine (purchase)
- 耦合点 L188: ErpInvTransferOrder.class (daoFor) (inventory)
- 耦合点 L200: ErpInvTransferOrderLine.class (daoFor) (inventory)
- 耦合点 L212: ErpPurOrder.class (daoFor) (purchase)
- 耦合点 L225: ErpPurOrderLine.class (daoFor) (purchase)
- 耦合点 L242: ErpMdMaterial.class (daoFor) (master-data)
- 耦合点 L254: ErpMdCurrency.class (daoFor) (master-data)
  - id 行 L67: `public String releaseLine(Long lineId) {`
  - id 行 L105: `public int releaseApproved(Long planId) {`
  - id 行 L112: `releaseLine(line.getId());`
  - id 行 L121: `private void advancePlanToExecutedIfComplete(Long planId) {`
  - id 行 L146: `private ErpDrpLine requireReleasable(Long lineId) {`
  - id 行 L187: `private String releaseToTransferOrder(ErpDrpLine line, Long sourceWarehouseId, LocalDate today) {`
  - id 行 L190: `String code = ErpDrpConstants.RELEASE_TO_CODE_PREFIX + "TO-" + line.getId();`
  - id 行 L192: `order.setOrgId(line.getOrgId());`
  - id 行 L194: `order.setFromWarehouseId(sourceWarehouseId);`
  - id 行 L195: `order.setToWarehouseId(line.getWarehouseId());`
  - id 行 L202: `toLine.setTransferId(order.getId());`
  - id 行 L204: `toLine.setMaterialId(line.getMaterialId());`
  - id 行 L205: `toLine.setUoMId(resolveUoM(line.getMaterialId()));`
  - id 行 L211: `private String releaseToPurchaseOrder(ErpDrpLine line, Long supplierId, LocalDate today) {`
  - id 行 L214: `String code = ErpDrpConstants.RELEASE_TO_CODE_PREFIX + "PO-" + line.getId();`
  - id 行 L216: `order.setOrgId(line.getOrgId());`
  - id 行 L217: `order.setSupplierId(supplierId);`
  - id 行 L220: `order.setCurrencyId(resolveDefaultCurrencyId());`
  - id 行 L227: `poLine.setOrderId(order.getId());`
  - id 行 L229: `poLine.setMaterialId(line.getMaterialId());`
  - id 行 L230: `poLine.setUoMId(resolveUoM(line.getMaterialId()));`
  - id 行 L238: `private Long resolveUoM(Long materialId) {`
  - id 行 L250: `private Long resolveDefaultCurrencyId() {`
  - id 行 L255: `return list.isEmpty() ? null : list.get(0).getId();`

### module-drp/erp-drp-service/src/main/java/app/erp/drp/service/job/ErpDrpCrossDockStagingTimeoutJob.java
- 外域: inventory, master-data  |  本文件 id-as-Long 证据行: 13
- 耦合点 L7: IErpInvStockMoveBiz (inventory)
- 耦合点 L8: StockMoveLineRequest (inventory)
- 耦合点 L9: StockMoveRequest (inventory)
- 耦合点 L10: ErpInvStockMove (inventory)
- 耦合点 L11: ErpMdLocation (master-data)
- 耦合点 L12: ErpMdMaterial (master-data)
- 耦合点 L180: ErpMdLocation.class (daoFor) (master-data)
- 耦合点 L186: ErpInvStockMove.class (daoFor) (inventory)
- 耦合点 L198: ErpMdMaterial.class (daoFor) (master-data)
  - id 行 L130: `dock.getId(), e.getMessage());`
  - id 行 L145: `Long stagingWarehouseId = resolveStagingWarehouseId(dock);`
  - id 行 L147: `LOG.warn("erp-drp-xdock-staging-timeout: 无法解析暂存仓库，跳过：crossDockId={}", dock.getId());`
  - id 行 L152: `request.setOrgId(dock.getOrgId());`
  - id 行 L154: `request.setSourceWarehouseId(stagingWarehouseId);`
  - id 行 L155: `request.setSourceLocationId(dock.getStagingLocationId());`
  - id 行 L156: `request.setDestWarehouseId(stagingWarehouseId);`
  - id 行 L161: `line.setMaterialId(dock.getMaterialId());`
  - id 行 L162: `line.setUoMId(resolveMaterialUomId(dock.getMaterialId()));`
  - id 行 L164: `line.setSourceLocationId(dock.getStagingLocationId());`
  - id 行 L169: `ErpInvDrpCrossDock fresh = daoProvider.daoFor(ErpInvDrpCrossDock.class).getEntityById(dock.getId());`
  - id 行 L178: `protected Long resolveStagingWarehouseId(ErpInvDrpCrossDock dock) {`
  - id 行 L194: `protected Long resolveMaterialUomId(Long materialId) {`

### module-drp/erp-drp-service/src/main/java/app/erp/drp/service/processor/ErpInvDrpCrossDockProcessor.java
- 外域: inventory, master-data, quality, sales  |  本文件 id-as-Long 证据行: 14
- 耦合点 L8: IErpInvStockMoveBiz (inventory)
- 耦合点 L9: StockMoveLineRequest (inventory)
- 耦合点 L10: StockMoveRequest (inventory)
- 耦合点 L11: ErpInvStockMove (inventory)
- 耦合点 L12: ErpMdLocation (master-data)
- 耦合点 L13: ErpMdMaterial (master-data)
- 耦合点 L14: IErpQaInspectionBiz (quality)
- 耦合点 L15: IErpQaInspectionTemplateBiz (quality)
- 耦合点 L16: ErpQaInspection (quality)
- 耦合点 L17: ErpQaInspectionTemplate (quality)
- 耦合点 L18: IErpSalOrderBiz (sales)
- 耦合点 L19: IErpSalOrderLineBiz (sales)
- 耦合点 L20: ErpSalOrder (sales)
- 耦合点 L21: ErpSalOrderLine (sales)
- 耦合点 L401: ErpMdLocation.class (daoFor) (master-data)
- 耦合点 L408: ErpInvStockMove.class (daoFor) (inventory)
- 耦合点 L423: ErpMdMaterial.class (daoFor) (master-data)
  - id 行 L113: `public ErpInvDrpCrossDock receiveMark(Long id, Long inboundMoveId, IServiceContext context) {`
  - id 行 L168: `public int markReceivedFromPurchase(String purchaseOrderCode, Long inboundMoveId, List<Long> materialIds,`
  - id 行 L201: `protected void doReceiveMark(ErpInvDrpCrossDock dock, Long inboundMoveId, IServiceContext context) {`
  - id 行 L203: `dock.setInboundMoveId(inboundMoveId);`
  - id 行 L219: `dock.setOutboundMoveId(outboundMove != null ? outboundMove.getId() : null);`
  - id 行 L345: `protected boolean requiresQualityInspection(Long materialId, IServiceContext context) {`
  - id 行 L380: `request.setOrgId(dock.getOrgId());`
  - id 行 L382: `request.setSourceWarehouseId(resolveStagingWarehouseId(dock));`
  - id 行 L383: `request.setSourceLocationId(dock.getStagingLocationId());`
  - id 行 L388: `line.setMaterialId(dock.getMaterialId());`
  - id 行 L389: `line.setUoMId(resolveMaterialUomId(dock.getMaterialId()));`
  - id 行 L391: `line.setSourceLocationId(dock.getStagingLocationId());`
  - id 行 L399: `protected Long resolveStagingWarehouseId(ErpInvDrpCrossDock dock) {`
  - id 行 L419: `protected Long resolveMaterialUomId(Long materialId) {`

### module-drp/erp-drp-service/src/main/java/app/erp/drp/service/processor/ErpInvDrpLeadTimeProcessor.java
- 外域: purchase, quality  |  本文件 id-as-Long 证据行: 18
- 耦合点 L10: IErpPurOrderBiz (purchase)
- 耦合点 L11: ErpPurOrder (purchase)
- 耦合点 L12: ErpPurOrderLine (purchase)
- 耦合点 L13: IErpQaInspectionBiz (quality)
- 耦合点 L14: ErpQaInspection (quality)
  - id 行 L87: `public int recordFromPurchaseReceive(String purchaseOrderCode, Long supplierId, LocalDate orderDate,`
  - id 行 L101: `for (Long materialId : new LinkedHashSet<>(materialIds)) {`
  - id 行 L111: `public LeadTimeStatsBean findLeadTimeStats(Long supplierId, Long materialId, IServiceContext context) {`
  - id 行 L118: `public ErpInvDrpSupplierScore recalculateLeadTimeStats(Long supplierId, Long materialId,`
  - id 行 L135: `protected int createRecord(String purchaseOrderCode, Long supplierId, Long materialId,`
  - id 行 L139: `record.setSupplierId(supplierId);`
  - id 行 L140: `record.setMaterialId(materialId);`
  - id 行 L177: `protected boolean existsRecord(String purchaseOrderCode, Long materialId) {`
  - id 行 L187: `protected List<ErpInvDrpLeadTimeRecord> loadRecords(Long supplierId, Long materialId) {`
  - id 行 L209: `protected LeadTimeStatsBean computeStats(List<ErpInvDrpLeadTimeRecord> records, Long supplierId,`
  - id 行 L210: `Long materialId) {`
  - id 行 L212: `stats.setSupplierId(supplierId);`
  - id 行 L213: `stats.setMaterialId(materialId);`
  - id 行 L301: `score.setSupplierId(stats.getSupplierId());`
  - id 行 L302: `score.setMaterialId(stats.getMaterialId());`
  - id 行 L344: `protected BigDecimal computeQuantityAccuracy(Long supplierId, Long materialId, IServiceContext context) {`
  - id 行 L382: `protected BigDecimal computeQualityPassRate(Long supplierId, Long materialId, IServiceContext context) {`
  - id 行 L410: `protected ErpInvDrpSupplierScore findScore(Long supplierId, Long materialId) {`

### module-drp/erp-drp-service/src/main/java/app/erp/drp/service/safetystock/SafetyStockEngine.java
- 外域: inventory  |  本文件 id-as-Long 证据行: 10
- 耦合点 L9: ErpInvStockMove (inventory)
- 耦合点 L10: ErpInvStockMoveLine (inventory)
- 耦合点 L274: ErpInvStockMove.class (daoFor) (inventory)
- 耦合点 L275: ErpInvStockMoveLine.class (daoFor) (inventory)
  - id 行 L85: `public ErpInvDrpSafetyStockCalc calculate(Long calcId) {`
  - id 行 L167: `public BigDecimal findEffectiveSafetyStock(Long materialId, Long warehouseId, Long orgId) {`
  - id 行 L192: `public BigDecimal findEffectiveSafetyStockByParameterId(Long parameterId) {`
  - id 行 L205: `public void confirmWriteback(Long calcId) {`
  - id 行 L238: `Long supplierId = param != null ? param.getPreferredSupplierId() : null;`
  - id 行 L273: `private List<BigDecimal> monthlyDemands(Long materialId, Long warehouseId, int historyMonths) {`
  - id 行 L295: `moveIds.add(m.getId());`
  - id 行 L296: `moveById.put(m.getId(), m);`
  - id 行 L366: `private ErpDrpParameter findParameter(Long materialId, Long warehouseId, Long orgId) {`
  - id 行 L378: `private ErpInvDrpSafetyStockCalc requireCalc(Long calcId) {`

## module-finance （引用外域的耦合文件 35 个）

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/annualclose/AnnualCloseService.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 35
- 耦合点 L9: ErpMdCurrency (master-data)
- 耦合点 L10: ErpMdSubject (master-data)
- 耦合点 L373: ErpMdSubject.class (daoFor) (master-data)
- 耦合点 L402: ErpMdCurrency.class (daoFor) (master-data)
  - id 行 L76: `Long primarySchemaId = resolveAcctSchemaId(period.getId());`
  - id 行 L78: `Long lastVoucherId = null;`
  - id 行 L79: `for (Long schemaId : schemas) {`
  - id 行 L85: `private Long executeAnnualCloseForSchema(ErpFinAccountingPeriod period, Long acctSchemaId, IServiceContext context) {`
  - id 行 L86: `Long voucherId = transferProfitToRetainedEarnings(period, acctSchemaId);`
  - id 行 L95: `public Long transferProfitToRetainedEarnings(ErpFinAccountingPeriod period, Long acctSchemaId) {`
  - id 行 L102: `BigDecimal cypNet = subjectNetForYear(cypSubject.getId(), year);`
  - id 行 L113: `lines.add(new Line(cypSubject.getId(), cypSubject.getCode(), cypSubject.getName(),`
  - id 行 L115: `lines.add(new Line(retainedSubject.getId(), retainedSubject.getCode(), retainedSubject.getName(),`
  - id 行 L119: `lines.add(new Line(retainedSubject.getId(), retainedSubject.getCode(), retainedSubject.getName(),`
  - id 行 L121: `lines.add(new Line(cypSubject.getId(), cypSubject.getCode(), cypSubject.getName(),`
  - id 行 L125: `Long functionalCurrencyId = resolveFunctionalCurrencyId();`
  - id 行 L129: `period.getOrgId(), acctSchemaId, period.getId(), functionalCurrencyId, BigDecimal.ONE,`
  - id 行 L138: `public void populateNextYearOpening(ErpFinAccountingPeriod period, Long acctSchemaId) {`
  - id 行 L148: `Long functionalCurrencyId = resolveFunctionalCurrencyId();`
  - id 行 L158: `clearQ.addFilter(eq("periodId", nextJan.getId()));`
  - id 行 L169: `gl.setOrgId(period.getOrgId());`
  - id 行 L170: `gl.setAcctSchemaId(acctSchemaId);`
  - id 行 L171: `gl.setPeriodId(nextJan.getId());`
  - id 行 L172: `gl.setSubjectId(a.subjectId);`
  - id 行 L173: `gl.setCurrencyId(functionalCurrencyId);`
  - id 行 L218: `arGl = subjectNetForYear(arSubject.getId(), year).negate().max(BigDecimal.ZERO);`
  - id 行 L221: `apGl = subjectNetForYear(apSubject.getId(), year).max(BigDecimal.ZERO);`
  - id 行 L277: `private BigDecimal subjectNetForYear(Long subjectId, int year) {`
  - id 行 L327: `if (p.getId() != null) {`
  - id 行 L328: `periodIds.add(p.getId());`
  - id 行 L345: `ids.add(v.getId());`
  - id 行 L350: `private ErpFinAccountingPeriod findNextYearJanuaryPeriod(int nextYear, Long orgId) {`
  - id 行 L381: `private Long resolveAcctSchemaId(Long periodId) {`
  - id 行 L383: `Long orgId = period != null ? period.getOrgId() : null;`
  - id 行 L385: `Long schemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);`
  - id 行 L401: `private Long resolveFunctionalCurrencyId() {`
  - id 行 L408: `return list.get(0).getId();`
  - id 行 L414: `final Long subjectId;`
  - id 行 L418: `SubjectYearAgg(Long subjectId) {`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/baddebt/BadDebtProvisionService.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 23
- 耦合点 L12: ErpMdCurrency (master-data)
- 耦合点 L13: ErpMdSubject (master-data)
- 耦合点 L330: ErpMdSubject.class (daoFor) (master-data)
- 耦合点 L367: ErpMdCurrency.class (daoFor) (master-data)
  - id 行 L85: `public BadDebtProvisionResult runBadDebtProvision(Long periodId, IServiceContext context) {`
  - id 行 L87: `Long primarySchemaId = resolveAcctSchemaId(periodId);`
  - id 行 L90: `for (Long schemaId : schemas) {`
  - id 行 L116: `public BadDebtProvisionReversalResult reverseBadDebtProvision(Long periodId, IServiceContext context) {`
  - id 行 L148: `result.setPeriodId(periodId);`
  - id 行 L193: `private BadDebtProvisionResult runBadDebtProvisionForSchema(ErpFinAccountingPeriod period, Long acctSchemaId, IServiceContext context) {`
  - id 行 L204: `new Line(expense.getId(), expense.getCode(), expense.getName(),`
  - id 行 L206: `new Line(allowance.getId(), allowance.getCode(), allowance.getName(),`
  - id 行 L208: `Long voucherId = CloseVoucherWriter.writeVoucher(daoProvider, "BDR",`
  - id 行 L211: `period.getOrgId(), acctSchemaId, period.getId(),`
  - id 行 L214: `result.setVoucherId(voucherId);`
  - id 行 L220: `new Line(allowance.getId(), allowance.getCode(), allowance.getName(),`
  - id 行 L222: `new Line(expense.getId(), expense.getCode(), expense.getName(),`
  - id 行 L224: `Long voucherId = CloseVoucherWriter.writeVoucher(daoProvider, "BDL",`
  - id 行 L227: `period.getOrgId(), acctSchemaId, period.getId(),`
  - id 行 L230: `result.setVoucherId(voucherId);`
  - id 行 L262: `q.addFilter(eq("subjectId", allowance.getId()));`
  - id 行 L338: `protected ErpFinAccountingPeriod requirePeriod(Long periodId) {`
  - id 行 L346: `protected Long resolveAcctSchemaId(Long periodId) {`
  - id 行 L348: `Long orgId = period != null ? period.getOrgId() : null;`
  - id 行 L350: `Long schemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);`
  - id 行 L366: `protected Long resolveFunctionalCurrencyId() {`
  - id 行 L372: `return list.isEmpty() ? 1L : list.get(0).getId();`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/bankrecon/BankReconAdjustmentVoucherBuilder.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 9
- 耦合点 L124: ErpMdSubject (master-data)
  - id 行 L78: `Long acctSchemaId = resolveAcctSchemaId(recon, fundAccount);`
  - id 行 L83: `event.setAcctSchemaId(acctSchemaId);`
  - id 行 L84: `event.setOrgId(recon.getOrgId());`
  - id 行 L85: `event.setCurrencyId(fundAccount.getCurrencyId());`
  - id 行 L121: `.param(ErpFinErrors.ARG_FUND_ACCOUNT_ID, fundAccount.getId())`
  - id 行 L127: `.param(ErpFinErrors.ARG_FUND_ACCOUNT_ID, fundAccount.getId());`
  - id 行 L141: `protected Long resolveAcctSchemaId(ErpFinBankReconciliation recon, ErpFinFundAccount fundAccount) {`
  - id 行 L142: `Long orgId = fundAccount != null ? fundAccount.getOrgId() : null;`
  - id 行 L146: `Long schemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/budget/BudgetVoucherGenerator.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 36
- 耦合点 L9: ErpMdSubject (master-data)
- 耦合点 L220: ErpMdSubject.class (daoFor) (master-data)
  - id 行 L54: `List<ErpFinBudgetLine> lines = loadBudgetLines(scenario.getId());`
  - id 行 L67: `Long voucherId = writeBudgetVoucher(scenario, e.getKey(), e.getValue(), false, null);`
  - id 行 L86: `List<ErpFinVoucherLine> origLines = loadVoucherLines(original.getId());`
  - id 行 L87: `Long reversalId = writeBudgetVoucher(scenario, original.getPeriodId(), origLines, true, original.getId());`
  - id 行 L97: `private Long writeBudgetVoucher(ErpFinBudgetScenario scenario, Long periodId, List<?> rawLines,`
  - id 行 L98: `boolean isReversal, Long reversalOfVoucherId) {`
  - id 行 L128: `voucher.setOrgId(scenario.getOrgId());`
  - id 行 L129: `voucher.setAcctSchemaId(scenario.getAcctSchemaId());`
  - id 行 L130: `voucher.setPeriodId(periodId);`
  - id 行 L135: `voucher.setReversalOfVoucherId(reversalOfVoucherId);`
  - id 行 L140: `Long voucherId = voucher.getId();`
  - id 行 L145: `line.setVoucherId(voucherId);`
  - id 行 L147: `line.setSubjectId(f.subjectId);`
  - id 行 L154: `line.setCurrencyId(scenario.getCurrencyId());`
  - id 行 L159: `line.setAcctSchemaId(scenario.getAcctSchemaId());`
  - id 行 L160: `line.setOrgId(scenario.getOrgId());`
  - id 行 L163: `line.setPartnerId(f.partnerId);`
  - id 行 L164: `line.setDepartmentId(f.departmentId);`
  - id 行 L165: `line.setProjectId(f.projectId);`
  - id 行 L166: `line.setWarehouseId(f.warehouseId);`
  - id 行 L167: `line.setMaterialId(f.materialId);`
  - id 行 L168: `line.setCostCenterId(f.costCenterId);`
  - id 行 L173: `billR.setVoucherId(voucherId);`
  - id 行 L190: `return new VoucherFact(subject.getId(), subject.getCode(), subject.getName(),`
  - id 行 L223: `private List<ErpFinBudgetLine> loadBudgetLines(Long scenarioId) {`
  - id 行 L230: `private List<ErpFinVoucherLine> loadVoucherLines(Long voucherId) {`
  - id 行 L258: `final Long subjectId;`
  - id 行 L263: `final Long costCenterId;`
  - id 行 L264: `final Long projectId;`
  - id 行 L265: `final Long partnerId;`
  - id 行 L266: `final Long departmentId;`
  - id 行 L267: `final Long warehouseId;`
  - id 行 L268: `final Long materialId;`
  - id 行 L270: `VoucherFact(Long subjectId, String subjectCode, String subjectName, String dcDirection,`
  - id 行 L271: `BigDecimal amount, Long costCenterId, Long projectId, Long partnerId,`
  - id 行 L272: `Long departmentId, Long warehouseId, Long materialId) {`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/budget/CommitmentVoucherGenerator.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 35
- 耦合点 L7: ErpMdSubject (master-data)
  - id 行 L61: `public Long generateCommitment(String sourceBillType, String sourceBillCode, ErpMdSubject subject, Long costCenterId,`
  - id 行 L62: `Long orgId, Long acctSchemaId, Long periodId, Long currencyId,`
  - id 行 L67: `return voucher != null ? voucher.getId() : null;`
  - id 行 L85: `List<ErpFinVoucherLine> origLines = loadVoucherLines(original.getId());`
  - id 行 L90: `reversalIds.add(reversal.getId());`
  - id 行 L119: `private ErpFinVoucher writeCommitmentVoucher(String sourceBillCode, String billType, ErpMdSubject subject, Long costCenterId,`
  - id 行 L120: `Long orgId, Long acctSchemaId, Long periodId, Long currencyId,`
  - id 行 L121: `BigDecimal amount, boolean isReversal, Long reversalOfVoucherId) {`
  - id 行 L137: `voucher.setOrgId(orgId);`
  - id 行 L138: `voucher.setAcctSchemaId(acctSchemaId);`
  - id 行 L139: `voucher.setPeriodId(periodId);`
  - id 行 L147: `voucher.setReversalOfVoucherId(reversalOfVoucherId);`
  - id 行 L152: `Long voucherId = voucher.getId();`
  - id 行 L155: `line.setVoucherId(voucherId);`
  - id 行 L157: `line.setSubjectId(subject.getId());`
  - id 行 L163: `line.setCurrencyId(currencyId);`
  - id 行 L167: `line.setAcctSchemaId(acctSchemaId);`
  - id 行 L168: `line.setOrgId(orgId);`
  - id 行 L171: `line.setCostCenterId(costCenterId);`
  - id 行 L175: `billR.setVoucherId(voucherId);`
  - id 行 L210: `reversal.setOrgId(original.getOrgId());`
  - id 行 L211: `reversal.setAcctSchemaId(original.getAcctSchemaId());`
  - id 行 L212: `reversal.setPeriodId(original.getPeriodId());`
  - id 行 L216: `reversal.setReversalOfVoucherId(original.getId());`
  - id 行 L220: `Long reversalId = reversal.getId();`
  - id 行 L227: `line.setVoucherId(reversalId);`
  - id 行 L229: `line.setSubjectId(ol.getSubjectId());`
  - id 行 L235: `line.setCurrencyId(ol.getCurrencyId());`
  - id 行 L239: `line.setAcctSchemaId(ol.getAcctSchemaId());`
  - id 行 L240: `line.setOrgId(ol.getOrgId());`
  - id 行 L243: `line.setCostCenterId(ol.getCostCenterId());`
  - id 行 L248: `billR.setVoucherId(reversalId);`
  - id 行 L250: `billR.setBillCode(findOriginalBillCode(original.getId(), billType));`
  - id 行 L257: `private String findOriginalBillCode(Long voucherId, String billType) {`
  - id 行 L273: `private List<ErpFinVoucherLine> loadVoucherLines(Long voucherId) {`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/budget/ErpFinBudgetCommitmentBizModel.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 11
- 耦合点 L8: ErpMdSubject (master-data)
- 耦合点 L122: ErpMdSubject.class (daoFor) (master-data)
- 耦合点 L131: ErpMdSubject.class (daoFor) (master-data)
  - id 行 L55: `public Long commit(String sourceBillType, String sourceBillCode, Long subjectId, Long costCenterId,`
  - id 行 L56: `Long periodId, BigDecimal amount, IServiceContext context) {`
  - id 行 L68: `Long currencyId = resolveCurrencyId(periodId);`
  - id 行 L70: `Long orgId = orgSchema[0] != null ? orgSchema[0] : 1L;`
  - id 行 L71: `Long acctSchemaId = orgSchema[1] != null ? orgSchema[1] : 1L;`
  - id 行 L73: `Long voucherId = commitmentVoucherGenerator.generateCommitment(sourceBillType, sourceBillCode, subject, costCenterId,`
  - id 行 L121: `private ErpMdSubject loadSubject(Long subjectId) {`
  - id 行 L140: `public Long resolvePeriodId(LocalDate businessDate) {`
  - id 行 L150: `return list.isEmpty() ? null : list.get(0).getId();`
  - id 行 L153: `private Long resolveCurrencyId(Long periodId) {`
  - id 行 L164: `private Long[] resolveOrgAndSchema(Long periodId) {`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/budget/ErpFinBudgetControlBiz.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 14
- 耦合点 L12: ErpMdSubject (master-data)
- 耦合点 L231: ErpMdSubject.class (daoFor) (master-data)
  - id 行 L62: `public BudgetCheckResult check(Long subjectId, Long costCenterId, Long periodId, BigDecimal amount,`
  - id 行 L84: `return new BudgetCheckResult(BudgetCheckResult.ACTION_PASS, available, match.line.getId());`
  - id 行 L101: `return new BudgetCheckResult(BudgetCheckResult.ACTION_WARNED, available, match.line.getId());`
  - id 行 L103: `return new BudgetCheckResult(BudgetCheckResult.ACTION_PASS, available, match.line.getId());`
  - id 行 L117: `private BigDecimal aggregateAmount(Long periodId, Long subjectId, Long costCenterId, String direction, AmountChannel channel) {`
  - id 行 L180: `private BudgetLineMatch findMatchingBudgetLine(Long subjectId, Long costCenterId, Long periodId) {`
  - id 行 L199: `private void writeControlLog(BudgetLineMatch match, Long periodId, String sourceBillType, String sourceBillCode,`
  - id 行 L203: `logEntry.setOrgId(match.scenario.getOrgId());`
  - id 行 L205: `logEntry.setScenarioId(match.scenario.getId());`
  - id 行 L206: `logEntry.setBudgetLineId(match.line.getId());`
  - id 行 L209: `logEntry.setSubjectId(match.line.getSubjectId());`
  - id 行 L210: `logEntry.setCostCenterId(match.line.getCostCenterId());`
  - id 行 L211: `logEntry.setPeriodId(periodId);`
  - id 行 L218: `logEntry.setOperatorId(context.getUserContext().getUserId());`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/dashboard/ErpFinDashboardBizModel.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 14
- 耦合点 L9: ErpMdSubject (master-data)
  - id 行 L57: `public Map<String, Object> getDashboardKpi(@Optional @Name("periodId") Long periodId,`
  - id 行 L161: `private List<ErpFinGlBalance> loadGlBalances(Long periodId) {`
  - id 行 L166: `Long latestPeriodId = findLatestPeriodId();`
  - id 行 L181: `private Long findLatestPeriodId() {`
  - id 行 L187: `return latest.isEmpty() ? null : latest.get(0).getId();`
  - id 行 L198: `for (ErpFinAccountingPeriod p : periods) periodIds.add(p.getId());`
  - id 行 L203: `Long orgId = periods.get(0).getOrgId();`
  - id 行 L206: `Long schemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);`
  - id 行 L226: `private BigDecimal sumArApOpen(String direction, Long periodId) {`
  - id 行 L245: `private Long resolvePeriodOrgId(Long periodId) {`
  - id 行 L247: `Long latestPeriodId = findLatestPeriodId();`
  - id 行 L257: `private void applyOrgAndSchemaScope(QueryBean q, Long periodId) {`
  - id 行 L258: `Long orgId = resolvePeriodOrgId(periodId);`
  - id 行 L263: `Long schemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinBudgetLineBizModel.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 7
- 耦合点 L10: ErpMdSubject (master-data)
- 耦合点 L142: ErpMdSubject.class (daoFor) (master-data)
  - id 行 L56: `public List<BudgetVsActualRow> getBudgetVsActual(@Name("acctSchemaId") Long acctSchemaId,`
  - id 行 L57: `@Name("periodId") Long periodId,`
  - id 行 L58: `@Name("subjectId") Long subjectId,`
  - id 行 L81: `voucherChannel.put(v.getId(), channelOf(v.getPostingType()));`
  - id 行 L133: `row.setSubjectId(l.getSubjectId());`
  - id 行 L136: `row.setCostCenterId(l.getCostCenterId());`
  - id 行 L137: `row.setProjectId(l.getProjectId());`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinPostingExceptionBizModel.java
- 外域: notify  |  本文件 id-as-Long 证据行: 12
- 耦合点 L18: IErpSysNotificationBiz (notify)
  - id 行 L140: `public ErpFinPostingException retry(@Name("exceptionId") Long exceptionId, IServiceContext context) {`
  - id 行 L146: `public ErpFinPostingException ignore(@Name("exceptionId") Long exceptionId,`
  - id 行 L161: `ctx.put("exceptionId", entity.getId());`
  - id 行 L173: `entity.getId(), e.getMessage());`
  - id 行 L179: `public ErpFinPostingException manualEntry(@Name("exceptionId") Long exceptionId,`
  - id 行 L180: `@Name("voucherId") Long voucherId,`
  - id 行 L190: `entity.setVoucherId(voucherId);`
  - id 行 L321: `private ErpFinPostingException requirePending(Long exceptionId) {`
  - id 行 L339: `event.setTraceId(entity.getTraceId());`
  - id 行 L343: `event.setOrgId(entity.getOrgId());`
  - id 行 L344: `event.setAcctSchemaId(entity.getAcctSchemaId());`
  - id 行 L345: `event.setCurrencyId(entity.getCurrencyId());`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/fx/ExchangeRevaluationService.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 24
- 耦合点 L10: ErpMdCurrency (master-data)
- 耦合点 L11: ErpMdSubject (master-data)
- 耦合点 L261: ErpMdSubject.class (daoFor) (master-data)
- 耦合点 L270: ErpMdSubject.class (daoFor) (master-data)
- 耦合点 L283: ErpMdCurrency.class (daoFor) (master-data)
  - id 行 L70: `Long primarySchemaId = resolveAcctSchemaId(period.getId());`
  - id 行 L72: `Long lastVoucherId = null;`
  - id 行 L73: `for (Long schemaId : schemas) {`
  - id 行 L79: `private Long revalueForSchema(ErpFinAccountingPeriod period, Long acctSchemaId, IServiceContext context) {`
  - id 行 L80: `Long functionalCurrencyId = resolveFunctionalCurrencyId();`
  - id 行 L83: `Long arApVoucherId = revalueArAp(period, functionalCurrencyId, null, acctSchemaId);`
  - id 行 L84: `Long bankVoucherId = null;`
  - id 行 L103: `private Long revalueArAp(ErpFinAccountingPeriod period, Long functionalCurrencyId, BigDecimal periodEndRate, Long acctSchemaId) {`
  - id 行 L143: `lines.add(new Line(counterpartSubject.getId(), counterpartSubject.getCode(), counterpartSubject.getName(),`
  - id 行 L145: `lines.add(new Line(fxSubject.getId(), fxSubject.getCode(), fxSubject.getName(), fxDc, abs, null));`
  - id 行 L153: `period.getOrgId(), acctSchemaId, period.getId(), functionalCurrencyId, BigDecimal.ONE,`
  - id 行 L162: `private Long revalueBankDeposits(ErpFinAccountingPeriod period, Long functionalCurrencyId,`
  - id 行 L163: `BigDecimal periodEndRate, Long acctSchemaId) {`
  - id 行 L179: `Map<Long, BigDecimal> bookBySubject = aggregateBankSubjectBookFunctional(period.getId());`
  - id 行 L205: `lines.add(new Line(bankSubject.getId(), bankSubject.getCode(), bankSubject.getName(),`
  - id 行 L207: `lines.add(new Line(fxSubject.getId(), fxSubject.getCode(), fxSubject.getName(), fxDc, abs, null));`
  - id 行 L214: `period.getOrgId(), acctSchemaId, period.getId(), functionalCurrencyId, BigDecimal.ONE,`
  - id 行 L219: `private Map<Long, BigDecimal> aggregateBankSubjectBookFunctional(Long periodId) {`
  - id 行 L232: `voucherIds.add(v.getId());`
  - id 行 L282: `private Long resolveFunctionalCurrencyId() {`
  - id 行 L288: `return list.isEmpty() ? null : list.get(0).getId();`
  - id 行 L291: `private Long resolveAcctSchemaId(Long periodId) {`
  - id 行 L293: `Long orgId = period != null ? period.getOrgId() : null;`
  - id 行 L295: `Long schemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/intercompany/ErpFinIntercompanyTransferBizModel.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 22
- 耦合点 L7: ErpMdOrganization (master-data)
- 耦合点 L8: ErpMdWarehouse (master-data)
- 耦合点 L199: ErpMdWarehouse.class (daoFor) (master-data)
- 耦合点 L210: ErpMdOrganization.class (daoFor) (master-data)
  - id 行 L59: `public List<Long> onTransferConfirmed(Long transferOrderId, Long fromWarehouseId, Long toWarehouseId,`
  - id 行 L68: `Long fromOrgId = resolveWarehouseOrgId(fromWarehouseId);`
  - id 行 L69: `Long toOrgId = resolveWarehouseOrgId(toWarehouseId);`
  - id 行 L74: `Long fromLegalId = resolveLegalEntityRoot(fromOrgId);`
  - id 行 L75: `Long toLegalId = resolveLegalEntityRoot(toOrgId);`
  - id 行 L95: `Long fromAcctSchemaId = resolveOrgAcctSchemaId(fromLegalId);`
  - id 行 L96: `Long toAcctSchemaId = resolveOrgAcctSchemaId(toLegalId);`
  - id 行 L97: `Long periodId = resolvePeriodId(businessDate);`
  - id 行 L98: `Long currencyId = 1L;`
  - id 行 L106: `public List<Long> onTradeDocumentApproved(String docType, Long docId, String docCode, Long executingOrgId,`
  - id 行 L144: `Long sellerAcctSchemaId = resolveOrgAcctSchemaId(sellerLegal);`
  - id 行 L145: `Long buyerAcctSchemaId = resolveOrgAcctSchemaId(buyerLegal);`
  - id 行 L146: `Long periodId = resolvePeriodId(businessDate);`
  - id 行 L147: `Long currencyId = 1L;`
  - id 行 L154: `public List<Long> onTradeDocumentReversed(String docType, Long docId, String docCode, IServiceContext context) {`
  - id 行 L174: `private Long resolveCounterpartyLegalEntity(Long executingLegalId, String docType, LocalDate businessDate) {`
  - id 行 L198: `private Long resolveWarehouseOrgId(Long warehouseId) {`
  - id 行 L207: `Long resolveLegalEntityRoot(Long orgId) {`
  - id 行 L228: `private String resolveTransferCode(Long transferOrderId) {`
  - id 行 L233: `private Long resolveOrgAcctSchemaId(Long orgId) {`
  - id 行 L238: `private Long resolvePeriodId(LocalDate businessDate) {`
  - id 行 L250: `return list.isEmpty() ? null : list.get(0).getId();`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/intercompany/IntercompanyVoucherGenerator.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 40
- 耦合点 L9: ErpMdSubject (master-data)
- 耦合点 L372: ErpMdSubject.class (daoFor) (master-data)
  - id 行 L69: `public List<Long> generatePairedVouchers(String transferOrderCode, Long fromOrgLegalId, Long toOrgLegalId,`
  - id 行 L70: `Long fromAcctSchemaId, Long toAcctSchemaId, Long periodId,`
  - id 行 L71: `Long currencyId, BigDecimal amount) {`
  - id 行 L78: `intercompanyDims.setFromOrgId(fromOrgLegalId);`
  - id 行 L79: `intercompanyDims.setToOrgId(toOrgLegalId);`
  - id 行 L88: `Long arVoucherId = writeIntercompanyVoucher(transferOrderCode, ErpFinConstants.INTERCOMPANY_SALE_BILL_TYPE,`
  - id 行 L102: `Long apVoucherId = writeIntercompanyVoucher(transferOrderCode, ErpFinConstants.INTERCOMPANY_PURCHASE_BILL_TYPE,`
  - id 行 L130: `List<ErpFinVoucherLine> origLines = loadVoucherLines(original.getId());`
  - id 行 L135: `reversalIds.add(reversal.getId());`
  - id 行 L173: `private List<ErpFinVoucherLine> loadVoucherLines(Long voucherId) {`
  - id 行 L207: `reversal.setOrgId(original.getOrgId());`
  - id 行 L208: `reversal.setAcctSchemaId(original.getAcctSchemaId());`
  - id 行 L209: `reversal.setPeriodId(original.getPeriodId());`
  - id 行 L213: `reversal.setReversalOfVoucherId(original.getId());`
  - id 行 L217: `Long reversalId = reversal.getId();`
  - id 行 L224: `line.setVoucherId(reversalId);`
  - id 行 L226: `line.setSubjectId(ol.getSubjectId());`
  - id 行 L232: `line.setCurrencyId(ol.getCurrencyId());`
  - id 行 L236: `line.setAcctSchemaId(ol.getAcctSchemaId());`
  - id 行 L237: `line.setOrgId(ol.getOrgId());`
  - id 行 L244: `billR.setVoucherId(reversalId);`
  - id 行 L246: `billR.setBillCode(findOriginalIntercompanyBillCode(original.getId()));`
  - id 行 L253: `private String findOriginalIntercompanyBillCode(Long voucherId) {`
  - id 行 L269: `Long acctSchemaId, String defaultCode) {`
  - id 行 L283: `private Long writeIntercompanyVoucher(String transferOrderCode, String billType, Long orgId, Long acctSchemaId,`
  - id 行 L284: `Long periodId, Long currencyId, BigDecimal amount,`
  - id 行 L299: `voucher.setOrgId(orgId);`
  - id 行 L300: `voucher.setAcctSchemaId(acctSchemaId);`
  - id 行 L301: `voucher.setPeriodId(periodId);`
  - id 行 L308: `Long voucherId = voucher.getId();`
  - id 行 L312: `debitLine.setVoucherId(voucherId);`
  - id 行 L318: `debitLine.setCurrencyId(currencyId);`
  - id 行 L322: `debitLine.setAcctSchemaId(acctSchemaId);`
  - id 行 L323: `debitLine.setOrgId(orgId);`
  - id 行 L330: `creditLine.setVoucherId(voucherId);`
  - id 行 L336: `creditLine.setCurrencyId(currencyId);`
  - id 行 L340: `creditLine.setAcctSchemaId(acctSchemaId);`
  - id 行 L341: `creditLine.setOrgId(orgId);`
  - id 行 L348: `billR.setVoucherId(voucherId);`
  - id 行 L359: `line.setSubjectId(subject.getId());`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/EmployeeAdvancePostingDispatcher.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 18
- 耦合点 L8: ErpMdEmployee (master-data)
- 耦合点 L151: ErpMdEmployee.class (daoFor) (master-data)
  - id 行 L41: `Long voucherId = executor.postEvent(event);`
  - id 行 L61: `public boolean postSettle(String claimCode, Long partnerId, BigDecimal netAmount, Long orgId,`
  - id 行 L62: `Long currencyId, java.time.LocalDate voucherDate) {`
  - id 行 L66: `event.setOrgId(orgId);`
  - id 行 L67: `event.setAcctSchemaId(resolveAcctSchemaId(orgId));`
  - id 行 L68: `event.setCurrencyId(currencyId != null ? currencyId : 1L);`
  - id 行 L76: `Long voucherId = executor.postEvent(event);`
  - id 行 L96: `Long partnerId = resolveEmployeePartnerId(advance.getEmployeeId());`
  - id 行 L101: `event.setOrgId(advance.getOrgId());`
  - id 行 L102: `event.setAcctSchemaId(resolveAcctSchemaId(advance.getOrgId()));`
  - id 行 L103: `event.setCurrencyId(advance.getCurrencyId() != null ? advance.getCurrencyId() : 1L);`
  - id 行 L113: `Long voucherId = executor.postEvent(event);`
  - id 行 L122: `Long partnerId = resolveEmployeePartnerId(advance.getEmployeeId());`
  - id 行 L127: `event.setOrgId(advance.getOrgId());`
  - id 行 L128: `event.setAcctSchemaId(resolveAcctSchemaId(advance.getOrgId()));`
  - id 行 L129: `event.setCurrencyId(advance.getCurrencyId());`
  - id 行 L142: `private Long resolveAcctSchemaId(Long orgId) {`
  - id 行 L147: `private Long resolveEmployeePartnerId(Long employeeId) {`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinDeferredPostingRetryHelper.java
- 外域: notify  |  本文件 id-as-Long 证据行: 10
- 耦合点 L8: IErpSysNotificationBiz (notify)
  - id 行 L76: `public boolean retry(Long exceptionId, IServiceContext ctx) {`
  - id 行 L104: `Long voucherId = voucherBiz.post(event, ctx);`
  - id 行 L106: `ex.getId(), ex.getBillHeadCode(), voucherId);`
  - id 行 L121: `event.setOrgId(ex.getOrgId());`
  - id 行 L122: `event.setAcctSchemaId(ex.getAcctSchemaId());`
  - id 行 L123: `event.setCurrencyId(ex.getCurrencyId());`
  - id 行 L145: `ErpFinPostingException managed = dao.getEntityById(ex.getId());`
  - id 行 L162: `ex.getId(), persistErr.getMessage());`
  - id 行 L179: `ctx.put("exceptionId", ex.getId());`
  - id 行 L190: `ex.getId(), notifyErr.getMessage());`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinGlMappingResolver.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 21
- 耦合点 L6: ErpMdMaterial (master-data)
- 耦合点 L219: ErpMdMaterial.class (daoFor) (master-data)
  - id 行 L82: `Long acctSchemaId) {`
  - id 行 L87: `Long orgId = resolveOrgIdFromDimensions(effectiveDims);`
  - id 行 L158: `private boolean matches(ErpFinGlMappingRule rule, GlMappingDimensions dims, Long acctSchemaId) {`
  - id 行 L194: `expanded.setOrgId(input.getOrgId());`
  - id 行 L195: `expanded.setPartnerId(input.getPartnerId());`
  - id 行 L196: `expanded.setPartnerGroupId(input.getPartnerGroupId());`
  - id 行 L197: `expanded.setWarehouseId(input.getWarehouseId());`
  - id 行 L198: `expanded.setDepartmentId(input.getDepartmentId());`
  - id 行 L199: `expanded.setProjectId(input.getProjectId());`
  - id 行 L201: `expanded.setFromOrgId(input.getFromOrgId());`
  - id 行 L202: `expanded.setToOrgId(input.getToOrgId());`
  - id 行 L206: `expanded.setMaterialCategoryId(input.getMaterialCategoryId());`
  - id 行 L208: `Long categoryId = lookupMaterialCategoryId(input.getMaterialId());`
  - id 行 L209: `expanded.setMaterialCategoryId(categoryId); // null 表示找不到 → 参与通配`
  - id 行 L210: `expanded.setMaterialId(input.getMaterialId());`
  - id 行 L212: `expanded.setMaterialId(null);`
  - id 行 L217: `private Long lookupMaterialCategoryId(Long materialId) {`
  - id 行 L250: `private List<ErpFinGlMappingRule> loadFromCache(Long orgId, String businessType, String accountKey) {`
  - id 行 L257: `private List<ErpFinGlMappingRule> loadFromDb(Long orgId, String businessType, String accountKey) {`
  - id 行 L278: `Long bucketOrgId = orgDimEnabled ? rule.getOrgId() : null;`
  - id 行 L289: `private static String cacheKey(Long orgId, String businessType, String accountKey) {`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostingExceptionRecorder.java
- 外域: notify  |  本文件 id-as-Long 证据行: 9
- 耦合点 L5: IErpSysNotificationBiz (notify)
  - id 行 L91: `LocalDate voucherDate, Long orgId, Long acctSchemaId,`
  - id 行 L92: `Long currencyId, BigDecimal exchangeRate, String eventData) {`
  - id 行 L93: `Long exceptionId = null;`
  - id 行 L99: `entity.setTraceId(traceId);`
  - id 行 L107: `entity.setOrgId(orgId);`
  - id 行 L108: `entity.setAcctSchemaId(acctSchemaId);`
  - id 行 L109: `entity.setCurrencyId(currencyId);`
  - id 行 L117: `return entity.getId();`
  - id 行 L136: `private void dispatchNotify(Long exceptionId, String billHeadCode, String businessType, String postingType,`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostingProcessor.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 89
- 耦合点 L14: IErpMdCurrencyBiz (master-data)
- 耦合点 L15: IErpMdSubjectBiz (master-data)
- 耦合点 L16: ErpMdCurrency (master-data)
- 耦合点 L17: ErpMdSubject (master-data)
- 耦合点 L742: ErpMdSubject.class (daoFor) (master-data)
  - id 行 L154: `primaryCtx.setTraceId(run.traceId);`
  - id 行 L165: `Long primaryVoucherId = null;`
  - id 行 L166: `Long originalSchemaId = event.getAcctSchemaId();`
  - id 行 L167: `for (Long schemaId : targetSchemas) {`
  - id 行 L174: `ctx.setAcctSchemaId(schemaId);`
  - id 行 L175: `ctx.setTraceId(run.traceId);`
  - id 行 L181: `final Long currentSchemaId = schemaId;`
  - id 行 L183: `Long voucherId = timeStage("persistVoucher_" + schemaId, run,`
  - id 行 L187: `event.setAcctSchemaId(currentSchemaId);`
  - id 行 L195: `event.setAcctSchemaId(originalSchemaId);`
  - id 行 L206: `event.setAcctSchemaId(event.getAcctSchemaId());`
  - id 行 L234: `Long primaryReversalId = null;`
  - id 行 L235: `Long primarySchemaId = originals.get(0).getAcctSchemaId();`
  - id 行 L237: `List<ErpFinVoucherLine> originalLines = loadLines(original.getId(), context);`
  - id 行 L240: `ctx.setTraceId(run.traceId);`
  - id 行 L244: `Long voucherId = persistVoucher(null, ctx, draft.facts, draft.totalDebit, draft.totalCredit, true,`
  - id 行 L245: `original.getId(), POSTING_TYPE_REVERSAL, billHeadCode, businessType, context);`
  - id 行 L253: `final Long finalPrimaryReversalId = primaryReversalId;`
  - id 行 L254: `final Long firstOriginalId = originals.get(0).getId();`
  - id 行 L280: `event.setTraceId(StringHelper.generateUUID());`
  - id 行 L336: `Long orgId = event != null ? event.getOrgId() : null;`
  - id 行 L337: `Long acctSchemaId = event != null ? event.getAcctSchemaId() : null;`
  - id 行 L338: `Long currencyId = event != null ? event.getCurrencyId() : null;`
  - id 行 L379: `protected void dispatchReversalEvent(PostingRun run, Long voucherId, Long reversalOfVoucherId,`
  - id 行 L383: `event.setVoucherId(voucherId);`
  - id 行 L384: `event.setReversalOfVoucherId(reversalOfVoucherId);`
  - id 行 L388: `event.setTraceId(run.traceId);`
  - id 行 L488: `protected boolean alreadyPosted(PostingEvent event, Long acctSchemaId, IServiceContext context) {`
  - id 行 L511: `protected ErpFinAccountingPeriod resolveOpenPeriod(LocalDate voucherDate, Long orgId, IServiceContext context) {`
  - id 行 L537: `ctx.setAcctSchemaId(event.getAcctSchemaId());`
  - id 行 L538: `ctx.setOrgId(event.getOrgId());`
  - id 行 L539: `ctx.setCurrencyId(event.getCurrencyId());`
  - id 行 L544: `ctx.setPeriodId(period.getId());`
  - id 行 L571: `protected ErpMdCurrency findCurrencyById(Long currencyId, IServiceContext context) {`
  - id 行 L583: `ctx.setAcctSchemaId(original.getAcctSchemaId());`
  - id 行 L584: `ctx.setOrgId(original.getOrgId());`
  - id 行 L585: `ctx.setPeriodId(period.getId());`
  - id 行 L590: `ctx.setCurrencyId(first.getCurrencyId());`
  - id 行 L605: `Long orgId = event.getOrgId();`
  - id 行 L608: `fact.setOrgId(orgId);`
  - id 行 L666: `fact.setSubjectId(subject.getId());`
  - id 行 L676: `dims.setOrgId(fact.getOrgId());`
  - id 行 L677: `dims.setPartnerId(fact.getPartnerId());`
  - id 行 L678: `dims.setMaterialId(fact.getMaterialId());`
  - id 行 L679: `dims.setWarehouseId(fact.getWarehouseId());`
  - id 行 L680: `dims.setDepartmentId(fact.getDepartmentId());`
  - id 行 L681: `dims.setProjectId(fact.getProjectId());`
  - id 行 L701: `protected List<VoucherFact> translateFactsForSchema(List<VoucherFact> facts, Long sourceSchemaId,`
  - id 行 L702: `Long targetSchemaId, IServiceContext context) {`
  - id 行 L720: `copy.setSubjectId(f.getSubjectId());`
  - id 行 L729: `copy.setOrgId(f.getOrgId());`
  - id 行 L730: `copy.setPartnerId(f.getPartnerId());`
  - id 行 L731: `copy.setDepartmentId(f.getDepartmentId());`
  - id 行 L732: `copy.setProjectId(f.getProjectId());`
  - id 行 L733: `copy.setWarehouseId(f.getWarehouseId());`
  - id 行 L734: `copy.setMaterialId(f.getMaterialId());`
  - id 行 L735: `copy.setCostCenterId(f.getCostCenterId());`
  - id 行 L738: `Long mappedId = mapping.get(f.getSubjectId());`
  - id 行 L748: `copy.setSubjectId(targetSubject.getId());`
  - id 行 L794: `fact.setSubjectId(ol.getSubjectId());`
  - id 行 L801: `fact.setPartnerId(ol.getPartnerId());`
  - id 行 L802: `fact.setDepartmentId(ol.getDepartmentId());`
  - id 行 L803: `fact.setProjectId(ol.getProjectId());`
  - id 行 L804: `fact.setWarehouseId(ol.getWarehouseId());`
  - id 行 L805: `fact.setMaterialId(ol.getMaterialId());`
  - id 行 L806: `fact.setCostCenterId(ol.getCostCenterId());`
  - id 行 L814: `Long reversalOfVoucherId, String postingType, IServiceContext context) {`
  - id 行 L821: `Long reversalOfVoucherId, String postingType, String billHeadCode,`
  - id 行 L827: `Long acctSchemaId = ctx.getAcctSchemaId();`
  - id 行 L828: `Long orgId = ctx.getOrgId();`
  - id 行 L829: `Long periodId = ctx.getPeriodId();`
  - id 行 L839: `voucher.setOrgId(orgId);`
  - id 行 L840: `voucher.setAcctSchemaId(acctSchemaId);`
  - id 行 L841: `voucher.setPeriodId(periodId);`
  - id 行 L846: `voucher.setReversalOfVoucherId(reversalOfVoucherId);`
  - id 行 L851: `Long voucherId = voucher.getId();`
  - id 行 L853: `Long currencyId = ctx.getCurrencyId();`
  - id 行 L866: `line.setVoucherId(voucherId);`
  - id 行 L868: `line.setSubjectId(fact.getSubjectId());`
  - id 行 L876: `line.setCurrencyId(currencyId);`
  - id 行 L880: `line.setAcctSchemaId(acctSchemaId);`
  - id 行 L884: `line.setPartnerId(fact.getPartnerId());`
  - id 行 L885: `line.setDepartmentId(fact.getDepartmentId());`
  - id 行 L886: `line.setProjectId(fact.getProjectId());`
  - id 行 L887: `line.setWarehouseId(fact.getWarehouseId());`
  - id 行 L888: `line.setMaterialId(fact.getMaterialId());`
  - id 行 L889: `line.setCostCenterId(fact.getCostCenterId());`
  - id 行 L898: `billR.setVoucherId(voucherId);`
  - id 行 L952: `protected List<ErpFinVoucherLine> loadLines(Long voucherId, IServiceContext context) {`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinTransferPriceResolver.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 9
- 耦合点 L6: ErpMdMaterial (master-data)
- 耦合点 L167: ErpMdMaterial.class (daoFor) (master-data)
  - id 行 L56: `public TransferPriceResult resolvePrice(Long fromOrgId, Long toOrgId, Long materialId,`
  - id 行 L61: `Long materialCategoryId = lookupMaterialCategoryId(materialId);`
  - id 行 L73: `result.setRuleId(winner.getId());`
  - id 行 L90: `Long fromOrgId, Long toOrgId, Long materialId,`
  - id 行 L91: `Long materialCategoryId, LocalDate businessDate) {`
  - id 行 L118: `private boolean matchesMaterial(ErpFinIntercompanyTransferPrice rule, Long materialId, Long materialCategoryId) {`
  - id 行 L162: `private Long lookupMaterialCategoryId(Long materialId) {`
  - id 行 L175: `private List<ErpFinIntercompanyTransferPrice> loadCandidates(Long fromOrgId, Long toOrgId) {`
  - id 行 L203: `private static String cacheKey(Long fromOrgId, Long toOrgId) {`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ExpenseClaimPostingDispatcher.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 7
- 耦合点 L8: ErpMdEmployee (master-data)
- 耦合点 L95: ErpMdEmployee.class (daoFor) (master-data)
  - id 行 L42: `Long voucherId = executor.postEvent(event);`
  - id 行 L59: `Long partnerId = resolveEmployeePartnerId(claim.getClaimantId());`
  - id 行 L64: `event.setOrgId(claim.getOrgId());`
  - id 行 L65: `event.setAcctSchemaId(resolveAcctSchemaId(claim.getOrgId()));`
  - id 行 L66: `event.setCurrencyId(claim.getCurrencyId());`
  - id 行 L86: `private Long resolveAcctSchemaId(Long orgId) {`
  - id 行 L91: `private Long resolveEmployeePartnerId(Long claimantId) {`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/SchemaPropagator.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 8
- 耦合点 L4: ErpMdAcctSchema (master-data)
- 耦合点 L107: ErpMdAcctSchema.class (daoFor) (master-data)
- 耦合点 L112: ErpMdAcctSchema.class (daoFor) (master-data)
  - id 行 L44: `public List<Long> resolveTargetSchemas(Long orgId, Long primarySchemaId) {`
  - id 行 L76: `if (!schema.getId().equals(primarySchemaId)) {`
  - id 行 L77: `targets.add(schema.getId());`
  - id 行 L94: `public Long findPrimarySchemaId(Long orgId) {`
  - id 行 L100: `return s.getId();`
  - id 行 L103: `return schemas.isEmpty() ? null : schemas.get(0).getId();`
  - id 行 L106: `private ErpMdAcctSchema loadSchema(Long schemaId) {`
  - id 行 L111: `private List<ErpMdAcctSchema> findActiveSchemasByOrg(Long orgId) {`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/AbstractErpFinReconciliationProcessor.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 8
- 耦合点 L16: ErpMdSubject (master-data)
- 耦合点 L240: ErpMdSubject.class (daoFor) (master-data)
  - id 行 L93: `protected void assertOpen(ErpFinArApItem item, Long itemId) {`
  - id 行 L102: `protected void assertNotOver(BigDecimal amt, ErpFinArApItem item, Long itemId, BigDecimal precision) {`
  - id 行 L123: `protected List<ErpFinReconciliationLine> loadLines(Long reconciliationId) {`
  - id 行 L174: `.param(ErpFinErrors.ARG_RECONCILIATION_ID, head.getId())`
  - id 行 L204: `lines.add(new Line(counterpart.getId(), counterpart.getCode(), counterpart.getName(),`
  - id 行 L206: `lines.add(new Line(fxSubject.getId(), fxSubject.getCode(), fxSubject.getName(), fxDc, abs, null));`
  - id 行 L253: `protected Long resolvePeriodId(LocalDate businessDate) {`
  - id 行 L263: `return periods.isEmpty() ? null : periods.get(0).getId();`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinAccountingPeriodProcessor.java
- 外域: assets, inventory  |  本文件 id-as-Long 证据行: 29
- 耦合点 L3: IErpAstDepreciationScheduleBiz (assets)
- 耦合点 L4: ErpAstDepreciationSchedule (assets)
- 耦合点 L15: CostingRecloseReport (inventory)
- 耦合点 L16: IErpInvCostingBiz (inventory)
- 耦合点 L250: ErpAstDepreciationSchedule.class (daoFor) (assets)
- 耦合点 L512: ErpAstDepreciationSchedule.class (daoFor) (assets)
- 耦合点 L536: ErpInvLandedCost (inventory)
- 耦合点 L537: ErpInvLandedCost.class (inventory)
- 耦合点 L544: ErpInvLandedCost (inventory)
  - id 行 L137: `protected Long resolveDefaultOrgId() {`
  - id 行 L222: `CostingRecloseReport report = costingBiz.reclosePeriodCosts(period.getId(),`
  - id 行 L333: `List<Long> voucherIds = findPostedVoucherIds(period.getId());`
  - id 行 L336: `clearQ.addFilter(eq("periodId", period.getId()));`
  - id 行 L350: `Long fallbackSchema = resolveAcctSchemaId(period.getId());`
  - id 行 L355: `Long schemaId = l.getAcctSchemaId() != null ? l.getAcctSchemaId() : fallbackSchema;`
  - id 行 L364: `Long acctSchemaId = schemaEntry.getKey();`
  - id 行 L367: `tb.setOrgId(period.getOrgId());`
  - id 行 L368: `tb.setAcctSchemaId(acctSchemaId);`
  - id 行 L369: `tb.setPeriodId(period.getId());`
  - id 行 L370: `tb.setSubjectId(a.subjectId);`
  - id 行 L386: `protected List<Long> findPostedVoucherIds(Long periodId) {`
  - id 行 L399: `protected Long resolveAcctSchemaId(Long periodId) {`
  - id 行 L401: `Long orgId = period != null ? period.getOrgId() : null;`
  - id 行 L403: `Long schemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);`
  - id 行 L420: `final Long subjectId;`
  - id 行 L438: `q.addFilter(eq("periodId", period.getId()));`
  - id 行 L450: `Long orgId = period.getOrgId();`
  - id 行 L453: `Long schemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);`
  - id 行 L522: `keys.add("depreciation:" + (s.getAssetId() == null ? s.getId() : s.getAssetId())`
  - id 行 L569: `protected ErpFinAccountingPeriod requirePeriod(Long periodId) {`
  - id 行 L596: `Long scopeSchemaId = resolveAcctSchemaId(period);`
  - id 行 L598: `q.addFilter(eq("periodId", period.getId()));`
  - id 行 L608: `status.setPeriodId(period.getId());`
  - id 行 L609: `status.setAcctSchemaId(scopeSchemaId);`
  - id 行 L622: `protected Long resolveAcctSchemaId(ErpFinAccountingPeriod period) {`
  - id 行 L623: `Long orgId = period != null ? period.getOrgId() : null;`
  - id 行 L625: `Long schemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);`
  - id 行 L630: `Long periodId = period != null ? period.getId() : null;`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinBadDebtProcessor.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 23
- 耦合点 L8: ErpMdCurrency (master-data)
- 耦合点 L9: ErpMdSubject (master-data)
- 耦合点 L357: ErpMdSubject.class (daoFor) (master-data)
  - id 行 L78: `public ErpFinBadDebt submit(Long badDebtId, IServiceContext context) {`
  - id 行 L82: `public ErpFinBadDebt approve(Long badDebtId, IServiceContext context) {`
  - id 行 L86: `public ErpFinBadDebt reject(Long badDebtId, IServiceContext context) {`
  - id 行 L105: `public ErpFinBadDebt reverseApprove(Long badDebtId, IServiceContext context) {`
  - id 行 L182: `new Line(allowance.getId(), allowance.getCode(), allowance.getName(),`
  - id 行 L184: `new Line(ar.getId(), ar.getCode(), ar.getName(),`
  - id 行 L186: `Long voucherId = writeBadDebtVoucher(debt, item, ErpFinBusinessType.BAD_DEBT_WRITE_OFF, "坏账核销", lines);`
  - id 行 L187: `debt.setVoucherId(voucherId);`
  - id 行 L206: `new Line(ar.getId(), ar.getCode(), ar.getName(),`
  - id 行 L208: `new Line(allowance.getId(), allowance.getCode(), allowance.getName(),`
  - id 行 L210: `Long voucherId = writeBadDebtVoucher(debt, item, ErpFinBusinessType.BAD_DEBT_RECOVERY, "坏账收回恢复", lines);`
  - id 行 L211: `debt.setVoucherId(voucherId);`
  - id 行 L265: `protected ErpFinArApItem requireOpenArApItem(Long arApItemId) {`
  - id 行 L283: `protected ErpFinArApItem requireWrittenOffArApItem(Long arApItemId) {`
  - id 行 L306: `protected ErpFinBadDebt requireBadDebt(Long badDebtId) {`
  - id 行 L314: `protected ErpFinArApItem loadArApItem(Long arApItemId) {`
  - id 行 L321: `debt.setOrgId(item.getOrgId());`
  - id 行 L322: `debt.setAcctSchemaId(item.getAcctSchemaId());`
  - id 行 L324: `debt.setPartnerId(item.getPartnerId());`
  - id 行 L325: `debt.setSourceArApItemId(item.getId());`
  - id 行 L327: `debt.setCurrencyId(item.getCurrencyId());`
  - id 行 L332: `debt.setPeriodId(item.getPeriodId());`
  - id 行 L339: `q.addFilter(eq("sourceArApItemId", item.getId()));`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinBudgetScenarioCarryForwardProcessor.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 32
- 耦合点 L14: ErpMdSubject (master-data)
  - id 行 L55: `public ErpFinBudgetScenario carryForward(Long id, Long targetScenarioId, String rule, IServiceContext context) {`
  - id 行 L164: `sq.addFilter(eq("periodId", p.getId()));`
  - id 行 L180: `List<ErpFinBudgetLine> lines = facade.loadBudgetLines(source.getId());`
  - id 行 L194: `List<ErpFinBudgetLine> lines = facade.loadBudgetLines(source.getId());`
  - id 行 L225: `voucherIds.add(v.getId());`
  - id 行 L269: `q.addFilter(eq("scenarioId", target.getId()));`
  - id 行 L278: `cl.setScenarioId(target.getId());`
  - id 行 L280: `cl.setOrgId(target.getOrgId());`
  - id 行 L281: `cl.setAcctSchemaId(target.getAcctSchemaId());`
  - id 行 L282: `cl.setSubjectId(source.getId());`
  - id 行 L286: `cl.setCurrencyId(target.getCurrencyId());`
  - id 行 L298: `Long periodId = resolveFirstPeriodId(source);`
  - id 行 L309: `v.setOrgId(target.getOrgId());`
  - id 行 L310: `v.setAcctSchemaId(target.getAcctSchemaId());`
  - id 行 L311: `v.setPeriodId(periodId);`
  - id 行 L320: `d.setVoucherId(v.getId());`
  - id 行 L322: `d.setSubjectId(source.getId());`
  - id 行 L328: `d.setCurrencyId(target.getCurrencyId());`
  - id 行 L332: `d.setAcctSchemaId(target.getAcctSchemaId());`
  - id 行 L333: `d.setOrgId(target.getOrgId());`
  - id 行 L339: `c.setVoucherId(v.getId());`
  - id 行 L341: `c.setSubjectId(source.getId());`
  - id 行 L347: `c.setCurrencyId(target.getCurrencyId());`
  - id 行 L351: `c.setAcctSchemaId(target.getAcctSchemaId());`
  - id 行 L352: `c.setOrgId(target.getOrgId());`
  - id 行 L358: `billR.setVoucherId(v.getId());`
  - id 行 L366: `protected Long resolveFirstPeriodId(ErpFinBudgetScenario source) {`
  - id 行 L367: `List<ErpFinBudgetLine> lines = facade.loadBudgetLines(source.getId());`
  - id 行 L381: `log.setOrgId(source.getOrgId());`
  - id 行 L382: `log.setScenarioId(source.getId());`
  - id 行 L383: `log.setSourceScenarioId(source.getId());`
  - id 行 L384: `log.setTargetScenarioId(target.getId());`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinConsolidationEliminationPostEliminationProcessor.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 18
- 耦合点 L95: ErpMdSubject (master-data)
- 耦合点 L119: ErpMdSubject (master-data)
- 耦合点 L163: ErpMdSubject (master-data)
- 耦合点 L167: ErpMdSubject (master-data)
- 耦合点 L168: ErpMdSubject.class (master-data)
- 耦合点 L172: ErpMdSubject (master-data)
  - id 行 L40: `public Long postElimination(Long candidateId, IServiceContext context) {`
  - id 行 L58: `Long voucherId = writeDraftEliminationVoucher(candidate, amount);`
  - id 行 L60: `candidate.setDraftVoucherId(voucherId);`
  - id 行 L83: `voucher.setOrgId(candidate.getOrgId());`
  - id 行 L84: `voucher.setAcctSchemaId(1L);`
  - id 行 L85: `voucher.setPeriodId(candidate.getPeriodId());`
  - id 行 L91: `Long voucherId = voucher.getId();`
  - id 行 L97: `debitLine.setVoucherId(voucherId);`
  - id 行 L100: `debitLine.setSubjectId(debitSubject.getId());`
  - id 行 L107: `debitLine.setCurrencyId(1L);`
  - id 行 L111: `debitLine.setAcctSchemaId(1L);`
  - id 行 L112: `debitLine.setOrgId(candidate.getOrgId());`
  - id 行 L121: `creditLine.setVoucherId(voucherId);`
  - id 行 L124: `creditLine.setSubjectId(creditSubject.getId());`
  - id 行 L131: `creditLine.setCurrencyId(1L);`
  - id 行 L135: `creditLine.setAcctSchemaId(1L);`
  - id 行 L136: `creditLine.setOrgId(candidate.getOrgId());`
  - id 行 L143: `billR.setVoucherId(voucherId);`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinEmployeeAdvanceProcessor.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 3
- 耦合点 L10: ErpMdEmployee (master-data)
  - id 行 L76: `public ErpFinEmployeeAdvance cancel(Long advanceId, IServiceContext context) {`
  - id 行 L226: `protected ErpFinEmployeeAdvance doCancel(Long advanceId, ErpFinEmployeeAdvance advance, IServiceContext context) {`
  - id 行 L241: `protected ErpFinEmployeeAdvance requireAdvance(Long advanceId, IServiceContext context) {`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinExpenseClaimProcessor.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 14
- 耦合点 L14: ErpMdEmployee (master-data)
- 耦合点 L15: ErpMdSubject (master-data)
- 耦合点 L249: ErpMdSubject.class (daoFor) (master-data)
  - id 行 L96: `public ErpFinExpenseClaim cancel(Long claimId, IServiceContext context) {`
  - id 行 L194: `List<ErpFinExpenseClaimLine> lines = loadLines(claim.getId());`
  - id 行 L213: `for (ErpFinExpenseClaimLine line : loadLines(claim.getId())) {`
  - id 行 L234: `Long subjectId = resolveBudgetSubjectId(ErpFinConstants.CONFIG_BUDGET_EXPENSE_SUBJECT_CODE);`
  - id 行 L238: `Long periodId = resolvePeriodId(claim.getBusinessDate());`
  - id 行 L244: `protected Long resolveBudgetSubjectId(String configKey) {`
  - id 行 L254: `return list.isEmpty() ? null : list.get(0).getId();`
  - id 行 L257: `protected Long resolvePeriodId(LocalDate businessDate) {`
  - id 行 L267: `return list.isEmpty() ? null : list.get(0).getId();`
  - id 行 L313: `claim.setSettleAdvanceId(null);`
  - id 行 L322: `protected ErpFinExpenseClaim doCancel(Long claimId, ErpFinExpenseClaim claim, IServiceContext context) {`
  - id 行 L332: `claim.setSettleAdvanceId(null);`
  - id 行 L341: `protected ErpFinExpenseClaim requireClaim(Long claimId, IServiceContext context) {`
  - id 行 L358: `protected List<ErpFinExpenseClaimLine> loadLines(Long claimId) {`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinNotesReceivableProcessor.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 18
- 耦合点 L11: ErpMdCurrency (master-data)
- 耦合点 L320: ErpMdCurrency.class (daoFor) (master-data)
  - id 行 L88: `protected void requireDiscountInputs(ErpFinNotesReceivable note, LocalDate discountDate, Long bankId,`
  - id 行 L99: `protected ErpFinNotesReceivable doReceive(Long notesId, ErpFinNotesReceivable note, IServiceContext context) {`
  - id 行 L110: `protected ErpFinNotesReceivable doDiscount(Long notesId, ErpFinNotesReceivable note, ErpFinNotesDiscount discount,`
  - id 行 L116: `note.setDiscountId(discount.getId());`
  - id 行 L126: `protected ErpFinNotesReceivable doEndorse(Long notesId, ErpFinNotesReceivable note, Long endorsementFromId,`
  - id 行 L129: `note.setEndorsementFromId(endorsementFromId);`
  - id 行 L141: `protected ErpFinNotesReceivable doHonor(Long notesId, ErpFinNotesReceivable note, IServiceContext context) {`
  - id 行 L168: `protected ErpFinNotesDiscount buildDiscount(ErpFinNotesReceivable note, LocalDate discountDate, Long bankId,`
  - id 行 L210: `discount.setNotesReceivableId(note.getId());`
  - id 行 L211: `discount.setOrgId(note.getOrgId());`
  - id 行 L213: `discount.setBankId(bankId);`
  - id 行 L217: `discount.setCurrencyId(note.getCurrencyId());`
  - id 行 L243: `protected ErpFinNotesReceivable requireNote(Long notesId, IServiceContext context) {`
  - id 行 L247: `protected ErpFinNotesReceivable requireNote(Long notesId) {`
  - id 行 L271: `protected ErpFinNotesReceivable reload(Long notesId) {`
  - id 行 L312: `Long functionalCurrencyId = resolveFunctionalCurrencyId();`
  - id 行 L319: `private Long resolveFunctionalCurrencyId() {`
  - id 行 L325: `return list.isEmpty() ? null : list.get(0).getId();`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinPostingExceptionIgnoreProcessor.java
- 外域: notify  |  本文件 id-as-Long 证据行: 4
- 耦合点 L6: IErpSysNotificationBiz (notify)
  - id 行 L32: `public ErpFinPostingException ignore(Long exceptionId, String resolutionNote, IServiceContext context) {`
  - id 行 L59: `ctx.put("exceptionId", entity.getId());`
  - id 行 L71: `entity.getId(), e.getMessage());`
  - id 行 L75: `protected ErpFinPostingException requirePending(Long exceptionId) {`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/profitloss/ProfitLossClosingService.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 18
- 耦合点 L8: ErpMdCurrency (master-data)
- 耦合点 L9: ErpMdSubject (master-data)
- 耦合点 L173: ErpMdSubject.class (daoFor) (master-data)
- 耦合点 L182: ErpMdSubject.class (daoFor) (master-data)
- 耦合点 L219: ErpMdCurrency.class (daoFor) (master-data)
  - id 行 L60: `Long primarySchemaId = resolveAcctSchemaId(period.getId());`
  - id 行 L62: `Long lastVoucherId = null;`
  - id 行 L63: `for (Long schemaId : schemas) {`
  - id 行 L69: `private Long closeForSchema(ErpFinAccountingPeriod period, Long acctSchemaId, IServiceContext context) {`
  - id 行 L71: `List<Long> voucherIds = findPostedVoucherIds(period.getId());`
  - id 行 L120: `plLines.add(new Line(a.subject.getId(), a.subject.getCode(), a.subject.getName(),`
  - id 行 L128: `plLines.add(new Line(a.subject.getId(), a.subject.getCode(), a.subject.getName(),`
  - id 行 L142: `plLines.add(new Line(cypSubject.getId(), cypSubject.getCode(), cypSubject.getName(),`
  - id 行 L146: `plLines.add(new Line(cypSubject.getId(), cypSubject.getCode(), cypSubject.getName(),`
  - id 行 L150: `Long orgId = period.getOrgId();`
  - id 行 L151: `Long functionalCurrencyId = resolveFunctionalCurrencyId();`
  - id 行 L154: `orgId, acctSchemaId, period.getId(), functionalCurrencyId, BigDecimal.ONE,`
  - id 行 L185: `private List<Long> findPostedVoucherIds(Long periodId) {`
  - id 行 L198: `private Long resolveAcctSchemaId(Long periodId) {`
  - id 行 L200: `Long orgId = period != null ? period.getOrgId() : null;`
  - id 行 L202: `Long schemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);`
  - id 行 L218: `private Long resolveFunctionalCurrencyId() {`
  - id 行 L225: `return list.get(0).getId();`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/reconciliation/DualSideConsistencyChecker.java
- 外域: purchase, sales  |  本文件 id-as-Long 证据行: 4
- 耦合点 L8: ErpPurInvoice (purchase)
- 耦合点 L9: ErpSalInvoice (sales)
- 耦合点 L133: ErpPurInvoice.class (daoFor) (purchase)
- 耦合点 L141: ErpSalInvoice.class (daoFor) (sales)
  - id 行 L52: `public DualSideDiffReport check(String direction, Long partnerId, IServiceContext context) {`
  - id 行 L55: `report.setPartnerId(partnerId);`
  - id 行 L86: `row.setPartnerId(pid);`
  - id 行 L104: `protected List<ErpFinArApItem> findInvoiceItems(String direction, Long partnerId, IServiceContext context) {`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/reconciliation/PartnerBalanceUpdater.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 2
- 耦合点 L5: ErpMdPartner (master-data)
- 耦合点 L22: ErpMdPartner.class (daoFor) (master-data)
- 耦合点 L37: ErpMdPartner.class (daoFor) (master-data)
  - id 行 L33: `public void refresh(Long partnerId) {`
  - id 行 L46: `protected BigDecimal sumOpen(Long partnerId, String direction) {`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/report/ErpFinReportBizModel.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 30
- 耦合点 L16: ErpMdSubject (master-data)
  - id 行 L238: `public List<app.erp.fin.dao.dto.BudgetVsActualRow> buildBudgetVsActualDataset(Long acctSchemaId,`
  - id 行 L239: `Long periodId, Long subjectId) {`
  - id 行 L246: `public List<Map<String, Object>> balanceSheetData(@Name("periodId") Long periodId, IServiceContext context) {`
  - id 行 L252: `public List<Map<String, Object>> incomeStatementData(@Name("periodId") Long periodId, IServiceContext context) {`
  - id 行 L258: `public List<Map<String, Object>> cashFlowStatementData(@Name("periodId") Long periodId, IServiceContext context) {`
  - id 行 L266: `public List<Map<String, Object>> indirectCashFlowData(@Name("periodId") Long periodId, IServiceContext context) {`
  - id 行 L279: `public List<Map<String, Object>> periodCloseReportData(@Name("periodId") Long periodId, IServiceContext context) {`
  - id 行 L285: `List<Map<String, Object>> buildBalanceSheetDataset(Long periodId) {`
  - id 行 L300: `List<Map<String, Object>> buildIncomeStatementDataset(Long periodId) {`
  - id 行 L320: `List<Map<String, Object>> buildCashFlowDataset(Long periodId) {`
  - id 行 L367: `List<Map<String, Object>> buildIndirectCashFlowDataset(Long periodId) {`
  - id 行 L430: `List<Map<String, Object>> buildPeriodCloseDataset(Long periodId) {`
  - id 行 L465: `private List<ErpFinGlBalance> loadGlBalances(Long periodId) {`
  - id 行 L470: `Long latestPeriodId = findLatestPeriodId();`
  - id 行 L494: `private Long findLatestPeriodId() {`
  - id 行 L500: `return latest.isEmpty() ? null : latest.get(0).getId();`
  - id 行 L503: `private List<ErpFinVoucherLine> loadPostedVoucherLines(Long periodId) {`
  - id 行 L512: `private List<ErpFinVoucherLine> loadPostedVoucherLines(Long periodId, boolean excludeShadowPostings) {`
  - id 行 L527: `for (ErpFinVoucher v : vouchers) voucherIds.add(v.getId());`
  - id 行 L541: `private ErpFinAccountingPeriodStatus loadPeriodStatus(Long periodId) {`
  - id 行 L550: `private int countBillR(Long periodId, String businessType) {`
  - id 行 L557: `for (ErpFinVoucher v : vouchers) voucherIds.add(v.getId());`
  - id 行 L568: `private Long resolvePeriodOrgId(Long periodId) {`
  - id 行 L576: `private Long resolveOrgSchemaId(Long orgId) {`
  - id 行 L581: `private void applyOrgAndSchemaScope(QueryBean q, Long periodId) {`
  - id 行 L582: `Long orgId = resolvePeriodOrgId(periodId);`
  - id 行 L587: `Long schemaId = resolveOrgSchemaId(orgId);`
  - id 行 L594: `private void applySchemaScope(QueryBean q, Long periodId) {`
  - id 行 L595: `Long orgId = resolvePeriodOrgId(periodId);`
  - id 行 L596: `Long schemaId = resolveOrgSchemaId(orgId);`

### module-finance/erp-fin-service/src/main/java/app/erp/fin/service/treasury/CreditFacilityInterestVoucherBuilder.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 8
- 耦合点 L9: ErpMdAcctSchema (master-data)
- 耦合点 L100: ErpMdAcctSchema.class (daoFor) (master-data)
  - id 行 L60: `Long acctSchemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, facility.getOrgId());`
  - id 行 L64: `Long currencyId = resolveCurrencyId(facility, acctSchemaId);`
  - id 行 L68: `event.setBillHeadCode(buildBillHeadCode(facility.getId(), fromDate, toDate));`
  - id 行 L69: `event.setAcctSchemaId(acctSchemaId);`
  - id 行 L70: `event.setOrgId(facility.getOrgId());`
  - id 行 L71: `event.setCurrencyId(currencyId);`
  - id 行 L84: `public static String buildBillHeadCode(Long facilityId, LocalDate fromDate, LocalDate toDate) {`
  - id 行 L92: `protected Long resolveCurrencyId(ErpFinCreditFacility facility, Long acctSchemaId) {`

## module-hr （引用外域的耦合文件 5 个）

### module-hr/erp-hr-service/src/main/java/app/erp/hr/service/job/ErpHrContractExpiryJob.java
- 外域: notify  |  本文件 id-as-Long 证据行: 2
- 耦合点 L6: IErpSysNotificationBiz (notify)
  - id 行 L83: `c.getId(), ex.getMessage());`
  - id 行 L100: `map.put("contractId", contract.getId());`

### module-hr/erp-hr-service/src/main/java/app/erp/hr/service/job/ErpHrLeaveApproverTimeoutJob.java
- 外域: notify  |  本文件 id-as-Long 证据行: 8
- 耦合点 L10: IErpSysNotificationBiz (notify)
  - id 行 L141: `leave.getId(), e.getMessage());`
  - id 行 L152: `Long targetId = resolveEscalationTarget(leave.getEmployeeId(), ctx);`
  - id 行 L155: `leave.getId(), leave.getEmployeeId());`
  - id 行 L160: `LOG.info("erp-hr-leave-approver-timeout: 幂等跳过（approverId 已为目标审批人）：leaveId={}", leave.getId());`
  - id 行 L163: `leave.setApproverId(targetId);`
  - id 行 L173: `protected Long resolveEscalationTarget(Long employeeId, IServiceContext ctx) {`
  - id 行 L195: `protected void notifyEscalation(ErpHrLeaveRequest leave, Long targetId, IServiceContext ctx) {`
  - id 行 L213: `protected String resolveUserId(Long employeeId, IServiceContext ctx) {`

### module-hr/erp-hr-service/src/main/java/app/erp/hr/service/posting/SalaryPostingDispatcher.java
- 外域: finance, master-data, notify  |  本文件 id-as-Long 证据行: 30
- 耦合点 L5: ErpFinVoucher (finance)
- 耦合点 L6: ErpFinVoucherBillR (finance)
- 耦合点 L15: ErpMdAcctSchema (master-data)
- 耦合点 L16: IErpSysNotificationBiz (notify)
- 耦合点 L192: ErpFinVoucherBillR.class (daoFor) (finance)
- 耦合点 L200: ErpFinVoucher.class (daoFor) (finance)
- 耦合点 L355: ErpMdAcctSchema.class (daoFor) (master-data)
  - id 行 L89: `salary.getId(), buildBillCode(salary));`
  - id 行 L94: `Long voucherId = executor.postEvent(event);`
  - id 行 L99: `salary.getId(), e.getMessage());`
  - id 行 L101: `LOG.error("薪酬计提过账异常，薪酬记录 {} 保持 APPROVED", salary.getId(), e);`
  - id 行 L116: `salary.getId(), buildBillCode(salary));`
  - id 行 L124: `Long voucherId = executor.postEvent(event);`
  - id 行 L128: `LOG.warn("社保公司承担过账失败，薪酬记录 {} 保持 APPROVED：{}", salary.getId(), e.getMessage());`
  - id 行 L130: `LOG.error("社保公司承担过账异常，薪酬记录 {} 保持 APPROVED", salary.getId(), e);`
  - id 行 L145: `salary.getId(), buildBillCode(salary));`
  - id 行 L153: `Long voucherId = executor.postEvent(event);`
  - id 行 L157: `LOG.warn("公积金公司承担过账失败，薪酬记录 {} 保持 APPROVED：{}", salary.getId(), e.getMessage());`
  - id 行 L159: `LOG.error("公积金公司承担过账异常，薪酬记录 {} 保持 APPROVED", salary.getId(), e);`
  - id 行 L173: `Long voucherId = executor.postEvent(event);`
  - id 行 L177: `LOG.warn("薪酬发放过账失败，薪酬记录 {} 已 PAID：{}", salary.getId(), e.getMessage());`
  - id 行 L179: `LOG.error("薪酬发放过账异常，薪酬记录 {} 已 PAID", salary.getId(), e);`
  - id 行 L217: `ctx.put("salaryId", salary.getId());`
  - id 行 L230: `salary.getId(), notifyErr.getMessage());`
  - id 行 L268: `billData.put(ErpHrConstants.BILL_DATA_SALARY_ID, salary.getId());`
  - id 行 L319: `billData.put(ErpHrConstants.BILL_DATA_SALARY_ID, salary.getId());`
  - id 行 L344: `Long orgId = salary.getOrgId() != null ? salary.getOrgId() : resolveEmployeeOrgId(salary.getEmployeeId());`
  - id 行 L345: `event.setOrgId(orgId);`
  - id 行 L346: `Long schemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);`
  - id 行 L347: `event.setAcctSchemaId(schemaId);`
  - id 行 L348: `event.setCurrencyId(resolveFunctionalCurrencyId(schemaId));`
  - id 行 L351: `private Long resolveFunctionalCurrencyId(Long schemaId) {`
  - id 行 L360: `private Long resolveEmployeeOrgId(Long employeeId) {`
  - id 行 L369: `return "SAL-" + salary.getYear() + String.format("%02d", salary.getMonth()) + "-" + salary.getId();`
  - id 行 L372: `private Long resolveDepartmentId(Long employeeId) {`
  - id 行 L377: `private Long resolveCostCenterId(Long employeeId) {`
  - id 行 L382: `private ErpHrEmployee findEmployee(Long employeeId) {`

### module-hr/erp-hr-service/src/main/java/app/erp/hr/service/posting/SalaryPostingExecutor.java
- 外域: finance  |  本文件 id-as-Long 证据行: 0
- 耦合点 L3: IErpFinVoucherBiz (finance)

### module-hr/erp-hr-service/src/main/java/app/erp/hr/service/report/ErpHrReportBizModel.java
- 外域: finance, master-data  |  本文件 id-as-Long 证据行: 8
- 耦合点 L3: IErpFinArApItemBiz (finance)
- 耦合点 L4: ErpFinArApItem (finance)
- 耦合点 L9: ErpMdPartner (master-data)
- 耦合点 L268: ErpMdPartner.class (daoFor) (master-data)
  - id 行 L196: `public List<Map<String, Object>> payrollSimulationComparisonData(@Optional @Name("simulationId") Long simulationId,`
  - id 行 L227: `for (Long partnerId : partnerIds) {`
  - id 行 L253: `Long partnerId = item.getPartnerId();`
  - id 行 L272: `names.put(p.getId(), p.getName());`
  - id 行 L282: `List<Map<String, Object>> buildPayrollSimulationComparisonDataset(Long simulationId, IServiceContext context) {`
  - id 行 L298: `Long departmentId = emp != null ? emp.getDepartmentId() : null;`
  - id 行 L326: `private List<ErpHrSalarySimulationItemAdjustment> loadAdjustments(Long simulationId) {`
  - id 行 L350: `map.put(e.getId(), e);`

## module-inventory （引用外域的耦合文件 14 个）

### module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/CostAdjustmentService.java
- 外域: manufacturing, master-data  |  本文件 id-as-Long 证据行: 25
- 耦合点 L13: ErpMfgCostRollup (manufacturing)
- 耦合点 L14: ErpMfgCostRollupLine (manufacturing)
- 耦合点 L15: ErpMdMaterial (master-data)
- 耦合点 L210: ErpMfgCostRollup.class (daoFor) (manufacturing)
- 耦合点 L219: ErpMfgCostRollupLine.class (daoFor) (manufacturing)
- 耦合点 L233: ErpMfgCostRollup.class (daoFor) (manufacturing)
- 耦合点 L291: ErpMdMaterial.class (daoFor) (master-data)
  - id 行 L154: `layer.setOrgId(adjust.getOrgId());`
  - id 行 L155: `layer.setMaterialId(line.getMaterialId());`
  - id 行 L156: `layer.setSkuId(balance.getSkuId());`
  - id 行 L157: `layer.setWarehouseId(line.getWarehouseId());`
  - id 行 L164: `layer.setCurrencyId(line.getCurrencyId() != null ? line.getCurrencyId() : balance.getCurrencyId());`
  - id 行 L168: `layer.setIncomingMoveId(-line.getId());`
  - id 行 L169: `layer.setAcctSchemaId(null);`
  - id 行 L197: `q.addFilter(eq("incomingMoveId", -line.getId()));`
  - id 行 L208: `Long uomId = material != null ? material.getUoMId() : null;`
  - id 行 L213: `header.setOrgId(adjust.getOrgId());`
  - id 行 L221: `rollupLine.setCostRollupId(header.getId());`
  - id 行 L223: `rollupLine.setMaterialId(line.getMaterialId());`
  - id 行 L224: `rollupLine.setUoMId(uomId);`
  - id 行 L228: `rollupLine.setCurrencyId(line.getCurrencyId() != null ? line.getCurrencyId() : adjust.getCurrencyId());`
  - id 行 L243: `return "ROLLUP-CA-" + adjust.getCode() + "-" + line.getId();`
  - id 行 L253: `ledger.setOrgId(adjust.getOrgId());`
  - id 行 L254: `ledger.setMoveId(ErpInvConstants.LEDGER_MOVE_ID_COST_ADJUST);`
  - id 行 L255: `ledger.setMoveLineId(ErpInvConstants.LEDGER_MOVE_ID_COST_ADJUST);`
  - id 行 L256: `ledger.setMaterialId(line.getMaterialId());`
  - id 行 L257: `ledger.setSkuId(balance.getSkuId());`
  - id 行 L258: `ledger.setWarehouseId(line.getWarehouseId());`
  - id 行 L259: `ledger.setLocationId(balance.getLocationId());`
  - id 行 L266: `ledger.setCurrencyId(line.getCurrencyId() != null ? line.getCurrencyId() : balance.getCurrencyId());`
  - id 行 L274: `private ErpInvStockBalance findBalance(Long orgId, Long materialId, Long warehouseId, String batchNo) {`
  - id 行 L287: `private String resolveCostMethod(ErpInvStockBalance balance, Long materialId) {`

### module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/CostMethodResolver.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 3
- 耦合点 L7: ErpMdAcctSchema (master-data)
- 耦合点 L8: ErpMdMaterial (master-data)
- 耦合点 L61: ErpMdMaterial.class (daoFor) (master-data)
- 耦合点 L70: ErpMdAcctSchema.class (daoFor) (master-data)
  - id 行 L32: `public String resolve(ErpInvStockMoveLine line, Long acctSchemaId) {`
  - id 行 L57: `private String readMaterialCostMethod(Long materialId) {`
  - id 行 L66: `private String readAcctSchemaCostingMethod(Long acctSchemaId) {`

### module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/StandardCostResolver.java
- 外域: manufacturing, master-data  |  本文件 id-as-Long 证据行: 4
- 耦合点 L7: ErpMfgCostRollup (manufacturing)
- 耦合点 L8: ErpMfgCostRollupLine (manufacturing)
- 耦合点 L9: ErpMdMaterial (master-data)
- 耦合点 L83: ErpMfgCostRollup.class (daoFor) (manufacturing)
- 耦合点 L93: ErpMfgCostRollupLine.class (daoFor) (manufacturing)
- 耦合点 L107: ErpMdMaterial.class (daoFor) (master-data)
  - id 行 L65: `public BigDecimal resolve(Long materialId) {`
  - id 行 L81: `private BigDecimal resolveFromRollup(Long materialId) {`
  - id 行 L97: `.addFilter(eq("costRollupId", header.getId()))`
  - id 行 L106: `private BigDecimal resolveFromMaterialMaster(Long materialId) {`

### module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/dashboard/ErpInvDashboardBizModel.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 10
- 耦合点 L10: ErpMdMaterial (master-data)
- 耦合点 L11: ErpMdWarehouse (master-data)
- 耦合点 L152: ErpMdWarehouse.class (daoFor) (master-data)
- 耦合点 L372: ErpMdMaterial.class (daoFor) (master-data)
- 耦合点 L384: ErpMdMaterial.class (daoFor) (master-data)
- 耦合点 L396: ErpMdWarehouse.class (daoFor) (master-data)
  - id 行 L303: `moveIds.add(m.getId());`
  - id 行 L305: `incomingMoveIds.add(m.getId());`
  - id 行 L307: `outgoingMoveIds.add(m.getId());`
  - id 行 L330: `allMoveIds.add(m.getId());`
  - id 行 L332: `outgoingMoveIds.add(m.getId());`
  - id 行 L377: `map.put(m.getId(), DashboardUtil.nz(m.getSafetyStock()));`
  - id 行 L389: `map.put(m.getId(), m.getName());`
  - id 行 L401: `map.put(w.getId(), w.getName());`
  - id 行 L424: `for (ErpInvStockMove m : moves) moveIds.add(m.getId());`
  - id 行 L428: `moveDateByMoveId.put(m.getId(), m.getBusinessDate());`

### module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/CostAdjustmentPostingDispatcher.java
- 外域: finance  |  本文件 id-as-Long 证据行: 4
- 耦合点 L3: IErpFinVoucherBiz (finance)
  - id 行 L87: `event.setOrgId(adjust.getOrgId());`
  - id 行 L88: `event.setAcctSchemaId(resolveAcctSchemaId(adjust.getOrgId()));`
  - id 行 L89: `event.setCurrencyId(adjust.getCurrencyId());`
  - id 行 L114: `private Long resolveAcctSchemaId(Long orgId) {`

### module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/InvPostingExecutor.java
- 外域: finance  |  本文件 id-as-Long 证据行: 0
- 耦合点 L3: IErpFinVoucherBiz (finance)

### module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/LandedCostPostingDispatcher.java
- 外域: finance  |  本文件 id-as-Long 证据行: 4
- 耦合点 L3: IErpFinVoucherBiz (finance)
  - id 行 L100: `event.setOrgId(landedCost.getOrgId());`
  - id 行 L101: `event.setAcctSchemaId(resolveAcctSchemaId(landedCost.getOrgId()));`
  - id 行 L102: `event.setCurrencyId(landedCost.getCurrencyId());`
  - id 行 L141: `private Long resolveAcctSchemaId(Long orgId) {`

### module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvLandedCostApproveProcessor.java
- 外域: purchase  |  本文件 id-as-Long 证据行: 2
- 耦合点 L11: ErpPurReceive (purchase)
- 耦合点 L12: ErpPurReceiveLine (purchase)
  - id 行 L60: `List<ErpInvLandedCostLine> costLines = processor.loadCostLines(landedCost.getId());`
  - id 行 L69: `processor.validateNotAlreadyAllocated(landedCost.getReceiveId(), landedCost.getId());`

### module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvLandedCostGenerateFreightLandedCostProcessor.java
- 外域: purchase  |  本文件 id-as-Long 证据行: 3
- 耦合点 L5: ErpPurReceive (purchase)
  - id 行 L23: `Long freightCurrencyId, BigDecimal freightExchangeRate,`
  - id 行 L26: `facade.validateNoDraftExists(receive.getId());`
  - id 行 L28: `Long currencyId = freightCurrencyId != null ? freightCurrencyId : receive.getCurrencyId();`

### module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvLandedCostProcessor.java
- 外域: notify, purchase  |  本文件 id-as-Long 证据行: 31
- 耦合点 L13: IErpSysNotificationBiz (notify)
- 耦合点 L14: ErpPurReceive (purchase)
- 耦合点 L15: ErpPurReceiveLine (purchase)
- 耦合点 L228: ErpPurReceive.class (daoFor) (purchase)
- 耦合点 L466: ErpPurReceive.class (daoFor) (purchase)
- 耦合点 L470: ErpPurReceiveLine.class (daoFor) (purchase)
  - id 行 L100: `List<ErpInvLandedCostLine> costLines = loadCostLines(landedCost.getId());`
  - id 行 L186: `ErpInvLandedCost managed = landedCostDao().getEntityById(landedCost.getId());`
  - id 行 L198: `ErpInvCostAdjust managedAdjust = adjustDao.getEntityById(costAdjust.getId());`
  - id 行 L219: `protected List<ErpInvCostAdjustLine> loadAdjustLines(Long adjustId) {`
  - id 行 L235: `protected void validateNoDraftExists(Long receiveId) {`
  - id 行 L250: `protected BigDecimal resolveExchangeRate(BigDecimal freightExchangeRate, Long freightCurrencyId,`
  - id 行 L259: `Long currencyId, BigDecimal exchangeRate) {`
  - id 行 L263: `head.setOrgId(receive.getOrgId());`
  - id 行 L264: `head.setReceiveId(receive.getId());`
  - id 行 L265: `head.setSupplierId(receive.getSupplierId());`
  - id 行 L266: `head.setCurrencyId(currencyId);`
  - id 行 L278: `protected void createFreightLine(ErpInvLandedCost head, BigDecimal freightAmount, Long apPartnerId) {`
  - id 行 L281: `line.setLandedCostId(head.getId());`
  - id 行 L285: `line.setApPartnerId(apPartnerId);`
  - id 行 L305: `adjust.setOrgId(landedCost.getOrgId());`
  - id 行 L312: `adjust.setCurrencyId(landedCost.getCurrencyId());`
  - id 行 L320: `line.setAdjustId(adjust.getId());`
  - id 行 L322: `line.setMaterialId(r.getMaterialId());`
  - id 行 L323: `line.setWarehouseId(r.getWarehouseId() != null ? r.getWarehouseId() : receive.getWarehouseId());`
  - id 行 L327: `line.setCurrencyId(landedCost.getCurrencyId());`
  - id 行 L338: `adjust = adjustDao.getEntityById(adjust.getId());`
  - id 行 L350: `Long voucherId = postingDispatcher.tryPost(landedCost, costLines, allocations);`
  - id 行 L352: `landedCost = reload(landedCost.getId());`
  - id 行 L365: `ErpInvCostAdjust adjust = adjustDao.getEntityById(costAdjust.getId());`
  - id 行 L386: `.param(ErpInvErrors.ARG_RECEIVE_ID, receive.getId());`
  - id 行 L411: `protected void validateNotAlreadyAllocated(Long receiveId, Long currentLandedCostId) {`
  - id 行 L420: `Long siblingId = ((Number) row.get("id")).longValue();`
  - id 行 L454: `protected List<ErpInvLandedCostLine> loadCostLines(Long landedCostId) {`
  - id 行 L462: `protected ErpPurReceive loadReceive(Long receiveId) {`
  - id 行 L469: `protected List<ErpPurReceiveLine> loadReceiveLines(Long receiveId) {`
  - id 行 L495: `rl.getId(), rl.getMaterialId(), rl.getWarehouseId(),`

### module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvStockMoveProcessor.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 22
- 耦合点 L18: IErpMdMaterialBiz (master-data)
- 耦合点 L19: ErpMdMaterial (master-data)
  - id 行 L83: `public TraceChainResult forwardTrace(Long moveId, IServiceContext context) {`
  - id 行 L87: `public TraceChainResult backwardTrace(Long moveId, IServiceContext context) {`
  - id 行 L91: `public TraceChainResult returnTrace(Long moveId, IServiceContext context) {`
  - id 行 L118: `protected void doComplete(ErpInvStockMove move, List<ErpInvStockMoveLine> lines, Long acctSchemaId,`
  - id 行 L238: `move.setOrgId(request.getOrgId());`
  - id 行 L240: `move.setSourceWarehouseId(request.getSourceWarehouseId());`
  - id 行 L241: `move.setSourceLocationId(request.getSourceLocationId());`
  - id 行 L242: `move.setDestWarehouseId(request.getDestWarehouseId());`
  - id 行 L243: `move.setDestLocationId(request.getDestLocationId());`
  - id 行 L250: `move.setOriginMoveId(request.getOriginMoveId());`
  - id 行 L251: `move.setOriginReturnedMoveId(request.getOriginReturnedMoveId());`
  - id 行 L263: `line.setMoveId(move.getId());`
  - id 行 L265: `line.setMaterialId(req.getMaterialId());`
  - id 行 L266: `line.setSkuId(req.getSkuId());`
  - id 行 L267: `line.setUoMId(req.getUoMId());`
  - id 行 L273: `line.setCurrencyId(req.getCurrencyId() != null ? req.getCurrencyId() : request.getCurrencyId());`
  - id 行 L276: `line.setSourceLocationId(req.getSourceLocationId() != null ? req.getSourceLocationId()`
  - id 行 L278: `line.setDestLocationId(req.getDestLocationId() != null ? req.getDestLocationId()`
  - id 行 L288: `protected ErpInvStockMove requireMove(Long moveId, IServiceContext context) {`
  - id 行 L304: `protected List<ErpInvStockMoveLine> loadLines(Long moveId) {`
  - id 行 L322: `protected Long resolveReservationWarehouseId(ErpInvStockMove move) {`
  - id 行 L326: `protected Long resolveReservationLocationId(ErpInvStockMove move, ErpInvStockMoveLine line) {`

### module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvStockTakeCompleteTakeProcessor.java
- 外域: notify  |  本文件 id-as-Long 证据行: 18
- 耦合点 L14: IErpSysNotificationBiz (notify)
  - id 行 L79: `List<ErpInvStockTakeLine> lines = loadLines(take.getId());`
  - id 行 L86: `return finalizeComplete(take.getId());`
  - id 行 L98: `.param(ErpInvErrors.ARG_TAKE_ID, take.getId())`
  - id 行 L105: `protected List<ErpInvStockTakeLine> loadLines(Long takeId) {`
  - id 行 L172: `request.setOrgId(take.getOrgId());`
  - id 行 L175: `request.setDestWarehouseId(take.getWarehouseId());`
  - id 行 L176: `request.setDestLocationId(line.getLocationId());`
  - id 行 L178: `request.setSourceWarehouseId(take.getWarehouseId());`
  - id 行 L179: `request.setSourceLocationId(line.getLocationId());`
  - id 行 L185: `reqLine.setMaterialId(line.getMaterialId());`
  - id 行 L186: `reqLine.setSkuId(line.getSkuId());`
  - id 行 L187: `reqLine.setUoMId(line.getUoMId());`
  - id 行 L192: `reqLine.setDestLocationId(line.getLocationId());`
  - id 行 L194: `reqLine.setSourceLocationId(line.getLocationId());`
  - id 行 L223: `List<ErpInvStockMoveLine> candLines = loadMoveLines(candidate.getId());`
  - id 行 L246: `protected ErpInvStockTake finalizeComplete(Long takeId) {`
  - id 行 L268: `ctx.put("takeId", take.getId());`
  - id 行 L302: `protected List<ErpInvStockMoveLine> loadMoveLines(Long moveId) {`

### module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvTransferOrderConfirmProcessor.java
- 外域: finance  |  本文件 id-as-Long 证据行: 4
- 耦合点 L3: IErpFinIntercompanyTransferBiz (finance)
  - id 行 L29: `public ErpInvTransferOrder confirm(Long transferOrderId, IServiceContext context) {`
  - id 行 L38: `protected void validateDraft(ErpInvTransferOrder order, Long transferOrderId) {`
  - id 行 L57: `intercompanyTransferBiz.onTransferConfirmed(order.getId(), order.getFromWarehouseId(),`
  - id 行 L61: `.warn("intercompany posting failed for transfer {}: {}", order.getId(), e.getMessage());`

### module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/spi/ErpInvSkuReferenceChecker.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 12
- 耦合点 L4: ErpMdMaterialSku (master-data)
  - id 行 L70: `if (sku == null || sku.getId() == null) {`
  - id 行 L73: `Long skuId = sku.getId();`
  - id 行 L86: `private boolean existsStockBalance(Long skuId) {`
  - id 行 L95: `private boolean existsReservationLine(Long skuId) {`
  - id 行 L105: `private boolean existsCostLayer(Long skuId) {`
  - id 行 L114: `private boolean existsBatch(Long skuId) {`
  - id 行 L124: `private boolean existsSerialNumber(Long skuId) {`
  - id 行 L133: `private boolean existsStockMoveLine(Long skuId) {`
  - id 行 L142: `private boolean existsTransferOrderLine(Long skuId) {`
  - id 行 L151: `private boolean existsStockTakeLine(Long skuId) {`
  - id 行 L160: `private boolean existsOwnershipTransferLine(Long skuId) {`
  - id 行 L169: `private boolean existsPickingOrderLine(Long skuId) {`

## module-logistics （引用外域的耦合文件 3 个）

### module-logistics/erp-log-service/src/main/java/app/erp/log/service/gateway/GatewayDispatcher.java
- 外域: notify  |  本文件 id-as-Long 证据行: 10
- 耦合点 L19: IErpSysNotificationBiz (notify)
  - id 行 L68: `public ErpLogShipment advise(Long shipmentId) {`
  - id 行 L94: `public ErpLogShipment completeShipment(Long shipmentId, IServiceContext context) {`
  - id 行 L145: `public ErpLogShipment cancelShipment(Long shipmentId, IServiceContext context) {`
  - id 行 L282: `log.setShipmentId(shipment.getId());`
  - id 行 L283: `log.setGatewayId(resolveGatewayId(shipment));`
  - id 行 L293: `public ErpLogShipment loadShipment(Long shipmentId) {`
  - id 行 L315: `if (shipment.getId() == null) {`
  - id 行 L319: `deliveryBookingBiz.releaseForShipment(shipment.getId(), new ServiceContextImpl());`
  - id 行 L423: `log.setShipmentId(shipment.getId());`
  - id 行 L424: `log.setGatewayId(resolveGatewayId(shipment));`

### module-logistics/erp-log-service/src/main/java/app/erp/log/service/job/ErpLogDraftEscalationJob.java
- 外域: notify  |  本文件 id-as-Long 证据行: 2
- 耦合点 L7: IErpSysNotificationBiz (notify)
  - id 行 L114: `shipment.getId(), e.getMessage());`
  - id 行 L132: `map.put("shipmentId", shipment.getId());`

### module-logistics/erp-log-service/src/main/java/app/erp/log/service/processor/AbstractErpLogShipmentDeliveredProcessor.java
- 外域: finance, inventory, notify, sales  |  本文件 id-as-Long 证据行: 7
- 耦合点 L3: IErpFinVoucherBiz (finance)
- 耦合点 L6: IErpInvLandedCostBiz (inventory)
- 耦合点 L7: ErpInvLandedCost (inventory)
- 耦合点 L16: IErpSysNotificationBiz (notify)
- 耦合点 L17: IErpSalDeliveryBiz (sales)
- 耦合点 L18: IErpSalOrderBiz (sales)
- 耦合点 L19: ErpSalDelivery (sales)
- 耦合点 L20: ErpSalOrder (sales)
  - id 行 L121: `Long voucherId = voucherBiz.post(event, context);`
  - id 行 L246: `event.setOrgId(shipment.getOrgId());`
  - id 行 L247: `event.setAcctSchemaId(resolveAcctSchemaId(shipment.getOrgId()));`
  - id 行 L248: `event.setCurrencyId(shipment.getFreightCurrencyId());`
  - id 行 L265: `protected Long resolveAcctSchemaId(Long orgId) {`
  - id 行 L269: `protected Long resolveCarrierPartnerId(ErpLogShipment shipment) {`
  - id 行 L283: `ShipmentDeliveredEvent evt = new ShipmentDeliveredEvent(shipment.getId(), shipment.getCode(),`

## module-maintenance （引用外域的耦合文件 9 个）

### module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MaintenanceIssuePostingDispatcher.java
- 外域: finance, inventory, master-data, notify  |  本文件 id-as-Long 证据行: 10
- 耦合点 L3: IErpFinVoucherBiz (finance)
- 耦合点 L6: ErpFinVoucherBillR (finance)
- 耦合点 L7: ErpInvStockLedger (inventory)
- 耦合点 L8: ErpInvStockMove (inventory)
- 耦合点 L13: ErpMdAcctSchema (master-data)
- 耦合点 L14: ErpMdMaterial (master-data)
- 耦合点 L15: IErpSysNotificationBiz (notify)
- 耦合点 L211: ErpFinVoucherBillR.class (daoFor) (finance)
- 耦合点 L220: ErpInvStockMove.class (daoFor) (inventory)
- 耦合点 L231: ErpInvStockLedger.class (daoFor) (inventory)
- 耦合点 L242: ErpMdAcctSchema.class (daoFor) (master-data)
  - id 行 L90: `public void dispatchIfApplicable(Long sparePartUsageId) {`
  - id 行 L112: `List<ErpInvStockLedger> ledgers = loadLedgers(move.getId());`
  - id 行 L157: `event.setOrgId(usage.getOrgId());`
  - id 行 L159: `Long acctSchemaId = null;`
  - id 行 L160: `Long currencyId = null;`
  - id 行 L177: `event.setAcctSchemaId(acctSchemaId);`
  - id 行 L178: `event.setCurrencyId(currencyId);`
  - id 行 L229: `private List<ErpInvStockLedger> loadLedgers(Long moveId) {`
  - id 行 L237: `private Long resolveAcctSchemaId(Long orgId) {`
  - id 行 L241: `private Long resolveFunctionalCurrencyId(Long acctSchemaId) {`

### module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MaintenanceLaborPostingDispatcher.java
- 外域: finance, master-data, notify  |  本文件 id-as-Long 证据行: 9
- 耦合点 L3: IErpFinVoucherBiz (finance)
- 耦合点 L6: ErpFinVoucherBillR (finance)
- 耦合点 L11: ErpMdAcctSchema (master-data)
- 耦合点 L12: IErpSysNotificationBiz (notify)
- 耦合点 L232: ErpFinVoucherBillR.class (daoFor) (finance)
- 耦合点 L245: ErpMdAcctSchema.class (daoFor) (master-data)
  - id 行 L117: `Long voucherId = executor.postEvent(event);`
  - id 行 L199: `event.setOrgId(visit.getOrgId());`
  - id 行 L201: `Long acctSchemaId = resolveAcctSchemaId(visit.getOrgId());`
  - id 行 L202: `Long currencyId = acctSchemaId != null ? resolveFunctionalCurrencyId(acctSchemaId) : null;`
  - id 行 L203: `event.setAcctSchemaId(acctSchemaId);`
  - id 行 L204: `event.setCurrencyId(currencyId);`
  - id 行 L213: `Long equipmentId = visit.getEquipmentId();`
  - id 行 L240: `private Long resolveAcctSchemaId(Long orgId) {`
  - id 行 L244: `private Long resolveFunctionalCurrencyId(Long acctSchemaId) {`

### module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MntPostingExecutor.java
- 外域: finance  |  本文件 id-as-Long 证据行: 0
- 耦合点 L3: IErpFinVoucherBiz (finance)

### module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/processor/AbstractErpMntDowntimeEntryProcessor.java
- 外域: notify  |  本文件 id-as-Long 证据行: 6
- 耦合点 L9: IErpSysNotificationBiz (notify)
  - id 行 L48: `protected ErpMntDowntimeEntry requireDowntime(Long downtimeId, IServiceContext context) {`
  - id 行 L59: `.param(ErpMntErrors.ARG_DOWNTIME_ID, downtime.getId());`
  - id 行 L66: `.param(ErpMntErrors.ARG_DOWNTIME_ID, downtime.getId());`
  - id 行 L82: `ctx.put("downtimeId", downtime.getId());`
  - id 行 L85: `ctx.put("equipmentId", equipment.getId());`
  - id 行 L97: `eventType, downtime.getId(), e.getMessage());`

### module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/processor/AbstractErpMntSparePartUsageProcessor.java
- 外域: inventory  |  本文件 id-as-Long 证据行: 2
- 耦合点 L3: IErpInvStockMoveBiz (inventory)
- 耦合点 L4: ErpInvStockMove (inventory)
- 耦合点 L121: ErpInvStockMove.class (daoFor) (inventory)
  - id 行 L59: `protected ErpMntSparePartUsage requireUsage(Long usageId, IServiceContext context) {`
  - id 行 L144: `protected List<ErpMntSparePartUsageLine> loadLines(Long usageId) {`

### module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/processor/ErpMntSparePartUsageConfirmProcessor.java
- 外域: inventory  |  本文件 id-as-Long 证据行: 1
- 耦合点 L3: ErpInvStockMove (inventory)
  - id 行 L21: `public ErpMntSparePartUsage confirm(Long usageId, IServiceContext context) {`

### module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/processor/ErpMntSparePartUsageReverseConfirmProcessor.java
- 外域: inventory  |  本文件 id-as-Long 证据行: 2
- 耦合点 L3: ErpInvStockMove (inventory)
  - id 行 L23: `public ErpMntSparePartUsage reverseConfirm(Long usageId, IServiceContext context) {`
  - id 行 L46: `stockMoveBiz.reverse(originalMove.getId(), context);`

### module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/support/OeeCalculator.java
- 外域: manufacturing, quality  |  本文件 id-as-Long 证据行: 11
- 耦合点 L3: ErpMfgJobCard (manufacturing)
- 耦合点 L4: ErpMfgJobCardTimeLog (manufacturing)
- 耦合点 L5: ErpMfgWorkOrder (manufacturing)
- 耦合点 L6: ErpMfgWorkcenterCalendar (manufacturing)
- 耦合点 L7: ErpMfgWorkcenterCapacity (manufacturing)
- 耦合点 L10: ErpQaInspection (quality)
- 耦合点 L172: ErpMfgWorkcenterCalendar.class (daoFor) (manufacturing)
- 耦合点 L267: ErpMfgJobCard.class (daoFor) (manufacturing)
- 耦合点 L279: ErpMfgJobCardTimeLog.class (daoFor) (manufacturing)
- 耦合点 L296: ErpMfgWorkOrder.class (daoFor) (manufacturing)
- 耦合点 L311: ErpMfgWorkcenterCapacity.class (daoFor) (manufacturing)
- 耦合点 L340: ErpQaInspection.class (daoFor) (quality)
  - id 行 L71: `public Map<String, Object> computeOee(Long equipmentId, LocalDate dateFrom, LocalDate dateTo) {`
  - id 行 L82: `Long workcenterId = equipment.getWorkcenterId();`
  - id 行 L85: `result.put("equipmentId", equipment.getId());`
  - id 行 L94: `equipment.getId(), windowStart, windowEndExclusive);`
  - id 行 L98: `? computeDowntimeHours(equipment.getId(), windowStart, windowEndExclusive)`
  - id 行 L171: `protected BigDecimal computeCalendarHours(Long workcenterId, LocalDate dateFrom, LocalDate dateTo) {`
  - id 行 L246: `protected BigDecimal computeDowntimeHours(Long equipmentId, Timestamp windowStart, Timestamp windowEndExclusive) {`
  - id 行 L266: `protected OutputAggregate collectOutput(Long workcenterId, LocalDate dateFrom, LocalDate dateTo) {`
  - id 行 L273: `cardIds.add(card.getId());`
  - id 行 L310: `protected BigDecimal resolveCapacityPerHour(Long workcenterId, Set<Long> productIds) {`
  - id 行 L327: `Long productId = productIds.iterator().next();`

### module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/support/SparePartIssueService.java
- 外域: inventory  |  本文件 id-as-Long 证据行: 4
- 耦合点 L3: IErpInvStockMoveBiz (inventory)
- 耦合点 L4: StockMoveLineRequest (inventory)
- 耦合点 L5: StockMoveRequest (inventory)
- 耦合点 L6: ErpInvStockMove (inventory)
  - id 行 L37: `request.setOrgId(usage.getOrgId());`
  - id 行 L39: `request.setSourceWarehouseId(usage.getWarehouseId());`
  - id 行 L46: `lr.setMaterialId(line.getMaterialId());`
  - id 行 L47: `lr.setUoMId(line.getUoMId());`

## module-manufacturing （引用外域的耦合文件 23 个）

### module-manufacturing/erp-mfg-dao/src/main/java/app/erp/mfg/biz/IErpMfgMrpPlanLineBiz.java
- 外域: purchase  |  本文件 id-as-Long 证据行: 7
- 耦合点 L14: ErpPurOrder (purchase)
  - id 行 L21: `ErpMfgMrpPlanLine releasePurchaseRequest(@Name("planLineId") Long planLineId,`
  - id 行 L22: `@Name("supplierId") Long supplierId,`
  - id 行 L23: `@Name("currencyId") Long currencyId,`
  - id 行 L31: `ErpMfgMrpPlanLine releaseWorkRequest(@Name("planLineId") Long planLineId, IServiceContext context);`
  - id 行 L41: `ErpMfgMrpPlanLine releaseSubcontractRequest(@Name("planLineId") Long planLineId,`
  - id 行 L42: `@Name("supplierId") Long supplierId,`
  - id 行 L43: `@Name("currencyId") Long currencyId,`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/costing/CostRollupService.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 22
- 耦合点 L3: ErpMdMaterial (master-data)
- 耦合点 L4: ErpMdMaterialSku (master-data)
- 耦合点 L148: ErpMdMaterial.class (daoFor) (master-data)
- 耦合点 L307: ErpMdMaterialSku.class (daoFor) (master-data)
  - id 行 L93: `public CostRollupResult rollup(Long bomId) {`
  - id 行 L100: `writeLines(head.getId(), computed);`
  - id 行 L109: `head.setCode("ROLLUP-" + today.toString() + "-" + bom.getId());`
  - id 行 L117: `private void writeLines(Long rollupId, Map<Long, CostBreakdown> computed) {`
  - id 行 L123: `line.setCostRollupId(rollupId);`
  - id 行 L125: `line.setMaterialId(e.getKey());`
  - id 行 L126: `line.setUoMId(cb.uoMId);`
  - id 行 L138: `private CostBreakdown computeUnit(Long materialId, Map<Long, CostBreakdown> computed, Set<Long> path) {`
  - id 行 L170: `for (ErpMfgBomLine line : loadLines(bom.getId())) {`
  - id 行 L175: `OperationCost oc = sumOperationCost(bom.getId());`
  - id 行 L190: `private OperationCost sumOperationCost(Long bomId) {`
  - id 行 L194: `Long wcId = op.getWorkcenterId();`
  - id 行 L265: `BigDecimal aggregateSubcontractCost(Long materialId) {`
  - id 行 L280: `for (ErpMfgSubcontractOrderLine line : loadSubcontractLines(order.getId(), materialId)) {`
  - id 行 L295: `private List<ErpMfgSubcontractOrderLine> loadSubcontractLines(Long orderId, Long materialId) {`
  - id 行 L302: `private BigDecimal defaultSkuPurchasePrice(Long materialId) {`
  - id 行 L314: `private ErpMfgBom requireBom(Long bomId) {`
  - id 行 L325: `private List<ErpMfgBomLine> loadLines(Long bomId) {`
  - id 行 L332: `private List<ErpMfgBomOperation> loadOperations(Long bomId) {`
  - id 行 L341: `result.setRollupId(head.getId());`
  - id 行 L346: `v.setMaterialId(e.getKey());`
  - id 行 L379: `Long uoMId;`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/costing/ProductionVarianceCalculator.java
- 外域: notify  |  本文件 id-as-Long 证据行: 22
- 耦合点 L17: IErpSysNotificationBiz (notify)
  - id 行 L109: `public List<ErpMfgCostVariance> calculateVariances(Long workOrderId) {`
  - id 行 L111: `Long productId = wo.getProductId();`
  - id 行 L125: `Long bomId = wo.getBomId();`
  - id 行 L126: `Long workcenterId = resolvePrimaryWorkcenterId(wo, bomId);`
  - id 行 L251: `ctx.put("workOrderId", wo.getId());`
  - id 行 L262: `wo.getId(), e.getMessage());`
  - id 行 L286: `public void deleteByWorkOrder(Long workOrderId) {`
  - id 行 L297: `public List<ErpMfgCostVariance> findByWorkOrder(Long workOrderId) {`
  - id 行 L305: `private ErpMfgCostVariance buildLine(Long workOrderId, int lineNo, String varianceType, String costElement,`
  - id 行 L306: `Long materialId, Long workcenterId, LocalDate bizDate,`
  - id 行 L312: `line.setWorkOrderId(workOrderId);`
  - id 行 L316: `line.setMaterialId(materialId);`
  - id 行 L326: `line.setWorkcenterId(workcenterId);`
  - id 行 L332: `private ErpMfgWorkOrder requireWorkOrder(Long workOrderId) {`
  - id 行 L345: `private ErpMfgCostRollupLine findFirmedRollupLine(Long productId) {`
  - id 行 L363: `.addFilter(eq("costRollupId", header.getId()))`
  - id 行 L376: `private BigDecimal sumBomOperationStandardMins(ErpMfgWorkOrder wo, Long bomId) {`
  - id 行 L397: `private BigDecimal sumJobCardActualMins(Long workOrderId) {`
  - id 行 L413: `private BigDecimal deriveStandardLaborRate(ErpMfgWorkOrder wo, Long bomId,`
  - id 行 L420: `Long wcId = op.getWorkcenterId();`
  - id 行 L439: `Long wcId = op.getWorkcenterId();`
  - id 行 L459: `private Long resolvePrimaryWorkcenterId(ErpMfgWorkOrder wo, Long bomId) {`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/entity/ErpMfgBatchGenealogyBizModel.java
- 外域: inventory  |  本文件 id-as-Long 证据行: 10
- 耦合点 L4: ErpInvBatch (inventory)
- 耦合点 L143: ErpInvBatch.class (daoFor) (inventory)
  - id 行 L46: `public List<ErpMfgBatchGenealogy> forwardTrace(@Name("outputLotId") Long outputLotId,`
  - id 行 L54: `public List<ErpMfgBatchGenealogy> backwardTrace(@Name("inputLotId") Long inputLotId,`
  - id 行 L62: `public List<ErpMfgBatchGenealogy> traceChain(@Name("lotId") Long lotId,`
  - id 行 L72: `public RecallReport recallReport(@Name("lotId") Long lotId, IServiceContext context) {`
  - id 行 L75: `report.setSourceLotId(lotId);`
  - id 行 L94: `Long outputLotId = edge.getOutputLotId();`
  - id 行 L109: `protected void collectAffectedIfFinishedGood(Long lotId, RecallReport report) {`
  - id 行 L122: `affected.setLotId(lotId);`
  - id 行 L124: `affected.setMaterialId(lot.getMaterialId());`
  - id 行 L130: `protected void requireLot(Long lotId, IServiceContext context) {`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/entity/MaterialIssueStockMoveBuilder.java
- 外域: inventory, master-data  |  本文件 id-as-Long 证据行: 11
- 耦合点 L3: StockMoveLineRequest (inventory)
- 耦合点 L4: StockMoveRequest (inventory)
- 耦合点 L5: IErpMdAcctSchemaBiz (master-data)
- 耦合点 L6: ErpMdAcctSchema (master-data)
  - id 行 L36: `request.setOrgId(issue.getOrgId());`
  - id 行 L39: `request.setSourceWarehouseId(issue.getWarehouseId());`
  - id 行 L40: `request.setSourceLocationId(null);`
  - id 行 L41: `request.setAcctSchemaId(resolveAcctSchemaId(issue.getOrgId(), context));`
  - id 行 L42: `request.setCurrencyId(issue.getCurrencyId());`
  - id 行 L49: `private Long resolveAcctSchemaId(Long orgId, IServiceContext context) {`
  - id 行 L54: `return schema == null ? null : schema.getId();`
  - id 行 L61: `req.setMaterialId(line.getMaterialId());`
  - id 行 L62: `req.setSkuId(line.getSkuId());`
  - id 行 L63: `req.setUoMId(line.getUoMId());`
  - id 行 L67: `req.setSourceLocationId(line.getLocationId());`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/genealogy/BatchGenealogyWriter.java
- 外域: inventory, notify  |  本文件 id-as-Long 证据行: 23
- 耦合点 L3: ErpInvBatch (inventory)
- 耦合点 L10: IErpSysNotificationBiz (notify)
- 耦合点 L305: ErpInvBatch.class (daoFor) (inventory)
  - id 行 L103: `ctx.put("workOrderId", wo.getId());`
  - id 行 L120: `ErpMfgWorkOrderLine outputLine = findOutputLine(wo.getId());`
  - id 行 L124: `Long productId = wo.getProductId();`
  - id 行 L128: `Long uomId = outputLine.getUoMId();`
  - id 行 L129: `Long warehouseId = outputLine.getDestWarehouseId();`
  - id 行 L134: `List<ErpMfgMaterialIssueLine> issueLines = findIssueLinesWithBatch(wo.getId());`
  - id 行 L158: `if (!usedInputLots.add(inputLot.getId())) {`
  - id 行 L167: `row.setWorkOrderId(wo.getId());`
  - id 行 L168: `row.setInputLotId(inputLot.getId());`
  - id 行 L169: `row.setInputMaterialId(issueLine.getMaterialId());`
  - id 行 L171: `row.setInputUoMId(issueLine.getUoMId());`
  - id 行 L172: `row.setOutputLotId(outputLot.getId());`
  - id 行 L173: `row.setOutputMaterialId(productId);`
  - id 行 L175: `row.setOutputUoMId(uomId);`
  - id 行 L188: `protected ErpInvBatch ensureOutputLot(ErpMfgWorkOrder wo, Long productId, Long warehouseId,`
  - id 行 L199: `batch.setOrgId(wo.getOrgId());`
  - id 行 L201: `batch.setMaterialId(productId);`
  - id 行 L202: `batch.setWarehouseId(warehouseId);`
  - id 行 L213: `protected ErpInvBatch resolveInputLot(ErpMfgMaterialIssueLine issueLine, Long warehouseId) {`
  - id 行 L223: `protected List<ErpMfgMaterialIssueLine> findIssueLinesWithBatch(Long workOrderId) {`
  - id 行 L238: `lq.addFilter(eq("issueId", issue.getId()));`
  - id 行 L256: `protected ErpMfgWorkOrderLine findOutputLine(Long workOrderId) {`
  - id 行 L266: `protected ErpInvBatch findBatchByNo(String batchNo, Long materialId, Long warehouseId) {`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/mrp/DemandAggregator.java
- 外域: inventory, master-data, sales  |  本文件 id-as-Long 证据行: 16
- 耦合点 L3: ErpInvStockBalance (inventory)
- 耦合点 L4: ErpMdMaterial (master-data)
- 耦合点 L11: ErpSalOrder (sales)
- 耦合点 L12: ErpSalOrderLine (sales)
- 耦合点 L86: ErpSalOrder.class (daoFor) (sales)
- 耦合点 L87: ErpSalOrderLine.class (daoFor) (sales)
- 耦合点 L125: ErpMdMaterial.class (daoFor) (master-data)
- 耦合点 L238: ErpInvStockBalance.class (daoFor) (inventory)
  - id 行 L68: `public List<ErpMfgMrpDemand> aggregate(Long planId) {`
  - id 行 L98: `lq.addFilter(eq("orderId", order.getId()));`
  - id 行 L108: `demand.setMaterialId(line.getMaterialId());`
  - id 行 L109: `demand.setUoMId(line.getUoMId());`
  - id 行 L132: `BigDecimal available = availableQuantity(material.getId(), plan.getOrgId());`
  - id 行 L138: `demand.setMaterialId(material.getId());`
  - id 行 L139: `demand.setUoMId(material.getUoMId());`
  - id 行 L186: `headIds.add(h.getId());`
  - id 行 L215: `Long materialId = e.getKey();`
  - id 行 L218: `demand.setMaterialId(materialId);`
  - id 行 L219: `demand.setUoMId(materialUoM.get(materialId));`
  - id 行 L232: `private BigDecimal availableQuantity(Long materialId, Long orgId) {`
  - id 行 L252: `demand.setMrpPlanId(plan.getId());`
  - id 行 L257: `private List<ErpMfgMrpDemand> clearSynthesized(IEntityDao<ErpMfgMrpDemand> dao, Long planId) {`
  - id 行 L273: `private int nextLineNo(IEntityDao<ErpMfgMrpDemand> dao, Long planId) {`
  - id 行 L285: `private ErpMfgMrpPlan requirePlan(Long planId) {`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/mrp/MrpEngine.java
- 外域: inventory, master-data  |  本文件 id-as-Long 证据行: 19
- 耦合点 L3: ErpInvStockBalance (inventory)
- 耦合点 L4: ErpMdMaterial (master-data)
- 耦合点 L196: ErpMdMaterial.class (daoFor) (master-data)
- 耦合点 L207: ErpMdMaterial.class (daoFor) (master-data)
- 耦合点 L217: ErpInvStockBalance.class (daoFor) (inventory)
  - id 行 L79: `public void runMrp(Long planId, List<ErpMfgMrpDemand> demands) {`
  - id 行 L106: `private void processMaterial(ErpMfgMrpPlan plan, Long materialId, BigDecimal grossQty, Long uoMId,`
  - id 行 L107: `LocalDate requirementDate, Long parentLineId, Set<Long> path,`
  - id 行 L129: `long leadDays = manufactured ? mfgLeadDays(bom.getId()) : purLeadDays(materialId);`
  - id 行 L133: `line.setMrpPlanId(plan.getId());`
  - id 行 L135: `line.setMaterialId(materialId);`
  - id 行 L136: `line.setUoMId(resolveUoM(uoMId, materialId));`
  - id 行 L145: `line.setParentLineId(parentLineId);`
  - id 行 L152: `List<BomExplosionNode> children = bomExpander.explode(bom.getId(), planned, false);`
  - id 行 L155: `plannedDate, line.getId(), path, lineDao, lineNo);`
  - id 行 L177: `private long mfgLeadDays(Long bomId) {`
  - id 行 L195: `private long purLeadDays(Long materialId) {`
  - id 行 L203: `private Long resolveUoM(Long uoMId, Long materialId) {`
  - id 行 L211: `private BigDecimal availableQuantity(Long materialId, Long orgId) {`
  - id 行 L247: `private void clearLines(IEntityDao<ErpMfgMrpPlanLine> dao, Long planId) {`
  - id 行 L256: `private ErpMfgMrpPlan requirePlan(Long planId) {`
  - id 行 L272: `final Long materialId;`
  - id 行 L274: `Long uoMId;`
  - id 行 L277: `TopDemand(Long materialId) {`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/mrp/MrpReleaseService.java
- 外域: purchase  |  本文件 id-as-Long 证据行: 34
- 耦合点 L13: ErpPurOrder (purchase)
- 耦合点 L14: ErpPurOrderLine (purchase)
- 耦合点 L142: ErpPurOrder.class (daoFor) (purchase)
- 耦合点 L158: ErpPurOrderLine.class (daoFor) (purchase)
  - id 行 L72: `public String releasePurchaseRequest(Long planLineId, Long supplierId, Long currencyId) {`
  - id 行 L88: `public String releaseWorkRequest(Long planLineId) {`
  - id 行 L103: `public String releaseSubcontractRequest(Long planLineId, Long supplierId, Long currencyId) {`
  - id 行 L120: `private ErpMfgMrpPlanLine requireReleasable(Long planLineId, String expectedOrderType) {`
  - id 行 L140: `private String releaseToPurchaseOrder(ErpMfgMrpPlanLine line, ErpMfgMrpPlan plan, Long supplierId,`
  - id 行 L141: `Long currencyId, LocalDate today) {`
  - id 行 L144: `String code = ErpMfgConstants.RELEASE_PO_CODE_PREFIX + line.getId();`
  - id 行 L146: `order.setOrgId(plan != null ? plan.getOrgId() : null);`
  - id 行 L147: `order.setSupplierId(supplierId);`
  - id 行 L148: `order.setCurrencyId(currencyId);`
  - id 行 L156: `flushReleaseOrThrow(line.getId());`
  - id 行 L160: `poLine.setOrderId(order.getId());`
  - id 行 L162: `poLine.setMaterialId(line.getMaterialId());`
  - id 行 L163: `poLine.setUoMId(line.getUoMId());`
  - id 行 L174: `String code = ErpMfgConstants.RELEASE_WO_CODE_PREFIX + line.getId();`
  - id 行 L176: `wo.setProductId(line.getMaterialId());`
  - id 行 L179: `wo.setBomId(defaultBom.getId());`
  - id 行 L184: `wo.setOrgId(plan != null ? plan.getOrgId() : null);`
  - id 行 L188: `flushReleaseOrThrow(line.getId());`
  - id 行 L192: `private String releaseToSubcontractOrder(ErpMfgMrpPlanLine line, ErpMfgMrpPlan plan, Long supplierId,`
  - id 行 L193: `Long currencyId, LocalDate today) {`
  - id 行 L196: `String code = ErpMfgConstants.RELEASE_SUBCONTRACT_CODE_PREFIX + line.getId();`
  - id 行 L198: `order.setOrgId(plan != null ? plan.getOrgId() : null);`
  - id 行 L199: `order.setSupplierId(supplierId);`
  - id 行 L200: `order.setProductId(line.getMaterialId());`
  - id 行 L201: `order.setCurrencyId(currencyId);`
  - id 行 L211: `flushReleaseOrThrow(line.getId());`
  - id 行 L215: `subLine.setSubcontractOrderId(order.getId());`
  - id 行 L217: `subLine.setMaterialId(line.getMaterialId());`
  - id 行 L218: `subLine.setUoMId(line.getUoMId());`
  - id 行 L231: `q.addFilter(eq("mrpPlanId", plan.getId()));`
  - id 行 L253: `private ErpMfgBom findDefaultBomOrNull(Long productId) {`
  - id 行 L266: `private ErpMfgMrpPlanLine requireLine(Long planLineId) {`
  - id 行 L289: `private void flushReleaseOrThrow(Long planLineId) {`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/ManufacturingIssuePostingDispatcher.java
- 外域: inventory, master-data  |  本文件 id-as-Long 证据行: 10
- 耦合点 L5: IErpInvStockLedgerBiz (inventory)
- 耦合点 L6: ErpInvStockLedger (inventory)
- 耦合点 L7: ErpInvStockMove (inventory)
- 耦合点 L13: ErpMdMaterial (master-data)
- 耦合点 L163: ErpInvStockMove.class (daoFor) (inventory)
- 耦合点 L174: ErpInvStockLedger.class (daoFor) (inventory)
  - id 行 L79: `public void dispatchIfApplicable(Long materialIssueId) {`
  - id 行 L95: `List<ErpInvStockLedger> ledgers = loadLedgers(move.getId());`
  - id 行 L103: `Long voucherId = executor.postEvent(event);`
  - id 行 L122: `event.setOrgId(issue.getOrgId());`
  - id 行 L123: `event.setAcctSchemaId(resolveAcctSchemaId(issue.getOrgId()));`
  - id 行 L124: `event.setCurrencyId(issue.getCurrencyId());`
  - id 行 L154: `private void markIssuePosted(Long issueId) {`
  - id 行 L172: `private List<ErpInvStockLedger> loadLedgers(Long moveId) {`
  - id 行 L180: `private List<ErpMfgMaterialIssueLine> loadIssueLines(Long issueId) {`
  - id 行 L187: `private Long resolveAcctSchemaId(Long orgId) {`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/MfgPostingExecutor.java
- 外域: finance  |  本文件 id-as-Long 证据行: 0
- 耦合点 L3: IErpFinVoucherBiz (finance)

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/SubcontractPostingDispatcher.java
- 外域: inventory, master-data, notify  |  本文件 id-as-Long 证据行: 20
- 耦合点 L5: IErpInvStockLedgerBiz (inventory)
- 耦合点 L6: ErpInvStockLedger (inventory)
- 耦合点 L7: ErpInvStockMove (inventory)
- 耦合点 L12: ErpMdMaterial (master-data)
- 耦合点 L13: IErpSysNotificationBiz (notify)
- 耦合点 L291: ErpInvStockMove.class (daoFor) (inventory)
- 耦合点 L302: ErpInvStockLedger.class (daoFor) (inventory)
  - id 行 L85: `public void dispatchIssuePosting(Long subcontractOrderId) {`
  - id 行 L94: `List<ErpInvStockLedger> ledgers = loadLedgers(move.getId());`
  - id 行 L106: `public void dispatchReceiptPosting(Long subcontractOrderId) {`
  - id 行 L115: `List<ErpInvStockLedger> ledgers = loadLedgers(move.getId());`
  - id 行 L127: `public void dispatchFeePosting(Long subcontractOrderId) {`
  - id 行 L141: `Long voucherId = executor.postEvent(event);`
  - id 行 L195: `event.setOrgId(order.getOrgId());`
  - id 行 L196: `event.setAcctSchemaId(resolveAcctSchemaId(order.getOrgId()));`
  - id 行 L197: `event.setCurrencyId(order.getCurrencyId());`
  - id 行 L229: `event.setOrgId(order.getOrgId());`
  - id 行 L230: `event.setAcctSchemaId(resolveAcctSchemaId(order.getOrgId()));`
  - id 行 L231: `event.setCurrencyId(order.getCurrencyId());`
  - id 行 L263: `event.setOrgId(order.getOrgId());`
  - id 行 L264: `event.setAcctSchemaId(resolveAcctSchemaId(order.getOrgId()));`
  - id 行 L265: `event.setCurrencyId(order.getCurrencyId());`
  - id 行 L278: `private void markPosted(Long orderId) {`
  - id 行 L286: `private ErpMfgSubcontractOrder loadOrder(Long orderId) {`
  - id 行 L300: `private List<ErpInvStockLedger> loadLedgers(Long moveId) {`
  - id 行 L308: `private List<ErpMfgSubcontractOrderLine> loadLines(Long orderId) {`
  - id 行 L315: `private Long resolveAcctSchemaId(Long orgId) {`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/AbstractErpMfgMaterialIssueProcessor.java
- 外域: inventory  |  本文件 id-as-Long 证据行: 2
- 耦合点 L3: IErpInvStockLedgerBiz (inventory)
- 耦合点 L4: IErpInvStockMoveBiz (inventory)
- 耦合点 L5: ErpInvStockMove (inventory)
- 耦合点 L113: ErpInvStockMove.class (daoFor) (inventory)
  - id 行 L63: `protected ErpMfgMaterialIssue requireIssue(Long issueId, IServiceContext context) {`
  - id 行 L100: `protected List<ErpMfgMaterialIssueLine> loadLines(Long issueId) {`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgMaterialIssueConfirmProcessor.java
- 外域: inventory  |  本文件 id-as-Long 证据行: 10
- 耦合点 L3: IErpInvReservationBiz (inventory)
- 耦合点 L4: ReservationConsumeLine (inventory)
- 耦合点 L5: ReservationConsumeRequest (inventory)
- 耦合点 L6: StockMoveRequest (inventory)
- 耦合点 L7: ErpInvReservation (inventory)
- 耦合点 L8: ErpInvReservationLine (inventory)
- 耦合点 L9: ErpInvStockLedger (inventory)
- 耦合点 L10: ErpInvStockMove (inventory)
- 耦合点 L177: ErpInvReservation.class (daoFor) (inventory)
- 耦合点 L185: ErpInvReservationLine.class (daoFor) (inventory)
  - id 行 L48: `public ErpMfgMaterialIssue confirm(Long issueId, IServiceContext context) {`
  - id 行 L107: `Long workOrderId = issue.getWorkOrderId();`
  - id 行 L119: `List<ErpInvReservationLine> reservationLines = findReservationLines(reservation.getId());`
  - id 行 L138: `consume.setMaterialId(line.getMaterialId());`
  - id 行 L139: `consume.setWarehouseId(issue.getWarehouseId());`
  - id 行 L140: `consume.setLocationId(line.getLocationId());`
  - id 行 L157: `Long materialId, BigDecimal issued) {`
  - id 行 L181: `protected List<ErpInvReservationLine> findReservationLines(Long reservationId) {`
  - id 行 L239: `q.addFilter(eq("moveId", move.getId()));`
  - id 行 L248: `protected void applyMaterialCostToWorkOrder(Long workOrderId, BigDecimal materialCostDelta, IServiceContext context) {`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgMaterialIssueReverseConfirmProcessor.java
- 外域: inventory  |  本文件 id-as-Long 证据行: 2
- 耦合点 L3: ErpInvStockMove (inventory)
  - id 行 L20: `public ErpMfgMaterialIssue reverseConfirm(Long issueId, IServiceContext context) {`
  - id 行 L42: `stockMoveBiz.reverse(originalMove.getId(), context);`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgScheduleToJobCardProcessor.java
- 外域: maintenance  |  本文件 id-as-Long 证据行: 13
- 耦合点 L9: IErpMntDowntimeEntryBiz (maintenance)
- 耦合点 L10: MntOpenDowntimeWindow (maintenance)
  - id 行 L117: `wo.getId(), wo.getCode(), slot.getWorkcenterId());`
  - id 行 L139: `ids.add(wo.getId());`
  - id 行 L154: `if (!scheduledIds.contains(wo.getId())) {`
  - id 行 L157: `if (alreadyHasJobCards.contains(wo.getId())) {`
  - id 行 L200: `Long srcId = slot.getOperationOrderId();`
  - id 行 L216: `jc.setWorkOrderId(wo.getId());`
  - id 行 L217: `jc.setWorkcenterId(slot.getWorkcenterId());`
  - id 行 L221: `jc.setSourceScheduleId(slot.getOperationOrderId());`
  - id 行 L232: `Long firstId = slots.get(0).getOperationOrderId();`
  - id 行 L234: `wo.setSourceScheduleId(firstId);`
  - id 行 L240: `String prefix = wo.getCode() != null ? wo.getCode() : ("WO" + wo.getId());`
  - id 行 L247: `protected ErpMfgWorkOrder requireWorkOrder(Long workOrderId) {`
  - id 行 L275: `protected List<ErpMfgJobCard> findJobCardsForWorkOrder(Long workOrderId) {`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgSubcontractOrderProcessor.java
- 外域: finance, inventory, master-data  |  本文件 id-as-Long 证据行: 23
- 耦合点 L3: IErpFinVoucherBiz (finance)
- 耦合点 L5: IErpInvStockMoveBiz (inventory)
- 耦合点 L6: StockMoveLineRequest (inventory)
- 耦合点 L7: StockMoveRequest (inventory)
- 耦合点 L8: ErpInvStockMove (inventory)
- 耦合点 L20: ErpMdMaterial (master-data)
- 耦合点 L456: ErpMdMaterial.class (daoFor) (master-data)
  - id 行 L127: `public ErpMfgSubcontractOrder cancel(Long subcontractOrderId, IServiceContext context) {`
  - id 行 L202: `stockMoveBiz.reverse(original.getId(), context);`
  - id 行 L335: `List<ErpMfgSubcontractOrderLine> lines, Long sourceWarehouseId,`
  - id 行 L339: `request.setOrgId(order.getOrgId());`
  - id 行 L341: `request.setSourceWarehouseId(sourceWarehouseId);`
  - id 行 L342: `request.setCurrencyId(order.getCurrencyId());`
  - id 行 L343: `request.setAcctSchemaId(resolveAcctSchemaId(order.getOrgId()));`
  - id 行 L350: `ml.setMaterialId(line.getMaterialId());`
  - id 行 L351: `ml.setUoMId(line.getUoMId());`
  - id 行 L353: `ml.setCurrencyId(order.getCurrencyId());`
  - id 行 L365: `Long destWarehouseId, IServiceContext context) {`
  - id 行 L368: `request.setOrgId(order.getOrgId());`
  - id 行 L370: `request.setDestWarehouseId(destWarehouseId);`
  - id 行 L371: `request.setCurrencyId(order.getCurrencyId());`
  - id 行 L372: `request.setAcctSchemaId(resolveAcctSchemaId(order.getOrgId()));`
  - id 行 L380: `ml.setMaterialId(order.getProductId());`
  - id 行 L381: `Long uomId = resolveProductUomId(order.getProductId());`
  - id 行 L382: `ml.setUoMId(uomId);`
  - id 行 L385: `ml.setCurrencyId(order.getCurrencyId());`
  - id 行 L437: `protected List<ErpMfgSubcontractOrderLine> loadLines(Long subcontractOrderId) {`
  - id 行 L444: `protected BigDecimal sumLineQuantity(Long subcontractOrderId) {`
  - id 行 L452: `protected Long resolveProductUomId(Long productId) {`
  - id 行 L460: `protected Long resolveAcctSchemaId(Long orgId) {`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgWorkOrderProcessor.java
- 外域: inventory, master-data, notify, quality  |  本文件 id-as-Long 证据行: 40
- 耦合点 L3: IErpInvReservationBiz (inventory)
- 耦合点 L4: IErpInvStockMoveBiz (inventory)
- 耦合点 L5: ReservationCreateRequest (inventory)
- 耦合点 L6: ReservationLineRequest (inventory)
- 耦合点 L7: StockMoveLineRequest (inventory)
- 耦合点 L8: StockMoveRequest (inventory)
- 耦合点 L32: ErpMdMaterial (master-data)
- 耦合点 L33: IErpSysNotificationBiz (notify)
- 耦合点 L34: IErpQaInspectionBiz (quality)
- 耦合点 L35: InspectionTrigger (quality)
- 耦合点 L384: ErpMdMaterial.class (daoFor) (master-data)
- 耦合点 L707: ErpMdMaterial.class (daoFor) (master-data)
  - id 行 L132: `public ErpMfgWorkOrder checkAvailability(Long workOrderId, IServiceContext context) {`
  - id 行 L148: `public ErpMfgWorkOrder cancel(Long workOrderId, IServiceContext context) {`
  - id 行 L189: `ctx.put("workOrderId", wo.getId());`
  - id 行 L376: `ErpMfgWorkOrderLine outputLine = findOutputLine(wo.getId());`
  - id 行 L377: `Long destWarehouseId = outputLine != null ? outputLine.getDestWarehouseId() : null;`
  - id 行 L381: `Long productId = wo.getProductId();`
  - id 行 L382: `Long uomId = outputLine != null ? outputLine.getUoMId() : null;`
  - id 行 L392: `request.setOrgId(wo.getOrgId());`
  - id 行 L394: `request.setDestWarehouseId(destWarehouseId);`
  - id 行 L395: `request.setCurrencyId(wo.getCurrencyId());`
  - id 行 L396: `request.setAcctSchemaId(resolveAcctSchemaId(wo.getOrgId()));`
  - id 行 L400: `line.setMaterialId(productId);`
  - id 行 L401: `line.setUoMId(uomId);`
  - id 行 L404: `line.setCurrencyId(wo.getCurrencyId());`
  - id 行 L472: `protected ErpMfgWorkOrderLine findOutputLine(Long workOrderId) {`
  - id 行 L486: `protected Long resolveAcctSchemaId(Long orgId) {`
  - id 行 L541: `snap.setWorkOrderId(wo.getId());`
  - id 行 L542: `snap.setBomId(bom.getId());`
  - id 行 L543: `snap.setProductId(bom.getProductId());`
  - id 行 L548: `for (ErpMfgBomLine line : bomExpander.loadLines(bom.getId())) {`
  - id 行 L550: `sl.setSnapshotId(snap.getId());`
  - id 行 L552: `sl.setMaterialId(line.getMaterialId());`
  - id 行 L553: `sl.setSkuId(line.getSkuId());`
  - id 行 L554: `sl.setUoMId(line.getUoMId());`
  - id 行 L556: `sl.setOperationId(line.getOperationId());`
  - id 行 L558: `sl.setWarehouseId(line.getWarehouseId());`
  - id 行 L559: `sl.setAlternativeMaterialId(line.getAlternativeMaterialId());`
  - id 行 L563: `for (ErpMfgBomOperation op : bomExpander.loadOperations(bom.getId())) {`
  - id 行 L565: `so.setSnapshotId(snap.getId());`
  - id 行 L567: `so.setOperationId(op.getOperationId());`
  - id 行 L568: `so.setWorkcenterId(op.getWorkcenterId());`
  - id 行 L574: `wo.setSnapshotBomId(snap.getId());`
  - id 行 L610: `Long bomId;`
  - id 行 L646: `request.setOrgId(wo.getOrgId());`
  - id 行 L652: `Long materialId = e.getKey();`
  - id 行 L653: `Long warehouseId = warehouseByMaterial.get(materialId);`
  - id 行 L659: `line.setMaterialId(materialId);`
  - id 行 L660: `line.setWarehouseId(warehouseId);`
  - id 行 L663: `line.setUomId(resolveReservationUom(materialId, uomByMaterial.get(materialId)));`
  - id 行 L703: `protected Long resolveReservationUom(Long materialId, Long woLineUomId) {`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgWorkOrderReportCompletionProcessor.java
- 外域: quality  |  本文件 id-as-Long 证据行: 1
- 耦合点 L6: InspectionTrigger (quality)
  - id 行 L31: `public ErpMfgWorkOrder reportCompletion(Long workOrderId, BigDecimal completedQty, IServiceContext context) {`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/simulation/SimulationMrpEngine.java
- 外域: inventory, master-data  |  本文件 id-as-Long 证据行: 41
- 耦合点 L3: ErpInvStockBalance (inventory)
- 耦合点 L4: ErpMdMaterial (master-data)
- 耦合点 L278: ErpMdMaterial.class (daoFor) (master-data)
- 耦合点 L412: ErpMdMaterial.class (daoFor) (master-data)
- 耦合点 L423: ErpMdMaterial.class (daoFor) (master-data)
- 耦合点 L433: ErpInvStockBalance.class (daoFor) (inventory)
  - id 行 L99: `public ErpMfgMrpScenarioVersion runSimulation(Long scenarioId) {`
  - id 行 L124: `computed.setOrgId(basePlan.getOrgId());`
  - id 行 L134: `List<ErpMfgMrpDemand> demands = loadDemands(basePlan.getId());`
  - id 行 L170: `version.setScenarioId(scenarioId);`
  - id 行 L172: `version.setComputedMrpPlanId(computed.getId());`
  - id 行 L173: `version.setSnapshotSummary(buildSnapshotSummary(computed.getId()));`
  - id 行 L189: `public ErpMfgMrpPlan promoteToFormalPlan(Long scenarioVersionId) {`
  - id 行 L210: `promoted.setOrgId(computed.getOrgId());`
  - id 行 L220: `q.addFilter(eq("mrpPlanId", computed.getId()));`
  - id 行 L224: `dst.setMrpPlanId(promoted.getId());`
  - id 行 L226: `dst.setMaterialId(src.getMaterialId());`
  - id 行 L227: `dst.setUoMId(src.getUoMId());`
  - id 行 L235: `dst.setParentLineId(null); // 重置 pegging，转正后用户自行重算或保留`
  - id 行 L243: `version.setPromotedPlanId(promoted.getId());`
  - id 行 L250: `private List<ErpMfgMrpDemand> loadDemands(Long planId) {`
  - id 行 L280: `BigDecimal override = paramResolver.resolveSafetyStockOverride(scenario.getId(), material.getId());`
  - id 行 L285: `BigDecimal available = availableQuantity(material.getId(), plan.getOrgId());`
  - id 行 L292: `demand.setMaterialId(material.getId());`
  - id 行 L293: `demand.setUoMId(material.getUoMId());`
  - id 行 L306: `private void processMaterial(ErpMfgMrpPlan plan, Long materialId, BigDecimal grossQty, Long uoMId,`
  - id 行 L307: `LocalDate requirementDate, Long parentLineId, Set<Long> path,`
  - id 行 L308: `IEntityDao<ErpMfgMrpPlanLine> lineDao, int[] lineNo, Long scenarioId) {`
  - id 行 L329: `long leadDays = manufactured ? mfgLeadDays(bom.getId()) : purLeadDays(materialId, scenarioId);`
  - id 行 L333: `line.setMrpPlanId(plan.getId());`
  - id 行 L335: `line.setMaterialId(materialId);`
  - id 行 L336: `line.setUoMId(resolveUoM(uoMId, materialId));`
  - id 行 L345: `line.setParentLineId(parentLineId);`
  - id 行 L352: `List<BomExplosionNode> children = bomExpander.explode(bom.getId(), planned, false);`
  - id 行 L355: `plannedDate, line.getId(), path, lineDao, lineNo, scenarioId);`
  - id 行 L366: `private BigDecimal lotSize(BigDecimal net, Long scenarioId) {`
  - id 行 L386: `private long mfgLeadDays(Long bomId) {`
  - id 行 L407: `private long purLeadDays(Long materialId, Long scenarioId) {`
  - id 行 L419: `private Long resolveUoM(Long uoMId, Long materialId) {`
  - id 行 L427: `private BigDecimal availableQuantity(Long materialId, Long orgId) {`
  - id 行 L465: `private String buildSnapshotSummary(Long computedPlanId) {`
  - id 行 L483: `private int nextVersionNo(Long scenarioId) {`
  - id 行 L495: `private ErpMfgMrpScenario requireScenario(Long scenarioId) {`
  - id 行 L508: `private ErpMfgMrpScenarioVersion requireVersion(Long versionId) {`
  - id 行 L522: `final Long materialId;`
  - id 行 L524: `Long uoMId;`
  - id 行 L527: `TopDemand(Long materialId) {`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/simulation/SimulationVersionComparator.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 7
- 耦合点 L3: ErpMdMaterial (master-data)
- 耦合点 L170: ErpMdMaterial.class (daoFor) (master-data)
  - id 行 L72: `for (Long materialId : allMaterials) {`
  - id 行 L82: `d.setMaterialId(materialId);`
  - id 行 L124: `private ErpMfgMrpScenarioVersion requireVersion(Long versionId) {`
  - id 行 L137: `.param(ErpMfgErrors.ARG_SCENARIO_VERSION_ID, a.getId())`
  - id 行 L138: `.param(ErpMfgErrors.ARG_EXPECTED_STATUS, String.valueOf(b.getId()));`
  - id 行 L142: `private Map<Long, ErpMfgMrpPlanLine> indexLines(Long planId) {`
  - id 行 L169: `private BigDecimal lookupStandardCost(Long materialId) {`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/spi/ErpMfgSkuReferenceChecker.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 6
- 耦合点 L3: ErpMdMaterialSku (master-data)
  - id 行 L46: `if (sku == null || sku.getId() == null) {`
  - id 行 L49: `Long skuId = sku.getId();`
  - id 行 L54: `private boolean existsBomLine(Long skuId) {`
  - id 行 L63: `private boolean existsBomByproduct(Long skuId) {`
  - id 行 L72: `private boolean existsWorkOrderLine(Long skuId) {`
  - id 行 L83: `private boolean existsMaterialIssueLine(Long skuId) {`

### module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/workorder/KitAvailabilityChecker.java
- 外域: inventory  |  本文件 id-as-Long 证据行: 8
- 耦合点 L10: ErpInvStockBalance (inventory)
- 耦合点 L160: ErpInvStockBalance.class (daoFor) (inventory)
  - id 行 L69: `public KitAvailabilityResult check(Long workOrderId) {`
  - id 行 L83: `Long materialId = e.getKey();`
  - id 行 L111: `public List<BomExplosionNode> explodeRequirements(Long bomId, BigDecimal requestedQty) {`
  - id 行 L125: `return bomExpander.explode(latest.getId(), requestedQty, true);`
  - id 行 L139: `Long bomId = resolveBomId(wo);`
  - id 行 L175: `private ErpMfgWorkOrder requireWorkOrder(Long workOrderId) {`
  - id 行 L186: `public Long resolveBomId(ErpMfgWorkOrder wo) {`
  - id 行 L195: `return defaultBom.getId();`

## module-projects （引用外域的耦合文件 4 个）

### module-projects/erp-prj-service/src/main/java/app/erp/prj/service/cost/ExpenseCostAggregator.java
- 外域: finance  |  本文件 id-as-Long 证据行: 17
- 耦合点 L3: IErpFinExpenseClaimBiz (finance)
- 耦合点 L4: ErpFinExpenseClaim (finance)
- 耦合点 L5: ErpFinExpenseClaimLine (finance)
- 耦合点 L190: ErpFinExpenseClaimLine.class (daoFor) (finance)
  - id 行 L71: `public BigDecimal refreshExpenseCost(Long projectId) {`
  - id 行 L93: `List<ErpFinExpenseClaimLine> lines = findLinesForProject(claim.getId(), projectId);`
  - id 行 L120: `saveExpenseLine(existingHead.getId(), p.sourceBillCode, p.amount, p.subjectId);`
  - id 行 L127: `newHead.setProjectId(projectId);`
  - id 行 L128: `newHead.setOrgId(project.getOrgId());`
  - id 行 L140: `saveExpenseLine(newHead.getId(), ++lineNo, p.sourceBillCode, p.amount, p.subjectId);`
  - id 行 L153: `final Long subjectId;`
  - id 行 L155: `PendingExpenseLine(String sourceBillCode, BigDecimal amount, Long subjectId) {`
  - id 行 L189: `private List<ErpFinExpenseClaimLine> findLinesForProject(Long claimId, Long projectId) {`
  - id 行 L204: `private void saveExpenseLine(Long headId, String sourceBillCode, BigDecimal amount, Long subjectId) {`
  - id 行 L208: `private void saveExpenseLine(Long headId, int lineNo, String sourceBillCode, BigDecimal amount,`
  - id 行 L209: `Long subjectId) {`
  - id 行 L212: `line.setCostCollectionId(headId);`
  - id 行 L217: `line.setSubjectId(subjectId);`
  - id 行 L222: `private int nextLineNo(Long headId) {`
  - id 行 L229: `private ErpPrjCostCollection findHead(Long projectId) {`
  - id 行 L239: `private ErpPrjProject loadProject(Long projectId) {`

### module-projects/erp-prj-service/src/main/java/app/erp/prj/service/posting/ProjectPostingExecutor.java
- 外域: finance  |  本文件 id-as-Long 证据行: 0
- 耦合点 L3: IErpFinVoucherBiz (finance)

### module-projects/erp-prj-service/src/main/java/app/erp/prj/service/posting/TimesheetPostingDispatcher.java
- 外域: master-data, notify  |  本文件 id-as-Long 证据行: 13
- 耦合点 L6: IErpMdCurrencyBiz (master-data)
- 耦合点 L7: IErpMdExchangeRateBiz (master-data)
- 耦合点 L9: ErpMdCurrency (master-data)
- 耦合点 L10: ErpMdExchangeRate (master-data)
- 耦合点 L11: ErpMdSubject (master-data)
- 耦合点 L19: IErpSysNotificationBiz (notify)
- 耦合点 L278: ErpMdSubject.class (daoFor) (master-data)
  - id 行 L79: `Long voucherId = executor.postEvent(event);`
  - id 行 L143: `event.setOrgId(timesheet.getOrgId());`
  - id 行 L144: `event.setAcctSchemaId(resolveAcctSchemaId(timesheet.getOrgId()));`
  - id 行 L145: `event.setCurrencyId(timesheet.getCurrencyId());`
  - id 行 L172: `private ErpPrjProject loadProject(Long projectId) {`
  - id 行 L180: `private ErpPrjProjectType loadProjectType(Long projectTypeId) {`
  - id 行 L185: `private ErpPrjActivityType loadActivityType(Long activityTypeId) {`
  - id 行 L190: `private Long resolveAcctSchemaId(Long orgId) {`
  - id 行 L201: `protected BigDecimal resolveExchangeRate(Long currencyId, LocalDate voucherDate) {`
  - id 行 L218: `BigDecimal rate = findExchangeRate(currencyId, functional.getId(), voucherDate, context);`
  - id 行 L226: `protected ErpMdCurrency findCurrencyById(Long currencyId, IServiceContext context) {`
  - id 行 L250: `protected BigDecimal findExchangeRate(Long fromCurrencyId, Long toCurrencyId, LocalDate voucherDate,`
  - id 行 L274: `private String resolveSubjectCode(Long subjectId, String defaultCode) {`

### module-projects/erp-prj-service/src/main/java/app/erp/prj/service/processor/ErpPrjProjectSettlementProcessor.java
- 外域: assets, finance  |  本文件 id-as-Long 证据行: 9
- 耦合点 L3: IErpAstAssetBiz (assets)
- 耦合点 L4: ErpAstAsset (assets)
- 耦合点 L6: ErpFinVoucherBillR (finance)
- 耦合点 L227: ErpAstAsset.class (daoFor) (assets)
- 耦合点 L311: ErpFinVoucherBillR.class (daoFor) (finance)
  - id 行 L181: `line.setSettlementId(settlement.getId());`
  - id 行 L191: `line.setSettlementId(settlement.getId());`
  - id 行 L214: `if (asset != null && asset.getId() != null) {`
  - id 行 L215: `settlement.setAssetCardId(asset.getId());`
  - id 行 L224: `if (settlement.getAssetCardId() == null) {`
  - id 行 L228: `ErpAstAsset asset = dao.getEntityById(settlement.getAssetCardId());`
  - id 行 L251: `protected List<ErpPrjBilling> findBillings(Long projectId) {`
  - id 行 L259: `protected List<ErpPrjCostCollection> findCostCollections(Long projectId) {`
  - id 行 L267: `protected ErpPrjProject loadProject(Long projectId) {`

## module-purchase （引用外域的耦合文件 21 个）

### module-purchase/erp-pur-dao/src/main/java/app/erp/pur/biz/IErpPurPaymentBiz.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 4
- 耦合点 L9: SettlementAllocation (master-data)
  - id 行 L22: `ErpPurPayment cancel(@Name("paymentId") Long paymentId, IServiceContext context);`
  - id 行 L25: `ErpPurPayment settle(@Name("paymentId") Long paymentId,`
  - id 行 L30: `ErpPurPayment reverseSettlement(@Name("paymentId") Long paymentId,`
  - id 行 L31: `@Name("invoiceId") Long invoiceId,`

### module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/ScorecardStandingLinker.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 0
- 耦合点 L3: IErpMdSupplierApprovalBiz (master-data)

### module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/SupplierEligibilityChecker.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 2
- 耦合点 L3: IErpMdSupplierApprovalBiz (master-data)
- 耦合点 L4: ErpMdSupplierApproval (master-data)
  - id 行 L41: `public Decision check(Long partnerId, io.nop.core.context.IServiceContext context) {`
  - id 行 L74: `protected ErpPurSupplierScorecard findLatestFinalizedScorecard(Long partnerId) {`

### module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/dashboard/ErpPurDashboardBizModel.java
- 外域: finance, master-data  |  本文件 id-as-Long 证据行: 6
- 耦合点 L3: IErpFinArApItemBiz (finance)
- 耦合点 L4: ErpFinArApItem (finance)
- 耦合点 L6: ErpMdPartner (master-data)
- 耦合点 L155: ErpMdPartner.class (daoFor) (master-data)
- 耦合点 L184: ErpMdPartner.class (daoFor) (master-data)
- 耦合点 L230: ErpMdPartner.class (daoFor) (master-data)
  - id 行 L187: `if (hasPriceVariance(inv.getId(), tolerance)) {`
  - id 行 L189: `row.put("invoiceId", inv.getId());`
  - id 行 L301: `map.put(o.getId(), o.getDeliveryDate());`
  - id 行 L308: `private boolean hasPriceVariance(Long invoiceId, BigDecimal tolerance) {`
  - id 行 L333: `orderLinePrice.put(ol.getId(), ol.getUnitPrice());`
  - id 行 L337: `receiveToOrderPrice.put(rl.getId(), orderLinePrice.get(rl.getOrderLineId()));`

### module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ErpPurPaymentBizModel.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 4
- 耦合点 L5: SettlementAllocation (master-data)
  - id 行 L42: `public ErpPurPayment cancel(@Name("paymentId") Long paymentId, IServiceContext context) {`
  - id 行 L48: `public ErpPurPayment settle(@Name("paymentId") Long paymentId,`
  - id 行 L56: `public ErpPurPayment reverseSettlement(@Name("paymentId") Long paymentId,`
  - id 行 L57: `@Name("invoiceId") Long invoiceId,`

### module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/PaymentSettler.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 20
- 耦合点 L3: SettlementAllocation (master-data)
  - id 行 L76: `BigDecimal paymentSettled = sumPaymentLines(payment.getId());`
  - id 行 L107: `line.setPaymentId(payment.getId());`
  - id 行 L108: `line.setInvoiceId(alloc.getInvoiceId());`
  - id 行 L116: `for (Long invoiceId : touchedInvoices.keySet()) {`
  - id 行 L119: `recomputePaymentWrittenOff(payment.getId());`
  - id 行 L120: `return daoProvider.daoFor(ErpPurPayment.class).getEntityById(payment.getId());`
  - id 行 L126: `public ErpPurPayment reverseSettlement(ErpPurPayment payment, Long invoiceId) {`
  - id 行 L127: `List<ErpPurPaymentLine> existing = findLines(payment.getId(), invoiceId);`
  - id 行 L138: `reversal.setPaymentId(payment.getId());`
  - id 行 L139: `reversal.setInvoiceId(invoiceId);`
  - id 行 L145: `recomputePaymentWrittenOff(payment.getId());`
  - id 行 L146: `return daoProvider.daoFor(ErpPurPayment.class).getEntityById(payment.getId());`
  - id 行 L151: `private ErpPurInvoice requireInvoiceForSettle(ErpPurPayment payment, Long invoiceId) {`
  - id 行 L182: `List<ErpPurInvoiceLine> lines = loadInvoiceLines(invoice.getId());`
  - id 行 L193: `private List<ErpPurInvoiceLine> loadInvoiceLines(Long invoiceId) {`
  - id 行 L201: `private void recomputeInvoicePaid(Long invoiceId) {`
  - id 行 L219: `private void recomputePaymentWrittenOff(Long paymentId) {`
  - id 行 L236: `private BigDecimal sumInvoiceLines(Long invoiceId) {`
  - id 行 L246: `private BigDecimal sumPaymentLines(Long paymentId) {`
  - id 行 L256: `private List<ErpPurPaymentLine> findLines(Long paymentId, Long invoiceId) {`

### module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ReceiveStockMoveBuilder.java
- 外域: inventory, master-data  |  本文件 id-as-Long 证据行: 10
- 耦合点 L3: StockMoveLineRequest (inventory)
- 耦合点 L4: StockMoveRequest (inventory)
- 耦合点 L5: IErpMdAcctSchemaBiz (master-data)
- 耦合点 L6: ErpMdAcctSchema (master-data)
  - id 行 L33: `request.setOrgId(receive.getOrgId());`
  - id 行 L35: `request.setDestWarehouseId(receive.getWarehouseId());`
  - id 行 L36: `request.setSourceWarehouseId(null);`
  - id 行 L37: `request.setAcctSchemaId(resolveAcctSchemaId(receive.getOrgId(), context));`
  - id 行 L38: `request.setCurrencyId(receive.getCurrencyId());`
  - id 行 L48: `private Long resolveAcctSchemaId(Long orgId, IServiceContext context) {`
  - id 行 L53: `return schema == null ? null : schema.getId();`
  - id 行 L60: `req.setMaterialId(line.getMaterialId());`
  - id 行 L61: `req.setSkuId(line.getSkuId());`
  - id 行 L62: `req.setUoMId(line.getUoMId());`

### module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ReturnStockMoveBuilder.java
- 外域: inventory, master-data  |  本文件 id-as-Long 证据行: 10
- 耦合点 L3: StockMoveLineRequest (inventory)
- 耦合点 L4: StockMoveRequest (inventory)
- 耦合点 L5: IErpMdAcctSchemaBiz (master-data)
- 耦合点 L6: ErpMdAcctSchema (master-data)
  - id 行 L37: `request.setOrgId(returnOrder.getOrgId());`
  - id 行 L39: `request.setSourceWarehouseId(returnOrder.getWarehouseId());`
  - id 行 L40: `request.setDestWarehouseId(null);`
  - id 行 L41: `request.setAcctSchemaId(resolveAcctSchemaId(returnOrder.getOrgId(), context));`
  - id 行 L42: `request.setCurrencyId(returnOrder.getCurrencyId());`
  - id 行 L49: `private Long resolveAcctSchemaId(Long orgId, IServiceContext context) {`
  - id 行 L54: `return schema == null ? null : schema.getId();`
  - id 行 L61: `req.setMaterialId(line.getMaterialId());`
  - id 行 L62: `req.setSkuId(line.getSkuId());`
  - id 行 L63: `req.setUoMId(line.getUoMId());`

### module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurPostingExecutor.java
- 外域: finance  |  本文件 id-as-Long 证据行: 0
- 耦合点 L3: IErpFinVoucherBiz (finance)

### module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurInvoiceProcessor.java
- 外域: finance, master-data  |  本文件 id-as-Long 证据行: 4
- 耦合点 L3: IErpFinBudgetCommitmentBiz (finance)
- 耦合点 L5: IErpMdPartnerBiz (master-data)
- 耦合点 L6: IErpMdSubjectBiz (master-data)
- 耦合点 L7: ErpMdPartner (master-data)
- 耦合点 L8: ErpMdSubject (master-data)
  - id 行 L379: `Long subjectId = resolveBudgetSubjectId(ErpFinConstants.CONFIG_BUDGET_COMMITMENT_SUBJECT_CODE, context);`
  - id 行 L383: `Long periodId = orderProcessor.resolvePeriodId(order.getBusinessDate());`
  - id 行 L410: `protected Long resolveBudgetSubjectId(String configKey, IServiceContext context) {`
  - id 行 L416: `return subject == null ? null : subject.getId();`

### module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurOrderProcessor.java
- 外域: finance, master-data  |  本文件 id-as-Long 证据行: 10
- 耦合点 L3: IErpFinBudgetCommitmentBiz (finance)
- 耦合点 L4: IErpFinBudgetControlBiz (finance)
- 耦合点 L5: IErpFinIntercompanyTransferBiz (finance)
- 耦合点 L6: ErpFinAccountingPeriod (finance)
- 耦合点 L8: IErpMdPartnerBiz (master-data)
- 耦合点 L9: ErpMdPartner (master-data)
- 耦合点 L10: ErpMdSubject (master-data)
- 耦合点 L311: ErpMdSubject.class (daoFor) (master-data)
- 耦合点 L323: ErpFinAccountingPeriod.class (daoFor) (finance)
  - id 行 L216: `Long subjectId = resolveBudgetSubjectId(ErpFinConstants.CONFIG_BUDGET_PURCHASE_EXPENSE_SUBJECT_CODE);`
  - id 行 L220: `Long periodId = resolvePeriodId(order.getBusinessDate());`
  - id 行 L236: `Long subjectId = resolveBudgetSubjectId(ErpFinConstants.CONFIG_BUDGET_COMMITMENT_SUBJECT_CODE);`
  - id 行 L240: `Long periodId = resolvePeriodId(order.getBusinessDate());`
  - id 行 L284: `ErpFinConstants.INTERCOMPANY_DOC_TYPE_PURCHASE_ORDER, order.getId(), order.getCode(),`
  - id 行 L299: `ErpFinConstants.INTERCOMPANY_DOC_TYPE_PURCHASE_ORDER, order.getId(), order.getCode(), context);`
  - id 行 L306: `protected Long resolveBudgetSubjectId(String configKey) {`
  - id 行 L316: `return list.isEmpty() ? null : list.get(0).getId();`
  - id 行 L319: `protected Long resolvePeriodId(LocalDate businessDate) {`
  - id 行 L329: `return list.isEmpty() ? null : list.get(0).getId();`

### module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurPaymentProcessor.java
- 外域: finance, master-data  |  本文件 id-as-Long 证据行: 6
- 耦合点 L3: IErpFinBudgetControlBiz (finance)
- 耦合点 L4: ErpFinAccountingPeriod (finance)
- 耦合点 L6: IErpMdPartnerBiz (master-data)
- 耦合点 L7: ErpMdPartner (master-data)
- 耦合点 L8: ErpMdSubject (master-data)
- 耦合点 L197: ErpMdSubject.class (daoFor) (master-data)
- 耦合点 L209: ErpFinAccountingPeriod.class (daoFor) (finance)
  - id 行 L182: `Long subjectId = resolveBudgetSubjectId(ErpFinConstants.CONFIG_BUDGET_PURCHASE_EXPENSE_SUBJECT_CODE);`
  - id 行 L186: `Long periodId = resolvePeriodId(payment.getBusinessDate());`
  - id 行 L192: `protected Long resolveBudgetSubjectId(String configKey) {`
  - id 行 L202: `return list.isEmpty() ? null : list.get(0).getId();`
  - id 行 L205: `protected Long resolvePeriodId(LocalDate businessDate) {`
  - id 行 L215: `return list.isEmpty() ? null : list.get(0).getId();`

### module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurPaymentSettleProcessor.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 0
- 耦合点 L3: SettlementAllocation (master-data)

### module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveApproveProcessor.java
- 外域: inventory  |  本文件 id-as-Long 证据行: 0
- 耦合点 L3: ErpInvStockMove (inventory)

### module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveCancelProcessor.java
- 外域: quality  |  本文件 id-as-Long 证据行: 0
- 耦合点 L8: IErpQaInspectionBiz (quality)

### module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveProcessor.java
- 外域: drp, inventory, master-data, projects, quality  |  本文件 id-as-Long 证据行: 14
- 耦合点 L3: IErpInvStockMoveBiz (inventory)
- 耦合点 L4: StockMoveRequest (inventory)
- 耦合点 L5: ErpInvStockMove (inventory)
- 耦合点 L6: IErpMdPartnerBiz (master-data)
- 耦合点 L7: ErpMdPartner (master-data)
- 耦合点 L8: IErpPrjCostCollectionBiz (projects)
- 耦合点 L18: IErpQaInspectionBiz (quality)
- 耦合点 L19: InspectionTrigger (quality)
- 耦合点 L20: IErpInvDrpCrossDockBiz (drp)
- 耦合点 L21: IErpInvDrpLeadTimeRecordBiz (drp)
  - id 行 L204: `Long orderId = receive.getOrderId();`
  - id 行 L220: `if (r.getId().equals(receive.getId())) {`
  - id 行 L227: `Long orderLineId = rl.getOrderLineId();`
  - id 行 L324: `crossDockBiz.markReceivedFromPurchase(orderCode, move != null ? move.getId() : null,`
  - id 行 L335: `protected String resolveOrderCode(Long orderId) {`
  - id 行 L407: `Long orderLineId = line.getOrderLineId();`
  - id 行 L444: `stockMoveBiz.reverse(original.getId(), context);`
  - id 行 L448: `Long orderId = currentReceive.getOrderId();`
  - id 行 L460: `if (r.getId().equals(currentReceive.getId())) {`
  - id 行 L470: `BigDecimal received = receivedByOrderLine.getOrDefault(ol.getId(), BigDecimal.ZERO);`
  - id 行 L556: `protected List<ErpPurOrderLine> loadOrderLines(Long orderId) {`
  - id 行 L563: `protected List<ErpPurReceive> findApprovedReceives(Long orderId) {`
  - id 行 L579: `protected ErpPurOrderLine findOrderLine(List<ErpPurOrderLine> orderLines, Long orderLineId) {`
  - id 行 L581: `if (ol.getId().equals(orderLineId)) {`

### module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReturnApproveProcessor.java
- 外域: inventory  |  本文件 id-as-Long 证据行: 0
- 耦合点 L3: ErpInvStockMove (inventory)

### module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReturnProcessor.java
- 外域: finance, inventory, master-data  |  本文件 id-as-Long 证据行: 9
- 耦合点 L3: IErpFinBudgetCommitmentBiz (finance)
- 耦合点 L5: IErpInvStockMoveBiz (inventory)
- 耦合点 L6: StockMoveRequest (inventory)
- 耦合点 L7: ErpInvStockMove (inventory)
- 耦合点 L8: IErpMdPartnerBiz (master-data)
- 耦合点 L9: IErpMdSubjectBiz (master-data)
- 耦合点 L10: ErpMdPartner (master-data)
- 耦合点 L11: ErpMdSubject (master-data)
  - id 行 L244: `request.setOriginReturnedMoveId(resolveSourceReceiveMoveId(returnOrder, context));`
  - id 行 L248: `protected Long resolveSourceReceiveMoveId(ErpPurReturn returnOrder, IServiceContext context) {`
  - id 行 L255: `return sourceMove == null ? null : sourceMove.getId();`
  - id 行 L261: `returnOrder = returnDao().getEntityById(returnOrder.getId());`
  - id 行 L277: `stockMoveBiz.reverse(original.getId(), context);`
  - id 行 L362: `Long subjectId = resolveBudgetSubjectId(ErpFinConstants.CONFIG_BUDGET_COMMITMENT_SUBJECT_CODE, context);`
  - id 行 L366: `Long periodId = orderProcessor.resolvePeriodId(order.getBusinessDate());`
  - id 行 L393: `protected Long resolveBudgetSubjectId(String configKey, IServiceContext context) {`
  - id 行 L399: `return subject == null ? null : subject.getId();`

### module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/spi/ErpPurSkuReferenceChecker.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 5
- 耦合点 L3: ErpMdMaterialSku (master-data)
  - id 行 L37: `if (sku == null || sku.getId() == null) {`
  - id 行 L40: `return existsOrderLine(sku.getId()) || existsReceiveLine(sku.getId()) || existsReturnLine(sku.getId());`
  - id 行 L43: `private boolean existsOrderLine(Long skuId) {`
  - id 行 L52: `private boolean existsReceiveLine(Long skuId) {`
  - id 行 L61: `private boolean existsReturnLine(Long skuId) {`

### module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/support/ErpPurCtDiscountApplier.java
- 外域: contract  |  本文件 id-as-Long 证据行: 0
- 耦合点 L3: IErpCtVolumeDiscountBiz (contract)

### module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/support/ErpPurSupplierPriceResolver.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 2
- 耦合点 L14: ErpMdMaterialSku (master-data)
  - id 行 L45: `public BigDecimal resolveSupplierPrice(ErpMdMaterialSku sku, Long partnerId) {`
  - id 行 L63: `protected List<ErpPurSupplierPriceList> findCandidates(ErpMdMaterialSku sku, Long partnerId) {`

## module-quality （引用外域的耦合文件 6 个）

### module-quality/erp-qa-service/src/main/java/app/erp/qa/service/entity/RecallTargetLocator.java
- 外域: inventory, sales  |  本文件 id-as-Long 证据行: 4
- 耦合点 L4: IErpInvBatchBiz (inventory)
- 耦合点 L5: IErpInvStockMoveBiz (inventory)
- 耦合点 L6: TraceChainResult (inventory)
- 耦合点 L7: ErpInvBatch (inventory)
- 耦合点 L8: ErpInvStockMove (inventory)
- 耦合点 L9: ErpInvStockMoveLine (inventory)
- 耦合点 L16: IErpSalDeliveryBiz (sales)
- 耦合点 L17: ErpSalDelivery (sales)
  - id 行 L102: `target.setRecallId(recall.getId());`
  - id 行 L103: `target.setPartnerId(delivery.getCustomerId());`
  - id 行 L105: `target.setSalesDeliveryId(delivery.getId());`
  - id 行 L124: `Long batchId = recall.getBatchId();`

### module-quality/erp-qa-service/src/main/java/app/erp/qa/service/posting/NcrPostingDispatcher.java
- 外域: finance, inventory  |  本文件 id-as-Long 证据行: 10
- 耦合点 L5: ErpInvStockBalance (inventory)
- 耦合点 L32: IErpFinVoucherBiz (finance)
- 耦合点 L121: ErpInvStockBalance.class (daoFor) (inventory)
  - id 行 L71: `Long currencyId = balance != null ? balance.getCurrencyId() : null;`
  - id 行 L72: `Long warehouseId = balance != null ? balance.getWarehouseId() : null;`
  - id 行 L73: `Long orgId = balance != null ? balance.getOrgId() : null;`
  - id 行 L76: `Long voucherId = executor.postEvent(event);`
  - id 行 L99: `private PostingEvent buildScrapEvent(ErpQaNonConformance ncr, BigDecimal scrapAmount, Long currencyId, Long warehouseId, Long orgId) {`
  - id 行 L103: `event.setOrgId(orgId);`
  - id 行 L104: `event.setAcctSchemaId(resolveAcctSchemaId());`
  - id 行 L105: `event.setCurrencyId(currencyId);`
  - id 行 L117: `private ErpInvStockBalance resolveStockBalance(Long materialId) {`
  - id 行 L133: `private Long resolveAcctSchemaId() {`

### module-quality/erp-qa-service/src/main/java/app/erp/qa/service/posting/NcrPostingExecutor.java
- 外域: finance  |  本文件 id-as-Long 证据行: 0
- 耦合点 L3: IErpFinVoucherBiz (finance)

### module-quality/erp-qa-service/src/main/java/app/erp/qa/service/posting/NcrReturnOrchestrator.java
- 外域: inventory, purchase, sales  |  本文件 id-as-Long 证据行: 9
- 耦合点 L3: ErpInvStockBalance (inventory)
- 耦合点 L4: IErpPurReturnBiz (purchase)
- 耦合点 L6: ErpPurReturn (purchase)
- 耦合点 L9: IErpSalReturnBiz (sales)
- 耦合点 L11: ErpSalReturn (sales)
- 耦合点 L135: ErpInvStockBalance.class (daoFor) (inventory)
  - id 行 L73: `Long warehouseId = resolveWarehouseId(ncr.getMaterialId());`
  - id 行 L74: `Long currencyId = resolveCurrencyId(ncr.getMaterialId());`
  - id 行 L84: `private String createPurchaseReturn(ErpQaNonConformance ncr, Long warehouseId, Long currencyId, IServiceContext context) {`
  - id 行 L91: `data.put("code", "PR-FROM-NCR-" + ncr.getId());`
  - id 行 L103: `private String createSalesReturn(ErpQaNonConformance ncr, Long warehouseId, Long currencyId, IServiceContext context) {`
  - id 行 L110: `data.put("code", "SR-FROM-NCR-" + ncr.getId());`
  - id 行 L121: `private Long resolveWarehouseId(Long materialId) {`
  - id 行 L126: `private Long resolveCurrencyId(Long materialId) {`
  - id 行 L131: `private ErpInvStockBalance findStockBalance(Long materialId) {`

### module-quality/erp-qa-service/src/main/java/app/erp/qa/service/processor/ErpQaRecallGenerateReturnsProcessor.java
- 外域: sales  |  本文件 id-as-Long 证据行: 8
- 耦合点 L6: IErpSalDeliveryBiz (sales)
- 耦合点 L7: IErpSalReturnBiz (sales)
- 耦合点 L8: ErpSalDelivery (sales)
- 耦合点 L9: ErpSalDeliveryLine (sales)
- 耦合点 L10: ErpSalReturn (sales)
  - id 行 L36: `public ErpQaRecall generateReturns(Long recallId, IServiceContext context) {`
  - id 行 L45: `target.setGeneratedReturnId(salReturn.getId());`
  - id 行 L56: `Long warehouseId = delivery != null ? delivery.getWarehouseId() : null;`
  - id 行 L57: `Long currencyId = delivery != null ? delivery.getCurrencyId() : null;`
  - id 行 L58: `Long uoMId = pickUoMId(delivery, recall.getMaterialId());`
  - id 行 L68: `data.put("code", "RMA-" + recall.getCode() + "-" + target.getId());`
  - id 行 L80: `private Long pickUoMId(ErpSalDelivery delivery, Long materialId) {`
  - id 行 L88: `Long firstUoMId = null;`

### module-quality/erp-qa-service/src/main/java/app/erp/qa/service/report/ErpQaReportBizModel.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 9
- 耦合点 L3: ErpMdMaterial (master-data)
- 耦合点 L361: ErpMdMaterial.class (daoFor) (master-data)
  - id 行 L200: `public List<Map<String, Object>> inspectionSummaryData(@Optional @Name("materialId") Long materialId,`
  - id 行 L223: `List<Map<String, Object>> buildInspectionSummaryDataset(Long materialId, LocalDate startDate, LocalDate endDate) {`
  - id 行 L278: `if (n.getId() != null) ncrIds.add(n.getId());`
  - id 行 L291: `a.capaActionCount += actionCountByNcr.getOrDefault(n.getId(), 0);`
  - id 行 L292: `a.completedActionCount += completedActionCountByNcr.getOrDefault(n.getId(), 0);`
  - id 行 L325: `private List<ErpQaInspection> loadInspections(Long materialId, LocalDate startDate, LocalDate endDate) {`
  - id 行 L364: `names.put(m.getId(), m.getName());`
  - id 行 L370: `final Long materialId;`
  - id 行 L376: `InspectionAggregator(Long materialId) {`

## module-sales （引用外域的耦合文件 24 个）

### module-sales/erp-sal-dao/src/main/java/app/erp/sal/biz/IErpSalReceiptBiz.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 4
- 耦合点 L9: SettlementAllocation (master-data)
  - id 行 L28: `ErpSalReceipt cancel(@Name("receiptId") Long receiptId, IServiceContext context);`
  - id 行 L35: `ErpSalReceipt settle(@Name("receiptId") Long receiptId,`
  - id 行 L43: `ErpSalReceipt reverseSettlement(@Name("receiptId") Long receiptId,`
  - id 行 L44: `@Name("invoiceId") Long invoiceId,`

### module-sales/erp-sal-dao/src/main/java/app/erp/sal/dao/entity/ErpSalPriceList.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 0
- 耦合点 L4: IDateRange (master-data)

### module-sales/erp-sal-dao/src/main/java/app/erp/sal/dao/entity/ErpSalPriceListLine.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 0
- 耦合点 L4: IDateRange (master-data)

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/dashboard/ErpSalDashboardBizModel.java
- 外域: finance, master-data  |  本文件 id-as-Long 证据行: 0
- 耦合点 L3: IErpFinArApItemBiz (finance)
- 耦合点 L4: ErpFinArApItem (finance)
- 耦合点 L6: ErpMdPartner (master-data)
- 耦合点 L150: ErpMdPartner.class (daoFor) (master-data)
- 耦合点 L198: ErpMdPartner.class (daoFor) (master-data)

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/CreditLimitChecker.java
- 外域: finance, master-data, notify  |  本文件 id-as-Long 证据行: 13
- 耦合点 L3: IErpFinArApItemBiz (finance)
- 耦合点 L4: ErpFinArApItem (finance)
- 耦合点 L6: IErpMdPartnerBiz (master-data)
- 耦合点 L7: ErpMdPartner (master-data)
- 耦合点 L8: IErpSysNotificationBiz (notify)
  - id 行 L97: `public void check(Long customerId, BigDecimal thisOrderAmount, BigDecimal thisOrderExchangeRate,`
  - id 行 L109: `public void check(Long customerId, BigDecimal thisOrderAmount, BigDecimal thisOrderExchangeRate,`
  - id 行 L143: `public void checkCreditHold(Long customerId, String billCode, String billType, IServiceContext context) {`
  - id 行 L181: `throw buildHardBlockException(billType, partner.getId(), billCode, creditLimit, available,`
  - id 行 L187: `partner.getId(), creditLimit, available, billAmountFunctional, billType);`
  - id 行 L191: `.param(ErpSalErrors.ARG_CUSTOMER_ID, partner.getId())`
  - id 行 L197: `partner.getId(), creditLimit, available, billAmountFunctional, billType, level);`
  - id 行 L203: `private NopException buildHardBlockException(String billType, Long customerId, String billCode,`
  - id 行 L243: `ctx.put("customerId", partner.getId());`
  - id 行 L253: `partner.getId(), e.getMessage());`
  - id 行 L276: `private BigDecimal sumOutstanding(Long customerId, IServiceContext context) {`
  - id 行 L285: `private BigDecimal sumOutstandingOrders(Long customerId) {`
  - id 行 L303: `private BigDecimal sumArOpenFunctional(Long customerId, IServiceContext context) {`

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/DeliveryStockMoveBuilder.java
- 外域: inventory, master-data  |  本文件 id-as-Long 证据行: 10
- 耦合点 L3: StockMoveLineRequest (inventory)
- 耦合点 L4: StockMoveRequest (inventory)
- 耦合点 L5: IErpMdAcctSchemaBiz (master-data)
- 耦合点 L6: ErpMdAcctSchema (master-data)
  - id 行 L31: `request.setOrgId(delivery.getOrgId());`
  - id 行 L33: `request.setSourceWarehouseId(delivery.getWarehouseId());`
  - id 行 L34: `request.setDestWarehouseId(null);`
  - id 行 L35: `request.setAcctSchemaId(resolveAcctSchemaId(delivery.getOrgId(), context));`
  - id 行 L36: `request.setCurrencyId(delivery.getCurrencyId());`
  - id 行 L46: `private Long resolveAcctSchemaId(Long orgId, IServiceContext context) {`
  - id 行 L51: `return schema == null ? null : schema.getId();`
  - id 行 L58: `req.setMaterialId(line.getMaterialId());`
  - id 行 L59: `req.setSkuId(line.getSkuId());`
  - id 行 L60: `req.setUoMId(line.getUoMId());`

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ErpSalOrderBizModel.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 11
- 耦合点 L4: IErpMdMaterialSkuBiz (master-data)
- 耦合点 L5: IErpMdPartnerBiz (master-data)
- 耦合点 L7: ErpMdMaterial (master-data)
- 耦合点 L8: ErpMdPartner (master-data)
  - id 行 L109: `public ErpSalOrder cancel(@Name("orderId") Long orderId, IServiceContext context) {`
  - id 行 L166: `protected String resolveCustomerGroup(Long partnerId, IServiceContext context) {`
  - id 行 L196: `if (line.getId() == null) {`
  - id 行 L197: `line.setOrderId(order.getId());`
  - id 行 L212: `if (line.getId() != null && line.getLineNo() != null && line.getLineNo() > max) {`
  - id 行 L276: `Long skuId = line.getSkuId();`
  - id 行 L290: `Long materialCategoryId = resolveMaterialCategoryId(line);`
  - id 行 L304: `protected Long resolveMaterialCategoryId(ErpSalOrderLine line) {`
  - id 行 L323: `line.setOrderId(order.getId());`
  - id 行 L331: `public boolean existsActiveByQuotation(@Name("quotationId") Long quotationId, IServiceContext context) {`
  - id 行 L348: `public void updateDeliveryStatus(@Name("orderId") Long orderId,`

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ErpSalPricingRuleBizModel.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 3
- 耦合点 L4: IDateRange (master-data)
  - id 行 L92: `entity.getId(),`
  - id 行 L107: `this.id = rule.getId();`
  - id 行 L123: `public Long getId() {`

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ErpSalReceiptBizModel.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 4
- 耦合点 L5: SettlementAllocation (master-data)
  - id 行 L42: `public ErpSalReceipt cancel(@Name("receiptId") Long receiptId, IServiceContext context) {`
  - id 行 L48: `public ErpSalReceipt settle(@Name("receiptId") Long receiptId,`
  - id 行 L56: `public ErpSalReceipt reverseSettlement(@Name("receiptId") Long receiptId,`
  - id 行 L57: `@Name("invoiceId") Long invoiceId,`

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ReceiptSettler.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 18
- 耦合点 L3: SettlementAllocation (master-data)
  - id 行 L66: `BigDecimal receiptSettled = sumReceiptLines(receipt.getId());`
  - id 行 L97: `line.setReceiptId(receipt.getId());`
  - id 行 L98: `line.setInvoiceId(alloc.getInvoiceId());`
  - id 行 L106: `for (Long invoiceId : touchedInvoices.keySet()) {`
  - id 行 L109: `recomputeReceiptWrittenOff(receipt.getId());`
  - id 行 L110: `return daoProvider.daoFor(ErpSalReceipt.class).getEntityById(receipt.getId());`
  - id 行 L116: `public ErpSalReceipt reverseSettlement(ErpSalReceipt receipt, Long invoiceId) {`
  - id 行 L117: `List<ErpSalReceiptLine> existing = findLines(receipt.getId(), invoiceId);`
  - id 行 L128: `reversal.setReceiptId(receipt.getId());`
  - id 行 L129: `reversal.setInvoiceId(invoiceId);`
  - id 行 L135: `recomputeReceiptWrittenOff(receipt.getId());`
  - id 行 L136: `return daoProvider.daoFor(ErpSalReceipt.class).getEntityById(receipt.getId());`
  - id 行 L141: `private ErpSalInvoice requireInvoiceForSettle(ErpSalReceipt receipt, Long invoiceId) {`
  - id 行 L161: `private void recomputeInvoiceReceived(Long invoiceId) {`
  - id 行 L179: `private void recomputeReceiptWrittenOff(Long receiptId) {`
  - id 行 L196: `private BigDecimal sumInvoiceLines(Long invoiceId) {`
  - id 行 L206: `private BigDecimal sumReceiptLines(Long receiptId) {`
  - id 行 L216: `private List<ErpSalReceiptLine> findLines(Long receiptId, Long invoiceId) {`

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ReturnCostStrategyResolver.java
- 外域: inventory  |  本文件 id-as-Long 证据行: 2
- 耦合点 L3: ErpInvStockBalance (inventory)
- 耦合点 L78: ErpInvStockBalance.class (daoFor) (inventory)
  - id 行 L60: `Long materialId, Long warehouseId) {`
  - id 行 L74: `private static BigDecimal findAvgCost(IDaoProvider daoProvider, Long materialId, Long warehouseId) {`

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ReturnStockMoveBuilder.java
- 外域: inventory, master-data  |  本文件 id-as-Long 证据行: 10
- 耦合点 L3: StockMoveLineRequest (inventory)
- 耦合点 L4: StockMoveRequest (inventory)
- 耦合点 L5: IErpMdAcctSchemaBiz (master-data)
- 耦合点 L6: ErpMdAcctSchema (master-data)
  - id 行 L43: `request.setOrgId(returnOrder.getOrgId());`
  - id 行 L45: `request.setSourceWarehouseId(null);`
  - id 行 L46: `request.setDestWarehouseId(returnOrder.getWarehouseId());`
  - id 行 L47: `request.setAcctSchemaId(resolveAcctSchemaId(returnOrder.getOrgId(), context));`
  - id 行 L48: `request.setCurrencyId(returnOrder.getCurrencyId());`
  - id 行 L55: `private Long resolveAcctSchemaId(Long orgId, IServiceContext context) {`
  - id 行 L60: `return schema == null ? null : schema.getId();`
  - id 行 L68: `req.setMaterialId(line.getMaterialId());`
  - id 行 L69: `req.setSkuId(line.getSkuId());`
  - id 行 L70: `req.setUoMId(line.getUoMId());`

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/posting/SalPostingExecutor.java
- 外域: finance  |  本文件 id-as-Long 证据行: 0
- 耦合点 L3: IErpFinVoucherBiz (finance)

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalDeliveryApproveProcessor.java
- 外域: inventory  |  本文件 id-as-Long 证据行: 0
- 耦合点 L3: ErpInvStockMove (inventory)

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalDeliveryCancelProcessor.java
- 外域: quality  |  本文件 id-as-Long 证据行: 0
- 耦合点 L8: IErpQaInspectionBiz (quality)

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalDeliveryProcessor.java
- 外域: inventory, master-data, quality  |  本文件 id-as-Long 证据行: 13
- 耦合点 L3: IErpInvStockMoveBiz (inventory)
- 耦合点 L4: StockMoveRequest (inventory)
- 耦合点 L5: ErpInvStockMove (inventory)
- 耦合点 L6: IErpMdPartnerBiz (master-data)
- 耦合点 L7: ErpMdPartner (master-data)
- 耦合点 L8: IErpQaInspectionBiz (quality)
- 耦合点 L9: InspectionTrigger (quality)
  - id 行 L223: `delivery = deliveryDao().getEntityById(delivery.getId());`
  - id 行 L242: `List<ErpSalDeliveryLine> lines = loadLines(delivery.getId());`
  - id 行 L267: `stockMoveBiz.reverse(original.getId(), context);`
  - id 行 L271: `Long orderId = currentDelivery.getOrderId();`
  - id 行 L281: `addLineQuantities(deliveredByOrderLine, loadLines(currentDelivery.getId()));`
  - id 行 L283: `if (d.getId().equals(currentDelivery.getId())) {`
  - id 行 L286: `addLineQuantities(deliveredByOrderLine, loadLines(d.getId()));`
  - id 行 L293: `BigDecimal delivered = deliveredByOrderLine.getOrDefault(ol.getId(), BigDecimal.ZERO);`
  - id 行 L317: `for (ErpSalDeliveryLine line : loadLines(delivery.getId())) {`
  - id 行 L349: `if (loadLines(delivery.getId()).isEmpty()) {`
  - id 行 L367: `protected List<ErpSalDeliveryLine> loadLines(Long deliveryId) {`
  - id 行 L374: `protected List<ErpSalOrderLine> loadOrderLines(Long orderId) {`
  - id 行 L381: `protected List<ErpSalDelivery> findApprovedDeliveries(Long orderId) {`

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalInvoiceProcessor.java
- 外域: finance, master-data  |  本文件 id-as-Long 证据行: 4
- 耦合点 L3: IErpFinBudgetCommitmentBiz (finance)
- 耦合点 L5: IErpMdPartnerBiz (master-data)
- 耦合点 L6: ErpMdPartner (master-data)
  - id 行 L215: `invoice = invoiceDao().getEntityById(invoice.getId());`
  - id 行 L255: `if (loadLines(invoice.getId()).isEmpty()) {`
  - id 行 L273: `protected List<ErpSalInvoiceLine> loadLines(Long invoiceId) {`
  - id 行 L349: `List<ErpSalInvoiceLine> lines = loadLines(invoice.getId());`

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalOrderProcessor.java
- 外域: finance, inventory, master-data  |  本文件 id-as-Long 证据行: 17
- 耦合点 L3: IErpFinBudgetCommitmentBiz (finance)
- 耦合点 L4: IErpFinIntercompanyTransferBiz (finance)
- 耦合点 L5: ErpFinAccountingPeriod (finance)
- 耦合点 L7: IErpInvStockBalanceBiz (inventory)
- 耦合点 L8: ErpInvStockBalance (inventory)
- 耦合点 L9: IErpMdPartnerBiz (master-data)
- 耦合点 L10: ErpMdPartner (master-data)
- 耦合点 L11: ErpMdSubject (master-data)
- 耦合点 L461: ErpMdSubject.class (daoFor) (master-data)
- 耦合点 L473: ErpFinAccountingPeriod.class (daoFor) (finance)
  - id 行 L191: `List<ErpSalOrderLine> lines = loadLines(order.getId());`
  - id 行 L232: `Long orderWarehouseId = order.getWarehouseId();`
  - id 行 L233: `for (ErpSalOrderLine line : loadLines(order.getId())) {`
  - id 行 L234: `Long materialId = line.getMaterialId();`
  - id 行 L238: `Long warehouseId = line.getWarehouseId() != null ? line.getWarehouseId() : orderWarehouseId;`
  - id 行 L270: `protected BigDecimal resolveAvailableQuantity(Long materialId, Long warehouseId, IServiceContext context) {`
  - id 行 L338: `if (loadLines(order.getId()).isEmpty()) {`
  - id 行 L360: `List<ErpSalOrderLine> lines = loadLines(order.getId());`
  - id 行 L396: `ErpFinConstants.INTERCOMPANY_DOC_TYPE_SALES_ORDER, order.getId(), order.getCode(),`
  - id 行 L410: `ErpFinConstants.INTERCOMPANY_DOC_TYPE_SALES_ORDER, order.getId(), order.getCode(), context);`
  - id 行 L426: `Long subjectId = resolveBudgetSubjectId(ErpFinConstants.CONFIG_BUDGET_COMMITMENT_SALES_SUBJECT_CODE);`
  - id 行 L430: `Long periodId = resolvePeriodId(order.getBusinessDate());`
  - id 行 L456: `protected Long resolveBudgetSubjectId(String configKey) {`
  - id 行 L466: `return list.isEmpty() ? null : list.get(0).getId();`
  - id 行 L469: `protected Long resolvePeriodId(LocalDate businessDate) {`
  - id 行 L479: `return list.isEmpty() ? null : list.get(0).getId();`
  - id 行 L482: `protected List<ErpSalOrderLine> loadLines(Long orderId) {`

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalReceiptProcessor.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 2
- 耦合点 L3: IErpMdPartnerBiz (master-data)
- 耦合点 L4: ErpMdPartner (master-data)
  - id 行 L161: `receipt = receiptDao().getEntityById(receipt.getId());`
  - id 行 L181: `receipt = receiptDao().getEntityById(receipt.getId());`

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalReceiptSettleProcessor.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 0
- 耦合点 L3: SettlementAllocation (master-data)

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalReturnProcessor.java
- 外域: finance, inventory, master-data  |  本文件 id-as-Long 证据行: 15
- 耦合点 L3: ErpFinAccountingPeriod (finance)
- 耦合点 L4: IErpInvStockMoveBiz (inventory)
- 耦合点 L5: StockMoveRequest (inventory)
- 耦合点 L6: ErpInvStockMove (inventory)
- 耦合点 L7: IErpMdPartnerBiz (master-data)
- 耦合点 L8: ErpMdPartner (master-data)
- 耦合点 L349: ErpFinAccountingPeriod.class (daoFor) (finance)
  - id 行 L191: `List<ErpSalReturnLine> lines = loadLines(returnOrder.getId());`
  - id 行 L219: `returnOrder = returnDao().getEntityById(returnOrder.getId());`
  - id 行 L235: `returnOrder = returnDao().getEntityById(returnOrder.getId());`
  - id 行 L345: `protected ErpFinAccountingPeriod findPeriodByDate(Long orgId, LocalDate date) {`
  - id 行 L393: `List<ErpSalReturnLine> lines = loadLines(returnOrder.getId());`
  - id 行 L395: `request.setOriginReturnedMoveId(resolveSourceDeliveryMoveId(returnOrder, context));`
  - id 行 L399: `protected Long resolveSourceDeliveryMoveId(ErpSalReturn returnOrder, IServiceContext context) {`
  - id 行 L406: `return sourceMove == null ? null : sourceMove.getId();`
  - id 行 L426: `stockMoveBiz.reverse(original.getId(), context);`
  - id 行 L432: `ErpSalReturn reloaded = returnDao().getEntityById(returnOrder.getId());`
  - id 行 L458: `if (loadLines(returnOrder.getId()).isEmpty()) {`
  - id 行 L476: `protected List<ErpSalReturnLine> loadLines(Long returnId) {`
  - id 行 L495: `List<ErpSalReturnLine> lines = loadLines(returnOrder.getId());`
  - id 行 L499: `Long deliveryLineId = line.getDeliveryLineId();`
  - id 行 L513: `for (Long orderLineId : orderLineIds) {`

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/spi/ErpSalSkuReferenceChecker.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 7
- 耦合点 L3: ErpMdMaterialSku (master-data)
  - id 行 L41: `if (sku == null || sku.getId() == null) {`
  - id 行 L44: `return existsOrderLine(sku.getId()) || existsDeliveryLine(sku.getId())`
  - id 行 L45: `|| existsReturnLine(sku.getId()) || existsPriceListLine(sku.getId());`
  - id 行 L48: `private boolean existsOrderLine(Long skuId) {`
  - id 行 L57: `private boolean existsDeliveryLine(Long skuId) {`
  - id 行 L66: `private boolean existsReturnLine(Long skuId) {`
  - id 行 L75: `private boolean existsPriceListLine(Long skuId) {`

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/support/ErpSalCtDiscountApplier.java
- 外域: contract  |  本文件 id-as-Long 证据行: 0
- 耦合点 L3: IErpCtVolumeDiscountBiz (contract)

### module-sales/erp-sal-service/src/main/java/app/erp/sal/service/support/ErpSalCustomerPriceResolver.java
- 外域: master-data  |  本文件 id-as-Long 证据行: 8
- 耦合点 L3: IErpMdPartnerBiz (master-data)
- 耦合点 L5: ErpMdMaterialSku (master-data)
- 耦合点 L6: ErpMdPartner (master-data)
  - id 行 L51: `public ResolvedPrice resolveCustomerPrice(ErpMdMaterialSku sku, Long partnerId,`
  - id 行 L52: `BigDecimal quantity, Long currencyId,`
  - id 行 L68: `ErpSalPriceListLine line = matchLine(priceList.getId(), sku, qty, today, context);`
  - id 行 L72: `priceList.getId(), priceList.getName());`
  - id 行 L81: `protected List<ErpSalPriceList> findCandidatePriceLists(Long partnerId, Long currencyId,`
  - id 行 L116: `protected String resolveCustomerGroup(Long partnerId, IServiceContext context) {`
  - id 行 L127: `protected ErpSalPriceListLine matchLine(Long priceListId, ErpMdMaterialSku sku,`
  - id 行 L155: `return Objects.equals(line.getSkuId(), sku.getId());`

## 域对汇总（权重 = 耦合点行 + id 证据行）

| 引用方 | 被引用方 | 权重 |
| --- | --- | --- |
| finance | master-data | 625 |
| manufacturing | inventory | 249 |
| manufacturing | master-data | 226 |
| sales | master-data | 158 |
| purchase | master-data | 122 |
| cs | master-data | 111 |
| manufacturing | notify | 109 |
| cs | notify | 103 |
| contract | notify | 100 |
| crm | sales | 96 |
| contract | purchase | 89 |
| contract | sales | 89 |
| inventory | master-data | 82 |
| drp | inventory | 78 |
| sales | inventory | 74 |
| assets | master-data | 73 |
| crm | master-data | 72 |
| drp | purchase | 57 |
| aps | manufacturing | 55 |
| sales | finance | 55 |
| drp | master-data | 54 |
| inventory | notify | 51 |
| purchase | inventory | 49 |
| contract | master-data | 47 |
| hr | notify | 43 |
| manufacturing | purchase | 43 |
| manufacturing | quality | 43 |
| hr | finance | 41 |
| purchase | finance | 41 |
| hr | master-data | 40 |
| finance | notify | 39 |
| inventory | purchase | 39 |
| aps | notify | 36 |
| aps | inventory | 34 |
| drp | quality | 34 |
| inventory | manufacturing | 31 |
| finance | assets | 30 |
| finance | inventory | 30 |
| projects | finance | 29 |
| assets | notify | 28 |
| maintenance | notify | 28 |
| b2b | purchase | 27 |
| quality | inventory | 26 |
| manufacturing | finance | 25 |
| maintenance | inventory | 24 |
| quality | sales | 24 |
| logistics | notify | 22 |
| maintenance | finance | 22 |
| maintenance | master-data | 21 |
| b2b | master-data | 18 |
| manufacturing | sales | 17 |
| inventory | finance | 16 |
| purchase | quality | 16 |
| drp | sales | 15 |
| purchase | drp | 15 |
| purchase | projects | 15 |
| sales | quality | 15 |
| manufacturing | maintenance | 14 |
| projects | master-data | 14 |
| projects | notify | 14 |
| sales | notify | 14 |
| drp | manufacturing | 13 |
| cs | quality | 12 |
| maintenance | manufacturing | 12 |
| maintenance | quality | 12 |
| quality | finance | 12 |
| projects | assets | 10 |
| quality | purchase | 10 |
| quality | master-data | 10 |
| assets | maintenance | 9 |
| logistics | finance | 8 |
| logistics | inventory | 8 |
| logistics | sales | 8 |
| assets | finance | 6 |
| crm | notify | 5 |
| finance | purchase | 5 |
| finance | sales | 5 |
| b2b | notify | 3 |
| cs | crm | 2 |
| b2b | sales | 1 |
| purchase | contract | 1 |
| sales | contract | 1 |

---

# 附录 B：dao 层手写跨域 import 全量枚举（非 _gen，2026-08-21）

> 命令：`grep -rn '^import app\.erp\.' module-*/erp-*-dao/src/main/java --include='*.java' | grep -v _gen`（按域排除本域包后）
> 结论：跨域 import 仅存在于 crm/pur/sal 三域（与路线图基线「crm-dao 3 文件 + pur/sal-dao 4 行」一致，全量重扫无新增遗漏）；其余 16 域 dao 跨域 import = 0。

| 域 | 文件:行 | import | 用法性质 |
| --- | --- | --- | --- |
| crm | module-crm/erp-crm-dao/src/main/java/app/erp/crm/biz/IErpCrmLeadBiz.java:4-5 | md ErpMdPartner / sal ErpSalQuotation | 类型级（javadoc 与返回类型；无 id 用法） |
| crm | module-crm/erp-crm-dao/src/main/java/app/erp/crm/biz/IErpCrmConversionBiz.java:4-5 | md ErpMdPartner / sal ErpSalQuotation | 类型级（convertToCustomer/convertToQuotation 返回类型；无 id 用法） |
| crm | module-crm/erp-crm-dao/src/main/java/app/erp/crm/biz/IErpCrmProductConfiguratorBiz.java:11 | sal ErpSalQuotation | 类型级（generateQuote 返回类型；无 id 用法） |
| pur | module-purchase/erp-pur-dao/src/main/java/app/erp/pur/biz/IErpPurPaymentBiz.java:9 | md biz.SettlementAllocation | DTO 参数（见附录 C watch 项） |
| sal | module-sales/erp-sal-dao/src/main/java/app/erp/sal/biz/IErpSalReceiptBiz.java:9 | md biz.SettlementAllocation | DTO 参数（见附录 C watch 项） |
| sal | module-sales/erp-sal-dao/src/main/java/app/erp/sal/dao/entity/ErpSalPriceList.java:4 | md dao.daterange.IDateRange | 接口实现（日期区间 getter，无 id） |
| sal | module-sales/erp-sal-dao/src/main/java/app/erp/sal/dao/entity/ErpSalPriceListLine.java:4 | md dao.daterange.IDateRange | 接口实现（日期区间 getter，无 id） |

补充（非 import 形态）：
- module-manufacturing/erp-mfg-dao/src/main/java/app/erp/mfg/biz/IErpMfgMrpPlanLineBiz.java:14 —— javadoc `{@link app.erp.pur.dao.entity.ErpPurOrder}`，注释级引用，非编译依赖。
- dao 模块 `daoFor(` 仅 md-dao 2 处且均为本域实体（AcctSchemaResolver.java:32、SubjectMappingResolver.java:43）。

# 附录 C：dao 层语义性跨域 FK Long 参数/字段清单（编译器不报错，随本域迁移翻转）

> 扫描：dao 手写代码中 `@Name("xxxId") Long` 参数与 `private Long xxxId` 字段，xxxId 按 orm 实体归属映射到他域（生成命令：`node tools/scan-dao-semantic-fk-params.mjs`）。
> 这些签名/字段在本域未迁移时 Long 自洽（不引用他域 Java 类型，闭包可编译）；**本域迁移时须显式翻转为 String**（语义陷阱 grep 门控类别，各域 plan Phase 2/4 清单输入）。
> 归属映射为启发式（materialId/skuId/uoMId/currencyId/partnerId/supplierId/customerId/employeeId/departmentId→md|hr、warehouseId/locationId→inv、periodId→fin、projectId→prj、assetId→ast、workcenterId/routingId→mfg 等），各域 plan 执行时以本域 orm 的 FK 列为准复核。合计 82 处 / 11 域（aps/crm/cs/drp/finance/inventory/maintenance/manufacturing/purchase/quality/sales）。

| module-aps | module-aps/erp-aps-dao/src/main/java/app/erp/aps/biz/IErpApsAtpCtpService.java:25 | `materialId` | -> master-data |
| module-aps | module-aps/erp-aps-dao/src/main/java/app/erp/aps/biz/IErpApsAtpCtpService.java:31 | `materialId` | -> master-data |
| module-aps | module-aps/erp-aps-dao/src/main/java/app/erp/aps/biz/IErpApsAtpCtpService.java:39 | `materialId` | -> master-data |
| module-aps | module-aps/erp-aps-dao/src/main/java/app/erp/aps/biz/IErpApsOperationOrderBiz.java:110 | `materialId` | -> master-data |
| module-aps | module-aps/erp-aps-dao/src/main/java/app/erp/aps/biz/IErpApsOperationOrderBiz.java:116 | `materialId` | -> master-data |
| module-aps | module-aps/erp-aps-dao/src/main/java/app/erp/aps/biz/IErpApsOperationOrderBiz.java:44 | `routingId` | -> manufacturing |
| module-aps | module-aps/erp-aps-dao/src/main/java/app/erp/aps/biz/ScheduledOperationView.java:14 | `workcenterId(field)` | -> manufacturing |
| module-crm | module-crm/erp-crm-dao/src/main/java/app/erp/crm/biz/IErpCrmForecastBiz.java:23 | `periodId` | -> finance |
| module-crm | module-crm/erp-crm-dao/src/main/java/app/erp/crm/biz/IErpCrmForecastPeriodBiz.java:22 | `periodId` | -> finance |
| module-crm | module-crm/erp-crm-dao/src/main/java/app/erp/crm/biz/IErpCrmForecastPeriodBiz.java:28 | `periodId` | -> finance |
| module-cs | module-cs/erp-cs-dao/src/main/java/app/erp/cs/biz/IErpCsEntitlementBiz.java:52 | `partnerId` | -> master-data |
| module-cs | module-cs/erp-cs-dao/src/main/java/app/erp/cs/biz/IErpCsTicketBiz.java:73 | `materialId` | -> master-data |
| module-cs | module-cs/erp-cs-dao/src/main/java/app/erp/cs/biz/IErpCsTicketBiz.java:78 | `supplierId` | -> master-data |
| module-cs | module-cs/erp-cs-dao/src/main/java/app/erp/cs/biz/IErpCsTicketBiz.java:90 | `customerId` | -> master-data |
| module-drp | module-drp/erp-drp-dao/src/main/java/app/erp/drp/biz/IErpInvDrpLeadTimeRecordBiz.java:40 | `supplierId` | -> master-data |
| module-drp | module-drp/erp-drp-dao/src/main/java/app/erp/drp/biz/IErpInvDrpLeadTimeRecordBiz.java:54 | `supplierId` | -> master-data |
| module-drp | module-drp/erp-drp-dao/src/main/java/app/erp/drp/biz/IErpInvDrpLeadTimeRecordBiz.java:55 | `materialId` | -> master-data |
| module-drp | module-drp/erp-drp-dao/src/main/java/app/erp/drp/biz/IErpInvDrpLeadTimeRecordBiz.java:64 | `supplierId` | -> master-data |
| module-drp | module-drp/erp-drp-dao/src/main/java/app/erp/drp/biz/IErpInvDrpLeadTimeRecordBiz.java:65 | `materialId` | -> master-data |
| module-drp | module-drp/erp-drp-dao/src/main/java/app/erp/drp/biz/LeadTimeStatsBean.java:22 | `supplierId(field)` | -> master-data |
| module-drp | module-drp/erp-drp-dao/src/main/java/app/erp/drp/biz/LeadTimeStatsBean.java:23 | `materialId(field)` | -> master-data |
| module-drp | module-drp/erp-drp-dao/src/main/java/app/erp/drp/dao/dto/DrpSimulationDiffResult.java:41 | `materialId(field)` | -> master-data |
| module-drp | module-drp/erp-drp-dao/src/main/java/app/erp/drp/dao/dto/DrpSimulationDiffResult.java:42 | `warehouseId(field)` | -> inventory |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/biz/IErpFinArApItemBiz.java:25 | `partnerId` | -> master-data |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/biz/IErpFinBudgetLineBiz.java:30 | `acctSchemaId` | -> master-data |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/biz/IErpFinReconciliationBiz.java:27 | `partnerId` | -> master-data |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/biz/IErpFinReconciliationBiz.java:74 | `partnerId` | -> master-data |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/biz/IErpFinReconciliationBiz.java:90 | `partnerId` | -> master-data |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/dto/ArApAgingRow.java:10 | `partnerId(field)` | -> master-data |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/dto/AutoReconUnmatched.java:7 | `partnerId(field)` | -> master-data |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/dto/BudgetVsActualRow.java:24 | `projectId(field)` | -> projects |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/dto/DualSideDiffReport.java:18 | `partnerId(field)` | -> master-data |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/dto/DualSideDiffReport.java:57 | `partnerId(field)` | -> master-data |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/dto/GlMappingDimensions.java:16 | `orgId(field)` | -> master-data |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/dto/GlMappingDimensions.java:17 | `partnerId(field)` | -> master-data |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/dto/GlMappingDimensions.java:19 | `materialId(field)` | -> master-data |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/dto/GlMappingDimensions.java:20 | `materialCategoryId(field)` | -> master-data |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/dto/GlMappingDimensions.java:21 | `warehouseId(field)` | -> inventory |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/dto/GlMappingDimensions.java:22 | `departmentId(field)` | -> hr |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/dto/GlMappingDimensions.java:23 | `projectId(field)` | -> projects |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/dto/ReconciliationReversePreview.java:21 | `partnerId(field)` | -> master-data |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/PostingEvent.java:28 | `acctSchemaId(field)` | -> master-data |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/PostingEvent.java:29 | `orgId(field)` | -> master-data |
| module-finance | module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/PostingEvent.java:30 | `currencyId(field)` | -> master-data |
| module-inventory | module-inventory/erp-inv-dao/src/main/java/app/erp/inv/biz/CostingRecloseReport.java:16 | `periodId(field)` | -> finance |
| module-inventory | module-inventory/erp-inv-dao/src/main/java/app/erp/inv/biz/IErpInvCostingBiz.java:30 | `periodId` | -> finance |
| module-inventory | module-inventory/erp-inv-dao/src/main/java/app/erp/inv/biz/ReservationConsumeLine.java:12 | `materialId(field)` | -> master-data |
| module-inventory | module-inventory/erp-inv-dao/src/main/java/app/erp/inv/biz/ReservationCreateRequest.java:13 | `orgId(field)` | -> master-data |
| module-inventory | module-inventory/erp-inv-dao/src/main/java/app/erp/inv/biz/ReservationLineRequest.java:12 | `materialId(field)` | -> master-data |
| module-inventory | module-inventory/erp-inv-dao/src/main/java/app/erp/inv/biz/ReservationLineRequest.java:13 | `skuId(field)` | -> master-data |
| module-inventory | module-inventory/erp-inv-dao/src/main/java/app/erp/inv/biz/StockMoveLineRequest.java:11 | `materialId(field)` | -> master-data |
| module-inventory | module-inventory/erp-inv-dao/src/main/java/app/erp/inv/biz/StockMoveLineRequest.java:12 | `skuId(field)` | -> master-data |
| module-inventory | module-inventory/erp-inv-dao/src/main/java/app/erp/inv/biz/StockMoveLineRequest.java:13 | `uoMId(field)` | -> master-data |
| module-inventory | module-inventory/erp-inv-dao/src/main/java/app/erp/inv/biz/StockMoveLineRequest.java:16 | `currencyId(field)` | -> master-data |
| module-inventory | module-inventory/erp-inv-dao/src/main/java/app/erp/inv/biz/StockMoveRequest.java:22 | `orgId(field)` | -> master-data |
| module-inventory | module-inventory/erp-inv-dao/src/main/java/app/erp/inv/biz/StockMoveRequest.java:30 | `acctSchemaId(field)` | -> master-data |
| module-inventory | module-inventory/erp-inv-dao/src/main/java/app/erp/inv/biz/StockMoveRequest.java:31 | `currencyId(field)` | -> master-data |
| module-maintenance | module-maintenance/erp-mnt-dao/src/main/java/app/erp/mnt/biz/IErpMntEquipmentBiz.java:26 | `assetId` | -> assets |
| module-maintenance | module-maintenance/erp-mnt-dao/src/main/java/app/erp/mnt/biz/IErpMntEquipmentBiz.java:35 | `assetId` | -> assets |
| module-maintenance | module-maintenance/erp-mnt-dao/src/main/java/app/erp/mnt/biz/MntOpenDowntimeWindow.java:27 | `workcenterId(field)` | -> manufacturing |
| module-manufacturing | module-manufacturing/erp-mfg-dao/src/main/java/app/erp/mfg/biz/BomExplosionNode.java:17 | `materialId(field)` | -> master-data |
| module-manufacturing | module-manufacturing/erp-mfg-dao/src/main/java/app/erp/mfg/biz/CostRollupLineView.java:14 | `materialId(field)` | -> master-data |
| module-manufacturing | module-manufacturing/erp-mfg-dao/src/main/java/app/erp/mfg/biz/IErpMfgCostRollupBiz.java:25 | `materialId` | -> master-data |
| module-manufacturing | module-manufacturing/erp-mfg-dao/src/main/java/app/erp/mfg/biz/IErpMfgMrpPlanLineBiz.java:22 | `supplierId` | -> master-data |
| module-manufacturing | module-manufacturing/erp-mfg-dao/src/main/java/app/erp/mfg/biz/IErpMfgMrpPlanLineBiz.java:23 | `currencyId` | -> master-data |
| module-manufacturing | module-manufacturing/erp-mfg-dao/src/main/java/app/erp/mfg/biz/IErpMfgMrpPlanLineBiz.java:42 | `supplierId` | -> master-data |
| module-manufacturing | module-manufacturing/erp-mfg-dao/src/main/java/app/erp/mfg/biz/IErpMfgMrpPlanLineBiz.java:43 | `currencyId` | -> master-data |
| module-manufacturing | module-manufacturing/erp-mfg-dao/src/main/java/app/erp/mfg/biz/IErpMfgSubcontractOrderBiz.java:44 | `sourceWarehouseId` | -> inventory |
| module-manufacturing | module-manufacturing/erp-mfg-dao/src/main/java/app/erp/mfg/biz/IErpMfgSubcontractOrderBiz.java:55 | `destWarehouseId` | -> inventory |
| module-manufacturing | module-manufacturing/erp-mfg-dao/src/main/java/app/erp/mfg/biz/RecallReport.java:54 | `materialId(field)` | -> master-data |
| module-manufacturing | module-manufacturing/erp-mfg-dao/src/main/java/app/erp/mfg/dao/dto/SimulationDiffResult.java:61 | `materialId(field)` | -> master-data |
| module-purchase | module-purchase/erp-pur-dao/src/main/java/app/erp/pur/biz/ConvertToOrderRequest.java:26 | `warehouseId(field)` | -> inventory |
| module-purchase | module-purchase/erp-pur-dao/src/main/java/app/erp/pur/biz/ConvertToOrderRequest.java:27 | `currencyId(field)` | -> master-data |
| module-purchase | module-purchase/erp-pur-dao/src/main/java/app/erp/pur/biz/IErpPurOrderBiz.java:47 | `supplierId` | -> master-data |
| module-purchase | module-purchase/erp-pur-dao/src/main/java/app/erp/pur/biz/SupplierConversionOption.java:14 | `warehouseId(field)` | -> inventory |
| module-purchase | module-purchase/erp-pur-dao/src/main/java/app/erp/pur/biz/SupplierConversionOption.java:15 | `currencyId(field)` | -> master-data |
| module-quality | module-quality/erp-qa-dao/src/main/java/app/erp/qa/biz/IErpQaInspectionBiz.java:49 | `materialId` | -> master-data |
| module-quality | module-quality/erp-qa-dao/src/main/java/app/erp/qa/biz/IErpQaInspectionBiz.java:52 | `supplierId` | -> master-data |
| module-quality | module-quality/erp-qa-dao/src/main/java/app/erp/qa/biz/IErpQaInspectionBiz.java:53 | `warehouseId` | -> inventory |
| module-sales | module-sales/erp-sal-dao/src/main/java/app/erp/sal/biz/ErpSalExchangeDeliveryLine.java:14 | `materialId(field)` | -> master-data |
| module-sales | module-sales/erp-sal-dao/src/main/java/app/erp/sal/biz/ErpSalExchangeDeliveryLine.java:16 | `skuId(field)` | -> master-data |
| module-sales | module-sales/erp-sal-dao/src/main/java/app/erp/sal/biz/ErpSalExchangeDeliveryLine.java:18 | `uoMId(field)` | -> master-data |

**Watch 项（SettlementAllocation DTO）**：md-dao 手写 `app/erp/md/biz/SettlementAllocation` 含 `private Long invoiceId`（module-master-data/erp-md-dao/.../biz/SettlementAllocation.java:12，语义为 pur/sal 各自的 invoice id）。pur-dao/sal-dao 仅引用类型（本域 invoice 语义），md 迁移时该字段保持 Long 不破坏编译；**pur/sal 各自迁移时其调用侧与该 DTO 字段语义须一并对齐**（pur/sal plan Phase 2/4 登记项）。

# 附录 D：ErpOrgContext / orgId 语义调用点全量清单（2026-08-21）

> 归属：M1.3（common-service 三文件本体适配）+ M2.1（TestErpOrgIsolation 测试修复）+ 各域 plan（eq("orgId") 随域自洽）。

**A. ErpOrgContext 外部调用点（全仓唯一，与基线一致）**

| file:line | 内容 |
| --- | --- |
| module-finance/erp-fin-service/src/test/java/app/erp/common/org/TestErpOrgIsolation.java:72 | `ErpOrgContext.setCurrentOrgId(ctx, 2L)`（M2.1 修复预告） |

**B. ContextProvider.setContextAttr(CONTEXT_ATTR_CURRENT_ORG_ID, ...) 写入点（全部位于同一测试类）**

| file:line | 内容 |
| --- | --- |
| module-finance/erp-fin-service/src/test/java/app/erp/common/org/TestErpOrgIsolation.java:56 | setContextAttr(..., null)（清理） |
| module-finance/erp-fin-service/src/test/java/app/erp/common/org/TestErpOrgIsolation.java:87 | setContextAttr(..., 2L)（Long 字面量，M2.1） |
| module-finance/erp-fin-service/src/test/java/app/erp/common/org/TestErpOrgIsolation.java:102 | setContextAttr(..., null)（清理） |

**C. `eq("orgId", ...)` 业务调用点（34 行 / 13 模块；值全部来自实体 getter 或其派生局部变量——随各域迁移自洽，watch-only）**

| 域 | file:line |
| --- | --- |
| ast | module-assets/erp-ast-service/.../processor/ErpAstInventoryProcessor.java:114 |
| b2b | module-b2b/erp-b2b-service/.../job/ErpB2bOnboardingMonitorJob.java:216 |
| crm | module-crm/erp-crm-service/.../entity/ErpCrmLeadBizModel.java:288,298 |
| drp | module-drp/erp-drp-service/.../drp/DrpReleaseService.java:175; DrpDemandAggregator.java:129,170; safetystock/SafetyStockEngine.java:172,371 |
| fin | module-finance/erp-fin-service/.../processor/ErpFinAccountingPeriodProcessor.java:452; dashboard/ErpFinDashboardBizModel.java:205,262; report/ErpFinReportBizModel.java:586; posting/SchemaPropagator.java:114; posting/ErpFinGlMappingResolver.java:263; posting/ErpFinPostingProcessor.java:519 |
| inv(main) | module-inventory/erp-inv-service/.../costing/CostAdjustmentService.java:277; SpecificCostingStrategy.java:178; LifoCostingStrategy.java:170; FifoCostingStrategy.java:184; BatchCostingStrategy.java:177; entity/ErpInvReservationBizModel.java:414; processor/ErpInvOwnershipTransferProcessor.java:144; stock/StockMoveBookkeeper.java:354,456 |
| inv(test) | module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvOwnershipTransfer.java:266（`ORG_ID` 常量，inv plan Phase 3 登记） |
| mfg | module-manufacturing/erp-mfg-service/.../mrp/MrpEngine.java:215; mrp/DemandAggregator.java:92,177,236; simulation/SimulationMrpEngine.java:431 |
| md(dao) | module-master-data/erp-md-dao/.../dao/AcctSchemaResolver.java:34 |
| md(service) | module-master-data/erp-md-service/.../entity/ErpMdAcctSchemaBizModel.java:33; spi/ErpMdOrganizationReferenceChecker.java:46 |
| sal | module-sales/erp-sal-service/.../processor/ErpSalReturnProcessor.java:353 |

# 附录 E：路线图计数复核（本机复测，2026-08-21）

| 口径 | 路线图 08-16 | 本机复测 08-21 | 复核命令 |
| --- | --- | --- | --- |
| `.getId()` service main | 1026 | **1176**（352 文件） | `rg -c '\.getId\(\)' module-*/erp-*-service/src/main/java` |
| `Long xxxId` 声明行 | ~2020（occurrence ~2680） | **2586 行 / 2896 occurrence** | `rg -c 'Long\s+[a-zA-Z]+Id' ...` |
| 合计行（getId 行 + Long Id 行） | — | 3740 | `rg -c '\.getId\(\)|Long [a-zA-Z]*Id' ...` |

# 附录 F：E2E 影响面清单（M4.1 修复输入，2026-08-21 复测）

> 口径：`tests/e2e/**/*.ts` 全量复测（rg/grep -E），本计划只盘点不修复（M4.1 统一修复，路线图横切 §4）。

## F1 总量复测

| 指标 | 08-16 口径 | 08-21 复测 |
| --- | --- | --- |
| `Number(` 全量 | ~800+/865/822 争议口径 | **874**（105 文件） |
| `Number(lnk.voucherId)` | 11 | **11**（精确不变） |
| `eqFilter('id'` | — | **36** |

## F2 `Number(...)` 参数形态分类

- **简单表达式形态 757 处**（`Number(<单标识符链>)`，正则 `Number\([a-zA-Z_][a-zA-Z0-9_.]*\)`）：
  - **id 族 234 处**（参数名以 `Id` 结尾或为 `id`）——M4.1 必改面：`id` 312 处简单形态中归 id 族主体（含复合表达式后 id 族合计见 F3 说明），`employeeId` 55、`voucherId` 17、`periodId` 13、`materialId` 10、`shiftId` 9、`targetDeptId` 8、`scenarioId` 8、`targetPositionId` 6 等；
  - **数值族 523 处**（金额/控制限等，**合法保持 Number 不改**）：`availableAmount` 13、`netBookValue` 12、`openAmountFunctional` 11、`settledAmount` 9、`outstandingAmount` 9、`usedAmount` 8、`originalValue` 7、`actualAmount` 6、`cl/lcl/ucl`（SPC 控制限）等；
- **复合表达式形态 117 处**（874 − 757，含 `.value` 链/内联表达式）——M4.1 逐个判定 id 族/数值族。

## F3 修复预告分类（M4.1 输入）

1. **id 断言/传参数字形态**：`Number(xxxId)` 族（234+ 复合形态部分）→ 迁移后 id 为 String，断言/查询传参改为字符串比较；
2. **`eqFilter('id'` 36 处 + FK 字段 eqFilter**（`eqFilter('employeeId'` 13、`eqFilter('orderId'` 7、`eqFilter('moveId'` 10、`eqFilter('materialId'` 5、`eqFilter('competencyId'` 5 等）→ 值来源多为行内实体 id，随 String 化调整；
3. **既有正向兼容形态**：`String(...[iI]d...)` 27 处已显式转字符串（迁移后行为不变，核对即可）；
4. **无关面**：数值族 `Number(`（金额/控制限/比率）523+ 处与 id 无关，M4.1 不得误改。

## F4 目录分布（M4.1 工作量锚点）

`business-actions/` 764、`orchestration/` 81、`crud/` 14、`dashboards/` 11、`visual/` 2、`negative/` 2（reports/pages/examples 0）。

**复核命令**（口径可复现）：
```
grep -rEo 'Number\(' tests/e2e --include='*.ts' | wc -l
grep -rEc 'Number\(lnk\.voucherId\)' tests/e2e --include='*.ts'   # 11
grep -rEc "eqFilter\('id'" tests/e2e --include='*.ts'             # 36
grep -rEoh 'Number\([a-zA-Z_][a-zA-Z0-9_.]*\)' tests/e2e --include='*.ts'
```
