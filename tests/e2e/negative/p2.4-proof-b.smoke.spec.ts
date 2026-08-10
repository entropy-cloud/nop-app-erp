import {
  test,
  expect,
  expectActionDenied,
  loginAsRole,
  callMutation,
  ENFORCEMENT_ERROR_CODES,
} from './_helper';

/**
 * P2.4 Proof B（enforcement 拒绝形状运行时确认，fold P2.3 Deferred）。
 *
 * 用 role-restricted（绑平台 user 角色，无敏感 FNPT）登录，调一个 P1.4 已声明 FNPT 的敏感动作
 * （ErpFinBadDebt__writeOff，roles=财务员），经 expectActionDenied + ENFORCEMENT_ERROR_CODES.NO_PERMISSION
 * + token「没有访问权限」断言运行时拒绝形状与 P2.3 静态表征一致：
 *   (1) errors 数组存在 + message 含「没有访问权限」；
 *   (2) extensions["nop-error-code"] = nop.err.auth.no-permission；
 *   (3) HTTP 200（GraphQLWebService 硬编码，非 403）。
 *
 * **运行时确认结果（P2.4 dry-run，2026-08-10）**：形状与静态表征完全一致——
 * `{data:null, errors:[{message:"没有访问权限"}], extensions:{"nop-error-code":"nop.err.auth.no-permission","nop-status":-1}}`。
 * ENFORCEMENT_ERROR_CODES 常量值收敛无需调整。
 */
test.describe('P2.4 Proof B: enforcement rejection shape (role-restricted)', () => {
  test('FNPT-declared action denied for role-restricted with expected shape', async ({ page }) => {
    await loginAsRole(page, 'restricted');
    await page.goto('/#/ErpFinBadDebt-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    const rej = await callMutation(
      page,
      'ErpFinBadDebt',
      'writeOff',
      { arApItemId: '999999', reason: 'p2.4-proof-b-deny-shape' },
      'id',
    );

    const errors = expectActionDenied(rej, {
      errorCode: ENFORCEMENT_ERROR_CODES.NO_PERMISSION,
      token: '没有访问权限',
    });
    expect(errors.length, 'at least one error entry').toBeGreaterThan(0);
  });
});
