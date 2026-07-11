import { test, expect, loginAndNavigate } from './_helper';
import {
  eqFilter, deleteByFilter,
  findItems, cleanupVoucherByBillCode, assertVoucherLines, SEED,
} from '../orchestration/_helper';
import type { Page } from '@playwright/test';

/**
 * finance voucher 手工 post E2E（plan 2026-07-11-2329-2 Phase 2）。
 *
 * 验证 `IErpFinVoucherBiz.post(PostingEvent, IServiceContext)` 业财过账工厂+入口的浏览器层全栈可达性。
 * 补齐 0335-2 Deferred「finance voucher 手工 post」——`reverse` 已在 2004-2 覆盖，本 spec 聚焦正向 post。
 *
 * Explore 探针裁决（plan §Phase 2 Explore）：
 *   (a) PostingEvent 经 Nop GraphQL 暴露为 input 类型 `PostingEventInput`，`businessType` 枚举以 String
 *       scalar 传递（quoted `"LANDED_COST"`，对齐 2004-2 reverse 中 businessType 裁决），`billData` Map +
 *       嵌套 List/Map 序列化正常，BigDecimal 金额以 JS number 传递。
 *   (b) LANDED_COST(490) 的 LandedCostAcctDocProvider 接受最简 billData（无库存/订单状态依赖）：
 *       ALLOCATIONS（借方分摊行 {materialId,warehouseId,allocatedAmount}）+ COST_ELEMENTS（贷方费用行
 *       {costElement,amount,apPartnerId}）。凭证行：Dr 1401(库存) / Cr 2202(应付)，科目在种子 COA 可达。
 *   (c) `post` 返回 Long scalar（非实体），不经选择集——直接构造 `mutation($e:PostingEventInput){ ErpFinVoucher__post(event:$e) }`
 *       原始查询（与 2004-2 `reverse` scalar Long 处理范式一致），不与 `_helper.ts callMutation`（总包选择集）兼容。
 *
 * 幂等语义：源单据已过账（按业财回链 ErpFinVoucherBillR 反查 NORMAL+POSTED+未冲销凭证）返回 null。
 *
 * 注意：ErpFinVoucher 是过账产物本身，`posted` 字段是源单据标记不是凭证标记——凭证 docStatus=POSTED 才是
 * 凭证级过账标记。本 spec 断言凭证头字段（code/voucherType/postingType/docStatus/totalDebit/totalCredit）+
 * 业财回链（billHeadCode/businessType 经 ErpFinVoucherBillR）+ 行明细（subjectCode/dcDirection/金额），
 * 不断言 `posted=true`（凭证无 posted 列语义，对齐 plan S3 裁决）。
 *
 * 清理：复用 orchestration/_helper.ts `cleanupVoucherByBillCode`（按 billCode 关联删凭证行+凭证+回链）。
 * post 仅写 voucher/voucher_line/voucher_bill_r（不写 gl_balance），故清理此三表即清除过账凭证污染
 * （finance 看板读 gl_balance 不受影响）。
 */

const BDATE = '2026-07-09'; // 落在种子 OPEN 期间 2026-07（id=1，resolveOpenPeriod 需要）
const POSTING_EVENT_INPUT = 'PostingEventInput';

/**
 * 经 `ErpFinVoucher__post(event:$e)` 创建过账凭证。返回新凭证 id；幂等命中（源单已过账）返回 null。
 *
 * `post` 返回 Long scalar，故直接构造原始 mutation（无选择集），event 经 GraphQL variable + 显式 input 类型。
 */
async function postVoucher(
  page: Page,
  event: Record<string, unknown>,
): Promise<{ voucherId: number | null; errors: any[] | null; json: any }> {
  const resp = await page.request.post('/graphql', {
    data: {
      query: `mutation($e:${POSTING_EVENT_INPUT}){ ErpFinVoucher__post(event:$e) }`,
      variables: { e: event },
    },
  });
  const json: any = await resp.json();
  const raw = json?.data?.ErpFinVoucher__post;
  const voucherId = raw == null ? null : Number(raw);
  return { voucherId, errors: json?.errors ?? null, json };
}

/** 构造最小 LANDED_COST PostingEvent（billHeadCode 为幂等/红冲键，须唯一）。 */
function buildLandedCostEvent(billHeadCode: string): Record<string, unknown> {
  return {
    businessType: 'LANDED_COST',
    billHeadCode,
    orgId: SEED.ORG,
    acctSchemaId: SEED.ACCT_SCHEMA,
    currencyId: SEED.CURRENCY,
    exchangeRate: 1,
    voucherDate: BDATE,
    billData: {
      // 借方：每入库行分摊金额 → 存货(1401)
      ALLOCATIONS: [
        { materialId: SEED.MAT_1, warehouseId: SEED.WH_RAW, allocatedAmount: 100 },
      ],
      // 贷方：每费用要素 → 应付账款(2202)
      COST_ELEMENTS: [
        { costElement: 'FREIGHT', amount: 100, apPartnerId: SEED.SUPPLIER },
      ],
    },
  };
}

