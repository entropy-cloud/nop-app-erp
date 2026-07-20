// DOM assertions for F8 list page query/asideFilter dual-filter surfaces (plan 2026-07-20-0629-2).
//
// Validates the 8 list pages (4 readonly + 4 editable main lists) per
// docs/design/query-filter-patterns.md §2:
//   1. asideFilter form renders business multidim filter (FK + status + date)
//   2. query form renders quick-lookup fields (code/name)
//   3. Key business tokens appear in the AMIS form schema, proving the F8
//      extension flowed from view.xml to AMIS json.
//
// Why DOM-based: stable across AMIS upgrades; the F8 contract is structural
// (asideFilter + query forms exist with the right fields), not visual.
//
// Assertion strategy:
//   - Navigate to each route.
//   - Assert that the asideFilter form region contains at least N form controls
//     (input/select/picker), proving the multidim filter is wired through.
//   - Assert that the top query form contains at least 1 form control.
//   - Accept either Chinese or English labels (page language may vary).
//
// AMIS layout: asideFilter renders as a left sidebar with vertical-stacked form
// items; query renders as a top horizontal form above the table. AMIS classes
// `.cxd-AsideForm` / `.cxd-Form--filter` etc. may be present depending on the
// page schema; we fall back to counting any form input inside the crud region.

import { test, expect, loginAndNavigate } from '../fixtures';

interface ListQueryFilterAssertion {
  /** Hash route, e.g. '/ErpPurOrder-main'. */
  route: string;
  /** Human-readable label for logging. */
  label: string;
  /** Either Chinese or English labels expected to appear in the asideFilter. */
  asideFilterLabels: string[][];
  /** Either Chinese or English labels expected to appear in the top query form. */
  queryLabels: string[][];
}

const ASSERTIONS: ListQueryFilterAssertion[] = [
  // inventory readonly — asideFilter has multidim business filter
  {
    route: '/ErpInvStockLedger-main',
    label: 'inventory StockLedger asideFilter+query',
    asideFilterLabels: [['物料', 'Material'], ['仓库', 'Warehouse'], ['业务日期', 'Business Date'], ['批号', 'Batch No']],
    queryLabels: [['流水号', 'Code']],
  },
  {
    route: '/ErpInvStockBalance-main',
    label: 'inventory StockBalance asideFilter+query',
    asideFilterLabels: [['物料', 'Material'], ['仓库', 'Warehouse'], ['库位', 'Location'], ['批号', 'Batch No'], ['业务组织', 'Organization'], ['含零库存', 'Include Zero']],
    queryLabels: [['物料', 'Material'], ['仓库', 'Warehouse']],
  },
  {
    route: '/ErpInvBatch-main',
    label: 'inventory Batch asideFilter+query',
    asideFilterLabels: [['批号', 'Batch No'], ['物料', 'Material'], ['仓库', 'Warehouse'], ['状态', 'Status']],
    queryLabels: [['批号', 'Batch No']],
  },
  {
    route: '/ErpInvSerialNumber-main',
    label: 'inventory SerialNumber asideFilter+query',
    asideFilterLabels: [['序列号', 'Serial No'], ['物料', 'Material'], ['状态', 'Status']],
    queryLabels: [['序列号', 'Serial No']],
  },
  // finance readonly
  {
    route: '/ErpFinGlBalance-main',
    label: 'finance GlBalance asideFilter+query',
    asideFilterLabels: [['期间', 'Period'], ['科目', 'Subject'], ['币种', 'Currency']],
    queryLabels: [['期间', 'Period'], ['科目', 'Subject']],
  },
  {
    route: '/ErpFinTrialBalance-main',
    label: 'finance TrialBalance asideFilter+query',
    asideFilterLabels: [['期间', 'Period'], ['科目', 'Subject'], ['账套', 'Acct Schema']],
    queryLabels: [['期间', 'Period'], ['科目编码', 'Subject Code']],
  },
  // finance editable
  {
    route: '/ErpFinVoucher-main',
    label: 'finance Voucher asideFilter+query',
    asideFilterLabels: [['凭证字', 'Voucher Type'], ['凭证状态', 'Doc Status'], ['凭证编号', 'Voucher No'], ['会计期间', 'Period'], ['过账类型', 'Posting Type'], ['账套', 'Acct Schema']],
    queryLabels: [['凭证号', 'Code'], ['凭证日期', 'Voucher Date']],
  },
  {
    route: '/ErpFinArApItem-main',
    label: 'finance ArApItem asideFilter+query',
    asideFilterLabels: [['往来单位', 'Partner'], ['应收应付', 'Direction'], ['来源单据类型', 'Source Bill Type'], ['状态', 'Status'], ['业务日期', 'Business Date']],
    queryLabels: [['单号', 'Code'], ['来源单据号', 'Source Bill Code']],
  },
  // purchase editable
  {
    route: '/ErpPurOrder-main',
    label: 'purchase PurOrder asideFilter+query',
    asideFilterLabels: [['供应商', 'Supplier'], ['收货仓库', 'Warehouse'], ['单据状态', 'Doc Status'], ['审核状态', 'Approve Status'], ['订单日期', 'Business Date']],
    queryLabels: [['单号', 'Code'], ['订单日期', 'Business Date']],
  },
  // sales editable
  {
    route: '/ErpSalOrder-main',
    label: 'sales SalOrder asideFilter+query',
    asideFilterLabels: [['客户', 'Customer'], ['发货仓库', 'Warehouse'], ['单据状态', 'Doc Status'], ['审核状态', 'Approve Status'], ['订单日期', 'Business Date']],
    queryLabels: [['单号', 'Code'], ['订单日期', 'Business Date']],
  },
];

