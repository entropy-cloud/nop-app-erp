package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurPayment;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.common.service.AbstractCancelProcessor;
import app.erp.pur.service.entity.PaymentSettler;
import app.erp.pur.service.posting.PurPaymentPostingDispatcher;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpPurPayment）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。

public class ErpPurPaymentCancelProcessor extends AbstractCancelProcessor<ErpPurPayment> {

    @Inject
    ErpPurPaymentProcessor processor;

    @Inject
    PurPaymentPostingDispatcher postingDispatcher;

    @Inject
    PaymentSettler settler;

    @Override
    public ErpPurPayment cancel(String id, IServiceContext context) {
        ErpPurPayment payment = requireEntity(id);
        processor.validateTransitionForCancel(payment, context);
        // P2-CK-pur-005：存在净核销（含反向负行后净额≠0）拒绝作废——GL 凭证红冲后发票 paidAmount/paidStatus
        // 派生态将失真；须先 reverseSettlement（returns.md §异常处理「需先撤回核销」拒绝语义）。
        if (settler.sumNetSettledForPayment(id).signum() != 0) {
            throw new NopException(ErpPurErrors.ERR_PAYMENT_SETTLED_EXISTS)
                    .param(ErpPurErrors.ARG_PAYMENT_CODE, payment.getCode());
        }
        String approveStatus = payment.getApproveStatus();
        if (approveStatus != null && Objects.equals(approveStatus, ErpPurConstants.APPROVE_STATUS_APPROVED)
                && Boolean.TRUE.equals(payment.getPosted())) {
            postingDispatcher.reverse(payment);
            payment = dao().getEntityById(id);
            payment.setPosted(false);
            payment.setPostedAt(null);
            payment.setPostedBy(null);
        }
        processor.doCancel(payment, context);
        return payment;
    }

    @Override
    protected IEntityDao<ErpPurPayment> dao() {
        return daoProvider.daoFor(ErpPurPayment.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpPurErrors.ERR_PAYMENT_NOT_FOUND)
                .param(ErpPurErrors.ARG_PAYMENT_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpPurPayment entity, String current, String... expected) {
        return new NopException(ErpPurErrors.ERR_PAYMENT_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_PAYMENT_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getDocStatus(ErpPurPayment entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpPurPayment entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return ErpPurConstants.DOC_STATUS_CANCELLED;
    }
}
