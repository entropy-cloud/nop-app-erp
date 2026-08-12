package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrLeaveRequest;
import io.nop.core.context.IServiceContext;

/**
 * ErpHrLeaveRequest submit per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 DRAFT→SUBMITTED 提交编排：状态守卫 + 天数计算 + 余额校验 + 日期重叠校验（UC-HR-02）。
 *
 * <p>固定来源态/目标态判断委托 {@link app.erp.hr.service.statemachine.ErpHrLeaveRequestStateMachine}
 * （Bean 矩阵权威，契约 §4/§7）；非法边由 {@link #assertCanSubmit} 映射领域码。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 * 共享 helper 单一真相源在 {@link AbstractErpHrLeaveRequestProcessor}。
 */
public class ErpHrLeaveRequestSubmitProcessor extends AbstractErpHrLeaveRequestProcessor {

    public ErpHrLeaveRequest submit(String id, IServiceContext context) {
        ErpHrLeaveRequest leave = requireLeave(id);
        assertCanSubmit(leave);
        computeDurationDays(leave);
        checkLeaveBalance(leave, context);
        checkDateOverlap(leave, false, context);
        leave.setStatus(stateMachine.submitTargetStatus());
        leaveRequestDao().updateEntity(leave);
        return leave;
    }
}
