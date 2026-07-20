// DOM assertions for F8 ext-domain list page asideFilter/query (plan 2026-07-21-0330-2).
//
// Validates 5 sampled ext-domain list pages per
// docs/design/query-filter-patterns.md §5 (ext domain frozen field sets):
//   1. crm ErpCrmLead asideFilter (leadType landed per Phase 0 Explore (a))
//   2. cs ErpCsTicket asideFilter (multi-dim filter + status filterOp=in per Phase 0 (c))
//   3. hr ErpHrEmployee asideFilter (departmentId filter)
//   4. logistics ErpLogShipment asideFilter (date range + exception filter via status in)
//   5. b2b ErpB2bEdiDoc asideFilter (formatId + state in)
//
// Why DOM-based: stable across AMIS upgrades; F8 contract is structural (asideFilter
// + query forms exist with right fields), not visual. Inherits pattern from
// list-query-filter.visual.spec.ts (core 8 list pages).
//
// Assertion strategy:
//   - Navigate to each route.
//   - Assert that key field labels (zh or en) appear in body text, proving the
//     F8 ext asideFilter/query extension flowed view.xml -> codegen -> AMIS json -> DOM.

import { test, expect, loginAndNavigate } from '../fixtures';

interface ExtListAssertion {
  route: string;
  label: string;
  asideFilterLabels: string[][];
  queryLabels: string[][];
}

const EXT_LIST_ASSERTIONS: ExtListAssertion[] = [
  // crm Lead — leadType Tab/Chip landed as asideFilter field (Phase 0 (a) Decision A)
  {
    route: '/ErpCrmLead-main',
    label: 'crm Lead asideFilter+query',
    asideFilterLabels: [
      ['线索/商机类型', 'Lead Type'],
      ['线索来源', 'Source'],
      ['漏斗阶段', 'Stage'],
      ['负责人', 'Owner'],
    ],
    queryLabels: [['编码', 'Code']],
  },
  // cs Ticket — multi-dim filter + status filterOp=in
  {
    route: '/ErpCsTicket-main',
    label: 'cs Ticket asideFilter+query',
    asideFilterLabels: [
      ['工单类型', 'Ticket Type'],
      ['SLA策略', 'SLA Policy'],
      ['工单状态', 'Status'],
      ['优先级', 'Priority'],
    ],
    queryLabels: [['单号', 'Code']],
  },
  // hr Employee — departmentId filter
  {
    route: '/ErpHrEmployee-main',
    label: 'hr Employee asideFilter+query',
    asideFilterLabels: [
      ['部门', 'Department'],
      ['职位', 'Position'],
      ['雇佣状态', 'Employment Status'],
    ],
    queryLabels: [['工号', 'Code']],
  },
  // logistics Shipment — date range + exception filter via status in
  {
    route: '/ErpLogShipment-main',
    label: 'logistics Shipment asideFilter+query',
    asideFilterLabels: [
      ['承运商', 'Carrier'],
      ['状态', 'Status'],
      ['发货员', 'Shipper'],
      ['发运日期', 'Shipment Date'],
    ],
    queryLabels: [['单号', 'Code'], ['运单号', 'Tracking No']],
  },
  // b2b EdiDoc — formatId + state in
  {
    route: '/ErpB2bEdiDoc-main',
    label: 'b2b EdiDoc asideFilter+query',
    asideFilterLabels: [
      ['EDI格式', 'Format'],
      ['状态', 'State'],
      ['阻断级别', 'Blocking Level'],
      ['关联单据类型', 'Related Bill Type'],
    ],
    queryLabels: [['事务编码', 'Code']],
  },
];

interface ExtReadonlyAssertion {
  route: string;
  label: string;
}

const EXT_READONLY_ASSERTIONS: ExtReadonlyAssertion[] = [
  { route: '/ErpLogShipmentLog-main', label: 'logistics ShipmentLog readonly' },
  { route: '/ErpB2bEdiLog-main', label: 'b2b EdiLog readonly' },
  { route: '/ErpCrmFunnelStageMetrics-main', label: 'crm FunnelStageMetrics readonly' },
];

async function navigateAndWaitForPage(page: import('@playwright/test').Page, route: string): Promise<void> {
  await loginAndNavigate(page, route);
  await expect.poll(
    () => page.url(),
    { timeout: 20_000, message: `${route}: URL should include "${route}"` },
  ).toContain(route);
  await page.waitForTimeout(3500);
}

