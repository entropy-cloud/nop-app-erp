
package app.erp.pur.service.entity;

import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.biz.IErpPurInvoiceBiz;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurInvoiceLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.posting.PurInvoicePostingDispatcher;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.dao.api.IEntityDao;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 采购发票 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现三轴审批状态机（对齐
 * {@code docs/design/purchase/state-machine.md}）+ 发票审核执行三单匹配（{@code three-way-match.md}）
 * + 触发 AP_INVOICE 过账（{@code posting.md}，借费用/采购 + 借进项税 / 贷应付）。
 *
 * <p>跨实体访问对齐 {@code ai-defaults.md}：供应商启用校验经 {@link IErpMdPartnerBiz}；
 * 三单匹配经 {@link ThreeWayMatcher}（回链入库行→订单行）；过账经 {@link PurInvoicePostingDispatcher}
 * →凭证聚合根 Facade {@code IErpFinVoucherBiz}。
 *
 * <p>状态机迁移校验前置 {@code approveStatus}/{@code docStatus}，违反抛 {@link NopException}。
 */
@BizModel("ErpPurInvoice")
public class ErpPurInvoiceBizModel extends CrudBizModel<ErpPurInvoice> implements IErpPurInvoiceBiz {

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    @Inject
    ThreeWayMatcher threeWayMatcher;

    @Inject
    PurInvoicePostingDispatcher postingDispatcher;

    public ErpPurInvoiceBizModel() {
        setEntityName(ErpPurInvoice.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurInvoice submit(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        ErpPurInvoice invoice = requireInvoice(invoiceId, context);
        requireNotCancelled(invoice);
        Integer status = invoice.getApproveStatus();
        if (status == null) {
            status = ErpPurConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (status != ErpPurConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpPurConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(invoice, status, "UNSUBMITTED 或 REJECTED");
        }
        requireLinesNonEmpty(invoice);
        requireSupplierActive(invoice, context);
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        dao().updateEntity(invoice);
        return invoice;
    }

    @Override
    @BizMutation
    public ErpPurInvoice withdrawSubmit(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        ErpPurInvoice invoice = requireInvoice(invoiceId, context);
        requireNotCancelled(invoice);
        Integer status = invoice.getApproveStatus();
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(invoice, status, "SUBMITTED");
        }
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        dao().updateEntity(invoice);
        return invoice;
    }

    @Override
    @BizMutation
    public ErpPurInvoice approve(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        ErpPurInvoice invoice = requireInvoice(invoiceId, context);
        Integer status = invoice.getApproveStatus();
        // 幂等：已审核单据再次审核为空操作（state-machine §4），凭证已存在，不重复触发。
        if (status != null && status == ErpPurConstants.APPROVE_STATUS_APPROVED) {
            return invoice;
        }
        requireNotCancelled(invoice);
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(invoice, status, "SUBMITTED");
        }
        requireSupplierActive(invoice, context);

        // 三单匹配（订单↔入库↔发票）。失败按严格模式拒绝审核；非严格模式放行告警。先于过账执行，
        // 避免不匹配单据产生应付凭证。
        List<ErpPurInvoiceLine> lines = loadLines(invoice.getId());
        threeWayMatcher.match(invoice.getCode(), lines, null);

        // AP_INVOICE 过账（跨域 REQUIRES_NEW 由 Facade 承接，失败吞异常保持终态 posted=false）
        boolean posted = postingDispatcher.tryPost(invoice);

        // 跨域 generateMove/post 调用可能扰动会话脏跟踪，故重新加载并以 updateEntity 显式持久化
        // （对齐 ErpPurReceiveBizModel.approve）。
        invoice = requireEntity(String.valueOf(invoiceId), null, context);
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        invoice.setApprovedBy(currentUserId());
        invoice.setApprovedAt(CoreMetrics.currentDateTime());
        if (posted) {
            invoice.setPosted(true);
            invoice.setPostedAt(CoreMetrics.currentDateTime());
            invoice.setPostedBy(currentUserId());
        }
        dao().updateEntity(invoice);
        return invoice;
    }

    @Override
    @BizMutation
    public ErpPurInvoice reject(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        ErpPurInvoice invoice = requireInvoice(invoiceId, context);
        requireNotCancelled(invoice);
        Integer status = invoice.getApproveStatus();
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(invoice, status, "SUBMITTED");
        }
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(invoice);
        return invoice;
    }

