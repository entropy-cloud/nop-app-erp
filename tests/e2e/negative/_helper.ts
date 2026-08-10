/**
 * 负向隔离测试原语（plan 2026-08-09-2210-2 / P2.3，permissions-enforcement mission）。
 *
 * 关注点分离：本模块隔离「负向隔离测试」原语供 E1.x（action 级负向）/ E2.x（data 级负向）
 * 清晰消费，不复用 `business-actions/_helper.ts`（正向业务动作原语职责不同）。`.ts` 文件
 * 在本仓库 `opencode.json` 下可自由编辑（无 `_*.ts` deny 规则）——独立模块是清洁设计选择。
 *
 * **rejection-source-agnostic 设计**：`expectActionDenied` 同时适用于
 *   (1) 业务逻辑拒绝（如 hr-payroll markPaid UNSUBMITTED 守卫抛 NopException，message 含「不允许执行该操作」）；
 *   (2) enforcement 拒绝（action-auth 拦截抛 `nop.err.auth.no-permission`，message「没有访问权限」）。
 * 两者同经 `GraphQLEngine.buildGraphQLResponse` → 同 `{errors}` 信封 + 同 `extensions["nop-error-code"]`
 * 位置（Phase 1 静态表征结论），故按 `{errors}` 存在 + 可选 token/errorCode 匹配设计。
 *
 * **enforcement 拒绝形状（运行时已确认 P2.4 dry-run，形状与静态表征一致）**：
 *   - HTTP status 恒 200（`GraphQLWebService.runGraphQL` 硬编码 200，非 403）；
 *   - body `{errors:[{message}], data:null, extensions:{"nop-error-code":..., "nop-status":-1}}`；
 *   - errorCode 在**顶层** `extensions["nop-error-code"]`，不在每条 error entry 内；
 *   - `error.params` 不序列化进 JSON（仅用于 message 渲染）。
 *   enforcement 专用 errorCode 常量（`nop.err.auth.no-permission` / `no-permission-for-field` /
 *   `no-data-auth`）预留给 P2.4 运行时确认后收敛。
 *
 * **data-auth 行过滤形状（Phase 1 表征）**：`DefaultDataAuthChecker.getFilter` 注入 SQL WHERE →
 * `__findPage` 静默返回过滤后行集（无 errors，total/items 减少）。故「越权数据被过滤」=
 * 断言特定行 absent / 行集收敛，**非** errors 断言——独立原语 `expectRowsHidden/Visible`。
 */

import { test, expect } from '../fixtures';
import type { Page } from '@playwright/test';
import { login } from '../pages';
import { GraphQLClient } from '../pages';
import {
  callMutation,
  callQuery,
  findItems,
  findPageTotal,
} from '../business-actions/_helper';

export { test, expect };
// Re-export for consumer convenience: negative spec 单一 import 入口（避免跨模块 import 认知负担）。
export { callMutation, callQuery, findItems, findPageTotal };

/**
 * Scalar-returning mutation 调用（如 `ErpFinVoucher.post/reverse` 返 Long、`ErpB2bAsn.handleInboundWebhook` 返 Long）。
 *
 * `callMutation` 默认 selection=`'id'` 会产出 `{ Action{ id } }` 子选择，对 Long/scalar 返回类型
 * 触发 GraphQL 校验错误 `[Long]不是对象类型，不支持字段选择`（enforcement 检查在校验之后，永不可达）。
 * 本原语走 `GraphQLClient.raw` 无子选择查询，使 Long 返回型动作也能抵达 enforcement 检查层。
 * 返回与 `callMutation` 同形 `{data, errors, json}` 信封，可直接喂 `expectActionDenied`。
 */
export async function callMutationScalar(
  page: Page,
  entityName: string,
  action: string,
  args: Record<string, unknown>,
): Promise<{ data: any | null; errors: any[] | null; json: any }> {
  const gql = new GraphQLClient(page);
  const parts: string[] = [];
  for (const [name, arg] of Object.entries(args)) {
    parts.push(`${name}:${JSON.stringify(arg)}`);
  }
  const query = `mutation{ ${entityName}__${action}(${parts.join(',')}) }`;
  const json: any = await gql.raw(query);
  return {
    data: json?.data?.[`${entityName}__${action}`] ?? null,
    errors: json?.errors ?? null,
    json,
  };
}

