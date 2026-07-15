
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrPayrollBankFileBiz;
import app.erp.hr.dao.entity.ErpHrPayrollBankFile;

import java.util.List;

@BizModel("ErpHrPayrollBankFile")
public class ErpHrPayrollBankFileBizModel extends CrudBizModel<ErpHrPayrollBankFile> implements IErpHrPayrollBankFileBiz{
    public ErpHrPayrollBankFileBizModel(){
        setEntityName(ErpHrPayrollBankFile.class.getName());
    }

}
