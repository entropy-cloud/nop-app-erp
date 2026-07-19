package app.erp.notify.service.entity;

import app.erp.notify.biz.IErpSysNotificationBiz;
import app.erp.notify.biz.IErpSysNotificationTemplateBiz;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationRead;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import app.erp.notify.service.ErpNotifyConstants;
import app.erp.notify.service.dispatch.NotificationDispatcher;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.notIn;

/**
 * 通知实例 BizModel。薄委派层：{@link #notify} 委派给 {@link NotificationDispatcher}（模板渲染→接收人解析→频控合并→站内落库→外发通道），
 * 其余 {@link #markRead}/{@link #markAllRead}/{@link #findUnread}/{@link #countUnread} 围绕 ErpSysNotificationRead 维护已读状态。
 *
 * <p>权威：`docs/architecture/notification-strategy.md`、
 * `docs/plans/2026-07-06-0504-1-notification-dispatch-subsystem.md`。
 */
@BizModel("ErpSysNotification")
public class ErpSysNotificationBizModel extends CrudBizModel<ErpSysNotification> implements IErpSysNotificationBiz {
    private static final Logger LOG = LoggerFactory.getLogger(ErpSysNotificationBizModel.class);

    @Inject
    NotificationDispatcher dispatcher;
    @Inject
    IErpSysNotificationTemplateBiz templateBiz;

    public ErpSysNotificationBizModel() {
        setEntityName(ErpSysNotification.class.getName());
    }

