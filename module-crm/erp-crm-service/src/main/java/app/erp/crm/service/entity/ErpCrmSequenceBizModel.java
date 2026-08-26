
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.crm.biz.IErpCrmSequenceBiz;
import app.erp.crm.dao.entity.ErpCrmSequence;
import java.util.List;

@BizModel("ErpCrmSequence")
public class ErpCrmSequenceBizModel extends AbstractErpCrudBizModel<ErpCrmSequence> implements IErpCrmSequenceBiz{
    public ErpCrmSequenceBizModel(){
        setEntityName(ErpCrmSequence.class.getName());
    }

    

}
