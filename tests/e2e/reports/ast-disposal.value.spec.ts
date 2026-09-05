import { assertReportRenderedWithValue } from './_helper';

// ast-disposal: E2E seeded DB carries 2 frozen disposal rows
// (app-erp-all/_vfs/_init-data/erp_ast_disposal.csv: DSP-2026-001 SOLD 500.00
// 2026-07-10 DRAFT / DSP-2026-002 SCRAPPED 0 2026-07-12 CANCELLED, added by
// plan 2026-09-01-2255-1 M1.2c). buildAssetDisposalDetailDataset applies no
// filter for an empty date range (ErpAstReportBizModel.loadDisposals), so the
// zero-param render includes both rows — verified live against the seeded
// runner (plan 2026-09-03-0400-3 M2.3 Phase 2). The tokens below assert row
// identity + amounts + statuses so an empty/zero render FAILS (upgraded from
// the title-only structural token registered as the M1.2c Deferred successor:
// "ast-disposal gains E2E seed data" → numeric tokens).
assertReportRenderedWithValue({
  reportLabel: 'ast-disposal',
  route: '/asset-disposal-detail',
  query: 'query($reportName:String!,$startDate:String,$endDate:String){ ErpAstReport__renderHtml(reportName:$reportName,data:{startDate:$startDate,endDate:$endDate}) }',
  variables: { reportName: 'asset-disposal-detail' },
  responseKey: 'ErpAstReport__renderHtml',
  expectedTokens: [
    'DSP-2026-001',
    'DSP-2026-002',
    '500.00',
    '2026-07-10',
    '2026-07-12',
    'SOLD',
    'SCRAPPED',
    'DRAFT',
    'CANCELLED',
  ],
});
