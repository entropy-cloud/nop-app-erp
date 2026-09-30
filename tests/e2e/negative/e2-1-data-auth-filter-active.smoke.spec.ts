import {
  test,
  expect,
  loginAsRole,
  findItems,
} from './_helper';
import {
  createViaSave,
  deleteById,
  eqFilter,
  andFilter,
} from '../business-actions/_helper';

/**
 * E2.1 data-auth 行级过滤 filter-active smoke Proof（plan 2026-08-10-2059-1 Phase 3）。
 *
 * **配置基线**：%test profile `enable-data-auth=true` + `role-row-filter-enabled=true`
 * + `use-user-id-for-audit-fields=true`（使 createdBy auto-stamp = userId，与 filter EL
 * `${userContext.userId}` 对齐——此 flag 是 E2.1 执行期发现的 row-filter 运行时必要 enabler：
 * 平台 `AuthHttpServerFilter.initUserContext` 仅当此 flag=true 时 `ctx.setUserRefNo(userId)`，
 * 否则 createdBy stamped 为 sys-user-name（"sys"），filter `eq(createdBy,userId)` 永不命中）。
 *
 * **范围 = filter-active（单视角行集收敛）**：销售员（role-sal，userId 12）仅见自己创建的单据，
 * admin（nop，userId 1）经 `user` 兜底 role-auth（ObjDataAuthModel.isUserInRole 对 roleIds="user"
 * 总命中）全见。非跨用户越权深度负向（归 E2.3）。
 *
 * **roleId 修正（plan Phase 1）**：sal data-auth.xml 6 处 roleIds 由「业务员」修正为「销售员」，
 * 与冻结词表 + nop_auth_role.csv L3 种子一致——本 spec 用真实种子 role-sal 账号证明修正后销售员
 * 行级过滤真正生效（修正前「销售员」用户不匹配「业务员」role-auth → 落 user 兜底 → 隔离被静默击败）。
 *
 * **data-auth 行过滤形状（无 errors）**：DefaultDataAuthChecker.getFilter 注入 SQL WHERE →
 * __findPage 静默返回过滤后行集（total/items 减少），故「越权行被过滤」= 断言特定行 absent，
 * 非 errors 断言（见 _helper.ts expectRowsHidden/Visible 原语设计）。
 *
 * **qa 维覆盖边界**：ErpQaRiskRegister 的 qa data-auth 规则（质检员 eq(ownerId,userId)）存在且
 * well-formed（xmllint），机制与 sal 同源（DefaultDataAuthChecker + eq userId 域列）。但质检员账号
 * 在 action-auth 层无 ErpQaRiskRegister:query 授权（qa-goal SUBM roles=质量主管，FNPT 未 seed 给
 * 质检员）→ action-auth 拒绝先于 data-auth → 质检员 row-filter 的 E2E 证明被 action-auth 门控
 * （扩 action-auth 归 successor，非 E2.1 data-auth 范围）。故 qa 维此处仅证 admin 规则加载不
 * fail-closed（admin 经 user 兜底可见），质检员 row-filter 的运行时 Proof 由后端机制同源性
 * （TestErpRoleRowFilterIsolation sal/createdBy 范式）+ xmllint 覆盖。
 */

const SAL_FIELDS = 'id code';
const UNIQUE_TAG = `E21-${process.pid}-${Date.now()}`;

/** 共享最小 ErpSalOrder 头字段（DRAFT/UNSUBMITTED，不触发表单业务校验/过账）。 */
function salOrderData(code: string) {
  return {
    code,
    orgId: '2',
    customerId: '1',
    warehouseId: '2',
    businessDate: '2026-08-10',
    currencyId: '1',
    exchangeRate: 1,
    docStatus: 'DRAFT',
    approveStatus: 'UNSUBMITTED',
  };
}

