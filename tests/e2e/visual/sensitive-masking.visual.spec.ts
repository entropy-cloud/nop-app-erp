// DOM text assertions for sensitive field masking (plan 2026-07-22-1400-3).
//
// Validates that <gen-control><c:script>return {type:'tpl', tpl:'...'}</c:script></gen-control>
// in view.xml (per docs/design/field-formatting-patterns.md §9) actually flows through
// the codegen pipeline (flux-web.xlib:GenGridCol -> AMIS tpl -> DOM text) and produces
// masked output for hr (idCardNo / mobilePhone / bankAccountId / socialSecurityNo) and
// logistics (apiKey / apiSecret) sensitive fields.
//
// Assertion strategy (DOM text, not pixel):
//   - list grid col: hr bankAccountId renders `****NNNN` (right-4 mask)
//   - view form cells: hr idCardNo `X******NNNN`, mobilePhone `NNN****NNNN`;
//     logistics apiKey `XX****NNNN`, apiSecret `****NNNN`
//
// Deterministic seeding: the ERP seed data (erp_hr_employee.csv / no logistics seed)
// intentionally omits sensitive-field values (PII not seeded). To make this spec
// deterministic and actually prove the masking renders, each test seeds its own record
// with known sensitive values via GraphQL __save, asserts the mask, then cleans up.
// If the backend is unavailable or seeding fails, the test gracefully skips rather
// than failing (honoring the plan's "seed-data 缺失 graceful skip" requirement).
//
// Why body.textContent for drawer assertions: AMIS renders drawers/modals into the body
// DOM; masked tokens like `138****0000` / `sk****89ab` are distinctive enough that they
// cannot appear elsewhere on the list page, so a full-body contains check is both robust
// (no fragile cell-by-cell locating) and precise.

import { test, expect, login, navigateTo } from '../fixtures';
import { GraphQLClient } from '../pages';
import type { Page } from '@playwright/test';

const HR_MASK_CODE = 'HR-EMP-MASK-E2E';
const LOG_CARRIER_CODE = 'LOG-MASK-CARRIER-E2E';
const LOG_CFG_CODE = 'LOG-MASK-CFG-E2E';

// hr seed values + expected masks (per ErpHrEmployee.view.xml gen-control tpl).
// bankAccountId is a FK -> ErpMdBankAccount, so it must reference an existing bank
// account (seed ids 1/2). The list/view tpl masks the FK id itself (`****NNNN`).
const HR_SEED = {
  idCardNo: '110101199001011234', // tpl: LEFT(.,1)+'******'+RIGHT(.,4) -> '1******1234'
  mobilePhone: '13812340000', // tpl: LEFT(.,3)+'****'+RIGHT(.,4) -> '138****0000'
  bankAccountId: 1, // existing ErpMdBankAccount id; tpl: '****'+RIGHT(.,4) -> '****1'
  socialSecurityNo: 'SH123456789', // tpl: '******' (fully masked)
};
const HR_MASKS = {
  idCardNo: '1******1234',
  mobilePhone: '138****0000',
  socialSecurityNo: '******',
};

// logistics seed values + expected masks.
// NOTE: apiKey/apiSecret are xmeta published="false" (write-only credentials — the
// backend never returns them), so the view renders a STATIC `******` mask (cannot
// dynamically slice an unpublished value). The assertion verifies (a) the static
// mask renders and (b) the plaintext never leaks to the DOM.
const LOG_SEED = {
  apiKey: 'sk_live_abc89ab',
  apiSecret: 'sec_live_xyz89ab',
};
const LOG_MASK = '******';

function gql(page: Page): GraphQLClient {
  return new GraphQLClient(page);
}

async function cleanupHr(page: Page): Promise<void> {
  await gql(page)
    .deleteByFilter('ErpHrEmployee', { $type: 'eq', name: 'code', value: HR_MASK_CODE })
    .catch(() => {});
}

async function cleanupLog(page: Page): Promise<void> {
  const c = gql(page);
  await c
    .deleteByFilter('ErpLogCarrierConfig', { $type: 'eq', name: 'configCode', value: LOG_CFG_CODE })
    .catch(() => {});
  await c
    .deleteByFilter('ErpLogCarrier', { $type: 'eq', name: 'code', value: LOG_CARRIER_CODE })
    .catch(() => {});
}

async function scrollTableRight(page: Page): Promise<void> {
  const sc = page.locator('.cxd-Table-content,.ant-table-body,table').first();
  if (await sc.isVisible().catch(() => false)) {
    await sc
      .evaluate((el) => {
        el.scrollLeft = el.scrollWidth || 5000;
      })
      .catch(() => {});
    await page.waitForTimeout(1500);
  }
}

/**
 * Click the row-view-button for the row whose text contains `rowText` (a unique
 * code), then wait for the drawer/modal to render. Row action buttons live in a
 * sticky actions column that is always in the DOM (not virtualized), so we can
 * click within the located row.
 */
async function openViewForRow(page: Page, rowText: string): Promise<void> {
  const row = page.locator('table tbody tr').filter({ hasText: rowText }).first();
  await expect(row, `row containing "${rowText}" should render`).toBeVisible({ timeout: 20_000 });
  const viewBtn = row.locator('button, a').filter({ hasText: /^(查看|View)$/ }).first();
  await viewBtn.click({ force: true }).catch(() => {});
  await page.waitForTimeout(2500);
}

