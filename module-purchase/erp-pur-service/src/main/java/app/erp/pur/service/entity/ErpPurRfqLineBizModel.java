
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.pur.biz.IErpPurRfqLineBiz;
import app.erp.pur.dao.entity.ErpPurRfqLine;

@BizModel("ErpPurRfqLine")
public class ErpPurRfqLineBizModel extends CrudBizModel<ErpPurRfqLine> implements IErpPurRfqLineBiz{
    public ErpPurRfqLineBizModel(){
        setEntityName(ErpPurRfqLine.class.getName());
    }
}
