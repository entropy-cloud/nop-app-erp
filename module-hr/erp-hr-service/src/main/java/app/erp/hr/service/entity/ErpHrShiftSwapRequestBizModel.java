
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrShiftSwapRequestBiz;
import app.erp.hr.dao.entity.ErpHrShiftSwapRequest;

@BizModel("ErpHrShiftSwapRequest")
public class ErpHrShiftSwapRequestBizModel extends CrudBizModel<ErpHrShiftSwapRequest> implements IErpHrShiftSwapRequestBiz{
    public ErpHrShiftSwapRequestBizModel(){
        setEntityName(ErpHrShiftSwapRequest.class.getName());
    }
}
