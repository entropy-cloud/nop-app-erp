package app.erp.ct.service;

import app.erp.ct.biz.IErpCtDocumentBiz;
import app.erp.ct.dao.ErpCtDaoConstants;
import app.erp.ct.dao.dto.DocumentSearchResult;
import app.erp.contract.dao.entity.ErpCtDocument;
import app.erp.ct.service.spi.testfix.FixedTextOcrEngine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 合同文档仓库测试（RC-R1.80 Phase 3，P1-RC-079，UC-CT-10 A/B/C）。
 *
 * <p>覆盖：① 上传缺省填充（ocrStatus=PENDING + retentionDate/purgeDate 按 config 年限推算
 * + fullTextSearch 初始构建含 metadataTags 键值）；② OCR 状态机——manual 基线引擎→FAILED +
 * remark 失败原因；test-fixed 引擎→COMPLETED + ocrText 落库 + fullTextSearch 重建；FAILED 重试
 * 放行；PROCESSING 并发拒绝；③ 手动补录 submitOcrText→COMPLETED 语义 + fullTextSearch 重建；
 * ④ searchDocuments 7 类过滤组合（keyword contains fullTextSearch / code 精确 / docType /
 * contractId / 上传日期范围 / ocrStatus / 归档）+ 归档文档可搜索不可修改；⑤ GraphQL RPC 冒烟
 * （save/startOcr/submitOcrText/searchDocuments 可路由）。
 *
 * <p>OCR 引擎选型经 config {@code erp-ct.ocr-engine} 切换（manual / test-fixed），
 * FixedTextOcrEngine 经 test-mock.beans.xml 注册（nop test-mock 范式）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testBeansFile = "/erp/ct/beans/test-mock.beans.xml")
public class TestErpCtDocumentRepository extends JunitAutoTestCase {

    @RegisterExtension
    static CtFrozenClockExtension frozenClock = new CtFrozenClockExtension();

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpCtDocumentBiz documentBiz;

