import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'mfg-crp-load',
  route: '/crp-load-report',
  query:
    'query($reportName:String!,$workcenterId:BigDecimal,$startDate:String,$endDate:String){ ErpMfgReport__renderHtml(reportName:$reportName,data:{workcenterId:$workcenterId,startDate:$startDate,endDate:$endDate}) }',
  variables: {
    reportName: 'crp-load-report',
    workcenterId: '1',
    startDate: '2026-07-15',
    endDate: '2026-07-15',
  },
  responseKey: 'ErpMfgReport__renderHtml',
  expectedTokens: ['CRP 工作中心负荷分析表', 'WC-001', '2026-07-15', '4.00', '8.00', '0.50'],
});
