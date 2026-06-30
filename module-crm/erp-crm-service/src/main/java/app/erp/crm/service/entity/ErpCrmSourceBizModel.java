
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmSourceBiz;
import app.erp.crm.dao.entity.ErpCrmSource;

@BizModel("ErpCrmSource")
public class ErpCrmSourceBizModel extends CrudBizModel<ErpCrmSource> implements IErpCrmSourceBiz{
    public ErpCrmSourceBizModel(){
        setEntityName(ErpCrmSource.class.getName());
    }
}