    @AfterEach
    void resetConfig() {
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_OCR_ENGINE,
                ErpCtConfigs.DEFAULT_OCR_ENGINE);
    }

    // ---------- ① 上传缺省填充 + fullTextSearch 初始构建 ----------

    @Test
    public void testSaveDefaultsAndInitialFullText() {
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtDocument__save", ApiRequest.build(Map.of(
                "data", Map.of(
                        "code", "CT-DOC-REPO-001",
                        "docName", "年度采购合同扫描件",
                        "docType", ErpCtDaoConstants.DOC_TYPE_CONTRACT_SCAN,
                        "metadataTags", "{\"party\":\"供应商A\",\"region\":\"华东\"}"))));
        assertEquals(0, resp.getStatus(), "上传应成功: " + resp);

        ErpCtDocument doc = documentByCode("CT-DOC-REPO-001");
        assertEquals(ErpCtDaoConstants.OCR_STATUS_PENDING, doc.getOcrStatus(), "ocrStatus 缺省 PENDING");
        assertEquals(LocalDate.of(2036, 7, 17), doc.getRetentionDate(),
                "retentionDate 缺省 = 上传日(2026-07-17) + 10 年");
        assertEquals(LocalDate.of(2056, 7, 17), doc.getPurgeDate(),
                "purgeDate 缺省 = retentionDate + 20 年");

        String fullText = doc.getFullTextSearch();
        assertNotNull(fullText, "fullTextSearch 应初始构建");
        assertTrue(fullText.contains("年度采购合同扫描件"), "fullTextSearch 含 docName: " + fullText);
        assertTrue(fullText.contains("CT-DOC-REPO-001"), "fullTextSearch 含 code");
        assertTrue(fullText.contains("party=供应商A") && fullText.contains("region=华东"),
                "fullTextSearch 含 metadataTags 键值: " + fullText);
        assertFalse(fullText.contains("null"), "空 ocrText 分段不拼接 null");
    }

    @Test
    public void testSaveManualRetentionOverrideNotOverwritten() {
        // 手工录入 retentionDate/purgeDate 优先于缺省推算（fill-when-absent）
        executeRpc(mutation, "ErpCtDocument__save", ApiRequest.build(Map.of(
                "data", Map.of(
                        "code", "CT-DOC-REPO-OVR",
                        "docName", "手工保留期文档",
                        "docType", ErpCtDaoConstants.DOC_TYPE_OTHER,
                        "retentionDate", "2027-01-31",
                        "purgeDate", "2030-06-30"))));
        ErpCtDocument doc = documentByCode("CT-DOC-REPO-OVR");
        assertEquals(LocalDate.of(2027, 1, 31), doc.getRetentionDate(), "手工 retentionDate 不被覆盖");
        assertEquals(LocalDate.of(2030, 6, 30), doc.getPurgeDate(), "手工 purgeDate 不被覆盖");
    }

    // ---------- ② OCR 状态机 ----------

    @Test
    public void testOcrManualEngineFailsWithReason() {
        String docId = seedDocument("CT-DOC-OCR-FAIL", "OCR 失败路径文档");

        ApiResponse<?> resp = executeRpc(mutation, "ErpCtDocument__startOcr",
                ApiRequest.build(Map.of("documentId", docId)));
        assertEquals(0, resp.getStatus(), "manual 引擎提交应成功（落 FAILED 非异常）: " + resp);

        ErpCtDocument doc = documentById(docId);
        assertEquals(ErpCtDaoConstants.OCR_STATUS_FAILED, doc.getOcrStatus(), "manual 引擎识别恒失败");
        assertNotNull(doc.getRemark(), "失败原因应记 remark");
        assertTrue(doc.getRemark().contains("OCR 失败"), "remark 记录失败原因: " + doc.getRemark());
        assertNull(doc.getOcrText(), "失败不写 ocrText");
    }

    @Test
    public void testOcrFullTransitionToCompletedAndRetryFromFailed() {
        String docId = seedDocument("CT-DOC-OCR-OK", "OCR 成功路径文档");

        // manual（默认）→ FAILED
        ormTemplate.runInSession(session -> documentBiz.startOcr(docId, new ServiceContextImpl()));
        assertEquals(ErpCtDaoConstants.OCR_STATUS_FAILED, documentById(docId).getOcrStatus());

        // 切换 test-fixed 引擎重试：FAILED→COMPLETED（人工重新提交通道）
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_OCR_ENGINE,
                FixedTextOcrEngine.ENGINE_CODE);
        ErpCtDocument doc = ormTemplate.runInSession(session ->
                documentBiz.startOcr(docId, new ServiceContextImpl()));
        assertEquals(ErpCtDaoConstants.OCR_STATUS_COMPLETED, doc.getOcrStatus(), "FAILED 重试应可 COMPLETED");
        assertEquals(FixedTextOcrEngine.FIXED_TEXT, documentById(docId).getOcrText(), "ocrText = 引擎识别文本");
        assertTrue(documentById(docId).getFullTextSearch().contains("FIXED-OCR-TEXT"),
                "fullTextSearch 重建含识别文本");
    }

    @Test
    public void testOcrProcessingRejectsConcurrentSubmit() {
        String docId = seedDocumentWithOcrStatus("CT-DOC-OCR-PROC", ErpCtDaoConstants.OCR_STATUS_PROCESSING);

        ApiResponse<?> resp = executeRpc(mutation, "ErpCtDocument__startOcr",
                ApiRequest.build(Map.of("documentId", docId)));
        assertNotEquals(0, resp.getStatus(), "PROCESSING 中应拒绝重复提交");
        assertTrue(String.valueOf(resp).contains("ocr-illegal-transition")
                        || String.valueOf(resp).contains("不允许该操作"),
                "应报 ERR_CT_DOCUMENT_OCR_ILLEGAL_TRANSITION: " + resp);
        assertEquals(ErpCtDaoConstants.OCR_STATUS_PROCESSING, documentById(docId).getOcrStatus(),
                "状态零变更");
    }

    // ---------- ③ 手动补录（补录等同 COMPLETED 语义） ----------

    @Test
    public void testSubmitOcrTextManualEntry() {
        String docId = seedDocument("CT-DOC-OCR-MANUAL", "人工补录文档");
        ormTemplate.runInSession(session -> documentBiz.startOcr(docId, new ServiceContextImpl()));
        assertEquals(ErpCtDaoConstants.OCR_STATUS_FAILED, documentById(docId).getOcrStatus(),
                "manual 引擎先落 FAILED");

        ErpCtDocument doc = ormTemplate.runInSession(session -> documentBiz.submitOcrText(docId,
                "合同编号 HT-2026-007 金额拾万元", new ServiceContextImpl()));
        assertEquals(ErpCtDaoConstants.OCR_STATUS_COMPLETED, doc.getOcrStatus(), "补录等同 COMPLETED 语义");
        ErpCtDocument reloaded = documentById(docId);
        assertEquals("合同编号 HT-2026-007 金额拾万元", reloaded.getOcrText());
        assertTrue(reloaded.getFullTextSearch().contains("HT-2026-007"), "补录后 fullTextSearch 重建");
    }

    // ---------- ④ searchDocuments 7 类过滤组合 + 归档可搜索不可修改 ----------

    @Test
    public void testSearchKeywordAndFilterCombinations() {
        String contractId = "88001";
        String docA = seedDocumentFull("CT-DOC-SRCH-A", "采购框架合同扫描", ErpCtDaoConstants.DOC_TYPE_CONTRACT_SCAN,
                contractId, ErpCtDaoConstants.OCR_STATUS_COMPLETED, false, "{\"party\":\"供应商A\"}");
        String docB = seedDocumentFull("CT-DOC-SRCH-B", "质量证明文件", ErpCtDaoConstants.DOC_TYPE_CERTIFICATE,
                null, ErpCtDaoConstants.OCR_STATUS_PENDING, true, null);
        // docA 补入可检索 OCR 文本
        ormTemplate.runInSession(session -> documentBiz.submitOcrText(docA, "关键字段 SUPPLIER-GOLD-STAR",
                new ServiceContextImpl()));

        // keyword → fullTextSearch contains（命中 docA 的 ocrText）
        assertEquals(1, search(null, "SUPPLIER-GOLD-STAR", null, null, null, null, null, null).getDocuments().size(),
                "keyword 命中 ocrText 分段");

        // code 精确
        assertEquals(1, search("CT-DOC-SRCH-B", null, null, null, null, null, null, null).getDocuments().size(),
                "code 精确过滤");
        assertEquals(0, search("CT-DOC-SRCH", null, null, null, null, null, null, null).getDocuments().size(),
                "code 为精确匹配非 like");

        // docType
        assertEquals(1, search(null, null, ErpCtDaoConstants.DOC_TYPE_CERTIFICATE, null, null, null, null, null).getDocuments().size());

        // contractId
        assertEquals(1, search(null, null, null, contractId, null, null, null, null).getDocuments().size());

        // 上传日期范围（冻结时钟 2026-07-17：含当日；未来窗口为空）
        assertEquals(2, search(null, null, null, null, "2026-07-01", "2026-07-17", null, null).getDocuments().size(),
                "日期范围含上传当日");
        assertEquals(0, search(null, null, null, null, "2026-08-01", "2026-08-31", null, null).getDocuments().size(),
                "未来窗口零命中");

        // ocrStatus + archived
        assertEquals(1, search(null, null, null, null, null, null, ErpCtDaoConstants.OCR_STATUS_COMPLETED, null).getDocuments().size());
        assertEquals(1, search(null, null, null, null, null, null, null, true).getDocuments().size(), "归档过滤");
        assertEquals(1, search(null, null, null, null, null, null, null, false).getDocuments().size(), "未归档过滤");

        // keyword + docType 组合（析取过滤面交集）
        assertEquals(1, search(null, "采购框架", ErpCtDaoConstants.DOC_TYPE_CONTRACT_SCAN,
                contractId, "2026-07-01", "2026-07-17", ErpCtDaoConstants.OCR_STATUS_COMPLETED, false).getDocuments().size(),
                "7 类过滤组合命中 docA");
    }

    @Test
    public void testArchivedDocumentSearchableButImmutable() {
        String docId = seedDocumentFull("CT-DOC-SRCH-ARC", "已归档仍可检索", ErpCtDaoConstants.DOC_TYPE_OTHER,
                null, ErpCtDaoConstants.OCR_STATUS_PENDING, true, null);

        List<ErpCtDocument> hits = search(null, "已归档仍可检索", null, null, null, null, null, true).getDocuments();
        assertEquals(1, hits.size(), "归档文档仍可被 keyword 检索（归档期可搜索）");
        assertEquals(docId, hits.get(0).getId());

        ApiResponse<?> update = executeRpc(mutation, "ErpCtDocument__update",
                ApiRequest.build(Map.of("data", Map.of("id", docId, "ocrText", "late-entry"))));
        assertNotEquals(0, update.getStatus(), "归档文档不可修改（只读）");
    }

    // ---------- ⑤ GraphQL RPC 冒烟 ----------

    @Test
    public void testGraphQLRpcSmoke() {
        String docId = seedDocument("CT-DOC-RPC", "RPC 冒烟文档");
        ApiResponse<?> ocr = executeRpc(mutation, "ErpCtDocument__startOcr",
                ApiRequest.build(Map.of("documentId", docId)));
        assertEquals(0, ocr.getStatus(), "startOcr 可路由: " + ocr);

        ApiResponse<?> entry = executeRpc(mutation, "ErpCtDocument__submitOcrText",
                ApiRequest.build(Map.of("documentId", docId, "ocrText", "补录文本")));
        assertEquals(0, entry.getStatus(), "submitOcrText 可路由: " + entry);

        ApiResponse<?> search = executeRpc(query, "ErpCtDocument__searchDocuments",
                ApiRequest.build(Map.of("keyword", "补录文本")));
        assertEquals(0, search.getStatus(), "searchDocuments 可路由: " + search);
    }

    // ---------- helpers ----------

    private DocumentSearchResult search(String code, String keyword, String docType, String contractId,
                                        String from, String to, String ocrStatus, Boolean archived) {
        return ormTemplate.runInSession(session -> documentBiz.searchDocuments(keyword, code, docType,
                contractId, from, to, ocrStatus, archived, new ServiceContextImpl()));
    }

    private ErpCtDocument documentByCode(String code) {
        return ormTemplate.runInSession(session -> daoProvider.daoFor(ErpCtDocument.class).findAllByQuery(
                        eqQuery("code", code)).get(0));
    }

    private ErpCtDocument documentById(String docId) {
        return ormTemplate.runInSession(session -> daoProvider.daoFor(ErpCtDocument.class).getEntityById(docId));
    }

    private String seedDocument(String code, String docName) {
        return seedDocumentFull(code, docName, ErpCtDaoConstants.DOC_TYPE_CONTRACT_SCAN,
                null, ErpCtDaoConstants.OCR_STATUS_PENDING, false, null);
    }

    private String seedDocumentWithOcrStatus(String code, String ocrStatus) {
        return seedDocumentFull(code, "OCR 状态预设文档", ErpCtDaoConstants.DOC_TYPE_CONTRACT_SCAN,
                null, ocrStatus, false, null);
    }

    private String seedDocumentFull(String code, String docName, String docType, String contractId,
                                  String ocrStatus, boolean archived, String metadataTags) {
        String[] ret = new String[1];
        ormTemplate.runInSession(session -> {
            ErpCtDocument doc = daoProvider.daoFor(ErpCtDocument.class).newEntity();
            doc.orm_disableAutoStamp(true);
            doc.setCode(code);
            doc.setDocName(docName);
            doc.setDocType(docType);
            if (contractId != null) {
                doc.setContractId(contractId);
            }
            doc.setOcrStatus(ocrStatus);
            doc.setIsArchived(archived);
            if (archived) {
                doc.setArchiveDate(LocalDate.of(2026, 7, 1));
            }
            if (metadataTags != null) {
                doc.setMetadataTags(metadataTags);
            }
            doc.setFullTextSearch((docName + " " + code).trim());
            doc.setCreatedBy("ct-repo-test");
            doc.setUpdatedBy("ct-repo-test");
            doc.setCreateTime(java.sql.Timestamp.valueOf(CtFrozenClockExtension.REFERENCE_DATE.atStartOfDay()));
            doc.setUpdateTime(java.sql.Timestamp.valueOf(CtFrozenClockExtension.REFERENCE_DATE.atStartOfDay()));
            daoProvider.daoFor(ErpCtDocument.class).saveEntity(doc);
            ret[0] = doc.getId();
            return null;
        });
        return ret[0];
    }

    private io.nop.api.core.beans.query.QueryBean eqQuery(String field, Object value) {
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq(field, value));
        return q;
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
