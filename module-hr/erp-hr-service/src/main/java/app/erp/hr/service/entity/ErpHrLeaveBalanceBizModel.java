
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

import app.erp.hr.biz.IErpHrLeaveBalanceBiz;
import app.erp.hr.dao.entity.ErpHrLeaveBalance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpHrLeaveBalance.class)
    public List<String> employeeDisplayName(@ContextSource List<ErpHrLeaveBalance> rows) {
        orm().batchLoadProps(rows, Collections.singleton("employee"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrLeaveBalance row : rows) {
            result.add(row.orm_attached() && row.getEmployee() != null ? row.getEmployee().getFullName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrLeaveBalance.class)
    public List<String> orgName(@ContextSource List<ErpHrLeaveBalance> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrLeaveBalance row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }
}
