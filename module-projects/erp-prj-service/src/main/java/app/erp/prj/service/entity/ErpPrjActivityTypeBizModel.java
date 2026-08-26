
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.prj.biz.IErpPrjActivityTypeBiz;
import app.erp.prj.dao.entity.ErpPrjActivityType;

import java.util.List;

@BizModel("ErpPrjActivityType")
public class ErpPrjActivityTypeBizModel extends AbstractErpCrudBizModel<ErpPrjActivityType> implements IErpPrjActivityTypeBiz{
    public ErpPrjActivityTypeBizModel(){
        setEntityName(ErpPrjActivityType.class.getName());
    }

}
