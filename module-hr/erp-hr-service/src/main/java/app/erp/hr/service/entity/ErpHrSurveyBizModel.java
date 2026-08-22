
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.convert.ConvertHelper;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

import app.erp.hr.biz.IErpHrSurveyBiz;
import app.erp.hr.biz.IErpHrSurveyQuestionBiz;
import app.erp.hr.biz.IErpHrSurveyResultBiz;
import app.erp.hr.dao.entity.ErpHrSurvey;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 问卷模板 BizModel（UC-HR-11，RC-R1.9 P1-MA2-041 + P1-RC-016）。
 *
 * <p>状态机：DRAFT→publish→OPEN→close→CLOSED（触发聚合）→archive→ARCHIVED；
 * publish 后问卷配置字段与题目经 defaultPrepareUpdate 守卫禁止修改（不可编辑守卫，版本化归 successor）。
 */
@BizModel("ErpHrSurvey")
public class ErpHrSurveyBizModel extends CrudBizModel<ErpHrSurvey> implements IErpHrSurveyBiz {

    @Inject
    IErpHrSurveyQuestionBiz surveyQuestionBiz;

    @Inject
    IErpHrSurveyResultBiz surveyResultBiz;

    public ErpHrSurveyBizModel() {
        setEntityName(ErpHrSurvey.class.getName());
    }

    @Override
    @BizMutation
    public ErpHrSurvey publish(@Name("surveyId") String surveyId, IServiceContext context) {
        ErpHrSurvey survey = requireEntity(surveyId, null, context);
        requireTransition(surveyId, survey.getStatus(), ErpHrConstants.SURVEY_STATUS_DRAFT, context);
        if (survey.getStartDate() == null || survey.getEndDate() == null) {
            throw new NopException(ErpHrErrors.ERR_HR_SURVEY_ILLEGAL_TRANSITION)
                    .param(ErpHrErrors.ARG_SURVEY_ID, surveyId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, survey.getStatus())
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, "startDate/endDate");
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("surveyId", surveyId));
        if (surveyQuestionBiz.findCount(q, context) == 0) {
            throw new NopException(ErpHrErrors.ERR_HR_SURVEY_ILLEGAL_TRANSITION)
                    .param(ErpHrErrors.ARG_SURVEY_ID, surveyId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, survey.getStatus())
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, "questions-not-empty");
        }
        survey.setStatus(ErpHrConstants.SURVEY_STATUS_OPEN);
        updateEntity(survey, null, context);
        return survey;
    }

    @Override
    @BizMutation
    public ErpHrSurvey close(@Name("surveyId") String surveyId, IServiceContext context) {
        ErpHrSurvey survey = requireEntity(surveyId, null, context);
        requireTransition(surveyId, survey.getStatus(), ErpHrConstants.SURVEY_STATUS_OPEN, context);
        survey.setStatus(ErpHrConstants.SURVEY_STATUS_CLOSED);
        updateEntity(survey, null, context);
        surveyResultBiz.aggregateResult(surveyId, context);
        return survey;
    }

    @Override
    @BizMutation
    public ErpHrSurvey archive(@Name("surveyId") String surveyId, IServiceContext context) {
        ErpHrSurvey survey = requireEntity(surveyId, null, context);
        requireTransition(surveyId, survey.getStatus(), ErpHrConstants.SURVEY_STATUS_CLOSED, context);
        survey.setStatus(ErpHrConstants.SURVEY_STATUS_ARCHIVED);
        updateEntity(survey, null, context);
        return survey;
    }

    /**
     * publish 后编辑守卫（RC-R1.9 P1-MA2-041，Decision 选项 A）：问卷已非 DRAFT 时，
     * 拒绝问卷配置字段（title/description/surveyType/isAnonymous/startDate/endDate/targetDepartmentId/
     * includeENps/eNpsQuestion/reminderDays）及 questions 子表的修改——在既有默认逻辑之后追加，
     * 不破坏 CRUD 基线。已发布状态经 ORM 脏值追踪取"本次更新前"的持久化状态（客户端同请求改 status
     * 时也能拿到旧值），仅 DRAFT 问卷可编辑配置。
     */
    @Override
    protected void defaultPrepareUpdate(EntityData<ErpHrSurvey> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        Map<String, Object> data = entityData.getData();
        if (data == null || !touchesGuardedFields(data)) {
            return;
        }
        ErpHrSurvey entity = entityData.getEntity();
        String persistedStatus = entity.getStatus();
        if (entity.orm_propDirtyByName("status")) {
            Object oldStatus = entity.orm_dirtyOldValues().get("status");
            if (oldStatus != null) {
                persistedStatus = ConvertHelper.toString(oldStatus, "");
            }
        }
        if (!Objects.equals(persistedStatus, ErpHrConstants.SURVEY_STATUS_DRAFT)) {
            throw new NopException(ErpHrErrors.ERR_HR_SURVEY_PUBLISHED_IMMUTABLE)
                    .param(ErpHrErrors.ARG_SURVEY_ID, entity.getId())
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, persistedStatus);
        }
    }

    private void requireTransition(String surveyId, String currentStatus, String expectedStatus,
                                   IServiceContext context) {
        if (!Objects.equals(currentStatus, expectedStatus)) {
            throw new NopException(ErpHrErrors.ERR_HR_SURVEY_ILLEGAL_TRANSITION)
                    .param(ErpHrErrors.ARG_SURVEY_ID, surveyId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, currentStatus)
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, expectedStatus);
        }
    }

    private boolean touchesGuardedFields(Map<String, Object> data) {
        return data.containsKey("title")
                || data.containsKey("description")
                || data.containsKey("surveyType")
                || data.containsKey("isAnonymous")
                || data.containsKey("startDate")
                || data.containsKey("endDate")
                || data.containsKey("targetDepartmentId")
                || data.containsKey("includeENps")
                || data.containsKey("eNpsQuestion")
                || data.containsKey("reminderDays")
                || data.containsKey("questions");
    }
}
