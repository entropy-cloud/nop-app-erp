package app.erp.md.service.report;

import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.WebContentBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主数据域报表渲染端到端测试（plan 2026-07-06-1815-1 Phase 3 Proof）。
 *
 * <p>覆盖两张主数据报表（物料价格清单 / 往来单位清单）的 {@code renderHtml}/{@code download(xlsx|pdf)}
 * 渲染管线、数据集口径断言、空数据集不报错、以及路径注入防护（非法 reportName 抛 {@code ERR_REPORT_NAME_INVALID}）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMdReportRendering extends JunitAutoTestCase {

    private static final io.nop.core.context.IServiceContext CTX = new io.nop.core.context.ServiceContextImpl();

    @Inject
    ErpMdReportBizModel reportBiz;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    // ===================== Phase 3: 物料价格清单报表 =====================

    @Test
    public void testMaterialPriceListRenderHtml() {
        seedMaterialBaseline();
        String html = reportBiz.renderHtml("material-price-list", null, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
        assertTrue(html.contains("MAT-MD-RPT"), "renderHtml 含物料编码");
    }

    @Test
    public void testMaterialPriceListDownloadXlsxAndPdf() {
        seedMaterialBaseline();
        for (String renderType : new String[]{"xlsx", "pdf"}) {
            WebContentBean bean = reportBiz.download("material-price-list", renderType, null, CTX);
            assertNotNull(bean, "download 非空: " + renderType);
            Object content = bean.getContent();
            assertNotNull(content, "download 内容非空: " + renderType);
            assertTrue(content instanceof java.io.File, "download 返回 File: " + renderType);
            java.io.File f = (java.io.File) content;
            assertTrue(f.exists() && f.length() > 0, "download 文件有效: " + renderType);
            f.delete();
        }
    }

    @Test
    public void testMaterialPriceListDataset() {
        seedMaterialBaseline();
        List<Map<String, Object>> ds = reportBiz.buildMaterialPriceListDataset(null);
        assertFalse(ds.isEmpty(), "物料价格清单数据集非空");
        Map<String, Object> row = ds.get(0);
        // 默认 SKU 四档价格：100/200/180/220
        assertEquals(0, bd("100").compareTo(toBd(row.get("purchasePrice"))), "purchasePrice=100");
        assertEquals(0, bd("200").compareTo(toBd(row.get("salePrice"))), "salePrice=200");
        assertEquals(0, bd("180").compareTo(toBd(row.get("wholesalePrice"))), "wholesalePrice=180");
        assertEquals(0, bd("220").compareTo(toBd(row.get("retailPrice"))), "retailPrice=220");
    }

    @Test
    public void testMaterialPriceListEmptyDatasetNoError() {
        List<Map<String, Object>> ds = reportBiz.buildMaterialPriceListDataset(null);
        assertNotNull(ds, "空数据集不报错");
        assertTrue(ds.isEmpty(), "无物料记录 → 空列表");
    }

    // ===================== Phase 3: 往来单位清单报表 =====================

    @Test
    public void testPartnerListRenderHtml() {
        seedPartnerBaseline();
        String html = reportBiz.renderHtml("partner-list", null, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
        assertTrue(html.contains("P-MD-RPT"), "renderHtml 含往来单位编码");
    }

    @Test
    public void testPartnerListDownloadXlsxAndPdf() {
        seedPartnerBaseline();
        for (String renderType : new String[]{"xlsx", "pdf"}) {
            WebContentBean bean = reportBiz.download("partner-list", renderType, null, CTX);
            assertNotNull(bean, "download 非空: " + renderType);
            Object content = bean.getContent();
            assertTrue(content instanceof java.io.File, "download 返回 File: " + renderType);
            java.io.File f = (java.io.File) content;
            assertTrue(f.exists() && f.length() > 0, "download 文件有效: " + renderType);
            f.delete();
        }
    }

    @Test
    public void testPartnerListDataset() {
        seedPartnerBaseline();
        List<Map<String, Object>> ds = reportBiz.buildPartnerListDataset(null);
        assertFalse(ds.isEmpty(), "往来单位清单数据集非空");
        boolean hasCustomer = false;
        for (Map<String, Object> row : ds) {
            if ("CUSTOMER".equals(row.get("partnerType"))) {
                hasCustomer = true;
                break;
            }
        }
        assertTrue(hasCustomer, "数据集含 CUSTOMER 类型");
    }

    @Test
    public void testPartnerListEmptyDatasetNoError() {
        List<Map<String, Object>> ds = reportBiz.buildPartnerListDataset(null);
        assertNotNull(ds, "空数据集不报错");
        assertTrue(ds.isEmpty(), "无往来单位记录 → 空列表");
    }

    // ===================== 路径注入防护 =====================

    @Test
    public void testPathInjectionRejected() {
        assertThrows(NopException.class,
                () -> reportBiz.renderHtml("../etc/passwd", null, CTX),
                "非法 reportName（含 ../）抛 NopException");
        assertThrows(NopException.class,
                () -> reportBiz.renderHtml(null, null, CTX),
                "空 reportName 抛 NopException");
        seedMaterialBaseline();
        assertThrows(NopException.class,
                () -> reportBiz.download("material-price-list", "exe", null, CTX),
                "非法 renderType 抛 NopException");
    }

    // ===================== 数据准备 =====================

    private void seedMaterialBaseline() {
        ormTemplate.runInSession(() -> {
            Long matId = 8001L;
            seedMaterial(matId, "MAT-MD-RPT", "ACTIVE");
            seedDefaultSku(8101L, matId, bd("100"), bd("200"), bd("180"), bd("220"));
        });
    }

    private void seedPartnerBaseline() {
        ormTemplate.runInSession(() -> {
            seedPartner(8201L, "P-MD-RPT", "客户报表", "CUSTOMER", "ACTIVE");
            seedPartner(8202L, "V-MD-RPT", "供应商报表", "VENDOR", "ACTIVE");
        });
    }

    private void seedMaterial(Long id, String code, String status) {
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial m = new ErpMdMaterial();
        m.orm_propValueByName("id", id);
        m.setCode(code);
        m.setName("物料-" + code);
        m.orm_propValueByName("materialType", "GOODS");
        m.setUoMId(1L);
        m.setStatus(status);
        m.orm_propValueByName("costMethod", "MOVING_AVERAGE");
        dao.saveEntity(m);
    }

    private void seedDefaultSku(Long id, Long materialId, BigDecimal purchase, BigDecimal sale,
                                BigDecimal wholesale, BigDecimal retail) {
        IEntityDao<ErpMdMaterialSku> dao = daoProvider.daoFor(ErpMdMaterialSku.class);
        ErpMdMaterialSku s = new ErpMdMaterialSku();
        s.orm_propValueByName("id", id);
        s.setMaterialId(materialId);
        s.setSkuCode("SKU-" + id);
        s.setUoMId(1L);
        s.setPurchasePrice(purchase);
        s.setSalePrice(sale);
        s.setWholesalePrice(wholesale);
        s.setRetailPrice(retail);
        s.setIsDefault(Boolean.TRUE);
        dao.saveEntity(s);
    }

    private void seedPartner(Long id, String code, String name, String partnerType, String status) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner p = new ErpMdPartner();
        p.orm_propValueByName("id", id);
        p.setCode(code);
        p.setName(name);
        p.orm_propValueByName("partnerType", partnerType);
        p.setStatus(status);
        p.setReceivableBalance(BigDecimal.ZERO);
        p.setPayableBalance(BigDecimal.ZERO);
        dao.saveEntity(p);
    }

    // ===================== helpers =====================

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static BigDecimal toBd(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(v.toString());
        return new BigDecimal(String.valueOf(v));
    }
}
