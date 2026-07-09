import {
  test,
  expect,
  loginAndNavigate,
  runO2cChain,
  cleanupO2c,
  findPageTotal,
  findItems,
  eqFilter,
  andFilter,
  O2C_EXPECT,
} from './_helper';

/**
 * O2C（Order-to-Cash）核心链路浏览器层 E2E（plan 2026-07-09-1249-1 Phase 2）。
 *
 * 经 GraphQL /graphql 驱动全链：SO __save → submitForApproval → approve（信用控制通过）
 *   → Delivery __save(orderId) → submitForApproval → approve（触发出库移动 + posted + SO deliveryStatus 回写）
 *   → Invoice __save(deliveryLineId) → submitForApproval → approve（触发 GL 过账 posted=true + AR 辅助账）。
 *
 * 断言三层：
 *   1. 状态流转——SO/Delivery/Invoice approveStatus UNSUBMITTED→SUBMITTED→APPROVED（见 helper）。
 *   2. 库存出库移动产物——Delivery approve 后 ErpInvStockMove(relatedBillCode=delivery.code) 存在 + DONE。
 *   3. GL 过账产物——Invoice approve 后 posted=true + voucher_bill_r 回链 + AR 辅助账项
 *      （sourceBillType=AR_INVOICE, direction=RECEIVABLE, openAmountSource=含税总额 113）。
 *
 * 备货前置：WH-RAW/MAT-1 种子无余额，出库会因负库存禁止（CONFIG_ALLOW_NEGATIVE_STOCK 默认 false）失败。
 * 故链路前先 generateMove INCOMING 备货 20（独立移动 → CONFIRMED → complete → DONE），出库消费 10 余 10。
 * WH-RAW/MAT-1 余额无种子行，清理时整行删除安全（不污染 inventory dashboard totalValue 基线）。
 *
 * 清理：含前置备货移动在内全部产物逐域逻辑删除（finally）。
 */

test.describe('O2C orchestration chain (SO → Delivery → Invoice) browser-layer E2E', () => {
  test('full chain: state flips + outgoing move + GL posting + AR item', async ({ page }) => {
    await loginAndNavigate(page, '/ErpSalOrder-main');

    const r = await runO2cChain(page);
    try {
      // ---- 库存出库移动产物：Delivery approve 触发 OUTGOING 移动（DONE + posted）----
      const moveTotal = await findPageTotal(
        page, 'ErpInvStockMove',
        andFilter(eqFilter('relatedBillType', 'ERP_SAL_DELIVERY'), eqFilter('relatedBillCode', r.codes.delivery)),
      );
      expect(moveTotal, 'Delivery approve should produce an ErpInvStockMove').toBeGreaterThan(0);
      expect(r.deliveryMove, 'delivery move should be found').toBeTruthy();
      expect(r.deliveryMove!.docStatus, 'business-linked move auto-completes to DONE').toBe('DONE');

      // ---- GL 过账产物：Invoice approve → posted=true + voucher 回链 + AR 辅助账 ----
      const invState = await findItems<any>(
        page, 'ErpSalInvoice', eqFilter('code', r.codes.invoice), 'id posted',
      );
      expect(invState[0]?.posted, 'Invoice approve should post (posted=true)').toBe(true);

      const vbrTotal = await findPageTotal(page, 'ErpFinVoucherBillR', eqFilter('billCode', r.codes.invoice));
      expect(vbrTotal, 'Invoice posting should write a voucher bill_r link').toBeGreaterThan(0);

      const arItems = await findItems<any>(
        page, 'ErpFinArApItem',
        andFilter(eqFilter('sourceBillType', 'AR_INVOICE'), eqFilter('sourceBillCode', r.codes.invoice)),
        'direction openAmountSource status',
      );
      expect(arItems.length, 'Invoice posting should create an AR (RECEIVABLE) auxiliary item').toBeGreaterThan(0);
      expect(arItems[0].direction, 'AR invoice → RECEIVABLE direction').toBe('RECEIVABLE');
      expect(Number(arItems[0].openAmountSource), 'AR openAmount = invoice totalAmountWithTax').toBe(O2C_EXPECT.invoiceWithTax);
      expect(arItems[0].status, 'newly posted AR item is OPEN').toBe('OPEN');
    } finally {
      await cleanupO2c(page, r);
    }
  });
});
