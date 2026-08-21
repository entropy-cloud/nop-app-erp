package app.erp.notify.service.processor;

import app.erp.notify.dao.entity.ErpSysNotification;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;

/**
 * ErpSysNotification markRead per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含单条已读标记编排：加载通知 + userId 解析（优先 recipientUserId，回退 ctx.getUserId()）+ 已读判重 + 写入 ErpSysNotificationRead。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpSysNotificationProcessor}。
 */
public class ErpSysNotificationMarkReadProcessor extends AbstractErpSysNotificationProcessor {

    public ErpSysNotification markRead(String notificationId, IServiceContext ctx) {
        IEntityDao<ErpSysNotification> dao = notificationDao();
        ErpSysNotification n = dao.getEntityById(notificationId);
        // 通知按 recipientUserId 投递，已读记录以接收人为准（与 findUnread/countUnread 口径一致）；
        // recipientUserId 缺失时回退当前登录用户（与 resolveUserId 模式一致）
        String userId = n != null ? n.getRecipientUserId() : null;
        if (userId == null) {
            userId = ctx.getUserId();
        }
        IEntityDao<app.erp.notify.dao.entity.ErpSysNotificationRead> readDao = readDao();
        if (!isRead(notificationId, userId, readDao)) {
            readDao.saveEntity(newReadEntry(notificationId, userId));
        }
        return n;
    }
}
