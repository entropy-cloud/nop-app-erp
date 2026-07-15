package app.erp.mnt.service.entity;

import app.erp.mnt.biz.IErpMntRequestBiz;
import app.erp.mnt.biz.IErpMntVisitBiz;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntRequest;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.service.ErpMntErrors;
import io.nop.api.core.annotations.biz.BizModel;
import java.util.List;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import java.util.Objects;

import java.util.LinkedHashMap;
import java.util.Map;

@BizModel("ErpMntRequest")
public class ErpMntRequestBizModel extends CrudBizModel<ErpMntRequest> implements IErpMntRequestBiz {

    @jakarta.inject.Inject
    IErpMntVisitBiz visitBiz;

    public ErpMntRequestBizModel() {
        setEntityName(ErpMntRequest.class.getName());
    }

    @Override
    @BizMutation
    public ErpMntRequest accept(@Name("requestId") Long requestId, IServiceContext context) {
        ErpMntRequest request = requireRequest(requestId, context);
        validateTransition(request, ErpMntDaoConstants.REQUEST_STATUS_OPEN, "OPEN", context);
        ErpMntVisit visit = generateResponsiveVisit(request, context);
        doAccept(request, visit, context);
        return request;
    }

    @Override
    @BizMutation
    public ErpMntRequest startRepair(@Name("requestId") Long requestId, IServiceContext context) {
        ErpMntRequest request = requireRequest(requestId, context);
        validateTransition(request, ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED, "ACCEPTED", context);
        doStartRepair(request, context);
        return request;
    }

    @Override
    @BizMutation
    public ErpMntRequest complete(@Name("requestId") Long requestId, IServiceContext context) {
        ErpMntRequest request = requireRequest(requestId, context);
        validateTransition(request, ErpMntDaoConstants.REQUEST_STATUS_IN_PROGRESS, "IN_PROGRESS", context);
        doComplete(request, context);
        return request;
    }

    @Override
    @BizMutation
    public ErpMntRequest rejectRequest(@Name("requestId") Long requestId, IServiceContext context) {
        ErpMntRequest request = requireRequest(requestId, context);
        String status = request.getStatus();
        if (status == null || (!Objects.equals(status, ErpMntDaoConstants.REQUEST_STATUS_OPEN)
                && !Objects.equals(status, ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED))) {
            throw illegalRequestTransition(request, status, "OPEN 或 ACCEPTED");
        }
        doReject(request, context);
        return request;
    }

    @Override
    @BizMutation
    public ErpMntRequest cancel(@Name("requestId") Long requestId, IServiceContext context) {
        ErpMntRequest request = requireRequest(requestId, context);
        String status = request.getStatus();
        if (status == null || (!Objects.equals(status, ErpMntDaoConstants.REQUEST_STATUS_OPEN)
                && !Objects.equals(status, ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED))) {
            throw illegalRequestTransition(request, status, "OPEN 或 ACCEPTED");
        }
        doCancel(request, context);
        return request;
    }

    // ---------- step：迁移校验 ----------

    protected ErpMntRequest requireRequest(Long requestId, IServiceContext context) {
        ErpMntRequest request = get(String.valueOf(requestId), false, context);
        if (request == null) {
            throw new NopException(ErpMntErrors.ERR_REQUEST_NOT_FOUND).param(ErpMntErrors.ARG_REQUEST_ID, requestId);
        }
        return request;
    }

    protected void validateTransition(ErpMntRequest request, String expected, String expectedName, IServiceContext context) {
        String status = request.getStatus();
        if (status == null || !Objects.equals(status, expected)) {
            throw illegalRequestTransition(request, status, expectedName);
        }
    }

    // ---------- step：执行 ----------

    protected ErpMntVisit generateResponsiveVisit(ErpMntRequest request, IServiceContext context) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "VST-REQ-" + request.getId());
        data.put("equipmentId", request.getEquipmentId());
        data.put("visitDate", CoreMetrics.currentDate());
        data.put("status", ErpMntDaoConstants.VISIT_STATUS_DRAFT);
        data.put("visitType", ErpMntDaoConstants.VISIT_TYPE_RESPONSIVE);
        data.put("assignedTo", request.getAssignedTo() != null ? request.getAssignedTo() : request.getRequestedBy());
        return visitBiz.save(data, context);
    }

    protected void doAccept(ErpMntRequest request, ErpMntVisit visit, IServiceContext context) {
        request.setStatus(ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED);
        updateEntity(request, null, context);
    }

    protected void doStartRepair(ErpMntRequest request, IServiceContext context) {
        request.setStatus(ErpMntDaoConstants.REQUEST_STATUS_IN_PROGRESS);
        updateEntity(request, null, context);
    }

    protected void doComplete(ErpMntRequest request, IServiceContext context) {
        request.setStatus(ErpMntDaoConstants.REQUEST_STATUS_COMPLETED);
        request.setCompletedAt(CoreMetrics.currentTimestamp());
        updateEntity(request, null, context);
    }

    protected void doReject(ErpMntRequest request, IServiceContext context) {
        request.setStatus(ErpMntDaoConstants.REQUEST_STATUS_REJECTED);
        updateEntity(request, null, context);
    }

    protected void doCancel(ErpMntRequest request, IServiceContext context) {
        request.setStatus(ErpMntDaoConstants.REQUEST_STATUS_CANCELLED);
        updateEntity(request, null, context);
    }

    protected NopException illegalRequestTransition(ErpMntRequest request, String current, String expected) {
        return new NopException(ErpMntErrors.ERR_INVALID_REQUEST_STATUS_TRANSITION)
                .param(ErpMntErrors.ARG_REQUEST_CODE, request.getCode())
                .param(ErpMntErrors.ARG_CURRENT_STATUS, current)
                .param(ErpMntErrors.ARG_EXPECTED_STATUS, expected);
    }
}
