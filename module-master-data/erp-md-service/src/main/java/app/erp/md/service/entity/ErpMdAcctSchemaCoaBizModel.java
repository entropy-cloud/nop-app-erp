
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdAcctSchemaCoaBiz;
import app.erp.md.dao.entity.ErpMdAcctSchemaCoa;

@BizModel("ErpMdAcctSchemaCoa")
public class ErpMdAcctSchemaCoaBizModel extends CrudBizModel<ErpMdAcctSchemaCoa> implements IErpMdAcctSchemaCoaBiz{
    public ErpMdAcctSchemaCoaBizModel(){
        setEntityName(ErpMdAcctSchemaCoa.class.getName());
    }
}
