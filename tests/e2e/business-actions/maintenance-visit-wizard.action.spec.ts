import { test, expect, loginAndNavigate, createViaSave, callMutationOk, verifyState, deleteById } from './_helper';

/**
 * maintenance ErpMntVisit 执行向导 action E2E（plan 2026-07-23-1145-1 Phase 4）。
 *
 * 浏览器层驱动 wizard 编排的状态机链 + F4 child-table-editor 新增的 tasks 子集合聚合保存能力：
 *   __save(DRAFT + tasks 子集合) → __schedule(SCHEDULED) → __start(IN_PROGRESS) → __save(结果字段 result) → __complete(COMPLETED)
 *
 * 与既有 maintenance-visit.action.spec.ts 的区别：本 spec 聚焦向导编排链 + Phase 1 新增的 tasks 子集合经
 * ErpMntVisit__save 聚合保存可达性（F4 child-table-editor 基线验证），每步 verifyState 断言 status 翻转。
 *
 * 权威状态机（ErpMntVisitBizModel）：DRAFT→SCHEDULED→IN_PROGRESS→COMPLETED（终态）；非终态→CANCELLED。
 *
 * 种子引用（init-data）：equipment id=1（EQ-2026-001，RUNNING）；employee id=2（李四，作为 assignedTo）。
 * visitDate 用未来日 2026-12-26 避开种子 visit（日期唯一，无 SCHEDULED/IN_PROGRESS 冲突）。
 */

const EQ_ID = 1;
const ASSIGNED_TO = 2;
const VISIT_DATE = '2026-12-26';

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
 return `${tag}-${Date.now()}-${_seq}`;
}

test.describe('maintenance visit-wizard action E2E (F4 tasks aggregate-save + wizard chain)', () => {
  test('save(DRAFT + tasks) → schedule → start → save(result) → complete full wizard chain', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMntVisit-main');

    // ── Step 1: __save DRAFT visit 含 tasks 子集合（Phase 1 child-table-editor 聚合保存验证）──
    const code = uniq('E2E-MNT-WIZ');
    const taskCode = uniq('E2E-MNT-WIZ-TASK');
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
        tasks: [
          {
            lineNo: 1,
            taskDescription: taskCode,
            status: 'PENDING',
          },
        ],
      },
      'id status',
    );
    expect(visit.id, '__save with tasks should create a DRAFT visit').toBeTruthy();
    expect(visit.status, 'new visit status=DRAFT').toBe('DRAFT');

    // 验证 tasks 子集合经聚合保存后可达（__get 含 tasks 子集合）
    const withTasks = await verifyState(page, 'ErpMntVisit', visit.id, 'id status tasks{ id lineNo taskDescription status }');
    expect(withTasks.tasks, 'aggregated tasks sub-collection should be persisted').toBeTruthy();
    expect(withTasks.tasks.length, 'one task row should be saved').toBe(1);
    expect(withTasks.tasks[0].taskDescription, 'task description should match').toBe(taskCode);

    try {
      // ── Step 2: schedule（DRAFT → SCHEDULED，wizard Step 1 "开始执行" 前置）──
      const scheduled = await callMutationOk(
        page, 'ErpMntVisit', 'schedule', { visitId: visit.id }, 'id status',
      );
      expect(scheduled.status, 'schedule should transition DRAFT → SCHEDULED').toBe('SCHEDULED');

      // ── Step 3: start（SCHEDULED → IN_PROGRESS + 设备联动，wizard "开始执行" 按钮）──
      const started = await callMutationOk(
        page, 'ErpMntVisit', 'start', { visitId: visit.id }, 'id status',
      );
      expect(started.status, 'start should transition SCHEDULED → IN_PROGRESS').toBe('IN_PROGRESS');

      // ── Step 4: __save 结果字段（wizard Step 3 执行结果录入，result + remark）──
      const GraphQLClient = (await import('../pages')).GraphQLClient;
      const gql = new GraphQLClient(page);
      const savedResult: any = await gql.raw(
        `mutation($d:ErpMntVisit__save_input){ ErpMntVisit__save(data:$d){ id status result remark } }`,
        {
          d: {
            id: visit.id,
            code,
            equipmentId: EQ_ID,
            visitDate: VISIT_DATE,
            status: 'IN_PROGRESS',
            result: 'NORMAL',
            remark: 'wizard-execution-ok',
          },
        },
      );
      expect(savedResult?.errors, '__save result should not return GraphQL errors').toBeFalsy();
      expect(savedResult?.data?.ErpMntVisit__save?.result, 'result should be persisted as NORMAL').toBe('NORMAL');

      // ── Step 5: complete（IN_PROGRESS → COMPLETED + 设备恢复，wizard "确认完成" 按钮）──
      const completed = await callMutationOk(
        page, 'ErpMntVisit', 'complete', { visitId: visit.id }, 'id status',
      );
      expect(completed.status, 'complete should transition IN_PROGRESS → COMPLETED').toBe('COMPLETED');

      const verified = await verifyState(page, 'ErpMntVisit', visit.id, 'status result remark');
      expect(verified.status, '__get should confirm COMPLETED').toBe('COMPLETED');
      expect(verified.result, 'result should remain NORMAL after complete').toBe('NORMAL');
    } finally {
      await deleteById(page, 'ErpMntVisit', visit.id);
    }
  });
});
