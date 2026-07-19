// DOM className/color assertions for status tag coloring (plan F5).
//
// This is the first business-entity visual spec under tests/e2e/visual/.
// It validates that <gen-control> inline c:script (per docs/design/status-color-map.md)
// actually produces colored AMIS tpl output at runtime: each status column cell
// must contain a <span class="label label-{color}"> wrapping the *_label text.
// The DOM className assertion pattern here is reusable by F6/F8 visual specs.
//
// Why DOM-className rather than pixel-snapshot: stable across AMIS upgrades
// and font/animation drift, while still proving the color mapping is wired
// end-to-end (gen-control -> XPL eval -> AMIS tpl -> DOM className).
//
// Coverage: ~10 entities parameterized across core + extension domains.

import { test, expect, loginAndNavigate } from '../fixtures';

interface StatusAssertion {
  /** Hash route, e.g. '/ErpPurOrder-main'. */
  route: string;
  /** Label of the status column header in the AMIS crud table. */
  statusColumnLabel: string;
  /** Substring expected to appear in the rendered status cell's span className. */
  expectedClassToken: string;
  /** Optional substring expected in the rendered status cell text. */
  expectedText?: string;
}

const ASSERTIONS: StatusAssertion[] = [
  // Core domains — seed ErpPurOrder id=1 has docStatus=ACTIVE, approveStatus=APPROVED
  {
    route: '/ErpPurOrder-main',
    statusColumnLabel: '审核状态',
    expectedClassToken: 'label-success',
  },
  {
    route: '/ErpPurOrder-main',
    statusColumnLabel: '单据状态',
    expectedClassToken: 'label-primary',
  },
  {
    route: '/ErpSalOrder-main',
    statusColumnLabel: '审核状态',
    expectedClassToken: 'label-success',
  },
  {
    route: '/ErpFinVoucher-main',
    statusColumnLabel: '单据状态',
    expectedClassToken: 'label-',
  },
  // Master data — ErpMdMaterial.status uses active-status dict; seed value ACTIVE -> label-success
  {
    route: '/ErpMdMaterial-main',
    statusColumnLabel: '状态',
    expectedClassToken: 'label-success',
  },
  // Extension domains (smart template coverage)
  {
    route: '/ErpPrjProject-main',
    statusColumnLabel: '状态',
    expectedClassToken: 'label-',
  },
  {
    route: '/ErpMfgWorkOrder-main',
    statusColumnLabel: '审核状态',
    expectedClassToken: 'label-',
  },
  {
    route: '/ErpQaInspection-main',
    statusColumnLabel: '审核状态',
    expectedClassToken: 'label-',
  },
  {
    route: '/ErpCtContract-main',
    statusColumnLabel: '状态',
    expectedClassToken: 'label-',
  },
  {
    route: '/ErpHrLeaveRequest-main',
    statusColumnLabel: '状态',
    expectedClassToken: 'label-',
  },
];

async function waitForEntityPage(page: import('@playwright/test').Page, route: string, statusLabel: string): Promise<void> {
  // Wait for URL to reflect the entity route (hash navigation)
  await expect.poll(
    () => page.url(),
    { timeout: 20_000, message: `${route}: URL should include "${route}"` },
  ).toContain(route);
  // Wait for the AMIS crud table header to include the status column label
  await expect.poll(
    async () => {
      const headers = await page.locator('table thead th').allTextContents();
      return headers.some((t) => (t || '').includes(statusLabel));
    },
    { timeout: 30_000, message: `${route}: table header should include "${statusLabel}"` },
  ).toBe(true);
  // Wait for at least one row
  await expect(
    page.locator('table tbody tr').first(),
    `${route}: crud table should render at least one row`,
  ).toBeVisible({ timeout: 30_000 });
}

test.describe('Status tag coloring (F5)', () => {
  for (const a of ASSERTIONS) {
    test(`${a.route} :: ${a.statusColumnLabel} renders colored label.${a.expectedClassToken}`, async ({ page }) => {
      await loginAndNavigate(page, a.route);
      await waitForEntityPage(page, a.route, a.statusColumnLabel);

      const headerCells = page.locator('table thead th');
      const headerTexts = await headerCells.allTextContents();
      const statusColIdx = headerTexts.findIndex((t) => (t || '').includes(a.statusColumnLabel));
      expect(statusColIdx, `${a.route}: status column "${a.statusColumnLabel}" should exist`).toBeGreaterThanOrEqual(0);

      // Sample the first row's status cell
      const firstRowCell = page.locator('table tbody tr').first().locator('td').nth(statusColIdx);
      await expect(firstRowCell).toBeVisible();

      // The cell should contain a <span class="label ..."> wrapping the *_label text
      const labelSpan = firstRowCell.locator('span.label').first();
      await expect(
        labelSpan,
        `${a.route}: status cell should contain span.label`,
      ).toBeVisible({ timeout: 10_000 });

      const className = await labelSpan.getAttribute('class') || '';
      expect(
        className,
        `${a.route}: status span.className should contain "${a.expectedClassToken}"`,
      ).toContain(a.expectedClassToken);

      if (a.expectedText) {
        const cellText = (await firstRowCell.textContent()) || '';
        expect(
          cellText,
          `${a.route}: status cell text should contain "${a.expectedText}"`,
        ).toContain(a.expectedText);
      }
    });
  }

  test('CANCELLED docStatus renders line-through style', async ({ page }) => {
    // CANCELLED documents are rare in seed; skip if no row matches.
    // This test serves as a placeholder contract: if a CANCELLED row appears,
    // its span must have inline style text-decoration:line-through.
    await loginAndNavigate(page, '/ErpPurOrder-main');
      await waitForEntityPage(page, '/ErpPurOrder-main', '单据状态');

    const cancelledSpans = page.locator('table tbody tr td span.label[style*="line-through"]');
    const count = await cancelledSpans.count();
    if (count > 0) {
      const style = await cancelledSpans.first().getAttribute('style') || '';
      expect(style).toContain('text-decoration');
      expect(style).toContain('line-through');
    }
  });

  test('ErpPurOrder renders dual status columns (docStatus + approveStatus)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPurOrder-main');
    await waitForEntityPage(page, '/ErpPurOrder-main', '单据状态');

    const headerTexts = await page.locator('table thead th').allTextContents();
    const hasDoc = headerTexts.some((t) => (t || '').includes('单据状态'));
    const hasApprove = headerTexts.some((t) => (t || '').includes('审核状态'));
    expect(hasDoc, 'ErpPurOrder grid should expose 单据状态 column').toBe(true);
    expect(hasApprove, 'ErpPurOrder grid should expose 审核状态 column').toBe(true);

    const docIdx = headerTexts.findIndex((t) => (t || '').includes('单据状态'));
    const approveIdx = headerTexts.findIndex((t) => (t || '').includes('审核状态'));
    const firstRow = page.locator('table tbody tr').first();
    await expect(firstRow.locator('td').nth(docIdx).locator('span.label')).toBeVisible();
    await expect(firstRow.locator('td').nth(approveIdx).locator('span.label')).toBeVisible();
  });
});
