
package app.erp.log.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.log.biz.IErpLogShipmentParcelBiz;
import app.erp.log.dao.entity.ErpLogShipmentParcel;

@BizModel("ErpLogShipmentParcel")
public class ErpLogShipmentParcelBizModel extends AbstractErpCrudBizModel<ErpLogShipmentParcel> implements IErpLogShipmentParcelBiz{
    public ErpLogShipmentParcelBizModel(){
        setEntityName(ErpLogShipmentParcel.class.getName());
    }

}
