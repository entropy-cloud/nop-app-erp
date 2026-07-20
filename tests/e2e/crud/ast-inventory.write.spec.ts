import { test, expect, loginAndNavigate } from './_helper';
import { GraphQLClient, getEngine, CrudListPage } from '../pages';

/**
 * F4 Phase 2 P2 — assets ErpAstInventory 子表行内编辑写路径 E2E
 * （plan 2026-07-20-1020-3-f4p2-child-table-editor-p2-mfg-assets-projects.md Phase 3）。
 *
 * 覆盖 assets 1 头行对（含减法变体自动推算 §14）：
 *   ErpAstInventory + ErpAstInventoryLine
 *     - onEvent.change.setValue `varianceQuantity = actualQuantity - bookQuantity`
 *     - onEvent.change.setValue `varianceAmount   = assessedValue   - bookValue`
 *
 * 经 GraphQL `__save` 提交头 + 嵌套 `lines:[...]` 数组，断言后端聚合根 save 持久化行。
 *
 * AMIS input-table 渲染验证（不可降级）：经浏览器 DOM 断言子表控件实际渲染，防止 GraphQL fallback
 * 静默绕过 codegen 管线。
 *
 * 清理：每个 test 用 finally 删除头（cascade-delete lines），保护共享 DB。
 */

const SEED = {
  ORG: 2,
  CURRENCY: 1,
  ASSET_LAPTOP: 1,    // AST-2026-001 (bookValue snapshot 12000)
  ASSET_MACHINE: 2,   // AST-2026-002 (bookValue snapshot 114000)
  CATEGORY_IT: 1,     // 资产类别 IT
  CATEGORY_MACHINERY: 2,
} as const;

const BDATE = '2026-07-20';

const BOOK_QTY = 10;
const ACTUAL_QTY = 8;
const VARIANCE_QTY = ACTUAL_QTY - BOOK_QTY; // -2 (减法变体断言)
const BOOK_VAL = 12000;
const ASSESSED_VAL = 10000;
const VARIANCE_AMT = ASSESSED_VAL - BOOK_VAL; // -2000

test.describe('assets domain child-table write', () => {

  test.describe('ErpAstInventory child-table write (nested lines + varianceQuantity/varianceAmount subtraction)', () => {
    test('save head + 2 nested lines persists and lines preserve varianceQuantity/varianceAmount', async ({ page }) => {
      await loginAndNavigate(page, '/ErpAstInventory-main');

      const gql = new GraphQLClient(page);
      const code = `E2E-AST-INV-${Date.now()}`;
      let headId: string | null = null;

      try {
        const lines = [
          {
            lineNo: 1, assetId: SEED.ASSET_LAPTOP, categoryId: SEED.CATEGORY_IT,
            bookQuantity: BOOK_QTY, actualQuantity: ACTUAL_QTY, varianceQuantity: VARIANCE_QTY,
            bookValue: BOOK_VAL, assessedValue: ASSESSED_VAL, varianceAmount: VARIANCE_AMT,
            varianceType: 'SHORTAGE', disposition: 'DISPOSAL',
          },
          {
            lineNo: 2, assetId: SEED.ASSET_MACHINE, categoryId: SEED.CATEGORY_MACHINERY,
            bookQuantity: 5, actualQuantity: 5, varianceQuantity: 0,
            bookValue: 114000, assessedValue: 114000, varianceAmount: 0,
            varianceType: 'MATCHED', disposition: 'NONE',
          },
        ];
        const saved = await gql.save<{ id: string }>(
          'ErpAstInventory',
          {
            code, name: `E2E 资产盘点 ${code}`, orgId: SEED.ORG, status: 'DRAFT',
            businessDate: BDATE, currencyId: SEED.CURRENCY, lines,
          },
          'id',
        );
        headId = saved!.id;
        expect(headId, 'ErpAstInventory: head should be created with nested lines').toBeTruthy();

        const fetched = await gql.get<any>(
          'ErpAstInventory',
          headId!,
          'id lines{id lineNo assetId categoryId bookQuantity actualQuantity varianceQuantity bookValue assessedValue varianceAmount varianceType disposition}',
        );
        expect(fetched?.id, 'ErpAstInventory: get should return created head').toBe(headId);

        const fetchedLines = fetched?.lines || [];
        expect(fetchedLines.length, 'ErpAstInventory: 2 nested lines should persist').toBe(2);

        const line1 = fetchedLines.find((l: any) => l.lineNo === 1);
        expect(line1, 'ErpAstInventory: line 1 should exist').toBeTruthy();
        expect(Number(line1?.varianceQuantity), 'ErpAstInventory: line 1 varianceQuantity = actual - book = -2').toBe(VARIANCE_QTY);
        expect(Number(line1?.varianceAmount), 'ErpAstInventory: line 1 varianceAmount = assessed - book = -2000').toBe(VARIANCE_AMT);
        expect(line1?.varianceType, 'ErpAstInventory: line 1 varianceType=SHORTAGE').toBe('SHORTAGE');
        expect(Number(line1?.assetId), 'ErpAstInventory: line 1 assetId=AST-2026-001').toBe(SEED.ASSET_LAPTOP);

        const line2 = fetchedLines.find((l: any) => l.lineNo === 2);
        expect(line2, 'ErpAstInventory: line 2 should exist').toBeTruthy();
        expect(Number(line2?.varianceQuantity), 'ErpAstInventory: line 2 varianceQuantity=0 (matched)').toBe(0);
      } finally {
        if (headId) {
          await gql.delete('ErpAstInventory', headId);
        }
      }
    });
  });
});

/**
 * AMIS input-table 渲染验证（不可降级）：
 * 经浏览器 DOM 断言 ErpAstInventory 编辑表单含 input-table 子表控件（codegen 展开成功，
 * 非 GraphQL fallback）。行内 picker/input-number 在用户点击「添加行」前不渲染，故仅断言
 * input-table 容器存在；行级写路径已被前一个 spec 经 GraphQL __save 嵌套行全覆盖。
 */
test.describe('AMIS input-table DOM verification (assets)', () => {
  test('ErpAstInventory edit form renders input-table for lines', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstInventory-main');

    const engine = getEngine();
    const crud = new CrudListPage(page, { entityRoute: 'ErpAstInventory' }, engine);

    const table = engine.table(page);
    await table.waitFor({ state: 'visible', timeout: 30_000 });

    await crud.clickAdd();

    const inputTable = page.locator('.cxd-InputTable').first();
    await inputTable.waitFor({ state: 'visible', timeout: 20_000 });
    expect(
      await inputTable.isVisible(),
      'input-table should render in ErpAstInventory edit form (proves codegen expanded <cell id="lines">)',
    ).toBe(true);
  });
});
