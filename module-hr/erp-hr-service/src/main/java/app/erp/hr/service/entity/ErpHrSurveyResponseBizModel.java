
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrSurveyResponseBiz;
import app.erp.hr.dao.entity.ErpHrSurveyResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpHrSurveyResponse")
public class ErpHrSurveyResponseBizModel extends CrudBizModel<ErpHrSurveyResponse> implements IErpHrSurveyResponseBiz{
    public ErpHrSurveyResponseBizModel(){
        setEntityName(ErpHrSurveyResponse.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpHrSurveyResponse.class)
    public List<String> surveyTitle(@ContextSource List<ErpHrSurveyResponse> rows) {
        orm().batchLoadProps(rows, Collections.singleton("survey"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrSurveyResponse row : rows) {
            result.add(row.orm_attached() && row.getSurvey() != null ? row.getSurvey().getTitle() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrSurveyResponse.class)
    public List<String> employeeDisplayName(@ContextSource List<ErpHrSurveyResponse> rows) {
        orm().batchLoadProps(rows, Collections.singleton("employee"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrSurveyResponse row : rows) {
            result.add(row.orm_attached() && row.getEmployee() != null ? row.getEmployee().getFullName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrSurveyResponse.class)
    public List<String> orgName(@ContextSource List<ErpHrSurveyResponse> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrSurveyResponse row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }
}
