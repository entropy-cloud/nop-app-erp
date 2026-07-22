// DOM assertions for F12 — page structure (tabs / dashboard) rendering
// (plan 2026-07-21-0330-3-f12-page-structure-tabs-wizards.md +
//  plan 2026-07-22-0845-1-f12-tier-d-and-dashboard-drawer-successor.md).
//
// Validates that sampled head entities actually render the AMIS `tabs`
// component at runtime — proving the codegen pipeline (`<form layoutControl="tabs">`
// → GenLayoutTabs → AMIS `type: tabs`) is wired end-to-end. Same DOM-className
// strategy as readonly-views.visual.spec.ts / status-tag.visual.spec.ts /
// ext-domains-child-table.visual.spec.ts.
//
// Why DOM-className rather than pixel-snapshot: stable across AMIS upgrades
// and font/animation drift, while still proving the tabs contract is wired
// end-to-end (view.xml layoutControl="tabs" → codegen → AMIS JSON → DOM).
//
// Coverage (8 sampled entities per plan Phase 3):
//   - ErpPurOrder (purchase, Tier A: head+line tabs + sub-grid-edit lines tab)
//   - ErpFinVoucher (finance, Tier A: voucher + 17-col sub-grid-edit + autoBalance button)
//   - ErpHrEmployee (hr, Tier B: multi-tab employee profile + sensitive fields hidden)
//   - ErpAstAsset (assets, Tier B: dashboard-style multi-group tabs)
//   - ErpCtContract (contract, Tier D: 7-tab form + lines+versions sub-grid cells)
//   - ErpQaInspection (quality, Tier D: 4-tab form)
//   - ErpHrEmployee drawer (hr, Tier B drawer: 5-tab employee archive)
//   - ErpMntEquipment drawer (mnt, Tier B drawer: 3-tab equipment dashboard)
//
// Two-layer assertion:
//   1. AMIS `tabs` component rendered in the view/edit drawer (.cxd-Tabs)
//   2. At least one expected tab title is present in the rendered tabs
//   3. (Tier A only) AMIS `input-table` (sub-grid-edit) still rendered in lines tab
//   4. (ErpHrEmployee only) sensitive fields (bankAccountId/socialSecurityNo/taxFileNo/idCardNo)
//      NOT visible in the rendered form
//   5. (Tier B drawer only) row-action triggers drawer + drawer contains .cxd-Tabs +
//      switching tab loads crud data

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

/** Tier B drawer assertion: row-action triggers a drawer that contains tabs. */
interface F12DrawerAssertion {
  /** Hash route for the head entity list page. */
  route: string;
  /** Human-readable label for logging. */
  label: string;
  /** Row action button text used to trigger the drawer (e.g. '完整档案'). */
  drawerActionText: string[];
  /** Expected tab titles inside the drawer. */
  expectedDrawerTabTitles: string[];
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
  {
    route: '/ErpCtContract-main',
    label: 'contract CtContract edit drawer (Tier D: 7 tabs + lines+versions sub-grid)',
    openActionText: ['编辑', 'Edit'],
    expectedTabTitles: ['基本信息', '合同方信息', '合同明细行', '合同版本'],
    expectInputTable: true,
  },
  {
    route: '/ErpQaInspection-main',
    label: 'quality QaInspection edit drawer (Tier D: 4 tabs)',
    openActionText: ['编辑', 'Edit'],
    expectedTabTitles: ['基本信息', '检验信息', '抽样信息'],
  },
];

const DRAWER_ASSERTIONS: F12DrawerAssertion[] = [
  {
    route: '/ErpHrEmployee-main',
    label: 'hr Employee full archive drawer (Tier B drawer: 5 tabs)',
    drawerActionText: ['完整档案'],
    expectedDrawerTabTitles: ['基本信息', '合同', '考勤', '休假', '工时'],
  },
  {
    route: '/ErpMntEquipment-main',
    label: 'mnt Equipment full dashboard drawer (Tier B drawer: 3 tabs)',
    drawerActionText: ['完整仪表板'],
    expectedDrawerTabTitles: ['基本信息', '维护时间线', '备件消耗'],
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

test.describe('F12 — Tier B dashboard drawer DOM rendering', () => {
  for (const assertion of DRAWER_ASSERTIONS) {
    test(`${assertion.label} triggers drawer via row-action and renders tabs`, async ({ page }) => {
      await navigateAndWaitForCrud(page, assertion.route);

      const crud = page.locator('.cxd-Crud');
      const firstRow = crud.locator('tbody tr').first();
      const hasRow = await firstRow.count().then(c => c > 0).catch(() => false);
      if (!hasRow) {
        test.skip(true, `${assertion.label}: no seed row available — codegen-level coverage via ErpAllWebPagesTest`);
        return;
      }

      await firstRow.hover();

      // Click the drawer-trigger row-action (e.g. '完整档案' / '完整仪表板')
      const drawerBtn = crud.locator('button, a').filter({ hasText: new RegExp(assertion.drawerActionText.join('|')) }).first();
      await expect.poll(
        async () => drawerBtn.count(),
        { timeout: 10_000, message: `${assertion.label}: drawer trigger button should appear` },
      ).toBeGreaterThan(0);
      await drawerBtn.click();

      // Wait for drawer to render with tabs
      const drawer = page.locator('.cxd-Drawer').last();
      await expect.poll(
        async () => drawer.locator('.cxd-Tabs').count(),
        { timeout: 30_000, message: `${assertion.label}: .cxd-Tabs should render in drawer` },
      ).toBeGreaterThan(0);

      // Collect rendered drawer tab titles
      const tabLinks = drawer.locator('.cxd-Tabs .cxd-Tabs-link');
      const tabLinkCount = await tabLinks.count();
      expect(tabLinkCount, `${assertion.label}: drawer should have multiple tabs`).toBeGreaterThanOrEqual(2);

      const renderedTitles: string[] = [];
      for (let i = 0; i < tabLinkCount; i++) {
        const text = await tabLinks.nth(i).innerText().catch(() => '');
        renderedTitles.push(text.trim());
      }

      // All expected drawer tab titles must be present (drawer tabs are deterministic)
      const missingTitles = assertion.expectedDrawerTabTitles.filter(
        expected => !renderedTitles.some(rendered => rendered.includes(expected)),
      );
      expect(
        missingTitles.length,
        `${assertion.label}: missing drawer tab titles ${JSON.stringify(missingTitles)}. Rendered: ${JSON.stringify(renderedTitles)}`,
      ).toBe(0);

      // Click a non-header tab to verify crud data loads (switch from first tab)
      if (tabLinkCount > 1) {
        await tabLinks.nth(1).click().catch(() => {});
        await page.waitForTimeout(800);
        // Either a Crud table or a Form should be present in the tab body
        const tabBody = drawer.locator('.cxd-Tabs-pane, .cxd-Tabs-body').last();
        await expect.poll(
          async () => tabBody.locator('.cxd-Crud, .cxd-Form, .cxd-Service').count(),
          { timeout: 20_000, message: `${assertion.label}: tab body should render crud/form/service after switch` },
        ).toBeGreaterThan(0);
      }
    });
  }
});
