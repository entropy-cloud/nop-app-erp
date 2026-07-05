
package app.erp.notify.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.notify.biz.IErpSysNotificationReadBiz;
import app.erp.notify.dao.entity.ErpSysNotificationRead;

@BizModel("ErpSysNotificationRead")
public class ErpSysNotificationReadBizModel extends CrudBizModel<ErpSysNotificationRead> implements IErpSysNotificationReadBiz{
    public ErpSysNotificationReadBizModel(){
        setEntityName(ErpSysNotificationRead.class.getName());
    }
}
