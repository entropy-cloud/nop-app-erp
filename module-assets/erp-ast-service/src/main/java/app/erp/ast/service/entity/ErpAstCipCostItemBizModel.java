
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstCipCostItemBiz;
import app.erp.ast.dao.entity.ErpAstCipCostItem;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpAstCipCostItem")
public class ErpAstCipCostItemBizModel extends CrudBizModel<ErpAstCipCostItem> implements IErpAstCipCostItemBiz {
    public ErpAstCipCostItemBizModel() {
        setEntityName(ErpAstCipCostItem.class.getName());
    }

    @BizLoader(forType = ErpAstCipCostItem.class)
    public List<String> cipCode(@ContextSource List<ErpAstCipCostItem> rows) {
        orm().batchLoadProps(rows, Collections.singleton("cip"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstCipCostItem row : rows) {
            result.add(row.getCip() != null ? row.getCip().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstCipCostItem.class)
    public List<String> orgName(@ContextSource List<ErpAstCipCostItem> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstCipCostItem row : rows) {
            result.add(row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstCipCostItem.class)
    public List<String> currencyName(@ContextSource List<ErpAstCipCostItem> rows) {
        orm().batchLoadProps(rows, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstCipCostItem row : rows) {
            result.add(row.getCurrency() != null ? row.getCurrency().getName() : null);
        }
        return result;
    }
}
