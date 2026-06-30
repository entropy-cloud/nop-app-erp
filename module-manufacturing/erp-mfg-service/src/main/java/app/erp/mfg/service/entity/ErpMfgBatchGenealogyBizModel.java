
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgBatchGenealogyBiz;
import app.erp.mfg.dao.entity.ErpMfgBatchGenealogy;

@BizModel("ErpMfgBatchGenealogy")
public class ErpMfgBatchGenealogyBizModel extends CrudBizModel<ErpMfgBatchGenealogy> implements IErpMfgBatchGenealogyBiz{
    public ErpMfgBatchGenealogyBizModel(){
        setEntityName(ErpMfgBatchGenealogy.class.getName());
    }
}
