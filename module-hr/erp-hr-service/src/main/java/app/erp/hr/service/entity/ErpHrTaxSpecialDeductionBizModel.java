
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.hr.biz.IErpHrTaxSpecialDeductionBiz;
import app.erp.hr.dao.entity.ErpHrTaxSpecialDeduction;

import java.util.List;

@BizModel("ErpHrTaxSpecialDeduction")
public class ErpHrTaxSpecialDeductionBizModel extends AbstractErpCrudBizModel<ErpHrTaxSpecialDeduction> implements IErpHrTaxSpecialDeductionBiz{
    public ErpHrTaxSpecialDeductionBizModel(){
        setEntityName(ErpHrTaxSpecialDeduction.class.getName());
    }

}
