package app.erp.qa.service.dashboard;

import app.erp.qa.dao.entity.ErpQaAction;
import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 质量看板聚合（{@code ErpQaDashboard__*}）集成测试。覆盖：质检数/合格率/不合格数/开放 NCR 计数、
 * 合格率趋势、不合格原因 TOP 聚合、CAPA 逾期预警触发/不触发、空数据集零值。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpQaDashboard extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpQaDashboardBizModel dashboardBiz;

    @Test
    public void testKpiEmptyDatasetReturnsZeros() {
        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, null, CTX);
        assertEquals(0L, kpi.get("inspectionCount"));
        assertEquals(0.0, (double) kpi.get("passRate"), 0.001);
        assertEquals(0L, kpi.get("rejectedCount"));
        assertEquals(0L, kpi.get("openNcrCount"));
    }

    @Test
    public void testKpiPassRateArithmetic() {
        LocalDate today = CoreMetrics.currentDate();
        ormTemplate.runInSession(() -> {
            // 本期 4 质检：2 ACCEPTED + 1 REJECTED + 1 CONDITIONAL → 合格率 2/4=0.5
            seedInspection(101L, today, ErpQaConstants.INSPECTION_RESULT_ACCEPTED);
            seedInspection(102L, today, ErpQaConstants.INSPECTION_RESULT_ACCEPTED);
            seedInspection(103L, today, ErpQaConstants.INSPECTION_RESULT_REJECTED);
            seedInspection(104L, today, ErpQaConstants.INSPECTION_RESULT_CONDITIONAL);
            // 非本期不计入
            seedInspection(105L, today.minusMonths(2), ErpQaConstants.INSPECTION_RESULT_ACCEPTED);
            // NCR：2 OPEN + 1 IN_REVIEW + 1 RESOLVED → 开放 3
            seedNcr(201L, today, ErpQaConstants.NCR_STATUS_OPEN, ErpQaConstants.DISPOSITION_TYPE_SCRAP);
            seedNcr(202L, today, ErpQaConstants.NCR_STATUS_IN_REVIEW, ErpQaConstants.DISPOSITION_TYPE_RETURN);
            seedNcr(203L, today, ErpQaConstants.NCR_STATUS_RESOLVED, ErpQaConstants.DISPOSITION_TYPE_SCRAP);
        });

        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, null, CTX);
        assertEquals(4L, kpi.get("inspectionCount"));
        assertEquals(0.5, (double) kpi.get("passRate"), 0.001);
        assertEquals(1L, kpi.get("rejectedCount"));
        assertEquals(2L, kpi.get("openNcrCount"));
    }

    @Test
    public void testTrendMonthlyPassRate() {
        LocalDate today = CoreMetrics.currentDate();
        ormTemplate.runInSession(() -> {
            // 本月 2 质检：1 ACCEPTED + 1 REJECTED → 合格率 0.5
            seedInspection(111L, today, ErpQaConstants.INSPECTION_RESULT_ACCEPTED);
            seedInspection(112L, today, ErpQaConstants.INSPECTION_RESULT_REJECTED);
            // 上月 1 质检：1 ACCEPTED → 合格率 1.0
            seedInspection(113L, today.minusMonths(1), ErpQaConstants.INSPECTION_RESULT_ACCEPTED);
        });
        List<Map<String, Object>> trend = dashboardBiz.getDashboardTrend(2, CTX);
        assertEquals(2, trend.size());
        // 找到本月和上月的数据
        String currentMonth = today.getYear() + "-" + String.format("%02d", today.getMonthValue());
        for (Map<String, Object> row : trend) {
            if (currentMonth.equals(row.get("month"))) {
                assertEquals(2L, row.get("total"));
                assertEquals(1L, row.get("accepted"));
                assertEquals(0.5, (double) row.get("passRate"), 0.001);
            }
        }
    }

    @Test
    public void testDefectTopN() {
        LocalDate today = CoreMetrics.currentDate();
        ormTemplate.runInSession(() -> {
            // SCRAP 3 + RETURN 1
            seedNcr(221L, today, ErpQaConstants.NCR_STATUS_OPEN, ErpQaConstants.DISPOSITION_TYPE_SCRAP);
            seedNcr(222L, today, ErpQaConstants.NCR_STATUS_OPEN, ErpQaConstants.DISPOSITION_TYPE_SCRAP);
            seedNcr(223L, today, ErpQaConstants.NCR_STATUS_OPEN, ErpQaConstants.DISPOSITION_TYPE_SCRAP);
            seedNcr(224L, today, ErpQaConstants.NCR_STATUS_OPEN, ErpQaConstants.DISPOSITION_TYPE_RETURN);
        });
        List<Map<String, Object>> top = dashboardBiz.findDefectTopN(10, CTX);
        assertEquals(2, top.size(), "2 种处置类型");
        // SCRAP 3 > RETURN 1 → SCRAP 排第一
        assertEquals(ErpQaConstants.DISPOSITION_TYPE_SCRAP, top.get(0).get("dispositionType"));
        assertEquals(3L, top.get(0).get("count"));
    }

    @Test
    public void testCapaOverdueAlertTriggersAndNot() {
        LocalDate past = CoreMetrics.currentDate().minusDays(10);
        LocalDate future = CoreMetrics.currentDate().plusDays(10);
        ormTemplate.runInSession(() -> {
            // Action A: dueDate 过去, PENDING → 触发
            seedAction(301L, past, ErpQaConstants.ACTION_STATUS_PENDING);
            // Action B: dueDate 过去, COMPLETED → 不触发
            seedAction(302L, past, ErpQaConstants.ACTION_STATUS_COMPLETED);
            // Action C: dueDate 未来, PENDING → 不触发
            seedAction(303L, future, ErpQaConstants.ACTION_STATUS_PENDING);
        });
        AppConfig.getConfigProvider().assignConfigValue(
                ErpQaConstants.CONFIG_DASH_QA_CAPA_OVERDUE_DAYS,
                String.valueOf(ErpQaConstants.DEFAULT_DASH_QA_CAPA_OVERDUE_DAYS));
        List<Map<String, Object>> alerts = dashboardBiz.findCapaOverdueAlert(CTX);
        assertEquals(1, alerts.size(), "仅 Action A 触发");
        assertEquals(301L, alerts.get(0).get("actionId"));
        assertEquals(10L, alerts.get(0).get("overdueDays"));
    }

    // ---------- helpers ----------

    private void seedInspection(long id, LocalDate inspectionDate, String result) {
        IEntityDao<ErpQaInspection> dao = daoProvider.daoFor(ErpQaInspection.class);
        ErpQaInspection i = dao.newEntity();
        i.orm_propValue(1, id);
        i.setCode("INS-" + id);
        i.setInspectionType(ErpQaConstants.INSPECTION_TYPE_INCOMING);
        i.setMaterialId(1L);
        i.setBusinessDate(inspectionDate);
        i.setInspectionDate(inspectionDate);
        i.setResult(result);
        i.setDocStatus(ErpQaConstants.DOC_STATUS_ACTIVE);
        i.setApproveStatus(ErpQaConstants.APPROVE_STATUS_APPROVED);
        dao.saveEntity(i);
    }

    private void seedNcr(long id, LocalDate ncrDate, String status, String dispositionType) {
        IEntityDao<ErpQaNonConformance> dao = daoProvider.daoFor(ErpQaNonConformance.class);
        ErpQaNonConformance n = dao.newEntity();
        n.orm_propValue(1, id);
        n.setCode("NCR-" + id);
        n.setNcrDate(ncrDate);
        n.setMaterialId(1L);
        n.setSeverity("MAJOR");
        n.setStatus(status);
        n.setDispositionType(dispositionType);
        dao.saveEntity(n);
    }

    private void seedAction(long id, LocalDate dueDate, String status) {
        IEntityDao<ErpQaAction> dao = daoProvider.daoFor(ErpQaAction.class);
        ErpQaAction a = dao.newEntity();
        a.orm_propValue(1, id);
        a.setNcrId(1L);
        a.setActionType("CORRECTIVE");
        a.setDueDate(dueDate);
        a.setStatus(status);
        dao.saveEntity(a);
    }
}
