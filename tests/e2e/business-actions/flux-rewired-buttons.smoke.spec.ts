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

    // 填盘点单 ID（actionForm.takeId input-number）
    const takeIdInput = page.getByRole('spinbutton', { name: /盘点单ID|Stock Take Order ID/ });
    await expect(takeIdInput).toBeVisible({ timeout: 15_000 });
    await takeIdInput.fill(String(take.id));

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
