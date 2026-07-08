import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'fin-ar-ap-aging',
  route: '/ar-ap-aging',
  query: 'query($reportName:String!,$asOfDate:String){ ErpFinReport__renderHtml(reportName:$reportName,data:{asOfDate:$asOfDate}) }',
  variables: { reportName: 'ar-ap-aging', asOfDate: '2026-07-08' },
  responseKey: 'ErpFinReport__renderHtml',
  expectedTokens: ['应收应付账龄分析表', '未核销余额合计'],
});
