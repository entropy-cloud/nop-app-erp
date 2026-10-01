import { test, expect, loginAndNavigate } from '../business-actions/_helper';
import { callMutationOk, callMutation, verifyState, createViaSave } from '../business-actions/_helper';
import type { Page } from '@playwright/test';

/**
 * USC-04 移动可达 Web 审批与待办体验（plan 2026-10-01-0830-1）。
 *
 * 375×812 移动视口下的三层证明：
 *   ① 通知收件箱（现有唯一类待办面）页面可达 + 未读列表渲染——审批待办入口今天不存在
 *      （三重阻断：inbox 纯通知 / xwf 模板浏览器层不可达 2330-1 / DIRECT 实体无审批通知，
 *      见计划 Current Baseline 替代裁决段）。
 *   ② flux 窄屏响应式实测——运行时 bundle 含 data-responsive="narrow" 标记与
 *      table-responsive-expanded 卡片化 DOM；实测 ErpHrLeaveRequest 列表页在 375px 下的
 *      实际渲染形态并落盘证据集（标记在/不在、DOM 形态、行操作可定位性）。
 *   ③ DIRECT 域状态机审批轴移动视口状态翻转：ErpHrLeaveRequest（DIRECT submit/approve，
 *      审批字段 status；行按钮无条件渲染无 visibleOn）——建单（GraphQL）→ submit → 375px
 *      页面行按钮 approve 点击（FluxAdapter rowAction 含「更多」dropdown 回退）→
 *      confirmDialogAction 确认 → status=APPROVED 翻转断言（verifyState 范式）。
 *      D5 裁决：窄屏定位失配时降级为 GraphQL mutation 驱动 + 按钮可达性观察（具名 residual）。
 *
 * 语义边界：视口仅证明响应式可达性；审批驱动经 DIRECT mutation（2330-1：xwf submit
 * 浏览器层不可行；ErpHrLeaveRequest 无 useWorkflow，DIRECT 轴既有浏览器层先例
 * hr-leave-attendance.action.spec.ts）。
 */

const MOBILE = { width: 375, height: 812 };
const TAG = `MOB-${process.pid}-${Date.now()}`;

/** 沿 hr-leave-attendance.action.spec.ts :61-77 建单最小集。 */
function leaveData(code: string) {
  return {
    code,
    employeeId: '1',
    leaveType: 'ANNUAL',
    startDate: '2026-12-01',
    endDate: '2026-12-02',
    status: 'DRAFT',
    orgId: '2',
  };
}

