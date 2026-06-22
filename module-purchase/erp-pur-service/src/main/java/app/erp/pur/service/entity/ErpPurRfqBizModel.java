
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.pur.biz.IErpPurRfqBiz;
import app.erp.pur.dao.entity.ErpPurRfq;

@BizModel("ErpPurRfq")
public class ErpPurRfqBizModel extends CrudBizModel<ErpPurRfq> implements IErpPurRfqBiz{
    public ErpPurRfqBizModel(){
        setEntityName(ErpPurRfq.class.getName());
    }
}
