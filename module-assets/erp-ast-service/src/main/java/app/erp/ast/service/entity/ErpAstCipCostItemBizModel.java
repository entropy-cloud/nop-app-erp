
package app.erp.ast.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ast.biz.IErpAstCipCostItemBiz;
import app.erp.ast.dao.entity.ErpAstCipCostItem;

@BizModel("ErpAstCipCostItem")
public class ErpAstCipCostItemBizModel extends CrudBizModel<ErpAstCipCostItem> implements IErpAstCipCostItemBiz{
    public ErpAstCipCostItemBizModel(){
        setEntityName(ErpAstCipCostItem.class.getName());
    }
}
