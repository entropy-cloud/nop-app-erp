import {
  test,
  expect,
  loginAndNavigate,
  runP2pChain,
  cleanupP2p,
  callMutationOk,
  verifyState,
  findFirst,
  findVoucherIdByBillCode,
  eqFilter,
} from './_helper';

/**
 * 业财闭环：域侧 DIRECT 审批逆向浏览器层 E2E（plan 2026-07-11-1234-1 Phase 2）。
 *
 * 验证 `ErpPurInvoice__reverseApprove(id)` 域侧审批逆向动作（domain-initiated reversal path）
 * 经 GraphQL /graphql 的全栈可达性。与 p2p-reverse.spec.ts（finance-initiated
 * `ErpFinVoucher__reverse` → 域监听者回退）互补——入口不同但回退目标态一致。
 *
 * 权威实现（ErpPurInvoiceProcessor.reverseApprove:90）：
 *   1. 校验 approveStatus=APPROVED（非法态抛 ERR_INVOICE_ILLEGAL_STATUS_TRANSITION）
 *   2. 若 posted=true → postingDispatcher.reverse(invoice)（内部调 ErpFinVoucher__reverse 红冲 AP 凭证）
 *      → reload → posted=false + postedAt=null + postedBy=null
 *   3. doReverseApprove → approveStatus=REJECTED + approvedBy=null + approvedAt=null
 *
 * 断言三层：
 *   1. 域状态回退：approveStatus APPROVED→REJECTED + posted true→false + postedAt/postedBy null
 *   2. 红字凭证生成：REVERSAL 类型凭证存在（经 voucher_bill_r 回链反查）
 *   3. 原凭证标记已冲销：NORMAL 类型凭证 isReversed=true
 *
 * 清理：复用既有 cleanupP2p（红字凭证与原凭证共用 invoice code，既有
 * cleanupVoucherByBillCode 已覆盖；AR-AP 既有行取消非新增）。
 */

test.describe('P2P reverseApprove (domain DIRECT approval reversal) browser-layer E2E', () => {
  test('reverseApprove: posted Invoice → REJECTED + posted=false + red-letter voucher + original isReversed', async ({ page }) => {
    test.setTimeout(120000);
    await loginAndNavigate(page, '/ErpPurOrder-main');

    const r = await runP2pChain(page);
    try {
      // ---- 前置态验证：Invoice 为 APPROVED + posted ----
      let inv = await verifyState(page, 'ErpPurInvoice', r.invoice.id, 'approveStatus posted');
      expect(inv.approveStatus, 'pre: approveStatus=APPROVED').toBe('APPROVED');
      expect(inv.posted, 'pre: posted=true (approve triggers posting)').toBe(true);

      // ---- reverseApprove（域侧 DIRECT 审批逆向）----
      await callMutationOk(page, 'ErpPurInvoice', 'reverseApprove', { id: r.invoice.id }, 'id');

      // ---- 断言 1：域状态回退 ----
      inv = await verifyState(page, 'ErpPurInvoice', r.invoice.id, 'approveStatus posted postedAt postedBy');
      expect(inv.approveStatus, 'after reverseApprove: approveStatus=REJECTED').toBe('REJECTED');
      expect(inv.posted, 'after reverseApprove: posted=false').toBe(false);
      expect(inv.postedAt, 'after reverseApprove: postedAt=null').toBeNull();
      expect(inv.postedBy, 'after reverseApprove: postedBy=null').toBeNull();

      // ---- 断言 2：红字凭证生成 ----
      const reversalVoucherId = await findVoucherIdByBillCode(page, r.codes.invoice, 'REVERSAL');
      expect(reversalVoucherId, 'red-letter (REVERSAL) voucher should exist').toBeTruthy();

      // ---- 断言 3：原正常凭证 isReversed=true ----
      const originalVoucherId = await findVoucherIdByBillCode(page, r.codes.invoice, 'NORMAL');
      expect(originalVoucherId, 'original NORMAL voucher should exist').toBeTruthy();
      const origVoucher = await findFirst<any>(
        page, 'ErpFinVoucher', eqFilter('id', originalVoucherId), 'id postingType isReversed',
      );
      expect(origVoucher?.postingType, 'original voucher postingType=NORMAL').toBe('NORMAL');
      expect(origVoucher?.isReversed, 'original voucher isReversed=true after reverseApprove').toBe(true);
    } finally {
      await cleanupP2p(page, r);
    }
  });
});
