package app.erp.crm.service.processor;

import app.erp.crm.dao.entity.ErpCrmEvent;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.ErpCrmErrors;
import app.erp.crm.service.support.LeadActivityDerivationHelper;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpCrmEvent complete per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含活动/事件完成编排（PLANNED→COMPLETED + flush 后派生回写关联 Lead 字段）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCrmEventCompleteProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    LeadActivityDerivationHelper leadDerivationHelper;

    public ErpCrmEvent complete(Long eventId, IServiceContext context) {
        ErpCrmEvent event = requireEvent(eventId);
        validatePlanned(event, "complete");
        event.setStatus(ErpCrmConstants.EVENT_STATUS_COMPLETED);
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

    private IEntityDao<ErpCrmEvent> dao() {
        return daoProvider.daoFor(ErpCrmEvent.class);
    }
}
