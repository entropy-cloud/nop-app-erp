
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstSplitLineBiz;
import app.erp.ast.dao.entity.ErpAstSplitLine;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpAstSplitLine")
public class ErpAstSplitLineBizModel extends CrudBizModel<ErpAstSplitLine> implements IErpAstSplitLineBiz {
    public ErpAstSplitLineBizModel() {
        setEntityName(ErpAstSplitLine.class.getName());
    }

    @BizLoader(forType = ErpAstSplitLine.class)
    public List<String> splitCode(@ContextSource List<ErpAstSplitLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("split"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstSplitLine row : rows) {
            result.add(row.getSplit() != null ? row.getSplit().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstSplitLine.class)
    public List<String> orgName(@ContextSource List<ErpAstSplitLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstSplitLine row : rows) {
            result.add(row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstSplitLine.class)
    public List<String> categoryName(@ContextSource List<ErpAstSplitLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("category"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstSplitLine row : rows) {
            result.add(row.getCategory() != null ? row.getCategory().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstSplitLine.class)
    public List<String> targetAssetCode(@ContextSource List<ErpAstSplitLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("targetAsset"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstSplitLine row : rows) {
            result.add(row.getTargetAsset() != null ? row.getTargetAsset().getCode() : null);
        }
        return result;
    }
}
