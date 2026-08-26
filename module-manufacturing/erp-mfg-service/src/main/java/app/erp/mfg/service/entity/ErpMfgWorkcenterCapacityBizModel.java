
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.mfg.biz.IErpMfgWorkcenterCapacityBiz;
import app.erp.mfg.dao.entity.ErpMfgWorkcenterCapacity;

@BizModel("ErpMfgWorkcenterCapacity")
public class ErpMfgWorkcenterCapacityBizModel extends AbstractErpCrudBizModel<ErpMfgWorkcenterCapacity> implements IErpMfgWorkcenterCapacityBiz{
    public ErpMfgWorkcenterCapacityBizModel(){
        setEntityName(ErpMfgWorkcenterCapacity.class.getName());
    }
}
