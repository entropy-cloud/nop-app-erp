package app.erp.notify.biz;

import app.erp.notify.dao.entity.ErpSysNotification;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.orm.biz.ICrudBiz;
import io.nop.core.context.IServiceContext;

import java.util.List;
import java.util.Map;

public interface IErpSysNotificationBiz extends ICrudBiz<ErpSysNotification> {

    /**
     * 统一派发入口：按 eventType 解析 ACTIVE 模板→解析接收人→频控合并→落站内消息→（config-gated）派发外发通道。
     * 无 ACTIVE 模板则 config-gated 静默跳过（不阻断调用方）。返回派发的通知实例集合（可能为空）。
     */
    @BizMutation
    List<ErpSysNotification> notify(@Name("eventType") String eventType,
                                    @Name("context") Map<String, Object> context,
                                    IServiceContext context2);

    /**
     * 标记单条通知为已读：写入 ErpSysNotificationRead（唯一键 notificationId+userId 防重复）。
     */
    @BizMutation
    ErpSysNotification markRead(@Name("notificationId") Long notificationId, IServiceContext ctx);

    /**
     * 标记某用户所有未读通知为已读，返回处理条数。
     */
    @BizMutation
    int markAllRead(@Name("userId") String userId, IServiceContext ctx);

    /**
     * 查询某用户的未读通知列表（status=SENT/MERGED 且无已读记录）。
     */
    @BizQuery
    List<ErpSysNotification> findUnread(@Name("userId") String userId, IServiceContext ctx);

    /**
     * 统计某用户未读通知数。
     */
    @BizQuery
    long countUnread(@Name("userId") String userId, IServiceContext ctx);
}
