import { test, expect, loginAndNavigate } from '../fixtures';

// AMIS front-end render-layer assertions for reports.
//
// Defect B (docs/bugs/2026-07-09-1249-report-render-container-wiring.md):
// the "渲染报表" button fired renderHtml (backend returned HTML) but the
// response never reached the DOM. Defect A additionally mangled the `$var`
// in the renderHtml query. Both are fixed (plan 2026-07-09-1728-1).
//
// IMPLEMENTATION NOTE (deviation from the plan's prescribed onEvent mirror):
// the balance-sheet `onEvent: setVariable(event.data.result) + setValue`
// reference pattern turned out to be itself broken at runtime — (1) the
// legacy button-ajax result is NOT exposed as `event.data.result` (amis-core
// AjaxAction stores it under `outputVar`, default `responseResult`), and (2)
// CmptAction resolves targets via `componentId`/`componentName`, ignoring the
// `target:` field. The working pattern (verified here) is the same one the
// dashboards use: the render button does `actionType: reload` of an in-form
// `service` (name: reportService, initFetch: false) whose api has an adaptor
// that flattens the report HTML to `data.reportHtml`, and whose body is a
// `type: html html: "${reportHtml}"`. The service lives INSIDE the form so it
// shares the form's field-value scope (a sibling service gets periodId="" ->
// BigDecimal NumberFormatException). See plan Phase 3 Decision Record.
//
// These assertions drive the real AMIS page, click "渲染报表", wait for the
// AMIS-issued renderHtml response, then assert the report-specific tokens
// (subject names / labels that appear ONLY in the rendered report HTML) are
// now in the DOM. Pre-fix the container is empty; post-fix the rendered HTML
// is injected. Tokens derive from the value-spec layer
// (tests/e2e/reports/*.value.spec.ts); numeric values are comma-formatted in
// the HTML (e.g. "1,130.00") so format-independent subject/label tokens are
// used here, with comma-stripping left to the value-spec layer.

interface ReportVisualAssertion {
  reportLabel: string;
  route: string;
  expectedTokens: string[];
  fill?: Record<string, string>;
}

function assertReportRendered(cfg: ReportVisualAssertion): void {
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

      await page.getByRole('button', { name: /渲染报表|Render/ }).first().click();

      const renderResponse = await renderResponsePromise;
      expect(renderResponse.status(), `${cfg.reportLabel} renderHtml should return 200`).toBe(200);

      // Defect B drops the response; post-fix the onEvent pipeline injects the
      // rendered HTML into the DOM. Poll body text until the first distinctive
      // report token appears, then assert all expected tokens.
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

assertReportRendered({
  reportLabel: 'fin-income-statement',
  route: '/income-statement',
  fill: { periodId: '1' },
  // Subject/label tokens (format-independent; values are comma-formatted
  // e.g. "1,130.00" so they're verified in the value-spec layer instead).
  expectedTokens: ['主营业务收入', '净利润'],
});

assertReportRendered({
  reportLabel: 'fin-balance-sheet',
  route: '/balance-sheet',
  fill: { periodId: '1' },
  expectedTokens: ['资产负债表', '银行存款', '169.50'],
});

assertReportRendered({
  reportLabel: 'crm-lead-conversion-funnel',
  route: '/lead-conversion-funnel',
  expectedTokens: ['线索转化漏斗表', '验证', '报价'],
});

assertReportRendered({
  reportLabel: 'hr-employee-net-balance',
  route: '/employee-net-balance',
  expectedTokens: ['员工净余额报表', '张三员工往来', '员工欠公司'],
});
