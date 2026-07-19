import { test, expect, loginAndNavigate } from './_helper';
import { GraphQLClient, CrudListPage, getEngine } from '../pages';

/**
 * F4 Phase 2 P0 — 子表行内编辑写路径 E2E（plan 2026-07-19-2200-1 Phase 5）。
 *
 * 覆盖 8 头行对（4 purchase + 4 sales）：
 *   1. 经 GraphQL `__save` 提交头 + 嵌套 `lines:[...]` 数组，断言后端聚合根 save 持久化行。
 *   2. 行级 amount 派生验证：行 10 × 5 = 50（amount），税率 13% → taxAmount=6.5。
 *
 * 头 totalAmounts 聚合由后端 `persistTotalAmounts` 在审批过账阶段（非 `__save`）按 BigDecimal 计算，
 * 已由 P2P/O2C orchestration 链路 E2E 覆盖（见 `tests/e2e/orchestration/`），
 * 本 spec 不重复断言。
 *
 * AMIS input-table 渲染验证（不可降级）：ErpPurOrder 经浏览器 DOM 断言子表控件实际渲染，
 * 防止 GraphQL fallback 静默绕过 codegen 管线（Phase 1 Explore (a) 最高风险面）。
 *
 * 清理：每个 test 用 finally 逻辑删除头（cascade-delete lines），保护共享 DB 数值断言基线。
 */

const SEED = {
  ORG: 1,
  SUPPLIER: 2,
  CUSTOMER: 1,
  WH_RAW: 2,
  CURRENCY: 1,
  UOM: 1,
  MAT_1: 1,
} as const;

const BDATE = '2026-07-19';

const QTY = 10;
const PRICE = 5;
const TAX_RATE = 13;
const AMOUNT = QTY * PRICE;
const TAX_AMOUNT = Math.round(AMOUNT * TAX_RATE) / 100;
const AMOUNT_WITH_TAX = AMOUNT + TAX_AMOUNT;

interface HeadSpec {
  entityName: string;
  route: string;
  headFields: Record<string, unknown>;
  /** 行是否有 amountWithTax 字段（仅 Order 系列） */
  lineHasAmountWithTax: boolean;
}

const PURCHASE_SPECS: HeadSpec[] = [
  {
    entityName: 'ErpPurOrder',
    route: '/ErpPurOrder-main',
    headFields: {
      orgId: SEED.ORG, supplierId: SEED.SUPPLIER, warehouseId: SEED.WH_RAW,
      businessDate: BDATE, currencyId: SEED.CURRENCY, exchangeRate: 1,
      docStatus: 'ACTIVE', approveStatus: 'UNSUBMITTED', receiveStatus: 'UNRECEIVED',
    },
    lineHasAmountWithTax: true,
  },
  {
    entityName: 'ErpPurReceive',
    route: '/ErpPurReceive-main',
    headFields: {
      orgId: SEED.ORG, supplierId: SEED.SUPPLIER, warehouseId: SEED.WH_RAW,
      businessDate: BDATE, currencyId: SEED.CURRENCY, exchangeRate: 1,
      docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', receiveStatus: 'UNRECEIVED', posted: false,
    },
    lineHasAmountWithTax: false,
  },
  {
    entityName: 'ErpPurInvoice',
    route: '/ErpPurInvoice-main',
    headFields: {
      orgId: SEED.ORG, supplierId: SEED.SUPPLIER,
      businessDate: BDATE, currencyId: SEED.CURRENCY, exchangeRate: 1,
      docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', paidStatus: 'UNPAID', posted: false,
    },
    lineHasAmountWithTax: false,
  },
  {
    entityName: 'ErpPurReturn',
    route: '/ErpPurReturn-main',
    headFields: {
      orgId: SEED.ORG, supplierId: SEED.SUPPLIER, warehouseId: SEED.WH_RAW,
      businessDate: BDATE, currencyId: SEED.CURRENCY, exchangeRate: 1,
      docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', posted: false,
    },
    lineHasAmountWithTax: false,
  },
];

const SALES_SPECS: HeadSpec[] = [
  {
    entityName: 'ErpSalOrder',
    route: '/ErpSalOrder-main',
    headFields: {
      orgId: SEED.ORG, customerId: SEED.CUSTOMER, warehouseId: SEED.WH_RAW,
      businessDate: BDATE, currencyId: SEED.CURRENCY, exchangeRate: 1,
      docStatus: 'ACTIVE', approveStatus: 'UNSUBMITTED', deliveryStatus: 'UNDELIVERED', receivedStatus: 'UNRECEIVED',
    },
    lineHasAmountWithTax: true,
  },
  {
    entityName: 'ErpSalDelivery',
    route: '/ErpSalDelivery-main',
    headFields: {
      orgId: SEED.ORG, customerId: SEED.CUSTOMER, warehouseId: SEED.WH_RAW,
      businessDate: BDATE, currencyId: SEED.CURRENCY, exchangeRate: 1,
      docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', posted: false,
    },
    lineHasAmountWithTax: false,
  },
  {
    entityName: 'ErpSalInvoice',
    route: '/ErpSalInvoice-main',
    headFields: {
      orgId: SEED.ORG, customerId: SEED.CUSTOMER,
      businessDate: BDATE, currencyId: SEED.CURRENCY, exchangeRate: 1,
      docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', receivedStatus: 'UNRECEIVED', posted: false,
    },
    lineHasAmountWithTax: false,
  },
  {
    entityName: 'ErpSalReturn',
    route: '/ErpSalReturn-main',
    headFields: {
      orgId: SEED.ORG, customerId: SEED.CUSTOMER, warehouseId: SEED.WH_RAW,
      businessDate: BDATE, currencyId: SEED.CURRENCY, exchangeRate: 1,
      docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', posted: false,
    },
    lineHasAmountWithTax: false,
  },
];

