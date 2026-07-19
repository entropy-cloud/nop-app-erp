// DOM text assertions for F6 field formatting (千分位 + date + decimal precision).
//
// Validates that <gen-control><c:script>return {type:'number', kilometer:true, precision:N}</c:script></gen-control>
// in view.xml (per docs/design/field-formatting-patterns.md) actually flows through the codegen pipeline
// (flux-web.xlib:GenGridCol -> AMIS column JSON) and produces AMIS-rendered DOM with thousand-separator
// formatted numbers, fixed decimal precision, and YYYY-MM-DD dates.
//
// Assertion strategy (DOM text, not pixel):
// 1. Navigate to entity list page.
// 2. Wait for crud table rows.
// 3. Scroll horizontally to expose amount/date columns (AMIS uses column virtualization).
// 4. Assert at least one cell text matches the expected format regex.
//
// Regex choices:
//   - Thousand-separated number: /\d{1,3}(,\d{3}){1,}(\.\d+)?/ — matches "1,130.00" / "1,000"
//   - 2-decimal precision (amount): /\d+\.\d{2}(?!\d)/ — matches "850.00" / "960.50"
//   - 4-decimal precision (quantity/unitPrice): /\d+\.\d{4}(?!\d)/ — matches "100.0000"
//   - 8-decimal precision (exchangeRate): /\d+\.\d{8}(?!\d)/ — matches "7.25000000"
//   - YYYY-MM-DD date: /\d{4}-\d{2}-\d{2}/ — matches "2026-07-19"
//
// Why regex over strict equality: seed data values may change; we want to prove the format pattern
// is rendered (gen-control is wired through), not assert a specific value. F5 visual spec uses a
// similar pattern-matching approach for span.label tokens.
//
// Why both thousand-sep and precision: seed values < 1000 (e.g., 850) won't trigger thousand-sep,
// but they still get precision:2 formatting applied (850 -> "850.00"). Asserting either pattern
// proves the gen-control is effective.

import { test, expect, loginAndNavigate } from '../fixtures';

interface FieldFormatAssertion {
  /** Hash route, e.g. '/ErpPurOrder-main'. */
  route: string;
  /** Human-readable label for logging. */
  label: string;
  /** Regex expected to match at least one cell text in the crud table. */
  expectedRegex: RegExp;
}

async function navigateAndWaitForTable(page: import('@playwright/test').Page, route: string): Promise<void> {
  await loginAndNavigate(page, route);
  await expect.poll(
    () => page.url(),
    { timeout: 20_000, message: `${route}: URL should include "${route}"` },
  ).toContain(route);
  await expect(
    page.locator('table tbody tr').first(),
    `${route}: crud table should render at least one row`,
  ).toBeVisible({ timeout: 30_000 });
  await page.waitForTimeout(2000);
  // Scroll horizontally to expose amount/date columns (virtualization).
  const scrollContainer = page.locator('.cxd-Table-content,.ant-table-body,table').first();
  if (await scrollContainer.isVisible().catch(() => false)) {
    await scrollContainer.evaluate((el) => {
      el.scrollLeft = (el.scrollWidth || 5000);
    }).catch(() => {});
    await page.waitForTimeout(1500);
  }
}

// Format regexes
const THOUSAND_SEP_RE = /\d{1,3}(,\d{3}){1,}(\.\d+)?/;
const AMOUNT_PRECISION_2_RE = /\d+\.\d{2}(?!\d)/;
const QUANTITY_PRECISION_4_RE = /\d+\.\d{4}(?!\d)/;
const RATE_PRECISION_8_RE = /\d+\.\d{8}(?!\d)/;
const DATE_RE = /\d{4}-\d{2}-\d{2}/;

const ASSERTIONS: FieldFormatAssertion[] = [
  // Master-data — ErpMdExchangeRate.rate=7.25 -> "7.25000000" (8-decimal precision)
  { route: '/ErpMdExchangeRate-main', label: 'master-data rate 8-decimal precision', expectedRegex: RATE_PRECISION_8_RE },
  // Sales — ErpSalOrder.totalAmountWithTax=1130.00 -> "1,130.00" (thousand-sep)
  { route: '/ErpSalOrder-main', label: 'sales totalAmountWithTax 千分位', expectedRegex: THOUSAND_SEP_RE },
  // Purchase — ErpPurOrder.totalAmountWithTax=960.50 -> "960.50" (precision:2; <1000 no thousand-sep)
  { route: '/ErpPurOrder-main', label: 'purchase totalAmountWithTax precision:2', expectedRegex: AMOUNT_PRECISION_2_RE },
  // Finance — ErpFinVoucher.voucherDate=2026-07-05 (YYYY-MM-DD)
  { route: '/ErpFinVoucher-main', label: 'finance voucherDate YYYY-MM-DD', expectedRegex: DATE_RE },
  // Inventory — ErpInvStockMove.businessDate=2026-07-03 (YYYY-MM-DD)
  { route: '/ErpInvStockMove-main', label: 'inventory businessDate YYYY-MM-DD', expectedRegex: DATE_RE },
];

const SOFT_PROBES: FieldFormatAssertion[] = [
  // ErpPurOrderLine has data but the page may require parent context
  { route: '/ErpPurOrderLine-main', label: 'purchase line quantity 4-decimal precision (soft)', expectedRegex: QUANTITY_PRECISION_4_RE },
];

test.describe('Field formatting 千分位 + date + precision (F6)', () => {
  for (const a of ASSERTIONS) {
    test(`${a.route} :: ${a.label}`, async ({ page }) => {
      await navigateAndWaitForTable(page, a.route);

      const cells = page.locator('table tbody tr td');
      const cellCount = await cells.count();
      expect(cellCount, `${a.route}: table should have cells`).toBeGreaterThan(0);

      const cellTexts: string[] = [];
      for (let i = 0; i < cellCount; i++) {
        const txt = (await cells.nth(i).textContent()) || '';
        cellTexts.push(txt.trim());
      }

      const matches = cellTexts.filter((t) => a.expectedRegex.test(t));
      expect(
        matches.length,
        `${a.route}: expected at least one cell to match ${a.expectedRegex}. Got sample: ${JSON.stringify(cellTexts.slice(0, 20))}`,
      ).toBeGreaterThan(0);
    });
  }

  for (const a of SOFT_PROBES) {
    test(`${a.route} :: ${a.label}`, async ({ page }) => {
      await navigateAndWaitForTable(page, a.route).catch(() => {});
      const cells = page.locator('table tbody tr td');
      const cellCount = await cells.count();
      if (cellCount > 0) {
        const cellTexts: string[] = [];
        for (let i = 0; i < cellCount; i++) {
          const txt = (await cells.nth(i).textContent()) || '';
          cellTexts.push(txt.trim());
        }
        const matches = cellTexts.filter((t) => a.expectedRegex.test(t));
        console.log(`[soft-probe] ${a.route}: ${matches.length}/${cellCount} cells match ${a.expectedRegex}`);
      } else {
        console.log(`[soft-probe] ${a.route}: no rows rendered (likely requires parent context)`);
      }
    });
  }
});
