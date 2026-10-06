/**
 * 通知中心菜单可达性回归探针（plan 2026-10-06-1836-1 Phase 1 / roadmap WI-02 / F-R20）。
 *
 * 历史：F-R20 诊断时本探针形态为「当前必红」红灯（角色绑定缺失 + 客户端角色管线断裂）；
 * 实施时两断点已闭合（种子 1,user 重打包生效 + normalizeUser roleInfos 映射修复），
 * 按确认纪律注销红灯转**回归守卫**：任一断点回退（种子绑定删除/重打包时差/角色管线回归）
 * 本探针即红。断言链 = 登录角色管线健全 → 直航路由页面渲染 → 菜单组「通知中心」可见。
 */
import { test, expect } from '@playwright/test';
import { login, navigateTo } from '../pages/Navigation';

test('notify-inbox menu reachable: role pipeline → inbox page renders → menu group visible', async ({ page }) => {
  await login(page);

  // 客户端角色管线健全性：登录用户必须持有非空角色集合（normalizeUser roleInfos 映射在位）。
  // 该断言钉死 F-R20 断点②——roles 管线回退为 [] 时本断言先红（角色守卫拒绝一切 role-scoped 项）。
  const authRaw = await page.evaluate(() => sessionStorage.getItem('auth:v2'));
  const auth = authRaw ? JSON.parse(authRaw) : null;
  expect(auth?.state?.user?.roles?.length ?? 0, 'client user.roles must be non-empty (role pipeline intact)').toBeGreaterThan(0);

  // 直航路由（第二层守卫放行即页面可达；第一层菜单可见性由下方菜单组断言守护）
  await navigateTo(page, '/ErpSysNotification-inbox');
  await expect(page.getByText('全部标记已读').first()).toBeVisible({ timeout: 15_000 });

  // 第一层菜单可见性：侧栏含「通知中心」菜单组（roles="user" 绑定 + 角色管线双断点回归面）
  const menuCenter = page.getByText('通知中心').first();
  await expect(menuCenter).toBeVisible({ timeout: 10_000 });
});
