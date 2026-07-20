import { test, expect, loginAndNavigate } from './_helper';
import { GraphQLClient, getEngine, CrudListPage } from '../pages';

/**
 * F4 Phase 2 P2 — projects ErpPrjCostCollection 子表行内编辑写路径 E2E
 * （plan 2026-07-20-1020-3-f4p2-child-table-editor-p2-mfg-assets-projects.md Phase 3）。
 *
 * 覆盖 projects 1 头行对（退化变体，无 onEvent 自动推算）：
 *   ErpPrjCostCollection + ErpPrjCostCollectionLine
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
  PROJECT: 1,        // PRJ-2026-001 华东科技 ERP 实施项目（OPEN）
  CURRENCY: 1,
  SUBJECT_COST: 7,   // 6001 主营业务成本（COST, DEBIT）
  SUBJECT_EXPENSE: 8,// 6601 销售费用（EXPENSE, DEBIT）
} as const;

const BDATE = '2026-07-20';
const AMOUNT_1 = 1500;
const AMOUNT_2 = 800;

test.describe('projects domain child-table write', () => {

  test.describe('ErpPrjCostCollection child-table write (nested lines, degenerate variant)', () => {
    test('save head + 2 nested lines persists costCategory/subjectId/amount', async ({ page }) => {
      await loginAndNavigate(page, '/ErpPrjCostCollection-main');

      const gql = new GraphQLClient(page);
      const code = `E2E-PRJ-CC-${Date.now()}`;
      let headId: string | null = null;

      try {
        const lines = [
          {
            lineNo: 1, costCategory: 'LABOR', sourceBillType: 'TIMESHEET',
            sourceBillCode: 'TS-001', subjectId: SEED.SUBJECT_COST, amount: AMOUNT_1,
          },
          {
            lineNo: 2, costCategory: 'EXPENSE', sourceBillType: 'EXPENSE',
            sourceBillCode: 'EX-002', subjectId: SEED.SUBJECT_EXPENSE, amount: AMOUNT_2,
          },
        ];
        const saved = await gql.save<{ id: string }>(
          'ErpPrjCostCollection',
          {
            code, projectId: SEED.PROJECT, orgId: SEED.ORG, businessDate: BDATE,
            currencyId: SEED.CURRENCY, docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED',
            lines,
          },
          'id',
        );
        headId = saved!.id;
        expect(headId, 'ErpPrjCostCollection: head should be created with nested lines').toBeTruthy();

        const fetched = await gql.get<any>(
          'ErpPrjCostCollection',
          headId!,
          'id lines{id lineNo costCategory sourceBillType sourceBillCode subjectId taskId amount}',
        );
        expect(fetched?.id, 'ErpPrjCostCollection: get should return created head').toBe(headId);

        const fetchedLines = fetched?.lines || [];
        expect(fetchedLines.length, 'ErpPrjCostCollection: 2 nested lines should persist').toBe(2);

        const line1 = fetchedLines.find((l: any) => l.lineNo === 1);
        expect(line1, 'ErpPrjCostCollection: line 1 should exist').toBeTruthy();
        expect(line1?.costCategory, 'ErpPrjCostCollection: line 1 costCategory=LABOR').toBe('LABOR');
        expect(Number(line1?.amount), 'ErpPrjCostCollection: line 1 amount=1500').toBe(AMOUNT_1);
        expect(Number(line1?.subjectId), 'ErpPrjCostCollection: line 1 subjectId=6001 main cost').toBe(SEED.SUBJECT_COST);

        const line2 = fetchedLines.find((l: any) => l.lineNo === 2);
        expect(line2, 'ErpPrjCostCollection: line 2 should exist').toBeTruthy();
        expect(line2?.costCategory, 'ErpPrjCostCollection: line 2 costCategory=EXPENSE').toBe('EXPENSE');
        expect(Number(line2?.amount), 'ErpPrjCostCollection: line 2 amount=800').toBe(AMOUNT_2);
      } finally {
        if (headId) {
          await gql.delete('ErpPrjCostCollection', headId);
        }
      }
    });
  });
});

/**
 * AMIS input-table 渲染验证（不可降级）：
 * 经浏览器 DOM 断言 ErpPrjCostCollection 编辑表单含 input-table 子表控件（codegen 展开成功，
 * 非 GraphQL fallback）。行内 picker/input-number 在用户点击「添加行」前不渲染，故仅断言
 * input-table 容器存在；行级写路径已被前一个 spec 经 GraphQL __save 嵌套行全覆盖。
 */
test.describe('AMIS input-table DOM verification (projects)', () => {
  test('ErpPrjCostCollection edit form renders input-table for lines', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPrjCostCollection-main');

    const engine = getEngine();
    const crud = new CrudListPage(page, { entityRoute: 'ErpPrjCostCollection' }, engine);

    const table = engine.table(page);
    await table.waitFor({ state: 'visible', timeout: 30_000 });

    await crud.clickAdd();

    const inputTable = page.locator('.cxd-InputTable').first();
    await inputTable.waitFor({ state: 'visible', timeout: 20_000 });
    expect(
      await inputTable.isVisible(),
      'input-table should render in ErpPrjCostCollection edit form (proves codegen expanded <cell id="lines">)',
    ).toBe(true);
  });
});
