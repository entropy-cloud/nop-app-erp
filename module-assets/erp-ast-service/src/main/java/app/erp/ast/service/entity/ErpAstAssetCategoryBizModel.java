
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstAssetCategoryBiz;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpAstAssetCategory")
public class ErpAstAssetCategoryBizModel extends CrudBizModel<ErpAstAssetCategory> implements IErpAstAssetCategoryBiz {
    public ErpAstAssetCategoryBizModel() {
        setEntityName(ErpAstAssetCategory.class.getName());
    }

    @BizLoader(forType = ErpAstAssetCategory.class)
    public List<String> subjectName(@ContextSource List<ErpAstAssetCategory> rows) {
        orm().batchLoadProps(rows, Collections.singleton("subject"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstAssetCategory row : rows) {
            result.add(row.getSubject() != null ? row.getSubject().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstAssetCategory.class)
    public List<String> depreciationSubjectName(@ContextSource List<ErpAstAssetCategory> rows) {
        orm().batchLoadProps(rows, Collections.singleton("depreciationSubject"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstAssetCategory row : rows) {
            result.add(row.getDepreciationSubject() != null ? row.getDepreciationSubject().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstAssetCategory.class)
    public List<String> expenseSubjectName(@ContextSource List<ErpAstAssetCategory> rows) {
        orm().batchLoadProps(rows, Collections.singleton("expenseSubject"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstAssetCategory row : rows) {
            result.add(row.getExpenseSubject() != null ? row.getExpenseSubject().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstAssetCategory.class)
    public List<String> disposalGainLossSubjectName(@ContextSource List<ErpAstAssetCategory> rows) {
        orm().batchLoadProps(rows, Collections.singleton("disposalGainLossSubject"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstAssetCategory row : rows) {
            result.add(row.getDisposalGainLossSubject() != null ? row.getDisposalGainLossSubject().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstAssetCategory.class)
    public List<String> cipSubjectName(@ContextSource List<ErpAstAssetCategory> rows) {
        orm().batchLoadProps(rows, Collections.singleton("cipSubject"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstAssetCategory row : rows) {
            result.add(row.getCipSubject() != null ? row.getCipSubject().getName() : null);
        }
        return result;
    }
}
