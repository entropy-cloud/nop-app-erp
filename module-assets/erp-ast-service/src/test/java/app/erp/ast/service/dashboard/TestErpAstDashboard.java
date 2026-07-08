package app.erp.ast.service.dashboard;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstCip;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.service.ErpAstConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资产看板聚合（{@code ErpAstDashboard__*}）集成测试。覆盖：原值/累计折旧/净值/本期折旧/在建工程余额算术、
 * 类别分布、折旧未计提预警触发/不触发两路径、空数据集零值。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstDashboard extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();
    private static final String CURRENT_PERIOD =
            CoreMetrics.currentDate().getYear() + "-" + String.format("%02d", CoreMetrics.currentDate().getMonthValue());

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpAstDashboardBizModel dashboardBiz;

    @Test
    public void testKpiEmptyDatasetReturnsZeros() {
        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, CTX);
        assertEquals(0, ((BigDecimal) kpi.get("originalValue")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) kpi.get("accumulatedDepreciation")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) kpi.get("netBookValue")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) kpi.get("periodDepreciation")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) kpi.get("cipBalance")).compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testKpiAggregationArithmetic() {
        ormTemplate.runInSession(() -> {
            // 资产 A: 原值 1000, 累计折旧 200 → 净值 800
            seedAsset(101L, 11L, new BigDecimal("1000"), new BigDecimal("200"),
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            // 资产 B: 原值 3000, 累计折旧 500 → 净值 2500
            seedAsset(102L, 12L, new BigDecimal("3000"), new BigDecimal("500"),
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            // 非 IN_SERVICE 资产不计入（SCRAPPED）
            seedAsset(103L, 11L, new BigDecimal("9999"), new BigDecimal("0"),
                    ErpAstConstants.ASSET_STATUS_SCRAPPED);
            // 本期已计提折旧：A 分摊 50, B 分摊 80 → 合计 130
            seedDepreciationSchedule(201L, 101L, CURRENT_PERIOD,
                    new BigDecimal("50"), ErpAstConstants.SCHEDULE_STATUS_EXECUTED);
            seedDepreciationSchedule(202L, 102L, CURRENT_PERIOD,
                    new BigDecimal("80"), ErpAstConstants.SCHEDULE_STATUS_EXECUTED);
            // PENDING 状态不计入本期折旧
            seedDepreciationSchedule(203L, 101L, CURRENT_PERIOD,
                    new BigDecimal("999"), ErpAstConstants.SCHEDULE_STATUS_PENDING);
            // 在建工程：未转固 600 + 已转固 100（不计入）
            seedCip(301L, new BigDecimal("600"), false);
            seedCip(302L, new BigDecimal("100"), true);
        });

        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, CTX);
        // 原值 1000+3000=4000（SCRAPPED 不计入）
        assertEquals(0, ((BigDecimal) kpi.get("originalValue")).compareTo(new BigDecimal("4000")));
        // 累计折旧 200+500=700
        assertEquals(0, ((BigDecimal) kpi.get("accumulatedDepreciation")).compareTo(new BigDecimal("700")));
        // 净值 4000-700=3300
        assertEquals(0, ((BigDecimal) kpi.get("netBookValue")).compareTo(new BigDecimal("3300")));
        // 本期折旧 50+80=130（PENDING 不计入）
        assertEquals(0, ((BigDecimal) kpi.get("periodDepreciation")).compareTo(new BigDecimal("130")));
        // 在建工程 600（已转固不计入）
        assertEquals(0, ((BigDecimal) kpi.get("cipBalance")).compareTo(new BigDecimal("600")));
    }

    @Test
    public void testAssetCategoryDistribution() {
        ormTemplate.runInSession(() -> {
            // 类别 21: 原值 1000 折旧 200 → 净值 800
            seedAsset(111L, 21L, new BigDecimal("1000"), new BigDecimal("200"),
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            // 类别 21: 原值 500 折旧 100 → 净值 400（类别 21 合计 1200）
            seedAsset(112L, 21L, new BigDecimal("500"), new BigDecimal("100"),
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            // 类别 22: 原值 2000 折旧 0 → 净值 2000
            seedAsset(113L, 22L, new BigDecimal("2000"), new BigDecimal("0"),
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });
        List<Map<String, Object>> dist = dashboardBiz.getAssetCategoryDistribution(CTX);
        assertEquals(2, dist.size(), "2 个类别");
        // 类别 22 净值 2000 > 类别 21 净值 1200 → 22 排第一
        assertEquals(22L, dist.get(0).get("categoryId"));
        assertEquals(0, ((BigDecimal) dist.get(0).get("netBookValue")).compareTo(new BigDecimal("2000")));
        assertEquals(21L, dist.get(1).get("categoryId"));
        assertEquals(0, ((BigDecimal) dist.get(1).get("netBookValue")).compareTo(new BigDecimal("1200")));
    }

    @Test
    public void testDepreciationMissingAlertTriggers() {
        ormTemplate.runInSession(() -> {
            // 资产 A: IN_SERVICE 但本期无 EXECUTED 折旧 → 触发预警
            seedAsset(121L, 31L, new BigDecimal("1000"), new BigDecimal("0"),
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            // 资产 B: IN_SERVICE 且本期有 EXECUTED 折旧 → 不触发
            seedAsset(122L, 31L, new BigDecimal("2000"), new BigDecimal("0"),
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            seedDepreciationSchedule(221L, 122L, CURRENT_PERIOD,
                    new BigDecimal("100"), ErpAstConstants.SCHEDULE_STATUS_EXECUTED);
            // 资产 C: 非 IN_SERVICE → 不计入
            seedAsset(123L, 31L, new BigDecimal("3000"), new BigDecimal("0"),
                    ErpAstConstants.ASSET_STATUS_IDLE);
        });
        List<Map<String, Object>> alerts = dashboardBiz.findDepreciationMissingAlert(CTX);
        assertEquals(1, alerts.size(), "仅资产 A 触发预警");
        assertEquals(121L, alerts.get(0).get("assetId"));
    }

    @Test
    public void testTrendMonthlySeries() {
        String lastMonth = minusMonthsPeriod(1);
        ormTemplate.runInSession(() -> {
            seedDepreciationSchedule(231L, 141L, CURRENT_PERIOD,
                    new BigDecimal("150"), ErpAstConstants.SCHEDULE_STATUS_EXECUTED);
            seedDepreciationSchedule(232L, 142L, lastMonth,
                    new BigDecimal("250"), ErpAstConstants.SCHEDULE_STATUS_EXECUTED);
        });
        List<Map<String, Object>> trend = dashboardBiz.getDashboardTrend(2, CTX);
        assertEquals(2, trend.size(), "近 2 月序列长度");
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : trend) {
            total = total.add((BigDecimal) row.get("depreciationAmount"));
        }
        assertEquals(0, total.compareTo(new BigDecimal("400")), "近 2 月折旧合计 150+250=400");
    }

    // ---------- helpers ----------

    private void seedAsset(long id, long categoryId, BigDecimal originalValue,
                           BigDecimal accumulatedDepreciation, String status) {
        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        ErpAstAsset a = dao.newEntity();
        a.orm_propValue(1, id);
        a.setCode("AST-" + id);
        a.setName("资产-" + id);
        a.setOrgId(1L);
        a.setCategoryId(categoryId);
        a.setAcquisitionDate(LocalDate.of(2026, 1, 1));
        a.setCurrencyId(1L);
        a.setOriginalValue(originalValue);
        a.setCurrentValue(originalValue);
        a.setResidualValue(BigDecimal.ZERO);
        a.setDepreciationMethod("STRAIGHT_LINE");
        a.setUsefulLifeMonths(60);
        a.setAccumulatedDepreciation(accumulatedDepreciation);
        a.setNetBookValue(originalValue.subtract(accumulatedDepreciation));
        a.setStatus(status);
        dao.saveEntity(a);
    }

    private void seedDepreciationSchedule(long id, long assetId, String period,
                                          BigDecimal actualAmount, String status) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        ErpAstDepreciationSchedule s = dao.newEntity();
        s.orm_propValue(1, id);
        s.setAssetId(assetId);
        s.setOrgId(1L);
        s.setPeriod(period);
        s.setPlannedAmount(actualAmount);
        s.setActualAmount(actualAmount);
        s.setAccumulatedDepreciation(actualAmount);
        s.setNetBookValue(actualAmount);
        s.setStatus(status);
        s.setBusinessDate(CoreMetrics.currentDate());
        dao.saveEntity(s);
    }

    private void seedCip(long id, BigDecimal balance, boolean completed) {
        IEntityDao<ErpAstCip> dao = daoProvider.daoFor(ErpAstCip.class);
        ErpAstCip c = dao.newEntity();
        c.orm_propValue(1, id);
        c.setCode("CIP-" + id);
        c.setName("在建工程-" + id);
        c.setOrgId(1L);
        c.setCategoryId(1L);
        c.setBusinessDate(LocalDate.of(2026, 1, 1));
        c.setAccumulatedCost(balance);
        c.setIsCompleted(completed);
        c.setStatus(ErpAstConstants.ASSET_STATUS_DRAFT);
        c.setCurrencyId(1L);
        c.setExchangeRate(BigDecimal.ONE);
        c.setAmountSource(balance);
        c.setAmountFunctional(balance);
        dao.saveEntity(c);
    }

    private static String minusMonthsPeriod(int monthsAgo) {
        LocalDate d = CoreMetrics.currentDate().minusMonths(monthsAgo);
        return d.getYear() + "-" + String.format("%02d", d.getMonthValue());
    }
}