test.describe('finance ErpFinVoucher manual post (LANDED_COST)', () => {
  test('happy path: post creates voucher with head + lines, idempotent re-post returns null', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinVoucher-main');

    const billHeadCode = `E2E-VCH-POST-happy-${Date.now()}`;
    const event = buildLandedCostEvent(billHeadCode);

    // 正路径：post → 凭证创建
    const { voucherId, errors } = await postVoucher(page, event);
    expect(errors, `ErpFinVoucher__post should not return GraphQL errors: ${JSON.stringify(errors)}`).toBeNull();
    expect(voucherId, 'post should return non-null voucherId for new billHeadCode').not.toBeNull();

    try {
      // 凭证头断言（经 __get 权威查库）
      const headResp = await page.request.post('/graphql', {
        data: { query: `{ ErpFinVoucher__get(id:${voucherId}){ id code voucherType postingType voucherDate totalDebit totalCredit docStatus isReversed } }` },
      });
      const headJson: any = await headResp.json();
      const head = headJson?.data?.ErpFinVoucher__get;
      expect(head, 'voucher head should exist').not.toBeNull();
      expect(head.voucherType, 'voucherType defaults to TRANSFER (AcctDocContext null → DEFAULT)').toBe('TRANSFER');
      expect(head.postingType, 'postingType should be NORMAL (正向过账)').toBe('NORMAL');
      expect(head.docStatus, 'docStatus should be POSTED (凭证级过账标记)').toBe('POSTED');
      expect(head.isReversed, 'isReversed should be false for fresh NORMAL voucher').toBe(false);
      expect(Number(head.totalDebit), 'totalDebit = 100 (LANDED_COST 借存货)').toBe(100);
      expect(Number(head.totalCredit), 'totalCredit = 100 (LANDED_COST 贷应付)').toBe(100);

      // 业财回链断言（billHeadCode + businessType 经 ErpFinVoucherBillR）
      const links = await findItems<any>(page, 'ErpFinVoucherBillR', eqFilter('billCode', billHeadCode), 'voucherId billCode billType businessType');
      expect(links.length, 'should have exactly one billR link for new billHeadCode').toBe(1);
      expect(Number(links[0].voucherId), 'billR.voucherId should match returned voucherId').toBe(voucherId);
      expect(links[0].billCode, 'billR.billCode should match billHeadCode').toBe(billHeadCode);
      expect(links[0].businessType, 'billR.businessType should be LANDED_COST').toBe('LANDED_COST');
      expect(links[0].billType, 'billR.billType should be LANDED_COST').toBe('LANDED_COST');

      // 凭证行精确数值断言（复用 orchestration/_helper.ts assertVoucherLines）
      // LANDED_COST 凭证行（LandedCostAcctDocProvider 派生）：
      //   Dr 1401(库存商品) = 100  /  Cr 2202(应付账款) = 100
      await assertVoucherLines(page, voucherId, [
        { subjectCode: '1401', dcDirection: 'DEBIT', debitAmount: 100, creditAmount: 0 },
        { subjectCode: '2202', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: 100 },
      ]);

      // 幂等路径：重复 post 同 billHeadCode → 返回 null（源单已过账，alreadyPosted 命中）
      const { voucherId: idempotentId, errors: idempotentErrors } = await postVoucher(page, event);
      expect(idempotentErrors, 'idempotent re-post should not return GraphQL errors').toBeNull();
      expect(idempotentId, 'idempotent re-post should return null (already posted)').toBeNull();
    } finally {
      // 清理：按 billHeadCode 删凭证行+凭证+回链（post 仅写 voucher/voucher_line/voucher_bill_r，不写 gl_balance）
      await cleanupVoucherByBillCode(page, billHeadCode);
    }

    // 清理完整性核实：凭证 + 回链均无残留
    const afterLinks = await findItems<any>(page, 'ErpFinVoucherBillR', eqFilter('billCode', billHeadCode), 'voucherId');
    expect(afterLinks.length, 'billR should be removed after cleanup').toBe(0);
    const afterLines = await findItems<any>(page, 'ErpFinVoucherLine', eqFilter('voucherId', voucherId!), 'id');
    expect(afterLines.length, 'voucher lines should be removed after cleanup').toBe(0);
  });
});
