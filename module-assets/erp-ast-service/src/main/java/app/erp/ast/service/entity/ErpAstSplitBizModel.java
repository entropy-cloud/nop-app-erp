
package app.erp.ast.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ast.biz.IErpAstSplitBiz;
import app.erp.ast.dao.entity.ErpAstSplit;

@BizModel("ErpAstSplit")
public class ErpAstSplitBizModel extends CrudBizModel<ErpAstSplit> implements IErpAstSplitBiz{
    public ErpAstSplitBizModel(){
        setEntityName(ErpAstSplit.class.getName());
    }
}
