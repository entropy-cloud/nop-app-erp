package app.erp.cs.service.entity;

import app.erp.cs.biz.IErpCsTicketTimerSessionBiz;
import app.erp.cs.dao.entity.ErpCsTicketTimerSession;
import app.erp.cs.service.processor.ErpCsTicketTimerSessionPauseTimerProcessor;
import app.erp.cs.service.processor.ErpCsTicketTimerSessionResumeTimerProcessor;
import app.erp.cs.service.processor.ErpCsTicketTimerSessionStartTimerProcessor;
import app.erp.cs.service.processor.ErpCsTicketTimerSessionStopTimerProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import static io.nop.api.core.beans.FilterBeans.eq;

import java.util.List;

/**
 * 工单计时器会话 BizModel（RC-R1.66，P1-RC-055，UC-CS-11 ②③④⑧⑨）。
 *
 * <p>四 mutation 经 per-mutation Processor 编排（单活跃计时器守卫 + 12h 惰性结算 +
 * 停止生成 ErpCsTimeEntry），findActiveTimer 读取时惰性结算（plan D3 等价实现）。
 * config {@code erp-cs.time-tracking-enabled=false} 拒绝全部计时器 mutation（plan D6）。
 */
@BizModel("ErpCsTicketTimerSession")
public class ErpCsTicketTimerSessionBizModel extends CrudBizModel<ErpCsTicketTimerSession>
        implements IErpCsTicketTimerSessionBiz {

    @Inject
    ErpCsTicketTimerSessionStartTimerProcessor startTimerProcessor;
    @Inject
    ErpCsTicketTimerSessionPauseTimerProcessor pauseTimerProcessor;
    @Inject
    ErpCsTicketTimerSessionResumeTimerProcessor resumeTimerProcessor;
    @Inject
    ErpCsTicketTimerSessionStopTimerProcessor stopTimerProcessor;
    @Inject
    app.erp.cs.service.processor.ErpCsTicketTimerSessionOps timerSessionOps;

    public ErpCsTicketTimerSessionBizModel() {
        setEntityName(ErpCsTicketTimerSession.class.getName());
    }

    @Override
    @BizMutation
    public ErpCsTicketTimerSession startTimer(@Name("ticketId") Long ticketId, IServiceContext context) {
        return startTimerProcessor.startTimer(ticketId, context);
    }

    @Override
    @BizMutation
    public ErpCsTicketTimerSession pauseTimer(@Name("sessionId") Long sessionId,
                                              @Optional @Name("pauseReason") String pauseReason,
                                              IServiceContext context) {
        return pauseTimerProcessor.pauseTimer(sessionId, pauseReason, context);
    }

    @Override
    @BizMutation
    public ErpCsTicketTimerSession resumeTimer(@Name("sessionId") Long sessionId, IServiceContext context) {
        return resumeTimerProcessor.resumeTimer(sessionId, context);
    }

    @Override
    @BizMutation
    public ErpCsTicketTimerSession stopTimer(@Name("sessionId") Long sessionId, IServiceContext context) {
        return stopTimerProcessor.stopTimer(sessionId, context);
    }

    @Override
    @BizQuery
    public ErpCsTicketTimerSession findActiveTimer(@Optional @Name("agentId") String agentId,
                                                   IServiceContext context) {
        String agent = agentId != null && !agentId.trim().isEmpty() ? agentId.trim() : context.getUserId();
        io.nop.api.core.beans.query.QueryBean query = new io.nop.api.core.beans.query.QueryBean();
        query.addFilter(eq("agentId", agent));
        query.addFilter(eq("activeFlag", app.erp.cs.service.ErpCsConstants.TIMER_SESSION_ACTIVE_FLAG));
        query.setLimit(1);
        List<ErpCsTicketTimerSession> list = findList(query, null, context);
        if (list.isEmpty()) {
            return null;
        }
        ErpCsTicketTimerSession session = list.get(0);
        // 读取入口惰性结算（plan D3，REQUIRES_NEW 物化——@BizQuery 只读会话不影响结算提交）：
        // 超 12h 会话封顶停止后不再有活跃计时器
        return timerSessionOps.settleIfOverdueInNewTx(session.getId(), context) ? null : session;
    }
}
