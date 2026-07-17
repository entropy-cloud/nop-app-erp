import {
  test,
  expect,
  loginAndNavigate,
  input,
  createViaSave,
  callMutation,
  callMutationOk,
  verifyState,
  eqFilter,
  deleteByFilter,
  deleteById,
  GraphQLClient,
} from './_helper';

/**
 * Finance ErpFinReconciliation 核销单生命周期浏览器层 E2E（plan 2026-07-12-0204-2）。
 *
 * 验证 finance 域正式核销单 create→post→reverse 生命周期经 GraphQL /graphql 的全栈可达性 +
 * 辅助账（ErpFinArApItem）openAmount/status 回写 + 双面对账查询（checkDualSideConsistency）可达性 +
 * validateLine 5 守卫负路径 ErrorCode。
 *
 * 权威设计（docs/design/finance/ar-ap-reconciliation.md）：核销独立作用于辅助账 ErpFinArApItem，
 * 不经 xwf、不直接生成 GL 凭证（凭证由收付款审核时生成）。post 经 ReconciliationSettler 回写双方
 * openAmount→0/status=SETTLED；reverse 反向恢复。PartnerBalanceUpdater.refresh 重算 ErpMdPartner
 * receivableBalance/payableBalance（写缓存字段，非独立余额表）。
 *
 * 自包含隔离（关键）：种子 erp_fin_ar_ap_item 1-4 全 SETTLED（不可再核销），5/6 非 invoice-payment 对，
 * 故本 spec 自包含建 OPEN 对——新建 partner（E2E-RECON-PN- 前缀，避开种子 1/3/5）+ 2 行 ArApItem
 * （同 partner+direction，AP_INVOICE + PAYMENT，金额相等）。cleanup 删 partner 使 PartnerBalanceUpdater
 * 写入的余额字段随之消失，不污染 finance 看板（读 GL gl_balance）/ ar-ap-aging（按 sourceBillCode 前缀隔离）。
 *
 * GraphQL 入参（Phase 1 Decision）：create(direction, partnerId, businessDate, lines:List<ReconciliationLineInput>)
 * 的 lines 顶参经 input('[i_app_erp_fin_dao_dto_ReconciliationLineInput]', [...]) 走 typed variable
 * （包名 app.erp.fin.dao.dto + 类名 ReconciliationLineInput，对齐 StockMoveRequest→i_app_erp_inv_biz_StockMoveRequest
 * 命名先例）。标量顶参 direction/partnerId/businessDate 内联。
 *
 * 种子引用：org id=2 / acctSchema ACCT-FIN-01 id=1 / currency CNY id=1 / period id=1（OPEN）。
 */
const RECON_LINE_INPUT_TYPE = '[i_app_erp_fin_dao_dto_ReconciliationLineInput]';
const BDATE = '2026-07-10';
const AMOUNT = 100;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

async function createPartner(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page,
    'ErpMdPartner',
    {
      code: uniq('E2E-RECON-PN'),
      name: `E2E Recon Partner ${tag}`,
      partnerType: 'SUPPLIER',
      status: 'ACTIVE',
    },
    'id',
  );
}

interface ItemOpts {
  direction: string;
  partnerId: number | string;
  sourceBillType: string;
  sourceBillCode: string;
  businessDate: string;
  amount: number;
  status?: string;
  tag: string;
}

async function createItem(page: import('@playwright/test').Page, o: ItemOpts): Promise<{ id: string }> {
  const status = o.status || 'OPEN';
  const settled = status === 'SETTLED' ? o.amount : 0;
  const open = status === 'SETTLED' ? 0 : o.amount;
  return createViaSave(
    page,
    'ErpFinArApItem',
    {
      code: uniq(`E2E-ARAP-${o.tag}`),
      orgId: 2,
      acctSchemaId: 1,
      direction: o.direction,
      partnerId: o.partnerId,
      sourceBillType: o.sourceBillType,
      sourceBillCode: o.sourceBillCode,
      businessDate: o.businessDate,
      currencyId: 1,
      exchangeRate: 1,
      amountSource: o.amount,
      amountFunctional: o.amount,
      settledAmountSource: settled,
      settledAmountFunctional: settled,
      openAmountSource: open,
      openAmountFunctional: open,
      status,
      periodId: 1,
    },
    'id',
  );
}

