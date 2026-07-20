import { test, expect, loginAndNavigate, createViaSave, callMutationOk, findPageTotal, findItems, eqFilter, andFilter, input, deleteByFilter, deleteById } from './_helper';

/**
 * F9 跨单据导航与关联回链 业务动作浏览器层 E2E（plan 2026-07-20-0629-3）。
 *
 * 验证 F9 落地的 4 类交互背后的数据可达性（前端 AMIS 行为依赖后端 GraphQL 数据可读）：
 *   1. PO 详情关联区 drawer（ref-order.page.yaml fixedProps=orderId）→ ErpPurReceive __findPage filter_orderId 可达
 *   2. PO 「创建入库单」link 跳转目标 → ErpPurReceive __findPage filter_orderId 可达
 *   3. Receive 子表「从订单导入」picker 源 → ErpPurOrderLine __findPage 可达（multi-select 数据基线）
 *   4. Receive copy-line 行导入持久化 → 创建 Receive orderId=PO + lines 映射自 PO lines（__save + 行数断言）
 *   5. Invoice 子表「从入库导入」picker 源 → ErpPurReceiveLine __findPage 可达
 *   6. StockMove「查看流水」link 跳转目标 → ErpInvStockLedger __findPage filter_moveId 可达
 *
 * 注：UI 渲染层（DOM 内的 drawer / picker 按钮）属 Playwright visual spec 范畴（F9 视觉验证 successor）；
 * 本 spec 仅验证 AMIS 行为依赖的后端数据可达性 + copy-line-from-order 端到端持久化链路。
 *
 * 种子引用（master-data init-data）：material MAT-001 id=1 / uom id=1 / currency CNY id=1 / supplier id=1。
 */

const SUPPLIER_ID = 1;
const CUSTOMER_ID = 1;
const MATERIAL_ID = 1;
const UOM_ID = 1;
const CURRENCY_ID = 1;
const WAREHOUSE_ID = 2;
const BDATE = '2026-07-20';

async function seedPurchaseOrder(page: import('@playwright/test').Page, tag: string): Promise<{ id: string; lineIds: string[] }> {
  const po = await createViaSave(
    page, 'ErpPurOrder',
    {
      code: `E2E-F9-PO-${tag}-${Date.now()}`,
      supplierId: SUPPLIER_ID,
      businessDate: BDATE,
      currencyId: CURRENCY_ID,
      docStatus: 'ACTIVE',
      approveStatus: 'APPROVED',
      lines: [
        { lineNo: 1, materialId: MATERIAL_ID, uoMId: UOM_ID, quantity: 10, unitPrice: 5, taxRate: 13, amount: 50, taxAmount: 6.5, amountWithTax: 56.5 },
        { lineNo: 2, materialId: MATERIAL_ID, uoMId: UOM_ID, quantity: 20, unitPrice: 4, taxRate: 13, amount: 80, taxAmount: 10.4, amountWithTax: 90.4 },
      ],
    },
    'id lines { id }',
  );
  return { id: po.id, lineIds: (po.lines ?? []).map((l: any) => l.id) };
}

async function seedSalesOrder(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  const so = await createViaSave(
    page, 'ErpSalOrder',
    {
      code: `E2E-F9-SO-${tag}-${Date.now()}`,
      customerId: CUSTOMER_ID,
      businessDate: BDATE,
      currencyId: CURRENCY_ID,
      docStatus: 'ACTIVE',
      approveStatus: 'APPROVED',
      lines: [
        { lineNo: 1, materialId: MATERIAL_ID, uoMId: UOM_ID, quantity: 8, unitPrice: 12, taxRate: 13, amount: 96, taxAmount: 12.48, amountWithTax: 108.48 },
      ],
    },
    'id',
  );
  return { id: so.id };
}

