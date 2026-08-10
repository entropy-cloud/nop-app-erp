import {
  test,
  expect,
  expectActionDenied,
  loginAsRole,
  callMutation,
  callMutationScalar,
  ENFORCEMENT_ERROR_CODES,
} from './_helper';
import type { Page } from '@playwright/test';

/**
 * E1.1 b2b 域 enforcement 覆盖闭环 Proof（plan 2026-08-10-0739-1 Phase 2，permissions-enforcement mission）。
 *
 * 灰度纪律：admin 兜底绿 → 授权角色正向 → restricted 负向。
 *
 * **b2b 域 E1.1 动作集**（全部 Java @BizMutation → restricted 必被拒；本域无 bypassed 项）：
 *   - ErpB2bEdiDoc.markSent(B2B 对账员) / cancel(B2B 管理员) / markAcknowledged(对账员) / markError(管理员) / retry(管理员) / archive(管理员)
 *   - ErpB2bAsn.handleInboundWebhook(B2B 管理员，返 Long) / matchPurchaseOrder(对账员) / createReceiveFromAsn(对账员) / retryMatch(对账员)
 *
 * 授权角色双账号：B2B 管理员（EDI 文档生命周期 + webhook）+ B2B 对账员（ASN 对账匹配）——按各动作 FNPT roles 声明。
 * 测试结构同 finance：restricted 单测全动作；授权角色每角色独立 test（fresh page）。
 */

const DUMMY_ID = 999999;

interface B2bAction {
  entity: string;
  action: string;
  args: Record<string, unknown>;
  authorizedRole: string;
  scalarReturn?: boolean; // handleInboundWebhook 返 Long
}

const B2B_ACTIONS: B2bAction[] = [
  {
    entity: 'ErpB2bEdiDoc',
    action: 'markSent',
    args: { ediDocId: DUMMY_ID },
    authorizedRole: 'B2B 对账员',
  },
  {
    entity: 'ErpB2bEdiDoc',
    action: 'cancel',
    args: { ediDocId: DUMMY_ID },
    authorizedRole: 'B2B 管理员',
  },
  {
    entity: 'ErpB2bEdiDoc',
    action: 'markAcknowledged',
    args: { ediDocId: DUMMY_ID },
    authorizedRole: 'B2B 对账员',
  },
  {
    entity: 'ErpB2bEdiDoc',
    action: 'markError',
    args: { ediDocId: DUMMY_ID, error: 'e1-1-b2b-proof' },
    authorizedRole: 'B2B 管理员',
  },
  {
    entity: 'ErpB2bEdiDoc',
    action: 'retry',
    args: { ediDocId: DUMMY_ID },
    authorizedRole: 'B2B 管理员',
  },
  {
    entity: 'ErpB2bEdiDoc',
    action: 'archive',
    args: { ediDocId: DUMMY_ID },
    authorizedRole: 'B2B 管理员',
  },
  {
    entity: 'ErpB2bAsn',
    action: 'handleInboundWebhook',
    args: {
      formatCode: 'X12',
      partnerCode: 'probe',
      signature: 'probe',
      eventId: 'probe',
      payload: '{}',
    },
    authorizedRole: 'B2B 管理员',
    scalarReturn: true,
  },
  {
    entity: 'ErpB2bAsn',
    action: 'matchPurchaseOrder',
    args: { asnId: DUMMY_ID },
    authorizedRole: 'B2B 对账员',
  },
  {
    entity: 'ErpB2bAsn',
    action: 'createReceiveFromAsn',
    args: { asnId: DUMMY_ID },
    authorizedRole: 'B2B 对账员',
  },
  {
    entity: 'ErpB2bAsn',
    action: 'retryMatch',
    args: { asnId: DUMMY_ID },
    authorizedRole: 'B2B 对账员',
  },
];

async function callB2b(page: Page, a: B2bAction) {
  return a.scalarReturn
    ? callMutationScalar(page, a.entity, a.action, a.args)
    : callMutation(page, a.entity, a.action, a.args, 'id');
}

test.describe('E1.1 b2b: enforcement closed-loop (admin→auth-positive→restricted-negative)', () => {
  test('restricted denied for all b2b E1.1 actions', async ({ page }) => {
    await loginAsRole(page, 'restricted');
    await page.goto('/#/ErpB2bEdiDoc-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    for (const a of B2B_ACTIONS) {
      const rej = await callB2b(page, a);
      expectActionDenied(rej, {
        errorCode: ENFORCEMENT_ERROR_CODES.NO_PERMISSION,
        token: '没有访问权限',
      });
    }
  });

  const actionsByRole = new Map<string, B2bAction[]>();
  for (const a of B2B_ACTIONS) {
    const list = actionsByRole.get(a.authorizedRole) ?? [];
    list.push(a);
    actionsByRole.set(a.authorizedRole, list);
  }

  for (const [role, actions] of actionsByRole) {
    test(`authorized role ${role} enforcement passes (seed permission CAN)`, async ({ page }) => {
      await loginAsRole(page, role);
      await page.goto('/#/ErpB2bEdiDoc-main', { waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(1500);

      for (const a of actions) {
        const rej = await callB2b(page, a);
        const errorCode = rej.json?.extensions?.['nop-error-code'];
        expect(
          errorCode,
          `${a.entity}.${a.action} for ${role}: expected enforcement pass (not no-permission), got ${errorCode}`,
        ).not.toBe(ENFORCEMENT_ERROR_CODES.NO_PERMISSION);
      }
    });
  }
});
