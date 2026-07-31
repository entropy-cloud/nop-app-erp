package app.erp.notify.service.entity;

import app.erp.notify.biz.IErpSysNotificationBiz;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationRead;
import app.erp.notify.service.ErpNotifyConstants;
import app.erp.notify.service.processor.ErpSysNotificationMarkAllReadProcessor;
import app.erp.notify.service.processor.ErpSysNotificationMarkReadProcessor;
import app.erp.notify.service.processor.ErpSysNotificationNotifyProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 通知实例 BizModel。薄委派层：{@link #notify}/{@link #markRead}/{@link #markAllRead} 委派给 per-mutation Processor，
 * {@link #findUnread}/{@link #findRead}/{@link #countUnread} 围绕 ErpSysNotificationRead 维护已读状态查询。
 *
 * <p>权威：`docs/architecture/notification-strategy.md`、
 * `docs/plans/2026-07-06-0504-1-notification-dispatch-subsystem.md`。
 */
@BizModel("ErpSysNotification")
public class ErpSysNotificationBizModel extends CrudBizModel<ErpSysNotification> implements IErpSysNotificationBiz {

    @Inject
    ErpSysNotificationNotifyProcessor notifyProcessor;
    @Inject
    ErpSysNotificationMarkReadProcessor markReadProcessor;
    @Inject
    ErpSysNotificationMarkAllReadProcessor markAllReadProcessor;

    public ErpSysNotificationBizModel() {
        setEntityName(ErpSysNotification.class.getName());
    }

    @Override
    @BizMutation
    public List<ErpSysNotification> notify(@Name("eventType") String eventType,
                                           @Name("context") Map<String, Object> context,
                                           IServiceContext ctx) {
        return notifyProcessor.notify(eventType, context, ctx);
    }

    @Override
    @BizMutation
    public ErpSysNotification markRead(@Name("notificationId") Long notificationId, IServiceContext ctx) {
        return markReadProcessor.markRead(notificationId, ctx);
    }

    @Override
    @BizMutation
    public int markAllRead(@Optional @Name("userId") String userId, IServiceContext ctx) {
        return markAllReadProcessor.markAllRead(userId, ctx);
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

    // ---------- helpers（@BizQuery 查询路径复用，与 Processor 内副本语义一致） ----------

    private String resolveUserId(String userId, IServiceContext ctx) {
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }
        return ctx == null ? null : ctx.getUserId();
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
            q.addFilter(io.nop.api.core.beans.FilterBeans.notIn("id", new ArrayList<>(readIds)));
        }
        q.addOrderField("sentAt", true);
        return dao.findAllByQuery(q);
    }
}
