
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.inv.biz.IErpInvReservationLineBiz;
import app.erp.inv.dao.entity.ErpInvReservationLine;

@BizModel("ErpInvReservationLine")
public class ErpInvReservationLineBizModel extends CrudBizModel<ErpInvReservationLine> implements IErpInvReservationLineBiz{
    public ErpInvReservationLineBizModel(){
        setEntityName(ErpInvReservationLine.class.getName());
    }
}
