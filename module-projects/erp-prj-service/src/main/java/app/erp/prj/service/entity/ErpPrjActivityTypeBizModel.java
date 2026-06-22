
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjActivityTypeBiz;
import app.erp.prj.dao.entity.ErpPrjActivityType;

@BizModel("ErpPrjActivityType")
public class ErpPrjActivityTypeBizModel extends CrudBizModel<ErpPrjActivityType> implements IErpPrjActivityTypeBiz{
    public ErpPrjActivityTypeBizModel(){
        setEntityName(ErpPrjActivityType.class.getName());
    }
}
