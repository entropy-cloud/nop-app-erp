package app.erp.ast.service.dashboard;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstCip;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.service.ErpAstConstants;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Description;
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
import java.util.Arrays;
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
import app.erp.common.org.ErpOrgContext;
import app.erp.common.service.DashboardUtil;

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

    @Description("资产看板 KPI（资产原值/净值/累计折旧、状态分布、本期折旧）")
    @BizQuery
    public Map<String, Object> getDashboardKpi(@Optional @Name("periodId") String periodId,
                                                IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            String period = periodId != null ? periodId : currentPeriod();
            // DB 级投影聚合（perf-ux plan 0325-2：原在役资产全量实体物化内存求和改为
            // GROUP BY status 维度分组求和——过滤后单组，Σ 原值/累计折旧一次取回）
            BigDecimal[] valueAgg = sumInServiceAssetValues(resolveOrgId(context));
            BigDecimal originalValue = valueAgg[0];
            BigDecimal accumulatedDepreciation = valueAgg[1];
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
            // DB 级 GROUP BY categoryId 投影聚合（perf-ux plan 0325-2：原全量资产物化内存分组
            // 改为 SQL 聚合，行数=科目类别数；Σ(a−b) = Σa−Σb 逐位等价），null 类别组跳过（原语义）
            QueryBean q = new QueryBean();
            q.setSourceName(ErpAstAsset.class.getName());
            q.addFilter(eq("status", ErpAstConstants.ASSET_STATUS_IN_SERVICE));
            String orgId = resolveOrgId(context);
            if (orgId != null) {
                q.addFilter(eq("orgId", orgId));
            }
            QueryFieldBean dim = QueryFieldBean.mainField("categoryId");
            QueryFieldBean sumOrig = QueryFieldBean.mainField("originalValue").sum().alias("originalValue");
            QueryFieldBean sumAcc = QueryFieldBean.mainField("accumulatedDepreciation").sum().alias("accumulatedDepreciation");
            q.setFields(Arrays.asList(dim, sumOrig, sumAcc));
            Map<String, BigDecimal> netByCategory = new LinkedHashMap<>();
            for (Map<String, Object> row : ormTemplate.findListByQuery(q)) {
                Object cid = row.get("categoryId");
                if (cid == null) continue;
                BigDecimal net = DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("originalValue")))
                        .subtract(DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("accumulatedDepreciation"))));
                netByCategory.merge((String) cid, net, BigDecimal::add);
            }
            Map<String, String> categoryNames = loadCategoryNames(netByCategory.keySet());
            List<Map<String, Object>> rows = new ArrayList<>();
            netByCategory.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder()))
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
            // DB 级 GROUP BY period 投影聚合（perf-ux plan 0325-2：原全部已执行折旧计划实体物化
            // 内存分桶改为 SQL 聚合，行数=期间数；null period 组跳过，与原语义一致）
            QueryBean q = new QueryBean();
            q.setSourceName(ErpAstDepreciationSchedule.class.getName());
            q.addFilter(eq("status", ErpAstConstants.SCHEDULE_STATUS_EXECUTED));
            QueryFieldBean dim = QueryFieldBean.mainField("period");
            QueryFieldBean sumAmt = QueryFieldBean.mainField("actualAmount").sum().alias("amount");
            q.setFields(Arrays.asList(dim, sumAmt));
            Map<String, BigDecimal> amountByPeriod = new LinkedHashMap<>();
            for (Map<String, Object> row : ormTemplate.findListByQuery(q)) {
                Object p = row.get("period");
                if (p == null) continue;
                amountByPeriod.merge((String) p, DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("amount"))), BigDecimal::add);
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
            // 明细列表型硬上限（perf-ux plan 0325-2 Decision：行级字段进列表行，cap 5000；
            // 尾部数据价值低于 OOM 风险，cap 截断登记于计划留痕区）
            List<ErpAstAsset> inServiceAssets = loadInServiceAssets(resolveOrgId(context), ALERT_LIST_MAX_ROWS);
            Set<String> assetIdsWithDepreciation = loadAssetIdsWithExecutedDepreciationInPeriod(period);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpAstAsset a : inServiceAssets) {
                if (!assetIdsWithDepreciation.contains(a.getId())) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("assetId", a.getId());
                    row.put("assetCode", a.getCode());
                    row.put("assetName", a.getName());
                    row.put("period", period);
                    row.put("originalValue", DashboardUtil.nz(a.getOriginalValue()));
                    rows.add(row);
                }
            }
            return rows;
        });
    }

    // ===================== helpers =====================

    private Map<String, String> loadCategoryNames(java.util.Set<String> categoryIds) {
        if (categoryIds.isEmpty()) return Collections.emptyMap();
        IEntityDao<ErpAstAssetCategory> dao = daoProvider.daoFor(ErpAstAssetCategory.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("id", categoryIds));
        Map<String, String> map = new HashMap<>();
        for (ErpAstAssetCategory c : dao.findAllByQuery(q)) {
            map.put(c.getId(), c.getName());
        }
        return map;
    }

    /**
     * P1-CK-ast-009（plan 2026-09-12-1000-1 Phase 1）：从 context 解析当前组织 id。
     * null-skip 契约：scope 不可解析时返回 null → 查询不加 orgId 过滤，保护单组织基线零回归。
     */
    private String resolveOrgId(IServiceContext context) {
        return ErpOrgContext.currentOrgId(context);
    }

    /** 明细列表型硬上限（perf-ux plan 0325-2：各域各自声明，不跨域引用）。 */
    private static final int ALERT_LIST_MAX_ROWS = 5000;

    private List<ErpAstAsset> loadInServiceAssets(String orgId, int limit) {
        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpAstConstants.ASSET_STATUS_IN_SERVICE));
        // P1-CK-ast-009：orgId 过滤（null-skip：scope 不可解析时不加过滤）
        if (orgId != null) {
            q.addFilter(eq("orgId", orgId));
        }
        if (limit > 0) {
            q.setLimit(limit);
        }
        return dao.findAllByQuery(q);
    }

    /** 在役资产原值/累计折旧投影聚合（GROUP BY status 单组；orgId null-skip 语义保留）。 */
    private BigDecimal[] sumInServiceAssetValues(String orgId) {
        QueryBean q = new QueryBean();
        q.setSourceName(ErpAstAsset.class.getName());
        q.addFilter(eq("status", ErpAstConstants.ASSET_STATUS_IN_SERVICE));
        if (orgId != null) {
            q.addFilter(eq("orgId", orgId));
        }
        QueryFieldBean dim = QueryFieldBean.mainField("status");
        QueryFieldBean sumOrig = QueryFieldBean.mainField("originalValue").sum().alias("originalValue");
        QueryFieldBean sumAcc = QueryFieldBean.mainField("accumulatedDepreciation").sum().alias("accumulatedDepreciation");
        q.setFields(Arrays.asList(dim, sumOrig, sumAcc));
        BigDecimal originalValue = BigDecimal.ZERO;
        BigDecimal accumulatedDepreciation = BigDecimal.ZERO;
        for (Map<String, Object> row : ormTemplate.findListByQuery(q)) {
            originalValue = originalValue.add(DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("originalValue"))));
            accumulatedDepreciation = accumulatedDepreciation.add(DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("accumulatedDepreciation"))));
        }
        return new BigDecimal[]{originalValue, accumulatedDepreciation};
    }

    private BigDecimal sumPeriodDepreciation(String period) {
        // 投影聚合（perf-ux plan 0325-2：GROUP BY period 单组 SUM，原全量物化消除）
        QueryBean q = new QueryBean();
        q.setSourceName(ErpAstDepreciationSchedule.class.getName());
        q.addFilter(eq("status", ErpAstConstants.SCHEDULE_STATUS_EXECUTED));
        q.addFilter(eq("period", period));
        QueryFieldBean dim = QueryFieldBean.mainField("period");
        QueryFieldBean sumAmt = QueryFieldBean.mainField("actualAmount").sum().alias("amount");
        q.setFields(Arrays.asList(dim, sumAmt));
        BigDecimal sum = BigDecimal.ZERO;
        for (Map<String, Object> row : ormTemplate.findListByQuery(q)) {
            sum = sum.add(DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("amount"))));
        }
        return sum;
    }

    private BigDecimal sumCipBalance() {
        // 投影聚合（perf-ux plan 0325-2：GROUP BY isCompleted 单组 SUM）
        QueryBean q = new QueryBean();
        q.setSourceName(ErpAstCip.class.getName());
        q.addFilter(eq("isCompleted", Boolean.FALSE));
        QueryFieldBean dim = QueryFieldBean.mainField("isCompleted");
        QueryFieldBean sumCost = QueryFieldBean.mainField("accumulatedCost").sum().alias("cipBalance");
        q.setFields(Arrays.asList(dim, sumCost));
        BigDecimal sum = BigDecimal.ZERO;
        for (Map<String, Object> row : ormTemplate.findListByQuery(q)) {
            sum = sum.add(DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("cipBalance"))));
        }
        return sum;
    }

    private Set<String> loadAssetIdsWithExecutedDepreciationInPeriod(String period) {
        // 投影去重（perf-ux plan 0325-2：GROUP BY assetId 维度投影替代全量物化，行数=资产数）
        QueryBean q = new QueryBean();
        q.setSourceName(ErpAstDepreciationSchedule.class.getName());
        q.addFilter(eq("status", ErpAstConstants.SCHEDULE_STATUS_EXECUTED));
        q.addFilter(eq("period", period));
        QueryFieldBean dim = QueryFieldBean.mainField("assetId");
        q.setFields(Arrays.asList(dim));
        Set<String> ids = new HashSet<>();
        for (Map<String, Object> row : ormTemplate.findListByQuery(q)) {
            Object aid = row.get("assetId");
            if (aid != null) ids.add((String) aid);
        }
        return ids;
    }

    private static String currentPeriod() {
        LocalDate today = CoreMetrics.currentDate();
        return today.getYear() + "-" + String.format("%02d", today.getMonthValue());
    }
}
