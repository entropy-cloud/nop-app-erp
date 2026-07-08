
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrRecruitmentBiz;
import app.erp.hr.dao.entity.ErpHrRecruitment;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

@BizModel("ErpHrRecruitment")
public class ErpHrRecruitmentBizModel extends CrudBizModel<ErpHrRecruitment> implements IErpHrRecruitmentBiz{
    public ErpHrRecruitmentBizModel(){
        setEntityName(ErpHrRecruitment.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpHrRecruitment> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpHrRecruitment entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
    }

}
