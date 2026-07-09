import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, deleteById } from './_helper';

/**
 * maintenance ErpMntVisit 业务动作浏览器层 E2E（plan 2026-07-09-2004-1 Phase 1）。
 *
 * 验证自定义 @BizMutation（schedule/start/complete/cancel）经 GraphQL /graphql 的全栈可达性 + 5 态状态机迁移 +
 * 设备状态联动副作用（EquipmentStatusLinker：start→UNDER_MAINTENANCE，complete/cancel→RUNNING）。
 *
 * 权威状态机（ErpMntVisitBizModel + _ErpMntDaoConstants）：VISIT 5 态
 *   DRAFT --schedule--> SCHEDULED --start--> IN_PROGRESS --complete--> COMPLETED（终态）
 *   非终态 --cancel--> CANCELLED（终态）
 * schedule 前置：assignedTo + visitDate 非空 + 同设备同日 SCHEDULED/IN_PROGRESS 冲突检查。
 * start/complete 经 EquipmentStatusLinker 联动设备状态（config-gated，默认 enabled）。
 *
 * 实现裁决（Phase 1 Decision）：策略 (a) 全链 __save 创建 DRAFT visit（mandatory 字段经 ORM 核实：
 * code/equipmentId/visitDate/status；schedule 前置 assignedTo 非空），唯一编码 `E2E-MNT-VIS-<ts>`，
 * 驱动 schedule→start→complete 全链 + cancel 异常路径 + 非法迁移守卫。
 *
 * 种子引用（init-data）：equipment id=1（EQ-2026-001 数控机床，RUNNING——start→UNDER_MAINTENANCE，
 * complete→RUNNING 恢复种子态，无污染）；employee id=2（李四，作为 assignedTo）。
 * visitDate 用未来日 2026-12-25 避开种子 visit（COMPLETED 态不冲突，且日期唯一）。
 * 设备副作用断言：经 ErpMntEquipment__get 断言 status 字段翻转（UNDER_MAINTENANCE/RUNNING）——
 * helper 三原语足够，无需扩展（__get 即可断言任意实体状态字段）。
 */

const EQ_ID = 1;
const ASSIGNED_TO = 2;
const VISIT_DATE = '2026-12-25';

test.describe('maintenance ErpMntVisit business action lifecycle', () => {
  test('save(DRAFT) → schedule(SCHEDULED) → start(IN_PROGRESS, equipment UNDER_MAINTENANCE) → complete(COMPLETED, equipment RUNNING)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMntVisit-main');

    const code = `E2E-MNT-VIS-${Date.now()}`;
    const visit = await createViaSave(
      page, 'ErpMntVisit',
      {
        code,
        equipmentId: EQ_ID,
        visitDate: VISIT_DATE,
        status: 'DRAFT',
        assignedTo: ASSIGNED_TO,
        visitType: 'PLANNED',
        orgId: 2,
      },
      'id status',
    );
    expect(visit.id, '__save should create a DRAFT visit').toBeTruthy();
    expect(visit.status, 'new visit status=DRAFT').toBe('DRAFT');

    // schedule: DRAFT → SCHEDULED（前置 assignedTo/visitDate 已填，冲突检查无同日 SCHEDULED/IN_PROGRESS 种子）
    const scheduled = await callMutationOk(
      page, 'ErpMntVisit', 'schedule', { visitId: visit.id }, 'id status',
    );
    expect(scheduled.status, 'schedule should transition DRAFT → SCHEDULED').toBe('SCHEDULED');

    // start: SCHEDULED → IN_PROGRESS + 设备联动副作用（RUNNING → UNDER_MAINTENANCE）
    const started = await callMutationOk(
      page, 'ErpMntVisit', 'start', { visitId: visit.id }, 'id status',
    );
    expect(started.status, 'start should transition SCHEDULED → IN_PROGRESS').toBe('IN_PROGRESS');

    const eqUnderMaint = await verifyState(page, 'ErpMntEquipment', EQ_ID, 'status');
    expect(eqUnderMaint.status, 'start should link equipment to UNDER_MAINTENANCE').toBe('UNDER_MAINTENANCE');

    // complete: IN_PROGRESS → COMPLETED + 设备恢复（UNDER_MAINTENANCE → RUNNING，恢复种子态无污染）
    const completed = await callMutationOk(
      page, 'ErpMntVisit', 'complete', { visitId: visit.id }, 'id status',
    );
    expect(completed.status, 'complete should transition IN_PROGRESS → COMPLETED').toBe('COMPLETED');

    const verified = await verifyState(page, 'ErpMntVisit', visit.id, 'status');
    expect(verified.status, '__get should confirm COMPLETED').toBe('COMPLETED');

    const eqRestored = await verifyState(page, 'ErpMntEquipment', EQ_ID, 'status');
    expect(eqRestored.status, 'complete should restore equipment to RUNNING').toBe('RUNNING');

    // 非法迁移守卫：COMPLETED → start（须 IN_PROGRESS），经 GraphQL 返回 errors（ErrorCode）
    const rej = await callMutation(page, 'ErpMntVisit', 'start', { visitId: visit.id }, 'id');
    expect(rej.errors, 'start from COMPLETED should be rejected as illegal transition').toBeTruthy();

    // 清理：visit 状态机无不可逆下游产物（仅设备状态翻转已恢复种子态），逻辑删除 visit 自身
    await deleteById(page, 'ErpMntVisit', visit.id);
  });

  test('cancel path: save(DRAFT) → schedule(SCHEDULED) → cancel(CANCELLED, equipment RUNNING)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMntVisit-main');

    const code = `E2E-MNT-VIS-CNL-${Date.now()}`;
    const visit = await createViaSave(
      page, 'ErpMntVisit',
      {
        code,
        equipmentId: EQ_ID,
        visitDate: VISIT_DATE,
        status: 'DRAFT',
        assignedTo: ASSIGNED_TO,
        visitType: 'PLANNED',
        orgId: 2,
      },
      'id status',
    );
    expect(visit.status, 'new visit status=DRAFT').toBe('DRAFT');

    const scheduled = await callMutationOk(
      page, 'ErpMntVisit', 'schedule', { visitId: visit.id }, 'id status',
    );
    expect(scheduled.status, 'schedule should transition DRAFT → SCHEDULED').toBe('SCHEDULED');

    // cancel: SCHEDULED（非终态）→ CANCELLED + 设备恢复（SCHEDULED 态未联动设备，cancel 的 restoreToRunning 幂等）
    const cancelled = await callMutationOk(
      page, 'ErpMntVisit', 'cancel', { visitId: visit.id }, 'id status',
    );
    expect(cancelled.status, 'cancel should transition SCHEDULED → CANCELLED').toBe('CANCELLED');

    const verified = await verifyState(page, 'ErpMntVisit', visit.id, 'status');
    expect(verified.status, '__get should confirm CANCELLED').toBe('CANCELLED');

    // 清理
    await deleteById(page, 'ErpMntVisit', visit.id);
  });
});
