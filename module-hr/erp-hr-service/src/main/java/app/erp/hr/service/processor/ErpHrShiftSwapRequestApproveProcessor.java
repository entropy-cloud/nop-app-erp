package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import app.erp.hr.dao.entity.ErpHrShiftSwapRequest;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;

/**
 * ErpHrShiftSwapRequest approve per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含调换审批通过：PENDING→APPROVED，互换双方 assignment 班次并记录 swapRequestId + replacedByAssignmentId 双向追溯，
 * 重新置 SCHEDULED 等待 calcAttendance 重算（shift-scheduling.md §5.2）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrShiftSwapRequestProcessor}。
 */
public class ErpHrShiftSwapRequestApproveProcessor extends AbstractErpHrShiftSwapRequestProcessor {

    public ErpHrShiftSwapRequest approve(Long swapRequestId, IServiceContext context) {
        ErpHrShiftSwapRequest req = requireSwapRequest(String.valueOf(swapRequestId), context);
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
        IEntityDao<ErpHrShiftAssignment> dao = daoProvider.daoFor(ErpHrShiftAssignment.class);
        dao.updateEntity(source);
        dao.updateEntity(target);
        req.setStatus(ErpHrConstants.SWAP_STATUS_APPROVED);
        req.setApprovedById(context.getUserId());
        swapRequestDao().updateEntity(req);
        return req;
    }
}
