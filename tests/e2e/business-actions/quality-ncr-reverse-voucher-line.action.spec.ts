import { test, expect, loginAndNavigate, createViaSave, callMutationOk, verifyState, deleteById } from './_helper';
import {
  findVoucherIdByBillCode,
  assertVoucherLines,
  cleanupVoucherByBillCode,
  findFirst,
  eqFilter,
} from '../orchestration/_helper';

/**
 * quality ErpQaNonConformance（NCR）reverseNcr 红字凭证行级断言浏览器层 E2E
 * （plan 2026-07-11-0730-2 Phase 1）。
 *
 * 验证 reverseNcr 经财务过账引擎 reverse 生成红字凭证（REVERSAL）的行级正确性：
 *   - 红字凭证行同向取负（buildReversalDraft 保持 dcDirection 不变、金额取负）：
 *     Dr 6711=-SCRAP_AMOUNT / Cr 1401=-SCRAP_AMOUNT
 *   - 原正常凭证被引擎公共流程 markOriginalVoucherReversed 补标 isReversed=true（O-8 统一行为）
 *
 * 过账路径（ErpQaNonConformanceBizModel.reverseNcr:124 → NcrPostingDispatcher.reverseScrap:92 →
 * NcrPostingExecutor.reverse:38 → IErpFinVoucherBiz.reverse）：
 *   reverseNcr（posted=true 前置）→ reverseScrap → executor.reverse(ncrCode, NCR_SCRAP)
 *   → voucherBiz.reverse 生成红字凭证 + markOriginalVoucherReversed。
 *
 * 价值增量（区别于 1800-1 quality-ncr-scrap-posting）：1800-1 仅断言 reverseNcr 后 ncr.posted=false
 * （实体级回退），本 spec 新增①红字凭证行级金额取负断言 ②原正向凭证 isReversed=true（凭证级回退标记）。
 *
 * 确定性裁决（avgCost）：MAT_1(materialId=1) 种子唯一余额行 avgCost=120（1800-1 基线），
 * 故 SCRAP_AMOUNT = NCR_QTY(1) × 120 = 120，红字凭证行金额 = -120。
 *
 * 清理：cleanupVoucherByBillCode 删 NORMAL+REVERSAL 凭证 → deleteById 删 NCR。
 */

const MATERIAL_ID = 1; // MAT-001，种子余额 avgCost=120
const NCR_QTY = 1;
const SCRAP_AMOUNT = 120; // NCR_QTY(1) × seed avgCost(120) = 120

test.describe('quality ErpQaNonConformance reverseNcr red-character voucher line assertion', () => {
  test('reverseNcr → reversal voucher lines negated + original voucher isReversed=true', async ({ page }) => {
    await loginAndNavigate(page, '/ErpQaNonConformance-main');

    const ncr = await createViaSave(
      page, 'ErpQaNonConformance',
      {
        code: `E2E-NCR-REV-${Date.now()}`,
        ncrDate: '2026-07-11',
        materialId: MATERIAL_ID,
        severity: 'NORMAL',
        dispositionType: 'SCRAP',
        status: 'OPEN',
        quantity: NCR_QTY,
        description: 'E2E NCR reverse voucher line',
      },
      'id status code posted',
    );
    expect(ncr.status, 'new ncr status=OPEN').toBe('OPEN');

    try {
      await callMutationOk(
        page, 'ErpQaNonConformance', 'submitReview', { ncrId: ncr.id }, 'id status',
      );

      const resolved = await callMutationOk(
        page, 'ErpQaNonConformance', 'resolve',
        { ncrId: ncr.id, resolution: 'SCRAP disposed' }, 'id status posted',
      );
      expect(resolved.status, 'resolve should transition IN_REVIEW → RESOLVED').toBe('RESOLVED');
      expect(resolved.posted, 'AUTO_POST should set posted=true').toBe(true);

      const reversed = await callMutationOk(
        page, 'ErpQaNonConformance', 'reverseNcr', { ncrId: ncr.id }, 'id posted',
      );
      expect(reversed.posted, 'reverseNcr should clear posted=false').toBe(false);

      const ncrFinal = await verifyState(page, 'ErpQaNonConformance', ncr.id, 'posted');
      expect(ncrFinal.posted, '__get should confirm posted=false after reverse').toBe(false);

      const reversalVoucherId = await findVoucherIdByBillCode(page, ncr.code, 'REVERSAL');
      expect(reversalVoucherId, 'REVERSAL voucher should exist for ncr.code').toBeTruthy();

      await assertVoucherLines(page, reversalVoucherId, [
        { subjectCode: '6711', dcDirection: 'DEBIT', debitAmount: -SCRAP_AMOUNT, creditAmount: 0 },
        { subjectCode: '1401', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -SCRAP_AMOUNT },
      ]);

      const originalVoucherId = await findVoucherIdByBillCode(page, ncr.code, 'NORMAL');
      expect(originalVoucherId, 'NORMAL voucher should exist for ncr.code').toBeTruthy();

      const origVoucher = await findFirst<any>(
        page, 'ErpFinVoucher', eqFilter('id', Number(originalVoucherId)),
        'id postingType isReversed',
      );
      expect(origVoucher?.postingType, 'original voucher postingType=NORMAL').toBe('NORMAL');
      expect(origVoucher?.isReversed, 'original voucher isReversed=true after reverseNcr (O-8 markOriginalVoucherReversed)').toBe(true);

      await assertVoucherLines(page, originalVoucherId, [
        { subjectCode: '6711', dcDirection: 'DEBIT', debitAmount: SCRAP_AMOUNT, creditAmount: 0 },
        { subjectCode: '1401', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: SCRAP_AMOUNT },
      ]);
    } finally {
      await cleanupVoucherByBillCode(page, ncr.code);
      await deleteById(page, 'ErpQaNonConformance', ncr.id);
    }
  });
});
