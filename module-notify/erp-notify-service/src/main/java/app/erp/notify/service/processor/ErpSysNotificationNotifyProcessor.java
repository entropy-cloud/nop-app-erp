package app.erp.notify.service.processor;

import app.erp.notify.biz.IErpSysNotificationTemplateBiz;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import app.erp.notify.service.ErpNotifyConstants;
import app.erp.notify.service.dispatch.NotificationDispatcher;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpSysNotification）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpSysNotification notify per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含通知派发编排：查找 ACTIVE 模板 → 派发引擎（接收人解析→频控合并→站内落库→外发通道）→ 落库通知实例。
 * best-effort 语义：任何失败（模板缺失/渲染失败/接收人解析失败/落库失败）均不阻断调用方业务事实
 *（与 notification-strategy.md config-gated 语义一致）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpSysNotificationNotifyProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpSysNotificationNotifyProcessor.class);

    @Inject
    NotificationDispatcher dispatcher;
    @Inject
    IErpSysNotificationTemplateBiz templateBiz;
    @Inject
    IDaoProvider daoProvider;

    public List<ErpSysNotification> notify(String eventType, Map<String, Object> context, IServiceContext ctx) {
        try {
            ErpSysNotificationTemplate template = findActiveTemplate(eventType, ctx);
            if (template == null) {
                LOG.warn("notify: business event [{}] has no ACTIVE template, config-gated silent skip", eventType);
                return Collections.emptyList();
            }
            // P1-CK-notify-001（plan 2026-09-12-0400-1 Phase 6）：先持久化、后外发——
            // 修复前外发先于 saveEntity 执行（EMAIL/SMS 不可逆副作用早于落库）
            List<ErpSysNotification> result = dispatcher.prepare(template, context);
            IEntityDao<ErpSysNotification> dao = daoProvider.daoFor(ErpSysNotification.class);
            for (ErpSysNotification n : result) {
                if (n.getId() == null) {
                    dao.saveEntity(n);
                } else {
                    dao.updateEntity(n);
                }
            }
            dispatcher.dispatchExternal(result, dispatcher.parseChannels(template));
            return result;
        } catch (Exception e) {
            // 通知是 best-effort 关注点：任何失败（模板缺失/渲染失败/接收人解析失败/落库失败）
            // 均不阻断调用方业务事实（与 notification-strategy.md config-gated 语义一致）。
            LOG.error("notify: business event [{}] notification dispatch failed (non-blocking for caller): {}", eventType, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    protected ErpSysNotificationTemplate findActiveTemplate(String eventType, IServiceContext ctx) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", eventType));
        q.addFilter(eq("status", ErpNotifyConstants.TEMPLATE_ACTIVE));
        q.setLimit(1);
        return templateBiz.findFirst(q, null, ctx);
    }
}
