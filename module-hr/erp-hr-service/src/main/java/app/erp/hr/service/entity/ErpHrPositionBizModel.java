
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrPositionBiz;
import app.erp.hr.dao.entity.ErpHrPosition;

@BizModel("ErpHrPosition")
public class ErpHrPositionBizModel extends CrudBizModel<ErpHrPosition> implements IErpHrPositionBiz{
    public ErpHrPositionBizModel(){
        setEntityName(ErpHrPosition.class.getName());
    }
}
