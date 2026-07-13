
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjBudgetBiz;
import app.erp.prj.dao.entity.ErpPrjBudget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpPrjBudget")
public class ErpPrjBudgetBizModel extends CrudBizModel<ErpPrjBudget> implements IErpPrjBudgetBiz{
    public ErpPrjBudgetBizModel(){
        setEntityName(ErpPrjBudget.class.getName());
    }

    @BizLoader(forType = ErpPrjBudget.class)
    public List<String> projectName(@ContextSource List<ErpPrjBudget> budgets) {
        orm().batchLoadProps(budgets, Collections.singleton("project"));
        List<String> result = new ArrayList<>(budgets.size());
        for (ErpPrjBudget budget : budgets) {
            result.add(budget.getProject() != null ? budget.getProject().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjBudget.class)
    public List<String> orgName(@ContextSource List<ErpPrjBudget> budgets) {
        orm().batchLoadProps(budgets, Collections.singleton("org"));
        List<String> result = new ArrayList<>(budgets.size());
        for (ErpPrjBudget budget : budgets) {
            result.add(budget.getOrg() != null ? budget.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjBudget.class)
    public List<String> currencyName(@ContextSource List<ErpPrjBudget> budgets) {
        orm().batchLoadProps(budgets, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(budgets.size());
        for (ErpPrjBudget budget : budgets) {
            result.add(budget.getCurrency() != null ? budget.getCurrency().getName() : null);
        }
        return result;
    }
}
