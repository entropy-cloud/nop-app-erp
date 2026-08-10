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
} from '../business-actions/_helper';

/**
 * E2.2 employee-id 域列 data-auth 行级过滤 filter-active smoke Proof
 * （plan 2026-08-11-0915-1 Phase 3）。
 *
 * **配置基线**：%test profile data-auth 双开关 ON（E2.1 落地）：
 *   - `nop.auth.enable-data-auth=true` + `erp.data-auth.role-row-filter-enabled=true`
 *   - `nop.auth.use-user-id-for-audit-fields=true`
 *
 * **范围 = filter-active（单视角行集收敛）**：质检员（role-inspector，userId 21）仅见
 * `inspectorId == 21` 的检验单（经 erp_md_employee.csv 种子对齐 user.id==employee.id=21）；
 * 维护人员（role-mnt-tech，userId 17）仅见 `assignedTo == 17` 的维护访问。
 * admin（nop，userId 1）经 `user` 兜底 role-auth 全见。非跨用户越权深度负向（归 E2.3）。
 *
 * **employee-id 域列默认等效方案**：规则 `eq(inspectorId, ${userContext.userId})` /
 * `eq(assignedTo, ${userContext.userId})` 整数直比成立，前提 user.id==employee.id 种子对齐
 * （erp_md_employee.csv 补 id=17 维护员甲 + id=21 质检员甲）。不触 ORM，通用 user→employee
 * 解析须 ErpMdEmployee.userId 扩展（ask-first successor）。
 *
 * **action-auth 门控状态（Phase 1 实证）**：
 *   - ErpQaInspection-main 在 qa-inspection SUBM（roles=质检员/质量主管）→ 质检员有 query 授权 ✓
 *   - ErpMntVisit-main 在 mnt-work SUBM（roles=维护主管/维护人员）→ 维护人员有 query 授权 ✓
 *   - ErpQaSpcSample 在 qa-spc SUBM（roles=质量主管）→ 质检员无授权，E2E proof 降级后端
 *     （TestErpQaEmployeeIdRowFilterIsolation 已覆盖）
 *
 * **data-auth 行过滤形状（无 errors）**：DefaultDataAuthChecker.getFilter 注入 SQL WHERE →
 * __findPage 静默返回过滤后行集（total/items 减少），故「越权行被过滤」= 断言特定行 absent。
 */

const QA_FIELDS = 'id code';
const MNT_FIELDS = 'id code';
const UNIQUE_TAG = `E22-${process.pid}-${Date.now()}`;

/** 共享最小 ErpQaInspection 头字段（DRAFT/UNSUBMITTED，不触发表单业务校验/过账）。 */
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

/** 共享最小 ErpMntVisit 头字段。 */
function mntVisitData(code: string, assignedTo: number) {
  return {
    code,
    equipmentId: 1,
    visitDate: '2026-08-11',
    status: 'PLANNED',
    assignedTo,
  };
}

test.describe('E2.2 employee-id row-filter filter-active smoke', () => {
  test('inspector (role-inspector, userId 21) sees only own ErpQaInspection rows', async ({ page }) => {
    const codeOwn = `${UNIQUE_TAG}-QA-OWN`;
    const codeOther = `${UNIQUE_TAG}-QA-OTHER`;

    // 1. admin 建检验单 inspectorId=999（他人任务）
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

    // 2. 质检员（role-inspector, userId 21）建检验单 inspectorId=21（自己任务）
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

    // 3. 质检员视角：查他人单 → 行集收敛为空（越权行被过滤，无 errors）
    const otherRows = await findItems(
      page,
      'ErpQaInspection',
      eqFilter('code', codeOther),
      QA_FIELDS,
    );
    expect(
      otherRows.length,
      '质检员不应见 inspectorId=999 的检验单（data-auth 行级过滤收敛）',
    ).toBe(0);

    // 4. 质检员视角：查自己单 → 可见（inspectorId == 自己 userId 21）
    const ownRows = await findItems(
      page,
      'ErpQaInspection',
      eqFilter('code', codeOwn),
      QA_FIELDS,
    );
    expect(
      ownRows.length,
      '质检员应见自己 inspectorId=21 的检验单（整数直比成立）',
    ).toBe(1);
    expect(ownRows[0].code).toBe(codeOwn);

    // 5. cleanup（admin 删两单）
    await loginAsRole(page, 'admin');
    await page.goto('/#/ErpQaInspection-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    await deleteById(page, 'ErpQaInspection', otherOrder.id);
    await deleteById(page, 'ErpQaInspection', ownOrder.id);
  });

  test('maintenance tech (role-mnt-tech, userId 17) sees only own ErpMntVisit rows', async ({ page }) => {
    const codeOwn = `${UNIQUE_TAG}-MNT-OWN`;
    const codeOther = `${UNIQUE_TAG}-MNT-OTHER`;

    // 1. admin 建维护访问 assignedTo=999（他人任务）
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

    // 2. 维护人员（role-mnt-tech, userId 17）建维护访问 assignedTo=17（自己任务）
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

    // 3. 维护人员视角：查他人单 → 行集收敛为空（越权行被过滤）
    const otherRows = await findItems(
      page,
      'ErpMntVisit',
      eqFilter('code', codeOther),
      MNT_FIELDS,
    );
    expect(
      otherRows.length,
      '维护人员不应见 assignedTo=999 的维护访问（data-auth 行级过滤收敛）',
    ).toBe(0);

    // 4. 维护人员视角：查自己单 → 可见（assignedTo == 自己 userId 17）
    const ownRows = await findItems(
      page,
      'ErpMntVisit',
      eqFilter('code', codeOwn),
      MNT_FIELDS,
    );
    expect(
      ownRows.length,
      '维护人员应见自己 assignedTo=17 的维护访问（整数直比成立）',
    ).toBe(1);
    expect(ownRows[0].code).toBe(codeOwn);

    // 5. cleanup（admin 删两单）
    await loginAsRole(page, 'admin');
    await page.goto('/#/ErpMntVisit-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    await deleteById(page, 'ErpMntVisit', otherVisit.id);
    await deleteById(page, 'ErpMntVisit', ownVisit.id);
  });
});
