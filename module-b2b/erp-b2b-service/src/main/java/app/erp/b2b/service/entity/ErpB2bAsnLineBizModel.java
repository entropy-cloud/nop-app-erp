
package app.erp.b2b.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.b2b.biz.IErpB2bAsnLineBiz;
import app.erp.b2b.dao.entity.ErpB2bAsnLine;

@BizModel("ErpB2bAsnLine")
public class ErpB2bAsnLineBizModel extends CrudBizModel<ErpB2bAsnLine> implements IErpB2bAsnLineBiz{
    public ErpB2bAsnLineBizModel(){
        setEntityName(ErpB2bAsnLine.class.getName());
    }

}
