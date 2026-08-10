import {
  test,
  expect,
  expectActionDenied,
  loginAsRole,
  callMutation,
  ENFORCEMENT_ERROR_CODES,
} from './_helper';

/**
 * E1.1 mfg 域 enforcement 覆盖闭环 Proof（plan 2026-08-10-0739-1 Phase 2，permissions-enforcement mission）。
 *
 * 灰度纪律：admin 兜底绿 → 授权角色正向（生产主管 role-mfg-lead）→ restricted 负向。
 *
 * **mfg 域 E1.1 动作集**（全部 enforcement 闭环）：
 *   - ErpMfgWorkOrder.start / close / cancel（Java @BizMutation → field.auth 非空 → restricted 必被拒）✅
 *   - ErpMfgSubcontractOrder.approve（xbiz `<mutation>` 内补 `<auth permissions="ErpMfgSubcontractOrder:approve"/>`
 *     → field.auth 非空 → enforcement 进入 → 生产主管通过 + restricted 拒）✅
 *
 * **Phase 1 根因裁决**：ErpMfgSubcontractOrder.approve 经 approval-support.xbiz 注入，保留层
 * ErpMfgSubcontractOrder.xbiz 声明 `<mutation name="approve">` 但无 `<auth>` 子元素 →
 * `BizModelToGraphQLDefinition.java:80` `field.setAuth(null)` → `isAllowAccess(null)=true` → bypass。
 * **修复方案（已落地）**：保留层 xbiz `<mutation>` 内补 `<auth permissions="ErpMfgSubcontractOrder:approve"/>`。
 *
 * **保护区域裁决（auth plan-first）**：xbiz enforcement 绑定触 auth plan-first 保护区域，计划审计已通过
 * （Draft Review Record: accept），审查者可用性 = subagent，按 ai-autonomy-policy plan-first 规则
 * （计划审计 + 必需证据齐备即允许实施）。`<auth>` 已补齐，subcontract.approve 翻为 active（去 fixme）。
 */

const DUMMY_ID = 999999;
const DUMMY_STR = '999999';

interface MfgAction {
  entity: string;
  action: string;
  args: Record<string, unknown>;
  authorizedRole: string;
}

const MFG_ACTIONS: MfgAction[] = [
  {
    entity: 'ErpMfgWorkOrder',
    action: 'start',
    args: { workOrderId: DUMMY_ID },
    authorizedRole: '生产主管',
  },
  {
    entity: 'ErpMfgWorkOrder',
    action: 'close',
    args: { workOrderId: DUMMY_ID },
    authorizedRole: '生产主管',
  },
  {
    entity: 'ErpMfgWorkOrder',
    action: 'cancel',
    args: { workOrderId: DUMMY_ID },
    authorizedRole: '生产主管',
  },
];

const MFG_SUBCONTRACT_APPROVE: MfgAction[] = [
  {
    entity: 'ErpMfgSubcontractOrder',
    action: 'approve',
    args: { id: DUMMY_STR },
    authorizedRole: '生产主管',
  },
];

test.describe('E1.1 mfg: enforcement closed-loop (admin→auth-positive→restricted-negative)', () => {
  test('restricted denied for mfg Java-denied actions (WorkOrder start/close/cancel)', async ({ page }) => {
    await loginAsRole(page, 'restricted');
    await page.goto('/#/ErpMfgWorkOrder-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    for (const a of MFG_ACTIONS) {
      const rej = await callMutation(page, a.entity, a.action, a.args, 'id');
      expectActionDenied(rej, {
        errorCode: ENFORCEMENT_ERROR_CODES.NO_PERMISSION,
        token: '没有访问权限',
      });
    }
  });

  test('authorized role (生产主管) enforcement passes for Java-denied actions', async ({ page }) => {
    await loginAsRole(page, '生产主管');
    await page.goto('/#/ErpMfgWorkOrder-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    for (const a of MFG_ACTIONS) {
      const rej = await callMutation(page, a.entity, a.action, a.args, 'id');
      const errorCode = rej.json?.extensions?.['nop-error-code'];
      expect(
        errorCode,
        `${a.entity}.${a.action} for 生产主管: expected enforcement pass (not no-permission), got ${errorCode}`,
      ).not.toBe(ENFORCEMENT_ERROR_CODES.NO_PERMISSION);
    }
  });

  // ErpMfgSubcontractOrder.approve：xbiz <auth> 已补齐（plan-first 审计通过），enforcement 现闭环。
  test('restricted denied for ErpMfgSubcontractOrder.approve (xbiz <auth> closed)', async ({ page }) => {
    await loginAsRole(page, 'restricted');
    await page.goto('/#/ErpMfgSubcontractOrder-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    for (const a of MFG_SUBCONTRACT_APPROVE) {
      const rej = await callMutation(page, a.entity, a.action, a.args, 'id');
      expectActionDenied(rej, {
        errorCode: ENFORCEMENT_ERROR_CODES.NO_PERMISSION,
        token: '没有访问权限',
      });
    }
  });

  test('authorized role (生产主管) enforcement passes for ErpMfgSubcontractOrder.approve', async ({ page }) => {
    await loginAsRole(page, '生产主管');
    await page.goto('/#/ErpMfgSubcontractOrder-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    for (const a of MFG_SUBCONTRACT_APPROVE) {
      const rej = await callMutation(page, a.entity, a.action, a.args, 'id');
      const errorCode = rej.json?.extensions?.['nop-error-code'];
      expect(
        errorCode,
        `${a.entity}.${a.action} for 生产主管: expected enforcement pass (not no-permission), got ${errorCode}`,
      ).not.toBe(ENFORCEMENT_ERROR_CODES.NO_PERMISSION);
    }
  });
});