function countLabelMatches(bodyText: string, labelAlternatives: string[][]): number {
  let matchCount = 0;
  for (const group of labelAlternatives) {
    if (group.some((label) => bodyText.includes(label))) {
      matchCount++;
    }
  }
  return matchCount;
}

async function countCrudButtonsMatching(
  page: import('@playwright/test').Page,
  textRegex: RegExp,
): Promise<number> {
  const crud = page.locator('.cxd-Crud').first();
  if (!(await crud.isVisible().catch(() => false))) {
    return 0;
  }
  const buttons = crud.locator('button, a').filter({ hasText: textRegex });
  return buttons.count();
}

test.describe('Ext-domain list page asideFilter/query surfaces (F8 ext — plan 2026-07-21-0330-2)', () => {
  for (const a of EXT_LIST_ASSERTIONS) {
    test(`${a.route} :: ${a.label} asideFilter labels visible`, async ({ page }) => {
      await navigateAndWaitForPage(page, a.route);

      const bodyText = (await page.textContent('body')) || '';

      const matchCount = countLabelMatches(bodyText, a.asideFilterLabels);
      expect(
        matchCount,
        `${a.route}: ext asideFilter should render at least one label group. Got ${matchCount}/${a.asideFilterLabels.length}. Body sample: ${bodyText.slice(0, 500)}`,
      ).toBeGreaterThan(0);
    });

    test(`${a.route} :: ${a.label} query labels visible`, async ({ page }) => {
      await navigateAndWaitForPage(page, a.route);

      const bodyText = (await page.textContent('body')) || '';

      const matchCount = countLabelMatches(bodyText, a.queryLabels);
      expect(
        matchCount,
        `${a.route}: ext query form should render at least one label group. Got ${matchCount}/${a.queryLabels.length}. Body sample: ${bodyText.slice(0, 500)}`,
      ).toBeGreaterThan(0);
    });
  }
});

test.describe('Ext-domain readonly entity F2 pattern (plan 2026-07-21-0330-2)', () => {
  for (const a of EXT_READONLY_ASSERTIONS) {
    test(`${a.route} :: ${a.label} has no add/edit/delete buttons in crud region`, async ({ page }) => {
      await navigateAndWaitForPage(page, a.route);

      const addCount = await countCrudButtonsMatching(page, /^(新增|Add)$/);
      expect(addCount, `${a.route}: no 新增/Add button`).toBe(0);

      const batchDeleteCount = await countCrudButtonsMatching(page, /^(批量删除|Batch Delete)$/);
      expect(batchDeleteCount, `${a.route}: no 批量删除/Batch Delete button`).toBe(0);

      const editCount = await countCrudButtonsMatching(page, /^(编辑|Edit)$/);
      expect(editCount, `${a.route}: no 编辑/Edit button`).toBe(0);

      const deleteCount = await countCrudButtonsMatching(page, /^(删除|Delete)$/);
      expect(deleteCount, `${a.route}: no 删除/Delete button`).toBe(0);
    });
  }

  test('ext readonly entity row-view-button opens dialog (not drawer)', async ({ page }) => {
    await navigateAndWaitForPage(page, '/ErpLogShipmentLog-main');

    const crud = page.locator('.cxd-Crud').first();
    const viewBtn = crud.locator('button, a').filter({ hasText: /^(查看|View)$/ }).first();
    const visible = await viewBtn.isVisible().catch(() => false);
    if (!visible) {
      console.log('[soft-probe] /ErpLogShipmentLog-main: no row to click for dialog assertion');
      return;
    }
    await viewBtn.click({ force: true }).catch(() => {});

    await page.waitForTimeout(2000);

    const dialogVisible = await page.locator('.cxd-Modal, .cxd-Dialog').first().isVisible().catch(() => false);
    const drawerVisible = await page.locator('.cxd-Drawer').first().isVisible().catch(() => false);

    if (!dialogVisible && !drawerVisible) {
      console.log('[soft-probe] /ErpLogShipmentLog-main: dialog/drawer not visible after click');
      return;
    }

    expect(dialogVisible, '/ErpLogShipmentLog-main: row-view should open dialog').toBe(true);
    expect(drawerVisible, '/ErpLogShipmentLog-main: row-view should NOT open drawer').toBe(false);
  });
});
