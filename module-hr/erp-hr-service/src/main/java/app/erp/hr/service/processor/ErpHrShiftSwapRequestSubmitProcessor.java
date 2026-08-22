package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import app.erp.hr.dao.entity.ErpHrShiftSwapRequest;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;

/**
 * ErpHrShiftSwapRequest submit per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含调换申请提交：加载源/目标 assignment（一对一交换），新建 PENDING 调换申请（shift-scheduling.md §5.1/§5.2）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrShiftSwapRequestProcessor}。
 */
public class ErpHrShiftSwapRequestSubmitProcessor extends AbstractErpHrShiftSwapRequestProcessor {

    public ErpHrShiftSwapRequest submit(String sourceAssignmentId, String targetAssignmentId, String reason,
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
        ErpHrShiftSwapRequest req = swapRequestDao().newEntity();
        req.setBusinessDate(CoreMetrics.today());
        req.setCode("SWAP-" + source.getId() + "-" + CoreMetrics.nanoTime());
        req.setRequesterId(source.getEmployeeId());
        req.setTargetEmployeeId(target.getEmployeeId());
        req.setSourceAssignmentId(source.getId());
        req.setTargetAssignmentId(target.getId());
        req.setSwapDate(source.getAssignmentDate());
        req.setReason(reason);
        req.setStatus(ErpHrConstants.SWAP_STATUS_PENDING);
        swapRequestDao().saveEntity(req);
        return req;
    }
}
