package app.erp.notify.service.processor;

import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationRead;
import app.erp.notify.service.ErpNotifyConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.notIn;

/**
 * 通知已读标记 per-mutation Processor 共享基类（R6.7，{@code processor-extension-pattern.md} facade protected helper 范式）。
 * 承载 markRead/markAllRead 共用的 userId 解析、未读列表查询、已读判重辅助（单一真相源）。子类只编排单 mutation 步骤顺序。
 */
public abstract class AbstractErpSysNotificationProcessor {

    @Inject
    IDaoProvider daoProvider;

    protected IEntityDao<ErpSysNotification> notificationDao() {
        return daoProvider.daoFor(ErpSysNotification.class);
    }

    protected IEntityDao<ErpSysNotificationRead> readDao() {
        return daoProvider.daoFor(ErpSysNotificationRead.class);
    }

    protected String resolveUserId(String userId, IServiceContext ctx) {
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }
        return ctx == null ? null : ctx.getUserId();
    }

    protected boolean isRead(String notificationId, String userId, IEntityDao<ErpSysNotificationRead> dao) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationId", notificationId));
        q.addFilter(eq("userId", userId));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    protected List<ErpSysNotification> unreadOf(String userId) {
        if (userId == null || userId.isEmpty()) {
            return Collections.emptyList();
        }
        IEntityDao<ErpSysNotification> dao = notificationDao();
        QueryBean readQ = new QueryBean();
        readQ.addFilter(eq("userId", userId));
        List<ErpSysNotificationRead> reads = readDao().findAllByQuery(readQ);
        Set<String> readIds = new HashSet<>();
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

    protected ErpSysNotificationRead newReadEntry(String notificationId, String userId) {
        ErpSysNotificationRead read = readDao().newEntity();
        read.setNotificationId(notificationId);
        read.setUserId(userId);
        read.setReadTime(CoreMetrics.currentTimestamp());
        return read;
    }
}
