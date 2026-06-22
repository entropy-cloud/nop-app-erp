
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.pur.biz.IErpPurReceiveBiz;
import app.erp.pur.dao.entity.ErpPurReceive;

@BizModel("ErpPurReceive")
public class ErpPurReceiveBizModel extends CrudBizModel<ErpPurReceive> implements IErpPurReceiveBiz{
    public ErpPurReceiveBizModel(){
        setEntityName(ErpPurReceive.class.getName());
    }
}
