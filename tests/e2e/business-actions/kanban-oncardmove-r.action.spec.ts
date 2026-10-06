import { test, expect, loginAndNavigate, createViaSave, verifyState, deleteById, deleteByFilter, eqFilter, GraphQLClient } from './_helper';
import { readFileSync } from 'fs';
import { resolve } from 'path';

/**
 * 三看板 onCardMove /r/ 直发探针（plan 2026-10-06-0355-2 Phase 2）。
 *
 * 背景：kanban onCardMove 事件处理器的 ajax args 曾携带 @mutation: 标量参数 URL——
 * 壳层 nopRpcResolver 对未注册 @mutation: 回退 {data:...} 信封，标量参数操作报
 * unknown-operation-arg（G-002/F13 陷阱的事件处理器族，批次 1 按钮清单漏网），
 * 拖拽改状态全链必败。本批三看板 URL 改 /r/Entity__method 直发。
 *
 * 证明策略：①结构断言（三文件零 @mutation: 残留、/r/ 形态在位、prj 载荷无 i18nEn 杂键）；
 * ②fetch 级分支探针——onCardMove 发出的正是这些 /r/ 标量请求（/r/ 透传语义已由
 * flux-rewired-buttons 真实点击链实证），逐分支断言 rpc ok + 状态翻转：
 * prj startTask（种子任务查找，f13 同款）；cs assign/start/resolve/cancel 四分支（plan 契约）。
 */

type Page = import('@playwright/test').Page;

async function postR(page: Page, url: string, body: Record<string, unknown>) {
  return page.evaluate(
    async ({ url, body }) => {
      const resp = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(body),
      });
      const json = await resp.json().catch(() => null);
      return { httpStatus: resp.status, json } as { httpStatus: number; json: any };
    },
    { url, body },
  );
}

test.describe('kanban onCardMove /r/ direct URL (plan 2026-10-06-0355-2)', () => {
  test('structural: three kanban files carry zero @mutation: and /r/ in place', async () => {
    const files = [
      'module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/ErpPrjTask/kanban.flux.yaml',
      'module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicket/kanban.flux.yaml',
      'module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/ErpCrmLead/opportunity-kanban.flux.yaml',
    ];
    for (const f of files) {
      const text = readFileSync(resolve(__dirname, '../../../', f), 'utf-8');
      expect(text, `${f} must not use @mutation:`).not.toContain('@mutation:');
    }
    const prj = readFileSync(resolve(__dirname, '../../../', files[0]), 'utf-8');
    expect(prj, 'prj branches use /r/ direct form').toContain('/r/ErpPrjTask__startTask');
    expect(prj, 'prj data payload carries no stray i18nEn key').not.toContain('i18nEn: Blocked via kanban drag');
  });

  test('prj startTask branch: /r/ scalar body flips task to IN_PROGRESS', async ({ page }) => {
    await loginAndNavigate(page, '/prj-task-kanban');
    const gql = new GraphQLClient(page);
    const found: any = await gql.raw(
      `query{ ErpPrjTask__findPage(filter_status:"TODO",limit:1){ items{ id status } } }`,
      {},
    );
    let todo = found?.data?.ErpPrjTask__findPage?.items?.[0];
    let created = false;
    if (!todo) {
      // 无种子 TODO 时自建（projectId=1 种子项目；projectId/title/status 为必填面）
      todo = await createViaSave(
        page, 'ErpPrjTask',
        { projectId: '1', title: `kanban r-probe ${Date.now()}`, status: 'TODO' },
        'id status',
      );
      created = true;
      expect(todo.id, 'TODO task created').toBeTruthy();
    }

    try {
      const { httpStatus, json } = await postR(page, '/r/ErpPrjTask__startTask', { taskId: todo.id });
      expect(httpStatus, 'http 200').toBe(200);
      expect(json?.status, 'rpc status 0').toBe(0);
      const st = await verifyState(page, 'ErpPrjTask', todo.id, 'status');
      expect(st.status, 'startTask flips TODO → IN_PROGRESS').toBe('IN_PROGRESS');
    } finally {
      if (created) await deleteById(page, 'ErpPrjTask', todo.id).catch(() => {});
    }
  });

  test('cs assign/start/resolve/cancel branches: /r/ scalar bodies flip states', async ({ page }) => {
    await loginAndNavigate(page, '/ErpCsTicket-main');

    const code = `E2E-KANBAN-CS-${Date.now()}`;
    const ticket = await createViaSave(
      page, 'ErpCsTicket',
      {
        code,
        subject: 'E2E kanban r-probe',
        customerId: '1',
        ticketTypeId: '1',
        priority: 'HIGH',
        status: 'NEW',
        docStatus: 'ACTIVE',
        approveStatus: 'UNSUBMITTED',
        orgId: '2',
      },
      'id status',
    );
    expect(ticket.id, 'NEW ticket created').toBeTruthy();

    try {
      // col-ASSIGNED → assign（onCardMove 仅传 ticketId；assignedToId @Optional）
      const assign = await postR(page, '/r/ErpCsTicket__assign', { ticketId: ticket.id });
      expect(assign.httpStatus, 'assign http 200').toBe(200);
      expect(assign.json?.status, 'assign rpc ok').toBe(0);
      expect((await verifyState(page, 'ErpCsTicket', ticket.id, 'status')).status).toBe('ASSIGNED');

      // col-IN_PROGRESS → start
      const start = await postR(page, '/r/ErpCsTicket__start', { ticketId: ticket.id });
      expect(start.json?.status, 'start rpc ok').toBe(0);
      expect((await verifyState(page, 'ErpCsTicket', ticket.id, 'status')).status).toBe('IN_PROGRESS');

      // col-RESOLVED → resolve（resolution @Optional + processor null 安全）
      const resolveResp = await postR(page, '/r/ErpCsTicket__resolve', { ticketId: ticket.id });
      expect(resolveResp.json?.status, 'resolve rpc ok').toBe(0);
      expect((await verifyState(page, 'ErpCsTicket', ticket.id, 'status')).status).toBe('RESOLVED');

      // col-CANCELLED → cancel（RESOLVED → CANCELLED 合法迁移边，cs-026-r3 路由）
      const cancel = await postR(page, '/r/ErpCsTicket__cancel', { ticketId: ticket.id, cancelReason: 'e2e-probe' });
      expect(cancel.json?.status, 'cancel rpc ok').toBe(0);
      expect((await verifyState(page, 'ErpCsTicket', ticket.id, 'status')).status).toBe('CANCELLED');
    } finally {
      // 清理：状态机迁移写 TicketAction 审计，逐域删除防污染 cs 报表断言（cs-ticket spec 同款）
      await deleteByFilter(page, 'ErpCsTicketAction', eqFilter('ticketId', ticket.id)).catch(() => {});
      await deleteByFilter(page, 'ErpCsSurvey', eqFilter('ticketId', ticket.id)).catch(() => {});
      await deleteById(page, 'ErpCsTicket', ticket.id).catch(() => {});
    }
  });
});
