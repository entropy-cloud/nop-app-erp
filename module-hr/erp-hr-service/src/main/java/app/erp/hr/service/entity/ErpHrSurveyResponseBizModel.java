
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.convert.ConvertHelper;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.commons.crypto.HashHelper;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;

import app.erp.hr.biz.IErpHrSurveyBiz;
import app.erp.hr.biz.IErpHrSurveyQuestionBiz;
import app.erp.hr.biz.IErpHrSurveyResponseBiz;
import app.erp.hr.dao.entity.ErpHrSurvey;
import app.erp.hr.dao.entity.ErpHrSurveyAnswer;
import app.erp.hr.dao.entity.ErpHrSurveyQuestion;
import app.erp.hr.dao.entity.ErpHrSurveyResponse;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import jakarta.inject.Inject;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 答卷 BizModel（UC-HR-11 基本流程 6-7，RC-R1.9 P1-RC-016）。
 *
 * <p>submitResponse 支持匿名/非匿名双路径：匿名模式（isAnonymous=true）下 employeeId 不落库，
 * 仅写 respondentHash = SHA-256(employeeId + ":" + surveyId)（稳定可复算，见 Plan Decision），
 * 同人重复提交经 (surveyId, respondentHash) / (surveyId, employeeId) 查询校验拦截（无 DB UK，
 * check-then-insert 单请求并发窗口与 A4.2.144 TOCTOU 同型 watch-only，见 Deferred But Adjudicated）。
 */
@BizModel("ErpHrSurveyResponse")
public class ErpHrSurveyResponseBizModel extends CrudBizModel<ErpHrSurveyResponse> implements IErpHrSurveyResponseBiz {

    @Inject
    IErpHrSurveyBiz surveyBiz;

    @Inject
    IErpHrSurveyQuestionBiz surveyQuestionBiz;

    public ErpHrSurveyResponseBizModel() {
        setEntityName(ErpHrSurveyResponse.class.getName());
    }

    @Override
    @BizMutation
    public ErpHrSurveyResponse submitResponse(@Name("surveyId") Long surveyId,
                                              @Name("employeeId") Long employeeId,
                                              @Name("answers") List<Map<String, Object>> answers,
                                              IServiceContext context) {
        ErpHrSurvey survey = surveyBiz.requireEntity(String.valueOf(surveyId), null, context);
        if (!Objects.equals(survey.getStatus(), ErpHrConstants.SURVEY_STATUS_OPEN)) {
            throw new NopException(ErpHrErrors.ERR_HR_SURVEY_NOT_OPEN)
                    .param(ErpHrErrors.ARG_SURVEY_ID, surveyId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, survey.getStatus());
        }
        boolean anonymous = Boolean.TRUE.equals(survey.getIsAnonymous());
        String respondentHash = anonymous ? respondentHashOf(employeeId, surveyId) : null;

        QueryBean dup = new QueryBean();
        dup.addFilter(eq("surveyId", surveyId));
        dup.addFilter(anonymous ? eq("respondentHash", respondentHash) : eq("employeeId", employeeId));
        dup.setLimit(1);
        if (!findList(dup, null, context).isEmpty()) {
            throw new NopException(ErpHrErrors.ERR_HR_SURVEY_ALREADY_SUBMITTED)
                    .param(ErpHrErrors.ARG_SURVEY_ID, surveyId);
        }

        Set<Long> questionIds = loadSurveyQuestionIds(surveyId, context);

        ErpHrSurveyResponse response = newEntity();
        response.setSurveyId(surveyId);
        response.setSubmittedAt(CoreMetrics.currentTimestamp());
        response.setIsComplete(true);
        response.setOrgId(survey.getOrgId());
        if (anonymous) {
            response.setRespondentHash(respondentHash);
        } else {
            response.setEmployeeId(employeeId);
        }
        if (answers != null) {
            for (Map<String, Object> ans : answers) {
                if (ans == null) {
                    continue;
                }
                Long questionId = ConvertHelper.toLong(ans.get("questionId"), null);
                if (questionId == null || !questionIds.contains(questionId)) {
                    throw new NopException(ErpHrErrors.ERR_HR_SURVEY_INVALID_QUESTION)
                            .param(ErpHrErrors.ARG_SURVEY_ID, surveyId)
                            .param(ErpHrErrors.ARG_QUESTION_ID, questionId);
                }
                ErpHrSurveyAnswer answer = (ErpHrSurveyAnswer) orm().newEntity(ErpHrSurveyAnswer.class.getName());
                answer.setQuestionId(questionId);
                answer.setRatingValue(ConvertHelper.toInteger(ans.get("ratingValue"), null));
                answer.setSelectedOption(StringHelper.toString(ans.get("selectedOption"), null));
                answer.setOpenText(StringHelper.toString(ans.get("openText"), null));
                response.getAnswers().add(answer);
            }
        }
        saveEntity(response, null, context);

        int current = survey.getTotalResponses() == null ? 0 : survey.getTotalResponses();
        survey.setTotalResponses(current + 1);
        surveyBiz.updateEntity(survey, null, context);
        return response;
    }

    private Set<Long> loadSurveyQuestionIds(Long surveyId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("surveyId", surveyId));
        List<ErpHrSurveyQuestion> questions = surveyQuestionBiz.findList(q, null, context);
        Set<Long> ids = new HashSet<>();
        for (ErpHrSurveyQuestion question : questions) {
            ids.add(question.getId());
        }
        return ids;
    }

    /** 稳定可复算的匿名应答者哈希：SHA-256(employeeId + ":" + surveyId) 十六进制（Plan Decision 选项 A）。 */
    private static String respondentHashOf(Long employeeId, Long surveyId) {
        String input = employeeId + ":" + surveyId;
        byte[] hash = HashHelper.sha256(input.getBytes(StandardCharsets.UTF_8), null);
        return StringHelper.bytesToHex(hash);
    }
}
