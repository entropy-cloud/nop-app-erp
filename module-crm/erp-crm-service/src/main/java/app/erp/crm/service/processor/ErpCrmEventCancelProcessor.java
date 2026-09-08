package app.erp.crm.service.processor;

import app.erp.crm.dao.entity.ErpCrmEvent;
import app.erp.crm.service.ErpCrmErrors;
import app.erp.crm.service.statemachine.ErpCrmEventStateMachine;
import app.erp.crm.service.support.LeadActivityDerivationHelper;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

/**
 * ErpCrmEvent cancel per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含活动/事件取消编排（PLANNED→CANCELLED + flush 后派生回写关联 Lead 字段）。
 *
 * <p>固定来源态/目标态判断委托 {@link ErpCrmEventStateMachine}（Event status 轴 Bean，契约 §4/§7）；
 * 动态业务守卫（requireEvent not-found、Lead 派生、relatedLeadId==null 跳过、乐观锁）保留原位。非法边 Bean 直抛领域码
 * {@link ErpCrmErrors#ERR_EVENT_ILLEGAL_STATUS_TRANSITION}（plan 2026-09-07-2200-1），本处同码补参 eventCode。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCrmEventCancelProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    LeadActivityDerivationHelper leadDerivationHelper;

    @Inject
    ErpCrmEventStateMachine stateMachine;

    public ErpCrmEvent cancel(String eventId, IServiceContext context) {
        ErpCrmEvent event = requireEvent(eventId);
        try {
            stateMachine.assertCanCancel(event.getStatus());
        } catch (NopException e) {
            throw e.param(ErpCrmErrors.ARG_EVENT_CODE, event.getCode());
        }
        event.setStatus(stateMachine.cancelTargetStatus());
        dao().updateEntity(event);
        ormTemplate.flushSession();
        deriveLeadFields(event.getRelatedLeadId());
        return event;
    }

    // ---------- 内部辅助 ----------

    protected ErpCrmEvent requireEvent(String eventId) {
        ErpCrmEvent event = dao().getEntityById(eventId);
        if (event == null) {
            throw new NopException(ErpCrmErrors.ERR_EVENT_NOT_FOUND)
                    .param(ErpCrmErrors.ARG_EVENT_ID, eventId);
        }
        return event;
    }

    /**
     * Event 无关联 Lead 时跳过派生（{@code relatedLeadId} 为空）。
     */
    protected void deriveLeadFields(String relatedLeadId) {
        if (relatedLeadId == null) {
            return;
        }
        leadDerivationHelper.recalculateForLead(relatedLeadId);
    }

    private IEntityDao<ErpCrmEvent> dao() {
        return daoProvider.daoFor(ErpCrmEvent.class);
    }
}
