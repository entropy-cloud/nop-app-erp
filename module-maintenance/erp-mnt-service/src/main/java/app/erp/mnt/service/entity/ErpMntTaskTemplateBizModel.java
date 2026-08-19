
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntTaskTemplateBiz;
import app.erp.mnt.dao.entity.ErpMntTaskTemplate;

@BizModel("ErpMntTaskTemplate")
public class ErpMntTaskTemplateBizModel extends CrudBizModel<ErpMntTaskTemplate> implements IErpMntTaskTemplateBiz{
    public ErpMntTaskTemplateBizModel(){
        setEntityName(ErpMntTaskTemplate.class.getName());
    }
}
