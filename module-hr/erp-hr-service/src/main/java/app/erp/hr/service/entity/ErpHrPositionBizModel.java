
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrPositionBiz;
import app.erp.hr.dao.entity.ErpHrPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpHrPosition")
public class ErpHrPositionBizModel extends CrudBizModel<ErpHrPosition> implements IErpHrPositionBiz{
    public ErpHrPositionBizModel(){
        setEntityName(ErpHrPosition.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpHrPosition.class)
    public List<String> departmentName(@ContextSource List<ErpHrPosition> rows) {
        orm().batchLoadProps(rows, Collections.singleton("department"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrPosition row : rows) {
            result.add(row.orm_attached() && row.getDepartment() != null ? row.getDepartment().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrPosition.class)
    public List<String> orgName(@ContextSource List<ErpHrPosition> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrPosition row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }
}
