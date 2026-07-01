
package app.erp.sal.service.entity;

import app.erp.sal.biz.IErpSalQuotationBiz;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.dao.entity.ErpSalQuotation;
import app.erp.sal.dao.entity.ErpSalQuotationLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.annotations.txn.Transactional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.annotations.core.Name;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * 销售报价单 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现审核/客户确认状态机 + 报价→订单转化
 * （对齐 {@code docs/design/sales/quotation.md}）。
 *
 * <ul>
 *   <li>审核轴：UNSUBMITTED→SUBMITTED→APPROVED/REJECTED，驳回→重提，反审核 APPROVED→REJECTED。</li>
 *   <li>{@link #confirmCustomerAccepted}：APPROVED 且 {@code validTo ≥ today} → {@code isAccepted=true}
 *       （「客户确认 ACCEPTED」映射为布尔标记——模型无独立客户确认状态轴）。</li>
 *   <li>{@link #convertToOrder}：APPROVED + {@code isAccepted} + 未过期 → 经 {@link QuotationToOrderConverter}
 *       组装 {@link ErpSalOrder}(UNSUBMITTED/DRAFT，回链 {@code quotationId}) + 行；幂等防重复转化
 *       （按 {@code quotationId AND docStatus≠CANCELLED} 查既有订单）；转化成功置 {@code isAccepted=true}。</li>
 * </ul>
 *
 * <p>模型边界：报价单无 {@code approvedBy}/{@code approvedAt} 列——审核仅翻转 {@code approveStatus}，
 * 不记录审核人/时间（已知缺口，不改 ORM）。「EXPIRED」不在持久化（无列），由 {@code validTo < today} 在
 * 确认/转化时派生校验（无后台扫描）。
 */
@BizModel("ErpSalQuotation")
public class ErpSalQuotationBizModel extends CrudBizModel<ErpSalQuotation> implements IErpSalQuotationBiz {

    @Inject
    QuotationToOrderConverter quotationToOrderConverter;

    @Inject
    IOrmTemplate ormTemplate;

    public ErpSalQuotationBizModel() {
        setEntityName(ErpSalQuotation.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalQuotation submit(@Name("quotationId") Long quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId);
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
        ErpSalQuotation quotation = requireQuotation(quotationId);
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
        ErpSalQuotation quotation = requireQuotation(quotationId);
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
        ErpSalQuotation quotation = requireQuotation(quotationId);
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
        ErpSalQuotation quotation = requireQuotation(quotationId);
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
        ErpSalQuotation quotation = requireQuotation(quotationId);
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
        ErpSalQuotation quotation = requireQuotation(quotationId);
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
        ErpSalQuotation quotation = requireQuotation(quotationId);
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
        // 幂等防重复转化：已存在 docStatus≠CANCELLED 且 quotationId=该报价 的订单 → 拒绝。
        if (findConvertedOrder(quotationId) != null) {
            throw new NopException(ErpSalErrors.ERR_QUOTATION_ALREADY_CONVERTED)
                    .param(ErpSalErrors.ARG_QUOTATION_CODE, quotation.getCode());
        }

        List<ErpSalQuotationLine> quotationLines = loadLines(quotationId);
        ErpSalOrder order = quotationToOrderConverter.build(quotation, quotationLines);
        // 持久化头 + 行（经注入的 IErpSalOrderBiz 的 dao 保存，确保订单实体在 sales 域内统一落库）。
        saveConvertedOrder(order);

        // 回链：置 quotation.isAccepted=true（纯标记，无 ORM FK 改动；quotationId 列已存在于订单）。
        quotation = dao().getEntityById(quotationId);
        quotation.setIsAccepted(true);
        dao().updateEntity(quotation);
        return order;
    }

    // ---------- conversion helpers ----------

    /**
     * 持久化转化订单头 + 行。订单 code 由 UUID 生成（避免与既有编码规则耦合）；行依次 saveEntity。
     */
    private void saveConvertedOrder(ErpSalOrder order) {
        if (order.getCode() == null) {
            order.setCode("SO-FROM-Q-" + io.nop.commons.util.StringHelper.generateUUID());
        }
        IEntityDao<ErpSalOrder> orderDao = daoFor(ErpSalOrder.class);
        orderDao.saveEntity(order);
        IEntityDao<ErpSalOrderLine> lineDao = daoFor(ErpSalOrderLine.class);
        for (ErpSalOrderLine line : order.getLines() == null ? new ArrayList<ErpSalOrderLine>() : order.getLines()) {
            line.setOrderId(order.getId());
            lineDao.saveEntity(line);
        }
    }

    /**
     * 按 {@code quotationId AND docStatus≠CANCELLED} 查既有转化订单（纯查询，幂等键）。
     */
    private ErpSalOrder findConvertedOrder(Long quotationId) {
        ormTemplate.flushSession();
        IEntityDao<ErpSalOrder> dao = daoFor(ErpSalOrder.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("quotationId", quotationId), ne("docStatus", ErpSalConstants.DOC_STATUS_CANCELLED)));
        List<ErpSalOrder> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    // ---------- validation helpers ----------

    private ErpSalQuotation requireQuotation(Long quotationId) {
        ErpSalQuotation quotation = dao().getEntityById(quotationId);
        if (quotation == null) {
            throw new NopException(ErpSalErrors.ERR_QUOTATION_NOT_FOUND)
                    .param(ErpSalErrors.ARG_QUOTATION_ID, quotationId);
        }
        return quotation;
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
