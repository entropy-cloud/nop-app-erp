
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstMergeLineBiz;
import app.erp.ast.dao.entity.ErpAstMergeLine;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpAstMergeLine")
public class ErpAstMergeLineBizModel extends CrudBizModel<ErpAstMergeLine> implements IErpAstMergeLineBiz {
    public ErpAstMergeLineBizModel() {
        setEntityName(ErpAstMergeLine.class.getName());
    }

    @BizLoader(forType = ErpAstMergeLine.class)
    public List<String> mergeCode(@ContextSource List<ErpAstMergeLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("merge"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstMergeLine row : rows) {
            result.add(row.getMerge() != null ? row.getMerge().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstMergeLine.class)
    public List<String> orgName(@ContextSource List<ErpAstMergeLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstMergeLine row : rows) {
            result.add(row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstMergeLine.class)
    public List<String> sourceAssetCode(@ContextSource List<ErpAstMergeLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("sourceAsset"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstMergeLine row : rows) {
            result.add(row.getSourceAsset() != null ? row.getSourceAsset().getCode() : null);
        }
        return result;
    }
}
