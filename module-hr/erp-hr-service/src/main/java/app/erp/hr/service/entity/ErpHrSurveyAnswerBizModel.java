
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrSurveyAnswerBiz;
import app.erp.hr.dao.entity.ErpHrSurveyAnswer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpHrSurveyAnswer")
public class ErpHrSurveyAnswerBizModel extends CrudBizModel<ErpHrSurveyAnswer> implements IErpHrSurveyAnswerBiz{
    public ErpHrSurveyAnswerBizModel(){
        setEntityName(ErpHrSurveyAnswer.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpHrSurveyAnswer.class)
    public List<String> questionText(@ContextSource List<ErpHrSurveyAnswer> rows) {
        orm().batchLoadProps(rows, Collections.singleton("question"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrSurveyAnswer row : rows) {
            result.add(row.orm_attached() && row.getQuestion() != null ? row.getQuestion().getQuestionText() : null);
        }
        return result;
    }
}
