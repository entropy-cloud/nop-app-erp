import {
  test,
  expect,
  expectActionDenied,
  loginAsRole,
  callMutation,
  ENFORCEMENT_ERROR_CODES,
} from './_helper';

/**
 * E1.2 residual + extended domains enforcement 闭环 Proof（plan 2026-08-10-1404-1 Phase 3）。
 *
 * 覆盖 Phase 1 扫描终态清单中 E1.1 域残留 bypassed（inv/fin/mfg/hr）+ E1.2 新增域（prj/qa/mnt）
 * 的 approve/reverseApprove xbiz `<auth>` 闭环动作。
 *
 * FNPT roles: approve = 域主角色 / reverseApprove = 管理员（统一范式）。
 */
const DUMMY_ID = 999999;

interface DomainAction {
  entity: string;
  action: string;
  authorizedRole: string;
  domain: string;
}

const ACTIONS: DomainAction[] = [
  // inv residual: ErpInvCostAdjust approve=库管员 / reverseApprove=管理员
  { entity: 'ErpInvCostAdjust', action: 'approve', authorizedRole: '库管员', domain: 'inv' },
  { entity: 'ErpInvCostAdjust', action: 'reverseApprove', authorizedRole: '管理员', domain: 'inv' },
  // fin residual: ErpFinEmployeeAdvance / ErpFinExpenseClaim approve=财务员 / reverseApprove=管理员
  { entity: 'ErpFinEmployeeAdvance', action: 'approve', authorizedRole: '财务员', domain: 'fin' },
  { entity: 'ErpFinEmployeeAdvance', action: 'reverseApprove', authorizedRole: '管理员', domain: 'fin' },
  { entity: 'ErpFinExpenseClaim', action: 'approve', authorizedRole: '财务员', domain: 'fin' },
  { entity: 'ErpFinExpenseClaim', action: 'reverseApprove', authorizedRole: '管理员', domain: 'fin' },
  // mfg residual: MaterialIssue / WorkOrder approve=生产主管 / SubcontractOrder+WorkOrder reverseApprove=管理员
  { entity: 'ErpMfgMaterialIssue', action: 'approve', authorizedRole: '生产主管', domain: 'mfg' },
  { entity: 'ErpMfgMaterialIssue', action: 'reverseApprove', authorizedRole: '管理员', domain: 'mfg' },
  { entity: 'ErpMfgSubcontractOrder', action: 'reverseApprove', authorizedRole: '管理员', domain: 'mfg' },
  { entity: 'ErpMfgWorkOrder', action: 'approve', authorizedRole: '生产主管', domain: 'mfg' },
  { entity: 'ErpMfgWorkOrder', action: 'reverseApprove', authorizedRole: '管理员', domain: 'mfg' },
  // hr residual: Salary.reverseApprove=管理员 (approve already E1.1)
  { entity: 'ErpHrSalary', action: 'reverseApprove', authorizedRole: '管理员', domain: 'hr' },
  // prj: Billing/Budget/CostCollection approve=项目经理 / reverseApprove=管理员
  ...['ErpPrjBilling', 'ErpPrjBudget', 'ErpPrjCostCollection'].flatMap((e) => [
    { entity: e, action: 'approve', authorizedRole: '项目经理', domain: 'prj' },
    { entity: e, action: 'reverseApprove', authorizedRole: '管理员', domain: 'prj' },
  ]),
  // qa: 5 entities approve=质量主管 / reverseApprove=管理员
  ...['ErpQaCalibration', 'ErpQaInspection', 'ErpQaRecall', 'ErpQaReview', 'ErpQaSpcChart'].flatMap((e) => [
    { entity: e, action: 'approve', authorizedRole: '质量主管', domain: 'qa' },
    { entity: e, action: 'reverseApprove', authorizedRole: '管理员', domain: 'qa' },
  ]),
  // mnt: Calibration/Request approve=维护主管 / reverseApprove=管理员
  ...['ErpMntCalibration', 'ErpMntRequest'].flatMap((e) => [
    { entity: e, action: 'approve', authorizedRole: '维护主管', domain: 'mnt' },
    { entity: e, action: 'reverseApprove', authorizedRole: '管理员', domain: 'mnt' },
  ]),
];

test.describe('E1.2 residual+extended: enforcement closed-loop', () => {
  test('restricted denied for all residual+extended approve/reverseApprove', async ({ page }) => {
    await loginAsRole(page, 'restricted');
    await page.goto('/', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    for (const a of ACTIONS) {
      const rej = await callMutation(page, a.entity, a.action, { id: DUMMY_ID }, 'id');
      expectActionDenied(rej, { errorCode: ENFORCEMENT_ERROR_CODES.NO_PERMISSION, token: '没有访问权限' });
    }
  });

  // authorized role positive: group by role, one test per role
  const byRole = new Map<string, DomainAction[]>();
  for (const a of ACTIONS) {
    const list = byRole.get(a.authorizedRole) ?? [];
    list.push(a);
    byRole.set(a.authorizedRole, list);
  }
  for (const [role, actions] of byRole) {
    test(`authorized role ${role} enforcement passes (${actions.length} actions)`, async ({ page }) => {
      await loginAsRole(page, role);
      await page.goto('/', { waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(1500);
      for (const a of actions) {
        const rej = await callMutation(page, a.entity, a.action, { id: DUMMY_ID }, 'id');
        expect(rej.json?.extensions?.['nop-error-code'], `${a.entity}.${a.action} for ${role}`).not.toBe(ENFORCEMENT_ERROR_CODES.NO_PERMISSION);
      }
    });
  }
});
