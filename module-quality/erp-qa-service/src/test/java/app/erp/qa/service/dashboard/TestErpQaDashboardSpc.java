package app.erp.qa.service.dashboard;

import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.dao.entity.ErpQaSpcCapability;
import app.erp.qa.dao.entity.ErpQaSpcSample;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import app.erp.qa.service.QaFrozenClockExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPC 失控预警看板聚合（{@code ErpQaDashboard__getSpcOutOfControlWarning}）集成测试
 * （plan 2026-07-07-1100-3 Phase 3）。覆盖：空数据零值结构、INADEQUATE 能力图数计数、
 * SPC NCR 开/关状态纳入差异、config-gated 关闭纳入段。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpQaDashboardSpc extends JunitAutoTestCase {

    @RegisterExtension
    static QaFrozenClockExtension frozenClock = new QaFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpQaDashboardBizModel dashboardBiz;

    @AfterEach
    void clearConfig() {
        AppConfig.getConfigProvider().assignConfigValue(ErpQaConstants.CONFIG_DASH_QA_SPC_INCLUDE_INADEQUATE, "");
        AppConfig.getConfigProvider().assignConfigValue(ErpQaConstants.CONFIG_DASH_QA_SPC_INCLUDE_NCR, "");
    }

    @Test
    public void testEmptyDatasetReturnsZeros() {
        Map<String, Object> w = dashboardBiz.getSpcOutOfControlWarning(CTX);
        assertEquals(0L, w.get("outOfControlChartCount"));
        assertEquals(0L, w.get("inadequateCapabilityCount"));
        assertEquals(0L, w.get("openSpcNcrCount"));
        assertTrue((Boolean) w.get("includeInadequate"), "默认纳入 INADEQUATE");
        assertTrue((Boolean) w.get("includeNcr"), "默认纳入 SPC NCR");
    }

    @Test
    public void testInadequateCapabilityCount() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        ormTemplate.runInSession(() -> {
            // chart 601/602 INADEQUATE, chart 603 ACCEPTABLE
            seedCapability(8601L, 601L, from, to, ErpQaConstants.SPC_CAPABILITY_INADEQUATE);
            seedCapability(8602L, 602L, from, to, ErpQaConstants.SPC_CAPABILITY_INADEQUATE);
            seedCapability(8603L, 603L, from, to, ErpQaConstants.SPC_CAPABILITY_ACCEPTABLE);
            // 同 chart 多条 INADEQUATE 记录去重为 1 图
            seedCapability(8604L, 601L, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28),
                    ErpQaConstants.SPC_CAPABILITY_INADEQUATE);
        });

        Map<String, Object> w = dashboardBiz.getSpcOutOfControlWarning(CTX);
        assertEquals(2L, w.get("inadequateCapabilityCount"), "distinct chart 601/602");
    }

    @Test
    public void testOutOfControlChartCount() {
        ormTemplate.runInSession(() -> {
            // chart 701 失控（2 子组）, chart 702 失控, chart 703 受控
            seedSample(9701L, 701L, 1, true);
            seedSample(9702L, 701L, 2, true);
            seedSample(9703L, 702L, 1, true);
            seedSample(9704L, 703L, 1, false);
        });

        Map<String, Object> w = dashboardBiz.getSpcOutOfControlWarning(CTX);
        assertEquals(2L, w.get("outOfControlChartCount"), "distinct chart 701/702");
    }

    @Test
    public void testSpcNcrStatusInclusion() {
        LocalDate today = CoreMetrics.currentDate();
        ormTemplate.runInSession(() -> {
            // SPC 来源：2 OPEN + 1 IN_REVIEW + 1 RESOLVED → 开放 3
            seedSpcNcr(5001L, today, ErpQaConstants.NCR_STATUS_OPEN);
            seedSpcNcr(5002L, today, ErpQaConstants.NCR_STATUS_IN_REVIEW);
            seedSpcNcr(5005L, today, ErpQaConstants.NCR_STATUS_OPEN);
            seedSpcNcr(5003L, today, ErpQaConstants.NCR_STATUS_RESOLVED);
            // INSPECTION 来源不计入 SPC NCR
            seedNcr(5004L, today, ErpQaConstants.NCR_STATUS_OPEN, ErpQaConstants.NCR_SOURCE_TYPE_INSPECTION);
        });

        Map<String, Object> w = dashboardBiz.getSpcOutOfControlWarning(CTX);
        assertEquals(3L, w.get("openSpcNcrCount"), "OPEN+IN_REVIEW=3，RESOLVED 与 INSPECTION 来源不计");
    }

    @Test
    public void testConfigGatedClosesInclusionSegments() {
        LocalDate today = CoreMetrics.currentDate();
        ormTemplate.runInSession(() -> {
            seedCapability(8611L, 611L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                    ErpQaConstants.SPC_CAPABILITY_INADEQUATE);
            seedSpcNcr(5111L, today, ErpQaConstants.NCR_STATUS_OPEN);
        });
        // 关闭两段纳入
        AppConfig.getConfigProvider().assignConfigValue(ErpQaConstants.CONFIG_DASH_QA_SPC_INCLUDE_INADEQUATE, "false");
        AppConfig.getConfigProvider().assignConfigValue(ErpQaConstants.CONFIG_DASH_QA_SPC_INCLUDE_NCR, "false");

        Map<String, Object> w = dashboardBiz.getSpcOutOfControlWarning(CTX);
        assertEquals(0L, w.get("inadequateCapabilityCount"), "config 关闭 → INADEQUATE 计数归零");
        assertEquals(0L, w.get("openSpcNcrCount"), "config 关闭 → SPC NCR 计数归零");
        assertEquals(false, w.get("includeInadequate"));
        assertEquals(false, w.get("includeNcr"));
    }

    // ---------- helpers ----------

    private void seedSample(long id, long chartId, int subgroupNo, boolean outOfControl) {
        IEntityDao<ErpQaSpcSample> dao = daoProvider.daoFor(ErpQaSpcSample.class);
        ErpQaSpcSample s = dao.newEntity();
        s.orm_propValueByName("id", id);
        s.setChartId(chartId);
        s.setSubgroupNo(subgroupNo);
        s.setSampleTime(CoreMetrics.currentTimestamp());
        s.setMeasuredValues("[10]");
        s.setMean(BigDecimal.TEN);
        s.setRange(BigDecimal.ZERO);
        s.setStdDev(BigDecimal.ZERO);
        s.setIsOutOfControl(outOfControl);
        dao.saveEntity(s);
    }

    private void seedCapability(long id, long chartId, LocalDate from, LocalDate to, String capabilityLevel) {
        IEntityDao<ErpQaSpcCapability> dao = daoProvider.daoFor(ErpQaSpcCapability.class);
        ErpQaSpcCapability c = dao.newEntity();
        c.orm_propValueByName("id", id);
        c.setChartId(chartId);
        c.setPeriodFrom(from);
        c.setPeriodTo(to);
        c.setCapabilityLevel(capabilityLevel);
        dao.saveEntity(c);
    }

    private void seedSpcNcr(long id, LocalDate ncrDate, String status) {
        seedNcr(id, ncrDate, status, ErpQaConstants.NCR_SOURCE_TYPE_SPC);
    }

    private void seedNcr(long id, LocalDate ncrDate, String status, String sourceType) {
        IEntityDao<ErpQaNonConformance> dao = daoProvider.daoFor(ErpQaNonConformance.class);
        ErpQaNonConformance n = dao.newEntity();
        n.orm_propValue(1, id);
        n.setCode("NCR-SPC-" + id);
        n.setNcrDate(ncrDate);
        n.setMaterialId(1L);
        n.setSeverity("MAJOR");
        n.setStatus(status);
        n.setSourceType(sourceType);
        dao.saveEntity(n);
    }
}
