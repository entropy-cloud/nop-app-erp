
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmLeadScoreConfigLineBiz;
import app.erp.crm.dao.entity.ErpCrmLeadScoreConfigLine;
import java.util.List;

@BizModel("ErpCrmLeadScoreConfigLine")
public class ErpCrmLeadScoreConfigLineBizModel extends CrudBizModel<ErpCrmLeadScoreConfigLine> implements IErpCrmLeadScoreConfigLineBiz{
    public ErpCrmLeadScoreConfigLineBizModel(){
        setEntityName(ErpCrmLeadScoreConfigLine.class.getName());
    }

    

}
