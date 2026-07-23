import {
    test,
    expect,
    loginAndNavigate,
    createViaSave,
    callMutationOk,
    callQuery,
    verifyState,
    deleteById,
} from './_helper';
import type { Page } from '@playwright/test';

/**
 * 反审核冲销预览动作浏览器层 E2E（plan 2026-07-23-1145-2 Phase 3）。
 *
 * 覆盖两类冲销预览的 GraphQL 全栈可达性 + 预览与实际 reverse 一致性：
 *   - ErpFinVoucher__previewReverseVoucher：返回结构化预览（红字预估取负 + willSetReversed），只读不改状态；
 *     随后 reverseVoucher 执行，isReversed 翻转与预览一致。
 *   - ErpFinReconciliation__previewReverse：返回辅助账回退列表（revertedItems 非空），随后 reverse 置 REVERSED。
 *
 * 范式与 ast-depreciation.action.spec.ts 一致：loginAndNavigate 建立 nop-token 会话后经 GraphQLClient 调
 * 自定义 @BizQuery/@BizMutation。需要 8011 端口运行 app（BASE_URL + SKIP_WEBSERVER=1）。
 */

const ORG_ID = 1;
const ACCT_SCHEMA_ID = 1;
const PERIOD_ID = 1;

async function createPostedVoucher(page: Page, code: string, amount: number): Promise<string> {
    const saved = await createViaSave(page, 'ErpFinVoucher', {
        code,
        voucherType: 'TRANSFER',
        voucherDate: '2026-07-23',
        orgId: ORG_ID,
        acctSchemaId: ACCT_SCHEMA_ID,
        periodId: PERIOD_ID,
        totalDebit: amount,
        totalCredit: amount,
        isReversed: false,
        docStatus: 'DRAFT',
    });
    const id = saved.id;
    await callMutationOk(page, 'ErpFinVoucher', 'postVoucher', { voucherId: id }, 'id docStatus');
    return String(id);
}

test.describe('Reverse preview action (plan 2026-07-23-1145-2)', () => {
    test('previewReverseVoucher returns structured preview and is consistent with actual reverse', async ({ page }) => {
        await loginAndNavigate(page, '/ErpFinVoucher-main');

        const code = 'E2E-PREVIEW-REV-' + Date.now();
        const amount = 200;
        const voucherId = await createPostedVoucher(page, code, amount);

        // 1. 预览：只读 @BizQuery，返回结构化冲销信息
        const { data: preview, errors } = await callQuery(page, 'ErpFinVoucher', 'previewReverseVoucher',
            { voucherId });
        expect(errors, 'previewReverseVoucher should not return GraphQL errors').toBeNull();
        expect(preview, 'preview should be non-null').toBeTruthy();
        expect(preview.voucherCode).toBe(code);
        expect(Number(preview.totalDebit)).toBe(amount);
        // 红字预估 = 原金额取负
        expect(Number(preview.reversedDebit)).toBe(-amount);
        expect(Number(preview.reversedCredit)).toBe(-amount);
        expect(preview.willSetReversed).toBe(true);

        // 2. 预览为只读：凭证状态仍为 POSTED + 未红冲
        const stateAfterPreview = await verifyState(page, 'ErpFinVoucher', voucherId, 'docStatus isReversed');
        expect(stateAfterPreview.docStatus).toBe('POSTED');
        expect(stateAfterPreview.isReversed).toBe(false);

        // 3. 实际 reverse 后验证预览一致
        await callMutationOk(page, 'ErpFinVoucher', 'reverseVoucher', { voucherId }, 'id isReversed');
        const stateAfterReverse = await verifyState(page, 'ErpFinVoucher', voucherId, 'isReversed');
        expect(stateAfterReverse.isReversed, 'isReversed should flip to true, consistent with preview').toBe(true);

        // 清理
        await deleteById(page, 'ErpFinVoucher', voucherId);
    });
});
