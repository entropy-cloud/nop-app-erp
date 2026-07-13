import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, deleteById } from './_helper';

/**
 * b2b ErpB2bEdiDoc EDI 信封状态机业务动作浏览器层 E2E（plan 2026-07-14-0508-1 Phase 2）。
 *
 * 验证 EDI 信封 DIRECT @BizMutation 状态机经 GraphQL /graphql 的全栈可达性 + state 翻转 +
 * 重试 retryCount 递增。
 *
 * 权威状态机（ErpB2bEdiDocBizModel，对齐 docs/design/b2b/edi-formats.md §七）：
 *   出站：createOutbound→TO_SEND --markSent--> SENT --markAcknowledged--> ACKNOWLEDGED（终态）
 *        失败 markError-->ERROR（任意态可达）--retry--> TO_SEND + retryCount++
 *        取消 cancel-->CANCELLED（终态，允许 TO_SEND/SENT/ERROR 态）
 *   入站：createInbound-->RECEIVED --archive--> ARCHIVED（终态）
 *
 * 字段名核实（B1 修复）：ErpB2bEdiDoc 状态字段为 `state`（ORM column name="state"，
 *   app-erp-b2b.orm.xml:173，BizModel 全文 getState/setState），非 `status`。verifyState
 *   selection 一律用 `state`，否则 `__get` 字段不解析返回 null 致断言全断。
 *
 * Phase 2 Explore Decision（createOutbound provider 可达性裁定）：
 *   - createOutbound happy-path 需：(1) 注册的 IErpB2bEdiProvider（UblInvoiceEdiProvider for
 *     AR_INVOICE ✓）；(2) ErpB2bEdiFormat 记录 code="UBL_INVOICE"；(3) ErpSalInvoice 跨域
 *     存在（generatePayload 按 code 查销售发票，缺失抛 ERR_B2B_EDI_PARSE_FAILED）。
 *   - 跨域 ErpSalInvoice 自包含 setup 耦合度高（违反单域隔离原则），故 createOutbound 入口
 *     **降级为 watch-only residual**（不在 spec 测）：经 `__save` 预置 TO_SEND 态
 *     ErpB2bEdiDoc 直接测 markSent/markAcknowledged/cancel/markError/retry 状态迁移（对齐
 *     0215-2 contract 经 `__save` 置入口范式）。
 *   - 入站 createInbound 无跨域依赖（formatCode 可不存在），落 spec 测 RECEIVED→archive 正路径。
 *
 * 自包含 setup：经 __save 直接置 state=TO_SEND/RECEIVED 入口（绕过 createOutbound 跨域依赖）。
 *   formatId 列非 mandatory，留 null；唯一键 UK_EDI_DOC_FORMAT_BILL（formatId+relatedBillType+
 *   relatedBillCode）因 formatId=null 不触发；UK_B2B_EDI_DOC_CODE_ORG（code+orgId）经唯一 code 保证。
 * 清理：删 ErpB2bEdiDoc（无下游产物；ErpB2bEdiLog 经 ediDocId 残留但不阻断——逻辑删除可选）。
 */

async function seedEdiDoc(page: import('@playwright/test').Page, tag: string, state: string): Promise<{ id: string; state: string; retryCount: number | string }> {
  const code = `E2E-B2B-EDI-${tag}-${Date.now()}`;
  return createViaSave(
    page, 'ErpB2bEdiDoc',
    {
      code,
      relatedBillType: `E2E_RBT_${tag}`,
      relatedBillCode: code,
      state,
      blockingLevel: state === 'ERROR' ? 'ERROR' : 'INFO',
      retryCount: 0,
    },
    'id state retryCount',
  );
}

