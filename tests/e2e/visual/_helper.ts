import { test, expect, loginAndNavigate } from '../fixtures';
import { getEngine } from '../pages';
import type { Page, Locator } from '@playwright/test';

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

export interface DashboardVisualAssertion {
  domain: string;
  route: string;
  expectedKpiTokens?: string[];
  hasChart: boolean;
  alertTable: boolean;
  /** Form field values to fill before reloading, locking deterministic seed
   * values (aligned with the value-spec layer). Without these, date/period
   * KPIs would drift with the server clock. */
  filterValues?: Record<string, string>;
  /** GraphQL action name whose response marks the post-reload KPI ready.
   * Defaults to `getDashboardKpi`. */
  kpiAction?: string;
}

async function kpiSpanTexts(page: Page): Promise<string[]> {
  return page.locator('span.h3').allTextContents();
}

export function assertDashboardRendered(cfg: DashboardVisualAssertion): void {
  test.describe(`${cfg.domain} dashboard AMIS render`, () => {
    test('renders KPI cards + echarts canvas + alert table via AMIS GraphQL pipeline', async ({ page }) => {
      const kpiAction = cfg.kpiAction ?? 'getDashboardKpi';

      // Initial load (default/empty filters) — captures the AMIS GraphQL
      // pipeline integrity. Defect A (mangled `$var`) still returns HTTP 200
      // here, so this alone does not prove values; the token assertions below do.
      const initialResponsePromise = page.waitForResponse(
        (resp) => {
          if (!resp.url().includes('/graphql')) return false;
          const body = resp.request().postData() || '';
          return body.includes(kpiAction);
        },
        { timeout: 30_000 },
      );

      await loginAndNavigate(page, cfg.route);
      await initialResponsePromise;

      // Deterministic filtered reload: fill the filter form, then click the
      // "刷新" reload button, then wait for the post-reload KPI response. This
      // locks date/period ranges to seed values (same caliber as value specs).
      if (cfg.filterValues && Object.keys(cfg.filterValues).length > 0) {
        for (const [name, value] of Object.entries(cfg.filterValues)) {
          await page.locator(`input[name="${name}"]`).first().fill(value);
        }
        const reloadResponsePromise = page.waitForResponse(
          (resp) => {
            if (!resp.url().includes('/graphql')) return false;
            const body = resp.request().postData() || '';
            return body.includes(kpiAction);
          },
          { timeout: 30_000 },
        );
        await page.getByRole('button', { name: /刷新|Refresh/ }).first().click();
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
        await expect(
          page.locator('canvas').first(),
          `${cfg.domain} echarts canvas should render`,
        ).toBeVisible({ timeout: 20_000 });
        const box = await page.locator('canvas').first().boundingBox();
        expect(
          box !== null && box.width > 0 && box.height > 0,
          `${cfg.domain} echarts canvas should have non-zero size`,
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
  fill?: Record<string, string>;
  /** AMIS input-date fields to fill, keyed by form-item label text. Date
   * inputs have no fillable <input name>, so they are targeted by the label
   * of their enclosing .cxd-Form-item wrapper. Used when the page.yaml
   * default (e.g. ${NOW()}) produces an unparseable value. */
  fillDates?: Record<string, string>;
}

export function assertReportRendered(cfg: ReportVisualAssertion): void {
  test.describe(`${cfg.reportLabel} report AMIS render`, () => {
    test('injects renderHtml response into the page via AMIS service reload', async ({ page }) => {
      const renderResponsePromise = page.waitForResponse(
        (resp) => {
          if (!resp.url().includes('/graphql')) return false;
          const body = resp.request().postData() || '';
          return body.includes('renderHtml');
        },
        { timeout: 30_000 },
      );

      await loginAndNavigate(page, cfg.route);

      if (cfg.fill) {
        for (const [name, value] of Object.entries(cfg.fill)) {
          await page.locator(`input[name="${name}"]`).first().fill(value);
        }
      }

      if (cfg.fillDates) {
        const engine = getEngine();
        for (const [label, value] of Object.entries(cfg.fillDates)) {
          await engine.dateInputByLabel(page, label).fill(value);
        }
      }

      await page.getByRole('button', { name: /渲染报表|Render/ }).first().click();

      const renderResponse = await renderResponsePromise;
      expect(renderResponse.status(), `${cfg.reportLabel} renderHtml should return 200`).toBe(200);

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
