
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntDowntimeEntryBiz;
import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;

@BizModel("ErpMntDowntimeEntry")
public class ErpMntDowntimeEntryBizModel extends CrudBizModel<ErpMntDowntimeEntry> implements IErpMntDowntimeEntryBiz{
    public ErpMntDowntimeEntryBizModel(){
        setEntityName(ErpMntDowntimeEntry.class.getName());
    }
}
