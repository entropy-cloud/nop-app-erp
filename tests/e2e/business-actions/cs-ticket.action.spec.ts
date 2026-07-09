import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, eqFilter, deleteByFilter, deleteById } from './_helper';

/**
 * CS Ticket 六态状态机浏览器层 E2E（plan 2026-07-09-0814-2 Phase 3）。
 *
 * 验证自定义 @BizMutation（assign/start/resolve/close/cancel）经 GraphQL /graphql 的全栈可达性 + status 翻转 +
 * 非法迁移 ErrorCode 验证。
 * 权威状态机（docs/design/customer-service/state-machine.md，六态无 OPEN）：
 *   NEW → ASSIGNED(assign) → IN_PROGRESS(start) → RESOLVED(resolve) → CLOSED(close)；
 *   非终态 → CANCELLED(cancel)。非法迁移抛 ERR_INVALID_TICKET_STATUS_TRANSITION（经 GraphQL 返回 errors）。
 *
 * 种子引用（init-data）：cs_ticket_type id=1(TT-COMPLAIGHT) / id=2(TT-INQUIRY)；customer CUST-001 id=1；org id=2。
 * resolve 在 deadline 为空（未挂 SLA）时置 isSlaCompleted=true，close 无需 remark。
 */

test.describe('CS Ticket six-state machine actions', () => {
  test('save(NEW) → assign → start → resolve → close full lifecycle', async ({ page }) => {
    await loginAndNavigate(page, '/ErpCsTicket-main');

    const code = `E2E-TKT-${Date.now()}`;
    const ticket = await createViaSave(
      page, 'ErpCsTicket',
      {
        code,
        subject: 'E2E Action Ticket',
        customerId: 1,
        ticketTypeId: 1,
        priority: 'HIGH',
        status: 'NEW',
        docStatus: 'ACTIVE',
        approveStatus: 'UNSUBMITTED',
        orgId: 2,
      },
      'id status',
    );
    expect(ticket.id, '__save should create a NEW ticket').toBeTruthy();

    // assign: NEW → ASSIGNED
    const assigned = await callMutationOk(
      page, 'ErpCsTicket', 'assign', { ticketId: ticket.id, assignedToId: 'user-e2e' }, 'id status assignedToId',
    );
    expect(assigned.status, 'assign should transition NEW → ASSIGNED').toBe('ASSIGNED');
    expect(assigned.assignedToId, 'assign should record assignee').toBe('user-e2e');

    // start: ASSIGNED → IN_PROGRESS
    const started = await callMutationOk(
      page, 'ErpCsTicket', 'start', { ticketId: ticket.id }, 'id status',
    );
    expect(started.status, 'start should transition ASSIGNED → IN_PROGRESS').toBe('IN_PROGRESS');

    // resolve: IN_PROGRESS → RESOLVED
    const resolved = await callMutationOk(
      page, 'ErpCsTicket', 'resolve', { ticketId: ticket.id, resolution: 'E2E fixed' }, 'id status',
    );
    expect(resolved.status, 'resolve should transition IN_PROGRESS → RESOLVED').toBe('RESOLVED');

    // close: RESOLVED → CLOSED
    const closed = await callMutationOk(
      page, 'ErpCsTicket', 'close', { ticketId: ticket.id }, 'id status',
    );
    expect(closed.status, 'close should transition RESOLVED → CLOSED').toBe('CLOSED');

    const verified = await verifyState(page, 'ErpCsTicket', ticket.id, 'status');
    expect(verified.status, '__get should confirm CLOSED').toBe('CLOSED');

    // 非法迁移：CLOSED → start（须 ASSIGNED），经 GraphQL 返回 errors（ErrorCode）
    const rej = await callMutation(page, 'ErpCsTicket', 'start', { ticketId: ticket.id }, 'id');
    expect(rej.errors, 'start from CLOSED should be rejected as illegal transition').toBeTruthy();

    // 清理：状态机迁移写 TicketAction 审计 + resolve 可能触发 CSAT 调查，
    // 污染 cs-ticket-sla-csat 报表数值断言。逐域删除：actions → survey → ticket
    await deleteByFilter(page, 'ErpCsTicketAction', eqFilter('ticketId', Number(ticket.id)));
    await deleteByFilter(page, 'ErpCsSurvey', eqFilter('ticketId', Number(ticket.id)));
    await deleteById(page, 'ErpCsTicket', ticket.id);
  });

  test('cancel from NEW directly (NEW → CANCELLED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpCsTicket-main');

    const ticket = await createViaSave(
      page, 'ErpCsTicket',
      {
        code: `E2E-TKT-CNL-${Date.now()}`,
        subject: 'E2E Cancel Ticket',
        customerId: 1,
        ticketTypeId: 2,
        priority: 'NORMAL',
        status: 'NEW',
        docStatus: 'ACTIVE',
        approveStatus: 'UNSUBMITTED',
        orgId: 2,
      },
      'id status',
    );

    const cancelled = await callMutationOk(
      page, 'ErpCsTicket', 'cancel', { ticketId: ticket.id, cancelReason: 'E2E duplicate' }, 'id status',
    );
    expect(cancelled.status, 'cancel should transition NEW → CANCELLED').toBe('CANCELLED');

    // 清理：cancel 写 TicketAction 审计
    await deleteByFilter(page, 'ErpCsTicketAction', eqFilter('ticketId', Number(ticket.id)));
    await deleteById(page, 'ErpCsTicket', ticket.id);
  });
});
