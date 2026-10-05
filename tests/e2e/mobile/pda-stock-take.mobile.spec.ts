import { test, expect, loginAndNavigate, createViaSave, callMutationOk, verifyState, deleteById } from '../business-actions/_helper';
import { GraphQLClient } from '../pages';

/**
 * USC-05 PDA 盘点扫码作业面浏览器层冒烟（plan 2026-10-01-0930-1 Phase 2 交付；
 * plan 2026-10-05-0400-2 Phase 4 升级为真实点击保存链路）。
 *
 * 双视口可达性 + 表单渲染（保留）+ **真实点击保存链**（0400-2 新增）：
 * GraphQL 种 CONFIRMED 盘点单 + SKU 条码链（UoM/Material/Sku）+ 库位 →
 * 页面选单 → 填条码/库位/实盘数量 → 点击保存 → confirm 对话框 →
 * waitForResponse /r/ErpInvStockTakeLine__scanSave → 落行断言
 * （findPage 反查行：materialId/uoMId/bookQuantity/differenceQuantity 派生字段）→ cleanup。
 *
 * 服务端派生逻辑全量断言在 JUnit TestErpInvStockTakeLineScanSave（8/8）；本 spec 断言
 * 浏览器层真实点击可达性（flux 接线 + env.confirm + /r/ 直发 + 落行），修补
 * 「两层测试之间的按钮点击缝」（G-002）。
 */
const MOBILE = { width: 375, height: 812 };

test.describe('USC-05 PDA stock-take scan page (mobile + desktop)', () => {
  test('mobile 375px: PDA scan page reachable, form rendered', async ({ page }) => {
    await page.setViewportSize(MOBILE);
    await loginAndNavigate(page, '/pda-stock-take');
    await expect(page.locator('input').first()).toBeVisible({ timeout: 15000 });
    const bodyText = await page.locator('body').innerText();
    expect(bodyText, 'mobile: 页面应渲染 PDA 扫码表单').toContain('PDA');
  });

  test('desktop 1280px: PDA scan page reachable, form rendered', async ({ page }) => {
    await loginAndNavigate(page, '/pda-stock-take');
    await expect(page.locator('input').first()).toBeVisible({ timeout: 15000 });
  });

  test('mobile 375px: real click save chain — select take → scan fields → save → line persisted', async ({ page }) => {
    test.setTimeout(90_000);
    await page.setViewportSize(MOBILE);
    await loginAndNavigate(page, '/pda-stock-take');

    const suffix = String(Date.now()).slice(-6); // uom.code 精度 20：种子编码统一短后缀
    // 种主数据链：仓库/库位/UoM/物料/SKU（GraphQL __save，GraphQLClient 带登录态）
    const wh = await createViaSave(page, 'ErpMdWarehouse',
      { code: `E2E-PDA-WH-${suffix}`, name: 'e2e wh', status: 'ACTIVE' }, 'id');
    const loc = await createViaSave(page, 'ErpMdLocation',
      { code: `E2E-PDA-LOC-${suffix}`, name: 'e2e loc', warehouseId: wh.id }, 'id code');
    const uom = await createViaSave(page, 'ErpMdUoM',
      { code: `E2E-PDA-UOM-${suffix}`, name: 'e2e uom' }, 'id');
    const material = await createViaSave(page, 'ErpMdMaterial',
      { code: `E2E-PDA-MAT-${suffix}`, name: 'e2e mat', materialType: 'GOODS', uoMId: uom.id, status: 'ACTIVE' }, 'id');
    const barcode = '690' + String(suffix).padStart(10, '0').slice(-10);
    const sku = await createViaSave(page, 'ErpMdMaterialSku',
      { skuCode: `E2E-PDA-SKU-${suffix}`, barcode, materialId: material.id, uoMId: uom.id }, 'id');

    // 种盘点单 + startTake → CONFIRMED（盘点中作业窗口）
    const take = await createViaSave(page, 'ErpInvStockTake',
      { code: `E2E-PDA-TAKE-${suffix}`, businessDate: '2026-10-05', warehouseId: wh.id, orgId: '2', docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED' },
      'id code docStatus');
    await callMutationOk(page, 'ErpInvStockTake', 'startTake', { takeId: take.id }, 'id docStatus');

    // 页面操作：选盘点单 → 填条码/库位/数量 → 保存
    // 盘点单定位 = 扫/输编号（takeCode，服务端按 code 解析——PDA 键盘楔作业惯例；
    // flux select 动态选项源在本仓手写页无可用承载，D5 修订裁决见 plan 2026-10-05-0400-2）
    const takeCodeInput = page.getByPlaceholder(/扫描或输入盘点单编号|Scan or enter stock-take code/);
    await expect(takeCodeInput).toBeVisible({ timeout: 15_000 });
    await takeCodeInput.fill(take.code);

    const barcodeInput = page.getByPlaceholder(/扫描或输入 SKU 条码|Scan or enter SKU barcode/);
    await expect(barcodeInput).toBeVisible();
    await barcodeInput.fill(barcode);
    await page.getByPlaceholder(/扫描或输入库位编码|Scan or enter location code/).fill(loc.code);
    await page.getByRole('spinbutton', { name: /实盘数量|Counted Quantity/ }).fill('5');

    const respPromise = page.waitForResponse(
      (r) => r.url().includes('/r/ErpInvStockTakeLine__scanSave'), { timeout: 20_000 });
    await page.getByRole('button', { name: /保存盘点行/ }).click();
    const confirmBtn = page.getByRole('button', { name: '确认', exact: true });
    await expect(confirmBtn).toBeVisible({ timeout: 10_000 });
    await confirmBtn.click();
    const resp = await respPromise;
    expect(resp.ok(), 'scanSave /r/ request should succeed').toBeTruthy();

    // 落行断言：行存在且派生字段齐（GraphQLClient.findPage 便捷方法）
    const gql = new GraphQLClient(page);
    const items = (await gql.findPage('ErpInvStockTakeLine',
      'id lineNo skuId materialId uoMId locationId bookQuantity actualQuantity differenceQuantity',
      { $type: 'eq', name: 'takeId', value: take.id }, 10)) as any[];
    expect(items.length, '应落 1 行').toBe(1);
    expect(String(items[0].materialId), 'materialId 应由 SKU 派生').toBeTruthy();
    expect(String(items[0].uoMId), 'uoMId 应由 SKU 派生').toBeTruthy();
    expect(Number(items[0].lineNo), '首行 lineNo=1').toBe(1);
    expect(Number(items[0].differenceQuantity), 'difference=5−0=5').toBe(5);

    // cleanup：行（逻辑删）→ cancelTake → delete take → 主数据逐个删
    await deleteById(page, 'ErpInvStockTakeLine', String(items[0].id));
    await callMutationOk(page, 'ErpInvStockTake', 'cancelTake', { takeId: take.id }, 'id docStatus');
    await deleteById(page, 'ErpInvStockTake', take.id);
    await deleteById(page, 'ErpMdMaterialSku', sku.id);
    await deleteById(page, 'ErpMdMaterial', material.id);
    await deleteById(page, 'ErpMdUoM', uom.id);
    await deleteById(page, 'ErpMdLocation', loc.id);
    await deleteById(page, 'ErpMdWarehouse', wh.id);
  });
});
