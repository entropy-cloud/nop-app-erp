
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjBudgetLineBiz;
import app.erp.prj.dao.entity.ErpPrjBudgetLine;

@BizModel("ErpPrjBudgetLine")
public class ErpPrjBudgetLineBizModel extends CrudBizModel<ErpPrjBudgetLine> implements IErpPrjBudgetLineBiz{
    public ErpPrjBudgetLineBizModel(){
        setEntityName(ErpPrjBudgetLine.class.getName());
    }
}
