import { test, expect, loginAndNavigate } from '../fixtures';
import type { Page, Locator } from '@playwright/test';

/**
 * AI 截屏仅诊断不裁决——本文件所有 assertSnapshot / assertXxxPixelSnapshot 调用结果为 pass/fail 唯一裁决依据。
 * CI 中任何 toHaveScreenshot 失败由独立子代理 plan-audit 复核根因，不得由 AI 主观判定。
 */

// ----------------------------------------------------------------------------
// Pixel-snapshot layer (plan 2026-07-17-2010-2)
// ----------------------------------------------------------------------------
//
// Phase 1 exploration (see _exploration/) proved cross-run pixel-exact
// stability on macOS + Chrome (channel: 'chrome') + system fonts, with
// maxDiffPixels: 0 passing on every variant tested. The configuration below
// codifies the selected approach so every snapshot assertion in this layer
// shares the same font hardening, mask convention, tolerance, and echarts
// settle behavior.

/**
 * Explicit font chain injected before every snapshot. Defends against future
 * environment drift (different CI images, missing CJK fallbacks). On the
 * current macOS + Chrome baseline it produces 0 cross-run diff even without
 * the injection, so this is belt-and-suspenders.
 */
const SNAPSHOT_FONT_CHAIN = `
  *, *::before, *::after {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "PingFang SC", "Microsoft YaHei", sans-serif !important;
  }
`;

/**
 * Default per-snapshot tolerance. Phase 1 measured 0 diff on the strict
 * maxDiffPixels: 0 setting; 1% ratio is a generous CI environment drift
 * absorber and is still far tighter than any real layout regression (CSS
 * misalignment / element overlap / canvas collapse all exceed 1%).
 */
const SNAPSHOT_MAX_DIFF_PIXEL_RATIO = 0.01;

/**
 * Let echarts animation settle before any snapshot capture. Default echarts
 * animation is ~1s; networkidle + 1500ms grace covers the tail.
 */
async function waitForEchartsSettle(page: Page): Promise<void> {
  await page.waitForLoadState('networkidle', { timeout: 10_000 }).catch(() => {});
  await page.waitForTimeout(1500);
}

export interface SnapshotOptions {
  /** Snapshot file name (Playwright will add the `-chromium-darwin.png`
   * suffix). Pass a stable, descriptive name. */
  name: string;
  /** Additional locators to mask beyond the canonical header + canvas set. */
  mask?: Locator[];
  /** Override the default 1% ratio tolerance. */
  maxDiffPixelRatio?: number;
  /** Skip the canonical font-chain injection (rare; only when the page's own
   * fonts are the subject of the assertion). */
  skipFontHardening?: boolean;
  /** Skip the canonical echarts settle wait (use for non-chart pages where
   * networkidle is sufficient). */
  skipEchartsSettle?: boolean;
}

/**
 * Canonical snapshot assertion primitive. Wraps `expect(page).toHaveScreenshot`
 * with the Phase 1 selected approach: font hardening + echarts settle +
 * canonical mask (header + all canvases) + 1% ratio tolerance.
 *
 * Canonical mask targets the two known dynamic regions identified in Phase 1:
 *   - `header` — contains user name / avatar / current-user text that varies
 *      per session.
 *   - `canvas` — echarts draws to canvas; final animation frame timing has
 *      inherent cross-environment drift risk even when this OS is stable.
 *
 * Callers can pass additional `mask` locators for page-specific dynamic
 * regions (e.g. report `${NOW()}` date-stamp containers).
 */
export async function assertSnapshot(page: Page, opts: SnapshotOptions): Promise<void> {
  if (!opts.skipEchartsSettle) {
    await waitForEchartsSettle(page);
  }
  if (!opts.skipFontHardening) {
    await page.addStyleTag({ content: SNAPSHOT_FONT_CHAIN });
    await page.waitForTimeout(300);
  }
  const canonicalMask: Locator[] = [
    page.locator('header').first(),
    page.locator('canvas'),
  ];
  const mask = [...canonicalMask, ...(opts.mask ?? [])];
  await expect(page).toHaveScreenshot(opts.name, {
    mask,
    maxDiffPixelRatio: opts.maxDiffPixelRatio ?? SNAPSHOT_MAX_DIFF_PIXEL_RATIO,
  });
}

// ----------------------------------------------------------------------------

/** Options for the CRUD pixel-snapshot subset (plan 2026-09-03-0400-1 M2.1).
 * `name` is required; everything else mirrors SnapshotOptions defaults. */
export interface CrudPixelSnapshotOptions extends Omit<SnapshotOptions, 'skipEchartsSettle'> {
  /** CRUD target pages are table/form surfaces without charts by default, so
   * the canonical echarts settle wait is skipped. Pass false for a CRUD page
   * that embeds a canvas chart. */
  skipEchartsSettle?: boolean;
}

