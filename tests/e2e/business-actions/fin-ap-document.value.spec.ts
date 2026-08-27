import { test, expect, loginAndNavigate } from '../fixtures';
import { GraphQLClient } from '../pages';

/**
 * E3.5 文档摄取管道 value-spec（`document-driven-ap-automation.md` §1/§2）：
 * 上传 → 解析（默认文本抽取引擎，数字文本夹具）→ 规则分类（seed 供应商匹配置信度 1.0）→
 * 草稿 ErpPurInvoice（UNSUBMITTED 人工确认门 AP-1）→ 三单匹配预填断言（解析结果只作预填，AP-5）。
 *
 * 服务器 config：`-Derp-fin.ap-doc-pipeline-enabled=true`（playwright.config.ts/_tmp-server.sh 固定）。
 * 清理：删 AP 文档 + 草稿发票（finally，保护共享 DB）。
 */

const SUPPLIER_SEED = { id: '3', name: '北方钢铁供应商' } as const;

test.describe('finance AP document ingestion pipeline', () => {
  test('upload → parse → classify → draft invoice (prefill, manual gate kept)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinApDocument-main');
    const gql = new GraphQLClient(page);
    const ts = Date.now();

    const content = [
      '增值税专用发票',
      `名称：${SUPPLIER_SEED.name}`,
      `发票号码：E2E-APD-${ts}`,
      '开票日期：2026年08月20日',
      '金额（不含税）：1,000.00',
      '税额：130.00',
      '价税合计（大写）壹仟壹佰叁拾元整 1,130.00',
    ].join('\n');
    const base64 = Buffer.from(content, 'utf-8').toString('base64');

    const uploadArgs = { fileName: `e2e-apd-${ts}.txt`, mimeType: 'text/plain', fileBase64: base64 };
    let docId: string | null = null;
    let invoiceId: string | null = null;
    try {
      const doc = await gql.callMutationOk<any>(
        'ErpFinApDocument', 'uploadApDocument', uploadArgs, 'id status fileId fileName',
      );
      docId = doc?.id ?? null;
      expect(docId, 'upload should persist RECEIVED doc').toBeTruthy();
      expect(doc.status).toBe('RECEIVED');
      expect(doc.fileId, 'file body stored in nop-file (fileId ref, AP-3)').toBeTruthy();

      const processed = await gql.callMutationOk<any>(
        'ErpFinApDocument', 'processApDocument', { documentId: docId },
        'id status docType confidence partnerId invoiceId parseResult',
      );
      expect(processed.status, 'high-confidence doc should be DRAFTED').toBe('DRAFTED');
      expect(processed.docType).toBe('VAT_INVOICE');
      expect(Number(processed.confidence), 'seed supplier matched → confidence 1.0').toBe(1);
      expect(processed.invoiceId, 'draft invoice back-link').toBeTruthy();
      invoiceId = String(processed.invoiceId);
      expect(String(processed.parseResult)).toContain(`E2E-APD-${ts}`);

      // 草稿发票（既有 IErpPurInvoiceBiz 管道 + UNSUBMITTED 人工确认门 + 预填断言）
      const invoice = await gql.get<any>(
        'ErpPurInvoice', invoiceId,
        'id invoiceNo approveStatus docStatus businessDate totalAmountWithTax totalTaxAmount',
      );
      expect(invoice.approveStatus, 'draft must stay UNSUBMITTED (manual gate, AP-1)').toBe('UNSUBMITTED');
      expect(invoice.docStatus).toBe('DRAFT');
      expect(invoice.invoiceNo, 'parsed invoiceNo prefilled (AP-5)').toBe(`E2E-APD-${ts}`);
      expect(String(invoice.businessDate)).toContain('2026-08-20');
      expect(Number(invoice.totalAmountWithTax)).toBe(1130);
      expect(Number(invoice.totalTaxAmount)).toBe(130);

      // 审计追溯：处理轨迹日志覆盖 RECEIVE → PARSE → CLASSIFY → DRAFT
      const logs = await gql.findItems<any>(
        'ErpFinApDocumentLog',
        { $type: 'eq', name: 'documentId', value: docId },
        'step success detail',
      );
      const steps = logs.map((l: any) => l.step);
      expect(steps).toContain('RECEIVE');
      expect(steps).toContain('PARSE');
      expect(steps).toContain('CLASSIFY');
      expect(steps).toContain('DRAFT');
    } finally {
      if (invoiceId) await gql.delete('ErpPurInvoice', invoiceId);
      if (docId) await gql.delete('ErpFinApDocument', docId);
    }
  });

  test('low-confidence document goes to MANUAL_REVIEW (manual gate not bypassed)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinApDocument-main');
    const gql = new GraphQLClient(page);
    const ts = Date.now();

    const content = '收据\n收款单位：不知名公司\n金额：50.00\n';
    const base64 = Buffer.from(content, 'utf-8').toString('base64');
    let docId: string | null = null;
    try {
      const doc = await gql.callMutationOk<any>(
        'ErpFinApDocument', 'uploadApDocument',
        { fileName: `e2e-apd-low-${ts}.txt`, mimeType: 'text/plain', fileBase64: base64 },
        'id',
      );
      docId = doc?.id ?? null;
      expect(docId).toBeTruthy();

      const processed = await gql.callMutationOk<any>(
        'ErpFinApDocument', 'processApDocument', { documentId: docId },
        'id status confidence invoiceId',
      );
      expect(processed.status, 'low-confidence doc must be MANUAL_REVIEW').toBe('MANUAL_REVIEW');
      expect(processed.invoiceId ?? null, 'no draft before human confirmation (AP-1)').toBeNull();
      expect(Number(processed.confidence)).toBeLessThan(0.7);
    } finally {
      if (docId) await gql.delete('ErpFinApDocument', docId);
    }
  });
});
