
package app.erp.hr.service.entity;

import app.erp.hr.biz.IErpHrShiftAssignmentBiz;
import app.erp.hr.biz.IErpHrShiftSwapRequestBiz;
import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import app.erp.hr.dao.entity.ErpHrShiftSwapRequest;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.time.LocalDate;

/**
 * 排班调换审批 BizModel（shift-scheduling.md §五）。继承 {@link CrudBizModel} 标准 CRUD，
 * 扩展 PENDING→APPROVED/REJECTED/CANCELLED 状态机；APPROVED 时双方 assignment 互换班次并记录 swapRequestId。
 */
@BizModel("ErpHrShiftSwapRequest")
public class ErpHrShiftSwapRequestBizModel extends CrudBizModel<ErpHrShiftSwapRequest>
        implements IErpHrShiftSwapRequestBiz {

    @Inject
    IErpHrShiftAssignmentBiz assignmentBiz;

    public ErpHrShiftSwapRequestBizModel() {
        setEntityName(ErpHrShiftSwapRequest.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrShiftSwapRequest submit(@Name("sourceAssignmentId") Long sourceAssignmentId,
                                        @Name("targetAssignmentId") Long targetAssignmentId,
                                        @Name("reason") String reason,
                                        IServiceContext context) {
        ErpHrShiftAssignment source = assignmentBiz.requireEntity(String.valueOf(sourceAssignmentId), null, context);
        ErpHrShiftAssignment target = targetAssignmentId != null
                ? assignmentBiz.requireEntity(String.valueOf(targetAssignmentId), null, context)
                : null;
        // 目标员工目标日须有 assignment（一对一交换，shift-scheduling.md §5.2）
        if (target == null) {
            throw new NopException(ErpHrErrors.ERR_SHIFT_SWAP_TARGET_OCCUPIED)
                    .param(ErpHrErrors.ARG_ASSIGNMENT_DATE, source.getAssignmentDate());
        }
        ErpHrShiftSwapRequest req = newEntity();
        req.setCode("SWAP-" + source.getId() + "-" + System.nanoTime());
        req.setRequesterId(source.getEmployeeId());
        req.setTargetEmployeeId(target.getEmployeeId());
        req.setSourceAssignmentId(source.getId());
        req.setTargetAssignmentId(target.getId());
        req.setSwapDate(source.getAssignmentDate());
        req.setReason(reason);
        req.setStatus(ErpHrConstants.SWAP_STATUS_PENDING);
        saveEntity(req, null, context);
        return req;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrShiftSwapRequest approve(@Name("swapRequestId") Long swapRequestId, IServiceContext context) {
        ErpHrShiftSwapRequest req = requireEntity(String.valueOf(swapRequestId), null, context);
        assertTransition(req, ErpHrConstants.SWAP_STATUS_PENDING, ErpHrConstants.SWAP_STATUS_APPROVED);
        ErpHrShiftAssignment source = assignmentBiz.requireEntity(
                String.valueOf(req.getSourceAssignmentId()), null, context);
        ErpHrShiftAssignment target = req.getTargetAssignmentId() != null
                ? assignmentBiz.requireEntity(String.valueOf(req.getTargetAssignmentId()), null, context)
                : null;
        if (target == null) {
            throw new NopException(ErpHrErrors.ERR_SHIFT_ASSIGNMENT_NOT_SWAPPABLE)
                    .param(ErpHrErrors.ARG_SWAP_REQUEST_ID, swapRequestId);
        }
        // 互换班次（shift-scheduling.md §5.2）
        Long sourceShiftId = source.getShiftId();
        source.setShiftId(target.getShiftId());
        target.setShiftId(sourceShiftId);
        // 记录 swapRequestId + replacedByAssignmentId 双向追溯
        source.setSwapRequestId(req.getId());
        target.setSwapRequestId(req.getId());
        source.setReplacedByAssignmentId(target.getId());
        target.setReplacedByAssignmentId(source.getId());
        // 重新置为 SCHEDULED，等待下次 calcAttendance 按新排班重算
        source.setStatus(ErpHrConstants.ASSIGNMENT_STATUS_SCHEDULED);
        target.setStatus(ErpHrConstants.ASSIGNMENT_STATUS_SCHEDULED);
        IEntityDao<ErpHrShiftAssignment> dao = daoProvider().daoFor(ErpHrShiftAssignment.class);
        dao.updateEntity(source);
        dao.updateEntity(target);
        req.setStatus(ErpHrConstants.SWAP_STATUS_APPROVED);
        req.setApprovedById(context.getUserId());
        updateEntity(req, null, context);
        return req;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrShiftSwapRequest reject(@Name("swapRequestId") Long swapRequestId, IServiceContext context) {
        ErpHrShiftSwapRequest req = requireEntity(String.valueOf(swapRequestId), null, context);
        assertTransition(req, ErpHrConstants.SWAP_STATUS_PENDING, ErpHrConstants.SWAP_STATUS_REJECTED);
        req.setStatus(ErpHrConstants.SWAP_STATUS_REJECTED);
        updateEntity(req, null, context);
        return req;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrShiftSwapRequest cancel(@Name("swapRequestId") Long swapRequestId, IServiceContext context) {
        ErpHrShiftSwapRequest req = requireEntity(String.valueOf(swapRequestId), null, context);
        assertTransition(req, ErpHrConstants.SWAP_STATUS_PENDING, ErpHrConstants.SWAP_STATUS_CANCELLED);
        req.setStatus(ErpHrConstants.SWAP_STATUS_CANCELLED);
        updateEntity(req, null, context);
        return req;
    }

    // ---------- helpers ----------

    void assertTransition(ErpHrShiftSwapRequest req, String expectedFrom, String targetTo) {
        String current = req.getStatus();
        if (current == null || !current.equals(expectedFrom)) {
            throw new NopException(ErpHrErrors.ERR_SHIFT_SWAP_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpHrErrors.ARG_SWAP_REQUEST_ID, req.getId())
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, current)
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, targetTo);
        }
    }
}
