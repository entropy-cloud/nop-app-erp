
package app.erp.ast.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ast.biz.IErpAstMergeLineBiz;
import app.erp.ast.dao.entity.ErpAstMergeLine;

@BizModel("ErpAstMergeLine")
public class ErpAstMergeLineBizModel extends CrudBizModel<ErpAstMergeLine> implements IErpAstMergeLineBiz{
    public ErpAstMergeLineBizModel(){
        setEntityName(ErpAstMergeLine.class.getName());
    }
}
