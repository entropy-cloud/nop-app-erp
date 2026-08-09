import { test, expect } from '../fixtures';
import type { Page } from '@playwright/test';
import { GraphQLClient } from '../pages';
import { loginAsRole, ROLE_ACCOUNTS } from './_helper';

/**
 * 角色账号池运行时认证 + 角色解析 Proof（plan 2026-08-10-0119-1 / P2.2b）。
 *
 * 对 P2.2b 新增 9 角色账号各跑 `LoginApi__login(principalId, principalSecret="123")`，断言
 * `userInfo.roleInfos` 含预期业务 roleId（镜像 P2.2a Proof B 范式：`LoginServiceImpl.buildUserContext`
 * 经 `nop_auth_user_role`→`nop_auth_role` join 解析得 `IUserContext.getRoles()`，`roleInfos` 忠实
 * 反映该集合）。覆盖：
 *   - 9 账号认证成功（CSV 种子加载 + BCrypt 密码 "123" 往返）；
 *   - 角色解析与种子绑定一致（含**双命名空间 Proof**：role-biz-admin 解析为业务「管理员」**非**
 *     平台 `admin`——横切关注点 2）。
 *
 * 另证 `loginAsRole` 真实角色登录可演示：fresh page 上 `loginAsRole(page, '财务员')` 登录成功 +
 * 运行时身份 = role-finance；`loginAsRole(page, 'restricted')` 登录成功 + 身份 = role-restricted。
 *
 * enforcement OFF（三开关 `%test` profile false）：账号惰性加载不触发 FNPT 检查，证明 = 种子正确性
 * （认证 + 角色解析），非权限拒绝（拒绝归 P2.4/E1.x 翻启后）。
 */

interface LoginResult {
  userInfo: { userName: string; roleInfos: Array<{ roleId: string; roleName: string }> } | null;
}

async function loginViaGraphql(
  page: Page,
  username: string,
  password = '123',
): Promise<LoginResult> {
  const gql = new GraphQLClient(page);
  const json: any = await gql.raw(
    `mutation { LoginApi__login(loginType:1, principalId:${JSON.stringify(username)}, principalSecret:${JSON.stringify(password)}) { userInfo { userName roleInfos { roleId roleName } } } }`,
  );
  expect(json?.errors ?? null, `LoginApi__login(${username}) should not return errors`).toBeNull();
  expect(json?.data?.LoginApi__login?.userInfo, `LoginApi__login(${username}) must return userInfo`).toBeTruthy();
  return { userInfo: json.data.LoginApi__login.userInfo };
}

/**
 * 从浏览器 cookie 中解码 `__Host-nop-token` JWT，提取 `preferred_username`（UI 登录后由平台
 * HTTP-only cookie 设置，`page.context().cookies()` 可读——Playwright API 不受 httpOnly 限制）。
 * 用于证明 `loginAsRole` 的 UI 登录确实建立了预期身份的会话。
 */
async function readLoggedInUsername(page: Page): Promise<string> {
  const cookies = await page.context().cookies();
  const token = cookies.find((c) => c.name === '__Host-nop-token')?.value ?? '';
  const parts = token.split('.');
  if (parts.length < 2) return '';
  try {
    const payload = JSON.parse(Buffer.from(parts[1], 'base64').toString('utf8'));
    return payload.preferred_username ?? '';
  } catch {
    return '';
  }
}

/**
 * 证明 UI 登录建立的会话可执行业务查询（enforcement OFF 下所有角色等价全通——断言查询可达
 * 非权限拒绝；会话有效性证明 = UI 登录产出了有效 nop-token cookie）。
 */
async function assertSessionWorks(page: Page): Promise<void> {
  const resp = await page.request.post('/graphql', {
    data: { query: '{ ErpMdCurrency__findPage(query:{offset:0,limit:1}){ total } }' },
  });
  const json: any = await resp.json();
  expect(json?.errors ?? null, 'business query should succeed with session cookie').toBeNull();
  expect(json?.data?.ErpMdCurrency__findPage, 'business query should return data').toBeTruthy();
}

function expectRoleContains(roleIds: string[], expected: string, username: string): void {
  expect(
    roleIds,
    `user ${username} should have roleId "${expected}" in roleInfos, got ${JSON.stringify(roleIds)}`,
  ).toContain(expected);
}

