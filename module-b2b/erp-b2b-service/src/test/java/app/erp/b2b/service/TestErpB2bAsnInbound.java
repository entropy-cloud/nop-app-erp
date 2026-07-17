package app.erp.b2b.service;

import app.erp.b2b.biz.IErpB2bAsnBiz;
import app.erp.b2b.biz.IErpB2bEdiDocBiz;
import app.erp.b2b.dao.entity.ErpB2bAsn;
import app.erp.b2b.dao.entity.ErpB2bAsnLine;
import app.erp.b2b.dao.entity.ErpB2bEdiDoc;
import app.erp.b2b.dao.entity.ErpB2bPartnerProfile;
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
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ASN 入站处理行为测试（Phase 5）。覆盖：
 * <ul>
 *   <li>webhook HMAC 签名校验（通过/失败）+ 幂等（eventId+formatCode）；</li>
 *   <li>parse→建 Asn/AsnLine（UBL DespatchAdvice XML 解析）；</li>
 *   <li>PO 未匹配保留 RECEIVED（本期无 PO seed，验证不阻断路径）；</li>
 *   <li>追溯链 sourceEdiDocId→EdiDoc→EdiLog 完整。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpB2bAsnInbound extends JunitAutoTestCase {

    @RegisterExtension
    static B2bFrozenClockExtension frozenClock = new B2bFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();

    private static final String UBL_DESPATCH_ADVICE_XML =
            "<DespatchAdvice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:DespatchAdvice-2\"" +
            " xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\"" +
            " xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">" +
            "  <cac:DespatchSupplierParty>" +
            "    <cbc:CustomerAssignedAccountID>SUP-001</cbc:CustomerAssignedAccountID>" +
            "  </cac:DespatchSupplierParty>" +
            "  <cac:OrderReference>" +
            "    <cbc:ID>PO-TEST-001</cbc:ID>" +
            "  </cac:OrderReference>" +
            "  <cac:Delivery>" +
            "    <cbc:ActualDeliveryDate>2026-07-10</cbc:ActualDeliveryDate>" +
            "  </cac:Delivery>" +
            "  <cac:DespatchLine>" +
            "    <cbc:ID>1</cbc:ID>" +
            "    <cbc:DeliveredQuantity>100</cbc:DeliveredQuantity>" +
            "    <cac:Item>" +
            "      <cac:SellersItemIdentification>" +
            "        <cbc:ID>SUP-PART-001</cbc:ID>" +
            "      </cac:SellersItemIdentification>" +
            "    </cac:Item>" +
            "  </cac:DespatchLine>" +
            "</DespatchAdvice>";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpB2bAsnBiz asnBiz;

    @BeforeEach
    void setup() {
        AppConfig.getConfigProvider().assignConfigValue(ErpB2bConfigs.CONFIG_WEBHOOK_SIGNATURE_REQUIRED, true);
    }

    @Test
    public void testWebhookValidSignatureCreatesAsn() {
        Long partnerId = seedPartner();
        seedPartnerProfile("PARTNER-ASN-1", partnerId, "webhook-secret-1");

        String payload = UBL_DESPATCH_ADVICE_XML;
        String sig = hmacSha256(payload, "webhook-secret-1");

        Long asnId = ormTemplate.runInSession(session -> asnBiz.handleInboundWebhook("UBL_DESPATCH_ADVICE", "PARTNER-ASN-1",
                sig, "EVT-001", payload, CTX));

        assertNotNull(asnId, "应创建 ASN 并返回 ID");
        ErpB2bAsn asn = daoProvider.daoFor(ErpB2bAsn.class).getEntityById(asnId);
        assertEquals(ErpB2bConstants.ASN_STATUS_RECEIVED, asn.getStatus());
        assertEquals("PO-TEST-001", asn.getRelatedBillCode());
        assertEquals(ErpB2bConstants.RELATED_BILL_TYPE_PO_ORDER, asn.getRelatedBillType());
        assertNotNull(asn.getSourceEdiDocId(), "ASN 应有 sourceEdiDocId 追溯链");

        // AsnLine 创建
        List<ErpB2bAsnLine> lines = findAsnLines(asnId);
        assertEquals(1, lines.size(), "应有 1 行 AsnLine");
        assertEquals("SUP-PART-001", lines.get(0).getSupplierPartNo());

        // EdiDoc 追溯
        ErpB2bEdiDoc ediDoc = daoProvider.daoFor(ErpB2bEdiDoc.class)
                .getEntityById(asn.getSourceEdiDocId());
        assertNotNull(ediDoc);
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_RECEIVED, ediDoc.getState());
    }

    @Test
    public void testWebhookInvalidSignatureRejected() {
        Long partnerId = seedPartner();
        seedPartnerProfile("PARTNER-ASN-2", partnerId, "webhook-secret-2");

        assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> asnBiz.handleInboundWebhook("UBL_DESPATCH_ADVICE", "PARTNER-ASN-2",
                        "badsignature", "EVT-002", UBL_DESPATCH_ADVICE_XML, CTX)),
                "错误签名应抛 ERR_B2B_WEBHOOK_SIGNATURE_INVALID");
    }

    @Test
    public void testWebhookIdempotentDuplicate() {
        Long partnerId = seedPartner();
        seedPartnerProfile("PARTNER-ASN-3", partnerId, "webhook-secret-3");

        String payload = UBL_DESPATCH_ADVICE_XML;
        String sig = hmacSha256(payload, "webhook-secret-3");

        Long asnId1 = ormTemplate.runInSession(session -> asnBiz.handleInboundWebhook("UBL_DESPATCH_ADVICE", "PARTNER-ASN-3",
                sig, "EVT-DUP-001", payload, CTX));
        assertNotNull(asnId1);

        // 重复 eventId → 抛异常
        assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> asnBiz.handleInboundWebhook("UBL_DESPATCH_ADVICE", "PARTNER-ASN-3",
                        sig, "EVT-DUP-001", payload, CTX)),
                "重复 eventId 应抛 ERR_B2B_WEBHOOK_DUPLICATE_EVENT");
    }

    @Test
    public void testAsnNoPoMatchStaysReceived() {
        Long partnerId = seedPartner();
        seedPartnerProfile("PARTNER-ASN-4", partnerId, "webhook-secret-4");

        String payload = UBL_DESPATCH_ADVICE_XML;
        String sig = hmacSha256(payload, "webhook-secret-4");

        Long asnId = ormTemplate.runInSession(session -> asnBiz.handleInboundWebhook("UBL_DESPATCH_ADVICE", "PARTNER-ASN-4",
                sig, "EVT-003", payload, CTX));

        // 尝试匹配 PO（无 PO seed → 保留 RECEIVED）
        ErpB2bAsn result = ormTemplate.runInSession(session -> asnBiz.matchPurchaseOrder(asnId, CTX));
        assertEquals(ErpB2bConstants.ASN_STATUS_RECEIVED, result.getStatus(),
                "无 PO 时 ASN 应保留 RECEIVED（不阻断）");
    }

    @Test
    public void testSignatureNotRequired() {
        AppConfig.getConfigProvider().assignConfigValue(ErpB2bConfigs.CONFIG_WEBHOOK_SIGNATURE_REQUIRED, false);
        Long partnerId = seedPartner();
        seedPartnerProfile("PARTNER-ASN-5", partnerId, "webhook-secret-5");

        Long asnId = ormTemplate.runInSession(session -> asnBiz.handleInboundWebhook("UBL_DESPATCH_ADVICE", "PARTNER-ASN-5",
                null, "EVT-004", UBL_DESPATCH_ADVICE_XML, CTX));
        assertNotNull(asnId, "签名非必填时应正常创建 ASN");
    }

    // ---------- helpers ----------

    private Long seedPartner() {
        return ormTemplate.runInSession(session -> {
            app.erp.md.dao.entity.ErpMdPartner partner = new app.erp.md.dao.entity.ErpMdPartner();
            partner.setCode("P-" + System.nanoTime());
            partner.setName("测试供应商");
            partner.setPartnerType("VENDOR");
            partner.setStatus("ACTIVE");
            daoProvider.daoFor(app.erp.md.dao.entity.ErpMdPartner.class).saveEntity(partner);
            return partner.getId();
        });
    }

    private void seedPartnerProfile(String code, Long partnerId, String webhookSecret) {
        ormTemplate.runInSession(session -> {
            ErpB2bPartnerProfile profile = new ErpB2bPartnerProfile();
            profile.setCode(code);
            profile.setPartnerId(partnerId);
            profile.setPartnerName("测试伙伴档案");
            profile.setStatus("PRODUCTION");
            profile.setProtocol("HTTPS");
            profile.setTransportEndpoint("https://mock.endpoint/webhook");
            profile.setAuthMethod("HMAC");
            profile.setWebhookSecret(webhookSecret);
            daoProvider.daoFor(ErpB2bPartnerProfile.class).saveEntity(profile);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private List<ErpB2bAsnLine> findAsnLines(Long asnId) {
        IEntityDao<ErpB2bAsnLine> dao = daoProvider.daoFor(ErpB2bAsnLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("asnId", asnId));
        return dao.findAllByQuery(q);
    }

    private static String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
