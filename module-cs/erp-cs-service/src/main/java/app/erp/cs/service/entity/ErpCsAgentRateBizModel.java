
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.cs.biz.IErpCsAgentRateBiz;
import app.erp.cs.dao.entity.ErpCsAgentRate;
import java.util.List;

@BizModel("ErpCsAgentRate")
public class ErpCsAgentRateBizModel extends AbstractErpCrudBizModel<ErpCsAgentRate> implements IErpCsAgentRateBiz{
    public ErpCsAgentRateBizModel(){
        setEntityName(ErpCsAgentRate.class.getName());
    }

    

}
