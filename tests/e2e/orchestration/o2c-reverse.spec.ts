import {
  test,
  expect,
  loginAndNavigate,
  runO2cReverse,
  cleanupO2cReverse,
  findItems,
  eqFilter,
} from './_helper';

/**
 * 业财闭环方向二浏览器层 E2E — O2C 侧（plan 2026-07-09-2004-2 Phase 2）。
 *
 * 财务侧 DIRECT 红字冲销：经 `runO2cReverse` 驱动 O2C 正向链产 posted AR_INVOICE 凭证
 * → 调 `ErpFinVoucher__reverse(billHeadCode, businessType="AR_INVOICE")`
 * → 断言红字凭证生成 + 原凭证 isReversed=true + 销售域监听者回退（SalReversalListener.rollbackInvoice）。
 *
 * 断言三层（业财闭环方向二 §裁决4 回退目标态表，AR_INVOICE）：
 *   1. 原正常凭证：NORMAL + isReversed=true（markOriginalVoucherReversed）。
 *   2. 红字凭证：REVERSAL + isReversed=true + reversalOfVoucherId 指向原凭证。
 *   3. 域监听者回退：ErpSalInvoice posted=false + approveStatus APPROVED→REJECTED
 *      （VoucherReversedEvent → SalReversalListener.rollbackInvoice）。
 *
 * 入参/清理裁决同 P2P reverse（businessType 为 String scalar quoted；复用既有 cleanupO2c，无扩展）。
 */
test.describe('O2C reverse voucher (finance DIRECT red-letter reversal) browser-layer E2E', () => {
  test('reverse: red-letter voucher + original isReversed + SalInvoice rollback', async ({ page }) => {
    test.setTimeout(120000);
    await loginAndNavigate(page, '/ErpSalOrder-main');

    const r = await runO2cReverse(page);
    try {
      expect(r.reversalVoucherId, 'runO2cReverse should return reversal voucher id').toBeTruthy();
      expect(r.originalVoucherId, 'runO2cReverse should resolve original voucher id').toBeTruthy();

      // ---- 原正常凭证：NORMAL + isReversed=true ----
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

      // ---- 域监听者回退：ErpSalInvoice posted=false + approveStatus APPROVED→REJECTED ----
      const invoice = await findItems<any>(
        page, 'ErpSalInvoice', eqFilter('code', r.codes.invoice),
        'id posted approveStatus',
      );
      expect(invoice.length, 'sal invoice should exist').toBe(1);
      expect(invoice[0].posted, 'SalInvoice posted=false after reversal (SalReversalListener)').toBe(false);
      expect(invoice[0].approveStatus, 'SalInvoice approveStatus APPROVED→REJECTED').toBe('REJECTED');
    } finally {
      await cleanupO2cReverse(page, r);
    }
  });
});
