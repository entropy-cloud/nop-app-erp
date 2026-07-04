
package app.erp.cs.service.entity;

import app.erp.cs.biz.IErpCsSurveyBiz;
import app.erp.cs.dao.entity.ErpCsSurvey;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.ErpCsErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import java.time.LocalDateTime;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.isNull;
import static io.nop.api.core.beans.FilterBeans.lt;

/**
 * 满意度调查 BizModel。权威：{@code docs/design/customer-service/csat.md}、
 * {@code docs/plans/2026-07-04-0700-2-cs-ticket-sla-csat.md} Phase 2。
 *
 * <p>调查生命周期（状态由时间戳派生，无独立 status 列——见 plan Decision）：
 * <ul>
 *   <li>PENDING：surveySentAt 空（延迟发送模式，delay&gt;0 时创建后未发送）</li>
 *   <li>SENT：surveySentAt 非空 且 respondedAt 空</li>
 *   <li>COMPLETED：respondedAt 非空</li>
 * </ul>
 *
 * <p>NPS 分类（PROMOTER/PASSIVE/DETRACTOR）经 {@link NpsClassifier} 派生，不持久化（ORM 无分类列）。
 */
@BizModel("ErpCsSurvey")
public class ErpCsSurveyBizModel extends CrudBizModel<ErpCsSurvey> implements IErpCsSurveyBiz {

    public ErpCsSurveyBizModel() {
        setEntityName(ErpCsSurvey.class.getName());
    }

    @Override
    @BizMutation
    public ErpCsSurvey createSurvey(@Name("ticketId") Long ticketId, IServiceContext context) {
        if (!ErpCsConfigs.isSurveyEnabled()) {
            return null;
        }
        if (ticketId == null) {
            throw new NopException(ErpCsErrors.ERR_TICKET_NOT_FOUND).param(ErpCsErrors.ARG_TICKET_ID, ticketId);
        }
        // 校验工单存在（经 daoProvider 只读校验，避免与 ErpCsTicketBizModel 形成循环依赖）
        ErpCsTicket ticket = daoProvider().daoFor(ErpCsTicket.class).getEntityById(ticketId);
        if (ticket == null) {
            throw new NopException(ErpCsErrors.ERR_TICKET_NOT_FOUND).param(ErpCsErrors.ARG_TICKET_ID, ticketId);
        }
        // 唯一约束：一工单一调查
        ErpCsSurvey existing = findSurveyByTicket(ticketId);
        if (existing != null) {
            throw new NopException(ErpCsErrors.ERR_SURVEY_ALREADY_EXISTS).param(ErpCsErrors.ARG_TICKET_ID, ticketId);
        }

        ErpCsSurvey survey = newEntity();
        survey.setTicketId(ticket.getId());
        survey.setSurveyToken(SurveyTokenGenerator.generate());
        survey.setSurveyChannel(ErpCsConstants.SURVEY_CHANNEL_PORTAL);
        int delayHours = ErpCsConfigs.getSurveySendDelayHours();
        // delay=0 立即发送（surveySentAt=now，状态 SENT）；delay>0 留空（状态 PENDING，待 nop-job 延迟发送）
        survey.setSurveySentAt(delayHours <= 0 ? LocalDateTime.now() : null);
        saveEntity(survey, null, context);
        return survey;
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
        ErpCsSurvey survey = findSurveyByToken(surveyToken);
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
        survey.setRespondedAt(LocalDateTime.now());
        // NPS 分类（派生，不持久化——ORM 无分类列）
        dao().updateEntity(survey);
        return survey;
    }

    @Override
    @BizQuery
    public List<ErpCsSurvey> findSurveyReminders(@Optional @Name("reminderHours") Integer reminderHours,
                                                  IServiceContext context) {
        int hours = reminderHours != null ? reminderHours : ErpCsConfigs.getSurveyReminderHours();
        LocalDateTime threshold = LocalDateTime.now().minusHours(hours);
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
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        QueryBean q = new QueryBean();
        q.addFilter(isNull("respondedAt"));
        q.addFilter(lt("surveySentAt", threshold));
        return findList(q, null, context);
    }

    // ---------- helpers ----------

    private ErpCsSurvey findSurveyByTicket(Long ticketId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        q.setLimit(1);
        List<ErpCsSurvey> list = dao().findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpCsSurvey findSurveyByToken(String token) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("surveyToken", token));
        q.setLimit(1);
        List<ErpCsSurvey> list = dao().findAllByQuery(q);
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
