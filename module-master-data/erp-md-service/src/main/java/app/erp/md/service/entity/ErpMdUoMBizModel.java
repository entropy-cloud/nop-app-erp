
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.md.biz.IErpMdUoMBiz;
import app.erp.md.dao.entity.ErpMdUoM;

@BizModel("ErpMdUoM")
public class ErpMdUoMBizModel extends AbstractErpCrudBizModel<ErpMdUoM> implements IErpMdUoMBiz{
    public ErpMdUoMBizModel(){
        setEntityName(ErpMdUoM.class.getName());
    }
}
