
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmLeadScoreBiz;
import app.erp.crm.dao.entity.ErpCrmLeadScore;

@BizModel("ErpCrmLeadScore")
public class ErpCrmLeadScoreBizModel extends CrudBizModel<ErpCrmLeadScore> implements IErpCrmLeadScoreBiz{
    public ErpCrmLeadScoreBizModel(){
        setEntityName(ErpCrmLeadScore.class.getName());
    }
}
