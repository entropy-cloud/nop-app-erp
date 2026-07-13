package app.erp.prj.service.entity;

import app.erp.prj.biz.IErpPrjProjectPnlBiz;
import app.erp.prj.dao.entity.ErpPrjProjectPnl;
import app.erp.prj.service.pnl.ProjectPnlCalculator;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 项目损益汇总 BizModel。CRUD 之上承载损益汇总计算引擎入口（{@code profitability.md §关键流程 1}）：
 * <ul>
 *   <li>{@link #refreshPnl}：手工/批量触发经 {@link ProjectPnlCalculator} 聚合 Billing 收入 + CostCollection 四类成本。</li>
 *   <li>{@link #getProjectPnl}：返回最新 {@code calcStatus=CALCULATED} 快照（看板 successor 取数通道）。</li>
 * </ul>
 */
@BizModel("ErpPrjProjectPnl")
public class ErpPrjProjectPnlBizModel extends CrudBizModel<ErpPrjProjectPnl> implements IErpPrjProjectPnlBiz {

    @Inject
    ProjectPnlCalculator pnlCalculator;

    public ErpPrjProjectPnlBizModel() {
        setEntityName(ErpPrjProjectPnl.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpPrjProjectPnl refreshPnl(@Name("projectId") Long projectId,
                                       @Name("periodFrom") LocalDate periodFrom,
                                       @Name("periodTo") LocalDate periodTo,
                                       IServiceContext context) {
        return pnlCalculator.refreshPnl(projectId, periodFrom, periodTo);
    }

    @Override
    @BizQuery
    @SingleSession
    public ErpPrjProjectPnl getProjectPnl(@Name("projectId") Long projectId, IServiceContext context) {
        return pnlCalculator.findLatestCalculated(projectId);
    }

    @BizLoader(forType = ErpPrjProjectPnl.class)
    public List<String> projectName(@ContextSource List<ErpPrjProjectPnl> pnls) {
        orm().batchLoadProps(pnls, Collections.singleton("project"));
        List<String> result = new ArrayList<>(pnls.size());
        for (ErpPrjProjectPnl pnl : pnls) {
            result.add(pnl.getProject() != null ? pnl.getProject().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjProjectPnl.class)
    public List<String> orgName(@ContextSource List<ErpPrjProjectPnl> pnls) {
        orm().batchLoadProps(pnls, Collections.singleton("org"));
        List<String> result = new ArrayList<>(pnls.size());
        for (ErpPrjProjectPnl pnl : pnls) {
            result.add(pnl.getOrg() != null ? pnl.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjProjectPnl.class)
    public List<String> currencyName(@ContextSource List<ErpPrjProjectPnl> pnls) {
        orm().batchLoadProps(pnls, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(pnls.size());
        for (ErpPrjProjectPnl pnl : pnls) {
            result.add(pnl.getCurrency() != null ? pnl.getCurrency().getName() : null);
        }
        return result;
    }
}
