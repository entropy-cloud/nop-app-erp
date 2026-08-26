
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.pur.biz.IErpPurQuotationLineBiz;
import app.erp.pur.dao.entity.ErpPurQuotationLine;

@BizModel("ErpPurQuotationLine")
public class ErpPurQuotationLineBizModel extends AbstractErpCrudBizModel<ErpPurQuotationLine> implements IErpPurQuotationLineBiz{
    public ErpPurQuotationLineBizModel(){
        setEntityName(ErpPurQuotationLine.class.getName());
    }
}
