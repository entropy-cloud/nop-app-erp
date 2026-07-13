
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrSalaryItemBiz;
import app.erp.hr.dao.entity.ErpHrSalaryItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpHrSalaryItem")
public class ErpHrSalaryItemBizModel extends CrudBizModel<ErpHrSalaryItem> implements IErpHrSalaryItemBiz{
    public ErpHrSalaryItemBizModel(){
        setEntityName(ErpHrSalaryItem.class.getName());
    }

    @BizLoader(forType = ErpHrSalaryItem.class)
    public List<String> orgName(@ContextSource List<ErpHrSalaryItem> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrSalaryItem row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }
}
