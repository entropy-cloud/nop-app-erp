
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmLeadScoreLineBiz;
import app.erp.crm.dao.entity.ErpCrmLeadScoreLine;
import java.util.List;

@BizModel("ErpCrmLeadScoreLine")
public class ErpCrmLeadScoreLineBizModel extends CrudBizModel<ErpCrmLeadScoreLine> implements IErpCrmLeadScoreLineBiz{
    public ErpCrmLeadScoreLineBizModel(){
        setEntityName(ErpCrmLeadScoreLine.class.getName());
    }

    

}
