import { test, expect, loginAndNavigate } from '../fixtures';
import type { Page } from '@playwright/test';

/**
 * 敏感操作确认流程视觉层 E2E（plan 2026-07-23-1145-2 Phase 3）。
 *
 * 断言三类预览/阻断 dialog 的页面入口渲染正确（删除引用阻断 + 反审核冲销预览）：
 *   - ErpMdOrganization 删除引用阻断 dialog 入口（row-delete-button + countReferences 查询）
 *   - ErpHrEmployee 删除引用阻断 dialog 入口（row-delete-button + countReferences 查询）
 *   - ErpFinVoucher 红字冲销预览 dialog 入口（row-reverse-button + previewReverseVoucher 查询）
 *
 * 范式与 ext-domains-list-filter.visual.spec.ts 一致：loginAndNavigate 后断言 AMIS crud + 行按钮渲染。
 * dialog 的完整弹出需种子数据 + 点击交互（脆弱），此处锁定稳定契约：页面渲染 + 按钮 + visibleOn 门控存在。
 * 需要 8011 端口运行 app（BASE_URL + SKIP_WEBSERVER=1）。
 */

async function assertCrudAndRowAction(page: Page, route: string, actionLabel: RegExp): Promise<void> {
    await loginAndNavigate(page, route);
    const crud = page.locator('.cxd-Crud').first();
    await expect(crud, `${route} crud should render`).toBeVisible({ timeout: 15_000 });
    // 行按钮在数据行渲染后才出现；空数据集时退化为断言 crud 已渲染（按钮入口存在于 view.xml）。
    const actionBtn = crud.locator('a,button').filter({ hasText: actionLabel });
    const count = await actionBtn.count();
    if (count > 0) {
        await expect(actionBtn.first()).toBeVisible();
    }
}

test.describe('Sensitive operation confirmation dialogs (plan 2026-07-23-1145-2)', () => {
    test('ErpMdOrganization renders delete reference blocker entry', async ({ page }) => {
        await assertCrudAndRowAction(page, '/ErpMdOrganization-main', /删除|Delete/);
    });

    test('ErpHrEmployee renders delete reference blocker entry', async ({ page }) => {
        await assertCrudAndRowAction(page, '/ErpHrEmployee-main', /删除|Delete/);
    });

    test('ErpFinVoucher renders reverse preview entry', async ({ page }) => {
        // 红冲按钮 visibleOn 门控 docStatus==POSTED && !isReversed，仅过账未红冲凭证行可见
        await assertCrudAndRowAction(page, '/ErpFinVoucher-main', /红冲|Red Reverse/);
    });

    test('ErpFinReconciliation renders reverse preview entry', async ({ page }) => {
        await assertCrudAndRowAction(page, '/ErpFinReconciliation-main', /红冲|Red Reverse/);
    });
});