// 9 角色账号 → 预期 roleId（与 nop_auth_user_role.csv 种子逐字一致）
const ROLE_ACCOUNT_CASES: Array<{ username: string; expectedRoleId: string; label: string }> = [
  { username: 'role-finance', expectedRoleId: '财务员', label: 'finance 正向' },
  { username: 'role-biz-admin', expectedRoleId: '管理员', label: 'finance reverseClose（业务管理员，非平台 admin）' },
  { username: 'role-b2b-admin', expectedRoleId: 'B2B 管理员', label: 'b2b admin' },
  { username: 'role-b2b-recon', expectedRoleId: 'B2B 对账员', label: 'b2b recon' },
  { username: 'role-mfg-lead', expectedRoleId: '生产主管', label: 'mfg lead' },
  { username: 'role-inventory', expectedRoleId: '库管员', label: 'inventory' },
  { username: 'role-hr-salary', expectedRoleId: '薪酬审批人', label: 'hr salary' },
  { username: 'role-hr', expectedRoleId: 'HR 专员', label: 'hr leave' },
  { username: 'role-restricted', expectedRoleId: 'user', label: '通用受限（平台 user）' },
];

test.describe('P2.2b 角色账号池：运行时认证 + 角色解析', () => {
  for (const c of ROLE_ACCOUNT_CASES) {
    test(`LoginApi__login(${c.username}) 解析 roleId="${c.expectedRoleId}"（${c.label}）`, async ({ page }) => {
      const result = await loginViaGraphql(page, c.username);
      expect(result.userInfo, `userInfo must be present for ${c.username}`).toBeTruthy();
      const roleIds = (result.userInfo!.roleInfos ?? []).map((r) => r.roleId);
      expectRoleContains(roleIds, c.expectedRoleId, c.username);
    });
  }

  test('双命名空间 Proof：role-biz-admin 解析为业务「管理员」非平台 admin', async ({ page }) => {
    const result = await loginViaGraphql(page, 'role-biz-admin');
    const roleIds = (result.userInfo!.roleInfos ?? []).map((r) => r.roleId);
    expect(roleIds, `role-biz-admin must bind business 管理员, not platform admin`).toContain('管理员');
    expect(roleIds, `role-biz-admin must NOT receive platform admin skip-check bypass`).not.toContain('admin');
    expect(roleIds, `role-biz-admin must NOT receive platform nop-admin`).not.toContain('nop-admin');
  });

  test('密码 "123" 复用 nop BCrypt hash 往返（9 账号同密码全认证成功）', async ({ page }) => {
    for (const c of ROLE_ACCOUNT_CASES) {
      const result = await loginViaGraphql(page, c.username);
      expect(result.userInfo, `${c.username} must authenticate with shared password "123"`).toBeTruthy();
    }
  });

  test('loginAsRole 真实角色登录可演示：loginAsRole(page,"财务员") → 身份 role-finance', async ({ page }) => {
    // loginAsRole 完成 = UI 表单提交成功（login() 的 waitForURL 离开 /auth/login 未超时）
    await loginAsRole(page, '财务员');
    // JWT cookie 证明 UI 登录建立的身份 = role-finance
    const username = await readLoggedInUsername(page);
    expect(username, `loginAsRole('财务员') should authenticate as role-finance`).toBe('role-finance');
    // 会话有效（业务查询可达）
    await assertSessionWorks(page);
  });

  test('loginAsRole 真实角色登录可演示：loginAsRole(page,"restricted") → 身份 role-restricted', async ({ page }) => {
    await loginAsRole(page, 'restricted');
    const username = await readLoggedInUsername(page);
    expect(username, `loginAsRole('restricted') should authenticate as role-restricted`).toBe('role-restricted');
    await assertSessionWorks(page);
  });

  test('loginAsRole 未知 key 回退 restricted（保守：最小权限）', async ({ page }) => {
    await loginAsRole(page, 'requester');
    const username = await readLoggedInUsername(page);
    expect(username, `unknown key 'requester' must fall back to role-restricted`).toBe('role-restricted');
  });
});

void ROLE_ACCOUNTS;
