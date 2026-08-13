package app.erp.mnt.service.processor;

import app.erp.mnt.biz.IErpMntVisitBiz;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.service.ErpMntErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.exceptions.NopException;
import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * ErpMntVisit schedule per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 DRAFT→SCHEDULED 编排：状态守卫 + 排程前置校验（执行人/日期）+ 同设备同日期冲突检测 + 状态翻转 + 落库。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpMntVisitProcessor}。
 */
public class ErpMntVisitScheduleProcessor extends AbstractErpMntVisitProcessor {

    public ErpMntVisit schedule(Long visitId, IServiceContext context) {
        ErpMntVisit visit = requireVisit(visitId, context);
        String from = visit.getStatus();
        try {
            stateMachine.assertCanSchedule(from);
        } catch (NopException e) {
            throw illegalVisitTransition(visit, from, ErpMntDaoConstants.VISIT_STATUS_DRAFT, e);
        }
        validateSchedulePrereqs(visit, context);
        checkScheduleConflict(visit, context);
        doSchedule(visit, context);
        return visit;
    }

    protected void validateSchedulePrereqs(ErpMntVisit visit, IServiceContext context) {
        if (visit.getAssignedTo() == null) {
            throw new NopException(ErpMntErrors.ERR_VISIT_ASSIGNED_TO_REQUIRED)
                    .param(ErpMntErrors.ARG_VISIT_CODE, visit.getCode())
                    .param(ErpMntErrors.ARG_ASSIGNED_TO, null);
        }
        if (visit.getVisitDate() == null) {
            throw new NopException(ErpMntErrors.ERR_VISIT_DATE_REQUIRED)
                    .param(ErpMntErrors.ARG_VISIT_CODE, visit.getCode())
                    .param(ErpMntErrors.ARG_VISIT_DATE, null);
        }
    }

    protected void checkScheduleConflict(ErpMntVisit visit, IServiceContext context) {
        if (visit.getEquipmentId() == null || visit.getVisitDate() == null) {
            return;
        }
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("equipmentId", visit.getEquipmentId()),
                eq("visitDate", visit.getVisitDate()),
                in("status", java.util.Arrays.asList(
                        ErpMntDaoConstants.VISIT_STATUS_SCHEDULED,
                        ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS))));
        java.util.List<ErpMntVisit> conflicts = visitDao().findAllByQuery(q);
        for (ErpMntVisit conflict : conflicts) {
            if (!conflict.getId().equals(visit.getId())) {
                throw new NopException(ErpMntErrors.ERR_VISIT_SCHEDULE_CONFLICT)
                        .param(ErpMntErrors.ARG_VISIT_CODE, visit.getCode())
                        .param(ErpMntErrors.ARG_EQUIPMENT_ID, visit.getEquipmentId())
                        .param(ErpMntErrors.ARG_CONFLICT_VISIT_CODE, conflict.getCode());
            }
        }
    }

    protected void doSchedule(ErpMntVisit visit, IServiceContext context) {
        visit.setStatus(stateMachine.scheduleTargetStatus());
        visitDao().updateEntity(visit);
    }
}
