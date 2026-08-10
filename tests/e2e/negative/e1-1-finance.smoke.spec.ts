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
 * E1.1 finance 域 enforcement 覆盖闭环 Proof（plan 2026-08-10-0739-1 Phase 2，permissions-enforcement mission）。
 *
 * 灰度纪律：admin 兜底绿 → 授权角色正向（enforcement 通过 = 种子授权 CAN）→ restricted 负向（真拒绝）。
 *
 * **finance 域 E1.1 动作集**（全部 Java @BizMutation → field.auth 非空 → restricted 必被拒）：
 *   - ErpFinVoucher.post / reverse（返 Long，须 callMutationScalar 无子选择）
 *   - ErpFinAccountingPeriod.closePeriod（roles=财务员）/ reverseClose（roles=管理员）
 *   - ErpFinBadDebt.writeOff（roles=财务员）/ reverseApprove（roles=管理员）[P2.4 denied 锚点]
 *
 * **根因裁决消费**（Phase 1）：Java 方法经 ReflectionBizModelBuilder:330-336 恒定附加非空 ActionAuthMeta，
 * enforcement 路径必达——本域无 bypassed 项。
 *
 * **测试结构**：restricted 负向 = 单测全动作循环（单次登录）；授权角色正向 = 每角色独立 test（fresh page，
 * 避免单 page 角色切换导致 SPA 不重定向到 login 表单——见 Navigation.login usernameInput 等待）。
 */

const DUMMY_ID = 999999;
const DUMMY_STR = '999999';

interface FinAction {
  entity: string;
  action: string;
  args: Record<string, unknown>;
  authorizedRole: string; // action-auth.xml FNPT roles= 声明的授权角色（业务 roleId 字面）
  scalarReturn?: boolean; // true = 返回 Long/scalar，须用 callMutationScalar（无子选择）
}

const FIN_ACTIONS: FinAction[] = [
  {
    entity: 'ErpFinVoucher',
    action: 'post',
    args: { event: {} },
    authorizedRole: '财务员',
    scalarReturn: true,
  },
  {
    entity: 'ErpFinVoucher',
    action: 'reverse',
    args: { billHeadCode: DUMMY_STR, businessType: 'PURCHASE_INPUT' },
    authorizedRole: '财务员',
    scalarReturn: true,
  },
  {
    entity: 'ErpFinAccountingPeriod',
    action: 'closePeriod',
    args: { periodId: DUMMY_ID },
    authorizedRole: '财务员',
  },
  {
    entity: 'ErpFinAccountingPeriod',
    action: 'reverseClose',
    args: { periodId: DUMMY_ID },
    authorizedRole: '管理员',
  },
  {
    entity: 'ErpFinBadDebt',
    action: 'writeOff',
    args: { arApItemId: DUMMY_ID, reason: 'e1-1-fin-proof' },
    authorizedRole: '财务员',
  },
  {
    entity: 'ErpFinBadDebt',
    action: 'reverseApprove',
    args: { id: DUMMY_ID },
    authorizedRole: '管理员',
  },
];

async function callFin(page: Page, a: FinAction) {
  return a.scalarReturn
    ? callMutationScalar(page, a.entity, a.action, a.args)
    : callMutation(page, a.entity, a.action, a.args, 'id');
}

test.describe('E1.1 finance: enforcement closed-loop (admin→auth-positive→restricted-negative)', () => {
  test('restricted denied for all finance E1.1 actions', async ({ page }) => {
    await loginAsRole(page, 'restricted');
    await page.goto('/#/ErpFinVoucher-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    for (const a of FIN_ACTIONS) {
      const rej = await callFin(page, a);
      expectActionDenied(rej, {
        errorCode: ENFORCEMENT_ERROR_CODES.NO_PERMISSION,
        token: '没有访问权限',
      });
    }
  });

  // 授权角色正向：每角色独立 test（fresh page），断言该角色所有动作 enforcement 层放行（不返回 no-permission）。
  // 业务逻辑层可能因哨兵 id 返回 not-found / invalid-status，但 enforcement 通过 = 种子授权证明。
  const actionsByRole = new Map<string, FinAction[]>();
  for (const a of FIN_ACTIONS) {
    const list = actionsByRole.get(a.authorizedRole) ?? [];
    list.push(a);
    actionsByRole.set(a.authorizedRole, list);
  }

  for (const [role, actions] of actionsByRole) {
    test(`authorized role ${role} enforcement passes (seed permission CAN)`, async ({ page }) => {
      await loginAsRole(page, role);
      await page.goto('/#/ErpFinVoucher-main', { waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(1500);

      for (const a of actions) {
        const rej = await callFin(page, a);
        const errorCode = rej.json?.extensions?.['nop-error-code'];
        expect(
          errorCode,
          `${a.entity}.${a.action} for ${role}: expected enforcement pass (not no-permission), got ${errorCode}`,
        ).not.toBe(ENFORCEMENT_ERROR_CODES.NO_PERMISSION);
      }
    });
  }
});
