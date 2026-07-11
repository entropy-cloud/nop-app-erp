import { test, expect, loginAndNavigate, input, createViaSave, callMutationOk, verifyState, eqFilter, andFilter, deleteByFilter, deleteById } from './_helper';
import { findFirst, findItems, SEED, cleanupVoucherByBillCode } from '../orchestration/_helper';

/**
 * quality ErpQaRecall generateReturns 跨域建退货单浏览器层 E2E（plan 2026-07-11-1234-1 Phase 3）。
 *
 * 验证 `ErpQaRecall__generateReturns(recallId)` 跨域编排动作经 GraphQL /graphql 的全栈可达性：
 *   register→submit→approve→locateTargets(→IN_PROGRESS)→notifyCustomers→generateReturns
 *   → 每个 RecallTarget 经 createSalesReturnFor → IErpSalReturnBiz.save 创建销售退货单。
 *
 * 权威实现（ErpQaRecallBizModel.generateReturns:147）：
 *   - 前置 status=IN_PROGRESS（locateTargets:124 已满足）
 *   - 遍历 ErpQaRecallTarget（skip 已 RETURNED）
 *   - createSalesReturnFor：target.partnerId → ErpSalReturn.customerId + deliveryId +
 *     warehouseId/currencyId（从 delivery）+ uoMId（pickUoMId 从 delivery line）+ lines[{materialId,quantity}]
 *   - target.generatedReturnId = salReturn.id + target.returnStatus = RETURNED
 *
 * 目标定位链路（RecallTargetLocator.locate:78）：
 *   recall.batchId → resolveBatchNo → stockMoveBiz.batchTrace(batchNo) → 遍历 moves
 *   → isSalesOutbound（OUTGOING + DONE + ERP_SAL_DELIVERY + relatedBillCode 非空）
 *   → findDeliveryByCode → 建 RecallTarget（partnerId=delivery.customerId, salesDeliveryId, shippedQty）
 *
 * 前置数据编排（spec 自包含）：
 *   1. ErpInvBatch（batchNo 唯一）
 *   2. INCOMING 移动（line.batchNo）→ DONE：建立批次维度库存余额
 *   3. ErpSalDelivery + Line（供 pickUoMId + createSalesReturnFor 取 warehouseId/currencyId）
 *   4. OUTGOING 业务联动移动（relatedBillType=ERP_SAL_DELIVERY, line.batchNo）→ auto DONE：
 *      生成可被 batchTrace 追溯的批次出库移动（isSalesOutbound 命中）
 *
 * 清理：ErpSalReturn(+Line) + RecallTarget + Recall + Delivery(+Line) + 双移动单(Ledger+Line+Move)
 *   + 库存余额（MAT-1/WH-RAW 种子无余额行，整行删除安全）+ Batch。
 */

const MOVE_REQ_TYPE = 'i_app_erp_inv_biz_StockMoveRequest';
const BDATE = '2026-07-11';

async function cleanupMove(page: import('@playwright/test').Page, move: { id?: any; code?: string } | null): Promise<void> {
  if (!move) return;
  if (move.code) await cleanupVoucherByBillCode(page, move.code);
  if (move.id != null) {
    await deleteByFilter(page, 'ErpInvStockLedger', eqFilter('moveId', Number(move.id)));
    await deleteByFilter(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(move.id)));
    await deleteById(page, 'ErpInvStockMove', move.id);
  }
}

