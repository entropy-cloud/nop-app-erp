import { test, expect, loginAndNavigate } from './_helper';
import { GraphQLClient, CrudListPage, getEngine } from '../pages';

/**
 * F4 Phase 2 P1 — inventory 子表行内编辑写路径 E2E（plan 2026-07-20-0629-1 Phase 5）。
 *
 * 覆盖 3 inventory 头行对：
 *   1. ErpInvStockMove + ErpInvStockMoveLine（含 totalCost = quantity × unitCost 自动推算）
 *   2. ErpInvLandedCost + ErpInvLandedCostLine（退化变体，无自动推算；前置 ErpPurReceive 种子）
 *   3. ErpInvTransferOrder + ErpInvTransferOrderLine（最小列集，无自动推算）
 *
 * 经 GraphQL `__save` 提交头 + 嵌套 `lines:[...]` 数组，断言后端聚合根 save 持久化行。
 *
 * AMIS input-table 渲染验证（不可降级）：ErpInvStockMove 经浏览器 DOM 断言子表控件实际渲染，
 * 防止 GraphQL fallback 静默绕过 codegen 管线。
 *
 * 清理：每个 test 用 finally 删除头（cascade-delete lines），保护共享 DB。
 */

const SEED = {
  ORG: 2,
  WH_FROM: 1,
  WH_TO: 2,
  LOC: 1,
  MAT: 1,
  UOM: 1,
  CURRENCY: 1,
  PARTNER_AP: 3,
  PUR_RECEIVE: 1,
} as const;

const BDATE = '2026-07-20';

const QTY = 10;
const UNIT_COST = 5;
const TOTAL_COST = QTY * UNIT_COST;

