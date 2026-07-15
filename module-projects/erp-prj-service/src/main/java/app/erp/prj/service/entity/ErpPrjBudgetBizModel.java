
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjBudgetBiz;
import app.erp.prj.dao.entity.ErpPrjBudget;

import java.util.List;

@BizModel("ErpPrjBudget")
public class ErpPrjBudgetBizModel extends CrudBizModel<ErpPrjBudget> implements IErpPrjBudgetBiz{
    public ErpPrjBudgetBizModel(){
        setEntityName(ErpPrjBudget.class.getName());
    }

}