/**
 * GraphQL 调用结果信封（callMutation/callQuery 返回值）。
 * errors = json.errors（非空时表示拒绝，含 message）；json = 完整响应（含顶层 extensions）。
 */
export interface ActionResult {
  data: any | null;
  errors: any[] | null;
  json: any;
}

/**
 * expectActionDenied 选项。
 *
 * - `token`：message 子串匹配（`JSON.stringify(errors)` 包含检查）。用于中文 token 断言
 *   （如业务拒绝「不允许执行该操作」/ enforcement 拒绝「没有访问权限」）。
 * - `errorCode`：精确匹配 `json.extensions["nop-error-code"]`（如 enforcement `nop.err.auth.no-permission`）。
 */
export interface ActionDeniedOptions {
  token?: string;
  errorCode?: string;
}

/**
 * enforcement 专用 ErrorCode 常量（运行时已确认 P2.4 dry-run，形状与静态表征一致）。
 * P2.4 翻启 action-auth 后经 `tests/e2e/negative/p2.4-proof-b.smoke.spec.ts` 实测拒绝信封
 * `{data:null, errors:[{message:"没有访问权限"}], extensions:{"nop-error-code":"nop.err.auth.no-permission","nop-status":-1}}`，
 * NO_PERMISSION 常量值收敛无需调整。负向 spec 可用这些常量做 `opts.errorCode` 精确断言。
 */
export const ENFORCEMENT_ERROR_CODES = {
  /** 顶层 @BizMutation/@BizQuery action 缺 FNPT 权限点（GraphQLActionAuthChecker.java:102）。 */
  NO_PERMISSION: 'nop.err.auth.no-permission',
  /** 嵌套字段权限拒绝（GraphQLActionAuthChecker.java:110）。 */
  NO_PERMISSION_FOR_FIELD: 'nop.err.auth.no-permission-for-field',
  /** 单实体 data-auth 拒绝 / 有规则但角色不匹配（DefaultDataAuthChecker.java getFilter 分支 B）。 */
  NO_DATA_AUTH: 'nop.err.auth.no-data-auth',
} as const;

/**
 * 原语：断言「未授权动作被拒绝」（rejection-source-agnostic）。
 *
 * 断言 `result.errors` 真值（GraphQL errors 数组存在）+ 可选 token（message 子串）/ errorCode
 * （`extensions["nop-error-code"]` 精确）匹配。返回 errors 数组供链式断言。
 *
 * 适用于业务逻辑拒绝（NopException 守卫）与 enforcement 拒绝（action-auth 拦截）——两者同信封。
 *
 * @example
 * // 业务拒绝载体（demo 范式，action-auth OFF 下证明机制成立）
 * const rej = await callMutation(page, 'ErpHrSalary', 'markPaid', { salaryId }, 'id');
 * expectActionDenied(rej, { token: '不允许执行该操作' });
 *
 * @example
 * // enforcement 拒绝（P2.4/E1.x 翻启 action-auth 后）
 * const rej = await callMutation(page, 'ErpFinVoucher', 'reverseClose', { id }, 'id');
 * expectActionDenied(rej, {
 *   errorCode: ENFORCEMENT_ERROR_CODES.NO_PERMISSION,
 *   token: '没有访问权限',
 * });
 */
export function expectActionDenied(
  result: ActionResult,
  opts?: ActionDeniedOptions,
): any[] {
  expect(
    result.errors,
    'expectActionDenied: expected GraphQL errors array to be present (action denied)',
  ).toBeTruthy();
  const errors = result.errors as any[];
  expect(
    errors.length,
    'expectActionDenied: expected at least one error entry',
  ).toBeGreaterThan(0);

  if (opts?.token) {
    expect(
      JSON.stringify(errors),
      `expectActionDenied: expected errors to contain token "${opts.token}"`,
    ).toContain(opts.token);
  }

  if (opts?.errorCode) {
    const actualCode = result.json?.extensions?.['nop-error-code'];
    expect(
      actualCode,
      `expectActionDenied: expected extensions["nop-error-code"] to be "${opts.errorCode}"`,
    ).toBe(opts.errorCode);
  }

  return errors;
}