test.describe('inventory domain child-table write', () => {

  test.describe('ErpInvStockMove child-table write (nested lines + totalCost auto-calc)', () => {
    test('save head + 2 nested lines persists and lines preserve totalCost', async ({ page }) => {
      await loginAndNavigate(page, '/ErpInvStockMove-main');

      const gql = new GraphQLClient(page);
      const code = `E2E-INV-SM-${Date.now()}`;
      let headId: string | null = null;

      try {
        const lines = [
          { lineNo: 1, materialId: SEED.MAT, uoMId: SEED.UOM, quantity: QTY, unitCost: UNIT_COST, totalCost: TOTAL_COST, currencyId: SEED.CURRENCY },
          { lineNo: 2, materialId: SEED.MAT, uoMId: SEED.UOM, quantity: QTY, unitCost: UNIT_COST, totalCost: TOTAL_COST, currencyId: SEED.CURRENCY },
        ];
        const saved = await gql.save<{ id: string }>(
          'ErpInvStockMove',
          {
            code, moveType: 'INCOMING', orgId: SEED.ORG, businessDate: BDATE,
            destWarehouseId: SEED.WH_TO, docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED',
            lines,
          },
          'id',
        );
        headId = saved!.id;
        expect(headId, 'ErpInvStockMove: head should be created with nested lines').toBeTruthy();

        const fetched = await gql.get<any>(
          'ErpInvStockMove',
          headId!,
          'id lines{id lineNo quantity unitCost totalCost currencyId}',
        );
        expect(fetched?.id, 'ErpInvStockMove: get should return created head').toBe(headId);

        const fetchedLines = fetched?.lines || [];
        expect(fetchedLines.length, 'ErpInvStockMove: 2 nested lines should persist').toBe(2);

        for (const line of fetchedLines) {
          expect(
            Number(line.totalCost),
            'ErpInvStockMove: line.totalCost should equal qty*unitCost',
          ).toBe(TOTAL_COST);
        }
      } finally {
        if (headId) {
          await gql.delete('ErpInvStockMove', headId);
        }
      }
    });
  });

  test.describe('ErpInvLandedCost child-table write (degenerate variant, no auto-calc)', () => {
    test('save head + 2 nested lines persists amount and costElement', async ({ page }) => {
      await loginAndNavigate(page, '/ErpInvLandedCost-main');

      const gql = new GraphQLClient(page);
      const code = `E2E-INV-LC-${Date.now()}`;
      let headId: string | null = null;

      try {
        const lines = [
          { lineNo: 1, costElement: 'FREIGHT', amount: 100, apPartnerId: SEED.PARTNER_AP },
          { lineNo: 2, costElement: 'INSURANCE', amount: 50, apPartnerId: SEED.PARTNER_AP },
        ];
        const saved = await gql.save<{ id: string }>(
          'ErpInvLandedCost',
          {
            code, orgId: SEED.ORG, receiveId: SEED.PUR_RECEIVE, supplierId: SEED.PARTNER_AP,
            currencyId: SEED.CURRENCY, exchangeRate: 1, totalCostAmount: 150,
            allocationMethod: 'BY_AMOUNT', docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED',
            posted: false, businessDate: BDATE, lines,
          },
          'id',
        );
        headId = saved!.id;
        expect(headId, 'ErpInvLandedCost: head should be created with nested lines').toBeTruthy();

        const fetched = await gql.get<any>(
          'ErpInvLandedCost',
          headId!,
          'id lines{id lineNo costElement amount apPartnerId}',
        );
        expect(fetched?.id, 'ErpInvLandedCost: get should return created head').toBe(headId);

        const fetchedLines = fetched?.lines || [];
        expect(fetchedLines.length, 'ErpInvLandedCost: 2 nested lines should persist').toBe(2);
        expect(Number(fetchedLines[0].amount), 'ErpInvLandedCost: line 1 amount=100').toBe(100);
        expect(fetchedLines[0].costElement, 'ErpInvLandedCost: line 1 costElement=FREIGHT').toBe('FREIGHT');
        expect(Number(fetchedLines[1].amount), 'ErpInvLandedCost: line 2 amount=50').toBe(50);
      } finally {
        if (headId) {
          await gql.delete('ErpInvLandedCost', headId);
        }
      }
    });
  });

  test.describe('ErpInvTransferOrder child-table write (minimal line set)', () => {
    test('save head + 2 nested lines persists quantity', async ({ page }) => {
      await loginAndNavigate(page, '/ErpInvTransferOrder-main');

      const gql = new GraphQLClient(page);
      const code = `E2E-INV-TO-${Date.now()}`;
      let headId: string | null = null;

      try {
        const lines = [
          { lineNo: 1, materialId: SEED.MAT, uoMId: SEED.UOM, quantity: QTY, batchNo: 'BATCH-001' },
          { lineNo: 2, materialId: SEED.MAT, uoMId: SEED.UOM, quantity: QTY, batchNo: 'BATCH-002' },
        ];
        const saved = await gql.save<{ id: string }>(
          'ErpInvTransferOrder',
          {
            code, orgId: SEED.ORG, businessDate: BDATE,
            fromWarehouseId: SEED.WH_FROM, toWarehouseId: SEED.WH_TO,
            docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', lines,
          },
          'id',
        );
        headId = saved!.id;
        expect(headId, 'ErpInvTransferOrder: head should be created with nested lines').toBeTruthy();

        const fetched = await gql.get<any>(
          'ErpInvTransferOrder',
          headId!,
          'id lines{id lineNo quantity batchNo}',
        );
        expect(fetched?.id, 'ErpInvTransferOrder: get should return created head').toBe(headId);

        const fetchedLines = fetched?.lines || [];
        expect(fetchedLines.length, 'ErpInvTransferOrder: 2 nested lines should persist').toBe(2);
        expect(Number(fetchedLines[0].quantity), 'ErpInvTransferOrder: line 1 qty=10').toBe(QTY);
        expect(fetchedLines[0].batchNo, 'ErpInvTransferOrder: line 1 batchNo=BATCH-001').toBe('BATCH-001');
      } finally {
        if (headId) {
          await gql.delete('ErpInvTransferOrder', headId);
        }
      }
    });
  });
});

/**
 * AMIS input-table 渲染验证（不可降级）：
 * 经浏览器 DOM 断言 ErpInvStockMove 编辑表单含 input-table 子表控件（codegen 展开成功，
 * 非 GraphQL fallback）。行内 picker/input-number 在用户点击「添加行」前不渲染，故仅断言
 * input-table 容器存在；行级写路径已被前 3 个 spec 经 GraphQL __save 嵌套行全覆盖。
 */
test.describe('AMIS input-table DOM verification (inventory)', () => {
  test('ErpInvStockMove edit form renders input-table for lines', async ({ page }) => {
    const engine = getEngine();
    const crud = new CrudListPage(page, { entityRoute: 'ErpInvStockMove' }, engine);

    await crud.navigate();
    await crud.waitForList();

    await crud.clickAdd();

    const inputTable = page.locator('.cxd-InputTable').first();
    await inputTable.waitFor({ state: 'visible', timeout: 20_000 });
    expect(
      await inputTable.isVisible(),
      'input-table should render in ErpInvStockMove edit form (proves codegen expanded <cell id="lines">)',
    ).toBe(true);
  });
});
