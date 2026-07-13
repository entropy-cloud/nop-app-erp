
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrSurveyResultBiz;
import app.erp.hr.dao.entity.ErpHrSurveyResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpHrSurveyResult")
public class ErpHrSurveyResultBizModel extends CrudBizModel<ErpHrSurveyResult> implements IErpHrSurveyResultBiz{
    public ErpHrSurveyResultBizModel(){
        setEntityName(ErpHrSurveyResult.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpHrSurveyResult.class)
    public List<String> surveyTitle(@ContextSource List<ErpHrSurveyResult> rows) {
        orm().batchLoadProps(rows, Collections.singleton("survey"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrSurveyResult row : rows) {
            result.add(row.orm_attached() && row.getSurvey() != null ? row.getSurvey().getTitle() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrSurveyResult.class)
    public List<String> departmentName(@ContextSource List<ErpHrSurveyResult> rows) {
        orm().batchLoadProps(rows, Collections.singleton("department"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrSurveyResult row : rows) {
            result.add(row.orm_attached() && row.getDepartment() != null ? row.getDepartment().getName() : null);
        }
        return result;
    }
}
