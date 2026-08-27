package io.nop.app.all.it;

import app.erp.fin.service.ErpFinConfigs;

import app.erp.fin.biz.IErpFinApDocumentBiz;
import app.erp.fin.dao.entity.ErpFinApDocument;
import app.erp.fin.dao.entity.ErpFinApDocumentLog;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.ServiceContextImpl;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.graphql.core.IGraphQLExecutionContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E3.5 文档摄取管道 Proof（`document-driven-ap-automation.md` §1/§2 + 前置调研结论分层验证）：
 *
 * <ul>
 *   <li>正路径：上传（txt 真实内容走默认文本抽取引擎）→ 解析要素 → 规则分类（供应商匹配置信度 1.0）→
 *       草稿 ErpPurInvoice（UNSUBMITTED 人工确认门 AP-1）→ invoiceId 回链 + 全步骤轨迹日志；</li>
 *   <li>低置信人工门：无供应商匹配/要素缺失 → MANUAL_REVIEW 不生成草稿；人工放行
 *       {@code confirmAndDraftApDocument}（显式 partnerId）→ DRAFTED；</li>
 *   <li>扫描件路径：无可抽取文本 → 置信度 0 挂人工门（JUnit 层验证，真 OCR 引擎为 SPI 可插拔项）；</li>
 *   <li>幂等/失败重试：DRAFTED 重复处理拒绝；MANUAL_REVIEW 重试计数 + RETRY 轨迹；</li>
 *   <li>config-gate：管道关闭时 process 拒绝（默认关闭，既有套件零回归）。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@io.nop.api.core.annotations.autotest.NopTestProperty(name = "nop.file.store-dir", value = "./target/ap-doc-file-store")
