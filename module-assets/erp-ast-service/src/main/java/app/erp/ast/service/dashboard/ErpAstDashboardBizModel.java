package app.erp.ast.service.dashboard;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstCip;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.service.ErpAstConstants;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.beans.query.QueryFieldBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 资产看板聚合入口（{@code dashboards.md §5}）。服务型 BizObject（非实体聚合），
 * 注入 {@link IDaoProvider}/{@link IOrmTemplate} 经 {@link QueryBean} 过滤后内存聚合，
 * 镜像 {@code ErpFinDashboardBizModel} 范式。
 *
 * <p>KPI 口径：资产原值合计取自 {@link ErpAstAsset}（IN_SERVICE Σ originalValue）；
 * 累计折旧取自 {@link ErpAstAsset} Σ accumulatedDepreciation；资产净值 = 原值 − 累计折旧；
 * 本期折旧取自 {@link ErpAstDepreciationSchedule}（EXECUTED 期内 Σ actualAmount）；
 * 在建工程余额取自 {@link ErpAstCip}（未转固 Σ accumulatedCost）。
 */
@BizModel("ErpAstDashboard")
public class ErpAstDashboardBizModel {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @BizQuery
    public Map<String, Object> getDashboardKpi(@Optional @Name("periodId") String periodId,
                                                IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            String period = periodId != null ? periodId : currentPeriod();
            List<ErpAstAsset> inServiceAssets = loadInServiceAssets();
            BigDecimal originalValue = BigDecimal.ZERO;
            BigDecimal accumulatedDepreciation = BigDecimal.ZERO;
            for (ErpAstAsset a : inServiceAssets) {
                originalValue = originalValue.add(nz(a.getOriginalValue()));
                accumulatedDepreciation = accumulatedDepreciation.add(nz(a.getAccumulatedDepreciation()));
            }
            BigDecimal netBookValue = originalValue.subtract(accumulatedDepreciation);
            BigDecimal periodDepreciation = sumPeriodDepreciation(period);
            BigDecimal cipBalance = sumCipBalance();

            Map<String, Object> kpi = new LinkedHashMap<>();
            kpi.put("period", period);
            kpi.put("originalValue", originalValue);
            kpi.put("accumulatedDepreciation", accumulatedDepreciation);
            kpi.put("netBookValue", netBookValue);
            kpi.put("periodDepreciation", periodDepreciation);
            kpi.put("cipBalance", cipBalance);
            return kpi;
        });
    }

    /** 资产类别分布（按 categoryId 聚合净值）。 */
    @BizQuery
    public List<Map<String, Object>> getAssetCategoryDistribution(IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            List<ErpAstAsset> assets = loadInServiceAssets();
            Map<Long, BigDecimal> netByCategory = new LinkedHashMap<>();
            for (ErpAstAsset a : assets) {
                Long cid = a.getCategoryId();
                if (cid == null) continue;
                BigDecimal net = nz(a.getOriginalValue()).subtract(nz(a.getAccumulatedDepreciation()));
                netByCategory.merge(cid, net, BigDecimal::add);
            }
            Map<Long, String> categoryNames = loadCategoryNames(netByCategory.keySet());
            List<Map<String, Object>> rows = new ArrayList<>();
            netByCategory.entrySet().stream()
                    .sorted(Map.Entry.<Long, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                    .forEach(e -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("categoryId", e.getKey());
                        row.put("categoryName", categoryNames.get(e.getKey()));
                        row.put("netBookValue", e.getValue());
                        rows.add(row);
                    });
            return rows;
        });
    }

    /** 近 12 月折旧趋势（按 period 字符串聚合 actualAmount，仅 EXECUTED）。 */
    @BizQuery
    public List<Map<String, Object>> getDashboardTrend(@Optional @Name("months") Integer months,
                                                        IServiceContext context) {
        int n = months == null || months <= 0 ? 12 : months;
        LocalDate today = CoreMetrics.currentDate();
        LocalDate from = today.minusMonths(n - 1L).withDayOfMonth(1);
        return ormTemplate.runInSession(session -> {
            List<ErpAstDepreciationSchedule> schedules = loadExecutedSchedulesInRange(from, today);
            Map<String, BigDecimal> amountByPeriod = new LinkedHashMap<>();
            for (ErpAstDepreciationSchedule s : schedules) {
                String p = s.getPeriod();
                if (p == null) continue;
                amountByPeriod.merge(p, nz(s.getActualAmount()), BigDecimal::add);
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                LocalDate m = from.plusMonths(i);
                String key = m.getYear() + "-" + String.format("%02d", m.getMonthValue());
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("period", key);
                row.put("depreciationAmount", amountByPeriod.getOrDefault(key, BigDecimal.ZERO));
                rows.add(row);
            }
            return rows;
        });
    }

    /**
     * 折旧未计提预警：IN_SERVICE 资产中本期（当前 period）无 EXECUTED 折旧计划条目者。
     */
    @BizQuery
    public List<Map<String, Object>> findDepreciationMissingAlert(IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            String period = currentPeriod();
            List<ErpAstAsset> inServiceAssets = loadInServiceAssets();
            Set<Long> assetIdsWithDepreciation = loadAssetIdsWithExecutedDepreciationInPeriod(period);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpAstAsset a : inServiceAssets) {
                if (!assetIdsWithDepreciation.contains(a.getId())) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("assetId", a.getId());
                    row.put("assetCode", a.getCode());
                    row.put("assetName", a.getName());
                    row.put("period", period);
                    row.put("originalValue", nz(a.getOriginalValue()));
                    rows.add(row);
                }
            }
            return rows;
        });
    }

    // ===================== helpers =====================

    private Map<Long, String> loadCategoryNames(java.util.Set<Long> categoryIds) {
        if (categoryIds.isEmpty()) return Collections.emptyMap();
        IEntityDao<ErpAstAssetCategory> dao = daoProvider.daoFor(ErpAstAssetCategory.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("id", categoryIds));
        Map<Long, String> map = new HashMap<>();
        for (ErpAstAssetCategory c : dao.findAllByQuery(q)) {
            map.put(c.getId(), c.getName());
        }
        return map;
    }

    private List<ErpAstAsset> loadInServiceAssets() {
        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpAstConstants.ASSET_STATUS_IN_SERVICE));
        return dao.findAllByQuery(q);
    }

    private BigDecimal sumPeriodDepreciation(String period) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpAstConstants.SCHEDULE_STATUS_EXECUTED));
        q.addFilter(eq("period", period));
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpAstDepreciationSchedule s : dao.findAllByQuery(q)) {
            sum = sum.add(nz(s.getActualAmount()));
        }
        return sum;
    }

    private BigDecimal sumCipBalance() {
        IEntityDao<ErpAstCip> dao = daoProvider.daoFor(ErpAstCip.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("isCompleted", Boolean.FALSE));
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpAstCip c : dao.findAllByQuery(q)) {
            sum = sum.add(nz(c.getAccumulatedCost()));
        }
        return sum;
    }

    private List<ErpAstDepreciationSchedule> loadExecutedSchedulesInRange(LocalDate from, LocalDate to) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpAstConstants.SCHEDULE_STATUS_EXECUTED));
        return dao.findAllByQuery(q);
    }

    private Set<Long> loadAssetIdsWithExecutedDepreciationInPeriod(String period) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpAstConstants.SCHEDULE_STATUS_EXECUTED));
        q.addFilter(eq("period", period));
        Set<Long> ids = new HashSet<>();
        for (ErpAstDepreciationSchedule s : dao.findAllByQuery(q)) {
            if (s.getAssetId() != null) ids.add(s.getAssetId());
        }
        return ids;
    }

    private static String currentPeriod() {
        LocalDate today = CoreMetrics.currentDate();
        return today.getYear() + "-" + String.format("%02d", today.getMonthValue());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
