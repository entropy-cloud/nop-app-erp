
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmLeadScoreConfigBiz;
import app.erp.crm.dao.entity.ErpCrmLeadScoreConfig;

@BizModel("ErpCrmLeadScoreConfig")
public class ErpCrmLeadScoreConfigBizModel extends CrudBizModel<ErpCrmLeadScoreConfig> implements IErpCrmLeadScoreConfigBiz{
    public ErpCrmLeadScoreConfigBizModel(){
        setEntityName(ErpCrmLeadScoreConfig.class.getName());
    }
}
