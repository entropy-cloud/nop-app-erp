package app.erp.crm.service.support;

import app.erp.crm.dao.entity.ErpCrmEvent;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.service.ErpCrmConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 线索活动派生字段回写器：从关联 {@link ErpCrmEvent} 派生 {@link ErpCrmLead} 的
 * {@code lastContactDate}（最近 COMPLETED 事件 startDateTime 最大值）与
 * {@code nextActivityDate}（最近未来 PLANNED 事件 startDateTime 最小值）。
 *
 * <p>对齐 {@code docs/design/crm/README.md §业务规则2 活动时间线自动派生}。
 * 推模式：Event 状态变更时即时回写 Lead（查询零成本，避免 Lead 列表 N+1）。
 */
public class LeadActivityDerivationHelper {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 重算并回写指定 Lead 的派生字段。无关联事件时将派生字段置 null。
     */
    public void recalculateForLead(Long leadId) {
        if (leadId == null) {
            return;
        }
        IEntityDao<ErpCrmLead> leadDao = daoProvider.daoFor(ErpCrmLead.class);
        ErpCrmLead lead = leadDao.getEntityById(leadId);
        if (lead == null) {
            return;
        }

        lead.setLastContactDate(latestCompletedStartDateTime(leadId));
        lead.setNextActivityDate(earliestPlannedStartDateTime(leadId));
        leadDao.updateEntity(lead);
    }

    /**
     * 取指定线索关联的最近一条 COMPLETED 事件的 startDateTime（最大值）。
     */
    protected LocalDateTime latestCompletedStartDateTime(Long leadId) {
        List<ErpCrmEvent> completed = loadEvents(leadId, ErpCrmConstants.EVENT_STATUS_COMPLETED);
        return completed.stream()
                .map(ErpCrmEvent::getStartDateTime)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    /**
     * 取指定线索关联的最早一条未来 PLANNED 事件的 startDateTime（最小值）。
     * "未来" 指 startDateTime >= 当前时间；过期但未完成的事件也计入 nextActivityDate（提醒补跟进）。
     */
    protected LocalDateTime earliestPlannedStartDateTime(Long leadId) {
        List<ErpCrmEvent> planned = loadEvents(leadId, ErpCrmConstants.EVENT_STATUS_PLANNED);
        return planned.stream()
                .map(ErpCrmEvent::getStartDateTime)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    protected List<ErpCrmEvent> loadEvents(Long leadId, String status) {
        IEntityDao<ErpCrmEvent> dao = daoProvider.daoFor(ErpCrmEvent.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedLeadId", leadId));
        q.addFilter(eq("status", status));
        return dao.findAllByQuery(q);
    }
}