interface ReconLineIn {
  paymentItemId: string | number;
  invoiceItemId: string | number;
  settledAmountSource: number;
  settledAmountFunctional: number;
}

async function createRecon(
  page: import('@playwright/test').Page,
  direction: string,
  partnerId: string | number,
  businessDate: string,
  lines: ReconLineIn[],
): Promise<{ id: string; docStatus: string }> {
  return callMutationOk(
    page,
    'ErpFinReconciliation',
    'create',
    {
      direction,
      partnerId,
      businessDate,
      lines: input(RECON_LINE_INPUT_TYPE, lines),
    },
    'id docStatus',
  );
}

async function cleanupRecon(
  page: import('@playwright/test').Page,
  partnerId: string | number | null,
  reconId: string | number | null,
  itemIds: Array<string | number>,
): Promise<void> {
  if (reconId != null) {
    await deleteByFilter(page, 'ErpFinReconciliationLine', eqFilter('reconciliationId', Number(reconId)));
    await deleteById(page, 'ErpFinReconciliation', reconId);
  }
  for (const id of itemIds) {
    await deleteById(page, 'ErpFinArApItem', id);
  }
  if (partnerId != null) {
    await deleteById(page, 'ErpMdPartner', partnerId);
  }
}

function buildPair(partnerId: string | number, direction: string, tag: string, invoiceBusinessDate: string) {
  const invoiceType = direction === 'PAYABLE' ? 'AP_INVOICE' : 'AR_INVOICE';
  return {
    invoiceBillType: invoiceType,
    invoiceBillCode: uniq(`E2E-${tag}-INV`),
    paymentBillCode: uniq(`E2E-${tag}-PAY`),
    invoiceBusinessDate,
  };
}

