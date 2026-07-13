import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, deleteById } from './_helper';

/**
 * aps ErpApsSchedule 排产方案状态机业务动作浏览器层 E2E（plan 2026-07-14-0508-1 Phase 1）。
 *
 * 验证排产方案 DIRECT @BizMutation 状态机经 GraphQL /graphql 的全栈可达性 + status 翻转。
 *
 * 权威状态机（ErpApsScheduleBizModel，对齐 docs/design/aps/scheduling.md §九）：
 *   DRAFT --publish--> PUBLISHED --archive--> ARCHIVED
 *   DRAFT --archive--> ARCHIVED（archive 允许 DRAFT|PUBLISHED 态）
 *   publish 须 DRAFT，否则抛 ERR_APS_SCHEDULE_ILLEGAL_STATUS
 *     （message token「不允许执行该操作」，i18n 描述来自 ErpApsErrors.ERR_APS_SCHEDULE_ILLEGAL_STATUS）
 *
 * ORM 无 useWorkflow / 无 useApproval（rg 核实零命中），纯 DIRECT @BizMutation 浏览器层可达。
 *
 * 自包含 setup：经 __save 直接置 status=DRAFT 入口。Schedule 字段集（参考 Java
 *   TestErpApsScheduleManagement#createSchedule）：code/name/scheduleDate/schedulingMode/
 *   horizonStart/horizonEnd/status。orgId 列非 mandatory（Java 测试亦未传），故本 spec 亦不传。
 * 清理：删 Schedule（无下游产物）。
 */

const HORIZON_START = '2026-07-10T00:00:00';
const HORIZON_END = '2026-07-20T00:00:00';

async function seedSchedule(page: import('@playwright/test').Page, tag: string): Promise<{ id: string; status: string }> {
  return createViaSave(
    page, 'ErpApsSchedule',
    {
      code: `E2E-APS-SCH-${tag}-${Date.now()}`,
      name: `E2E Schedule ${tag}`,
      scheduleDate: '2026-07-10',
      schedulingMode: 'FORWARD',
      horizonStart: HORIZON_START,
      horizonEnd: HORIZON_END,
      status: 'DRAFT',
    },
    'id status',
  );
}

test.describe('aps ErpApsSchedule state machine (publish/archive)', () => {
  test('happy path: save(DRAFT) → publish(PUBLISHED) → archive(ARCHIVED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpApsSchedule-main');

    const s = await seedSchedule(page, 'hp');
    expect(s.status, 'new schedule status=DRAFT').toBe('DRAFT');

    // publish: DRAFT → PUBLISHED
    await callMutationOk(page, 'ErpApsSchedule', 'publish', { id: s.id }, 'id');
    let v = await verifyState(page, 'ErpApsSchedule', s.id, 'status');
    expect(v.status, 'after publish status=PUBLISHED').toBe('PUBLISHED');

    // archive: PUBLISHED → ARCHIVED
    await callMutationOk(page, 'ErpApsSchedule', 'archive', { id: s.id }, 'id');
    v = await verifyState(page, 'ErpApsSchedule', s.id, 'status');
    expect(v.status, 'after archive status=ARCHIVED').toBe('ARCHIVED');

    await deleteById(page, 'ErpApsSchedule', s.id);
  });

  test('archive from DRAFT is legal (archive allows DRAFT|PUBLISHED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpApsSchedule-main');

    const s = await seedSchedule(page, 'ad');
    // archive: DRAFT → ARCHIVED（archive 允许 DRAFT|PUBLISHED，非守卫路径）
    await callMutationOk(page, 'ErpApsSchedule', 'archive', { id: s.id }, 'id');
    const v = await verifyState(page, 'ErpApsSchedule', s.id, 'status');
    expect(v.status, 'after archive from DRAFT status=ARCHIVED').toBe('ARCHIVED');

    await deleteById(page, 'ErpApsSchedule', s.id);
  });

  test('illegal transition guard: ARCHIVED→publish rejected (ERR_APS_SCHEDULE_ILLEGAL_STATUS)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpApsSchedule-main');

    const s = await seedSchedule(page, 'gd');
    await callMutationOk(page, 'ErpApsSchedule', 'publish', { id: s.id }, 'id');
    await callMutationOk(page, 'ErpApsSchedule', 'archive', { id: s.id }, 'id');
    let v = await verifyState(page, 'ErpApsSchedule', s.id, 'status');
    expect(v.status, 'precondition status=ARCHIVED').toBe('ARCHIVED');

    // ARCHIVED → publish：终态不可再迁移（publish 须 DRAFT）
    const rej = await callMutation(page, 'ErpApsSchedule', 'publish', { id: s.id }, 'id');
    expect(rej.errors, 'publish from ARCHIVED should be rejected (requires DRAFT)').toBeTruthy();
    expect(JSON.stringify(rej.errors), 'reject should carry illegal-transition token').toContain('不允许执行该操作');

    // status 不变（守卫失败不应推进状态）
    v = await verifyState(page, 'ErpApsSchedule', s.id, 'status');
    expect(v.status, 'failed publish should not change status').toBe('ARCHIVED');

    await deleteById(page, 'ErpApsSchedule', s.id);
  });
});
