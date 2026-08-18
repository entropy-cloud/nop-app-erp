
package app.erp.cs.service.entity;

import app.erp.cs.biz.IErpCsSurveyBiz;
import app.erp.cs.dao.entity.ErpCsSurvey;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.ErpCsErrors;
import app.erp.cs.service.processor.ErpCsSurveyCreateSurveyProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.isNull;
import static io.nop.api.core.beans.FilterBeans.lt;
import io.nop.api.core.time.CoreMetrics;

/**
 * 满意度调查 BizModel。权威：{@code docs/design/customer-service/csat.md}、
 * {@code docs/plans/2026-07-04-0700-2-cs-ticket-sla-csat.md} Phase 2。
 *
 * <p>调查生命周期（RC-R1.70 起持久化 {@code status} 列，dict {@code erp-cs/survey-status}；
 * 遗留行 status=null 走时间戳派生兼容——surveySentAt 空=PENDING / respondedAt 非空=COMPLETED / 否则 SENT）：
 * <ul>
 *   <li>PENDING：延迟发送模式（delay&gt;0 创建后未发送），由 ErpCsSurveySendJob 到期派发</li>
 *   <li>SENT：surveySentAt 非空 且 respondedAt 空</li>
 *   <li>COMPLETED：respondedAt 非空（submitSurvey 显式写）</li>
 *   <li>FAILED：派发异常标记，failureCount 计数，job 扫描重试至上限</li>
 * </ul>
 *
 * <p>NPS 分类（PROMOTER/PASSIVE/DETRACTOR）经 {@link NpsClassifier} 派生，不持久化（ORM 无分类列）。
 */
@BizModel("ErpCsSurvey")
public class ErpCsSurveyBizModel extends CrudBizModel<ErpCsSurvey> implements IErpCsSurveyBiz {

    public ErpCsSurveyBizModel() {
        setEntityName(ErpCsSurvey.class.getName());
    }

    @Inject
    ErpCsSurveyCreateSurveyProcessor createSurveyProcessor;

    @Override
    @BizMutation
    public ErpCsSurvey createSurvey(@Name("ticketId") Long ticketId, IServiceContext context) {
        return createSurveyProcessor.createSurvey(ticketId, context);
    }

    @Override
    @BizMutation
    public ErpCsSurvey submitSurvey(@Name("surveyToken") String surveyToken,
                                    @Optional @Name("csatScore") Integer csatScore,
                                    @Optional @Name("npsScore") Integer npsScore,
                                    @Optional @Name("cesScore") Integer cesScore,
                                    @Optional @Name("comment") String comment,
                                    IServiceContext context) {
        if (surveyToken == null || surveyToken.isEmpty()) {
            throw new NopException(ErpCsErrors.ERR_SURVEY_TOKEN_INVALID).param(ErpCsErrors.ARG_SURVEY_TOKEN, surveyToken);
        }
        ErpCsSurvey survey = findSurveyByToken(surveyToken, context);
        if (survey == null) {
            throw new NopException(ErpCsErrors.ERR_SURVEY_TOKEN_INVALID).param(ErpCsErrors.ARG_SURVEY_TOKEN, surveyToken);
        }
        if (survey.getRespondedAt() != null) {
            throw new NopException(ErpCsErrors.ERR_SURVEY_ALREADY_RESPONDED).param(ErpCsErrors.ARG_SURVEY_ID, survey.getId());
        }
        // 评分区间校验（各评分 config-gated，未启用的不校验）
        if (ErpCsConfigs.isSurveyCsatEnabled() && csatScore != null) {
            requireScoreRange("csatScore", csatScore, 1, 5);
        }
        if (ErpCsConfigs.isSurveyNpsEnabled() && npsScore != null) {
            requireScoreRange("npsScore", npsScore, 0, 10);
        }
        if (ErpCsConfigs.isSurveyCesEnabled() && cesScore != null) {
            requireScoreRange("cesScore", cesScore, 1, 7);
        }
        survey.setCsatScore(csatScore);
        survey.setNpsScore(npsScore);
        survey.setCesScore(cesScore);
        survey.setComment(comment);
        survey.setRespondedAt(CoreMetrics.currentTimestamp());
        survey.setStatus(ErpCsConstants.SURVEY_STATUS_COMPLETED);
        // NPS 分类（派生，不持久化——ORM 无分类列）
        updateEntity(survey, null, context);
        return survey;
    }

    @Override
    @BizQuery
    public List<ErpCsSurvey> findSurveyReminders(@Optional @Name("reminderHours") Integer reminderHours,
                                                  IServiceContext context) {
        int hours = reminderHours != null ? reminderHours : ErpCsConfigs.getSurveyReminderHours();
        LocalDateTime threshold = CoreMetrics.currentDateTime().minusHours(hours);
        QueryBean q = new QueryBean();
        // SENT：surveySentAt 非空 且 respondedAt 空 且 surveySentAt < now - reminderHours
        q.addFilter(isNull("respondedAt"));
        q.addFilter(lt("surveySentAt", threshold));
        return findList(q, null, context);
    }

    @Override
    @BizQuery
    public List<ErpCsSurvey> findExpiredSurveys(@Optional @Name("expireDays") Integer expireDays,
                                                 IServiceContext context) {
        int days = expireDays != null ? expireDays : ErpCsConfigs.getSurveyExpireDays();
        LocalDateTime threshold = CoreMetrics.currentDateTime().minusDays(days);
        QueryBean q = new QueryBean();
        q.addFilter(isNull("respondedAt"));
        q.addFilter(lt("surveySentAt", threshold));
        return findList(q, null, context);
    }

    // ---------- helpers ----------

    private ErpCsSurvey findSurveyByToken(String token, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("surveyToken", token));
        q.setLimit(1);
        List<ErpCsSurvey> list = findList(q, null, context);
        return list.isEmpty() ? null : list.get(0);
    }

    private void requireScoreRange(String field, int value, int min, int max) {
        if (value < min || value > max) {
            throw new NopException(ErpCsErrors.ERR_SURVEY_SCORE_OUT_OF_RANGE)
                    .param(ErpCsErrors.ARG_FIELD, field)
                    .param(ErpCsErrors.ARG_VALUE, value)
                    .param(ErpCsErrors.ARG_MIN, min)
                    .param(ErpCsErrors.ARG_MAX, max);
        }
    }

    

}
