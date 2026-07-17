package app.erp.b2b.service;

import app.erp.b2b.biz.IErpB2bAsnBiz;
import app.erp.b2b.dao.entity.ErpB2bAsn;
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
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpB2bAsnInventoryIntegration extends JunitAutoTestCase {

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
        AppConfig.getConfigProvider().assignConfigValue(ErpB2bConfigs.CONFIG_ASN_AUTO_CREATE_RECEIVE, true);
    }

    @Test
    public void testCreateReceiveFromMatchedAsn() {
        Long partnerId = seedPartner();
        seedPartnerProfile("PARTNER-RCV-1", partnerId, "secret-rcv-1");
        Long poId = seedPurchaseOrder("PO-TEST-001", 7001L, 7101L);

        String payload = UBL_DESPATCH_ADVICE_XML;
        String sig = hmacSha256(payload, "secret-rcv-1");

        Long asnId = ormTemplate.runInSession(session -> asnBiz.handleInboundWebhook("UBL_DESPATCH_ADVICE", "PARTNER-RCV-1",
                sig, "EVT-RCV-001", payload, CTX));
        assertNotNull(asnId);

        ErpB2bAsn matched = ormTemplate.runInSession(session -> asnBiz.matchPurchaseOrder(asnId, CTX));
        assertEquals(ErpB2bConstants.ASN_STATUS_MATCHED, matched.getStatus());

        ErpB2bAsn afterReceive = ormTemplate.runInSession(session -> asnBiz.createReceiveFromAsn(asnId, CTX));
        assertEquals(ErpB2bConstants.ASN_STATUS_RECEIVED_TO_STOCK, afterReceive.getStatus());

        List<app.erp.pur.dao.entity.ErpPurReceive> receives = findReceivesByOrderId(poId);
        assertEquals(1, receives.size());
        app.erp.pur.dao.entity.ErpPurReceive receive = receives.get(0);
        assertEquals(poId, receive.getOrderId());
        assertEquals(7001L, receive.getSupplierId());
        assertEquals(7101L, receive.getWarehouseId());
        assertNotNull(receive.getCode());
        assertTrue(receive.getCode().startsWith("RCV-FROM-ASN-"));
    }

    @Test
    public void testCreateReceiveFromUnmatchedAsnFails() {
        Long partnerId = seedPartner();
        seedPartnerProfile("PARTNER-RCV-2", partnerId, "secret-rcv-2");

        String payload = UBL_DESPATCH_ADVICE_XML;
        String sig = hmacSha256(payload, "secret-rcv-2");

        Long asnId = ormTemplate.runInSession(session -> asnBiz.handleInboundWebhook("UBL_DESPATCH_ADVICE", "PARTNER-RCV-2",
                sig, "EVT-RCV-002", payload, CTX));
        assertNotNull(asnId);

        ErpB2bAsn result = ormTemplate.runInSession(session -> asnBiz.matchPurchaseOrder(asnId, CTX));
        assertEquals(ErpB2bConstants.ASN_STATUS_RECEIVED, result.getStatus());

        assertThrows(NopException.class, () -> ormTemplate.runInSession(session -> asnBiz.createReceiveFromAsn(asnId, CTX)));

        ErpB2bAsn unchanged = daoProvider.daoFor(ErpB2bAsn.class).getEntityById(asnId);
        assertEquals(ErpB2bConstants.ASN_STATUS_RECEIVED, unchanged.getStatus());
    }

    @Test
    public void testFindUnmatchedAsns() {
        Long partnerId = seedPartner();
        seedPartnerProfile("PARTNER-RCV-3", partnerId, "secret-rcv-3");
        seedPurchaseOrder("PO-TEST-001", 7001L, 7101L);

        Long asnId1 = ormTemplate.runInSession(session -> asnBiz.handleInboundWebhook("UBL_DESPATCH_ADVICE", "PARTNER-RCV-3",
                hmacSha256(UBL_DESPATCH_ADVICE_XML, "secret-rcv-3"), "EVT-FIND-001",
                UBL_DESPATCH_ADVICE_XML, CTX));
        Long asnId2 = ormTemplate.runInSession(session -> asnBiz.handleInboundWebhook("UBL_DESPATCH_ADVICE", "PARTNER-RCV-3",
                hmacSha256(ublDespatchAdvice("PO-TEST-002"), "secret-rcv-3"), "EVT-FIND-002",
                ublDespatchAdvice("PO-TEST-002"), CTX));
        Long asnId3 = ormTemplate.runInSession(session -> asnBiz.handleInboundWebhook("UBL_DESPATCH_ADVICE", "PARTNER-RCV-3",
                hmacSha256(ublDespatchAdvice("PO-TEST-003"), "secret-rcv-3"), "EVT-FIND-003",
                ublDespatchAdvice("PO-TEST-003"), CTX));

        assertNotNull(asnId1);
        assertNotNull(asnId2);
        assertNotNull(asnId3);

        ormTemplate.runInSession(() -> asnBiz.matchPurchaseOrder(asnId1, CTX));
        assertEquals(ErpB2bConstants.ASN_STATUS_MATCHED,
                daoProvider.daoFor(ErpB2bAsn.class).getEntityById(asnId1).getStatus());
        assertEquals(ErpB2bConstants.ASN_STATUS_RECEIVED,
                daoProvider.daoFor(ErpB2bAsn.class).getEntityById(asnId2).getStatus());
        assertEquals(ErpB2bConstants.ASN_STATUS_RECEIVED,
                daoProvider.daoFor(ErpB2bAsn.class).getEntityById(asnId3).getStatus());

        List<ErpB2bAsn> unmatched = ormTemplate.runInSession(session -> asnBiz.findUnmatchedAsns(null, CTX));
        assertEquals(2, unmatched.size());
        assertTrue(unmatched.stream().noneMatch(a -> a.getId().equals(asnId1)));
    }

    private String ublDespatchAdvice(String poCode) {
        return "<DespatchAdvice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:DespatchAdvice-2\"" +
               " xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\"" +
               " xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">" +
               "  <cac:DespatchSupplierParty>" +
               "    <cbc:CustomerAssignedAccountID>SUP-001</cbc:CustomerAssignedAccountID>" +
               "  </cac:DespatchSupplierParty>" +
               "  <cac:OrderReference>" +
               "    <cbc:ID>" + poCode + "</cbc:ID>" +
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
    }

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

    private Long seedPurchaseOrder(String code, Long supplierId, Long warehouseId) {
        return ormTemplate.runInSession(session -> {
            app.erp.pur.dao.entity.ErpPurOrder po = new app.erp.pur.dao.entity.ErpPurOrder();
            po.setCode(code);
            po.setSupplierId(supplierId);
            po.setWarehouseId(warehouseId);
            po.setBusinessDate(LocalDate.of(2026, 7, 1));
            po.setCurrencyId(6701L);
            po.setDocStatus("APPROVED");
            po.setApproveStatus("APPROVED");
            daoProvider.daoFor(app.erp.pur.dao.entity.ErpPurOrder.class).saveEntity(po);
            return po.getId();
        });
    }

    @SuppressWarnings("unchecked")
    private List<app.erp.pur.dao.entity.ErpPurReceive> findReceivesByOrderId(Long orderId) {
        IEntityDao<app.erp.pur.dao.entity.ErpPurReceive> dao =
                daoProvider.daoFor(app.erp.pur.dao.entity.ErpPurReceive.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orderId", orderId));
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
