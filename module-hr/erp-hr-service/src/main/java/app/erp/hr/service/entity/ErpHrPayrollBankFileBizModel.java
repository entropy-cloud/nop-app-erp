
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrPayrollBankFileBiz;
import app.erp.hr.dao.entity.ErpHrPayrollBankFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpHrPayrollBankFile")
public class ErpHrPayrollBankFileBizModel extends CrudBizModel<ErpHrPayrollBankFile> implements IErpHrPayrollBankFileBiz{
    public ErpHrPayrollBankFileBizModel(){
        setEntityName(ErpHrPayrollBankFile.class.getName());
    }

    @BizLoader(forType = ErpHrPayrollBankFile.class)
    public List<String> bankDisplayName(@ContextSource List<ErpHrPayrollBankFile> rows) {
        orm().batchLoadProps(rows, Collections.singleton("bank"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrPayrollBankFile row : rows) {
            result.add(row.orm_attached() && row.getBank() != null ? row.getBank().getBankName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrPayrollBankFile.class)
    public List<String> orgName(@ContextSource List<ErpHrPayrollBankFile> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrPayrollBankFile row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }
}
