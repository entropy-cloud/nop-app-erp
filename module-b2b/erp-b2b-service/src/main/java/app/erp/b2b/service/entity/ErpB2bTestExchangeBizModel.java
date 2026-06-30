
package app.erp.b2b.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.b2b.biz.IErpB2bTestExchangeBiz;
import app.erp.b2b.dao.entity.ErpB2bTestExchange;

@BizModel("ErpB2bTestExchange")
public class ErpB2bTestExchangeBizModel extends CrudBizModel<ErpB2bTestExchange> implements IErpB2bTestExchangeBiz{
    public ErpB2bTestExchangeBizModel(){
        setEntityName(ErpB2bTestExchange.class.getName());
    }
}
