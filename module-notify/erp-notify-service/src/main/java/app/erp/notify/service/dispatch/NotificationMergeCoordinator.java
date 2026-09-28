package app.erp.notify.service.dispatch;

import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationRead;
import app.erp.notify.service.ErpNotifyConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpSysNotification、ErpSysNotificationRead）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * 通知频控合并协调器。按模板 {@code mergeStrategy} + {@code mergeWindowSeconds} 在时间窗口内
 * 对同 (recipientUser, eventType) 合并：业务提醒合并为一条、异常告警合并含次数（mergeCount）。
 *
 * <p>并发安全（Decision Phase 3）：采用 DB 时间窗查询兜底——窗口内查询既有 PENDING/SENT 且未读的实例，
 * 命中则原子递增 mergeCount 并更新 body；未命中则由调用方新建。ErpSysNotificationRead 唯一键
 * (notificationId,userId) 防重复已读。多实例高并发场景归 nop-message 异步总线后继。
 */
public class NotificationMergeCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationMergeCoordinator.class);

    @Inject
    IDaoProvider daoProvider;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    /**
     * 查找窗口内可合并的既有通知实例（同 user+eventType，状态 PENDING/SENT，且尚未被标记已读）。
     *
     * @return 命中则返回该实例（调用方应合并、不新建）；未命中或策略 NONE 返回 null
     */
    public ErpSysNotification findMergeable(String mergeStrategy, int mergeWindowSeconds,
                                            String notificationType, String recipientUserId) {
        if (StringHelper.isBlank(recipientUserId)) {
            return null;
        }
        if (ErpNotifyConstants.MERGE_NONE.equals(mergeStrategy) || mergeWindowSeconds <= 0) {
            return null;
        }
        LocalDateTime windowStart = CoreMetrics.currentDateTime().minusSeconds(mergeWindowSeconds);

        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", notificationType));
        q.addFilter(eq("recipientUserId", recipientUserId));
        q.addFilter(in("status", List.of(
                ErpNotifyConstants.STATUS_PENDING,
                ErpNotifyConstants.STATUS_SENT,
                ErpNotifyConstants.STATUS_MERGED)));
        q.addFilter(ge("createTime", windowStart));
        q.addOrderField("createTime", true);
        q.setLimit(10);

        IEntityDao<ErpSysNotification> dao = daoProvider.daoFor(ErpSysNotification.class);
        List<ErpSysNotification> candidates = dao.findAllByQuery(q);
        if (candidates.isEmpty()) {
            return null;
        }
        // perf-ux plan 0835-1 B5：原逐候选 isRead（每候选 1 次查询，每接收人 ≤11 次往返）改单次
        // in(notificationId) 批载已读集，内存按候选原序（createTime desc）取首个未读——
        // 成员判定与顺序无关，与逐候选短路逐位等价（候选 ≤10 不分块，留痕）。
        Set<String> readIds = findReadNotificationIds(candidates, recipientUserId);
        for (ErpSysNotification n : candidates) {
            if (!readIds.contains(n.getId())) {
                return n;
            }
        }
        return null;
    }

    /**
     * 将一次新的派发合并到既有实例：递增 mergeCount，追加 body 标记（异常告警含发生次数），状态置 MERGED。
     * 仅修改内存对象，不持久化——持久化由调用方（BizModel）统一处理。
     *
     * @return 合并后的既有实例（待持久化）
     */
    public ErpSysNotification mergeInto(ErpSysNotification existing, String renderedSubject, String renderedBody) {
        int newCount = (existing.getMergeCount() == null ? 1 : existing.getMergeCount()) + 1;
        existing.setMergeCount(newCount);
        existing.setStatus(ErpNotifyConstants.STATUS_MERGED);
        existing.setUpdateTime(CoreMetrics.currentTimestamp());
        if (!StringHelper.isBlank(renderedBody)) {
            existing.setBody(renderedBody);
        }
        if (!StringHelper.isBlank(renderedSubject)) {
            existing.setSubject(renderedSubject);
        }
        return existing;
    }

    /** 批量查已读通知 id 集（等价替代逐候选 isRead 短路查询）。 */
    private Set<String> findReadNotificationIds(List<ErpSysNotification> candidates, String userId) {
        List<String> ids = new java.util.ArrayList<>(candidates.size());
        for (ErpSysNotification n : candidates) {
            ids.add(n.getId());
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("userId", userId));
        q.addFilter(in("notificationId", ids));
        Set<String> ret = new java.util.HashSet<>();
        for (ErpSysNotificationRead r : daoProvider.daoFor(ErpSysNotificationRead.class).findAllByQuery(q)) {
            ret.add(r.getNotificationId());
        }
        return ret;
    }
}
