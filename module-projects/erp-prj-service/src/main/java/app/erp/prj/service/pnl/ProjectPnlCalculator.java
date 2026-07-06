package app.erp.prj.service.pnl;

import app.erp.prj.dao.entity.ErpPrjBilling;
import app.erp.prj.dao.entity.ErpPrjBudget;
import app.erp.prj.dao.entity.ErpPrjBudgetLine;
import app.erp.prj.dao.entity.ErpPrjCostCollection;
import app.erp.prj.dao.entity.ErpPrjCostCollectionLine;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjProjectPnl;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * 项目损益汇总计算引擎（{@code profitability.md §关键流程 1}）。按项目+期间聚合：
 * <ul>
 *   <li>收入：{@link ErpPrjBilling} 头 {@code amountFunctional}（过滤审批通过/未取消）。</li>
 *   <li>成本：{@link ErpPrjCostCollectionLine} 按 {@code costCategory} 分组（LABOR/MATERIAL/EXPENSE/SUBCONTRACT），
 *       经父头 {@code businessDate} 过滤期间。</li>
 *   <li>已承诺成本：{@link ErpPrjBudgetLine#committedAmount} 汇总。</li>
 *   <li>预算：{@link ErpPrjBudget#totalAmount} 汇总。</li>
 *   <li>完工预测（EAC）= 实际成本合计 + ETC（ETC = max(budgetAmount − committedCost, 0)）。</li>
 * </ul>
 *
 * <p>幂等：同 {@code projectId + periodFrom + periodTo} 的已存在快照若未过账则清旧重建；已过账（{@code posted=true}）
 * 抛 {@link ErpPrjErrors#ERR_PRJ_PNL_RECALC_FROZEN} 拒绝重算。
 *
 * <p>多币种基线：聚合使用 {@code amountFunctional}（本位币）与行 {@code amount}，PnL 快照 {@code exchangeRate=1}。
 * 跨币种 rollup 精度归 successor。
 */
public class ProjectPnlCalculator {

    private static final LocalDate FLOOR_DATE = LocalDate.of(2000, 1, 1);

    @Inject
    IDaoProvider daoProvider;

    public ErpPrjProjectPnl refreshPnl(Long projectId, LocalDate periodFrom, LocalDate periodTo) {
        ErpPrjProject project = loadProject(projectId);
        if (project == null) {
            throw new NopException(ErpPrjErrors.ERR_PROJECT_NOT_REFERENCEABLE)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, projectId);
        }

        LocalDate from = periodFrom != null ? periodFrom : project.getStartDate();
        LocalDate to = periodTo != null ? periodTo : CoreMetrics.today();
        if (from == null) {
            from = FLOOR_DATE;
        }
        if (to == null) {
            to = CoreMetrics.today();
        }
        if (from.isAfter(to)) {
            throw new NopException(ErpPrjErrors.ERR_PRJ_PNL_PERIOD_INVALID)
                    .param(ErpPrjErrors.ARG_PERIOD_FROM, from)
                    .param(ErpPrjErrors.ARG_PERIOD_TO, to);
        }

        ErpPrjProjectPnl existing = findExisting(projectId, from, to);
        if (existing != null && Boolean.TRUE.equals(existing.getPosted())) {
            throw new NopException(ErpPrjErrors.ERR_PRJ_PNL_RECALC_FROZEN)
                    .param(ErpPrjErrors.ARG_PNL_CODE, existing.getCode());
        }

        BigDecimal revenue = sumRevenue(projectId, from, to);
        CostBreakdown cost = sumCostByCategory(projectId, from, to);
        BigDecimal totalCost = cost.total();
        BigDecimal grossProfit = revenue.subtract(totalCost);
        String grossMarginPct = marginPct(grossProfit, revenue);

        BigDecimal committedCost = sumCommittedCost(projectId);
        BigDecimal budgetAmount = sumBudgetAmount(projectId);
        BigDecimal etc = budgetAmount.subtract(committedCost).max(BigDecimal.ZERO);
        BigDecimal forecastCompleteCost = totalCost.add(etc);

        IEntityDao<ErpPrjProjectPnl> pnlDao = daoProvider.daoFor(ErpPrjProjectPnl.class);
        ErpPrjProjectPnl pnl;
        if (existing != null) {
            pnl = existing;
        } else {
            pnl = pnlDao.newEntity();
            pnl.setCode("PNL-" + projectId + "-" + CoreMetrics.currentTimeMillis());
            pnl.setProjectId(projectId);
            pnl.setOrgId(project.getOrgId());
            pnl.setCurrencyId(project.getCurrencyId());
            pnl.setExchangeRate(BigDecimal.ONE);
        }
        pnl.setPeriodFrom(from);
        pnl.setPeriodTo(to);
        pnl.setAmountSource(revenue);
        pnl.setAmountFunctional(revenue);
        pnl.setRevenueAmount(revenue);
        pnl.setCostLabor(cost.labor);
        pnl.setCostMaterial(cost.material);
        pnl.setCostExpense(cost.expense);
        pnl.setCostSubcontract(cost.subcontract);
        pnl.setTotalCost(totalCost);
        pnl.setGrossProfit(grossProfit);
        pnl.setGrossMarginPct(grossMarginPct);
        pnl.setCommittedCost(committedCost);
        pnl.setBudgetAmount(budgetAmount);
        pnl.setForecastCompleteCost(forecastCompleteCost);
        pnl.setCalcStatus(ErpPrjConstants.PNL_CALC_STATUS_CALCULATED);
        pnl.setDocStatus(ErpPrjConstants.DOC_STATUS_DRAFT);
        pnl.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);

        if (existing != null) {
            pnlDao.updateEntity(pnl);
        } else {
            pnlDao.saveEntity(pnl);
        }
        return pnl;
    }

    public ErpPrjProjectPnl findLatestCalculated(Long projectId) {
        IEntityDao<ErpPrjProjectPnl> dao = daoProvider.daoFor(ErpPrjProjectPnl.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("projectId", projectId), eq("calcStatus", ErpPrjConstants.PNL_CALC_STATUS_CALCULATED)));
        q.addOrderField("periodTo", true);
        q.setLimit(1);
        List<ErpPrjProjectPnl> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private BigDecimal sumRevenue(Long projectId, LocalDate from, LocalDate to) {
        IEntityDao<ErpPrjBilling> dao = daoProvider.daoFor(ErpPrjBilling.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("projectId", projectId));
        q.addFilter(ne("docStatus", ErpPrjConstants.DOC_STATUS_CANCELLED));
        if (from != null) {
            q.addFilter(ge("businessDate", from));
        }
        if (to != null) {
            q.addFilter(le("businessDate", to));
        }
        List<ErpPrjBilling> billings = dao.findAllByQuery(q);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpPrjBilling b : billings) {
            sum = sum.add(nz(b.getAmountFunctional()));
        }
        return sum;
    }

    private CostBreakdown sumCostByCategory(Long projectId, LocalDate from, LocalDate to) {
        List<ErpPrjCostCollection> heads = findCostHeads(projectId, from, to);
        if (heads.isEmpty()) {
            return new CostBreakdown();
        }
        Set<Long> headIds = new HashSet<>();
        for (ErpPrjCostCollection h : heads) {
            headIds.add(h.getId());
        }
        IEntityDao<ErpPrjCostCollectionLine> lineDao = daoProvider.daoFor(ErpPrjCostCollectionLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("costCollectionId", headIds));
        List<ErpPrjCostCollectionLine> lines = lineDao.findAllByQuery(q);

        CostBreakdown cb = new CostBreakdown();
        for (ErpPrjCostCollectionLine l : lines) {
            BigDecimal amt = nz(l.getAmount());
            String cat = l.getCostCategory();
            if (ErpPrjConstants.COST_CATEGORY_LABOR.equals(cat)) {
                cb.labor = cb.labor.add(amt);
            } else if (ErpPrjConstants.COST_CATEGORY_MATERIAL.equals(cat)) {
                cb.material = cb.material.add(amt);
            } else if (ErpPrjConstants.COST_CATEGORY_EXPENSE.equals(cat)) {
                cb.expense = cb.expense.add(amt);
            } else if (ErpPrjConstants.COST_CATEGORY_SUBCONTRACT.equals(cat)) {
                cb.subcontract = cb.subcontract.add(amt);
            } else {
                cb.expense = cb.expense.add(amt);
            }
        }
        return cb;
    }

    private List<ErpPrjCostCollection> findCostHeads(Long projectId, LocalDate from, LocalDate to) {
        IEntityDao<ErpPrjCostCollection> dao = daoProvider.daoFor(ErpPrjCostCollection.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("projectId", projectId));
        q.addFilter(ne("docStatus", ErpPrjConstants.DOC_STATUS_CANCELLED));
        if (from != null) {
            q.addFilter(ge("businessDate", from));
        }
        if (to != null) {
            q.addFilter(le("businessDate", to));
        }
        return dao.findAllByQuery(q);
    }

    private BigDecimal sumCommittedCost(Long projectId) {
        List<ErpPrjBudget> budgets = findBudgets(projectId);
        if (budgets.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Set<Long> budgetIds = new HashSet<>();
        for (ErpPrjBudget b : budgets) {
            budgetIds.add(b.getId());
        }
        IEntityDao<ErpPrjBudgetLine> dao = daoProvider.daoFor(ErpPrjBudgetLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("budgetId", budgetIds));
        List<ErpPrjBudgetLine> lines = dao.findAllByQuery(q);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpPrjBudgetLine l : lines) {
            sum = sum.add(nz(l.getCommittedAmount()));
        }
        return sum;
    }

    private BigDecimal sumBudgetAmount(Long projectId) {
        List<ErpPrjBudget> budgets = findBudgets(projectId);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpPrjBudget b : budgets) {
            sum = sum.add(nz(b.getTotalAmount()));
        }
        return sum;
    }

    private List<ErpPrjBudget> findBudgets(Long projectId) {
        IEntityDao<ErpPrjBudget> dao = daoProvider.daoFor(ErpPrjBudget.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("projectId", projectId));
        q.addFilter(ne("docStatus", ErpPrjConstants.DOC_STATUS_CANCELLED));
        return dao.findAllByQuery(q);
    }

    private ErpPrjProjectPnl findExisting(Long projectId, LocalDate from, LocalDate to) {
        IEntityDao<ErpPrjProjectPnl> dao = daoProvider.daoFor(ErpPrjProjectPnl.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("projectId", projectId));
        if (from != null) {
            q.addFilter(eq("periodFrom", from));
        }
        if (to != null) {
            q.addFilter(eq("periodTo", to));
        }
        q.setLimit(1);
        List<ErpPrjProjectPnl> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private String marginPct(BigDecimal grossProfit, BigDecimal revenue) {
        if (revenue == null || revenue.signum() == 0) {
            return "0";
        }
        return grossProfit.multiply(BigDecimal.valueOf(100))
                .divide(revenue, 4, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private ErpPrjProject loadProject(Long projectId) {
        if (projectId == null) {
            return null;
        }
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        return dao.getEntityById(projectId);
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static class CostBreakdown {
        BigDecimal labor = BigDecimal.ZERO;
        BigDecimal material = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        BigDecimal subcontract = BigDecimal.ZERO;

        BigDecimal total() {
            return labor.add(material).add(expense).add(subcontract);
        }
    }
}
