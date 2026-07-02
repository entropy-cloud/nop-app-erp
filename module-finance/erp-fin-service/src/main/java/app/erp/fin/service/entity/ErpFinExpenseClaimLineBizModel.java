
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinExpenseClaimLineBiz;
import app.erp.fin.dao.entity.ErpFinExpenseClaimLine;

@BizModel("ErpFinExpenseClaimLine")
public class ErpFinExpenseClaimLineBizModel extends CrudBizModel<ErpFinExpenseClaimLine> implements IErpFinExpenseClaimLineBiz{
    public ErpFinExpenseClaimLineBizModel(){
        setEntityName(ErpFinExpenseClaimLine.class.getName());
    }
}
