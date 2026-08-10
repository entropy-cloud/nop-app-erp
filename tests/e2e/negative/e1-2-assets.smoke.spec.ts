import {
  test,
  expect,
  expectActionDenied,
  loginAsRole,
  callMutation,
  ENFORCEMENT_ERROR_CODES,
} from './_helper';

/**
 * E1.2 assets 域 enforcement 闭环 Proof（plan 2026-08-10-1404-1 Phase 3）。
 * ast 6 实体 × approve+reverseApprove = 12 动作。
 * FNPT: approve=资产管理员/管理员（ErpAstDisposal 既有 + 本计划补齐 5 实体）/ reverseApprove=管理员。
 */
const DUMMY_ID = 999999;
const AST_ENTITIES = ['ErpAstDisposal', 'ErpAstValueAdjustment', 'ErpAstSplit', 'ErpAstMerge', 'ErpAstMovement', 'ErpAstAssetCapitalization'];
const AST_ACTIONS = AST_ENTITIES.flatMap((e) => [
  { entity: e, action: 'approve', authorizedRole: '资产管理员' },
  { entity: e, action: 'reverseApprove', authorizedRole: '管理员' },
]);

test.describe('E1.2 assets: enforcement closed-loop', () => {
  test('restricted denied for all ast approve/reverseApprove', async ({ page }) => {
    await loginAsRole(page, 'restricted');
    await page.goto('/#/ErpAstDisposal-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    for (const a of AST_ACTIONS) {
      const rej = await callMutation(page, a.entity, a.action, { id: DUMMY_ID }, 'id');
      expectActionDenied(rej, { errorCode: ENFORCEMENT_ERROR_CODES.NO_PERMISSION, token: '没有访问权限' });
    }
  });

  // 资产管理员正向（approve 授权角色）
  test('authorized role 资产管理员 enforcement passes (approve)', async ({ page }) => {
    await loginAsRole(page, '资产管理员');
    await page.goto('/#/ErpAstDisposal-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    for (const a of AST_ACTIONS.filter((x) => x.action === 'approve')) {
      const rej = await callMutation(page, a.entity, a.action, { id: DUMMY_ID }, 'id');
      expect(rej.json?.extensions?.['nop-error-code'], `${a.entity}.${a.action}`).not.toBe(ENFORCEMENT_ERROR_CODES.NO_PERMISSION);
    }
  });

  // 管理员正向（reverseApprove 授权角色）
  test('authorized role 管理员 enforcement passes (reverseApprove)', async ({ page }) => {
    await loginAsRole(page, '管理员');
    await page.goto('/#/ErpAstDisposal-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    for (const a of AST_ACTIONS.filter((x) => x.action === 'reverseApprove')) {
      const rej = await callMutation(page, a.entity, a.action, { id: DUMMY_ID }, 'id');
      expect(rej.json?.extensions?.['nop-error-code'], `${a.entity}.${a.action}`).not.toBe(ENFORCEMENT_ERROR_CODES.NO_PERMISSION);
    }
  });
});
