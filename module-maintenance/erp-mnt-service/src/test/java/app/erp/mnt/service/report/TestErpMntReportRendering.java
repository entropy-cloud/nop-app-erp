package app.erp.mnt.service.report;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntSparePartUsage;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.dao.entity.ErpMntVisitTask;
import app.erp.mnt.service.ErpMntConstants;
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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 维护域报表渲染端到端测试（plan 2026-07-06-1815-1 Phase 2 Proof）。
 *
 * <p>覆盖两张维护报表（维护历史 / 停机统计）的 {@code renderHtml}/{@code download(xlsx|pdf)} 渲染管线、
 * 数据集口径断言、空数据集不报错、以及路径注入防护（非法 reportName 抛 {@code ERR_REPORT_NAME_INVALID}）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntReportRendering extends JunitAutoTestCase {

    private static final io.nop.core.context.IServiceContext CTX = new io.nop.core.context.ServiceContextImpl();

    static final Long ORG_ID = 1L;

    @Inject
    ErpMntReportBizModel reportBiz;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    // ===================== Phase 2: 维护历史报表 =====================

    @Test
    public void testMaintenanceHistoryRenderHtml() {
        seedMaintenanceHistoryBaseline();
        String html = reportBiz.renderHtml("maintenance-history", null, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
        assertTrue(html.contains("VST-RPT-1"), "renderHtml 含访问编码");
    }

    @Test
    public void testMaintenanceHistoryDownloadXlsxAndPdf() {
        seedMaintenanceHistoryBaseline();
        for (String renderType : new String[]{"xlsx", "pdf"}) {
            WebContentBean bean = reportBiz.download("maintenance-history", renderType, null, CTX);
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
    public void testMaintenanceHistoryDataset() {
        seedMaintenanceHistoryBaseline();
        List<Map<String, Object>> ds = reportBiz.buildMaintenanceHistoryDataset(null, null, null);
        assertFalse(ds.isEmpty(), "维护历史数据集非空");
        Map<String, Object> row = ds.get(0);
        // totalMinutes=120, taskCount=2, sparePartUsageCount=1
        assertEquals(0, bd("120").compareTo(toBd(row.get("totalMinutes"))), "totalMinutes=120");
        assertEquals(2, ((Number) row.get("taskCount")).intValue(), "taskCount=2");
        assertEquals(1, ((Number) row.get("sparePartUsageCount")).intValue(), "sparePartUsageCount=1");
    }

    @Test
    public void testMaintenanceHistoryEmptyDatasetNoError() {
        List<Map<String, Object>> ds = reportBiz.buildMaintenanceHistoryDataset(null, null, null);
        assertNotNull(ds, "空数据集不报错");
        assertTrue(ds.isEmpty(), "无访问记录 → 空列表");
    }

    // ===================== Phase 2: 停机统计报表 =====================

    @Test
    public void testDowntimeSummaryRenderHtml() {
        seedDowntimeBaseline();
        String html = reportBiz.renderHtml("downtime-summary", null, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
        assertTrue(html.contains("BREAKDOWN"), "renderHtml 含停机原因");
    }

    @Test
    public void testDowntimeSummaryDownloadXlsxAndPdf() {
        seedDowntimeBaseline();
        for (String renderType : new String[]{"xlsx", "pdf"}) {
            WebContentBean bean = reportBiz.download("downtime-summary", renderType, null, CTX);
            assertNotNull(bean, "download 非空: " + renderType);
            Object content = bean.getContent();
            assertTrue(content instanceof java.io.File, "download 返回 File: " + renderType);
            java.io.File f = (java.io.File) content;
            assertTrue(f.exists() && f.length() > 0, "download 文件有效: " + renderType);
            f.delete();
        }
    }

    @Test
    public void testDowntimeSummaryDataset() {
        seedDowntimeBaseline();
        List<Map<String, Object>> ds = reportBiz.buildDowntimeSummaryDataset(null, null, null);
        assertFalse(ds.isEmpty(), "停机统计数据集非空");
        Map<String, Object> row = ds.get(0);
        // 2 条同设备同原因：downtimeMinutes=60+40=100, entryCount=2
        assertEquals(0, bd("100").compareTo(toBd(row.get("downtimeMinutes"))), "downtimeMinutes=60+40=100");
        assertEquals(2, ((Number) row.get("entryCount")).intValue(), "entryCount=2");
        assertEquals("BREAKDOWN", row.get("reason"), "reason=BREAKDOWN");
    }

    @Test
    public void testDowntimeSummaryEmptyDatasetNoError() {
        List<Map<String, Object>> ds = reportBiz.buildDowntimeSummaryDataset(null, null, null);
        assertNotNull(ds, "空数据集不报错");
        assertTrue(ds.isEmpty(), "无停机记录 → 空列表");
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
        seedMaintenanceHistoryBaseline();
        assertThrows(NopException.class,
                () -> reportBiz.download("maintenance-history", "exe", null, CTX),
                "非法 renderType 抛 NopException");
    }

    // ===================== 数据准备 =====================

    private void seedMaintenanceHistoryBaseline() {
        ormTemplate.runInSession(() -> {
            Long eqId = 3001L;
            seedEquipment(eqId, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            Long visitId = 3101L;
            seedVisit(visitId, "VST-RPT-1", eqId, LocalDate.of(2026, 7, 10),
                    ErpMntDaoConstants.VISIT_TYPE_PLANNED,
                    ErpMntDaoConstants.VISIT_STATUS_COMPLETED, bd("120"));
            seedVisitTask(3201L, visitId, 10, "检查液压系统");
            seedVisitTask(3202L, visitId, 20, "更换滤芯");
            seedSparePartUsage(3301L, "SPU-RPT-1", visitId, eqId);
        });
    }

    private void seedDowntimeBaseline() {
        ormTemplate.runInSession(() -> {
            Long eqId = 3002L;
            seedEquipment(eqId, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            // 同设备同原因 2 条：60 + 40
            seedDowntime(3401L, eqId, LocalDateTime.of(2026, 7, 10, 9, 0),
                    "BREAKDOWN", bd("60"));
            seedDowntime(3402L, eqId, LocalDateTime.of(2026, 7, 11, 14, 0),
                    "BREAKDOWN", bd("40"));
        });
    }

    private void seedEquipment(Long id, String status) {
        IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
        ErpMntEquipment e = new ErpMntEquipment();
        e.orm_propValueByName("id", id);
        e.setCode("EQ-" + id);
        e.setName("设备" + id);
        e.orm_propValueByName("status", status);
        dao.saveEntity(e);
    }

    private void seedVisit(Long id, String code, Long equipmentId, LocalDate visitDate,
                           String visitType, String status, BigDecimal totalMinutes) {
        IEntityDao<ErpMntVisit> dao = daoProvider.daoFor(ErpMntVisit.class);
        ErpMntVisit v = new ErpMntVisit();
        v.orm_propValueByName("id", id);
        v.setCode(code);
        v.setOrgId(ORG_ID);
        v.setEquipmentId(equipmentId);
        v.setVisitDate(visitDate);
        v.orm_propValueByName("visitType", visitType);
        v.orm_propValueByName("status", status);
        v.setTotalMinutes(totalMinutes);
        dao.saveEntity(v);
    }

    private void seedVisitTask(Long id, Long visitId, int lineNo, String desc) {
        IEntityDao<ErpMntVisitTask> dao = daoProvider.daoFor(ErpMntVisitTask.class);
        ErpMntVisitTask t = new ErpMntVisitTask();
        t.orm_propValueByName("id", id);
        t.setVisitId(visitId);
        t.orm_propValueByName("lineNo", lineNo);
        t.orm_propValueByName("taskDescription", desc);
        t.orm_propValueByName("status", ErpMntDaoConstants.VISIT_TASK_STATUS_COMPLETED);
        dao.saveEntity(t);
    }

    private void seedSparePartUsage(Long id, String code, Long visitId, Long equipmentId) {
        IEntityDao<ErpMntSparePartUsage> dao = daoProvider.daoFor(ErpMntSparePartUsage.class);
        ErpMntSparePartUsage u = new ErpMntSparePartUsage();
        u.orm_propValueByName("id", id);
        u.setCode(code);
        u.setOrgId(ORG_ID);
        u.setVisitId(visitId);
        u.setEquipmentId(equipmentId);
        u.setWarehouseId(9001L);
        u.setBusinessDate(LocalDate.of(2026, 7, 10));
        u.orm_propValueByName("docStatus", ErpMntDaoConstants.DOC_STATUS_ACTIVE);
        u.orm_propValueByName("approveStatus", ErpMntConstants.APPROVE_STATUS_APPROVED);
        dao.saveEntity(u);
    }

    private void seedDowntime(Long id, Long equipmentId, LocalDateTime startTime, String reason, BigDecimal totalMinutes) {
        IEntityDao<ErpMntDowntimeEntry> dao = daoProvider.daoFor(ErpMntDowntimeEntry.class);
        ErpMntDowntimeEntry d = new ErpMntDowntimeEntry();
        d.orm_propValueByName("id", id);
        d.setEquipmentId(equipmentId);
        d.setStartTime(Timestamp.valueOf(startTime));
        d.setEndTime(Timestamp.valueOf(startTime.plusMinutes(totalMinutes.longValue())));
        d.setTotalMinutes(totalMinutes);
        d.orm_propValueByName("reason", reason);
        dao.saveEntity(d);
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