public class TestErpFinApDocumentPipeline extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IGraphQLEngine graphQLEngine;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    io.nop.orm.IOrmTemplate ormTemplate;

    @Inject
    IErpFinApDocumentBiz apDocumentBiz;

    @AfterEach
    public void resetConfig() {
        setConfig(ErpFinConfigs.CONFIG_AP_DOC_PIPELINE_ENABLED, "false");
    }

    @Test
    public void testUploadParseClassifyDraftHappyPath() {
        enablePipeline();
        seedCurrency();
        seedSupplier("深圳市测试供应商有限公司", "SUP-APD-1");

        String content = "增值税专用发票\n"
                + "名称：深圳市测试供应商有限公司\n"
                + "发票号码：INV-2026-0001\n"
                + "开票日期：2026年08月15日\n"
                + "金额（不含税）：8,000.00\n"
                + "税额：1,040.00\n"
                + "价税合计（大写）玖仟零肆拾元整 9,040.00\n";
        String docId = upload("invoice-happy.txt", "text/plain", content);

        Map<String, Object> doc = process(docId);
        assertEquals("DRAFTED", doc.get("status"), "达标置信度应生成草稿: " + doc);
        assertEquals(0, new BigDecimal("1.0000").compareTo(new BigDecimal(String.valueOf(doc.get("confidence")))),
                "供应商匹配 + 要素齐 → 置信度 1.0");
        assertEquals("VAT_INVOICE", doc.get("docType"));
        assertNotNull(doc.get("invoiceId"), "草稿发票回链应落库");
        assertNull(doc.get("errorMsg"));

        // 草稿经既有 IErpPurInvoiceBiz 管道创建（UNSUBMITTED 人工确认门）
        Map<String, Object> invoice = get("ErpPurInvoice", String.valueOf(doc.get("invoiceId")),
                "id code invoiceNo businessDate supplierId approveStatus docStatus totalAmountWithTax totalTaxAmount");
        assertEquals("UNSUBMITTED", invoice.get("approveStatus"), "草稿必须为 UNSUBMITTED（人工门不可绕过）");
        assertEquals("INV-2026-0001", invoice.get("invoiceNo"), "解析结果只作预填（AP-5）");
        assertEquals("2026-08-15", String.valueOf(invoice.get("businessDate")));
        assertEquals(0, new BigDecimal("9040.00").compareTo(new BigDecimal(String.valueOf(invoice.get("totalAmountWithTax")))));

        // 全步骤轨迹日志（审计追溯：RECEIVE → PARSE → CLASSIFY → DRAFT）
        List<String> steps = logSteps(docId);
        assertEquals(List.of("RECEIVE", "PARSE", "CLASSIFY", "DRAFT"), steps, "轨迹日志按序覆盖四步骤");
    }

    @Test
    public void testLowConfidenceManualGateThenConfirmAndDraft() {
        enablePipeline();
        seedCurrency();
        String partnerId = seedSupplier("深圳市测试供应商有限公司", "SUP-APD-2");

        // 无供应商名匹配 + 要素缺失 → 置信度 0.3 < 0.7 → 人工门（不生成草稿）
        String content = "收据\n收款单位：某某公司\n金额：300.00\n";
        String docId = upload("receipt-lowconf.txt", "text/plain", content);

        Map<String, Object> doc = process(docId);
        assertEquals("MANUAL_REVIEW", doc.get("status"), "低置信应挂人工队列");
        assertNull(doc.get("invoiceId"), "人工门内不得生成草稿（AP-1）");
        assertEquals(0, new BigDecimal("0.3000").compareTo(new BigDecimal(String.valueOf(doc.get("confidence")))));
        assertTrue(String.valueOf(doc.get("errorMsg")).contains("人工复核"));
        assertTrue(logSteps(docId).contains("MANUAL_REVIEW"), "人工门应落轨迹");

        // 人工放行：显式补充对应方
        Map<String, Object> confirmed = mutation("ErpFinApDocument__confirmAndDraftApDocument",
                Map.of("documentId", docId, "partnerId", partnerId));
        assertEquals("DRAFTED", confirmed.get("status"));
        assertNotNull(confirmed.get("invoiceId"), "人工放行后生成草稿");
    }

    @Test
    public void testScannedImageNoTextGoesManualReview() {
        enablePipeline();
        // 默认引擎不支持图片 → 无文本 → 置信度 0 挂人工门（真 OCR 引擎为 SPI 可插拔项）
        byte[] fakeImage = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13};
        String docId = upload("scan.png", "image/png", new String(fakeImage, StandardCharsets.ISO_8859_1));

        Map<String, Object> doc = process(docId);
        assertEquals("MANUAL_REVIEW", doc.get("status"), "扫描件无文本应挂人工门");
        assertEquals(0, new BigDecimal("0.0000").compareTo(new BigDecimal(String.valueOf(doc.get("confidence")))));
        assertNull(doc.get("invoiceId"));
    }

    @Test
    public void testRetryCountingAndDraftedIdempotentGuard() {
        enablePipeline();
        String content = "普通发票\n金额：100.00\n";
        String docId = upload("retry.txt", "text/plain", content);

        Map<String, Object> doc = process(docId);
        assertEquals("MANUAL_REVIEW", doc.get("status"));

        // 重试：计数 + RETRY 轨迹（内容未变仍挂人工门）
        Map<String, Object> retried = mutation("ErpFinApDocument__retryApDocument", Map.of("documentId", docId));
        assertEquals("MANUAL_REVIEW", retried.get("status"));
        assertEquals(1, ((Number) retried.get("retryCount")).intValue(), "retryCount 应 +1");
        assertTrue(logSteps(docId).contains("RETRY"), "重试应落轨迹");

        // 人工放行生成草稿后，重复 process 拒绝（幂等守卫）
        seedCurrency();
        String partnerId = seedSupplier("深圳市测试供应商有限公司", "SUP-APD-3");
        Map<String, Object> confirmed = mutation("ErpFinApDocument__confirmAndDraftApDocument",
                Map.of("documentId", docId, "partnerId", partnerId));
        assertEquals("DRAFTED", confirmed.get("status"));

        ApiResponse<?> again = executeRpc(GraphQLOperationType.mutation, "ErpFinApDocument__processApDocument",
                ApiRequest.build(Map.of("documentId", docId)));
        assertTrue(again.getStatus() != 0, "DRAFTED 重复处理应拒绝");
    }

    @Test
    public void testUploadRateLimitedGuard() {
        // E3.6 护栏（IRateLimiter 接线落点 = E3.5 管道入口，自动化批量面）：低 rps 下第二次即时上传被拒
        enablePipeline();
        setConfig(ErpFinConfigs.CONFIG_AP_DOC_UPLOAD_RATE_LIMIT_RPS, "0.1");
        String content = "发票\n名称：深圳市测试供应商有限公司\n发票号码：INV-RL-0001\n";
        try {
            String first = upload("rate-limit-1.txt", "text/plain", content);
            assertNotNull(first, "第一次上传应通过（令牌桶首个 permit）");

            ApiResponse<?> second = executeRpc(GraphQLOperationType.mutation, "ErpFinApDocument__uploadApDocument",
                    ApiRequest.build(Map.of("fileName", "rate-limit-2.txt", "mimeType", "text/plain",
                            "fileBase64", java.util.Base64.getEncoder()
                                    .encodeToString(content.getBytes(StandardCharsets.UTF_8)))));
            assertTrue(second.getStatus() != 0, "限流窗口内第二次上传应被拒");
            assertTrue(String.valueOf(second.getCode()).contains("rate-limited"),
                    "拒绝应携带 rate-limited 错误码: " + second.getCode());
        } finally {
            setConfig(ErpFinConfigs.CONFIG_AP_DOC_UPLOAD_RATE_LIMIT_RPS,
                    String.valueOf(ErpFinConfigs.DEFAULT_AP_DOC_UPLOAD_RATE_LIMIT_RPS));
        }
    }

    @Test
    public void testPipelineDisabledByDefault() {
        // config-gate 默认关闭：process 拒绝且错误码可断言
        String content = "发票\n名称：深圳市测试供应商有限公司\n发票号码：INV-2026-0002\n";
        String docId = upload("gate-off.txt", "text/plain", content);

        ApiResponse<?> resp = executeRpc(GraphQLOperationType.mutation, "ErpFinApDocument__processApDocument",
                ApiRequest.build(Map.of("documentId", docId)));
        assertTrue(resp.getStatus() != 0, "管道关闭时应拒绝处理");
        assertTrue(String.valueOf(resp.getCode()).contains("pipeline-disabled"),
                "拒绝应携带 pipeline-disabled 错误码: " + resp.getCode());
    }

    // ---------- seeds & helpers ----------

    private void enablePipeline() {
        setConfig(ErpFinConfigs.CONFIG_AP_DOC_PIPELINE_ENABLED, "true");
    }

    private void setConfig(String key, String value) {
        io.nop.api.core.config.AppConfig.getConfigProvider().assignConfigValue(key, value);
    }

    private void seedCurrency() {
        ormTemplate.runInSession(sess -> {
            IEntityDao<app.erp.md.dao.entity.ErpMdCurrency> dao = daoProvider.daoFor(app.erp.md.dao.entity.ErpMdCurrency.class);
            if (dao.getEntityById("1") == null) {
                app.erp.md.dao.entity.ErpMdCurrency c = dao.newEntity();
                c.orm_propValue(1, "1");
                c.setCode("CNY");
                c.setName("人民币");
                dao.saveEntity(c);
            }
            return null;
        });
    }

    private String seedSupplier(String name, String code) {
        return ormTemplate.runInSession(sess -> {
            IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
            QueryBean q = new QueryBean();
            q.addFilter(eq("name", name));
            List<ErpMdPartner> existing = dao.findAllByQuery(q);
            if (!existing.isEmpty()) {
                return existing.get(0).getId();
            }
            ErpMdPartner p = dao.newEntity();
            p.setCode(code);
            p.setName(name);
            p.setPartnerType("SUPPLIER");
            p.setStatus("ACTIVE");
            dao.saveEntity(p);
            return p.getId();
        });
    }

    private String upload(String fileName, String mimeType, String content) {
        String base64 = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> doc = mutation("ErpFinApDocument__uploadApDocument",
                Map.of("fileName", fileName, "mimeType", mimeType, "fileBase64", base64));
        assertEquals("RECEIVED", doc.get("status"), "上传后应落 RECEIVED");
        assertNotNull(doc.get("fileId"), "文件本体应存 nop-file（fileId 引用，AP-3）");
        assertNotNull(doc.get("id"));
        return String.valueOf(doc.get("id"));
    }

    private Map<String, Object> process(String docId) {
        return mutation("ErpFinApDocument__processApDocument", Map.of("documentId", docId));
    }

    private Map<String, Object> mutation(String action, Map<String, Object> args) {
        ApiResponse<?> resp = executeRpc(GraphQLOperationType.mutation, action, ApiRequest.build(args));
        assertEquals(0, resp.getStatus(), action + " 应成功: " + resp);
        return (Map<String, Object>) resp.getData();
    }

    private Map<String, Object> get(String entity, String id, String fields) {
        ApiResponse<?> resp = executeRpc(GraphQLOperationType.query, entity + "__get",
                ApiRequest.build(Map.of("id", id)));
        assertEquals(0, resp.getStatus(), entity + "__get 应成功");
        return (Map<String, Object>) resp.getData();
    }

    private List<String> logSteps(String docId) {
        IEntityDao<ErpFinApDocumentLog> dao = daoProvider.daoFor(ErpFinApDocumentLog.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("documentId", docId));
        List<ErpFinApDocumentLog> logs = dao.findAllByQuery(q);
        // 按数值 id 排序（seq 随插入单调递增；id 为 String 列，避免字典序失真）
        logs.sort(java.util.Comparator.comparingLong(l -> Long.parseLong(l.getId())));
        return logs.stream().map(ErpFinApDocumentLog::getStep).collect(java.util.stream.Collectors.toList());
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
