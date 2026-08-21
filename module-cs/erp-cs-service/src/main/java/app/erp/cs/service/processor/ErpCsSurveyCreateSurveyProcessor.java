package app.erp.cs.service.processor;

import app.erp.cs.dao.entity.ErpCsSurvey;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.ErpCsErrors;
import app.erp.cs.service.entity.SurveyTokenGenerator;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpCsSurvey createSurvey per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含满意度调查创建编排（唯一约束校验 + token 生成 + 延迟发送模式 + 工单存在性校验）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCsSurveyCreateSurveyProcessor {

    @Inject
    IDaoProvider daoProvider;

    public ErpCsSurvey createSurvey(String ticketId, IServiceContext context) {
        if (!ErpCsConfigs.isSurveyEnabled()) {
            return null;
        }
        if (ticketId == null) {
            throw new NopException(ErpCsErrors.ERR_TICKET_NOT_FOUND).param(ErpCsErrors.ARG_TICKET_ID, ticketId);
        }
        // 唯一约束：一工单一调查
        ErpCsSurvey existing = findSurveyByTicket(ticketId);
        if (existing != null) {
            throw new NopException(ErpCsErrors.ERR_SURVEY_ALREADY_EXISTS).param(ErpCsErrors.ARG_TICKET_ID, ticketId);
        }

        ErpCsSurvey survey = dao().newEntity();
        survey.setTicketId(ticketId);
        survey.setSurveyToken(SurveyTokenGenerator.generate());
        survey.setSurveyChannel(ErpCsConstants.SURVEY_CHANNEL_PORTAL);
        int delayHours = ErpCsConfigs.getSurveySendDelayHours();
        // delay=0 立即发送（surveySentAt=now，状态 SENT）；delay>0 留空（状态 PENDING，待 nop-job 延迟发送）
        survey.setSurveySentAt(delayHours <= 0 ? CoreMetrics.currentTimestamp() : null);
        survey.setStatus(delayHours <= 0 ? ErpCsConstants.SURVEY_STATUS_SENT : ErpCsConstants.SURVEY_STATUS_PENDING);
        dao().saveEntity(survey);
        // 经 ORM to-one 关系 {@code ErpCsSurvey.ticket} 透明懒加载校验工单存在（避免 daoFor 跨聚合访问 +
        // 避免与 ErpCsTicketBizModel 循环依赖）。saveEntity 后实体已入 session，getTicket() 触发懒加载；
        // 若工单不存在则事务回滚撤销本次保存。
        ErpCsTicket ticket = survey.getTicket();
        if (ticket == null) {
            throw new NopException(ErpCsErrors.ERR_TICKET_NOT_FOUND).param(ErpCsErrors.ARG_TICKET_ID, ticketId);
        }
        return survey;
    }

    private ErpCsSurvey findSurveyByTicket(String ticketId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        q.setLimit(1);
        List<ErpCsSurvey> list = dao().findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private IEntityDao<ErpCsSurvey> dao() {
        return daoProvider.daoFor(ErpCsSurvey.class);
    }
}
