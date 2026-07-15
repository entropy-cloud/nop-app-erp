
package app.erp.sal.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.sal.biz.IErpSalReturnLineBiz;
import app.erp.sal.dao.entity.ErpSalReturnLine;

import java.util.List;

@BizModel("ErpSalReturnLine")
public class ErpSalReturnLineBizModel extends CrudBizModel<ErpSalReturnLine> implements IErpSalReturnLineBiz{
    public ErpSalReturnLineBizModel(){
        setEntityName(ErpSalReturnLine.class.getName());
    }

}
