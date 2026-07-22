import { test, expect, loginAndNavigate, createViaSave, deleteById, GraphQLClient } from './_helper';

/**
 * finance ErpFinVoucherTemplate__renderTemplate 浏览器层 E2E（F16 P1，plan §Phase 2/6）。
 *
 * 验证 Phase 0 Explore (b) 候选 (c) 的 `@BizMutation renderTemplate(businessType, context)` 经运行中
 * 应用 GraphQL `/graphql` 端点的全栈可达性 + 算术表达式求值正确性。补 TestErpFinVoucherTemplateRender
 * 单测的浏览器层覆盖。
 *
 * 场景：建模板 + 2 行（amountKey 路径 + amountExpression 算术 `DOC_TOTAL * 0.13`），调 renderTemplate，
 * 断言：(1) 行数；(2) debit/credit 按 dcDirection 拆分；(3) 占位符替换；(4) 算术求值（130）。
 *
 * renderTemplate 返回 List<Map>（非实体），故用 GraphQLClient.raw 构造原始 mutation + JSONObject 变量
 * （与 finance-voucher-post.action.spec.ts `ErpFinVoucher__post` scalar Long 处理范式一致）。
 *
 * 清理：删模板行 + 模板（逻辑删除）。renderTemplate 是只读预览，无下游产物。
 */

const JO = 'JSONObject';

async function renderTemplate(
  page: import('@playwright/test').Page,
  businessType: string,
  ctx: Record<string, unknown>,
): Promise<{ rows: any[] | null; errors: any[] | null; json: any }> {
  const gql = new GraphQLClient(page);
  const json: any = await gql.raw(
    `mutation(${'$'}bt:String,${'$'}ctx:${JO}){ ErpFinVoucherTemplate__renderTemplate(businessType:${'$'}bt,context:${'$'}ctx) }`,
    { bt: businessType, ctx },
  );
  const rows = json?.data?.ErpFinVoucherTemplate__renderTemplate ?? null;
  return { rows, errors: json?.errors ?? null, json };
}

function bd(v: any): number {
  return Number(v);
}

test.describe('finance ErpFinVoucherTemplate renderTemplate (F16 P1)', () => {
  test('amountKey lookup + arithmetic expression (DOC_TOTAL * 0.13) produce balanced preview lines', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinVoucherTemplate-main');

    const tag = `F16-ACT-${Date.now()}`;
    // 1. 模板（isActive=true + 宽 valid range，确保 findActiveTemplate 命中本模板）
    const tpl = await createViaSave(
      page, 'ErpFinVoucherTemplate',
      {
        code: tag,
        name: 'F16 action spec 模板',
        businessType: 'PURCHASE_INPUT',
        voucherType: 'TRANSFER',
        isActive: true,
        validFrom: '2020-01-01',
        validTo: '2099-12-31',
      },
      'id',
    );

    try {
      // 2. 行 1：DEBIT + amountKey=DOC_TOTAL + 占位符 subjectCode/memo
      await createViaSave(
        page, 'ErpFinVoucherTemplateLine',
        {
          lineNo: 1,
          subjectCode: '${PARTNER}',
          dcDirection: 'DEBIT',
          amountKey: 'DOC_TOTAL',
          memoTemplate: '采购入库 ${DOC_TOTAL}',
          templateId: Number(tpl.id),
        },
        'id',
      );
      // 3. 行 2：CREDIT + amountExpression 算术 DOC_TOTAL * 0.13
      const line2 = await createViaSave(
        page, 'ErpFinVoucherTemplateLine',
        {
          lineNo: 2,
          subjectCode: '2202',
          dcDirection: 'CREDIT',
          amountExpression: 'DOC_TOTAL * 0.13',
          memoTemplate: '应付 13%',
          templateId: Number(tpl.id),
        },
        'id',
      );

      // 4. 调 renderTemplate
      const { rows, errors } = await renderTemplate(page, 'PURCHASE_INPUT', { DOC_TOTAL: 1000, PARTNER: 'P001' });
      expect(errors, `renderTemplate should not return GraphQL errors: ${JSON.stringify(errors)}`).toBeNull();
      expect(rows, 'renderTemplate should return a non-null list').not.toBeNull();
      expect(rows!.length, 'should return 2 preview lines').toBe(2);

      const r1 = rows![0];
      expect(r1.subjectCode, 'subjectCode placeholder replaced').toBe('P001');
      expect(r1.dcDirection).toBe('DEBIT');
      expect(bd(r1.debitAmount), 'DEBIT line debit = DOC_TOTAL').toBe(1000);
      expect(bd(r1.creditAmount), 'DEBIT line credit = 0').toBe(0);
      expect(r1.memo, 'memo placeholder replaced').toBe('采购入库 1000');

      const r2 = rows![1];
      expect(r2.subjectCode).toBe('2202');
      expect(r2.dcDirection).toBe('CREDIT');
      expect(bd(r2.creditAmount), 'CREDIT line credit = DOC_TOTAL*0.13 = 130').toBe(130);
      expect(bd(r2.debitAmount), 'CREDIT line debit = 0').toBe(0);

      // 5. 清理：删行 + 模板
      await deleteById(page, 'ErpFinVoucherTemplateLine', line2.id);
    } finally {
      await deleteById(page, 'ErpFinVoucherTemplate', tpl.id);
    }
  });
});