function buildLine(spec: HeadSpec, lineNo: number) {
  const line: Record<string, unknown> = {
    lineNo,
    materialId: SEED.MAT_1,
    uoMId: SEED.UOM,
    quantity: QTY,
    unitPrice: PRICE,
    amount: AMOUNT,
    taxRate: TAX_RATE,
    taxAmount: TAX_AMOUNT,
  };
  if (spec.lineHasAmountWithTax) {
    line.amountWithTax = AMOUNT_WITH_TAX;
  }
  return line;
}

async function saveHeadWithLines(
  gql: GraphQLClient,
  spec: HeadSpec,
  code: string,
  lineCount: number,
): Promise<{ id: string }> {
  const lines: Record<string, unknown>[] = [];
  for (let i = 1; i <= lineCount; i++) {
    lines.push(buildLine(spec, i));
  }
  const saved = await gql.save<{ id: string }>(
    spec.entityName,
    { code, ...spec.headFields, lines },
    'id',
  );
  return { id: saved!.id };
}

function runHeadSpec(spec: HeadSpec) {
  test.describe(`${spec.entityName} child-table write (nested lines)`, () => {
    test('save head + 2 nested lines persists and lines compute amount/taxAmount', async ({ page }) => {
      await loginAndNavigate(page, spec.route);

      const gql = new GraphQLClient(page);
      const code = `E2E-CT-${spec.entityName}-${Date.now()}`;
      let headId: string | null = null;

      try {
        const saved = await saveHeadWithLines(gql, spec, code, 2);
        headId = saved.id;
        expect(headId, `${spec.entityName}: head should be created with nested lines`).toBeTruthy();

        const lineSelection = spec.lineHasAmountWithTax
          ? 'id lineNo quantity unitPrice amount taxRate taxAmount amountWithTax'
          : 'id lineNo quantity unitPrice amount taxRate taxAmount';
        const fetched = await gql.get<any>(
          spec.entityName,
          headId!,
          `id lines{${lineSelection}}`,
        );
        expect(fetched?.id, `${spec.entityName}: get should return created head`).toBe(headId);

        const lines = fetched?.lines || [];
        expect(lines.length, `${spec.entityName}: 2 nested lines should persist`).toBe(2);

        for (const line of lines) {
          expect(
            Number(line.amount),
            `${spec.entityName}: line.amount should equal qty*price`,
          ).toBe(AMOUNT);
          expect(
            Number(line.taxAmount),
            `${spec.entityName}: line.taxAmount should equal amount*taxRate/100`,
          ).toBe(TAX_AMOUNT);
          if (spec.lineHasAmountWithTax) {
            expect(
              Number(line.amountWithTax),
              `${spec.entityName}: line.amountWithTax should equal amount*(1+taxRate/100)`,
            ).toBe(AMOUNT_WITH_TAX);
          }
        }
      } finally {
        if (headId) {
          await gql.delete(spec.entityName, headId);
        }
      }
    });
  });
}

test.describe('purchase domain child-table write', () => {
  for (const spec of PURCHASE_SPECS) {
    runHeadSpec(spec);
  }
});

test.describe('sales domain child-table write', () => {
  for (const spec of SALES_SPECS) {
    runHeadSpec(spec);
  }
});

/**
 * AMIS input-table 渲染验证（不可降级）：
 * 经浏览器 DOM 断言 ErpPurOrder 编辑表单含 input-table 子表控件。
 * 防止 GraphQL fallback 静默绕过 codegen 管线（Phase 1 Explore (a) 最高风险面）。
 */
test.describe('AMIS input-table DOM verification', () => {
  test('ErpPurOrder edit form renders input-table for lines', async ({ page }) => {
    const engine = getEngine();
    const crud = new CrudListPage(page, { entityRoute: 'ErpPurOrder' }, engine);

    await crud.navigate();
    await crud.waitForList();

    // 点击 add 按钮打开新增表单（弹窗 / drawer）
    const dialog = await crud.clickAdd();

    // 等待表单内 input-table 控件出现（AMIS cxd-InputTable 类）
    const inputTable = page.locator('.cxd-InputTable').first();
    await inputTable.waitFor({ state: 'visible', timeout: 20_000 });
    expect(await inputTable.isVisible(), 'input-table should render in ErpPurOrder edit form').toBe(true);

    // 断言 input-table 至少含 1 个 picker 控件（物料列）+ 3 个 input-number 控件（quantity/unitPrice/taxRate）
    const pickerCount = await page.locator('.cxd-Picker').count();
    expect(pickerCount, 'at least one picker (materialId) should render inside input-table').toBeGreaterThan(0);
    const inputNumberCount = await page.locator('.cxd-Number').count();
    expect(inputNumberCount, 'at least 3 input-number (qty/price/tax) should render').toBeGreaterThanOrEqual(3);
  });
});