    @Override
    @BizMutation
    public ErpPurInvoice reverseApprove(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        ErpPurInvoice invoice = requireInvoice(invoiceId, context);
        Integer status = invoice.getApproveStatus();
        // 幂等：已 REJECTED（曾驳回或已反审核）无更多可冲销，直接返回。
        if (status != null && status == ErpPurConstants.APPROVE_STATUS_REJECTED) {
            return invoice;
        }
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(invoice, status, "APPROVED");
        }
        // 红字冲销已过账凭证（幂等：未过账则跳过），冲销前置防篡改已入账数据（state-machine §3）
        if (Boolean.TRUE.equals(invoice.getPosted())) {
            postingDispatcher.reverse(invoice);
            // 跨域 reverse 调用扰动会话脏跟踪，重新加载后置 posted=false 并显式持久化。
            invoice = requireEntity(String.valueOf(invoiceId), null, context);
            invoice.setPosted(false);
            invoice.setPostedAt(null);
            invoice.setPostedBy(null);
        }
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(invoice);
        return invoice;
    }

    @Override
    @BizMutation
    public ErpPurInvoice cancel(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        ErpPurInvoice invoice = requireInvoice(invoiceId, context);
        Integer docStatus = invoice.getDocStatus();
        if (docStatus != null && docStatus == ErpPurConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(invoice, docStatus, "非已作废");
        }
        Integer approveStatus = invoice.getApproveStatus();
        if (approveStatus != null && approveStatus == ErpPurConstants.APPROVE_STATUS_APPROVED
                && Boolean.TRUE.equals(invoice.getPosted())) {
            postingDispatcher.reverse(invoice);
            invoice = requireEntity(String.valueOf(invoiceId), null, context);
            invoice.setPosted(false);
            invoice.setPostedAt(null);
            invoice.setPostedBy(null);
        }
        invoice.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        dao().updateEntity(invoice);
        return invoice;
    }

    // ---------- validation helpers ----------

    private ErpPurInvoice requireInvoice(Long invoiceId, IServiceContext context) {
        return requireEntity(String.valueOf(invoiceId), null, context);
    }

    private void requireNotCancelled(ErpPurInvoice invoice) {
        Integer docStatus = invoice.getDocStatus();
        if (docStatus != null && docStatus == ErpPurConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(invoice, docStatus, "非已作废");
        }
    }

    private void requireLinesNonEmpty(ErpPurInvoice invoice) {
        if (loadLines(invoice.getId()).isEmpty()) {
            throw new NopException(ErpPurErrors.ERR_INVOICE_LINES_EMPTY)
                    .param(ErpPurErrors.ARG_INVOICE_CODE, invoice.getCode());
        }
    }

    private void requireSupplierActive(ErpPurInvoice invoice, IServiceContext context) {
        if (invoice.getSupplierId() == null) {
            return;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(invoice.getSupplierId(), context);
        if (partner == null || partner.getStatus() == null
                || partner.getStatus() != ErpPurConstants.PARTNER_STATUS_ACTIVE) {
            throw new NopException(ErpPurErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpPurErrors.ARG_SUPPLIER_ID, invoice.getSupplierId());
        }
    }

    // ---------- query helpers ----------

    List<ErpPurInvoiceLine> loadLines(Long invoiceId) {
        // D2 边界场景：同聚合子表加载，父实体已由 requireEntity 经数据权限/Meta 管道授权，子行无独立权限规则。
        IEntityDao<ErpPurInvoiceLine> dao = daoFor(ErpPurInvoiceLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("invoiceId", invoiceId));
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

    private NopException illegalTransition(ErpPurInvoice invoice, Integer current, String expected) {
        return new NopException(ErpPurErrors.ERR_INVOICE_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_INVOICE_CODE, invoice.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, expected);
    }

    private NopException illegalDocTransition(ErpPurInvoice invoice, Integer current, String expected) {
        return new NopException(ErpPurErrors.ERR_INVOICE_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_INVOICE_CODE, invoice.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
