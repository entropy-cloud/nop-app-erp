
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstCipCostItemBiz;
import app.erp.ast.dao.entity.ErpAstCipCostItem;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import java.util.List;

@BizModel("ErpAstCipCostItem")
public class ErpAstCipCostItemBizModel extends CrudBizModel<ErpAstCipCostItem> implements IErpAstCipCostItemBiz {
    public ErpAstCipCostItemBizModel() {
        setEntityName(ErpAstCipCostItem.class.getName());
    }

}
