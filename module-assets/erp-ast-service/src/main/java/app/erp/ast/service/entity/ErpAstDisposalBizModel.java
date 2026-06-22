
package app.erp.ast.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ast.biz.IErpAstDisposalBiz;
import app.erp.ast.dao.entity.ErpAstDisposal;

@BizModel("ErpAstDisposal")
public class ErpAstDisposalBizModel extends CrudBizModel<ErpAstDisposal> implements IErpAstDisposalBiz{
    public ErpAstDisposalBizModel(){
        setEntityName(ErpAstDisposal.class.getName());
    }
}
