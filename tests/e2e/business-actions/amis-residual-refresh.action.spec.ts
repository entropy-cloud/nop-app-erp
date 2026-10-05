import { test, expect, loginAndNavigate } from './_helper';
import { readFileSync } from 'fs';
import { resolve } from 'path';

/**
 * AMIS 残留形态清零运行时探针（plan 2026-10-06-0355-1 Phase 3）。
 *
 * 背景：16 个被服务 *.flux.yaml 孪生页存在 17 个「hybrid 形态」按钮
 * （actionType:ajax + action:ajax + args 平铺按钮级、缺 onClick 包裹）——
 * flux button renderer 仅消费 onClick，平铺键全部 unknown-property、点击零请求
 * （机制与 plan 2026-10-05-0400-1 探针实锤同源，负对照证据引述该批 G-002 服务端日志）。
 * 本批全部转换为 onClick:{action:'refreshSource', targetId:<同名数据源>}。
 *
 * 正对照：prj 看板「刷新看板」点击 → boardData 数据源重载 → findBoardData 请求重发。
 * 结构断言：源文件中该按钮无 actionType 平铺键且 refreshSource 在位
 * （data-source/onCardMove 层的 action: ajax 是 flux 合法形态，不在断言面）。
 */
test.describe('AMIS residual forms cleanup probe', () => {
  test('prj kanban 刷新看板: click → refreshSource refires findBoardData', async ({ page }) => {
    // 监听须在导航前挂载：数据源初始加载（dependsOn __board_load__）可能先于任何 wait 就绪
    let count = 0;
    page.on('request', (r) => {
      if (r.url().includes('findBoardData') || (r.postData() ?? '').includes('findBoardData')) count++;
    });

    await loginAndNavigate(page, '/prj-task-kanban');
    // 初始加载到达（正对照基线）
    await expect
      .poll(() => count, { timeout: 20_000, message: 'initial board load should fire findBoardData' })
      .toBeGreaterThanOrEqual(1);

    const before = count;
    await page.getByRole('button', { name: /刷新看板|Refresh Kanban/ }).click();
    await expect
      .poll(() => count, { timeout: 10_000, message: 'refresh click should refire findBoardData via refreshSource' })
      .toBeGreaterThan(before);
  });

  test('structural: served hybrid button converted (no flat actionType keys)', async () => {
    const file = resolve(
      __dirname,
      '../../../module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/ErpPrjTask/kanban.flux.yaml',
    );
    const text = readFileSync(file, 'utf-8');
    expect(text, 'no flat actionType on live flux.yaml (data-source action: ajax is legal and excluded)')
      .not.toMatch(/actionType:/);
    expect(text, 'refresh button wired via refreshSource → boardData').toMatch(
      /action:\s*refreshSource\s*\n\s*targetId:\s*boardData/,
    );
  });

});
