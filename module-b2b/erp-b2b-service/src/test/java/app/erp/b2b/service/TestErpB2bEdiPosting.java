package app.erp.b2b.service;

import app.erp.b2b.biz.IErpB2bEdiDocBiz;
import app.erp.b2b.dao.entity.ErpB2bEdiDoc;
import app.erp.b2b.dao.entity.ErpB2bEdiFormat;
import app.erp.b2b.dao.entity.ErpB2bEdiLog;
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

@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpB2bEdiPosting extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpB2bEdiDocBiz ediDocBiz;

    @BeforeEach
    void setup() {
        AppConfig.getConfigProvider().assignConfigValue(ErpB2bConfigs.CONFIG_B2B_ENABLED, true);
    }

    @Test
    public void testOutboundFullChainToAcknowledged() {
        Long formatId = seedFormat("UBL_INVOICE", "UBL");
        Long docId = ormTemplate.runInSession(session -> {
            ErpB2bEdiDoc doc = createDoc(formatId, "AR_INVOICE", "INV-POST-1",
                    ErpB2bConstants.EDI_DOC_STATE_TO_SEND);
            return doc.getId();
        });

        assertEquals(ErpB2bConstants.EDI_DOC_STATE_TO_SEND,
                daoProvider.daoFor(ErpB2bEdiDoc.class).getEntityById(docId).getState());
        assertEquals(0, findLogs(docId).size());

        ErpB2bEdiDoc sent = ediDocBiz.markSent(docId, CTX);
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_SENT, sent.getState());
        assertNotNull(sent.getSentAt());
        assertEquals(1, findLogs(docId).size());

        ErpB2bEdiDoc acked = ediDocBiz.markAcknowledged(docId, CTX);
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_ACKNOWLEDGED, acked.getState());
        assertNotNull(acked.getAcknowledgedAt());
        assertEquals(2, findLogs(docId).size());

        assertTrue(findLogs(docId).stream().allMatch(l -> ErpB2bConstants.EDI_RESULT_SUCCESS.equals(l.getResultCode())));
    }

    @Test
    public void testErrorAndRetryCycle() {
        Long formatId = seedFormat("UBL_INVOICE", "UBL");
        Long docId = ormTemplate.runInSession(session -> {
            ErpB2bEdiDoc doc = createDoc(formatId, "AR_INVOICE", "INV-POST-2",
                    ErpB2bConstants.EDI_DOC_STATE_TO_SEND);
            return doc.getId();
        });

        ediDocBiz.markSent(docId, CTX);
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_SENT,
                daoProvider.daoFor(ErpB2bEdiDoc.class).getEntityById(docId).getState());

        ErpB2bEdiDoc errored = ediDocBiz.markError(docId, "network timeout", CTX);
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_ERROR, errored.getState());
        assertEquals("network timeout", errored.getError());
        assertEquals(ErpB2bConstants.BLOCKING_LEVEL_ERROR, errored.getBlockingLevel());

        ErpB2bEdiDoc retried = ediDocBiz.retry(docId, CTX);
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_TO_SEND, retried.getState());
        assertNotNull(retried.getRetryCount());
        assertTrue(retried.getRetryCount() >= 1);
        assertEquals(ErpB2bConstants.BLOCKING_LEVEL_INFO, retried.getBlockingLevel());

        ErpB2bEdiDoc resent = ediDocBiz.markSent(docId, CTX);
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_SENT, resent.getState());

        assertTrue(findLogs(docId).size() >= 4);
    }

    @Test
    public void testArchiveAfterAcknowledged() {
        Long formatId = seedFormat("UBL_INVOICE", "UBL");
        Long docId = ormTemplate.runInSession(session -> {
            ErpB2bEdiDoc doc = createDoc(formatId, "AR_INVOICE", "INV-POST-3",
                    ErpB2bConstants.EDI_DOC_STATE_TO_SEND);
            return doc.getId();
        });

        ediDocBiz.markSent(docId, CTX);
        ediDocBiz.markAcknowledged(docId, CTX);
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_ACKNOWLEDGED,
                daoProvider.daoFor(ErpB2bEdiDoc.class).getEntityById(docId).getState());

        assertThrows(NopException.class, () -> ediDocBiz.archive(docId, CTX));

        assertEquals(ErpB2bConstants.EDI_DOC_STATE_ACKNOWLEDGED,
                daoProvider.daoFor(ErpB2bEdiDoc.class).getEntityById(docId).getState());
    }

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
        doc.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        doc.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
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
