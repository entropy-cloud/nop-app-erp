
package app.erp.sal.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.sal.biz.IErpSalOrderLineBiz;
import app.erp.sal.dao.entity.ErpSalOrderLine;

@BizModel("ErpSalOrderLine")
public class ErpSalOrderLineBizModel extends CrudBizModel<ErpSalOrderLine> implements IErpSalOrderLineBiz{
    public ErpSalOrderLineBizModel(){
        setEntityName(ErpSalOrderLine.class.getName());
    }
}
