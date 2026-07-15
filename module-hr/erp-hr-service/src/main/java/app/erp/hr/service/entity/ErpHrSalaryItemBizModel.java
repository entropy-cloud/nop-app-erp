
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrSalaryItemBiz;
import app.erp.hr.dao.entity.ErpHrSalaryItem;

import java.util.List;

@BizModel("ErpHrSalaryItem")
public class ErpHrSalaryItemBizModel extends CrudBizModel<ErpHrSalaryItem> implements IErpHrSalaryItemBiz{
    public ErpHrSalaryItemBizModel(){
        setEntityName(ErpHrSalaryItem.class.getName());
    }

}
