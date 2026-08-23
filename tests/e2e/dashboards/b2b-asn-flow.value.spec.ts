import { test, expect, loginAndNavigate } from '../fixtures';
import { GraphQLClient } from '../pages';
import { createViaSave, deleteById } from '../business-actions/_helper';

/**
 * b2b asn-flow 页面数据源运行时复核（plan 2026-08-23-0434-2 Phase 2 Add 载体）。
 *
 * 批内序 1 修复 asn-flow.page.yaml:79 的 `$aid:Long` → `$aid:String`（+ adaptor
 * `!== ''` 守卫）——id String 化后 Long 变量收 String 值被 GraphQL 类型系统拒绝，
 * adaptor 静默降级为空流程详情。本 spec 原样重放页面 raw-GraphQL 数据源（含类型化
 * 变量），证明 asn 经 String id get 非空 + 行级 asnId 客户端过滤命中。
 *
 * 自包含 setup：经 __save 直置 RECEIVED 态 ErpB2bAsn + ErpB2bAsnLine 一行
 * （b2b-asn-match-receive.action.spec.ts seedAsn/seedAsnLine 同范式，relatedBillType
 * 用自建 E2E 标记避免触发 PO 匹配语义）。清理：删 AsnLine + Asn（无下游产物）。
 */

test.describe('b2b asn-flow page data source (asn get)', () => {
  test('asn: ErpB2bAsn__get(id:$aid String) loads the asn non-empty + line filterable', async ({ page }) => {
    await loginAndNavigate(page, '/ErpB2bAsn-main');

    const code = `E2E-ASN-FLOW-${Date.now()}`;
    const asn = await createViaSave(
      page, 'ErpB2bAsn',
      {
        code,
        orgId: '2',
        partnerId: '3',
        relatedBillType: 'E2E_FLOW',
        relatedBillCode: code,
        status: 'RECEIVED',
        shipmentDate: '2026-07-10',
        businessDate: '2026-07-10',
      },
      'id code status',
    );
    expect(asn.status, 'precondition status=RECEIVED').toBe('RECEIVED');

    const line = await createViaSave(
      page, 'ErpB2bAsnLine',
      { asnId: asn.id, lineNo: 1, materialId: '1', shippedQty: 10, quantity: 10 },
      'id',
    );

    // asn-flow.page.yaml:79 原样查询（类型化 String 变量 $aid + Int $lim）
    const query = 'query($lim:Int,$aid:String){ asn: ErpB2bAsn__get(id:$aid){ id code status partnerId shipmentDate estimatedArrivalDate trackingNo relatedBillType relatedBillCode } lines: ErpB2bAsnLine__findPage(query:{limit:$lim}){ items{ id asnId lineNo materialId supplierPartNo quantity shippedQty } total } }';
    const json: any = await new GraphQLClient(page).raw(query, { lim: 5000, aid: asn.id });

    expect(json?.errors, 'page query should not return GraphQL errors (adaptor silent-degradation eliminated)').toBeFalsy();
    const a = json?.data?.asn;
    expect(a, 'asn should load non-empty via String id variable $aid').toBeTruthy();
    expect(String(a.id), 'asn id round-trips').toBe(String(asn.id));
    expect(a.code, 'asn code round-trips').toBe(code);
    expect(a.status, 'asn status round-trips').toBe('RECEIVED');

    const lines: any[] = json?.data?.lines?.items || [];
    const mine = lines.find((l: any) => String(l.asnId) === String(asn.id));
    expect(mine, 'asn line present via adaptor client-side asnId filter').toBeTruthy();
    expect(String(mine?.id), 'line id round-trips').toBe(String(line.id));

    await deleteById(page, 'ErpB2bAsnLine', line.id);
    await deleteById(page, 'ErpB2bAsn', asn.id);
  });
});
