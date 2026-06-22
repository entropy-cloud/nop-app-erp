
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgCostRollupBiz;
import app.erp.mfg.dao.entity.ErpMfgCostRollup;

@BizModel("ErpMfgCostRollup")
public class ErpMfgCostRollupBizModel extends CrudBizModel<ErpMfgCostRollup> implements IErpMfgCostRollupBiz{
    public ErpMfgCostRollupBizModel(){
        setEntityName(ErpMfgCostRollup.class.getName());
    }
}
