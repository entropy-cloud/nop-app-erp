import { test, expect } from './fixtures';
import { loginAndNavigate, getEngine, getEngineType } from './pages';

test('diag: cs ErpCsTicket add-form surface', async ({ page }) => {
  test.skip(getEngineType() !== 'flux', 'flux only');
  await loginAndNavigate(page, '/ErpCsTicket-main');
  await page.waitForTimeout(2500);
  const addBtn = page
    .locator('[data-slot="crud-toolbar-main"] button')
    .filter({ hasText: /新增|Add|添加/ })
    .first();
  const btnCount = await addBtn.count();
  const btnText = btnCount > 0 ? await addBtn.textContent() : '(none)';
  if (btnCount > 0) await addBtn.click({ force: true });
  await page.waitForTimeout(4000);
  const surfaces = await page.evaluate(
    ([bCount, bText]) => {
      const slots: Record<string, number> = {};
      document.querySelectorAll('[data-slot]').forEach((el) => {
        const s = el.getAttribute('data-slot') || '';
        slots[s] = (slots[s] || 0) + 1;
      });
      const fluxDebug = (window as any).__fluxDebug || [];
      const errors = fluxDebug.filter(
        (e: any) => e && (e.phase === 'error' || (e.notify && e.notify.level === 'error')),
      );
      return {
        url: location.hash,
        addButtonFound: bCount > 0,
        addButtonFoundText: bText,
        slotCounts: slots,
        bodyLen: document.body.innerHTML.length,
        surfaceVisible: {
          'dialog-surface': !!document.querySelector('[data-slot="dialog-surface"]'),
          'dialog-content': !!document.querySelector('[data-slot="dialog-content"]'),
          'drawer-surface': !!document.querySelector('[data-slot="drawer-surface"]'),
          'drawer-content': !!document.querySelector('[data-slot="drawer-content"]'),
        },
        anyRoleDialog: !!document.querySelector('[role="dialog"]'),
        fluxErrorCount: errors.length,
        fluxErrorSamples: errors.slice(0, 3).map((e: any) =>
          e.notify ? e.notify.message : e.error || e.message || String(e).slice(0, 200),
        ),
      };
    },
    [btnCount, btnText],
  );
  console.log('CS_DIAG_RESULT=' + JSON.stringify(surfaces));
  expect(true).toBe(true);
});
