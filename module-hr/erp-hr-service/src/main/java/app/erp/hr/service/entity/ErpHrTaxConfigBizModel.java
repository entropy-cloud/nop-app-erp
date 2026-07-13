
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrTaxConfigBiz;
import app.erp.hr.dao.entity.ErpHrTaxConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpHrTaxConfig")
public class ErpHrTaxConfigBizModel extends CrudBizModel<ErpHrTaxConfig> implements IErpHrTaxConfigBiz{
    public ErpHrTaxConfigBizModel(){
        setEntityName(ErpHrTaxConfig.class.getName());
    }

    @BizLoader(forType = ErpHrTaxConfig.class)
    public List<String> orgName(@ContextSource List<ErpHrTaxConfig> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrTaxConfig row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }
}
