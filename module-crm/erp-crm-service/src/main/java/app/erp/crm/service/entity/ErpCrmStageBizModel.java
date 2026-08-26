
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.crm.biz.IErpCrmStageBiz;
import app.erp.crm.dao.entity.ErpCrmStage;
import java.util.List;

@BizModel("ErpCrmStage")
public class ErpCrmStageBizModel extends AbstractErpCrudBizModel<ErpCrmStage> implements IErpCrmStageBiz{
    public ErpCrmStageBizModel(){
        setEntityName(ErpCrmStage.class.getName());
    }

    

}
