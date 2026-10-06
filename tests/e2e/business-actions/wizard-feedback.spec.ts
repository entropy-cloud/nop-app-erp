import { test, expect, loginAndNavigate } from './_helper';
import { readFileSync } from 'fs';
import { resolve } from 'path';

/**
 * 向导/作业页交互反馈完整性探针（plan 2026-10-06-0355-4 Phase 3）。
 *
 * 空值门断言（disabled 初始态）+ 范式结构断言（messages/i18nEn/min 门在位）。
 * 成功路径（toast + 数据刷新）由 flux-rewired-buttons 的 startTake 全链覆盖
 * （该按钮带 messages.success，翻转断言即成功路径证明）。
 */

test.describe('wizard feedback completeness (plan 2026-10-06-0355-4)', () => {
  test('pda save button disabled until required fields filled', async ({ page }) => {
    await loginAndNavigate(page, '/pda-stock-take');
    const save = page.getByRole('button', { name: /保存盘点行|Save/ });
    await expect(save).toBeVisible({ timeout: 15_000 });
    await expect(save, 'empty fields → save disabled (F-R9 gate)').toBeDisabled();
  });

  test('lead-conversion confirm button gated on toStageId', async ({ page }) => {
    const text = readFileSync(
      resolve(__dirname, '../../../module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/lead-conversion/main.page.yaml'),
      'utf-8',
    );
    expect(text, 'confirm button carries disabled gate on toStageId').toMatch(
      /disabled:\s*"\$\{!step2Form\?\.toStageId\}"/,
    );
    expect(text, 'moveStage carries messages feedback').toMatch(/success:\s*"?\u7ebf\u7d22\u5df2\u8f6c\u5316/);
    expect(text, 'then chain refreshes leadsData/stagesData').toMatch(
      /targetId:\s*leadsData[\s\S]*targetId:\s*stagesData/,
    );
  });

  test('structural: messages/min/gates landed across touched pages', async () => {
    const read = (f: string) => readFileSync(resolve(__dirname, '../../../', f), 'utf-8');

    const periodClose = read('module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/period-close-wizard/main.flux.yaml');
    expect(periodClose.match(/messages:/g)?.length, 'period-close 3 mutation buttons carry messages').toBe(3);
    expect(periodClose, 'reverse close gated on reason').toMatch(/disabled:\s*\$\{!reverseCloseReason\}/);
    expect(periodClose, 'period select source carries query limit').toMatch(/findList'[\s\S]*?query:\n\s+limit: 200/);

    const disposal = read('module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/disposal-wizard/main.page.yaml');
    expect(disposal, 'assetsData filtered to disposable statuses').toMatch(/filter_status__in:\s*\["IN_SERVICE",\s*"IDLE"\]/);
    expect(disposal, 'disposalAmount clamped to non-negative').toMatch(/min: 0/);
    expect(disposal, 'submit carries messages + then chain').toMatch(/messages:[\s\S]*?then:[\s\S]*?targetId:\s*assetsData/);

    const visit = read('module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/visit-wizard/main.flux.yaml');
    expect(visit, 'visit save carries messages + refresh').toMatch(/messages:[\s\S]*?targetId:\s*visitData/);

    const pda = read('module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/pda-stock-take/main.page.yaml');
    expect(pda, 'actualQuantity clamped to non-negative').toMatch(/min: 0/);

    const stockFlow = read('module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/stock-take-flow/main.page.yaml');
    expect(stockFlow, 'takeId is a select bound to stockTakeData (no blind id input)').not.toMatch(/input-number\n        name: takeId/);
    expect(stockFlow, 'crud footer carries pagination + count/cap hint').toMatch(/footerToolbar:[\s\S]*?pagination[\s\S]*?共 /);
  });
});
