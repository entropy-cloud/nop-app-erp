
package app.erp.hr.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.hr.dao.entity.ErpHrSurvey;

public interface IErpHrSurveyBiz extends ICrudBiz<ErpHrSurvey>{

    /**
     * 发布问卷：DRAFT→OPEN。校验题目非空 + 起止日期完整（UC-HR-11 基本流程 5，P1-MA2-041）。
     */
    @BizMutation
    ErpHrSurvey publish(@Name("surveyId") Long surveyId, IServiceContext context);

    /**
     * 截止问卷：OPEN→CLOSED，触发 ErpHrSurveyResult 自动聚合（UC-HR-11 基本流程 8，P1-MA2-041 + P1-RC-016）。
     */
    @BizMutation
    ErpHrSurvey close(@Name("surveyId") Long surveyId, IServiceContext context);

    /**
     * 归档问卷：CLOSED→ARCHIVED（UC-HR-11，P1-MA2-041）。
     */
    @BizMutation
    ErpHrSurvey archive(@Name("surveyId") Long surveyId, IServiceContext context);
}
