package app.erp.qa.service.report;

import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.qa.dao.entity.ErpQaAction;
import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.service.ErpQaConstants;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 质量域报表渲染端到端测试（plan 2026-07-06-1815-1 Phase 2 Proof）。
 *
 * <p>覆盖两张质量报表（质检合格率统计 / NCR-CAPA 统计）的 {@code renderHtml}/{@code download(xlsx|pdf)}
 * 渲染管线、数据集口径断言、空数据集不报错、以及路径注入防护（非法 reportName 抛 {@code ERR_REPORT_NAME_INVALID}）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpQaReportRendering extends JunitAutoTestCase {

    private static final io.nop.core.context.IServiceContext CTX = new io.nop.core.context.ServiceContextImpl();

    static final Long UOM_ID = 1L;

    @Inject
    ErpQaReportBizModel reportBiz;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    // ===================== Phase 2: 质检合格率统计报表 =====================

    @Test
    public void testInspectionSummaryRenderHtml() {
        seedInspectionBaseline();
        String html = reportBiz.renderHtml("inspection-summary", null, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
        assertTrue(html.contains("MAT-QA-RPT"), "renderHtml 含物料编码");
    }

    @Test
    public void testInspectionSummaryDownloadXlsxAndPdf() {
        seedInspectionBaseline();
        for (String renderType : new String[]{"xlsx", "pdf"}) {
            WebContentBean bean = reportBiz.download("inspection-summary", renderType, null, CTX);
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
    public void testInspectionSummaryDataset() {
        seedInspectionBaseline();
        List<Map<String, Object>> ds = reportBiz.buildInspectionSummaryDataset(null, null, null);
        assertFalse(ds.isEmpty(), "质检汇总数据集非空");
        Map<String, Object> row = ds.get(0);
        // 3 次检验：2 ACCEPTED/CONDITIONAL + 1 REJECTED → passRate=2/3≈0.6667
        assertEquals(3, ((Number) row.get("totalInspections")).intValue(), "totalInspections=3");
        assertEquals(2, ((Number) row.get("acceptedCount")).intValue(), "acceptedCount=2");
        assertEquals(1, ((Number) row.get("rejectedCount")).intValue(), "rejectedCount=1");
        assertTrue(toBd(row.get("passRate")).signum() > 0, "passRate 非零");
    }

    @Test
    public void testInspectionSummaryEmptyDatasetNoError() {
        List<Map<String, Object>> ds = reportBiz.buildInspectionSummaryDataset(null, null, null);
        assertNotNull(ds, "空数据集不报错");
        assertTrue(ds.isEmpty(), "无检验记录 → 空列表");
    }

    // ===================== Phase 2: NCR-CAPA 统计报表 =====================

    @Test
    public void testNcrCapaSummaryRenderHtml() {
        seedNcrBaseline();
        String html = reportBiz.renderHtml("ncr-capa-summary", null, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
        assertTrue(html.contains("HIGH"), "renderHtml 含严重度");
    }

    @Test
    public void testNcrCapaSummaryDownloadXlsxAndPdf() {
        seedNcrBaseline();
        for (String renderType : new String[]{"xlsx", "pdf"}) {
            WebContentBean bean = reportBiz.download("ncr-capa-summary", renderType, null, CTX);
            assertNotNull(bean, "download 非空: " + renderType);
            Object content = bean.getContent();
            assertTrue(content instanceof java.io.File, "download 返回 File: " + renderType);
            java.io.File f = (java.io.File) content;
            assertTrue(f.exists() && f.length() > 0, "download 文件有效: " + renderType);
            f.delete();
        }
    }

    @Test
    public void testNcrCapaSummaryDataset() {
        seedNcrBaseline();
        List<Map<String, Object>> ds = reportBiz.buildNcrCapaSummaryDataset(null, null);
        assertFalse(ds.isEmpty(), "NCR-CAPA 数据集非空");
        Map<String, Object> row = ds.get(0);
        // HIGH 严重度：2 NCR，1 RESOLVED，1 CAPA 动作 COMPLETED
        assertEquals("HIGH", row.get("severity"), "severity=HIGH");
        assertEquals(2, ((Number) row.get("ncrCount")).intValue(), "ncrCount=2");
        assertEquals(1, ((Number) row.get("resolvedNcrCount")).intValue(), "resolvedNcrCount=1");
        assertEquals(1, ((Number) row.get("capaActionCount")).intValue(), "capaActionCount=1");
        assertEquals(1, ((Number) row.get("completedActionCount")).intValue(), "completedActionCount=1");
    }

    @Test
    public void testNcrCapaSummaryEmptyDatasetNoError() {
        List<Map<String, Object>> ds = reportBiz.buildNcrCapaSummaryDataset(null, null);
        assertNotNull(ds, "空数据集不报错");
        assertTrue(ds.isEmpty(), "无 NCR 记录 → 空列表");
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
        seedInspectionBaseline();
        assertThrows(NopException.class,
                () -> reportBiz.download("inspection-summary", "exe", null, CTX),
                "非法 renderType 抛 NopException");
    }

    // ===================== 数据准备 =====================

    private void seedInspectionBaseline() {
        ormTemplate.runInSession(() -> {
            Long matId = 7001L;
            seedMaterial(matId, "MAT-QA-RPT");
            seedInspection(7101L, "INS-RPT-1", matId, ErpQaConstants.INSPECTION_RESULT_ACCEPTED);
            seedInspection(7102L, "INS-RPT-2", matId, ErpQaConstants.INSPECTION_RESULT_CONDITIONAL);
            seedInspection(7103L, "INS-RPT-3", matId, ErpQaConstants.INSPECTION_RESULT_REJECTED);
        });
    }

    private void seedNcrBaseline() {
        ormTemplate.runInSession(() -> {
            Long ncr1 = 7201L;
            Long ncr2 = 7202L;
            seedNcr(ncr1, "NCR-RPT-1", ErpQaConstants.RECALL_SEVERITY_HIGH, ErpQaConstants.NCR_STATUS_RESOLVED);
            seedNcr(ncr2, "NCR-RPT-2", ErpQaConstants.RECALL_SEVERITY_HIGH, ErpQaConstants.NCR_STATUS_OPEN);
            // 1 CAPA 动作，COMPLETED，挂在 ncr1
            seedAction(7301L, ncr1, ErpQaConstants.ACTION_STATUS_COMPLETED);
        });
    }

    private void seedMaterial(Long id, String code) {
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial m = new ErpMdMaterial();
        m.orm_propValueByName("id", id);
        m.setCode(code);
        m.setName("物料 " + code);
        m.orm_propValueByName("materialType", "GOODS");
        m.setUoMId(UOM_ID);
        m.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(m);
    }

    private void seedInspection(Long id, String code, Long materialId, String result) {
        IEntityDao<ErpQaInspection> dao = daoProvider.daoFor(ErpQaInspection.class);
        ErpQaInspection ins = new ErpQaInspection();
        ins.orm_propValueByName("id", id);
        ins.setCode(code);
        ins.orm_propValueByName("inspectionType", ErpQaConstants.INSPECTION_TYPE_INCOMING);
        ins.setMaterialId(materialId);
        ins.orm_propValueByName("result", result);
        ins.orm_propValueByName("docStatus", ErpQaConstants.DOC_STATUS_ACTIVE);
        ins.orm_propValueByName("approveStatus", ErpQaConstants.APPROVE_STATUS_APPROVED);
        ins.setPosted(Boolean.FALSE);
        ins.setInspectionDate(LocalDate.of(2026, 7, 10));
        ins.setBusinessDate(LocalDate.of(2026, 7, 10));
        ins.orm_propValueByName("relatedBillType", "ERP_PUR_RECEIPT");
        ins.orm_propValueByName("relatedBillCode", "BILL-" + code);
        dao.saveEntity(ins);
    }

    private void seedNcr(Long id, String code, String severity, String status) {
        IEntityDao<ErpQaNonConformance> dao = daoProvider.daoFor(ErpQaNonConformance.class);
        ErpQaNonConformance ncr = new ErpQaNonConformance();
        ncr.orm_propValueByName("id", id);
        ncr.setCode(code);
        ncr.setNcrDate(LocalDate.of(2026, 7, 10));
        ncr.setMaterialId(7001L);
        ncr.setQuantity(bd("10"));
        ncr.orm_propValueByName("dispositionType", "REWORK");
        ncr.orm_propValueByName("status", status);
        ncr.orm_propValueByName("severity", severity);
        ncr.orm_propValueByName("description", "测试NCR:" + code);
        dao.saveEntity(ncr);
    }

    private void seedAction(Long id, Long ncrId, String status) {
        IEntityDao<ErpQaAction> dao = daoProvider.daoFor(ErpQaAction.class);
        ErpQaAction a = new ErpQaAction();
        a.orm_propValueByName("id", id);
        a.setNcrId(ncrId);
        a.orm_propValueByName("actionType", "CAPA");
        a.orm_propValueByName("status", status);
        a.orm_propValueByName("description", "纠正预防措施");
        dao.saveEntity(a);
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
