
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjBudgetLineBiz;
import app.erp.prj.dao.entity.ErpPrjBudgetLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpPrjBudgetLine")
public class ErpPrjBudgetLineBizModel extends CrudBizModel<ErpPrjBudgetLine> implements IErpPrjBudgetLineBiz{
    public ErpPrjBudgetLineBizModel(){
        setEntityName(ErpPrjBudgetLine.class.getName());
    }

    @BizLoader(forType = ErpPrjBudgetLine.class)
    public List<String> budgetCode(@ContextSource List<ErpPrjBudgetLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("budget"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpPrjBudgetLine line : lines) {
            result.add(line.getBudget() != null ? line.getBudget().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjBudgetLine.class)
    public List<String> subjectName(@ContextSource List<ErpPrjBudgetLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("subject"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpPrjBudgetLine line : lines) {
            result.add(line.getSubject() != null ? line.getSubject().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjBudgetLine.class)
    public List<String> taskName(@ContextSource List<ErpPrjBudgetLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("task"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpPrjBudgetLine line : lines) {
            result.add(line.getTask() != null ? line.getTask().getTitle() : null);
        }
        return result;
    }
}
