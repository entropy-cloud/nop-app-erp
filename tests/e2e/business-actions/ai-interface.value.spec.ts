import { test, expect, loginAndNavigate } from '../fixtures';
import { GraphQLClient } from '../pages';
import { rpc } from '../pages/RpcClient';

/** 从浏览器会话提取 token（cookie → Authorization Bearer），供 /r/ 通道复用同身份（e1-2 先例）。 */
async function bearerFromPage(page: import('@playwright/test').Page): Promise<string | undefined> {
  const cookies = await page.context().cookies();
  const tokenCookie =
    cookies.find((c) => c.name === '__Host-nop-token') ?? cookies.find((c) => c.name === 'nop-token');
  return tokenCookie ? `Bearer ${tokenCookie.value}` : undefined;
}

/**
 * E3.6 AI 接口层 value-spec（`ai-native-interface.md` 前置调研结论「最小落地集」items 3/4/5）：
 *
 * - item 3 REST/GraphQL 双通道一致性：同一 operation（`ErpFinDashboard__getDashboardKpi`）经
 *   `/graphql` 与 `/r/` 两通道（共享 IGraphQLEngine）数值一致断言；
 * - item 4 护栏负路径：无权限角色经 `/r/` 通道调用高影响 mutation（`ErpFinBadDebt__writeOff`，
 *   FNPT 声明）被拒（enforcement 门径复用，AI/用户共用 action 层无旁路）；
 *   **P2-E 归因修正 + 补齐（plan 2026-08-28-0219-1）**：E3 计划 Phase 7 ④ 的负路径证据实测的是
 *   既有 BadDebt mutation（非 E3 批新增 API 面）；新增 E3.5 管道 mutation 面（`uploadApDocument`）
 *   的无权限拒绝路径由本 spec `uploadApDocument denied for unauthorized role` 用例承载；
 * - item 5 调用方身份落账：REST 通道以 role-finance 身份经 E3.5 管道上传 →
 *   `ErpFinApDocument.createdBy` = 该账号 userId（审计标识 = 身份落账，actorType 不落地裁决）。
 */
