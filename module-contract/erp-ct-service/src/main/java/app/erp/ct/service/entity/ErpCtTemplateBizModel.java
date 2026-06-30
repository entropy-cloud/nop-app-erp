
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtTemplateBiz;
import app.erp.contract.dao.entity.ErpCtTemplate;

@BizModel("ErpCtTemplate")
public class ErpCtTemplateBizModel extends CrudBizModel<ErpCtTemplate> implements IErpCtTemplateBiz{
    public ErpCtTemplateBizModel(){
        setEntityName(ErpCtTemplate.class.getName());
    }
}
