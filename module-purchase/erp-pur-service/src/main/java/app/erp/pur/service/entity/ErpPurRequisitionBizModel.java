
package app.erp.pur.service.entity;

import app.erp.pur.biz.ConvertToOrderRequest;
import app.erp.pur.biz.IErpPurOrderBiz;
import app.erp.pur.biz.IErpPurRequisitionBiz;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurRequisition;
import app.erp.pur.dao.entity.ErpPurRequisitionLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import io.nop.core.context.IServiceContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 采购请购单 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现三轴审批状态机 + 请购→订单转化
 * （对齐 {@code docs/design/purchase/requisition.md} + {@code docs/design/purchase/state-machine.md}）。
 *
 * <p>请购→订单转化为跨聚合写：经 {@link IErpPurOrderBiz} 的 {@code createFromRequisition}/{@code existsActiveByRequisition}
 * 委托订单聚合（订单/行的组装与持久化归订单侧，请购侧仅做 APPROVED 校验、供应商一致性校验与幂等防重）。
 *
 * <p>状态机迁移校验前置 {@code approveStatus}/{@code docStatus}，违反抛 {@link NopException}。
 */
@BizModel("ErpPurRequisition")
public class ErpPurRequisitionBizModel extends CrudBizModel<ErpPurRequisition> implements IErpPurRequisitionBiz {

    @Inject
    IErpPurOrderBiz orderBiz;

    public ErpPurRequisitionBizModel() {
        setEntityName(ErpPurRequisition.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurRequisition submit(@Name("requisitionId") Long requisitionId, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(requisitionId, context);
        requireNotCancelled(req);
        Integer status = req.getApproveStatus();
        if (status == null) {
            status = ErpPurConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (status != ErpPurConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpPurConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(req, status, "UNSUBMITTED 或 REJECTED");
        }
        requireLinesNonEmpty(req);
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        dao().updateEntity(req);
        return req;
    }

    @Override
    @BizMutation
    public ErpPurRequisition withdrawSubmit(@Name("requisitionId") Long requisitionId, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(requisitionId, context);
        requireNotCancelled(req);
        Integer status = req.getApproveStatus();
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(req, status, "SUBMITTED");
        }
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        dao().updateEntity(req);
        return req;
    }

    @Override
    @BizMutation
    public ErpPurRequisition approve(@Name("requisitionId") Long requisitionId, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(requisitionId, context);
        Integer status = req.getApproveStatus();
        // 幂等：已审核请购再次审核为空操作（state-machine §4）。
        if (status != null && status == ErpPurConstants.APPROVE_STATUS_APPROVED) {
            return req;
        }
        requireNotCancelled(req);
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(req, status, "SUBMITTED");
        }
        // 请购 approve 仅状态推进（请购无自动下游触发，转化是显式独立动作，对齐 requisition.md）。
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        req.setApprovedBy(currentUserId());
        req.setApprovedAt(CoreMetrics.currentDateTime());
        dao().updateEntity(req);
        return req;
    }

    @Override
    @BizMutation
    public ErpPurRequisition reject(@Name("requisitionId") Long requisitionId, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(requisitionId, context);
        requireNotCancelled(req);
        Integer status = req.getApproveStatus();
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(req, status, "SUBMITTED");
        }
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(req);
        return req;
    }

