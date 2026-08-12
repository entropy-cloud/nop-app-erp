package app.erp.crm.service.processor;

import app.erp.crm.dao.entity.ErpCrmEvent;
import app.erp.crm.service.ErpCrmConstants;
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
 * ErpCrmEvent complete per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含活动/事件完成编排（PLANNED→COMPLETED + flush 后派生回写关联 Lead 字段）。
 *
 * <p>固定来源态/目标态判断委托 {@link ErpCrmEventStateMachine}（Event status 轴 Bean，契约 §4/§7）；
 * 动态业务守卫（requireEvent not-found、Lead 派生、relatedLeadId==null 跳过、乐观锁）保留原位。非法边 Bean 抛 common 层码
 * （含 {@code action}/fromStatus 元数据），本 Processor 捕获后映射领域码 {@link ErpCrmErrors#ERR_EVENT_ILLEGAL_STATUS_TRANSITION}
 * （+ eventCode/currentStatus/expectedStatus 实体编号/上下文，common 码作 cause 保留）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCrmEventCompleteProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    LeadActivityDerivationHelper leadDerivationHelper;

    @Inject
    ErpCrmEventStateMachine stateMachine;

    public ErpCrmEvent complete(Long eventId, IServiceContext context) {
        ErpCrmEvent event = requireEvent(eventId);
        try {
            stateMachine.assertCanComplete(event.getStatus());
        } catch (NopException e) {
            throw illegalTransition(event, ErpCrmConstants.EVENT_STATUS_PLANNED, e);
        }
        event.setStatus(stateMachine.completeTargetStatus());
        dao().updateEntity(event);
        // 派生查询需读取本轮 status 变更：显式 flush 使派生查询可见。
        ormTemplate.flushSession();
        deriveLeadFields(event.getRelatedLeadId());
        return event;
    }

    // ---------- 内部辅助 ----------

    protected ErpCrmEvent requireEvent(Long eventId) {
        ErpCrmEvent event = dao().getEntityById(eventId);
        if (event == null) {
            throw new NopException(ErpCrmErrors.ERR_EVENT_NOT_FOUND)
                    .param(ErpCrmErrors.ARG_EVENT_ID, eventId);
        }
        return event;
    }

    /** 领域非法迁移异常构造；可选 {@code cause} 保留 Bean 抛出的 common 层非法边报告（契约 §7）。 */
    protected NopException illegalTransition(ErpCrmEvent event, String expected, Throwable cause) {
        return new NopException(ErpCrmErrors.ERR_EVENT_ILLEGAL_STATUS_TRANSITION, cause)
                .param(ErpCrmErrors.ARG_EVENT_CODE, event.getCode())
                .param(ErpCrmErrors.ARG_CURRENT_STATUS, event.getStatus())
                .param(ErpCrmErrors.ARG_EXPECTED_STATUS, expected);
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

    private IEntityDao<ErpCrmEvent> dao() {
        return daoProvider.daoFor(ErpCrmEvent.class);
    }
}
