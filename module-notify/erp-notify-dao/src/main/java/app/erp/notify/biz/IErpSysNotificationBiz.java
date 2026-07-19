package app.erp.notify.biz;

import app.erp.notify.dao.entity.ErpSysNotification;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
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
     * 标记某用户所有未读通知为已读，返回处理条数。userId 留空时回退到当前登录用户 ctx.getUserId()
     * （供 AMIS 收件箱页面无显式 userId 入参时复用，与 markRead 一致）。
     */
    @BizMutation
    int markAllRead(@Optional @Name("userId") String userId, IServiceContext ctx);

    /**
     * 查询某用户的未读通知列表（status=SENT/MERGED 且无已读记录）。userId 留空时回退到当前登录用户
     * ctx.getUserId()。
     */
    @BizQuery
    List<ErpSysNotification> findUnread(@Optional @Name("userId") String userId, IServiceContext ctx);

    /**
     * 查询某用户的已读通知列表（在 ErpSysNotificationRead 表中存在记录）。userId 留空时回退到 ctx.getUserId()。
     * 与 {@link #findUnread} 对称，供 AMIS 收件箱「已读」tab 直接接入（避免方案 A GraphQL sub-query 与
     * 方案 B 客户端双查拼接的复杂性——裁决见 `docs/design/notify/inbox-patterns.md` §2）。
     */
    @BizQuery
    List<ErpSysNotification> findRead(@Optional @Name("userId") String userId, IServiceContext ctx);

    /**
     * 统计某用户未读通知数。userId 留空时回退到当前登录用户 ctx.getUserId()。
     */
    @BizQuery
    long countUnread(@Optional @Name("userId") String userId, IServiceContext ctx);
}
