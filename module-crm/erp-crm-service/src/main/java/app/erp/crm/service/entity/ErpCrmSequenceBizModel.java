
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmSequenceBiz;
import app.erp.crm.dao.entity.ErpCrmSequence;

@BizModel("ErpCrmSequence")
public class ErpCrmSequenceBizModel extends CrudBizModel<ErpCrmSequence> implements IErpCrmSequenceBiz{
    public ErpCrmSequenceBizModel(){
        setEntityName(ErpCrmSequence.class.getName());
    }
}
