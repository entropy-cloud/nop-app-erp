
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmActivityBiz;
import app.erp.crm.dao.entity.ErpCrmActivity;
import java.util.List;

@BizModel("ErpCrmActivity")
public class ErpCrmActivityBizModel extends CrudBizModel<ErpCrmActivity> implements IErpCrmActivityBiz{
    public ErpCrmActivityBizModel(){
        setEntityName(ErpCrmActivity.class.getName());
    }

    

}
