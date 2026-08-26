
package app.erp.log.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.log.biz.IErpLogCarrierBiz;
import app.erp.log.dao.entity.ErpLogCarrier;

@BizModel("ErpLogCarrier")
public class ErpLogCarrierBizModel extends AbstractErpCrudBizModel<ErpLogCarrier> implements IErpLogCarrierBiz{
    public ErpLogCarrierBizModel(){
        setEntityName(ErpLogCarrier.class.getName());
    }

}
