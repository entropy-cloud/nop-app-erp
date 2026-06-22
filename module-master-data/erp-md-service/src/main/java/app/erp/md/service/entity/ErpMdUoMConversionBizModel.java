
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdUoMConversionBiz;
import app.erp.md.dao.entity.ErpMdUoMConversion;

@BizModel("ErpMdUoMConversion")
public class ErpMdUoMConversionBizModel extends CrudBizModel<ErpMdUoMConversion> implements IErpMdUoMConversionBiz{
    public ErpMdUoMConversionBizModel(){
        setEntityName(ErpMdUoMConversion.class.getName());
    }
}
