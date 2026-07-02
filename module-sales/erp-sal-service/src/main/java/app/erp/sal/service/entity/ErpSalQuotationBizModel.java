
package app.erp.sal.service.entity;

import app.erp.sal.biz.IErpSalOrderBiz;
import app.erp.sal.biz.IErpSalQuotationBiz;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalQuotation;
import app.erp.sal.dao.entity.ErpSalQuotationLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import io.nop.core.context.IServiceContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 销售报价单 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现审核/客户确认状态机 + 报价→订单转化
 * （对齐 {@code docs/design/sales/quotation.md}）。
 *
 * <p>报价→订单转化为跨聚合写：经 {@link IErpSalOrderBiz} 的 {@code createFromQuotation}/{@code existsActiveByQuotation}
 * 委托订单聚合（订单/行的组装与持久化归订单侧，报价侧仅做 APPROVED+已确认校验、过期校验与幂等防重）。
 *
 * <p>模型边界：报价单无 {@code approvedBy}/{@code approvedAt} 列——审核仅翻转 {@code approveStatus}，
 * 不记录审核人/时间。「EXPIRED」不在持久化（无列），由 {@code validTo < today} 在确认/转化时派生校验。
 */
@BizModel("ErpSalQuotation")
public class ErpSalQuotationBizModel extends CrudBizModel<ErpSalQuotation> implements IErpSalQuotationBiz {

    @Inject
    IErpSalOrderBiz orderBiz;

    public ErpSalQuotationBizModel() {
        setEntityName(ErpSalQuotation.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalQuotation submit(@Name("quotationId") Long quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        requireNotCancelled(quotation);
        Integer status = quotation.getApproveStatus();
        if (status == null) {
            status = ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (status != ErpSalConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpSalConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(quotation, status, "UNSUBMITTED 或 REJECTED");
        }
        requireLinesNonEmpty(quotation);
        quotation.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        dao().updateEntity(quotation);
        return quotation;
    }

    @Override
    @BizMutation
    public ErpSalQuotation withdrawSubmit(@Name("quotationId") Long quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        requireNotCancelled(quotation);
        Integer status = quotation.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(quotation, status, "SUBMITTED");
        }
        quotation.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        dao().updateEntity(quotation);
        return quotation;
    }

    @Override
    @BizMutation
    public ErpSalQuotation approve(@Name("quotationId") Long quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        Integer status = quotation.getApproveStatus();
        // 幂等：已审核再次审核为空操作。
        if (status != null && status == ErpSalConstants.APPROVE_STATUS_APPROVED) {
            return quotation;
        }
        requireNotCancelled(quotation);
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(quotation, status, "SUBMITTED");
        }
        // 模型边界：无 approvedBy/approvedAt 列，审核仅翻转 approveStatus。
        quotation.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        dao().updateEntity(quotation);
        return quotation;
    }

    @Override
    @BizMutation
    public ErpSalQuotation reject(@Name("quotationId") Long quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        requireNotCancelled(quotation);
        Integer status = quotation.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(quotation, status, "SUBMITTED");
        }
        quotation.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(quotation);
        return quotation;
    }

