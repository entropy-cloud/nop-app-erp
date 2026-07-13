package app.erp.prj.service.entity;

import app.erp.prj.biz.IErpPrjCostCollectionBiz;
import app.erp.prj.dao.entity.ErpPrjCostCollection;
import app.erp.prj.service.ErpPrjConfigs;
import app.erp.prj.service.cost.ExpenseCostAggregator;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 项目成本归集 BizModel。CRUD 之上承载费用报销归集接入（projects 驱动只读聚合）。
 *
 * <p>{@link #refreshExpenseCost(Long, IServiceContext)} 受 {@code erp-prj.expense-aggregation-enabled}
 * （默认 true）config-gated。关闭时直接返回 0（{@code closeProject} 也据此跳过费用刷新）。
 */
@BizModel("ErpPrjCostCollection")
public class ErpPrjCostCollectionBizModel extends CrudBizModel<ErpPrjCostCollection>
        implements IErpPrjCostCollectionBiz {

    @Inject
    ExpenseCostAggregator expenseCostAggregator;

    public ErpPrjCostCollectionBizModel() {
        setEntityName(ErpPrjCostCollection.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public BigDecimal refreshExpenseCost(@Name("projectId") Long projectId, IServiceContext context) {
        if (!ErpPrjConfigs.expenseAggregationEnabled()) {
            return BigDecimal.ZERO;
        }
        return expenseCostAggregator.refreshExpenseCost(projectId);
    }

    @BizLoader(forType = ErpPrjCostCollection.class)
    public List<String> projectName(@ContextSource List<ErpPrjCostCollection> collections) {
        orm().batchLoadProps(collections, Collections.singleton("project"));
        List<String> result = new ArrayList<>(collections.size());
        for (ErpPrjCostCollection cc : collections) {
            result.add(cc.getProject() != null ? cc.getProject().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjCostCollection.class)
    public List<String> orgName(@ContextSource List<ErpPrjCostCollection> collections) {
        orm().batchLoadProps(collections, Collections.singleton("org"));
        List<String> result = new ArrayList<>(collections.size());
        for (ErpPrjCostCollection cc : collections) {
            result.add(cc.getOrg() != null ? cc.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjCostCollection.class)
    public List<String> currencyName(@ContextSource List<ErpPrjCostCollection> collections) {
        orm().batchLoadProps(collections, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(collections.size());
        for (ErpPrjCostCollection cc : collections) {
            result.add(cc.getCurrency() != null ? cc.getCurrency().getName() : null);
        }
        return result;
    }
}
