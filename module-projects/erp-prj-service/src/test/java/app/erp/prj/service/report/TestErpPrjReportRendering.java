package app.erp.prj.service.report;

import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjTimesheet;
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
 * 项目域报表渲染端到端测试（plan 2026-07-06-1815-1 Phase 1 Proof）。
 *
 * <p>覆盖两张项目报表（项目成本汇总 / 工时明细）的 {@code renderHtml}/{@code download(xlsx|pdf)} 渲染管线、
 * 数据集口径断言、空数据集不报错、以及路径注入防护（非法 reportName 抛 {@code ERR_REPORT_NAME_INVALID}）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPrjReportRendering extends JunitAutoTestCase {

    private static final io.nop.core.context.IServiceContext CTX = new io.nop.core.context.ServiceContextImpl();

    static final Long ORG_ID = 1L;
    static final Long CURRENCY_ID = 1L;
    static final String PROJECT_STATUS_OPEN = "OPEN";
    static final String TIMESHEET_STATUS_APPROVED = "APPROVED";

    @Inject
    ErpPrjReportBizModel reportBiz;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    // ===================== Phase 1: 项目成本汇总报表 =====================

    @Test
    public void testProjectCostSummaryRenderHtml() {
        seedProjectBaseline();
        String html = reportBiz.renderHtml("project-cost-summary", null, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
        assertTrue(html.contains("PRJ-CST-1"), "renderHtml 含项目编码");
    }

    @Test
    public void testProjectCostSummaryDownloadXlsxAndPdf() {
        seedProjectBaseline();
        for (String renderType : new String[]{"xlsx", "pdf"}) {
            WebContentBean bean = reportBiz.download("project-cost-summary", renderType, null, CTX);
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
    public void testProjectCostSummaryDataset() {
        seedProjectBaseline();
        List<Map<String, Object>> ds = reportBiz.buildProjectCostSummaryDataset(null, null, null);
        assertFalse(ds.isEmpty(), "项目成本汇总数据集非空");
        Map<String, Object> row = ds.get(0);
        // budget=10000, actualCost=4000 → executionRate=0.4
        assertEquals(0, bd("10000").compareTo(toBd(row.get("budget"))), "budget=10000");
        assertEquals(0, bd("4000").compareTo(toBd(row.get("actualCost"))), "actualCost=4000");
        assertEquals(0, bd("0.4").compareTo(toBd(row.get("executionRate"))), "executionRate=0.4");
    }

    @Test
    public void testProjectCostSummaryEmptyDatasetNoError() {
        List<Map<String, Object>> ds = reportBiz.buildProjectCostSummaryDataset(null, null, null);
        assertNotNull(ds, "空数据集不报错");
        assertTrue(ds.isEmpty(), "无项目记录 → 空列表");
    }

    // ===================== Phase 1: 工时明细报表 =====================

    @Test
    public void testTimesheetDetailRenderHtml() {
        seedTimesheetBaseline();
        String html = reportBiz.renderHtml("timesheet-detail", null, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
    }

    @Test
    public void testTimesheetDetailDownloadXlsxAndPdf() {
        seedTimesheetBaseline();
        for (String renderType : new String[]{"xlsx", "pdf"}) {
            WebContentBean bean = reportBiz.download("timesheet-detail", renderType, null, CTX);
            assertNotNull(bean, "download 非空: " + renderType);
            Object content = bean.getContent();
            assertTrue(content instanceof java.io.File, "download 返回 File: " + renderType);
            java.io.File f = (java.io.File) content;
            assertTrue(f.exists() && f.length() > 0, "download 文件有效: " + renderType);
            f.delete();
        }
    }

    @Test
    public void testTimesheetDetailDataset() {
        seedTimesheetBaseline();
        List<Map<String, Object>> ds = reportBiz.buildTimesheetDetailDataset(null, null, null);
        assertFalse(ds.isEmpty(), "工时明细数据集非空");
        Map<String, Object> row = ds.get(0);
        // 同项目+员工 2 行：hours=8+6=14, costAmount=400+300=700
        assertEquals(0, bd("14").compareTo(toBd(row.get("hours"))), "hours=8+6=14");
        assertEquals(0, bd("700").compareTo(toBd(row.get("costAmount"))), "costAmount=400+300=700");
    }

    @Test
    public void testTimesheetDetailEmptyDatasetNoError() {
        List<Map<String, Object>> ds = reportBiz.buildTimesheetDetailDataset(null, null, null);
        assertNotNull(ds, "空数据集不报错");
        assertTrue(ds.isEmpty(), "无工时记录 → 空列表");
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
        seedProjectBaseline();
        assertThrows(NopException.class,
                () -> reportBiz.download("project-cost-summary", "exe", null, CTX),
                "非法 renderType 抛 NopException");
    }

    // ===================== 数据准备 =====================

    private void seedProjectBaseline() {
        ormTemplate.runInSession(() -> {
            seedProject(5001L, "PRJ-CST-1", "成本汇总报表项目",
                    bd("10000"), bd("4000"), PROJECT_STATUS_OPEN,
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        });
    }

    private void seedTimesheetBaseline() {
        ormTemplate.runInSession(() -> {
            Long projectId = 5002L;
            seedProject(projectId, "PRJ-TS-1", "工时明细报表项目",
                    bd("5000"), bd("700"), PROJECT_STATUS_OPEN,
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
            // 同员工 2 行：hours 8/6, costAmount 400/300
            seedTimesheet(6001L, projectId, 7001L, "TS-RPT-1",
                    LocalDate.of(2026, 7, 10), bd("8"), bd("400"));
            seedTimesheet(6002L, projectId, 7001L, "TS-RPT-2",
                    LocalDate.of(2026, 7, 11), bd("6"), bd("300"));
        });
    }

    private void seedProject(Long id, String code, String name, BigDecimal budget, BigDecimal actualCost,
                             String status, LocalDate startDate, LocalDate endDate) {
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        ErpPrjProject p = new ErpPrjProject();
        p.orm_propValueByName("id", id);
        p.setCode(code);
        p.setName(name);
        p.setOrgId(ORG_ID);
        p.setCurrencyId(CURRENCY_ID);
        p.setStartDate(startDate);
        p.setEndDate(endDate);
        p.setBudget(budget);
        p.setActualCost(actualCost);
        p.orm_propValueByName("committedCost", bd("0"));
        p.orm_propValueByName("billedAmount", bd("0"));
        p.orm_propValueByName("status", status);
        dao.saveEntity(p);
    }

    private void seedTimesheet(Long id, Long projectId, Long userId, String code,
                               LocalDate workDate, BigDecimal hours, BigDecimal costAmount) {
        IEntityDao<ErpPrjTimesheet> dao = daoProvider.daoFor(ErpPrjTimesheet.class);
        ErpPrjTimesheet t = new ErpPrjTimesheet();
        t.orm_propValueByName("id", id);
        t.setCode(code);
        t.setOrgId(ORG_ID);
        t.setProjectId(projectId);
        t.setUserId(userId);
        t.setWorkDate(workDate);
        t.setHours(hours);
        t.setCurrencyId(CURRENCY_ID);
        t.setCostAmount(costAmount);
        t.orm_propValueByName("status", TIMESHEET_STATUS_APPROVED);
        dao.saveEntity(t);
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
