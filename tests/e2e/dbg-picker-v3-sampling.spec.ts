// One-off picker v3 visual sampling (plan 2026-09-02-2028-1 Phase 5 抽样视觉验证).
// Not a committed baseline spec — sampling evidence for the plan closure.
// Drives the edit-form relation pickers (flux-control.xlib v3 tags) inside the
// edit drawer of the 5 business domains whose views were migrated to v3.
import { test, expect } from '@playwright/test';
import { loginAndNavigate } from './fixtures';

const PAGES = [
  '/ErpSalDelivery-main',
  '/ErpSalInvoice-main',
  '/ErpPurReceive-main',
  '/ErpPurInvoice-main',
  '/ErpFinVoucher-main',
];

test.describe('picker v3 visual sampling (edit-drawer relation pickers)', () => {
  for (const route of PAGES) {
    test(`${route}: drawer picker opens, CRUD loads, select + confirm writes back`, async ({ page }) => {
      test.setTimeout(120_000);
      await loginAndNavigate(page, route);
      await expect.poll(() => page.url(), { timeout: 20_000 }).toContain(route);
      const addBtn = page
        .locator('button:has-text("新建"), button:has-text("新增"), button:has-text("Add"), button:has-text("Create")')
        .first();
      await expect(addBtn).toBeVisible({ timeout: 30_000 });
      await addBtn.click();

      // The edit drawer is the topmost dialog portal; relation pickers live inside it.
      const drawer = page.locator('[data-slot="dialog-portal"]').last();
      const trigger = drawer.locator('[data-slot="picker-trigger"]').first();
      await expect(trigger).toBeVisible({ timeout: 30_000 });
      await trigger.evaluate((el) => (el as HTMLElement).click());

      // The picker's own popup is appended as a new portal on top of the drawer.
      const pickerDialog = page.locator('[data-slot="picker-dialog-content"]').last();
      await expect(pickerDialog).toBeVisible({ timeout: 20_000 });
      const rows = pickerDialog.locator('tbody tr');
      await expect(rows.first()).toBeVisible({ timeout: 20_000 });

      await pickerDialog.locator('[role="radio"], [role="checkbox"]').first().click();
      await page.locator('[data-slot="picker-confirm"]').last().click();
      await expect(pickerDialog).toBeHidden({ timeout: 20_000 });

      const label = drawer.locator('[data-testid="picker-selected-label"]').first();
      await expect(label).toBeVisible({ timeout: 20_000 });
      await expect(label).not.toHaveText(/^\s*(Not selected|未选择)\s*$/);
    });
  }
});
