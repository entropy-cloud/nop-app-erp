
package app.erp.aps.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.aps.biz.IErpApsCapacityReservationBiz;
import app.erp.aps.dao.entity.ErpApsCapacityReservation;

@BizModel("ErpApsCapacityReservation")
public class ErpApsCapacityReservationBizModel extends CrudBizModel<ErpApsCapacityReservation> implements IErpApsCapacityReservationBiz{
    public ErpApsCapacityReservationBizModel(){
        setEntityName(ErpApsCapacityReservation.class.getName());
    }
}
