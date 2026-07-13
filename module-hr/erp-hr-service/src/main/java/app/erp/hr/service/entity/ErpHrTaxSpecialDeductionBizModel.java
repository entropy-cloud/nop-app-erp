
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrTaxSpecialDeductionBiz;
import app.erp.hr.dao.entity.ErpHrTaxSpecialDeduction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpHrTaxSpecialDeduction")
public class ErpHrTaxSpecialDeductionBizModel extends CrudBizModel<ErpHrTaxSpecialDeduction> implements IErpHrTaxSpecialDeductionBiz{
    public ErpHrTaxSpecialDeductionBizModel(){
        setEntityName(ErpHrTaxSpecialDeduction.class.getName());
    }

    @BizLoader(forType = ErpHrTaxSpecialDeduction.class)
    public List<String> employeeDisplayName(@ContextSource List<ErpHrTaxSpecialDeduction> rows) {
        orm().batchLoadProps(rows, Collections.singleton("employee"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrTaxSpecialDeduction row : rows) {
            result.add(row.orm_attached() && row.getEmployee() != null ? row.getEmployee().getFullName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrTaxSpecialDeduction.class)
    public List<String> orgName(@ContextSource List<ErpHrTaxSpecialDeduction> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrTaxSpecialDeduction row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }
}
