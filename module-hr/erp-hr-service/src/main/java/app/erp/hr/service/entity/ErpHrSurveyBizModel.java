
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrSurveyBiz;
import app.erp.hr.dao.entity.ErpHrSurvey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpHrSurvey")
public class ErpHrSurveyBizModel extends CrudBizModel<ErpHrSurvey> implements IErpHrSurveyBiz{
    public ErpHrSurveyBizModel(){
        setEntityName(ErpHrSurvey.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpHrSurvey.class)
    public List<String> orgName(@ContextSource List<ErpHrSurvey> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrSurvey row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrSurvey.class)
    public List<String> targetDepartmentName(@ContextSource List<ErpHrSurvey> rows) {
        orm().batchLoadProps(rows, Collections.singleton("targetDepartment"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrSurvey row : rows) {
            result.add(row.orm_attached() && row.getTargetDepartment() != null ? row.getTargetDepartment().getName() : null);
        }
        return result;
    }
}
