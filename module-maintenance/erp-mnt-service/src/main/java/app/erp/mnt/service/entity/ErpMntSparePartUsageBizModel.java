
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntSparePartUsageBiz;
import app.erp.mnt.dao.entity.ErpMntSparePartUsage;

@BizModel("ErpMntSparePartUsage")
public class ErpMntSparePartUsageBizModel extends CrudBizModel<ErpMntSparePartUsage> implements IErpMntSparePartUsageBiz{
    public ErpMntSparePartUsageBizModel(){
        setEntityName(ErpMntSparePartUsage.class.getName());
    }
}