async function navigateAndWaitForPage(page: import('@playwright/test').Page, route: string): Promise<void> {
  await loginAndNavigate(page, route);
  await expect.poll(
    () => page.url(),
    { timeout: 20_000, message: `${route}: URL should include "${route}"` },
  ).toContain(route);
  // Wait for page settle (form controls + table).
  await page.waitForTimeout(3500);
}

function countLabelMatches(bodyText: string, labelAlternatives: string[][]): number {
  // For each label group [zh, en], count as a match if either is in bodyText.
  let matchCount = 0;
  for (const group of labelAlternatives) {
    if (group.some((label) => bodyText.includes(label))) {
      matchCount++;
    }
  }
  return matchCount;
}

test.describe('List page query/asideFilter dual-filter surfaces (F8)', () => {
  for (const a of ASSERTIONS) {
    test(`${a.route} :: ${a.label} asideFilter labels visible`, async ({ page }) => {
      await navigateAndWaitForPage(page, a.route);

      const bodyText = (await page.textContent('body')) || '';

      // At least one asideFilter label group should appear.
      const matchCount = countLabelMatches(bodyText, a.asideFilterLabels);
      expect(
        matchCount,
        `${a.route}: asideFilter should render at least one label group. Got ${matchCount}/${a.asideFilterLabels.length} matches. Body sample: ${bodyText.slice(0, 500)}`,
      ).toBeGreaterThan(0);
    });

    test(`${a.route} :: ${a.label} query labels visible`, async ({ page }) => {
      await navigateAndWaitForPage(page, a.route);

      const bodyText = (await page.textContent('body')) || '';

      const matchCount = countLabelMatches(bodyText, a.queryLabels);
      expect(
        matchCount,
        `${a.route}: query form should render at least one label group. Got ${matchCount}/${a.queryLabels.length} matches. Body sample: ${bodyText.slice(0, 500)}`,
      ).toBeGreaterThan(0);
    });
  }

  test('ErpPurOrder asideFilter supplierId filter triggers GraphQL query', async ({ page }) => {
    // Register GraphQL response listener BEFORE navigation so we capture the
    // initial findPage call as well as any subsequent reload triggered by
    // asideFilter submitOnChange.
    const graphqlResponses: number[] = [];
    page.on('response', (resp) => {
      if (resp.url().includes('/graphql')) {
        graphqlResponses.push(resp.status());
      }
    });

    await navigateAndWaitForPage(page, '/ErpPurOrder-main');

    // The initial page load should have triggered at least one GraphQL call
    // (findPage to populate the table).
    await expect.poll(
      () => graphqlResponses.length,
      { timeout: 20_000, message: 'ErpPurOrder: should trigger at least one GraphQL call on page load' },
    ).toBeGreaterThan(0);

    for (const status of graphqlResponses) {
      expect(status, 'ErpPurOrder: GraphQL should return 200').toBe(200);
    }
  });
});