/**
 * 原语：断言「越权数据被过滤」（data-auth 行级规则排除后特定行不可见）。
 *
 * data-auth 静默过滤（无 errors），absent 是唯一可观测信号：断言匹配 `filter` 的行集为空
 * （越权行被过滤）。返回空数组（语义明确，调用方可继续断言）。
 *
 * **运行时 demo 随 E2.1**（data-auth 双开关 OFF 下行过滤不可观测；本原语交付 + 静态签名 Proof）。
 *
 * @example
 * // E2.1 翻启 data-auth 后：断言其他用户的行不可见
 * await expectRowsHidden(page, 'ErpPrjTask', eqFilter('assigneeId', otherUserId), 'id');
 */
export async function expectRowsHidden<T = any>(
  page: Page,
  entity: string,
  filter: Record<string, unknown>,
  selection: string,
): Promise<T[]> {
  const items = await findItems<T>(page, entity, filter, selection);
  expect(
    items.length,
    `expectRowsHidden: expected 0 visible rows for ${entity} matching filter (越权行应被过滤), got ${items.length}`,
  ).toBe(0);
  return items;
}

/**
 * 原语：断言「可见行集收敛」（data-auth 过滤后剩余可见行符合预期）。
 *
 * `expectedCount` 提供时断言精确收敛至该数（如 admin 全见 vs 受限账号只见自有行）；
 * 省略时断言可见行集 ≥ 1（存在性）。返回可见行数组。
 *
 * **运行时 demo 随 E2.1**（data-auth 双开关 OFF 下行过滤不可观测）。
 *
 * @example
 * // E2.1 后：受限账号只见自有 3 行
 * await expectRowsVisible(page, 'ErpPrjTask', eqFilter('assigneeId', myUserId), 'id', 3);
 */
export async function expectRowsVisible<T = any>(
  page: Page,
  entity: string,
  filter: Record<string, unknown>,
  selection: string,
  expectedCount?: number,
): Promise<T[]> {
  if (expectedCount !== undefined) {
    const total = await findPageTotal(page, entity, filter);
    expect(
      total,
      `expectRowsVisible: expected ${expectedCount} visible rows for ${entity} (行集收敛), got ${total}`,
    ).toBe(expectedCount);
  }
  const items = await findItems<T>(page, entity, filter, selection);
  if (expectedCount === undefined) {
    expect(
      items.length,
      `expectRowsVisible: expected ≥1 visible row for ${entity}, got 0`,
    ).toBeGreaterThan(0);
  }
  return items;
}

/**
 * 角色账号池（plan 2026-08-10-0119-1 / P2.2b）。
 *
 * key = 业务 roleId 字面值（与 `nop_auth_role.csv` 一致）+ username 别名；
 * value = {username, password}。覆盖 E1.1 五高危域 8 授权角色 + 1 通用受限账号 + 平台 admin
 * 正向控制。账号种子见 `app-erp-all/_vfs/_init-data/nop_auth_user.csv`（userId 2-10）。
 *
 * **双命名空间（横切 2）**：业务「管理员」（roleId=「管理员」，账号 role-biz-admin）≠ 平台 `admin`
 * （roleId=`admin`，账号 nop，触发 skip-check 兜底）。两 key 各自映射，不可互换。
 *
 * **E1.2 扩展点**：新增角色账号按 `role-<slug>` 命名 + 小整数 userId 追加 CSV 行 +
 * 此处追加 ROLE_ACCOUNTS 条目（机械扩展，无机制性返工）。
 */
