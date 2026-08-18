package app.erp.cs.service.job;

import app.erp.cs.dao.entity.ErpCsSurvey;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.isNull;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 满意度调查延迟发送链 Job Bean（RC-R1.70，P1-RC-059，UC-CS-08 ①② + 后置 + 异常）。
 *
 * <p>R1.37 简单 job 范式：由 nop-job-local 的 scheduler.yaml 经 BeanMethodJobInvoker 反射调用
 * {@link #execute()}；双层门控 = job.yaml 调度级 enabled/cron-expr + bean 内
 * {@code erp-cs.survey-send-cron} 空值跳过（「不调度」语义）。
 *
 * <p>扫描单一队列（{@code erp-cs.survey-send-batch-limit} 单批上限，逐条 try/catch 失败隔离，
 * {@code ormTemplate.runInSession} 包裹保持实体 MANAGED 原位修正）：
 * <b>未发送未响应且到期</b>（surveySentAt 空 + respondedAt 空 + createTime + delay 小时已到）——
 * 覆盖 status=PENDING、遗留 status=null 且 surveySentAt=null（派生 PENDING）与 FAILED 未超限重试三类行；
 * 终态 FAILED（failureCount &gt;= {@code erp-cs.survey-send-retry-max}）Java 侧跳过保留可查询。
 * 扫描走 dao 直查（isNull 算子不经 CrudBizModel 管道校验，见 {@link #findSendableSurveys}）。
 *
 * <p>派发语义：{@code notificationBiz.notify("cs.survey-invitation", ...)}（模板种子 7205，ROLE 客服员
 * 转达 + IN_APP 占位——客户非系统用户，EMAIL/SMS 实际通道投递归 nop-notification 独立面 successor）；
 * 成功判据 = notify 调用无异常（占位语义下落库即成功）。工单/客户读取经 ORM to-one 关系懒加载
 * （{@code survey.ticket} / {@code ticket.customer}）。
 */
public class ErpCsSurveySendJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpCsSurveySendJob.class);

    @Inject
    IErpSysNotificationBiz notificationBiz;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时扫描到期 PENDING + 可重试 FAILED 调查并逐条派发。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-cs-survey-send-skipped: cron config empty (erp-cs.survey-send-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        try {
            int sent = runSend(ctx);
            LOG.info("erp-cs-survey-send-done: sent={}", sent);
        } catch (Exception e) {
            LOG.error("erp-cs-survey-send-failed", e);
        }
    }

    /**
     * 扫描 + 派发；返回成功转 SENT 的调查条数。
     * 扫描与修正同一 ORM session 内完成（实体保持 MANAGED 供状态原位修正，session 提交落库）。
     */
    protected int runSend(IServiceContext ctx) {
        return ormTemplate.runInSession(session -> dispatchAll(findSendableSurveys(ctx), ctx));
    }

    /**
     * 可发送扫描：未发送（surveySentAt 空——PENDING/遗留派生 PENDING/FAILED 未成功行）+ 未响应
     * （respondedAt 空，派生 COMPLETED 优先）+ createTime+delay 已到期（FAILED 重试行本就已过延迟窗）。
     *
     * <p>daoFor 原因：isNull 过滤算子经 CrudBizModel 管道会被 ObjMetaBasedFilterValidator 拒绝
     * （xmeta 默认仅 eq/in/dateBetween/dateTimeBetween）；job 内部系统扫描非公共查询契约，
     * 走 dao 直查（同域聚合 + {@code ErpCsCsatReminderJob.loadTicket} job 只读先例同型）。
     */
    protected List<ErpCsSurvey> findSendableSurveys(IServiceContext ctx) {
        LocalDateTime threshold = CoreMetrics.currentDateTime()
                .minusHours(ErpCsConfigs.getSurveySendDelayHours());
        QueryBean q = new QueryBean();
        q.addFilter(isNull("surveySentAt"));
        q.addFilter(isNull("respondedAt"));
        q.addFilter(le("createTime", java.sql.Timestamp.valueOf(threshold)));
        q.addOrderField("createTime", false);
        q.setLimit(ErpCsConfigs.getSurveySendBatchLimit());
        return dao().findAllByQuery(q);
    }

    /** 逐条派发列表内调查（单条失败隔离：异常 → status=FAILED + failureCount++，不阻断后续）。 */
    protected int dispatchAll(List<ErpCsSurvey> surveys, IServiceContext ctx) {
        if (surveys == null || surveys.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (ErpCsSurvey survey : surveys) {
            if (isTerminalFailed(survey)) {
                continue;
            }
            try {
                dispatchSurvey(survey, ctx);
                count++;
            } catch (Exception e) {
                markFailed(survey, e);
            }
        }
        return count;
    }

    /** 终态判定：status=FAILED 且 failureCount 已达 retry-max（终态 FAILED 保留可查询，不再重试）。 */
    protected boolean isTerminalFailed(ErpCsSurvey survey) {
        return ErpCsConstants.SURVEY_STATUS_FAILED.equals(survey.getStatus())
                && survey.getFailureCount() != null
                && survey.getFailureCount() >= ErpCsConfigs.getSurveySendRetryMax();
    }

    /**
     * 派发单条调查邀请并转 SENT。成功判据 = notify 调用无异常（IN_APP 占位语义下落库即成功）。
     * protected 以允许测试覆盖（如模拟 notify 异常驱动 FAILED 路径）。
     */
    protected void dispatchSurvey(ErpCsSurvey survey, IServiceContext ctx) {
        notificationBiz.notify(ErpCsConstants.NOTIFY_EVENT_SURVEY_INVITATION, buildNotifyContext(survey), ctx);
        survey.setStatus(ErpCsConstants.SURVEY_STATUS_SENT);
        survey.setSurveySentAt(CoreMetrics.currentTimestamp());
    }

    /** 失败标记：status=FAILED + failureCount++（null 起算 1）；超限终态由扫描条件自然排除。 */
    protected void markFailed(ErpCsSurvey survey, Exception cause) {
        survey.setStatus(ErpCsConstants.SURVEY_STATUS_FAILED);
        survey.setFailureCount(survey.getFailureCount() == null ? 1 : survey.getFailureCount() + 1);
        LOG.warn("erp-cs-survey-send: 单条调查派发失败（标记 FAILED，隔离继续）：surveyId={}, failureCount={}, reason={}",
                survey.getId(), survey.getFailureCount(), cause.getMessage());
    }

    /** notify 上下文：{surveyId, surveyToken, ticketCode, channel, customerName}（渠道携带于模板渲染）。 */
    protected Map<String, Object> buildNotifyContext(ErpCsSurvey survey) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("surveyId", survey.getId());
        map.put("surveyToken", survey.getSurveyToken());
        map.put("channel", survey.getSurveyChannel());
        ErpCsTicket ticket = loadTicket(survey);
        map.put("ticketCode", ticket == null ? null : ticket.getCode());
        map.put("customerName", ticket == null ? null : resolveCustomerName(ticket));
        return map;
    }

    /** 经 ORM to-one 关系 {@code survey.ticket} 懒加载（同域聚合关系 getter，避免 daoFor 直查）。 */
    private ErpCsTicket loadTicket(ErpCsSurvey survey) {
        try {
            return survey.getTicket();
        } catch (Exception e) {
            return null;
        }
    }

    /** 经 ORM to-one 关系 {@code ticket.customer} 懒加载客户名（客户缺失降级 null）。 */
    private String resolveCustomerName(ErpCsTicket ticket) {
        try {
            return ticket.getCustomer() == null ? null : ticket.getCustomer().getName();
        } catch (Exception e) {
            return null;
        }
    }

    private IEntityDao<ErpCsSurvey> dao() {
        return daoProvider.daoFor(ErpCsSurvey.class);
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpCsConstants.CONFIG_SURVEY_SEND_CRON, "");
    }
}
