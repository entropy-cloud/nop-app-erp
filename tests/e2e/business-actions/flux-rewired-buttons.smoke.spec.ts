import { test, expect, loginAndNavigate, createViaSave, callMutationOk, verifyState, deleteById } from './_helper';

/**
 * flux 手写页按钮重接线冒烟（plan 2026-10-05-0400-1 Phase 3）。
 *
 * 背景：手写复杂页的 AMIS 风格按钮（actionType:ajax+api）在 flux 运行期惰性（无事件计划、
 * 点击零请求——服务端日志实锤，见 docs/architecture/flux-integration-gotchas.md G-002）。
 * 本 spec 以 stock-take-flow 页「开始盘点 (startTake)」为代表性被改按钮，断言重接线后的
 * 真实点击链路：click → confirm 对话框（env.confirm 宿主能力）→ /r/ 请求发出 → 服务端状态翻转。
 *
 * 接线契约：标量参数自定义 mutation 走 '/r/Entity__method'（壳层 nopRpcResolver 对未注册
 * @mutation: 回退 {data:...} 信封，仅适配 __save 类 data 参数——G-002 附带登记）。
 *
 * 自包含：建 DRAFT 盘点单 → 页面点击 startTake → 断言 CONFIRMED → cleanup（cancelTake + delete）。
 */
test.describe('flux rewired buttons smoke', () => {
  test('stock-take-flow startTake: click → confirm → /r/ request → docStatus CONFIRMED', async ({ page }) => {
    await loginAndNavigate(page, '/stock-take-flow');

    const code = `E2E-REWIRE-${Date.now()}`;
    const take = await createViaSave(
      page, 'ErpInvStockTake',
      { code, businessDate: '2026-10-05', warehouseId: '1', orgId: '2', docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED' },
      'id docStatus',
    );
    expect(take.id, 'DRAFT stock take should be created').toBeTruthy();

    // 选盘点单（actionForm.takeId select，绑定 stockTakeData 派生选项；batch-4 起为 select）
    // 数据源在建单前已加载——跨路由往返强制重挂载，使 select 选项包含新建单
    // （同路由 reload/goto 会触发 SPA 白屏怪癖，见 batch-3 执行记录）
    await page.goto('/#/erp-crm', { waitUntil: 'domcontentloaded' });
    await page.goto('/#/stock-take-flow', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(4_000);
    const takeSelect = page.getByRole('combobox', { name: /盘点单|Stock Take Order/ });
    await expect(takeSelect).toBeVisible({ timeout: 15_000 });
    // flux 自定义 combobox：点击展开 dialog 列表，按单号点选 option（value=id 由组件内部承载）
    await takeSelect.click();
    await page.getByRole('option', { name: new RegExp(code) }).first().click();
    await expect(takeSelect).toContainText(code, { timeout: 5_000 });

    // 点击重接线按钮 → 确认对话框 → /r/ 请求
    const respPromise = page.waitForResponse(
      (r) => r.url().includes('/r/ErpInvStockTake__startTake'),
      { timeout: 20_000 },
    );
    await page.getByRole('button', { name: /开始盘点/ }).click();
    // env.confirm 确认框（alertdialog「确认」+ 确认按钮）
    const confirmBtn = page.getByRole('button', { name: '确认', exact: true });
    await expect(confirmBtn).toBeVisible({ timeout: 10_000 });
    await confirmBtn.click();

    const resp = await respPromise;
    expect(resp.ok(), 'startTake /r/ request should succeed').toBeTruthy();

    // 服务端状态翻转断言（DRAFT → CONFIRMED，盘点中持久化值）
    const st = await verifyState(page, 'ErpInvStockTake', take.id, 'docStatus');
    expect(st.docStatus, 'startTake should flip docStatus to CONFIRMED').toBe('CONFIRMED');

    // cleanup：cancelTake → delete（逻辑删）
    await callMutationOk(page, 'ErpInvStockTake', 'cancelTake', { takeId: String(take.id) }, 'id docStatus');
    await deleteById(page, 'ErpInvStockTake', take.id);
  });
});
