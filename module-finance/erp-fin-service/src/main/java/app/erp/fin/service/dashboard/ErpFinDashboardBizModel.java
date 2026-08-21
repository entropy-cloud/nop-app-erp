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
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @BizQuery
    public Map<String, Object> getDashboardKpi(@Optional @Name("periodId") String periodId,
                                                IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            Map<String, Object> kpi = new LinkedHashMap<>();
            List<ErpFinGlBalance> balances = loadGlBalances(periodId);
            BigDecimal revenue = BigDecimal.ZERO;
            BigDecimal expense = BigDecimal.ZERO;
            for (ErpFinGlBalance b : balances) {
                ErpMdSubject s = b.getSubject();
                if (s == null || s.getSubjectClass() == null) continue;
                String cls = s.getSubjectClass();
                BigDecimal activity = periodActivity(b, s);
                if (ErpFinConstants.SUBJECT_CLASS_INCOME.equals(cls)) {
                    revenue = revenue.add(activity);
                } else if (ErpFinConstants.SUBJECT_CLASS_EXPENSE.equals(cls)
                        || ErpFinConstants.SUBJECT_CLASS_COST.equals(cls)) {
                    expense = expense.add(activity);
                }
            }
            kpi.put("periodId", periodId);
            kpi.put("revenue", revenue);
            kpi.put("expense", expense);
            kpi.put("netProfit", revenue.subtract(expense));
            kpi.put("bankBalance", sumBankBalance());
            kpi.put("arBalance", sumArApOpen(ErpFinConstants.DIRECTION_RECEIVABLE, periodId));
            kpi.put("apBalance", sumArApOpen(ErpFinConstants.DIRECTION_PAYABLE, periodId));
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
            List<ErpFinGlBalance> balances = loadGlBalancesInRange(from, today);
            Map<String, BigDecimal> revenueByMonth = new LinkedHashMap<>();
            Map<String, BigDecimal> expenseByMonth = new LinkedHashMap<>();
            for (ErpFinGlBalance b : balances) {
                ErpMdSubject s = b.getSubject();
                if (s == null || s.getSubjectClass() == null) continue;
                ErpFinAccountingPeriod period = b.getPeriod();
                if (period == null || period.getYear() == null || period.getMonth() == null) continue;
                String key = period.getYear() + "-" + String.format("%02d", period.getMonth());
                LocalDate periodStart = period.getStartDate();
                if (periodStart == null || periodStart.isBefore(from.minusDays(1))) continue;
                BigDecimal activity = periodActivity(b, s);
                String cls = s.getSubjectClass();
                if (ErpFinConstants.SUBJECT_CLASS_INCOME.equals(cls)) {
                    revenueByMonth.merge(key, activity, BigDecimal::add);
                } else if (ErpFinConstants.SUBJECT_CLASS_EXPENSE.equals(cls)
                        || ErpFinConstants.SUBJECT_CLASS_COST.equals(cls)) {
                    expenseByMonth.merge(key, activity, BigDecimal::add);
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

    private BigDecimal sumBankBalance() {
        IEntityDao<ErpFinFundAccount> dao = daoProvider.daoFor(ErpFinFundAccount.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("accountType", ErpFinConstants.FUND_ACCOUNT_TYPE_BANK));
        List<ErpFinFundAccount> accounts = dao.findAllByQuery(q);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpFinFundAccount a : accounts) {
            sum = sum.add(DashboardUtil.nz(a.getCurrentBalance()));
        }
        return sum;
    }

    private BigDecimal sumArApOpen(String direction, String periodId) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("direction", direction));
        q.addFilter(in("status", Arrays.asList(
                ErpFinConstants.AR_AP_STATUS_OPEN,
                ErpFinConstants.AR_AP_STATUS_PARTIAL)));
        applyOrgAndSchemaScope(q, periodId);
        List<ErpFinArApItem> items = dao.findAllByQuery(q);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpFinArApItem it : items) {
            sum = sum.add(DashboardUtil.nz(it.getOpenAmountFunctional()));
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

    private static BigDecimal periodActivity(ErpFinGlBalance b, ErpMdSubject s) {
        BigDecimal debit = DashboardUtil.nz(b.getPeriodDebit());
        BigDecimal credit = DashboardUtil.nz(b.getPeriodCredit());
        return ErpFinConstants.DC_DEBIT.equals(s.getDirection())
                ? debit.subtract(credit)
                : credit.subtract(debit);
    }
}
