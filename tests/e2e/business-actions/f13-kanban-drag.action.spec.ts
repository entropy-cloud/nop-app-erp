// Action spec for F13 — projects task kanban state machine (startTask / completeTask / blockTask / unblockTask)
// (plan 2026-07-22-0845-3-f13-non-standard-views-kanban-timeline-calendar.md §Phase 4).
//
// Phase 0 Explore decisions covered:
//   (a) dragdrop degraded → row-action state-machine buttons (no native AMIS cross-crud drag)
//   (c) playwright dragTo PoC skipped → action spec covers row-action path instead of drag path
//   (d) existing mutations cover all state changes (no new BizModel delta)
//
// Validates the ErpPrjTask state-machine row-action path end-to-end via GraphQL:
//   1. Find a TODO task (or skip if none seeded — codegen-level coverage via ErpAllWebPagesTest)
//   2. startTask: TODO → IN_PROGRESS (assert status flip)
//   3. completeTask: IN_PROGRESS → DONE (assert status flip)
//   4. (Separate scenario) blockTask with empty blockReason → expect ERR_TASK_BLOCK_REASON_REQUIRED
//   5. blockTask with blockReason → BLOCKED; unblockTask → IN_PROGRESS
//
// Uses raw GraphQL mutation calls (same pattern as f16-template-preview.action.spec.ts) — no UI
// interaction required since row-action buttons are direct GraphQL triggers (validated by visual spec).
// State machine guard enforcement is at the BizModel layer; this spec covers the contract.

import { test, expect, loginAndNavigate, GraphQLClient } from './_helper';

interface Task {
  id: number;
  status: string;
  title: string;
}

async function findTodoTask(page: import('@playwright/test').Page): Promise<Task | null> {
  const gql = new GraphQLClient(page);
  const json: any = await gql.raw(
    `query{ ErpPrjTask__findPage(filter_status:"TODO",limit:1){ items{ id status title } total } }`,
    {},
  );
  const items = json?.data?.ErpPrjTask__findPage?.items ?? [];
  return items.length > 0 ? items[0] : null;
}

async function fetchTaskStatus(page: import('@playwright/test').Page, taskId: number): Promise<string | null> {
  const gql = new GraphQLClient(page);
  const json: any = await gql.raw(
    `query(${'$'}id:Long){ ErpPrjTask__get(id:${'$'}id){ id status } }`,
    { id: taskId },
  );
  return json?.data?.ErpPrjTask__get?.status ?? null;
}

async function startTask(page: import('@playwright/test').Page, taskId: number): Promise<any> {
  const gql = new GraphQLClient(page);
  return gql.raw(
    `mutation(${'$'}id:Long){ ErpPrjTask__startTask(taskId:${'$'}id){ id status } }`,
    { id: taskId },
  );
}

async function completeTask(page: import('@playwright/test').Page, taskId: number): Promise<any> {
  const gql = new GraphQLClient(page);
  return gql.raw(
    `mutation(${'$'}id:Long){ ErpPrjTask__completeTask(taskId:${'$'}id){ id status } }`,
    { id: taskId },
  );
}

async function blockTask(page: import('@playwright/test').Page, taskId: number, blockReason: string): Promise<any> {
  const gql = new GraphQLClient(page);
  return gql.raw(
    `mutation(${'$'}id:Long,${'$'}r:String){ ErpPrjTask__blockTask(taskId:${'$'}id,blockReason:${'$'}r){ id status } }`,
    { id: taskId, r: blockReason },
  );
}

async function unblockTask(page: import('@playwright.test').Page, taskId: number): Promise<any> {
  const gql = new GraphQLClient(page);
  return gql.raw(
    `mutation(${'$'}id:Long){ ErpPrjTask__unblockTask(taskId:${'$'}id){ id status } }`,
    { id: taskId },
  );
}