/**
 * CRUD pixel-snapshot subset. Reuses the assertSnapshot paradigm (font
 * hardening + canonical mask header/canvas + 1% ratio tolerance) with
 * `skipEchartsSettle` defaulting to true — CRUD list/add-form/drawer pages
 * have no echarts canvas, and the page-driving layer (CrudListPage waits +
 * networkidle) already synchronizes on data load. Additive-only per the M0.3
 * §7 frozen-helpers rule: assertSnapshot and the DOM assertXxxRendered
 * helpers are referenced, never modified.
 */
export async function assertCrudPixelSnapshot(page: Page, opts: CrudPixelSnapshotOptions): Promise<void> {
  await assertSnapshot(page, { ...opts, skipEchartsSettle: opts.skipEchartsSettle ?? true });
}

// ----------------------------------------------------------------------------

/** Options for the business-action pixel-snapshot subset (plan 2026-09-03-0400-2 M2.2). */
export interface BusinessActionPixelSnapshotOptions extends Omit<SnapshotOptions, 'skipEchartsSettle'> {
  /** Business-action surfaces (confirm dialogs / drawers / post-execution lists)
   * have no charts by default, so the canonical echarts settle wait is skipped. */
  skipEchartsSettle?: boolean;
  /** Mode C wait strategy: selector that must become visible before capture
   * (the sonner rejection toast, `[data-sonner-toast]`). A timeout THROWS —
   * the rejection rendering is part of the asserted behavior, a missing toast
   * is a failure, not a skip. */
  waitForSelector?: string;
  /** Timeout for waitForSelector (default 8s — covers the mutation round-trip). */
  waitForSelectorTimeout?: number;
  /** Mode B settle: wait until the success toast has auto-dismissed before
   * capturing the post-execution list state (best-effort, 10s cap). */
  waitToastGone?: boolean;
}

/**
 * Business-action pixel-snapshot subset. Reuses the assertSnapshot paradigm
 * (font hardening + canonical mask header/canvas + 1% ratio tolerance) with
 * mode-specific wait strategies: mode A (dialog open) needs none; mode B
 * (post-execution) waits the success toast gone; mode C (guard rejection)
 * waits the error toast visible and fails if it never renders. Additive-only
 * per the M0.3 §7 frozen-helpers rule: assertSnapshot / assertCrudPixelSnapshot
 * are referenced, never modified.
 */
export async function assertBusinessActionPixelSnapshot(
  page: Page,
  opts: BusinessActionPixelSnapshotOptions,
): Promise<void> {
  if (opts.waitForSelector) {
    await page
      .locator(opts.waitForSelector)
      .first()
      .waitFor({ state: 'visible', timeout: opts.waitForSelectorTimeout ?? 8_000 });
  }
  if (opts.waitToastGone) {
    await page
      .locator('[data-sonner-toast]')
      .first()
      .waitFor({ state: 'hidden', timeout: 10_000 })
      .catch(() => {});
  }
  await assertSnapshot(page, { ...opts, skipEchartsSettle: opts.skipEchartsSettle ?? true });
}

// ----------------------------------------------------------------------------

export interface DashboardVisualAssertion {
  domain: string;
  route: string;
  expectedKpiTokens?: string[];
  hasChart: boolean;
  alertTable: boolean;
  /** Fillable filter inputs (input-text / input-number), keyed by field name. */
  filterValues?: Record<string, string>;
  /** Flux date-picker filters, keyed by the trigger's aria-label (field label).
   * The picker is a button + calendar popover (no fillable input), so dates are
   * chosen by driving the calendar day buttons (aria-label "YYYY年M月D日 星期X"). */
  filterDates?: Record<string, string>;
  /** GraphQL action name whose response marks the post-reload KPI ready.
   * Defaults to `getDashboardKpi`. */
  kpiAction?: string;
}

async function kpiSpanTexts(page: Page): Promise<string[]> {
  // Flux renders KPI values as real <h3> tags (text tag: h3) inside the
  // `.border.rounded.p-3` cards; `span.h3` is the legacy AMIS markup.
  return page.locator('.border.rounded.p-3 h3, .border.rounded.p-3 span.h3').allTextContents();
}

/** Pick a date in the flux calendar popover (react-day-picker day buttons carry
 * a full-date aria-label, month nav buttons are 前往上个月/前往下个月). Exported
 * additively (plan 2026-09-01-0527-2) for sibling snapshot specs; frozen helpers
 * (assertSnapshot / assertDashboardRendered / assertReportRendered) untouched. */
