
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgMaterialIssueBiz;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;

@BizModel("ErpMfgMaterialIssue")
public class ErpMfgMaterialIssueBizModel extends CrudBizModel<ErpMfgMaterialIssue> implements IErpMfgMaterialIssueBiz{
    public ErpMfgMaterialIssueBizModel(){
        setEntityName(ErpMfgMaterialIssue.class.getName());
    }
}
