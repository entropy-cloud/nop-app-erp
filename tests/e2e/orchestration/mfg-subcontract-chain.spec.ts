import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  deleteByFilter,
  deleteById,
  runSubcontractChain,
  cleanupSubcontract,
  findPageTotal,
  findFirst,
  verifyState,
  findVoucherIdByBillCode,
  assertVoucherLines,
  eqFilter,
  andFilter,
  SUBCONTRACT_EXPECT,
  SEED,
} from './_helper';

/**
 * 委外加工单生命周期编排浏览器层 E2E（plan 2026-07-13-0701-2 Phase 1）。
 *
 * 经 GraphQL /graphql 驱动 ErpMfgSubcontractOrder 全链：
 *   建测试专用组件/成品物料 → 前置备货（组件物料建库存）→ 委外订单头+行 → 审批轴（submit→approve）
 *   → 发料（issueMaterials APPROVED→ISSUED，触发 OUTGOING 移动 + SUBCONTRACT_ISSUE 凭证）
 *   → 收货（receiveFinished ISSUED→RECEIVED，触发 MANUFACTURE 入库移动 + SUBCONTRACT_RECEIPT 凭证）
 *   → 加工费过账（postProcessingFee RECEIVED→COMPLETED，SUBCONTRACT_FEE 凭证 + posted=true）。
 *
 * 断言三层（生命周期 + 库存 + GL 凭证）：
 *   1. 状态流转——每步 docStatus 翻转经 helper 内 verifyState `__get` 独立断言（DRAFT→SUBMITTED→APPROVED
 *      →ISSUED→RECEIVED→COMPLETED）。
 *   2. 库存移动——发料 OUTGOING 移动（relatedBillType=ERP_MFG_SUBCONTRACT_ISSUE）+ 收货 MANUFACTURE 入库移动
 *      （relatedBillType=ERP_MFG_SUBCONTRACT_RECEIPT）各存在一条。
 *   3. 三段 GL 凭证 + assertVoucherLines 凭证行精确数值断言：
 *      SUBCONTRACT_ISSUE `{code}-SI` Dr 1408 委外物资 10 / Cr 1401 原材料 10
 *      SUBCONTRACT_RECEIPT `{code}-SR` Dr 1405 产成品 50 / Cr 1408 委外物资 50
 *      SUBCONTRACT_FEE `{code}-SF` Dr 1408 委外物资 50 / Cr 2202 应付账款 50
 *   4. posted=true（COMPLETED 后由 postProcessingFee 标记）。
 *   5. 非法迁移守卫（DRAFT 直接 issueMaterials 抛 ERR_SUBCONTRACT_ILLEGAL_STATUS_TRANSITION）。
 *
 * 确定性期望值（见 helper SUBCONTRACT_EXPECT）：组件用量 2 × moving-average unitCost 5 = issueCost 10；
 *   processingFee 50；receivedQty 1 → receiptUnitCost 50/1 = 50 → receiptCost 50。
 *
 * 清理：链路创建不可逆下游产物（库存流水/余额、GL 凭证），全栈共享同一 H2 实例，不清理会污染
 *   下游数值断言基线。finally 调 cleanupSubcontract 逐域逻辑删除。
 */

