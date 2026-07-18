import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  deleteById,
} from './_helper';
import {
  findVoucherIdByBillCode,
  assertVoucherLines,
  cleanupVoucherByBillCode,
  findFirst,
  eqFilter,
} from '../orchestration/_helper';
import { GraphQLClient } from '../pages';
import type { Page } from '@playwright/test';

/**
 * finance ErpFinCreditFacility 授信利息计提 accrueInterest DIRECT 浏览器层 E2E
 * （plan 2026-07-18-0718-1 Phase 3）。
 *
 * 验证 `ErpFinCreditFacilityBizModel.accrueInterest(creditFacilityId, fromDate, toDate)`
 * 经 GraphQL `/graphql` 的全栈可达性 + 计息公式 + Dr 6603 / Cr 1002 凭证行精确数值
 * （treasury.md §业财过账 CREDIT_FACILITY_INTEREST）。
 *
 * 权威计息逻辑（ErpFinCreditFacilityBizModel:81-111）：
 *   - 守卫：fromDate ≤ toDate；usedAmount > 0；rate > 0（rate 来自 config
 *     `erp-fin.credit-facility-default-interest-rate`，webServer JVM arg 设 0.05=5% 年化）。
 *   - 计息：days = ChronoUnit.DAYS.between(from, to) + 1（闭区间）；
 *     interest = usedAmount × rate × days / 360（HALF_UP scale=4）。
 *   - 委派 CreditFacilityInterestVoucherBuilder.post 构造 PostingEvent
 *     （billHeadCode=CFI-INT-{facilityId}-{fromDate}_{toDate}）+ 调 voucherBiz.post。
 *
 * 幂等键 = (billHeadCode, businessType)（IErpFinVoucherBiz.post 内置 alreadyPosted
 * 按 ErpFinVoucherBillR 反查，同 facility + 同区间二次调用返回 null 无第二张凭证）。
 *
 * 自包含 setup：每测试经 __save 直置 facility 三值（total/used/available），
 * 对齐 2256-1 直 __save 范式。cleanup 经 cleanupVoucherByBillCode 删凭证 + 行 + 回链，
 * 再删 facility，保护 finance 看板/报表数值断言基线。
 *
 * 种子引用：org=2 / facilityType=BANK_ACCEPTANCE_LINE（erp-fin/credit-facility-type）。
 * GraphQL 入参：LocalDate ISO String quoted；返回 Long 标量（对齐 fin-cash-forecast
 * refreshForecast `gql.raw` 长标量返回范式）。
 */
