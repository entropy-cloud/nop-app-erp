import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, deleteById } from './_helper';

/**
 * projects ErpPrjTask 业务动作浏览器层 E2E（plan 2026-07-09-2004-1 Phase 2）。
 *
 * 验证自定义 @BizMutation（startTask/completeTask/blockTask/unblockTask）经 GraphQL /graphql 的全栈可达性 +
 * 4 态状态机迁移 + 非法迁移守卫 + 前驱 DAG 门控规避。
 *
 * 权威状态机（ErpPrjTaskBizModel + ErpPrjConstants + task-dag.md）：TASK 4 态
 *   TODO --startTask--> IN_PROGRESS --completeTask--> DONE（终态）
 *   IN_PROGRESS --blockTask(blockReason)--> BLOCKED --unblockTask--> IN_PROGRESS
 * startTask 前驱 DAG 门控：config `erp-prj.task-strict-predecessor-check` 默认 STRICT（未完成前置抛
 * ERR_TASK_PREDECESSOR_NOT_DONE），WARN 模式 log+proceed。本 spec 创建无前驱任务（dependsOnId=null）
 * 绕过门控（validatePredecessorDone 在 dependsOnId==null 时直接放行）。
 *
 * 实现裁决：mandatory 字段经 ORM 核实为 projectId+title+status（注意：实体无 code/name 列，标题字段为
 * `title` domain=taskTitle，与 plan 草稿「code/name」表述不一致，以 ORM 权威源为准）。唯一性由 title 自然区分。
 *
 * 种子引用（init-data）：project id=1（PRJ-2026-001 华东科技 ERP 实施项目，OPEN）。
 * 清理：task 状态机无下游产物，逻辑删除 task 自身。
 */

const PROJECT_ID = 1;

test.describe('projects ErpPrjTask state machine actions', () => {
  test('save(TODO) → startTask(IN_PROGRESS) → completeTask(DONE) + illegal transition guard', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPrjTask-main');

    const task = await createViaSave(
      page, 'ErpPrjTask',
      {
        title: `E2E-PRJ-TASK-${Date.now()}`,
        projectId: PROJECT_ID,
        status: 'TODO',
        priority: 'NORMAL',
      },
      'id status',
    );
    expect(task.id, '__save should create a TODO task').toBeTruthy();
    expect(task.status, 'new task status=TODO').toBe('TODO');

    // startTask: TODO → IN_PROGRESS（无前驱 dependsOnId=null 绕过 STRICT 门控）
    const started = await callMutationOk(
      page, 'ErpPrjTask', 'startTask', { taskId: task.id }, 'id status',
    );
    expect(started.status, 'startTask should transition TODO → IN_PROGRESS').toBe('IN_PROGRESS');

    // completeTask: IN_PROGRESS → DONE
    const completed = await callMutationOk(
      page, 'ErpPrjTask', 'completeTask', { taskId: task.id }, 'id status',
    );
    expect(completed.status, 'completeTask should transition IN_PROGRESS → DONE').toBe('DONE');

    const verified = await verifyState(page, 'ErpPrjTask', task.id, 'status');
    expect(verified.status, '__get should confirm DONE').toBe('DONE');

    // 非法迁移守卫：DONE → startTask（须 TODO），经 GraphQL 返回 errors（ErrorCode）
    const rej = await callMutation(page, 'ErpPrjTask', 'startTask', { taskId: task.id }, 'id');
    expect(rej.errors, 'startTask from DONE should be rejected as illegal transition').toBeTruthy();

    // 清理
    await deleteById(page, 'ErpPrjTask', task.id);
  });

  test('block/unblock cycle: save(TODO) → startTask → blockTask(BLOCKED) → unblockTask(IN_PROGRESS)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPrjTask-main');

    const task = await createViaSave(
      page, 'ErpPrjTask',
      {
        title: `E2E-PRJ-BLK-${Date.now()}`,
        projectId: PROJECT_ID,
        status: 'TODO',
        priority: 'HIGH',
      },
      'id status',
    );

    await callMutationOk(page, 'ErpPrjTask', 'startTask', { taskId: task.id }, 'id status');

    // blockTask: IN_PROGRESS → BLOCKED（blockReason 必填非空）
    const blocked = await callMutationOk(
      page, 'ErpPrjTask', 'blockTask',
      { taskId: task.id, blockReason: 'E2E waiting on external dependency' },
      'id status blockReason',
    );
    expect(blocked.status, 'blockTask should transition IN_PROGRESS → BLOCKED').toBe('BLOCKED');
    expect(blocked.blockReason, 'blockTask should record blockReason').toBeTruthy();

    // unblockTask: BLOCKED → IN_PROGRESS（清 blockReason）
    const unblocked = await callMutationOk(
      page, 'ErpPrjTask', 'unblockTask', { taskId: task.id }, 'id status blockReason',
    );
    expect(unblocked.status, 'unblockTask should transition BLOCKED → IN_PROGRESS').toBe('IN_PROGRESS');

    const verified = await verifyState(page, 'ErpPrjTask', task.id, 'status blockReason');
    expect(verified.status, '__get should confirm IN_PROGRESS').toBe('IN_PROGRESS');

    // 非法迁移守卫：blockTask 须 IN_PROGRESS，现已是 IN_PROGRESS 但先 complete 到 DONE 再 block 验证守卫
    await callMutationOk(page, 'ErpPrjTask', 'completeTask', { taskId: task.id }, 'id status');
    const rej = await callMutation(
      page, 'ErpPrjTask', 'blockTask',
      { taskId: task.id, blockReason: 'should be rejected' },
      'id',
    );
    expect(rej.errors, 'blockTask from DONE should be rejected as illegal transition').toBeTruthy();

    // 清理
    await deleteById(page, 'ErpPrjTask', task.id);
  });
});
