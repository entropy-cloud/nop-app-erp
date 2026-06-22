
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.entity.ErpFinVoucher;

@BizModel("ErpFinVoucher")
public class ErpFinVoucherBizModel extends CrudBizModel<ErpFinVoucher> implements IErpFinVoucherBiz{
    public ErpFinVoucherBizModel(){
        setEntityName(ErpFinVoucher.class.getName());
    }
}
