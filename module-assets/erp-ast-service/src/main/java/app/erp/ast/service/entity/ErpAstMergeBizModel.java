
package app.erp.ast.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ast.biz.IErpAstMergeBiz;
import app.erp.ast.dao.entity.ErpAstMerge;

@BizModel("ErpAstMerge")
public class ErpAstMergeBizModel extends CrudBizModel<ErpAstMerge> implements IErpAstMergeBiz{
    public ErpAstMergeBizModel(){
        setEntityName(ErpAstMerge.class.getName());
    }
}
