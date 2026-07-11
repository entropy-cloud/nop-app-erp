import { test, expect, loginAndNavigate, createViaSave, callMutationOk, verifyState, deleteById, deleteByFilter, eqFilter, findFirst } from './_helper';

/**
 * maintenance Request→Visit 副作用编排 E2E（plan 2026-07-11-2329-2 Phase 1）。
 *
 * 验证 Request `accept` 经 `ErpMntRequestBizModel.generateResponsiveVisit` 创建响应式 DRAFT Visit 的
 * 浏览器层全栈可达性 + 字段精确断言。补齐 0335-2 Deferred「maintenance Request→Visit 编排」——
 * 既有 `mnt-request.action.spec.ts` 显式 Non-Goal 不编排 Visit（仅按 code 清理防看板基线污染），
 * 本 spec 聚焦 Visit 副作用字段断言（code/equipmentId/visitDate/visitType/status/assignedTo 6 字段）。
 *
 * 权威实现（`ErpMntRequestBizModel.generateResponsiveVisit`）：
 *   code = "VST-REQ-" + request.id（精确值，无后缀，eqFilter 非 like）
 *   equipmentId = request.equipmentId
 *   visitDate = CoreMetrics.currentDate()（服务端当前日期）
 *   status = VISIT_STATUS_DRAFT
 *   visitType = VISIT_TYPE_RESPONSIVE
 *   assignedTo = request.assignedTo ?? request.requestedBy
 *
 * visitDate 时区裁决：CoreMetrics.currentDate() 经 LocalDate.now()（JVM 默认时区），与客户端时区可能
 * 跨日。故 visitDate 期望值取自同事务服务端戳记的 visit.createTime 日期部分（两者同为服务端时钟派生，
 * 时区一致），证明 visitDate 已被 accept 副作用填充为服务端当日，而非 null 或种子固定值。
 *
 * 清理：accept 生成的 Visit（visitDate=today 落入 maintenance 看板数值断言区间，须删除避免污染
 * periodVisitCount 基线 1）+ Request 自身。镜像既有 mnt-request spec 清理范式。
 */

const EQ_ID = 1;
const REQUESTED_BY = 2;

async function seedRequest(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpMntRequest',
    {
      code: `E2E-MNT-REQ-VST-${tag}-${Date.now()}`,
      equipmentId: EQ_ID,
      requestDate: '2026-07-10',
      description: `E2E maintenance request visit orchestration ${tag}`,
      priority: 'HIGH',
      status: 'OPEN',
      requestedBy: REQUESTED_BY,
    },
    'id status',
  );
}

test.describe('maintenance Request→Visit side-effect orchestration', () => {
  test('accept generates responsive DRAFT Visit with exact fields', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMntRequest-main');

    const req = await seedRequest(page, 'vst');
    expect(req.status, 'new request status=OPEN').toBe('OPEN');

    // accept: OPEN → ACCEPTED（生成响应式 DRAFT visit 副作用）
    const accepted = await callMutationOk(
      page, 'ErpMntRequest', 'accept', { requestId: req.id }, 'id status',
    );
    expect(accepted.status, 'accept should transition OPEN → ACCEPTED').toBe('ACCEPTED');

    // 经 __get 权威查库确认 Request 已迁移（独立于 mutation 返回值）
    const verified = await verifyState(page, 'ErpMntRequest', req.id, 'status');
    expect(verified.status, '__get should confirm ACCEPTED').toBe('ACCEPTED');

    // 反查生成的响应式 Visit（code 为精确值 VST-REQ-{requestId}，eqFilter 非 like）
    const visitCode = `VST-REQ-${req.id}`;
    const visit = await findFirst<any>(
      page, 'ErpMntVisit', eqFilter('code', visitCode),
      'id code equipmentId visitDate createTime status visitType assignedTo',
    );
    expect(visit, `responsive visit with code=${visitCode} should exist`).not.toBeNull();

    // 6 字段精确断言
    expect(visit!.code, 'visit.code should exact-match VST-REQ-{requestId}').toBe(visitCode);
    expect(Number(visit!.equipmentId), 'visit.equipmentId should match request.equipmentId').toBe(EQ_ID);
    // visitDate 取自同事务服务端戳记 createTime 的日期部分（时区一致，证明 accept 副作用填充当日）
    const expectedVisitDate = String(visit!.createTime).slice(0, 10);
    expect(visit!.visitDate, 'visit.visitDate should be server today (match createTime date)').toBe(expectedVisitDate);
    expect(visit!.status, 'visit.status should be DRAFT').toBe('DRAFT');
    expect(visit!.visitType, 'visit.visitType should be RESPONSIVE').toBe('RESPONSIVE');
    // assignedTo = request.assignedTo(未设) ?? request.requestedBy(REQUESTED_BY=2)
    expect(Number(visit!.assignedTo), 'visit.assignedTo should fall back to request.requestedBy').toBe(REQUESTED_BY);

    // 清理：accept 生成的响应式 Visit（visitDate=today 落入看板区间，须删除避免污染 periodVisitCount）+ Request
    await deleteById(page, 'ErpMntVisit', visit!.id);
    await deleteById(page, 'ErpMntRequest', req.id);

    // 清理完整性核实：Visit 已无残留
    const afterCleanup = await findFirst<any>(page, 'ErpMntVisit', eqFilter('code', visitCode), 'id');
    expect(afterCleanup, 'visit should be removed after cleanup').toBeNull();
  });
});
