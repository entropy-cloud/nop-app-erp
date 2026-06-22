
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgMrpPlanLineBiz;
import app.erp.mfg.dao.entity.ErpMfgMrpPlanLine;

@BizModel("ErpMfgMrpPlanLine")
public class ErpMfgMrpPlanLineBizModel extends CrudBizModel<ErpMfgMrpPlanLine> implements IErpMfgMrpPlanLineBiz{
    public ErpMfgMrpPlanLineBizModel(){
        setEntityName(ErpMfgMrpPlanLine.class.getName());
    }
}
