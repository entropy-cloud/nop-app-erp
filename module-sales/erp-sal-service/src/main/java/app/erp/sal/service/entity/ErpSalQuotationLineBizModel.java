
package app.erp.sal.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.sal.biz.IErpSalQuotationLineBiz;
import app.erp.sal.dao.entity.ErpSalQuotationLine;

import java.util.List;

@BizModel("ErpSalQuotationLine")
public class ErpSalQuotationLineBizModel extends CrudBizModel<ErpSalQuotationLine> implements IErpSalQuotationLineBiz{
    public ErpSalQuotationLineBizModel(){
        setEntityName(ErpSalQuotationLine.class.getName());
    }

}