const ORG_ID = 2;
const FACILITY_TYPE = 'BANK_ACCEPTANCE_LINE';
const TOTAL_AMOUNT = 1000;
const RATE = 0.05;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now().toString(36)}-${_seq}`;
}

async function createFacility(
  page: Page,
  overrides: Record<string, unknown> = {},
): Promise<{ id: string; code: string }> {
  const code = uniq('E2E-CFI-FAC');
  return createViaSave(
    page,
    'ErpFinCreditFacility',
    {
      code,
      orgId: ORG_ID,
      facilityType: FACILITY_TYPE,
      totalAmount: TOTAL_AMOUNT,
      usedAmount: 0,
      availableAmount: TOTAL_AMOUNT,
      validFrom: '2026-01-01',
      validTo: '2026-12-31',
      status: 'ACTIVE',
      ...overrides,
    },
    'id code',
  );
}

async function accrueInterest(
  page: Page,
  facilityId: string | number,
  from: string,
  to: string,
): Promise<{ voucherId: number | null; errors: any[] | null; json: any }> {
  const gql = new GraphQLClient(page);
  const json: any = await gql.raw(
    `mutation{ ErpFinCreditFacility__accrueInterest(creditFacilityId:${Number(facilityId)},fromDate:${JSON.stringify(from)},toDate:${JSON.stringify(to)}) }`,
  );
  const raw = json?.data?.ErpFinCreditFacility__accrueInterest;
  const voucherId = raw == null ? null : Number(raw);
  return { voucherId, errors: json?.errors ?? null, json };
}

test.describe('finance ErpFinCreditFacility accrueInterest credit-facility interest posting', () => {
  test('happy path: interest = used × rate × days / 360 + voucher Dr 6603 / Cr 1002 exact value', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinCreditFacility-main');
    const facility = await createFacility(page, {
      usedAmount: 300,
      availableAmount: 700,
    });
    try {
      const from = '2026-07-01';
      const to = '2026-07-31';
      const { voucherId, errors } = await accrueInterest(page, facility.id, from, to);
      expect(errors, 'accrueInterest should not return GraphQL errors').toBeNull();
      expect(voucherId, 'accrueInterest should return voucherId').not.toBeNull();

      // 反查凭证经 billHeadCode（区间级幂等键）
      const expectedBillHeadCode = `CFI-INT-${facility.id}-${from}_${to}`;
      const reverseVoucherId = await findVoucherIdByBillCode(page, expectedBillHeadCode, 'NORMAL');
      expect(reverseVoucherId, 'voucher should be reverse-lookup-able via billHeadCode').toBe(voucherId);

      // 300 × 0.05 × 31 / 360 = 1.2916666... → HALF_UP scale=4 = 1.2917（对齐 Java BigDecimal.divide 精度）
      // voucherDate=2026-07-31 落种子 OPEN 期间 2026-07
      const expectedInterest = 1.2917;
      await assertVoucherLines(page, voucherId, [
        { subjectCode: '6603', dcDirection: 'DEBIT', debitAmount: expectedInterest, creditAmount: 0 },
        { subjectCode: '1002', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: expectedInterest },
      ]);

      // facility 三值在计提后不变（计息非额度占用）
      const after = await findFirst<any>(
        page, 'ErpFinCreditFacility', eqFilter('id', Number(facility.id)),
        'id usedAmount availableAmount totalAmount',
      );
      expect(Number(after.usedAmount), 'facility.usedAmount unchanged=300').toBe(300);
      expect(Number(after.availableAmount), 'facility.availableAmount unchanged=700').toBe(700);
      expect(Number(after.totalAmount), 'facility.totalAmount unchanged=1000').toBe(1000);
    } finally {
      const from = '2026-07-01';
      const to = '2026-07-31';
      const billHeadCode = `CFI-INT-${facility.id}-${from}_${to}`;
      await cleanupVoucherByBillCode(page, billHeadCode);
      await deleteById(page, 'ErpFinCreditFacility', facility.id);
    }
  });

  test('zero usedAmount: accrueInterest returns null + no voucher generated', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinCreditFacility-main');
    const facility = await createFacility(page, {
      usedAmount: 0,
      availableAmount: 1000,
    });
    try {
      const from = '2026-07-01';
      const to = '2026-07-31';
      const { voucherId, errors } = await accrueInterest(page, facility.id, from, to);
      expect(errors, 'accrueInterest(used=0) should not return GraphQL errors').toBeNull();
      expect(voucherId, 'accrueInterest(used=0) returns null (no-op)').toBeNull();

      // 反查无 CFI-INT-{id}- 凭证生成
      const anyVoucher = await findFirst<any>(
        page, 'ErpFinVoucherBillR',
        eqFilter('billCode', `CFI-INT-${facility.id}-${from}_${to}`),
        'id voucherId billCode',
      );
      expect(anyVoucher, 'no voucher bill link should exist for used=0 facility').toBeNull();
    } finally {
      await deleteById(page, 'ErpFinCreditFacility', facility.id);
    }
  });

  test('invalid date range: fromDate > toDate → ERR_CREDIT_FACILITY_INTEREST_INVALID_DATE_RANGE token + no voucher', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinCreditFacility-main');
    const facility = await createFacility(page, {
      usedAmount: 300,
      availableAmount: 700,
    });
    try {
      const { voucherId, errors } = await accrueInterest(page, facility.id, '2026-07-31', '2026-07-01');
      expect(errors, 'accrueInterest(fromDate>toDate) should return GraphQL errors').toBeTruthy();
      // GraphQL 错误响应携带 NopException 中文描述（非 ErrorCode 字符串），「日期」为
      // ERR_CREDIT_FACILITY_INTEREST_INVALID_DATE_RANGE 的语义 token（对齐 2256-1 守卫范式）
      const errMsg = JSON.stringify(errors);
      expect(errMsg, 'error should carry date-range semantic token「日期」').toContain('日期');
      expect(voucherId, 'no voucherId when guard rejects').toBeNull();

      // 反查无凭证生成
      const anyVoucher = await findFirst<any>(
        page, 'ErpFinVoucherBillR',
        eqFilter('billCode', `CFI-INT-${facility.id}-2026-07-31_2026-07-01`),
        'id voucherId billCode',
      );
      expect(anyVoucher, 'no voucher bill link should exist when guard rejects').toBeNull();
    } finally {
      await deleteById(page, 'ErpFinCreditFacility', facility.id);
    }
  });
});
