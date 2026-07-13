
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrTimesheetBiz;
import app.erp.hr.dao.entity.ErpHrTimesheet;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpHrTimesheet")
public class ErpHrTimesheetBizModel extends CrudBizModel<ErpHrTimesheet> implements IErpHrTimesheetBiz{
    public ErpHrTimesheetBizModel(){
        setEntityName(ErpHrTimesheet.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpHrTimesheet> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpHrTimesheet entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpHrTimesheet.class)
    public List<String> employeeDisplayName(@ContextSource List<ErpHrTimesheet> rows) {
        orm().batchLoadProps(rows, Collections.singleton("employee"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrTimesheet row : rows) {
            result.add(row.orm_attached() && row.getEmployee() != null ? row.getEmployee().getFullName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrTimesheet.class)
    public List<String> orgName(@ContextSource List<ErpHrTimesheet> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrTimesheet row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
