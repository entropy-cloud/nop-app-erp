
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinArApItemBiz;
import app.erp.fin.dao.entity.ErpFinArApItem;

@BizModel("ErpFinArApItem")
public class ErpFinArApItemBizModel extends CrudBizModel<ErpFinArApItem> implements IErpFinArApItemBiz{
    public ErpFinArApItemBizModel(){
        setEntityName(ErpFinArApItem.class.getName());
    }
}