export async function pickFluxDate(page: Page, label: string, value: string): Promise<void> {
  const [y, mo, d] = value.split('-').map(Number);
  const target = `${y}年${mo}月${d}日`;
  const trigger = page.locator(`[data-testid="date-trigger"][aria-label="${label}"]`).first();
  await trigger.click();
  const popover = page.locator('[data-testid="date-popover"]');
  await popover.waitFor({ state: 'visible', timeout: 5_000 });

  for (let i = 0; i < 24; i++) {
    const day = popover.locator(`button[aria-label^="${target} "]`);
    if (await day.count() > 0) {
      await day.first().click();
      await popover.waitFor({ state: 'hidden', timeout: 5_000 }).catch(() => {});
      return;
    }
    // Detect the DISPLAYED month via the day-15 cell: leading/trailing grid
    // cells belong to adjacent months, but day 15 always belongs to the
    // displayed one.
    const probe = await popover.locator('button').filter({ hasText: /^15$/ }).first().getAttribute('aria-label');
    const m = probe?.match(/(\d{4})年(\d{1,2})月/);
    if (!m) throw new Error(`Cannot read displayed month for date picker "${label}"`);
    const cur = Number(m[1]) * 12 + Number(m[2]);
    const tgt = y * 12 + mo;
    const nav = tgt < cur ? '前往上个月' : '前往下个月';
    await popover.locator(`button[aria-label="${nav}"]`).click();
    await page.waitForTimeout(250);
  }
  throw new Error(`Date ${value} not reachable in the "${label}" calendar`);
}

export function assertDashboardRendered(cfg: DashboardVisualAssertion): void {
  test.describe(`${cfg.domain} dashboard AMIS render`, () => {
    test('renders KPI cards + echarts canvas + alert table via AMIS GraphQL pipeline', async ({ page }) => {
      const kpiAction = cfg.kpiAction ?? 'getDashboardKpi';

      // Initial load (default/empty filters) — captures the dashboard data
      // pipeline integrity. A mangled `$var` still returns HTTP 200 here, so
      // this alone does not prove values; the token assertions below do.
      // Dashboards call the backend via REST (`/r/<Biz>__<action>`), not GraphQL.
      const initialResponsePromise = page.waitForResponse(
        (resp) => {
          if (!resp.url().includes('/r/')) return false;
          return resp.url().includes(kpiAction) || decodeURIComponent(resp.url()).includes(kpiAction);
        },
        { timeout: 30_000 },
      );

      await loginAndNavigate(page, cfg.route);
      await initialResponsePromise;

      // Deterministic filtered reload: set the filter fields, then wait for the
      // auto-reload they trigger. No button click is needed (and clicking is
      // unreliable): the data-source args templates read `filterForm?.x`, flux
      // dependency-tracking treats those reads as dependencies, and the
      // valuesPath publish after each field change re-dispatches the source
      // (debounced ~4s). Register the response listener BEFORE the fills —
      // the click-triggered refreshSource after a fill-triggered reload is
      // deduped and would never fire a new request.
      const hasDateFilters = cfg.filterDates && Object.keys(cfg.filterDates).length > 0;
      const hasValueFilters = cfg.filterValues && Object.keys(cfg.filterValues).length > 0;
      if (hasDateFilters || hasValueFilters) {
        const reloadResponsePromise = page.waitForResponse(
          (resp) => {
            if (!resp.url().includes('/r/')) return false;
            return resp.url().includes(kpiAction) || decodeURIComponent(resp.url()).includes(kpiAction);
          },
          { timeout: 30_000 },
        );
        if (hasValueFilters) {
          for (const [name, value] of Object.entries(cfg.filterValues!)) {
            await page.locator(`input[name="${name}"]`).first().fill(value);
          }
        }
        if (hasDateFilters) {
          for (const [label, value] of Object.entries(cfg.filterDates!)) {
            await pickFluxDate(page, label, value);
          }
        }
        // The fills are debounced; the first caught response may still carry a
        // partial filter set. The token assertions below poll to the final
        // values, so this wait only synchronizes on the reload pipeline.
        await reloadResponsePromise;
      }

      const kpiCards = page.locator('.border.rounded.p-3');
      await expect(
        kpiCards.first(),
        `${cfg.domain} KPI cards should render`,
      ).toBeVisible({ timeout: 15_000 });
      expect(
        await kpiCards.count(),
        `${cfg.domain} should render multiple KPI cards`,
      ).toBeGreaterThanOrEqual(1);

      if (cfg.expectedKpiTokens && cfg.expectedKpiTokens.length > 0) {
        for (const tok of cfg.expectedKpiTokens) {
          await expect.poll(
            async () => (await kpiSpanTexts(page)).join('||'),
            { timeout: 20_000, message: `${cfg.domain} KPI span.h3 should render token "${tok}"` },
          ).toContain(tok);
        }
      }

      if (cfg.hasChart) {
        // Flux renders charts with recharts (SVG); legacy AMIS used echarts canvas.
        const chart = page.locator('canvas, svg.recharts-surface').first();
        await expect(
          chart,
          `${cfg.domain} chart should render`,
        ).toBeVisible({ timeout: 20_000 });
        const box = await chart.boundingBox();
        expect(
          box !== null && box.width > 0 && box.height > 0,
          `${cfg.domain} chart should have non-zero size`,
        ).toBe(true);
      }

      if (cfg.alertTable) {
        await expect(
          page.locator('table').first(),
          `${cfg.domain} alert crud table should render`,
        ).toBeVisible({ timeout: 15_000 });
      }
    });
  });
}

