
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import java.util.List;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntSparePartUsageLineBiz;
import app.erp.mnt.dao.entity.ErpMntSparePartUsageLine;

@BizModel("ErpMntSparePartUsageLine")
public class ErpMntSparePartUsageLineBizModel extends CrudBizModel<ErpMntSparePartUsageLine> implements IErpMntSparePartUsageLineBiz{
    public ErpMntSparePartUsageLineBizModel(){
        setEntityName(ErpMntSparePartUsageLine.class.getName());
    }

}
