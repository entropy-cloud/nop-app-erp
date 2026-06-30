
package app.erp.drp.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.drp.biz.IErpInvDrpDockAppointmentBiz;
import app.erp.drp.dao.entity.ErpInvDrpDockAppointment;

@BizModel("ErpInvDrpDockAppointment")
public class ErpInvDrpDockAppointmentBizModel extends CrudBizModel<ErpInvDrpDockAppointment> implements IErpInvDrpDockAppointmentBiz{
    public ErpInvDrpDockAppointmentBizModel(){
        setEntityName(ErpInvDrpDockAppointment.class.getName());
    }
}