    @Override
    @BizMutation
    public ErpSalQuotation reverseApprove(@Name("quotationId") Long quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        Integer status = quotation.getApproveStatus();
        // 幂等：已 REJECTED 直接返回。
        if (status != null && status == ErpSalConstants.APPROVE_STATUS_REJECTED) {
            return quotation;
        }
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(quotation, status, "APPROVED");
        }
        quotation.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(quotation);
        return quotation;
    }

    @Override
    @BizMutation
    public ErpSalQuotation cancel(@Name("quotationId") Long quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        Integer docStatus = quotation.getDocStatus();
        if (docStatus != null && docStatus == ErpSalConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(quotation, docStatus, "非已作废");
        }
        quotation.setDocStatus(ErpSalConstants.DOC_STATUS_CANCELLED);
        dao().updateEntity(quotation);
        return quotation;
    }

    @Override
    @BizMutation
    public ErpSalQuotation confirmCustomerAccepted(@Name("quotationId") Long quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        requireNotCancelled(quotation);
        Integer status = quotation.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(quotation, status, "APPROVED");
        }
        requireNotExpired(quotation);
        quotation.setIsAccepted(true);
        dao().updateEntity(quotation);
        return quotation;
    }

    @Override
    @BizMutation
    public ErpSalOrder convertToOrder(@Name("quotationId") Long quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        requireNotCancelled(quotation);
        Integer status = quotation.getApproveStatus();
        boolean accepted = Boolean.TRUE.equals(quotation.getIsAccepted());
        // 转化前置：APPROVED + isAccepted（否则 ERR_QUOTATION_NOT_READY）。
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_APPROVED || !accepted) {
            throw new NopException(ErpSalErrors.ERR_QUOTATION_NOT_READY)
                    .param(ErpSalErrors.ARG_QUOTATION_CODE, quotation.getCode())
                    .param(ErpSalErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpSalErrors.ARG_IS_ACCEPTED, accepted);
        }
        // 过期报价不可转化。
        requireNotExpired(quotation);
        // 幂等防重复转化：订单侧查既有 docStatus≠CANCELLED 且 quotationId 命中 → 拒绝。
        if (orderBiz.existsActiveByQuotation(quotationId, context)) {
            throw new NopException(ErpSalErrors.ERR_QUOTATION_ALREADY_CONVERTED)
                    .param(ErpSalErrors.ARG_QUOTATION_CODE, quotation.getCode());
        }

        List<ErpSalQuotationLine> quotationLines = loadLines(quotationId);
        ErpSalOrder order = orderBiz.createFromQuotation(quotation, quotationLines, context);

        // 回链：置 quotation.isAccepted=true（纯标记，无 ORM FK 改动；quotationId 列已存在于订单）。
        quotation = requireEntity(String.valueOf(quotationId), null, context);
        quotation.setIsAccepted(true);
        dao().updateEntity(quotation);
        return order;
    }

    // ---------- validation helpers ----------

    private ErpSalQuotation requireQuotation(Long quotationId, IServiceContext context) {
        return requireEntity(String.valueOf(quotationId), null, context);
    }

    private void requireNotCancelled(ErpSalQuotation quotation) {
        Integer docStatus = quotation.getDocStatus();
        if (docStatus != null && docStatus == ErpSalConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(quotation, docStatus, "非已作废");
        }
    }

    private void requireLinesNonEmpty(ErpSalQuotation quotation) {
        if (loadLines(quotation.getId()).isEmpty()) {
            throw new NopException(ErpSalErrors.ERR_QUOTATION_LINES_EMPTY)
                    .param(ErpSalErrors.ARG_QUOTATION_CODE, quotation.getCode());
        }
    }

    /**
     * EXPIRED 派生校验：{@code validTo < today} 视为过期（无持久化 EXPIRED 列）。
     */
    private void requireNotExpired(ErpSalQuotation quotation) {
        LocalDate validTo = quotation.getValidTo();
        if (validTo != null && validTo.isBefore(CoreMetrics.currentDate())) {
            throw new NopException(ErpSalErrors.ERR_QUOTATION_EXPIRED)
                    .param(ErpSalErrors.ARG_QUOTATION_CODE, quotation.getCode())
                    .param(ErpSalErrors.ARG_VALID_TO, validTo);
        }
    }

    // ---------- query helpers ----------

    List<ErpSalQuotationLine> loadLines(Long quotationId) {
        // D2 边界场景：同聚合子表加载，父实体已由 requireEntity 经数据权限/Meta 管道授权，子行无独立权限规则。
        IEntityDao<ErpSalQuotationLine> dao = daoFor(ErpSalQuotationLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("quotationId", quotationId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    // ---------- misc helpers ----------

    private NopException illegalTransition(ErpSalQuotation quotation, Integer current, String expected) {
        return new NopException(ErpSalErrors.ERR_QUOTATION_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_QUOTATION_CODE, quotation.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, expected);
    }

    private NopException illegalDocTransition(ErpSalQuotation quotation, Integer current, String expected) {
        return new NopException(ErpSalErrors.ERR_QUOTATION_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_QUOTATION_CODE, quotation.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
