
package app.erp.crm.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.crm.dao.entity.ErpCrmEvent;

import java.util.List;
import java.util.Map;

/**
 * 活动/事件业务接口。除标准 CRUD 外，定义 Event 状态机（complete/cancel：PLANNED→COMPLETED/CANCELLED）+
 * 到期提醒查询（{@link #findDueReminders}）+ 线索活动时间线聚合（{@link #getLeadTimeline}）。
 *
 * <p>对齐 {@code docs/design/crm/README.md §业务规则2 活动时间线派生 / §业务规则4 事件提醒}。
 */
public interface IErpCrmEventBiz extends ICrudBiz<ErpCrmEvent> {

    /**
     * 完成事件（PLANNED→COMPLETED），并派生回写关联 Lead 的 lastContactDate/nextActivityDate。
     */
    @BizMutation
    ErpCrmEvent complete(@Name("eventId") Long eventId, IServiceContext context);

    /**
     * 取消事件（PLANNED→CANCELLED），并派生回写关联 Lead 的 nextActivityDate。
     */
    @BizMutation
    ErpCrmEvent cancel(@Name("eventId") Long eventId, IServiceContext context);

    /**
     * 到期/临近事件提醒查询（供 nop-job 调用）。
     *
     * <p>config-gated by {@code erp-crm.event-reminder-enabled}（默认 true）。
     * 返回 {@code status=PLANNED} 且 {@code startDateTime BETWEEN now AND now+windowMinutes} 的事件。
     * {@code windowMinutes} 为空时取默认 60 分钟。
     */
    @BizQuery
    List<ErpCrmEvent> findDueReminders(@Optional @Name("windowMinutes") Integer windowMinutes,
                                       IServiceContext context);

    /**
     * 线索活动时间线聚合查询：合并关联的 Event + Activity，按时间倒序返回只读聚合视图。
     * 每条记录含 {@code sourceType}(EVENT/ACTIVITY)、{@code timestamp}、{@code subject/summary} 等字段。
     */
    @BizQuery
    List<Map<String, Object>> getLeadTimeline(@Name("leadId") Long leadId, IServiceContext context);
}
