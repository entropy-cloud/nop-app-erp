package app.erp.notify.service.processor;

import app.erp.notify.dao.entity.ErpSysNotification;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;

import java.util.List;

/**
 * ErpSysNotification markAllRead per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含全部未读标记已读编排：userId 解析（显式传入优先，回退 ctx.getUserId()）+ 未读列表查询 + 逐条判重写入 ErpSysNotificationRead。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpSysNotificationProcessor}。
 */
public class ErpSysNotificationMarkAllReadProcessor extends AbstractErpSysNotificationProcessor {

    public int markAllRead(String userId, IServiceContext ctx) {
        String resolved = resolveUserId(userId, ctx);
        List<ErpSysNotification> unread = unreadOf(resolved);
        IEntityDao<app.erp.notify.dao.entity.ErpSysNotificationRead> readDao = readDao();
        int count = 0;
        for (ErpSysNotification n : unread) {
            if (!isRead(n.getId(), resolved, readDao)) {
                readDao.saveEntity(newReadEntry(n.getId(), resolved));
                count++;
            }
        }
        return count;
    }
}
