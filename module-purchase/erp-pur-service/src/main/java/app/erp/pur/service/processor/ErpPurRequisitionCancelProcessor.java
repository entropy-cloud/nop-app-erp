package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurRequisition;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.statemachine.ErpPurRequisitionDocumentStateMachine;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpPurRequisition）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpPurRequisition cancel per-mutation Processor (plan 2026-07-25-1057-2；StateMachine 接线 plan 2026-08-12-0918-1 M2.7)。
 *
 * <p>运行 {@link AbstractCancelProcessor} 骨架；固定来源态/目标态判断委托
 * {@link ErpPurRequisitionDocumentStateMachine}（docStatus 业务生命周期轴 Bean，契约 §4/§7）。
 * Requisition 无 beforeCancel 业务 hook（保持）。
 *
 * <p>非法边：Bean 直抛领域码 {@link ErpPurErrors#ERR_REQ_ILLEGAL_DOC_STATUS_TRANSITION}
 * （plan 2026-09-07-2200-1），{@link #validateTransitionForCancel} 同码补参补齐 {@code requisitionCode} 元数据。
 */
public class ErpPurRequisitionCancelProcessor extends AbstractCancelProcessor<ErpPurRequisition> {

    @Inject
    ErpPurRequisitionProcessor processor;

    @Inject
    ErpPurRequisitionDocumentStateMachine stateMachine;

    @Override
    protected IEntityDao<ErpPurRequisition> dao() {
        return daoProvider.daoFor(ErpPurRequisition.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpPurErrors.ERR_REQ_NOT_FOUND)
                .param(ErpPurErrors.ARG_REQUISITION_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpPurRequisition entity, String current, String... expected) {
        return new NopException(ErpPurErrors.ERR_REQ_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_REQUISITION_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateTransitionForCancel(ErpPurRequisition entity, IServiceContext context) {
        try {
            stateMachine.assertCanCancel(entity.getDocStatus());
        } catch (NopException e) {
            throw e.param(ErpPurErrors.ARG_REQUISITION_CODE, entity.getCode());
        }
    }

    @Override
    protected String getDocStatus(ErpPurRequisition entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpPurRequisition entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return stateMachine.cancelTargetStatus();
    }
}
