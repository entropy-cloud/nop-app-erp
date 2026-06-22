
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.pur.biz.IErpPurReturnBiz;
import app.erp.pur.dao.entity.ErpPurReturn;

@BizModel("ErpPurReturn")
public class ErpPurReturnBizModel extends CrudBizModel<ErpPurReturn> implements IErpPurReturnBiz{
    public ErpPurReturnBizModel(){
        setEntityName(ErpPurReturn.class.getName());
    }
}
