
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.pur.biz.IErpPurPaymentBiz;
import app.erp.pur.dao.entity.ErpPurPayment;

@BizModel("ErpPurPayment")
public class ErpPurPaymentBizModel extends CrudBizModel<ErpPurPayment> implements IErpPurPaymentBiz{
    public ErpPurPaymentBizModel(){
        setEntityName(ErpPurPayment.class.getName());
    }
}