test.describe('F13 projects task kanban state machine (row-action path)', () => {
  test('startTask + completeTask: TODO → IN_PROGRESS → DONE', async ({ page }) => {
    await loginAndNavigate(page, '/prj-task-kanban');
    const todo = await findTodoTask(page);
    if (!todo) {
      test.skip(true, 'no seed TODO task — codegen-level coverage via ErpAllWebPagesTest');
      return;
    }

    // startTask: TODO → IN_PROGRESS
    const startJson = await startTask(page, todo.id);
    expect(startJson.errors, `startTask should succeed: ${JSON.stringify(startJson.errors)}`).toBeFalsy();
    expect(startJson.data.ErpPrjTask__startTask.status).toBe('IN_PROGRESS');

    // Verify persisted
    const afterStart = await fetchTaskStatus(page, todo.id);
    expect(afterStart).toBe('IN_PROGRESS');

    // completeTask: IN_PROGRESS → DONE
    const completeJson = await completeTask(page, todo.id);
    expect(completeJson.errors, `completeTask should succeed: ${JSON.stringify(completeJson.errors)}`).toBeFalsy();
    expect(completeJson.data.ErpPrjTask__completeTask.status).toBe('DONE');

    // Verify persisted
    const afterComplete = await fetchTaskStatus(page, todo.id);
    expect(afterComplete).toBe('DONE');
  });

  test('blockTask guard: empty blockReason rejected with ERR_TASK_BLOCK_REASON_REQUIRED', async ({ page }) => {
    await loginAndNavigate(page, '/prj-task-kanban');
    // Find an IN_PROGRESS task (or skip if none)
    const gql = new GraphQLClient(page);
    const findJson: any = await gql.raw(
      `query{ ErpPrjTask__findPage(filter_status:"IN_PROGRESS",limit:1){ items{ id status title } } }`,
      {},
    );
    const inProgress = findJson?.data?.ErpPrjTask__findPage?.items ?? [];
    if (inProgress.length === 0) {
      test.skip(true, 'no seed IN_PROGRESS task for blockTask guard — codegen-level coverage');
      return;
    }
    const taskId = inProgress[0].id;

    // blockTask with empty reason → expect GraphQL error containing ERR_TASK_BLOCK_REASON_REQUIRED
    const blockJson = await blockTask(page, taskId, '');
    expect(blockJson.errors, 'empty blockReason should trigger GraphQL error').toBeTruthy();
    const errMsg = JSON.stringify(blockJson.errors);
    expect(errMsg).toMatch(/ERR_TASK_BLOCK_REASON_REQUIRED|blockReason|阻塞原因/);
  });

  test('blockTask + unblockTask roundtrip with valid blockReason', async ({ page }) => {
    await loginAndNavigate(page, '/prj-task-kanban');
    const gql = new GraphQLClient(page);
    const findJson: any = await gql.raw(
      `query{ ErpPrjTask__findPage(filter_status:"IN_PROGRESS",limit:1){ items{ id status title } } }`,
      {},
    );
    const inProgress = findJson?.data?.ErpPrjTask__findPage?.items ?? [];
    if (inProgress.length === 0) {
      test.skip(true, 'no seed IN_PROGRESS task for blockTask roundtrip — codegen-level coverage');
      return;
    }
    const taskId = inProgress[0].id;

    // blockTask with valid reason → BLOCKED
    const blockJson = await blockTask(page, taskId, `F13 action spec 阻塞原因 @${Date.now()}`);
    expect(blockJson.errors, `blockTask should succeed with reason: ${JSON.stringify(blockJson.errors)}`).toBeFalsy();
    expect(blockJson.data.ErpPrjTask__blockTask.status).toBe('BLOCKED');

    // unblockTask: BLOCKED → IN_PROGRESS
    const unblockJson = await unblockTask(page, taskId);
    expect(unblockJson.errors, `unblockTask should succeed: ${JSON.stringify(unblockJson.errors)}`).toBeFalsy();
    expect(unblockJson.data.ErpPrjTask__unblockTask.status).toBe('IN_PROGRESS');
  });
});
