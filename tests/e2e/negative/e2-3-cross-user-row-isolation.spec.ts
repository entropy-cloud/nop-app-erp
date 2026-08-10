import {
  test,
  expect,
  loginAsRole,
  expectRowsHidden,
  expectRowsVisible,
} from './_helper';
import {
  createViaSave,
  deleteById,
  eqFilter,
} from '../business-actions/_helper';

/**
 * E2.3 跨用户越权不可见深度负向 Proof（plan 2026-08-11-0915-2 Phase 2）。
 *
 * 与 E2.1/E2.2 的 filter-active smoke 区别：本 spec 显式以「跨用户越权不可见」负向语义组织，
 * 用专用原语 `expectRowsHidden`/`expectRowsVisible`（而非 inline findItems+expect）断言：
 *   - 账号 A（userId_A）创建数据 → 账号 B（userId_B，不同整数 userId）登录查询时
 *     A 的越权行被 data-auth 行级过滤排除（`expectRowsHidden`，absent 非 error），
 *     B 自己的行仍可见（`expectRowsVisible`）。
 *
 * **配置基线**：%test profile data-auth 双开关 ON（E2.1 落地）：
 *   - `nop.auth.enable-data-auth=true` + `erp.data-auth.role-row-filter-enabled=true`
 *   - `nop.auth.use-user-id-for-audit-fields=true`（使 createdBy auto-stamp = userId）
 *
 * **覆盖列域（E2.3 Phase 1 覆盖矩阵激活域子集）**：
 *   - userId 域列：sal `createdBy`（admin userId=1 创建 → 销售员 userId=12 越权不可见）
 *   - employee-id 域列：qa `inspectorId`（admin 创建 inspectorId=999 → 质检员 userId=21 越权不可见）
 *   - employee-id 域列：mnt `assignedTo`（admin 创建 assignedTo=999 → 维护人员 userId=17 越权不可见）
 *
 * **action-auth 门控边界**：仅覆盖 E2E 可达实体（SUBM roles 含对应角色）：
 *   - ErpSalOrder（sal SUBM roles=销售员）✓
 *   - ErpQaInspection（qa-inspection SUBM roles=质检员/质量主管）✓
 *   - ErpMntVisit（mnt-work SUBM roles=维护主管/维护人员）✓
 *   ErpQaRiskRegister / ErpQaSpcSample 被 action-auth 门控（质检员无 query 授权）→ 降级后端 Proof
 *   （TestErpQaEmployeeIdRowFilterIsolation 跨用户变体已覆盖）。
 *
 * **data-auth 行过滤形状（无 errors）**：DefaultDataAuthChecker.getFilter 注入 SQL WHERE →
 * __findPage 静默返回过滤后行集（total/items 减少），故「越权行被过滤」= 断言特定行 absent
 * （`expectRowsHidden`），非 errors 断言——这是 data-auth 层与 action-auth 层拒绝形状的本质差异。
 */

const SAL_FIELDS = 'id code';
const QA_FIELDS = 'id code';
const MNT_FIELDS = 'id code';
const UNIQUE_TAG = `E23-${process.pid}-${Date.now()}`;

/** 共享最小 ErpSalOrder 头字段（DRAFT/UNSUBMITTED，不触发表单业务校验/过账）。 */
function salOrderData(code: string) {
  return {
    code,
    orgId: 2,
    customerId: 1,
    warehouseId: 2,
    businessDate: '2026-08-11',
    currencyId: 1,
    exchangeRate: 1,
    docStatus: 'DRAFT',
    approveStatus: 'UNSUBMITTED',
  };
}

/** 共享最小 ErpQaInspection 头字段。inspectorId 显式传入（employee-id 域列，非 auto-stamp）。 */
function qaInspectionData(code: string, inspectorId: number) {
  return {
    code,
    inspectionType: 'INCOMING',
    materialId: 1,
    businessDate: '2026-08-11',
    inspectionDate: '2026-08-11',
    inspectorId,
    result: 'PENDING',
    docStatus: 'DRAFT',
    approveStatus: 'UNSUBMITTED',
  };
}

/** 共享最小 ErpMntVisit 头字段。assignedTo 显式传入（employee-id 域列）。 */
function mntVisitData(code: string, assignedTo: number) {
  return {
    code,
    equipmentId: 1,
    visitDate: '2026-08-11',
    status: 'PLANNED',
    assignedTo,
  };
}

