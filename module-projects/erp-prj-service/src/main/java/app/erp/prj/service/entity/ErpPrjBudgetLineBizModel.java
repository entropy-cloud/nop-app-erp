
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.prj.biz.IErpPrjBudgetLineBiz;
import app.erp.prj.dao.entity.ErpPrjBudgetLine;

import java.util.List;

@BizModel("ErpPrjBudgetLine")
public class ErpPrjBudgetLineBizModel extends AbstractErpCrudBizModel<ErpPrjBudgetLine> implements IErpPrjBudgetLineBiz{
    public ErpPrjBudgetLineBizModel(){
        setEntityName(ErpPrjBudgetLine.class.getName());
    }

}