const COMMON_PASS = '123';
export const ROLE_ACCOUNTS: Record<string, { username: string; password: string }> = {
  // 平台 admin 正向控制（skip-check 兜底触发）
  admin: { username: 'nop', password: COMMON_PASS },
  nop: { username: 'nop', password: COMMON_PASS },
  // E1.1 五高危域 8 授权角色（业务 roleId 字面值 → 角色账号）
  财务员: { username: 'role-finance', password: COMMON_PASS },
  管理员: { username: 'role-biz-admin', password: COMMON_PASS },
  'B2B 管理员': { username: 'role-b2b-admin', password: COMMON_PASS },
  'B2B 对账员': { username: 'role-b2b-recon', password: COMMON_PASS },
  生产主管: { username: 'role-mfg-lead', password: COMMON_PASS },
  库管员: { username: 'role-inventory', password: COMMON_PASS },
  薪酬审批人: { username: 'role-hr-salary', password: COMMON_PASS },
  'HR 专员': { username: 'role-hr', password: COMMON_PASS },
  // 通用受限账号（仅绑平台 user 角色，无敏感 FNPT，供 P2.4 dry-run 全 403 影响面）
  user: { username: 'role-restricted', password: COMMON_PASS },
  restricted: { username: 'role-restricted', password: COMMON_PASS },
  // username 别名（同账号，便于调用方按 username 直传）
  'role-finance': { username: 'role-finance', password: COMMON_PASS },
  'role-biz-admin': { username: 'role-biz-admin', password: COMMON_PASS },
  'role-b2b-admin': { username: 'role-b2b-admin', password: COMMON_PASS },
  'role-b2b-recon': { username: 'role-b2b-recon', password: COMMON_PASS },
  'role-mfg-lead': { username: 'role-mfg-lead', password: COMMON_PASS },
  'role-inventory': { username: 'role-inventory', password: COMMON_PASS },
  'role-hr-salary': { username: 'role-hr-salary', password: COMMON_PASS },
  'role-hr': { username: 'role-hr', password: COMMON_PASS },
  'role-restricted': { username: 'role-restricted', password: COMMON_PASS },
};

/**
 * 角色登录 indirection（负向测试脚手架，plan 2026-08-10-0119-1 / P2.2b 真实映射填充）。
 *
 * 按 `roleOrUser` 从 {@link ROLE_ACCOUNTS} 解析账号凭据，先**防御性清空会话**（cookies +
 * localStorage，支持 fresh page 与复用 page 两种调用形态——`Navigation.ts#login` 在已登录
 * 页会跳过登录，切换身份须先清会话），再委派既有 `login(page, username, password)`。
 *
 * **解析顺序**：(1) 精确 roleId/别名匹配 → (2) 回退 `restricted`（保守：未知角色 = 最小权限，
 * 保证负向断言倾向拒绝而非意外放行）。
 *
 * enforcement OFF（三开关 false）下，所有角色账号行为等价（action-auth 不拦截）——故 P2.3 既有
 * demo `loginAsRole(page, 'requester')`（'requester' 回退 restricted）仍绿：身份切换不影响业务
 * 逻辑拒绝载体的断言（markPaid UNSUBMITTED 守卫与身份无关）。enforcement 翻启后（P2.4/E1.x），
 * 受限账号才触发真权限拒绝。
 *
 * @param roleOrUser 角色 roleId（如「财务员」）/ username 别名（如 `role-finance`）/ 通用别名
 *   （`restricted`/`admin`/`nop`）；未知 key 回退 restricted
 */
export async function loginAsRole(page: Page, roleOrUser: string): Promise<void> {
  const account = ROLE_ACCOUNTS[roleOrUser] ?? ROLE_ACCOUNTS.restricted;
  // 防御性会话清空：复用 page 上切换身份时须先清旧会话（login 在已登录页会跳过）。
  // clearCookies 是 context 级（任何 URL 都生效）；localStorage.clear() 需同源 page——fresh page
  // 在 about:blank 时 localStorage 不可访问（SecurityError），try-catch 容错（fresh page 无残留）。
  await page.context().clearCookies();
  try {
    await page.evaluate(() => localStorage.clear());
  } catch {
    // about:blank / cross-origin：fresh page 无 localStorage 须清，静默跳过
  }
  await login(page, account.username, account.password);
}
