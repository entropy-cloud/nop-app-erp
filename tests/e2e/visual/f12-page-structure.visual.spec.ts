// DOM assertions for F12 — page structure (tabs / dashboard) rendering
// (plan 2026-07-21-0330-3-f12-page-structure-tabs-wizards.md).
//
// Validates that 8 sampled head entities actually render the AMIS `tabs`
// component at runtime — proving the codegen pipeline (`<form layoutControl="tabs">`
// → GenLayoutTabs → AMIS `type: tabs`) is wired end-to-end. Same DOM-className
// strategy as readonly-views.visual.spec.ts / status-tag.visual.spec.ts /
// ext-domains-child-table.visual.spec.ts.
//
// Why DOM-className rather than pixel-snapshot: stable across AMIS upgrades
// and font/animation drift, while still proving the tabs contract is wired
// end-to-end (view.xml layoutControl="tabs" → codegen → AMIS JSON → DOM).
//
// Coverage (4 sampled entities per plan Phase 3):
//   - ErpPurOrder (purchase, Tier A: head+line tabs + sub-grid-edit lines tab)
//   - ErpFinVoucher (finance, Tier A: voucher + 17-col sub-grid-edit + autoBalance button)
//   - ErpHrEmployee (hr, Tier B: multi-tab employee profile + sensitive fields hidden)
//   - ErpAstAsset (assets, Tier B: dashboard-style multi-group tabs)
//
// Two-layer assertion:
//   1. AMIS `tabs` component rendered in the view/edit drawer (.cxd-Tabs)
//   2. At least one expected tab title is present in the rendered tabs
//   3. (Tier A only) AMIS `input-table` (sub-grid-edit) still rendered in lines tab
//   4. (ErpHrEmployee only) sensitive fields (bankAccountId/socialSecurityNo/taxFileNo/idCardNo)
//      NOT visible in the rendered form

import { test, expect, loginAndNavigate } from '../fixtures';

interface F12Assertion {
  /** Hash route for the head entity list page, e.g. '/ErpPurOrder-main'. */
  route: string;
  /** Human-readable label for logging. */
  label: string;
  /** Row action button used to open the view or edit drawer. */
  openActionText: string[];
  /** Expected tab titles (substring match, at least one must be present). */
  expectedTabTitles: string[];
  /** Whether to assert sub-grid-edit (input-table) is present (Tier A only). */
  expectInputTable?: boolean;
  /** Sensitive field labels that must NOT be visible (ErpHrEmployee only). */
  hiddenSensitiveFields?: string[];
}

const ASSERTIONS: F12Assertion[] = [
  {
    route: '/ErpPurOrder-main',
    label: 'purchase PurOrder edit drawer (Tier A: 5 tabs + sub-grid-edit lines)',
    openActionText: ['编辑', 'Edit'],
    expectedTabTitles: ['基本信息', '金额信息', '明细行'],
    expectInputTable: true,
  },
  {
    route: '/ErpFinVoucher-main',
    label: 'finance Voucher edit drawer (Tier A: tabs + 17-col sub-grid-edit + autoBalance)',
    openActionText: ['编辑', 'Edit'],
    expectedTabTitles: ['基本信息', '过账信息', '分录行'],
    expectInputTable: true,
  },
  {
    route: '/ErpHrEmployee-main',
    label: 'hr Employee edit drawer (Tier B: 6 tabs + sensitive fields hidden)',
    openActionText: ['编辑', 'Edit'],
    expectedTabTitles: ['基本信息', '联系方式', '证件信息', '雇佣信息', '薪酬信息'],
    hiddenSensitiveFields: ['工资卡账户', '社保号', '个税档案号', '证件号码'],
  },
  {
    route: '/ErpAstAsset-main',
    label: 'assets Asset edit drawer (Tier B: 5 tabs dashboard)',
    openActionText: ['编辑', 'Edit'],
    expectedTabTitles: ['基本信息', '价值信息', '折旧信息', '使用信息'],
  },
];

async function navigateAndWaitForCrud(page: import('@playwright/test').Page, route: string): Promise<void> {
  await loginAndNavigate(page, route);
  await expect.poll(
    () => page.url(),
    { timeout: 20_000, message: `${route}: URL should include "${route}"` },
  ).toContain(route);
  // Wait for crud container to render
  await page.waitForSelector('.cxd-Crud', { timeout: 20_000 });
  await page.waitForTimeout(1500);
}

