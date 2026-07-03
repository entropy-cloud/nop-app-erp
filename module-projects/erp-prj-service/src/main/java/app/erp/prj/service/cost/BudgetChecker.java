package app.erp.prj.service.cost;

import app.erp.prj.dao.entity.ErpPrjCostCollection;
import app.erp.prj.dao.entity.ErpPrjCostCollectionLine;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.service.ErpPrjConfigs;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 项目预算检查器。按 {@code erp-prj.budget-control-mode}（{@code cost-collection.md §3.2}）：
 * <ul>
 *   <li>{@code WARNING}（默认）：超预算仅记日志警告放行。</li>
 *   <li>{@code STRICT}：超预算抛 {@link ErpPrjErrors#ERR_BUDGET_EXCEEDED}。</li>
 * </ul>
 *
 * <p>预算口径：{@link ErpPrjProject#getBudget()} 为总预算（项目级 config-gated 模式，实现偏差见计划 Task Route
 * Decision——设计 §3.1 暗示预算头 controlMode 字段，本期以项目级 config 实现）。已使用 = 该项目所有归集行
 * {@link ErpPrjCostCollectionLine#getAmount()} 之和。
 */
public class BudgetChecker {

    private static final Logger LOG = LoggerFactory.getLogger(BudgetChecker.class);

    @Inject
    IDaoProvider daoProvider;

    /**
     * 检查「已使用 + 拟新增」是否超总预算。无预算配置或非超预算时静默返回。
     */
    public void check(Long projectId, BigDecimal addAmount) {
        if (projectId == null || addAmount == null || addAmount.signum() <= 0) {
            return;
        }
        ErpPrjProject project = loadProject(projectId);
        if (project == null) {
            return;
        }
        BigDecimal total = project.getBudget();
        if (total == null || total.signum() <= 0) {
            return;
        }
        BigDecimal used = sumUsedAmount(projectId);
        BigDecimal projected = used.add(addAmount);
        if (projected.compareTo(total) > 0) {
            if (ErpPrjConfigs.budgetControlStrict()) {
                throw new NopException(ErpPrjErrors.ERR_BUDGET_EXCEEDED)
                        .param(ErpPrjErrors.ARG_PROJECT_ID, projectId)
                        .param(ErpPrjErrors.ARG_BUDGET_TOTAL, total)
                        .param(ErpPrjErrors.ARG_BUDGET_USED, used)
                        .param(ErpPrjErrors.ARG_AMOUNT, addAmount);
            }
            LOG.warn("项目 {} 预算超限（WARNING 模式放行）：总预算={}, 已使用={}, 拟新增={}",
                    projectId, total, used, addAmount);
        }
    }

    private ErpPrjProject loadProject(Long projectId) {
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        return dao.getEntityById(projectId);
    }

    /**
     * 聚合项目所有归集行金额（已使用预算）。归集行经 {@link ErpPrjCostCollection#getProjectId()} 反查所属项目。
     */
    public BigDecimal sumUsedAmount(Long projectId) {
        IEntityDao<ErpPrjCostCollection> headDao = daoProvider.daoFor(ErpPrjCostCollection.class);
        QueryBean headQuery = new QueryBean();
        headQuery.addFilter(eq("projectId", projectId));
        List<ErpPrjCostCollection> heads = headDao.findAllByQuery(headQuery);
        if (heads.isEmpty()) {
            return BigDecimal.ZERO;
        }

        java.util.List<Long> headIds = new java.util.ArrayList<>(heads.size());
        for (ErpPrjCostCollection h : heads) {
            headIds.add(h.getId());
        }

        IEntityDao<ErpPrjCostCollectionLine> lineDao = daoProvider.daoFor(ErpPrjCostCollectionLine.class);
        QueryBean lineQuery = new QueryBean();
        lineQuery.addFilter(in("costCollectionId", headIds));
        List<ErpPrjCostCollectionLine> lines = lineDao.findAllByQuery(lineQuery);

        BigDecimal sum = BigDecimal.ZERO;
        for (ErpPrjCostCollectionLine l : lines) {
            sum = sum.add(nz(l.getAmount()));
        }
        return sum;
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