    public void setDispatcher(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void setTemplateBiz(IErpSysNotificationTemplateBiz templateBiz) {
        this.templateBiz = templateBiz;
    }

    @Override
    @BizMutation
    public List<ErpSysNotification> notify(@Name("eventType") String eventType,
                                           @Name("context") java.util.Map<String, Object> context,
                                           IServiceContext ctx) {
        try {
            ErpSysNotificationTemplate template = findActiveTemplate(eventType, ctx);
            if (template == null) {
                LOG.warn("notify: 业务事件[{}]无 ACTIVE 模板，config-gated 静默跳过", eventType);
                return Collections.emptyList();
            }
            List<ErpSysNotification> result = dispatcher.dispatch(template, context);
            IEntityDao<ErpSysNotification> dao = daoProvider().daoFor(ErpSysNotification.class);
            for (ErpSysNotification n : result) {
                if (n.getId() == null) {
                    dao.saveEntity(n);
                } else {
                    dao.updateEntity(n);
                }
            }
            return result;
        } catch (Exception e) {
            // 通知是 best-effort 关注点：任何失败（模板缺失/渲染失败/接收人解析失败/落库失败）
            // 均不阻断调用方业务事实（与 notification-strategy.md config-gated 语义一致）。
            LOG.error("notify: 业务事件[{}]通知派发失败（不阻断调用方）: {}", eventType, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @BizMutation
    public ErpSysNotification markRead(@Name("notificationId") Long notificationId, IServiceContext ctx) {
        ErpSysNotification n = requireEntity(String.valueOf(notificationId), null, ctx);
        // 通知按 recipientUserId 投递，已读记录以接收人为准（与 findUnread/countUnread 口径一致）；
        // recipientUserId 缺失时回退当前登录用户（与 resolveUserId 模式一致）
        String userId = n.getRecipientUserId();
        if (userId == null) {
            userId = ctx.getUserId();
        }
        IEntityDao<ErpSysNotificationRead> readDao = daoProvider().daoFor(ErpSysNotificationRead.class);
        if (!isRead(notificationId, userId, readDao)) {
            ErpSysNotificationRead read = readDao.newEntity();
            read.setNotificationId(notificationId);
            read.setUserId(userId);
            read.setReadTime(CoreMetrics.currentTimestamp());
            readDao.saveEntity(read);
        }
        return n;
    }

    @Override
    @BizMutation
    public int markAllRead(@Optional @Name("userId") String userId, IServiceContext ctx) {
        List<ErpSysNotification> unread = unreadOf(resolveUserId(userId, ctx));
        IEntityDao<ErpSysNotificationRead> readDao = daoProvider().daoFor(ErpSysNotificationRead.class);
        int count = 0;
        for (ErpSysNotification n : unread) {
            if (!isRead(n.getId(), resolveUserId(userId, ctx), readDao)) {
                ErpSysNotificationRead read = readDao.newEntity();
                read.setNotificationId(n.getId());
                read.setUserId(resolveUserId(userId, ctx));
                read.setReadTime(CoreMetrics.currentTimestamp());
                readDao.saveEntity(read);
                count++;
            }
        }
        return count;
    }

    @Override
    @BizQuery
    public List<ErpSysNotification> findUnread(@Optional @Name("userId") String userId, IServiceContext ctx) {
        return unreadOf(resolveUserId(userId, ctx));
    }

    @Override
    @BizQuery
    public List<ErpSysNotification> findRead(@Optional @Name("userId") String userId, IServiceContext ctx) {
        String resolved = resolveUserId(userId, ctx);
        if (resolved == null || resolved.isEmpty()) {
            return Collections.emptyList();
        }
        // 已读 = 该用户的 ErpSysNotificationRead 记录关联回 ErpSysNotification（保持与 findUnread 对称）
        IEntityDao<ErpSysNotificationRead> readDao = daoProvider().daoFor(ErpSysNotificationRead.class);
        QueryBean readQ = new QueryBean();
        readQ.addFilter(eq("userId", resolved));
        List<ErpSysNotificationRead> reads = readDao.findAllByQuery(readQ);
        if (reads.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> readIds = new HashSet<>();
        for (ErpSysNotificationRead r : reads) {
            if (r.getNotificationId() != null) readIds.add(r.getNotificationId());
        }
        IEntityDao<ErpSysNotification> dao = daoProvider().daoFor(ErpSysNotification.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("id", new ArrayList<>(readIds)));
        q.addOrderField("sentAt", true);
        return dao.findAllByQuery(q);
    }

    @Override
    @BizQuery
    public long countUnread(@Optional @Name("userId") String userId, IServiceContext ctx) {
        return unreadOf(resolveUserId(userId, ctx)).size();
    }

    // ---------- helpers ----------

    /**
     * 解析收件箱操作的 userId：显式传入优先（向后兼容 seed/测试/跨用户场景），否则回退到当前登录用户。
     * 与 {@link #markRead} 内部 「优先 recipientUserId，回退 ctx.getUserId()」 模式一致——
     * 前端 AMIS 收件箱页面（inbox.page.yaml）不传 userId 时自动落到当前登录用户。
     */
    private String resolveUserId(String userId, IServiceContext ctx) {
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }
        return ctx == null ? null : ctx.getUserId();
    }

    private ErpSysNotificationTemplate findActiveTemplate(String eventType, IServiceContext ctx) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", eventType));
        q.addFilter(eq("status", ErpNotifyConstants.TEMPLATE_ACTIVE));
        q.setLimit(1);
        return templateBiz.findFirst(q, null, ctx);
    }

    private List<ErpSysNotification> unreadOf(String userId) {
        if (userId == null || userId.isEmpty()) {
            return Collections.emptyList();
        }
        IEntityDao<ErpSysNotification> dao = daoProvider().daoFor(ErpSysNotification.class);
        // 该用户已读的 notificationId 集合
        QueryBean readQ = new QueryBean();
        readQ.addFilter(eq("userId", userId));
        List<ErpSysNotificationRead> reads = daoProvider().daoFor(ErpSysNotificationRead.class).findAllByQuery(readQ);
        Set<Long> readIds = new HashSet<>();
        for (ErpSysNotificationRead r : reads) {
            if (r.getNotificationId() != null) readIds.add(r.getNotificationId());
        }

        QueryBean q = new QueryBean();
        q.addFilter(eq("recipientUserId", userId));
        q.addFilter(in("status", List.of(ErpNotifyConstants.STATUS_SENT, ErpNotifyConstants.STATUS_MERGED)));
        if (!readIds.isEmpty()) {
            q.addFilter(notIn("id", new ArrayList<>(readIds)));
        }
        q.addOrderField("sentAt", true);
        return dao.findAllByQuery(q);
    }

    private boolean isRead(Long notificationId, String userId, IEntityDao<ErpSysNotificationRead> readDao) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationId", notificationId));
        q.addFilter(eq("userId", userId));
        q.setLimit(1);
        return !readDao.findAllByQuery(q).isEmpty();
    }
}
