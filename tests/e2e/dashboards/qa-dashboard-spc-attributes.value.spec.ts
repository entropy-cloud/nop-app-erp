import { test, expect, loginAndNavigate } from '../fixtures';
import { GraphQLClient } from '../pages';

/**
 * quality SPC 计数型（P/NP/C/U）看板 getSpcControlChartData 数据驱动数值断言
 * （plan 2026-07-19-0120-2 Phase 4，iter-2 审查 MINOR-3 路径：`tests/e2e/dashboards/`）。
 *
 * 验证 @BizQuery ErpQaDashboard__getSpcControlChartData 对 4 计数型 chartType 的全栈可达：
 *   - 顶层 chartType 字段非空（== P/NP/C/U 之一）
 *   - per-sample 字段 defectRate/defectCount/inspectedCount 非空
 *   - cl/ucl/lcl 一致性（UCL ≥ CL ≥ LCL）
 *
 * 纯 @BizQuery 数据断言（不经 AMIS 渲染层，对齐 1430-1 范式）。
 * 种子引用（init-data）：spc_chart.csv id=2/3/4/5（P/NP/C/U 各 1 chart CALCULATED）+
 * spc_sample.csv id=101~180（80 计数型子组样本，defectCount/inspectedCount 字段非空）。
 */

const ATTRIBUTES_CHART_IDS = [
  { chartId: 2, chartType: 'P', label: 'P chart' },
  { chartId: 3, chartType: 'NP', label: 'NP chart' },
  { chartId: 4, chartType: 'C', label: 'C chart' },
  { chartId: 5, chartType: 'U', label: 'U chart' },
];

for (const cfg of ATTRIBUTES_CHART_IDS) {
  test.describe(`quality-spc-attributes-${cfg.chartType}`, () => {
    test(`${cfg.label}: getSpcControlChartData returns top-level chartType + per-sample defectRate/defectCount/inspectedCount`, async ({ page }) => {
      await loginAndNavigate(page, '/qa-dashboard-main');

      const json: any = await new GraphQLClient(page).raw(
        'query($chartId:Long){ ErpQaDashboard__getSpcControlChartData(chartId:$chartId) }',
        { chartId: cfg.chartId },
      );
      const result = json?.data?.ErpQaDashboard__getSpcControlChartData;
      expect(result, `result map should be present for ${cfg.label}`).toBeTruthy();

      // 顶层 chartType 字段非空 + 匹配预期 chartType
      expect(result.chartType, `${cfg.label} top-level chartType`).toBe(cfg.chartType);

      // cl/ucl/lcl 一致性（CALCULATED 状态应有值）
      expect(result.cl, `${cfg.label} cl`).not.toBeNull();
      expect(result.ucl, `${cfg.label} ucl`).not.toBeNull();
      expect(result.lcl, `${cfg.label} lcl`).not.toBeNull();
      expect(Number(result.ucl), `${cfg.label} UCL >= CL`).toBeGreaterThanOrEqual(Number(result.cl));
      expect(Number(result.cl), `${cfg.label} CL >= LCL`).toBeGreaterThanOrEqual(Number(result.lcl));

      // per-sample 字段 defectRate/defectCount/inspectedCount 非空
      const samples = result.samples || [];
      expect(samples.length, `${cfg.label} samples count`).toBeGreaterThanOrEqual(20);
      const first = samples[0];
      expect(first.defectCount, `${cfg.label} sample[0].defectCount`).not.toBeNull();
      expect(first.inspectedCount, `${cfg.label} sample[0].inspectedCount`).not.toBeNull();
      expect(first.defectRate, `${cfg.label} sample[0].defectRate`).not.toBeNull();
      expect(Number(first.defectRate), `${cfg.label} defectRate = defectCount/inspectedCount`)
        .toBeCloseTo(Number(first.defectCount) / Number(first.inspectedCount), 5);
    });
  });
}
