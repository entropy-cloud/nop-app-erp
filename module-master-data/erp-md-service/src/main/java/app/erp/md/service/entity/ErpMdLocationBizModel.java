
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdLocationBiz;
import app.erp.md.dao.entity.ErpMdLocation;

@BizModel("ErpMdLocation")
public class ErpMdLocationBizModel extends CrudBizModel<ErpMdLocation> implements IErpMdLocationBiz{
    public ErpMdLocationBizModel(){
        setEntityName(ErpMdLocation.class.getName());
    }
}
