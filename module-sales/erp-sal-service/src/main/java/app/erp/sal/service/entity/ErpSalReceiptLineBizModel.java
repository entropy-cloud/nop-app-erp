
package app.erp.sal.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.sal.biz.IErpSalReceiptLineBiz;
import app.erp.sal.dao.entity.ErpSalReceiptLine;

@BizModel("ErpSalReceiptLine")
public class ErpSalReceiptLineBizModel extends AbstractErpCrudBizModel<ErpSalReceiptLine> implements IErpSalReceiptLineBiz{
    public ErpSalReceiptLineBizModel(){
        setEntityName(ErpSalReceiptLine.class.getName());
    }
}