test.describe('E3.6 AI interface layer (dual channel + guardrails + identity audit)', () => {
  test('getDashboardKpi returns identical values via /graphql and /r/ channels', async ({ page }) => {
    await loginAndNavigate(page, '/fin-dashboard-main');
    const gql = new GraphQLClient(page);
    const auth = await bearerFromPage(page);

    const viaGraphql: any = await gql.raw(
      'query{ ErpFinDashboard__getDashboardKpi }',
    );
    const kpiGraphql = viaGraphql?.data?.ErpFinDashboard__getDashboardKpi;
    expect(kpiGraphql, 'GraphQL channel should return KPI map').toBeTruthy();

    const viaRest = await page.request.post('/r/ErpFinDashboard__getDashboardKpi', {
      headers: auth ? { Authorization: auth } : {},
      data: {},
    }).then(r => r.json());
    expect(viaRest.status, 'REST /r/ channel should succeed (status 0)').toBe(0);
    const kpiRest = viaRest.data;
    expect(kpiRest, 'REST channel should return KPI map').toBeTruthy();

    // 双通道数值一致（共享 IGraphQLEngine，键集与代表性数值逐项对齐）
    expect(Object.keys(kpiRest).sort()).toEqual(Object.keys(kpiGraphql).sort());
    for (const key of Object.keys(kpiGraphql)) {
      expect(
        String(kpiRest[key]),
        `channel parity for ${key}: /r/=${kpiRest[key]} vs /graphql=${kpiGraphql[key]}`,
      ).toBe(String(kpiGraphql[key]));
    }
  });

  test('high-impact mutation denied for unauthorized role via /r/ channel', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBadDebt-main');
    // restricted 账号经 REST 登录（无 UI 登录 race），高影响 FNPT 声明动作经 /r/ 通道被拒
    const loginResp = await page.request.post('/r/LoginApi__login', {
      data: { loginType: 1, principalId: 'role-restricted', principalSecret: '123' },
    });
    const restrictedToken = (await loginResp.json())?.data?.accessToken;
    expect(restrictedToken, 'role-restricted REST login should return token').toBeTruthy();

    const resp = await page.request.post('/r/ErpFinBadDebt__writeOff', {
      headers: { Authorization: `Bearer ${restrictedToken}` },
      data: { arApItemId: '999999', reason: 'ai-interface-guard-proof' },
    });
    const json: any = await resp.json();
    expect(json?.status, 'denied call should be non-zero status').not.toBe(0);
    expect(
      JSON.stringify(json),
      'rejection should carry no-permission / 没有访问权限 token',
    ).toContain('没有访问权限');
  });

  test('uploadApDocument denied for unauthorized role via /r/ channel (P2-E)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinApDocument-main');
    // P2-E（plan 2026-08-28-0219-1）：E3 批新增 mutation 面的鉴权拒绝路径——
    // role-restricted 无 FNPT:ErpFinApDocument:uploadApDocument 权限（种子 roles=财务员），
    // action-auth 已登记该权限项，enforcement 应拒绝（区别于既有 BadDebt 负路径实测面）
    const loginResp = await page.request.post('/r/LoginApi__login', {
      data: { loginType: 1, principalId: 'role-restricted', principalSecret: '123' },
    });
    const restrictedToken = (await loginResp.json())?.data?.accessToken;
    expect(restrictedToken, 'role-restricted REST login should return token').toBeTruthy();

    const ts = Date.now();
    const base64 = Buffer.from(`收据\n收款单位：无权限上传探测\n金额：1.00\n`, 'utf-8').toString('base64');
    const resp = await page.request.post('/r/ErpFinApDocument__uploadApDocument', {
      headers: { Authorization: `Bearer ${restrictedToken}` },
      data: { fileName: `p2e-deny-${ts}.txt`, mimeType: 'text/plain', fileBase64: base64 },
    });
    const json: any = await resp.json();
    expect(json?.status, 'unauthorized uploadApDocument should be non-zero status').not.toBe(0);
    expect(
      JSON.stringify(json),
      'rejection should carry no-permission / 没有访问权限 token (FNPT enforcement, not business guard)',
    ).toContain('没有访问权限');
  });

  test('caller identity recorded on E3.5 pipeline upload via /r/ channel', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinApDocument-main');
    const gql = new GraphQLClient(page);
    const loginResp = await page.request.post('/r/LoginApi__login', {
      data: { loginType: 1, principalId: 'role-finance', principalSecret: '123' },
    });
    const financeToken = (await loginResp.json())?.data?.accessToken;
    expect(financeToken, 'role-finance REST login should return token').toBeTruthy();

    const ts = Date.now();
    const content = `增值税专用发票\n名称：北方钢铁供应商\n发票号码：AI-ID-${ts}\n开票日期：2026年08月22日\n金额（不含税）：500.00\n税额：65.00\n价税合计（大写）伍佰陆拾伍元整 565.00\n`;
    const base64 = Buffer.from(content, 'utf-8').toString('base64');

    let docId: string | null = null;
    try {
      const resp = await page.request.post('/r/ErpFinApDocument__uploadApDocument', {
        headers: { Authorization: `Bearer ${financeToken}` },
        data: { fileName: `ai-apd-${ts}.txt`, mimeType: 'text/plain', fileBase64: base64 },
      });
      const json: any = await resp.json();
      expect(json?.status, '/r/ upload should succeed for authorized role').toBe(0);
      docId = String(json?.data?.id);
      expect(docId).toBeTruthy();

      // item 5：调用方身份落账（userId 审计标识；%test use-user-id-for-audit-fields）
      // 服务端返回的持久化行（RECEIVED）即落账证据
      expect(json?.data?.createdBy, 'createdBy should equal role-finance userId (identity audit)').toBe('2');
      expect(json?.data?.status).toBe('RECEIVED');
    } finally {
      if (docId) await gql.delete('ErpFinApDocument', docId);
    }
  });
});
