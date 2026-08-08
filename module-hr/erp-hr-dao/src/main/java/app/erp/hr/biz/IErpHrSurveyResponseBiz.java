
package app.erp.hr.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.hr.dao.entity.ErpHrSurveyResponse;

import java.util.List;
import java.util.Map;

public interface IErpHrSurveyResponseBiz extends ICrudBiz<ErpHrSurveyResponse>{

    /**
     * 员工提交答卷（UC-HR-11 基本流程 6-7，RC-R1.9 P1-RC-016）：
     * 仅 OPEN 问卷可提交；匿名模式（isAnonymous=true）下 employeeId 不落库，
     * 仅写 respondentHash = SHA-256(employeeId + ":" + surveyId) 防重复；
     * 非匿名模式存 employeeId；同人重复提交（匿名按 respondentHash / 非匿名按 employeeId）被拦截。
     * answers 为回答明细列表，每项含 questionId + ratingValue/selectedOption/openText。
     */
    @BizMutation
    ErpHrSurveyResponse submitResponse(@Name("surveyId") Long surveyId,
                                       @Name("employeeId") Long employeeId,
                                       @Name("answers") List<Map<String, Object>> answers,
                                       IServiceContext context);
}
