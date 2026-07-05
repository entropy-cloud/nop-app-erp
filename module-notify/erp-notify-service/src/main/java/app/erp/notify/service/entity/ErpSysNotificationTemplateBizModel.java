
package app.erp.notify.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.notify.biz.IErpSysNotificationTemplateBiz;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;

@BizModel("ErpSysNotificationTemplate")
public class ErpSysNotificationTemplateBizModel extends CrudBizModel<ErpSysNotificationTemplate> implements IErpSysNotificationTemplateBiz{
    public ErpSysNotificationTemplateBizModel(){
        setEntityName(ErpSysNotificationTemplate.class.getName());
    }
}
