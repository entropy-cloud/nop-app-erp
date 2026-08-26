
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.mfg.biz.IErpMfgRoutingOperationBiz;
import app.erp.mfg.dao.entity.ErpMfgRoutingOperation;

import java.util.List;

@BizModel("ErpMfgRoutingOperation")
public class ErpMfgRoutingOperationBizModel extends AbstractErpCrudBizModel<ErpMfgRoutingOperation> implements IErpMfgRoutingOperationBiz{
    public ErpMfgRoutingOperationBizModel(){
        setEntityName(ErpMfgRoutingOperation.class.getName());
    }

}
