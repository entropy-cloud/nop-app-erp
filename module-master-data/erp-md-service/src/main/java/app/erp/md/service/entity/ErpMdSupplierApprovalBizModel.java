
package app.erp.md.service.entity;

import app.erp.md.biz.IErpMdSupplierApprovalBiz;
import app.erp.md.dao.entity.ErpMdSupplierApproval;
import app.erp.md.service.ErpMdConstants;
import app.erp.md.service.ErpMdErrors;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
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
 * 非法迁移抛 {@link ErpMdErrors#ERR_INVALID_APPROVAL_STATUS_TRANSITION}。
 *
 * <p>{@link #suspendByPartner} 为评分 standing=RED 跨域联动入口（purchase→master-data I*Biz，单事务）。
 * DAO 访问走 {@link CrudBizModel#dao()} / {@link #findFirst} 管道（对齐 service-layer 跨实体访问规则）。
 */
@BizModel("ErpMdSupplierApproval")
public class ErpMdSupplierApprovalBizModel extends CrudBizModel<ErpMdSupplierApproval> implements IErpMdSupplierApprovalBiz {

    public ErpMdSupplierApprovalBizModel() {
        setEntityName(ErpMdSupplierApproval.class.getName());
    }

    @Override
    @BizMutation
    public ErpMdSupplierApproval apply(@Name("approvalId") Long approvalId, IServiceContext context) {
        ErpMdSupplierApproval approval = requireApproval(approvalId, context);
        String status = currentStatus(approval);
        if (status != null && !Objects.equals(status, ErpMdConstants.APPROVAL_STATUS_REJECTED)) {
            throw illegalTransition(approval, status, "空 或 REJECTED");
        }
        approval.setStatus(ErpMdConstants.APPROVAL_STATUS_APPLIED);
        updateEntity(approval, null, context);
        return approval;
    }

    @Override
    @BizMutation
    public ErpMdSupplierApproval approve(@Name("approvalId") Long approvalId, IServiceContext context) {
        ErpMdSupplierApproval approval = requireApproval(approvalId, context);
        String status = currentStatus(approval);
        if (status == null || (!Objects.equals(status, ErpMdConstants.APPROVAL_STATUS_APPLIED)
                && !Objects.equals(status, ErpMdConstants.APPROVAL_STATUS_PROBATION))) {
            throw illegalTransition(approval, status, "APPLIED 或 PROBATION");
        }
        requireQualificationValid(approval);
        approval.setStatus(ErpMdConstants.APPROVAL_STATUS_APPROVED);
        approval.setApprovedBy(currentUserId());
        approval.setApprovedAt(CoreMetrics.currentTimestamp());
        updateEntity(approval, null, context);
        return approval;
    }

    @Override
    @BizMutation
    public ErpMdSupplierApproval probate(@Name("approvalId") Long approvalId, IServiceContext context) {
        ErpMdSupplierApproval approval = requireApproval(approvalId, context);
        String status = currentStatus(approval);
        if (status == null || !Objects.equals(status, ErpMdConstants.APPROVAL_STATUS_APPROVED)) {
            throw illegalTransition(approval, status, "APPROVED");
        }
        approval.setStatus(ErpMdConstants.APPROVAL_STATUS_PROBATION);
        updateEntity(approval, null, context);
        return approval;
    }

    @Override
    @BizMutation
    public ErpMdSupplierApproval suspend(@Name("approvalId") Long approvalId, IServiceContext context) {
        ErpMdSupplierApproval approval = requireApproval(approvalId, context);
        return doSuspend(approval, context);
    }

    @Override
    @BizMutation
    public int suspendByPartner(@Name("partnerId") Long partnerId, IServiceContext context) {
        if (partnerId == null) {
            return 0;
        }
        List<ErpMdSupplierApproval> active = findActiveByPartner(partnerId);
        for (ErpMdSupplierApproval approval : active) {
            doSuspend(approval, context);
        }
        return active.size();
    }

    @Override
    @BizMutation
    public ErpMdSupplierApproval reinstate(@Name("approvalId") Long approvalId, IServiceContext context) {
        ErpMdSupplierApproval approval = requireApproval(approvalId, context);
        String status = currentStatus(approval);
        if (status == null || !Objects.equals(status, ErpMdConstants.APPROVAL_STATUS_SUSPENDED)) {
            throw illegalTransition(approval, status, "SUSPENDED");
        }
        approval.setStatus(ErpMdConstants.APPROVAL_STATUS_APPROVED);
        approval.setApprovedBy(currentUserId());
        approval.setApprovedAt(CoreMetrics.currentTimestamp());
        updateEntity(approval, null, context);
        return approval;
    }

    @Override
    @BizMutation
    public ErpMdSupplierApproval reject(@Name("approvalId") Long approvalId, IServiceContext context) {
        ErpMdSupplierApproval approval = requireApproval(approvalId, context);
        String status = currentStatus(approval);
        if (status == null || !Objects.equals(status, ErpMdConstants.APPROVAL_STATUS_APPLIED)) {
            throw illegalTransition(approval, status, "APPLIED");
        }
        approval.setStatus(ErpMdConstants.APPROVAL_STATUS_REJECTED);
        updateEntity(approval, null, context);
        return approval;
    }

    @Override
    @BizQuery
    public ErpMdSupplierApproval findEffectiveByPartner(@Name("partnerId") Long partnerId, IServiceContext context) {
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
        if (status != null && Objects.equals(status, ErpMdConstants.APPROVAL_STATUS_SUSPENDED)) {
            return approval;
        }
        if (status == null || (!Objects.equals(status, ErpMdConstants.APPROVAL_STATUS_APPLIED)
                && !Objects.equals(status, ErpMdConstants.APPROVAL_STATUS_APPROVED)
                && !Objects.equals(status, ErpMdConstants.APPROVAL_STATUS_PROBATION))) {
            throw illegalTransition(approval, status, "APPLIED/APPROVED/PROBATION");
        }
        approval.setStatus(ErpMdConstants.APPROVAL_STATUS_SUSPENDED);
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

    protected List<ErpMdSupplierApproval> findActiveByPartner(Long partnerId) {
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

    protected ErpMdSupplierApproval requireApproval(Long approvalId, IServiceContext context) {
        ErpMdSupplierApproval approval = get(String.valueOf(approvalId), true, context);
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

    protected NopException illegalTransition(ErpMdSupplierApproval approval, String current, String expected) {
        return new NopException(ErpMdErrors.ERR_INVALID_APPROVAL_STATUS_TRANSITION)
                .param(ErpMdErrors.ARG_APPROVAL_ID, approval.getId())
                .param(ErpMdErrors.ARG_CURRENT_STATUS, current)
                .param(ErpMdErrors.ARG_EXPECTED_STATUS, expected);
    }
}
