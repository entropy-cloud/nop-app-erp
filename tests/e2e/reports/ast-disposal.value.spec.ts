import { assertReportRenderedWithValue } from './_helper';

// ast-disposal: E2E seeded DB has NO disposal rows (_vfs/_init-data/ lacks
// erp_ast_disposal.csv), so renderHtml yields only the report title + empty
// total — no numeric token is assertable. The title token proves the disposal
// report specifically renders via the renderHtml pipeline (stronger than the
// smoke spec's generic GraphQL-200/DOM check). Upgrade to numeric tokens is a
// registered successor (trigger: ast-disposal gains E2E seed data).
assertReportRenderedWithValue({
  reportLabel: 'ast-disposal',
  route: '/asset-disposal-detail',
  query: 'query($reportName:String!,$startDate:String,$endDate:String){ ErpAstReport__renderHtml(reportName:$reportName,data:{startDate:$startDate,endDate:$endDate}) }',
  variables: { reportName: 'asset-disposal-detail' },
  responseKey: 'ErpAstReport__renderHtml',
  expectedTokens: ['资产处置明细表'],
});
