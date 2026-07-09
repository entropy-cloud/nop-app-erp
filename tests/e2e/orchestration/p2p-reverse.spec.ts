import {
  test,
  expect,
  loginAndNavigate,
  runP2pReverse,
  cleanupP2pReverse,
  findItems,
  eqFilter,
} from './_helper';

/**
 * 业财闭环方向二浏览器层 E2E（plan 2026-07-09-2004-2 Phase 1）。
 *
 * 财务侧 DIRECT 红字冲销：经 `runP2pReverse` 驱动 P2P 正向链产 posted AP_INVOICE 凭证
 * → 调 `ErpFinVoucher__reverse(billHeadCode, businessType="AP_INVOICE")`
 * → 断言红字凭证生成 + 原凭证 isReversed=true + 采购域监听者回退（PurReversalListener.rollbackInvoice）。
 *
 * 断言三层（业财闭环方向二 §裁决4 回退目标态表）：
 *   1. 原正常凭证：NORMAL + isReversed=true（reverseProcess.markOriginalVoucherReversed）。
 *   2. 红字凭证：REVERSAL + isReversed=true + reversalOfVoucherId 指向原凭证。
 *   3. 域监听者回退：ErpPurInvoice posted=false + approveStatus APPROVED→REJECTED
 *      （VoucherReversedEvent → PurReversalListener.rollbackInvoice）。
 *
 * 入参裁决：`businessType` 经 Nop GraphQL 暴露为 String scalar——quoted `"AP_INVOICE"` 接受
 * （unquoted enum 名被 `非法的字符` 拒绝）。helper 内联 JSON.stringify 自动产生 quoted 字面量。
 *
 * 清理：复用既有 cleanupP2p（red-letter voucher 与原凭证共用 billCode，既有
 * cleanupVoucherByBillCode 已覆盖；AR-AP 既有行 status→CANCELLED 非新增，既有 cleanupArApByCode 已覆盖）。
 */
test.describe('P2P reverse voucher (finance DIRECT red-letter reversal) browser-layer E2E', () => {
  test('reverse: red-letter voucher + original isReversed + PurInvoice rollback', async ({ page }) => {
    test.setTimeout(120000);
    await loginAndNavigate(page, '/ErpPurOrder-main');

    const r = await runP2pReverse(page);
    try {
      expect(r.reversalVoucherId, 'runP2pReverse should return reversal voucher id').toBeTruthy();
      expect(r.originalVoucherId, 'runP2pReverse should resolve original voucher id').toBeTruthy();

      // ---- 原正常凭证：NORMAL + isReversed=true（markOriginalVoucherReversed）----
      const original = await findItems<any>(
        page, 'ErpFinVoucher', eqFilter('id', r.originalVoucherId),
        'id postingType isReversed reversalOfVoucherId',
      );
      expect(original.length, 'original voucher should exist').toBe(1);
      expect(original[0].postingType, 'original voucher postingType=NORMAL').toBe('NORMAL');
      expect(original[0].isReversed, 'original voucher isReversed=true after reverse').toBe(true);

      // ---- 红字凭证：REVERSAL + isReversed=true + reversalOfVoucherId→原凭证 ----
      const reversal = await findItems<any>(
        page, 'ErpFinVoucher', eqFilter('id', r.reversalVoucherId),
        'id postingType isReversed reversalOfVoucherId',
      );
      expect(reversal.length, 'reversal voucher should exist').toBe(1);
      expect(reversal[0].postingType, 'reversal voucher postingType=REVERSAL').toBe('REVERSAL');
      expect(reversal[0].isReversed, 'reversal voucher isReversed=true').toBe(true);
      expect(Number(reversal[0].reversalOfVoucherId), 'reversal reversalOfVoucherId→original')
        .toBe(r.originalVoucherId);

      // ---- 域监听者回退：ErpPurInvoice posted=false + approveStatus APPROVED→REJECTED ----
      const invoice = await findItems<any>(
        page, 'ErpPurInvoice', eqFilter('code', r.codes.invoice),
        'id posted approveStatus',
      );
      expect(invoice.length, 'pur invoice should exist').toBe(1);
      expect(invoice[0].posted, 'PurInvoice posted=false after reversal (PurReversalListener)').toBe(false);
      expect(invoice[0].approveStatus, 'PurInvoice approveStatus APPROVED→REJECTED').toBe('REJECTED');
    } finally {
      await cleanupP2pReverse(page, r);
    }
  });
});
