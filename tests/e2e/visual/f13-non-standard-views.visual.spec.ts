// DOM assertions for F13 — non-standard views (kanban / timeline / calendar)
// (plan 2026-07-22-0845-3-f13-non-standard-views-kanban-timeline-calendar.md §Phase 4).
//
// Validates that the 7 F13 pages render their core interaction DOM at runtime:
//   1. crm  opportunity-kanban: dynamic stage columns (service + each + crud per stage)
//   2. cs   ticket-kanban: 6 fixed status columns
//   3. prj  task-kanban: 4 status columns (TODO/IN_PROGRESS/DONE/BLOCKED)
//   4. crm  activity-timeline: each + tpl custom timeline (native timeline prop contract failed → 降级)
//   5. cs   ticket-action-timeline: each + tpl custom timeline
//   6. crm  activity-calendar: each + tpl date-grouped cards (native calendar React error → 降级)
//   7. hr   team-vacation-calendar: custom matrix table
//
// DOM-className + page-title text strategy (stable across AMIS upgrades) — same as f16 visual spec.
// Row-dependent pages skip gracefully if no seed row (codegen-level coverage via ErpAllWebPagesTest).
//
// Phase 0 Explore decisions covered:
//   (a) dragdrop degraded → row-action buttons (no native AMIS cross-crud drag)
//   (b) timeline/calendar: native components exist in bundle but runtime prop/render issues
//       → implementation-time downgrade to each + tpl / matrix table (Closure裁决)
//   (c) playwright dragTo PoC skipped (relies on (a) downgrade)
//   (d) existing mutations cover all state changes (no new BizModel delta)

import { test, expect, loginAndNavigate } from '../fixtures';
import type { Page } from '@playwright/test';

async function navigateAndWait(page: Page, route: string, waitForSelector = '.cxd-Crud, .cxd-Service, .cxd-Table'): Promise<void> {
  await loginAndNavigate(page, route);
  await expect.poll(
    () => page.url(),
    { timeout: 20_000, message: `${route}: URL should include "${route}"` },
  ).toContain(route);
  await page.waitForSelector(waitForSelector, { timeout: 20_000 });
  await page.waitForTimeout(1500);
}

test.describe('F13 — kanban DOM rendering', () => {
  test('projects task-kanban renders 4 status columns', async ({ page }) => {
    await navigateAndWait(page, '/prj-task-kanban');
    // 4 cruds (TODO/IN_PROGRESS/DONE/BLOCKED)
    await expect.poll(
      () => page.locator('.cxd-Crud').count(),
      { timeout: 20_000, message: 'task-kanban should render 4 status cruds' },
    ).toBeGreaterThanOrEqual(4);
    // Page title present
    await expect(page.locator('text=任务看板').first()).toBeVisible({ timeout: 10_000 });
    // Column header text present (each column has its title rendered as text in AMIS crud header)
    await expect(page.locator('text=待开始').first()).toBeVisible({ timeout: 10_000 });
    await expect(page.locator('text=阻塞').first()).toBeVisible({ timeout: 10_000 });
  });

  test('cs ticket-kanban renders 6 status columns', async ({ page }) => {
    await navigateAndWait(page, '/cs-ticket-kanban');
    await expect.poll(
      () => page.locator('.cxd-Crud').count(),
      { timeout: 20_000, message: 'ticket-kanban should render 6 status cruds' },
    ).toBeGreaterThanOrEqual(6);
    await expect(page.locator('text=工单看板').first()).toBeVisible({ timeout: 10_000 });
    await expect(page.locator('text=新建').first()).toBeVisible({ timeout: 10_000 });
    await expect(page.locator('text=已关闭').first()).toBeVisible({ timeout: 10_000 });
  });

  test('crm opportunity-kanban renders dynamic stage columns or placeholder', async ({ page }) => {
    await navigateAndWait(page, '/crm-opportunity-kanban');
    await expect(page.locator('text=商机看板').first()).toBeVisible({ timeout: 10_000 });
    // Either: dynamic stage columns render (>=1 crud) OR placeholder shows (no ErpCrmStage seed)
    const crudCount = await page.locator('.cxd-Crud').count().catch(() => 0);
    const placeholderVisible = await page.locator('text=尚未配置商机阶段').count().catch(() => 0);
    expect(
      crudCount + placeholderVisible,
      'opportunity-kanban should render either stage cruds or empty placeholder',
    ).toBeGreaterThan(0);
  });
});

test.describe('F13 — timeline DOM rendering (custom each+tpl)', () => {
  test('crm activity-timeline renders page + service container', async ({ page }) => {
    await navigateAndWait(page, '/crm-activity-timeline');
    await expect(page.locator('text=活动时间线').first()).toBeVisible({ timeout: 10_000 });
    // Service container rendered (always present), timeline content (each) renders if seed data
    await expect(page.locator('.cxd-Service').first()).toBeVisible({ timeout: 10_000 });
  });

  test('cs ticket-action-timeline renders page + service container', async ({ page }) => {
    await navigateAndWait(page, '/cs-action-log');
    await expect(page.locator('text=工单操作').first()).toBeVisible({ timeout: 10_000 });
    await expect(page.locator('.cxd-Service').first()).toBeVisible({ timeout: 10_000 });
  });
});

test.describe('F13 — calendar DOM rendering', () => {
  test('crm activity-calendar renders date-grouped cards page', async ({ page }) => {
    await navigateAndWait(page, '/crm-activity-calendar');
    await expect(page.locator('text=活动日历').first()).toBeVisible({ timeout: 10_000 });
    await expect(page.locator('.cxd-Service').first()).toBeVisible({ timeout: 10_000 });
  });

  test('hr team-vacation-calendar renders custom matrix Table', async ({ page }) => {
    await navigateAndWait(page, '/hr-team-vacation-calendar');
    await expect(page.locator('text=团队休假日历').first()).toBeVisible({ timeout: 10_000 });
    // Custom matrix renders as either .cxd-Table (seed data) or service container (loading/empty)
    const tableCount = await page.locator('.cxd-Table').count().catch(() => 0);
    const hasService = await page.locator('.cxd-Service').count().catch(() => 0);
    expect(tableCount + hasService, 'matrix calendar should render table or service container').toBeGreaterThan(0);
    // Legend present (年假/病假/事假)
    await expect(page.locator('text=年假').first()).toBeVisible({ timeout: 10_000 });
  });
});
