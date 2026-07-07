
package app.erp.ast.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ast.biz.IErpAstInventoryLineBiz;
import app.erp.ast.dao.entity.ErpAstInventoryLine;

@BizModel("ErpAstInventoryLine")
public class ErpAstInventoryLineBizModel extends CrudBizModel<ErpAstInventoryLine> implements IErpAstInventoryLineBiz{
    public ErpAstInventoryLineBizModel(){
        setEntityName(ErpAstInventoryLine.class.getName());
    }
}
