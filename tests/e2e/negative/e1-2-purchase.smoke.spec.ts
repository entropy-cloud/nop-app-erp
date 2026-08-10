import {
  test,
  expect,
  expectActionDenied,
  loginAsRole,
  callMutation,
  ENFORCEMENT_ERROR_CODES,
} from './_helper';
import type { Page } from '@playwright/test';

/**
 * E1.2 purchase 域 enforcement 覆盖闭环 Proof（plan 2026-08-10-1404-1 Phase 3）。
 *
 * Phase 1 闭环的 pur 域 approve/reverseApprove xbiz `<auth>`（8 实体 × 2 = 16 动作）。
 * FNPT 声明：approve=审核人 / reverseApprove=管理员（6 实体 baseline）+ Quotation/Rfq（本计划补齐）。
 *
 * 灰度纪律：admin 兜底 → 授权角色正向 → restricted 负向。
 * restricted 负向全 16 动作循环（单次登录）；授权角色正向按 role 分组（审核人 / 管理员）。
 */

const DUMMY_ID = 999999;

interface PurAction {
  entity: string;
  action: string;
  authorizedRole: string;
}

const PUR_ACTIONS: PurAction[] = [
  { entity: 'ErpPurRequisition', action: 'approve', authorizedRole: '审核人' },
  { entity: 'ErpPurRequisition', action: 'reverseApprove', authorizedRole: '管理员' },
  { entity: 'ErpPurOrder', action: 'approve', authorizedRole: '审核人' },
  { entity: 'ErpPurOrder', action: 'reverseApprove', authorizedRole: '管理员' },
  { entity: 'ErpPurReceive', action: 'approve', authorizedRole: '审核人' },
  { entity: 'ErpPurReceive', action: 'reverseApprove', authorizedRole: '管理员' },
  { entity: 'ErpPurInvoice', action: 'approve', authorizedRole: '审核人' },
  { entity: 'ErpPurInvoice', action: 'reverseApprove', authorizedRole: '管理员' },
  { entity: 'ErpPurPayment', action: 'approve', authorizedRole: '审核人' },
  { entity: 'ErpPurPayment', action: 'reverseApprove', authorizedRole: '管理员' },
  { entity: 'ErpPurReturn', action: 'approve', authorizedRole: '审核人' },
  { entity: 'ErpPurReturn', action: 'reverseApprove', authorizedRole: '管理员' },
  { entity: 'ErpPurQuotation', action: 'approve', authorizedRole: '审核人' },
  { entity: 'ErpPurQuotation', action: 'reverseApprove', authorizedRole: '管理员' },
  { entity: 'ErpPurRfq', action: 'approve', authorizedRole: '审核人' },
  { entity: 'ErpPurRfq', action: 'reverseApprove', authorizedRole: '管理员' },
];

test.describe('E1.2 purchase: enforcement closed-loop', () => {
  test('restricted denied for all pur approve/reverseApprove', async ({ page }) => {
    await loginAsRole(page, 'restricted');
    await page.goto('/#/ErpPurOrder-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    for (const a of PUR_ACTIONS) {
      const rej = await callMutation(page, a.entity, a.action, { id: DUMMY_ID }, 'id');
      expectActionDenied(rej, {
        errorCode: ENFORCEMENT_ERROR_CODES.NO_PERMISSION,
        token: '没有访问权限',
      });
    }
  });

  const actionsByRole = new Map<string, PurAction[]>();
  for (const a of PUR_ACTIONS) {
    const list = actionsByRole.get(a.authorizedRole) ?? [];
    list.push(a);
    actionsByRole.set(a.authorizedRole, list);
  }

  for (const [role, actions] of actionsByRole) {
    test(`authorized role ${role} enforcement passes`, async ({ page }) => {
      await loginAsRole(page, role);
      await page.goto('/#/ErpPurOrder-main', { waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(1500);

      for (const a of actions) {
        const rej = await callMutation(page, a.entity, a.action, { id: DUMMY_ID }, 'id');
        const errorCode = rej.json?.extensions?.['nop-error-code'];
        expect(
          errorCode,
          `${a.entity}.${a.action} for ${role}: expected enforcement pass, got ${errorCode}`,
        ).not.toBe(ENFORCEMENT_ERROR_CODES.NO_PERMISSION);
      }
    });
  }
});
