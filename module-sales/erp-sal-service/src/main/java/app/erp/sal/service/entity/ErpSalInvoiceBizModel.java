
package app.erp.sal.service.entity;

import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.biz.IErpSalInvoiceBiz;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalInvoiceLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.posting.SalInvoicePostingDispatcher;
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
 * 销售发票 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现三轴审批状态机（对齐
 * {@code docs/design/sales/state-machine.md}，与采购域镜像对称）+ 发票审核触发 AR_INVOICE 过账
 * （{@code posting.md}，借应收 / 贷收入 / 贷销项税）。
 *
 * <p>跨实体访问对齐 {@code ai-defaults.md}：客户启用校验经 {@link IErpMdPartnerBiz}；
 * 过账经 {@link SalInvoicePostingDispatcher} →凭证聚合根 Facade {@code IErpFinVoucherBiz}。
 *
 * <p>状态机迁移校验前置 {@code approveStatus}/{@code docStatus}，违反抛 {@link NopException}。
 */
@BizModel("ErpSalInvoice")
public class ErpSalInvoiceBizModel extends CrudBizModel<ErpSalInvoice> implements IErpSalInvoiceBiz {

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    @Inject
    SalInvoicePostingDispatcher postingDispatcher;

    public ErpSalInvoiceBizModel() {
        setEntityName(ErpSalInvoice.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalInvoice submit(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        ErpSalInvoice invoice = requireInvoice(invoiceId, context);
        requireNotCancelled(invoice);
        Integer status = invoice.getApproveStatus();
        if (status == null) {
            status = ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (status != ErpSalConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpSalConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(invoice, status, "UNSUBMITTED 或 REJECTED");
        }
        requireLinesNonEmpty(invoice);
        requireCustomerActive(invoice, context);
        invoice.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        dao().updateEntity(invoice);
        return invoice;
    }

    @Override
    @BizMutation
    public ErpSalInvoice withdrawSubmit(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        ErpSalInvoice invoice = requireInvoice(invoiceId, context);
        requireNotCancelled(invoice);
        Integer status = invoice.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(invoice, status, "SUBMITTED");
        }
        invoice.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        dao().updateEntity(invoice);
        return invoice;
    }

    @Override
    @BizMutation
    public ErpSalInvoice approve(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        ErpSalInvoice invoice = requireInvoice(invoiceId, context);
        Integer status = invoice.getApproveStatus();
        // 幂等：已审核单据再次审核为空操作（state-machine §4），凭证已存在，不重复触发。
        if (status != null && status == ErpSalConstants.APPROVE_STATUS_APPROVED) {
            return invoice;
        }
        requireNotCancelled(invoice);
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(invoice, status, "SUBMITTED");
        }
        requireCustomerActive(invoice, context);

        // AR_INVOICE 过账（跨域 REQUIRES_NEW 由 Facade 承接，失败吞异常保持终态 posted=false）
        boolean posted = postingDispatcher.tryPost(invoice);

        // 跨域 post 调用扰动会话脏跟踪，重新加载后置 posted 标志并显式持久化（对齐 ErpSalDeliveryBizModel）
        invoice = requireEntity(String.valueOf(invoiceId), null, context);
        invoice.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
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
    public ErpSalInvoice reject(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        ErpSalInvoice invoice = requireInvoice(invoiceId, context);
        requireNotCancelled(invoice);
        Integer status = invoice.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(invoice, status, "SUBMITTED");
        }
        invoice.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(invoice);
        return invoice;
    }

    @Override
    @BizMutation
    public ErpSalInvoice reverseApprove(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        ErpSalInvoice invoice = requireInvoice(invoiceId, context);
        Integer status = invoice.getApproveStatus();
        // 幂等：已 REJECTED（曾驳回或已反审核）无更多可冲销，直接返回。
        if (status != null && status == ErpSalConstants.APPROVE_STATUS_REJECTED) {
            return invoice;
        }
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_APPROVED) {
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
        invoice.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(invoice);
        return invoice;
    }

    @Override
    @BizMutation
    public ErpSalInvoice cancel(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        ErpSalInvoice invoice = requireInvoice(invoiceId, context);
        Integer docStatus = invoice.getDocStatus();
        if (docStatus != null && docStatus == ErpSalConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(invoice, docStatus, "非已作废");
        }
        Integer approveStatus = invoice.getApproveStatus();
        if (approveStatus != null && approveStatus == ErpSalConstants.APPROVE_STATUS_APPROVED
                && Boolean.TRUE.equals(invoice.getPosted())) {
            postingDispatcher.reverse(invoice);
            invoice = requireEntity(String.valueOf(invoiceId), null, context);
            invoice.setPosted(false);
            invoice.setPostedAt(null);
            invoice.setPostedBy(null);
        }
        invoice.setDocStatus(ErpSalConstants.DOC_STATUS_CANCELLED);
        dao().updateEntity(invoice);
        return invoice;
    }

    // ---------- validation helpers ----------

    private ErpSalInvoice requireInvoice(Long invoiceId, IServiceContext context) {
        return requireEntity(String.valueOf(invoiceId), null, context);
    }

    private void requireNotCancelled(ErpSalInvoice invoice) {
        Integer docStatus = invoice.getDocStatus();
        if (docStatus != null && docStatus == ErpSalConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(invoice, docStatus, "非已作废");
        }
    }

    private void requireLinesNonEmpty(ErpSalInvoice invoice) {
        if (loadLines(invoice.getId()).isEmpty()) {
            throw new NopException(ErpSalErrors.ERR_INVOICE_LINES_EMPTY)
                    .param(ErpSalErrors.ARG_INVOICE_CODE, invoice.getCode());
        }
    }

    private void requireCustomerActive(ErpSalInvoice invoice, IServiceContext context) {
        if (invoice.getCustomerId() == null) {
            return;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(invoice.getCustomerId(), context);
        if (partner == null || partner.getStatus() == null
                || partner.getStatus() != ErpSalConstants.PARTNER_STATUS_ACTIVE) {
            throw new NopException(ErpSalErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpSalErrors.ARG_CUSTOMER_ID, invoice.getCustomerId());
        }
    }

    // ---------- query helpers ----------

    List<ErpSalInvoiceLine> loadLines(Long invoiceId) {
        // D2 边界场景：同聚合子表加载，父实体已由 requireEntity 经数据权限/Meta 管道授权，子行无独立权限规则。
        IEntityDao<ErpSalInvoiceLine> dao = daoFor(ErpSalInvoiceLine.class);
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

    private NopException illegalTransition(ErpSalInvoice invoice, Integer current, String expected) {
        return new NopException(ErpSalErrors.ERR_INVOICE_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_INVOICE_CODE, invoice.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, expected);
    }

    private NopException illegalDocTransition(ErpSalInvoice invoice, Integer current, String expected) {
        return new NopException(ErpSalErrors.ERR_INVOICE_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_INVOICE_CODE, invoice.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
