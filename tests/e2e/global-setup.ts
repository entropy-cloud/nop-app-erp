import { chromium, type FullConfig } from '@playwright/test';
import * as path from 'path';
import * as fs from 'fs';
import { performLogin, AUTH_FILE } from './auth';

export default async function globalSetup(config: FullConfig) {
  const baseURL = config.projects[0]?.use?.baseURL || 'http://127.0.0.1:8080';
  const useChromeChannel = !fs.existsSync(
    path.join(process.env.HOME || '', 'Library/Caches/ms-playwright/chromium_headless_shell-1228')
  );

  const browser = await chromium.launch({ headless: true, ...(useChromeChannel ? { channel: 'chrome' } : {}) });
  const context = await browser.newContext({ baseURL });
  const page = await context.newPage();

  try {
    await performLogin(page);

    const cookies = await context.cookies();
    const tokenCookie = cookies.find((c) => c.name === 'nop-token');
    if (tokenCookie) {
      await page.evaluate((token) => {
        localStorage.setItem('nop-token', token);
      }, tokenCookie.value);
    }

    // E2E 运行环境预置：部署种子仅有 2026-07 一个 OPEN 会计期间，而部分业务动作的
    // 凭证日期派生自 CoreMetrics.today()（cashRepay / laborPosting businessDate 兜底等，
    // 均无 date 参数可覆写）。运行日期超出种子期间时过账 resolveOpenPeriod 抛
    // erp.err.fin.posting.period-not-found（日期漂移，runbook「日期漂移防护」同源问题）。
    // 此处幂等预置「运行月」OPEN 期间（测试层环境预置，不改产品；fresh-DB 每次重建）。
    await ensureCurrentMonthOpenPeriod(page, context);

    await context.storageState({ path: AUTH_FILE });
  } finally {
    await browser.close();
  }
}

async function ensureCurrentMonthOpenPeriod(
  page: import('@playwright/test').Page,
  context: import('@playwright/test').BrowserContext,
): Promise<void> {
  try {
    const cookies = await context.cookies();
    const token =
      cookies.find((c) => c.name === '__Host-nop-token')?.value ??
      cookies.find((c) => c.name === 'nop-token')?.value;
    if (!token) return;

    const now = new Date();
    const y = now.getFullYear();
    const m = now.getMonth() + 1;
    const mm = String(m).padStart(2, '0');
    const start = `${y}-${mm}-01`;
    const endDay = new Date(y, m, 0).getDate();
    const end = `${y}-${mm}-${String(endDay).padStart(2, '0')}`;
    const today = `${y}-${mm}-${String(now.getDate()).padStart(2, '0')}`;
    const code = `E2E-AUTO-${y}${mm}`;

    const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
    // filter 须走 GraphQL variable（Map）——内联对象字面量不支持带 $ 前缀的 key。
    // 「期间包含 today」用受支持运算符表达（plan 2026-09-01-0527-2）：缺省过滤校验
    // 只允许 eq/in/dateBetween/dateTimeBetween（不支持 le/ge），故 startDate/endDate
    // 各用一条 dateBetween（value=[min,max]，含端点）表达 startDate<=today 与
    // endDate>=today 的包含语义。
    const findResp = await page.request.post('/graphql', {
      headers,
      data: {
        query:
          'query($f:Map){ ErpFinAccountingPeriod__findPage(query:{limit:200,filter:$f}){ items{ id code status } total } }',
        variables: {
          f: {
            $type: 'and',
            $body: [
              { $type: 'dateBetween', name: 'startDate', value: ['1900-01-01', today] },
              { $type: 'dateBetween', name: 'endDate', value: [today, '2999-12-31'] },
            ],
          },
        },
      },
    });
    const findJson: any = await findResp.json();
    if (findJson?.errors) {
      console.warn('[global-setup] period find failed:', JSON.stringify(findJson.errors));
    }
    const items = findJson?.data?.ErpFinAccountingPeriod__findPage?.items || [];
    if (items.some((p: any) => p.status === 'OPEN')) return;

    const saveResp = await page.request.post('/graphql', {
      headers,
      data: {
        query:
          'mutation($d:ErpFinAccountingPeriod__save_input){ ErpFinAccountingPeriod__save(data:$d){ id code } }',
        variables: {
          d: {
            code,
            name: `E2E auto open period ${code}`,
            orgId: '2',
            year: y,
            month: m,
            startDate: start,
            endDate: end,
            quarter: Math.ceil(m / 3),
            isAdjustment: false,
            status: 'OPEN',
          },
        },
      },
    });
    const saveJson: any = await saveResp.json();
    if (saveJson?.errors) {
      console.warn('[global-setup] ensureCurrentMonthOpenPeriod save failed:', JSON.stringify(saveJson.errors));
    }
  } catch (e) {
    console.warn('[global-setup] ensureCurrentMonthOpenPeriod error:', e);
  }
}