test.describe('manufacturing SubcontractOrder full lifecycle orchestration browser-layer E2E', () => {
  test('full chain: approve → issue → receive → post fee (lifecycle + stock moves + GL vouchers)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgSubcontractOrder-main');

    const r = await runSubcontractChain(page);
    try {
      // ---- 发料出库产物：issueMaterials 触发 OUTGOING 移动（relatedBillType=ERP_MFG_SUBCONTRACT_ISSUE）----
      const issueMoveTotal = await findPageTotal(
        page, 'ErpInvStockMove',
        andFilter(eqFilter('relatedBillType', 'ERP_MFG_SUBCONTRACT_ISSUE'), eqFilter('relatedBillCode', r.codes.order)),
      );
      expect(issueMoveTotal, 'issueMaterials should produce an ErpInvStockMove').toBeGreaterThan(0);

      // ---- 收货入库产物：receiveFinished 触发 MANUFACTURE 入库移动（relatedBillType=ERP_MFG_SUBCONTRACT_RECEIPT）----
      const receiptMoveTotal = await findPageTotal(
        page, 'ErpInvStockMove',
        andFilter(eqFilter('relatedBillType', 'ERP_MFG_SUBCONTRACT_RECEIPT'), eqFilter('relatedBillCode', r.codes.order)),
      );
      expect(receiptMoveTotal, 'receiveFinished should produce an ErpInvStockMove').toBeGreaterThan(0);

      // ---- 发料 GL 凭证：SUBCONTRACT_ISSUE（Dr 委外物资 1408 / Cr 原材料 1401）----
      const issueVoucherId = await findVoucherIdByBillCode(page, r.codes.order + '-SI');
      expect(issueVoucherId, 'SUBCONTRACT_ISSUE voucher should exist').toBeTruthy();
      await assertVoucherLines(page, issueVoucherId, [
        { subjectCode: '1408', dcDirection: 'DEBIT', debitAmount: SUBCONTRACT_EXPECT.issueCost, creditAmount: 0 },
        { subjectCode: '1401', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: SUBCONTRACT_EXPECT.issueCost },
      ]);

      // ---- 收货 GL 凭证：SUBCONTRACT_RECEIPT（Dr 产成品 1405 / Cr 委外物资 1408）----
      const receiptVoucherId = await findVoucherIdByBillCode(page, r.codes.order + '-SR');
      expect(receiptVoucherId, 'SUBCONTRACT_RECEIPT voucher should exist').toBeTruthy();
      await assertVoucherLines(page, receiptVoucherId, [
        { subjectCode: '1405', dcDirection: 'DEBIT', debitAmount: SUBCONTRACT_EXPECT.receiptCost, creditAmount: 0 },
        { subjectCode: '1408', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: SUBCONTRACT_EXPECT.receiptCost },
      ]);

      // ---- 加工费 GL 凭证：SUBCONTRACT_FEE（Dr 委外物资 1408 / Cr 应付账款 2202）----
      const feeVoucherId = await findVoucherIdByBillCode(page, r.codes.order + '-SF');
      expect(feeVoucherId, 'SUBCONTRACT_FEE voucher should exist').toBeTruthy();
      await assertVoucherLines(page, feeVoucherId, [
        { subjectCode: '1408', dcDirection: 'DEBIT', debitAmount: SUBCONTRACT_EXPECT.processingFee, creditAmount: 0 },
        { subjectCode: '2202', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: SUBCONTRACT_EXPECT.processingFee },
      ]);

      // ---- 终态断言：COMPLETED + posted=true ----
      const state = await verifyState(page, 'ErpMfgSubcontractOrder', r.order.id, 'docStatus posted');
      expect(state?.docStatus, 'final docStatus=COMPLETED').toBe('COMPLETED');
      expect(state?.posted, 'posted=true after postProcessingFee').toBe(true);
    } finally {
      await cleanupSubcontract(page, r);
    }
  });

  test('guard: DRAFT → issueMaterials rejected (illegal status transition)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgSubcontractOrder-main');

    const ts = Date.now();
    const componentMat = await createViaSave(
      page, 'ErpMdMaterial',
      {
        code: `E2E-SC-ILL-MAT-${ts}`, name: 'E2E 委外非法迁移守卫组件',
        materialType: 'RAW_MATERIAL', uoMId: SEED.UOM_KG, status: 'ACTIVE',
        costMethod: 'MOVING_AVERAGE', defaultWarehouseId: SEED.WH_RAW,
      },
      'id',
    );
    const productMat = await createViaSave(
      page, 'ErpMdMaterial',
      {
        code: `E2E-SC-ILL-PROD-${ts}`, name: 'E2E 委外非法迁移守卫成品',
        materialType: 'FINISHED_PRODUCT', uoMId: SEED.UOM, status: 'ACTIVE',
        costMethod: 'MOVING_AVERAGE', defaultWarehouseId: SEED.WH_RAW,
      },
      'id',
    );
    const order = await createViaSave(
      page, 'ErpMfgSubcontractOrder',
      {
        code: `E2E-SC-ILL-${ts}`, orgId: SEED.ORG, supplierId: SEED.SUPPLIER,
        productId: productMat.id, businessDate: '2026-07-13',
        currencyId: SEED.CURRENCY, exchangeRate: 1,
        processingFee: SUBCONTRACT_EXPECT.processingFee, totalAmount: SUBCONTRACT_EXPECT.processingFee,
        docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', postedStatus: 'DRAFT',
      },
      'id docStatus',
    );
    await createViaSave(
      page, 'ErpMfgSubcontractOrderLine',
      {
        subcontractOrderId: order.id, lineNo: 1,
        materialId: componentMat.id, uoMId: SEED.UOM_KG, quantity: SUBCONTRACT_EXPECT.lineQty,
      },
      'id',
    );

    try {
      // DRAFT → issueMaterials 应被 requireStatus(APPROVED) 拒绝
      const rej = await callMutation(
        page, 'ErpMfgSubcontractOrder', 'issueMaterials',
        { subcontractOrderId: order.id, sourceWarehouseId: SEED.WH_RAW },
        'id',
      );
      expect(rej.errors, 'DRAFT→issueMaterials should be rejected as illegal status transition').toBeTruthy();

      const s = await verifyState(page, 'ErpMfgSubcontractOrder', order.id, 'docStatus');
      expect(s?.docStatus, 'docStatus unchanged after rejected issueMaterials').toBe('DRAFT');
    } finally {
      await deleteByFilter(page, 'ErpMfgSubcontractOrderLine', eqFilter('subcontractOrderId', Number(order.id)));
      await deleteById(page, 'ErpMfgSubcontractOrder', order.id);
      await deleteById(page, 'ErpMdMaterial', productMat.id);
      await deleteById(page, 'ErpMdMaterial', componentMat.id);
    }
  });
});

// 显式标注种子引用（供 lint/可读性）
void findFirst;
