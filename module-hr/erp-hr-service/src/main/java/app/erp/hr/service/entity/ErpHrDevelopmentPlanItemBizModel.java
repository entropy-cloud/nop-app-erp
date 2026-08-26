
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.hr.biz.IErpHrDevelopmentPlanItemBiz;
import app.erp.hr.dao.entity.ErpHrDevelopmentPlanItem;

import java.util.List;

@BizModel("ErpHrDevelopmentPlanItem")
public class ErpHrDevelopmentPlanItemBizModel extends AbstractErpCrudBizModel<ErpHrDevelopmentPlanItem> implements IErpHrDevelopmentPlanItemBiz{
    public ErpHrDevelopmentPlanItemBizModel(){
        setEntityName(ErpHrDevelopmentPlanItem.class.getName());
    }

}
