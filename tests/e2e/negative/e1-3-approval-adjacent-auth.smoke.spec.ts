import {
  test,
  expect,
  expectActionDenied,
  loginAsRole,
  callMutation,
  ENFORCEMENT_ERROR_CODES,
} from './_helper';

/**
 * R1-E1-2 审批邻接 xbiz mutation（submitForApproval/reject/withdrawApproval）auth 补齐 Proof
 * （plan 2026-08-25-1956-3 Phase 3，permissions-enforcement mission）。
 *
 * **修复本体**：39 文件 × 3 mutation = 117 处保留层 xbiz `<mutation>` 首子元素补
 * `<auth permissions="<Entity>:<mapped>"/>`（E1.1 先例位置，xbiz.xdef:35）。
 * 映射基线：submitForApproval→`:mutation`（编制者轴）／reject→`:approve`（审批者轴）／
 * withdrawApproval→`:reverseApprove`（撤审轴）。映射表 =
 * `docs/testing/permissions-enforcement-approval-adjacent-xbiz-auth-mapping.md`。
 *
 * **修复前 fail-open 机理**：xbiz `<mutation>` 无 `<auth>` → field.auth=null →
 * `isAllowAccess(null)=true` 放行（E1.1 根因同款）——restricted 可无权限校验执行审批轴状态变更。
 * **修复后期望**：三动作 enforcement 必达（field.auth 非空）→ restricted 拒（NO_PERMISSION）；
 * admin（skip-check 兜底）与持权业务角色通过。
 *
 * **种子现状（抽样角色依据，映射表 §种子现状）**：
 *   - `:mutation` 全 39 实体零角色种子 → submitForApproval 对业务角色 fail-closed admin-only
 *     （映射复用既有权限点、不新增种子 = 计划 Non-Goal 边界内既有姿态）→ 该族正向主体 = admin。
 *   - `:approve` 38 实体有种子（pur/sal=审核人、prj=项目经理 等）→ reject 正向主体 = 持权业务角色。
 *   - `:reverseApprove` 38 实体种子=管理员（业务角色，非平台 admin 命名空间）→ withdrawApproval
 *     正向主体 = 业务「管理员」（真实 permissionToRoles 命中，非 skip-check）。
 *   - 例外 `ErpCsTicket`（cs）：`:approve`/`:reverseApprove` 未声明（计划预裁决）→ 任何业务角色
 *     （含审核人）均 deny-by-default = 预期正确结果（非缺陷）；admin 经 skip-check 通过。
 *
 * 抽样面：三动作族 × pur（8 文件簇代表）+ sal（7 文件簇代表）+ prj（扩展域代表）+ cs（例外实体）。
 * dummy id 999999：enforcement 通过后进入业务逻辑 → `*-not-found` 业务错误（errorCode ≠
 * NO_PERMISSION 即 enforcement pass，e1-2 同款断言范式）。
 */

const DUMMY_ID = '999999';

interface ApprovalAction {
  entity: string;
  action: 'submitForApproval' | 'reject' | 'withdrawApproval';
}

const TRIO = (entity: string): ApprovalAction[] => [
  { entity, action: 'submitForApproval' },
  { entity, action: 'reject' },
  { entity, action: 'withdrawApproval' },
];

/** 负向抽样：pur/sal 审批集 + 扩展域 prj（restricted 全拒） */
const RESTRICTED_DENIED: ApprovalAction[] = [
  ...TRIO('ErpPurOrder'),
  ...TRIO('ErpSalOrder'),
  ...TRIO('ErpPrjBudget'),
];

/** cs 例外实体：未声明权限点 → 一切业务角色 deny-by-default（预期正确） */
const CS_TICKET_TRIO = TRIO('ErpCsTicket');

/** 正向抽样：admin（skip-check）三动作族全覆盖 */
const ADMIN_PASS: ApprovalAction[] = [
  ...TRIO('ErpPurOrder'),
  ...TRIO('ErpSalOrder'),
  ...TRIO('ErpPrjBudget'),
];

/** 正向抽样：持权业务角色 reject（:approve 种子） */
const REJECT_PASS: Array<{ entity: string; role: string }> = [
  { entity: 'ErpPurOrder', role: '审核人' },
  { entity: 'ErpSalOrder', role: '审核人' },
  { entity: 'ErpPrjBudget', role: '项目经理' },
];

/** 正向抽样：持权业务角色 withdrawApproval（:reverseApprove 种子=管理员） */
const WITHDRAW_PASS: Array<{ entity: string; role: string }> = [
  { entity: 'ErpPurOrder', role: '管理员' },
  { entity: 'ErpSalOrder', role: '管理员' },
  { entity: 'ErpPrjBudget', role: '管理员' },
];

