
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrDepartmentBiz;
import app.erp.hr.dao.entity.ErpHrDepartment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpHrDepartment")
public class ErpHrDepartmentBizModel extends CrudBizModel<ErpHrDepartment> implements IErpHrDepartmentBiz{
    public ErpHrDepartmentBizModel(){
        setEntityName(ErpHrDepartment.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpHrDepartment.class)
    public List<String> parentName(@ContextSource List<ErpHrDepartment> rows) {
        orm().batchLoadProps(rows, Collections.singleton("parent"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrDepartment row : rows) {
            result.add(row.orm_attached() && row.getParent() != null ? row.getParent().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrDepartment.class)
    public List<String> managerDisplayName(@ContextSource List<ErpHrDepartment> rows) {
        orm().batchLoadProps(rows, Collections.singleton("manager"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrDepartment row : rows) {
            result.add(row.orm_attached() && row.getManager() != null ? row.getManager().getFullName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrDepartment.class)
    public List<String> costCenterName(@ContextSource List<ErpHrDepartment> rows) {
        orm().batchLoadProps(rows, Collections.singleton("costCenter"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrDepartment row : rows) {
            result.add(row.orm_attached() && row.getCostCenter() != null ? row.getCostCenter().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrDepartment.class)
    public List<String> orgName(@ContextSource List<ErpHrDepartment> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrDepartment row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }
}