test.describe('F12 — page structure tabs DOM rendering', () => {
  for (const assertion of ASSERTIONS) {
    test(`${assertion.label} renders AMIS tabs component with expected tab titles`, async ({ page }) => {
      await navigateAndWaitForCrud(page, assertion.route);

      // Skip gracefully if no seed row available (ext domain sparseness)
      const crud = page.locator('.cxd-Crud');
      const firstRow = crud.locator('tbody tr').first();
      const hasRow = await firstRow.count().then(c => c > 0).catch(() => false);
      if (!hasRow) {
        test.skip(true, `${assertion.label}: no seed row available — codegen-level coverage via ErpAllWebPagesTest`);
        return;
      }

      // Hover the first row to reveal row actions
      await firstRow.hover();

      // Click the row action button (try each openActionText candidate)
      const actionBtn = crud.locator('button, a').filter({ hasText: new RegExp(assertion.openActionText.join('|')) }).first();
      await expect.poll(
        async () => actionBtn.count(),
        { timeout: 10_000, message: `${assertion.label}: row action button should appear` },
      ).toBeGreaterThan(0);
      await actionBtn.click();

      // Wait for drawer/dialog to render and AMIS Tabs component to mount
      // AMIS Tabs renders as `.cxd-Tabs` container with `.cxd-Tabs-link` per tab
      const modalOrDrawer = page.locator('.cxd-Modal, .cxd-Drawer').last();
      await expect.poll(
        async () => modalOrDrawer.locator('.cxd-Tabs').count(),
        { timeout: 30_000, message: `${assertion.label}: .cxd-Tabs should render in drawer/modal` },
      ).toBeGreaterThan(0);

      // Assert at least one expected tab title is rendered as a tab link
      const tabLinks = modalOrDrawer.locator('.cxd-Tabs .cxd-Tabs-link');
      const tabLinkCount = await tabLinks.count();
      expect(tabLinkCount, `${assertion.label}: should have multiple tab links`).toBeGreaterThanOrEqual(2);

      // Collect rendered tab titles
      const renderedTitles: string[] = [];
      for (let i = 0; i < tabLinkCount; i++) {
        const text = await tabLinks.nth(i).innerText().catch(() => '');
        renderedTitles.push(text.trim());
      }

      // At least one expected title must be a substring of a rendered tab title
      const matchedTitles = assertion.expectedTabTitles.filter(expected =>
        renderedTitles.some(rendered => rendered.includes(expected)),
      );
      expect(
        matchedTitles.length,
        `${assertion.label}: expected at least one of ${JSON.stringify(assertion.expectedTabTitles)} to be substring of rendered tab titles ${JSON.stringify(renderedTitles)}`,
      ).toBeGreaterThan(0);

      // (Tier A only) Assert sub-grid-edit (input-table) is rendered in some tab
      if (assertion.expectInputTable) {
        await expect.poll(
          async () => modalOrDrawer.locator('.cxd-InputTable').count(),
          { timeout: 20_000, message: `${assertion.label}: .cxd-InputTable should render inside a tab` },
        ).toBeGreaterThan(0);
      }

      // (ErpHrEmployee only) Assert sensitive fields are NOT visible
      if (assertion.hiddenSensitiveFields && assertion.hiddenSensitiveFields.length > 0) {
        // Click through each tab to ensure all tab bodies have been rendered
        for (let i = 0; i < tabLinkCount; i++) {
          await tabLinks.nth(i).click().catch(() => {});
          await page.waitForTimeout(300);
        }
        const formLabels = modalOrDrawer.locator('label');
        const formLabelCount = await formLabels.count();
        const renderedLabels: string[] = [];
        for (let i = 0; i < formLabelCount; i++) {
          const text = await formLabels.nth(i).innerText().catch(() => '');
          renderedLabels.push(text.trim());
        }
        for (const hidden of assertion.hiddenSensitiveFields) {
          const leaked = renderedLabels.filter(l => l.includes(hidden));
          expect(
            leaked.length,
            `${assertion.label}: sensitive field "${hidden}" should NOT be visible. Found labels containing it: ${JSON.stringify(leaked)}`,
          ).toBe(0);
        }
      }
    });
  }
});
