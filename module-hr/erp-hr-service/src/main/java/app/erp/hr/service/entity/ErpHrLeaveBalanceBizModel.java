
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

import app.erp.hr.biz.IErpHrLeaveBalanceBiz;
import app.erp.hr.dao.entity.ErpHrLeaveBalance;

/**
 * 休假额度 BizModel（use-cases.md UC-HR-02）。标准 CRUD——HR 管理员维护年度额度。
 * usedDays 为派生值（实时聚合 Σ approved LeaveRequest.durationDays），不落库。
 */
@BizModel("ErpHrLeaveBalance")
public class ErpHrLeaveBalanceBizModel extends CrudBizModel<ErpHrLeaveBalance> implements IErpHrLeaveBalanceBiz {
    public ErpHrLeaveBalanceBizModel() {
        setEntityName(ErpHrLeaveBalance.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpHrLeaveBalance> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpHrLeaveBalance entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(CoreMetrics.today());
        }
        if (entity.getCarriedForwardDays() == null) {
            entity.setCarriedForwardDays(java.math.BigDecimal.ZERO);
        }
    }
}
