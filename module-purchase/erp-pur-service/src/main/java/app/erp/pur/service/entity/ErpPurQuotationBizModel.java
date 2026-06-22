
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.pur.biz.IErpPurQuotationBiz;
import app.erp.pur.dao.entity.ErpPurQuotation;

@BizModel("ErpPurQuotation")
public class ErpPurQuotationBizModel extends CrudBizModel<ErpPurQuotation> implements IErpPurQuotationBiz{
    public ErpPurQuotationBizModel(){
        setEntityName(ErpPurQuotation.class.getName());
    }
}
