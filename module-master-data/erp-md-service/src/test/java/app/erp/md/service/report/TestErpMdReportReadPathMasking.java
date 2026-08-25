package app.erp.md.service.report;

import app.erp.common.service.MaskAuditRecorder;
import app.erp.common.service.MaskHelper;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.audit.AuditRequest;
import io.nop.api.core.audit.IAuditService;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.config.AppConfig;
import io.nop.auth.core.login.UserContextImpl;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 物料价格清单报表读取面脱敏单测（plan 2026-08-25-1956-1 Phase 3 Proof）。
 *
 * <p>范式 = {@code TestErpMfgResponseMasking}（loginAs 三态上下文）+ {@code TestMaskAuditRecorder}
 * （fake {@link IAuditService} 捕获 {@link AuditRequest}）：
 * <ol>
 *   <li>授权（采购员/管理员，与 {@code ErpMdMaterialSkuBizModel} PRICE_ROLES 同源）→ 四价格列明文 +
 *       E4.2 披露审计写入（审计载体 = 默认 SKU 实体）；</li>
 *   <li>非授权角色 → 四价格列 null，无审计；</li>
 *   <li>无用户上下文 → fail-closed null，无审计；</li>
 *   <li>无默认 SKU 行 → 价格列 null 直通且不产生伪披露审计（源实体缺席无披露行为）。</li>
 * </ol>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMdReportReadPathMasking extends JunitAutoTestCase {

    private static final io.nop.core.context.IServiceContext CTX = new io.nop.core.context.ServiceContextImpl();

    @Inject
    ErpMdReportBizModel reportBiz;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    private CapturingAuditService auditService;
    private MaskAuditRecorder fakeRecorder;
    private MaskAuditRecorder prevRecorder;
    private Object prevConfigState;
    private IUserContext prevCtx;

    @BeforeEach
    void setUpAuditCapture() {
        prevCtx = IUserContext.get();
        auditService = new CapturingAuditService();
        fakeRecorder = new MaskAuditRecorder();
        fakeRecorder.setAuditService(auditService);
        prevRecorder = MaskAuditRecorder.instance();
        fakeRecorder.init();
        prevConfigState = AppConfig.var(MaskAuditRecorder.CONFIG_FIELD_READ_AUDIT_ENABLED, Boolean.FALSE);
        AppConfig.getConfigProvider().assignConfigValue(MaskAuditRecorder.CONFIG_FIELD_READ_AUDIT_ENABLED, true);
    }

    @AfterEach
    void tearDownAuditCapture() {
        AppConfig.getConfigProvider().assignConfigValue(
                MaskAuditRecorder.CONFIG_FIELD_READ_AUDIT_ENABLED, prevConfigState);
        if (prevRecorder != null) {
            prevRecorder.init();
        } else {
            fakeRecorder.destroy();
        }
        IUserContext.set(prevCtx);
    }

    @Test
    public void authorizedPurchaserSeesPlaintextAndAuditWritten() {
        seedMaterialWithDefaultSku();
        loginAs(MaskHelper.ROLE_PURCHASER);

        Map<String, Object> row = firstRow();
        assertEquals(0, new BigDecimal("100").compareTo((BigDecimal) row.get("purchasePrice")), "采购员见 purchasePrice=100");
        assertEquals(0, new BigDecimal("200").compareTo((BigDecimal) row.get("salePrice")), "采购员见 salePrice=200");
        assertEquals(0, new BigDecimal("180").compareTo((BigDecimal) row.get("wholesalePrice")), "采购员见 wholesalePrice=180");
        assertEquals(0, new BigDecimal("220").compareTo((BigDecimal) row.get("retailPrice")), "采购员见 retailPrice=220");

        assertAuditFieldsCaptured();
    }

    @Test
    public void authorizedBizAdminSeesPlaintext() {
        seedMaterialWithDefaultSku();
        loginAs(MaskHelper.ROLE_BIZ_ADMIN);

        Map<String, Object> row = firstRow();
        assertEquals(0, new BigDecimal("100").compareTo((BigDecimal) row.get("purchasePrice")), "管理员见 purchasePrice=100");
        assertEquals(0, new BigDecimal("220").compareTo((BigDecimal) row.get("retailPrice")), "管理员见 retailPrice=220");
        assertAuditFieldsCaptured();
    }

    @Test
    public void unauthorizedRoleSeesNullNoAudit() {
        seedMaterialWithDefaultSku();
        loginAs("STAFF");

        Map<String, Object> row = firstRow();
        assertNull(row.get("purchasePrice"), "非授权 purchasePrice=null");
        assertNull(row.get("salePrice"), "非授权 salePrice=null");
        assertNull(row.get("wholesalePrice"), "非授权 wholesalePrice=null");
        assertNull(row.get("retailPrice"), "非授权 retailPrice=null");
        assertEquals("SKU-8101", row.get("skuCode"), "非授权仍见非保密列（skuCode）");

        assertTrue(auditService.captured.isEmpty(), "非授权无明文披露 = 无审计");
    }

    @Test
    public void noContextFailClosedNullNoAudit() {
        seedMaterialWithDefaultSku();
        IUserContext.set(null);

        Map<String, Object> row = firstRow();
        assertNull(row.get("purchasePrice"), "无上下文 fail-closed：purchasePrice=null");
        assertNull(row.get("salePrice"), "无上下文 fail-closed：salePrice=null");
        assertNull(row.get("wholesalePrice"), "无上下文 fail-closed：wholesalePrice=null");
        assertNull(row.get("retailPrice"), "无上下文 fail-closed：retailPrice=null");
        assertTrue(auditService.captured.isEmpty(), "无上下文无披露 = 无审计");
    }

    @Test
    public void noDefaultSkuRowNullThroughNoFakeDisclosure() {
        seedMaterialWithoutSku();
        loginAs(MaskHelper.ROLE_PURCHASER);

        Map<String, Object> row = firstRow();
        assertNull(row.get("skuCode"), "无默认 SKU 行 skuCode=null");
        assertNull(row.get("purchasePrice"), "无默认 SKU 行价格 null 直通");
        assertNull(row.get("retailPrice"), "无默认 SKU 行价格 null 直通");
        assertTrue(auditService.captured.isEmpty(), "源实体缺席无披露行为 = 无伪审计记录");
    }

    // ---------- helpers ----------

    private Map<String, Object> firstRow() {
        List<Map<String, Object>> ds = reportBiz.buildMaterialPriceListDataset(null);
        assertNotNull(ds, "数据集非 null");
        assertTrue(!ds.isEmpty(), "数据集非空");
        return ds.get(0);
    }

    private void assertAuditFieldsCaptured() {
        assertTrue(!auditService.captured.isEmpty(), "授权读取应写 E4.2 披露审计");
        boolean hasPurchase = false;
        boolean hasRetail = false;
        for (AuditRequest req : auditService.captured) {
            assertEquals(MaskAuditRecorder.OPERATION_FIELD_READ_DISCLOSURE, req.getOperation());
            assertTrue(req.getEntityId().startsWith("ErpMdMaterialSku"),
                    "审计载体 = 源 ORM 实体 ErpMdMaterialSku，实际 " + req.getEntityId());
            if (req.getRequestData().contains("\"field\":\"purchasePrice\"")) hasPurchase = true;
            if (req.getRequestData().contains("\"field\":\"retailPrice\"")) hasRetail = true;
        }
        assertTrue(hasPurchase, "审计含 purchasePrice 字段披露");
        assertTrue(hasRetail, "审计含 retailPrice 字段披露");
    }

    private void loginAs(String... roles) {
        UserContextImpl ctx = new UserContextImpl();
        ctx.setUserId("md-rpt-mask-test");
        ctx.setUserName("md-rpt-mask-test");
        ctx.setRoles(Set.of(roles));
        IUserContext.set(ctx);
    }

    private void seedMaterialWithDefaultSku() {
        ormTemplate.runInSession(() -> {
            seedMaterial(8001L, "MAT-MD-MASK");
            seedDefaultSku(8101L, 8001L);
        });
    }

    private void seedMaterialWithoutSku() {
        ormTemplate.runInSession(() -> seedMaterial(8002L, "MAT-MD-NOSKU"));
    }

    private void seedMaterial(Long id, String code) {
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial m = dao.newEntity();
        m.orm_propValueByName("id", String.valueOf(id));
        m.setCode(code);
        m.setName("物料-" + code);
        m.orm_propValueByName("materialType", "GOODS");
        m.setUoMId("1");
        m.setStatus("ACTIVE");
        m.orm_propValueByName("costMethod", "MOVING_AVERAGE");
        dao.saveEntity(m);
    }

    private void seedDefaultSku(Long id, Long materialId) {
        IEntityDao<ErpMdMaterialSku> dao = daoProvider.daoFor(ErpMdMaterialSku.class);
        ErpMdMaterialSku s = dao.newEntity();
        s.orm_propValueByName("id", String.valueOf(id));
        s.setMaterialId(String.valueOf(materialId));
        s.setSkuCode("SKU-" + id);
        s.setUoMId("1");
        s.setPurchasePrice(new BigDecimal("100"));
        s.setSalePrice(new BigDecimal("200"));
        s.setWholesalePrice(new BigDecimal("180"));
        s.setRetailPrice(new BigDecimal("220"));
        s.setIsDefault(Boolean.TRUE);
        dao.saveEntity(s);
    }

    private static class CapturingAuditService implements IAuditService {
        final List<AuditRequest> captured = new ArrayList<>();

        @Override
        public void saveAudit(AuditRequest request) {
            captured.add(request);
        }

        @Override
        public boolean isAllProcessed() {
            return true;
        }
    }
}
