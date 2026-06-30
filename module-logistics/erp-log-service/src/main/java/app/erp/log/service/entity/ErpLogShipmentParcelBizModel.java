
package app.erp.log.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.log.biz.IErpLogShipmentParcelBiz;
import app.erp.log.dao.entity.ErpLogShipmentParcel;

@BizModel("ErpLogShipmentParcel")
public class ErpLogShipmentParcelBizModel extends CrudBizModel<ErpLogShipmentParcel> implements IErpLogShipmentParcelBiz{
    public ErpLogShipmentParcelBizModel(){
        setEntityName(ErpLogShipmentParcel.class.getName());
    }
}
