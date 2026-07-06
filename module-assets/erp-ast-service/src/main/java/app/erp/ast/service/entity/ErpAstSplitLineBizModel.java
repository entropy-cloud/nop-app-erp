
package app.erp.ast.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ast.biz.IErpAstSplitLineBiz;
import app.erp.ast.dao.entity.ErpAstSplitLine;

@BizModel("ErpAstSplitLine")
public class ErpAstSplitLineBizModel extends CrudBizModel<ErpAstSplitLine> implements IErpAstSplitLineBiz{
    public ErpAstSplitLineBizModel(){
        setEntityName(ErpAstSplitLine.class.getName());
    }
}
