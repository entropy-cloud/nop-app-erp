package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.common.service.AbstractApproveProcessor;
import app.erp.common.service.SoDGuard;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpPurInvoice）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。

public class ErpPurInvoiceApproveProcessor extends AbstractApproveProcessor<ErpPurInvoice> {

    @Inject
    ErpPurInvoiceProcessor processor;

    @Override
    public ErpPurInvoice approve(String id, IServiceContext context) {
        ErpPurInvoice invoice = requireEntity(id);
        if (invoice.isApproved()) {
            return invoice;
        }
        // F1.2（P1-CK-pur-002）：SoD 守卫 SoD-first（最廉价失败优先，对齐 ErpPurReceiveApproveProcessor 范式）——
        // 前置于 doPosting（REQUIRES_NEW 凭证独立提交）之前，杜绝「守卫抛错回滚主事务但凭证已提交」的孤儿凭证
        SoDGuard.assertApproverNotCreator(invoice.getCreatedBy(), currentUserId(),
                ErpPurErrors.ERR_PUR_APPROVER_IS_CREATOR);
        processor.validateNotCancelled(invoice, context);
        processor.validateTransitionForApprove(invoice, context);
        processor.validateBusinessRulesForApprove(invoice, context);
        // F1.2（R3）：承付释放 hook 前移至 doPosting 之前（release 为 SYNC 同事务，前移事务安全；
        // 原位置在 REQUIRES_NEW 凭证之后构成孤儿凭证窗口）
        processor.runCommitmentReleaseOnInvoiceApproveHook(invoice, context);
        boolean posted = processor.doPosting(invoice, context);
        invoice = dao().getEntityById(id);
        processor.doApprove(invoice, posted, context);
        return invoice;
    }

    @Override
    protected IEntityDao<ErpPurInvoice> dao() {
        return daoProvider.daoFor(ErpPurInvoice.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpPurErrors.ERR_INVOICE_NOT_FOUND)
                .param(ErpPurErrors.ARG_INVOICE_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpPurInvoice entity, String current, String... expected) {
        return new NopException(ErpPurErrors.ERR_INVOICE_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_INVOICE_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateNotCancelled(ErpPurInvoice entity, IServiceContext context) {
        processor.validateNotCancelled(entity, context);
    }

    @Override
    protected String getApproveStatus(ErpPurInvoice entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpPurConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpPurInvoice entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpPurInvoice entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpPurInvoice entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isApproved(ErpPurInvoice entity) {
        return entity.isApproved();
    }

    @Override
    protected boolean isCancelled(ErpPurInvoice entity) {
        return entity.isCancelled();
    }

    @Override
    protected String submittedStatus() {
        return ErpPurConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String approvedStatus() {
        return ErpPurConstants.APPROVE_STATUS_APPROVED;
    }
}
