import {
  test,
  expect,
  expectActionDenied,
  loginAsRole,
  callMutation,
  ENFORCEMENT_ERROR_CODES,
} from './_helper';

/**
 * E1.1 inventory 域 enforcement 覆盖闭环 Proof（plan 2026-08-10-0739-1 Phase 2，permissions-enforcement mission）。
 *
 * 灰度纪律：admin 兜底绿 → 授权角色正向（库管员 role-inventory）→ restricted 负向。
 *
 * **inv 域 E1.1 动作集**（全部 Java @BizMutation → restricted 必被拒；本域无 bypassed 项）：
 *   - ErpInvStockMove.confirm（Java @BizMutation）
 *   - ErpInvLandedCost.approve（Java @BizMutation，P2.4 denied 锚点——approve 动作中唯一被 enforcement 覆盖的）
 *
 * landedCost.approve 锚点说明：同为 approve 命名但由 Java @BizMutation 声明（非 approval-support.xbiz），
 * 故 ReflectionBizModelBuilder 恒定附加 auth → enforcement 覆盖（与 mfg/hr 的 xbiz-approve bypass 形成对照）。
 */

const DUMMY_ID = 999999;

interface InvAction {
  entity: string;
  action: string;
  args: Record<string, unknown>;
}

const INV_ACTIONS: InvAction[] = [
  {
    entity: 'ErpInvStockMove',
    action: 'confirm',
    args: { moveId: DUMMY_ID },
  },
  {
    entity: 'ErpInvLandedCost',
    action: 'approve',
    args: { id: DUMMY_ID },
  },
];

test.describe('E1.1 inventory: enforcement closed-loop (admin→auth-positive→restricted-negative)', () => {
  test('restricted denied for all inventory E1.1 actions', async ({ page }) => {
    await loginAsRole(page, 'restricted');
    await page.goto('/#/ErpInvStockMove-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    for (const a of INV_ACTIONS) {
      const rej = await callMutation(page, a.entity, a.action, a.args, 'id');
      expectActionDenied(rej, {
        errorCode: ENFORCEMENT_ERROR_CODES.NO_PERMISSION,
        token: '没有访问权限',
      });
    }
  });

  test('authorized role (库管员) enforcement passes (seed permission CAN)', async ({ page }) => {
    await loginAsRole(page, '库管员');
    await page.goto('/#/ErpInvStockMove-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    for (const a of INV_ACTIONS) {
      const rej = await callMutation(page, a.entity, a.action, a.args, 'id');
      const errorCode = rej.json?.extensions?.['nop-error-code'];
      expect(
        errorCode,
        `${a.entity}.${a.action} for 库管员: expected enforcement pass (not no-permission), got ${errorCode}`,
      ).not.toBe(ENFORCEMENT_ERROR_CODES.NO_PERMISSION);
    }
  });
});
