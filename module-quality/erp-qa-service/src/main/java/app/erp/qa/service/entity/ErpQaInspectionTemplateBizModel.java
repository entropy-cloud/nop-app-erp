
package app.erp.qa.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.qa.biz.IErpQaInspectionTemplateBiz;
import app.erp.qa.dao.entity.ErpQaInspectionTemplate;

@BizModel("ErpQaInspectionTemplate")
public class ErpQaInspectionTemplateBizModel extends CrudBizModel<ErpQaInspectionTemplate> implements IErpQaInspectionTemplateBiz{
    public ErpQaInspectionTemplateBizModel(){
        setEntityName(ErpQaInspectionTemplate.class.getName());
    }
}
