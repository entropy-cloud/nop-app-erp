
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.inv.biz.IErpInvReservationBiz;
import app.erp.inv.dao.entity.ErpInvReservation;

@BizModel("ErpInvReservation")
public class ErpInvReservationBizModel extends CrudBizModel<ErpInvReservation> implements IErpInvReservationBiz{
    public ErpInvReservationBizModel(){
        setEntityName(ErpInvReservation.class.getName());
    }
}
