/**
 * 报表下载角色可达性回归（plan 2026-10-06-1900-1 / roadmap WI-01 / F-R19）。
 *
 * 验证「持有报表菜单的业务角色账号 → `/p/Erp{Domain}Report__download` → 200 + 二进制产物」全链：
 * 每域一个具名角色账号（镜像该域报表菜单 roles 种子；禁用平台 admin——skip-check-for-admin 恒放行
 * 会产假绿，正是 F-R19 首轮「fin/md 假通过」的根因）。断言 200 + 非空 + XLSX 魔数（PK\x03\x04）。
 *
 * 历史：修复前四域统一 93B JSON 错误信封 `nop.err.auth.no-permission`（action-auth 资源缺口——
 * 报表 BizModel 为非实体纯 BizModel，codegen 不产资源点，手写 overlay 无 FNPT 注册）。
 */
import { test, expect } from '@playwright/test';
import { loginAsRole } from '../negative/_helper';

interface RoleDownloadCase {
  domain: string;
  bizName: string;
  reportName: string;
  roleAccount: string; // ROLE_ACCOUNTS key
  data: Record<string, unknown>;
}

const CASES: RoleDownloadCase[] = [
  {
    domain: 'fin',
    bizName: 'ErpFinReport',
    reportName: 'income-statement',
    roleAccount: '财务员',
    data: { periodId: '1' },
  },
  {
    domain: 'md',
    bizName: 'ErpMdReport',
    reportName: 'material-price-list',
    roleAccount: '采购员',
    data: {},
  },
  {
    domain: 'mfg',
    bizName: 'ErpMfgReport',
    reportName: 'production-variance-report',
    roleAccount: '生产主管',
    data: {},
  },
  {
    domain: 'cs',
    bizName: 'ErpCsReport',
    reportName: 'ticket-sla-csat-summary',
    roleAccount: '管理员',
    data: { ticketType: '1' },
  },
];

test.describe('report download role-account reachability (WI-01)', () => {
  for (const c of CASES) {
    test(`${c.domain} ${c.reportName} xlsx download as role account (${c.roleAccount})`, async ({ page }) => {
      await loginAsRole(page, c.roleAccount);

      // __Host-nop-token 为 Secure cookie，Node 侧请求不自动附带（P2.4 同源修正同款）——
      // 从浏览器 cookie 取值显式注入 Authorization（server 两者均接受）。
      const cookies = await page.context().cookies();
      const token =
        cookies.find((ck) => ck.name === '__Host-nop-token')?.value ??
        cookies.find((ck) => ck.name === 'nop-token')?.value;
      expect(token, 'role login must yield nop-token cookie').toBeTruthy();

      const resp = await page.request.post(`/p/${c.bizName}__download`, {
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        data: { reportName: c.reportName, renderType: 'xlsx', data: c.data },
      });
      expect(resp.status(), `${c.domain} download should be authorized for ${c.roleAccount}`).toBe(200);
      const buf = await resp.body();
      expect(buf.length, 'download payload must be non-empty').toBeGreaterThan(0);
      expect(buf.subarray(0, 4).toString('latin1'), 'XLSX magic (PK zip header)').toBe('PK\x03\x04');
    });
  }
});