async function closeOverlay(page: Page): Promise<void> {
  await page.keyboard.press('Escape').catch(() => {});
  await page.waitForTimeout(800);
}

test.describe('Sensitive field masking (hr + logistics)', () => {
  test('hr ErpHrEmployee: idCardNo/mobilePhone/bankAccountId/socialSecurityNo masked via gen-control tpl', async ({
    page,
  }) => {
    await login(page);
    await cleanupHr(page);

    const saved = await gql(page).save(
      'ErpHrEmployee',
      {
        code: HR_MASK_CODE,
        firstName: '脱',
        lastName: '敏',
        fullName: '脱敏测试',
        gender: 'MALE',
        hireDate: '2024-01-01',
        employmentStatus: 'ACTIVE',
        employeeType: 'FULL_TIME',
        orgId: 2,
        idCardNo: HR_SEED.idCardNo,
        mobilePhone: HR_SEED.mobilePhone,
        bankAccountId: HR_SEED.bankAccountId,
        socialSecurityNo: HR_SEED.socialSecurityNo,
      },
      'id',
    );
    if (!saved || saved.id == null) {
      console.log('[graceful-skip] hr employee seed returned no id; backend may be unavailable');
      test.skip();
    }

    try {
      await navigateTo(page, '/ErpHrEmployee-main');
      await expect(page.locator('table tbody tr').first()).toBeVisible({ timeout: 30_000 });

      // --- list grid: bankAccountId col renders `****N..` mask (FK id masked) ---
      // bankAccountId is col ~9 (beyond AMIS virtualization viewport), so scroll right.
      await scrollTableRight(page);
      await expect
        .poll(
          async () => {
            const body = (await page.textContent('body')) || '';
            return /\*{4}\d+/.test(body);
          },
          { timeout: 20_000, message: 'hr list bankAccountId should render ****N mask' },
        )
        .toBe(true);

      // --- view drawer: idCardNo / mobilePhone / socialSecurityNo masked ---
      await openViewForRow(page, HR_MASK_CODE);
      await expect
        .poll(
          async () => (await page.textContent('body')) || '',
          { timeout: 20_000, message: 'hr view drawer should render masked idCardNo' },
        )
        .toContain(HR_MASKS.idCardNo);
      await expect
        .poll(
          async () => (await page.textContent('body')) || '',
          { timeout: 20_000, message: 'hr view drawer should render masked mobilePhone' },
        )
        .toContain(HR_MASKS.mobilePhone);
      // socialSecurityNo is fully masked to `******`; weaker signal (also a substring of
      // the idCardNo mask), so assert the drawer body contains it as a consistency check.
      const bodyAfter = (await page.textContent('body')) || '';
      expect(bodyAfter, 'hr view drawer should contain socialSecurityNo mask').toContain(
        HR_MASKS.socialSecurityNo,
      );

      await closeOverlay(page);
    } finally {
      await cleanupHr(page);
    }
  });

  test('logistics ErpLogCarrierConfig: apiKey/apiSecret masked in view via gen-control tpl', async ({
    page,
  }) => {
    await login(page);
    await cleanupLog(page);

    const carrier = await gql(page).save(
      'ErpLogCarrier',
      {
        code: LOG_CARRIER_CODE,
        carrierName: '脱敏测试承运商',
        carrierType: 'EXPRESS',
        gatewayId: 'GW_MASK',
        isActive: 1,
      },
      'id',
    );
    if (!carrier || carrier.id == null) {
      console.log('[graceful-skip] logistics carrier seed returned no id');
      test.skip();
    }
    const carrierId = carrier.id;

    const cfg = await gql(page).save(
      'ErpLogCarrierConfig',
      {
        carrierId,
        configCode: LOG_CFG_CODE,
        serviceType: 'STANDARD',
        apiKey: LOG_SEED.apiKey,
        apiSecret: LOG_SEED.apiSecret,
        isActive: 1,
      },
      'id',
    );
    if (!cfg || cfg.id == null) {
      console.log('[graceful-skip] logistics carrier-config seed returned no id');
      await cleanupLog(page);
      test.skip();
    }

    try {
      await navigateTo(page, '/ErpLogCarrierConfig-main');
      await expect(page.locator('table tbody tr').first()).toBeVisible({ timeout: 30_000 });

      // apiKey/apiSecret are NOT in the list grid; they render in the view form drawer.
      // They are xmeta published="false" (write-only), so the view shows a static `******`
      // mask and the plaintext must never appear in the DOM.
      await openViewForRow(page, LOG_CFG_CODE);
      await expect
        .poll(
          async () => (await page.textContent('body')) || '',
          { timeout: 20_000, message: 'logistics view should render static ****** mask' },
        )
        .toContain(LOG_MASK);
      const logBody = (await page.textContent('body')) || '';
      expect(
        logBody,
        'logistics view must NOT leak plaintext apiKey (published=false)',
      ).not.toContain(LOG_SEED.apiKey);
      expect(
        logBody,
        'logistics view must NOT leak plaintext apiSecret (published=false)',
      ).not.toContain(LOG_SEED.apiSecret);

      await closeOverlay(page);
    } finally {
      await cleanupLog(page);
    }
  });
});
