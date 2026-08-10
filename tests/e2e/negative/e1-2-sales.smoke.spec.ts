import {
  test,
  expect,
  expectActionDenied,
  loginAsRole,
  callMutation,
  ENFORCEMENT_ERROR_CODES,
} from './_helper';

/**
 * E1.2 sales 域 enforcement 闭环 Proof（plan 2026-08-10-1404-1 Phase 3）。
 * sal 7 实体 × approve+reverseApprove = 14 动作。FNPT: approve=审核人 / reverseApprove=管理员。
 */
const DUMMY_ID = 999999;
const SAL_ENTITIES = ['ErpSalQuotation', 'ErpSalContract', 'ErpSalOrder', 'ErpSalDelivery', 'ErpSalInvoice', 'ErpSalReceipt', 'ErpSalReturn'];
const SAL_ACTIONS = SAL_ENTITIES.flatMap((e) => [
  { entity: e, action: 'approve', authorizedRole: '审核人' },
  { entity: e, action: 'reverseApprove', authorizedRole: '管理员' },
]);

test.describe('E1.2 sales: enforcement closed-loop', () => {
  test('restricted denied for all sal approve/reverseApprove', async ({ page }) => {
    await loginAsRole(page, 'restricted');
    await page.goto('/#/ErpSalOrder-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    for (const a of SAL_ACTIONS) {
      const rej = await callMutation(page, a.entity, a.action, { id: DUMMY_ID }, 'id');
      expectActionDenied(rej, { errorCode: ENFORCEMENT_ERROR_CODES.NO_PERMISSION, token: '没有访问权限' });
    }
  });

  const byRole = new Map<string, typeof SAL_ACTIONS>();
  for (const a of SAL_ACTIONS) {
    const list = byRole.get(a.authorizedRole) ?? [];
    list.push(a);
    byRole.set(a.authorizedRole, list);
  }
  for (const [role, actions] of byRole) {
    test(`authorized role ${role} enforcement passes`, async ({ page }) => {
      await loginAsRole(page, role);
      await page.goto('/#/ErpSalOrder-main', { waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(1500);
      for (const a of actions) {
        const rej = await callMutation(page, a.entity, a.action, { id: DUMMY_ID }, 'id');
        expect(rej.json?.extensions?.['nop-error-code'], `${a.entity}.${a.action}`).not.toBe(ENFORCEMENT_ERROR_CODES.NO_PERMISSION);
      }
    });
  }
});
