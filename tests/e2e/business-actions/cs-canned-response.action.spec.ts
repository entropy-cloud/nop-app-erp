import { test, expect, loginAndNavigate, createViaSave, callQuery, verifyState, deleteByFilter, deleteById } from './_helper';

/**
 * CS Canned Response 预设应答浏览器层 E2E（plan 2026-07-11-1234-2 §Phase 4）。
 *
 * 验证自定义 @BizQuery/@BizMutation 经 GraphQL /graphql 的全栈可达性：
 *   - suggestForTicket：三级宏匹配（精确 > 类型 > 全局兜底）返回列表
 *   - renderTemplate：系统变量 + 自定义变量替换
 *   - applyCannedResponse：渲染 + usageCount 递增 + TicketAction NOTE 审计写入
 *
 * 种子引用（init-data）：customer id=1 CUST-001；ticket_type id=1。
 * 每个测试自建自清。
 */

test.describe('CS Canned Response actions', () => {
  test('suggestForTicket returns macro-matched responses + renderTemplate replaces vars + applyCannedResponse increments usage', async ({ page }) => {
    await loginAndNavigate(page, '/ErpCsCannedResponse-main');

    // 前置：建 CannedResponse（精确匹配 type=1 + priority=HIGH）
    const varDefs = JSON.stringify({
      variables: [
        { key: '{customer_name}', label: '客户名称', required: true },
        { key: '{ticket_id}', label: '工单编号', required: false },
      ],
    });
    const canned = await createViaSave(
      page, 'ErpCsCannedResponse',
      {
        code: `E2E-CR-${Date.now()}`,
        title: 'E2E 测试应答',
        content: '您好 {customer_name}，工单 {ticket_id} 由 {agent_name} 处理',
        variableDefs: varDefs,
        macroTicketTypeId: 1,
        macroPriority: 'HIGH',
        sequence: 10,
        isActive: true,
        usageCount: 0,
      },
      'id usageCount',
    );
    expect(canned.id).toBeTruthy();

    // 前置：建 Ticket（type=1, priority=HIGH → 精确匹配）
    const ticket = await createViaSave(
      page, 'ErpCsTicket',
      {
        code: `E2E-CR-TKT-${Date.now()}`,
        subject: 'E2E Canned Response Ticket',
        customerId: 1,
        ticketTypeId: 1,
        priority: 'HIGH',
        status: 'NEW',
        docStatus: 'ACTIVE',
        approveStatus: 'UNSUBMITTED',
        orgId: 2,
      },
      'id',
    );
    expect(ticket.id).toBeTruthy();

    // suggestForTicket（@BizQuery，返回 List<ErpCsCannedResponse> 复杂列表，需 selection set，非 callQuery 标量原语可表达）
    // 镜像 fin-reconciliation.action.spec.ts:235-239 inline query 范式
    const suggestResp = await page.request.post('/graphql', {
      data: {
        query: `query{ ErpCsCannedResponse__suggestForTicket(ticketId:${Number(ticket.id)}){ id title content macroTicketTypeId macroPriority sequence usageCount } }`,
      },
    });
    const suggestJson: any = await suggestResp.json();
    expect(suggestJson?.errors, `suggestForTicket should not return GraphQL errors: ${JSON.stringify(suggestJson?.errors)}`).toBeFalsy();
    const suggestions = suggestJson?.data?.ErpCsCannedResponse__suggestForTicket;
    expect(Array.isArray(suggestions), 'suggestForTicket should return a list').toBe(true);
    expect(suggestions.length, 'should match at least the exact-matched canned response').toBeGreaterThan(0);
    const matched = suggestions.find((s: any) => s.id === canned.id);
    expect(matched, 'the exact-matched canned response should be in suggestions').toBeTruthy();

    // renderTemplate（@BizQuery）——系统变量 + 自定义变量替换
    const renderResp = await callQuery(page, 'ErpCsCannedResponse', 'renderTemplate', {
      cannedResponseId: Number(canned.id),
      ticketId: Number(ticket.id),
    });
    expect(renderResp.errors, 'renderTemplate should not return GraphQL errors').toBeNull();
    expect(renderResp.data, 'renderTemplate should return rendered content').toBeTruthy();
    expect(renderResp.data, 'rendered content should not contain {customer_name} placeholder').not.toContain('{customer_name}');

    // applyCannedResponse（@BizMutation，返回 String 标量，无 selection set；标量 mutation 不能选 'id'）
    const before = canned.usageCount || 0;
    const applyMutationResp = await page.request.post('/graphql', {
      data: {
        query: `mutation{ ErpCsCannedResponse__applyCannedResponse(cannedResponseId:${Number(canned.id)},ticketId:${Number(ticket.id)}) }`,
      },
    });
    const applyJson: any = await applyMutationResp.json();
    expect(applyJson?.errors, `applyCannedResponse should not return GraphQL errors: ${JSON.stringify(applyJson?.errors)}`).toBeFalsy();
    const applyResp = applyJson?.data?.ErpCsCannedResponse__applyCannedResponse;
    expect(applyResp, 'applyCannedResponse should return rendered content').toBeTruthy();
    expect(String(applyResp), 'rendered content should not contain {customer_name} placeholder').not.toContain('{customer_name}');

    // 验证 usageCount 递增（__get 经 verifyState 原语，已支持 selection）
    const after = await verifyState(page, 'ErpCsCannedResponse', canned.id, 'usageCount');
    const usageAfter = after?.usageCount;
    expect(Number(usageAfter), `usageCount should increment from ${before} to ${before + 1}`).toBe(before + 1);

    // 验证 TicketAction NOTE 写入
    const actionResp = await page.request.post('/graphql', {
      data: {
        query: `query($f:Map){ ErpCsTicketAction__findPage(query:{offset:0,limit:10,filter:$f}){ total items{ id actionType content } } }`,
        variables: { f: { $type: 'eq', name: 'ticketId', value: Number(ticket.id) } },
      },
    });
    const actionJson = await actionResp.json();
    const actions = actionJson?.data?.ErpCsTicketAction__findPage?.items || [];
    const noteActions = actions.filter((a: any) => a.actionType === 'NOTE');
    expect(noteActions.length, 'should have at least one NOTE action from applyCannedResponse').toBeGreaterThan(0);

    // 清理
    await deleteByFilter(page, 'ErpCsTicketAction', { $type: 'eq', name: 'ticketId', value: Number(ticket.id) });
    await deleteById(page, 'ErpCsTicket', ticket.id);
    await deleteById(page, 'ErpCsCannedResponse', canned.id);
  });
});