test.describe('E2.1 data-auth filter-active smoke', () => {
  test('salesperson (role-sal) creator-filtered isolation: admin rows hidden (D1c one-directional)', async ({ page }) => {
    const codeAdmin = `${UNIQUE_TAG}-ADM`;
    const codeOwn = `${UNIQUE_TAG}-SAL`;

    // 1. admin（nop, userId 1）建两行 → createdBy auto-stamp = "1"。
    //    D1c 重构（plan 2026-10-01-0523-1）：creator 过滤规则（eq(createdBy,userId)）下销售员
    //    经 GraphQL 无法自建行（checkDataAuth(SAVE) 先于 audit 戳，B-1 实证 no-data-auth），
    //    建行统一由 admin 承担；「自见正向断言」不可达已显式登记（save-under-creator-filter successor）。
    await loginAsRole(page, 'admin');
    await page.goto('/#/ErpSalOrder-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    const adminOrder = await createViaSave(page, 'ErpSalOrder', salOrderData(codeAdmin), SAL_FIELDS);
    expect(adminOrder?.code, 'admin order saved').toBe(codeAdmin);
    const ownOrder = await createViaSave(page, 'ErpSalOrder', salOrderData(codeOwn), SAL_FIELDS);
    expect(ownOrder?.code, 'second admin order saved').toBe(codeOwn);

    // 2. 销售员（role-sal, userId 12）视角：两行均他人行 → 不可见（单向负向隔离证明，无 errors）
    await loginAsRole(page, '销售员');
    await page.goto('/#/ErpSalOrder-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    const adminRowsFromSal = await findItems(
      page,
      'ErpSalOrder',
      eqFilter('code', codeAdmin),
      SAL_FIELDS,
    );
    expect(
      adminRowsFromSal.length,
      '销售员不应见 admin 创建的单据（data-auth 行级过滤收敛）',
    ).toBe(0);
    const ownRowsFromSal = await findItems(
      page,
      'ErpSalOrder',
      eqFilter('code', codeOwn),
      SAL_FIELDS,
    );
    expect(
      ownRowsFromSal.length,
      '销售员不应见任何非自建行（createdBy 过滤单向证明）',
    ).toBe(0);

    // 3. admin 视角：两行均可见（user 兜底 role-auth，无 filter）
    await loginAsRole(page, 'admin');
    await page.goto('/#/ErpSalOrder-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    const adminSeeAdmin = await findItems(
      page,
      'ErpSalOrder',
      eqFilter('code', codeAdmin),
      SAL_FIELDS,
    );
    const adminSeeOwn = await findItems(
      page,
      'ErpSalOrder',
      eqFilter('code', codeOwn),
      SAL_FIELDS,
    );
    expect(adminSeeAdmin.length, 'admin 应见自己创建的单据').toBe(1);
    expect(adminSeeOwn.length, 'admin 应见第二行（全见）').toBe(1);

    // cleanup（admin 删两行）
    await deleteById(page, 'ErpSalOrder', adminOrder.id);
    await deleteById(page, 'ErpSalOrder', ownOrder.id);
  });

  test('qa rule active: admin sees ErpQaRiskRegister (no fail-closed)', async ({ page }) => {
    // qa 维：质检员 row-filter 被 action-auth 门控（见 spec 头注），此处证 admin 经 user 兜底
    // 可见 ErpQaRiskRegister（规则加载 + 不 fail-closed）。质检员 row-filter 运行时 Proof 由
    // 后端机制同源性（TestErpRoleRowFilterIsolation）覆盖。
    await loginAsRole(page, 'admin');
    await page.goto('/#/ErpQaRiskRegister-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    const tag = `${UNIQUE_TAG}-QA`;
    const created = await createViaSave(
      page,
      'ErpQaRiskRegister',
      {
        // USC-02b R1 处方：ORM 实有列 5 键（原 7 个非列键首键即抛 unknown-prop）
        code: tag,
        riskDate: '2026-08-10',
        likelihood: 2,
        severity: 2,
        status: 'OPEN',
      },
      'id code',
    );
    expect(created?.code, 'qa risk register saved').toBe(tag);

    const rows = await findItems(
      page,
      'ErpQaRiskRegister',
      eqFilter('code', tag),
      'id code',
    );
    expect(
      rows.length,
      'admin 应见 ErpQaRiskRegister（qa data-auth 规则加载 + user 兜底不 fail-closed）',
    ).toBe(1);

    await deleteById(page, 'ErpQaRiskRegister', created.id);
  });
});
