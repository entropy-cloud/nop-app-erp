import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  deleteById,
} from './_helper';
import {
  runP2pChain,
  cleanupP2p,
  findVoucherIdByBillCode,
  assertVoucherLines,
  P2P_EXPECT,
  SEED,
} from '../orchestration/_helper';

/**
 * finance GL Mapping 凭证科目路由浏览器层 E2E（plan 2026-07-26-0410-1）。
 *
 * 验证 GL Mapping 规则命中时凭证行 subjectCode 被覆盖的全栈浏览器层路径：
 *   GraphQL `ErpFinGlMappingRule__save` 建规则 → runP2pChain 链路审核过账 →
 *   `ErpFinPostingProcessor.resolveSubjects` 调 `IErpFinGlMappingResolver` →
 *   凭证行 subjectCode 覆盖可观测（assertVoucherLines 断言）。
 *
 * 三组断言：
 *   1. 命中覆盖 —— 建 AP_INVOICE+PURCHASE 规则 → 目标科目 1401（区别种子默认 1403）→
 *      AP_INVOICE 凭证 PURCHASE 行被 1401 替换（Dr 1401=50），INPUT_VAT/ACCOUNTS_PAYABLE
 *      两行（2221/2202）保持 Provider 默认不变（证明仅命中 accountKey 被覆盖）。
 *   2. 控制对照 —— 同一链路无规则时凭证行保持 1403（先于建规则运行同一 runP2pChain，
 *      断言 1403/2221/2202 默认科目，证明覆盖非偶然）。
 *   3. orgId 维度 —— org-dimension-enabled=true（webServer JVM arg 全局）下，建 orgId=1
 *      规则（非匹配链路 org=2）→ 链路凭证行保留默认 1403（orgId 维度差异化：cache 按
 *      orgId 分桶，org=2 桶空 → null → 保留 Provider fallback）。
 *
 * 缓存自动失效：`ErpFinGlMappingRuleBizModel.defaultPrepareSave/Delete` 注册 post-commit
 * `invalidateCache`（`:42-64`），spec 经 `__save`/`__delete` 创建/清理规则后无需手动刷缓存。
 *
 * 清理：规则经 `ErpFinGlMappingRule__delete`（defaultPrepareDelete 自动失效缓存）；
 * P2P 链路产物（凭证/凭证行/回链/AR-AP/库存）经既有 `cleanupP2p`（mapped 科目凭证与默认
 * 科目凭证同 billCode，`cleanupVoucherByBillCode` 已覆盖）。
 *
 * 覆盖目标科目 1401（原材料，ASSET/DEBIT，种子 id=9）经种子 `erp_md_subject.csv` 可达，
 * 覆盖后 `resolveSubjects` 的 `code → ErpMdSubject.findByCode` 查找成功；与默认 1403（在途
 * 物资）同向（DEBIT asset），方向语义一致。org 1（GROUP-HQ）+ org 2（ERP-CO）均存于种子。
 */

/** 覆盖目标科目（区别种子默认 PURCHASE=1403；1401 原材料 DEBIT asset 同向）。 */
const OVERRIDE_SUBJECT_CODE = '1401';

