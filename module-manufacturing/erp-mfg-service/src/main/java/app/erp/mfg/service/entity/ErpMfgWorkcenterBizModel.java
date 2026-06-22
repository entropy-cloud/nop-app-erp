
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgWorkcenterBiz;
import app.erp.mfg.dao.entity.ErpMfgWorkcenter;

@BizModel("ErpMfgWorkcenter")
public class ErpMfgWorkcenterBizModel extends CrudBizModel<ErpMfgWorkcenter> implements IErpMfgWorkcenterBiz{
    public ErpMfgWorkcenterBizModel(){
        setEntityName(ErpMfgWorkcenter.class.getName());
    }
}
