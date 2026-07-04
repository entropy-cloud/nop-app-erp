package app.erp.b2b.service;

import app.erp.b2b.biz.IErpB2bEdiDocBiz;
import app.erp.b2b.dao.entity.ErpB2bEdiDoc;
import app.erp.b2b.dao.entity.ErpB2bEdiFormat;
import app.erp.b2b.dao.entity.ErpB2bMftConfig;
import app.erp.b2b.dao.entity.ErpB2bMftLog;
import app.erp.b2b.service.spi.transport.ErpB2bMftTransportRegistry;
import app.erp.b2b.service.spi.transport.TransportManager;
import app.erp.b2b.service.spi.transport.mock.MockTransportAdapter;
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
 * MFT 传输层行为测试（Phase 5）。覆盖：
 * <ul>
 *   <li>MockTransportAdapter 全链：EdiDoc TO_SEND→SENT、MftLog SENT 记录 messageId/fileHash/durationMs；</li>
 *   <li>5xx 重试至死信 DEAD_LETTER；4xx 不重试直接死信；</li>
 *   <li>缺配置抛 ERR_B2B_MFT_CONFIG_MISSING；未注册协议抛 ERR_B2B_MFT_ADAPTER_NOT_REGISTERED。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpB2bMftTransport extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    TransportManager transportManager;
    @Inject
    ErpB2bMftTransportRegistry transportRegistry;
    @Inject
    IErpB2bEdiDocBiz ediDocBiz;

    @BeforeEach
    void setup() {
        AppConfig.getConfigProvider().assignConfigValue(ErpB2bConfigs.CONFIG_MFT_MAX_RETRIES, 2);
        MockTransportAdapter.failureMode = MockTransportAdapter.FAILURE_MODE_SUCCESS;
    }

    @Test
    public void testTransportSuccess() {
        Long partnerId = seedPartner();
        Long configId = seedMftConfig(partnerId, "HTTPS", true);
        Long formatId = seedFormat("UBL_INVOICE", "UBL");
        Long ediDocId = ormTemplate.runInSession(session -> {
            ErpB2bEdiDoc doc = new ErpB2bEdiDoc();
            doc.setCode("EDI-MFT-1-" + System.nanoTime());
            doc.setFormatId(formatId);
            doc.setRelatedBillType("AR_INVOICE");
            doc.setRelatedBillCode("INV-MFT-1");
            doc.setState(ErpB2bConstants.EDI_DOC_STATE_TO_SEND);
            doc.setBlockingLevel(ErpB2bConstants.BLOCKING_LEVEL_INFO);
            doc.setRetryCount(0);
            daoProvider.daoFor(ErpB2bEdiDoc.class).saveEntity(doc);
            return doc.getId();
        });

        boolean success = ormTemplate.runInSession(session ->
                transportManager.send(ediDocId, partnerId, "<invoice/>", "test.xml"));
        assertTrue(success, "传输应成功");

        ErpB2bEdiDoc doc = ormTemplate.runInSession(session ->
                daoProvider.daoFor(ErpB2bEdiDoc.class).getEntityById(ediDocId));
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_SENT, doc.getState(),
                "EdiDoc 应迁移到 SENT");

        List<ErpB2bMftLog> logs = findMftLogs(configId);
        assertTrue(logs.stream().anyMatch(l -> ErpB2bConstants.MFT_STATUS_SENT.equals(l.getStatus())),
                "应有 SENT 状态的 MftLog");
        ErpB2bMftLog sentLog = logs.stream()
                .filter(l -> ErpB2bConstants.MFT_STATUS_SENT.equals(l.getStatus()))
                .findFirst().orElse(null);
        assertNotNull(sentLog);
        assertNotNull(sentLog.getMessageId(), "SENT log 应记录 messageId");
        assertNotNull(sentLog.getFileHash(), "SENT log 应记录 fileHash");
        assertNotNull(sentLog.getDurationMs(), "SENT log 应记录 durationMs");
    }

    @Test
    public void testTransport5xxRetryDeadLetter() {
        Long partnerId = seedPartner();
        Long configId = seedMftConfig(partnerId, "HTTPS", true);
        Long formatId = seedFormat("UBL_INVOICE", "UBL");
        Long ediDocId = ormTemplate.runInSession(session -> {
            ErpB2bEdiDoc doc = new ErpB2bEdiDoc();
            doc.setCode("EDI-MFT-2-" + System.nanoTime());
            doc.setFormatId(formatId);
            doc.setRelatedBillType("AR_INVOICE");
            doc.setRelatedBillCode("INV-MFT-2");
            doc.setState(ErpB2bConstants.EDI_DOC_STATE_TO_SEND);
            doc.setBlockingLevel(ErpB2bConstants.BLOCKING_LEVEL_INFO);
            doc.setRetryCount(0);
            daoProvider.daoFor(ErpB2bEdiDoc.class).saveEntity(doc);
            return doc.getId();
        });

        MockTransportAdapter.failureMode = MockTransportAdapter.FAILURE_MODE_5XX;
        boolean success = ormTemplate.runInSession(session ->
                transportManager.send(ediDocId, partnerId, "<invoice/>", "test.xml"));
        assertTrue(!success, "5xx 重试耗尽应返回 false");

        List<ErpB2bMftLog> logs = ormTemplate.runInSession(session -> findMftLogs(configId));
        assertTrue(logs.stream().anyMatch(l -> ErpB2bConstants.MFT_STATUS_DEAD_LETTER.equals(l.getStatus())),
                "5xx 重试耗尽应有 DEAD_LETTER 日志");
    }

    @Test
    public void testTransport4xxNoRetryDeadLetter() {
        Long partnerId = seedPartner();
        Long configId = seedMftConfig(partnerId, "HTTPS", true);
        Long formatId = seedFormat("UBL_INVOICE", "UBL");
        Long ediDocId = ormTemplate.runInSession(session -> {
            ErpB2bEdiDoc doc = new ErpB2bEdiDoc();
            doc.setCode("EDI-MFT-3-" + System.nanoTime());
            doc.setFormatId(formatId);
            doc.setRelatedBillType("AR_INVOICE");
            doc.setRelatedBillCode("INV-MFT-3");
            doc.setState(ErpB2bConstants.EDI_DOC_STATE_TO_SEND);
            doc.setBlockingLevel(ErpB2bConstants.BLOCKING_LEVEL_INFO);
            doc.setRetryCount(0);
            daoProvider.daoFor(ErpB2bEdiDoc.class).saveEntity(doc);
            return doc.getId();
        });

        MockTransportAdapter.failureMode = MockTransportAdapter.FAILURE_MODE_4XX;
        boolean success = ormTemplate.runInSession(session ->
                transportManager.send(ediDocId, partnerId, "<invoice/>", "test.xml"));
        assertTrue(!success, "4xx 不重试应返回 false");

        List<ErpB2bMftLog> logs = ormTemplate.runInSession(session -> findMftLogs(configId));
        assertTrue(logs.stream().anyMatch(l -> ErpB2bConstants.MFT_STATUS_DEAD_LETTER.equals(l.getStatus())
                || ErpB2bConstants.MFT_STATUS_FAILED.equals(l.getStatus())),
                "4xx 应有 DEAD_LETTER 或 FAILED 日志");
    }

    @Test
    public void testTransportConfigMissing() {
        Long partnerId = seedPartner();
        assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session ->
                        transportManager.send(null, partnerId, "<payload/>", "test.xml")),
                "缺 MftConfig 应抛 ERR_B2B_MFT_CONFIG_MISSING");
    }

    @Test
    public void testTransportAdapterNotRegistered() {
        List<String> protocols = transportRegistry.getRegisteredProtocols();
        assertTrue(protocols.contains("HTTPS"), "Registry 应包含 HTTPS Mock 适配器");
        assertThrows(NopException.class,
                () -> transportRegistry.getAdapter("SFTP"),
                "未注册协议 SFTP 应抛 ERR_B2B_MFT_ADAPTER_NOT_REGISTERED");
    }

    // ---------- helpers ----------

    private Long seedPartner() {
        return ormTemplate.runInSession(session -> {
            app.erp.md.dao.entity.ErpMdPartner partner = new app.erp.md.dao.entity.ErpMdPartner();
            partner.setCode("PARTNER-MFT-" + System.nanoTime());
            partner.setName("测试伙伴");
            partner.setPartnerType("VENDOR");
            partner.setStatus("ACTIVE");
            daoProvider.daoFor(app.erp.md.dao.entity.ErpMdPartner.class).saveEntity(partner);
            return partner.getId();
        });
    }

    private Long seedMftConfig(Long partnerId, String protocol, boolean deadLetterEnabled) {
        return ormTemplate.runInSession(session -> {
            ErpB2bMftConfig config = new ErpB2bMftConfig();
            config.setPartnerId(partnerId);
            config.setProtocol(protocol);
            config.setTransportEndpoint("https://mock.endpoint/as2");
            config.setActive(true);
            config.setMaxRetries(3);
            config.setRetryIntervalMin(1);
            config.setDeadLetterEnabled(deadLetterEnabled);
            daoProvider.daoFor(ErpB2bMftConfig.class).saveEntity(config);
            return config.getId();
        });
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

    @SuppressWarnings("unchecked")
    private List<ErpB2bMftLog> findMftLogs(Long configId) {
        IEntityDao<ErpB2bMftLog> dao = daoProvider.daoFor(ErpB2bMftLog.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("configId", configId));
        return dao.findAllByQuery(q);
    }
}
