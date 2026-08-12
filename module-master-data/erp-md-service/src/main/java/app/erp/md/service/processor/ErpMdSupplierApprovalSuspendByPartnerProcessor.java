package app.erp.md.service.processor;

import app.erp.md.dao.entity.ErpMdSupplierApproval;
import app.erp.md.service.ErpMdConstants;
import app.erp.md.service.ErpMdErrors;
import app.erp.md.service.statemachine.ErpMdSupplierApprovalStateMachine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpMdSupplierApproval suspendByPartner per-mutation Processor（R6.9，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 *
 * <p>自包含批量挂起编排：按 partnerId 查询全部有效（APPROVED/PROBATION）资格 → 逐条状态翻转至 SUSPENDED → 返回处理条数。
 * 批量循环内含写（updateEntity），不满足 exemption-registry §判定规则条件 4（无循环副作用），故须拆 Processor
 * （与 R6.7 批量操作拆 Processor 先例一致：HrSalarySimulation.applyBatchAdjustment / HrShiftAssignment.assignBatch 等）。
 *
 * <p>固定来源态/目标态判断委托 {@link ErpMdSupplierApprovalStateMachine}（status 轴 Bean，契约 §4/§7）；
 * 非法边 Bean 抛 common 层码（含 {@code action}/fromStatus 元数据），本 Processor 捕获后映射领域码
 * {@link ErpMdErrors#ERR_INVALID_APPROVAL_STATUS_TRANSITION}（+ approvalId/currentStatus/expectedStatus 实体编号/上下文，
 * common 码作 cause 保留——对齐契约 §7 + M1.1 Option A 范式）。幂等「已 SUSPENDED」短路保留在 Bean 调用前。
 *
 * <p>注：单步 {@code suspend} 入口保留在 {@code ErpMdSupplierApprovalBizModel}；本 Processor 的
 * {@code doSuspend} step 与 BizModel 的 {@code doSuspend} 为同语义的批量 vs 单步副本（per-mutation 自包含要求，
 * 避免与 BizModel 形成循环注入依赖）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpMdSupplierApprovalSuspendByPartnerProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpMdSupplierApprovalStateMachine stateMachine;

    public int suspendByPartner(Long partnerId, IServiceContext context) {
        if (partnerId == null) {
            return 0;
        }
        List<ErpMdSupplierApproval> active = findActiveByPartner(partnerId);
        for (ErpMdSupplierApproval approval : active) {
            doSuspend(approval);
        }
        return active.size();
    }

    protected List<ErpMdSupplierApproval> findActiveByPartner(Long partnerId) {
        IEntityDao<ErpMdSupplierApproval> dao = daoProvider.daoFor(ErpMdSupplierApproval.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("partnerId", partnerId));
        q.addFilter(eq("status", ErpMdConstants.APPROVAL_STATUS_APPROVED));
        List<ErpMdSupplierApproval> list = new ArrayList<>(dao.findAllByQuery(q));

        QueryBean q2 = new QueryBean();
        q2.addFilter(eq("partnerId", partnerId));
        q2.addFilter(eq("status", ErpMdConstants.APPROVAL_STATUS_PROBATION));
        list.addAll(dao.findAllByQuery(q2));
        return list;
    }

    protected void doSuspend(ErpMdSupplierApproval approval) {
        String status = approval.getStatus();
        // 幂等短路：已 SUSPENDED 直接 return（不抛）——保留原位，不进 Bean（Bean 到达此处按非法边报告）
        if (status != null && Objects.equals(status, ErpMdConstants.APPROVAL_STATUS_SUSPENDED)) {
            return;
        }
        try {
            stateMachine.assertCanSuspend(status);
        } catch (NopException e) {
            throw illegalTransition(approval, "APPLIED/APPROVED/PROBATION", e);
        }
        approval.setStatus(stateMachine.suspendTargetStatus());
        daoProvider.daoFor(ErpMdSupplierApproval.class).updateEntity(approval);
    }

    /** 领域非法迁移异常构造；可选 {@code cause} 保留 Bean 抛出的 common 层非法边报告（契约 §7）。 */
    protected NopException illegalTransition(ErpMdSupplierApproval approval, String expected, Throwable cause) {
        return new NopException(ErpMdErrors.ERR_INVALID_APPROVAL_STATUS_TRANSITION, cause)
                .param(ErpMdErrors.ARG_APPROVAL_ID, approval.getId())
                .param(ErpMdErrors.ARG_CURRENT_STATUS, approval.getStatus())
                .param(ErpMdErrors.ARG_EXPECTED_STATUS, expected);
    }
}
