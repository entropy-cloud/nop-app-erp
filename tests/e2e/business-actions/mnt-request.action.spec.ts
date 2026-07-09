import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, deleteById, deleteByFilter, eqFilter } from './_helper';

/**
 * maintenance ErpMntRequest 5 态状态机浏览器层 E2E（plan 2026-07-10-0335-2 Phase 2）。
 *
 * 验证自定义 @BizMutation（accept/startRepair/complete/rejectRequest/cancel）经 GraphQL /graphql 的全栈可达性 +
 * 自定义 status 状态机迁移 + 非法迁移守卫。
 *
 * 权威状态机（ErpMntRequestBizModel + ErpMntDaoConstants + state-machine.md）：REQUEST 5 态
 *   OPEN --accept--> ACCEPTED --startRepair--> IN_PROGRESS --complete--> COMPLETED（终态）
 *   OPEN/ACCEPTED --rejectRequest--> REJECTED（终态）
 *   OPEN/ACCEPTED --cancel--> CANCELLED（终态）
 * 非法迁移抛 ERR_INVALID_REQUEST_STATUS_TRANSITION。
 *
 * ORM 异常注记（plan §Current Baseline）：ErpMntRequest 标 use-approval tagSet 且 xbiz extends approval-support.xbiz，
 * 但 ORM 无 approveStatus 列——生命周期完全由自定义 status 状态机驱动。本 spec 验证自定义 status 迁移路径，
 * 不调用平台 submitForApproval/approve（无列可写）。
 *
 * 副作用注记（accept 生成 ErpMntVisit）：accept 经 generateResponsiveVisit 创建响应式 DRAFT visit
 * （code=VST-REQ-{requestId}，visitDate=today）。本 spec Non-Goal 不编排 Visit，仅断言 Request 自身 status 迁移；
 * 但 visit.visitDate=today 落在 maintenance 看板数值断言区间（2026-07-01~07-31）内，不清理会污染
 * periodVisitCount 基线（1），故 accept 路径完成后按 code 清理生成 visit。
 *
 * 实现裁决：mandatory 字段经 ORM 核实为 code+equipmentId+requestDate+description+priority+status+requestedBy
 * （UK_MNT_REQUEST_CODE 单列唯一，code 须唯一——用 `E2E-MNT-REQ-<tag>-<ts>`）。
 *
 * 种子引用（init-data）：equipment id=1（EQ-2026-001 数控机床，RUNNING）；employee id=2（李四，作 requestedBy）。
 * 清理：正路径 accept 生成 visit 按 code 删除 + Request 自身删除；分支路径无 visit 副作用，仅删 Request。
 */

const EQ_ID = 1;
const REQUESTED_BY = 2;

async function seedRequest(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpMntRequest',
    {
      code: `E2E-MNT-REQ-${tag}-${Date.now()}`,
      equipmentId: EQ_ID,
      requestDate: '2026-07-10',
      description: `E2E maintenance request ${tag}`,
      priority: 'HIGH',
      status: 'OPEN',
      requestedBy: REQUESTED_BY,
    },
    'id status',
  );
}

test.describe('maintenance ErpMntRequest state machine actions', () => {
  test('happy path: save(OPEN) → accept(ACCEPTED) → startRepair(IN_PROGRESS) → complete(COMPLETED) + guard', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMntRequest-main');

    const req = await seedRequest(page, 'happy');
    expect(req.status, 'new request status=OPEN').toBe('OPEN');

    // accept: OPEN → ACCEPTED（生成响应式 DRAFT visit 副作用，Non-Goal 不编排）
    const accepted = await callMutationOk(
      page, 'ErpMntRequest', 'accept', { requestId: req.id }, 'id status',
    );
    expect(accepted.status, 'accept should transition OPEN → ACCEPTED').toBe('ACCEPTED');

    // startRepair: ACCEPTED → IN_PROGRESS
    const started = await callMutationOk(
      page, 'ErpMntRequest', 'startRepair', { requestId: req.id }, 'id status',
    );
    expect(started.status, 'startRepair should transition ACCEPTED → IN_PROGRESS').toBe('IN_PROGRESS');

    // complete: IN_PROGRESS → COMPLETED + completedAt
    const completed = await callMutationOk(
      page, 'ErpMntRequest', 'complete', { requestId: req.id }, 'id status completedAt',
    );
    expect(completed.status, 'complete should transition IN_PROGRESS → COMPLETED').toBe('COMPLETED');
    expect(completed.completedAt, 'complete should stamp completedAt').toBeTruthy();

    const verified = await verifyState(page, 'ErpMntRequest', req.id, 'status');
    expect(verified.status, '__get should confirm COMPLETED').toBe('COMPLETED');

    // 非法迁移守卫：COMPLETED → accept（须 OPEN），经 GraphQL 返回 errors
    const rej = await callMutation(page, 'ErpMntRequest', 'accept', { requestId: req.id }, 'id');
    expect(rej.errors, 'accept from COMPLETED should be rejected as illegal transition').toBeTruthy();

    // 清理：accept 生成的响应式 visit（visitDate=today 落入看板区间，须删除避免污染 periodVisitCount）+ Request 自身
    await deleteByFilter(page, 'ErpMntVisit', eqFilter('code', `VST-REQ-${req.id}`));
    await deleteById(page, 'ErpMntRequest', req.id);
  });

  test('branch paths: rejectRequest(OPEN→REJECTED) and cancel(OPEN→CANCELLED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMntRequest-main');

    // rejectRequest: OPEN → REJECTED
    const toReject = await seedRequest(page, 'rej');
    const rejected = await callMutationOk(
      page, 'ErpMntRequest', 'rejectRequest', { requestId: toReject.id }, 'id status',
    );
    expect(rejected.status, 'rejectRequest should transition OPEN → REJECTED').toBe('REJECTED');

    const verifiedReject = await verifyState(page, 'ErpMntRequest', toReject.id, 'status');
    expect(verifiedReject.status, '__get should confirm REJECTED').toBe('REJECTED');

    // cancel: OPEN → CANCELLED
    const toCancel = await seedRequest(page, 'cnl');
    const cancelled = await callMutationOk(
      page, 'ErpMntRequest', 'cancel', { requestId: toCancel.id }, 'id status',
    );
    expect(cancelled.status, 'cancel should transition OPEN → CANCELLED').toBe('CANCELLED');

    const verifiedCancel = await verifyState(page, 'ErpMntRequest', toCancel.id, 'status');
    expect(verifiedCancel.status, '__get should confirm CANCELLED').toBe('CANCELLED');

    // 清理：分支路径无 accept，无 visit 副作用，仅删 Request 自身
    await deleteById(page, 'ErpMntRequest', toReject.id);
    await deleteById(page, 'ErpMntRequest', toCancel.id);
  });
});