test.describe('quality ErpQaRecall generateReturns cross-domain return creation', () => {
  test('generateReturns: recall with batch-traced delivery → ErpSalReturn created', async ({ page }) => {
    await loginAndNavigate(page, '/ErpQaRecall-main');

    const ts = Date.now();
    const batchNo = `E2E-RECALL-BATCH-${ts}`;
    const deliveryCode = `E2E-RECALL-DLV-${ts}`;
    const recallCode = `E2E-RECALL-GEN-${ts}`;

    let batch: any = null;
    let incoming: any = null;
    let delivery: any = null;
    let outgoing: any = null;
    let recall: any = null;

    try {
      // 1. ErpInvBatch（batchNo 唯一，供 batchTrace 追溯）
      batch = await createViaSave(
        page, 'ErpInvBatch',
        {
          orgId: SEED.ORG, batchNo, materialId: SEED.MAT_1, warehouseId: SEED.WH_RAW,
          totalQuantity: 20, availableQuantity: 20,
          productionDate: BDATE, status: 'OPEN',
        },
        'id',
      );

      // 2. INCOMING 移动（line.batchNo）→ CONFIRMED → complete → DONE（建立批次维度库存余额）
      incoming = await callMutationOk(
        page, 'ErpInvStockMove', 'generateMove',
        {
          request: input(MOVE_REQ_TYPE, {
            moveType: 'INCOMING', orgId: SEED.ORG, businessDate: BDATE,
            destWarehouseId: SEED.WH_RAW, currencyId: SEED.CURRENCY,
            lines: [{
              materialId: SEED.MAT_1, uoMId: SEED.UOM,
              quantity: 20, unitCost: 10, currencyId: SEED.CURRENCY, batchNo,
            }],
          }),
        },
        'id code docStatus',
      );
      expect(incoming.docStatus, 'independent INCOMING stops at CONFIRMED').toBe('CONFIRMED');
      await callMutationOk(page, 'ErpInvStockMove', 'complete', { moveId: incoming.id }, 'id docStatus');

      // 3. ErpSalDelivery + Line（供 pickUoMId + createSalesReturnFor 取 warehouseId/currencyId）
      delivery = await createViaSave(
        page, 'ErpSalDelivery',
        {
          code: deliveryCode, orgId: SEED.ORG, customerId: SEED.CUSTOMER, warehouseId: SEED.WH_RAW,
          businessDate: BDATE, currencyId: SEED.CURRENCY, exchangeRate: 1,
          docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', posted: false,
        },
        'id',
      );
      await createViaSave(
        page, 'ErpSalDeliveryLine',
        {
          deliveryId: delivery.id, lineNo: 1, materialId: SEED.MAT_1, uoMId: SEED.UOM,
          quantity: 10, unitPrice: 10, warehouseId: SEED.WH_RAW,
        },
        'id',
      );

      // 4. OUTGOING 业务联动移动（relatedBillType=ERP_SAL_DELIVERY, line.batchNo）→ auto DONE
      //    生成可被 batchTrace 追溯的批次出库移动（isSalesOutbound 命中）
      outgoing = await callMutationOk(
        page, 'ErpInvStockMove', 'generateMove',
        {
          request: input(MOVE_REQ_TYPE, {
            moveType: 'OUTGOING', orgId: SEED.ORG, businessDate: BDATE,
            sourceWarehouseId: SEED.WH_RAW,
            relatedBillType: 'ERP_SAL_DELIVERY', relatedBillCode: deliveryCode,
            currencyId: SEED.CURRENCY,
            lines: [{
              materialId: SEED.MAT_1, uoMId: SEED.UOM,
              quantity: 10, currencyId: SEED.CURRENCY, batchNo,
            }],
          }),
        },
        'id code docStatus',
      );
      expect(outgoing.docStatus, 'business-linked OUTGOING auto-DONE').toBe('DONE');

      // 5. Recall register（batchId → batchNo 解析供 locateTargets）
      recall = await callMutationOk(
        page, 'ErpQaRecall', 'register',
        {
          data: input('Map', {
            code: recallCode, recallName: 'E2E Recall generateReturns',
            triggerType: 'MANUAL', severityLevel: 'HIGH',
            businessDate: BDATE, materialId: SEED.MAT_1, batchId: batch.id,
          }),
        },
        'id status approveStatus',
      );
      expect(recall.status, 'new recall status=OPEN').toBe('OPEN');
      expect(recall.approveStatus, 'new recall approveStatus=UNSUBMITTED').toBe('UNSUBMITTED');

      // 6. 审批轴：submit → approve
      await callMutationOk(page, 'ErpQaRecall', 'submitForApproval', { id: recall.id }, 'id');
      await callMutationOk(page, 'ErpQaRecall', 'approve', { id: recall.id }, 'id');
      let rs = await verifyState(page, 'ErpQaRecall', recall.id, 'status approveStatus');
      expect(rs.approveStatus, 'after approve approveStatus=APPROVED').toBe('APPROVED');
      expect(rs.status, 'after approve status=APPROVED').toBe('APPROVED');

      // 7. locateTargets → batchTrace 追溯批次出库移动 → 建 RecallTarget → IN_PROGRESS
      await callMutationOk(page, 'ErpQaRecall', 'locateTargets', { recallId: recall.id }, 'id status');
      rs = await verifyState(page, 'ErpQaRecall', recall.id, 'status');
      expect(rs.status, 'after locateTargets status=IN_PROGRESS').toBe('IN_PROGRESS');

      // 8. notifyCustomers → target.returnStatus=NOTIFIED
      await callMutationOk(page, 'ErpQaRecall', 'notifyCustomers', { recallId: recall.id }, 'id');

      // 9. generateReturns → 跨域建退货单
      await callMutationOk(page, 'ErpQaRecall', 'generateReturns', { recallId: recall.id }, 'id');

      // ---- 断言：RecallTarget returnStatus=RETURNED + generatedReturnId 非空 ----
      const targets = await findItems<any>(
        page, 'ErpQaRecallTarget', eqFilter('recallId', Number(recall.id)),
        'id returnStatus generatedReturnId partnerId salesDeliveryId',
      );
      expect(targets.length, 'should have at least one recall target').toBeGreaterThanOrEqual(1);
      const target = targets[0];
      expect(target.returnStatus, 'target returnStatus=RETURNED').toBe('RETURNED');
      expect(target.generatedReturnId, 'target generatedReturnId non-null').toBeTruthy();
      expect(Number(target.partnerId), 'target partnerId=delivery.customerId').toBe(SEED.CUSTOMER);

      // ---- 断言：ErpSalReturn 存在 + customerId 匹配 + docStatus=DRAFT ----
      const salReturn = await verifyState(
        page, 'ErpSalReturn', target.generatedReturnId,
        'id code customerId docStatus approveStatus',
      );
      expect(salReturn, 'ErpSalReturn should exist').toBeTruthy();
      expect(Number(salReturn.customerId), 'return customerId=target.partnerId').toBe(Number(target.partnerId));
      expect(salReturn.docStatus, 'return docStatus=DRAFT').toBe('DRAFT');
      expect(salReturn.approveStatus, 'return approveStatus=UNSUBMITTED').toBe('UNSUBMITTED');

      // ---- 清理 ----
      // ErpSalReturn + Line
      await deleteByFilter(page, 'ErpSalReturnLine', eqFilter('returnId', Number(target.generatedReturnId)));
      await deleteById(page, 'ErpSalReturn', target.generatedReturnId);
      // RecallTarget
      await deleteByFilter(page, 'ErpQaRecallTarget', eqFilter('recallId', Number(recall.id)));
    } finally {
      // Recall（try 块已清理 target；recall 自身无论成功失败都需删）
      if (recall) await deleteById(page, 'ErpQaRecall', recall.id);
      // Delivery Line + Delivery
      if (delivery) {
        await deleteByFilter(page, 'ErpSalDeliveryLine', eqFilter('deliveryId', Number(delivery.id)));
        await deleteById(page, 'ErpSalDelivery', delivery.id);
      }
      // 双移动单（OUTGOING 先于 INCOMING，因 OUTGOING 依赖 INCOMING 建立的库存）
      await cleanupMove(page, outgoing);
      await cleanupMove(page, incoming);
      // 库存余额（MAT-1/WH-RAW 种子无余额行，整行删除安全）
      await deleteByFilter(
        page, 'ErpInvStockBalance',
        andFilter(eqFilter('materialId', SEED.MAT_1), eqFilter('warehouseId', SEED.WH_RAW)),
      );
      // Batch
      if (batch) await deleteById(page, 'ErpInvBatch', batch.id);
    }
  });
});
