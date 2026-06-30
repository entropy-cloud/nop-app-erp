
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmStageBiz;
import app.erp.crm.dao.entity.ErpCrmStage;

@BizModel("ErpCrmStage")
public class ErpCrmStageBizModel extends CrudBizModel<ErpCrmStage> implements IErpCrmStageBiz{
    public ErpCrmStageBizModel(){
        setEntityName(ErpCrmStage.class.getName());
    }
}