test.describe('R1-E1-2: approval-adjacent xbiz auth backfill (117 处 enforcement 闭环 Proof)', () => {
  test('restricted denied for pur/sal/prj approval-adjacent actions (fail-open closed)', async ({ page }) => {
    await loginAsRole(page, 'restricted');
    await page.goto('/#/ErpPurOrder-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    for (const a of RESTRICTED_DENIED) {
      const rej = await callMutation(page, a.entity, a.action, { id: DUMMY_ID }, 'id');
      expectActionDenied(rej, {
        errorCode: ENFORCEMENT_ERROR_CODES.NO_PERMISSION,
        token: '没有访问权限',
      });
    }
  });

  test('restricted denied for ErpCsTicket (undeclared perms, fail-closed expected-correct)', async ({ page }) => {
    await loginAsRole(page, 'restricted');
    await page.goto('/#/ErpPurOrder-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    for (const a of CS_TICKET_TRIO) {
      const rej = await callMutation(page, a.entity, a.action, { id: DUMMY_ID }, 'id');
      expectActionDenied(rej, {
        errorCode: ENFORCEMENT_ERROR_CODES.NO_PERMISSION,
        token: '没有访问权限',
      });
    }
  });

  test('business role 审核人 denied for ErpCsTicket (deny-by-default expected-correct, 非缺陷)', async ({ page }) => {
    await loginAsRole(page, '审核人');
    await page.goto('/#/ErpPurOrder-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    for (const a of CS_TICKET_TRIO) {
      const rej = await callMutation(page, a.entity, a.action, { id: DUMMY_ID }, 'id');
      expectActionDenied(rej, {
        errorCode: ENFORCEMENT_ERROR_CODES.NO_PERMISSION,
        token: '没有访问权限',
      });
    }
  });

  test('admin passes for sampled approval-adjacent actions (pur/sal/prj × 三动作族)', async ({ page }) => {
    await loginAsRole(page, 'admin');
    await page.goto('/#/ErpPurOrder-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    for (const a of ADMIN_PASS) {
      const rej = await callMutation(page, a.entity, a.action, { id: DUMMY_ID }, 'id');
      const errorCode = rej.json?.extensions?.['nop-error-code'];
      expect(
        errorCode,
        `${a.entity}.${a.action} for admin: expected enforcement pass (not no-permission), got ${errorCode}`,
      ).not.toBe(ENFORCEMENT_ERROR_CODES.NO_PERMISSION);
    }
  });

  test('submitForApproval fail-closed admin-only: business role 审核人 denied (zero :mutation seeds, Non-Goal 边界)', async ({ page }) => {
    await loginAsRole(page, '审核人');
    await page.goto('/#/ErpPurOrder-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    for (const entity of ['ErpPurOrder', 'ErpSalOrder']) {
      const rej = await callMutation(page, entity, 'submitForApproval', { id: DUMMY_ID }, 'id');
      expectActionDenied(rej, {
        errorCode: ENFORCEMENT_ERROR_CODES.NO_PERMISSION,
        token: '没有访问权限',
      });
    }
  });

  for (const { entity, role } of REJECT_PASS) {
    test(`authorized role ${role} passes reject on ${entity}`, async ({ page }) => {
      await loginAsRole(page, role);
      await page.goto('/#/ErpPurOrder-main', { waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(1500);

      const rej = await callMutation(page, entity, 'reject', { id: DUMMY_ID }, 'id');
      const errorCode = rej.json?.extensions?.['nop-error-code'];
      expect(
        errorCode,
        `${entity}.reject for ${role}: expected enforcement pass (not no-permission), got ${errorCode}`,
      ).not.toBe(ENFORCEMENT_ERROR_CODES.NO_PERMISSION);
    });
  }

  for (const { entity, role } of WITHDRAW_PASS) {
    test(`authorized role ${role} passes withdrawApproval on ${entity}`, async ({ page }) => {
      await loginAsRole(page, role);
      await page.goto('/#/ErpPurOrder-main', { waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(1500);

      const rej = await callMutation(page, entity, 'withdrawApproval', { id: DUMMY_ID }, 'id');
      const errorCode = rej.json?.extensions?.['nop-error-code'];
      expect(
        errorCode,
        `${entity}.withdrawApproval for ${role}: expected enforcement pass (not no-permission), got ${errorCode}`,
      ).not.toBe(ENFORCEMENT_ERROR_CODES.NO_PERMISSION);
    });
  }
});
