
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmQuoteTemplateBiz;
import app.erp.crm.dao.entity.ErpCrmQuoteTemplate;

@BizModel("ErpCrmQuoteTemplate")
public class ErpCrmQuoteTemplateBizModel extends CrudBizModel<ErpCrmQuoteTemplate> implements IErpCrmQuoteTemplateBiz{
    public ErpCrmQuoteTemplateBizModel(){
        setEntityName(ErpCrmQuoteTemplate.class.getName());
    }
}
