
package app.erp.aps.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.aps.biz.IErpApsConstraintBiz;
import app.erp.aps.dao.entity.ErpApsConstraint;

@BizModel("ErpApsConstraint")
public class ErpApsConstraintBizModel extends CrudBizModel<ErpApsConstraint> implements IErpApsConstraintBiz{
    public ErpApsConstraintBizModel(){
        setEntityName(ErpApsConstraint.class.getName());
    }
}
