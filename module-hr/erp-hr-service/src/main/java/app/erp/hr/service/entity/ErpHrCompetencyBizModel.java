
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrCompetencyBiz;
import app.erp.hr.dao.entity.ErpHrCompetency;

@BizModel("ErpHrCompetency")
public class ErpHrCompetencyBizModel extends CrudBizModel<ErpHrCompetency> implements IErpHrCompetencyBiz{
    public ErpHrCompetencyBizModel(){
        setEntityName(ErpHrCompetency.class.getName());
    }
}
