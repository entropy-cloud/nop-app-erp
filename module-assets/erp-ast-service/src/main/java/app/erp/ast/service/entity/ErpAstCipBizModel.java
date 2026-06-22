
package app.erp.ast.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ast.biz.IErpAstCipBiz;
import app.erp.ast.dao.entity.ErpAstCip;

@BizModel("ErpAstCip")
public class ErpAstCipBizModel extends CrudBizModel<ErpAstCip> implements IErpAstCipBiz{
    public ErpAstCipBizModel(){
        setEntityName(ErpAstCip.class.getName());
    }
}
