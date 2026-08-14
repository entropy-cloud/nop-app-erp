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
 * 自包含 DRAFT→SCHEDULED 编排：状态守卫 + 排程前置校验（执行人/日期）+ 同设备/同执行人 同日冲突检测（双维度）+ 状态翻转 + 落库。
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

    /**
     * 排程冲突检测（L1 UC-MAIN-09 双维度）：设备维度（equipmentId + visitDate）与人员维度（assignedTo + visitDate）
     * 独立查询、独立判定，命中即抛 {@link ErpMntErrors#ERR_VISIT_SCHEDULE_CONFLICT}（E1 裁决选项 A：单一冲突语义复用同一错误码，
     * 参数经 ARG_EQUIPMENT_ID/ARG_ASSIGNED_TO 区分维度）。设备维度查询先于人员维度执行（E1 确定性顺序义务）。
     */
    protected void checkScheduleConflict(ErpMntVisit visit, IServiceContext context) {
        if (visit.getVisitDate() == null) {
            return;
        }
        if (visit.getEquipmentId() != null) {
            checkEquipmentDimensionConflict(visit);
        }
        checkPersonnelDimensionConflict(visit);
    }

    protected void checkEquipmentDimensionConflict(ErpMntVisit visit) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("equipmentId", visit.getEquipmentId()),
                eq("visitDate", visit.getVisitDate()),
                in("status", java.util.Arrays.asList(
                        ErpMntDaoConstants.VISIT_STATUS_SCHEDULED,
                        ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS))));
        for (ErpMntVisit conflict : visitDao().findAllByQuery(q)) {
            if (!conflict.getId().equals(visit.getId())) {
                throwScheduleConflict(visit, conflict);
            }
        }
    }

    protected void checkPersonnelDimensionConflict(ErpMntVisit visit) {
        if (visit.getAssignedTo() == null) {
            return;
        }
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("assignedTo", visit.getAssignedTo()),
                eq("visitDate", visit.getVisitDate()),
                in("status", java.util.Arrays.asList(
                        ErpMntDaoConstants.VISIT_STATUS_SCHEDULED,
                        ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS))));
        for (ErpMntVisit conflict : visitDao().findAllByQuery(q)) {
            if (!conflict.getId().equals(visit.getId())) {
                throwScheduleConflict(visit, conflict);
            }
        }
    }

    protected void throwScheduleConflict(ErpMntVisit visit, ErpMntVisit conflict) {
        throw new NopException(ErpMntErrors.ERR_VISIT_SCHEDULE_CONFLICT)
                .param(ErpMntErrors.ARG_VISIT_CODE, visit.getCode())
                .param(ErpMntErrors.ARG_EQUIPMENT_ID, visit.getEquipmentId())
                .param(ErpMntErrors.ARG_ASSIGNED_TO, visit.getAssignedTo())
                .param(ErpMntErrors.ARG_CONFLICT_VISIT_CODE, conflict.getCode());
    }

    protected void doSchedule(ErpMntVisit visit, IServiceContext context) {
        visit.setStatus(stateMachine.scheduleTargetStatus());
        visitDao().updateEntity(visit);
    }
}
