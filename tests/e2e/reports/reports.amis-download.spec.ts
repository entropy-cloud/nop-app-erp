import { test } from '../fixtures';
import { assertAmisDownloadButton, type AmisDownloadAssertion } from './_helper';

// AMIS download-button runtime regression (page.yaml button layer).
//
// Distinct from `reports.download.spec.ts` (which calls `/p/{biz}__download`
// directly via `page.request.post`, bypassing AMIS), this spec drives the REAL
// AMIS "下载 XLSX/PDF" button declared in each report's page.yaml. It is the
// first layer able to catch a page.yaml download-button URL/structure drift
// (e.g. the historical `/graphql` URL that JSON-serializes the WebContentBean
// instead of returning a binary stream — plan 2026-07-12-0413-1).
//
// Sampling (not full 24×2=48): three parameter shapes are covered to exercise
// the page.yaml `api.data` REST body map under different field structures:
//   - parameterized: fin income-statement (periodId) + mfg crp-load-report
//     (workcenterId filled; date params left empty → backend full extent)
//   - zero-param:    crm lead-conversion-funnel (no form fields)
//   - string param:  md partner-list (zero-param full render) + cs
//     ticket-sla-csat-summary (ticketType filled)
// Each case × {xlsx, pdf}. The direct `/p/` layer (reports.download.spec.ts)
// already covers all 24 reports × 2 products for backend reachability, so a
// sampled AMIS layer is sufficient to guard the page.yaml wiring without
// duplicating the full matrix.
//
// Byte source: AMIS reads the `/p/` response as a blob (responseType: blob)
// then triggers a file save via a blob <a download>. Playwright's
// response.body() returns empty for blob-consumed responses, so assertions use
// the actually-downloaded file (page.waitForEvent('download') → saved file
// bytes), which is the true end-to-end user-facing artifact. A request-level
// guard on the `/p/{biz}__download` URL additionally catches a url regression
// back to `/graphql` (which would never issue a /p/ request nor fire a
// download). See `assertAmisDownloadButton` in `_helper.ts`.

const AMIS_DOWNLOAD_CASES: Array<Omit<AmisDownloadAssertion, 'renderType'>> = [
  // parameterized — single numeric param (periodId defaults to 1 in page.yaml)
  {
    domain: 'fin',
    reportName: 'income-statement',
    route: '/income-statement',
    expectedTokens: ['利润表', '主营业务收入', '净利润'],
  },
  // parameterized — numeric ID + date range. workcenterId filled (WC-001 seed);
  // date inputs left empty — the helper's /p/ route normalization converts
  // AMIS empty-string dates -> null so the backend uses full extent (incl. the
  // seed crp_load row), mirroring the renderHtml visual helper's workaround for
  // AMIS's date serialization.
  {
    domain: 'mfg',
    reportName: 'crp-load-report',
    route: '/crp-load-report',
    formFields: { workcenterId: '1' },
    expectedTokens: ['CRP 工作中心负荷分析表'],
  },
  // zero-param — no form fields at all
  {
    domain: 'crm',
    reportName: 'lead-conversion-funnel',
    route: '/lead-conversion-funnel',
    expectedTokens: ['线索转化漏斗表', '验证', '报价'],
  },
  // string param — left empty for a full zero-param render
  {
    domain: 'md',
    reportName: 'partner-list',
    route: '/partner-list',
    expectedTokens: ['往来单位清单'],
  },
  // string param — filled (ticketType=1 投诉 bucket)
  {
    domain: 'cs',
    reportName: 'ticket-sla-csat-summary',
    route: '/ticket-sla-csat-summary',
    formFields: { ticketType: '1' },
    expectedTokens: ['工单 SLA/CSAT 综合统计表', '投诉'],
  },
];

const RENDER_TYPES: Array<'xlsx' | 'pdf'> = ['xlsx', 'pdf'];

test.describe('report AMIS download button regression (sampling)', () => {
  for (const c of AMIS_DOWNLOAD_CASES) {
    for (const rt of RENDER_TYPES) {
      assertAmisDownloadButton({ ...c, renderType: rt });
    }
  }
});
