import type { Page } from '@playwright/test';
import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  deleteByFilter,
  deleteById,
  runSubcontractChain,
  cleanupSubcontract,
  runSubcontractMrpRelease,
  cleanupSubcontractMrpRelease,
  findPageTotal,
  findFirst,
  verifyState,
  findVoucherIdByBillCode,
  assertVoucherLines,
  eqFilter,
  andFilter,
  SUBCONTRACT_EXPECT,
  SUBCONTRACT_MRP_EXPECT,
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

/**
 * 建最小可取消委外单（DRAFT）+ 行 + 测试专用物料（cancel 正路径专用）。
 * DRAFT/SUBMITTED/APPROVED 取消不消耗库存、不产凭证，故无需前置备货/过账。
 */
async function buildCancellableOrder(page: Page, suffix: string) {
  const ts = Date.now();
  const componentMat = await createViaSave(
    page, 'ErpMdMaterial',
    {
      code: `E2E-SC-CANCEL-MAT-${ts}-${suffix}`, name: `E2E 委外取消组件 ${suffix}`,
      materialType: 'RAW_MATERIAL', uoMId: SEED.UOM_KG, status: 'ACTIVE',
      costMethod: 'MOVING_AVERAGE', defaultWarehouseId: SEED.WH_RAW,
    },
    'id',
  );
  const productMat = await createViaSave(
    page, 'ErpMdMaterial',
    {
      code: `E2E-SC-CANCEL-PROD-${ts}-${suffix}`, name: `E2E 委外取消成品 ${suffix}`,
      materialType: 'FINISHED_PRODUCT', uoMId: SEED.UOM, status: 'ACTIVE',
      costMethod: 'MOVING_AVERAGE', defaultWarehouseId: SEED.WH_RAW,
    },
    'id',
  );
  const order = await createViaSave(
    page, 'ErpMfgSubcontractOrder',
    {
      code: `E2E-SC-CANCEL-${ts}-${suffix}`, orgId: SEED.ORG, supplierId: SEED.SUPPLIER,
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
      subcontractOrderId: order.id, lineNo: 10,
      materialId: componentMat.id, uoMId: SEED.UOM_KG, quantity: SUBCONTRACT_EXPECT.lineQty,
    },
    'id',
  );
  return { order, componentMat, productMat };
}

