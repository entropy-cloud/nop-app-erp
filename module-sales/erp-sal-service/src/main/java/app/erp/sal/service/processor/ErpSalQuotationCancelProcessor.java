package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalQuotation;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.statemachine.ErpSalQuotationDocumentStateMachine;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpSalQuotation）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpSalQuotation cancel per-mutation Processor (plan 2026-07-30-1433-2 R5.2；StateMachine 接线 plan 2026-08-12-0918-2 M2.9)。
 *
 * <p>运行 {@link AbstractCancelProcessor} 骨架；固定来源态/目标态判断委托
 * {@link ErpSalQuotationDocumentStateMachine}（docStatus 业务生命周期轴 Bean，契约 §4/§7）。
 * 报价单 cancel 无域特有 hook（facade cancel 仅 setDocStatus）。
 *
 * <p>非法边直抛领域码：{@link ErpSalQuotationDocumentStateMachine} 直接抛
 * {@link ErpSalErrors#ERR_QUOTATION_ILLEGAL_DOC_STATUS_TRANSITION}（plan 2026-09-07-2200-1），
 * {@link #validateTransitionForCancel} 同码补参 {@code quotationCode} 实体编号。
 */
public class ErpSalQuotationCancelProcessor extends AbstractCancelProcessor<ErpSalQuotation> {

    @Inject
    ErpSalQuotationProcessor processor;

    @Inject
    ErpSalQuotationDocumentStateMachine stateMachine;

    @Override
    protected IEntityDao<ErpSalQuotation> dao() {
        return daoProvider.daoFor(ErpSalQuotation.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpSalErrors.ERR_QUOTATION_NOT_FOUND)
                .param(ErpSalErrors.ARG_QUOTATION_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpSalQuotation entity, String current, String... expected) {
        return new NopException(ErpSalErrors.ERR_QUOTATION_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_QUOTATION_CODE, entity.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateTransitionForCancel(ErpSalQuotation entity, IServiceContext context) {
        try {
            stateMachine.assertCanCancel(entity.getDocStatus());
        } catch (NopException e) {
            throw e.param(ErpSalErrors.ARG_QUOTATION_CODE, entity.getCode());
        }
    }

    @Override
    protected String getDocStatus(ErpSalQuotation entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpSalQuotation entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return stateMachine.cancelTargetStatus();
    }
}