    @Override
    @BizMutation
    public ErpPurRequisition reverseApprove(@Name("requisitionId") Long requisitionId, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(requisitionId, context);
        Integer status = req.getApproveStatus();
        // 幂等：已 REJECTED 无更多可反审核，直接返回。
        if (status != null && status == ErpPurConstants.APPROVE_STATUS_REJECTED) {
            return req;
        }
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(req, status, "APPROVED");
        }
        // 请购无下游触发，反审核仅状态迁移（反审核目标态 REJECTED，对齐 §3/§11.4）。
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(req);
        return req;
    }

    @Override
    @BizMutation
    public ErpPurRequisition cancel(@Name("requisitionId") Long requisitionId, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(requisitionId, context);
        Integer docStatus = req.getDocStatus();
        if (docStatus != null && docStatus == ErpPurConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(req, docStatus, "非已作废");
        }
        req.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        dao().updateEntity(req);
        return req;
    }

    // ---------- 请购→订单转化（跨聚合写委托 IErpPurOrderBiz） ----------

    @Override
    @BizMutation
    public ErpPurOrder convertToOrder(@Name("requisitionId") Long requisitionId, @Name("request") ConvertToOrderRequest request, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(requisitionId, context);
        Integer status = req.getApproveStatus();
        // (a) 仅 APPROVED 请购可转化
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_APPROVED) {
            throw new NopException(ErpPurErrors.ERR_REQ_NOT_APPROVED)
                    .param(ErpPurErrors.ARG_REQUISITION_CODE, req.getCode())
                    .param(ErpPurErrors.ARG_CURRENT_STATUS, status);
        }
        List<ErpPurRequisitionLine> lines = loadLines(requisitionId);
        if (lines.isEmpty()) {
            throw new NopException(ErpPurErrors.ERR_REQ_LINES_EMPTY)
                    .param(ErpPurErrors.ARG_REQUISITION_CODE, req.getCode());
        }
        // (b) 供应商一致性约束（单请购单供应商，MVP）
        Long supplierId = requireConsistentSupplier(req, lines);
        // (c) 幂等防重复转化：订单侧查既有 docStatus≠CANCELLED 且 requisitionId 命中
        if (orderBiz.existsActiveByRequisition(requisitionId, context)) {
            throw new NopException(ErpPurErrors.ERR_REQ_ALREADY_CONVERTED)
                    .param(ErpPurErrors.ARG_REQUISITION_ID, requisitionId);
        }
        // (d)(e)(f) 组装并持久化订单 + 行（委托订单聚合）
        return orderBiz.createFromRequisition(req, lines, supplierId, request, context);
    }

    /**
     * 校验所有请购行 {@code suggestedSupplierId} 非空且一致；不一致或缺失抛
     * {@link ErpPurErrors#ERR_REQ_MIXED_OR_MISSING_SUPPLIER}。一致供应商作为订单 supplierId。
     */
    private Long requireConsistentSupplier(ErpPurRequisition req, List<ErpPurRequisitionLine> lines) {
        Set<Long> suppliers = new HashSet<>();
        for (ErpPurRequisitionLine line : lines) {
            if (line.getSuggestedSupplierId() == null) {
                throw new NopException(ErpPurErrors.ERR_REQ_MIXED_OR_MISSING_SUPPLIER)
                        .param(ErpPurErrors.ARG_REQUISITION_CODE, req.getCode());
            }
            suppliers.add(line.getSuggestedSupplierId());
        }
        if (suppliers.size() != 1) {
            throw new NopException(ErpPurErrors.ERR_REQ_MIXED_OR_MISSING_SUPPLIER)
                    .param(ErpPurErrors.ARG_REQUISITION_CODE, req.getCode());
        }
        return suppliers.iterator().next();
    }

    // ---------- validation helpers ----------

    ErpPurRequisition requireRequisition(Long requisitionId, IServiceContext context) {
        return requireEntity(String.valueOf(requisitionId), null, context);
    }

    void requireNotCancelled(ErpPurRequisition req) {
        Integer docStatus = req.getDocStatus();
        if (docStatus != null && docStatus == ErpPurConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(req, docStatus, "非已作废");
        }
    }

    void requireLinesNonEmpty(ErpPurRequisition req) {
        if (loadLines(req.getId()).isEmpty()) {
            throw new NopException(ErpPurErrors.ERR_REQ_LINES_EMPTY)
                    .param(ErpPurErrors.ARG_REQUISITION_CODE, req.getCode());
        }
    }

    // ---------- query helpers ----------

    List<ErpPurRequisitionLine> loadLines(Long requisitionId) {
        // D2 边界场景：同聚合子表加载，父实体已由 requireEntity 经数据权限/Meta 管道授权，子行无独立权限规则。
        IEntityDao<ErpPurRequisitionLine> dao = daoFor(ErpPurRequisitionLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("requisitionId", requisitionId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    // ---------- misc helpers ----------

    private String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            if (ctx == null) {
                return null;
            }
            return ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private NopException illegalTransition(ErpPurRequisition req, Integer current, String expected) {
        return new NopException(ErpPurErrors.ERR_REQ_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_REQUISITION_CODE, req.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, expected);
    }

    private NopException illegalDocTransition(ErpPurRequisition req, Integer current, String expected) {
        return new NopException(ErpPurErrors.ERR_REQ_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_REQUISITION_CODE, req.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
