
package app.erp.hr.biz;

import app.erp.hr.dao.entity.ErpHrShiftSwapRequest;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

/**
 * 排班调换申请聚合根 Biz（shift-scheduling.md §五）。除标准 CRUD 外，承载：
 * <ul>
 *   <li>{@link #submit} 提交调换（→PENDING）。</li>
 *   <li>{@link #approve} PENDING→APPROVED，互换双方 assignment 班次 + 记录 swapRequestId + replacedByAssignmentId。</li>
 *   <li>{@link #reject} PENDING→REJECTED。</li>
 *   <li>{@link #cancel} PENDING→CANCELLED。</li>
 * </ul>
 */
public interface IErpHrShiftSwapRequestBiz extends ICrudBiz<ErpHrShiftSwapRequest> {

    @BizMutation
    @SingleSession
    ErpHrShiftSwapRequest submit(@Name("sourceAssignmentId") Long sourceAssignmentId,
                                 @Name("targetAssignmentId") Long targetAssignmentId,
                                 @Name("reason") String reason,
                                 IServiceContext context);

    @BizMutation
    @SingleSession
    ErpHrShiftSwapRequest approve(@Name("swapRequestId") Long swapRequestId, IServiceContext context);

    @BizMutation
    @SingleSession
    ErpHrShiftSwapRequest reject(@Name("swapRequestId") Long swapRequestId, IServiceContext context);

    @BizMutation
    @SingleSession
    ErpHrShiftSwapRequest cancel(@Name("swapRequestId") Long swapRequestId, IServiceContext context);
}
