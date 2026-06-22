
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinVoucherLineBiz;
import app.erp.fin.dao.entity.ErpFinVoucherLine;

@BizModel("ErpFinVoucherLine")
public class ErpFinVoucherLineBizModel extends CrudBizModel<ErpFinVoucherLine> implements IErpFinVoucherLineBiz{
    public ErpFinVoucherLineBizModel(){
        setEntityName(ErpFinVoucherLine.class.getName());
    }
}
