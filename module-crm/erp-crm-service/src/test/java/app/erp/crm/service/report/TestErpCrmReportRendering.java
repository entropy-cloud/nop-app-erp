package app.erp.crm.service.report;

import app.erp.crm.dao.entity.ErpCrmForecast;
import app.erp.crm.dao.entity.ErpCrmForecastLine;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmStage;
import app.erp.crm.service.ErpCrmConstants;
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
 * CRM 域报表渲染端到端测试（plan 2026-07-06-1815-1 Phase 3 Proof）。
 *
 * <p>覆盖两张 CRM 报表（线索转化漏斗 / 销售预测准确率）的 {@code renderHtml}/{@code download(xlsx|pdf)}
 * 渲染管线、数据集口径断言、空数据集不报错、以及路径注入防护（非法 reportName 抛 {@code ERR_REPORT_NAME_INVALID}）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCrmReportRendering extends JunitAutoTestCase {

    private static final io.nop.core.context.IServiceContext CTX = new io.nop.core.context.ServiceContextImpl();

    static final Long ORG_ID = 1L;

    @Inject
    ErpCrmReportBizModel reportBiz;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    // ===================== Phase 3: 线索转化漏斗报表 =====================

    @Test
    public void testLeadConversionFunnelRenderHtml() {
        seedFunnelBaseline();
        String html = reportBiz.renderHtml("lead-conversion-funnel", null, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
        assertTrue(html.contains("STAGE-RPT"), "renderHtml 含阶段名称");
    }

    @Test
    public void testLeadConversionFunnelDownloadXlsxAndPdf() {
        seedFunnelBaseline();
        for (String renderType : new String[]{"xlsx", "pdf"}) {
            WebContentBean bean = reportBiz.download("lead-conversion-funnel", renderType, null, CTX);
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
    public void testLeadConversionFunnelDataset() {
        seedFunnelBaseline();
        List<Map<String, Object>> ds = reportBiz.buildLeadConversionFunnelDataset();
        assertFalse(ds.isEmpty(), "线索漏斗数据集非空");
        Map<String, Object> row = ds.get(0);
        // 同 stage 2 条 lead：leadCount=2, expectedRevenue=10000+5000=15000
        assertEquals(2, ((Number) row.get("leadCount")).intValue(), "leadCount=2");
        assertEquals(0, bd("15000").compareTo(toBd(row.get("expectedRevenue"))), "expectedRevenue=15000");
    }

    @Test
    public void testLeadConversionFunnelEmptyDatasetNoError() {
        List<Map<String, Object>> ds = reportBiz.buildLeadConversionFunnelDataset();
        assertNotNull(ds, "空数据集不报错");
        assertTrue(ds.isEmpty(), "无线索记录 → 空列表");
    }

    // ===================== Phase 3: 销售预测准确率报表 =====================

    @Test
    public void testForecastAccuracyRenderHtml() {
        seedForecastBaseline();
        String html = reportBiz.renderHtml("forecast-accuracy", null, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
    }

    @Test
    public void testForecastAccuracyDownloadXlsxAndPdf() {
        seedForecastBaseline();
        for (String renderType : new String[]{"xlsx", "pdf"}) {
            WebContentBean bean = reportBiz.download("forecast-accuracy", renderType, null, CTX);
            assertNotNull(bean, "download 非空: " + renderType);
            Object content = bean.getContent();
            assertTrue(content instanceof java.io.File, "download 返回 File: " + renderType);
            java.io.File f = (java.io.File) content;
            assertTrue(f.exists() && f.length() > 0, "download 文件有效: " + renderType);
            f.delete();
        }
    }

    @Test
    public void testForecastAccuracyDataset() {
        seedForecastBaseline();
        List<Map<String, Object>> ds = reportBiz.buildForecastAccuracyDataset(null);
        assertFalse(ds.isEmpty(), "预测准确率数据集非空");
        Map<String, Object> row = ds.get(0);
        // commitAmount=10000, 2 行加权收入 6000+4000=10000
        assertEquals(0, bd("10000").compareTo(toBd(row.get("commitAmount"))), "commitAmount=10000");
        assertEquals(2, ((Number) row.get("lineCount")).intValue(), "lineCount=2");
        assertEquals(0, bd("10000").compareTo(toBd(row.get("lineWeightedRevenue"))), "lineWeightedRevenue=6000+4000");
    }

    @Test
    public void testForecastAccuracyEmptyDatasetNoError() {
        List<Map<String, Object>> ds = reportBiz.buildForecastAccuracyDataset(null);
        assertNotNull(ds, "空数据集不报错");
        assertTrue(ds.isEmpty(), "无预测记录 → 空列表");
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
        seedFunnelBaseline();
        assertThrows(NopException.class,
                () -> reportBiz.download("lead-conversion-funnel", "exe", null, CTX),
                "非法 renderType 抛 NopException");
    }

    // ===================== 数据准备 =====================

    private void seedFunnelBaseline() {
        ormTemplate.runInSession(() -> {
            Long stageId = 9001L;
            seedStage(stageId, "STAGE-RPT");
            seedLead(9101L, "LEAD-RPT-1", stageId, bd("10000"));
            seedLead(9102L, "LEAD-RPT-2", stageId, bd("5000"));
        });
    }

    private void seedForecastBaseline() {
        ormTemplate.runInSession(() -> {
            Long forecastId = 9201L;
            Long leadId = 9401L;
            seedForecast(forecastId, bd("10000"), bd("8000"), bd("12000"), bd("9000"), 5);
            seedForecastLine(9301L, forecastId, leadId, bd("6000"));
            seedForecastLine(9302L, forecastId, leadId, bd("4000"));
        });
    }

    private void seedStage(Long id, String stageName) {
        IEntityDao<ErpCrmStage> dao = daoProvider.daoFor(ErpCrmStage.class);
        ErpCrmStage s = new ErpCrmStage();
        s.orm_propValueByName("id", id);
        s.setCode("STG-" + id);
        s.setOrgId(ORG_ID);
        s.setStageName(stageName);
        s.orm_propValueByName("sequence", 10);
        dao.saveEntity(s);
    }

    private void seedLead(Long id, String code, Long stageId, BigDecimal expectedRevenue) {
        IEntityDao<ErpCrmLead> dao = daoProvider.daoFor(ErpCrmLead.class);
        ErpCrmLead l = new ErpCrmLead();
        l.orm_propValueByName("id", id);
        l.setCode(code);
        l.setOrgId(ORG_ID);
        l.orm_propValueByName("leadType", ErpCrmConstants.LEAD_TYPE_LEAD);
        l.orm_propValueByName("docStatus", ErpCrmConstants.DOC_STATUS_NEW);
        l.setStageId(stageId);
        l.setContactName("联系人" + id);
        l.setExpectedRevenue(expectedRevenue);
        dao.saveEntity(l);
    }

    private void seedForecast(Long id, BigDecimal commit, BigDecimal weighted,
                              BigDecimal bestCase, BigDecimal expectedClosed, int oppCount) {
        IEntityDao<ErpCrmForecast> dao = daoProvider.daoFor(ErpCrmForecast.class);
        ErpCrmForecast f = new ErpCrmForecast();
        f.orm_propValueByName("id", id);
        f.setOrgId(ORG_ID);
        f.setPeriodId(9901L);
        f.setCommitAmount(commit);
        f.setWeightedAmount(weighted);
        f.setBestCaseAmount(bestCase);
        f.setExpectedClosedRevenue(expectedClosed);
        f.setOpportunityCount(oppCount);
        dao.saveEntity(f);
    }

    private void seedForecastLine(Long id, Long forecastId, Long leadId, BigDecimal weightedRevenue) {
        IEntityDao<ErpCrmForecastLine> dao = daoProvider.daoFor(ErpCrmForecastLine.class);
        ErpCrmForecastLine l = new ErpCrmForecastLine();
        l.orm_propValueByName("id", id);
        l.setForecastId(forecastId);
        l.setLeadId(leadId);
        l.setOrgId(ORG_ID);
        l.setWeightedRevenue(weightedRevenue);
        l.orm_propValueByName("forecastCategory", "COMMIT");
        dao.saveEntity(l);
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
