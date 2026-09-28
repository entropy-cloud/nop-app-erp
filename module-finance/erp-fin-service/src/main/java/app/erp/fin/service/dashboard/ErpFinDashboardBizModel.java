package app.erp.fin.service.dashboard;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinFundAccount;
import app.erp.fin.dao.entity.ErpFinGlBalance;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Description;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.beans.query.QueryFieldBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;
import app.erp.common.service.DashboardUtil;

/**
 * 财务看板聚合入口（{@code dashboards.md §4}）。服务型 BizObject（非实体聚合），
 * 注入 {@link IDaoProvider}/{@link IOrmTemplate} 经 {@link QueryBean} 过滤后内存聚合，
 * 镜像 {@code ErpFinReportBizModel} 域隔离范式。
 *
 * <p>KPI 口径：本期收入/支出/净利润取自 {@link ErpFinGlBalance} 损益类科目本期发生净额
 * （对齐 {@code ErpFinReportBizModel.periodActivity}）；银行存款余额取自 {@link ErpFinFundAccount}
 * （accountType=BANK）；应收/应付余额取自 {@link ErpFinArApItem}（OPEN+PARTIAL openAmountFunctional）。
 */
@BizModel("ErpFinDashboard")
public class ErpFinDashboardBizModel {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Description("财务看板 KPI（本期收入/支出/净利润、银行存款、应收/应付余额）")
    @BizQuery
    public Map<String, Object> getDashboardKpi(@Optional @Name("periodId") String periodId,
                                                IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            Map<String, Object> kpi = new LinkedHashMap<>();
            // DB 级 GROUP BY subjectId 聚合（perf-ux plan 0325-2：原全量 GlBalance 实体物化+
            // to-one 科目导航内存分类改为投影聚合（行数=科目数）+ 科目映射内存桶装；
            // activity 符号按科目 direction 常量，Σ±(d−c) = ±(Σd−Σc) 逐位等价）
            // perf-ux plan 0835-1 E2：三腿共用同一 periodId 基准的 org/schema scope，单次解析传递
            // （原 sumArApOpen×2 + 损益腿各重复 resolvePeriodOrgId+resolvePrimarySchemaId；
            // orgId 不可解析跳过 filter 的语义逐字保留）
            OrgSchemaScope scope = resolveOrgAndSchemaScope(periodId);
            PnlAgg pnl = aggPnlActivityBySubject(periodId, scope);
            BigDecimal revenue = pnl.revenue;
            BigDecimal expense = pnl.expense;
            kpi.put("periodId", periodId);
            kpi.put("revenue", revenue);
            kpi.put("expense", expense);
            kpi.put("netProfit", revenue.subtract(expense));
            kpi.put("bankBalance", sumBankBalance());
            kpi.put("arBalance", sumArApOpen(ErpFinConstants.DIRECTION_RECEIVABLE, scope));
            kpi.put("apBalance", sumArApOpen(ErpFinConstants.DIRECTION_PAYABLE, scope));
            return kpi;
        });
    }

    @BizQuery
    public List<Map<String, Object>> getDashboardTrend(@Optional @Name("months") Integer months,
                                                        IServiceContext context) {
        int n = months == null || months <= 0 ? 12 : months;
        LocalDate today = CoreMetrics.currentDate();
        LocalDate from = today.minusMonths(n - 1L).withDayOfMonth(1);
        return ormTemplate.runInSession(session -> {
            // DB 级 GROUP BY (subjectId, periodId) 多维投影聚合（perf-ux plan 0325-2 Decision-c fin 分支：
            // GlBalance 无 businessDate，月桶 key 经 period→(year,month) 内存映射；period.startDate
            // 空值跳过语义显式保留；多维分组在 StockLedger aggregateLedgerUpTo 先例形态内）
            Map<String, BigDecimal> revenueByMonth = new LinkedHashMap<>();
            Map<String, BigDecimal> expenseByMonth = new LinkedHashMap<>();
            TrendAggHolder agg = aggPnlActivityBySubjectPeriod(from, today);
            for (Map.Entry<String, SubjectPeriodActivity> e : agg.activityBySubjectPeriod.entrySet()) {
                SubjectPeriodActivity spa = e.getValue();
                String cls = spa.subjectClass;
                if (cls == null) continue;
                if (spa.periodKey == null) continue;
                if (spa.periodStart == null || spa.periodStart.isBefore(from.minusDays(1))) continue;
                BigDecimal activity = spa.activity();
                if (ErpFinConstants.SUBJECT_CLASS_INCOME.equals(cls)) {
                    revenueByMonth.merge(spa.periodKey, activity, BigDecimal::add);
                } else if (ErpFinConstants.SUBJECT_CLASS_EXPENSE.equals(cls)
                        || ErpFinConstants.SUBJECT_CLASS_COST.equals(cls)) {
                    expenseByMonth.merge(spa.periodKey, activity, BigDecimal::add);
                }
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                LocalDate m = from.plusMonths(i);
                String key = m.getYear() + "-" + String.format("%02d", m.getMonthValue());
                BigDecimal rev = revenueByMonth.getOrDefault(key, BigDecimal.ZERO);
                BigDecimal exp = expenseByMonth.getOrDefault(key, BigDecimal.ZERO);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("month", key);
                row.put("revenue", rev);
                row.put("expense", exp);
                row.put("netProfit", rev.subtract(exp));
                rows.add(row);
            }
            return rows;
        });
    }

    /**
     * 现金流预警：银行余额 < 阈值（{@code erp-dash.fin-cash-flow-threshold}，默认 0=关闭）。
     * 阈值 ≤0 时不触发预警，返回空列表。
     */
    @BizQuery
    public List<Map<String, Object>> findCashFlowAlert(IServiceContext context) {
        BigDecimal threshold = AppConfig.var(
                ErpFinConstants.CONFIG_DASH_FIN_CASH_FLOW_THRESHOLD,
                ErpFinConstants.DEFAULT_DASH_FIN_CASH_FLOW_THRESHOLD);
        if (threshold == null || threshold.signum() <= 0) {
            return Collections.emptyList();
        }
        return ormTemplate.runInSession(session -> {
            BigDecimal bank = sumBankBalance();
            if (bank.compareTo(threshold) >= 0) {
                return Collections.emptyList();
            }
            Map<String, Object> alert = new LinkedHashMap<>();
            alert.put("alertType", "CASH_FLOW_LOW");
            alert.put("bankBalance", bank);
            alert.put("threshold", threshold);
            alert.put("shortfall", threshold.subtract(bank));
            List<Map<String, Object>> rows = new ArrayList<>();
            rows.add(alert);
            return rows;
        });
    }

    // ===================== helpers =====================

    private List<ErpFinGlBalance> loadGlBalances(String periodId) {
        IEntityDao<ErpFinGlBalance> dao = daoProvider.daoFor(ErpFinGlBalance.class);
        if (periodId == null) {
            // periodId 缺省时限定最近一个会计期间，避免全表回退（原全表加载会物化全表）。
            // 取最近期间的 id 后按该期间过滤；无任何期间时返回空（KPI 退化为 0）。
            String latestPeriodId = findLatestPeriodId();
            if (latestPeriodId == null) {
                return Collections.emptyList();
            }
            QueryBean q = new QueryBean();
            q.addFilter(eq("periodId", latestPeriodId));
            applyOrgAndSchemaScope(q, latestPeriodId);
            return dao.findAllByQuery(q);
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        applyOrgAndSchemaScope(q, periodId);
        return dao.findAllByQuery(q);
    }

    private String findLatestPeriodId() {
        IEntityDao<ErpFinAccountingPeriod> pDao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean q = new QueryBean();
        q.addOrderField("startDate", true);
        q.setLimit(1);
        List<ErpFinAccountingPeriod> latest = pDao.findAllByQuery(q);
        return latest.isEmpty() ? null : latest.get(0).getId();
    }

    private List<ErpFinGlBalance> loadGlBalancesInRange(LocalDate from, LocalDate to) {
        IEntityDao<ErpFinAccountingPeriod> pDao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean pq = new QueryBean();
        pq.addFilter(ge("startDate", from));
        pq.addFilter(le("startDate", to));
        List<ErpFinAccountingPeriod> periods = pDao.findAllByQuery(pq);
        if (periods.isEmpty()) return Collections.emptyList();
        List<String> periodIds = new ArrayList<>();
        for (ErpFinAccountingPeriod p : periods) periodIds.add(p.getId());
        IEntityDao<ErpFinGlBalance> dao = daoProvider.daoFor(ErpFinGlBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("periodId", periodIds));
        // 多账套隔离：按范围内首个期间的所属组织 + 主账套过滤（同期范围假定同组织）
        String orgId = periods.get(0).getOrgId();
        if (orgId != null) {
            q.addFilter(eq("orgId", orgId));
            String schemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
            if (schemaId != null) {
                q.addFilter(eq("acctSchemaId", schemaId));
            }
        }
        return dao.findAllByQuery(q);
    }

    /** in() 分块上限（perf-ux plan 0325-2 分块纪律）。 */
    private static final int IN_CLAUSE_CHUNK = 500;

    /** 损益聚合结果。 */
    private static final class PnlAgg {
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
    }

    /** 单科目-期间聚合行。 */
    private static final class SubjectPeriodActivity {
        String subjectClass;
        String periodKey;
        LocalDate periodStart;
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        boolean debitDirection;

        BigDecimal activity() {
            return debitDirection ? debit.subtract(credit) : credit.subtract(debit);
        }
    }

    /** 单期间损益聚合（KPI 用）：GROUP BY subjectId 投影 + 科目映射，收入/支出桶装。 */
    private PnlAgg aggPnlActivityBySubject(String periodId, OrgSchemaScope scope) {
        PnlAgg agg = new PnlAgg();
        // 与 loadGlBalances 相同的过滤（periodId 缺省取最近期间 + org/schema scope），投影聚合替代实体物化
        String effectivePeriodId = periodId;
        if (effectivePeriodId == null) {
            effectivePeriodId = findLatestPeriodId();
            if (effectivePeriodId == null) return agg;
        }
        QueryBean q = new QueryBean();
        q.setSourceName(ErpFinGlBalance.class.getName());
        q.addFilter(eq("periodId", effectivePeriodId));
        applyScope(q, scope);
        QueryFieldBean dim = QueryFieldBean.mainField("subjectId");
        QueryFieldBean sumDebit = QueryFieldBean.mainField("periodDebit").sum().alias("periodDebit");
        QueryFieldBean sumCredit = QueryFieldBean.mainField("periodCredit").sum().alias("periodCredit");
        q.setFields(Arrays.asList(dim, sumDebit, sumCredit));

        Set<String> subjectIds = new HashSet<>();
        Map<String, BigDecimal[]> debitCreditBySubject = new HashMap<>();
        for (Map<String, Object> row : ormTemplate.findListByQuery(q)) {
            Object sid = row.get("subjectId");
            if (sid == null) continue;
            String subjectId = (String) sid;
            BigDecimal d = DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("periodDebit")));
            BigDecimal c = DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("periodCredit")));
            subjectIds.add(subjectId);
            BigDecimal[] dc = debitCreditBySubject.computeIfAbsent(subjectId, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            dc[0] = dc[0].add(d);
            dc[1] = dc[1].add(c);
        }
        // 科目映射（科目表小，分块单次加载）
        Map<String, ErpMdSubject> subjectById = loadSubjects(subjectIds);
        for (Map.Entry<String, BigDecimal[]> e : debitCreditBySubject.entrySet()) {
            ErpMdSubject s = subjectById.get(e.getKey());
            if (s == null || s.getSubjectClass() == null) continue;
            BigDecimal activity = periodActivitySums(e.getValue()[0], e.getValue()[1], s);
            String cls = s.getSubjectClass();
            if (ErpFinConstants.SUBJECT_CLASS_INCOME.equals(cls)) {
                agg.revenue = agg.revenue.add(activity);
            } else if (ErpFinConstants.SUBJECT_CLASS_EXPENSE.equals(cls)
                    || ErpFinConstants.SUBJECT_CLASS_COST.equals(cls)) {
                agg.expense = agg.expense.add(activity);
            }
        }
        return agg;
    }

    /** 范围内多期间损益聚合（Trend 用）：GROUP BY (subjectId, periodId) 多维投影 + period 映射。 */
    private TrendAggHolder aggPnlActivityBySubjectPeriod(LocalDate from, LocalDate to) {
        TrendAggHolder holder = new TrendAggHolder();
        IEntityDao<ErpFinAccountingPeriod> pDao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean pq = new QueryBean();
        pq.addFilter(ge("startDate", from));
        pq.addFilter(le("startDate", to));
        List<ErpFinAccountingPeriod> periods = pDao.findAllByQuery(pq);
        if (periods.isEmpty()) return holder;
        List<String> periodIds = new ArrayList<>();
        for (ErpFinAccountingPeriod p : periods) periodIds.add(p.getId());
        // period 元数据映射（year/month/startDate），保留 startDate 空值跳过语义
        Map<String, ErpFinAccountingPeriod> periodById = new HashMap<>();
        for (ErpFinAccountingPeriod p : periods) periodById.put(p.getId(), p);

        QueryBean q = new QueryBean();
        q.setSourceName(ErpFinGlBalance.class.getName());
        q.addFilter(in("periodId", periodIds));
        // 多账套隔离：按范围内首个期间的所属组织 + 主账套过滤（同期范围假定同组织）——原语义逐字保留
        String orgId = periods.get(0).getOrgId();
        if (orgId != null) {
            q.addFilter(eq("orgId", orgId));
            String schemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
            if (schemaId != null) {
                q.addFilter(eq("acctSchemaId", schemaId));
            }
        }
        QueryFieldBean dimSubject = QueryFieldBean.mainField("subjectId");
        QueryFieldBean dimPeriod = QueryFieldBean.mainField("periodId");
        QueryFieldBean sumDebit = QueryFieldBean.mainField("periodDebit").sum().alias("periodDebit");
        QueryFieldBean sumCredit = QueryFieldBean.mainField("periodCredit").sum().alias("periodCredit");
        q.setFields(Arrays.asList(dimSubject, dimPeriod, sumDebit, sumCredit));

        Set<String> subjectIds = new HashSet<>();
        for (Map<String, Object> row : ormTemplate.findListByQuery(q)) {
            Object sid = row.get("subjectId");
            Object pid = row.get("periodId");
            if (sid == null || pid == null) continue;
            subjectIds.add((String) sid);
            SubjectPeriodActivity spa = holder.activityBySubjectPeriod.computeIfAbsent(
                    sid + "#" + pid, k -> new SubjectPeriodActivity());
            spa.debit = spa.debit.add(DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("periodDebit"))));
            spa.credit = spa.credit.add(DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("periodCredit"))));
        }
        Map<String, ErpMdSubject> subjectById = loadSubjects(subjectIds);
        for (Map.Entry<String, SubjectPeriodActivity> e : holder.activityBySubjectPeriod.entrySet()) {
            SubjectPeriodActivity spa = e.getValue();
            String sid = e.getKey().split("#", 2)[0];
            ErpMdSubject s = subjectById.get(sid);
            if (s == null) continue;
            spa.subjectClass = s.getSubjectClass();
            spa.debitDirection = ErpFinConstants.DC_DEBIT.equals(s.getDirection());
            ErpFinAccountingPeriod period = periodById.get(e.getKey().split("#", 2)[1]);
            if (period == null || period.getYear() == null || period.getMonth() == null) continue;
            spa.periodKey = period.getYear() + "-" + String.format("%02d", period.getMonth());
            spa.periodStart = period.getStartDate();
        }
        return holder;
    }

    /** Trend 聚合载体。 */
    private static final class TrendAggHolder {
        final Map<String, SubjectPeriodActivity> activityBySubjectPeriod = new LinkedHashMap<>();
    }

    /** 科目批量预载（分块 in），供投影聚合后映射 subjectClass/direction。 */
    private Map<String, ErpMdSubject> loadSubjects(Set<String> subjectIds) {
        Map<String, ErpMdSubject> map = new HashMap<>();
        if (subjectIds.isEmpty()) return map;
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        List<String> ids = new ArrayList<>(subjectIds);
        for (int i = 0; i < ids.size(); i += IN_CLAUSE_CHUNK) {
            QueryBean q = new QueryBean();
            q.addFilter(in("id", ids.subList(i, Math.min(ids.size(), i + IN_CLAUSE_CHUNK))));
            for (ErpMdSubject s : dao.findAllByQuery(q)) {
                map.put(s.getId(), s);
            }
        }
        return map;
    }

    /** activity 派生（与 periodActivity(ErpFinGlBalance, ErpMdSubject) 同式，输入为聚合和）。 */
    private static BigDecimal periodActivitySums(BigDecimal debitSum, BigDecimal creditSum, ErpMdSubject s) {
        return ErpFinConstants.DC_DEBIT.equals(s.getDirection())
                ? debitSum.subtract(creditSum)
                : creditSum.subtract(debitSum);
    }

    private BigDecimal sumBankBalance() {
        // 按有界维度（currencyId）分组 SQL sum 后内存汇总（perf-ux plan 0325-2：规避无维度 SUM
        // 被强制注入主键维度的平台坑；null 币种行单独成组被计入，与原全量求和等价）
        QueryBean q = new QueryBean();
        q.setSourceName(ErpFinFundAccount.class.getName());
        q.addFilter(eq("accountType", ErpFinConstants.FUND_ACCOUNT_TYPE_BANK));
        QueryFieldBean dim = QueryFieldBean.mainField("currencyId");
        QueryFieldBean sumBal = QueryFieldBean.mainField("currentBalance").sum().alias("bankBalance");
        q.setFields(Arrays.asList(dim, sumBal));
        BigDecimal sum = BigDecimal.ZERO;
        for (Map<String, Object> row : ormTemplate.findListByQuery(q)) {
            sum = sum.add(DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("bankBalance"))));
        }
        return sum;
    }

    private BigDecimal sumArApOpen(String direction, OrgSchemaScope scope) {
        // 按有界维度（status，过滤后 ≤2 组）分组 SQL sum 后内存汇总（perf-ux plan 0325-2：
        // 原 OPEN+PARTIAL 全量实体物化内存求和改为投影聚合；org/schema scope 语义逐字保留，
        // perf-ux plan 0835-1 E2 起 scope 由 KPI 入口单次解析传入）
        QueryBean q = new QueryBean();
        q.setSourceName(ErpFinArApItem.class.getName());
        q.addFilter(eq("direction", direction));
        q.addFilter(in("status", Arrays.asList(
                ErpFinConstants.AR_AP_STATUS_OPEN,
                ErpFinConstants.AR_AP_STATUS_PARTIAL)));
        applyScope(q, scope);
        QueryFieldBean dim = QueryFieldBean.mainField("status");
        QueryFieldBean sumOpen = QueryFieldBean.mainField("openAmountFunctional").sum().alias("openAmount");
        q.setFields(Arrays.asList(dim, sumOpen));
        BigDecimal sum = BigDecimal.ZERO;
        for (Map<String, Object> row : ormTemplate.findListByQuery(q)) {
            sum = sum.add(DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("openAmount"))));
        }
        return sum;
    }

    // ===================== 多账套/多组织读路径隔离 scope（P1-MA2-095）=====================
    // scope 不可解析时（period.orgId 为空等）跳过 filter，保护单组织基线零回归。

    private String resolvePeriodOrgId(String periodId) {
        if (periodId == null) {
            String latestPeriodId = findLatestPeriodId();
            if (latestPeriodId == null) {
                return null;
            }
            periodId = latestPeriodId;
        }
        ErpFinAccountingPeriod period = daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
        return period != null ? period.getOrgId() : null;
    }

    private void applyOrgAndSchemaScope(QueryBean q, String periodId) {
        String orgId = resolvePeriodOrgId(periodId);
        if (orgId == null) {
            return;
        }
        q.addFilter(eq("orgId", orgId));
        String schemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
        if (schemaId != null) {
            q.addFilter(eq("acctSchemaId", schemaId));
        }
    }

    /** org/schema scope 解析结果（perf-ux plan 0835-1 E2：请求内单次解析、多腿复用）。 */
    private static final class OrgSchemaScope {
        static final OrgSchemaScope EMPTY = new OrgSchemaScope(null, null);
        final String orgId;
        final String schemaId;

        OrgSchemaScope(String orgId, String schemaId) {
            this.orgId = orgId;
            this.schemaId = schemaId;
        }
    }

    /**
     * 单次解析 org/schema scope：orgId 不可解析（period 无 org 等）时返回 EMPTY，
     * 调用方跳过 filter——与 applyOrgAndSchemaScope 的跳过语义逐字一致。
     */
    private OrgSchemaScope resolveOrgAndSchemaScope(String periodId) {
        String orgId = resolvePeriodOrgId(periodId);
        if (orgId == null) {
            return OrgSchemaScope.EMPTY;
        }
        return new OrgSchemaScope(orgId, AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId));
    }

    private void applyScope(QueryBean q, OrgSchemaScope scope) {
        if (scope.orgId == null) {
            return;
        }
        q.addFilter(eq("orgId", scope.orgId));
        if (scope.schemaId != null) {
            q.addFilter(eq("acctSchemaId", scope.schemaId));
        }
    }

    private static BigDecimal periodActivity(ErpFinGlBalance b, ErpMdSubject s) {
        BigDecimal debit = DashboardUtil.nz(b.getPeriodDebit());
        BigDecimal credit = DashboardUtil.nz(b.getPeriodCredit());
        return ErpFinConstants.DC_DEBIT.equals(s.getDirection())
                ? debit.subtract(credit)
                : credit.subtract(debit);
    }
}
