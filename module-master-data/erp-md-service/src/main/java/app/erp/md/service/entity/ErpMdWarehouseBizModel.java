
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdWarehouseBiz;
import app.erp.md.dao.entity.ErpMdWarehouse;

@BizModel("ErpMdWarehouse")
public class ErpMdWarehouseBizModel extends CrudBizModel<ErpMdWarehouse> implements IErpMdWarehouseBiz{
    public ErpMdWarehouseBizModel(){
        setEntityName(ErpMdWarehouse.class.getName());
    }
}
