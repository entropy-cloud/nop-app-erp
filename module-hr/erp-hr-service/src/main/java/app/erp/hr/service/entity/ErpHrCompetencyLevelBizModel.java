
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrCompetencyLevelBiz;
import app.erp.hr.dao.entity.ErpHrCompetencyLevel;

import java.util.List;

@BizModel("ErpHrCompetencyLevel")
public class ErpHrCompetencyLevelBizModel extends CrudBizModel<ErpHrCompetencyLevel> implements IErpHrCompetencyLevelBiz{
    public ErpHrCompetencyLevelBizModel(){
        setEntityName(ErpHrCompetencyLevel.class.getName());
    }

}