test.describe('Finance ErpFinReconciliation lifecycle browser-layer E2E', () => {
  test('happy path: create(DRAFT) → post(POSTED + both items SETTLED) → reverse(REVERSED + both items OPEN)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinReconciliation-main');

    const partner = await createPartner(page, 'happy');
    const pair = buildPair(partner.id, 'PAYABLE', 'HAPPY', '2026-07-05');
    const invoice = await createItem(page, {
      direction: 'PAYABLE', partnerId: partner.id, sourceBillType: pair.invoiceBillType,
      sourceBillCode: pair.invoiceBillCode, businessDate: pair.invoiceBusinessDate, amount: AMOUNT, tag: 'HAPPY-INV',
    });
    const payment = await createItem(page, {
      direction: 'PAYABLE', partnerId: partner.id, sourceBillType: 'PAYMENT',
      sourceBillCode: pair.paymentBillCode, businessDate: BDATE, amount: AMOUNT, tag: 'HAPPY-PAY',
    });

    try {
      const created = await createRecon(
        page, 'PAYABLE', partner.id, BDATE,
        [{ paymentItemId: payment.id, invoiceItemId: invoice.id, settledAmountSource: AMOUNT, settledAmountFunctional: AMOUNT }],
      );
      expect(created.id, 'create should return reconciliation id').toBeTruthy();
      expect(created.docStatus, 'create should produce DRAFT').toBe('DRAFT');

      // post → POSTED + 双方辅助账 openAmount→0 / status=SETTLED
      const posted = await callMutationOk(
        page, 'ErpFinReconciliation', 'post', { reconciliationId: created.id }, 'id docStatus',
      );
      expect(posted.docStatus, 'post should transition DRAFT → POSTED').toBe('POSTED');

      const payAfter = await verifyState(page, 'ErpFinArApItem', payment.id, 'openAmountFunctional status');
      expect(Number(payAfter.openAmountFunctional), 'payment openAmount should be 0 after post').toBe(0);
      expect(payAfter.status, 'payment status should be SETTLED after post').toBe('SETTLED');
      const invAfter = await verifyState(page, 'ErpFinArApItem', invoice.id, 'openAmountFunctional status');
      expect(Number(invAfter.openAmountFunctional), 'invoice openAmount should be 0 after post').toBe(0);
      expect(invAfter.status, 'invoice status should be SETTLED after post').toBe('SETTLED');

      // reverse → REVERSED + 双方辅助账 openAmount 恢复 / status=OPEN
      const reversed = await callMutationOk(
        page, 'ErpFinReconciliation', 'reverse', { reconciliationId: created.id }, 'id docStatus',
      );
      expect(reversed.docStatus, 'reverse should transition POSTED → REVERSED').toBe('REVERSED');

      const payRev = await verifyState(page, 'ErpFinArApItem', payment.id, 'openAmountFunctional status');
      expect(Number(payRev.openAmountFunctional), 'payment openAmount should restore after reverse').toBe(AMOUNT);
      expect(payRev.status, 'payment status should restore OPEN after reverse').toBe('OPEN');
      const invRev = await verifyState(page, 'ErpFinArApItem', invoice.id, 'openAmountFunctional status');
      expect(Number(invRev.openAmountFunctional), 'invoice openAmount should restore after reverse').toBe(AMOUNT);
      expect(invRev.status, 'invoice status should restore OPEN after reverse').toBe('OPEN');

      const finalHead = await verifyState(page, 'ErpFinReconciliation', created.id, 'docStatus');
      expect(finalHead.docStatus, '__get should confirm REVERSED').toBe('REVERSED');
    } finally {
      await cleanupRecon(page, partner.id, null, [payment.id, invoice.id]);
    }
  });

  test('checkDualSideConsistency @BizQuery returns non-empty DualSideDiffReport structure', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinReconciliation-main');

    const partner = await createPartner(page, 'dual');
    const pair = buildPair(partner.id, 'PAYABLE', 'DUAL', '2026-07-05');
    const invoice = await createItem(page, {
      direction: 'PAYABLE', partnerId: partner.id, sourceBillType: pair.invoiceBillType,
      sourceBillCode: pair.invoiceBillCode, businessDate: pair.invoiceBusinessDate, amount: AMOUNT, tag: 'DUAL-INV',
    });
    const payment = await createItem(page, {
      direction: 'PAYABLE', partnerId: partner.id, sourceBillType: 'PAYMENT',
      sourceBillCode: pair.paymentBillCode, businessDate: BDATE, amount: AMOUNT, tag: 'DUAL-PAY',
    });

    try {
      // checkDualSideConsistency 返回 DualSideDiffReport（复杂对象，需 selection set，非 callQuery 原语可表达）。
      // 自包含 setup 仅建 finance 侧 OPEN 发票项（settled=0）+ 无域侧发票 → diff=0 → consistent=true。
      // 本用例仅断言查询可达 + 报告结构非空（direction/partnerId/consistent/rows 字段可观测），不断言 consistent 取值。
      const json: any = await new GraphQLClient(page).raw(
        `query{ ErpFinReconciliation__checkDualSideConsistency(direction:"PAYABLE",partnerId:${partner.id}){ direction partnerId consistent rows{ partnerId financeSettled domainSettled diff status } } }`,
      );
      expect(json?.errors, `checkDualSideConsistency should not return GraphQL errors: ${JSON.stringify(json?.errors)}`).toBeFalsy();
      const report = json?.data?.ErpFinReconciliation__checkDualSideConsistency;
      expect(report, 'DualSideDiffReport should be returned').toBeTruthy();
      expect(report.direction, 'report.direction echoes query').toBe('PAYABLE');
      expect(Number(report.partnerId), 'report.partnerId echoes query').toBe(Number(partner.id));
      expect(typeof report.consistent, 'report.consistent is a boolean').toBe('boolean');
      expect(Array.isArray(report.rows), 'report.rows is an array').toBe(true);
    } finally {
      await cleanupRecon(page, partner.id, null, [payment.id, invoice.id]);
    }
  });

  test('negative: direction mismatch (invoice direction != head direction) → ERR_RECONCILIATION_DIRECTION_MISMATCH', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinReconciliation-main');

    const partner = await createPartner(page, 'dir');
    // 发票项方向 RECEIVABLE，核销单方向 PAYABLE → validateLine direction 守卫
    const invoice = await createItem(page, {
      direction: 'RECEIVABLE', partnerId: partner.id, sourceBillType: 'AR_INVOICE',
      sourceBillCode: uniq('E2E-DIR-INV'), businessDate: '2026-07-05', amount: AMOUNT, tag: 'DIR-INV',
    });
    const payment = await createItem(page, {
      direction: 'PAYABLE', partnerId: partner.id, sourceBillType: 'PAYMENT',
      sourceBillCode: uniq('E2E-DIR-PAY'), businessDate: BDATE, amount: AMOUNT, tag: 'DIR-PAY',
    });

    try {
      const created = await createRecon(
        page, 'PAYABLE', partner.id, BDATE,
        [{ paymentItemId: payment.id, invoiceItemId: invoice.id, settledAmountSource: AMOUNT, settledAmountFunctional: AMOUNT }],
      );

      const rej = await callMutation(page, 'ErpFinReconciliation', 'post', { reconciliationId: created.id }, 'id docStatus');
      expect(rej.errors, 'post with direction mismatch should be rejected').toBeTruthy();
      expect(JSON.stringify(rej.errors), 'reject should carry direction-mismatch message').toContain('方向不一致');

      const head = await verifyState(page, 'ErpFinReconciliation', created.id, 'docStatus');
      expect(head.docStatus, 'rejected post should leave head DRAFT').toBe('DRAFT');
    } finally {
      await cleanupRecon(page, partner.id, null, [payment.id, invoice.id]);
    }
  });

  test('negative: partner mismatch between payment and invoice items → ERR_RECONCILIATION_PARTNER_MISMATCH', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinReconciliation-main');

    const partnerA = await createPartner(page, 'pmA');
    const partnerB = await createPartner(page, 'pmB');
    const invoice = await createItem(page, {
      direction: 'PAYABLE', partnerId: partnerB.id, sourceBillType: 'AP_INVOICE',
      sourceBillCode: uniq('E2E-PM-INV'), businessDate: '2026-07-05', amount: AMOUNT, tag: 'PM-INV',
    });
    const payment = await createItem(page, {
      direction: 'PAYABLE', partnerId: partnerA.id, sourceBillType: 'PAYMENT',
      sourceBillCode: uniq('E2E-PM-PAY'), businessDate: BDATE, amount: AMOUNT, tag: 'PM-PAY',
    });

    try {
      const created = await createRecon(
        page, 'PAYABLE', partnerA.id, BDATE,
        [{ paymentItemId: payment.id, invoiceItemId: invoice.id, settledAmountSource: AMOUNT, settledAmountFunctional: AMOUNT }],
      );

      const rej = await callMutation(page, 'ErpFinReconciliation', 'post', { reconciliationId: created.id }, 'id docStatus');
      expect(rej.errors, 'post with partner mismatch should be rejected').toBeTruthy();
      expect(JSON.stringify(rej.errors), 'reject should carry partner-mismatch message').toContain('往来单位不一致');

      const head = await verifyState(page, 'ErpFinReconciliation', created.id, 'docStatus');
      expect(head.docStatus, 'rejected post should leave head DRAFT').toBe('DRAFT');
    } finally {
      await cleanupRecon(page, null, null, [payment.id, invoice.id]);
      await deleteById(page, 'ErpMdPartner', partnerA.id);
      await deleteById(page, 'ErpMdPartner', partnerB.id);
    }
  });

  test('negative: settle already-SETTLED item → ERR_RECONCILIATION_ITEM_NOT_OPEN', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinReconciliation-main');

    const partner = await createPartner(page, 'settled');
    const invoice = await createItem(page, {
      direction: 'PAYABLE', partnerId: partner.id, sourceBillType: 'AP_INVOICE',
      sourceBillCode: uniq('E2E-ST-INV'), businessDate: '2026-07-05', amount: AMOUNT, status: 'SETTLED', tag: 'ST-INV',
    });
    const payment = await createItem(page, {
      direction: 'PAYABLE', partnerId: partner.id, sourceBillType: 'PAYMENT',
      sourceBillCode: uniq('E2E-ST-PAY'), businessDate: BDATE, amount: AMOUNT, tag: 'ST-PAY',
    });

    try {
      const created = await createRecon(
        page, 'PAYABLE', partner.id, BDATE,
        [{ paymentItemId: payment.id, invoiceItemId: invoice.id, settledAmountSource: AMOUNT, settledAmountFunctional: AMOUNT }],
      );

      const rej = await callMutation(page, 'ErpFinReconciliation', 'post', { reconciliationId: created.id }, 'id docStatus');
      expect(rej.errors, 'post on SETTLED item should be rejected').toBeTruthy();
      expect(JSON.stringify(rej.errors), 'reject should carry item-not-open message').toContain('已结清');

      const head = await verifyState(page, 'ErpFinReconciliation', created.id, 'docStatus');
      expect(head.docStatus, 'rejected post should leave head DRAFT').toBe('DRAFT');
    } finally {
      await cleanupRecon(page, partner.id, null, [payment.id, invoice.id]);
    }
  });

  test('negative: settle amount over open → ERR_RECONCILIATION_OVER_AMOUNT', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinReconciliation-main');

    const partner = await createPartner(page, 'over');
    const invoice = await createItem(page, {
      direction: 'PAYABLE', partnerId: partner.id, sourceBillType: 'AP_INVOICE',
      sourceBillCode: uniq('E2E-OV-INV'), businessDate: '2026-07-05', amount: AMOUNT, tag: 'OV-INV',
    });
    const payment = await createItem(page, {
      direction: 'PAYABLE', partnerId: partner.id, sourceBillType: 'PAYMENT',
      sourceBillCode: uniq('E2E-OV-PAY'), businessDate: BDATE, amount: AMOUNT, tag: 'OV-PAY',
    });

    try {
      const overAmount = AMOUNT * 2;
      const created = await createRecon(
        page, 'PAYABLE', partner.id, BDATE,
        [{ paymentItemId: payment.id, invoiceItemId: invoice.id, settledAmountSource: overAmount, settledAmountFunctional: overAmount }],
      );

      const rej = await callMutation(page, 'ErpFinReconciliation', 'post', { reconciliationId: created.id }, 'id docStatus');
      expect(rej.errors, 'post over open amount should be rejected').toBeTruthy();
      expect(JSON.stringify(rej.errors), 'reject should carry over-amount message').toContain('超过未核销余额');

      const head = await verifyState(page, 'ErpFinReconciliation', created.id, 'docStatus');
      expect(head.docStatus, 'rejected post should leave head DRAFT').toBe('DRAFT');
    } finally {
      await cleanupRecon(page, partner.id, null, [payment.id, invoice.id]);
    }
  });

  test('negative: business date before invoice date → ERR_RECONCILIATION_DATE_BEFORE_INVOICE', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinReconciliation-main');

    const partner = await createPartner(page, 'date');
    const invoice = await createItem(page, {
      direction: 'PAYABLE', partnerId: partner.id, sourceBillType: 'AP_INVOICE',
      sourceBillCode: uniq('E2E-DT-INV'), businessDate: '2026-07-15', amount: AMOUNT, tag: 'DT-INV',
    });
    const payment = await createItem(page, {
      direction: 'PAYABLE', partnerId: partner.id, sourceBillType: 'PAYMENT',
      sourceBillCode: uniq('E2E-DT-PAY'), businessDate: BDATE, amount: AMOUNT, tag: 'DT-PAY',
    });

    try {
      // 核销单 businessDate(2026-07-10) 早于发票业务日期(2026-07-15) → date 守卫
      const created = await createRecon(
        page, 'PAYABLE', partner.id, BDATE,
        [{ paymentItemId: payment.id, invoiceItemId: invoice.id, settledAmountSource: AMOUNT, settledAmountFunctional: AMOUNT }],
      );

      const rej = await callMutation(page, 'ErpFinReconciliation', 'post', { reconciliationId: created.id }, 'id docStatus');
      expect(rej.errors, 'post with date before invoice should be rejected').toBeTruthy();
      expect(JSON.stringify(rej.errors), 'reject should carry date-before-invoice message').toContain('早于发票业务日期');

      const head = await verifyState(page, 'ErpFinReconciliation', created.id, 'docStatus');
      expect(head.docStatus, 'rejected post should leave head DRAFT').toBe('DRAFT');
    } finally {
      await cleanupRecon(page, partner.id, null, [payment.id, invoice.id]);
    }
  });
});
