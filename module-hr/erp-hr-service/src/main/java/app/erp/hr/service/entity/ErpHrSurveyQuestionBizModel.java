
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrSurveyQuestionBiz;
import app.erp.hr.dao.entity.ErpHrSurveyQuestion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpHrSurveyQuestion")
public class ErpHrSurveyQuestionBizModel extends CrudBizModel<ErpHrSurveyQuestion> implements IErpHrSurveyQuestionBiz{
    public ErpHrSurveyQuestionBizModel(){
        setEntityName(ErpHrSurveyQuestion.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpHrSurveyQuestion.class)
    public List<String> surveyTitle(@ContextSource List<ErpHrSurveyQuestion> rows) {
        orm().batchLoadProps(rows, Collections.singleton("survey"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrSurveyQuestion row : rows) {
            result.add(row.orm_attached() && row.getSurvey() != null ? row.getSurvey().getTitle() : null);
        }
        return result;
    }
}
