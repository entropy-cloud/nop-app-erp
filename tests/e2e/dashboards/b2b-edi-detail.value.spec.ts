import { test, expect, loginAndNavigate } from '../fixtures';
import { GraphQLClient } from '../pages';
import { createViaSave, deleteById } from '../business-actions/_helper';

/**
 * b2b edi-detail 页面数据源运行时复核（plan 2026-08-23-0434-2 Phase 2 Add 载体）。
 *
 * 批内序 1 修复 edi-detail.page.yaml:45 的 `$did:Long` → `$did:String`（含 :48 `|| 0` Int
 * 兜底改 `|| null` + adaptor `!== ''` 守卫）——id String 化后 Long 变量收 String 值被
 * GraphQL 类型系统拒绝，adaptor 静默降级为「请输入有效的 EDI 文档ID」空态。本 spec 原样
 * 重放页面 raw-GraphQL 数据源（含类型化变量），证明 doc 经 String id 加载非空。
 *
 * 自包含 setup：经 __save 直置 TO_SEND 态 ErpB2bEdiDoc（b2b-edi-doc.action.spec.ts
 * seedEdiDoc 同范式）+ ErpB2bEdiLog 一行（direction/logTime mandatory，时间线非空证明）。
 * 清理：删 EdiLog + EdiDoc。
 */

test.describe('b2b edi-detail page data source (doc load)', () => {
  test('doc: ErpB2bEdiDoc__get(id:$did String) loads the doc non-empty + log timeline row', async ({ page }) => {
    await loginAndNavigate(page, '/ErpB2bEdiDoc-main');

    const code = `E2E-EDI-DETAIL-${Date.now()}`;
    const doc = await createViaSave(
      page, 'ErpB2bEdiDoc',
      {
        code,
        relatedBillType: 'E2E_RBT_DETAIL',
        relatedBillCode: code,
        state: 'TO_SEND',
        blockingLevel: 'INFO',
        retryCount: 0,
      },
      'id code state',
    );
    expect(doc.state, 'precondition state=TO_SEND').toBe('TO_SEND');

    const log = await createViaSave(
      page, 'ErpB2bEdiLog',
      {
        ediDocId: doc.id,
        direction: 'OUTBOUND',
        resultCode: 'OK',
        resultMsg: 'E2E edi-detail timeline entry',
        logTime: '2026-07-10 08:00:00',
      },
      'id',
    );

    // edi-detail.page.yaml:45 原样查询（类型化 String 变量 $did + Int $lim）
    const query = 'query($lim:Int,$did:String){ logs: ErpB2bEdiLog__findPage(query:{limit:$lim}){ items{ id ediDocId direction resultCode resultMsg requestPayload responsePayload logTime } total } doc: ErpB2bEdiDoc__get(id:$did){ id code state relatedBillType relatedBillCode formatId error retryCount sentAt acknowledgedAt } }';
    const json: any = await new GraphQLClient(page).raw(query, { lim: 5000, did: doc.id });

    expect(json?.errors, 'page query should not return GraphQL errors (adaptor silent-degradation eliminated)').toBeFalsy();
    const d = json?.data?.doc;
    expect(d, 'doc should load non-empty via String id variable $did').toBeTruthy();
    expect(String(d.id), 'doc id round-trips').toBe(String(doc.id));
    expect(d.code, 'doc code round-trips').toBe(code);
    expect(d.state, 'doc state round-trips').toBe('TO_SEND');

    const logs: any[] = json?.data?.logs?.items || [];
    const mine = logs.find((r: any) => String(r.ediDocId) === String(doc.id));
    expect(mine, 'log timeline non-empty for the doc (adaptor client-side ediDocId filter)').toBeTruthy();
    expect(mine?.direction, 'log direction round-trips').toBe('OUTBOUND');

    await deleteById(page, 'ErpB2bEdiLog', log.id);
    await deleteById(page, 'ErpB2bEdiDoc', doc.id);
  });
});
