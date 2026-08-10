import {
  test,
  expect,
  expectActionDenied,
  loginAsRole,
  callMutation,
  ENFORCEMENT_ERROR_CODES,
} from './_helper';

/**
 * E1.1 hr 域 enforcement 覆盖闭环 Proof（plan 2026-08-10-0739-1 Phase 2，permissions-enforcement mission）。
 *
 * 灰度纪律：admin 兜底绿 → 授权角色正向 → restricted 负向。
 *
 * **hr 域 E1.1 动作集**（全部 enforcement 闭环）：
 *   - ErpHrSalary.markPaid / voidSalary（Java @BizMutation → restricted 必被拒）[P2.4 denied 锚点] ✅
 *   - ErpHrLeaveRequest.approve（Java @BizMutation → restricted 必被拒）✅
 *   - ErpHrSalary.approve（xbiz `<mutation>` 内补 `<auth permissions="ErpHrSalary:approve"/>`
 *     → field.auth 非空 → enforcement 进入 → 薪酬审批人通过 + restricted 拒）✅
 *
 * 授权角色双账号：薪酬审批人（role-hr-salary，薪酬机密动作）+ HR 专员（role-hr，休假审批）。
 *
 * **Phase 1 根因裁决**：ErpHrSalary.approve 经保留层 ErpHrSalary.xbiz 声明 inline DIRECT `<source>`
 * （状态守卫 + approveStatus 翻转），但无 `<auth>` 子元素 → field.auth=null → bypass。
 * **修复方案（已落地）**：保留层 xbiz `<mutation name="approve">` 内补 `<auth permissions="ErpHrSalary:approve"/>`。
 *
 * **保护区域裁决（auth plan-first）**：xbiz enforcement 绑定触 auth plan-first 保护区域，计划审计已通过
 * （Draft Review Record: accept），审查者可用性 = subagent，按 ai-autonomy-policy plan-first 规则
 * （计划审计 + 必需证据齐备即允许实施）。`<auth>` 已补齐，salary.approve 翻为 active（去 fixme）。
 *
 * 测试结构：restricted 单测全动作；授权角色每角色独立 test（fresh page）。
 */

const DUMMY_ID = 999999;
const DUMMY_STR = '999999';

interface HrAction {
  entity: string;
  action: string;
  args: Record<string, unknown>;
  authorizedRole: string;
}

const HR_ACTIONS: HrAction[] = [
  {
    entity: 'ErpHrSalary',
    action: 'markPaid',
    args: { salaryId: DUMMY_ID },
    authorizedRole: '薪酬审批人',
  },
  {
    entity: 'ErpHrSalary',
    action: 'voidSalary',
    args: { salaryId: DUMMY_ID },
    authorizedRole: '薪酬审批人',
  },
  {
    entity: 'ErpHrLeaveRequest',
    action: 'approve',
    args: { id: DUMMY_STR },
    authorizedRole: 'HR 专员',
  },
];

const HR_SALARY_APPROVE: HrAction[] = [
  {
    entity: 'ErpHrSalary',
    action: 'approve',
    args: { id: DUMMY_STR },
    authorizedRole: '薪酬审批人',
  },
];

test.describe('E1.1 hr: enforcement closed-loop (admin→auth-positive→restricted-negative)', () => {
  test('restricted denied for hr Java-denied actions (markPaid/voidSalary/leave.approve)', async ({ page }) => {
    await loginAsRole(page, 'restricted');
    await page.goto('/#/ErpHrSalary-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    for (const a of HR_ACTIONS) {
      const rej = await callMutation(page, a.entity, a.action, a.args, 'id');
      expectActionDenied(rej, {
        errorCode: ENFORCEMENT_ERROR_CODES.NO_PERMISSION,
        token: '没有访问权限',
      });
    }
  });

  const actionsByRole = new Map<string, HrAction[]>();
  for (const a of HR_ACTIONS) {
    const list = actionsByRole.get(a.authorizedRole) ?? [];
    list.push(a);
    actionsByRole.set(a.authorizedRole, list);
  }

  for (const [role, actions] of actionsByRole) {
    test(`authorized role ${role} enforcement passes for Java-denied actions`, async ({ page }) => {
      await loginAsRole(page, role);
      await page.goto('/#/ErpHrSalary-main', { waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(1500);

      for (const a of actions) {
        const rej = await callMutation(page, a.entity, a.action, a.args, 'id');
        const errorCode = rej.json?.extensions?.['nop-error-code'];
        expect(
          errorCode,
          `${a.entity}.${a.action} for ${role}: expected enforcement pass (not no-permission), got ${errorCode}`,
        ).not.toBe(ENFORCEMENT_ERROR_CODES.NO_PERMISSION);
      }
    });
  }

  // ErpHrSalary.approve：xbiz <auth> 已补齐（plan-first 审计通过），enforcement 现闭环。
  test('restricted denied for ErpHrSalary.approve (xbiz <auth> closed)', async ({ page }) => {
    await loginAsRole(page, 'restricted');
    await page.goto('/#/ErpHrSalary-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    for (const a of HR_SALARY_APPROVE) {
      const rej = await callMutation(page, a.entity, a.action, a.args, 'id');
      expectActionDenied(rej, {
        errorCode: ENFORCEMENT_ERROR_CODES.NO_PERMISSION,
        token: '没有访问权限',
      });
    }
  });

  test('authorized role (薪酬审批人) enforcement passes for ErpHrSalary.approve', async ({ page }) => {
    await loginAsRole(page, '薪酬审批人');
    await page.goto('/#/ErpHrSalary-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    for (const a of HR_SALARY_APPROVE) {
      const rej = await callMutation(page, a.entity, a.action, a.args, 'id');
      const errorCode = rej.json?.extensions?.['nop-error-code'];
      expect(
        errorCode,
        `${a.entity}.${a.action} for 薪酬审批人: expected enforcement pass (not no-permission), got ${errorCode}`,
      ).not.toBe(ENFORCEMENT_ERROR_CODES.NO_PERMISSION);
    }
  });
});