export interface ReportVisualAssertion {
  reportLabel: string;
  route: string;
  expectedTokens: string[];
  /** Fillable filter inputs (input-number / input-text), keyed by field name. */
  fill?: Record<string, string>;
  /** Flux date-picker filters, keyed by the trigger's aria-label (field label).
   * The picker is a button + calendar popover (no fillable input), so dates are
   * chosen by driving the calendar day buttons via pickFluxDate. Used when the
   * page.yaml default (e.g. ${NOW()}) produces an unparseable value. */
  fillDates?: Record<string, string>;
}

export function assertReportRendered(cfg: ReportVisualAssertion): void {
  test.describe(`${cfg.reportLabel} report flux render`, () => {
    test('renders renderHtml response into the page via flux data-source', async ({ page }) => {
      // Flux report pages auto-fetch on load: the data-source (action ajax,
      // `@query:<Biz>__renderHtml`) dispatches a REST `/r/<Biz>__renderHtml`
      // request as soon as the page mounts. Register the listener BEFORE
      // navigation (plan 2026-09-01-0527-2: the legacy AMIS orchestration —
      // `/graphql` + renderHtml body + 渲染报表 button click — no longer exists).
      const responsePredicate = (resp: import('@playwright/test').Response): boolean => {
        if (!resp.url().includes('/r/')) return false;
        return (
          resp.url().includes('renderHtml') || decodeURIComponent(resp.url()).includes('renderHtml')
        );
      };
      const initialResponsePromise = page.waitForResponse(responsePredicate, { timeout: 30_000 });

      await loginAndNavigate(page, cfg.route);

      const renderResponse = await initialResponsePromise;
      expect(renderResponse.status(), `${cfg.reportLabel} renderHtml should return 200`).toBe(200);

      // Deterministic filtered reload: set the filter fields, then wait for the
      // auto-reload they trigger. No button click is needed (and clicking is
      // unreliable): the data-source args templates read `filterForm?.x`, flux
      // dependency-tracking treats those reads as dependencies, and the
      // valuesPath publish after each field change re-dispatches the source
      // (debounced ~4s). A fill that sets the input's current value does NOT
      // re-dispatch the source — only wait for the reload when something
      // actually changed (the initial load already rendered with the unchanged
      // value). The response listener is registered only when a reload is
      // expected, right before the first mutation.
      const hasValueFills = cfg.fill && Object.keys(cfg.fill).length > 0;
      const hasDateFills = cfg.fillDates && Object.keys(cfg.fillDates).length > 0;
      if (hasValueFills || hasDateFills) {
        let reloadExpected = hasDateFills;
        const pendingFills: Array<() => Promise<void>> = [];
        if (hasValueFills) {
          for (const [name, value] of Object.entries(cfg.fill!)) {
            const input = page.locator(`input[name="${name}"]`).first();
            const current = await input.inputValue();
            if (current !== value) {
              pendingFills.push(() => input.fill(value));
              reloadExpected = true;
            }
          }
        }
        if (reloadExpected) {
          const reloadResponsePromise = page.waitForResponse(responsePredicate, { timeout: 30_000 });
          for (const fillOp of pendingFills) {
            await fillOp();
          }
          if (hasDateFills) {
            for (const [label, value] of Object.entries(cfg.fillDates!)) {
              await pickFluxDate(page, label, value);
            }
          }
          // The fills are debounced; the first caught response may still carry
          // a partial filter set. The token assertions below poll to the final
          // values, so this wait only synchronizes on the reload pipeline.
          await reloadResponsePromise;
        }
      }

      const firstToken = cfg.expectedTokens[0];
      await expect.poll(
        async () => (await page.textContent('body')) || '',
        { timeout: 20_000, message: `${cfg.reportLabel} body should contain rendered token "${firstToken}"` },
      ).toContain(firstToken);

      const bodyText = (await page.textContent('body')) || '';
      for (const tok of cfg.expectedTokens) {
        expect(
          bodyText,
          `${cfg.reportLabel} rendered report should contain token "${tok}"`,
        ).toContain(tok);
      }
    });
  });
}
