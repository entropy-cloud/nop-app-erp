
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrTimesheetLineBiz;
import app.erp.hr.dao.entity.ErpHrTimesheetLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpHrTimesheetLine")
public class ErpHrTimesheetLineBizModel extends CrudBizModel<ErpHrTimesheetLine> implements IErpHrTimesheetLineBiz{
    public ErpHrTimesheetLineBizModel(){
        setEntityName(ErpHrTimesheetLine.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpHrTimesheetLine.class)
    public List<String> timesheetCode(@ContextSource List<ErpHrTimesheetLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("timesheet"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrTimesheetLine row : rows) {
            result.add(row.orm_attached() && row.getTimesheet() != null ? row.getTimesheet().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrTimesheetLine.class)
    public List<String> employeeDisplayName(@ContextSource List<ErpHrTimesheetLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("employee"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrTimesheetLine row : rows) {
            result.add(row.orm_attached() && row.getEmployee() != null ? row.getEmployee().getFullName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrTimesheetLine.class)
    public List<String> projectName(@ContextSource List<ErpHrTimesheetLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("project"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrTimesheetLine row : rows) {
            result.add(row.orm_attached() && row.getProject() != null ? row.getProject().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrTimesheetLine.class)
    public List<String> taskName(@ContextSource List<ErpHrTimesheetLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("task"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrTimesheetLine row : rows) {
            result.add(row.orm_attached() && row.getTask() != null ? row.getTask().getTitle() : null);
        }
        return result;
    }
}
