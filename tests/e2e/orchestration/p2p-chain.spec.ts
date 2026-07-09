import {
  test,
  expect,
  loginAndNavigate,
  runP2pChain,
  cleanupP2p,
  findPageTotal,
  findItems,
  eqFilter,
  andFilter,
  P2P_EXPECT,
  SEED,
} from './_helper';

/**
 * P2P（Procure-to-Pay）核心链路浏览器层 E2E（plan 2026-07-09-1249-1 Phase 1）。
 *
 * 经 GraphQL /graphql 驱动全链：PO __save → submitForApproval → approve
 *   → Receive __save(orderId) → submitForApproval → approve（触发入库移动 + posted）
 *   → Invoice __save(receiveLineId) → submitForApproval → approve（触发 GL 过账 posted=true + AP 辅助账）。
 *
 * 断言三层：
 *   1. 状态流转——PO/Receive/Invoice approveStatus UNSUBMITTED→SUBMITTED→APPROVED（经 __get 独立验证，见 helper）。
 *   2. 库存移动产物——Receive approve 后 ErpInvStockMove(relatedBillCode=receive.code) 存在 + DONE。
 *   3. GL 过账产物——Invoice approve 后 posted=true + voucher_bill_r(billCode=invoice.code) 回链 + AP 辅助账项
 *      （sourceBillType=AP_INVOICE, direction=PAYABLE, openAmountSource=含税总额 56.5）。
 *
 * 清理：链路创建不可逆下游产物（库存流水/余额、GL 凭证/行/回链、AP 辅助账），全栈共享同一 H2 实例，
 * 不清理会污染下游数值断言基线（inventory dashboard totalValue / ar-ap-aging）。finally 逐域逻辑删除。
 *
 * 前置：种子 COA 已补齐过账科目码（1403/2221/2202），发票过账 happy-path 可达（see plan + e2e-runbook）。
 */

test.describe('P2P orchestration chain (PO → Receive → Invoice) browser-layer E2E', () => {
  test('full chain: state flips + stock move + GL posting + AP item', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPurOrder-main');

    const r = await runP2pChain(page);
    try {
      // ---- 库存移动产物：Receive approve 触发 INCOMING 移动（DONE + posted）----
      const moveTotal = await findPageTotal(
        page, 'ErpInvStockMove',
        andFilter(eqFilter('relatedBillType', 'ERP_PUR_RECEIVE'), eqFilter('relatedBillCode', r.codes.receive)),
      );
      expect(moveTotal, 'Receive approve should produce an ErpInvStockMove').toBeGreaterThan(0);
      expect(r.receiveMove, 'receive move should be found').toBeTruthy();
      expect(r.receiveMove!.docStatus, 'business-linked move auto-completes to DONE').toBe('DONE');

      // ---- GL 过账产物：Invoice approve → posted=true + voucher 回链 + AP 辅助账 ----
      const invState = await findItems<any>(
        page, 'ErpPurInvoice', eqFilter('code', r.codes.invoice), 'id posted',
      );
      expect(invState[0]?.posted, 'Invoice approve should post (posted=true)').toBe(true);

      const vbrTotal = await findPageTotal(page, 'ErpFinVoucherBillR', eqFilter('billCode', r.codes.invoice));
      expect(vbrTotal, 'Invoice posting should write a voucher bill_r link').toBeGreaterThan(0);

      const apItems = await findItems<any>(
        page, 'ErpFinArApItem',
        andFilter(eqFilter('sourceBillType', 'AP_INVOICE'), eqFilter('sourceBillCode', r.codes.invoice)),
        'direction openAmountSource status',
      );
      expect(apItems.length, 'Invoice posting should create an AP (PAYABLE) auxiliary item').toBeGreaterThan(0);
      expect(apItems[0].direction, 'AP invoice → PAYABLE direction').toBe('PAYABLE');
      expect(Number(apItems[0].openAmountSource), 'AP openAmount = invoice totalAmountWithTax').toBe(P2P_EXPECT.invoiceWithTax);
      expect(apItems[0].status, 'newly posted AP item is OPEN').toBe('OPEN');
    } finally {
      await cleanupP2p(page, r);
    }
  });
});

// 显式标注种子引用（供 lint/可读性；SEED 复用避免硬编码漂移）
void SEED;