async function cleanupCancellableOrder(
  page: Page,
  order: any, componentMat: any, productMat: any,
) {
  if (order) {
    await deleteByFilter(page, 'ErpMfgSubcontractOrderLine', eqFilter('subcontractOrderId', Number(order.id)));
    await deleteById(page, 'ErpMfgSubcontractOrder', order.id);
  }
  if (productMat) await deleteById(page, 'ErpMdMaterial', productMat.id);
  if (componentMat) await deleteById(page, 'ErpMdMaterial', componentMat.id);
}

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

  // ===== plan 2026-07-14-0035-2 Phase 1: MRP 释放→自动建单 =====

  test('MRP release → auto-create APPROVED subcontract skeleton (code=SUB-MRP-{lineId}, fee=0)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgSubcontractOrder-main');

    const r = await runSubcontractMrpRelease(page);
    try {
      // 计划行 isFirmed=true + convertedBillCode 回写
      expect(r.mrpPlanLine?.isFirmed, 'plan line isFirmed=true after release').toBe(true);
      const billCode = r.mrpPlanLine?.convertedBillCode;
      expect(billCode, 'convertedBillCode written back').toBeTruthy();

      // 自动建 APPROVED 委外单骨架
      const o = r.releasedOrder;
      expect(o, 'released subcontract order should exist').toBeTruthy();
      expect(o.code, 'released code = SUB-MRP-{lineId}').toBe(`SUB-MRP-${r.mrpPlanLine.id}`);
      expect(o.docStatus, 'released docStatus=APPROVED (skip approval)').toBe('APPROVED');
      expect(o.approveStatus, 'released approveStatus=APPROVED').toBe('APPROVED');
      expect(Number(o.processingFee), 'released processingFee=0 skeleton').toBe(0);
      expect(Number(o.totalAmount), 'released totalAmount=0 skeleton').toBe(0);
      expect(o.code, 'convertedBillCode matches released order code').toBe(billCode);
      expect(Number(o.supplierId), 'released supplierId from release arg').toBe(SEED.SUPPLIER);

      // 单行 qty=plannedQuantity
      const lineTotal = await findPageTotal(page, 'ErpMfgSubcontractOrderLine', eqFilter('subcontractOrderId', Number(o.id)));
      expect(lineTotal, 'released order has exactly 1 line').toBe(1);
      const line = await findFirst<any>(page, 'ErpMfgSubcontractOrderLine', eqFilter('subcontractOrderId', Number(o.id)), 'quantity');
      expect(Number(line?.quantity), 'released line qty=plannedQuantity').toBe(SUBCONTRACT_MRP_EXPECT.plannedQuantity);

      // 计划 status→FIRMED（单行计划释放后全部 firmed）
      const plan = await verifyState(page, 'ErpMfgMrpPlan', r.mrpPlan.id, 'status');
      expect(plan?.status, 'mrp plan status=FIRMED after all lines firmed').toBe('FIRMED');
    } finally {
      await cleanupSubcontractMrpRelease(page, r);
    }
  });

  // ===== plan 2026-07-14-0035-2 Phase 2: 多行发料 + 部分收货 =====

  test('multi-line issue: 3 component lines → 3 OUTGOING move lines + aggregated SI voucher', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgSubcontractOrder-main');

    const r = await runSubcontractChain(page, { lineCount: 3 });
    try {
      // issue OUTGOING 移动单含 3 行明细（每行独立组件物料）
      const issueLineTotal = await findPageTotal(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(r.issueMove?.id)));
      expect(issueLineTotal, 'issue OUTGOING move should have 3 lines').toBe(3);

      // 汇总 SUBCONTRACT_ISSUE 凭证：Dr 委外物资 1408 汇总（3 行 × issueCost 10 = 30）+ Cr 原材料 1401 按物料分列（3 行各 10）。
      // SubcontractIssueAcctDocProvider 镜像 ManufacturingIssueAcctDocProvider：Dr 汇总委外物资 / Cr 按物料分列原材料。
      const issueVoucherId = await findVoucherIdByBillCode(page, r.codes.order + '-SI');
      expect(issueVoucherId, 'aggregated SI voucher should exist').toBeTruthy();
      const expectedIssueCost = 3 * SUBCONTRACT_EXPECT.issueCost;
      const dr1408 = await findFirst<any>(page, 'ErpFinVoucherLine',
        andFilter(eqFilter('voucherId', Number(issueVoucherId)), eqFilter('subjectCode', '1408')),
        'debitAmount creditAmount');
      expect(dr1408, 'SI voucher Dr 1408 line exists').toBeTruthy();
      expect(Number(dr1408?.debitAmount), 'SI Dr 1408 aggregated = 3 × issueCost').toBe(expectedIssueCost);
      const cr1401Count = await findPageTotal(page, 'ErpFinVoucherLine',
        andFilter(eqFilter('voucherId', Number(issueVoucherId)), eqFilter('subjectCode', '1401')));
      expect(cr1401Count, 'SI Cr 1401 split per material = 3 lines').toBe(3);

      // 终态 COMPLETED + posted
      const state = await verifyState(page, 'ErpMfgSubcontractOrder', r.order.id, 'docStatus posted');
      expect(state?.docStatus, 'multi-line final docStatus=COMPLETED').toBe('COMPLETED');
      expect(state?.posted, 'multi-line posted=true').toBe(true);
    } finally {
      await cleanupSubcontract(page, r);
    }
  });

  test('partial receive: receivedQty < lineQty → RECEIVED + MANUFACTURE move line quantity=receivedQty', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgSubcontractOrder-main');

    const r = await runSubcontractChain(page, { stopAfterIssue: true });
    try {
      const partialQty = 1;
      await callMutationOk(
        page, 'ErpMfgSubcontractOrder', 'receiveFinished',
        { subcontractOrderId: r.order.id, receivedQty: partialQty, destWarehouseId: SEED.WH_RAW },
        'id docStatus',
      );

      const ws = await verifyState(page, 'ErpMfgSubcontractOrder', r.order.id, 'docStatus');
      expect(ws?.docStatus, 'partial receive → RECEIVED').toBe('RECEIVED');

      // MANUFACTURE 入库移动行 quantity = partialQty（部分量）
      const receiptMove = await findFirst<any>(
        page, 'ErpInvStockMove',
        andFilter(eqFilter('relatedBillType', 'ERP_MFG_SUBCONTRACT_RECEIPT'), eqFilter('relatedBillCode', r.codes.order)),
        'id code',
      );
      expect(receiptMove, 'receipt MANUFACTURE move exists').toBeTruthy();
      r.receiptMove = receiptMove;

      const receiptLine = await findFirst<any>(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(receiptMove.id)), 'quantity');
      expect(Number(receiptLine?.quantity), 'receipt move line quantity=receivedQty (partial)').toBe(partialQty);
    } finally {
      await cleanupSubcontract(page, r);
    }
  });

  // ===== plan 2026-07-14-0035-2 Phase 3: cancel 路径 =====

  test('cancel positive path: DRAFT / SUBMITTED / APPROVED → CANCELLED', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgSubcontractOrder-main');

    // DRAFT → cancel → CANCELLED
    const a = await buildCancellableOrder(page, 'DRAFT');
    try {
      await callMutationOk(page, 'ErpMfgSubcontractOrder', 'cancel', { subcontractOrderId: a.order.id }, 'id docStatus');
      const sa = await verifyState(page, 'ErpMfgSubcontractOrder', a.order.id, 'docStatus');
      expect(sa?.docStatus, 'DRAFT → cancel → CANCELLED').toBe('CANCELLED');
    } finally {
      await cleanupCancellableOrder(page, a.order, a.componentMat, a.productMat);
    }

    // SUBMITTED → cancel → CANCELLED
    const b = await buildCancellableOrder(page, 'SUB');
    try {
      await callMutationOk(page, 'ErpMfgSubcontractOrder', 'submitForApproval', { id: b.order.id }, 'id');
      await callMutationOk(page, 'ErpMfgSubcontractOrder', 'cancel', { subcontractOrderId: b.order.id }, 'id docStatus');
      const sb = await verifyState(page, 'ErpMfgSubcontractOrder', b.order.id, 'docStatus');
      expect(sb?.docStatus, 'SUBMITTED → cancel → CANCELLED').toBe('CANCELLED');
    } finally {
      await cleanupCancellableOrder(page, b.order, b.componentMat, b.productMat);
    }

    // APPROVED → cancel → CANCELLED
    const c = await buildCancellableOrder(page, 'APV');
    try {
      await callMutationOk(page, 'ErpMfgSubcontractOrder', 'submitForApproval', { id: c.order.id }, 'id');
      await callMutationOk(page, 'ErpMfgSubcontractOrder', 'approve', { id: c.order.id }, 'id');
      await callMutationOk(page, 'ErpMfgSubcontractOrder', 'cancel', { subcontractOrderId: c.order.id }, 'id docStatus');
      const sc = await verifyState(page, 'ErpMfgSubcontractOrder', c.order.id, 'docStatus');
      expect(sc?.docStatus, 'APPROVED → cancel → CANCELLED').toBe('CANCELLED');
    } finally {
      await cleanupCancellableOrder(page, c.order, c.componentMat, c.productMat);
    }
  });

  test('cancel guard: ISSUED / RECEIVED / COMPLETED rejected (illegal status transition)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgSubcontractOrder-main');

    const r = await runSubcontractChain(page, { stopAfterIssue: true });
    try {
      // ISSUED → cancel rejected
      const rej1 = await callMutation(page, 'ErpMfgSubcontractOrder', 'cancel', { subcontractOrderId: r.order.id }, 'id');
      expect(rej1.errors, 'ISSUED → cancel should be rejected').toBeTruthy();
      let s = await verifyState(page, 'ErpMfgSubcontractOrder', r.order.id, 'docStatus');
      expect(s?.docStatus, 'docStatus unchanged (ISSUED) after rejected cancel').toBe('ISSUED');

      // 手动收货 → RECEIVED
      await callMutationOk(
        page, 'ErpMfgSubcontractOrder', 'receiveFinished',
        { subcontractOrderId: r.order.id, receivedQty: SUBCONTRACT_EXPECT.receivedQty, destWarehouseId: SEED.WH_RAW },
        'id docStatus',
      );
      r.receiptMove = await findFirst<any>(
        page, 'ErpInvStockMove',
        andFilter(eqFilter('relatedBillType', 'ERP_MFG_SUBCONTRACT_RECEIPT'), eqFilter('relatedBillCode', r.codes.order)),
        'id code',
      );
      s = await verifyState(page, 'ErpMfgSubcontractOrder', r.order.id, 'docStatus');
      expect(s?.docStatus, 'after manual receive docStatus=RECEIVED').toBe('RECEIVED');

      // RECEIVED → cancel rejected
      const rej2 = await callMutation(page, 'ErpMfgSubcontractOrder', 'cancel', { subcontractOrderId: r.order.id }, 'id');
      expect(rej2.errors, 'RECEIVED → cancel should be rejected').toBeTruthy();
      s = await verifyState(page, 'ErpMfgSubcontractOrder', r.order.id, 'docStatus');
      expect(s?.docStatus, 'docStatus unchanged (RECEIVED) after rejected cancel').toBe('RECEIVED');

      // 手动加工费过账 → COMPLETED
      await callMutationOk(page, 'ErpMfgSubcontractOrder', 'postProcessingFee', { subcontractOrderId: r.order.id }, 'id docStatus posted');
      s = await verifyState(page, 'ErpMfgSubcontractOrder', r.order.id, 'docStatus');
      expect(s?.docStatus, 'after manual postFee docStatus=COMPLETED').toBe('COMPLETED');

      // COMPLETED → cancel rejected
      const rej3 = await callMutation(page, 'ErpMfgSubcontractOrder', 'cancel', { subcontractOrderId: r.order.id }, 'id');
      expect(rej3.errors, 'COMPLETED → cancel should be rejected').toBeTruthy();
      s = await verifyState(page, 'ErpMfgSubcontractOrder', r.order.id, 'docStatus');
      expect(s?.docStatus, 'docStatus unchanged (COMPLETED) after rejected cancel').toBe('COMPLETED');
    } finally {
      await cleanupSubcontract(page, r);
    }
  });
});

// 显式标注种子引用（供 lint/可读性）
void findFirst;