test.describe('finance GL Mapping voucher subject routing (browser-layer E2E)', () => {
  test('hit override + control: AP_INVOICE PURCHASE line overridden to 1401, other lines unchanged', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinGlMappingRule-main');

    // ---- 控制对照：无规则时同一 runP2pChain 链路凭证行保持默认 1403/2221/2202 ----
    const controlChain = await runP2pChain(page);
    let controlRule: any;
    try {
      const controlVoucherId = await findVoucherIdByBillCode(page, controlChain.codes.invoice, 'NORMAL');
      await assertVoucherLines(page, controlVoucherId, [
        { subjectCode: '1403', dcDirection: 'DEBIT', debitAmount: P2P_EXPECT.invoiceNet, creditAmount: 0 },
        { subjectCode: '2221', dcDirection: 'DEBIT', debitAmount: P2P_EXPECT.invoiceTax, creditAmount: 0 },
        { subjectCode: '2202', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: P2P_EXPECT.invoiceWithTax },
      ]);
    } finally {
      await cleanupP2p(page, controlChain);
    }

    // ---- 命中覆盖：建规则（orgId=2 匹配链路 org）→ 同一链路 PURCHASE 行被 1401 覆盖 ----
    const ts = Date.now();
    try {
      controlRule = await createViaSave(
        page, 'ErpFinGlMappingRule',
        {
          code: `E2E-GLMAP-HIT-${ts}`,
          name: 'E2E GL Mapping hit override PURCHASE',
          orgId: SEED.ORG,
          businessType: 'AP_INVOICE',
          accountKey: 'PURCHASE',
          targetSubjectCode: OVERRIDE_SUBJECT_CODE,
          priority: 0,
          isActive: true,
        },
        'id code targetSubjectCode',
      );
      expect(controlRule.targetSubjectCode, 'rule __save should persist targetSubjectCode')
        .toBe(OVERRIDE_SUBJECT_CODE);

      const hitChain = await runP2pChain(page);
      try {
        const hitVoucherId = await findVoucherIdByBillCode(page, hitChain.codes.invoice, 'NORMAL');
        // PURCHASE 行被规则覆盖为 1401（Dr 1401=50）；INPUT_VAT(2221)/ACCOUNTS_PAYABLE(2202) 无规则 → 保留默认
        await assertVoucherLines(page, hitVoucherId, [
          { subjectCode: OVERRIDE_SUBJECT_CODE, dcDirection: 'DEBIT', debitAmount: P2P_EXPECT.invoiceNet, creditAmount: 0 },
          { subjectCode: '2221', dcDirection: 'DEBIT', debitAmount: P2P_EXPECT.invoiceTax, creditAmount: 0 },
          { subjectCode: '2202', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: P2P_EXPECT.invoiceWithTax },
        ]);
      } finally {
        await cleanupP2p(page, hitChain);
      }
    } finally {
      // defaultPrepareDelete post-commit 自动失效缓存（无需手动刷）
      if (controlRule?.id) await deleteById(page, 'ErpFinGlMappingRule', controlRule.id);
    }
  });

  test('orgId dimension: non-matching org (orgId=1) rule retains default subjectCode for org=2 chain', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinGlMappingRule-main');

    // org-dimension-enabled=true（webServer JVM arg 全局）：建 orgId=1 规则（非匹配链路 org=2）
    // → cache 按 orgId 分桶，org=2 桶空 → resolver 返回 null → 保留 Provider 默认 1403。
    const ts = Date.now();
    let nonMatchingRule: any;
    try {
      nonMatchingRule = await createViaSave(
        page, 'ErpFinGlMappingRule',
        {
          code: `E2E-GLMAP-ORG-MISMATCH-${ts}`,
          name: 'E2E GL Mapping orgId dimension non-matching (orgId=1)',
          orgId: 1, // GROUP-HQ（种子 erp_md_organization.csv id=1），区别于链路 org=2
          businessType: 'AP_INVOICE',
          accountKey: 'PURCHASE',
          targetSubjectCode: OVERRIDE_SUBJECT_CODE,
          priority: 0,
          isActive: true,
        },
        'id code orgId targetSubjectCode',
      );
      expect(nonMatchingRule.orgId, 'rule __save should persist orgId=1').toBe(1);

      const chain = await runP2pChain(page); // 链路固定 org=SEED.ORG=2
      try {
        const voucherId = await findVoucherIdByBillCode(page, chain.codes.invoice, 'NORMAL');
        // org 不匹配 → 保留 Provider 默认 1403（orgId 维度差异化：非匹配组织规则不覆盖）
        await assertVoucherLines(page, voucherId, [
          { subjectCode: '1403', dcDirection: 'DEBIT', debitAmount: P2P_EXPECT.invoiceNet, creditAmount: 0 },
          { subjectCode: '2221', dcDirection: 'DEBIT', debitAmount: P2P_EXPECT.invoiceTax, creditAmount: 0 },
          { subjectCode: '2202', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: P2P_EXPECT.invoiceWithTax },
        ]);
      } finally {
        await cleanupP2p(page, chain);
      }
    } finally {
      if (nonMatchingRule?.id) await deleteById(page, 'ErpFinGlMappingRule', nonMatchingRule.id);
    }
  });
});
