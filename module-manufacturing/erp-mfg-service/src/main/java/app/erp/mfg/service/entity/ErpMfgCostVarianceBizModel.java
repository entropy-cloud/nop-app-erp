
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgCostVarianceBiz;
import app.erp.mfg.dao.entity.ErpMfgCostVariance;

@BizModel("ErpMfgCostVariance")
public class ErpMfgCostVarianceBizModel extends CrudBizModel<ErpMfgCostVariance> implements IErpMfgCostVarianceBiz{
    public ErpMfgCostVarianceBizModel(){
        setEntityName(ErpMfgCostVariance.class.getName());
    }
}
