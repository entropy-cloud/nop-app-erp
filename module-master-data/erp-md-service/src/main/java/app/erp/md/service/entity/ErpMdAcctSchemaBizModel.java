
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdAcctSchemaBiz;
import app.erp.md.dao.entity.ErpMdAcctSchema;

@BizModel("ErpMdAcctSchema")
public class ErpMdAcctSchemaBizModel extends CrudBizModel<ErpMdAcctSchema> implements IErpMdAcctSchemaBiz{
    public ErpMdAcctSchemaBizModel(){
        setEntityName(ErpMdAcctSchema.class.getName());
    }
}
