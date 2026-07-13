
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstCipProgressBillingBiz;
import app.erp.ast.dao.entity.ErpAstCipProgressBilling;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpAstCipProgressBilling")
public class ErpAstCipProgressBillingBizModel extends CrudBizModel<ErpAstCipProgressBilling> implements IErpAstCipProgressBillingBiz {
    public ErpAstCipProgressBillingBizModel() {
        setEntityName(ErpAstCipProgressBilling.class.getName());
    }

    @BizLoader(forType = ErpAstCipProgressBilling.class)
    public List<String> cipCode(@ContextSource List<ErpAstCipProgressBilling> rows) {
        orm().batchLoadProps(rows, Collections.singleton("cip"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstCipProgressBilling row : rows) {
            result.add(row.getCip() != null ? row.getCip().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstCipProgressBilling.class)
    public List<String> orgName(@ContextSource List<ErpAstCipProgressBilling> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstCipProgressBilling row : rows) {
            result.add(row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstCipProgressBilling.class)
    public List<String> currencyName(@ContextSource List<ErpAstCipProgressBilling> rows) {
        orm().batchLoadProps(rows, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstCipProgressBilling row : rows) {
            result.add(row.getCurrency() != null ? row.getCurrency().getName() : null);
        }
        return result;
    }
}
