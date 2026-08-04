import { test } from '@playwright/test';
import { loginAndNavigate, getEngine } from './pages';
import { CrudListPage } from './pages/CrudListPage';

test('probe purchase add-button click behavior', async ({ page }) => {
  const consoleErrors: string[] = [];
  const reqs: string[] = [];
  page.on('console', (m) => {
    if (m.type() === 'error' || m.type() === 'warning') consoleErrors.push(m.text().slice(0, 250));
  });
  page.on('request', (r) => {
    if (!r.url().includes('.js') && !r.url().includes('.css')) reqs.push(r.method() + ' ' + r.url().split('?')[0]);
  });
  await loginAndNavigate(page, '/ErpPurOrder-main');
  const engine = getEngine();
  const crud = new CrudListPage(page, engine, { entityRoute: 'ErpPurOrder', domain: 'purchase' });
  await crud.waitForList();
  const addBtn = await crud.getAddButton();
  console.log('BTN_VISIBLE=' + (await addBtn.isVisible()));
  console.log('BTN_ENABLED=' + (await addBtn.isEnabled().catch(() => 'n/a')));
  console.log('BTN_COUNT=' + (await addBtn.count()));
  await addBtn.click();
  await page.waitForTimeout(1500);
  console.log('AFTER_CLICK_DIALOGS=' + (await engine.dialog(page).count()));
  console.log('AFTER_CLICK_REQS=' + JSON.stringify(reqs));
  console.log('CONSOLE=' + JSON.stringify(consoleErrors.slice(0, 8)));
  await addBtn.click({ force: true });
  await page.waitForTimeout(1500);
  console.log('AFTER_FORCE_DIALOGS=' + (await engine.dialog(page).count()));
});
