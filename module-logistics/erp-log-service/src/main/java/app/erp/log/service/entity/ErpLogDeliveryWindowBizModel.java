
package app.erp.log.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.log.biz.IErpLogDeliveryWindowBiz;
import app.erp.log.dao.entity.ErpLogDeliveryWindow;

@BizModel("ErpLogDeliveryWindow")
public class ErpLogDeliveryWindowBizModel extends AbstractErpCrudBizModel<ErpLogDeliveryWindow> implements IErpLogDeliveryWindowBiz{
    public ErpLogDeliveryWindowBizModel(){
        setEntityName(ErpLogDeliveryWindow.class.getName());
    }

}