test.describe('E2.3 cross-user row isolation (越权不可见深度负向)', () => {
  test('sal createdBy userId 域: 销售员越权不可见 admin 创建的单据', async ({ page }) => {
    const codeAdmin = `${UNIQUE_TAG}-SAL-ADM`;
    const codeOwn = `${UNIQUE_TAG}-SAL-OWN`;

    // 账号 A = admin（userId 1）创建单据 → createdBy auto-stamp = "1"
    await loginAsRole(page, 'admin');
    await page.goto('/#/ErpSalOrder-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    const adminOrder = await createViaSave(page, 'ErpSalOrder', salOrderData(codeAdmin), SAL_FIELDS);
    expect(adminOrder?.code, 'admin order saved').toBe(codeAdmin);

    // 账号 B = 销售员（userId 12）创建自己的单据 → createdBy = "12"
    await loginAsRole(page, '销售员');
    await page.goto('/#/ErpSalOrder-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    const ownOrder = await createViaSave(page, 'ErpSalOrder', salOrderData(codeOwn), SAL_FIELDS);
    expect(ownOrder?.code, 'salesperson own order saved').toBe(codeOwn);

    // 跨用户越权不可见：销售员 B 查询 admin A 的单据 → 越权行 absent（非 error）
    await expectRowsHidden(
      page,
      'ErpSalOrder',
      eqFilter('code', codeAdmin),
      SAL_FIELDS,
    );

    // 自己的行仍可见（createdBy == userId 12）
    const ownRows = await expectRowsVisible(
      page,
      'ErpSalOrder',
      eqFilter('code', codeOwn),
      SAL_FIELDS,
      1,
    );
    expect(ownRows[0].code).toBe(codeOwn);

    // cleanup（admin 删两单）
    await loginAsRole(page, 'admin');
    await page.goto('/#/ErpSalOrder-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    await deleteById(page, 'ErpSalOrder', adminOrder.id);
    await deleteById(page, 'ErpSalOrder', ownOrder.id);
  });

  test('qa inspectorId employee-id 域: 质检员越权不可见 inspectorId=999 的检验单', async ({ page }) => {
    const codeOther = `${UNIQUE_TAG}-QA-OTHER`;
    const codeOwn = `${UNIQUE_TAG}-QA-OWN`;

    // 账号 A = admin 创建 inspectorId=999（他人任务，不匹配任何真实 userId）
    await loginAsRole(page, 'admin');
    await page.goto('/#/ErpQaInspection-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    const otherOrder = await createViaSave(
      page,
      'ErpQaInspection',
      qaInspectionData(codeOther, 999),
      QA_FIELDS,
    );
    expect(otherOrder?.code, 'admin created other inspection').toBe(codeOther);

    // 账号 B = 质检员（userId 21）创建 inspectorId=21（自己任务）
    await loginAsRole(page, '质检员');
    await page.goto('/#/ErpQaInspection-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    const ownOrder = await createViaSave(
      page,
      'ErpQaInspection',
      qaInspectionData(codeOwn, 21),
      QA_FIELDS,
    );
    expect(ownOrder?.code, 'inspector own inspection saved').toBe(codeOwn);

    // 跨用户越权不可见：质检员 B 查询 inspectorId=999 的单据 → 越权行 absent
    await expectRowsHidden(
      page,
      'ErpQaInspection',
      eqFilter('code', codeOther),
      QA_FIELDS,
    );

    // 自己的行仍可见（inspectorId == userId 21，整数直比成立）
    const ownRows = await expectRowsVisible(
      page,
      'ErpQaInspection',
      eqFilter('code', codeOwn),
      QA_FIELDS,
      1,
    );
    expect(ownRows[0].code).toBe(codeOwn);

    // cleanup（admin 删两单）
    await loginAsRole(page, 'admin');
    await page.goto('/#/ErpQaInspection-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    await deleteById(page, 'ErpQaInspection', otherOrder.id);
    await deleteById(page, 'ErpQaInspection', ownOrder.id);
  });

  test('mnt assignedTo employee-id 域: 维护人员越权不可见 assignedTo=999 的维护访问', async ({ page }) => {
    const codeOther = `${UNIQUE_TAG}-MNT-OTHER`;
    const codeOwn = `${UNIQUE_TAG}-MNT-OWN`;

    // 账号 A = admin 创建 assignedTo=999（他人任务）
    await loginAsRole(page, 'admin');
    await page.goto('/#/ErpMntVisit-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    const otherVisit = await createViaSave(
      page,
      'ErpMntVisit',
      mntVisitData(codeOther, 999),
      MNT_FIELDS,
    );
    expect(otherVisit?.code, 'admin created other visit').toBe(codeOther);

    // 账号 B = 维护人员（userId 17）创建 assignedTo=17（自己任务）
    await loginAsRole(page, '维护人员');
    await page.goto('/#/ErpMntVisit-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    const ownVisit = await createViaSave(
      page,
      'ErpMntVisit',
      mntVisitData(codeOwn, 17),
      MNT_FIELDS,
    );
    expect(ownVisit?.code, 'technician own visit saved').toBe(codeOwn);

    // 跨用户越权不可见：维护人员 B 查询 assignedTo=999 的单据 → 越权行 absent
    await expectRowsHidden(
      page,
      'ErpMntVisit',
      eqFilter('code', codeOther),
      MNT_FIELDS,
    );

    // 自己的行仍可见（assignedTo == userId 17，整数直比成立）
    const ownRows = await expectRowsVisible(
      page,
      'ErpMntVisit',
      eqFilter('code', codeOwn),
      MNT_FIELDS,
      1,
    );
    expect(ownRows[0].code).toBe(codeOwn);

    // cleanup（admin 删两单）
    await loginAsRole(page, 'admin');
    await page.goto('/#/ErpMntVisit-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    await deleteById(page, 'ErpMntVisit', otherVisit.id);
    await deleteById(page, 'ErpMntVisit', ownVisit.id);
  });
});
