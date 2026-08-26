
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.crm.biz.IErpCrmLeadScoreConfigBiz;
import app.erp.crm.dao.entity.ErpCrmLeadScoreConfig;
import java.util.List;

@BizModel("ErpCrmLeadScoreConfig")
public class ErpCrmLeadScoreConfigBizModel extends AbstractErpCrudBizModel<ErpCrmLeadScoreConfig> implements IErpCrmLeadScoreConfigBiz{
    public ErpCrmLeadScoreConfigBizModel(){
        setEntityName(ErpCrmLeadScoreConfig.class.getName());
    }

    

}
