import { test, expect, loginAndNavigate, createViaSave, callMutationOk, verifyState, deleteById } from './_helper';
import { findVoucherIdByBillCode, assertVoucherLines, cleanupVoucherByBillCode } from '../orchestration/_helper';

/**
 * quality ErpQaNonConformance（NCR）SCRAP 过账凭证行精确数值断言浏览器层 E2E
 * （plan 2026-07-10-1800-1 Phase 2）。
 *
 * 验证 NCR resolve（SCRAP 处置，默认 AUTO_POST 配置）自动触发过账 → NCR_SCRAP 凭证行级
 * subjectCode + dcDirection + debitAmount/creditAmount 精确数值断言：
 *   Dr 6711 营业外支出-报废损失 = SCRAP_AMOUNT / Cr 1401 库存商品 = SCRAP_AMOUNT
 *
 * 过账路径（ErpQaNonConformanceBizModel.resolve:84 → dispatchFinancialImpact:195 →
 * NcrPostingDispatcher.dispatchScrap:63 → NcrScrapAcctDocProvider.createFacts:50）：
 *   resolve(IN_REVIEW→RESOLVED) → AUTO_POST → dispatchScrap → SCRAP_AMOUNT = qty × avgCost
 *   → postEvent(NCR_SCRAP) → NcrScrapAcctDocProvider: Dr 6711 / Cr 1401。
 *
 * 确定性裁决（avgCost 来源）：NcrPostingDispatcher.resolveStockBalance 按 materialId 查
 * ErpInvStockBalance(limit 1)。MAT_1(materialId=1) 在种子中有唯一余额行（warehouseId=1,
 * avgCost=120, MOVING_AVERAGE），故 resolveStockBalance(1) 确定性返回 avgCost=120。
 * SCRAP posting 不扣减物理库存量（NcrPostingDispatcher 注释：物理出库属 successor），
 * 故种子余额行不受过账影响。无需前置备货——备货会为 materialId=1 新增第二行余额导致
 * resolveStockBalance(limit 1) 返回行不确定（非确定性）。
 *
 * 清理：reverseNcr 红冲（验证红冲路径可达，红字凭证行断言归 Deferred successor）→
 * cleanupVoucherByBillCode 删 NORMAL+REVERSAL 凭证 → deleteById 删 NCR。
 * SCRAP posting 仅写 voucher/voucher_line/voucher_bill_r（不写 gl_balance），
 * 不污染 finance dashboard / 资产负债表 / 利润表数值基线。
 */

const MATERIAL_ID = 1; // MAT-001，种子余额 avgCost=120
const NCR_QTY = 1;
const SCRAP_AMOUNT = 120; // NCR_QTY(1) × seed avgCost(120) = 120

test.describe('quality ErpQaNonConformance SCRAP posting voucher line assertion', () => {
  test('resolve AUTO_POST → NCR_SCRAP voucher Dr 6711 / Cr 1401 exact amounts', async ({ page }) => {
    await loginAndNavigate(page, '/ErpQaNonConformance-main');

    // 建 NCR（SCRAP 处置，无 CAPA——空集放行 resolve 门控）
    const ncr = await createViaSave(
      page, 'ErpQaNonConformance',
      {
        code: `E2E-NCR-SCRAP-${Date.now()}`,
        ncrDate: '2026-07-10',
        materialId: MATERIAL_ID,
        severity: 'NORMAL',
        dispositionType: 'SCRAP',
        status: 'OPEN',
        quantity: NCR_QTY,
        description: 'E2E NCR SCRAP posting',
      },
      'id status code posted',
    );
    expect(ncr.status, 'new ncr status=OPEN').toBe('OPEN');

    try {
      // submitReview: OPEN → IN_REVIEW（resolve 前置态）
      const reviewed = await callMutationOk(
        page, 'ErpQaNonConformance', 'submitReview', { ncrId: ncr.id }, 'id status',
      );
      expect(reviewed.status, 'submitReview should transition OPEN → IN_REVIEW').toBe('IN_REVIEW');

      // resolve: IN_REVIEW → RESOLVED（无 CAPA 空集放行）→ AUTO_POST 自动触发 SCRAP 过账
      const resolved = await callMutationOk(
        page, 'ErpQaNonConformance', 'resolve',
        { ncrId: ncr.id, resolution: 'SCRAP disposed' }, 'id status posted',
      );
      expect(resolved.status, 'resolve should transition IN_REVIEW → RESOLVED').toBe('RESOLVED');
      expect(resolved.posted, 'AUTO_POST should set posted=true').toBe(true);

      // __get 权威确认 posted=true
      const ncrFinal = await verifyState(page, 'ErpQaNonConformance', ncr.id, 'status posted');
      expect(ncrFinal.status, '__get should confirm RESOLVED').toBe('RESOLVED');
      expect(ncrFinal.posted, '__get should confirm posted=true').toBe(true);

      // NCR_SCRAP 凭证行精确数值断言：Dr 6711 = SCRAP_AMOUNT / Cr 1401 = SCRAP_AMOUNT
      // 派生自 NcrScrapAcctDocProvider.createFacts: Dr 6711(LOSS)=SCRAP_AMOUNT / Cr 1401(INVENTORY)=SCRAP_AMOUNT
      const voucherId = await findVoucherIdByBillCode(page, ncr.code, 'NORMAL');
      await assertVoucherLines(page, voucherId, [
        { subjectCode: '6711', dcDirection: 'DEBIT', debitAmount: SCRAP_AMOUNT, creditAmount: 0 },
        { subjectCode: '1401', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: SCRAP_AMOUNT },
      ]);

      // 清理：reverseNcr 红冲（验证红冲路径可达，红字凭证行断言归 Deferred successor）
      const reversed = await callMutationOk(
        page, 'ErpQaNonConformance', 'reverseNcr', { ncrId: ncr.id }, 'id posted',
      );
      expect(reversed.posted, 'reverseNcr should clear posted=false').toBe(false);
    } finally {
      // cleanupVoucherByBillCode 删 NORMAL+REVERSAL 凭证（billHeadCode=ncr.code）
      await cleanupVoucherByBillCode(page, ncr.code);
      await deleteById(page, 'ErpQaNonConformance', ncr.id);
    }
  });
});
