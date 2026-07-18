package app.erp.mnt.service.entity;

import app.erp.mnt.biz.IErpMntVisitBiz;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.service.ErpMntErrors;
import app.erp.mnt.service.posting.MaintenanceLaborPostingDispatcher;
import app.erp.mnt.service.support.EquipmentStatusLinker;
import io.nop.api.core.annotations.biz.BizModel;
import java.util.List;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import io.nop.core.context.IServiceContext;
import java.util.Objects;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

@BizModel("ErpMntVisit")
public class ErpMntVisitBizModel extends CrudBizModel<ErpMntVisit> implements IErpMntVisitBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpMntVisitBizModel.class);

    @jakarta.inject.Inject
    EquipmentStatusLinker equipmentStatusLinker;

    @jakarta.inject.Inject
    MaintenanceLaborPostingDispatcher laborPostingDispatcher;

    public ErpMntVisitBizModel() {
        setEntityName(ErpMntVisit.class.getName());
    }

    @Override
    @BizMutation
    public ErpMntVisit schedule(@Name("visitId") Long visitId, IServiceContext context) {
        ErpMntVisit visit = requireVisit(visitId, context);
        validateTransition(visit, ErpMntDaoConstants.VISIT_STATUS_DRAFT, "DRAFT", context);
        validateSchedulePrereqs(visit, context);
        checkScheduleConflict(visit, context);
        doSchedule(visit, context);
        return visit;
    }

    @Override
    @BizMutation
    public ErpMntVisit start(@Name("visitId") Long visitId, IServiceContext context) {
        ErpMntVisit visit = requireVisit(visitId, context);
        validateTransition(visit, ErpMntDaoConstants.VISIT_STATUS_SCHEDULED, "SCHEDULED", context);
        doStart(visit, context);
        equipmentStatusLinker.linkToUnderMaintenance(visit.getEquipmentId(), context);
        return visit;
    }

    @Override
    @BizMutation
    public ErpMntVisit complete(@Name("visitId") Long visitId, IServiceContext context) {
        ErpMntVisit visit = requireVisit(visitId, context);
        validateTransition(visit, ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS, "IN_PROGRESS", context);
        doComplete(visit, context);
        equipmentStatusLinker.restoreToRunning(visit.getEquipmentId(), context);
        return visit;
    }

    @Override
    @BizMutation
    public ErpMntVisit cancel(@Name("visitId") Long visitId, IServiceContext context) {
        ErpMntVisit visit = requireVisit(visitId, context);
        validateNotTerminal(visit, context);
        doCancel(visit, context);
        equipmentStatusLinker.restoreToRunning(visit.getEquipmentId(), context);
        return visit;
    }

    // ---------- step：迁移校验 ----------

    protected ErpMntVisit requireVisit(Long visitId, IServiceContext context) {
        ErpMntVisit visit = get(String.valueOf(visitId), false, context);
        if (visit == null) {
            throw new NopException(ErpMntErrors.ERR_VISIT_NOT_FOUND).param(ErpMntErrors.ARG_VISIT_ID, visitId);
        }
        return visit;
    }

    protected void validateTransition(ErpMntVisit visit, String expected, String expectedName, IServiceContext context) {
        String status = visit.getStatus();
        if (status == null || !Objects.equals(status, expected)) {
            throw illegalVisitTransition(visit, status, expectedName);
        }
    }

    protected void validateNotTerminal(ErpMntVisit visit, IServiceContext context) {
        String status = visit.getStatus();
        if (status != null && (Objects.equals(status, ErpMntDaoConstants.VISIT_STATUS_COMPLETED)
                || Objects.equals(status, ErpMntDaoConstants.VISIT_STATUS_CANCELLED))) {
            throw illegalVisitTransition(visit, status, "非终态");
        }
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
        ErpMntVisit conflict = findFirst(q, null, context);
        if (conflict != null && !conflict.getId().equals(visit.getId())) {
            throw new NopException(ErpMntErrors.ERR_VISIT_SCHEDULE_CONFLICT)
                    .param(ErpMntErrors.ARG_VISIT_CODE, visit.getCode())
                    .param(ErpMntErrors.ARG_EQUIPMENT_ID, visit.getEquipmentId())
                    .param(ErpMntErrors.ARG_CONFLICT_VISIT_CODE, conflict.getCode());
        }
    }

    // ---------- step：执行 ----------

    protected void doSchedule(ErpMntVisit visit, IServiceContext context) {
        visit.setStatus(ErpMntDaoConstants.VISIT_STATUS_SCHEDULED);
        updateEntity(visit, null, context);
    }

    protected void doStart(ErpMntVisit visit, IServiceContext context) {
        visit.setStatus(ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS);
        if (visit.getStartTime() == null) {
            visit.setStartTime(CoreMetrics.currentTimestamp());
        }
        updateEntity(visit, null, context);
    }

    protected void doComplete(ErpMntVisit visit, IServiceContext context) {
        visit.setStatus(ErpMntDaoConstants.VISIT_STATUS_COMPLETED);
        Timestamp endTime = visit.getEndTime() == null ? CoreMetrics.currentTimestamp() : visit.getEndTime();
        visit.setEndTime(endTime);
        if (visit.getStartTime() != null) {
            long minutes = Duration.between(visit.getStartTime().toLocalDateTime(), endTime.toLocalDateTime()).toMinutes();
            visit.setTotalMinutes(BigDecimal.valueOf(minutes));
        }
        visit.setCompletedAt(CoreMetrics.currentTimestamp());
        updateEntity(visit, null, context);

        // 维修工时费用化 GL 过账（Dr: 折旧费用 6602 / Cr: 应付职工薪酬 2211），config-gated 默认关
        //（plan 2026-07-18-0949-1 Phase 1 Decision (c) 内嵌触发 + (e) 显式消费 boolean 返回值）。
        // 失败不阻断 complete 终态（吞异常范式，对齐 MaintenanceIssuePostingDispatcher.dispatchIfApplicable）。
        if (laborPostingDispatcher.isPostingEnabled()) {
            if (!laborPostingDispatcher.postLabor(visit, context)) {
                LOG.warn("Labor posting skipped or failed for visit {}", visit.getCode());
            }
        }
    }

    protected void doCancel(ErpMntVisit visit, IServiceContext context) {
        visit.setStatus(ErpMntDaoConstants.VISIT_STATUS_CANCELLED);
        updateEntity(visit, null, context);

        // 维修工时费用化 GL 红冲（cancel 时已生成 MAINTENANCE_LABOR 凭证则红冲），config-gated 与正向对称
        //（plan 2026-07-18-1745-1）。失败不阻断 cancel 终态（吞异常告警，对齐 doComplete 内 postLabor 失败语义）。
        if (laborPostingDispatcher.isPostingEnabled()) {
            try {
                laborPostingDispatcher.reverseLabor(visit);
            } catch (Exception e) {
                if (e instanceof NopException) {
                    LOG.warn("维修工时费用化红冲失败，访问 {} 保持 CANCELLED 终态（凭证孤儿由人工或兜底处理）：{}",
                            visit.getCode(), e.getMessage());
                } else {
                    LOG.error("维修工时费用化红冲异常，访问 {} 保持 CANCELLED 终态", visit.getCode(), e);
                }
            }
        }
    }

    protected NopException illegalVisitTransition(ErpMntVisit visit, String current, String expected) {
        return new NopException(ErpMntErrors.ERR_INVALID_VISIT_STATUS_TRANSITION)
                .param(ErpMntErrors.ARG_VISIT_CODE, visit.getCode())
                .param(ErpMntErrors.ARG_CURRENT_STATUS, current)
                .param(ErpMntErrors.ARG_EXPECTED_STATUS, expected);
    }
}
