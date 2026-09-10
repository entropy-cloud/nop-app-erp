// DOM assertions for F13 — non-standard views (kanban / timeline / calendar / org-chart)
// (plan 2026-08-03-1232-2-flux-f13-nonstandard-views-rewrite.md §Phase 4).
//
// Validates that the 8 F13 pages render flux-native components at runtime:
//   1. prj  task-kanban: flux kanban (4 columns TODO/IN_PROGRESS/DONE/BLOCKED)
//   2. cs   ticket-kanban: flux kanban (6 status columns)
//   3. crm  opportunity-kanban: flux kanban (dynamic stage columns)
//   4. crm  activity-timeline: flux timeline (native items)
//   5. cs   ticket-action-timeline: flux timeline (native items)
//   6. crm  activity-calendar: flux calendar (month view)
//   7. hr   team-vacation-calendar: flux calendar (month view)
//   8. hr   org-chart: flux tree (collapsible department hierarchy)
//
// Flux selector strategy (cs-027-r3 纪律修正): 全部非标视图控件 selector 经 FluxAdapter 取用，
// 本 spec 零内联控件 selector（runbook「E2E 编写规范（强制）」selector 唯一合法位置 = adapter）。
// Row-dependent pages skip gracefully if no seed row.

import { test, expect, loginAndNavigate } from '../fixtures';
import { FluxAdapter } from '../pages';
import type { Locator, Page } from '@playwright/test';

const flux = new FluxAdapter();

async function navigateAndWait(page: Page, route: string, waitRoot?: Locator): Promise<void> {
  await loginAndNavigate(page, route);
  await expect.poll(
    () => page.url(),
    { timeout: 20_000, message: `${route}: URL should include "${route}"` },
  ).toContain(route);
  await (waitRoot ?? flux.nonStandardViewRoot(page)).waitFor({ state: 'visible', timeout: 20_000 });
  await page.waitForTimeout(1500);
}

test.describe('F13 — flux kanban DOM rendering', () => {
  test('projects task-kanban renders flux kanban with columns', async ({ page }) => {
    await navigateAndWait(page, '/prj-task-kanban', flux.kanbanRoot(page).first());
    await expect(page.locator('text=任务看板').first()).toBeVisible({ timeout: 10_000 });
    await expect(flux.kanbanRoot(page).first()).toBeVisible({ timeout: 10_000 });
    const colCount = await flux.kanbanColumn(page).count();
    expect(colCount, 'task-kanban should render >=1 kanban columns').toBeGreaterThanOrEqual(1);
  });

  test('cs ticket-kanban renders flux kanban with columns', async ({ page }) => {
    await navigateAndWait(page, '/cs-ticket-kanban', flux.kanbanRoot(page).first());
    await expect(page.locator('text=工单看板').first()).toBeVisible({ timeout: 10_000 });
    await expect(flux.kanbanRoot(page).first()).toBeVisible({ timeout: 10_000 });
    const colCount = await flux.kanbanColumn(page).count();
    expect(colCount, 'ticket-kanban should render >=1 kanban columns').toBeGreaterThanOrEqual(1);
  });

  test('crm opportunity-kanban renders flux kanban or empty placeholder', async ({ page }) => {
    await navigateAndWait(page, '/crm-opportunity-kanban',
      flux.kanbanRoot(page).first().or(flux.kanbanEmpty(page).first()));
    await expect(page.locator('text=商机看板').first()).toBeVisible({ timeout: 10_000 });
    const kanbanCount = await flux.kanbanRoot(page).count().catch(() => 0);
    const emptyCount = await flux.kanbanEmpty(page).count().catch(() => 0);
    expect(kanbanCount + emptyCount, 'opportunity-kanban should render kanban or empty placeholder').toBeGreaterThan(0);
  });
});

test.describe('F13 — flux timeline DOM rendering', () => {
  test('crm activity-timeline renders flux timeline', async ({ page }) => {
    await navigateAndWait(page, '/crm-activity-timeline', flux.timelineRoot(page).first());
    await expect(page.locator('text=活动时间线').first()).toBeVisible({ timeout: 10_000 });
    await expect(flux.timelineRoot(page).first()).toBeVisible({ timeout: 10_000 });
  });

  test('cs ticket-action-timeline renders flux timeline', async ({ page }) => {
    await navigateAndWait(page, '/cs-action-log', flux.timelineRoot(page).first());
    await expect(page.locator('text=工单操作').first()).toBeVisible({ timeout: 10_000 });
    await expect(flux.timelineRoot(page).first()).toBeVisible({ timeout: 10_000 });
  });
});

test.describe('F13 — flux calendar DOM rendering', () => {
  test('crm activity-calendar renders flux calendar', async ({ page }) => {
    await navigateAndWait(page, '/crm-activity-calendar', flux.calendarRoot(page).first());
    await expect(page.locator('text=活动日历').first()).toBeVisible({ timeout: 10_000 });
    await expect(flux.calendarRoot(page).first()).toBeVisible({ timeout: 10_000 });
  });

  test('hr team-vacation-calendar renders flux calendar', async ({ page }) => {
    await navigateAndWait(page, '/hr-team-vacation-calendar', flux.calendarRoot(page).first());
    await expect(page.locator('text=团队休假日历').first()).toBeVisible({ timeout: 10_000 });
    await expect(flux.calendarRoot(page).first()).toBeVisible({ timeout: 10_000 });
  });
});

test.describe('F13 — flux tree DOM rendering (org-chart)', () => {
  test('hr org-chart renders flux tree', async ({ page }) => {
    await navigateAndWait(page, '/hr-org-chart', flux.treeRoot(page).first());
    await expect(page.locator('text=组织架构图').first()).toBeVisible({ timeout: 10_000 });
    await expect(flux.treeRoot(page).first()).toBeVisible({ timeout: 10_000 });
    const nodeCount = await flux.treeNode(page).count();
    expect(nodeCount, 'org-chart should render >=1 tree nodes').toBeGreaterThanOrEqual(1);
  });
});