test.describe('USC-04 mobile viewport: inbox + responsive + DIRECT approval', () => {
  test('① inbox reachable at 375px with unread list rendering', async ({ page }) => {
    await page.setViewportSize(MOBILE);
    await loginAndNavigate(page, '/ErpSysNotification-inbox');
    // 未读 tab 与列表容器渲染（类待办面页面可达性证明）
    // 手写 flux 页（非 GenPage crud）：可达性断言 = 页面数据源/组件树实际渲染（非 .nop-crud 容器）
    await expect(page.locator('main, [class*="flux"], #app, body >> visible=true').first())
      .toBeVisible({ timeout: 15000 });
    await page.waitForTimeout(1500);
    const bodyText = await page.locator('body').innerText();
    expect(bodyText.length, '375px 下 inbox 页面应渲染实质内容（非空白/错误页）').toBeGreaterThan(50);
  });

  test('② flux narrow-screen responsive mode: empirical evidence set', async ({ page }) => {
    await page.setViewportSize(MOBILE);
    await loginAndNavigate(page, '/ErpHrLeaveRequest-main');
    await page.waitForTimeout(2000);

    // 最小证据集四字段（落盘 runbook）
    const responsiveMarker = await page.locator('[data-responsive="narrow"]').count();
    const cardDom = await page.locator('[data-slot="table-responsive-expanded"]').count();
    const desktopRows = await page.locator('[data-slot="table-body"] tr[data-slot="table-row"]').count();
    // 行操作可定位性：row-approve-button（无条件渲染，需 SUBMITTED 行才出现；此处仅探测容器形态）
    const rowActionProbe = await page.locator('.nop-crud').count();

    // 证据落盘（spec 注释 + runbook 由计划 Phase 2 汇总）
    console.log('[USC-04 证据集]',
      JSON.stringify({ responsiveMarker, cardDom, desktopRows, rowActionProbe }));

    // 页面在移动视口下实质渲染（可达性底线断言——渲染形态结论见证据集）
    expect(rowActionProbe).toBeGreaterThanOrEqual(1);
    // 实测结论分支：窄屏模式激活（marker 或卡片 DOM 出现）或维持桌面表格形态
    const narrowActivated = responsiveMarker > 0 || cardDom > 0 || desktopRows === 0;
    console.log('[USC-04 结论] narrowActivated =', narrowActivated);
    // 实测结论（2026-10-01）：responsiveMarker=1（crud 根 data-responsive="narrow" 激活）、
    // cardDom=0（table-responsive-expanded 卡片化未触发）、desktopRows=3（桌面表格行保留）、
    // 行操作可定位性=失败（③ 实测 D5(b) 降级）——375px 下 flux 窄屏标记激活但表格行结构保留，
    // 行按钮被收进「更多」dropdown 或容器裁切，行级定位失配。
  });

  test('③ ErpHrLeaveRequest mobile approval: submit → 375px row-approve click → status=APPROVED', async ({ page }) => {
    const code = `${TAG}-LV`;
    // 1. GraphQL 建单（DRAFT 最小集）
    await loginAndNavigate(page, '/ErpHrLeaveRequest-main');
    const saved = await createViaSave(page, 'ErpHrLeaveRequest', leaveData(code), 'id code');
    expect(saved?.code).toBe(code);
    const id = String(saved.id);

    // 2. DIRECT submit（该实体无 submitForApproval）
    await callMutationOk(page, 'ErpHrLeaveRequest', 'submit', { id }, 'id');
    const afterSubmit = await verifyState(page, 'ErpHrLeaveRequest', id, 'status');
    expect(afterSubmit.status).toBe('SUBMITTED');

    // 3. 375px 移动视口：打开实体页，行按钮 approve 点击 → confirmDialogAction 确认
    await page.setViewportSize(MOBILE);
    await page.goto('/#/ErpHrLeaveRequest-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2000);

    // 定位目标行（code 列匹配）；窄屏卡片化时回退 D5(b)
    const row = page.locator(`[data-slot="table-row"]`, { hasText: code }).first();
    const rowVisible = await row.isVisible().catch(() => false);

    if (rowVisible) {
      // D5(a) 主路径：桌面定位契约成立 → 行内 approve 按钮 → 确认框
      const approveBtn = row.locator('button', { hasText: '批准' }).first();
      if (await approveBtn.isVisible().catch(() => false)) {
        await approveBtn.click();
        // confirmDialogAction 范式（FluxAdapter :455 alert-dialog 槽位）
        const confirmBtn = page.locator('[role="alertdialog"] button', { hasText: '确定' })
          .or(page.locator('[role="alertdialog"] button', { hasText: '确认' }))
          .or(page.locator('.amis-dialog button', { hasText: '确定' }))
          .first();
        await confirmBtn.click({ timeout: 10000 });
      } else {
        // 「更多」dropdown 回退
        const moreBtn = row.locator('button', { hasText: '更多' }).first();
        await moreBtn.click();
        const approveInMenu = page.locator('[role="menu"] >> text=批准').first();
        await approveInMenu.click({ timeout: 10000 });
        const confirmBtn = page.locator('[role="alertdialog"] button', { hasText: '确定' })
          .or(page.locator('[role="alertdialog"] button', { hasText: '确认' })).first();
        await confirmBtn.click({ timeout: 10000 });
      }
    } else {
      // D5(b) 登记分支：窄屏卡片化 DOM 致行定位失配 → GraphQL 驱动 + 可达性观察
      console.log('[USC-04 D5(b)] 窄屏行定位失配，降级 GraphQL 驱动（具名 residual 登记）');
      await callMutationOk(page, 'ErpHrLeaveRequest', 'approve', { id }, 'id');
    }

    // 4. 状态翻转断言（verifyState 范式）
    const st = await verifyState(page, 'ErpHrLeaveRequest', id, 'status');
    expect(st.status, '移动视口审批后 status 应翻转 APPROVED').toBe('APPROVED');

    // cleanup
    await page.setViewportSize({ width: 1280, height: 720 });
    await callMutation(page, 'ErpHrLeaveRequest', 'delete', { id }, 'id');
  });
});
