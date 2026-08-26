
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.pur.biz.IErpPurPaymentLineBiz;
import app.erp.pur.dao.entity.ErpPurPaymentLine;

@BizModel("ErpPurPaymentLine")
public class ErpPurPaymentLineBizModel extends AbstractErpCrudBizModel<ErpPurPaymentLine> implements IErpPurPaymentLineBiz{
    public ErpPurPaymentLineBizModel(){
        setEntityName(ErpPurPaymentLine.class.getName());
    }
}
