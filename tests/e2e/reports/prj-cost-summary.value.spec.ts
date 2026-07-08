import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'prj-cost-summary',
  route: '/project-cost-summary',
  query: 'query($reportName:String!){ ErpPrjReport__renderHtml(reportName:$reportName) }',
  variables: { reportName: 'project-cost-summary' },
  responseKey: 'ErpPrjReport__renderHtml',
  expectedTokens: ['项目成本汇总表', '50000.00', '30000.00', '60.00%'],
});
