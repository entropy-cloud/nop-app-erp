
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrLeaveRequestBiz;
import app.erp.hr.dao.entity.ErpHrLeaveRequest;

@BizModel("ErpHrLeaveRequest")
public class ErpHrLeaveRequestBizModel extends CrudBizModel<ErpHrLeaveRequest> implements IErpHrLeaveRequestBiz{
    public ErpHrLeaveRequestBizModel(){
        setEntityName(ErpHrLeaveRequest.class.getName());
    }
}
