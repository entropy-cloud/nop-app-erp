
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinVoucherBillRBiz;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;

import java.util.List;

@BizModel("ErpFinVoucherBillR")
public class ErpFinVoucherBillRBizModel extends CrudBizModel<ErpFinVoucherBillR> implements IErpFinVoucherBillRBiz{
    public ErpFinVoucherBillRBizModel(){
        setEntityName(ErpFinVoucherBillR.class.getName());
    }

}
