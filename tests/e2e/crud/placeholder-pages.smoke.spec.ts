/**
 * 占位页落地 E2E 冒烟（plan 2026-08-03-1232-4 Phase 1/2/3）。
 *
 * 验证已落地占位页 + 未实现设计项在 flux 引擎下渲染成功：
 * - Phase 1: SPC 三件套 + 财务 5 页
 * - Phase 2: 处置向导 + 盘点流程 + 线索转化向导
 * - Phase 3: 项目损益/结算 + 成本中心 + 资产盘点
 *
 * 冒烟级：页面 DOM 渲染 + 数据请求返回 200 + 关键内容关键词存在。
 */
import { test, expect, loginAndNavigate } from '../fixtures';

interface PageCase {
  id: string;
  route: string;
  keywords: string[];
}

const pages: PageCase[] = [
  // Phase 1 - SPC trio
  { id: 'spc-chart', route: '/spc-chart-main', keywords: ['SPC', '控制图'] },
  { id: 'spc-capability', route: '/spc-capability-main', keywords: ['能力'] },
  { id: 'spc-sample', route: '/spc-sample-main', keywords: ['样本'] },
  // Phase 1 - finance
  { id: 'expense-claim', route: '/expense-claim-main', keywords: ['报销'] },
  { id: 'budget-scenario', route: '/budget-scenario-main', keywords: ['预算'] },
  { id: 'budget-control-log', route: '/budget-control-log-main', keywords: ['控制'] },
  { id: 'bank-statement', route: '/bank-statement-main', keywords: ['对账单'] },
  { id: 'bank-reconciliation', route: '/bank-reconciliation-main', keywords: ['调节'] },
  // Phase 2 - wizards/flow
  { id: 'disposal-wizard', route: '/disposal-wizard-main', keywords: ['处置', '资产'] },
  { id: 'stock-take-flow', route: '/stock-take-flow-main', keywords: ['盘点'] },
  { id: 'lead-conversion', route: '/lead-conversion-main', keywords: ['转化', '线索'] },
  // Phase 3 - remaining
  { id: 'project-pnl', route: '/project-pnl-main', keywords: ['损益'] },
  { id: 'project-settlement', route: '/project-settlement-main', keywords: ['结算'] },
  { id: 'cost-center', route: '/cost-center-main', keywords: ['成本中心'] },
  { id: 'asset-stocktake', route: '/asset-stocktake-main', keywords: ['盘点'] },
];

for (const pg of pages) {
  test.describe(`${pg.id} placeholder page smoke`, () => {
    test('renders page with data 200 and content keywords', async ({ page }) => {
      const dataResponses: number[] = [];
      page.on('response', (resp) => {
        const url = resp.url();
        if (url.includes('/graphql') || url.includes('/r/')) {
          dataResponses.push(resp.status());
        }
      });

      await loginAndNavigate(page, pg.route);

      const bodyText = (await page.textContent('body')) || '';
      expect(bodyText.length, `${pg.id}: page should render substantial content`).toBeGreaterThan(50);

      const hasKeyword = pg.keywords.some((kw) => bodyText.includes(kw));
      expect(hasKeyword, `${pg.id}: page should contain keyword: ${pg.keywords.join(', ')}`).toBe(true);

      expect(dataResponses.length, `${pg.id}: should have data calls`).toBeGreaterThan(0);
      for (const status of dataResponses) {
        expect(status, `${pg.id}: data call should return 200`).toBe(200);
      }
    });
  });
}
