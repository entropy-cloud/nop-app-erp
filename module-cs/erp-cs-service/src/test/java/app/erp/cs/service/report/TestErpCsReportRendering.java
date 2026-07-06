package app.erp.cs.service.report;

import app.erp.cs.dao.entity.ErpCsSurvey;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.service.ErpCsConstants;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 客服域报表渲染端到端测试（plan 2026-07-06-1815-1 Phase 3 Proof）。
 *
 * <p>覆盖工单 SLA/CSAT 综合统计表的 {@code renderHtml}/{@code download(xlsx|pdf)} 渲染管线、数据集口径断言、
 * 空数据集不报错、以及路径注入防护（非法 reportName 抛 {@code ERR_REPORT_NAME_INVALID}）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCsReportRendering extends JunitAutoTestCase {

    private static final io.nop.core.context.IServiceContext CTX = new io.nop.core.context.ServiceContextImpl();

    static final Long ORG_ID = 1L;
    static final Long CUSTOMER_ID = 5001L;
    static final Long TICKET_TYPE_ID = 6001L;

    @Inject
    ErpCsReportBizModel reportBiz;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    // ===================== Phase 3: 工单 SLA/CSAT 综合统计报表 =====================

    @Test
    public void testTicketSlaCsatSummaryRenderHtml() {
        seedBaseline();
        String html = reportBiz.renderHtml("ticket-sla-csat-summary", null, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
        assertTrue(html.contains("CSAT"), "renderHtml 含 CSAT 标识");
    }

    @Test
    public void testTicketSlaCsatSummaryDownloadXlsxAndPdf() {
        seedBaseline();
        for (String renderType : new String[]{"xlsx", "pdf"}) {
            WebContentBean bean = reportBiz.download("ticket-sla-csat-summary", renderType, null, CTX);
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
    public void testTicketSlaCsatSummaryDataset() {
        seedBaseline();
        List<Map<String, Object>> ds = reportBiz.buildTicketSlaCsatSummaryDataset(null);
        assertFalse(ds.isEmpty(), "SLA/CSAT 数据集非空");
        Map<String, Object> row = ds.get(0);
        // 3 工单：1 SLA 命中 + 2 SLA 超时；2 调查回复 csat=4/5 nps=8/9 → avgCsat=4.5 avgNps=8.5
        assertEquals(3, ((Number) row.get("totalTickets")).intValue(), "totalTickets=3");
        assertEquals(1, ((Number) row.get("slaCompletedCount")).intValue(), "slaCompletedCount=1");
        assertEquals(2, ((Number) row.get("slaBreachedCount")).intValue(), "slaBreachedCount=2");
        assertEquals(2, ((Number) row.get("surveyCount")).intValue(), "surveyCount=2");
        assertEquals(0, bd("4.5").compareTo(toBd(row.get("avgCsat"))), "avgCsat=(4+5)/2=4.5");
        assertEquals(0, bd("8.5").compareTo(toBd(row.get("avgNps"))), "avgNps=(8+9)/2=8.5");
    }

    @Test
    public void testTicketSlaCsatSummaryEmptyDatasetNoError() {
        List<Map<String, Object>> ds = reportBiz.buildTicketSlaCsatSummaryDataset(null);
        assertNotNull(ds, "空数据集不报错");
        assertTrue(ds.isEmpty(), "无工单记录 → 空列表");
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
        seedBaseline();
        assertThrows(NopException.class,
                () -> reportBiz.download("ticket-sla-csat-summary", "exe", null, CTX),
                "非法 renderType 抛 NopException");
    }

    // ===================== 数据准备 =====================

    private void seedBaseline() {
        ormTemplate.runInSession(() -> {
            // 3 工单：1 SLA 命中 + 2 SLA 超时
            Long t1 = seedTicket(7001L, "TKT-RPT-1", Boolean.TRUE);
            Long t2 = seedTicket(7002L, "TKT-RPT-2", Boolean.FALSE);
            Long t3 = seedTicket(7003L, "TKT-RPT-3", Boolean.FALSE);
            // 2 调查回复：csat 4/5, nps 8/9
            seedSurvey(7101L, t1, 4, 8);
            seedSurvey(7102L, t2, 5, 9);
        });
    }

    private Long seedTicket(Long id, String code, Boolean isSlaCompleted) {
        IEntityDao<ErpCsTicket> dao = daoProvider.daoFor(ErpCsTicket.class);
        ErpCsTicket t = new ErpCsTicket();
        t.orm_propValueByName("id", id);
        t.setCode(code);
        t.setOrgId(ORG_ID);
        t.orm_propValueByName("subject", "工单-" + code);
        t.setCustomerId(CUSTOMER_ID);
        t.setTicketTypeId(TICKET_TYPE_ID);
        t.orm_propValueByName("priority", ErpCsConstants.TICKET_PRIORITY_HIGH);
        t.orm_propValueByName("status", ErpCsConstants.TICKET_STATUS_CLOSED);
        t.orm_propValueByName("docStatus", ErpCsConstants.DOC_STATUS_ACTIVE);
        t.orm_propValueByName("approveStatus", ErpCsConstants.APPROVE_STATUS_UNSUBMITTED);
        t.setIsSlaCompleted(isSlaCompleted);
        dao.saveEntity(t);
        return id;
    }

    private void seedSurvey(Long id, Long ticketId, int csat, int nps) {
        IEntityDao<ErpCsSurvey> dao = daoProvider.daoFor(ErpCsSurvey.class);
        ErpCsSurvey s = new ErpCsSurvey();
        s.orm_propValueByName("id", id);
        s.setOrgId(ORG_ID);
        s.setTicketId(ticketId);
        s.setCsatScore(csat);
        s.setNpsScore(nps);
        s.setRespondedAt(LocalDateTime.of(2026, 7, 10, 12, 0));
        dao.saveEntity(s);
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
