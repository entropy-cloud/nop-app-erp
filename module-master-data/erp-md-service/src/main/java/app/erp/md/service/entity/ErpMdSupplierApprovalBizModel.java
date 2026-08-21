
package app.erp.md.service.entity;

import app.erp.md.biz.IErpMdSupplierApprovalBiz;
import app.erp.md.dao.entity.ErpMdSupplierApproval;
import app.erp.md.service.ErpMdConstants;
import app.erp.md.service.ErpMdErrors;
import app.erp.md.service.daterange.ErpDateRangeOverlapValidator;
import app.erp.md.service.processor.ErpMdSupplierApprovalSuspendByPartnerProcessor;
import app.erp.md.service.statemachine.ErpMdSupplierApprovalStateMachine;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 供应商准入资格（AVL）BizModel。承载 6 态状态机（apply/approve/probate/suspend/reinstate/reject）。
 *
 * <p>每个动作 = 单步状态推进（校验迁移 + 校验业务规则 + 执行），不构成多步编排，
 * 故不拆 Processor（对齐 {@code nop-backend-dev} 反模式表「不要为单步操作强行拆 Processor」）。
 * 固定来源态/目标态判断下沉 {@link ErpMdSupplierApprovalStateMachine} Bean（plan 2026-08-12-2142-1 M2.1，
 * 契约 {@code docs/architecture/entity-state-machine-bean.md}），非法迁移 Bean 抛 common 层码，
 * 本类映射为领域 {@link ErpMdErrors#ERR_INVALID_APPROVAL_STATUS_TRANSITION}（common 码作 cause 保留）。
 *
 * <p>{@link #suspendByPartner} 为评分 standing=RED 跨域联动入口（purchase→master-data I*Biz，单事务）。
 * DAO 访问走 {@link CrudBizModel#dao()} / {@link #findFirst} 管道（对齐 service-layer 跨实体访问规则）。
 *
 * <p>C3 日期范围有效性模式试点（docs/design/date-ranged-validity-pattern.md §6/§7）：扩展
 * {@code defaultPrepareSave} / {@code defaultPrepareUpdate} 钩子，对同 partnerId 维度做区间互斥校验
 * （MUTEX 策略）。{@code status=REJECTED} 的记录视为「已废弃」不参与互斥（业务上 REJECTED 不生效）。
 */
@BizModel("ErpMdSupplierApproval")
public class ErpMdSupplierApprovalBizModel extends CrudBizModel<ErpMdSupplierApproval> implements IErpMdSupplierApprovalBiz {

    public ErpMdSupplierApprovalBizModel() {
        setEntityName(ErpMdSupplierApproval.class.getName());
    }

    @Inject
    ErpMdSupplierApprovalSuspendByPartnerProcessor suspendByPartnerProcessor;

    @Inject
    ErpMdSupplierApprovalStateMachine stateMachine;

    @Override
    protected void defaultPrepareSave(EntityData<ErpMdSupplierApproval> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        enforceNoOverlapIfEffective(entityData.getEntity());
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpMdSupplierApproval> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        enforceNoOverlapIfEffective(entityData.getEntity());
    }

    /**
     * 同 partnerId 区间互斥校验（仅当 status != REJECTED 时生效）。
     *
     * <p>REJECTED 记录视为「已废弃」不参与互斥（业务上不生效）；其他 5 态（APPLIED/APPROVED/PROBATION/SUSPENDED
     * + null）视为有效记录参与区间检查。同 partnerId 同一时刻至多 1 条有效 AVL 资格。
     */
    protected void enforceNoOverlapIfEffective(ErpMdSupplierApproval entity) {
        if (entity == null) {
            return;
        }
        String status = entity.getStatus();
        if (Objects.equals(status, ErpMdConstants.APPROVAL_STATUS_REJECTED)) {
            return;
        }
        QueryBean query = new QueryBean();
        query.addFilter(eq("partnerId", entity.getPartnerId()));
        List<ErpMdSupplierApproval> samePartner = dao().findAllByQuery(query);
        // 排除同 partnerId 下所有 REJECTED 记录（不参与互斥）
        List<ErpMdSupplierApproval> effective = new ArrayList<>();
        for (ErpMdSupplierApproval other : samePartner) {
            String s = other.getStatus();
            if (!Objects.equals(s, ErpMdConstants.APPROVAL_STATUS_REJECTED)) {
                effective.add(other);
            }
        }
        ErpDateRangeOverlapValidator.enforceMutex(
                entity, effective, ErpMdErrors.ERR_MD_DATE_RANGE_OVERLAP, entity.getId());
    }

    @Override
    @BizMutation
    public ErpMdSupplierApproval apply(@Name("approvalId") String approvalId, IServiceContext context) {
        ErpMdSupplierApproval approval = requireApproval(approvalId, context);
        String status = currentStatus(approval);
        try {
            stateMachine.assertCanApply(status);
        } catch (NopException e) {
            throw illegalTransition(approval, "空 或 REJECTED", e);
        }
        approval.setStatus(stateMachine.applyTargetStatus());
        updateEntity(approval, null, context);
        return approval;
    }

    @Override
    @BizMutation
    public ErpMdSupplierApproval approve(@Name("approvalId") String approvalId, IServiceContext context) {
        ErpMdSupplierApproval approval = requireApproval(approvalId, context);
        String status = currentStatus(approval);
        try {
            stateMachine.assertCanApprove(status);
        } catch (NopException e) {
            throw illegalTransition(approval, "APPLIED 或 PROBATION", e);
        }
        requireQualificationValid(approval);
        approval.setStatus(stateMachine.approveTargetStatus());
        approval.setApprovedBy(currentUserId());
        approval.setApprovedAt(CoreMetrics.currentTimestamp());
        updateEntity(approval, null, context);
        return approval;
    }

    @Override
    @BizMutation
    public ErpMdSupplierApproval probate(@Name("approvalId") String approvalId, IServiceContext context) {
        ErpMdSupplierApproval approval = requireApproval(approvalId, context);
        String status = currentStatus(approval);
        try {
            stateMachine.assertCanProbate(status);
        } catch (NopException e) {
            throw illegalTransition(approval, "APPROVED", e);
        }
        approval.setStatus(stateMachine.probateTargetStatus());
        updateEntity(approval, null, context);
        return approval;
    }

    @Override
    @BizMutation
    public ErpMdSupplierApproval suspend(@Name("approvalId") String approvalId, IServiceContext context) {
        ErpMdSupplierApproval approval = requireApproval(approvalId, context);
        return doSuspend(approval, context);
    }

    @Override
    @BizMutation
    public int suspendByPartner(@Name("partnerId") String partnerId, IServiceContext context) {
        return suspendByPartnerProcessor.suspendByPartner(partnerId, context);
    }

    @Override
    @BizMutation
    public ErpMdSupplierApproval reinstate(@Name("approvalId") String approvalId, IServiceContext context) {
        ErpMdSupplierApproval approval = requireApproval(approvalId, context);
        String status = currentStatus(approval);
        try {
            stateMachine.assertCanReinstate(status);
        } catch (NopException e) {
            throw illegalTransition(approval, "SUSPENDED", e);
        }
        approval.setStatus(stateMachine.reinstateTargetStatus());
        approval.setApprovedBy(currentUserId());
        approval.setApprovedAt(CoreMetrics.currentTimestamp());
        updateEntity(approval, null, context);
        return approval;
    }

    @Override
    @BizMutation
    public ErpMdSupplierApproval reject(@Name("approvalId") String approvalId, IServiceContext context) {
        ErpMdSupplierApproval approval = requireApproval(approvalId, context);
        String status = currentStatus(approval);
        try {
            stateMachine.assertCanReject(status);
        } catch (NopException e) {
            throw illegalTransition(approval, "APPLIED", e);
        }
        approval.setStatus(stateMachine.rejectTargetStatus());
        updateEntity(approval, null, context);
        return approval;
    }

    @Override
    @BizQuery
    public ErpMdSupplierApproval findEffectiveByPartner(@Name("partnerId") String partnerId, IServiceContext context) {
        if (partnerId == null) {
            return null;
        }
        // status 字段为字典类型，xmeta 仅允许 eq/in 过滤（不支持 ne，见 ErpPurOrderBizModel 同类约束），
        // 故按 partnerId 取全部后在内存剔除 REJECTED，取第一条有效资格。
        QueryBean q = new QueryBean();
        q.addFilter(eq("partnerId", partnerId));
        for (ErpMdSupplierApproval approval : findList(q, null, context)) {
            String status = currentStatus(approval);
            if (status == null || !Objects.equals(status, ErpMdConstants.APPROVAL_STATUS_REJECTED)) {
                return approval;
            }
        }
        return null;
    }

    // ---------- 内部步骤 ----------

    protected ErpMdSupplierApproval doSuspend(ErpMdSupplierApproval approval, IServiceContext context) {
        String status = currentStatus(approval);
        // 幂等短路：已 SUSPENDED 直接 return（不抛）——保留原位，不进 Bean（Bean 到达此处按非法边报告）
        if (status != null && Objects.equals(status, ErpMdConstants.APPROVAL_STATUS_SUSPENDED)) {
            return approval;
        }
        try {
            stateMachine.assertCanSuspend(status);
        } catch (NopException e) {
            throw illegalTransition(approval, "APPLIED/APPROVED/PROBATION", e);
        }
        approval.setStatus(stateMachine.suspendTargetStatus());
        updateEntity(approval, null, context);
        return approval;
    }

    protected void requireQualificationValid(ErpMdSupplierApproval approval) {
        boolean hasDoc = approval.getQualificationDoc() != null && !approval.getQualificationDoc().isEmpty();
        LocalDate from = approval.getValidFrom();
        LocalDate to = approval.getValidTo();
        if (!hasDoc || from == null || to == null || !to.isAfter(from)) {
            throw new NopException(ErpMdErrors.ERR_APPROVAL_QUALIFICATION_MISSING)
                    .param(ErpMdErrors.ARG_APPROVAL_ID, approval.getId());
        }
    }

    protected List<ErpMdSupplierApproval> findActiveByPartner(String partnerId) {
        IEntityDao<ErpMdSupplierApproval> dao = dao();
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

    protected ErpMdSupplierApproval requireApproval(String approvalId, IServiceContext context) {
        ErpMdSupplierApproval approval = get(approvalId, true, context);
        if (approval == null) {
            throw new NopException(ErpMdErrors.ERR_APPROVAL_NOT_FOUND)
                    .param(ErpMdErrors.ARG_APPROVAL_ID, approvalId);
        }
        return approval;
    }

    protected String currentStatus(ErpMdSupplierApproval approval) {
        return approval.getStatus();
    }

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 领域非法迁移异常构造。可选 {@code cause} 保留 Bean 抛出的 common 层非法边报告（契约 §7：
     * Bean 报 common 码 + action/fromStatus 元数据，BizModel/Processor 映射领域码 + 实体编号/上下文，common 码作 cause 保留）。
     */
    protected NopException illegalTransition(ErpMdSupplierApproval approval, String expected, Throwable cause) {
        return new NopException(ErpMdErrors.ERR_INVALID_APPROVAL_STATUS_TRANSITION, cause)
                .param(ErpMdErrors.ARG_APPROVAL_ID, approval.getId())
                .param(ErpMdErrors.ARG_CURRENT_STATUS, currentStatus(approval))
                .param(ErpMdErrors.ARG_EXPECTED_STATUS, expected);
    }
}
