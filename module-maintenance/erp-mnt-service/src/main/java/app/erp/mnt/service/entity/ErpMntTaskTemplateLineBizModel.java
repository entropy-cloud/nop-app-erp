
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntTaskTemplateLineBiz;
import app.erp.mnt.dao.entity.ErpMntTaskTemplateLine;

@BizModel("ErpMntTaskTemplateLine")
public class ErpMntTaskTemplateLineBizModel extends CrudBizModel<ErpMntTaskTemplateLine> implements IErpMntTaskTemplateLineBiz{
    public ErpMntTaskTemplateLineBizModel(){
        setEntityName(ErpMntTaskTemplateLine.class.getName());
    }
}
