
package app.erp.pur.service.entity;

import app.erp.pur.biz.ConvertToOrderRequest;
import app.erp.pur.biz.IErpPurRequisitionBiz;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurRequisition;
import app.erp.pur.dao.entity.ErpPurRequisitionLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.annotations.txn.Transactional;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.commons.util.StringHelper;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * 采购请购单 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现三轴审批状态机 + 请购→订单转化
 * （对齐 {@code docs/design/purchase/requisition.md} + {@code docs/design/purchase/state-machine.md}）。
 *
 * <ul>
 *   <li>审核轴：UNSUBMITTED→SUBMITTED→APPROVED/REJECTED，驳回→重提，反审核 APPROVED→REJECTED。</li>
 *   <li>单据轴：任意非终态→docStatus=CANCELLED。</li>
 *   <li>{@link #approve} 仅状态推进（请购无自动下游触发，转化是显式独立动作）。</li>
 *   <li>请购头无供应商，状态机不做供应商启用校验（转化时校验行建议供应商一致性）。</li>
 *   <li>{@link #convertToOrder}：APPROVED 请购 + 调用方补充字段 → 创建 {@link ErpPurOrder}(UNSUBMITTED/DRAFT) + 行，
 *       回链 {@code order.requisitionId}，幂等防重复转化。</li>
 * </ul>
 *
 * <p>状态机迁移校验前置 {@code approveStatus}/{@code docStatus}，违反抛 {@link NopException}。
 */
@BizModel("ErpPurRequisition")
public class ErpPurRequisitionBizModel extends CrudBizModel<ErpPurRequisition> implements IErpPurRequisitionBiz {

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    RequisitionToOrderConverter converter;

    public ErpPurRequisitionBizModel() {
        setEntityName(ErpPurRequisition.class.getName());
    }

    @SingleSession
    @Transactional
    @Override
    public ErpPurRequisition submit(Long requisitionId) {
        ErpPurRequisition req = requireRequisition(requisitionId);
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

    @SingleSession
    @Transactional
    @Override
    public ErpPurRequisition withdrawSubmit(Long requisitionId) {
        ErpPurRequisition req = requireRequisition(requisitionId);
        requireNotCancelled(req);
        Integer status = req.getApproveStatus();
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(req, status, "SUBMITTED");
        }
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        dao().updateEntity(req);
        return req;
    }

    @SingleSession
    @Transactional
    @Override
    public ErpPurRequisition approve(Long requisitionId) {
        ErpPurRequisition req = requireRequisition(requisitionId);
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

    @SingleSession
    @Transactional
    @Override
    public ErpPurRequisition reject(Long requisitionId) {
        ErpPurRequisition req = requireRequisition(requisitionId);
        requireNotCancelled(req);
        Integer status = req.getApproveStatus();
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(req, status, "SUBMITTED");
        }
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(req);
        return req;
    }

    @SingleSession
    @Transactional
    @Override
    public ErpPurRequisition reverseApprove(Long requisitionId) {
        ErpPurRequisition req = requireRequisition(requisitionId);
        Integer status = req.getApproveStatus();
        // 幂等：已 REJECTED 无更多可反审核，直接返回。
        if (status != null && status == ErpPurConstants.APPROVE_STATUS_REJECTED) {
            return req;
        }
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(req, status, "APPROVED");
        }
        // 请购无下游触发，反审核仅状态迁移（反审核目标态 REJECTED，对齐 §3/§11.4）。
        // 若请购已转化为订单，反审核请购的下游影响属独立关注点，本计划不处理。
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(req);
        return req;
    }

    @SingleSession
    @Transactional
    @Override
    public ErpPurRequisition cancel(Long requisitionId) {
        ErpPurRequisition req = requireRequisition(requisitionId);
        Integer docStatus = req.getDocStatus();
        if (docStatus != null && docStatus == ErpPurConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(req, docStatus, "非已作废");
        }
        req.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        dao().updateEntity(req);
        return req;
    }

    // ---------- Phase 2: 请购→订单转化 ----------

    @SingleSession
    @Transactional
    @Override
    public ErpPurOrder convertToOrder(Long requisitionId, ConvertToOrderRequest request) {
        ErpPurRequisition req = requireRequisition(requisitionId);
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
        // (c) 幂等防重复转化：已存在 docStatus≠CANCELLED 且 requisitionId=该请购 的订单
        requireNotAlreadyConverted(requisitionId);
        // (d)(e)(f) 组装并持久化订单 + 行
        ErpPurOrder order = converter.build(req, lines, supplierId, request);
        daoFor(ErpPurOrder.class).saveEntity(order);
        // flush 使订单 ID 落地，再保存行（行 orderId 依赖头 ID）
        ormTemplate.flushSession();
        for (ErpPurOrderLine orderLine : converter.buildLines(order, lines, request)) {
            daoFor(ErpPurOrderLine.class).saveEntity(orderLine);
        }
        return order;
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

    /**
     * 幂等防重复转化：查询 {@code ErpPurOrder} where {@code requisitionId=该请购 AND docStatus≠CANCELLED}；
     * 存在则抛 {@link ErpPurErrors#ERR_REQ_ALREADY_CONVERTED}（纯查询，无 ORM FK 改动，requisitionId 列已存在）。
     * 原订单作废（docStatus=CANCELLED）后允许重新转化。
     */
    private void requireNotAlreadyConverted(Long requisitionId) {
        ormTemplate.flushSession();
        IEntityDao<ErpPurOrder> dao = daoFor(ErpPurOrder.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("requisitionId", requisitionId),
                ne("docStatus", ErpPurConstants.DOC_STATUS_CANCELLED)));
        if (!dao.findAllByQuery(q).isEmpty()) {
            throw new NopException(ErpPurErrors.ERR_REQ_ALREADY_CONVERTED)
                    .param(ErpPurErrors.ARG_REQUISITION_ID, requisitionId);
        }
    }

    // ---------- validation helpers ----------

    ErpPurRequisition requireRequisition(Long requisitionId) {
        ErpPurRequisition req = dao().getEntityById(requisitionId);
        if (req == null) {
            throw new NopException(ErpPurErrors.ERR_REQ_NOT_FOUND)
                    .param(ErpPurErrors.ARG_REQUISITION_ID, requisitionId);
        }
        return req;
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
