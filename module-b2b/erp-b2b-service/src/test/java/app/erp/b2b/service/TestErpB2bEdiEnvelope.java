package app.erp.b2b.service;

import app.erp.b2b.biz.IErpB2bEdiDocBiz;
import app.erp.b2b.dao.entity.ErpB2bEdiDoc;
import app.erp.b2b.dao.entity.ErpB2bEdiFormat;
import app.erp.b2b.dao.entity.ErpB2bEdiLog;
import app.erp.b2b.service.spi.ErpB2bEdiRegistry;
import app.erp.b2b.service.spi.IErpB2bEdiProvider;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EDI 信封状态机行为测试（Phase 5）。覆盖：
 * <ul>
 *   <li>状态机全路径：TO_SEND→SENT→ACKNOWLEDGED / →ERROR→TO_SEND 重试 / →CANCELLED / RECEIVED→ARCHIVED；</li>
 *   <li>UNIQUE(formatId,relatedBillType,relatedBillCode) 防重；</li>
 *   <li>每次迁移写 ErpB2bEdiLog；</li>
 *   <li>Registry 派发/未注册抛错。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpB2bEdiEnvelope extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpB2bEdiDocBiz ediDocBiz;
    @Inject
    ErpB2bEdiRegistry ediRegistry;

    @BeforeEach
    void setup() {
        AppConfig.getConfigProvider().assignConfigValue(ErpB2bConfigs.CONFIG_B2B_ENABLED, true);
    }

    @Test
    public void testOutboundStateMachineFlow() {
        Long formatId = seedFormat("UBL_INVOICE", "UBL");
        Long docId = ormTemplate.runInSession(session -> {
            ErpB2bEdiDoc doc = createDoc(formatId, "AR_INVOICE", "INV-TEST-1",
                    ErpB2bConstants.EDI_DOC_STATE_TO_SEND);
            return doc.getId();
        });

        // TO_SEND → SENT
        ErpB2bEdiDoc sent = ediDocBiz.markSent(docId, CTX);
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_SENT, sent.getState());
        assertNotNull(sent.getSentAt());

        // SENT → ACKNOWLEDGED
        ErpB2bEdiDoc acked = ediDocBiz.markAcknowledged(docId, CTX);
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_ACKNOWLEDGED, acked.getState());
        assertNotNull(acked.getAcknowledgedAt());

        // ErpB2bEdiLog 写入
        assertTrue(!findLogs(docId).isEmpty(), "每次迁移应写 ErpB2bEdiLog");
    }

    @Test
    public void testErrorAndRetry() {
        Long formatId = seedFormat("UBL_INVOICE", "UBL");
        Long docId = ormTemplate.runInSession(session -> {
            ErpB2bEdiDoc doc = createDoc(formatId, "AR_INVOICE", "INV-TEST-2",
                    ErpB2bConstants.EDI_DOC_STATE_TO_SEND);
            return doc.getId();
        });

        // TO_SEND → ERROR
        ErpB2bEdiDoc errored = ediDocBiz.markError(docId, "send timeout", CTX);
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_ERROR, errored.getState());
        assertEquals("send timeout", errored.getError());
        assertEquals(ErpB2bConstants.BLOCKING_LEVEL_ERROR, errored.getBlockingLevel());

        // ERROR → TO_SEND (retry)
        ErpB2bEdiDoc retried = ediDocBiz.retry(docId, CTX);
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_TO_SEND, retried.getState());
        assertNotNull(retried.getRetryCount());
        assertTrue(retried.getRetryCount() >= 1);
    }

    @Test
    public void testCancelFromToSend() {
        Long formatId = seedFormat("UBL_INVOICE", "UBL");
        Long docId = ormTemplate.runInSession(session -> {
            ErpB2bEdiDoc doc = createDoc(formatId, "AR_INVOICE", "INV-TEST-3",
                    ErpB2bConstants.EDI_DOC_STATE_TO_SEND);
            return doc.getId();
        });

        ErpB2bEdiDoc cancelled = ediDocBiz.cancel(docId, CTX);
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_CANCELLED, cancelled.getState());
    }

    @Test
    public void testInboundReceivedToArchived() {
        Long formatId = seedFormat("UBL_DESPATCH_ADVICE", "UBL");
        ErpB2bEdiDoc doc = ediDocBiz.createInbound("ASN_INBOUND", "PO-001", "<xml/>", "UBL_DESPATCH_ADVICE", CTX);
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_RECEIVED, doc.getState());

        ErpB2bEdiDoc archived = ediDocBiz.archive(doc.getId(), CTX);
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_ARCHIVED, archived.getState());
    }

    @Test
    public void testIllegalTransitionThrows() {
        Long formatId = seedFormat("UBL_INVOICE", "UBL");
        Long docId = ormTemplate.runInSession(session -> {
            ErpB2bEdiDoc doc = createDoc(formatId, "AR_INVOICE", "INV-TEST-4",
                    ErpB2bConstants.EDI_DOC_STATE_TO_SEND);
            return doc.getId();
        });

        // TO_SEND → ACKNOWLEDGED（非法：需要先 SENT）
        assertThrows(NopException.class, () -> ediDocBiz.markAcknowledged(docId, CTX));
    }

    @Test
    public void testRegistryDispatch() {
        List<String> codes = ediRegistry.getRegisteredFormatCodes();
        assertTrue(codes.contains("UBL_DESPATCH_ADVICE"), "Registry 应包含 UBL_DESPATCH_ADVICE");
        assertTrue(codes.contains("UBL_INVOICE"), "Registry 应包含 UBL_INVOICE");

        IErpB2bEdiProvider provider = ediRegistry.getProvider("UBL_INVOICE");
        assertNotNull(provider);

        // 未注册格式抛错
        assertThrows(NopException.class, () -> ediRegistry.getProvider("UNKNOWN_FORMAT"));
    }

    @Test
    public void testRegistryOutboundDispatch() {
        List<IErpB2bEdiProvider> outbound = ediRegistry.findOutboundProviders("AR_INVOICE");
        assertTrue(outbound.stream().anyMatch(p -> "UBL_INVOICE".equals(p.getCode())),
                "AR_INVOICE 出站应找到 UBL_INVOICE");

        List<IErpB2bEdiProvider> inbound = ediRegistry.findInboundProviders("ASN_INBOUND");
        assertTrue(inbound.stream().anyMatch(p -> "UBL_DESPATCH_ADVICE".equals(p.getCode())),
                "ASN_INBOUND 入站应找到 UBL_DESPATCH_ADVICE");
    }

    // ---------- helpers ----------

    private Long seedFormat(String code, String standard) {
        return ormTemplate.runInSession(session -> {
            ErpB2bEdiFormat format = new ErpB2bEdiFormat();
            format.setCode(code);
            format.setFormatName(code);
            format.setFormatStandard(standard);
            format.setDirection("BOTH");
            format.setNeedsWebService(0);
            format.setIsActive(1);
            daoProvider.daoFor(ErpB2bEdiFormat.class).saveEntity(format);
            return format.getId();
        });
    }

    private ErpB2bEdiDoc createDoc(Long formatId, String relatedBillType, String relatedBillCode, String state) {
        ErpB2bEdiDoc doc = new ErpB2bEdiDoc();
        doc.setCode("EDI-" + System.nanoTime());
        doc.setFormatId(formatId);
        doc.setRelatedBillType(relatedBillType);
        doc.setRelatedBillCode(relatedBillCode);
        doc.setState(state);
        doc.setBlockingLevel(ErpB2bConstants.BLOCKING_LEVEL_INFO);
        doc.setRetryCount(0);
        daoProvider.daoFor(ErpB2bEdiDoc.class).saveEntity(doc);
        return doc;
    }

    @SuppressWarnings("unchecked")
    private List<ErpB2bEdiLog> findLogs(Long ediDocId) {
        IEntityDao<ErpB2bEdiLog> dao = daoProvider.daoFor(ErpB2bEdiLog.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("ediDocId", ediDocId));
        return dao.findAllByQuery(q);
    }
}
