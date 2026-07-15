package app.erp.crm.service.support;

import app.erp.crm.dao.entity.ErpCrmActivity;
import app.erp.crm.dao.entity.ErpCrmEvent;
import app.erp.crm.service.ErpCrmConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 线索活动时间线聚合器：合并关联的 {@link ErpCrmEvent} + {@link ErpCrmActivity}，
 * 按时间倒序返回只读聚合视图（每条 Map 含 {@code sourceType}/{@code timestamp}/{@code title} 等字段）。
 *
 * <p>对齐 {@code docs/design/crm/README.md §业务规则2 活动时间线}。同域 to-many 查询，不跨域。
 */
public class EventTimelineAggregator {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 返回指定线索的活动时间线（Event + Activity 合并，按 timestamp 倒序）。
     */
    public List<Map<String, Object>> buildTimeline(Long leadId) {
        List<Map<String, Object>> entries = new ArrayList<>();
        if (leadId == null) {
            return entries;
        }
        for (ErpCrmEvent event : loadEvents(leadId)) {
            entries.add(toEntry(event));
        }
        for (ErpCrmActivity activity : loadActivities(leadId)) {
            entries.add(toEntry(activity));
        }
        entries.sort(Comparator.comparing(m -> (LocalDateTime) m.get("timestamp"),
                Comparator.nullsLast(Comparator.reverseOrder())));
        return entries;
    }

    protected Map<String, Object> toEntry(ErpCrmEvent event) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sourceType", ErpCrmConstants.TIMELINE_SOURCE_EVENT);
        m.put("id", event.getId());
        Timestamp ts = event.getStartDateTime();
        m.put("timestamp", ts != null ? ts.toLocalDateTime() : null);
        m.put("title", event.getSubject());
        m.put("typeCode", event.getEventType());
        m.put("status", event.getStatus());
        m.put("priority", event.getPriority());
        m.put("ownerId", event.getOwnerId());
        m.put("remark", event.getRemark());
        return m;
    }

    protected Map<String, Object> toEntry(ErpCrmActivity activity) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sourceType", ErpCrmConstants.TIMELINE_SOURCE_ACTIVITY);
        m.put("id", activity.getId());
        LocalDate d = activity.getActivityDate();
        m.put("timestamp", d != null ? d.atStartOfDay() : null);
        m.put("title", activity.getSummary());
        m.put("typeCode", activity.getActivityType());
        m.put("status", null);
        m.put("priority", null);
        m.put("ownerId", activity.getOwnerId());
        m.put("remark", activity.getRemark());
        return m;
    }

    protected List<ErpCrmEvent> loadEvents(Long leadId) {
        IEntityDao<ErpCrmEvent> dao = daoProvider.daoFor(ErpCrmEvent.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedLeadId", leadId));
        return dao.findAllByQuery(q);
    }

    protected List<ErpCrmActivity> loadActivities(Long leadId) {
        IEntityDao<ErpCrmActivity> dao = daoProvider.daoFor(ErpCrmActivity.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("leadId", leadId));
        return dao.findAllByQuery(q);
    }
}
