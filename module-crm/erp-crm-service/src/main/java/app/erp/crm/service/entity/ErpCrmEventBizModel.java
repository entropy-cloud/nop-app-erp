package app.erp.crm.service.entity;

import app.erp.crm.biz.IErpCrmEventBiz;
import app.erp.crm.dao.entity.ErpCrmEvent;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.ErpCrmErrors;
import app.erp.crm.service.support.EventTimelineAggregator;
import app.erp.crm.service.support.LeadActivityDerivationHelper;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.le;
import java.util.Collections;

/**
 * 活动/事件 BizModel。Event 状态机（complete/cancel：PLANNED→COMPLETED/CANCELLED）+
 * 派生回写关联 Lead 的 lastContactDate/nextActivityDate（推模式）+
 * 到期提醒查询（{@link #findDueReminders}，config-gated）+ 线索活动时间线聚合（{@link #getLeadTimeline}）。
 *
 * <p>对齐 {@code docs/design/crm/README.md §业务规则2 活动时间线派生 / §业务规则4 事件提醒}。
 */
@BizModel("ErpCrmEvent")
public class ErpCrmEventBizModel extends CrudBizModel<ErpCrmEvent> implements IErpCrmEventBiz {

    @Inject
    LeadActivityDerivationHelper leadDerivationHelper;

    @Inject
    EventTimelineAggregator timelineAggregator;

    @Inject
    IOrmTemplate ormTemplate;

    public ErpCrmEventBizModel() {
        setEntityName(ErpCrmEvent.class.getName());
    }

    @Override
    @BizMutation
    public ErpCrmEvent complete(@Name("eventId") Long eventId, IServiceContext context) {
        ErpCrmEvent event = requireEvent(eventId, context);
        validatePlanned(event, "complete");
        event.setStatus(ErpCrmConstants.EVENT_STATUS_COMPLETED);
        updateEntity(event, null, context);
        // 派生查询需读取本轮 status 变更：显式 flush 使派生查询可见。
        ormTemplate.flushSession();
        deriveLeadFields(event.getRelatedLeadId());
        return event;
    }

    @Override
    @BizMutation
    public ErpCrmEvent cancel(@Name("eventId") Long eventId, IServiceContext context) {
        ErpCrmEvent event = requireEvent(eventId, context);
        validatePlanned(event, "cancel");
        event.setStatus(ErpCrmConstants.EVENT_STATUS_CANCELLED);
        updateEntity(event, null, context);
        ormTemplate.flushSession();
        deriveLeadFields(event.getRelatedLeadId());
        return event;
    }

    @Override
    @BizQuery
    public List<ErpCrmEvent> findDueReminders(@Optional @Name("windowMinutes") Integer windowMinutes,
                                              IServiceContext context) {
        boolean enabled = io.nop.api.core.config.AppConfig.var(
                ErpCrmConstants.CONFIG_EVENT_REMINDER_ENABLED, Boolean.TRUE);
        if (!enabled) {
            return java.util.Collections.emptyList();
        }
        int window = windowMinutes == null ? 60 : windowMinutes;
        LocalDateTime now = CoreMetrics.currentDateTime();
        LocalDateTime upper = now.plusMinutes(window);

        // 到期提醒窗口（now ≤ startDateTime ≤ now+window）由方法内部构造，非用户传入过滤；
        // startDateTime 的 XMeta 仅暴露 eq/in/dateBetween，故走直接查询避免 meta 限制内部派生查询。
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpCrmConstants.EVENT_STATUS_PLANNED));
        q.addFilter(ge("startDateTime", now));
        q.addFilter(le("startDateTime", upper));
        q.addOrderField("startDateTime", false);
        return doFindListByQueryDirectly(q, context);
    }

    @Override
    @BizQuery
    public List<Map<String, Object>> getLeadTimeline(@Name("leadId") Long leadId, IServiceContext context) {
        return timelineAggregator.buildTimeline(leadId);
    }

    // ---------- 内部辅助 ----------

    protected ErpCrmEvent requireEvent(Long eventId, IServiceContext context) {
        ErpCrmEvent event = get(String.valueOf(eventId), false, context);
        if (event == null) {
            throw new NopException(ErpCrmErrors.ERR_EVENT_NOT_FOUND)
                    .param(ErpCrmErrors.ARG_EVENT_ID, eventId);
        }
        return event;
    }

    protected void validatePlanned(ErpCrmEvent event, String action) {
        String status = event.getStatus();
        if (!Objects.equals(status, ErpCrmConstants.EVENT_STATUS_PLANNED)) {
            throw new NopException(ErpCrmErrors.ERR_EVENT_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCrmErrors.ARG_EVENT_CODE, event.getCode())
                    .param(ErpCrmErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpCrmErrors.ARG_EXPECTED_STATUS, ErpCrmConstants.EVENT_STATUS_PLANNED);
        }
    }

    /**
     * Event 无关联 Lead 时跳过派生（{@code relatedLeadId} 为空）。
     */
    protected void deriveLeadFields(Long relatedLeadId) {
        if (relatedLeadId == null) {
            return;
        }
        leadDerivationHelper.recalculateForLead(relatedLeadId);
    }

    

}
