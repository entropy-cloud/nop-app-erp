
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgMaterialIssueLineBiz;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;

import java.util.List;

@BizModel("ErpMfgMaterialIssueLine")
public class ErpMfgMaterialIssueLineBizModel extends CrudBizModel<ErpMfgMaterialIssueLine> implements IErpMfgMaterialIssueLineBiz{
    public ErpMfgMaterialIssueLineBizModel(){
        setEntityName(ErpMfgMaterialIssueLine.class.getName());
    }

}
