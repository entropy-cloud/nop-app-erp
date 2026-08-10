import { test, expect, loginAsRole } from './_helper';
import type { Page } from '@playwright/test';

/**
 * E1.2 菜单过滤负向验证（plan 2026-08-10-1404-1 Phase 2，permissions-enforcement mission）。
 *
 * 验证 action-auth ON（%test profile）下 `SiteMapProviderImpl.filterAllowedMenu` deny-by-default
 * 按角色过滤菜单的 4 类断言：
 *   (a) B 类 5 域（CRM/CS/APS/Logistics/DRP）admin-only → role-restricted 不可见
 *   (b) 角色域按角色过滤 → 采购员仅见 erp-pur + 授权只读域，不见 fin/sal/ct 等
 *   (c) notify inbox roles="user" → 所有登录用户可见
 *   (d) sys / l10n-cn roles="admin" → role-restricted 不可见
 *
 * 机制：调用 `/r/SiteMapApi__getSiteMap` REST RPC（与 flux 前端同源），server 经
 * `filterAllowedMenu` 按当前用户角色过滤后返回可见菜单树。断言 = 检查 TOPM 资源 ID
 * 在响应 JSON 中存在/缺席。
 */

async function getSiteMapIds(page: Page): Promise<Set<string>> {
  const cookies = await page.context().cookies();
  const tokenCookie =
    cookies.find((c) => c.name === '__Host-nop-token') ??
    cookies.find((c) => c.name === 'nop-token');
  const headers: Record<string, string> = {};
  if (tokenCookie) headers['Authorization'] = `Bearer ${tokenCookie.value}`;
  const resp = await page.request.post('/r/SiteMapApi__getSiteMap', { headers, data: {} });
  const json = await resp.json();
  // flatten all resource ids from the site-map tree
  const ids = new Set<string>();
  const walk = (node: any) => {
    if (!node) return;
    if (node.id) ids.add(node.id);
    if (Array.isArray(node.children)) node.children.forEach(walk);
    if (Array.isArray(node.items)) node.items.forEach(walk);
  };
  const data = json?.data ?? json;
  if (Array.isArray(data)) {
    data.forEach(walk);
  } else if (data) {
    walk(data);
  }
  return ids;
}

test.describe('E1.2 menu filter: deny-by-default role filtering', () => {
  test('(a) B-class 5 domains hidden + (c) notify visible + (d) sys/l10n hidden for role-restricted', async ({ page }) => {
    await loginAsRole(page, 'restricted');
    await page.goto('/', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    const ids = await getSiteMapIds(page);

    // (a) B-class 5 domains admin-only → hidden
    const bClassHidden = ['erp-crm', 'erp-cs', 'erp-aps', 'erp-log', 'erp-drp'];
    for (const id of bClassHidden) {
      expect(ids.has(id), `B-class TOPM ${id} should be hidden for role-restricted`).toBe(false);
    }

    // (c) notify inbox roles="user" → visible for all logged-in users
    const notifyPresent = Array.from(ids).some((id) => id.includes('notify'));
    expect(notifyPresent, 'notify TOPM should be visible for role-restricted (roles="user")').toBe(true);

    // (d) sys-*/l10n-cn roles="admin" → hidden for role-restricted
    expect(ids.has('erp-sys'), 'erp-sys should be hidden for role-restricted (roles="admin")').toBe(false);
    expect(ids.has('erp-l10n-cn'), 'erp-l10n-cn should be hidden for role-restricted (roles="admin")').toBe(false);
  });

  test('(b) role-based filtering: 财务员 sees erp-fin, not pur/sal/ct', async ({ page }) => {
    await loginAsRole(page, '财务员');
    await page.goto('/', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    const ids = await getSiteMapIds(page);

    // 财务员 should see erp-fin (FNPT roles="财务员" cascadeUp + TOPM roles="财务员")
    expect(ids.has('erp-fin'), 'erp-fin should be visible for 财务员').toBe(true);

    // 财务员 should NOT see pur/sal/ct/b2b/hr/mfg/ast/prj/qa/mnt (different role domains)
    const otherDomains = ['erp-pur', 'erp-sal', 'erp-ct', 'erp-b2b', 'erp-hr', 'erp-mfg', 'erp-ast', 'erp-prj', 'erp-qa', 'erp-mnt'];
    for (const id of otherDomains) {
      expect(ids.has(id), `${id} should be hidden for 财务员 (role-filtered)`).toBe(false);
    }

    // B-class also hidden
    expect(ids.has('erp-crm'), 'erp-crm should be hidden for 财务员').toBe(false);
  });

  test('(b2) 采购员 role-based filtering: cross-domain deny', async ({ page }) => {
    await loginAsRole(page, '采购员');
    await page.goto('/', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    const ids = await getSiteMapIds(page);

    // 采购员 should NOT see other role domains (deny-by-default filtering active)
    const otherDomains = ['erp-fin', 'erp-sal', 'erp-ct', 'erp-b2b', 'erp-hr', 'erp-mfg', 'erp-ast', 'erp-prj', 'erp-qa', 'erp-mnt'];
    let deniedCount = 0;
    for (const id of otherDomains) {
      if (!ids.has(id)) deniedCount++;
    }
    // At least 8 of 10 other domains must be hidden (role-based deny-by-default active)
    expect(deniedCount, `采购员 should have most other role domains hidden, only ${deniedCount}/10 denied`).toBeGreaterThanOrEqual(8);

    // B-class hidden
    expect(ids.has('erp-crm'), 'erp-crm should be hidden for 采购员').toBe(false);

    // notify inbox visible (roles="user" always allowed)
    const notifyPresent = Array.from(ids).some((id) => id.includes('notify'));
    expect(notifyPresent, 'notify TOPM should be visible for 采购员').toBe(true);
  });
});
