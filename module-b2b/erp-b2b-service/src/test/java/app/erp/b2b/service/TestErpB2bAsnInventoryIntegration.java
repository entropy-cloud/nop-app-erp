package app.erp.b2b.service;

import app.erp.b2b.biz.IErpB2bAsnBiz;
import app.erp.b2b.dao.entity.ErpB2bAsn;
import app.erp.b2b.dao.entity.ErpB2bAsnLine;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        // 显式 seed Material（uoMId=1）：行级回填 uoMId 反查前置（不依赖跨模块 _init-data）
        Long materialId = seedMaterial(1L, "MAT-RCV-1");
        Long poId = seedPurchaseOrder("PO-TEST-001", 7001L, 7101L);
        // PO 行补齐：materialId → createReceiveFromAsn 行级回填可反查 unitPrice/taxRate/orderLineId
        seedPurchaseOrderLine(poId, 1, materialId, new BigDecimal("5"), new BigDecimal("10"));

        String payload = UBL_DESPATCH_ADVICE_XML;
        String sig = hmacSha256(payload, "secret-rcv-1");

        Long asnId = ormTemplate.runInSession(session -> asnBiz.handleInboundWebhook("UBL_DESPATCH_ADVICE", "PARTNER-RCV-1",
                sig, "EVT-RCV-001", payload, CTX));
        assertNotNull(asnId);

        // webhook 解析 AsnLine 时仅置 supplierPartNo，未置 materialId（parseToAsn 已知 gap）。
        // 后置回填 AsnLine.materialId 模拟 matchPurchaseOrder 已建立 ASN↔PO materialId 关联的语义。
        fixAsnLineMaterialId(asnId, materialId);

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

        // 行级回填断言：1 行 ReceiveLine（plan 2026-07-19-0849-1 Phase 2）
        List<app.erp.pur.dao.entity.ErpPurReceiveLine> lines = findReceiveLinesByReceiveId(receive.getId());
        assertEquals(1, lines.size(), "createReceiveFromAsn 行级回填应产 1 ReceiveLine");
        app.erp.pur.dao.entity.ErpPurReceiveLine line = lines.get(0);
        assertEquals(materialId, line.getMaterialId(), "ReceiveLine.materialId 透传 AsnLine.materialId");
        assertEquals(1L, line.getUoMId(), "ReceiveLine.uoMId 反查 ErpMdMaterial.uoMId");
        assertEquals(Integer.valueOf(1), line.getLineNo(), "ReceiveLine.lineNo 透传 AsnLine.lineNo");
        assertEquals(0, new BigDecimal("100").compareTo(line.getQuantity()), "ReceiveLine.quantity=AsnLine.shippedQty");
        assertEquals(0, new BigDecimal("5").compareTo(line.getUnitPrice()), "ReceiveLine.unitPrice 反查 PO line");
        assertEquals(0, new BigDecimal("500").compareTo(line.getAmount()), "ReceiveLine.amount=unitPrice×qty");
        assertEquals(receive.getWarehouseId(), line.getWarehouseId(), "ReceiveLine.warehouseId=receive.warehouseId");
    }

    @Test
    public void testCreateReceiveFromAsnMultiLineMapping() {
        Long partnerId = seedPartner();
        seedPartnerProfile("PARTNER-RCV-ML", partnerId, "secret-ml");
        // 显式 seed 2 Material（行级回填 uoMId 反查前置，独立不依赖跨模块 _init-data）
        Long matId1 = seedMaterial(1L, "MAT-ML-1");
        Long matId2 = seedMaterial(1L, "MAT-ML-2");
        Long poId = seedPurchaseOrder("PO-TEST-ML-001", 7002L, 7102L);
        // 多行 PO：mat1 unitPrice=5 / mat2 unitPrice=12
        seedPurchaseOrderLine(poId, 1, matId1, new BigDecimal("5"), new BigDecimal("20"));
        seedPurchaseOrderLine(poId, 2, matId2, new BigDecimal("12"), new BigDecimal("10"));

        // 直接 seed ASN（绕过 webhook）+ 2 AsnLine（materialId 各异，对齐 Phase 1 Proof materialId 反查语义）
        Long asnId = seedMatchedAsnDirectly("PO-TEST-ML-001", partnerId, "ASN-ML-" + System.nanoTime());
        seedAsnLine(asnId, 1, matId1, new BigDecimal("15"), new BigDecimal("15"));
        seedAsnLine(asnId, 2, matId2, new BigDecimal("8"), new BigDecimal("8"));

        ErpB2bAsn afterReceive = ormTemplate.runInSession(session -> asnBiz.createReceiveFromAsn(asnId, CTX));
        assertEquals(ErpB2bConstants.ASN_STATUS_RECEIVED_TO_STOCK, afterReceive.getStatus());

        List<app.erp.pur.dao.entity.ErpPurReceive> receives = findReceivesByOrderId(poId);
        assertEquals(1, receives.size());
        Long receiveId = receives.get(0).getId();

        List<app.erp.pur.dao.entity.ErpPurReceiveLine> lines = findReceiveLinesByReceiveId(receiveId);
        assertEquals(2, lines.size(), "多行 AsnLine 应产 2 ReceiveLine（行级回填完整）");

        // 逐行断言：lineNo 透传 + materialId 透传 + uoMId 反查 + amount 派生（plan Phase 1 Decision (a)-(f)）
        app.erp.pur.dao.entity.ErpPurReceiveLine l1 = lines.stream()
                .filter(l -> l.getLineNo() == 1).findFirst().orElseThrow();
        assertEquals(matId1, l1.getMaterialId());
        assertEquals(1L, l1.getUoMId(), "MAT-ML-1.uoMId=1 反查");
        assertEquals(0, new BigDecimal("15").compareTo(l1.getQuantity()));
        assertEquals(0, new BigDecimal("5").compareTo(l1.getUnitPrice()));
        assertEquals(0, new BigDecimal("75").compareTo(l1.getAmount()), "amount=5×15=75 HALF_UP scale=4");

        app.erp.pur.dao.entity.ErpPurReceiveLine l2 = lines.stream()
                .filter(l -> l.getLineNo() == 2).findFirst().orElseThrow();
        assertEquals(matId2, l2.getMaterialId());
        assertEquals(1L, l2.getUoMId(), "MAT-ML-2.uoMId=1 反查");
        assertEquals(0, new BigDecimal("8").compareTo(l2.getQuantity()));
        assertEquals(0, new BigDecimal("12").compareTo(l2.getUnitPrice()));
        assertEquals(0, new BigDecimal("96").compareTo(l2.getAmount()), "amount=12×8=96");
    }

    @Test
    public void testCreateReceiveFromAsnEmptyLines() {
        Long partnerId = seedPartner();
        seedPartnerProfile("PARTNER-RCV-EMPTY", partnerId, "secret-empty");
        Long poId = seedPurchaseOrder("PO-TEST-EMPTY-001", 7003L, 7103L);

        // 直接 seed ASN（无 AsnLine）+ 状态直置 MATCHED（Phase 1 Decision (e) 0 行合法边界）
        Long asnId = seedMatchedAsnDirectly("PO-TEST-EMPTY-001", partnerId, "ASN-EMPTY-" + System.nanoTime());

        ErpB2bAsn afterReceive = ormTemplate.runInSession(session -> asnBiz.createReceiveFromAsn(asnId, CTX));
        assertEquals(ErpB2bConstants.ASN_STATUS_RECEIVED_TO_STOCK, afterReceive.getStatus(),
                "空白 AsnLine：Receive 头仍创建 + ASN RECEIVED_TO_STOCK（Decision (e)①）");

        List<app.erp.pur.dao.entity.ErpPurReceive> receives = findReceivesByOrderId(poId);
        assertEquals(1, receives.size(), "Receive 头创建不阻塞");
        List<app.erp.pur.dao.entity.ErpPurReceiveLine> lines = findReceiveLinesByReceiveId(receives.get(0).getId());
        assertEquals(0, lines.size(), "0 AsnLine → 0 ReceiveLine");
    }

    @Test
    public void testCreateReceiveFromAsnConfigGateDisabled() {
        // 临时关闭 config-gate（Decision Phase 1 plan l.191-194 默认 false → null 返回守卫）
        AppConfig.getConfigProvider().assignConfigValue(ErpB2bConfigs.CONFIG_ASN_AUTO_CREATE_RECEIVE, false);
        try {
            Long partnerId = seedPartner();
            seedPartnerProfile("PARTNER-RCV-GATE", partnerId, "secret-gate");
            Long poId = seedPurchaseOrder("PO-TEST-GATE-001", 7004L, 7104L);
            Long asnId = seedMatchedAsnDirectly("PO-TEST-GATE-001", partnerId, "ASN-GATE-" + System.nanoTime());

            ErpB2bAsn result = ormTemplate.runInSession(session -> asnBiz.createReceiveFromAsn(asnId, CTX));
            assertNull(result, "config-gated 关闭路径：返回 null 跳过");

            // ASN 状态不迁移；Receive 不创建
            ErpB2bAsn unchanged = daoProvider.daoFor(ErpB2bAsn.class).getEntityById(asnId);
            assertEquals(ErpB2bConstants.ASN_STATUS_MATCHED, unchanged.getStatus(),
                    "config-gated 关闭：ASN 状态保持 MATCHED");
            assertEquals(0, findReceivesByOrderId(poId).size(), "config-gated 关闭：不建 Receive");
        } finally {
            // 恢复全局 config 默认（BeforeEach 设 true），避免污染后续测试
            AppConfig.getConfigProvider().assignConfigValue(ErpB2bConfigs.CONFIG_ASN_AUTO_CREATE_RECEIVE, true);
        }
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

    private void seedPurchaseOrderLine(Long orderId, int lineNo, Long materialId,
                                       BigDecimal unitPrice, BigDecimal quantity) {
        ormTemplate.runInSession(session -> {
            app.erp.pur.dao.entity.ErpPurOrderLine line = new app.erp.pur.dao.entity.ErpPurOrderLine();
            line.setOrderId(orderId);
            line.setLineNo(lineNo);
            line.setMaterialId(materialId);
            line.setUoMId(1L);
            line.setQuantity(quantity);
            line.setUnitPrice(unitPrice);
            line.setAmount(unitPrice.multiply(quantity));
            daoProvider.daoFor(app.erp.pur.dao.entity.ErpPurOrderLine.class).saveEntity(line);
            return null;
        });
    }

    /**
     * Seed ErpMdMaterial（含 mandatory uoMId）— createReceiveFromAsn 行级回填 uoMId 反查前置。
     * 不与全局 _init-data/erp_md_material.csv 强耦合，独立 seed 避免依赖跨模块资源加载。
     */
    private Long seedMaterial(Long uoMId, String code) {
        return ormTemplate.runInSession(session -> {
            app.erp.md.dao.entity.ErpMdMaterial material = new app.erp.md.dao.entity.ErpMdMaterial();
            material.setCode(code);
            material.setName("测试物料-" + code);
            material.setMaterialType("FINISHED_PRODUCT");
            material.setUoMId(uoMId);
            material.setStatus("ACTIVE");
            daoProvider.daoFor(app.erp.md.dao.entity.ErpMdMaterial.class).saveEntity(material);
            return material.getId();
        });
    }

    /**
     * 直接 seed ASN（绕过 handleInboundWebhook）并置 status=MATCHED，便于 createReceiveFromAsn 行级回填路径
     * 独立测试（不被 webhook 解析路径耦合 AsnLine.materialId=null 的 gap 约束）。
     */
    private Long seedMatchedAsnDirectly(String poCode, Long partnerId, String asnCode) {
        return ormTemplate.runInSession(session -> {
            ErpB2bAsn asn = new ErpB2bAsn();
            asn.setCode(asnCode);
            asn.setPartnerId(partnerId);
            asn.setRelatedBillType(ErpB2bConstants.RELATED_BILL_TYPE_PO_ORDER);
            asn.setRelatedBillCode(poCode);
            asn.setStatus(ErpB2bConstants.ASN_STATUS_MATCHED);
            asn.setBusinessDate(LocalDate.of(2026, 7, 1));
            asn.setShipmentDate(LocalDate.of(2026, 7, 1));
            daoProvider.daoFor(ErpB2bAsn.class).saveEntity(asn);
            return asn.getId();
        });
    }

    private void seedAsnLine(Long asnId, int lineNo, Long materialId,
                             BigDecimal shippedQty, BigDecimal quantity) {
        ormTemplate.runInSession(session -> {
            ErpB2bAsnLine line = new ErpB2bAsnLine();
            line.setAsnId(asnId);
            line.setLineNo(lineNo);
            line.setMaterialId(materialId);
            line.setShippedQty(shippedQty);
            line.setQuantity(quantity);
            daoProvider.daoFor(ErpB2bAsnLine.class).saveEntity(line);
            return null;
        });
    }

    private void fixAsnLineMaterialId(Long asnId, Long materialId) {
        ormTemplate.runInSession(session -> {
            QueryBean q = new QueryBean();
            q.addFilter(eq("asnId", asnId));
            List<ErpB2bAsnLine> lines = daoProvider.daoFor(ErpB2bAsnLine.class).findAllByQuery(q);
            for (ErpB2bAsnLine line : lines) {
                line.setMaterialId(materialId);
                daoProvider.daoFor(ErpB2bAsnLine.class).saveOrUpdateEntity(line);
            }
            return null;
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

    @SuppressWarnings("unchecked")
    private List<app.erp.pur.dao.entity.ErpPurReceiveLine> findReceiveLinesByReceiveId(Long receiveId) {
        IEntityDao<app.erp.pur.dao.entity.ErpPurReceiveLine> dao =
                daoProvider.daoFor(app.erp.pur.dao.entity.ErpPurReceiveLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("receiveId", receiveId));
        q.addOrderField("lineNo", false);
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