test.describe('F9 cross-document navigation: data reachability + copy-line persistence', () => {
  test('1. PO → Receive drawer/link target: ErpPurReceive __findPage filter_orderId reachable', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPurOrder-main');

    const po = await seedPurchaseOrder(page, 'drawer');

    // Simulate the drawer target query: list all receives for this PO
    // Initially 0 receives (just verifying the query path works)
    const totalEmpty = await findPageTotal(page, 'ErpPurReceive', eqFilter('orderId', Number(po.id)));
    expect(totalEmpty, 'ErpPurReceive __findPage filter_orderId should be reachable (0 before receive created)').toBe(0);

    // cleanup
    await deleteByFilter(page, 'ErpPurOrderLine', eqFilter('orderId', Number(po.id)));
    await deleteById(page, 'ErpPurOrder', po.id);
  });

  test('2. Receive copy-line-from-order persistence: Receive.orderId=PO + lines mapped from PO lines', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPurOrder-main');

    const po = await seedPurchaseOrder(page, 'copyline');
    expect(po.lineIds.length, 'PO should have 2 lines seeded').toBe(2);

    // Fetch PO lines via __findPage (this is what the picker source does)
    const poLines = await findItems<any>(
      page, 'ErpPurOrderLine',
      eqFilter('orderId', Number(po.id)),
      'id lineNo materialId uoMId quantity unitPrice taxRate amount',
    );
    expect(poLines.length, 'ErpPurOrderLine picker source should return 2 rows').toBe(2);

    // Simulate the copy-line AMIS setValue mapping: build Receive lines from PO lines
    // (mapping rule per docs/design/cross-doc-navigation-patterns.md §3.4 + ErpPurReceive.view.xml copyFromOrder cell)
    const receiveLines = poLines.map((l: any, idx: number) => ({
      lineNo: idx + 1,
      materialId: l.materialId,
      uoMId: l.uoMId,
      quantity: l.quantity,
      unitPrice: l.unitPrice,
      taxRate: l.taxRate,
      amount: l.amount,
      orderLineId: l.id,
    }));

    // Persist via __save (the same path the form save button uses)
    const receive = await createViaSave(
      page, 'ErpPurReceive',
      {
        code: `E2E-F9-RCV-${Date.now()}`,
        orderId: Number(po.id),
        supplierId: SUPPLIER_ID,
        warehouseId: WAREHOUSE_ID,
        businessDate: BDATE,
        currencyId: CURRENCY_ID,
        docStatus: 'ACTIVE',
        approveStatus: 'UNSUBMITTED',
        lines: receiveLines,
      },
      'id lines { id lineNo materialId quantity }',
    );

    expect(receive.id, 'Receive should be created').toBeTruthy();
    expect((receive.lines ?? []).length, 'Receive should have 2 lines mapped from PO').toBe(2);
    // Verify copy-line material/quantity mapping preserved (orderLineId may be normalized by backend)
    expect(Number(receive.lines[0].materialId), 'Receive line 1 materialId should match PO line').toBe(MATERIAL_ID);
    expect(Number(receive.lines[0].quantity), 'Receive line 1 quantity should match PO line 1 (10)').toBe(10);
    expect(Number(receive.lines[1].quantity), 'Receive line 2 quantity should match PO line 2 (20)').toBe(20);

    // Cleanup (reverse dependency order: receive lines → receive → po lines → po)
    await deleteByFilter(page, 'ErpPurReceiveLine', eqFilter('receiveId', Number(receive.id)));
    await deleteById(page, 'ErpPurReceive', receive.id);
    await deleteByFilter(page, 'ErpPurOrderLine', eqFilter('orderId', Number(po.id)));
    await deleteById(page, 'ErpPurOrder', po.id);
  });

  test('3. Invoice copy-line-from-receive picker source: ErpPurReceiveLine __findPage reachable', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPurOrder-main');

    const po = await seedPurchaseOrder(page, 'invline');

    // Create a Receive so we have ReceiveLines to pick from
    const receive = await createViaSave(
      page, 'ErpPurReceive',
      {
        code: `E2E-F9-RCV-INV-${Date.now()}`,
        orderId: Number(po.id),
        supplierId: SUPPLIER_ID,
        warehouseId: WAREHOUSE_ID,
        businessDate: BDATE,
        currencyId: CURRENCY_ID,
        docStatus: 'ACTIVE',
        approveStatus: 'UNSUBMITTED',
        lines: [
          { lineNo: 1, materialId: MATERIAL_ID, uoMId: UOM_ID, quantity: 5, unitPrice: 5, taxRate: 13, amount: 25 },
        ],
      },
      'id',
    );

    // Fetch ReceiveLines via __findPage (this is what the Invoice copy-from-receive picker source does)
    const rcvLines = await findItems<any>(
      page, 'ErpPurReceiveLine',
      eqFilter('receiveId', Number(receive.id)),
      'id lineNo materialId uoMId quantity unitPrice',
    );
    expect(rcvLines.length, 'ErpPurReceiveLine picker source should return 1 row').toBe(1);

    // Cleanup
    await deleteByFilter(page, 'ErpPurReceiveLine', eqFilter('receiveId', Number(receive.id)));
    await deleteById(page, 'ErpPurReceive', receive.id);
    await deleteByFilter(page, 'ErpPurOrderLine', eqFilter('orderId', Number(po.id)));
    await deleteById(page, 'ErpPurOrder', po.id);
  });

  test('4. SO → Delivery drawer/link target: ErpSalDelivery __findPage filter_orderId reachable', async ({ page }) => {
    await loginAndNavigate(page, '/ErpSalOrder-main');

    const so = await seedSalesOrder(page, 'drawer');

    // Simulate the drawer target query: list all deliveries for this SO
    const totalEmpty = await findPageTotal(page, 'ErpSalDelivery', eqFilter('orderId', Number(so.id)));
    expect(totalEmpty, 'ErpSalDelivery __findPage filter_orderId should be reachable (0 before delivery created)').toBe(0);

    // Cleanup
    await deleteByFilter(page, 'ErpSalOrderLine', eqFilter('orderId', Number(so.id)));
    await deleteById(page, 'ErpSalOrder', so.id);
  });

  test('5. StockMove → StockLedger link target: ErpInvStockLedger __findPage filter_moveId reachable', async ({ page }) => {
    await loginAndNavigate(page, '/ErpInvStockMove-main');

    // Create a StockMove via generateMove (the canonical creation path; __save direct on ErpInvStockMove
    // requires moveType dict validation + complex line cost fields). generateMove handles defaults.
    const moveReq = input('i_app_erp_inv_biz_StockMoveRequest', {
      moveType: 'INCOMING',
      orgId: 2,
      businessDate: BDATE,
      destWarehouseId: WAREHOUSE_ID,
      acctSchemaId: 1,
      currencyId: CURRENCY_ID,
      lines: [{ materialId: MATERIAL_ID, uoMId: UOM_ID, quantity: 10, unitCost: 5, currencyId: CURRENCY_ID }],
      remark: `E2E-F9-MV-${Date.now()}`,
    });
    const created = await callMutationOk(
      page, 'ErpInvStockMove', 'generateMove',
      { request: moveReq },
      'id docStatus',
    );
    expect(created.id, 'generateMove should return move id').toBeTruthy();

    // The "查看流水" link target query: __findPage filter_moveId
    // generateMove → CONFIRMED (no complete) → no ledger written yet, but the query path is what we verify
    const totalEmpty = await findPageTotal(page, 'ErpInvStockLedger', eqFilter('moveId', Number(created.id)));
    expect(totalEmpty, 'ErpInvStockLedger __findPage filter_moveId should be reachable (0 before complete)').toBe(0);

    // Cleanup (move lines + move; no ledger/balance written before complete)
    await deleteByFilter(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(created.id)));
    await deleteById(page, 'ErpInvStockMove', created.id);
  });
});