test.describe('b2b ErpB2bEdiDoc EDI envelope state machine', () => {
  test('outbound forward chain: save(TO_SEND) → markSent(SENT) → markAcknowledged(ACKNOWLEDGED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpB2bEdiDoc-main');

    const doc = await seedEdiDoc(page, 'out-fwd', 'TO_SEND');
    expect(doc.state, 'precondition state=TO_SEND').toBe('TO_SEND');

    // markSent: TO_SEND → SENT
    await callMutationOk(page, 'ErpB2bEdiDoc', 'markSent', { ediDocId: doc.id }, 'id');
    let v = await verifyState(page, 'ErpB2bEdiDoc', doc.id, 'state');
    expect(v.state, 'after markSent state=SENT').toBe('SENT');

    // markAcknowledged: SENT → ACKNOWLEDGED
    await callMutationOk(page, 'ErpB2bEdiDoc', 'markAcknowledged', { ediDocId: doc.id }, 'id');
    v = await verifyState(page, 'ErpB2bEdiDoc', doc.id, 'state');
    expect(v.state, 'after markAcknowledged state=ACKNOWLEDGED').toBe('ACKNOWLEDGED');

    await deleteById(page, 'ErpB2bEdiDoc', doc.id);
  });

  test('failed retry path: save(TO_SEND) → markError(ERROR) → retry(TO_SEND + retryCount++)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpB2bEdiDoc-main');

    const doc = await seedEdiDoc(page, 'retry', 'TO_SEND');
    const initialRetryCount = Number(doc.retryCount);

    // markError: 任意态 → ERROR
    await callMutationOk(page, 'ErpB2bEdiDoc', 'markError', { ediDocId: doc.id, error: 'E2E simulated gateway timeout' }, 'id');
    let v = await verifyState(page, 'ErpB2bEdiDoc', doc.id, 'state error');
    expect(v.state, 'after markError state=ERROR').toBe('ERROR');

    // retry: ERROR → TO_SEND + retryCount++
    await callMutationOk(page, 'ErpB2bEdiDoc', 'retry', { ediDocId: doc.id }, 'id');
    v = await verifyState(page, 'ErpB2bEdiDoc', doc.id, 'state retryCount');
    expect(v.state, 'after retry state=TO_SEND').toBe('TO_SEND');
    expect(Number(v.retryCount), 'retryCount should increment').toBe(initialRetryCount + 1);

    await deleteById(page, 'ErpB2bEdiDoc', doc.id);
  });

  test('cancel path: save(TO_SEND) → cancel(CANCELLED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpB2bEdiDoc-main');

    const doc = await seedEdiDoc(page, 'cancel', 'TO_SEND');

    // cancel: TO_SEND → CANCELLED（cancel 允许 TO_SEND/SENT/ERROR 态）
    await callMutationOk(page, 'ErpB2bEdiDoc', 'cancel', { ediDocId: doc.id }, 'id');
    const v = await verifyState(page, 'ErpB2bEdiDoc', doc.id, 'state');
    expect(v.state, 'after cancel state=CANCELLED').toBe('CANCELLED');

    await deleteById(page, 'ErpB2bEdiDoc', doc.id);
  });

  test('inbound path: createInbound(RECEIVED) → archive(ARCHIVED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpB2bEdiDoc-main');

    const billCode = `E2E-INBOUND-${Date.now()}`;
    // createInbound: 建 + state=RECEIVED（formatCode=null 不查 format，跳过 checkDuplicate）
    const created = await callMutationOk(
      page, 'ErpB2bEdiDoc', 'createInbound',
      {
        relatedBillType: 'INBOUND_E2E',
        relatedBillCode: billCode,
        rawPayload: '<?xml version="1.0"?><test>E2E inbound payload</test>',
        formatCode: `NONEXISTENT-${Date.now()}`,
      },
      'id state',
    );
    expect(created.state, 'createInbound should produce state=RECEIVED').toBe('RECEIVED');

    // archive: RECEIVED → ARCHIVED
    await callMutationOk(page, 'ErpB2bEdiDoc', 'archive', { ediDocId: created.id }, 'id');
    const v = await verifyState(page, 'ErpB2bEdiDoc', created.id, 'state');
    expect(v.state, 'after archive state=ARCHIVED').toBe('ARCHIVED');

    await deleteById(page, 'ErpB2bEdiDoc', created.id);
  });

  test('illegal transition guards: ACKNOWLEDGED→markSent rejected; CANCELLED→retry rejected (ERR_B2B_EDI_DOC_ILLEGAL_TRANSITION)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpB2bEdiDoc-main');

    // ACKNOWLEDGED → markSent（markSent 须 TO_SEND）
    const ack = await seedEdiDoc(page, 'gd-ack', 'TO_SEND');
    await callMutationOk(page, 'ErpB2bEdiDoc', 'markSent', { ediDocId: ack.id }, 'id');
    await callMutationOk(page, 'ErpB2bEdiDoc', 'markAcknowledged', { ediDocId: ack.id }, 'id');
    let v = await verifyState(page, 'ErpB2bEdiDoc', ack.id, 'state');
    expect(v.state, 'precondition state=ACKNOWLEDGED').toBe('ACKNOWLEDGED');

    const rej1 = await callMutation(page, 'ErpB2bEdiDoc', 'markSent', { ediDocId: ack.id }, 'id');
    expect(rej1.errors, 'markSent from ACKNOWLEDGED should be rejected').toBeTruthy();
    expect(JSON.stringify(rej1.errors), 'reject should carry illegal-transition token').toContain('不允许执行该操作');
    v = await verifyState(page, 'ErpB2bEdiDoc', ack.id, 'state');
    expect(v.state, 'failed markSent should not change state').toBe('ACKNOWLEDGED');

    // CANCELLED → retry（retry 须 ERROR）
    const cancelled = await seedEdiDoc(page, 'gd-rc', 'TO_SEND');
    await callMutationOk(page, 'ErpB2bEdiDoc', 'cancel', { ediDocId: cancelled.id }, 'id');
    v = await verifyState(page, 'ErpB2bEdiDoc', cancelled.id, 'state');
    expect(v.state, 'precondition state=CANCELLED').toBe('CANCELLED');

    const rej2 = await callMutation(page, 'ErpB2bEdiDoc', 'retry', { ediDocId: cancelled.id }, 'id');
    expect(rej2.errors, 'retry from CANCELLED should be rejected').toBeTruthy();
    expect(JSON.stringify(rej2.errors), 'reject should carry illegal-transition token').toContain('不允许执行该操作');
    v = await verifyState(page, 'ErpB2bEdiDoc', cancelled.id, 'state');
    expect(v.state, 'failed retry should not change state').toBe('CANCELLED');

    await deleteById(page, 'ErpB2bEdiDoc', ack.id);
    await deleteById(page, 'ErpB2bEdiDoc', cancelled.id);
  });
});
